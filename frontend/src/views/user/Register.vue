<template>
    <div class="auth-minimal">
        <div class="ambient ambient-one"></div>
        <div class="ambient ambient-two"></div>
        <div class="ambient ambient-three"></div>
        <div class="inject-layer" aria-hidden="true">
            <span style="--x: 8%; --y: 12%; --d: 0s;">const auth = inject('profile')</span>
            <span style="--x: 70%; --y: 18%; --d: -2.4s;">mount(RegisterForm)</span>
            <span style="--x: 14%; --y: 72%; --d: -4.8s;">rules.validate()</span>
            <span style="--x: 76%; --y: 68%; --d: -7.2s;">create.account()</span>
        </div>
        <div class="scan-beam" aria-hidden="true"></div>
        <main class="minimal-card register-card">
            <div class="trace-ring" aria-hidden="true"></div>
            <button class="minimal-brand" type="button" @click="router.push('/')">
                <span>闲</span>
                <strong>校园闲置</strong>
            </button>

            <section class="minimal-form">
                <p>开始使用</p>
                <h1>创建账号</h1>

                <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
                    <el-form-item label="用户名" prop="username">
                        <el-input v-model="form.username" placeholder="username" size="large" />
                    </el-form-item>
                    <el-form-item label="密码" prop="password">
                        <el-input v-model="form.password" type="password" show-password placeholder="password" size="large" />
                    </el-form-item>
                    <el-form-item label="昵称" prop="nickname">
                        <el-input v-model="form.nickname" placeholder="nickname" size="large" />
                    </el-form-item>
                    <div class="compact-grid">
                        <el-form-item label="手机号" prop="phone">
                            <el-input v-model="form.phone" placeholder="optional" size="large" />
                        </el-form-item>
                        <el-form-item label="邮箱" prop="email">
                            <el-input v-model="form.email" placeholder="optional" size="large" />
                        </el-form-item>
                    </div>
                    <el-button class="minimal-submit" type="primary" :loading="loading" @click="handleRegister">
                        创建
                    </el-button>
                </el-form>

                <div class="minimal-switch">
                    <span>已有账号？</span>
                    <button type="button" @click="router.push('/login')">登录</button>
                </div>
            </section>
        </main>
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
.auth-minimal {
    min-height: 100vh;
    position: relative;
    display: grid;
    place-items: center;
    overflow: hidden;
    padding: 28px;
    background:
        radial-gradient(circle at 14% 16%, rgba(255, 225, 0, 0.22), transparent 28%),
        radial-gradient(circle at 88% 18%, rgba(61, 214, 185, 0.2), transparent 28%),
        radial-gradient(circle at 76% 86%, rgba(255, 113, 91, 0.16), transparent 32%),
        linear-gradient(135deg, #fbfaf4 0%, #eef5f2 48%, #f6f1ec 100%);
}

.auth-minimal::before {
    position: absolute;
    inset: 0;
    content: "";
    background-image:
        linear-gradient(rgba(21, 29, 43, 0.045) 1px, transparent 1px),
        linear-gradient(90deg, rgba(21, 29, 43, 0.045) 1px, transparent 1px);
    background-size: 54px 54px;
    mask-image: radial-gradient(circle at center, black, transparent 74%);
}

.ambient {
    position: absolute;
    width: 420px;
    height: 420px;
    border-radius: 50%;
    filter: blur(22px);
    opacity: 0.5;
    animation: drift 12s ease-in-out infinite alternate;
}

.ambient-one {
    left: 14%;
    top: 14%;
    background: linear-gradient(135deg, rgba(255, 225, 0, 0.58), rgba(255, 166, 0, 0.24));
}

.ambient-two {
    right: 12%;
    bottom: 10%;
    background: linear-gradient(135deg, rgba(39, 201, 178, 0.38), rgba(44, 65, 94, 0.14));
    animation-delay: -4s;
}

.ambient-three {
    width: 300px;
    height: 300px;
    right: 24%;
    top: 18%;
    background: linear-gradient(135deg, rgba(255, 116, 91, 0.24), rgba(143, 107, 255, 0.16));
    animation-delay: -8s;
}

.minimal-card {
    position: relative;
    z-index: 1;
    width: min(460px, 100%);
    min-height: 640px;
    border: 1px solid rgba(255, 255, 255, 0.82);
    border-radius: 32px;
    padding: 30px;
    display: flex;
    flex-direction: column;
    background:
        linear-gradient(180deg, rgba(255, 255, 255, 0.84), rgba(255, 255, 255, 0.68)),
        radial-gradient(circle at 50% 0%, rgba(255, 225, 0, 0.16), transparent 40%);
    backdrop-filter: blur(28px) saturate(1.15);
    box-shadow:
        0 34px 90px rgba(27, 34, 44, 0.16),
        0 1px 0 rgba(255, 255, 255, 0.9) inset;
    animation: cardIn 0.42s ease both;
}

.minimal-brand {
    align-self: center;
    border: 0;
    padding: 0;
    display: inline-flex;
    align-items: center;
    gap: 10px;
    background: transparent;
    color: #161a22;
    cursor: pointer;
}

.minimal-brand span {
    width: 42px;
    height: 42px;
    display: grid;
    place-items: center;
    border: 2px solid rgba(22, 26, 34, 0.86);
    background: linear-gradient(135deg, #ffe100, #ff9d5c);
    border-radius: 14px;
    font-weight: 900;
    font-size: 24px;
}

.minimal-brand strong {
    font-size: 18px;
    font-weight: 900;
}

.minimal-form {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.minimal-form p {
    margin: 0 0 6px;
    color: #697380;
    text-align: center;
    font-weight: 700;
}

.minimal-form h1 {
    margin: 0 0 28px;
    color: #151923;
    text-align: center;
    font-size: 34px;
    letter-spacing: 0;
}

.minimal-form :deep(.el-form-item) {
    margin-bottom: 16px;
}

.minimal-form :deep(.el-form-item__label) {
    color: #4a5361;
    font-weight: 800;
}

.minimal-form :deep(.el-input__wrapper) {
    min-height: 48px;
    border-radius: 16px;
    background: rgba(255, 255, 255, 0.82);
    box-shadow: 0 0 0 1px rgba(28, 39, 55, 0.09) inset;
}

.minimal-form :deep(.el-input__wrapper.is-focus) {
    box-shadow:
        0 0 0 2px rgba(255, 225, 0, 0.86) inset,
        0 0 0 5px rgba(61, 214, 185, 0.14);
}

.compact-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
}

.minimal-submit {
    width: 100%;
    height: 48px;
    margin-top: 4px;
    border-radius: 999px;
    font-size: 16px;
    font-weight: 900;
    border: 0;
    color: #171b22;
    background: linear-gradient(135deg, #ffe45c 0%, #ffc83d 58%, #72dfca 100%);
    box-shadow: 0 14px 28px rgba(255, 189, 35, 0.22);
}

.minimal-switch {
    margin-top: 22px;
    display: flex;
    justify-content: center;
    gap: 8px;
    color: #68727f;
}

.minimal-switch button {
    border: 0;
    padding: 0;
    background: transparent;
    color: #151923;
    cursor: pointer;
    font-weight: 900;
}

@keyframes drift {
    from {
        transform: translate3d(-24px, -18px, 0) scale(0.96);
    }
    to {
        transform: translate3d(24px, 18px, 0) scale(1.05);
    }
}

@keyframes cardIn {
    from {
        opacity: 0;
        transform: translateY(14px) scale(0.98);
    }
    to {
        opacity: 1;
        transform: translateY(0) scale(1);
    }
}

@media (max-width: 560px) {
    .compact-grid {
        grid-template-columns: 1fr;
        gap: 0;
    }
}
</style>
