<template>
    <div class="auth-wrap">
        <div class="auth-card page-card">
            <h2 class="page-title">账号登录</h2>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
                <el-form-item label="账号" prop="username">
                    <el-input v-model="form.username" placeholder="请输入账号" />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" :loading="loading" @click="handleLogin">登录</el-button>
                    <el-button @click="router.push('/register')">注册</el-button>
                </el-form-item>
            </el-form>
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
const formRef = ref();
const loading = ref(false);
const form = reactive({
    username: 'user',
    password: 'user123'
});

const rules = {
    username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

async function handleLogin() {
    await formRef.value.validate();
    loading.value = true;
    try {
        const userInfo = await userStore.login(form);
        await userStore.fetchProfile();
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
