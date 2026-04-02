package com.placute.ocrbackend.integration.dto;

import java.util.List;

public class MlAlprResult {

    private final String plateText;
    private final Double confidence;
    private final Bbox bbox;
    private final List<Candidate> candidates;

    public MlAlprResult(String plateText, Double confidence, Bbox bbox, List<Candidate> candidates) {
        this.plateText = plateText;
        this.confidence = confidence;
        this.bbox = bbox;
        this.candidates = candidates;
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

    public List<Candidate> getCandidates() {
        return candidates;
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

    public static class Candidate {
        private final String text;
        private final Double confidence;

        public Candidate(String text, Double confidence) {
            this.text = text;
            this.confidence = confidence;
        }

        public String getText() {
            return text;
        }

        public Double getConfidence() {
            return confidence;
        }
    }
}
