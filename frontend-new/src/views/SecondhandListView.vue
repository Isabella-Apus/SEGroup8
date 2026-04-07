<template>
  <section class="page-card">
    <h2>二手市场</h2>
    <div class="actions">
      <el-button type="primary" @click="$router.push('/secondhand/publish')">去发布二手</el-button>
    </div>
    <el-empty description="二手列表按钮入口已复现" />
  </section>
</template>

<style scoped>
.page-card {
  background: #fff;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 16px;
}

.actions {
  margin: 8px 0 12px;
}
</style>
