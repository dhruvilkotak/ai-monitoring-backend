package com.monitoring.github;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GitHubService {
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.github.com")
            .build();

    public Mono<String> createBranch(User user, RepoMapping repo, String newBranch) {
        final String token;
        try {
            token = EncryptionUtil.decrypt(user.getGithubTokenEncrypted());
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt GitHub token", e);
        }

        return webClient.get()
                .uri("/repos/{owner}/{repo}/git/ref/heads/{branch}",
                        repo.getOwner(), repo.getRepoName(), repo.getBranch())
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(refData -> {
                    Map<String, Object> object = (Map<String, Object>) refData.get("object");
                    String baseSha = (String) object.get("sha");

                    return webClient.post()
                            .uri("/repos/{owner}/{repo}/git/refs", repo.getOwner(), repo.getRepoName())
                            .headers(h -> h.setBearerAuth(token))
                            .bodyValue(Map.of(
                                    "ref", "refs/heads/" + newBranch,
                                    "sha", baseSha
                            ))
                            .retrieve()
                            .bodyToMono(String.class);
                });
    }

    public Mono<String> commitFix(User user, RepoMapping repo, String branch, String filePath, String fileContent, String commitMessage) {
        final String token;
        try {
            token = EncryptionUtil.decrypt(user.getGithubTokenEncrypted());
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt GitHub token", e);
        }

        return webClient.get()
                .uri("/repos/{owner}/{repo}/git/ref/heads/{branch}", repo.getOwner(), repo.getRepoName(), branch)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(refData -> {
                    Map<String, Object> object = (Map<String, Object>) refData.get("object");
                    String baseSha = (String) object.get("sha");

                    return webClient.post()
                            .uri("/repos/{owner}/{repo}/git/blobs", repo.getOwner(), repo.getRepoName())
                            .headers(h -> h.setBearerAuth(token))
                            .bodyValue(Map.of(
                                    "content", fileContent,
                                    "encoding", "utf-8"
                            ))
                            .retrieve()
                            .bodyToMono(Map.class)
                            .flatMap(blobResponse -> {
                                String blobSha = (String) blobResponse.get("sha");

                                return webClient.get()
                                        .uri("/repos/{owner}/{repo}/git/commits/{sha}",
                                                repo.getOwner(), repo.getRepoName(), baseSha)
                                        .headers(h -> h.setBearerAuth(token))
                                        .retrieve()
                                        .bodyToMono(Map.class)
                                        .flatMap(commitData -> {
                                            Map<String, Object> tree = (Map<String, Object>) commitData.get("tree");
                                            String baseTreeSha = (String) tree.get("sha");

                                            return webClient.post()
                                                    .uri("/repos/{owner}/{repo}/git/trees", repo.getOwner(), repo.getRepoName())
                                                    .headers(h -> h.setBearerAuth(token))
                                                    .bodyValue(Map.of(
                                                            "base_tree", baseTreeSha,
                                                            "tree", List.of(Map.of(
                                                                    "path", filePath,
                                                                    "mode", "100644",
                                                                    "type", "blob",
                                                                    "sha", blobSha
                                                            ))
                                                    ))
                                                    .retrieve()
                                                    .bodyToMono(Map.class)
                                                    .flatMap(treeData -> {
                                                        String newTreeSha = (String) treeData.get("sha");

                                                        return webClient.post()
                                                                .uri("/repos/{owner}/{repo}/git/commits", repo.getOwner(), repo.getRepoName())
                                                                .headers(h -> h.setBearerAuth(token))
                                                                .bodyValue(Map.of(
                                                                        "message", commitMessage,
                                                                        "tree", newTreeSha,
                                                                        "parents", List.of(baseSha)
                                                                ))
                                                                .retrieve()
                                                                .bodyToMono(Map.class)
                                                                .flatMap(newCommit -> {
                                                                    String newCommitSha = (String) newCommit.get("sha");

                                                                    return webClient.patch()
                                                                            .uri("/repos/{owner}/{repo}/git/refs/heads/{branch}",
                                                                                    repo.getOwner(), repo.getRepoName(), branch)
                                                                            .headers(h -> h.setBearerAuth(token))
                                                                            .bodyValue(Map.of(
                                                                                    "sha", newCommitSha
                                                                            ))
                                                                            .retrieve()
                                                                            .bodyToMono(String.class);
                                                                });
                                                    });
                                        });
                            });
                });
    }

    public Mono<String> createPullRequest(User user, RepoMapping repo, String branch, String title, String body) {
        final String token;
        try {
            token = EncryptionUtil.decrypt(user.getGithubTokenEncrypted());
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt token", e);
        }

        return webClient.post()
                .uri("/repos/{owner}/{repo}/pulls", repo.getOwner(), repo.getRepoName())
                .headers(h -> h.setBearerAuth(token))
                .bodyValue(Map.of(
                        "title", title,
                        "head", branch,
                        "base", repo.getBranch(),
                        "body", body
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .map(prData -> {
                    String prUrl = (String) prData.get("html_url");
                    System.out.println("✅ PR opened at: " + prUrl);
                    return prUrl;
                });
    }

    public Mono<String> fetchFileContent(User user, RepoMapping repo, String branch, String filePath) {
        String token;
        try {
            token = EncryptionUtil.decrypt(user.getGithubTokenEncrypted());
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to decrypt GitHub token", e));
        }

        return webClient.get()
                .uri("/repos/{owner}/{repo}/contents/{filePath}?ref={branch}",
                        repo.getOwner(), repo.getRepoName(), filePath, branch)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(Map.class)
                .map(content -> {
                    System.out.println("DEBUG GitHub raw content JSON: " + content);

                    if (content.containsKey("encoding") && "base64".equals(content.get("encoding"))) {
                        String encoded = (String) content.get("content");
                        if (encoded == null) {
                            throw new RuntimeException("GitHub returned null content with encoding=base64");
                        }
                        try {
                            // remove any line breaks or spaces
                            byte[] decodedBytes = Base64.getDecoder().decode(encoded.replaceAll("\\s", ""));
                            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
                            System.out.println("✅ Successfully decoded first 100 characters:\n" + decoded.substring(0, Math.min(100, decoded.length())));
                            return decoded;
                        } catch (IllegalArgumentException ex) {
                            throw new RuntimeException("Base64 decode failed on cleaned GitHub content: "
                                    + encoded.substring(0, Math.min(30, encoded.length())) + "...", ex);
                        }
                    } else {
                        throw new RuntimeException("GitHub did not return base64 encoding or missing field: " + content);
                    }
                });
    }

    /**
     * Extracts a snippet of code around the failure line.
     *
     * @param fileContent Full file content
     * @param lineNumber  The line with the error (1-based)
     * @param radius      Number of surrounding lines to include before/after
     * @return Snippet of source code
     */
    public String extractSnippet(String fileContent, int lineNumber, int radius) {
        if (fileContent == null || fileContent.isEmpty()) {
            return "";
        }
        String[] lines = fileContent.split("\\r?\\n");
        int start = Math.max(0, lineNumber - radius - 1); // zero-based index
        int end = Math.min(lines.length, lineNumber + radius);

        StringBuilder snippet = new StringBuilder();
        for (int i = start; i < end; i++) {
            snippet.append(lines[i]).append("\n");
        }
        return snippet.toString();
    }
}