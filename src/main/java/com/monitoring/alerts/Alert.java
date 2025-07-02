package com.monitoring.alerts;

public class Alert {
    private final String originalLog;
    private final String summary;
    private final double confidence;
    private final String suggestedFix;
    private final String timestamp;

    public Alert(String originalLog, String summary, double confidence, String timestamp, String suggestedFix) {
        this.originalLog = originalLog;
        this.summary = summary;
        this.confidence = confidence;
        this.timestamp = timestamp;
        this.suggestedFix = suggestedFix;
    }

    public String getOriginalLog() {
        return originalLog;
    }

    public String getSummary() {
        return summary;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSuggestedFix() {
        return suggestedFix;
    }
}