<template>
  <div class="page-card">
    <h2 class="page-title">发布二手商品</h2>
    <el-form :model="form" label-width="100px" style="max-width: 680px">
      <el-form-item label="商品名称">
        <el-input v-model="form.name" placeholder="请输入二手商品名称" />
      </el-form-item>
      <el-form-item label="封面地址">
        <el-input v-model="form.cover" placeholder="可选，填写图片URL" />
      </el-form-item>
      <el-form-item label="商品描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="请输入商品描述"
        />
      </el-form-item>
      <el-form-item label="原价">
        <el-input-number v-model="form.originPrice" :min="0.01" :precision="2" :step="10" />
      </el-form-item>
      <el-form-item label="售价">
        <el-input-number v-model="form.salePrice" :min="0.01" :precision="2" :step="10" />
      </el-form-item>
      <el-form-item label="成色">
        <el-input v-model="form.conditionLevel" placeholder="例如：95新 / 90%" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 180px">
          <el-option label="在售" :value="1" />
          <el-option label="下架" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">发布</el-button>
      </el-form-item>
    </el-form>

    <h3 style="margin: 20px 0 10px">我发布的二手商品</h3>
    <el-table v-loading="loading" :data="myList" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="商品名" />
      <el-table-column prop="salePrice" label="售价" width="120" />
      <el-table-column prop="conditionLevel" label="成色" width="120" />
      <el-table-column prop="statusName" label="状态" width="120" />
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="changeStatus(scope.row, scope.row.status === 1 ? 2 : 1)"
          >
            {{ scope.row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button link type="danger" @click="removeItem(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  changeSecondhandStatusApi,
  createSecondhandApi,
  deleteSecondhandApi,
  getMySecondhandListApi
} from "@/api/secondhand";

const loading = ref(false);
const submitting = ref(false);
const myList = ref([]);

const form = ref({
  name: "",
  cover: "",
  description: "",
  originPrice: undefined,
  salePrice: undefined,
  conditionLevel: "",
  status: 1
});

onMounted(async () => {
  await fetchMyList();
});

async function fetchMyList() {
  loading.value = true;
  try {
    const result = await getMySecondhandListApi({ pageNum: 1, pageSize: 100 });
    myList.value = result.data?.records || [];
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  submitting.value = true;
  try {
    await createSecondhandApi(form.value);
    ElMessage.success("发布成功");
    form.value = {
      name: "",
      cover: "",
      description: "",
      originPrice: undefined,
      salePrice: undefined,
      conditionLevel: "",
      status: 1
    };
    await fetchMyList();
  } finally {
    submitting.value = false;
  }
}

async function changeStatus(row, status) {
  await changeSecondhandStatusApi(row.id, status);
  ElMessage.success(status === 1 ? "已上架" : "已下架");
  await fetchMyList();
}

async function removeItem(row) {
  await ElMessageBox.confirm(`确认删除「${row.name}」吗？`, "提示", { type: "warning" });
  await deleteSecondhandApi(row.id);
  ElMessage.success("删除成功");
  await fetchMyList();
}
</script>
