import { afterEach, beforeEach, test } from 'node:test'
import assert from 'node:assert/strict'
import { effectScope, type EffectScope } from 'vue'
import { useChat } from '../src/composables/useChat.ts'

const originalFetch = globalThis.fetch
const storageDescriptor = Object.getOwnPropertyDescriptor(globalThis, 'localStorage')
let scopes: EffectScope[] = []
let values: Map<string, string>
const frame = (type: string, data: unknown = null) => `event: message\ndata: ${JSON.stringify({ type, data })}\n\n`
const sse = (text: string) => new Response(text, { headers: { 'Content-Type': 'text/event-stream' } })
function chat() {
  const scope = effectScope()
  scopes.push(scope)
  return scope.run(() => useChat())!
}

beforeEach(() => {
  values = new Map()
  Object.defineProperty(globalThis, 'localStorage', { configurable: true, value: {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
  } })
})
afterEach(() => {
  for (const scope of scopes) scope.stop()
  scopes = []
  globalThis.fetch = originalFetch
  if (storageDescriptor) Object.defineProperty(globalThis, 'localStorage', storageDescriptor)
  else Reflect.deleteProperty(globalThis, 'localStorage')
})

test('quick and streaming modes share one backend conversation Id', async () => {
  const requests: { url: string; body: { Id: string; Question: string } }[] = []
  globalThis.fetch = (async (url, init) => {
    requests.push({ url: String(url), body: JSON.parse(init!.body as string) })
    return url === '/api/chat' ? Response.json({ code: 200, data: { success: true, answer: '快速回复' } })
      : sse(frame('content', '流式') + frame('content', '回复') + frame('done'))
  }) as typeof fetch
  const store = chat()
  await store.send('第一轮')
  store.setMode('stream')
  await store.send('第二轮')
  assert.equal(requests[0]!.body.Id, requests[1]!.body.Id)
  assert.deepEqual(requests.map(item => item.url), ['/api/chat', '/api/chat_stream'])
  assert.equal(store.active.value.messages.at(-1)?.content, '流式回复')
  store.createConversation()
  const secondId = store.activeId.value
  store.selectConversation(requests[0]!.body.Id)
  assert.equal(store.active.value.mode, 'stream')
  assert.notEqual(store.activeId.value, secondId)
})

test('partial stream failures retain content and release the pending state', async () => {
  globalThis.fetch = (async () => sse(frame('content', '已收到部分结果') + frame('error', '工具错误'))) as typeof fetch
  const store = chat()
  store.setMode('stream')
  await store.send('查询日志')
  assert.equal(store.active.value.messages.at(-1)?.content, '已收到部分结果')
  assert.equal(store.active.value.messages.at(-1)?.error, '工具错误')
  assert.equal(store.active.value.messages.at(-1)?.streaming, false)
  assert.equal(store.busy.value, false)
})

test('AI Ops blocks new chats, switching, uploads and repeat execution until done', async () => {
  let controller!: ReadableStreamDefaultController<Uint8Array>
  let calls = 0
  globalThis.fetch = (async (url, init) => {
    calls++
    assert.equal(url, '/api/ai_ops')
    assert.equal(init?.body, undefined)
    return new Response(new ReadableStream<Uint8Array>({ start(value) { controller = value } }), {
      headers: { 'Content-Type': 'text/event-stream' },
    })
  }) as typeof fetch
  const store = chat()
  const oldId = store.activeId.value
  const task = store.startAiOps()
  const reportId = store.activeId.value
  assert.equal(store.aiOpsRunning.value, true)
  store.createConversation()
  store.selectConversation(oldId)
  store.setMode('stream')
  await store.send('不应发送')
  await store.upload(new File(['# note'], 'note.md'))
  await store.startAiOps()
  assert.equal(store.activeId.value, reportId)
  assert.equal(calls, 1)
  controller.enqueue(new TextEncoder().encode(frame('content', '正在读取告警')))
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(store.active.value.messages.at(-1)?.content, '正在读取告警')
  assert.equal(store.aiOpsRunning.value, true)
  controller.enqueue(new TextEncoder().encode(frame('content', '\n# 最终报告') + frame('done')))
  await task
  assert.equal(store.aiOpsRunning.value, false)
  assert.equal(store.busy.value, false)
  assert.equal(store.active.value.kind, 'ai-ops')
  store.createConversation()
  assert.notEqual(store.activeId.value, reportId)
  assert.ok(store.history.value.some(item => item.id === reportId))
})

test('AI Ops errors always unlock and preserve the failed report', async () => {
  globalThis.fetch = (async () => sse(frame('content', '正在分析') + frame('error', '无法获取告警'))) as typeof fetch
  const store = chat()
  await store.startAiOps()
  assert.equal(store.aiOpsRunning.value, false)
  assert.equal(store.pendingId.value, null)
  assert.equal(store.active.value.messages.at(-1)?.content, '正在分析')
  assert.equal(store.active.value.messages.at(-1)?.error, '无法获取告警')
})

test('an EOF without done also unlocks AI Ops', async () => {
  globalThis.fetch = (async () => sse(frame('content', '连接中断'))) as typeof fetch
  const store = chat()
  await store.startAiOps()
  assert.equal(store.aiOpsRunning.value, false)
  assert.match(store.active.value.messages.at(-1)?.error || '', /完成标记/)
})

test('uploads are recorded in history and restored independently of chat messages', async () => {
  globalThis.fetch = (async () => Response.json({ code: 200, data: {
    fileName: 'runbook.md', filePath: 'uploads/runbook.md', fileSize: 10,
  } })) as typeof fetch
  const store = chat()
  await store.upload(new File(['# Runbook'], 'runbook.md'))
  assert.equal(store.uploading.value, false)
  assert.equal(store.history.value[0]?.uploads[0]?.fileName, 'runbook.md')
  const restored = chat()
  assert.equal(restored.active.value.uploads[0]?.fileName, 'runbook.md')
})

test('refresh marks partial streams interrupted instead of leaving an eternal spinner', () => {
  values.set('oncall-agent.chat.v1', JSON.stringify({ activeId: 'test', conversations: [{
    id: 'test', title: '中断的报告', updatedAt: Date.now(), kind: 'ai-ops', mode: 'stream', uploads: [],
    messages: [{ id: 'm', role: 'assistant', content: '部分报告', createdAt: Date.now(), streaming: true }],
  }] }))
  const store = chat()
  assert.equal(store.aiOpsRunning.value, false)
  assert.equal(store.active.value.messages[0]?.streaming, false)
  assert.equal(store.active.value.messages[0]?.content, '部分报告')
  assert.equal(store.active.value.messages[0]?.failed, true)
})
