import axios from 'axios'
import { ElMessage } from 'element-plus'

// 全站共用 Axios 实例。开发环境由 Vite 代理把 /api 转发到 Spring Boot。
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 后端统一返回 { code, message, data }，页面只接收真正的 data。
// HTTP 2xx 时直接返回 data（后端保证此时 code 必为 200），
// 4xx/5xx 时按状态码展示对应级别的提示。
request.interceptors.response.use(
  (response) => {
    return response.data?.data
  },
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message
    if (status === 400) {
      ElMessage.error(msg || '参数错误')
    } else if (status === 404) {
      ElMessage.error(msg || '资源不存在')
    } else if (status === 409) {
      ElMessage.warning(msg || '数据冲突，请刷新后重试')
    } else {
      ElMessage.error('网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
