<template>
  <div class="flex flex-col h-full">
    <div ref="messagesEl" class="flex-1 overflow-y-auto p-4 space-y-4">
      <div v-if="messages.length === 0" class="text-center py-8">
        <MessageSquare class="w-10 h-10 text-slate-200 mx-auto mb-2" />
        <p class="text-sm text-slate-400">이 문서에 대해 질문해보세요</p>
        <div class="mt-4 space-y-1">
          <button
            v-for="q in suggestedQuestions"
            :key="q"
            @click="sendQuestion(q)"
            class="block w-full text-left text-xs px-3 py-2 bg-surface-50 hover:bg-primary-50 text-slate-600 hover:text-primary-700 rounded-lg transition-colors"
          >
            {{ q }}
          </button>
        </div>
      </div>

      <div v-for="(msg, i) in messages" :key="i">
        <div class="flex justify-end">
          <div class="max-w-[80%] px-3 py-2 bg-primary-600 text-white rounded-xl rounded-tr-sm text-sm">
            {{ msg.question }}
          </div>
        </div>
        <div class="flex justify-start mt-2">
          <div class="max-w-[90%] space-y-2">
            <div class="px-3 py-2 bg-surface-100 text-slate-700 rounded-xl rounded-tl-sm text-sm leading-relaxed">
              <Loader2 v-if="msg.loading" class="w-4 h-4 animate-spin text-slate-400" />
              <span v-else>{{ msg.answer }}</span>
            </div>
            <div v-if="msg.sources && msg.sources.length > 0" class="px-3">
              <p class="text-xs text-slate-400 mb-1">출처</p>
              <div class="flex flex-wrap gap-1">
                <span
                  v-for="src in msg.sources"
                  :key="src.chunkId"
                  class="px-2 py-0.5 bg-white border border-slate-200 text-slate-500 rounded text-xs"
                >
                  p.{{ src.pageFrom }}-{{ src.pageTo }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="p-4 border-t border-slate-100">
      <div class="flex gap-2">
        <input
          v-model="input"
          @keydown.enter.prevent="sendQuestion(input)"
          type="text"
          class="input flex-1 text-sm"
          placeholder="질문을 입력하세요..."
          :disabled="isLoading"
        />
        <button
          @click="sendQuestion(input)"
          :disabled="!input.trim() || isLoading"
          class="btn-primary px-3"
        >
          <Send class="w-4 h-4" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { MessageSquare, Send, Loader2 } from 'lucide-vue-next'
import { api } from '@/lib/api'
import type { QaResponse, ChunkSource } from '@/types'

const props = defineProps<{ documentId: number }>()

interface Message {
  question: string
  answer: string
  loading: boolean
  sources: ChunkSource[]
}

const messages = ref<Message[]>([])
const input = ref('')
const isLoading = ref(false)
const messagesEl = ref<HTMLElement>()

const suggestedQuestions = [
  '이 문서의 핵심 내용을 요약해주세요.',
  '주요 조항이나 규정이 있나요?',
  '중요한 날짜나 기한이 있나요?',
]

async function sendQuestion(question: string) {
  if (!question.trim() || isLoading.value) return
  input.value = ''
  isLoading.value = true

  const msg: Message = { question, answer: '', loading: true, sources: [] }
  messages.value.push(msg)
  await nextTick()
  messagesEl.value?.scrollTo({ top: messagesEl.value.scrollHeight, behavior: 'smooth' })

  try {
    const res = await api.post<QaResponse>('/ai/qa', {
      question,
      documentId: props.documentId
    })
    msg.answer = res.data.answer
    msg.sources = res.data.sources
  } catch {
    msg.answer = '답변을 생성하는 중 오류가 발생했습니다.'
  } finally {
    msg.loading = false
    isLoading.value = false
    await nextTick()
    messagesEl.value?.scrollTo({ top: messagesEl.value.scrollHeight, behavior: 'smooth' })
  }
}
</script>
