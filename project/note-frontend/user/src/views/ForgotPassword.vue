<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo" @click="router.push('/')" title="返回首页">
        <div class="logo-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect x="2" y="2" width="24" height="24" rx="6" fill="var(--color-primary)" />
            <path d="M9 10h10M9 14h7M9 18h4" stroke="#fff" stroke-width="2" stroke-linecap="round" />
          </svg>
        </div>
        <span class="logo-text">笔记共享平台</span>
      </div>

      <h2 class="auth-title">重置密码</h2>
      <p class="auth-subtitle">通过注册邮箱验证身份，设置新的登录密码</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @submit.prevent="handleReset"
      >
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入注册邮箱"
            :prefix-icon="Message"
            autocomplete="email"
          />
        </el-form-item>

        <el-form-item prop="verificationCode">
          <div class="code-input-wrapper">
            <el-input
              v-model="form.verificationCode"
              placeholder="6位邮箱验证码"
              :prefix-icon="Key"
              maxlength="6"
              autocomplete="one-time-code"
            />
            <el-button
              type="primary"
              class="send-code-btn"
              :disabled="codeCountdown > 0 || !form.email.trim()"
              :loading="codeLoading"
              @click="handleSendCode"
            >
              {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="请输入新密码（至少6位）"
            :prefix-icon="Lock"
            autocomplete="new-password"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            :prefix-icon="Lock"
            autocomplete="new-password"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="resetLoading"
            @click="handleReset"
          >
            重置密码
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        想起密码了？<router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Key, Lock, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { resetPassword, sendVerificationCode } from '@/api/user'

const router = useRouter()
const formRef = ref<FormInstance>()
const resetLoading = ref(false)
const codeLoading = ref(false)
const codeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  email: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码必须为6位数字', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度为6-100个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

function startCountdown() {
  if (countdownTimer) clearInterval(countdownTimer)
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    codeCountdown.value -= 1
    if (codeCountdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function handleSendCode() {
  const email = form.email.trim()
  if (!email) {
    ElMessage.warning('请先输入邮箱')
    return
  }

  codeLoading.value = true
  try {
    await sendVerificationCode({ email, scene: 'resetPassword' })
    ElMessage.success('如果该邮箱已注册，验证码将发送至你的邮箱')
    startCountdown()
  } catch {
    // 错误已由请求拦截器处理
  } finally {
    codeLoading.value = false
  }
}

async function handleReset() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  resetLoading.value = true
  try {
    await resetPassword({
      email: form.email.trim(),
      verificationCode: form.verificationCode.trim(),
      newPassword: form.newPassword,
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } catch {
    // 错误已由请求拦截器处理
  } finally {
    resetLoading.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 16px 0;
  background: var(--color-bg-page);
  position: relative;
  overflow: hidden;
}

.auth-page::before {
  content: '';
  position: absolute;
  top: -20%;
  right: -10%;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: var(--color-primary-lighter);
  opacity: 0.5;
}

.auth-page::after {
  content: '';
  position: absolute;
  bottom: -15%;
  left: -5%;
  width: 350px;
  height: 350px;
  border-radius: 50%;
  background: var(--color-primary-lighter);
  opacity: 0.3;
}

.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 460px;
  padding: 40px 36px;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.auth-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
  cursor: pointer;
  user-select: none;
  transition: opacity 0.2s;
}

.auth-logo:hover {
  opacity: 0.85;
}

.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.auth-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.auth-subtitle {
  font-size: 14px;
  color: var(--color-text-tertiary);
  margin-bottom: 24px;
  line-height: 1.5;
}

.code-input-wrapper {
  display: flex;
  gap: 8px;
  width: 100%;
}

.code-input-wrapper .el-input {
  flex: 1;
  min-width: 0;
}

.send-code-btn {
  min-width: 110px;
  white-space: nowrap;
}

.submit-btn {
  width: 100%;
  height: 44px;
  border-radius: var(--radius-sm);
  font-size: 15px;
  font-weight: 500;
}

.auth-footer {
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.auth-footer a {
  color: var(--color-primary);
  font-weight: 500;
}

.auth-footer a:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .auth-card {
    margin: 16px;
    padding: 32px 24px;
  }

  .code-input-wrapper {
    align-items: stretch;
  }

  .send-code-btn {
    min-width: 100px;
    padding: 0 12px;
  }
}
</style>
