import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useModulesStore } from '../stores/modules'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'

const routes = [
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: DashboardView },
  { path: '/persons', component: () => import('../views/PersonsView.vue') },
  { path: '/boats', component: () => import('../views/BoatsView.vue') },
  { path: '/docks', component: () => import('../views/DocksView.vue') },
  { path: '/docks/:id/layout', component: () => import('../views/DockLayoutView.vue') },
  { path: '/assignments', component: () => import('../views/AssignmentsView.vue') },
  { path: '/queue', component: () => import('../views/QueueView.vue') },
  { path: '/mail', meta: { module: 'ai-mail' }, component: () => import('../views/MailView.vue') },
  { path: '/tariffs', component: () => import('../views/TariffsView.vue') },
  { path: '/import-export', meta: { module: 'import-export' }, component: () => import('../views/ImportExportView.vue') },
  { path: '/audit', meta: { module: 'audit' }, component: () => import('../views/AuditView.vue') },
  { path: '/winter', meta: { module: 'storage' }, component: () => import('../views/WinterSeasonsView.vue') },
  { path: '/winter/yard/:seasonId', meta: { module: 'storage' }, component: () => import('../views/StorageYardView.vue') },
]

const router = createRouter({
  history: createWebHistory('/admin'),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    await auth.checkAuth()
    if (!auth.isLoggedIn) return '/login'
  }
  if (!to.meta.public && auth.role === 'MEMBER') {
    window.location.href = '/member/'
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
