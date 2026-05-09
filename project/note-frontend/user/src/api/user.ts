import request from '@/utils/request'
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ChangePasswordRequest,
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

// 发送验证码
export function sendVerificationCode(data: SendCodeRequest): Promise<ApiResponse> {
  return request.post('/auth/send-code', data)
}
