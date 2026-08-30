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
    private static final String TEST_DB = "messaging_v1_test";
    private static final String FOREIGN_DB = "messaging_v1_foreign_test";

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
            statement.execute("create user if not exists 'messaging_v1_test_app'@'%' identified by 'messaging_v1_test_password'");
            statement.execute("grant all privileges on " + TEST_DB + ".* to 'messaging_v1_test_app'@'%'");
        }
        String appUrl = "jdbc:mysql://127.0.0.1:3306/" + TEST_DB + "?useSSL=false&allowPublicKeyRetrieval=true";
        try {
            Flyway.configure().dataSource(appUrl, "messaging_v1_test_app", "messaging_v1_test_password")
                    .locations("classpath:db/migration").load().migrate();
            try (Connection app = DriverManager.getConnection(appUrl, "messaging_v1_test_app", "messaging_v1_test_password")) {
                Set<String> tables;
                try (var rs = app.getMetaData().getTables(TEST_DB, null, "%", new String[]{"TABLE"})) {
                    var names = new java.util.HashSet<String>();
                    while (rs.next()) names.add(rs.getString("TABLE_NAME").toLowerCase());
                    tables = names.stream().collect(Collectors.toSet());
                }
                assertEquals(Set.of("chat_conversation", "chat_message", "notification",
                        "user_access_projection", "user_block_projection"), tables.stream()
                        .filter(name -> !name.equals("flyway_schema_history")).collect(Collectors.toSet()));
                assertThrows(SQLException.class, () -> app.createStatement().executeQuery(
                        "select * from " + FOREIGN_DB + ".secret_fact"));
            }
        } finally {
            try (Connection root = DriverManager.getConnection(rootUrl, rootUser, rootPassword); var statement = root.createStatement()) {
                statement.execute("drop database if exists " + TEST_DB);
                statement.execute("drop database if exists " + FOREIGN_DB);
                statement.execute("drop user if exists 'messaging_v1_test_app'@'%'");
            }
        }
    }
}
