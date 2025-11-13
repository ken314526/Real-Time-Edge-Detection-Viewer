#include <jni.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <android/log.h>
#include <vector>
#include <mutex>
#include <cstring>

#define LOG_TAG "VisionProcessor"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
    std::mutex gMutex;
    cv::Mat gInputGray;
    cv::Mat gOutput;
    int gWidth = 0;
    int gHeight = 0;

    void yuv420ToGray(const uint8_t *yPlane, int width, int height, int yRowStride, int yPixelStride) {
        if (gInputGray.empty() || gInputGray.cols != width || gInputGray.rows != height) {
            gInputGray = cv::Mat(height, width, CV_8UC1);
        }

        uint8_t *dst = gInputGray.data;
        for (int row = 0; row < height; ++row) {
            const uint8_t *srcRow = yPlane + row * yRowStride;
            uint8_t *dstRow = dst + row * width;
            for (int col = 0; col < width; ++col) {
                dstRow[col] = srcRow[col * yPixelStride];
            }
        }
    }

    void ensureOutput(int width, int height) {
        if (gOutput.empty() || gOutput.cols != width || gOutput.rows != height) {
            gOutput = cv::Mat(height, width, CV_8UC4);
        }
    }

    std::vector<uint8_t> processFrame(
            const uint8_t *yPlane,
            int width,
            int height,
            int yRowStride,
            int yPixelStride
    ) {
        yuv420ToGray(yPlane, width, height, yRowStride, yPixelStride);
        ensureOutput(width, height);

        cv::Mat edges;
        cv::Canny(gInputGray, edges, 80.0, 160.0);
        cv::cvtColor(edges, gOutput, cv::COLOR_GRAY2RGBA);

        std::vector<uint8_t> buffer(gOutput.total() * gOutput.elemSize());
        std::memcpy(buffer.data(), gOutput.data, buffer.size());
        return buffer;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_vision_processing_NativeProcessor_init(
        JNIEnv *env,
        jclass clazz,
        jint width,
        jint height
) {
    std::lock_guard<std::mutex> lock(gMutex);
    gWidth = width;
    gHeight = height;
    gInputGray = cv::Mat();
    gOutput = cv::Mat();
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_vision_processing_NativeProcessor_processFrame(
        JNIEnv *env,
        jclass clazz,
        jbyteArray y_plane,
        jbyteArray u_plane,
        jbyteArray v_plane,
        jint width,
        jint height,
        jint y_row_stride,
        jint u_row_stride,
        jint v_row_stride,
        jint y_pixel_stride,
        jint u_pixel_stride,
        jint v_pixel_stride
) {
    std::lock_guard<std::mutex> lock(gMutex);
    jbyte *yData = env->GetByteArrayElements(y_plane, nullptr);
    std::vector<uint8_t> processed;

    try {
        processed = processFrame(
                reinterpret_cast<uint8_t *>(yData),
                width,
                height,
                y_row_stride,
                y_pixel_stride
        );
    } catch (const cv::Exception &e) {
        LOGE("OpenCV error: %s", e.what());
        processed.clear();
    }

    env->ReleaseByteArrayElements(y_plane, yData, JNI_ABORT);

    jbyteArray outputArray = env->NewByteArray(static_cast<jsize>(processed.size()));
    if (!outputArray || processed.empty()) {
        return outputArray;
    }

    env->SetByteArrayRegion(outputArray, 0, static_cast<jsize>(processed.size()),
                            reinterpret_cast<const jbyte *>(processed.data()));
    return outputArray;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_vision_processing_NativeProcessor_release(
        JNIEnv *env,
        jclass clazz
) {
    std::lock_guard<std::mutex> lock(gMutex);
    gInputGray.release();
    gOutput.release();
    gWidth = 0;
    gHeight = 0;
}

