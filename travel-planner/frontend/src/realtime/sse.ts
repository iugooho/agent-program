/** 一条 SSE 事件。 */
export interface SseEvent {
  /** 事件 id，对应后端 delta 的 sequence。 */
  id?: string
  /** 事件名，未声明时为 message。 */
  event: string
  /** data 字段，多行时用换行拼接。 */
  data: string
}

/**
 * 解析一个以空行分隔的 SSE 事件块。
 *
 * <p>只实现项目实际使用的字段（event/id/data），注释和 retry 会被忽略；
 * 返回 null 表示该块没有 data，不需要派发。</p>
 */
export function parseSseBlock(block: string): SseEvent | null {
  let event = 'message'
  let id: string | undefined
  const data: string[] = []

  for (const line of block.split('\n')) {
    if (line === '' || line.startsWith(':')) {
      continue
    }
    const separator = line.indexOf(':')
    const field = separator === -1 ? line : line.slice(0, separator)
    let value = separator === -1 ? '' : line.slice(separator + 1)
    if (value.startsWith(' ')) {
      value = value.slice(1)
    }
    if (field === 'event') {
      event = value
    } else if (field === 'id') {
      id = value
    } else if (field === 'data') {
      data.push(value)
    }
  }

  if (data.length === 0) {
    return null
  }
  return { event, id, data: data.join('\n') }
}

/**
 * 把 fetch 的 ReadableStream 解析成 SSE 事件。
 *
 * <p>跨 chunk 的事件会先缓冲；CRLF 与 LF 都支持；流结束时冲刷最后一个事件。</p>
 */
export async function* parseSseStream(
  stream: ReadableStream<Uint8Array>
): AsyncGenerator<SseEvent> {
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      buffer = buffer.replace(/\r\n/g, '\n')
      let separator = buffer.indexOf('\n\n')
      while (separator >= 0) {
        const block = buffer.slice(0, separator)
        buffer = buffer.slice(separator + 2)
        const event = parseSseBlock(block)
        if (event) {
          yield event
        }
        separator = buffer.indexOf('\n\n')
      }
    }
    buffer += decoder.decode()
    buffer = buffer.replace(/\r\n/g, '\n')
    for (const block of buffer.split('\n\n')) {
      const event = parseSseBlock(block)
      if (event) {
        yield event
      }
    }
  } finally {
    // 收到 done/error 或提前返回时主动关闭底层连接，避免代理一直保持 SSE 长连接。
    await reader.cancel().catch(() => undefined)
    reader.releaseLock()
  }
}
