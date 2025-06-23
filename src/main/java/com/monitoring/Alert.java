package com.monitoring;

public class Alert {
    private final String originalLog;
    private final String summary;
    private final double confidence;
    private final String timestamp;

    public Alert(String originalLog, String summary, double confidence, String timestamp) {
        this.originalLog = originalLog;
        this.summary = summary;
        this.confidence = confidence;
        this.timestamp = timestamp;
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
}