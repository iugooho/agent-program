import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { getAccessToken } from '@/auth/tokenStorage'
import { useAuthStore } from '@/stores/auth'
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
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/access',
    name: 'admin-access',
    component: () => import('@/views/AdminAccessView.vue'),
    meta: { requiresAuth: true, permission: 'admin.console.access' }
  },
  {
    path: '/admin/rbac',
    name: 'admin-rbac',
    component: () => import('@/views/RbacAdminView.vue'),
    meta: { requiresAuth: true, permission: 'admin.rbac.manage' }
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/ForbiddenView.vue')
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

router.beforeEach(async (to) => {
  if (to.meta.requiresAuth && !getAccessToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  const auth = useAuthStore()
  if (getAccessToken() && !auth.user) {
    await auth.restore()
  }
  if (to.meta.requiresAuth && !getAccessToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  const requiredPermission = typeof to.meta.permission === 'string' ? to.meta.permission : undefined
  if (requiredPermission && !auth.can(requiredPermission)) {
    return { name: 'forbidden' }
  }
  if ((to.name === 'login' || to.name === 'register') && getAccessToken()) {
    return { name: 'trips' }
  }
  return true
})

export default router
