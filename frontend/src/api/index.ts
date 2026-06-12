import axios from 'axios'
import type { ApiResponse } from '@/types/api'
import { storage } from '@/utils/storage'
import router from '@/router'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

http.interceptors.request.use(config => {
  const token = storage.get('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  response => response.data as ApiResponse<any>,
  async error => {
    if (error.response?.status === 401) {
      const refreshToken = storage.get('refreshToken')
      if (refreshToken) {
        try {
          const res = await axios.post('/api/auth/refresh', { refreshToken })
          storage.set('token', res.data.data.token)
          storage.set('refreshToken', res.data.data.refreshToken)
          error.config.headers.Authorization = `Bearer ${res.data.data.token}`
          return http(error.config)
        } catch {
          storage.remove('token'); storage.remove('refreshToken'); storage.remove('userInfo')
          router.push('/login')
        }
      } else {
        storage.remove('token'); storage.remove('refreshToken'); storage.remove('userInfo')
        router.push('/login')
      }
    }
    return Promise.reject(error)
  }
)

export default http
