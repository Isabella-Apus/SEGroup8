package com.segroup8.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

class MySqlMigrationTest {
    private static final String TEST_DB = "messaging_v2_test";
    private static final String FOREIGN_DB = "messaging_v2_foreign_test";
    private static final String APP_USER = "messaging_v2_test_app";
    private static final String APP_PASSWORD = "messaging_v2_test_password";

    @Test
    @EnabledIfSystemProperty(named = "messaging.mysql.test", matches = "true")
    void migrationRunsOnRealMysqlAndAccountCannotReadForeignSchema() throws Exception {
        String rootUrl = System.getProperty("messaging.mysql.root-url", "jdbc:mysql://127.0.0.1:3306/?useSSL=false&allowPublicKeyRetrieval=true");
        String rootUser = System.getProperty("messaging.mysql.root-user", "root");
        String rootPassword = System.getProperty("messaging.mysql.root-password", "root");
        try (Connection root = DriverManager.getConnection(rootUrl, rootUser, rootPassword); var statement = root.createStatement()) {
            statement.execute("create database if not exists " + TEST_DB);
            statement.execute("create database if not exists " + FOREIGN_DB);
            statement.execute("create table if not exists " + FOREIGN_DB + ".secret_fact(id bigint primary key)");
            statement.execute("create user if not exists '" + APP_USER + "'@'%' identified by '" + APP_PASSWORD + "'");
            statement.execute("grant all privileges on " + TEST_DB + ".* to '" + APP_USER + "'@'%'");
        }
        String appUrl = jdbcUrlForDatabase(rootUrl, TEST_DB);
        try {
            Flyway.configure().dataSource(appUrl, APP_USER, APP_PASSWORD)
                    .locations("classpath:db/migration").load().migrate();
            try (Connection app = DriverManager.getConnection(appUrl, APP_USER, APP_PASSWORD)) {
                Set<String> tables;
                try (var rs = app.getMetaData().getTables(TEST_DB, null, "%", new String[]{"TABLE"})) {
                    var names = new java.util.HashSet<String>();
                    while (rs.next()) names.add(rs.getString("TABLE_NAME").toLowerCase());
                    tables = names.stream().collect(Collectors.toSet());
                }
                assertEquals(Set.of("chat_conversation", "chat_message", "notification",
                        "user_access_projection", "user_block_projection", "inbox_event",
                        "idempotency_record", "outbox_event"), tables.stream()
                        .filter(name -> !name.equals("flyway_schema_history")).collect(Collectors.toSet()));
                verifyV2ReliabilityPersistence(app);
                assertThrows(SQLException.class, () -> app.createStatement().executeQuery(
                        "select * from " + FOREIGN_DB + ".secret_fact"));
            }
        } finally {
            try (Connection root = DriverManager.getConnection(rootUrl, rootUser, rootPassword); var statement = root.createStatement()) {
                statement.execute("drop database if exists " + TEST_DB);
                statement.execute("drop database if exists " + FOREIGN_DB);
                statement.execute("drop user if exists '" + APP_USER + "'@'%'");
            }
        }
    }

    private void verifyV2ReliabilityPersistence(Connection app) throws SQLException {
        try (var statement = app.createStatement()) {
            statement.executeUpdate("insert into inbox_event(event_id,event_type,payload,status,retry_count,"
                    + "next_retry_at,trace_id) values ('mysql-event-1','NotificationRequested.v1','{}',"
                    + "'RETRY',1,current_timestamp,'mysql-trace')");
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "insert into inbox_event(event_id,event_type,payload,trace_id) values "
                            + "('mysql-event-1','NotificationRequested.v1','{}','duplicate')"));
            statement.executeUpdate("update inbox_event set status='DLQ', retry_count=5, "
                    + "last_error='verified failure' where event_id='mysql-event-1'");
            assertEquals("DLQ", queryString(app,
                    "select status from inbox_event where event_id='mysql-event-1'"));

            statement.executeUpdate("insert into idempotency_record(dedupe_key,service_identity,request_hash) "
                    + "values ('mysql-dedupe-1','producer-test','hash-1')");
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "insert into idempotency_record(dedupe_key,service_identity,request_hash) "
                            + "values ('mysql-dedupe-1','producer-test','hash-1')"));

            statement.executeUpdate("insert into outbox_event(event_id,source_event_id,dedupe_key,delivery_kind,"
                    + "recipient_user_id,event_type,payload,trace_id,status,retry_count,next_attempt_at) values "
                    + "('mysql-delivery-1','mysql-event-1','mysql-delivery-dedupe','WEBSOCKET',1,"
                    + "'NOTIFICATION','{}','mysql-trace','PENDING',0,current_timestamp)");
            statement.executeUpdate("update outbox_event set status='DELIVERED', delivered_at=current_timestamp "
                    + "where event_id='mysql-delivery-1'");
            assertEquals("DELIVERED", queryString(app,
                    "select status from outbox_event where event_id='mysql-delivery-1'"));
        }
    }

    private String queryString(Connection app, String sql) throws SQLException {
        try (var statement = app.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private String jdbcUrlForDatabase(String rootUrl, String database) {
        int schemeEnd = rootUrl.indexOf("://");
        int pathStart = schemeEnd < 0 ? -1 : rootUrl.indexOf('/', schemeEnd + 3);
        String authority = pathStart < 0 ? rootUrl : rootUrl.substring(0, pathStart);
        return authority + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true";
    }
}
