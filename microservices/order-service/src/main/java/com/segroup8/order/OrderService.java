package com.segroup8.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.order.ApiModels.CreateOrderRequest;
import com.segroup8.order.ApiModels.ItemReviewRequest;
import com.segroup8.order.ApiModels.FollowUpReviewRequest;
import com.segroup8.order.ApiModels.LogisticsPushRequest;
import com.segroup8.order.ApiModels.LogisticsView;
import com.segroup8.order.ApiModels.OrderView;
import com.segroup8.order.ApiModels.PageView;
import com.segroup8.order.ApiModels.PayRequest;
import com.segroup8.order.ApiModels.RefundRequest;
import com.segroup8.order.ApiModels.SecondhandOrderRequest;
import com.segroup8.order.ApiModels.ShipRequest;
import com.segroup8.order.ApiModels.ReviewView;
import com.segroup8.order.DownstreamGateway.RemoteResult;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class OrderService {
    private final OrderRepository repository;
    private final DownstreamGateway downstream;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;

    OrderService(OrderRepository repository, DownstreamGateway downstream,
            TransactionTemplate transactions, ObjectMapper json) {
        this.repository = repository;
        this.downstream = downstream;
        this.transactions = transactions;
        this.json = json;
    }

    OrderView create(long buyerId, String key, CreateOrderRequest request) {
        requireKey(key);
        String hash = hash(request);
        var previous = repository.idempotentResource("CREATE_ORDER", key, hash);
        if (previous.isPresent()) return repository.get(previous.get());
        String reservationId = "reservation:" + key;
        String sagaId = "create:" + key;
        MDC.put("reservationId", reservationId); MDC.put("sagaId", sagaId);
        var reservation = downstream.reserve(reservationId, buyerId, request.items());
        BigDecimal total = reservation.items().stream()
                .map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var quote = downstream.quote("quote:" + key, buyerId, total, request.voucherId());
        try {
            Long id = transactions.execute(status -> {
                long created = repository.insertOrder(buyerId, null, request, reservation.items(), quote, reservationId);
                repository.saveIdempotency("CREATE_ORDER", key, hash, created, "CREATED");
                return created;
            });
            return repository.get(id);
        } catch (RuntimeException localFailure) {
            try {
                downstream.releaseReservation(reservationId);
            } catch (RuntimeException compensationFailure) {
                transactions.executeWithoutResult(s -> repository.saga(sagaId, null, "CREATE_ORDER",
                        "COMPENSATION_PENDING", "RELEASE_INVENTORY", compensationFailure.getMessage()));
            }
            throw localFailure;
        }
    }

    OrderView createSecondhand(SecondhandOrderRequest request) {
        String businessKey = request.tradeType() + ":" + request.tradeId();
        var existing = repository.byBusinessKey(businessKey);
        if (existing.isPresent()) return existing.get();
        try {
            Long id = transactions.execute(s -> repository.insertSecondhand(request));
            return repository.get(id);
        } catch (DataIntegrityViolationException race) {
            return repository.byBusinessKey(businessKey).orElseThrow(() -> race);
        }
    }

    OrderView getForBuyer(long orderId, long userId) {
        OrderView order = repository.get(orderId);
        if (order.buyerUserId() != userId) throw forbidden();
        return order;
    }

    OrderView getForSeller(long orderId, long sellerId) {
        OrderView order = repository.get(orderId);
        if (order.items().stream().noneMatch(i -> i.sellerUserId() != null && i.sellerUserId() == sellerId))
            throw forbidden();
        return order;
    }

    PageView<OrderView> buyerOrders(long userId,long page,long size){return repository.listBuyer(userId,page,size);}
    PageView<OrderView> sellerOrders(long userId,long page,long size){return repository.listSeller(userId,page,size);}
    PageView<OrderView> buyerOrders(long userId,long page,long size,Integer orderStatus,Integer refundStatus,String productType,String keyword){
        return repository.listBuyer(userId,page,size,orderStatus,refundStatus,productType,keyword);}
    PageView<OrderView> sellerOrders(long userId,long page,long size,Integer orderStatus,Integer refundStatus,String productType,String keyword){
        return repository.listSeller(userId,page,size,orderStatus,refundStatus,productType,keyword);}

    OrderView pay(long orderId, long buyerId, String key, PayRequest request) {
        requireKey(key); String requestHash=hash(request == null ? "" : request);
        var previous=repository.idempotentResource("PAY_ORDER",key,requestHash);
        if(previous.isPresent()) return repository.get(previous.get());
        OrderView order=getForBuyer(orderId,buyerId);
        String paymentId="payment:"+key; MDC.put("paymentRequestId",paymentId);
        PayRequest pay=request==null?new PayRequest("COIN",null):request;
        RemoteResult result;
        if (order.orderStatus()==OrderState.PENDING_PAY) {
            transactions.executeWithoutResult(s->repository.transition(orderId,order.version(),OrderState.PENDING_PAY,
                    OrderState.PAYMENT_PENDING,"payment_request_id=?",paymentId));
            result=downstream.debit(paymentId,orderId,buyerId,order.payableAmount(),pay.payMode(),pay.payChannel());
            if(result==RemoteResult.UNKNOWN) result=downstream.paymentResult(paymentId);
        } else if (order.orderStatus()==OrderState.PAYMENT_PENDING && paymentId.equals(repository.paymentRequestId(orderId))) {
            result=downstream.paymentResult(paymentId);
        } else {
            OrderState.require(order.orderStatus(),OrderState.Action.PAY);
            throw new IllegalStateException("unreachable");
        }
        if(result==RemoteResult.FAILED){
            OrderView pending=repository.get(orderId);
            transactions.executeWithoutResult(s->repository.transition(orderId,pending.version(),OrderState.PAYMENT_PENDING,
                    OrderState.PENDING_PAY,null));
            throw new OrderException("PAYMENT_FAILED","Payment was rejected",409);
        }
        if(result==RemoteResult.UNKNOWN){
            transactions.executeWithoutResult(s->repository.saga("pay:"+key,orderId,"PAYMENT","RESULT_PENDING",
                    "QUERY_DEBIT_RESULT","Finance result is unknown"));
            throw new OrderException("PAYMENT_TEMPORARILY_UNAVAILABLE","Payment result is being confirmed",503);
        }
        OrderView pending=repository.get(orderId);
        transactions.executeWithoutResult(s->{
            repository.transition(orderId,pending.version(),OrderState.PAYMENT_PENDING,OrderState.PENDING_SHIP,
                    "pay_status='PAID',pay_method=?,paid_time=current_timestamp",pay.payMode());
            repository.saveIdempotency("PAY_ORDER",key,requestHash,orderId,"PAID");
        });
        String reservationId=repository.reservationId(orderId);
        if(reservationId!=null&&!reservationId.isBlank()){
            try { downstream.confirmReservation(reservationId); }
            catch(RuntimeException ex){transactions.executeWithoutResult(s->repository.saga("confirm:"+key,orderId,
                    "PAYMENT","COMPENSATION_PENDING","CONFIRM_INVENTORY",ex.getMessage()));}
        }
        return repository.get(orderId);
    }

    OrderView cancel(long orderId,long buyerId,String key){
        requireKey(key); String hash=hash(orderId);
        var previous=repository.idempotentResource("CANCEL_ORDER",key,hash); if(previous.isPresent())return repository.get(previous.get());
        OrderView order=getForBuyer(orderId,buyerId);
        if(order.orderStatus()!=OrderState.CANCEL_PENDING){OrderState.require(order.orderStatus(),OrderState.Action.CANCEL);
            transactions.executeWithoutResult(s->repository.transition(orderId,order.version(),order.orderStatus(),OrderState.CANCEL_PENDING,null));}
        try{
            String reservationId = repository.reservationId(orderId);
            if (reservationId != null) downstream.releaseReservation(reservationId);
            downstream.releaseVoucher("voucher-release:"+key,orderId,repository.voucherId(orderId),buyerId);
        }catch(RuntimeException ex){
            transactions.executeWithoutResult(s->repository.saga("cancel:"+key,orderId,"CANCEL","COMPENSATION_PENDING","RELEASE",ex.getMessage()));
            throw new OrderException("CANCELLATION_PENDING","Cancellation compensation will be retried",503);
        }
        OrderView pending=repository.get(orderId);
        transactions.executeWithoutResult(s->{repository.transition(orderId,pending.version(),OrderState.CANCEL_PENDING,OrderState.CANCELLED,
                "closed_time=current_timestamp");repository.saveIdempotency("CANCEL_ORDER",key,hash,orderId,"CANCELLED");});
        return repository.get(orderId);
    }

    OrderView ship(long orderId,long sellerId,String key,ShipRequest request){
        requireKey(key); String requestHash=hash(List.of(orderId,request==null?"":request));
        var previous=repository.idempotentResource("SHIP_ORDER",key,requestHash);
        if(previous.isPresent())return repository.get(previous.get());
        OrderView order=getForSeller(orderId,sellerId); OrderState.require(order.orderStatus(),OrderState.Action.SHIP);
        String delivery=request==null||request.deliveryNo()==null?"SF"+System.currentTimeMillis():request.deliveryNo();
        transactions.executeWithoutResult(s->{repository.transition(orderId,order.version(),OrderState.PENDING_SHIP,OrderState.SHIPPED,
                "delivery_no=?,shipped_time=current_timestamp",delivery);repository.logistics(orderId,"已发货","包裹已由卖家发出");
                repository.saveIdempotency("SHIP_ORDER",key,requestHash,orderId,"SHIPPED");});
        return repository.get(orderId);
    }

    void remindShip(long orderId,long buyerId,String key){
        requireKey(key); String requestHash=hash(orderId);
        var previous=repository.idempotentResource("REMIND_SHIP",key,requestHash);if(previous.isPresent())return;
        OrderView order=getForBuyer(orderId,buyerId);
        if(order.orderStatus()!=OrderState.PENDING_SHIP)throw new OrderException("ORDER_NOT_WAITING_FOR_SHIPMENT","Order is not waiting for shipment",409);
        transactions.executeWithoutResult(s->{repository.remindShip(orderId);
            repository.saveIdempotency("REMIND_SHIP",key,requestHash,orderId,"QUEUED");});
    }

    OrderView confirmReceive(long orderId,long buyerId,String key){
        requireKey(key); String requestHash=hash(orderId);
        var previous=repository.idempotentResource("CONFIRM_RECEIVE",key,requestHash);
        if(previous.isPresent())return repository.get(previous.get());
        OrderView order=getForBuyer(orderId,buyerId); OrderState.require(order.orderStatus(),OrderState.Action.CONFIRM_RECEIVE);
        Map<Long,BigDecimal> sellerAmounts=new LinkedHashMap<>();
        order.items().stream().filter(i->i.sellerUserId()!=null).forEach(i->sellerAmounts.merge(i.sellerUserId(),
                i.price().multiply(BigDecimal.valueOf(i.quantity())),BigDecimal::add));
        for(var settlement:sellerAmounts.entrySet()){
            String settlementId="settlement:"+orderId+":"+settlement.getKey();
            RemoteResult result=downstream.settlementResult(settlementId);
            if(result==null)result=RemoteResult.UNKNOWN;
            if(result==RemoteResult.UNKNOWN)result=downstream.settle(settlementId,orderId,settlement.getKey(),settlement.getValue());
            if(result==null)result=RemoteResult.UNKNOWN;
            if(result==RemoteResult.UNKNOWN)result=downstream.settlementResult(settlementId);
            if(result==null)result=RemoteResult.UNKNOWN;
            if(result!=RemoteResult.SUCCEEDED){
                String resultDescription=result.name();
                transactions.executeWithoutResult(s->repository.saga("settle:"+key+":"+settlement.getKey(),orderId,"SETTLEMENT","RESULT_PENDING",
                        "QUERY_SETTLEMENT_RESULT","Finance result is "+resultDescription));
                if(result==RemoteResult.FAILED)throw new OrderException("SETTLEMENT_FAILED","Seller settlement was rejected",409);
                throw new OrderException("SETTLEMENT_TEMPORARILY_UNAVAILABLE","Seller settlement is being confirmed",503);
            }
        }
        transactions.executeWithoutResult(s->{repository.transition(orderId,order.version(),OrderState.SHIPPED,OrderState.RECEIVED,
                "received_time=current_timestamp");repository.logistics(orderId,"已签收","买家确认收货");
                repository.saveIdempotency("CONFIRM_RECEIVE",key,requestHash,orderId,"RECEIVED");}); return repository.get(orderId);
    }

    OrderView complete(long orderId,long buyerId,String key){
        requireKey(key); String requestHash=hash(orderId);
        var previous=repository.idempotentResource("COMPLETE_ORDER",key,requestHash);
        if(previous.isPresent())return repository.get(previous.get());
        OrderView order=getForBuyer(orderId,buyerId); OrderState.require(order.orderStatus(),OrderState.Action.COMPLETE);
        transactions.executeWithoutResult(s->{repository.transition(orderId,order.version(),OrderState.RECEIVED,OrderState.COMPLETED,
                "completed_time=current_timestamp");repository.saveIdempotency("COMPLETE_ORDER",key,requestHash,orderId,"COMPLETED");}); return repository.get(orderId);
    }

    OrderView requestRefund(long orderId,long buyerId,String key,RefundRequest request){
        requireKey(key); String requestHash=hash(request==null?"":request);
        var previous=repository.idempotentResource("REQUEST_REFUND",key,requestHash);if(previous.isPresent())return repository.get(previous.get());
        OrderView order=getForBuyer(orderId,buyerId); OrderState.require(order.orderStatus(),OrderState.Action.REFUND);
        String reason=request==null?null:request.reason();
        transactions.executeWithoutResult(s->{repository.refundStatus(orderId,"REQUESTED",reason,null);repository.saveIdempotency("REQUEST_REFUND",key,requestHash,orderId,"REQUESTED");}); return repository.get(orderId);
    }

    OrderView decideRefund(long orderId,long operatorId,String role,boolean approve,String key,String remark){
        requireKey(key); String requestHash=hash((approve?"APPROVE":"REJECT")+orderId+String.valueOf(remark));
        var previous=repository.idempotentResource("DECIDE_REFUND",key,requestHash);if(previous.isPresent())return repository.get(previous.get());
        OrderView order=repository.get(orderId);
        if(!"REQUESTED".equals(order.refundStatus())&&!"REFUND_PENDING".equals(order.refundStatus())) throw new OrderException("INVALID_REFUND_STATE","Refund is not awaiting a decision",409);
        if(!"ADMIN".equals(role)) getForSeller(orderId,operatorId);
        if(!approve){transactions.executeWithoutResult(s->{repository.refundStatus(orderId,"REJECTED",null,null);
            repository.afterSale(orderId,"REJECT",operatorId,role,remark);repository.saveIdempotency("DECIDE_REFUND",key,requestHash,orderId,"REJECTED");});return repository.get(orderId);}
        String refundId="refund:"+key;
        RemoteResult result;
        if("REQUESTED".equals(order.refundStatus())){
            transactions.executeWithoutResult(s->repository.refundStatus(orderId,"REFUND_PENDING",null,refundId));
            result=downstream.refund(refundId,repository.paymentRequestId(orderId),orderId,
                    order.buyerUserId(),order.payableAmount());
            if(result==RemoteResult.UNKNOWN)result=downstream.refundResult(refundId);
        }else if(refundId.equals(repository.refundRequestId(orderId))){result=downstream.refundResult(refundId);}
        else throw new OrderException("REFUND_ALREADY_IN_PROGRESS","Refund is in progress under another request",409);
        if(result!=RemoteResult.SUCCEEDED){
            String resultDescription = result.name();
            transactions.executeWithoutResult(s->repository.saga("refund:"+key,orderId,"REFUND",
                "RESULT_PENDING","QUERY_REFUND_RESULT","Finance result is "+resultDescription));
            throw new OrderException("REFUND_TEMPORARILY_UNAVAILABLE","Refund remains pending and can be retried",503);}
        transactions.executeWithoutResult(s->{repository.refundStatus(orderId,"REFUNDED",null,refundId);
            repository.afterSale(orderId,"APPROVE",operatorId,role,remark);repository.saveIdempotency("DECIDE_REFUND",key,requestHash,orderId,"REFUNDED");});return repository.get(orderId);
    }

    OrderView review(long orderId,long buyerId,String key,List<ItemReviewRequest> reviews){
        requireKey(key); String requestHash=hash(List.of(orderId,reviews));
        var previous=repository.idempotentResource("REVIEW_ORDER",key,requestHash);
        if(previous.isPresent())return repository.get(previous.get());
        OrderView order=getForBuyer(orderId,buyerId);
        if(order.orderStatus()!=OrderState.RECEIVED)throw new OrderException("ORDER_NOT_REVIEWABLE","Order must be received before review",409);
        for(ItemReviewRequest review:reviews){boolean belongs=order.items().stream().anyMatch(i->i.productId()==review.productId()&&i.productType().equals(review.productType()));
            if(!belongs)throw new OrderException("ITEM_NOT_IN_ORDER","Review item is not in the order",400);}
        transactions.executeWithoutResult(s->{reviews.forEach(r->repository.review(orderId,buyerId,r,"ORIGINAL"));
            OrderView current=repository.get(orderId);repository.transition(orderId,current.version(),OrderState.RECEIVED,OrderState.COMPLETED,"completed_time=current_timestamp");
            repository.saveIdempotency("REVIEW_ORDER",key,requestHash,orderId,"REVIEWED");});
        return repository.get(orderId);
    }

    ReviewView followUp(long buyerId,String key,FollowUpReviewRequest request){
        requireKey(key); String requestHash=hash(request);
        var previous=repository.idempotentResource("FOLLOWUP_REVIEW",key,requestHash);
        if(previous.isPresent())return repository.review(previous.get());
        return transactions.execute(s->{ReviewView created=repository.followUp(buyerId,request);
            repository.saveIdempotency("FOLLOWUP_REVIEW",key,requestHash,created.id(),"CREATED");return created;});
    }

    void replyReview(long reviewId,long sellerId,String key,String reply){
        requireKey(key);if(reply==null||reply.isBlank())throw new OrderException("INVALID_REVIEW_REPLY","Reply is required",400);
        String requestHash=hash(List.of(reviewId,reply));
        var previous=repository.idempotentResource("REPLY_REVIEW",key,requestHash);if(previous.isPresent())return;
        if(!repository.reviewBelongsToSeller(reviewId,sellerId))throw new OrderException("REVIEW_ACCESS_DENIED","Review does not belong to seller",403);
        transactions.executeWithoutResult(s->{repository.replyReview(reviewId,reply);
            repository.saveIdempotency("REPLY_REVIEW",key,requestHash,reviewId,"REPLIED");});
    }

    List<LogisticsView> logistics(long orderId,long userId){
        OrderView order=repository.get(orderId);
        if(order.buyerUserId()!=userId&&order.items().stream().noneMatch(i->i.sellerUserId()!=null&&i.sellerUserId()==userId))throw forbidden();
        return repository.logistics(orderId);
    }

    LogisticsView pushLogistics(LogisticsPushRequest request,long sellerId,String key){
        requireKey(key);if(request.nodeName()==null||request.nodeName().isBlank()||request.statusDesc()==null||request.statusDesc().isBlank())
            throw new OrderException("INVALID_LOGISTICS_TRACE","Logistics node and description are required",400);
        String requestHash=hash(request);var previous=repository.idempotentResource("PUSH_LOGISTICS",key,requestHash);
        if(previous.isPresent())return repository.logisticsTrace(previous.get());
        getForSeller(request.orderId(),sellerId);
        return transactions.execute(s->{LogisticsView created=repository.logistics(request.orderId(),request.nodeName(),request.statusDesc());
            repository.saveIdempotency("PUSH_LOGISTICS",key,requestHash,created.id(),"APPENDED");return created;});
    }

    LogisticsView pushNextLogistics(long orderId,long sellerId,String key){
        requireKey(key);String requestHash=hash(orderId);var previous=repository.idempotentResource("PUSH_NEXT_LOGISTICS",key,requestHash);
        if(previous.isPresent())return repository.logisticsTrace(previous.get());
        getForSeller(orderId,sellerId);int step=repository.logistics(orderId).size()+1;
        String node="运输节点 "+step;
        return transactions.execute(s->{LogisticsView created=repository.logistics(orderId,node,"包裹运输状态已更新");
            repository.saveIdempotency("PUSH_NEXT_LOGISTICS",key,requestHash,created.id(),"APPENDED");return created;});
    }

    void closeAdmin(long orderId){OrderView o=repository.get(orderId);if(o.orderStatus()==OrderState.CANCELLED||o.orderStatus()==OrderState.COMPLETED)return;
        transactions.executeWithoutResult(s->repository.transition(orderId,o.version(),o.orderStatus(),OrderState.CANCELLED,"closed_time=current_timestamp"));}

    void closeAdminBatch(List<Long> orderIds,String key){
        requireKey(key);String requestHash=hash(orderIds);
        var previous=repository.idempotentResource("ADMIN_BATCH_CLOSE",key,requestHash);if(previous.isPresent())return;
        orderIds.forEach(this::closeAdmin);
        long resourceId=orderIds.get(0);
        transactions.executeWithoutResult(s->repository.saveIdempotency("ADMIN_BATCH_CLOSE",key,requestHash,resourceId,"CLOSED"));
    }

    private void requireKey(String key){if(key==null||key.isBlank()||key.length()>160)throw new OrderException("IDEMPOTENCY_KEY_REQUIRED","A valid Idempotency-Key header is required",400);}
    private String hash(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(value).getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException|JsonProcessingException e){throw new IllegalStateException(e);}}
    private OrderException forbidden(){return new OrderException("ORDER_ACCESS_DENIED","Order is not owned by the caller",403);}
}
