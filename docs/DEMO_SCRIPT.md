# Naskah Video Demo CanteenCare (1080p)

Target durasi sekitar 8–10 menit. Urutan ini menunjukkan ketiga peran (Admin, Pembeli, Penjual) dan ketiga fungsi inti.

## Persiapan (tidak direkam)

1. **Nyalakan backend** dari folder `backend`:
   `.venv\Scripts\python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000`.
   Jika ingin menunjukkan notifikasi, pastikan log menampilkan `Firebase siap, notifikasi aktif.` Tanpa Firebase, adegan notifikasi dilewati dan status pesanan dilihat dari layar detail atau tab Pesanan.
2. **Pastikan hanya ada satu server.** Jalankan `netstat -ano | findstr :8000`; hanya boleh ada satu PID yang `LISTENING`.
3. **Reset data demo.** Buka http://localhost:8000/docs, login `admin@canteencare.id` / `admin123`, Authorize, lalu jalankan `POST /api/v1/admin/dev/reset-demo`. Dengan ini, contoh pesanan "hari ini" sesuai tanggal perekaman dan Nasi Ayam Geprek kembali PENDING.
4. **Siapkan dua perangkat**, supaya notifikasi masuk ke akun yang benar:
   - **Perangkat A (pembeli):** HP fisik atau emulator pertama, login Daffa.
   - **Perangkat B (penjual/admin):** emulator kedua, login Gapura, lalu nanti Admin.

   Kalau hanya ada satu perangkat, notifikasi hanya masuk ke akun yang terakhir login. Bagian notifikasi bisa ditunjukkan dengan login ulang sebagai Daffa lalu membuka tab Pesanan.
5. **Atur tampilan:** di semua perangkat, aktifkan izin notifikasi dan kosongkan keranjang.
6. **Atur perekaman:** rekam layar 1920×1080. Tampilkan perangkat di tengah dengan latar polos, dan tutup notifikasi aplikasi lain.

## Adegan

### 1. Pembuka (±30 detik)
- Tampilkan ikon aplikasi lalu splash.
- **Narasi:** "CanteenCare membantu mahasiswa memesan makanan kantin sambil memantau gizi. Ada tiga peran: pembeli, penjual, dan admin, dengan tiga fungsi inti: pemesanan, total gizi pesanan, dan pelacakan gizi harian."

### 2. Admin memverifikasi menu baru (±1,5 menit) — Perangkat B
1. Login `admin@canteencare.id`. Tab **Verifikasi** menampilkan **Nasi Ayam Geprek** (Menunggu verifikasi, gizi belum diverifikasi).
2. Buka menu itu. Tunjukkan tombol **Setujui nonaktif** dengan pesan merah "Verifikasi gizi terlebih dahulu".
3. Scroll ke **Gizi per Porsi**, periksa nilainya, lalu tekan **Simpan & Verifikasi Gizi**. Lencana berubah menjadi hijau.
4. Tekan **Setujui**. Muncul pesan "Menu disetujui dan sekarang tampil untuk pembeli".
5. (Opsional) Tab **Penjual** menampilkan daftar stan.
6. Logout, lalu login `gapura@canteencare.id` dan biarkan di layar **Pesanan Masuk**.
- **Narasi:** "Menu dari penjual hanya tampil setelah admin memverifikasi gizinya."

### 3. Pembeli memilih menu dan melihat gizi (±1,5 menit) — Perangkat A
1. Login `daffa@student.ub.ac.id`. Beranda menampilkan sapaan dan grid stan.
2. Ketik "es" di pencarian. Hasil muncul setelah berhenti mengetik, dari dua stan berbeda.
   - **Narasi:** "Pencarian memakai debounce 500 ms, distinctUntilChanged, dan collectLatest."
3. Hapus pencarian, lalu buka **Dapur Gapura**. Tunjukkan chip kategori dan **Nasi Ayam Geprek** yang sekarang muncul.
4. Ketuk **Mie Ayam Solo**. Bottom sheet menampilkan tabel gizi per porsi dan lencana "Gizi terverifikasi admin". Tekan **Tambah ke Keranjang**.
5. Ketuk **Es Teh Manis**, set jumlah 2, lalu tambahkan.

### 4. Fungsi 1 & 2 — Keranjang (±2 menit) — Perangkat A
1. Buka keranjang: 1 Mie Ayam Solo + 2 Es Teh Manis.
2. Tunjukkan **Total Gizi Pesanan** (890 kkal, 145 g karbohidrat, 1.820 mg garam, 66 g gula) dan **Total harga Rp28.000**.
3. Tekan **+** pada Es Teh. Total langsung menjadi **Rp32.000 dan 1.010 kkal**. Tekan **−** untuk kembali.
   - **Narasi:** "Total harga dan total gizi dihitung di aplikasi oleh NutritionCalculator dan berubah otomatis dari Flow keranjang di Room."
4. Pilih **QRIS** dan tunjukkan QR simulasi. Isi catatan "Sambal dipisah".
5. Tekan **Pesan Sekarang**. Detail pesanan tampil dengan status **Menunggu**. Biarkan layar ini terbuka.

### 5. Penjual memproses pesanan sampai selesai (±2 menit) — Perangkat B
1. Notifikasi **"Pesanan Baru"** muncul di perangkat penjual. Tab **Masuk** menampilkan badge.
2. Buka pesanan. Tunjukkan nama pembeli, catatan, item, dan total.
3. Tekan **Terima**. Muncul pesan "Pesanan diterima dan sedang dimasak. Stok sudah dikurangi." Pesanan pindah ke tab **Dimasak**.
4. Buka **Menu & Stok** dan tunjukkan stok Mie Ayam Solo berkurang. Kembali ke Pesanan Masuk.
5. Buka pesanan di tab **Dimasak**, lalu tekan **Tandai Siap Diambil**. Pesanan pindah ke tab **Siap Diambil**.
6. Tekan **Selesaikan Pesanan** dan setujui dialog. Pesanan pindah ke tab **Riwayat**.
- **Narasi:** "Stok baru berkurang saat penjual menerima pesanan, dan dicek ulang di server dalam satu transaksi. Setelah itu pesanan mengalir dari Dimasak, Siap Diambil, sampai Selesai."

### 6. Pembeli memantau status (±1 menit) — Perangkat A
1. Notifikasi **"Pesanan Sedang Dimasak …"** muncul. Status di layar detail berubah sendiri dalam ≤10 detik dan timeline menunjukkan tahap pertama.
2. Saat penjual menandai siap, notifikasi **"Pesanan Siap Diambil …"** muncul dan timeline maju ke tahap Siap Diambil.
3. Saat pesanan diselesaikan, notifikasi **"Pesanan Selesai. Terima kasih!"** muncul.
4. Buka tab **Pesanan**. Tunjukkan tab **Dalam Proses** (masih ada contoh pesanan hari ini dari seed) dan tab **Riwayat** berisi pesanan **Selesai** yang baru diselesaikan serta contoh **Ditolak** dari seed. Tunjukkan filter **Selesai** / **Ditolak**.

### 7. Fungsi 3 — Gizi Hari Ini (±1,5 menit) — Perangkat A
1. Buka tab **Gizi**. Tampil tanggal hari ini (WIB), jumlah pesanan yang sudah diterima, dan enam bar berwarna.
2. Jelaskan satu baris GOAL (kalori: "Sisa … kkal", kuning **Kurang**) dan satu baris LIMIT (garam: **Mendekati batas** atau **Melebihi batas**).
3. Tekan **Ubah Target Gizi** dan ubah target kalori menjadi lebih kecil, misalnya 1800, lalu simpan. Tunjukkan perubahan status atau warna.
4. (Opsional) Isi 0 di salah satu kolom target untuk menunjukkan validasi, lalu tekan **Kembalikan ke default** dan simpan.
- **Narasi:** "Fungsi 3 menjumlahkan gizi semua pesanan yang sudah diterima penjual hari ini, lalu membandingkannya dengan target. Kalori, karbohidrat, dan protein adalah target yang ingin dicapai, sedangkan lemak, garam, dan gula adalah batas maksimal."

### 8. Penolakan pesanan (opsional, ±45 detik)
1. Daffa memesan 1 Tahu Telur.
2. Gapura menekan **Tolak** dan mengisi alasan "Bahan habis".
3. Daffa menerima notifikasi "… ditolak: Bahan habis". Tunjukkan bahwa Gizi Hari Ini tidak berubah, karena pesanan ditolak tidak dihitung.

### 9. Penutup (±30 detik)
- Tampilkan sekilas hasil tes: `pytest` (77 passed) dan `gradlew testDebugUnitTest` (BUILD SUCCESSFUL).
- **Narasi:** "Logika inti diuji dengan unit test, termasuk contoh 63,7% kurang dan 152% melebihi batas."

## Tips Perekaman

- Rekam per adegan, lalu gabungkan, supaya kesalahan kecil cukup diulang di adegan itu saja.
- Jika internet lambat, notifikasi bisa telat beberapa detik. Tunggu, jangan buru-buru pindah layar.
- Jangan tampilkan file `.env` atau `firebase-service-account.json` di video.
