# Catatan untuk Laporan Bab III

Dokumen ini memetakan setiap fungsi inti CanteenCare ke bagian laporan Bab III:

- **A. Logika Pemrosesan Data (Problem Solving)**
- **B. Layout dan UI**
- **C. Event Handling & State**
- **D. Komunikasi Client-Server / Database**

Semua path relatif terhadap `android/app/src/main/java/com/kelompok10/canteencare/`, kecuali disebut lain. Potongan kode di bawah sudah diringkas; versi lengkapnya ada di file aslinya.

Paragraf "Penjelasan" ditulis dengan gaya pemahaman mahasiswa dan boleh langsung diadaptasi ke laporan dengan kata-kata sendiri.

---

## Fungsi 1 — Pemesanan Makanan dan Minuman Terintegrasi

### A. Logika Pemrosesan Data

**File:** `domain/calculator/NutritionCalculator.kt`, fungsi `calculateSubtotal` dan `calculateTotalPrice`.

```kotlin
/** Subtotal satu baris: harga satuan × jumlah porsi. */
fun calculateSubtotal(price: Int, quantity: Int): Int = price * quantity

/** Fungsi 1 — Total harga pesanan. Rumus: Σ (harga × jumlah) untuk setiap item. */
fun calculateTotalPrice(items: List<LineItem>): Int =
    items.sumOf { item -> calculateSubtotal(item.price, item.quantity) }
```

Pendukung: `domain/model/LineItem.kt` adalah interface yang dipakai `CartItem` (keranjang) dan `OrderItem` (pesanan), sehingga satu rumus berlaku untuk keduanya. Tesnya ada di `app/src/test/.../domain/calculator/NutritionCalculatorTest.kt`, contohnya 1 Mie Ayam Solo (Rp20.000) + 2 Es Teh Manis (Rp4.000) = Rp28.000.

**Penjelasan:** Total harga dihitung dengan menjumlahkan subtotal setiap item di keranjang, di mana subtotal adalah harga satuan dikali jumlah porsi. Perhitungan ini kami pisahkan ke objek `NutritionCalculator` yang murni Kotlin, tanpa kode Android, sehingga bisa diuji dengan JUnit tanpa emulator. Total di layar keranjang dan di detail pesanan sama-sama memakai fungsi ini. Server juga menghitung ulang total dari harga di database, supaya pembeli tidak bisa mengirim total palsu, tetapi angka yang tampil di aplikasi tetap hasil perhitungan aplikasi sendiri.

### B. Layout dan UI

**File:** `ui/pembeli/cart/CartScreen.kt`. Bagian penting:
- `CartItemCard`: nama menu, harga per porsi, subtotal, `QuantityStepper` (− jumlah +), dan tombol hapus.
- `PaymentMethodSelector`: `SingleChoiceSegmentedButtonRow` untuk Tunai/QRIS; QRIS menampilkan `QrisSimulationCard` (`ui/pembeli/cart/QrisSimulation.kt`).
- `CheckoutBar`: bar bawah berisi total harga dan tombol "Pesan Sekarang".

```kotlin
Scaffold(
    topBar = { TopAppBar(title = { Text("Keranjang") }, ...) },
    bottomBar = { if (!uiState.isEmpty) CheckoutBar(uiState.totalPrice, uiState.isPlacingOrder, onPlaceOrder) },
) { padding ->
    LazyColumn(...) {
        items(uiState.items, key = { it.menuId }) { item -> CartItemCard(item, onIncrease, onDecrease, onRemove) }
        item { SectionTitle("Total Gizi Pesanan"); NutritionTable(uiState.totalNutrition) }
        item { SectionTitle("Metode Pembayaran"); PaymentMethodSelector(uiState.paymentMethod, onPaymentMethodChange) }
        ...
    }
}
```

Layar lain di alur Fungsi 1:
- `ui/pembeli/order/OrderDetailScreen.kt`: detail pesanan dengan timeline status.
- `ui/pembeli/history/HistoryScreen.kt`: tab Dalam Proses dan Riwayat dengan filter.
- `ui/penjual/order/SellerOrderDetailScreen.kt`: tombol Terima/Tolak, Tandai Siap Diambil, dan Selesaikan Pesanan.

**Penjelasan:** Layar keranjang dibangun dengan `Scaffold`. `LazyColumn` dipakai supaya daftar item bisa di-scroll dan efisien. Total harga diletakkan di `bottomBar`, jadi selalu terlihat walaupun daftar panjang. Setiap baris item adalah `Card` dengan stepper, sehingga pembeli bisa mengubah jumlah tanpa pindah layar.

### C. Event Handling & State

**File:** `ui/pembeli/cart/CartViewModel.kt`.

```kotlin
fun onIncrease(item: CartItem) {
    if (item.quantity >= item.stock) return // tidak boleh melebihi stok
    viewModelScope.launch { cartRepository.updateQuantity(item.menuId, item.quantity + 1) }
}

fun placeOrder() {
    ...
    viewModelScope.launch {
        form.update { it.copy(isPlacingOrder = true, errorMessage = null) }
        when (val result = orderRepository.placeOrder(vendorId, state.paymentMethod, state.note, state.items)) {
            is ApiResult.Success -> {
                cartRepository.clear()
                form.update { FormState(placedOrderId = result.data.id) } // tanda untuk pindah ke detail pesanan
            }
            is ApiResult.Error -> {
                form.update { it.copy(isPlacingOrder = false, errorMessage = result.message) }
                if (result.code in STOCK_ERROR_CODES) refreshStock(vendorId) // stok di keranjang disesuaikan
            }
        }
    }
}
```

Di `CartScreen.kt` (fungsi `CartRoute`), `placedOrderId` diamati dengan `LaunchedEffect`, lalu aplikasi pindah ke `order/{orderId}`. Setelah itu `onOrderOpened()` mengosongkan tanda tersebut supaya navigasi tidak terulang saat layar diputar.

**Penjelasan:** Setiap tombol di layar hanya memanggil fungsi di ViewModel, dan ViewModel yang menjalankan pekerjaan di `viewModelScope`. State layar, seperti metode pembayaran, catatan, dan status loading, disimpan di `MutableStateFlow` milik ViewModel. Karena ViewModel tetap hidup saat layar diputar, pilihan pembayaran dan catatan yang sudah diketik tidak hilang. Untuk kejadian sekali jalan seperti "pesanan berhasil, buka detail", kami memakai field `placedOrderId` yang dikosongkan lagi setelah navigasi.

### D. Komunikasi Client-Server / Database

- **Retrofit:** `core/network/ApiService.kt`, yaitu `@POST("orders") createOrder(...)`. Untuk penjual: `@POST("seller/orders/{id}/confirm") confirmOrder(...)` dan `rejectOrder(...)`.
- **Repository:** `data/repository/DefaultOrderRepository.kt`, fungsi `placeOrder`.
- **Room:** `core/database/CartDao.kt` dan `CartItemEntity.kt` (tabel `cart_items`).
- **Notifikasi:** `core/notification/CanteenCareMessagingService.kt`; backend `backend/app/services/notification_service.py`.

```kotlin
override suspend fun placeOrder(...): ApiResult<Order> = safeApiCall {
    val body = CreateOrderRequestDto(
        vendorId = vendorId,
        paymentMethod = paymentMethod.name,
        note = note?.trim()?.takeIf { it.isNotEmpty() },
        // Hanya id menu dan jumlah yang dikirim; harga dan gizi diambil server dari database
        items = items.map { OrderItemRequestDto(menuId = it.menuId, quantity = it.quantity) },
    )
    api.createOrder(body).toDomain()
}
```

**Penjelasan:** Keranjang disimpan secara lokal di database Room, jadi isinya tidak hilang walaupun aplikasi ditutup. Saat pembeli menekan "Pesan Sekarang", aplikasi mengirim `POST /orders` lewat Retrofit di dalam coroutine (`suspend fun`), sehingga UI tidak macet. Backend memvalidasi stok, menyimpan pesanan berstatus MENUNGGU, dan mengirim notifikasi FCM ke penjual. Saat penjual mengonfirmasi, backend mengurangi stok dalam satu transaksi dan mengirim notifikasi ke pembeli. Jika notifikasi diketuk, aplikasi langsung membuka detail pesanan.

---

## Fungsi 2 — Informasi dan Perhitungan Total Gizi Pesanan

### A. Logika Pemrosesan Data

**File:** `domain/calculator/NutritionCalculator.kt`, fungsi `calculateOrderNutrition`.

```kotlin
/**
 * Fungsi 2 — Total gizi pesanan.
 * Untuk setiap jenis gizi: Σ (gizi per porsi × jumlah porsi).
 * Contoh: 1 Mie Ayam (650 kkal) + 2 Es Teh (120 kkal) = 650 + 2 × 120 = 890 kkal.
 */
fun calculateOrderNutrition(items: List<LineItem>): Nutrition = Nutrition(
    calories = items.sumOf { it.nutrition.calories * it.quantity },
    carbs = items.sumOf { it.nutrition.carbs * it.quantity },
    protein = items.sumOf { it.nutrition.protein * it.quantity },
    fat = items.sumOf { it.nutrition.fat * it.quantity },
    salt = items.sumOf { it.nutrition.salt * it.quantity },
    sugar = items.sumOf { it.nutrition.sugar * it.quantity },
)
```

Tes di `NutritionCalculatorTest.kt`: contoh 890 kkal / 145 g karbohidrat / 1.820 mg garam / 66 g gula, keranjang kosong menghasilkan nol, jumlah 0 tidak menambah total, dan total berubah sesuai jumlah porsi.

**Penjelasan:** Setiap menu menyimpan enam nilai gizi untuk satu porsi, yaitu kalori, karbohidrat, protein, lemak, garam (natrium), dan gula. Total gizi pesanan didapat dengan mengalikan gizi per porsi dengan jumlah porsi, lalu menjumlahkan semua item, untuk keenam gizi sekaligus. Hasilnya berupa objek `Nutrition` yang langsung bisa ditampilkan di tabel.

### B. Layout dan UI

**File:**
- `ui/components/NutritionTable.kt`: tabel enam gizi beserta satuannya.
- `ui/pembeli/cart/CartScreen.kt`: kartu "Total Gizi Pesanan".
- `ui/pembeli/vendor/MenuDetailSheet.kt`: `ModalBottomSheet` berisi gizi per porsi dan lencana "Gizi terverifikasi admin".

```kotlin
@Composable
fun NutritionTable(nutrition: Nutrition, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Nutrient.entries.forEachIndexed { index, nutrient ->
                if (index > 0) HorizontalDivider(...)
                Row(horizontalArrangement = Arrangement.SpaceBetween, ...) {
                    Text(nutrient.label)
                    Text(Formatters.nutrient(nutrition.valueOf(nutrient), nutrient.unit), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
```

**Penjelasan:** Tabel gizi dibuat sebagai komponen yang bisa dipakai ulang. Komponen yang sama dipakai untuk gizi per porsi di detail menu, total gizi di keranjang, dan total gizi di detail pesanan. Isinya diambil dengan perulangan `Nutrient.entries`, jadi keenam baris selalu tampil dengan urutan dan satuan yang sama, misalnya "1.820 mg".

### C. Event Handling & State

**File:** `ui/pembeli/cart/CartViewModel.kt`.

```kotlin
/**
 * Fungsi 1 dan 2 secara reaktif: setiap kali isi keranjang di Room berubah, Flow memancarkan
 * daftar item baru, lalu total harga dan total gizi langsung dihitung ulang oleh NutritionCalculator.
 */
val uiState: StateFlow<CartUiState> = combine(cartRepository.observeCart(), form) { items, form ->
    CartUiState(
        items = items,
        totalPrice = NutritionCalculator.calculateTotalPrice(items),
        totalNutrition = NutritionCalculator.calculateOrderNutrition(items),
        ...
    )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())
```

Layar mengamati state ini dengan `collectAsStateWithLifecycle()` di `CartRoute`.

**Penjelasan:** Total gizi tidak dihitung saat tombol ditekan, melainkan diturunkan secara reaktif dari isi keranjang. `observeCart()` adalah `Flow` dari Room yang otomatis mengirim data baru setiap tabel keranjang berubah. Dengan `combine`, setiap data baru langsung dihitung ulang oleh `NutritionCalculator` dan disimpan di `StateFlow`. Hasilnya, begitu pembeli menekan + atau −, angka kalori dan gizi lainnya langsung berubah tanpa kode tambahan. Karena berada di ViewModel, nilainya juga tetap ada saat layar diputar.

### D. Komunikasi Client-Server / Database

- **Room:** `core/database/CartDao.kt`:
  ```kotlin
  @Query("SELECT * FROM cart_items ORDER BY name")
  fun observeAll(): Flow<List<CartItemEntity>>
  ```
  `CartItemEntity` menyimpan enam nilai gizi per porsi untuk setiap item keranjang.
- **Retrofit:** gizi per porsi didapat dari `GET /vendors/{id}/menus` dan `GET /menus?search=` (`ApiService`, `DefaultCatalogRepository`), lalu disalin ke Room saat menu ditambahkan ke keranjang (`DefaultCartRepository.addItem`).
- **Pencarian menu:** `ui/pembeli/home/HomeViewModel.kt` memakai `debounce(500)`, `distinctUntilChanged()`, dan `collectLatest` sebelum memanggil `searchMenus`.

**Penjelasan:** Data gizi berasal dari server, yang sudah diverifikasi admin, lalu dibawa ke database lokal saat menu masuk keranjang. Dengan begitu perhitungan total gizi di keranjang tidak perlu request ke server setiap kali jumlah diubah: cukup membaca Room lewat Flow. Di layar Beranda, pencarian menu memakai operator Flow supaya request tidak dikirim di setiap huruf, tetapi hanya setelah user berhenti mengetik 500 ms.

---

## Fungsi 3 — Pelacakan Asupan Gizi Harian terhadap Target

### A. Logika Pemrosesan Data

**File:** `domain/calculator/NutritionCalculator.kt`, fungsi `sumNutrition`, `calculateProgress`, dan `statusOf`. Model ada di `domain/model/NutritionTarget.kt` (`NutrientProgress`, `NutrientStatus`) dan `domain/model/Nutrition.kt` (`NutrientKind` GOAL/LIMIT).

```kotlin
/** Langkah 1: hanya pesanan yang sudah diterima penjual yang dihitung. */
fun sumNutrition(orders: List<Order>): Nutrition {
    val counted = orders.filter { it.status.isCounted }
    val perOrder = counted.map { calculateOrderNutrition(it.items) }
    return Nutrition(calories = perOrder.sumOf { it.calories }, ...)
}

/** Langkah 2: progres keenam gizi terhadap target. */
fun calculateProgress(consumed: Nutrition, target: NutritionTarget): List<NutrientProgress> =
    Nutrient.entries.map { nutrient ->
        val eaten = consumed.valueOf(nutrient)
        val goal = target.valueOf(nutrient)
        val rawPercentage = if (goal > 0) eaten / goal * 100 else 0.0
        NutrientProgress(
            nutrient = nutrient, consumed = eaten, target = goal, unit = nutrient.unit,
            percentage = roundToOneDecimal(rawPercentage),
            remaining = max(0.0, goal - eaten),
            excess = max(0.0, eaten - goal),
            kind = nutrient.kind,
            status = statusOf(nutrient.kind, rawPercentage),
        )
    }

/** GOAL: <80% KURANG, 80–100% TERCUKUPI, >100% BERLEBIH. LIMIT: <80% AMAN, 80–100% MENDEKATI_BATAS, >100% MELEBIHI_BATAS. */
fun statusOf(kind: NutrientKind, percentage: Double): NutrientStatus = when (kind) {
    NutrientKind.GOAL -> when {
        percentage < 80.0 -> NutrientStatus.KURANG
        percentage <= 100.0 -> NutrientStatus.TERCUKUPI
        else -> NutrientStatus.BERLEBIH
    }
    NutrientKind.LIMIT -> when { ... }
}
```

Tes di `app/src/test/.../domain/calculator/DailyNutritionTest.kt`:
- Kalori 1370 dari 2150 menghasilkan **63,7% KURANG, sisa 780**.
- Gula 76 dari 50 menghasilkan **152% MELEBIHI_BATAS, lebih 26**.
- Batas tepat di 80% dan 100%.
- Pesanan sedang dimasak, siap diambil, dan selesai ikut dihitung; pesanan menunggu dan ditolak tidak ikut dihitung.

**Penjelasan:** Fungsi 3 terdiri dari dua langkah. Pertama, semua pesanan hari ini yang sudah diterima penjual dijumlahkan gizinya. Pesanan yang masih menunggu atau ditolak tidak dihitung karena belum tentu dimakan, sedangkan pesanan yang sedang dimasak, siap diambil, atau sudah selesai dianggap akan atau sudah dikonsumsi. Kedua, total tersebut dibandingkan dengan target harian pembeli untuk setiap gizi, menghasilkan persentase, sisa, kelebihan, dan status. Ada dua jenis gizi. Kalori, karbohidrat, dan protein adalah target yang ingin dicapai, sehingga di bawah 80% berarti kurang. Lemak, garam, dan gula adalah batas maksimal, sehingga di bawah 80% justru aman dan di atas 100% berarti melebihi batas.

### B. Layout dan UI

**File:**
- `ui/pembeli/nutrition/NutritionScreen.kt`: layar "Gizi Hari Ini" dengan tanggal WIB, kartu ringkasan, dan tombol "Ubah Target Gizi".
- `ui/components/NutrientProgressBar.kt`: satu baris progres.
- `ui/pembeli/target/NutritionTargetScreen.kt`: form target dengan "Kembalikan ke default".

```kotlin
@Composable
fun NutrientProgressBar(progress: NutrientProgress, modifier: Modifier = Modifier) {
    val color = progress.status.tone.color() // hijau / kuning / merah
    Column(...) {
        Row { Text(progress.nutrient.label); ...; Text(progress.status.label, color = color, ...) }
        LinearProgressIndicator(
            progress = { (progress.percentage / 100).toFloat().coerceIn(0f, 1f) },
            color = color, trackColor = color.copy(alpha = 0.18f), ...
        )
        Row { Text(remainingText(progress)); Text("${Formatters.number(progress.percentage)}%", color = color) }
    }
}
```

**Penjelasan:** Setiap gizi ditampilkan sebagai satu kartu berisi bar progres. Warnanya mengikuti status: hijau berarti baik, kuning berarti perlu perhatian, merah berarti berlebih atau melewati batas. Di bawah bar ada teks yang mudah dipahami, seperti "Sisa 780 kkal" atau "Melebihi batas 26 g", serta persentasenya. Bar dibatasi maksimal penuh, sedangkan kelebihannya ditunjukkan lewat warna merah dan teks.

### C. Event Handling & State

**File:**
- `ui/pembeli/nutrition/NutritionViewModel.kt` dan `NutritionScreen.kt` (fungsi `NutritionRoute`).
- `ui/pembeli/target/NutritionTargetViewModel.kt`.

```kotlin
// NutritionRoute: muat ulang setiap kali layar tampil (misalnya setelah target diubah)
LifecycleResumeEffect(Unit) {
    viewModel.onScreenResumed()
    onPauseOrDispose { }
}

// NutritionViewModel.load(): target dan pesanan diambil bersamaan
val targetCall = async { nutritionRepository.getTarget() }
val ordersCall = async { nutritionRepository.getOrdersForDate(today) }
...
val consumed = NutritionCalculator.sumNutrition(orders.data)
val progress = NutritionCalculator.calculateProgress(consumed, target.data)
_uiState.update {
    it.copy(
        isLoading = false,
        progress = progress,
        countedOrderCount = orders.data.count { order -> order.status.isCounted },
    )
}
```

Form target menyimpan isian sebagai teks di `NutritionTargetUiState`. Saat "Simpan" ditekan, setiap nilai divalidasi dengan `TargetValidator.parse` (harus lebih dari 0). Kolom yang salah ditandai merah.

**Penjelasan:** Layar gizi dimuat ulang setiap kali tampil di layar, sehingga setelah target diubah atau penjual mengonfirmasi pesanan, progres langsung terbarui saat pembeli kembali ke tab Gizi. Pembeli juga bisa menarik layar ke bawah untuk refresh. Target dan pesanan diambil bersamaan dengan `async` supaya lebih cepat. Hasil perhitungan disimpan di `StateFlow` ViewModel, jadi tidak perlu dihitung ulang saat layar diputar.

### D. Komunikasi Client-Server / Database

- **Retrofit:** `ApiService`:
  - `@GET("nutrition-target")` dan `@PUT("nutrition-target")`.
  - `@GET("orders/me")` dengan query `date=YYYY-MM-DD`; status tidak difilter di server.
- **Repository:** `data/repository/DefaultNutritionRepository.kt`:
  ```kotlin
  override suspend fun getOrdersForDate(date: LocalDate): ApiResult<List<Order>> = safeApiCall {
      api.getMyOrders(date = date.toString()).map { it.toDomain() }
  }
  ```
- **Tanggal WIB:** `core/util/WibTime.kt` (`today()` memakai zona `Asia/Jakarta`); backend memfilter `created_at` menurut tanggal WIB.

**Penjelasan:** Aplikasi meminta dua data ke server lewat Retrofit: target gizi pembeli dan daftar semua pesanan hari ini. "Hari ini" selalu memakai WIB, baik di aplikasi maupun di server, supaya pesanan jam 6 pagi WIB tidak terhitung sebagai hari kemarin, karena server menyimpan waktu dalam UTC. Status pesanan tidak difilter di server; `NutritionCalculator` yang memilih pesanan yang sudah diterima penjual. Semua perhitungan dilakukan di aplikasi, bukan oleh server.

---

## Daftar Screenshot untuk Laporan

Ambil dari emulator 1080×2400 setelah menjalankan `reset-demo`. Ukuran yang sama untuk semua gambar membuat laporan lebih rapi.

| No | Layar | Akun | Untuk bagian |
|---|---|---|---|
| 1 | Login | – | Umum |
| 2 | Beranda: grid stan dan badge keranjang | Daffa | Fungsi 1 B |
| 3 | Beranda: hasil pencarian "es" | Daffa | Fungsi 2 D (debounce) |
| 4 | Stan Dapur Gapura dengan chip kategori | Daffa | Fungsi 1 B |
| 5 | Bottom sheet detail Mie Ayam Solo (tabel gizi, lencana terverifikasi) | Daffa | Fungsi 2 B |
| 6 | Dialog "Keranjang berisi menu dari … Kosongkan dan ganti?" | Daffa | Fungsi 1 C |
| 7 | Keranjang: 1 Mie Ayam + 2 Es Teh, total Rp28.000 dan 890 kkal | Daffa | Fungsi 1 B, Fungsi 2 B |
| 8 | Keranjang setelah Es Teh ditambah menjadi 3 (Rp32.000, 1.010 kkal) | Daffa | Fungsi 2 C (reaktif) |
| 9 | Keranjang dengan QRIS simulasi | Daffa | Fungsi 1 B |
| 10 | Detail pesanan berstatus Menunggu dengan timeline | Daffa | Fungsi 1 |
| 11 | Pesanan Masuk penjual dengan badge Masuk | Gapura | Fungsi 1 |
| 12 | Detail pesanan penjual dengan tombol Terima/Tolak | Gapura | Fungsi 1 C |
| 13 | Dialog alasan penolakan | Gapura | Fungsi 1 C |
| 14 | Notifikasi "Pesanan Sedang Dimasak" di status bar | Daffa | Fungsi 1 D |
| 15 | Tab Pesanan: Dalam Proses dan Riwayat dengan chip status berwarna | Daffa | Fungsi 1 |
| 15a | Detail pesanan penjual: Tandai Siap Diambil dan Selesaikan Pesanan | Gapura | Fungsi 1 C |
| 16 | Gizi Hari Ini: campuran warna hijau/kuning/merah | Daffa | Fungsi 3 B |
| 17 | Form Target Gizi dengan error "lebih dari 0" | Daffa | Fungsi 3 C |
| 18 | Gizi Hari Ini setelah target diubah (status berubah) | Daffa | Fungsi 3 C |
| 19 | Menu & Stok penjual (edit stok inline) | Gapura | Pendukung |
| 20 | Form Menu Baru | Gapura | Pendukung |
| 21 | Detail menu admin: tombol Setujui nonaktif sebelum verifikasi | Admin | Pendukung |
| 22 | Detail menu admin setelah verifikasi dan disetujui | Admin | Pendukung |
| 23 | Hasil `pytest` 77 passed dan `testDebugUnitTest` | – | Pengujian |
| 24 | Swagger UI `/docs` | – | Pengujian / D |

Tips:
- **Screenshot dari terminal:** `adb exec-out screencap -p > nama.png`.
- **Membuat warna merah di Gizi Hari Ini (no. 16):** pesan beberapa Es Teh Manis (gula 30 g per porsi), lalu terima pesanan sebagai penjual, sehingga gula melewati 50 g.
