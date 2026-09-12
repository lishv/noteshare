import request from './index'

// 认证
export const login = (data: { username: string; password: string }) =>
  request.post('/auth/login', data)
export const getProfile = () => request.get('/auth/profile')
export const changePassword = (data: { oldPassword: string; newPassword: string }) =>
  request.put('/auth/password', data)
export const logout = () => request.post('/auth/logout')

// 仪表盘
export const getDashboardOverview = () => request.get('/dashboard/overview')
export const getUserTrend = (days?: number) => request.get('/dashboard/user-trend', { params: { days } })
export const getNoteTrend = (days?: number) => request.get('/dashboard/note-trend', { params: { days } })
export const getTopNotes = (limit?: number) => request.get('/dashboard/top-notes', { params: { limit } })
export const getUserActivity = () => request.get('/dashboard/user-activity')

// 用户管理
export const getUserList = (params: { page?: number; size?: number; keyword?: string; status?: number }) =>
  request.get('/users', { params })
export const getUserDetail = (userId: number) => request.get(`/users/${userId}`)
export const createUser = (data: { username: string; password: string; email: string; phone?: string; bio?: string }) =>
  request.post('/users', data)
export const updateUser = (userId: number, data: { username: string; email: string; phone?: string; bio?: string; status?: number; password?: string }) =>
  request.put(`/users/${userId}`, data)
export const deleteUser = (userId: number) => request.delete(`/users/${userId}`)
export const toggleUserStatus = (userId: number, status: number) =>
  request.put(`/users/${userId}/status`, null, { params: { status } })
export const unlockUser = (userId: number) => request.post(`/users/${userId}/unlock`)
export const resetUserPassword = (userId: number) => request.put(`/users/${userId}/reset-password`)
export const batchToggleUserStatus = (data: { ids: number[]; status: number }) =>
  request.put('/users/batch/status', data)
export const batchDeleteUsers = (data: { ids: number[] }) =>
  request.delete('/users/batch', { data })

// 笔记管理
export const getNoteList = (params: { page?: number; size?: number; keyword?: string; noteType?: number; isPublic?: number; isDeleted?: number }) =>
  request.get('/notes', { params })
export const getNoteDetail = (noteId: number) => request.get(`/notes/${noteId}`)
export const createNote = (userId: number, data: { title: string; noteType?: number; locationOrUrl?: string; content?: string; summary?: string; isPublic?: number }) =>
  request.post(`/notes?userId=${userId}`, data)
export const deleteNote = (noteId: number) => request.put(`/notes/${noteId}/delete`)
export const restoreNote = (noteId: number) => request.put(`/notes/${noteId}/restore`)
export const pinNote = (noteId: number) => request.put(`/notes/${noteId}/pin`)
export const unpinNote = (noteId: number) => request.put(`/notes/${noteId}/unpin`)
export const batchDeleteNotes = (data: { ids: number[] }) => request.put('/notes/batch/delete', data)
export const batchRestoreNotes = (data: { ids: number[] }) => request.put('/notes/batch/restore', data)
export const getNoteFavoriteCount = (noteId: number) => request.get(`/notes/${noteId}/favorites`)
export const updateNoteTags = (noteId: number, tagNames: string[]) => request.put(`/notes/${noteId}/tags`, { tagNames })

// 链接管理
export const getLinkList = (params: { page?: number; size?: number; keyword?: string }) =>
  request.get('/links', { params })
export const editLink = (linkId: number, data: { mainUrl: string }) => request.put(`/links/${linkId}`, data)
export const deleteLink = (linkId: number) => request.delete(`/links/${linkId}`)
export const batchDeleteLinks = (data: { ids: number[] }) => request.delete('/links/batch', { data })

// 敏感词管理
export const getWordList = (params: { page?: number; size?: number; keyword?: string; type?: number; status?: number }) =>
  request.get('/words', { params })
export const getWordDetail = (id: number) => request.get(`/words/${id}`)
export const createWord = (data: { word: string; type?: number; status?: number }) => request.post('/words', data)
export const updateWord = (id: number, data: { word?: string; type?: number; status?: number }) =>
  request.put(`/words/${id}`, data)
export const deleteWord = (id: number) => request.delete(`/words/${id}`)
export const toggleWordStatus = (id: number, status: number) =>
  request.put(`/words/${id}/status`, null, { params: { status } })
export const batchImportWords = (data: { words: string[]; type?: number }) => request.post('/words/batch-import', data)
export const batchDeleteWords = (data: { ids: number[] }) => request.delete('/words/batch', { data })
export const batchToggleWordStatus = (data: { ids: number[]; status: number }) =>
  request.put('/words/batch/status', data)

// 数据统计
export const getPlatformSummary = () => request.get('/stats/summary')
export const getRegistrationStats = (days?: number) => request.get('/stats/user-registration-trend', { params: { days } })
export const getActivityStats = (period?: string) => request.get('/stats/user-activity', { params: { period } })
export const getDistributionStats = () => request.get('/stats/user-note-distribution')
export const getPublishStats = (days?: number) => request.get('/stats/note-publish-trend', { params: { days } })
export const getViewRanking = (limit?: number) => request.get('/stats/note-view-ranking', { params: { limit } })
export const getLinkTypeStats = () => request.get('/stats/note-link-type')
export const getNoteTypeStats = () => request.get('/stats/note-type-distribution')
export const getVisibilityStats = () => request.get('/stats/note-visibility')
export const getInteractionStats = () => request.get('/stats/interaction-distribution')
export const getPlatformTrends = (days?: number) => request.get('/stats/platform-trend', { params: { days } })

// 操作日志
export const getLogList = (params: { page?: number; size?: number; operationType?: string; startTime?: string; endTime?: string }) =>
  request.get('/logs', { params })

// 管理员管理
export const getAdminList = (params: { page?: number; size?: number; keyword?: string }) =>
  request.get('/admins', { params })
export const createAdmin = (data: { username: string; password: string; email?: string; phone?: string; role?: number }) =>
  request.post('/admins', data)
export const editAdmin = (adminId: number, data: { email?: string; phone?: string; role?: number }) =>
  request.put(`/admins/${adminId}`, data)
export const deleteAdmin = (adminId: number) => request.delete(`/admins/${adminId}`)
export const toggleAdminStatus = (adminId: number, status: number) =>
  request.put(`/admins/${adminId}/status`, null, { params: { status } })
export const resetAdminPassword = (adminId: number) => request.put(`/admins/${adminId}/reset-password`)
