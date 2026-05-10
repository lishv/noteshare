import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getProfile } from '@/api/admin'

interface AdminInfo {
  id: number
  username: string
  email: string
  phone: string
  avatar: string
  role: number
  lastLoginAt: string
  createdAt: string
}

export const useAdminStore = defineStore('admin', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const adminInfo = ref<AdminInfo | null>(null)
  const loginTime = ref<number>(localStorage.getItem('admin_login_time') ? Number(localStorage.getItem('admin_login_time')) : 0)

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('admin_token', t)
    loginTime.value = Date.now()
    localStorage.setItem('admin_login_time', String(loginTime.value))
  }

  function setAdminInfo(info: AdminInfo) {
    adminInfo.value = info
    localStorage.setItem('admin_info', JSON.stringify(info))
  }

  async function fetchProfile() {
    const res = await getProfile()
    setAdminInfo(res.data)
  }

  function clearAuth() {
    token.value = ''
    adminInfo.value = null
    loginTime.value = 0
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_info')
    localStorage.removeItem('admin_login_time')
  }

  function isSuperAdmin() {
    return adminInfo.value?.role === 1
  }

  // 初始化时从 localStorage 恢复
  const savedInfo = localStorage.getItem('admin_info')
  if (savedInfo) {
    try { adminInfo.value = JSON.parse(savedInfo) } catch {}
  }

  return { token, adminInfo, loginTime, setToken, setAdminInfo, fetchProfile, clearAuth, isSuperAdmin }
})
