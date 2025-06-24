package com.monitoring;

public class RCARequest {
    private String logContext;

    public RCARequest(String logContext) {
        this.logContext = logContext;
    }

    public String getLogContext() {
        return logContext;
    }

    public void setLogContext(String logContext) {
        this.logContext = logContext;
    }
}
