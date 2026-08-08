<template>
  <svg viewBox="0 0 40 40" :width="size" :height="size">
    <!-- 知识图谱Logo -->
    <circle cx="10" cy="10" r="3" :fill="primaryColor"/>
    <circle cx="30" cy="10" r="3" :fill="primaryColor"/>
    <circle cx="20" cy="30" r="3" :fill="accentColor"/>
    <line x1="10" y1="10" x2="30" y2="10" :stroke="primaryColor" stroke-width="2"/>
    <line x1="10" y1="10" x2="20" y2="30" :stroke="accentColor" stroke-width="2"/>
    <line x1="30" y1="10" x2="20" y2="30" :stroke="accentColor" stroke-width="2"/>
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  size: {
    type: Number,
    default: 40
  },
  primaryColor: {
    type: String,
    default: '#3B82F6'
  },
  accentColor: {
    type: String,
    default: '#10B981'
  }
})
</script>

<style scoped>
svg {
  display: inline-block;
}
</style>