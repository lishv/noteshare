import request from '@/utils/request'
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ChangePasswordRequest,
  ResetPasswordRequest,
  SendCodeRequest,
  User,
} from '@/types'

// 用户注册
export function register(data: RegisterRequest): Promise<ApiResponse> {
  return request.post('/auth/register', data)
}

// 用户登录
export function login(data: LoginRequest): Promise<ApiResponse<LoginResponse>> {
  return request.post('/auth/login', data)
}

// 获取当前用户信息
export function getCurrentUser(): Promise<ApiResponse<User>> {
  return request.get('/users/current')
}

// 获取指定用户公开主页信息（包含笔记数、粉丝数、关注数及回关状态）
export function getUserProfile(userId: number): Promise<ApiResponse<User>> {
  return request.get(`/users/${userId}`)
}

// 获取系统推荐创作者（排除当前用户本人及已关注作者）
export function getRecommendedCreators(limit = 5): Promise<ApiResponse<User[]>> {
  return request.get('/users/recommended', { limit })
}

// 修改密码
export function changePassword(data: ChangePasswordRequest): Promise<ApiResponse> {
  return request.put('/users/password', data)
}

// 上传头像
export function uploadAvatar(file: File): Promise<ApiResponse<User>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/avatar', formData)
}

// 修改个性签名
export function updateUserBio(bio: string): Promise<ApiResponse<User>> {
  return request.put('/users/bio', { bio })
}

// 发送验证码
export function sendVerificationCode(data: SendCodeRequest): Promise<ApiResponse> {
  return request.post('/auth/send-code', data)
}

// 使用邮箱验证码重置密码
export function resetPassword(data: ResetPasswordRequest): Promise<ApiResponse> {
  return request.post('/auth/reset-password', data)
}
