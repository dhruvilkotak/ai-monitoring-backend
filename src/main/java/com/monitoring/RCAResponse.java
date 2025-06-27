package com.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RCAResponse {
    private String summary;

    @JsonProperty("suggested_fix")
    private String suggestedFix;

    private double confidence;

    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSuggestedFix() {
        return suggestedFix;
    }
    public void setSuggestedFix(String suggestedFix) {
        this.suggestedFix = suggestedFix;
    }

    public double getConfidence() {
        return confidence;
    }
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}