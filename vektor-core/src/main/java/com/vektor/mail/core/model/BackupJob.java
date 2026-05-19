package com.vektor.mail.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backup_jobs")
@Getter
@Setter
public class BackupJob {

    public enum Status { IDLE, RUNNING, SUCCESS, FAILURE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plugin_id", nullable = false, length = 255)
    private String pluginId;

    /** Cron expression, e.g. "0 2 * * *" */
    @Column(nullable = false, length = 128)
    private String schedule;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_status", length = 32)
    private Status lastStatus = Status.IDLE;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private boolean enabled = true;
}
