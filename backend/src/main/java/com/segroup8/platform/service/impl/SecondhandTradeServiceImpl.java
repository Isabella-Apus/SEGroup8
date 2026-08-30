package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.common.TransactionTradeTypeEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.AuctionBidRequest;
import com.segroup8.platform.dto.AuctionCreateRequest;
import com.segroup8.platform.dto.BargainApplyRequest;
import com.segroup8.platform.dto.BargainConfirmRequest;
import com.segroup8.platform.entity.AuctionLog;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.ProductAuction;
import com.segroup8.platform.entity.ProductNegotiation;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.event.EventTypes;
import com.segroup8.platform.event.ProducerOutboxService;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.AuctionLogMapper;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ProductAuctionMapper;
import com.segroup8.platform.mapper.ProductNegotiationMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.realtime.RealtimeEventTypes;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.service.ChatService;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.service.SecondhandTradeService;
import com.segroup8.platform.service.settlement.EscrowSettlementService;
import com.segroup8.platform.vo.AuctionLogVO;
import com.segroup8.platform.vo.ChatConversationVO;
import com.segroup8.platform.vo.PageVO;
import com.segroup8.platform.vo.ProductAuctionVO;
import com.segroup8.platform.vo.ProductNegotiationVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class SecondhandTradeServiceImpl implements SecondhandTradeService {

    private static final Logger log = LoggerFactory.getLogger(SecondhandTradeServiceImpl.class);

    private static final String NEGOTIATION_APPLIED = "APPLIED";
    private static final String NEGOTIATION_CONFIRMED = "CONFIRMED";
    private static final String NEGOTIATION_REJECTED = "REJECTED";
    private static final String NEGOTIATION_USED = "USED";

    private static final String AUCTION_ONGOING = "ONGOING";
    private static final String AUCTION_SETTLING = "SETTLING";
    private static final String AUCTION_FINISHED = "FINISHED";
    private static final String AUCTION_FLOW = "FLOW";

    private static final int SECONDHAND_ON_SHELF = 1;
    private static final int SECONDHAND_OFF_SHELF = 2;
    private static final int SECONDHAND_SOLD = 3;
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ProductNegotiationMapper productNegotiationMapper;
    private final ProductAuctionMapper productAuctionMapper;
    private final AuctionLogMapper auctionLogMapper;
    private final SecondhandProductMapper secondhandProductMapper;
    private final UserMapper userMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ChatService chatService;
    private final RealtimePushService realtimePushService;
    private final NotificationService notificationService;
    private final ProducerOutboxService outbox;
    private final EscrowSettlementService escrowSettlementService;

    public SecondhandTradeServiceImpl(ProductNegotiationMapper productNegotiationMapper,
            ProductAuctionMapper productAuctionMapper,
            AuctionLogMapper auctionLogMapper,
            SecondhandProductMapper secondhandProductMapper,
            UserMapper userMapper,
            OrderInfoMapper orderInfoMapper,
            OrderItemMapper orderItemMapper,
            ChatService chatService,
            RealtimePushService realtimePushService,
            NotificationService notificationService,
            EscrowSettlementService escrowSettlementService,
            ProducerOutboxService outbox) {
        this.productNegotiationMapper = productNegotiationMapper;
        this.productAuctionMapper = productAuctionMapper;
        this.auctionLogMapper = auctionLogMapper;
        this.secondhandProductMapper = secondhandProductMapper;
        this.userMapper = userMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.chatService = chatService;
        this.realtimePushService = realtimePushService;
        this.notificationService = notificationService;
        this.outbox = outbox;
        this.escrowSettlementService = escrowSettlementService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductNegotiationVO applyBargain(BargainApplyRequest request) {
        Long buyerUserId = requireUserId();
        if (request == null || request.getProductId() == null || request.getSellerUserId() == null
                || request.getProposedPrice() == null) {
            throw new BusinessException(400, "议价参数不完整");
        }
        SecondhandProduct product = secondhandProductMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException(404, "二手商品不存在");
        }
        if (!Objects.equals(product.getSellerUserId(), request.getSellerUserId())) {
            throw new BusinessException(400, "卖家与商品不匹配");
        }
        if (Objects.equals(product.getSellerUserId(), buyerUserId)) {
            throw new BusinessException(400, "不能给自己的商品议价");
        }
        if (!Objects.equals(product.getStatus(), SECONDHAND_ON_SHELF)) {
            throw new BusinessException(400, "商品已下架，无法议价");
        }
        if (!Objects.equals(product.getIsNegotiable(), 1)) {
            throw new BusinessException(400, "该商品不支持议价");
        }
        if (request.getProposedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "议价金额必须大于0");
        }
        if (request.getProposedPrice().compareTo(product.getSalePrice()) > 0) {
            throw new BusinessException(400, "议价金额不能高于商品当前售价");
        }
        long activeCount = productNegotiationMapper.selectCount(new LambdaQueryWrapper<ProductNegotiation>()
                .eq(ProductNegotiation::getProductId, request.getProductId())
                .eq(ProductNegotiation::getBuyerUserId, buyerUserId)
                .and(wrapper -> wrapper.eq(ProductNegotiation::getStatus, NEGOTIATION_APPLIED)
                        .or(confirmed -> confirmed.eq(ProductNegotiation::getStatus, NEGOTIATION_CONFIRMED)
                                .isNull(ProductNegotiation::getUsedOrderId)
                                .ge(ProductNegotiation::getEffectiveUntil, LocalDateTime.now()))));
        if (activeCount > 0) {
            throw new BusinessException(409, "已有待处理或生效中的议价，请勿重复申请");
        }

        ProductNegotiation negotiation = new ProductNegotiation();
        negotiation.setProductId(request.getProductId());
        negotiation.setBuyerUserId(buyerUserId);
        negotiation.setSellerUserId(request.getSellerUserId());
        negotiation.setProposedPrice(request.getProposedPrice());
        negotiation.setStatus(NEGOTIATION_APPLIED);
        productNegotiationMapper.insert(negotiation);
        notificationService.createNotification(negotiation.getSellerUserId(),
                "收到新的议价申请",
                "商品“" + product.getName() + "”收到议价 " + negotiation.getProposedPrice(),
                "/merchant/messages");
        runAfterCommit(() -> publishBargainApply(negotiation, product));

        return toNegotiationVO(negotiation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductNegotiationVO confirmBargain(BargainConfirmRequest request) {
        Long sellerUserId = requireUserId();
        if (request == null || request.getNegotiationId() == null || request.getConfirmedPrice() == null) {
            throw new BusinessException(400, "确认议价参数不完整");
        }
        ProductNegotiation negotiation = productNegotiationMapper.selectById(request.getNegotiationId());
        if (negotiation == null) {
            throw new BusinessException(404, "议价记录不存在");
        }
        if (!Objects.equals(negotiation.getSellerUserId(), sellerUserId)) {
            throw new BusinessException(403, "无权确认该议价");
        }
        if (!NEGOTIATION_APPLIED.equalsIgnoreCase(negotiation.getStatus())) {
            throw new BusinessException(400, "当前议价状态不可确认");
        }

        SecondhandProduct product = secondhandProductMapper.selectById(negotiation.getProductId());
        if (product == null || !Objects.equals(product.getStatus(), SECONDHAND_ON_SHELF)) {
            throw new BusinessException(400, "商品不可交易，无法确认议价");
        }
        if (request.getConfirmedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "确认价格必须大于0");
        }
        if (request.getConfirmedPrice().compareTo(product.getSalePrice()) > 0) {
            throw new BusinessException(400, "确认价格不能高于商品当前售价");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveUntil = now.plusHours(24);
        int updated = productNegotiationMapper.update(null, new UpdateWrapper<ProductNegotiation>()
                .set("confirmed_price", request.getConfirmedPrice())
                .set("status", NEGOTIATION_CONFIRMED)
                .set("effective_from", now)
                .set("effective_until", effectiveUntil)
                .eq("id", negotiation.getId())
                .eq("seller_user_id", sellerUserId)
                .eq("status", NEGOTIATION_APPLIED));
        if (updated == 0) {
            throw new BusinessException(409, "议价已被处理，请刷新后查看");
        }
        negotiation.setConfirmedPrice(request.getConfirmedPrice());
        negotiation.setStatus(NEGOTIATION_CONFIRMED);
        negotiation.setEffectiveFrom(now);
        negotiation.setEffectiveUntil(effectiveUntil);

        if (Boolean.TRUE.equals(request.getCreateOrder())) {
            OrderInfo order = createPendingBargainOrder(negotiation, product, request.getConfirmedPrice());
            negotiation.setStatus(NEGOTIATION_USED);
            negotiation.setUsedOrderId(order.getId());
            productNegotiationMapper.updateById(negotiation);
        }

        String bargainTarget = negotiation.getUsedOrderId() == null
                ? "/secondhand/" + negotiation.getProductId()
                : "/secondhand/orders/" + negotiation.getUsedOrderId();
        notificationService.createNotification(negotiation.getBuyerUserId(),
                "卖家已确认议价",
                "商品“" + product.getName() + "”议价已确认，成交价 " + negotiation.getConfirmedPrice(),
                bargainTarget);
        runAfterCommit(() -> publishBargainConfirmation(negotiation, product, sellerUserId));

        return toNegotiationVO(negotiation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductNegotiationVO rejectBargain(Long negotiationId) {
        Long sellerUserId = requireUserId();
        if (negotiationId == null) {
            throw new BusinessException(400, "议价记录ID不能为空");
        }
        ProductNegotiation negotiation = productNegotiationMapper.selectById(negotiationId);
        if (negotiation == null) {
            throw new BusinessException(404, "议价记录不存在");
        }
        if (!Objects.equals(negotiation.getSellerUserId(), sellerUserId)) {
            throw new BusinessException(403, "无权驳回该议价");
        }
        if (!NEGOTIATION_APPLIED.equalsIgnoreCase(negotiation.getStatus())) {
            throw new BusinessException(400, "当前议价状态不可驳回");
        }

        int updated = productNegotiationMapper.update(null, new UpdateWrapper<ProductNegotiation>()
                .set("status", NEGOTIATION_REJECTED)
                .eq("id", negotiation.getId())
                .eq("seller_user_id", sellerUserId)
                .eq("status", NEGOTIATION_APPLIED));
        if (updated == 0) {
            throw new BusinessException(409, "议价已被处理，请刷新后查看");
        }
        negotiation.setStatus(NEGOTIATION_REJECTED);

        SecondhandProduct product = secondhandProductMapper.selectById(negotiation.getProductId());
        notificationService.createNotification(negotiation.getBuyerUserId(),
                "议价申请未通过",
                product == null ? "卖家未接受本次议价" : "卖家未接受商品“" + product.getName() + "”的本次议价",
                "/secondhand/" + negotiation.getProductId());
        runAfterCommit(() -> publishBargainRejection(negotiation, product, sellerUserId));

        return toNegotiationVO(negotiation);
    }

    @Override
    public PageVO<ProductNegotiationVO> pageMyBargains(Long pageNum, Long pageSize, Long productId,
            Long counterpartUserId, String status) {
        Long currentUserId = requireUserId();
        long safePageNum = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long safePageSize = pageSize == null || pageSize < 1 ? 20L : Math.min(pageSize, 50L);
        LambdaQueryWrapper<ProductNegotiation> wrapper = new LambdaQueryWrapper<ProductNegotiation>()
                .and(participant -> participant.eq(ProductNegotiation::getBuyerUserId, currentUserId)
                        .or()
                        .eq(ProductNegotiation::getSellerUserId, currentUserId))
                .eq(productId != null, ProductNegotiation::getProductId, productId)
                .and(counterpartUserId != null,
                        counterpart -> counterpart.eq(ProductNegotiation::getBuyerUserId, counterpartUserId)
                                .or()
                                .eq(ProductNegotiation::getSellerUserId, counterpartUserId))
                .eq(status != null && !status.isBlank(), ProductNegotiation::getStatus,
                        status == null ? null : status.trim().toUpperCase(Locale.ROOT))
                .orderByDesc(ProductNegotiation::getCreateTime)
                .orderByDesc(ProductNegotiation::getId);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ProductNegotiation> page =
                productNegotiationMapper.selectPage(
                        com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(safePageNum, safePageSize),
                        wrapper);
        if (page.getTotal() == 0 && !page.getRecords().isEmpty()) {
            page.setTotal(productNegotiationMapper.selectCount(wrapper));
        }
        PageVO<ProductNegotiationVO> vo = new PageVO<>();
        vo.setTotal(page.getTotal());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setRecords(page.getRecords().stream().map(this::toNegotiationVO).toList());
        return vo;
    }

    @Override
    public ProductNegotiationVO getMyEffectiveNegotiation(Long productId) {
        Long buyerUserId = requireUserId();
        ProductNegotiation negotiation = findEffectiveNegotiation(productId, buyerUserId);
        return negotiation == null ? null : toNegotiationVO(negotiation);
    }

    @Override
    public BigDecimal resolveEffectivePriceForBuyer(Long productId, Long buyerUserId) {
        if (productId == null || buyerUserId == null) {
            return null;
        }
        ProductNegotiation negotiation = findEffectiveNegotiation(productId, buyerUserId);
        return negotiation == null ? null : negotiation.getConfirmedPrice();
    }

    @Override
    public void markNegotiationUsed(Long productId, Long buyerUserId, Long orderId) {
        if (productId == null || buyerUserId == null || orderId == null) {
            return;
        }
        ProductNegotiation negotiation = findEffectiveNegotiation(productId, buyerUserId);
        if (negotiation == null) {
            return;
        }
        negotiation.setStatus(NEGOTIATION_USED);
        negotiation.setUsedOrderId(orderId);
        productNegotiationMapper.updateById(negotiation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductAuctionVO createAuction(AuctionCreateRequest request) {
        Long sellerUserId = requireUserId();
        if (request == null || request.getProductId() == null || request.getStartPrice() == null
                || request.getIncrementAmount() == null || request.getDurationMinutes() == null) {
            throw new BusinessException(400, "拍卖参数不完整");
        }
        if (request.getDurationMinutes() < 10 || request.getDurationMinutes() > 1440) {
            throw new BusinessException(400, "拍卖时长需在10-1440分钟之间");
        }

        SecondhandProduct product = secondhandProductMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException(404, "二手商品不存在");
        }
        if (!Objects.equals(product.getSellerUserId(), sellerUserId)) {
            throw new BusinessException(403, "仅商品卖家可发起拍卖");
        }
        if (!Objects.equals(product.getStatus(), SECONDHAND_ON_SHELF)) {
            throw new BusinessException(400, "商品已下架，无法发起拍卖");
        }

        ProductAuction existing = productAuctionMapper.selectOne(new LambdaQueryWrapper<ProductAuction>()
                .eq(ProductAuction::getProductId, request.getProductId())
                .eq(ProductAuction::getStatus, AUCTION_ONGOING)
                .last("limit 1"));
        if (existing != null) {
            throw new BusinessException(400, "该商品已有进行中的拍卖");
        }

        LocalDateTime now = LocalDateTime.now();
        ProductAuction auction = new ProductAuction();
        auction.setProductId(request.getProductId());
        auction.setSellerUserId(sellerUserId);
        auction.setStartPrice(request.getStartPrice());
        auction.setIncrementAmount(request.getIncrementAmount());
        auction.setCurrentPrice(request.getStartPrice());
        auction.setCurrentBidderUserId(null);
        auction.setStartTime(now);
        auction.setEndTime(now.plusMinutes(request.getDurationMinutes()));
        auction.setStatus(AUCTION_ONGOING);
        auction.setVersion(0);
        productAuctionMapper.insert(auction);

        return getAuctionByProductId(request.getProductId());
    }

    @Override
    public ProductAuctionVO getAuctionByProductId(Long productId) {
        if (productId == null) {
            return null;
        }
        ProductAuction auction = productAuctionMapper.selectOne(new LambdaQueryWrapper<ProductAuction>()
                .eq(ProductAuction::getProductId, productId)
                .orderByDesc(ProductAuction::getId)
                .last("limit 1"));
        return auction == null ? null : toAuctionVO(auction);
    }

    @Override
    public PageVO<ProductAuctionVO> pageMyAuctions(Long pageNum, Long pageSize, String status) {
        Long sellerUserId = requireUserId();
        long safePageNum = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long safePageSize = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 50L);
        LambdaQueryWrapper<ProductAuction> wrapper = new LambdaQueryWrapper<ProductAuction>()
                .eq(ProductAuction::getSellerUserId, sellerUserId)
                .orderByDesc(ProductAuction::getCreateTime);
        if (status != null && !status.isBlank()) {
            wrapper.eq(ProductAuction::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ProductAuction> page = productAuctionMapper.selectPage(
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(safePageNum, safePageSize), wrapper);
        if (page.getTotal() == 0 && !page.getRecords().isEmpty()) {
            page.setTotal(productAuctionMapper.selectCount(wrapper));
        }
        PageVO<ProductAuctionVO> vo = new PageVO<>();
        vo.setTotal(page.getTotal());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setRecords(page.getRecords().stream().map(this::toAuctionVO).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductAuctionVO closeAuctionEarly(Long auctionId) {
        ProductAuction auction = getOwnedOngoingAuction(auctionId);
        int updated = productAuctionMapper.update(null, new UpdateWrapper<ProductAuction>()
                .set("end_time", LocalDateTime.now().minusSeconds(1))
                .eq("id", auction.getId())
                .eq("status", AUCTION_ONGOING));
        if (updated == 0) {
            throw new BusinessException(409, "拍卖状态已变化，请刷新后重试");
        }
        settleOneAuction(auction.getId());
        return getAuctionByProductId(auction.getProductId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductAuctionVO markAuctionFlow(Long auctionId) {
        ProductAuction auction = getOwnedOngoingAuction(auctionId);
        if (auction.getCurrentBidderUserId() != null) {
            throw new BusinessException(400, "已有出价记录，不能直接标记为流拍");
        }
        int updated = productAuctionMapper.update(null, new UpdateWrapper<ProductAuction>()
                .set("status", AUCTION_FLOW)
                .eq("id", auction.getId())
                .eq("status", AUCTION_ONGOING));
        if (updated == 0) {
            throw new BusinessException(409, "拍卖状态已变化，请刷新后重试");
        }
        secondhandProductMapper.update(null, new UpdateWrapper<SecondhandProduct>()
                .set("status", SECONDHAND_OFF_SHELF)
                .eq("id", auction.getProductId())
                .eq("status", SECONDHAND_ON_SHELF));
        realtimePushService.pushToUser(auction.getSellerUserId(), RealtimeEventTypes.MSG_TYPE_AUCTION_SETTLED, Map.of(
                "auctionId", auction.getId(),
                "productId", auction.getProductId(),
                "settleResult", AUCTION_FLOW));
        return getAuctionByProductId(auction.getProductId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductAuctionVO placeBid(Long auctionId, AuctionBidRequest request) {
        Long bidderUserId = requireUserId();
        if (auctionId == null || request == null || request.getBidAmount() == null) {
            throw new BusinessException(400, "出价参数不完整");
        }

        ProductAuction auction = productAuctionMapper.selectById(auctionId);
        if (auction == null) {
            throw new BusinessException(404, "拍卖不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!AUCTION_ONGOING.equalsIgnoreCase(auction.getStatus())) {
            throw new BusinessException(400, "拍卖已结束，无法出价");
        }
        if (auction.getStartTime() == null || auction.getStartTime().isAfter(now)) {
            throw new BusinessException(400, "拍卖尚未开始，无法出价");
        }
        if (auction.getEndTime() == null || !auction.getEndTime().isAfter(now)) {
            throw new BusinessException(400, "拍卖已结束，无法出价");
        }
        if (Objects.equals(auction.getSellerUserId(), bidderUserId)) {
            throw new BusinessException(400, "卖家不能参与自己的拍卖");
        }
        BigDecimal minBid = resolveMinBid(auction);
        if (request.getBidAmount().compareTo(minBid) < 0) {
            throw new BusinessException(400, "当前最低可出价为: " + minBid);
        }

        Long oldBidderUserId = auction.getCurrentBidderUserId();
        BigDecimal oldBidAmount = auction.getCurrentPrice();

        if (Objects.equals(oldBidderUserId, bidderUserId)) {
            BigDecimal delta = request.getBidAmount().subtract(oldBidAmount == null ? BigDecimal.ZERO : oldBidAmount);
            if (delta.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "新的出价必须高于当前出价");
            }
            escrowSettlementService.changePersonalBalance(
                    bidderUserId,
                    delta.negate(),
                    null,
                    "AUCTION_BID_DEDUCT",
                    TransactionTradeTypeEnum.EXPENSE_PURCHASE,
                    "拍卖加价冻结");
        } else {
            escrowSettlementService.changePersonalBalance(
                    bidderUserId,
                    request.getBidAmount().negate(),
                    null,
                    "AUCTION_BID_DEDUCT",
                    TransactionTradeTypeEnum.EXPENSE_PURCHASE,
                    "拍卖出价冻结");
            if (oldBidderUserId != null && oldBidAmount != null && oldBidAmount.compareTo(BigDecimal.ZERO) > 0) {
                escrowSettlementService.changePersonalBalance(
                        oldBidderUserId,
                        oldBidAmount,
                        null,
                        "AUCTION_OUTBID_REFUND",
                        TransactionTradeTypeEnum.REFUND_BACKFLOW,
                        "拍卖被超价自动退回");
            }
        }

        int updated = productAuctionMapper.update(null, new UpdateWrapper<ProductAuction>()
                .set("current_price", request.getBidAmount())
                .set("current_bidder_user_id", bidderUserId)
                .setSql("version = version + 1")
                .eq("id", auction.getId())
                .eq("version", auction.getVersion() == null ? 0 : auction.getVersion())
                .eq("status", AUCTION_ONGOING));
        if (updated == 0) {
            throw new BusinessException(409, "竞价冲突，请重试");
        }

        AuctionLog log = new AuctionLog();
        log.setAuctionId(auction.getId());
        log.setProductId(auction.getProductId());
        log.setBidderUserId(bidderUserId);
        log.setBidAmount(request.getBidAmount());
        log.setStatus("ACCEPTED");
        auctionLogMapper.insert(log);

        realtimePushService.pushToUsers(List.of(bidderUserId, auction.getSellerUserId()),
                RealtimeEventTypes.MSG_TYPE_AUCTION_BID_ACCEPTED,
                Map.of(
                        "auctionId", auction.getId(),
                        "productId", auction.getProductId(),
                        "bidderUserId", bidderUserId,
                        "bidAmount", request.getBidAmount()));
        notificationService.createNotification(
                auction.getSellerUserId(),
                "竞拍收到新出价",
                "您的竞拍商品收到新出价 " + request.getBidAmount(),
                "/secondhand/" + auction.getProductId());
        if (oldBidderUserId != null && !Objects.equals(oldBidderUserId, bidderUserId)) {
            realtimePushService.pushToUser(oldBidderUserId,
                    RealtimeEventTypes.MSG_TYPE_AUCTION_OUTBID,
                    Map.of(
                            "auctionId", auction.getId(),
                            "productId", auction.getProductId(),
                            "oldBidAmount", oldBidAmount,
                            "newBidAmount", request.getBidAmount()));
            notificationService.createNotification(
                    oldBidderUserId,
                    "您的竞拍出价已被超过",
                    "当前最高出价为 " + request.getBidAmount(),
                    "/secondhand/" + auction.getProductId());
        }
        return getAuctionByProductId(auction.getProductId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<ProductAuction> expiredAuctions = productAuctionMapper.selectList(new LambdaQueryWrapper<ProductAuction>()
                .eq(ProductAuction::getStatus, AUCTION_ONGOING)
                .le(ProductAuction::getEndTime, now)
                .orderByAsc(ProductAuction::getEndTime)
                .last("limit 200"));
        for (ProductAuction auction : expiredAuctions) {
            settleOneAuction(auction.getId());
        }
    }

    private void settleOneAuction(Long auctionId) {
        ProductAuction auction = productAuctionMapper.selectById(auctionId);
        if (auction == null || !AUCTION_ONGOING.equalsIgnoreCase(auction.getStatus())) {
            return;
        }
        if (auction.getEndTime() == null || auction.getEndTime().isAfter(LocalDateTime.now())) {
            return;
        }
        int claimed = productAuctionMapper.update(null, new UpdateWrapper<ProductAuction>()
                .set("status", AUCTION_SETTLING)
                .setSql("version = version + 1")
                .eq("id", auctionId)
                .eq("status", AUCTION_ONGOING)
                .le("end_time", LocalDateTime.now()));
        if (claimed == 0) {
            return;
        }

        SecondhandProduct product = secondhandProductMapper.selectById(auction.getProductId());
        if (product == null) {
            int flowed = productAuctionMapper.update(null, new UpdateWrapper<ProductAuction>()
                    .set("status", AUCTION_FLOW)
                    .eq("id", auctionId)
                    .eq("status", AUCTION_SETTLING));
            if (flowed == 0) {
                throw new BusinessException(409, "拍卖结算状态冲突，请稍后重试");
            }
            return;
        }

        if (auction.getCurrentBidderUserId() == null || auction.getCurrentPrice() == null) {
            int flowed = productAuctionMapper.update(null, new UpdateWrapper<ProductAuction>()
                    .set("status", AUCTION_FLOW)
                    .eq("id", auctionId)
                    .eq("status", AUCTION_SETTLING));
            if (flowed == 0) {
                throw new BusinessException(409, "拍卖结算状态冲突，请稍后重试");
            }
            secondhandProductMapper.update(null, new UpdateWrapper<SecondhandProduct>()
                    .set("status", SECONDHAND_OFF_SHELF)
                    .eq("id", product.getId())
                    .eq("status", SECONDHAND_ON_SHELF));
            realtimePushService.pushToUser(auction.getSellerUserId(), RealtimeEventTypes.MSG_TYPE_AUCTION_SETTLED, Map.of(
                    "auctionId", auctionId,
                    "productId", auction.getProductId(),
                    "settleResult", "FLOW"));
            return;
        }

        OrderInfo order = new OrderInfo();
        order.setOrderNo(generateAuctionOrderNo(auction.getCurrentBidderUserId()));
        order.setBuyerUserId(auction.getCurrentBidderUserId());
        order.setTotalAmount(auction.getCurrentPrice());
        order.setVoucherDiscountAmount(BigDecimal.ZERO);
        order.setSellerBearAmount(BigDecimal.ZERO);
        order.setPlatformBearAmount(BigDecimal.ZERO);
        order.setPayableAmount(auction.getCurrentPrice());
        order.setPayStatus(1);
        order.setOrderStatus(OrderStatusEnum.PENDING_SHIP.getCode());
        order.setCanRefund(0);
        order.setLogisticsStatus("PENDING");
        order.setLogisticsCurrentIndex(0);
        order.setPayMethod("拍卖保证金结算");
        order.setRemark("拍卖到期自动成交");
        order.setCreateTime(LocalDateTime.now());
        orderInfoMapper.insert(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductType("SECONDHAND");
        orderItem.setProductId(product.getId());
        orderItem.setProductName(product.getName());
        orderItem.setPrice(auction.getCurrentPrice());
        orderItem.setQuantity(1);
        orderItem.setStatus(1);
        orderItemMapper.insert(orderItem);

        int sold = secondhandProductMapper.update(null, new UpdateWrapper<SecondhandProduct>()
                .set("status", SECONDHAND_SOLD)
                .eq("id", product.getId())
                .eq("status", SECONDHAND_ON_SHELF));
        if (sold == 0) {
            throw new BusinessException(409, "商品状态已变化，拍卖结算失败");
        }

        int finished = productAuctionMapper.update(null, new UpdateWrapper<ProductAuction>()
                .set("status", AUCTION_FINISHED)
                .set("settled_order_id", order.getId())
                .eq("id", auction.getId())
                .eq("status", AUCTION_SETTLING));
        if (finished == 0) {
            throw new BusinessException(409, "拍卖结算状态冲突，请稍后重试");
        }

        outbox.publish(EventTypes.SECONDHAND_TRADE_SETTLED, "secondhand-monolith", "SECONDHAND_TRADE",
                auction.getId(), Map.ofEntries(
                        Map.entry("recipientUserIds", List.of(auction.getSellerUserId(), auction.getCurrentBidderUserId())),
                        Map.entry("tradeType", "AUCTION"),
                        Map.entry("tradeId", auction.getId()),
                        Map.entry("productId", product.getId()),
                        Map.entry("buyerId", auction.getCurrentBidderUserId()),
                        Map.entry("sellerId", auction.getSellerUserId()),
                        Map.entry("price", auction.getCurrentPrice()),
                        Map.entry("businessId", String.valueOf(order.getId())),
                        Map.entry("businessType", "SECONDHAND_TRADE"),
                        Map.entry("displayTitle", "Secondhand trade settled"),
                        Map.entry("displayText", "Trade for " + (product.getName() == null ? "[Unavailable item]" : product.getName()) + " was settled"),
                        Map.entry("targetPath", "/secondhand/orders/" + order.getId()),
                        Map.entry("dedupeKey", "secondhand-settled:" + auction.getId())));

        realtimePushService.pushToUsers(
                List.of(auction.getSellerUserId(), auction.getCurrentBidderUserId()),
                RealtimeEventTypes.MSG_TYPE_AUCTION_SETTLED,
                Map.of(
                        "auctionId", auction.getId(),
                        "productId", auction.getProductId(),
                        "orderId", order.getId(),
                        "winnerUserId", auction.getCurrentBidderUserId(),
                        "amount", auction.getCurrentPrice(),
                        "settleResult", AUCTION_FINISHED));
    }

    private ProductNegotiation findEffectiveNegotiation(Long productId, Long buyerUserId) {
        if (productId == null || buyerUserId == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        return productNegotiationMapper.selectOne(new LambdaQueryWrapper<ProductNegotiation>()
                .eq(ProductNegotiation::getProductId, productId)
                .eq(ProductNegotiation::getBuyerUserId, buyerUserId)
                .eq(ProductNegotiation::getStatus, NEGOTIATION_CONFIRMED)
                .isNull(ProductNegotiation::getUsedOrderId)
                .le(ProductNegotiation::getEffectiveFrom, now)
                .ge(ProductNegotiation::getEffectiveUntil, now)
                .orderByDesc(ProductNegotiation::getEffectiveUntil)
                .last("limit 1"));
    }

    private ProductNegotiationVO toNegotiationVO(ProductNegotiation negotiation) {
        ProductNegotiationVO vo = new ProductNegotiationVO();
        vo.setId(negotiation.getId());
        vo.setProductId(negotiation.getProductId());
        vo.setBuyerUserId(negotiation.getBuyerUserId());
        vo.setSellerUserId(negotiation.getSellerUserId());
        vo.setProposedPrice(negotiation.getProposedPrice());
        vo.setConfirmedPrice(negotiation.getConfirmedPrice());
        vo.setStatus(negotiation.getStatus());
        vo.setEffectiveFrom(negotiation.getEffectiveFrom());
        vo.setEffectiveUntil(negotiation.getEffectiveUntil());
        vo.setOrderId(negotiation.getUsedOrderId());
        return vo;
    }

    private OrderInfo createPendingBargainOrder(ProductNegotiation negotiation, SecondhandProduct product, BigDecimal dealPrice) {
        int updatedProduct = secondhandProductMapper.update(null, new UpdateWrapper<SecondhandProduct>()
                .set("status", SECONDHAND_SOLD)
                .eq("id", product.getId())
                .eq("status", SECONDHAND_ON_SHELF));
        if (updatedProduct == 0) {
            throw new BusinessException(400, "二手商品已售出");
        }

        OrderInfo order = new OrderInfo();
        order.setOrderNo(generateBargainOrderNo(negotiation.getBuyerUserId()));
        order.setBuyerUserId(negotiation.getBuyerUserId());
        order.setTotalAmount(dealPrice);
        order.setVoucherDiscountAmount(BigDecimal.ZERO);
        order.setSellerBearAmount(BigDecimal.ZERO);
        order.setPlatformBearAmount(BigDecimal.ZERO);
        order.setPayableAmount(dealPrice);
        order.setPayStatus(0);
        order.setOrderStatus(OrderStatusEnum.PENDING_PAY.getCode());
        order.setCanRefund(1);
        order.setLogisticsStatus("PENDING");
        order.setLogisticsCurrentIndex(0);
        order.setRemark("议价确认生成二手订单");
        order.setCreateTime(LocalDateTime.now());
        orderInfoMapper.insert(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductType("SECONDHAND");
        orderItem.setProductId(product.getId());
        orderItem.setProductName(product.getName());
        orderItem.setPrice(dealPrice);
        orderItem.setQuantity(1);
        orderItem.setStatus(1);
        orderItemMapper.insert(orderItem);
        return order;
    }

    private void publishBargainApply(ProductNegotiation negotiation, SecondhandProduct product) {
        final Long[] conversationId = {null};
        runBestEffort("create bargain conversation", () -> {
            ChatConversationVO conversation = chatService.createOrGetConversation(
                    negotiation.getBuyerUserId(), negotiation.getSellerUserId(), "SECONDHAND",
                    negotiation.getProductId());
            if (conversation != null && conversation.getId() != null) {
                conversationId[0] = conversation.getId();
                negotiation.setConversationId(conversation.getId());
                productNegotiationMapper.update(null, new UpdateWrapper<ProductNegotiation>()
                        .set("conversation_id", conversation.getId())
                        .eq("id", negotiation.getId()));
            }
        });
        if (conversationId[0] != null) {
            runBestEffort("send bargain application chat card", () -> chatService.sendMessage(
                    negotiation.getBuyerUserId(), conversationId[0],
                    buildBargainApplyCardMessage(negotiation, product)));
        }
        runBestEffort("push bargain application", () -> realtimePushService.pushToUser(
                negotiation.getSellerUserId(), RealtimeEventTypes.MSG_TYPE_BARGAIN_APPLY, Map.of(
                        "negotiationId", negotiation.getId(),
                        "productId", negotiation.getProductId(),
                        "buyerUserId", negotiation.getBuyerUserId(),
                        "proposedPrice", negotiation.getProposedPrice())));
    }

    private void publishBargainConfirmation(ProductNegotiation negotiation, SecondhandProduct product,
            Long sellerUserId) {
        if (negotiation.getConversationId() != null) {
            runBestEffort("send bargain confirmation chat card", () -> chatService.sendMessage(
                    sellerUserId, negotiation.getConversationId(),
                    buildBargainConfirmCardMessage(negotiation, product)));
        }
        runBestEffort("push bargain confirmation", () -> realtimePushService.pushToUser(
                negotiation.getBuyerUserId(), RealtimeEventTypes.MSG_TYPE_BARGAIN_CONFIRM, Map.of(
                        "negotiationId", negotiation.getId(),
                        "productId", negotiation.getProductId(),
                        "sellerUserId", sellerUserId,
                        "confirmedPrice", negotiation.getConfirmedPrice(),
                        "effectiveUntil", negotiation.getEffectiveUntil(),
                        "orderId", negotiation.getUsedOrderId() == null ? 0L : negotiation.getUsedOrderId())));
    }

    private void publishBargainRejection(ProductNegotiation negotiation, SecondhandProduct product,
            Long sellerUserId) {
        if (negotiation.getConversationId() != null && product != null) {
            runBestEffort("send bargain rejection chat card", () -> chatService.sendMessage(
                    sellerUserId, negotiation.getConversationId(),
                    buildBargainRejectCardMessage(negotiation, product)));
        }
        runBestEffort("push bargain rejection", () -> realtimePushService.pushToUser(
                negotiation.getBuyerUserId(), RealtimeEventTypes.MSG_TYPE_BARGAIN_CONFIRM, Map.of(
                        "negotiationId", negotiation.getId(),
                        "productId", negotiation.getProductId(),
                        "sellerUserId", sellerUserId,
                        "status", NEGOTIATION_REJECTED)));
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private void runBestEffort(String actionName, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.warn("UC18 side effect failed after core decision: {}", actionName, exception);
        }
    }

    private ProductAuctionVO toAuctionVO(ProductAuction auction) {
        ProductAuctionVO vo = new ProductAuctionVO();
        vo.setId(auction.getId());
        vo.setProductId(auction.getProductId());
        vo.setSellerUserId(auction.getSellerUserId());
        vo.setStartPrice(auction.getStartPrice());
        vo.setIncrementAmount(auction.getIncrementAmount());
        vo.setCurrentPrice(auction.getCurrentPrice());
        vo.setCurrentBidderUserId(auction.getCurrentBidderUserId());
        vo.setStartTime(auction.getStartTime());
        vo.setEndTime(auction.getEndTime());
        vo.setStatus(auction.getStatus());
        vo.setStatusName(resolveAuctionStatusName(auction.getStatus()));
        vo.setSettledOrderId(auction.getSettledOrderId());

        SecondhandProduct product = secondhandProductMapper.selectById(auction.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
        }

        if (auction.getCurrentBidderUserId() != null) {
            User user = userMapper.selectById(auction.getCurrentBidderUserId());
            if (user != null) {
                vo.setCurrentBidderName(resolveUserName(user));
            }
        }

        List<AuctionLog> logs = auctionLogMapper.selectList(new LambdaQueryWrapper<AuctionLog>()
                .eq(AuctionLog::getAuctionId, auction.getId())
                .orderByDesc(AuctionLog::getCreateTime)
                .last("limit 20"));
        vo.setBidCount(auctionLogMapper.selectCount(new LambdaQueryWrapper<AuctionLog>()
                .eq(AuctionLog::getAuctionId, auction.getId())));
        Map<Long, User> userMap = new HashMap<>();
        for (AuctionLog log : logs) {
            if (log.getBidderUserId() != null && !userMap.containsKey(log.getBidderUserId())) {
                userMap.put(log.getBidderUserId(), userMapper.selectById(log.getBidderUserId()));
            }
        }
        vo.setLogs(logs.stream().map(log -> {
            AuctionLogVO logVO = new AuctionLogVO();
            logVO.setId(log.getId());
            logVO.setBidderUserId(log.getBidderUserId());
            User bidder = userMap.get(log.getBidderUserId());
            if (bidder != null) {
                logVO.setBidderName(resolveUserName(bidder));
            }
            logVO.setBidAmount(log.getBidAmount());
            logVO.setStatus(log.getStatus());
            logVO.setCreateTime(log.getCreateTime());
            return logVO;
        }).toList());
        return vo;
    }

    private BigDecimal resolveMinBid(ProductAuction auction) {
        if (auction.getCurrentBidderUserId() == null) {
            return auction.getStartPrice() == null ? BigDecimal.ZERO : auction.getStartPrice();
        }
        if (auction.getCurrentPrice() == null) {
            return auction.getStartPrice();
        }
        BigDecimal increment = auction.getIncrementAmount() == null || auction.getIncrementAmount().compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE
                : auction.getIncrementAmount();
        return auction.getCurrentPrice().add(increment);
    }

    private String resolveAuctionStatusName(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case AUCTION_ONGOING -> "进行中";
            case AUCTION_SETTLING -> "结算中";
            case AUCTION_FINISHED -> "已成交";
            case AUCTION_FLOW -> "已流拍";
            default -> status;
        };
    }

    private String buildBargainApplyCardMessage(ProductNegotiation negotiation, SecondhandProduct product) {
        return "[BARGAIN_APPLY]{\"negotiationId\":" + negotiation.getId()
                + ",\"productId\":" + negotiation.getProductId()
                + ",\"productName\":\"" + safeJsonValue(product.getName()) + "\""
                + ",\"buyerUserId\":" + negotiation.getBuyerUserId()
                + ",\"sellerUserId\":" + negotiation.getSellerUserId()
                + ",\"proposedPrice\":\"" + negotiation.getProposedPrice() + "\"}";
    }

    private String buildBargainConfirmCardMessage(ProductNegotiation negotiation, SecondhandProduct product) {
        return "[BARGAIN_CONFIRM]{\"negotiationId\":" + negotiation.getId()
                + ",\"productId\":" + negotiation.getProductId()
                + ",\"productName\":\"" + safeJsonValue(product.getName()) + "\""
                + ",\"buyerUserId\":" + negotiation.getBuyerUserId()
                + ",\"sellerUserId\":" + negotiation.getSellerUserId()
                + ",\"confirmedPrice\":\"" + negotiation.getConfirmedPrice() + "\""
                + ",\"effectiveUntil\":\"" + negotiation.getEffectiveUntil() + "\""
                + (negotiation.getUsedOrderId() == null ? "" : ",\"orderId\":" + negotiation.getUsedOrderId())
                + "}";
    }

    private String buildBargainRejectCardMessage(ProductNegotiation negotiation, SecondhandProduct product) {
        return "[BARGAIN_REJECT]{\"negotiationId\":" + negotiation.getId()
                + ",\"productId\":" + negotiation.getProductId()
                + ",\"productName\":\"" + safeJsonValue(product.getName()) + "\""
                + ",\"buyerUserId\":" + negotiation.getBuyerUserId()
                + ",\"sellerUserId\":" + negotiation.getSellerUserId()
                + ",\"proposedPrice\":\"" + negotiation.getProposedPrice() + "\"}";
    }

    private String safeJsonValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String resolveUserName(User user) {
        if (user == null) {
            return "用户";
        }
        String nickname = user.getNickname();
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        return user.getUsername() == null ? "用户" : user.getUsername();
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }

    private ProductAuction getOwnedOngoingAuction(Long auctionId) {
        Long sellerUserId = requireUserId();
        if (auctionId == null) {
            throw new BusinessException(400, "拍卖ID不能为空");
        }
        ProductAuction auction = productAuctionMapper.selectById(auctionId);
        if (auction == null) {
            throw new BusinessException(404, "拍卖不存在");
        }
        if (!Objects.equals(auction.getSellerUserId(), sellerUserId)) {
            throw new BusinessException(403, "无权操作该拍卖");
        }
        if (!AUCTION_ONGOING.equalsIgnoreCase(auction.getStatus())) {
            throw new BusinessException(400, "当前拍卖状态不可操作");
        }
        return auction;
    }

    private String generateAuctionOrderNo(Long buyerUserId) {
        Long safeUserId = buyerUserId == null ? 0L : buyerUserId;
        return "AUC" + LocalDateTime.now().format(ORDER_NO_FORMATTER)
                + String.format(Locale.ROOT, "%04d", safeUserId % 10000);
    }

    private String generateBargainOrderNo(Long buyerUserId) {
        Long safeUserId = buyerUserId == null ? 0L : buyerUserId;
        return "BRG" + LocalDateTime.now().format(ORDER_NO_FORMATTER)
                + String.format(Locale.ROOT, "%04d", safeUserId % 10000);
    }
}
