<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ArrowDown, LoaderCircle, PanelLeftOpen, SquarePen, Workflow } from 'lucide-vue-next'
import ChatSidebar from './components/ChatSidebar.vue'
import ChatComposer from './components/ChatComposer.vue'
import ChatMessage from './components/ChatMessage.vue'
import { useChat } from './composables/useChat'

const { active, activeId, history, pendingId, aiOpsRunning, uploading, uploadingName, uploadError,
  storageNotice, busy, createConversation, selectConversation, setMode, send, retry, upload, startAiOps } = useChat()
const sidebarOpen = ref(window.innerWidth > 800)
const drafts = ref<Record<string, string>>({})
const draft = computed({ get: () => drafts.value[activeId.value] || '', set: value => { drafts.value[activeId.value] = value } })
const composer = ref<InstanceType<typeof ChatComposer>>()
const transcript = ref<HTMLElement>()
const scrolledUp = ref(false)
const aiOpsOverlay = ref<HTMLElement>()
const toast = ref('')
let toastTimer: ReturnType<typeof setTimeout>
const composerProps = computed(() => ({ busy: busy.value, mode: active.value.mode, uploading: uploading.value,
  uploadingName: uploadingName.value, uploads: active.value.uploads, uploadError: uploadError.value }))
const modeLabel = computed(() => active.value.kind === 'ai-ops' ? 'AI Ops 报告' : active.value.mode === 'stream' ? '流式对话' : '快速对话')
const opsProgress = computed(() => active.value.kind === 'ai-ops' ? active.value.messages.at(-1)?.content.slice(-1000) || '' : '')

function notify(message: string) {
  toast.value = message
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 3000)
}
function focusComposer() { nextTick(() => composer.value?.focus()) }
function newChat() {
  if (aiOpsRunning.value || uploading.value) return
  createConversation()
  draft.value = ''
  scrolledUp.value = false
  if (window.innerWidth <= 800) sidebarOpen.value = false
  focusComposer()
}
function selectChat(id: string) {
  if (aiOpsRunning.value || uploading.value) return
  selectConversation(id)
  if (window.innerWidth <= 800) sidebarOpen.value = false
  scrolledUp.value = false
}
async function submit(text: string) {
  if (busy.value || !text.trim()) return
  draft.value = ''
  scrolledUp.value = false
  await send(text)
  focusComposer()
}
function runAiOps() {
  if (busy.value) return
  if (window.innerWidth <= 800) sidebarOpen.value = false
  scrolledUp.value = false
  void startAiOps()
}
async function scrollBottom() {
  await nextTick()
  transcript.value?.scrollTo({ top: transcript.value.scrollHeight, behavior: 'auto' })
}
function onScroll() {
  const el = transcript.value
  if (el) scrolledUp.value = el.scrollHeight - el.scrollTop - el.clientHeight > 130
}
watch([activeId, () => active.value.messages.length, () => active.value.messages.at(-1)?.content.length, pendingId], () => {
  if (!scrolledUp.value) void scrollBottom()
}, { flush: 'post' })
watch(aiOpsRunning, async running => {
  if (running) { await nextTick(); aiOpsOverlay.value?.focus() }
})
function shortcuts(event: KeyboardEvent) {
  if (aiOpsRunning.value) return
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); newChat() }
  if (event.key === 'Escape' && window.innerWidth <= 800) sidebarOpen.value = false
}
window.addEventListener('keydown', shortcuts)
onBeforeUnmount(() => { window.removeEventListener('keydown', shortcuts); clearTimeout(toastTimer) })
</script>

<template>
  <div class="app-root">
    <div class="app-shell" :class="{ 'sidebar-is-open': sidebarOpen }" :inert="aiOpsRunning || undefined" :aria-busy="aiOpsRunning">
      <button v-if="sidebarOpen" class="mobile-scrim" aria-label="关闭侧边栏" @click="sidebarOpen = false" />
      <ChatSidebar :open="sidebarOpen" :conversations="history" :active-id="activeId" :pending-id="pendingId" :locked="aiOpsRunning || uploading" @toggle="sidebarOpen = !sidebarOpen" @create="newChat" @select="selectChat" />
      <main class="main-panel">
        <header class="topbar">
          <div class="topbar-left"><button class="icon-button mobile-menu" aria-label="打开侧边栏" @click="sidebarOpen = true"><PanelLeftOpen :size="21" /></button><span class="app-name">OnCall Agent</span></div>
          <span class="page-mode">{{ modeLabel }}</span>
          <div class="topbar-actions"><button class="ai-ops-button" :disabled="busy" @click="runAiOps"><LoaderCircle v-if="aiOpsRunning" :size="16" class="spin" /><Workflow v-else :size="16" /><span>AI Ops</span></button><button class="icon-button header-new-chat" :disabled="uploading" aria-label="新建对话" title="新建对话" @click="newChat"><SquarePen :size="19" /></button></div>
        </header>

        <section v-if="!active.messages.length" class="welcome-stage" aria-label="开始对话">
          <div class="welcome-content"><h1>有什么可以帮忙的？</h1><ChatComposer ref="composer" v-model:draft="draft" v-bind="composerProps" @send="submit" @update:mode="setMode" @upload="upload" /><p v-if="pendingId && pendingId !== activeId" class="background-notice"><LoaderCircle :size="12" class="spin" /> 另一段对话正在回复，请稍候。</p></div>
          <p class="page-disclaimer">OnCall 可能会出错，请核实重要信息。</p>
        </section>

        <section v-else class="chat-stage" aria-label="聊天内容">
          <div ref="transcript" class="transcript" @scroll="onScroll"><div class="message-list" role="log" aria-live="polite" aria-relevant="additions text"><div class="conversation-date">{{ new Date(active.messages[0]!.createdAt).toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' }) }}<span> · {{ modeLabel }}</span></div><ChatMessage v-for="(message, index) in active.messages" :key="message.id" :message="message" :can-retry="active.kind === 'chat' && !!message.failed && index === active.messages.length - 1 && !busy" @retry="retry" @notice="notify" /></div></div>
          <div v-if="active.kind === 'chat'" class="composer-dock"><button v-if="scrolledUp" class="scroll-bottom" aria-label="回到最新消息" @click="scrolledUp = false; scrollBottom()"><ArrowDown :size="17" /></button><ChatComposer ref="composer" v-model:draft="draft" v-bind="composerProps" compact @send="submit" @update:mode="setMode" @upload="upload" /><p v-if="pendingId && pendingId !== activeId" class="background-notice"><LoaderCircle :size="12" class="spin" /> 另一段对话正在回复，请稍候。</p><p class="page-disclaimer">OnCall 可能会出错，请核实重要信息。</p></div>
          <div v-else class="report-footer"><span>AI Ops 分析记录已保存在左侧历史中</span><button class="report-new-chat" @click="newChat"><SquarePen :size="15" /> 新建对话</button></div>
        </section>
        <div v-if="storageNotice" class="storage-notice" role="status">{{ storageNotice }}</div>
      </main>
    </div>
    <Transition name="toast"><div v-if="toast" class="toast-notice" role="status">{{ toast }}</div></Transition>
    <div v-if="aiOpsRunning" ref="aiOpsOverlay" class="operation-overlay" role="dialog" aria-modal="true" aria-labelledby="ops-title" tabindex="-1"><div class="operation-card"><div class="operation-spinner"><LoaderCircle :size="28" class="spin" /></div><h2 id="ops-title">AI Ops 正在执行</h2><p role="status">正在读取告警、执行排查并生成分析报告。</p><pre v-if="opsProgress" class="operation-progress" aria-live="polite">{{ opsProgress }}</pre><p class="operation-note">请等待本次响应完成，页面将在完成或失败后自动解锁。</p></div></div>
  </div>
</template>
