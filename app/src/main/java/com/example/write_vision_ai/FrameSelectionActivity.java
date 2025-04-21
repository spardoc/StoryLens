package com.example.write_vision_ai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.FileOutputStream;

public class FrameSelectionActivity extends AppCompatActivity
        implements FrameAdapter.OnFrameSelectedListener {

    private FrameLayout frameContainer;
    private ImageView imagePreview, frameOverlay;
    private RecyclerView frameRecycler;
    private Button btnApplyFrame;
    private Bitmap textBitmap;

    // Scale detectors
    private ScaleGestureDetector frameScaleDetector;
    private ScaleGestureDetector imageScaleDetector;
    private float frameScaleFactor = 1f;
    private float imageScaleFactor = 1f;

    // Drag variables for frame
    private float frameLastX, frameLastY;
    private float framePosX = 0f, framePosY = 0f;

    // Drag variables for image
    private float imageLastX, imageLastY;
    private float imagePosX = 0f, imagePosY = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_select_frame);

        frameContainer = findViewById(R.id.frameContainer);
        imagePreview   = findViewById(R.id.imagePreview);
        frameOverlay   = findViewById(R.id.frameOverlay);
        frameRecycler  = findViewById(R.id.frameRecycler);
        btnApplyFrame  = findViewById(R.id.btnApplyFrame);

        frameRecycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        // Carga la imagen de texto procesada
        String imagePath = getIntent().getStringExtra("image_path");
        if (imagePath != null) {
            textBitmap = BitmapFactory.decodeFile(imagePath);
            imagePreview.setImageBitmap(textBitmap);
        } else {
            Toast.makeText(this, "No se encontró la imagen a enmarcar", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializa detectores de gesto
        frameScaleDetector = new ScaleGestureDetector(this, new FrameScaleListener());
        imageScaleDetector = new ScaleGestureDetector(this, new ImageScaleListener());

        // Touch listener para el overlay (drag + pinch)
        frameOverlay.setOnTouchListener((v, event) -> {
            frameScaleDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    frameLastX = event.getRawX() - framePosX;
                    frameLastY = event.getRawY() - framePosY;
                    break;
                case MotionEvent.ACTION_MOVE:
                    framePosX = event.getRawX() - frameLastX;
                    framePosY = event.getRawY() - frameLastY;
                    v.setTranslationX(framePosX);
                    v.setTranslationY(framePosY);
                    break;
            }
            return true;
        });

        // Touch listener para la imagen (drag + pinch)
        imagePreview.setOnTouchListener((v, event) -> {
            imageScaleDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    imageLastX = event.getRawX() - imagePosX;
                    imageLastY = event.getRawY() - imagePosY;
                    break;
                case MotionEvent.ACTION_MOVE:
                    imagePosX = event.getRawX() - imageLastX;
                    imagePosY = event.getRawY() - imageLastY;
                    v.setTranslationX(imagePosX);
                    v.setTranslationY(imagePosY);
                    break;
            }
            return true;
        });

        // Carga listado de frames
        try {
            String[] assets = getAssets().list("frames");
            List<String> paths = new ArrayList<>();
            for (String f : assets) paths.add("frames/" + f);
            frameRecycler.setAdapter(new FrameAdapter(this, paths, this));
        } catch (IOException e) { e.printStackTrace(); }

        btnApplyFrame.setOnClickListener(v -> returnFramedImage());
    }

    @Override
    public void onFrameSelected(@NonNull String assetPath) {
        try (InputStream is = getAssets().open(assetPath)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            frameOverlay.setImageBitmap(bmp);
            frameOverlay.setVisibility(View.VISIBLE);

            // Reset del frame
            frameScaleFactor = 1f;
            frameOverlay.setScaleX(frameScaleFactor);
            frameOverlay.setScaleY(frameScaleFactor);
            framePosX = framePosY = 0f;
            frameOverlay.setTranslationX(0f);
            frameOverlay.setTranslationY(0f);

            // Reset de la imagen de texto
            imageScaleFactor = 1f;
            imagePreview.setScaleX(imageScaleFactor);
            imagePreview.setScaleY(imageScaleFactor);
            imagePosX = imagePosY = 0f;
            imagePreview.setTranslationX(0f);
            imagePreview.setTranslationY(0f);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Combina fondo + overlay tal como está posicionado en pantalla */
    private void returnFramedImage() {
        Bitmap result = Bitmap.createBitmap(
                frameContainer.getWidth(),
                frameContainer.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        frameContainer.draw(canvas);

        try {
            FileOutputStream fos = openFileOutput("framed_text.png", MODE_PRIVATE);
            result.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Intent data = new Intent();
            data.putExtra("framed_image_path",
                    getFileStreamPath("framed_text.png").getAbsolutePath());
            setResult(RESULT_OK, data);
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error guardando viñeta", Toast.LENGTH_SHORT).show();
        }
    }

    /** Detecta pinch-to-zoom para el marco */
    private class FrameScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            frameScaleFactor *= detector.getScaleFactor();
            frameScaleFactor = Math.max(0.5f, Math.min(frameScaleFactor, 3.0f));
            frameOverlay.setScaleX(frameScaleFactor);
            frameOverlay.setScaleY(frameScaleFactor);
            return true;
        }
    }

    /** Detecta pinch-to-zoom para la imagen de texto */
    private class ImageScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            imageScaleFactor *= detector.getScaleFactor();
            imageScaleFactor = Math.max(0.5f, Math.min(imageScaleFactor, 3.0f));
            imagePreview.setScaleX(imageScaleFactor);
            imagePreview.setScaleY(imageScaleFactor);
            return true;
        }
    }
}