package com.example.write_vision_ai;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.write_vision_ai.databinding.ActivityMainBinding;

import java.io.IOException;
import java.util.ArrayList;
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

public class MainActivity extends AppCompatActivity {

    private Button btnGenerate;
    private EditText etPrompt;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private ApiService apiService;
    private ActivityMainBinding binding;

    private final List<String> imageUrls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        btnGenerate = findViewById(R.id.btnGenerate);
        etPrompt = findViewById(R.id.etPrompt);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(imageUrls);
        recyclerView.setAdapter(imageAdapter);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        Log.d("API_REQUEST", "Request URL: " + request.url());
                        return chain.proceed(request);
                    }
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://dashscope-intl.aliyuncs.com/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        apiService = retrofit.create(ApiService.class);

        btnGenerate.setOnClickListener(view -> generateImages());
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
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", prompt);
        input.put("seed", System.currentTimeMillis());  // Para generar imágenes únicas

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("input", input);

        apiService.generateImage(requestBody).enqueue(new Callback<QwenResponse>() {
            @Override
            public void onResponse(Call<QwenResponse> call, Response<QwenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String outputText = (String) response.body().getOutput().get("text");
                    String imageUrl = extractImageUrl(outputText);

                    if (imageUrl != null) {
                        imageUrls.add(imageUrl);
                        imageAdapter.notifyItemInserted(imageUrls.size() - 1);
                    } else {
                        Toast.makeText(MainActivity.this, "No se encontró imagen para: " + prompt, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Error en la respuesta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<QwenResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error en la conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extractImageUrl(String text) {
        if (text != null && text.contains("![](") && text.contains(")")) {
            int startIndex = text.indexOf("![](") + 4;
            int endIndex = text.indexOf(")", startIndex);
            return text.substring(startIndex, endIndex);
        }
        return null;
    }
}


