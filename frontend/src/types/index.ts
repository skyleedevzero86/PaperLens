export interface User {
  email: string
  name: string
  role: 'USER' | 'ADMIN'
}

export interface Document {
  id: number
  title: string
  description: string | null
  originalFileName: string
  pageCount: number
  fileSize: number
  documentType: string | null
  summaryShort: string | null
  status: 'PENDING' | 'PROCESSING' | 'INDEXED' | 'FAILED'
  tags: string[]
  createdAt: string
}

export interface DocumentDetail extends Document {
  summaryLong: string | null
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface SearchResult {
  documentId: number
  title: string
  summaryShort: string | null
  documentType: string | null
  score: number
  highlights: string[]
}

export interface SearchResponse {
  results: SearchResult[]
  total: number
  query: string
  mode: string
}

export interface QaResponse {
  answer: string
  sources: ChunkSource[]
}

export interface ChunkSource {
  chunkId: number
  pageFrom: number
  pageTo: number
  excerpt: string
}

export interface SimilarDocument {
  documentId: number
  title: string
  summaryShort: string | null
  similarity: number
}

export interface AdminStats {
  totalDocuments: number
  indexedDocuments: number
  pendingDocuments: number
  failedDocuments: number
  indexingRate: number
  totalQueries: number
  avgLatencyMs: number
}
