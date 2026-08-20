<template>
  <section class="manga-reader unselectable" :class="{ 'immersive-mode': isImmersive }">
    <div class="top-menu" :class="{ hidden: isImmersive }">
      <div class="menu-left">
        <button type="button" class="back-btn" aria-label="返回漫画详情" @click="goBack">
          <icon icon="#icon-left"></icon>
        </button>
        <span class="manga-title">{{ manga.chineseTitle || manga.title }}</span>
      </div>

      <div class="menu-center">
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      </div>

      <div class="menu-right">
        <div ref="settingsMenuRef" class="settings-menu">
          <button type="button" class="menu-btn" aria-label="阅读设置" @click="toggleSettings">
            <icon icon="#icon-setting"></icon>
          </button>
          <div v-show="showSettings" ref="settingsPanelRef" class="settings-panel">
            <div class="panel-header">
              <span>阅读设置</span>
              <button type="button" class="close-btn" aria-label="关闭阅读设置" @click="closeSettings">×</button>
            </div>
            <div class="panel-content">
              <div class="setting-item">
                <label for="auto-play">自动播放:</label>
                <input id="auto-play" v-model="autoPlay" type="checkbox" />
              </div>
              <div v-if="autoPlay" class="setting-item">
                <label for="auto-play-interval">播放间隔:</label>
                <input id="auto-play-interval" v-model="autoPlayInterval" type="range" min="1" max="10" step="1" />
                <span>{{ autoPlayInterval }}秒</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="manga-display" :class="{ immersive: isImmersive }">
      <div class="manga-container">
        <img
          v-if="currentPageImage && !error"
          :key="imageKey"
          :src="currentPageImage"
          :data-image-key="imageKey"
          :alt="`第${currentPage}页`"
          class="manga-image"
          @load="onImageLoad"
          @error="onImageError"
        />

        <div v-if="loading" class="loading" role="status" aria-live="polite">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>
      </div>

      <div class="click-overlay" aria-label="翻页区域">
        <div class="click-zone left-zone" role="button" aria-label="上一页" @click="previousPage">
          <icon icon="#icon-left"></icon>
        </div>
        <div class="click-zone center-zone" role="button" aria-label="切换沉浸模式" @click="toggleImmersive"></div>
        <div class="click-zone right-zone" role="button" aria-label="下一页" @click="nextPage">
          <icon icon="#icon-right"></icon>
        </div>
      </div>

      <div v-if="error" class="error" role="alert">
        <p>{{ error }}</p>
        <button type="button" @click="retryLoad">重试</button>
      </div>

      <div v-if="showEndPanel" class="end-panel" role="dialog" aria-modal="true" aria-labelledby="reader-end-title">
        <h2 id="reader-end-title">{{ nextChapter ? '本章阅读完毕' : '整本漫画已读完' }}</h2>
        <p>{{ nextChapter ? '可以继续阅读下一章。' : '感谢阅读。' }}</p>
        <div class="end-actions">
          <button v-if="nextChapter" type="button" class="next-chapter-btn" @click="goToNextChapter">
            阅读下一章
          </button>
          <button type="button" class="return-detail-btn" @click="goBack">返回详情</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import server from '@/util/request'

export default {
  name: 'MangaReader',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()

    const settingsMenuRef = ref(null)
    const settingsPanelRef = ref(null)
    const manga = reactive({
      id: null,
      title: '',
      chineseTitle: '',
      currentChapter: null,
    })
    const mangaPages = reactive([])
    const mangaChapters = reactive([])
    const currentPage = ref(1)
    const totalPages = ref(0)
    const loading = ref(false)
    const error = ref('')
    const showSettings = ref(false)
    const autoPlay = ref(false)
    const autoPlayInterval = ref(6)
    const isImmersive = ref(false)
    const showEndPanel = ref(false)
    const imageKey = ref(0)

    let autoPlayTimer = null
    let dataRequestVersion = 0
    let preloadImages = []
    let headerVisibilityCaptured = false
    let headerVisibilityBeforeImmersive = true

    const currentPageImage = computed(() => {
      const page = mangaPages[currentPage.value - 1]
      return page?.path || ''
    })

    const nextChapter = computed(() => {
      const currentIndex = mangaChapters.findIndex(chapter => chapter.chapter === manga.currentChapter)
      if (currentIndex < 0) return null
      return mangaChapters.slice(currentIndex + 1).find(chapter => chapter.pagelist.length > 0) || null
    })

    function parsePositiveInteger(value) {
      const rawValue = Array.isArray(value) ? value[0] : value
      if (rawValue === undefined || rawValue === null || !/^[1-9]\d*$/.test(String(rawValue))) {
        return null
      }
      const parsed = Number(rawValue)
      return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : null
    }

    function getRouteMangaId() {
      return parsePositiveInteger(route.params.id)
    }

    function normalizePages(pagelist) {
      if (!Array.isArray(pagelist)) return []
      return pagelist.map((page, index) => ({
        ...page,
        page: index + 1,
      }))
    }

    function normalizeChapters(pages) {
      if (!Array.isArray(pages)) return []
      return pages.map((chapter, index) => ({
        ...chapter,
        chapter: parsePositiveInteger(chapter?.chapter) || index + 1,
        title: chapter?.title || '',
        pagelist: normalizePages(chapter?.pagelist),
      }))
    }

    function routePath(chapter, page) {
      return `/acg/manga/${manga.id}/${chapter}/${page}`
    }

    function replaceRoute(chapter, page) {
      const path = routePath(chapter, page)
      if (route.path !== path) {
        router.replace(path)
      }
    }

    function clearPreloads() {
      preloadImages.forEach(image => {
        image.onload = null
        image.onerror = null
        image.removeAttribute('src')
      })
      preloadImages = []
    }

    function getNextPagesForPreload() {
      const currentIndex = mangaChapters.findIndex(chapter => chapter.chapter === manga.currentChapter)
      if (currentIndex < 0) return []

      const pages = []
      const currentChapterPages = mangaChapters[currentIndex].pagelist
      for (let pageIndex = currentPage.value; pageIndex < currentChapterPages.length && pages.length < 3; pageIndex += 1) {
        const page = currentChapterPages[pageIndex]
        if (page?.path) pages.push(page)
      }

      for (let chapterIndex = currentIndex + 1; chapterIndex < mangaChapters.length && pages.length < 3; chapterIndex += 1) {
        const chapterPages = mangaChapters[chapterIndex].pagelist
        for (const page of chapterPages) {
          if (page?.path) pages.push(page)
          if (pages.length >= 3) break
        }
      }
      return pages
    }

    function preloadNextPages() {
      clearPreloads()
      getNextPagesForPreload().forEach(page => {
        const image = new Image()
        image.src = page.path
        preloadImages.push(image)
      })
    }

    function setCurrentChapter(chapter, pageNumber) {
      const page = Math.min(Math.max(pageNumber, 1), chapter.pagelist.length)
      const chapterChanged = manga.currentChapter !== chapter.chapter || mangaPages.length !== chapter.pagelist.length
      const pageChanged = currentPage.value !== page
      if (!chapterChanged && !pageChanged && currentPageImage.value) {
        return
      }

      manga.currentChapter = chapter.chapter
      mangaPages.splice(0, mangaPages.length, ...chapter.pagelist)
      totalPages.value = chapter.pagelist.length
      currentPage.value = page
      showEndPanel.value = false
      error.value = currentPageImage.value ? '' : '当前页没有可用图片'
      loading.value = Boolean(currentPageImage.value)
      imageKey.value += 1
      preloadNextPages()
    }

    function syncRoutePosition() {
      if (!mangaChapters.length) return
      const chapterNumber = parsePositiveInteger(route.params.chapterNum)
      if (!chapterNumber) {
        error.value = '章节参数无效'
        loading.value = false
        return
      }

      const chapter = mangaChapters.find(item => item.chapter === chapterNumber)
      if (!chapter || chapter.pagelist.length === 0) {
        error.value = '章节不存在或没有可阅读页面'
        loading.value = false
        clearPreloads()
        return
      }

      const requestedPage = parsePositiveInteger(route.params.pageNum) || 1
      const page = Math.min(requestedPage, chapter.pagelist.length)
      setCurrentChapter(chapter, page)
      replaceRoute(chapter.chapter, page)
    }

    async function loadMangaData() {
      const mangaId = getRouteMangaId()
      const requestVersion = ++dataRequestVersion
      clearPreloads()
      loading.value = true
      error.value = ''
      showEndPanel.value = false
      mangaPages.splice(0, mangaPages.length)
      mangaChapters.splice(0, mangaChapters.length)
      totalPages.value = 0

      if (!mangaId) {
        loading.value = false
        if (route.path !== '/404') router.push('/404')
        return
      }

      try {
        const res = await server.get(`/manga/${mangaId}`)
        if (requestVersion !== dataRequestVersion || getRouteMangaId() !== mangaId) return
        if (res?.code === 404 || (res?.code === 200 && !res.data)) {
          if (route.path !== '/404') router.push('/404')
          return
        }
        if (res?.code !== 200 || !res.data) {
          throw new Error(res?.message || '漫画详情请求失败')
        }

        const mangaData = res.data
        const chapters = normalizeChapters(mangaData.pages)
        Object.assign(manga, mangaData, { id: mangaId })
        mangaChapters.push(...chapters)
        syncRoutePosition()
        if (!mangaPages.length && !error.value) {
          throw new Error('漫画没有可阅读页面')
        }
      } catch (requestError) {
        if (requestVersion !== dataRequestVersion) return
        console.error('加载漫画详情时发生错误:', requestError)
        loading.value = false
        error.value = requestError?.message === '漫画没有可阅读页面'
          ? '漫画没有可阅读页面'
          : '加载漫画数据失败'
      }
    }

    function goBack() {
      const mangaId = manga.id || getRouteMangaId()
      router.push(mangaId ? `/acg/manga/${mangaId}` : '/acg/manga')
    }

    function previousPage() {
      if (currentPage.value <= 1) return
      showEndPanel.value = false
      currentPage.value -= 1
      loading.value = Boolean(currentPageImage.value)
      error.value = ''
      imageKey.value += 1
      preloadNextPages()
      replaceRoute(manga.currentChapter, currentPage.value)
    }

    function nextPage() {
      showEndPanel.value = false
      if (currentPage.value < totalPages.value) {
        currentPage.value += 1
        loading.value = Boolean(currentPageImage.value)
        error.value = ''
        imageKey.value += 1
        preloadNextPages()
        replaceRoute(manga.currentChapter, currentPage.value)
        return
      }

      showEndPanel.value = true
      stopAutoPlay()
      autoPlay.value = false
    }

    function goToNextChapter() {
      if (!nextChapter.value) return
      setCurrentChapter(nextChapter.value, 1)
      replaceRoute(nextChapter.value.chapter, 1)
    }

    function onImageLoad(event) {
      if (String(event?.target?.dataset?.imageKey) !== String(imageKey.value)) return
      loading.value = false
      error.value = ''
    }

    function onImageError(event) {
      if (String(event?.target?.dataset?.imageKey) !== String(imageKey.value)) return
      loading.value = false
      error.value = '图片加载失败'
    }

    async function retryLoad() {
      await loadMangaData()
    }

    function toggleSettings() {
      showSettings.value = !showSettings.value
    }

    function closeSettings() {
      showSettings.value = false
    }

    function handleClickOutside(event) {
      if (!showSettings.value) return
      const panel = settingsPanelRef.value
      const menu = settingsMenuRef.value
      if (!panel?.contains(event.target) && !menu?.contains(event.target)) {
        closeSettings()
      }
    }

    function toggleImmersive() {
      if (isImmersive.value) {
        isImmersive.value = false
        if (headerVisibilityCaptured) {
          store.commit('setAcg17HeaderVisible', headerVisibilityBeforeImmersive)
          headerVisibilityCaptured = false
        }
        return
      }

      headerVisibilityBeforeImmersive = Boolean(store.state.acg17Header?.show)
      headerVisibilityCaptured = true
      isImmersive.value = true
      store.commit('setAcg17HeaderVisible', false)
    }

    function startAutoPlay() {
      stopAutoPlay()
      autoPlayTimer = setInterval(() => {
        nextPage()
      }, Number(autoPlayInterval.value) * 1000)
    }

    function stopAutoPlay() {
      if (autoPlayTimer) {
        clearInterval(autoPlayTimer)
        autoPlayTimer = null
      }
    }

    function handleKeydown(event) {
      if (event.key === 'Escape') {
        if (showSettings.value) {
          closeSettings()
        } else if (isImmersive.value) {
          toggleImmersive()
        } else {
          goBack()
        }
        return
      }

      const target = event.target
      const isFormElement = target?.isContentEditable
        || ['INPUT', 'SELECT', 'TEXTAREA', 'BUTTON'].includes(target?.tagName)
      if (isFormElement) return

      if (event.key === ' ') {
        event.preventDefault()
        nextPage()
        return
      }
      if (event.key === 'ArrowLeft' || event.key === 'a' || event.key === 'A') {
        previousPage()
      } else if (event.key === 'ArrowRight' || event.key === 'd' || event.key === 'D') {
        nextPage()
      }
    }

    watch(autoPlay, value => {
      if (value) startAutoPlay()
      else stopAutoPlay()
    })

    watch(autoPlayInterval, () => {
      if (autoPlay.value) startAutoPlay()
    })

    watch(() => route.params.id, (newId, oldId) => {
      if (newId !== oldId) loadMangaData()
    }, { immediate: true })

    watch(() => [route.params.chapterNum, route.params.pageNum], () => {
      if (mangaChapters.length) syncRoutePosition()
    })

    onMounted(() => {
      document.addEventListener('keydown', handleKeydown)
      document.addEventListener('click', handleClickOutside)
    })

    onUnmounted(() => {
      dataRequestVersion += 1
      document.removeEventListener('keydown', handleKeydown)
      document.removeEventListener('click', handleClickOutside)
      stopAutoPlay()
      clearPreloads()
      if (headerVisibilityCaptured) {
        store.commit('setAcg17HeaderVisible', headerVisibilityBeforeImmersive)
        headerVisibilityCaptured = false
      }
    })

    return {
      manga,
      currentPage,
      totalPages,
      loading,
      error,
      showSettings,
      settingsMenuRef,
      settingsPanelRef,
      autoPlay,
      autoPlayInterval,
      currentPageImage,
      isImmersive,
      showEndPanel,
      nextChapter,
      imageKey,
      goBack,
      previousPage,
      nextPage,
      goToNextChapter,
      onImageLoad,
      onImageError,
      retryLoad,
      toggleSettings,
      closeSettings,
      toggleImmersive,
    }
  },
}
</script>

<style scoped>
.manga-reader {
  width: 100%;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  position: relative;
  color: #fff;
}
.manga-reader.immersive-mode {
  position: fixed;
  inset: 0;
  z-index: 9999;
}

.top-menu {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 20px;
  color: #303133;
  background: rgba(255, 255, 255, .95);
  backdrop-filter: blur(10px);
  transition: transform .3s ease-in-out;
}

.top-menu.hidden {
  transform: translateY(-64px);
}

.menu-left,
.menu-right {
  display: flex;
  flex: 1;
  align-items: center;
}

.menu-right {
  justify-content: flex-end;
}

.menu-center {
  display: flex;
  align-items: center;
}

.back-btn,
.menu-btn,
.close-btn {
  border: 0;
  border-radius: 6px;
  cursor: pointer;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  padding: 0;
  color: #606266;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  transition: color .2s ease, background-color .2s ease, border-color .2s ease, transform .15s ease;
}

.back-btn .icon {
  position: relative;
  top: 1px;
  width: 20px;
  height: 20px;
}

.back-btn:hover {
  color: #409eff;
  background: #ecf5ff;
  border-color: #d9ecff;
}

.back-btn:active {
  transform: scale(.94);
}

.back-btn:focus-visible {
  outline: 2px solid rgba(64, 158, 255, .45);
  outline-offset: 2px;
}

.menu-btn:hover {
  background: #ecf5ff;
}

.manga-title {
  max-width: 45vw;
  overflow: hidden;
  font-size: 18px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-info {
  padding: 6px 12px;
  border-radius: 20px;
  color: #303133;
  background: rgba(0, 0, 0, .06);
}

.menu-btn {
  padding: 8px;
  color: #303133;
  background: none;
}

.menu-btn .icon {
  font-size: 24px;
}

.settings-menu {
  position: relative;
  display: flex;
  align-items: center;
}

.settings-panel {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  z-index: 1100;
  box-sizing: border-box;
  width: 340px;
  max-height: 70vh;
  overflow: hidden;
  color: #606266;
  background: #ffffff;
  border: 1px solid #e8edf3;
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(31, 45, 61, .1);
  font-family: 'Blueaka', 'PingFang SC', sans-serif;
  animation: settingsPanelIn .22s cubic-bezier(.22, 1, .36, 1);
}

@keyframes settingsPanelIn {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 56px;
  box-sizing: border-box;
  padding: 12px 16px 12px 20px;
  color: #303133;
  background: #ffffff;
  border-bottom: 1px solid #edf0f4;
  font-size: 16px;
  font-weight: 600;
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: 8px;
  color: #909399;
  background: transparent;
  font-family: Arial, sans-serif;
  font-size: 22px;
  line-height: 1;
  transition: color .2s ease, background-color .2s ease;
}

.close-btn:hover {
  color: #606266;
  background: #f3f4f6;
}

.close-btn:focus-visible {
  outline: 2px solid rgba(64, 158, 255, .35);
  outline-offset: 1px;
}

.panel-content {
  display: flex;
  flex-direction: column;
  padding: 4px 20px 8px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 52px;
  border-bottom: 1px solid #f0f2f5;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-item label,
.setting-item span {
  font-size: 13px;
  white-space: nowrap;
}

.setting-item label {
  color: #5f6977;
  font-weight: 500;
}

.setting-item span {
  min-width: 46px;
  box-sizing: border-box;
  padding: 4px 8px;
  border-radius: 8px;
  color: #409eff;
  background: #eef6ff;
  text-align: center;
}

.setting-item input[type='range'] {
  flex: 1;
  min-width: 90px;
  height: 4px;
  margin: 0;
  appearance: none;
  border-radius: 999px;
  outline: none;
  background: #dfe5ec;
  cursor: pointer;
}

.setting-item input[type='range']::-webkit-slider-thumb {
  width: 18px;
  height: 18px;
  appearance: none;
  border: 2px solid #409eff;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 2px 6px rgba(64, 158, 255, .2);
}

.setting-item input[type='range']::-moz-range-track {
  height: 4px;
  border-radius: 999px;
  background: #dfe5ec;
}

.setting-item input[type='range']::-moz-range-progress {
  height: 4px;
  border-radius: 999px;
  background: #409eff;
}

.setting-item input[type='range']::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border: 2px solid #409eff;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 2px 6px rgba(64, 158, 255, .2);
}

.setting-item input[type='range']:focus-visible {
  box-shadow: 0 0 0 3px rgba(64, 158, 255, .14);
}

.setting-item input[type='checkbox'] {
  flex: 0 0 40px;
  width: 40px;
  height: 22px;
  margin: 0;
  appearance: none;
  border: none;
  border-radius: 999px;
  outline: none;
  background-color: #d8dee8;
  background-image: radial-gradient(circle, #ffffff 0 8px, transparent 8.5px);
  background-position: 0 0;
  background-repeat: no-repeat;
  background-size: 22px 22px;
  cursor: pointer;
  transition: background-color .2s ease, background-position .2s ease;
}

.setting-item input[type='checkbox']:checked {
  background-color: #409eff;
  background-position: 18px 0;
}

.setting-item input[type='checkbox']:focus-visible {
  box-shadow: 0 0 0 3px rgba(64, 158, 255, .18);
}

.manga-display {
  position: absolute;
  top: 64px;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: calc(100vh - 64px);
  height: calc(100dvh - 64px);
  box-sizing: border-box;
}

.manga-display.immersive {
  top: 0;
  height: 100vh;
  height: 100dvh;
}

.manga-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.manga-image {
  width: auto;
  height: 100%;
  max-width: 100%;
  object-fit: contain;
  box-shadow: 0 4px 20px rgba(0, 0, 0, .5);
}

.click-overlay {
  position: absolute;
  inset: 0;
  z-index: 100;
  display: flex;
}

.click-zone {
  height: 100%;
  cursor: pointer;
  transition: background-color .2s ease;
}

.click-zone:hover {
  background: rgba(255, 255, 255, .05);
}

.left-zone,
.right-zone {
  flex: 0 0 35%;
}

.center-zone {
  flex: 0 0 30%;
}

.left-zone .icon,
.right-zone .icon {
  position: relative;
  top: 50%;
  left: 50%;
  width: 35%;
  height: 35%;
  fill: #00000000;
  pointer-events: none;
  transform: translate(-50%, -50%);
  transition: transform .2s ease, fill .2s ease;
}

.left-zone:hover .icon,
.right-zone:hover .icon {
  fill: #00000008;
  transform: translate(-50%, -50%) scale(1.1);
}

.loading {
  position: absolute;
  inset: 0;
  z-index: 150;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  background: rgba(0, 0, 0, .8);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(255, 255, 255, .3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.error,
.end-panel {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 200;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  min-width: 300px;
  max-width: min(500px, calc(100vw - 32px));
  padding: 32px;
  color: #fff;
  text-align: center;
  background: rgba(0, 0, 0, .85);
  border-radius: 12px;
  transform: translate(-50%, -50%);
}

.error button,
.end-panel button {
  padding: 10px 20px;
  border: 0;
  border-radius: 6px;
  color: #fff;
  background: #409eff;
  cursor: pointer;
}

.end-panel .return-detail-btn {
  background: #606266;
}

.end-panel h2,
.end-panel p {
  margin: 0;
}

.end-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media screen and (max-width: 768px) {
  .top-menu {
    padding: 0 15px;
  }

  .manga-title {
    max-width: 35vw;
    font-size: 16px;
  }

  .settings-panel {
    right: 8px;
    width: calc(100vw - 32px);
    max-width: 420px;
  }
}

@media screen and (max-width: 480px) {
  .menu-left {
    gap: 10px;
  }

  .manga-title {
    max-width: 28vw;
    font-size: 14px;
  }

  .page-info {
    padding: 4px 8px;
    font-size: 14px;
  }
}
</style>
