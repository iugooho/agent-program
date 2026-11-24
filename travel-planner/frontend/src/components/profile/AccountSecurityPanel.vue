<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchSecurityEvents, fetchSessions, revokeSession } from '@/api/account'
import type { SecurityEventResponse, UserSessionResponse } from '@/api/schema'

const sessions = ref<UserSessionResponse[]>([])
const events = ref<SecurityEventResponse[]>([])
const loading = ref(true)
const error = ref('')
const revoking = ref('')

onMounted(() => {
  void loadSecurity()
})

async function loadSecurity(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const [sessionResult, eventResult] = await Promise.all([fetchSessions(), fetchSecurityEvents()])
    sessions.value = sessionResult
    events.value = eventResult
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '账号安全信息加载失败'
  } finally {
    loading.value = false
  }
}

async function handleRevoke(session: UserSessionResponse): Promise<void> {
  if (!window.confirm('确认撤销这个登录会话吗？此设备需要重新登录。')) {
    return
  }
  revoking.value = session.sessionId
  error.value = ''
  try {
    await revokeSession(session.sessionId)
    await loadSecurity()
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '会话撤销失败'
  } finally {
    revoking.value = ''
  }
}

function clientLabel(userAgent: string | null | undefined): string {
  if (!userAgent) {
    return '未知设备'
  }
  const value = userAgent.toLowerCase()
  if (value.includes('edg/')) {
    return 'Microsoft Edge'
  }
  if (value.includes('chrome/')) {
    return 'Chrome'
  }
  if (value.includes('firefox/')) {
    return 'Firefox'
  }
  if (value.includes('safari/')) {
    return 'Safari'
  }
  return userAgent.length > 52 ? `${userAgent.slice(0, 52)}…` : userAgent
}

function eventLabel(value: string): string {
  const labels: Record<string, string> = {
    LOGIN: '登录',
    LOGOUT: '退出登录',
    SESSION_REVOKED: '撤销会话',
    SECURITY_PROFILE_UPDATED: '安全资料更新',
    ACCOUNT_DELETED: '账号注销'
  }
  return labels[value] ?? value
}

function formatDateTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}
</script>

<template>
  <section class="security-panel" aria-label="登录会话和安全日志">
    <header class="panel-header">
      <div>
        <p>登录设备</p>
        <h2>活跃会话</h2>
      </div>
      <button type="button" :disabled="loading" @click="loadSecurity">
        {{ loading ? '刷新中…' : '刷新' }}
      </button>
    </header>

    <p v-if="error" class="message error" role="alert">{{ error }}</p>

    <div v-if="loading && sessions.length === 0" class="empty">正在读取登录设备…</div>
    <div v-else-if="sessions.length === 0" class="empty">当前没有可显示的登录会话。</div>
    <ul v-else class="session-list">
      <li v-for="session in sessions" :key="session.sessionId">
        <div class="device-mark" aria-hidden="true">
          {{ clientLabel(session.userAgent).slice(0, 1) }}
        </div>
        <div class="session-copy">
          <div>
            <strong>{{ clientLabel(session.userAgent) }}</strong>
            <span v-if="session.current" class="current-badge">当前设备</span>
          </div>
          <p>
            {{ session.clientIp || '未知 IP' }} · 最近活跃
            {{ formatDateTime(session.lastSeenTime) }}
          </p>
          <small>登录于 {{ formatDateTime(session.createTime) }}</small>
        </div>
        <button
          v-if="!session.current"
          class="danger-button"
          type="button"
          :disabled="revoking === session.sessionId"
          @click="handleRevoke(session)"
        >
          {{ revoking === session.sessionId ? '撤销中…' : '撤销' }}
        </button>
      </li>
    </ul>

    <header class="panel-header log-header">
      <div>
        <p>安全动态</p>
        <h2>最近安全日志</h2>
      </div>
    </header>
    <div v-if="loading && events.length === 0" class="empty">正在读取安全日志…</div>
    <div v-else-if="events.length === 0" class="empty">暂时没有安全事件记录。</div>
    <ol v-else class="event-list">
      <li v-for="event in events" :key="event.id">
        <span class="event-dot" aria-hidden="true" />
        <div>
          <strong>{{ eventLabel(event.eventType) }}</strong>
          <p>{{ event.description }}</p>
          <small>{{ formatDateTime(event.createTime) }} · {{ event.clientIp || '未知 IP' }}</small>
        </div>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.security-panel {
  display: grid;
  gap: 14px;
  padding: 20px;
  border: 1px solid #e1e5eb;
  border-radius: 8px;
  background: #ffffff;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-header p {
  margin: 0 0 4px;
  color: #355f9c;
  font-size: 12px;
  font-weight: 700;
}

.panel-header h2 {
  margin: 0;
  color: #172033;
  font-size: 18px;
}

.panel-header button,
.danger-button {
  min-height: 34px;
  padding: 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 5px;
  background: #ffffff;
  color: #344054;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.danger-button {
  border-color: #efc8cc;
  background: #fff7f8;
  color: #a1323d;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.session-list,
.event-list {
  display: grid;
  margin: 0;
  padding: 0;
  list-style: none;
}

.session-list li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-top: 1px solid #edf0f3;
}

.device-mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 8px;
  background: #eef4fd;
  color: #285594;
  font-weight: 800;
}

.session-copy {
  min-width: 0;
}

.session-copy > div {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.session-copy strong {
  font-size: 13px;
}

.session-copy p,
.session-copy small,
.event-list p,
.event-list small {
  margin: 3px 0 0;
  color: #667085;
  font-size: 11px;
  overflow-wrap: anywhere;
}

.current-badge {
  padding: 2px 6px;
  border-radius: 999px;
  background: #e8f6ee;
  color: #17663c;
  font-size: 10px;
  font-weight: 700;
}

.log-header {
  margin-top: 4px;
  padding-top: 16px;
  border-top: 1px solid #e1e5eb;
}

.event-list li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  padding: 10px 0;
}

.event-dot {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 50%;
  background: #6f94d2;
}

.event-list strong {
  font-size: 12px;
}

.empty {
  padding: 16px;
  border: 1px dashed #d8dee7;
  border-radius: 6px;
  color: #667085;
  font-size: 12px;
}

.message {
  margin: 0;
  padding: 9px 11px;
  border-radius: 6px;
  font-size: 12px;
}

.message.error {
  border: 1px solid #f1c8cc;
  background: #fff7f8;
  color: #9d2633;
}

.session-list li:first-child {
  border-top: 0;
}

@media (max-width: 600px) {
  .session-list li {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .danger-button {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
