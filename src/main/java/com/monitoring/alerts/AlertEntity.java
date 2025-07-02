package com.monitoring.alerts;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AlertEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String originalLog;
    private String summary;
    private double confidence;
    private String timestamp;
    private String suggestedFix;

    private String prUrl;

    public AlertEntity() {}

    public AlertEntity(Long userId, String originalLog, String summary, double confidence, String timestamp, String suggestedFix) {
        this.userId = userId;
        this.originalLog = originalLog;
        this.summary = summary;
        this.confidence = confidence;
        this.timestamp = timestamp;
        this.suggestedFix = suggestedFix;
    }

    // getters and setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOriginalLog() { return originalLog; }
    public void setOriginalLog(String originalLog) { this.originalLog = originalLog; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getSuggestedFix() { return suggestedFix; }
    public void setSuggestedFix(String suggestedFix) { this.suggestedFix = suggestedFix; }
    public String getPrUrl() { return prUrl; }
    public void setPrUrl(String prUrl) { this.prUrl = prUrl; }
}