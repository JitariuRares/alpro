package com.placute.ocrbackend.service;

import com.placute.ocrbackend.dto.VehicleAttributesDto;
import com.placute.ocrbackend.integration.OpenAIVehicleAttributeService;
import com.placute.ocrbackend.integration.MlAlprClient;
import com.placute.ocrbackend.integration.dto.MlAlprVideoResult;
import com.placute.ocrbackend.model.PlateType;
import com.placute.ocrbackend.model.VideoDetection;
import com.placute.ocrbackend.model.VideoJob;
import com.placute.ocrbackend.model.VideoJobStatus;
import com.placute.ocrbackend.repository.VideoDetectionRepository;
import com.placute.ocrbackend.repository.VideoJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoJobProcessorService {

    private static final Logger log = LoggerFactory.getLogger(VideoJobProcessorService.class);

    @Autowired
    private VideoJobRepository videoJobRepository;

    @Autowired
    private VideoDetectionRepository videoDetectionRepository;

    @Autowired
    private MlAlprClient mlAlprClient;

    @Autowired
    private OpenAIVehicleAttributeService vehicleAttributeService;

    @Autowired
    private RomanianPlateValidator plateValidator;

    @Value("${alpr.video.min-confidence:0.70}")
    private double minConfidence;

    @Value("${alpr.video.min-frame-gap-per-track:12}")
    private int minFrameGapPerTrack;

    @Value("${openai.vehicle-attributes.video.max-calls-per-job:5}")
    private int maxVehicleAttributeCallsPerJob;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Async
    public void processJobAsync(Long jobId, Integer frameStep, Integer maxFrames) {
        VideoJob job = videoJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        try {
            job.setStatus(VideoJobStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            job.setProgressPercent(5);
            job.setErrorMessage(null);
            videoJobRepository.save(job);

            File videoFile = new File(job.getStoragePath());
            if (!videoFile.exists() || !videoFile.isFile()) {
                throw new IllegalStateException("Fisierul video nu mai exista pe disc.");
            }

            MlAlprVideoResult mlResult = mlAlprClient.detectVideo(videoFile, frameStep, maxFrames);

            videoDetectionRepository.deleteByJob_Id(jobId);
            List<VideoDetection> detections = mapDetections(mlResult, job);
            enrichVideoDetections(detections);
            if (!detections.isEmpty()) {
                videoDetectionRepository.saveAll(detections);
            }

            job.setStatus(VideoJobStatus.COMPLETED);
            job.setProgressPercent(100);
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(null);
            videoJobRepository.save(job);
        } catch (Exception ex) {
            VideoJob failedJob = videoJobRepository.findById(jobId).orElse(null);
            if (failedJob == null) {
                return;
            }

            failedJob.setStatus(VideoJobStatus.FAILED);
            failedJob.setProgressPercent(100);
            failedJob.setCompletedAt(LocalDateTime.now());
            failedJob.setErrorMessage(limitError(ex.getMessage()));
            videoJobRepository.save(failedJob);
        }
    }

    private List<VideoDetection> mapDetections(MlAlprVideoResult mlResult, VideoJob job) {
        List<VideoDetection> mapped = new ArrayList<>();
        if (mlResult == null || mlResult.getDetections() == null) {
            return mapped;
        }

        Map<Integer, VideoDetection> lastDetectionByTrack = new HashMap<>();

        for (MlAlprVideoResult.Detection mlDetection : mlResult.getDetections()) {
            if (mlDetection == null || mlDetection.getPlateText() == null || mlDetection.getPlateText().isBlank()) {
                continue;
            }
            RomanianPlateValidator.ValidationResult plate = plateValidator.validate(mlDetection.getPlateText());
            if (plate.plateType() == PlateType.UNKNOWN) {
                continue;
            }
            if (mlDetection.getConfidence() != null && mlDetection.getConfidence() < minConfidence) {
                continue;
            }

            VideoDetection detection = new VideoDetection();
            detection.setJob(job);
            detection.setFrameIndex(mlDetection.getFrameIndex() != null ? mlDetection.getFrameIndex() : 0);
            detection.setTimestampMs(mlDetection.getTimestampMs());
            detection.setTrackId(mlDetection.getTrackId());
            detection.setPlateText(plate.normalizedPlate());
            detection.setPlateType(plate.plateType());
            detection.setConfidence(mlDetection.getConfidence());
            if (mlDetection.getBbox() != null) {
                detection.setBboxX(mlDetection.getBbox().getX());
                detection.setBboxY(mlDetection.getBbox().getY());
                detection.setBboxW(mlDetection.getBbox().getW());
                detection.setBboxH(mlDetection.getBbox().getH());
            }
            detection.setAiSourceImageBase64(mlDetection.getVehicleImageBase64());
            detection.setAiSourceImageMimeType(mlDetection.getVehicleImageMimeType());
            detection.setVehicleImagePath(storeVehicleFrameImage(
                    mlDetection.getVehicleImageBase64(),
                    mlDetection.getVehicleImageMimeType(),
                    job.getId(),
                    plate.normalizedPlate(),
                    detection.getFrameIndex()
            ));

            Integer trackId = detection.getTrackId();
            if (trackId != null) {
                VideoDetection previous = lastDetectionByTrack.get(trackId);
                if (previous != null && shouldSkipAsDuplicate(previous, detection)) {
                    continue;
                }
                lastDetectionByTrack.put(trackId, detection);
            }

            mapped.add(detection);
        }

        return mapped;
    }

    private String storeVehicleFrameImage(
            String imageBase64,
            String mimeType,
            Long jobId,
            String plateText,
            Integer frameIndex
    ) {
        if (imageBase64 == null || imageBase64.isBlank()) {
            return null;
        }

        try {
            String cleanBase64 = imageBase64;
            int commaIndex = cleanBase64.indexOf(',');
            if (commaIndex >= 0 && commaIndex < cleanBase64.length() - 1) {
                cleanBase64 = cleanBase64.substring(commaIndex + 1);
            }

            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);
            String extension = imageExtension(mimeType);
            Path uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
            Path frameRoot = uploadRoot.resolve("video-frames").normalize();
            Files.createDirectories(frameRoot);

            String safePlate = plateText != null ? plateText.replaceAll("[^A-Z0-9]", "") : "UNKNOWN";
            String fileName = "video-frame-job-" + jobId
                    + "-" + safePlate
                    + "-f" + (frameIndex != null ? frameIndex : 0)
                    + "-" + UUID.randomUUID()
                    + extension;
            Path target = frameRoot.resolve(fileName).normalize();
            if (!target.startsWith(frameRoot)) {
                return null;
            }

            Files.write(target, imageBytes);
            return target.toString();
        } catch (Exception ex) {
            log.warn("Could not store video frame image for {}: {}", plateText, ex.getMessage());
            return null;
        }
    }

    private String imageExtension(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return ".jpg";
        }

        String normalized = mimeType.toLowerCase(Locale.ROOT);
        if (normalized.contains("png")) {
            return ".png";
        }
        if (normalized.contains("webp")) {
            return ".webp";
        }
        return ".jpg";
    }

    private void enrichVideoDetections(List<VideoDetection> detections) {
        if (detections == null || detections.isEmpty() || maxVehicleAttributeCallsPerJob <= 0) {
            return;
        }

        Map<String, VideoDetection> representatives = new LinkedHashMap<>();
        for (VideoDetection detection : detections) {
            if (detection.getAiSourceImageBase64() == null || detection.getAiSourceImageBase64().isBlank()) {
                continue;
            }

            String key = aiGroupKey(detection);
            VideoDetection current = representatives.get(key);
            if (current == null || confidenceOf(detection) > confidenceOf(current)) {
                representatives.put(key, detection);
            }
        }

        int calls = 0;
        for (Map.Entry<String, VideoDetection> entry : representatives.entrySet()) {
            if (calls >= maxVehicleAttributeCallsPerJob) {
                break;
            }

            VideoDetection representative = entry.getValue();
            try {
                Optional<VehicleAttributesDto> attributes = vehicleAttributeService.analyzeBase64Image(
                        representative.getAiSourceImageBase64(),
                        representative.getAiSourceImageMimeType()
                );
                calls++;
                attributes.ifPresent(dto -> applyAttributesToGroup(detections, entry.getKey(), dto));
            } catch (Exception e) {
                log.warn("Video vehicle attribute analysis skipped for {}: {}",
                        representative.getPlateText(),
                        e.getMessage());
            }
        }
    }

    private void applyAttributesToGroup(List<VideoDetection> detections, String groupKey, VehicleAttributesDto dto) {
        LocalDateTime analyzedAt = LocalDateTime.now();
        for (VideoDetection detection : detections) {
            if (!groupKey.equals(aiGroupKey(detection))) {
                continue;
            }

            detection.setAiMakeSuggestion(dto.getMake());
            detection.setAiModelSuggestion(dto.getModel());
            detection.setAiColorSuggestion(dto.getColor());
            detection.setAiBodyTypeSuggestion(dto.getBodyType());
            detection.setAiVehicleConfidence(dto.getConfidence());
            detection.setAiVehicleReasoning(dto.getReasoning());
            detection.setAiVehicleAnalyzedAt(analyzedAt);
        }
    }

    private String aiGroupKey(VideoDetection detection) {
        if (detection.getTrackId() != null) {
            return "track:" + detection.getTrackId();
        }
        return "plate:" + detection.getPlateText();
    }

    private double confidenceOf(VideoDetection detection) {
        return detection.getConfidence() != null ? detection.getConfidence() : 0.0;
    }

    private boolean shouldSkipAsDuplicate(VideoDetection previous, VideoDetection current) {
        if (previous == null || current == null) {
            return false;
        }

        if (previous.getPlateText() == null || current.getPlateText() == null) {
            return false;
        }

        int previousFrame = previous.getFrameIndex() != null ? previous.getFrameIndex() : 0;
        int currentFrame = current.getFrameIndex() != null ? current.getFrameIndex() : 0;
        int frameGap = Math.abs(currentFrame - previousFrame);

        return previous.getPlateText().equals(current.getPlateText()) && frameGap < minFrameGapPerTrack;
    }

    private String limitError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Procesarea video a esuat.";
        }
        if (errorMessage.length() <= 1000) {
            return errorMessage;
        }
        return errorMessage.substring(0, 1000);
    }
}
