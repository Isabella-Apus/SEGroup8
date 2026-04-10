<template>
    <div class="auth-wrap">
        <div class="auth-stage fade-in-up">
            <aside class="auth-side">
                <h1>SoftWhere</h1>
                <p>购物与二手交易平台</p>
                <p class="hint">统一商品流、订单流与售后流程的课程实训系统</p>
            </aside>

            <div class="auth-card page-card">
                <h2 class="page-title">账号登录</h2>
                <el-form :model="form" label-position="top" @submit.prevent>
                    <el-form-item label="账号">
                        <el-input v-model="form.username" placeholder="请输入账号" />
                    </el-form-item>
                    <el-form-item label="密码">
                        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
                    </el-form-item>
                    <el-form-item class="action-row">
                        <el-button type="primary" :loading="loading" @click="handleLogin">登录</el-button>
                        <el-button @click="router.push('/register')">去注册</el-button>
                    </el-form-item>
                </el-form>
                <p class="empty-tip">测试账号：admin/admin123 或 user/user123</p>
            </div>
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
    padding: 24px;
    display: grid;
    place-items: center;
    background: linear-gradient(170deg, #f7fbff 0%, #eef4fb 100%);
}

.auth-stage {
    width: 100%;
    max-width: 980px;
    display: grid;
    grid-template-columns: 1.1fr 1fr;
    border-radius: 24px;
    overflow: hidden;
    border: 1px solid #dce5f2;
    box-shadow: 0 10px 24px rgba(24, 43, 79, 0.12);
}

.auth-side {
    padding: 44px 36px;
    background: linear-gradient(145deg, #0f7f6b 0%, #0c6556 52%, #144b6e 100%);
    color: #f4fffd;
}

.auth-side h1 {
    margin: 0;
    font-size: 42px;
    letter-spacing: 0.8px;
}

.auth-side p {
    margin: 14px 0 0;
    font-size: 18px;
}

.auth-side .hint {
    margin-top: 18px;
    font-size: 14px;
    line-height: 1.75;
    opacity: 0.9;
}

.auth-card {
    border-radius: 0;
    border: 0;
    box-shadow: none;
    min-height: 460px;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.action-row :deep(.el-form-item__content) {
    display: flex;
    gap: 10px;
}

@media (max-width: 900px) {
    .auth-stage {
        grid-template-columns: 1fr;
    }

    .auth-side {
        padding: 24px;
    }

    .auth-side h1 {
        font-size: 32px;
    }

    .auth-card {
        min-height: unset;
    }
}
</style>
