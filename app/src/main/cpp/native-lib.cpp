#include <jni.h>
#include <string>
#include <android/log.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <GLES2/gl2.h>
#include <mutex> // For thread safety

#define TAG "NativeLib"

// Global variable to hold the latest processed frame
cv::Mat processedMat;
// Mutex to prevent threads from interfering with each other
std::mutex mtx;

// FUNCTION 1: Processes the frame from the camera thread
extern "C" JNIEXPORT void JNICALL
Java_com_anas_pixeltrace_NativeBridge_processFrame(
        JNIEnv* env,
        jclass clazz,
        jbyteArray data,
        jint width,
        jint height) {

    jbyte* dataPtr = env->GetByteArrayElements(data, nullptr);
    cv::Mat grayMat(height, width, CV_8UC1, dataPtr);

    // Lock the mutex before writing to the global variable
    std::lock_guard<std::mutex> lock(mtx);
    // Run Canny and store the result in our global 'processedMat'
    cv::Canny(grayMat, processedMat, 100, 200);

    env->ReleaseByteArrayElements(data, dataPtr, 0);
}

// FUNCTION 2: Uploads the texture from the OpenGL thread
extern "C" JNIEXPORT void JNICALL
Java_com_anas_pixeltrace_NativeBridge_updateTexture(
        JNIEnv* env,
        jclass clazz,
        jint texture_id) {

    // Lock the mutex before reading from the global variable
    std::lock_guard<std::mutex> lock(mtx);

    // Check if there is data to upload
    if (!processedMat.empty() && texture_id > 0) {
        glBindTexture(GL_TEXTURE_2D, texture_id);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE,
                     processedMat.cols, processedMat.rows,
                     0, GL_LUMINANCE, GL_UNSIGNED_BYTE,
                     processedMat.ptr());
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}