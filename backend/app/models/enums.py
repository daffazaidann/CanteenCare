from enum import StrEnum


class Role(StrEnum):
    PEMBELI = "PEMBELI"
    PENJUAL = "PENJUAL"
    ADMIN = "ADMIN"


class MenuCategory(StrEnum):
    MAKANAN = "MAKANAN"
    MINUMAN = "MINUMAN"


class MenuStatus(StrEnum):
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    ARCHIVED = "ARCHIVED"


class OrderStatus(StrEnum):
    MENUNGGU = "MENUNGGU"
    SEDANG_DIMASAK = "SEDANG_DIMASAK"
    SIAP_DIAMBIL = "SIAP_DIAMBIL"
    SELESAI = "SELESAI"
    DITOLAK = "DITOLAK"


class PaymentMethod(StrEnum):
    TUNAI = "TUNAI"
    QRIS = "QRIS"
