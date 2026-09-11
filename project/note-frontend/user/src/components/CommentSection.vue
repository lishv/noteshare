<template>
  <div class="comment-section">
    <!-- 头部标题栏 -->
    <div class="comment-header">
      <h2 class="comment-title">
        全文评论
        <span class="comment-count" v-if="comments.length > 0">({{ comments.length }})</span>
      </h2>
    </div>

    <!-- 发表主评论区域 -->
    <div class="main-input-card">
      <template v-if="userStore.token">
        <div class="input-header">
          <el-avatar
            :size="38"
            :src="userStore.userInfo?.avatar"
            :style="{ backgroundColor: getAvatarColor(userStore.userInfo?.username || 'U') }"
            class="user-avatar"
          >
            {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
          </el-avatar>
          <div class="input-right">
            <el-input
              v-model="mainContent"
              type="textarea"
              :rows="3"
              placeholder="写下你的想法，参与讨论与交流..."
              maxlength="1000"
              show-word-limit
              class="comment-textarea"
            />
            <div class="input-footer">
              <span class="tip-text">支持友好交流，请勿发布违规敏感内容</span>
              <el-button
                type="primary"
                :loading="submitting"
                @click="handleSubmitMainComment"
              >
                发表评论
              </el-button>
            </div>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="login-prompt">
          <span>登录后参与评论与互动</span>
          <el-button type="primary" size="small" @click="goToLogin">立即登录</el-button>
        </div>
      </template>
    </div>

    <el-divider class="section-divider" />

    <!-- 评论列表区域 -->
    <div class="comment-list" v-loading="loading">
      <template v-if="comments.length > 0">
        <div
          v-for="item in comments"
          :key="item.id"
          class="comment-item"
        >
          <!-- 头像 -->
          <el-avatar
            :size="40"
            :src="item.avatar"
            :style="{ backgroundColor: getAvatarColor(item.username) }"
            class="item-avatar"
          >
            {{ item.username?.charAt(0)?.toUpperCase() || 'U' }}
          </el-avatar>

          <!-- 评论右侧主体 -->
          <div class="item-content">
            <!-- 用户名与时间行 -->
            <div class="item-meta">
              <span class="item-username">{{ item.username }}</span>
              <span class="item-time">{{ formatCommentDate(item.createdAt) }}</span>
            </div>

            <!-- 评论正文 (支持 @被回复者 高亮) -->
            <div class="item-text">
              <span
                v-if="item.replyToUsername"
                class="reply-mention"
              >
                @{{ item.replyToUsername }}
              </span>
              <span class="text-body">{{ item.content }}</span>
            </div>

            <!-- 底部操作栏 -->
            <div class="item-actions">
              <el-button
                link
                type="primary"
                size="small"
                :icon="ChatDotRound"
                @click="handleToggleReply(item)"
              >
                回复
              </el-button>
              <el-button
                v-if="canDelete(item)"
                link
                type="danger"
                size="small"
                :icon="Delete"
                @click="handleDeleteComment(item.id)"
              >
                删除
              </el-button>
            </div>

            <!-- 展开的二级回复输入框 -->
            <div v-if="replyingCommentId === item.id" class="inline-reply-box">
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="2"
                :placeholder="`回复 @${item.username}...`"
                maxlength="1000"
                show-word-limit
                class="reply-textarea"
                ref="replyInputRef"
              />
              <div class="inline-reply-footer">
                <el-button size="small" @click="replyingCommentId = null">取消</el-button>
                <el-button
                  type="primary"
                  size="small"
                  :loading="replySubmitting"
                  @click="handleSubmitReply(item)"
                >
                  发送回复
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 空评论状态 -->
      <div v-else-if="!loading" class="empty-comments">
        <el-empty description="暂无评论，快来抢沙发吧~" :image-size="100" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Delete } from '@element-plus/icons-vue'
import { getNoteComments, createComment, deleteNoteComment } from '@/api/note'
import type { CommentItem } from '@/types'
import { useUserStore } from '@/store/user'
import { containsSensitiveWords } from '@/utils/helpers'

const props = defineProps<{
  noteId: number
  noteAuthorId?: number
}>()

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const comments = ref<CommentItem[]>([])
const loading = ref(false)
const mainContent = ref('')
const submitting = ref(false)

const replyingCommentId = ref<number | null>(null)
const replyContent = ref('')
const replySubmitting = ref(false)
const replyInputRef = ref()

// 头像颜色池
const AVATAR_COLORS = [
  '#f57c00', // 橙色
  '#00b894', // 青绿
  '#0984e3', // 亮蓝
  '#6c5ce7', // 蓝紫
  '#e17055', // 珊瑚橙
  '#00cec9', // 湖水绿
  '#d63031', // 玫瑰红
  '#fdcb6e', // 暖黄
]

function getAvatarColor(name: string): string {
  if (!name) return '#409eff'
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  const index = Math.abs(hash) % AVATAR_COLORS.length
  return AVATAR_COLORS[index]
}

// 格式化评论时间为类似：2025年2月20日 14:30
function formatCommentDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const year = d.getFullYear()
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  return `${year}年${month}月${day}日 ${hour}:${minute}`
}

// 检查删除权限 (评论本人 或 笔记作者)
function canDelete(item: CommentItem): boolean {
  if (!userStore.userInfo) return false
  const currentUid = userStore.userInfo.id
  return item.userId === currentUid || (props.noteAuthorId != null && props.noteAuthorId === currentUid)
}

// 获取评论列表
async function fetchComments() {
  loading.value = true
  try {
    const res = await getNoteComments(props.noteId)
    comments.value = res.data || []
  } catch {
    comments.value = []
  } finally {
    loading.value = false
  }
}

// 切换回复输入框
function handleToggleReply(item: CommentItem) {
  if (!userStore.token) {
    goToLogin()
    return
  }
  if (replyingCommentId.value === item.id) {
    replyingCommentId.value = null
    replyContent.value = ''
  } else {
    replyingCommentId.value = item.id
    replyContent.value = ''
    nextTick(() => {
      replyInputRef.value?.focus()
    })
  }
}

// 提交主评论
async function handleSubmitMainComment() {
  const content = mainContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }
  if (containsSensitiveWords(content)) {
    ElMessage.error('内容包含敏感违规词汇，请修改后重试')
    return
  }

  submitting.value = true
  try {
    const res = await createComment(props.noteId, { content })
    ElMessage.success('评论发表成功')
    mainContent.value = ''
    if (res.data) {
      comments.value.push(res.data)
    } else {
      await fetchComments()
    }
  } catch {
    // 错误在 request 统一捕获处理
  } finally {
    submitting.value = false
  }
}

// 提交回复
async function handleSubmitReply(item: CommentItem) {
  const content = replyContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入回复内容')
    return
  }
  if (containsSensitiveWords(content)) {
    ElMessage.error('内容包含敏感违规词汇，请修改后重试')
    return
  }

  replySubmitting.value = true
  try {
    const res = await createComment(props.noteId, {
      content,
      parentId: item.parentId || item.id,
      replyToUserId: item.userId,
      replyToCommentId: item.id,
    })
    ElMessage.success('回复发表成功')
    replyContent.value = ''
    replyingCommentId.value = null
    if (res.data) {
      comments.value.push(res.data)
    } else {
      await fetchComments()
    }
  } catch {
    // error handled
  } finally {
    replySubmitting.value = false
  }
}

// 删除评论
async function handleDeleteComment(commentId: number) {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '删除提示', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    })
    await deleteNoteComment(props.noteId, commentId)
    ElMessage.success('评论已删除')
    comments.value = comments.value.filter(c => c.id !== commentId)
  } catch {
    // cancel
  }
}

function goToLogin() {
  router.push({ name: 'Login', query: { redirect: route.fullPath } })
}

onMounted(() => {
  if (props.noteId) {
    fetchComments()
  }
})
</script>

<style scoped>
.comment-section {
  margin-top: 48px;
  padding-top: 36px;
  border-top: 1px solid #ebeef5;
}

.comment-header {
  margin-bottom: 24px;
}

.comment-title {
  font-size: 22px;
  font-weight: 700;
  color: #1f2329;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-count {
  font-size: 16px;
  font-weight: 500;
  color: #8f959e;
}

/* 主评论输入区域 */
.main-input-card {
  margin-bottom: 24px;
}

.input-header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.user-avatar {
  flex-shrink: 0;
  color: #ffffff;
  font-weight: 600;
}

.input-right {
  flex: 1;
}

.comment-textarea :deep(.el-textarea__inner) {
  border-radius: 6px;
  padding: 12px;
  font-size: 14px;
  background-color: #f8f9fa;
  border-color: #e4e7ed;
  transition: all 0.2s ease;
}

.comment-textarea :deep(.el-textarea__inner:focus) {
  background-color: #ffffff;
  border-color: #409eff;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.tip-text {
  font-size: 12px;
  color: #909399;
}

.login-prompt {
  background: #f4f6f8;
  border-radius: 6px;
  padding: 20px;
  text-align: center;
  color: #606266;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.section-divider {
  margin: 28px 0;
  border-color: #ebeef5;
}

/* 评论列表 */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.comment-item {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding-bottom: 20px;
  border-bottom: 1px solid #f2f3f5;
}

.comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.item-avatar {
  flex-shrink: 0;
  color: #ffffff;
  font-weight: 600;
}

.item-content {
  flex: 1;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.item-username {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}

.item-time {
  font-size: 13px;
  color: #8f959e;
}

.item-text {
  font-size: 15px;
  line-height: 1.6;
  color: #262626;
  word-break: break-word;
  margin-bottom: 8px;
}

.reply-mention {
  color: #409eff;
  font-weight: 500;
  margin-right: 6px;
  cursor: pointer;
}

.reply-mention:hover {
  text-decoration: underline;
}

.text-body {
  color: #333333;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.item-actions :deep(.el-button) {
  font-size: 13px;
  padding: 0;
  height: auto;
}

/* 二级回复输入框 */
.inline-reply-box {
  margin-top: 12px;
  background: #f7f8fa;
  border-radius: 6px;
  padding: 12px 16px;
  border: 1px solid #ebeef5;
}

.reply-textarea :deep(.el-textarea__inner) {
  background-color: #ffffff;
}

.inline-reply-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

.empty-comments {
  padding: 20px 0;
}
</style>
