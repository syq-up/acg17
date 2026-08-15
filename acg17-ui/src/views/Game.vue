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
              placeholder="搜索游戏原文标题或中文标题"
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
          <img class="game-img" :src="game.cover" :alt="game.title">
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

  <!-- 游戏详情弹出框 -->
  <div v-if="showModal" class="modal-overlay" @click="closeModal">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h2>{{ selectedGame.chineseTitle || selectedGame.title }}</h2>
        <button class="close-btn" @click="closeModal">&times;</button>
      </div>

      <div class="modal-body">
        <div class="game-detail-container">
          <!-- 游戏缩略图 -->
          <div class="game-thumbnail unselectable">
            <img :src="selectedGame.cover" :alt="selectedGame.title" />
          </div>

          <!-- 游戏基本信息 -->
          <div class="game-basic-info">
            <div class="info-row">
              <span class="label">游戏名称:</span>
              <span class="value">{{ selectedGame.title }}</span>
            </div>

            <div class="info-row">
              <span class="label">中文名称:</span>
              <span class="value">{{ selectedGame.chineseTitle }}</span>
            </div>

            <div class="info-row">
              <span class="label">版本号:</span>
              <span class="value">{{ selectedGame.version || '-' }}</span>
            </div>

            <!-- <div class="info-row">
              <span class="label">上传时间:</span>
              <span class="value">{{ selectedGame.createTime }}</span>
            </div> -->

            <div class="info-row">
              <span class="label">游戏简介:</span>
              <div class="game-description" style="white-space: pre-wrap;">{{ selectedGame.description }}</div>
            </div>
          </div>
        </div>

        <!-- 游戏预览图 -->
        <div class="game-previews unselectable" v-if="selectedGame.previewImages && selectedGame.previewImages.length > 0">
          <h3>游戏预览</h3>
          <div class="preview-images">
            <div v-for="(image, index) in selectedGame.previewImages" :key="index" class="preview-item">
              <img :src="image" :alt="`预览图 ${index + 1}`" @click="showImagePreview(index)" />
            </div>
          </div>
        </div>
      </div>

      <div class="modal-footer unselectable">
        <button class="btn-secondary" @click="closeModal">关闭</button>
      </div>
    </div>
  </div>

  <!-- 图片预览弹出框 -->
  <div v-if="showImageModal" class="image-modal-overlay" @click="closeImageModal">
    <div class="image-modal-content" @click.stop>
      <img :src="selectedGame.previewImages[currentImageIndex]" alt="预览图" />
      <button class="image-close-btn" @click="closeImageModal">&times;</button>
      
      <!-- 左右切换按钮 -->
      <button v-if="currentImageIndex > 0" class="image-nav-btn prev-btn" @click="previousImage">
        <icon icon="#icon-left"></icon>
      </button>
      <button v-if="currentImageIndex < selectedGame.previewImages.length - 1" class="image-nav-btn next-btn" @click="nextImage">
        <icon icon="#icon-right"></icon>
      </button>
      
      <!-- 图片计数器 -->
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
    const previewImage = ref('')
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
      previewImage.value = selectedGame.value.previewImages[index]
      showImageModal.value = true
      // 添加键盘事件监听
      document.addEventListener('keydown', handleImageKeydown)
    }

    // 关闭图片预览
    function closeImageModal() {
      showImageModal.value = false
      previewImage.value = ''
      currentImageIndex.value = 0
      // 移除键盘事件监听
      document.removeEventListener('keydown', handleImageKeydown)
    }

    // 上一张图片
    function previousImage() {
      if (currentImageIndex.value > 0) {
        currentImageIndex.value--
        previewImage.value = selectedGame.value.previewImages[currentImageIndex.value]
      }
    }

    // 下一张图片
    function nextImage() {
      if (currentImageIndex.value < selectedGame.value.previewImages.length - 1) {
        currentImageIndex.value++
        previewImage.value = selectedGame.value.previewImages[currentImageIndex.value]
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
      if (resizeObserver) resizeObserver.disconnect()
    })

    return {
      gameData,
      showModal,
      selectedGame,
      showImageModal,
      previewImage,
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
      clearTitleSearch
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
  padding: 20px;
  box-sizing: border-box;
}

.modal-content {
  background-color: white;
  border-radius: 12px;
  max-width: 1160px;
  max-height: 90vh;
  width: 100%;
  overflow-y: auto;
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

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 30px;
  border-bottom: 1px solid #e9ecef;
  background-color: #f8f9fa;
  border-radius: 12px 12px 0 0;
}

.modal-header h2 {
  margin: 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
  flex: 1;
  margin-right: 20px;
  word-break: break-word;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #6c757d;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.close-btn:hover {
  background-color: #e9ecef;
  color: #495057;
}

.modal-body {
  padding: 30px;
}

.game-detail-container {
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
}

.game-thumbnail {
  flex-shrink: 0;
}

.game-thumbnail img {
  width: 330px;
  height: auto;
  max-height: 330px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.game-basic-info {
  flex: 1;
  min-width: 0;
}

.info-row {
  display: flex;
  margin-bottom: 15px;
  align-items: flex-start;
}

.label {
  font-weight: 600;
  color: #495057;
  margin-right: 12px;
  min-width: 80px;
  flex-shrink: 0;
}

.value {
  color: #495057;
  flex: 1;
  word-break: break-word;
}

.game-description {
  max-height: 220px;
  overflow-y: auto;
  color: #495057;
  line-height: 1.6;
  flex: 1;
  word-break: break-word;
}

.game-previews {
  margin-top: 30px;
}

.game-previews h3 {
  font-size: 18px;
  color: #2c3e50;
  margin-bottom: 20px;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.preview-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
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

.modal-footer {
  padding: 20px 30px;
  border-top: 1px solid #e9ecef;
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  background-color: #f8f9fa;
  border-radius: 0 0 12px 12px;
}

.btn-primary {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.btn-primary:hover {
  background-color: #337ab7;
}

.btn-secondary {
  background-color: transparent;
  color: #6c757d;
  border: 1px solid #dee2e6;
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-secondary:hover {
  background-color: #e9ecef;
  border-color: #adb5bd;
}

/* 图片预览弹出框 */
.image-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.9);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1100;
  padding: 20px;
  box-sizing: border-box;
}

.image-modal-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  border: 1px solid #ffffff4d;
  border-radius: 8px;
}

.image-modal-content img {
  max-width: 100%;
  max-height: 100%;
  height: 90vh;
  object-fit: contain;
  border-radius: 8px;
  display: block;
}

.image-close-btn {
  position: absolute;
  top: -40px;
  right: 0;
  background: none;
  border: none;
  color: white;
  font-size: 32px;
  cursor: pointer;
  padding: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.image-close-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

/* 图片导航按钮 */
.image-nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.5);
  border: none;
  color: white;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  z-index: 1101;
}

.image-nav-btn:hover {
  background: rgba(0, 0, 0, 0.8);
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
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  z-index: 1101;
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
}

@media (prefers-reduced-motion: reduce) {
  .filter-container {
    transition: none;
  }
}
</style>
