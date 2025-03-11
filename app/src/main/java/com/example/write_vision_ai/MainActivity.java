package com.example.write_vision_ai;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.example.write_vision_ai.databinding.ActivityMainBinding;

import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
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
    private ApiService apiService;
    private ImageView imageView;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        btnGenerate = findViewById(R.id.btnGenerate);
        imageView = findViewById(R.id.imageView);

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
                .baseUrl("https://dashscope-intl.aliyuncs.com/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();


        apiService = retrofit.create(ApiService.class);

        btnGenerate.setOnClickListener(view -> generateImage());

    }

    public native String stringFromJNI();

    private void generateImage() {
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", "Genera una imagen de un robot ayudando a un niño a escribir en un cuaderno");
        input.put("seed", 42);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("input", input);

        Log.d("API_REQUEST", "Request Body: " + requestBody.toString());

        apiService.generateImage(requestBody).enqueue(new Callback<QwenResponse>() {
            @Override
            public void onResponse(Call<QwenResponse> call, Response<QwenResponse> response) {
                // Log para verificar el estado de la respuesta
                Log.d("API_RESPONSE", "Response Code: " + response.code());
                Log.d("API_RESPONSE", "Response Body: " + (response.body() != null ? response.body().toString() : "null"));

                if (response.isSuccessful() && response.body() != null) {
                    String outputText = (String) response.body().getOutput().get("text");
                    Log.d("API_RESPONSE", "Output Text: " + outputText);

                    String imageUrl = extractImageUrl(outputText);
                    Log.d("API_RESPONSE", "Extracted Image URL: " + imageUrl);

                    if (imageUrl != null) {

                        Glide.with(MainActivity.this)
                                .load(imageUrl)
                                .into(imageView);
                        Log.d("IMAGE_LOAD", "Image URL loaded successfully");
                    } else {
                        Log.e("IMAGE_LOAD", "No image URL found");
                        Toast.makeText(MainActivity.this, "No se encontró imagen en la respuesta", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("API_RESPONSE", "Error en la respuesta, Código: " + response.code());
                    Toast.makeText(MainActivity.this, "Error en la respuesta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<QwenResponse> call, Throwable t) {
                Log.e("API_ERROR", "Error en la conexión: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Error en la conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extractImageUrl(String text) {
        if (text != null && text.contains("![](") && text.contains(")")) {

            int startIndex = text.indexOf("![](") + 4;
            int endIndex = text.indexOf(")", startIndex);
            if (startIndex != -1 && endIndex != -1) {
                return text.substring(startIndex, endIndex);
            }
        }
        return null;
    }


}