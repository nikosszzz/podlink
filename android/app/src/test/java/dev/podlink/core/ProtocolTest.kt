package dev.podlink.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {
    @Test
    fun parsesHandshakeAck() {
        assertEquals(AacpPacket.HandshakeAck, AacpCodec.parse(AacpSpec.HANDSHAKE_ACK_PREFIX + byteArrayOf(0x00, 0x00)))
    }

    @Test
    fun parsesBatteryInfo() {
        val raw = AacpSpec.FRAME_HEADER + byteArrayOf(
            PacketOpcode.BATTERY_INFO.code, 0x00, 0x02,
            BatterySlot.LEFT.code, 0x01, 0x55, BatteryState.CHARGING.code, 0x01,
            BatterySlot.CASE.code, 0x01, 0x64, BatteryState.DISCHARGING.code, 0x01,
        )
        val parsed = AacpCodec.parse(raw) as AacpPacket.BatteryInfo
        assertEquals(2, parsed.metrics.size)
        assertEquals(85, parsed.metrics[0].level)
        assertTrue(parsed.metrics[0].isCharging)
        assertEquals(100, parsed.metrics[1].level)
        assertFalse(parsed.metrics[1].isCharging)
    }

    @Test
    fun buildsAndParsesCustomEq() {
        val frame = AacpCodec.customEq(EqCurve(state = EqCurve.CUSTOM, low = 70, mid = 55, high = 35))
        val parsed = AacpCodec.parse(frame) as AacpPacket.CustomEqUpdate
        assertEquals(EqCurve(state = EqCurve.CUSTOM, low = 70, mid = 55, high = 35), parsed.curve)
    }

    @Test
    fun buildsAdaptiveStrengthInverse() {
        val frame = AacpCodec.adaptiveStrength(75)
        val parsed = AacpCodec.parse(frame) as AacpPacket.ControlCommand
        assertEquals(ControlId.AUTO_ANC_STRENGTH, parsed.id)
        assertArrayEquals(byteArrayOf(25.toByte()), parsed.value)
    }

    @Test
    fun buildsRenameWithinLimit() {
        val frame = AacpCodec.rename("My Pods")
        assertEquals(PacketOpcode.RENAME.code, frame[4])
        assertEquals(7, frame[7].toInt())
    }
}
