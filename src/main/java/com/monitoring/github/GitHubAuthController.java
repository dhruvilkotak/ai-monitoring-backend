package com.monitoring.github;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class GitHubAuthController {

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.redirect.uri}")
    private String redirectUri;

    @Resource
    private UserRepository userRepository;

    private final WebClient webClient = WebClient.builder().baseUrl("https://github.com").build();

    @GetMapping("/github/login")
    public Mono<Void> githubLogin(ServerWebExchange exchange) {
        String state = UUID.randomUUID().toString();

        return exchange.getSession()
                .flatMap(webSession -> {
                    webSession.getAttributes().put("oauthState", state);
                    String url = "https://github.com/login/oauth/authorize"
                            + "?client_id=" + clientId
                            + "&scope=repo,read:user"
                            + "&state=" + state
                            + "&redirect_uri=" + redirectUri;
                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                    exchange.getResponse().getHeaders().setLocation(URI.create(url));
                    return exchange.getResponse().setComplete();
                });
    }

    @GetMapping("/github/callback")
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
                        return Mono.error(new RuntimeException("OAuth flow failed: no token"));
                    }

                    String encryptedToken;
                    try {
                        encryptedToken = EncryptionUtil.encrypt(token);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Encryption failed", e));
                    }

                    // ideally get GitHub user profile here
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

                                // redirect to frontend after success
                                exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                                exchange.getResponse().getHeaders().setLocation(
                                        URI.create("https://ai-infra-monitoring-ui.netlify.app/onboard?userId=" + user.getId())
                                );
                                return exchange.getResponse().setComplete();
                            });
                });
    }

    @GetMapping("/github/user-repos")
    public Mono<List<Map<String, Object>>> getUserRepos(@RequestParam Long userId) {
        return Mono.fromCallable(() -> userRepository.findById(userId))
                .flatMap(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        User user = optionalUser.get();
                        String decrypted;
                        try {
                            decrypted = EncryptionUtil.decrypt(user.getGithubTokenEncrypted());
                        } catch (Exception e) {
                            return Mono.error(new RuntimeException("Could not decrypt token", e));
                        }

                        return WebClient.create("https://api.github.com")
                                .get()
                                .uri("/user/repos")
                                .headers(h -> h.setBearerAuth(decrypted))
                                .retrieve()
                                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                                .collectList();
                    } else {
                        return Mono.error(new RuntimeException("User not found"));
                    }
                });
    }
}