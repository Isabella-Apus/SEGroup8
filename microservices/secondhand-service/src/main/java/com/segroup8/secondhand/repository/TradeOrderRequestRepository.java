package com.segroup8.secondhand.repository;

import com.segroup8.secondhand.domain.TradeOrderRequest;
import com.segroup8.secondhand.domain.OrderCreationSnapshot;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TradeOrderRequestRepository {
    private static final RowMapper<TradeOrderRequest> MAPPER = TradeOrderRequestRepository::map;
    private final NamedParameterJdbcTemplate db;

    public TradeOrderRequestRepository(NamedParameterJdbcTemplate db) {
        this.db = db;
    }

    public TradeOrderRequest createOrFind(String tradeType, String tradeId, String businessKey,
            long productId, long buyerId, long sellerId, BigDecimal price,
            OrderCreationSnapshot snapshot, String remark) {
        try {
            var keyHolder = new GeneratedKeyHolder();
            db.update("insert into trade_order_request(trade_type,trade_id,order_business_key,product_id,buyer_user_id,"
                            + "seller_user_id,price,address_id,product_name,receiver_name,receiver_phone,"
                            + "receiver_province,receiver_city,receiver_detail_address,remark,request_status,next_retry_at) "
                            + "values(:type,:tradeId,:businessKey,:product,:buyer,:seller,:price,:address,:productName,"
                            + ":receiverName,:receiverPhone,:receiverProvince,:receiverCity,:receiverDetailAddress,"
                            + ":remark,'PENDING',CURRENT_TIMESTAMP)",
                    new MapSqlParameterSource().addValue("type", tradeType).addValue("tradeId", tradeId)
                            .addValue("businessKey", businessKey).addValue("product", productId)
                            .addValue("buyer", buyerId).addValue("seller", sellerId).addValue("price", price)
                            .addValue("address", snapshot.addressId()).addValue("productName", snapshot.productName())
                            .addValue("receiverName", snapshot.receiverName())
                            .addValue("receiverPhone", snapshot.receiverPhone())
                            .addValue("receiverProvince", snapshot.receiverProvince())
                            .addValue("receiverCity", snapshot.receiverCity())
                            .addValue("receiverDetailAddress", snapshot.receiverDetailAddress())
                            .addValue("remark", remark),
                    keyHolder, new String[] {"id"});
            return findById(keyHolder.getKey().longValue()).orElseThrow();
        } catch (DuplicateKeyException duplicate) {
            return findByTrade(tradeType, tradeId).orElseThrow(() -> duplicate);
        }
    }

    public Optional<TradeOrderRequest> findById(long id) {
        return db.query("select * from trade_order_request where id=:id", Map.of("id", id), MAPPER)
                .stream().findFirst();
    }

    public Optional<TradeOrderRequest> findByTrade(String type, String tradeId) {
        return db.query("select * from trade_order_request where trade_type=:type and trade_id=:tradeId",
                Map.of("type", type, "tradeId", tradeId), MAPPER).stream().findFirst();
    }

    public Optional<TradeOrderRequest> findByBusinessKey(String key) {
        return db.query("select * from trade_order_request where order_business_key=:key",
                Map.of("key", key), MAPPER).stream().findFirst();
    }

    public Optional<TradeOrderRequest> findActiveForProduct(String type, long productId) {
        return db.query("select * from trade_order_request where trade_type=:type and product_id=:product "
                        + "and request_status in ('PENDING','RETRY','CREATED') order by id desc limit 1",
                Map.of("type", type, "product", productId), MAPPER).stream().findFirst();
    }

    public int markCreated(long id, long orderId, String orderNo, String orderStatus) {
        return db.update("update trade_order_request set request_status='CREATED',order_id=:orderId,order_no=:orderNo,"
                        + "order_status=:orderStatus,last_error=null,next_retry_at=null,version=version+1,"
                        + "update_time=CURRENT_TIMESTAMP where id=:id and request_status in ('PENDING','RETRY')",
                new MapSqlParameterSource().addValue("orderId", orderId).addValue("orderNo", orderNo)
                        .addValue("orderStatus", orderStatus).addValue("id", id));
    }

    public int markRetry(long id, String error, LocalDateTime nextRetryAt) {
        return db.update("update trade_order_request set request_status='RETRY',attempts=attempts+1,last_error=:error,"
                        + "next_retry_at=:retry,version=version+1,update_time=CURRENT_TIMESTAMP "
                        + "where id=:id and request_status in ('PENDING','RETRY')",
                new MapSqlParameterSource().addValue("error", truncate(error)).addValue("retry", nextRetryAt)
                        .addValue("id", id));
    }

    public int markFailed(long id, String error) {
        return db.update("update trade_order_request set request_status='FAILED',last_error=:error,next_retry_at=null,"
                        + "version=version+1,update_time=CURRENT_TIMESTAMP where id=:id and request_status in ('PENDING','RETRY')",
                Map.of("error", truncate(error), "id", id));
    }

    public int updateOrderStatus(long id, String orderStatus) {
        return db.update("update trade_order_request set order_status=:status,version=version+1,"
                        + "update_time=CURRENT_TIMESTAMP where id=:id and request_status='CREATED'",
                Map.of("status", orderStatus, "id", id));
    }

    public int markCancelled(long id, String orderStatus) {
        return db.update("update trade_order_request set request_status='CANCELLED',order_status=:status,"
                        + "version=version+1,update_time=CURRENT_TIMESTAMP where id=:id "
                        + "and request_status in ('CREATED','CANCELLED')",
                Map.of("status", orderStatus, "id", id));
    }

    public List<TradeOrderRequest> findRetryable(int limit) {
        return db.query("select * from trade_order_request where request_status in ('PENDING','RETRY') "
                        + "and (next_retry_at is null or next_retry_at<=CURRENT_TIMESTAMP) order by create_time asc limit :limit",
                Map.of("limit", limit), MAPPER);
    }

    private static TradeOrderRequest map(ResultSet rs, int row) throws SQLException {
        return new TradeOrderRequest(rs.getLong("id"), rs.getString("trade_type"), rs.getString("trade_id"),
                rs.getString("order_business_key"), rs.getLong("product_id"), rs.getLong("buyer_user_id"),
                rs.getLong("seller_user_id"), rs.getBigDecimal("price"), nullableLong(rs, "address_id"),
                rs.getString("product_name"), rs.getString("receiver_name"), rs.getString("receiver_phone"),
                rs.getString("receiver_province"), rs.getString("receiver_city"),
                rs.getString("receiver_detail_address"), rs.getString("remark"),
                rs.getString("request_status"), nullableLong(rs, "order_id"),
                rs.getString("order_no"), rs.getString("order_status"), rs.getInt("attempts"),
                rs.getString("last_error"), time(rs, "next_retry_at"), rs.getInt("version"),
                time(rs, "create_time"), time(rs, "update_time"));
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static String truncate(String value) {
        String safe = value == null ? "unknown downstream error" : value;
        return safe.substring(0, Math.min(500, safe.length()));
    }
}
