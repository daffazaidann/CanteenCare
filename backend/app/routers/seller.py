from fastapi import APIRouter, BackgroundTasks, Query, status
from sqlalchemy import select

from app.core.deps import DbSession, SellerUser
from app.core.errors import AppError
from app.models import Menu, Nutrition, Order, User, Vendor
from app.models.enums import MenuStatus, OrderStatus
from app.schemas.catalog import MenuCreate, MenuOut, StockUpdate
from app.schemas.order import OrderOut, RejectRequest
from app.services import notification_service, order_service

router = APIRouter(prefix="/seller", tags=["Penjual"])


def _own_vendor(seller: User) -> Vendor:
    if seller.vendor is None:
        raise AppError(404, "NOT_FOUND", "Stan Anda tidak ditemukan.")
    return seller.vendor


def _own_order(db: DbSession, seller: User, order_id: int) -> Order:
    # Penjual hanya boleh menyentuh pesanan stannya sendiri; selain itu 404
    order = db.get(Order, order_id)
    if order is None or order.vendor_id != _own_vendor(seller).id:
        raise AppError(404, "NOT_FOUND", "Pesanan tidak ditemukan.")
    return order


def _own_menu(db: DbSession, seller: User, menu_id: int) -> Menu:
    menu = db.get(Menu, menu_id)
    if menu is None or menu.vendor_id != _own_vendor(seller).id:
        raise AppError(404, "NOT_FOUND", "Menu tidak ditemukan.")
    return menu


@router.get("/orders", response_model=list[OrderOut], summary="Pesanan masuk ke stan saya")
def list_orders(
    db: DbSession,
    seller: SellerUser,
    status_filter: OrderStatus | None = Query(default=None, alias="status"),
) -> list[OrderOut]:
    orders = order_service.list_orders(db, vendor_id=_own_vendor(seller).id, status=status_filter)
    return [OrderOut.from_order(order) for order in orders]


@router.post("/orders/{order_id}/confirm", response_model=OrderOut, summary="Konfirmasi pesanan (stok berkurang)")
def confirm_order(order_id: int, db: DbSession, seller: SellerUser, background: BackgroundTasks) -> OrderOut:
    order = order_service.confirm_order(db, _own_order(db, seller, order_id))
    background.add_task(notification_service.notify_order_confirmed, order.id)
    return OrderOut.from_order(order)


@router.post("/orders/{order_id}/reject", response_model=OrderOut, summary="Tolak pesanan (stok tetap)")
def reject_order(
    order_id: int, body: RejectRequest, db: DbSession, seller: SellerUser, background: BackgroundTasks
) -> OrderOut:
    order = order_service.reject_order(db, _own_order(db, seller, order_id), body.reason.strip())
    background.add_task(notification_service.notify_order_rejected, order.id)
    return OrderOut.from_order(order)


@router.post("/orders/{order_id}/ready", response_model=OrderOut, summary="Tandai pesanan siap diambil")
def mark_order_ready(order_id: int, db: DbSession, seller: SellerUser, background: BackgroundTasks) -> OrderOut:
    order = order_service.mark_order_ready(db, _own_order(db, seller, order_id))
    background.add_task(notification_service.notify_order_ready, order.id)
    return OrderOut.from_order(order)


@router.post("/orders/{order_id}/complete", response_model=OrderOut, summary="Selesaikan pesanan (sudah diambil)")
def complete_order(order_id: int, db: DbSession, seller: SellerUser, background: BackgroundTasks) -> OrderOut:
    order = order_service.complete_order(db, _own_order(db, seller, order_id))
    background.add_task(notification_service.notify_order_completed, order.id)
    return OrderOut.from_order(order)


@router.get("/menus", response_model=list[MenuOut], summary="Semua menu milik saya")
def list_menus(db: DbSession, seller: SellerUser) -> list[MenuOut]:
    query = select(Menu).where(Menu.vendor_id == _own_vendor(seller).id).order_by(Menu.name)
    return [MenuOut.from_menu(menu) for menu in db.scalars(query)]


@router.post(
    "/menus", response_model=MenuOut, status_code=status.HTTP_201_CREATED, summary="Ajukan menu baru (PENDING)"
)
def create_menu(body: MenuCreate, db: DbSession, seller: SellerUser) -> MenuOut:
    menu = Menu(
        vendor=_own_vendor(seller),
        status=MenuStatus.PENDING,
        **body.model_dump(exclude={"nutrition"}),
    )
    # Gizi dari penjual masih estimasi: verified_by kosong sampai admin memverifikasi
    menu.nutrition = Nutrition(**body.nutrition.model_dump(), verified_by=None)
    db.add(menu)
    db.commit()
    return MenuOut.from_menu(menu)


@router.patch("/menus/{menu_id}/stock", response_model=MenuOut, summary="Ubah stok menu")
def update_stock(menu_id: int, body: StockUpdate, db: DbSession, seller: SellerUser) -> MenuOut:
    menu = _own_menu(db, seller, menu_id)
    menu.stock = body.stock
    db.commit()
    return MenuOut.from_menu(menu)


@router.delete("/menus/{menu_id}", response_model=MenuOut, summary="Arsipkan menu")
def archive_menu(menu_id: int, db: DbSession, seller: SellerUser) -> MenuOut:
    # Tidak pernah dihapus permanen, karena riwayat pesanan masih merujuk ke menu ini
    menu = _own_menu(db, seller, menu_id)
    menu.status = MenuStatus.ARCHIVED
    db.commit()
    return MenuOut.from_menu(menu)
