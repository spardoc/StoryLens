#include <jni.h>
#include <string>
#include <sstream>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/dnn.hpp>
#include <opencv2/video.hpp>
#include <opencv2/opencv.hpp>
#include "android/bitmap.h"
#include <android/log.h>
#include <deque>


#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>

using namespace cv;

void processImageInternal(const Mat& input, Mat& output) {
    cvtColor(input, output, COLOR_RGBA2GRAY);
    GaussianBlur(output, output, Size(5, 5), 0);
    adaptiveThreshold(output, output, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 11, 2);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_write_1vision_1ai_TextCaptureActivity_processImage(JNIEnv *env, jobject thiz) {
    Mat input = Mat::zeros(480, 640, CV_8UC4);
    input.setTo(Scalar(255, 0, 0, 255));

    Mat output;
    processImageInternal(input, output);

    std::string result = "Imagen dummy procesada exitosamente en C++ con OpenCV";
    return env->NewStringUTF(result.c_str());
}