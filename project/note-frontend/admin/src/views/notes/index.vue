<template>
  <div class="note-management">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索笔记标题/用户名" clearable @keyup.enter="handleSearch" style="width: 230px;" />
      <el-select v-model="noteTypeFilter" placeholder="笔记类型" clearable style="width: 130px;" @change="handleSearch">
        <el-option label="外链笔记" :value="1" />
        <el-option label="原创创作" :value="2" />
      </el-select>
      <el-select v-model="isPublicFilter" placeholder="公开状态" clearable style="width: 120px;" @change="handleSearch">
        <el-option label="公开" :value="1" />
        <el-option label="私密" :value="0" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px;" @change="handleSearch">
        <el-option label="正常" :value="0" />
        <el-option label="已删除" :value="1" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button type="success" @click="showCreateDialog">插入笔记</el-button>
      <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除</el-button>
      <el-button type="warning" :disabled="!selectedIds.length" @click="handleBatchRestore">批量恢复</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" @selection-change="handleSelectionChange" stripe>
      <el-table-column type="selection" width="50" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.noteType === 2 ? 'success' : 'info'" size="small">
            {{ row.noteType === 2 ? '原创' : '外链' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="可见性" width="85">
        <template #default="{ row }">
          <el-tag :type="row.isPublic === 0 ? 'warning' : 'info'" size="small">
            {{ row.isPublic === 0 ? '私密' : '公开' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="180">
        <template #default="{ row }">
          <el-tag v-if="row.isPinned === 1" type="warning" size="small" style="margin-right: 6px;">置顶</el-tag>
          <span class="note-title-cell" @click="handleViewDetail(row)">{{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="所属用户" width="120" />
      <el-table-column label="内容摘要 / 链接" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.noteType === 2" class="summary-text">{{ row.summary || '（暂无摘要）' }}</span>
          <a v-else-if="isUrl(row.locationOrUrl)" :href="row.locationOrUrl" target="_blank" rel="noopener">{{ row.locationOrUrl }}</a>
          <span v-else>{{ row.locationOrUrl }}</span>
        </template>
      </el-table-column>
      <el-table-column label="标签" width="160">
        <template #default="{ row }">
          <div v-if="row.tags && row.tags.length" style="display: flex; flex-wrap: wrap; gap: 4px;">
            <el-tag v-for="t in row.tags" :key="t" size="small" type="info">{{ t }}</el-tag>
          </div>
          <span v-else style="color: #999; font-size: 12px;">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览量" width="80" />
      <el-table-column label="收藏" width="70">
        <template #default="{ row }">
          <span>{{ row.favoriteCount ?? '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.isDeleted === 1 ? 'danger' : 'success'">{{ row.isDeleted === 1 ? '已删除' : '正常' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="发布时间" width="170" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.isDeleted === 0" link type="danger" @click="handleDelete(row)">删除</el-button>
          <el-button v-if="row.isDeleted === 1" link type="success" @click="handleRestore(row)">恢复</el-button>
          <el-button v-if="row.isDeleted === 0" link :type="row.isPinned === 1 ? 'warning' : 'primary'" @click="handleTogglePin(row)">
            {{ row.isPinned === 1 ? '取消置顶' : '置顶' }}
          </el-button>
          <el-button link type="primary" @click="handleEditTags(row)">修改标签</el-button>
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
    <el-dialog v-model="detailVisible" title="笔记详情与审查" :width="detailData.noteType === 2 ? '820px' : '620px'">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="标题" :span="2">
          <strong>{{ detailData.title }}</strong>
          <el-tag v-if="detailData.isPinned === 1" type="warning" size="small" style="margin-left: 8px;">已置顶</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属用户">{{ detailData.username }}</el-descriptions-item>
        <el-descriptions-item label="公开状态">
          <el-tag :type="detailData.isPublic === 0 ? 'warning' : 'info'" size="small">
            {{ detailData.isPublic === 0 ? '私密 (仅作者可见)' : '公开' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="笔记类型">
          <el-tag :type="detailData.noteType === 2 ? 'success' : 'info'" size="small">
            {{ detailData.noteType === 2 ? '原创创作 (Markdown)' : '外链笔记' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="浏览量">{{ detailData.viewCount }}</el-descriptions-item>
        <el-descriptions-item label="收藏数">{{ detailData.favoriteCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailData.isDeleted === 1 ? 'danger' : 'success'">
            {{ detailData.isDeleted === 1 ? '已删除' : '正常' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ detailData.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="标签" :span="2">
          <div v-if="detailData.tags && detailData.tags.length" style="display: flex; flex-wrap: wrap; gap: 6px;">
            <el-tag v-for="t in detailData.tags" :key="t" size="small">{{ t }}</el-tag>
          </div>
          <span v-else style="color: #999;">无标签</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.noteType === 1" label="链接/位置" :span="2">
          <a v-if="isUrl(detailData.locationOrUrl)" :href="detailData.locationOrUrl" target="_blank" rel="noopener">{{ detailData.locationOrUrl }}</a>
          <span v-else>{{ detailData.locationOrUrl }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.noteType === 2 && detailData.summary" label="摘要" :span="2">
          {{ detailData.summary }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 原创笔记 Markdown 预览 -->
      <div v-if="detailData.noteType === 2" class="markdown-preview-box">
        <h4 class="preview-title">Markdown 内容预览：</h4>
        <div class="preview-content">
          <MdPreview editorId="admin-note-detail-preview" :modelValue="detailData.content || '（暂无正文内容）'" />
        </div>
      </div>
    </el-dialog>

    <!-- 新增笔记对话框 -->
    <el-dialog v-model="createVisible" title="插入笔记" width="600px" @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="所属用户" prop="userId">
          <el-select v-model="createForm.userId" filterable remote reserve-keyword
            placeholder="输入用户名搜索" :remote-method="searchUsers" :loading="userSearching"
            style="width: 100%;">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="笔记类型" prop="noteType">
          <el-radio-group v-model="createForm.noteType">
            <el-radio :value="1">外链笔记</el-radio>
            <el-radio :value="2">原创创作笔记</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="公开状态" prop="isPublic">
          <el-radio-group v-model="createForm.isPublic">
            <el-radio :value="1">公开</el-radio>
            <el-radio :value="0">私密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="笔记标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入笔记标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item v-if="createForm.noteType === 1" label="链接/位置" prop="locationOrUrl">
          <el-input v-model="createForm.locationOrUrl" placeholder="请输入链接或位置" />
        </el-form-item>
        <template v-if="createForm.noteType === 2">
          <el-form-item label="摘要" prop="summary">
            <el-input v-model="createForm.summary" type="textarea" :rows="2" placeholder="可选，留空将从正文自动提取" />
          </el-form-item>
          <el-form-item label="正文内容" prop="content">
            <el-input v-model="createForm.content" type="textarea" :rows="8" placeholder="请输入 Markdown 格式的笔记正文" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改标签对话框 -->
    <el-dialog v-model="tagsDialogVisible" title="修改笔记标签" width="500px">
      <div style="margin-bottom: 14px; color: #606266; font-size: 14px;">
        当前笔记：<strong>{{ currentEditNote?.title }}</strong>
      </div>
      <el-form label-width="80px">
        <el-form-item label="标签列表">
          <el-select
            v-model="editTagList"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入标签名称并回车添加"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="tagsSubmitLoading" @click="submitEditTags">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormRules } from 'element-plus'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import {
  getNoteList,
  getNoteDetail,
  createNote,
  deleteNote,
  restoreNote,
  batchDeleteNotes,
  batchRestoreNotes,
  getUserList,
  pinNote,
  unpinNote,
  getNoteFavoriteCount,
  updateNoteTags,
} from '@/api/admin'

const loading = ref(false)
const keyword = ref('')
const noteTypeFilter = ref<number | undefined>(undefined)
const isPublicFilter = ref<number | undefined>(undefined)
const statusFilter = ref<number | undefined>(undefined)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<any[]>([])
const selectedIds = ref<number[]>([])

const detailVisible = ref(false)
const detailData = ref<any>({})

const tagsDialogVisible = ref(false)
const tagsSubmitLoading = ref(false)
const currentEditNote = ref<any>(null)
const editTagList = ref<string[]>([])

const handleEditTags = (row: any) => {
  currentEditNote.value = row
  editTagList.value = row.tags ? [...row.tags] : []
  tagsDialogVisible.value = true
}

const submitEditTags = async () => {
  if (!currentEditNote.value) return
  tagsSubmitLoading.value = true
  try {
    await updateNoteTags(currentEditNote.value.id, editTagList.value)
    ElMessage.success('标签更新成功')
    currentEditNote.value.tags = [...editTagList.value]
    tagsDialogVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e.message || '更新标签失败')
  } finally {
    tagsSubmitLoading.value = false
  }
}

const isUrl = (text?: string) => {
  if (!text) return false
  return /^https?:\/\//i.test(text.trim())
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getNoteList({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      noteType: noteTypeFilter.value,
      isPublic: isPublicFilter.value,
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
  selectedIds.value = rows.map((r) => r.id)
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
    } catch {
      detailData.value.favoriteCount = 0
    }
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
  noteType: 1,
  isPublic: 1,
  title: '',
  locationOrUrl: '',
  summary: '',
  content: '',
})

const createRules = computed<FormRules>(() => ({
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  title: [{ required: true, message: '请输入笔记标题', trigger: 'blur' }],
  locationOrUrl: createForm.value.noteType === 1 ? [{ required: true, message: '请输入链接/位置', trigger: 'blur' }] : [],
  content: createForm.value.noteType === 2 ? [{ required: true, message: '请输入笔记正文内容', trigger: 'blur' }] : [],
}))

const showCreateDialog = () => {
  createVisible.value = true
}

const resetCreateForm = () => {
  createForm.value = {
    userId: undefined,
    noteType: 1,
    isPublic: 1,
    title: '',
    locationOrUrl: '',
    summary: '',
    content: '',
  }
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
      noteType: createForm.value.noteType,
      isPublic: createForm.value.isPublic,
      locationOrUrl: createForm.value.noteType === 1 ? createForm.value.locationOrUrl : undefined,
      content: createForm.value.noteType === 2 ? createForm.value.content : undefined,
      summary: createForm.value.noteType === 2 ? createForm.value.summary : undefined,
    })
    ElMessage.success('插入成功')
    createVisible.value = false
    fetchData()
  } catch (e) { /* handled */ }
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
.note-title-cell {
  cursor: pointer;
  color: #409eff;
}
.note-title-cell:hover {
  text-decoration: underline;
}
.summary-text {
  color: #606266;
  font-size: 13px;
}
.markdown-preview-box {
  margin-top: 16px;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}
.preview-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}
.preview-content {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  max-height: 380px;
  overflow-y: auto;
  background: #fafafa;
  padding: 8px;
}
</style>
