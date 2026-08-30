package com.segroup8.secondhand.repository;

import com.segroup8.secondhand.domain.AuctionBid;
import com.segroup8.secondhand.domain.ProductAuction;
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
public class AuctionRepository {
    private static final RowMapper<ProductAuction> AUCTION_MAPPER = AuctionRepository::mapAuction;
    private static final RowMapper<AuctionBid> BID_MAPPER = AuctionRepository::mapBid;
    private final NamedParameterJdbcTemplate db;

    public AuctionRepository(NamedParameterJdbcTemplate db) {
        this.db = db;
    }

    public long insert(long productId, long sellerId, BigDecimal startPrice, BigDecimal increment,
            LocalDateTime startTime, LocalDateTime endTime) {
        var keyHolder = new GeneratedKeyHolder();
        db.update("insert into product_auction(product_id,seller_user_id,start_price,increment_amount,current_price,"
                        + "start_time,end_time,status) values(:product,:seller,:start,:increment,:start,:startTime,:endTime,'ONGOING')",
                new MapSqlParameterSource().addValue("product", productId).addValue("seller", sellerId)
                        .addValue("start", startPrice).addValue("increment", increment)
                        .addValue("startTime", startTime).addValue("endTime", endTime),
                keyHolder, new String[] {"id"});
        return keyHolder.getKey().longValue();
    }

    public Optional<ProductAuction> findById(long id) {
        return db.query("select * from product_auction where id=:id", Map.of("id", id), AUCTION_MAPPER)
                .stream().findFirst();
    }

    public Optional<ProductAuction> findLatestByProduct(long productId) {
        return db.query("select * from product_auction where product_id=:product order by id desc limit 1",
                Map.of("product", productId), AUCTION_MAPPER).stream().findFirst();
    }

    public boolean hasActiveAuction(long productId) {
        Long count = db.queryForObject("select count(*) from product_auction where product_id=:product "
                        + "and status in ('ONGOING','SETTLING')",
                Map.of("product", productId), Long.class);
        return count != null && count > 0;
    }

    public int placeBid(ProductAuction auction, long bidderId, BigDecimal amount) {
        return db.update("update product_auction set current_price=:amount,current_bidder_user_id=:bidder,"
                        + "version=version+1,update_time=CURRENT_TIMESTAMP where id=:id and version=:version "
                        + "and status='ONGOING' and end_time>CURRENT_TIMESTAMP",
                Map.of("amount", amount, "bidder", bidderId, "id", auction.id(), "version", auction.version()));
    }

    public void insertBid(long auctionId, long productId, long bidderId, String bidderName, BigDecimal amount) {
        db.update("update auction_log set status='OUTBID' where auction_id=:auction and status='LEADING'",
                Map.of("auction", auctionId));
        db.update("insert into auction_log(auction_id,product_id,bidder_user_id,bidder_name_snapshot,bid_amount,status) "
                        + "values(:auction,:product,:bidder,:name,:amount,'LEADING')",
                new MapSqlParameterSource().addValue("auction", auctionId).addValue("product", productId)
                        .addValue("bidder", bidderId).addValue("name", bidderName).addValue("amount", amount));
    }

    public int beginSettlement(ProductAuction auction) {
        return db.update("update product_auction set status='SETTLING',version=version+1,update_time=CURRENT_TIMESTAMP "
                        + "where id=:id and version=:version and status='ONGOING'",
                Map.of("id", auction.id(), "version", auction.version()));
    }

    public int markFlow(ProductAuction auction) {
        return db.update("update product_auction set status='FLOW',version=version+1,update_time=CURRENT_TIMESTAMP "
                        + "where id=:id and version=:version and status='ONGOING'",
                Map.of("id", auction.id(), "version", auction.version()));
    }

    public int markFinished(long auctionId, long orderId) {
        return db.update("update product_auction set status='FINISHED',settled_order_id=:orderId,"
                        + "version=version+1,update_time=CURRENT_TIMESTAMP where id=:id "
                        + "and status in ('SETTLING','FINISHED') and (settled_order_id is null or settled_order_id=:orderId)",
                Map.of("id", auctionId, "orderId", orderId));
    }

    public int markFailedFlow(long auctionId) {
        return db.update("update product_auction set status='FLOW',version=version+1,update_time=CURRENT_TIMESTAMP "
                        + "where id=:id and status='SETTLING'", Map.of("id", auctionId));
    }

    public List<ProductAuction> findDue(int limit) {
        return db.query("select * from product_auction where status='ONGOING' and end_time<=CURRENT_TIMESTAMP "
                        + "order by end_time asc limit :limit", Map.of("limit", limit), AUCTION_MAPPER);
    }

    public AuctionPage listSeller(long sellerId, long pageNum, long pageSize, String status) {
        String where = " where seller_user_id=:seller";
        Map<String, Object> params = new HashMap<>();
        params.put("seller", sellerId);
        if (status != null && !status.isBlank()) {
            where += " and status=:status";
            params.put("status", status.toUpperCase());
        }
        Long total = db.queryForObject("select count(*) from product_auction" + where, params, Long.class);
        params.put("limit", pageSize);
        params.put("offset", (pageNum - 1) * pageSize);
        List<ProductAuction> records = db.query("select * from product_auction" + where
                + " order by create_time desc,id desc limit :limit offset :offset", params, AUCTION_MAPPER);
        return new AuctionPage(total == null ? 0 : total, records);
    }

    public List<AuctionBid> listBids(long auctionId) {
        return db.query("select * from auction_log where auction_id=:auction order by create_time desc,id desc limit 20",
                Map.of("auction", auctionId), BID_MAPPER);
    }

    public long countBids(long auctionId) {
        Long total = db.queryForObject("select count(*) from auction_log where auction_id=:auction",
                Map.of("auction", auctionId), Long.class);
        return total == null ? 0 : total;
    }

    private static ProductAuction mapAuction(ResultSet rs, int row) throws SQLException {
        return new ProductAuction(rs.getLong("id"), rs.getLong("product_id"), rs.getLong("seller_user_id"),
                rs.getBigDecimal("start_price"), rs.getBigDecimal("increment_amount"),
                rs.getBigDecimal("current_price"), nullableLong(rs, "current_bidder_user_id"),
                time(rs, "start_time"), time(rs, "end_time"), rs.getString("status"),
                nullableLong(rs, "settled_order_id"), rs.getInt("version"),
                time(rs, "create_time"), time(rs, "update_time"));
    }

    private static AuctionBid mapBid(ResultSet rs, int row) throws SQLException {
        return new AuctionBid(rs.getLong("id"), rs.getLong("auction_id"), rs.getLong("product_id"),
                rs.getLong("bidder_user_id"), rs.getString("bidder_name_snapshot"),
                rs.getBigDecimal("bid_amount"), rs.getString("status"), time(rs, "create_time"));
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    public record AuctionPage(long total, List<ProductAuction> records) {
    }
}
