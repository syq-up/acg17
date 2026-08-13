<template>
  <button 
    :class="isRecycle ? 'active' : ''"
    @click="toggleRecycle"
  >
    回收站
  </button>
  <button 
    :class="!isRecycle ? 'active' : ''"
    @click="() => setRecycle(false)"
  >
    全部
  </button>
</template>

<script>
import { useRecycleState } from '@/composables/useRecycleState'

export default {
  name: 'RecycleToggleButtons',
  props: {
    pageName: {
      type: String,
      required: true,
      // validator: (value) => ['illustration', 'manga', 'anime', 'novel'].includes(value)
      validator: (value) => ['illustration', 'manga', 'game', 'novel'].includes(value)
    }
  },
  setup(props) {
    const { isRecycle, toggleRecycle, setRecycle } = useRecycleState(props.pageName)

    return {
      isRecycle,
      toggleRecycle,
      setRecycle
    }
  }
}
</script>

<style scoped>
/* 按钮样式继承自父组件的全局样式 */
</style>