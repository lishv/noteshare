import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import { getCurrentUser } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<User | null>(null)
  const loginTime = ref<number>(localStorage.getItem('login_time') ? Number(localStorage.getItem('login_time')) : 0)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
    loginTime.value = Date.now()
    localStorage.setItem('login_time', String(loginTime.value))
  }

  function setUserInfo(user: User) {
    userInfo.value = user
  }

  async function fetchUserInfo() {
    try {
      const res = await getCurrentUser()
      userInfo.value = res.data
      return res.data
    } catch {
      return null
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    loginTime.value = 0
    localStorage.removeItem('token')
    localStorage.removeItem('login_time')
  }

  return {
    token,
    userInfo,
    loginTime,
    isLoggedIn,
    setToken,
    setUserInfo,
    fetchUserInfo,
    logout,
  }
})
