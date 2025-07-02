package com.monitoring.rca;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class RCAService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://ai-rca-service.onrender.com")
            .build();

    public Mono<RCAResponse> suggestFix(String log, String fileContent) {
        if (log == null) log = "";
        if (fileContent == null) fileContent = "";

        System.out.println("🔍 [RCAService] Sending logContext:");
        System.out.println(log.length() > 300 ? log.substring(0, 300) + "..." : log);

        System.out.println("🔍 [RCAService] Sending fileContent preview:");
        System.out.println(fileContent.isBlank() ? "(empty)" :
                (fileContent.length() > 300 ? fileContent.substring(0, 300) + "..." : fileContent)
        );

        return webClient.post()
                .uri("/rca")
                .bodyValue(Map.of(
                        "logContext", log,
                        "fileContent", fileContent
                ))
                .retrieve()
                .bodyToMono(RCAResponse.class)
                .doOnError(WebClientResponseException.class, ex -> {
                    System.err.println("❌ [RCAService] RCA responded with error: " + ex.getResponseBodyAsString());
                })
                .onErrorResume(e -> {
                    System.err.println("❌ [RCAService] Exception: " + e.getMessage());
                    return Mono.empty();
                });
    }
}