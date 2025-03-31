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

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
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
}


