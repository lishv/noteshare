<template>
  <div class="note-detail-page" v-loading="loading">
    <!-- 顶部导航栏 -->
    <header class="detail-header">
      <div class="header-content">
        <el-button @click="handleBack" :icon="ArrowLeft" circle title="返回上一页" />
        <span class="header-title" @click="router.push('/')">笔记共享平台</span>
        <el-button
          class="home-btn"
          type="primary"
          plain
          round
          size="small"
          :icon="House"
          @click="router.push('/')"
        >
          返回主页
        </el-button>
      </div>
      <div class="header-actions" v-if="note">
        <el-button
          :type="isFavorited ? 'warning' : 'default'"
          :icon="isFavorited ? StarFilled : Star"
          @click="handleToggleFavorite"
        >
          {{ isFavorited ? '已收藏' : '收藏' }} ({{ favoriteCount }})
        </el-button>

        <template v-if="isOwner">
          <el-button type="primary" :icon="EditPen" @click="handleEdit">
            编辑
          </el-button>
          <el-button type="danger" :icon="Delete" @click="handleDelete">
            删除
          </el-button>
        </template>
      </div>
    </header>

    <!-- 文章主体区域 -->
    <div class="detail-container" v-if="note">
      <div class="article-wrapper">
        <!-- 标题与元数据 -->
        <div class="article-header">
          <div class="type-badge-row">
            <el-tag type="success" effect="plain" round size="small">原创创作笔记</el-tag>
            <el-tag v-if="note.isPinned === 1" type="warning" size="small">置顶</el-tag>
            <el-tag v-if="note.isPublic === 0" type="danger" effect="dark" size="small" round>
              <el-icon style="margin-right: 3px; vertical-align: -1px;"><Lock /></el-icon>私密 (仅自己可见)
            </el-tag>
          </div>

          <h1 class="article-title">{{ note.title }}</h1>

          <div class="author-meta-bar">
            <div class="author-info" @click="router.push(`/user/${note.userId}`)">
              <el-avatar :size="40" :src="note.avatar || defaultAvatar">
                {{ note.username?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="author-text">
                <span class="author-name">{{ note.username }}</span>
                <span class="publish-time">发布于 {{ formatDate(note.createdAt) }} · 更新于 {{ formatDate(note.updatedAt) }}</span>
              </div>
            </div>

            <div class="article-stats">
              <span class="stat-item">
                <el-icon><View /></el-icon> {{ note.viewCount }} 次阅读
              </span>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- Markdown 正文预览 -->
        <main class="article-content">
          <MdPreview
            editorId="note-preview-content"
            :modelValue="note.content || ''"
          />
        </main>

        <!-- 评论与讨论区 -->
        <CommentSection
          :noteId="note.id"
          :noteAuthorId="note.userId"
        />
      </div>

      <!-- 右侧目录浮窗 (TOC) -->
      <aside class="toc-sidebar">
        <div class="toc-card">
          <div class="toc-title">
            <el-icon><Reading /></el-icon>
            <span>文章目录</span>
          </div>
          <MdCatalog
            editorId="note-preview-content"
            :scrollElement="scrollElement"
            class="catalog-nav"
          />
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Star, StarFilled, EditPen, Delete, View, Reading, House, Lock } from '@element-plus/icons-vue'
import { MdPreview, MdCatalog } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { getNoteById, deleteNote, addFavorite, removeFavorite, checkFavorite } from '@/api/note'
import type { NoteItem } from '@/types'
import { useUserStore } from '@/store/user'
import { formatDate } from '@/utils/helpers'
import CommentSection from '@/components/CommentSection.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const noteId = Number(route.params.id)
const note = ref<NoteItem | null>(null)
const loading = ref(true)
const isFavorited = ref(false)
const favoriteCount = ref(0)
const scrollElement = ref<HTMLElement | string>('html')

const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" fill="%23409eff"/><text x="50" y="55" text-anchor="middle" fill="white" font-size="40">U</text></svg>')

const isOwner = computed(() => {
  return userStore.userInfo && note.value && userStore.userInfo.id === note.value.userId
})

async function fetchNote() {
  loading.value = true
  try {
    const res = await getNoteById(noteId)
    note.value = res.data
    // 如果是外链笔记且直接访问了此页面，友好跳转或展示
    if (note.value.noteType === 1 && note.value.locationOrUrl) {
      if (note.value.locationOrUrl.startsWith('http')) {
        window.location.href = note.value.locationOrUrl
        return
      }
    }
    await checkFavStatus()
  } catch {
    ElMessage.error('获取笔记详情失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

async function checkFavStatus() {
  if (!userStore.token) return
  try {
    const res = await checkFavorite(noteId)
    isFavorited.value = res.data
  } catch {
    // ignore
  }
}

async function handleToggleFavorite() {
  if (!userStore.token) {
    ElMessage.warning('请先登录后再收藏')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }

  try {
    if (isFavorited.value) {
      await removeFavorite(noteId)
      isFavorited.value = false
      favoriteCount.value = Math.max(0, favoriteCount.value - 1)
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(noteId)
      isFavorited.value = true
      favoriteCount.value += 1
      ElMessage.success('收藏成功')
    }
  } catch {
    // handled
  }
}

function handleBack() {
  if (window.history.state && window.history.state.back) {
    router.back()
  } else {
    router.push('/')
  }
}

function handleEdit() {
  router.push(`/note/editor/${noteId}`)
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这篇笔记吗？删除后可在管理端恢复。', '删除提示', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    })
    await deleteNote(noteId)
    ElMessage.success('笔记已删除')
    router.push('/')
  } catch {
    // cancel
  }
}

onMounted(() => {
  fetchNote()
})
</script>

<style scoped>
.note-detail-page {
  min-height: 100vh;
  background-color: #f7f8fa;
  display: flex;
  flex-direction: column;
}

.detail-header {
  height: 60px;
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  cursor: pointer;
  transition: color 0.2s;
}

.header-title:hover {
  color: #409eff;
}

.home-btn {
  margin-left: 4px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-container {
  max-width: 1200px;
  width: 100%;
  margin: 24px auto;
  padding: 0 20px;
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.article-wrapper {
  flex: 1;
  background: #ffffff;
  border-radius: 8px;
  padding: 36px 48px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.04);
}

.type-badge-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.article-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.4;
  margin: 0 0 20px 0;
}

.author-meta-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.author-text {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.publish-time {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.article-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #909399;
  font-size: 13px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.article-content {
  font-size: 16px;
  line-height: 1.8;
  color: #2c3e50;
  min-height: 300px;
}

.toc-sidebar {
  width: 280px;
  position: sticky;
  top: 84px;
}

.toc-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.04);
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.toc-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f2f5;
}

.catalog-nav {
  font-size: 13px;
}

@media (max-width: 900px) {
  .toc-sidebar {
    display: none;
  }
  .article-wrapper {
    padding: 20px;
  }
}
</style>
