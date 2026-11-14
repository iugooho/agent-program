<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { sendEmailCode } from '@/api/auth'
import type { CaptchaAnswer } from '@/api/captcha'
import { fetchPreferences, updateCurrentUser, updatePreferences } from '@/api/users'
import type { ProfileUpdateRequest, UserPreferencesRequest } from '@/api/schema'
import CaptchaChallenge from '@/components/CaptchaChallenge.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(true)
const savingProfile = ref(false)
const savingPreferences = ref(false)
const sendingCode = ref('')
const profileMessage = ref('')
const profileError = ref('')
const preferenceMessage = ref('')
const preferenceError = ref('')
const currentEmailCaptcha = ref<CaptchaAnswer | null>(null)
const newEmailCaptcha = ref<CaptchaAnswer | null>(null)
const currentEmailCaptchaKey = ref(0)
const newEmailCaptchaKey = ref(0)
const currentCodeCountdown = ref(0)
const newCodeCountdown = ref(0)
const showSensitive = ref(false)

const profileForm = reactive({
  nickname: '',
  phone: '',
  newUsername: '',
  newPassword: '',
  newEmail: '',
  verificationCode: '',
  newEmailCode: ''
})

const preferenceForm = reactive({
  travelStyleDefault: '',
  foodHabit: '',
  forbiddenList: '',
  accommodationPreference: '',
  budgetLevel: ''
})

onMounted(async () => {
  loading.value = true
  try {
    if (!auth.user) {
      await auth.restore()
    }
    profileForm.nickname = auth.user?.nickname ?? ''
    profileForm.phone = auth.user?.phone ?? ''
    const preferences = await fetchPreferences()
    preferenceForm.travelStyleDefault = preferences.travelStyleDefault ?? ''
    preferenceForm.foodHabit = preferences.foodHabit ?? ''
    preferenceForm.forbiddenList = preferences.forbiddenList.join('、')
    preferenceForm.accommodationPreference = preferences.accommodationPreference ?? ''
    preferenceForm.budgetLevel = preferences.budgetLevel ?? ''
  } catch (error: unknown) {
    preferenceError.value = error instanceof Error ? error.message : '个人中心加载失败'
  } finally {
    loading.value = false
  }
})

async function saveProfile(): Promise<void> {
  profileMessage.value = ''
  profileError.value = ''
  const request: ProfileUpdateRequest = {
    nickname: profileForm.nickname,
    phone: profileForm.phone
  }
  if (profileForm.newUsername.trim()) {
    request.newUsername = profileForm.newUsername.trim()
  }
  if (profileForm.newPassword) {
    request.newPassword = profileForm.newPassword
  }
  if (profileForm.newEmail.trim()) {
    request.newEmail = profileForm.newEmail.trim()
  }
  if (profileForm.verificationCode.trim()) {
    request.verificationCode = profileForm.verificationCode.trim()
  }
  if (profileForm.newEmailCode.trim()) {
    request.newEmailCode = profileForm.newEmailCode.trim()
  }
  if (
    (request.newUsername || request.newPassword || request.newEmail) &&
    !request.verificationCode
  ) {
    profileError.value = '修改账号、密码或邮箱前，请先验证当前邮箱'
    return
  }
  savingProfile.value = true
  try {
    auth.user = await updateCurrentUser(request)
    profileMessage.value = '个人资料已更新'
    profileForm.newUsername = ''
    profileForm.newPassword = ''
    profileForm.newEmail = ''
    profileForm.verificationCode = ''
    profileForm.newEmailCode = ''
    showSensitive.value = false
  } catch (error: unknown) {
    profileError.value = error instanceof Error ? error.message : '个人资料更新失败'
  } finally {
    savingProfile.value = false
  }
}

async function sendCurrentEmailCode(): Promise<void> {
  if (!auth.user?.email || !currentEmailCaptcha.value) {
    profileError.value = '请先完成人机验证'
    return
  }
  sendingCode.value = 'current'
  profileError.value = ''
  profileMessage.value = ''
  try {
    const result = await sendEmailCode(auth.user.email, 'UPDATE_PROFILE', currentEmailCaptcha.value)
    currentCodeCountdown.value = result.retryAfterSeconds
    startCountdown('current')
    currentEmailCaptcha.value = null
    currentEmailCaptchaKey.value += 1
    profileMessage.value = '验证码已发送到当前邮箱'
  } catch (error: unknown) {
    profileError.value = error instanceof Error ? error.message : '验证码发送失败'
  } finally {
    sendingCode.value = ''
  }
}

async function sendNewEmailCode(): Promise<void> {
  if (!profileForm.newEmail.trim() || !newEmailCaptcha.value) {
    profileError.value = '请填写新邮箱并完成人机验证'
    return
  }
  sendingCode.value = 'new'
  profileError.value = ''
  profileMessage.value = ''
  try {
    const result = await sendEmailCode(
      profileForm.newEmail.trim(),
      'UPDATE_PROFILE',
      newEmailCaptcha.value
    )
    newCodeCountdown.value = result.retryAfterSeconds
    startCountdown('new')
    newEmailCaptcha.value = null
    newEmailCaptchaKey.value += 1
    profileMessage.value = '验证码已发送到新邮箱'
  } catch (error: unknown) {
    profileError.value = error instanceof Error ? error.message : '验证码发送失败'
  } finally {
    sendingCode.value = ''
  }
}

async function savePreferences(): Promise<void> {
  preferenceMessage.value = ''
  preferenceError.value = ''
  const request: UserPreferencesRequest = {
    travelStyleDefault: preferenceForm.travelStyleDefault,
    foodHabit: preferenceForm.foodHabit,
    forbiddenList: preferenceForm.forbiddenList
      .split(/[、,，\n]/)
      .map((item) => item.trim())
      .filter(Boolean),
    accommodationPreference: preferenceForm.accommodationPreference,
    budgetLevel: preferenceForm.budgetLevel
  }
  savingPreferences.value = true
  try {
    const saved = await updatePreferences(request)
    preferenceForm.forbiddenList = saved.forbiddenList.join('、')
    preferenceMessage.value = '长期偏好已保存'
  } catch (error: unknown) {
    preferenceError.value = error instanceof Error ? error.message : '偏好保存失败'
  } finally {
    savingPreferences.value = false
  }
}

function startCountdown(target: 'current' | 'new'): void {
  const timer = window.setInterval(() => {
    if (target === 'current') {
      currentCodeCountdown.value -= 1
      if (currentCodeCountdown.value <= 0) {
        window.clearInterval(timer)
      }
    } else {
      newCodeCountdown.value -= 1
      if (newCodeCountdown.value <= 0) {
        window.clearInterval(timer)
      }
    }
  }, 1000)
}
</script>

<template>
  <section class="profile-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">ACCOUNT CENTER</p>
        <h1>个人中心</h1>
        <p>管理账号资料、安全信息和长期旅行偏好。</p>
      </div>
      <div class="account-summary">
        <span class="avatar">{{ auth.user?.username.slice(0, 1).toUpperCase() }}</span>
        <div>
          <strong>{{ auth.user?.nickname || auth.user?.username }}</strong>
          <small>{{ auth.user?.email }}</small>
        </div>
      </div>
    </header>

    <p v-if="loading" class="panel-message">正在加载个人资料…</p>

    <div v-else class="profile-grid">
      <section class="form-panel">
        <header>
          <div>
            <p class="panel-kicker">BASIC PROFILE</p>
            <h2>基础资料</h2>
          </div>
          <span class="switcher" @click="showSensitive = !showSensitive">
            {{ showSensitive ? '收起安全设置' : '修改账号与安全' }}
          </span>
        </header>

        <form @submit.prevent="saveProfile">
          <div class="form-grid">
            <label>
              昵称
              <input v-model="profileForm.nickname" maxlength="50" placeholder="请输入昵称" />
            </label>
            <label>
              联系电话
              <input v-model="profileForm.phone" maxlength="20" placeholder="可选" />
            </label>
          </div>

          <div v-if="showSensitive" class="sensitive-section">
            <div class="section-divider"><span>敏感资料修改需要当前邮箱验证码</span></div>
            <div class="form-grid">
              <label>
                新用户名
                <input
                  v-model="profileForm.newUsername"
                  maxlength="32"
                  pattern="[A-Za-z0-9_]+"
                  placeholder="留空表示不修改"
                />
              </label>
              <label>
                新密码
                <input
                  v-model="profileForm.newPassword"
                  type="password"
                  minlength="8"
                  maxlength="64"
                  placeholder="留空表示不修改"
                />
              </label>
            </div>

            <label>
              新邮箱
              <input
                v-model="profileForm.newEmail"
                type="email"
                maxlength="254"
                placeholder="留空表示不修改"
              />
            </label>

            <div class="verification-card">
              <div class="verification-title">
                <div>
                  <strong>当前邮箱验证</strong>
                  <small>{{ auth.user?.email }}</small>
                </div>
                <button
                  type="button"
                  :disabled="sendingCode === 'current' || currentCodeCountdown > 0"
                  @click="sendCurrentEmailCode"
                >
                  {{
                    currentCodeCountdown > 0
                      ? `${currentCodeCountdown}s`
                      : sendingCode === 'current'
                        ? '发送中…'
                        : '发送验证码'
                  }}
                </button>
              </div>
              <CaptchaChallenge
                :key="currentEmailCaptchaKey"
                purpose="EMAIL_CODE"
                :reset-key="currentEmailCaptchaKey"
                @changed="currentEmailCaptcha = $event"
              />
              <input
                v-model="profileForm.verificationCode"
                inputmode="numeric"
                maxlength="6"
                placeholder="当前邮箱 6 位验证码"
              />
            </div>

            <div v-if="profileForm.newEmail" class="verification-card">
              <div class="verification-title">
                <div>
                  <strong>新邮箱验证</strong>
                  <small>{{ profileForm.newEmail }}</small>
                </div>
                <button
                  type="button"
                  :disabled="sendingCode === 'new' || newCodeCountdown > 0"
                  @click="sendNewEmailCode"
                >
                  {{
                    newCodeCountdown > 0
                      ? `${newCodeCountdown}s`
                      : sendingCode === 'new'
                        ? '发送中…'
                        : '发送验证码'
                  }}
                </button>
              </div>
              <CaptchaChallenge
                :key="newEmailCaptchaKey"
                purpose="EMAIL_CODE"
                :reset-key="newEmailCaptchaKey"
                @changed="newEmailCaptcha = $event"
              />
              <input
                v-model="profileForm.newEmailCode"
                inputmode="numeric"
                maxlength="6"
                placeholder="新邮箱 6 位验证码"
              />
            </div>
          </div>

          <p v-if="profileError" class="message error">{{ profileError }}</p>
          <p v-if="profileMessage" class="message success">{{ profileMessage }}</p>
          <button class="primary-button" type="submit" :disabled="savingProfile">
            {{ savingProfile ? '保存中…' : '保存个人资料' }}
          </button>
        </form>
      </section>

      <section class="form-panel">
        <header>
          <div>
            <p class="panel-kicker">TRAVEL PREFERENCES</p>
            <h2>长期旅行偏好</h2>
          </div>
        </header>

        <form @submit.prevent="savePreferences">
          <div class="form-grid">
            <label>
              默认出行风格
              <input
                v-model="preferenceForm.travelStyleDefault"
                maxlength="50"
                placeholder="如 RELAXED、FAMILY"
              />
            </label>
            <label>
              预算等级
              <select v-model="preferenceForm.budgetLevel">
                <option value="">未设置</option>
                <option value="ECONOMY">经济</option>
                <option value="COMFORT">舒适</option>
                <option value="LUXURY">高端</option>
              </select>
            </label>
          </div>
          <label>
            饮食偏好
            <input
              v-model="preferenceForm.foodHabit"
              maxlength="200"
              placeholder="如 清淡、喜欢当地特色"
            />
          </label>
          <label>
            禁忌清单
            <textarea
              v-model="preferenceForm.forbiddenList"
              rows="3"
              placeholder="使用逗号或顿号分隔，如 不吃辣、不购物、避免长时间步行"
            />
          </label>
          <label>
            住宿偏好
            <input
              v-model="preferenceForm.accommodationPreference"
              maxlength="200"
              placeholder="如 近地铁、含早餐、安静"
            />
          </label>
          <p v-if="preferenceError" class="message error">{{ preferenceError }}</p>
          <p v-if="preferenceMessage" class="message success">{{ preferenceMessage }}</p>
          <button class="primary-button" type="submit" :disabled="savingPreferences">
            {{ savingPreferences ? '保存中…' : '保存长期偏好' }}
          </button>
        </form>
      </section>
    </div>
  </section>
</template>

<style scoped>
.profile-page {
  display: grid;
  gap: 24px;
  max-width: 1180px;
  margin: 0 auto;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 22px;
  border-bottom: 1px solid #e1e5eb;
}

.page-heading h1,
.page-heading p {
  margin: 0;
}

.page-heading h1 {
  margin-top: 8px;
  color: #172033;
  font-size: 30px;
  letter-spacing: -0.04em;
}

.page-heading > div:first-child > p:last-child {
  margin-top: 8px;
  color: #75808e;
  font-size: 13px;
}

.eyebrow,
.panel-kicker {
  color: #2563eb;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.13em;
}

.account-summary {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 250px;
  padding: 12px 15px;
  border: 1px solid #e1e5eb;
  border-radius: 9px;
  background: #fff;
}

.avatar {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 8px;
  background: #e8f0ff;
  color: #2563eb;
  font-weight: 800;
}

.account-summary > div {
  display: grid;
  gap: 2px;
}

.account-summary strong {
  color: #27364d;
  font-size: 12px;
}

.account-summary small {
  color: #8993a1;
  font-size: 10px;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  align-items: start;
}

.form-panel {
  padding: 22px;
  border: 1px solid #e1e5eb;
  border-radius: 11px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(16 24 40 / 3%);
}

.form-panel > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf0f3;
}

.panel-kicker {
  margin: 0 0 5px;
}

.form-panel h2 {
  margin: 0;
  color: #263148;
  font-size: 17px;
}

.switcher {
  color: #3264bd;
  font-size: 10px;
  font-weight: 650;
  cursor: pointer;
}

form {
  display: grid;
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

label {
  display: grid;
  gap: 7px;
  color: #4c5565;
  font-size: 10px;
  font-weight: 650;
}

input,
select,
textarea {
  width: 100%;
  padding: 10px 11px;
  border: 1px solid #d9dee6;
  border-radius: 6px;
  outline: none;
  background: #fff;
  color: #253044;
  font-size: 11px;
}

textarea {
  resize: vertical;
  line-height: 1.6;
}

input:focus,
select:focus,
textarea:focus {
  border-color: #6b91d8;
  box-shadow: 0 0 0 2px rgb(37 99 235 / 7%);
}

.sensitive-section {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid #e6e9ee;
  border-radius: 8px;
  background: #fafbfc;
}

.section-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #8b94a1;
  font-size: 9px;
}

.section-divider::before,
.section-divider::after {
  flex: 1;
  height: 1px;
  background: #e4e8ed;
  content: '';
}

.verification-card {
  display: grid;
  gap: 10px;
  padding: 13px;
  border: 1px solid #e1e5eb;
  border-radius: 7px;
  background: #fff;
}

.verification-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.verification-title > div {
  display: grid;
  gap: 2px;
}

.verification-title strong {
  color: #38445a;
  font-size: 10px;
}

.verification-title small {
  color: #8c95a2;
  font-size: 9px;
}

.verification-title button {
  padding: 7px 10px;
  border: 1px solid #cfdbef;
  border-radius: 5px;
  background: #f2f6fd;
  color: #315fae;
  font-size: 9px;
  cursor: pointer;
}

.verification-title button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.primary-button {
  justify-self: start;
  min-width: 130px;
  padding: 10px 16px;
  border: 1px solid #2459c6;
  border-radius: 7px;
  background: #2459c6;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}

.primary-button:disabled {
  cursor: wait;
  opacity: 0.6;
}

.message,
.panel-message {
  margin: 0;
  padding: 9px 11px;
  border-radius: 6px;
  font-size: 10px;
}

.message.error {
  border: 1px solid #f2c5c9;
  background: #fff6f7;
  color: #a1323d;
}

.message.success {
  border: 1px solid #bfe1c8;
  background: #f5fbf7;
  color: #286541;
}

.panel-message {
  border: 1px solid #e1e5eb;
  background: #fff;
  color: #667181;
}
</style>
