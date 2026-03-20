<template>
  <div class="space-y-3">
    <div v-if="loading" class="flex justify-center py-8">
      <Loader2 class="w-6 h-6 animate-spin text-slate-300" />
    </div>
    <div v-else-if="helperMessage" class="text-center py-6 text-sm text-slate-500">
      {{ helperMessage }}
    </div>
    <div v-else-if="errorMessage" class="text-center py-6 text-sm text-amber-600">
      {{ errorMessage }}
    </div>
    <div v-else-if="documents.length === 0" class="text-center py-8 text-sm text-slate-400">
      유사한 문서를 찾지 못했습니다.
    </div>
    <RouterLink
      v-else
      v-for="document in documents"
      :key="document.documentId"
      :to="`/documents/${document.documentId}`"
      class="block p-3 border border-slate-100 rounded-lg hover:border-primary-200 hover:bg-primary-50 transition-colors"
    >
      <div class="flex items-start justify-between gap-2">
        <p class="text-sm font-medium text-slate-700 line-clamp-1">{{ document.title }}</p>
        <span class="text-xs text-primary-600 bg-primary-50 px-1.5 py-0.5 rounded flex-shrink-0">
          {{ Math.round(document.similarity * 100) }}%
        </span>
      </div>
      <p v-if="document.summaryShort" class="text-xs text-slate-500 mt-1 line-clamp-2">
        {{ document.summaryShort }}
      </p>
    </RouterLink>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Loader2 } from 'lucide-vue-next'
import { api } from '@/lib/api'
import type { SimilarDocument } from '@/types'

const props = defineProps<{
  documentId: number
  documentStatus?: 'PENDING' | 'PROCESSING' | 'INDEXED' | 'FAILED' | null
}>()

const MAX_RETRIES = 15

const documents = ref<SimilarDocument[]>([])
const loading = ref(false)
const errorMessage = ref('')
const retryCount = ref(0)
let retryTimer: ReturnType<typeof setTimeout> | null = null

const helperMessage = computed(() => {
  if (loading.value || errorMessage.value) return ''
  if (props.documentStatus === 'PENDING' || props.documentStatus === 'PROCESSING') {
    return '유사 문서를 준비하는 중입니다. 잠시 후 자동으로 갱신됩니다.'
  }
  if (props.documentStatus === 'FAILED') {
    return '문서 인덱싱을 다시 시도하는 중입니다. 잠시 후 자동으로 갱신됩니다.'
  }
  return ''
})

function clearRetryTimer() {
  if (!retryTimer) return
  clearTimeout(retryTimer)
  retryTimer = null
}

function scheduleRetry() {
  clearRetryTimer()
  if (retryCount.value >= MAX_RETRIES) return

  retryTimer = setTimeout(async () => {
    retryCount.value += 1
    await loadSimilarDocuments()
  }, 2000)
}

async function loadSimilarDocuments() {
  if (!props.documentId) return

  loading.value = true
  errorMessage.value = ''
  try {
    const response = await api.get<SimilarDocument[]>(`/ai/similar/${props.documentId}`)
    documents.value = response.data
  } catch (error: any) {
    const message = error.response?.data?.message
    errorMessage.value = typeof message === 'string'
      ? message
      : '유사 문서를 불러오는 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }

  if (documents.value.length === 0 && props.documentStatus !== 'INDEXED' && !errorMessage.value) {
    scheduleRetry()
  } else {
    clearRetryTimer()
  }
}

watch(
  () => [props.documentId, props.documentStatus] as const,
  async () => {
    retryCount.value = 0
    documents.value = []
    clearRetryTimer()
    await loadSimilarDocuments()
  },
)

onMounted(async () => {
  await loadSimilarDocuments()
})

onBeforeUnmount(() => {
  clearRetryTimer()
})
</script>
