export interface UploadedFile {
  fileName: string
  filePath: string
  fileSize: number
}

export async function uploadMarkdown(file: File, options: { fetcher?: typeof fetch; timeoutMs?: number } = {}): Promise<UploadedFile> {
  if (!/\.md$/i.test(file.name)) throw new Error('请选择 .md 格式的 Markdown 文件。')
  if (!file.size) throw new Error('不能上传空文件。')
  const form = new FormData()
  form.append('file', file)
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), options.timeoutMs ?? 310_000)
  try {
    const response = await (options.fetcher ?? fetch)('/api/upload', {
      method: 'POST', body: form, signal: controller.signal,
      // Let the browser supply the multipart boundary; never set Content-Type manually.
    })
    const text = await response.text()
    let payload: { code?: number; message?: string; data?: UploadedFile } | undefined
    try { payload = JSON.parse(text) } catch { /* Validation failures are plain text in this controller. */ }
    if (!response.ok) {
      if (response.status === 413) throw new Error('文件超过后端允许的大小，请缩小文件后重试。')
      if (response.status >= 500) throw new Error(payload?.message || '上传服务暂时不可用，请确认后端已启动。')
      throw new Error(payload?.message || (text && !text.trim().startsWith('<') ? text.slice(0, 300) : '文件上传失败。'))
    }
    if (payload?.code !== 200) throw new Error(payload?.message || '文件上传失败，服务返回了无效数据。')
    if (typeof payload.data?.fileName !== 'string' || typeof payload.data.filePath !== 'string' || typeof payload.data.fileSize !== 'number') {
      throw new Error('无法确认上传结果，请检查后端响应。')
    }
    return payload.data
  } catch (error) {
    if (controller.signal.aborted) throw new Error('上传等待超时，服务端可能仍在处理，请稍后确认。')
    if (error instanceof TypeError) throw new Error('上传连接失败，请检查网络及后端服务。')
    throw error
  } finally { clearTimeout(timeout) }
}
