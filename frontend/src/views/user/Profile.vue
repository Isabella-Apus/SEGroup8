<template>
    <div class="page-card">
        <h2 class="page-title">个人资料</h2>
        <div class="profile-head">
            <el-avatar :size="88" :src="toAbsoluteUrl(form.avatar)">{{ (form.nickname || form.username || 'U').slice(0,
                1)
                }}</el-avatar>
            <el-upload :show-file-list="false" :http-request="uploadAvatar" accept="image/*">
                <el-button :loading="avatarUploading">{{ form.avatar ? '重新上传头像' : '上传头像' }}</el-button>
            </el-upload>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 600px">
            <el-form-item label="用户名">
                <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
                <el-input v-model="form.nickname" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" />
            </el-form-item>
            <el-form-item label="角色">
                <el-input v-model="form.role" disabled />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" :loading="loading" @click="saveProfile">保存</el-button>
            </el-form-item>
        </el-form>

        <div class="wallet-panel">
            <h3 class="wallet-title">我的钱包</h3>
            <div class="wallet-balance">商城币余额：<strong>{{ Number(walletBalance || 0).toFixed(2) }}</strong></div>
            <el-space>
                <el-button type="primary" @click="openRechargeDialog">充值商城币</el-button>
                <el-button @click="loadWalletData">刷新余额</el-button>
            </el-space>
            <el-table :data="walletRecords" border style="margin-top: 12px">
                <el-table-column prop="orderId" label="订单ID" width="100" />
                <el-table-column label="类型" min-width="180">
                    <template #default="scope">{{ resolveTradeTypeLabel(scope.row) }}</template>
                </el-table-column>
                <el-table-column prop="amount" label="金额" width="120">
                    <template #default="scope">
                        <span :class="Number(scope.row.amount || 0) >= 0 ? 'amount-plus' : 'amount-minus'">
                            {{ Number(scope.row.amount || 0).toFixed(2) }}
                        </span>
                    </template>
                </el-table-column>
                <el-table-column prop="remark" label="备注" min-width="220" />
                <el-table-column prop="createTime" label="时间" min-width="180" />
            </el-table>
        </div>

        <el-dialog v-model="rechargeDialogVisible" title="充值商城币" width="520px">
            <el-form label-width="90px">
                <el-form-item label="充值金额">
                    <el-input-number v-model="rechargeForm.amount" :min="0.01" :step="10" :precision="2" style="width: 220px" />
                </el-form-item>
                <el-form-item label="支付方式">
                    <el-radio-group v-model="rechargeForm.channel">
                        <el-radio-button value="WECHAT">微信</el-radio-button>
                        <el-radio-button value="ALIPAY">支付宝</el-radio-button>
                    </el-radio-group>
                </el-form-item>
                <div class="qr-placeholder">支付确认区</div>
                <div class="qr-tip">确认支付后，充值金额会写入钱包流水。</div>
            </el-form>
            <template #footer>
                <el-button @click="rechargeDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="rechargeLoading" @click="confirmRechargePaid">我已支付，确认入账</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getProfileApi, updateProfileApi } from '@/api/user';
import { uploadImageApi } from '@/api/upload';
import { getFinanceDashboardApi, getMyWalletRecordsApi, rechargeCoinApi } from '@/api/finance';
import { useUserStore } from '@/stores/user';
import { resolveTradeTypeLabel } from '@/utils/finance';

const userStore = useUserStore();
const formRef = ref();
const loading = ref(false);
const avatarUploading = ref(false);
const walletBalance = ref(0);
const walletRecords = ref([]);
const rechargeDialogVisible = ref(false);
const rechargeLoading = ref(false);
const rechargeForm = reactive({
    amount: 100,
    channel: 'WECHAT'
});
const form = reactive({
    username: '',
    nickname: '',
    avatar: '',
    phone: '',
    email: '',
    role: '',
});

const rules = {
    nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
    phone: [{ pattern: /^$|^1\d{10}$/, message: '手机号需为11位', trigger: 'blur' }]
};

onMounted(async () => {
    await loadProfile();
    await loadWalletData();
});

async function loadProfile() {
    const result = await getProfileApi();
    Object.assign(form, result.data || {});
}

async function uploadAvatar(option) {
    avatarUploading.value = true;
    try {
        const result = await uploadImageApi(option.file);
        form.avatar = result.data.url;
        await updateProfileApi({
            avatar: form.avatar
        });
        await userStore.fetchProfile();
        option.onSuccess(result);
        ElMessage.success('头像上传并保存成功');
    } catch (error) {
        option.onError(error);
    } finally {
        avatarUploading.value = false;
    }
}

function toAbsoluteUrl(url) {
    if (!url) {
        return '';
    }
    const normalizedUrl = String(url).replace(/\\\\/g, '/');
    if (/^https?:\/\//i.test(normalizedUrl)) {
        return encodeURI(normalizedUrl);
    }
    let withPrefix = normalizedUrl;
    if (!withPrefix.startsWith('/')) {
        withPrefix = withPrefix.startsWith('uploads/') ? `/${withPrefix}` : `/uploads/${withPrefix}`;
    }
    return encodeURI(`http://localhost:8080${withPrefix}`);
}

async function saveProfile() {
    await formRef.value.validate();
    loading.value = true;
    try {
        await updateProfileApi({
            nickname: form.nickname,
            avatar: form.avatar,
            phone: form.phone,
            email: form.email
        });
        await userStore.fetchProfile();
        ElMessage.success('资料已更新');
    } finally {
        loading.value = false;
    }
}

async function loadWalletData() {
    const dashboard = await getFinanceDashboardApi();
    walletBalance.value = dashboard.data?.personalBalance || 0;
    const records = await getMyWalletRecordsApi();
    walletRecords.value = records.data || [];
}

function openRechargeDialog() {
    rechargeForm.amount = 100;
    rechargeForm.channel = 'WECHAT';
    rechargeDialogVisible.value = true;
}

async function confirmRechargePaid() {
    if (!rechargeForm.amount || Number(rechargeForm.amount) <= 0) {
        ElMessage.warning('请输入有效充值金额');
        return;
    }
    rechargeLoading.value = true;
    try {
        await rechargeCoinApi({
            amount: Number(rechargeForm.amount),
            channel: rechargeForm.channel
        });
        rechargeDialogVisible.value = false;
        ElMessage.success('充值成功');
        await loadWalletData();
    } finally {
        rechargeLoading.value = false;
    }
}
</script>

<style scoped>
.profile-head {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
}

.wallet-panel {
    margin-top: 24px;
    padding-top: 12px;
    border-top: 1px solid #e5e7eb;
}

.wallet-title {
    margin: 0 0 8px;
    font-size: 18px;
}

.wallet-balance {
    margin-bottom: 10px;
}

.amount-plus {
    color: #16a34a;
    font-weight: 600;
}

.amount-minus {
    color: #dc2626;
    font-weight: 600;
}

.qr-placeholder {
    height: 180px;
    border: 2px dashed #d1d5db;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #6b7280;
    margin: 6px 0;
}

.qr-tip {
    color: #9ca3af;
    font-size: 12px;
}
</style>
