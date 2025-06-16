package com.example.write_vision_ai.main;



import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.write_vision_ai.R;
import com.example.write_vision_ai.auth.LoginActivity;
import com.example.write_vision_ai.data.ApiService;
import com.example.write_vision_ai.data.ImageResponse;
import com.example.write_vision_ai.data.adapters.ImageAdapter;
import com.example.write_vision_ai.utils.PdfGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements ImageAdapter.OnAddTextClickListener {

    private Button btnGenerate, btnLogout, btnExportPdf;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private ApiService apiService;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<String> imageUrls = new ArrayList<>();
    private final List<Spinner> spinners = new ArrayList<>();
    private LinearLayout storyLayout;
    private TextView tvStoryPreview;
    private static final int REQ_CAPTURE_TEXT = 2001;
    private static final int REQ_SELECT_FRAME = 2002;
    private int pendingImageIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnGenerate = findViewById(R.id.btnGenerate);
        btnLogout = findViewById(R.id.btnLogout);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        recyclerView = findViewById(R.id.recyclerView);
        storyLayout = findViewById(R.id.storyLayout);
        tvStoryPreview = findViewById(R.id.tvStoryPreview);

        btnLogout.setOnClickListener(view -> {
            mAuth.signOut();
            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(imageUrls, this);
        recyclerView.setAdapter(imageAdapter);

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

        String[] storyParts = {
                "______ salió a pasear por el parque.",
                "Encontró ______ en el suelo.",
                "Decidió seguir ______.",
                "Llegó a ______.",
                "Allí conoció a ______.",
                "Juntos inventaron ______.",
                "Pero apareció ______.",
                "Al final, todos celebraron con ______."
        };

        String[][] options = {
                {"un niño", "una niña", "un perro", "un gato"},
                {"una flor mágica", "una piedra brillante", "una hoja gigante"},
                {"un camino dorado", "una huella misteriosa", "un mapa antiguo"},
                {"un árbol parlante", "una cueva secreta", "un castillo en miniatura"},
                {"una ardilla sabia", "un búho cantante", "un ratón inventor"},
                {"una bicicleta voladora", "un paraguas que habla", "un robot de hojas"},
                {"una nube gigante", "un monstruo de chocolate", "una sombra traviesa"},
                {"una fiesta de globos", "una danza mágica", "un picnic de estrellas"}
        };

        buildStoryForm(storyParts, options);

        btnGenerate.setOnClickListener(view -> generateImages());

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

    private void buildStoryForm(String[] storyParts, String[][] options) {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < storyParts.length; i++) {
            LinearLayout itemContainer = new LinearLayout(this);
            itemContainer.setOrientation(LinearLayout.VERTICAL);
            itemContainer.setPadding(16, 16, 16, 16);

            TextView tvStoryPart = new TextView(this);
            tvStoryPart.setText(storyParts[i]);
            tvStoryPart.setTextSize(18);
            itemContainer.addView(tvStoryPart);

            Spinner spinner = new Spinner(this);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, options[i]);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            final int position = i;
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    updateStoryPreview(position, parent.getItemAtPosition(pos).toString());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            itemContainer.addView(spinner);
            spinners.add(spinner);
            storyLayout.addView(itemContainer);
        }
    }

    private void updateStoryPreview(int position, String selection) {
        String[] basePrompts = {
                "%s salió a pasear por el parque.",
                "Encontró %s en el suelo.",
                "Decidió seguir %s.",
                "Llegó a %s.",
                "Allí conoció a %s.",
                "Juntos inventaron %s.",
                "Pero apareció %s.",
                "Al final, todos celebraron con %s."
        };

        StringBuilder storyBuilder = new StringBuilder();
        for (int i = 0; i < spinners.size(); i++) {
            String currentSelection = (i == position) ? selection : spinners.get(i).getSelectedItem().toString();
            storyBuilder.append(String.format(basePrompts[i], currentSelection)).append("\n\n");
        }

        tvStoryPreview.setText(storyBuilder.toString());
    }

    private void generateImages() {
        imageUrls.clear();
        imageAdapter.notifyDataSetChanged();

        String[] basePrompts = {
                "%s salió a pasear por el parque.",
                "Encontró %s en el suelo.",
                "Decidió seguir %s.",
                "Llegó a %s.",
                "Allí conoció a %s.",
                "Juntos inventaron %s.",
                "Pero apareció %s.",
                "Al final, todos celebraron con %s."
        };

        for (int i = 0; i < spinners.size(); i++) {
            String selection = spinners.get(i).getSelectedItem().toString();
            String prompt = String.format(basePrompts[i], selection);
            generateImage(prompt);
        }
    }

    private void generateImage(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "dall-e-3");
        requestBody.put("prompt", prompt);
        requestBody.put("n", 1);
        requestBody.put("size", "1024x1024");

        apiService.generateImage(requestBody).enqueue(new Callback<ImageResponse>() {
            @Override
            public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String imageUrl = response.body().getData().get(0).getUrl();
                    imageUrls.add(imageUrl);
                    imageAdapter.notifyItemInserted(imageUrls.size() - 1);
                    saveImageToFirestore(imageUrl, prompt);
                } else {
                    Toast.makeText(MainActivity.this, "Error en la respuesta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ImageResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error en la conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveImageToFirestore(String imageUrl, String prompt) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Guardando imagen...");
        progress.setCancelable(false);
        progress.show();

        Map<String, Object> imageData = new HashMap<>();
        imageData.put("url", imageUrl);
        imageData.put("prompt", prompt);
        imageData.put("userId", currentUser.getUid());
        imageData.put("userEmail", currentUser.getEmail());
        imageData.put("createdAt", new Date());
        imageData.put("isPublic", false);
        imageData.put("views", 0);
        imageData.put("likes", 0);
        imageData.put("tags", extractTagsFromPrompt(prompt));

        db.collection("generated_images")
                .add(imageData)
                .addOnCompleteListener(task -> {
                    progress.dismiss();
                    if (task.isSuccessful()) {
                        updateUserImageCount(currentUser.getUid());
                        Toast.makeText(MainActivity.this, "Imagen guardada", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Error al guardar imagen", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private List<String> extractTagsFromPrompt(String prompt) {
        List<String> tags = new ArrayList<>();
        for (String word : prompt.split(" ")) {
            if (word.startsWith("#") && word.length() > 1) {
                tags.add(word.substring(1));
            }
        }
        return tags;
    }

    private void updateUserImageCount(String userId) {
        db.collection("users").document(userId)
                .update("generatedImagesCount", FieldValue.increment(1))
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error al actualizar contador", e));
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
}
