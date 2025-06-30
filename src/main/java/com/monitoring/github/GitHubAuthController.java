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
                .flatMap(session -> {
                    session.getAttributes().put("oauthState", state);
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
                    if (token == null || token.isBlank()) {
                        return Mono.error(new RuntimeException("GitHub did not return access_token"));
                    }

                    String encryptedToken;
                    try {
                        encryptedToken = EncryptionUtil.encrypt(token);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Encryption failed", e));
                    }

                    // Now call GitHub /user to get profile
                    return WebClient.create("https://api.github.com")
                            .get()
                            .uri("/user")
                            .headers(h -> h.setBearerAuth(token))
                            .retrieve()
                            .bodyToMono(Map.class)
                            .flatMap(profile -> {
                                System.out.println("GitHub profile: " + profile);
                                String login = (String) profile.get("login");
                                Integer id = (Integer) profile.get("id");

                                if (login == null || id == null) {
                                    return Mono.error(new RuntimeException("GitHub profile information missing"));
                                }

                                // save to DB
                                User user = new User();
                                user.setGithubTokenEncrypted(encryptedToken);
                                user.setGithubLogin(login);
                                user.setGithubId(id.longValue());
                                userRepository.save(user);

                                return Mono.just("✅ GitHub connected successfully! You can now use CortexOps.");
                            });
                });
    }
}