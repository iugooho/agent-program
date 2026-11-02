<script setup lang="ts">
import { ref } from 'vue'

import { planTrip } from '@/api/trips'
import type { TripPlanResponse } from '@/api/schema'

const message = ref('从北京去杭州玩 4 天，2 人，预算 8000，想轻松一点，不吃辣，不要购物')
const loading = ref(false)
const error = ref('')
const result = ref<TripPlanResponse | null>(null)

async function submit() {
  loading.value = true
  error.value = ''
  try {
    result.value = await planTrip(message.value)
  } catch (reason: unknown) {
    error.value = reason instanceof Error ? reason.message : '生成行程失败，请稍后重试'
  } finally {
    loading.value = false
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
  .notes {
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
}
</style>
