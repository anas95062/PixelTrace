#include <jni.h>
#include <string>
#include <android/log.h>

// OpenCV ke liye zaroori headers
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

#define TAG "NativeLib"

extern "C" JNIEXPORT void JNICALL
Java_com_anas_pixeltrace_NativeBridge_processFrame(
        JNIEnv* env,
        jclass clazz,
        jbyteArray data,
        jint width,
        jint height) {

    // Step 1: Java byte array se C++ pointer haasil karein
    jbyte* dataPtr = env->GetByteArrayElements(data, nullptr);

    // Step 2: C++ pointer se ek OpenCV Mat (Matrix) banayein
    // Humara data pehle se hi grayscale (Y-plane) hai, isliye CV_8UC1
    cv::Mat grayMat(height, width, CV_8UC1, dataPtr);

    // Ek naya Mat banayein jismein Canny ka result store hoga
    cv::Mat edgesMat;

    // Step 3: Canny Edge Detection filter lagayein
    cv::Canny(grayMat, edgesMat, 100, 200); // Aap in threshold values ko badal sakte hain

    // Abhi ke liye, hum sirf log karenge ki processing ho gayi
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Frame processed with Canny. Result size: %d x %d", edgesMat.cols, edgesMat.rows);

    // Step 4: Memory release karein
    env->ReleaseByteArrayElements(data, dataPtr, 0);
}