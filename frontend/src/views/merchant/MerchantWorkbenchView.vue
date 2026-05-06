<template>
    <div class="page-card">
        <div class="head-row">
            <h2 class="page-title">商品管理</h2>
            <el-button type="primary" @click="openCreate">新增商品</el-button>
        </div>

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

        <el-table v-loading="loading" :data="list" border>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="商品名" min-width="180" />
            <el-table-column label="封面" width="96">
                <template #default="scope">
                    <el-image v-if="scope.row.cover" :src="toFullImageUrl(scope.row.cover)" fit="cover" class="table-cover" />
                    <span v-else class="empty-tip">无</span>
                </template>
            </el-table-column>
            <el-table-column prop="price" label="价格" width="110" />
            <el-table-column prop="stock" label="库存" width="90" />
            <el-table-column prop="statusName" label="状态" width="90" />
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

        <el-dialog v-model="formVisible" :title="form.id ? '编辑商品' : '新增商品'" width="560px">
            <el-form :model="form" :rules="rules" ref="formRef" label-width="88px">
                <el-form-item label="商品名称" prop="name">
                    <el-input v-model="form.name" maxlength="120" show-word-limit />
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

        <el-dialog v-model="stockVisible" title="调整库存" width="420px">
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
import {
    getSellerProductListApi,
    createSellerProductApi,
    updateSellerProductApi,
    deleteSellerProductApi,
    changeSellerProductStatusApi,
    adjustSellerProductStockApi
} from '@/api/product';
import { uploadImageApi } from '@/api/upload';
import { toApiAssetUrl } from '@/utils/url';

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const uploading = ref(false);

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
    price: 1,
    stock: 0
});
const statusSwitch = ref(true);

const stockVisible = ref(false);
const currentRow = ref(null);
const stockDelta = ref(1);

const rules = {
    name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
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
    form.price = 1;
    form.stock = 0;
    statusSwitch.value = true;
    formVisible.value = true;
}

function openEdit(row) {
    form.id = row.id;
    form.name = row.name;
    form.cover = row.cover || '';
    form.description = row.description || '';
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
    if (!url) {
        return '';
    }
    return toApiAssetUrl(url);
}
</script>

<style scoped>
.head-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.query-form {
    margin-bottom: 12px;
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
}

.dialog-cover {
    width: 64px;
    height: 64px;
    border-radius: 6px;
}
</style>
