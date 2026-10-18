export interface StreamOptions {
  fetcher?: typeof fetch
  timeoutMs?: number
}

/** Spring SseEmitter frames: event: message, data: { type: content|error|done, data }. */
export async function consumeSse(response: Response, onContent: (chunk: string) => void): Promise<void> {
  if (!response.body) throw new Error('服务未返回可读取的消息流。')
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let event = 'message'
  let data: string[] = []
  let completed = false

  function dispatch() {
    const payload = data.join('\n')
    const currentEvent = event
    data = []
    event = 'message'
    if (!payload || currentEvent !== 'message' || completed) return
    let message: { type?: string; data?: unknown }
    try { message = JSON.parse(payload) }
    catch { throw new Error('收到无法解析的流式消息，请稍后重试。') }
    if (!message || typeof message !== 'object') throw new Error('流式消息格式不正确。')
    if (message.type === 'content' && typeof message.data === 'string') onContent(message.data)
    else if (message.type === 'error') throw new Error(typeof message.data === 'string' && message.data ? message.data : '服务端处理失败。')
    else if (message.type === 'done') completed = true
    else throw new Error('收到无法识别的流式消息类型。')
  }
  function line(value: string) {
    if (!value) { dispatch(); return }
    if (value.startsWith(':')) return
    const colon = value.indexOf(':')
    const field = colon < 0 ? value : value.slice(0, colon)
    let content = colon < 0 ? '' : value.slice(colon + 1)
    if (content.startsWith(' ')) content = content.slice(1)
    if (field === 'event') event = content || 'message'
    if (field === 'data') data.push(content)
  }
  function drain(final = false) {
    while (!completed) {
      const index = buffer.search(/[\r\n]/)
      if (index < 0) break
      if (!final && buffer[index] === '\r' && index === buffer.length - 1) break
      const separatorLength = buffer[index] === '\r' && buffer[index + 1] === '\n' ? 2 : 1
      const value = buffer.slice(0, index)
      buffer = buffer.slice(index + separatorLength)
      line(value)
    }
    if (final && !completed) {
      if (buffer) line(buffer)
      buffer = ''
      dispatch()
    }
  }

  try {
    while (!completed) {
      const { value, done } = await reader.read()
      buffer += done ? decoder.decode() : decoder.decode(value, { stream: true })
      drain(done)
      if (done) break
    }
    // A closed socket is not successful completion: Spring explicitly sends type=done.
    if (!completed) throw new Error('连接已中断，尚未收到完成标记。已接收内容已保留。')
  } finally {
    try { await reader.cancel() } catch { /* The connection may already be closed. */ }
    reader.releaseLock()
  }
}

export async function requestSse(
  path: '/api/chat_stream' | '/api/ai_ops',
  body: { Id: string; Question: string } | undefined,
  onContent: (chunk: string) => void,
  options: StreamOptions = {},
): Promise<void> {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), options.timeoutMs ?? (path === '/api/ai_ops' ? 610_000 : 310_000))
  try {
    const response = await (options.fetcher ?? fetch)(path, {
      method: 'POST',
      headers: body ? { Accept: 'text/event-stream', 'Content-Type': 'application/json' } : { Accept: 'text/event-stream' },
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    })
    if (!response.ok) throw new Error(`无法连接服务（HTTP ${response.status}），请确认后端已启动。`)
    if (!response.headers.get('content-type')?.includes('text/event-stream')) {
      throw new Error('服务没有返回 SSE 消息流，请检查接口和代理配置。')
    }
    await consumeSse(response, onContent)
  } catch (error) {
    if (controller.signal.aborted) throw new Error('等待服务响应超时，服务端可能仍在处理。已接收内容已保留。')
    if (error instanceof TypeError) throw new Error('流式连接失败，请检查网络及后端服务。')
    throw error
  } finally { clearTimeout(timeout) }
}

export function requestStreamChat(id: string, question: string, onContent: (chunk: string) => void, options?: StreamOptions) {
  return requestSse('/api/chat_stream', { Id: id, Question: question }, onContent, options)
}

export function requestAiOps(onContent: (chunk: string) => void, options?: StreamOptions) {
  // ChatController.aiOps() has no request parameters or request body.
  return requestSse('/api/ai_ops', undefined, onContent, options)
}
