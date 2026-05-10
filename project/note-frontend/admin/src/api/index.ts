import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'
import { useAdminStore } from '@/store/admin'

const request = axios.create({
  baseURL: '/api/admin',
  timeout: 15000,
})

function handleTokenExpired(message: string) {
  ElMessageBox.alert(
    '您的登录已过期，请重新登录。如果正在编辑内容，请先复制保存。',
    '登录过期提醒',
    { confirmButtonText: '去登录', type: 'warning', showClose: false }
  ).then(() => {
    const store = useAdminStore()
    store.clearAuth()
    router.push('/login')
  })
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 1007 || res.code === 1008) {
        handleTokenExpired(res.message || '登录已过期')
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      handleTokenExpired('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

// ========== 在线时长监控 ==========
const LEVEL_18H_MS = 18 * 60 * 60 * 1000
const LEVEL_23H50_MS = (23 * 60 + 50) * 60 * 1000
const LEVEL_24H_MS = 24 * 60 * 60 * 1000

let level18Shown = false
let level2350Shown = false
let dialogShowing = false
let monitorTimer: ReturnType<typeof setInterval> | null = null

function formatDuration(ms: number): string {
  const totalMinutes = Math.floor(ms / 60000)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (minutes === 0) return `${hours}小时`
  return `${hours}小时${minutes}分钟`
}

function checkOnlineTime() {
  const adminStore = useAdminStore()
  if (!adminStore.token || !adminStore.loginTime) return
  if (dialogShowing) return

  const elapsed = Date.now() - adminStore.loginTime

  // 24小时: 只有"退出登录"，不可关闭，反复弹
  if (elapsed >= LEVEL_24H_MS) {
    dialogShowing = true
    ElMessageBox.alert(
      `您已连续在线 ${formatDuration(elapsed)}，请放松一下吧！`,
      '休息提醒',
      { confirmButtonText: '退出登录', type: 'warning', showClose: false, closeOnClickModal: false, closeOnPressEscape: false }
    ).then(() => {
      dialogShowing = false
      adminStore.clearAuth()
      router.push('/login')
    })
    return
  }

  // 23小时50分: "关闭" + "退出登录"
  if (elapsed >= LEVEL_23H50_MS && !level2350Shown) {
    level2350Shown = true
    dialogShowing = true
    ElMessageBox.alert(
      `您已连续在线 ${formatDuration(elapsed)}，建议您适时休息。`,
      '休息提醒',
      { confirmButtonText: '退出登录', cancelButtonText: '关闭', showCancelButton: true, type: 'warning' }
    ).then(() => {
      dialogShowing = false
      adminStore.clearAuth()
      router.push('/login')
    }).catch(() => {
      dialogShowing = false
    })
    return
  }

  // 18小时: "关闭" + "退出登录"
  if (elapsed >= LEVEL_18H_MS && !level18Shown) {
    level18Shown = true
    dialogShowing = true
    ElMessageBox.alert(
      `您已连续在线 ${formatDuration(elapsed)}，请您休息一下吧！`,
      '休息提醒',
      { confirmButtonText: '退出登录', cancelButtonText: '关闭', showCancelButton: true, type: 'info' }
    ).then(() => {
      dialogShowing = false
      adminStore.clearAuth()
      router.push('/login')
    }).catch(() => {
      dialogShowing = false
    })
  }
}

export function startAdminTokenMonitor() {
  if (monitorTimer) return
  checkOnlineTime()
  monitorTimer = setInterval(checkOnlineTime, 60000)
}

export function stopAdminTokenMonitor() {
  if (monitorTimer) {
    clearInterval(monitorTimer)
    monitorTimer = null
  }
  level18Shown = false
  level2350Shown = false
  dialogShowing = false
}

export default request
