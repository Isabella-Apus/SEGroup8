package com.segroup8.identity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.identity.api.ApiException;
import com.segroup8.identity.security.CurrentUser;
import com.segroup8.security.JwtPrincipal;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSetMetaData;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdentityService {
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder passwords;
    private final ObjectMapper json;
    private final SecretKey signingKey;
    private final long tokenMinutes;

    public IdentityService(JdbcTemplate jdbc, BCryptPasswordEncoder passwords, ObjectMapper json,
            @Value("${app.jwt-secret}") String jwtSecret,
            @Value("${app.access-token-minutes:30}") long tokenMinutes) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.json = json;
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.tokenMinutes = tokenMinutes;
    }

    @Transactional
    public void register(String username, String password, String nickname, String phone, String email) {
        if (findUserByUsername(username).isPresent()) {
            throw new ApiException(400, "用户名已存在");
        }
        jdbc.update("INSERT INTO `user`(username,password,nickname,phone,email,role,status,credit_score,buyer_credit_score,seller_credit_score,access_version) "
                        + "VALUES(?,?,?,?,?,'USER','NORMAL',100,100,100,1)",
                username, passwords.encode(password), nickname, blankToNull(phone), blankToNull(email));
    }

    public Map<String, Object> login(String username, String password) {
        UserAccount user = findUserByUsername(username)
                .orElseThrow(() -> new ApiException(401, "用户名或密码错误"));
        if (!passwords.matches(password, user.password())) {
            throw new ApiException(401, "用户名或密码错误");
        }
        if (!"NORMAL".equals(user.status())) {
            throw new ApiException(403, "账号已禁用");
        }
        Instant now = Instant.now();
        String token = Jwts.builder()
                .claim("uid", user.id())
                .claim("username", user.username())
                .claim("role", user.role())
                .claim("accessVersion", user.accessVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(tokenMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("role", user.role());
        result.put("user", userView(user));
        return result;
    }

    public void assertActive(long userId) {
        UserAccount user = findUser(userId).orElseThrow(() -> new ApiException(401, "用户不存在"));
        if (!"NORMAL".equals(user.status())) {
            throw new ApiException(403, "账号已禁用");
        }
    }

    public void requireAdmin() {
        if (!"ADMIN".equals(CurrentUser.require().role())) {
            throw new ApiException(403, "需要管理员权限");
        }
    }

    public Map<String, Object> profile() {
        return userView(findUser(CurrentUser.require().userId())
                .orElseThrow(() -> new ApiException(404, "用户不存在")));
    }

    @Transactional
    public void updateProfile(String nickname, String avatar, String phone, String email) {
        long id = CurrentUser.require().userId();
        jdbc.update("UPDATE `user` SET nickname=COALESCE(?,nickname),avatar=COALESCE(?,avatar),"
                        + "phone=COALESCE(?,phone),email=COALESCE(?,email),update_time=CURRENT_TIMESTAMP WHERE id=?",
                blankToNull(nickname), blankToNull(avatar), blankToNull(phone), blankToNull(email), id);
    }

    public List<Map<String, Object>> searchUsers(String keyword) {
        String value = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        return rows("SELECT id,username,nickname,avatar,role,status FROM `user` "
                + "WHERE username LIKE ? OR nickname LIKE ? ORDER BY id LIMIT 20", value, value);
    }

    public List<Map<String, Object>> addresses() {
        return rows("SELECT id,user_id,receiver_name,receiver_phone,province,city,detail_address,is_default,create_time,update_time "
                + "FROM address WHERE user_id=? ORDER BY is_default DESC,id", CurrentUser.require().userId());
    }

    public Map<String, Object> addressSnapshot(long userId, long addressId) {
        return addressSnapshot("WHERE id=? AND user_id=?", addressId, userId);
    }

    public Map<String, Object> shippingAddress(long userId) {
        return addressSnapshot("WHERE user_id=? ORDER BY is_default DESC,id LIMIT 1", userId);
    }

    private Map<String, Object> addressSnapshot(String suffix, Object... arguments) {
        List<Map<String, Object>> matches = jdbc.query(
                "SELECT id,user_id,receiver_name,receiver_phone,province,city,detail_address FROM address " + suffix,
                (rs, row) -> {
                    Map<String, Object> snapshot = new LinkedHashMap<>();
                    snapshot.put("addressId", rs.getLong("id"));
                    snapshot.put("userId", rs.getLong("user_id"));
                    snapshot.put("receiverName", rs.getString("receiver_name"));
                    snapshot.put("receiverPhone", rs.getString("receiver_phone"));
                    snapshot.put("province", rs.getString("province"));
                    snapshot.put("city", rs.getString("city"));
                    snapshot.put("detailAddress", rs.getString("detail_address"));
                    return snapshot;
                }, arguments);
        if (matches.isEmpty()) throw new ApiException(404, "address not found");
        return matches.get(0);
    }

    @Transactional
    public void addAddress(Map<String, Object> request) {
        long userId = CurrentUser.require().userId();
        int isDefault = intValue(request.get("isDefault"), 0);
        if (isDefault == 1) {
            jdbc.update("UPDATE address SET is_default=0 WHERE user_id=?", userId);
        }
        jdbc.update("INSERT INTO address(user_id,receiver_name,receiver_phone,province,city,detail_address,is_default) "
                        + "VALUES(?,?,?,?,?,?,?)", userId,
                required(request, "receiverName"), required(request, "receiverPhone"),
                required(request, "province"), required(request, "city"),
                required(request, "detailAddress"), isDefault);
    }

    @Transactional
    public void updateAddress(long addressId, Map<String, Object> request) {
        long userId = CurrentUser.require().userId();
        if (count("SELECT COUNT(*) FROM address WHERE id=? AND user_id=?", addressId, userId) == 0) {
            throw new ApiException(404, "地址不存在");
        }
        int isDefault = intValue(request.get("isDefault"), 0);
        if (isDefault == 1) {
            jdbc.update("UPDATE address SET is_default=0 WHERE user_id=?", userId);
        }
        jdbc.update("UPDATE address SET receiver_name=?,receiver_phone=?,province=?,city=?,detail_address=?,is_default=?,"
                        + "update_time=CURRENT_TIMESTAMP WHERE id=? AND user_id=?",
                required(request, "receiverName"), required(request, "receiverPhone"),
                required(request, "province"), required(request, "city"), required(request, "detailAddress"),
                isDefault, addressId, userId);
    }

    @Transactional
    public void deleteAddress(long addressId) {
        if (jdbc.update("DELETE FROM address WHERE id=? AND user_id=?", addressId,
                CurrentUser.require().userId()) == 0) {
            throw new ApiException(404, "地址不存在");
        }
    }

    @Transactional
    public void submitMerchantApplication(Map<String, Object> request) {
        long userId = CurrentUser.require().userId();
        if (count("SELECT COUNT(*) FROM merchant_application WHERE user_id=? AND status IN (0,1)", userId) > 0) {
            throw new ApiException(400, "已有待审核或已通过的申请");
        }
        jdbc.update("INSERT INTO merchant_application(user_id,store_name,category_id,id_card_no,bank_card_no,license_img,"
                        + "warehouse_addr,warehouse_province,warehouse_city,warehouse_detail,contact_name,contact_phone,status) "
                        + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,0)", userId, required(request, "storeName"),
                longValue(request.get("categoryId"), "categoryId"), required(request, "idCardNo"),
                required(request, "bankCardNo"), required(request, "licenseImg"), required(request, "warehouseAddr"),
                required(request, "warehouseProvince"), required(request, "warehouseCity"),
                required(request, "warehouseDetail"), required(request, "contactName"), required(request, "contactPhone"));
    }

    public Map<String, Object> myMerchantApplication() {
        return one("SELECT * FROM merchant_application WHERE user_id=? ORDER BY id DESC LIMIT 1",
                CurrentUser.require().userId()).orElse(null);
    }

    public Map<String, Object> merchantApplications(Integer status, int page, int size) {
        requireAdmin();
        String where = status == null ? "" : " WHERE status=?";
        Object[] args = status == null ? new Object[]{} : new Object[]{status};
        return page("merchant_application", where, args, page, size);
    }

    @Transactional
    public void approveMerchant(long applicationId) {
        requireAdmin();
        Map<String, Object> application = one("SELECT * FROM merchant_application WHERE id=?", applicationId)
                .orElseThrow(() -> new ApiException(404, "申请不存在"));
        if (intValue(application.get("status"), -1) != 0) {
            throw new ApiException(400, "申请已处理");
        }
        long userId = longValue(application.get("userId"), "userId");
        jdbc.update("UPDATE merchant_application SET status=1,reject_reason=NULL WHERE id=?", applicationId);
        jdbc.update("UPDATE `user` SET role='OFFICIAL_SELLER',shop_name=?,access_version=access_version+1 WHERE id=?",
                application.get("storeName"), userId);
        outbox("MerchantApproved.v1", "merchant-application", applicationId,
                Map.of("applicationId", applicationId, "userId", userId, "storeName", application.get("storeName")));
        audit("APPROVE_MERCHANT_APPLICATION", "MERCHANT_APPLICATION", applicationId, "通过入驻申请");
    }

    @Transactional
    public void rejectMerchant(long applicationId, String reason) {
        requireAdmin();
        if (jdbc.update("UPDATE merchant_application SET status=2,reject_reason=? WHERE id=? AND status=0",
                reason, applicationId) == 0) {
            throw new ApiException(400, "申请不存在或已处理");
        }
        audit("REJECT_MERCHANT_APPLICATION", "MERCHANT_APPLICATION", applicationId, "驳回入驻申请");
    }

    public Map<String, Object> users(int page, int size) {
        requireAdmin();
        return page("`user`", "", new Object[]{}, page, size);
    }

    @Transactional
    public void changeBan(long userId, boolean banned) {
        requireAdmin();
        if (userId == CurrentUser.require().userId()) {
            throw new ApiException(400, "管理员不能修改自己的状态");
        }
        if (jdbc.update("UPDATE `user` SET status=?,access_version=access_version+1 WHERE id=?",
                banned ? "BANNED" : "NORMAL", userId) == 0) {
            throw new ApiException(404, "用户不存在");
        }
        String action = banned ? "BAN_USER" : "UNBAN_USER";
        UserAccount changed = findUser(userId).orElseThrow(() -> new ApiException(404, "用户不存在"));
        outbox("UserAccessChanged.v1", "user", userId,
                Map.of("userId", userId, "status", banned ? "BANNED" : "NORMAL",
                        "version", changed.accessVersion(), "role", changed.role(),
                        "displayName", safe(changed.nickname()), "avatarUrl", safe(changed.avatar())));
        audit(action, "USER", userId, banned ? "管理员封禁用户" : "管理员解禁用户");
    }

    @Transactional
    public void submitReport(Map<String, Object> request) {
        JwtPrincipal current = CurrentUser.require();
        long reportedId = longValue(request.get("reportedId"), "reportedId");
        if (reportedId == current.userId()) {
            throw new ApiException(400, "不能举报自己");
        }
        if (findUser(reportedId).isEmpty()) {
            throw new ApiException(404, "被举报用户不存在");
        }
        jdbc.update("INSERT INTO user_report(reporter_id,reported_id,reporter_role,trade_context,reason_type,reason_desc,evidence_urls,status) "
                        + "VALUES(?,?,?,?,?,?,?,0)", current.userId(), reportedId, current.role(),
                value(request, "tradeContext", "SHOP"), required(request, "reasonType"),
                blankToNull(text(request.get("reasonDesc"))), blankToNull(text(request.get("evidenceUrls"))));
    }

    public Map<String, Object> myReports(int page, int size) {
        return page("user_report", " WHERE reporter_id=?", new Object[]{CurrentUser.require().userId()}, page, size);
    }

    @Transactional
    public void block(long targetUserId) {
        long userId = CurrentUser.require().userId();
        if (targetUserId == userId) {
            throw new ApiException(400, "不能拉黑自己");
        }
        if (isBlocking(userId, targetUserId)) {
            throw new ApiException(400, "已拉黑该用户");
        }
        jdbc.update("INSERT INTO user_block(blocker_id,blocked_id) VALUES(?,?)", userId, targetUserId);
    }

    @Transactional
    public void unblock(long targetUserId) {
        if (jdbc.update("DELETE FROM user_block WHERE blocker_id=? AND blocked_id=?",
                CurrentUser.require().userId(), targetUserId) == 0) {
            throw new ApiException(400, "尚未拉黑该用户");
        }
    }

    public List<Map<String, Object>> myBlocks() {
        return rows("SELECT id,blocker_id,blocked_id,create_time FROM user_block WHERE blocker_id=? ORDER BY id",
                CurrentUser.require().userId());
    }

    public boolean isBlocking(long blockerId, long blockedId) {
        return count("SELECT COUNT(*) FROM user_block WHERE blocker_id=? AND blocked_id=?", blockerId, blockedId) > 0;
    }

    public Map<String, Object> credit(long userId) {
        UserAccount user = findUser(userId).orElseThrow(() -> new ApiException(404, "用户不存在"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.id());
        result.put("buyerScore", user.creditScore());
        result.put("sellerScore", user.sellerCreditScore());
        result.put("logs", rows("SELECT * FROM credit_score_log WHERE user_id=? ORDER BY id DESC", userId));
        return result;
    }

    public Map<String, Object> reports(Integer status, Long reportedId, int page, int size) {
        requireAdmin();
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (status != null) {
            clauses.add("status=?");
            args.add(status);
        }
        if (reportedId != null) {
            clauses.add("reported_id=?");
            args.add(reportedId);
        }
        String where = clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
        return page("user_report", where, args.toArray(), page, size);
    }

    @Transactional
    public void auditReport(long reportId, int decision, String remark, Integer customDelta) {
        requireAdmin();
        if (decision != 1 && decision != 2) {
            throw new ApiException(400, "审核结果只能为 1 或 2");
        }
        Map<String, Object> report = one("SELECT * FROM user_report WHERE id=?", reportId)
                .orElseThrow(() -> new ApiException(404, "举报不存在"));
        if (intValue(report.get("status"), -1) != 0) {
            throw new ApiException(400, "举报已审核");
        }
        long adminId = CurrentUser.require().userId();
        jdbc.update("UPDATE user_report SET status=?,admin_id=?,admin_remark=?,audit_time=CURRENT_TIMESTAMP,"
                + "update_time=CURRENT_TIMESTAMP WHERE id=?", decision, adminId, blankToNull(remark), reportId);
        if (decision == 1) {
            int delta = Math.max(1, Math.min(customDelta == null ? 10 : customDelta, 30));
            long userId = longValue(report.get("reportedId"), "reportedId");
            jdbc.update("UPDATE `user` SET credit_score=GREATEST(0,credit_score-?),buyer_credit_score=GREATEST(0,buyer_credit_score-?) WHERE id=?",
                    delta, delta, userId);
            jdbc.update("INSERT INTO credit_score_log(user_id,role,delta,reason_code,reason_desc,ref_id,operator_id) "
                            + "VALUES(?,'BUYER',?,'REPORT_UPHELD',?,?,?)", userId, -delta, remark, reportId, adminId);
        }
        audit(decision == 1 ? "REPORT_UPHELD" : "REPORT_REJECTED", "USER_REPORT", reportId,
                decision == 1 ? "举报成立，已扣分" : "举报不成立，已驳回");
    }

    @Transactional
    public void adjustCredit(long userId, String role, int delta, String remark) {
        requireAdmin();
        String normalized = role == null ? "BUYER" : role.toUpperCase(Locale.ROOT);
        String column = "SELLER".equals(normalized) ? "seller_credit_score" : "credit_score";
        jdbc.update("UPDATE `user` SET " + column + "=LEAST(100,GREATEST(0," + column + "+?)) WHERE id=?",
                delta, userId);
        jdbc.update("INSERT INTO credit_score_log(user_id,role,delta,reason_code,reason_desc,operator_id) "
                + "VALUES(?,?,?,'ADMIN_ADJUST',?,?)", userId, normalized, delta, remark, CurrentUser.require().userId());
        audit("CREDIT_ADJUST", "USER", userId, "管理员调整" + normalized + "信用分：" + delta);
    }

    public Map<String, Object> auditLogs(int page, int size, String targetType) {
        requireAdmin();
        String where = targetType == null || targetType.isBlank() ? "" : " WHERE target_type=?";
        Object[] args = where.isEmpty() ? new Object[]{} : new Object[]{targetType};
        return page("admin_audit_log", where, args, page, size);
    }

    public Map<String, Object> introspect(String token) {
        try {
            var claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
            long userId = ((Number) claims.get("uid")).longValue();
            UserAccount user = findUser(userId).orElseThrow(() -> new ApiException(401, "用户不存在"));
            return Map.of("active", "NORMAL".equals(user.status()), "userId", user.id(),
                    "username", user.username(), "role", user.role(), "accessVersion", user.accessVersion());
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            return Map.of("active", false);
        }
    }

    public Map<String, Object> userSummary(long userId) {
        UserAccount user = findUser(userId).orElseThrow(() -> new ApiException(404, "用户不存在"));
        return Map.of("id", user.id(), "username", user.username(), "nickname", safe(user.nickname()),
                "avatar", safe(user.avatar()), "role", user.role(), "status", user.status());
    }

    public List<Map<String, Object>> checkBlocks(List<Map<String, Object>> pairs) {
        return pairs.stream().map(pair -> {
            long blockerId = longValue(pair.get("blockerId"), "blockerId");
            long blockedId = longValue(pair.get("blockedId"), "blockedId");
            return Map.<String, Object>of("blockerId", blockerId, "blockedId", blockedId,
                    "blocked", isBlocking(blockerId, blockedId));
        }).toList();
    }

    private void audit(String action, String targetType, Long targetId, String detail) {
        JwtPrincipal admin = CurrentUser.require();
        jdbc.update("INSERT INTO admin_audit_log(admin_user_id,admin_username,action,target_type,target_id,detail) "
                + "VALUES(?,?,?,?,?,?)", admin.userId(), admin.username(), action, targetType, targetId, detail);
    }

    private void outbox(String eventType, String aggregateType, long aggregateId, Map<String, Object> payload) {
        try {
            jdbc.update("INSERT INTO outbox_event(event_id,event_type,aggregate_type,aggregate_id,payload,status) "
                            + "VALUES(?,?,?,?,?,'PENDING')", UUID.randomUUID().toString(), eventType, aggregateType,
                    aggregateId, json.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize outbox payload", ex);
        }
    }

    private Map<String, Object> page(String table, String where, Object[] args, int page, int size) {
        int normalizedPage = Math.max(1, page);
        int normalizedSize = Math.max(1, Math.min(100, size));
        long total = count("SELECT COUNT(*) FROM " + table + where, args);
        List<Object> queryArgs = new ArrayList<>(List.of(args));
        queryArgs.add(normalizedSize);
        queryArgs.add((normalizedPage - 1) * normalizedSize);
        List<Map<String, Object>> records = rows("SELECT * FROM " + table + where + " ORDER BY id DESC LIMIT ? OFFSET ?",
                queryArgs.toArray());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("pageNum", normalizedPage);
        result.put("pageSize", normalizedSize);
        result.put("records", records);
        return result;
    }

    private Optional<UserAccount> findUser(long userId) {
        List<UserAccount> users = jdbc.query("SELECT * FROM `user` WHERE id=?", (rs, row) -> new UserAccount(
                rs.getLong("id"), rs.getString("username"), rs.getString("password"), rs.getString("nickname"),
                rs.getString("avatar"), rs.getString("phone"), rs.getString("email"), rs.getString("role"),
                rs.getString("status"), rs.getInt("credit_score"), rs.getInt("buyer_credit_score"),
                rs.getInt("seller_credit_score"), rs.getString("shop_name"), rs.getLong("access_version")), userId);
        return users.stream().findFirst();
    }

    private Optional<UserAccount> findUserByUsername(String username) {
        List<UserAccount> users = jdbc.query("SELECT * FROM `user` WHERE username=?", (rs, row) -> new UserAccount(
                rs.getLong("id"), rs.getString("username"), rs.getString("password"), rs.getString("nickname"),
                rs.getString("avatar"), rs.getString("phone"), rs.getString("email"), rs.getString("role"),
                rs.getString("status"), rs.getInt("credit_score"), rs.getInt("buyer_credit_score"),
                rs.getInt("seller_credit_score"), rs.getString("shop_name"), rs.getLong("access_version")), username);
        return users.stream().findFirst();
    }

    private Map<String, Object> userView(UserAccount user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.id());
        result.put("username", user.username());
        result.put("nickname", user.nickname());
        result.put("avatar", user.avatar());
        result.put("phone", user.phone());
        result.put("email", user.email());
        result.put("role", user.role());
        result.put("status", user.status());
        result.put("creditScore", user.creditScore());
        result.put("buyerCreditScore", user.buyerCreditScore());
        result.put("sellerCreditScore", user.sellerCreditScore());
        result.put("shopName", user.shopName());
        return result;
    }

    private Optional<Map<String, Object>> one(String sql, Object... args) {
        return rows(sql, args).stream().findFirst();
    }

    private List<Map<String, Object>> rows(String sql, Object... args) {
        return jdbc.query(sql, (rs, rowNum) -> {
            ResultSetMetaData meta = rs.getMetaData();
            Map<String, Object> value = new LinkedHashMap<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                value.put(camel(meta.getColumnLabel(i)), rs.getObject(i));
            }
            return value;
        }, args);
    }

    private long count(String sql, Object... args) {
        Long result = jdbc.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }

    private String camel(String name) {
        String source = name.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        boolean upper = false;
        for (char ch : source.toCharArray()) {
            if (ch == '_') {
                upper = true;
            } else {
                result.append(upper ? Character.toUpperCase(ch) : ch);
                upper = false;
            }
        }
        return result.toString();
    }

    private String required(Map<String, Object> request, String name) {
        String value = text(request.get(name));
        if (value == null || value.isBlank()) {
            throw new ApiException(400, name + " 不能为空");
        }
        return value.trim();
    }

    private long longValue(Object value, String name) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            throw new ApiException(400, name + " 不合法");
        }
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String value(Map<String, Object> request, String name, String fallback) {
        String value = text(request.get(name));
        return value == null || value.isBlank() ? fallback : value;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record UserAccount(long id, String username, String password, String nickname, String avatar,
            String phone, String email, String role, String status, int creditScore, int buyerCreditScore,
            int sellerCreditScore, String shopName, long accessVersion) {
    }
}
