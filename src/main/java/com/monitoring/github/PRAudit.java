package com.monitoring.github;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pr_audit")
public class PRAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String prUrl;

    private String branch;

    private String status;

    private Instant createdAt = Instant.now();

    @ManyToOne
    @JoinColumn(name = "repo_mapping_id")
    private RepoMapping repoMapping;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPrUrl() { return prUrl; }
    public void setPrUrl(String prUrl) { this.prUrl = prUrl; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public RepoMapping getRepoMapping() { return repoMapping; }
    public void setRepoMapping(RepoMapping repoMapping) { this.repoMapping = repoMapping; }
}