package com.example.write_vision_ai.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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

public class MainActivity extends AppCompatActivity implements ImageAdapter.OnAddTextClickListener {

    private Button btnGenerate;
    private EditText etPrompt;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private ApiService apiService;
    private ActivityMainBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private final List<String> imageUrls = new ArrayList<>();

    private static final int REQ_CAPTURE_TEXT = 2001;
    private static final int REQ_SELECT_FRAME = 2002;
    private int pendingImageIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Verificar permisos de cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configurar botón de logout
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(MainActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        // Inicializar UI
        btnGenerate = findViewById(R.id.btnGenerate);
        etPrompt = findViewById(R.id.etPrompt);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(imageUrls, this);
        recyclerView.setAdapter(imageAdapter);

        // Configurar cliente HTTP
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        Log.d("API_REQUEST", "Request URL: " + request.url());
                        return chain.proceed(request);
                    }
                })
                .build();

        // Configurar Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        apiService = retrofit.create(ApiService.class);

        btnGenerate.setOnClickListener(view -> generateImages());
    }

    @Override
    public void onAddTextClicked(String imageUrl) {
        pendingImageIndex = imageUrls.indexOf(imageUrl);
        if (pendingImageIndex == -1) return; // seguridad
        String url = imageUrls.get(pendingImageIndex);

        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("base_image_url", url);
        startActivityForResult(intent, REQ_CAPTURE_TEXT);
    }


    private void generateImages() {
        String userInput = etPrompt.getText().toString().trim();

        if (userInput.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa al menos un prompt", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] userPrompts = userInput.split("\n|,");

        imageUrls.clear();
        imageAdapter.notifyDataSetChanged();

        for (String prompt : userPrompts) {
            if (!prompt.trim().isEmpty()) {
                generateImage(prompt.trim());
            }
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
                Log.d("API_RESPONSE", "Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    String imageUrl = response.body().getData().get(0).getUrl();
                    Log.d("API_RESPONSE", "Image URL: " + imageUrl);

                    imageUrls.add(imageUrl);
                    imageAdapter.notifyItemInserted(imageUrls.size() - 1);

                    // Guardar la imagen en Firestore
                    saveImageToFirestore(imageUrl, prompt);
                } else {
                    Log.e("API_RESPONSE", "Error en la respuesta: " + response.errorBody());
                    Toast.makeText(MainActivity.this, "Error en la respuesta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ImageResponse> call, Throwable t) {
                Log.e("API_ERROR", "Error en la conexión: " + t.getMessage());
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

        // Mostrar progreso
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Guardando imagen...");
        progress.setCancelable(false);
        progress.show();

        // Crear datos de la imagen
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

        // Guardar en Firestore
        db.collection("generated_images")
                .add(imageData)
                .addOnCompleteListener(task -> {
                    progress.dismiss();
                    if (task.isSuccessful()) {
                        Log.d("FIRESTORE", "Imagen guardada con ID: " + task.getResult().getId());
                        Toast.makeText(MainActivity.this, "Imagen guardada", Toast.LENGTH_SHORT).show();

                        // Actualizar contador de imágenes del usuario
                        updateUserImageCount(currentUser.getUid());
                    } else {
                        Log.e("FIRESTORE", "Error al guardar", task.getException());
                        Toast.makeText(MainActivity.this, "Error al guardar: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private List<String> extractTagsFromPrompt(String prompt) {
        List<String> tags = new ArrayList<>();
        // Implementación básica para extraer tags (puedes mejorarla)
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("MainActivity", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if ((requestCode == REQ_CAPTURE_TEXT || requestCode == REQ_SELECT_FRAME) && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("final_image_path")) {
                String finalImagePath = data.getStringExtra("final_image_path");
                Log.d("MainActivity", "final_image_path: " + finalImagePath);

                if (finalImagePath != null && pendingImageIndex != -1) {
                    // Actualiza la lista con la nueva imagen
                    imageUrls.set(pendingImageIndex, finalImagePath);
                    // Notifica al adapter para refrescar esa posición
                    imageAdapter.notifyItemChanged(pendingImageIndex);

                    // Reinicia pendingImageIndex
                    pendingImageIndex = -1;
                }
            } else {
                Log.e("MainActivity", "No final_image_path in intent data or data is null");
            }
        }
    }



}
