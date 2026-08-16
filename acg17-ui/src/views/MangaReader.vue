<template>
  <section class="manga-reader unselectable" :class="{ 'fullscreen-mode': isFullscreen }">
    <!-- 顶部菜单栏 -->
    <div class="top-menu" :class="{ 'hidden': isFullscreen }">
      <div class="menu-left">
        <button class="back-btn" @click="goBack">
          <icon icon="#icon-left"></icon>
          返回
        </button>
        <span class="manga-title">{{ manga.chineseTitle || manga.title }}</span>
      </div>

      <div class="menu-center">
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
      </div>

      <div class="menu-right">
        <div class="settings-menu" ref="settingsMenuRef">
          <button class="menu-btn" @click="toggleSettings">
            <icon icon="#icon-setting"></icon>
          </button>
          <div v-show="showSettings" class="settings-panel" ref="settingsPanelRef">
            <div class="panel-header">
              <span>阅读设置</span>
              <button class="close-btn" @click="closeSettings">×</button>
            </div>
            <div class="panel-content">
              <div class="setting-item">
                <label>阅读模式:</label>
                <select v-model="readingMode">
                  <option value="single">单页模式</option>
                  <option value="double">双页模式</option>
                </select>
              </div>
              <div class="setting-item">
                <label>自动播放:</label>
                <input type="checkbox" v-model="autoPlay" />
              </div>
              <div class="setting-item" v-if="autoPlay">
                <label>播放间隔:</label>
                <input type="range" v-model="autoPlayInterval" min="1" max="10" step="1" />
                <span>{{ autoPlayInterval }}秒</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 漫画显示区域 -->
    <div class="manga-display" ref="mangaDisplay" :class="{ 'fullscreen': isFullscreen }">
      <div class="manga-container">
        <!-- 图片显示 -->
        <img v-if="currentPageImage && !error" :src="currentPageImage" :alt="`第${currentPage}页`" class="manga-image"
          @load="onImageLoad" @error="onImageError" />

        <!-- 加载状态 -->
        <div v-if="loading" class="loading">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>
      </div>

      <!-- 点击蒙版 -->
      <div class="click-overlay">
        <div class="click-zone left-zone" @click="previousPage"><icon icon="#icon-left"></icon></div>
        <div class="click-zone center-zone" @click="toggleFullscreen"></div>
        <div class="click-zone right-zone" @click="nextPage"><icon icon="#icon-right"></icon></div>
      </div>

      <!-- 错误状态 -->
      <div v-if="error" class="error">
        <p>{{ error }}</p>
        <button @click="retryLoad">重试</button>
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
    const mangaDisplay = ref(null)

    const manga = reactive({
      id: null,
      title: '',
      pages: [],
      currentChapter: 1,
    })

    const mangaPages = reactive([])

    const mangaChapters = reactive([])

    const currentPage = ref(1)
    const totalPages = ref(0)
    const loading = ref(false)
    const error = ref('')
    const showSettings = ref(false)
    const settingsMenuRef = ref(null)
    const settingsPanelRef = ref(null)
    const readingMode = ref('single')
    const autoPlay = ref(false)
    const autoPlayInterval = ref(6)
    const isFullscreen = ref(false)
    let autoPlayTimer = null

    // 计算当前页面图片URL
    const currentPageImage = computed(() => {
      if (!mangaPages.length || !currentPage.value) return ''
      const currentPageData = mangaPages.find(page => page.page === currentPage.value)
      return currentPageData ? currentPageData.path : ''
    })

    // 从后端加载漫画详情数据
    async function loadMangaData() {
      try {
        const mangaId = parseInt(route.params.id)
        const chapterNum = parseInt(route.params.chapterNum) || 1
        const pageNum = parseInt(route.params.pageNum)

        // 更新当前章节
        manga.currentChapter = chapterNum
        manga.id = mangaId

        // 如果没有pageNum参数，自动重定向到第1页
        if (!pageNum) {
          router.replace(`/acg/manga/${mangaId}/${chapterNum}/1`)
          return
        }

        // 检查是否从MangaDetail页面跳转过来，如果有缓存数据则使用
        const cachedMangaData = history.state?.mangaData
        if (cachedMangaData && cachedMangaData.pages && cachedMangaData.pages.length > 0) {
          Object.assign(manga, cachedMangaData)
          mangaPages.length = 0
          mangaPages.push(...cachedMangaData.pages)
          totalPages.value = cachedMangaData.pages.length
          currentPage.value = Math.min(Math.max(pageNum, 1), cachedMangaData.pages.length)
          return
        }

        // 从后端API获取数据
        const res = await server.get(`/manga/${mangaId}`)

        if (res.code === 200 && res.data) {
          // 如果res.data为空，则进入404页面
          if (!res.data) {
            router.push('/404')
            return
          }
          const mangaData = res.data
          Object.assign(manga, mangaData)

          // 设置漫画章节数据
          mangaChapters.length = 0
          if (mangaData.pages && mangaData.pages.length > 0) {
            mangaChapters.push(...mangaData.pages)

            // 找到对应的章节
            const targetChapter = mangaChapters.find(c => c.chapter === chapterNum)

            if (targetChapter && targetChapter.pagelist && targetChapter.pagelist.length > 0) {
              // 设置漫画页面数据
              mangaPages.length = 0
              mangaPages.push(...targetChapter.pagelist)
              totalPages.value = targetChapter.pagelist.length
              currentPage.value = Math.min(Math.max(pageNum, 1), totalPages.value)
              manga.currentChapter = chapterNum
            } else {
              console.error('章节不存在或没有页面数据')
              error.value = '章节不存在'
            }
          }
        } else {
          console.error('获取漫画详情失败:', res.message)
          error.value = '加载漫画数据失败'
        }
      } catch (err) {
        console.error('加载漫画详情时发生错误:', err)
        error.value = '加载漫画数据失败'
      }
    }

    function goBack() {
      router.push(`/acg/manga/${manga.id}`)
    }

    function previousPage() {
      if (currentPage.value > 1) {
        currentPage.value--
        updateRoute()
      }
    }

    function nextPage() {
      if (currentPage.value < totalPages.value) {
        currentPage.value++
        updateRoute()
      }
    }

    function updateRoute() {
      router.replace(`/acg/manga/${manga.id}/${manga.currentChapter}/${currentPage.value}`)
    }

    function onImageLoad() {
      loading.value = false
      error.value = ''
    }

    function onImageError() {
      loading.value = false
      error.value = '图片加载失败'
    }

    function retryLoad() {
      loading.value = true
      error.value = ''
    }

    function toggleSettings() {
      showSettings.value = !showSettings.value
    }

    function closeSettings() {
      showSettings.value = false
    }

    function handleClickOutside(event) {
      if (!showSettings.value) return
      const settingsPanel = settingsPanelRef.value
      const settingsMenu = settingsMenuRef.value
      const isClickInsidePanel = settingsPanel && settingsPanel.contains(event.target)
      const isClickInsideMenu = settingsMenu && settingsMenu.contains(event.target)

      if (!isClickInsidePanel && !isClickInsideMenu) {
        showSettings.value = false
      }
    }

    function toggleFullscreen() {
      isFullscreen.value = !isFullscreen.value
      // 切换全局顶栏显示状态
      store.commit('toggleAcg17Header')
    }

    function startAutoPlay() {
      if (autoPlayTimer) {
        clearInterval(autoPlayTimer)
      }
      autoPlayTimer = setInterval(() => {
        if (currentPage.value < totalPages.value) {
          nextPage()
        } else {
          stopAutoPlay()
        }
      }, autoPlayInterval.value * 1000)
    }

    function stopAutoPlay() {
      if (autoPlayTimer) {
        clearInterval(autoPlayTimer)
        autoPlayTimer = null
      }
    }

    // 键盘导航
    function handleKeydown(event) {
      switch (event.key) {
        case 'ArrowLeft':
        case 'a':
        case 'A':
          previousPage()
          break
        case 'ArrowRight':
        case 'd':
        case 'D':
        case ' ':
          nextPage()
          break
        case 'Escape':
          goBack()
          break
      }
    }

    // 监听自动播放设置变化
    watch(autoPlay, (newValue) => {
      if (newValue) {
        startAutoPlay()
      } else {
        stopAutoPlay()
      }
    })

    watch(autoPlayInterval, () => {
      if (autoPlay.value) {
        stopAutoPlay()
        startAutoPlay()
      }
    })

    // 监听路由变化，重新加载数据
    watch(() => route.params, (newParams, oldParams) => {
      if (newParams.id !== oldParams?.id ||
        newParams.chapterNum !== oldParams?.chapterNum ||
        newParams.pageNum !== oldParams?.pageNum) {
        loadMangaData()
      }
    }, { immediate: false })

    onMounted(() => {
      loadMangaData()
      document.addEventListener('keydown', handleKeydown)
      document.addEventListener('click', handleClickOutside)
    })

    onUnmounted(() => {
      document.removeEventListener('keydown', handleKeydown)
      document.removeEventListener('click', handleClickOutside)
      stopAutoPlay()
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
      readingMode,
      autoPlay,
      autoPlayInterval,
      currentPageImage,
      mangaDisplay,
      isFullscreen,
      goBack,
      previousPage,
      nextPage,
      onImageLoad,
      onImageError,
      retryLoad,
      toggleSettings,
      closeSettings,
      toggleFullscreen
    }
  }
}
</script>

<style scoped>
.manga-reader {
  width: 100%;
  height: 100vh;
  /* background-color: #1a1a1a; */
  color: #ffffff;
  overflow: hidden;
  position: relative;
}

.manga-reader.fullscreen-mode {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
}

/* 顶部菜单栏 */
.top-menu {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  z-index: 1000;
  transition: transform 0.3s ease-in-out;
}

.top-menu.hidden {
  transform: translateY(-64px);
}

.menu-left {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 15px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #606266;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.back-btn .icon {
  font-size: 16px;
}

.back-btn:hover {
  background-color: #ecf5ff;
  transform: scale(1.2);
}

.manga-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.menu-center {
  display: flex;
  align-items: center;
}

.page-info {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  background-color: rgba(0, 0, 0, 0.06);
  padding: 6px 12px;
  border-radius: 20px;
}

.menu-right {
  display: flex;
  flex: 1;
  justify-content: flex-end;
  align-items: center;
}

.menu-btn {
  background: none;
  border: none;
  color: #303133;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.menu-btn .icon {
  font-size: 24px;
}

.menu-btn:hover {
  background-color: #ecf5ff;
  transform: scale(1.2);
}

/* 漫画显示区域 */
.manga-display {
  width: 100%;
  height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  top: 64px;
  left: 0;
  right: 0;
  box-sizing: border-box;
}

.manga-display.fullscreen {
  height: 100vh;
  top: 0;
}

.manga-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.manga-image {
  width: auto;
  height: 100%;
  max-width: 100%;
  object-fit: contain;
  /* border-radius: 8px; */
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

/* 点击蒙版 */
.click-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  z-index: 100;
}

.click-zone {
  height: 100%;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.click-zone:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.left-zone {
  flex: 0 0 35%;
}

.center-zone {
  flex: 0 0 30%;
}

.right-zone {
  flex: 0 0 35%;
}

.left-zone .icon, .right-zone .icon  {
  width: 35%;
  height: 35%;
  position: relative;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  fill: #00000000;
  pointer-events: none;
  transition: transform 0.2s ease, fill 0.2s ease;
}
.left-zone:hover .icon, .right-zone:hover .icon  {
  transform: translate(-50%, -50%) scale(1.1);
  fill: #00000008;
}

/* 加载状态 */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  width: 100%;
  height: 100%;
  min-height: 300px;
  background-color: rgba(0, 0, 0, 0.8);
  border-radius: 8px;
  border: 2px dashed rgba(255, 255, 255, 0.3);
}

/* 错误状态 */
.error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 40px;
  z-index: 200;
  min-width: 300px;
  max-width: 500px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top: 4px solid #ffffff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}

.error button {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
}

.error button:hover {
  background-color: #337ab7;
}

/* 设置面板 */
.settings-menu {
  position: relative;
  display: flex;
  align-items: center;
}

.settings-panel {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  width: 320px;
  max-height: 70vh;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(15px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  z-index: 1100;
  overflow: hidden;
  animation: slideInDown 0.2s ease-out;
}

@keyframes slideInDown {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #409eff 0%, #409effdd 100%);
  color: white;
  font-weight: 600;
  font-size: 14px;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 18px;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background-color 0.3s ease;
}

.close-btn:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.panel-content {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.setting-item label {
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
}

.setting-item span {
  color: #409eff;
  font-size: 12px;
  white-space: nowrap;
}

.setting-item select,
.setting-item input[type="range"] {
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  color: #606266;
  padding: 6px 10px;
  border-radius: 6px;
}

.setting-item select {
  min-width: 120px;
}

.setting-item input[type="range"] {
  flex: 1;
}

.setting-item input[type="checkbox"] {
  width: 18px;
  height: 18px;
  accent-color: #409eff;
}

/* 响应式设计 */
@media screen and (max-width: 768px) {
  .top-menu {
    padding: 0 15px;
  }

  .manga-title {
    max-width: 200px;
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
    max-width: 150px;
    font-size: 14px;
  }

  .page-info {
    font-size: 14px;
    padding: 4px 8px;
  }
}
</style>
