<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

async function handleLogout(): Promise<void> {
  await auth.logout()
  await router.push('/login')
}

function handleExpired(): void {
  auth.clearSession()
  if (router.currentRoute.value.name !== 'login') {
    void router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
  }
}

function roleLabel(): string {
  if (auth.hasRole('SUPER_ADMIN')) {
    return '超级管理员'
  }
  if (auth.hasRole('ADMIN')) {
    return '管理员'
  }
  return '普通用户'
}

onMounted(() => {
  window.addEventListener('travel-planner:auth-expired', handleExpired)
  void auth.restore()
})

onBeforeUnmount(() => {
  window.removeEventListener('travel-planner:auth-expired', handleExpired)
})
</script>

<template>
  <header class="app-header">
    <strong>travel-planner</strong>
    <nav>
      <RouterLink to="/"> 首页 </RouterLink>
      <RouterLink to="/trips"> 智能规划 </RouterLink>
      <RouterLink v-if="auth.authenticated" to="/profile"> 个人中心 </RouterLink>
      <RouterLink to="/dashboard"> 数据大屏 </RouterLink>
      <RouterLink v-if="auth.can('admin.console.access')" to="/admin/access"> 权限验证 </RouterLink>
      <RouterLink v-if="auth.can('admin.rbac.manage')" to="/admin/rbac"> 权限分配 </RouterLink>
      <RouterLink to="/demo"> Demo </RouterLink>
    </nav>
    <div class="account">
      <span v-if="auth.user" class="user-summary">
        {{ auth.user.nickname || auth.user.username }}
        <span class="role-badge">{{ roleLabel() }}</span>
      </span>
      <button v-if="auth.authenticated" type="button" @click="handleLogout">退出登录</button>
      <template v-else>
        <RouterLink to="/login">登录</RouterLink>
        <RouterLink to="/register">注册</RouterLink>
      </template>
    </div>
  </header>

  <main class="app-main">
    <RouterView />
  </main>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px 24px;
  border-bottom: 1px solid #e5e7eb;
}

nav {
  display: flex;
  gap: 16px;
}

.account {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.account button {
  cursor: pointer;
}

.user-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-badge {
  padding: 3px 7px;
  background: #eef2f7;
  color: #475569;
  font-size: 12px;
}

.app-main {
  padding: 24px;
}
</style>
