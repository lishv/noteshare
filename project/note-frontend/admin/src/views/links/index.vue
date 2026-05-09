<template>
  <div class="link-management">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索链接" clearable @keyup.enter="handleSearch" style="width: 300px; margin-right: 10px;" />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="username" label="所属用户" width="120" />
      <el-table-column prop="mainUrl" label="链接URL" min-width="250" />
      <el-table-column prop="viewCount" label="浏览量" width="100" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="editDialogVisible" title="编辑链接" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="链接URL">
          <el-input v-model="editForm.mainUrl" placeholder="请输入链接URL" />
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
import { getLinkList, editLink, deleteLink } from '@/api/admin'

const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const tableData = ref<any[]>([])
const editDialogVisible = ref(false)
const editForm = reactive<{ id: number; mainUrl: string }>({ id: 0, mainUrl: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLinkList({ page: page.value, size: size.value, keyword: keyword.value || undefined })
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

const handleEdit = (row: any) => {
  editForm.id = row.id
  editForm.mainUrl = row.mainUrl
  editDialogVisible.value = true
}

const submitEdit = async () => {
  try {
    await editLink(editForm.id, { mainUrl: editForm.mainUrl })
    ElMessage.success('编辑成功')
    editDialogVisible.value = false
    fetchData()
  } catch (e) {
    // handled
  }
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确定删除该链接吗？', '提示', { type: 'warning' })
  try {
    await deleteLink(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    // handled
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.link-management {
  padding: 20px;
}
.search-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
</style>
