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
        <!-- 点赞按钮 (爱心，点赞变红) -->
        <el-button
          class="heart-btn"
          :class="{ 'is-liked': isLiked }"
          @click="handleToggleLike"
        >
          <span class="btn-icon heart-icon-wrapper">
            <svg v-if="isLiked" class="icon-svg heart-svg liked" viewBox="0 0 24 24" width="16" height="16" fill="#ef4444"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
            <svg v-else class="icon-svg heart-svg" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          </span>
          <span class="btn-label" :class="{ 'text-liked': isLiked }">{{ isLiked ? '已点赞' : '点赞' }} ({{ likeCount }})</span>
        </el-button>

        <!-- 收藏按钮 (五角星，收藏变黄) -->
        <el-button
          class="star-btn"
          :class="{ 'is-favorited': isFavorited }"
          @click="handleToggleFavorite"
        >
          <span class="btn-icon star-icon-wrapper">
            <svg v-if="isFavorited" class="icon-svg star-svg favorited" viewBox="0 0 24 24" width="16" height="16" fill="#f59e0b"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
            <svg v-else class="icon-svg star-svg" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          </span>
          <span class="btn-label" :class="{ 'text-favorited': isFavorited }">{{ isFavorited ? '已收藏' : '收藏' }} ({{ favoriteCount }})</span>
        </el-button>

        <el-button
          v-if="note.noteType === 2"
          class="export-markdown-btn"
          :icon="Download"
          @click="handleExportMarkdown"
          title="下载 Markdown 文件"
        >
          导出 Markdown
        </el-button>
        <el-button
          v-else-if="externalUrl"
          type="primary"
          plain
          :icon="TopRight"
          @click="openExternalUrl"
          title="在新标签页中访问外链"
        >
          访问外链
        </el-button>
        <el-button
          class="share-poster-btn"
          type="success"
          plain
          :icon="Share"
          @click="openSharePoster"
          title="生成分享海报"
        >
          分享海报
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
            <el-tag :type="note.noteType === 2 ? 'success' : 'info'" effect="plain" round size="small">
              {{ note.noteType === 2 ? '原创创作笔记' : '外部资源链接' }}
            </el-tag>
            <el-tag v-if="note.isPinned === 1" type="warning" size="small">置顶</el-tag>
            <el-tag v-if="note.isPublic === 0" type="danger" effect="dark" size="small" round>
              <el-icon style="margin-right: 3px; vertical-align: -1px;"><Lock /></el-icon>私密 (仅自己可见)
            </el-tag>
          </div>

          <h1 class="article-title">{{ note.title }}</h1>

          <!-- 标签展示 -->
          <div v-if="note.tags && note.tags.length" class="article-tags-row">
            <el-tag
              v-for="t in note.tags"
              :key="t"
              size="small"
              class="article-tag-item"
              type="info"
              effect="light"
            >
              # {{ t }}
            </el-tag>
          </div>

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
              <span class="stat-item" :class="{ 'stat-liked': isLiked }">
                <svg viewBox="0 0 24 24" width="14" height="14" :fill="isLiked ? '#ef4444' : 'none'" :stroke="isLiked ? '#ef4444' : 'currentColor'" stroke-width="2" style="vertical-align: -2px; margin-right: 3px;"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
                {{ likeCount }} 次点赞
              </span>
              <span class="stat-item" :class="{ 'stat-favorited': isFavorited }">
                <svg viewBox="0 0 24 24" width="14" height="14" :fill="isFavorited ? '#f59e0b' : 'none'" :stroke="isFavorited ? '#f59e0b' : 'currentColor'" stroke-width="2" style="vertical-align: -2px; margin-right: 3px;"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                {{ favoriteCount }} 次收藏
              </span>
            </div>
          </div>
        </div>

        <el-divider />

        <!-- 1. 外链笔记展示模块 (不自动强制跳转，清晰展示链接地址，用户点击按钮或链接才跳转) -->
        <div v-if="note.noteType === 1" class="external-note-wrapper">
          <div class="external-link-card">
            <div class="external-header-row">
              <div class="external-icon-box">
                <el-icon :size="28" color="#409eff"><Link /></el-icon>
              </div>
              <div class="external-info-box">
                <div class="external-badge-label">收录的外部资源链接</div>
                <a
                  :href="externalUrl || undefined"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="external-url-link"
                  :title="note.locationOrUrl"
                >
                  {{ note.locationOrUrl }}
                </a>
              </div>
            </div>

            <div class="external-action-row">
              <el-button
                type="primary"
                size="default"
                :icon="TopRight"
                @click="openExternalUrl"
              >
                访问外部链接
              </el-button>
              <el-button
                size="default"
                :icon="DocumentCopy"
                @click="copyExternalUrl"
              >
                复制链接
              </el-button>
            </div>
            <div class="external-notice">
              💡 提示：此笔记为作者收录分享的外部链接，点击“访问外部链接”将在新标签页中安全打开，不会强制离开本站。
            </div>
          </div>

          <!-- 笔记摘要 / 导读 -->
          <div v-if="note.summary" class="external-summary-block">
            <h3 class="external-section-title">📌 笔记导读 / 摘要说明</h3>
            <p class="summary-paragraph">{{ note.summary }}</p>
          </div>

          <!-- 详细内容或备忘 -->
          <div v-if="note.content && note.content.trim()" class="external-content-block">
            <h3 class="external-section-title">📝 详细备忘与笔记内容</h3>
            <MdPreview
              editorId="note-preview-content"
              :modelValue="note.content"
            />
          </div>
        </div>

        <!-- 2. 原创 Markdown 笔记正文预览 -->
        <main v-else class="article-content">
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

      <!-- 右侧目录浮窗 (TOC) - 仅在原创Markdown笔记且有内容时展示 -->
      <aside class="toc-sidebar" v-if="note.noteType === 2 && note.content">
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

    <!-- 分享海报预览 -->
    <el-dialog
      v-model="shareDialogVisible"
      title="分享海报"
      width="min(92vw, 560px)"
      class="share-poster-dialog"
      destroy-on-close
      @opened="renderSharePoster"
    >
      <div class="poster-preview" v-loading="posterGenerating">
        <canvas
          ref="posterCanvas"
          class="share-poster-canvas"
          aria-label="笔记分享海报预览"
        ></canvas>
      </div>
      <template #footer>
        <el-button @click="copyShareLink">复制笔记链接</el-button>
        <el-button @click="shareDialogVisible = false">关闭</el-button>
        <el-button
          type="success"
          :loading="posterGenerating"
          :disabled="posterGenerating || !posterCanvas"
          @click="downloadSharePoster"
        >
          下载海报
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Star,
  StarFilled,
  EditPen,
  Delete,
  View,
  Reading,
  House,
  Lock,
  Download,
  Share,
  Link,
  TopRight,
  DocumentCopy,
} from '@element-plus/icons-vue'
import { MdPreview, MdCatalog } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import {
  getNoteById,
  deleteNote,
  addFavorite,
  removeFavorite,
  checkFavorite,
  likeNote,
  unlikeNote,
  checkNoteLiked,
} from '@/api/note'
import type { NoteItem } from '@/types'
import { useUserStore } from '@/store/user'
import { formatDate, isSafeUrl } from '@/utils/helpers'
import CommentSection from '@/components/CommentSection.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const noteId = Number(route.params.id)
const note = ref<NoteItem | null>(null)
const loading = ref(true)
const isFavorited = ref(false)
const favoriteCount = ref(0)
const isLiked = ref(false)
const likeCount = ref(0)
const likeLoading = ref(false)
const scrollElement = ref<HTMLElement | string>('html')
const shareDialogVisible = ref(false)
const posterGenerating = ref(false)
const posterCanvas = ref<HTMLCanvasElement | null>(null)

const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" fill="%23409eff"/><text x="50" y="55" text-anchor="middle" fill="white" font-size="40">U</text></svg>')

const isOwner = computed(() => {
  return userStore.userInfo && note.value && userStore.userInfo.id === note.value.userId
})

const externalUrl = computed(() => {
  const value = note.value?.locationOrUrl?.trim()
  if (!value) return null
  const targetUrl = /^https?:\/\//i.test(value) ? value : `https://${value}`
  return isSafeUrl(targetUrl) && /^https?:\/\//i.test(targetUrl) ? targetUrl : null
})

async function fetchNote() {
  loading.value = true
  try {
    const res = await getNoteById(noteId)
    note.value = res.data
    favoriteCount.value = note.value.favoriteCount || 0
    likeCount.value = note.value.likeCount || 0
    isFavorited.value = note.value.isFavorited === true
    isLiked.value = note.value.isLiked === true
    await checkFavStatus()
    await checkLikeStatus()
  } catch {
    ElMessage.error('获取笔记详情失败')
    router.push('/')
  } finally {
    loading.value = false
  }
}

// 访问外部链接（用户主动点击后新窗口打开）
function openExternalUrl() {
  if (!externalUrl.value) {
    ElMessage.warning('链接地址无效，仅支持 HTTP/HTTPS 链接')
    return
  }
  window.open(externalUrl.value, '_blank', 'noopener,noreferrer')
}

// 复制外部链接
function copyExternalUrl() {
  const url = note.value?.locationOrUrl
  if (!url) return
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url).then(() => {
      ElMessage.success('外部链接已复制到剪贴板')
    }).catch(() => {
      ElMessage.info(`链接地址：${url}`)
    })
  } else {
    ElMessage.info(`链接地址：${url}`)
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

async function checkLikeStatus() {
  if (!userStore.token) return
  try {
    const res = await checkNoteLiked(noteId)
    isLiked.value = res.data
  } catch {
    // ignore
  }
}

async function handleToggleLike() {
  if (!userStore.token) {
    ElMessage.warning('请先登录后再点赞')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  if (likeLoading.value) return

  const previousLiked = isLiked.value
  const previousCount = likeCount.value
  isLiked.value = !previousLiked
  likeCount.value = Math.max(0, previousCount + (isLiked.value ? 1 : -1))
  likeLoading.value = true
  try {
    if (isLiked.value) {
      await likeNote(noteId)
      ElMessage.success('点赞成功')
    } else {
      await unlikeNote(noteId)
      ElMessage.success('已取消点赞')
    }
  } catch {
    isLiked.value = previousLiked
    likeCount.value = previousCount
  } finally {
    likeLoading.value = false
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
      if (note.value) note.value.isFavorited = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(noteId)
      isFavorited.value = true
      favoriteCount.value += 1
      if (note.value) note.value.isFavorited = true
      ElMessage.success('收藏成功')
    }
  } catch {
    // handled
  }
}

function sanitizeFileName(value: string): string {
  const normalized = value.trim().replace(/[\\/:*?"<>|\\x00-\\x1f]/g, '_').replace(/\s+/g, ' ')
  return (normalized || 'note').slice(0, 80)
}

function handleExportMarkdown() {
  if (!note.value) return
  const title = note.value.title?.trim() || '未命名笔记'
  const lines = [`# ${title}`, '', `> 作者：${note.value.username || '匿名用户'}`]
  if (note.value.createdAt) lines.push(`> 发布于：${formatDate(note.value.createdAt)}`)
  if (note.value.summary) lines.push('', `> ${note.value.summary.trim()}`)
  lines.push('', note.value.content || '')

  const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `${sanitizeFileName(title)}.md`
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
  ElMessage.success('Markdown 文件已开始下载')
}

function getShareUrl(): string {
  return `${window.location.origin}/note/${noteId}`
}

async function copyShareLink() {
  try {
    await navigator.clipboard.writeText(getShareUrl())
    ElMessage.success('笔记链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制地址栏链接')
  }
}

function wrapCanvasText(
  ctx: CanvasRenderingContext2D,
  text: string,
  maxWidth: number,
  maxLines: number,
): string[] {
  const chars = Array.from(text || '')
  const lines: string[] = []
  let line = ''
  for (const char of chars) {
    const candidate = line + char
    if (line && ctx.measureText(candidate).width > maxWidth) {
      lines.push(line)
      line = char
      if (lines.length >= maxLines) break
    } else {
      line = candidate
    }
  }
  if (lines.length < maxLines && line) lines.push(line)
  if (lines.length === maxLines && chars.length > 0) {
    let last = lines[maxLines - 1] || ''
    while (last && ctx.measureText(`${last}…`).width > maxWidth) {
      last = Array.from(last).slice(0, -1).join('')
    }
    lines[maxLines - 1] = `${last}…`
  }
  return lines
}

async function loadShareQrImage(value: string): Promise<HTMLImageElement | null> {
  const endpoint = `/api/user/share/qr-code?text=${encodeURIComponent(value)}`
  try {
    const response = await fetch(endpoint, {
      credentials: 'same-origin',
    })
    if (!response.ok) return null

    const blob = await response.blob()
    const imageUrl = URL.createObjectURL(blob)
    try {
      const image = new Image()
      await new Promise<void>((resolve, reject) => {
        image.onload = () => resolve()
        image.onerror = () => reject(new Error('二维码图片加载失败'))
        image.src = imageUrl
      })
      return image
    } finally {
      URL.revokeObjectURL(imageUrl)
    }
  } catch {
    return null
  }
}

function openSharePoster() {
  if (!note.value) return
  shareDialogVisible.value = true
}

async function renderSharePoster() {
  const currentNote = note.value
  const canvas = posterCanvas.value
  if (!currentNote || !canvas) return

  posterGenerating.value = true
  try {
    const scale = Math.min(2, Math.max(1, window.devicePixelRatio || 1))
    const width = 720
    const height = 960
    canvas.width = width * scale
    canvas.height = height * scale
    canvas.style.width = `${width}px`
    canvas.style.height = `${height}px`
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    ctx.scale(scale, scale)

    const gradient = ctx.createLinearGradient(0, 0, width, height)
    gradient.addColorStop(0, '#eff6ff')
    gradient.addColorStop(0.52, '#ffffff')
    gradient.addColorStop(1, '#ecfdf5')
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, width, height)

    ctx.fillStyle = 'rgba(64, 158, 255, 0.1)'
    ctx.beginPath()
    ctx.arc(width - 30, 20, 190, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = 'rgba(16, 185, 129, 0.08)'
    ctx.beginPath()
    ctx.arc(30, height - 10, 180, 0, Math.PI * 2)
    ctx.fill()

    ctx.fillStyle = '#409eff'
    ctx.font = '600 22px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'
    ctx.fillText('笔记共享平台', 56, 68)
    ctx.fillStyle = '#94a3b8'
    ctx.font = '400 14px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'
    ctx.fillText('SHARE YOUR KNOWLEDGE', 56, 94)

    const title = currentNote.title || '未命名笔记'
    ctx.fillStyle = '#172033'
    ctx.font = '700 38px -apple-system, BlinkMacSystemFont, "Microsoft YaHei", sans-serif'
    const titleLines = wrapCanvasText(ctx, title, width - 112, 3)
    titleLines.forEach((line, index) => ctx.fillText(line, 56, 178 + index * 52))

    const titleBottom = 178 + titleLines.length * 52
    ctx.fillStyle = '#64748b'
    ctx.font = '400 18px -apple-system, BlinkMacSystemFont, "Microsoft YaHei", sans-serif'
    const summary = currentNote.summary || '记录思考，分享知识，让每一次阅读都产生新的连接。'
    const summaryLines = wrapCanvasText(ctx, summary, width - 112, 4)
    summaryLines.forEach((line, index) => ctx.fillText(line, 56, titleBottom + 20 + index * 30))

    const authorY = titleBottom + 170
    ctx.fillStyle = '#e0f2fe'
    ctx.beginPath()
    ctx.roundRect(48, authorY - 34, width - 96, 72, 18)
    ctx.fill()
    ctx.fillStyle = '#0f172a'
    ctx.font = '600 19px -apple-system, BlinkMacSystemFont, "Microsoft YaHei", sans-serif'
    ctx.fillText(`作者：${currentNote.username || '匿名用户'}`, 76, authorY - 2)
    ctx.fillStyle = '#64748b'
    ctx.font = '400 14px -apple-system, BlinkMacSystemFont, "Microsoft YaHei", sans-serif'
    ctx.fillText(`发布于 ${formatDate(currentNote.createdAt) || '刚刚'}`, 76, authorY + 23)

    const qrSize = 142
    const qrX = width - qrSize - 64
    const qrY = height - qrSize - 112
    const qrImage = await loadShareQrImage(getShareUrl())
    if (qrImage) {
      ctx.drawImage(qrImage, qrX, qrY, qrSize, qrSize)
      ctx.strokeStyle = '#d1d5db'
      ctx.lineWidth = 1
      ctx.strokeRect(qrX, qrY, qrSize, qrSize)
    } else {
      ctx.fillStyle = '#ffffff'
      ctx.fillRect(qrX, qrY, qrSize, qrSize)
      ctx.strokeStyle = '#d1d5db'
      ctx.lineWidth = 1
      ctx.strokeRect(qrX, qrY, qrSize, qrSize)
      ctx.fillStyle = '#64748b'
      ctx.font = '400 13px -apple-system, BlinkMacSystemFont, "Microsoft YaHei", sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText('二维码生成失败', qrX + qrSize / 2, qrY + qrSize / 2)
      ctx.textAlign = 'left'
      ElMessage.warning('二维码生成失败，请稍后重试')
    }
    ctx.fillStyle = '#475569'
    ctx.font = '400 14px -apple-system, BlinkMacSystemFont, "Microsoft YaHei", sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText('扫码或点击链接阅读', qrX + qrSize / 2, qrY + qrSize + 28)
    ctx.textAlign = 'left'

    ctx.fillStyle = '#94a3b8'
    ctx.font = '400 13px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'
    ctx.fillText('note sharing platform  ·  knowledge grows by sharing', 56, height - 38)
    ctx.fillStyle = 'rgba(64, 158, 255, 0.12)'
    ctx.font = '700 46px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'
    ctx.fillText('NOTE', 56, height - 122)
  } finally {
    posterGenerating.value = false
  }
}

function downloadSharePoster() {
  if (!posterCanvas.value || !note.value) return
  const anchor = document.createElement('a')
  anchor.href = posterCanvas.value.toDataURL('image/png')
  anchor.download = `${sanitizeFileName(note.value.title || 'note')}-分享海报.png`
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  ElMessage.success('分享海报已开始下载')
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

.heart-btn,
.star-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 20px;
  padding: 8px 16px;
}

.heart-btn.is-liked {
  background-color: #fef2f2;
  border-color: #fecaca;
  color: #ef4444;
}

.heart-btn.is-liked:hover {
  background-color: #fee2e2;
  border-color: #fca5a5;
  color: #dc2626;
}

.star-btn.is-favorited {
  background-color: #fffbeb;
  border-color: #fde68a;
  color: #d97706;
}

.star-btn.is-favorited:hover {
  background-color: #fef3c7;
  border-color: #fcd34d;
  color: #b45309;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.heart-svg.liked {
  animation: heartPulse 0.35s ease-out;
}

.star-svg.favorited {
  animation: starPop 0.35s ease-out;
}

@keyframes heartPulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.35); }
  100% { transform: scale(1); }
}

@keyframes starPop {
  0% { transform: scale(1) rotate(0deg); }
  50% { transform: scale(1.35) rotate(15deg); }
  100% { transform: scale(1) rotate(0deg); }
}

.text-liked {
  color: #ef4444;
  font-weight: 600;
}

.text-favorited {
  color: #d97706;
  font-weight: 600;
}

.article-tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.article-tag-item {
  border-radius: 12px;
  font-size: 13px;
  padding: 0 10px;
  height: 26px;
  line-height: 24px;
}

.stat-item.stat-liked {
  color: #ef4444;
  font-weight: 500;
}

.stat-item.stat-favorited {
  color: #d97706;
  font-weight: 500;
}

.poster-preview {
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 10px;
}

.share-poster-canvas {
  display: block;
  width: min(100%, 480px) !important;
  height: auto !important;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
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
  min-width: 0;
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
  min-width: 0;
}

/* Markdown 代码块需要限制在文章内容内，避免 sticky 工具栏覆盖页面弹窗 */
.article-content :deep(.md-editor-preview),
.external-content-block :deep(.md-editor-preview) {
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
  overflow-wrap: anywhere;
}

.article-content :deep(.md-editor-code),
.external-content-block :deep(.md-editor-code) {
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
}

.article-content :deep(.md-editor-code .md-editor-code-head),
.external-content-block :deep(.md-editor-code .md-editor-code-head) {
  /* md-editor 默认 z-index: 10000 会盖住 Element Plus 对话框 */
  position: relative !important;
  top: auto !important;
  inset-block-start: auto !important;
  z-index: 1 !important;
  max-width: 100%;
}

.article-content :deep(.md-editor-code pre),
.external-content-block :deep(.md-editor-code pre),
.article-content :deep(.md-editor-code pre code),
.external-content-block :deep(.md-editor-code pre code) {
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
  overflow-x: auto;
}

.article-content :deep(.md-editor-code pre code .md-editor-code-block),
.external-content-block :deep(.md-editor-code pre code .md-editor-code-block) {
  min-width: 100%;
  max-width: none;
}

/* 外链笔记专属展示卡片样式 */
.external-note-wrapper {
  margin-bottom: 28px;
}

.external-link-card {
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 100%);
  border: 1px solid #d0e5ff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.08);
}

.external-header-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.external-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: #ecf5ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.external-info-box {
  flex: 1;
  min-width: 0;
}

.external-badge-label {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.external-url-link {
  font-size: 16px;
  font-weight: 500;
  color: #1e40af;
  text-decoration: underline;
  word-break: break-all;
  display: block;
  line-height: 1.5;
  transition: color 0.2s;
}

.external-url-link:hover {
  color: #2563eb;
}

.external-action-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.external-notice {
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
  background: rgba(255, 255, 255, 0.7);
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid #60a5fa;
}

.external-summary-block,
.external-content-block {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 24px;
}

.external-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 12px 0;
}

.summary-paragraph {
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
  margin: 0;
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
  .detail-header {
    padding: 0 16px;
  }
  .header-actions {
    gap: 6px;
  }
  .header-actions .el-button {
    padding: 8px 10px;
  }
  .header-actions .el-button:not(.export-markdown-btn):not(.share-poster-btn) {
    font-size: 0;
  }
  .header-actions .el-button:not(.export-markdown-btn):not(.share-poster-btn) .el-icon {
    margin: 0;
  }
  .export-markdown-btn,
  .share-poster-btn {
    font-size: 0;
  }
  .export-markdown-btn .el-icon,
  .share-poster-btn .el-icon {
    margin: 0;
  }
  .home-btn {
    display: none;
  }
  .header-title {
    font-size: 14px;
  }
  .detail-container {
    padding: 0 12px;
    margin: 12px auto;
  }
  .article-wrapper {
    padding: 20px 16px;
  }
  .article-title {
    font-size: 24px;
  }
  .author-meta-bar {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
}

@media (max-width: 560px) {
  .detail-header {
    height: 54px;
  }
  .header-actions {
    flex-wrap: wrap;
    justify-content: flex-end;
  }
  .poster-preview {
    padding: 0;
  }
}
</style>
