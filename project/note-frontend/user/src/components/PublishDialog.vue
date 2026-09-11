<template>
  <el-dialog
    v-model="dialogVisible"
    :title="editNote ? '编辑笔记' : '发布笔记'"
    width="560px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="90px"
    >
      <!-- 操作类型选择 -->
      <el-form-item label="操作类型">
        <el-radio-group v-model="form.type" @change="handleTypeChange">
          <el-radio value="mainLink">设置总链接</el-radio>
          <el-radio value="note">添加单篇链接笔记</el-radio>
          <el-radio value="article">在线创作笔记</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 总链接表单 -->
      <template v-if="form.type === 'mainLink'">
        <el-form-item label="总链接" prop="mainUrl">
          <el-input
            v-model="form.mainUrl"
            placeholder="请输入你的知识库主页链接，如 Notion/语雀/飞书等"
          />
          <div class="form-tip">每个用户只能有一个总链接，提交新链接将覆盖原有记录</div>
        </el-form-item>
      </template>

      <!-- 链接笔记表单 -->
      <template v-if="form.type === 'note'">
        <el-form-item label="笔记名称" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入笔记名称"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="链接/位置" prop="locationOrUrl">
          <el-input
            v-model="form.locationOrUrl"
            placeholder="输入完整URL或相对位置描述"
          />
          <div class="form-tip" v-if="linkTip">{{ linkTip }}</div>
          <el-alert
            v-if="!hasMainLink && form.locationOrUrl && !isUrl(form.locationOrUrl)"
            title="您还未设置总链接，建议先设置总链接后再录入相对位置笔记"
            type="warning"
            :closable="false"
            show-icon
            class="form-alert"
          />
        </el-form-item>
        <el-form-item label="公开状态">
          <el-radio-group v-model="form.isPublic">
            <el-radio :value="1">
              <span>公开</span>
              <span class="sub-label">（所有人及访客可见）</span>
            </el-radio>
            <el-radio :value="0">
              <span>私密</span>
              <span class="sub-label">（仅自己可见）</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </template>

      <!-- 在线创作笔记提示与入口 -->
      <template v-if="form.type === 'article'">
        <div class="article-creation-card">
          <div class="article-creation-icon">
            <el-icon :size="36" color="#409eff"><EditPen /></el-icon>
          </div>
          <div class="article-creation-content">
            <h4>沉浸式 Markdown 创作工作台</h4>
            <p>支持 Markdown 语法、代码块高亮、公式、表格排版以及实时粘贴上传图片，提供专注、高效的创作体验。</p>
            <el-button type="primary" size="default" :icon="EditPen" @click="goToEditor">
              进入独立编辑器创作
            </el-button>
          </div>
        </div>
      </template>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button v-if="form.type !== 'article'" type="primary" :loading="loading" @click="handleSubmit">
        {{ editNote ? '保存' : '发布' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { setMainLink, createNote, updateNote, getMyTags } from '@/api/note'
import type { Tag } from '@/api/note'
import { isUrl, autoCompleteUrl, containsSensitiveWords } from '@/utils/helpers'
import type { NoteItem } from '@/types'

const router = useRouter()

const props = defineProps<{
  visible: boolean
  editNote: NoteItem | null
  hasMainLink: boolean
  isOwner: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  type: 'note' as 'mainLink' | 'note' | 'article',
  title: '',
  locationOrUrl: '',
  mainUrl: '',
  isPublic: 1,
  tagIds: [] as number[],
})

function goToEditor() {
  dialogVisible.value = false
  if (props.editNote && props.editNote.noteType === 2) {
    router.push(`/note/editor/${props.editNote.id}`)
  } else {
    router.push('/note/editor')
  }
}

const myTags = ref<Tag[]>([])

async function loadTags() {
  try {
    const res = await getMyTags()
    myTags.value = res.data || []
  } catch { myTags.value = [] }
}

const linkTip = computed(() => {
  if (form.locationOrUrl && !isUrl(form.locationOrUrl)) {
    const autoUrl = autoCompleteUrl(form.locationOrUrl)
    if (autoUrl) {
      return `检测到域名格式，是否使用 ${autoUrl}？`
    }
    return '未检测到有效网址，将作为普通文本位置展示'
  }
  return ''
})

const rules: FormRules = {
  title: [
    { required: true, message: '请输入笔记名称', trigger: 'blur' },
    { max: 100, message: '笔记名称不能超过100个字符', trigger: 'blur' },
  ],
  locationOrUrl: [
    { required: true, message: '请输入链接或位置', trigger: 'blur' },
  ],
  mainUrl: [
    { required: true, message: '请输入总链接', trigger: 'blur' },
  ],
}

function handleTypeChange() {
  form.title = ''
  form.locationOrUrl = ''
  form.mainUrl = ''
  form.isPublic = 1
  form.tagIds = []
  formRef.value?.clearValidate()
}

function handleClose() {
  form.type = 'note'
  form.title = ''
  form.locationOrUrl = ''
  form.mainUrl = ''
  form.isPublic = 1
  form.tagIds = []
  formRef.value?.resetFields()
}

// 编辑时回填
watch(
  () => props.visible,
  (val) => {
    if (val) loadTags()
  }
)

watch(
  () => props.editNote,
  (note) => {
    if (note) {
      form.type = 'note'
      form.title = note.title
      form.locationOrUrl = note.locationOrUrl || ''
      form.isPublic = note.isPublic !== undefined ? note.isPublic : 1
    }
  },
  { immediate: true }
)

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // 敏感词校验
  if (form.type === 'note') {
    if (containsSensitiveWords(form.title) || containsSensitiveWords(form.locationOrUrl)) {
      ElMessage.error('内容包含敏感词汇，请修改后重试')
      return
    }
  } else if (form.type === 'mainLink') {
    if (containsSensitiveWords(form.mainUrl)) {
      ElMessage.error('内容包含敏感词汇，请修改后重试')
      return
    }
  }

  loading.value = true
  try {
    if (form.type === 'mainLink') {
      await setMainLink({ mainUrl: form.mainUrl })
      ElMessage.success('总链接设置成功')
    } else {
      if (props.editNote) {
        await updateNote(props.editNote.id, {
          title: form.title,
          locationOrUrl: form.locationOrUrl,
          isPublic: form.isPublic,
          tagIds: form.tagIds,
        })
        ElMessage.success('笔记更新成功')
      } else {
        await createNote({
          title: form.title,
          locationOrUrl: form.locationOrUrl,
          isPublic: form.isPublic,
          tagIds: form.tagIds,
        })
        ElMessage.success('笔记发布成功')
      }
    }
    dialogVisible.value = false
    emit('success')
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.article-creation-card {
  display: flex;
  gap: 16px;
  background: #f0f7ff;
  border: 1px solid #d0e5ff;
  border-radius: 8px;
  padding: 20px;
  margin-top: 8px;
  align-items: flex-start;
}

.article-creation-icon {
  margin-top: 2px;
}

.article-creation-content h4 {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
  margin: 0 0 6px 0;
}

.article-creation-content p {
  font-size: 13px;
  color: #4e5969;
  line-height: 1.6;
  margin: 0 0 16px 0;
}

.form-tip {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
  line-height: 1.5;
}

.sub-label {
  font-size: 12px;
  color: #909399;
  margin-left: 2px;
}

.form-alert {
  margin-top: 8px;
}

:deep(.el-dialog) {
  border-radius: var(--radius-lg);
}

:deep(.el-dialog__header) {
  padding: var(--space-lg) var(--space-xl);
  border-bottom: 1px solid var(--color-border);
  margin-right: 0;
}

:deep(.el-dialog__title) {
  font-weight: 600;
  color: var(--color-text-primary);
}

:deep(.el-dialog__body) {
  padding: var(--space-xl);
}

:deep(.el-dialog__footer) {
  padding: var(--space-md) var(--space-xl) var(--space-lg);
  border-top: 1px solid var(--color-border);
}
</style>
