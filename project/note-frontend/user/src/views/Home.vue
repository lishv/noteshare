<template>
  <div class="app-container">
    <!-- 顶部操作区 -->
    <header class="top-bar">
      <div class="top-bar-left">
        <h1 class="logo">笔记共享平台</h1>
        <div class="search-group">
          <el-input
            v-model="authorKeyword"
            placeholder="搜索作者姓名..."
            class="search-input search-author"
            clearable
            :prefix-icon="UserIcon"
            @keyup.enter="handleAuthorSearch"
            @clear="handleClearAuthorSearch"
          />
          <el-input
            v-model="noteKeyword"
            placeholder="搜索笔记名称或内容..."
            class="search-input search-note"
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleNoteSearch"
            @clear="handleClearNoteSearch"
          />
        </div>
      </div>
      <div class="top-bar-right">
        <template v-if="isLoggedIn">
          <el-button type="primary" @click="showPublishDialog = true">
            <el-icon><Plus /></el-icon>
            发布笔记
          </el-button>
          <el-dropdown @command="handleUserCommand">
            <span class="user-avatar">
              <el-avatar :size="32" :src="userInfo?.avatar || defaultAvatar">
                {{ userInfo?.username?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="username-text">{{ userInfo?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                <el-dropdown-item command="settings">个人设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" @click="$router.push('/login')">登录</el-button>
          <el-button @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </header>

    <!-- 移动端菜单按钮 -->
    <el-button
      class="mobile-menu-btn"
      :icon="Menu"
      circle
      @click="drawerVisible = true"
    />

    <div class="main-content">
      <!-- 左侧导航栏 -->
      <aside class="sidebar" :class="{ 'drawer-open': drawerVisible }" v-show="!isMobile || drawerVisible">
        <div class="sidebar-header">
          <h3>活跃同学</h3>
          <span class="user-count" v-if="userList.length">共 {{ totalUsers }} 人</span>
          <el-button v-if="isMobile" text circle size="small" @click="drawerVisible = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="user-list" v-loading="userLoading">
          <div
            v-for="user in userList"
            :key="user.id"
            class="user-item"
            :class="{ active: selectedUserId === user.id }"
            @click="selectUser(user)"
          >
            <el-avatar :size="36" :src="user.avatar || defaultAvatar">
              {{ user.username.charAt(0) }}
            </el-avatar>
            <div class="user-info">
              <div class="user-name" :title="user.username">{{ user.username }}</div>
              <div class="user-meta">
                <span>{{ user.noteCount || 0 }} 篇笔记</span>
                <span class="time">{{ formatDate(user.lastActiveAt || '') }}</span>
              </div>
            </div>
          </div>
          <el-empty v-if="!userLoading && userList.length === 0" description="暂无活跃同学" />
        </div>
        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="totalUsers > pageSize">
          <el-pagination
            small
            layout="prev, pager, next"
            :total="totalUsers"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="handlePageChange"
          />
        </div>
      </aside>

      <!-- 移动端遮罩 -->
      <div v-if="drawerVisible && isMobile" class="drawer-mask" @click="drawerVisible = false" />

      <!-- 右侧内容区 -->
      <main class="content-area" v-loading="contentLoading">
        <template v-if="selectedUser">
          <!-- 用户基本信息 -->
          <div class="user-header">
            <el-avatar :size="64" :src="selectedUser.avatar || defaultAvatar">
              {{ selectedUser.username.charAt(0) }}
            </el-avatar>
            <div class="user-details">
              <h2 class="clickable-username" @click="$router.push(`/user/${selectedUser.id}`)">{{ selectedUser.username }}</h2>
              <p class="user-stats">{{ selectedUser.noteCount || 0 }} 篇笔记</p>
            </div>
          </div>

          <!-- 总链接区 -->
          <div v-if="mainLink" class="main-link-card" @click="openMainLink">
            <div class="main-link-icon">
              <el-icon :size="24"><Link /></el-icon>
            </div>
            <div class="main-link-info">
              <div class="main-link-title">Ta 的知识库主页</div>
              <div class="main-link-url">{{ mainLink.mainUrl }}</div>
            </div>
            <el-icon class="main-link-arrow"><ArrowRight /></el-icon>
          </div>
          <el-alert
            v-else-if="isOwner"
            title="您还未设置总链接"
            description="建议先设置总链接，再录入相对位置笔记"
            type="info"
            :closable="false"
            show-icon
            class="main-link-alert"
          />

          <!-- 子笔记列表 -->
          <div class="notes-section">
            <div class="notes-toolbar">
              <h3>笔记列表</h3>
              <div class="note-filter-bar">
                <el-input
                  v-model="localNoteKeyword"
                  placeholder="搜索笔记名/链接位置..."
                  clearable
                  :prefix-icon="Search"
                  style="width: 260px"
                  @keyup.enter="handleLocalNoteSearch"
                  @clear="handleClearLocalNoteSearch"
                />
                <el-date-picker
                  v-model="noteDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  size="default"
                  style="width: 260px"
                  @change="handleLocalNoteSearch"
                />
                <el-button type="primary" :icon="Search" @click="handleLocalNoteSearch">搜索</el-button>
              </div>
            </div>
            <el-table :data="noteList" style="width: 100%" stripe>
              <el-table-column prop="title" label="笔记名称" min-width="200">
                <template #default="{ row }">
                  <span class="note-title">
                    <el-tag v-if="row.isPinned === 1" type="warning" size="small" style="margin-right: 6px;">置顶</el-tag>
                    {{ row.title }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="locationOrUrl" label="链接/位置" min-width="250">
                <template #default="{ row }">
                  <a
                    v-if="isUrl(row.locationOrUrl)"
                    :href="row.locationOrUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="note-link"
                    @click="incrementView(row)"
                  >
                    <el-icon><Link /></el-icon>
                    {{ row.locationOrUrl }}
                  </a>
                  <div v-else class="note-location">
                    <el-tooltip
                      v-if="!mainLink"
                      content="建议先设置总链接后再录入相对位置笔记"
                      placement="top"
                    >
                      <span>{{ row.locationOrUrl }}</span>
                    </el-tooltip>
                    <span v-else>{{ row.locationOrUrl }}</span>
                    <el-button
                      type="info"
                      size="small"
                      text
                      @click="copyToClipboard(row.locationOrUrl)"
                    >
                      <el-icon><CopyDocument /></el-icon>
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="viewCount" label="浏览量" width="90" align="center" />
              <el-table-column prop="updatedAt" label="更新时间" width="160">
                <template #default="{ row }">
                  {{ formatDate(row.updatedAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" :width="isOwner ? '240' : '140'" align="center">
                <template #default="{ row }">
                  <el-button v-if="isLoggedIn && !isOwner" :type="favoriteMap[row.id] ? 'warning' : 'default'" size="small" text @click="toggleFavorite(row)">
                    {{ favoriteMap[row.id] ? '已收藏' : '收藏' }}
                  </el-button>
                  <el-button v-if="isOwner" type="primary" size="small" text @click="editNote(row)">编辑</el-button>
                  <el-button v-if="isOwner" :type="row.isPinned === 1 ? 'warning' : 'default'" size="small" text @click="togglePin(row)">
                    {{ row.isPinned === 1 ? '取消置顶' : '置顶' }}
                  </el-button>
                  <el-button v-if="isOwner" type="danger" size="small" text @click="handleDeleteNote(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <!-- 笔记分页 -->
            <div class="note-pagination" v-if="totalNotes > 0">
              <el-pagination
                layout="total, prev, pager, next"
                :total="totalNotes"
                :page-size="notePageSize"
                :current-page="noteCurrentPage"
                @current-change="handleNotePageChange"
              />
            </div>
            <el-empty v-if="!contentLoading && noteList.length === 0" description="暂无笔记" />
          </div>
        </template>
        <el-empty v-else description="请选择一位同学查看笔记" :image-size="200" />
      </main>
    </div>

    <!-- 发布/编辑笔记弹窗 -->
    <PublishDialog
      v-model:visible="showPublishDialog"
      :edit-note="editingNote"
      :has-main-link="!!mainLink"
      :is-owner="isOwner"
      @success="onPublishSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { User as UserIcon, Search, Plus, Menu, Link, ArrowRight, CopyDocument, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getActiveUsers, getMainLink, getUserNotes, deleteNote, globalSearch, incrementNoteView, addFavorite, removeFavorite, checkFavorite, pinNote, unpinNote } from '@/api/note'
import { formatDate, isUrl, isSafeUrl } from '@/utils/helpers'
import PublishDialog from '@/components/PublishDialog.vue'
import type { User, MainLink, NoteItem } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)

const defaultAvatar = ''
const authorKeyword = ref('')
const noteKeyword = ref('')
const userList = ref<User[]>([])
const userLoading = ref(false)
const contentLoading = ref(false)
const selectedUserId = ref<number | null>(null)
const selectedUser = ref<User | null>(null)
const mainLink = ref<MainLink | null>(null)
const noteList = ref<NoteItem[]>([])
const currentPage = ref(1)
const pageSize = 20
const totalUsers = ref(0)
const totalNotes = ref(0)
const noteCurrentPage = ref(1)
const notePageSize = 10
const localNoteKeyword = ref('')
const noteDateRange = ref<[string, string] | null>(null)
const showPublishDialog = ref(false)
const editingNote = ref<NoteItem | null>(null)
const drawerVisible = ref(false)
const isMobile = ref(false)

const isOwner = computed(() => {
  return isLoggedIn.value && selectedUserId.value === userStore.userInfo?.id
})

// 响应式检测
function checkMobile() {
  isMobile.value = window.innerWidth <= 768
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  fetchUsers()
  if (isLoggedIn.value) {
    userStore.fetchUserInfo()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

// 获取用户列表
async function fetchUsers() {
  userLoading.value = true
  try {
    const res = await getActiveUsers({
      page: currentPage.value,
      size: pageSize,
      keyword: authorKeyword.value || undefined,
    })
    userList.value = res.data.list
    totalUsers.value = res.data.total
  } catch (error) {
    // 网络错误时显示提示，但不阻断页面使用
    console.warn('API 调用失败（后端服务可能未启动）:', error)
    // 使用模拟数据进行演示
    userList.value = [
      { id: 1, username: '张三', avatar: '', status: 1, lastActiveAt: new Date().toISOString(), noteCount: 5 },
      { id: 2, username: '李四', avatar: '', status: 1, lastActiveAt: new Date().toISOString(), noteCount: 3 },
      { id: 3, username: '王五', avatar: '', status: 1, lastActiveAt: new Date().toISOString(), noteCount: 8 },
    ]
    totalUsers.value = 3
  } finally {
    userLoading.value = false
  }
}

// 选择用户
async function selectUser(user: User) {
  selectedUserId.value = user.id
  selectedUser.value = user
  localNoteKeyword.value = ''
  noteDateRange.value = null
  noteCurrentPage.value = 1
  drawerVisible.value = false
  await fetchUserDetail(user.id)
}

// 获取用户详情
async function fetchUserDetail(userId: number) {
  if (!userId) return
  contentLoading.value = true
  try {
    const [linkRes, notesRes] = await Promise.all([
      getMainLink(userId),
      getUserNotes(userId, {
        page: noteCurrentPage.value,
        size: notePageSize,
        keyword: localNoteKeyword.value || undefined,
        startDate: noteDateRange.value?.[0] || undefined,
        endDate: noteDateRange.value?.[1] || undefined,
      }),
    ])
    mainLink.value = linkRes.data
    noteList.value = notesRes.data.list
    totalNotes.value = notesRes.data.total
  } catch (error) {
    console.warn('API 调用失败（后端服务可能未启动）:', error)
    // 使用模拟数据进行演示
    if (userId === 1) {
      mainLink.value = { id: 1, userId: 1, mainUrl: 'https://notion.so/zhangsan', viewCount: 0, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }
      noteList.value = [
        { id: 1, userId: 1, title: 'Java 基础笔记', locationOrUrl: 'https://notion.so/zhangsan/java', viewCount: 10, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
        { id: 2, userId: 1, title: 'Vue3 学习笔记', locationOrUrl: '2025/三月份学习计划', viewCount: 5, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
      ]
      totalNotes.value = 2
    } else if (userId === 2) {
      mainLink.value = null
      noteList.value = [
        { id: 1, userId: 2, title: '英语词汇积累', locationOrUrl: '笔记本第 3 页', viewCount: 3, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
      ]
      totalNotes.value = 1
    } else {
      mainLink.value = { id: 3, userId: 3, mainUrl: 'https://feishu.cn/wangwu', viewCount: 0, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }
      noteList.value = []
      totalNotes.value = 0
    }
  } finally {
    contentLoading.value = false
    loadFavorites()
  }
}

// 搜索作者姓名
async function handleAuthorSearch() {
  if (!authorKeyword.value.trim()) {
    fetchUsers()
    return
  }
  userLoading.value = true
  try {
    const res = await getActiveUsers({
      page: 1,
      size: 20,
      keyword: authorKeyword.value,
    })
    userList.value = res.data.list
    totalUsers.value = res.data.total
    // 如果只搜到一个用户，自动跳转到该用户页面
    if (res.data.list.length === 1) {
      selectUser(res.data.list[0])
    }
  } catch {
    fetchUsers()
  } finally {
    userLoading.value = false
  }
}

function handleClearAuthorSearch() {
  authorKeyword.value = ''
  fetchUsers()
}

// 搜索笔记名称或内容
async function handleNoteSearch() {
  if (!noteKeyword.value.trim()) {
    fetchUsers()
    return
  }
  userLoading.value = true
  try {
    const res = await globalSearch({
      keyword: noteKeyword.value,
      page: 1,
      size: 50, // 获取更多结果以便提取用户
    })

    // 从笔记搜索结果中提取拥有这些笔记的用户
    const userMap = new Map<number, User>()
    res.data.notes.forEach(note => {
      if (note.userId && note.username) {
        if (!userMap.has(note.userId)) {
          userMap.set(note.userId, {
            id: note.userId,
            username: note.username,
            avatar: note.avatar || '',
            status: 1,
            createdAt: '',
          } as User)
        }
      }
    })

    // 显示在左侧活跃同学列表
    userList.value = Array.from(userMap.values())
    totalUsers.value = userList.value.length

    // 如果只找到一个用户，自动跳转到该用户页面
    if (userList.value.length === 1) {
      selectUser(userList.value[0])
    }

    // 如果没有找到匹配笔记的用户，提示用户
    if (userList.value.length === 0) {
      ElMessage.info('未找到包含该内容的笔记')
    }
  } catch {
    fetchUsers()
  } finally {
    userLoading.value = false
  }
}

function handleClearNoteSearch() {
  noteKeyword.value = ''
  fetchUsers()
}

// 分页
function handlePageChange(page: number) {
  currentPage.value = page
  fetchUsers()
}

function handleNotePageChange(page: number) {
  noteCurrentPage.value = page
  if (selectedUserId.value) {
    fetchUserDetail(selectedUserId.value)
  }
}

function handleLocalNoteSearch() {
  noteCurrentPage.value = 1
  if (selectedUserId.value) {
    fetchUserDetail(selectedUserId.value)
  }
}

function handleClearLocalNoteSearch() {
  localNoteKeyword.value = ''
  noteCurrentPage.value = 1
  if (selectedUserId.value) {
    fetchUserDetail(selectedUserId.value)
  }
}

// 打开总链接
function openMainLink() {
  if (mainLink.value && isSafeUrl(mainLink.value.mainUrl)) {
    window.open(mainLink.value.mainUrl, '_blank', 'noopener,noreferrer')
  }
}

// 复制链接
function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

// 编辑笔记
function editNote(note: NoteItem) {
  editingNote.value = note
  showPublishDialog.value = true
}

// 删除笔记
async function handleDeleteNote(note: NoteItem) {
  try {
    await ElMessageBox.confirm('确定要删除该笔记吗？此操作无法恢复。', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteNote(note.id)
    ElMessage.success('删除成功')
    if (selectedUserId.value) {
      fetchUserDetail(selectedUserId.value)
    }
  } catch {
    // 用户取消
  }
}

// 增加浏览量
function incrementView(note: NoteItem) {
  incrementNoteView(note.id).catch(() => {})
}

// 发布成功回调
function onPublishSuccess() {
  showPublishDialog.value = false
  editingNote.value = null
  // 刷新当前用户详情
  if (selectedUserId.value) {
    fetchUserDetail(selectedUserId.value)
  } else {
    fetchUsers()
  }
}

// 用户菜单操作
async function handleUserCommand(command: string) {
  if (command === 'favorites') {
    router.push('/favorites')
  } else if (command === 'settings') {
    router.push('/settings')
  } else if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}

// ========== 收藏 ==========
const favoriteMap = ref<Record<number, boolean>>({})

async function loadFavorites() {
  if (!isLoggedIn.value) return
  favoriteMap.value = {}
  for (const note of noteList.value) {
    if (!note.id) continue
    try {
      const res = await checkFavorite(note.id)
      favoriteMap.value[note.id] = res.data
    } catch { favoriteMap.value[note.id] = false }
  }
}

async function toggleFavorite(row: NoteItem) {
  if (!isLoggedIn.value) return
  try {
    if (favoriteMap.value[row.id]) {
      await removeFavorite(row.id)
      favoriteMap.value[row.id] = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(row.id)
      favoriteMap.value[row.id] = true
      ElMessage.success('收藏成功')
    }
  } catch { /* handled */ }
}

// ========== 置顶 ==========
async function togglePin(row: NoteItem) {
  try {
    if (row.isPinned === 1) {
      await unpinNote(row.id)
      ElMessage.success('已取消置顶')
    } else {
      await pinNote(row.id)
      ElMessage.success('已置顶')
    }
    if (selectedUserId.value) {
      fetchUserDetail(selectedUserId.value)
    }
  } catch { /* handled */ }
}
</script>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 顶部操作区 */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 50;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.logo {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-primary);
  white-space: nowrap;
}

.logo::after {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  margin-left: 4px;
  vertical-align: middle;
  opacity: 0.5;
}

.search-group {
  display: flex;
  gap: 8px;
  flex: 1;
  max-width: 600px;
}

.search-input {
  flex: 1;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username-text {
  font-size: 14px;
  color: var(--color-text-secondary);
}

/* 移动端菜单按钮 */
.mobile-menu-btn {
  display: none;
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 100;
  width: 48px;
  height: 48px;
}

/* 主内容区 */
.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 左侧导航栏 */
.sidebar {
  width: 280px;
  background: var(--color-bg-sidebar);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--color-border);
}

.sidebar-header h3 {
  font-size: 16px;
  color: var(--color-text-primary);
  font-weight: 600;
}

.user-count {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.user-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: var(--radius-sm);
  margin: 2px 0;
  transition: all 0.15s ease;
}

.user-item:hover {
  background: var(--color-bg-hover);
}

.user-item.active {
  background: var(--color-bg-active);
  border-left: 3px solid var(--color-primary);
}

.user-item.active .user-name {
  color: var(--color-primary-dark);
  font-weight: 600;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
}

.pagination-wrapper {
  padding: 8px;
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: center;
}

/* 右侧内容区 */
.content-area {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xl);
  background: var(--color-bg-page);
}

.user-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: var(--space-lg);
  padding: var(--space-lg);
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.user-details h2 {
  font-size: 20px;
  color: var(--color-text-primary);
  font-weight: 600;
}

.clickable-username {
  cursor: pointer;
  transition: color 0.2s;
}
.clickable-username:hover {
  color: var(--color-primary, #409eff);
}

.user-stats {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

/* 总链接卡片 */
.main-link-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: var(--space-lg);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-left: 4px solid var(--color-primary);
  border-radius: var(--radius-md);
  cursor: pointer;
  margin-bottom: var(--space-lg);
  transition: all 0.2s ease;
}

.main-link-card:hover {
  border-color: var(--color-primary);
  border-left-color: var(--color-primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.main-link-icon {
  width: 48px;
  height: 48px;
  background: var(--color-bg-active);
  color: var(--color-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.main-link-info {
  flex: 1;
  min-width: 0;
}

.main-link-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.main-link-url {
  font-size: 13px;
  color: var(--color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main-link-arrow {
  font-size: 20px;
  color: var(--color-text-tertiary);
}

.main-link-alert {
  margin-bottom: var(--space-lg);
}

/* 笔记区 */
.notes-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-md);
  gap: 16px;
}

.notes-toolbar h3 {
  white-space: nowrap;
  margin: 0;
}

.note-filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.notes-section h3 {
  font-size: 16px;
  color: var(--color-text-primary);
  font-weight: 600;
  margin-bottom: var(--space-md);
}

.notes-section :deep(.el-table) {
  border-radius: var(--radius-md);
  overflow: hidden;
}

.notes-section :deep(.el-table__header th) {
  background: var(--color-bg-page);
  color: var(--color-text-secondary);
  font-weight: 500;
  font-size: 13px;
  border-bottom: 1px solid var(--color-border);
}

.notes-section :deep(.el-table__row:hover > td) {
  background: var(--color-bg-active) !important;
}

.notes-section :deep(.el-table td) {
  border-bottom: 1px solid var(--color-border-light);
}

.note-title {
  font-weight: 500;
  color: var(--color-text-primary);
}

.note-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-primary);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-link:hover {
  text-decoration: underline;
}

.note-location {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-secondary);
}

.note-pagination {
  margin-top: var(--space-md);
  display: flex;
  justify-content: center;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .mobile-menu-btn {
    display: block;
  }

  .logo {
    font-size: 16px;
  }

  .search-group {
    display: none;
  }

  .username-text {
    display: none;
  }

  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 280px;
    z-index: 1001;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
    box-shadow: 2px 0 12px rgba(0, 0, 0, 0.1);
  }

  .sidebar.drawer-open {
    transform: translateX(0);
  }

  .drawer-mask {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 1000;
  }

  .content-area {
    padding: var(--space-md);
  }

  .user-header {
    padding: var(--space-md);
  }

  .user-header h2 {
    font-size: 18px;
  }

  .main-link-card {
    padding: var(--space-md);
  }

  .notes-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .note-filter-bar {
    flex-direction: column;
  }

  .note-filter-bar .el-input,
  .note-filter-bar .el-date-picker {
    width: 100% !important;
  }
}
</style>
