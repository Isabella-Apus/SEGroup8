<template>
  <section class="page-card">
    <div class="head-row">
      <h2>地址管理</h2>
      <el-button type="primary" @click="openDialog()">新增地址</el-button>
    </div>
    <el-table :data="rows" border>
      <el-table-column prop="receiverName" label="收件人" width="120" />
      <el-table-column prop="detailAddress" label="详细地址" />
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button link type="primary" @click="openDialog(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="remove(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="地址编辑" width="520px">
      <el-form label-width="90px">
        <el-form-item label="收件人"><el-input v-model="form.receiverName" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.detailAddress" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

const rows = ref([{ id: 1, receiverName: '张三', detailAddress: '测试路 1 号' }]);
const dialogVisible = ref(false);
const form = reactive({ id: null, receiverName: '', detailAddress: '' });

function openDialog(row) {
  if (row) {
    form.id = row.id;
    form.receiverName = row.receiverName;
    form.detailAddress = row.detailAddress;
  } else {
    form.id = null;
    form.receiverName = '';
    form.detailAddress = '';
  }
  dialogVisible.value = true;
}

function remove(id) {
  rows.value = rows.value.filter((row) => row.id !== id);
  ElMessage.success('删除按钮已复现');
}

function saveAddress() {
  if (form.id) {
    rows.value = rows.value.map((row) => (row.id === form.id ? { ...row, ...form } : row));
  } else {
    rows.value.push({ id: Date.now(), receiverName: form.receiverName, detailAddress: form.detailAddress });
  }
  dialogVisible.value = false;
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

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
</style>
