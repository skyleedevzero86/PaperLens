<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="$emit('close')">
    <div class="card w-full max-w-lg p-6 mx-4">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-lg font-bold text-slate-800">PDF 업로드</h2>
        <button @click="$emit('close')" class="p-1 hover:bg-surface-100 rounded">
          <X class="w-5 h-5 text-slate-400" />
        </button>
      </div>

      <form @submit.prevent="handleUpload" class="space-y-4">
        <div
          @dragover.prevent
          @drop.prevent="handleDrop"
          @click="fileInput?.click()"
          :class="[
            'border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-colors',
            file ? 'border-primary-300 bg-primary-50' : 'border-slate-200 hover:border-primary-300 hover:bg-surface-50'
          ]"
        >
          <input ref="fileInput" type="file" accept=".pdf" class="hidden" @change="handleFileChange" />
          <FileText class="w-10 h-10 text-slate-300 mx-auto mb-2" />
          <p v-if="file" class="text-sm font-medium text-primary-700">{{ file.name }}</p>
          <p v-else class="text-sm text-slate-500">
            PDF 파일을 드래그하거나 클릭하여 선택
          </p>
          <p class="text-xs text-slate-400 mt-1">최대 100MB</p>
        </div>

        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">제목 <span class="text-red-500">*</span></label>
          <input v-model="title" type="text" class="input" placeholder="문서 제목" required />
        </div>

        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">설명</label>
          <textarea v-model="description" class="input resize-none" rows="2" placeholder="문서 설명 (선택)" />
        </div>

        <div v-if="uploading" class="space-y-1">
          <div class="flex justify-between text-xs text-slate-500">
            <span>업로드 중...</span>
            <span>{{ progress }}%</span>
          </div>
          <div class="h-1.5 bg-surface-200 rounded-full overflow-hidden">
            <div class="h-full bg-primary-500 transition-all" :style="{ width: `${progress}%` }" />
          </div>
        </div>

        <div v-if="error" class="text-sm text-red-600 bg-red-50 px-3 py-2 rounded-lg">{{ error }}</div>

        <div class="flex gap-3 justify-end">
          <button type="button" @click="$emit('close')" class="btn-secondary">취소</button>
          <button type="submit" class="btn-primary" :disabled="!file || !title || uploading">
            <Loader2 v-if="uploading" class="w-4 h-4 animate-spin" />
            {{ uploading ? '업로드 중...' : '업로드' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { FileText, X, Loader2 } from 'lucide-vue-next'
import { useDocumentStore } from '@/stores/document'

const emit = defineEmits<{ close: []; success: [] }>()
const store = useDocumentStore()

const fileInput = ref<HTMLInputElement>()
const file = ref<File | null>(null)
const title = ref('')
const description = ref('')
const uploading = ref(false)
const progress = ref(0)
const error = ref('')

function handleFileChange(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (f) { file.value = f; if (!title.value) title.value = f.name.replace('.pdf', '') }
}

function handleDrop(e: DragEvent) {
  const f = e.dataTransfer?.files[0]
  if (f?.type === 'application/pdf') { file.value = f; if (!title.value) title.value = f.name.replace('.pdf', '') }
}

async function handleUpload() {
  if (!file.value) return
  uploading.value = true
  error.value = ''
  progress.value = 10

  try {
    const formData = new FormData()
    formData.append('file', file.value)
    formData.append('title', title.value)
    if (description.value) formData.append('description', description.value)

    progress.value = 50
    await store.uploadDocument(formData)
    progress.value = 100
    emit('success')
  } catch {
    error.value = '업로드에 실패했습니다. 다시 시도해주세요.'
  } finally {
    uploading.value = false
  }
}
</script>
