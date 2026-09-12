<template>
  <div class="profile-page">
    <div class="profile-header">
      <el-button @click="router.push('/')" :icon="ArrowLeft" circle size="small" />
      <h2>作者主页</h2>
    </div>

    <div v-if="loading" class="profile-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else-if="userInfo">
      <div class="profile-card">
        <img :src="userInfo.avatar || defaultAvatar" class="profile-avatar" />
        <div class="profile-info">
          <div class="profile-name-row">
            <h3>{{ userInfo.username }}</h3>
            <el-tag v-if="isOwner" size="small" type="success" effect="plain" round>我</el-tag>

            <!-- 30字个性签名（本人可点击即时修改） -->
            <div class="user-bio-wrapper">
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
              <template v-else>
                <div
                  v-if="isOwner"
                  class="bio-badge editable-bio"
                  @click="startEditBio"
                  title="点击修改个性签名 (最多30字)"
                >
                  <el-icon class="bio-icon"><EditPen /></el-icon>
                  <span v-if="userInfo.bio" class="bio-text">{{ userInfo.bio }}</span>
                  <span v-else class="bio-placeholder">添加个性签名...</span>
                </div>
                <div v-else-if="userInfo.bio" class="bio-badge">
                  <span class="bio-text">“{{ userInfo.bio }}”</span>
                </div>
              </template>
            </div>
          </div>
          <p class="profile-meta">
            <span><strong>{{ noteTotal }}</strong> 篇笔记</span>
            <span class="meta-dot">·</span>
            <span class="meta-fans-clickable" @click="openFansDialog" title="点击查看粉丝列表">
              <strong>{{ followStats?.followerCount ?? userInfo.followerCount ?? 0 }}</strong> 位粉丝
            </span>
            <span class="meta-dot" v-if="(followStats?.followingCount ?? userInfo.followingCount) !== undefined">·</span>
            <span v-if="(followStats?.followingCount ?? userInfo.followingCount) !== undefined"><strong>{{ followStats?.followingCount ?? userInfo.followingCount ?? 0 }}</strong> 关注</span>
            <span class="meta-dot" v-if="userInfo.lastActiveAt">·</span>
            <span v-if="userInfo.lastActiveAt">最后活跃 {{ formatDate(userInfo.lastActiveAt) }}</span>
          </p>
        </div>

        <div class="profile-actions">
          <template v-if="isOwner">
            <el-button round size="small" @click="openFansDialog">
              查看粉丝 ({{ followStats?.followerCount ?? userInfo.followerCount ?? 0 }})
            </el-button>
            <el-button round size="small" type="danger" plain @click="router.push('/settings?tab=recycle')">
              <el-icon><Delete /></el-icon>
              笔记回收站
            </el-button>
          </template>
          <template v-else>
            <el-button
              v-if="isLoggedIn"
              :type="followStats?.isFollowing ? 'default' : 'primary'"
              :icon="followStats?.isFollowing ? Check : Plus"
              :loading="followLoading"
              round
              @click="toggleFollow"
            >
              {{ followStats?.isFollowing ? '已关注' : '关注作者' }}
            </el-button>
            <el-button
              v-else
              type="primary"
              :icon="Plus"
              round
              @click="router.push('/login')"
            >
              关注作者
            </el-button>
          </template>
        </div>
      </div>

      <!-- 主链接 -->
      <div v-if="mainLink" class="main-link-card" @click="openLink(mainLink.mainUrl)">
        <el-icon><Link /></el-icon>
        <span class="main-link-url">{{ mainLink.mainUrl }}</span>
        <el-icon><ArrowRight /></el-icon>
      </div>

      <!-- 笔记列表 -->
      <div class="profile-notes">
        <h4>{{ isOwner ? '我的笔记' : 'TA 的笔记' }}</h4>
        <el-table :data="notes" stripe v-loading="notesLoading">
          <el-table-column prop="title" label="标题" min-width="180">
            <template #default="{ row }">
              <el-tag :type="row.noteType === 2 ? 'success' : 'info'" size="small" style="margin-right: 6px;">
                {{ row.noteType === 2 ? '原创' : '外链' }}
              </el-tag>
              <el-tag v-if="row.isPublic === 0" type="danger" effect="plain" size="small" style="margin-right: 6px;">
                <el-icon style="margin-right: 2px; vertical-align: -1px;"><Lock /></el-icon>私密
              </el-tag>
              <span
                :style="row.noteType === 2 ? 'cursor: pointer; color: #409eff;' : ''"
                @click="row.noteType === 2 && router.push(`/note/${row.id}`)"
              >
                {{ row.title }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="locationOrUrl" label="内容/位置" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span
                v-if="row.noteType === 2"
                style="cursor: pointer; color: #606266;"
                @click="router.push(`/note/${row.id}`)"
              >
                {{ row.summary || '在线创作笔记' }}
              </span>
              <a v-else-if="isUrl(row.locationOrUrl)" :href="row.locationOrUrl" target="_blank" rel="noopener">{{ row.locationOrUrl }}</a>
              <span v-else>{{ row.locationOrUrl }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览" width="80" />
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ formatDate(row.updatedAt || row.createdAt) }}</template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="noteTotal > 0"
          v-model:current-page="notePage"
          v-model:page-size="noteSize"
          :total="noteTotal"
          :page-sizes="[10, 20]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchNotes"
          @current-change="fetchNotes"
          style="margin-top: 16px; justify-content: flex-end;"
        />
      </div>

      <!-- 粉丝列表弹窗 -->
      <el-dialog
        v-model="fansDialogVisible"
        :title="`${isOwner ? '我' : (userInfo?.username || '')} 的粉丝`"
        width="500px"
        append-to-body
      >
        <div v-loading="fansLoading" class="dialog-fans-container">
          <template v-if="dialogFansList.length > 0">
            <div
              v-for="fan in dialogFansList"
              :key="fan.id"
              class="dialog-fan-card"
              @click="handleGoFanProfile(fan.id)"
            >
              <el-avatar :size="44" :src="fan.avatar || defaultAvatar" class="dialog-fan-avatar">
                {{ fan.username?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="dialog-fan-info">
                <div class="dialog-fan-name-row">
                  <span class="dialog-fan-name">{{ fan.username }}</span>
                  <span v-if="fan.bio" class="dialog-fan-bio" :title="fan.bio">“{{ fan.bio }}”</span>
                </div>
                <div class="dialog-fan-meta">
                  <span>{{ fan.noteCount || 0 }} 篇笔记</span>
                  <span class="meta-dot">·</span>
                  <span>{{ fan.followerCount || 0 }} 粉丝</span>
                </div>
              </div>
              <div v-if="isLoggedIn && fan.id !== userStore.userInfo?.id" class="dialog-fan-action" @click.stop>
                <el-button
                  v-if="fan.isFollowing"
                  type="success"
                  plain
                  size="small"
                  round
                  :loading="followActionLoadingId === fan.id"
                  @click="handleToggleFollowFan(fan)"
                >
                  <el-icon><Check /></el-icon>
                  互相关注
                </el-button>
                <el-button
                  v-else
                  type="primary"
                  size="small"
                  round
                  :loading="followActionLoadingId === fan.id"
                  @click="handleToggleFollowFan(fan)"
                >
                  <el-icon><Plus /></el-icon>
                  回关
                </el-button>
              </div>
            </div>
          </template>
          <el-empty v-else description="暂无粉丝关注" :image-size="80" />
        </div>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Link, ArrowRight, Lock, Plus, Check, EditPen, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateUserBio, getUserProfile } from '@/api/user'
import { getMainLink, getUserNotes, followUser, unfollowUser, getFollowStats, getFollowers } from '@/api/note'
import type { User, MainLink, NoteItem, FollowStatsVO } from '@/types'
import { isUrl, formatDate } from '@/utils/helpers'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const isOwner = computed(() => isLoggedIn.value && Number(route.params.id) === userStore.userInfo?.id)

const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" fill="%23409eff"/><text x="50" y="55" text-anchor="middle" fill="white" font-size="40">U</text></svg>')

const loading = ref(true)
const userInfo = ref<User | null>(null)
const mainLink = ref<MainLink | null>(null)
const followStats = ref<FollowStatsVO | null>(null)
const followLoading = ref(false)
const notes = ref<NoteItem[]>([])
const noteTotal = ref(0)
const notePage = ref(1)
const noteSize = ref(10)
const notesLoading = ref(false)

// 粉丝弹窗相关
const fansDialogVisible = ref(false)
const fansLoading = ref(false)
const dialogFansList = ref<User[]>([])
const followActionLoadingId = ref<number | null>(null)

// 个性签名即时编辑
const isEditingBio = ref(false)
const editBioText = ref('')
const bioSaving = ref(false)
const bioInputRef = ref<any>(null)

function startEditBio() {
  if (!isOwner.value) return
  editBioText.value = userInfo.value?.bio || ''
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
  if (!userInfo.value || !isOwner.value) return
  const newBio = editBioText.value.trim()
  if (newBio.length > 30) {
    ElMessage.warning('个性签名不能超过30个字符')
    return
  }
  bioSaving.value = true
  try {
    const res = await updateUserBio(newBio)
    const savedBio = res.data.bio !== undefined ? res.data.bio : newBio
    userInfo.value.bio = savedBio
    if (userStore.userInfo) {
      userStore.userInfo.bio = savedBio
    }
    ElMessage.success('个性签名修改成功')
    isEditingBio.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '修改个性签名失败')
  } finally {
    bioSaving.value = false
  }
}

const fetchProfile = async () => {
  const userId = Number(route.params.id)
  if (!userId || isNaN(userId)) {
    userInfo.value = null
    loading.value = false
    return
  }
  loading.value = true
  try {
    // 异步查询作者关注统计与状态
    fetchFollowStats(userId)

    // 直接请求该用户主页公开信息
    const res = await getUserProfile(userId)
    userInfo.value = res.data
    if (!followStats.value && res.data) {
      followStats.value = {
        userId,
        isFollowing: res.data.isFollowing || false,
        followerCount: res.data.followerCount || 0,
        followingCount: (res.data as any).followingCount || 0,
      }
    }

    // 获取主链接
    try {
      const linkRes = await getMainLink(userId)
      mainLink.value = linkRes.data
    } catch { mainLink.value = null }

    await fetchNotes()
  } catch {
    userInfo.value = null
  } finally {
    loading.value = false
  }
}

const fetchFollowStats = async (userId: number) => {
  try {
    const res = await getFollowStats(userId)
    followStats.value = res.data
  } catch {
    followStats.value = null
  }
}

const toggleFollow = async () => {
  const userId = Number(route.params.id)
  if (!userId) return
  followLoading.value = true
  try {
    if (followStats.value?.isFollowing) {
      await unfollowUser(userId)
      ElMessage.success('已取消关注')
      if (followStats.value) {
        followStats.value.isFollowing = false
        followStats.value.followerCount = Math.max(0, followStats.value.followerCount - 1)
      }
    } else {
      await followUser(userId)
      ElMessage.success('关注成功')
      if (followStats.value) {
        followStats.value.isFollowing = true
        followStats.value.followerCount = (followStats.value.followerCount || 0) + 1
      }
    }
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    followLoading.value = false
  }
}

const fetchNotes = async () => {
  const userId = Number(route.params.id)
  notesLoading.value = true
  try {
    const res = await getUserNotes(userId, { page: notePage.value, size: noteSize.value })
    notes.value = res.data.list
    noteTotal.value = res.data.total
  } catch { /* handled */ }
  finally { notesLoading.value = false }
}

const openLink = (url: string) => {
  if (isUrl(url)) window.open(url, '_blank', 'noopener')
}

// 打开粉丝列表弹窗
const openFansDialog = async () => {
  const userId = Number(route.params.id)
  if (!userId) return
  fansDialogVisible.value = true
  fansLoading.value = true
  try {
    const res = await getFollowers({ userId, page: 1, size: 100 })
    dialogFansList.value = res.data?.list || []
  } catch {
    dialogFansList.value = []
  } finally {
    fansLoading.value = false
  }
}

// 弹窗中回关/取消关注粉丝
const handleToggleFollowFan = async (fan: User) => {
  if (!isLoggedIn.value) {
    router.push('/login')
    return
  }
  followActionLoadingId.value = fan.id
  try {
    if (fan.isFollowing) {
      await unfollowUser(fan.id)
      ElMessage.success('已取消关注')
      fan.isFollowing = false
      if (Number(route.params.id) === fan.id && followStats.value) {
        followStats.value.isFollowing = false
        followStats.value.followerCount = Math.max(0, (followStats.value.followerCount || 1) - 1)
      }
    } else {
      await followUser(fan.id)
      ElMessage.success('关注成功')
      fan.isFollowing = true
      if (Number(route.params.id) === fan.id && followStats.value) {
        followStats.value.isFollowing = true
        followStats.value.followerCount = (followStats.value.followerCount || 0) + 1
      }
    }
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    followActionLoadingId.value = null
  }
}

// 点击粉丝条目跳转主页
const handleGoFanProfile = (fanId: number) => {
  fansDialogVisible.value = false
  router.push(`/user/${fanId}`)
}

watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      notePage.value = 1
      fetchProfile()
    }
  }
)

onMounted(fetchProfile)
</script>

<style scoped>
.profile-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 20px;
}
.profile-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.profile-header h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}
.profile-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 24px;
  background: var(--color-bg-card, #fff);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.profile-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  background: #e8e8e8;
  flex-shrink: 0;
}
.profile-info {
  flex: 1;
  min-width: 200px;
}
.profile-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.profile-name-row h3 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
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
  color: var(--color-text-secondary, #606266);
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
.profile-meta {
  font-size: 13px;
  color: var(--color-text-secondary, #666);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.profile-meta strong {
  color: var(--color-text-primary, #333);
}
.meta-dot {
  color: var(--color-border, #dcdfe6);
  margin: 0 4px;
}
.profile-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.main-link-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: var(--color-bg-card, #fff);
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.main-link-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}
.main-link-url {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-primary, #409eff);
  font-size: 14px;
}
.profile-notes h4 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.meta-fans-clickable {
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
}
.meta-fans-clickable:hover {
  background-color: var(--el-color-primary-light-9, #ecf5ff);
  color: var(--color-primary, #409eff);
}

.dialog-fans-container {
  max-height: 420px;
  overflow-y: auto;
  padding: 4px 6px;
}

.dialog-fan-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 8px;
  background: var(--color-bg-page, #f8f9fa);
  margin-bottom: 10px;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.15s;
}

.dialog-fan-card:hover {
  background-color: #f0f2f5;
  transform: translateY(-1px);
}

.dialog-fan-avatar {
  flex-shrink: 0;
}

.dialog-fan-info {
  flex: 1;
  min-width: 0;
}

.dialog-fan-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dialog-fan-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary, #303133);
}

.dialog-fan-bio {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 170px;
}

.dialog-fan-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  display: flex;
  align-items: center;
}

.dialog-fan-action {
  flex-shrink: 0;
}
</style>
