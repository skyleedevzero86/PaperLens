<template>
  <div v-if="document" class="h-[calc(100vh-56px)] flex">
    <div class="flex-1 min-w-0 border-r border-slate-200 flex flex-col">
      <div class="flex items-center gap-2 px-4 py-2 bg-white border-b border-slate-200">
        <RouterLink to="/" class="btn-secondary text-xs px-2 py-1">
          <ChevronLeft class="w-3 h-3" />
          목록
        </RouterLink>
        <span class="text-sm font-medium text-slate-700 truncate">{{ document.title }}</span>
        <div class="ml-auto flex items-center gap-2">
          <span class="text-xs text-slate-400">{{ document.pageCount }}페이지</span>
          <a :href="`/api/documents/${document.id}/download`" class="btn-secondary text-xs px-2 py-1">
            <Download class="w-3 h-3" />
            다운로드
          </a>
        </div>
      </div>
      <PdfViewer :document-id="document.id" :document-title="document.title" class="flex-1" />
    </div>

    <div class="w-96 flex flex-col overflow-hidden bg-white">
      <div class="flex border-b border-slate-200 px-4">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          @click="activeTab = tab.key"
          :class="[
            'py-3 px-2 text-sm font-medium border-b-2 -mb-px transition-colors',
            activeTab === tab.key
              ? 'border-primary-600 text-primary-700'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          ]"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="flex-1 overflow-y-auto p-4">
        <div v-if="activeTab === 'meta'" class="space-y-4">
          <div>
            <p class="text-xs font-medium text-slate-500 uppercase tracking-wide mb-1">제목</p>
            <p class="text-sm text-slate-800 font-medium">{{ document.title }}</p>
          </div>
          <div v-if="document.description">
            <p class="text-xs font-medium text-slate-500 uppercase tracking-wide mb-1">설명</p>
            <p class="text-sm text-slate-600">{{ document.description }}</p>
          </div>
          <div>
            <p class="text-xs font-medium text-slate-500 uppercase tracking-wide mb-1">정보</p>
            <div class="space-y-1 text-sm text-slate-600">
              <div class="flex justify-between gap-4">
                <span>파일명</span>
                <span class="text-slate-800 truncate max-w-48" :title="document.originalFileName">
                  {{ document.originalFileName }}
                </span>
              </div>
              <div class="flex justify-between">
                <span>페이지 수</span>
                <span class="text-slate-800">{{ document.pageCount }}페이지</span>
              </div>
              <div class="flex justify-between">
                <span>파일 크기</span>
                <span class="text-slate-800">{{ formatSize(document.fileSize) }}</span>
              </div>
              <div class="flex justify-between">
                <span>문서 유형</span>
                <span class="text-slate-800">{{ document.documentType || '-' }}</span>
              </div>
              <div class="flex justify-between">
                <span>상태</span>
                <StatusBadge :status="document.status" />
              </div>
            </div>
          </div>
          <div v-if="document.tags.length > 0">
            <p class="text-xs font-medium text-slate-500 uppercase tracking-wide mb-2">태그</p>
            <div class="flex flex-wrap gap-1">
              <span
                v-for="tag in document.tags"
                :key="tag"
                class="px-2 py-0.5 bg-primary-50 text-primary-700 rounded-full text-xs"
              >
                {{ tag }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="activeTab === 'summary'" class="space-y-4">
          <div v-if="document.summaryShort" class="p-3 bg-primary-50 rounded-lg">
            <p class="text-xs font-medium text-primary-700 mb-1">3줄 요약</p>
            <p class="text-sm text-slate-700 leading-relaxed">{{ document.summaryShort }}</p>
          </div>
          <div v-if="document.summaryLong">
            <p class="text-xs font-medium text-slate-500 uppercase tracking-wide mb-2">상세 요약</p>
            <p class="text-sm text-slate-600 leading-relaxed whitespace-pre-line">{{ document.summaryLong }}</p>
          </div>
          <div v-if="!document.summaryShort && !document.summaryLong" class="text-center py-8 text-slate-400 text-sm">
            <p>{{ shouldRefreshDocument(document) ? '문서를 분석하는 중입니다. 잠시 후 자동으로 갱신됩니다.' : '요약이 아직 생성되지 않았습니다.' }}</p>
          </div>
        </div>

        <div v-if="activeTab === 'qa'" class="flex flex-col h-full -m-4">
          <QaPanel :document-id="document.id" />
        </div>

        <div v-if="activeTab === 'similar'">
          <SimilarDocuments :document-id="document.id" :document-status="document.status" />
        </div>
      </div>
    </div>
  </div>
  <div v-else class="flex items-center justify-center h-64">
    <Loader2 class="w-8 h-8 animate-spin text-primary-500" />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronLeft, Download, Loader2 } from 'lucide-vue-next'
import { useDocumentStore } from '@/stores/document'
import type { DocumentDetail } from '@/types'
import PdfViewer from '@/components/viewer/PdfViewer.vue'
import QaPanel from '@/components/ai/QaPanel.vue'
import SimilarDocuments from '@/components/ai/SimilarDocuments.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const route = useRoute()
const store = useDocumentStore()
const document = ref<DocumentDetail | null>(store.currentDocument)

const activeTab = ref('summary')
const tabs = [
  { key: 'summary', label: 'AI 요약' },
  { key: 'qa', label: '질문응답' },
  { key: 'similar', label: '유사 문서' },
  { key: 'meta', label: '정보' },
]

let refreshTimer: ReturnType<typeof setTimeout> | null = null
let refreshAttempts = 0

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function shouldRefreshDocument(nextDocument: DocumentDetail | null) {
  if (!nextDocument) return false
  if (nextDocument.status === 'FAILED') return false
  if (nextDocument.status === 'PENDING' || nextDocument.status === 'PROCESSING') return true
  return !nextDocument.summaryShort && !nextDocument.summaryLong
}

function clearRefreshTimer() {
  if (!refreshTimer) return
  clearTimeout(refreshTimer)
  refreshTimer = null
}

function scheduleRefresh() {
  clearRefreshTimer()
  if (!shouldRefreshDocument(document.value) || refreshAttempts >= 15) return

  refreshTimer = setTimeout(async () => {
    const id = Number(route.params.id)
    refreshAttempts += 1
    document.value = await store.fetchDocument(id)
    scheduleRefresh()
  }, 2000)
}

onMounted(async () => {
  const id = Number(route.params.id)
  refreshAttempts = 0
  document.value = await store.fetchDocument(id)
  scheduleRefresh()
})

watch(
  () => route.params.id,
  async (nextId) => {
    const id = Number(nextId)
    if (!id || Number.isNaN(id)) return
    refreshAttempts = 0
    document.value = await store.fetchDocument(id)
    scheduleRefresh()
  },
)

onBeforeUnmount(() => {
  clearRefreshTimer()
})
</script>
