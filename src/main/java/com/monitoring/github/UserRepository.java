package com.monitoring.github;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // you can add custom finders later if needed
    Optional<User> findByGithubId(Long githubId);
}