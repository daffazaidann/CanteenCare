# CanteenCare

Aplikasi Android untuk memesan makanan dan minuman dari stan kantin kampus sambil melihat dan memantau kandungan gizi pesanan.

Final project Praktikum Pengembangan Aplikasi Perangkat Bergerak (PAPB) TIF-A, FILKOM Universitas Brawijaya 2026, Kelompok 10.

## Fitur Inti

1. **Pemesanan Makanan dan Minuman Terintegrasi**: keranjang, total harga, pembayaran simulasi (Tunai/QRIS), penerimaan penjual yang mengurangi stok, alur pesanan sampai selesai, dan push notification.
2. **Informasi dan Perhitungan Total Gizi Pesanan**: total kalori, karbohidrat, protein, lemak, garam, dan gula dihitung langsung di keranjang dan berubah setiap kali jumlah porsi diubah.
3. **Pelacakan Asupan Gizi Harian terhadap Target**: progres gizi hari ini (WIB) dari pesanan yang sudah diterima penjual (sedang dimasak, siap diambil, atau selesai), dibandingkan target pribadi, lengkap dengan persentase, sisa, dan warna status.

Ketiga perhitungan dilakukan di aplikasi oleh `NutritionCalculator` (Kotlin murni, diuji JUnit). Backend menghitung ulang total pesanan secara terpisah untuk menjaga keabsahan data.

## Peran Pengguna

| Peran | Bisa apa |
|---|---|
| **Pembeli** | Daftar, login, lihat stan dan menu beserta gizinya, cari menu, keranjang, pesan, pantau pesanan dalam proses dan riwayat, notifikasi status pesanan, atur target gizi, lihat gizi hari ini |
| **Penjual** | Lihat pesanan masuk, terima (stok berkurang) atau tolak dengan alasan, tandai siap diambil, selesaikan pesanan, ubah stok, ajukan menu baru dengan estimasi gizi, arsipkan menu, notifikasi pesanan baru |
| **Admin** | Verifikasi gizi dan setujui/tolak menu, edit data menu, buat akun penjual beserta stannya |

## Struktur Repository

```
CanteenCare/
├── CLAUDE.md              # brief proyek lengkap
├── README.md
├── docs/
│   ├── erd.dbml           # desain database (buka di dbdiagram.io)
│   ├── api.md             # daftar endpoint API
│   ├── ARCHITECTURE.md    # penjelasan arsitektur untuk anggota tim
│   ├── LAPORAN_NOTES.md   # pemetaan kode ke laporan Bab III + daftar screenshot
│   └── DEMO_SCRIPT.md     # skenario video demo 1080p
├── backend/               # FastAPI + SQLite
└── android/               # aplikasi Android (Kotlin, Jetpack Compose)
```

## Prasyarat

- Python 3.11 atau lebih baru
- Git
- Android Studio (sudah termasuk Android SDK dan JDK)
- Project Firebase, untuk push notification (lihat Setup Firebase)

## Menjalankan Backend

Langkah lengkap dan cara mencoba lewat Swagger ada di [backend/README.md](backend/README.md). Ringkasnya, dari folder `backend`:

```
python -m venv .venv
.venv\Scripts\python -m pip install -r requirements.txt
copy .env.example .env
.venv\Scripts\python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

- Buka `.env` dan isi `SECRET_KEY` dengan string acak sebelum menjalankan server.
- Swagger UI: http://localhost:8000/docs
- Data demo terisi otomatis saat pertama kali menyala. Untuk mengulang dari awal, login sebagai admin di Swagger lalu jalankan `POST /api/v1/admin/dev/reset-demo`.
- Tes backend: `.venv\Scripts\python -m pytest -q`

## Menjalankan Aplikasi Android

1. Buka folder `android/` di Android Studio dengan **File → Open**, lalu tunggu Gradle sync selesai.
2. Pastikan `android/app/google-services.json` ada. File ini tidak ikut di Git (lihat Setup Firebase).
3. Nyalakan backend dulu, lalu tekan ▶ **Run**.

**Alamat backend.** Aplikasi membaca alamat backend dari `android/local.properties`, dengan key `canteencare.baseUrl`:

- **Emulator** tidak perlu diatur apa-apa. Default-nya `http://10.0.2.2:8000/api/v1/`, karena `10.0.2.2` adalah alamat laptop jika dilihat dari emulator.
- **HP fisik** harus terhubung ke Wi‑Fi yang sama dengan laptop.
  1. Cari IP laptop: jalankan `ipconfig` di terminal, lalu lihat **IPv4 Address** di adapter Wi‑Fi, misalnya `192.168.1.10`.
  2. Tambahkan baris ini di `android/local.properties`:
     ```
     canteencare.baseUrl=http://192.168.1.10:8000/api/v1/
     ```
  3. Pastikan backend dijalankan dengan `--host 0.0.0.0`. Kalau HP tetap tidak bisa terhubung, izinkan Python di Windows Firewall untuk jaringan Private.
  4. Klik **Sync Project with Gradle Files**, lalu Run ulang.

HTTP biasa tanpa HTTPS hanya diizinkan di build debug, lewat `app/src/debug/res/xml/network_security_config.xml`.

**Build dan tes dari terminal (PowerShell).** Gunakan JDK bawaan Android Studio:

```
cd android
$env:JAVA_HOME="C:\Program Files\Android\Android Studio1\jbr"
.\gradlew assembleDebug testDebugUnitTest
```

Sesuaikan path `JAVA_HOME` dengan lokasi instalasi Android Studio di laptopmu.

## Setup Firebase

Firebase dipakai hanya untuk push notification (FCM). Tanpa Firebase pun backend tetap berjalan, hanya notifikasinya yang tidak terkirim.

**A. Membuat project Firebase**

1. Buka https://console.firebase.google.com dan login dengan akun Google.
2. Klik **Create a project** atau **Add project**, beri nama `CanteenCare`, lalu lanjutkan.
3. Google Analytics boleh dimatikan, karena tidak dipakai. Klik **Create project** dan tunggu sampai selesai.

**B. Mendaftarkan aplikasi Android**

1. Di halaman project, klik **+ Add app**, lalu ikon **Android**.
2. Isi **Android package name** dengan `com.kelompok10.canteencare`, persis tanpa salah ketik. Nickname boleh `CanteenCare`, SHA-1 boleh dikosongkan.
3. Klik **Register app**, lalu **Download google-services.json**, dan simpan di `android/app/google-services.json`.
4. Langkah tambahan tentang Gradle di wizard boleh dilewati dengan **Next** sampai selesai, karena sudah diatur di project.

**C. Kunci service account untuk backend**

1. Buka **Settings → Project settings → Service accounts**.
2. Pilih **Firebase Admin SDK**, lalu klik **Generate new private key → Generate key**. Sebuah file `.json` akan terunduh.
3. Ganti nama file itu menjadi `firebase-service-account.json` dan pindahkan ke folder `backend/`.
4. Restart server backend. Di log harus muncul `Firebase siap, notifikasi aktif.`

**D. Pastikan Cloud Messaging aktif**

Di **Project settings → Cloud Messaging**, bagian **Firebase Cloud Messaging API (V1)** harus berstatus **Enabled**. Untuk project baru, biasanya sudah aktif otomatis.

> **Penting:** `firebase-service-account.json` adalah kunci rahasia yang memberi akses penuh ke project Firebase. File ini dan `google-services.json` sudah masuk `.gitignore`, jadi jangan pernah diunggah ke GitHub. Anggota tim perlu meminta kedua file ini langsung kepada pemilik project Firebase.

## Akun Demo

| Peran | Email | Password | Keterangan |
|---|---|---|---|
| Admin | admin@canteencare.id | admin123 | |
| Penjual | gapura@canteencare.id | penjual123 | Dapur Gapura |
| Penjual | busri@canteencare.id | penjual123 | Warung Bu Sri |
| Penjual | segar@canteencare.id | penjual123 | Kedai Segar |
| Pembeli | daffa@student.ub.ac.id | pembeli123 | Target gizi default, punya contoh pesanan hari ini dan kemarin |
| Pembeli | rafael@student.ub.ac.id | pembeli123 | Target gizi sendiri (2500 kkal) |

Data demo juga berisi 13 menu, termasuk **Nasi Ayam Geprek** yang masih PENDING untuk demo verifikasi admin. Nilai gizi pada data demo adalah estimasi yang wajar, bukan hasil uji laboratorium.

## Skenario Demo

Naskah lengkap video demo (semua peran dan ketiga fungsi inti) ada di [docs/DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md). Selalu jalankan `reset-demo` sebelum merekam supaya data dan tanggal contoh pesanan sesuai hari perekaman.

## Troubleshooting

| Masalah | Penyebab dan solusi |
|---|---|
| `python` membuka Microsoft Store | Panggil Python lewat venv: `.venv\Scripts\python ...` |
| Aplikasi tidak bisa terhubung ke server | Backend belum menyala, atau HP fisik belum diatur `canteencare.baseUrl` (lihat di atas) |
| Notifikasi kadang terkirim, kadang tidak | Ada dua server backend di port 8000. Cek `netstat -ano \| findstr :8000` dan matikan yang lama |
| Notifikasi masuk ke akun yang salah | Satu HP hanya menerima notifikasi untuk akun yang **terakhir login** di HP itu. Untuk demo, pakai dua perangkat |
| Build Gradle gagal karena memori | Tutup aplikasi lain (browser, Discord) saat build sambil menjalankan emulator |
| Gizi hari ini kosong padahal sudah pesan | Hanya pesanan yang sudah **diterima penjual** (sedang dimasak, siap diambil, atau selesai) yang dihitung |
