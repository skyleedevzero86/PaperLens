import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/lib/api'
import type { Document, DocumentDetail, PageResponse } from '@/types'

export const useDocumentStore = defineStore('document', () => {
  const documents = ref<Document[]>([])
  const currentDocument = ref<DocumentDetail | null>(null)
  const loading = ref(false)
  const total = ref(0)
  const totalPages = ref(0)

  async function fetchDocuments(params: {
    keyword?: string
    docType?: string
    page?: number
    size?: number
  } = {}) {
    loading.value = true
    try {
      const res = await api.get<PageResponse<Document>>('/documents', { params })
      documents.value = res.data.content
      total.value = res.data.totalElements
      totalPages.value = res.data.totalPages
    } finally {
      loading.value = false
    }
  }

  async function fetchDocument(id: number) {
    loading.value = true
    try {
      const res = await api.get<DocumentDetail>(`/documents/${id}`)
      currentDocument.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function uploadDocument(formData: FormData) {
    const res = await api.post<Document>('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    await fetchDocuments()
    return res.data
  }

  async function deleteDocument(id: number) {
    await api.delete(`/documents/${id}`)
    documents.value = documents.value.filter(d => d.id !== id)
  }

  async function updateDocument(id: number, data: { title?: string; description?: string; tags?: string[] }) {
    const res = await api.patch<DocumentDetail>(`/documents/${id}`, data)
    currentDocument.value = res.data
    return res.data
  }

  return {
    documents, currentDocument, loading, total, totalPages,
    fetchDocuments, fetchDocument, uploadDocument, deleteDocument, updateDocument
  }
})
