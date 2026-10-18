<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { ArrowUp, ChevronDown, FileText, LoaderCircle, Plus } from 'lucide-vue-next'
import type { Attachment, ChatMode } from '../composables/useChat'

const props = defineProps<{
  busy: boolean
  draft: string
  mode: ChatMode
  uploading: boolean
  uploadingName: string
  uploads: Attachment[]
  uploadError: string
  compact?: boolean
}>()
const emit = defineEmits<{ send: [text: string]; 'update:draft': [value: string]; 'update:mode': [value: ChatMode]; upload: [file: File] }>()
const textarea = ref<HTMLTextAreaElement>()
const fileInput = ref<HTMLInputElement>()
function resize() {
  if (!textarea.value) return
  textarea.value.style.height = 'auto'
  textarea.value.style.height = `${Math.min(textarea.value.scrollHeight, 190)}px`
}
watch(() => props.draft, () => nextTick(resize))
function submit() {
  if (!props.busy && props.draft.trim()) emit('send', props.draft)
}
function keydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing && event.keyCode !== 229) {
    event.preventDefault()
    submit()
  }
}
function chooseFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file && !props.busy) emit('upload', file)
  input.value = ''
}
function fileSize(size: number) { return size < 1024 ? `${size} B` : `${(size / 1024).toFixed(1)} KB` }
function focus() { textarea.value?.focus() }
defineExpose({ focus })
</script>

<template>
  <div class="composer-container">
    <div v-if="uploads.length || uploading" class="upload-list" aria-label="已上传的文档">
      <div v-for="file in uploads" :key="file.id" class="upload-chip"><FileText :size="18" /><div><strong :title="file.fileName">{{ file.fileName }}</strong><span>{{ fileSize(file.fileSize) }} · 已上传</span></div></div>
      <div v-if="uploading" class="upload-chip" role="status"><LoaderCircle :size="18" class="spin" /><div><strong>{{ uploadingName }}</strong><span>正在上传，请稍候…</span></div></div>
    </div>
    <form class="composer" :class="{ 'composer-compact': compact }" @submit.prevent="submit">
      <textarea ref="textarea" :value="draft" :disabled="uploading" rows="1" aria-label="输入你的问题" placeholder="询问任何问题" @input="emit('update:draft', ($event.target as HTMLTextAreaElement).value)" @keydown="keydown" />
      <div class="composer-toolbar">
        <input ref="fileInput" class="file-input" type="file" accept=".md,text/markdown" aria-label="选择 Markdown 文件" tabindex="-1" @change="chooseFile" />
        <button class="icon-button upload-button" type="button" :disabled="busy" aria-label="上传 Markdown 文件" title="上传 .md 文档" @click="fileInput?.click()"><LoaderCircle v-if="uploading" :size="20" class="spin" /><Plus v-else :size="22" /></button>
        <div class="composer-right">
          <div class="mode-select"><select :value="mode" :disabled="busy" aria-label="对话模式" @change="emit('update:mode', ($event.target as HTMLSelectElement).value as ChatMode)"><option value="quick">快速对话</option><option value="stream">流式对话</option></select><ChevronDown :size="14" /></div>
          <button class="send-button" type="submit" :disabled="busy || !draft.trim()" :aria-label="busy ? '正在等待处理完成' : '发送消息'" :title="busy ? '正在等待处理完成' : '发送消息'"><LoaderCircle v-if="busy && !uploading" :size="19" class="spin" /><ArrowUp v-else :size="21" :stroke-width="2.2" /></button>
        </div>
      </div>
    </form>
    <p v-if="uploadError" class="upload-error" role="alert">{{ uploadError }}</p>
  </div>
</template>
