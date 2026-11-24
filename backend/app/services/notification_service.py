"""Pengiriman push notification lewat Firebase Cloud Messaging (FCM).

Jika file service-account Firebase tidak ada, service ini hanya mencatat peringatan
dan tidak mengirim apa pun (no-op), sehingga backend tetap bisa berjalan tanpa Firebase.
Kegagalan mengirim notifikasi TIDAK PERNAH membuat request API gagal.

Fungsi notify_* dipanggil sebagai background task setelah respons dikirim ke client,
jadi masing-masing membuka sesi database sendiri.
"""

import logging
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, exceptions, messaging

from app.core.config import get_settings
from app.db import SessionLocal
from app.models import Order, User

logger = logging.getLogger(__name__)

# Harus sama dengan id notification channel yang dibuat di aplikasi Android
ANDROID_CHANNEL_ID = "orders"
TYPE_NEW_ORDER = "NEW_ORDER"
TYPE_ORDER_STATUS = "ORDER_STATUS"

_firebase_app: firebase_admin.App | None = None
_initialized = False


def init_firebase() -> firebase_admin.App | None:
    """Inisialisasi Firebase satu kali saja. Mengembalikan None jika kredensial tidak ada."""
    global _firebase_app, _initialized
    if not _initialized:
        _initialized = True
        path = Path(get_settings().FIREBASE_CREDENTIALS)
        if not path.is_file():
            logger.warning("File kredensial Firebase '%s' tidak ditemukan. Notifikasi dinonaktifkan.", path)
        else:
            try:
                _firebase_app = firebase_admin.initialize_app(credentials.Certificate(path), name="canteencare")
                logger.info("Firebase siap, notifikasi aktif.")
            except (ValueError, OSError):
                logger.exception("Gagal membaca kredensial Firebase. Notifikasi dinonaktifkan.")
    return _firebase_app


def _send(user_id: int, title: str, body: str, data: dict[str, str]) -> None:
    app = init_firebase()
    if app is None:
        return

    with SessionLocal() as db:
        user = db.get(User, user_id)
        if user is None or not user.fcm_token:
            return  # user belum login di HP mana pun

        message = messaging.Message(
            # firebase-admin 7.8 menandai `token` sebagai usang (diganti `fid`), tapi masih berfungsi.
            # Kita tetap memakai token FCM biasa karena itu yang dikirim aplikasi Android.
            token=user.fcm_token,
            notification=messaging.Notification(title=title, body=body),
            data=data,  # dipakai aplikasi untuk membuka layar detail pesanan saat notifikasi diketuk
            android=messaging.AndroidConfig(
                priority="high",
                notification=messaging.AndroidNotification(channel_id=ANDROID_CHANNEL_ID),
            ),
        )
        try:
            messaging.send(message, app=app)
        except (messaging.UnregisteredError, messaging.SenderIdMismatchError, exceptions.InvalidArgumentError):
            # Token sudah tidak berlaku (aplikasi dihapus, token kedaluwarsa, dll), jadi dihapus
            logger.info("Token FCM user %s tidak valid, dihapus.", user_id)
            user.fcm_token = None
            db.commit()
        except Exception:
            # Apa pun error-nya, cukup dicatat. Pesanan tetap berhasil diproses.
            logger.exception("Gagal mengirim notifikasi ke user %s.", user_id)


def notify_new_order(order_id: int) -> None:
    """Pesanan baru → penjual pemilik stan."""
    with SessionLocal() as db:
        order = db.get(Order, order_id)
        if order is None:
            return
        target_id = order.vendor.owner_id
        body = f"Pesanan baru {order.order_number} dari {order.buyer.full_name}"
    _send(target_id, "Pesanan Baru", body, {"type": TYPE_NEW_ORDER, "order_id": str(order_id)})


def notify_order_confirmed(order_id: int) -> None:
    """Pesanan diterima penjual dan mulai dimasak → pembeli."""
    with SessionLocal() as db:
        order = db.get(Order, order_id)
        if order is None:
            return
        target_id = order.buyer_id
        body = f"Pesanan {order.order_number} diterima dan sedang dimasak di {order.vendor.name}."
    _send(target_id, "Pesanan Sedang Dimasak", body, {"type": TYPE_ORDER_STATUS, "order_id": str(order_id)})


def notify_order_ready(order_id: int) -> None:
    """Pesanan selesai dimasak dan siap diambil → pembeli."""
    with SessionLocal() as db:
        order = db.get(Order, order_id)
        if order is None:
            return
        target_id = order.buyer_id
        body = f"Pesanan {order.order_number} siap diambil di {order.vendor.name}."
    _send(target_id, "Pesanan Siap Diambil", body, {"type": TYPE_ORDER_STATUS, "order_id": str(order_id)})


def notify_order_completed(order_id: int) -> None:
    """Pesanan sudah diambil dan transaksi selesai → pembeli."""
    with SessionLocal() as db:
        order = db.get(Order, order_id)
        if order is None:
            return
        target_id = order.buyer_id
        body = f"Pesanan {order.order_number} selesai. Terima kasih!"
    _send(target_id, "Pesanan Selesai", body, {"type": TYPE_ORDER_STATUS, "order_id": str(order_id)})


def notify_order_rejected(order_id: int) -> None:
    """Pesanan ditolak → pembeli, beserta alasannya."""
    with SessionLocal() as db:
        order = db.get(Order, order_id)
        if order is None:
            return
        target_id = order.buyer_id
        body = f"Pesanan {order.order_number} ditolak: {order.rejection_reason}"
    _send(target_id, "Pesanan Ditolak", body, {"type": TYPE_ORDER_STATUS, "order_id": str(order_id)})
