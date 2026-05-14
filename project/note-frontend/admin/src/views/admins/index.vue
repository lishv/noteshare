<template>
  <div class="admin-management">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索管理员" clearable @keyup.enter="handleSearch" style="width: 300px; margin-right: 10px;" />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button type="success" @click="showCreateDialog">新增管理员</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="row.role === 1 ? 'danger' : 'info'">{{ row.role === 1 ? '超级管理员' : '普通管理员' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLoginAt" label="最后登录" width="180" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <template v-if="row.username === 'admin'">
            <el-tag type="info" size="small">默认超管</el-tag>
          </template>
          <template v-else>
            <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
            <el-button link type="primary" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="warning" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
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

    <!-- Create Dialog -->
    <el-dialog v-model="createDialogVisible" title="新增管理员" width="500px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="createForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="createForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.role" style="width: 100%;">
            <el-option label="超级管理员" :value="1" />
            <el-option label="普通管理员" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- Edit Dialog -->
    <el-dialog v-model="editDialogVisible" title="编辑管理员" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width: 100%;" :disabled="editingAdminIsAdmin">
            <el-option label="超级管理员" :value="1" />
            <el-option label="普通管理员" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminList, createAdmin, editAdmin, toggleAdminStatus, resetAdminPassword, deleteAdmin } from '@/api/admin'

const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<any[]>([])

const createDialogVisible = ref(false)
const createForm = reactive<{ username: string; password: string; email: string; phone: string; role: number }>({
  username: '', password: '', email: '', phone: '', role: 2,
})

const editDialogVisible = ref(false)
const editForm = reactive<{ email: string; phone: string; role: number }>({ email: '', phone: '', role: 2 })
let editingAdminId = 0
let editingAdminIsAdmin = false

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminList({ page: page.value, size: size.value, keyword: keyword.value || undefined })
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

const showCreateDialog = () => {
  createForm.username = ''
  createForm.password = ''
  createForm.email = ''
  createForm.phone = ''
  createForm.role = 2
  createDialogVisible.value = true
}

const submitCreate = async () => {
  try {
    await createAdmin(createForm)
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    fetchData()
  } catch (e) {
    // handled
  }
}

const showEditDialog = (row: any) => {
  editingAdminId = row.id
  editingAdminIsAdmin = row.username === 'admin'
  editForm.email = row.email
  editForm.phone = row.phone
  editForm.role = row.role
  editDialogVisible.value = true
}

const submitEdit = async () => {
  try {
    await editAdmin(editingAdminId, editForm)
    ElMessage.success('编辑成功')
    editDialogVisible.value = false
    fetchData()
  } catch (e) {
    // handled
  }
}

const handleToggleStatus = async (row: any) => {
  await ElMessageBox.confirm(`确定${row.status === 1 ? '禁用' : '启用'}管理员 ${row.username} 吗？`, '提示', { type: 'warning' })
  try {
    await toggleAdminStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    fetchData()
  } catch (e) {
    // handled
  }
}

const handleResetPassword = async (row: any) => {
  await ElMessageBox.confirm(`确定重置管理员 ${row.username} 的密码吗？`, '提示', { type: 'warning' })
  try {
    await resetAdminPassword(row.id)
    ElMessage.success('重置密码成功')
    fetchData()
  } catch (e) {
    // handled
  }
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(`确定删除管理员 "${row.username}" 吗？此操作不可恢复！`, '警告', { type: 'error' })
  try {
    await deleteAdmin(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    // handled
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.admin-management {
  padding: 20px;
}
.search-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
</style>
