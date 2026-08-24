import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'

const routes = [
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: () => import('../views/DashboardView.vue') },
  { path: '/slip', component: () => import('../views/MySlipView.vue') },
  { path: '/boats', component: () => import('../views/MyBoatsView.vue') },
  { path: '/queue', component: () => import('../views/MyQueueView.vue') },
  { path: '/profile', component: () => import('../views/ProfileView.vue') },
]

const router = createRouter({
  history: createWebHistory('/'),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) return '/login'
  if (!to.meta.public && (auth.role === 'ADMIN' || auth.role === 'HARBOUR_MASTER')) {
    window.location.href = '/admin'
    return false
  }
})

export default router
