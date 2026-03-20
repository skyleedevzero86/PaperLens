<template>
  <div class="max-w-7xl mx-auto px-4 py-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-slate-800">Documents</h1>
        <p class="text-sm text-slate-500 mt-0.5">{{ store.total }} files</p>
      </div>
      <button @click="showUpload = true" class="btn-primary">
        <Upload class="w-4 h-4" />
        Upload PDF
      </button>
    </div>

    <div class="card p-4 mb-6">
      <div class="flex flex-col gap-3">
        <div class="flex gap-3">
          <div class="flex-1 relative">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input
              v-model="searchQuery"
              @input="debounceSearch"
              type="text"
              class="input pl-9"
              placeholder="Search by title or content"
            />
          </div>
          <select v-model="searchMode" @change="doSearch" class="input w-36">
            <option value="HYBRID">Hybrid</option>
            <option value="KEYWORD">Keyword</option>
            <option value="SEMANTIC">Semantic</option>
            <option value="FUZZY">Fuzzy</option>
          </select>
          <select v-model="docTypeFilter" @change="onFilterChange" class="input w-36">
            <option value="">All types</option>
            <option value="계약서">Contract</option>
            <option value="매뉴얼">Manual</option>
            <option value="제안서">Proposal</option>
            <option value="보고서">Report</option>
            <option value="정책문서">Policy</option>
          </select>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <span class="text-xs font-medium text-slate-500">Tag filter</span>
          <button
            type="button"
            @click="clearTags"
            :class="[
              'px-2.5 py-1 rounded-full text-xs border transition-colors',
              selectedTags.length === 0
                ? 'bg-primary-600 text-white border-primary-600'
                : 'bg-white text-slate-600 border-slate-200 hover:border-primary-300'
            ]"
          >
            All
          </button>
          <button
            v-for="tag in availableTags"
            :key="tag"
            type="button"
            @click="toggleTag(tag)"
            :class="[
              'px-2.5 py-1 rounded-full text-xs border transition-colors',
              selectedTags.includes(tag)
                ? 'bg-primary-50 text-primary-700 border-primary-300'
                : 'bg-white text-slate-600 border-slate-200 hover:border-primary-300'
            ]"
          >
            {{ tag }}
          </button>
          <span v-if="availableTags.length === 0" class="text-xs text-slate-400">
            No tags available yet.
          </span>
        </div>
      </div>
    </div>

    <div v-if="store.loading" class="flex justify-center py-20">
      <Loader2 class="w-8 h-8 animate-spin text-primary-500" />
    </div>

    <div v-else-if="displayDocuments.length === 0" class="card p-16 text-center">
      <FileText class="w-12 h-12 text-slate-300 mx-auto mb-3" />
      <p class="text-slate-500">No documents match the current filters.</p>
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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Upload, FileText, Loader2 } from 'lucide-vue-next'
import { useDocumentStore } from '@/stores/document'
import { api } from '@/lib/api'
import type { SearchResult } from '@/types'
import DocumentCard from '@/components/document/DocumentCard.vue'
import UploadModal from '@/components/document/UploadModal.vue'

const route = useRoute()
const router = useRouter()
const store = useDocumentStore()

const showUpload = ref(false)
const searchQuery = ref('')
const searchMode = ref('HYBRID')
const docTypeFilter = ref('')
const availableTags = ref<string[]>([])
const selectedTags = ref<string[]>([])
const currentPage = ref(0)
const searchResults = ref<SearchResult[]>([])
const isSearchMode = ref(false)

let debounceTimer: ReturnType<typeof setTimeout>

function parseRouteTags(value: unknown): string[] {
  const rawValues = Array.isArray(value) ? value : value ? [value] : []
  return [...new Set(
    rawValues
      .flatMap((entry) => String(entry).split(','))
      .map((entry) => entry.trim())
      .filter(Boolean)
  )]
}

function syncRouteQuery() {
  const query: Record<string, string | string[] | undefined> = {}

  if (searchQuery.value.trim()) query.query = searchQuery.value.trim()
  if (searchQuery.value.trim()) query.mode = searchMode.value
  if (docTypeFilter.value) query.docType = docTypeFilter.value
  if (selectedTags.value.length > 0) query.tags = selectedTags.value
  if (!searchQuery.value.trim() && currentPage.value > 0) query.page = String(currentPage.value)

  void router.replace({ query })
}

function debounceSearch() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(doSearch, 400)
}

function buildParams() {
  const params = new URLSearchParams()
  if (docTypeFilter.value) params.append('docType', docTypeFilter.value)
  selectedTags.value.forEach(tag => params.append('tags', tag))
  return params
}

async function doSearch() {
  const params = buildParams()
  syncRouteQuery()

  if (searchQuery.value.trim()) {
    isSearchMode.value = true
    params.append('query', searchQuery.value)
    params.append('mode', searchMode.value)
    const res = await api.get('/search', { params })
    searchResults.value = res.data.results
  } else {
    isSearchMode.value = false
    await store.fetchDocuments({
      docType: docTypeFilter.value || undefined,
      tags: selectedTags.value,
      page: currentPage.value,
    })
  }
}

async function loadAvailableTags() {
  const res = await api.get<string[]>('/documents/tags')
  availableTags.value = res.data
}

function toggleTag(tag: string) {
  selectedTags.value = selectedTags.value.includes(tag)
    ? selectedTags.value.filter((value) => value !== tag)
    : [...selectedTags.value, tag]
  currentPage.value = 0
  void doSearch()
}

function clearTags() {
  selectedTags.value = []
  currentPage.value = 0
  void doSearch()
}

function onFilterChange() {
  currentPage.value = 0
  void doSearch()
}

const displayDocuments = computed(() => {
  if (isSearchMode.value) {
    return searchResults.value.map((result) => ({
      id: result.documentId,
      title: result.title,
      description: null,
      originalFileName: '',
      pageCount: 0,
      fileSize: 0,
      documentType: result.documentType,
      summaryShort: result.summaryShort,
      status: 'INDEXED' as const,
      tags: result.tags,
      createdAt: '',
    }))
  }

  return store.documents
})

async function handleDelete(id: number) {
  if (confirm('Delete this document?')) {
    await store.deleteDocument(id)
    await doSearch()
  }
}

async function goPage(page: number) {
  currentPage.value = page
  syncRouteQuery()
  await store.fetchDocuments({
    page,
    docType: docTypeFilter.value || undefined,
    tags: selectedTags.value,
  })
}

function onUploadSuccess() {
  showUpload.value = false
  void doSearch()
}

onMounted(async () => {
  searchQuery.value = typeof route.query.query === 'string' ? route.query.query : ''
  searchMode.value = typeof route.query.mode === 'string' ? route.query.mode : 'HYBRID'
  docTypeFilter.value = typeof route.query.docType === 'string' ? route.query.docType : ''
  selectedTags.value = parseRouteTags(route.query.tags ?? route.query.tag)
  currentPage.value = typeof route.query.page === 'string' ? Number(route.query.page) || 0 : 0

  await loadAvailableTags()
  await doSearch()
})
</script>
