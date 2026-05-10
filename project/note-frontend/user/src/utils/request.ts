import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ApiResponse } from '@/types'
import { useUserStore } from '@/store/user'
import router from '@/router'

const service: AxiosInstance = axios.create({
  baseURL: '/api/user',
  timeout: 15000,
})

function handleTokenExpired(message: string) {
  const userStore = useUserStore()
  ElMessageBox.alert(
    '您的登录已过期，请重新登录。如果正在编辑内容，请先复制保存。',
    '登录过期提醒',
    { confirmButtonText: '去登录', type: 'warning', showClose: false }
  ).then(() => {
    userStore.logout()
    window.location.href = '/login'
  })
}

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 1007 || res.code === 1008) {
        handleTokenExpired(res.message || '登录已过期')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return response
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 401:
          handleTokenExpired('登录已过期，请重新登录')
          break
        case 403:
          ElMessage.error('无权限访问，请确认已登录')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接异常')
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
  const userStore = useUserStore()
  if (!userStore.token || !userStore.loginTime) return
  if (dialogShowing) return

  const elapsed = Date.now() - userStore.loginTime

  // 24小时: 只有"退出登录"，不可关闭，反复弹
  if (elapsed >= LEVEL_24H_MS) {
    dialogShowing = true
    ElMessageBox.alert(
      `您已连续在线 ${formatDuration(elapsed)}，请放松一下吧！`,
      '休息提醒',
      { confirmButtonText: '退出登录', type: 'warning', showClose: false, closeOnClickModal: false, closeOnPressEscape: false }
    ).then(() => {
      dialogShowing = false
      userStore.logout()
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
      userStore.logout()
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
      userStore.logout()
      router.push('/login')
    }).catch(() => {
      dialogShowing = false
    })
  }
}

export function startTokenMonitor() {
  if (monitorTimer) return
  checkOnlineTime()
  monitorTimer = setInterval(checkOnlineTime, 60000)
}

export function stopTokenMonitor() {
  if (monitorTimer) {
    clearInterval(monitorTimer)
    monitorTimer = null
  }
  level18Shown = false
  level2350Shown = false
  dialogShowing = false
}

// 封装请求方法
const request = {
  get<T = unknown>(url: string, params?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.get(url, { params, ...config }).then(res => res.data)
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.post(url, data, config).then(res => res.data)
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.put(url, data, config).then(res => res.data)
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.delete(url, config).then(res => res.data)
  },
}

export default request
