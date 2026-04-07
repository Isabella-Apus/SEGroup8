<template>
  <section class="page-card">
    <div class="head-row">
      <h2>商品管理</h2>
      <el-button type="primary" @click="openCreate">新增商品</el-button>
    </div>

    <div class="query-row">
      <el-input v-model="query.keyword" placeholder="商品名" style="width: 180px" />
      <el-select v-model="query.status" placeholder="全部" style="width: 120px">
        <el-option label="在售" :value="1" />
        <el-option label="已下架" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" border>
      <el-table-column prop="name" label="商品名" min-width="180" />
      <el-table-column prop="statusName" label="状态" width="90" />
      <el-table-column label="操作" min-width="280">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link @click="toggleStatus(scope.row)">{{ scope.row.status === 1 ? '下架' : '上架' }}</el-button>
          <el-button link @click="openAdjustStock(scope.row)">调库存</el-button>
          <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="formVisible" title="编辑商品" width="520px">
      <el-form label-width="90px">
        <el-form-item label="商品名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="封面图"><el-button @click="uploadImage">上传图片</el-button></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stockVisible" title="调整库存" width="420px">
      <el-input-number v-model="stockDelta" :step="1" />
      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjustStock">确认调整</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

const query = reactive({ keyword: '', status: undefined });
const rows = ref([{ id: 1, name: '示例商品', status: 1, statusName: '在售' }]);
const formVisible = ref(false);
const stockVisible = ref(false);
const stockDelta = ref(1);
const current = ref(null);
const form = reactive({ id: null, name: '' });

function openCreate() {
  form.id = null;
  form.name = '';
  formVisible.value = true;
}

function openEdit(row) {
  form.id = row.id;
  form.name = row.name;
  formVisible.value = true;
}

function save() {
  ElMessage.success('保存按钮已复现');
  formVisible.value = false;
}

function toggleStatus(row) {
  row.status = row.status === 1 ? 0 : 1;
  row.statusName = row.status === 1 ? '在售' : '已下架';
  ElMessage.success('上架/下架按钮已复现');
}

function openAdjustStock(row) {
  current.value = row;
  stockDelta.value = 1;
  stockVisible.value = true;
}

function confirmAdjustStock() {
  ElMessage.success(`确认调整按钮已复现，变更 ${stockDelta.value}`);
  stockVisible.value = false;
}

function remove() {
  ElMessage.success('删除按钮已复现');
}

function search() {
  ElMessage.info('查询按钮已复现');
}

function reset() {
  query.keyword = '';
  query.status = undefined;
  ElMessage.info('重置按钮已复现');
}

function uploadImage() {
  ElMessage.success('上传图片按钮已复现');
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

.query-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
</style>
