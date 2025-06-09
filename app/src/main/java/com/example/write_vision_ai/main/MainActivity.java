package com.example.write_vision_ai.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;

import com.example.write_vision_ai.data.ApiService;
import com.example.write_vision_ai.data.adapters.ImageAdapter;
import com.example.write_vision_ai.data.ImageResponse;
import com.example.write_vision_ai.R;
import com.example.write_vision_ai.drawing.SelectFrameActivity;
import com.example.write_vision_ai.databinding.ActivityMainBinding;
import com.example.write_vision_ai.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;



import android.view.View;

import android.widget.AdapterView;


public class MainActivity extends AppCompatActivity implements ImageAdapter.OnAddTextClickListener {

    private Button btnGenerate, btnLogout;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private ApiService apiService;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private final List<String> imageUrls = new ArrayList<>();
    private final List<Spinner> spinners = new ArrayList<>();
    private LinearLayout storyLayout;
    private TextView tvStoryPreview;
    private Button btnPreviewStory;

    private static final int REQ_CAPTURE_TEXT = 2001;
    private static final int REQ_SELECT_FRAME = 2002;
    private int pendingImageIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Permisossssss
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnGenerate = findViewById(R.id.btnGenerate);
        btnLogout = findViewById(R.id.btnLogout);
        recyclerView = findViewById(R.id.recyclerView);
        storyLayout = findViewById(R.id.storyLayout);

        // Logout
        btnLogout.setOnClickListener(view -> {
            mAuth.signOut();
            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(imageUrls, this);
        recyclerView.setAdapter(imageAdapter);

        // Configurar cliente HTTP
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

        // Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        apiService = retrofit.create(ApiService.class);

        // Opciones por viñeta
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

        // TextView para mostrar la historia
        tvStoryPreview = new TextView(this);
        tvStoryPreview.setTextSize(16);
        tvStoryPreview.setTextColor(Color.parseColor("#333333"));
        tvStoryPreview.setPadding(0, 24, 0, 8);
        tvStoryPreview.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        storyLayout.addView(tvStoryPreview);

        // Botón para previsualizar la historia
        btnPreviewStory = new Button(this);
        btnPreviewStory.setText("Previsualizar historia");
        btnPreviewStory.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        btnPreviewStory.setOnClickListener(v -> previewStory());
        storyLayout.addView(btnPreviewStory);

        // Crear spinners y etiquetas
        for (int i = 0; i < options.length; i++) {
            // Etiqueta de la viñeta
            TextView label = new TextView(this);
            label.setText("Viñeta " + (i + 1));
            label.setTextSize(18);
            label.setTextColor(Color.parseColor("#333333"));
            label.setPadding(0, 24, 0, 8);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            storyLayout.addView(label);

            // Spinner con las opciones
            Spinner spinner = new Spinner(this);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options[i]);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            // Listener para actualizar la historia automáticamente
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    previewStory();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            storyLayout.addView(spinner);
            spinners.add(spinner);
        }

        btnGenerate.setOnClickListener(view -> generateImages());
    }

    private void previewStory() {
        StringBuilder storyBuilder = new StringBuilder();
        String[] basePrompts = {
                "%s salió a pasear por el parque.\n\n",
                "Encontró %s en el suelo.\n\n",
                "Decidió seguir %s.\n\n",
                "Llegó a %s.\n\n",
                "Allí conoció a %s.\n\n",
                "Juntos inventaron %s.\n\n",
                "Pero apareció %s.\n\n",
                "Al final, todos celebraron con %s.\n\n"
        };

        for (int i = 0; i < spinners.size(); i++) {
            String selection = spinners.get(i).getSelectedItem().toString();
            storyBuilder.append(String.format(basePrompts[i], selection));
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
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

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
                        Toast.makeText(MainActivity.this, "Imagen guardada", Toast.LENGTH_SHORT).show();
                        updateUserImageCount(currentUser.getUid());
                    } else {
                        Toast.makeText(MainActivity.this, "Error al guardar: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private List<String> extractTagsFromPrompt(String prompt) {
        List<String> tags = new ArrayList<>();
        String[] words = prompt.split(" ");
        for (String word : words) {
            if (word.startsWith("#") && word.length() > 1) {
                tags.add(word.substring(1));
            }
        }
        return tags;
    }

    private void updateUserImageCount(String userId) {
        db.collection("users").document(userId)
                .update("generatedImagesCount", FieldValue.increment(1))
                .addOnFailureListener(e ->
                        Log.e("FIRESTORE", "Error al actualizar contador", e));
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