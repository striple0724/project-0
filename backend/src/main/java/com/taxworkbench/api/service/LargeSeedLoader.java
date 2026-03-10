package com.taxworkbench.api.service;

import com.taxworkbench.api.config.AppSettings;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LargeSeedLoader {
    private static final Logger log = LoggerFactory.getLogger(LargeSeedLoader.class);
    private static final int CLIENT_ID_BASE = 100000;
    private static final int WORK_ITEM_ID_BASE = 200000;

    private final JdbcTemplate jdbcTemplate;
    private final AppSettings appSettings;
    private final ExecutorService seedExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "large-seed-loader");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<SeedState> state = new AtomicReference<>(SeedState.PENDING);

    private volatile String message = "pending";
    private volatile OffsetDateTime startedAt;
    private volatile OffsetDateTime completedAt;
    private volatile int seededClients;
    private volatile int seededWorkItems;
    private volatile int seededAudits;

    public LargeSeedLoader(JdbcTemplate jdbcTemplate, AppSettings appSettings) {
        this.jdbcTemplate = jdbcTemplate;
        this.appSettings = appSettings;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!appSettings.getSeed().getLarge().isEnabled()) {
            state.set(SeedState.DISABLED);
            message = "auto-start disabled by app.seed.large.enabled=false";
            log.info("Large seed auto-start is disabled.");
            return;
        }
        trigger("application-ready");
    }

    public boolean triggerManual() {
        return trigger("manual-api");
    }

    public boolean runSync(String triggerBy) {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        runSeed(triggerBy);
        return true;
    }

    public SeedStatus getStatus() {
        return new SeedStatus(
                state.get().name(),
                message,
                startedAt,
                completedAt,
                seededClients,
                seededWorkItems,
                seededAudits
        );
    }

    @PreDestroy
    void shutdown() {
        seedExecutor.shutdownNow();
    }

    private boolean trigger(String triggerBy) {
        if (!running.compareAndSet(false, true)) {
            return false;
        }

        state.set(SeedState.PENDING);
        message = "queued";
        seedExecutor.submit(() -> runSeed(triggerBy));
        return true;
    }

    private void runSeed(String triggerBy) {
        state.set(SeedState.RUNNING);
        startedAt = OffsetDateTime.now();
        completedAt = null;
        message = "running";
        seededClients = 0;
        seededWorkItems = 0;
        seededAudits = 0;

        int clientCount = Math.max(1, appSettings.getSeed().getLarge().getClientCount());
        int workItemCount = Math.max(1, appSettings.getSeed().getLarge().getWorkItemCount());
        int clientIdEnd = CLIENT_ID_BASE + clientCount;
        int workItemIdEnd = WORK_ITEM_ID_BASE + workItemCount;

        long beginNanos = System.nanoTime();
        log.info("Large seed started. trigger={}, clientCount={}, workItemCount={}", triggerBy, clientCount, workItemCount);

        try {
            jdbcTemplate.update("DELETE FROM work_item_audits WHERE changed_by LIKE 'seed-bot%'");
            jdbcTemplate.update(
                    "DELETE FROM work_items WHERE id BETWEEN ? AND ? OR memo LIKE '[seed]%'",
                    WORK_ITEM_ID_BASE + 1,
                    workItemIdEnd
            );
            jdbcTemplate.update(
                    "DELETE FROM clients WHERE id BETWEEN ? AND ? OR name LIKE 'GEN_CLIENT_%'",
                    CLIENT_ID_BASE + 1,
                    clientIdEnd
            );

            seededClients = jdbcTemplate.update(buildClientSeedSql(clientCount));
            seededWorkItems = jdbcTemplate.update(buildWorkItemSeedSql(clientCount, workItemCount, clientIdEnd));

            int auditStatusCount = jdbcTemplate.update(buildAuditStatusSql(workItemIdEnd));
            int auditDueDateCount = jdbcTemplate.update(buildAuditDueDateSql(workItemIdEnd));
            int auditAssigneeCount = jdbcTemplate.update(buildAuditAssigneeSql(workItemIdEnd));
            seededAudits = auditStatusCount + auditDueDateCount + auditAssigneeCount;

            completedAt = OffsetDateTime.now();
            double elapsedSec = (System.nanoTime() - beginNanos) / 1_000_000_000.0;
            message = String.format(
                    Locale.ROOT,
                    "completed in %.2fs (clients=%d, work_items=%d, audits=%d)",
                    elapsedSec,
                    seededClients,
                    seededWorkItems,
                    seededAudits
            );
            state.set(SeedState.COMPLETED);
            log.info("Large seed completed. {}", message);
        } catch (Exception ex) {
            completedAt = OffsetDateTime.now();
            message = "failed: " + ex.getMessage();
            state.set(SeedState.FAILED);
            log.error("Large seed failed.", ex);
        } finally {
            running.set(false);
        }
    }

    private String buildClientSeedSql(int clientCount) {
        return """
                MERGE INTO clients (id, name, biz_no, type, status, tier)
                KEY(id)
                SELECT
                    %d + seq AS id,
                    CONCAT('GEN_CLIENT_', LPAD(CAST(seq AS VARCHAR), 5, '0')) AS name,
                    CONCAT(
                        LPAD(CAST(MOD(seq, 1000) AS VARCHAR), 3, '0'),
                        '-',
                        LPAD(CAST(MOD(seq * 37, 100) AS VARCHAR), 2, '0'),
                        '-',
                        LPAD(CAST(seq AS VARCHAR), 5, '0')
                    ) AS biz_no,
                    CASE WHEN MOD(seq, 5) = 0 THEN 'INDIVIDUAL' ELSE 'CORPORATE' END AS type,
                    CASE WHEN MOD(seq, 20) = 0 THEN 'INACTIVE' ELSE 'ACTIVE' END AS status,
                    CASE
                        WHEN MOD(seq, 10) = 0 THEN 'VIP'
                        WHEN MOD(seq, 10) IN (1, 2, 3) THEN 'PREMIUM'
                        ELSE 'BASIC'
                    END AS tier
                FROM SYSTEM_RANGE(1, %d) AS r(seq)
                """.formatted(CLIENT_ID_BASE, clientCount);
    }

    private String buildWorkItemSeedSql(int clientCount, int workItemCount, int clientIdEnd) {
        int firstYearCount = Math.max(1, workItemCount / 2);
        return """
                MERGE INTO work_items (id, client_id, biz_no, type, status, assignee, due_date, tags_json, memo, version, updated_at)
                KEY(id)
                SELECT
                    %d + n.seq AS id,
                    cp.id AS client_id,
                    cp.biz_no,
                    CASE MOD(n.seq, 4)
                        WHEN 0 THEN 'FILING'
                        WHEN 1 THEN 'BOOKKEEPING'
                        WHEN 2 THEN 'REVIEW'
                        ELSE 'ETC'
                    END AS type,
                    CASE
                        WHEN MOD(n.seq, 17) = 0 THEN 'HOLD'
                        WHEN MOD(n.seq, 5) = 0 THEN 'DONE'
                        WHEN MOD(n.seq, 3) = 0 THEN 'IN_PROGRESS'
                        ELSE 'TODO'
                    END AS status,
                    CONCAT('assignee_', LPAD(CAST(MOD(n.seq, 300) + 1 AS VARCHAR), 3, '0')) AS assignee,
                    CASE
                        WHEN n.seq <= %d THEN DATEADD('DAY', MOD(n.seq - 1, 366), DATE '2024-01-01')
                        ELSE DATEADD('DAY', MOD(n.seq - %d - 1, 365), DATE '2025-01-01')
                    END AS due_date,
                    CASE
                        WHEN MOD(n.seq, 11) = 0 THEN '["vip","priority"]'
                        WHEN MOD(n.seq, 5) = 0 THEN '["monthly","review"]'
                        WHEN MOD(n.seq, 2) = 0 THEN '["vat"]'
                        ELSE '["general"]'
                    END AS tags_json,
                    CONCAT('[seed] work item #', LPAD(CAST(n.seq AS VARCHAR), 6, '0'), ' for ', cp.name) AS memo,
                    0 AS version,
                    CASE
                        WHEN n.seq <= %d THEN DATEADD('MINUTE', MOD(n.seq - 1, 1440), TIMESTAMP '2024-01-01 09:00:00')
                        ELSE DATEADD('MINUTE', MOD(n.seq - %d - 1, 1440), TIMESTAMP '2025-01-01 09:00:00')
                    END AS updated_at
                FROM SYSTEM_RANGE(1, %d) AS n(seq)
                JOIN (
                    SELECT id, biz_no, name, ROW_NUMBER() OVER (ORDER BY id) AS rn
                    FROM clients
                    WHERE id BETWEEN %d AND %d
                ) cp
                ON cp.rn = MOD(n.seq - 1, %d) + 1
                """.formatted(
                WORK_ITEM_ID_BASE,
                firstYearCount,
                firstYearCount,
                firstYearCount,
                firstYearCount,
                workItemCount,
                CLIENT_ID_BASE + 1,
                clientIdEnd,
                clientCount
        );
    }

    private String buildAuditStatusSql(int workItemIdEnd) {
        return """
                INSERT INTO work_item_audits (work_item_id, field_name, before_value, after_value, changed_by, changed_at)
                SELECT
                    wi.id AS work_item_id,
                    'status' AS field_name,
                    CASE wi.status
                        WHEN 'TODO' THEN 'IN_PROGRESS'
                        WHEN 'IN_PROGRESS' THEN 'TODO'
                        WHEN 'DONE' THEN 'IN_PROGRESS'
                        ELSE 'TODO'
                    END AS before_value,
                    wi.status AS after_value,
                    CONCAT('seed-bot-', LPAD(CAST(MOD(wi.id, 50) + 1 AS VARCHAR), 2, '0')) AS changed_by,
                    DATEADD('HOUR', -MOD(wi.id, 240), wi.updated_at) AS changed_at
                FROM work_items wi
                WHERE wi.id BETWEEN %d AND %d
                """.formatted(WORK_ITEM_ID_BASE + 1, workItemIdEnd);
    }

    private String buildAuditDueDateSql(int workItemIdEnd) {
        return """
                INSERT INTO work_item_audits (work_item_id, field_name, before_value, after_value, changed_by, changed_at)
                SELECT
                    wi.id AS work_item_id,
                    'dueDate' AS field_name,
                    CAST(DATEADD('DAY', -7, wi.due_date) AS VARCHAR) AS before_value,
                    CAST(wi.due_date AS VARCHAR) AS after_value,
                    CONCAT('seed-bot-', LPAD(CAST(MOD(wi.id, 50) + 1 AS VARCHAR), 2, '0')) AS changed_by,
                    DATEADD('DAY', -1, wi.updated_at) AS changed_at
                FROM work_items wi
                WHERE wi.id BETWEEN %d AND %d
                  AND MOD(wi.id, 3) = 0
                """.formatted(WORK_ITEM_ID_BASE + 1, workItemIdEnd);
    }

    private String buildAuditAssigneeSql(int workItemIdEnd) {
        return """
                INSERT INTO work_item_audits (work_item_id, field_name, before_value, after_value, changed_by, changed_at)
                SELECT
                    wi.id AS work_item_id,
                    'assignee' AS field_name,
                    CONCAT('assignee_', LPAD(CAST(MOD(wi.id + 13, 300) + 1 AS VARCHAR), 3, '0')) AS before_value,
                    wi.assignee AS after_value,
                    CONCAT('seed-bot-', LPAD(CAST(MOD(wi.id, 50) + 1 AS VARCHAR), 2, '0')) AS changed_by,
                    DATEADD('HOUR', -2, wi.updated_at) AS changed_at
                FROM work_items wi
                WHERE wi.id BETWEEN %d AND %d
                  AND MOD(wi.id, 5) = 0
                """.formatted(WORK_ITEM_ID_BASE + 1, workItemIdEnd);
    }

    private enum SeedState {
        DISABLED,
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    @io.swagger.v3.oas.annotations.media.Schema(description = "대량 적재 상태 정보")
    public record SeedStatus(
            @io.swagger.v3.oas.annotations.media.Schema(description = "진행 상태", example = "COMPLETED")
            String state,
            @io.swagger.v3.oas.annotations.media.Schema(description = "상세 메시지", example = "completed in 657.00s")
            String message,
            @io.swagger.v3.oas.annotations.media.Schema(description = "시작 시각")
            OffsetDateTime startedAt,
            @io.swagger.v3.oas.annotations.media.Schema(description = "종료 시각")
            OffsetDateTime completedAt,
            @io.swagger.v3.oas.annotations.media.Schema(description = "적재된 고객사 수", example = "10000")
            int clientCount,
            @io.swagger.v3.oas.annotations.media.Schema(description = "적재된 업무 수", example = "200000")
            int workItemCount,
            @io.swagger.v3.oas.annotations.media.Schema(description = "적재된 이력 수", example = "300000")
            int auditCount
    ) {
    }
}
