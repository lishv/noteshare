<template>
  <div class="settings-page">
    <div class="settings-container">
      <h2>个人设置</h2>
      <el-tabs v-model="activeTab">
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
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { changePassword, sendVerificationCode, uploadAvatar } from '@/api/user'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const userInfo = ref(userStore.userInfo)
const activeTab = ref('avatar')
const defaultAvatar = ''
const avatarUrl = ref(userInfo.value?.avatar || '')
const previewUrl = ref('')
const selectedFile = ref<File | null>(null)
const uploadLoading = ref(false)

const hasUnsavedChanges = computed(() =>
  !!(passwordForm.oldPassword || passwordForm.newPassword || passwordForm.confirmPassword ||
     resetForm.email || resetForm.verificationCode || resetForm.newPassword || resetForm.confirmPassword ||
     selectedFile.value)
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
    avatarUrl.value = res.data.avatar
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
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
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
</style>
