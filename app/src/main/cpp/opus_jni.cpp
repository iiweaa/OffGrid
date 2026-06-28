#include <jni.h>
#include <opus.h>
#include <android/log.h>

#define LOG_TAG "OpusJni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

// ---------- OpusEncoder ----------

JNIEXPORT jlong JNICALL
Java_com_offgrid_app_audio_opus_OpusEncoder_nativeInit(
    JNIEnv* env,
    jobject /*thiz*/,
    jint sampleRate,
    jint channels,
    jint application) {
    int error = 0;
    OpusEncoder* encoder = opus_encoder_create(sampleRate, channels, application, &error);
    if (error != OPUS_OK || encoder == nullptr) {
        LOGE("opus_encoder_create failed: %d", error);
        return 0;
    }
    return reinterpret_cast<jlong>(encoder);
}

JNIEXPORT void JNICALL
Java_com_offgrid_app_audio_opus_OpusEncoder_nativeSetBitrate(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle,
    jint bitrate) {
    OpusEncoder* encoder = reinterpret_cast<OpusEncoder*>(handle);
    if (encoder == nullptr) return;
    int error = opus_encoder_ctl(encoder, OPUS_SET_BITRATE(bitrate));
    if (error != OPUS_OK) {
        LOGE("OPUS_SET_BITRATE failed: %d", error);
    }
}

JNIEXPORT void JNICALL
Java_com_offgrid_app_audio_opus_OpusEncoder_nativeSetComplexity(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle,
    jint complexity) {
    OpusEncoder* encoder = reinterpret_cast<OpusEncoder*>(handle);
    if (encoder == nullptr) return;
    int error = opus_encoder_ctl(encoder, OPUS_SET_COMPLEXITY(complexity));
    if (error != OPUS_OK) {
        LOGE("OPUS_SET_COMPLEXITY failed: %d", error);
    }
}

JNIEXPORT jint JNICALL
Java_com_offgrid_app_audio_opus_OpusEncoder_nativeEncode(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jshortArray pcm,
    jint frameSize,
    jbyteArray out) {
    OpusEncoder* encoder = reinterpret_cast<OpusEncoder*>(handle);
    if (encoder == nullptr || pcm == nullptr || out == nullptr) return -1;

    jshort* pcmPtr = env->GetShortArrayElements(pcm, nullptr);
    jbyte* outPtr = env->GetByteArrayElements(out, nullptr);
    if (pcmPtr == nullptr || outPtr == nullptr) {
        if (pcmPtr) env->ReleaseShortArrayElements(pcm, pcmPtr, JNI_ABORT);
        if (outPtr) env->ReleaseByteArrayElements(out, outPtr, JNI_ABORT);
        return -1;
    }

    int outCapacity = env->GetArrayLength(out);
    int encoded = opus_encode(
        encoder,
        pcmPtr,
        frameSize,
        reinterpret_cast<unsigned char*>(outPtr),
        outCapacity);

    env->ReleaseShortArrayElements(pcm, pcmPtr, JNI_ABORT);
    env->ReleaseByteArrayElements(out, outPtr, encoded > 0 ? 0 : JNI_ABORT);

    return encoded;
}

JNIEXPORT void JNICALL
Java_com_offgrid_app_audio_opus_OpusEncoder_nativeDestroy(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    OpusEncoder* encoder = reinterpret_cast<OpusEncoder*>(handle);
    if (encoder != nullptr) {
        opus_encoder_destroy(encoder);
    }
}

// ---------- OpusDecoder ----------

JNIEXPORT jlong JNICALL
Java_com_offgrid_app_audio_opus_OpusDecoder_nativeInit(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jint sampleRate,
    jint channels) {
    int error = 0;
    OpusDecoder* decoder = opus_decoder_create(sampleRate, channels, &error);
    if (error != OPUS_OK || decoder == nullptr) {
        LOGE("opus_decoder_create failed: %d", error);
        return 0;
    }
    return reinterpret_cast<jlong>(decoder);
}

JNIEXPORT jint JNICALL
Java_com_offgrid_app_audio_opus_OpusDecoder_nativeDecode(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jbyteArray data,
    jshortArray outPcm,
    jint frameSize) {
    OpusDecoder* decoder = reinterpret_cast<OpusDecoder*>(handle);
    if (decoder == nullptr || data == nullptr || outPcm == nullptr) return -1;

    jbyte* dataPtr = env->GetByteArrayElements(data, nullptr);
    jshort* outPtr = env->GetShortArrayElements(outPcm, nullptr);
    if (dataPtr == nullptr || outPtr == nullptr) {
        if (dataPtr) env->ReleaseByteArrayElements(data, dataPtr, JNI_ABORT);
        if (outPtr) env->ReleaseShortArrayElements(outPcm, outPtr, JNI_ABORT);
        return -1;
    }

    int dataLen = env->GetArrayLength(data);
    int decoded = opus_decode(
        decoder,
        reinterpret_cast<const unsigned char*>(dataPtr),
        dataLen,
        outPtr,
        frameSize,
        0);

    env->ReleaseByteArrayElements(data, dataPtr, JNI_ABORT);
    env->ReleaseShortArrayElements(outPcm, outPtr, decoded > 0 ? 0 : JNI_ABORT);

    return decoded;
}

JNIEXPORT void JNICALL
Java_com_offgrid_app_audio_opus_OpusDecoder_nativeDestroy(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    OpusDecoder* decoder = reinterpret_cast<OpusDecoder*>(handle);
    if (decoder != nullptr) {
        opus_decoder_destroy(decoder);
    }
}

} // extern "C"
