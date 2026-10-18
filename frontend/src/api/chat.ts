type ApiEnvelope = {
  code?: number
  message?: string
  data?: { success?: boolean; answer?: string; errorMessage?: string }
}

export function parseChatResponse(payload: unknown): string {
  if (!payload || typeof payload !== 'object') throw new Error('服务返回了无法识别的数据，请稍后重试。')
  const response = payload as ApiEnvelope
  if (response.code !== 200) throw new Error(response.message || '请求未成功，请稍后重试。')
  // ChatController returns HTTP 200 and an outer code=200 even for tool/model failures.
  if (response.data?.success !== true) {
    throw new Error(response.data?.errorMessage || '助手暂时无法回答，请稍后重试。')
  }
  if (typeof response.data.answer !== 'string' || !response.data.answer.trim()) {
    throw new Error('助手返回了空回复，请重新发送。')
  }
  return response.data.answer
}

export async function requestChat(
  id: string,
  question: string,
  options: { fetcher?: typeof fetch; timeoutMs?: number } = {},
): Promise<string> {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), options.timeoutMs ?? 240_000)
  try {
    const response = await (options.fetcher ?? fetch)('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ Id: id, Question: question }),
      signal: controller.signal,
    })
    if (!response.ok) {
      if (response.status === 500 || response.status === 502 || response.status === 503 || response.status === 504) {
        throw new Error('暂时连接不到助手服务，请确认后端已启动，或稍后重试。')
      }
      throw new Error(`请求失败（HTTP ${response.status}），请稍后重试。`)
    }
    let data: unknown
    try { data = await response.json() }
    catch { throw new Error('服务没有返回有效的 JSON，请检查后端连接。') }
    return parseChatResponse(data)
  } catch (error) {
    if (controller.signal.aborted) {
      throw new Error('等待回复超时，服务端可能仍在处理。请稍后重试或新建对话。')
    }
    if (error instanceof TypeError) throw new Error('网络连接失败，请检查网络及后端服务。')
    throw error
  } finally {
    clearTimeout(timeout)
  }
}
