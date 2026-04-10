<template>
    <div class="page-card">
        <h2 class="page-title">订单管理</h2>

        <el-form :inline="true" :model="query" class="query-form">
            <el-form-item label="关键字">
                <el-input v-model="query.keyword" placeholder="订单号/商品名" clearable style="width: 220px" />
            </el-form-item>
            <el-form-item label="订单状态">
                <el-input-number v-model="query.orderStatus" :min="0" :max="10" :step="1" style="width: 120px" />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="search">查询</el-button>
                <el-button @click="reset">重置</el-button>
            </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="records" border>
            <el-table-column prop="orderNo" label="订单号" min-width="200" />
            <el-table-column prop="buyerUserId" label="买家ID" width="90" />
            <el-table-column label="金额" width="120">
                <template #default="scope">￥{{ Number(scope.row.totalAmount || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="orderStatusName" label="订单状态" width="130" />
            <el-table-column prop="refundStatusName" label="售后状态" width="130" />
            <el-table-column prop="createTime" label="下单时间" min-width="180" />
            <el-table-column label="操作" min-width="280">
                <template #default="scope">
                    <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
                    <el-button v-if="canShip(scope.row)" link type="success" @click="ship(scope.row)">发货</el-button>
                    <el-button v-if="canApproveRefund(scope.row)" link type="success" @click="approveRefund(scope.row)">同意退货</el-button>
                    <el-button v-if="canRejectRefund(scope.row)" link type="danger" @click="rejectRefund(scope.row)">拒绝退货</el-button>
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

        <el-dialog v-model="detailVisible" title="订单详情" width="860px">
            <el-descriptions v-if="detail" :column="2" border>
                <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
                <el-descriptions-item label="买家ID">{{ detail.buyerUserId }}</el-descriptions-item>
                <el-descriptions-item label="订单状态">{{ detail.orderStatusName }}</el-descriptions-item>
                <el-descriptions-item label="售后状态">{{ detail.refundStatusName }}</el-descriptions-item>
                <el-descriptions-item label="收货人">{{ detail.receiverName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="联系电话">{{ detail.receiverPhone || '-' }}</el-descriptions-item>
                <el-descriptions-item label="收货地址">{{ fullAddress(detail) }}</el-descriptions-item>
                <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
            <el-table v-if="detail" :data="detail.items || []" border style="margin-top: 12px">
                <el-table-column prop="productName" label="商品名" min-width="220" />
                <el-table-column prop="productType" label="类型" width="120" />
                <el-table-column prop="price" label="单价" width="120">
                    <template #default="scope">￥{{ Number(scope.row.price || 0).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column prop="quantity" label="数量" width="100" />
            </el-table>
        </el-dialog>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
    approveSellerRefundApi,
    getSellerOrderDetailApi,
    getSellerOrderListApi,
    rejectSellerRefundApi,
    shipSellerOrderApi,
} from '@/api/order';

const loading = ref(false);
const total = ref(0);
const records = ref([]);
const detailVisible = ref(false);
const detail = ref(null);

const query = reactive({
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    orderStatus: undefined,
});

onMounted(() => {
    fetchOrders();
});

async function fetchOrders() {
    loading.value = true;
    try {
        const result = await getSellerOrderListApi(query);
        records.value = result.data?.records || [];
        total.value = result.data?.total || 0;
    } finally {
        loading.value = false;
    }
}

function search() {
    query.pageNum = 1;
    fetchOrders();
}

function reset() {
    query.pageNum = 1;
    query.pageSize = 10;
    query.keyword = '';
    query.orderStatus = undefined;
    fetchOrders();
}

function handlePageChange(pageNum) {
    query.pageNum = pageNum;
    fetchOrders();
}

function handleSizeChange(pageSize) {
    query.pageSize = pageSize;
    query.pageNum = 1;
    fetchOrders();
}

function canShip(order) {
    return Number(order?.orderStatus) === 1;
}

function canApproveRefund(order) {
    return Number(order?.refundStatus) === 1;
}

function canRejectRefund(order) {
    return Number(order?.refundStatus) === 1;
}

async function ship(order) {
    await shipSellerOrderApi(order.id);
    ElMessage.success('发货成功');
    await fetchOrders();
}

async function approveRefund(order) {
    await approveSellerRefundApi(order.id);
    ElMessage.success('已同意退货');
    await fetchOrders();
}

async function rejectRefund(order) {
    await rejectSellerRefundApi(order.id);
    ElMessage.success('已拒绝退货');
    await fetchOrders();
}

async function openDetail(order) {
    const result = await getSellerOrderDetailApi(order.id);
    detail.value = result.data || null;
    detailVisible.value = true;
}

function fullAddress(order) {
    if (!order) {
        return '-';
    }
    const parts = [order.receiverProvince, order.receiverCity, order.receiverDetailAddress]
        .filter(Boolean);
    return parts.length ? parts.join(' ') : '-';
}
</script>

<style scoped>
.query-form {
    margin-bottom: 12px;
}

.pager-wrap {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
}
</style>
