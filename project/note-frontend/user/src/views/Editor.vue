<template>
  <div class="editor-page">
    <!-- 顶部导航与发布栏 -->
    <header class="editor-header">
      <div class="header-left">
        <el-button @click="handleBack" :icon="ArrowLeft" circle title="返回" />
        <el-button @click="handleGoHome" :icon="House" circle title="返回主页" />
        <el-input
          v-model="title"
          placeholder="请输入笔记标题 (最多100字)..."
          class="title-input"
          maxlength="100"
        />
      </div>

      <div class="header-right">
        <!-- 草稿状态提示 -->
        <div class="draft-status" :title="draftStatusTooltip">
          <el-icon v-if="isDraftSaving" class="is-loading draft-icon"><Loading /></el-icon>
          <el-icon v-else-if="lastSavedTime" class="draft-icon success"><Check /></el-icon>
          <span class="draft-text">{{ draftStatusText }}</span>
        </div>

        <!-- 手动暂存草稿按钮 -->
        <el-button
          size="small"
          :icon="DocumentChecked"
          :loading="isDraftSaving"
          @click="handleManualSaveDraft"
          plain
          class="save-draft-btn"
        >
          暂存草稿
        </el-button>

        <!-- 公开/私密选择 -->
        <el-select
          v-model="isPublic"
          class="visibility-select"
          size="default"
          style="width: 110px;"
        >
          <el-option :value="1" label="公开">
            <div class="visibility-option">
              <el-icon color="#67c23a"><Unlock /></el-icon>
              <span>公开</span>
            </div>
          </el-option>
          <el-option :value="0" label="私密">
            <div class="visibility-option">
              <el-icon color="#e6a23c"><Lock /></el-icon>
              <span>私密</span>
            </div>
          </el-option>
        </el-select>

        <span class="word-count" v-if="content">
          {{ content.length }} 字
        </span>

        <el-button
          type="primary"
          :loading="submitting"
          :icon="Check"
          @click="handlePublish"
        >
          {{ isEdit ? '更新笔记' : '发布笔记' }}
        </el-button>
      </div>
    </header>

    <!-- 笔记标签设置栏 -->
    <div class="editor-tags-bar">
      <span class="tags-bar-label">标签：</span>
      <el-select
        v-model="tags"
        multiple
        filterable
        allow-create
        default-first-option
        placeholder="输入标签并按回车添加（如：Vue3、Java、算法、读书）"
        class="tags-selector"
        size="small"
      />
    </div>

    <!-- Markdown 主体编辑器 -->
    <main class="editor-main" v-loading="loading">
      <MdEditor
        v-model="content"
        :toolbars="toolbars"
        :preview="true"
        placeholder="开始记录你的知识与思考... 支持 Markdown 语法、公式、表格以及拖拽/粘贴图片上传。系统会自动为您双重暂存草稿。"
        class="md-editor-container"
        @onUploadImg="handleUploadImg"
      />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, House, Lock, Unlock, Loading, DocumentChecked } from '@element-plus/icons-vue'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import {
  createNote,
  updateNote,
  getNoteById,
  uploadNoteImage,
  saveNoteDraft,
  getNoteDraft,
  deleteNoteDraft,
} from '@/api/note'
import { containsSensitiveWords } from '@/utils/helpers'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const noteId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : null
})
const isEdit = computed(() => !!noteId.value)

const title = ref('')
const content = ref('')
const isPublic = ref<number>(1)
const tags = ref<string[]>([])
const loading = ref(false)
const submitting = ref(false)

// 初始已载入内容（用于对比是否有未保存变更）
const originalTitle = ref('')
const originalContent = ref('')
const originalIsPublic = ref<number>(1)

// 草稿相关状态
const isDraftSaving = ref(false)
const lastSavedTime = ref('')
const isRestoring = ref(false)
const isPublishedSuccess = ref(false)

// 本地草稿 Storage Key
const localDraftKey = computed(() => {
  const uid = userStore.userInfo?.id || 'guest'
  return isEdit.value ? `note_draft_${uid}_${noteId.value}` : `note_draft_${uid}_new`
})

// 草稿状态展示文字与提示
const draftStatusText = computed(() => {
  if (isDraftSaving.value) return '草稿保存中...'
  if (lastSavedTime.value) return `已暂存 ${lastSavedTime.value}`
  return '未产生草稿'
})

const draftStatusTooltip = computed(() => {
  return '系统已开启前端 LocalStorage + 云端 Redis 双重实时防丢保护'
})

// 是否有未保存的修改
const hasUnsavedChanges = computed(() => {
  if (isPublishedSuccess.value) return false
  return title.value !== originalTitle.value || content.value !== originalContent.value || isPublic.value !== originalIsPublic.value
})

const toolbars: ToolbarNames[] = [
  'bold',
  'underline',
  'italic',
  'strikeThrough',
  '-',
  'title',
  'sub',
  'sup',
  'quote',
  'unorderedList',
  'orderedList',
  'task',
  '-',
  'codeRow',
  'code',
  'link',
  'image',
  'table',
  'mermaid',
  'katex',
  '-',
  'revoke',
  'next',
  '=',
  'pageFullscreen',
  'fullscreen',
  'preview',
  'catalog',
]

// ==================== 1. 本地 LocalStorage 自动防抖暂存 ====================
let localSaveTimer: any = null
function triggerLocalDraftSave() {
  if (isRestoring.value || isPublishedSuccess.value) return
  if (!title.value.trim() && !content.value.trim()) return

  if (localSaveTimer) clearTimeout(localSaveTimer)
  localSaveTimer = setTimeout(() => {
    try {
      const draftData = {
        noteId: noteId.value,
        title: title.value,
        content: content.value,
        isPublic: isPublic.value,
        savedAt: new Date().toISOString(),
      }
      localStorage.setItem(localDraftKey.value, JSON.stringify(draftData))
      const now = new Date()
      lastSavedTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
    } catch (e) {
      console.warn('LocalStorage draft save error:', e)
    }
  }, 600)
}

// ==================== 2. 云端 Redis 自动静默同步 ====================
let remoteSyncTimer: any = null
async function syncDraftToCloud() {
  if (isRestoring.value || isPublishedSuccess.value || !userStore.isLoggedIn) return
  if (!title.value.trim() && !content.value.trim()) return

  isDraftSaving.value = true
  try {
    await saveNoteDraft({
      noteId: noteId.value || undefined,
      title: title.value,
      content: content.value,
      isPublic: isPublic.value,
    })
    const now = new Date()
    lastSavedTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  } catch (e) {
    // 静默失败，不打扰用户输入
    console.warn('Sync draft to cloud warning:', e)
  } finally {
    isDraftSaving.value = false
  }
}

function triggerCloudDraftSync() {
  if (remoteSyncTimer) clearTimeout(remoteSyncTimer)
  remoteSyncTimer = setTimeout(() => {
    syncDraftToCloud()
  }, 3000)
}

// 手动暂存草稿
async function handleManualSaveDraft() {
  if (!title.value.trim() && !content.value.trim()) {
    ElMessage.warning('内容为空，无需暂存草稿')
    return
  }

  // 1. 本地立即存
  const draftData = {
    noteId: noteId.value,
    title: title.value,
    content: content.value,
    isPublic: isPublic.value,
    savedAt: new Date().toISOString(),
  }
  localStorage.setItem(localDraftKey.value, JSON.stringify(draftData))

  // 2. 云端存
  if (userStore.isLoggedIn) {
    isDraftSaving.value = true
    try {
      await saveNoteDraft({
        noteId: noteId.value || undefined,
        title: title.value,
        content: content.value,
        isPublic: isPublic.value,
      })
      ElMessage.success('草稿已成功保存到本地与云端')
    } catch {
      ElMessage.success('草稿已保存到本地')
    } finally {
      isDraftSaving.value = false
    }
  } else {
    ElMessage.success('草稿已保存到本地')
  }

  const now = new Date()
  lastSavedTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
}

// 监听输入变化
watch([title, content, isPublic], () => {
  if (!isRestoring.value && !isPublishedSuccess.value) {
    triggerLocalDraftSave()
    triggerCloudDraftSync()
  }
})

// ==================== 3. 草稿检测与智能恢复 ====================
async function checkAndRestoreDraft() {
  let candidateDraft: { title?: string; content?: string; isPublic?: number; savedAt?: string } | null = null
  let draftSource = '本地'

  // 1. 先查本地 LocalStorage
  const localRaw = localStorage.getItem(localDraftKey.value)
  if (localRaw) {
    try {
      candidateDraft = JSON.parse(localRaw)
      draftSource = '本地'
    } catch {
      localStorage.removeItem(localDraftKey.value)
    }
  }

  // 2. 若用户已登录，向后端请求最新 Redis 草稿比对
  if (userStore.isLoggedIn) {
    try {
      const res = await getNoteDraft(noteId.value || undefined)
      if (res.data) {
        const cloudDraft = res.data
        // 如果云端草稿存在且本地没有，或云端草稿时间更新
        if (!candidateDraft || (cloudDraft.savedAt && (!candidateDraft.savedAt || new Date(cloudDraft.savedAt) > new Date(candidateDraft.savedAt)))) {
          candidateDraft = cloudDraft
          draftSource = '云端'
        }
      }
    } catch (e) {
      console.warn('Check cloud draft failed:', e)
    }
  }

  if (!candidateDraft) return

  const hasContent = (candidateDraft.title && candidateDraft.title.trim()) || (candidateDraft.content && candidateDraft.content.trim())
  if (!hasContent) return

  // 对比草稿是否与当前已载入的内容有实质差异
  const isDifferent = candidateDraft.title !== originalTitle.value || candidateDraft.content !== originalContent.value

  if (isDifferent) {
    const timeFormatted = candidateDraft.savedAt ? new Date(candidateDraft.savedAt).toLocaleString() : '最近'
    try {
      await ElMessageBox.confirm(
        `检测到您在 [${timeFormatted}] 有未保存的${draftSource}草稿，是否恢复？`,
        '发现未保存草稿',
        {
          confirmButtonText: '恢复草稿',
          cancelButtonText: '丢弃草稿',
          type: 'info',
          distinguishCancelAndClose: true,
        }
      )

      // 用户选择恢复
      isRestoring.value = true
      title.value = candidateDraft.title || ''
      content.value = candidateDraft.content || ''
      if (candidateDraft.isPublic !== undefined) {
        isPublic.value = candidateDraft.isPublic
      }
      lastSavedTime.value = candidateDraft.savedAt ? new Date(candidateDraft.savedAt).toLocaleTimeString() : '刚刚'
      ElMessage.success('已成功恢复未保存草稿')
      setTimeout(() => {
        isRestoring.value = false
      }, 500)
    } catch (action) {
      if (action === 'cancel') {
        // 用户明确点击“丢弃草稿”
        clearAllDrafts()
        ElMessage.info('已丢弃草稿')
      }
    }
  }
}

// 清除全部草稿（本地 + 云端）
function clearAllDrafts() {
  try {
    localStorage.removeItem(localDraftKey.value)
  } catch (e) {}

  if (userStore.isLoggedIn) {
    deleteNoteDraft(noteId.value || undefined).catch(() => {})
  }
  lastSavedTime.value = ''
}

// ==================== 4. 加载笔记详情（编辑模式） ====================
async function loadNoteData() {
  if (!noteId.value) return
  loading.value = true
  try {
    const res = await getNoteById(noteId.value)
    const note = res.data
    title.value = note.title || ''
    content.value = note.content || ''
    isPublic.value = note.isPublic !== undefined ? note.isPublic : 1
    if (note.tags && Array.isArray(note.tags)) {
      tags.value = [...note.tags]
    }

    originalTitle.value = note.title || ''
    originalContent.value = note.content || ''
    originalIsPublic.value = isPublic.value
  } catch {
    ElMessage.error('获取笔记信息失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

// 图片上传钩子
async function handleUploadImg(files: File[], callback: (urls: string[]) => void) {
  try {
    const uploadPromises = files.map(file => uploadNoteImage(file))
    const results = await Promise.all(uploadPromises)
    const urls = results.map(res => res.data)
    callback(urls)
    ElMessage.success('图片上传成功')
  } catch {
    ElMessage.error('图片上传失败，请重试')
  }
}

// ==================== 5. 离开与防误关保护 ====================
// 浏览器刷新/关闭标签页拦截
function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (hasUnsavedChanges.value) {
    e.preventDefault()
    e.returnValue = '您有正在编辑的内容尚未发布，确定离开吗？'
    return e.returnValue
  }
}

// 路由离开拦截
onBeforeRouteLeave(async (to, from, next) => {
  if (isPublishedSuccess.value || !hasUnsavedChanges.value) {
    next()
    return
  }

  try {
    await ElMessageBox.confirm('您有正在编辑的笔记内容尚未发布，离开后可从草稿箱恢复。确定离开吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定离开',
      cancelButtonText: '继续编辑',
    })
    next()
  } catch {
    next(false)
  }
})

// 返回
async function handleBack() {
  if (hasUnsavedChanges.value) {
    try {
      await ElMessageBox.confirm('当前编辑的内容尚未发布（系统已为您自动暂存草稿），确定返回吗？', '提示', {
        type: 'warning',
        confirmButtonText: '确定返回',
        cancelButtonText: '继续编辑',
      })
      if (window.history.state && window.history.state.back) {
        router.back()
      } else {
        router.push('/')
      }
    } catch {
      // 取消离开
    }
  } else {
    if (window.history.state && window.history.state.back) {
      router.back()
    } else {
      router.push('/')
    }
  }
}

// 返回主页
async function handleGoHome() {
  if (hasUnsavedChanges.value) {
    try {
      await ElMessageBox.confirm('当前编辑的内容尚未发布（系统已为您自动暂存草稿），确定返回主页吗？', '提示', {
        type: 'warning',
        confirmButtonText: '确定离开',
        cancelButtonText: '继续编辑',
      })
      router.push('/')
    } catch {
      // 取消离开
    }
  } else {
    router.push('/')
  }
}

// ==================== 6. 发布 / 更新笔记 ====================
async function handlePublish() {
  if (!title.value.trim()) {
    ElMessage.warning('请输入笔记标题')
    return
  }
  if (!content.value.trim()) {
    ElMessage.warning('请输入笔记内容')
    return
  }

  // 敏感词检查
  if (containsSensitiveWords(title.value) || containsSensitiveWords(content.value)) {
    ElMessage.error('内容包含敏感词汇，请修改后重试')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value && noteId.value) {
      await updateNote(noteId.value, {
        title: title.value.trim(),
        noteType: 2,
        content: content.value,
        isPublic: isPublic.value,
        tagNames: tags.value,
      })
      isPublishedSuccess.value = true
      clearAllDrafts()
      ElMessage.success('笔记更新成功，草稿缓存已自动清除')
      router.push(`/note/${noteId.value}`)
    } else {
      const res = await createNote({
        title: title.value.trim(),
        noteType: 2,
        content: content.value,
        isPublic: isPublic.value,
        tagNames: tags.value,
      })
      isPublishedSuccess.value = true
      clearAllDrafts()
      ElMessage.success('笔记发布成功，草稿缓存已自动清除')
      if (res.data?.id) {
        router.push(`/note/${res.data.id}`)
      } else {
        router.push('/')
      }
    }
  } catch {
    // handled
  } finally {
    submitting.value = false
  }
}

// 定时云端同步计时器（每30秒静默同步一次）
let periodicSyncTimer: any = null

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)

  if (isEdit.value) {
    await loadNoteData()
  }
  // 检测并尝试恢复草稿
  await checkAndRestoreDraft()

  // 开启每 30 秒定时草稿同步
  periodicSyncTimer = setInterval(() => {
    if (hasUnsavedChanges.value) {
      syncDraftToCloud()
    }
  }, 30000)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  if (localSaveTimer) clearTimeout(localSaveTimer)
  if (remoteSyncTimer) clearTimeout(remoteSyncTimer)
  if (periodicSyncTimer) clearInterval(periodicSyncTimer)
})
</script>

<style scoped>
.editor-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background: #ffffff;
  overflow: hidden;
}

.editor-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #e4e7ed;
  background-color: #ffffff;
  gap: 16px;
  flex-shrink: 0;
}

.editor-tags-bar {
  display: flex;
  align-items: center;
  padding: 6px 24px;
  background: #fafbfc;
  border-bottom: 1px solid #edf0f2;
  gap: 12px;
  flex-shrink: 0;
}

.tags-bar-label {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
}

.tags-selector {
  flex: 1;
  max-width: 680px;
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
  gap: 12px;
}

.title-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  padding: 0 8px;
}

.title-input :deep(.el-input__inner) {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.draft-status {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}

.draft-icon {
  font-size: 14px;
}

.draft-icon.success {
  color: #67c23a;
}

.draft-text {
  font-size: 12px;
  white-space: nowrap;
}

.save-draft-btn {
  font-size: 12px;
  padding: 5px 10px;
}

.visibility-option {
  display: flex;
  align-items: center;
  gap: 6px;
}

.word-count {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
}

.editor-main {
  flex: 1;
  height: calc(100vh - 60px);
  position: relative;
}

.md-editor-container {
  height: 100% !important;
  border: none !important;
}

:deep(.md-editor-toolbar-wrapper) {
  border-bottom: 1px solid #f2f3f5;
}
</style>
