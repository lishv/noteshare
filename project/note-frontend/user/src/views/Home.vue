<template>
  <div class="app-container">
    <!-- 顶部操作区 -->
    <header class="top-bar">
      <div class="top-bar-left">
        <h1 class="logo" @click="handleLogoClick" title="返回首页">笔记共享平台</h1>
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
      <!-- 左侧导航栏（支持折叠/收起） -->
      <aside
        class="sidebar"
        :class="{ 'drawer-open': drawerVisible, 'collapsed': isSidebarCollapsed }"
        v-show="!isMobile || drawerVisible"
      >
        <div class="sidebar-header">
          <div class="sidebar-title-group" v-if="!isSidebarCollapsed">
            <h3>{{ isSearching ? '搜索结果' : (isLoggedIn ? '我的关注' : '活跃作者') }}</h3>
            <span class="user-count" v-if="isSearching">共 {{ totalUsers }} 人</span>
            <span class="user-count" v-else-if="isLoggedIn">关注 {{ totalUsers }} 人</span>
            <span class="user-count" v-else>共 {{ totalUsers }} 人</span>
            <el-button
              v-if="isSearching"
              type="primary"
              link
              size="small"
              class="clear-search-link"
              @click="clearAllSearch"
            >
              {{ isLoggedIn ? '返回关注' : '返回列表' }}
            </el-button>
          </div>
          <div class="sidebar-actions">
            <!-- 桌面端折叠切换按钮 -->
            <el-tooltip :content="isSidebarCollapsed ? '展开侧边栏' : '收起侧边栏'" placement="right">
              <el-button
                v-if="!isMobile"
                text
                circle
                size="small"
                class="collapse-btn"
                @click="toggleSidebarCollapse"
              >
                <el-icon :size="16"><Expand v-if="isSidebarCollapsed" /><Fold v-else /></el-icon>
              </el-button>
            </el-tooltip>
            <!-- 移动端关闭按钮 -->
            <el-button v-if="isMobile" text circle size="small" @click="drawerVisible = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="user-list" v-loading="userLoading">
          <template v-for="user in userList" :key="user.id">
            <!-- 折叠模式：仅显示头像，悬浮 Tooltip 提示详细信息 -->
            <el-tooltip
              v-if="isSidebarCollapsed"
              :content="`${user.username} (${user.id === userInfo?.id ? '我 · ' : ''}${user.noteCount || 0}篇笔记 · ${user.followerCount || 0}粉丝)`"
              placement="right"
            >
              <div
                class="user-item collapsed-item"
                :class="{ active: selectedUserId === user.id }"
                @click="selectUser(user)"
              >
                <el-avatar :size="36" :src="user.avatar || defaultAvatar">
                  {{ user.username.charAt(0) }}
                </el-avatar>
              </div>
            </el-tooltip>

            <!-- 展开模式：完整信息 -->
            <div
              v-else
              class="user-item"
              :class="{ active: selectedUserId === user.id }"
              @click="selectUser(user)"
            >
              <el-avatar :size="36" :src="user.avatar || defaultAvatar">
                {{ user.username.charAt(0) }}
              </el-avatar>
              <div class="user-info">
                <div class="user-name" :title="user.username">
                  <span>{{ user.username }}</span>
                  <el-tag
                    v-if="user.id === userInfo?.id"
                    size="small"
                    type="primary"
                    effect="plain"
                    class="self-tag"
                  >我</el-tag>
                </div>
                <div class="user-meta">
                  <span>{{ user.noteCount || 0 }} 篇笔记</span>
                  <span class="meta-dot">·</span>
                  <span>{{ user.followerCount || 0 }} 粉丝</span>
                  <span class="time">{{ formatDate(user.lastActiveAt || '') }}</span>
                </div>
              </div>
            </div>
          </template>
          <el-empty
            v-if="!userLoading && userList.length === 0"
            :description="isSidebarCollapsed ? '' : (isSearching ? '未找到匹配的作者' : (isLoggedIn ? '暂无关注作者' : '暂无活跃作者'))"
          />
        </div>

        <!-- 分页（仅展开时显示） -->
        <div class="pagination-wrapper" v-if="totalUsers > pageSize && !isSidebarCollapsed">
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
          <!-- 用户基本信息与关注状态 -->
          <div class="user-header">
            <div class="user-header-left">
              <el-avatar
                :size="64"
                :src="selectedUser.avatar || defaultAvatar"
                class="clickable-avatar"
                @click="$router.push(`/user/${selectedUser.id}`)"
              >
                {{ selectedUser.username.charAt(0) }}
              </el-avatar>
              <div class="user-details">
                <div class="user-details-top">
                  <h2 class="clickable-username" @click="$router.push(`/user/${selectedUser.id}`)">
                    {{ selectedUser.username }}
                  </h2>
                  <el-tag v-if="isOwner" size="small" type="success" effect="plain" round>
                    我的主页
                  </el-tag>

                  <!-- 30字个性签名（本人可点击即时修改） -->
                  <div class="user-bio-wrapper">
                    <!-- 编辑状态（仅本人） -->
                    <div v-if="isEditingBio" class="bio-edit-inline" @click.stop>
                      <el-input
                        ref="bioInputRef"
                        v-model="editBioText"
                        maxlength="30"
                        show-word-limit
                        placeholder="写一句个性签名吧 (最多30字)..."
                        size="small"
                        class="bio-input"
                        @keyup.enter="handleSaveBio"
                        @keyup.esc="cancelEditBio"
                      />
                      <el-button type="primary" size="small" :loading="bioSaving" @click="handleSaveBio">保存</el-button>
                      <el-button size="small" :disabled="bioSaving" @click="cancelEditBio">取消</el-button>
                    </div>

                    <!-- 展示状态 -->
                    <template v-else>
                      <!-- 本人：点击即可编辑 -->
                      <div
                        v-if="isOwner"
                        class="bio-badge editable-bio"
                        @click="startEditBio"
                        title="点击修改个性签名 (最多30字)"
                      >
                        <el-icon class="bio-icon"><EditPen /></el-icon>
                        <span v-if="selectedUser.bio" class="bio-text">{{ selectedUser.bio }}</span>
                        <span v-else class="bio-placeholder">添加个性签名...</span>
                      </div>

                      <!-- 访客/其他用户：仅展示签名 -->
                      <div v-else-if="selectedUser.bio" class="bio-badge">
                        <span class="bio-text">“{{ selectedUser.bio }}”</span>
                      </div>
                    </template>
                  </div>
                </div>
                <div class="user-stats">
                  <span class="stat-item"><strong>{{ selectedUser.noteCount || 0 }}</strong> 篇笔记</span>
                  <span class="stat-divider">·</span>
                  <span class="stat-item">
                    <strong>{{ currentFollowStats?.followerCount ?? selectedUser.followerCount ?? 0 }}</strong> 位粉丝
                  </span>
                  <template v-if="currentFollowStats?.followingCount !== undefined">
                    <span class="stat-divider">·</span>
                    <span class="stat-item"><strong>{{ currentFollowStats.followingCount }}</strong> 关注</span>
                  </template>
                </div>
              </div>
            </div>

            <!-- 关注/取关操作 -->
            <div class="user-header-actions">
              <template v-if="!isOwner">
                <el-button
                  v-if="isLoggedIn"
                  :type="currentFollowStats?.isFollowing ? 'default' : 'primary'"
                  :icon="currentFollowStats?.isFollowing ? Check : Plus"
                  :loading="followActionLoading"
                  round
                  @click="toggleFollowAuthor"
                >
                  {{ currentFollowStats?.isFollowing ? '已关注' : '关注作者' }}
                </el-button>
                <el-button
                  v-else
                  type="primary"
                  :icon="Plus"
                  round
                  @click="$router.push('/login')"
                >
                  关注作者
                </el-button>
              </template>
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
                  <span class="note-title" :class="{ 'clickable-title': row.noteType === 2 }" @click="row.noteType === 2 && router.push(`/note/${row.id}`)">
                    <el-tag v-if="row.isPinned === 1" type="warning" size="small" style="margin-right: 6px;">置顶</el-tag>
                    <el-tag :type="row.noteType === 2 ? 'success' : 'info'" size="small" style="margin-right: 6px;">
                      {{ row.noteType === 2 ? '原创' : '外链' }}
                    </el-tag>
                    <el-tag v-if="row.isPublic === 0" type="danger" effect="plain" size="small" style="margin-right: 6px;">
                      <el-icon style="margin-right: 2px; vertical-align: -1px;"><Lock /></el-icon>私密
                    </el-tag>
                    {{ row.title }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="内容/位置" min-width="250">
                <template #default="{ row }">
                  <!-- 原创笔记：显示摘要并支持跳转 -->
                  <div
                    v-if="row.noteType === 2"
                    class="note-article-summary"
                    @click="router.push(`/note/${row.id}`)"
                  >
                    <el-icon><Document /></el-icon>
                    <span class="summary-text">{{ row.summary || '在线创作笔记 (点击阅读全文)' }}</span>
                  </div>
                  <!-- 链接笔记：外链或相对位置 -->
                  <template v-else>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  User as UserIcon,
  Search,
  Plus,
  Menu,
  Link,
  ArrowRight,
  CopyDocument,
  Close,
  Document,
  Lock,
  Fold,
  Expand,
  Check,
  EditPen,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateUserBio } from '@/api/user'
import {
  getActiveUsers,
  getMainLink,
  getUserNotes,
  deleteNote,
  globalSearch,
  incrementNoteView,
  addFavorite,
  removeFavorite,
  checkFavorite,
  pinNote,
  unpinNote,
  followUser,
  unfollowUser,
  getFollowStats,
} from '@/api/note'
import { formatDate, isUrl, isSafeUrl } from '@/utils/helpers'
import PublishDialog from '@/components/PublishDialog.vue'
import type { User, MainLink, NoteItem, FollowStatsVO } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)

const defaultAvatar = ''
const authorKeyword = ref('')
const noteKeyword = ref('')
const isSearching = computed(() => !!authorKeyword.value.trim() || !!noteKeyword.value.trim())
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

// 侧边栏折叠状态与持久化
const isSidebarCollapsed = ref(localStorage.getItem('sidebar_collapsed') === 'true')

function toggleSidebarCollapse() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
  localStorage.setItem('sidebar_collapsed', String(isSidebarCollapsed.value))
}

// 关注状态与加载
const currentFollowStats = ref<FollowStatsVO | null>(null)
const followActionLoading = ref(false)

// 个性签名即时编辑
const isEditingBio = ref(false)
const editBioText = ref('')
const bioSaving = ref(false)
const bioInputRef = ref<any>(null)

function startEditBio() {
  if (!isOwner.value) return
  editBioText.value = selectedUser.value?.bio || ''
  isEditingBio.value = true
  setTimeout(() => {
    bioInputRef.value?.focus()
  }, 50)
}

function cancelEditBio() {
  isEditingBio.value = false
  editBioText.value = ''
}

async function handleSaveBio() {
  if (!selectedUser.value || !isOwner.value) return
  const newBio = editBioText.value.trim()
  if (newBio.length > 30) {
    ElMessage.warning('个性签名不能超过30个字符')
    return
  }
  bioSaving.value = true
  try {
    const res = await updateUserBio(newBio)
    const savedBio = res.data.bio !== undefined ? res.data.bio : newBio
    selectedUser.value.bio = savedBio
    if (userStore.userInfo) {
      userStore.userInfo.bio = savedBio
    }
    // 同步更新侧边栏列表中对应用户的个性签名
    const found = userList.value.find(u => u.id === selectedUser.value?.id)
    if (found) {
      found.bio = savedBio
    }
    ElMessage.success('个性签名修改成功')
    isEditingBio.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '修改个性签名失败')
  } finally {
    bioSaving.value = false
  }
}

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

// 获取用户列表（登录状态返回已关注作者，支持关键词搜索全量作者）
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
    console.warn('API 调用失败（后端服务可能未启动）:', error)
    userList.value = [
      { id: 1, username: '张三', avatar: '', status: 1, lastActiveAt: new Date().toISOString(), noteCount: 5, followerCount: 2 },
      { id: 2, username: '李四', avatar: '', status: 1, lastActiveAt: new Date().toISOString(), noteCount: 3, followerCount: 10 },
      { id: 3, username: '王五', avatar: '', status: 1, lastActiveAt: new Date().toISOString(), noteCount: 8, followerCount: 1 },
    ]
    totalUsers.value = 3
  } finally {
    userLoading.value = false
  }
}

// 选择用户
async function selectUser(user: User) {
  isEditingBio.value = false
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
  mainLink.value = null
  noteList.value = []
  try {
    // 异步查询总链接
    try {
      const linkRes = await getMainLink(userId)
      mainLink.value = linkRes.data
    } catch { mainLink.value = null }

    // 异步查询关注状态与统计
    fetchFollowStats(userId)

    const notesRes = await getUserNotes(userId, {
      page: noteCurrentPage.value,
      size: notePageSize,
      keyword: localNoteKeyword.value || undefined,
      startDate: noteDateRange.value?.[0] || undefined,
      endDate: noteDateRange.value?.[1] || undefined,
    })
    noteList.value = notesRes.data.list
    totalNotes.value = notesRes.data.total
  } catch (error) {
    console.warn('获取笔记列表失败:', error)
  } finally {
    contentLoading.value = false
    loadFavorites()
  }
}

// 获取作者关注状态与统计数据
async function fetchFollowStats(userId: number) {
  try {
    const res = await getFollowStats(userId)
    currentFollowStats.value = res.data
  } catch {
    currentFollowStats.value = null
  }
}

// 关注 / 取消关注作者
async function toggleFollowAuthor() {
  if (!selectedUserId.value) return
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('关注作者需要先登录，是否前往登录？', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info',
    }).then(() => router.push('/login')).catch(() => {})
    return
  }

  followActionLoading.value = true
  try {
    if (currentFollowStats.value?.isFollowing) {
      await unfollowUser(selectedUserId.value)
      ElMessage.success('已取消关注')
      if (currentFollowStats.value) {
        currentFollowStats.value.isFollowing = false
        currentFollowStats.value.followerCount = Math.max(0, currentFollowStats.value.followerCount - 1)
      }
    } else {
      await followUser(selectedUserId.value)
      ElMessage.success('关注成功')
      if (currentFollowStats.value) {
        currentFollowStats.value.isFollowing = true
        currentFollowStats.value.followerCount = (currentFollowStats.value.followerCount || 0) + 1
      }
    }
    // 刷新侧边栏已关注作者列表
    fetchUsers()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    followActionLoading.value = false
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
    if (res.data.list.length === 1) {
      selectUser(res.data.list[0])
    }
  } catch {
    fetchUsers()
  } finally {
    userLoading.value = false
  }
}

function handleLogoClick() {
  clearAllSearch()
  localNoteKeyword.value = ''
  noteDateRange.value = null
  noteCurrentPage.value = 1
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function clearAllSearch() {
  authorKeyword.value = ''
  noteKeyword.value = ''
  currentPage.value = 1
  fetchUsers()
}

function handleClearAuthorSearch() {
  authorKeyword.value = ''
  currentPage.value = 1
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
      size: 50,
    })

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

    userList.value = Array.from(userMap.values())
    totalUsers.value = userList.value.length

    if (userList.value.length === 1) {
      selectUser(userList.value[0])
    }

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
  currentPage.value = 1
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
  if (note.noteType === 2) {
    router.push(`/note/editor/${note.id}`)
  } else {
    editingNote.value = note
    showPublishDialog.value = true
  }
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
  cursor: pointer;
  user-select: none;
  transition: opacity 0.2s, transform 0.2s;
}

.logo:hover {
  opacity: 0.85;
}

.logo:active {
  transform: scale(0.98);
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
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border);
  height: 52px;
  box-sizing: border-box;
}

.sidebar.collapsed .sidebar-header {
  justify-content: center;
  padding: 14px 8px;
}

.sidebar-title-group {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.sidebar-header h3 {
  font-size: 15px;
  color: var(--color-text-primary);
  font-weight: 600;
  margin: 0;
}

.user-count {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.clear-search-link {
  font-size: 12px;
  padding: 0;
  height: auto;
  margin-left: 4px;
}

.sidebar-actions {
  display: flex;
  align-items: center;
}

.collapse-btn {
  color: var(--color-text-secondary);
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: var(--color-primary);
}

.user-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.sidebar.collapsed .user-list {
  padding: 8px 4px;
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

.user-item.collapsed-item {
  justify-content: center;
  padding: 8px 0;
  margin: 4px 0;
  gap: 0;
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
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.self-tag {
  font-size: 10px;
  height: 18px;
  padding: 0 4px;
  line-height: 16px;
}

.user-meta {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
}

.meta-dot {
  margin: 0 4px;
  color: var(--color-border);
}

.user-meta .time {
  margin-left: auto;
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
  justify-content: space-between;
  margin-bottom: var(--space-lg);
  padding: var(--space-lg);
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  flex-wrap: wrap;
  gap: 16px;
}

.user-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.clickable-avatar {
  cursor: pointer;
  transition: transform 0.2s ease;
}

.clickable-avatar:hover {
  transform: scale(1.05);
}

.user-details-top {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.user-bio-wrapper {
  display: inline-flex;
  align-items: center;
}

.bio-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
  background: var(--color-bg-page, #f5f7fa);
  max-width: 380px;
  transition: all 0.2s ease;
  line-height: 1.4;
}

.bio-badge.editable-bio {
  cursor: pointer;
  border: 1px dashed #dcdfe6;
  background: #fafafa;
}

.bio-badge.editable-bio:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

.bio-badge.editable-bio .bio-placeholder {
  color: #909399;
  font-size: 12px;
}

.bio-badge.editable-bio:hover .bio-placeholder {
  color: #409eff;
}

.bio-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bio-icon {
  font-size: 13px;
  opacity: 0.7;
}

.bio-badge.editable-bio:hover .bio-icon {
  opacity: 1;
}

.bio-edit-inline {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.bio-input {
  width: 240px;
}

.user-details h2 {
  font-size: 20px;
  color: var(--color-text-primary);
  font-weight: 600;
  margin: 0;
}

.clickable-username {
  cursor: pointer;
  transition: color 0.2s;
}

.clickable-username:hover {
  color: var(--color-primary, #409eff);
}

.user-stats {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 6px;
  display: flex;
  align-items: center;
}

.stat-item strong {
  color: var(--color-text-primary);
  font-weight: 600;
}

.stat-divider {
  margin: 0 8px;
  color: var(--color-border);
}

.user-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
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
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.main-link-url {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main-link-arrow {
  color: var(--color-text-tertiary);
  font-size: 18px;
}

.main-link-alert {
  margin-bottom: var(--space-lg);
}

/* 笔记列表区 */
.notes-section {
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  box-shadow: var(--shadow-sm);
}

.notes-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-lg);
  flex-wrap: wrap;
  gap: 12px;
}

.notes-toolbar h3 {
  font-size: 16px;
  color: var(--color-text-primary);
  font-weight: 600;
  margin: 0;
}

.note-filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.note-title {
  font-weight: 500;
  color: var(--color-text-primary);
}

.clickable-title {
  cursor: pointer;
  transition: color 0.2s;
}

.clickable-title:hover {
  color: var(--color-primary, #409eff);
}

.note-article-summary {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s;
}

.note-article-summary:hover {
  color: var(--color-primary, #409eff);
}

.summary-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-primary);
  text-decoration: none;
  font-size: 13px;
}

.note-link:hover {
  text-decoration: underline;
}

.note-location {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.note-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .top-bar-left {
    gap: 8px;
  }
  .search-group {
    display: none;
  }
  .mobile-menu-btn {
    display: flex;
  }
  .sidebar {
    position: fixed;
    top: 56px;
    left: -280px;
    bottom: 0;
    z-index: 99;
    transition: left 0.3s ease;
  }
  .sidebar.drawer-open {
    left: 0;
  }
  .drawer-mask {
    position: fixed;
    top: 56px;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 98;
  }
}
</style>
