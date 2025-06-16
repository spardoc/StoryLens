package com.example.write_vision_ai.main;

import static com.example.write_vision_ai.utils.StoryConstants.basePrompts;
import static com.example.write_vision_ai.utils.StoryConstants.options;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;

import com.example.write_vision_ai.data.ApiService;
import com.example.write_vision_ai.data.adapters.ImageAdapter;
import com.example.write_vision_ai.R;
import com.example.write_vision_ai.auth.LoginActivity;
import com.example.write_vision_ai.manager.FirebaseManager;
import com.example.write_vision_ai.manager.StoryManager;
import com.example.write_vision_ai.utils.ImageGenerator;
import com.example.write_vision_ai.utils.PdfGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


import android.widget.LinearLayout;
import android.widget.Spinner;
import android.graphics.Bitmap;

public class MainActivity extends AppCompatActivity implements ImageAdapter.OnAddTextClickListener {

    private Button btnGenerate, btnLogout;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private ApiService apiService;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<String> imageUrls = new ArrayList<>();
    private LinearLayout storyLayout;
    private Button btnPreviewStory;
    private Button btnExportPdf;
    private static final int REQ_CAPTURE_TEXT = 2001;
    private static final int REQ_SELECT_FRAME = 2002;
    private int pendingImageIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de Main
        initializeFirebase();
        initializeViews();
        setupRecyclerView();
        setupRetrofit();
        setupLogoutButton();
        setupExportPdfButton();

        StoryManager storyManager = new StoryManager(
                this,
                storyLayout,
                basePrompts,
                options
        );
        storyManager.setupStoryBuilder();

        ImageGenerator imageGenerator = new ImageGenerator(
                this,
                storyManager.getSpinners(),
                basePrompts,
                imageUrls,
                imageAdapter,
                apiService
        );

        btnGenerate.setOnClickListener(view -> {
            Log.d("BUTTON", "Click detectado");
            imageGenerator.generateImages();
        });

    }

    @Override
    public void onAddTextClicked(String imageUrl) {
        pendingImageIndex = imageUrls.indexOf(imageUrl);
        if (pendingImageIndex == -1) return;

        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("base_image_url", imageUrl);
        startActivityForResult(intent, REQ_CAPTURE_TEXT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == REQ_CAPTURE_TEXT || requestCode == REQ_SELECT_FRAME) && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("final_image_path")) {
                String finalImagePath = data.getStringExtra("final_image_path");
                if (finalImagePath != null && pendingImageIndex != -1) {
                    imageUrls.set(pendingImageIndex, finalImagePath);
                    imageAdapter.notifyItemChanged(pendingImageIndex);
                    pendingImageIndex = -1;
                }
            }
        }
    }

    private void initializeViews() {
        btnPreviewStory = findViewById(R.id.previewView);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnLogout = findViewById(R.id.btnLogout);
        recyclerView = findViewById(R.id.recyclerView);
        storyLayout = findViewById(R.id.storyLayout);
        btnExportPdf = findViewById(R.id.btnExportPdf);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(imageUrls, this);
        recyclerView.setAdapter(imageAdapter);
    }

    private void setupRetrofit() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    Log.d("API_REQUEST", "Request URL: " + request.url());
                    return chain.proceed(request);
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    private void initializeFirebase() {
        FirebaseManager.initialize();
        mAuth = FirebaseManager.getAuth();
        db = FirebaseManager.getFirestore();
    }

    private void setupLogoutButton() {
        btnLogout.setOnClickListener(view -> {
            FirebaseManager.logout(new FirebaseManager.OnLogoutListener() {
                @Override
                public void onLogoutSuccess() {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    });
                }

                @Override
                public void onLogoutFailure(String errorMessage) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al cerrar sesión: " + errorMessage, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }

    private void setupExportPdfButton() {
        btnExportPdf.setOnClickListener(v -> new Thread(() -> {
            List<String> imagePaths = imageAdapter.getCompositedImages();
            List<Bitmap> bitmaps = new ArrayList<>();

            for (String path : imagePaths) {
                Bitmap bitmap = null;
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    bitmap = PdfGenerator.loadBitmapFromUrl(path);
                } else if (path.startsWith("content://")) {
                    bitmap = PdfGenerator.loadBitmapFromUri(this, path);
                }
                if (bitmap != null) {
                    bitmaps.add(bitmap);
                }
            }

            if (!bitmaps.isEmpty()) {
                try {
                    PdfGenerator.export(MainActivity.this, bitmaps, "comic_historia.pdf");
                    runOnUiThread(this::abrirPdfGenerado);
                } catch (IOException e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al generar PDF", Toast.LENGTH_SHORT).show());
                }
            } else {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "No se pudieron cargar imágenes", Toast.LENGTH_SHORT).show());
            }
        }).start());
    }

    private void abrirPdfGenerado() {
        File pdfFile = new File(getExternalFilesDir(null), "comic_historia.pdf");
        Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay aplicación para abrir PDF", Toast.LENGTH_LONG).show();
        }
    }
}