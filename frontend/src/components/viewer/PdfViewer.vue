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
      <canvas v-else ref="canvasRef" class="shadow-2xl" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ChevronLeft, ChevronRight, ZoomIn, ZoomOut, Loader2 } from 'lucide-vue-next'

const props = defineProps<{ documentId: number }>()

const canvasRef = ref<HTMLCanvasElement>()
const currentPage = ref(1)
const totalPages = ref(0)
const scale = ref(1.2)
const loading = ref(true)

let pdfDoc: any = null

async function loadPdf() {
  loading.value = true
  try {
    const pdfjsLib = await import('pdfjs-dist')
    pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
      'pdfjs-dist/build/pdf.worker.mjs',
      import.meta.url
    ).toString()

    const url = `/api/viewer/${props.documentId}/stream`
    pdfDoc = await pdfjsLib.getDocument(url).promise
    totalPages.value = pdfDoc.numPages
    await renderPage(1)
  } finally {
    loading.value = false
  }
}

async function renderPage(num: number) {
  if (!pdfDoc || !canvasRef.value) return
  const page = await pdfDoc.getPage(num)
  const viewport = page.getViewport({ scale: scale.value })
  const canvas = canvasRef.value
  canvas.width = viewport.width
  canvas.height = viewport.height
  const ctx = canvas.getContext('2d')!
  await page.render({ canvasContext: ctx, viewport }).promise
  currentPage.value = num
}

function prevPage() { if (currentPage.value > 1) renderPage(currentPage.value - 1) }
function nextPage() { if (currentPage.value < totalPages.value) renderPage(currentPage.value + 1) }
function zoomIn() { scale.value = Math.min(scale.value + 0.2, 3.0); renderPage(currentPage.value) }
function zoomOut() { scale.value = Math.max(scale.value - 0.2, 0.5); renderPage(currentPage.value) }

onMounted(loadPdf)
watch(() => props.documentId, loadPdf)
</script>
