<template>
  <section class="publish-page">
    <div class="hero">
      <div>
        <h1>发布二手商品</h1>
      </div>
      <div class="hero-dot"></div>
    </div>

    <div class="form-shell">
      <el-form :model="form" label-width="96px">
        <el-form-item label="商品名称">
          <el-input v-model="form.name" placeholder="例如：九成新办公椅" />
        </el-form-item>
        <el-form-item label="封面链接">
          <el-input v-model="form.cover" placeholder="请输入图片 URL" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="form.originPrice" :min="1" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="售价">
          <el-input-number v-model="form.salePrice" :min="1" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="成色">
          <el-select v-model="form.condition" style="width: 180px">
            <el-option label="95新" value="95%" />
            <el-option label="9成新" value="90%" />
            <el-option label="8成新" value="80%" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">发布二手</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button text @click="$router.push('/secondhand')">返回列表</el-button>
        </el-form-item>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { publishSecondhandApi } from '@/api/secondhand';

const form = reactive({
  name: '',
  cover: '',
  originPrice: 100,
  salePrice: 80,
  condition: '90%',
  description: ''
});

const submitting = ref(false);

async function submit() {
  submitting.value = true;
  try {
    await publishSecondhandApi({
      name: form.name,
      cover: form.cover,
      description: form.description,
      originPrice: form.originPrice,
      salePrice: form.salePrice,
      conditionLevel: form.condition,
    });
    ElMessage.success('二手商品发布成功');
    reset();
  } finally {
    submitting.value = false;
  }
}

function reset() {
  form.name = '';
  form.cover = '';
  form.originPrice = 100;
  form.salePrice = 80;
  form.condition = '90%';
  form.description = '';
}
</script>

<style scoped>
.publish-page {
  padding: 8px 10px 20px;
}

.hero {
  border-radius: 22px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  margin-bottom: 14px;
  background: linear-gradient(120deg, #ff6f2f, #ff9822);
}

.hero h1 {
  margin: 0;
  font-size: 30px;
}

.hero p {
  margin: 8px 0 0;
  opacity: .92;
}

.hero-dot {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  background: radial-gradient(circle at 25% 25%, #fff5, #fff1 60%, transparent 70%);
}

.form-shell {
  background: #fff;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 16px;
}

@media (max-width: 760px) {
  .publish-page {
    padding: 6px;
  }

  .hero {
    padding: 14px;
    border-radius: 16px;
  }

  .hero h1 {
    font-size: 24px;
  }
}
</style>
