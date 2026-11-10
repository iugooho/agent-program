<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import type { CaptchaAnswer } from '@/api/captcha'
import CaptchaChallenge from '@/components/CaptchaChallenge.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const submitting = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const errorMessage = ref('')
const successMessage = ref('')
const captcha = ref<CaptchaAnswer | null>(null)
const captchaKey = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | undefined

const form = reactive({
  username: '',
  password: '',
  email: '',
  emailCode: ''
})

const canSendCode = computed(
  () =>
    form.email.length > 0 && captcha.value !== null && countdown.value === 0 && !sendingCode.value
)

async function submit(): Promise<void> {
  errorMessage.value = ''
  successMessage.value = ''
  submitting.value = true
  try {
    await auth.register(form.username, form.password, form.email, form.emailCode)
    await router.replace('/trips')
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '注册失败'
  } finally {
    submitting.value = false
  }
}

async function sendCode(): Promise<void> {
  errorMessage.value = ''
  successMessage.value = ''
  const currentCaptcha = captcha.value
  if (!currentCaptcha) {
    errorMessage.value = '请先完成人机验证'
    return
  }
  sendingCode.value = true
  try {
    const result = await auth.sendRegisterCode(form.email, currentCaptcha)
    successMessage.value = `验证码已发送，${result.expiresIn / 60} 分钟内有效`
    startCountdown(result.retryAfterSeconds)
    captcha.value = null
    captchaKey.value += 1
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '验证码发送失败'
    captcha.value = null
    captchaKey.value += 1
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
  <section class="register-shell">
    <div class="register-card">
      <div class="brand">
        <p class="eyebrow">TRAVEL PLANNER</p>
        <h1>创建旅行账号</h1>
        <p>验证邮箱后即可保存偏好、管理会话并生成个性化行程。</p>
      </div>

      <form class="register-form" @submit.prevent="submit">
        <label>
          登录账号
          <input
            v-model.trim="form.username"
            autocomplete="username"
            required
            minlength="4"
            maxlength="32"
            pattern="[A-Za-z0-9_]+"
            title="4-32 位字母、数字或下划线"
            placeholder="traveler_01"
          />
        </label>

        <label>
          密码
          <input
            v-model="form.password"
            type="password"
            autocomplete="new-password"
            required
            minlength="8"
            maxlength="64"
            placeholder="至少 8 位"
          />
        </label>

        <label>
          邮箱
          <input
            v-model.trim="form.email"
            type="email"
            autocomplete="email"
            required
            maxlength="254"
            placeholder="name@example.com"
          />
        </label>

        <div class="captcha-block">
          <span class="captcha-label">人机验证</span>
          <CaptchaChallenge
            :key="captchaKey"
            purpose="EMAIL_CODE"
            :reset-key="captchaKey"
            @changed="captcha = $event"
          />
        </div>

        <label>
          注册验证码
          <span class="code-row">
            <input
              v-model.trim="form.emailCode"
              inputmode="numeric"
              autocomplete="one-time-code"
              required
              pattern="[0-9]{6}"
              maxlength="6"
              placeholder="6 位验证码"
            />
            <button type="button" :disabled="!canSendCode" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s 后重发` : sendingCode ? '发送中…' : '发送验证码' }}
            </button>
          </span>
        </label>

        <p v-if="errorMessage" class="message error" role="alert">{{ errorMessage }}</p>
        <p v-if="successMessage" class="message success">{{ successMessage }}</p>

        <button class="submit" type="submit" :disabled="submitting">
          {{ submitting ? '注册中…' : '注册并登录' }}
        </button>
      </form>

      <p class="hint">
        已有账号？
        <RouterLink to="/login">返回登录</RouterLink>
      </p>
    </div>
  </section>
</template>

<style scoped>
.register-shell {
  display: grid;
  min-height: calc(100vh - 130px);
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 15% 20%, rgb(54 132 255 / 18%), transparent 34%),
    radial-gradient(circle at 85% 80%, rgb(21 190 147 / 18%), transparent 30%), #f7f9fc;
}

.register-card {
  width: min(100%, 480px);
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

.register-form {
  display: grid;
  gap: 18px;
  margin-top: 28px;
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

.hint a {
  color: #1d4ed8;
  font-weight: 600;
}
</style>
