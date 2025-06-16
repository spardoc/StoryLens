package com.example.write_vision_ai.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PdfGenerator {

    public interface PdfExportCallback {
        void onSuccess(File pdfFile);
        void onFailure(Exception e);
    }

    public static void generateAndSavePdf(Context context, List<String> imagePaths, String fileName, PdfExportCallback callback) {
        new Thread(() -> {
            try {
                List<Bitmap> bitmaps = new ArrayList<>();
                for (String path : imagePaths) {
                    Bitmap bitmap = loadBitmap(context, path);
                    if (bitmap != null) bitmaps.add(bitmap);
                }

                if (bitmaps.isEmpty()) throw new IOException("No se pudieron cargar las imágenes.");

                File pdfFile = export(context, bitmaps, fileName);

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(pdfFile));

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }
        }).start();
    }

    private static Bitmap loadBitmap(Context context, String path) {
        try {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                return BitmapFactory.decodeStream(new URL(path).openStream());
            } else if (path.startsWith("content://")) {
                InputStream input = context.getContentResolver().openInputStream(Uri.parse(path));
                return BitmapFactory.decodeStream(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File export(Context context, List<Bitmap> images, String fileName) throws IOException {
        int factor = 2;
        int pageWidth = 595 * factor;
        int pageHeight = 842 * factor;

        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint();
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(10);  // Borde grueso tipo cómic

        int margin = 8 * factor;           // ← reducido (antes 20*f)
        int spacing = 10 * factor;         // espacio entre imágenes
        int cols = 2, rows = 4;
        int imgW = (pageWidth - (cols * margin) - ((cols - 1) * spacing)) / cols;
        int imgH = (pageHeight - (rows * margin) - ((rows - 1) * spacing)) / rows;

        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdf.startPage(pi);
        Canvas c = page.getCanvas();

        int x = margin, y = margin, count = 0;

        for (Bitmap bmp : images) {
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, imgW, imgH, true);
            c.drawBitmap(scaled, x, y, paint);
            c.drawRect(x, y, x + imgW, y + imgH, borderPaint);

            count++;
            if (count % cols == 0) {
                x = margin;
                y += imgH + spacing;
            } else {
                x += imgW + spacing;
            }

            if (count % (cols * rows) == 0 && count < images.size()) {
                pdf.finishPage(page);
                pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, (count / (cols * rows)) + 1).create();
                page = pdf.startPage(pi);
                c = page.getCanvas();
                x = margin;
                y = margin;
            }
        }

        pdf.finishPage(page);

        File pdfFile = new File(context.getExternalFilesDir(null), fileName);
        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            pdf.writeTo(fos);
        }
        pdf.close();

        return pdfFile;
    }
}