<template>
    <div class="merchant-product-page">
        <section class="workbench-hero">
            <div>
                <span class="eyebrow">Official Goods</span>
                <h2 class="page-title">新品商品管理</h2>
                <p>维护官方商品、库存和上下架状态，买家下单前看到的商品都在这里。</p>
            </div>
            <div class="hero-actions">
                <el-button @click="routerBackToMall">返回商城</el-button>
                <el-button type="primary" @click="openCreate">新增商品</el-button>
            </div>
        </section>

        <section class="metric-row">
            <div class="metric-card">
                <span>当前结果</span>
                <strong>{{ total }}</strong>
            </div>
            <div class="metric-card">
                <span>在售商品</span>
                <strong>{{ activeCount }}</strong>
            </div>
            <div class="metric-card">
                <span>库存合计</span>
                <strong>{{ stockTotal }}</strong>
            </div>
            <div class="metric-card">
                <span>低库存</span>
                <strong>{{ lowStockCount }}</strong>
            </div>
        </section>

        <el-form :inline="true" :model="query" class="query-form">
            <el-form-item label="关键字">
                <el-input v-model="query.keyword" placeholder="商品名" clearable style="width: 180px" />
            </el-form-item>
            <el-form-item label="状态">
                <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
                    <el-option label="在售" :value="1" />
                    <el-option label="已下架" :value="0" />
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="search">查询</el-button>
                <el-button @click="reset">重置</el-button>
            </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="list" class="goods-table">
            <el-table-column label="商品" min-width="300">
                <template #default="scope">
                    <div class="goods-cell">
                        <el-image v-if="scope.row.cover" :src="toFullImageUrl(scope.row.cover)" fit="cover" class="table-cover" />
                        <span v-else class="table-cover cover-empty">无图</span>
                        <div class="goods-meta">
                            <strong>{{ scope.row.name }}</strong>
                            <span>{{ scope.row.categoryName || scope.row.category || '生活百货' }} · ID {{ scope.row.id }}</span>
                        </div>
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="价格" width="120">
                <template #default="{ row }">¥{{ Number(row.price || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="库存" width="100">
                <template #default="{ row }">
                    <el-tag :type="Number(row.stock || 0) <= 5 ? 'warning' : 'info'" effect="plain">
                        {{ Number(row.stock || 0) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
                <template #default="{ row }">
                    <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'" effect="plain">
                        {{ row.statusName || (Number(row.status) === 1 ? '在售' : '已下架') }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="操作" min-width="340">
                <template #default="scope">
                    <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
                    <el-button link @click="toggleStatus(scope.row)">{{ scope.row.status === 1 ? '下架' : '上架' }}</el-button>
                    <el-button link @click="openAdjustStock(scope.row)">调库存</el-button>
                    <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

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

        <el-dialog
            v-model="formVisible"
            :title="form.id ? '编辑商品' : '新增商品'"
            width="560px"
            align-center
            append-to-body
        >
            <el-form :model="form" :rules="rules" ref="formRef" label-width="88px">
                <el-form-item label="商品名称" prop="name">
                    <el-input v-model="form.name" maxlength="120" show-word-limit />
                </el-form-item>
                <el-form-item label="商品分类" prop="category">
                    <el-select v-model="form.category" placeholder="请选择分类" style="width: 180px">
                        <el-option
                            v-for="item in productCategoryOptions"
                            :key="item"
                            :label="item"
                            :value="item"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="商品封面">
                    <el-space>
                        <el-upload :show-file-list="false" :http-request="uploadCover" accept="image/*">
                            <el-button :loading="uploading">上传图片</el-button>
                        </el-upload>
                        <el-image v-if="form.cover" :src="toFullImageUrl(form.cover)" fit="cover" class="dialog-cover" />
                    </el-space>
                </el-form-item>
                <el-form-item label="商品价格" prop="price">
                    <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="10" style="width: 180px" />
                </el-form-item>
                <el-form-item label="库存" prop="stock">
                    <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 180px" />
                </el-form-item>
                <el-form-item label="上架状态">
                    <el-switch v-model="statusSwitch" />
                </el-form-item>
                <el-form-item label="商品描述">
                    <el-input v-model="form.description" type="textarea" :rows="4" maxlength="2000" show-word-limit />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="formVisible = false">取消</el-button>
                <el-button type="primary" @click="submit">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="stockVisible" title="调整库存" width="420px" align-center append-to-body>
            <el-form label-width="90px">
                <el-form-item label="当前商品">
                    <span>{{ currentRow?.name || '-' }}</span>
                </el-form-item>
                <el-form-item label="调整数量">
                    <el-input-number v-model="stockDelta" :step="1" />
                    <span class="empty-tip" style="margin-left: 8px">可填正数或负数</span>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="stockVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmAdjustStock">确认调整</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import {
    getSellerProductListApi,
    createSellerProductApi,
    updateSellerProductApi,
    deleteSellerProductApi,
    changeSellerProductStatusApi,
    adjustSellerProductStockApi
} from '@/api/product';
import { uploadImageApi } from '@/api/upload';
import { ALL_CATEGORY, productCategories } from '@/utils/categoryRules';
import { toAssetUrl } from '@/utils/url';

const router = useRouter();
const loading = ref(false);
const list = ref([]);
const total = ref(0);
const uploading = ref(false);
const productCategoryOptions = productCategories.filter((item) => item !== ALL_CATEGORY);
const activeCount = computed(() => list.value.filter((item) => Number(item.status) === 1).length);
const stockTotal = computed(() => list.value.reduce((sum, item) => sum + Number(item.stock || 0), 0));
const lowStockCount = computed(() => list.value.filter((item) => Number(item.stock || 0) <= 5).length);

const query = reactive({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    status: undefined
});

const formVisible = ref(false);
const formRef = ref();
const form = reactive({
    id: null,
    name: '',
    cover: '',
    description: '',
    category: '生活百货',
    price: 1,
    stock: 0
});
const statusSwitch = ref(true);

const stockVisible = ref(false);
const currentRow = ref(null);
const stockDelta = ref(1);

const rules = {
    name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
    category: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
    price: [{ required: true, message: '请输入商品价格', trigger: 'change' }],
    stock: [{ required: true, message: '请输入库存', trigger: 'change' }]
};

onMounted(() => {
    fetchList();
});

async function fetchList() {
    loading.value = true;
    try {
        const result = await getSellerProductListApi(query);
        list.value = result.data?.records || [];
        total.value = result.data?.total || 0;
    } finally {
        loading.value = false;
    }
}

function search() {
    query.pageNum = 1;
    fetchList();
}

function reset() {
    query.pageNum = 1;
    query.pageSize = 10;
    query.keyword = '';
    query.status = undefined;
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

function openCreate() {
    form.id = null;
    form.name = '';
    form.cover = '';
    form.description = '';
    form.category = '生活百货';
    form.price = 1;
    form.stock = 0;
    statusSwitch.value = true;
    formVisible.value = true;
}

function routerBackToMall() {
    router.push('/product');
}

function openEdit(row) {
    form.id = row.id;
    form.name = row.name;
    form.cover = row.cover || '';
    form.description = row.description || '';
    form.category = row.categoryName || row.category || '生活百货';
    form.price = Number(row.price || 0);
    form.stock = Number(row.stock || 0);
    statusSwitch.value = row.status === 1;
    formVisible.value = true;
}

async function submit() {
    await formRef.value?.validate();
    const payload = {
        name: form.name,
        cover: form.cover,
        description: form.description,
        categoryName: form.category,
        category: form.category,
        price: form.price,
        stock: form.stock,
        status: statusSwitch.value ? 1 : 0
    };
    if (form.id) {
        await updateSellerProductApi(form.id, payload);
        ElMessage.success('商品更新成功');
    } else {
        await createSellerProductApi(payload);
        ElMessage.success('商品创建成功');
    }
    formVisible.value = false;
    fetchList();
}

async function toggleStatus(row) {
    const targetStatus = row.status === 1 ? 0 : 1;
    await changeSellerProductStatusApi(row.id, targetStatus);
    ElMessage.success(targetStatus === 1 ? '已上架' : '已下架');
    fetchList();
}

function openAdjustStock(row) {
    currentRow.value = row;
    stockDelta.value = 1;
    stockVisible.value = true;
}

async function confirmAdjustStock() {
    if (!currentRow.value) {
        return;
    }
    if (!stockDelta.value) {
        ElMessage.warning('调整数量不能为0');
        return;
    }
    await adjustSellerProductStockApi(currentRow.value.id, Number(stockDelta.value));
    ElMessage.success('库存调整成功');
    stockVisible.value = false;
    fetchList();
}

async function remove(row) {
    await ElMessageBox.confirm(`确认删除商品「${row.name}」吗？`, '提示', { type: 'warning' });
    await deleteSellerProductApi(row.id);
    ElMessage.success('删除成功');
    fetchList();
}

async function uploadCover(option) {
    uploading.value = true;
    try {
        const result = await uploadImageApi(option.file);
        form.cover = result.data?.url || '';
        option.onSuccess?.(result);
    } catch (error) {
        option.onError?.(error);
    } finally {
        uploading.value = false;
    }
}

function toFullImageUrl(url) {
    return toAssetUrl(url);
}
</script>

<style scoped>
.merchant-product-page {
    display: grid;
    gap: 14px;
}

.workbench-hero {
    min-height: 146px;
    border: 1px solid rgba(137, 199, 255, 0.32);
    border-radius: 8px;
    background:
        linear-gradient(135deg, rgba(233, 255, 248, 0.94), rgba(234, 244, 255, 0.94) 58%, rgba(255, 247, 251, 0.92)),
        #ffffff;
    padding: 22px;
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    gap: 18px;
    box-shadow: var(--shadow-soft);
}

.eyebrow {
    color: var(--brand-primary);
    font-size: 12px;
    font-weight: 900;
}

.workbench-hero .page-title {
    margin: 8px 0 6px;
}

.workbench-hero p {
    margin: 0;
    color: var(--text-secondary);
    font-weight: 700;
}

.hero-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 10px;
}

.metric-row {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 10px;
}

.metric-card {
    min-height: 82px;
    border: 1px solid rgba(137, 199, 255, 0.28);
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.88);
    padding: 14px;
    display: grid;
    align-content: center;
    gap: 4px;
    box-shadow: var(--shadow-soft);
}

.metric-card span {
    color: var(--text-secondary);
    font-size: 12px;
    font-weight: 900;
}

.metric-card strong {
    color: var(--text-main);
    font-size: 28px;
    line-height: 1;
}

.query-form {
    margin: 0;
    border: 1px solid rgba(137, 199, 255, 0.24);
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.9);
    padding: 12px 12px 0;
    box-shadow: var(--shadow-soft);
}

.goods-table {
    border: 1px solid rgba(137, 199, 255, 0.22);
    border-radius: 8px;
    overflow: hidden;
    box-shadow: var(--shadow-soft);
}

.goods-cell {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
}

.goods-meta {
    min-width: 0;
    display: grid;
    gap: 5px;
}

.goods-meta strong,
.goods-meta span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.goods-meta span {
    color: var(--text-muted);
    font-size: 12px;
    font-weight: 700;
}

.pager-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
}

.table-cover {
    width: 56px;
    height: 56px;
    border-radius: 6px;
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

.dialog-cover {
    width: 64px;
    height: 64px;
    border-radius: 6px;
}

@media (max-width: 900px) {
    .workbench-hero {
        flex-direction: column;
        align-items: stretch;
    }

    .hero-actions {
        justify-content: flex-start;
    }

    .metric-row {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 560px) {
    .metric-row {
        grid-template-columns: 1fr;
    }
}
</style>
