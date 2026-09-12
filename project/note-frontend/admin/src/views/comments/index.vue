<template>
  <div class="comment-management">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索评论内容/作者/笔记"
        clearable
        style="width: 260px;"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="statusFilter"
        placeholder="删除状态"
        clearable
        style="width: 130px;"
        @change="handleSearch"
      >
        <el-option label="正常" :value="0" />
        <el-option label="已删除" :value="1" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px;"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="评论ID" width="85" />
      <el-table-column label="所属笔记" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="note-cell">
            <span class="note-title">{{ row.noteTitle || '笔记已不存在' }}</span>
            <span class="note-id">#{{ row.noteId }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="评论作者" width="130">
        <template #default="{ row }">
          <span>{{ row.username || `用户 #${row.userId}` }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="评论内容" min-width="260" show-overflow-tooltip />
      <el-table-column label="回复关系" width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.replyToUsername">回复 @{{ row.replyToUsername }}</span>
          <span v-else-if="row.replyToCommentId">回复评论 #{{ row.replyToCommentId }}</span>
          <span v-else-if="row.parentId">子评论 #{{ row.parentId }}</span>
          <span v-else class="muted">顶级评论</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.isDeleted === 1 ? 'danger' : 'success'" size="small">
            {{ row.isDeleted === 1 ? '已删除' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="175" />
      <el-table-column prop="updatedAt" label="更新时间" width="175" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.isDeleted !== 1" link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
          <el-button v-else link type="success" @click="handleRestore(row)">
            恢复
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="fetchData"
      @current-change="fetchData"
      style="margin-top: 20px; justify-content: flex-end;"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteComment, getCommentList, restoreComment } from '@/api/admin'

interface AdminComment {
  id: number
  noteId?: number
  noteTitle?: string
  userId?: number
  username?: string
  parentId?: number
  replyToUsername?: string
  replyToCommentId?: number
  content?: string
  isDeleted?: number
  createdAt?: string
  updatedAt?: string
}

const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const dateRange = ref<[string, string] | null>(null)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<AdminComment[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    const params: {
      page: number
      size: number
      keyword?: string
      isDeleted?: number
      startTime?: string
      endTime?: string
    } = {
      page: page.value,
      size: size.value,
    }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (statusFilter.value !== undefined) params.isDeleted = statusFilter.value
    if (dateRange.value) {
      params.startTime = `${dateRange.value[0]} 00:00:00`
      params.endTime = `${dateRange.value[1]} 23:59:59`
    }

    const res = await getCommentList(params)
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    // 统一请求拦截器负责提示错误
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

const handleReset = () => {
  keyword.value = ''
  statusFilter.value = undefined
  dateRange.value = null
  page.value = 1
  fetchData()
}

const handleDelete = async (row: AdminComment) => {
  try {
    await ElMessageBox.confirm(
      `确定删除这条评论吗？\n${row.content || ''}`,
      '删除评论',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  try {
    await deleteComment(row.id)
    ElMessage.success('评论已删除')
    await fetchData()
  } catch {
    // 统一请求拦截器负责提示错误
  }
}

const handleRestore = async (row: AdminComment) => {
  try {
    await ElMessageBox.confirm('确定恢复这条评论吗？', '恢复评论', {
      type: 'warning',
      confirmButtonText: '恢复',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    await restoreComment(row.id)
    ElMessage.success('评论已恢复')
    await fetchData()
  } catch {
    // 统一请求拦截器负责提示错误
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.comment-management {
  padding: 20px;
}

.search-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.note-cell {
  display: flex;
  align-items: baseline;
  gap: 6px;
  min-width: 0;
}

.note-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-id {
  flex: 0 0 auto;
  color: #909399;
  font-size: 12px;
}

.muted {
  color: #909399;
}
</style>
