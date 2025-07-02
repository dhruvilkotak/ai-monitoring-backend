package com.monitoring.alerts;

import com.monitoring.github.GitHubService;
import com.monitoring.github.RepoMapping;
import com.monitoring.github.RepoMappingRepository;
import com.monitoring.github.User;
import com.monitoring.github.UserRepository;
import com.monitoring.rca.RCAService;
import com.monitoring.rca.StackFrame;
import com.monitoring.rca.StackTraceParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepoMappingRepository repoMappingRepository;

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private RCAService rcaService;

    private final List<Alert> alerts = new CopyOnWriteArrayList<>();

    @PostMapping
    public Mono<String> createAlert(@RequestBody LogInput input) {
        var frames = StackTraceParser.parse(input.getLog());

        if (frames.isEmpty()) {
            return Mono.just("No stack trace found in log.");
        }

        StackFrame topFrame = frames.get(0);
        String filePath = extractFilePathFromFrame(topFrame);
        int lineNumber = topFrame.getLineNumber();

        Long userId = 1L; // hard-coded for now
        User user = userRepository.findById(userId).orElseThrow();

        RepoMapping repo = repoMappingRepository.findByUser(user)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No repo mapping found for user."));

        return gitHubService.fetchFileContent(user, repo, repo.getBranch(), filePath)
                .flatMap(fileContent -> {
                    // extract snippet around the failure
                    String snippet = gitHubService.extractSnippet(fileContent, lineNumber, 10);

                    return rcaService.suggestFix(input.getLog(), snippet)
                            .flatMap(rcaResponse -> {
                                // store in-memory alerts
                                alerts.add(new Alert(
                                        input.getLog(),
                                        rcaResponse.getSummary(),
                                        0.9,
                                        Instant.now().toString(),
                                        rcaResponse.getSuggested_fix()
                                ));

                                if (rcaResponse.getReplacement_code() == null ||
                                        rcaResponse.getReplacement_code().isEmpty()) {
                                    return Mono.just("No fix suggested by RCA, but alert recorded.");
                                }

                                // 🔥 check if target line is in a comment
                                int safeLine = gitHubService.adjustLineForComments(fileContent, rcaResponse.getStart_line());
                                if (safeLine == -1) {
                                    return Mono.just("Fix suggested, but the line is inside an unclosed comment block. Alert recorded, no PR created.");
                                }

                                // rebuild file
                                String[] lines = fileContent.split("\\r?\\n");
                                int start = Math.max(0, safeLine - 1); // safe 0-based
                                int end = Math.min(lines.length, rcaResponse.getEnd_line());

                                StringBuilder patched = new StringBuilder();
                                for (int i = 0; i < start; i++) {
                                    patched.append(lines[i]).append("\n");
                                }
                                for (String line : rcaResponse.getReplacement_code()) {
                                    patched.append(line).append("\n");
                                }
                                for (int i = end; i < lines.length; i++) {
                                    patched.append(lines[i]).append("\n");
                                }

                                String fixBranch = "cortexops-fix-" + System.currentTimeMillis();

                                return gitHubService.createBranch(user, repo, fixBranch)
                                        .flatMap(branchResult -> gitHubService.commitFix(
                                                user,
                                                repo,
                                                fixBranch,
                                                filePath,
                                                patched.toString(),
                                                "Automated RCA fix"
                                        ).flatMap(commitResult -> gitHubService.createPullRequest(
                                                user,
                                                repo,
                                                fixBranch,
                                                "Automated RCA Fix",
                                                "This PR was created automatically by CortexOps."
                                        )));
                            });
                });
    }

    /**
     * Determine the correct file path based on stack trace frame and language.
     */
    private String extractFilePathFromFrame(StackFrame frame) {
        String fileName = frame.getFileName();
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1);

        Map<String, String> languageRoots = Map.of(
                "py", "src/python/",
                "js", "src/javascript/",
                "ts", "src/typescript/",
                "go", "src/go/"
        );
        String root = languageRoots.getOrDefault(ext, "src/");

        if (ext.equals("java")) {
            String classPath = frame.getClassName().replace('.', '/');
            String packageFolder = classPath.substring(0, classPath.lastIndexOf('/'));
            return "src/" + packageFolder + "/" + fileName;
        } else {
            return root + fileName;
        }
    }

    @GetMapping
    public List<Alert> getAlerts() {
        return alerts;
    }
}