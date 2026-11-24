import { describe, expect, it } from 'vitest'

import { parseSseBlock, parseSseStream, type SseEvent } from '@/realtime/sse'

function streamOf(chunks: string[]): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder()
  return new ReadableStream({
    start(controller) {
      chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)))
      controller.close()
    }
  })
}

async function collect(chunks: string[]): Promise<SseEvent[]> {
  const events: SseEvent[] = []
  for await (const event of parseSseStream(streamOf(chunks))) {
    events.push(event)
  }
  return events
}

describe('SSE 解析', () => {
  it('跨 chunk 缓冲并解析事件名、id 和 JSON data', async () => {
    const events = await collect([
      'event: delta\nid: 1\ndata: {"messageId":9,"sequence":1,',
      '"content":"你"}\n\nevent: delta\nid: 2\ndata: {"messageId":9,"sequence":2,"content":"好"}\n\n'
    ])

    expect(events).toHaveLength(2)
    expect(events[0]).toEqual({
      event: 'delta',
      id: '1',
      data: '{"messageId":9,"sequence":1,"content":"你"}'
    })
    expect(events[1].id).toBe('2')
  })

  it('支持 CRLF、注释和多行 data', async () => {
    const events = await collect([
      ': keepalive\r\n',
      'event: done\r\nid: 7\r\ndata: line1\r\ndata: line2\r\n\r\n'
    ])

    expect(events).toHaveLength(1)
    expect(events[0]).toEqual({ event: 'done', id: '7', data: 'line1\nline2' })
  })

  it('没有 data 的块不派发', () => {
    expect(parseSseBlock(': heartbeat')).toBeNull()
    expect(parseSseBlock('event: ready')).toBeNull()
  })

  it('事件名缺省为 message', () => {
    expect(parseSseBlock('data: hello')).toEqual({ event: 'message', id: undefined, data: 'hello' })
  })
})
