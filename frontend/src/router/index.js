import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import ReimbursementList from '../views/ReimbursementList.vue'
import ReimbursementForm from '../views/ReimbursementForm.vue'
import PersonalStats from '../views/PersonalStats.vue'
import Login from '../views/Login.vue'

const routes = [
  { path: '/', redirect: '/reimbursements' },
  { path: '/login', component: Login, meta: { guest: true } },
  { path: '/reimbursements', component: ReimbursementList, meta: { requiresAuth: true } },
  { path: '/reimbursements/new', component: ReimbursementForm, meta: { requiresAuth: true } },
  { path: '/reimbursements/:reimNo', component: ReimbursementForm, props: true, meta: { requiresAuth: true } },
  { path: '/statistics/personal', component: PersonalStats, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 首次进入时需要检查登录状态（Pinia 状态不持久化，刷新丢失）
  if (!authStore.isLoggedIn) {
    await authStore.checkAuth()
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    // 需要登录但未登录 → 跳转登录页
    next('/login')
  } else if (to.meta.guest && authStore.isLoggedIn) {
    // 已登录访问登录页 → 跳转首页
    next('/')
  } else {
    next()
  }
})

export default router
