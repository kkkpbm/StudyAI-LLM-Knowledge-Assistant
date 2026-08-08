<template>
  <svg viewBox="0 0 24 24" :width="size" :height="size">
    <!-- 书本图标 -->
    <path 
      d="M4 4H20V20H4V4Z" 
      :stroke="color"
      stroke-width="2"
      fill="none"
    />
    <line x1="12" y1="4" x2="12" y2="20" :stroke="color" stroke-width="1"/>
    <path 
      d="M6 8L10 6V18L6 16V8Z" 
      :fill="color"
      opacity="0.3"
    />
    <path 
      d="M14 6L18 8V16L14 18V6Z" 
      :fill="color"
      opacity="0.3"
    />
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
    default: '#F59E0B'
  }
})
</script>

<style scoped>
svg {
  display: inline-block;
}
</style>