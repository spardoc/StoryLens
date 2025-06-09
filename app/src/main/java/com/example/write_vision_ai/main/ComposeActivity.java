package com.example.write_vision_ai.main;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
            FrameLayout compositeContainer = findViewById(R.id.composeContainer);
            btnFinish.setVisibility(View.INVISIBLE);

            compositeContainer.post(() -> {
                Bitmap composedBitmap = Bitmap.createBitmap(
                        compositeContainer.getWidth(),
                        compositeContainer.getHeight(),
                        Bitmap.Config.ARGB_8888
                );

                Canvas canvas = new Canvas(composedBitmap);
                compositeContainer.draw(canvas);

                btnFinish.setVisibility(View.VISIBLE);

                Uri savedUri = saveImageToGallery(composedBitmap);

                Intent resultIntent = new Intent();
                if (savedUri != null) {
                    resultIntent.putExtra("final_image_path", savedUri.toString());
                    setResult(RESULT_OK, resultIntent);
                } else {
                    setResult(RESULT_CANCELED);
                }
                finish();
            });
        });

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
