<template>
  <section class="detail-page">
    <el-skeleton v-if="loading" :rows="8" animated class="page-card" />

    <div v-else-if="item" class="detail-shell">
      <div class="gallery">
        <el-image :src="selectedImage" fit="cover" class="main-image" />
        <div class="thumbs" v-if="imageList.length > 1">
          <button
            v-for="image in imageList"
            :key="image"
            class="thumb"
            :class="{ active: selectedImage === image }"
            type="button"
            @click="selectedImage = image"
          >
            <img :src="image" :alt="item.name" />
          </button>
        </div>
      </div>

      <div class="buy-panel">
        <div class="crumb">二手市场 / 个人闲置</div>
        <h1>{{ item.name }}</h1>
        <p class="desc">{{ item.description || "暂无商品描述" }}</p>

        <div class="price-box">
          <span>闲置价</span>
          <strong>¥{{ Number(item.salePrice || 0).toFixed(2) }}</strong>
          <em>原价 ¥{{ Number(item.originPrice || item.salePrice || 0).toFixed(2) }}</em>
          <small v-if="effectiveBargain" class="deal-price">
            已同意议价 ¥{{ Number(effectiveBargain.confirmedPrice || effectiveBargain.proposedPrice || 0).toFixed(2) }}
          </small>
        </div>

        <div class="promo-line">
          <span>{{ item.conditionLevel || item.condition || "成色良好" }}</span>
          <span>可议价</span>
          <span>单件闲置</span>
          <span v-if="auctionInfo">{{ auctionInfo.statusName || auctionInfo.status }}</span>
        </div>

        <dl class="info-grid">
          <div>
            <dt>成色</dt>
            <dd>{{ item.conditionLevel || item.condition || "未知" }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd>{{ item.statusName || "在售" }}</dd>
          </div>
          <div>
            <dt>卖家</dt>
            <dd>{{ item.sellerName || item.sellerUserId || "个人卖家" }}</dd>
          </div>
        </dl>

        <div class="notice-box">
          {{ auctionInfo?.status === "ONGOING" ? "该商品正在拍卖中，一口价购买暂不可用，可以参与竞拍。" : "二手商品默认单件交易，请先阅读描述并确认卖家信息。付款后可在二手订单页查看进度。" }}
        </div>

        <div v-if="auctionInfo" class="auction-panel">
          <div class="auction-panel__head">
            <div>
              <span class="auction-kicker">Auction Live</span>
              <h3>拍卖情况</h3>
              <p>{{ auctionRoleHint }}</p>
            </div>
            <el-tag :type="auctionInfo.status === 'ONGOING' ? 'success' : 'info'" effect="plain">
              {{ auctionInfo.statusName || auctionInfo.status }}
            </el-tag>
          </div>

          <div class="auction-main-price">
            <span>{{ auctionInfo.status === "ONGOING" ? "当前最高价" : "最终价格" }}</span>
            <strong>¥{{ Number(auctionInfo.currentPrice || auctionInfo.startPrice || 0).toFixed(2) }}</strong>
            <small>{{ auctionStageCopy }}</small>
          </div>

          <div class="auction-stats">
            <div>
              <span>起拍价</span>
              <strong>¥{{ Number(auctionInfo.startPrice || 0).toFixed(2) }}</strong>
            </div>
            <div>
              <span>最低出价</span>
              <strong>¥{{ Number(minBidPrice || 0).toFixed(2) }}</strong>
            </div>
            <div>
              <span>加价幅度</span>
              <strong>¥{{ Number(auctionInfo.incrementAmount || 1).toFixed(2) }}</strong>
            </div>
            <div>
              <span>出价次数</span>
              <strong>{{ Number(auctionInfo.bidCount || 0) }}</strong>
            </div>
            <div>
              <span>当前领先</span>
              <strong>{{ auctionInfo.currentBidderName || "暂无出价" }}</strong>
            </div>
            <div>
              <span>截止时间</span>
              <strong>{{ formatTime(auctionInfo.endTime) }}</strong>
            </div>
          </div>
        </div>

        <div class="actions">
          <el-button type="warning" size="large" :disabled="!canBuyOnePrice" @click="handleBuyNow">
            立即购买
          </el-button>
          <el-button size="large" :disabled="!canBuy" @click="handleAddToSecondhandCart">
            加入二手购物车
          </el-button>
          <el-button type="primary" size="large" @click="handleContactSeller">
            和卖家聊一聊
          </el-button>
          <el-button size="large" :disabled="!item.sellerUserId" @click="handleEnterSeller">
            <el-icon><User /></el-icon>
            进入卖家
          </el-button>
          <el-button size="large" :type="canBargain ? 'primary' : 'default'" @click="openBargainDialog">
            我要议价
          </el-button>
          <el-button v-if="canCreateAuction" size="large" type="danger" plain @click="openCreateAuctionDialog">
            发起拍卖
          </el-button>
          <el-button v-if="canBidAuction" size="large" type="danger" plain @click="openBidDialog">
            参与竞拍
          </el-button>
          <el-button size="large" @click="router.push('/secondhand/publish')">我也要发布</el-button>
        </div>

        <div v-if="bargainBlockedReason" class="action-hint">
          {{ bargainBlockedReason }}
        </div>

        <div v-if="canChatWithSeller" class="risk-actions">
          <el-button type="warning" plain size="small" @click="openReportDialog">举报卖家</el-button>
          <el-button v-if="!isSellerBlocked" type="danger" plain size="small" @click="handleBlock">拉黑卖家</el-button>
          <el-button v-else type="info" plain size="small" @click="handleUnblock">取消拉黑</el-button>
        </div>
      </div>
    </div>

    <p v-else class="empty-tip page-card">二手商品不存在</p>

    <el-dialog v-model="reportDialogVisible" title="举报卖家" width="480px">
      <el-form :model="reportForm" label-width="90px">
        <el-form-item label="举报类型" required>
          <el-select v-model="reportForm.reasonType" style="width:100%">
            <el-option label="诈骗/虚假交易" value="FRAUD" />
            <el-option label="商品与描述不符" value="FAKE_ITEM" />
            <el-option label="态度恶劣/骚扰" value="BAD_ATTITUDE" />
            <el-option label="刷单/广告骚扰" value="SPAM" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="reportForm.reasonDesc" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportSubmitting" @click="handleSubmitReport">提交举报</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bargainDialogVisible" title="发送议价" width="420px" align-center>
      <el-form label-width="90px">
        <el-form-item label="卖家标价">
          <span>¥{{ Number(item?.salePrice || 0).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="我的出价" required>
          <el-input-number v-model="bargainPrice" :min="1" :precision="2" :step="5" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bargainDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bargainSubmitting" @click="handleApplyBargain">发送给卖家</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bidDialogVisible" title="参与竞拍" width="420px" align-center>
      <el-form label-width="90px">
        <el-form-item label="当前价">
          <span>¥{{ Number(auctionCurrentPrice || 0).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="最低出价">
          <span>¥{{ Number(minBidPrice || 0).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="我的出价" required>
          <el-input-number v-model="bidAmount" :min="1" :precision="2" :step="Number(auctionInfo?.incrementAmount || 5)" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bidDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bidSubmitting" @click="handlePlaceBid">确认出价</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="auctionCreateDialogVisible" title="发起拍卖" width="460px" align-center>
      <el-form label-width="110px">
        <el-form-item label="当前售价">
          <span>¥{{ Number(item?.salePrice || 0).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="起拍价" required>
          <el-input-number v-model="auctionCreateForm.startPrice" :min="1" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="加价幅度" required>
          <el-input-number v-model="auctionCreateForm.incrementAmount" :min="1" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="拍卖时长" required>
          <el-input-number v-model="auctionCreateForm.durationMinutes" :min="10" :max="1440" :step="10" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auctionCreateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auctionCreateSubmitting" @click="handleCreateAuction">确认发起</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addressDialogVisible" title="确认收货地址" width="560px" align-center>
      <el-radio-group v-model="selectedAddressId" class="address-list">
        <el-radio v-for="addr in addresses" :key="addr.id" :value="addr.id" class="address-item">
          {{ addr.receiverName }} {{ addr.receiverPhone }} / {{ addr.province }} {{ addr.city }} {{ addr.detailAddress }}
        </el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="buySubmitting" @click="confirmBuyWithAddress">确认下单</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { User } from "@element-plus/icons-vue";
import { useRoute, useRouter } from "vue-router";
import {
  applyBargainApi,
  buySecondhandApi,
  createAuctionApi,
  getAuctionByProductIdApi,
  getMyEffectiveBargainApi,
  getSecondhandDetailApi,
  placeAuctionBidApi,
} from "@/api/secondhand";
import { listAddressesApi, recordBrowseHistoryApi } from "@/api/user";
import { submitReportApi, blockUserApi, unblockUserApi, isBlockingApi, isBlockedByApi } from "@/api/credit";
import { getUser } from "@/utils/storage";
import { addSecondhandToCart } from "@/utils/secondhandCart";
import { toAssetUrl } from "@/utils/url";
import { useUserStore } from "@/stores/user";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const loading = ref(false);
const item = ref(null);
const selectedImage = ref("");
const reportDialogVisible = ref(false);
const reportSubmitting = ref(false);
const reportForm = ref({ reasonType: "", reasonDesc: "" });
const isSellerBlocked = ref(false);
const effectiveBargain = ref(null);
const auctionInfo = ref(null);
const bargainDialogVisible = ref(false);
const bargainSubmitting = ref(false);
const bargainPrice = ref(1);
const bidDialogVisible = ref(false);
const bidSubmitting = ref(false);
const bidAmount = ref(1);
const auctionCreateDialogVisible = ref(false);
const auctionCreateSubmitting = ref(false);
const auctionCreateForm = ref({
  startPrice: 1,
  incrementAmount: 5,
  durationMinutes: 60,
});
const addressDialogVisible = ref(false);
const buySubmitting = ref(false);
const addresses = ref([]);
const selectedAddressId = ref(null);
const auctionHashHandledFor = ref("");

const imageList = computed(() => {
  if (!item.value) {
    return [];
  }
  const images = Array.isArray(item.value.images) ? item.value.images : [];
  const list = [item.value.cover, ...images]
    .filter(Boolean)
    .map((source) => toAssetUrl(source))
    .filter(Boolean);
  return [...new Set(list)];
});

const currentUser = computed(() => userStore.userInfo || getUser());
const canBuy = computed(() => !!item.value && Number(item.value.status ?? 1) === 1);
const canBuyOnePrice = computed(() => canBuy.value && auctionInfo.value?.status !== "ONGOING");
const canChatWithSeller = computed(() => {
  const sellerId = getSellerUserId();
  const userId = currentUser.value?.id;
  if (!sellerId || !userId) return false;
  return Number(sellerId) !== Number(userId);
});
const isOwner = computed(() => {
  const sellerId = getSellerUserId();
  const userId = currentUser.value?.id;
  return sellerId && userId && Number(sellerId) === Number(userId);
});
const bargainBlockedReason = computed(() => {
  if (!item.value) return "商品还在加载，请稍后再试";
  if (!currentUser.value?.id) return "请先登录后再议价";
  if (isOwner.value) return "！！！不能购买自己发布的闲置！！！";
  if (!canBuy.value) return "商品已下架或已售出，不能议价";
  if (auctionInfo.value?.status === "ONGOING") return "该商品正在拍卖中，请直接参与竞拍";
  return "";
});
const canBargain = computed(() => !bargainBlockedReason.value);
const canCreateAuction = computed(() => canBuy.value && isOwner.value && auctionInfo.value?.status !== "ONGOING");
const canBidAuction = computed(() => canChatWithSeller.value && auctionInfo.value?.status === "ONGOING");
const auctionCurrentPrice = computed(() => Number(auctionInfo.value?.currentPrice || auctionInfo.value?.startPrice || 0));
const minBidPrice = computed(() => {
  if (!auctionInfo.value) return 0;
  return auctionInfo.value.currentBidderUserId
    ? auctionCurrentPrice.value + Number(auctionInfo.value.incrementAmount || 1)
    : auctionCurrentPrice.value;
});
const isLeadingAuction = computed(() => {
  const bidderId = auctionInfo.value?.currentBidderUserId;
  const userId = currentUser.value?.id;
  return bidderId && userId && Number(bidderId) === Number(userId);
});
const auctionStageCopy = computed(() => {
  if (!auctionInfo.value) return "";
  if (auctionInfo.value.status === "ONGOING") {
    if (isOwner.value) return "你是卖家，可以在卖家工作台查看全部出价情况。";
    if (isLeadingAuction.value) return "你目前是最高出价，拍卖结束后会生成待付款订单。";
    return "出价成功后会实时刷新当前最高价。";
  }
  if (auctionInfo.value.status === "FLOW") return "本次拍卖已流拍，没有买家成交。";
  if (isLeadingAuction.value) return "你已竞拍成功，请到二手订单里完成付款。";
  return "本次拍卖已结束。";
});
const auctionRoleHint = computed(() => {
  if (!auctionInfo.value) return "";
  if (isOwner.value) return "卖家可在工作台查看出价次数、最高出价和结束拍卖。";
  if (auctionInfo.value.status !== "ONGOING") return "拍卖结束后，成交买家会收到一笔待付款二手订单。";
  return "买家在这里看当前最高价，并用“参与竞拍”提交新的出价。";
});

watch(() => route.params.id, () => {
  fetchDetail();
}, { immediate: true });

watch(() => route.hash, () => {
  maybeOpenAuctionFromHash();
});

async function fetchDetail() {
  loading.value = true;
  try {
    isSellerBlocked.value = false;
    const result = await getSecondhandDetailApi(route.params.id);
    item.value = result.data;
    selectedImage.value = imageList.value[0] || "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=900&q=80";
    await loadTradeInfo();
    maybeOpenAuctionFromHash();

    const sellerUserId = item.value?.sellerUserId;
    const currentUserId = currentUser.value?.id;
    if (sellerUserId && currentUserId && Number(sellerUserId) !== Number(currentUserId)) {
      const [iBlocked, blockedMe] = await Promise.all([
        isBlockingApi(sellerUserId),
        isBlockedByApi(sellerUserId),
      ]);
      if (iBlocked.data || blockedMe.data) {
        ElMessage.warning("该商品不可访问");
        router.replace("/secondhand");
        return;
      }
      isSellerBlocked.value = iBlocked.data === true;
    }
    recordCurrentItem();
  } finally {
    loading.value = false;
  }
}

async function loadTradeInfo() {
  effectiveBargain.value = null;
  auctionInfo.value = null;
  if (!item.value?.id) {
    return;
  }
  const [auctionRes, bargainRes] = await Promise.allSettled([
    getAuctionByProductIdApi(item.value.id),
    getMyEffectiveBargainApi(item.value.id),
  ]);
  if (auctionRes.status === "fulfilled") {
    auctionInfo.value = auctionRes.value?.data || null;
  }
  if (bargainRes.status === "fulfilled") {
    effectiveBargain.value = bargainRes.value?.data || null;
  }
}

async function recordCurrentItem() {
  if (!item.value?.id || !currentUser.value?.id) {
    return;
  }
  try {
    await recordBrowseHistoryApi({
      productId: item.value.id,
      productType: "SECONDHAND",
    });
  } catch {
    // Browse history is helpful but should never block product viewing.
  }
}

async function handleBuyNow() {
  if (!canBuyOnePrice.value) {
    ElMessage.warning("当前商品暂不可购买");
    return;
  }
  buySubmitting.value = true;
  try {
    const result = await listAddressesApi();
    const list = result?.data || [];
    if (!list.length) {
      ElMessage.warning("请先添加收货地址");
      router.push("/addresses");
      return;
    }
    addresses.value = list;
    const preferred = list.find((addr) => Number(addr.isDefault) === 1) || list[0];
    selectedAddressId.value = preferred?.id || null;
    addressDialogVisible.value = true;
  } finally {
    buySubmitting.value = false;
  }
}

async function confirmBuyWithAddress() {
  if (!selectedAddressId.value) {
    ElMessage.warning("请选择收货地址");
    return;
  }
  buySubmitting.value = true;
  try {
    await buySecondhandApi(item.value.id, { addressId: selectedAddressId.value });
    addressDialogVisible.value = false;
    ElMessage.success("下单成功");
    router.push("/secondhand/orders");
  } finally {
    buySubmitting.value = false;
  }
}

function handleAddToSecondhandCart() {
  if (!canBuyOnePrice.value) {
    ElMessage.warning("当前商品暂不可加入购物车");
    return;
  }
  if (!canChatWithSeller.value) {
    ElMessage.warning("这是你自己的闲置，不能加入二手购物车");
    return;
  }
  addSecondhandToCart(item.value);
  ElMessage.success("已加入二手购物车");
}

function handleContactSeller() {
  openSellerChat();
}

function handleEnterSeller() {
  const sellerUserId = getSellerUserId();
  if (!sellerUserId) {
    ElMessage.warning("当前商品暂未关联卖家");
    return;
  }
  router.push({ name: "secondhandSeller", params: { sellerId: sellerUserId } });
}

function openBargainDialog() {
  if (!canBargain.value) {
    ElMessage.closeAll();
    ElMessage.warning(bargainBlockedReason.value || "当前商品暂不能议价");
    return;
  }
  bargainPrice.value = Math.max(1, Number(item.value?.salePrice || 1) - 10);
  bargainDialogVisible.value = true;
}

async function handleApplyBargain() {
  if (!item.value?.id || Number(bargainPrice.value || 0) <= 0) {
    ElMessage.warning("请输入有效议价金额");
    return;
  }
  bargainSubmitting.value = true;
  try {
    await applyBargainApi({
      productId: item.value.id,
      sellerUserId: getSellerUserId(),
      proposedPrice: Number(bargainPrice.value).toFixed(2),
    });
    bargainDialogVisible.value = false;
    ElMessage.success("议价已发送给卖家");
    openSellerChat();
  } finally {
    bargainSubmitting.value = false;
  }
}

function openBidDialog() {
  bidAmount.value = Number(minBidPrice.value || 1);
  bidDialogVisible.value = true;
}

async function handlePlaceBid() {
  if (!auctionInfo.value?.id) {
    ElMessage.warning("当前没有进行中的拍卖");
    return;
  }
  if (Number(bidAmount.value || 0) < Number(minBidPrice.value || 0)) {
    ElMessage.warning(`出价不能低于 ¥${Number(minBidPrice.value || 0).toFixed(2)}`);
    return;
  }
  bidSubmitting.value = true;
  try {
    const result = await placeAuctionBidApi(auctionInfo.value.id, {
      bidAmount: Number(bidAmount.value).toFixed(2),
    });
    auctionInfo.value = result.data;
    bidDialogVisible.value = false;
    ElMessage.success("出价成功");
  } finally {
    bidSubmitting.value = false;
  }
}

function openCreateAuctionDialog() {
  auctionCreateForm.value = {
    startPrice: Number(item.value?.salePrice || 1),
    incrementAmount: 5,
    durationMinutes: 60,
  };
  auctionCreateDialogVisible.value = true;
}

function maybeOpenAuctionFromHash() {
  if (route.hash !== "#auction" || !item.value?.id) {
    return;
  }
  const key = `${item.value.id}:${route.hash}`;
  if (auctionHashHandledFor.value === key) {
    return;
  }
  auctionHashHandledFor.value = key;
  if (canCreateAuction.value) {
    openCreateAuctionDialog();
    return;
  }
  if (auctionInfo.value) {
    ElMessage.info("该商品已有拍卖记录，可在详情页查看");
    return;
  }
  if (!isOwner.value) {
    ElMessage.warning("只有发布该闲置的卖家可以发起拍卖");
  }
}

async function handleCreateAuction() {
  if (!item.value?.id) return;
  if (Number(auctionCreateForm.value.startPrice || 0) <= 0) {
    ElMessage.warning("起拍价必须大于 0");
    return;
  }
  auctionCreateSubmitting.value = true;
  try {
    const result = await createAuctionApi({
      productId: item.value.id,
      startPrice: Number(auctionCreateForm.value.startPrice).toFixed(2),
      incrementAmount: Number(auctionCreateForm.value.incrementAmount || 1).toFixed(2),
      durationMinutes: Number(auctionCreateForm.value.durationMinutes || 60),
    });
    auctionInfo.value = result.data;
    auctionCreateDialogVisible.value = false;
    ElMessage.success("拍卖已发起");
  } finally {
    auctionCreateSubmitting.value = false;
  }
}

function openSellerChat(initialMessage = "") {
  const sellerUserId = getSellerUserId();
  if (!sellerUserId) {
    ElMessage.warning("当前无法联系卖家");
    return;
  }
  if (!canChatWithSeller.value) {
    ElMessage.warning(currentUser.value?.id ? "这是你自己的闲置，不能和自己发起会话" : "请先登录后再联系卖家");
    return;
  }
  router.push({
    path: "/messages",
    query: {
      participantId: sellerUserId,
      sourceType: "SECONDHAND",
      sourceId: item.value.id,
      ...(initialMessage ? { initialMessage } : {}),
    },
  });
}

function getSellerUserId() {
  return item.value?.sellerUserId || item.value?.sellerId || null;
}

function formatTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString("zh-CN", { hour12: false });
}

function openReportDialog() {
  reportForm.value = { reasonType: "", reasonDesc: "" };
  reportDialogVisible.value = true;
}

async function handleSubmitReport() {
  if (!reportForm.value.reasonType) {
    ElMessage.warning("请选择举报类型");
    return;
  }
  reportSubmitting.value = true;
  try {
    await submitReportApi({
      reportedId: item.value.sellerUserId,
      tradeContext: "SH_BUYER",
      reasonType: reportForm.value.reasonType,
      reasonDesc: reportForm.value.reasonDesc,
    });
    ElMessage.success("举报已提交，等待管理员审核");
    reportDialogVisible.value = false;
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || "举报提交失败");
  } finally {
    reportSubmitting.value = false;
  }
}

async function handleBlock() {
  try {
    await ElMessageBox.confirm(
      `确认拉黑卖家「${item.value.sellerName || item.value.sellerUserId}」？拉黑后对方无法与你发起会话。`,
      "拉黑确认",
      { type: "warning", confirmButtonText: "确认拉黑", cancelButtonText: "取消" },
    );
    await blockUserApi(item.value.sellerUserId);
    isSellerBlocked.value = true;
    ElMessage.success("已拉黑该卖家");
  } catch (e) {
    if (e === "cancel" || e?.toString?.().includes("cancel")) return;
    ElMessage.error(e?.response?.data?.message || "操作失败");
  }
}

async function handleUnblock() {
  try {
    await ElMessageBox.confirm(
      `确认取消拉黑卖家「${item.value.sellerName || item.value.sellerUserId}」？`,
      "取消拉黑",
      { type: "warning", confirmButtonText: "确认取消", cancelButtonText: "取消" },
    );
    await unblockUserApi(item.value.sellerUserId);
    isSellerBlocked.value = false;
    ElMessage.success("已取消拉黑");
  } catch (e) {
    if (e === "cancel" || e?.toString?.().includes("cancel")) return;
    ElMessage.error(e?.response?.data?.message || "操作失败");
  }
}
</script>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-shell {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.1fr);
  gap: 22px;
}

.gallery,
.buy-panel {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 18px;
  box-shadow: var(--shadow-soft);
}

.main-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 8px;
  overflow: hidden;
  background: #f1efe6;
}

.thumbs {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.thumb {
  aspect-ratio: 1 / 1;
  border: 2px solid transparent;
  border-radius: 8px;
  padding: 0;
  overflow: hidden;
  background: #f1efe6;
  cursor: pointer;
}

.thumb.active {
  border-color: var(--brand-primary);
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.buy-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.crumb {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.buy-panel h1 {
  margin: 0;
  font-size: clamp(26px, 4vw, 38px);
  line-height: 1.18;
  letter-spacing: 0;
}

.desc {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.price-box {
  border-radius: 8px;
  background: linear-gradient(90deg, #e9fff8 0%, #eaf4ff 100%);
  padding: 16px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: baseline;
}

.price-box span {
  color: var(--brand-accent-strong);
  font-weight: 800;
}

.price-box strong {
  color: var(--brand-accent-strong);
  font-size: 36px;
  line-height: 1;
}

.price-box em {
  color: var(--text-muted);
  font-style: normal;
  text-decoration: line-through;
}

.deal-price {
  grid-column: 1 / -1;
  color: #0b8f72;
  font-weight: 900;
}

.promo-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.promo-line span {
  border: 1px solid rgba(18, 165, 148, 0.28);
  border-radius: 4px;
  background: #e9fbf8;
  color: var(--brand-accent-strong);
  padding: 5px 9px;
  font-size: 12px;
  font-weight: 900;
}

.info-grid {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.info-grid div {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  padding: 12px;
  background: var(--surface-soft);
}

.info-grid dt {
  color: var(--text-muted);
  font-size: 12px;
}

.info-grid dd {
  margin: 6px 0 0;
  font-weight: 800;
}

.notice-box {
  border-radius: 8px;
  background: var(--surface-soft);
  color: var(--text-secondary);
  padding: 13px 14px;
  line-height: 1.7;
}

.auction-panel {
  border: 1px solid rgba(255, 198, 220, 0.64);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(255, 247, 251, 0.96) 0%, rgba(234, 244, 255, 0.96) 56%, rgba(233, 255, 248, 0.92) 100%);
  padding: 14px;
  display: grid;
  gap: 12px;
}

.auction-panel__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.auction-panel__head h3 {
  margin: 4px 0 4px;
  font-size: 20px;
}

.auction-panel__head p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.auction-kicker {
  color: var(--brand-primary);
  font-size: 12px;
  font-weight: 900;
}

.auction-main-price {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.8);
  padding: 14px;
  box-shadow: 0 12px 24px rgba(137, 199, 255, 0.12);
}

.auction-main-price span,
.auction-main-price small,
.auction-stats span,
.auction-stats strong {
  display: block;
}

.auction-main-price span,
.auction-stats span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.auction-main-price strong {
  display: block;
  margin-top: 4px;
  color: var(--brand-accent-strong);
  font-size: 30px;
  line-height: 1.1;
}

.auction-main-price small {
  margin-top: 6px;
  color: var(--text-secondary);
  font-weight: 700;
}

.auction-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.auction-stats div {
  min-height: 64px;
  border: 1px solid rgba(137, 199, 255, 0.24);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.64);
  padding: 10px;
}

.auction-stats strong {
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-main);
}

.address-list {
  width: 100%;
  display: grid;
  gap: 10px;
}

.address-item {
  width: 100%;
  min-height: 42px;
  margin-right: 0;
  align-items: center;
}

.actions,
.risk-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.action-hint {
  width: fit-content;
  border: 1px solid rgba(255, 193, 7, 0.28);
  border-radius: 8px;
  background: #fff8e8;
  color: #b7791f;
  padding: 8px 10px;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.5;
}

.risk-actions {
  border-top: 1px solid var(--line-soft);
  padding-top: 14px;
}

@media (max-width: 900px) {
  .detail-shell {
    grid-template-columns: 1fr;
  }

  .info-grid,
  .price-box,
  .auction-stats {
    grid-template-columns: 1fr;
  }
}
</style>
