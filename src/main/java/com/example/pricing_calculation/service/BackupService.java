package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.dto.BackupResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class BackupService {

    private final Path backupDirectory;
    private final AuditLogService auditLogService;
    private volatile BackupResponse lastBackup;

    public BackupService(
            @Value("${parking.backup.directory:target/backups}") String backupDirectory,
            AuditLogService auditLogService) {
        this.backupDirectory = Path.of(backupDirectory);
        this.auditLogService = auditLogService;
    }

    @Scheduled(fixedDelayString = "${parking.backup.fixed-delay-ms:900000}")
    public void scheduledBackup() {
        createBackup(null, "SCHEDULED_BACKUP");
    }

    public BackupResponse createManualBackup(UserAccount user) {
        return createBackup(user, "MANUAL_BACKUP");
    }

    public BackupResponse lastBackup() {
        return lastBackup == null
                ? new BackupResponse("NOT_RUN", null, null, "No backup checkpoint has been created yet")
                : lastBackup;
    }

    private BackupResponse createBackup(UserAccount user, String action) {
        LocalDateTime now = LocalDateTime.now();
        try {
            Files.createDirectories(backupDirectory);
            Path file = backupDirectory.resolve("smartparking-backup-" + now.toString().replace(':', '-') + ".json");
            String content = """
                    {
                      "system": "Parking Payment System",
                      "backupType": "operational-checkpoint",
                      "createdAt": "%s",
                      "note": "This checkpoint records backup readiness metadata. Use SQL Server/MySQL tools for full production database dumps."
                    }
                    """
                    .formatted(now);
            Files.writeString(file, content);
            lastBackup = new BackupResponse("SUCCESS", file.toAbsolutePath().toString(), now,
                    "Backup checkpoint created");
            if (user != null) {
                auditLogService.record(user, action, "SystemBackup", null);
            }
            return lastBackup;
        } catch (IOException exception) {
            lastBackup = new BackupResponse("FAILED", null, now, exception.getMessage());
            return lastBackup;
        }
    }
}
