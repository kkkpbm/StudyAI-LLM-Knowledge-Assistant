<template>
  <div class="graph-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="mini-stat">
          <div class="stat-val">{{ stats.conceptCount }}</div>
          <div class="stat-label">知识节点</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="mini-stat">
          <div class="stat-val">{{ stats.relationCount }}</div>
          <div class="stat-label">关系数量</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="mini-stat">
          <div class="stat-val">{{ stats.noteCount }}</div>
          <div class="stat-label">关联笔记</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="mini-stat">
          <div class="stat-val">{{ stats.avgWeight }}</div>
          <div class="stat-label">平均权重</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleBuildGraph" :loading="building">
          <el-icon><MagicStick /></el-icon>
          AI 构建知识图谱
        </el-button>
        <el-button @click="loadRelations" :loading="loading" :disabled="building">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button @click="openManualDialog">手动添加关系</el-button>
        <el-button @click="openRecommendations">关系推荐</el-button>
        <el-button
          v-if="relations.length > 0"
          type="danger"
          text
          @click="handleDeleteAll"
          :disabled="building"
        >
          清空图谱
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-select v-model="noteFilter" clearable placeholder="按笔记筛选" style="width:170px">
          <el-option v-for="note in noteOptions" :key="note.id" :label="note.title" :value="note.id" />
        </el-select>
        <el-select v-model="typeFilter" clearable placeholder="关系类型" style="width:130px">
          <el-option label="相关" value="related" /><el-option label="扩展" value="extends" />
          <el-option label="前置" value="prerequisite" /><el-option label="矛盾" value="contradicts" />
        </el-select>
        <el-input
          v-if="viewMode === 'graph'"
          v-model="searchKeyword"
          placeholder="搜索节点..."
          clearable
          :prefix-icon="Search"
          style="width: 200px"
        />
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="graph">关系图</el-radio-button>
          <el-radio-button value="table">关系列表</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 构建进度提示 -->
    <el-alert
      v-if="building"
      title="AI 正在分析笔记并构建知识图谱，请稍候..."
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <!-- 图视图 -->
    <el-card v-if="viewMode === 'graph'" v-loading="loading" class="graph-card">
      <div v-if="relations.length === 0 && !loading" class="empty-state">
        <el-icon :size="64" color="#c0c4cc"><Share /></el-icon>
        <p>还没有知识图谱数据</p>
        <p class="empty-hint">点击"AI 构建知识图谱"，让 AI 自动分析你的笔记</p>
      </div>
      <div v-else ref="graphChart" class="graph-container"></div>
    </el-card>

    <!-- 表视图 -->
    <el-card v-else v-loading="loading">
      <div v-if="filteredRelations.length === 0 && !loading" class="empty-state">
        <el-icon :size="64" color="#c0c4cc"><Share /></el-icon>
        <p>还没有知识图谱数据</p>
      </div>
      <el-table v-else :data="filteredRelations" stripe max-height="calc(100vh - 300px)">
        <el-table-column prop="source" label="源概念" width="180" sortable="custom">
          <template #default="{ row }">
            <el-button link type="primary" @click="openConceptDetail(row.source)">
              {{ row.source }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="target" label="目标概念" width="180" sortable="custom">
          <template #default="{ row }">
            <el-button link type="primary" @click="openConceptDetail(row.target)">
              {{ row.target }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="relationType" label="关系类型" width="120">
          <template #default="{ row }">
            <el-tag :type="relationTagType(row.relationType)" size="small">
              {{ relationLabel(row.relationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="weight" label="权重" width="100" sortable="custom">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round(row.weight * 100)"
              :color="weightColor(row.weight)"
              :stroke-width="6"
            />
          </template>
        </el-table-column>
        <el-table-column prop="noteId" label="笔记ID" width="100" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="editRelation(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDeleteRelation(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 图例 -->
    <div v-if="viewMode === 'graph' && relations.length > 0" class="legend">
      <span class="legend-item"><span class="legend-dot" style="background:#315EFB"></span> 基础 (related)</span>
      <span class="legend-item"><span class="legend-dot" style="background:#FF6D4A"></span> 扩展 (extends)</span>
      <span class="legend-item"><span class="legend-dot" style="background:#CF563F"></span> 前置 (prerequisite)</span>
      <span class="legend-item"><span class="legend-dot" style="background:#D49A1D"></span> 矛盾 (contradicts)</span>
    </div>

    <!-- 概念详情抽屉 -->
    <el-drawer
      v-model="detailDrawerVisible"
      :title="selectedConcept"
      size="420px"
      direction="rtl"
    >
      <template v-if="conceptDetail">
        <div class="detail-section">
          <h4 class="detail-title">
            <el-icon><Connection /></el-icon>
            关联关系 ({{ conceptDetail.relationCount }})
          </h4>
          <div class="relation-list">
            <div
              v-for="rel in conceptDetail.relations"
              :key="rel.id"
              class="relation-item"
            >
              <span class="rel-source">{{ rel.source }}</span>
              <el-tag :type="relationTagType(rel.relationType)" size="small" class="rel-tag">
                {{ relationLabel(rel.relationType) }}
              </el-tag>
              <span class="rel-target">{{ rel.target }}</span>
              <el-progress
                :percentage="Math.round(rel.weight * 100)"
                :color="weightColor(rel.weight)"
                :stroke-width="4"
                style="width: 60px; flex-shrink: 0"
              />
            </div>
          </div>
        </div>

        <el-divider />

        <div class="detail-section">
          <h4 class="detail-title">
            <el-icon><Link /></el-icon>
            连接概念 ({{ conceptDetail.connectedConcepts?.length || 0 }})
          </h4>
          <div class="concept-tags">
            <el-tag
              v-for="concept in conceptDetail.connectedConcepts"
              :key="concept"
              type="primary"
              effect="plain"
              class="concept-tag"
              @click="openConceptDetail(concept)"
            >
              {{ concept }}
            </el-tag>
          </div>
        </div>

        <el-divider />

        <div class="detail-section">
          <h4 class="detail-title">
            <el-icon><Document /></el-icon>
            相关笔记 ({{ conceptDetail.relatedNotes?.length || 0 }})
          </h4>
          <div class="note-list">
            <div
              v-for="note in conceptDetail.relatedNotes"
              :key="note.id"
              class="note-item"
              @click="router.push(`/notes/${note.id}`)"
            >
              <el-icon :size="16"><Document /></el-icon>
              <span class="note-title">{{ note.title }}</span>
              <el-tag v-if="note.difficultyLevel" size="small" type="info">
                {{ note.difficultyLevel }}
              </el-tag>
              <el-icon class="note-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
          <el-empty v-if="conceptDetail.relatedNotes?.length === 0" description="暂无关联笔记" :image-size="60" />
        </div>
      </template>

      <template v-else>
        <div v-loading="detailLoading" style="min-height: 200px"></div>
      </template>
    </el-drawer>

    <el-dialog v-model="manualVisible" :title="manualForm.id ? '编辑知识关系' : '建立知识关系'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="关联笔记">
          <el-select v-model="manualForm.noteId" filterable style="width:100%">
            <el-option v-for="note in noteOptions" :key="note.id" :label="note.title" :value="note.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="源概念"><el-input v-model="manualForm.source" /></el-form-item>
        <el-form-item label="目标概念"><el-input v-model="manualForm.target" /></el-form-item>
        <el-form-item label="关系类型">
          <el-select v-model="manualForm.relationType" style="width:100%">
            <el-option label="相关" value="related" /><el-option label="扩展" value="extends" />
            <el-option label="前置依赖" value="prerequisite" /><el-option label="矛盾" value="contradicts" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系权重"><el-slider v-model="manualForm.weightPercent" :min="10" :max="100" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="manualVisible=false">取消</el-button>
        <el-button type="primary" @click="saveManualRelation">保存关系</el-button></template>
    </el-dialog>

    <el-drawer v-model="recommendVisible" title="可能缺失的知识关系" size="460px">
      <div class="recommend-list">
        <div v-for="item in recommendations" :key="`${item.noteId}-${item.source}-${item.target}`">
          <strong>{{ item.source }} → {{ item.target }}</strong>
          <p>{{ item.reason }}</p>
          <el-button size="small" type="primary" @click="acceptRecommendation(item)">添加关系</el-button>
        </div>
        <el-empty v-if="!recommendations.length" description="暂未发现缺失关系" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, MagicStick, Connection, Link, Document, ArrowRight } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  buildKnowledgeGraph,
  getKnowledgeRelations,
  deleteAllRelations,
  deleteNoteRelations,
  getConceptDetail,
  createKnowledgeRelation,
  deleteKnowledgeRelation,
  getRelationRecommendations,
  updateKnowledgeRelation,
} from '@/api/ai'
import { getNotes } from '@/api/notes'

interface Relation {
  id: number
  noteId: number
  source: string
  target: string
  relationType: string
  weight: number
}

interface NoteInfo {
  id: number
  title: string
  difficultyLevel: string | null
}

interface ConceptDetail {
  conceptName: string
  relationCount: number
  relations: Relation[]
  connectedConcepts: string[]
  relatedNotes: NoteInfo[]
}

const router = useRouter()
const loading = ref(false)
const building = ref(false)
const viewMode = ref<'graph' | 'table'>('graph')
const searchKeyword = ref('')
const noteFilter = ref<number>()
const typeFilter = ref('')
const relations = ref<Relation[]>([])
const graphChart = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

// 概念详情相关
const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const selectedConcept = ref('')
const conceptDetail = ref<ConceptDetail | null>(null)
const manualVisible = ref(false)
const recommendVisible = ref(false)
const recommendations = ref<any[]>([])
const noteOptions = ref<any[]>([])
const manualForm = ref({ id: undefined as number | undefined, noteId: undefined as number | undefined, source: '', target: '', relationType: 'related', weightPercent: 50 })

// 统计数据
const stats = computed(() => {
  const conceptSet = new Set<string>()
  const noteSet = new Set<number>()
  let totalWeight = 0
  relations.value.forEach((r) => {
    conceptSet.add(r.source)
    conceptSet.add(r.target)
    noteSet.add(r.noteId)
    totalWeight += r.weight
  })
  return {
    conceptCount: conceptSet.size,
    relationCount: relations.value.length,
    noteCount: noteSet.size,
    avgWeight:
      relations.value.length > 0
        ? (totalWeight / relations.value.length).toFixed(2)
        : '0.00',
  }
})

// 过滤后的关系（用于表格搜索）
const filteredRelations = computed(() => {
  let result = relations.value
  if (noteFilter.value) result = result.filter(r => r.noteId === noteFilter.value)
  if (typeFilter.value) result = result.filter(r => r.relationType === typeFilter.value)
  if (!searchKeyword.value) return result
  const kw = searchKeyword.value.toLowerCase()
  return result.filter(
    (r) =>
      r.source.toLowerCase().includes(kw) ||
      r.target.toLowerCase().includes(kw) ||
      r.relationType.toLowerCase().includes(kw)
  )
})

// 关系类型标签
function relationTagType(type: string) {
  const map: Record<string, string> = {
    prerequisite: 'danger',
    related: '',
    extends: 'success',
    contradicts: 'warning',
  }
  return map[type] || 'info'
}

function relationLabel(type: string) {
  const map: Record<string, string> = {
    prerequisite: '前置依赖',
    related: '相关',
    extends: '扩展',
    contradicts: '矛盾',
  }
  return map[type] || type
}

function weightColor(w: number) {
  if (w >= 0.8) return '#FF6D4A'
  if (w >= 0.5) return '#315EFB'
  return '#D49A1D'
}

// 打开概念详情
async function openConceptDetail(conceptName: string) {
  selectedConcept.value = conceptName
  detailDrawerVisible.value = true
  conceptDetail.value = null
  detailLoading.value = true
  try {
    const res: any = await getConceptDetail(conceptName)
    conceptDetail.value = res.data || null
  } catch {
    conceptDetail.value = null
  } finally {
    detailLoading.value = false
  }
}

// 加载已有关系
async function loadRelations() {
  loading.value = true
  try {
    const res: any = await getKnowledgeRelations()
    relations.value = res.data || []
    await nextTick()
    renderGraph()
  } finally {
    loading.value = false
  }
}

// 构建知识图谱
async function handleBuildGraph() {
  building.value = true
  try {
    const res: any = await buildKnowledgeGraph()
    const data = res.data || {}
    ElMessage.success(
      `构建完成！处理了 ${data.processedNotes || 0} 篇笔记，提取了 ${data.totalRelations || 0} 条关系`
    )
    await loadRelations()
  } catch {
    // error handled by interceptor
  } finally {
    building.value = false
  }
}

// 删除所有关系
async function handleDeleteAll() {
  try {
    await ElMessageBox.confirm('确定要清空所有知识图谱数据吗？', '确认', {
      type: 'warning',
    })
    await deleteAllRelations()
    relations.value = []
    ElMessage.success('已清空')
  } catch {
    // cancelled
  }
}

// 删除单条关系
async function handleDeleteRelation(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除这条关系吗？', '确认', {
      type: 'warning',
    })
    await deleteKnowledgeRelation(id)
    await loadRelations()
    ElMessage.success('已删除')
  } catch {
    // cancelled
  }
}

function openManualDialog() {
  manualForm.value = { id: undefined, noteId: noteOptions.value[0]?.id, source: '', target: '', relationType: 'related', weightPercent: 50 }
  manualVisible.value = true
}

function editRelation(relation: Relation) {
  manualForm.value = {
    id: relation.id,
    noteId: relation.noteId,
    source: relation.source,
    target: relation.target,
    relationType: relation.relationType,
    weightPercent: Math.round(relation.weight * 100),
  }
  manualVisible.value = true
}

async function saveManualRelation() {
  if (!manualForm.value.noteId || !manualForm.value.source || !manualForm.value.target) {
    return ElMessage.warning('请完整填写关系信息')
  }
  const payload = {
    ...manualForm.value,
    weight: manualForm.value.weightPercent / 100,
  }
  if (manualForm.value.id) await updateKnowledgeRelation(manualForm.value.id, payload)
  else await createKnowledgeRelation(payload)
  manualVisible.value = false
  await loadRelations()
  ElMessage.success('关系已添加')
}

async function openRecommendations() {
  const res: any = await getRelationRecommendations()
  recommendations.value = res.data || []
  recommendVisible.value = true
}

async function acceptRecommendation(item: any) {
  await createKnowledgeRelation(item)
  recommendations.value = recommendations.value.filter((r: any) => r !== item)
  await loadRelations()
}

// 渲染关系图
function renderGraph() {
  if (!graphChart.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(graphChart.value)

  if (relations.value.length === 0) {
    chartInstance.setOption({
      title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#999' } },
    })
    return
  }

  const data = filteredRelations.value

  // 构建节点和边
  const nodeMap = new Map<string, { name: string; category: number; count: number }>()
  const edges: any[] = []

  data.forEach((r) => {
    if (!nodeMap.has(r.source)) {
      nodeMap.set(r.source, { name: r.source, category: getCategory(r.relationType), count: 0 })
    }
    if (!nodeMap.has(r.target)) {
      nodeMap.set(r.target, { name: r.target, category: getCategory(r.relationType), count: 0 })
    }
    nodeMap.get(r.source)!.count++
    nodeMap.get(r.target)!.count++

    edges.push({
      source: r.source,
      target: r.target,
      label: { show: true, formatter: relationLabel(r.relationType), fontSize: 10 },
      lineStyle: {
        color: edgeColor(r.relationType),
        width: Math.max(1, r.weight * 3),
        curveness: 0.2,
      },
    })
  })

  const nodes = Array.from(nodeMap.values()).map((n) => ({
    name: n.name,
    symbolSize: Math.min(60, 15 + n.count * 8),
    category: n.category,
    label: { show: true, fontSize: 12 },
  }))

  const categories = [
    { name: '基础', itemStyle: { color: '#315EFB' } },
    { name: '扩展', itemStyle: { color: '#FF6D4A' } },
    { name: '前置', itemStyle: { color: '#CF563F' } },
    { name: '矛盾', itemStyle: { color: '#D49A1D' } },
  ]

  chartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.dataType === 'edge') {
          return `${params.data.source} → ${params.data.target}<br/>${params.data.label?.formatter || ''}`
        }
        return `${params.name}<br/>关联数: ${params.data.symbolSize ? Math.round((params.data.symbolSize - 15) / 8) : 0}<br/><span style="color:#315EFB;font-size:12px">点击查看详情</span>`
      },
    },
    legend: { show: false },
    series: [
      {
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        categories,
        data: nodes,
        edges,
        label: {
          show: true,
          position: 'right',
          fontSize: 12,
          color: '#333',
        },
        labelLayout: { hideOverlap: true },
        force: {
          repulsion: 300,
          edgeLength: [80, 200],
          gravity: 0.1,
          friction: 0.6,
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 4 },
        },
      },
    ],
  })

  // 绑定节点点击事件
  chartInstance.off('click')
  chartInstance.on('click', (params: any) => {
    if (params.dataType === 'node') {
      openConceptDetail(params.name)
    }
  })

  // 窗口大小变化时自适应
  window.addEventListener('resize', handleResize)
}

function handleResize() {
  chartInstance?.resize()
}

function getCategory(type: string): number {
  const map: Record<string, number> = {
    related: 0,
    extends: 1,
    prerequisite: 2,
    contradicts: 3,
  }
  return map[type] || 0
}

function edgeColor(type: string): string {
  const map: Record<string, string> = {
    related: '#315EFB',
    extends: '#FF6D4A',
    prerequisite: '#CF563F',
    contradicts: '#D49A1D',
  }
  return map[type] || '#94A3B8'
}

watch([searchKeyword, noteFilter, typeFilter], () => {
  if (viewMode.value === 'graph') {
    renderGraph()
  }
})

onMounted(async () => {
  const res: any = await getNotes({ page: 1, size: 200 })
  noteOptions.value = res.data?.records || []
  loadRelations()
})
</script>

<style scoped>
.graph-page { display: flex; flex-direction: column; gap: var(--space-4); }

.stats-row { margin-bottom: 0; }
.mini-stat {
  text-align: left;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  position: relative; overflow: hidden; background: rgba(252,251,247,.9);
}
.mini-stat::after { content: ''; position: absolute; right: -16px; bottom: -16px; width: 58px; height: 58px; border: 1px solid var(--brand-200); border-radius: 50%; }
.mini-stat .stat-val { font-family: var(--font-display); font-size: 28px; font-weight: 700; color: var(--brand-600); }
.mini-stat .stat-label { font-size: var(--text-xs); color: var(--text-secondary); margin-top: 4px; }

.toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: var(--space-3); }
.toolbar-left { display: flex; gap: var(--space-2); align-items: center; }
.toolbar-right { display: flex; gap: var(--space-3); align-items: center; }

.graph-card { border-radius: var(--radius-lg); border: 1px solid var(--border-light); background-image: radial-gradient(rgba(49,94,251,.1) .8px,transparent .8px) !important; background-size: 20px 20px !important; }
.graph-container { width: 100%; height: calc(100vh - 340px); min-height: 400px; }

.empty-state { display: flex; flex-direction: column; align-items: center; padding: 60px 0; color: var(--text-tertiary); }
.empty-state p { margin-top: var(--space-4); font-size: var(--text-base); color: var(--text-secondary); }
.empty-hint { font-size: var(--text-sm) !important; color: var(--text-tertiary) !important; }

.legend { display: flex; justify-content: center; gap: var(--space-6); margin-top: var(--space-3); padding: 10px 16px; flex-wrap: wrap; border: 1px solid var(--border-light); border-radius: 999px; background: rgba(252,251,247,.7); }
.legend-item { display: flex; align-items: center; gap: var(--space-2); font-size: var(--text-sm); color: var(--text-secondary); }
.legend-dot { width: 12px; height: 12px; border-radius: 50%; }

/* 详情抽屉 */
.detail-section { margin-bottom: var(--space-2); }
.detail-title { display: flex; align-items: center; gap: var(--space-2); font-size: var(--text-base); font-weight: 600; color: var(--text-primary); margin-bottom: var(--space-3); }

.relation-list { display: flex; flex-direction: column; gap: var(--space-2); }
.relation-item {
  display: flex; align-items: center; gap: var(--space-2);
  padding: var(--space-2) var(--space-3); background: var(--bg-hover);
  border-radius: var(--radius-md); font-size: var(--text-sm);
}
.rel-source, .rel-target { color: var(--text-primary); font-weight: 500; }
.rel-tag { flex-shrink: 0; }

.concept-tags { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.concept-tag { cursor: pointer; transition: all var(--transition-fast); }
.concept-tag:hover { transform: scale(1.05); box-shadow: var(--shadow-md); }

.note-list { display: flex; flex-direction: column; gap: 4px; }
.note-item {
  display: flex; align-items: center; gap: var(--space-2);
  padding: var(--space-2) var(--space-3); border-radius: var(--radius-md);
  cursor: pointer; transition: all var(--transition-fast); border: 1px solid transparent;
}
.note-item:hover { background: var(--brand-50); border-color: var(--brand-300); }
.note-title { flex: 1; font-size: var(--text-sm); color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.note-arrow { color: var(--text-tertiary); transition: all var(--transition-fast); }
.note-item:hover .note-arrow { color: var(--brand-600); transform: translateX(4px); }
.recommend-list { display:flex; flex-direction:column; gap:12px; }
.recommend-list>div { padding:14px; border:1px solid var(--border-light); border-radius:var(--radius-md); background:var(--bg-surface); }
.recommend-list p { margin:6px 0 10px; color:var(--text-secondary); font-size:12px; }
</style>
