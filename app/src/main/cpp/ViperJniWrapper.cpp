#include <jni.h>
#include <android/log.h>
#include "ViperDspEngine.hpp"

#define TAG "ViperJniWrapper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static ViperDspEngine gViperEngine;

extern "C" {

JNIEXPORT void JNICALL
Java_com_codewave_player_core_audio_ViperJniWrapper_nativeInit(
    JNIEnv* /*env*/,
    jclass /*clazz*/,
    jdouble sampleRate
) {
    gViperEngine.init(sampleRate);
}

JNIEXPORT void JNICALL
Java_com_codewave_player_core_audio_ViperJniWrapper_nativeReset(
    JNIEnv* /*env*/,
    jclass /*clazz*/
) {
    gViperEngine.reset();
}

JNIEXPORT void JNICALL
Java_com_codewave_player_core_audio_ViperJniWrapper_nativeSetGlobalEnabled(
    JNIEnv* /*env*/,
    jclass /*clazz*/,
    jboolean enabled
) {
    gViperEngine.setGlobalEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_codewave_player_core_audio_ViperJniWrapper_nativeSetEqEnabled(
    JNIEnv* /*env*/,
    jclass /*clazz*/,
    jboolean enabled
) {
    gViperEngine.setEqEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_codewave_player_core_audio_ViperJniWrapper_nativeSetLimiterEnabled(
    JNIEnv* /*env*/,
    jclass /*clazz*/,
    jboolean enabled
) {
    gViperEngine.setLimiterEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_codewave_player_core_audio_ViperJniWrapper_nativeSetEqBand(
    JNIEnv* /*env*/,
    jclass /*clazz*/,
    jint bandIndex,
    jdouble gainDb
) {
    gViperEngine.setEqBand(bandIndex, gainDb);
}

JNIEXPORT void JNICALL
Java_com_codewave_player_core_audio_ViperJniWrapper_nativeSetUserPreamp(
    JNIEnv* /*env*/,
    jclass /*clazz*/,
    jfloat preampDb
) {
    gViperEngine.setUserPreampDb(preampDb);
}

JNIEXPORT void JNICALL
Java_com_codewave_player_core_audio_ViperJniWrapper_nativeSetBass(
    JNIEnv* /*env*/,
    jclass /*clazz*/,
    jboolean enabled,
    jfloat gainDb
) {
    gViperEngine.setBass(enabled, gainDb);
}

JNIEXPORT void JNICALL
Java_com_codewave_player_core_audio_ViperJniWrapper_nativeSetClarity(
    JNIEnv* /*env*/,
    jclass /*clazz*/,
    jboolean enabled,
    jfloat gainDb
) {
    gViperEngine.setClarity(enabled, gainDb);
}

JNIEXPORT void JNICALL
Java_com_codewave_player_core_audio_ViperJniWrapper_nativeSetConvolverEnabled(
    JNIEnv* /*env*/,
    jclass /*clazz*/,
    jboolean enabled
) {
    gViperEngine.setConvolverEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_codewave_player_core_audio_ViperJniWrapper_nativeLoadImpulseResponse(
    JNIEnv* env,
    jclass /*clazz*/,
    jfloatArray irData,
    jint length,
    jint channels
) {
    if (irData == nullptr || length <= 0) {
        gViperEngine.loadImpulseResponse(nullptr, 0, 1);
        return;
    }
    jfloat* elements = env->GetFloatArrayElements(irData, nullptr);
    if (elements != nullptr) {
        gViperEngine.loadImpulseResponse(elements, length, channels);
        env->ReleaseFloatArrayElements(irData, elements, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL
Java_com_codewave_player_core_audio_ViperJniWrapper_nativeProcessStereo(
    JNIEnv* env,
    jclass /*clazz*/,
    jfloatArray audioBuffer,
    jint frameCount
) {
    if (audioBuffer == nullptr || frameCount <= 0) return;

    // Use GetPrimitiveArrayCritical to eliminate JNI copy overhead & GC pauses (Edge Case 47)
    jfloat* bufferPtr = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(audioBuffer, nullptr));
    if (bufferPtr != nullptr) {
        gViperEngine.processStereoInterleaved(bufferPtr, frameCount);
        env->ReleasePrimitiveArrayCritical(audioBuffer, bufferPtr, 0); // 0 = write back buffer changes
    }
}

} // extern "C"
