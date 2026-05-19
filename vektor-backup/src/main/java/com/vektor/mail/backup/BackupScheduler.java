package com.vektor.mail.backup;

import com.vektor.mail.core.model.BackupJob;
import com.vektor.mail.core.plugin.BackupContext;
import com.vektor.mail.core.plugin.BackupException;
import com.vektor.mail.core.plugin.BackupPlugin;
import com.vektor.mail.storage.db.repository.BackupJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Triggers backup jobs on their configured cron schedules.
 * Checks every minute and runs jobs whose nextRunAt has passed.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class BackupScheduler {

    private final BackupJobRepository backupJobRepository;
    private final List<BackupPlugin> backupPlugins;

    @Value("${vektor.storage.maildir.base:./data/maildir}")
    private String mailDataDir;

    @Value("${vektor.security.master-key:}")
    private String masterKeyBase64;

    @Value("${spring.datasource.url:jdbc:h2:mem:vektor}")
    private String datasourceUrl;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void runDueJobs() {
        Instant now = Instant.now();
        Map<String, BackupPlugin> pluginMap = backupPlugins.stream()
                .collect(Collectors.toMap(BackupPlugin::getPluginId, Function.identity()));

        List<BackupJob> dueJobs = backupJobRepository.findByEnabledAndNextRunAtBefore(true, now);
        for (BackupJob job : dueJobs) {
            BackupPlugin plugin = pluginMap.get(job.getPluginId());
            if (plugin == null) {
                log.warn("Backup plugin not found: {}", job.getPluginId());
                continue;
            }
            log.info("Starting backup job {} using plugin {}", job.getId(), job.getPluginId());
            job.setLastRunAt(now);
            job.setLastStatus(BackupJob.Status.RUNNING);
            backupJobRepository.save(job);

            try {
                byte[] masterKey = masterKeyBase64.isBlank() ? new byte[32]
                        : Base64.getDecoder().decode(masterKeyBase64);
                plugin.backup(new BackupContext(
                        Path.of(mailDataDir),
                        datasourceUrl,
                        masterKey,
                        now,
                        "./data/backups"
                ));
                job.setLastStatus(BackupJob.Status.SUCCESS);
                job.setLastError(null);
            } catch (BackupException e) {
                job.setLastStatus(BackupJob.Status.FAILURE);
                job.setLastError(e.getMessage());
                log.error("Backup job {} failed", job.getId(), e);
            }
            backupJobRepository.save(job);
        }
    }
}
