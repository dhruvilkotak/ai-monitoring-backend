package com.monitoring.github;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // you can add custom finders later if needed
}