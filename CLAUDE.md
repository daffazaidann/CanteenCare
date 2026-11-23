# CanteenCare — Project Brief for Claude Code

> You are the lead engineer building **CanteenCare** end to end: a FastAPI backend and a native Android app.
> Read this entire file before doing anything. It is the single source of truth for scope, architecture,
> data model, API contract, business rules, and the phased build plan. If something here conflicts with
> your own preference, follow this file. If something is genuinely ambiguous or blocked, ask the user.

---

## 0. How to work with the user (read first)

- **The user is Daffa**, an Informatics student at Universitas Brawijaya. He is learning while building.
  Speak to him in **Bahasa Indonesia**, in plain, friendly language. Code, identifiers, and commit messages stay in English.
  In Indonesian explanations, avoid parenthetical asides; write them as normal sentences instead.
- **Work strictly phase by phase** (Section 11). At the end of every phase:
  1. Run the phase's checks and make sure they pass.
  2. Commit with a clear message.
  3. Report in Indonesian: what was built, which files matter and what each does in one line, how to run or test it, and anything Daffa must do himself.
  4. Ask "Sudah paham dan berhasil dijalankan? Lanjut ke fase berikutnya?" and **wait** for confirmation. Never start the next phase on your own.
- Do not dump huge explanations. Short, structured, practical.
- Write **code comments in Bahasa Indonesia** for important logic, so his teammates can follow.
- When a step needs something only Daffa can do (install a tool, create a Firebase project, download a file, run the app on his phone), stop and give him exact numbered instructions.
- Verify your work by actually running it: backend tests, `./gradlew assembleDebug`, unit tests. Never claim something works without running it.
- Never commit secrets: `.env`, Firebase service-account JSON, keystores.

---

## 1. Project context

- **Course:** Praktikum Pengembangan Aplikasi Perangkat Bergerak (PAPB) TIF-A, FILKOM Universitas Brawijaya, 2026. Lecturer: Fais Al Huda, S.Kom., M.Kom. Group: Kelompok 10.
- **Deliverable:** final project app + written report + GitHub repository + 1080p demo video.
- One person (Daffa) builds the whole app with you. Teammates will later read, adjust, and fix things, so the code must be **clean, consistent, and easy to explain**.
- **What gets graded** (from the report template, Bab III, written per core function):
  - **A. Logika Pemrosesan Data (Problem Solving):** the algorithm, shown as Kotlin code in the ViewModel or domain layer.
  - **B. Layout dan UI:** the Compose layout that displays the output.
  - **C. Event Handling & State:** how user events are processed and how state survives, e.g. StateFlow in ViewModel surviving rotation.
  - **D. Komunikasi Client-Server / Database:** Coroutines, Retrofit, Room.
  - The template stresses that the value-add must be **more than plain CRUD**.
- **Course modules covered so far** define the house style and must remain visible in the code:
  Kotlin basics, Jetpack Compose + Material 3, Navigation Compose with routes and arguments,
  layered architecture UI → ViewModel → Repository → DataSource, Retrofit + Gson with DTO → model mappers,
  Coroutines with `suspend`, `viewModelScope`, private `MutableStateFlow` exposed as `StateFlow`,
  `collectAsStateWithLifecycle()`, Flow operators `debounce`, `distinctUntilChanged`, `collectLatest`,
  and explicit loading / error / retry states.
- The team chose to go beyond the modules with **Hilt, Room, DataStore, and Firebase Cloud Messaging**. Keep the module patterns intact; these tools only automate or extend them. Everything must be explainable to a lab assistant in simple terms.

---

## 2. Product overview

**CanteenCare** is an Android app for university students to **order food and drinks from campus canteen stalls while seeing and tracking the nutrition of what they order.**

**Problem:** students rely on canteen food due to limited time, but have no information about nutritional content, so their diet becomes unbalanced without them noticing.

**Solution:** ordering and nutrition tracking in one app. Every menu shows calories, carbohydrates, protein, fat, salt (sodium), and sugar. The cart shows total nutrition before ordering. Confirmed orders feed a daily nutrition progress view compared with the student's personal daily target.

### 2.1 Roles

| Role | Who | Can do |
|---|---|---|
| **PEMBELI** | Student, main user | Register, log in, browse stalls and menus, search menus, view nutrition per menu, manage cart, see total price and total nutrition, choose payment method, place order, receive push notification when order is confirmed or rejected, view order history and detail, set daily nutrition target, view today's nutrition progress |
| **PENJUAL** | Stall owner, one account = one stall | Log in, see incoming orders, confirm order which deducts stock, reject order with reason, update stock, submit new menu with estimated nutrition, archive menu, receive push notification for new orders |
| **ADMIN** | App manager or nutrition verifier | Log in, create seller accounts with their stall, review pending menus, approve or reject menus, edit menu data, fill in and verify nutrition data |

Relationship flow:
```
Penjual submits menu  →  Admin verifies nutrition and approves  →  Menu visible to Pembeli
Pembeli places order  →  Penjual confirms or rejects            →  Pembeli gets push notification
Confirmed order       →  counted in Pembeli's daily nutrition progress
```

### 2.2 The three core functions (the graded value-add)

**Fungsi 1 — Pemesanan Makanan dan Minuman Terintegrasi**
Pembeli picks menus into the cart, the app computes total price = Σ(price × quantity), Pembeli pays (simulated), the order goes to the Penjual. When the Penjual confirms, stock is deducted, a push notification goes to the Pembeli, and the order appears in history.

**Fungsi 2 — Informasi dan Perhitungan Total Gizi Pesanan**
For every cart item, nutrition per portion × quantity, summed into total order nutrition for all six nutrients. The total updates instantly whenever quantity changes, so the Pembeli knows the nutrition before ordering.

**Fungsi 3 — Pelacakan Asupan Gizi Harian terhadap Target**
Sum the total nutrition of all of today's orders that have been accepted by the seller (**SEDANG_DIMASAK**, **SIAP_DIAMBIL**, or **SELESAI**; date in Asia/Jakarta, WIB), compare with the Pembeli's daily target, and show progress per nutrient with percentage, remaining amount, and a status color.

**Critical rule:** the calculations for Fungsi 1, 2, and 3 must live in **Kotlin on the Android side**, in a pure, unit-tested calculator class used by ViewModels. The backend recomputes order totals independently for integrity, but the app must never just display numbers computed by the server for these functions.

### 2.3 Out of scope

No chat feature. No real payment gateway. No Firebase Auth, no Firestore. No web admin panel. No multi-vendor cart. No image upload; use image URLs or placeholders.

---

## 3. System architecture

```
 Android app (same APK for all roles; UI depends on role after login)
   Compose UI → ViewModel (StateFlow) → Repository → { Retrofit API, Room (cart), DataStore (session) }
        │ HTTPS/HTTP + Bearer token
        ▼
 FastAPI backend  →  SQLite via SQLAlchemy 2.x
        │ firebase-admin
        ▼
 Firebase Cloud Messaging  →  push notification to the buyer's or seller's phone
```

What runs where:

| On the phone | On the server |
|---|---|
| UI rendering and input | Shared persistent data |
| Cart storage in Room | Authentication, password hashing, JWT, role checks |
| Session token in DataStore | Stock validation, order state machine, stock deduction |
| **Fungsi 1/2/3 calculations** | Independent recomputation of order totals from DB values |
| Showing notifications | Sending FCM messages |

Repository layout:
```
CanteenCare/
├── CLAUDE.md
├── README.md                  # how to run everything, demo accounts
├── docs/
│   ├── erd.dbml               # database design
│   ├── api.md                 # endpoint list
│   ├── ARCHITECTURE.md        # for teammates, written in Indonesian
│   └── LAPORAN_NOTES.md       # mapping code to report Bab III A–D per function, in Indonesian
├── backend/
└── android/
```

---

## 4. Backend tech stack

- **Python 3.11+**, **FastAPI**, **Uvicorn**, **SQLAlchemy 2.x** with typed `Mapped[...]` models, **Pydantic v2**, **pydantic-settings** reading `.env`.
- **SQLite** file `canteencare.db`. Tables created with `Base.metadata.create_all` on startup. No Alembic needed.
- Password hashing with the **`bcrypt`** package directly.
- JWT with **PyJWT**, HS256, expiry 7 days, payload `sub` = user id and `role`.
- **firebase-admin** for FCM. If the service-account file is missing, the notification service logs a warning and becomes a no-op, so the backend always runs.
- **pytest** + FastAPI `TestClient` for tests, using a separate temporary SQLite DB.
- Swagger UI at `/docs` must be clean: tags per role, examples on request schemas, and a working "Authorize" Bearer button.

Backend structure:
```
backend/
├── app/
│   ├── main.py                # app factory, CORS, routers, lifespan: create tables + seed
│   ├── core/config.py         # Settings: SECRET_KEY, TOKEN_EXPIRE_DAYS, DATABASE_URL, FIREBASE_CREDENTIALS, TIMEZONE
│   ├── core/security.py       # hash/verify password, create/decode JWT
│   ├── core/deps.py           # get_db, get_current_user, require_role(...)
│   ├── core/errors.py         # AppError + handler producing the standard error shape
│   ├── db.py
│   ├── models/                # one file per table
│   ├── schemas/               # Pydantic request/response models
│   ├── routers/               # auth, catalog, orders, nutrition_target, seller, admin
│   ├── services/              # order_service, notification_service, nutrition helpers
│   └── seed.py                # demo data, idempotent
├── tests/
├── requirements.txt
├── .env.example
└── README.md
```

Run command to document:
```
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```
`--host 0.0.0.0` is required so a physical phone on the same Wi‑Fi can reach the laptop.

---

## 5. Database schema

Use exactly these tables and columns. JSON responses use **snake_case**.

**users**
| column | type | rules |
|---|---|---|
| id | int | PK, autoincrement |
| full_name | str | required |
| email | str | required, unique, stored lowercase |
| password_hash | str | required |
| role | str | `PEMBELI` \| `PENJUAL` \| `ADMIN` |
| fcm_token | str | nullable |
| created_at | datetime | default now, timezone-aware UTC |

**vendors**
| column | type | rules |
|---|---|---|
| id | int | PK |
| owner_id | int | FK users.id, unique: one seller ↔ one stall |
| name | str | required |
| description | str | nullable |
| image_url | str | nullable |
| open_hours | str | nullable, e.g. `07.00 - 16.00` |
| is_active | bool | default true; inactive stalls hidden from buyers |
| created_at | datetime | |

**menus**
| column | type | rules |
|---|---|---|
| id | int | PK |
| vendor_id | int | FK vendors.id |
| name | str | required |
| description | str | nullable |
| category | str | `MAKANAN` \| `MINUMAN` |
| price | int | rupiah, ≥ 0, integer only |
| stock | int | ≥ 0 |
| image_url | str | nullable |
| status | str | `PENDING` \| `APPROVED` \| `REJECTED` \| `ARCHIVED`; only APPROVED is visible to buyers |
| updated_at | datetime | auto update |

**nutritions** — one row per menu, values **per one portion**
| column | type | unit |
|---|---|---|
| id | int | PK |
| menu_id | int | FK menus.id, unique |
| calories | float | kcal |
| carbs | float | g |
| protein | float | g |
| fat | float | g |
| salt | float | mg sodium |
| sugar | float | g |
| verified_by | int | FK users.id of admin, nullable = not verified yet |
| updated_at | datetime | |

**orders**
| column | type | rules |
|---|---|---|
| id | int | PK |
| order_number | str | unique, format `CC-YYYYMMDD-XXXX`, date in WIB, XXXX random uppercase hex |
| buyer_id | int | FK users.id |
| vendor_id | int | FK vendors.id |
| status | str | `MENUNGGU` \| `SEDANG_DIMASAK` \| `SIAP_DIAMBIL` \| `SELESAI` \| `DITOLAK` |
| payment_method | str | `TUNAI` \| `QRIS` (simulated) |
| total_price | int | |
| total_calories, total_carbs, total_protein, total_fat, total_salt, total_sugar | float | |
| note | str | nullable, buyer note to seller |
| rejection_reason | str | nullable |
| created_at | datetime | |
| resolved_at | datetime | nullable, set only when the order becomes SELESAI or DITOLAK |

**order_items** — **snapshot** of name, price, and nutrition at order time, so history never changes when menus are edited later
| column | type |
|---|---|
| id | int PK |
| order_id | FK orders.id |
| menu_id | FK menus.id |
| menu_name | str |
| unit_price | int |
| quantity | int ≥ 1 |
| calories, carbs, protein, fat, salt, sugar | float, per portion |

**nutrition_targets** — one row per buyer, created automatically at registration with defaults
| column | type | default | kind |
|---|---|---|---|
| id | int PK | | |
| user_id | FK users.id, unique | | |
| target_calories | float | 2150 | goal to reach |
| target_carbs | float | 325 | goal to reach |
| target_protein | float | 60 | goal to reach |
| target_fat | float | 67 | maximum limit |
| target_salt | float | 2000 mg | maximum limit |
| target_sugar | float | 50 | maximum limit |
| updated_at | datetime | | |

All target values must be > 0.

---

## 6. API contract

Base path: **`/api/v1`**. All endpoints except register and login require `Authorization: Bearer <token>`.

**Standard error shape** for every error:
```json
{ "detail": { "code": "INSUFFICIENT_STOCK", "message": "Stok Mie Ayam Solo tidak mencukupi, tersisa 2." } }
```
Messages are in Indonesian and safe to show directly in the app. Use proper HTTP codes: 400, 401, 403, 404, 409, 422.

### Auth
| Method | Path | Role | Description |
|---|---|---|---|
| POST | /auth/register | public | Register a **PEMBELI** only. Body `full_name`, `email`, `password` min 6. Creates default nutrition target. Returns `{access_token, token_type, user}` |
| POST | /auth/login | public | Body `email`, `password`. Returns `{access_token, token_type, user}` |
| GET | /auth/me | any | Current user. `user` object: `id, full_name, email, role, vendor_id, vendor_name` |
| PUT | /users/me/fcm-token | any | Body `fcm_token`. Saves device token |

### Catalog
| Method | Path | Role | Description |
|---|---|---|---|
| GET | /vendors | any | Active stalls, optional `?search=` by name |
| GET | /vendors/{id} | any | Stall detail |
| GET | /vendors/{id}/menus | any | APPROVED menus of that stall, optional `?category=` |
| GET | /menus | any | Search APPROVED menus across all active stalls, `?search=` matches menu name, includes `vendor_name` |
| GET | /menus/{id} | any | Buyers get 404 for non-APPROVED menus; sellers can see their own; admins see all |

Menu response:
```json
{
  "id": 1, "vendor_id": 1, "vendor_name": "Dapur Gapura",
  "name": "Mie Ayam Solo", "description": "...", "category": "MAKANAN",
  "price": 20000, "stock": 25, "image_url": null, "status": "APPROVED",
  "nutrition": { "calories": 650, "carbs": 85, "protein": 30, "fat": 25, "salt": 1800, "sugar": 6,
                 "is_verified": true }
}
```

### Orders — PEMBELI
| Method | Path | Description |
|---|---|---|
| POST | /orders | Body `{ vendor_id, payment_method, note?, items: [{ menu_id, quantity }] }`. See rules in Section 7.2 |
| GET | /orders/me | Own orders, newest first. Filters `?status=` and `?date=YYYY-MM-DD` meaning WIB date of `created_at` |
| GET | /orders/{id} | Only the buyer who owns it, the seller of that stall, or admin. Otherwise 404 |

Order response: `id, order_number, buyer_id, buyer_name, vendor_id, vendor_name, status, payment_method, total_price, total_nutrition {six fields}, note, rejection_reason, created_at, resolved_at, items[{menu_id, menu_name, unit_price, quantity, subtotal_price, nutrition_per_portion{six fields}}]`. Datetimes in ISO 8601 with timezone offset.

### Nutrition target — PEMBELI
| Method | Path | Description |
|---|---|---|
| GET | /nutrition-target | Own target |
| PUT | /nutrition-target | Update all six values, each > 0 |

### Seller — PENJUAL, always scoped to own stall
| Method | Path | Description |
|---|---|---|
| GET | /seller/orders | Optional `?status=`, newest first |
| POST | /seller/orders/{id}/confirm | Only from MENUNGGU. Re-check and deduct stock atomically. Set SEDANG_DIMASAK. Send FCM to buyer |
| POST | /seller/orders/{id}/ready | Only from SEDANG_DIMASAK. Set SIAP_DIAMBIL. Send FCM to buyer |
| POST | /seller/orders/{id}/complete | Only from SIAP_DIAMBIL. Set SELESAI + resolved_at. Send FCM to buyer |
| POST | /seller/orders/{id}/reject | Body `reason`. Only from MENUNGGU. Set DITOLAK + resolved_at. Stock untouched. Send FCM to buyer |
| GET | /seller/menus | All own menus, any status |
| POST | /seller/menus | Submit new menu with estimated nutrition. Status PENDING, `verified_by` null |
| PATCH | /seller/menus/{id}/stock | Body `stock` ≥ 0 |
| DELETE | /seller/menus/{id} | Archive: status ARCHIVED, never hard delete |

### Admin
| Method | Path | Description |
|---|---|---|
| POST | /admin/sellers | Create PENJUAL account + its stall in one request |
| GET | /admin/sellers | List sellers with their stall |
| GET | /admin/menus | All menus, optional `?status=` |
| PUT | /admin/menus/{id} | Edit name, description, category, price, image_url |
| PUT | /admin/menus/{id}/nutrition | Set six nutrition values, set `verified_by` = admin |
| POST | /admin/menus/{id}/approve | Requires verified nutrition, else 409 `NUTRITION_NOT_VERIFIED`. Sets APPROVED |
| POST | /admin/menus/{id}/reject | Sets REJECTED |

### Dev
| Method | Path | Description |
|---|---|---|
| POST | /admin/dev/reset-demo | **ADMIN only.** Drop, recreate, and reseed the database for demos |

---

## 7. Business rules

### 7.1 Auth and access
- Emails are case-insensitive. Duplicate email → 409 `EMAIL_EXISTS`. Wrong credentials → 401 `INVALID_CREDENTIALS` without revealing which part is wrong.
- Role mismatch → 403 `FORBIDDEN_ROLE`. Accessing another user's resource → 404, not 403, to avoid leaking existence.
- Sellers can only touch their own stall's menus and orders.

### 7.2 Creating an order
1. `items` not empty, each `quantity` ≥ 1, no duplicate `menu_id`.
2. Every menu exists, is APPROVED, belongs to `vendor_id`, and the stall is active.
3. Every `quantity` ≤ current stock, else 409 `INSUFFICIENT_STOCK` naming the menu and remaining stock.
4. **Recompute everything from the database**: snapshot name, price, nutrition into `order_items`, totals = Σ(value × quantity). Never trust totals sent by the client.
5. Status MENUNGGU. Stock is **not** deducted yet.
6. Send FCM to the stall owner: "Pesanan baru CC-... dari {buyer}".

### 7.3 Order state machine
```
MENUNGGU ──confirm──▶ SEDANG_DIMASAK ──ready──▶ SIAP_DIAMBIL ──complete──▶ SELESAI
    └──────reject───▶ DITOLAK
```
`SELESAI` and `DITOLAK` are final. Any transition outside the arrows above → 409 `INVALID_ORDER_STATE`.
Confirm re-validates stock for all items in one transaction; if any is short, nothing changes and return 409.
Stock is deducted at confirm. `resolved_at` is set only on `SELESAI` or `DITOLAK`.
Daily nutrition counts `SEDANG_DIMASAK`, `SIAP_DIAMBIL`, and `SELESAI`; it does not count `MENUNGGU` or `DITOLAK`.

### 7.4 Notifications (FCM)
- App sends its FCM token after login and whenever the token refreshes.
- Events: new order → seller; accepted/cooking → buyer; ready → buyer; completed → buyer; rejected → buyer with reason.
- Message contains notification title/body in Indonesian plus data payload `{ "type": "ORDER_STATUS" | "NEW_ORDER", "order_id": "<id>" }`.
- Example bodies: "Pesanan CC-20261117-A3F9 diterima dan sedang dimasak di Dapur Gapura.", "Pesanan CC-20261117-A3F9 siap diambil di Dapur Gapura.", "Pesanan CC-20261117-A3F9 selesai. Terima kasih!", and "Pesanan CC-20261117-A3F9 ditolak: Bahan habis."
- Sending failures never break the API request; log and continue. Invalid tokens are cleared.

### 7.5 Time
Store UTC. Every "today" or date filter uses **Asia/Jakarta (WIB)** on both backend and Android.

---

## 8. Seed data

Seeding is idempotent: only when the users table is empty, and also via the reset endpoint. Document the demo accounts in README.

Accounts:
| Role | Email | Password | Note |
|---|---|---|---|
| ADMIN | admin@canteencare.id | admin123 | |
| PENJUAL | gapura@canteencare.id | penjual123 | owns Dapur Gapura |
| PENJUAL | busri@canteencare.id | penjual123 | owns Warung Bu Sri |
| PENJUAL | segar@canteencare.id | penjual123 | owns Kedai Segar |
| PEMBELI | daffa@student.ub.ac.id | pembeli123 | default target |
| PEMBELI | rafael@student.ub.ac.id | pembeli123 | target calories 2500, carbs 375, protein 65, fat 70, salt 2000, sugar 50 |

Stalls: **Dapur Gapura** "Aneka mie dan nasi rumahan", 07.00 - 16.00. **Warung Bu Sri** "Masakan Jawa dan lauk pauk", 07.00 - 15.00. **Kedai Segar** "Jus buah, minuman, dan camilan sehat", 08.00 - 17.00.

Menus, all APPROVED with verified nutrition unless noted. Nutrition = calories kcal, carbs g, protein g, fat g, salt mg, sugar g:
| Stall | Menu | Cat | Price | Stock | Nutrition |
|---|---|---|---|---|---|
| Dapur Gapura | Mie Ayam Solo | MAKANAN | 20000 | 25 | 650, 85, 30, 25, 1800, 6 |
| Dapur Gapura | Tahu Telur | MAKANAN | 12000 | 20 | 420, 30, 18, 26, 900, 12 |
| Dapur Gapura | Nasi Goreng Ayam | MAKANAN | 17000 | 30 | 600, 80, 22, 21, 1500, 5 |
| Dapur Gapura | Es Teh Manis | MINUMAN | 4000 | 50 | 120, 30, 0, 0, 10, 30 |
| Dapur Gapura | Nasi Ayam Geprek | MAKANAN | 18000 | 10 | 590, 55, 38, 22, 1150, 4 — **PENDING, not verified**, for the admin demo |
| Warung Bu Sri | Nasi Pecel | MAKANAN | 12000 | 25 | 480, 70, 14, 16, 800, 10 |
| Warung Bu Sri | Nasi Ayam Bakar | MAKANAN | 18000 | 20 | 620, 75, 35, 18, 1100, 8 |
| Warung Bu Sri | Sayur Sop | MAKANAN | 8000 | 15 | 150, 20, 5, 5, 700, 4 |
| Warung Bu Sri | Es Jeruk | MINUMAN | 5000 | 40 | 110, 27, 1, 0, 5, 22 |
| Kedai Segar | Jus Alpukat | MINUMAN | 12000 | 20 | 250, 20, 3, 18, 15, 14 |
| Kedai Segar | Salad Buah | MAKANAN | 15000 | 15 | 220, 40, 5, 5, 60, 30 |
| Kedai Segar | Air Mineral | MINUMAN | 4000 | 60 | 0, 0, 0, 0, 0, 0 |
| Kedai Segar | Roti Gandum Telur | MAKANAN | 14000 | 15 | 350, 35, 16, 15, 600, 5 |

Sample orders for Daffa, with dates **relative to seeding time** so the demo always has data:
- Today, SIAP_DIAMBIL, Dapur Gapura: 1 Mie Ayam Solo + 2 Es Teh Manis, TUNAI.
- Yesterday, SELESAI, Dapur Gapura: 1 Nasi Goreng Ayam, QRIS.
- Yesterday, DITOLAK, Warung Bu Sri: 1 Nasi Ayam Bakar, QRIS, reason "Bahan habis".

Nutrition values are reasonable estimates; mention this in README.

---

## 9. Android tech stack and conventions

- Project lives in `android/`. App name **CanteenCare**, package **`com.kelompok10.canteencare`**, Kotlin, single Activity, **Jetpack Compose + Material 3**, Gradle Kotlin DSL with version catalog `libs.versions.toml`. minSdk 26, compile/target latest stable SDK installed on Daffa's machine.
- Use latest stable, mutually compatible versions. Known-good versions from the course modules: Navigation Compose 2.9.8, Retrofit 3.0.0 + converter-gson 3.0.0, Lifecycle 2.9.4, kotlinx-coroutines 1.11.0. Verify compatibility before pinning.
- Libraries: Navigation Compose, Lifecycle ViewModel Compose + runtime-compose, **Hilt** with hilt-navigation-compose using KSP, **Retrofit + Gson + OkHttp** with logging interceptor in debug only, **Room** with KSP, **DataStore Preferences**, **Firebase Messaging** via Firebase BoM, **Coil** for images with placeholder fallback.
- Gson uses `FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES`, or explicit `@SerializedName`.
- `BASE_URL` comes from `BuildConfig`, defined in `local.properties` or `gradle.properties`, default `http://10.0.2.2:8000/api/v1/` for the emulator. Document how to switch to the laptop's LAN IP for a physical phone. Allow cleartext HTTP via a network security config limited to debug/local hosts.
- Request `POST_NOTIFICATIONS` permission on Android 13+ at the right moment, after login, with a short explanation.

### 9.1 Architecture

```
com.kelompok10.canteencare/
├── CanteenCareApp.kt            # @HiltAndroidApp, notification channel
├── MainActivity.kt              # setContent → CanteenCareNavHost, handles notification intent → order detail
├── core/
│   ├── network/                 # ApiService, AuthInterceptor, Result/ErrorParser, NetworkModule
│   ├── datastore/               # SessionDataStore: token, user id, role, name
│   ├── database/                # AppDatabase, CartDao, CartItemEntity
│   ├── notification/            # CanteenCareMessagingService, FcmTokenManager
│   ├── util/                    # Rupiah formatter, WIB date helper
│   └── di/                      # Hilt modules
├── domain/
│   ├── model/                   # Menu, Vendor, Nutrition, CartItem, Order, NutritionTarget, NutrientProgress...
│   └── calculator/NutritionCalculator.kt   # Fungsi 1, 2, 3 — pure Kotlin, unit tested
├── data/
│   ├── remote/dto/              # DTOs + toDomain() mappers
│   └── repository/              # interfaces + Default*Repository implementations
│       # AuthRepository, CatalogRepository, CartRepository, OrderRepository,
│       # NutritionRepository, SellerRepository, AdminRepository
├── ui/
│   ├── theme/                   # Color, Type, Theme
│   ├── components/              # LoadingView, ErrorView with retry, EmptyView, NutritionTable, NutrientProgressBar, QuantityStepper, StatusChip
│   ├── navigation/              # Routes object, NavHost, role-based bottom bars
│   ├── auth/  pembeli/  penjual/  admin/  profile/
│   │   # each screen: XxxScreen.kt (stateless UI), XxxViewModel.kt, XxxUiState.kt
```

Rules:
- Screens are **stateless**: they receive `uiState` and callbacks. Only the route-level composable obtains the ViewModel with `hiltViewModel()` and collects with `collectAsStateWithLifecycle()`.
- ViewModels expose `StateFlow<UiState>` from a private `MutableStateFlow`, launch work in `viewModelScope`, rethrow `CancellationException`, and map errors to the Indonesian server message.
- Repositories expose `suspend` functions for one-shot calls and `Flow` for observable data such as the Room cart.
- Composables never touch Retrofit, Room, or DataStore directly.
- Every data screen handles **loading, error with "Coba Lagi", empty, and success**.
- 401 from any request → clear session and return to Login with a message "Sesi berakhir, silakan login lagi".

### 9.2 NutritionCalculator — the heart of the graded logic

Pure Kotlin object, no Android dependencies, fully covered by JUnit tests.
```kotlin
fun calculateSubtotal(price: Int, quantity: Int): Int
fun calculateTotalPrice(items: List<CartItem>): Int                    // Fungsi 1
fun calculateOrderNutrition(items: List<CartItem>): Nutrition         // Fungsi 2: Σ(nutrition per portion × qty)
fun sumNutrition(orders: List<Order>): Nutrition                      // Fungsi 3 step 1
fun calculateProgress(consumed: Nutrition, target: NutritionTarget): List<NutrientProgress>  // Fungsi 3 step 2
```
`NutrientProgress`: `nutrient, consumed, target, unit, percentage, remaining = max(0, target - consumed), excess = max(0, consumed - target), kind, status`.

Two kinds of nutrients:
- **GOAL** (calories, carbs, protein): < 80% → `KURANG` (amber), 80–100% → `TERCUKUPI` (green), > 100% → `BERLEBIH` (red).
- **LIMIT** (fat, salt, sugar): < 80% → `AMAN` (green), 80–100% → `MENDEKATI_BATAS` (amber), > 100% → `MELEBIHI_BATAS` (red).

Example to put in a unit test: consumed calories 1370 vs target 2150 → 63.7%, KURANG, remaining 780. Sugar 76 vs 50 → 152%, MELEBIHI_BATAS, excess 26.

Total nutrition in the cart must be derived reactively, e.g. `cartRepository.observeCart().map { NutritionCalculator.calculateOrderNutrition(it) }` exposed through the ViewModel, so it updates instantly when quantity changes.

### 9.3 Cart rules (Room)
- `CartItemEntity`: menuId PK, vendorId, vendorName, name, price, stock, quantity, and six nutrition fields per portion.
- Single stall per cart. Adding from another stall shows a dialog: "Keranjang berisi menu dari {stall}. Kosongkan dan ganti?"
- Quantity between 1 and stock. Reaching 0 removes the item.
- Cart is scoped to the logged-in user: clear it on logout.
- On successful order: clear cart, navigate to order detail.
- On 409 stock error: show the server message and refresh the menu stock.

### 9.4 Screens and navigation

Use a `Routes` object with string routes and `navArgument`, consistent with the course. Login and splash are outside the role graphs; after login, route by role and clear the back stack.

**Shared:** `splash` checks session → `login` ↔ `register` → role home. `profile` shows name, email, role, stall name for sellers, and Logout.

**PEMBELI** — bottom bar: Beranda, Gizi, Pesanan, Profil
- `home`: greeting "Selamat datang, {nama}", search bar with **debounce 500 ms + distinctUntilChanged + collectLatest** searching menus across stalls; when search is empty show the stall grid; cart button with item count badge.
- `vendor/{vendorId}`: stall header, category filter chips, menu grid with price, kcal, and stock; tapping opens a **ModalBottomSheet** with menu detail, full nutrition table, "Gizi terverifikasi admin" badge, quantity stepper, and "Tambah ke Keranjang".
- `cart`: items with stepper and remove, **total nutrition card** with all six nutrients, **total price**, payment method selector TUNAI / QRIS, optional note, "Pesan Sekarang". QRIS shows a sample QR image with "Simulasi pembayaran".
- `order/{orderId}`: status chip, progress steps, order number, stall, items, totals, rejection reason if any, timestamps. Auto-refresh every 10 s while the status is active (MENUNGGU, SEDANG_DIMASAK, SIAP_DIAMBIL).
- `history`: "Pesanan Saya" with tabs Dalam Proses and Riwayat, status chip, pull-to-refresh. Riwayat has filter chips Semua / Selesai / Ditolak.
- `nutrition`: "Gizi Hari Ini" with date, six **NutrientProgressBar** rows colored by status with texts like "Sisa 780 kkal" or "Melebihi batas 26 g", count of confirmed orders today, button to edit target.
- `nutrition_target`: form for six values with validation > 0 and a "Kembalikan ke default" option.

**PENJUAL** — bottom bar: Pesanan, Menu & Stok, Profil
- `seller_orders`: tabs Masuk / Dimasak / Siap Diambil / Riwayat, auto-refresh every 10 s on active tabs, badges with pending and ready counts.
- `seller_order/{orderId}`: items, buyer name, note, totals. MENUNGGU has Terima and Tolak (dialog for reason); SEDANG_DIMASAK has Tandai Siap Diambil; SIAP_DIAMBIL has Selesaikan Pesanan with confirmation dialog.
- `seller_menus`: list with status chip, inline stock edit, archive with confirmation dialog.
- `seller_menu_new`: form for name, description, category, price, stock, image URL, and estimated nutrition, with info text "Menu akan diverifikasi admin sebelum tampil".

**ADMIN** — bottom bar: Verifikasi, Semua Menu, Penjual, Profil
- `admin_pending`: PENDING menus.
- `admin_menus`: all menus with status filter.
- `admin_menu/{menuId}`: edit menu data, edit nutrition and save as verified, Approve and Reject buttons. Approve disabled until nutrition is verified.
- `admin_sellers` + `admin_seller_new`: list sellers and create seller + stall.

Notification tap opens `order/{orderId}` for buyers and `seller_order/{orderId}` for sellers.

### 9.5 Design system
- Material 3, **Forest Green** primary `#228B22`, darker container/primary variants derived from it, white background, light green accents, matching the "healthy app" look in the team's Figma.
- Status colors: green `#2E7D32`, amber `#F9A825`, red `#C62828`.
- Rupiah format "Rp20.000". Nutrition units: kkal, g, mg.
- Rounded cards, generous spacing, clear typography hierarchy, consistent top app bars, `Scaffold` + `NavigationBar`.
- Placeholder image: a soft green rounded box with a food or drink icon when `image_url` is null or fails.
- Provide `@Preview` for key components.
- All UI text in Bahasa Indonesia.

---

## 10. Quality bar

- Backend: pytest covering auth, role checks, catalog visibility rules, order creation validation and recomputed totals, confirm/reject state machine with stock deduction, ownership checks, date filter in WIB, target validation.
- Android: JUnit tests for `NutritionCalculator`, including the examples in 9.2 and edge cases like empty list and zero quantity. `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` must pass.
- No hardcoded secrets. No dead code. No unused dependencies. Consistent naming.
- Every screen works on rotation without losing state.
- README explains setup, running backend, running app on emulator and phone, Firebase setup, demo accounts, and a demo script.

---

## 11. Build plan — execute one phase at a time

After each phase: verify → commit → report in Indonesian → ask to continue → **wait**.

**Phase 0 — Repository setup**
Create folder structure, root README skeleton, `.gitignore` for Python, Android, and secrets, `docs/erd.dbml` and `docs/api.md` reflecting Sections 5 and 6, `git init`, first commit. Check that Python 3.11+, Git, and the Android SDK are available and tell Daffa what is missing.

**Phase 1 — Backend foundation**
Config, DB, all models, security, deps, error handling, seed, auth endpoints, `/auth/me`, fcm-token endpoint. Tests for auth. Done when the server starts, Swagger opens, and the demo accounts can log in.

**Phase 2 — Backend features**
Catalog, orders, nutrition target, seller, admin, reset-demo. Full test suite passing. Done when Daffa can walk through: buyer orders → seller confirms → stock decreases → order shows SEDANG_DIMASAK → ready → SELESAI, all from Swagger. Give him that walkthrough as numbered steps.

**Phase 3 — Backend notifications**
Notification service with firebase-admin, no-op without credentials. Then give Daffa exact steps to create a Firebase project, add an Android app with package `com.kelompok10.canteencare`, download `google-services.json` into `android/app/`, and generate a service-account key saved as `backend/firebase-service-account.json` which is gitignored.

**Phase 4 — Android project foundation**
Ask Daffa to create the project in Android Studio: New Project → Empty Activity, name CanteenCare, package `com.kelompok10.canteencare`, save location `CanteenCare/android`, language Kotlin, minimum SDK API 26, build configuration language Kotlin DSL. Wait until it exists. Then add dependencies, Hilt, theme, network layer, DataStore, Room setup, navigation skeleton with all routes as placeholders and role bottom bars. Done when `assembleDebug` passes and the app launches to the splash/login placeholder.

**Phase 5 — Auth**
Splash, Login, Register, session persistence, role routing, logout, 401 handling, FCM token upload after login. Done when each demo account lands on its own role home and stays logged in after restarting the app.

**Phase 6 — Buyer catalog**
Home with stall grid and reactive search, vendor screen, menu detail bottom sheet with nutrition.

**Phase 7 — Cart and checkout: Fungsi 1 and Fungsi 2**
`NutritionCalculator` + tests, Room cart, cart screen with live totals, payment method, place order, order detail.

**Phase 8 — History and notifications**
History with filters, order detail auto-refresh, `FirebaseMessagingService`, notification channel, permission request, tap-to-open deep navigation.

**Phase 9 — Daily nutrition: Fungsi 3**
Target screen and nutrition progress screen with status colors, driven by `NutritionCalculator.calculateProgress`.

**Phase 10 — Seller features**
Orders with tabs and auto-refresh, confirm and reject, menu list with stock edit and archive, new menu form.

**Phase 11 — Admin features**
Pending list, all menus, menu edit with nutrition verification, approve and reject, seller management.

**Phase 12 — Polish and handover**
Consistency pass on UI, empty states, rotation, error messages; final tests; complete README; `docs/ARCHITECTURE.md` in Indonesian for teammates; `docs/LAPORAN_NOTES.md` in Indonesian that, for each of the three functions, points to the exact files and functions for report sections A, B, C, D with short code excerpts and a one-paragraph plain explanation written like a student's own understanding; a demo script for the 1080p video covering all three functions and all three roles; a list of screenshots to take for the report.

---

## 12. Definition of done

- Backend runs with one command, seeds demo data, has clean Swagger docs, and all tests pass.
- The Android app builds and runs on the emulator and on a physical phone over Wi‑Fi.
- All three roles work end to end with the demo accounts.
- Buyer sees live total price and total nutrition in the cart, places an order, receives a push notification when the seller confirms or rejects, and sees today's nutrition progress update with correct colors.
- Seller confirms an order and stock decreases; rejected orders keep stock and are not counted in nutrition.
- Admin verifies nutrition and approves a pending menu, which then appears for buyers.
- `NutritionCalculator` is unit tested and is the only place the three core calculations happen on the client.
- Docs exist so teammates can run, understand, and modify the project, and Daffa can write report Bab III and record the demo.
