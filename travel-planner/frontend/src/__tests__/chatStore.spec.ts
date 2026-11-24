import { flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { createConversation, listMessages, sendMessage, streamReply } from '@/api/chat'
import type { ChatMessageResponse, ConversationResponse } from '@/api/schema'
import { useChatStore } from '@/stores/chat'

vi.mock('@/api/chat', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/chat')>()
  return {
    ...actual,
    createConversation: vi.fn(),
    listConversations: vi.fn(),
    listMessages: vi.fn(),
    sendMessage: vi.fn(),
    streamReply: vi.fn()
  }
})

const conversation: ConversationResponse = {
  id: 1,
  title: '杭州行程',
  status: 'ACTIVE',
  messageCount: 1,
  lastMessagePreview: '去杭州',
  createTime: '2026-11-20T10:00:00',
  updateTime: '2026-11-20T10:00:00'
}

function message(overrides: Partial<ChatMessageResponse>): ChatMessageResponse {
  return {
    id: 1,
    conversationId: 1,
    role: 'USER',
    content: '去杭州',
    msgType: 'TEXT',
    status: 'COMPLETED',
    createTime: '2026-11-20T10:00:00',
    updateTime: '2026-11-20T10:00:00',
    ...overrides
  }
}

describe('chat store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('进入会话后恢复未完成的流式回复', async () => {
    const reply = message({
      id: 2,
      role: 'ASSISTANT',
      content: '',
      msgType: 'STREAM',
      status: 'PENDING'
    })
    vi.mocked(listMessages).mockResolvedValue({
      items: [message({}), reply],
      hasMore: false
    })
    vi.mocked(streamReply).mockImplementation(async (_conversationId, _messageId, handlers) => {
      handlers.onReady?.(2)
      handlers.onDelta({ messageId: 2, sequence: 1, content: '杭州' })
      handlers.onDelta({ messageId: 2, sequence: 2, content: '不错' })
      handlers.onDone({ ...reply, content: '杭州不错', status: 'COMPLETED', model: 'rule' })
    })

    const store = useChatStore()
    await store.openConversation(1)
    await flushPromises()
    await new Promise((resolve) => window.setTimeout(resolve, 0))

    expect(store.messages).toHaveLength(2)
    expect(store.messages[1].content).toBe('杭州不错')
    expect(store.messages[1].status).toBe('COMPLETED')
    expect(store.streaming).toBe(false)
    expect(streamReply).toHaveBeenCalledWith(
      1,
      2,
      expect.any(Object),
      expect.objectContaining({ lastEventId: '' })
    )
  })

  it('没有活动会话时先创建会话再发送消息', async () => {
    const userMessage = message({})
    const reply = message({
      id: 2,
      role: 'ASSISTANT',
      content: '',
      msgType: 'STREAM',
      status: 'PENDING'
    })
    vi.mocked(createConversation).mockResolvedValue(conversation)
    vi.mocked(sendMessage).mockResolvedValue({
      conversation,
      userMessage,
      reply
    })
    vi.mocked(streamReply).mockImplementation(async (_conversationId, _messageId, handlers) => {
      handlers.onDone({
        ...reply,
        content: '可以，先告诉我天数',
        status: 'COMPLETED',
        model: 'rule'
      })
    })

    const store = useChatStore()
    await store.send('  去杭州  ')
    await flushPromises()
    await new Promise((resolve) => window.setTimeout(resolve, 0))

    expect(createConversation).toHaveBeenCalled()
    expect(sendMessage).toHaveBeenCalledWith(1, '去杭州')
    expect(store.messages).toHaveLength(2)
    expect(store.messages[1].content).toContain('天数')
  })
})
