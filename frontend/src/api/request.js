import axios from 'axios'
import { ElMessage } from 'element-plus'

// 全站共用 Axios 实例。开发环境由 Vite 代理把 /api 转发到 Spring Boot。
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 后端统一返回 { code, message, data }，页面只接收真正的 data。
request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code !== 200) {
      // HTTP 可能仍是 200，因此还必须检查响应体中的业务 code。
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body?.data
  },
  (error) => {
    // 网络超时、代理失败或非 2xx HTTP 状态进入此分支。
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
