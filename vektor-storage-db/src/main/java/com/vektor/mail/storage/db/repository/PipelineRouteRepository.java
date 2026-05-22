package com.vektor.mail.storage.db.repository;

import com.vektor.mail.core.model.PipelineRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PipelineRouteRepository extends JpaRepository<PipelineRoute, UUID> {
    List<PipelineRoute> findByEnabled(boolean enabled);
}
