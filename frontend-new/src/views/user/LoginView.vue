<template>
  <div style="min-height: 100vh; display: grid; place-items: center; padding: 20px">
    <div class="page-card" style="width: 100%; max-width: 420px">
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
        </el-form-item>
      </el-form>
      <p class="empty-tip">测试账号：admin/admin123 或 user/user123</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();

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
  try {
    loading.value = true;
    await authStore.login(form);
    await authStore.fetchMe();
    ElMessage.success('登录成功');
    router.push('/');
  } finally {
    loading.value = false;
  }
}
</script>
