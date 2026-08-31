package com.segroup8.secondhand.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@Tag("DOMAIN_D")
@Tag("MYSQL_INTEGRATION")
class MySqlSchemaOwnershipIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.6")
            .withDatabaseName("secondhand_db")
            .withUsername("secondhand_app")
            .withPassword("secondhand-test-password");

    @Test
    void migrationsCoverOwnedTradeTablesAndServiceAccountCannotInsertOrderDatabase() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration")
                .load().migrate();
        JdbcTemplate db = new JdbcTemplate(dataSource);

        assertThat(db.queryForObject("select count(*) from information_schema.tables "
                + "where table_schema='secondhand_db' and table_name in "
                + "('secondhand_product','product_negotiation','product_auction','auction_log',"
                + "'trade_order_request','idempotency_record','outbox_event')", Integer.class)).isEqualTo(7);
        assertThat(db.queryForObject("select count(*) from information_schema.tables "
                + "where table_schema='secondhand_db' and table_name='order_info'", Integer.class)).isZero();

        var rootSetup = MYSQL.execInContainer("sh", "-c",
                "mysql -uroot -p\"$MYSQL_ROOT_PASSWORD\" -e \"create database order_db; "
                        + "create table order_db.order_info(id bigint primary key);\"");
        assertThat(rootSetup.getExitCode()).withFailMessage(rootSetup.getStderr()).isZero();
        assertThatThrownBy(() -> db.update("insert into order_db.order_info(id) values(1)"))
                .isInstanceOf(DataAccessException.class);

        db.update("insert into secondhand_product(seller_user_id,name,images,origin_price,sale_price,category_id,"
                        + "sub_category_id,condition_level,is_negotiable,status,risk_status) "
                        + "values(10,'MySQL商品','[]',100,60,8,801,'九成新',1,1,'APPROVED')");
        Long productId = db.queryForObject("select id from secondhand_product where name='MySQL商品'", Long.class);

        db.update("insert into product_negotiation(product_id,buyer_user_id,seller_user_id,proposed_price,status) "
                + "values(?,20,10,50,'PENDING')", productId);
        db.update("insert into product_auction(product_id,seller_user_id,start_price,increment_amount,current_price,"
                        + "start_time,end_time,status) values(?,10,40,5,40,current_timestamp,"
                        + "date_add(current_timestamp, interval 1 hour),'ONGOING')", productId);
        Long auctionId = db.queryForObject("select id from product_auction where product_id=?", Long.class, productId);
        db.update("insert into auction_log(auction_id,product_id,bidder_user_id,bid_amount) values(?,?,20,40)",
                auctionId, productId);
        db.update("insert into trade_order_request(trade_type,trade_id,order_business_key,product_id,buyer_user_id,"
                        + "seller_user_id,price,request_status) values('DIRECT_BUY','mysql-1',"
                        + "'SECONDHAND:DIRECT_BUY:mysql-1',?,20,10,60,'PENDING')", productId);
        db.update("insert into idempotency_record(scope_name,idempotency_key,response_reference) "
                + "values('DIRECT_BUY','mysql-1','SECONDHAND:DIRECT_BUY:mysql-1')");
        db.update("insert into outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload) "
                + "values('mysql-event-1','SECONDHAND_PRODUCT',?,'ProductSubmitted.v1','{}')", productId);

        assertThat(db.queryForObject("select count(*) from secondhand_product", Integer.class)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from product_negotiation", Integer.class)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from product_auction", Integer.class)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from auction_log", Integer.class)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from trade_order_request", Integer.class)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from idempotency_record", Integer.class)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from outbox_event where event_status='NEW'", Integer.class))
                .isEqualTo(1);
    }
}
