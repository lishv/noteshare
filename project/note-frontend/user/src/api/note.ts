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
  NoteDraft,
  NoteDraftRequest,
  NotificationItem,
  PopularTagItem,
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

// ========== 笔记草稿 (Redis暂存) ==========
export function saveNoteDraft(data: NoteDraftRequest): Promise<ApiResponse<NoteDraft>> {
  return request.post('/notes/draft', data)
}

export function getNoteDraft(noteId?: number): Promise<ApiResponse<NoteDraft>> {
  return request.get('/notes/draft', noteId ? { noteId } : undefined)
}

export function deleteNoteDraft(noteId?: number): Promise<ApiResponse> {
  return request.delete('/notes/draft', noteId ? { params: { noteId } } : undefined)
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
  return request.get('/follow/list', params)
}

export function getFollowers(params?: { userId?: number; page?: number; size?: number }): Promise<ApiResponse<PageResult<User>>> {
  return request.get('/follow/followers', params)
}

// ========== 笔记回收站 ==========
export function getRecycleBinNotes(params?: { page?: number; size?: number }): Promise<ApiResponse<PageResult<NoteItem>>> {
  return request.get('/notes/recycle-bin', params)
}

export function restoreRecycleNote(noteId: number): Promise<ApiResponse<null>> {
  return request.put(`/notes/recycle-bin/${noteId}/restore`)
}

export function hardDeleteRecycleNote(noteId: number): Promise<ApiResponse<null>> {
  return request.delete(`/notes/recycle-bin/${noteId}`)
}

export function clearRecycleBin(): Promise<ApiResponse<null>> {
  return request.delete('/notes/recycle-bin/clear')
}

// ========== 关注动态流 ==========
export function getFollowingFeed(params?: { page?: number; size?: number }): Promise<ApiResponse<PageResult<NoteItem>>> {
  return request.get('/notes/feed', params)
}

// ========== 全站热度榜 ==========
export function getTrendingNotes(limit: number = 10): Promise<ApiResponse<NoteItem[]>> {
  return request.get('/notes/trending', { limit })
}

// ========== 公共主页推荐 ==========
export function getRecommendedNotes(params?: {
  page?: number
  size?: number
  tag?: string
}): Promise<ApiResponse<PageResult<NoteItem>>> {
  return request.get('/notes/recommend', params)
}

// ========== 点赞机制 ==========
export function likeNote(noteId: number): Promise<ApiResponse<null>> {
  return request.post(`/notes/${noteId}/like`)
}

export function unlikeNote(noteId: number): Promise<ApiResponse<null>> {
  return request.delete(`/notes/${noteId}/like`)
}

export function checkNoteLiked(noteId: number): Promise<ApiResponse<boolean>> {
  return request.get(`/notes/${noteId}/like`)
}

export function getMyLikes(params?: {
  page?: number
  size?: number
}): Promise<ApiResponse<PageResult<NoteItem>>> {
  return request.get('/notes/my-likes', params)
}

// ========== 站内消息通知 ==========
export function getNotifications(params?: {
  type?: string
  page?: number
  size?: number
}): Promise<ApiResponse<PageResult<NotificationItem>>> {
  return request.get('/notifications', params)
}

export function getUnreadNotificationCount(): Promise<ApiResponse<number>> {
  return request.get('/notifications/unread-count')
}

export function markNotificationAsRead(id: number): Promise<ApiResponse<null>> {
  return request.put(`/notifications/${id}/read`)
}

export function removeNotification(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/notifications/${id}`)
}

export function markAllNotificationsAsRead(): Promise<ApiResponse<null>> {
  return request.put('/notifications/read-all')
}

// ========== 热门标签 ==========
export function getPopularTags(limit: number = 10): Promise<ApiResponse<PopularTagItem[]>> {
  return request.get('/tags/popular', { limit })
}
