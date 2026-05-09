import request from '@/utils/request'
import type {
  ApiResponse,
  PageResult,
  User,
  MainLink,
  NoteItem,
  NoteRequest,
  SearchResult,
} from '@/types'

// 获取活跃用户列表
export function getActiveUsers(params: {
  page?: number
  size?: number
  keyword?: string
}): Promise<ApiResponse<PageResult<User>>> {
  return request.get('/users/active', params)
}

// 设置/更新总链接
export function setMainLink(data: { mainUrl: string }): Promise<ApiResponse<MainLink>> {
  return request.post('/main-link', data)
}

// 获取用户总链接
export function getMainLink(userId: number): Promise<ApiResponse<MainLink>> {
  return request.get(`/main-link/${userId}`)
}

// 删除总链接
export function deleteMainLink(): Promise<ApiResponse> {
  return request.delete('/main-link')
}

// 创建笔记
export function createNote(data: NoteRequest): Promise<ApiResponse<NoteItem>> {
  return request.post('/notes', data)
}

// 获取用户笔记列表
export function getUserNotes(
  userId: number,
  params: { page?: number; size?: number }
): Promise<ApiResponse<PageResult<NoteItem>>> {
  return request.get(`/notes/user/${userId}`, params)
}

// 更新笔记
export function updateNote(
  noteId: number,
  data: NoteRequest
): Promise<ApiResponse<NoteItem>> {
  return request.put(`/notes/${noteId}`, data)
}

// 删除笔记
export function deleteNote(noteId: number): Promise<ApiResponse> {
  return request.delete(`/notes/${noteId}`)
}

// 增加笔记浏览量
export function incrementNoteView(noteId: number): Promise<ApiResponse<{ viewCount: number }>> {
  return request.post(`/notes/${noteId}/view`)
}

// 全局搜索
export function globalSearch(params: {
  keyword: string
  page?: number
  size?: number
}): Promise<ApiResponse<SearchResult>> {
  return request.get('/search', params)
}
