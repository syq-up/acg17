<!--
 * @author shiyongqiang
 * @date 2025/9/1
 * @description 游戏页面，展示游戏列表和详情弹出框
-->
<template>
  <section>
    <div class="side-btn-group left-btn-group" :style="{ right: '50%', marginRight: (containerWidth / 2 + 30) + 'px' }">
      <div class="side-btn" @click="randomGame">
        <icon icon="#icon-random"></icon>
      </div>
      <div class="side-btn" :class="{ active: showTitleSearch || hasTitleFilter }" @click="toggleTitleSearch">
        <icon icon="#icon-search"></icon>
        <span v-if="hasTitleFilter" class="side-btn-status" aria-hidden="true"></span>
      </div>
    </div>
    <div class="side-btn right-btn" v-show="showBackToTop" @click="scrollToTop" :style="{ left: '50%', marginLeft: (containerWidth / 2 + 30) + 'px' }">
      <icon icon="#icon-sort-asc"></icon>
    </div>

    <div class="filter-container" :class="{ 'is-visible': showTitleSearch }" @transitionend="handleFilterPanelTransitionEnd">
      <div class="filter-collapse">
        <form class="filter-panel title-search-body" role="search" @submit.prevent="commitTitleSearch">
          <div class="filter-search-box">
            <icon icon="#icon-search" class="filter-search-icon"></icon>
            <input
              ref="titleSearchInput"
              v-model="titleSearchDraft"
              type="search"
              maxlength="255"
              placeholder="搜索游戏原标题或中文标题"
              aria-label="搜索游戏标题"
              autocomplete="off"
              spellcheck="false"
              @keydown.esc.prevent="closeTitleSearch"
            >
            <button
              v-if="titleSearchDraft || hasTitleFilter"
              type="button"
              class="clear-filter-search"
              aria-label="清空标题搜索"
              @click="clearTitleSearch"
            >
              <icon icon="#icon-close"></icon>
            </button>
          </div>
          <button type="submit" class="submit-title-search">搜索</button>
        </form>
      </div>
    </div>

    <ul class="game-container unselectable">
      <li v-for="(game) in gameData.list" :key="game.id" @click="showGameDetail(game)">
        <div class="game-img-container">
          <img class="game-img" :src="withMediaStyle(game.cover, 'small')" :alt="game.title">
        </div>
        <div class="game-info">
          <div class="game-title">{{ game.chineseTitle || game.title }}</div>
        </div>
      </li>
    </ul>
    <div v-if="!gameData.loading && gameData.list.length === 0" class="empty-game">
      <span>{{ emptyGameText }}</span>
      <button v-if="hasTitleFilter" type="button" @click="clearTitleSearch">清除标题搜索</button>
    </div>
  </section>

  <div v-if="showModal" class="modal-overlay" @click="closeModal">
    <div class="modal-content" @click.stop>
      <button type="button" class="close-btn" aria-label="关闭游戏详情" @click="closeModal">&times;</button>

      <div class="modal-body">
        <div class="game-detail-container">
          <div class="game-thumbnail unselectable">
            <img
              class="game-thumbnail-backdrop"
              :src="withMediaStyle(selectedGame.cover, 'medium')"
              alt=""
              aria-hidden="true"
            >
            <img
              class="game-thumbnail-image"
              :src="withMediaStyle(selectedGame.cover, 'medium')"
              :alt="selectedGame.title"
            >
          </div>

          <div class="game-basic-info">
            <div class="game-heading">
              <h2>{{ selectedGame.chineseTitle || selectedGame.title }}</h2>
            </div>

            <div v-if="selectedGame.chineseTitle" class="info-row">
              <span class="label">原名</span>
              <span class="value">{{ selectedGame.title }}</span>
            </div>

            <div class="info-row">
              <span class="label">版本号</span>
              <span class="value">{{ selectedGame.version || '-' }}</span>
            </div>

            <div class="info-row">
              <span class="label">游戏简介</span>
              <div class="game-description">{{ selectedGame.description }}</div>
            </div>
          </div>
        </div>

        <div class="game-previews unselectable" v-if="selectedGame.previewImages && selectedGame.previewImages.length > 0">
          <h3>游戏预览</h3>
          <div class="preview-images">
            <div v-for="(image, index) in selectedGame.previewImages" :key="index" class="preview-item">
              <img :src="withMediaStyle(image, 'small')" :alt="`预览图 ${index + 1}`" loading="lazy" decoding="async" @click="showImagePreview(index)" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-if="showImageModal" class="image-modal-overlay" @click="closeImageModal">
    <div class="image-modal-content" @click.stop>
      <img :src="selectedGame.previewImages[currentImageIndex]" :alt="`预览图 ${currentImageIndex + 1}`" />
      <button type="button" class="image-close-btn" aria-label="关闭图片预览" @click="closeImageModal">&times;</button>
      
      <button
        v-if="currentImageIndex > 0"
        type="button"
        class="image-nav-btn prev-btn"
        aria-label="上一张预览图"
        @click="previousImage"
      >
        <icon icon="#icon-left"></icon>
      </button>
      <button
        v-if="currentImageIndex < selectedGame.previewImages.length - 1"
        type="button"
        class="image-nav-btn next-btn"
        aria-label="下一张预览图"
        @click="nextImage"
      >
        <icon icon="#icon-right"></icon>
      </button>
      
      <div class="image-counter">
        {{ currentImageIndex + 1 }} / {{ selectedGame.previewImages.length }}
      </div>
    </div>
  </div>

  <acg17-footer v-if="gameData.disabled"></acg17-footer>
</template>

<script>
import { computed, reactive, ref, onMounted, watch, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import server from '@/util/request'
import Acg17Footer from "../components/Acg17Footer"
import Icon from "../components/Icon"
import { useRecycleState } from '@/composables/useRecycleState'
import { withMediaStyle } from '@/util/media'

function normalizeTitle(value) {
  const queryValue = Array.isArray(value) ? value[0] : value
  return String(queryValue ?? '').trim()
}

export default {
  name: "Game",
  components: {
    'acg17-footer': Acg17Footer,
    'icon': Icon,
  },
  setup() {
    const route = useRoute()
    const router = useRouter()

    // 使用全局回收站状态管理
    const { isRecycle, toggleRecycle, setRecycle } = useRecycleState('game')

    const gameData = reactive({
      currentPage: 1,
      list: [],
      loading: false,
      disabled: false,
      total: 0,
    })

    const showModal = ref(false)
    const selectedGame = ref({})
    const showImageModal = ref(false)
    const currentImageIndex = ref(0)

    const containerWidth = ref(1380)
    const showBackToTop = ref(false)
    const showTitleSearch = ref(false)
    const titleSearchInput = ref(null)
    const activeTitle = computed(() => normalizeTitle(route.query.title))
    const titleSearchDraft = ref(activeTitle.value)
    const hasTitleFilter = computed(() => activeTitle.value.length > 0)
    const emptyGameText = computed(() => (
      hasTitleFilter.value
        ? `没有找到标题包含“${activeTitle.value}”的游戏`
        : '暂无游戏'
    ))
    let resizeObserver = null
    let gameRequestVersion = 0
    let pendingFilterFocus = false

    const handleScroll = () => {
      showBackToTop.value = window.scrollY > 500
    }

    const updateWidth = () => {
      const container = document.querySelector('.game-container')
      if (container) {
        containerWidth.value = container.clientWidth
      }
    }


    // 获取游戏列表
    async function getGameList(pageNum = 1, deleted = false) {
      const requestVersion = ++gameRequestVersion
      try {
        gameData.loading = true
        gameData.list = []
        gameData.total = 0
        gameData.disabled = false

        const params = {
          pageNum,
          deleted
        }
        if (hasTitleFilter.value) params.title = activeTitle.value

        const response = await server.get('/game/list', {
          params
        })

        if (requestVersion === gameRequestVersion && response.code === 200) {
          const pageData = response.data
          gameData.list = pageData.records || []
          gameData.total = pageData.total || 0
          gameData.currentPage = pageData.current || 1
          gameData.disabled = pageData.records.length < pageData.size
        }
      } catch (error) {
        if (requestVersion !== gameRequestVersion) return
        console.error('获取游戏列表失败:', error)
      } finally {
        if (requestVersion === gameRequestVersion) gameData.loading = false
      }
    }

    function focusTitleSearchInput() {
      if (!pendingFilterFocus) return
      titleSearchInput.value?.focus({ preventScroll: true })
      pendingFilterFocus = false
    }

    function closeTitleSearch() {
      titleSearchDraft.value = activeTitle.value
      showTitleSearch.value = false
      pendingFilterFocus = false
    }

    function toggleTitleSearch() {
      if (showTitleSearch.value) {
        closeTitleSearch()
        return
      }
      pendingFilterFocus = true
      showTitleSearch.value = true
      titleSearchDraft.value = activeTitle.value
      scrollToTop()
      if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        nextTick(focusTitleSearchInput)
      }
    }

    function handleFilterPanelTransitionEnd(event) {
      if (event.target !== event.currentTarget || event.propertyName !== 'grid-template-rows') return
      if (showTitleSearch.value) nextTick(focusTitleSearchInput)
    }

    function updateTitleFilter(title) {
      const normalizedTitle = normalizeTitle(title)
      if (normalizedTitle === activeTitle.value) return
      router.push({
        path: '/acg/game',
        query: normalizedTitle ? { title: normalizedTitle } : {}
      })
    }

    function commitTitleSearch() {
      const title = normalizeTitle(titleSearchDraft.value)
      titleSearchDraft.value = title
      updateTitleFilter(title)
    }

    function clearTitleSearch() {
      titleSearchDraft.value = ''
      updateTitleFilter('')
      if (showTitleSearch.value) {
        nextTick(() => titleSearchInput.value?.focus({ preventScroll: true }))
      }
    }

    // 显示游戏详情
    function showGameDetail(game) {
      selectedGame.value = game
      showModal.value = true
      // 防止背景滚动
      document.body.style.overflow = 'hidden'
    }

    // 关闭详情弹出框
    function closeModal() {
      showModal.value = false
      selectedGame.value = {}
      // 恢复背景滚动
      document.body.style.overflow = 'auto'
    }

    // 显示图片预览
    function showImagePreview(index) {
      currentImageIndex.value = index
      showImageModal.value = true
      // 添加键盘事件监听
      document.addEventListener('keydown', handleImageKeydown)
    }

    // 关闭图片预览
    function closeImageModal() {
      showImageModal.value = false
      currentImageIndex.value = 0
      // 移除键盘事件监听
      document.removeEventListener('keydown', handleImageKeydown)
    }

    // 上一张图片
    function previousImage() {
      if (currentImageIndex.value > 0) {
        currentImageIndex.value--
      }
    }

    // 下一张图片
    function nextImage() {
      if (currentImageIndex.value < selectedGame.value.previewImages.length - 1) {
        currentImageIndex.value++
      }
    }

    // 键盘事件处理
    function handleImageKeydown(event) {
      switch (event.key) {
        case 'ArrowLeft':
          previousImage()
          break
        case 'ArrowRight':
          nextImage()
          break
        case 'Escape':
          closeImageModal()
          break
      }
    }

    // 随机获取一个游戏
    function randomGame() {
      server.get('/game/random').then(response => {
        if (response.code === 200) {
          showGameDetail(response.data)
        }
      })
    }

    function scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: "smooth"
      })
    }

    // 监听回收站状态变化，重新获取数据
    watch(isRecycle, () => {
      getGameList(1, isRecycle.value)
    })

    watch(activeTitle, title => {
      titleSearchDraft.value = title
      getGameList(1, isRecycle.value)
    })

    // 组件挂载时获取数据
    onMounted(() => {
      window.addEventListener('scroll', handleScroll)
      getGameList(1, isRecycle.value)
      nextTick(() => {
        updateWidth()
        window.addEventListener('resize', updateWidth)
        resizeObserver = new ResizeObserver(() => updateWidth())
        const container = document.querySelector('.game-container')
        if (container) resizeObserver.observe(container)
      })
    })

    onUnmounted(() => {
      window.removeEventListener('scroll', handleScroll)
      window.removeEventListener('resize', updateWidth)
      document.removeEventListener('keydown', handleImageKeydown)
      document.body.style.overflow = 'auto'
      if (resizeObserver) resizeObserver.disconnect()
    })

    return {
      gameData,
      showModal,
      selectedGame,
      showImageModal,
      currentImageIndex,
      isRecycle,
      toggleRecycle,
      setRecycle,
      showGameDetail,
      closeModal,
      showImagePreview,
      closeImageModal,
      previousImage,
      nextImage,
      getGameList,
      randomGame,
      scrollToTop,
      showBackToTop,
      containerWidth,
      showTitleSearch,
      titleSearchInput,
      titleSearchDraft,
      activeTitle,
      hasTitleFilter,
      emptyGameText,
      toggleTitleSearch,
      closeTitleSearch,
      handleFilterPanelTransitionEnd,
      commitTitleSearch,
      clearTitleSearch,
      withMediaStyle
    }
  }
}
</script>

<style scoped>
/* 响应式CSS变量 */
* {
  --column: 6;
  --width: 220px;
  --height: calc(var(--width) * 1.5 + var(--title-height));
  --row-gap: 12px;
  --title-height: 36px;
  --container-padding: 20px;
}

section {
  margin: 84px auto 20px;
  max-width: 100%;
  padding: 0 var(--container-padding);
  box-sizing: border-box;
  min-height: calc(100vh - 104px - 200px);
}

.filter-container {
  display: grid;
  grid-template-rows: 0fr;
  max-width: 1380px;
  margin: 0 auto;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
  transition:
    grid-template-rows 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    margin-bottom 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.2s ease;
}

.filter-container.is-visible {
  grid-template-rows: 1fr;
  margin-bottom: 20px;
  opacity: 1;
  pointer-events: auto;
}

.filter-collapse {
  min-height: 0;
  overflow: hidden;
}

.filter-panel {
  padding: 16px 18px;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.title-search-body {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-search-box {
  position: relative;
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
}

.filter-search-icon {
  position: absolute;
  left: 11px;
  width: 18px;
  height: 18px;
  color: #909399;
  pointer-events: none;
}

.filter-search-box input {
  width: 100%;
  height: 38px;
  padding: 0 42px 0 38px;
  box-sizing: border-box;
  border: 1px solid #dcdfe6;
  border-radius: 7px;
  outline: none;
  background: #ffffff;
  color: #303133;
  font: inherit;
  font-size: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.filter-search-box input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
}

.filter-search-box input::-webkit-search-cancel-button {
  display: none;
}

.clear-filter-search {
  position: absolute;
  right: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #909399;
  cursor: pointer;
}

.clear-filter-search:hover {
  background: #f2f6fc;
  color: #409eff;
}

.clear-filter-search svg {
  width: 17px;
  height: 17px;
}

.submit-title-search {
  flex: 0 0 auto;
  height: 38px;
  padding: 0 18px;
  border: 1px solid #409eff;
  border-radius: 7px;
  background: #409eff;
  color: #ffffff;
  font: inherit;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.submit-title-search:hover {
  border-color: #66b1ff;
  background: #66b1ff;
}

ul {
  width: 100%;
  max-width: 1380px;
  padding: 0;
  margin: 0 auto;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(var(--column), var(--width));
  grid-auto-rows: var(--height);
  grid-row-gap: var(--row-gap);
  justify-content: space-between;
}

.side-btn {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 4px;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  transition: all 0.3s;
  z-index: 8;
  color: #409eff;
}

.side-btn:hover {
  background-color: #f2f6fc;
  color: #409eff;
}

.side-btn svg, .side-btn .icon {
  width: 24px;
  height: 24px;
  fill: currentColor;
}

.left-btn-group {
  position: fixed;
  top: 84px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.right-btn {
  position: fixed;
  bottom: 50px;
}

.side-btn.active {
  background: #ecf5ff;
  color: #1976d2;
}

.side-btn-status {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  box-sizing: border-box;
  border: 2px solid #ffffff;
  border-radius: 50%;
  background: #f56c6c;
}

.empty-game {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  min-height: 260px;
  color: #909399;
  text-align: center;
}

.empty-game button {
  padding: 7px 14px;
  border: 1px solid #409eff;
  border-radius: 6px;
  background: #ffffff;
  color: #409eff;
  font: inherit;
  cursor: pointer;
}

.empty-game button:hover {
  background: #ecf5ff;
}

ul li {
  box-sizing: border-box;
  width: 100%;
  height: var(--height);
  overflow: visible;
  border-radius: 10px;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  z-index: 1;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

ul li:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.25);
  height: auto;
  min-height: var(--height);
  z-index: 10;
}

ul li .game-img-container {
  width: 100%;
  height: calc(var(--height) - var(--title-height));
  overflow: hidden;
  border-radius: 10px 10px 0 0;
  background-color: #f1fcff;
}

ul li .game-img {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: top;
  display: block;
}

ul li .game-info {
  padding: 8px 12px;
  background-color: #ffffff;
  border-radius: 0 0 10px 10px;
  overflow: hidden;
  transition: all 0.3s ease;
  height: var(--title-height);
  box-sizing: border-box;
}

ul li:hover .game-info {
  background-color: #f8f9fa;
  overflow: visible;
  height: auto;
  min-height: var(--title-height);
  line-clamp: unset;
  -webkit-line-clamp: unset;
}

.game-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  line-height: 20px;
  display: -webkit-box;
  line-clamp: 1;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.3s ease;
  word-break: break-word;
}

ul li:hover .game-title {
  line-clamp: unset;
  -webkit-line-clamp: unset;
  overflow: visible;
}

/* 弹出框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
  padding: 16px;
  box-sizing: border-box;
}

.modal-content {
  position: relative;
  display: flex;
  flex-direction: column;
  background-color: white;
  border-radius: 12px;
  width: 100%;
  max-width: 1200px;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: modalSlideIn 0.3s ease-out;
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-50px) scale(0.9);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 14px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  background-color: rgba(255, 255, 255, 0.9);
  border: 0;
  border-radius: 50%;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.12);
  color: #6c757d;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background-color: #e9ecef;
  color: #495057;
}

.modal-body {
  min-height: 0;
  padding: 32px;
  overflow-y: auto;
}

.game-detail-container {
  display: grid;
  grid-template-columns: minmax(0, 280px) minmax(0, 1fr);
  align-items: start;
  gap: 30px;
}

.game-thumbnail {
  position: relative;
  width: 280px;
  height: 420px;
  overflow: hidden;
  isolation: isolate;
  border-radius: 10px;
  background-color: #edf1f5;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.14);
}

.game-thumbnail-backdrop,
.game-thumbnail-image {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
}

.game-thumbnail-backdrop {
  object-fit: cover;
  filter: blur(16px);
  opacity: 0.3;
  transform: scale(1.08);
}

.game-thumbnail-image {
  z-index: 1;
  object-fit: contain;
  object-position: center;
}

.game-basic-info {
  min-width: 0;
  padding-top: 4px;
}

.game-heading {
  padding: 0 48px 18px 0;
}

.game-heading h2 {
  margin: 0;
  color: #2c3e50;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.info-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.label {
  font-weight: 600;
  color: #495057;
  white-space: nowrap;
}

.value {
  color: #495057;
  overflow-wrap: anywhere;
}

.game-description {
  max-height: 260px;
  padding-right: 8px;
  overflow-y: auto;
  color: #495057;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.game-previews {
  width: 100%;
  margin-top: 28px;
}

.game-previews h3 {
  margin: 0 0 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #409eff;
  color: #2c3e50;
  font-size: 18px;
  line-height: 1.4;
}

.preview-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.preview-item {
  cursor: pointer;
  transition: transform 0.3s ease;
}

.preview-item:hover {
  transform: translateY(-5px);
}

.preview-item img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

/* 图片预览弹出框 */
.image-modal-overlay {
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100dvh;
  --image-modal-gutter: clamp(12px, 3vw, 24px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1100;
  background-color: rgba(10, 14, 20, 0);
  backdrop-filter: blur(5px);
  padding: var(--image-modal-gutter);
  box-sizing: border-box;
  overflow: hidden;
}

.image-modal-content {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  height: fit-content;
  max-width: calc(100vw - var(--image-modal-gutter) - var(--image-modal-gutter));
  max-height: calc(100dvh - var(--image-modal-gutter) - var(--image-modal-gutter));
  box-sizing: border-box;
  overflow: hidden;
  border: 1px solid rgba(51, 62, 72, 0.24);
  border-radius: 10px;
  background-color: #ffffff;
  box-shadow: 0 16px 48px rgba(31, 42, 52, 0.28);
}

.image-modal-content img {
  display: block;
  width: auto;
  height: auto;
  max-width: calc(100vw - var(--image-modal-gutter) - var(--image-modal-gutter));
  max-height: calc(100dvh - var(--image-modal-gutter) - var(--image-modal-gutter));
  object-fit: contain;
  border-radius: 8px;
}

.image-close-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1102;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background-color: rgba(20, 26, 34, 0.76);
  color: #ffffff;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s ease;
}

.image-close-btn:hover {
  background-color: rgba(20, 26, 34, 0.92);
}

/* 图片导航按钮 */
.image-nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 1102;
  width: 50px;
  height: 50px;
  border: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(20, 26, 34, 0.76);
  color: #ffffff;
  cursor: pointer;
  transition: all 0.3s ease;
}

.image-nav-btn:hover {
  background: rgba(20, 26, 34, 0.92);
  transform: translateY(-50%) scale(1.1);
}

.prev-btn {
  left: 20px;
}

.next-btn {
  right: 20px;
}

.image-nav-btn .icon {
  width: 24px;
  height: 24px;
  fill: currentColor;
}

/* 图片计数器 */
.image-counter {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1102;
  background: rgba(20, 26, 34, 0.78);
  color: #ffffff;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

@media (max-width: 767px) {
  .filter-panel {
    padding: 12px;
  }

  .title-search-body {
    gap: 8px;
  }

  .filter-search-box input {
    font-size: 13px;
  }

  .submit-title-search {
    padding: 0 14px;
    font-size: 13px;
  }

  .modal-overlay {
    padding: 10px;
  }

  .modal-body {
    padding: 28px 16px 20px;
  }

  .game-detail-container {
    grid-template-columns: minmax(0, 1fr);
    gap: 22px;
  }

  .game-thumbnail {
    width: min(280px, 100%);
    height: auto;
    aspect-ratio: 2 / 3;
    justify-self: center;
  }

  .game-basic-info {
    padding-top: 0;
  }

  .game-heading {
    padding-right: 42px;
  }

  .game-heading h2 {
    font-size: 22px;
  }

  .preview-images {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  }

  .preview-item img {
    height: 160px;
  }

  .image-modal-overlay {
    --image-modal-gutter: 10px;
  }

  .image-close-btn {
    top: 8px;
    right: 8px;
    width: 34px;
    height: 34px;
    font-size: 24px;
  }

  .image-nav-btn {
    width: 38px;
    height: 38px;
  }

  .image-nav-btn .icon {
    width: 18px;
    height: 18px;
  }

  .prev-btn {
    left: 8px;
  }

  .next-btn {
    right: 8px;
  }

  .image-counter {
    bottom: 8px;
    padding: 5px 10px;
    font-size: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .filter-container {
    transition: none;
  }
}
</style>
