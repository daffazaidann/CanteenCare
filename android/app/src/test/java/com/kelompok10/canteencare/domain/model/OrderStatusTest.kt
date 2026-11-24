package com.kelompok10.canteencare.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tes pemetaan status dari API dan pengelompokan status untuk tab serta gizi. */
class OrderStatusTest {

    @Test
    fun `fromApi membaca semua status baru`() {
        assertEquals(OrderStatus.MENUNGGU, OrderStatus.fromApi("MENUNGGU"))
        assertEquals(OrderStatus.SEDANG_DIMASAK, OrderStatus.fromApi("SEDANG_DIMASAK"))
        assertEquals(OrderStatus.SIAP_DIAMBIL, OrderStatus.fromApi("SIAP_DIAMBIL"))
        assertEquals(OrderStatus.SELESAI, OrderStatus.fromApi("SELESAI"))
        assertEquals(OrderStatus.DITOLAK, OrderStatus.fromApi("DITOLAK"))
    }

    @Test
    fun `status lama DIKONFIRMASI tetap terbaca sebagai SEDANG_DIMASAK`() {
        assertEquals(OrderStatus.SEDANG_DIMASAK, OrderStatus.fromApi("DIKONFIRMASI"))
    }

    @Test
    fun `status tidak dikenal tidak membuat aplikasi crash`() {
        assertEquals(OrderStatus.MENUNGGU, OrderStatus.fromApi("STATUS_BARU_DARI_SERVER"))
    }

    @Test
    fun `pengelompokan status aktif riwayat dan gizi`() {
        assertTrue(OrderStatus.MENUNGGU.isActive)
        assertTrue(OrderStatus.SEDANG_DIMASAK.isActive)
        assertTrue(OrderStatus.SIAP_DIAMBIL.isActive)
        assertFalse(OrderStatus.SELESAI.isActive)
        assertFalse(OrderStatus.DITOLAK.isActive)

        assertTrue(OrderStatus.SELESAI.isHistory)
        assertTrue(OrderStatus.DITOLAK.isHistory)
        assertFalse(OrderStatus.MENUNGGU.isHistory)

        assertTrue(OrderStatus.SEDANG_DIMASAK.isCounted)
        assertTrue(OrderStatus.SIAP_DIAMBIL.isCounted)
        assertTrue(OrderStatus.SELESAI.isCounted)
        assertFalse(OrderStatus.MENUNGGU.isCounted)
        assertFalse(OrderStatus.DITOLAK.isCounted)
    }
}
