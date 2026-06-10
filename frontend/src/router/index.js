import { createRouter, createWebHistory } from 'vue-router'
import ReimbursementList from '../views/ReimbursementList.vue'
import ReimbursementForm from '../views/ReimbursementForm.vue'
import PersonalStats from '../views/PersonalStats.vue'

const routes = [
  { path: '/', redirect: '/reimbursements' },
  { path: '/reimbursements', component: ReimbursementList },
  { path: '/reimbursements/new', component: ReimbursementForm },
  { path: '/reimbursements/:reimNo', component: ReimbursementForm, props: true },
  { path: '/statistics/personal', component: PersonalStats }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
