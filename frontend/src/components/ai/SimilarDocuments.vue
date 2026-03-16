<template>
  <div class="space-y-3">
    <div v-if="loading" class="flex justify-center py-8">
      <Loader2 class="w-6 h-6 animate-spin text-slate-300" />
    </div>
    <div v-else-if="documents.length === 0" class="text-center py-8 text-sm text-slate-400">
      유사한 문서가 없습니다.
    </div>
    <RouterLink
      v-else
      v-for="doc in documents"
      :key="doc.documentId"
      :to="`/documents/${doc.documentId}`"
      class="block p-3 border border-slate-100 rounded-lg hover:border-primary-200 hover:bg-primary-50 transition-colors"
    >
      <div class="flex items-start justify-between gap-2">
        <p class="text-sm font-medium text-slate-700 line-clamp-1">{{ doc.title }}</p>
        <span class="text-xs text-primary-600 bg-primary-50 px-1.5 py-0.5 rounded flex-shrink-0">
          {{ Math.round(doc.similarity * 100) }}%
        </span>
      </div>
      <p v-if="doc.summaryShort" class="text-xs text-slate-500 mt-1 line-clamp-2">{{ doc.summaryShort }}</p>
    </RouterLink>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Loader2 } from 'lucide-vue-next'
import { api } from '@/lib/api'
import type { SimilarDocument } from '@/types'

const props = defineProps<{ documentId: number }>()
const documents = ref<SimilarDocument[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await api.get<SimilarDocument[]>(`/ai/similar/${props.documentId}`)
    documents.value = res.data
  } finally {
    loading.value = false
  }
})
</script>
