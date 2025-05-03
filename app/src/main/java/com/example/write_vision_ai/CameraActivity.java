package com.example.write_vision_ai;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_FRAME = 101;

    static {
        System.loadLibrary("write_vision_ai");
    }

    private PreviewView previewView;
    private ImageView imageCaptureView;
    private Button btnCapture, btnConfirm, btnRetry;
    private LinearLayout controlsLayout;
    private ImageCapture imageCapture;

    public CameraActivity() throws IOException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        imageCaptureView = findViewById(R.id.imageCaptureView);
        btnCapture = findViewById(R.id.btnCapture);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnRetry = findViewById(R.id.btnRetry);
        controlsLayout = findViewById(R.id.controlsLayout);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        }

        btnCapture.setOnClickListener(v -> takePhoto());
        btnRetry.setOnClickListener(v -> resetPreview());
        btnConfirm.setOnClickListener(v -> confirmPhoto());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        Bitmap bmp = toBitmap(image);
                        image.close();

                        runOnUiThread(() -> {
                            previewView.setVisibility(View.GONE);
                            imageCaptureView.setImageBitmap(bmp);
                            imageCaptureView.setVisibility(View.VISIBLE);
                            btnCapture.setVisibility(View.GONE);
                            controlsLayout.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        super.onError(exception);
                        Toast.makeText(CameraActivity.this,
                                "Error al capturar: " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private Bitmap toBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void resetPreview() {
        imageCaptureView.setVisibility(View.GONE);
        controlsLayout.setVisibility(View.GONE);
        btnCapture.setVisibility(View.VISIBLE);
        previewView.setVisibility(View.VISIBLE);
    }

    private void confirmPhoto() {
        Bitmap original = ((BitmapDrawable) imageCaptureView.getDrawable()).getBitmap();
        Bitmap processed = processImage(original);

        // guarda en cache...
        File cache = new File(getCacheDir(), "text_processed.png");
        try (FileOutputStream fos = new FileOutputStream(cache)) {
            processed.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construimos el Intent a DrawFrameActivity:
        Intent intent = new Intent(this, SelectFrameActivity.class);
        intent.putExtra("text_image_path", cache.getAbsolutePath());

        // Propagamos la URL base que recibimos de MainActivity:
        String baseUrl = getIntent().getStringExtra("base_image_url");
        if (baseUrl != null) {
            intent.putExtra("base_image_url", baseUrl);
        }

        startActivityForResult(intent, REQUEST_FRAME);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
        }
    }

    public native Bitmap processImage(Bitmap inputBitmap);

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FRAME && resultCode == RESULT_OK) {
            String framedPath = data.getStringExtra("framed_image_path");
            if (framedPath != null) {
                Bitmap framed = BitmapFactory.decodeFile(framedPath);
                // Aquí ya tienes la imagen del texto con su viñeta
                imageCaptureView.setImageBitmap(framed);
                // Y si quieres, puedes continuar el flujo de insertar en el cómic
            }
        }
    }

}
