<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import type { ChatMessageResponse } from '@/api/schema'
import { useChatStore } from '@/stores/chat'

const route = useRoute()
const router = useRouter()
const chat = useChatStore()

const draft = ref('')
const listRef = ref<HTMLElement | null>(null)
const nearBottom = ref(true)

const suggestions = [
  '从成都去云南 5 天，2 个人，预算 8000，休闲一点，不要购物',
  '帮我把杭州 3 天行程排得轻松一点',
  '第一次去北京，哪些景点值得优先安排？',
  '预算 6000，去西安 4 天，住哪里方便？'
]

const routeConversationId = computed(() => {
  const raw = route.params.conversationId
  const value = Array.isArray(raw) ? raw[0] : raw
  const id = Number(value)
  return Number.isFinite(id) && id > 0 ? id : null
})

const activeTitle = computed(() => chat.activeConversation?.title ?? '实时对话')
const lastMessage = computed(() => chat.messages[chat.messages.length - 1] ?? null)
const composerDisabled = computed(() => chat.sending || chat.streaming)
const canSend = computed(() => draft.value.trim().length > 0 && !composerDisabled.value)

watch(
  routeConversationId,
  async (id) => {
    if (id !== null && id !== chat.activeConversationId) {
      await chat.openConversation(id)
    }
  },
  { immediate: true }
)

watch(
  () => chat.messages.length,
  () => {
    void scrollToBottom(false)
  }
)

watch(
  () => lastMessage.value?.content,
  () => {
    void scrollToBottom(false)
  }
)

onMounted(async () => {
  await chat.loadConversations()
  if (routeConversationId.value === null && chat.conversations.length > 0) {
    await router.replace(`/chat/${chat.conversations[0].id}`)
  }
})

function isNearBottom(): boolean {
  const element = listRef.value
  if (!element) {
    return true
  }
  return element.scrollHeight - element.scrollTop - element.clientHeight < 96
}

function onScroll(): void {
  nearBottom.value = isNearBottom()
}

async function scrollToBottom(force: boolean): Promise<void> {
  await nextTick()
  const element = listRef.value
  if (!element || (!force && !nearBottom.value)) {
    return
  }
  element.scrollTop = element.scrollHeight
}

async function selectConversation(id: number): Promise<void> {
  if (id !== chat.activeConversationId) {
    await router.push(`/chat/${id}`)
  }
}

async function newConversation(): Promise<void> {
  try {
    const conversation = await chat.createConversation()
    draft.value = ''
    await router.push(`/chat/${conversation.id}`)
  } catch {
    // 错误已经由 store 写入 error，页面会展示提示。
  }
}

async function sendDraft(): Promise<void> {
  if (!canSend.value) {
    return
  }
  const text = draft.value
  const sent = await chat.send(text)
  if (!sent) {
    draft.value = text
    return
  }
  draft.value = ''
  if (
    chat.activeConversationId !== null &&
    routeConversationId.value !== chat.activeConversationId
  ) {
    await router.replace(`/chat/${chat.activeConversationId}`)
  }
  await scrollToBottom(true)
}

function onComposerKeydown(event: KeyboardEvent): void {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) {
    return
  }
  event.preventDefault()
  void sendDraft()
}

function useSuggestion(text: string): void {
  draft.value = text
  void sendDraft()
}

async function removeConversation(id: number): Promise<void> {
  const confirmed = window.confirm('删除这个对话及全部消息？此操作不可撤销。')
  if (!confirmed) {
    return
  }
  await chat.removeConversation(id)
  if (chat.activeConversationId === null) {
    const next = chat.conversations[0]
    if (next) {
      await router.replace(`/chat/${next.id}`)
    } else {
      await router.replace('/chat')
    }
  }
}

function formatTime(value?: string): string {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

function isPending(message: ChatMessageResponse): boolean {
  return (
    message.role === 'ASSISTANT' &&
    message.content.length === 0 &&
    (message.status === 'PENDING' || message.status === 'STREAMING')
  )
}

function isStreaming(message: ChatMessageResponse): boolean {
  return message.id === chat.streamingMessageId && message.status === 'STREAMING'
}

function modelLabel(message: ChatMessageResponse): string {
  if (message.model === 'rule') {
    return '本地规则'
  }
  if (message.model) {
    return '大模型'
  }
  return '助手'
}
</script>

<template>
  <section class="chat-page">
    <aside class="conversation-panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">CONVERSATIONS</p>
          <h1>实时对话</h1>
        </div>
        <button class="new-button" type="button" @click="newConversation">新对话</button>
      </div>

      <div v-if="chat.loadingConversations" class="panel-hint">正在加载会话…</div>
      <div v-else-if="chat.conversations.length === 0" class="panel-hint">
        还没有对话，直接输入旅行需求开始。
      </div>
      <ul v-else class="conversation-list" role="list">
        <li v-for="conversation in chat.conversations" :key="conversation.id">
          <button
            type="button"
            class="conversation-item"
            :class="{ active: conversation.id === chat.activeConversationId }"
            @click="selectConversation(conversation.id)"
          >
            <span class="conversation-title">{{ conversation.title }}</span>
            <span class="conversation-preview">
              {{ conversation.lastMessagePreview || '还没有消息' }}
            </span>
            <span class="conversation-meta">
              <time>{{ formatTime(conversation.lastMessageTime || conversation.updateTime) }}</time>
              <span>{{ conversation.messageCount }} 条</span>
            </span>
          </button>
          <button
            class="delete-button"
            type="button"
            aria-label="删除对话"
            title="删除对话"
            @click.stop="removeConversation(conversation.id)"
          >
            ×
          </button>
        </li>
      </ul>
    </aside>

    <main class="chat-main">
      <header class="chat-header">
        <div class="chat-heading">
          <p class="eyebrow">TRAVEL AGENT · SSE</p>
          <h2>{{ activeTitle }}</h2>
        </div>
        <div class="chat-badges">
          <span v-if="chat.streaming" class="badge live">正在生成</span>
          <span class="badge">流式回复</span>
        </div>
      </header>

      <div
        ref="listRef"
        class="message-list"
        role="log"
        aria-live="polite"
        :aria-busy="chat.streaming"
        @scroll.passive="onScroll"
      >
        <div v-if="chat.hasMore" class="load-older">
          <button type="button" :disabled="chat.loadingOlder" @click="chat.loadOlder">
            {{ chat.loadingOlder ? '加载中…' : '加载更早的消息' }}
          </button>
        </div>

        <div v-if="chat.loadingMessages" class="empty-state">
          <span class="pulse" />
          正在加载消息…
        </div>

        <div v-else-if="chat.messages.length === 0" class="empty-state welcome">
          <p class="eyebrow">START A CONVERSATION</p>
          <h3>把一句旅行需求，拆成能执行的行程</h3>
          <p>
            告诉我目的地、天数、人数和预算；缺的信息我会追问。回复通过 SSE 实时返回，
            断线后重新进入会自动恢复。
          </p>
          <div class="suggestions">
            <button
              v-for="suggestion in suggestions"
              :key="suggestion"
              type="button"
              @click="useSuggestion(suggestion)"
            >
              {{ suggestion }}
            </button>
          </div>
        </div>

        <template v-else>
          <article
            v-for="message in chat.messages"
            :key="message.id"
            class="message"
            :class="message.role === 'USER' ? 'is-user' : 'is-assistant'"
          >
            <header class="message-head">
              <span>{{ message.role === 'USER' ? '你' : '旅行助手' }}</span>
              <time>{{ formatTime(message.createTime) }}</time>
            </header>
            <div class="bubble">
              <p v-if="message.content" class="message-content">{{ message.content }}</p>
              <p v-else-if="isPending(message)" class="thinking">
                <span class="pulse" />
                正在组织回答…
              </p>
              <span v-if="isStreaming(message)" class="caret" aria-hidden="true" />
              <p v-if="message.status === 'FAILED'" class="failure">
                {{ message.errorMessage || '回复生成失败，请重新发送' }}
              </p>
            </div>
            <footer v-if="message.role === 'ASSISTANT' && message.model" class="message-meta">
              {{ modelLabel(message) }}
            </footer>
          </article>
        </template>
      </div>

      <div v-if="chat.error" class="alert" role="alert">
        <span>{{ chat.error }}</span>
        <button type="button" @click="chat.dismissError">知道了</button>
      </div>
      <div v-if="chat.notice" class="notice">
        <span>{{ chat.notice }}</span>
        <button type="button" @click="chat.dismissNotice">知道了</button>
      </div>

      <form class="composer" @submit.prevent="sendDraft">
        <textarea
          v-model="draft"
          rows="2"
          :disabled="composerDisabled"
          placeholder="例如：从北京去杭州玩 4 天，2 人，预算 8000，想轻松一点，不吃辣，不要购物"
          aria-label="输入旅行需求"
          @keydown="onComposerKeydown"
        />
        <div class="composer-actions">
          <span class="composer-hint">Enter 发送 · Shift + Enter 换行</span>
          <button
            v-if="chat.streaming"
            class="stop-button"
            type="button"
            @click="chat.stopStreaming"
          >
            停止接收
          </button>
          <button v-else type="submit" :disabled="!canSend">
            {{ chat.sending ? '发送中…' : '发送' }}
          </button>
        </div>
      </form>
    </main>
  </section>
</template>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 18px;
  height: calc(100vh - 120px);
  min-height: 560px;
  max-width: 1280px;
  margin: 0 auto;
}

.conversation-panel,
.chat-main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 18px 60px rgb(15 23 42 / 6%);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 18px 14px;
  border-bottom: 1px solid #eef2f7;
}

.panel-head h1,
.chat-header h2 {
  margin: 2px 0 0;
  font-size: 20px;
}

.eyebrow {
  margin: 0;
  color: #2563eb;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.new-button {
  padding: 8px 12px;
  border: 0;
  border-radius: 999px;
  background: #2563eb;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.panel-hint {
  padding: 18px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.conversation-list {
  display: grid;
  gap: 4px;
  margin: 0;
  padding: 8px;
  overflow-y: auto;
  list-style: none;
}

.conversation-list li {
  position: relative;
}

.conversation-item {
  display: grid;
  width: 100%;
  gap: 4px;
  padding: 12px 40px 12px 12px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.conversation-item:hover {
  background: #f8fafc;
}

.conversation-item.active {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.conversation-title {
  overflow: hidden;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-preview {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-meta {
  display: flex;
  justify-content: space-between;
  color: #94a3b8;
  font-size: 11px;
}

.delete-button {
  position: absolute;
  top: 8px;
  right: 6px;
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #94a3b8;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
}

.conversation-list li:hover .delete-button,
.delete-button:focus-visible {
  opacity: 1;
}

.delete-button:hover {
  background: #fee2e2;
  color: #b91c1c;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 22px;
  border-bottom: 1px solid #eef2f7;
}

.chat-badges {
  display: flex;
  gap: 8px;
}

.badge {
  padding: 5px 10px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.badge.live {
  background: #dcfce7;
  color: #166534;
}

.message-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 18px;
  min-height: 0;
  padding: 22px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.load-older {
  display: flex;
  justify-content: center;
}

.load-older button {
  padding: 6px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  background: #ffffff;
  color: #475569;
  font-size: 12px;
  cursor: pointer;
}

.empty-state {
  display: grid;
  gap: 12px;
  align-content: center;
  max-width: 640px;
  margin: auto;
  padding: 24px;
  color: #475569;
  text-align: center;
}

.empty-state h3 {
  margin: 0;
  color: #0f172a;
  font-size: clamp(22px, 3vw, 30px);
}

.empty-state p {
  margin: 0;
  line-height: 1.7;
}

.suggestions {
  display: grid;
  gap: 8px;
  margin-top: 8px;
  text-align: left;
}

.suggestions button {
  padding: 10px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #f8fafc;
  color: #334155;
  font: inherit;
  font-size: 13px;
  line-height: 1.5;
  text-align: left;
  cursor: pointer;
}

.suggestions button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.message {
  display: grid;
  gap: 6px;
  max-width: 82%;
}

.message.is-user {
  align-self: flex-end;
  justify-items: end;
}

.message.is-assistant {
  align-self: flex-start;
  justify-items: start;
}

.message-head {
  display: flex;
  gap: 10px;
  color: #94a3b8;
  font-size: 11px;
}

.message-head span {
  color: #475569;
  font-weight: 700;
}

.bubble {
  padding: 13px 15px;
  border-radius: 16px;
  background: #f1f5f9;
  color: #0f172a;
  line-height: 1.7;
}

.is-user .bubble {
  border-bottom-right-radius: 4px;
  background: #2563eb;
  color: #ffffff;
}

.is-assistant .bubble {
  border-bottom-left-radius: 4px;
  background: #f8fafc;
  box-shadow: inset 0 0 0 1px #e2e8f0;
}

.message-content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.thinking {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #64748b;
}

.pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2563eb;
  animation: pulse 1.1s infinite;
}

.caret {
  display: inline-block;
  width: 7px;
  height: 1.05em;
  margin-left: 2px;
  border-radius: 1px;
  background: #2563eb;
  vertical-align: text-bottom;
  animation: blink 0.9s step-end infinite;
}

.failure {
  margin: 8px 0 0;
  color: #b91c1c;
  font-size: 12px;
}

.message-meta {
  color: #94a3b8;
  font-size: 11px;
}

.alert,
.notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 22px;
  font-size: 13px;
}

.alert {
  background: #fef2f2;
  color: #991b1b;
}

.notice {
  background: #fffbeb;
  color: #92400e;
}

.alert button,
.notice button {
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.composer {
  display: grid;
  gap: 10px;
  padding: 14px 22px 18px;
  border-top: 1px solid #eef2f7;
}

.composer textarea {
  width: 100%;
  box-sizing: border-box;
  resize: vertical;
  padding: 12px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  color: inherit;
  font: inherit;
  line-height: 1.6;
}

.composer textarea:focus {
  border-color: #93c5fd;
  outline: 2px solid #dbeafe;
}

.composer textarea:disabled {
  background: #f8fafc;
  color: #94a3b8;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.composer-hint {
  color: #94a3b8;
  font-size: 12px;
}

.composer-actions button {
  padding: 9px 18px;
  border: 0;
  border-radius: 999px;
  background: #2563eb;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
}

.composer-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.composer-actions .stop-button {
  background: #0f172a;
}

@keyframes pulse {
  50% {
    opacity: 0.25;
    transform: scale(0.72);
  }
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

@media (max-width: 900px) {
  .chat-page {
    grid-template-columns: 1fr;
    height: auto;
  }

  .conversation-panel {
    max-height: 240px;
  }

  .chat-main {
    height: calc(100vh - 320px);
    min-height: 520px;
  }

  .message {
    max-width: 94%;
  }
}
</style>
