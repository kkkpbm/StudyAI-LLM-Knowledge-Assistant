import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    return res.data
  },
  (err) => {
    if (err.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
    } else {
      ElMessage.error(err.response?.data?.message || 'Request failed')
    }
    return Promise.reject(err)
  }
)

export default http
