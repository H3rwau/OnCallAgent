import { test } from 'node:test'
import assert from 'node:assert/strict'
import { consumeSse, requestAiOps, requestStreamChat } from '../src/api/stream.ts'

const frame = (type: string, data: unknown = null, newline = '\n') => `event: message${newline}data: ${JSON.stringify({ type, data })}${newline}${newline}`
function response(text: string, byteSize = 7) {
  const bytes = new TextEncoder().encode(text)
  return new Response(new ReadableStream({
    start(controller) {
      for (let offset = 0; offset < bytes.length; offset += byteSize) controller.enqueue(bytes.slice(offset, offset + byteSize))
      controller.close()
    },
  }), { headers: { 'Content-Type': 'text/event-stream;charset=UTF-8' } })
}

test('decodes Chinese and emoji across arbitrary UTF-8 and SSE frame boundaries', async () => {
  const chunks: string[] = []
  await consumeSse(response(frame('content', '中文🙂') + frame('content', '\n下一段') + frame('done'), 1), chunk => chunks.push(chunk))
  assert.deepEqual(chunks, ['中文🙂', '\n下一段'])
})

test('handles CRLF split across packets, comments, multiple data lines and coalesced events', async () => {
  const text = ': heartbeat\r\n\r\nevent: message\r\ndata: {"type":"content",\r\ndata: "data":"第一段"}\r\n\r\n' + frame('content', '第二段', '\r\n') + frame('done', null, '\r\n')
  let result = ''
  await consumeSse(response(text, 1), chunk => { result += chunk })
  assert.equal(result, '第一段第二段')
})

test('delivers content before done and cancels the completed reader', async () => {
  let controller!: ReadableStreamDefaultController<Uint8Array>
  let cancelled = false
  const result: string[] = []
  const stream = new ReadableStream<Uint8Array>({ start(value) { controller = value }, cancel() { cancelled = true } })
  const reading = consumeSse(new Response(stream), chunk => result.push(chunk))
  controller.enqueue(new TextEncoder().encode(frame('content', '先到达的内容')))
  await new Promise(resolve => setImmediate(resolve))
  assert.deepEqual(result, ['先到达的内容'])
  controller.enqueue(new TextEncoder().encode(frame('done')))
  await reading
  assert.equal(cancelled, true)
})

test('preserves delivered content when the backend emits error', async () => {
  let partial = ''
  await assert.rejects(consumeSse(response(frame('content', '部分结果') + frame('error', '工具调用失败')), chunk => { partial += chunk }), /工具调用失败/)
  assert.equal(partial, '部分结果')
})

test('does not treat EOF, invalid JSON or unknown events as success', async () => {
  await assert.rejects(consumeSse(response(frame('content', '连接中断')), () => {}), /完成标记/)
  await assert.rejects(consumeSse(response('data: not-json\n\n'), () => {}), /无法解析/)
  await assert.rejects(consumeSse(response(frame('unknown')), () => {}), /无法识别/)
})

test('stream chat POST uses the same Id and Question contract', async () => {
  const fetcher = (async (url, init) => {
    assert.equal(url, '/api/chat_stream')
    assert.equal(init?.method, 'POST')
    assert.deepEqual(JSON.parse(init!.body as string), { Id: 'same-session', Question: '继续排查' })
    assert.equal(new Headers(init?.headers).get('Accept'), 'text/event-stream')
    return response(frame('content', '结果') + frame('done'))
  }) as typeof fetch
  await requestStreamChat('same-session', '继续排查', () => {}, { fetcher })
})

test('AI Ops POST has no invented request body and uses SSE', async () => {
  const fetcher = (async (url, init) => {
    assert.equal(url, '/api/ai_ops')
    assert.equal(init?.method, 'POST')
    assert.equal(init?.body, undefined)
    assert.equal(new Headers(init?.headers).get('Content-Type'), null)
    return response(frame('content', '报告') + frame('done'))
  }) as typeof fetch
  await requestAiOps(() => {}, { fetcher })
})

test('rejects non-SSE and failed HTTP responses', async () => {
  await assert.rejects(requestAiOps(() => {}, { fetcher: (async () => new Response('{}', { headers: { 'Content-Type': 'application/json' } })) as typeof fetch }), /没有返回 SSE/)
  await assert.rejects(requestAiOps(() => {}, { fetcher: (async () => new Response('', { status: 502 })) as typeof fetch }), /HTTP 502/)
})
