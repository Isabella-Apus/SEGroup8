<template>
  <div class="page-card voucher-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">优惠券管理</h2>
        <p class="page-desc">管理员直接发放全平台优惠券，支持编辑、关闭和删除</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">发放优惠券</el-button>
    </div>

    <el-form :inline="true" class="toolbar" @submit.prevent>
      <el-form-item>
        <el-input v-model="query.name" placeholder="优惠券名称" clearable style="width: 220px" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
          <el-option label="生效中" :value="1" />
          <el-option label="已关闭" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.scopeType" placeholder="范围" clearable style="width: 140px">
          <el-option label="店铺券" :value="1" />
          <el-option label="平台券" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadList">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="scopeTypeName" label="范围" width="100" />
      <el-table-column prop="typeName" label="类型" width="120" />
      <el-table-column prop="minAmount" label="门槛" width="110" />
      <el-table-column prop="discountAmount" label="优惠金额" width="110" />
      <el-table-column prop="discountRate" label="折扣率" width="110" />
      <el-table-column prop="totalCount" label="总量" width="90" />
      <el-table-column prop="usedCount" label="已用" width="90" />
      <el-table-column prop="remainCount" label="剩余" width="90" />
      <el-table-column prop="statusName" label="状态" width="100" />
      <el-table-column prop="startTime" label="开始时间" width="180" />
      <el-table-column prop="endTime" label="结束时间" width="180" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="editVoucher(row)">编辑</el-button>
          <el-button v-if="row.status === 1" link type="warning" @click="handleClose(row)">关闭</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="loadList"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="发行方类型" prop="issuerType">
          <el-select v-model="form.issuerType" style="width: 100%" disabled>
            <el-option label="管理员" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="优惠范围" prop="scopeType">
          <el-radio-group v-model="form.scopeType">
            <el-radio :value="2">全平台</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优惠类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减</el-radio>
            <el-radio :value="2">折扣</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="门槛类型" prop="noThreshold">
          <el-radio-group v-model="form.noThreshold">
            <el-radio :value="false">有门槛</el-radio>
            <el-radio :value="true">无门槛</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!form.noThreshold" label="门槛金额" prop="minAmount">
          <el-input-number v-model="form.minAmount" :min="0.01" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="优惠金额" prop="discountAmount">
          <el-input-number v-model="form.discountAmount" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item v-else label="折扣率" prop="discountRate">
          <el-input-number v-model="form.discountRate" :min="0.1" :max="0.99" :precision="2" :step="0.01" style="width: 100%" />
        </el-form-item>
        <el-form-item label="总数量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="抢券开始" prop="grabStartTime">
          <el-date-picker v-model="form.grabStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="抢券结束" prop="grabEndTime">
          <el-date-picker v-model="form.grabEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="使用开始" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="使用结束" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { closeAdminVoucherApi, createAdminVoucherApi, deleteAdminVoucherApi, pageAdminVoucherApi, updateAdminVoucherApi } from '@/api/adminVoucher';

const records = ref([]);
const total = ref(0);
const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref();
const editingId = ref(null);
const query = reactive({ pageNum: 1, pageSize: 10, name: '', status: '', scopeType: '' });
const form = reactive({
  name: '',
  issuerType: 2,
  scopeType: 2,
  shopId: null,
  type: 1,
  noThreshold: false,
  minAmount: 0,
  discountAmount: 0,
  discountRate: 0.9,
  totalCount: 1,
  grabStartTime: '',
  grabEndTime: '',
  startTime: '',
  endTime: ''
});

const dialogTitle = computed(() => (editingId.value ? '编辑优惠券' : '发放优惠券'));

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  scopeType: [{ required: true, message: '请选择范围', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  noThreshold: [{ required: true, message: '请选择是否有门槛', trigger: 'change' }],
  minAmount: [{
    validator: (_, value, callback) => {
      if (form.noThreshold) return callback()
      if (value == null || value <= 0) return callback(new Error('请输入门槛金额'))
      callback()
    },
    trigger: 'change'
  }],
  discountAmount: [{
    validator: (_, value, callback) => {
      if (form.type !== 1) return callback()
      if (value == null || value <= 0) return callback(new Error('请输入优惠金额'))
      if (!form.noThreshold && form.minAmount > 0 && value > form.minAmount) {
        return callback(new Error('优惠金额不能大于最低消费'))
      }
      callback()
    },
    trigger: 'change'
  }],
  totalCount: [{ required: true, message: '请输入总数量', trigger: 'change' }],
  grabStartTime: [{ required: true, message: '请选择抢券开始时间', trigger: 'change' }],
  grabEndTime: [{ required: true, message: '请选择抢券结束时间', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择使用开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择使用结束时间', trigger: 'change' }]
};

onMounted(loadList);

async function loadList() {
  const result = await pageAdminVoucherApi(query);
  records.value = result.data.records || [];
  total.value = result.data.total || 0;
}

function resetForm() {
  editingId.value = null;
  form.name = '';
  form.issuerType = 2;
  form.scopeType = 2;
  form.shopId = null;
  form.type = 1;
  form.noThreshold = false;
  form.minAmount = 0;
  form.discountAmount = 0;
  form.discountRate = 0.9;
  form.totalCount = 1;
  form.grabStartTime = '';
  form.grabEndTime = '';
  form.startTime = '';
  form.endTime = '';
}

function openCreateDialog() {
  resetForm();
  dialogVisible.value = true;
}

function editVoucher(row) {
  editingId.value = row.id;
  form.name = row.name;
  form.issuerType = row.issuerType ?? 2;
  form.scopeType = row.scopeType ?? 2;
  form.shopId = row.shopId ?? null;
  form.type = row.type ?? 1;
  form.noThreshold = (row.minAmount ?? 0) <= 0;
  form.minAmount = row.minAmount ?? 0;
  form.discountAmount = row.discountAmount ?? 0;
  form.discountRate = row.discountRate ?? 0.9;
  form.totalCount = row.totalCount ?? 1;
  form.grabStartTime = row.grabStartTime;
  form.grabEndTime = row.grabEndTime;
  form.startTime = row.startTime;
  form.endTime = row.endTime;
  dialogVisible.value = true;
}

async function submitForm() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    const payload = {
      ...form,
      scopeType: 2,
      shopId: null,
      issuerType: 2,
      minAmount: form.noThreshold ? 0 : form.minAmount,
      discountAmount: form.type === 1 ? form.discountAmount : null,
      discountRate: form.type === 2 ? form.discountRate : null
    };
    if (editingId.value) {
      await updateAdminVoucherApi(editingId.value, payload);
      ElMessage.success('更新成功');
    } else {
      await createAdminVoucherApi(payload);
      ElMessage.success('创建成功');
    }
    dialogVisible.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

async function handleClose(row) {
  await ElMessageBox.confirm(`确认关闭优惠券「${row.name}」吗？`, '提示', { type: 'warning' });
  await closeAdminVoucherApi(row.id);
  ElMessage.success('已关闭');
  await loadList();
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除优惠券「${row.name}」吗？`, '提示', { type: 'warning' });
  await deleteAdminVoucherApi(row.id);
  ElMessage.success('已删除');
  await loadList();
}
</script>

<style scoped>
.voucher-page {
  background: #fff;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.page-desc {
  margin: 6px 0 0;
  color: #6b7280;
}

.toolbar {
  margin-bottom: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
