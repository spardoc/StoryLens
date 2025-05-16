package com.example.write_vision_ai.main;


import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.write_vision_ai.R;



import android.content.Context;
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        baseImageView   = findViewById(R.id.baseImageView);
        draggableBubble = findViewById(R.id.draggableBubble);
        btnFinish       = findViewById(R.id.btnFinishCompose);

        String baseUrl  = getIntent().getStringExtra("base_image_url");
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
        draggableBubble.setOnTouchListener((view, ev) -> {
            scaleDetector.onTouchEvent(ev);
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);

            if (!scaleDetector.isInProgress()) {
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - ev.getRawX();
                        dY = view.getY() - ev.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        view.setX(ev.getRawX() + dX);
                        view.setY(ev.getRawY() + dY);
                        return true;
                }
            }
            return true;
        });

        btnFinish.setOnClickListener(v -> {
            baseImageView.setDrawingCacheEnabled(true);
            Bitmap base = Bitmap.createBitmap(baseImageView.getDrawingCache());
            baseImageView.setDrawingCacheEnabled(false);

            Bitmap composedBitmap = Bitmap.createBitmap(
                    base.getWidth(), base.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(composedBitmap);
            canvas.drawBitmap(base, 0f, 0f, null);

            float left = draggableBubble.getX() - baseImageView.getX();
            float top = draggableBubble.getY() - baseImageView.getY();

            Matrix matrix = new Matrix();
            matrix.postScale(draggableBubble.getScaleX(), draggableBubble.getScaleY());
            Bitmap scaledBubble = Bitmap.createBitmap(
                    bubbleBitmap, 0, 0,
                    bubbleBitmap.getWidth(), bubbleBitmap.getHeight(),
                    matrix, true
            );

            canvas.drawBitmap(scaledBubble, left, top, null);

            saveImageToGallery(this, composedBitmap);
        });
    }

    private static void saveImageToGallery(Context context, Bitmap bitmap) {
        String filename = "compose_" + System.currentTimeMillis() + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Compositions");

        Uri imageUri = context.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (imageUri != null) {
            try (OutputStream out = context.getContentResolver().openOutputStream(imageUri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                Toast.makeText(context, "Imagen guardada en galería", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(context, "Error al guardar imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "No se pudo guardar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.3f, Math.min(scaleFactor, 3.0f));
            return true;
        }
    }
}