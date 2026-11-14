<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { AuthApiError } from '@/api/auth'
import type { CaptchaAnswer } from '@/api/captcha'
import CaptchaChallenge from '@/components/CaptchaChallenge.vue'
import { useAuthStore } from '@/stores/auth'

type LoginMode = 'PASSWORD' | 'EMAIL_CODE'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const isDevelopment = import.meta.env.DEV

const mode = ref<LoginMode>('PASSWORD')
const submitting = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const errorMessage = ref('')
const successMessage = ref('')
const emailCaptcha = ref<CaptchaAnswer | null>(null)
const emailCaptchaKey = ref(0)
const loginCaptchaRequired = ref(false)
const loginCaptcha = ref<CaptchaAnswer | null>(null)
const loginCaptchaKey = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | undefined

const form = reactive({
  username: '',
  password: '',
  email: '',
  emailCode: ''
})

const canSendCode = computed(
  () =>
    mode.value === 'EMAIL_CODE' &&
    form.email.length > 0 &&
    emailCaptcha.value !== null &&
    countdown.value === 0 &&
    !sendingCode.value
)

const canSubmit = computed(() => !loginCaptchaRequired.value || loginCaptcha.value !== null)

function switchMode(next: LoginMode): void {
  mode.value = next
  errorMessage.value = ''
  successMessage.value = ''
}

async function submit(): Promise<void> {
  errorMessage.value = ''
  successMessage.value = ''
  if (loginCaptchaRequired.value && !loginCaptcha.value) {
    errorMessage.value = '请先完成人机验证'
    return
  }
  submitting.value = true
  try {
    if (mode.value === 'PASSWORD') {
      await auth.loginWithPassword(
        form.username,
        form.password,
        loginCaptchaRequired.value ? (loginCaptcha.value ?? undefined) : undefined
      )
    } else {
      await auth.loginWithEmailCode(form.email, form.emailCode)
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/trips'
    await router.replace(redirect)
  } catch (error: unknown) {
    if (error instanceof AuthApiError && (error.code === 1003 || error.code === 1010)) {
      loginCaptchaRequired.value = true
      loginCaptcha.value = null
      loginCaptchaKey.value += 1
      errorMessage.value =
        error.code === 1003 ? '账号或密码错误，请完成人机验证后重试' : '请完成人机验证后重试'
      return
    }
    if (error instanceof AuthApiError && error.code === 1011) {
      loginCaptcha.value = null
      loginCaptchaKey.value += 1
    }
    errorMessage.value = error instanceof Error ? error.message : '登录失败'
  } finally {
    submitting.value = false
  }
}

async function sendCode(): Promise<void> {
  errorMessage.value = ''
  successMessage.value = ''
  const captcha = emailCaptcha.value
  if (!captcha) {
    errorMessage.value = '请先完成人机验证'
    return
  }
  sendingCode.value = true
  try {
    const result = await auth.sendLoginCodeWithCaptcha(form.email, captcha)
    successMessage.value = `验证码已发送，${result.expiresIn / 60} 分钟内有效`
    startCountdown(result.retryAfterSeconds)
    emailCaptcha.value = null
    emailCaptchaKey.value += 1
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '验证码发送失败'
    if (error instanceof AuthApiError && error.code === 1011) {
      emailCaptcha.value = null
      emailCaptchaKey.value += 1
    }
  } finally {
    sendingCode.value = false
  }
}

function startCountdown(seconds: number): void {
  countdown.value = seconds
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = undefined
    }
  }, 1000)
}

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<template>
  <section class="login-shell">
    <div class="login-card">
      <div class="brand">
        <p class="eyebrow">TRAVEL PLANNER</p>
        <h1>登录旅行规划</h1>
        <p>登录后即可保存会话、复用偏好并生成完整个性化行程。</p>
      </div>

      <div class="mode-switch" role="tablist" aria-label="登录方式">
        <button
          type="button"
          :class="{ active: mode === 'PASSWORD' }"
          @click="switchMode('PASSWORD')"
        >
          账号密码
        </button>
        <button
          type="button"
          :class="{ active: mode === 'EMAIL_CODE' }"
          @click="switchMode('EMAIL_CODE')"
        >
          邮箱验证码
        </button>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <template v-if="mode === 'PASSWORD'">
          <label>
            登录账号
            <input v-model.trim="form.username" autocomplete="username" required maxlength="32" />
          </label>
          <label>
            密码
            <input
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              required
              minlength="8"
              maxlength="64"
            />
          </label>
          <div v-if="loginCaptchaRequired" class="captcha-block">
            <span class="captcha-label">人机验证</span>
            <CaptchaChallenge
              :key="loginCaptchaKey"
              purpose="LOGIN"
              :reset-key="loginCaptchaKey"
              @changed="loginCaptcha = $event"
            />
          </div>
        </template>

        <template v-else>
          <label>
            邮箱
            <input v-model.trim="form.email" type="email" autocomplete="email" required />
          </label>
          <div class="captcha-block">
            <span class="captcha-label">发送验证码前的人机验证</span>
            <CaptchaChallenge
              :key="emailCaptchaKey"
              purpose="EMAIL_CODE"
              :reset-key="emailCaptchaKey"
              @changed="emailCaptcha = $event"
            />
          </div>
          <label>
            验证码
            <span class="code-row">
              <input
                v-model.trim="form.emailCode"
                inputmode="numeric"
                autocomplete="one-time-code"
                required
                maxlength="6"
              />
              <button type="button" :disabled="!canSendCode" @click="sendCode">
                {{
                  countdown > 0 ? `${countdown}s 后重发` : sendingCode ? '发送中…' : '发送验证码'
                }}
              </button>
            </span>
          </label>
        </template>

        <p v-if="errorMessage" class="message error" role="alert">{{ errorMessage }}</p>
        <p v-if="successMessage" class="message success">{{ successMessage }}</p>

        <button class="submit" type="submit" :disabled="submitting || !canSubmit">
          {{ submitting ? '登录中…' : '登录' }}
        </button>
      </form>

      <p v-if="isDevelopment && mode === 'PASSWORD'" class="hint">
        本地普通用户：<code>demo</code> / <code>Demo123456</code>
        <br />
        本地管理员：<code>admin</code> / <code>Admin123456</code>
      </p>
      <p class="hint">
        还没有账号？
        <RouterLink to="/register">邮箱注册</RouterLink>
      </p>
    </div>
  </section>
</template>

<style scoped>
.login-shell {
  display: grid;
  min-height: calc(100vh - 130px);
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 15% 20%, rgb(54 132 255 / 18%), transparent 34%),
    radial-gradient(circle at 85% 80%, rgb(21 190 147 / 18%), transparent 30%), #f7f9fc;
}

.login-card {
  width: min(100%, 460px);
  padding: 36px;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 24px 80px rgb(15 23 42 / 12%);
}

.brand h1 {
  margin: 8px 0;
  color: #0f172a;
  font-size: 30px;
}

.brand p {
  color: #64748b;
  line-height: 1.7;
}

.eyebrow {
  margin: 0;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  margin: 24px 0;
  padding: 4px;
  border-radius: 12px;
  background: #eef2f7;
}

.mode-switch button {
  padding: 10px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
}

.mode-switch button.active {
  background: #fff;
  color: #0f172a;
  box-shadow: 0 3px 10px rgb(15 23 42 / 10%);
}

.login-form {
  display: grid;
  gap: 18px;
}

.captcha-block {
  display: grid;
  gap: 8px;
}

.captcha-label {
  color: #334155;
  font-size: 14px;
  font-weight: 600;
}

label {
  display: grid;
  gap: 8px;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
}

input {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  outline: none;
  font: inherit;
}

input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgb(37 99 235 / 12%);
}

.code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.code-row button,
.submit {
  border: 0;
  border-radius: 10px;
  cursor: pointer;
  font: inherit;
}

.code-row button {
  padding: 0 14px;
  background: #e8efff;
  color: #1d4ed8;
  white-space: nowrap;
}

.code-row button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.submit {
  padding: 13px;
  background: #1d4ed8;
  color: #fff;
  font-weight: 700;
}

.submit:disabled {
  cursor: wait;
  opacity: 0.65;
}

.message {
  margin: 0;
  padding: 10px 12px;
  border-radius: 9px;
  font-size: 14px;
}

.message.error {
  background: #fff1f2;
  color: #be123c;
}

.message.success {
  background: #ecfdf5;
  color: #047857;
}

.hint {
  margin: 18px 0 0;
  color: #64748b;
  font-size: 13px;
  text-align: center;
}

code {
  padding: 2px 6px;
  border-radius: 5px;
  background: #f1f5f9;
  color: #0f172a;
}
</style>
