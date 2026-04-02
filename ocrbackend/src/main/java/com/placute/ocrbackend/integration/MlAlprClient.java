package com.placute.ocrbackend.integration;

import com.placute.ocrbackend.integration.dto.MlAlprResult;
import okhttp3.MultipartBody;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class MlAlprClient {

    @Value("${alpr.ml.base-url}")
    private String baseUrl;

    private final OkHttpClient httpClient = new OkHttpClient();

    public MlAlprResult detectPlate(File imageFile) throws IOException {
        RequestBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "image",
                        imageFile.getName(),
                        RequestBody.create(
                                imageFile,
                                okhttp3.MediaType.parse("application/octet-stream")
                        )
                )
                .build();

        Request request = new Request.Builder()
                .url(inferEndpoint())
                .post(multipartBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("ALPR ML service error: " + response.code() + " " + response.message());
            }

            if (response.body() == null) {
                return null;
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);

            String plateText = json.optString("plateText", null);
            Double confidence = readOptionalDouble(json, "confidence");
            MlAlprResult.Bbox bbox = readBbox(json.optJSONObject("bbox"));
            List<MlAlprResult.Candidate> candidates = readCandidates(json.optJSONArray("candidates"));

            return new MlAlprResult(plateText, confidence, bbox, candidates);
        }
    }

    private String inferEndpoint() {
        if (baseUrl.endsWith("/")) {
            return baseUrl + "infer/car-image";
        }
        return baseUrl + "/infer/car-image";
    }

    private MlAlprResult.Bbox readBbox(JSONObject bboxJson) {
        if (bboxJson == null) {
            return null;
        }
        return new MlAlprResult.Bbox(
                readOptionalInt(bboxJson, "x"),
                readOptionalInt(bboxJson, "y"),
                readOptionalInt(bboxJson, "w"),
                readOptionalInt(bboxJson, "h")
        );
    }

    private List<MlAlprResult.Candidate> readCandidates(JSONArray candidatesJson) {
        List<MlAlprResult.Candidate> candidates = new ArrayList<>();
        if (candidatesJson == null) {
            return candidates;
        }

        for (int i = 0; i < candidatesJson.length(); i++) {
            JSONObject item = candidatesJson.optJSONObject(i);
            if (item == null) {
                continue;
            }
            candidates.add(
                    new MlAlprResult.Candidate(
                            item.optString("text", null),
                            readOptionalDouble(item, "confidence")
                    )
            );
        }

        return candidates;
    }

    private Double readOptionalDouble(JSONObject source, String key) {
        if (!source.has(key) || source.isNull(key)) {
            return null;
        }
        return source.getDouble(key);
    }

    private Integer readOptionalInt(JSONObject source, String key) {
        if (!source.has(key) || source.isNull(key)) {
            return null;
        }
        return source.getInt(key);
    }
}
