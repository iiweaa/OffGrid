package com.offgrid.app.audio.opus

/**
 * JNI wrapper around Xiph libopus decoder.
 *
 * License: libopus is BSD-3-Clause; this wrapper is MIT (OffGrid project).
 */
class OpusDecoder {

    private var handle: Long = 0

    fun init(sampleRate: Int, channels: Int): Boolean {
        handle = nativeInit(sampleRate, channels)
        return handle != 0L
    }

    /**
     * Decodes an Opus packet into PCM shorts.
     *
     * @param data the encoded Opus packet
     * @param outPcm output buffer for decoded samples
     * @param frameSize number of samples per channel that can fit in [outPcm]
     * @return number of decoded samples per channel, or negative on error
     */
    fun decode(data: ByteArray, outPcm: ShortArray, frameSize: Int): Int {
        return nativeDecode(handle, data, outPcm, frameSize)
    }

    fun release() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0
        }
    }

    /** Convenience alias for callers expecting a `close()` API. */
    fun close() = release()

    private external fun nativeInit(sampleRate: Int, channels: Int): Long
    private external fun nativeDecode(handle: Long, data: ByteArray, outPcm: ShortArray, frameSize: Int): Int
    private external fun nativeDestroy(handle: Long)

    companion object {
        init {
            System.loadLibrary("opus_jni")
        }
    }
}
