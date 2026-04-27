package com.example.callcenter.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DbSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        tryAlter("request", "description", "TEXT");
        tryAlter("request", "note", "TEXT");
        repairOrphanReportLinks();
    }

    /**
     * Historical bug: ReportService only set report.request (inverse side) without setting
     * request.report (owning side), so request.report_id was never written. For each orphan
     * report, link the FIRST matching request (by title) — respecting the unique FK.
     */
    private void repairOrphanReportLinks() {
        try {
            // Find reports nobody currently points to
            List<java.util.Map<String, Object>> orphans = jdbc.queryForList(
                    "SELECT rep.id AS report_id, rep.request_title AS title " +
                            "FROM report rep " +
                            "WHERE rep.request_title IS NOT NULL " +
                            "  AND rep.id NOT IN (SELECT report_id FROM request WHERE report_id IS NOT NULL)"
            );
            int repaired = 0;
            for (java.util.Map<String, Object> row : orphans) {
                Long reportId = ((Number) row.get("report_id")).longValue();
                String title = (String) row.get("title");
                try {
                    int updated = jdbc.update(
                            "UPDATE request SET report_id = ? " +
                                    "WHERE title = ? AND report_id IS NULL LIMIT 1",
                            reportId, title);
                    if (updated > 0) repaired++;
                } catch (Exception rowEx) {
                    // Skip rows that still conflict (e.g. another request already claimed this report elsewhere)
                    log.debug("Skipped report {} during repair: {}", reportId, rowEx.getMessage());
                }
            }
            if (repaired > 0) {
                log.info("Repaired {} orphan report→request links", repaired);
            }
        } catch (Exception e) {
            log.warn("Could not repair orphan report links: {}", e.getMessage());
        }
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
