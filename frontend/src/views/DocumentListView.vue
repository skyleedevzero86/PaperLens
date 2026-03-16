<template>
  <div class="max-w-7xl mx-auto px-4 py-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-slate-800">문서</h1>
        <p class="text-sm text-slate-500 mt-0.5">{{ store.total }}개의 문서</p>
      </div>
      <button @click="showUpload = true" class="btn-primary">
        <Upload class="w-4 h-4" />
        PDF 업로드
      </button>
    </div>

    <div class="card p-4 mb-6">
      <div class="flex gap-3">
        <div class="flex-1 relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            v-model="searchQuery"
            @input="debounceSearch"
            type="text"
            class="input pl-9"
            placeholder="문서 제목, 내용으로 검색..."
          />
        </div>
        <select v-model="searchMode" @change="doSearch" class="input w-36">
          <option value="HYBRID">하이브리드</option>
          <option value="KEYWORD">키워드</option>
          <option value="SEMANTIC">의미 검색</option>
          <option value="FUZZY">오타 허용</option>
        </select>
        <select v-model="docTypeFilter" @change="doSearch" class="input w-36">
          <option value="">전체 유형</option>
          <option value="계약서">계약서</option>
          <option value="매뉴얼">매뉴얼</option>
          <option value="제안서">제안서</option>
          <option value="보고서">보고서</option>
          <option value="정책문서">정책문서</option>
        </select>
      </div>
    </div>

    <div v-if="store.loading" class="flex justify-center py-20">
      <Loader2 class="w-8 h-8 animate-spin text-primary-500" />
    </div>

    <div v-else-if="displayDocuments.length === 0" class="card p-16 text-center">
      <FileText class="w-12 h-12 text-slate-300 mx-auto mb-3" />
      <p class="text-slate-500">문서가 없습니다. PDF를 업로드해주세요.</p>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <DocumentCard
        v-for="doc in displayDocuments"
        :key="doc.id"
        :document="doc"
        @click="router.push(`/documents/${doc.id}`)"
        @delete="handleDelete(doc.id)"
      />
    </div>

    <div v-if="!isSearchMode && store.totalPages > 1" class="flex justify-center mt-8 gap-2">
      <button
        v-for="p in store.totalPages"
        :key="p"
        @click="goPage(p - 1)"
        :class="[
          'w-8 h-8 rounded-lg text-sm font-medium transition-colors',
          currentPage === p - 1
            ? 'bg-primary-600 text-white'
            : 'bg-white text-slate-600 hover:bg-surface-100 border border-slate-200'
        ]"
      >
        {{ p }}
      </button>
    </div>

    <UploadModal v-if="showUpload" @close="showUpload = false" @success="onUploadSuccess" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Upload, FileText, Loader2 } from 'lucide-vue-next'
import { useDocumentStore } from '@/stores/document'
import { api } from '@/lib/api'
import type { SearchResult } from '@/types'
import DocumentCard from '@/components/document/DocumentCard.vue'
import UploadModal from '@/components/document/UploadModal.vue'

const router = useRouter()
const store = useDocumentStore()

const showUpload = ref(false)
const searchQuery = ref('')
const searchMode = ref('HYBRID')
const docTypeFilter = ref('')
const currentPage = ref(0)
const searchResults = ref<SearchResult[]>([])
const isSearchMode = ref(false)

let debounceTimer: ReturnType<typeof setTimeout>

function debounceSearch() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(doSearch, 400)
}

async function doSearch() {
  if (searchQuery.value.trim()) {
    isSearchMode.value = true
    const res = await api.get('/search', {
      params: { query: searchQuery.value, mode: searchMode.value, docType: docTypeFilter.value || undefined }
    })
    searchResults.value = res.data.results
  } else {
    isSearchMode.value = false
    store.fetchDocuments({ docType: docTypeFilter.value || undefined, page: currentPage.value })
  }
}

const displayDocuments = computed(() => {
  if (isSearchMode.value) {
    return searchResults.value.map(r => ({
      id: r.documentId,
      title: r.title,
      description: null,
      originalFileName: '',
      pageCount: 0,
      fileSize: 0,
      documentType: r.documentType,
      summaryShort: r.summaryShort,
      status: 'INDEXED' as const,
      tags: [],
      createdAt: ''
    }))
  }
  return store.documents
})

async function handleDelete(id: number) {
  if (confirm('문서를 삭제하시겠습니까?')) {
    await store.deleteDocument(id)
  }
}

function goPage(page: number) {
  currentPage.value = page
  store.fetchDocuments({ page, docType: docTypeFilter.value || undefined })
}

function onUploadSuccess() {
  showUpload.value = false
  store.fetchDocuments()
}

onMounted(() => store.fetchDocuments())
</script>
