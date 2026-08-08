<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ collapsed: isMobile && !menuVisible }">
      <router-link to="/dashboard" class="sidebar-brand" aria-label="返回学习总览">
        <div class="brand-mark" aria-hidden="true">
          <span class="mark-node mark-a"></span>
          <span class="mark-node mark-b"></span>
          <span class="mark-node mark-c"></span>
        </div>
        <div class="brand-copy">
          <strong>知径</strong>
          <span>Knowledge Atlas</span>
        </div>
      </router-link>

      <div class="space-card">
        <span class="space-label">当前知识空间</span>
        <strong>{{ userStore.username || '学习者' }}</strong>
      </div>

      <nav class="sidebar-nav" aria-label="主导航">
        <router-link
          v-for="(item, index) in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          @click="isMobile && (menuVisible = false)"
        >
          <span class="nav-index">{{ String(index + 1).padStart(2, '0') }}</span>
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span class="nav-label">{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button class="logout-btn" @click="handleLogout">
          <el-icon :size="17"><SwitchButton /></el-icon>
          <span>退出当前空间</span>
        </button>
      </div>
    </aside>

    <div v-if="isMobile && menuVisible" class="mobile-overlay" @click="menuVisible = false" />

    <section class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <button v-if="isMobile" class="menu-trigger" @click="menuVisible = !menuVisible" aria-label="打开菜单">
            <el-icon :size="20"><Menu /></el-icon>
          </button>
          <div class="page-heading">
            <h1>{{ route.meta.title }}</h1>
            <p>{{ pageDescription }}</p>
          </div>
        </div>
        <div class="topbar-right">
          <router-link to="/ai" class="ai-shortcut">
            <ChatDotRound class="shortcut-icon" />
            <span>询问 AI</span>
          </router-link>
          <router-link to="/profile" class="profile-link" aria-label="个人中心">
            <img v-if="userStore.avatar" :src="userStore.avatar" alt="用户头像" />
            <span v-else>{{ userStore.username?.charAt(0)?.toUpperCase() || 'U' }}</span>
          </router-link>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  Odometer, Document, Share, List, ChatDotRound, Reading,
  User, SwitchButton, Menu, Collection, PriceTag,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isMobile = ref(false)
const menuVisible = ref(false)

const navItems = [
  { path: '/dashboard', label: '学习总览', icon: Odometer },
  { path: '/notes', label: '知识笔记', icon: Document },
  { path: '/graph', label: '关系图谱', icon: Share },
  { path: '/plans', label: '学习计划', icon: List },
  { path: '/ai', label: 'AI 助手', icon: ChatDotRound },
  { path: '/learning-center', label: '学习中心', icon: Reading },
  { path: '/categories', label: '分类体系', icon: Collection },
  { path: '/tags', label: '标签索引', icon: PriceTag },
  { path: '/profile', label: '个人档案', icon: User },
]

const descriptions: Record<string, string> = {
  '/dashboard': '把笔记、计划、图谱和复习状态放在同一张学习地图里',
  '/notes': '沉淀知识片段，让每一条笔记都能被再次连接',
  '/graph': '从节点关系中观察你的知识结构',
  '/plans': '把长期目标拆成今天可以推进的一步',
  '/ai': '基于你的知识库进行问答、总结和启发',
  '/learning-center': '用主动回忆、语义检索和学习报告形成完整学习闭环',
  '/categories': '建立稳定清晰的知识分类骨架',
  '/tags': '用轻量标签索引重要线索',
  '/profile': '查看学习轨迹、积累和成长状态',
}

const pageDescription = computed(() => {
  const base = navItems.find(item => route.path === item.path || route.path.startsWith(item.path + '/'))?.path
  return descriptions[base || ''] || '专注整理此刻最重要的知识'
})

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}

function checkMobile() {
  isMobile.value = window.innerWidth < 860
  if (!isMobile.value) menuVisible.value = true
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})
onUnmounted(() => window.removeEventListener('resize', checkMobile))
</script>

<style scoped>
.app-shell {
  display: flex;
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  background: var(--bg-body);
}

.sidebar {
  width: var(--sidebar-width);
  min-width: var(--sidebar-width);
  height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 100;
  color: #E8EEF8;
  background:
    linear-gradient(180deg, rgba(75, 111, 255, .18), transparent 34%),
    linear-gradient(180deg, #10172A 0%, #0B1020 100%);
  border-right: 1px solid rgba(255,255,255,.08);
  transition: transform var(--transition-slow);
}

.sidebar-brand {
  height: 88px;
  padding: 20px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff !important;
  text-decoration: none !important;
  border-bottom: 1px solid rgba(255,255,255,.08);
}

.brand-mark {
  width: 44px;
  height: 44px;
  position: relative;
  border-radius: 14px;
  background: #315EFB;
  box-shadow: 0 14px 26px rgba(49,94,251,.28);
}

.brand-mark::before,
.brand-mark::after {
  content: '';
  position: absolute;
  height: 2px;
  border-radius: 999px;
  background: rgba(255,255,255,.78);
  transform-origin: left center;
}

.brand-mark::before { width: 23px; left: 11px; top: 17px; transform: rotate(28deg); }
.brand-mark::after { width: 21px; left: 13px; top: 29px; transform: rotate(-45deg); }
.mark-node { position: absolute; width: 8px; height: 8px; border-radius: 50%; background: #fff; box-shadow: 0 0 0 4px rgba(255,255,255,.16); }
.mark-a { left: 9px; top: 12px; }
.mark-b { right: 8px; top: 23px; }
.mark-c { left: 14px; bottom: 6px; background: var(--accent-400); }

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.05;
}

.brand-copy strong {
  font-size: 22px;
  font-weight: 850;
  letter-spacing: .05em;
}

.brand-copy span {
  margin-top: 7px;
  color: rgba(232,238,248,.56);
  font: 650 10px/1 var(--font-mono);
  letter-spacing: .06em;
  text-transform: uppercase;
}

.space-card {
  margin: 16px 14px 10px;
  padding: 13px 14px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255,255,255,.09);
  background: rgba(255,255,255,.055);
}

.space-label {
  color: rgba(232,238,248,.5);
  font-size: 11px;
}

.space-card strong {
  overflow: hidden;
  color: #fff;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-nav {
  flex: 1;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  overflow-y: auto;
}

.nav-item {
  min-height: 44px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 11px;
  color: rgba(232,238,248,.58) !important;
  text-decoration: none !important;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  transition: all var(--transition-fast);
}

.nav-index {
  width: 19px;
  color: rgba(232,238,248,.28);
  font: 650 10px/1 var(--font-mono);
}

.nav-label {
  flex: 1;
  font-size: 14px;
  font-weight: 650;
}

.nav-item:hover {
  color: #fff !important;
  background: var(--bg-sidebar-hover);
}

.nav-item.active {
  color: #fff !important;
  border-color: rgba(157,175,255,.18);
  background: var(--bg-sidebar-active);
}

.nav-item.active .nav-index {
  color: var(--accent-400);
}

.sidebar-footer {
  padding: 14px;
  border-top: 1px solid rgba(255,255,255,.08);
}

.logout-btn {
  width: 100%;
  min-height: 40px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(232,238,248,.6);
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.logout-btn:hover {
  color: #fff;
  background: rgba(255,109,74,.14);
}

.main-area {
  min-width: 0;
  min-height: 0;
  height: 100vh;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.topbar {
  min-height: 78px;
  padding: 0 clamp(20px, 3vw, 40px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  background: rgba(255,255,255,.86);
  border-bottom: 1px solid var(--border-light);
  backdrop-filter: blur(14px);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.page-heading h1 {
  font-size: clamp(24px, 2vw, 32px);
  font-weight: 850;
  line-height: 1.2;
}

.page-heading p {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  white-space: nowrap;
}

.ai-shortcut,
.profile-link {
  text-decoration: none !important;
}

.ai-shortcut {
  min-width: 88px;
  height: 42px;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 8px;
  color: var(--brand-700) !important;
  font-size: 14px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
  border: 1px solid var(--brand-200);
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 6px 16px rgba(49,94,251,.08);
  transition: all var(--transition-fast);
}

.shortcut-icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
}

.ai-shortcut span {
  display: none;
}

.ai-shortcut::after {
  content: '问 AI';
  display: inline-block;
  flex: 0 0 auto;
}

.ai-shortcut:hover {
  color: #fff !important;
  border-color: var(--brand-600);
  background: var(--brand-600);
  transform: translateY(-1px);
}

.profile-link {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  color: var(--text-primary) !important;
  font-size: 18px;
  font-weight: 850;
  border-radius: 50%;
  border: 1px solid var(--brand-200);
  background: #fff;
  box-shadow: 0 6px 16px rgba(49,94,251,.08);
}

.profile-link img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: inherit;
}

.menu-trigger {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  color: var(--text-primary);
}

.content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: clamp(20px, 2.4vw, 34px) clamp(18px, 3vw, 42px) 46px;
}

.mobile-overlay {
  position: fixed;
  inset: 0;
  z-index: 99;
  background: var(--bg-mask);
  backdrop-filter: blur(3px);
}

.page-enter-active,
.page-leave-active {
  transition: opacity .18s ease, transform .24s ease;
}
.page-enter-from { opacity: 0; transform: translateY(6px); }
.page-leave-to { opacity: 0; transform: translateY(-4px); }

@media (max-width: 1100px) {
  .page-heading p { display: none; }
}

@media (max-width: 860px) {
  .sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    width: 248px;
    min-width: 248px;
    box-shadow: var(--shadow-xl);
  }

  .sidebar.collapsed { transform: translateX(-105%); }
  .topbar { min-height: 72px; }
}

@media (max-width: 560px) {
  .ai-shortcut { min-width: 42px; width: 42px; padding: 0; justify-content: center; }
  .ai-shortcut::after { content: ''; display: none; }
  .content { padding-top: 16px; }
}
</style>
