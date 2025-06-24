package com.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@CrossOrigin(origins = "https://ai-infra-monitoring-ui.netlify.app")
@RestController
@RequestMapping("/alerts")
public class AlertController {

    @Autowired
    private RCAService rcaService;

    private final List<Alert> alerts = new CopyOnWriteArrayList<>();

    // POST /alerts
    @PostMapping
    public Mono<Alert> createAlert(@RequestBody LogInput input) {
        System.out.println("Received log: " + input.getLog());
        return rcaService.callRCA(input.getLog())
                .map(response -> {
                    Alert alert = new Alert(
                            input.getLog(),
                            response.getSummary(),
                            response.getConfidence(),
                            Instant.now().toString()
                    );
                    alerts.add(alert);
                    return alert;
                });
    }

    // GET /alerts
    @GetMapping
    public List<Alert> getAlerts() {
        return alerts;
    }
}