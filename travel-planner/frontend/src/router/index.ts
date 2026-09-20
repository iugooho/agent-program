import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import HomeView from '@/views/HomeView.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/views/DashboardView.vue')
  },
  {
    path: '/demo',
    name: 'demo',
    component: () => import('@/features/demo/views/DemoView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
