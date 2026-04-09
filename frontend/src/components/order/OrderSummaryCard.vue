<template>
  <el-alert
    :title="`当前阶段：${stage}`"
    :description="`建议操作：${nextAction}`"
    type="info"
    show-icon
    :closable="false"
    style="margin-bottom: 12px"
  />
</template>

<script setup>
defineProps({
  stage: { type: String, default: "-" },
  nextAction: { type: String, default: "-" }
});
</script>

