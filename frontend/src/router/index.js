import { createRouter, createWebHistory } from 'vue-router'
import ReimbursementList from '../views/ReimbursementList.vue'
import ReimbursementForm from '../views/ReimbursementForm.vue'
import PersonalStats from '../views/PersonalStats.vue'

// 路由区分列表、新建、详情/编辑和个人统计四类页面。
const routes = [
  { path: '/', redirect: '/reimbursements' },
  { path: '/reimbursements', component: ReimbursementList },
  { path: '/reimbursements/new', component: ReimbursementForm },
  { path: '/reimbursements/:reimNo', component: ReimbursementForm, props: true },
  { path: '/statistics/personal', component: PersonalStats }
]

export default createRouter({
  // 使用 HTML5 History，生产部署时服务器需要把未知路径回退到 index.html。
  history: createWebHistory(),
  routes
})
