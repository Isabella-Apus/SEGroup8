<template>
  <div class="page-card">
    <h2 class="page-title">我的评价</h2>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="关键字(内容)"
        clearable
        style="max-width: 260px"
      />
      <el-select v-model="query.score" placeholder="评分" clearable style="width: 140px">
        <el-option v-for="n in [5,4,3,2,1]" :key="n" :label="`${n}星`" :value="n" />
      </el-select>
      <el-date-picker
        v-model="query.startTime"
        type="datetime"
        placeholder="开始时间"
        value-format="x"
        style="width: 210px"
        clearable
      />
      <el-date-picker
        v-model="query.endTime"
        type="datetime"
        placeholder="结束时间"
        value-format="x"
        style="width: 210px"
        clearable
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />

    <el-empty v-else-if="grouped.length === 0" description="暂无评价" />

    <div v-else class="review-list">
      <el-card v-for="group in grouped" :key="group.key" class="review-card" shadow="hover">
        <div class="review-card__header">
          <div>
            <div class="title">
              {{ group.productName }}
              <el-tag v-if="group.productType === 'SECONDHAND'" size="small" type="warning" style="margin-left: 6px">
                二手
              </el-tag>
            </div>
            <div class="meta">订单号：{{ group.orderNo }}</div>
          </div>
          <div class="score-block">
            <span>综合评分：</span>
            <el-rate :model-value="group.latestScore" disabled />
          </div>
        </div>

        <div class="timeline">
          <div
            v-for="item in group.items"
            :key="item.id"
            class="timeline-item"
          >
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

              <div v-if="item.reviewType === 'ORIGINAL' && item.sellerReply" class="seller-reply">
                <div class="seller-reply__title">卖家回复：</div>
                <div class="seller-reply__content">
                  {{ item.sellerReply }}
                  <span class="seller-reply__time">{{ formatTime(item.sellerReplyTime) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="review-card__footer">
          <el-button
            v-if="!group.hasFollowUp"
            link
            type="primary"
            size="small"
            @click="openFollowUp(group.original)"
          >
            追评
          </el-button>
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

    <el-dialog v-model="followDialogVisible" title="提交追评" width="420px">
      <el-form label-width="70px">
        <el-form-item label="评分">
          <el-rate v-model="followForm.score" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="followForm.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入追加评价内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFollowUp">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getMyReviewListApi, submitFollowUpReviewApi } from "@/api/review";

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  score: null,
  keyword: "",
  startTime: null,
  endTime: null
});

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
        items: [],
      });
    }
    map.get(key).items.push(r);
  }
  return Array.from(map.values()).map((g) => {
    g.items.sort((a, b) => (a.createTime || "").localeCompare(b.createTime || ""));
    g.hasFollowUp = g.items.some((i) => i.reviewType === "FOLLOWUP");
    g.original = g.items.find((i) => i.reviewType === "ORIGINAL") || g.items[0];
    g.latestScore = g.items[g.items.length - 1]?.score || g.original?.score || 0;
    return g;
  });
});

const followDialogVisible = ref(false);
const followTarget = reactive({
  orderId: null,
  productType: "",
  productId: null
});
const followForm = reactive({
  score: 5,
  content: ""
});

onMounted(() => fetchList());

async function fetchList() {
  loading.value = true;
  try {
    const res = await getMyReviewListApi(query);
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
  query.score = null;
  query.keyword = "";
  query.startTime = null;
  query.endTime = null;
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

function openFollowUp(row) {
  followTarget.orderId = row.orderId;
  followTarget.productType = row.productType;
  followTarget.productId = row.productId;
  followForm.score = 5;
  followForm.content = "";
  followDialogVisible.value = true;
}

async function submitFollowUp() {
  if (!followTarget.orderId || !followTarget.productId) {
    ElMessage.error("追评目标无效");
    return;
  }
  const content = followForm.content?.trim();
  if (!content) {
    ElMessage.warning("请填写追评内容");
    return;
  }
  await submitFollowUpReviewApi({
    orderId: followTarget.orderId,
    productType: followTarget.productType,
    productId: followTarget.productId,
    score: followForm.score,
    content
  });
  followDialogVisible.value = false;
  ElMessage.success("追评提交成功");
  await fetchList();
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

.review-card__footer {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.seller-reply {
  margin-top: 8px;
  padding: 10px;
  border-radius: 8px;
  background: #f9fafb;
  border: 1px solid #f3f4f6;
}

.seller-reply__title {
  color: #374151;
  font-weight: 500;
  margin-bottom: 6px;
}

.seller-reply__content {
  color: #374151;
  line-height: 1.6;
}

.seller-reply__time {
  margin-left: 10px;
  color: #6b7280;
  font-size: 12px;
}
</style>

