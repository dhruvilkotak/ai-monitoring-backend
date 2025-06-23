package com.monitoring;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RCAService {

    private final WebClient client = WebClient.create("http://localhost:8000");

    public Mono<RCAResponse> callRCA(String log) {
        RCARequest request = new RCARequest(log);
        return client.post()
                .uri("/rca")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RCAResponse.class);
    }
}