package com.vektor.mail.storage.db.repository;

import com.vektor.mail.core.model.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {
    List<Rule> findByAccountIdAndActiveOrderByPriorityAsc(UUID accountId, boolean active);
}
