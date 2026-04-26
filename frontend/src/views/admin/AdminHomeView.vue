<template>
  <div class="page-card">
    <h2 class="page-title">管理后台首页</h2>
    <el-alert title="管理员功能入口" type="info" :closable="false" show-icon style="margin-bottom: 16px" />
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="entry-card" @click="router.push('/admin/users')">
          <h3>用户管理</h3>
          <p class="empty-tip">查看用户列表，执行封禁与解封</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="entry-card" @click="router.push('/admin/merchant-review')">
          <h3>入驻审核</h3>
          <p class="empty-tip">审核商家入驻申请</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="entry-card" @click="router.push('/admin/orders')">
          <h3>订单监管</h3>
          <p class="empty-tip">处理退款、批量关闭、查看售后日志</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="entry-card" @click="router.push('/admin/vouchers')">
          <h3>优惠券管理</h3>
          <p class="empty-tip">创建、编辑、关闭和删除平台优惠券</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<style scoped>
.entry-card {
  cursor: pointer;
  min-height: 130px;
}

.entry-card h3 {
  margin: 0 0 10px;
}
</style>
