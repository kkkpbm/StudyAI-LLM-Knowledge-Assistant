<template>
  <div class="notes-page">
    <div class="page-toolbar">
      <div class="page-intro">
        <span>KNOWLEDGE LIBRARY</span>
        <h2>知识笔记</h2>
      </div>
      <el-input v-model="keyword" placeholder="搜索笔记..." clearable style="width: 260px" @input="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="categoryId" placeholder="分类筛选" clearable style="width: 150px" @change="search">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <div class="toolbar-spacer" />
      <el-button @click="workflowVisible = true">
        <el-icon><UploadFilled /></el-icon> 导入资料
      </el-button>
      <el-button type="primary" @click="$router.push({ name: 'NoteEditor', params: { id: 'new' } })">
        <el-icon><Plus /></el-icon> 新建笔记
      </el-button>
    </div>

    <div class="notes-table-card">
      <el-table :data="notes" stripe @row-click="(row: any) => $router.push(`/notes/${row.id}`)" style="cursor: pointer">
        <el-table-column prop="title" label="标题" min-width="220">
          <template #default="{ row }">
            <div class="title-cell">
              <el-icon :size="16" color="var(--brand-600)"><Document /></el-icon>
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="300" show-overflow-tooltip />
        <el-table-column prop="difficultyLevel" label="难度" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.difficultyLevel" size="small" :type="diffTag(row.difficultyLevel)">
              {{ diffLabel(row.difficultyLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click.stop="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrap" v-if="total > size">
      <el-pagination
        v-model:current-page="page"
        :total="total"
        :page-size="size"
        layout="prev, pager, next"
        @current-change="fetchNotes"
      />
    </div>

    <el-dialog v-model="workflowVisible" width="760px" destroy-on-close class="document-workflow-dialog">
      <template #header>
        <div class="workflow-title"><span>IMPORT WORKFLOW</span><h3>把资料变成可学习的知识</h3></div>
      </template>

      <div v-if="!workflow" class="workflow-start">
        <div class="workflow-copy">
          <span class="workflow-index">01 / UPLOAD</span>
          <h4>导入一份学习资料</h4>
          <p>支持 PDF、DOCX、TXT、Markdown；系统将提取正文、生成摘要与知识标签，再由你确认入库。</p>
        </div>
        <label class="drop-zone" :class="{ busy: parsing }">
          <input type="file" accept=".pdf,.docx,.txt,.md,.markdown" :disabled="parsing" @change="handleFileChange" />
          <el-icon :size="30"><UploadFilled /></el-icon>
          <strong>{{ selectedFile ? selectedFile.name : '选择或拖入文档' }}</strong>
          <small>{{ selectedFile ? formatFileSize(selectedFile.size) : '单个文件不超过 15MB' }}</small>
        </label>
        <el-button class="parse-btn" type="primary" :disabled="!selectedFile" :loading="parsing" @click="startParse">
          {{ parsing ? '正在解析资料…' : '开始解析' }}
        </el-button>
        <div v-if="parsing" class="workflow-steps">
          <span class="done">上传完成</span><i></i><span class="active">提取文本与 AI 整理中</span><i></i><span>等待确认入库</span>
        </div>
      </div>

      <div v-else class="workflow-review">
        <div class="workflow-progress"><span class="done">上传</span><i></i><span class="done">提取</span><i></i><span class="done">AI 整理</span><i></i><span class="active">确认入库</span></div>
        <div class="source-strip"><el-icon><Document /></el-icon><span>{{ workflow.file_name }}</span><small>{{ workflow.draft?.char_count || 0 }} 字已提取</small></div>
        <div class="review-grid">
          <div class="review-main">
            <label>笔记标题</label><el-input v-model="draft.title" />
            <label>AI 摘要</label><div class="summary-preview">{{ workflow.draft?.summary }}</div>
            <label>正文预览</label><el-input v-model="draft.contentMd" type="textarea" :rows="10" resize="none" />
          </div>
          <aside class="review-side">
            <label>建议标签</label>
            <div class="suggested-tags"><el-tag v-for="tag in workflow.draft?.suggested_tags || []" :key="tag" round>{{ tag }}</el-tag><span v-if="!(workflow.draft?.suggested_tags || []).length">暂无建议标签</span></div>
            <label>分类</label><el-select v-model="draft.categoryId" clearable placeholder="选择分类"><el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" /></el-select>
            <label>难度</label><el-select v-model="draft.difficultyLevel"><el-option label="入门" value="beginner" /><el-option label="中级" value="intermediate" /><el-option label="高级" value="advanced" /></el-select>
            <label>已有标签</label><el-select v-model="draft.tagIds" multiple collapse-tags placeholder="可选"><el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" /></el-select>
          </aside>
        </div>
      </div>

      <template #footer>
        <el-button @click="resetWorkflow">取消</el-button>
        <el-button v-if="workflow" type="primary" :loading="confirming" @click="confirmImport">确认并创建笔记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Document, Plus, Search, UploadFilled } from '@element-plus/icons-vue'
import { confirmDocumentWorkflow, getCategories, getNotes, getTags, deleteNote, parseDocument } from '@/api/notes'
import { ElMessage, ElMessageBox } from 'element-plus'

const keyword = ref('')
const categoryId = ref<number | null>(null)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const notes = ref<any[]>([])
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const router = useRouter()
const workflowVisible = ref(false)
const selectedFile = ref<File | null>(null)
const parsing = ref(false)
const confirming = ref(false)
const workflow = ref<any>(null)
const draft = reactive({ title: '', contentMd: '', categoryId: null as number | null, difficultyLevel: 'intermediate', tagIds: [] as number[] })

function diffTag(l: string) { const m: Record<string, string> = { beginner: 'success', intermediate: 'warning', advanced: 'danger' }; return m[l] || '' }
function diffLabel(l: string) { const m: Record<string, string> = { beginner: '入门', intermediate: '中级', advanced: '高级' }; return m[l] || l }

async function fetchNotes() {
  try {
    const res: any = await getNotes({ keyword: keyword.value, categoryId: categoryId.value, page: page.value, size: size.value })
    notes.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { notes.value = []; total.value = 0 }
}
async function search() { page.value = 1; await fetchNotes() }
async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该笔记？', '确认', { type: 'warning' })
  await deleteNote(id)
  ElMessage.success('删除成功')
  await fetchNotes()
}
function formatFileSize(size: number) { return size < 1024 * 1024 ? `${Math.ceil(size / 1024)} KB` : `${(size / 1024 / 1024).toFixed(1)} MB` }
function handleFileChange(event: Event) { selectedFile.value = (event.target as HTMLInputElement).files?.[0] || null }
async function startParse() {
  if (!selectedFile.value) return
  parsing.value = true
  try {
    const res: any = await parseDocument(selectedFile.value)
    workflow.value = res.data
    Object.assign(draft, { title: workflow.value.draft?.title || selectedFile.value.name, contentMd: workflow.value.draft?.content_md || '', categoryId: null, difficultyLevel: workflow.value.draft?.difficulty_level || 'intermediate', tagIds: [] })
  } catch { ElMessage.error('资料解析失败，请检查文件内容后重试') } finally { parsing.value = false }
}
async function confirmImport() {
  confirming.value = true
  try {
    const res: any = await confirmDocumentWorkflow(workflow.value.id, draft)
    ElMessage.success('资料已转化为知识笔记')
    workflowVisible.value = false
    resetWorkflow()
    await fetchNotes()
    const id = res.data?.note?.id
    if (id) router.push(`/notes/${id}`)
  } finally { confirming.value = false }
}
function resetWorkflow() { selectedFile.value = null; workflow.value = null; parsing.value = false; Object.assign(draft, { title: '', contentMd: '', categoryId: null, difficultyLevel: 'intermediate', tagIds: [] }) }
onMounted(async () => {
  await fetchNotes()
  try { const [cats, tagRes]: any = await Promise.all([getCategories(), getTags()]); categories.value = cats.data || []; tags.value = tagRes.data || [] } catch { /* */ }
})
</script>

<style scoped>
.notes-page { display: flex; flex-direction: column; gap: var(--space-5); }.page-intro{margin-right:8px}.page-intro span{color:var(--brand-600);font:700 9px var(--font-mono);letter-spacing:.13em}.page-intro h2{margin-top:5px;font:800 23px/1 var(--font-display)}
.page-toolbar { display: flex; gap: var(--space-3); align-items: center; }
.toolbar-spacer { flex: 1; }
.notes-table-card {
  position: relative; background: rgba(252,251,247,.94);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden; box-shadow: var(--shadow-card);
}
.notes-table-card::before { content: ''; position: absolute; z-index: 2; left: 0; top: 0; bottom: 0; width: 3px; background: linear-gradient(var(--brand-500), transparent 80%); }
.title-cell { display: flex; align-items: center; gap: var(--space-2); font-weight: 650; }
.pagination-wrap { display: flex; justify-content: center; }
.workflow-title span{color:var(--brand-600);font:700 10px var(--font-mono);letter-spacing:.13em}.workflow-title h3{margin-top:7px;font:800 22px var(--font-display)}.workflow-start{display:grid;grid-template-columns:1fr minmax(250px,.9fr);gap:24px;align-items:center;padding:8px 4px 16px}.workflow-index{color:var(--brand-600);font:700 10px var(--font-mono)}.workflow-copy h4{margin:12px 0 8px;font:800 24px/1.25 var(--font-display)}.workflow-copy p{color:var(--text-secondary);font-size:13px;line-height:1.75}.drop-zone{min-height:180px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:9px;border:1px dashed var(--brand-300);border-radius:var(--radius-lg);color:var(--brand-600);background:var(--brand-50);cursor:pointer}.drop-zone:hover{border-color:var(--brand-500);background:#fff}.drop-zone.busy{opacity:.65;cursor:wait}.drop-zone input{display:none}.drop-zone strong{color:var(--text-primary);font-size:13px}.drop-zone small{color:var(--text-secondary);font-size:11px}.parse-btn{grid-column:1/-1}.workflow-steps,.workflow-progress{display:flex;align-items:center;gap:8px;grid-column:1/-1;color:var(--text-tertiary);font-size:11px}.workflow-steps i,.workflow-progress i{height:1px;flex:1;background:var(--border-medium)}.workflow-steps .active,.workflow-progress .active{color:var(--brand-600);font-weight:700}.workflow-steps .done,.workflow-progress .done{color:var(--success-500)}.source-strip{display:flex;align-items:center;gap:8px;margin:4px 0 18px;padding:10px 12px;border:1px solid var(--border-light);border-radius:var(--radius-md);background:var(--gray-50);font-size:12px}.source-strip svg{color:var(--brand-600)}.source-strip small{margin-left:auto;color:var(--text-secondary)}.review-grid{display:grid;grid-template-columns:minmax(0,1.45fr) minmax(210px,.55fr);gap:18px}.review-main,.review-side{display:flex;flex-direction:column;gap:8px}.review-main>label,.review-side>label{margin-top:5px;color:var(--text-secondary);font-size:11px;font-weight:700}.summary-preview{padding:11px 12px;border-left:3px solid var(--accent-500);border-radius:0 var(--radius-sm) var(--radius-sm) 0;background:#fff7f3;color:var(--text-secondary);font-size:12px;line-height:1.7}.suggested-tags{display:flex;flex-wrap:wrap;gap:6px;min-height:32px}.suggested-tags span{color:var(--text-tertiary);font-size:11px}
@media (max-width: 700px) { .page-toolbar { flex-wrap: wrap; }.page-intro{width:100%}.toolbar-spacer { display: none; }.page-toolbar :deep(.el-input) { width: 100% !important; }.page-toolbar :deep(.el-select) { flex: 1; }.workflow-start,.review-grid{grid-template-columns:1fr} }
</style>
