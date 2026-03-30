package com.placute.ocrbackend.integration;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class OpenAIOcrService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    public String detectPlateNumber(File imageFile) throws IOException {
        OkHttpClient client = new OkHttpClient();

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");

        JSONArray content = new JSONArray();

        JSONObject textPart = new JSONObject();
        textPart.put("type", "text");
        textPart.put("text", "Ce numar de inmatriculare apare in aceasta imagine? Da-mi doar textul, fara explicatii.");
        content.put(textPart);

        JSONObject imagePart = new JSONObject();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", new JSONObject().put("url", "data:image/png;base64," +
                java.util.Base64.getEncoder().encodeToString(java.nio.file.Files.readAllBytes(imageFile.toPath()))));
        content.put(imagePart);

        message.put("content", content);
        messages.put(message);

        JSONObject requestBodyJson = new JSONObject();
        requestBodyJson.put("model", "gpt-4o");
        requestBodyJson.put("messages", messages);
        requestBodyJson.put("max_tokens", 20);

        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer " + openaiApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBodyJson.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Eroare la OpenAI: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray choices = jsonResponse.getJSONArray("choices");
            String plate = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
            return plate;
        }
    }
}