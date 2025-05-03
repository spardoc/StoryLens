package com.example.write_vision_ai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SelectFrameActivity extends AppCompatActivity {

    static { System.loadLibrary("write_vision_ai"); }

    private FrameLayout frameContainer;
    private ImageView imagePreview;
    private DrawingView drawingView;
    private Spinner spinnerShape;
    private Button btnConfirm;
    private Bitmap textBitmap;

    // Variables para drag
    private float dX, dY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_frame);

        frameContainer  = findViewById(R.id.frameContainer);
        imagePreview    = findViewById(R.id.imagePreview);
        drawingView     = findViewById(R.id.drawingView);
        spinnerShape    = findViewById(R.id.spinnerShape);
        btnConfirm      = findViewById(R.id.btnConfirm);

        // Carga imagen de texto (burbuja sin trazo)
        String imagePath = getIntent().getStringExtra("text_image_path");
        if (imagePath != null) {
            textBitmap = BitmapFactory.decodeFile(imagePath);
            imagePreview.setImageBitmap(textBitmap);
        } else {
            Toast.makeText(this, "Imagen no encontrada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Al cambiar la selección del Spinner, procesar y actualizar preview
        spinnerShape.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                processAndPreview(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* no-op */ }
        });

        btnConfirm.setOnClickListener(v -> {
            // Guardar bitmap procesado y lanzar ComposeActivity
            File outFile = new File(getCacheDir(), "framed_text.png");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                // obtener la última imagen mostrada en imagePreview
                imagePreview.buildDrawingCache();
                Bitmap result = imagePreview.getDrawingCache();
                result.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al guardar imagen", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lanzar ComposeActivity con la ruta de la imagen y la base URL
            Intent compose = new Intent(SelectFrameActivity.this, ComposeActivity.class);
            String baseUrl = getIntent().getStringExtra("base_image_url");
            compose.putExtra("base_image_url", baseUrl);
            compose.putExtra("processed_image_path", outFile.getAbsolutePath());
            startActivity(compose);
            finish();
        });
    }

    /**
     * Procesa la imagen según la opción y actualiza el preview y el arrastre.
     */
    private void processAndPreview(int shapeType) {
        Bitmap drawing = drawingView.exportDrawing();
        Bitmap scaled = Bitmap.createScaledBitmap(
                drawing,
                textBitmap.getWidth(),
                textBitmap.getHeight(),
                false
        );
        Bitmap result = processDrawing(textBitmap, scaled, shapeType);
        if (result == null) {
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        imagePreview.setImageBitmap(result);
        imagePreview.bringToFront();
        imagePreview.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dX = view.getX() - event.getRawX();
                    dY = view.getY() - event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    view.setX(event.getRawX() + dX);
                    view.setY(event.getRawY() + dY);
                    return true;
                default:
                    return false;
            }
        });

        Toast.makeText(this, "Contorno aplicado: ajústalo si lo deseas", Toast.LENGTH_SHORT).show();
    }

    public native Bitmap processDrawing(Bitmap text, Bitmap drawing, int shapeType);
}
