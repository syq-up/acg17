<template>
  <section>
    <div class="side-btn-group left-btn-group">
      <button
        type="button"
        class="side-btn"
        :disabled="illustration.list.length === 0 || sortMode || randomPending"
        :aria-busy="randomPending"
        aria-label="随机插画"
        title="随机插画"
        @click="randomArtwork($event)"
      >
        <icon icon="#icon-random"></icon>
      </button>
      <button
        v-if="!isRecycle"
        type="button"
        class="side-btn sort-mode-button"
        :class="{ active: sortMode }"
        :disabled="illustration.list.length < 2 || sortPending"
        :aria-pressed="sortMode"
        :aria-label="sortMode ? '退出排序模式' : '进入排序模式'"
        :title="sortMode ? '完成排序' : '调整插画顺序'"
        @click="toggleSortMode"
      >
        <icon icon="#icon-sort"></icon>
      </button>
      <button
        v-show="showBackToTop"
        type="button"
        class="side-btn mobile-back-to-top"
        aria-label="返回顶部"
        title="返回顶部"
        @click="scrollToTop"
      >
        <icon icon="#icon-sort-asc"></icon>
      </button>
    </div>
    <button
      v-show="showBackToTop"
      type="button"
      class="side-btn right-btn"
      aria-label="返回顶部"
      title="返回顶部"
      @click="scrollToTop"
    >
      <icon icon="#icon-sort-asc"></icon>
    </button>

    <div v-if="sortMode" class="sort-hint" role="status">
      拖动插画调整顺序，再次点击排序按钮即可退出
    </div>

    <div
      ref="galleryContainer"
      class="gallery-container"
      v-infinite-scroll="loadArtworks"
      :infinite-scroll-disabled="illustration.loading || illustration.error || illustration.disabled || sortMode"
    >
      <div
        v-for="(row, rowIndex) in layoutRows"
        :key="rowIndex"
        class="gallery-row"
        :style="{ height: row.height + 'px', gap: row.gap + 'px', marginBottom: row.gap + 'px' }"
      >
        <div
          v-for="entry in row.items"
          :key="entry.item.id ?? entry.index"
          class="gallery-item"
          :class="{
            'is-dragging': drag.isDrag && entry.item === drag.sourceItem,
            'is-sorting': sortMode,
          }"
          :style="{ width: entry.width + 'px' }"
          :draggable="sortMode && !sortPending"
          @dragstart="handleDragStart($event, entry.index)"
          @dragover="handleDragover"
          @dragenter="handleDragenter($event, entry.index)"
          @drop="handleDrop($event, entry.index)"
          @dragend="handleDragend"
        >
          <button
            type="button"
            class="gallery-preview-button"
            :aria-disabled="sortMode"
            :tabindex="sortMode ? -1 : 0"
            :aria-label="`预览第 ${entry.index + 1} 张插画`"
            @click="openPreview(entry.index, $event)"
          >
            <img
              :src="withMediaStyle(entry.item.url, 'small')"
              class="gallery-img"
              :alt="`第 ${entry.index + 1} 张插画`"
              loading="lazy"
              decoding="async"
            >
          </button>

          <div v-if="!sortMode && !drag.isDrag" class="item-action-overlay">
            <button
              v-if="!isRecycle"
              type="button"
              class="action delete"
              :disabled="isArtworkPending(entry.item.id)"
              :aria-busy="isArtworkPending(entry.item.id)"
              aria-label="删除插画"
              title="删除插画"
              @click.stop="deleteArtwork(entry.item)"
            >
              <icon icon="#icon-delete"></icon>
            </button>
            <button
              v-else
              type="button"
              class="action restore"
              :disabled="isArtworkPending(entry.item.id)"
              :aria-busy="isArtworkPending(entry.item.id)"
              aria-label="恢复插画"
              title="恢复插画"
              @click.stop="restoreArtwork(entry.item)"
            >
              <icon icon="#icon-restore"></icon>
            </button>
          </div>

          <div v-if="sortMode" class="sort-overlay" aria-hidden="true">
            <icon icon="#icon-sort"></icon>
          </div>
        </div>
      </div>
    </div>

    <div v-if="illustration.error" class="gallery-state gallery-error" role="alert">
      <span>插画加载失败</span>
      <button type="button" @click="retryLoadArtworks">重试</button>
    </div>
    <div
      v-else-if="!illustration.loading && illustration.disabled && illustration.list.length === 0"
      class="gallery-state gallery-empty"
    >
      {{ isRecycle ? '回收站中暂无插画' : '暂无插画' }}
    </div>
    <LoadingHeart v-show="illustration.loading"></LoadingHeart>
  </section>

  <acg17-footer v-if="illustration.disabled"></acg17-footer>

  <div
    v-if="preview.show && selectedArtwork"
    class="preview-layer"
    role="dialog"
    aria-modal="true"
    aria-label="插画预览"
  >
    <button type="button" class="preview-shade" aria-label="关闭插画预览" @click="closePreview"></button>
    <img
      v-show="!preview.imageError"
      class="preview-artwork"
      :src="selectedArtwork.url"
      :alt="preview.standaloneArtwork ? '随机插画' : `第 ${preview.index + 1} 张插画`"
      :title="preview.standaloneArtwork ? '双击切换随机插画' : ''"
      decoding="async"
      @load="handlePreviewLoad"
      @error="handlePreviewError"
      @dblclick.stop="refreshRandomPreview"
      @touchstart.stop="touchstart"
      @touchmove.stop="touchmove"
      @touchend.stop="touchend"
    >
    <div v-if="preview.imageLoading" class="preview-status" role="status">正在加载原图…</div>
    <div v-else-if="preview.imageError" class="preview-status preview-error" role="alert">原图加载失败</div>
    <button
      ref="previewCloseButton"
      type="button"
      class="preview-control close-shade-btn"
      aria-label="关闭插画预览"
      title="关闭"
      @click="closePreview"
    >
      <icon icon="#icon-close"></icon>
    </button>
    <button
      v-if="!preview.standaloneArtwork && preview.index > 0"
      type="button"
      class="preview-control last-artwork-btn"
      aria-label="上一张插画"
      title="上一张"
      @click="lastPage"
    >
      <icon icon="#icon-left"></icon>
    </button>
    <button
      v-if="!preview.standaloneArtwork && preview.index < illustration.list.length - 1"
      type="button"
      class="preview-control next-artwork-btn"
      aria-label="下一张插画"
      title="下一张"
      @click="nextPage"
    >
      <icon icon="#icon-right"></icon>
    </button>
    <div v-if="!preview.standaloneArtwork" class="preview-counter" aria-live="polite">
      {{ preview.index + 1 }} / {{ illustration.list.length }}
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import server from '@/util/request'
import LoadingHeart from '@/components/LoadingHeart'
import Acg17Footer from '@/components/Acg17Footer'
import { useRecycleState, loadData } from '@/composables/useRecycleState'
import { useBackToTop } from '@/composables/useBackToTop'
import { useIllustrationRefresh } from '@/composables/useIllustrationRefresh'
import { withMediaStyle } from '@/util/media'
import { createJustifiedRows } from '@/utils/justifiedGallery.mjs'

defineOptions({ name: 'Illustration' })

const { isRecycle } = useRecycleState('illustration')
const illustrationRefreshRevision = useIllustrationRefresh()
const { showBackToTop, scrollToTop } = useBackToTop()

const illustration = reactive({
  currentPage: 0,
  list: [],
  loading: false,
  error: false,
  disabled: false,
  total: 0,
})

const galleryContainer = ref(null)
const containerWidth = ref(0)
const layoutRows = computed(() => createJustifiedRows(illustration.list, containerWidth.value))
let resizeObserver = null
let artworkRequestVersion = 0

function updateWidth() {
  if (galleryContainer.value) {
    containerWidth.value = galleryContainer.value.clientWidth
  }
}

async function loadArtworks() {
  if (illustration.loading || illustration.error || illustration.disabled || sortMode.value) return

  const requestVersion = artworkRequestVersion
  const pageNum = illustration.currentPage + 1
  illustration.loading = true
  illustration.error = false

  try {
    const response = await loadData({
      basePath: '/illustration',
      isRecycle: isRecycle.value,
      pageNum,
      server,
    })
    if (requestVersion !== artworkRequestVersion) return

    const pageData = response.data || {}
    const records = Array.isArray(pageData.records) ? pageData.records : []
    const existingIds = new Set(illustration.list.map(artwork => artwork.id))
    illustration.list.push(...records.filter(artwork => !existingIds.has(artwork.id)))
    illustration.currentPage = Number(pageData.current) || pageNum

    const total = Number(pageData.total)
    if (Number.isFinite(total) && total >= 0) {
      illustration.total = total
      illustration.disabled = records.length === 0 || illustration.list.length >= total
    } else {
      illustration.disabled = records.length === 0
    }
  } catch (error) {
    if (requestVersion !== artworkRequestVersion) return
    illustration.error = true
    console.error('获取插画列表失败:', error)
  } finally {
    if (requestVersion === artworkRequestVersion) {
      illustration.loading = false
    }
  }
}

function retryLoadArtworks() {
  if (illustration.loading) return
  illustration.error = false
  loadArtworks()
}

function resetArtworkList() {
  artworkRequestVersion += 1
  closePreview(false)
  resetDragState()
  sortMode.value = false
  illustration.currentPage = 0
  illustration.list = []
  illustration.loading = false
  illustration.error = false
  illustration.disabled = false
  illustration.total = 0
  loadArtworks()
}

watch(isRecycle, resetArtworkList)
watch(illustrationRefreshRevision, () => {
  if (!isRecycle.value) resetArtworkList()
})

const pendingArtworkIds = reactive(new Set())

function isArtworkPending(id) {
  return pendingArtworkIds.has(id)
}

function removeArtworkAndBackfill(id) {
  const index = illustration.list.findIndex(artwork => artwork.id === id)
  if (index !== -1) illustration.list.splice(index, 1)
  illustration.total = Math.max(0, illustration.total - 1)

  if (illustration.list.length >= illustration.total) {
    illustration.disabled = true
    return
  }

  if (illustration.currentPage > 0) {
    illustration.currentPage -= 1
  }
  illustration.disabled = false
  loadArtworks()
}

async function deleteArtwork(artwork) {
  if (!artwork?.id || pendingArtworkIds.has(artwork.id)) return

  const operationRecycleState = isRecycle.value
  pendingArtworkIds.add(artwork.id)
  try {
    const response = await server.delete(`/illustration/${artwork.id}`)
    if (response?.code !== 200) throw new Error('删除插画失败')

    if (isRecycle.value === operationRecycleState) {
      removeArtworkAndBackfill(artwork.id)
    } else {
      resetArtworkList()
    }
    ElMessage.success('插画已删除')
  } catch (error) {
    console.error('删除插画失败:', error)
    ElMessage.error('删除插画失败，请重试')
  } finally {
    pendingArtworkIds.delete(artwork.id)
  }
}

async function restoreArtwork(artwork) {
  if (!artwork?.id || pendingArtworkIds.has(artwork.id)) return

  const operationRecycleState = isRecycle.value
  pendingArtworkIds.add(artwork.id)
  try {
    const response = await server.put(`/illustration/${artwork.id}/restore`)
    if (response?.code !== 200) throw new Error('恢复插画失败')

    if (isRecycle.value === operationRecycleState) {
      removeArtworkAndBackfill(artwork.id)
    } else {
      resetArtworkList()
    }
    ElMessage.success('插画已恢复')
  } catch (error) {
    console.error('恢复插画失败:', error)
    ElMessage.error('恢复插画失败，请重试')
  } finally {
    pendingArtworkIds.delete(artwork.id)
  }
}

const previewCloseButton = ref(null)
const preview = reactive({
  show: false,
  index: 0,
  standaloneArtwork: null,
  startX: 0,
  moveX: 0,
  imageLoading: false,
  imageError: false,
})
const selectedArtwork = computed(() => preview.standaloneArtwork || illustration.list[preview.index] || null)
let previewTriggerElement = null
let bodyOverflowBeforePreview = ''

function openPreview(index, event) {
  if (sortMode.value || !illustration.list[index]) return

  preview.standaloneArtwork = null
  preview.index = index
  showPreview(event?.currentTarget)
}

function openStandalonePreview(artwork, triggerElement) {
  if (!artwork?.url) return

  preview.standaloneArtwork = artwork
  preview.index = -1
  showPreview(triggerElement)
}

function showPreview(triggerElement) {
  previewTriggerElement = triggerElement instanceof HTMLElement ? triggerElement : null
  preview.imageLoading = true
  preview.imageError = false
  preview.show = true
  bodyOverflowBeforePreview = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  nextTick(() => previewCloseButton.value?.focus())
}

function replaceStandalonePreview(artwork) {
  if (!artwork?.url) return

  preview.standaloneArtwork = artwork
  preview.imageLoading = true
  preview.imageError = false
}

function closePreview(restoreFocus = true) {
  if (!preview.show) return

  const focusTarget = previewTriggerElement
  preview.show = false
  preview.imageLoading = false
  preview.imageError = false
  preview.standaloneArtwork = null
  preview.startX = 0
  preview.moveX = 0
  previewTriggerElement = null
  document.body.style.overflow = bodyOverflowBeforePreview
  bodyOverflowBeforePreview = ''

  if (restoreFocus) {
    nextTick(() => {
      if (focusTarget?.isConnected) focusTarget.focus()
    })
  }
}

function setPreviewIndex(index) {
  if (preview.standaloneArtwork || index < 0 || index >= illustration.list.length) return
  preview.index = index
  preview.imageLoading = true
  preview.imageError = false
}

function lastPage() {
  setPreviewIndex(preview.index - 1)
}

function nextPage() {
  setPreviewIndex(preview.index + 1)
}

function handlePreviewLoad() {
  preview.imageLoading = false
  preview.imageError = false
}

function handlePreviewError() {
  preview.imageLoading = false
  preview.imageError = true
}

function handlePageKeydown(event) {
  if (!preview.show) return

  switch (event.key) {
    case 'Escape':
      event.preventDefault()
      closePreview()
      break
    case 'ArrowLeft':
      event.preventDefault()
      lastPage()
      break
    case 'ArrowRight':
      event.preventDefault()
      nextPage()
      break
  }
}

function touchstart(event) {
  preview.startX = 0
  preview.moveX = 0
  if (event.touches.length === 1) {
    preview.startX = event.touches[0].clientX
  }
}

function touchmove(event) {
  if (event.touches.length === 1 && preview.startX !== 0) {
    preview.moveX = event.touches[0].clientX - preview.startX
  }
}

function touchend() {
  if (preview.moveX > 75) {
    lastPage()
  } else if (preview.moveX < -75) {
    nextPage()
  }
  preview.startX = 0
  preview.moveX = 0
}

const sortMode = ref(false)
const sortPending = ref(false)
const randomPending = ref(false)
const drag = reactive({
  isDrag: false,
  originalList: [],
  dragIndex: -1,
  dragenterIndex: -1,
  dropIndex: -1,
  sourceItem: null,
})

function toggleSortMode() {
  if (sortPending.value || illustration.list.length < 2 || isRecycle.value) return
  sortMode.value = !sortMode.value
}

function resetDragState() {
  drag.isDrag = false
  drag.originalList = []
  drag.dragIndex = -1
  drag.dragenterIndex = -1
  drag.dropIndex = -1
  drag.sourceItem = null
}

function restoreOriginalOrder(originalList) {
  illustration.list.splice(0, illustration.list.length, ...originalList)
}

function handleDragStart(event, index) {
  if (!sortMode.value || sortPending.value) {
    event.preventDefault()
    return
  }

  drag.isDrag = true
  drag.originalList = [...illustration.list]
  drag.dragIndex = index
  drag.dragenterIndex = index
  drag.dropIndex = -1
  drag.sourceItem = illustration.list[index]
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', String(drag.sourceItem.id))
}

function handleDragover(event) {
  if (!drag.isDrag) return
  event.preventDefault()
  event.dataTransfer.dropEffect = 'move'
}

function handleDragenter(event, index) {
  if (!drag.isDrag || index === drag.dragenterIndex) return
  event.preventDefault()

  const [sourceItem] = illustration.list.splice(drag.dragenterIndex, 1)
  illustration.list.splice(index, 0, sourceItem)
  drag.dragenterIndex = index
}

function handleDrop(event, index) {
  if (!drag.isDrag) return
  event.preventDefault()
  drag.dropIndex = index
}

async function handleDragend(event) {
  event.preventDefault()
  if (!drag.isDrag) return

  const originalList = drag.originalList
  const dragIndex = drag.dragIndex
  const dropIndex = drag.dropIndex
  const validDrop = drag.dragenterIndex === dropIndex && dropIndex >= 0
  const changed = validDrop && dropIndex !== dragIndex
  const source = originalList[dragIndex]
  const target = originalList[dropIndex]
  const requestVersion = artworkRequestVersion
  resetDragState()

  if (!changed || !source || !target) {
    if (!validDrop) restoreOriginalOrder(originalList)
    return
  }

  sortPending.value = true
  try {
    const response = await server.post('/illustration/reorder', {
      id: source.id,
      targetId: target.id,
    })
    if (response?.code !== 200) throw new Error('插画排序失败')
    ElMessage.success('插画顺序已更新')
  } catch (error) {
    if (requestVersion === artworkRequestVersion) {
      restoreOriginalOrder(originalList)
    }
    console.error('插画排序失败:', error)
    ElMessage.error('插画排序失败，请重试')
  } finally {
    sortPending.value = false
  }
}

async function requestRandomArtwork() {
  if (randomPending.value) return null

  randomPending.value = true
  try {
    const response = await server.get('/illustration/random', { timeout: 15000 })
    return response?.data?.url ? response.data : null
  } catch (error) {
    console.error('随机获取插画失败:', error)
    return null
  } finally {
    randomPending.value = false
  }
}

async function randomArtwork(event) {
  if (illustration.list.length === 0 || sortMode.value) return

  const triggerElement = event?.currentTarget instanceof HTMLElement ? event.currentTarget : null
  const artwork = await requestRandomArtwork()
  if (artwork) openStandalonePreview(artwork, triggerElement)
}

async function refreshRandomPreview() {
  if (!preview.show || !preview.standaloneArtwork) return

  const artwork = await requestRandomArtwork()
  if (artwork) replaceStandalonePreview(artwork)
}

onMounted(() => {
  updateWidth()
  resizeObserver = new ResizeObserver(updateWidth)
  if (galleryContainer.value) resizeObserver.observe(galleryContainer.value)
  document.addEventListener('keydown', handlePageKeydown)
  loadArtworks()
})

onUnmounted(() => {
  artworkRequestVersion += 1
  resizeObserver?.disconnect()
  document.removeEventListener('keydown', handlePageKeydown)
  closePreview(false)
})
</script>

<style scoped>
section {
  --page-side-actions-z-index: 8;
  margin: 84px auto 20px;
  max-width: 1380px;
  min-height: calc(100vh - 104px - 200px);
}

.side-btn.active {
  background-color: #409eff;
  color: #ffffff;
}

.sort-hint {
  box-sizing: border-box;
  margin-bottom: 12px;
  padding: 9px 14px;
  border: 1px solid #b3d8ff;
  border-radius: 8px;
  background: #ecf5ff;
  color: #337ecc;
  font-size: 14px;
  text-align: center;
}

.gallery-container {
  position: relative;
  width: 100%;
  margin: 0 auto;
}

.gallery-row {
  display: flex;
}

.gallery-item {
  position: relative;
  flex: 0 0 auto;
  height: 100%;
  overflow: hidden;
  border-radius: 4px;
  background-color: #252525;
  transition: filter 0.2s, box-shadow 0.2s;
}

.gallery-item.is-dragging {
  opacity: 0.06;
}

.gallery-item.is-sorting {
  cursor: grab;
  box-shadow: inset 0 0 0 2px rgba(64, 158, 255, 0.8);
}

.gallery-item.is-sorting:active {
  cursor: grabbing;
}

.gallery-item:not(.is-sorting):hover {
  filter: brightness(0.9);
}

.gallery-preview-button {
  display: block;
  width: 100%;
  height: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.gallery-preview-button[aria-disabled="true"] {
  cursor: inherit;
}

.gallery-preview-button:focus-visible {
  outline: 3px solid #409eff;
  outline-offset: -3px;
}

.gallery-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-action-overlay {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

.gallery-item:hover .item-action-overlay,
.item-action-overlay:focus-within {
  opacity: 1;
}

.action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 0;
  border-radius: 0 0 0 6px;
  color: #ffffff;
  cursor: pointer;
  pointer-events: auto;
}

.action.delete {
  background-color: #f56c6c;
}

.action.restore {
  background-color: #409eff;
}

.action:hover:not(:disabled) {
  filter: brightness(1.08);
}

.action:disabled {
  cursor: wait;
  opacity: 0.65;
}

.action:focus-visible {
  outline: 3px solid #ffffff;
  outline-offset: -4px;
}

.action .icon {
  width: 28px;
  height: 28px;
  fill: currentColor;
}

.sort-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.12);
  color: #ffffff;
  pointer-events: none;
}

.sort-overlay .icon {
  width: 36px;
  height: 36px;
  padding: 8px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.3);
  fill: currentColor;
}

.gallery-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 260px;
  color: #909399;
  font-size: 15px;
}

.gallery-error {
  color: #f56c6c;
}

.gallery-error button {
  padding: 7px 14px;
  border: 1px solid #f5c6cb;
  border-radius: 6px;
  background: #ffffff;
  color: #c0392b;
  font: inherit;
  cursor: pointer;
}

.preview-layer {
  position: fixed;
  inset: 0;
  z-index: 18;
}

.preview-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  padding: 0;
  border: 0;
  background-color: rgba(0, 0, 0, 0.6);
  cursor: zoom-out;
}

.preview-artwork {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 1;
  display: block;
  max-width: calc(100vw - 176px);
  max-height: calc(100vh - 56px);
  object-fit: contain;
  transform: translate(-50%, -50%);
  touch-action: pan-y pinch-zoom;
  user-select: none;
}

.preview-control {
  position: fixed;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  color: #ffffff;
  background: rgba(0, 0, 0, 0.2);
  cursor: pointer;
  transition: background-color 0.2s, transform 0.2s;
}

.preview-control:hover {
  background: rgba(0, 0, 0, 0.58);
  transform: scale(1.08);
}

.preview-control:focus-visible {
  outline: 3px solid #409eff;
  outline-offset: 3px;
}

.preview-control .icon {
  width: 32px;
  height: 32px;
  fill: currentColor;
}

.close-shade-btn {
  top: 20px;
  right: 20px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.last-artwork-btn,
.next-artwork-btn {
  top: 50%;
  width: 52px;
  height: 92px;
  border-radius: 10px;
  transform: translateY(-50%);
}

.last-artwork-btn:hover,
.next-artwork-btn:hover {
  transform: translateY(-50%) scale(1.08);
}

.last-artwork-btn {
  left: 20px;
}

.next-artwork-btn {
  right: 20px;
}

.preview-counter {
  position: fixed;
  bottom: 18px;
  left: 50%;
  z-index: 2;
  padding: 6px 12px;
  border-radius: 999px;
  color: #ffffff;
  background: rgba(0, 0, 0, 0.4);
  font-size: 14px;
  transform: translateX(-50%);
}

.preview-status {
  position: fixed;
  top: 50%;
  left: 50%;
  z-index: 2;
  padding: 10px 16px;
  border-radius: 8px;
  color: #ffffff;
  background: rgba(0, 0, 0, 0.58);
  font-size: 14px;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.preview-error {
  color: #fbc4c4;
}

@media (hover: none) {
  .sort-mode-button {
    display: none;
  }
}

@media (max-width: 767px) {
  section {
    padding-bottom: calc(76px + env(safe-area-inset-bottom, 0px));
  }

  .preview-artwork {
    max-width: 100vw;
    max-height: 100vh;
  }

  .close-shade-btn {
    top: 12px;
    right: 12px;
    width: 44px;
    height: 44px;
  }

  .last-artwork-btn,
  .next-artwork-btn {
    width: 42px;
    height: 72px;
  }

  .last-artwork-btn {
    left: 8px;
  }

  .next-artwork-btn {
    right: 8px;
  }
}
</style>
