import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      children: [
        {
          path: '',
          name: 'documents',
          component: () => import('@/views/DocumentListView.vue')
        },
        {
          path: 'documents/:id',
          name: 'document-detail',
          component: () => import('@/views/DocumentDetailView.vue')
        },
        {
          path: 'admin',
          name: 'admin',
          component: () => import('@/views/AdminView.vue'),
          meta: { adminOnly: true }
        }
      ]
    },
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.token) {
    return { name: 'login' }
  }
  if (to.meta.adminOnly && auth.user?.role !== 'ADMIN') {
    return { name: 'documents' }
  }
})

export default router
