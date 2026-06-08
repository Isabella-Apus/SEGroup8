<template>
  <section class="seller-goods-page">
    <div class="seller-hero">
      <div>
        <span>Seller Center</span>
        <h1>卖家工作台</h1>
        <p>管理新品商品、库存和上下架状态。</p>
      </div>
      <div class="hero-actions">
        <el-button @click="router.push('/')">返回商城</el-button>
        <el-button type="primary" @click="router.push('/merchant/seller-products/edit')">发布新商品</el-button>
      </div>
    </div>

    <div class="score-grid">
      <article class="score-card">
        <div>
          <span class="score-label">当前结果</span>
          <strong>{{ total }}</strong>
          <small>商品</small>
        </div>
        <el-progress type="dashboard" :percentage="Math.min(total, 100)" :width="96" :stroke-width="10" color="#89c7ff" />
      </article>
      <article class="score-card">
        <div>
          <span class="score-label">在售商品</span>
          <strong>{{ activeCount }}</strong>
          <small>正在展示</small>
        </div>
        <el-progress type="dashboard" :percentage="activePercent" :width="96" :stroke-width="10" color="#5fe6bd" />
      </article>
      <article class="score-card">
        <div>
          <span class="score-label">低库存</span>
          <strong>{{ lowStockCount }}</strong>
          <small>库存不超过 5</small>
        </div>
        <el-progress type="dashboard" :percentage="lowStockPercent" :width="96" :stroke-width="10" color="#ffb9d6" />
      </article>
    </div>

    <article class="panel">
      <div class="panel-head">
        <div>
          <h2>商品列表</h2>
          <span>维护价格、库存和上架状态</span>
        </div>
      </div>

      <div class="search-card">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索商品名称"
          clearable
          @keyup.enter="loadProducts"
        />
        <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
          <el-option label="在售" :value="1" />
          <el-option label="已下架" :value="0" />
          <el-option label="审核中" :value="2" />
        </el-select>
        <el-button type="primary" @click="loadProducts">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="products" v-loading="loading" class="goods-table">
        <el-table-column label="商品" min-width="280">
          <template #default="{ row }">
            <div class="goods-cell">
              <el-image v-if="row.imageUrl" :src="row.imageUrl" fit="cover" class="table-cover" />
              <span v-else class="table-cover cover-empty">无图</span>
              <div class="goods-meta">
                <strong>{{ row.name }}</strong>
                <small>{{ row.categoryName || row.category || '生活百货' }} · ID {{ row.id }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="120">
          <template #default="{ row }">
            ¥{{ Number(row.price || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="库存" width="110">
          <template #default="{ row }">
            <el-tag :type="Number(row.stock || 0) <= 5 ? 'warning' : 'info'" effect="plain">
              {{ Number(row.stock || 0) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">
              {{ row.statusName || statusLabel(row.status) }}
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
            <span v-else class="risk-empty">待生成</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              @click="$router.push(`/merchant/seller-products/edit/${row.id}`)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="handleDelete(row)"
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
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadProducts"
        />
      </div>
    </article>
  </section>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMyProducts,
  updateProductStatus,
  deleteProduct
} from '@/api/seller'
import { toAssetUrl } from '@/utils/url'

const router = useRouter()
const loading = ref(false)
const products = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  keyword: '',
  status: null
})

const activeCount = computed(() => products.value.filter(item => Number(item.status) === 1).length)
const lowStockCount = computed(() => products.value.filter(item => Number(item.stock || 0) <= 5).length)
const activePercent = computed(() => products.value.length ? Math.round((activeCount.value / products.value.length) * 100) : 0)
const lowStockPercent = computed(() => products.value.length ? Math.round((lowStockCount.value / products.value.length) * 100) : 0)

async function loadProducts() {
  loading.value = true
  try {
    const res = await getMyProducts({
      pageNum: page.value,
      pageSize: pageSize.value,
      keyword: searchForm.keyword || undefined,
      status: searchForm.status ?? undefined
    })
    products.value = (res.data.records || []).map(item => ({
        ...item,
        imageUrl: toFullImageUrl(item.cover)
    }))
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载商品失败')
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchForm.keyword = ''
  searchForm.status = null
  page.value = 1
  loadProducts()
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateProductStatus(row.id, newStatus)
    ElMessage.success(newStatus === 1 ? '上架成功' : '下架成功')
    loadProducts()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', {
      type: 'warning'
    })
    await deleteProduct(row.id)
    ElMessage.success('删除成功')
    loadProducts()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

function statusTagType(status) {
  const map = { 1: 'success', 0: 'info', 2: 'warning' }
  return map[status] ?? 'info'
}

function statusLabel(status) {
  const map = { 1: '在售', 0: '已下架', 2: '审核中' }
  return map[status] ?? status
}

function riskLabel(level) {
  const map = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' }
  return map[level] ?? '未评估'
}

function riskTagType(level) {
  const map = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' }
  return map[level] ?? 'info'
}

function auditStatusLabel(status) {
  const map = { PENDING: '待处理', APPROVED: '已通过', REJECTED: '已驳回', CHANGE_REQUESTED: '要求修改' }
  return map[status] ?? '待处理'
}

function toFullImageUrl(url) {
  return toAssetUrl(url)
}

onMounted(loadProducts)
</script>

<style scoped>
.seller-goods-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.seller-hero {
  min-height: 160px;
  border: 1px solid rgba(137, 199, 255, 0.36);
  border-radius: 8px;
  padding: 22px;
  background: linear-gradient(135deg, #e9fff8 0%, #eaf4ff 54%, #fff7fb 100%);
  box-shadow: var(--shadow-soft);
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.seller-hero span {
  color: var(--brand-primary);
  font-weight: 900;
}

.seller-hero h1 {
  margin: 10px 0 8px;
  font-size: 34px;
}

.seller-hero p {
  max-width: 760px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
  font-weight: 700;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.score-card,
.panel {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: var(--shadow-soft);
}

.score-card {
  min-height: 138px;
  padding: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.score-label,
.score-card small {
  display: block;
  color: var(--text-secondary);
  font-weight: 800;
}

.score-card strong {
  display: block;
  margin: 8px 0 4px;
  color: var(--text-main);
  font-size: 34px;
  line-height: 1;
}

.panel {
  padding: 16px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}

.panel-head h2 {
  margin: 0;
  font-size: 20px;
}

.panel-head span {
  color: var(--text-muted);
  font-weight: 800;
}

.search-card {
  margin-bottom: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 170px auto auto;
  gap: 10px;
  align-items: center;
}

.goods-table {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  overflow: hidden;
}

.goods-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.table-cover {
  width: 58px;
  height: 58px;
  border-radius: 8px;
  border: 1px solid var(--line-soft);
  overflow: hidden;
  flex: 0 0 auto;
}

.cover-empty {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  background: var(--surface-soft);
  font-size: 12px;
  font-weight: 800;
}

.goods-meta {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.goods-meta strong,
.goods-meta small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-meta small {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.risk-cell {
  display: grid;
  gap: 4px;
  justify-items: start;
}

.risk-cell small,
.risk-empty {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 980px) {
  .seller-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .score-grid {
    grid-template-columns: 1fr;
  }

  .search-card {
    grid-template-columns: 1fr;
  }
}
</style>
