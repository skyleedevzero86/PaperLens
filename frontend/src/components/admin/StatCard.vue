<template>
  <div class="card p-4">
    <p class="text-xs font-medium text-slate-500 mb-1">{{ title }}</p>
    <p class="text-2xl font-bold text-slate-800">{{ value.toLocaleString() }}</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; value: number; icon?: string; color?: string }>()
</script>
