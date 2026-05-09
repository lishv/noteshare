<template>
  <div class="user-management">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索用户名/邮箱" clearable @keyup.enter="handleSearch" style="width: 250px; margin-right: 10px;" />
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 150px; margin-right: 10px;" @change="handleSearch">
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
        <el-option label="锁定" :value="2" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleBatchDisable" :disabled="!selectedIds.length" type="danger">批量禁用</el-button>
      <el-button @click="handleBatchEnable" :disabled="!selectedIds.length" type="success">批量启用</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" @selection-change="handleSelectionChange" stripe>
      <el-table-column type="selection" width="55" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="success">正常</el-tag>
          <el-tag v-else-if="row.status === 0" type="danger">禁用</el-tag>
          <el-tag v-else-if="row.status === 2" type="warning">锁定</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="noteCount" label="笔记数量" width="100" />
      <el-table-column prop="lastActiveAt" label="最后活跃时间" width="180" />
      <el-table-column prop="createdAt" label="注册时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button v-if="row.status === 2" link type="warning" @click="handleUnlock(row)">解锁</el-button>
          <el-button link type="primary" @click="handleResetPassword(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="fetchData"
      @current-change="fetchData"
      style="margin-top: 20px; justify-content: flex-end;"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, toggleUserStatus, unlockUser, resetUserPassword, batchToggleUserStatus } from '@/api/admin'

const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<any[]>([])
const selectedIds = ref<number[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUserList({ page: page.value, size: size.value, keyword: keyword.value || undefined, status: statusFilter.value })
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

const handleSelectionChange = (rows: any[]) => {
  selectedIds.value = rows.map(r => r.id)
}

const handleToggleStatus = async (row: any) => {
  const action = row.status === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定${action}用户 ${row.username} 吗？`, '提示', { type: 'warning' })
  try {
    await toggleUserStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(`${action}成功`)
    fetchData()
  } catch (e) {
    // handled
  }
}

const handleUnlock = async (row: any) => {
  await ElMessageBox.confirm(`确定解锁用户 ${row.username} 吗？`, '提示', { type: 'warning' })
  try {
    await unlockUser(row.id)
    ElMessage.success('解锁成功')
    fetchData()
  } catch (e) {
    // handled
  }
}

const handleResetPassword = async (row: any) => {
  await ElMessageBox.confirm(`确定重置用户 ${row.username} 的密码吗？`, '提示', { type: 'warning' })
  try {
    await resetUserPassword(row.id)
    ElMessage.success('重置密码成功')
    fetchData()
  } catch (e) {
    // handled
  }
}

const handleBatchDisable = async () => {
  await ElMessageBox.confirm(`确定批量禁用选中的 ${selectedIds.value.length} 个用户吗？`, '提示', { type: 'warning' })
  try {
    await batchToggleUserStatus({ ids: selectedIds.value, status: 0 })
    ElMessage.success('批量禁用成功')
    fetchData()
  } catch (e) { /* handled */ }
}

const handleBatchEnable = async () => {
  await ElMessageBox.confirm(`确定批量启用选中的 ${selectedIds.value.length} 个用户吗？`, '提示', { type: 'warning' })
  try {
    await batchToggleUserStatus({ ids: selectedIds.value, status: 1 })
    ElMessage.success('批量启用成功')
    fetchData()
  } catch (e) { /* handled */ }
}

onMounted(() => fetchData())
</script>

<style scoped>
.user-management {
  padding: 20px;
}
.search-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
</style>
