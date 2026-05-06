<template>
  <div class="page-card">
    <h2 class="page-title">二手商品详情</h2>
    <el-skeleton v-if="loading" :rows="6" animated />

    <div v-else-if="item" class="detail-wrap">
      <div class="cover-box">
        <el-image v-if="item.cover" :src="toFullImageUrl(item.cover)" fit="cover" class="cover-image" />
        <div v-else class="cover-placeholder">暂无图片</div>
      </div>

      <div class="info-box">
        <h3>{{ item.name }}</h3>
        <p class="price">￥{{ Number(item.salePrice || item.price || 0).toFixed(2) }}</p>
        <p v-if="effectiveBargain" class="price bargain-price">
          议价成交价：￥{{ Number(effectiveBargain.confirmedPrice || 0).toFixed(2) }}
        </p>
        <p class="origin">原价：￥{{ Number(item.originPrice || item.salePrice || 0).toFixed(2) }}</p>
        <p>成色：{{ item.conditionLevel || item.condition || '-' }}</p>
        <p>状态：{{ item.statusName || '在售' }}</p>
        <p v-if="item.sellerName">卖家：{{ item.sellerName }}</p>
        <p v-if="auctionInfo" class="desc">
          拍卖状态：{{ auctionInfo.status }} / 当前价 ￥{{ Number(auctionInfo.currentPrice || auctionInfo.startPrice || 0).toFixed(2) }}
          / 截止时间 {{ formatTime(auctionInfo.endTime) }}
        </p>

        <el-alert
          v-if="auctionInfo && auctionInfo.status === 'ONGOING'"
          class="auction-alert"
          type="warning"
          show-icon
          :closable="false"
          title="该商品正在拍卖中，一口价购买已关闭，请参与拍卖出价。"
        />

        <p class="desc">{{ item.description || '暂无商品描述' }}</p>
        <p class="desc">二手商品仅支持单件下单购买。</p>

        <el-space wrap>
          <el-button type="primary" :loading="buySubmitting" :disabled="!canBuyOnePrice" @click="handleBuyNow">立即购买</el-button>
          <el-button v-if="canChatWithSeller" type="success" plain @click="handleContactSeller">联系卖家</el-button>
          <el-button
            v-if="canChatWithSeller && Number(item?.isNegotiable) === 1"
            type="warning"
            plain
            :disabled="!canBargain"
            @click="openBargainDialog"
          >
            我要议价
          </el-button>
          <el-button v-if="canCreateAuction" type="danger" plain @click="openCreateAuctionDialog">发起拍卖</el-button>
          <el-button v-if="canChatWithSeller && auctionInfo && auctionInfo.status === 'ONGOING'" type="danger" plain @click="openBidDialog">
            参与拍卖
          </el-button>
          <el-button type="primary" plain @click="router.push('/secondhand/publish')">我也要发布</el-button>
          <el-button text @click="router.push('/secondhand')">返回</el-button>
        </el-space>

        <el-divider v-if="canChatWithSeller" />
        <el-space v-if="canChatWithSeller" wrap>
          <el-button type="warning" plain size="small" @click="openReportDialog">举报卖家</el-button>
          <el-button v-if="!isSellerBlocked" type="danger" plain size="small" @click="handleBlock">拉黑卖家</el-button>
          <el-button v-else type="info" plain size="small" @click="handleUnblock">取消拉黑</el-button>
        </el-space>
      </div>
    </div>

    <p v-else class="empty-tip">二手商品不存在</p>

    <el-dialog v-model="reportDialogVisible" title="举报卖家" width="480px">
      <el-form :model="reportForm" label-width="110px">
        <el-form-item label="举报原因" required>
          <el-select v-model="reportForm.reasonType" style="width: 100%">
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
        <el-button type="primary" :loading="reportSubmitting" @click="handleSubmitReport">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bargainDialogVisible" title="发送议价" width="420px">
      <el-form label-width="110px">
        <el-form-item label="卖家标价">
          <span>￥{{ Number(item?.salePrice || 0).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="我的出价" required>
          <el-input-number v-model="bargainPrice" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bargainDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bargainSubmitting" @click="handleApplyBargain">发送</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bidDialogVisible" title="参与拍卖" width="420px">
      <el-form label-width="110px">
        <el-form-item label="当前价">
          <span>￥{{ Number(auctionCurrentPrice || 0).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="最低出价">
          <span>￥{{ Number(minBidPrice || 0).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="我的出价" required>
          <el-input-number v-model="bidAmount" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bidDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bidSubmitting" @click="handlePlaceBid">确认出价</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="auctionCreateDialogVisible" title="发起拍卖" width="460px">
      <el-form label-width="130px">
        <el-form-item label="当前一口价">
          <span>￥{{ Number(item?.salePrice || 0).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="起拍价" required>
          <el-input-number v-model="auctionCreateForm.startPrice" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="加价幅度" required>
          <el-input-number v-model="auctionCreateForm.incrementAmount" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="拍卖时长(分钟)" required>
          <el-input-number v-model="auctionCreateForm.durationMinutes" :min="10" :max="1440" :step="10" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auctionCreateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auctionCreateSubmitting" @click="handleCreateAuction">确认发起</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addressDialogVisible" title="选择收货地址" width="560px">
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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import {
  applyBargainApi,
  buySecondhandApi,
  createAuctionApi,
  getAuctionByProductIdApi,
  getMyEffectiveBargainApi,
  getSecondhandDetailApi,
  placeAuctionBidApi,
} from '@/api/secondhand';
import { submitReportApi, blockUserApi, unblockUserApi, isBlockingApi, isBlockedByApi } from '@/api/credit';
import { listAddressesApi } from '@/api/user';
import { getUser } from '@/utils/storage';
import { toApiAssetUrl } from '@/utils/url';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const item = ref(null);
const reportDialogVisible = ref(false);
const reportSubmitting = ref(false);
const reportForm = ref({ reasonType: '', reasonDesc: '' });
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
  incrementAmount: 1,
  durationMinutes: 60,
});

const addressDialogVisible = ref(false);
const buySubmitting = ref(false);
const addresses = ref([]);
const selectedAddressId = ref(null);

const canBuy = computed(() => !!item.value && Number(item.value.status || 1) === 1);
const canBuyOnePrice = computed(() => canBuy.value && !(auctionInfo.value && auctionInfo.value.status === 'ONGOING'));
const canBargain = computed(() => canBuyOnePrice.value && canChatWithSeller.value && Number(item.value?.isNegotiable) === 1);
const isOwner = computed(() => {
  if (!item.value?.sellerUserId) return false;
  return Number(item.value.sellerUserId) === Number(getUser()?.id);
});
const canChatWithSeller = computed(() => {
  if (!item.value?.sellerUserId) return false;
  return Number(item.value.sellerUserId) !== Number(getUser()?.id);
});
const canCreateAuction = computed(() => {
  if (!item.value) return false;
  return isOwner.value && Number(item.value.status || 0) === 1 && !auctionInfo.value;
});
const auctionCurrentPrice = computed(() => Number(auctionInfo.value?.currentPrice || auctionInfo.value?.startPrice || 0));
const minBidPrice = computed(() => {
  const current = auctionCurrentPrice.value;
  if (!auctionInfo.value) return 0;
  return Number(auctionInfo.value?.currentPrice) > 0 ? current + 1 : current;
});

onMounted(async () => {
  await fetchDetail();
  if (route.hash === '#auction' && canCreateAuction.value) {
    openCreateAuctionDialog();
  }
  if (route.query.action === 'buy' && canBuyOnePrice.value && canChatWithSeller.value) {
    await handleBuyNow();
  }
});

async function fetchDetail() {
  loading.value = true;
  try {
    const result = await getSecondhandDetailApi(route.params.id);
    item.value = result.data;
    await loadTradeInfo();

    const sellerUserId = item.value?.sellerUserId;
    const currentUserId = getUser()?.id;
    if (sellerUserId && currentUserId && Number(sellerUserId) !== Number(currentUserId)) {
      const [iBlocked, blockedMe] = await Promise.all([
        isBlockingApi(sellerUserId),
        isBlockedByApi(sellerUserId),
      ]);
      if (iBlocked.data || blockedMe.data) {
        ElMessage.warning('该商品不可访问');
        router.replace('/secondhand');
        return;
      }
      isSellerBlocked.value = iBlocked.data === true;
    }
  } finally {
    loading.value = false;
  }
}

async function loadTradeInfo() {
  if (!item.value?.id) return;
  const tasks = [getAuctionByProductIdApi(item.value.id)];
  if (getUser()?.id) tasks.push(getMyEffectiveBargainApi(item.value.id));
  const [auctionRes, bargainRes] = await Promise.all(tasks);
  auctionInfo.value = auctionRes?.data || null;
  effectiveBargain.value = bargainRes?.data || null;
}

async function handleBuyNow() {
  if (!canBuy.value) {
    ElMessage.warning('当前商品暂不可购买');
    return;
  }
  buySubmitting.value = true;
  try {
    const addressRes = await listAddressesApi();
    const list = addressRes?.data || [];
    if (!list.length) {
      ElMessage.warning('请先添加收货地址');
      router.push('/addresses');
      return;
    }
    addresses.value = list;
    const defaultAddr = list.find((it) => Number(it.isDefault) === 1) || list[0];
    selectedAddressId.value = defaultAddr?.id || null;
    addressDialogVisible.value = true;
  } finally {
    buySubmitting.value = false;
  }
}

async function confirmBuyWithAddress() {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址');
    return;
  }
  buySubmitting.value = true;
  try {
    await buySecondhandApi(item.value.id, { addressId: selectedAddressId.value });
    addressDialogVisible.value = false;
    ElMessage.success('下单成功，请前往订单完成支付');
    router.push('/order');
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '下单失败'));
  } finally {
    buySubmitting.value = false;
  }
}

function openBargainDialog() {
  if (!canBargain.value) {
    ElMessage.warning('该商品正在拍卖中，暂不能议价');
    return;
  }
  bargainPrice.value = Number(item.value?.salePrice || 1);
  bargainDialogVisible.value = true;
}

async function handleApplyBargain() {
  if (!item.value?.id || !item.value?.sellerUserId) return;
  if (Number(bargainPrice.value || 0) <= 0) {
    ElMessage.warning('请输入有效议价金额');
    return;
  }
  bargainSubmitting.value = true;
  try {
    await applyBargainApi({
      productId: item.value.id,
      sellerUserId: item.value.sellerUserId,
      proposedPrice: Number(bargainPrice.value).toFixed(2),
    });
    bargainDialogVisible.value = false;
    ElMessage.success('议价已发送');
    handleContactSeller();
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '议价发送失败'));
  } finally {
    bargainSubmitting.value = false;
  }
}

function openBidDialog() {
  bidAmount.value = Number(minBidPrice.value || auctionCurrentPrice.value || 1);
  bidDialogVisible.value = true;
}

function openCreateAuctionDialog() {
  auctionCreateForm.value = {
    startPrice: Number(item.value?.salePrice || 1),
    incrementAmount: 1,
    durationMinutes: 60,
  };
  auctionCreateDialogVisible.value = true;
}

async function handleCreateAuction() {
  if (!item.value?.id) return;
  if (Number(auctionCreateForm.value.startPrice || 0) <= 0) {
    ElMessage.warning('起拍价必须大于 0');
    return;
  }
  if (Number(auctionCreateForm.value.incrementAmount || 0) <= 0) {
    ElMessage.warning('加价幅度必须大于 0');
    return;
  }
  if (Number(auctionCreateForm.value.durationMinutes || 0) < 10) {
    ElMessage.warning('拍卖时长不能少于 10 分钟');
    return;
  }
  auctionCreateSubmitting.value = true;
  try {
    const result = await createAuctionApi({
      productId: item.value.id,
      startPrice: Number(auctionCreateForm.value.startPrice).toFixed(2),
      incrementAmount: Number(auctionCreateForm.value.incrementAmount).toFixed(2),
      durationMinutes: Number(auctionCreateForm.value.durationMinutes),
    });
    auctionInfo.value = result.data;
    auctionCreateDialogVisible.value = false;
    ElMessage.success('拍卖已发起');
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '发起拍卖失败'));
  } finally {
    auctionCreateSubmitting.value = false;
  }
}

async function handlePlaceBid() {
  if (!auctionInfo.value?.id) {
    ElMessage.warning('当前没有进行中的拍卖');
    return;
  }
  if (Number(bidAmount.value || 0) < Number(minBidPrice.value || 0)) {
    ElMessage.warning(`出价不能低于 ￥${Number(minBidPrice.value || 0).toFixed(2)}`);
    return;
  }
  bidSubmitting.value = true;
  try {
    const result = await placeAuctionBidApi(auctionInfo.value.id, {
      bidAmount: Number(bidAmount.value).toFixed(2),
    });
    auctionInfo.value = result.data;
    bidDialogVisible.value = false;
    ElMessage.success('出价成功');
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '出价失败'));
  } finally {
    bidSubmitting.value = false;
  }
}

function handleContactSeller() {
  if (!item.value?.sellerUserId) {
    ElMessage.warning('当前无法联系卖家');
    return;
  }
  router.push({
    path: '/messages',
    query: {
      participantId: item.value.sellerUserId,
      sourceType: 'SECONDHAND',
      sourceId: item.value.id,
    },
  });
}

function openReportDialog() {
  reportForm.value = { reasonType: '', reasonDesc: '' };
  reportDialogVisible.value = true;
}

async function handleSubmitReport() {
  if (!reportForm.value.reasonType) {
    ElMessage.warning('请选择举报原因');
    return;
  }
  reportSubmitting.value = true;
  try {
    await submitReportApi({
      reportedId: item.value.sellerUserId,
      tradeContext: 'SH_BUYER',
      reasonType: reportForm.value.reasonType,
      reasonDesc: reportForm.value.reasonDesc,
    });
    ElMessage.success('举报已提交');
    reportDialogVisible.value = false;
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '举报提交失败'));
  } finally {
    reportSubmitting.value = false;
  }
}

async function handleBlock() {
  try {
    await ElMessageBox.confirm(`确认拉黑卖家 ${item.value.sellerName || item.value.sellerUserId}？`, '确认操作', {
      type: 'warning',
      confirmButtonText: '确认拉黑',
      cancelButtonText: '取消',
    });
    await blockUserApi(item.value.sellerUserId);
    isSellerBlocked.value = true;
    ElMessage.success('已拉黑该卖家');
  } catch (e) {
    if (isCancel(e)) return;
    ElMessage.error(getErrorMessage(e, '操作失败'));
  }
}

async function handleUnblock() {
  try {
    await ElMessageBox.confirm(`确认取消拉黑卖家 ${item.value.sellerName || item.value.sellerUserId}？`, '确认操作', {
      type: 'warning',
      confirmButtonText: '确认取消',
      cancelButtonText: '取消',
    });
    await unblockUserApi(item.value.sellerUserId);
    isSellerBlocked.value = false;
    ElMessage.success('已取消拉黑');
  } catch (e) {
    if (isCancel(e)) return;
    ElMessage.error(getErrorMessage(e, '操作失败'));
  }
}

function toFullImageUrl(url) {
  if (!url) return '';
  return toApiAssetUrl(url);
}

function formatTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString('zh-CN', { hour12: false });
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message || fallback;
}

function isCancel(error) {
  return error === 'cancel' || error?.toString?.().includes('cancel');
}
</script>

<style scoped>
.detail-wrap {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
}

.cover-box {
  width: 280px;
  height: 280px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  background: #f9fafb;
}

.price {
  color: #ef4444;
  font-size: 22px;
  font-weight: 700;
  margin: 8px 0;
}

.bargain-price {
  color: #b45309;
  font-size: 18px;
  margin-top: -4px;
}

.origin {
  color: #6b7280;
}

.desc {
  color: #4b5563;
  line-height: 1.8;
}

.auction-alert {
  margin: 10px 0 12px;
}

.address-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.address-item {
  margin-right: 0;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .detail-wrap {
    grid-template-columns: 1fr;
  }

  .cover-box {
    width: 100%;
    max-width: 360px;
    margin: 0 auto;
  }
}
</style>
