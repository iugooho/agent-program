import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  ChatApiError,
  createConversation as createConversationRequest,
  deleteConversation as deleteConversationRequest,
  listConversations as listConversationsRequest,
  listMessages as listMessagesRequest,
  sendMessage as sendMessageRequest,
  streamReply
} from '@/api/chat'
import type { ChatMessageResponse, ConversationResponse } from '@/api/schema'

const MAX_RECONNECT = 3
const RECONNECT_BASE_MS = 500

function sleep(milliseconds: number): Promise<void> {
  return new Promise((resolve) => {
    window.setTimeout(resolve, milliseconds)
  })
}

function readableError(error: unknown): string {
  if (error instanceof ChatApiError) {
    return error.message
  }
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

/** 实时对话状态：会话列表、消息、流式增量与断线续传。 */
export const useChatStore = defineStore('chat', () => {
  const conversations = ref<ConversationResponse[]>([])
  const activeConversationId = ref<number | null>(null)
  const messages = ref<ChatMessageResponse[]>([])
  const loadingConversations = ref(false)
  const loadingMessages = ref(false)
  const loadingOlder = ref(false)
  const sending = ref(false)
  const streamingMessageId = ref<number | null>(null)
  const error = ref('')
  const notice = ref('')
  const hasMore = ref(false)
  const nextBeforeId = ref<number | null>(null)

  const activeConversation = computed(
    () => conversations.value.find((item) => item.id === activeConversationId.value) ?? null
  )
  const streaming = computed(() => streamingMessageId.value !== null)

  let streamController: AbortController | null = null

  async function loadConversations(): Promise<void> {
    loadingConversations.value = true
    try {
      const page = await listConversationsRequest(1, 50)
      conversations.value = page.items
    } catch (reason: unknown) {
      error.value = readableError(reason)
    } finally {
      loadingConversations.value = false
    }
  }

  async function createConversation(title?: string): Promise<ConversationResponse> {
    abortStream()
    const conversation = await createConversationRequest(title)
    conversations.value = [
      conversation,
      ...conversations.value.filter((item) => item.id !== conversation.id)
    ]
    activeConversationId.value = conversation.id
    messages.value = []
    hasMore.value = false
    nextBeforeId.value = null
    error.value = ''
    return conversation
  }

  async function openConversation(id: number, force = false): Promise<void> {
    if (!force && activeConversationId.value === id && messages.value.length > 0) {
      return
    }
    abortStream()
    activeConversationId.value = id
    messages.value = []
    hasMore.value = false
    nextBeforeId.value = null
    error.value = ''
    notice.value = ''
    loadingMessages.value = true
    try {
      const page = await listMessagesRequest(id)
      messages.value = page.items
      hasMore.value = page.hasMore
      nextBeforeId.value = page.nextBeforeId ?? null
      resumePendingReply()
    } catch (reason: unknown) {
      error.value = readableError(reason)
    } finally {
      loadingMessages.value = false
    }
  }

  async function send(content: string): Promise<boolean> {
    const text = content.trim()
    if (!text || sending.value || streaming.value) {
      return false
    }
    sending.value = true
    error.value = ''
    notice.value = ''
    try {
      let conversationId = activeConversationId.value
      if (conversationId === null) {
        const conversation = await createConversation()
        conversationId = conversation.id
      }
      const exchange = await sendMessageRequest(conversationId, text)
      if (activeConversationId.value !== conversationId) {
        return false
      }
      upsertConversation(exchange.conversation)
      messages.value.push(exchange.userMessage, exchange.reply)
      void consumeStream(exchange.reply.id)
      return true
    } catch (reason: unknown) {
      error.value = readableError(reason)
      return false
    } finally {
      sending.value = false
    }
  }

  async function consumeStream(replyId: number): Promise<void> {
    const conversationId = activeConversationId.value
    if (conversationId === null) {
      return
    }
    streamController?.abort()
    const controller = new AbortController()
    streamController = controller
    streamingMessageId.value = replyId
    let lastEventId = ''
    const streamState: { error: ChatApiError | null } = { error: null }
    const stillActive = (): boolean => activeConversationId.value === conversationId

    for (let attempt = 0; attempt <= MAX_RECONNECT; attempt++) {
      try {
        await streamReply(
          conversationId,
          replyId,
          {
            onReady: () => {
              if (stillActive()) {
                markStatus(replyId, 'STREAMING')
              }
            },
            onDelta: (payload) => {
              if (!stillActive()) {
                return
              }
              lastEventId = String(payload.sequence)
              appendDelta(replyId, payload.content)
            },
            onDone: (message) => {
              if (!stillActive()) {
                return
              }
              replaceMessage(message)
              updateConversationFromReply(conversationId, message)
            },
            onError: (streamError) => {
              if (!stillActive()) {
                return
              }
              streamState.error = streamError
              markFailed(replyId, streamError.message)
            }
          },
          { signal: controller.signal, lastEventId }
        )
        if (!stillActive()) {
          return
        }
        if (streamState.error) {
          error.value = streamState.error.message
        }
        return
      } catch (reason: unknown) {
        if (controller.signal.aborted || !stillActive()) {
          return
        }
        if (reason instanceof ChatApiError && isTerminal(reason)) {
          error.value = reason.message
          markFailed(replyId, reason.message)
          return
        }
        if (attempt === MAX_RECONNECT) {
          error.value = '回复连接中断，重新进入会话可以恢复已生成内容'
          markFailed(replyId, '连接中断')
          return
        }
        await sleep(RECONNECT_BASE_MS * 2 ** attempt)
      }
    }
  }

  async function loadOlder(): Promise<void> {
    if (
      loadingOlder.value ||
      !hasMore.value ||
      nextBeforeId.value === null ||
      activeConversationId.value === null
    ) {
      return
    }
    loadingOlder.value = true
    try {
      const page = await listMessagesRequest(activeConversationId.value, nextBeforeId.value)
      const existing = new Set(messages.value.map((message) => message.id))
      messages.value = [
        ...page.items.filter((message) => !existing.has(message.id)),
        ...messages.value
      ]
      hasMore.value = page.hasMore
      nextBeforeId.value = page.nextBeforeId ?? null
    } catch (reason: unknown) {
      error.value = readableError(reason)
    } finally {
      loadingOlder.value = false
    }
  }

  async function removeConversation(id: number): Promise<void> {
    try {
      await deleteConversationRequest(id)
      conversations.value = conversations.value.filter((item) => item.id !== id)
      if (activeConversationId.value === id) {
        abortStream()
        activeConversationId.value = null
        messages.value = []
        hasMore.value = false
        nextBeforeId.value = null
      }
    } catch (reason: unknown) {
      error.value = readableError(reason)
    }
  }

  function stopStreaming(): void {
    if (!streaming.value) {
      return
    }
    abortStream()
    notice.value = '已停止接收，后台仍会保存完整回复'
  }

  function abortStream(): void {
    streamController?.abort()
    streamController = null
    streamingMessageId.value = null
  }

  function resumePendingReply(): void {
    const pending = [...messages.value]
      .reverse()
      .find(
        (message) =>
          message.role === 'ASSISTANT' &&
          (message.status === 'PENDING' || message.status === 'STREAMING')
      )
    if (pending) {
      void consumeStream(pending.id)
    }
  }

  function appendDelta(replyId: number, delta: string): void {
    const message = messages.value.find((item) => item.id === replyId)
    if (!message) {
      return
    }
    message.content += delta
    message.status = 'STREAMING'
    updateConversationPreview(message.conversationId, message.content)
  }

  function markStatus(replyId: number, status: string): void {
    const message = messages.value.find((item) => item.id === replyId)
    if (message) {
      message.status = status
    }
  }

  function markFailed(replyId: number, reason: string): void {
    const message = messages.value.find((item) => item.id === replyId)
    if (!message) {
      return
    }
    message.status = 'FAILED'
    message.errorMessage = reason
    if (streamingMessageId.value === replyId) {
      streamingMessageId.value = null
    }
  }

  function replaceMessage(message: ChatMessageResponse): void {
    const index = messages.value.findIndex((item) => item.id === message.id)
    if (index >= 0) {
      messages.value[index] = message
    } else {
      messages.value.push(message)
    }
    if (streamingMessageId.value === message.id) {
      streamingMessageId.value = null
    }
  }

  function upsertConversation(conversation: ConversationResponse): void {
    const existing = conversations.value.find((item) => item.id === conversation.id)
    if (existing) {
      Object.assign(existing, conversation)
    } else {
      conversations.value.unshift(conversation)
    }
  }

  function updateConversationPreview(conversationId: number, content: string): void {
    const conversation = conversations.value.find((item) => item.id === conversationId)
    if (!conversation) {
      return
    }
    conversation.lastMessagePreview = content.replace(/\s+/g, ' ').slice(0, 60)
    conversation.lastMessageTime = new Date().toISOString()
    conversations.value = [
      conversation,
      ...conversations.value.filter((item) => item.id !== conversationId)
    ]
  }

  function updateConversationFromReply(conversationId: number, message: ChatMessageResponse): void {
    const conversation = conversations.value.find((item) => item.id === conversationId)
    if (!conversation) {
      return
    }
    conversation.lastMessagePreview = message.content.replace(/\s+/g, ' ').slice(0, 60)
    conversation.lastMessageTime = message.updateTime
    if (message.status === 'COMPLETED') {
      conversation.messageCount = Math.max(conversation.messageCount, messages.value.length)
    }
    conversations.value = [
      conversation,
      ...conversations.value.filter((item) => item.id !== conversationId)
    ]
  }

  function isTerminal(streamError: ChatApiError): boolean {
    return (
      streamError.code === 401 ||
      streamError.code === 403 ||
      streamError.code === 4000 ||
      streamError.code === 4001 ||
      streamError.status === 404 ||
      streamError.status === 409
    )
  }

  function dismissError(): void {
    error.value = ''
  }

  function dismissNotice(): void {
    notice.value = ''
  }

  function reset(): void {
    abortStream()
    conversations.value = []
    messages.value = []
    activeConversationId.value = null
    hasMore.value = false
    nextBeforeId.value = null
    error.value = ''
    notice.value = ''
  }

  return {
    conversations,
    activeConversationId,
    activeConversation,
    messages,
    loadingConversations,
    loadingMessages,
    loadingOlder,
    sending,
    streamingMessageId,
    streaming,
    error,
    notice,
    hasMore,
    nextBeforeId,
    loadConversations,
    createConversation,
    openConversation,
    send,
    loadOlder,
    removeConversation,
    stopStreaming,
    dismissError,
    dismissNotice,
    reset
  }
})
