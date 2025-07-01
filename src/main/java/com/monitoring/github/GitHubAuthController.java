package com.monitoring.github;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/github")
public class GitHubAuthController {

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.redirect.uri}")
    private String redirectUri;

    @Resource
    private UserRepository userRepository;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://github.com")
            .build();

    @GetMapping("/login")
    public Mono<Void> githubLogin(ServerWebExchange exchange) {
        String state = UUID.randomUUID().toString();
        return exchange.getSession()
                .flatMap(session -> {
                    session.getAttributes().put("oauthState", state);
                    String url = String.format(
                            "https://github.com/login/oauth/authorize?client_id=%s&scope=repo,read:user&state=%s&redirect_uri=%s",
                            clientId, state, redirectUri
                    );
                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                    exchange.getResponse().getHeaders().setLocation(URI.create(url));
                    return exchange.getResponse().setComplete();
                });
    }

    @GetMapping("/callback")
    public Mono<Void> githubCallback(
            @RequestParam String code,
            @RequestParam String state,
            ServerWebExchange exchange
    ) {
        return webClient.post()
                .uri("/login/oauth/access_token")
                .header("Accept", "application/json")
                .bodyValue(Map.of(
                        "client_id", clientId,
                        "client_secret", clientSecret,
                        "code", code,
                        "redirect_uri", redirectUri
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    String token = (String) response.get("access_token");
                    if (token == null) {
                        return Mono.error(new IllegalStateException("OAuth flow failed: no token"));
                    }

                    String encryptedToken;
                    try {
                        encryptedToken = EncryptionUtil.encrypt(token);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Encryption failed", e));
                    }

                    // get profile from GitHub
                    return WebClient.create("https://api.github.com")
                            .get()
                            .uri("/user")
                            .headers(h -> h.setBearerAuth(token))
                            .retrieve()
                            .bodyToMono(Map.class)
                            .flatMap(profile -> {
                                String login = (String) profile.get("login");
                                Long githubId = ((Number) profile.get("id")).longValue();

                                User user = userRepository.findByGithubId(githubId)
                                        .orElse(new User());
                                user.setGithubId(githubId);
                                user.setGithubLogin(login);
                                user.setGithubTokenEncrypted(encryptedToken);

                                userRepository.save(user);

                                String redirectUrl = String.format(
                                        "https://ai-infra-monitoring-ui.netlify.app/onboard?userId=%d", user.getId()
                                );
                                exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                                exchange.getResponse().getHeaders().setLocation(URI.create(redirectUrl));
                                return exchange.getResponse().setComplete();
                            });
                });
    }

    @GetMapping("/user-repos")
    public Mono<ResponseEntity<List<Map<String, Object>>>> getUserRepos(@RequestParam Long userId) {
        return Mono.fromCallable(() -> userRepository.findById(userId))
                .flatMap(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        User user = optionalUser.get();
                        String decryptedToken;
                        try {
                            decryptedToken = EncryptionUtil.decrypt(user.getGithubTokenEncrypted());
                        } catch (Exception e) {
                            return Mono.error(new RuntimeException("Could not decrypt token", e));
                        }

                        return WebClient.create("https://api.github.com")
                                .get()
                                .uri("/user/repos")
                                .headers(h -> h.setBearerAuth(decryptedToken))
                                .retrieve()
                                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                                .collectList()
                                .map(list -> ResponseEntity.ok(list));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                    }
                });
    }
}