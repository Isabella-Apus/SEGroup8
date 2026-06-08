<template>
    <div class="page-card">
        <h2 class="page-title">{{ isOfficialSeller ? "卖家工作台入口" : "申请成为卖家" }}</h2>

        <div v-if="isOfficialSeller" class="merchant-entry">
            <el-alert title="您已通过官方卖家认证" type="success" show-icon style="margin-bottom: 16px" />
            <el-button type="primary" size="large" @click="router.push('/merchant')">进入卖家工作台</el-button>
        </div>

        <template v-else>

            <el-alert v-if="myApplication" :title="statusText(myApplication.status)"
                :type="myApplication.status === 1 ? 'success' : myApplication.status === 2 ? 'error' : 'info'"
                :description="myApplication.rejectReason || '已提交申请，管理员审核中。'" show-icon style="margin-bottom: 16px" />

            <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 760px">
                <el-form-item label="店名" prop="storeName">
                    <el-input v-model="form.storeName" />
                </el-form-item>
                <el-form-item label="主营领域" prop="categoryId">
                    <el-select v-model="form.categoryId" placeholder="请选择主营领域" style="width: 100%">
                        <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name"
                            :value="item.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="负责人姓名" prop="contactName">
                    <el-input v-model="form.contactName" />
                </el-form-item>
                <el-form-item label="负责人电话" prop="contactPhone">
                    <el-input v-model="form.contactPhone" />
                </el-form-item>
                <el-form-item label="身份证号" prop="idCardNo">
                    <el-input v-model="form.idCardNo" />
                </el-form-item>
                <el-form-item label="银行卡号" prop="bankCardNo">
                    <el-input v-model="form.bankCardNo" />
                </el-form-item>
                <el-form-item label="营业执照" prop="licenseImg">
                    <div class="license-upload-block">
                        <div class="license-preview-wrap">
                            <el-image v-if="form.licenseImg" :src="toAbsoluteUrl(form.licenseImg)" fit="cover"
                                class="license-preview">
                                <template #error>
                                    <div class="license-error">图片加载失败，请重新上传</div>
                                </template>
                            </el-image>
                            <div v-else class="license-placeholder">请上传营业执照图片</div>
                        </div>
                        <el-upload class="license-upload-btn" :show-file-list="false" :http-request="uploadLicense"
                            accept="image/*">
                            <el-button type="primary" plain :loading="uploading">{{ form.licenseImg ? "重新上传营业执照图片" :
                                "上传营业执照图片" }}</el-button>
                        </el-upload>
                    </div>
                </el-form-item>
                <el-form-item label="仓库省份" prop="warehouseProvince">
                    <el-select v-model="form.warehouseProvince" filterable placeholder="请选择省份" style="width: 100%">
                        <el-option v-for="province in provinceOptions" :key="province" :label="province"
                            :value="province" />
                    </el-select>
                </el-form-item>
                <el-form-item label="仓库城市" prop="warehouseCity">
                    <el-input v-model="form.warehouseCity" />
                </el-form-item>
                <el-form-item label="详细地址" prop="warehouseDetail">
                    <el-input v-model="form.warehouseDetail" type="textarea" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
                </el-form-item>
            </el-form>
        </template>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { getMyMerchantApplicationApi, submitMerchantApplicationApi } from "@/api/merchantApplication";
import { uploadImageApi } from "@/api/upload";
import { useUserStore } from "@/stores/user";
import { getCategoryTree } from "@/api/seller";
import { toAssetUrl } from "@/utils/url";
import { provinceOptions } from "@/utils/provinces";

const router = useRouter();
const userStore = useUserStore();
const formRef = ref();
const submitting = ref(false);
const uploading = ref(false);
const myApplication = ref(null);
const isOfficialSeller = ref(false);

const categoryOptions = ref([]);

const form = reactive({
    storeName: "",
    categoryId: null,
    idCardNo: "",
    bankCardNo: "",
    licenseImg: "",
    warehouseAddr: "",
    warehouseProvince: "",
    warehouseCity: "",
    warehouseDetail: "",
    contactName: "",
    contactPhone: ""
});

const rules = {
    storeName: [{ required: true, message: "请输入店名", trigger: "blur" }],
    categoryId: [{ required: true, message: "请选择主营领域", trigger: "change" }],
    idCardNo: [{ required: true, message: "请输入身份证号", trigger: "blur" }],
    bankCardNo: [{ required: true, message: "请输入银行卡号", trigger: "blur" }],
    licenseImg: [{ required: true, message: "请上传营业执照图片", trigger: "change" }],
    warehouseProvince: [{ required: true, message: "请选择仓库省份", trigger: "change" }],
    warehouseCity: [{ required: true, message: "请输入仓库城市", trigger: "blur" }],
    warehouseDetail: [{ required: true, message: "请输入仓库详细地址", trigger: "blur" }],
    contactName: [{ required: true, message: "请输入负责人姓名", trigger: "blur" }],
    contactPhone: [
        { required: true, message: "请输入负责人电话", trigger: "blur" },
        { pattern: /^1\d{10}$/, message: "手机号需为11位", trigger: "blur" }
    ]
};

onMounted(async () => {
    await loadCategories();
    await loadMyApplication();
});

async function loadCategories() {
    const result = await getCategoryTree("NEW");
    categoryOptions.value = result.data || [];
    if (!form.categoryId && categoryOptions.value.length) {
        form.categoryId = categoryOptions.value[0].id;
    }
}

async function loadMyApplication() {
    try {
        isOfficialSeller.value = userStore.currentRole === "OFFICIAL_SELLER";
        if (isOfficialSeller.value) {
            return;
        }
        const result = await getMyMerchantApplicationApi();
        myApplication.value = result.data;
        if (result.data) {
            Object.assign(form, {
                storeName: result.data.storeName || "",
                categoryId: result.data.categoryId || categoryOptions.value[0]?.id || null,
                licenseImg: result.data.licenseImg || "",
                warehouseAddr: result.data.warehouseAddr || "",
                warehouseProvince: result.data.warehouseProvince || "",
                warehouseCity: result.data.warehouseCity || "",
                warehouseDetail: result.data.warehouseDetail || "",
                contactName: result.data.contactName || "",
                contactPhone: result.data.contactPhone || ""
            });
            if (!form.warehouseDetail && form.warehouseAddr) {
                form.warehouseDetail = form.warehouseAddr;
            }
        }
    } catch (error) {
        const message = error?.message || "";
        if (message.includes("No static resource")) {
            myApplication.value = null;
            return;
        }
        throw error;
    }
}

function statusText(status) {
    if (status === 1) return "审核通过";
    if (status === 2) return "审核驳回";
    return "待审核";
}

async function submit() {
    await formRef.value.validate();
    submitting.value = true;
    try {
        const payload = {
            ...form,
            warehouseAddr: `${form.warehouseProvince} ${form.warehouseCity} ${form.warehouseDetail}`.trim()
        };
        await submitMerchantApplicationApi(payload);
        ElMessage.success("申请已提交");
        await loadMyApplication();
    } finally {
        submitting.value = false;
    }
}

async function uploadLicense(option) {
    uploading.value = true;
    try {
        const result = await uploadImageApi(option.file);
        form.licenseImg = result.data.url;
        option.onSuccess(result);
        ElMessage.success("营业执照上传成功");
    } catch (error) {
        option.onError(error);
    } finally {
        uploading.value = false;
    }
}

function toAbsoluteUrl(url) {
    if (!url) {
        return "";
    }
    const normalizedUrl = String(url).replace(/\\\\/g, "/");
    let withPrefix = normalizedUrl;
    if (!withPrefix.startsWith("/")) {
        withPrefix = withPrefix.startsWith("uploads/") ? `/${withPrefix}` : `/uploads/${withPrefix}`;
    }
    return encodeURI(toAssetUrl(withPrefix));
}
</script>

<style scoped>
.merchant-entry {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
}

.license-upload-block {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
}

.license-preview-wrap {
    width: 260px;
    height: 160px;
    margin-bottom: 12px;
}

.license-preview {
    width: 100%;
    height: 100%;
    border-radius: 8px;
    border: 1px solid #dcdfe6;
}

.license-error {
    width: 100%;
    height: 100%;
    border-radius: 8px;
    border: 1px solid #f56c6c;
    color: #f56c6c;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fef0f0;
}

.license-placeholder {
    width: 100%;
    height: 100%;
    border-radius: 8px;
    border: 1px dashed #c0c4cc;
    color: #909399;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fafafa;
}
</style>
