<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-slate-100">
    <div class="card p-8 w-full max-w-md">
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-12 h-12 bg-primary-600 rounded-xl mb-4">
          <FileText class="w-6 h-6 text-white" />
        </div>
        <h1 class="text-2xl font-bold text-slate-800">PaperLens AI</h1>
        <p class="text-sm text-slate-500 mt-1">AI 문서 지식 플랫폼</p>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">이메일</label>
          <input
            v-model="form.email"
            type="email"
            class="input"
            placeholder="admin@paperlens.com"
            required
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">비밀번호</label>
          <input
            v-model="form.password"
            type="password"
            class="input"
            placeholder="••••••••"
            required
          />
        </div>

        <div v-if="error" class="text-sm text-red-600 bg-red-50 px-3 py-2 rounded-lg">
          {{ error }}
        </div>

        <button type="submit" class="btn-primary w-full justify-center" :disabled="loading">
          <Loader2 v-if="loading" class="w-4 h-4 animate-spin" />
          {{ loading ? '로그인 중...' : '로그인' }}
        </button>
      </form>

      <div class="mt-6 p-3 bg-surface-50 rounded-lg text-xs text-slate-500">
        <p class="font-medium mb-1">데모 계정</p>
        <p>admin@paperlens.com / admin123</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { FileText, Loader2 } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = ref({ email: 'admin@paperlens.com', password: 'admin123' })
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    await auth.login(form.value.email, form.value.password)
    router.push('/')
  } catch {
    error.value = '이메일 또는 비밀번호가 올바르지 않습니다.'
  } finally {
    loading.value = false
  }
}
</script>
