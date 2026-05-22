package com.vektor.mail.storage.db.repository;

import com.vektor.mail.core.model.Alias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AliasRepository extends JpaRepository<Alias, UUID> {
    Optional<Alias> findBySourceAddressAndActive(String sourceAddress, boolean active);
    List<Alias> findByDomainId(UUID domainId);
}
