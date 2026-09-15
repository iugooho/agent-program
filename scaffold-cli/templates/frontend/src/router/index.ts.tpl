import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import HomeView from '@/views/HomeView.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  }{{dashboardRoute}}]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
