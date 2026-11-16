package dev.podlink.core

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

object AacpSpec {
    const val AACP_PSM: Int = 0x1001
    const val ATT_PSM: Int = 31
    const val SERVICE_UUID: String = "74ec2172-0bad-4d01-8f77-997b2be0722a"
    const val ATT_ZERO_UUID: String = "00000000-0000-0000-0000-000000000000"
    const val DEFAULT_MODE_MASK: Int = 0x0E

    val FRAME_HEADER: ByteArray = byteArrayOf(0x04, 0x00, 0x04, 0x00)
    val HANDSHAKE_FRAME: ByteArray = byteArrayOf(
        0x00, 0x00, 0x04, 0x00,
        0x01, 0x00, 0x02, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
    )
    val HANDSHAKE_ACK_PREFIX: ByteArray = byteArrayOf(0x01, 0x00, 0x04, 0x00)
}

enum class PacketOpcode(val code: Byte) {
    BATTERY_INFO(0x04),
    EAR_DETECTION(0x06),
    CONTROL_COMMAND(0x09),
    TIPI_3(0x0C),
    AUDIO_SOURCE(0x0E),
    REQUEST_NOTIFICATIONS(0x0F),
    SMART_ROUTING(0x10),
    SMART_ROUTING_RESP(0x11),
    SEND_CONNECTED_MAC(0x14),
    HEAD_TRACKING(0x17),
    STEM_PRESS(0x19),
    RENAME(0x1A),
    INFORMATION(0x1D),
    EQ_UNLOCK(0x29),
    CONNECTED_DEVICES(0x2E),
    PROXIMITY_KEYS_REQ(0x30),
    PROXIMITY_KEYS_RSP(0x31),
    CONVERSATION_AWARENESS(0x4B),
    SET_FEATURE_FLAGS(0x4D),
    EQ_DATA(0x53),
    CUSTOM_EQ(0x63);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromByte(value: Byte): PacketOpcode? = byCode[value]
    }
}

enum class ControlId(val code: Byte) {
    MIC_MODE(0x01),
    BUTTON_SEND_MODE(0x05),
    OWNS_CONNECTION(0x06),
    EAR_DETECTION_CONFIG(0x0A),
    LISTENING_MODE(0x0D),
    VOICE_TRIGGER(0x12),
    SINGLE_CLICK_MODE(0x14),
    DOUBLE_CLICK_MODE(0x15),
    CLICK_HOLD_MODE(0x16),
    DOUBLE_CLICK_INTERVAL(0x17),
    CLICK_HOLD_INTERVAL(0x18),
    LISTENING_MODE_CONFIGS(0x1A),
    ONE_BUD_ANC_MODE(0x1B),
    CROWN_ROTATION_DIRECTION(0x1C),
    AUTO_ANSWER_MODE(0x1E),
    CHIME_VOLUME(0x1F),
    AUTOMATIC_CONNECTION_CONFIG(0x20),
    VOLUME_SWIPE_INTERVAL(0x23),
    CALL_MANAGEMENT_CONFIG(0x24),
    VOLUME_SWIPE_MODE(0x25),
    ADAPTIVE_VOLUME_CONFIG(0x26),
    SOFTWARE_MUTE_CONFIG(0x27),
    CONVERSATION_DETECT_CONFIG(0x28),
    SSL(0x29),
    HEARING_AID(0x2C),
    AUTO_ANC_STRENGTH(0x2E),
    HPS_GAIN_SWIPE(0x2F),
    HRM_STATE(0x30),
    IN_CASE_TONE_CONFIG(0x31),
    SIRI_MULTITONE_CONFIG(0x32),
    HEARING_ASSIST_CONFIG(0x33),
    ALLOW_OFF_OPTION(0x34),
    SLEEP_DETECTION_CONFIG(0x35),
    ALLOW_AUTO_CONNECT(0x36),
    PPE_TOGGLE_CONFIG(0x37),
    PPE_CAP_LEVEL_CONFIG(0x38),
    STEM_CONFIG(0x39),
    DYNAMIC_END_OF_CHARGE(0x3B);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromByte(value: Byte): ControlId? = byCode[value]
    }
}

enum class ListeningMode(val wireValue: Byte, val cycleMask: Int) {
    OFF(0x01, 0x01),
    NOISE_CANCELLATION(0x02, 0x02),
    TRANSPARENCY(0x03, 0x04),
    ADAPTIVE(0x04, 0x08);

    companion object {
        private val byWire = entries.associateBy { it.wireValue }
        private val cycleOrder = listOf(OFF, TRANSPARENCY, ADAPTIVE, NOISE_CANCELLATION)

        fun fromWire(value: Byte): ListeningMode? = byWire[value]

        fun nextInCycle(current: ListeningMode, mask: Int, allowOff: Boolean): ListeningMode {
            val candidates = cycleOrder.filter { mode ->
                (mode != OFF || allowOff) && (mask and mode.cycleMask) != 0
            }.ifEmpty {
                cycleOrder.filter { mode -> mode != OFF || allowOff }
            }
            val currentIdx = candidates.indexOf(current)
            return candidates[(currentIdx + 1) % candidates.size]
        }
    }
}

enum class BatterySlot(val code: Byte) {
    HEADPHONE(0x01),
    RIGHT(0x02),
    LEFT(0x04),
    CASE(0x08);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromByte(value: Byte): BatterySlot? = byCode[value]
    }
}

enum class BatteryState(val code: Byte) {
    CHARGING(0x01),
    DISCHARGING(0x02),
    DISCONNECTED(0x04),
    OPTIMIZED_CHARGING(0x05);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromByte(value: Byte): BatteryState? = byCode[value]
    }
}

enum class WearState(val code: Byte) {
    IN_EAR(0x00),
    OUT_OF_EAR(0x01),
    IN_CASE(0x02),
    DISCONNECTED(0x03);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromByte(value: Byte): WearState? = byCode[value]
    }
}

enum class StemPress(val code: Byte) {
    SINGLE_PRESS(0x05),
    DOUBLE_PRESS(0x06),
    TRIPLE_PRESS(0x07),
    LONG_PRESS(0x08);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromByte(value: Byte): StemPress? = byCode[value]
    }
}

enum class StemSide(val code: Byte) {
    LEFT(0x01),
    RIGHT(0x02);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromByte(value: Byte): StemSide? = byCode[value]
    }
}

enum class AudioRouteType(val code: Byte) {
    NONE(0x00),
    CALL(0x01),
    MEDIA(0x02);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromByte(value: Byte): AudioRouteType? = byCode[value]
    }
}

enum class ProximityKey(val code: Byte) {
    IRK(0x01),
    ENC_KEY(0x04);

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromByte(value: Byte): ProximityKey? = byCode[value]
    }
}

sealed interface AacpPacket {
    data object HandshakeAck : AacpPacket
    data class BatteryInfo(val metrics: List<BatteryMetric>) : AacpPacket

    data class ControlCommand(val id: ControlId?, val rawId: Byte, val value: ByteArray) : AacpPacket {
        override fun equals(other: Any?): Boolean =
            other is ControlCommand && rawId == other.rawId && value.contentEquals(other.value)

        override fun hashCode(): Int = 31 * rawId.toInt() + value.contentHashCode()
    }

    data class EarDetection(val wear: WearSnapshot) : AacpPacket
    data class ConversationAwareness(val status: Byte) : AacpPacket
    data class Information(val metadata: DeviceMetadata) : AacpPacket
    data class ProximityKeys(val keys: Map<ProximityKey, ByteArray>) : AacpPacket
    data class StemEvent(val press: StemPress, val side: StemSide) : AacpPacket
    data class AudioSourceUpdate(val source: AudioRoute) : AacpPacket
    data class ConnectedDevices(val peers: List<PeerDevice>) : AacpPacket

    data class SmartRouting(
        val rawText: String,
        val ownershipFalseRequested: Boolean,
        val senderMac: String? = null,
        val inferredType: String? = null,
    ) : AacpPacket

    data class CustomEqUpdate(val curve: EqCurve) : AacpPacket
    data class HeadTrackingSample(val sample: MotionSample) : AacpPacket

    data class Accommodation(
        val onMedia: Boolean,
        val onPhone: Boolean,
        val bands: FloatArray,
    ) : AacpPacket {
        override fun equals(other: Any?): Boolean =
            other is Accommodation && onMedia == other.onMedia && onPhone == other.onPhone && bands.contentEquals(other.bands)

        override fun hashCode(): Int {
            var res = onMedia.hashCode()
            res = 31 * res + onPhone.hashCode()
            res = 31 * res + bands.contentHashCode()
            return res
        }
    }

    data class EqRaw(val payload: ByteArray) : AacpPacket {
        override fun equals(other: Any?): Boolean = other is EqRaw && payload.contentEquals(other.payload)
        override fun hashCode(): Int = payload.contentHashCode()
    }

    data class Unknown(val opcode: Byte?, val raw: ByteArray) : AacpPacket {
        override fun equals(other: Any?): Boolean =
            other is Unknown && opcode == other.opcode && raw.contentEquals(other.raw)

        override fun hashCode(): Int = 31 * (opcode?.toInt() ?: -1) + raw.contentHashCode()
    }
}

object AacpCodec {
    fun handshake(): ByteArray = AacpSpec.HANDSHAKE_FRAME.copyOf()

    fun setFeatureFlags(flags: Byte = 0xFF.toByte()): ByteArray =
        frame(PacketOpcode.SET_FEATURE_FLAGS, byteArrayOf(flags, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))

    fun requestNotifications(earDetection: Boolean = true): ByteArray = frame(
        PacketOpcode.REQUEST_NOTIFICATIONS,
        byteArrayOf(0xFF.toByte(), 0xFF.toByte(), if (earDetection) 0xFF.toByte() else 0xFD.toByte(), 0xFF.toByte()),
    )

    fun requestProximityKeys(
        keys: Collection<ProximityKey> = listOf(ProximityKey.IRK, ProximityKey.ENC_KEY),
    ): ByteArray {
        val mask = keys.fold(0) { acc, item -> acc or (item.code.toInt() and 0xFF) }.toByte()
        return frame(PacketOpcode.PROXIMITY_KEYS_REQ, byteArrayOf(mask, 0x00))
    }

    fun eqUnlock(): ByteArray = frame(
        PacketOpcode.EQ_UNLOCK,
        byteArrayOf(0x00, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
    )

    fun control(id: ControlId, value: ByteArray): ByteArray {
        val body = ByteArray(5)
        body[0] = id.code
        for (i in 0 until minOf(4, value.size)) body[1 + i] = value[i]
        return frame(PacketOpcode.CONTROL_COMMAND, body)
    }

    fun control(id: ControlId, enabled: Boolean): ByteArray =
        control(id, byteArrayOf(if (enabled) 0x01 else 0x02))

    fun control(id: ControlId, byteVal: Byte): ByteArray =
        control(id, byteArrayOf(byteVal))

    fun listeningMode(mode: ListeningMode): ByteArray =
        control(ControlId.LISTENING_MODE, mode.wireValue)

    fun customEq(curve: EqCurve): ByteArray = frame(
        PacketOpcode.CUSTOM_EQ,
        byteArrayOf(0x05, 0x00, 0x01, curve.state.toByte(), curve.low.toByte(), curve.mid.toByte(), curve.high.toByte()),
    )

    fun accommodation(bands: FloatArray, onPhone: Boolean, onMedia: Boolean): ByteArray {
        require(bands.size == 8) { "EQ must contain 8 floats, got ${bands.size}" }
        val header = byteArrayOf(
            0x04, 0x00, 0x04, 0x00, PacketOpcode.EQ_DATA.code, 0x00, 0x84.toByte(), 0x00, 0x02, 0x02,
            if (onPhone) 0x01 else 0x02,
            if (onMedia) 0x01 else 0x02,
        )
        val buffer = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN)
        repeat(4) { for (band in bands) buffer.putFloat(band) }
        return header + buffer.array()
    }

    fun startHeadTracking(alternate: Boolean = true): ByteArray = frame(
        PacketOpcode.HEAD_TRACKING,
        if (alternate) {
            byteArrayOf(0x00, 0x00, 0x10, 0x00, 0x0F, 0x00, 0x08, 0x73, 0x42, 0x0B, 0x08, 0x10, 0x10, 0x02, 0x1A, 0x05, 0x01, 0x40, 0x9C.toByte(), 0x00, 0x00)
        } else {
            byteArrayOf(0x00, 0x00, 0x10, 0x00, 0x10, 0x00, 0x08, 0xA1.toByte(), 0x02, 0x42, 0x0B, 0x08, 0x0E, 0x10, 0x02, 0x1A, 0x05, 0x01, 0x40, 0x9C.toByte(), 0x00, 0x00)
        },
    )

    fun stopHeadTracking(alternate: Boolean = true): ByteArray = frame(
        PacketOpcode.HEAD_TRACKING,
        if (alternate) {
            byteArrayOf(0x00, 0x00, 0x10, 0x00, 0x0F, 0x00, 0x08, 0x75, 0x42, 0x0B, 0x08, 0x10, 0x10, 0x02, 0x1A, 0x05, 0x01, 0x00, 0x00, 0x00, 0x00)
        } else {
            byteArrayOf(0x00, 0x00, 0x10, 0x00, 0x11, 0x00, 0x08, 0x7E, 0x10, 0x02, 0x42, 0x0B, 0x08, 0x4E, 0x10, 0x02, 0x1A, 0x05, 0x01, 0x00, 0x00, 0x00, 0x00)
        },
    )

    fun stemConfig(single: Boolean, doublePress: Boolean, triple: Boolean, longPress: Boolean): ByteArray {
        val bits = (if (single) 0x01 else 0) or (if (doublePress) 0x02 else 0) or (if (triple) 0x04 else 0) or (if (longPress) 0x08 else 0)
        return control(ControlId.STEM_CONFIG, bits.toByte())
    }

    fun chimeVolume(level: Int): ByteArray =
        control(ControlId.CHIME_VOLUME, byteArrayOf(level.coerceIn(0, 100).toByte(), 0x50))

    fun adaptiveStrength(sliderLevel: Int): ByteArray =
        control(ControlId.AUTO_ANC_STRENGTH, byteArrayOf((100 - sliderLevel).coerceIn(0, 100).toByte()))

    fun callManagement(flipped: Boolean): ByteArray =
        control(ControlId.CALL_MANAGEMENT_CONFIG, byteArrayOf(0x00, if (flipped) 0x02 else 0x03))

    fun hearingAidFrames(enabled: Boolean): List<ByteArray> = listOf(
        control(ControlId.HEARING_AID, byteArrayOf(0x01, if (enabled) 0x01 else 0x02)),
        control(ControlId.HEARING_ASSIST_CONFIG, enabled),
    )

    fun rename(name: String): ByteArray {
        val bytes = name.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= 0xFF) { "Name exceeds 255 UTF-8 bytes" }
        val data = ByteArray(3 + bytes.size).apply {
            this[0] = 0x01
            this[1] = bytes.size.toByte()
            this[2] = 0x00
            bytes.copyInto(this, 3)
        }
        return frame(PacketOpcode.RENAME, data)
    }

    fun parse(packet: ByteArray): AacpPacket {
        if (packet.startsWith(AacpSpec.HANDSHAKE_ACK_PREFIX)) return AacpPacket.HandshakeAck
        if (!packet.startsWith(AacpSpec.FRAME_HEADER) || packet.size < 5) {
            return AacpPacket.Unknown(null, packet)
        }
        val opcode = packet[4]
        val payload = packet.copyOfRange(4, packet.size)
        return when (PacketOpcode.fromByte(opcode)) {
            PacketOpcode.BATTERY_INFO -> parseBattery(payload, packet)
            PacketOpcode.CONTROL_COMMAND -> parseControl(payload, packet)
            PacketOpcode.EAR_DETECTION -> parseWear(packet)
            PacketOpcode.CONVERSATION_AWARENESS ->
                if (packet.size == 10) AacpPacket.ConversationAwareness(packet[9]) else AacpPacket.Unknown(opcode, packet)
            PacketOpcode.INFORMATION -> parseInfo(payload, packet)
            PacketOpcode.PROXIMITY_KEYS_RSP -> parseProximityKeys(payload, packet)
            PacketOpcode.STEM_PRESS -> parseStemPress(payload, packet)
            PacketOpcode.AUDIO_SOURCE -> parseAudioSource(payload, packet)
            PacketOpcode.CONNECTED_DEVICES -> parsePeers(payload, packet)
            PacketOpcode.SMART_ROUTING_RESP -> parseSmartRouting(packet)
            PacketOpcode.HEAD_TRACKING -> parseHeadTracking(packet)
            PacketOpcode.EQ_DATA -> parseAccommodation(payload, packet)
            PacketOpcode.CUSTOM_EQ -> parseCustomEq(packet)
            else -> AacpPacket.Unknown(opcode, packet)
        }
    }

    private fun frame(opcode: PacketOpcode, data: ByteArray): ByteArray {
        val header = AacpSpec.FRAME_HEADER
        return ByteArray(header.size + 2 + data.size).apply {
            header.copyInto(this, 0)
            this[4] = opcode.code
            this[5] = 0x00
            data.copyInto(this, 6)
        }
    }

    private fun parseBattery(payload: ByteArray, raw: ByteArray): AacpPacket {
        if (payload.size < 3) return AacpPacket.Unknown(payload.firstOrNull(), raw)
        val count = payload[2].toInt() and 0xFF
        if (payload.size < 3 + count * 5) return AacpPacket.Unknown(payload[0], raw)
        val list = buildList(count) {
            for (i in 0 until count) {
                val base = 3 + i * 5
                val slot = BatterySlot.fromByte(payload[base]) ?: continue
                val level = payload[base + 2].toInt() and 0xFF
                val state = BatteryState.fromByte(payload[base + 3]) ?: continue
                add(BatteryMetric(slot, level, state))
            }
        }
        return AacpPacket.BatteryInfo(list)
    }

    private fun parseControl(payload: ByteArray, raw: ByteArray): AacpPacket {
        if (payload.size < 7) return AacpPacket.Unknown(payload.firstOrNull(), raw)
        val idByte = payload[2]
        val trimmed = trimZeros(payload.copyOfRange(3, 7))
        return AacpPacket.ControlCommand(ControlId.fromByte(idByte), idByte, trimmed)
    }

    private fun parseWear(packet: ByteArray): AacpPacket {
        if (packet.size < 8) return AacpPacket.Unknown(packet.getOrNull(4), packet)
        val primary = WearState.fromByte(packet[6]) ?: WearState.OUT_OF_EAR
        val secondary = WearState.fromByte(packet[7]) ?: WearState.OUT_OF_EAR
        return AacpPacket.EarDetection(WearSnapshot(primary, secondary))
    }

    private fun parseInfo(payload: ByteArray, raw: ByteArray): AacpPacket {
        if (payload.size < 6) return AacpPacket.Unknown(payload.firstOrNull(), raw)
        val data = payload.copyOfRange(4, payload.size)
        var i = 0
        while (i < data.size && data[i] != 0.toByte()) i++
        val strings = mutableListOf<String>()
        while (i < data.size) {
            while (i < data.size && data[i] == 0.toByte()) i++
            if (i >= data.size) break
            val start = i
            while (i < data.size && data[i] != 0.toByte()) i++
            strings += String(data, start, i - start, StandardCharsets.UTF_8)
        }
        if (strings.isNotEmpty()) strings.removeAt(0)
        return AacpPacket.Information(
            DeviceMetadata(
                name = strings.getOrElse(0) { "" },
                modelNumber = strings.getOrElse(1) { "" },
                manufacturer = strings.getOrElse(2) { "" },
                serialNumber = strings.getOrElse(3) { "" },
                version1 = strings.getOrElse(4) { "" },
                version2 = strings.getOrElse(5) { "" },
                hardwareRevision = strings.getOrElse(6) { "" },
                updaterId = strings.getOrElse(7) { "" },
                leftSerial = strings.getOrElse(8) { "" },
                rightSerial = strings.getOrElse(9) { "" },
                version3 = strings.getOrElse(10) { "" },
            ),
        )
    }

    private fun parseProximityKeys(payload: ByteArray, raw: ByteArray): AacpPacket {
        if (payload.size < 4) return AacpPacket.Unknown(payload.firstOrNull(), raw)
        val count = payload[2].toInt() and 0xFF
        var off = 3
        val keys = linkedMapOf<ProximityKey, ByteArray>()
        repeat(count) {
            if (off + 3 >= payload.size) return AacpPacket.Unknown(payload[0], raw)
            val keyByte = payload[off]
            val len = payload[off + 2].toInt() and 0xFF
            off += 4
            if (off + len > payload.size) return AacpPacket.Unknown(payload[0], raw)
            ProximityKey.fromByte(keyByte)?.let { keys[it] = payload.copyOfRange(off, off + len) }
            off += len
        }
        return AacpPacket.ProximityKeys(keys)
    }

    private fun parseStemPress(payload: ByteArray, raw: ByteArray): AacpPacket {
        if (payload.size < 4) return AacpPacket.Unknown(payload.firstOrNull(), raw)
        val press = StemPress.fromByte(payload[2])
        val side = StemSide.fromByte(payload[3])
        return if (press != null && side != null) {
            AacpPacket.StemEvent(press, side)
        } else {
            AacpPacket.Unknown(payload[0], raw)
        }
    }

    private fun parseAudioSource(payload: ByteArray, raw: ByteArray): AacpPacket {
        if (payload.size < 9) return AacpPacket.Unknown(payload.firstOrNull(), raw)
        val mac = "%02X:%02X:%02X:%02X:%02X:%02X".format(
            payload[7], payload[6], payload[5], payload[4], payload[3], payload[2],
        )
        return AacpPacket.AudioSourceUpdate(AudioRoute(mac, AudioRouteType.fromByte(payload[8]) ?: AudioRouteType.NONE))
    }

    private fun parsePeers(payload: ByteArray, raw: ByteArray): AacpPacket {
        if (payload.size < 3) return AacpPacket.Unknown(payload.firstOrNull(), raw)
        val count = payload[2].toInt() and 0xFF
        if (payload.size < 5 + count * 8) return AacpPacket.Unknown(payload[0], raw)
        val peers = buildList(count) {
            for (i in 0 until count) {
                val base = 5 + i * 8
                val mac = "%02X:%02X:%02X:%02X:%02X:%02X".format(
                    payload[base], payload[base + 1], payload[base + 2],
                    payload[base + 3], payload[base + 4], payload[base + 5],
                )
                add(PeerDevice(mac, payload[base + 6], payload[base + 7]))
            }
        }
        return AacpPacket.ConnectedDevices(peers)
    }

    private fun parseSmartRouting(packet: ByteArray): AacpPacket {
        val text = if (packet.size > 6) String(packet, 6, packet.size - 6, StandardCharsets.UTF_8) else ""
        val mac = if (packet.size >= 12) {
            packet.sliceArray(6..11).reversedArray().joinToString(":") { "%02X".format(it) }
        } else {
            null
        }
        val type = when {
            "iPad" in text -> "iPad"
            "Mac" in text -> "Mac"
            "iPhone" in text -> "iPhone"
            "Linux" in text -> "Linux"
            "Android" in text -> "Android"
            else -> null
        }
        return AacpPacket.SmartRouting(text, text.contains("SetOwnershipToFalse"), mac, type)
    }

    private fun parseHeadTracking(packet: ByteArray): AacpPacket {
        if (packet.size < 70) return AacpPacket.Unknown(PacketOpcode.HEAD_TRACKING.code, packet)
        return AacpPacket.HeadTrackingSample(
            MotionSample(
                o1 = int16(packet[43], packet[44]),
                o2 = int16(packet[45], packet[46]),
                o3 = int16(packet[47], packet[48]),
                horizontalAccel = int16(packet[51], packet[52]).toFloat(),
                verticalAccel = int16(packet[53], packet[54]).toFloat(),
            ),
        )
    }

    private fun parseAccommodation(payload: ByteArray, packet: ByteArray): AacpPacket {
        if (packet.size != 140 || packet[6] != 0x84.toByte()) return AacpPacket.EqRaw(payload)
        val buf = ByteBuffer.wrap(packet, 12, 32).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        return AacpPacket.Accommodation(
            onMedia = packet[10] == 0x01.toByte(),
            onPhone = packet[11] == 0x01.toByte(),
            bands = FloatArray(8) { idx -> buf.get(idx) },
        )
    }

    private fun parseCustomEq(packet: ByteArray): AacpPacket {
        if (packet.size < 13) return AacpPacket.CustomEqUpdate(EqCurve())
        val data = packet.copyOfRange(6, packet.size)
        if (data.size < 7) return AacpPacket.CustomEqUpdate(EqCurve())
        val state = data[3].toInt() and 0xFF
        return AacpPacket.CustomEqUpdate(
            EqCurve(
                state = if (state == EqCurve.CUSTOM) EqCurve.CUSTOM else EqCurve.RECOMMENDED,
                low = (data[4].toInt() and 0xFF).coerceIn(0, 100),
                mid = (data[5].toInt() and 0xFF).coerceIn(0, 100),
                high = (data[6].toInt() and 0xFF).coerceIn(0, 100),
            ),
        )
    }

    private fun int16(b1: Byte, b2: Byte): Int = (b2.toInt() shl 8) or (b1.toInt() and 0xFF)

    fun trimZeros(bytes: ByteArray): ByteArray {
        val last = bytes.indexOfLast { it != 0.toByte() }
        return if (last >= 0) bytes.copyOfRange(0, last + 1) else byteArrayOf(0)
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        for (i in prefix.indices) if (this[i] != prefix[i]) return false
        return true
    }
}
