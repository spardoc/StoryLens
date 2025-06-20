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

    public interface GenerateImagesCallback {
        void onAllImagesGenerated(); // Se llama cuando todas las imágenes están listas
        void onError(String error);  // Se llama si hay un error
    }

    private final Context context;
    private final String[] currentSelections;
    private final String[] basePrompts;
    private final List<String> imageUrls;
    private final RecyclerView.Adapter<?> imageAdapter;
    private final ApiService apiService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private GenerateImagesCallback callback;

    public ImageGenerator(Context context,
                          String[] currentSelections,
                          String[] basePrompts,
                          List<String> imageUrls,
                          RecyclerView.Adapter<?> imageAdapter,
                          ApiService apiService,
                          GenerateImagesCallback callback) { // Añade el callback
        this.context = context;
        this.currentSelections = currentSelections;
        this.basePrompts = basePrompts;
        this.imageUrls = imageUrls;
        this.imageAdapter = imageAdapter;
        this.apiService = apiService;
        this.callback = callback;
    }

    public void generateImages() {
        Log.d("DEBUG", "currentSelections length: " + currentSelections.length);
        imageUrls.clear();
        mainHandler.post(() -> imageAdapter.notifyDataSetChanged());

        if (currentSelections.length != basePrompts.length) {
            String error = "El número de selecciones no coincide con los prompts";
            Log.e("ERROR", error);
            if (callback != null) callback.onError(error);
            return;
        }

        generateImageSequentially(0); // Iniciar secuencia
    }

    private void generateImageSequentially(int index) {
        if (index >= currentSelections.length) {
            mainHandler.post(() -> {
                Toast.makeText(context, "Generación completada ✅", Toast.LENGTH_SHORT).show();
                if (callback != null) callback.onAllImagesGenerated(); // Notificar éxito
            });
            return;
        }

        String selection = currentSelections[index];
        if (selection == null) {
            selection = StoryConstants.options[index][0]; // Valor por defecto
        }

        String prompt = StoryConstants.comicStyleJSONPrompt + "\n" +
                "Please illustrate the following story scene: " +
                String.format(basePrompts[index], selection);

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
                    imageUrls.add(imageUrl);
                    mainHandler.post(() -> imageAdapter.notifyDataSetChanged());
                } else {
                    String error = "Error en la respuesta: " + response.code();
                    Log.e("IMAGE_ERROR", error);
                    if (callback != null) callback.onError(error);
                }
                mainHandler.postDelayed(() -> generateImageSequentially(index + 1), 3000);
            }

            @Override
            public void onFailure(Call<ImageResponse> call, Throwable t) {
                String error = "Error en la conexión: " + t.getMessage();
                if (callback != null) callback.onError(error);
                mainHandler.postDelayed(() -> generateImageSequentially(index + 1), 1200);
            }
        });
    }
}