package com.monitoring.github;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PRAuditRepository extends JpaRepository<PRAudit, Long> {
    // you can extend it with custom queries later if needed
}