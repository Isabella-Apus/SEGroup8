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
            <el-form-item label="信用分">
                <el-input :model-value="String(form.creditScore || '-')" disabled />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" :loading="loading" @click="saveProfile">保存</el-button>
            </el-form-item>
        </el-form>
    </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getProfileApi, updateProfileApi } from '@/api/user';
import { uploadImageApi } from '@/api/upload';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const formRef = ref();
const loading = ref(false);
const avatarUploading = ref(false);
const form = reactive({
    username: '',
    nickname: '',
    avatar: '',
    phone: '',
    email: '',
    role: '',
    creditScore: 100
});

const rules = {
    nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
    phone: [{ pattern: /^$|^1\d{10}$/, message: '手机号需为11位', trigger: 'blur' }]
};

onMounted(loadProfile);

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
</script>

<style scoped>
.profile-head {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
}
</style>
