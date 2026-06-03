<template>
    <div class="merchant-apply-page">
        <section class="merchant-hero">
            <div>
                <span>Seller Onboarding</span>
                <h1>{{ isOfficialSeller ? "卖家工作台入口" : "申请成为卖家" }}</h1>
                <p>填写店铺、资质与仓库信息，提交后由管理员审核。</p>
            </div>
            <div class="hero-steps" aria-hidden="true">
                <span>店铺信息</span>
                <span>资质图片</span>
                <span>仓库地址</span>
            </div>
        </section>

        <section class="application-card">

        <div v-if="isOfficialSeller" class="merchant-entry">
            <el-alert class="status-alert" title="您已通过官方卖家认证" type="success" show-icon />
            <el-button type="primary" size="large" @click="router.push('/merchant')">进入卖家工作台</el-button>
        </div>

        <template v-else>

            <el-alert v-if="myApplication" class="status-alert" :title="statusText(myApplication.status)"
                :type="myApplication.status === 1 ? 'success' : myApplication.status === 2 ? 'error' : 'info'"
                :description="myApplication.rejectReason || '已提交申请，管理员审核中。'" show-icon />

            <el-form ref="formRef" class="application-form" :model="form" :rules="rules" label-position="top">
                <el-form-item label="店名" prop="storeName">
                    <el-input v-model="form.storeName" />
                </el-form-item>
                <el-form-item label="主营领域" prop="categoryId">
                    <el-select v-model="form.categoryId" placeholder="请选择主营领域" style="width: 100%">
                        <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label"
                            :value="item.value" />
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
        </section>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { getMyMerchantApplicationApi, submitMerchantApplicationApi } from "@/api/merchantApplication";
import { uploadImageApi } from "@/api/upload";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const userStore = useUserStore();
const formRef = ref();
const submitting = ref(false);
const uploading = ref(false);
const myApplication = ref(null);
const isOfficialSeller = ref(false);

const categoryOptions = [
    { label: "食品", value: 1 },
    { label: "3C", value: 2 },
    { label: "美妆", value: 3 },
    { label: "服装", value: 4 },
    { label: "运动", value: 5 }
];

const provinceOptions = [
    "北京市", "天津市", "上海市", "重庆市",
    "河北省", "山西省", "辽宁省", "吉林省", "黑龙江省",
    "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省",
    "河南省", "湖北省", "湖南省", "广东省", "海南省",
    "四川省", "贵州省", "云南省", "陕西省", "甘肃省", "青海省",
    "台湾省",
    "内蒙古自治区", "广西壮族自治区", "西藏自治区", "宁夏回族自治区", "新疆维吾尔自治区",
    "香港特别行政区", "澳门特别行政区"
];

const form = reactive({
    storeName: "",
    categoryId: 1,
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

onMounted(loadMyApplication);

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
                categoryId: result.data.categoryId || 1,
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
    if (/^https?:\/\//i.test(normalizedUrl)) {
        return encodeURI(normalizedUrl);
    }
    let withPrefix = normalizedUrl;
    if (!withPrefix.startsWith("/")) {
        withPrefix = withPrefix.startsWith("uploads/") ? `/${withPrefix}` : `/uploads/${withPrefix}`;
    }
    return encodeURI(`http://localhost:8080${withPrefix}`);
}
</script>

<style scoped>
.merchant-apply-page {
    display: grid;
    gap: 16px;
}

.merchant-hero {
    position: relative;
    overflow: hidden;
    min-height: 172px;
    border: 1px solid rgba(137, 199, 255, 0.36);
    border-radius: 8px;
    padding: 24px;
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 18px;
    background:
        linear-gradient(115deg, rgba(234, 244, 255, 0.94), rgba(233, 255, 248, 0.82), rgba(255, 247, 251, 0.72)),
        url("https://images.unsplash.com/photo-1556742044-3c52d6e88c62?auto=format&fit=crop&w=1400&q=80");
    background-size: cover;
    background-position: center;
    box-shadow: var(--shadow-soft);
}

.merchant-hero::before {
    position: absolute;
    inset: 0;
    content: "";
    background-image:
        linear-gradient(rgba(60, 146, 255, 0.08) 1px, transparent 1px),
        linear-gradient(90deg, rgba(53, 216, 171, 0.08) 1px, transparent 1px);
    background-size: 42px 42px;
    mask-image: linear-gradient(110deg, black 0%, black 72%, transparent 100%);
}

.merchant-hero::after {
    position: absolute;
    top: -36%;
    bottom: -36%;
    left: -28%;
    width: 26%;
    content: "";
    background: linear-gradient(110deg, transparent 0%, rgba(255, 255, 255, 0.66) 48%, transparent 100%);
    transform: skewX(-13deg);
    animation: heroSweep 7s ease-in-out infinite;
}

.merchant-hero > * {
    position: relative;
    z-index: 1;
}

.merchant-hero span {
    color: var(--brand-primary);
    font-weight: 900;
}

.merchant-hero h1 {
    margin: 10px 0 8px;
    color: var(--text-main);
    font-size: clamp(30px, 4vw, 44px);
    line-height: 1.08;
    letter-spacing: 0;
}

.merchant-hero p {
    max-width: 520px;
    margin: 0;
    color: var(--text-secondary);
    font-size: 16px;
    line-height: 1.7;
    font-weight: 800;
}

.hero-steps {
    min-width: 300px;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
}

.hero-steps span {
    min-height: 42px;
    border: 1px solid rgba(137, 199, 255, 0.34);
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.76);
    color: var(--text-main);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0 10px;
    box-shadow: 0 12px 24px rgba(137, 199, 255, 0.12);
    backdrop-filter: blur(10px);
}

.application-card {
    position: relative;
    overflow: hidden;
    border: 1px solid rgba(137, 199, 255, 0.28);
    border-radius: 8px;
    background:
        linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 251, 255, 0.94)),
        #ffffff;
    padding: 22px;
    box-shadow: var(--shadow-soft);
}

.application-card::before {
    position: absolute;
    inset: 0;
    content: "";
    background:
        linear-gradient(118deg, transparent 0%, transparent 28%, rgba(95, 230, 189, 0.1) 42%, transparent 62%),
        linear-gradient(250deg, transparent 8%, rgba(255, 185, 214, 0.12) 34%, transparent 58%);
    pointer-events: none;
}

.application-card > * {
    position: relative;
    z-index: 1;
}

.status-alert {
    margin-bottom: 16px;
    border-radius: 8px;
}

.merchant-entry {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
}

.application-form {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 2px 16px;
}

.application-form :deep(.el-form-item) {
    margin-bottom: 14px;
}

.application-form :deep(.el-form-item__label) {
    color: var(--text-main);
    font-weight: 900;
}

.application-form :deep(.el-input__wrapper),
.application-form :deep(.el-select__wrapper),
.application-form :deep(.el-textarea__inner) {
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 0 0 1px var(--line-soft) inset;
    transition: box-shadow 0.18s ease, transform 0.18s ease;
}

.application-form :deep(.el-input__wrapper),
.application-form :deep(.el-select__wrapper) {
    min-height: 44px;
}

.application-form :deep(.el-input__wrapper:hover),
.application-form :deep(.el-select__wrapper:hover),
.application-form :deep(.el-textarea__inner:hover) {
    box-shadow: 0 0 0 1px rgba(60, 146, 255, 0.38) inset;
}

.application-form :deep(.el-input__wrapper.is-focus),
.application-form :deep(.el-select__wrapper.is-focused),
.application-form :deep(.el-textarea__inner:focus) {
    transform: translateY(-1px);
    box-shadow:
        0 0 0 1px var(--brand-primary) inset,
        0 12px 24px rgba(137, 199, 255, 0.18);
}

.application-form > :deep(.el-form-item:nth-child(7)),
.application-form > :deep(.el-form-item:nth-child(10)),
.application-form > :deep(.el-form-item:nth-child(11)) {
    grid-column: 1 / -1;
}

.license-upload-block {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
}

.license-preview-wrap {
    width: min(360px, 100%);
    height: 190px;
    margin-bottom: 12px;
}

.license-preview {
    width: 100%;
    height: 100%;
    border-radius: 8px;
    border: 1px solid var(--line-soft);
    box-shadow: 0 12px 24px rgba(137, 199, 255, 0.12);
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
    border: 1px dashed rgba(60, 146, 255, 0.42);
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #eaf4ff 0%, #e9fff8 100%);
    font-weight: 800;
}

@keyframes heroSweep {
    0%,
    34% {
        opacity: 0;
        transform: translateX(0) skewX(-13deg);
    }
    54% {
        opacity: 1;
    }
    100% {
        opacity: 0;
        transform: translateX(520%) skewX(-13deg);
    }
}

@media (max-width: 980px) {
    .merchant-hero {
        align-items: flex-start;
        flex-direction: column;
    }

    .hero-steps,
    .application-form {
        width: 100%;
        grid-template-columns: 1fr;
    }

    .application-form > :deep(.el-form-item:nth-child(7)),
    .application-form > :deep(.el-form-item:nth-child(10)),
    .application-form > :deep(.el-form-item:nth-child(11)) {
        grid-column: auto;
    }
}

@media (max-width: 640px) {
    .merchant-hero,
    .application-card {
        padding: 18px;
    }

    .hero-steps {
        grid-template-columns: 1fr;
    }
}
</style>
