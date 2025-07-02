package com.monitoring;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalErrorHandler {
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleAll(Exception ex) {
        return Map.of(
                "error", ex.getClass().getSimpleName(),
                "message", ex.getMessage()
        );
    }
}