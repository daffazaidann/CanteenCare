# Arsitektur CanteenCare

Dokumen ini untuk anggota Kelompok 10 yang ingin membaca, mengubah, atau memperbaiki kode. Bacalah dari atas ke bawah sekali, setelah itu cukup dipakai sebagai peta.

## 1. Gambaran Besar

```
 Aplikasi Android (satu APK untuk semua peran)
   Compose UI → ViewModel (StateFlow) → Repository → { Retrofit API, Room (keranjang), DataStore (sesi) }
        │ HTTP + header Authorization: Bearer <token>
        ▼
 Backend FastAPI → SQLite (SQLAlchemy)
        │ firebase-admin
        ▼
 Firebase Cloud Messaging → push notification ke HP pembeli/penjual
```

Pembagian tugasnya:

| Di HP (Android) | Di server (FastAPI) |
|---|---|
| Menampilkan layar dan menerima input | Menyimpan data bersama (user, stan, menu, pesanan) |
| Menyimpan keranjang di Room | Login, hash password, token JWT, cek peran |
| Menyimpan sesi login di DataStore | Validasi stok, alur status pesanan, pengurangan stok |
| **Menghitung Fungsi 1, 2, 3** (`NutritionCalculator`) | Menghitung ulang total pesanan dari database (untuk keabsahan) |
| Menampilkan notifikasi | Mengirim notifikasi lewat FCM |

## 2. Lapisan di Aplikasi Android

Polanya sama dengan modul praktikum: **UI → ViewModel → Repository → sumber data**. Setiap lapisan hanya boleh bicara dengan lapisan tepat di bawahnya.

| Lapisan | Folder | Tugas | Aturan |
|---|---|---|---|
| UI | `ui/` | Composable yang menggambar layar | Layar *stateless*: menerima `uiState` dan callback. Tidak boleh memanggil Retrofit, Room, atau DataStore |
| ViewModel | `ui/<fitur>/XxxViewModel.kt` | Menyimpan state layar, menangani event, memanggil repository | State di `MutableStateFlow` privat, dibuka sebagai `StateFlow`. Kerja async di `viewModelScope` |
| Domain | `domain/` | Model data dan logika murni (calculator, validator) | Kotlin murni tanpa Android, jadi bisa diuji JUnit |
| Data | `data/` | Repository dan DTO | Repository mengembalikan `ApiResult` (sekali jalan) atau `Flow` (data yang bisa berubah, seperti keranjang) |
| Core | `core/` | Infrastruktur: network, database, sesi, notifikasi, Hilt | Dipakai oleh lapisan data |

### Struktur folder

```
com.kelompok10.canteencare/
├── CanteenCareApp.kt          # @HiltAndroidApp, membuat notification channel
├── MainActivity.kt            # satu-satunya Activity; meneruskan notifikasi yang diketuk ke navigasi
├── core/
│   ├── network/               # ApiService (Retrofit), AuthInterceptor, ApiResult + safeApiCall, ErrorParser, SessionEvents
│   ├── datastore/             # SessionDataStore: token, id, nama, peran
│   ├── database/              # Room: AppDatabase, CartDao, CartItemEntity
│   ├── notification/          # FirebaseMessagingService, FcmTokenManager, channel, NotificationTarget
│   ├── util/                  # Formatters (Rp, angka gizi), WibTime (tanggal WIB)
│   └── di/                    # Modul Hilt: Network, Database, Repository, Coroutine
├── domain/
│   ├── model/                 # Menu, Vendor, Nutrition, CartItem, Order, NutritionTarget, NutrientProgress, ...
│   ├── calculator/            # NutritionCalculator — Fungsi 1, 2, 3
│   └── validator/             # AuthValidator, TargetValidator, MenuFormValidator, SellerFormValidator
├── data/
│   ├── remote/dto/            # Bentuk JSON + fungsi toDomain()/toRequest()
│   └── repository/            # Interface + implementasi Default*Repository
└── ui/
    ├── theme/                 # Warna Forest Green, tipografi, bentuk
    ├── components/            # Komponen bersama: LoadingView, ErrorView, EmptyView, NutritionTable, NutrientProgressBar, ...
    ├── navigation/            # Routes, CanteenCareNavHost, bottom bar per peran, AppViewModel
    ├── auth/                  # Splash, Login, Register
    ├── profile/               # Profil + logout
    ├── pembeli/               # home, vendor, cart, order, history, nutrition, target
    ├── penjual/               # orders, order, menus, menunew
    └── admin/                 # menus, menu, sellers, sellernew
```

Setiap layar terdiri dari tiga file: `XxxScreen.kt` (tampilan), `XxxViewModel.kt` (logika layar), dan `XxxUiState.kt` (data class state). Di dalam `XxxScreen.kt` ada dua fungsi:

- `XxxRoute(...)`: mengambil ViewModel dengan `hiltViewModel()` dan mengumpulkan state dengan `collectAsStateWithLifecycle()`.
- `XxxScreen(uiState, ...)`: tampilan murni tanpa ViewModel, sehingga bisa dibuat `@Preview`.

## 3. Contoh Alur Data: Ubah Jumlah di Keranjang

1. User menekan tombol **+** di `CartScreen` → callback `onIncrease(item)`.
2. `CartViewModel.onIncrease` memanggil `cartRepository.updateQuantity(...)` di `viewModelScope`.
3. `DefaultCartRepository` memanggil `CartDao.updateQuantity(...)`, yang mengubah tabel `cart_items` di Room.
4. Karena `CartDao.observeAll()` mengembalikan `Flow`, Room otomatis memancarkan isi keranjang terbaru.
5. Di `CartViewModel`, `combine(observeCart(), form)` menerima daftar baru lalu memanggil
   `NutritionCalculator.calculateTotalPrice(items)` dan `calculateOrderNutrition(items)`.
6. `uiState` (StateFlow) berubah → `CartScreen` otomatis tergambar ulang dengan total baru.

Tidak ada kode yang "menyuruh" layar memperbarui total. Semuanya mengalir lewat Flow, dan karena state ada di ViewModel, nilainya tetap ada saat layar diputar.

## 4. Login, Sesi, dan Error

- **Login/daftar** (`DefaultAuthRepository`): token dan data user disimpan di `SessionDataStore`, lalu token FCM dikirim ke server.
- **Setiap request** melewati `AuthInterceptor`, yang menempelkan header `Authorization: Bearer <token>`.
- **Token tidak berlaku (401)**: `AuthInterceptor` mengirim kejadian lewat `SessionEvents` → `AppViewModel` menghapus sesi dan keranjang → `CanteenCareNavHost` kembali ke Login dengan pesan "Sesi berakhir, silakan login lagi".
- **Error lain**: `safeApiCall` mengubah exception menjadi `ApiResult.Error` berisi pesan bahasa Indonesia dari server (dibaca `ErrorParser`). ViewModel tinggal menampilkan `result.message`.
- **Splash** membaca sesi tersimpan. Jika ada, langsung ke home sesuai peran (pembeli: Beranda, penjual: Pesanan Masuk, admin: Verifikasi).

## 5. Notifikasi

1. Setelah login (dan setiap aplikasi dibuka), `FcmTokenManager` mengirim token FCM ke `PUT /users/me/fcm-token`.
2. Saat ada kejadian (pesanan baru, sedang dimasak, siap diambil, selesai, ditolak), backend mengirim notifikasi berisi data `{type, order_id}` ke token itu.
3. Notifikasi diketuk → `MainActivity` membaca `order_id` dari intent → `CanteenCareNavHost` membuka `order/{id}` (pembeli) atau `seller_order/{id}` (penjual).

Satu HP hanya terikat ke akun yang terakhir login di HP itu.

Firebase bersifat opsional. Plugin `google-services` di `app/build.gradle.kts` hanya dipasang jika `google-services.json` ada, dan `FcmTokenManager` melewati pengiriman token jika Firebase tidak aktif. Backend juga melewati pengiriman notifikasi jika `firebase-service-account.json` tidak ada. Jadi semua fitur lain tetap berjalan tanpa Firebase.

## 5a. Upload Gambar Menu

1. Penjual (form Menu Baru) atau admin (detail menu) menekan **Pilih Foto** di komponen `MenuImagePicker`. Komponen ini memakai Photo Picker bawaan Android, jadi tidak perlu izin galeri.
2. ViewModel memanggil `ImageRepository.uploadMenuImage(uri)`. `DefaultImageRepository` membaca isi file, mengecek ukurannya maksimal 2 MB, lalu mengirimnya ke `POST /uploads/menu-image` sebagai multipart.
3. Server mengecek jenis file dari isinya, menyimpannya di `backend/uploads/menus/`, lalu membalas path relatif seperti `/uploads/menus/abc.png`.
4. Path itu disimpan sebagai `image_url` menu saat form disimpan. `FoodImage` melengkapinya menjadi URL penuh lewat `ImageUrl.resolve()`, sehingga gambar tetap muncul di emulator maupun HP walau alamat servernya berbeda.

Gambar tersimpan di laptop yang menjalankan backend dan tidak ikut ke GitHub.

## 6. Backend Singkat

```
backend/app/
├── main.py              # membuat aplikasi, mendaftarkan router, membuat tabel + data demo saat start
├── core/                # config (.env), security (bcrypt + JWT), deps (get_current_user, require_role), errors
├── models/              # tabel SQLAlchemy, satu file per tabel
├── schemas/             # bentuk request/response (Pydantic)
├── routers/             # endpoint: auth, catalog, orders, nutrition_target, seller, admin
├── services/            # order_service (validasi, total, stok, status), notification_service (FCM)
└── seed.py              # data demo
```

Aturan penting di backend:
- Total pesanan **selalu dihitung ulang** dari database; total dari aplikasi tidak pernah dipercaya.
- Item pesanan menyimpan **snapshot** nama, harga, dan gizi, jadi riwayat tidak berubah walau menu diedit.
- Stok baru berkurang saat penjual **mengonfirmasi**, dan dicek ulang dalam satu transaksi.
- Semua error berbentuk `{"detail": {"code": "...", "message": "..."}}` dengan pesan bahasa Indonesia.
- Semua waktu disimpan UTC; "hari ini" dan filter tanggal memakai WIB.

## 7. Resep: Menambah Layar Baru

Contoh: layar "Daftar Favorit" untuk pembeli.

1. **Endpoint**: tambahkan fungsi `suspend` di `core/network/ApiService.kt` dan DTO-nya di `data/remote/dto/` beserta `toDomain()`.
2. **Repository**: tambahkan fungsi di interface repository yang sesuai (atau buat baru), implementasikan dengan `safeApiCall { ... }`. Jika repository baru, daftarkan di `core/di/RepositoryModule.kt`.
3. **UiState**: buat `FavoritUiState` (data class) dengan `isLoading`, `errorMessage`, dan datanya.
4. **ViewModel**: `@HiltViewModel class FavoritViewModel @Inject constructor(repo)` dengan `MutableStateFlow` privat dan fungsi `load()`.
5. **Screen**: `FavoritRoute` (ambil ViewModel) dan `FavoritScreen` (tampilan + `@Preview`). Tangani empat keadaan: loading (`LoadingView`), error dengan "Coba Lagi" (`ErrorView`), kosong (`EmptyView`), dan berhasil.
6. **Navigasi**: tambahkan konstanta di `Routes.kt` dan `composable(...)` di `CanteenCareNavHost.kt`. Jika perlu muncul di bottom bar, tambahkan di `tabsFor()` pada `RoleBottomBar.kt`.
7. **Tes**: logika murni (hitungan, validasi) taruh di `domain/` dan buat tes JUnit di `app/src/test/`.

## 8. Konvensi

- Teks yang terlihat user selalu bahasa Indonesia. Nama kode, variabel, dan pesan commit bahasa Inggris.
- Komentar penting ditulis dalam bahasa Indonesia.
- Warna dari tema (`MaterialTheme.colorScheme`) atau `ui/theme/Color.kt`; jangan menulis kode warna langsung di layar.
- Format uang dan angka gizi selalu lewat `Formatters`, tanggal lewat `WibTime`.
- Perhitungan Fungsi 1, 2, 3 di aplikasi **hanya** boleh lewat `NutritionCalculator`.

## 9. Menjalankan Tes

| Bagian | Perintah | Isi |
|---|---|---|
| Backend | `cd backend` lalu `.venv\Scripts\python -m pytest -q` | 74 tes: auth, peran, visibilitas katalog, validasi pesanan, stok, status pesanan, tanggal WIB, target, notifikasi, upload gambar |
| Android | `cd android` lalu `.\gradlew testDebugUnitTest` | 44 tes JUnit: `NutritionCalculator` (Fungsi 1–3), formatter, validator, parser error, alamat gambar |
