<template>
  <div class="settings-page">
    <div class="settings-container" :class="{ 'settings-container-wide': activeTab === 'recycle' }">
      <h2>个人设置</h2>
      <el-tabs v-model="activeTab">
        <!-- 个人资料/个性签名 -->
        <el-tab-pane label="个人资料" name="profile">
          <div class="settings-form-wrapper">
            <el-form label-width="100px" class="settings-form">
              <el-form-item label="用户名">
                <el-input :model-value="userInfo?.username" disabled />
              </el-form-item>
              <el-form-item label="电子邮箱">
                <el-input :model-value="userInfo?.email" disabled />
              </el-form-item>
              <el-form-item label="个性签名">
                <el-input
                  v-model="userBio"
                  maxlength="30"
                  show-word-limit
                  placeholder="请输入个性签名（最多30字）"
                  clearable
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="bioLoading" @click="handleUpdateBio">
                  保存个性签名
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 我的粉丝 -->
        <el-tab-pane label="我的粉丝" name="fans">
          <div class="fans-section">
            <div class="fans-header">
              <span class="fans-count-tip">共有 <strong>{{ fansTotal }}</strong> 位粉丝关注了您</span>
              <el-button size="small" :icon="Refresh" @click="fetchFans" :loading="fansLoading" circle title="刷新粉丝列表" />
            </div>

            <div v-loading="fansLoading" class="fans-container">
              <template v-if="fansList.length > 0">
                <div
                  v-for="fan in fansList"
                  :key="fan.id"
                  class="fan-item-card"
                  @click="handleGoUserProfile(fan.id)"
                >
                  <el-avatar :size="48" :src="fan.avatar || defaultAvatar" class="fan-avatar">
                    {{ fan.username?.charAt(0) || 'U' }}
                  </el-avatar>
                  <div class="fan-detail">
                    <div class="fan-name-row">
                      <span class="fan-name">{{ fan.username }}</span>
                      <span v-if="fan.bio" class="fan-bio-bubble" :title="fan.bio">“{{ fan.bio }}”</span>
                    </div>
                    <div class="fan-meta-info">
                      <span>{{ fan.noteCount || 0 }} 篇笔记</span>
                      <span class="meta-dot">·</span>
                      <span>{{ fan.followerCount || 0 }} 位粉丝</span>
                    </div>
                  </div>
                  <div class="fan-follow-btn-wrapper" @click.stop>
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

                <div class="fans-pagination" v-if="fansTotal > fansPageSize">
                  <el-pagination
                    v-model:current-page="fansPage"
                    :page-size="fansPageSize"
                    :total="fansTotal"
                    layout="prev, pager, next"
                    small
                    @current-change="fetchFans"
                  />
                </div>
              </template>

              <el-empty
                v-else
                description="暂无粉丝关注，多多发布优质笔记吸引粉丝吧~"
                :image-size="90"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 笔记回收站 -->
        <el-tab-pane label="笔记回收站" name="recycle">
          <div class="recycle-section">
            <div class="recycle-header">
              <div class="recycle-tip">
                <el-icon class="tip-icon"><Delete /></el-icon>
                <span>回收站内展示已删除的笔记，支持一键恢复或彻底永久销毁。</span>
              </div>
              <div class="recycle-actions">
                <el-button
                  size="small"
                  :icon="Refresh"
                  @click="fetchRecycleBin"
                  :loading="recycleLoading"
                  circle
                  title="刷新回收站"
                />
                <el-button
                  v-if="recycleTotal > 0"
                  type="danger"
                  plain
                  size="small"
                  :loading="clearLoading"
                  @click="handleClearRecycleBin"
                >
                  <el-icon><Delete /></el-icon>
                  清空回收站
                </el-button>
              </div>
            </div>

            <div v-loading="recycleLoading" class="recycle-container">
              <template v-if="recycleList.length > 0">
                <div v-for="item in recycleList" :key="item.id" class="recycle-item-card">
                  <div class="recycle-item-main">
                    <div class="recycle-item-title-row">
                      <el-tag :type="item.noteType === 2 ? 'success' : 'info'" size="small">
                        {{ item.noteType === 2 ? '原创' : '外链' }}
                      </el-tag>
                      <el-tag v-if="item.isPublic === 0" type="danger" effect="plain" size="small">
                        私密
                      </el-tag>
                      <span class="recycle-item-title" :title="item.title">{{ item.title }}</span>
                    </div>
                    <p class="recycle-item-summary" v-if="item.summary || item.locationOrUrl">
                      {{ item.summary || item.locationOrUrl }}
                    </p>
                    <div class="recycle-item-meta">
                      <span>删除时间：{{ item.deletedAt ? item.deletedAt.replace('T', ' ').substring(0, 19) : (item.updatedAt ? item.updatedAt.replace('T', ' ').substring(0, 19) : '-') }}</span>
                      <span class="meta-dot">·</span>
                      <span>浏览量：{{ item.viewCount || 0 }}</span>
                    </div>
                  </div>
                  <div class="recycle-item-ops">
                    <el-button
                      type="primary"
                      size="small"
                      plain
                      :loading="actionLoadingId === item.id"
                      @click="handleRestoreNote(item)"
                    >
                      <el-icon><RefreshRight /></el-icon>
                      恢复
                    </el-button>
                    <el-button
                      type="danger"
                      size="small"
                      plain
                      :loading="actionLoadingId === item.id"
                      @click="handleHardDeleteNote(item)"
                    >
                      <el-icon><Delete /></el-icon>
                      彻底销毁
                    </el-button>
                  </div>
                </div>

                <div class="recycle-pagination" v-if="recycleTotal > recyclePageSize">
                  <el-pagination
                    v-model:current-page="recyclePage"
                    :page-size="recyclePageSize"
                    :total="recycleTotal"
                    layout="prev, pager, next"
                    small
                    @current-change="fetchRecycleBin"
                  />
                </div>
              </template>

              <el-empty
                v-else
                description="回收站空空如也，暂无已删除笔记"
                :image-size="90"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 修改头像 -->
        <el-tab-pane label="修改头像" name="avatar">
          <div class="avatar-section">
            <div class="avatar-preview">
              <el-avatar :size="120" :src="avatarUrl || defaultAvatar">
                {{ userInfo?.username?.charAt(0) || 'U' }}
              </el-avatar>
            </div>
            <el-upload
              class="avatar-uploader"
              action=""
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleFileChange"
              accept="image/*"
            >
              <el-button type="primary">选择图片</el-button>
            </el-upload>
            <div class="avatar-tips">
              <p>支持 JPG、PNG 格式，大小不超过 2MB</p>
            </div>
            <el-button
              v-if="previewUrl"
              type="success"
              :loading="uploadLoading"
              @click="handleUploadAvatar"
            >
              确认上传
            </el-button>
          </div>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            class="settings-form"
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码（至少6位）"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 找回密码 -->
        <el-tab-pane label="找回密码" name="reset">
          <el-form
            ref="resetFormRef"
            :model="resetForm"
            :rules="resetRules"
            label-width="100px"
            class="settings-form"
          >
            <el-form-item label="注册邮箱" prop="email">
              <el-input
                v-model="resetForm.email"
                placeholder="请输入注册时使用的邮箱"
              />
            </el-form-item>
            <el-form-item prop="verificationCode">
              <div class="code-input-wrapper">
                <el-input
                  v-model="resetForm.verificationCode"
                  placeholder="邮箱验证码"
                />
                <el-button
                  type="primary"
                  :disabled="resetCountdown > 0 || !resetForm.email"
                  :loading="resetCodeLoading"
                  @click="handleSendResetCode"
                >
                  {{ resetCountdown > 0 ? `${resetCountdown}s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="resetForm.newPassword"
                type="password"
                placeholder="请输入新密码（至少6位）"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="resetForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="resetLoading" @click="handleResetPassword">
                重置密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <el-button text @click="$router.push('/')">
        <el-icon><ArrowLeft /></el-icon>
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, Check, Plus, Refresh, Delete, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { changePassword, sendVerificationCode, uploadAvatar, updateUserBio } from '@/api/user'
import {
  getFollowers,
  followUser,
  unfollowUser,
  getRecycleBinNotes,
  restoreRecycleNote,
  hardDeleteRecycleNote,
  clearRecycleBin,
} from '@/api/note'
import type { User, NoteItem } from '@/types'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const userInfo = ref(userStore.userInfo)
const activeTab = ref(route.query.tab ? String(route.query.tab) : 'profile')
const defaultAvatar = ''
const avatarUrl = ref(userInfo.value?.avatar || '')
const previewUrl = ref('')
const selectedFile = ref<File | null>(null)
const uploadLoading = ref(false)

// ========== 我的粉丝与回关 ==========
const fansList = ref<User[]>([])
const fansLoading = ref(false)
const fansTotal = ref(0)
const fansPage = ref(1)
const fansPageSize = ref(10)
const followActionLoadingId = ref<number | null>(null)

async function fetchFans() {
  fansLoading.value = true
  try {
    const res = await getFollowers({ page: fansPage.value, size: fansPageSize.value })
    fansList.value = res.data.list || []
    fansTotal.value = res.data.total || 0
  } catch (e) {
    // handled
  } finally {
    fansLoading.value = false
  }
}

async function handleToggleFollowFan(fan: User) {
  if (fan.isFollowing) {
    try {
      await ElMessageBox.confirm(`确定取消关注粉丝 "${fan.username}" 吗？`, '提示', {
        type: 'warning',
        confirmButtonText: '确定取关',
        cancelButtonText: '取消',
      })
      followActionLoadingId.value = fan.id
      await unfollowUser(fan.id)
      fan.isFollowing = false
      ElMessage.success(`已取消关注 ${fan.username}`)
    } catch {
      // 用户取消弹窗
    } finally {
      followActionLoadingId.value = null
    }
  } else {
    followActionLoadingId.value = fan.id
    try {
      await followUser(fan.id)
      fan.isFollowing = true
      ElMessage.success(`回关成功！已与 ${fan.username} 互相关注`)
    } catch {
      // 错误由拦截器统一提示
    } finally {
      followActionLoadingId.value = null
    }
  }
}

function handleGoUserProfile(userId: number) {
  router.push(`/user/${userId}`)
}

// ========== 笔记回收站 ==========
const recycleList = ref<NoteItem[]>([])
const recycleLoading = ref(false)
const recycleTotal = ref(0)
const recyclePage = ref(1)
const recyclePageSize = ref(10)
const actionLoadingId = ref<number | null>(null)
const clearLoading = ref(false)

async function fetchRecycleBin() {
  recycleLoading.value = true
  try {
    const res = await getRecycleBinNotes({ page: recyclePage.value, size: recyclePageSize.value })
    recycleList.value = res.data.list || []
    recycleTotal.value = res.data.total || 0
  } catch (e) {
    // handled
  } finally {
    recycleLoading.value = false
  }
}

async function handleRestoreNote(item: NoteItem) {
  try {
    actionLoadingId.value = item.id
    await restoreRecycleNote(item.id)
    ElMessage.success(`笔记《${item.title}》已成功恢复`)
    // 刷新回收站，如果当前页空了且不是第1页则往前翻一页
    if (recycleList.value.length === 1 && recyclePage.value > 1) {
      recyclePage.value--
    }
    await fetchRecycleBin()
  } catch {
    // error handled
  } finally {
    actionLoadingId.value = null
  }
}

async function handleHardDeleteNote(item: NoteItem) {
  try {
    await ElMessageBox.confirm(
      `确定要彻底物理销毁笔记《${item.title}》吗？此操作不可逆，将同时清除该笔记所有的评论、点赞和收藏数据！`,
      '永久销毁警告',
      {
        type: 'warning',
        confirmButtonText: '确定彻底销毁',
        confirmButtonClass: 'el-button--danger',
        cancelButtonText: '取消',
      }
    )
    actionLoadingId.value = item.id
    await hardDeleteRecycleNote(item.id)
    ElMessage.success(`笔记《${item.title}》已被彻底销毁`)
    if (recycleList.value.length === 1 && recyclePage.value > 1) {
      recyclePage.value--
    }
    await fetchRecycleBin()
  } catch {
    // 取消或错误
  } finally {
    actionLoadingId.value = null
  }
}

async function handleClearRecycleBin() {
  try {
    await ElMessageBox.confirm(
      '确定要清空回收站内的所有笔记吗？所有被删除的笔记及其评论、点赞数据都将被永久销毁且不可恢复！',
      '清空回收站警告',
      {
        type: 'warning',
        confirmButtonText: '确定全部清空',
        confirmButtonClass: 'el-button--danger',
        cancelButtonText: '取消',
      }
    )
    clearLoading.value = true
    await clearRecycleBin()
    ElMessage.success('回收站已成功清空')
    recyclePage.value = 1
    await fetchRecycleBin()
  } catch {
    // 取消
  } finally {
    clearLoading.value = false
  }
}

watch(activeTab, (newTab) => {
  if (newTab === 'fans') {
    fetchFans()
  } else if (newTab === 'recycle') {
    fetchRecycleBin()
  }
})

// 个性签名
const userBio = ref(userInfo.value?.bio || '')
const bioLoading = ref(false)

async function handleUpdateBio() {
  const bio = userBio.value ? userBio.value.trim() : ''
  if (bio.length > 30) {
    ElMessage.warning('个性签名不能超过30个字符')
    return
  }
  bioLoading.value = true
  try {
    const res = await updateUserBio(bio)
    userStore.setUserInfo(res.data)
    userInfo.value = res.data
    userBio.value = res.data.bio || ''
    ElMessage.success('个性签名修改成功')
  } catch {
    // 错误由拦截器统一处理
  } finally {
    bioLoading.value = false
  }
}

const hasUnsavedChanges = computed(() =>
  !!(passwordForm.oldPassword || passwordForm.newPassword || passwordForm.confirmPassword ||
     resetForm.email || resetForm.verificationCode || resetForm.newPassword || resetForm.confirmPassword ||
     selectedFile.value || (userBio.value !== (userInfo.value?.bio || '')))
)

function beforeUnloadHandler(e: BeforeUnloadEvent) {
  if (hasUnsavedChanges.value) {
    e.preventDefault()
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', beforeUnloadHandler)
  userStore.fetchUserInfo().then(() => {
    userInfo.value = userStore.userInfo
    avatarUrl.value = userInfo.value?.avatar || ''
    userBio.value = userInfo.value?.bio || ''
    if (activeTab.value === 'fans') {
      fetchFans()
    } else if (activeTab.value === 'recycle') {
      fetchRecycleBin()
    }
  })
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', beforeUnloadHandler)
})

// 头像上传
function handleFileChange(uploadFile: UploadFile) {
  const raw = uploadFile.raw
  if (!raw) return

  if (!raw.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }
  if (raw.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过2MB')
    return
  }

  selectedFile.value = raw
  previewUrl.value = URL.createObjectURL(raw)
  avatarUrl.value = previewUrl.value
}

async function handleUploadAvatar() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择图片')
    return
  }

  uploadLoading.value = true
  try {
    const res = await uploadAvatar(selectedFile.value)
    avatarUrl.value = res.data.avatar || ''
    userStore.setUserInfo(res.data)
    ElMessage.success('头像修改成功')
    previewUrl.value = ''
    selectedFile.value = null
  } catch {
    // 错误已由拦截器处理
  } finally {
    uploadLoading.value = false
  }
}

// 修改密码（通过旧密码）
const passwordFormRef = ref<FormInstance>()
const passwordLoading = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    userStore.logout()
    router.push('/login')
  } catch {
    // 错误已由拦截器处理
  } finally {
    passwordLoading.value = false
  }
}

// 找回密码（通过邮箱验证码）
const resetFormRef = ref<FormInstance>()
const resetLoading = ref(false)
const resetCodeLoading = ref(false)
const resetCountdown = ref(0)
let resetCountdownTimer: ReturnType<typeof setInterval> | null = null

const resetForm = reactive({
  email: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: '',
})

const validateResetConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== resetForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const resetRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateResetConfirmPassword, trigger: 'blur' },
  ],
}

function startResetCountdown() {
  resetCountdown.value = 60
  resetCountdownTimer = setInterval(() => {
    resetCountdown.value--
    if (resetCountdown.value <= 0) {
      if (resetCountdownTimer) clearInterval(resetCountdownTimer)
      resetCountdownTimer = null
    }
  }, 1000)
}

async function handleSendResetCode() {
  if (!resetForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  resetCodeLoading.value = true
  try {
    await sendVerificationCode({ email: resetForm.email, scene: 'changePassword' })
    ElMessage.success('验证码已发送')
    startResetCountdown()
  } catch {
    // 错误已由拦截器处理
  } finally {
    resetCodeLoading.value = false
  }
}

async function handleResetPassword() {
  const valid = await resetFormRef.value?.validate().catch(() => false)
  if (!valid) return

  resetLoading.value = true
  try {
    await changePassword({
      email: resetForm.email,
      verificationCode: resetForm.verificationCode,
      newPassword: resetForm.newPassword,
    })
    ElMessage.success('密码重置成功，请重新登录')
    userStore.logout()
    router.push('/login')
  } catch {
    // 错误已由拦截器处理
  } finally {
    resetLoading.value = false
  }
}
</script>

<style scoped>
.settings-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--color-bg-page);
}

.settings-container {
  max-width: 560px;
  width: 90%;
  padding: var(--space-xl);
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  transition: max-width 0.3s ease;
}

.settings-container.settings-container-wide {
  max-width: 780px;
}

.settings-container h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-md);
  text-align: left;
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--color-border);
}

.settings-container :deep(.el-tabs__nav-wrap::after) {
  background-color: var(--color-border);
}

.settings-container :deep(.el-tabs__active-bar) {
  background-color: var(--color-primary);
}

.settings-container :deep(.el-tabs__item.is-active) {
  color: var(--color-primary);
}

.settings-container :deep(.el-tabs__item:hover) {
  color: var(--color-primary-light);
}

.settings-container :deep(.el-tabs__item) {
  color: var(--color-text-secondary);
  font-weight: 500;
}

.settings-form {
  max-width: 400px;
  margin: var(--space-lg) auto;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-lg);
  padding: var(--space-xl) 0;
}

.avatar-preview :deep(.el-avatar) {
  border: 3px solid var(--color-border);
}

.avatar-tips {
  font-size: 12px;
  color: var(--color-text-tertiary);
  text-align: center;
}

.avatar-tips p {
  margin: 0;
}

.code-input-wrapper {
  display: flex;
  gap: 8px;
  width: 100%;
}

.code-input-wrapper .el-input {
  flex: 1;
}

.code-input-wrapper .el-button {
  white-space: nowrap;
}

/* 粉丝列表样式 */
.fans-section {
  padding: 8px 0;
}

.fans-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--color-border);
}

.fans-count-tip {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.fans-count-tip strong {
  color: var(--color-primary);
  font-size: 16px;
}

.fans-container {
  min-height: 180px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.fan-item-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  background: var(--color-bg-subtle, #f9fafb);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: all 0.2s ease;
}

.fan-item-card:hover {
  background: #ffffff;
  border-color: var(--color-primary-light, #409eff);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.fan-avatar {
  flex-shrink: 0;
  background: var(--color-primary-light, #ecf5ff);
  color: var(--color-primary, #409eff);
  font-weight: 600;
}

.fan-detail {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fan-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.fan-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary, #303133);
}

.fan-bio-bubble {
  font-size: 12px;
  color: var(--color-text-secondary, #606266);
  background: rgba(64, 158, 255, 0.08);
  padding: 2px 8px;
  border-radius: 12px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fan-meta-info {
  font-size: 12px;
  color: var(--color-text-tertiary, #909399);
  display: flex;
  align-items: center;
  gap: 6px;
}

.meta-dot {
  color: #c0c4cc;
}

.fan-follow-btn-wrapper {
  flex-shrink: 0;
}

.fans-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

/* 回收站样式 */
.recycle-section {
  padding: 8px 0;
}

.recycle-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--color-border);
}

.recycle-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.recycle-tip .tip-icon {
  color: #e6a23c;
  font-size: 16px;
}

.recycle-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.recycle-container {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recycle-item-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  background: var(--color-bg-subtle, #f9fafb);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  transition: all 0.2s ease;
}

.recycle-item-card:hover {
  background: #ffffff;
  border-color: #f56c6c;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.08);
}

.recycle-item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.recycle-item-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.recycle-item-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary, #303133);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recycle-item-summary {
  font-size: 13px;
  color: var(--color-text-secondary, #606266);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recycle-item-meta {
  font-size: 12px;
  color: var(--color-text-tertiary, #909399);
  display: flex;
  align-items: center;
  gap: 6px;
}

.recycle-item-ops {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.recycle-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
