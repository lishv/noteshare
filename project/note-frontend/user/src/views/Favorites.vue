<template>
  <div class="favorites-page">
    <div class="favorites-header">
      <el-button @click="router.push('/')" :icon="ArrowLeft" circle size="small" />
      <h2>我的收藏</h2>
    </div>

    <el-table :data="notes" v-loading="loading" stripe>
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="username" label="作者" width="120" />
      <el-table-column prop="locationOrUrl" label="链接/位置" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <a v-if="isUrl(row.locationOrUrl)" :href="row.locationOrUrl" target="_blank" rel="noopener">{{ row.locationOrUrl }}</a>
          <span v-else>{{ row.locationOrUrl }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="danger" @click="handleUnfavorite(row)">取消收藏</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20]"
      layout="total, sizes, prev, pager, next"
      @size-change="fetchData"
      @current-change="fetchData"
      style="margin-top: 20px; justify-content: flex-end;"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyFavorites, removeFavorite } from '@/api/note'
import type { NoteItem } from '@/types'
import { isUrl } from '@/utils/helpers'

const router = useRouter()
const loading = ref(false)
const notes = ref<NoteItem[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMyFavorites({ page: page.value, size: size.value })
    notes.value = res.data.list
    total.value = res.data.total
  } catch { /* handled */ }
  finally { loading.value = false }
}

const handleUnfavorite = async (row: NoteItem) => {
  await ElMessageBox.confirm(`取消收藏 "${row.title}" 吗？`, '提示', { type: 'warning' })
  try {
    await removeFavorite(row.id)
    ElMessage.success('已取消收藏')
    fetchData()
  } catch { /* handled */ }
}

onMounted(fetchData)
</script>

<style scoped>
.favorites-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 20px;
}
.favorites-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.favorites-header h2 {
  font-size: 20px;
  font-weight: 600;
}
</style>
