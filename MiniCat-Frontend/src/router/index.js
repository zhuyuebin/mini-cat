import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/app',
    component: Layout,
    redirect: '/connections',
    children: [
      {
        path: '/connections',
        name: 'Connections',
        component: () => import('@/views/Connections.vue'),
        meta: { title: '连接管理', icon: 'Connection' }
      },
      {
        path: '/query',
        name: 'Query',
        component: () => import('@/views/Query.vue'),
        meta: { title: 'SQL工作台', icon: 'Document' }
      },
      {
        path: '/tables',
        name: 'Tables',
        component: () => import('@/views/Tables.vue'),
        meta: { title: '数据表管理', icon: 'Grid' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
