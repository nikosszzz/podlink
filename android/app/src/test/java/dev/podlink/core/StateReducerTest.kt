package dev.podlink.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StateReducerTest {
    @Test
    fun updatesBatteryAndChargingStateBelowFull() {
        val next = PodStateReducer.reduce(
            PodUiState(),
            AacpPacket.BatteryInfo(
                listOf(
                    BatteryMetric(BatterySlot.LEFT, 98, BatteryState.CHARGING),
                    BatteryMetric(BatterySlot.RIGHT, 4, BatteryState.OPTIMIZED_CHARGING),
                    BatteryMetric(BatterySlot.CASE, 100, BatteryState.CHARGING),
                ),
            ),
        )
        assertTrue(next.battery.left?.isCharging == true)
        assertTrue(next.battery.right?.isCharging == true)
        assertEquals(98, next.battery.left?.level)
        assertEquals(4, next.battery.right?.level)
        assertEquals(100, next.battery.case?.level)
    }

    @Test
    fun reducesListeningModeAndMask() {
        val s1 = PodStateReducer.reduce(
            PodUiState(),
            AacpPacket.ControlCommand(ControlId.LISTENING_MODE, ControlId.LISTENING_MODE.code, byteArrayOf(ListeningMode.ADAPTIVE.wireValue)),
        )
        assertEquals(ListeningMode.ADAPTIVE, s1.listeningMode)
        assertTrue(s1.listeningModeKnown)

        val s2 = PodStateReducer.reduce(
            s1,
            AacpPacket.ControlCommand(ControlId.CALL_MANAGEMENT_CONFIG, ControlId.CALL_MANAGEMENT_CONFIG.code, byteArrayOf(0x00, 0x02)),
        )
        assertTrue(s2.callManagementFlipped)
    }

    @Test
    fun identifiesPlainAirPods4WithoutAnc() {
        val model = PodCatalog.findByModelNumber("A3053")
        assertEquals("AirPods 4", model?.name)
        assertFalse(PodFeature.LISTENING_MODE in model!!.features)
        assertNull(PodCatalog.findByModelNumber("UNKNOWN"))
    }
}
