<template>
  <div
    class="card p-4 cursor-pointer hover:shadow-md transition-all hover:border-primary-200 group"
    @click="$emit('click')"
  >
    <div class="flex items-start justify-between mb-3">
      <div class="flex items-center gap-2 min-w-0">
        <div class="w-8 h-8 bg-red-100 rounded-lg flex items-center justify-center flex-shrink-0">
          <FileText class="w-4 h-4 text-red-600" />
        </div>
        <div class="min-w-0">
          <h3 class="text-sm font-semibold text-slate-800 truncate group-hover:text-primary-700">
            {{ document.title }}
          </h3>
          <p class="text-xs text-slate-400 truncate">{{ document.originalFileName }}</p>
        </div>
      </div>
      <div class="flex items-center gap-1 flex-shrink-0 ml-2">
        <StatusBadge :status="document.status" />
        <button
          @click.stop="$emit('delete')"
          class="opacity-0 group-hover:opacity-100 p-1 hover:bg-red-50 rounded transition-all"
        >
          <Trash2 class="w-3.5 h-3.5 text-red-400" />
        </button>
      </div>
    </div>

    <p v-if="document.summaryShort" class="text-xs text-slate-500 line-clamp-2 mb-3 leading-relaxed">
      {{ document.summaryShort }}
    </p>
    <p v-else-if="document.description" class="text-xs text-slate-500 line-clamp-2 mb-3">
      {{ document.description }}
    </p>

    <div class="flex items-center justify-between text-xs text-slate-400">
      <div class="flex items-center gap-3">
        <span>{{ document.pageCount }}p</span>
        <span v-if="document.documentType" class="px-1.5 py-0.5 bg-surface-100 text-slate-500 rounded">
          {{ document.documentType }}
        </span>
      </div>
      <span>{{ formatDate(document.createdAt) }}</span>
    </div>

    <div v-if="document.tags.length > 0" class="flex flex-wrap gap-1 mt-2">
      <span
        v-for="tag in document.tags.slice(0, 3)"
        :key="tag"
        class="px-1.5 py-0.5 bg-primary-50 text-primary-600 rounded text-xs"
      >{{ tag }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { FileText, Trash2 } from 'lucide-vue-next'
import type { Document } from '@/types'
import StatusBadge from '@/components/common/StatusBadge.vue'

defineProps<{ document: Document }>()
defineEmits<{ click: []; delete: [] }>()

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}
</script>
