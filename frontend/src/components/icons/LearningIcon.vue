<template>
  <svg viewBox="0 0 24 24" :width="size" :height="size">
    <!-- 学习图标 -->
    <path 
      d="M12 3L1 9L12 15L21 9L12 3Z" 
      :fill="color"
      opacity="0.8"
    />
    <path 
      d="M1 9V17L12 23L21 17V9" 
      :stroke="color"
      stroke-width="2"
      fill="none"
    />
    <circle cx="12" cy="15" r="2" :fill="color"/>
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
    default: '#2563EB'
  }
})
</script>

<style scoped>
svg {
  display: inline-block;
}
</style>