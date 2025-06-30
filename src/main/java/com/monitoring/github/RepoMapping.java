package com.monitoring.github;

import jakarta.persistence.*;

@Entity
@Table(name = "repo_mappings")
public class RepoMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceName;

    private String owner;      // NEW
    private String repoName;   // NEW

    private String branch;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}