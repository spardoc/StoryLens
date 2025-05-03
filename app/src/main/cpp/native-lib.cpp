#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>

using namespace cv;
static const char* TAG = "NativeProcessor";

// ——————————————
//  Parámetros más suaves
// ——————————————
constexpr int   DENOISE_FILTER_SIZE       = 7;     // pequeño kernel
constexpr int   DENOISE_FILTER_SIGMA_COLOR= 50;    // menos color smoothing
constexpr int   DENOISE_FILTER_SIGMA_SPACE= 50;    // menos edge smoothing

constexpr double CLAHE_CLIP_LIMIT         = 1.5;   // menos contraste
constexpr int    CLAHE_TILE_SIZE          = 8;

constexpr double USM_SIGMA                = 1.0;   // desenfoque más ligero
constexpr double USM_AMOUNT               = 1.0;   // máscara menos marcada

constexpr float  BLEND_ALPHA              = 0.7f;  // 70% procesado + 30% original


void processImageInternal(const Mat& input, Mat& output) {
    // 1) Pasamos a BGR
    Mat bgr;  cvtColor(input, bgr, COLOR_RGBA2BGR);

    // 2) Denoise suave
    Mat denoised;
    bilateralFilter(bgr, denoised,
                    DENOISE_FILTER_SIZE,
                    DENOISE_FILTER_SIGMA_COLOR,
                    DENOISE_FILTER_SIGMA_SPACE);

    // 3) CLAHE ligero en L-channel
    Mat lab;
    cvtColor(denoised, lab, COLOR_BGR2Lab);
    std::vector<Mat> ch(3);
    split(lab, ch);
    Ptr<CLAHE> clahe = createCLAHE(CLAHE_CLIP_LIMIT, Size(CLAHE_TILE_SIZE, CLAHE_TILE_SIZE));
    clahe->apply(ch[0], ch[0]);
    merge(ch, lab);
    Mat contrastEnhanced;
    cvtColor(lab, contrastEnhanced, COLOR_Lab2BGR);

    // 4) Unsharp mask muy suave
    Mat blurred;
    GaussianBlur(contrastEnhanced, blurred, Size(), USM_SIGMA);
    Mat usm;
    addWeighted(contrastEnhanced, 1.0 + USM_AMOUNT,
                blurred, -USM_AMOUNT,
                0, usm);

    // 5) Convertir a gris y denoise final muy ligero
    Mat gray;
    cvtColor(usm, gray, COLOR_BGR2GRAY);

    Mat finalDenoised;
    // valor h más bajo para conservar detalle
    fastNlMeansDenoising(gray, finalDenoised, 15, 7, 21);

    // 6) Mezcla con el original gris para suavizar
    Mat origGray;
    cvtColor(bgr, origGray, COLOR_BGR2GRAY);
    Mat blended;
    addWeighted(finalDenoised, BLEND_ALPHA,
                origGray, 1.0f - BLEND_ALPHA,
                0, blended);

    // 7) A salida en RGBA
    cvtColor(blended, output, COLOR_GRAY2RGBA);
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

void makeRoundedRectMask(Mat& mask, int w, int h) {
    int margin       = 150;             // margen interior al borde de la imagen
    int cornerRadius = 200;             // radio de las esquinas redondeadas
    // Coordenadas de la caja
    int x1 = margin;
    int y1 = margin;
    int x2 = w - margin;
    int y2 = h - margin;

    // 2) Dibujar el cuerpo central (rectángulo horizontal)
    rectangle(mask,
              Point(x1 + cornerRadius, y1),
              Point(x2 - cornerRadius, y2),
              Scalar(255),
              FILLED);

    // 3) Dibujar el cuerpo vertical (rectángulo vertical)
    rectangle(mask,
              Point(x1, y1 + cornerRadius),
              Point(x2, y2 - cornerRadius),
              Scalar(255),
              FILLED);

    // 4) Añadir cuatro círculos en las esquinas
    circle(mask,
           Point(x1 + cornerRadius, y1 + cornerRadius),
           cornerRadius,
           Scalar(255),
           FILLED);
    circle(mask,
           Point(x2 - cornerRadius, y1 + cornerRadius),
           cornerRadius,
           Scalar(255),
           FILLED);
    circle(mask,
           Point(x1 + cornerRadius, y2 - cornerRadius),
           cornerRadius,
           Scalar(255),
           FILLED);
    circle(mask,
           Point(x2 - cornerRadius, y2 - cornerRadius),
           cornerRadius,
           Scalar(255),
           FILLED);
}

void makeEllipseMask(Mat& mask, int w, int h) {
    Point center(w/2, h/2);
    Size axes(w/2 - 10, h/3);
    ellipse(mask, center, axes, 0, 0, 360, Scalar(255), FILLED);
}

void makeBubbleMask(Mat& mask, int w, int h) {
    // Tamaño de los ejes de la elipse (ajustable)
    Size axes(w / 2 - 10, h / 3);  // horizontal y vertical
    // Radios base (con margen interior para que no toquen el borde)
    double rx = (w / 2.5) - 10;
    double ry = (h / 2.5) - 10;

    int spikes      = 16;
    double var      = 0.3;
    Point center(w/2, h/2);
    double angleStep   = CV_PI / spikes;
    double angleOffset = angleStep / 2.0;

    // Margen (en píxeles) para que no se peguen al borde
    double margin = 5.0;

    std::vector<Point> points;
    points.reserve(spikes * 2);

    for (int i = 0; i < spikes * 2; ++i) {
        double angle = angleOffset + i * angleStep;
        double cosA = std::cos(angle), sinA = std::sin(angle);

        // radio “base” para valle o pico
        double baseRX = (i % 2 == 0) ? rx : rx * (1.0 + var);
        double baseRY = (i % 2 == 0) ? ry : ry * (1.0 + var);

        // radio “ideal” en esa dirección (distancia real)
        double desiredDist = std::hypot(baseRX * cosA, baseRY * sinA);

        // calculamos hasta dónde podemos llegar sin salirme de la imagen:
        // en X: según signo de cosA
        double maxDistX = (cosA > 0)
                          ? ( (w - margin - center.x) / cosA )
                          : ( (0 + margin - center.x) / cosA );
        // en Y: según signo de sinA
        double maxDistY = (sinA > 0)
                          ? ( (h - margin - center.y) / sinA )
                          : ( (0 + margin - center.y) / sinA );

        // nos quedamos con el más restrictivo (en valor absoluto)
        double allowedDist = std::min(std::abs(maxDistX), std::abs(maxDistY));

        // factor de escala si desiredDist > allowedDist
        double scale = (desiredDist > allowedDist)
                       ? (allowedDist / desiredDist)
                       : 1.0;

        // punto final
        int x = static_cast<int>(center.x + baseRX * cosA * scale);
        int y = static_cast<int>(center.y + baseRY * sinA * scale);
        points.emplace_back(x, y);
    }

    // rellenar máscara y copiar como antes
    fillPoly(mask, std::vector<std::vector<Point>>{points}, Scalar(255));
}

void makeCloudMask(Mat& mask, int w, int h) {
    int r = std::min(w, h) / 5;  // radio un poco mayor
    int y0 = h / 2 - r / 3;      // ligeramente arriba del centro

    // Fila superior (7 círculos)
    std::vector<Point> centersTop;
    for (int i = 0; i < 3; ++i) {
        float fx = 0.4f + 0.9f * (i / 6.0f);
        int dy = (i % 2 == 0) ? -r / 6 : r / 8;
        centersTop.emplace_back(Point(int(w * fx), y0 + dy));
    }

    // Fila inferior más baja (5 círculos)
    std::vector<Point> centersBottom;
    for (int i = 0; i < 4; ++i) {
        float fx = 0.3f + 0.7f * (i / 4.0f);  // un poco más centrado
        centersBottom.emplace_back(Point(int(w * fx), y0 + r * 0.9f));
    }

    // Dibujar todos los círculos
    for (auto& c : centersTop)    circle(mask, c, r, Scalar(255), FILLED);
    for (auto& c : centersBottom) circle(mask, c, r * 0.9f, Scalar(255), FILLED);  // más pequeños

    // Círculo central inferior para dar base redondeada extra
    circle(mask, Point(w / 2, y0 + r), r + 10, Scalar(255), FILLED);

    // (opcional) suavizar un poco los bordes
    Mat kernel = getStructuringElement(MORPH_ELLIPSE, Size(9, 9));
    morphologyEx(mask, mask, MORPH_CLOSE, kernel);
}


extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_write_1vision_1ai_SelectFrameActivity_processDrawing(
        JNIEnv* env,
        jobject /* this */,
        jobject textBitmap,
        jobject /* drawingBitmap */,
        jint shapeType) {

    const char* TAG = "DrawFrameActivity";
    AndroidBitmapInfo info;
    void* pixelsText = nullptr;

    // 1) Lock y clonar solo el bitmap de texto
    if ( AndroidBitmap_getInfo(env, textBitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS ||
         AndroidBitmap_lockPixels(env, textBitmap, &pixelsText) != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Error al lockear textBitmap");
        return nullptr;
    }
    Mat srcText(info.height, info.width, CV_8UC4, pixelsText);
    Mat textMat = srcText.clone();
    AndroidBitmap_unlockPixels(env, textBitmap);

    // 2) Crear máscara con forma de elipse centrada
    int w = info.width, h = info.height;

// 1) Crear máscara en negro
    Mat mask = Mat::zeros(textMat.size(), CV_8UC1);

    switch (shapeType) {
        case 0:  // Rectángulo redondeado
            makeRoundedRectMask(mask, w, h);
            break;
        case 1:  // Elipse
            makeEllipseMask(mask, w, h);
            break;
        case 2:  // Bocadillo
            makeBubbleMask(mask, w, h);
            break;
        case 3:  // Nube
            makeCloudMask(mask, w, h);
            break;
        default:
            makeEllipseMask(mask, w, h);
    }

    // 3) Aplicar máscara de nube sobre textMat
    Mat result = Mat::zeros(textMat.size(), textMat.type());
    textMat.copyTo(result, mask);

    // 4) Convertir a Bitmap Android y devolver
    jclass bmpCls = env->FindClass("android/graphics/Bitmap");
    jmethodID createBmp = env->GetStaticMethodID(
            bmpCls, "createBitmap",
            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jclass cfgCls = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID fid = env->GetStaticFieldID(cfgCls, "ARGB_8888",
                                         "Landroid/graphics/Bitmap$Config;");
    jobject cfg = env->GetStaticObjectField(cfgCls, fid);
    jobject outBmp = env->CallStaticObjectMethod(
            bmpCls, createBmp,
            (jint)w, (jint)h, cfg);

    void* outPixels = nullptr;
    if (AndroidBitmap_lockPixels(env, outBmp, &outPixels) == ANDROID_BITMAP_RESULT_SUCCESS) {
        Mat outMat(h, w, CV_8UC4, outPixels);
        result.copyTo(outMat);
        AndroidBitmap_unlockPixels(env, outBmp);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Error al lockear outputBitmap");
        return nullptr;
    }

    return outBmp;
}
