<template>
  <div class="note-management">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索笔记标题/用户名" clearable @keyup.enter="handleSearch" style="width: 250px; margin-right: 10px;" />
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 150px; margin-right: 10px;" @change="handleSearch">
        <el-option label="正常" :value="0" />
        <el-option label="已删除" :value="1" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button type="success" @click="showCreateDialog">插入笔记</el-button>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除</el-button>
      <el-button type="warning" :disabled="!selectedIds.length" @click="handleBatchRestore">批量恢复</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" @selection-change="handleSelectionChange" stripe>
      <el-table-column type="selection" width="55" />
      <el-table-column prop="title" label="标题" min-width="180">
        <template #default="{ row }">
          <el-tag v-if="row.isPinned === 1" type="warning" size="small" style="margin-right: 6px;">置顶</el-tag>
          {{ row.title }}
        </template>
      </el-table-column>
      <el-table-column prop="username" label="所属用户" width="120" />
      <el-table-column prop="locationOrUrl" label="链接/位置" min-width="200" show-overflow-tooltip />
      <el-table-column prop="viewCount" label="浏览量" width="80" />
      <el-table-column label="收藏" width="70">
        <template #default="{ row }">
          <span>{{ row.favoriteCount ?? '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isDeleted === 1 ? 'danger' : 'success'">{{ row.isDeleted === 1 ? '已删除' : '正常' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="发布时间" width="180" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.isDeleted === 0" link type="danger" @click="handleDelete(row)">删除</el-button>
          <el-button v-if="row.isDeleted === 1" link type="success" @click="handleRestore(row)">恢复</el-button>
          <el-button v-if="row.isDeleted === 0" link :type="row.isPinned === 1 ? 'warning' : 'primary'" @click="handleTogglePin(row)">
            {{ row.isPinned === 1 ? '取消置顶' : '置顶' }}
          </el-button>
          <el-button link type="primary" @click="handleViewDetail(row)">查看</el-button>
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

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="笔记详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ detailData.title }}</el-descriptions-item>
        <el-descriptions-item label="所属用户">{{ detailData.username }}</el-descriptions-item>
        <el-descriptions-item label="链接/位置">{{ detailData.locationOrUrl }}</el-descriptions-item>
        <el-descriptions-item label="浏览量">{{ detailData.viewCount }}</el-descriptions-item>
        <el-descriptions-item label="收藏数">{{ detailData.favoriteCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="置顶">{{ detailData.isPinned === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailData.isDeleted === 1 ? '已删除' : '正常' }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ detailData.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailData.updatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 新增笔记对话框 -->
    <el-dialog v-model="createVisible" title="插入笔记" width="500px" @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="所属用户" prop="userId">
          <el-select v-model="createForm.userId" filterable remote reserve-keyword
            placeholder="输入用户名搜索" :remote-method="searchUsers" :loading="userSearching"
            style="width: 100%;">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="笔记标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入笔记标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="链接/位置" prop="locationOrUrl">
          <el-input v-model="createForm.locationOrUrl" placeholder="请输入链接或位置" />
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
import { getNoteList, getNoteDetail, createNote, deleteNote, restoreNote, batchDeleteNotes, batchRestoreNotes, getUserList, pinNote, unpinNote, getNoteFavoriteCount } from '@/api/admin'

const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<any[]>([])
const selectedIds = ref<number[]>([])

const detailVisible = ref(false)
const detailData = ref<any>({})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getNoteList({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      isDeleted: statusFilter.value,
    })
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

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(`确定删除笔记 "${row.title}" 吗？`, '提示', { type: 'warning' })
  try {
    await deleteNote(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* handled */ }
}

const handleRestore = async (row: any) => {
  await ElMessageBox.confirm(`确定恢复笔记 "${row.title}" 吗？`, '提示', { type: 'warning' })
  try {
    await restoreNote(row.id)
    ElMessage.success('恢复成功')
    fetchData()
  } catch (e) { /* handled */ }
}

const handleTogglePin = async (row: any) => {
  try {
    if (row.isPinned === 1) {
      await unpinNote(row.id)
      ElMessage.success('已取消置顶')
    } else {
      await pinNote(row.id)
      ElMessage.success('已置顶')
    }
    fetchData()
  } catch (e) { /* handled */ }
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selectedIds.value.length} 篇笔记吗？`, '提示', { type: 'warning' })
  try {
    await batchDeleteNotes({ ids: selectedIds.value })
    ElMessage.success('批量删除成功')
    fetchData()
  } catch (e) { /* handled */ }
}

const handleBatchRestore = async () => {
  await ElMessageBox.confirm(`确定批量恢复选中的 ${selectedIds.value.length} 篇笔记吗？`, '提示', { type: 'warning' })
  try {
    await batchRestoreNotes({ ids: selectedIds.value })
    ElMessage.success('批量恢复成功')
    fetchData()
  } catch (e) { /* handled */ }
}

const handleViewDetail = async (row: any) => {
  try {
    const res = await getNoteDetail(row.id)
    detailData.value = res.data
    try {
      const favRes = await getNoteFavoriteCount(row.id)
      detailData.value.favoriteCount = favRes.data
    } catch { detailData.value.favoriteCount = 0 }
    detailVisible.value = true
  } catch (e) { /* handled */ }
}

// 新增笔记
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref()
const userSearching = ref(false)
const userOptions = ref<{ id: number; username: string }[]>([])
const createForm = ref({
  userId: undefined as number | undefined,
  title: '',
  locationOrUrl: '',
})
const createRules = {
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  title: [{ required: true, message: '请输入笔记标题', trigger: 'blur' }],
  locationOrUrl: [{ required: true, message: '请输入链接/位置', trigger: 'blur' }],
}

const showCreateDialog = () => {
  createVisible.value = true
}

const resetCreateForm = () => {
  createForm.value = { userId: undefined, title: '', locationOrUrl: '' }
  userOptions.value = []
}

const searchUsers = async (query: string) => {
  if (!query) return
  userSearching.value = true
  try {
    const res = await getUserList({ keyword: query, size: 20 })
    userOptions.value = res.data.list.map((u: any) => ({ id: u.id, username: u.username }))
  } catch { /* handled */ }
  finally { userSearching.value = false }
}

const handleCreate = async () => {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    await createNote(createForm.value.userId!, {
      title: createForm.value.title,
      locationOrUrl: createForm.value.locationOrUrl,
    })
    ElMessage.success('插入成功')
    createVisible.value = false
    fetchData()
  } catch { /* handled */ }
  finally { createLoading.value = false }
}

onMounted(() => fetchData())
</script>

<style scoped>
.note-management {
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
