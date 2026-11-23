import pytest
from sqlalchemy import select

from app.db import SessionLocal
from app.models import NutritionTarget, Order, User
from app.models.enums import OrderStatus
from app.models.nutrition_target import DEFAULT_TARGETS
from tests.conftest import DAFFA, GAPURA, login

API = "/api/v1"


def test_register_creates_pembeli_with_default_target(client):
    response = client.post(
        f"{API}/auth/register",
        json={"full_name": "  Budi  ", "email": "Budi@Student.UB.ac.id", "password": "rahasia"},
    )

    assert response.status_code == 201
    body = response.json()
    assert body["token_type"] == "bearer"
    assert body["access_token"]
    assert body["user"]["role"] == "PEMBELI"
    assert body["user"]["email"] == "budi@student.ub.ac.id"  # disimpan lowercase
    assert body["user"]["full_name"] == "Budi"

    with SessionLocal() as db:
        target = db.scalar(select(NutritionTarget).where(NutritionTarget.user_id == body["user"]["id"]))
        assert target is not None
        for field, value in DEFAULT_TARGETS.items():
            assert getattr(target, field) == value


def test_register_duplicate_email_is_case_insensitive(client):
    response = client.post(
        f"{API}/auth/register",
        json={"full_name": "Daffa Lagi", "email": "DAFFA@student.ub.ac.id", "password": "rahasia"},
    )

    assert response.status_code == 409
    assert response.json() == {"detail": {"code": "EMAIL_EXISTS", "message": "Email sudah terdaftar."}}


def test_register_short_password_uses_standard_error_shape(client):
    response = client.post(f"{API}/auth/register", json={"full_name": "Budi", "email": "b@x.id", "password": "123"})

    assert response.status_code == 422
    detail = response.json()["detail"]
    assert detail["code"] == "VALIDATION_ERROR"
    assert detail["message"] == "Field password minimal 6 karakter."


def test_register_cannot_choose_role(client):
    response = client.post(
        f"{API}/auth/register",
        json={"full_name": "Hacker", "email": "h@x.id", "password": "rahasia", "role": "ADMIN"},
    )

    assert response.status_code == 201
    assert response.json()["user"]["role"] == "PEMBELI"


@pytest.mark.parametrize(
    ("email", "password", "role", "vendor_name"),
    [
        ("admin@canteencare.id", "admin123", "ADMIN", None),
        ("gapura@canteencare.id", "penjual123", "PENJUAL", "Dapur Gapura"),
        ("busri@canteencare.id", "penjual123", "PENJUAL", "Warung Bu Sri"),
        ("segar@canteencare.id", "penjual123", "PENJUAL", "Kedai Segar"),
        ("daffa@student.ub.ac.id", "pembeli123", "PEMBELI", None),
        ("rafael@student.ub.ac.id", "pembeli123", "PEMBELI", None),
    ],
)
def test_demo_accounts_can_login(client, email, password, role, vendor_name):
    response = client.post(f"{API}/auth/login", json={"email": email.upper(), "password": password})

    assert response.status_code == 200
    user = response.json()["user"]
    assert user["role"] == role
    assert user["vendor_name"] == vendor_name
    assert (user["vendor_id"] is not None) == (vendor_name is not None)


def test_wrong_password_and_unknown_email_give_same_error(client):
    wrong_password = client.post(f"{API}/auth/login", json={"email": "daffa@student.ub.ac.id", "password": "salah"})
    unknown_email = client.post(f"{API}/auth/login", json={"email": "siapa@x.id", "password": "pembeli123"})

    assert wrong_password.status_code == unknown_email.status_code == 401
    assert wrong_password.json() == unknown_email.json()
    assert wrong_password.json()["detail"]["code"] == "INVALID_CREDENTIALS"


def test_me_returns_current_user(client):
    headers = login(client, "gapura@canteencare.id", "penjual123")

    response = client.get(f"{API}/auth/me", headers=headers)

    assert response.status_code == 200
    assert response.json()["email"] == "gapura@canteencare.id"
    assert response.json()["vendor_name"] == "Dapur Gapura"


@pytest.mark.parametrize("headers", [{}, {"Authorization": "Bearer token-palsu"}])
def test_me_without_valid_token_is_401(client, headers):
    response = client.get(f"{API}/auth/me", headers=headers)

    assert response.status_code == 401
    assert response.json()["detail"]["code"] == "UNAUTHORIZED"


def test_fcm_token_is_saved(client):
    headers = login(client, "daffa@student.ub.ac.id", "pembeli123")

    response = client.put(f"{API}/users/me/fcm-token", headers=headers, json={"fcm_token": "abc123"})

    assert response.status_code == 200
    with SessionLocal() as db:
        user = db.scalar(select(User).where(User.email == "daffa@student.ub.ac.id"))
        assert user.fcm_token == "abc123"


def test_fcm_token_moves_to_latest_account_on_same_device(client):
    # HP yang sama dipakai login Daffa, lalu logout dan login sebagai Gapura
    client.put(f"{API}/users/me/fcm-token", headers=login(client, *DAFFA), json={"fcm_token": "hp-bersama"})
    client.put(f"{API}/users/me/fcm-token", headers=login(client, *GAPURA), json={"fcm_token": "hp-bersama"})

    assert _fcm_token("daffa@student.ub.ac.id") is None
    assert _fcm_token("gapura@canteencare.id") == "hp-bersama"


def _fcm_token(email: str) -> str | None:
    with SessionLocal() as db:
        return db.scalar(select(User.fcm_token).where(User.email == email))


def test_seed_creates_sample_orders_with_recomputed_totals(client):
    with SessionLocal() as db:
        orders = db.scalars(select(Order).order_by(Order.created_at)).all()

    assert [o.status for o in orders] == [
        OrderStatus.DITOLAK, OrderStatus.SELESAI, OrderStatus.SIAP_DIAMBIL
    ]
    ready = orders[2]
    # 1 Mie Ayam Solo + 2 Es Teh Manis
    assert ready.total_price == 20000 + 2 * 4000
    assert ready.total_calories == 650 + 2 * 120
    assert ready.total_sugar == 6 + 2 * 30
    assert ready.order_number.startswith("CC-")
    assert orders[0].rejection_reason == "Bahan habis"
