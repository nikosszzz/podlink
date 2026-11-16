package dev.podlink.core

import androidx.compose.runtime.Immutable
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class AttHandle(val code: Byte) {
    TRANSPARENCY(0x18),
    LOUD_SOUND_REDUCTION(0x1B),
    HEARING_AID(0x2A),
}

enum class AttCccdHandle(val code: Byte) {
    TRANSPARENCY((AttHandle.TRANSPARENCY.code + 1).toByte()),
    HEARING_AID((AttHandle.HEARING_AID.code + 1).toByte()),
}

@Immutable
data class TransparencyTuning(
    val enabled: Boolean,
    val leftEq: FloatArray,
    val rightEq: FloatArray,
    val leftAmplification: Float,
    val rightAmplification: Float,
    val leftTone: Float,
    val rightTone: Float,
    val leftConversationBoost: Boolean,
    val rightConversationBoost: Boolean,
    val leftAmbientNoiseReduction: Float,
    val rightAmbientNoiseReduction: Float,
    val netAmplification: Float,
    val balance: Float,
    val ownVoiceAmplification: Float? = null,
) {
    fun toAttBytes(): ByteArray {
        val capacity = if (ownVoiceAmplification != null) 104 else 100
        val buf = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN)
        buf.putFloat(if (enabled) 1f else 0f)
        for (i in 0 until 8) buf.putFloat(leftEq.getOrElse(i) { 0f })
        buf.putFloat(leftAmplification)
        buf.putFloat(leftTone)
        buf.putFloat(if (leftConversationBoost) 1f else 0f)
        buf.putFloat(leftAmbientNoiseReduction)
        for (i in 0 until 8) buf.putFloat(rightEq.getOrElse(i) { 0f })
        buf.putFloat(rightAmplification)
        buf.putFloat(rightTone)
        buf.putFloat(if (rightConversationBoost) 1f else 0f)
        buf.putFloat(rightAmbientNoiseReduction)
        if (ownVoiceAmplification != null) buf.putFloat(ownVoiceAmplification)
        return buf.array()
    }

    override fun equals(other: Any?): Boolean = other is TransparencyTuning &&
        enabled == other.enabled &&
        leftAmplification == other.leftAmplification &&
        rightAmplification == other.rightAmplification &&
        leftTone == other.leftTone &&
        rightTone == other.rightTone &&
        leftConversationBoost == other.leftConversationBoost &&
        rightConversationBoost == other.rightConversationBoost &&
        leftAmbientNoiseReduction == other.leftAmbientNoiseReduction &&
        rightAmbientNoiseReduction == other.rightAmbientNoiseReduction &&
        leftEq.contentEquals(other.leftEq) &&
        rightEq.contentEquals(other.rightEq) &&
        ownVoiceAmplification == other.ownVoiceAmplification

    override fun hashCode(): Int {
        var r = enabled.hashCode()
        r = 31 * r + leftAmplification.hashCode()
        r = 31 * r + rightAmplification.hashCode()
        r = 31 * r + leftTone.hashCode()
        r = 31 * r + rightTone.hashCode()
        r = 31 * r + leftConversationBoost.hashCode()
        r = 31 * r + rightConversationBoost.hashCode()
        r = 31 * r + leftAmbientNoiseReduction.hashCode()
        r = 31 * r + rightAmbientNoiseReduction.hashCode()
        r = 31 * r + leftEq.contentHashCode()
        r = 31 * r + rightEq.contentHashCode()
        r = 31 * r + (ownVoiceAmplification?.hashCode() ?: 0)
        return r
    }

    companion object {
        fun parse(data: ByteArray): TransparencyTuning? {
            if (data.size < 100) return null
            val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val enabled = b.float > 0.5f
            val leftEq = FloatArray(8) { b.float }
            val leftAmp = b.float
            val leftTone = b.float
            val leftBoost = b.float > 0.5f
            val leftAnr = b.float
            val rightEq = FloatArray(8) { b.float }
            val rightAmp = b.float
            val rightTone = b.float
            val rightBoost = b.float > 0.5f
            val rightAnr = b.float
            val ownVoice = if (b.remaining() >= 4) b.float else null
            return TransparencyTuning(
                enabled = enabled,
                leftEq = leftEq,
                rightEq = rightEq,
                leftAmplification = leftAmp,
                rightAmplification = rightAmp,
                leftTone = leftTone,
                rightTone = rightTone,
                leftConversationBoost = leftBoost,
                rightConversationBoost = rightBoost,
                leftAmbientNoiseReduction = leftAnr,
                rightAmbientNoiseReduction = rightAnr,
                netAmplification = ((leftAmp + rightAmp) / 2f).coerceIn(-1f, 1f),
                balance = (rightAmp - leftAmp).coerceIn(-1f, 1f),
                ownVoiceAmplification = ownVoice,
            )
        }
    }
}

@Immutable
data class HearingAidTuning(
    val leftEq: FloatArray,
    val rightEq: FloatArray,
    val leftAmplification: Float,
    val rightAmplification: Float,
    val leftTone: Float,
    val rightTone: Float,
    val leftConversationBoost: Boolean,
    val rightConversationBoost: Boolean,
    val leftAmbientNoiseReduction: Float,
    val rightAmbientNoiseReduction: Float,
    val netAmplification: Float,
    val balance: Float,
    val ownVoiceAmplification: Float,
    val rawBytes: ByteArray = ByteArray(0),
) {
    fun toUpdatedAttBytes(base: ByteArray = rawBytes): ByteArray? {
        if (base.size < 104) return null
        val out = base.copyOf()
        val buf = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(2, 0x64)
        for (i in 0 until 8) buf.putFloat(4 + i * 4, leftEq.getOrElse(i) { 0f })
        buf.putFloat(36, leftAmplification)
        buf.putFloat(40, leftTone)
        buf.putFloat(44, if (leftConversationBoost) 1f else 0f)
        buf.putFloat(48, leftAmbientNoiseReduction)
        for (i in 0 until 8) buf.putFloat(52 + i * 4, rightEq.getOrElse(i) { 0f })
        buf.putFloat(84, rightAmplification)
        buf.putFloat(88, rightTone)
        buf.putFloat(92, if (rightConversationBoost) 1f else 0f)
        buf.putFloat(96, rightAmbientNoiseReduction)
        buf.putFloat(100, ownVoiceAmplification)
        return out
    }

    override fun equals(other: Any?): Boolean = other is HearingAidTuning &&
        leftAmplification == other.leftAmplification &&
        rightAmplification == other.rightAmplification &&
        leftTone == other.leftTone &&
        rightTone == other.rightTone &&
        leftConversationBoost == other.leftConversationBoost &&
        rightConversationBoost == other.rightConversationBoost &&
        leftAmbientNoiseReduction == other.leftAmbientNoiseReduction &&
        rightAmbientNoiseReduction == other.rightAmbientNoiseReduction &&
        leftEq.contentEquals(other.leftEq) &&
        rightEq.contentEquals(other.rightEq) &&
        ownVoiceAmplification == other.ownVoiceAmplification &&
        rawBytes.contentEquals(other.rawBytes)

    override fun hashCode(): Int {
        var r = leftAmplification.hashCode()
        r = 31 * r + rightAmplification.hashCode()
        r = 31 * r + leftTone.hashCode()
        r = 31 * r + rightTone.hashCode()
        r = 31 * r + leftConversationBoost.hashCode()
        r = 31 * r + rightConversationBoost.hashCode()
        r = 31 * r + leftAmbientNoiseReduction.hashCode()
        r = 31 * r + rightAmbientNoiseReduction.hashCode()
        r = 31 * r + leftEq.contentHashCode()
        r = 31 * r + rightEq.contentHashCode()
        r = 31 * r + ownVoiceAmplification.hashCode()
        r = 31 * r + rawBytes.contentHashCode()
        return r
    }

    companion object {
        fun parse(data: ByteArray): HearingAidTuning? {
            if (data.size < 104) return null
            val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            b.get()
            b.get()
            b.getShort()
            val leftEq = FloatArray(8) { b.float }
            val leftAmp = b.float
            val leftTone = b.float
            val leftBoost = b.float > 0.5f
            val leftAnr = b.float
            val rightEq = FloatArray(8) { b.float }
            val rightAmp = b.float
            val rightTone = b.float
            val rightBoost = b.float > 0.5f
            val rightAnr = b.float
            val ownVoice = b.float
            return HearingAidTuning(
                leftEq = leftEq,
                rightEq = rightEq,
                leftAmplification = leftAmp,
                rightAmplification = rightAmp,
                leftTone = leftTone,
                rightTone = rightTone,
                leftConversationBoost = leftBoost,
                rightConversationBoost = rightBoost,
                leftAmbientNoiseReduction = leftAnr,
                rightAmbientNoiseReduction = rightAnr,
                netAmplification = ((leftAmp + rightAmp) / 2f).coerceIn(-1f, 1f),
                balance = (rightAmp - leftAmp).coerceIn(-1f, 1f),
                ownVoiceAmplification = ownVoice,
                rawBytes = data.copyOf(),
            )
        }
    }
}
