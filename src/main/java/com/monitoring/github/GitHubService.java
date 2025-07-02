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

                    if (content.containsKey("encoding") && "base64".equals(content.get("encoding"))) {
                        String encoded = (String) content.get("content");
                        if (encoded == null) {
                            throw new RuntimeException("GitHub returned null content with encoding=base64");
                        }
                        try {
                            // remove any line breaks or spaces
                            byte[] decodedBytes = Base64.getDecoder().decode(encoded.replaceAll("\\s", ""));
                            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
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

    /**
     * Checks whether a given line in the file is inside a comment block,
     * and optionally closes it if needed before applying replacement code.
     *
     * @param fileContent the entire source file
     * @param targetLine  the 1-based line to patch
     * @return adjusted startLine to patch safely
     */
    public int adjustLineForComments(String fileContent, int targetLine) {
        if (fileContent == null || fileContent.isEmpty()) return targetLine;

        String[] lines = fileContent.split("\\r?\\n");
        int zeroBasedLine = targetLine - 1;

        boolean insideComment = false;
        for (int i = 0; i <= zeroBasedLine; i++) {
            String line = lines[i];
            if (line.contains("/*")) insideComment = true;
            if (line.contains("*/")) insideComment = false;
        }

        if (insideComment) {
            System.out.println("⚠️ The target line is inside a block comment. Will attempt to adjust.");
            // look forward to close comment
            for (int i = zeroBasedLine; i < lines.length; i++) {
                if (lines[i].contains("*/")) {
                    return i + 2; // next line after closing the block
                }
            }
            // fallback: do not patch if comment never closes
            return -1;
        }

        return targetLine;
    }
}