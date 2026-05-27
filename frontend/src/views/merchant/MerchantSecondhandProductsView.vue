<template>
  <section class="secondhand-manage page-card">
    <div class="manage-head">
      <div>
        <span class="eyebrow">{{ pageCopy.eyebrow }}</span>
        <h2>{{ pageCopy.title }}</h2>
        <p>{{ pageCopy.desc }}</p>
      </div>
      <div class="head-actions">
        <el-button @click="openMessageCenter">{{ pageCopy.messageButton }}</el-button>
        <el-button v-if="!isMerchantConsole" @click="router.push('/secondhand')">返回二手商城</el-button>
        <el-button type="primary" @click="router.push('/secondhand/publish')">发布闲置</el-button>
      </div>
    </div>

    <section v-if="pendingBargains.length" class="bargain-panel">
      <div class="bargain-panel-head">
        <div>
          <span class="eyebrow">Bargain Inbox</span>
          <h3>待处理议价</h3>
        </div>
        <el-button text type="primary" @click="fetchPendingBargains">刷新</el-button>
      </div>

      <div class="bargain-list">
        <article v-for="request in pendingBargains" :key="request.id" class="bargain-card">
          <div class="bargain-copy">
            <span>{{ request.productName || "二手商品" }}</span>
            <strong>{{ request.buyerName || "买家" }} 出价 ¥{{ Number(request.proposedPrice || 0).toFixed(2) }}</strong>
            <small>{{ formatTime(request.updateTime || request.createTime) }}</small>
          </div>
          <div class="bargain-actions">
            <el-button size="small" @click="openBuyerChat(request)">打开聊天</el-button>
            <el-button
              size="small"
              type="primary"
              :loading="actionLoadingKey === `confirm-${request.id}`"
              @click="handleConfirmBargain(request)"
            >
              同意生成订单
            </el-button>
            <el-button
              size="small"
              :loading="actionLoadingKey === `reject-${request.id}`"
              @click="handleRejectBargain(request)"
            >
              拒绝
            </el-button>
          </div>
        </article>
      </div>
    </section>

    <section class="auction-board">
      <div class="auction-board-head">
        <div>
          <span class="eyebrow">Auction Monitor</span>
          <h3>{{ pageCopy.auctionTitle }}</h3>
          <p>{{ pageCopy.auctionDesc }}</p>
        </div>
        <el-button text type="primary" :loading="auctionLoading" @click="fetchAuctions">刷新</el-button>
      </div>

      <div v-if="ongoingAuctions.length" class="auction-cards">
        <article v-for="auction in ongoingAuctions" :key="auction.id" class="auction-card">
          <div class="auction-card-main">
            <span>{{ auction.productName || "二手商品" }}</span>
            <strong>¥{{ Number(auction.currentPrice || auction.startPrice || 0).toFixed(2) }}</strong>
            <small>{{ auction.currentBidderName ? `当前领先：${auction.currentBidderName}` : "暂无买家出价" }}</small>
          </div>
          <div class="auction-card-meta">
            <span>{{ Number(auction.bidCount || 0) }} 次出价</span>
            <span>{{ formatTime(auction.endTime) }} 截止</span>
          </div>
          <div class="auction-card-actions">
            <el-button size="small" @click="router.push(`/secondhand/${auction.productId}`)">查看商品</el-button>
            <el-button
              size="small"
              type="primary"
              plain
              :loading="auctionActionKey === `close-${auction.id}`"
              @click="handleCloseAuction(auction)"
            >
              提前结束
            </el-button>
          </div>
        </article>
      </div>
      <el-empty v-else :image-size="70" description="暂无进行中的拍卖" />
    </section>

    <div class="toolbar manage-toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索二手商品名"
        clearable
        @keyup.enter="fetchList(true)"
      />
      <el-select v-model="query.status" placeholder="状态" clearable>
        <el-option label="在售" :value="1" />
        <el-option label="下架/已售" :value="2" />
      </el-select>
      <el-button type="primary" @click="fetchList(true)">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="records" class="manage-table">
      <el-table-column label="商品" min-width="280">
        <template #default="{ row }">
          <div class="product-cell">
            <el-image v-if="row.cover" :src="toAssetUrl(row.cover)" fit="cover" class="cover" />
            <div v-else class="cover placeholder">暂无图片</div>
            <div class="product-meta">
              <strong>{{ row.name }}</strong>
              <span>{{ row.categoryName || row.category || '-' }} · {{ row.conditionLevel || '-' }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="售价" width="130">
        <template #default="{ row }">¥{{ Number(row.salePrice || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
            {{ row.statusName || (Number(row.status) === 1 ? "在售" : "已下架") }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="AI审核" width="150">
        <template #default="{ row }">
          <div v-if="row.riskAudit" class="risk-cell">
            <el-tag :type="riskTagType(row.riskAudit.riskLevel)" effect="plain">
              {{ riskLabel(row.riskAudit.riskLevel) }} · {{ row.riskAudit.riskScore }}
            </el-tag>
            <small>{{ auditStatusLabel(row.riskAudit.auditStatus) }}</small>
          </div>
          <span v-else class="muted">待生成</span>
        </template>
      </el-table-column>
      <el-table-column label="拍卖" min-width="190">
        <template #default="{ row }">
          <div v-if="auctionFor(row)" class="auction-cell">
            <el-tag :type="auctionTagType(auctionFor(row))" effect="plain">
              {{ auctionFor(row).statusName || auctionFor(row).status }}
            </el-tag>
            <span>¥{{ Number(auctionFor(row).currentPrice || auctionFor(row).startPrice || 0).toFixed(2) }}</span>
            <small>{{ Number(auctionFor(row).bidCount || 0) }} 次出价</small>
          </div>
          <span v-else class="muted">未发起</span>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" min-width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="390" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/secondhand/${row.id}`)">查看</el-button>
          <el-button link type="primary" @click="openDescriptionDialog(row)">修改描述</el-button>
          <el-button
            v-if="auctionFor(row)?.status === 'ONGOING'"
            link
            type="warning"
            @click="router.push(`/secondhand/${row.id}`)"
          >
            查看拍卖
          </el-button>
          <el-button
            v-else
            link
            type="warning"
            :disabled="Number(row.status) !== 1"
            @click="openAuctionDialog(row)"
          >
            发起拍卖
          </el-button>
          <el-button
            v-if="auctionFor(row)?.status === 'ONGOING'"
            link
            type="primary"
            :loading="auctionActionKey === `close-${auctionFor(row).id}`"
            @click="handleCloseAuction(auctionFor(row))"
          >
            结束
          </el-button>
          <el-button
            v-if="auctionFor(row)?.status === 'ONGOING'"
            link
            type="info"
            :loading="auctionActionKey === `flow-${auctionFor(row).id}`"
            @click="handleFlowAuction(auctionFor(row))"
          >
            流拍
          </el-button>
          <el-button
            v-if="Number(row.status) === 1"
            link
            type="danger"
            @click="toggleStatus(row, 2)"
          >
            下架
          </el-button>
          <el-button v-else link type="success" @click="toggleStatus(row, 1)">上架</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && records.length === 0" description="暂无二手商品" />

    <div class="pager-wrap">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        @current-change="onPageChange"
      />
    </div>

    <el-dialog
      v-model="auctionDialogVisible"
      title="发起拍卖"
      width="540px"
      top="7vh"
      append-to-body
      class="auction-dialog"
      :close-on-click-modal="false"
    >
      <div class="auction-summary">
        <el-image v-if="auctionTarget?.cover" :src="toAssetUrl(auctionTarget.cover)" fit="cover" class="auction-cover" />
        <div v-else class="auction-cover placeholder">暂无图片</div>
        <div class="auction-meta">
          <strong>{{ auctionTarget?.name || "-" }}</strong>
          <span>{{ auctionTarget?.categoryName || auctionTarget?.category || "二手闲置" }} · {{ auctionTarget?.conditionLevel || "-" }}</span>
        </div>
        <div class="auction-price">
          <small>当前售价</small>
          <b>¥{{ Number(auctionTarget?.salePrice || 0).toFixed(2) }}</b>
        </div>
      </div>

      <div class="auction-form">
        <label class="auction-field">
          <span>起拍价</span>
          <el-input-number
            v-model="auctionForm.startPrice"
            :min="1"
            :precision="2"
            :step="10"
            controls-position="right"
            class="auction-input"
          />
        </label>
        <label class="auction-field">
          <span>加价幅度</span>
          <el-input-number
            v-model="auctionForm.incrementAmount"
            :min="1"
            :precision="2"
            :step="1"
            controls-position="right"
            class="auction-input"
          />
        </label>
        <label class="auction-field">
          <span>拍卖时长</span>
          <el-input-number
            v-model="auctionForm.durationMinutes"
            :min="10"
            :max="1440"
            :step="10"
            controls-position="right"
            class="auction-input"
          />
          <small>分钟</small>
        </label>
      </div>

      <div class="auction-tips">
        买家出价达到起拍价即可参与；拍卖进行中商品不能一口价购买或议价。
      </div>

      <template #footer>
        <div class="auction-footer">
          <el-button @click="auctionDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="auctionSubmitting" @click="submitAuction">确认发起</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="descriptionDialogVisible"
      title="修改商品描述"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="商品">
          <span>{{ descriptionTarget?.name || '-' }}</span>
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input
            v-model="descriptionForm.description"
            type="textarea"
            :rows="6"
            maxlength="2000"
            show-word-limit
            placeholder="请根据实际商品补充描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="descriptionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="descriptionSubmitting" @click="submitDescriptionUpdate">提交修改并重新审核</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import {
  changeSellerSecondhandStatusApi,
  closeAuctionEarlyApi,
  confirmBargainApi,
  createAuctionApi,
  deleteSellerSecondhandApi,
  getSellerSecondhandListApi,
  getMyAuctionListApi,
  listBargainRequestsApi,
  markAuctionFlowApi,
  rejectBargainApi,
  updateSellerSecondhandApi,
} from "@/api/secondhand";
import { useUserStore } from "@/stores/user";
import { toAssetUrl } from "@/utils/url";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const records = ref([]);
const total = ref(0);
const pendingBargains = ref([]);
const bargainLoading = ref(false);
const actionLoadingKey = ref("");
const auctions = ref([]);
const auctionLoading = ref(false);
const auctionActionKey = ref("");
const auctionDialogVisible = ref(false);
const auctionSubmitting = ref(false);
const auctionTarget = ref(null);
const descriptionDialogVisible = ref(false);
const descriptionSubmitting = ref(false);
const descriptionTarget = ref(null);
const descriptionForm = reactive({
  description: "",
});
const auctionForm = reactive({
  startPrice: 1,
  incrementAmount: 5,
  durationMinutes: 60,
});

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  status: undefined,
});

const currentUserId = computed(() => userStore.userInfo?.id);
const isMerchantConsole = computed(() => route.path.startsWith("/merchant"));
const messageCenterPath = computed(() => (route.path.startsWith("/merchant") ? "/merchant/messages" : "/messages"));
const pageCopy = computed(() => {
  if (isMerchantConsole.value) {
    return {
      eyebrow: "Secondhand Desk",
      title: "二手商品管理",
      desc: "管理店铺关联的闲置商品，议价和拍卖状态会集中出现在这里。",
      messageButton: "买家消息",
      auctionTitle: "拍卖看板",
      auctionDesc: "查看每个二手拍卖的最高价、出价次数和结束时间。",
    };
  }
  return {
    eyebrow: "My Idle Goods",
    title: "我的闲置/拍卖",
    desc: "管理自己发布的二手闲置，查看买家的议价、拍卖出价和成交进度。",
    messageButton: "我的消息",
    auctionTitle: "我的拍卖",
    auctionDesc: "查看自己发起的拍卖、当前最高价、领先买家和截止时间。",
  };
});
const auctionByProductId = computed(() => {
  const map = new Map();
  auctions.value.forEach((auction) => {
    const productId = Number(auction.productId);
    const previous = map.get(productId);
    if (!previous || Number(auction.id || 0) > Number(previous.id || 0)) {
      map.set(productId, auction);
    }
  });
  return map;
});
const ongoingAuctions = computed(() =>
  auctions.value
    .filter((auction) => auction.status === "ONGOING")
    .sort((a, b) => new Date(a.endTime || 0).getTime() - new Date(b.endTime || 0).getTime()),
);

onMounted(async () => {
  await Promise.all([fetchList(true), fetchPendingBargains(), fetchAuctions()]);
});

async function fetchList(reset = false) {
  if (loading.value) return;
  loading.value = true;
  try {
    if (reset) query.pageNum = 1;
    const res = await getSellerSecondhandListApi({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined,
      status: query.status,
    });
    records.value = res.data?.records || [];
    total.value = Number(res.data?.total || 0);
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.keyword = "";
  query.status = undefined;
  fetchList(true);
}

function onPageChange(page) {
  query.pageNum = page;
  fetchList(false);
}

async function fetchPendingBargains() {
  if (bargainLoading.value) return;
  bargainLoading.value = true;
  try {
    const res = await listBargainRequestsApi({ pageNum: 1, pageSize: 20, status: "PENDING" });
    const records = res.data?.records || res.data || [];
    pendingBargains.value = records.filter((item) => Number(item.sellerUserId) === Number(currentUserId.value));
  } finally {
    bargainLoading.value = false;
  }
}

async function fetchAuctions() {
  if (auctionLoading.value) return;
  auctionLoading.value = true;
  try {
    const res = await getMyAuctionListApi({ pageNum: 1, pageSize: 100 });
    auctions.value = res.data?.records || res.data || [];
  } finally {
    auctionLoading.value = false;
  }
}

function auctionFor(row) {
  return auctionByProductId.value.get(Number(row?.id));
}

function auctionTagType(auction) {
  if (!auction) return "info";
  if (auction.status === "ONGOING") return "success";
  if (auction.status === "FINISHED") return "primary";
  if (auction.status === "FLOW") return "warning";
  return "info";
}

function riskLabel(level) {
  const map = { LOW: "低风险", MEDIUM: "中风险", HIGH: "高风险" };
  return map[level] ?? "未评估";
}

function riskTagType(level) {
  const map = { LOW: "success", MEDIUM: "warning", HIGH: "danger" };
  return map[level] ?? "info";
}

function auditStatusLabel(status) {
  const map = { PENDING: "待处理", APPROVED: "已通过", REJECTED: "已驳回", CHANGE_REQUESTED: "要求修改" };
  return map[status] ?? "待处理";
}

function openMessageCenter() {
  router.push(messageCenterPath.value);
}

function openBuyerChat(request) {
  if (!request?.buyerUserId || !request?.productId) {
    router.push(messageCenterPath.value);
    return;
  }
  router.push({
    path: messageCenterPath.value,
    query: {
      participantId: request.buyerUserId,
      sourceType: "SECONDHAND",
      sourceId: request.productId,
    },
  });
}

function openDescriptionDialog(row) {
  descriptionTarget.value = row;
  descriptionForm.description = row?.description || "";
  descriptionDialogVisible.value = true;
}

async function submitDescriptionUpdate() {
  const target = descriptionTarget.value;
  if (!target?.id) return;
  descriptionSubmitting.value = true;
  try {
    await updateSellerSecondhandApi(target.id, {
      name: target.name,
      cover: target.cover || "",
      images: Array.isArray(target.images) ? target.images : (target.cover ? [target.cover] : []),
      description: descriptionForm.description,
      originPrice: target.originPrice,
      salePrice: target.salePrice,
      categoryId: target.categoryId,
      subCategoryId: target.subCategoryId,
      conditionLevel: target.conditionLevel,
      isNegotiable: target.isNegotiable ?? 1,
      status: 1,
    });
    ElMessage.success("Description updated and resubmitted for audit");
    descriptionDialogVisible.value = false;
    await fetchList(false);
  } finally {
    descriptionSubmitting.value = false;
  }
}

async function handleConfirmBargain(request) {
  actionLoadingKey.value = `confirm-${request.id}`;
  try {
    await confirmBargainApi({
      negotiationId: request.id,
      confirmedPrice: request.proposedPrice,
      createOrder: true,
    });
    ElMessage.success("已同意议价，并生成二手订单");
    await Promise.all([fetchPendingBargains(), fetchList(false)]);
  } finally {
    actionLoadingKey.value = "";
  }
}

async function handleRejectBargain(request) {
  actionLoadingKey.value = `reject-${request.id}`;
  try {
    await rejectBargainApi(request.id);
    ElMessage.success("已拒绝议价");
    await fetchPendingBargains();
  } finally {
    actionLoadingKey.value = "";
  }
}

async function toggleStatus(row, status) {
  try {
    await ElMessageBox.confirm(
      `确认将「${row.name}」${status === 1 ? "上架" : "下架"}？`,
      "操作确认",
      { type: "warning", confirmButtonText: "确认", cancelButtonText: "取消" },
    );
    await changeSellerSecondhandStatusApi(row.id, status);
    ElMessage.success("操作成功");
    fetchList(false);
  } catch (error) {
    if (String(error?.message || error || "").includes("cancel")) return;
    ElMessage.error(error?.response?.data?.message || "操作失败");
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除二手商品「${row.name}」？`,
      "删除确认",
      { type: "warning", confirmButtonText: "确认删除", cancelButtonText: "取消" },
    );
    await deleteSellerSecondhandApi(row.id);
    ElMessage.success("删除成功");
    fetchList(true);
  } catch (error) {
    if (String(error?.message || error || "").includes("cancel")) return;
    ElMessage.error(error?.response?.data?.message || "删除失败");
  }
}

function openAuctionDialog(row) {
  auctionTarget.value = row;
  auctionForm.startPrice = Number(row.salePrice || 1);
  auctionForm.incrementAmount = 5;
  auctionForm.durationMinutes = 60;
  auctionDialogVisible.value = true;
}

async function submitAuction() {
  if (!auctionTarget.value?.id) return;
  if (Number(auctionForm.startPrice || 0) <= 0 || Number(auctionForm.incrementAmount || 0) <= 0) {
    ElMessage.warning("拍卖金额必须大于 0");
    return;
  }
  auctionSubmitting.value = true;
  try {
    await createAuctionApi({
      productId: auctionTarget.value.id,
      startPrice: Number(auctionForm.startPrice).toFixed(2),
      incrementAmount: Number(auctionForm.incrementAmount).toFixed(2),
      durationMinutes: Number(auctionForm.durationMinutes),
    });
    ElMessage.success("拍卖已发起，买家可在商品详情页参与竞拍");
    auctionDialogVisible.value = false;
    await fetchAuctions();
  } finally {
    auctionSubmitting.value = false;
  }
}

async function handleCloseAuction(auction) {
  if (!auction?.id) return;
  auctionActionKey.value = `close-${auction.id}`;
  try {
    await closeAuctionEarlyApi(auction.id);
    ElMessage.success("已结束拍卖，若已有最高出价会为买家生成待付款订单");
    await Promise.all([fetchAuctions(), fetchList(false)]);
  } finally {
    auctionActionKey.value = "";
  }
}

async function handleFlowAuction(auction) {
  if (!auction?.id) return;
  try {
    await ElMessageBox.confirm(
      "确认将这场拍卖标记为流拍？流拍后不会生成订单。",
      "确认流拍",
      { type: "warning", confirmButtonText: "确认流拍", cancelButtonText: "取消" },
    );
    auctionActionKey.value = `flow-${auction.id}`;
    await markAuctionFlowApi(auction.id);
    ElMessage.success("已标记流拍");
    await fetchAuctions();
  } catch (error) {
    if (String(error?.message || error || "").includes("cancel")) return;
    ElMessage.error(error?.response?.data?.message || "操作失败");
  } finally {
    auctionActionKey.value = "";
  }
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}
</script>

<style scoped>
.secondhand-manage {
  display: grid;
  gap: 14px;
}

.manage-head {
  min-height: 144px;
  border: 1px solid rgba(137, 199, 255, 0.32);
  border-radius: 8px;
  padding: 20px;
  background: linear-gradient(135deg, #e9fff8 0%, #eaf4ff 58%, #fff7fb 100%);
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.eyebrow {
  color: var(--brand-primary);
  font-size: 12px;
  font-weight: 900;
}

.manage-head h2 {
  margin: 8px 0 6px;
  font-size: 28px;
}

.manage-head p {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 700;
}

.bargain-panel {
  border: 1px solid rgba(137, 199, 255, 0.32);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  padding: 14px;
  display: grid;
  gap: 12px;
}

.bargain-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.bargain-panel-head h3 {
  margin: 4px 0 0;
  font-size: 20px;
}

.bargain-list {
  display: grid;
  gap: 10px;
}

.bargain-card {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: linear-gradient(135deg, #ffffff 0%, #f4fbff 100%);
  padding: 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.bargain-copy {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.bargain-copy span,
.bargain-copy strong,
.bargain-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bargain-copy span,
.bargain-copy small {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.bargain-copy strong {
  color: var(--text-main);
  font-size: 16px;
}

.bargain-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.auction-board {
  border: 1px solid rgba(137, 199, 255, 0.32);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 247, 251, 0.88), rgba(234, 244, 255, 0.9) 48%, rgba(233, 255, 248, 0.86)),
    #ffffff;
  padding: 14px;
  display: grid;
  gap: 12px;
}

.auction-board-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.auction-board-head h3 {
  margin: 4px 0 4px;
  font-size: 20px;
}

.auction-board-head p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.auction-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.auction-card {
  border: 1px solid rgba(137, 199, 255, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  padding: 12px;
  display: grid;
  gap: 10px;
  box-shadow: 0 12px 24px rgba(137, 199, 255, 0.12);
}

.auction-card-main {
  display: grid;
  gap: 3px;
}

.auction-card-main span,
.auction-card-main small,
.auction-card-meta span {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.auction-card-main strong {
  color: var(--brand-accent-strong);
  font-size: 24px;
  line-height: 1.1;
}

.auction-card-main span,
.auction-card-main small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auction-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.auction-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.manage-toolbar {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 160px auto auto;
  gap: 10px;
  padding: 12px;
}

.manage-table {
  width: 100%;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cover {
  width: 58px;
  height: 58px;
  border-radius: 8px;
  border: 1px solid var(--line-soft);
  overflow: hidden;
  flex: 0 0 auto;
}

.placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  background: var(--surface-soft);
  font-size: 12px;
}

.product-meta {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.product-meta strong,
.product-meta span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.auction-cell {
  display: grid;
  gap: 3px;
  align-items: start;
}

.risk-cell {
  display: grid;
  gap: 4px;
  justify-items: start;
}

.auction-cell span,
.auction-cell small,
.risk-cell small,
.muted {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
}

.auction-summary {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: linear-gradient(135deg, #e9fff8 0%, #eaf4ff 100%);
  padding: 12px;
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.auction-cover {
  width: 58px;
  height: 58px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(137, 199, 255, 0.32);
  background: #ffffff;
}

.auction-meta {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.auction-meta strong,
.auction-meta span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auction-meta span {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.auction-price {
  min-width: 96px;
  border-left: 1px solid rgba(137, 199, 255, 0.32);
  padding-left: 12px;
  display: grid;
  gap: 3px;
  text-align: right;
}

.auction-price small {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.auction-price b {
  color: var(--brand-primary);
  font-size: 18px;
}

.auction-form {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.auction-field {
  min-height: 58px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 10px 12px;
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}

.auction-field > span {
  color: var(--text-main);
  font-weight: 900;
}

.auction-field > small {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.auction-input {
  width: 100%;
}

.auction-tips {
  margin-top: 12px;
  border-radius: 8px;
  background: var(--surface-soft);
  color: var(--text-secondary);
  padding: 10px 12px;
  line-height: 1.6;
  font-size: 13px;
  font-weight: 700;
}

.auction-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:global(.auction-dialog.el-dialog) {
  border-radius: 8px;
  max-height: calc(100vh - 96px);
  overflow: hidden;
}

:global(.auction-dialog .el-dialog__body) {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
  padding-top: 8px;
}

:global(.auction-dialog .el-dialog__footer) {
  border-top: 1px solid var(--line-soft);
  padding-top: 14px;
}

@media (max-width: 760px) {
  .manage-head {
    flex-direction: column;
    align-items: stretch;
  }

  .head-actions,
  .bargain-actions {
    justify-content: flex-start;
  }

  .bargain-card {
    grid-template-columns: 1fr;
  }

  .manage-toolbar {
    grid-template-columns: 1fr;
  }

  .auction-cards {
    grid-template-columns: 1fr;
  }

  .auction-summary,
  .auction-field {
    grid-template-columns: 1fr;
    text-align: left;
  }

  .auction-price {
    border-left: 0;
    padding-left: 0;
    text-align: left;
  }
}
</style>
