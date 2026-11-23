# CanteenCare Backend

FastAPI + SQLite. Dokumentasi endpoint ada di [../docs/api.md](../docs/api.md) dan Swagger UI di `/docs`.

## Setup pertama kali (Windows)

Jalankan dari folder `backend/`:

```
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
```

Lalu buka `.env` dan ganti `SECRET_KEY` dengan string acak. Untuk membuatnya:

```
python -c "import secrets; print(secrets.token_hex(32))"
```

## Menjalankan server

```
.venv\Scripts\python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Perintah di atas memanggil Python dari venv secara langsung, jadi tidak perlu aktivasi venv. Ini juga menghindari masalah ketika `python` di Windows mengarah ke pintasan Microsoft Store.

- Swagger UI: http://localhost:8000/docs
- `--host 0.0.0.0` diperlukan supaya HP di Wi‑Fi yang sama bisa mengakses laptop.
- Saat pertama kali menyala, tabel dibuat otomatis dan data demo diisi jika database masih kosong.
- Untuk mengulang data demo dari awal, hentikan server lalu hapus file `canteencare.db`.

## Mencoba di Swagger

1. Buka `POST /api/v1/auth/login`, klik **Try it out**, isi email dan password akun demo, lalu **Execute**.
2. Salin nilai `access_token` dari respons.
3. Klik tombol **Authorize** di kanan atas, tempel token, lalu **Authorize**.
4. Sekarang endpoint lain seperti `GET /api/v1/auth/me` bisa dicoba.

## Walkthrough: pembeli pesan → penjual memproses → pesanan selesai

Semua dari Swagger. Tip: buka dua tab Swagger, satu untuk pembeli dan satu untuk penjual, supaya tidak perlu ganti token.

1. Opsional, untuk mulai dari data bersih: login `admin@canteencare.id` / `admin123`, Authorize, lalu jalankan `POST /api/v1/admin/dev/reset-demo`.
2. Login sebagai pembeli `daffa@student.ub.ac.id` / `pembeli123`, lalu Authorize dengan tokennya.
3. `GET /api/v1/vendors/1/menus` → catat `stock` Mie Ayam Solo (id 1), awalnya 25.
4. `POST /api/v1/orders` dengan body:
   ```json
   { "vendor_id": 1, "payment_method": "TUNAI", "items": [{ "menu_id": 1, "quantity": 2 }, { "menu_id": 4, "quantity": 1 }] }
   ```
   Respons berstatus `MENUNGGU`, `total_price` 44000. Catat `id` pesanannya.
5. `GET /api/v1/menus/1` → stok masih 25, karena stok baru berkurang saat penjual mengonfirmasi.
6. Klik Authorize → Logout, lalu login sebagai penjual `gapura@canteencare.id` / `penjual123` dan Authorize.
7. `GET /api/v1/seller/orders?status=MENUNGGU` → pesanan tadi muncul.
8. `POST /api/v1/seller/orders/{order_id}/confirm` → status `SEDANG_DIMASAK`, `resolved_at` masih `null`.
9. `GET /api/v1/seller/menus` → stok Mie Ayam Solo sekarang 23.
10. Jalankan confirm sekali lagi → 409 `INVALID_ORDER_STATE`, karena pesanan sudah melewati tahap MENUNGGU.
11. `POST /api/v1/seller/orders/{order_id}/ready` → status `SIAP_DIAMBIL`.
12. `POST /api/v1/seller/orders/{order_id}/complete` → status `SELESAI` dan `resolved_at` terisi.
13. Login lagi sebagai Daffa, lalu `GET /api/v1/orders/{order_id}` → status `SELESAI`.

## Notifikasi (FCM)

Taruh kunci service account Firebase di `backend/firebase-service-account.json`. Cara mendapatkannya ada di README utama, bagian Setup Firebase. Tanpa file ini server tetap berjalan, hanya mencatat peringatan `Notifikasi dinonaktifkan`.

Notifikasi dikirim otomatis pada lima kejadian berikut:
- **Pesanan baru:** dikirim ke penjual, dengan data `{"type": "NEW_ORDER", "order_id": "..."}`.
- **Pesanan diterima / sedang dimasak:** dikirim ke pembeli, dengan data `{"type": "ORDER_STATUS", "order_id": "..."}`.
- **Pesanan siap diambil:** dikirim ke pembeli, dengan data `{"type": "ORDER_STATUS", "order_id": "..."}`.
- **Pesanan selesai:** dikirim ke pembeli, dengan data `{"type": "ORDER_STATUS", "order_id": "..."}`.
- **Pesanan ditolak:** dikirim ke pembeli beserta alasannya, dengan data `{"type": "ORDER_STATUS", "order_id": "..."}`.

**Notifikasi kadang terkirim, kadang tidak?** Kemungkinan ada dua server yang berjalan di port 8000, misalnya terminal lama yang lupa ditutup. Cek dengan `netstat -ano | findstr :8000`. Jika ada lebih dari satu PID yang `LISTENING`, tutup terminal server lama, atau jalankan `taskkill /PID <pid> /T /F` untuk PID yang lama.

Notifikasi hanya terkirim ke user yang sudah mengirim token lewat `PUT /api/v1/users/me/fcm-token`. Aplikasi Android melakukannya otomatis setelah login. Kalau pengiriman gagal, request tetap berhasil, dan token yang sudah tidak berlaku akan dihapus.

## Menjalankan tes

```
.venv\Scripts\python -m pytest -q
```

Tes memakai database sementara yang terpisah, jadi `canteencare.db` tidak tersentuh.

## Struktur

| Path | Isi |
|---|---|
| `app/main.py` | Membuat aplikasi FastAPI, CORS, router, dan membuat tabel + seed saat start |
| `app/db.py` | Koneksi SQLite, `Base`, tipe `UTCDateTime`, dependency `get_db` |
| `app/core/config.py` | Pengaturan dari `.env` |
| `app/core/security.py` | Hash password bcrypt dan JWT |
| `app/core/deps.py` | `get_current_user` dan `require_role(...)` |
| `app/core/errors.py` | `AppError` dan format error standar |
| `app/core/time.py` | Waktu UTC dan helper tanggal WIB |
| `app/models/` | Tabel database, satu file per tabel |
| `app/schemas/` | Model request dan response Pydantic |
| `app/routers/` | Endpoint API |
| `app/services/` | Logika bisnis: `order_service` untuk pesanan dan stok, `notification_service` untuk FCM |
| `app/seed.py` | Data demo |
| `tests/` | Tes pytest |
