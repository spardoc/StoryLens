package com.example.write_vision_ai.main;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.write_vision_ai.R;

public class ComposeActivity extends AppCompatActivity {

    private ImageView baseImageView;
    private ImageView draggableBubble;
    private Button btnFinish;
    private float dX, dY;                  // ← campos de clase

    // Para el pinch‑to‑zoom
    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.0f;

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

        // Cargo el fondo con Glide
        Glide.with(this).asBitmap().load(baseUrl).into(baseImageView);

        // Cargo el PNG procesado
        Bitmap bubbleBmp = BitmapFactory.decodeFile(procPath);
        draggableBubble.setImageBitmap(bubbleBmp);
        draggableBubble.bringToFront();
        draggableBubble.setVisibility(View.VISIBLE);

        // Iniciamos el detector de escalado
        scaleDetector = new ScaleGestureDetector(this, new ScaleListener());

        // Listener combinado: primero pinch, luego drag
        draggableBubble.setOnTouchListener((view, ev) -> {
            // 1) Pinch‑to‑zoom
            scaleDetector.onTouchEvent(ev);
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);

            // 2) Drag sólo si no está pellizcando
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
            // TODO: aquí combinas la imagen final y la guardas
            Toast.makeText(this, "¡Composición finalizada!", Toast.LENGTH_SHORT).show();
        });
    }

    // Detecta el factor de escala del pellizco
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            // Limitar tamaño mínimo/máximo
            scaleFactor = Math.max(0.3f, Math.min(scaleFactor, 3.0f));
            return true;
        }
    }
}
