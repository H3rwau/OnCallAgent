import { test } from 'node:test'
import assert from 'node:assert/strict'
import { parseChatResponse, requestChat } from '../src/api/chat.ts'

test('extracts the nested backend answer', () => {
  assert.equal(parseChatResponse({ code: 200, message: 'success', data: { success: true, answer: '排查结果' } }), '排查结果')
})

test('rejects model errors even when both HTTP and outer code are successful', async () => {
  const fetcher = (async () => new Response(JSON.stringify({
    code: 200, message: 'success', data: { success: false, answer: null, errorMessage: 'MCP 连接失败' },
  }))) as typeof fetch
  await assert.rejects(requestChat('session-a', '查询日志', { fetcher }), /MCP 连接失败/)
})

test('sends the exact backend field names and preserves the conversation ID across turns', async () => {
  const requests: unknown[] = []
  const fetcher = (async (url, init) => {
    assert.equal(url, '/api/chat')
    assert.equal(init?.method, 'POST')
    requests.push(JSON.parse(init!.body as string))
    return new Response(JSON.stringify({ code: 200, data: { success: true, answer: '收到' } }))
  }) as typeof fetch
  await requestChat('fixed-session-id', '第一轮', { fetcher })
  await requestChat('fixed-session-id', '继续', { fetcher })
  assert.deepEqual(requests, [
    { Id: 'fixed-session-id', Question: '第一轮' },
    { Id: 'fixed-session-id', Question: '继续' },
  ])
})

test('reports proxy failures and invalid JSON', async () => {
  await assert.rejects(requestChat('a', 'q', {
    fetcher: (async () => new Response('Bad Gateway', { status: 502 })) as typeof fetch,
  }), /后端已启动/)
  await assert.rejects(requestChat('a', 'q', {
    fetcher: (async () => new Response('<html>not json</html>')) as typeof fetch,
  }), /有效的 JSON/)
})

test('rejects malformed envelopes and empty answers', () => {
  assert.throws(() => parseChatResponse(null), /无法识别/)
  assert.throws(() => parseChatResponse({ code: 500, message: '会话异常' }), /会话异常/)
  assert.throws(() => parseChatResponse({ code: 200, data: { success: true, answer: '  ' } }), /空回复/)
})

test('aborts a slow request with a useful timeout message', async () => {
  const fetcher = ((_url, init) => new Promise((_resolve, reject) => {
    init?.signal?.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')))
  })) as typeof fetch
  await assert.rejects(requestChat('a', 'q', { fetcher, timeoutMs: 5 }), /服务端可能仍在处理/)
})
