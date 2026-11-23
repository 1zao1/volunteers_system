import axios from 'axios'
import { useAuthStore } from '../store/auth'
import router from '../router'

const request = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  res => {
    const body = res.data
    // 后端统一返回 { code, message, data }
    if (body && typeof body.code === 'number' && body.code !== 200) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body ? body.data : body
  },
  err => {
    const status = err.response?.status
    const msg = err.response?.data?.message || err.message || '网络异常，请稍后重试'
    if (status === 401) {
      useAuthStore().logout()
      // 公开页面（活动广场/详情）不强制跳登录，游客可继续浏览
      if (!router.currentRoute.value.meta?.public) router.push('/login')
    }
    return Promise.reject(new Error(msg))
  }
)

export default request
