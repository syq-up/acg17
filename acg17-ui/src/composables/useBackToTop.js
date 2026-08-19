import { onActivated, onDeactivated, onMounted, onUnmounted, ref } from 'vue'

export function useBackToTop(threshold = 500) {
  const showBackToTop = ref(false)
  let listening = false

  function updateVisibility() {
    showBackToTop.value = window.scrollY > threshold
  }

  function startListening() {
    if (!listening) {
      window.addEventListener('scroll', updateVisibility)
      listening = true
    }
    updateVisibility()
  }

  function stopListening() {
    if (!listening) return
    window.removeEventListener('scroll', updateVisibility)
    listening = false
  }

  function scrollToTop() {
    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    })
  }

  onMounted(startListening)
  onActivated(startListening)
  onDeactivated(stopListening)
  onUnmounted(stopListening)

  return {
    showBackToTop,
    scrollToTop,
  }
}
