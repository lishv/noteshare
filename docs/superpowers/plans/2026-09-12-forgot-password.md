# 邮箱验证码重置密码 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为未登录用户增加登录页“忘记密码”入口，通过绑定邮箱验证码安全设置新密码，并使旧会话失效。

**Architecture:** 复用现有 `VerificationCodeService` 的 Redis 验证码、发送间隔、每日限额和 `MailService`，在 `AuthController` 增加公开的 `reset-password` 接口。后端按邮箱查找用户并用现有 BCrypt 编码器更新密码；前端新增公开的忘记密码页面和路由，不改动已登录用户的 `/users/password` 流程。

**Tech Stack:** Spring Boot 3.2 / Java 17 / Jakarta Validation / MyBatis-Plus / Redis / BCrypt / Vue 3 `<script setup lang="ts">` / Vue Router / Element Plus / Axios

---

## 文件职责与变更范围

- Create: `project/note-backend/src/main/java/com/note/dto/ResetPasswordDTO.java` — 重置密码请求校验模型。
- Modify: `project/note-backend/src/main/java/com/note/controller/user/AuthController.java` — 转发验证码发送和公开重置密码请求。
- Modify: `project/note-backend/src/main/java/com/note/service/AuthService.java` — 限制验证码场景、隐藏未注册邮箱、执行密码重置和旧会话失效。
- Modify: `project/note-backend/src/main/java/com/note/dto/SendCodeDTO.java` — 补充新的验证码场景说明。
- Create: `project/note-backend/src/test/java/com/note/service/AuthServiceTest.java` — 覆盖重置密码关键服务行为；当前测试目录不存在时一并创建。
- Modify: `project/note-frontend/user/src/types/index.ts` — 增加重置密码请求类型。
- Modify: `project/note-frontend/user/src/api/user.ts` — 增加 `resetPassword` API 封装。
- Modify: `project/note-frontend/user/src/router/index.ts` — 增加公开 `/forgot-password` 路由。
- Modify: `project/note-frontend/user/src/views/Login.vue` — 增加“忘记密码？”入口。
- Create: `project/note-frontend/user/src/views/ForgotPassword.vue` — 邮箱、验证码和新密码表单及倒计时交互。

不修改数据库、`User` 实体、`UserMapper`、`VerificationCodeService`、`MailService` 或已登录改密接口。

---

### Task 1: 增加后端重置请求模型

**Files:**
- Create: `project/note-backend/src/main/java/com/note/dto/ResetPasswordDTO.java`
- Modify: `project/note-backend/src/main/java/com/note/dto/SendCodeDTO.java:16-18`

- [ ] **Step 1: 创建带服务端校验的 DTO**

创建文件，使用与 `RegisterDTO`、`ChangePasswordDTO` 相同的 Lombok 和 Jakarta Validation 风格：

```java
package com.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "邮箱验证码重置密码请求参数")
public class ResetPasswordDTO {
    @Schema(description = "注册邮箱", example = "test@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String email;

    @Schema(description = "邮箱验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
    private String verificationCode;

    @Schema(description = "新密码", example = "newpass123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度为6-100个字符")
    private String newPassword;
}
```

- [ ] **Step 2: 更新验证码场景文档**

将 `SendCodeDTO` 的 `scene` schema 描述改为：

```java
@Schema(description = "场景：register-注册，changePassword-已登录修改密码，resetPassword-忘记密码", example = "register", requiredMode = Schema.RequiredMode.REQUIRED)
```

不在 DTO 上使用开放式正则；场景白名单在服务层统一校验，避免绕过发送限额。

---

### Task 2: 实现后端公开发送与重置流程

**Files:**
- Modify: `project/note-backend/src/main/java/com/note/controller/user/AuthController.java:3-37`
- Modify: `project/note-backend/src/main/java/com/note/service/AuthService.java:21-123`

- [ ] **Step 1: 在 `AuthService` 增加验证码场景常量和统一发送方法**

在现有 `LOGIN_FAIL_KEY_PREFIX` 附近增加：

```java
private static final String RESET_PASSWORD_SCENE = "resetPassword";
private static final Set<String> ALLOWED_CODE_SCENES =
        Set.of("register", "changePassword", RESET_PASSWORD_SCENE);
```

同时增加 `java.util.Set` import。

在 `register` 方法之前增加：

```java
public void sendVerificationCode(SendCodeDTO dto) {
    String email = dto.getEmail().trim();
    String scene = dto.getScene().trim();
    if (!ALLOWED_CODE_SCENES.contains(scene)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码场景无效");
    }

    // 忘记密码场景不向未注册邮箱发信，但返回相同成功结果，避免账户枚举。
    if (RESET_PASSWORD_SCENE.equals(scene) && userMapper.selectByEmail(email) == null) {
        return;
    }

    verificationCodeService.sendCode(email, scene);
}
```

`SendCodeDTO` 已由 Controller 的 `@Valid` 校验邮箱和场景非空，因此这里仅做首尾空白规范化和业务白名单校验。

- [ ] **Step 2: 在 `AuthService` 增加密码重置方法**

在 `login` 方法之后增加：

```java
@Transactional
public void resetPassword(ResetPasswordDTO dto) {
    String email = dto.getEmail().trim();
    String verificationCode = dto.getVerificationCode().trim();

    // 先验证并消费验证码；不存在邮箱不会通过发送阶段取得 resetPassword 验证码。
    verificationCodeService.verifyCode(email, RESET_PASSWORD_SCENE, verificationCode);

    User user = userMapper.selectByEmail(email);
    if (user == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
    if (user.getStatus() != null && user.getStatus() == 2) {
        user.setStatus(1);
    }
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.updateById(user);

    invalidateUserAccess(user);
}

private void invalidateUserAccess(User user) {
    try {
        redisTemplate.opsForValue().set(
                "user:token:invalid_before:" + user.getId(),
                System.currentTimeMillis(),
                7,
                TimeUnit.DAYS
        );
        Set<String> keys = redisTemplate.keys(LOGIN_FAIL_KEY_PREFIX + user.getUsername() + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    } catch (Exception ignored) {
        // Redis 不可用时不阻断已完成的密码更新，与已登录改密保持一致。
    }
}
```

保留状态 `0`（管理员禁用）不解禁；状态 `2`（登录锁定）按现有 `UserService.changePassword` 规则恢复为 `1`。密码始终使用当前 `BCryptPasswordEncoder` 编码。

- [ ] **Step 3: 让 Controller 使用服务层发送逻辑并暴露重置接口**

将 `AuthController` 的 import 增加：

```java
import com.note.dto.ResetPasswordDTO;
```

删除 `VerificationCodeService` 字段、import 和构造器参数；Controller 只保留 `AuthService` 与 `JwtUtil`：

```java
public AuthController(AuthService authService, JwtUtil jwtUtil) {
    this.authService = authService;
    this.jwtUtil = jwtUtil;
}
```

将现有发送验证码方法改为：

```java
@PostMapping("/send-code")
@Operation(summary = "发送验证码", description = "向邮箱发送注册、修改密码或重置密码验证码")
public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
    authService.sendVerificationCode(dto);
    return Result.success("验证码发送成功");
}
```

在 `register` 方法之前新增：

```java
@PostMapping("/reset-password")
@Operation(summary = "重置密码", description = "使用邮箱验证码重置用户密码")
public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
    authService.resetPassword(dto);
    return Result.success("密码重置成功");
}
```

由于 `AuthInterceptor` 已将 `/user/auth/**` 列入公开白名单，不新增拦截器配置。

- [ ] **Step 4: 运行后端编译验证**

Run from `project/note-backend`:

```text
mvn -DskipTests compile
```

Expected: `BUILD SUCCESS`，且无 `ResetPasswordDTO`、Controller 映射或 import 编译错误。

---

### Task 3: 为认证服务增加窄范围单元测试

**Files:**
- Create: `project/note-backend/src/test/java/com/note/service/AuthServiceTest.java`

- [ ] **Step 1: 创建 Mockito 测试类**

创建测试目录和测试类，使用项目已有 `spring-boot-starter-test`：

```java
package com.note.service;

import com.note.dto.ResetPasswordDTO;
import com.note.dto.SendCodeDTO;
import com.note.entity.User;
import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import com.note.mapper.UserMapper;
import com.note.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthService authService;

    @Test
    void resetPasswordUpdatesEncodedPasswordAndInvalidatesAccess() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("$2a$10$old");
        user.setStatus(2);

        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setEmail(" alice@example.com ");
        dto.setVerificationCode(" 123456 ");
        dto.setNewPassword("newpass123");

        when(userMapper.selectByEmail("alice@example.com")).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.keys("login:fail:alice:*")).thenReturn(Set.of("login:fail:alice:127.0.0.1"));

        authService.resetPassword(dto);

        verify(verificationCodeService).verifyCode("alice@example.com", "resetPassword", "123456");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User saved = captor.getValue();
        assertEquals(1, saved.getStatus());
        assertEquals("alice@example.com", saved.getEmail());
        org.junit.jupiter.api.Assertions.assertTrue(
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                        .matches("newpass123", saved.getPassword()));
        verify(valueOperations).set(eq("user:token:invalid_before:7"), any(), eq(7L), eq(TimeUnit.DAYS));
        verify(redisTemplate).delete(any(Set.class));
    }

    @Test
    void resetPasswordFailsWhenVerificationCodeIsInvalid() {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setEmail("alice@example.com");
        dto.setVerificationCode("123456");
        dto.setNewPassword("newpass123");
        BusinessException failure = new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        org.mockito.Mockito.doThrow(failure).when(verificationCodeService)
                .verifyCode("alice@example.com", "resetPassword", "123456");

        BusinessException thrown = assertThrows(BusinessException.class, () -> authService.resetPassword(dto));

        assertEquals(ErrorCode.VERIFICATION_CODE_ERROR.getCode(), thrown.getCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void resetCodeIsNotSentForUnknownEmail() {
        SendCodeDTO dto = new SendCodeDTO();
        dto.setEmail("unknown@example.com");
        dto.setScene("resetPassword");
        when(userMapper.selectByEmail("unknown@example.com")).thenReturn(null);

        authService.sendVerificationCode(dto);

        verify(verificationCodeService, never()).sendCode(any(), any());
    }
}
```

- [ ] **Step 2: 运行后端测试**

Run from `project/note-backend`:

```text
mvn -Dtest=AuthServiceTest test
```

Expected: 3 tests pass. If the local Maven/Spring setup has no test source tree, this command should still compile the newly created test and report `BUILD SUCCESS`.

- [ ] **Step 3: 保持测试与实现同步**

如果实现选择了不同的私有方法名，测试只验证公开的 `sendVerificationCode`、`resetPassword` 和外部可观察结果，不依赖私有实现细节；不得放宽到跳过验证码或明文密码断言。

---

### Task 4: 增加用户端 API、类型和公开路由

**Files:**
- Modify: `project/note-frontend/user/src/types/index.ts:75-87`
- Modify: `project/note-frontend/user/src/api/user.ts:1-57`
- Modify: `project/note-frontend/user/src/router/index.ts:12-20`

- [ ] **Step 1: 增加请求类型**

在 `ChangePasswordRequest` 后增加：

```ts
// 忘记密码请求
export interface ResetPasswordRequest {
  email: string
  verificationCode: string
  newPassword: string
}
```

- [ ] **Step 2: 增加 API 类型导入和封装**

在 `user.ts` 的类型导入中增加 `ResetPasswordRequest`：

```ts
  ResetPasswordRequest,
```

在 `sendVerificationCode` 后增加：

```ts
// 使用邮箱验证码重置密码
export function resetPassword(data: ResetPasswordRequest): Promise<ApiResponse> {
  return request.post('/auth/reset-password', data)
}
```

发送验证码继续复用：

```ts
sendVerificationCode({ email, scene: 'resetPassword' })
```

- [ ] **Step 3: 注册公开路由**

在 `/register` 路由后增加：

```ts
{
  path: '/forgot-password',
  name: 'ForgotPassword',
  component: () => import('@/views/ForgotPassword.vue'),
},
```

不要添加 `requiresAuth`，否则未登录用户无法找回密码。

---

### Task 5: 完成登录入口和忘记密码页面

**Files:**
- Modify: `project/note-frontend/user/src/views/Login.vue:30-49,191-197`
- Create: `project/note-frontend/user/src/views/ForgotPassword.vue`

- [ ] **Step 1: 在登录页增加入口**

在登录页密码 `el-form-item` 后、提交按钮前插入：

```vue
<div class="password-help">
  <router-link to="/forgot-password">忘记密码？</router-link>
</div>
```

在 Login 样式中增加：

```css
.password-help {
  margin: -4px 0 16px;
  text-align: right;
  font-size: 13px;
}

.password-help a {
  color: var(--color-primary);
}

.password-help a:hover {
  text-decoration: underline;
}
```

- [ ] **Step 2: 创建忘记密码页面模板**

创建 `ForgotPassword.vue`，沿用 Login/Register 的 `.auth-page`、`.auth-card`、logo SVG 和 Element Plus 表单风格。模板主体应为：

```vue
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
          <el-input v-model="form.email" placeholder="请输入注册邮箱" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="verificationCode">
          <div class="code-input-wrapper">
            <el-input v-model="form.verificationCode" placeholder="6位邮箱验证码" :prefix-icon="Key" maxlength="6" />
            <el-button
              type="primary"
              class="send-code-btn"
              :disabled="codeCountdown > 0 || !form.email"
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
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="resetLoading" @click="handleReset">
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
```

- [ ] **Step 3: 实现页面表单、倒计时和提交逻辑**

在同一文件的 `<script setup lang="ts">` 中使用：

```ts
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
```

- [ ] **Step 4: 添加与现有认证页一致的响应式样式**

复制 Login 页的背景、卡片、logo、标题、按钮和页脚视觉变量；额外加入：

```css
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

.auth-footer {
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.auth-footer a {
  color: var(--color-primary);
  font-weight: 500;
}

@media (max-width: 768px) {
  .auth-card {
    margin: 16px;
    padding: 32px 24px;
  }
}
```

- [ ] **Step 5: 运行用户端类型检查和构建**

Run from `project/note-frontend/user`:

```text
npm run type-check
npm run build
```

Expected: 两条命令均成功；仅允许出现项目已有的 Rollup chunk 大小或 `#__PURE__` 警告，不得出现 `ForgotPassword`、路由、API 类型或 Element Plus 模板类型错误。

---

### Task 6: 端到端行为验证

**Files:** 无新增文件；验证 Task 2 和 Task 5 的接口与页面。

- [ ] **Step 1: 启动后端和用户端**

按项目现有启动方式分别启动 `note-backend` 与 `note-frontend/user`，不要使用会在正常启动后主动杀进程的短 timeout 作为应用运行结论。

- [ ] **Step 2: 验证公开路由与页面入口**

确认：

- 未登录访问 `/forgot-password` 不会被路由守卫重定向；
- `/login` 能看到“忘记密码？”并可进入页面；
- 页面刷新后仍能正常加载；
- 手机宽度下验证码输入框和按钮不溢出。

- [ ] **Step 3: 验证验证码发送行为**

使用已注册邮箱请求：

```http
POST /api/user/auth/send-code
Content-Type: application/json

{"email":"registered@example.com","scene":"resetPassword"}
```

确认收到邮件；60 秒内重复请求返回统一“发送过于频繁”错误。使用未注册但格式合法的邮箱，确认接口仍返回通用成功响应且不会发邮件。使用任意伪造场景，确认返回“验证码场景无效”。

- [ ] **Step 4: 验证密码重置和会话失效**

输入真实验证码提交：

```http
POST /api/user/auth/reset-password
Content-Type: application/json

{"email":"registered@example.com","verificationCode":"123456","newPassword":"newpass123"}
```

确认密码重置成功、旧密码无法登录、新密码可以登录；重置前签发的 JWT 访问受保护接口时被拦截并提示密码已修改。再次使用同一验证码确认不能重复使用；错误、过期验证码均返回统一验证码错误或过期错误。

- [ ] **Step 5: 验证账户状态和回归功能**

确认 status 为 `2` 的登录锁定用户重置成功后可按现有规则恢复登录；status 为 `0` 的禁用用户即使重置密码也仍无法登录。最后确认注册验证码、登录和已登录设置页中的改密流程仍可用。

不执行 Git 检查、提交或推送；这些操作按用户要求留给后续明确指令。
