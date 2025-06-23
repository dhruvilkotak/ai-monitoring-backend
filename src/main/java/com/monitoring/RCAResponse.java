package com.monitoring;

public class RCAResponse {
    private String summary;
    private double confidence;

    public RCAResponse() {}

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}