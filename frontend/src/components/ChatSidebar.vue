<script setup lang="ts">
import { computed } from 'vue'
import { LoaderCircle, PanelLeftClose, PanelLeftOpen, SquarePen, Workflow } from 'lucide-vue-next'
import type { Conversation } from '../composables/useChat'
const props = defineProps<{ open: boolean; conversations: Conversation[]; activeId: string; pendingId: string | null; locked: boolean }>()
const emit = defineEmits<{ toggle: []; create: []; select: [id: string] }>()
const groups = computed(() => {
  const today = new Date().setHours(0, 0, 0, 0)
  const week = today - 6 * 86_400_000
  const result = [
    { title: '今天', conversations: [] as Conversation[] },
    { title: '最近 7 天', conversations: [] as Conversation[] },
    { title: '更早', conversations: [] as Conversation[] },
  ]
  for (const conversation of props.conversations) {
    result[conversation.updatedAt >= today ? 0 : conversation.updatedAt >= week ? 1 : 2]!.conversations.push(conversation)
  }
  return result.filter(group => group.conversations.length)
})
</script>

<template>
  <aside class="sidebar" :class="{ 'sidebar-expanded': open }" aria-label="对话导航">
    <template v-if="open">
      <div class="sidebar-top"><span class="brand">OnCall Agent</span><button class="icon-button" :disabled="locked" aria-label="收起侧边栏" title="收起侧边栏" @click="emit('toggle')"><PanelLeftClose :size="18" /></button></div>
      <button class="new-chat" :disabled="locked" @click="emit('create')"><SquarePen :size="18" /><span>新建对话</span><kbd>⌘ K</kbd></button>
      <div class="history-scroll">
        <div v-for="group in groups" :key="group.title" class="history-group"><h2>{{ group.title }}</h2>
          <div v-for="conversation in group.conversations" :key="conversation.id" class="history-row" :class="{ selected: activeId === conversation.id }"><button class="history-item" :disabled="locked" :aria-current="activeId === conversation.id ? 'page' : undefined" :title="conversation.title" @click="emit('select', conversation.id)"><LoaderCircle v-if="pendingId === conversation.id" :size="13" class="spin history-icon" /><Workflow v-else-if="conversation.kind === 'ai-ops'" :size="14" class="history-icon" /><span>{{ conversation.title }}</span></button></div>
        </div>
        <p v-if="!conversations.length" class="history-empty">你的对话会显示在这里</p>
      </div>
      <p class="history-storage-note">对话历史保存在此浏览器</p>
    </template>
    <div v-else class="collapsed-navigation"><button class="icon-button" :disabled="locked" aria-label="展开侧边栏" title="展开侧边栏" @click="emit('toggle')"><PanelLeftOpen :size="20" /></button><button class="icon-button" :disabled="locked" aria-label="新建对话" title="新建对话" @click="emit('create')"><SquarePen :size="20" /></button></div>
  </aside>
</template>
