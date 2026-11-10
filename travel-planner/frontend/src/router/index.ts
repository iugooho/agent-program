import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { getAccessToken } from '@/auth/tokenStorage'
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
    component: () => import('@/views/DashboardView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/demo',
    name: 'demo',
    component: () => import('@/features/demo/views/DemoView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/trips',
    name: 'trips',
    component: () => import('@/views/TripPlannerView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/access',
    name: 'admin-access',
    component: () => import('@/views/AdminAccessView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !getAccessToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if ((to.name === 'login' || to.name === 'register') && getAccessToken()) {
    return { name: 'trips' }
  }
  return true
})

export default router
