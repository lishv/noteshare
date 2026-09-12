<template>
  <div class="notification-management">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索标题/内容/用户"
        clearable
        style="width: 230px;"
        @keyup.enter="handleSearch"
      />
      <el-input
        v-model="userIdText"
        placeholder="接收用户ID"
        clearable
        style="width: 140px;"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="typeFilter"
        placeholder="消息类型"
        clearable
        style="width: 130px;"
        @change="handleSearch"
      >
        <el-option label="评论" value="COMMENT" />
        <el-option label="关注" value="FOLLOW" />
        <el-option label="点赞" value="LIKE" />
        <el-option label="系统" value="SYSTEM" />
      </el-select>
      <el-select
        v-model="readFilter"
        placeholder="阅读状态"
        clearable
        style="width: 120px;"
        @change="handleSearch"
      >
        <el-option label="未读" :value="0" />
        <el-option label="已读" :value="1" />
      </el-select>
      <el-select
        v-model="deletedFilter"
        placeholder="移除状态"
        clearable
        style="width: 120px;"
        @change="handleSearch"
      >
        <el-option label="正常" :value="0" />
        <el-option label="已移除" :value="1" />
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
      <el-table-column prop="id" label="消息ID" width="85" />
      <el-table-column label="接收用户" width="150">
        <template #default="{ row }">
          <div>{{ row.receiverUsername || `用户 #${row.userId}` }}</div>
          <span class="sub-text">ID: {{ row.userId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发送者" width="135">
        <template #default="{ row }">
          <span>{{ row.senderUsername || '系统' }}</span>
          <span v-if="row.senderId" class="sub-text">ID: {{ row.senderId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="85">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)" size="small">{{ typeName(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="消息内容" min-width="300" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="message-content">
            <strong>{{ row.title }}</strong>
            <span>{{ row.content }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="关联对象" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <div v-if="row.targetType || row.targetId">
            <span>{{ targetTypeName(row.targetType) }} #{{ row.targetId || '-' }}</span>
            <span v-if="row.targetTitle" class="target-title">{{ row.targetTitle }}</span>
          </div>
          <span v-else class="sub-text">无</span>
        </template>
      </el-table-column>
      <el-table-column label="阅读状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.isRead === 1 ? 'info' : 'warning'" size="small">
            {{ row.isRead === 1 ? '已读' : '未读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="移除状态" width="95">
        <template #default="{ row }">
          <el-tag :type="row.isDeleted === 1 ? 'danger' : 'success'" size="small">
            {{ row.isDeleted === 1 ? '已移除' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="175" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.isDeleted !== 1" link type="danger" @click="handleDelete(row)">
            移除
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
import {
  deleteNotification,
  getNotificationList,
  restoreNotification,
} from '@/api/admin'

interface AdminNotification {
  id: number
  userId?: number
  receiverUsername?: string
  senderId?: number
  senderUsername?: string
  type?: string
  title?: string
  content?: string
  targetType?: string
  targetId?: number
  targetTitle?: string
  isRead?: number
  isDeleted?: number
  createdAt?: string
}

const typeNames: Record<string, string> = {
  COMMENT: '评论',
  FOLLOW: '关注',
  LIKE: '点赞',
  SYSTEM: '系统',
}

const typeTags: Record<string, string> = {
  COMMENT: 'primary',
  FOLLOW: 'success',
  LIKE: 'danger',
  SYSTEM: 'warning',
}

const targetNames: Record<string, string> = {
  NOTE: '笔记',
  USER: '用户',
  COMMENT: '评论',
  SYSTEM: '系统',
}

const loading = ref(false)
const keyword = ref('')
const userIdText = ref('')
const typeFilter = ref('')
const readFilter = ref<number | undefined>(undefined)
const deletedFilter = ref<number | undefined>(undefined)
const dateRange = ref<[string, string] | null>(null)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<AdminNotification[]>([])

const typeName = (type?: string) => typeNames[type || ''] || type || '未知'
const typeTag = (type?: string) => typeTags[type || ''] || 'info'
const targetTypeName = (type?: string) => targetNames[type || ''] || type || '对象'

const fetchData = async () => {
  loading.value = true
  try {
    const params: {
      page: number
      size: number
      keyword?: string
      userId?: number
      type?: string
      isRead?: number
      isDeleted?: number
      startTime?: string
      endTime?: string
    } = {
      page: page.value,
      size: size.value,
    }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()

    const parsedUserId = Number(userIdText.value.trim())
    if (userIdText.value.trim() && Number.isInteger(parsedUserId) && parsedUserId > 0) {
      params.userId = parsedUserId
    }
    if (typeFilter.value) params.type = typeFilter.value
    if (readFilter.value !== undefined) params.isRead = readFilter.value
    if (deletedFilter.value !== undefined) params.isDeleted = deletedFilter.value
    if (dateRange.value) {
      params.startTime = `${dateRange.value[0]} 00:00:00`
      params.endTime = `${dateRange.value[1]} 23:59:59`
    }

    const res = await getNotificationList(params)
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
  userIdText.value = ''
  typeFilter.value = ''
  readFilter.value = undefined
  deletedFilter.value = undefined
  dateRange.value = null
  page.value = 1
  fetchData()
}

const handleDelete = async (row: AdminNotification) => {
  try {
    await ElMessageBox.confirm(
      `确定移除这条用户消息吗？\n${row.title || ''}`,
      '移除用户消息',
      { type: 'warning', confirmButtonText: '移除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  try {
    await deleteNotification(row.id)
    ElMessage.success('消息已移除')
    await fetchData()
  } catch {
    // 统一请求拦截器负责提示错误
  }
}

const handleRestore = async (row: AdminNotification) => {
  try {
    await ElMessageBox.confirm('确定恢复这条用户消息吗？', '恢复用户消息', {
      type: 'warning',
      confirmButtonText: '恢复',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    await restoreNotification(row.id)
    ElMessage.success('消息已恢复')
    await fetchData()
  } catch {
    // 统一请求拦截器负责提示错误
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.notification-management {
  padding: 20px;
}

.search-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.message-content strong,
.message-content span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sub-text,
.target-title {
  display: block;
  color: #909399;
  font-size: 12px;
}

.target-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 3px;
}
</style>
