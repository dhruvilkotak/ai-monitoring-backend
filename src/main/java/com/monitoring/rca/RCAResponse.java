package com.monitoring.rca;

import java.util.List;

public class RCAResponse {
    private String summary;
    private String suggested_fix;
    private String file_path;
    private int start_line;
    private int end_line;
    private List<String> replacement_code;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSuggested_fix() {
        return suggested_fix;
    }

    public void setSuggested_fix(String suggested_fix) {
        this.suggested_fix = suggested_fix;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public int getStart_line() {
        return start_line;
    }

    public void setStart_line(int start_line) {
        this.start_line = start_line;
    }

    public int getEnd_line() {
        return end_line;
    }

    public void setEnd_line(int end_line) {
        this.end_line = end_line;
    }

    public List<String> getReplacement_code() {
        return replacement_code;
    }

    public void setReplacement_code(List<String> replacement_code) {
        this.replacement_code = replacement_code;
    }

    @Override
    public String toString() {
        return "RCAResponse{" +
                "summary='" + summary + '\'' +
                ", suggested_fix='" + suggested_fix + '\'' +
                ", file_path='" + file_path + '\'' +
                ", start_line=" + start_line +
                ", end_line=" + end_line +
                ", replacement_code=" + replacement_code +
                '}';
    }
}