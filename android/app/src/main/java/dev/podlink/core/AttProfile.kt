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
        writeChannels(
            buf = buf,
            leftEq = leftEq,
            leftAmp = leftAmplification,
            leftTone = leftTone,
            leftBoost = leftConversationBoost,
            leftAnr = leftAmbientNoiseReduction,
            rightEq = rightEq,
            rightAmp = rightAmplification,
            rightTone = rightTone,
            rightBoost = rightConversationBoost,
            rightAnr = rightAmbientNoiseReduction,
            ownVoice = ownVoiceAmplification,
        )
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
            val ch = readChannels(b, readOptionalOwnVoice = true)
            return TransparencyTuning(
                enabled = enabled,
                leftEq = ch.leftEq,
                rightEq = ch.rightEq,
                leftAmplification = ch.leftAmp,
                rightAmplification = ch.rightAmp,
                leftTone = ch.leftTone,
                rightTone = ch.rightTone,
                leftConversationBoost = ch.leftBoost,
                rightConversationBoost = ch.rightBoost,
                leftAmbientNoiseReduction = ch.leftAnr,
                rightAmbientNoiseReduction = ch.rightAnr,
                netAmplification = ch.netAmplification,
                balance = ch.balance,
                ownVoiceAmplification = ch.ownVoice,
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
        buf.position(4)
        writeChannels(
            buf = buf,
            leftEq = leftEq,
            leftAmp = leftAmplification,
            leftTone = leftTone,
            leftBoost = leftConversationBoost,
            leftAnr = leftAmbientNoiseReduction,
            rightEq = rightEq,
            rightAmp = rightAmplification,
            rightTone = rightTone,
            rightBoost = rightConversationBoost,
            rightAnr = rightAmbientNoiseReduction,
            ownVoice = ownVoiceAmplification,
        )
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
            b.position(4)
            val ch = readChannels(b, readOptionalOwnVoice = false)
            return HearingAidTuning(
                leftEq = ch.leftEq,
                rightEq = ch.rightEq,
                leftAmplification = ch.leftAmp,
                rightAmplification = ch.rightAmp,
                leftTone = ch.leftTone,
                rightTone = ch.rightTone,
                leftConversationBoost = ch.leftBoost,
                rightConversationBoost = ch.rightBoost,
                leftAmbientNoiseReduction = ch.leftAnr,
                rightAmbientNoiseReduction = ch.rightAnr,
                netAmplification = ch.netAmplification,
                balance = ch.balance,
                ownVoiceAmplification = ch.ownVoice ?: 0f,
                rawBytes = data.copyOf(),
            )
        }
    }
}

private class AttChannels(
    val leftEq: FloatArray,
    val leftAmp: Float,
    val leftTone: Float,
    val leftBoost: Boolean,
    val leftAnr: Float,
    val rightEq: FloatArray,
    val rightAmp: Float,
    val rightTone: Float,
    val rightBoost: Boolean,
    val rightAnr: Float,
    val ownVoice: Float?,
) {
    val netAmplification: Float get() = ((leftAmp + rightAmp) / 2f).coerceIn(-1f, 1f)
    val balance: Float get() = (rightAmp - leftAmp).coerceIn(-1f, 1f)
}

private fun readChannels(b: ByteBuffer, readOptionalOwnVoice: Boolean): AttChannels {
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
    val ownVoice = if (!readOptionalOwnVoice || b.remaining() >= 4) b.float else null
    return AttChannels(
        leftEq = leftEq,
        leftAmp = leftAmp,
        leftTone = leftTone,
        leftBoost = leftBoost,
        leftAnr = leftAnr,
        rightEq = rightEq,
        rightAmp = rightAmp,
        rightTone = rightTone,
        rightBoost = rightBoost,
        rightAnr = rightAnr,
        ownVoice = ownVoice,
    )
}

private fun writeChannels(
    buf: ByteBuffer,
    leftEq: FloatArray,
    leftAmp: Float,
    leftTone: Float,
    leftBoost: Boolean,
    leftAnr: Float,
    rightEq: FloatArray,
    rightAmp: Float,
    rightTone: Float,
    rightBoost: Boolean,
    rightAnr: Float,
    ownVoice: Float?,
) {
    for (i in 0 until 8) buf.putFloat(leftEq.getOrElse(i) { 0f })
    buf.putFloat(leftAmp)
    buf.putFloat(leftTone)
    buf.putFloat(if (leftBoost) 1f else 0f)
    buf.putFloat(leftAnr)
    for (i in 0 until 8) buf.putFloat(rightEq.getOrElse(i) { 0f })
    buf.putFloat(rightAmp)
    buf.putFloat(rightTone)
    buf.putFloat(if (rightBoost) 1f else 0f)
    buf.putFloat(rightAnr)
    if (ownVoice != null) buf.putFloat(ownVoice)
}
