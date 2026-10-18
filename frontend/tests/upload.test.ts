import { test } from 'node:test'
import assert from 'node:assert/strict'
import { uploadMarkdown } from '../src/api/upload.ts'

test('uploads Markdown as multipart file without manually setting the boundary', async () => {
  const file = new File(['# Runbook'], '排查手册.md', { type: 'text/markdown' })
  const fetcher = (async (url, init) => {
    assert.equal(url, '/api/upload')
    assert.equal(init?.method, 'POST')
    assert.equal(new Headers(init?.headers).has('Content-Type'), false)
    const uploaded = (init?.body as FormData).get('file') as File
    assert.equal(uploaded.name, file.name)
    assert.equal(await uploaded.text(), '# Runbook')
    return Response.json({ code: 200, data: { fileName: file.name, filePath: `uploads/${file.name}`, fileSize: file.size } })
  }) as typeof fetch
  assert.equal((await uploadMarkdown(file, { fetcher })).fileName, file.name)
})

test('rejects non-Markdown and empty documents before contacting backend', async () => {
  await assert.rejects(uploadMarkdown(new File(['text'], 'image.png')), /\.md/)
  await assert.rejects(uploadMarkdown(new File([], 'empty.md')), /空文件/)
})

test('handles plain-text backend validation errors and file size limits', async () => {
  const file = new File(['text'], 'runbook.md')
  await assert.rejects(uploadMarkdown(file, { fetcher: (async () => new Response('不支持的文件格式', { status: 400 })) as typeof fetch }), /不支持的文件格式/)
  await assert.rejects(uploadMarkdown(file, { fetcher: (async () => new Response('', { status: 413 })) as typeof fetch }), /超过后端允许/)
})

test('does not announce upload success for incomplete or failed envelopes', async () => {
  const file = new File(['text'], 'runbook.md')
  await assert.rejects(uploadMarkdown(file, { fetcher: (async () => Response.json({ code: 200, data: {} })) as typeof fetch }), /无法确认上传结果/)
  await assert.rejects(uploadMarkdown(file, { fetcher: (async () => Response.json({ code: 500, message: '上传失败' })) as typeof fetch }), /上传失败/)
})
