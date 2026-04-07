<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索用户名/昵称" clearable style="width: 260px" />
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 160px">
        <el-option label="NORMAL" value="NORMAL" />
        <el-option label="BANNED" value="BANNED" />
      </el-select>
      <el-button type="primary" @click="loadUsers">查询</el-button>
    </div>

    <el-table :data="records" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="role" label="角色" width="100" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="creditScore" label="信用分" width="100" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'BANNED'" link type="danger" @click="changeStatus(row, 'ban')">封禁</el-button>
          <el-button v-else link type="success" @click="changeStatus(row, 'unban')">解封</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="loadUsers"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { banUserApi, pageUsersApi, unbanUserApi } from '@/api/adminUser';

const records = ref([]);
const total = ref(0);
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: ''
});

onMounted(loadUsers);

async function loadUsers() {
  try {
    const result = await pageUsersApi(query);
    records.value = result.data.records || [];
    total.value = result.data.total || 0;
  } catch (error) {
    records.value = [];
    total.value = 0;
  }
}

async function changeStatus(row, action) {
  if (action === 'ban') {
    await banUserApi(row.id);
    ElMessage.success('封禁成功');
  } else {
    await unbanUserApi(row.id);
    ElMessage.success('解封成功');
  }
  await loadUsers();
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
