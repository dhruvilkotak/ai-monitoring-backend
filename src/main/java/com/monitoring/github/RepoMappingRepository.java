package com.monitoring.github;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepoMappingRepository extends JpaRepository<RepoMapping, Long> {
    List<RepoMapping> findByUser(User user);
}