import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text

from app import models  # noqa: F401  (memastikan semua tabel terdaftar di Base.metadata)
from app.core.errors import register_error_handlers
from app.db import Base, SessionLocal, engine
from app.routers import admin, auth, catalog, nutrition_target, orders, seller
from app.seed import seed_if_empty
from app.services import notification_service

API_PREFIX = "/api/v1"

logging.basicConfig(level=logging.INFO)


def migrate_legacy_order_statuses() -> None:
    """Ubah data lama DIKONFIRMASI menjadi SEDANG_DIMASAK.

    Status DIKONFIRMASI dipakai sebelum alur Dalam Proses diperkenalkan. Migrasi ini
    menjaga database demo lama tetap bisa dibuka tanpa perlu reset manual.
    """
    with engine.begin() as connection:
        connection.execute(
            text(
                "UPDATE orders SET status = 'SEDANG_DIMASAK', resolved_at = NULL "
                "WHERE status = 'DIKONFIRMASI'"
            )
        )


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    # Saat server menyala: buat tabel jika belum ada, migrasi status lama, lalu isi data demo.
    Base.metadata.create_all(engine)
    migrate_legacy_order_statuses()
    with SessionLocal() as db:
        seed_if_empty(db)
    # Cek kredensial Firebase sejak awal supaya peringatannya langsung terlihat di log
    notification_service.init_firebase()
    yield


def create_app() -> FastAPI:
    app = FastAPI(
        title="CanteenCare API",
        description="Backend aplikasi pemesanan makanan kantin dengan pelacakan gizi. Kelompok 10 PAPB 2026.",
        version="1.0.0",
        lifespan=lifespan,
    )
    # Untuk pengembangan lokal semua origin diizinkan
    app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
    register_error_handlers(app)
    routers = (
        auth.router, catalog.router, orders.router, nutrition_target.router,
        seller.router, admin.router, admin.dev_router,
    )
    for router in routers:
        app.include_router(router, prefix=API_PREFIX)
    return app


app = create_app()
