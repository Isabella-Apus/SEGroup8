package com.segroup8.secondhand.service;

import com.segroup8.secondhand.api.AuctionCreateRequest;
import com.segroup8.secondhand.api.AuctionView;
import com.segroup8.secondhand.api.BargainApplyRequest;
import com.segroup8.secondhand.api.BargainConfirmRequest;
import com.segroup8.secondhand.api.NegotiationView;
import com.segroup8.secondhand.api.TradeOrderView;
import com.segroup8.secondhand.common.DomainException;
import com.segroup8.secondhand.common.PageResponse;
import com.segroup8.secondhand.client.IdentityAddressNotFoundException;
import com.segroup8.secondhand.client.IdentityGateway;
import com.segroup8.secondhand.client.IdentityGateway.AddressSnapshot;
import com.segroup8.secondhand.client.IdentityServiceUnavailableException;
import com.segroup8.secondhand.domain.OrderCreationSnapshot;
import com.segroup8.secondhand.domain.ProductAuction;
import com.segroup8.secondhand.domain.ProductNegotiation;
import com.segroup8.secondhand.domain.SecondhandProduct;
import com.segroup8.secondhand.domain.TradeOrderRequest;
import com.segroup8.secondhand.repository.AuctionRepository;
import com.segroup8.secondhand.repository.IdempotencyRepository;
import com.segroup8.secondhand.repository.NegotiationRepository;
import com.segroup8.secondhand.repository.OutboxRepository;
import com.segroup8.secondhand.repository.ProductRepository;
import com.segroup8.secondhand.repository.TradeOrderRequestRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TradeApplicationService {
    private static final Logger log = LoggerFactory.getLogger(TradeApplicationService.class);
    private final ProductRepository products;
    private final NegotiationRepository negotiations;
    private final AuctionRepository auctions;
    private final TradeOrderRequestRepository requests;
    private final IdempotencyRepository idempotency;
    private final OutboxRepository outbox;
    private final TradeOrderCoordinator coordinator;
    private final IdentityGateway identityGateway;
    private final TransactionTemplate transactions;

    public TradeApplicationService(ProductRepository products, NegotiationRepository negotiations,
            AuctionRepository auctions, TradeOrderRequestRepository requests,
            IdempotencyRepository idempotency, OutboxRepository outbox,
            TradeOrderCoordinator coordinator, IdentityGateway identityGateway,
            TransactionTemplate transactions) {
        this.products = products;
        this.negotiations = negotiations;
        this.auctions = auctions;
        this.requests = requests;
        this.idempotency = idempotency;
        this.outbox = outbox;
        this.coordinator = coordinator;
        this.identityGateway = identityGateway;
        this.transactions = transactions;
    }

    public TradeOrderView buy(long buyerId, long productId, long addressId, String remark) {
        TradeOrderRequest request = transactions.execute(status -> {
            SecondhandProduct product = requireProduct(productId);
            TradeOrderRequest existing = requests.findActiveForProduct("DIRECT_BUY", productId).orElse(null);
            if (existing != null) {
                if (existing.buyerUserId() != buyerId) {
                    throw DomainException.conflict("PRODUCT_SOLD", "商品已被其他买家锁定");
                }
                return existing;
            }
            ensureTradableBy(product, buyerId);
            if (auctions.hasActiveAuction(productId)) {
                throw DomainException.conflict("AUCTION_ACTIVE", "商品正在拍卖中，不能直接购买");
            }
            if (products.compareAndSetStatus(productId, SecondhandProduct.ON_SHELF,
                    SecondhandProduct.TRADE_PENDING) == 0) {
                throw DomainException.conflict("PRODUCT_SOLD", "商品已被购买或状态已变化");
            }
            String tradeId = productId + "-v" + (product.version() + 1);
            TradeOrderRequest created = requests.createOrFind("DIRECT_BUY", tradeId,
                    businessKey("DIRECT_BUY", tradeId), productId, buyerId, product.sellerUserId(),
                    product.salePrice(), orderSnapshot(product, buyerId, addressId), remark);
            appendOrderRequested(created);
            return created;
        });
        return coordinator.toView(coordinator.dispatch(request.id()));
    }

    public NegotiationView applyBargain(long buyerId, BargainApplyRequest command) {
        ProductNegotiation negotiation = transactions.execute(status -> {
            SecondhandProduct product = requireProduct(command.productId());
            ensureTradableBy(product, buyerId);
            if (product.negotiable() != 1) {
                throw DomainException.conflict("BARGAIN_DISABLED", "卖家未开启议价");
            }
            if (product.sellerUserId() != command.sellerUserId()) {
                throw DomainException.badRequest("SELLER_MISMATCH", "商品卖家信息不一致");
            }
            if (command.proposedPrice().compareTo(product.salePrice()) > 0) {
                throw DomainException.badRequest("BARGAIN_PRICE_INVALID", "议价金额不能高于当前售价");
            }
            ProductNegotiation active = negotiations.findActive(product.id(), buyerId).orElse(null);
            if (active != null) {
                return active;
            }
            String key = product.id() + ":" + buyerId;
            if (!idempotency.recordOnce("BARGAIN_APPLY", key, "pending")) {
                return negotiations.findActive(product.id(), buyerId)
                        .orElseThrow(() -> DomainException.conflict("BARGAIN_CONFLICT", "议价申请正在处理中"));
            }
            long id = negotiations.insert(product.id(), buyerId, product.sellerUserId(), command.proposedPrice());
            outbox.append("NEGOTIATION", id, "BargainApplied.v1", Map.of(
                    "negotiationId", id, "productId", product.id(), "buyerId", buyerId,
                    "sellerId", product.sellerUserId(), "proposedPrice", command.proposedPrice()));
            outbox.append("NEGOTIATION", id, "NotificationRequested.v1", Map.of(
                    "recipientIds", List.of(product.sellerUserId()), "type", "BARGAIN_APPLIED",
                    "dedupeKey", "BARGAIN_APPLIED:" + id));
            return requireNegotiation(id);
        });
        return toNegotiationView(negotiation);
    }

    public NegotiationView confirmBargain(long sellerId, BargainConfirmRequest command) {
        TradeOrderRequest orderRequest = transactions.execute(status -> {
            ProductNegotiation negotiation = requireNegotiation(command.negotiationId());
            if (negotiation.sellerUserId() != sellerId) {
                throw DomainException.forbidden("OWNERSHIP_REQUIRED", "只有商品卖家可以确认议价");
            }
            TradeOrderRequest existing = requests.findByTrade("BARGAIN", String.valueOf(negotiation.id())).orElse(null);
            if (existing != null) {
                return existing;
            }
            SecondhandProduct product = requireProduct(negotiation.productId());
            if (command.confirmedPrice().compareTo(product.salePrice()) > 0) {
                throw DomainException.badRequest("BARGAIN_PRICE_INVALID", "确认价格不能高于当前售价");
            }
            if (negotiations.beginAccepting(negotiation.id(), sellerId, negotiation.version(),
                    command.confirmedPrice(), LocalDateTime.now().plusHours(24)) == 0) {
                throw DomainException.conflict("BARGAIN_CONFLICT", "议价已处理或被其他操作修改");
            }
            if (products.compareAndSetStatus(product.id(), SecondhandProduct.ON_SHELF,
                    SecondhandProduct.TRADE_PENDING) == 0) {
                throw DomainException.conflict("PRODUCT_SOLD", "商品已被购买或状态已变化");
            }
            String tradeId = String.valueOf(negotiation.id());
            TradeOrderRequest created = requests.createOrFind("BARGAIN", tradeId, businessKey("BARGAIN", tradeId),
                    product.id(), negotiation.buyerUserId(), sellerId, command.confirmedPrice(),
                    orderSnapshot(product, negotiation.buyerUserId(), null),
                    "议价确认生成二手待付款订单");
            appendOrderRequested(created);
            return created;
        });
        coordinator.dispatch(orderRequest.id());
        return toNegotiationView(requireNegotiation(command.negotiationId()));
    }

    public NegotiationView rejectBargain(long sellerId, long negotiationId) {
        ProductNegotiation rejected = transactions.execute(status -> {
            ProductNegotiation negotiation = requireNegotiation(negotiationId);
            if (negotiation.sellerUserId() != sellerId) {
                throw DomainException.forbidden("OWNERSHIP_REQUIRED", "只有商品卖家可以拒绝议价");
            }
            if (negotiations.reject(negotiation.id(), sellerId, negotiation.version()) == 0) {
                throw DomainException.conflict("BARGAIN_CONFLICT", "议价已处理，不能重复拒绝");
            }
            idempotency.release("BARGAIN_APPLY", negotiation.productId() + ":" + negotiation.buyerUserId());
            outbox.append("NEGOTIATION", negotiation.id(), "BargainRejected.v1", Map.of(
                    "negotiationId", negotiation.id(), "productId", negotiation.productId(),
                    "buyerId", negotiation.buyerUserId(), "sellerId", sellerId));
            return requireNegotiation(negotiation.id());
        });
        return toNegotiationView(rejected);
    }

    public PageResponse<NegotiationView> listBargains(long userId, long pageNum, long pageSize,
            Long productId, Long counterpartId, String status) {
        validatePage(pageNum, pageSize);
        var page = negotiations.listForUser(userId, pageNum, pageSize, productId, counterpartId, status);
        return new PageResponse<>(page.total(), pageNum, pageSize,
                page.records().stream().map(this::toNegotiationView).toList());
    }

    public NegotiationView effectiveBargain(long buyerId, long productId) {
        return negotiations.findEffective(productId, buyerId).map(this::toNegotiationView).orElse(null);
    }

    public AuctionView createAuction(long sellerId, AuctionCreateRequest command) {
        ProductAuction auction = transactions.execute(status -> {
            SecondhandProduct product = requireProduct(command.productId());
            if (!product.ownedBy(sellerId)) {
                throw DomainException.forbidden("OWNERSHIP_REQUIRED", "只能拍卖自己发布的商品");
            }
            if (!product.publiclyTradable()) {
                throw DomainException.conflict("PRODUCT_STATE_CONFLICT", "只有审核通过的在售商品可以发起拍卖");
            }
            if (command.startPrice().compareTo(product.salePrice()) > 0) {
                throw DomainException.badRequest("AUCTION_PRICE_INVALID", "起拍价不能高于当前售价");
            }
            if (!idempotency.recordOnce("ACTIVE_AUCTION", String.valueOf(product.id()), "pending")) {
                return auctions.findLatestByProduct(product.id())
                        .filter(current -> List.of("ONGOING", "SETTLING").contains(current.status()))
                        .orElseThrow(() -> DomainException.conflict("AUCTION_CONFLICT", "拍卖创建正在处理中"));
            }
            LocalDateTime now = LocalDateTime.now();
            long id = auctions.insert(product.id(), sellerId, command.startPrice(), command.incrementAmount(),
                    now, now.plusMinutes(command.durationMinutes()));
            outbox.append("AUCTION", id, "AuctionCreated.v1", Map.of(
                    "auctionId", id, "productId", product.id(), "sellerId", sellerId,
                    "startPrice", command.startPrice(), "endTime", now.plusMinutes(command.durationMinutes()).toString()));
            return requireAuction(id);
        });
        return toAuctionView(auction);
    }

    public AuctionView auctionByProduct(long productId) {
        return toAuctionView(auctions.findLatestByProduct(productId)
                .orElseThrow(() -> DomainException.notFound("AUCTION_NOT_FOUND", "该商品暂无拍卖")));
    }

    public PageResponse<AuctionView> sellerAuctions(long sellerId, long pageNum, long pageSize, String status) {
        validatePage(pageNum, pageSize);
        var page = auctions.listSeller(sellerId, pageNum, pageSize, status);
        return new PageResponse<>(page.total(), pageNum, pageSize,
                page.records().stream().map(this::toAuctionView).toList());
    }

    public AuctionView placeBid(long bidderId, String bidderName, long auctionId, BigDecimal amount) {
        ProductAuction updated = transactions.execute(status -> {
            ProductAuction auction = requireAuction(auctionId);
            if (!"ONGOING".equals(auction.status()) || !auction.endTime().isAfter(LocalDateTime.now())) {
                throw DomainException.conflict("AUCTION_CLOSED", "拍卖已结束");
            }
            if (auction.sellerUserId() == bidderId) {
                throw DomainException.forbidden("FORBIDDEN_SELF_BID", "卖家不能参与自己商品的竞拍");
            }
            if (auction.currentBidderUserId() != null && auction.currentBidderUserId() == bidderId) {
                throw DomainException.conflict("AUCTION_ALREADY_LEADING", "您当前已经是最高出价者");
            }
            BigDecimal minimum = auction.currentBidderUserId() == null ? auction.startPrice()
                    : auction.currentPrice().add(auction.incrementAmount());
            if (amount.compareTo(minimum) < 0) {
                throw DomainException.badRequest("BID_TOO_LOW", "出价不得低于 " + minimum);
            }
            if (auctions.placeBid(auction, bidderId, amount) == 0) {
                throw DomainException.conflict("BID_CONFLICT", "已有新的出价，请刷新后重试");
            }
            auctions.insertBid(auction.id(), auction.productId(), bidderId, bidderName, amount);
            outbox.append("AUCTION", auction.id(), "AuctionBidPlaced.v1", Map.of(
                    "auctionId", auction.id(), "productId", auction.productId(),
                    "bidderId", bidderId, "bidAmount", amount));
            return requireAuction(auction.id());
        });
        log.info("auction bid accepted auctionId={} productId={} bidderId={} bidAmount={}",
                updated.id(), updated.productId(), bidderId, amount);
        return toAuctionView(updated);
    }

    public AuctionView closeAuction(long sellerId, long auctionId) {
        settleAuction(auctionId, sellerId, true);
        return toAuctionView(requireAuction(auctionId));
    }

    public AuctionView markAuctionFlow(long sellerId, long auctionId) {
        transactions.executeWithoutResult(status -> {
            ProductAuction auction = requireOwnedAuction(sellerId, auctionId);
            if (auction.currentBidderUserId() != null) {
                throw DomainException.conflict("AUCTION_HAS_BIDS", "已有有效出价，应结束拍卖并按最高价成交");
            }
            if (auctions.markFlow(auction) == 0) {
                throw DomainException.conflict("AUCTION_CONFLICT", "拍卖状态已变化");
            }
            idempotency.release("ACTIVE_AUCTION", String.valueOf(auction.productId()));
            outbox.append("AUCTION", auction.id(), "AuctionFlow.v1",
                    Map.of("auctionId", auction.id(), "productId", auction.productId()));
        });
        return toAuctionView(requireAuction(auctionId));
    }

    public void settleDueAuctions(int limit) {
        for (ProductAuction auction : auctions.findDue(limit)) {
            try {
                settleAuction(auction.id(), null, false);
            } catch (RuntimeException exception) {
                log.error("auction settlement failed auctionId={} productId={}",
                        auction.id(), auction.productId(), exception);
            }
        }
    }

    private void settleAuction(long auctionId, Long sellerId, boolean early) {
        TradeOrderRequest request = transactions.execute(status -> {
            ProductAuction auction = requireAuction(auctionId);
            if (sellerId != null && auction.sellerUserId() != sellerId) {
                throw DomainException.forbidden("OWNERSHIP_REQUIRED", "只能结束自己发起的拍卖");
            }
            TradeOrderRequest existing = requests.findByTrade("AUCTION", String.valueOf(auction.id())).orElse(null);
            if (existing != null) {
                return existing;
            }
            if (!"ONGOING".equals(auction.status())) {
                if ("FLOW".equals(auction.status()) || "FINISHED".equals(auction.status())) return null;
                throw DomainException.conflict("AUCTION_CONFLICT", "拍卖正在结算中");
            }
            if (!early && auction.endTime().isAfter(LocalDateTime.now())) {
                return null;
            }
            if (auction.currentBidderUserId() == null) {
                if (auctions.markFlow(auction) == 0) {
                    throw DomainException.conflict("AUCTION_CONFLICT", "拍卖状态已变化");
                }
                idempotency.release("ACTIVE_AUCTION", String.valueOf(auction.productId()));
                outbox.append("AUCTION", auction.id(), "AuctionFlow.v1",
                        Map.of("auctionId", auction.id(), "productId", auction.productId()));
                return null;
            }
            if (auctions.beginSettlement(auction) == 0) {
                throw DomainException.conflict("AUCTION_CONFLICT", "拍卖状态已变化");
            }
            SecondhandProduct product = requireProduct(auction.productId());
            if (products.compareAndSetStatus(auction.productId(), SecondhandProduct.ON_SHELF,
                    SecondhandProduct.TRADE_PENDING) == 0) {
                throw DomainException.conflict("PRODUCT_STATE_CONFLICT", "商品状态已变化，不能结算拍卖");
            }
            String tradeId = String.valueOf(auction.id());
            TradeOrderRequest created = requests.createOrFind("AUCTION", tradeId, businessKey("AUCTION", tradeId),
                    auction.productId(), auction.currentBidderUserId(), auction.sellerUserId(),
                    auction.currentPrice(), orderSnapshot(product, auction.currentBidderUserId(), null),
                    "拍卖成交生成二手待付款订单");
            appendOrderRequested(created);
            return created;
        });
        if (request != null) {
            TradeOrderRequest dispatched = coordinator.dispatch(request.id());
            log.info("auction settlement dispatched auctionId={} productId={} tradeType={} tradeId={} "
                            + "orderBusinessKey={} requestStatus={}",
                    auctionId, dispatched.productId(), dispatched.tradeType(), dispatched.tradeId(),
                    dispatched.orderBusinessKey(), dispatched.requestStatus());
        }
    }

    private NegotiationView toNegotiationView(ProductNegotiation negotiation) {
        TradeOrderRequest request = requests.findByTrade("BARGAIN", String.valueOf(negotiation.id())).orElse(null);
        return new NegotiationView(negotiation.id(), negotiation.productId(), negotiation.buyerUserId(),
                negotiation.sellerUserId(), negotiation.proposedPrice(), negotiation.confirmedPrice(),
                negotiation.status(), negotiation.effectiveFrom(), negotiation.effectiveUntil(),
                negotiation.usedOrderId(), request == null ? null : request.requestStatus());
    }

    private OrderCreationSnapshot orderSnapshot(SecondhandProduct product, long buyerId, Long addressId) {
        try {
            AddressSnapshot address = identityGateway.resolveAddress(buyerId, addressId);
            if (address.userId() != buyerId) {
                throw new IdentityAddressNotFoundException("address ownership mismatch");
            }
            return new OrderCreationSnapshot(address.addressId(), product.name(), address.receiverName(),
                    address.receiverPhone(), address.province(), address.city(), address.detailAddress());
        } catch (IdentityAddressNotFoundException notFound) {
            throw DomainException.badRequest("ADDRESS_INVALID", "收货地址不存在或不属于当前买家");
        } catch (IdentityServiceUnavailableException unavailable) {
            throw DomainException.unavailable("IDENTITY_SERVICE_UNAVAILABLE",
                    "收货地址暂时无法确认，请稍后重试");
        }
    }

    private AuctionView toAuctionView(ProductAuction auction) {
        SecondhandProduct product = products.findById(auction.productId()).orElse(null);
        var bids = auctions.listBids(auction.id());
        String bidderName = bids.stream().filter(bid -> auction.currentBidderUserId() != null
                        && bid.bidderUserId() == auction.currentBidderUserId())
                .map(bid -> bid.bidderNameSnapshot()).findFirst().orElse(null);
        TradeOrderRequest request = requests.findByTrade("AUCTION", String.valueOf(auction.id())).orElse(null);
        return new AuctionView(auction.id(), auction.productId(), product == null ? null : product.name(),
                auction.sellerUserId(), auction.startPrice(), auction.incrementAmount(), auction.currentPrice(),
                auction.currentBidderUserId(), bidderName, auction.startTime(), auction.endTime(), auction.status(),
                auctionStatusName(auction.status()), auction.settledOrderId(), auctions.countBids(auction.id()),
                bids.stream().map(bid -> new AuctionView.BidView(bid.id(), bid.bidderUserId(),
                        bid.bidderNameSnapshot(), bid.bidAmount(), bid.status(), bid.createTime())).toList(),
                request == null ? null : request.requestStatus());
    }

    private void appendOrderRequested(TradeOrderRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tradeType", request.tradeType());
        payload.put("tradeId", request.tradeId());
        payload.put("productId", request.productId());
        payload.put("buyerId", request.buyerUserId());
        payload.put("sellerId", request.sellerUserId());
        payload.put("price", request.price());
        payload.put("orderBusinessKey", request.orderBusinessKey());
        outbox.append("SECONDHAND_TRADE", request.tradeId(), "SecondhandOrderRequested.v1", payload);
    }

    private void ensureTradableBy(SecondhandProduct product, long buyerId) {
        if (product.sellerUserId() == buyerId) {
            throw DomainException.forbidden("FORBIDDEN_SELF_PURCHASE", "不能购买自己发布的二手商品");
        }
        if (!product.publiclyTradable()) {
            throw DomainException.conflict("PRODUCT_STATE_CONFLICT", "商品已下架、成交中或尚未通过审核");
        }
    }

    private ProductAuction requireOwnedAuction(long sellerId, long auctionId) {
        ProductAuction auction = requireAuction(auctionId);
        if (auction.sellerUserId() != sellerId) {
            throw DomainException.forbidden("OWNERSHIP_REQUIRED", "只能管理自己发起的拍卖");
        }
        return auction;
    }

    private ProductAuction requireAuction(long id) {
        return auctions.findById(id)
                .orElseThrow(() -> DomainException.notFound("AUCTION_NOT_FOUND", "拍卖不存在"));
    }

    private ProductNegotiation requireNegotiation(long id) {
        return negotiations.findById(id)
                .orElseThrow(() -> DomainException.notFound("NEGOTIATION_NOT_FOUND", "议价记录不存在"));
    }

    private SecondhandProduct requireProduct(long id) {
        return products.findById(id)
                .orElseThrow(() -> DomainException.notFound("PRODUCT_NOT_FOUND", "二手商品不存在"));
    }

    private void validatePage(long pageNum, long pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw DomainException.badRequest("PAGE_INVALID", "页码必须大于0且每页最多100条");
        }
    }

    private String businessKey(String type, String tradeId) {
        return "SECONDHAND:" + type + ":" + tradeId;
    }

    private String auctionStatusName(String status) {
        return switch (status) {
            case "ONGOING" -> "进行中";
            case "SETTLING" -> "结算中";
            case "FINISHED" -> "已成交";
            case "FLOW" -> "已流拍";
            default -> status;
        };
    }
}
