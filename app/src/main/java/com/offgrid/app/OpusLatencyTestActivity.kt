package com.offgrid.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Process
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.score.rahasak.utils.OpusDecoder
import com.score.rahasak.utils.OpusEncoder
import com.offgrid.app.ui.theme.OffGridTheme
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

class OpusLatencyTestActivity : ComponentActivity() {

    private val tag = "OpusLatency"

    // Audio configuration
    private val sampleRate = 16000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val frameMs = 20
    private val frameSamples = sampleRate * frameMs / 1000 // 320
    private val bytesPerFrame = frameSamples * 2 // 16-bit mono

    private var isRunning = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var encoder: OpusEncoder? = null
    private var decoder: OpusDecoder? = null

    // UI state
    private var statusText by mutableStateOf("Ready")
    private var latencyText by mutableStateOf("Avg latency: -- ms")
    private var buttonText by mutableStateOf("Start Loopback")

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startLoopback() else statusText = "RECORD_AUDIO denied"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OffGridTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OpusLatencyScreen(
                        status = statusText,
                        latency = latencyText,
                        button = buttonText,
                        onToggle = ::onToggle
                    )
                }
            }
        }
    }

    private fun onToggle() {
        if (isRunning.get()) {
            stopLoopback()
        } else {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
                PackageManager.PERMISSION_GRANTED -> startLoopback()
                else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLoopback() {
        if (isRunning.getAndSet(true)) return

        statusText = "Initializing..."
        buttonText = "Stop Loopback"

        thread(name = "OpusLoopback", priority = Thread.MAX_PRIORITY) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            try {
                initAudio()
                runLoopback()
            } catch (e: Exception) {
                Log.e(tag, "Loopback error", e)
                runOnUiThread { statusText = "Error: ${e.message}" }
            } finally {
                releaseAudio()
                runOnUiThread {
                    isRunning.set(false)
                    buttonText = "Start Loopback"
                    if (!statusText.startsWith("Error")) statusText = "Stopped"
                }
            }
        }
    }

    private fun stopLoopback() {
        isRunning.set(false)
    }

    @SuppressLint("MissingPermission")
    private fun initAudio() {
        val minRecord = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
        val recordBufferSize = max(minRecord, bytesPerFrame * 5)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfigIn,
            audioFormat,
            recordBufferSize
        ).also {
            if (it.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("AudioRecord not initialized")
            }
        }

        val minTrack = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
        val trackBufferSize = max(minTrack, bytesPerFrame * 5)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(audioFormat)
                    .setChannelMask(channelConfigOut)
                    .build()
            )
            .setBufferSizeInBytes(trackBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build().also {
                if (it.state != AudioTrack.STATE_INITIALIZED) {
                    throw IllegalStateException("AudioTrack not initialized")
                }
            }

        encoder = OpusEncoder().apply {
            init(sampleRate, 1, OpusEncoder.OPUS_APPLICATION_VOIP)
            setBitrate(24000)
            setComplexity(5)
        }

        decoder = OpusDecoder().apply {
            init(sampleRate, 1)
        }
    }

    private fun runLoopback() {
        val record = audioRecord ?: return
        val track = audioTrack ?: return
        val enc = encoder ?: return
        val dec = decoder ?: return

        val pcmInBytes = ByteArray(bytesPerFrame)
        val pcmInShorts = ShortArray(frameSamples)
        val opusOut = ByteArray(1275) // max single 20 ms opus frame
        val pcmOutShorts = ShortArray(frameSamples)
        val pcmOutBytes = ByteArray(bytesPerFrame)

        // Synthetic codec sanity check
        val benchStart = System.nanoTime()
        repeat(1000) {
            val len = enc.encode(pcmInShorts, frameSamples, opusOut)
            if (len > 0) dec.decode(opusOut, pcmOutShorts, frameSamples)
        }
        val benchMs = (System.nanoTime() - benchStart) / 1_000_000.0
        Log.i(tag, "Synthetic codec benchmark: %.1f ms for 1000 frames (%.3f ms/frame)".format(benchMs, benchMs / 1000.0))

        record.startRecording()
        track.play()

        var frameCount = 0L
        var totalLatencyUs = 0L
        var minLatencyUs = Long.MAX_VALUE
        var maxLatencyUs = 0L
        var totalCodecUs = 0L
        var totalReadUs = 0L
        var totalWriteUs = 0L

        runOnUiThread { statusText = "Running loopback (wear earphones to avoid feedback)" }

        while (isRunning.get()) {
            // Read one frame from mic
            var read = 0
            val loopStart = System.nanoTime()
            val readStart = loopStart
            while (read < bytesPerFrame && isRunning.get()) {
                val n = record.read(pcmInBytes, read, bytesPerFrame - read)
                if (n > 0) read += n
            }
            if (!isRunning.get()) break
            val readEnd = System.nanoTime()

            // Convert bytes -> shorts (little-endian)
            ByteBuffer.wrap(pcmInBytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .get(pcmInShorts)

            // Encode
            val encodeStart = System.nanoTime()
            val encodedLen = enc.encode(pcmInShorts, frameSamples, opusOut)
            val encodeEnd = System.nanoTime()
            if (encodedLen <= 0) {
                Log.w(tag, "Opus encode failed: $encodedLen")
                continue
            }

            // Decode
            val decodeStart = System.nanoTime()
            val decodedSamples = dec.decode(opusOut, pcmOutShorts, frameSamples)
            val decodeEnd = System.nanoTime()
            if (decodedSamples <= 0) {
                Log.w(tag, "Opus decode failed: $decodedSamples")
                continue
            }

            // Convert shorts -> bytes
            ByteBuffer.wrap(pcmOutBytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .put(pcmOutShorts)

            // Write to speaker
            val writeStart = System.nanoTime()
            track.write(pcmOutBytes, 0, decodedSamples * 2)
            val writeEnd = System.nanoTime()

            // Stats
            val readUs = readEnd - readStart
            val encodeUs = encodeEnd - encodeStart
            val decodeUs = decodeEnd - decodeStart
            val writeUs = writeEnd - writeStart
            val latencyUs = writeEnd - loopStart
            val codecUs = encodeUs + decodeUs

            frameCount++
            totalLatencyUs += latencyUs
            totalCodecUs += codecUs
            totalReadUs += readUs
            totalWriteUs += writeUs
            minLatencyUs = min(minLatencyUs, latencyUs)
            maxLatencyUs = max(maxLatencyUs, latencyUs)

            if (frameCount <= 5 || frameCount % 20 == 0L) {
                Log.d(tag, "frame=$frameCount read=${readUs / 1000}us enc=${encodeUs / 1000}us dec=${decodeUs / 1000}us write=${writeUs / 1000}us latency=${latencyUs / 1000}us")
            }

            // Update UI every ~20 frames
            if (frameCount % 20 == 0L) {
                val avgMs = totalLatencyUs / frameCount / 1_000_000.0
                val minMs = minLatencyUs / 1_000_000.0
                val maxMs = maxLatencyUs / 1_000_000.0
                val codecMs = totalCodecUs / frameCount / 1_000_000.0
                val readMs = totalReadUs / frameCount / 1_000_000.0
                val writeMs = totalWriteUs / frameCount / 1_000_000.0
                runOnUiThread {
                    latencyText = "Avg %.1f ms (codec %.1f ms)\nread %.1f write %.1f ms\nRange %.1f - %.1f ms".format(
                        avgMs, codecMs, readMs, writeMs, minMs, maxMs
                    )
                }
            }
        }
    }

    private fun releaseAudio() {
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioTrack?.stop() } catch (_: Exception) {}
        audioRecord?.release()
        audioTrack?.release()
        audioRecord = null
        audioTrack = null
        encoder?.close()
        decoder?.close()
        encoder = null
        decoder = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLoopback()
    }
}

@Composable
private fun OpusLatencyScreen(
    status: String,
    latency: String,
    button: String,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Opus Latency Test",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = latency,
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onToggle) {
            Text(button)
        }
    }
}
