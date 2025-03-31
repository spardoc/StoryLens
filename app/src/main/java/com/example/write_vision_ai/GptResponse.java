package com.example.write_vision_ai;

import java.util.List;

public class GptResponse {
    private List<Choice> choices;

    public List<Choice> getChoices() {
        return choices;
    }

    public static class Choice {
        private Message message;

        public Message getMessage() {
            return message;
        }
    }

    public static class Message {
        private String role;
        private String content;

        public String getContent() {
            return content;
        }
    }
}
