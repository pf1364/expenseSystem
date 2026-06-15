import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'

/**
 * 认证状态管理。
 *
 * 使用 Composition API（setup 写法），
 * 存储当前用户信息和登录状态，暴露 login/logout/checkAuth 三个 action。
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)        // { username, displayName } | null
  const isLoggedIn = ref(false)

  /** 登录并更新状态 */
  async function login(username, password) {
    const data = await loginApi(username, password)
    user.value = data
    isLoggedIn.value = true
  }

  /** 登出并清除状态 */
  async function logout() {
    try {
      await logoutApi()
    } finally {
      user.value = null
      isLoggedIn.value = false
    }
  }

  /** 应用启动/刷新时检查登录状态 */
  async function checkAuth() {
    try {
      const data = await getCurrentUser()
      if (data) {
        user.value = data
        isLoggedIn.value = true
      } else {
        user.value = null
        isLoggedIn.value = false
      }
    } catch {
      user.value = null
      isLoggedIn.value = false
    }
  }

  return { user, isLoggedIn, login, logout, checkAuth }
})
