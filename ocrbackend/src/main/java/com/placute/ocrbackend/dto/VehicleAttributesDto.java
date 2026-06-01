package com.placute.ocrbackend.dto;

public class VehicleAttributesDto {
    private String make;
    private String model;
    private String color;
    private String bodyType;
    private Double confidence;
    private String reasoning;

    public VehicleAttributesDto(String make, String model, String color, String bodyType,
                                Double confidence, String reasoning) {
        this.make = make;
        this.model = model;
        this.color = color;
        this.bodyType = bodyType;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }

    public String getMake() { return make; }
    public String getModel() { return model; }
    public String getColor() { return color; }
    public String getBodyType() { return bodyType; }
    public Double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
}
