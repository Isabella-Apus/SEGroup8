<template>
  <section class="page-card">
    <h2>个人资料</h2>
    <el-form label-width="90px">
      <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item>
        <el-button @click="uploadAvatar">上传头像</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup>
import { reactive } from 'vue';
import { ElMessage } from 'element-plus';

const form = reactive({ nickname: 'DemoUser', phone: '13800138000' });

function uploadAvatar() {
  ElMessage.success('上传头像按钮已复现');
}

function save() {
  ElMessage.success('保存按钮已复现');
}
</script>

<style scoped>
.page-card {
  background: #fff;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 16px;
}
</style>
