import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useModulesStore } from '../stores/modules'
import LoginView from '../views/LoginView.vue'

const routes = [
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: () => import('../views/DashboardView.vue') },
  { path: '/slip', meta: { module: 'member-portal' }, component: () => import('../views/MySlipView.vue') },
  { path: '/boats', meta: { module: 'member-portal' }, component: () => import('../views/MyBoatsView.vue') },
  { path: '/queue', meta: { module: 'member-portal' }, component: () => import('../views/MyQueueView.vue') },
  { path: '/profile', meta: { module: 'member-portal' }, component: () => import('../views/ProfileView.vue') },
  { path: '/haul-out', meta: { module: 'storage' }, component: () => import('../views/HaulOutView.vue') },
  { path: '/rentals', meta: { module: 'rentals' }, component: () => import('../views/RentalsView.vue') },
]

const router = createRouter({
  history: createWebHistory('/member/'),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    await auth.checkAuth()
    if (!auth.isLoggedIn) return '/login'
  }
  if (!to.meta.public && (auth.role === 'ADMIN' || auth.role === 'HARBOUR_MASTER')) {
    window.location.href = '/admin'
    return false
  }
  if (!to.meta.public) {
    const modules = useModulesStore()
    if (!modules.loaded) await modules.load()
    if (to.meta.module && !modules.isAccessible(to.meta.module)) {
      modules.notice = 'Funktionen är inte aktiverad eller saknar behörighet för din roll.'
      return '/dashboard'
    }
  }
})

export default router
