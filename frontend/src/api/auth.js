import request from './request'

// 认证相关 API 封装，风格对齐 reimbursement.js。

/** 登录 */
export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

/** 登出 */
export function logout() {
  return request.post('/auth/logout')
}

/** 获取当前登录用户信息 */
export function getCurrentUser() {
  return request.get('/auth/current-user')
}
