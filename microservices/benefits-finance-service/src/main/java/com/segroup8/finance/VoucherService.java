package com.segroup8.finance;

import com.segroup8.finance.ApiModels.ClaimResult;
import com.segroup8.finance.ApiModels.QuoteRequest;
import com.segroup8.finance.ApiModels.QuoteResult;
import com.segroup8.finance.ApiModels.VoucherAction;
import com.segroup8.finance.ApiModels.VoucherActionResult;
import com.segroup8.finance.ApiModels.VoucherSave;
import com.segroup8.finance.ApiModels.VoucherView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class VoucherService {
    private static final Logger LOG = LoggerFactory.getLogger(VoucherService.class);
    private static final RowMapper<VoucherView> VOUCHER_MAPPER = VoucherService::mapVoucher;
    private final JdbcClient db;
    private final String currency;
    private final long quoteTtlSeconds;
    private final long reservationTtlSeconds;

    VoucherService(JdbcClient db, @Value("${app.currency}") String currency,
            @Value("${app.quote-ttl-seconds}") long quoteTtlSeconds,
            @Value("${app.reservation-ttl-seconds}") long reservationTtlSeconds) {
        this.db = db;
        this.currency = currency;
        this.quoteTtlSeconds = quoteTtlSeconds;
        this.reservationTtlSeconds = reservationTtlSeconds;
    }

    List<VoucherView> sellerList(long sellerId) {
        return db.sql("select * from voucher where issuer_type='SELLER' and issuer_user_id=:userId order by id desc")
                .param("userId", sellerId).query(VOUCHER_MAPPER).list();
    }

    List<VoucherView> adminList() {
        return db.sql("select * from voucher order by id desc").query(VOUCHER_MAPPER).list();
    }

    List<VoucherView> available() {
        Instant now = Instant.now();
        return db.sql("select * from voucher where status in ('ACTIVE','SCHEDULED') "
                        + "and start_time<=:now and end_time>:now and (grab_start_time is null or grab_start_time<=:now) "
                        + "and (grab_end_time is null or grab_end_time>:now) and received_count<total_count order by id desc")
                .param("now", now).query(VOUCHER_MAPPER).list();
    }

    List<VoucherView> mine(long userId) {
        return db.sql("select v.* from voucher v join user_voucher uv on uv.voucher_id=v.id "
                        + "where uv.user_id=:userId order by uv.received_at desc")
                .param("userId", userId).query(VOUCHER_MAPPER).list();
    }

    @Transactional
    public VoucherView create(VoucherSave request, String issuerType, long issuerUserId) {
        validate(request, 0);
        String scope = normalizeScope(request.scopeType(), issuerType);
        if ("SELLER".equals(issuerType) && request.shopId() == null) {
            throw DomainException.badRequest("SHOP_REQUIRED", "卖家券必须指定 shopId");
        }
        if ("ADMIN".equals(issuerType) && !"PLATFORM".equals(scope)) {
            throw DomainException.badRequest("ADMIN_SCOPE_INVALID", "管理员券必须为 PLATFORM 范围");
        }
        KeyHolder key = new GeneratedKeyHolder();
        db.sql("insert into voucher(issuer_type,voucher_type,issuer_user_id,scope_type,shop_id,product_id,"
                        + "can_stack,name,discount_type,discount_amount,discount_rate,min_amount,total_count,"
                        + "grab_start_time,grab_end_time,start_time,end_time,status) values(:issuerType,:voucherType,"
                        + ":issuerUserId,:scopeType,:shopId,:productId,false,:name,:discountType,:discountAmount,"
                        + ":discountRate,:minAmount,:totalCount,:startTime,:endTime,:startTime,:endTime,:status)")
                .param("issuerType", issuerType).param("voucherType", "SELLER".equals(issuerType) ? "SELLER" : "PLATFORM")
                .param("issuerUserId", issuerUserId).param("scopeType", scope)
                .param("shopId", request.shopId()).param("productId", request.productId())
                .param("name", request.name().trim()).param("discountType", request.discountType())
                .param("discountAmount", request.discountAmount()).param("discountRate", request.discountRate())
                .param("minAmount", money(request.minAmount())).param("totalCount", request.totalCount())
                .param("startTime", request.startTime()).param("endTime", request.endTime())
                .param("status", request.startTime().isAfter(Instant.now()) ? "SCHEDULED" : "ACTIVE")
                .update(key, "id");
        Number id = Objects.requireNonNull(key.getKey(), "generated voucher id");
        return get(id.longValue());
    }

    @Transactional
    public VoucherView update(long id, VoucherSave request, String issuerType, Long issuerUserId) {
        VoucherView current = owned(id, issuerType, issuerUserId, true);
        validate(request, current.receivedCount());
        int changed = db.sql("update voucher set name=:name,discount_type=:discountType,discount_amount=:discountAmount,"
                        + "discount_rate=:discountRate,min_amount=:minAmount,total_count=:totalCount,start_time=:startTime,"
                        + "end_time=:endTime,grab_start_time=:startTime,grab_end_time=:endTime,shop_id=:shopId,"
                        + "product_id=:productId,status=:status,version=version+1,updated_at=current_timestamp "
                        + "where id=:id and version=:version")
                .param("name", request.name().trim()).param("discountType", request.discountType())
                .param("discountAmount", request.discountAmount()).param("discountRate", request.discountRate())
                .param("minAmount", money(request.minAmount())).param("totalCount", request.totalCount())
                .param("startTime", request.startTime()).param("endTime", request.endTime())
                .param("shopId", "ADMIN".equals(issuerType) ? null : request.shopId())
                .param("productId", "ADMIN".equals(issuerType) ? null : request.productId())
                .param("status", request.startTime().isAfter(Instant.now()) ? "SCHEDULED" : "ACTIVE")
                .param("id", id).param("version", version(id)).update();
        if (changed == 0) throw DomainException.conflict("VOUCHER_CONCURRENT_UPDATE", "优惠券已被并发修改");
        return get(id);
    }

    @Transactional
    public void close(long id, String issuerType, Long issuerUserId) {
        owned(id, issuerType, issuerUserId, false);
        db.sql("update voucher set status='CLOSED',version=version+1,updated_at=current_timestamp where id=:id")
                .param("id", id).update();
    }

    @Transactional
    public void delete(long id, String issuerType, Long issuerUserId) {
        VoucherView current = owned(id, issuerType, issuerUserId, false);
        if (current.receivedCount() > 0) {
            throw DomainException.conflict("VOUCHER_ALREADY_CLAIMED", "已有用户领取的优惠券只能关闭，不能删除");
        }
        db.sql("delete from voucher where id=:id").param("id", id).update();
    }

    @Transactional
    public ClaimResult claim(long voucherId, long userId) {
        VoucherView voucher = lockVoucher(voucherId);
        ensureClaimable(voucher);
        try {
            KeyHolder key = new GeneratedKeyHolder();
            db.sql("insert into user_voucher(user_id,voucher_id,status,expires_at) values(:userId,:voucherId,'AVAILABLE',:expiresAt)")
                    .param("userId", userId).param("voucherId", voucherId).param("expiresAt", voucher.endTime())
                    .update(key, "id");
            int changed = db.sql("update voucher set received_count=received_count+1,version=version+1,updated_at=current_timestamp "
                            + "where id=:id and received_count<total_count")
                    .param("id", voucherId).update();
            if (changed == 0) throw DomainException.conflict("VOUCHER_SOLD_OUT", "优惠券已领完");
            return new ClaimResult(Objects.requireNonNull(key.getKey()).longValue(), voucherId, "AVAILABLE", voucher.endTime());
        } catch (DataIntegrityViolationException error) {
            throw DomainException.conflict("VOUCHER_ALREADY_CLAIMED", "同一用户不能重复领取该优惠券");
        }
    }

    @Transactional
    public QuoteResult quote(QuoteRequest request) {
        QuoteResult result = existingQuote(request).orElseGet(() -> createQuote(request));
        LOG.info("checkout_quote_resolved requestId={} quoteId={} voucherId={} amount={} currency={}",
                request.orderRequestId(), result.quoteId(), result.voucherId(), result.payableAmount(), result.currency());
        return result;
    }

    @Transactional
    public VoucherActionResult reserve(VoucherAction request) {
        String existing = reservationStatus(request);
        if ("RESERVED".equals(existing) || "USED".equals(existing))
            return new VoucherActionResult(request.voucherId(), request.orderRequestId(), existing);
        int changed;
        try {
            changed = db.sql("update user_voucher set status='RESERVED',order_request_id=:orderRequestId,"
                            + "reserved_until=:reservedUntil,version=version+1,updated_at=current_timestamp "
                            + "where user_id=:userId and voucher_id=:voucherId and status='AVAILABLE' and expires_at>:now")
                    .param("orderRequestId", request.orderRequestId())
                    .param("reservedUntil", Instant.now().plus(reservationTtlSeconds, ChronoUnit.SECONDS))
                    .param("userId", request.userId()).param("voucherId", request.voucherId())
                    .param("now", Instant.now()).update();
        } catch (DataIntegrityViolationException reused) {
            throw DomainException.conflict("IDEMPOTENCY_KEY_REUSED",
                    "同一 orderRequestId 不能用于不同的用户或优惠券");
        }
        if (changed == 0) throw DomainException.conflict("VOUCHER_NOT_RESERVABLE", "优惠券不可预占或已被其他订单占用");
        return new VoucherActionResult(request.voucherId(), request.orderRequestId(), "RESERVED");
    }

    @Transactional
    public VoucherActionResult consume(VoucherAction request) {
        String existing = reservationStatus(request);
        if ("USED".equals(existing)) return new VoucherActionResult(request.voucherId(), request.orderRequestId(), "USED");
        int changed = db.sql("update user_voucher set status='USED',used_order_id=:orderId,used_at=current_timestamp,"
                        + "reserved_until=null,version=version+1,updated_at=current_timestamp where user_id=:userId "
                        + "and voucher_id=:voucherId and order_request_id=:orderRequestId and status='RESERVED'")
                .param("orderId", request.orderId()).param("userId", request.userId())
                .param("voucherId", request.voucherId()).param("orderRequestId", request.orderRequestId()).update();
        if (changed == 0) throw DomainException.conflict("VOUCHER_NOT_RESERVED", "优惠券未被该订单预占");
        db.sql("update voucher set used_count=used_count+1,version=version+1 where id=:id")
                .param("id", request.voucherId()).update();
        return new VoucherActionResult(request.voucherId(), request.orderRequestId(), "USED");
    }

    @Transactional
    public VoucherActionResult release(VoucherAction request) {
        String existing = reservationStatus(request);
        if (existing == null || "AVAILABLE".equals(existing)) {
            return new VoucherActionResult(request.voucherId(), request.orderRequestId(), "AVAILABLE");
        }
        if ("USED".equals(existing)) throw DomainException.conflict("VOUCHER_ALREADY_USED", "已核销优惠券不能释放");
        int changed = db.sql("update user_voucher set status='AVAILABLE',order_request_id=null,reserved_until=null,"
                        + "version=version+1,updated_at=current_timestamp where user_id=:userId and voucher_id=:voucherId "
                        + "and order_request_id=:orderRequestId and status='RESERVED'")
                .param("userId", request.userId()).param("voucherId", request.voucherId())
                .param("orderRequestId", request.orderRequestId()).update();
        if (changed == 0) throw DomainException.conflict("VOUCHER_RELEASE_CONFLICT", "优惠券释放发生状态冲突");
        return new VoucherActionResult(request.voucherId(), request.orderRequestId(), "AVAILABLE");
    }

    @Scheduled(fixedDelayString="${app.reservation-cleanup-ms:60000}")
    @Transactional
    public void releaseExpiredReservations() {
        db.sql("update user_voucher set status='AVAILABLE',order_request_id=null,reserved_until=null,version=version+1,"
                        + "updated_at=current_timestamp where status='RESERVED' and reserved_until<:now")
                .param("now", Instant.now()).update();
        db.sql("update user_voucher set status='EXPIRED',version=version+1,updated_at=current_timestamp "
                        + "where status='AVAILABLE' and expires_at<:now")
                .param("now", Instant.now()).update();
    }

    private QuoteResult createQuote(QuoteRequest request) {
        BigDecimal original = money(request.amount());
        BigDecimal discount = BigDecimal.ZERO.setScale(2);
        if (request.voucherId() != null) {
            VoucherView voucher = get(request.voucherId());
            ensureVoucherOwnedAndUsable(voucher, request);
            discount = calculateDiscount(voucher, original).min(original);
        }
        QuoteResult result = new QuoteResult(UUID.randomUUID().toString(), 1, original, discount,
                original.subtract(discount), currency, Instant.now().plus(quoteTtlSeconds, ChronoUnit.SECONDS),
                request.voucherId());
        try {
            db.sql("insert into checkout_quote(quote_id,quote_version,order_request_id,user_id,voucher_id,original_amount,"
                            + "discount_amount,payable_amount,currency,expires_at) values(:quoteId,1,:orderRequestId,:userId,"
                            + ":voucherId,:original,:discount,:payable,:currency,:expiresAt)")
                    .param("quoteId", result.quoteId()).param("orderRequestId", request.orderRequestId())
                    .param("userId", request.userId()).param("voucherId", request.voucherId())
                    .param("original", result.originalAmount()).param("discount", result.discountAmount())
                    .param("payable", result.payableAmount()).param("currency", currency)
                    .param("expiresAt", result.expiresAt()).update();
            return result;
        } catch (DataIntegrityViolationException concurrent) {
            return existingQuote(request).orElseThrow(() -> concurrent);
        }
    }

    private java.util.Optional<QuoteResult> existingQuote(QuoteRequest request) {
        return db.sql("select * from checkout_quote where order_request_id=:id")
                .param("id", request.orderRequestId())
                .query((rs, row) -> new QuoteRow(rs.getLong("user_id"), nullableLong(rs, "voucher_id"),
                        rs.getBigDecimal("original_amount"),
                        new QuoteResult(rs.getString("quote_id"), rs.getInt("quote_version"),
                                rs.getBigDecimal("original_amount"), rs.getBigDecimal("discount_amount"),
                                rs.getBigDecimal("payable_amount"), rs.getString("currency"),
                                instant(rs, "expires_at"), nullableLong(rs, "voucher_id"))))
                .optional().map(existing -> {
                    if (existing.userId() != request.userId()
                            || existing.originalAmount().compareTo(money(request.amount())) != 0
                            || !Objects.equals(existing.voucherId(), request.voucherId())) {
                        throw DomainException.conflict("IDEMPOTENCY_KEY_REUSED",
                                "同一 orderRequestId 不能用于不同的报价参数");
                    }
                    return existing.result();
                });
    }

    private void ensureVoucherOwnedAndUsable(VoucherView voucher, QuoteRequest request) {
        Instant now = Instant.now();
        if (voucher.startTime().isAfter(now) || !voucher.endTime().isAfter(now) || "CLOSED".equals(voucher.status()))
            throw DomainException.conflict("VOUCHER_NOT_USABLE", "优惠券不在有效期内");
        Integer owned = db.sql("select count(*) from user_voucher where user_id=:userId and voucher_id=:voucherId "
                        + "and status='AVAILABLE' and expires_at>:now")
                .param("userId", request.userId()).param("voucherId", voucher.id()).param("now", now)
                .query(Integer.class).single();
        if (owned == 0) throw DomainException.conflict("VOUCHER_NOT_OWNED", "用户没有可用的该优惠券");
        if (request.amount().compareTo(voucher.minAmount()) < 0)
            throw DomainException.conflict("VOUCHER_THRESHOLD_NOT_MET", "订单金额未达到优惠券门槛");
        if ("SHOP".equals(voucher.scopeType()) && (request.shopIds() == null || !request.shopIds().contains(voucher.shopId())))
            throw DomainException.conflict("VOUCHER_SCOPE_MISMATCH", "优惠券不适用于订单店铺");
        if ("PRODUCT".equals(voucher.scopeType()) && (request.productIds() == null || !request.productIds().contains(voucher.productId())))
            throw DomainException.conflict("VOUCHER_SCOPE_MISMATCH", "优惠券不适用于订单商品");
    }

    private String reservationStatus(VoucherAction request) {
        return db.sql("select status from user_voucher where user_id=:userId and voucher_id=:voucherId "
                        + "and (order_request_id=:orderRequestId or (order_request_id is null and status='AVAILABLE'))")
                .param("userId", request.userId()).param("voucherId", request.voucherId())
                .param("orderRequestId", request.orderRequestId()).query(String.class).optional().orElse(null);
    }

    private VoucherView owned(long id, String issuerType, Long issuerUserId, boolean editable) {
        VoucherView voucher = get(id);
        if (!issuerType.equals(voucher.issuerType()) || (issuerUserId != null && !issuerUserId.equals(voucher.issuerUserId())))
            throw DomainException.forbidden("VOUCHER_NOT_OWNED", "不能操作其他发行方的优惠券");
        if (editable && "CLOSED".equals(voucher.status()))
            throw DomainException.conflict("VOUCHER_CLOSED", "已关闭优惠券不能编辑");
        return voucher;
    }

    private VoucherView get(long id) {
        return db.sql("select * from voucher where id=:id").param("id", id).query(VOUCHER_MAPPER).optional()
                .orElseThrow(() -> DomainException.notFound("VOUCHER_NOT_FOUND", "优惠券不存在"));
    }

    private VoucherView lockVoucher(long id) {
        return db.sql("select * from voucher where id=:id for update").param("id", id).query(VOUCHER_MAPPER).optional()
                .orElseThrow(() -> DomainException.notFound("VOUCHER_NOT_FOUND", "优惠券不存在"));
    }

    private int version(long id) {
        return db.sql("select version from voucher where id=:id").param("id", id).query(Integer.class).single();
    }

    private void ensureClaimable(VoucherView voucher) {
        Instant now = Instant.now();
        if ("CLOSED".equals(voucher.status()) || voucher.startTime().isAfter(now) || !voucher.endTime().isAfter(now))
            throw DomainException.conflict("VOUCHER_NOT_CLAIMABLE", "优惠券当前不可领取");
        if (voucher.receivedCount() >= voucher.totalCount())
            throw DomainException.conflict("VOUCHER_SOLD_OUT", "优惠券已领完");
    }

    private void validate(VoucherSave request, int receivedCount) {
        if (!request.endTime().isAfter(request.startTime()))
            throw DomainException.badRequest("VOUCHER_TIME_INVALID", "结束时间必须晚于开始时间");
        if (request.totalCount() < receivedCount)
            throw DomainException.badRequest("VOUCHER_QUANTITY_INVALID", "总数量不能小于已领取数量");
        if ("AMOUNT".equals(request.discountType())) {
            if (request.discountAmount() == null || request.discountAmount().compareTo(BigDecimal.ZERO) <= 0)
                throw DomainException.badRequest("VOUCHER_DISCOUNT_INVALID", "满减券金额必须大于 0");
        } else if (request.discountRate() == null || request.discountRate().compareTo(BigDecimal.ZERO) <= 0
                || request.discountRate().compareTo(BigDecimal.ONE) >= 0) {
            throw DomainException.badRequest("VOUCHER_DISCOUNT_INVALID", "折扣率必须在 0 和 1 之间");
        }
    }

    private static String normalizeScope(String scope, String issuerType) {
        if ("ADMIN".equals(issuerType)) return "PLATFORM";
        String normalized = scope == null ? "SHOP" : scope.toUpperCase();
        if (!List.of("SHOP", "PRODUCT").contains(normalized))
            throw DomainException.badRequest("VOUCHER_SCOPE_INVALID", "卖家券范围必须为 SHOP 或 PRODUCT");
        return normalized;
    }

    private static BigDecimal calculateDiscount(VoucherView voucher, BigDecimal eligible) {
        return "AMOUNT".equals(voucher.discountType()) ? money(voucher.discountAmount())
                : money(eligible.multiply(BigDecimal.ONE.subtract(voucher.discountRate())));
    }

    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private record QuoteRow(long userId, Long voucherId, BigDecimal originalAmount, QuoteResult result) {}
    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
    private static VoucherView mapVoucher(ResultSet rs, int row) throws SQLException {
        return new VoucherView(rs.getLong("id"), rs.getString("issuer_type"), rs.getString("voucher_type"),
                nullableLong(rs, "issuer_user_id"), rs.getString("scope_type"), nullableLong(rs, "shop_id"),
                nullableLong(rs, "product_id"), rs.getString("name"), rs.getString("discount_type"),
                rs.getBigDecimal("discount_amount"), rs.getBigDecimal("discount_rate"), rs.getBigDecimal("min_amount"),
                rs.getInt("total_count"), rs.getInt("received_count"), rs.getInt("used_count"),
                instant(rs, "start_time"), instant(rs, "end_time"), rs.getString("status"));
    }
}
