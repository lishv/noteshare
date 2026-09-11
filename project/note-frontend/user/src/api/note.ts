import request from '@/utils/request'
import type {
  ApiResponse,
  PageResult,
  User,
  MainLink,
  NoteItem,
  NoteRequest,
  SearchResult,
  CommentItem,
  CommentDTO,
  FollowStatsVO,
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
  params: { page?: number; size?: number; keyword?: string; noteType?: number; startDate?: string; endDate?: string }
): Promise<ApiResponse<PageResult<NoteItem>>> {
  return request.get(`/notes/user/${userId}`, params)
}

// 获取单篇笔记详情 (包含 content 正文)
export function getNoteById(noteId: number): Promise<ApiResponse<NoteItem>> {
  return request.get(`/notes/${noteId}`)
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

// ========== 笔记收藏 ==========
export function addFavorite(noteId: number): Promise<ApiResponse> {
  return request.post(`/favorites/${noteId}`)
}

export function removeFavorite(noteId: number): Promise<ApiResponse> {
  return request.delete(`/favorites/${noteId}`)
}

export function getMyFavorites(params: {
  page?: number
  size?: number
}): Promise<ApiResponse<PageResult<NoteItem>>> {
  return request.get('/favorites', params)
}

export function checkFavorite(noteId: number): Promise<ApiResponse<boolean>> {
  return request.get(`/favorites/check/${noteId}`)
}

// ========== 笔记置顶 ==========
export function pinNote(noteId: number): Promise<ApiResponse> {
  return request.put(`/notes/${noteId}/pin`)
}

export function unpinNote(noteId: number): Promise<ApiResponse> {
  return request.put(`/notes/${noteId}/unpin`)
}

// 上传笔记插图/封面
export function uploadNoteImage(file: File): Promise<ApiResponse<string>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/image', formData)
}

// ========== 标签 ==========
export interface Tag {
  id: number
  userId: number
  name: string
  createdAt: string
}

export function getMyTags(): Promise<ApiResponse<Tag[]>> {
  return request.get('/tags')
}

export function createTag(name: string): Promise<ApiResponse<Tag>> {
  return request.post(`/tags?name=${encodeURIComponent(name)}`)
}

export function deleteTag(tagId: number): Promise<ApiResponse> {
  return request.delete(`/tags/${tagId}`)
}

// ========== 笔记评论与回复 ==========
export function getNoteComments(noteId: number): Promise<ApiResponse<CommentItem[]>> {
  return request.get(`/notes/${noteId}/comments`)
}

export function createComment(noteId: number, data: CommentDTO): Promise<ApiResponse<CommentItem>> {
  return request.post(`/notes/${noteId}/comments`, data)
}

export function deleteNoteComment(noteId: number, commentId: number): Promise<ApiResponse> {
  return request.delete(`/notes/${noteId}/comments/${commentId}`)
}

// ========== 关注系统 ==========
export function followUser(userId: number): Promise<ApiResponse<boolean>> {
  return request.post(`/follow/${userId}`)
}

export function unfollowUser(userId: number): Promise<ApiResponse<boolean>> {
  return request.delete(`/follow/${userId}`)
}

export function getFollowStats(userId: number): Promise<ApiResponse<FollowStatsVO>> {
  return request.get(`/follow/${userId}/status`)
}

export function getFollowedUsers(params?: { page?: number; size?: number }): Promise<ApiResponse<PageResult<User>>> {
  return request.get('/follow/following', params)
}

