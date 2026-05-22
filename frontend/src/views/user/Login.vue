<template>
    <div class="auth-wrap">
        <div class="auth-stage fade-in-up">
            <aside class="auth-side">
                <div class="brand-plate">
                    <img :src="logoUrl" alt="Kinda Goods" />
                </div>
                <h1>欢迎回来</h1>
                <p>继续逛好物、淘闲置、查订单。</p>
            </aside>

            <div class="auth-card page-card">
                <span class="login-kicker">Kinda Goods</span>
                <h2 class="page-title">账号登录</h2>
                <p class="auth-subtitle">好物还在，购物车也在。</p>
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
            </div>
        </div>
    </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import logoUrl from "@/assets/kinda-goods-logo.svg";

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const form = reactive({
    username: "",
    password: "",
});

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
        radial-gradient(circle at 18% 18%, rgba(95, 230, 189, 0.24), transparent 26%),
        radial-gradient(circle at 82% 18%, rgba(137, 199, 255, 0.25), transparent 26%),
        radial-gradient(circle at 82% 84%, rgba(255, 185, 214, 0.22), transparent 28%),
        linear-gradient(135deg, #f8fffc 0%, #eff9ff 45%, #fff7fb 100%);
}

.auth-stage {
    width: 100%;
    max-width: 920px;
    display: grid;
    grid-template-columns: minmax(0, 0.9fr) minmax(360px, 1fr);
    border: 1px solid rgba(255, 255, 255, 0.8);
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 22px 60px rgba(57, 118, 166, 0.14);
    background: rgba(255, 255, 255, 0.86);
    backdrop-filter: blur(14px);
}

.auth-side {
    padding: 44px 38px;
    background: linear-gradient(145deg, rgba(233, 255, 248, 0.82), rgba(234, 244, 255, 0.74));
    color: var(--text-main);
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: flex-start;
}

.brand-plate {
    width: min(100%, 310px);
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.9);
    padding: 14px 16px;
    box-shadow: 0 16px 34px rgba(60, 146, 255, 0.12);
    border: 1px solid rgba(255, 255, 255, 0.95);
}

.brand-plate img {
    width: 100%;
    height: auto;
    display: block;
}

.auth-side h1 {
    margin: 24px 0 12px;
    font-size: 42px;
    line-height: 1.12;
    letter-spacing: 0;
}

.auth-side p {
    margin: 0;
    max-width: 320px;
    color: var(--text-secondary);
    font-size: 16px;
    line-height: 1.7;
    font-weight: 700;
}

.auth-card {
    border-radius: 0;
    border: 0;
    box-shadow: none;
    min-height: 520px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 44px;
    background: rgba(255, 255, 255, 0.96);
}

.auth-card :deep(.el-form-item__label) {
    color: var(--text-main);
    font-weight: 800;
}

.auth-card :deep(.el-input__wrapper) {
    min-height: 46px;
    border-radius: 10px;
    box-shadow: 0 0 0 1px rgba(137, 199, 255, 0.3) inset;
}

.auth-card :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px var(--brand-primary) inset, 0 0 0 4px rgba(137, 199, 255, 0.18);
}

.auth-card :deep(.el-button) {
    min-height: 42px;
    border-radius: 10px;
    padding-inline: 22px;
}

.login-kicker {
    width: fit-content;
    border-radius: 999px;
    background: var(--brand-primary-weak);
    color: #1d7d8f;
    padding: 5px 11px;
    font-size: 12px;
    font-weight: 900;
    border: 1px solid rgba(137, 199, 255, 0.28);
}

.auth-card .page-title {
    margin-top: 14px;
    margin-bottom: 10px;
    font-size: 30px;
}

.auth-subtitle {
    margin: 0 0 24px;
    color: var(--text-secondary);
    line-height: 1.7;
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
        align-items: center;
        text-align: center;
    }

    .auth-card {
        min-height: unset;
        padding: 28px;
    }
}

@media (max-width: 620px) {
    .auth-wrap {
        padding: 14px;
        overflow: auto;
    }

    .auth-side,
    .auth-card {
        padding: 22px;
    }

    .brand-plate {
        width: 220px;
    }

    .action-row :deep(.el-form-item__content) {
        flex-direction: column;
    }

    .auth-card :deep(.el-button) {
        width: 100%;
    }
}
</style>
