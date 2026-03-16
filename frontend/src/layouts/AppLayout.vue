<template>
  <div class="min-h-screen flex flex-col">
    <header class="bg-white border-b border-slate-200 sticky top-0 z-40">
      <div class="max-w-7xl mx-auto px-4 h-14 flex items-center justify-between">
        <RouterLink to="/" class="flex items-center gap-2 font-bold text-lg text-primary-600">
          <FileText class="w-5 h-5" />
          PaperLens AI
        </RouterLink>

        <nav class="flex items-center gap-1">
          <RouterLink
            to="/"
            class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
            :class="$route.name === 'documents' ? 'bg-primary-50 text-primary-700' : 'text-slate-600 hover:bg-surface-100'"
          >
            문서
          </RouterLink>
          <RouterLink
            v-if="auth.isAdmin"
            to="/admin"
            class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
            :class="$route.name === 'admin' ? 'bg-primary-50 text-primary-700' : 'text-slate-600 hover:bg-surface-100'"
          >
            관리자
          </RouterLink>
        </nav>

        <div class="flex items-center gap-3">
          <span class="text-sm text-slate-600">{{ auth.user?.name }}</span>
          <button @click="handleLogout" class="btn-secondary text-xs px-3 py-1.5">
            로그아웃
          </button>
        </div>
      </div>
    </header>

    <main class="flex-1">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { FileText } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>
