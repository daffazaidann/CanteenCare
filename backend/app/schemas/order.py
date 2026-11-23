from pydantic import BaseModel, ConfigDict, Field

from app.models import Order
from app.models.enums import OrderStatus, PaymentMethod
from app.schemas.common import NutritionValues, WibDatetime
from app.services.order_service import NUTRIENT_FIELDS


class OrderItemIn(BaseModel):
    menu_id: int
    quantity: int = Field(ge=1)


class OrderCreate(BaseModel):
    model_config = ConfigDict(
        json_schema_extra={
            "examples": [{
                "vendor_id": 1, "payment_method": "TUNAI", "note": "Sambalnya dipisah ya",
                "items": [{"menu_id": 1, "quantity": 1}, {"menu_id": 4, "quantity": 2}],
            }]
        }
    )

    vendor_id: int
    payment_method: PaymentMethod
    note: str | None = Field(default=None, max_length=500)
    items: list[OrderItemIn]


class RejectRequest(BaseModel):
    model_config = ConfigDict(json_schema_extra={"examples": [{"reason": "Bahan habis"}]})

    reason: str = Field(min_length=1, max_length=500)


class OrderItemOut(BaseModel):
    menu_id: int
    menu_name: str
    unit_price: int
    quantity: int
    subtotal_price: int
    nutrition_per_portion: NutritionValues


class OrderOut(BaseModel):
    id: int
    order_number: str
    buyer_id: int
    buyer_name: str
    vendor_id: int
    vendor_name: str
    status: OrderStatus
    payment_method: PaymentMethod
    total_price: int
    total_nutrition: NutritionValues
    note: str | None
    rejection_reason: str | None
    created_at: WibDatetime
    resolved_at: WibDatetime | None
    items: list[OrderItemOut]

    @classmethod
    def from_order(cls, order: Order) -> "OrderOut":
        # DIKONFIRMASI adalah status lama sebelum alur Dalam Proses. Data lama tetap
        # ditampilkan sebagai SEDANG_DIMASAK supaya aplikasi tidak error saat masa transisi.
        status = OrderStatus.SEDANG_DIMASAK if order.status == "DIKONFIRMASI" else OrderStatus(order.status)
        return cls(
            id=order.id, order_number=order.order_number,
            buyer_id=order.buyer_id, buyer_name=order.buyer.full_name,
            vendor_id=order.vendor_id, vendor_name=order.vendor.name,
            status=status, payment_method=PaymentMethod(order.payment_method),
            total_price=order.total_price,
            total_nutrition=NutritionValues(**{f: getattr(order, f"total_{f}") for f in NUTRIENT_FIELDS}),
            note=order.note, rejection_reason=order.rejection_reason,
            created_at=order.created_at, resolved_at=order.resolved_at,
            items=[
                OrderItemOut(
                    menu_id=item.menu_id, menu_name=item.menu_name, unit_price=item.unit_price,
                    quantity=item.quantity, subtotal_price=item.unit_price * item.quantity,
                    nutrition_per_portion=NutritionValues(**{f: getattr(item, f) for f in NUTRIENT_FIELDS}),
                )
                for item in order.items
            ],
        )
