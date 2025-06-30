package com.monitoring.github;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/repo-mappings")
public class RepoMappingController {
    private final RepoMappingRepository repoMappingRepository;
    private final UserRepository userRepository;

    public RepoMappingController(RepoMappingRepository repoMappingRepository, UserRepository userRepository) {
        this.repoMappingRepository = repoMappingRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public RepoMapping createMapping(@RequestBody RepoMappingDto mapping) {
        User user = userRepository.findById(mapping.getUserId()).orElseThrow();
        RepoMapping repoMapping = new RepoMapping();
        repoMapping.setServiceName(mapping.getServiceName());
        repoMapping.setRepoName(mapping.getRepoName());
        repoMapping.setOwner(mapping.getOwner());
        repoMapping.setBranch(mapping.getBranch());
        repoMapping.setUser(user);
        return repoMappingRepository.save(repoMapping);
    }

    @GetMapping("/{userId}")
    public List<RepoMapping> getUserMappings(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return repoMappingRepository.findByUser(user);
    }
}