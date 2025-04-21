#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>

using namespace cv;
static const char* TAG = "NativeProcessor";

// Constantes enteras
constexpr int DENOISE_FILTER_SIZE = 9;
constexpr int DENOISE_FILTER_SIGMA_COLOR = 75;
constexpr int DENOISE_FILTER_SIGMA_SPACE = 75;
constexpr int CLAHE_TILE_SIZE = 8;
constexpr int USM_THRESHOLD = 0;

// Constantes flotantes
constexpr double CLAHE_CLIP_LIMIT = 3.0;
constexpr double USM_SIGMA = 3.0;
constexpr double USM_AMOUNT = 1.5;


void processImageInternal(const Mat& input, Mat& output) {
    // 1. RGBA -> BGR
    Mat bgr;
    cvtColor(input, bgr, COLOR_RGBA2BGR);

    // 2. Reduce noise but keep edges
    Mat denoised;
    bilateralFilter(bgr, denoised,
                    DENOISE_FILTER_SIZE,
                    DENOISE_FILTER_SIGMA_COLOR,
                    DENOISE_FILTER_SIGMA_SPACE);

    // 3. Contrast enhancement in LAB space
    Mat lab;
    cvtColor(denoised, lab, COLOR_BGR2Lab);
    std::vector<Mat> labChannels;
    split(lab, labChannels);
    Ptr<CLAHE> clahe = createCLAHE(CLAHE_CLIP_LIMIT, Size(CLAHE_TILE_SIZE, CLAHE_TILE_SIZE));
    clahe->apply(labChannels[0], labChannels[0]);
    merge(labChannels, lab);
    Mat contrastEnhanced;
    cvtColor(lab, contrastEnhanced, COLOR_Lab2BGR);

    // 4. Sharpen via Unsharp Mask
    Mat blurred;
    GaussianBlur(contrastEnhanced, blurred, Size(), USM_SIGMA);
    Mat usm;
    addWeighted(contrastEnhanced, USM_AMOUNT,
                blurred, - (USM_AMOUNT - 1.0),
                0, usm);

    // 5. Convert to grayscale
    Mat gray;
    cvtColor(usm, gray, COLOR_BGR2GRAY);

    // 6. Further denoise grayscale for pencil strokes
    Mat finalDenoised;
    fastNlMeansDenoising(gray, finalDenoised, 30);

    // 7. Optional adaptive histogram equalization
    Ptr<CLAHE> claheGray = createCLAHE(2.0, Size(CLAHE_TILE_SIZE, CLAHE_TILE_SIZE));
    claheGray->apply(finalDenoised, finalDenoised);

    // 8. Convert back to RGBA
    cvtColor(finalDenoised, output, COLOR_GRAY2RGBA);
}
// Superposición de marco sobre imagen
void overlayFrame(const Mat& base, const Mat& frame, Mat& output) {
    Mat resizedFrame;
    resize(frame, resizedFrame, base.size());

    std::vector<Mat> channels;
    split(resizedFrame, channels);
    Mat alpha = channels[3];

    // Convertir alpha a rango 0–1
    Mat alphaFloat;
    alpha.convertTo(alphaFloat, CV_32FC1, 1.0 / 255);

    // Convert base y frame a float
    Mat baseFloat, frameFloat;
    base.convertTo(baseFloat, CV_32FC3, 1.0 / 255);
    Mat rgbFrame;
    cvtColor(resizedFrame, rgbFrame, COLOR_BGRA2BGR);
    rgbFrame.convertTo(frameFloat, CV_32FC3, 1.0 / 255);

    Mat resultFloat;
    multiply(alphaFloat, frameFloat, resultFloat);
    multiply(1.0 - alphaFloat, baseFloat, baseFloat);
    add(resultFloat, baseFloat, resultFloat);

    resultFloat.convertTo(output, CV_8UC3, 255);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_write_1vision_1ai_CameraActivity_processImage(JNIEnv* env,
                                                               jobject thiz,
                                                               jobject inputBitmap) {
    AndroidBitmapInfo info;
    void *pixels = nullptr;
    if (AndroidBitmap_getInfo(env, inputBitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to get Bitmap info");
        return nullptr;
    }
    if (AndroidBitmap_lockPixels(env, inputBitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to lock Bitmap pixels");
        return nullptr;
    }

    Mat src(info.height, info.width, CV_8UC4, pixels);
    Mat dst;
    processImageInternal(src, dst);

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmap = env->GetStaticMethodID(bitmapClass,
                                                    "createBitmap",
                                                    "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID fid = env->GetStaticFieldID(configClass, "ARGB_8888",
                                         "Landroid/graphics/Bitmap$Config;");
    jobject argb8888 = env->GetStaticObjectField(configClass, fid);
    jobject outputBitmap = env->CallStaticObjectMethod(bitmapClass,
                                                       createBitmap,
                                                       static_cast<jint>(info.width),
                                                       static_cast<jint>(info.height),
                                                       argb8888);

    void *outPixels;
    AndroidBitmap_lockPixels(env, outputBitmap, &outPixels);
    memcpy(outPixels, dst.data, dst.total() * dst.elemSize());
    AndroidBitmap_unlockPixels(env, outputBitmap);
    AndroidBitmap_unlockPixels(env, inputBitmap);
    return outputBitmap;

    // Convierte Bitmap a Mat (Android bitmap to OpenCV Mat)
    Mat bitmapToMat(JNIEnv *env, jobject bitmap);
}
