package com.example.write_vision_ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
/**
 * Vista personalizada para dibujar la viñeta y superponer un bitmap de texto.
 */
public class DrawingView extends View {
    private Paint paint;
    private Path path;
    private Bitmap overlayBitmap;  // Bitmap de la viñeta de texto

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(20f);
        path = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Dibuja la viñeta de texto si existe
        if (overlayBitmap != null) {
            // Ajusta el tamaño del bitmap al del View
            Bitmap scaled = Bitmap.createScaledBitmap(overlayBitmap, getWidth(), getHeight(), false);
            canvas.drawBitmap(scaled, 0, 0, null);
        }
        // Dibuja el trazado libre
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                // nada adicional
                break;
        }
        invalidate();
        return true;
    }

    /**
     * Establece el bitmap de la viñeta de texto que se superpondrá.
     * @param bitmap Bitmap de la viñeta de texto.
     */
    public void setOverlayBitmap(Bitmap bitmap) {
        this.overlayBitmap = bitmap;
        invalidate();
    }

    /**
     * Exporta el dibujo actual (viñeta + trazado) en un Bitmap.
     */
    public Bitmap exportDrawing() {
        Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        draw(canvas);
        return bmp;
    }

    /**
     * Limpia el dibujo (trazado) y la viñeta.
     */
    public void clear() {
        path.reset();
        overlayBitmap = null;
        invalidate();
    }
}
