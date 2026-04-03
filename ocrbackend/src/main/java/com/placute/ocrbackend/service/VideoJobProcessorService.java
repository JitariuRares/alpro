package com.placute.ocrbackend.service;

import com.placute.ocrbackend.integration.MlAlprClient;
import com.placute.ocrbackend.integration.dto.MlAlprVideoResult;
import com.placute.ocrbackend.model.VideoDetection;
import com.placute.ocrbackend.model.VideoJob;
import com.placute.ocrbackend.model.VideoJobStatus;
import com.placute.ocrbackend.repository.VideoDetectionRepository;
import com.placute.ocrbackend.repository.VideoJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VideoJobProcessorService {

    @Autowired
    private VideoJobRepository videoJobRepository;

    @Autowired
    private VideoDetectionRepository videoDetectionRepository;

    @Autowired
    private MlAlprClient mlAlprClient;

    @Value("${alpr.video.min-confidence:0.70}")
    private double minConfidence;

    @Value("${alpr.video.min-frame-gap-per-track:12}")
    private int minFrameGapPerTrack;

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
            if (mlDetection.getConfidence() != null && mlDetection.getConfidence() < minConfidence) {
                continue;
            }

            VideoDetection detection = new VideoDetection();
            detection.setJob(job);
            detection.setFrameIndex(mlDetection.getFrameIndex() != null ? mlDetection.getFrameIndex() : 0);
            detection.setTimestampMs(mlDetection.getTimestampMs());
            detection.setTrackId(mlDetection.getTrackId());
            detection.setPlateText(mlDetection.getPlateText());
            detection.setConfidence(mlDetection.getConfidence());
            if (mlDetection.getBbox() != null) {
                detection.setBboxX(mlDetection.getBbox().getX());
                detection.setBboxY(mlDetection.getBbox().getY());
                detection.setBboxW(mlDetection.getBbox().getW());
                detection.setBboxH(mlDetection.getBbox().getH());
            }

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
