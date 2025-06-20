package com.example.write_vision_ai.utils;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.write_vision_ai.data.ApiService;
import com.example.write_vision_ai.data.ImageResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageGenerator {

    private final Context context;
    private final String[] currentSelections;  // Cambiado de List<Spinner> a String[]
    private final String[] basePrompts;
    private final List<String> imageUrls;
    private final RecyclerView.Adapter<?> imageAdapter;
    private final ApiService apiService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ImageGenerator(Context context,
                          String[] currentSelections,  // Cambiado el parámetro
                          String[] basePrompts,
                          List<String> imageUrls,
                          RecyclerView.Adapter<?> imageAdapter,
                          ApiService apiService) {
        this.context = context;
        this.currentSelections = currentSelections;
        this.basePrompts = basePrompts;
        this.imageUrls = imageUrls;
        this.imageAdapter = imageAdapter;
        this.apiService = apiService;
    }

    public void generateImages() {
        Log.d("DEBUG", "currentSelections length: " + currentSelections.length);
        imageUrls.clear();
        mainHandler.post(() -> imageAdapter.notifyDataSetChanged());

        // Verifica que tengamos suficientes selecciones
        if (currentSelections.length != basePrompts.length) {
            Log.e("ERROR", "El número de selecciones no coincide con los prompts");
            return;
        }

        for (int i = 0; i < currentSelections.length; i++) {
            String selection = currentSelections[i];
            if (selection == null) {
                selection = StoryConstants.options[i][0]; // Valor por defecto
            }

            String prompt = String.format(basePrompts[i], selection);
            Log.d("PROMPT_DEBUG", "Prompt " + i + ": " + prompt); // Para depuración
            generateImage(prompt);
        }
    }

    private void generateImage(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "dall-e-3");
        requestBody.put("prompt", prompt);
        requestBody.put("n", 1);
        requestBody.put("size", "1024x1024");
        requestBody.put("response_format", "url");

        apiService.generateImage(requestBody).enqueue(new Callback<ImageResponse>() {
            @Override
            public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String imageUrl = response.body().getData().get(0).getUrl();
                    Log.d("IMAGE_ADDED", "URL: " + imageUrl);
                    imageUrls.add(imageUrl);

                    mainHandler.post(() -> imageAdapter.notifyDataSetChanged());

                } else {
                    Log.e("IMAGE_ERROR", "Código: " + response.code());
                    Log.e("IMAGE_ERROR", "Mensaje: " + response.message());

                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("IMAGE_ERROR", "Cuerpo de error: " + errorBody);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mainHandler.post(() ->
                            Toast.makeText(context, "Error en la respuesta", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(Call<ImageResponse> call, Throwable t) {
                mainHandler.post(() ->
                        Toast.makeText(context, "Error en la conexión", Toast.LENGTH_SHORT).show());
            }
        });
    }
}