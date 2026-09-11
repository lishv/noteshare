<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo" @click="router.push('/')" title="返回首页">
        <div class="logo-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect x="2" y="2" width="24" height="24" rx="6" fill="var(--color-primary)"/>
            <path d="M9 10h10M9 14h7M9 18h4" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <span class="logo-text">笔记共享平台</span>
      </div>
      <h2 class="auth-title">创建账号</h2>
      <p class="auth-subtitle">注册后即可分享和浏览同学的笔记</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="UserIcon"
          />
        </el-form-item>
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱"
            :prefix-icon="Message"
          />
        </el-form-item>
        <el-form-item prop="verificationCode">
          <div class="code-input-wrapper">
            <el-input
              v-model="form.verificationCode"
              placeholder="邮箱验证码"
              :prefix-icon="Key"
            />
            <el-button
              type="primary"
              :disabled="codeCountdown > 0 || !form.email"
              :loading="codeLoading"
              class="send-code-btn"
              @click="handleSendCode"
            >
              {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="手机号（选填）"
            :prefix-icon="Phone"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码（至少6位）"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-divider">
        <span>或</span>
      </div>
      <div class="auth-footer">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User as UserIcon, Lock, Message, Phone, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { register, sendVerificationCode } from '@/api/user'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const codeLoading = ref(false)
const codeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  username: '',
  email: '',
  verificationCode: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为2-50个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

function startCountdown() {
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function handleSendCode() {
  if (!form.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  codeLoading.value = true
  try {
    await sendVerificationCode({ email: form.email, scene: 'register' })
    ElMessage.success('验证码已发送')
    startCountdown()
  } catch {
    // 错误已由拦截器处理
  } finally {
    codeLoading.value = false
  }
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      email: form.email,
      phone: form.phone || undefined,
      verificationCode: form.verificationCode,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--color-bg-page);
  position: relative;
  overflow: hidden;
  padding: 40px 0;
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

.submit-btn {
  width: 100%;
  height: 44px;
  border-radius: var(--radius-sm);
  font-size: 15px;
  font-weight: 500;
}

.code-input-wrapper {
  display: flex;
  gap: 8px;
  width: 100%;
}

.code-input-wrapper .el-input {
  flex: 1;
}

.send-code-btn {
  white-space: nowrap;
  min-width: 110px;
}

.auth-divider {
  display: flex;
  align-items: center;
  margin: 20px 0;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.auth-divider::before,
.auth-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border);
}

.auth-divider span {
  padding: 0 12px;
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
}
</style>
