package com.example.write_vision_ai;

import java.util.List;

public class PromptRequest {
    private List<String> prompts;

    public PromptRequest(List<String> prompts) {
        this.prompts = prompts;
    }

    public List<String> getPrompts() {
        return prompts;
    }
}

