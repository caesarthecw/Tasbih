package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.model.SoundTone
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Low-latency synthesized acoustic engine for Misbaha taps.
 * Generates gentle water-drop, wooden bead, crystal chime, and rain mist waveforms in memory
 * so sound playback is crisp, reliable offline, and volume-controllable.
 */
class MisbahaAudioEngine {

    private val sampleRate = 22050
    private val buffers: Map<SoundTone, ShortArray> by lazy {
        mapOf(
            SoundTone.WATER_DROP to synthesizeWaterDrop(),
            SoundTone.WOOD_BEAD to synthesizeWoodBead(),
            SoundTone.CRYSTAL_CHIME to synthesizeCrystalChime(),
            SoundTone.RAIN_MIST to synthesizeRainMist(),
        )
    }

    @Volatile
    private var activeTrack: AudioTrack? = null

    fun playTap(tone: SoundTone, volume: Float, enabled: Boolean) {
        if (!enabled || volume <= 0.01f) return
        val pcm = buffers[tone] ?: return
        val clampedVolume = volume.coerceIn(0f, 1f)

        try {
            activeTrack?.let { prev ->
                if (prev.state == AudioTrack.STATE_INITIALIZED) {
                    try {
                        prev.stop()
                    } catch (_: IllegalStateException) {
                    }
                }
                prev.release()
            }

            val bufferBytes = pcm.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.write(pcm, 0, pcm.size)
                track.setVolume(clampedVolume)
                track.play()
                activeTrack = track
            } else {
                track.release()
            }
        } catch (e: Exception) {
            Log.w("MisbahaAudio", "Unable to play synthesized tap sound: ${e.message}")
        }
    }

    fun release() {
        try {
            activeTrack?.release()
        } catch (_: Exception) {
        } finally {
            activeTrack = null
        }
    }

    private fun synthesizeWaterDrop(): ShortArray {
        val durationSec = 0.075
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val norm = i.toDouble() / count
            // Classic water droplet upward frequency chirp (440Hz -> 1180Hz)
            val freq = 440.0 + 740.0 * (norm * norm)
            phase += 2.0 * PI * freq / sampleRate
            val attack = (t / 0.005).coerceAtMost(1.0)
            val decay = exp(-t * 48.0)
            val sample = sin(phase) * attack * decay * 0.55
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthesizeWoodBead(): ShortArray {
        val durationSec = 0.045
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val attack = (t / 0.002).coerceAtMost(1.0)
            val decay = exp(-t * 95.0)
            val tone1 = sin(2.0 * PI * 540.0 * t)
            val tone2 = 0.45 * sin(2.0 * PI * 1180.0 * t)
            val sample = (tone1 + tone2) * 0.42 * attack * decay
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthesizeCrystalChime(): ShortArray {
        val durationSec = 0.14
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val attack = (t / 0.006).coerceAtMost(1.0)
            val decay = exp(-t * 26.0)
            val fundamental = sin(2.0 * PI * 1046.5 * t)
            val overtone = 0.35 * sin(2.0 * PI * 1567.98 * t)
            val sample = (fundamental + overtone) * 0.36 * attack * decay
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthesizeRainMist(): ShortArray {
        val durationSec = 0.095
        val count = (sampleRate * durationSec).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val norm = i.toDouble() / count
            val freq = 660.0 + 480.0 * norm
            phase += 2.0 * PI * freq / sampleRate
            val attack = (t / 0.008).coerceAtMost(1.0)
            val decay = exp(-t * 36.0)
            val shimmer = 0.25 * sin(2.0 * PI * 1980.0 * t) * exp(-t * 50.0)
            val sample = (sin(phase) * 0.42 + shimmer) * attack * decay
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }
}
