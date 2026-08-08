<template>
  <div class="editor-page" :class="{ 'is-focus-mode': focusMode }">
    <header class="editor-header">
      <div class="editor-title-group">
        <span class="editor-kicker">{{ isNew ? 'NEW KNOWLEDGE NOTE' : 'KNOWLEDGE NOTE' }}</span>
        <el-input v-model="form.title" class="title-input" placeholder="为这条知识命名" />
        <span class="save-status">{{ saving ? '正在保存…' : '更改会在保存后同步至知识库' }}</span>
      </div>
      <div class="editor-header-actions">
        <el-button text @click="focusMode = !focusMode">{{ focusMode ? '退出专注' : '专注模式' }}</el-button>
        <el-button v-if="!isNew" @click="openVersions">历史版本</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存笔记</el-button>
      </div>
    </header>

    <div v-show="!focusMode" class="toolbar">
      <el-select v-model="form.categoryId" placeholder="分类" clearable style="width: 140px">
        <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
      </el-select>
      <el-select v-model="form.difficultyLevel" placeholder="难度" clearable style="width: 120px">
        <el-option label="入门" value="beginner" />
        <el-option label="中级" value="intermediate" />
        <el-option label="高级" value="advanced" />
      </el-select>
      <el-select v-model="form.tagIds" placeholder="标签" multiple style="width: 200px">
        <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
      </el-select>
      <div class="toolbar-spacer" />
      <el-button :loading="aiLoading" @click="handleSummarize">
        <el-icon><MagicStick /></el-icon>AI 摘要
      </el-button>
      <el-button :loading="aiLoading" @click="handleAssess">AI 评估难度</el-button>
    </div>

    <section v-if="summaryVisible && !focusMode" class="ai-summary-panel">
      <div class="summary-heading">
        <div>
          <span class="summary-kicker">AI SUMMARY</span>
          <h3>笔记摘要</h3>
        </div>
        <button class="summary-close" aria-label="关闭摘要" @click="summaryVisible = false">×</button>
      </div>
      <p>{{ summaryText }}</p>
      <div class="summary-actions">
        <el-button size="small" @click="copySummary">复制摘要</el-button>
        <el-button size="small" type="primary" @click="insertSummary">插入笔记正文</el-button>
      </div>
    </section>

    <div id="vditor"></div>

    <el-drawer v-model="versionDrawer" title="笔记历史版本" size="420px">
      <div v-if="versions.length" class="version-list">
        <div v-for="version in versions" :key="version.id" class="version-item">
          <div>
            <strong>版本 {{ version.version_no }}</strong>
            <span>{{ version.created_at }}</span>
          </div>
          <el-button size="small" @click="handleRestoreVersion(version.id)">恢复此版本</el-button>
        </div>
      </div>
      <el-empty v-else description="编辑并保存后会自动保留历史版本" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { createNote, getCategories, getNote, getTags, updateNote } from '@/api/notes'
import { assessDifficulty, summarize } from '@/api/ai'
import { getNoteVersions, restoreNoteVersion } from '@/api/learningCenter'

const route = useRoute()
const router = useRouter()
const noteId = route.params.id as string
const isNew = noteId === 'new'
const aiLoading = ref(false)
const saving = ref(false)
const focusMode = ref(false)
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const summaryText = ref('')
const summaryVisible = ref(false)
const versionDrawer = ref(false)
const versions = ref<any[]>([])

const form = reactive({
  title: '',
  categoryId: null as number | null,
  difficultyLevel: '',
  tagIds: [] as number[],
})

let vditor: Vditor

onMounted(async () => {
  const [categoryResponse, tagResponse] = await Promise.all([getCategories(), getTags()])
  categories.value = (categoryResponse as any).data || []
  tags.value = (tagResponse as any).data || []

  await nextTick()
  vditor = new Vditor('vditor', {
    height: 'calc(100vh - 140px)',
    mode: 'ir',
    placeholder: '开始记录你的知识…',
    after: () => { if (!isNew) loadNote() },
  })
  if (isNew) vditor.setValue('')
})

async function loadNote() {
  const response: any = await getNote(Number(noteId))
  const note = response.data
  form.title = note.title
  form.categoryId = note.categoryId
  form.difficultyLevel = note.difficultyLevel
  form.tagIds = note.tagIds || []
  vditor.setValue(note.contentMd || '')
}

async function handleSave() {
  const payload = { ...form, contentMd: vditor.getValue() }
  saving.value = true
  try {
    if (isNew) {
      const response: any = await createNote(payload)
      ElMessage.success('创建成功')
      router.replace(`/notes/${response.data.id}`)
    } else {
      await updateNote(Number(noteId), payload)
      ElMessage.success('保存成功')
    }
  } finally { saving.value = false }
}

async function handleSummarize() {
  aiLoading.value = true
  try {
    const response: any = await summarize(vditor.getValue())
    summaryText.value = response.data?.summary || response.summary || '暂无可用摘要'
    summaryVisible.value = true
  } finally {
    aiLoading.value = false
  }
}

async function handleAssess() {
  aiLoading.value = true
  try {
    const response: any = await assessDifficulty(vditor.getValue())
    form.difficultyLevel = response.data?.level || response.data
    ElMessage.success(`难度评估：${form.difficultyLevel}`)
  } finally {
    aiLoading.value = false
  }
}

async function copySummary() {
  await navigator.clipboard.writeText(summaryText.value)
  ElMessage.success('摘要已复制')
}

function insertSummary() {
  const current = vditor.getValue().trim()
  const quote = summaryText.value.replace(/\n/g, '\n> ')
  const summary = `> **AI 摘要**\n>\n> ${quote}`
  vditor.setValue(current ? `${current}\n\n---\n\n${summary}` : summary)
  summaryVisible.value = false
  ElMessage.success('摘要已插入正文')
}

async function openVersions() {
  const response: any = await getNoteVersions(Number(noteId))
  versions.value = response.data || []
  versionDrawer.value = true
}

async function handleRestoreVersion(versionId: number) {
  await restoreNoteVersion(Number(noteId), versionId)
  await loadNote()
  versionDrawer.value = false
  ElMessage.success('已恢复到所选版本')
}
</script>

<style scoped>
.editor-page { padding-bottom:18px; }.editor-header{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;padding:8px 2px 20px}.editor-title-group{min-width:0;display:flex;flex-direction:column;align-items:flex-start}.editor-kicker{color:var(--brand-600);font:700 10px/1 var(--font-mono);letter-spacing:.13em}.title-input{width:min(520px,68vw);margin-top:10px}.title-input :deep(.el-input__wrapper){padding:0;box-shadow:none!important;background:transparent}.title-input :deep(.el-input__inner){height:38px;color:var(--text-primary);font:800 28px/1.1 var(--font-display)}.title-input :deep(.el-input__inner::placeholder){color:var(--text-tertiary)}.save-status{margin-top:7px;color:var(--text-tertiary);font-size:11px}.editor-header-actions{display:flex;align-items:center;gap:8px;flex-shrink:0}
.toolbar { display:flex; align-items:center; flex-wrap:wrap; gap:10px; margin-bottom:16px; padding:12px; border:1px solid var(--border-light); border-radius:var(--radius-md); background:var(--bg-surface); }
.toolbar-spacer { flex:1; }
.editor-page :deep(.vditor) { overflow:hidden; border-color:var(--border-light); border-radius:var(--radius-lg); background:var(--bg-surface); box-shadow:var(--shadow-card); }.is-focus-mode :deep(.vditor){border-radius:0;box-shadow:none}.is-focus-mode{max-width:920px;margin:0 auto}.is-focus-mode .editor-header{padding-top:18px}.is-focus-mode :deep(.vditor-toolbar){position:sticky;top:0;z-index:3}
.editor-page :deep(.vditor-toolbar) { border-color:var(--border-light); background:#f0eee6; }
.ai-summary-panel { margin-bottom:16px; padding:18px 20px; border:1px solid var(--brand-200); border-radius:var(--radius-lg); background:linear-gradient(135deg,var(--brand-50),#fff); box-shadow:var(--shadow-xs); }
.summary-heading { display:flex; align-items:flex-start; justify-content:space-between; gap:16px; }
.summary-kicker { color:var(--brand-600); font:700 10px/1 var(--font-mono); letter-spacing:.12em; }
.summary-heading h3 { margin-top:6px; font:800 18px/1 var(--font-display); }
.summary-close { width:28px; height:28px; border:0; border-radius:50%; color:var(--text-secondary); background:transparent; font-size:22px; cursor:pointer; }
.summary-close:hover { color:var(--brand-600); background:var(--brand-100); }
.ai-summary-panel p { margin:14px 0; color:var(--text-secondary); white-space:pre-wrap; line-height:1.7; }
.summary-actions { display:flex; justify-content:flex-end; gap:8px; }
.version-list { display:flex; flex-direction:column; gap:10px; }
.version-item { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:14px; border:1px solid var(--border-light); border-radius:var(--radius-md); }
.version-item strong,.version-item span { display:block; }
.version-item span { margin-top:4px; color:var(--text-secondary); font-size:12px; }
@media(max-width:720px) { .editor-header{align-items:flex-start;flex-direction:column;gap:14px}.editor-header-actions{width:100%;justify-content:space-between}.title-input{width:100%}.toolbar :deep(.el-input),.toolbar :deep(.el-select) { width:100%!important; }.toolbar-spacer{display:none} }
</style>
