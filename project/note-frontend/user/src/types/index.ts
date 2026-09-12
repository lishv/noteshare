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
  bio?: string
  status: number
  lastActiveAt?: string
  createdAt: string
  hasMainLink?: boolean
  noteCount?: number
  followerCount?: number
  followingCount?: number
  isFollowing?: boolean
}

export interface UpdateBioRequest {
  bio: string
}

// 关注状态与统计
export interface FollowStatsVO {
  userId: number
  followerCount: number
  followingCount: number
  isFollowing: boolean
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

// 忘记密码请求
export interface ResetPasswordRequest {
  email: string
  verificationCode: string
  newPassword: string
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
  avatar?: string
  title: string
  noteType?: number // 1: 链接笔记, 2: 创作笔记
  locationOrUrl?: string
  content?: string
  summary?: string
  coverImage?: string
  viewCount: number
  favoriteCount?: number
  likeCount?: number
  commentCount?: number
  heatScore?: number
  isLiked?: boolean
  isFavorited?: boolean
  isPinned?: number
  isPublic?: number // 1: 公开, 0: 私密
  isDeleted?: number
  deletedAt?: string
  tags?: string[]
  createdAt: string
  updatedAt: string
}

// 创建/更新笔记请求
export interface NoteRequest {
  title: string
  noteType?: number
  locationOrUrl?: string
  content?: string
  summary?: string
  coverImage?: string
  isPublic?: number // 1: 公开, 0: 私密
  tagIds?: number[]
  tagNames?: string[]
}

// 搜索结果
export interface SearchResult {
  total: number
  page: number
  size: number
  users: User[]
  notes: NoteItem[]
}

// 评论数据结构
export interface CommentItem {
  id: number
  noteId: number
  userId: number
  username: string
  avatar?: string
  parentId?: number
  replyToUserId?: number
  replyToUsername?: string
  replyToCommentId?: number
  content: string
  isOwner?: boolean
  createdAt: string
  updatedAt: string
}

// 发表评论请求
export interface CommentDTO {
  content: string
  parentId?: number
  replyToUserId?: number
  replyToCommentId?: number
}

// 笔记草稿
export interface NoteDraft {
  noteId?: number | null
  title?: string
  content?: string
  coverImage?: string
  isPublic?: number
  tagIds?: number[]
  savedAt?: string
}

export interface NoteDraftRequest {
  noteId?: number | null
  title?: string
  content?: string
  coverImage?: string
  isPublic?: number
  tagIds?: number[]
}

// 站内通知
export interface NotificationItem {
  id: number
  userId: number
  senderId?: number
  senderUsername?: string
  senderAvatar?: string
  type: 'COMMENT' | 'FOLLOW' | 'LIKE' | 'SYSTEM' | string
  title: string
  content: string
  targetType?: string
  targetId?: number
  isRead: number
  isDeleted?: number
  createdAt: string
}

// 热门标签统计
export interface PopularTagItem {
  name: string
  noteCount: number
}

