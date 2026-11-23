import pytest
from firebase_admin import messaging
from sqlalchemy import select

from app.db import SessionLocal
from app.models import User
from app.services import notification_service
from tests.conftest import DAFFA, GAPURA, login

API = "/api/v1"
ORDER_BODY = {"vendor_id": 1, "payment_method": "TUNAI", "items": [{"menu_id": 1, "quantity": 1}]}


@pytest.fixture
def sent(monkeypatch):
    """Memalsukan Firebase: pesan yang 'terkirim' dicatat di list ini, bukan dikirim sungguhan."""
    messages: list[messaging.Message] = []
    monkeypatch.setattr(notification_service, "init_firebase", lambda: object())
    monkeypatch.setattr(messaging, "send", lambda message, app=None: messages.append(message))
    return messages


def _register_tokens(client):
    buyer, seller = login(client, *DAFFA), login(client, *GAPURA)
    client.put(f"{API}/users/me/fcm-token", headers=buyer, json={"fcm_token": "token-daffa"})
    client.put(f"{API}/users/me/fcm-token", headers=seller, json={"fcm_token": "token-gapura"})
    return buyer, seller


def _token_of(email: str) -> str | None:
    with SessionLocal() as db:
        return db.scalar(select(User.fcm_token).where(User.email == email))


def test_new_order_notifies_seller(client, sent):
    buyer, _ = _register_tokens(client)

    order = client.post(f"{API}/orders", headers=buyer, json=ORDER_BODY).json()

    assert len(sent) == 1
    message = sent[0]
    assert message.token == "token-gapura"
    assert message.notification.body == f"Pesanan baru {order['order_number']} dari Daffa"
    assert message.data == {"type": "NEW_ORDER", "order_id": str(order["id"])}
    assert message.android.notification.channel_id == "orders"


def test_confirm_notifies_buyer(client, sent):
    buyer, seller = _register_tokens(client)
    order = client.post(f"{API}/orders", headers=buyer, json=ORDER_BODY).json()

    client.post(f"{API}/seller/orders/{order['id']}/confirm", headers=seller)

    message = sent[-1]
    assert message.token == "token-daffa"
    assert message.notification.body == (
        f"Pesanan {order['order_number']} diterima dan sedang dimasak di Dapur Gapura."
    )
    assert message.data == {"type": "ORDER_STATUS", "order_id": str(order["id"])}


def test_ready_and_complete_notify_buyer(client, sent):
    buyer, seller = _register_tokens(client)
    order = client.post(f"{API}/orders", headers=buyer, json=ORDER_BODY).json()
    client.post(f"{API}/seller/orders/{order['id']}/confirm", headers=seller)

    client.post(f"{API}/seller/orders/{order['id']}/ready", headers=seller)
    ready_message = sent[-1]
    client.post(f"{API}/seller/orders/{order['id']}/complete", headers=seller)
    complete_message = sent[-1]

    assert ready_message.notification.body == (
        f"Pesanan {order['order_number']} siap diambil di Dapur Gapura."
    )
    assert complete_message.notification.body == (
        f"Pesanan {order['order_number']} selesai. Terima kasih!"
    )
    assert ready_message.data == {"type": "ORDER_STATUS", "order_id": str(order["id"])}
    assert complete_message.data == {"type": "ORDER_STATUS", "order_id": str(order["id"])}


def test_reject_notifies_buyer_with_reason(client, sent):
    buyer, seller = _register_tokens(client)
    order = client.post(f"{API}/orders", headers=buyer, json=ORDER_BODY).json()

    client.post(f"{API}/seller/orders/{order['id']}/reject", headers=seller, json={"reason": "Bahan habis"})

    assert sent[-1].notification.body == f"Pesanan {order['order_number']} ditolak: Bahan habis"


def test_user_without_token_gets_nothing(client, sent):
    client.post(f"{API}/orders", headers=login(client, *DAFFA), json=ORDER_BODY)

    assert sent == []  # Gapura belum pernah mengirim token FCM


def test_invalid_token_is_cleared(client, monkeypatch):
    buyer, _ = _register_tokens(client)

    def reject_token(message, app=None):
        raise messaging.UnregisteredError("Token tidak terdaftar")

    monkeypatch.setattr(notification_service, "init_firebase", lambda: object())
    monkeypatch.setattr(messaging, "send", reject_token)

    response = client.post(f"{API}/orders", headers=buyer, json=ORDER_BODY)

    assert response.status_code == 201
    assert _token_of(GAPURA[0]) is None


def test_send_failure_never_breaks_request(client, monkeypatch):
    buyer, seller = _register_tokens(client)

    def boom(message, app=None):
        raise RuntimeError("Server FCM sedang down")

    monkeypatch.setattr(notification_service, "init_firebase", lambda: object())
    monkeypatch.setattr(messaging, "send", boom)

    created = client.post(f"{API}/orders", headers=buyer, json=ORDER_BODY)
    confirmed = client.post(f"{API}/seller/orders/{created.json()['id']}/confirm", headers=seller)

    assert created.status_code == 201
    assert confirmed.status_code == 200
    assert _token_of(GAPURA[0]) == "token-gapura"  # error biasa tidak menghapus token


def test_without_credentials_backend_still_works(client):
    # Tanpa fixture `sent`, Firebase benar-benar tidak dikonfigurasi (no-op)
    buyer, seller = _register_tokens(client)

    created = client.post(f"{API}/orders", headers=buyer, json=ORDER_BODY)
    confirmed = client.post(f"{API}/seller/orders/{created.json()['id']}/confirm", headers=seller)

    assert created.status_code == 201
    assert confirmed.status_code == 200
    assert notification_service.init_firebase() is None
