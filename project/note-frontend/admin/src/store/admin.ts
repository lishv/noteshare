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

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('admin_token', t)
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
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_info')
  }

  function isSuperAdmin() {
    return adminInfo.value?.role === 1
  }

  // 初始化时从 localStorage 恢复
  const savedInfo = localStorage.getItem('admin_info')
  if (savedInfo) {
    try { adminInfo.value = JSON.parse(savedInfo) } catch {}
  }

  return { token, adminInfo, setToken, setAdminInfo, fetchProfile, clearAuth, isSuperAdmin }
})
