package com.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
public class TelemetryController {

    @Autowired
    private RCAService rcaService;

    private final List<Alert> alerts = new CopyOnWriteArrayList<>();

    @PostMapping("/telemetry")
    public Mono<Alert> handleTelemetry(@RequestBody LogInput input) {
        return rcaService.callRCA(input.getLog())
                .map(response -> {
                    Alert alert = new Alert(input.getLog(), response.getSummary(), response.getConfidence(), Instant.now().toString());
                    alerts.add(alert);
                    return alert;
                });
    }

    @GetMapping("/alerts")
    public List<Alert> getAlerts() {
        return alerts;
    }
}