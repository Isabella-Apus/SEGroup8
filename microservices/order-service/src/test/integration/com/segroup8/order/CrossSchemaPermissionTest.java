package com.segroup8.order;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@Tag("MYSQL_INTEGRATION")
class CrossSchemaPermissionTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6")
            .withDatabaseName("order_db").withUsername("root").withPassword("rootpass");

    @Test void orderAccountCannotReadFinanceBalance() throws Exception {
        try (var root=DriverManager.getConnection(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword());var s=root.createStatement()) {
            s.execute("create database benefits_finance_db");
            s.execute("create table benefits_finance_db.balance(id bigint primary key, amount decimal(12,2))");
            s.execute("create user 'order_app'@'%' identified by 'orderpass'");
            s.execute("grant all on order_db.* to 'order_app'@'%'");
        }
        String url=MYSQL.getJdbcUrl().replace("order_db","order_db");
        try(var order=DriverManager.getConnection(url,"order_app","orderpass")) {
            assertThatThrownBy(()->order.createStatement().executeQuery("select * from benefits_finance_db.balance"))
                    .isInstanceOf(SQLException.class);
        }
    }
}
