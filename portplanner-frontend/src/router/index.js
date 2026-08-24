import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
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
  { path: '/mail', component: () => import('../views/MailView.vue') },
  { path: '/tariffs', component: () => import('../views/TariffsView.vue') },
  { path: '/import-export', component: () => import('../views/ImportExportView.vue') },
  { path: '/audit', component: () => import('../views/AuditView.vue') },
]

const router = createRouter({
  history: createWebHistory('/admin'),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) return '/login'
  if (!to.meta.public && auth.role === 'MEMBER') {
    window.location.href = '/'
    return false
  }
})

export default router
