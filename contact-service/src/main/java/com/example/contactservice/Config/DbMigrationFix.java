package com.example.contactservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DbMigrationFix implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        fixTagContactsTable();
    }

    private void fixTagContactsTable() {
        try {
            List<Map<String, Object>> cols = jdbc.queryForList(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tag_contacts'");
            List<String> colNames = cols.stream()
                    .map(c -> String.valueOf(c.get("COLUMN_NAME")).toLowerCase())
                    .toList();
            log.info("tag_contacts current columns: {}", colNames);

            // If the stale column 'contacts_idc' still exists, drop it
            if (colNames.contains("contacts_idc")) {
                log.info("Found stale 'contacts_idc' column, removing...");
                try {
                    // Remove any foreign key constraints on contacts_idc first
                    List<Map<String, Object>> fks = jdbc.queryForList(
                            "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag_contacts' AND COLUMN_NAME = 'contacts_idc'");
                    for (Map<String, Object> fk : fks) {
                        String fkName = String.valueOf(fk.get("CONSTRAINT_NAME"));
                        if (!"PRIMARY".equalsIgnoreCase(fkName)) {
                            try {
                                jdbc.execute("ALTER TABLE tag_contacts DROP FOREIGN KEY " + fkName);
                                log.info("Dropped FK: {}", fkName);
                            } catch (Exception ignored) {}
                        }
                    }
                    // Drop primary key if it references contacts_idc
                    try {
                        jdbc.execute("ALTER TABLE tag_contacts DROP PRIMARY KEY");
                    } catch (Exception ignored) {}
                    // Drop the column
                    jdbc.execute("ALTER TABLE tag_contacts DROP COLUMN contacts_idc");
                    log.info("Dropped contacts_idc column");
                    // Add primary key on correct columns if not exists
                    if (colNames.contains("contacts_id_c")) {
                        try {
                            jdbc.execute("ALTER TABLE tag_contacts ADD PRIMARY KEY (contacts_id_c, tags_id)");
                        } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    log.warn("Could not clean up contacts_idc: {}", e.getMessage());
                }
            }
            
            // Fix PK: must be composite (contacts_id_c, tags_id), not just tags_id
            try {
                List<Map<String, Object>> pkCols = jdbc.queryForList(
                        "SELECT COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag_contacts' AND CONSTRAINT_NAME = 'PRIMARY'");
                List<String> pkColNames = pkCols.stream()
                        .map(c -> String.valueOf(c.get("COLUMN_NAME")).toLowerCase())
                        .toList();
                log.info("tag_contacts PK columns: {}", pkColNames);

                if (pkColNames.size() == 1) {
                    log.info("PK is single-column ({}), fixing to composite (contacts_id_c, tags_id)...", pkColNames);
                    // Drop all FK constraints first (they block PK drop)
                    List<Map<String, Object>> allFks = jdbc.queryForList(
                            "SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tag_contacts' AND CONSTRAINT_TYPE = 'FOREIGN KEY'");
                    for (Map<String, Object> fk : allFks) {
                        String fkName = String.valueOf(fk.get("CONSTRAINT_NAME"));
                        try {
                            jdbc.execute("ALTER TABLE tag_contacts DROP FOREIGN KEY " + fkName);
                            log.info("Dropped FK: {}", fkName);
                        } catch (Exception ignored) {}
                    }
                    jdbc.execute("ALTER TABLE tag_contacts DROP PRIMARY KEY");
                    jdbc.execute("ALTER TABLE tag_contacts ADD PRIMARY KEY (contacts_id_c, tags_id)");
                    // Re-add FK constraints
                    try {
                        jdbc.execute("ALTER TABLE tag_contacts ADD CONSTRAINT fk_tc_contact FOREIGN KEY (contacts_id_c) REFERENCES contact(id_c)");
                        jdbc.execute("ALTER TABLE tag_contacts ADD CONSTRAINT fk_tc_tag FOREIGN KEY (tags_id) REFERENCES tag(id)");
                    } catch (Exception e) {
                        log.warn("Could not re-add FKs: {}", e.getMessage());
                    }
                    log.info("Fixed PK to composite (contacts_id_c, tags_id)");
                }
            } catch (Exception e) {
                log.warn("Could not fix PK: {}", e.getMessage());
            }

            // Verify final column state
            List<Map<String, Object>> finalCols = jdbc.queryForList(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tag_contacts'");
            log.info("tag_contacts final columns: {}", finalCols.stream()
                    .map(c -> String.valueOf(c.get("COLUMN_NAME"))).toList());

        } catch (Exception e) {
            log.warn("tag_contacts fix skipped: {}", e.getMessage());
        }
    }
}
