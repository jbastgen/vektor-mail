package com.vektor.mail.storage.db.repository;

import com.vektor.mail.core.model.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DomainRepository extends JpaRepository<Domain, UUID> {
    Optional<Domain> findByName(String name);
    boolean existsByName(String name);
}
