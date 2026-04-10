<template>
    <div class="auth-wrap">
        <div class="auth-stage fade-in-up">
            <aside class="auth-side">
                <h1>Join SoftWhere</h1>
                <p>创建账户，开启购物与二手交易体验</p>
                <p class="hint">注册后即可使用订单、地址、评价、二手发布等完整功能。</p>
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
                    <el-form-item label="手机号" prop="phone">
                        <el-input v-model="form.phone" placeholder="请输入手机号" />
                    </el-form-item>
                    <el-form-item label="邮箱" prop="email">
                        <el-input v-model="form.email" placeholder="请输入邮箱" />
                    </el-form-item>
                    <el-form-item class="action-row">
                        <el-button type="primary" :loading="loading" @click="handleRegister">注册</el-button>
                        <el-button @click="router.push('/login')">去登录</el-button>
                    </el-form-item>
                </el-form>
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
const formRef = ref();
const loading = ref(false);

const form = reactive({
    username: '',
    password: '',
    nickname: '',
    phone: '',
    email: ''
});

const rules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
    nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
    phone: [{ pattern: /^$|^1\d{10}$/, message: '手机号需为11位', trigger: 'blur' }]
};

async function handleRegister() {
    await formRef.value.validate();
    loading.value = true;
    try {
        await userStore.register(form);
        ElMessage.success('注册成功，请登录');
        router.push('/login');
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
    background: radial-gradient(circle at 20% 10%, #d9f6ef 0, transparent 30%),
        radial-gradient(circle at 100% 0, #ffe9cc 0, transparent 26%),
        linear-gradient(170deg, #f7fbff 0%, #eef4fb 100%);
}

.auth-stage {
    width: 100%;
    max-width: 980px;
    display: grid;
    grid-template-columns: 1.1fr 1fr;
    border-radius: 24px;
    overflow: hidden;
    border: 1px solid #dce5f2;
    box-shadow: 0 16px 40px rgba(24, 43, 79, 0.16);
}

.auth-side {
    padding: 44px 36px;
    background: linear-gradient(145deg, #0f7f6b 0%, #0c6556 52%, #144b6e 100%);
    color: #f4fffd;
}

.auth-side h1 {
    margin: 0;
    font-size: 38px;
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
    min-height: 540px;
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
        font-size: 30px;
    }

    .auth-card {
        min-height: unset;
    }
}
</style>
