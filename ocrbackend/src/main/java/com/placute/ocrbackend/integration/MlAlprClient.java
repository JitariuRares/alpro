package com.placute.ocrbackend.integration;

import com.placute.ocrbackend.integration.dto.MlAlprResult;
import com.placute.ocrbackend.integration.dto.MlAlprVideoResult;
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
import java.util.concurrent.TimeUnit;

@Service
public class MlAlprClient {

    @Value("${alpr.ml.base-url}")
    private String baseUrl;

    @Value("${alpr.ml.video-timeout-seconds:600}")
    private long videoTimeoutSeconds;

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

    public MlAlprVideoResult detectVideo(File videoFile, Integer frameStep, Integer maxFrames) throws IOException {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "video",
                        videoFile.getName(),
                        RequestBody.create(
                                videoFile,
                                okhttp3.MediaType.parse("application/octet-stream")
                        )
                );

        if (frameStep != null) {
            multipartBuilder.addFormDataPart("frame_step", String.valueOf(frameStep));
        }
        if (maxFrames != null) {
            multipartBuilder.addFormDataPart("max_frames", String.valueOf(maxFrames));
        }

        Request request = new Request.Builder()
                .url(videoInferEndpoint())
                .post(multipartBuilder.build())
                .build();

        OkHttpClient videoHttpClient = httpClient.newBuilder()
                .readTimeout(videoTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(videoTimeoutSeconds, TimeUnit.SECONDS)
                .callTimeout(videoTimeoutSeconds, TimeUnit.SECONDS)
                .build();

        try (Response response = videoHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("ALPR ML video service error: " + response.code() + " " + response.message());
            }

            if (response.body() == null) {
                return new MlAlprVideoResult(null, null, null, null, null, null, List.of());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);

            return new MlAlprVideoResult(
                    readOptionalInt(json, "frameStep"),
                    readOptionalInt(json, "maxFrames"),
                    readOptionalDouble(json, "fps"),
                    readOptionalInt(json, "totalFrames"),
                    readOptionalInt(json, "processedFrames"),
                    readOptionalInt(json, "processingMs"),
                    readVideoDetections(json.optJSONArray("detections"))
            );
        }
    }

    private String inferEndpoint() {
        if (baseUrl.endsWith("/")) {
            return baseUrl + "infer/car-image";
        }
        return baseUrl + "/infer/car-image";
    }

    private String videoInferEndpoint() {
        if (baseUrl.endsWith("/")) {
            return baseUrl + "infer/video";
        }
        return baseUrl + "/infer/video";
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

    private List<MlAlprVideoResult.Detection> readVideoDetections(JSONArray detectionsJson) {
        List<MlAlprVideoResult.Detection> detections = new ArrayList<>();
        if (detectionsJson == null) {
            return detections;
        }

        for (int i = 0; i < detectionsJson.length(); i++) {
            JSONObject item = detectionsJson.optJSONObject(i);
            if (item == null) {
                continue;
            }

            detections.add(new MlAlprVideoResult.Detection(
                    readOptionalInt(item, "frameIndex"),
                    readOptionalLong(item, "timestampMs"),
                    readOptionalInt(item, "trackId"),
                    item.optString("plateText", null),
                    readOptionalDouble(item, "confidence"),
                    readVideoBbox(item.optJSONObject("bbox"))
            ));
        }
        return detections;
    }

    private MlAlprVideoResult.Bbox readVideoBbox(JSONObject bboxJson) {
        if (bboxJson == null) {
            return null;
        }
        return new MlAlprVideoResult.Bbox(
                readOptionalInt(bboxJson, "x"),
                readOptionalInt(bboxJson, "y"),
                readOptionalInt(bboxJson, "w"),
                readOptionalInt(bboxJson, "h")
        );
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

    private Long readOptionalLong(JSONObject source, String key) {
        if (!source.has(key) || source.isNull(key)) {
            return null;
        }
        return source.getLong(key);
    }
}
