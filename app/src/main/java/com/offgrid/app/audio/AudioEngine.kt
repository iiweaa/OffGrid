package com.offgrid.app.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioDeviceInfo
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import com.score.rahasak.utils.OpusDecoder
import com.score.rahasak.utils.OpusEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class AudioEngine(private val context: Context) {
    companion object {
        private const val TAG = "AudioEngine"
        const val SAMPLE_RATE = 16000
        const val FRAME_MS = 20
        const val CHANNELS = 1
        const val BITRATE_NORMAL = 24000
        const val BITRATE_POWER_SAVING = 12000
        const val COMPLEXITY_NORMAL = 5
        const val COMPLEXITY_POWER_SAVING = 3
        val FRAME_SAMPLES: Int = SAMPLE_RATE * FRAME_MS / 1000
        val BYTES_PER_FRAME: Int = FRAME_SAMPLES * 2
    }

    private val playExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "OffGridAudioPlay").apply { isDaemon = true }
    }
    private val running = AtomicBoolean(false)
    private val muted = AtomicBoolean(false)
    private val powerSaving = AtomicBoolean(false)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var encoder: OpusEncoder? = null
    private var decoder: OpusDecoder? = null
    private var recordThread: Thread? = null

    private var onEncodedFrame: ((ByteArray, Int) -> Unit)? = null

    fun setOnEncodedFrame(listener: (ByteArray, Int) -> Unit) {
        onEncodedFrame = listener
    }

    fun setMuted(isMuted: Boolean) {
        muted.set(isMuted)
        Log.d(TAG, "Muted: $isMuted")
    }

    fun isMuted(): Boolean = muted.get()

    fun setPowerSaving(isPowerSaving: Boolean) {
        powerSaving.set(isPowerSaving)
        encoder?.apply {
            setBitrate(if (isPowerSaving) BITRATE_POWER_SAVING else BITRATE_NORMAL)
            setComplexity(if (isPowerSaving) COMPLEXITY_POWER_SAVING else COMPLEXITY_NORMAL)
        }
        Log.d(TAG, "Power saving mode: $isPowerSaving")
    }

    fun isPowerSaving(): Boolean = powerSaving.get()

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running.getAndSet(true)) return true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO not granted")
            running.set(false)
            return false
        }

        val initLatch = CountDownLatch(1)
        var initSuccess = false
        recordThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            try {
                initAudio()
                initSuccess = true
            } catch (e: Exception) {
                Log.e(TAG, "Audio init failed", e)
            } finally {
                initLatch.countDown()
            }
            if (initSuccess) {
                try {
                    recordLoop()
                } catch (e: Exception) {
                    Log.e(TAG, "Audio engine error", e)
                } finally {
                    releaseAudio()
                }
            }
            running.set(false)
        }, "OffGridAudioRecord").apply { isDaemon = true }
        recordThread?.start()

        return try {
            initLatch.await(2, TimeUnit.SECONDS) && initSuccess
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    fun stop() {
        running.set(false)
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        recordThread?.join(1000)
        playExecutor.shutdown()
        playExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
    }

    fun playPacket(opusFrame: ByteArray, length: Int) {
        if (length <= 0 || length > opusFrame.size) return
        val dec = decoder ?: return
        val frame = opusFrame.copyOf(length)
        playExecutor.execute {
            try {
                val outShorts = ShortArray(FRAME_SAMPLES)
                val decoded = dec.decode(frame, outShorts, FRAME_SAMPLES)
                val playMax = if (decoded > 0) outShorts.take(decoded).maxOf { kotlin.math.abs(it.toInt()) } else 0
                Log.d(TAG, "playPacket len=$length decoded=$decoded playMax=$playMax")
                if (decoded > 0) {
                    val bytes = ByteArray(decoded * 2)
                    ByteBuffer.wrap(bytes)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer()
                        .put(outShorts, 0, decoded)
                    val written = audioTrack?.write(bytes, 0, decoded * 2) ?: -1
                    if (written != decoded * 2) {
                        Log.w(TAG, "AudioTrack write partial: $written / ${decoded * 2}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Play packet failed", e)
            }
        }
    }

    @SuppressLint("MissingPermission", "WrongConstant")
    private fun initAudio() {
        val minRecord = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val recordBuffer = maxOf(minRecord, BYTES_PER_FRAME * 5)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            recordBuffer
        )
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord init failed")
        }

        val minTrack = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val trackBuffer = maxOf(minTrack, BYTES_PER_FRAME * 5)
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(trackBuffer)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
            throw IllegalStateException("AudioTrack init failed")
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val speaker = audioManager.availableCommunicationDevices
                .find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speaker != null) {
                val ok = audioManager.setCommunicationDevice(speaker)
                Log.d(TAG, "Set communication device to speaker: $ok")
            } else {
                Log.w(TAG, "No built-in speaker communication device found")
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true
        }
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, 0)
        Log.d(TAG, "Audio routed to speakerphone, voice call volume=$maxVol")

        encoder = OpusEncoder().apply {
            init(SAMPLE_RATE, CHANNELS, OpusEncoder.OPUS_APPLICATION_VOIP)
            setBitrate(if (powerSaving.get()) BITRATE_POWER_SAVING else BITRATE_NORMAL)
            setComplexity(if (powerSaving.get()) COMPLEXITY_POWER_SAVING else COMPLEXITY_NORMAL)
        }.also { verifyNativeHandle(it, "OpusEncoder") }

        decoder = OpusDecoder().apply {
            init(SAMPLE_RATE, CHANNELS)
        }.also { verifyNativeHandle(it, "OpusDecoder") }
    }

    private fun verifyNativeHandle(obj: Any, name: String) {
        try {
            val field = obj.javaClass.getDeclaredField("address")
            field.isAccessible = true
            val address = field.getLong(obj)
            if (address == 0L) {
                throw IllegalStateException("$name native handle is 0")
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to verify $name native handle", e)
        }
    }

    private fun recordLoop() {
        val record = audioRecord ?: return
        val enc = encoder ?: return
        val pcmBytes = ByteArray(BYTES_PER_FRAME)
        val pcmShorts = ShortArray(FRAME_SAMPLES)
        val opusOut = ByteArray(1275)

        record.startRecording()
        audioTrack?.play()
        Log.d(TAG, "recordLoop started")

        var frameCount = 0
        var lastEncoded = 0
        while (running.get()) {
            var read = 0
            while (read < BYTES_PER_FRAME && running.get()) {
                val n = record.read(pcmBytes, read, BYTES_PER_FRAME - read)
                if (n > 0) {
                    read += n
                } else if (n < 0) {
                    Log.w(TAG, "AudioRecord read error: $n")
                    return
                }
            }
            if (!running.get()) break

            ByteBuffer.wrap(pcmBytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .get(pcmShorts)

            // Simple AGC: boost quiet frames so the remote side can hear.
            applyMicGain(pcmShorts)

            if (!muted.get()) {
                val encoded = enc.encode(pcmShorts, FRAME_SAMPLES, opusOut)
                if (encoded > 0) {
                    lastEncoded = encoded
                    onEncodedFrame?.invoke(opusOut, encoded)
                }
            }
            frameCount++
            if (frameCount % 100 == 0) {
                val micMax = pcmShorts.maxOf { kotlin.math.abs(it.toInt()) }
                Log.d(TAG, "Encoded $frameCount frames, last=$lastEncoded bytes, micMax=$micMax")
            }
        }
    }

    private fun applyMicGain(samples: ShortArray) {
        val maxAmp = samples.maxOf { kotlin.math.abs(it.toInt()) }
        if (maxAmp == 0) return
        val target = 12000
        val gain = kotlin.math.min(target.toFloat() / maxAmp, 8.0f)
        if (gain <= 1.0f) return
        for (i in samples.indices) {
            val v = (samples[i] * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            samples[i] = v.toShort()
        }
    }

    private fun releaseAudio() {
        playExecutor.shutdown()
        playExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        try {
            audioTrack?.stop()
        } catch (_: Exception) {
        }
        audioRecord?.release()
        audioTrack?.release()
        encoder?.close()
        decoder?.close()
        audioRecord = null
        audioTrack = null
        encoder = null
        decoder = null
    }
}
