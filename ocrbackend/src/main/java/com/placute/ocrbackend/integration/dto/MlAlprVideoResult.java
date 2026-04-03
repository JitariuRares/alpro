package com.placute.ocrbackend.integration.dto;

import java.util.List;

public class MlAlprVideoResult {

    private final Integer frameStep;
    private final Integer maxFrames;
    private final Double fps;
    private final Integer totalFrames;
    private final Integer processedFrames;
    private final Integer processingMs;
    private final List<Detection> detections;

    public MlAlprVideoResult(
            Integer frameStep,
            Integer maxFrames,
            Double fps,
            Integer totalFrames,
            Integer processedFrames,
            Integer processingMs,
            List<Detection> detections
    ) {
        this.frameStep = frameStep;
        this.maxFrames = maxFrames;
        this.fps = fps;
        this.totalFrames = totalFrames;
        this.processedFrames = processedFrames;
        this.processingMs = processingMs;
        this.detections = detections;
    }

    public Integer getFrameStep() {
        return frameStep;
    }

    public Integer getMaxFrames() {
        return maxFrames;
    }

    public Double getFps() {
        return fps;
    }

    public Integer getTotalFrames() {
        return totalFrames;
    }

    public Integer getProcessedFrames() {
        return processedFrames;
    }

    public Integer getProcessingMs() {
        return processingMs;
    }

    public List<Detection> getDetections() {
        return detections;
    }

    public static class Detection {
        private final Integer frameIndex;
        private final Long timestampMs;
        private final Integer trackId;
        private final String plateText;
        private final Double confidence;
        private final Bbox bbox;

        public Detection(
                Integer frameIndex,
                Long timestampMs,
                Integer trackId,
                String plateText,
                Double confidence,
                Bbox bbox
        ) {
            this.frameIndex = frameIndex;
            this.timestampMs = timestampMs;
            this.trackId = trackId;
            this.plateText = plateText;
            this.confidence = confidence;
            this.bbox = bbox;
        }

        public Integer getFrameIndex() {
            return frameIndex;
        }

        public Long getTimestampMs() {
            return timestampMs;
        }

        public Integer getTrackId() {
            return trackId;
        }

        public String getPlateText() {
            return plateText;
        }

        public Double getConfidence() {
            return confidence;
        }

        public Bbox getBbox() {
            return bbox;
        }
    }

    public static class Bbox {
        private final Integer x;
        private final Integer y;
        private final Integer w;
        private final Integer h;

        public Bbox(Integer x, Integer y, Integer w, Integer h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public Integer getX() {
            return x;
        }

        public Integer getY() {
            return y;
        }

        public Integer getW() {
            return w;
        }

        public Integer getH() {
            return h;
        }
    }
}
