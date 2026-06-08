<template>
  <div class="admin-voucher-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">优惠券管理</h2>
        <p>统一查看商家券和平台券；平台券可由管理员创建和编辑。</p>
      </div>
      <el-button type="primary" @click="openDialog()">创建平台券</el-button>
    </div>

    <el-card class="filter-card">
      <div class="section-title">优惠券管理</div>
      <div class="toolbar">
        <el-input
          v-model="query.name"
          placeholder="优惠券名称"
          clearable
          style="max-width: 260px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 150px">
          <el-option label="未开始" :value="2" />
          <el-option label="可领取" :value="1" />
          <el-option label="已关闭" :value="0" />
          <el-option label="已领完" :value="3" />
          <el-option label="已结束" :value="4" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="coupons" stripe>
        <el-table-column prop="name" label="优惠券名称" min-width="160" />
        <el-table-column label="发放方" width="110">
          <template #default="{ row }">
            <el-tag :type="isPlatformCoupon(row) ? 'primary' : 'success'" size="small">
              {{ isPlatformCoupon(row) ? "平台" : "商家" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="适用范围" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="isPlatformCoupon(row) ? 'primary' : ''">
              {{ isPlatformCoupon(row) ? "全平台" : "店铺" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠内容" min-width="170">
          <template #default="{ row }">
            <span v-if="Number(row.type) === 1">
              满 ¥{{ formatMoney(row.minAmount) }} 减 ¥{{ formatMoney(row.discountAmount) }}
            </span>
            <span v-else>
              满 ¥{{ formatMoney(row.minAmount) }} 享 {{ formatRate(row.discountRate) }} 折
            </span>
          </template>
        </el-table-column>
        <el-table-column label="剩余/总量" width="150">
          <template #default="{ row }">
            <strong>{{ remainCount(row) }}</strong>
            <span class="muted"> / {{ Number(row.totalCount || 0) }}</span>
            <el-progress
              :percentage="remainPercent(row)"
              :show-text="false"
              style="margin-top: 6px"
            />
          </template>
        </el-table-column>
        <el-table-column label="已领取/已使用" width="130">
          <template #default="{ row }">
            {{ Number(row.receivedCount || 0) }} / {{ Number(row.usedCount || 0) }}
          </template>
        </el-table-column>
        <el-table-column label="领取时间" min-width="220">
          <template #default="{ row }">
            {{ formatTime(row.grabStartTime) }} ~ {{ formatTime(row.grabEndTime) }}
          </template>
        </el-table-column>
        <el-table-column label="使用有效期" min-width="220">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName || statusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :disabled="!isPlatformCoupon(row) || isClosed(row)"
              @click="openDialog(row)"
            >
              编辑
            </el-button>
            <el-button v-if="!isClosed(row)" size="small" type="warning" @click="handleClose(row)">关闭</el-button>
            <el-button v-if="Number(row.usedCount || 0) === 0" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && coupons.length === 0" description="暂无优惠券" />

      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="total"
          :page-size="query.pageSize"
          :current-page="query.page"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑平台券' : '创建平台券'"
      width="min(560px, calc(100vw - 24px))"
      append-to-body
      align-center
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：平台满减券" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="优惠类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :label="1">满减券</el-radio>
            <el-radio :label="2">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="使用门槛">
          <el-switch v-model="form.noThreshold" active-text="无门槛" inactive-text="有门槛" />
        </el-form-item>

        <el-form-item v-if="!form.noThreshold" label="最低消费" prop="minAmount">
          <el-input-number v-model="form.minAmount" :min="0.01" :precision="2" style="width: 180px" />
          <span class="unit">元</span>
        </el-form-item>

        <el-form-item v-if="form.type === 1" label="优惠金额" prop="discountAmount">
          <el-input-number v-model="form.discountAmount" :min="0.01" :precision="2" style="width: 180px" />
          <span class="unit">元</span>
        </el-form-item>

        <el-form-item v-if="form.type === 2" label="折扣率" prop="discountRate">
          <el-input-number
            v-model="form.discountRate"
            :min="0.01"
            :max="0.99"
            :precision="2"
            :step="0.05"
            style="width: 180px"
          />
          <span class="unit">0.8 = 8 折</span>
        </el-form-item>

        <el-form-item label="发放总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" :step="10" style="width: 180px" />
          <span class="unit">张</span>
        </el-form-item>

        <el-form-item label="领取时间" prop="grabRange">
          <el-date-picker
            v-model="form.grabRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="领取开始时间"
            end-placeholder="领取结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="使用有效期" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="使用开始时间"
            end-placeholder="使用结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingId ? "保存" : "创建" }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  closeAdminCouponApi,
  createAdminCouponApi,
  deleteAdminCouponApi,
  listAdminCouponsApi,
  updateAdminCouponApi
} from "@/api/coupon";

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const editingId = ref(null);
const coupons = ref([]);
const total = ref(0);
const formRef = ref(null);

const query = reactive({
  page: 1,
  pageSize: 10,
  name: "",
  status: null
});

const form = reactive({
  name: "",
  type: 1,
  noThreshold: true,
  minAmount: 0,
  discountAmount: null,
  discountRate: null,
  totalCount: 100,
  grabRange: null,
  timeRange: null
});

const rules = {
  name: [{ required: true, message: "请输入优惠券名称", trigger: "blur" }],
  type: [{ required: true, message: "请选择优惠类型", trigger: "change" }],
  minAmount: [{ required: true, message: "请输入最低消费", trigger: "blur" }],
  discountAmount: [{ required: true, message: "请输入优惠金额", trigger: "blur" }],
  discountRate: [{ required: true, message: "请输入折扣率", trigger: "blur" }],
  totalCount: [{ required: true, message: "请输入发放总量", trigger: "blur" }],
  grabRange: [{ required: true, message: "请选择领取时间", trigger: "change" }],
  timeRange: [{ required: true, message: "请选择使用有效期", trigger: "change" }]
};

onMounted(loadCoupons);

async function loadCoupons() {
  loading.value = true;
  try {
    const result = await listAdminCouponsApi(query);
    coupons.value = result.data?.records || [];
    total.value = result.data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.page = 1;
  loadCoupons();
}

function handleReset() {
  query.page = 1;
  query.pageSize = 10;
  query.name = "";
  query.status = null;
  loadCoupons();
}

function handlePageChange(page) {
  query.page = page;
  loadCoupons();
}

function handleSizeChange(pageSize) {
  query.pageSize = pageSize;
  query.page = 1;
  loadCoupons();
}

function openDialog(row = null) {
  if (row && !isPlatformCoupon(row)) {
    return;
  }
  editingId.value = row?.id || null;
  if (row) {
    form.name = row.name || "";
    form.type = Number(row.type || 1);
    form.noThreshold = Number(row.minAmount || 0) === 0;
    form.minAmount = Number(row.minAmount || 0);
    form.discountAmount = row.discountAmount == null ? null : Number(row.discountAmount);
    form.discountRate = row.discountRate == null ? null : Number(row.discountRate);
    form.totalCount = Number(row.totalCount || 1);
    form.grabRange = [row.grabStartTime || row.startTime, row.grabEndTime || row.endTime];
    form.timeRange = [row.startTime, row.endTime];
  } else {
    form.name = "";
    form.type = 1;
    form.noThreshold = true;
    form.minAmount = 0;
    form.discountAmount = null;
    form.discountRate = null;
    form.totalCount = 100;
    form.grabRange = null;
    form.timeRange = null;
  }
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const payload = {
      name: form.name,
      type: form.type,
      noThreshold: form.noThreshold,
      minAmount: form.noThreshold ? 0 : form.minAmount,
      discountAmount: form.type === 1 ? form.discountAmount : null,
      discountRate: form.type === 2 ? form.discountRate : null,
      totalCount: form.totalCount,
      grabStartTime: form.grabRange[0],
      grabEndTime: form.grabRange[1],
      startTime: form.timeRange[0],
      endTime: form.timeRange[1]
    };

    if (editingId.value) {
      await updateAdminCouponApi(editingId.value, payload);
      ElMessage.success("平台券已更新");
    } else {
      await createAdminCouponApi(payload);
      ElMessage.success("平台券已创建");
    }
    dialogVisible.value = false;
    await loadCoupons();
  } finally {
    submitting.value = false;
  }
}

async function handleClose(row) {
  await ElMessageBox.confirm(`确认关闭优惠券“${row.name}”？关闭后用户不可继续领取。`, "关闭优惠券", {
    type: "warning",
    confirmButtonText: "关闭",
    cancelButtonText: "取消"
  });
  await closeAdminCouponApi(row.id);
  ElMessage.success("优惠券已关闭");
  await loadCoupons();
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除优惠券“${row.name}”？`, "删除优惠券", {
    type: "warning",
    confirmButtonText: "删除",
    cancelButtonText: "取消"
  });
  await deleteAdminCouponApi(row.id);
  ElMessage.success("优惠券已删除");
  await loadCoupons();
}

function isPlatformCoupon(row) {
  return Number(row?.voucherType) === 2 || Number(row?.issuerType) === 2 || Number(row?.scopeType) === 2;
}

function remainCount(row) {
  return Number(row.remainCount ?? Math.max(0, Number(row.totalCount || 0) - Number(row.receivedCount || 0)));
}

function remainPercent(row) {
  const totalCount = Number(row.totalCount || 0);
  if (!totalCount) return 0;
  return Math.round((remainCount(row) / totalCount) * 100);
}

function isClosed(row) {
  return Number(row.status) === 0 || Number(row.status) === 3 || Number(row.status) === 4;
}

function statusTagType(status) {
  const value = Number(status);
  if (value === 1) return "success";
  if (value === 2) return "info";
  if (value === 0) return "danger";
  if (value === 3) return "warning";
  return "";
}

function statusName(status) {
  return {
    0: "已关闭",
    1: "可领取",
    2: "未开始",
    3: "已领完",
    4: "已结束"
  }[Number(status)] || "-";
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

function formatMoney(value) {
  return Number(value || 0).toFixed(2);
}

function formatRate(value) {
  return (Number(value || 0) * 10).toFixed(1);
}
</script>

<style scoped>
.admin-voucher-page {
  display: grid;
  gap: 14px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
}

.page-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.filter-card {
  border-radius: 8px;
}

.section-title {
  margin-bottom: 12px;
  font-weight: 800;
  color: #102f42;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.muted {
  color: #8a93a5;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.unit {
  margin-left: 8px;
  color: #64748b;
}

@media (max-width: 720px) {
  .page-header {
    flex-direction: column;
  }
}
</style>
