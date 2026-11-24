# CanteenCare API

Base path: **`/api/v1`**. Semua endpoint kecuali register dan login membutuhkan header `Authorization: Bearer <token>`.
Dokumentasi interaktif tersedia di `/docs` (Swagger UI) saat backend berjalan.

JSON memakai **snake_case**. Datetime dalam format ISO 8601 dengan offset zona waktu. Filter tanggal memakai zona waktu **Asia/Jakarta (WIB)**.

## Format Error

Semua error memakai bentuk yang sama:

```json
{ "detail": { "code": "INSUFFICIENT_STOCK", "message": "Stok Mie Ayam Solo tidak mencukupi, tersisa 2." } }
```

| HTTP | Kapan | Contoh `code` |
|---|---|---|
| 400 | Request tidak valid secara logika | `EMPTY_ITEMS`, `DUPLICATE_MENU`, `MENU_NOT_AVAILABLE` |
| 401 | Token tidak ada atau salah, login gagal | `UNAUTHORIZED`, `INVALID_CREDENTIALS` |
| 403 | Role tidak sesuai | `FORBIDDEN_ROLE` |
| 404 | Data tidak ada atau milik user lain | `NOT_FOUND` |
| 409 | Konflik state atau data | `EMAIL_EXISTS`, `INSUFFICIENT_STOCK`, `INVALID_ORDER_STATE`, `NUTRITION_NOT_VERIFIED`, `INVALID_MENU_STATE` |
| 422 | Validasi field gagal | `VALIDATION_ERROR` |

## Auth

| Method | Path | Role | Keterangan |
|---|---|---|---|
| POST | `/auth/register` | publik | Daftar sebagai **PEMBELI**. Body `full_name`, `email`, `password` (min 6). Membuat target gizi default. Respons `{access_token, token_type, user}` |
| POST | `/auth/login` | publik | Body `email`, `password`. Respons `{access_token, token_type, user}` |
| GET | `/auth/me` | semua | User saat ini |
| PUT | `/users/me/fcm-token` | semua | Body `fcm_token`. Menyimpan token perangkat |

Objek `user`:
```json
{ "id": 5, "full_name": "Daffa", "email": "daffa@student.ub.ac.id", "role": "PEMBELI", "vendor_id": null, "vendor_name": null }
```

## Katalog

| Method | Path | Role | Keterangan |
|---|---|---|---|
| GET | `/vendors` | semua | Stan aktif, opsional `?search=` berdasarkan nama |
| GET | `/vendors/{id}` | semua | Detail stan |
| GET | `/vendors/{id}/menus` | semua | Menu APPROVED milik stan, opsional `?category=MAKANAN\|MINUMAN` |
| GET | `/menus` | semua | Cari menu APPROVED di semua stan aktif, `?search=` mencocokkan nama menu |
| GET | `/menus/{id}` | semua | Pembeli dapat 404 untuk menu non-APPROVED; penjual bisa melihat menu miliknya; admin melihat semua |

Respons menu:
```json
{
  "id": 1, "vendor_id": 1, "vendor_name": "Dapur Gapura",
  "name": "Mie Ayam Solo", "description": "...", "category": "MAKANAN",
  "price": 20000, "stock": 25, "image_url": null, "status": "APPROVED",
  "nutrition": { "calories": 650, "carbs": 85, "protein": 30, "fat": 25, "salt": 1800, "sugar": 6, "is_verified": true }
}
```

## Pesanan — PEMBELI

| Method | Path | Keterangan |
|---|---|---|
| POST | `/orders` | Body `{ vendor_id, payment_method, note?, items: [{ menu_id, quantity }] }`. Total dihitung ulang dari database |
| GET | `/orders/me` | Pesanan sendiri, terbaru dulu. Filter `?status=` dan `?date=YYYY-MM-DD` (tanggal WIB dari `created_at`) |
| GET | `/orders/{id}` | Hanya pembeli pemilik, penjual stan terkait, atau admin. Selain itu 404 |

Respons pesanan:
```json
{
  "id": 1, "order_number": "CC-20261117-A3F9",
  "buyer_id": 5, "buyer_name": "Daffa", "vendor_id": 1, "vendor_name": "Dapur Gapura",
  "status": "MENUNGGU", "payment_method": "TUNAI", "total_price": 28000,
  "total_nutrition": { "calories": 890, "carbs": 145, "protein": 30, "fat": 25, "salt": 1820, "sugar": 66 },
  "note": null, "rejection_reason": null,
  "created_at": "2026-11-17T12:00:00+07:00", "resolved_at": null,
  "items": [
    { "menu_id": 1, "menu_name": "Mie Ayam Solo", "unit_price": 20000, "quantity": 1, "subtotal_price": 20000,
      "nutrition_per_portion": { "calories": 650, "carbs": 85, "protein": 30, "fat": 25, "salt": 1800, "sugar": 6 } }
  ]
}
```

## Target Gizi — PEMBELI

| Method | Path | Keterangan |
|---|---|---|
| GET | `/nutrition-target` | Target milik sendiri |
| PUT | `/nutrition-target` | Ubah keenam nilai, masing-masing > 0 |

Body/respons: `target_calories, target_carbs, target_protein, target_fat, target_salt, target_sugar`.

## Penjual — PENJUAL (selalu terbatas pada stan sendiri)

| Method | Path | Keterangan |
|---|---|---|
| GET | `/seller/orders` | Opsional `?status=`, terbaru dulu |
| POST | `/seller/orders/{id}/confirm` | Hanya dari MENUNGGU. Cek ulang dan kurangi stok secara atomik. Jadi SEDANG_DIMASAK. Kirim FCM ke pembeli |
| POST | `/seller/orders/{id}/ready` | Hanya dari SEDANG_DIMASAK. Jadi SIAP_DIAMBIL. Kirim FCM ke pembeli |
| POST | `/seller/orders/{id}/complete` | Hanya dari SIAP_DIAMBIL. Jadi SELESAI dan `resolved_at` terisi. Kirim FCM ke pembeli |
| POST | `/seller/orders/{id}/reject` | Body `reason`. Hanya dari MENUNGGU. Jadi DITOLAK dan `resolved_at` terisi, stok tidak berubah. Kirim FCM ke pembeli |
| GET | `/seller/menus` | Semua menu milik sendiri, semua status |
| POST | `/seller/menus` | Ajukan menu baru dengan estimasi gizi. Body `name, description?, category, price, stock, image_url?, nutrition{6 nilai}`. Status PENDING |
| PATCH | `/seller/menus/{id}/stock` | Body `stock` ≥ 0 |
| DELETE | `/seller/menus/{id}` | Arsipkan: status ARCHIVED, tidak pernah dihapus permanen |

## Admin

| Method | Path | Keterangan |
|---|---|---|
| POST | `/admin/sellers` | Buat akun PENJUAL beserta stannya. Body `full_name, email, password, vendor_name, vendor_description?, vendor_image_url?, open_hours?` |
| GET | `/admin/sellers` | Daftar penjual beserta stan |
| GET | `/admin/menus` | Semua menu, opsional `?status=` |
| PUT | `/admin/menus/{id}` | Ubah name, description, category, price, image_url |
| PUT | `/admin/menus/{id}/nutrition` | Isi enam nilai gizi, `verified_by` = admin |
| POST | `/admin/menus/{id}/approve` | Butuh gizi terverifikasi, jika belum 409 `NUTRITION_NOT_VERIFIED`. Jadi APPROVED |
| POST | `/admin/menus/{id}/reject` | Jadi REJECTED |

Menu berstatus ARCHIVED tidak bisa disetujui atau ditolak (409 `INVALID_MENU_STATE`).

## Upload Gambar — PENJUAL dan ADMIN

| Method | Path | Keterangan |
|---|---|---|
| POST | `/uploads/menu-image` | `multipart/form-data` dengan bagian `file`. PNG, JPG, atau WEBP, maksimal 2 MB. Respons 201 `{ "image_url": "/uploads/menus/<acak>.png" }`. Jenis file dicek dari isinya: 400 `INVALID_IMAGE` atau `IMAGE_TOO_LARGE` |
| GET | `/uploads/menus/{file}` | Di luar `/api/v1`, tanpa token. Menyajikan gambar yang sudah di-upload |

Alur pemakaian: upload dulu, lalu simpan `image_url` dari respons ke body `POST /seller/menus` atau `PUT /admin/menus/{id}`. Path disimpan relatif, dan aplikasi Android melengkapinya dengan alamat server (`ImageUrl.resolve`).

## Dev

| Method | Path | Keterangan |
|---|---|---|
| POST | `/admin/dev/reset-demo` | **ADMIN saja.** Hapus, buat ulang, dan isi ulang database untuk demo |

## Alur Status Pesanan

```
MENUNGGU ──confirm──▶ SEDANG_DIMASAK ──ready──▶ SIAP_DIAMBIL ──complete──▶ SELESAI
    └──────reject───▶ DITOLAK
```

- Stok berkurang saat `confirm`, bukan saat pesanan selesai.
- `SEDANG_DIMASAK`, `SIAP_DIAMBIL`, dan `SELESAI` dihitung ke gizi harian pembeli.
- `SELESAI` dan `DITOLAK` adalah status akhir dan masuk tab Riwayat.
- `resolved_at` hanya diisi saat `SELESAI` atau `DITOLAK`.
- Transisi lain → 409 `INVALID_ORDER_STATE`.
