package com.segroup8.catalogshop;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Tag("DOMAIN_B")
class MySqlSchemaOwnershipIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6")
        .withDatabaseName("catalog_shop_db")
        .withUsername("catalog_shop_app")
        .withPassword("catalog_shop_app");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void applicationAccountCannotReadIdentitySchema() throws Exception {
        try (var root = DriverManager.getConnection(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword());
             var statement = root.createStatement()) {
            statement.execute("create database if not exists identity_governance_db");
            statement.execute("create table if not exists identity_governance_db.user(id bigint primary key)");
        }
        try (var app = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = app.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeQuery("select * from identity_governance_db.user"));
        }
    }
}
