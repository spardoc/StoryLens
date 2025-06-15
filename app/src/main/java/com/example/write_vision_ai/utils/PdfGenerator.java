package com.example.write_vision_ai.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PdfGenerator {
    public static void export(Context context, List<Bitmap> images, String fileName) throws IOException {
        int factor = 2;
        int pageWidth = 595 * factor;
        int pageHeight = 842 * factor;

        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint();
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6); // Borde tipo cómic

        int margin = 20 * factor;
        int cols = 2;
        int rows = 4;
        int imgW = (pageWidth - (cols + 1) * margin) / cols;
        int imgH = imgW;

        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        int x = margin, y = margin, count = 0;
        PdfDocument.Page page = pdf.startPage(pi);
        Canvas c = page.getCanvas();

        for (Bitmap bmp : images) {
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, imgW, imgH, true);
            c.drawBitmap(scaled, x, y, paint);

            // 👇 Borde negro estilo cómic
            c.drawRect(x, y, x + imgW, y + imgH, borderPaint);

            count++;
            if (count % cols == 0) {
                x = margin;
                y += imgH + margin;
            } else {
                x += imgW + margin;
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
        pdfFile.createNewFile();
        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            pdf.writeTo(fos);
        }
        pdf.close();
    }
}
