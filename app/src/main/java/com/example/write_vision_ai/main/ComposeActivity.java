package com.example.write_vision_ai.main;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.example.write_vision_ai.R;

import java.io.OutputStream;

public class ComposeActivity extends AppCompatActivity {
    private ImageView baseImageView;
    private ImageView draggableBubble;
    private Button btnFinish;
    private float dX, dY;
    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.0f;
    private Bitmap bubbleBitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        baseImageView = findViewById(R.id.baseImageView);
        draggableBubble = findViewById(R.id.draggableBubble);
        btnFinish = findViewById(R.id.btnFinishCompose);

        String baseUrl = getIntent().getStringExtra("base_image_url");
        String procPath = getIntent().getStringExtra("processed_image_path");

        if (baseUrl == null || procPath == null) {
            Toast.makeText(this, "Faltan datos para componer", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Glide.with(this).asBitmap().load(baseUrl).into(baseImageView);
        bubbleBitmap = BitmapFactory.decodeFile(procPath);
        draggableBubble.setImageBitmap(bubbleBitmap);
        draggableBubble.setVisibility(View.VISIBLE);
        draggableBubble.bringToFront();

        scaleDetector = new ScaleGestureDetector(this, new ScaleListener());

        draggableBubble.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    v.setX(event.getRawX() + dX);
                    v.setY(event.getRawY() + dY);
                    break;
            }

            return true;
        });

        btnFinish.setOnClickListener(v -> {
            btnFinish.setVisibility(View.INVISIBLE);

            new Thread(() -> {
                try {
                    Bitmap originalBitmap = Glide.with(this)
                            .asBitmap()
                            .load(getIntent().getStringExtra("base_image_url"))
                            .submit()
                            .get();

                    int originalWidth = originalBitmap.getWidth();
                    int originalHeight = originalBitmap.getHeight();

                    int viewWidth = baseImageView.getWidth();
                    int viewHeight = baseImageView.getHeight();

                    // Tamaño y escala del bubble
                    int bubbleW = bubbleBitmap.getWidth();
                    int bubbleH = bubbleBitmap.getHeight();
                    float aspectRatio = (float) bubbleW / bubbleH;

                    float bubbleScale = (draggableBubble.getScaleX() + draggableBubble.getScaleY()) / 2f;
                    float bubbleVisualW = bubbleW * bubbleScale;
                    float bubbleVisualH = bubbleH * bubbleScale;

                    // Obtener matriz y valores de transformación de la imagen en ImageView
                    Matrix matrix = baseImageView.getImageMatrix();
                    float[] values = new float[9];
                    matrix.getValues(values);

                    float scaleX = values[Matrix.MSCALE_X];
                    float scaleY = values[Matrix.MSCALE_Y];
                    float transX = values[Matrix.MTRANS_X];
                    float transY = values[Matrix.MTRANS_Y];

                    // Calcular tamaño real del bubble en la imagen original
                    float realW = bubbleVisualW / scaleX;
                    float realH = bubbleVisualH / scaleY;

                    // Limitar tamaño al 80% de la imagen original
                    float maxW = originalWidth * 0.8f;
                    float maxH = originalHeight * 0.8f;
                    float limitFactor = Math.min(1f, Math.min(maxW / realW, maxH / realH));
                    realW *= limitFactor;
                    realH *= limitFactor;

                    // Reajustar tamaño para mantener aspecto original
                    if (aspectRatio >= 1f) {
                        realW = Math.min(realW, maxW);
                        realH = realW / aspectRatio;
                    } else {
                        realH = Math.min(realH, maxH);
                        realW = realH * aspectRatio;
                    }

                    // Posición real del bubble en la imagen original

                    int[] bubbleLoc = new int[2];
                    int[] imageLoc = new int[2];
                    draggableBubble.getLocationOnScreen(bubbleLoc);
                    baseImageView.getLocationOnScreen(imageLoc);

                    // Posición relativa dentro del ImageView, ajustando con la traslación (offset) de la imagen

                    float ajusteHorizontal = 150f;  // Ajusta este valor según convenga
                    float ajusteVertical = 50f;

                    float relativeX = bubbleLoc[0] - imageLoc[0] - transX + ajusteHorizontal;

                    float relativeY = bubbleLoc[1] - imageLoc[1] - transY - ajusteVertical;

                    // Posición real en la imagen original
                    float realX = relativeX / scaleX;
                    float realY = relativeY / scaleY;

                    // Logs para depurar
                    Log.d("ComposeActivity", "realX/Y: " + realX + "," + realY +
                            "  realW/H: " + (int) realW + "x" + (int) realH);

                    // Componer imagen
                    Bitmap composedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(composedBitmap);

                    Bitmap scaledBubble = Bitmap.createScaledBitmap(bubbleBitmap, (int) realW, (int) realH, true);
                    canvas.drawBitmap(scaledBubble, realX, realY, null);

                    Uri savedUri = saveImageToGallery(composedBitmap);

                    runOnUiThread(() -> {
                        btnFinish.setVisibility(View.VISIBLE);
                        Intent resultIntent = new Intent();
                        if (savedUri != null) {
                            resultIntent.putExtra("final_image_path", savedUri.toString());
                            setResult(RESULT_OK, resultIntent);
                        } else {
                            setResult(RESULT_CANCELED);
                        }
                        finish();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        btnFinish.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Error al componer imagen", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });
    }

    private Rect getImageBounds(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) return new Rect();

        // Tamaño de la imagen original
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();

        // Tamaño del ImageView
        int imageViewWidth = imageView.getWidth();
        int imageViewHeight = imageView.getHeight();

        float scale;
        float dx = 0, dy = 0;

        if (intrinsicWidth * imageViewHeight > imageViewWidth * intrinsicHeight) {
            scale = (float) imageViewWidth / (float) intrinsicWidth;
            dy = (imageViewHeight - intrinsicHeight * scale) * 0.5f;
        } else {
            scale = (float) imageViewHeight / (float) intrinsicHeight;
            dx = (imageViewWidth - intrinsicWidth * scale) * 0.5f;
        }

        int left = (int) dx;
        int top = (int) dy;
        int right = (int) (left + intrinsicWidth * scale);
        int bottom = (int) (top + intrinsicHeight * scale);

        return new Rect(left, top, right, bottom);
    }


    private Uri saveImageToGallery(Bitmap bitmap) {
        String displayName = "composed_" + System.currentTimeMillis() + ".png";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/WriteVision");

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.3f, Math.min(scaleFactor, 3.0f));
            draggableBubble.setScaleX(scaleFactor);
            draggableBubble.setScaleY(scaleFactor);
            return true;
        }
    }
}
