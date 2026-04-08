<template>
  <div class="status-wrap">
    <el-tag :size="size" :type="statusTagType(status)">{{ statusName || "-" }}</el-tag>
    <span v-if="showRefund && refundStatus > 0" class="sub-status">售后：{{ refundStatusName || "-" }}</span>
  </div>
</template>

<script setup>
const props = defineProps({
  status: { type: Number, default: undefined },
  statusName: { type: String, default: "" },
  refundStatus: { type: Number, default: 0 },
  refundStatusName: { type: String, default: "" },
  size: { type: String, default: "default" },
  showRefund: { type: Boolean, default: true }
});

function statusTagType(status) {
  if (status === 0) return "warning";
  if (status === 1) return "info";
  if (status === 2 || status === 3 || status === 4) return "success";
  if (status === 9) return "danger";
  return "";
}
</script>

<style scoped>
.status-wrap {
  display: inline-flex;
  align-items: center;
}

.sub-status {
  margin-left: 8px;
  color: #b91c1c;
  font-size: 12px;
}
</style>

