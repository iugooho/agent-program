<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'

import { fetchPreferences, fetchProfileOverview, updatePreferences } from '@/api/users'
import type {
  UserResponse,
  UserOverviewResponse,
  UserPreferencesRequest,
  UserPreferencesResponse
} from '@/api/schema'
import AccountCredentialsDialog from '@/components/profile/AccountCredentialsDialog.vue'
import AccountSecurityPanel from '@/components/profile/AccountSecurityPanel.vue'
import NotificationPrivacyPanel from '@/components/profile/NotificationPrivacyPanel.vue'
import ProfileEditDialog from '@/components/profile/ProfileEditDialog.vue'
import { useAuthStore } from '@/stores/auth'

type TabKey = 'overview' | 'preferences' | 'account'

const auth = useAuthStore()

const tabs: Array<{ key: TabKey; label: string }> = [
  { key: 'overview', label: '概览' },
  { key: 'preferences', label: '旅行偏好' },
  { key: 'account', label: '账号与安全' }
]

const travelStyleOptions = [
  { value: 'RELAXED', label: '轻松休闲' },
  { value: 'FAMILY', label: '家庭亲子' },
  { value: 'CULTURE', label: '深度文化' },
  { value: 'FOOD', label: '美食探索' },
  { value: 'ADVENTURE', label: '自然户外' },
  { value: 'PHOTOGRAPHY', label: '摄影打卡' }
]

const budgetOptions = [
  { value: '', label: '未设置' },
  { value: 'ECONOMY', label: '经济' },
  { value: 'COMFORT', label: '舒适' },
  { value: 'LUXURY', label: '高端' }
]

const activeTab = ref<TabKey>('overview')
const loading = ref(true)
const pageError = ref('')
const tripLoading = ref(false)
const tripError = ref('')
const overview = ref<UserOverviewResponse | null>(null)
const editDialogOpen = ref(false)
const credentialsDialogOpen = ref(false)

const savingPreferences = ref(false)
const preferenceMessage = ref('')
const preferenceError = ref('')
const forbiddenTags = ref<string[]>([])
const forbiddenDraft = ref('')
const preferenceBaseline = ref('')

const preferenceForm = reactive({
  travelStyleDefault: '',
  foodHabit: '',
  accommodationPreference: '',
  budgetLevel: ''
})

const displayName = computed(() => auth.user?.nickname || auth.user?.username || '旅行者')
const avatarText = computed(() => displayName.value.trim().slice(0, 1).toUpperCase() || 'U')
const avatarUrl = computed(() => resolveAvatarUrl(auth.user?.avatarUrl))
const roleLabel = computed(() => {
  if (auth.hasRole('SUPER_ADMIN')) {
    return '超级管理员'
  }
  if (auth.hasRole('ADMIN')) {
    return '管理员'
  }
  return '普通用户'
})
const statusLabel = computed(() => {
  const labels: Record<string, string> = {
    ACTIVE: '账号正常',
    DISABLED: '账号已停用',
    DELETED: '账号已注销'
  }
  return labels[auth.user?.status ?? ''] ?? '未知状态'
})

const nextTrip = computed(() => overview.value?.nextTrip ?? null)
const recentTrips = computed(() => overview.value?.recentTrips ?? [])
const tripStats = computed(() => ({
  totalTrips: overview.value?.totalTrips ?? 0,
  upcomingTrips: overview.value?.upcomingTrips ?? 0,
  cities: overview.value?.cities ?? 0,
  totalDays: overview.value?.totalDays ?? 0
}))

const preferenceTags = computed(() => {
  const values = [
    preferenceForm.travelStyleDefault ? travelStyleLabel(preferenceForm.travelStyleDefault) : '',
    preferenceForm.budgetLevel ? budgetLabel(preferenceForm.budgetLevel) : '',
    preferenceForm.foodHabit,
    preferenceForm.accommodationPreference,
    ...forbiddenTags.value.map((tag) => `避开 ${tag}`)
  ].filter(Boolean)
  return [...new Set(values)]
})

const preferencesDirty = computed(() => preferenceSnapshot() !== preferenceBaseline.value)
const hasUnsavedChanges = computed(() => preferencesDirty.value)

onMounted(() => {
  void loadProfile()
})

onBeforeRouteLeave(() => {
  if (!hasUnsavedChanges.value) {
    return true
  }
  return window.confirm('个人中心还有未保存的修改，确认离开吗？')
})

async function loadProfile(): Promise<void> {
  loading.value = true
  pageError.value = ''
  try {
    if (!auth.user) {
      await auth.restore()
    }
    if (!auth.user) {
      throw new Error('登录状态已失效，请重新登录')
    }
    await Promise.all([loadPreferences(), loadOverview()])
  } catch (error: unknown) {
    pageError.value = errorMessage(error, '个人中心加载失败')
  } finally {
    loading.value = false
  }
}

async function loadPreferences(): Promise<void> {
  preferenceError.value = ''
  try {
    applyPreferences(await fetchPreferences())
  } catch (error: unknown) {
    preferenceError.value = errorMessage(error, '长期旅行偏好加载失败')
  }
}

async function loadOverview(): Promise<void> {
  tripLoading.value = true
  tripError.value = ''
  try {
    overview.value = await fetchProfileOverview()
  } catch (error: unknown) {
    tripError.value = errorMessage(error, '个人行程概览加载失败')
  } finally {
    tripLoading.value = false
  }
}

function applyPreferences(preferences: UserPreferencesResponse): void {
  preferenceForm.travelStyleDefault = preferences.travelStyleDefault ?? ''
  preferenceForm.foodHabit = preferences.foodHabit ?? ''
  preferenceForm.accommodationPreference = preferences.accommodationPreference ?? ''
  preferenceForm.budgetLevel = preferences.budgetLevel ?? ''
  forbiddenTags.value = [...new Set(preferences.forbiddenList.filter(Boolean))]
  forbiddenDraft.value = ''
  preferenceBaseline.value = preferenceSnapshot()
}

function handleProfileSaved(user: UserResponse): void {
  auth.user = user
}

async function savePreferences(): Promise<void> {
  preferenceMessage.value = ''
  preferenceError.value = ''
  addForbiddenTag()
  const request: UserPreferencesRequest = {
    travelStyleDefault: preferenceForm.travelStyleDefault,
    foodHabit: preferenceForm.foodHabit.trim(),
    forbiddenList: [...forbiddenTags.value],
    accommodationPreference: preferenceForm.accommodationPreference.trim(),
    budgetLevel: preferenceForm.budgetLevel
  }
  savingPreferences.value = true
  try {
    const saved = await updatePreferences(request)
    applyPreferences(saved)
    preferenceMessage.value = '长期偏好已保存'
  } catch (error: unknown) {
    preferenceError.value = errorMessage(error, '偏好保存失败')
  } finally {
    savingPreferences.value = false
  }
}

function cancelPreferenceChanges(): void {
  if (preferenceBaseline.value) {
    const baseline = JSON.parse(preferenceBaseline.value) as {
      travelStyleDefault: string
      foodHabit: string
      forbiddenList: string[]
      accommodationPreference: string
      budgetLevel: string
    }
    preferenceForm.travelStyleDefault = baseline.travelStyleDefault
    preferenceForm.foodHabit = baseline.foodHabit
    preferenceForm.accommodationPreference = baseline.accommodationPreference
    preferenceForm.budgetLevel = baseline.budgetLevel
    forbiddenTags.value = [...baseline.forbiddenList]
  }
  forbiddenDraft.value = ''
  preferenceMessage.value = ''
  preferenceError.value = ''
}

function addForbiddenTag(rawValue = forbiddenDraft.value): void {
  const values = rawValue
    .split(/[、,，\n]/)
    .map((value) => value.trim())
    .filter(Boolean)
  if (values.length > 0) {
    forbiddenTags.value = [...new Set([...forbiddenTags.value, ...values])]
  }
  forbiddenDraft.value = ''
}

function removeForbiddenTag(tag: string): void {
  forbiddenTags.value = forbiddenTags.value.filter((item) => item !== tag)
}

function handleForbiddenKeydown(event: KeyboardEvent): void {
  if (event.key === 'Enter' || event.key === ',' || event.key === '，') {
    event.preventDefault()
    addForbiddenTag()
  }
  if (event.key === 'Backspace' && !forbiddenDraft.value && forbiddenTags.value.length > 0) {
    removeForbiddenTag(forbiddenTags.value[forbiddenTags.value.length - 1])
  }
}

function selectTab(tab: TabKey): void {
  activeTab.value = tab
}

function tabHasUnsavedChanges(tab: TabKey): boolean {
  if (tab === 'preferences') {
    return preferencesDirty.value
  }
  return false
}

function handleTabKeydown(event: KeyboardEvent, index: number): void {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) {
    return
  }
  event.preventDefault()
  let nextIndex = index
  if (event.key === 'ArrowLeft') {
    nextIndex = (index - 1 + tabs.length) % tabs.length
  } else if (event.key === 'ArrowRight') {
    nextIndex = (index + 1) % tabs.length
  } else if (event.key === 'Home') {
    nextIndex = 0
  } else if (event.key === 'End') {
    nextIndex = tabs.length - 1
  }
  activeTab.value = tabs[nextIndex].key
  const tabButtons = (event.currentTarget as HTMLElement)
    .closest('[role="tablist"]')
    ?.querySelectorAll<HTMLButtonElement>('[role="tab"]')
  tabButtons?.[nextIndex]?.focus()
}

function preferenceSnapshot(): string {
  return JSON.stringify({
    travelStyleDefault: preferenceForm.travelStyleDefault,
    foodHabit: preferenceForm.foodHabit,
    forbiddenList: forbiddenTags.value,
    accommodationPreference: preferenceForm.accommodationPreference,
    budgetLevel: preferenceForm.budgetLevel
  })
}

function travelStyleLabel(value: string): string {
  return travelStyleOptions.find((option) => option.value === value)?.label ?? value ?? ''
}

function budgetLabel(value: string): string {
  return budgetOptions.find((option) => option.value === value)?.label ?? value ?? ''
}

function startOfToday(): Date {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  return date
}

function parseDateOnly(value: string): Date {
  const parsed = new Date(`${value}T00:00:00`)
  return Number.isNaN(parsed.getTime()) ? new Date(0) : parsed
}

function daysUntil(value: string): number {
  const milliseconds = parseDateOnly(value).getTime() - startOfToday().getTime()
  return Math.max(0, Math.ceil(milliseconds / 86_400_000))
}

function upcomingLabel(value: string): string {
  const days = daysUntil(value)
  return days === 0 ? '今天出发' : `${days} 天后出发`
}

function formatDate(value?: string): string {
  if (!value) {
    return '未记录'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(date)
}

function formatDateTime(value?: string): string {
  if (!value) {
    return '暂无记录'
  }
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

function resolveAvatarUrl(value?: string): string {
  if (!value) {
    return ''
  }
  if (/^(https?:|data:|blob:)/.test(value)) {
    return value
  }
  const baseUrl = import.meta.env.VITE_API_BASE_URL
  if (baseUrl && /^https?:\/\//.test(baseUrl)) {
    return new URL(value, baseUrl).toString()
  }
  return value
}

function money(value: number): string {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0
  }).format(value)
}

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}
</script>

<template>
  <section class="profile-page">
    <header class="identity-band">
      <div class="identity-avatar" :class="{ 'has-image': avatarUrl }">
        <img v-if="avatarUrl" :src="avatarUrl" :alt="`${displayName}的头像`" />
        <span v-else aria-hidden="true">{{ avatarText }}</span>
      </div>
      <div class="identity-copy">
        <p class="section-kicker">个人中心</p>
        <div class="identity-heading">
          <h1>{{ displayName }}</h1>
          <span class="status-badge" :class="{ muted: auth.user?.status !== 'ACTIVE' }">
            {{ statusLabel }}
          </span>
        </div>
        <p class="identity-email">{{ auth.user?.email || '正在读取账号信息' }}</p>
        <p class="identity-bio">{{ auth.user?.bio || '还没有填写个人简介。' }}</p>
        <div class="identity-meta" aria-label="账号摘要">
          <span>{{ roleLabel }}</span>
          <span>加入于 {{ formatDate(auth.user?.createTime) }}</span>
          <span>最近登录 {{ formatDateTime(auth.user?.lastLoginTime) }}</span>
          <span v-if="auth.user">账号编号 #{{ auth.user.id }}</span>
        </div>
      </div>
      <div class="identity-actions">
        <button class="secondary-button" type="button" @click="editDialogOpen = true">
          编辑资料
        </button>
        <RouterLink class="primary-link" to="/trips">查看行程</RouterLink>
      </div>
      <nav
        v-if="!loading && !pageError"
        class="profile-tabs"
        role="tablist"
        aria-label="个人中心分区"
      >
        <button
          v-for="(tab, index) in tabs"
          :id="`profile-tab-${tab.key}`"
          :key="tab.key"
          type="button"
          role="tab"
          :aria-selected="activeTab === tab.key"
          :aria-controls="`profile-panel-${tab.key}`"
          :tabindex="activeTab === tab.key ? 0 : -1"
          :class="{ active: activeTab === tab.key }"
          @click="selectTab(tab.key)"
          @keydown="handleTabKeydown($event, index)"
        >
          {{ tab.label }}
          <span v-if="tabHasUnsavedChanges(tab.key)" class="dirty-dot" aria-label="有未保存修改" />
        </button>
      </nav>
    </header>

    <p v-if="loading" class="state-panel" role="status">正在加载个人中心…</p>

    <section v-else-if="pageError" class="state-panel error-state" role="alert">
      <span>{{ pageError }}</span>
      <button type="button" @click="loadProfile">重新加载</button>
    </section>

    <template v-else>
      <p v-if="hasUnsavedChanges" class="unsaved-note" role="status">
        当前有未保存修改。切换分区不会丢失内容，离开页面前会再次确认。
      </p>

      <section
        v-if="activeTab === 'overview'"
        id="profile-panel-overview"
        class="tab-panel"
        role="tabpanel"
        aria-labelledby="profile-tab-overview"
        tabindex="0"
      >
        <section class="metric-band" aria-label="个人行程统计">
          <div>
            <span>累计行程</span>
            <strong>{{ tripLoading ? '…' : tripStats.totalTrips }}</strong>
          </div>
          <div>
            <span>即将出发</span>
            <strong>{{ tripLoading ? '…' : tripStats.upcomingTrips }}</strong>
          </div>
          <div>
            <span>去过城市</span>
            <strong>{{ tripLoading ? '…' : tripStats.cities }}</strong>
          </div>
          <div>
            <span>累计天数</span>
            <strong>{{ tripLoading ? '…' : tripStats.totalDays }}</strong>
          </div>
        </section>

        <p v-if="tripError" class="message error" role="alert">
          {{ tripError }}
          <button class="inline-action" type="button" @click="loadOverview">重试</button>
        </p>

        <div class="overview-grid">
          <div class="overview-main">
            <section class="surface next-trip-panel">
              <header class="surface-header">
                <div>
                  <p class="section-kicker">下一段行程</p>
                  <h2>{{ nextTrip ? nextTrip.title : '还没有即将出发的行程' }}</h2>
                </div>
                <RouterLink to="/trips">全部行程</RouterLink>
              </header>
              <template v-if="nextTrip">
                <p class="trip-route">{{ nextTrip.destinations.join('、') }}</p>
                <div class="trip-facts">
                  <span>{{ nextTrip.startDate }}</span>
                  <span>{{ nextTrip.days }} 天</span>
                  <span>{{ money(nextTrip.totalBudget) }}</span>
                </div>
                <p class="trip-countdown">{{ upcomingLabel(nextTrip.startDate) }}</p>
              </template>
              <div v-else class="empty-state">
                到智能规划页描述你的下一次旅行，生成的计划会自动出现在这里。
              </div>
            </section>

            <section class="surface">
              <header class="surface-header">
                <div>
                  <p class="section-kicker">最近行程</p>
                  <h2>旅行记录</h2>
                </div>
                <span class="subtle-count">{{ recentTrips.length }} 条</span>
              </header>
              <div v-if="tripLoading" class="empty-state">正在读取个人行程…</div>
              <div v-else-if="recentTrips.length === 0" class="empty-state">暂无个人行程记录。</div>
              <ol v-else class="trip-list">
                <li v-for="trip in recentTrips" :key="trip.id">
                  <div>
                    <strong>{{ trip.title }}</strong>
                    <span>{{ trip.destinations.join('、') }} · {{ trip.days }} 天</span>
                  </div>
                  <div class="trip-list-meta">
                    <time :datetime="trip.startDate">{{ trip.startDate }}</time>
                    <span>{{ money(trip.totalBudget) }}</span>
                  </div>
                </li>
              </ol>
            </section>
          </div>

          <aside class="overview-side">
            <section class="surface">
              <header class="surface-header">
                <div>
                  <p class="section-kicker">旅行偏好</p>
                  <h2>默认设置</h2>
                </div>
                <button class="inline-action" type="button" @click="selectTab('preferences')">
                  调整
                </button>
              </header>
              <p v-if="preferenceError" class="message error" role="alert">
                {{ preferenceError }}
                <button class="inline-action" type="button" @click="loadPreferences">重试</button>
              </p>
              <div v-else-if="preferenceTags.length > 0" class="tag-list">
                <span v-for="tag in preferenceTags" :key="tag">{{ tag }}</span>
              </div>
              <div v-else class="empty-state">还没有设置长期偏好，规划时会按本次需求生成。</div>
            </section>

            <section class="surface">
              <header class="surface-header">
                <div>
                  <p class="section-kicker">账号安全</p>
                  <h2>当前状态</h2>
                </div>
                <span class="status-badge">已启用</span>
              </header>
              <dl class="details-list">
                <div>
                  <dt>登录邮箱</dt>
                  <dd>{{ auth.user?.email }}</dd>
                </div>
                <div>
                  <dt>联系电话</dt>
                  <dd>{{ auth.user?.phone || '未填写' }}</dd>
                </div>
                <div>
                  <dt>安全验证</dt>
                  <dd>敏感修改需邮箱验证码</dd>
                </div>
              </dl>
              <button
                class="secondary-button full-width"
                type="button"
                @click="selectTab('account')"
              >
                进入安全设置
              </button>
            </section>
          </aside>
        </div>
      </section>

      <section
        v-else-if="activeTab === 'preferences'"
        id="profile-panel-preferences"
        class="tab-panel"
        role="tabpanel"
        aria-labelledby="profile-tab-preferences"
        tabindex="0"
      >
        <section class="surface form-surface">
          <header class="surface-header">
            <div>
              <p class="section-kicker">长期偏好</p>
              <h2>默认旅行方式</h2>
            </div>
            <span v-if="preferencesDirty" class="dirty-label">有修改</span>
          </header>
          <form @submit.prevent="savePreferences">
            <fieldset>
              <legend>默认出行风格</legend>
              <div class="choice-grid">
                <label
                  v-for="option in travelStyleOptions"
                  :key="option.value"
                  class="choice-chip"
                  :class="{ selected: preferenceForm.travelStyleDefault === option.value }"
                >
                  <input
                    v-model="preferenceForm.travelStyleDefault"
                    type="radio"
                    name="travel-style"
                    :value="option.value"
                  />
                  <span>{{ option.label }}</span>
                </label>
              </div>
            </fieldset>

            <fieldset>
              <legend>预算等级</legend>
              <div class="choice-grid compact">
                <label
                  v-for="option in budgetOptions"
                  :key="option.value || 'unset'"
                  class="choice-chip"
                  :class="{ selected: preferenceForm.budgetLevel === option.value }"
                >
                  <input
                    v-model="preferenceForm.budgetLevel"
                    type="radio"
                    name="budget-level"
                    :value="option.value"
                  />
                  <span>{{ option.label }}</span>
                </label>
              </div>
            </fieldset>

            <div class="field-grid">
              <label for="preference-food">
                饮食偏好
                <input
                  id="preference-food"
                  v-model="preferenceForm.foodHabit"
                  maxlength="200"
                  placeholder="如 清淡、喜欢当地特色"
                />
              </label>
              <label for="preference-accommodation">
                住宿偏好
                <input
                  id="preference-accommodation"
                  v-model="preferenceForm.accommodationPreference"
                  maxlength="200"
                  placeholder="如 近地铁、含早餐、安静"
                />
              </label>
            </div>

            <div class="tag-field">
              <label for="preference-forbidden">禁忌与避开项</label>
              <div class="tag-editor">
                <span v-for="tag in forbiddenTags" :key="tag" class="tag-pill">
                  {{ tag }}
                  <button type="button" :aria-label="`移除${tag}`" @click="removeForbiddenTag(tag)">
                    ×
                  </button>
                </span>
                <input
                  id="preference-forbidden"
                  v-model="forbiddenDraft"
                  maxlength="50"
                  placeholder="输入后按回车添加"
                  @keydown="handleForbiddenKeydown"
                  @blur="addForbiddenTag()"
                />
              </div>
              <p class="field-hint">例如“不吃辣”“不购物”“避免长时间步行”，可用逗号批量添加。</p>
            </div>

            <p v-if="preferenceError" class="message error" role="alert">{{ preferenceError }}</p>
            <p v-if="preferenceMessage" class="message success" role="status">
              {{ preferenceMessage }}
            </p>
            <div class="form-actions">
              <button class="primary-button" type="submit" :disabled="savingPreferences">
                {{ savingPreferences ? '保存中…' : '保存偏好' }}
              </button>
              <button
                class="secondary-button"
                type="button"
                :disabled="!preferencesDirty || savingPreferences"
                @click="cancelPreferenceChanges"
              >
                取消修改
              </button>
            </div>
          </form>
        </section>
      </section>

      <section
        v-else
        id="profile-panel-account"
        class="tab-panel"
        role="tabpanel"
        aria-labelledby="profile-tab-account"
        tabindex="0"
      >
        <section class="surface credential-summary">
          <header class="surface-header">
            <div>
              <p class="section-kicker">账号安全</p>
              <h2>账号凭证</h2>
            </div>
            <span class="status-badge">受保护</span>
          </header>
          <dl class="details-list">
            <div>
              <dt>登录账号</dt>
              <dd>{{ auth.user?.username }}</dd>
            </div>
            <div>
              <dt>登录邮箱</dt>
              <dd>{{ auth.user?.email }}</dd>
            </div>
            <div>
              <dt>密码与邮箱</dt>
              <dd>修改时需完成当前邮箱验证</dd>
            </div>
          </dl>
          <button
            class="secondary-button full-width"
            type="button"
            @click="credentialsDialogOpen = true"
          >
            修改账号与密码
          </button>
        </section>
        <AccountSecurityPanel />
        <NotificationPrivacyPanel />
      </section>
    </template>
    <ProfileEditDialog
      v-if="auth.user"
      v-model:open="editDialogOpen"
      :user="auth.user"
      @saved="handleProfileSaved"
    />
    <AccountCredentialsDialog
      v-if="auth.user"
      v-model:open="credentialsDialogOpen"
      :user="auth.user"
      @saved="handleProfileSaved"
    />
  </section>
</template>

<style scoped>
.profile-page {
  --ink: #172033;
  --muted: #667085;
  --line: #e1e5eb;
  --soft: #f7f9fc;
  --primary: #2459c6;
  --primary-dark: #1d47a1;
  display: grid;
  gap: 18px;
  max-width: 1180px;
  margin: 0 auto;
  color: var(--ink);
}

.identity-band {
  --identity-padding: 24px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 20px;
  padding: var(--identity-padding);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--soft);
}

.identity-avatar {
  display: grid;
  width: 68px;
  height: 68px;
  overflow: hidden;
  place-items: center;
  border-radius: 50%;
  background: #23395d;
  color: #ffffff;
  font-size: 26px;
  font-weight: 800;
}

.identity-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.identity-copy {
  min-width: 0;
}

.section-kicker {
  margin: 0 0 5px;
  color: #355f9c;
  font-size: 12px;
  font-weight: 700;
}

.identity-heading {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.identity-heading h1 {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
  letter-spacing: 0;
}

.identity-email {
  margin: 5px 0 4px;
  color: var(--muted);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.identity-bio {
  max-width: 720px;
  margin: 0 0 9px;
  color: #3f4c5f;
  font-size: 13px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.identity-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: #536173;
  font-size: 12px;
}

.identity-meta span {
  position: relative;
}

.identity-meta span + span::before {
  position: absolute;
  left: -9px;
  color: #a6afba;
  content: '·';
}

.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 3px 9px;
  border-radius: 999px;
  background: #e8f6ee;
  color: #17663c;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.status-badge.muted {
  background: #f1f3f5;
  color: #667085;
}

.identity-actions {
  display: flex;
  align-items: center;
  gap: 9px;
}

button,
.primary-link {
  box-sizing: border-box;
  min-height: 40px;
  padding: 9px 15px;
  border-radius: 6px;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.primary-button,
.primary-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--primary);
  background: var(--primary);
  color: #ffffff;
  text-decoration: none;
}

.primary-button:hover,
.primary-link:hover {
  border-color: var(--primary-dark);
  background: var(--primary-dark);
}

.secondary-button {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #344054;
}

.secondary-button:hover {
  border-color: #9aa8ba;
  background: #f8fafc;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

button:focus-visible,
.primary-link:focus-visible,
input:focus-visible,
a:focus-visible {
  outline: 3px solid rgb(36 89 198 / 22%);
  outline-offset: 2px;
}

.state-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 0;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #ffffff;
  color: var(--muted);
  font-size: 13px;
}

.state-panel.error-state {
  border-color: #f1c8cc;
  background: #fff7f8;
  color: #9d2633;
}

.state-panel button {
  min-height: 34px;
  padding: 6px 11px;
  border: 1px solid currentcolor;
  background: transparent;
  color: inherit;
}

.profile-tabs {
  display: flex;
  grid-column: 1 / -1;
  overflow-x: auto;
  gap: 22px;
  margin: 4px calc(var(--identity-padding) * -1) calc(var(--identity-padding) * -1);
  padding: 10px var(--identity-padding) 0;
  border-top: 1px solid #dce3ec;
}

.profile-tabs button {
  position: relative;
  flex: 0 0 auto;
  min-height: 36px;
  padding: 8px 0 6px;
  border: 0;
  border-bottom: 2px solid transparent;
  border-radius: 0;
  background: transparent;
  color: #526071;
  white-space: nowrap;
}

.profile-tabs button:hover {
  color: #1f4fae;
}

.profile-tabs button.active {
  border-bottom-color: #2459c6;
  color: #1f4fae;
}

.dirty-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-left: 4px;
  border-radius: 50%;
  background: #d97706;
  vertical-align: super;
}

.unsaved-note {
  margin: -4px 0 0;
  padding: 9px 12px;
  border-left: 3px solid #d97706;
  background: #fffbeb;
  color: #8a5208;
  font-size: 12px;
}

.tab-panel {
  display: grid;
  gap: 16px;
}

.tab-panel:focus {
  outline: none;
}

.metric-band {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #ffffff;
}

.metric-band > div {
  display: grid;
  gap: 6px;
  padding: 18px 20px;
}

.metric-band > div + div {
  border-left: 1px solid var(--line);
}

.metric-band span {
  color: var(--muted);
  font-size: 12px;
}

.metric-band strong {
  font-size: 26px;
  line-height: 1;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(260px, 0.85fr);
  gap: 16px;
  align-items: start;
}

.overview-main,
.overview-side {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.surface {
  min-width: 0;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #ffffff;
}

.surface-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.surface-header h2 {
  margin: 0;
  font-size: 18px;
  line-height: 1.35;
}

.surface-header a,
.inline-action {
  min-height: auto;
  padding: 2px;
  border: 0;
  background: transparent;
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.inline-action:hover {
  color: var(--primary-dark);
  text-decoration: underline;
}

.subtle-count {
  color: var(--muted);
  font-size: 12px;
}

.next-trip-panel {
  border-left: 4px solid #d97706;
  background: #fffcf6;
}

.trip-route {
  margin: 0;
  font-size: 20px;
  font-weight: 750;
}

.trip-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.trip-facts span {
  padding: 5px 9px;
  border-radius: 5px;
  background: #ffffff;
  color: #536173;
  font-size: 12px;
}

.trip-countdown {
  margin: 14px 0 0;
  color: #9a5b06;
  font-size: 13px;
  font-weight: 700;
}

.trip-list {
  display: grid;
  margin: 0;
  padding: 0;
  list-style: none;
}

.trip-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 0;
}

.trip-list li + li {
  border-top: 1px solid #edf0f3;
}

.trip-list li > div:first-child {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.trip-list strong {
  font-size: 14px;
  overflow-wrap: anywhere;
}

.trip-list span,
.trip-list time {
  color: var(--muted);
  font-size: 12px;
}

.trip-list-meta {
  display: grid;
  flex: 0 0 auto;
  gap: 3px;
  text-align: right;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.tag-list span,
.tag-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 9px;
  border-radius: 5px;
  background: #eef4ff;
  color: #24509e;
  font-size: 12px;
}

.details-list {
  display: grid;
  gap: 0;
  margin: 0 0 16px;
}

.details-list > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
}

.details-list > div + div {
  border-top: 1px solid #edf0f3;
}

.details-list dt {
  color: var(--muted);
  font-size: 12px;
}

.details-list dd {
  margin: 0;
  color: #344054;
  font-size: 12px;
  text-align: right;
  overflow-wrap: anywhere;
}

.full-width {
  width: 100%;
}

.form-surface form {
  display: grid;
  gap: 18px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

label,
legend {
  color: #344054;
  font-size: 12px;
  font-weight: 700;
}

label {
  display: grid;
  gap: 7px;
}

input {
  box-sizing: border-box;
  width: 100%;
  min-height: 40px;
  padding: 9px 11px;
  border: 1px solid #cfd6df;
  border-radius: 6px;
  background: #ffffff;
  color: var(--ink);
  font: inherit;
  font-size: 13px;
}

input::placeholder {
  color: #98a2b3;
}

input:focus {
  border-color: #7b9dd7;
  box-shadow: 0 0 0 3px rgb(36 89 198 / 10%);
  outline: none;
}

fieldset {
  display: grid;
  gap: 9px;
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}

.choice-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 7px;
}

.choice-grid.compact {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.choice-chip {
  position: relative;
  display: flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  padding: 7px 9px;
  border: 1px solid #d8dee7;
  border-radius: 6px;
  background: #ffffff;
  color: #536173;
  font-size: 12px;
  font-weight: 650;
  text-align: center;
  cursor: pointer;
}

.choice-chip:hover {
  border-color: #aab8ca;
  background: #f9fbfd;
}

.choice-chip.selected {
  border-color: #6f94d2;
  background: #edf3fc;
  color: #1f4fae;
}

.choice-chip:focus-within {
  outline: 3px solid rgb(36 89 198 / 16%);
  outline-offset: 2px;
}

.choice-chip input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.field-hint {
  margin: -6px 0 0;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.55;
}

.tag-field {
  display: grid;
  gap: 7px;
}

.tag-editor {
  display: flex;
  min-height: 44px;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  padding: 7px;
  border: 1px solid #cfd6df;
  border-radius: 6px;
  background: #ffffff;
}

.tag-editor:focus-within {
  border-color: #7b9dd7;
  box-shadow: 0 0 0 3px rgb(36 89 198 / 10%);
}

.tag-editor input {
  flex: 1 1 160px;
  width: auto;
  min-height: 28px;
  padding: 3px 5px;
  border: 0;
  box-shadow: none;
}

.tag-editor input:focus {
  box-shadow: none;
}

.tag-pill {
  gap: 4px;
  padding-right: 4px;
}

.tag-pill button {
  display: grid;
  width: 22px;
  min-height: 22px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #496ba4;
  font-size: 16px;
}

.tag-pill button:hover {
  background: #dce8fb;
}

.form-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
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

.message .inline-action {
  margin-left: 6px;
  color: inherit;
}

.empty-state {
  padding: 18px;
  border: 1px dashed #d8dee7;
  border-radius: 6px;
  background: #fbfcfd;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.6;
}

.dirty-label {
  color: #9a5b06;
  font-size: 11px;
  font-weight: 700;
}

@media (max-width: 940px) {
  .identity-band {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .identity-actions {
    grid-column: 1 / -1;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .identity-band {
    --identity-padding: 18px;
    align-items: start;
  }

  .identity-avatar {
    width: 56px;
    height: 56px;
    font-size: 22px;
  }

  .identity-heading h1 {
    font-size: 26px;
  }

  .identity-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
  }

  .metric-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-band > div + div {
    border-left: 0;
  }

  .metric-band > div:nth-child(even) {
    border-left: 1px solid var(--line);
  }

  .metric-band > div:nth-child(n + 3) {
    border-top: 1px solid var(--line);
  }

  .field-grid {
    grid-template-columns: 1fr;
  }

  .choice-grid,
  .choice-grid.compact {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .trip-list li,
  .surface-header {
    align-items: flex-start;
  }

  .trip-list li {
    flex-direction: column;
    gap: 7px;
  }

  .trip-list-meta {
    width: 100%;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    text-align: left;
  }
}

@media (max-width: 480px) {
  .identity-band {
    grid-template-columns: 1fr;
  }

  .identity-avatar {
    width: 52px;
    height: 52px;
  }

  .identity-actions,
  .form-actions {
    display: grid;
    grid-template-columns: 1fr;
    width: 100%;
  }

  .primary-button,
  .secondary-button,
  .primary-link {
    width: 100%;
  }

  .surface {
    padding: 16px;
  }

  .choice-grid,
  .choice-grid.compact {
    grid-template-columns: 1fr;
  }
}
</style>
