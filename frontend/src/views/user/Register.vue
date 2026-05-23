<template>
    <div class="auth-wrap">
        <div class="auth-stage fade-in-up">
            <aside class="auth-side">
                <div class="brand-plate">
                    <img :src="logoUrl" alt="Kinda Goods" />
                </div>
                <h1>加入 Kinda Goods</h1>
                <p>注册后即可使用购物车、订单、地址、评价、二手发布和信用中心。</p>
            </aside>

            <div class="auth-card page-card">
                <h2 class="page-title">用户注册</h2>
                <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
                    <el-form-item label="用户名" prop="username">
                        <el-input v-model="form.username" placeholder="请输入用户名" />
                    </el-form-item>
                    <el-form-item label="密码" prop="password">
                        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
                    </el-form-item>
                    <el-form-item label="昵称" prop="nickname">
                        <el-input v-model="form.nickname" placeholder="请输入昵称" />
                    </el-form-item>
                    <div class="form-grid">
                        <el-form-item label="手机号" prop="phone">
                            <el-input v-model="form.phone" placeholder="请输入手机号" />
                        </el-form-item>
                        <el-form-item label="邮箱" prop="email">
                            <el-input v-model="form.email" placeholder="请输入邮箱" />
                        </el-form-item>
                    </div>
                    <el-form-item class="action-row">
                        <el-button type="primary" :loading="loading" @click="handleRegister">注册</el-button>
                        <el-button @click="router.push('/login')">返回登录</el-button>
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
const formRef = ref();
const loading = ref(false);

const form = reactive({
    username: "",
    password: "",
    nickname: "",
    phone: "",
    email: "",
});

const rules = {
    username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
    password: [{ required: true, message: "请输入密码", trigger: "blur" }, { min: 6, message: "密码至少6位", trigger: "blur" }],
    nickname: [{ required: true, message: "请输入昵称", trigger: "blur" }],
    phone: [{ pattern: /^$|^1\d{10}$/, message: "手机号需为11位", trigger: "blur" }],
};

async function handleRegister() {
    await formRef.value.validate();
    loading.value = true;
    try {
        await userStore.register(form);
        ElMessage.success("注册成功，请登录");
        router.push("/login");
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
        linear-gradient(120deg, rgba(233, 255, 248, 0.9), rgba(234, 244, 255, 0.86), rgba(255, 247, 251, 0.82)),
        url("https://images.unsplash.com/photo-1556742044-3c52d6e88c62?auto=format&fit=crop&w=1600&q=80");
    background-size: cover;
    background-position: center;
}

.auth-stage {
    width: 100%;
    max-width: 1040px;
    display: grid;
    grid-template-columns: 0.95fr 1.05fr;
    border: 1px solid rgba(255, 255, 255, 0.34);
    border-radius: 8px;
    overflow: hidden;
    box-shadow: var(--shadow-float);
    background: #ffffff;
}

.auth-side {
    padding: 44px 38px;
    background:
        linear-gradient(145deg, rgba(233, 255, 248, 0.94), rgba(234, 244, 255, 0.82), rgba(255, 247, 251, 0.78)),
        url("https://images.unsplash.com/photo-1607083206968-13611e3d76db?auto=format&fit=crop&w=1100&q=80");
    background-size: cover;
    background-position: center;
    color: var(--text-main);
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.brand-plate {
    width: min(100%, 340px);
    border-radius: 12px;
    background: #ffffff;
    padding: 14px 16px;
    box-shadow: 0 18px 42px rgba(0, 0, 0, 0.18);
}

.brand-plate img {
    width: 100%;
    height: auto;
    display: block;
}

.auth-side h1 {
    margin: 20px 0 12px;
    font-size: 46px;
    letter-spacing: 0;
}

.auth-side p {
    margin: 0;
    max-width: 380px;
    color: var(--text-secondary);
    font-size: 17px;
    line-height: 1.8;
    font-weight: 700;
}

.auth-card {
    border-radius: 0;
    border: 0;
    box-shadow: none;
    min-height: 560px;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.action-row :deep(.el-form-item__content) {
    display: flex;
    gap: 10px;
}

@media (max-width: 900px) {
    .auth-stage,
    .form-grid {
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
