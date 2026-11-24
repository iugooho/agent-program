<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'

import { sendEmailCode } from '@/api/auth'
import type { CaptchaAnswer } from '@/api/captcha'
import type { ProfileUpdateRequest, UserResponse } from '@/api/schema'
import { updateCurrentUser } from '@/api/users'
import CaptchaChallenge from '@/components/CaptchaChallenge.vue'

const props = defineProps<{
  open: boolean
  user: UserResponse
}>()

const emit = defineEmits<{
  'update:open': [open: boolean]
  saved: [user: UserResponse]
}>()

const dialogRef = ref<HTMLElement | null>(null)
const usernameRef = ref<HTMLInputElement | null>(null)
const saving = ref(false)
const sendingCode = ref('')
const error = ref('')
const currentCaptcha = ref<CaptchaAnswer | null>(null)
const newCaptcha = ref<CaptchaAnswer | null>(null)
const currentCaptchaKey = ref(0)
const newCaptchaKey = ref(0)
const currentCountdown = ref(0)
const newCountdown = ref(0)
const form = reactive({
  newUsername: '',
  newEmail: '',
  newPassword: '',
  confirmPassword: '',
  verificationCode: '',
  newEmailCode: ''
})
let currentTimer: number | undefined
let newTimer: number | undefined
let previousFocus: HTMLElement | null = null
let closeAfterSave = false

const dirty = computed(() => Object.values(form).some((value) => value.trim().length > 0))

watch(
  () => props.open,
  async (open) => {
    if (open) {
      reset()
      previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
      document.body.style.overflow = 'hidden'
      await nextTick()
      usernameRef.value?.focus()
      return
    }
    document.body.style.overflow = ''
    clearTimer('current')
    clearTimer('new')
    previousFocus?.focus()
    previousFocus = null
    closeAfterSave = false
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  clearTimer('current')
  clearTimer('new')
})

async function save(): Promise<void> {
  error.value = ''
  if (form.newPassword !== form.confirmPassword) {
    error.value = '两次输入的新密码不一致'
    return
  }
  const request: ProfileUpdateRequest = {}
  if (form.newUsername.trim()) {
    request.newUsername = form.newUsername.trim()
  }
  if (form.newEmail.trim()) {
    request.newEmail = form.newEmail.trim()
  }
  if (form.newPassword) {
    request.newPassword = form.newPassword
  }
  if (!request.newUsername && !request.newEmail && !request.newPassword) {
    error.value = '请至少填写一项需要修改的账号、邮箱或密码'
    return
  }
  if (!form.verificationCode.trim()) {
    error.value = '请先完成当前邮箱验证'
    return
  }
  request.verificationCode = form.verificationCode.trim()
  if (request.newEmail) {
    if (!form.newEmailCode.trim()) {
      error.value = '请先完成新邮箱验证'
      return
    }
    request.newEmailCode = form.newEmailCode.trim()
  }

  saving.value = true
  try {
    const updated = await updateCurrentUser(request)
    closeAfterSave = true
    emit('saved', updated)
    emit('update:open', false)
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '账号安全信息更新失败'
  } finally {
    saving.value = false
  }
}

async function sendCurrentCode(): Promise<void> {
  if (!currentCaptcha.value) {
    error.value = '请先完成人机验证'
    return
  }
  sendingCode.value = 'current'
  error.value = ''
  try {
    const result = await sendEmailCode(props.user.email, 'UPDATE_PROFILE', currentCaptcha.value)
    currentCountdown.value = result.retryAfterSeconds
    startCountdown('current')
    currentCaptcha.value = null
    currentCaptchaKey.value += 1
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '验证码发送失败'
  } finally {
    sendingCode.value = ''
  }
}

async function sendNewCode(): Promise<void> {
  if (!form.newEmail.trim() || !newCaptcha.value) {
    error.value = '请填写新邮箱并完成人机验证'
    return
  }
  sendingCode.value = 'new'
  error.value = ''
  try {
    const result = await sendEmailCode(form.newEmail.trim(), 'UPDATE_PROFILE', newCaptcha.value)
    newCountdown.value = result.retryAfterSeconds
    startCountdown('new')
    newCaptcha.value = null
    newCaptchaKey.value += 1
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '验证码发送失败'
  } finally {
    sendingCode.value = ''
  }
}

function requestClose(): void {
  if (!closeAfterSave && dirty.value && !window.confirm('账号安全信息尚未保存，确认关闭吗？')) {
    return
  }
  closeAfterSave = false
  emit('update:open', false)
}

function handleOverlayClick(event: MouseEvent): void {
  if (event.target === event.currentTarget) {
    requestClose()
  }
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') {
    event.preventDefault()
    requestClose()
    return
  }
  if (event.key !== 'Tab') {
    return
  }
  const focusable = dialogRef.value?.querySelectorAll<HTMLElement>(
    'button:not(:disabled), input:not(:disabled), [href], [tabindex]:not([tabindex="-1"])'
  )
  if (!focusable || focusable.length === 0) {
    return
  }
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

function startCountdown(target: 'current' | 'new'): void {
  clearTimer(target)
  const timer = window.setInterval(() => {
    if (target === 'current') {
      currentCountdown.value -= 1
      if (currentCountdown.value <= 0) {
        clearTimer('current')
      }
    } else {
      newCountdown.value -= 1
      if (newCountdown.value <= 0) {
        clearTimer('new')
      }
    }
  }, 1000)
  if (target === 'current') {
    currentTimer = timer
  } else {
    newTimer = timer
  }
}

function clearTimer(target: 'current' | 'new'): void {
  if (target === 'current' && currentTimer !== undefined) {
    window.clearInterval(currentTimer)
    currentTimer = undefined
  }
  if (target === 'new' && newTimer !== undefined) {
    window.clearInterval(newTimer)
    newTimer = undefined
  }
}

function reset(): void {
  form.newUsername = ''
  form.newEmail = ''
  form.newPassword = ''
  form.confirmPassword = ''
  form.verificationCode = ''
  form.newEmailCode = ''
  currentCaptcha.value = null
  newCaptcha.value = null
  currentCaptchaKey.value += 1
  newCaptchaKey.value += 1
  currentCountdown.value = 0
  newCountdown.value = 0
  error.value = ''
  clearTimer('current')
  clearTimer('new')
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="dialog-overlay"
      @mousedown.self="handleOverlayClick"
      @keydown="handleKeydown"
    >
      <section
        ref="dialogRef"
        class="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="credentials-dialog-title"
      >
        <header class="dialog-header">
          <div>
            <p>账号安全</p>
            <h2 id="credentials-dialog-title">修改账号与密码</h2>
          </div>
          <button type="button" aria-label="关闭账号安全弹窗" @click="requestClose">×</button>
        </header>

        <form @submit.prevent="save">
          <div class="security-notice">
            <span>当前邮箱为</span>
            <strong>{{ user.email }}</strong>
            <span>。修改账号、密码或邮箱前，需要先完成当前邮箱验证。</span>
          </div>

          <div class="field-grid">
            <label for="credentials-username">
              新用户名
              <input
                id="credentials-username"
                ref="usernameRef"
                v-model="form.newUsername"
                maxlength="32"
                pattern="[A-Za-z0-9_]+"
                autocomplete="username"
                placeholder="留空表示不修改"
              />
            </label>
            <label for="credentials-email">
              新邮箱
              <input
                id="credentials-email"
                v-model="form.newEmail"
                type="email"
                maxlength="254"
                autocomplete="email"
                placeholder="留空表示不修改"
              />
            </label>
            <label for="credentials-password">
              新密码
              <input
                id="credentials-password"
                v-model="form.newPassword"
                type="password"
                minlength="8"
                maxlength="64"
                autocomplete="new-password"
                placeholder="留空表示不修改"
              />
            </label>
            <label for="credentials-confirm">
              确认新密码
              <input
                id="credentials-confirm"
                v-model="form.confirmPassword"
                type="password"
                minlength="8"
                maxlength="64"
                autocomplete="new-password"
                placeholder="再次输入新密码"
              />
            </label>
          </div>

          <section class="verification-card">
            <div class="verification-heading">
              <div>
                <strong>验证当前邮箱</strong>
                <small>{{ user.email }}</small>
              </div>
              <button
                type="button"
                :disabled="sendingCode === 'current' || currentCountdown > 0"
                @click="sendCurrentCode"
              >
                {{
                  currentCountdown > 0
                    ? `${currentCountdown}s`
                    : sendingCode === 'current'
                      ? '发送中…'
                      : '发送验证码'
                }}
              </button>
            </div>
            <CaptchaChallenge
              :key="currentCaptchaKey"
              purpose="EMAIL_CODE"
              :reset-key="currentCaptchaKey"
              @changed="currentCaptcha = $event"
            />
            <label for="credentials-current-code">
              当前邮箱验证码
              <input
                id="credentials-current-code"
                v-model="form.verificationCode"
                inputmode="numeric"
                maxlength="6"
                autocomplete="one-time-code"
                placeholder="6 位验证码"
              />
            </label>
          </section>

          <section v-if="form.newEmail" class="verification-card">
            <div class="verification-heading">
              <div>
                <strong>验证新邮箱</strong>
                <small>{{ form.newEmail }}</small>
              </div>
              <button
                type="button"
                :disabled="sendingCode === 'new' || newCountdown > 0"
                @click="sendNewCode"
              >
                {{
                  newCountdown > 0
                    ? `${newCountdown}s`
                    : sendingCode === 'new'
                      ? '发送中…'
                      : '发送验证码'
                }}
              </button>
            </div>
            <CaptchaChallenge
              :key="newCaptchaKey"
              purpose="EMAIL_CODE"
              :reset-key="newCaptchaKey"
              @changed="newCaptcha = $event"
            />
            <label for="credentials-new-code">
              新邮箱验证码
              <input
                id="credentials-new-code"
                v-model="form.newEmailCode"
                inputmode="numeric"
                maxlength="6"
                autocomplete="one-time-code"
                placeholder="6 位验证码"
              />
            </label>
          </section>

          <p v-if="error" class="message error" role="alert">{{ error }}</p>

          <footer class="dialog-actions">
            <button class="secondary-button" type="button" @click="requestClose">取消</button>
            <button class="primary-button" type="submit" :disabled="saving">
              {{ saving ? '更新中…' : '保存安全设置' }}
            </button>
          </footer>
        </form>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog-overlay {
  position: fixed;
  z-index: 60;
  inset: 0;
  display: grid;
  overflow: auto;
  padding: 24px;
  place-items: center;
  background: rgb(15 23 42 / 45%);
}

.dialog {
  width: min(100%, 760px);
  max-height: calc(100vh - 48px);
  overflow: auto;
  border: 1px solid #dce3ec;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 24px 70px rgb(15 23 42 / 24%);
}

.dialog-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 16px;
  border-bottom: 1px solid #edf0f3;
}

.dialog-header p {
  margin: 0 0 4px;
  color: #355f9c;
  font-size: 12px;
  font-weight: 700;
}

.dialog-header h2 {
  margin: 0;
  color: #172033;
  font-size: 20px;
}

.dialog-header button {
  display: grid;
  width: 32px;
  height: 32px;
  padding: 0;
  place-items: center;
  border: 1px solid #d6dde6;
  border-radius: 6px;
  background: #ffffff;
  color: #526071;
  font-size: 20px;
  cursor: pointer;
}

form {
  display: grid;
  gap: 16px;
  padding: 20px 22px 0;
}

.security-notice {
  padding: 11px 13px;
  border-left: 3px solid #6f94d2;
  background: #f5f8fe;
  color: #42526a;
  font-size: 12px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.security-notice strong {
  margin-inline: 3px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

label {
  display: grid;
  gap: 7px;
  color: #344054;
  font-size: 12px;
  font-weight: 700;
}

input {
  box-sizing: border-box;
  width: 100%;
  min-height: 40px;
  padding: 9px 11px;
  border: 1px solid #cfd6df;
  border-radius: 6px;
  background: #ffffff;
  color: #172033;
  font: inherit;
  font-size: 13px;
}

input:focus {
  border-color: #7b9dd7;
  box-shadow: 0 0 0 3px rgb(36 89 198 / 10%);
  outline: none;
}

.verification-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid #dfe4eb;
  border-radius: 6px;
  background: #fafbfc;
}

.verification-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.verification-heading > div {
  display: grid;
  gap: 3px;
}

.verification-heading strong {
  color: #344054;
  font-size: 13px;
}

.verification-heading small {
  color: #667085;
  font-size: 11px;
  overflow-wrap: anywhere;
}

.verification-heading button {
  flex: 0 0 auto;
  min-height: 32px;
  padding: 5px 9px;
  border: 1px solid #c8d5e8;
  border-radius: 5px;
  background: #eef4fd;
  color: #285594;
  font: inherit;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}

.dialog-actions {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: flex-end;
  gap: 9px;
  margin: 2px -22px 0;
  padding: 14px 22px 18px;
  border-top: 1px solid #edf0f3;
  background: rgb(255 255 255 / 96%);
}

.primary-button,
.secondary-button {
  min-height: 40px;
  padding: 9px 15px;
  border-radius: 6px;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.primary-button {
  border: 1px solid #2459c6;
  background: #2459c6;
  color: #ffffff;
}

.secondary-button {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #344054;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
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

@media (max-width: 700px) {
  .dialog-overlay {
    align-items: end;
    padding: 0;
  }

  .dialog {
    width: 100%;
    max-height: 94vh;
    border-radius: 8px 8px 0 0;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }

  .verification-heading {
    flex-direction: column;
  }

  .verification-heading button {
    width: 100%;
  }
}
</style>
