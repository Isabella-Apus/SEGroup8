<template>
  <section class="page-card">
    <h2>发布二手</h2>
    <el-form label-width="90px">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="价格"><el-input-number v-model="form.price" :min="1" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.desc" type="textarea" :rows="3" /></el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">发布</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup>
import { reactive } from 'vue';
import { ElMessage } from 'element-plus';

const form = reactive({ title: '', price: 1, desc: '' });

function submit() {
  ElMessage.success('发布二手按钮已复现');
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
