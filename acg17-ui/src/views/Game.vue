<!--
 * @author shiyongqiang
 * @date 2025/9/1
 * @description 游戏页面，展示游戏列表和详情弹出框
-->
<template>
  <section>
    <div class="side-btn-group left-btn-group">
      <button
        ref="randomButton"
        type="button"
        class="side-btn"
        aria-label="随机游戏"
        :title="randomPending ? '正在随机选择游戏' : '随机游戏'"
        :disabled="randomPending"
        :aria-busy="randomPending"
        @click="randomGame"
      >
        <icon icon="#icon-random"></icon>
      </button>
      <button
        type="button"
        class="side-btn"
        :class="{ active: showTitleSearch || hasTitleFilter }"
        aria-label="标题搜索"
        title="标题搜索"
        @click="toggleTitleSearch"
      >
        <icon icon="#icon-search"></icon>
        <span v-if="hasTitleFilter" class="side-btn-status" aria-hidden="true"></span>
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

    <ul
      class="game-container unselectable"
      v-infinite-scroll="loadMoreGames"
      :infinite-scroll-disabled="gameData.loading || gameData.error || gameData.disabled"
    >
      <TransitionGroup name="game-list">
        <li v-for="game in gameData.list" :key="game.id">
          <button type="button" class="game-card" @click="showGameDetail(game, $event)">
            <div class="game-img-container">
              <img
                v-if="game.cover && !coverErrorIds.has(game.id)"
                class="game-img"
                :src="withMediaStyle(game.cover, 'small')"
                :alt="`${game.chineseTitle || game.title || '游戏'}封面`"
                loading="lazy"
                decoding="async"
                @error="handleCoverError(game.id)"
              >
              <div
                v-else
                class="game-cover-placeholder"
                role="img"
                :aria-label="`${game.chineseTitle || game.title || '游戏'}封面`"
              >
                暂无封面
              </div>
              <span v-if="game.favorite" class="game-favorite" aria-label="已收藏" title="已收藏">★</span>
            </div>
            <div class="game-info">
              <div class="game-title" :title="game.chineseTitle || game.title">
                {{ game.chineseTitle || game.title }}
              </div>
              <div class="game-title game-title-expanded" aria-hidden="true">
                {{ game.chineseTitle || game.title }}
              </div>
            </div>
          </button>
        </li>
      </TransitionGroup>
    </ul>
    <div v-if="gameData.error" class="game-load-error" role="alert">
      <span>加载失败，请重试</span>
      <button type="button" @click="retryLoadGames">重试</button>
    </div>
    <div
      v-if="!gameData.loading && !gameData.error && gameData.disabled && gameData.list.length === 0"
      class="empty-game"
    >
      <span>{{ emptyGameText }}</span>
      <button v-if="hasTitleFilter" type="button" @click="clearTitleSearch">清除标题搜索</button>
    </div>
    <acg17-loading-heart v-show="gameData.loading"></acg17-loading-heart>
  </section>

  <Transition name="game-modal" @after-leave="handleModalAfterLeave">
    <div v-if="showModal" class="modal-overlay" @click="closeModal">
      <div
        class="modal-content"
        role="dialog"
        aria-modal="true"
        aria-labelledby="game-detail-title"
        @click.stop
      >
        <button
          ref="detailCloseButton"
          type="button"
          class="close-btn"
          aria-label="关闭游戏详情"
          :disabled="deletePending"
          @click="closeModal"
        >
          &times;
        </button>

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
                <h2 id="game-detail-title">{{ selectedGame.chineseTitle || selectedGame.title }}</h2>
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

              <div class="game-action-buttons unselectable">
                <button
                  :class="selectedGame.favorite ? 'btn-icon favorite' : 'btn-icon'"
                  type="button"
                  :title="selectedGame.favorite ? '取消喜欢' : '添加喜欢'"
                  :aria-label="selectedGame.favorite ? '取消喜欢' : '添加喜欢'"
                  :disabled="favoritePending"
                  :aria-busy="favoritePending"
                  @click="toggleFavorite"
                >
                  <icon :icon="selectedGame.favorite ? '#icon-favorite-y' : '#icon-favorite-n'" class="icon-svg"></icon>
                </button>
                <button
                  :class="selectedGame.deleted ? 'btn-icon restore' : 'btn-icon'"
                  type="button"
                  :title="selectedGame.deleted ? '恢复游戏' : '删除游戏'"
                  :aria-label="selectedGame.deleted ? '恢复游戏' : '删除游戏'"
                  :disabled="deletePending"
                  :aria-busy="deletePending"
                  @click="toggleDeleteStatus"
                >
                  <icon :icon="selectedGame.deleted ? '#icon-restore' : '#icon-delete'" class="icon-svg"></icon>
                </button>
              </div>
            </div>
          </div>

          <div class="game-previews unselectable" v-if="selectedGame.previewImages && selectedGame.previewImages.length > 0">
            <h3>游戏预览</h3>
            <div class="preview-images">
              <button
                v-for="(image, index) in selectedGame.previewImages"
                :key="index"
                type="button"
                class="preview-item"
                :aria-label="`查看预览图 ${index + 1}`"
                @click="showImagePreview(index, $event)"
              >
                <img :src="withMediaStyle(image, 'small')" :alt="`预览图 ${index + 1}`" loading="lazy" decoding="async">
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Transition>

  <div
    v-if="showImageModal"
    class="image-modal-overlay"
    role="dialog"
    aria-modal="true"
    aria-label="游戏预览图"
    @click="closeImageModal"
  >
    <div class="image-modal-content" @click.stop>
      <img :src="selectedGame.previewImages[currentImageIndex]" :alt="`预览图 ${currentImageIndex + 1}`" />
      <button
        ref="imageCloseButton"
        type="button"
        class="image-close-btn"
        aria-label="关闭图片预览"
        @click="closeImageModal"
      >
        &times;
      </button>
      
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
import LoadingHeart from "../components/LoadingHeart"
import Acg17Footer from "../components/Acg17Footer"
import Icon from "../components/Icon"
import { useBackToTop } from '@/composables/useBackToTop'
import { useRecycleState } from '@/composables/useRecycleState'
import { withMediaStyle } from '@/util/media'

function normalizeTitle(value) {
  const queryValue = Array.isArray(value) ? value[0] : value
  return String(queryValue ?? '').trim()
}

export default {
  name: "Game",
  components: {
    'acg17-loading-heart': LoadingHeart,
    'acg17-footer': Acg17Footer,
    'icon': Icon,
  },
  setup() {
    const route = useRoute()
    const router = useRouter()

    // 使用全局回收站状态管理
    const { isRecycle } = useRecycleState('game')

    const gameData = reactive({
      currentPage: 0,
      list: [],
      loading: false,
      error: false,
      disabled: false,
      total: 0,
    })

    const showModal = ref(false)
    const selectedGame = ref({})
    const showImageModal = ref(false)
    const currentImageIndex = ref(0)
    const favoritePending = ref(false)
    const deletePending = ref(false)
    const randomPending = ref(false)
    const randomButton = ref(null)
    const detailCloseButton = ref(null)
    const imageCloseButton = ref(null)
    const coverErrorIds = reactive(new Set())

    const { showBackToTop, scrollToTop } = useBackToTop()
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
    let gameRequestVersion = 0
    let pendingFilterFocus = false
    let pendingGameRemovalId = null
    let modalClosing = false
    let detailTriggerElement = null
    let imageTriggerElement = null
    let bodyOverflowBeforeModal = ''

    // 分页加载游戏列表
    async function loadGames() {
      if (gameData.loading || gameData.error || gameData.disabled) return

      const requestVersion = gameRequestVersion
      const pageNum = gameData.currentPage + 1
      gameData.loading = true
      gameData.error = false

      try {
        const params = {
          pageNum,
          deleted: isRecycle.value
        }
        if (hasTitleFilter.value) params.title = activeTitle.value

        const response = await server.get('/game/list', {
          params
        })

        if (requestVersion === gameRequestVersion && response.code === 200) {
          const pageData = response.data || {}
          const records = Array.isArray(pageData.records) ? pageData.records : []
          const existingIds = new Set(gameData.list.map(game => game.id))
          gameData.list.push(...records.filter(game => !existingIds.has(game.id)))
          gameData.total = Number(pageData.total) || 0
          gameData.currentPage = Number(pageData.current) || pageNum
          gameData.disabled = records.length === 0 || gameData.list.length >= gameData.total
        }
      } catch (error) {
        if (requestVersion !== gameRequestVersion) return
        gameData.error = true
        console.error('获取游戏列表失败:', error)
      } finally {
        if (requestVersion === gameRequestVersion) gameData.loading = false
      }
    }

    function loadMoreGames() {
      loadGames()
    }

    function retryLoadGames() {
      if (gameData.loading) return
      gameData.error = false
      loadGames()
    }

    function resetGameList() {
      gameRequestVersion += 1
      gameData.currentPage = 0
      gameData.list = []
      gameData.loading = false
      gameData.error = false
      gameData.disabled = false
      gameData.total = 0
      coverErrorIds.clear()
      loadGames()
    }

    function handleCoverError(id) {
      coverErrorIds.add(id)
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

    async function toggleFavorite() {
      const game = selectedGame.value
      if (!game.id || favoritePending.value) return

      favoritePending.value = true
      try {
        const favorite = !game.favorite
        const response = await server.put(`/game/${game.id}/favorite?favorite=${favorite}`)
        if (response.code === 200) {
          game.favorite = favorite
          const listGame = gameData.list.find(item => item.id === game.id)
          if (listGame) listGame.favorite = favorite
          ElMessage.success(favorite ? '游戏已收藏' : '已取消收藏')
        } else {
          ElMessage.error('更新收藏状态失败')
        }
      } catch (error) {
        console.error('游戏收藏操作失败:', error)
        ElMessage.error('更新收藏状态失败')
      } finally {
        favoritePending.value = false
      }
    }

    async function toggleDeleteStatus() {
      const game = selectedGame.value
      if (!game.id || deletePending.value) return

      const wasDeleted = Boolean(game.deleted)
      deletePending.value = true
      try {
        const response = wasDeleted
          ? await server.put(`/game/${game.id}/restore`)
          : await server.delete(`/game/${game.id}`)

        if (response.code === 200) {
          ElMessage.success(wasDeleted ? '游戏已恢复' : '游戏已删除')
          pendingGameRemovalId = game.id
          modalClosing = true
          showModal.value = false
        } else {
          ElMessage.error(wasDeleted ? '恢复游戏失败' : '删除游戏失败')
        }
      } catch (error) {
        console.error('游戏删除/恢复操作失败:', error)
        ElMessage.error(wasDeleted ? '恢复游戏失败' : '删除游戏失败')
      } finally {
        if (pendingGameRemovalId === null) deletePending.value = false
      }
    }

    // 显示游戏详情
    function showGameDetail(game, trigger) {
      if (showModal.value || modalClosing) return
      const triggerElement = trigger?.currentTarget || trigger
      detailTriggerElement = triggerElement instanceof HTMLElement
        ? triggerElement
        : randomButton.value
      bodyOverflowBeforeModal = document.body.style.overflow
      selectedGame.value = game
      showModal.value = true
      document.body.style.overflow = 'hidden'
      nextTick(() => detailCloseButton.value?.focus())
    }

    // 关闭详情弹出框
    function closeModal() {
      if (!showModal.value || deletePending.value) return
      if (showImageModal.value) {
        closeImageModal()
        return
      }
      modalClosing = true
      showModal.value = false
    }

    function handleModalAfterLeave() {
      modalClosing = false
      const returnFocusTarget = detailTriggerElement
      let shouldBackfill = false

      if (pendingGameRemovalId !== null) {
        const listIndex = gameData.list.findIndex(item => item.id === pendingGameRemovalId)
        if (listIndex !== -1) {
          gameData.list.splice(listIndex, 1)
        }
        gameData.total = Math.max(0, gameData.total - 1)
        coverErrorIds.delete(pendingGameRemovalId)
        if (gameData.currentPage > 0) {
          gameData.currentPage = Math.max(0, gameData.currentPage - 1)
          gameData.disabled = gameData.list.length >= gameData.total
          shouldBackfill = !gameData.disabled
        } else {
          gameData.disabled = gameData.list.length >= gameData.total
        }
        pendingGameRemovalId = null
        deletePending.value = false
      }

      selectedGame.value = {}
      document.body.style.overflow = bodyOverflowBeforeModal
      bodyOverflowBeforeModal = ''
      detailTriggerElement = null

      nextTick(() => {
        const fallback = document.querySelector('.game-card') || randomButton.value
        const focusTarget = returnFocusTarget?.isConnected ? returnFocusTarget : fallback
        focusTarget?.focus()
      })

      if (shouldBackfill) loadGames()
    }

    // 显示图片预览
    function showImagePreview(index, event) {
      imageTriggerElement = event.currentTarget
      currentImageIndex.value = index
      showImageModal.value = true
      nextTick(() => imageCloseButton.value?.focus())
    }

    // 关闭图片预览
    function closeImageModal() {
      if (!showImageModal.value) return
      const returnFocusTarget = imageTriggerElement
      showImageModal.value = false
      currentImageIndex.value = 0
      imageTriggerElement = null
      nextTick(() => {
        if (showModal.value && returnFocusTarget?.isConnected) {
          returnFocusTarget.focus()
        }
      })
    }

    // 上一张图片
    function previousImage() {
      if (currentImageIndex.value > 0) {
        currentImageIndex.value--
      }
    }

    // 下一张图片
    function nextImage() {
      const previewImages = selectedGame.value.previewImages || []
      if (currentImageIndex.value < previewImages.length - 1) {
        currentImageIndex.value++
      }
    }

    function handlePageKeydown(event) {
      if (!showModal.value) return

      if (showImageModal.value) {
        switch (event.key) {
          case 'ArrowLeft':
            event.preventDefault()
            previousImage()
            break
          case 'ArrowRight':
            event.preventDefault()
            nextImage()
            break
          case 'Escape':
            event.preventDefault()
            closeImageModal()
            break
        }
        return
      }

      if (event.key === 'Escape') {
        event.preventDefault()
        closeModal()
      }
    }

    // 随机获取一个游戏
    async function randomGame() {
      if (randomPending.value) return

      randomPending.value = true
      try {
        const response = await server.get('/game/random')
        if (response.code === 200 && randomButton.value) {
          showGameDetail(response.data, randomButton.value)
        }
      } catch (error) {
        console.error('随机获取游戏失败:', error)
      } finally {
        randomPending.value = false
      }
    }

    // 监听回收站状态变化，重新获取数据
    watch(isRecycle, () => {
      resetGameList()
    })

    watch(activeTitle, title => {
      titleSearchDraft.value = title
      resetGameList()
    })

    // 组件挂载时获取数据
    onMounted(() => {
      document.addEventListener('keydown', handlePageKeydown)
      loadGames()
    })

    onUnmounted(() => {
      gameRequestVersion += 1
      document.removeEventListener('keydown', handlePageKeydown)
      if (showModal.value || modalClosing) {
        document.body.style.overflow = bodyOverflowBeforeModal
      }
    })

    return {
      gameData,
      showModal,
      selectedGame,
      showImageModal,
      currentImageIndex,
      favoritePending,
      deletePending,
      randomPending,
      randomButton,
      detailCloseButton,
      imageCloseButton,
      coverErrorIds,
      showGameDetail,
      closeModal,
      handleModalAfterLeave,
      showImagePreview,
      closeImageModal,
      previousImage,
      nextImage,
      loadMoreGames,
      retryLoadGames,
      handleCoverError,
      randomGame,
      scrollToTop,
      showBackToTop,
      showTitleSearch,
      titleSearchInput,
      titleSearchDraft,
      hasTitleFilter,
      emptyGameText,
      toggleTitleSearch,
      closeTitleSearch,
      handleFilterPanelTransitionEnd,
      commitTitleSearch,
      clearTitleSearch,
      toggleFavorite,
      toggleDeleteStatus,
      withMediaStyle
    }
  }
}
</script>

<style scoped>
/* 列表布局变量 */
section {
  --column: 6;
  --card-max-width: 220px;
  --gap: 12px;
  --container-padding: 20px;
  --title-height: 40px;
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

.game-container {
  width: 100%;
  max-width: 1380px;
  padding: 0;
  margin: 0 auto;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(var(--column), minmax(0, var(--card-max-width)));
  grid-auto-rows: auto;
  gap: var(--gap);
  justify-content: center;
}

.side-btn.active {
  background: #ecf5ff;
  color: #1976d2;
}

.empty-game,
.game-load-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: #909399;
  text-align: center;
}

.empty-game {
  min-height: 260px;
}

.game-load-error {
  min-height: 160px;
}

.empty-game button,
.game-load-error button {
  padding: 7px 14px;
  border: 1px solid #409eff;
  border-radius: 6px;
  background: #ffffff;
  color: #409eff;
  font: inherit;
  cursor: pointer;
}

.empty-game button:hover,
.game-load-error button:hover {
  background: #ecf5ff;
}

.game-container > li {
  min-width: 0;
  width: 100%;
  position: relative;
  z-index: 1;
}

.game-container > li:hover,
.game-container > li:focus-within {
  z-index: 10;
}

.game-card {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: auto;
  padding: 0;
  overflow: visible;
  border: 0;
  border-radius: 10px;
  background: #fff;
  color: inherit;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.game-card:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.25);
}

.game-card:focus-visible {
  outline: 3px solid rgba(64, 158, 255, 0.7);
  outline-offset: 3px;
}

.game-img-container {
  position: relative;
  width: 100%;
  height: auto;
  aspect-ratio: 2 / 3;
  overflow: hidden;
  border-radius: 10px 10px 0 0;
  background-color: #f1fcff;
}

.game-img,
.game-cover-placeholder {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
}

.game-img {
  object-fit: contain;
  object-position: top;
  display: block;
}

.game-cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  background: #f1f3f5;
  color: #909399;
  font-size: 13px;
}

.game-favorite {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: #ff5c64;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.16);
  font-size: 18px;
  line-height: 1;
}

.game-info {
  position: relative;
  box-sizing: border-box;
  flex: 0 0 var(--title-height);
  display: flex;
  align-items: center;
  height: var(--title-height);
  padding: 5px 12px;
  overflow: hidden;
  background-color: #ffffff;
  border-radius: 0 0 10px 10px;
  transition: background-color 0.3s ease;
}

.game-card:hover .game-info {
  background-color: #f8f9fa;
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

.game-title.game-title-expanded {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  z-index: 1;
  display: none;
  box-sizing: border-box;
  min-height: 100%;
  align-items: center;
  padding: inherit;
  border-radius: inherit;
  background-color: #f8f9fa;
  line-clamp: unset;
  -webkit-line-clamp: unset;
  overflow: visible;
  pointer-events: none;
}

.game-card:focus-visible .game-info {
  overflow: visible;
  background-color: #f8f9fa;
}

.game-card:focus-visible .game-title-expanded {
  display: flex;
}

@media (hover: hover) and (pointer: fine) {
  .game-card:hover .game-info {
    overflow: visible;
  }

  .game-card:hover .game-title-expanded {
    display: flex;
  }
}

/* 卡片以 180px 左右为换列下限，宽屏最多 6 列 */
@media (max-width: 1169px) {
  section {
    --column: 5;
  }
}

@media (max-width: 979px) {
  section {
    --column: 4;
  }
}

@media (max-width: 779px) {
  section {
    --column: 3;
  }
}

@media (max-width: 579px) {
  section {
    --column: 2;
  }
}

@media (max-width: 991px) and (min-width: 768px) {
  section {
    --gap: 10px;
    --container-padding: 16px;
    margin: 74px auto 20px;
  }

  .filter-container {
    margin: 12px auto;
  }

  .game-title {
    font-size: 13px;
    line-height: 18px;
  }
}

.game-list-leave-active {
  pointer-events: none;
  transition: opacity 0.24s ease, transform 0.24s ease;
}

.game-list-leave-to {
  opacity: 0;
  transform: scale(0.94);
}

.game-list-move {
  transition: transform 0.3s ease;
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
}

.game-modal-enter-active,
.game-modal-leave-active {
  transition: opacity 0.22s ease;
}

.game-modal-enter-active .modal-content,
.game-modal-leave-active .modal-content {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.game-modal-enter-from,
.game-modal-leave-to,
.game-modal-enter-from .modal-content,
.game-modal-leave-to .modal-content {
  opacity: 0;
}

.game-modal-enter-from .modal-content {
  transform: translateY(18px) scale(0.98);
}

.game-modal-leave-to .modal-content {
  transform: translateY(12px) scale(0.98);
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

.close-btn:disabled {
  cursor: wait;
  opacity: 0.65;
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
  box-sizing: border-box;
  min-width: 0;
  min-height: 420px;
  padding-bottom: 66px;
  padding-top: 4px;
  position: relative;
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

.game-action-buttons {
  position: absolute;
  left: 0;
  bottom: 0;
  display: flex;
  gap: 15px;
}

.btn-icon {
  background-color: transparent;
  color: #409eff;
  border: 2px solid #409eff;
  padding: 8px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
}

.btn-icon:hover {
  background-color: #409eff;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.btn-icon.favorite {
  border-color: #ff5c64;
  color: #ff5c64;
}

.btn-icon.restore {
  border-color: #57d055;
  color: #57d055;
}

.btn-icon.favorite:hover {
  background-color: #ff5c64;
}

.btn-icon.restore:hover {
  background-color: #57d055;
}

.icon-svg {
  width: 28px;
  height: 28px;
  transition: all 0.3s ease;
  fill: currentColor;
}

.btn-icon:hover .icon-svg {
  fill: white;
}

.btn-icon:disabled {
  pointer-events: none;
  cursor: wait;
  opacity: 0.6;
  transform: none;
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
  min-width: 0;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.preview-item:hover {
  transform: translateY(-5px);
}

.preview-item:focus-visible {
  outline: 3px solid rgba(64, 158, 255, 0.7);
  outline-offset: 3px;
}

.preview-item img {
  display: block;
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
  section {
    --gap: 10px;
    --container-padding: 12px;
    --title-height: 40px;
    margin: 64px auto 20px;
    padding-bottom: calc(76px + env(safe-area-inset-bottom, 0px));
  }

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
    min-height: 0;
    padding-bottom: 66px;
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

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .game-card:hover {
    transform: none;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .game-card:hover .game-info {
    background-color: #ffffff;
  }

  .game-card:active {
    transform: scale(0.98);
    transition: transform 0.1s ease;
  }
}

@media (prefers-reduced-motion: reduce) {
  .filter-container {
    transition: none;
  }

  .game-modal-enter-active,
  .game-modal-leave-active,
  .game-modal-enter-active .modal-content,
  .game-modal-leave-active .modal-content,
  .game-list-leave-active,
  .game-list-move {
    transition: none;
  }
}
</style>
