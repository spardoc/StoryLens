package com.example.write_vision_ai.data;

import java.util.List;

public class ImageResponse {
    private List<ImageData> data;

    public List<ImageData> getData() {
        return data;
    }

    public static class ImageData {
        private String url;

        public String getUrl() {
            return url;
        }
    }
}
