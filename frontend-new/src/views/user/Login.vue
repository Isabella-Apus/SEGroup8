<template>
    <div class="auth-wrap">
        <div class="auth-card page-card">
            <h2 class="page-title">账号登录</h2>
            <el-form :model="form" label-width="80px" @submit.prevent>
                <el-form-item label="账号">
                    <el-input v-model="form.username" placeholder="请输入账号" />
                </el-form-item>
                <el-form-item label="密码">
                    <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" :loading="loading" @click="handleLogin">登录</el-button>
                    <el-button @click="router.push('/register')">去注册</el-button>
                </el-form-item>
            </el-form>
            <p class="empty-tip">测试账号：admin/admin123 或 user/user123</p>
    </div>
    </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const form = reactive({
    username: 'user',
    password: 'user123'
});

async function handleLogin() {
    if (!form.username || !form.password) {
        ElMessage.warning('请填写完整账号密码');
        return;
    }
    loading.value = true;
    try {
        const userInfo = await userStore.login(form);
        ElMessage.success('登录成功');
        if (userInfo.role === 'ADMIN') {
            router.push('/admin');
            return;
        }
        if (userInfo.role === 'OFFICIAL_SELLER' || userInfo.role === 'SELLER') {
            router.push('/merchant');
            return;
        }
        router.push('/');
    } finally {
        loading.value = false;
    }
}
</script>

<style scoped>
.auth-wrap {
    min-height: 100vh;
    display: grid;
    place-items: center;
    padding: 20px;
}

.auth-card {
    width: 100%;
    max-width: 420px;
}
</style>
