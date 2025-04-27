package com.example.write_vision_ai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileOutputStream;
import java.io.IOException;

public class DrawFrameActivity extends AppCompatActivity {

    static { System.loadLibrary("write_vision_ai"); }

    private FrameLayout frameContainer;
    private ImageView imagePreview;
    private DrawingView drawingView;
    private Button btnConfirm;
    private Bitmap textBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.draw_frame);

        frameContainer = findViewById(R.id.frameContainer);
        imagePreview   = findViewById(R.id.imagePreview);
        drawingView    = findViewById(R.id.drawingView);
        btnConfirm     = findViewById(R.id.btnConfirm);

        // Carga imagen de texto
        String imagePath = getIntent().getStringExtra("image_path");
        if (imagePath != null) {
            textBitmap = BitmapFactory.decodeFile(imagePath);
            imagePreview.setImageBitmap(textBitmap);
        } else {
            Toast.makeText(this, "Imagen no encontrada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnConfirm.setOnClickListener(v -> {
            Bitmap drawing = drawingView.exportDrawing(); // Obtén lo dibujado

            // Redimensionar el dibujo para que coincida con las dimensiones del textBitmap
            Bitmap scaledDrawing = Bitmap.createScaledBitmap(drawing, textBitmap.getWidth(), textBitmap.getHeight(), false);

            Bitmap result = processDrawing(textBitmap, scaledDrawing);
            if (result != null) {
                imagePreview.setImageBitmap(result); // Mostrar el resultado en el preview

                // (opcional) Guardarlo si quieres
                try {
                    FileOutputStream out = new FileOutputStream(getFilesDir() + "/processed_image.png");
                    result.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Puedes agregar un mensaje de éxito si deseas
                Toast.makeText(DrawFrameActivity.this, "Imagen procesada exitosamente", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public native Bitmap processDrawing(Bitmap text, Bitmap drawing);
}
