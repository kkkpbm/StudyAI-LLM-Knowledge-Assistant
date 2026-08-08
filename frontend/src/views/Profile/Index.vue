<template>
  <div class="profile-page">
    <section class="profile-hero" v-loading="profileLoading">
      <div class="identity-block">
        <el-avatar :size="74" class="user-avatar">
          <img v-if="avatarUrl" :src="avatarUrl" alt="用户头像" />
          <el-icon v-else :size="36"><User /></el-icon>
        </el-avatar>

        <div>
          <span class="profile-label">LEARNING PROFILE</span>
          <h2>{{ userStore.username }}</h2>
          <div class="user-tags">
            <el-tag type="primary" effect="dark" size="small">{{ roleLabel }}</el-tag>
            <el-tag v-if="stats.streak > 0" type="warning" effect="plain" size="small">
              连续 {{ stats.streak }} 天学习
            </el-tag>
          </div>
          <div class="user-extras">
            <span v-if="profile.email">
              <el-icon><Message /></el-icon>
              {{ profile.email }}
            </span>
            <span>
              <el-icon><Calendar /></el-icon>
              {{ profile.createdAt ? formatDate(profile.createdAt) : '--' }}
            </span>
          </div>
        </div>
      </div>

      <div class="profile-actions">
        <el-button type="primary" @click="openEditDialog">
          <el-icon><Edit /></el-icon>
          编辑资料
        </el-button>
        <el-button @click="showPasswordDialog = true">
          <el-icon><Lock /></el-icon>
          修改密码
        </el-button>
      </div>
    </section>

    <section class="stats-row">
      <article v-for="(s, i) in statCards" :key="s.label" class="mini-stat">
        <span class="stat-index">{{ String(i + 1).padStart(2, '0') }}</span>
        <strong :style="{ color: s.color }">{{ s.value }}</strong>
        <span>{{ s.label }}</span>
      </article>
    </section>

    <section class="profile-grid">
      <div class="panel panel-wide">
        <div class="panel-header">
          <div>
            <h3>本周学习时长</h3>
            <p>按天统计学习分钟数</p>
          </div>
          <span>分钟</span>
        </div>
        <div ref="weeklyChart" class="chart-container"></div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <div>
            <h3>知识分布</h3>
            <p>各分类笔记数量</p>
          </div>
        </div>
        <div v-if="radarData.categories.length > 0" ref="radarChart" class="chart-container compact"></div>
        <el-empty v-else description="暂无分类数据" :image-size="72" />
      </div>
    </section>

    <el-dialog v-model="showEditDialog" title="编辑个人资料" width="460px" destroy-on-close>
      <el-form :model="editForm" label-width="82px">
        <el-form-item label="头像">
          <div class="avatar-editor">
            <el-avatar :size="64" class="avatar-preview">
              <img v-if="editForm.avatar" :src="editForm.avatar" alt="用户头像预览" />
              <el-icon v-else :size="30"><User /></el-icon>
            </el-avatar>
            <div class="avatar-editor-main">
              <label class="avatar-picker" :class="{ disabled: avatarUploading }">
                <input type="file" accept="image/*" :disabled="avatarUploading" @change="handleAvatarChange" />
                {{ avatarUploading ? '上传中...' : '选择头像' }}
              </label>
              <span class="avatar-tip">支持 JPG、PNG、WebP，建议不超过 2MB</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateProfile" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showPasswordDialog" title="修改密码" width="440px" destroy-on-close>
      <el-form :model="passwordForm" label-width="100px" ref="passwordFormRef">
        <el-form-item label="原密码" prop="oldPassword" :rules="[{ required: true, message: '请输入原密码' }]">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item
          label="新密码"
          prop="newPassword"
          :rules="[
            { required: true, message: '请输入新密码' },
            { min: 6, message: '密码至少6位' }
          ]"
        >
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item
          label="确认密码"
          prop="confirmPassword"
          :rules="[
            { required: true, message: '请确认新密码' },
            { validator: validateConfirm, trigger: 'blur' }
          ]"
        >
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword" :loading="savingPwd">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Edit, Message, Calendar, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import * as echarts from 'echarts'
import {
  getProfile,
  updateProfile,
  uploadAvatar,
  changePassword,
  getLearningStats,
  getWeeklyActivity,
  getCategoryDistribution,
} from '@/api/user'

const userStore = useUserStore()

const profileLoading = ref(false)
const profile = ref<Record<string, any>>({})
const roleLabel = computed(() => (profile.value.role === 'ADMIN' ? '管理员' : '普通用户'))
const avatarUrl = computed(() => profile.value.avatar || userStore.avatar)
const avatarUploading = ref(false)

const stats = reactive({
  noteCount: 0,
  planCount: 0,
  totalMinutes: 0,
  conceptCount: 0,
  streak: 0,
})

const statCards = computed(() => [
  { label: '知识笔记', value: stats.noteCount, color: '#315EFB' },
  { label: '学习计划', value: stats.planCount, color: '#FF6D4A' },
  { label: '学习时长(h)', value: (stats.totalMinutes / 60).toFixed(1), color: '#111827' },
  { label: '知识节点', value: stats.conceptCount, color: '#18A058' },
  { label: '连续天数', value: stats.streak, color: '#E29B19' },
])

const radarData = reactive({ categories: [] as string[], values: [] as number[] })

const weeklyChart = ref<HTMLElement>()
const radarChart = ref<HTMLElement>()
let weeklyChartInstance: echarts.ECharts | null = null
let radarChartInstance: echarts.ECharts | null = null

const showEditDialog = ref(false)
const saving = ref(false)
const editForm = reactive({ email: '', avatar: '' })

const showPasswordDialog = ref(false)
const savingPwd = ref(false)
const passwordFormRef = ref()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

function validateConfirm(_rule: any, value: string, callback: any) {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return '--'
  return dateStr.substring(0, 10)
}

function openEditDialog() {
  editForm.email = profile.value.email || ''
  editForm.avatar = profile.value.avatar || userStore.avatar || ''
  showEditDialog.value = true
}

function resolveAvatarUrl(response: any) {
  const data = response?.data ?? response
  if (typeof data === 'string') return data
  return data?.avatar || data?.avatarUrl || data?.url || data?.path || ''
}

async function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 2MB')
    return
  }

  avatarUploading.value = true
  try {
    const res: any = await uploadAvatar(file)
    const avatar = resolveAvatarUrl(res)
    if (!avatar) {
      ElMessage.warning('头像上传成功，但接口未返回图片地址')
      return
    }
    editForm.avatar = avatar
    profile.value.avatar = avatar
    userStore.setProfile({ avatar })
    await updateProfile({ email: editForm.email || profile.value.email, avatar })
    ElMessage.success('头像已更新')
  } finally {
    avatarUploading.value = false
  }
}

async function loadProfile() {
  profileLoading.value = true
  try {
    const res: any = await getProfile()
    profile.value = res.data || {}
    userStore.setProfile({
      email: profile.value.email,
      createdAt: profile.value.createdAt,
      avatar: profile.value.avatar,
    })
    editForm.email = profile.value.email || ''
    editForm.avatar = profile.value.avatar || ''
  } finally {
    profileLoading.value = false
  }
}

async function loadStats() {
  try {
    const res: any = await getLearningStats()
    const data = res.data || {}
    stats.noteCount = data.noteCount || 0
    stats.planCount = data.planCount || 0
    stats.totalMinutes = data.totalMinutes || 0
    stats.conceptCount = data.conceptCount || 0
    stats.streak = data.streak || 0
  } catch {
    // keep default empty state
  }
}

async function loadCategoryDistribution() {
  try {
    const res: any = await getCategoryDistribution()
    const data = res.data || {}
    radarData.categories = data.categories || []
    radarData.values = data.values || []
    await nextTick()
    renderRadarChart()
  } catch {
    // keep default empty state
  }
}

async function loadWeeklyActivity() {
  try {
    const res: any = await getWeeklyActivity()
    const data = res.data || []
    await nextTick()
    renderWeeklyChart(data)
  } catch {
    // keep default empty state
  }
}

function renderRadarChart() {
  if (!radarChart.value || radarData.categories.length === 0) return
  if (radarChartInstance) radarChartInstance.dispose()
  radarChartInstance = echarts.init(radarChart.value)

  const maxVal = Math.max(...radarData.values, 1)
  const indicator = radarData.categories.map((name) => ({ name, max: maxVal + 2 }))

  radarChartInstance.setOption({
    radar: {
      indicator,
      shape: 'circle',
      center: ['50%', '55%'],
      radius: '62%',
      axisName: { color: '#64748B', fontSize: 11 },
      splitLine: { lineStyle: { color: '#E2E8F0' } },
      splitArea: { areaStyle: { color: ['#F8FAFD', '#FFFFFF'] } },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: radarData.values,
            name: '笔记数',
            areaStyle: { color: 'rgba(49, 94, 251, 0.16)' },
          },
        ],
        lineStyle: { color: '#315EFB', width: 2 },
        itemStyle: { color: '#315EFB' },
      },
    ],
  })
  window.addEventListener('resize', () => radarChartInstance?.resize())
}

function renderWeeklyChart(data: any[]) {
  if (!weeklyChart.value) return
  if (weeklyChartInstance) weeklyChartInstance.dispose()
  weeklyChartInstance = echarts.init(weeklyChart.value)

  const days = data.map((d: any) => d.dayOfWeek)
  const minutes = data.map((d: any) => d.minutes)

  weeklyChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        return `${p.axisValue}<br/>学习时长: ${p.value} 分钟`
      },
    },
    grid: { left: 16, right: 20, top: 18, bottom: 26 },
    xAxis: {
      type: 'category',
      data: days,
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisLabel: { color: '#94A3B8', fontSize: 12 },
    },
    yAxis: {
      type: 'value',
      name: '分钟',
      splitLine: { lineStyle: { color: '#EEF2F7' } },
      axisLabel: { color: '#94A3B8', fontSize: 12 },
    },
    series: [
      {
        type: 'bar',
        data: minutes,
        barWidth: 30,
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#315EFB' },
            { offset: 1, color: '#9DAFFF' },
          ]),
        },
        emphasis: {
          itemStyle: { color: '#FF6D4A' },
        },
      },
    ],
  })
  window.addEventListener('resize', () => weeklyChartInstance?.resize())
}

async function handleUpdateProfile() {
  saving.value = true
  try {
    await updateProfile({ email: editForm.email, avatar: editForm.avatar })
    profile.value.email = editForm.email
    profile.value.avatar = editForm.avatar
    userStore.setProfile({ email: editForm.email, avatar: editForm.avatar })
    ElMessage.success('资料已更新')
    showEditDialog.value = false
    await loadProfile()
  } finally {
    saving.value = false
  }
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  savingPwd.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码已修改，请重新登录')
    showPasswordDialog.value = false
    setTimeout(() => userStore.logout(), 1500)
  } finally {
    savingPwd.value = false
  }
}

onMounted(async () => {
  await Promise.all([
    loadProfile(),
    loadStats(),
    loadCategoryDistribution(),
    loadWeeklyActivity(),
  ])
})
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  animation: fadeIn .3s ease;
}

.profile-hero {
  min-height: 178px;
  padding: clamp(22px, 3vw, 32px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
  overflow: hidden;
  position: relative;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background:
    linear-gradient(120deg, rgba(255,255,255,.96), rgba(238,243,255,.9)),
    radial-gradient(circle at 92% 10%, rgba(49,94,251,.18), transparent 28%);
  box-shadow: var(--shadow-card);
}

.profile-hero::after {
  content: '';
  width: 220px;
  height: 220px;
  position: absolute;
  right: -58px;
  top: -56px;
  border: 1px solid rgba(49,94,251,.16);
  border-radius: 50%;
}

.identity-block {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--space-5);
  position: relative;
  z-index: 1;
}

.user-avatar,
.avatar-preview {
  flex-shrink: 0;
  color: #fff;
  background: linear-gradient(135deg, var(--brand-700), var(--accent-500));
  box-shadow: 0 16px 28px rgba(49,94,251,.18);
}

.user-avatar :deep(img),
.avatar-preview :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-label {
  color: var(--brand-600);
  font: 750 11px/1 var(--font-mono);
  letter-spacing: .08em;
}

.identity-block h2 {
  margin: 7px 0 8px;
  font-size: clamp(26px, 3vw, 36px);
  font-weight: 900;
  line-height: 1.1;
}

.user-tags,
.user-extras {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.user-extras {
  margin-top: 9px;
  gap: 16px;
  color: var(--text-secondary);
  font-size: 13px;
}

.user-extras span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.profile-actions {
  display: flex;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.avatar-editor {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-editor-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.avatar-picker {
  width: fit-content;
  min-height: 34px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--brand-700);
  font-size: 13px;
  font-weight: 750;
  border: 1px solid var(--brand-200);
  border-radius: var(--radius-md);
  background: var(--brand-50);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.avatar-picker:hover {
  color: #fff;
  border-color: var(--brand-600);
  background: var(--brand-600);
}

.avatar-picker.disabled {
  cursor: not-allowed;
  opacity: .65;
}

.avatar-picker input {
  display: none;
}

.avatar-tip {
  color: var(--text-tertiary);
  font-size: 12px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-4);
}

.mini-stat {
  min-height: 118px;
  padding: 18px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
  box-shadow: var(--shadow-card);
}

.mini-stat::after {
  content: '';
  width: 64px;
  height: 64px;
  position: absolute;
  right: -22px;
  bottom: -22px;
  border: 1px solid var(--border-light);
  border-radius: 50%;
}

.stat-index {
  color: var(--text-tertiary);
  font: 750 11px/1 var(--font-mono);
}

.mini-stat strong {
  display: block;
  margin: 15px 0 4px;
  font-size: 29px;
  font-weight: 900;
  line-height: 1;
}

.mini-stat span:last-child {
  color: var(--text-secondary);
  font-size: 13px;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, .8fr);
  gap: var(--space-4);
}

.panel {
  min-width: 0;
  padding: 20px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
  box-shadow: var(--shadow-card);
}

.panel-header {
  margin-bottom: 12px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-header h3 {
  font-size: 17px;
  font-weight: 850;
}

.panel-header p,
.panel-header > span {
  margin-top: 2px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.chart-container {
  height: 280px;
}

.chart-container.compact {
  height: 250px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 1080px) {
  .stats-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .profile-grid { grid-template-columns: 1fr; }
}

@media (max-width: 680px) {
  .profile-hero,
  .identity-block,
  .profile-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .stats-row { grid-template-columns: 1fr; }
}
</style>
