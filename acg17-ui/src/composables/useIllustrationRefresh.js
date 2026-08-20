import { readonly, ref } from 'vue'

const revision = ref(0)

export function notifyIllustrationRefresh() {
  revision.value += 1
}

export function useIllustrationRefresh() {
  return readonly(revision)
}
