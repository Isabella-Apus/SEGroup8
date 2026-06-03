<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">优惠券管理</h2>
      <el-button type="primary" @click="openDialog()">+ 创建优惠券</el-button>
    </div>

    <!-- 列表 -->
    <el-card v-loading="loading">
      <div v-if="vouchers.length === 0 && !loading" class="empty-tip">
        暂无优惠券，点击右上角创建
      </div>

      <el-table :data="vouchers" stripe>
        <el-table-column prop="name" label="优惠券名称" min-width="150" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 1 ? 'primary' : 'warning'">
              {{ row.typeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠内容" min-width="160">
          <template #default="{ row }">
            <span v-if="row.type === 1">
              满 ¥{{ row.minAmount }} 减 ¥{{ row.discountAmount }}
            </span>
            <span v-else>
              满 ¥{{ row.minAmount }} 打 {{ (row.discountRate * 10).toFixed(1) }} 折
            </span>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="120">
          <template #default="{ row }">
            <span>{{ row.usedCount }}/{{ row.totalCount }}</span>
            <el-progress
              :percentage="Math.round(row.usedCount / row.totalCount * 100)"
              :show-text="false"
              style="margin-top: 4px"
            />
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="200">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }} ~
            {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ row.statusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              @click="openDialog(row)"
              :disabled="row.status === 0 || row.status === 3"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              type="warning"
              @click="handleClose(row)"
              v-if="row.status === 1 || row.status === 2"
            >
              关闭
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="handleDelete(row)"
              v-if="row.usedCount === 0"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadVouchers"
        />
      </div>
    </el-card>

    <!-- 创建/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑优惠券' : '创建优惠券'"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：新人专享券" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="优惠类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减券</el-radio>
            <el-radio :value="2">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="最低消费" prop="minAmount">
          <el-input-number
            v-model="form.minAmount"
            :min="0"
            :precision="2"
            style="width: 180px"
          />
          <span style="margin-left: 8px">元</span>
        </el-form-item>

        <el-form-item v-if="form.type === 1" label="优惠金额" prop="discountAmount">
          <el-input-number
            v-model="form.discountAmount"
            :min="0.01"
            :precision="2"
            style="width: 180px"
          />
          <span style="margin-left: 8px">元</span>
        </el-form-item>

        <el-form-item v-if="form.type === 2" label="折扣率" prop="discountRate">
          <el-input-number
            v-model="form.discountRate"
            :min="0.01"
            :max="0.99"
            :precision="2"
            :step="0.1"
            style="width: 180px"
          />
          <span style="margin-left: 8px">（0.8 = 8折）</span>
        </el-form-item>

        <el-form-item label="发放总量" prop="totalCount">
          <el-input-number
            v-model="form.totalCount"
            :min="1"
            :step="10"
            style="width: 180px"
          />
          <span style="margin-left: 8px">张</span>
        </el-form-item>

        <el-form-item label="有效期" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingId ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMyVouchers,
  createVoucher,
  updateVoucher,
  closeVoucher,
  deleteVoucher
} from '@/api/seller'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const vouchers = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const formRef = ref(null)

const form = reactive({
  name: '',
  type: 1,
  minAmount: 0,
  discountAmount: null,
  discountRate: null,
  totalCount: 100,
  timeRange: null
})

const rules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  minAmount: [{ required: true, message: '请输入最低消费', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入发放总量', trigger: 'blur' }],
  timeRange: [{ required: true, message: '请选择有效期', trigger: 'change' }]
}

async function loadVouchers() {
  loading.value = true
  try {
    const res = await getMyVouchers({ page: page.value, pageSize: pageSize.value })
    vouchers.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function openDialog(row = null) {
  editingId.value = row ? row.id : null
  if (row) {
    form.name = row.name
    form.type = row.type
    form.minAmount = row.minAmount
    form.discountAmount = row.discountAmount
    form.discountRate = row.discountRate
    form.totalCount = row.totalCount
    form.timeRange = [row.startTime, row.endTime]
  } else {
    form.name = ''
    form.type = 1
    form.minAmount = 0
    form.discountAmount = null
    form.discountRate = null
    form.totalCount = 100
    form.timeRange = null
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        name: form.name,
        type: form.type,
        minAmount: form.minAmount,
        discountAmount: form.type === 1 ? form.discountAmount : null,
        discountRate: form.type === 2 ? form.discountRate : null,
        totalCount: form.totalCount,
        startTime: form.timeRange[0],
        endTime: form.timeRange[1]
      }
      if (editingId.value) {
        await updateVoucher(editingId.value, payload)
        ElMessage.success('更新成功')
      } else {
        await createVoucher(payload)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadVouchers()
    } catch (e) {
      ElMessage.error(editingId.value ? '更新失败' : '创建失败')
    } finally {
      submitting.value = false
    }
  })
}

async function handleClose(row) {
  try {
    await ElMessageBox.confirm(`确定关闭「${row.name}」吗？关闭后用户将无法领取`, '提示', {
      type: 'warning'
    })
    await closeVoucher(row.id)
    ElMessage.success('已关闭')
    loadVouchers()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', {
      type: 'warning'
    })
    await deleteVoucher(row.id)
    ElMessage.success('删除成功')
    loadVouchers()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

function statusTagType(status) {
  const map = { 0: 'danger', 1: 'success', 2: 'info', 3: 'warning' }
  return map[status] ?? 'info'
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(loadVouchers)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title {
  margin: 0;
  font-size: 20px;
}
.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
