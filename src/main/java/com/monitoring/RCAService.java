package com.monitoring;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RCAService {

    private final WebClient client = WebClient.builder()
            .baseUrl("https://ai-rca-service.onrender.com")
            .build();

    public Mono<RCAResponse> callRCA(String log) {
        RCARequest request = new RCARequest(log);
        System.out.println("Sending to RCA: " + log);
        return client.post()
                .uri("/rca")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RCAResponse.class);
    }
}