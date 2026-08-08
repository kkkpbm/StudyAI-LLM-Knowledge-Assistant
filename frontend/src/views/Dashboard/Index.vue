<template>
  <div class="dashboard">
    <section class="dashboard-hero animate-fade-in-up">
      <div class="hero-copy">
        <span class="hero-kicker">TODAY'S LEARNING FIELD</span>
        <h2>今天，先完成一件<br><em>真正推动学习的事。</em></h2>
        <p>记录、复习、复盘——知识只有被调用，才会真正留下来。</p>
        <div class="hero-actions">
          <el-button type="primary" @click="router.push('/learning-center')">
            <el-icon><Reading /></el-icon>
            开始今日学习
          </el-button>
          <el-button @click="router.push('/notes/new')">
            <el-icon><Plus /></el-icon>
            记录新想法
          </el-button>
        </div>
      </div>

      <div class="knowledge-pulse" aria-hidden="true">
        <div class="pulse-ring ring-one"></div>
        <div class="pulse-ring ring-two"></div>
        <span class="pulse-node node-one"></span>
        <span class="pulse-node node-two"></span>
        <span class="pulse-node node-three"></span>
        <div class="pulse-center">
          <b>{{ stats[0].value }}</b>
          <small>篇知识</small>
        </div>
      </div>
      <span class="hero-coordinate">KNOWLEDGE IN MOTION</span>
    </section>

    <section class="action-deck animate-fade-in-up">
      <div class="deck-heading">
        <div>
          <span class="section-kicker">TODAY'S FOCUS</span>
          <h3>从这里开始今天的学习</h3>
        </div>
        <button class="text-link" @click="router.push('/learning-center')">查看学习中心 <el-icon><ArrowRight /></el-icon></button>
      </div>
      <div class="action-list">
        <button v-for="item in todayActions" :key="item.title" class="action-item" @click="router.push(item.path)">
          <span class="action-step">{{ item.step }}</span>
          <span class="action-icon"><el-icon><component :is="item.icon" /></el-icon></span>
          <span class="action-copy"><strong>{{ item.title }}</strong><small>{{ item.description }}</small></span>
          <span class="action-value">{{ item.value }}</span>
          <el-icon class="action-arrow"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>

    <div class="stats-grid">
      <div
        v-for="(stat, i) in stats"
        :key="stat.label"
        class="stat-card animate-fade-in-up"
        :style="{ animationDelay: `${i * 0.08}s` }"
      >
        <span class="stat-index">0{{ i + 1 }}</span>
        <div class="stat-icon-box" :class="`tone-${i + 1}`">
          <el-icon :size="24"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
        <div class="stat-trend" :class="stat.trend >= 0 ? 'up' : 'down'">
          <el-icon :size="14"><component :is="stat.trend >= 0 ? ArrowUp : ArrowDown" /></el-icon>
          <span>{{ Math.abs(stat.trend) }}%</span>
        </div>
      </div>
    </div>

    <section class="learning-loop animate-fade-in-up">
      <div class="loop-copy">
        <span class="section-kicker">YOUR LEARNING LOOP</span>
        <h3>让每一条笔记都有下一步</h3>
        <p>从沉淀知识到间隔复习，形成持续推进的学习闭环。</p>
      </div>
      <div class="loop-steps">
        <button v-for="(step, index) in learningLoop" :key="step.label" @click="router.push(step.path)">
          <span>{{ String(index + 1).padStart(2, '0') }}</span>
          <strong>{{ step.label }}</strong>
          <small>{{ step.detail }}</small>
        </button>
      </div>
    </section>

    <div class="charts-row">
      <div class="chart-card card-main animate-fade-in-up" style="animation-delay: 0.1s">
        <div class="chart-header">
          <h3>学习进度趋势</h3>
          <el-radio-group v-model="chartType" size="small">
            <el-radio-button value="week">本周</el-radio-button>
            <el-radio-button value="month">本月</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="progressChart" class="chart-body"></div>
      </div>

      <div class="chart-card card-side animate-fade-in-up" style="animation-delay: 0.15s">
        <div class="chart-header">
          <h3>知识分布</h3>
        </div>
        <div ref="categoryChart" class="chart-body"></div>
      </div>
    </div>

    <div class="section-card notes-panel animate-fade-in-up" style="animation-delay: 0.2s">
        <div class="section-header">
          <h3>最近笔记</h3>
          <el-button type="primary" size="small" @click="router.push('/notes')">查看全部</el-button>
        </div>
        <el-table :data="recentNotes" style="width: 100%" v-if="recentNotes.length">
          <el-table-column label="标题" min-width="220">
            <template #default="{ row }">
              <div class="note-cell">
                <el-icon :size="16" color="var(--brand-600)"><Document /></el-icon>
                <span class="note-title">{{ row.title }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="分类" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="getCategoryType(row.category)">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              <span class="time-text">{{ formatDate(row.createdAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="难度" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.difficultyLevel" size="small" :type="diffTag(row.difficultyLevel)">
                {{ diffLabel(row.difficultyLevel) }}
              </el-tag>
              <span v-else class="time-text">--</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="还没有笔记，去创建第一篇吧" :image-size="80" />
    </div>

    <div class="bottom-row">
        <div class="section-card animate-fade-in-up" style="animation-delay: 0.25s">
          <div class="section-header">
            <h3>
              <el-icon :size="20" color="var(--brand-600)"><AlarmClock /></el-icon>
              待复习
            </h3>
            <div class="section-actions">
              <el-button size="small" @click="showCheckInDialog = true">
                <el-icon><Plus /></el-icon>
                学习打卡
              </el-button>
            </div>
          </div>
          <div v-if="reviews.length > 0" class="review-list">
            <div v-for="r in reviews" :key="r.id" class="review-item">
              <div class="review-info">
                <el-icon color="#E29B19"><Warning /></el-icon>
                <span class="review-note">笔记 #{{ r.noteId }}</span>
                <span class="review-date">下次复习：{{ r.nextReviewAt }}</span>
              </div>
              <el-button size="small" type="primary" @click="handleCompleteReview(r.id!)">
                完成复习
              </el-button>
            </div>
          </div>
          <el-empty v-else description="暂无待复习内容" :image-size="60" />
        </div>

        <div class="section-card animate-fade-in-up" style="animation-delay: 0.3s">
          <div class="section-header">
            <h3>
              <el-icon :size="20" color="var(--brand-600)"><ChatDotRound /></el-icon>
              AI 学习建议
            </h3>
            <el-button text size="small" @click="loadInsights">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
          <el-empty v-if="suggestions.length === 0" description="AI 正在分析你的学习数据..." :image-size="60" />
          <div v-else class="suggestions">
            <div
              v-for="item in suggestions"
              :key="item.title"
              class="suggestion-item"
              @click="handleSuggestion(item)"
            >
              <div class="sg-icon" :style="{ background: item.color }">
                <el-icon :size="18" color="#fff"><component :is="item.icon" /></el-icon>
              </div>
              <div class="sg-text">
                <span class="sg-title">{{ item.title }}</span>
                <span class="sg-desc">{{ item.description }}</span>
              </div>
              <el-icon :size="16" color="var(--gray-400)"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
    </div>

    <el-dialog v-model="showCheckInDialog" title="学习打卡" width="400px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="学习时长">
          <el-input-number v-model="checkInMinutes" :min="1" :max="480" :step="5" />
          <span class="minute-unit">分钟</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCheckInDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCheckIn" :loading="checkInLoading">打卡</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getDashboardOverview, getCategoryDistribution } from '@/api/dashboard'
import { getNotes } from '@/api/notes'
import { getUpcomingReviews, completeReview } from '@/api/reviews'
import { getLearningInsight } from '@/api/ai'
import http from '@/api/index'
import { ElMessage } from 'element-plus'
import {
  Document, Timer, Share, Refresh, ChatDotRound,
  ArrowUp, ArrowDown, ArrowRight, List, AlarmClock, Warning, Plus, Reading, EditPen, TrendCharts,
} from '@element-plus/icons-vue'

const router = useRouter()

const stats = ref([
  { label: '笔记总数', value: 0, trend: 12, icon: Document },
  { label: '学习时长', value: '0 min', trend: 8, icon: Timer },
  { label: '知识节点', value: 0, trend: -3, icon: Share },
  { label: '复习次数', value: 0, trend: 15, icon: Refresh },
])

const chartType = ref('week')
const recentNotes = ref<any[]>([])
const reviews = ref<any[]>([])
const showCheckInDialog = ref(false)
const checkInMinutes = ref(30)
const checkInLoading = ref(false)
const suggestions = ref<any[]>([])
const progressChart = ref<HTMLElement>()
const categoryChart = ref<HTMLElement>()
const todayActions = computed(() => [
  { step: '01', title: '完成一次复习', description: reviews.value.length ? '待复习内容已经为你准备好' : '今天暂时没有待复习内容', value: reviews.value.length, icon: Reading, path: '/learning-center' },
  { step: '02', title: '沉淀一个想法', description: recentNotes.value.length ? '继续把零散思考变成知识' : '从第一篇笔记开始建立知识库', value: '+', icon: EditPen, path: '/notes/new' },
  { step: '03', title: '复盘学习节奏', description: '查看周报，调整下一步学习计划', value: '→', icon: TrendCharts, path: '/learning-center' },
])
const learningLoop = [
  { label: '沉淀笔记', detail: '记录与分类', path: '/notes/new' },
  { label: '生成测验', detail: '主动回忆', path: '/learning-center' },
  { label: '间隔复习', detail: '巩固记忆', path: '/learning-center' },
  { label: '复盘规划', detail: '持续成长', path: '/plans' },
]

async function loadInsights() {
  try {
    const res: any = await getLearningInsight()
    const data = res.data || {}
    suggestions.value = [
      { title: '学习总结', description: data.summary || '继续加油！', icon: ChatDotRound, color: 'linear-gradient(135deg, #315EFB, #6F8CFF)', action: '/notes' },
      { title: '下一步', description: data.next_step || '创建新的学习计划', icon: List, color: 'linear-gradient(135deg, #E29B19, #F6B83F)', action: '/plans' },
      { title: '需要复习', description: data.review_today?.join(', ') || '暂无待复习内容', icon: Refresh, color: 'linear-gradient(135deg, #18A058, #28C76F)', action: '/notes' },
      { title: '鼓励', description: data.motivation || '坚持就是胜利！', icon: Share, color: 'linear-gradient(135deg, #111827, #4C647F)', action: '/graph' },
    ]
  } catch {
    // keep defaults
  }
}

function formatDate(date: string) {
  return new Date(date).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function getCategoryType(cat: string) {
  const m: Record<string, string> = { 前端: '', AI: 'success', 后端: 'warning', 基础: 'info' }
  return m[cat] || 'info'
}

function handleSuggestion(s: any) {
  router.push(s.action)
}

function diffTag(level: string) {
  const m: Record<string, string> = { beginner: 'success', intermediate: 'warning', advanced: 'danger' }
  return m[level] || ''
}

function diffLabel(level: string) {
  const m: Record<string, string> = { beginner: '入门', intermediate: '中级', advanced: '高级' }
  return m[level] || level
}

async function loadData() {
  try {
    const [overRes, noteRes, reviewRes] = await Promise.all([
      getDashboardOverview(),
      getNotes({ page: 1, size: 5 }),
      getUpcomingReviews(5),
    ])
    const overview = overRes.data || {}
    stats.value[0].value = overview.noteCount ?? 0
    stats.value[1].value = (overview.todayMinutes ?? 0) + ' min'
    stats.value[2].value = overview.conceptCount ?? (overview.noteCount ? overview.noteCount * 2 : 0)
    stats.value[3].value = overview.reviewCount ?? 0

    const noteData = noteRes.data
    if (noteData?.records) {
      recentNotes.value = noteData.records.map((note: any) => ({
        id: note.id,
        title: note.title,
        category: note.category?.name || '未分类',
        createdAt: note.createdAt,
        difficultyLevel: note.difficultyLevel,
      }))
    }

    reviews.value = (reviewRes.data || []) as any[]
  } catch {
    // keep defaults
  }
  loadInsights()
}

async function handleCompleteReview(id: number) {
  try {
    await completeReview(id)
    ElMessage.success('复习完成')
    reviews.value = reviews.value.filter(r => r.id !== id)
    stats.value[3].value = Math.max(0, (stats.value[3].value as number) - 1)
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleCheckIn() {
  checkInLoading.value = true
  try {
    await http.post('/records/check-in', {
      durationMinutes: checkInMinutes.value,
    })
    ElMessage.success(`打卡成功！学习了 ${checkInMinutes.value} 分钟`)
    showCheckInDialog.value = false
    checkInMinutes.value = 30
    await loadData()
  } catch {
    ElMessage.error('打卡失败')
  } finally {
    checkInLoading.value = false
  }
}

async function initCharts() {
  if (!progressChart.value || !categoryChart.value) return
  const weekLabels = ['周一','周二','周三','周四','周五','周六','周日']
  let weekData = [0,0,0,0,0,0,0]
  let pieData: any[] = [{ value: 1, name: '暂无数据', itemStyle: { color: '#CBD5E1' } }]

  try {
    const month = new Date().toISOString().slice(0, 7)
    const [recordRes, categoryRes] = await Promise.all([
      http.get(`/records/calendar?month=${month}`),
      getCategoryDistribution(),
    ])
    const records = recordRes.data || {}
    const today = new Date()
    for (let i = 0; i < 7; i++) {
      const day = new Date(today)
      day.setDate(day.getDate() - (6 - i))
      weekData[i] = records[day.toISOString().slice(0, 10)] || 0
    }
    const categories = (categoryRes.data || []) as any[]
    if (categories.length) {
      const colors = ['#315EFB','#FF6D4A','#18A058','#E29B19','#38BDF8','#8B5CF6','#64748B']
      pieData = categories.map((category: any, index: number) => ({
        value: category.value || 0,
        name: category.name,
        itemStyle: { color: category.color || colors[index % colors.length] },
      }))
      if (pieData.every((item: any) => item.value === 0)) {
        pieData = [{ value: 1, name: '暂无数据', itemStyle: { color: '#CBD5E1' } }]
      }
    }
  } catch {
    // use defaults
  }

  const progress = echarts.init(progressChart.value)
  progress.setOption({
    tooltip: { trigger: 'axis', borderColor: '#E2E8F0' },
    grid: { left: 24, right: 24, top: 16, bottom: 24 },
    xAxis: {
      type: 'category',
      data: weekLabels,
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisLabel: { color: '#94A3B8', fontSize: 12 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#F1F5F9' } },
      axisLabel: { color: '#94A3B8', fontSize: 12 },
    },
    series: [{
      type: 'line',
      smooth: true,
      data: weekData,
      lineStyle: { color: '#315EFB', width: 3 },
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: { color: '#315EFB', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(49,94,251,0.2)' },
          { offset: 1, color: 'rgba(49,94,251,0.02)' },
        ]),
      },
    }],
  })

  const category = echarts.init(categoryChart.value)
  category.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['55%', '80%'],
      center: ['50%', '48%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      emphasis: { label: { show: true, fontWeight: 'bold' } },
      data: pieData,
    }],
  })

  window.addEventListener('resize', () => {
    progress.resize()
    category.resize()
  })
}

onMounted(() => {
  loadData()
  nextTick(() => initCharts())
})
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: var(--space-5); }

.dashboard-hero {
  min-height: 270px;
  padding: clamp(28px,4vw,48px);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  color: #fff;
  border-radius: var(--radius-xl);
  background: linear-gradient(120deg, #315EFB 0%, #172033 58%, #111827 100%);
  box-shadow: var(--shadow-lg);
}

.dashboard-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: .14;
  background-image: linear-gradient(rgba(255,255,255,.12) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.12) 1px,transparent 1px);
  background-size: 34px 34px;
  mask-image: linear-gradient(90deg,transparent,#000);
}

.hero-copy { position: relative; z-index: 2; }
.hero-kicker { color: #C4D0FF; font: 650 9px/1 var(--font-mono); letter-spacing: .16em; }
.hero-copy h2 { margin: 16px 0 10px; font-family: var(--font-display); font-size: clamp(30px,3.3vw,46px); font-weight: 800; line-height: 1.2; }
.hero-copy h2 em { color: #FFB39D; font-style: normal; }
.hero-copy p { color: rgba(255,255,255,.68); font-size: 13px; }
.hero-actions { margin-top: 24px; display: flex; gap: 10px; }
.hero-actions :deep(.el-button:not(.el-button--primary)) { color: #fff; border-color: rgba(255,255,255,.2); background: rgba(255,255,255,.08); }
.hero-coordinate { position: absolute; right: 24px; bottom: 18px; color: rgba(255,255,255,.32); font: 500 8px/1 var(--font-mono); letter-spacing: .12em; }

.action-deck,.learning-loop { padding:22px; border:1px solid var(--border-light); border-radius:var(--radius-lg); background:var(--bg-surface); box-shadow:var(--shadow-card); }
.deck-heading { display:flex; align-items:end; justify-content:space-between; gap:16px; margin-bottom:18px; }.section-kicker { color:var(--brand-600); font:700 10px/1 var(--font-mono); letter-spacing:.13em; }.deck-heading h3,.loop-copy h3 { margin-top:8px; font:800 21px/1.2 var(--font-display); }.text-link { display:inline-flex; align-items:center; gap:4px; border:0; color:var(--brand-600); background:transparent; font:700 12px var(--font-body); cursor:pointer; }
.action-list { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); border-top:1px solid var(--border-light); }.action-item { min-width:0; display:grid; grid-template-columns:auto auto minmax(0,1fr) auto auto; align-items:center; gap:10px; padding:18px 14px; border:0; border-right:1px solid var(--border-light); color:var(--text-primary); background:transparent; text-align:left; cursor:pointer; transition:background .2s; }.action-item:last-child{border-right:0}.action-item:hover{background:var(--brand-50)}.action-step{color:var(--text-tertiary);font:700 10px var(--font-mono)}.action-icon{width:32px;height:32px;display:grid;place-items:center;border-radius:10px;color:var(--brand-600);background:var(--brand-50)}.action-copy{min-width:0;display:flex;flex-direction:column;gap:3px}.action-copy strong{font-size:13px}.action-copy small{overflow:hidden;color:var(--text-secondary);font-size:11px;white-space:nowrap;text-overflow:ellipsis}.action-value{font:800 20px var(--font-display);color:var(--brand-700)}.action-arrow{color:var(--text-tertiary);font-size:14px}
.learning-loop { display:grid; grid-template-columns:250px 1fr; align-items:center; gap:28px; padding:26px 28px; background:linear-gradient(110deg,#f7f9ff,#fff); }.loop-copy p{margin-top:8px;color:var(--text-secondary);font-size:12px;line-height:1.6}.loop-steps{display:grid;grid-template-columns:repeat(4,1fr);gap:10px}.loop-steps button{position:relative;min-height:92px;padding:14px;border:1px solid var(--border-light);border-radius:var(--radius-md);color:var(--text-primary);background:#fff;text-align:left;cursor:pointer;transition:.2s}.loop-steps button:hover{border-color:var(--brand-300);transform:translateY(-2px);box-shadow:var(--shadow-xs)}.loop-steps span,.loop-steps strong,.loop-steps small{display:block}.loop-steps span{color:var(--brand-500);font:700 10px var(--font-mono)}.loop-steps strong{margin-top:12px;font-size:13px}.loop-steps small{margin-top:4px;color:var(--text-secondary);font-size:10px}

.knowledge-pulse {
  width: 190px;
  height: 190px;
  position: absolute;
  z-index: 1;
  right: 8%;
  top: 38px;
  border: 1px solid rgba(196,208,255,.26);
  border-radius: 50%;
}

.pulse-ring { position: absolute; border: 1px dashed rgba(196,208,255,.26); border-radius: 50%; }
.ring-one { inset: 25px; }
.ring-two { inset: 53px; }
.pulse-center { position: absolute; inset: 66px; display: grid; place-items: center; align-content: center; border-radius: 50%; background: rgba(17,24,39,.78); box-shadow: 0 0 0 8px rgba(255,255,255,.06); }
.pulse-center b { font: 750 23px/1 var(--font-display); }
.pulse-center small { margin-top: 4px; color: rgba(255,255,255,.58); font-size: 8px; }
.pulse-node { width: 8px; height: 8px; position: absolute; border-radius: 50%; background: #fff; box-shadow: 0 0 0 5px rgba(255,255,255,.12); }
.node-one { left: 16px; top: 38px; }
.node-two { right: 7px; top: 91px; }
.node-three { left: 67px; bottom: -3px; background: var(--accent-400); }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}

.stat-card {
  background: var(--bg-surface);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  border: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  position: relative;
  overflow: hidden;
  transition: all var(--transition-base);
}
.stat-card:hover { box-shadow: var(--shadow-md); border-color: var(--brand-200); transform: translateY(-2px); }
.stat-index { position: absolute; right: 12px; bottom: -10px; color: rgba(49,94,251,.06); font: 700 54px/1 var(--font-display); }
.stat-icon-box { width: 48px; height: 48px; border-radius: 14px 14px 14px 5px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.tone-1 { background: linear-gradient(135deg, #315EFB, #6F8CFF); }
.tone-2 { background: linear-gradient(135deg, #FF6D4A, #FF9A7D); }
.tone-3 { background: linear-gradient(135deg, #111827, #4C647F); }
.tone-4 { background: linear-gradient(135deg, #E29B19, #F6B83F); }
.stat-body { flex: 1; }
.stat-value { font-family: var(--font-display); font-size: 27px; font-weight: 800; display: block; line-height: 1.2; }
.stat-label { font-size: var(--text-xs); color: var(--text-secondary); }
.stat-trend { font-size: var(--text-xs); font-weight: 600; display: flex; align-items: center; gap: 2px; align-self: flex-start; }
.stat-trend.up { color: var(--success-500); }
.stat-trend.down { color: var(--danger-500); }

.charts-row { display: grid; grid-template-columns: 2fr 1fr; gap: var(--space-4); }
.chart-card,
.section-card {
  background: var(--bg-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  padding: 22px;
  box-shadow: var(--shadow-card);
}
.chart-header,
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-2); }
.chart-header h3,
.section-header h3 { font-family: var(--font-display); font-size: 17px; font-weight: 800; display: flex; align-items: center; gap: var(--space-2); }
.chart-body { height: 300px; }

.notes-panel {
  min-width: 0;
}

.bottom-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.bottom-row > .section-card { min-width: 0; }

.note-cell { display: flex; align-items: center; gap: var(--space-2); }
.note-title { font-weight: 500; }
.time-text { color: var(--text-secondary); font-size: var(--text-xs); }

.review-list { display: flex; flex-direction: column; gap: var(--space-3); }
.review-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  background: var(--gray-50);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}
.review-info { display: flex; align-items: center; gap: var(--space-3); }
.review-note { font-weight: 500; font-size: var(--text-sm); }
.review-date { color: var(--text-secondary); font-size: var(--text-xs); }

.suggestions { display: flex; flex-direction: column; gap: var(--space-3); }
.suggestion-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  background: var(--gray-50);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.suggestion-item:hover { background: var(--brand-50); transform: translateX(3px); }
.sg-icon { width: 42px; height: 42px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sg-text { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.sg-title { font-size: var(--text-sm); font-weight: 600; }
.sg-desc { font-size: var(--text-xs); color: var(--text-secondary); }
.minute-unit { margin-left: 8px; color: var(--text-secondary); }

@media (max-width: 1024px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .charts-row { grid-template-columns: 1fr; }
  .bottom-row { grid-template-columns: 1fr; }
  .action-list,.loop-steps { grid-template-columns:1fr; }.action-item{border-right:0;border-bottom:1px solid var(--border-light)}.action-item:last-child{border-bottom:0}.learning-loop{grid-template-columns:1fr}
}

@media (max-width: 640px) {
  .stats-grid { grid-template-columns: 1fr; }
  .dashboard-hero { min-height: 300px; align-items: flex-start; }
  .knowledge-pulse { width: 130px; height: 130px; right: -24px; top: 135px; opacity: .6; }
  .ring-two, .pulse-center { display: none; }
  .hero-actions { flex-wrap: wrap; }
  .deck-heading{align-items:flex-start;flex-direction:column}.action-deck,.learning-loop{padding:18px}
}
</style>
