import secrets
from datetime import date, datetime

from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.core.errors import AppError
from app.core.time import WIB, utcnow, wib_day_range_utc
from app.models import Menu, Order, OrderItem, User, Vendor
from app.models.enums import MenuStatus, OrderStatus, PaymentMethod

NUTRIENT_FIELDS = ("calories", "carbs", "protein", "fat", "salt", "sugar")


def generate_order_number(created_at: datetime) -> str:
    """Format CC-YYYYMMDD-XXXX. Tanggal memakai WIB, XXXX = 4 digit hex acak huruf besar."""
    date_part = created_at.astimezone(WIB).strftime("%Y%m%d")
    return f"CC-{date_part}-{secrets.token_hex(2).upper()}"


def snapshot_item(menu: Menu, quantity: int) -> OrderItem:
    """Menyalin nama, harga, dan gizi per porsi dari menu ke item pesanan."""
    nutrition = menu.nutrition
    return OrderItem(
        menu_id=menu.id,
        menu_name=menu.name,
        unit_price=menu.price,
        quantity=quantity,
        **{field: getattr(nutrition, field) for field in NUTRIENT_FIELDS},
    )


def apply_totals(order: Order) -> None:
    """Menghitung ulang total harga dan total gizi pesanan dari item-itemnya.

    total = Σ(nilai per porsi × jumlah). Server tidak pernah memakai total dari client.
    """
    order.total_price = sum(item.unit_price * item.quantity for item in order.items)
    for field in NUTRIENT_FIELDS:
        total = sum(getattr(item, field) * item.quantity for item in order.items)
        setattr(order, f"total_{field}", round(total, 2))


def _insufficient_stock(menu: Menu) -> AppError:
    return AppError(409, "INSUFFICIENT_STOCK", f"Stok {menu.name} tidak mencukupi, tersisa {menu.stock}.")


def _unique_order_number(db: Session, created_at: datetime) -> str:
    # Peluang bentrok sangat kecil, tapi tetap dicek supaya kolom unique tidak gagal
    while True:
        number = generate_order_number(created_at)
        if db.scalar(select(Order.id).where(Order.order_number == number)) is None:
            return number


def create_order(
    db: Session,
    buyer: User,
    vendor_id: int,
    payment_method: PaymentMethod,
    note: str | None,
    items: list[tuple[int, int]],
) -> Order:
    """Membuat pesanan baru berstatus MENUNGGU. `items` berisi pasangan (menu_id, quantity)."""
    # 1. Item tidak boleh kosong dan tidak boleh ada menu ganda
    if not items:
        raise AppError(400, "EMPTY_ITEMS", "Pesanan harus berisi minimal satu menu.")
    menu_ids = [menu_id for menu_id, _ in items]
    if len(set(menu_ids)) != len(menu_ids):
        raise AppError(400, "DUPLICATE_MENU", "Menu yang sama tidak boleh muncul dua kali dalam satu pesanan.")

    # 2. Stan harus ada dan aktif
    vendor = db.get(Vendor, vendor_id)
    if vendor is None or not vendor.is_active:
        raise AppError(404, "NOT_FOUND", "Stan tidak ditemukan.")

    order = Order(
        buyer=buyer,
        vendor=vendor,
        status=OrderStatus.MENUNGGU,
        payment_method=payment_method,
        note=note,
        created_at=utcnow(),
    )
    for menu_id, quantity in items:
        # 2. Setiap menu harus ada, APPROVED, dan milik stan yang sama
        menu = db.get(Menu, menu_id)
        if menu is None or menu.vendor_id != vendor.id or menu.status != MenuStatus.APPROVED:
            raise AppError(400, "MENU_NOT_AVAILABLE", f"Menu dengan id {menu_id} tidak tersedia di {vendor.name}.")
        # 3. Jumlah tidak boleh melebihi stok saat ini
        if quantity > menu.stock:
            raise _insufficient_stock(menu)
        # 4. Simpan snapshot nama, harga, dan gizi dari database
        order.items.append(snapshot_item(menu, quantity))

    # 4. Total dihitung ulang dari database. 5. Stok BELUM dikurangi di tahap ini.
    apply_totals(order)
    order.order_number = _unique_order_number(db, order.created_at)
    db.add(order)
    db.commit()
    return order


def list_orders(
    db: Session,
    *,
    buyer_id: int | None = None,
    vendor_id: int | None = None,
    status: OrderStatus | None = None,
    day: date | None = None,
) -> list[Order]:
    """Daftar pesanan terbaru dulu, dengan filter opsional. `day` adalah tanggal WIB."""
    query = select(Order).order_by(Order.created_at.desc(), Order.id.desc())
    if buyer_id is not None:
        query = query.where(Order.buyer_id == buyer_id)
    if vendor_id is not None:
        query = query.where(Order.vendor_id == vendor_id)
    if status is not None:
        query = query.where(Order.status == status)
    if day is not None:
        # Tanggal WIB diubah ke rentang UTC karena database menyimpan UTC
        start, end = wib_day_range_utc(day)
        query = query.where(Order.created_at >= start, Order.created_at <= end)
    return list(db.scalars(query))


def _ensure_status(order: Order, *allowed: OrderStatus) -> None:
    # Setiap transisi hanya boleh dari status tertentu, supaya pesanan tidak melompat tahap.
    if order.status not in allowed:
        raise AppError(409, "INVALID_ORDER_STATE", f"Pesanan {order.order_number} berstatus {order.status}.")


def confirm_order(db: Session, order: Order) -> Order:
    """MENUNGGU → SEDANG_DIMASAK sambil mengurangi stok semua item dalam satu transaksi."""
    _ensure_status(order, OrderStatus.MENUNGGU)
    for item in order.items:
        # UPDATE bersyarat: stok hanya dikurangi jika masih cukup. Ini aman meskipun
        # ada dua konfirmasi yang berjalan bersamaan.
        result = db.execute(
            update(Menu)
            .where(Menu.id == item.menu_id, Menu.stock >= item.quantity)
            .values(stock=Menu.stock - item.quantity)
        )
        if result.rowcount == 0:
            # Ada item yang stoknya kurang: batalkan SEMUA perubahan stok di transaksi ini
            db.rollback()
            raise _insufficient_stock(db.get(Menu, item.menu_id))

    order.status = OrderStatus.SEDANG_DIMASAK
    # resolved_at baru diisi saat pesanan benar-benar selesai atau ditolak.
    db.commit()
    db.expire_all()  # muat ulang stok menu terbaru dari database
    return order


def mark_order_ready(db: Session, order: Order) -> Order:
    """SEDANG_DIMASAK → SIAP_DIAMBIL. Stok tidak berubah."""
    _ensure_status(order, OrderStatus.SEDANG_DIMASAK)
    order.status = OrderStatus.SIAP_DIAMBIL
    db.commit()
    return order


def complete_order(db: Session, order: Order) -> Order:
    """SIAP_DIAMBIL → SELESAI. Pesanan masuk riwayat dan resolved_at diisi."""
    _ensure_status(order, OrderStatus.SIAP_DIAMBIL)
    order.status = OrderStatus.SELESAI
    order.resolved_at = utcnow()
    db.commit()
    return order


def reject_order(db: Session, order: Order, reason: str) -> Order:
    """MENUNGGU → DITOLAK. Stok tidak berubah dan pesanan masuk riwayat."""
    _ensure_status(order, OrderStatus.MENUNGGU)
    order.status = OrderStatus.DITOLAK
    order.rejection_reason = reason
    order.resolved_at = utcnow()
    db.commit()
    return order
