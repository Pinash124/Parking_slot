package com.example.pricing_calculation.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaCompatibilityInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaCompatibilityInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        allowGeneralFeedbackWithoutParkingSession();
        allowGeneralIncidentWithoutParkingSession();
    }

    private void allowGeneralFeedbackWithoutParkingSession() {
        allowNullableSession("feedbacks", "Feedbacks");
    }

    private void allowGeneralIncidentWithoutParkingSession() {
        allowNullableSession("incident_reports", "IncidentReports");
    }

    private void allowNullableSession(String lowerCaseTable, String mixedCaseTable) {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String database = metaData.getDatabaseProductName().toLowerCase();
            if (database.contains("postgres")) {
                jdbcTemplate.execute("ALTER TABLE " + lowerCaseTable + " ALTER COLUMN session_id DROP NOT NULL");
            } else if (database.contains("mysql")) {
                jdbcTemplate.execute("ALTER TABLE " + mixedCaseTable + " MODIFY session_id BIGINT NULL");
            } else if (database.contains("microsoft") || database.contains("sql server")) {
                jdbcTemplate.execute("ALTER TABLE " + mixedCaseTable + " ALTER COLUMN session_id BIGINT NULL");
            } else if (database.contains("h2")) {
                jdbcTemplate.execute("ALTER TABLE " + mixedCaseTable + " ALTER COLUMN session_id BIGINT NULL");
            }
        } catch (Exception ignored) {
            // Best-effort compatibility migration. A fresh schema or already-nullable column needs no action.
        }
    }
}