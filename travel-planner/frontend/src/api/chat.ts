import axios, { type AxiosResponse } from 'axios'

import { API_BASE_URL, authorizedFetch, http } from '@/api/http'
import type {
  ChatExchangeResponse,
  ChatMessagePageResponse,
  ChatMessageResponse,
  ConversationListResponse,
  ConversationResponse
} from '@/api/schema'
import { parseSseStream } from '@/realtime/sse'

interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

interface ApiErrorBody {
  code?: number
  message?: string
}

/** 对话接口业务错误，保留 code 供页面区分 401 / 404 / 409 等分支。 */
export class ChatApiError extends Error {
  constructor(
    message: string,
    readonly code?: number,
    readonly status?: number
  ) {
    super(message)
    this.name = 'ChatApiError'
  }
}

/** 创建会话。标题为空时后端生成默认标题。 */
export async function createConversation(title?: string): Promise<ConversationResponse> {
  return unwrap(
    http.post<ApiEnvelope<ConversationResponse>>('/v1/conversations', title ? { title } : {}),
    '创建会话失败'
  )
}

/** 分页查询会话。 */
export async function listConversations(page = 1, size = 20): Promise<ConversationListResponse> {
  return unwrap(
    http.get<ApiEnvelope<ConversationListResponse>>('/v1/conversations', {
      params: { page, size }
    }),
    '加载会话失败'
  )
}

/** 查询消息历史，beforeId 用于向前翻页。 */
export async function listMessages(
  conversationId: number,
  beforeId?: number,
  limit = 50
): Promise<ChatMessagePageResponse> {
  return unwrap(
    http.get<ApiEnvelope<ChatMessagePageResponse>>(`/v1/conversations/${conversationId}/messages`, {
      params: beforeId ? { beforeId, limit } : { limit }
    }),
    '加载消息失败'
  )
}

/** 删除会话（软删除）。 */
export async function deleteConversation(conversationId: number): Promise<void> {
  await unwrap(
    http.delete<ApiEnvelope<null>>(`/v1/conversations/${conversationId}`),
    '删除会话失败'
  )
}

/** 发送消息，返回用户消息和助手占位。 */
export async function sendMessage(
  conversationId: number,
  content: string
): Promise<ChatExchangeResponse> {
  return unwrap(
    http.post<ApiEnvelope<ChatExchangeResponse>>(`/v1/conversations/${conversationId}/messages`, {
      content
    }),
    '发送消息失败'
  )
}

/** SSE 事件回调。 */
export interface ChatStreamHandlers {
  onReady?: (messageId: number) => void
  onDelta: (payload: ChatDeltaPayload) => void
  onDone: (message: ChatMessageResponse) => void
  onError: (error: ChatApiError) => void
}

/** 后端 delta 事件。 */
export interface ChatDeltaPayload {
  messageId: number
  sequence: number
  content: string
}

/** 流式订阅参数。 */
export interface ChatStreamOptions {
  signal?: AbortSignal
  /** 断线重连时带上最后收到的事件 id，服务端只回放之后的内容。 */
  lastEventId?: string
}

/**
 * 订阅助手回复。
 *
 * <p>EventSource 不能带 Authorization，所以使用 fetch + ReadableStream；
 * 正常结束或收到 error 事件时 resolve，连接意外中断时抛出错误，由调用方决定是否续传。</p>
 */
export async function streamReply(
  conversationId: number,
  messageId: number,
  handlers: ChatStreamHandlers,
  options: ChatStreamOptions = {}
): Promise<void> {
  const url =
    `${API_BASE_URL}/v1/conversations/${encodeURIComponent(String(conversationId))}` +
    `/messages/${encodeURIComponent(String(messageId))}/stream`
  const headers: Record<string, string> = { Accept: 'text/event-stream' }
  if (options.lastEventId) {
    headers['Last-Event-ID'] = options.lastEventId
  }

  const response = await authorizedFetch(url, {
    headers,
    signal: options.signal,
    cache: 'no-store'
  })
  if (!response.ok || !response.body) {
    throw await readErrorResponse(response, '订阅回复失败')
  }

  for await (const event of parseSseStream(response.body)) {
    if (event.event === 'ready') {
      handlers.onReady?.(parsePayload<{ messageId: number }>(event.data).messageId)
      continue
    }
    if (event.event === 'delta') {
      handlers.onDelta(parsePayload<ChatDeltaPayload>(event.data))
      continue
    }
    if (event.event === 'done') {
      handlers.onDone(parsePayload<ChatMessageResponse>(event.data))
      return
    }
    if (event.event === 'error') {
      const payload = parsePayload<{ code: number; message: string }>(event.data)
      handlers.onError(new ChatApiError(payload.message, payload.code, response.status))
      return
    }
  }

  throw new ChatApiError('回复连接已断开', undefined, 0)
}

async function unwrap<T>(
  request: Promise<AxiosResponse<ApiEnvelope<T>>>,
  fallback: string
): Promise<T> {
  try {
    const response = await request
    if (response.data.code !== 0) {
      throw new ChatApiError(response.data.message, response.data.code, response.status)
    }
    return response.data.data
  } catch (error: unknown) {
    if (error instanceof ChatApiError) {
      throw error
    }
    if (axios.isAxiosError<ApiErrorBody>(error)) {
      throw new ChatApiError(
        error.response?.data?.message ?? fallback,
        error.response?.data?.code,
        error.response?.status
      )
    }
    throw error instanceof Error ? error : new ChatApiError(fallback)
  }
}

function parsePayload<T>(data: string): T {
  try {
    return JSON.parse(data) as T
  } catch {
    throw new ChatApiError('服务端返回了无法解析的事件数据')
  }
}

async function readErrorResponse(response: Response, fallback: string): Promise<ChatApiError> {
  let message = `${fallback}（HTTP ${response.status}）`
  let code: number | undefined
  try {
    const body = (await response.json()) as ApiErrorBody
    if (body.message) {
      message = body.message
    }
    code = body.code
  } catch {
    // 非 JSON 错误响应，保留 HTTP 状态即可。
  }
  return new ChatApiError(message, code, response.status)
}
