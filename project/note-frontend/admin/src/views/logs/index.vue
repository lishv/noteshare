<template>
  <div class="log-container">
    <div class="search-bar">
      <el-select v-model="operationType" placeholder="操作类型" clearable style="width: 180px; margin-right: 10px;">
        <el-option label="登录" value="LOGIN" />
        <el-option label="用户管理" value="USER_MANAGE" />
        <el-option label="笔记管理" value="NOTE_MANAGE" />
        <el-option label="评论管理" value="COMMENT_MANAGE" />
        <el-option label="消息管理" value="NOTIFICATION_MANAGE" />
        <el-option label="链接管理" value="LINK_MANAGE" />
        <el-option label="敏感词管理" value="WORD_MANAGE" />
        <el-option label="管理员管理" value="ADMIN_MANAGE" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="margin-right: 10px;"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="adminUsername" label="操作人" width="120" />
      <el-table-column label="操作类型" width="120">
        <template #default="{ row }">
          <el-tag :type="typeTagMap[row.operationType] || 'info'" size="small">{{ typeNameMap[row.operationType] || row.operationType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="操作描述" min-width="200" />
      <el-table-column label="操作对象" width="180">
        <template #default="{ row }">
          <span v-if="row.targetType">{{ typeNameMap[row.targetType] || row.targetType }} #{{ row.targetId }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="ipAddress" label="IP地址" width="140" />
      <el-table-column prop="createdAt" label="操作时间" width="180" />
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
import { ref, onMounted } from 'vue'
import { getLogList } from '@/api/admin'

const loading = ref(false)
const operationType = ref('')
const dateRange = ref<[string, string] | null>(null)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<any[]>([])

const typeNameMap: Record<string, string> = {
  LOGIN: '登录',
  USER_MANAGE: '用户管理',
  NOTE_MANAGE: '笔记管理',
  COMMENT_MANAGE: '评论管理',
  NOTIFICATION_MANAGE: '消息管理',
  LINK_MANAGE: '链接管理',
  WORD_MANAGE: '敏感词管理',
  ADMIN_MANAGE: '管理员管理',
  USER: '用户',
  NOTE: '笔记',
  COMMENT: '评论',
  NOTIFICATION: '消息',
  MAIN_LINK: '总链接',
  SENSITIVE_WORD: '敏感词',
  ADMIN: '管理员',
}

const typeTagMap: Record<string, string> = {
  LOGIN: '',
  USER_MANAGE: 'success',
  NOTE_MANAGE: 'warning',
  COMMENT_MANAGE: 'primary',
  NOTIFICATION_MANAGE: 'info',
  LINK_MANAGE: 'info',
  WORD_MANAGE: 'danger',
  ADMIN_MANAGE: 'danger',
}

const fetchData = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (operationType.value) params.operationType = operationType.value
    if (dateRange.value) {
      params.startTime = dateRange.value[0] + ' 00:00:00'
      params.endTime = dateRange.value[1] + ' 23:59:59'
    }
    const res = await getLogList(params)
    tableData.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

onMounted(() => fetchData())
</script>

<style scoped>
.log-container {
  padding: 20px;
}
.search-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
</style>
