package com.redditclone.comments.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Hibernate cannot describe a PostgreSQL expression/GIN index.  Create it
 * idempotently after Hibernate has created or updated the comments table.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommentSearchIndexInitializer {

    static final String CREATE_INDEX_SQL = """
            CREATE INDEX IF NOT EXISTS idx_comments_body_search
            ON comments USING GIN (to_tsvector('english', COALESCE(body, '')))
            """;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void createPostgresSearchIndex() {
        if (!isPostgres()) {
            return;
        }
        jdbcTemplate.execute(CREATE_INDEX_SQL);
        log.info("PostgreSQL full-text index for comments is ready");
    }

    private boolean isPostgres() {
        try (var connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to inspect the comment database", exception);
        }
    }
}
