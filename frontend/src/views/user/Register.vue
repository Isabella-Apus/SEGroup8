<template>
    <div class="auth-wrap">
        <div class="auth-card page-card">
            <h2 class="page-title">用户注册</h2>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
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
                <el-form-item>
                    <el-button type="primary" :loading="loading" @click="handleRegister">注册</el-button>
                    <el-button @click="router.push('/login')">去登录</el-button>
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
    display: grid;
    place-items: center;
    padding: 20px;
}

.auth-card {
    width: 100%;
    max-width: 500px;
}
</style>
