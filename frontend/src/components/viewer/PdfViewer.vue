<template>
  <div class="flex flex-col h-full bg-slate-700">
    <div class="flex items-center justify-between px-4 py-2 bg-slate-800 text-white">
      <div class="flex items-center gap-2">
        <button @click="prevPage" :disabled="currentPage <= 1" class="p-1 hover:bg-slate-700 rounded disabled:opacity-30">
          <ChevronLeft class="w-4 h-4" />
        </button>
        <span class="text-sm">{{ currentPage }} / {{ totalPages }}</span>
        <button @click="nextPage" :disabled="currentPage >= totalPages" class="p-1 hover:bg-slate-700 rounded disabled:opacity-30">
          <ChevronRight class="w-4 h-4" />
        </button>
        <div class="flex items-center gap-1 ml-3">
          <input
            v-model="pageInput"
            type="number"
            min="1"
            :max="Math.max(totalPages, 1)"
            class="w-16 rounded bg-slate-900 px-2 py-1 text-xs text-white border border-slate-600"
            @keyup.enter="goToSelectedPage"
          />
          <button
            @click="goToSelectedPage"
            :disabled="!totalPages"
            class="rounded bg-slate-600 px-2 py-1 text-xs hover:bg-slate-500 disabled:opacity-40"
          >
            이동
          </button>
          <button
            @click="captureSelectedPage"
            :disabled="!totalPages || loading"
            class="rounded bg-emerald-600 px-2 py-1 text-xs hover:bg-emerald-500 disabled:opacity-40"
          >
            페이지 캡처
          </button>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <button @click="zoomOut" class="p-1 hover:bg-slate-700 rounded">
          <ZoomOut class="w-4 h-4" />
        </button>
        <span class="text-xs w-12 text-center">{{ Math.round(scale * 100) }}%</span>
        <button @click="zoomIn" class="p-1 hover:bg-slate-700 rounded">
          <ZoomIn class="w-4 h-4" />
        </button>
      </div>
    </div>

    <div class="flex-1 overflow-auto flex justify-center p-4">
      <div v-if="loading" class="flex items-center justify-center w-full">
        <Loader2 class="w-8 h-8 animate-spin text-white" />
      </div>
      <div v-else-if="errorMessage" class="flex items-center justify-center w-full">
        <p class="text-sm text-white/80">{{ errorMessage }}</p>
      </div>
      <canvas v-else ref="canvasRef" class="shadow-2xl" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, ZoomIn, ZoomOut, Loader2 } from 'lucide-vue-next'
import { api } from '@/lib/api'

const props = defineProps<{
  documentId: number
  documentTitle?: string
}>()

const canvasRef = ref<HTMLCanvasElement>()
const currentPage = ref(1)
const totalPages = ref(0)
const scale = ref(1.2)
const loading = ref(true)
const errorMessage = ref('')
const pageInput = ref('1')

let pdfDoc: any = null

async function loadPdf() {
  if (!props.documentId || Number.isNaN(props.documentId)) return
  loading.value = true
  errorMessage.value = ''
  try {
    const pdfjsLib = await import('pdfjs-dist')
    pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
      'pdfjs-dist/build/pdf.worker.mjs',
      import.meta.url
    ).toString()

    const { data } = await api.get<ArrayBuffer>(`/viewer/${props.documentId}/stream`, {
      responseType: 'arraybuffer',
    })
    pdfDoc = await pdfjsLib.getDocument({ data }).promise
    totalPages.value = pdfDoc.numPages
    currentPage.value = 1
    pageInput.value = '1'
  } catch (error: any) {
    pdfDoc = null
    totalPages.value = 0
    currentPage.value = 1
    pageInput.value = '1'
    errorMessage.value = error?.response?.status === 404
      ? 'PDF 파일을 찾을 수 없습니다.'
      : 'PDF를 불러오지 못했습니다.'
    console.error('Failed to load PDF', error)
  } finally {
    loading.value = false
  }

  if (pdfDoc) {
    await nextTick()
    await renderPage(1)
  }
}

async function renderPage(num: number) {
  if (!pdfDoc || !canvasRef.value) return
  const targetPage = normalizePageNumber(num)
  const page = await pdfDoc.getPage(targetPage)
  const viewport = page.getViewport({ scale: scale.value })
  const canvas = canvasRef.value
  canvas.width = viewport.width
  canvas.height = viewport.height
  const ctx = canvas.getContext('2d')!
  await page.render({ canvasContext: ctx, viewport }).promise
  currentPage.value = targetPage
  pageInput.value = String(targetPage)
}

function normalizePageNumber(value: number) {
  if (!totalPages.value) return 1
  const parsed = Number.isFinite(value) ? Math.trunc(value) : 1
  return Math.min(Math.max(parsed, 1), totalPages.value)
}

async function goToSelectedPage() {
  if (!totalPages.value) return
  await renderPage(normalizePageNumber(Number(pageInput.value)))
}

async function captureSelectedPage() {
  if (!canvasRef.value || !pdfDoc) return
  await goToSelectedPage()
  const canvas = canvasRef.value
  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/png'))
  if (!blob) {
    errorMessage.value = '페이지 이미지를 저장하지 못했습니다.'
    return
  }

  const fileName = buildCaptureFileName(currentPage.value)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

function buildCaptureFileName(page: number) {
  const safeTitle = (props.documentTitle || `document-${props.documentId}`)
    .replace(/[\\/:*?"<>|]/g, '-')
    .replace(/\s+/g, '-')
    .toLowerCase()
  return `${safeTitle}-page-${page}.png`
}

function prevPage() { if (currentPage.value > 1) renderPage(currentPage.value - 1) }
function nextPage() { if (currentPage.value < totalPages.value) renderPage(currentPage.value + 1) }
function zoomIn() { scale.value = Math.min(scale.value + 0.2, 3.0); renderPage(currentPage.value) }
function zoomOut() { scale.value = Math.max(scale.value - 0.2, 0.5); renderPage(currentPage.value) }

onMounted(loadPdf)
watch(() => props.documentId, loadPdf)
</script>
