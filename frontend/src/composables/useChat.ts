import { computed, onScopeDispose, ref, watch } from 'vue'
import { requestChat } from '../api/chat'
import { requestAiOps, requestStreamChat } from '../api/stream'
import { uploadMarkdown, type UploadedFile } from '../api/upload'

export type ChatMode = 'quick' | 'stream'
export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: number
  failed?: boolean
  error?: string
  streaming?: boolean
  mode?: ChatMode
  source?: 'chat' | 'ai-ops'
}
export interface Attachment extends UploadedFile { id: string; createdAt: number }
export interface Conversation {
  id: string
  title: string
  updatedAt: number
  messages: Message[]
  kind: 'chat' | 'ai-ops'
  mode: ChatMode
  uploads: Attachment[]
}
const STORAGE_KEY = 'oncall-agent.chat.v1'
const errorText = (error: unknown) => error instanceof Error ? error.message.slice(0, 600) : '请求失败，请稍后重试。'

function newConversation(kind: Conversation['kind'] = 'chat'): Conversation {
  return { id: crypto.randomUUID(), title: '新对话', updatedAt: Date.now(), messages: [], kind, mode: 'quick', uploads: [] }
}
function loadConversations(): { conversations: Conversation[]; activeId: string } {
  try {
    const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
    if (!stored || !Array.isArray(stored.conversations)) return { conversations: [], activeId: '' }
    const conversations: Conversation[] = stored.conversations.filter((item: Conversation) =>
      item && typeof item.id === 'string' && typeof item.title === 'string' && Number.isFinite(item.updatedAt) &&
      Array.isArray(item.messages) && item.messages.every(message => message && typeof message.id === 'string' &&
        (message.role === 'user' || message.role === 'assistant') && typeof message.content === 'string' && Number.isFinite(message.createdAt)),
    ).slice(0, 100)
    for (const conversation of conversations) {
      conversation.kind = conversation.kind === 'ai-ops' ? 'ai-ops' : 'chat'
      conversation.mode = conversation.mode === 'stream' ? 'stream' : 'quick'
      conversation.uploads = Array.isArray(conversation.uploads) ? conversation.uploads.filter(file =>
        file && typeof file.id === 'string' && typeof file.fileName === 'string' && typeof file.filePath === 'string' && Number.isFinite(file.fileSize),
      ) : []
      for (const message of conversation.messages) {
        // Migrate errors saved by the earlier quick-chat-only frontend.
        if (message.failed && !message.error) { message.error = message.content; message.content = '' }
        if (message.streaming) {
          message.streaming = false
          message.failed = true
          message.error = '上次连接未接收完成，服务端可能仍在处理。已接收内容已保留。'
        }
      }
      if (conversation.messages.at(-1)?.role === 'user') {
        conversation.messages.push({ id: crypto.randomUUID(), role: 'assistant', content: '', failed: true,
          error: '上一条回复未接收完成，服务端可能仍在处理。请稍后重试。', createdAt: Date.now() })
      }
    }
    return { conversations, activeId: typeof stored.activeId === 'string' ? stored.activeId : '' }
  } catch { return { conversations: [], activeId: '' } }
}

export function useChat() {
  const saved = loadConversations()
  const conversations = ref<Conversation[]>(saved.conversations)
  if (!conversations.value.length) conversations.value.push(newConversation())
  const activeId = ref(conversations.value.some(item => item.id === saved.activeId) ? saved.activeId : conversations.value[0]!.id)
  const pendingId = ref<string | null>(null)
  const aiOpsRunning = ref(false)
  const uploading = ref(false)
  const uploadingName = ref('')
  const uploadError = ref('')
  const storageNotice = ref('')
  const active = computed(() => conversations.value.find(item => item.id === activeId.value)!)
  const history = computed(() => conversations.value.filter(item => item.messages.length || item.uploads.length)
    .sort((a, b) => b.updatedAt - a.updatedAt))
  const busy = computed(() => !!pendingId.value || uploading.value || aiOpsRunning.value)
  let persistenceTimer: ReturnType<typeof setTimeout> | undefined
  let disposed = false

  function persist() {
    if (disposed) return
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        conversations: [...history.value].slice(0, 100), activeId: activeId.value,
      }))
      storageNotice.value = ''
    } catch { storageNotice.value = '浏览器未能保存历史记录，当前对话仍可继续。' }
  }
  // Coalesce token updates rather than writing localStorage for every SSE event.
  watch([conversations, activeId], () => {
    clearTimeout(persistenceTimer)
    persistenceTimer = setTimeout(persist, 150)
  }, { deep: true })
  if (typeof window !== 'undefined') window.addEventListener('beforeunload', persist)
  onScopeDispose(() => {
    clearTimeout(persistenceTimer)
    persist()
    disposed = true
    if (typeof window !== 'undefined') window.removeEventListener('beforeunload', persist)
  })

  function createConversation() {
    if (aiOpsRunning.value || uploading.value) return
    const empty = conversations.value.find(item => item.kind === 'chat' && !item.messages.length && !item.uploads.length)
    if (empty) activeId.value = empty.id
    else {
      const conversation = newConversation()
      conversations.value.unshift(conversation)
      activeId.value = conversation.id
    }
    uploadError.value = ''
  }
  function selectConversation(id: string) {
    if (aiOpsRunning.value || uploading.value || !conversations.value.some(item => item.id === id)) return
    activeId.value = id
    uploadError.value = ''
  }
  function setMode(mode: ChatMode) {
    if (busy.value || active.value.kind !== 'chat') return
    active.value.mode = mode
  }
  function answerMessage(conversation: Conversation, mode: ChatMode): Message {
    conversation.messages.push({ id: crypto.randomUUID(), role: 'assistant', content: '', createdAt: Date.now(),
      streaming: true, mode, source: conversation.kind === 'ai-ops' ? 'ai-ops' : 'chat' })
    // Mutate the reactive array entry, not the raw object inserted above.
    return conversation.messages.at(-1)!
  }
  async function getAnswer(conversation: Conversation, question: string, mode: ChatMode) {
    pendingId.value = conversation.id
    const answer = answerMessage(conversation, mode)
    try {
      if (mode === 'stream') await requestStreamChat(conversation.id, question, chunk => { answer.content += chunk })
      else answer.content = await requestChat(conversation.id, question)
      if (!answer.content.trim()) throw new Error('助手返回了空回复，请重新发送。')
    } catch (error) {
      answer.failed = true
      answer.error = errorText(error)
    } finally {
      answer.streaming = false
      conversation.updatedAt = Date.now()
      pendingId.value = null
      persist()
    }
  }
  async function send(question: string) {
    const text = question.trim()
    if (!text || busy.value || active.value.kind !== 'chat') return
    const conversation = active.value
    if (!conversation.messages.length) conversation.title = text.replace(/\s+/g, ' ').slice(0, 40)
    conversation.messages.push({ id: crypto.randomUUID(), role: 'user', content: text, createdAt: Date.now() })
    conversation.updatedAt = Date.now()
    await getAnswer(conversation, text, conversation.mode)
  }
  async function retry() {
    if (busy.value || active.value.kind !== 'chat') return
    const conversation = active.value
    const last = conversation.messages.at(-1)
    const question = conversation.messages.at(-2)
    if (!last?.failed || question?.role !== 'user') return
    const mode = last.mode ?? conversation.mode
    conversation.messages.pop()
    await getAnswer(conversation, question.content, mode)
  }
  async function upload(file: File) {
    if (busy.value || active.value.kind !== 'chat') return
    const conversation = active.value
    uploading.value = true
    uploadingName.value = file.name
    uploadError.value = ''
    try {
      const result = await uploadMarkdown(file)
      conversation.uploads.push({ ...result, id: crypto.randomUUID(), createdAt: Date.now() })
      if (!conversation.messages.length) conversation.title = result.fileName
      conversation.updatedAt = Date.now()
    } catch (error) { uploadError.value = errorText(error) }
    finally { uploading.value = false; uploadingName.value = ''; persist() }
  }
  async function startAiOps() {
    if (busy.value) return
    aiOpsRunning.value = true
    const conversation = newConversation('ai-ops')
    conversation.title = `AI Ops · ${new Date().toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })}`
    conversations.value.unshift(conversation)
    activeId.value = conversation.id
    const current = active.value
    pendingId.value = current.id
    const answer = answerMessage(current, 'stream')
    try {
      await requestAiOps(chunk => { answer.content += chunk })
      if (!answer.content.trim()) throw new Error('AI Ops 未返回分析报告。')
    } catch (error) { answer.failed = true; answer.error = errorText(error) }
    finally {
      answer.streaming = false
      current.updatedAt = Date.now()
      pendingId.value = null
      aiOpsRunning.value = false
      persist()
    }
  }

  return { activeId, active, history, pendingId, aiOpsRunning, uploading, uploadingName, uploadError,
    storageNotice, busy, createConversation, selectConversation, setMode, send, retry, upload, startAiOps }
}
