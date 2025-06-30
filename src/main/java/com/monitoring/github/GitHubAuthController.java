package com.monitoring.github;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.net.URI;
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
    public Mono<String> githubCallback(@RequestParam String code, @RequestParam String state) {
        return WebClient.create("https://github.com")
                .post()
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
                    try {
                        String encryptedToken = EncryptionUtil.encrypt(token);
                    } catch (Exception e) {
                        throw new RuntimeException("Encryption failed", e);
                    }

                    // now call GitHub /user
                    return WebClient.create("https://github.com")
                            .post()
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
                            .map(tokenresponse -> {
                                String usertoken = (String) tokenresponse.get("access_token");

                                // lookup user or create new
                                User user = new User();
                                try {
                                    user.setGithubTokenEncrypted(EncryptionUtil.encrypt(usertoken));
                                } catch (Exception e) {
                                    throw new RuntimeException("Encryption failed", e);
                                }
                                user.setGithubLogin("someLogin"); // fill from GitHub profile if needed
                                user.setGithubId(12345L);         // fill from GitHub profile if needed
                                userRepository.save(user);

                                return "GitHub connected successfully! You can now use CortexOps.";
                            });
                });
    }

    private Mono<String> storeTokenForUser(String token, WebSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Mono.just("No logged in user found in session.");
        }

        String encryptedToken;
        try {
            encryptedToken = EncryptionUtil.encrypt(token);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Encryption failed", e));
        }

        return Mono.fromCallable(() -> userRepository.findById(userId))
                .flatMap(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        User user = optionalUser.get();
                        user.setGithubTokenEncrypted(encryptedToken);
                        userRepository.save(user);
                        return Mono.just("✅ GitHub connected successfully! You can now use CortexOps.");
                    } else {
                        return Mono.just("❌ No user found for this session.");
                    }
                });
    }
}