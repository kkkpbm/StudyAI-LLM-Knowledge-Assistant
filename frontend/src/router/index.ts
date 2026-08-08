import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login/Index.vue'),
    meta: { guest: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard/Index.vue'),
        meta: { title: '学习总览' },
      },
      {
        path: 'notes',
        name: 'Notes',
        component: () => import('@/views/Notes/Index.vue'),
        meta: { title: '知识笔记' },
      },
      {
        path: 'notes/:id',
        name: 'NoteEditor',
        component: () => import('@/views/NoteEditor/Index.vue'),
        meta: { title: '编辑笔记' },
      },
      {
        path: 'graph',
        name: 'KnowledgeGraph',
        component: () => import('@/views/KnowledgeGraph/Index.vue'),
        meta: { title: '关系图谱' },
      },
      {
        path: 'plans',
        name: 'LearningPlans',
        component: () => import('@/views/LearningPlans/Index.vue'),
        meta: { title: '学习计划' },
      },
      {
        path: 'ai',
        name: 'AiAssistant',
        component: () => import('@/views/AiAssistant/Index.vue'),
        meta: { title: 'AI 助手' },
      },
      {
        path: 'learning-center',
        name: 'LearningCenter',
        component: () => import('@/views/LearningCenter/Index.vue'),
        meta: { title: '学习中心' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile/Index.vue'),
        meta: { title: '个人中心' },
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/Categories/Index.vue'),
        meta: { title: '分类管理' },
      },
      {
        path: 'tags',
        name: 'Tags',
        component: () => import('@/views/Tags/Index.vue'),
        meta: { title: '标签管理' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  if (to.meta.guest) {
    next()
  } else if (!userStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router
