<template>
    <div class="auth-wrap">
        <div class="auth-stage fade-in-up">
            <aside class="auth-side">
                <span class="brand-mark">kg</span>
                <h1>Kinda Goods</h1>
                <p>一手商品、二手闲置、订单售后和卖家工作台都在这里。</p>
                <div class="account-grid">
                    <button type="button" @click="fillAccount('user', 'user123')">普通用户</button>
                    <button type="button" @click="fillAccount('seller', 'seller123')">卖家</button>
                    <button type="button" @click="fillAccount('admin', 'admin123')">管理员</button>
                </div>
            </aside>

            <div class="auth-card page-card">
                <h2 class="page-title">账号登录</h2>
                <p class="auth-subtitle">欢迎回来，继续发现好物。</p>
                <el-form :model="form" label-position="top" @submit.prevent>
                    <el-form-item label="账号">
                        <el-input v-model="form.username" placeholder="请输入账号" />
                    </el-form-item>
                    <el-form-item label="密码">
                        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
                    </el-form-item>
                    <el-form-item class="action-row">
                        <el-button type="primary" :loading="loading" @click="handleLogin">登录</el-button>
                        <el-button @click="router.push('/register')">注册新账号</el-button>
                    </el-form-item>
                </el-form>
                <p class="empty-tip">测试账号：admin/admin123、seller/seller123、user/user123</p>
            </div>
        </div>
    </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const form = reactive({
    username: "user",
    password: "user123",
});

function fillAccount(username, password) {
    form.username = username;
    form.password = password;
}

async function handleLogin() {
    if (!form.username || !form.password) {
        ElMessage.warning("请填写完整账号密码");
        return;
    }
    loading.value = true;
    try {
        const userInfo = await userStore.login(form);
        ElMessage.success("登录成功");
        if (userInfo.role === "ADMIN") {
            router.push("/admin");
            return;
        }
        if (userInfo.role === "OFFICIAL_SELLER" || userInfo.role === "SELLER") {
            router.push("/merchant");
            return;
        }
        router.push("/");
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
    background:
        linear-gradient(120deg, rgba(220, 239, 233, 0.96), rgba(241, 240, 251, 0.9)),
        url("https://images.unsplash.com/photo-1556742502-ec7c0e9f34b1?auto=format&fit=crop&w=1600&q=80");
    background-size: cover;
    background-position: center;
}

.auth-stage {
    width: 100%;
    max-width: 980px;
    display: grid;
    grid-template-columns: 1.05fr 0.95fr;
    border: 2px solid var(--brand-primary);
    border-radius: 28px;
    overflow: hidden;
    box-shadow: var(--shadow-float);
    background: #ffffff;
}

.auth-side {
    padding: 44px 38px;
    background: var(--brand-primary);
    color: #ffffff;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.brand-mark {
    width: 58px;
    height: 58px;
    border-radius: 18px;
    background: var(--brand-accent);
    color: var(--brand-primary);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    font-weight: 900;
}

.auth-side h1 {
    margin: 20px 0 12px;
    font-size: 48px;
    letter-spacing: 0;
}

.auth-side p {
    margin: 0;
    max-width: 420px;
    color: #dcefe9;
    font-size: 17px;
    line-height: 1.8;
    font-weight: 700;
}

.account-grid {
    margin-top: 28px;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
}

.account-grid button {
    min-height: 44px;
    border: 1px solid rgba(255, 255, 255, 0.16);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.08);
    color: #ffffff;
    font-weight: 800;
    cursor: pointer;
}

.account-grid button:hover {
    background: rgba(255, 255, 255, 0.16);
}

.auth-card {
    border-radius: 0;
    border: 0;
    box-shadow: none;
    min-height: 500px;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.auth-subtitle {
    margin: -6px 0 20px;
    color: var(--text-secondary);
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
        padding: 28px;
    }

    .auth-side h1 {
        font-size: 34px;
    }

    .auth-card {
        min-height: unset;
    }
}
</style>
