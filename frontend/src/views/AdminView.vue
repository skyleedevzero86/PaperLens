<template>
  <div class="max-w-6xl mx-auto px-4 py-6">
    <h1 class="text-2xl font-bold text-slate-800 mb-6">관리자 대시보드</h1>

    <div v-if="stats" class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
      <StatCard title="전체 문서" :value="stats.totalDocuments" icon="FileText" color="blue" />
      <StatCard title="인덱싱 완료" :value="stats.indexedDocuments" icon="CheckCircle" color="green" />
      <StatCard title="처리 실패" :value="stats.failedDocuments" icon="XCircle" color="red" />
      <StatCard title="총 질문 수" :value="stats.totalQueries" icon="MessageSquare" color="purple" />
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div class="card p-6">
        <h2 class="text-sm font-semibold text-slate-700 mb-4">인덱싱 성공률</h2>
        <div class="flex items-center gap-4">
          <div class="w-20 h-20 relative">
            <svg class="transform -rotate-90 w-20 h-20">
              <circle cx="40" cy="40" r="32" stroke="#e2e8f0" stroke-width="8" fill="none" />
              <circle
                cx="40" cy="40" r="32"
                stroke="#3b82f6" stroke-width="8" fill="none"
                :stroke-dasharray="`${(stats?.indexingRate || 0) * 2.01} 201`"
              />
            </svg>
            <div class="absolute inset-0 flex items-center justify-center">
              <span class="text-sm font-bold text-slate-700">{{ Math.round(stats?.indexingRate || 0) }}%</span>
            </div>
          </div>
          <div class="text-sm text-slate-600 space-y-1">
            <p>완료: <span class="font-medium text-slate-800">{{ stats?.indexedDocuments }}</span></p>
            <p>대기: <span class="font-medium text-slate-800">{{ stats?.pendingDocuments }}</span></p>
            <p>실패: <span class="font-medium text-red-600">{{ stats?.failedDocuments }}</span></p>
          </div>
        </div>
      </div>

      <div class="card p-6">
        <h2 class="text-sm font-semibold text-slate-700 mb-4">AI 응답 성능</h2>
        <div class="space-y-3">
          <div class="flex justify-between text-sm">
            <span class="text-slate-500">평균 응답 시간</span>
            <span class="font-medium text-slate-800">{{ Math.round(stats?.avgLatencyMs || 0) }}ms</span>
          </div>
          <div class="flex justify-between text-sm">
            <span class="text-slate-500">총 질문 수</span>
            <span class="font-medium text-slate-800">{{ stats?.totalQueries }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="card p-6 mt-6">
      <h2 class="text-sm font-semibold text-slate-700 mb-4">처리 실패 문서</h2>
      <div v-if="failedDocs.length === 0" class="text-sm text-slate-400">실패한 문서가 없습니다.</div>
      <div v-else class="space-y-2">
        <div
          v-for="doc in failedDocs"
          :key="doc.id"
          class="flex items-center justify-between p-3 bg-red-50 rounded-lg"
        >
          <span class="text-sm text-slate-700">{{ doc.title }}</span>
          <button @click="reprocess(doc.id)" class="btn-secondary text-xs px-2 py-1">
            재처리
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '@/lib/api'
import type { AdminStats, Document } from '@/types'
import StatCard from '@/components/admin/StatCard.vue'

const stats = ref<AdminStats | null>(null)
const failedDocs = ref<Document[]>([])

async function loadData() {
  const [statsRes, failedRes] = await Promise.all([
    api.get<AdminStats>('/admin/stats'),
    api.get<Document[]>('/admin/failed-documents')
  ])
  stats.value = statsRes.data
  failedDocs.value = failedRes.data
}

async function reprocess(id: number) {
  await api.post(`/admin/documents/${id}/reprocess`)
  await loadData()
}

onMounted(loadData)
</script>
