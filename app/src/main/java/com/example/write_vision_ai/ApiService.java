package com.example.write_vision_ai;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {
    @Headers({
            "Content-Type: application/json",
            "Authorization: Bearer {API_KEY}"
    })
    @POST("v1/images/generations") // Endpoint de OpenAI
    Call<ImageResponse> generateImage(@Body Map<String, Object> requestBody);
}