<template>
  <div class="note-management">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索笔记标题/用户名" clearable @keyup.enter="handleSearch" style="width: 250px; margin-right: 10px;" />
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 150px; margin-right: 10px;" @change="handleSearch">
        <el-option label="正常" :value="0" />
        <el-option label="已删除" :value="1" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" @selection-change="handleSelectionChange" stripe>
      <el-table-column type="selection" width="55" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="username" label="所属用户" width="120" />
      <el-table-column prop="locationOrUrl" label="链接/位置" min-width="200" show-overflow-tooltip />
      <el-table-column prop="viewCount" label="浏览量" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isDeleted === 1 ? 'danger' : 'success'">{{ row.isDeleted === 1 ? '已删除' : '正常' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="发布时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.isDeleted === 0" link type="danger" @click="handleDelete(row)">删除</el-button>
          <el-button v-if="row.isDeleted === 1" link type="success" @click="handleRestore(row)">恢复</el-button>
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
        <el-descriptions-item label="状态">{{ detailData.isDeleted === 1 ? '已删除' : '正常' }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ detailData.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailData.updatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNoteList, getNoteDetail, deleteNote, restoreNote, batchDeleteNotes, batchRestoreNotes } from '@/api/admin'

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

const handleViewDetail = async (row: any) => {
  try {
    const res = await getNoteDetail(row.id)
    detailData.value = res.data
    detailVisible.value = true
  } catch (e) { /* handled */ }
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
}
</style>
