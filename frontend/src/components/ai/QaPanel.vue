<template>
  <div class="flex flex-col h-full">
    <div ref="messagesEl" class="flex-1 overflow-y-auto p-4 space-y-4">
      <div v-if="messages.length === 0" class="text-center py-8">
        <MessageSquare class="w-10 h-10 text-slate-200 mx-auto mb-2" />
        <p class="text-sm text-slate-400">이 문서에 대해 질문해보세요</p>
        <div class="mt-4 space-y-1">
          <button
            v-for="question in suggestedQuestions"
            :key="question"
            @click="sendQuestion(question)"
            class="block w-full text-left text-xs px-3 py-2 bg-surface-50 hover:bg-primary-50 text-slate-600 hover:text-primary-700 rounded-lg transition-colors"
          >
            {{ question }}
          </button>
        </div>
      </div>

      <div v-for="(message, index) in messages" :key="index">
        <div class="flex justify-end">
          <div class="max-w-[80%] px-3 py-2 bg-primary-600 text-white rounded-xl rounded-tr-sm text-sm">
            {{ message.question }}
          </div>
        </div>
        <div class="flex justify-start mt-2">
          <div class="max-w-[90%] space-y-2">
            <div class="px-3 py-2 bg-surface-100 text-slate-700 rounded-xl rounded-tl-sm text-sm leading-relaxed">
              <div v-if="message.loading" class="flex items-center gap-2">
                <Loader2 class="w-4 h-4 animate-spin text-slate-400 shrink-0" />
                <span>{{ message.answer || '답변을 준비하는 중입니다.' }}</span>
              </div>
              <span v-else>{{ message.answer }}</span>
            </div>
            <div v-if="message.sources.length > 0" class="px-3">
              <p class="text-xs text-slate-400 mb-1">출처</p>
              <div class="flex flex-wrap gap-1">
                <span
                  v-for="source in message.sources"
                  :key="source.chunkId"
                  class="px-2 py-0.5 bg-white border border-slate-200 text-slate-500 rounded text-xs"
                >
                  p.{{ source.pageFrom }}-{{ source.pageTo }}
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
import { nextTick, onBeforeUnmount, ref } from 'vue'
import { Loader2, MessageSquare, Send } from 'lucide-vue-next'
import { api } from '@/lib/api'
import type { ChunkSource, QaResponse } from '@/types'

const props = defineProps<{ documentId: number }>()

interface Message {
  question: string
  answer: string
  loading: boolean
  sources: ChunkSource[]
}

const MAX_PENDING_RETRIES = 10

const messages = ref<Message[]>([])
const input = ref('')
const isLoading = ref(false)
const messagesEl = ref<HTMLElement>()
let retryTimer: ReturnType<typeof setTimeout> | null = null

const suggestedQuestions = [
  '이 문서의 핵심 내용을 요약해주세요.',
  '중요한 조항이나 규정은 무엇인가요?',
  '중요한 날짜나 기한이 있나요?',
]

async function scrollToBottom() {
  await nextTick()
  messagesEl.value?.scrollTo({ top: messagesEl.value.scrollHeight, behavior: 'smooth' })
}

async function sendQuestion(question: string, attempt = 0, existingMessage?: Message) {
  if (!question.trim() || (isLoading.value && !existingMessage)) return

  if (retryTimer) {
    clearTimeout(retryTimer)
    retryTimer = null
  }

  input.value = ''
  isLoading.value = true

  const message = existingMessage ?? {
    question,
    answer: '',
    loading: true,
    sources: [],
  }

  if (!existingMessage) {
    messages.value.push(message)
    await scrollToBottom()
  } else {
    message.loading = true
  }

  let scheduledRetry = false

  try {
    const response = await api.post<QaResponse>('/ai/qa', {
      question,
      documentId: props.documentId,
    })

    message.answer = response.data.answer
    message.sources = response.data.sources

    if (response.data.pending && attempt < MAX_PENDING_RETRIES) {
      scheduledRetry = true
      retryTimer = setTimeout(() => {
        retryTimer = null
        void sendQuestion(question, attempt + 1, message)
      }, response.data.retryAfterMs ?? 2000)
    }
  } catch (error: any) {
    const serverMessage = error.response?.data?.message
    message.answer = typeof serverMessage === 'string'
      ? serverMessage
      : '답변을 생성하는 중 오류가 발생했습니다.'
  } finally {
    if (!scheduledRetry) {
      message.loading = false
      isLoading.value = false
      await scrollToBottom()
    }
  }
}

onBeforeUnmount(() => {
  if (retryTimer) {
    clearTimeout(retryTimer)
    retryTimer = null
  }
})
</script>
