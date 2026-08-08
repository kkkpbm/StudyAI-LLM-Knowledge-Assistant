<template>
  <svg viewBox="0 0 24 24" :width="size" :height="size">
    <!-- AI大脑图标 -->
    <circle cx="12" cy="12" r="10" :stroke="color" stroke-width="2" fill="none"/>
    <circle cx="12" cy="12" r="4" :fill="color" opacity="0.3"/>
    <circle cx="8" cy="8" r="1.5" :fill="color"/>
    <circle cx="16" cy="8" r="1.5" :fill="color"/>
    <circle cx="8" cy="16" r="1.5" :fill="color"/>
    <circle cx="16" cy="16" r="1.5" :fill="color"/>
    <line x1="8" y1="8" x2="12" y2="12" :stroke="color" stroke-width="1"/>
    <line x1="16" y1="8" x2="12" y2="12" :stroke="color" stroke-width="1"/>
    <line x1="8" y1="16" x2="12" y2="12" :stroke="color" stroke-width="1"/>
    <line x1="16" y1="16" x2="12" y2="12" :stroke="color" stroke-width="1"/>
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  size: {
    type: Number,
    default: 24
  },
  color: {
    type: String,
    default: '#315EFB'
  }
})
</script>

<style scoped>
svg {
  display: inline-block;
}
</style>
