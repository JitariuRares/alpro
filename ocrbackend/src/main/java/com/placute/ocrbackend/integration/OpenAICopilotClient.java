package com.placute.ocrbackend.integration;

import com.placute.ocrbackend.model.UserRole;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class OpenAICopilotClient {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.copilot.model:gpt-4o-mini}")
    private String copilotModel;

    @Value("${openai.copilot.temperature:0.1}")
    private double copilotTemperature;

    @Value("${openai.copilot.enabled:true}")
    private boolean copilotEnabled;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public Optional<CopilotPlan> planIntent(String userMessage, UserRole userRole, Set<String> allowedIntents) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        if (userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", copilotModel);
        requestBody.put("temperature", copilotTemperature);
        requestBody.put("response_format", new JSONObject().put("type", "json_object"));
        requestBody.put("messages", buildMessages(userMessage, userRole, allowedIntents));

        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer " + openaiApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return Optional.empty();
            }

            JSONObject apiResponse = new JSONObject(response.body().string());
            JSONArray choices = apiResponse.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return Optional.empty();
            }

            JSONObject firstChoice = choices.optJSONObject(0);
            if (firstChoice == null) {
                return Optional.empty();
            }

            JSONObject message = firstChoice.optJSONObject("message");
            if (message == null) {
                return Optional.empty();
            }

            String content = message.optString("content", "");
            if (content.isBlank()) {
                return Optional.empty();
            }

            JSONObject parsed = parseJsonObject(content);
            if (parsed == null) {
                return Optional.empty();
            }

            JSONObject filters = parsed.optJSONObject("filters");
            String actor = filters != null ? normalizeNullable(filters.optString("actor", null)) : null;

            return Optional.of(new CopilotPlan(
                    normalizeNullable(parsed.optString("intent", null)),
                    normalizeNullable(parsed.optString("plateNumber", null)),
                    normalizeNullable(parsed.optString("reviewStatus", null)),
                    normalizeNullable(parsed.optString("targetModule", null)),
                    actor,
                    normalizeNullable(parsed.optString("reason", null))
            ));
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private JSONArray buildMessages(String userMessage, UserRole userRole, Set<String> allowedIntents) {
        String systemPrompt = """
                You are an intent planner for an operational ALPR application.
                Return ONLY JSON with this shape:
                {
                  "intent": "POLICE_LOOKUP|PARKING_LOOKUP|INSURANCE_LOOKUP|REVIEW_QUEUE|DASHBOARD_STATS|AUDIT_RECENT|NAVIGATE|HELP",
                  "plateNumber": "optional uppercase plate without spaces, ex B123ABC",
                  "reviewStatus": "optional DE_REVIEW|CONFIRMED|REJECTED",
                  "targetModule": "optional dashboard|detectii|vehicule|parcare|asigurari|audit",
                  "filters": {"actor": "optional audit actor"},
                  "reason": "short reason in Romanian"
                }
                Rules:
                - Select only one intent from the allowed list.
                - If the user asks about a vehicle, plate, masina, or numar, prefer POLICE_LOOKUP.
                - If the user asks about parking history, use PARKING_LOOKUP.
                - If the user asks about insurance, use INSURANCE_LOOKUP.
                - If user asks for detections to review, use REVIEW_QUEUE.
                - If user asks stats, totals, or overview, use DASHBOARD_STATS.
                - If user asks audit logs, use AUDIT_RECENT.
                - If user asks to go/open a page, use NAVIGATE.
                - If no clear action, use HELP.
                - Keep reason under 18 words.
                """;

        String userPrompt = """
                User role: %s
                Allowed intents: %s
                User message: %s
                """.formatted(
                userRole != null ? userRole.name() : "UNKNOWN",
                String.join(", ", allowedIntents),
                userMessage
        );

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", systemPrompt));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userPrompt));
        return messages;
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
            String candidate = content.substring(start, end + 1);
            try {
                return new JSONObject(candidate);
            } catch (RuntimeException ignoredAgain) {
                return null;
            }
        }
    }

    private boolean isConfigured() {
        return copilotEnabled
                && openaiApiKey != null
                && !openaiApiKey.isBlank()
                && openaiApiKey.toLowerCase(Locale.ROOT).startsWith("sk-");
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    public record CopilotPlan(
            String intent,
            String plateNumber,
            String reviewStatus,
            String targetModule,
            String actor,
            String reason
    ) {
    }
}
