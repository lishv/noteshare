<template>
  <div class="likes-page">
    <div class="likes-header">
      <div class="likes-header__title">
        <el-button :icon="ArrowLeft" circle size="small" aria-label="返回首页" @click="router.push('/')" />
        <div>
          <h2>我的点赞</h2>
          <p>查看你点赞过的公开笔记</p>
        </div>
      </div>
      <el-button :icon="Refresh" text :loading="loading" @click="fetchData">刷新</el-button>
    </div>

    <el-card shadow="never" class="likes-card">
      <el-table :data="notes" v-loading="loading" stripe>
        <el-table-column label="笔记" min-width="260">
          <template #default="{ row }">
            <div class="note-title-cell" @click="openNote(row)">
              <div class="note-title-line">
                <el-tag :type="row.noteType === 2 ? 'success' : 'info'" size="small">
                  {{ row.noteType === 2 ? '原创' : '外链' }}
                </el-tag>
                <strong>{{ row.title }}</strong>
              </div>
              <p class="note-summary">{{ row.summary || (row.noteType === 2 ? 'Markdown 创作笔记，点击查看正文' : '外部知识链接，点击查看地址') }}</p>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="username" label="作者" width="140">
          <template #default="{ row }">
            <button type="button" class="author-link" @click="router.push(`/user/${row.userId}`)">
              <el-avatar :size="26" :src="row.avatar || undefined">{{ row.username?.charAt(0) || 'U' }}</el-avatar>
              <span>{{ row.username || '匿名作者' }}</span>
            </button>
          </template>
        </el-table-column>

        <el-table-column label="内容 / 链接" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.noteType === 2" class="content-link" @click="openNote(row)">
              <el-icon><Document /></el-icon>
              <span>{{ row.summary || '在线创作笔记（点击查看全文）' }}</span>
            </div>
            <template v-else>
              <button
                v-if="getSafeExternalUrl(row.locationOrUrl)"
                type="button"
                class="external-link"
                @click="openExternalUrl(row.locationOrUrl)"
              >
                <el-icon><Link /></el-icon>
                <span>{{ row.locationOrUrl }}</span>
                <el-icon><TopRight /></el-icon>
              </button>
              <span v-else class="muted-text">{{ row.locationOrUrl || '暂无链接' }}</span>
            </template>
          </template>
        </el-table-column>

        <el-table-column label="互动" width="150" align="center">
          <template #default="{ row }">
            <div class="interaction-stats">
              <span class="liked-count"><el-icon><Pointer /></el-icon>{{ row.likeCount || 0 }}</span>
              <span><el-icon><Star /></el-icon>{{ row.favoriteCount || 0 }}</span>
              <span><el-icon><ChatDotRound /></el-icon>{{ row.commentCount || 0 }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openNote(row)">查看内容</el-button>
            <el-button link type="danger" :loading="unlikingId === row.id" @click="handleUnlike(row)">取消点赞</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && notes.length === 0" description="还没有点赞过笔记" />

      <div v-if="total > 0" class="likes-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ChatDotRound,
  Document,
  Link,
  Pointer,
  Refresh,
  Star,
  TopRight,
} from '@element-plus/icons-vue'
import { getMyLikes, unlikeNote } from '@/api/note'
import type { NoteItem } from '@/types'
import { isSafeUrl } from '@/utils/helpers'

const router = useRouter()
const loading = ref(false)
const notes = ref<NoteItem[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const unlikingId = ref<number | null>(null)

function getSafeExternalUrl(url?: string): string | null {
  const value = url?.trim()
  if (!value) return null
  const target = /^https?:\/\//i.test(value) ? value : `https://${value}`
  return /^https?:\/\//i.test(target) && isSafeUrl(target) ? target : null
}

function openExternalUrl(url?: string) {
  const target = getSafeExternalUrl(url)
  if (!target) {
    ElMessage.warning('链接地址无效，仅支持 HTTP/HTTPS 链接')
    return
  }
  window.open(target, '_blank', 'noopener,noreferrer')
}

function openNote(row: NoteItem) {
  router.push(`/note/${row.id}`)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getMyLikes({ page: page.value, size: size.value })
    notes.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    notes.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handlePageChange() {
  fetchData()
}

async function handleUnlike(row: NoteItem) {
  try {
    await ElMessageBox.confirm(`取消对《${row.title}》的点赞吗？`, '提示', {
      confirmButtonText: '取消点赞',
      cancelButtonText: '保留点赞',
      type: 'warning',
    })
  } catch {
    return
  }

  unlikingId.value = row.id
  try {
    await unlikeNote(row.id)
    ElMessage.success('已取消点赞')
    if (notes.value.length === 1 && page.value > 1) page.value -= 1
    await fetchData()
  } catch {
    // 请求错误由统一拦截器提示
  } finally {
    unlikingId.value = null
  }
}

onMounted(fetchData)
</script>

<style scoped>
.likes-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 22px;
}

.likes-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.likes-header__title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.likes-header h2 {
  margin: 0;
  color: #1e293b;
  font-size: 22px;
  font-weight: 700;
}

.likes-header p {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 13px;
}

.likes-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.note-title-cell {
  min-width: 0;
  cursor: pointer;
}

.note-title-line {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.note-title-line strong {
  overflow: hidden;
  color: #2563eb;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-title-cell:hover strong {
  text-decoration: underline;
}

.note-summary {
  overflow: hidden;
  margin: 6px 0 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author-link,
.external-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  max-width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: #475569;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.author-link:hover,
.external-link:hover {
  color: #2563eb;
}

.author-link span,
.external-link span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  color: #64748b;
  cursor: pointer;
}

.content-link span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.external-link {
  width: 100%;
  color: #2563eb;
}

.external-link .el-icon:last-child {
  flex: 0 0 auto;
}

.muted-text {
  color: #94a3b8;
}

.interaction-stats {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #94a3b8;
  font-size: 12px;
}

.interaction-stats span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.interaction-stats .liked-count {
  color: #ef4444;
}

.likes-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 700px) {
  .likes-page {
    padding: 18px 12px;
  }

  .likes-header h2 {
    font-size: 19px;
  }

  .likes-pagination {
    justify-content: center;
  }
}
</style>
