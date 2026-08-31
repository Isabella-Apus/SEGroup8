package com.segroup8.order;

import com.segroup8.order.ApiModels.InternalSnapshot;
import com.segroup8.order.ApiModels.AfterSaleLogView;
import com.segroup8.order.ApiModels.LogisticsView;
import com.segroup8.order.ApiModels.OrderItemView;
import com.segroup8.order.ApiModels.OrderView;
import com.segroup8.order.ApiModels.PageView;
import com.segroup8.order.ApiModels.ReviewView;
import com.segroup8.order.DownstreamGateway.ProductSnapshot;
import com.segroup8.order.DownstreamGateway.Quote;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
class OrderRepository {
    private final JdbcTemplate db;

    OrderRepository(JdbcTemplate db) { this.db = db; }

    Optional<Long> idempotentResource(String scope, String key, String requestHash) {
        List<Map<String, Object>> rows = db.queryForList(
                "select request_hash, resource_id from idempotency_record where operation_scope=? and idempotency_key=?",
                scope, key);
        if (rows.isEmpty()) return Optional.empty();
        if (!requestHash.equals(rows.get(0).get("request_hash"))) {
            throw new OrderException("IDEMPOTENCY_KEY_REUSED", "Idempotency key was used with another request", 409);
        }
        Number id = (Number) rows.get(0).get("resource_id");
        return Optional.ofNullable(id == null ? null : id.longValue());
    }

    void saveIdempotency(String scope, String key, String hash, long resourceId, String code) {
        db.update("insert into idempotency_record(operation_scope,idempotency_key,request_hash,resource_id,result_code) values(?,?,?,?,?)",
                scope, key, hash, resourceId, code);
    }

    long insertOrder(long buyerId, String businessKey, ApiModels.CreateOrderRequest request,
            List<ProductSnapshot> items, Quote quote, String reservationId) {
        BigDecimal total = items.stream().map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String orderNo = "O" + Instant.now().toEpochMilli() + UUID.randomUUID().toString().substring(0, 6);
        var kh = new GeneratedKeyHolder();
        db.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                insert into order_info(order_no,business_key,buyer_user_id,total_amount,payable_amount,order_status,
                  receiver_name,receiver_phone,receiver_province,receiver_city,receiver_detail_address,remark,voucher_id,
                  voucher_discount_amount,seller_bear_amount,platform_bear_amount,reservation_id)
                values(?,?,?,?,?,'PENDING_PAY',?,?,?,?,?,?,?,?,?,?,?)
                """, new String[]{"id"});
            int p = 1;
            ps.setString(p++, orderNo); ps.setString(p++, businessKey); ps.setLong(p++, buyerId);
            ps.setBigDecimal(p++, total); ps.setBigDecimal(p++, quote.payableAmount());
            ps.setString(p++, request.receiverName()); ps.setString(p++, request.receiverPhone());
            ps.setString(p++, request.receiverProvince()); ps.setString(p++, request.receiverCity());
            ps.setString(p++, request.receiverDetailAddress()); ps.setString(p++, request.remark());
            if (request.voucherId() == null) ps.setObject(p++, null); else ps.setLong(p++, request.voucherId());
            ps.setBigDecimal(p++, quote.voucherDiscountAmount()); ps.setBigDecimal(p++, quote.sellerBearAmount());
            ps.setBigDecimal(p++, quote.platformBearAmount()); ps.setString(p, reservationId);
            return ps;
        }, kh);
        long id = kh.getKey().longValue();
        for (ProductSnapshot item : items) {
            db.update("insert into order_item(order_id,product_type,product_id,product_name,price,quantity,seller_user_id,shop_id) values(?,?,?,?,?,?,?,?)",
                    id, "NEW", item.productId(), item.productName(), item.price(), item.quantity(),
                    item.sellerUserId(), item.shopId());
        }
        return id;
    }

    long insertSecondhand(ApiModels.SecondhandOrderRequest request) {
        String businessKey = request.tradeType() + ":" + request.tradeId();
        List<Long> existing = db.query("select id from order_info where business_key=?",
                (rs, n) -> rs.getLong(1), businessKey);
        if (!existing.isEmpty()) return existing.get(0);
        String orderNo = "S" + Instant.now().toEpochMilli() + UUID.randomUUID().toString().substring(0, 6);
        var kh = new GeneratedKeyHolder();
        db.update(c -> {
            PreparedStatement ps = c.prepareStatement("""
              insert into order_info(order_no,business_key,buyer_user_id,total_amount,payable_amount,order_status,
                receiver_name,receiver_phone,receiver_province,receiver_city,receiver_detail_address,remark)
              values(?,?,?,?,?,'PENDING_PAY',?,?,?,?,?,?)
              """, new String[]{"id"});
            int p=1; ps.setString(p++,orderNo); ps.setString(p++,businessKey); ps.setLong(p++,request.buyerUserId());
            ps.setBigDecimal(p++,request.price()); ps.setBigDecimal(p++,request.price()); ps.setString(p++,request.receiverName());
            ps.setString(p++,request.receiverPhone()); ps.setString(p++,request.receiverProvince()); ps.setString(p++,request.receiverCity());
            ps.setString(p++,request.receiverDetailAddress()); ps.setString(p,request.remark()); return ps;
        }, kh);
        long id = kh.getKey().longValue();
        db.update("insert into order_item(order_id,product_type,product_id,product_name,price,quantity,seller_user_id) values(?,?,?,?,?,1,?)",
                id,"SECONDHAND",request.productId(),request.productName(),request.price(),request.sellerUserId());
        return id;
    }

    OrderView get(long id) {
        List<OrderView> rows = db.query("select * from order_info where id=?", (rs, n) -> new OrderView(
                rs.getLong("id"), rs.getString("order_no"), rs.getLong("buyer_user_id"),
                rs.getBigDecimal("total_amount"), rs.getBigDecimal("payable_amount"), rs.getString("pay_status"),
                OrderState.valueOf(rs.getString("order_status")), rs.getString("refund_status"),
                rs.getString("refund_reason"), rs.getString("delivery_no"), rs.getString("receiver_name"),
                rs.getString("receiver_phone"),rs.getString("receiver_province"),rs.getString("receiver_city"),
                rs.getString("receiver_detail_address"),maskPhone(rs.getString("receiver_phone")),
                rs.getString("receiver_province") + " " + rs.getString("receiver_city"),
                rs.getString("remark"), rs.getInt("version"), instant(rs.getTimestamp("create_time")),
                instant(rs.getTimestamp("update_time")), items(id)), id);
        if (rows.isEmpty()) throw new OrderException("ORDER_NOT_FOUND", "Order does not exist", 404);
        return rows.get(0);
    }

    Optional<OrderView> byBusinessKey(String key) {
        List<Long> ids = db.query("select id from order_info where business_key=?", (rs,n)->rs.getLong(1), key);
        return ids.isEmpty() ? Optional.empty() : Optional.of(get(ids.get(0)));
    }

    List<OrderItemView> items(long id) {
        return db.query("select * from order_item where order_id=? order by id", (rs,n)->new OrderItemView(
                rs.getLong("id"),rs.getString("product_type"),rs.getLong("product_id"),rs.getString("product_name"),
                rs.getBigDecimal("price"),rs.getInt("quantity"),(Long)rs.getObject("seller_user_id"),
                (Long)rs.getObject("shop_id")),id);
    }

    PageView<OrderView> listBuyer(long buyerId, long page, long size) {
        return listBuyer(buyerId,page,size,null,null,null,null);
    }

    PageView<OrderView> listBuyer(long buyerId,long page,long size,Integer orderStatus,Integer refundStatus,
            String productType,String keyword) {
        List<Long> ids = db.query("select id from order_info where buyer_user_id=? order by create_time desc",
                (rs,n)->rs.getLong(1),buyerId);
        return filterOrders(ids.stream().map(this::get).toList(),page,size,orderStatus,refundStatus,productType,keyword);
    }

    PageView<OrderView> listSeller(long sellerId, long page, long size) {
        return listSeller(sellerId,page,size,null,null,null,null);
    }

    PageView<OrderView> listSeller(long sellerId,long page,long size,Integer orderStatus,Integer refundStatus,
            String productType,String keyword) {
        List<Long> all = db.query("select distinct o.id from order_info o join order_item i on i.order_id=o.id where i.seller_user_id=? order by o.id desc",
                (rs,n)->rs.getLong(1),sellerId);
        return filterOrders(all.stream().map(this::get).toList(),page,size,orderStatus,refundStatus,productType,keyword);
    }

    PageView<OrderView> listAll(long page,long size) {
        long total=db.queryForObject("select count(*) from order_info",Long.class);
        List<Long> ids=db.query("select id from order_info order by create_time desc limit ? offset ?",
                (rs,n)->rs.getLong(1),size,(page-1)*size);
        return new PageView<>(total,page,size,ids.stream().map(this::get).toList());
    }

    void transition(long id, int expectedVersion, OrderState expected, OrderState next, String extraSql, Object... extraArgs) {
        String sql = "update order_info set order_status=?,version=version+1,update_time=current_timestamp" +
                (extraSql == null ? "" : "," + extraSql) + " where id=? and version=? and order_status=?";
        Object[] args = new Object[extraArgs.length + 4];
        args[0]=next.name(); System.arraycopy(extraArgs,0,args,1,extraArgs.length);
        args[args.length-3]=id; args[args.length-2]=expectedVersion; args[args.length-1]=expected.name();
        if (db.update(sql,args)!=1) throw new OrderException("ORDER_CONCURRENTLY_MODIFIED","Order changed concurrently",409);
        outbox(id, "OrderStatusChanged.v1", "{\"orderId\":"+id+",\"status\":\""+next+"\"}");
    }

    void refundStatus(long id, String status, String reason, String refundRequestId) {
        db.update("update order_info set refund_status=?,refund_reason=coalesce(?,refund_reason),refund_request_id=coalesce(?,refund_request_id),version=version+1,update_time=current_timestamp where id=?",
                status,reason,refundRequestId,id);
        afterSale(id,status,null,null,reason);
        outbox(id,"OrderRefundStatusChanged.v1","{\"orderId\":"+id+",\"refundStatus\":\""+status+"\"}");
    }

    void afterSale(long id,String action,Long operator,String role,String remark) {
        db.update("insert into order_after_sale_log(order_id,action,operator_user_id,operator_role,remark) values(?,?,?,?,?)",
                id,action,operator,role,remark);
    }

    void review(long orderId,long userId,ApiModels.ItemReviewRequest r,String type) {
        db.update("insert into review(order_id,product_type,product_id,user_id,score,content,review_type) values(?,?,?,?,?,?,?)",
                orderId,r.productType(),r.productId(),userId,r.score(),r.content(),type);
        outbox(orderId,"ReviewSubmitted.v1","{\"orderId\":"+orderId+",\"productId\":"+r.productId()+"}");
    }

    List<ReviewView> reviews(long userId) {
        return db.query("select * from review where user_id=? order by create_time desc",(rs,n)->new ReviewView(
                rs.getLong("id"),rs.getLong("order_id"),rs.getString("product_type"),rs.getLong("product_id"),
                rs.getLong("user_id"),rs.getInt("score"),rs.getString("content"),rs.getString("review_type"),
                rs.getString("seller_reply"),instant(rs.getTimestamp("create_time"))),userId);
    }

    PageView<ReviewView> reviews(long userId, long page, long size) {
        List<ReviewView> all = reviews(userId);
        return pageReviews(all, page, size);
    }

    PageView<ReviewView> sellerReviews(long sellerId, long page, long size) {
        List<ReviewView> all = db.query("""
                select distinct r.* from review r
                join order_item i on i.order_id=r.order_id and i.product_type=r.product_type and i.product_id=r.product_id
                where i.seller_user_id=? order by r.create_time desc, r.id desc
                """, (rs,n)->new ReviewView(
                rs.getLong("id"),rs.getLong("order_id"),rs.getString("product_type"),rs.getLong("product_id"),
                rs.getLong("user_id"),rs.getInt("score"),rs.getString("content"),rs.getString("review_type"),
                rs.getString("seller_reply"),instant(rs.getTimestamp("create_time"))),sellerId);
        return pageReviews(all, page, size);
    }

    ReviewView review(long reviewId) {
        List<ReviewView> rows = db.query("select * from review where id=?", (rs,n)->new ReviewView(
                rs.getLong("id"),rs.getLong("order_id"),rs.getString("product_type"),rs.getLong("product_id"),
                rs.getLong("user_id"),rs.getInt("score"),rs.getString("content"),rs.getString("review_type"),
                rs.getString("seller_reply"),instant(rs.getTimestamp("create_time"))),reviewId);
        if (rows.isEmpty()) throw new OrderException("REVIEW_NOT_FOUND", "Review does not exist", 404);
        return rows.get(0);
    }

    ReviewView followUp(long userId, ApiModels.FollowUpReviewRequest request) {
        OrderView order = get(request.orderId());
        if (order.buyerUserId() != userId) {
            throw new OrderException("ORDER_ACCESS_DENIED", "Order is not owned by the caller", 403);
        }
        boolean originalExists = db.queryForObject("""
                select count(*) from review where order_id=? and product_type=? and product_id=?
                  and user_id=? and review_type='ORIGINAL'
                """, Integer.class, request.orderId(), request.productType(), request.productId(), userId) > 0;
        if (!originalExists) {
            throw new OrderException("ORIGINAL_REVIEW_REQUIRED", "An original review is required before follow-up", 409);
        }
        db.update("insert into review(order_id,product_type,product_id,user_id,score,content,review_type) values(?,?,?,?,?,?,?)",
                request.orderId(), request.productType(), request.productId(), userId, request.score(), request.content(), "FOLLOWUP");
        Long id = db.queryForObject("select max(id) from review where order_id=? and product_type=? and product_id=? and review_type='FOLLOWUP'",
                Long.class, request.orderId(), request.productType(), request.productId());
        outbox(request.orderId(), "ReviewFollowUpSubmitted.v1",
                "{\"orderId\":"+request.orderId()+",\"productId\":"+request.productId()+"}");
        return review(id);
    }

    void replyReview(long reviewId,String reply) {
        if(db.update("update review set seller_reply=?,seller_reply_time=current_timestamp,update_time=current_timestamp where id=?",reply,reviewId)!=1)
            throw new OrderException("REVIEW_NOT_FOUND","Review does not exist",404);
    }

    boolean reviewBelongsToSeller(long reviewId,long sellerId) {
        Integer count=db.queryForObject("select count(*) from review r join order_item i on i.order_id=r.order_id and i.product_type=r.product_type and i.product_id=r.product_id where r.id=? and i.seller_user_id=?",
                Integer.class,reviewId,sellerId);
        return count!=null&&count>0;
    }

    LogisticsView logistics(long orderId,String node,String desc) {
        get(orderId);
        var kh = new GeneratedKeyHolder();
        db.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into logistics_trace(order_id,node_name,status_desc) values(?,?,?)", new String[]{"id"});
            ps.setLong(1,orderId);ps.setString(2,node);ps.setString(3,desc);return ps;
        },kh);
        return logisticsTrace(kh.getKey().longValue());
    }

    LogisticsView logisticsTrace(long traceId) {
        List<LogisticsView> rows = db.query("select * from logistics_trace where id=?",
                (rs,n)->new LogisticsView(rs.getLong("id"),rs.getLong("order_id"),rs.getString("node_name"),
                        rs.getString("status_desc"),instant(rs.getTimestamp("create_time"))),traceId);
        if (rows.isEmpty()) throw new OrderException("LOGISTICS_NOT_FOUND", "Logistics trace does not exist", 404);
        return rows.get(0);
    }

    LogisticsView latestLogistics(long orderId) {
        List<LogisticsView> rows = db.query("select * from logistics_trace where order_id=? order by create_time desc,id desc limit 1",
                (rs,n)->new LogisticsView(rs.getLong("id"),rs.getLong("order_id"),rs.getString("node_name"),
                        rs.getString("status_desc"),instant(rs.getTimestamp("create_time"))),orderId);
        if (rows.isEmpty()) throw new OrderException("LOGISTICS_NOT_FOUND", "No logistics trace exists", 404);
        return rows.get(0);
    }

    List<LogisticsView> logistics(long orderId) {
        get(orderId);
        return db.query("select * from logistics_trace where order_id=? order by create_time,id",(rs,n)->new LogisticsView(
                rs.getLong("id"),rs.getLong("order_id"),rs.getString("node_name"),rs.getString("status_desc"),
                instant(rs.getTimestamp("create_time"))),orderId);
    }

    InternalSnapshot snapshot(long id) {
        OrderView o=get(id); return new InternalSnapshot(o.id(),o.orderNo(),o.buyerUserId(),o.payableAmount(),o.orderStatus(),o.items());
    }

    List<AfterSaleLogView> afterSaleLogs(long orderId) {
        get(orderId);
        return db.query("select * from order_after_sale_log where order_id=? order by create_time,id",
                (rs,n)->new AfterSaleLogView(rs.getLong("id"),rs.getLong("order_id"),rs.getString("action"),
                        (Long)rs.getObject("operator_user_id"),rs.getString("operator_role"),rs.getString("remark"),
                        instant(rs.getTimestamp("create_time"))),orderId);
    }

    void remindShip(long orderId) {
        outbox(orderId, "OrderShipmentReminded.v1", "{\"orderId\":"+orderId+"}");
    }

    String reservationId(long id) {
        return db.queryForObject("select reservation_id from order_info where id=?", String.class, id);
    }

    Long voucherId(long id) {
        return db.queryForObject("select voucher_id from order_info where id=?", Long.class, id);
    }

    String paymentRequestId(long id) {
        return db.queryForObject("select payment_request_id from order_info where id=?", String.class, id);
    }

    String refundRequestId(long id) {
        return db.queryForObject("select refund_request_id from order_info where id=?", String.class, id);
    }

    void saga(String sagaId,Long orderId,String type,String state,String step,String error) {
        String safeError=error == null ? null : error.substring(0,Math.min(500,error.length()));
        Integer existing=db.queryForObject("select count(*) from order_saga where saga_id=?",Integer.class,sagaId);
        if(existing!=null&&existing>0){
            db.update("update order_saga set state=?,failed_step=?,retry_count=retry_count+1,next_retry_time=?,last_error=?,update_time=current_timestamp where saga_id=?",
                    state,step,Timestamp.from(Instant.now().plusSeconds(30)),safeError,sagaId);
            return;
        }
        db.update("insert into order_saga(saga_id,order_id,saga_type,state,failed_step,next_retry_time,last_error) values(?,?,?,?,?,?,?)",
                sagaId,orderId,type,state,step,Timestamp.from(Instant.now().plusSeconds(30)),
                safeError);
    }

    private void outbox(long id,String type,String payload) {
        db.update("insert into outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload) values(?,?,?,?,?)",
                UUID.randomUUID().toString(),"ORDER",String.valueOf(id),type,payload);
    }

    List<OutboxMessage> pendingOutbox() {
        return db.query("select event_id,event_type,aggregate_type,aggregate_id,payload,create_time "
                        + "from outbox_event where status='PENDING' and available_at<=current_timestamp order by id limit 50",
                (rs,n)->new OutboxMessage(rs.getString("event_id"),rs.getString("event_type"),
                        rs.getString("aggregate_type"),rs.getString("aggregate_id"),rs.getString("payload"),
                        rs.getTimestamp("create_time").toInstant()));
    }

    void markPublished(String eventId) {
        db.update("update outbox_event set status='PUBLISHED',published_time=current_timestamp where event_id=? and status='PENDING'",eventId);
    }

    void markOutboxRetry(String eventId) {
        db.update("update outbox_event set attempts=attempts+1,available_at=? where event_id=? and status='PENDING'",
                Timestamp.from(Instant.now().plusSeconds(30)),eventId);
    }

    record OutboxMessage(String eventId,String eventType,String aggregateType,String aggregateId,
            String payload,Instant createdAt) {}
    private static PageView<ReviewView> pageReviews(List<ReviewView> all,long page,long size) {
        int from=(int)Math.min(all.size(),(page-1)*size), to=(int)Math.min(all.size(),from+size);
        return new PageView<>(all.size(),page,size,all.subList(from,to));
    }
    private static PageView<OrderView> filterOrders(List<OrderView> orders,long page,long size,Integer orderStatus,
            Integer refundStatus,String productType,String keyword) {
        String normalizedType=productType==null?null:productType.trim().toUpperCase();
        String normalizedKeyword=keyword==null?null:keyword.trim().toLowerCase();
        List<OrderView> filtered=orders.stream().filter(order->{
            ApiModels.PublicOrderView publicOrder=ApiModels.publicOrder(order);
            if(orderStatus!=null&&publicOrder.orderStatus()!=orderStatus)return false;
            if(refundStatus!=null&&publicOrder.refundStatus()!=refundStatus)return false;
            if(normalizedType!=null&&!normalizedType.isBlank()&&!"ALL".equals(normalizedType)
                    &&order.items().stream().noneMatch(item->normalizedType.equalsIgnoreCase(item.productType())))return false;
            if(normalizedKeyword!=null&&!normalizedKeyword.isBlank()
                    &&!order.orderNo().toLowerCase().contains(normalizedKeyword)
                    &&order.items().stream().noneMatch(item->item.productName().toLowerCase().contains(normalizedKeyword)))return false;
            return true;
        }).toList();
        int from=(int)Math.min(filtered.size(),(page-1)*size),to=(int)Math.min(filtered.size(),from+size);
        return new PageView<>(filtered.size(),page,size,filtered.subList(from,to));
    }
    private static Instant instant(Timestamp t){return t==null?null:t.toInstant();}
    private static String maskPhone(String p){return p==null||p.length()<7?"***":p.substring(0,3)+"****"+p.substring(p.length()-4);}
}
