<script setup lang="ts">
import { computed, ref } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { Activity, Copy, Check, RotateCcw, CircleAlert, LoaderCircle, Workflow } from 'lucide-vue-next'
import type { Message } from '../composables/useChat'

const props = defineProps<{ message: Message; canRetry?: boolean }>()
const emit = defineEmits<{ retry: []; notice: [message: string] }>()
const copied = ref(false)
const html = computed(() => {
  const rendered = DOMPurify.sanitize(marked.parse(props.message.content, { async: false, breaks: true }) as string)
  const document = new DOMParser().parseFromString(rendered, 'text/html')
  document.querySelectorAll('a').forEach(link => {
    link.target = '_blank'
    link.rel = 'noopener noreferrer'
  })
  return document.body.innerHTML
})
async function copy() {
  try {
    await navigator.clipboard.writeText(props.message.content)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch { emit('notice', '复制失败，请手动选择文字复制。') }
}
</script>

<template>
  <article v-if="message.role === 'user'" class="message message-user" aria-label="你的消息">
    <div class="user-bubble">{{ message.content }}</div>
  </article>
  <article v-else class="message message-assistant" :class="{ 'message-failed': message.failed }" :aria-label="message.failed ? '回复失败' : 'OnCall 的回复'">
    <div class="assistant-avatar"><Workflow v-if="message.source === 'ai-ops'" :size="17" /><Activity v-else :size="18" /></div>
    <div class="assistant-body">
      <div class="assistant-label">{{ message.source === 'ai-ops' ? 'AI Ops' : 'OnCall' }}<span v-if="message.streaming">{{ message.mode === 'stream' ? '正在接收' : '正在思考' }}</span></div>
      <div v-if="message.content" class="markdown" :class="{ 'streaming-content': message.streaming }" v-html="html" />
      <div v-if="message.streaming && !message.content" class="thinking-label" role="status"><LoaderCircle :size="14" class="spin" />{{ message.source === 'ai-ops' ? '正在启动智能运维分析…' : '正在等待助手回复…' }}</div>
      <div v-if="message.failed" class="message-error"><div class="error-content" role="alert"><CircleAlert :size="17" /><p>{{ message.error || '回复未能完成，请稍后重试。' }}</p></div><button v-if="canRetry" class="retry-button" @click="emit('retry')"><RotateCcw :size="14" /> 重新尝试</button></div>
      <div v-if="message.content && !message.streaming" class="message-actions"><button class="icon-button" :aria-label="copied ? '已复制' : '复制回复'" :title="copied ? '已复制' : '复制回复'" @click="copy"><Check v-if="copied" :size="15" /><Copy v-else :size="15" /></button><span v-if="copied" class="copy-label">已复制</span></div>
    </div>
  </article>
</template>
