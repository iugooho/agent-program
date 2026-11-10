<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

import { fetchCaptchaChallenge, type CaptchaAnswer } from '@/api/captcha'
import type { CaptchaChallengeResponse } from '@/api/schema'

const props = defineProps<{
  purpose: 'LOGIN' | 'EMAIL_CODE'
  resetKey?: number
}>()

const emit = defineEmits<{
  changed: [value: CaptchaAnswer | null]
}>()

const challenge = ref<CaptchaChallengeResponse | null>(null)
const answer = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function refresh(): Promise<void> {
  loading.value = true
  answer.value = ''
  errorMessage.value = ''
  try {
    challenge.value = await fetchCaptchaChallenge(props.purpose)
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '无法加载人机验证'
  } finally {
    loading.value = false
  }
}

watch([challenge, answer], () => {
  if (challenge.value && answer.value.trim()) {
    emit('changed', {
      challengeId: challenge.value.challengeId,
      answer: answer.value.trim()
    })
  } else {
    emit('changed', null)
  }
})

watch(
  () => props.resetKey,
  () => {
    void refresh()
  }
)

onMounted(() => {
  void refresh()
})
</script>

<template>
  <div class="captcha">
    <div class="captcha-row">
      <button class="image-button" type="button" title="刷新验证码" @click="refresh">
        <img v-if="challenge" :src="challenge.imageDataUrl" alt="图形人机验证码" />
        <span v-else>{{ loading ? '加载中…' : '刷新' }}</span>
      </button>
      <input
        v-model.trim="answer"
        autocomplete="off"
        maxlength="5"
        placeholder="输入图中字符"
        aria-label="人机验证码"
      />
    </div>
    <p v-if="errorMessage" class="captcha-error">{{ errorMessage }}</p>
    <p v-else class="captcha-hint">输入图中字符后直接提交；看不清可点击图片刷新。</p>
  </div>
</template>

<style scoped>
.captcha {
  display: grid;
  gap: 8px;
}

.captcha-row {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 10px;
}

.image-button {
  display: grid;
  width: 160px;
  height: 56px;
  padding: 0;
  place-items: center;
  overflow: hidden;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #64748b;
  cursor: pointer;
}

.image-button img {
  display: block;
  width: 160px;
  height: 56px;
}

.captcha input {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  outline: none;
  font: inherit;
  text-transform: uppercase;
}

.captcha input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgb(37 99 235 / 12%);
}

.captcha-error,
.captcha-hint {
  margin: 0;
  font-size: 12px;
}

.captcha-error {
  color: #be123c;
}

.captcha-hint {
  color: #64748b;
}

@media (max-width: 560px) {
  .captcha-row {
    grid-template-columns: 1fr;
  }

  .image-button {
    width: 100%;
  }
}
</style>
