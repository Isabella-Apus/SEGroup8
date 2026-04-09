<template>
  <div class="page-card">
    <h2 class="page-title">评价管理</h2>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="关键字(内容)" clearable style="max-width: 260px" />
      <el-select v-model="replyFilter" placeholder="回复状态" clearable style="width: 140px">
        <el-option label="待回复" value="PENDING" />
        <el-option label="已回复" value="REPLIED" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />

    <el-empty v-else-if="filteredGrouped.length === 0" description="暂无评价" />

    <div v-else class="review-list">
      <el-card v-for="group in filteredGrouped" :key="group.key" class="review-card" shadow="hover">
        <div class="review-card__header">
          <div>
            <div class="title">
              {{ group.productName }}
              <el-tag v-if="group.productType === 'SECONDHAND'" size="small" type="warning" style="margin-left: 6px">
                二手
              </el-tag>
            </div>
            <div class="meta">订单号：{{ group.orderNo }}　买家ID：{{ group.userId || '-' }}</div>
          </div>
          <div class="score-block">
            <span>评分：</span>
            <el-rate :model-value="group.latestScore" disabled />
          </div>
        </div>

        <div class="timeline">
          <div v-for="item in group.items" :key="item.id" class="timeline-item">
            <div class="timeline-dot" />
            <div class="timeline-content">
              <div class="timeline-header">
                <el-tag size="small" :type="item.reviewType === 'FOLLOWUP' ? 'danger' : 'info'">
                  {{ item.reviewType === 'FOLLOWUP' ? '追评' : '评价' }}
                </el-tag>
                <span class="time">{{ formatTime(item.createTime) }}</span>
              </div>
              <div class="timeline-body">
                <el-rate :model-value="item.score" disabled />
                <span class="text">{{ item.content }}</span>
              </div>

              <div v-if="item.reviewType === 'ORIGINAL'" class="reply-block">
                <div class="reply-title">
                  <span>卖家回复：</span>
                  <el-tag v-if="item.sellerReply" size="small" type="success">已回复</el-tag>
                  <el-tag v-else size="small" type="warning">待回复</el-tag>
                </div>
                <div v-if="item.sellerReply" class="reply-content">
                  {{ item.sellerReply }}
                  <span class="reply-time">{{ formatTime(item.sellerReplyTime) }}</span>
                </div>
                <div v-else class="reply-actions">
                  <el-button size="small" type="primary" @click="openReply(item)">回复</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="pager-wrap">
      <el-pagination
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNum"
        :page-sizes="[10, 20, 50]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="replyDialogVisible" title="回复评价" width="520px">
      <el-form label-width="80px">
        <el-form-item label="回复内容">
          <el-input v-model="replyForm.reply" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="给买家的回复" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="replySubmitting" @click="submitReply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getSellerReviewListApi, replyReviewApi } from "@/api/sellerReview";

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  score: null,
  startTime: null,
  endTime: null
});
const replyFilter = ref(null);
const total = ref(0);
const records = ref([]);
const loading = ref(false);

const grouped = computed(() => {
  const map = new Map();
  for (const r of records.value) {
    const key = `${r.orderId}-${r.productId}`;
    if (!map.has(key)) {
      map.set(key, {
        key,
        orderId: r.orderId,
        orderNo: r.orderNo,
        productId: r.productId,
        productType: r.productType,
        productName: r.productName,
        userId: r.userId,
        items: [],
      });
    }
    map.get(key).items.push(r);
  }
  return Array.from(map.values()).map((g) => {
    g.items.sort((a, b) => (a.createTime || "").localeCompare(b.createTime || ""));
    g.original = g.items.find((i) => i.reviewType === "ORIGINAL") || g.items[0];
    g.latestScore = g.items[g.items.length - 1]?.score || g.original?.score || 0;
    g.hasReply = !!g.original?.sellerReply;
    return g;
  });
});

const filteredGrouped = computed(() => {
  if (!replyFilter.value) return grouped.value;
  if (replyFilter.value === "PENDING") return grouped.value.filter((g) => !g.hasReply);
  if (replyFilter.value === "REPLIED") return grouped.value.filter((g) => g.hasReply);
  return grouped.value;
});

const replyDialogVisible = ref(false);
const replySubmitting = ref(false);
const replyTarget = ref(null);
const replyForm = reactive({ reply: "" });

onMounted(() => fetchList());

async function fetchList() {
  loading.value = true;
  try {
    const res = await getSellerReviewListApi(query);
    records.value = res.data?.records || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  fetchList();
}

function handleReset() {
  query.pageNum = 1;
  query.keyword = "";
  replyFilter.value = null;
  fetchList();
}

function handlePageChange(pageNum) {
  query.pageNum = pageNum;
  fetchList();
}

function handleSizeChange(pageSize) {
  query.pageSize = pageSize;
  query.pageNum = 1;
  fetchList();
}

function openReply(row) {
  replyTarget.value = row;
  replyForm.reply = "";
  replyDialogVisible.value = true;
}

async function submitReply() {
  const content = replyForm.reply?.trim();
  if (!replyTarget.value?.id) {
    ElMessage.error("回复目标无效");
    return;
  }
  if (!content) {
    ElMessage.warning("请输入回复内容");
    return;
  }
  replySubmitting.value = true;
  try {
    await replyReviewApi(replyTarget.value.id, { reply: content });
    replyDialogVisible.value = false;
    ElMessage.success("回复成功");
    await fetchList();
  } finally {
    replySubmitting.value = false;
  }
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.title {
  font-weight: 500;
}

.meta {
  color: #6b7280;
  font-size: 12px;
  margin-top: 4px;
}

.score-block {
  display: flex;
  align-items: center;
  gap: 6px;
}

.timeline {
  margin-top: 6px;
  padding-left: 8px;
  border-left: 1px solid #e5e7eb;
}

.timeline-item {
  display: flex;
  margin-top: 8px;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #3b82f6;
  margin-right: 8px;
  margin-top: 6px;
}

.timeline-content {
  flex: 1;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.timeline-header .time {
  font-size: 12px;
  color: #6b7280;
}

.timeline-body {
  display: flex;
  align-items: center;
  gap: 8px;
}

.timeline-body .text {
  color: #374151;
}

.reply-block {
  margin-top: 10px;
  padding: 10px;
  border-radius: 8px;
  background: #f9fafb;
  border: 1px solid #f3f4f6;
}

.reply-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  color: #374151;
}

.reply-content {
  color: #374151;
  line-height: 1.6;
}

.reply-time {
  margin-left: 10px;
  color: #6b7280;
  font-size: 12px;
}
</style>

