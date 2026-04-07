<template>
  <section class="page-card auth">
    <h2>登录</h2>
    <el-form label-width="80px">
      <el-form-item label="账号">
        <el-input v-model="form.username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleLogin">登录</el-button>
        <el-button @click="$router.push('/register')">去注册</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup>
import { reactive } from 'vue';
import { ElMessage } from 'element-plus';

const form = reactive({ username: 'user', password: 'user123' });

function handleLogin() {
  ElMessage.success(`登录按钮已复现：${form.username}`);
}
</script>

<style scoped>
.page-card {
  background: #fff;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 16px;
}

.auth {
  max-width: 560px;
  margin: 0 auto;
}
</style>
