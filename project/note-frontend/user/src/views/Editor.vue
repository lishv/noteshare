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

    <!-- Markdown 主体编辑器 -->
    <main class="editor-main" v-loading="loading">
      <MdEditor
        v-model="content"
        :toolbars="toolbars"
        :preview="true"
        placeholder="开始记录你的知识与思考... 支持 Markdown 语法、代码高亮、表格以及拖拽/粘贴图片上传。"
        class="md-editor-container"
        @onUploadImg="handleUploadImg"
      />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, House, Lock, Unlock } from '@element-plus/icons-vue'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { createNote, updateNote, getNoteById, uploadNoteImage } from '@/api/note'
import { containsSensitiveWords } from '@/utils/helpers'

const route = useRoute()
const router = useRouter()

const noteId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : null
})
const isEdit = computed(() => !!noteId.value)

const title = ref('')
const content = ref('')
const isPublic = ref<number>(1)
const loading = ref(false)
const submitting = ref(false)

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

// 加载现有笔记详情（编辑模式）
async function loadNoteData() {
  if (!noteId.value) return
  loading.value = true
  try {
    const res = await getNoteById(noteId.value)
    const note = res.data
    title.value = note.title || ''
    content.value = note.content || ''
    isPublic.value = note.isPublic !== undefined ? note.isPublic : 1
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

// 返回
async function handleBack() {
  if (content.value || title.value) {
    try {
      await ElMessageBox.confirm('当前编辑的内容尚未发布，确定离开吗？', '提示', {
        type: 'warning',
        confirmButtonText: '确定离开',
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
  if (content.value || title.value) {
    try {
      await ElMessageBox.confirm('当前编辑的内容尚未发布，确定返回主页吗？', '提示', {
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

// 发布 / 更新
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
      })
      ElMessage.success('笔记更新成功')
      router.push(`/note/${noteId.value}`)
    } else {
      const res = await createNote({
        title: title.value.trim(),
        noteType: 2,
        content: content.value,
        isPublic: isPublic.value,
      })
      ElMessage.success('笔记发布成功')
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

onMounted(() => {
  if (isEdit.value) {
    loadNoteData()
  }
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
  gap: 16px;
}

.visibility-option {
  display: flex;
  align-items: center;
  gap: 6px;
}

.word-count {
  font-size: 13px;
  color: #909399;
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
