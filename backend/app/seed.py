"""Data demo CanteenCare.

Seeding hanya berjalan jika tabel users masih kosong (idempotent),
atau dipaksa lewat reset_database() untuk keperluan demo.
Nilai gizi adalah estimasi yang wajar, bukan hasil uji laboratorium.
"""

import logging
from datetime import timedelta

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.security import hash_password
from app.core.time import utcnow, wib_day_range_utc, wib_today
from app.db import Base, SessionLocal, engine
from app.models import Menu, Nutrition, NutritionTarget, Order, User, Vendor
from app.models.enums import MenuCategory, MenuStatus, OrderStatus, PaymentMethod, Role
from app.services.order_service import apply_totals, generate_order_number, snapshot_item

logger = logging.getLogger(__name__)

# (email, nama stan, deskripsi, jam buka, daftar menu)
# Menu: (nama, kategori, harga, stok, (kalori, karbo, protein, lemak, garam mg, gula), status, terverifikasi)
SELLERS = [
    (
        "gapura@canteencare.id", "Dapur Gapura", "Aneka mie dan nasi rumahan", "07.00 - 16.00",
        [
            ("Mie Ayam Solo", MenuCategory.MAKANAN, 20000, 25, (650, 85, 30, 25, 1800, 6), MenuStatus.APPROVED, True),
            ("Tahu Telur", MenuCategory.MAKANAN, 12000, 20, (420, 30, 18, 26, 900, 12), MenuStatus.APPROVED, True),
            ("Nasi Goreng Ayam", MenuCategory.MAKANAN, 17000, 30, (600, 80, 22, 21, 1500, 5), MenuStatus.APPROVED, True),
            ("Es Teh Manis", MenuCategory.MINUMAN, 4000, 50, (120, 30, 0, 0, 10, 30), MenuStatus.APPROVED, True),
            # Menu PENDING yang belum diverifikasi, untuk demo alur admin
            ("Nasi Ayam Geprek", MenuCategory.MAKANAN, 18000, 10, (590, 55, 38, 22, 1150, 4), MenuStatus.PENDING, False),
        ],
    ),
    (
        "busri@canteencare.id", "Warung Bu Sri", "Masakan Jawa dan lauk pauk", "07.00 - 15.00",
        [
            ("Nasi Pecel", MenuCategory.MAKANAN, 12000, 25, (480, 70, 14, 16, 800, 10), MenuStatus.APPROVED, True),
            ("Nasi Ayam Bakar", MenuCategory.MAKANAN, 18000, 20, (620, 75, 35, 18, 1100, 8), MenuStatus.APPROVED, True),
            ("Sayur Sop", MenuCategory.MAKANAN, 8000, 15, (150, 20, 5, 5, 700, 4), MenuStatus.APPROVED, True),
            ("Es Jeruk", MenuCategory.MINUMAN, 5000, 40, (110, 27, 1, 0, 5, 22), MenuStatus.APPROVED, True),
        ],
    ),
    (
        "segar@canteencare.id", "Kedai Segar", "Jus buah, minuman, dan camilan sehat", "08.00 - 17.00",
        [
            ("Jus Alpukat", MenuCategory.MINUMAN, 12000, 20, (250, 20, 3, 18, 15, 14), MenuStatus.APPROVED, True),
            ("Salad Buah", MenuCategory.MAKANAN, 15000, 15, (220, 40, 5, 5, 60, 30), MenuStatus.APPROVED, True),
            ("Air Mineral", MenuCategory.MINUMAN, 4000, 60, (0, 0, 0, 0, 0, 0), MenuStatus.APPROVED, True),
            ("Roti Gandum Telur", MenuCategory.MAKANAN, 14000, 15, (350, 35, 16, 15, 600, 5), MenuStatus.APPROVED, True),
        ],
    ),
]

SELLER_NAMES = {
    "gapura@canteencare.id": "Pak Gapura",
    "busri@canteencare.id": "Bu Sri",
    "segar@canteencare.id": "Mas Segar",
}


def _create_user(db: Session, full_name: str, email: str, password: str, role: Role) -> User:
    user = User(full_name=full_name, email=email, password_hash=hash_password(password), role=role)
    db.add(user)
    return user


def seed(db: Session) -> None:
    admin = _create_user(db, "Admin CanteenCare", "admin@canteencare.id", "admin123", Role.ADMIN)
    db.flush()  # supaya admin.id terisi untuk kolom verified_by

    menus_by_name: dict[str, Menu] = {}
    vendors_by_name: dict[str, Vendor] = {}
    for email, vendor_name, description, open_hours, menus in SELLERS:
        owner = _create_user(db, SELLER_NAMES[email], email, "penjual123", Role.PENJUAL)
        vendor = Vendor(owner=owner, name=vendor_name, description=description, open_hours=open_hours)
        db.add(vendor)
        vendors_by_name[vendor_name] = vendor

        for name, category, price, stock, values, status, verified in menus:
            calories, carbs, protein, fat, salt, sugar = values
            menu = Menu(vendor=vendor, name=name, category=category, price=price, stock=stock, status=status)
            menu.nutrition = Nutrition(
                calories=calories, carbs=carbs, protein=protein, fat=fat, salt=salt, sugar=sugar,
                verified_by=admin.id if verified else None,
            )
            db.add(menu)
            menus_by_name[name] = menu

    daffa = _create_user(db, "Daffa", "daffa@student.ub.ac.id", "pembeli123", Role.PEMBELI)
    daffa.nutrition_target = NutritionTarget()
    rafael = _create_user(db, "Rafael", "rafael@student.ub.ac.id", "pembeli123", Role.PEMBELI)
    rafael.nutrition_target = NutritionTarget(
        target_calories=2500, target_carbs=375, target_protein=65, target_fat=70, target_salt=2000, target_sugar=50
    )
    db.flush()

    # Contoh pesanan Daffa, tanggalnya relatif terhadap waktu seeding agar demo selalu punya data.
    # Ada satu pesanan aktif hari ini dan dua pesanan riwayat kemarin.
    now = utcnow()
    start_of_today, _ = wib_day_range_utc(wib_today())
    today_created = max(now - timedelta(minutes=30), start_of_today)
    _add_sample_order(
        db, daffa, vendors_by_name["Dapur Gapura"],
        items=[(menus_by_name["Mie Ayam Solo"], 1), (menus_by_name["Es Teh Manis"], 2)],
        payment_method=PaymentMethod.TUNAI, status=OrderStatus.SIAP_DIAMBIL,
        created_at=today_created,
    )
    yesterday_selesai = start_of_today - timedelta(days=1) + timedelta(hours=10)
    _add_sample_order(
        db, daffa, vendors_by_name["Dapur Gapura"],
        items=[(menus_by_name["Nasi Goreng Ayam"], 1)],
        payment_method=PaymentMethod.QRIS, status=OrderStatus.SELESAI,
        created_at=yesterday_selesai, resolved_at=yesterday_selesai + timedelta(minutes=20),
    )
    yesterday_ditolak = start_of_today - timedelta(days=1) + timedelta(hours=9)
    _add_sample_order(
        db, daffa, vendors_by_name["Warung Bu Sri"],
        items=[(menus_by_name["Nasi Ayam Bakar"], 1)],
        payment_method=PaymentMethod.QRIS, status=OrderStatus.DITOLAK,
        created_at=yesterday_ditolak, resolved_at=yesterday_ditolak + timedelta(minutes=5),
        rejection_reason="Bahan habis",
    )
    db.commit()


def _add_sample_order(db, buyer, vendor, items, payment_method, status, created_at, resolved_at=None,
                      rejection_reason=None) -> None:
    order = Order(
        order_number=generate_order_number(created_at),
        buyer=buyer, vendor=vendor, status=status, payment_method=payment_method,
        created_at=created_at, resolved_at=resolved_at, rejection_reason=rejection_reason,
        items=[snapshot_item(menu, quantity) for menu, quantity in items],
    )
    apply_totals(order)
    db.add(order)


def seed_if_empty(db: Session) -> None:
    if db.scalar(select(func.count()).select_from(User)) == 0:
        logger.info("Database kosong, mengisi data demo...")
        seed(db)


def reset_database() -> None:
    """Hapus semua tabel, buat ulang, lalu isi data demo."""
    Base.metadata.drop_all(engine)
    Base.metadata.create_all(engine)
    with SessionLocal() as db:
        seed(db)
