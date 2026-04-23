package com.example.callcenter.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DbSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        tryAlter("request", "description", "TEXT");
        tryAlter("request", "note", "TEXT");
    }

    private void tryAlter(String table, String column, String newType) {
        try {
            String currentType = jdbc.queryForObject(
                    "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    String.class, table, column);
            if (currentType.equalsIgnoreCase(newType)) return;
            jdbc.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " " + newType);
            log.info("Migrated {}.{} from {} to {}", table, column, currentType, newType);
        } catch (Exception e) {
            log.warn("Could not migrate {}.{}: {}", table, column, e.getMessage());
        }
    }
}
