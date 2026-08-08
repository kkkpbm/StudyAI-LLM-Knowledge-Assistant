import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const email = ref(localStorage.getItem('email') || '')
  const createdAt = ref(localStorage.getItem('createdAt') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')

  async function login(form: { username: string; password: string }) {
    const res: any = await loginApi(form)
    const data = res.data
    token.value = data.token
    username.value = data.username
    userId.value = data.userId
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('userId', data.userId)
    router.push('/dashboard')
  }

  function setProfile(data: { email?: string; createdAt?: string; avatar?: string }) {
    if (data.email !== undefined) {
      email.value = data.email
      localStorage.setItem('email', data.email)
    }
    if (data.createdAt !== undefined) {
      createdAt.value = data.createdAt
      localStorage.setItem('createdAt', data.createdAt)
    }
    if (data.avatar !== undefined) {
      avatar.value = data.avatar
      if (data.avatar) {
        localStorage.setItem('avatar', data.avatar)
      } else {
        localStorage.removeItem('avatar')
      }
    }
  }

  function logout() {
    token.value = ''
    username.value = ''
    userId.value = ''
    email.value = ''
    createdAt.value = ''
    avatar.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('userId')
    localStorage.removeItem('email')
    localStorage.removeItem('createdAt')
    localStorage.removeItem('avatar')
  }

  return { token, username, userId, email, createdAt, avatar, login, setProfile, logout }
})
