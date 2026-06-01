package com.placute.ocrbackend.integration;

import com.placute.ocrbackend.dto.VehicleAttributesDto;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class OpenAIVehicleAttributeService {

    private static final String OPENAI_CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.vehicle-attributes.enabled:true}")
    private boolean enabled;

    @Value("${openai.vehicle-attributes.model:gpt-4o-mini}")
    private String model;

    public Optional<VehicleAttributesDto> analyze(File imageFile) throws IOException {
        if (!isConfigured()) {
            return Optional.empty();
        }

        String imageDataUrl = buildImageDataUrl(imageFile);
        return analyzeDataUrl(imageDataUrl);
    }

    public Optional<VehicleAttributesDto> analyzeBase64Image(String base64Image, String contentType) throws IOException {
        if (!isConfigured() || base64Image == null || base64Image.isBlank()) {
            return Optional.empty();
        }

        String safeContentType = contentType != null && contentType.startsWith("image/")
                ? contentType
                : "image/jpeg";
        return analyzeDataUrl("data:" + safeContentType + ";base64," + base64Image);
    }

    private Optional<VehicleAttributesDto> analyzeDataUrl(String imageDataUrl) throws IOException {
        JSONObject requestBody = new JSONObject()
                .put("model", model)
                .put("temperature", 0.1)
                .put("max_tokens", 300)
                .put("response_format", new JSONObject().put("type", "json_object"))
                .put("messages", new JSONArray()
                        .put(new JSONObject()
                                .put("role", "user")
                                .put("content", new JSONArray()
                                        .put(new JSONObject()
                                                .put("type", "text")
                                                .put("text", buildPrompt()))
                                        .put(new JSONObject()
                                                .put("type", "image_url")
                                                .put("image_url", new JSONObject()
                                                        .put("url", imageDataUrl)
                                                        .put("detail", "low"))))));

        Request request = new Request.Builder()
                .url(OPENAI_CHAT_COMPLETIONS_URL)
                .post(RequestBody.create(requestBody.toString(), JSON))
                .addHeader("Authorization", "Bearer " + openaiApiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI vehicle attributes error: " + response.code() + " " + responseBody);
            }
            return parseResponse(responseBody);
        }
    }

    private Optional<VehicleAttributesDto> parseResponse(String responseBody) {
        JSONObject root = new JSONObject(responseBody);
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return Optional.empty();
        }

        String content = choices.getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "")
                .trim();
        if (content.isBlank()) {
            return Optional.empty();
        }

        JSONObject attributes = parseJsonObject(content);
        if (attributes == null) {
            return Optional.empty();
        }
        VehicleAttributesDto dto = new VehicleAttributesDto(
                clean(attributes.opt("make")),
                clean(attributes.opt("model")),
                clean(attributes.opt("color")),
                clean(attributes.opt("bodyType")),
                clampConfidence(attributes.optDouble("confidence", 0.0)),
                truncate(clean(attributes.opt("reasoning")), 500)
        );

        if (dto.getMake() == null && dto.getModel() == null && dto.getColor() == null && dto.getBodyType() == null) {
            return Optional.empty();
        }
        return Optional.of(dto);
    }

    private String buildImageDataUrl(File imageFile) throws IOException {
        String contentType = Files.probeContentType(imageFile.toPath());
        if (contentType == null || !contentType.startsWith("image/")) {
            contentType = "image/jpeg";
        }
        String base64Image = Base64.getEncoder().encodeToString(Files.readAllBytes(imageFile.toPath()));
        return "data:" + contentType + ";base64," + base64Image;
    }

    private String buildPrompt() {
        return """
                Analyze the visible vehicle in this image and estimate only attributes that can be seen.
                Return JSON only, with exactly these keys:
                {
                  "make": string or null,
                  "model": string or null,
                  "color": string or null,
                  "bodyType": string or null,
                  "confidence": number between 0 and 1,
                  "reasoning": short Romanian explanation
                }
                Do not read or infer owner, insurance, personal data, or legal status.
                If the make or model is uncertain, use null or a low confidence score.
                """;
    }

    private boolean isConfigured() {
        return enabled
                && openaiApiKey != null
                && !openaiApiKey.isBlank()
                && openaiApiKey.toLowerCase(Locale.ROOT).startsWith("sk-");
    }

    private String clean(Object value) {
        if (value == null || JSONObject.NULL.equals(value)) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank() || "null".equalsIgnoreCase(text) || "unknown".equalsIgnoreCase(text)) {
            return null;
        }
        return text;
    }

    private Double clampConfidence(double confidence) {
        if (Double.isNaN(confidence)) {
            return null;
        }
        return Math.max(0.0, Math.min(1.0, confidence));
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private JSONObject parseJsonObject(String content) {
        try {
            return new JSONObject(content);
        } catch (RuntimeException ignored) {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return null;
            }
            try {
                return new JSONObject(content.substring(start, end + 1));
            } catch (RuntimeException ignoredAgain) {
                return null;
            }
        }
    }
}
