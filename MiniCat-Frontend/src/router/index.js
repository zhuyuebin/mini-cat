import { createRouter, createWebHashHistory } from 'vue-router'
import AppShell from '@/layout/AppShell.vue'

/**
 * P0 采用 Hash 路由：
 * 后端静态资源没有 SPA fallback，history 模式下刷新 /tables 会 404；
 * Hash 路由（/#/workspace）无需改后端即可支持刷新与深链。
 */
const routes = [
  {
    path: '/',
    component: AppShell,
    children: [
      { path: '', redirect: '/workspace' },
      { path: 'workspace', name: 'Workspace', component: () => import('@/views/WorkspaceView.vue') },
      { path: 'settings', name: 'Settings', component: () => import('@/views/SettingsView.vue') },
      { path: 'welcome', name: 'Welcome', component: () => import('@/views/WelcomeView.vue') }
    ]
  },
  // 兼容旧链接
  { path: '/app', redirect: '/workspace' },
  { path: '/connections', redirect: '/settings' },
  { path: '/query', redirect: '/workspace' },
  { path: '/tables', redirect: '/workspace' },
  { path: '/:pathMatch(.*)*', redirect: '/workspace' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
