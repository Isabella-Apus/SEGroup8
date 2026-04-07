<template>
  <section class="page-card">
    <h2>申请成为卖家</h2>
    <el-form label-width="110px">
      <el-form-item label="店名"><el-input v-model="form.storeName" /></el-form-item>
      <el-form-item label="负责人"><el-input v-model="form.contactName" /></el-form-item>
      <el-form-item>
        <el-button @click="uploadLicense">上传营业执照图片</el-button>
        <el-button type="primary" @click="submit">提交申请</el-button>
        <el-button type="success" @click="$router.push('/merchant')">进入卖家工作台</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup>
import { reactive } from 'vue';
import { ElMessage } from 'element-plus';

const form = reactive({ storeName: '', contactName: '' });

function uploadLicense() {
  ElMessage.success('上传营业执照图片按钮已复现');
}

function submit() {
  ElMessage.success('提交申请按钮已复现');
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
