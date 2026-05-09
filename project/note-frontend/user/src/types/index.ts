// API统一响应格式
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页参数
export interface PageParams {
  page: number
  size: number
  keyword?: string
}

// 分页响应
export interface PageResult<T> {
  total: number
  page: number
  size: number
  list: T[]
}

// 用户信息
export interface User {
  id: number
  username: string
  email: string
  phone?: string
  avatar?: string
  status: number
  lastActiveAt?: string
  createdAt: string
  hasMainLink?: boolean
  noteCount?: number
}

// 登录请求
export interface LoginRequest {
  username: string
  password: string
}

// 注册请求
export interface RegisterRequest {
  username: string
  password: string
  email: string
  phone?: string
  verificationCode: string
}

// 登录响应
export interface LoginResponse {
  token: string
  user: User
}

// 修改密码请求
export interface ChangePasswordRequest {
  oldPassword?: string
  newPassword: string
  email?: string
  verificationCode?: string
}

// 发送验证码请求
export interface SendCodeRequest {
  email: string
  scene: string
}

// 总链接
export interface MainLink {
  id: number
  userId: number
  mainUrl: string
  viewCount: number
  createdAt: string
  updatedAt: string
}

// 笔记项
export interface NoteItem {
  id: number
  userId: number
  username?: string
  title: string
  locationOrUrl: string
  viewCount: number
  createdAt: string
  updatedAt: string
}

// 创建/更新笔记请求
export interface NoteRequest {
  title: string
  locationOrUrl: string
}

// 搜索结果
export interface SearchResult {
  total: number
  page: number
  size: number
  users: User[]
  notes: NoteItem[]
}
