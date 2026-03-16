<template>
  <span :class="badgeClass" class="px-1.5 py-0.5 rounded text-xs font-medium">
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: string }>()

const config: Record<string, { label: string; class: string }> = {
  PENDING:    { label: '대기', class: 'bg-yellow-50 text-yellow-700' },
  PROCESSING: { label: '처리중', class: 'bg-blue-50 text-blue-700' },
  INDEXED:    { label: '완료', class: 'bg-green-50 text-green-700' },
  FAILED:     { label: '실패', class: 'bg-red-50 text-red-700' },
}

const badgeClass = computed(() => config[props.status]?.class || 'bg-slate-100 text-slate-600')
const label = computed(() => config[props.status]?.label || props.status)
</script>
