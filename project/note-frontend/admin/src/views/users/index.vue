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
      <el-button type="success" @click="showCreateDialog">插入用户</el-button>
      <el-button @click="handleBatchDisable" :disabled="!selectedIds.length" type="warning">批量禁用</el-button>
      <el-button @click="handleBatchEnable" :disabled="!selectedIds.length" type="success">批量启用</el-button>
      <el-button @click="handleBatchDelete" :disabled="!selectedIds.length" type="danger">批量删除</el-button>
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
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button v-if="row.status === 2" link type="warning" @click="handleUnlock(row)">解锁</el-button>
          <el-button link type="primary" @click="handleResetPassword(row)">重置密码</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 插入用户对话框 -->
    <el-dialog v-model="createVisible" title="插入用户" width="500px" @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入用户名" maxlength="50" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="createForm.phone" placeholder="请输入手机号（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, toggleUserStatus, unlockUser, resetUserPassword, batchToggleUserStatus, batchDeleteUsers, createUser, deleteUser } from '@/api/admin'

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

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(`确定删除用户 "${row.username}" 吗？该用户的所有笔记、链接、收藏将被一并删除！`, '警告', { type: 'error' })
  try {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
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

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selectedIds.value.length} 个用户吗？所有笔记、链接、收藏将被一并删除！`, '警告', { type: 'error' })
  try {
    await batchDeleteUsers({ ids: selectedIds.value })
    ElMessage.success('批量删除成功')
    fetchData()
  } catch (e) { /* handled */ }
}

// 插入用户
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref()
const createForm = ref({
  username: '',
  password: '',
  email: '',
  phone: '',
})
const createRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email' as const, message: '邮箱格式不正确', trigger: 'blur' }],
}

const showCreateDialog = () => {
  createVisible.value = true
}

const resetCreateForm = () => {
  createForm.value = { username: '', password: '', email: '', phone: '' }
}

const handleCreate = async () => {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    await createUser(createForm.value)
    ElMessage.success('创建成功')
    createVisible.value = false
    fetchData()
  } catch { /* handled */ }
  finally { createLoading.value = false }
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
  flex-wrap: wrap;
  gap: 10px;
}
</style>
