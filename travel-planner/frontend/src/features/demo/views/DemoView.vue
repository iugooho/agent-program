<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchHello } from '../api'

const message = ref('')
const error = ref('')
const loading = ref(false)

async function loadHello() {
  loading.value = true
  error.value = ''

  try {
    message.value = await fetchHello()
  } catch {
    error.value = '接口调用失败，请确认后端服务已启动。'
  } finally {
    loading.value = false
  }
}

onMounted(loadHello)
</script>

<template>
  <section class="demo">
    <div>
      <p class="eyebrow">
        API DEMO
      </p>
      <h1>Hello, world</h1>
      <p class="description">
        通过前端请求后端接口并展示返回值。
      </p>
    </div>

    <div
      class="result"
      aria-live="polite"
    >
      <p v-if="loading">
        正在调用...
      </p>
      <p
        v-else-if="error"
        class="error"
      >
        {{ error }}
      </p>
      <p
        v-else
        class="message"
      >
        {{ message }}
      </p>
    </div>

    <button
      type="button"
      :disabled="loading"
      @click="loadHello"
    >
      重新调用
    </button>
  </section>
</template>

<style scoped>
.demo {
  display: grid;
  gap: 20px;
  max-width: 640px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

h1 {
  margin: 0;
  font-size: 32px;
}

.description {
  margin: 8px 0 0;
  color: #4b5563;
}

.result {
  min-height: 80px;
  padding: 20px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #ffffff;
}

.message {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.error {
  margin: 0;
  color: #b91c1c;
}

button {
  width: fit-content;
  padding: 10px 16px;
  border: 1px solid #2563eb;
  border-radius: 6px;
  background: #2563eb;
  color: #ffffff;
  cursor: pointer;
}

button:disabled {
  cursor: wait;
  opacity: 0.65;
}
</style>
