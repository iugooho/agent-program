<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { deleteTripPlan, fetchTripPlan, fetchTripPlans, planTrip } from '@/api/trips'
import type {
  SavedTravelPlanResponse,
  TravelPlanSummaryResponse,
  TripPlanResponse
} from '@/api/schema'

const message = ref('从北京去杭州玩 4 天，2 人，预算 8000，想轻松一点，不吃辣，不要购物')
const loading = ref(false)
const error = ref('')
const result = ref<TripPlanResponse | null>(null)
const savedPlans = ref<TravelPlanSummaryResponse[]>([])
const savedPlansLoading = ref(false)
const savedPlansError = ref('')
const selectedSavedPlan = ref<SavedTravelPlanResponse | null>(null)
const detailLoading = ref(false)
const deletingPlanId = ref<number | null>(null)

onMounted(() => {
  void loadSavedPlans()
})

async function submit() {
  loading.value = true
  error.value = ''
  try {
    result.value = await planTrip(message.value)
    await loadSavedPlans()
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '生成行程失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function loadSavedPlans(): Promise<void> {
  savedPlansLoading.value = true
  savedPlansError.value = ''
  try {
    savedPlans.value = await fetchTripPlans()
  } catch (reason: unknown) {
    savedPlansError.value = reason instanceof Error ? reason.message : '旅行计划列表加载失败'
  } finally {
    savedPlansLoading.value = false
  }
}

async function openSavedPlan(planId: number): Promise<void> {
  detailLoading.value = true
  savedPlansError.value = ''
  try {
    selectedSavedPlan.value = await fetchTripPlan(planId)
  } catch (reason: unknown) {
    savedPlansError.value = reason instanceof Error ? reason.message : '旅行计划详情加载失败'
  } finally {
    detailLoading.value = false
  }
}

async function removeSavedPlan(plan: TravelPlanSummaryResponse): Promise<void> {
  if (!window.confirm(`确认删除“${plan.title}”吗？`)) {
    return
  }
  deletingPlanId.value = plan.id
  savedPlansError.value = ''
  try {
    await deleteTripPlan(plan.id)
    if (selectedSavedPlan.value?.id === plan.id) {
      selectedSavedPlan.value = null
    }
    await loadSavedPlans()
  } catch (reason: unknown) {
    savedPlansError.value = reason instanceof Error ? reason.message : '旅行计划删除失败'
  } finally {
    deletingPlanId.value = null
  }
}

function money(value: number): string {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0
  }).format(value)
}

function time(value: string): string {
  return value.slice(0, 5)
}

function dateTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}
</script>

<template>
  <section class="planner">
    <header class="hero">
      <p class="eyebrow">TRAVEL AGENT</p>
      <h1>把一句旅行需求变成可执行行程</h1>
      <p class="intro">DeepSeek 理解需求，高德补充景点，和风天气提供预报，Java 校验时间与预算。</p>
    </header>

    <form class="request-card" @submit.prevent="submit">
      <label for="trip-message">告诉智能体你想怎么旅行</label>
      <textarea
        id="trip-message"
        v-model="message"
        rows="4"
        placeholder="例如：从上海去成都玩 5 天，2 人，预算 1 万，喜欢美食和休闲"
      />
      <div class="request-footer">
        <span>第一版支持 1～14 天、最多 20 人</span>
        <button type="submit" :disabled="loading || !message.trim()">
          {{ loading ? '正在规划...' : '生成旅行计划' }}
        </button>
      </div>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
    </form>

    <div v-if="loading" class="loading" aria-live="polite">
      <span class="pulse" />
      正在解析需求、查询景点与天气并校验行程
    </div>

    <section class="saved-plans">
      <header class="saved-head">
        <div>
          <p class="eyebrow">SAVED PLANS</p>
          <h2>已保存旅行计划</h2>
          <p>列表已按当前角色数据范围过滤，单条详情和删除操作会再次校验归属。</p>
        </div>
        <div class="saved-actions">
          <span class="scope-badge">本人 / 部门数据范围</span>
          <button
            type="button"
            class="secondary"
            :disabled="savedPlansLoading"
            @click="loadSavedPlans"
          >
            {{ savedPlansLoading ? '刷新中…' : '刷新列表' }}
          </button>
        </div>
      </header>

      <p v-if="savedPlansError" class="error" role="alert">{{ savedPlansError }}</p>

      <div v-if="savedPlansLoading && savedPlans.length === 0" class="saved-empty">
        正在读取可见计划…
      </div>
      <div v-else-if="savedPlans.length === 0" class="saved-empty">
        暂时没有可见的已保存计划，先生成一份行程。
      </div>
      <div v-else class="saved-grid">
        <article
          v-for="plan in savedPlans"
          :key="plan.id"
          class="saved-card"
          :class="{ selected: selectedSavedPlan?.id === plan.id }"
        >
          <header>
            <div>
              <span>PLAN #{{ plan.id }}</span>
              <h3>{{ plan.title }}</h3>
            </div>
            <b>{{ money(plan.totalBudget) }}</b>
          </header>
          <p>
            {{ plan.destinations.join('、') }} · {{ plan.days }} 天 ·
            {{ plan.startDate }}
          </p>
          <small>
            所属部门 {{ plan.departmentId ?? '未归属' }} ·
            {{ dateTime(plan.createTime) }}
          </small>
          <footer>
            <button type="button" @click="openSavedPlan(plan.id)">查看详情</button>
            <button
              type="button"
              class="danger-text"
              :disabled="deletingPlanId === plan.id"
              @click="removeSavedPlan(plan)"
            >
              {{ deletingPlanId === plan.id ? '删除中…' : '删除' }}
            </button>
          </footer>
        </article>
      </div>

      <article v-if="selectedSavedPlan" class="saved-detail">
        <header>
          <div>
            <span>DETAIL · PLAN #{{ selectedSavedPlan.id }}</span>
            <h3>{{ selectedSavedPlan.itinerary.title }}</h3>
          </div>
          <button type="button" class="ghost" @click="selectedSavedPlan = null">关闭</button>
        </header>
        <div v-if="detailLoading" class="saved-empty">正在加载详情…</div>
        <template v-else>
          <div class="detail-meta">
            <span>
              {{ selectedSavedPlan.request.departureCity }} →
              {{ selectedSavedPlan.request.destinations.join('、') }}
            </span>
            <span>{{ selectedSavedPlan.request.days }} 天</span>
            <span>{{ selectedSavedPlan.request.travelers }} 人</span>
            <span>部门 {{ selectedSavedPlan.departmentId ?? '未归属' }}</span>
            <span>{{ selectedSavedPlan.mode }}</span>
          </div>
          <div class="detail-days">
            <article v-for="day in selectedSavedPlan.itinerary.days" :key="day.dayNumber">
              <strong>DAY {{ day.dayNumber }} · {{ day.city }}</strong>
              <span>{{ day.date }}</span>
              <p>{{ day.items.map((item) => item.title).join(' / ') }}</p>
            </article>
          </div>
        </template>
      </article>
    </section>

    <template v-if="result">
      <section class="summary">
        <div>
          <p class="eyebrow">PARSED REQUEST</p>
          <h2>{{ result.itinerary.title }}</h2>
          <p>
            {{ result.request.departureCity }} → {{ result.request.destinations.join('、') }} ·
            {{ result.request.days }} 天 · {{ result.request.travelers }} 人
          </p>
          <div class="tags">
            <span v-for="style in result.request.travelStyles" :key="style">{{ style }}</span>
            <span v-for="item in result.request.forbiddenItems" :key="item">避开 {{ item }}</span>
          </div>
        </div>
        <div class="mode">
          <span>规划模式</span>
          <strong>{{ result.mode }}</strong>
          <small>已保存为计划 #{{ result.savedPlanId }}</small>
        </div>
      </section>

      <section class="budget">
        <div>
          <span>预计总费用</span>
          <strong>{{ money(result.itinerary.totalBudget) }}</strong>
          <small>用户预算 {{ money(result.request.budget) }}</small>
        </div>
        <div>
          <span>景点门票</span>
          <strong>{{ money(result.itinerary.budgetSummary.attractions) }}</strong>
        </div>
        <div>
          <span>餐饮</span>
          <strong>{{ money(result.itinerary.budgetSummary.food) }}</strong>
        </div>
        <div>
          <span>住宿与市内交通</span>
          <strong>{{ money(result.itinerary.budgetSummary.accommodationAndTransport) }}</strong>
        </div>
      </section>

      <section class="days">
        <article v-for="day in result.itinerary.days" :key="day.dayNumber" class="day-card">
          <header>
            <span>DAY {{ day.dayNumber }}</span>
            <h3>{{ day.date }} · {{ day.city }}</h3>
          </header>
          <ol>
            <li v-for="item in day.items" :key="`${item.startTime}-${item.title}`">
              <time>{{ time(item.startTime) }}–{{ time(item.endTime) }}</time>
              <div>
                <strong>{{ item.title }}</strong>
                <span>{{ item.type }} · {{ item.location }}</span>
                <small>{{ item.notes }}</small>
              </div>
              <b>{{ money(item.estimatedCost) }}</b>
            </li>
          </ol>
          <p v-for="note in day.notes" :key="note" class="day-note">{{ note }}</p>
        </article>
      </section>

      <section class="notes">
        <div>
          <h3>数据来源</h3>
          <p v-if="result.itinerary.evidences.length === 0">当前使用降级数据，请在出行前复查。</p>
          <p
            v-for="evidence in result.itinerary.evidences"
            :key="`${evidence.source}-${evidence.description}`"
          >
            {{ evidence.source }} · {{ evidence.description }}
          </p>
        </div>
        <div>
          <h3>出行提醒</h3>
          <p v-for="warning in result.itinerary.warnings" :key="warning">{{ warning }}</p>
        </div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.planner {
  display: grid;
  gap: 24px;
  max-width: 1080px;
  margin: 0 auto;
}

.hero h1 {
  max-width: 720px;
  margin: 4px 0 10px;
  font-size: clamp(30px, 5vw, 52px);
  line-height: 1.08;
}

.eyebrow {
  margin: 0;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.intro {
  margin: 0;
  color: #64748b;
}

.request-card,
.day-card,
.notes {
  padding: 22px;
  border: 1px solid #dbe3ef;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 16px 50px rgb(15 23 42 / 6%);
}

.request-card {
  display: grid;
  gap: 12px;
}

.request-card label {
  font-weight: 700;
}

textarea {
  width: 100%;
  box-sizing: border-box;
  resize: vertical;
  padding: 14px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  font: inherit;
  line-height: 1.65;
}

.request-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: #64748b;
  font-size: 13px;
}

button {
  padding: 11px 20px;
  border: 0;
  border-radius: 999px;
  background: #2563eb;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
}

button:disabled {
  cursor: wait;
  opacity: 0.6;
}

.error {
  margin: 0;
  color: #b91c1c;
}

.loading {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #475569;
}

.pulse {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #2563eb;
  animation: pulse 1.2s infinite;
}

.summary,
.budget,
.notes {
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.summary h2 {
  margin: 4px 0 8px;
}

.summary p {
  margin: 0;
  color: #475569;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.tags span {
  padding: 4px 9px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
}

.mode {
  display: grid;
  align-content: center;
  min-width: 150px;
  text-align: right;
}

.mode span,
.budget span,
.day-card small,
.day-card li span {
  color: #64748b;
  font-size: 12px;
}

.budget {
  padding: 18px 22px;
  border-radius: 16px;
  background: #0f172a;
  color: #ffffff;
}

.budget > div {
  display: grid;
  gap: 4px;
}

.budget strong {
  font-size: 22px;
}

.budget small {
  color: #94a3b8;
}

.days {
  display: grid;
  gap: 16px;
}

.day-card header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.day-card header span {
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
}

.day-card h3 {
  margin: 0;
}

.day-card ol {
  display: grid;
  gap: 2px;
  padding: 0;
  list-style: none;
}

.day-card li {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr) auto;
  gap: 14px;
  padding: 13px 0;
  border-bottom: 1px solid #edf2f7;
}

.day-card li div {
  display: grid;
  gap: 2px;
}

.day-note {
  margin: 10px 0 0;
  color: #9a3412;
  font-size: 13px;
}

.notes {
  justify-content: flex-start;
}

.notes > div {
  flex: 1;
}

.notes h3 {
  margin-top: 0;
}

.notes p {
  color: #475569;
}

button.secondary {
  background: #eef2f7;
  color: #334155;
}

button.ghost {
  padding: 7px 12px;
  background: #f1f5f9;
  color: #475569;
}

.saved-plans,
.saved-detail {
  padding: 22px;
  border: 1px solid #dbe3ef;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 16px 50px rgb(15 23 42 / 6%);
}

.saved-head,
.saved-detail > header,
.saved-card > header,
.saved-card > footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.saved-head h2,
.saved-detail h3,
.saved-card h3 {
  margin: 4px 0;
}

.saved-head > div > p:not(.eyebrow),
.saved-detail > header span,
.saved-card small {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}

.saved-actions {
  display: flex;
  align-items: center;
  gap: 9px;
}

.scope-badge {
  padding: 5px 9px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #047857;
  font-size: 11px;
  font-weight: 700;
}

.saved-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.saved-card {
  display: grid;
  gap: 9px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fbfcfe;
}

.saved-card.selected {
  border-color: #8fb0ea;
  background: #f3f7ff;
}

.saved-card header span {
  color: #2563eb;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.saved-card header b {
  color: #0f172a;
  font-size: 14px;
  white-space: nowrap;
}

.saved-card > p {
  margin: 0;
  color: #475569;
  font-size: 12px;
}

.saved-card > footer {
  justify-content: flex-start;
  padding-top: 4px;
}

.saved-card > footer button {
  padding: 7px 11px;
  background: #e8eef8;
  color: #1e40af;
  font-size: 11px;
}

.saved-card > footer button.danger-text {
  background: #fff1f2;
  color: #be123c;
}

.saved-empty {
  margin-top: 14px;
  padding: 24px;
  border: 1px dashed #d8e0eb;
  border-radius: 12px;
  color: #64748b;
  text-align: center;
}

.saved-detail {
  margin-top: 16px;
  border-color: #b9ccec;
  background: #f8fbff;
  box-shadow: none;
}

.saved-detail > header > div {
  display: grid;
  gap: 3px;
}

.saved-detail > header span {
  color: #2563eb;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.detail-meta span {
  padding: 5px 9px;
  border-radius: 6px;
  background: #ffffff;
  color: #475569;
  font-size: 11px;
}

.detail-days {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
  margin-top: 12px;
}

.detail-days article {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #e0e8f3;
  border-radius: 9px;
  background: #ffffff;
}

.detail-days strong {
  color: #1e3a8a;
  font-size: 11px;
}

.detail-days span,
.detail-days p {
  margin: 0;
  color: #64748b;
  font-size: 11px;
}

.mode small {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 10px;
}

@keyframes pulse {
  50% {
    opacity: 0.25;
    transform: scale(0.75);
  }
}

@media (max-width: 720px) {
  .request-footer,
  .summary,
  .budget,
  .notes,
  .saved-head,
  .saved-card > header,
  .saved-card > footer {
    align-items: stretch;
    flex-direction: column;
  }

  .mode {
    text-align: left;
  }

  .day-card li {
    grid-template-columns: 76px minmax(0, 1fr);
  }

  .day-card li b {
    grid-column: 2;
  }

  .saved-grid,
  .detail-days {
    grid-template-columns: 1fr;
  }
}
</style>
