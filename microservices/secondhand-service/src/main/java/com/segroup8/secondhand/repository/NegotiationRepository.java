package com.segroup8.secondhand.repository;

import com.segroup8.secondhand.domain.ProductNegotiation;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class NegotiationRepository {
    private static final RowMapper<ProductNegotiation> MAPPER = NegotiationRepository::map;
    private final NamedParameterJdbcTemplate db;

    public NegotiationRepository(NamedParameterJdbcTemplate db) {
        this.db = db;
    }

    public long insert(long productId, long buyerId, long sellerId, BigDecimal price) {
        var keyHolder = new GeneratedKeyHolder();
        db.update("insert into product_negotiation(product_id,buyer_user_id,seller_user_id,proposed_price,status) "
                        + "values(:product,:buyer,:seller,:price,'PENDING')",
                new MapSqlParameterSource().addValue("product", productId).addValue("buyer", buyerId)
                        .addValue("seller", sellerId).addValue("price", price), keyHolder, new String[] {"id"});
        return keyHolder.getKey().longValue();
    }

    public Optional<ProductNegotiation> findById(long id) {
        return db.query("select * from product_negotiation where id=:id", Map.of("id", id), MAPPER)
                .stream().findFirst();
    }

    public Optional<ProductNegotiation> findActive(long productId, long buyerId) {
        return db.query("select * from product_negotiation where product_id=:product and buyer_user_id=:buyer "
                        + "and status in ('PENDING','ACCEPTING','ACCEPTED') order by id desc limit 1",
                Map.of("product", productId, "buyer", buyerId), MAPPER).stream().findFirst();
    }

    public Optional<ProductNegotiation> findEffective(long productId, long buyerId) {
        return db.query("select * from product_negotiation where product_id=:product and buyer_user_id=:buyer "
                        + "and status='ACCEPTED' and used_order_id is null and effective_from<=CURRENT_TIMESTAMP "
                        + "and effective_until>=CURRENT_TIMESTAMP order by effective_until desc limit 1",
                Map.of("product", productId, "buyer", buyerId), MAPPER).stream().findFirst();
    }

    public int beginAccepting(long id, long sellerId, int version, BigDecimal confirmedPrice,
            LocalDateTime effectiveUntil) {
        return db.update("update product_negotiation set status='ACCEPTING',confirmed_price=:price,"
                        + "effective_from=CURRENT_TIMESTAMP,effective_until=:until,version=version+1,"
                        + "update_time=CURRENT_TIMESTAMP where id=:id and seller_user_id=:seller "
                        + "and status='PENDING' and version=:version",
                new MapSqlParameterSource().addValue("price", confirmedPrice).addValue("until", effectiveUntil)
                        .addValue("id", id).addValue("seller", sellerId).addValue("version", version));
    }

    public int reject(long id, long sellerId, int version) {
        return db.update("update product_negotiation set status='REJECTED',version=version+1,update_time=CURRENT_TIMESTAMP "
                        + "where id=:id and seller_user_id=:seller and status='PENDING' and version=:version",
                Map.of("id", id, "seller", sellerId, "version", version));
    }

    public int markAccepted(long id, long orderId) {
        return db.update("update product_negotiation set status='ACCEPTED',used_order_id=:orderId,"
                        + "version=version+1,update_time=CURRENT_TIMESTAMP where id=:id "
                        + "and status in ('ACCEPTING','ACCEPTED') and (used_order_id is null or used_order_id=:orderId)",
                Map.of("id", id, "orderId", orderId));
    }

    public int releaseFailed(long id) {
        return db.update("update product_negotiation set status='FAILED',version=version+1,update_time=CURRENT_TIMESTAMP "
                        + "where id=:id and status='ACCEPTING'", Map.of("id", id));
    }

    public NegotiationPage listForUser(long userId, long pageNum, long pageSize, Long productId,
            Long counterpartUserId, String status) {
        StringBuilder where = new StringBuilder(" where (buyer_user_id=:user or seller_user_id=:user)");
        Map<String, Object> params = new HashMap<>();
        params.put("user", userId);
        if (productId != null) {
            where.append(" and product_id=:product");
            params.put("product", productId);
        }
        if (counterpartUserId != null) {
            where.append(" and ((buyer_user_id=:user and seller_user_id=:counterpart) "
                    + "or (seller_user_id=:user and buyer_user_id=:counterpart))");
            params.put("counterpart", counterpartUserId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" and status=:status");
            params.put("status", status.toUpperCase());
        }
        Long total = db.queryForObject("select count(*) from product_negotiation" + where, params, Long.class);
        params.put("limit", pageSize);
        params.put("offset", (pageNum - 1) * pageSize);
        List<ProductNegotiation> records = db.query("select * from product_negotiation" + where
                + " order by create_time desc,id desc limit :limit offset :offset", params, MAPPER);
        return new NegotiationPage(total == null ? 0 : total, records);
    }

    private static ProductNegotiation map(ResultSet rs, int row) throws SQLException {
        return new ProductNegotiation(rs.getLong("id"), rs.getLong("product_id"),
                rs.getLong("buyer_user_id"), rs.getLong("seller_user_id"), nullableLong(rs, "conversation_id"),
                rs.getBigDecimal("proposed_price"), rs.getBigDecimal("confirmed_price"), rs.getString("status"),
                time(rs, "effective_from"), time(rs, "effective_until"), nullableLong(rs, "used_order_id"),
                rs.getInt("version"), time(rs, "create_time"), time(rs, "update_time"));
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    public record NegotiationPage(long total, List<ProductNegotiation> records) {
    }
}
