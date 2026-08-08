<template>
  <div class="plans-page">
    <div class="toolbar">
      <el-button type="primary" @click="showPlanDialog = true">
        <el-icon><Plus /></el-icon> AI 生成计划
      </el-button>
      <el-button @click="fetchPlans" :loading="loading">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </div>

    <!-- 计划列表 -->
    <el-row :gutter="20" style="margin-top: 16px">
      <el-col :xs="24" :md="12" :lg="8" v-for="plan in plans" :key="plan.id">
        <el-card shadow="hover" class="plan-card">
          <template #header>
            <div class="plan-header">
              <span class="plan-title">{{ plan.title }}</span>
              <el-tag size="small" :type="statusTag(plan.status)">
                {{ statusText(plan.status) }}
              </el-tag>
            </div>
          </template>

          <p class="plan-desc">{{ plan.description || '暂无描述' }}</p>
          <p v-if="plan.goal" class="plan-goal">目标：{{ plan.goal }}</p>

          <!-- 计划项 -->
          <div v-if="plan.items && plan.items.length > 0" class="plan-items">
            <el-divider content-position="left">学习任务</el-divider>
            <div
              v-for="item in plan.items"
              :key="item.id"
              class="plan-item"
              :class="{ completed: item.completed }"
            >
              <el-checkbox
                :model-value="item.completed"
                @change="handleComplete(plan.id!, item.id!, $event)"
                :disabled="plan.status !== 1"
              />
              <span class="item-order">{{ item.orderNum }}.</span>
              <span class="item-title">{{ item.title || `任务 ${item.orderNum}` }}</span>
            </div>
          </div>

          <div class="plan-dates" v-if="plan.startDate || plan.endDate">
            <span v-if="plan.startDate">{{ plan.startDate }}</span>
            <span v-if="plan.endDate">→ {{ plan.endDate }}</span>
          </div>

          <div class="plan-actions">
            <el-button
              v-if="plan.status === 1"
              size="small"
              type="success"
              @click="handleFinish(plan.id!)"
            >
              完成
            </el-button>
            <el-button
              v-if="plan.status === 2"
              size="small"
              @click="handleReopen(plan.id!)"
            >
              重新开始
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(plan.id!)">
              删除
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 空状态 -->
    <el-empty v-if="!loading && plans.length === 0" description="暂无学习计划，点击上方按钮创建" />

    <!-- AI 生成计划对话框 -->
    <el-dialog v-model="showPlanDialog" title="AI 生成学习计划" width="500px">
      <el-input
        v-model="goal"
        placeholder="输入学习目标，例如：三个月掌握 Spring Boot 微服务开发"
        type="textarea"
        :rows="3"
      />
      <template #footer>
        <el-button @click="showPlanDialog = false">取消</el-button>
        <el-button type="primary" @click="handleGenPlan" :loading="aiLoading">
          生成计划
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { genPlan as genPlanApi } from '@/api/ai'
import {
  getPlans,
  getPlan,
  createPlan,
  updatePlan,
  deletePlan,
  addPlanItem,
  completePlanItem,
} from '@/api/plans'
import { ElMessage, ElMessageBox } from 'element-plus'

const plans = ref<any[]>([])
const loading = ref(false)
const showPlanDialog = ref(false)
const aiLoading = ref(false)
const goal = ref('')

// 状态映射
function statusText(status: number) {
  const map: Record<number, string> = { 1: '进行中', 2: '已完成', 3: '已暂停' }
  return map[status] || '未知'
}
function statusTag(status: number) {
  const map: Record<number, string> = { 1: 'warning', 2: 'success', 3: 'info' }
  return map[status] || 'info'
}

// 加载计划列表
async function fetchPlans() {
  loading.value = true
  try {
    const res: any = await getPlans()
    const list = res.data || []
    // 为每个计划加载 items
    for (const plan of list) {
      try {
        const detailRes: any = await getPlan(plan.id)
        plan.items = detailRes.data?.items || []
      } catch {
        plan.items = []
      }
    }
    plans.value = list
  } catch {
    plans.value = []
  } finally {
    loading.value = false
  }
}

// AI 生成计划
async function handleGenPlan() {
  if (!goal.value.trim()) {
    ElMessage.warning('请输入学习目标')
    return
  }
  aiLoading.value = true
  try {
    const res: any = await genPlanApi(goal.value)
    const data = res.data || res
    // 保存到后端
    const planData = {
      title: data.title || goal.value,
      description: data.description || '',
      goal: goal.value,
      status: 1,
      aiGenerated: true,
      startDate: new Date().toISOString().split('T')[0],
    }
    const createRes: any = await createPlan(planData)
    const planId = createRes.data?.id

    // 如果有 phases，创建 plan items
    if (planId && data.phases) {
      for (let i = 0; i < data.phases.length; i++) {
        const phase = data.phases[i]
        try {
          await addPlanItem(planId, {
            planId,
            title: phase.name || `阶段 ${i + 1}`,
            orderNum: i + 1,
            estimatedMinutes: (phase.estimated_days || 7) * 60,
          })
        } catch {
          // 忽略单项创建失败
        }
      }
    }

    ElMessage.success('学习计划已生成')
    showPlanDialog.value = false
    goal.value = ''
    await fetchPlans()
  } catch {
    ElMessage.error('生成失败，请稍后重试')
  } finally {
    aiLoading.value = false
  }
}

// 完成计划
async function handleFinish(id: number) {
  await updatePlan(id, { status: 2 })
  ElMessage.success('计划已完成')
  await fetchPlans()
}

// 重新开始
async function handleReopen(id: number) {
  await updatePlan(id, { status: 1 })
  ElMessage.success('已重新开始')
  await fetchPlans()
}

// 删除计划
async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该计划？', '确认', { type: 'warning' })
  await deletePlan(id)
  ElMessage.success('已删除')
  await fetchPlans()
}

// 完成计划项
async function handleComplete(planId: number, itemId: number, checked: boolean) {
  if (!checked) return
  try {
    await completePlanItem(planId, itemId)
    ElMessage.success('任务已完成')
    await fetchPlans()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  fetchPlans()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.plan-card {
  margin-bottom: 16px;
  position: relative; overflow: hidden; border-radius: var(--radius-lg);
}
.plan-card::before { content: ''; position: absolute; left: 0; top: 0; width: 44%; height: 3px; background: linear-gradient(90deg,var(--brand-500),transparent); }

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.plan-title {
  font-family: var(--font-display); font-weight: 700; font-size: 17px;
}

.plan-desc {
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.plan-goal {
  color: var(--brand-600);
  margin-bottom: 8px;
  font-size: 13px;
}

.plan-dates {
  color: var(--text-tertiary);
  font-size: 13px;
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.plan-items {
  margin: 8px 0;
}

.plan-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px; border-radius: 8px; background: rgba(240,238,230,.62);
  font-size: 14px;
}

.plan-item.completed .item-title {
  text-decoration: line-through;
  color: var(--text-tertiary);
}

.item-order {
  color: var(--text-tertiary);
  min-width: 24px;
}

.plan-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}
</style>
