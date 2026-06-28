package com.offgrid.app.audio.opus

/**
 * JNI wrapper around Xiph libopus encoder.
 *
 * License: libopus is BSD-3-Clause; this wrapper is MIT (OffGrid project).
 */
class OpusEncoder {

    private var handle: Long = 0

    fun init(sampleRate: Int, channels: Int, application: Int): Boolean {
        handle = nativeInit(sampleRate, channels, application)
        return handle != 0L
    }

    fun setBitrate(bitrate: Int) {
        nativeSetBitrate(handle, bitrate)
    }

    fun setComplexity(complexity: Int) {
        nativeSetComplexity(handle, complexity)
    }

    fun encode(pcm: ShortArray, frameSize: Int, out: ByteArray): Int {
        return nativeEncode(handle, pcm, frameSize, out)
    }

    fun release() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0
        }
    }

    /** Convenience alias for callers expecting a `close()` API. */
    fun close() = release()

    private external fun nativeInit(sampleRate: Int, channels: Int, application: Int): Long
    private external fun nativeSetBitrate(handle: Long, bitrate: Int)
    private external fun nativeSetComplexity(handle: Long, complexity: Int)
    private external fun nativeEncode(handle: Long, pcm: ShortArray, frameSize: Int, out: ByteArray): Int
    private external fun nativeDestroy(handle: Long)

    companion object {
        init {
            System.loadLibrary("opus_jni")
        }

        /**
         * Opus application mode optimized for VoIP.
         *
         * Matches OPUS_APPLICATION_VOIP from opus_defines.h (2048).
         */
        const val OPUS_APPLICATION_VOIP = 2048
    }
}
