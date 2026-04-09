<template>
  <div class="timeline-block">
    <div class="section-head">
      <div class="section-title">{{ title }}</div>
      <el-button v-if="collapsible" text type="primary" @click="$emit('toggle')">
        {{ expanded ? "收起时间线" : "展开时间线" }}
      </el-button>
    </div>
    <div class="timeline">
      <div v-for="step in displayedSteps" :key="step.key" class="timeline-step">
        <div class="timeline-dot">
          <span :class="['timeline-dot__inner', step.status]"></span>
        </div>
        <div class="timeline-content">
          <div class="timeline-label">
            {{ step.label }}
            <el-tag v-if="step.status === 'active'" size="small" :type="activeTagType" style="margin-left: 8px">进行中</el-tag>
          </div>
          <div class="timeline-time">{{ step.time || "-" }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  title: { type: String, required: true },
  steps: { type: Array, default: () => [] },
  expanded: { type: Boolean, default: false },
  defaultCount: { type: Number, default: 3 },
  collapsible: { type: Boolean, default: true },
  activeTagType: { type: String, default: "primary" }
});

defineEmits(["toggle"]);

const displayedSteps = computed(() => {
  if (!props.collapsible || props.expanded) return props.steps;
  return props.steps.slice(0, props.defaultCount);
});
</script>

<style scoped>
.timeline-block {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fbfbfc;
  border: 1px solid #f3f4f6;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.section-title {
  font-weight: 600;
  color: #111827;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.timeline-step {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.timeline-dot {
  width: 18px;
  display: flex;
  justify-content: center;
  padding-top: 2px;
}

.timeline-dot__inner {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  background: #e5e7eb;
}

.timeline-dot__inner.done {
  background: #22c55e;
}

.timeline-dot__inner.active {
  background: #3b82f6;
}

.timeline-dot__inner.todo {
  background: #e5e7eb;
}

.timeline-content {
  flex: 1;
}

.timeline-label {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-weight: 500;
  color: #111827;
}

.timeline-time {
  color: #6b7280;
  font-size: 12px;
  margin-top: 2px;
}
</style>

