<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  deleteAccount,
  exportUserData,
  fetchNotificationPreferences,
  updateNotificationPreferences
} from '@/api/account'
import { sendEmailCode } from '@/api/auth'
import type { CaptchaAnswer } from '@/api/captcha'
import CaptchaChallenge from '@/components/CaptchaChallenge.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const exporting = ref(false)
const sendingCode = ref(false)
const deleting = ref(false)
const showDeletion = ref(false)
const message = ref('')
const error = ref('')
const deleteCode = ref('')
const deleteConfirmation = ref('')
const captcha = ref<CaptchaAnswer | null>(null)
const captchaKey = ref(0)
const countdown = ref(0)
const preferences = reactive({
  tripReminder: true,
  tripChange: true,
  systemMessage: true,
  emailEnabled: true
})
let countdownTimer: number | undefined

onMounted(() => {
  void loadPreferences()
})

onBeforeUnmount(() => {
  if (countdownTimer !== undefined) {
    window.clearInterval(countdownTimer)
  }
})

async function loadPreferences(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    applyPreferences(await fetchNotificationPreferences())
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '通知偏好加载失败'
  } finally {
    loading.value = false
  }
}

async function savePreferences(): Promise<void> {
  saving.value = true
  message.value = ''
  error.value = ''
  try {
    applyPreferences(await updateNotificationPreferences({ ...preferences }))
    message.value = '通知偏好已保存'
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '通知偏好保存失败'
  } finally {
    saving.value = false
  }
}

async function exportData(): Promise<void> {
  exporting.value = true
  message.value = ''
  error.value = ''
  try {
    const data = await exportUserData()
    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: 'application/json;charset=utf-8'
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `travel-planner-data-${new Date().toISOString().slice(0, 10)}.json`
    link.click()
    URL.revokeObjectURL(url)
    message.value = '数据文件已生成'
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '数据导出失败'
  } finally {
    exporting.value = false
  }
}

async function sendDeletionCode(): Promise<void> {
  if (!captcha.value) {
    error.value = '请先完成人机验证'
    return
  }
  sendingCode.value = true
  error.value = ''
  message.value = ''
  try {
    const result = await sendEmailCode(currentEmail(), 'UPDATE_PROFILE', captcha.value)
    countdown.value = result.retryAfterSeconds
    startCountdown()
    captcha.value = null
    captchaKey.value += 1
    message.value = '验证码已发送到当前邮箱'
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '验证码发送失败'
  } finally {
    sendingCode.value = false
  }
}

async function handleDeleteAccount(): Promise<void> {
  if (deleteConfirmation.value !== '注销') {
    error.value = '请输入“注销”确认操作'
    return
  }
  if (!deleteCode.value.trim()) {
    error.value = '请输入当前邮箱验证码'
    return
  }
  if (!window.confirm('账号注销后会立即撤销全部登录会话，账号资料将脱敏且无法登录。确认继续吗？')) {
    return
  }
  deleting.value = true
  error.value = ''
  try {
    await deleteAccount(deleteCode.value.trim())
    auth.clearSession()
    await router.replace({ name: 'login', query: { deleted: '1' } })
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '账号注销失败'
  } finally {
    deleting.value = false
  }
}

function applyPreferences(value: {
  tripReminder: boolean
  tripChange: boolean
  systemMessage: boolean
  emailEnabled: boolean
}): void {
  preferences.tripReminder = value.tripReminder
  preferences.tripChange = value.tripChange
  preferences.systemMessage = value.systemMessage
  preferences.emailEnabled = value.emailEnabled
}

function startCountdown(): void {
  if (countdownTimer !== undefined) {
    window.clearInterval(countdownTimer)
  }
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer)
      countdownTimer = undefined
    }
  }, 1000)
}

function currentEmail(): string {
  return auth.user?.email ?? ''
}
</script>

<template>
  <section class="notification-panel" aria-label="通知偏好与账号数据">
    <header class="panel-header">
      <div>
        <p>通知偏好</p>
        <h2>消息提醒</h2>
      </div>
      <span v-if="loading" class="muted">读取中…</span>
    </header>

    <p v-if="error" class="message error" role="alert">{{ error }}</p>
    <p v-if="message" class="message success" role="status">{{ message }}</p>

    <div class="switch-list">
      <label>
        <span>
          <strong>行程提醒</strong>
          <small>出发前接收提醒和准备事项</small>
        </span>
        <input v-model="preferences.tripReminder" type="checkbox" role="switch" />
      </label>
      <label>
        <span>
          <strong>行程变更</strong>
          <small>天气、交通或计划内容变化时通知</small>
        </span>
        <input v-model="preferences.tripChange" type="checkbox" role="switch" />
      </label>
      <label>
        <span>
          <strong>系统消息</strong>
          <small>账号安全和产品维护通知</small>
        </span>
        <input v-model="preferences.systemMessage" type="checkbox" role="switch" />
      </label>
      <label>
        <span>
          <strong>邮件通知</strong>
          <small>同时发送到当前登录邮箱</small>
        </span>
        <input v-model="preferences.emailEnabled" type="checkbox" role="switch" />
      </label>
    </div>
    <button
      class="primary-button"
      type="button"
      :disabled="loading || saving"
      @click="savePreferences"
    >
      {{ saving ? '保存中…' : '保存通知偏好' }}
    </button>

    <header class="panel-header section-header">
      <div>
        <p>数据管理</p>
        <h2>导出账号数据</h2>
      </div>
    </header>
    <div class="data-row">
      <div>
        <strong>下载 JSON 数据包</strong>
        <p>包含账号资料、旅行偏好、通知设置和本人旅行计划。</p>
      </div>
      <button type="button" :disabled="exporting" @click="exportData">
        {{ exporting ? '生成中…' : '导出数据' }}
      </button>
    </div>

    <header class="panel-header section-header danger-header">
      <div>
        <p>危险操作</p>
        <h2>注销账号</h2>
      </div>
    </header>
    <div class="danger-zone">
      <p>注销会撤销全部登录会话、删除头像，并将账号资料脱敏。该操作不可撤销。</p>
      <button v-if="!showDeletion" class="delete-button" type="button" @click="showDeletion = true">
        开始注销流程
      </button>
      <template v-else>
        <div class="verification-row">
          <div class="captcha-block">
            <CaptchaChallenge
              :key="captchaKey"
              purpose="EMAIL_CODE"
              :reset-key="captchaKey"
              @changed="captcha = $event"
            />
            <button
              type="button"
              :disabled="sendingCode || countdown > 0"
              @click="sendDeletionCode"
            >
              {{ countdown > 0 ? `${countdown}s` : sendingCode ? '发送中…' : '发送当前邮箱验证码' }}
            </button>
          </div>
          <label>
            邮箱验证码
            <input
              v-model="deleteCode"
              inputmode="numeric"
              maxlength="6"
              autocomplete="one-time-code"
              placeholder="6 位验证码"
            />
          </label>
          <label>
            输入“注销”确认
            <input v-model="deleteConfirmation" maxlength="2" placeholder="注销" />
          </label>
        </div>
        <button
          class="delete-button"
          type="button"
          :disabled="deleting || loading"
          @click="handleDeleteAccount"
        >
          {{ deleting ? '注销中…' : '永久注销账号' }}
        </button>
      </template>
    </div>
  </section>
</template>

<style scoped>
.notification-panel {
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

.switch-list {
  display: grid;
  border-top: 1px solid #edf0f3;
}

.switch-list label {
  display: flex;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid #edf0f3;
}

.switch-list label > span {
  display: grid;
  gap: 3px;
}

.switch-list strong {
  color: #344054;
  font-size: 13px;
}

.switch-list small,
.data-row p,
.danger-zone > p {
  color: #667085;
  font-size: 11px;
  line-height: 1.55;
}

.switch-list input {
  width: 42px;
  height: 24px;
  appearance: none;
  position: relative;
  flex: 0 0 auto;
  border: 0;
  border-radius: 999px;
  background: #cbd5e1;
  cursor: pointer;
  transition: background-color 160ms ease;
}

.switch-list input::before {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 1px 3px rgb(15 23 42 / 24%);
  content: '';
  transition: transform 160ms ease;
}

.switch-list input:checked {
  background: #2459c6;
}

.switch-list input:checked::before {
  transform: translateX(18px);
}

.switch-list input:focus-visible {
  outline: 3px solid rgb(36 89 198 / 20%);
  outline-offset: 2px;
}

.primary-button,
.data-row button,
.captcha-block button,
.delete-button {
  min-height: 38px;
  padding: 8px 13px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #ffffff;
  color: #344054;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.primary-button {
  justify-self: start;
  border-color: #2459c6;
  background: #2459c6;
  color: #ffffff;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.section-header {
  margin-top: 8px;
  padding-top: 18px;
  border-top: 1px solid #e1e5eb;
}

.data-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border: 1px solid #e2e7ee;
  border-radius: 6px;
  background: #fafbfc;
}

.data-row p {
  margin: 4px 0 0;
}

.danger-header p,
.danger-header h2,
.danger-zone > p {
  color: #9d2633;
}

.danger-zone {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid #f0c8cc;
  border-radius: 6px;
  background: #fff8f8;
}

.danger-zone > p {
  margin: 0;
}

.verification-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.2fr) repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-items: end;
}

.captcha-block {
  display: grid;
  gap: 8px;
}

label {
  display: grid;
  gap: 6px;
  color: #344054;
  font-size: 12px;
  font-weight: 700;
}

input {
  box-sizing: border-box;
  width: 100%;
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid #cfd6df;
  border-radius: 6px;
  background: #ffffff;
  color: #172033;
  font: inherit;
  font-size: 13px;
}

.delete-button {
  justify-self: start;
  border-color: #b42332;
  background: #b42332;
  color: #ffffff;
}

.muted {
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

.message.success {
  border: 1px solid #bfe1c8;
  background: #f5fbf7;
  color: #286541;
}

@media (max-width: 760px) {
  .verification-row {
    grid-template-columns: 1fr;
  }

  .data-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
