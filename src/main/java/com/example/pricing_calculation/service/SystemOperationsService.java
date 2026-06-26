package com.example.pricing_calculation.service;

import com.example.pricing_calculation.dto.BackupResponse;
import com.example.pricing_calculation.dto.RecoveryStatusResponse;
import com.example.pricing_calculation.dto.SystemOperationalStatusResponse;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemOperationsService {

    private final DataSource dataSource;
    private final BackupService backupService;
    private final int responseSlaMs;
    private final int supportedConcurrentLanes;
    private final List<String> laneCodes;

    public SystemOperationsService(
            DataSource dataSource,
            BackupService backupService,
            @Value("${parking.sla.response-ms:3000}") int responseSlaMs,
            @Value("${parking.lanes.supported:4}") int supportedConcurrentLanes,
            @Value("${parking.lanes.codes:LANE-IN-1,LANE-IN-2,LANE-OUT-1,LANE-OUT-2}") String laneCodes) {
        this.dataSource = dataSource;
        this.backupService = backupService;
        this.responseSlaMs = responseSlaMs;
        this.supportedConcurrentLanes = supportedConcurrentLanes;
        this.laneCodes = Arrays.stream(laneCodes.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    public SystemOperationalStatusResponse status() {
        return new SystemOperationalStatusResponse(
                responseSlaMs,
                true,
                true,
                supportedConcurrentLanes,
                true,
                true,
                true,
                true,
                laneCodes,
                LocalDateTime.now()
        );
    }

    public RecoveryStatusResponse recoveryStatus() {
        boolean available = false;
        String message = "Database connection is not available";
        try (Connection connection = dataSource.getConnection()) {
            available = connection.isValid(2);
            message = available ? "Database connection is healthy; HikariCP auto-reconnect/retry is available"
                    : "Database connection validation failed";
        } catch (Exception exception) {
            message = exception.getMessage();
        }
        BackupResponse lastBackup = backupService.lastBackup();
        return new RecoveryStatusResponse(
                available ? "UP" : "DOWN",
                available,
                true,
                lastBackup.backupFile(),
                LocalDateTime.now(),
                message
        );
    }
}
