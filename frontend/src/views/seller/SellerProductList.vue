<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">商品管理</h2>
      <el-button type="primary" @click="$router.push('/merchant/seller-products/edit')">
        + 发布新商品
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-row :gutter="12">
        <el-col :span="8">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索商品名称"
            clearable
            @keyup.enter="loadProducts"
          />
        </el-col>
        <el-col :span="6">
          <el-select
            v-model="searchForm.status"
            placeholder="全部状态"
            clearable
          >
            <el-option label="在售" :value="1" />
            <el-option label="已下架" :value="0" />
            <el-option label="审核中" :value="2" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadProducts">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 商品列表 -->
    <el-card class="table-card">
      <el-table :data="products" v-loading="loading" stripe>
        <el-table-column label="商品图片" width="90">
          <template #default="{ row }">
            <el-image
              :src="row.imageUrl"
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 4px"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="160" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ row.statusName || statusLabel(row.status) }}
            </el-tag>
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

      <!-- 分页 -->
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
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMyProducts,
  updateProductStatus,
  deleteProduct
} from '@/api/seller'

const loading = ref(false)
const products = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  keyword: '',
  status: null
})

async function loadProducts() {
  loading.value = true
  try {
    const res = await getMyProducts({
      page: page.value,
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

function toFullImageUrl(url) {
  if (!url) {
    return ''
  }
  return url.startsWith('http') ? url : `http://localhost:8080${url}`
}

onMounted(loadProducts)
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
.search-card {
  margin-bottom: 16px;
}
.table-card {
  margin-bottom: 16px;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>