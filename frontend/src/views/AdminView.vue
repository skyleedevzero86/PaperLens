<template>
  <div class="max-w-6xl mx-auto px-4 py-6">
    <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-slate-800">관리자 대시보드</h1>
        <p class="text-sm text-slate-500 mt-1">문서 인덱싱 상태와 최근 작업 이력을 확인할 수 있습니다.</p>
      </div>
      <button @click="loadData" :disabled="loading" class="btn-secondary text-sm px-3 py-2">
        {{ loading ? '불러오는 중...' : '새로고침' }}
      </button>
    </div>

    <div v-if="errorMessage" class="mb-6 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
      {{ errorMessage }}
    </div>

    <div v-if="stats" class="grid grid-cols-2 xl:grid-cols-5 gap-4 mb-8">
      <StatCard title="전체 문서" :value="stats.totalDocuments" />
      <StatCard title="인덱싱 완료" :value="stats.indexedDocuments" />
      <StatCard title="대기" :value="stats.pendingDocuments" />
      <StatCard title="처리 중" :value="stats.processingDocuments" />
      <StatCard title="실패" :value="stats.failedDocuments" />
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div class="card p-6">
        <h2 class="text-sm font-semibold text-slate-700 mb-4">인덱싱 진행률</h2>
        <div v-if="stats" class="flex items-center gap-4">
          <div class="w-20 h-20 relative">
            <svg class="transform -rotate-90 w-20 h-20">
              <circle cx="40" cy="40" r="32" stroke="#e2e8f0" stroke-width="8" fill="none" />
              <circle
                cx="40"
                cy="40"
                r="32"
                stroke="#3b82f6"
                stroke-width="8"
                fill="none"
                :stroke-dasharray="`${Math.max(0, Math.min(100, stats.indexingRate)) * 2.01} 201`"
              />
            </svg>
            <div class="absolute inset-0 flex items-center justify-center">
              <span class="text-sm font-bold text-slate-700">{{ Math.round(stats.indexingRate) }}%</span>
            </div>
          </div>
          <div class="text-sm text-slate-600 space-y-1">
            <p>완료: <span class="font-medium text-slate-800">{{ stats.indexedDocuments }}</span></p>
            <p>대기: <span class="font-medium text-slate-800">{{ stats.pendingDocuments }}</span></p>
            <p>처리 중: <span class="font-medium text-blue-700">{{ stats.processingDocuments }}</span></p>
            <p>실패: <span class="font-medium text-red-600">{{ stats.failedDocuments }}</span></p>
          </div>
        </div>
      </div>

      <div class="card p-6">
        <h2 class="text-sm font-semibold text-slate-700 mb-4">AI 응답 성능</h2>
        <div v-if="stats" class="space-y-3">
          <div class="flex justify-between text-sm">
            <span class="text-slate-500">평균 응답 시간</span>
            <span class="font-medium text-slate-800">{{ Math.round(stats.avgLatencyMs) }}ms</span>
          </div>
          <div class="flex justify-between text-sm">
            <span class="text-slate-500">총 질문 수</span>
            <span class="font-medium text-slate-800">{{ stats.totalQueries }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 xl:grid-cols-[1.1fr_0.9fr] gap-6 mt-6">
      <div class="card p-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-sm font-semibold text-slate-700">최근 작업 이력</h2>
          <span class="text-xs text-slate-400">최근 10건</span>
        </div>

        <div v-if="jobs.length === 0" class="text-sm text-slate-400">
          아직 기록된 작업이 없습니다.
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="job in jobs"
            :key="job.id"
            class="rounded-xl border border-slate-100 px-4 py-3"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="text-sm font-medium text-slate-800 truncate">{{ job.documentTitle }}</p>
                <p class="text-xs text-slate-500 mt-1">
                  {{ formatJobType(job.jobType) }} · {{ formatDateTime(job.startedAt) }}
                </p>
                <p v-if="job.errorMessage" class="text-xs text-red-600 mt-2 break-words">
                  {{ job.errorMessage }}
                </p>
              </div>
              <StatusBadge :status="job.status" />
            </div>
          </div>
        </div>
      </div>

      <div class="card p-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-sm font-semibold text-slate-700">처리 실패 문서</h2>
          <span class="text-xs text-slate-400">{{ failedDocs.length }}건</span>
        </div>

        <div v-if="failedDocs.length === 0" class="text-sm text-slate-400">
          현재 실패한 문서가 없습니다.
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="document in failedDocs"
            :key="document.id"
            class="flex items-center justify-between gap-3 rounded-xl bg-red-50 px-4 py-3"
          >
            <div class="min-w-0">
              <p class="text-sm font-medium text-slate-800 truncate">{{ document.title }}</p>
              <p class="text-xs text-slate-500 mt-1">{{ document.originalFileName }}</p>
            </div>
            <button
              @click="reprocess(document.id)"
              :disabled="reprocessingIds.has(document.id)"
              class="btn-secondary text-xs px-2 py-1 shrink-0"
            >
              {{ reprocessingIds.has(document.id) ? '재처리 중...' : '재처리' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { api } from '@/lib/api'
import type { AdminStats, Document, DocumentJob } from '@/types'
import StatCard from '@/components/admin/StatCard.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const REFRESH_INTERVAL_MS = 5000

const stats = ref<AdminStats | null>(null)
const failedDocs = ref<Document[]>([])
const jobs = ref<DocumentJob[]>([])
const loading = ref(false)
const errorMessage = ref('')
const reprocessingIds = ref(new Set<number>())

let refreshTimer: ReturnType<typeof setInterval> | null = null

function formatDateTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function formatJobType(value: string) {
  const labels: Record<string, string> = {
    PARSE: '텍스트 추출',
    SUMMARY: '요약 생성',
    EMBED: '임베딩 생성',
  }
  return labels[value] || value
}

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [statsRes, failedRes, jobsRes] = await Promise.all([
      api.get<AdminStats>('/admin/stats'),
      api.get<Document[]>('/admin/failed-documents'),
      api.get<DocumentJob[]>('/admin/document-jobs', { params: { limit: 10 } }),
    ])

    stats.value = statsRes.data
    failedDocs.value = failedRes.data
    jobs.value = jobsRes.data
  } catch (error: any) {
    const message = error.response?.data?.message
    errorMessage.value = typeof message === 'string'
      ? message
      : '관리자 정보를 불러오는 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

async function reprocess(id: number) {
  const next = new Set(reprocessingIds.value)
  next.add(id)
  reprocessingIds.value = next

  try {
    await api.post(`/admin/documents/${id}/reprocess`)
    await loadData()
  } catch (error: any) {
    const message = error.response?.data?.message
    errorMessage.value = typeof message === 'string'
      ? message
      : '문서 재처리 중 오류가 발생했습니다.'
  } finally {
    const updated = new Set(reprocessingIds.value)
    updated.delete(id)
    reprocessingIds.value = updated
  }
}

onMounted(async () => {
  await loadData()
  refreshTimer = setInterval(() => {
    void loadData()
  }, REFRESH_INTERVAL_MS)
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>
