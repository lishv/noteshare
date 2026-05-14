<template>
  <div class="profile-page">
    <div class="profile-header">
      <el-button @click="router.push('/')" :icon="ArrowLeft" circle size="small" />
      <h2>个人主页</h2>
    </div>

    <div v-if="loading" class="profile-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else-if="userInfo">
      <div class="profile-card">
        <img :src="userInfo.avatar || defaultAvatar" class="profile-avatar" />
        <div class="profile-info">
          <h3>{{ userInfo.username }}</h3>
          <p class="profile-meta">
            <span>笔记 {{ noteTotal }} 篇</span>
            <span v-if="userInfo.lastActiveAt">最后活跃 {{ formatDate(userInfo.lastActiveAt) }}</span>
          </p>
        </div>
      </div>

      <!-- 主链接 -->
      <div v-if="mainLink" class="main-link-card" @click="openLink(mainLink.mainUrl)">
        <el-icon><Link /></el-icon>
        <span class="main-link-url">{{ mainLink.mainUrl }}</span>
        <el-icon><ArrowRight /></el-icon>
      </div>

      <!-- 笔记列表 -->
      <div class="profile-notes">
        <h4>TA 的笔记</h4>
        <el-table :data="notes" stripe v-loading="notesLoading">
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="locationOrUrl" label="链接/位置" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <a v-if="isUrl(row.locationOrUrl)" :href="row.locationOrUrl" target="_blank" rel="noopener">{{ row.locationOrUrl }}</a>
              <span v-else>{{ row.locationOrUrl }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览" width="80" />
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ formatDate(row.updatedAt || row.createdAt) }}</template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="noteTotal > 0"
          v-model:current-page="notePage"
          v-model:page-size="noteSize"
          :total="noteTotal"
          :page-sizes="[10, 20]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchNotes"
          @current-change="fetchNotes"
          style="margin-top: 16px; justify-content: flex-end;"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Link, ArrowRight } from '@element-plus/icons-vue'
import { getActiveUsers, getMainLink, getUserNotes } from '@/api/note'
import type { User, MainLink, NoteItem } from '@/types'
import { isUrl, formatDate } from '@/utils/helpers'

const route = useRoute()
const router = useRouter()
const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" fill="%23409eff"/><text x="50" y="55" text-anchor="middle" fill="white" font-size="40">U</text></svg>')

const loading = ref(true)
const userInfo = ref<User | null>(null)
const mainLink = ref<MainLink | null>(null)
const notes = ref<NoteItem[]>([])
const noteTotal = ref(0)
const notePage = ref(1)
const noteSize = ref(10)
const notesLoading = ref(false)

const fetchProfile = async () => {
  loading.value = true
  try {
    const userId = Number(route.params.id)
    // 从活跃用户列表找到用户信息
    const res = await getActiveUsers({ keyword: '', page: 1, size: 200 })
    userInfo.value = res.data.list.find((u: User) => u.id === userId) || null

    // 获取主链接
    try {
      const linkRes = await getMainLink(userId)
      mainLink.value = linkRes.data
    } catch { mainLink.value = null }

    await fetchNotes()
  } catch { /* handled */ }
  finally { loading.value = false }
}

const fetchNotes = async () => {
  const userId = Number(route.params.id)
  notesLoading.value = true
  try {
    const res = await getUserNotes(userId, { page: notePage.value, size: noteSize.value })
    notes.value = res.data.list
    noteTotal.value = res.data.total
  } catch { /* handled */ }
  finally { notesLoading.value = false }
}

const openLink = (url: string) => {
  if (isUrl(url)) window.open(url, '_blank', 'noopener')
}

onMounted(fetchProfile)
</script>

<style scoped>
.profile-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 20px;
}
.profile-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.profile-header h2 {
  font-size: 20px;
  font-weight: 600;
}
.profile-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: var(--color-bg-card, #fff);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}
.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  background: #e8e8e8;
}
.profile-info h3 {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 6px;
}
.profile-meta {
  font-size: 14px;
  color: var(--color-text-tertiary, #999);
  display: flex;
  gap: 16px;
}
.main-link-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: var(--color-bg-card, #fff);
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.main-link-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}
.main-link-url {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-primary, #409eff);
  font-size: 14px;
}
.profile-notes h4 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}
</style>
