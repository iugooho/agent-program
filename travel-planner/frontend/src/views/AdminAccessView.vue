<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { AdminAccessError, checkAdminAccess } from '@/api/admin'
import type { AdminAccessResponse } from '@/api/schema'
import { useAuthStore } from '@/stores/auth'

type CheckStatus = 'CHECKING' | 'ALLOWED' | 'DENIED' | 'ERROR'

const auth = useAuthStore()
const status = ref<CheckStatus>('CHECKING')
const message = ref('正在调用管理员接口…')
const result = ref<AdminAccessResponse | null>(null)

const roleLabel = computed(() => {
  if (auth.hasRole('SUPER_ADMIN')) {
    return '超级管理员'
  }
  if (auth.hasRole('ADMIN')) {
    return '管理员'
  }
  if (auth.hasRole('USER')) {
    return '普通用户'
  }
  return '未知角色'
})

async function verify(): Promise<void> {
  status.value = 'CHECKING'
  message.value = '正在调用管理员接口…'
  result.value = null
  if (!auth.user) {
    await auth.restore()
  }
  try {
    result.value = await checkAdminAccess()
    status.value = 'ALLOWED'
    message.value = '管理员接口访问成功，当前账号具备 ADMIN 权限。'
  } catch (error: unknown) {
    if (error instanceof AdminAccessError && error.status === 403) {
      status.value = 'DENIED'
      message.value = '403：当前账号已登录，但没有访问管理员接口的权限。'
      return
    }
    status.value = 'ERROR'
    message.value = error instanceof Error ? error.message : '权限校验失败'
  }
}

onMounted(() => {
  void verify()
})
</script>

<template>
  <section class="access-page">
    <div class="intro">
      <p class="eyebrow">RBAC ACCESS CHECK</p>
      <h1>管理员权限验证</h1>
      <p>页面会携带当前 JWT 请求受保护的管理员接口。普通用户得到 403，管理员可以正常进入。</p>
    </div>

    <div class="account-row">
      <div>
        <span class="label">当前账号</span>
        <strong>{{ auth.user?.username ?? '未加载' }}</strong>
      </div>
      <div>
        <span class="label">当前角色</span>
        <strong>{{ roleLabel }}</strong>
      </div>
      <code>{{ auth.user?.role ?? 'UNKNOWN' }}</code>
    </div>

    <div class="result" :class="status.toLowerCase()">
      <span class="status-mark">{{
        status === 'ALLOWED' ? '✓' : status === 'DENIED' ? '!' : '…'
      }}</span>
      <div>
        <h2>
          {{
            status === 'ALLOWED'
              ? '访问通过'
              : status === 'DENIED'
                ? '访问被拒绝'
                : status === 'ERROR'
                  ? '校验异常'
                  : '校验中'
          }}
        </h2>
        <p>{{ message }}</p>
        <p v-if="result" class="result-json">
          {{ result.username }} / {{ result.role }} / accessGranted={{ result.accessGranted }}
        </p>
      </div>
    </div>

    <button type="button" :disabled="status === 'CHECKING'" @click="verify">重新验证</button>
  </section>
</template>

<style scoped>
.access-page {
  display: grid;
  gap: 24px;
  max-width: 860px;
  margin: 0 auto;
}

.intro {
  max-width: 680px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

h1 {
  margin: 0;
  color: #0f172a;
}

.intro > p:last-child {
  color: #64748b;
  line-height: 1.75;
}

.account-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 36px;
  padding: 20px 24px;
  border: 1px solid #dbe3ef;
  background: #fff;
}

.account-row > div {
  display: grid;
  gap: 4px;
}

.label {
  color: #64748b;
  font-size: 12px;
}

.account-row code {
  margin-left: auto;
  padding: 6px 10px;
  background: #eef2f7;
  color: #334155;
}

.result {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 18px;
  align-items: start;
  min-height: 150px;
  padding: 28px;
  border: 1px solid #dbe3ef;
  background: #fff;
}

.result h2,
.result p {
  margin: 0;
}

.result p {
  margin-top: 8px;
  color: #64748b;
  line-height: 1.7;
}

.status-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 50%;
  background: #e2e8f0;
  font-size: 22px;
  font-weight: 800;
}

.result.allowed {
  border-color: #9fd5bd;
}

.result.allowed .status-mark {
  background: #dcfce7;
  color: #15803d;
}

.result.denied {
  border-color: #f2b8b5;
}

.result.denied .status-mark {
  background: #fee2e2;
  color: #b91c1c;
}

.result.error {
  border-color: #f3cf87;
}

.result-json {
  font-family: Consolas, monospace;
  font-size: 13px;
}

button {
  justify-self: start;
  padding: 10px 18px;
  border: 0;
  background: #0f172a;
  color: #fff;
  cursor: pointer;
}

button:disabled {
  cursor: wait;
  opacity: 0.6;
}
</style>
