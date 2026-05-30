<template>
  <div class="status-wrap">
    <el-tag class="status-tag" :class="statusClass(status)" :size="size" effect="plain">
      {{ statusName || "-" }}
    </el-tag>
    <el-tag
      v-if="showRefund && refundStatus > 0"
      class="status-tag sub-status"
      :class="refundStatusClass(refundStatus)"
      :size="size"
      effect="plain"
    >
      售后：{{ refundStatusName || "-" }}
    </el-tag>
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

function statusClass(status) {
  if (status === 4) return "status-success";
  if (status === 9) return "status-danger";
  if (status === 1 || status === 2 || status === 3) return "status-progress";
  if (status === 0) return "status-pending";
  return "status-muted";
}

function refundStatusClass(status) {
  if (status === 2) return "status-success";
  if (status === 3) return "status-danger";
  if (status === 1) return "status-progress";
  return "status-muted";
}
</script>

<style scoped>
.status-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.sub-status {
  font-size: 12px;
}
</style>

