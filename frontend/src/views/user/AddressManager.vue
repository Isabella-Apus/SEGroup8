<template>
    <div class="page-card">
        <div class="toolbar">
            <h2 class="page-title" style="margin: 0">地址管理</h2>
            <el-button type="primary" @click="openDialog()">新增地址</el-button>
        </div>

        <el-table :data="addresses" border>
            <el-table-column prop="receiverName" label="收件人" width="120" />
            <el-table-column prop="receiverPhone" label="电话" width="140" />
            <el-table-column label="地址">
                <template #default="{ row }">{{ row.province }} {{ row.city }} {{ row.detailAddress }}</template>
            </el-table-column>
            <el-table-column label="默认" width="90">
                <template #default="{ row }">
                    <el-tag v-if="row.isDefault === 1" type="success">默认</el-tag>
                    <span v-else>-</span>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="220">
                <template #default="{ row }">
                    <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
                    <el-popconfirm title="确定删除该地址吗？" @confirm="removeAddress(row.id)">
                        <template #reference>
                            <el-button link type="danger">删除</el-button>
                        </template>
                    </el-popconfirm>
                </template>
            </el-table-column>
        </el-table>

        <el-dialog
            v-model="dialogVisible"
            :title="form.id ? '编辑地址' : '新增地址'"
            width="560px"
            append-to-body
            align-center
            class="kg-dialog"
            modal-class="kg-dialog-overlay"
        >
            <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
                <el-form-item label="收件人" prop="receiverName">
                    <el-input v-model="form.receiverName" />
                </el-form-item>
                <el-form-item label="联系电话" prop="receiverPhone">
                    <el-input v-model="form.receiverPhone" />
                </el-form-item>
                <el-form-item label="省份" prop="province">
                    <el-select v-model="form.province" filterable placeholder="请选择省份" style="width: 100%">
                        <el-option v-for="province in provinceOptions" :key="province" :label="province"
                            :value="province" />
                    </el-select>
                </el-form-item>
                <el-form-item label="城市" prop="city">
                    <el-input v-model="form.city" />
                </el-form-item>
                <el-form-item label="详细地址" prop="detailAddress">
                    <el-input v-model="form.detailAddress" type="textarea" />
                </el-form-item>
                <el-form-item label="默认地址" prop="isDefault">
                    <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveAddress">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { createAddressApi, deleteAddressApi, listAddressesApi, updateAddressApi } from '@/api/user';

const addresses = ref([]);
const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref();
const form = reactive(resetForm());

const rules = {
    receiverName: [{ required: true, message: '请输入收件人', trigger: 'blur' }],
    receiverPhone: [
        { required: true, message: '请输入联系电话', trigger: 'blur' },
        { pattern: /^1\d{10}$/, message: '手机号需为11位', trigger: 'blur' }
    ],
    province: [{ required: true, message: '请选择省份', trigger: 'change' }],
    city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
    detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
};

const provinceOptions = [
    '北京市', '天津市', '上海市', '重庆市',
    '河北省', '山西省', '辽宁省', '吉林省', '黑龙江省',
    '江苏省', '浙江省', '安徽省', '福建省', '江西省', '山东省',
    '河南省', '湖北省', '湖南省', '广东省', '海南省',
    '四川省', '贵州省', '云南省', '陕西省', '甘肃省', '青海省',
    '台湾省',
    '内蒙古自治区', '广西壮族自治区', '西藏自治区', '宁夏回族自治区', '新疆维吾尔自治区',
    '香港特别行政区', '澳门特别行政区'
];

onMounted(loadAddresses);

function resetForm() {
    return {
        id: null,
        receiverName: '',
        receiverPhone: '',
        province: '',
        city: '',
        detailAddress: '',
        isDefault: 0
    };
}

async function loadAddresses() {
    const result = await listAddressesApi();
    addresses.value = result.data || [];
}

function openDialog(row) {
    Object.assign(form, resetForm(), row || {});
    dialogVisible.value = true;
}

async function saveAddress() {
    await formRef.value.validate();
    saving.value = true;
    try {
        const payload = {
            receiverName: form.receiverName,
            receiverPhone: form.receiverPhone,
            province: form.province,
            city: form.city,
            detailAddress: form.detailAddress,
            isDefault: form.isDefault
        };
        if (form.id) {
            await updateAddressApi(form.id, payload);
        } else {
            await createAddressApi(payload);
        }
        ElMessage.success('保存成功');
        dialogVisible.value = false;
        await loadAddresses();
    } finally {
        saving.value = false;
    }
}

async function removeAddress(id) {
    await deleteAddressApi(id);
    ElMessage.success('删除成功');
    await loadAddresses();
}
</script>

<style scoped>
.toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}
</style>
