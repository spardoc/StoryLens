package com.example.write_vision_ai;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;


public interface ApiService {
    @Headers({
            "Authorization: Bearer {API_KEY}",
            "Content-Type: application/json"
    })
    @POST("apps/{APP_ID}/completion")
    Call<QwenResponse> generateImage(@Body Map<String, Object> body);

}