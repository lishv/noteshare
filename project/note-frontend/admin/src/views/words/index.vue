<template>
  <div class="word-management">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索敏感词" clearable @keyup.enter="handleSearch" style="width: 200px; margin-right: 10px;" />
      <el-select v-model="typeFilter" placeholder="分类" style="width: 150px; margin-right: 10px;">
        <el-option label="全部" :value="undefined" />
        <el-option label="政治" :value="1" />
        <el-option label="色情" :value="2" />
        <el-option label="暴力" :value="3" />
        <el-option label="广告" :value="4" />
        <el-option label="其他" :value="5" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="状态" style="width: 120px; margin-right: 10px;">
        <el-option label="全部" :value="undefined" />
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button type="success" @click="showCreateDialog">新增</el-button>
      <el-button type="warning" @click="showBatchImportDialog">批量导入</el-button>
      <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedIds.length">批量删除</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" @selection-change="handleSelectionChange" stripe>
      <el-table-column type="selection" width="55" />
      <el-table-column prop="word" label="敏感词" min-width="150" />
      <el-table-column prop="type" label="分类" width="120">
        <template #default="{ row }">
          {{ getTypeName(row.type) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="primary" @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
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

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="wordDialogVisible" :title="isEdit ? '编辑敏感词' : '新增敏感词'" width="500px">
      <el-form :model="wordForm" label-width="80px">
        <el-form-item label="敏感词">
          <el-input v-model="wordForm.word" placeholder="请输入敏感词" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="wordForm.type" placeholder="请选择分类" style="width: 100%;">
            <el-option label="政治" :value="1" />
            <el-option label="色情" :value="2" />
            <el-option label="暴力" :value="3" />
            <el-option label="广告" :value="4" />
            <el-option label="其他" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="wordForm.status" style="width: 100%;">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="wordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitWordForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- Batch Import Dialog -->
    <el-dialog v-model="batchImportDialogVisible" title="批量导入" width="500px">
      <el-form :model="batchImportForm" label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="batchImportForm.type" style="width: 100%;">
            <el-option label="政治" :value="1" />
            <el-option label="色情" :value="2" />
            <el-option label="暴力" :value="3" />
            <el-option label="广告" :value="4" />
            <el-option label="其他" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="敏感词">
          <el-input
            v-model="batchImportText"
            type="textarea"
            :rows="8"
            placeholder="每行一个敏感词"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchImportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getWordList, createWord, updateWord, deleteWord, toggleWordStatus,
  batchImportWords, batchDeleteWords,
} from '@/api/admin'

const loading = ref(false)
const keyword = ref('')
const typeFilter = ref<number | undefined>(undefined)
const statusFilter = ref<number | undefined>(undefined)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<any[]>([])
const selectedIds = ref<number[]>([])

const wordDialogVisible = ref(false)
const isEdit = ref(false)
const wordForm = reactive<{ word: string; type: number; status: number }>({ word: '', type: 1, status: 1 })
let editingId = 0

const batchImportDialogVisible = ref(false)
const batchImportText = ref('')
const batchImportForm = reactive({ type: 5 })

const typeMap: Record<number, string> = { 1: '政治', 2: '色情', 3: '暴力', 4: '广告', 5: '其他' }
const getTypeName = (t: number) => typeMap[t] || '未知'

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getWordList({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      type: typeFilter.value,
      status: statusFilter.value,
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

const showCreateDialog = () => {
  isEdit.value = false
  editingId = 0
  wordForm.word = ''
  wordForm.type = 1
  wordForm.status = 1
  wordDialogVisible.value = true
}

const showEditDialog = (row: any) => {
  isEdit.value = true
  editingId = row.id
  wordForm.word = row.word
  wordForm.type = row.type
  wordForm.status = row.status
  wordDialogVisible.value = true
}

const submitWordForm = async () => {
  try {
    if (isEdit.value) {
      await updateWord(editingId, wordForm)
      ElMessage.success('编辑成功')
    } else {
      await createWord(wordForm)
      ElMessage.success('创建成功')
    }
    wordDialogVisible.value = false
    fetchData()
  } catch (e) {
    // handled
  }
}

const handleToggleStatus = async (row: any) => {
  await ElMessageBox.confirm(`确定${row.status === 1 ? '禁用' : '启用'}该敏感词吗？`, '提示', { type: 'warning' })
  try {
    await toggleWordStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    fetchData()
  } catch (e) {
    // handled
  }
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确定删除该敏感词吗？', '提示', { type: 'warning' })
  try {
    await deleteWord(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    // handled
  }
}

const handleBatchDelete = async () => {
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条敏感词吗？`, '提示', { type: 'warning' })
  try {
    await batchDeleteWords({ ids: selectedIds.value })
    ElMessage.success('批量删除成功')
    fetchData()
  } catch (e) {
    // handled
  }
}

const showBatchImportDialog = () => {
  batchImportText.value = ''
  batchImportForm.type = 5
  batchImportDialogVisible.value = true
}

const submitBatchImport = async () => {
  const words = batchImportText.value.split('\n').map(w => w.trim()).filter(Boolean)
  if (!words.length) {
    ElMessage.warning('请输入敏感词')
    return
  }
  try {
    await batchImportWords({ words, type: batchImportForm.type })
    ElMessage.success('导入成功')
    batchImportDialogVisible.value = false
    fetchData()
  } catch (e) {
    // handled
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.word-management {
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
