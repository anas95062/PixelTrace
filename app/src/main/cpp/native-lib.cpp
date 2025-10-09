#include <jni.h>
#include <string>
#include <android/log.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <GLES2/gl2.h>
#include <mutex>

#define TAG "NativeLib"

// --- ADD A FLAG TO CONTROL THE FILTER ---
bool isFilterEnabled = true;
// ----------------------------------------

cv::Mat processedMat;
std::mutex mtx;

// --- IMPLEMENT THE NEW TOGGLE FUNCTION ---
extern "C" JNIEXPORT void JNICALL
Java_com_anas_pixeltrace_NativeBridge_toggleFilter(
        JNIEnv* env,
        jclass clazz) {
    isFilterEnabled = !isFilterEnabled; // Flip the boolean value
}
// -----------------------------------------

extern "C" JNIEXPORT void JNICALL
Java_com_anas_pixeltrace_NativeBridge_processFrame(
        JNIEnv* env,
        jclass clazz,
        jbyteArray data,
        jint width,
        jint height) {

    jbyte* dataPtr = env->GetByteArrayElements(data, nullptr);
    cv::Mat grayMat(height, width, CV_8UC1, dataPtr);

    std::lock_guard<std::mutex> lock(mtx);

    // --- USE THE FLAG TO DECIDE WHAT TO DO ---
    if (isFilterEnabled) {
        // If filter is on, run Canny
        cv::Canny(grayMat, processedMat, 100, 200);
    } else {
        // If filter is off, just show the raw grayscale image
        processedMat = grayMat.clone();
    }
    // -----------------------------------------

    env->ReleaseByteArrayElements(data, dataPtr, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_anas_pixeltrace_NativeBridge_updateTexture(
        JNIEnv* env,
        jclass clazz,
        jint texture_id) {

    std::lock_guard<std::mutex> lock(mtx);

    if (!processedMat.empty() && texture_id > 0) {
        glBindTexture(GL_TEXTURE_2D, texture_id);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,
                     processedMat.cols, processedMat.rows,
                     0, GL_LUMINANCE, GL_UNSIGNED_BYTE,
                     processedMat.ptr());
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}