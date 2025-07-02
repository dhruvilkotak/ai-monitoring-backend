package com.monitoring.alerts;

import com.monitoring.github.*;
import com.monitoring.rca.RCAService;
import com.monitoring.rca.StackFrame;
import com.monitoring.rca.StackTraceParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private AlertRepository alertRepository;

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
                    String snippet = gitHubService.extractSnippet(fileContent, lineNumber, 10);

                    return rcaService.suggestFix(input.getLog(), snippet)
                            .flatMap(rcaResponse -> {
                                // store the alert persistently
                                AlertEntity entity = new AlertEntity(
                                        userId,
                                        input.getLog(),
                                        rcaResponse.getSummary(),
                                        0.9,
                                        Instant.now().toString(),
                                        rcaResponse.getSuggested_fix()
                                );
                                alertRepository.save(entity);

                                if ("no-op".equalsIgnoreCase(rcaResponse.getOperation())) {
                                    return Mono.just("No code change needed, alert recorded.");
                                }

                                int safeLine = gitHubService.adjustLineForComments(fileContent, rcaResponse.getStart_line());
                                if (safeLine == -1) {
                                    return Mono.just("Fix suggested, but target line inside an unclosed comment block. Alert recorded, no PR created.");
                                }

                                String[] lines = fileContent.split("\\r?\\n");
                                StringBuilder patched = new StringBuilder();
                                int start = Math.max(0, safeLine - 1);
                                int end = Math.min(lines.length, rcaResponse.getEnd_line());

                                switch (rcaResponse.getOperation()) {
                                    case "replace":
                                        for (int i = 0; i < start; i++) patched.append(lines[i]).append("\n");
                                        for (String line : rcaResponse.getFinal_code()) patched.append(line).append("\n");
                                        for (int i = end; i < lines.length; i++) patched.append(lines[i]).append("\n");
                                        break;
                                    case "delete":
                                        for (int i = 0; i < start; i++) patched.append(lines[i]).append("\n");
                                        for (int i = end; i < lines.length; i++) patched.append(lines[i]).append("\n");
                                        break;
                                    case "insert":
                                        for (int i = 0; i < start; i++) patched.append(lines[i]).append("\n");
                                        for (String line : rcaResponse.getFinal_code()) patched.append(line).append("\n");
                                        for (int i = start; i < lines.length; i++) patched.append(lines[i]).append("\n");
                                        break;
                                    default:
                                        return Mono.just("Unknown operation from RCA. Alert recorded, no PR created.");
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

    @GetMapping
    public List<AlertEntity> getAlerts(@RequestParam Long userId) {
        return alertRepository.findByUserId(userId);
    }

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
}