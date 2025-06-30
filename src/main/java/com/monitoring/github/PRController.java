package com.monitoring.github;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/pr")
public class PRController {
    private final GitHubService githubService;
    private final UserRepository userRepository;
    private final RepoMappingRepository repoMappingRepository;
    private final PRAuditRepository prAuditRepository;

    public PRController(GitHubService githubService,
                        UserRepository userRepository,
                        RepoMappingRepository repoMappingRepository,
                        PRAuditRepository prAuditRepository) {
        this.githubService = githubService;
        this.userRepository = userRepository;
        this.repoMappingRepository = repoMappingRepository;
        this.prAuditRepository = prAuditRepository;
    }

    @PostMapping("/trigger")
    public Mono<String> triggerPR(@RequestParam Long userId,
                                  @RequestParam Long repoMappingId,
                                  @RequestParam String filePath,
                                  @RequestParam String fileContents,
                                  @RequestParam String commitMessage,
                                  @RequestParam String prTitle,
                                  @RequestParam String prBody) {
        User user = userRepository.findById(userId).orElseThrow();
        RepoMapping repoMapping = repoMappingRepository.findById(repoMappingId).orElseThrow();

        String newBranch = "cortexops-fix-" + System.currentTimeMillis();

        return githubService.createBranch(user, repoMapping, newBranch)
                .flatMap(branchResult -> githubService.commitFix(user, repoMapping, newBranch, filePath, fileContents, commitMessage))
                .flatMap(commitResult -> githubService.createPullRequest(user, repoMapping, newBranch, prTitle, prBody))
                .map(prUrl -> {
                    // save to audit
                    PRAudit audit = new PRAudit();
                    audit.setBranch(newBranch);
                    audit.setStatus("OPEN");
                    audit.setPrUrl(prUrl);
                    audit.setRepoMapping(repoMapping);
                    prAuditRepository.save(audit);
                    return prUrl;
                });
    }
}