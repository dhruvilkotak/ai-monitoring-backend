package com.monitoring.alerts;

public class LogInput {
    private String log;
    private String fileContent;

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public String getLog() {
        return log;
    }
    public void setLog(String log) {
        this.log = log;
    }
}