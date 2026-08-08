<template>
  <div class="learning-center">
    <section class="center-hero">
      <div>
        <span class="eyebrow">ACTIVE RECALL STUDIO</span>
        <h2>把知识真正变成<br><em>可以调用的记忆</em></h2>
        <p>从笔记生成闪卡，用语义检索连接知识，并在每周报告中看见进步。</p>
        <button class="hero-start" @click="activeTab = 'review'">开始今日复习 <el-icon><ArrowRight /></el-icon></button>
      </div>
      <div class="hero-number">
        <strong>{{ dueCards.length }}</strong>
        <span>今日待复习</span>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="center-tabs">
      <el-tab-pane label="复习测验" name="review">
        <div class="review-layout">
          <section class="panel generator-panel">
            <div class="panel-heading">
              <div><span class="index">01</span><h3>AI 生成闪卡</h3></div>
            </div>
            <el-select v-model="selectedNoteId" filterable placeholder="选择一篇笔记">
              <el-option v-for="note in notes" :key="note.id" :label="note.title" :value="note.id" />
            </el-select>
            <div class="count-row">
              <span>生成数量</span>
              <el-input-number v-model="cardCount" :min="3" :max="12" />
            </div>
            <el-button type="primary" :loading="generating" @click="handleGenerate">
              <el-icon><MagicStick /></el-icon>从笔记生成测验
            </el-button>
            <p class="hint">AI 会混合生成问答、选择和判断题。</p>
          </section>

          <section class="panel study-panel">
            <div class="panel-heading">
              <div><span class="index">02</span><h3>今日记忆训练</h3></div>
              <span class="counter">{{ currentCardIndex + 1 }} / {{ dueCards.length }}</span>
            </div>
            <template v-if="currentCard">
              <div class="card-origin">{{ currentCard.note_title || currentCard.noteTitle }}</div>
              <div class="question">{{ currentCard.question }}</div>
              <p class="answer-guide">{{ cardType === 'qa' ? '先回忆答案，再与参考答案对照。' : '请选择你认为正确的答案。' }}</p>
              <div v-if="cardType === 'qa'" class="free-answer">
                <el-input v-model="writtenAnswer" type="textarea" :rows="3" maxlength="600" show-word-limit
                          placeholder="在这里写下你的答案（可选）" :disabled="answerVisible" />
              </div>
              <div v-else class="options">
                <button v-for="option in quizOptions" :key="option" @click="selectedOption = option"
                        :disabled="answerVisible" :class="{
                          selected: selectedOption === option,
                          correct: answerVisible && isCorrectOption(option),
                          incorrect: answerVisible && selectedOption === option && !isCorrectOption(option),
                        }">{{ option }}</button>
              </div>
              <button class="reveal" :disabled="cardType !== 'qa' && !selectedOption" @click="checkAnswer">
                {{ answerVisible ? '已查看结果' : (cardType === 'qa' ? '查看参考答案' : '提交答案') }}
              </button>
              <transition name="answer">
                <div v-if="answerVisible" class="answer-box">
                  <span>{{ cardType === 'qa' ? '参考答案' : (isAnswerCorrect ? '回答正确' : '正确答案') }}</span>
                  <p>{{ currentCard.answer }}</p>
                </div>
              </transition>
              <div v-if="answerVisible" class="quality-row">
                <button v-for="item in qualities" :key="item.value" :class="item.className"
                        @click="submitQuality(item.value)">
                  <strong>{{ item.label }}</strong><small>{{ item.tip }}</small>
                </button>
              </div>
            </template>
            <el-empty v-else description="今天没有待复习卡片，先从笔记生成一组吧" />
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="语义搜索" name="search">
        <section class="search-stage">
          <div class="search-copy">
            <span class="eyebrow">MEANING, NOT KEYWORDS</span>
            <h3>描述你想找的知识，<br>不必记住原文。</h3>
          </div>
          <div class="semantic-box">
            <el-input v-model="searchQuery" size="large" placeholder="例如：关于间隔复习为什么有效的笔记"
                      @keyup.enter="handleSemanticSearch">
              <template #append>
                <el-button :loading="searching" @click="handleSemanticSearch"><el-icon><Search /></el-icon></el-button>
              </template>
            </el-input>
          </div>
        </section>
        <div class="search-results">
          <article v-for="(item, index) in searchResults" :key="item.note_id" class="result-card"
                   @click="router.push(`/notes/${item.note_id}`)">
            <span class="result-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <div>
              <h4>{{ item.title }}</h4>
              <p>{{ item.content }}</p>
              <span class="score">语义相关度 {{ Math.round((item.score || 0) * 100) }}%</span>
            </div>
            <el-icon><ArrowRight /></el-icon>
          </article>
          <el-empty v-if="searched && !searchResults.length" description="没有找到语义相关的笔记" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="周报与成就" name="report">
        <div class="report-toolbar">
          <div><span class="eyebrow">WEEKLY REVIEW</span><h3>本周学习报告</h3></div>
          <el-button @click="printReport"><el-icon><Printer /></el-icon>导出 PDF</el-button>
        </div>
        <div class="report-stats">
          <div><strong>{{ report.minutes || 0 }}</strong><span>学习分钟</span></div>
          <div><strong>{{ report.newNotes || 0 }}</strong><span>新增笔记</span></div>
          <div><strong>{{ report.reviews || 0 }}</strong><span>完成复习</span></div>
          <div><strong>{{ report.correctRate || 0 }}%</strong><span>复习正确率</span></div>
        </div>
        <div class="report-grid">
          <section class="panel">
            <div class="panel-heading"><div><span class="index">03</span><h3>学习节奏</h3></div></div>
            <div class="daily-bars">
              <div v-for="day in weekDays" :key="day.date">
                <span class="bar" :style="{ height: `${Math.max(5, day.percent)}%` }"></span>
                <small>{{ day.label }}</small>
                <b>{{ day.minutes }}</b>
              </div>
            </div>
          </section>
          <section class="panel">
            <div class="panel-heading"><div><span class="index">04</span><h3>成就徽章</h3></div></div>
            <div class="achievement-list">
              <div v-for="item in achievements" :key="item.code" :class="{ unlocked: item.unlocked }">
                <span class="medal"><el-icon><Trophy /></el-icon></span>
                <div><strong>{{ item.name }}</strong><p>{{ item.description }}</p>
                  <el-progress :percentage="item.progress" :stroke-width="5" :show-text="false" />
                </div>
              </div>
            </div>
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="数据工具" name="tools">
        <div class="tools-grid">
          <section class="panel tool-card">
            <el-icon :size="28"><Upload /></el-icon><h3>导入笔记</h3>
            <p>支持 Markdown、TXT 和本系统导出的 JSON。</p>
            <label class="file-button"><input type="file" accept=".md,.txt,.json" multiple @change="handleImport">选择文件</label>
          </section>
          <section class="panel tool-card">
            <el-icon :size="28"><Download /></el-icon><h3>导出知识库</h3>
            <p>备份为 JSON，或合并导出为 Markdown 文档。</p>
            <div><el-button @click="handleExport('json')">导出 JSON</el-button>
              <el-button @click="handleExport('md')">导出 Markdown</el-button></div>
          </section>
        </div>
        <section class="panel trash-panel">
          <div class="panel-heading">
            <div><span class="index">05</span><h3>笔记回收站</h3></div>
            <el-button text @click="loadTrash"><el-icon><Refresh /></el-icon>刷新</el-button>
          </div>
          <el-table :data="trash">
            <el-table-column prop="title" label="标题" min-width="240" />
            <el-table-column prop="deleted_at" label="删除时间" width="190" />
            <el-table-column label="操作" width="190">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleRestoreTrash(row.id)">恢复</el-button>
                <el-button link type="danger" @click="handlePermanentDelete(row.id)">彻底删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Download, MagicStick, Printer, Refresh, Search, Trophy, Upload } from '@element-plus/icons-vue'
import { getNotes } from '@/api/notes'
import {
  deleteTrashPermanently, exportNotes, generateFlashcards, getAchievements,
  getDueFlashcards, getTrash, getWeeklyReport, importNotes, restoreTrash,
  reviewFlashcard, semanticSearch,
} from '@/api/learningCenter'

const router = useRouter()
const activeTab = ref('review')
const notes = ref<any[]>([])
const selectedNoteId = ref<number>()
const cardCount = ref(6)
const generating = ref(false)
const dueCards = ref<any[]>([])
const currentCardIndex = ref(0)
const answerVisible = ref(false)
const selectedOption = ref('')
const writtenAnswer = ref('')
const searchQuery = ref('')
const searching = ref(false)
const searched = ref(false)
const searchResults = ref<any[]>([])
const report = ref<any>({})
const achievements = ref<any[]>([])
const trash = ref<any[]>([])
const qualities = [
  { value: 1, label: '忘记', tip: '明天再见', className: 'q-forgot' },
  { value: 3, label: '困难', tip: '需要巩固', className: 'q-hard' },
  { value: 4, label: '一般', tip: '基本掌握', className: 'q-good' },
  { value: 5, label: '熟练', tip: '延长间隔', className: 'q-easy' },
]
const currentCard = computed(() => dueCards.value[currentCardIndex.value])
const cardType = computed(() => String(currentCard.value?.card_type || currentCard.value?.cardType || 'qa').toLowerCase())
const cardOptions = computed(() => {
  const raw = currentCard.value?.options_json
  if (!raw) return []
  try { return typeof raw === 'string' ? JSON.parse(raw) : raw } catch { return [] }
})
const quizOptions = computed(() => {
  if (cardOptions.value.length) return cardOptions.value
  return cardType.value === 'judge' ? ['正确', '错误'] : []
})
const isAnswerCorrect = computed(() => {
  if (!selectedOption.value) return false
  return normalizeAnswer(selectedOption.value) === normalizeAnswer(currentCard.value?.answer || '')
})
const weekDays = computed(() => {
  const values = report.value.daily || []
  const max = Math.max(1, ...values.map((v: any) => Number(v.minutes || 0)))
  return values.map((v: any) => ({
    date: v.date, minutes: Number(v.minutes || 0),
    label: new Date(v.date).toLocaleDateString('zh-CN', { weekday: 'short' }),
    percent: Number(v.minutes || 0) / max * 100,
  }))
})

async function loadCards() {
  const res: any = await getDueFlashcards()
  dueCards.value = res.data || []
  currentCardIndex.value = 0
}
async function handleGenerate() {
  if (!selectedNoteId.value) return ElMessage.warning('请先选择笔记')
  generating.value = true
  try {
    await generateFlashcards(selectedNoteId.value, cardCount.value)
    await loadCards()
    ElMessage.success('闪卡已生成')
  } finally { generating.value = false }
}
async function submitQuality(quality: number) {
  if (!currentCard.value) return
  await reviewFlashcard(currentCard.value.id, quality)
  dueCards.value.splice(currentCardIndex.value, 1)
  answerVisible.value = false
  selectedOption.value = ''
  writtenAnswer.value = ''
  ElMessage.success('已根据掌握度安排下次复习')
}
function normalizeAnswer(value: string) {
  return String(value).trim().replace(/\s+/g, '').replace(/[。！？!?,，]/g, '').toLowerCase()
}
function isCorrectOption(option: string) {
  return normalizeAnswer(option) === normalizeAnswer(currentCard.value?.answer || '')
}
function checkAnswer() {
  if (answerVisible.value) return
  if (cardType.value !== 'qa' && !selectedOption.value) return ElMessage.warning('请先选择一个答案')
  answerVisible.value = true
}
async function handleSemanticSearch() {
  if (!searchQuery.value.trim()) return
  searching.value = true
  try {
    const res: any = await semanticSearch(searchQuery.value)
    searchResults.value = res.data || []
    searched.value = true
  } finally { searching.value = false }
}
async function loadTrash() {
  const res: any = await getTrash()
  trash.value = res.data || []
}
async function handleRestoreTrash(id: number) {
  await restoreTrash(id); await loadTrash(); ElMessage.success('笔记已恢复')
}
async function handlePermanentDelete(id: number) {
  await ElMessageBox.confirm('彻底删除后无法恢复，确定继续吗？', '彻底删除', { type: 'warning' })
  await deleteTrashPermanently(id); await loadTrash()
}
async function handleImport(event: Event) {
  const files = Array.from((event.target as HTMLInputElement).files || [])
  const items: any[] = []
  for (const file of files) {
    const text = await file.text()
    if (file.name.endsWith('.json')) {
      const parsed = JSON.parse(text)
      items.push(...(Array.isArray(parsed) ? parsed : [parsed]))
    } else {
      items.push({ title: file.name.replace(/\.(md|txt)$/i, ''), contentMd: text })
    }
  }
  if (items.length) {
    await importNotes(items)
    ElMessage.success(`成功导入 ${items.length} 篇笔记`)
  }
}
function download(content: string, name: string, type: string) {
  const url = URL.createObjectURL(new Blob([content], { type }))
  const a = document.createElement('a'); a.href = url; a.download = name; a.click()
  URL.revokeObjectURL(url)
}
async function handleExport(type: 'json' | 'md') {
  const res: any = await exportNotes()
  const data = res.data || []
  if (type === 'json') download(JSON.stringify(data, null, 2), 'knowledge-backup.json', 'application/json')
  else download(data.map((n: any) => `# ${n.title}\n\n${n.contentMd || ''}`).join('\n\n---\n\n'),
    'knowledge-notes.md', 'text/markdown')
}
function printReport() { window.print() }

onMounted(async () => {
  const [notesRes, reportRes, achievementRes] = await Promise.all([
    getNotes({ page: 1, size: 200 }), getWeeklyReport(), getAchievements(),
  ])
  notes.value = (notesRes as any).data?.records || []
  report.value = (reportRes as any).data || {}
  achievements.value = (achievementRes as any).data || []
  await Promise.all([loadCards(), loadTrash()])
})
</script>

<style scoped>
.learning-center { display:flex; flex-direction:column; gap:var(--space-5); }
.center-hero { min-height:230px; padding:34px 42px; display:flex; align-items:center; justify-content:space-between; color:#fff; overflow:hidden; position:relative; border-radius:var(--radius-xl); background:linear-gradient(120deg,#111827 0%,#21345f 55%,#315efb 100%); box-shadow:var(--shadow-lg); }
.center-hero::after { content:''; width:280px; height:280px; position:absolute; right:-80px; top:-110px; border:1px solid rgba(255,255,255,.2); border-radius:50%; box-shadow:0 0 0 48px rgba(255,255,255,.04),0 0 0 96px rgba(255,255,255,.03); }
.eyebrow { color:#9db1ff; font:700 10px/1 var(--font-mono); letter-spacing:.16em; }
.center-hero h2 { margin:14px 0 10px; font:800 clamp(30px,3vw,44px)/1.18 var(--font-display); }
.center-hero h2 em { color:#ff9a7d; font-style:normal; }
.center-hero p { color:rgba(255,255,255,.65); }.hero-start{display:inline-flex;align-items:center;gap:6px;margin-top:20px;padding:9px 13px;border:1px solid rgba(255,255,255,.26);border-radius:999px;color:#fff;background:rgba(255,255,255,.08);font:700 12px var(--font-body);cursor:pointer;transition:.2s}.hero-start:hover{background:rgba(255,255,255,.16);transform:translateY(-1px)}
.hero-number { z-index:1; width:140px; height:140px; display:grid; place-items:center; align-content:center; border:1px solid rgba(255,255,255,.25); border-radius:50%; background:rgba(17,24,39,.25); }
.hero-number strong { font:800 42px/1 var(--font-display); }.hero-number span{margin-top:8px;font-size:12px;color:#cbd5ff}
.center-tabs :deep(.el-tabs__header){margin-bottom:20px}.center-tabs :deep(.el-tabs__item){font-weight:700}
.review-layout,.report-grid { display:grid; grid-template-columns:minmax(280px,.7fr) minmax(0,1.6fr); gap:var(--space-4); }
.panel { padding:24px; border:1px solid var(--border-light); border-radius:var(--radius-lg); background:var(--bg-surface); box-shadow:var(--shadow-card); }
.panel-heading { display:flex; align-items:center; justify-content:space-between; margin-bottom:20px; }
.panel-heading>div { display:flex; align-items:center; gap:10px }.panel-heading h3{font:800 18px/1 var(--font-display)}.index{color:var(--brand-500);font:700 10px/1 var(--font-mono)}
.generator-panel { display:flex; flex-direction:column; gap:18px; align-self:start }.count-row{display:flex;align-items:center;justify-content:space-between;font-size:13px}.hint{font-size:12px;color:var(--text-tertiary)}
.study-panel{min-height:430px}.counter,.card-origin{font:650 11px/1 var(--font-mono);color:var(--text-tertiary)}.question{margin:35px 0 10px;font:800 clamp(23px,2.3vw,34px)/1.35 var(--font-display)}.answer-guide{margin-bottom:18px;color:var(--text-secondary);font-size:13px}
.free-answer{margin-bottom:14px}.options{display:grid;grid-template-columns:repeat(2,1fr);gap:10px;margin-bottom:18px}.options button,.reveal{padding:12px 14px;border:1px solid var(--border-medium);border-radius:var(--radius-md);background:#fff;text-align:left;transition:.18s}.options button{cursor:pointer}.options button:not(:disabled):hover{border-color:var(--brand-400);transform:translateY(-1px)}.options button.selected{color:var(--brand-600);border-color:var(--brand-500);background:var(--brand-50)}.options button.correct{color:#18734b;border-color:#37ad78;background:#ecfaf2}.options button.incorrect{color:#ba3d37;border-color:#ed8f88;background:#fff0ef}.options button:disabled{cursor:default}.reveal{width:100%;text-align:center;color:var(--brand-600);font-weight:700;cursor:pointer}.reveal:disabled{color:var(--text-tertiary);background:var(--gray-50);cursor:not-allowed}
.answer-box{margin-top:16px;padding:18px;border-left:3px solid var(--accent-500);border-radius:0 var(--radius-md) var(--radius-md) 0;background:#fff7f3}.answer-box span{font-size:11px;color:var(--accent-600)}.answer-box p{margin-top:6px;line-height:1.7}
.quality-row{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin-top:18px}.quality-row button{padding:11px 6px;border:0;border-radius:var(--radius-md);cursor:pointer}.quality-row strong,.quality-row small{display:block}.quality-row small{margin-top:3px;font-size:9px;opacity:.7}.q-forgot{background:#fff0ed;color:#c94d35}.q-hard{background:#fff8e8;color:#a66b00}.q-good{background:#eef3ff;color:#315efb}.q-easy{background:#eaf8f1;color:#168456}
.search-stage{padding:38px;border-radius:var(--radius-xl);display:grid;grid-template-columns:1fr 1.4fr;align-items:end;background:linear-gradient(135deg,#eef3ff,#fff)}.search-copy h3{margin-top:12px;font:800 28px/1.3 var(--font-display)}.search-results{display:flex;flex-direction:column;gap:10px;margin-top:18px}.result-card{padding:18px 22px;display:grid;grid-template-columns:44px 1fr 24px;gap:14px;align-items:center;border:1px solid var(--border-light);border-radius:var(--radius-lg);background:#fff;cursor:pointer;transition:.2s}.result-card:hover{transform:translateX(4px);border-color:var(--brand-300)}.result-index{font:700 13px var(--font-mono);color:var(--brand-400)}.result-card h4{font-size:16px}.result-card p{margin:6px 0;color:var(--text-secondary);font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.score{font-size:11px;color:var(--brand-600)}
.report-toolbar{display:flex;justify-content:space-between;align-items:end;margin-bottom:18px}.report-toolbar h3{margin-top:8px;font:800 28px var(--font-display)}.report-stats{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:16px}.report-stats div{padding:20px;border-radius:var(--radius-lg);background:#fff;border:1px solid var(--border-light)}.report-stats strong{display:block;font:800 30px var(--font-display)}.report-stats span{font-size:12px;color:var(--text-secondary)}
.daily-bars{height:230px;display:flex;align-items:end;justify-content:space-around;gap:12px}.daily-bars>div{height:100%;flex:1;display:flex;flex-direction:column;justify-content:end;align-items:center;gap:5px}.bar{width:min(38px,70%);min-height:5px;border-radius:8px 8px 2px 2px;background:linear-gradient(#315efb,#9db1ff)}.daily-bars small{color:var(--text-tertiary)}.daily-bars b{font-size:11px}
.achievement-list{display:flex;flex-direction:column;gap:10px}.achievement-list>div{display:grid;grid-template-columns:42px 1fr;gap:10px;align-items:center;opacity:.45}.achievement-list>div.unlocked{opacity:1}.medal{width:38px;height:38px;display:grid;place-items:center;border-radius:50%;background:var(--gray-100);color:var(--gray-500)}.unlocked .medal{color:#fff;background:linear-gradient(135deg,#e29b19,#ff6d4a)}.achievement-list p{font-size:11px;color:var(--text-secondary);margin:2px 0 6px}
.tools-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:16px}.tool-card{display:flex;flex-direction:column;gap:12px}.tool-card>i{color:var(--brand-600)}.tool-card p{color:var(--text-secondary);font-size:13px}.file-button{align-self:start;padding:9px 16px;color:#fff;background:var(--brand-600);border-radius:var(--radius-md);cursor:pointer}.file-button input{display:none}.trash-panel{margin-top:16px}
.answer-enter-active,.answer-leave-active{transition:.2s}.answer-enter-from,.answer-leave-to{opacity:0;transform:translateY(-6px)}
@media(max-width:900px){.review-layout,.report-grid,.search-stage{grid-template-columns:1fr}.report-stats{grid-template-columns:repeat(2,1fr)}.hero-number{display:none}}
@media(max-width:600px){.center-hero{padding:28px}.quality-row,.options,.tools-grid{grid-template-columns:1fr 1fr}.report-stats{grid-template-columns:1fr 1fr}}
@media print{.center-hero,.center-tabs :deep(.el-tabs__header),.review-layout,.search-stage,.tools-grid,.trash-panel,.report-toolbar button{display:none!important}.learning-center{display:block}.report-grid{grid-template-columns:1fr 1fr}}
</style>
