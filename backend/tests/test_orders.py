from datetime import datetime, timedelta, timezone

import pytest

from app.db import SessionLocal
from app.models import Order
from tests.conftest import ADMIN, BUSRI, DAFFA, GAPURA, RAFAEL, login

API = "/api/v1"
DAPUR_GAPURA, WARUNG_BU_SRI = 1, 2
MIE_AYAM, TAHU_TELUR, ES_TEH, GEPREK_PENDING, NASI_PECEL = 1, 2, 4, 5, 6
SEED_TODAY_ORDER = 1
SEED_YESTERDAY_COMPLETED_ORDER = 2
SEED_YESTERDAY_ORDER = 3


def _order_body(items, vendor_id=DAPUR_GAPURA, **extra):
    return {"vendor_id": vendor_id, "payment_method": "TUNAI",
            "items": [{"menu_id": m, "quantity": q} for m, q in items], **extra}


def _menu_stock(client, headers, menu_id):
    return client.get(f"{API}/menus/{menu_id}", headers=headers).json()["stock"]


# ---------- Membuat pesanan ----------

def test_create_order_recomputes_totals_from_database(client):
    headers = login(client, *DAFFA)
    # Total palsu dari client harus diabaikan
    body = _order_body([(MIE_AYAM, 2), (ES_TEH, 3)], note="Pedas", total_price=1)

    response = client.post(f"{API}/orders", headers=headers, json=body)

    assert response.status_code == 201
    order = response.json()
    assert order["status"] == "MENUNGGU"
    assert order["total_price"] == 2 * 20000 + 3 * 4000
    assert order["total_nutrition"] == {
        "calories": 2 * 650 + 3 * 120, "carbs": 2 * 85 + 3 * 30, "protein": 60,
        "fat": 50, "salt": 2 * 1800 + 3 * 10, "sugar": 2 * 6 + 3 * 30,
    }
    assert order["items"][0]["subtotal_price"] == 40000
    assert order["items"][1]["nutrition_per_portion"]["sugar"] == 30
    assert order["note"] == "Pedas"
    assert order["buyer_name"] == "Daffa" and order["vendor_name"] == "Dapur Gapura"
    assert order["order_number"].startswith("CC-") and len(order["order_number"]) == 16
    assert order["created_at"].endswith("+07:00")
    # Stok BELUM berkurang sebelum dikonfirmasi penjual
    assert _menu_stock(client, headers, MIE_AYAM) == 25


@pytest.mark.parametrize(
    ("items", "vendor_id", "status", "code"),
    [
        ([], DAPUR_GAPURA, 400, "EMPTY_ITEMS"),
        ([(MIE_AYAM, 1), (MIE_AYAM, 2)], DAPUR_GAPURA, 400, "DUPLICATE_MENU"),
        ([(MIE_AYAM, 0)], DAPUR_GAPURA, 422, "VALIDATION_ERROR"),
        ([(NASI_PECEL, 1)], DAPUR_GAPURA, 400, "MENU_NOT_AVAILABLE"),  # menu stan lain
        ([(GEPREK_PENDING, 1)], DAPUR_GAPURA, 400, "MENU_NOT_AVAILABLE"),  # belum APPROVED
        ([(999, 1)], DAPUR_GAPURA, 400, "MENU_NOT_AVAILABLE"),
        ([(MIE_AYAM, 1)], 999, 404, "NOT_FOUND"),
    ],
)
def test_create_order_validation(client, items, vendor_id, status, code):
    response = client.post(f"{API}/orders", headers=login(client, *DAFFA), json=_order_body(items, vendor_id))

    assert response.status_code == status
    assert response.json()["detail"]["code"] == code


def test_create_order_insufficient_stock_names_menu(client):
    response = client.post(f"{API}/orders", headers=login(client, *DAFFA), json=_order_body([(MIE_AYAM, 26)]))

    assert response.status_code == 409
    assert response.json()["detail"] == {
        "code": "INSUFFICIENT_STOCK", "message": "Stok Mie Ayam Solo tidak mencukupi, tersisa 25."
    }


def test_only_buyer_can_create_order(client):
    response = client.post(f"{API}/orders", headers=login(client, *GAPURA), json=_order_body([(MIE_AYAM, 1)]))

    assert response.status_code == 403
    assert response.json()["detail"]["code"] == "FORBIDDEN_ROLE"


# ---------- Melihat pesanan ----------

def test_my_orders_newest_first_with_status_filter(client):
    headers = login(client, *DAFFA)

    all_orders = client.get(f"{API}/orders/me", headers=headers).json()
    rejected = client.get(f"{API}/orders/me", headers=headers, params={"status": "DITOLAK"}).json()

    assert [o["id"] for o in all_orders] == [
        SEED_TODAY_ORDER, SEED_YESTERDAY_COMPLETED_ORDER, SEED_YESTERDAY_ORDER
    ]
    assert [o["rejection_reason"] for o in rejected] == ["Bahan habis"]
    assert client.get(f"{API}/orders/me", headers=login(client, *RAFAEL)).json() == []


def test_date_filter_uses_wib(client):
    with SessionLocal() as db:
        order = db.get(Order, SEED_YESTERDAY_ORDER)
        # 17 Nov 2026 23:30 UTC = 18 Nov 2026 06:30 WIB, jadi harus terhitung tanggal 18
        order.created_at = datetime(2026, 11, 17, 23, 30, tzinfo=timezone.utc)
        db.commit()
    headers = login(client, *DAFFA)

    on_17 = client.get(f"{API}/orders/me", headers=headers, params={"date": "2026-11-17"}).json()
    on_18 = client.get(f"{API}/orders/me", headers=headers, params={"date": "2026-11-18"}).json()

    assert SEED_YESTERDAY_ORDER not in [o["id"] for o in on_17]
    assert SEED_YESTERDAY_ORDER in [o["id"] for o in on_18]
    assert on_18[-1]["created_at"] == "2026-11-18T06:30:00+07:00"


def test_today_filter_returns_seed_order(client):
    headers = login(client, *DAFFA)
    today_wib = (datetime.now(timezone.utc) + timedelta(hours=7)).date().isoformat()

    today = client.get(f"{API}/orders/me", headers=headers, params={"date": today_wib}).json()

    assert [o["id"] for o in today] == [SEED_TODAY_ORDER]


def test_order_detail_visibility(client):
    url = f"{API}/orders/{SEED_TODAY_ORDER}"  # pesanan Daffa di Dapur Gapura

    assert client.get(url, headers=login(client, *DAFFA)).status_code == 200
    assert client.get(url, headers=login(client, *GAPURA)).status_code == 200
    assert client.get(url, headers=login(client, *ADMIN)).status_code == 200
    # Pembeli lain dan penjual lain mendapat 404, bukan 403
    assert client.get(url, headers=login(client, *RAFAEL)).status_code == 404
    assert client.get(url, headers=login(client, *BUSRI)).status_code == 404


# ---------- Konfirmasi dan tolak ----------

def _place_order(client, items, buyer=DAFFA):
    response = client.post(f"{API}/orders", headers=login(client, *buyer), json=_order_body(items))
    assert response.status_code == 201, response.text
    return response.json()["id"]


def test_confirm_starts_cooking_and_deducts_stock(client):
    order_id = _place_order(client, [(MIE_AYAM, 3), (ES_TEH, 2)])
    seller = login(client, *GAPURA)

    response = client.post(f"{API}/seller/orders/{order_id}/confirm", headers=seller)

    assert response.status_code == 200
    assert response.json()["status"] == "SEDANG_DIMASAK"
    assert response.json()["resolved_at"] is None
    assert _menu_stock(client, seller, MIE_AYAM) == 22
    assert _menu_stock(client, seller, ES_TEH) == 48

    again = client.post(f"{API}/seller/orders/{order_id}/confirm", headers=seller)
    reject_after = client.post(f"{API}/seller/orders/{order_id}/reject", headers=seller, json={"reason": "x"})
    assert again.status_code == reject_after.status_code == 409
    assert again.json()["detail"]["code"] == "INVALID_ORDER_STATE"


def test_order_flow_from_cooking_to_ready_to_complete(client):
    order_id = _place_order(client, [(MIE_AYAM, 1)])
    seller = login(client, *GAPURA)
    assert client.post(f"{API}/seller/orders/{order_id}/confirm", headers=seller).status_code == 200

    ready = client.post(f"{API}/seller/orders/{order_id}/ready", headers=seller)
    complete = client.post(f"{API}/seller/orders/{order_id}/complete", headers=seller)

    assert ready.status_code == 200
    assert ready.json()["status"] == "SIAP_DIAMBIL"
    assert ready.json()["resolved_at"] is None
    assert complete.status_code == 200
    assert complete.json()["status"] == "SELESAI"
    assert complete.json()["resolved_at"] is not None
    # Pesanan yang sudah selesai tidak bisa diproses lagi
    assert client.post(f"{API}/seller/orders/{order_id}/ready", headers=seller).status_code == 409
    assert client.post(f"{API}/seller/orders/{order_id}/complete", headers=seller).status_code == 409


def test_ready_and_complete_require_previous_state(client):
    order_id = _place_order(client, [(MIE_AYAM, 1)])
    seller = login(client, *GAPURA)

    assert client.post(f"{API}/seller/orders/{order_id}/ready", headers=seller).status_code == 409
    assert client.post(f"{API}/seller/orders/{order_id}/complete", headers=seller).status_code == 409


def test_reject_keeps_stock(client):
    order_id = _place_order(client, [(MIE_AYAM, 3)])
    seller = login(client, *GAPURA)

    response = client.post(f"{API}/seller/orders/{order_id}/reject", headers=seller, json={"reason": "Tutup lebih awal"})

    assert response.status_code == 200
    assert response.json()["status"] == "DITOLAK"
    assert response.json()["rejection_reason"] == "Tutup lebih awal"
    assert _menu_stock(client, seller, MIE_AYAM) == 25
    assert client.post(f"{API}/seller/orders/{order_id}/confirm", headers=seller).status_code == 409


def test_confirm_with_insufficient_stock_changes_nothing(client):
    # Dua pesanan yang masing-masing valid, tapi totalnya melebihi stok Tahu Telur (20)
    first = _place_order(client, [(MIE_AYAM, 1), (TAHU_TELUR, 15)])
    second = _place_order(client, [(MIE_AYAM, 1), (TAHU_TELUR, 10)], buyer=RAFAEL)
    seller = login(client, *GAPURA)
    assert client.post(f"{API}/seller/orders/{first}/confirm", headers=seller).status_code == 200

    response = client.post(f"{API}/seller/orders/{second}/confirm", headers=seller)

    assert response.status_code == 409
    assert response.json()["detail"]["message"] == "Stok Tahu Telur tidak mencukupi, tersisa 5."
    # Stok Mie Ayam tidak ikut berkurang untuk pesanan kedua, dan status tetap MENUNGGU
    assert _menu_stock(client, seller, MIE_AYAM) == 24
    assert client.get(f"{API}/orders/{second}", headers=seller).json()["status"] == "MENUNGGU"


def test_seller_cannot_touch_other_stall_orders(client):
    order_id = _place_order(client, [(MIE_AYAM, 1)])
    other = login(client, *BUSRI)

    assert client.post(f"{API}/seller/orders/{order_id}/confirm", headers=other).status_code == 404
    assert client.post(f"{API}/seller/orders/{order_id}/reject", headers=other, json={"reason": "x"}).status_code == 404
    assert client.post(f"{API}/seller/orders/{order_id}/ready", headers=other).status_code == 404
    assert client.post(f"{API}/seller/orders/{order_id}/complete", headers=other).status_code == 404
    assert order_id not in [o["id"] for o in client.get(f"{API}/seller/orders", headers=other).json()]


def test_seller_orders_status_filter(client):
    order_id = _place_order(client, [(MIE_AYAM, 1)])
    seller = login(client, *GAPURA)

    waiting = client.get(f"{API}/seller/orders", headers=seller, params={"status": "MENUNGGU"}).json()
    assert [o["id"] for o in waiting] == [order_id]

    client.post(f"{API}/seller/orders/{order_id}/confirm", headers=seller)
    cooking = client.get(f"{API}/seller/orders", headers=seller, params={"status": "SEDANG_DIMASAK"}).json()
    assert [o["id"] for o in cooking] == [order_id]


def test_buyer_cannot_use_seller_endpoints(client):
    response = client.get(f"{API}/seller/orders", headers=login(client, *DAFFA))

    assert response.status_code == 403
    assert response.json()["detail"]["code"] == "FORBIDDEN_ROLE"
