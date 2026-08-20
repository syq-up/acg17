<template>
  <div class="novel-reader" :style="{
      '--reader-max-width': settings.selectedWidth === 'auto' ? '900px' : settings.selectedWidth + 'px'
    }">

    <!-- 顶部导航栏 -->
    <div class="reader-header">
      <div class="header-content">
        <div class="header-left">
          <button type="button" class="back-btn" aria-label="返回上一页" @click="$router.back()">
            <icon icon="#icon-left"></icon>
          </button>
          <div class="book-info">
            <h1 class="book-title">{{ chapter.novel.title }}</h1>
          </div>
        </div>

      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-container">

      <!-- 左侧菜单栏 -->
      <div class="left-side-menu-bar" ref="leftMenuBarRef">
        <!-- 目录菜单 -->
        <div class="menu-item-container">
          <div class="menu-item" @click="toggleCatalogMenu">
            <icon icon="#icon-catalog"></icon>
            <span class="menu-text">目录</span>
          </div>
          <!-- 目录面板 -->
          <div v-show="catalogMenu.show" class="side-menu-panel" ref="catalogPanelRef">
            <div class="panel-header">
              <div>
                <span style="margin-right: 12px;">章节目录</span>
                <span class="chapter-count">共 {{ chapter.chapterList.length }} 章</span>
              </div>
              <button class="close-btn" @click="catalogMenu.show = false">×</button>
            </div>
            <div class="panel-content">
              <div v-for="(item, i) in chapter.chapterList" :key="item.id" @click="toChapter(item.id, i)"
                :class="{ 'active-chapter': i === chapter.currentChapterIndex, 'is-disabled': chapter.loading }"
                :aria-disabled="chapter.loading" class="chapter-item">
                <!-- <span class="chapter-number">{{ i + 1 }}</span> -->
                <span class="chapter-item-title">{{ item.title }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 管理菜单 -->
        <div class="menu-item-container">
          <div class="menu-item" @click="toggleManagementMenu">
            <icon icon="#icon-book"></icon>
            <span class="menu-text">管理</span>
          </div>
          <!-- 管理面板 -->
          <div v-show="managementMenu.show" class="side-menu-panel management-panel" ref="managementPanelRef">
            <div class="panel-header">
              <span>管理与设置</span>
              <button class="close-btn" @click="managementMenu.show = false">×</button>
            </div>
            <div class="panel-content">
              <!-- 章节管理操作 -->
              <el-divider content-position="left">章节管理</el-divider>

              <div class="setting-item">
                <label class="setting-label">章节管理</label>
                <div class="chapter-management-options">
                  <button @click="openUpdatePanel" class="chapter-management-option"
                    :disabled="!chapter.content || chapter.loading">
                    <icon icon="#icon-edit"></icon>
                    修改章节
                  </button>
                  <button @click="toUploadChapter" class="chapter-management-option"
                    :disabled="!chapter.novel.id || chapter.loading">
                    <icon icon="#icon-add"></icon>
                    新增章节
                  </button>
                </div>
              </div>

              <!-- 阅读设置操作 -->
              <el-divider content-position="left">阅读设置</el-divider>

              <!-- 字体大小 -->
              <div class="setting-item">
                <label class="setting-label">字体大小</label>
                <div class="font-size-controls">
                  <button @click="decreaseFontSize" class="size-btn">A-</button>
                  <button class="font-size-display">{{ settings.fontSize }}</button>
                  <button @click="increaseFontSize" class="size-btn">A+</button>
                </div>
              </div>

              <!-- 页面宽度 -->
              <div class="setting-item">
                <label class="setting-label">页面宽度</label>
                <div class="width-options">
                  <button v-for="width in settings.widthOptions" :key="width.value"
                    @click="settings.selectedWidth = width.value"
                    :class="['width-option', { 'active': settings.selectedWidth === width.value }]">
                    {{ width.label }}
                  </button>
                </div>
              </div>

            </div>
          </div>
        </div>
      </div>

      <!-- 右侧工具栏 -->
      <div v-show="showBackToTop" class="right-side-tool-bar">
        <div class="tool-item" @click="scrollToTop" title="回到顶部">
          <icon icon="#icon-left" style="transform: rotate(90deg);"></icon>
          <span class="tool-text">顶部</span>
        </div>
      </div>

      <!-- 阅读主区域 -->
      <div class="reader-main">
        <div class="content-container">
          <div v-if="chapter.loading" class="reader-state reader-state-loading" role="status" aria-live="polite">
            <acg17-loading-heart></acg17-loading-heart>
            <p>正在加载章节...</p>
          </div>

          <div v-else-if="chapter.error && !chapter.content" class="reader-state reader-state-error" role="alert">
            <h2>内容加载失败</h2>
            <p>{{ chapter.error }}</p>
            <el-button type="primary" plain @click="retryLoad">重新加载</el-button>
          </div>

          <div v-else-if="chapter.chapterList.length === 0" class="reader-state reader-state-empty">
            <h2>暂无章节</h2>
            <p>新增第一章后即可开始阅读。</p>
            <div class="reader-state-actions">
              <el-button type="primary" @click="toUploadChapter">新增第一章</el-button>
              <el-button plain @click="loadNovel">刷新章节</el-button>
            </div>
          </div>

          <template v-else-if="chapter.content">
            <div v-if="chapter.error" class="chapter-error" role="alert">
              <span>{{ chapter.error }}</span>
              <el-button text type="primary" @click="retryLoad">重试</el-button>
            </div>

            <article class="chapter-article">
              <header class="chapter-header">
                <h2 class="chapter-title">{{ chapter.content.title }}</h2>
                <div class="chapter-meta">
                  <div class="meta-item">
                    <icon icon="#icon-words"></icon>
                    <span>{{ chapter.content.totalWords }} 字</span>
                  </div>
                  <div class="meta-item">
                    <icon icon="#icon-time"></icon>
                    <span>{{ chapter.content.updateTime }}</span>
                  </div>
                  <div class="meta-item meta-tags">
                    <icon icon="#icon-tag"></icon>
                    <div class="chapter-tags">
                      <el-tag v-for="(item, i) in chapter.novel.tags" :key="'meta-tag-' + i"
                        v-show="item !== ''" size="small" class="meta-tag-item">
                        {{ item }}
                      </el-tag>
                    </div>
                  </div>
                </div>
              </header>
              <div class="chapter-content" :style="{ fontSize: settings.fontSize + 'px' }">
                <p v-for="(paragraph, index) in chapter.content.content" :key="index" class="paragraph">
                  {{ paragraph }}
                </p>
              </div>
            </article>
          </template>
        </div>

        <!-- 章节导航 -->
        <div v-if="chapter.content && chapter.chapterList.length" class="chapter-navigation">
          <button type="button" :disabled="!canGoPrevious || chapter.loading" @click="changeChapter(-1)"
            class="chapter-nav-button prev-btn">
            <icon icon="#icon-left"></icon>
            <span>上一章</span>
          </button>

          <button type="button" :disabled="!canGoNext || chapter.loading"
            @click="changeChapter(1)" class="chapter-nav-button next-btn">
            <span>下一章</span>
            <icon icon="#icon-right"></icon>
          </button>
        </div>
      </div>
    </div>

  </div>

  <!-- 更新章节对话框 -->
  <el-dialog v-model="update.show" title="更新章节" width="800px" class="update-dialog">
    <div class="dialog-content">
      <div class="form-item">
        <label>章节标题</label>
        <el-input v-model="update.title" placeholder="请输入章节标题" size="large" />
      </div>
      <div class="form-item">
        <label>章节内容</label>
        <el-input v-model="update.content" :rows="12" type="textarea" placeholder="请输入章节内容" resize="vertical" />
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="update.show = false" size="large">取消</el-button>
        <el-button type="primary" @click="updateChapter" size="large">保存更新</el-button>
      </div>
    </template>
  </el-dialog>

</template>

<script>
import { computed, nextTick, reactive, onBeforeMount, onMounted, onUnmounted, ref } from 'vue';
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router';
import server from '@/util/request';
import { useBackToTop } from '@/composables/useBackToTop';
import LoadingHeart from "../components/LoadingHeart";

export default {
  name: "NovelReader",
  components: {
    'acg17-loading-heart': LoadingHeart,
  },
  setup() {
    const store = useStore()
    const route = useRoute()
    const router = useRouter()
    const { showBackToTop, scrollToTop } = useBackToTop()
    const headerVisibilityBeforeReader = Boolean(store.state.acg17Header?.show)

    store.commit('setAcg17HeaderVisible', false)

    // 面板引用
    const catalogPanelRef = ref(null)
    const managementPanelRef = ref(null)
    const leftMenuBarRef = ref(null)

    const chapter = reactive({
      novel: { id: '', title: '', author: '', tags: [], totalWords: '', updateTime: '' },
      chapterList: [],
      content: null,
      currentChapterIndex: -1,
      loading: false,
      error: '',
    })

    const canGoPrevious = computed(() => chapter.currentChapterIndex > 0)
    const canGoNext = computed(() => (
      chapter.currentChapterIndex >= 0
      && chapter.currentChapterIndex < chapter.chapterList.length - 1
    ))

    let contentRequestVersion = 0
    let failedChapterIndex = null

    // 加载小说和第一章内容
    async function loadNovel() {
      const requestVersion = ++contentRequestVersion
      chapter.loading = true
      chapter.error = ''
      chapter.content = null
      chapter.currentChapterIndex = -1
      chapter.chapterList = []
      failedChapterIndex = null

      try {
        const res = await server.get('/novel/getContentById/' + route.params.novelId)
        if (requestVersion !== contentRequestVersion) return

        if (!res?.data) {
          router.push('/404')
          return
        }
        if (!Array.isArray(res.chapterList)) {
          throw new Error('章节目录数据无效')
        }

        chapter.novel = res.data
        chapter.chapterList = res.chapterList

        if (chapter.chapterList.length > 0) {
          if (!res.firstChapter) {
            throw new Error('第一章内容缺失')
          }
          chapter.content = res.firstChapter
          chapter.currentChapterIndex = 0
        }
        chapter.loading = false
      } catch (error) {
        if (requestVersion !== contentRequestVersion) return
        console.error('加载小说内容失败:', error)
        chapter.loading = false
        chapter.error = '小说内容加载失败，请重试。'
      }
    }

    onBeforeMount(loadNovel)

    // 切换章节
    function changeChapter(change) {
      if (chapter.loading) return
      const targetIndex = chapter.currentChapterIndex + change
      if (targetIndex < 0 || targetIndex >= chapter.chapterList.length) return
      loadChapter(targetIndex)
    }

    // 跳转章节
    function toChapter(id, i) {
      if (chapter.loading || chapter.chapterList[i]?.id !== id) return
      if (i === chapter.currentChapterIndex) {
        catalogMenu.show = false
        return
      }
      loadChapter(i)
    }

    async function loadChapter(targetIndex) {
      const targetChapter = chapter.chapterList[targetIndex]
      if (chapter.loading || !targetChapter) return

      const requestVersion = ++contentRequestVersion
      chapter.loading = true
      chapter.error = ''
      failedChapterIndex = null

      try {
        const res = await server.get('/novel-chapter/getContentById/' + targetChapter.id)
        if (requestVersion !== contentRequestVersion) return
        if (!res?.data) throw new Error('章节内容不存在')

        chapter.content = res.data
        chapter.currentChapterIndex = targetIndex
        chapter.loading = false
        catalogMenu.show = false
        managementMenu.show = false

        await nextTick()
        window.scrollTo({ top: 0, behavior: 'auto' })
      } catch (error) {
        if (requestVersion !== contentRequestVersion) return
        console.error('加载章节内容失败:', error)
        chapter.loading = false
        chapter.error = `章节「${targetChapter.title}」加载失败，请重试。`
        failedChapterIndex = targetIndex
      }
    }

    function retryLoad() {
      if (failedChapterIndex !== null && chapter.content) {
        loadChapter(failedChapterIndex)
        return
      }
      loadNovel()
    }

    // 阅读设置
    const settings = reactive({
      fontSize: 18,
      selectedWidth: 900,
      widthOptions: [
        { label: '自动', value: 'auto' },
        { label: '640', value: 640 },
        { label: '800', value: 800 },
        { label: '900', value: 900 },
        { label: '1000', value: 1000 }
      ]
    })

    // 侧边菜单控制
    const catalogMenu = reactive({
      show: false
    })

    const managementMenu = reactive({
      show: false
    })

    function toggleCatalogMenu() {
      catalogMenu.show = !catalogMenu.show
      if (catalogMenu.show) {
        managementMenu.show = false
      }
    }

    function toggleManagementMenu() {
      managementMenu.show = !managementMenu.show
      if (managementMenu.show) {
        catalogMenu.show = false
      }
    }

    // 点击外部区域关闭面板
    function handleClickOutside(event) {
      const catalogPanel = catalogPanelRef.value
      const managementPanel = managementPanelRef.value
      const leftMenuBar = leftMenuBarRef.value

      // 检查点击是否在面板或菜单栏内部
      const isClickInsideCatalog = catalogPanel && catalogPanel.contains(event.target)
      const isClickInsideManagement = managementPanel && managementPanel.contains(event.target)
      const isClickInsideMenuBar = leftMenuBar && leftMenuBar.contains(event.target)

      // 如果点击在外部，关闭所有面板
      if (!isClickInsideCatalog && !isClickInsideManagement && !isClickInsideMenuBar) {
        catalogMenu.show = false
        managementMenu.show = false
      }
    }

    function isInteractiveTarget(target) {
      return target?.isContentEditable
        || ['A', 'INPUT', 'SELECT', 'TEXTAREA', 'BUTTON'].includes(target?.tagName)
        || Boolean(target?.closest?.('[role="dialog"]'))
    }

    function handleReaderKeydown(event) {
      if (event.defaultPrevented || event.repeat || update.show || isInteractiveTarget(event.target)) return

      if (event.key === 'Escape') {
        if (catalogMenu.show || managementMenu.show) {
          catalogMenu.show = false
          managementMenu.show = false
        } else {
          router.back()
        }
        return
      }

      if (chapter.loading || !chapter.content) return
      if (event.key === 'ArrowLeft') {
        event.preventDefault()
        changeChapter(-1)
      } else if (event.key === 'ArrowRight') {
        event.preventDefault()
        changeChapter(1)
      }
    }

    // 添加和移除事件监听器
    onMounted(() => {
      document.addEventListener('click', handleClickOutside)
      document.addEventListener('keydown', handleReaderKeydown)
    })

    onUnmounted(() => {
      contentRequestVersion += 1
      document.removeEventListener('click', handleClickOutside)
      document.removeEventListener('keydown', handleReaderKeydown)
      store.commit('setAcg17HeaderVisible', headerVisibilityBeforeReader)
    })

    // 更新章节
    const update = reactive({
      show: false,
      title: '',
      content: '',
    })

    function openUpdatePanel() {
      update.show = true
      update.title = chapter.content.title
      // 将字符串列表转换为换行分隔的文本
      update.content = chapter.content.content.join('\n\n')
    }

    function closeUpdatePanel() {
      update.show = false
      update.title = ''
      update.content = ''
    }
    // 更新章节
    function updateChapter() {
      const data = {
        id: chapter.content.id,
        title: update.title,
        content: update.content.split('\n\n').filter(p => p.trim() !== ''), // 将文本按双换行分割为字符串列表
      }
      server.post('/novel-chapter/updateChapter', data)
        .then(() => {
          ElMessage.success(`更新章节「${data.title}」成功。`)
          chapter.content.title = update.title
          chapter.content.content = data.content // 直接使用字符串列表
          // 更新章节列表中的标题
          const currentChapter = chapter.chapterList[chapter.currentChapterIndex]
          if (currentChapter) {
            currentChapter.title = update.title
          }
          closeUpdatePanel()
        })
        .catch(err => {
          console.log(err)
        })
    }

    // 上传章节
    function toUploadChapter() {
      store.commit('openUploadDrawer', {
        type: 'novel',
        mode: 'chapter',
        context: {
          novelId: chapter.novel.id,
          novelTitle: chapter.novel.title
        },
      })
    }



    function increaseFontSize() {
      if (settings.fontSize < 24) {
        settings.fontSize += 2
      }
    }

    function decreaseFontSize() {
      if (settings.fontSize > 12) {
        settings.fontSize -= 2
      }
    }
    return {
      chapter, canGoPrevious, canGoNext, changeChapter,
      toChapter,
      loadNovel, retryLoad,
      settings, increaseFontSize, decreaseFontSize,
      update, openUpdatePanel, closeUpdatePanel, updateChapter, toUploadChapter,
      catalogMenu, managementMenu, toggleCatalogMenu, toggleManagementMenu,
      catalogPanelRef, managementPanelRef, leftMenuBarRef,
      showBackToTop, scrollToTop,
    }
  }
}
</script>

<style scoped>
/* 主容器 */
.novel-reader {
  min-height: 100vh;
  background: #f4f1ea;
  cursor: default;
}

/* 顶部导航栏 */
.reader-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e4e7ed;
  z-index: 1000;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-left ::v-deep(span) {
  font-family: 'Blueaka', sans-serif;
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
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, transform 0.15s ease;
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
  transform: scale(0.94);
}

.back-btn:focus-visible {
  outline: 2px solid rgba(64, 158, 255, 0.45);
  outline-offset: 2px;
}

.book-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.book-title {
  font-size: 18px;
  font-weight: 400;
  color: #303133;
  margin: 0;
  font-family: 'Blueaka', 'PingFang SC', sans-serif;
}

.book-author {
  font-size: 12px;
  color: #909399;
}

/* 主容器 */
.main-container {
  padding-top: 64px;
  min-height: calc(100vh - 64px);
  position: relative;
}

/* 阅读主区域 */
.reader-main {
  max-width: var(--reader-max-width, 1000px);
  margin: 0 auto;
  padding: 40px 24px;
  position: relative;
}

/* 左侧菜单栏 */
.left-side-menu-bar {
  position: fixed;
  left: calc(50% - var(--reader-max-width, 1000px) / 2 - 72px);
  top: calc(64px + 40px);
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.left-side-menu-bar ::v-deep(.el-divider) {
  font-family: 'Blueaka', sans-serif;
}

/* 右侧工具栏 */
.right-side-tool-bar {
  position: fixed;
  right: calc(50% - var(--reader-max-width, 1000px) / 2 - 72px);
  bottom: 40px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.menu-item-container {
  position: relative;
}

.menu-item {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.menu-item:hover {
  background: rgba(64, 158, 255, 0.1);
  border-color: #409eff;
  transform: scale(1.05);
  box-shadow: 0 3px 12px rgba(64, 158, 255, 0.1);
}

.menu-item .icon {
  width: 20px;
  height: 20px;
  color: #606266;
  margin-bottom: 2px;
}

.menu-item:hover .icon {
  color: #409eff;
}

.menu-text {
  font-size: 10px;
  color: #909399;
  line-height: 1;
  font-weight: 500;
}

.menu-item:hover .menu-text {
  color: #409eff;
}

/* 右侧工具项样式 */
.tool-item {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.tool-item:hover {
  background: rgba(64, 158, 255, 0.1);
  border-color: #409eff;
  transform: scale(1.05);
  box-shadow: 0 3px 12px rgba(64, 158, 255, 0.1);
}

.tool-item .icon {
  width: 20px;
  height: 20px;
  color: #606266;
  margin-bottom: 2px;
}

.tool-item:hover .icon {
  color: #409eff;
}

.tool-text {
  font-size: 10px;
  color: #909399;
  line-height: 1;
  font-weight: 500;
}

.tool-item:hover .tool-text {
  color: #409eff;
}

/* 侧边面板 */
.side-menu-panel {
  position: fixed;
  left: calc(50% - var(--reader-max-width, 1000px) / 2 - 72px + 72px);
  top: calc(64px + 40px);
  box-sizing: border-box;
  width: 400px;
  max-height: 70vh;
  background: #ffffff;
  border: 1px solid #e8edf3;
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.1);
  z-index: 11;
  overflow: hidden;
  font-family: 'Blueaka', 'PingFang SC', sans-serif;
  animation: slideInRight 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(-10px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 56px;
  box-sizing: border-box;
  padding: 12px 16px 12px 20px;
  background: #ffffff;
  border-bottom: 1px solid #edf0f4;
  color: #303133;
  font-weight: 600;
  font-size: 16px;
}

.panel-header .chapter-count {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}

.close-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 32px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: #909399;
  font-family: Arial, sans-serif;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.close-btn:hover {
  color: #606266;
  background-color: #f3f4f6;
}

.close-btn:focus-visible {
  outline: 2px solid rgba(64, 158, 255, 0.35);
  outline-offset: 1px;
}

.panel-content {
  max-height: calc(70vh - 57px);
  overflow-y: auto;
  padding: 0;
  scrollbar-width: thin;
  scrollbar-color: #d8dee8 transparent;
}

.panel-content::-webkit-scrollbar {
  width: 6px;
}

.panel-content::-webkit-scrollbar-thumb {
  border-radius: 6px;
  background: #d8dee8;
}

.panel-content ::v-deep(.el-divider--horizontal) {
  margin: 24px 0;
  border-color: #edf0f4;
}

.panel-content ::v-deep(.el-divider--horizontal):first-child {
  margin-top: 6px;
}

.panel-content ::v-deep(.el-divider__text.is-left) {
  left: 12px;
  padding: 0 8px;
  background: #ffffff;
  color: #8a94a3;
  font-size: 12px;
  font-weight: 500;
}


/* 目录面板样式 */
.chapter-item {
  display: flex;
  align-items: center;
  min-height: 44px;
  box-sizing: border-box;
  padding: 11px 20px 11px 17px;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease;
  border-bottom: 1px solid #f0f2f5;
  border-left: 3px solid transparent;
}

.chapter-item:hover {
  background: #f5f9ff;
  color: #409eff;
}

.chapter-item.is-disabled {
  opacity: 0.55;
  cursor: not-allowed;
  pointer-events: none;
}

.chapter-item.active-chapter {
  background: #eef6ff;
  color: #409eff;
  font-weight: 600;
  border-left: 3px solid #409eff;
}

.chapter-item .chapter-number {
  width: 32px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.chapter-item .chapter-item-title {
  flex: 1;
  font-size: 14px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chapter-item.active-chapter .chapter-number {
  color: #409eff;
}

/* 管理面板样式 */
.management-panel .panel-content {
  padding: 18px 20px 20px;
}

.management-section {
  margin-bottom: 24px;
}

.management-section:last-child {
  margin-bottom: 0;
}

.management-section h4 {
  margin: 0 0 16px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.form-item {
  margin-bottom: 16px;
}

.form-item:last-child {
  margin-bottom: 0;
}

.form-item label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #606266;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}



/* 管理面板中的设置项样式 */
.setting-item {
  min-height: 36px;
  margin: 16px 0;
  display: flex;
  align-items: center;
  gap: 16px;
}

.setting-item:last-child {
  margin-bottom: 0;
}

.setting-item .setting-label {
  font-size: 13px;
  font-weight: 500;
  color: #5f6977;
  min-width: 64px;
  flex-shrink: 0;
  line-height: 36px;
}

/* 管理面板中的字体大小控制 */
.setting-item .font-size-controls {
  display: flex;
  align-items: center;
  gap: 0;
  flex: 1;
}

.setting-item .size-btn {
  width: 48px;
  height: 36px;
  border: 1px solid #dfe5ec;
  background: #ffffff;
  color: #606266;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.setting-item .size-btn:first-child {
  border-radius: 8px 0 0 8px;
  border-right: none;
}

.setting-item .size-btn:last-child {
  border-radius: 0 8px 8px 0;
  border-left: none;
}

.setting-item .size-btn:hover {
  background: #f0f7ff;
  color: #409eff;
  z-index: 1;
  border-color: #409eff;
}

.setting-item .font-size-display {
  width: 48px;
  height: 36px;
  padding: 0;
  border: 1px solid #dfe5ec;
  background: #f7f9fc;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 管理面板中的宽度选项 */
.setting-item .width-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
}

.setting-item .width-option {
  min-width: 48px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #dfe5ec;
  border-radius: 8px;
  background: #ffffff;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease;
  text-align: center;
}

.setting-item .width-option:hover {
  border-color: #409eff;
  color: #409eff;
}

.setting-item .width-option.active {
  border-color: #409eff;
  background: #eef6ff;
  color: #409eff;
}

/* 管理面板中的章节管理选项 */
.setting-item .chapter-management-options {
  display: flex;
  gap: 8px;
  flex: 1;
}

.setting-item .chapter-management-option {
  min-height: 38px;
  padding: 8px 12px;
  border: 1px solid #dfe5ec;
  border-radius: 8px;
  background: #ffffff;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease;
  flex: 1;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.setting-item .chapter-management-option:hover {
  border-color: #409eff;
  color: #409eff;
  background: #f0f7ff;
}

.setting-item .chapter-management-option:disabled {
  color: #c0c4cc;
  background: #f5f7fa;
  border-color: #e4e7ed;
  cursor: not-allowed;
}

.setting-item .chapter-management-option .icon {
  width: 14px;
  height: 14px;
}

/* 更新对话框 */
.update-dialog {
  border-radius: 12px;
}

.dialog-content {
  padding: 8px 0;
}

.dialog-content .form-item {
  margin-bottom: 24px;
}

.dialog-content .form-item:last-child {
  margin-bottom: 0;
}

.dialog-content .form-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.content-container {
  background: #fffdf9;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  margin-bottom: 32px;
  overflow: hidden;
}

.chapter-article {
  padding: 48px;
}

.chapter-header {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e4e7ed;
}

.chapter-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 16px 0;
  line-height: 1.4;
  font-family: 'Blueaka', 'PingFang SC', sans-serif;
}

.chapter-meta {
  display: flex;
  gap: 24px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #909399;
}

.meta-item .icon {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

.meta-tags {
  align-items: center;
}

.chapter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.meta-tag-item {
  margin: 0;
  font-size: 12px;
}

.chapter-content {
  font-size: 18px;
  line-height: 1.8;
  color: #303133;
  font-family: 'Blueaka', sans-serif;
}

.chapter-content .paragraph {
  margin: 1.2em 0;
  text-indent: 2em;
  line-height: inherit;
}

.reader-state {
  min-height: 400px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  padding: 48px 24px;
  text-align: center;
  color: #606266;
}

.reader-state h2 {
  margin: 0 0 12px;
  font-size: 22px;
  color: #303133;
}

.reader-state p {
  margin: 0 0 24px;
  line-height: 1.6;
}

.reader-state-loading p {
  margin-top: -24px;
  color: #909399;
}

.reader-state-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.chapter-error {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: #fef0f0;
  border-bottom: 1px solid #fde2e2;
  color: #f56c6c;
  font-size: 14px;
}

/* 章节导航 */
.chapter-navigation {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  background: #fffdf9;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.chapter-nav-button {
  min-width: 0;
  min-height: 72px;
  padding: 20px 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
  color: #606266;
  background: transparent;
  font-family: 'Blueaka', 'PingFang SC', sans-serif;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.chapter-nav-button.next-btn {
  border-left: 1px solid #ebeef5;
}

.chapter-nav-button .icon {
  width: 18px;
  height: 18px;
  transition: transform 0.2s ease;
}

.chapter-nav-button:not(:disabled):hover {
  color: #409eff;
  background: #f3f8ff;
}

.chapter-nav-button.prev-btn:not(:disabled):hover .icon {
  transform: translateX(-3px);
}

.chapter-nav-button.next-btn:not(:disabled):hover .icon {
  transform: translateX(3px);
}

.chapter-nav-button:not(:disabled):active {
  background: #eaf3ff;
}

.chapter-nav-button:focus-visible {
  position: relative;
  z-index: 1;
  outline: 2px solid rgba(64, 158, 255, 0.45);
  outline-offset: -3px;
}

.chapter-nav-button:disabled {
  color: #c0c4cc;
  background: #fafafa;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .book-title {
    font-size: 16px;
  }

  .book-author {
    font-size: 11px;
  }



  .reader-main {
    padding: 0 16px 20px;
  }

  .chapter-article {
    padding: 24px 20px;
  }

  .chapter-title {
    font-size: 24px;
  }

  .chapter-content {
    font-size: 16px;
  }

  .left-side-menu-bar {
    position: static;
    flex-direction: row;
    justify-content: center;
    margin-top: 20px;
    left: auto;
    top: auto;
    transform: none;
  }

  .content-container {
    margin-left: 0;
    /* 移动端取消左边距 */
  }

  .chapter-navigation {
    margin-left: 0;
    /* 移动端取消左边距 */
  }

  .side-menu-panel {
    left: 16px;
    right: 16px;
    top: calc(64px + 90px);
    width: auto;
    max-height: 60vh;
  }

  .chapter-nav-button {
    min-height: 64px;
    padding: 16px 20px;
  }
}

@media (max-width: 480px) {
  .reader-main {
    padding-right: 0;
    padding-left: 0;
  }

  .content-container,
  .chapter-navigation {
    border-right: none;
    border-left: none;
    border-radius: 0;
    box-shadow: none;
  }

  .chapter-nav-button {
    min-height: 60px;
    padding: 14px 12px;
    font-size: 15px;
  }

  .header-left {
    gap: 12px;
    width: 100%;
    min-width: 0;
  }

  .book-info {
    min-width: 0;
    flex: 1;
  }

  .book-title {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .chapter-meta {
    flex-direction: column;
    gap: 8px;
  }

  .meta-tags {
    align-items: flex-start;
  }

  .chapter-tags {
    margin-top: 4px;
  }

  .chapter-content {
    font-size: 15px;
    line-height: 1.7;
  }
}

/* 图标样式 */
.icon {
  width: 1em;
  height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
  display: inline-block;
}

/* 响应式设计 - 左侧菜单和右侧工具栏 */
@media (max-width: 768px) {
  .left-side-menu-bar {
    position: static;
    flex-direction: row;
    justify-content: center;
    margin-bottom: 20px;
    left: auto;
    top: auto;
    transform: none;
  }

  .right-side-tool-bar {
    position: fixed;
    right: 20px;
    bottom: 20px;
    left: auto;
    transform: none;
  }
}

@media (max-width: 480px) {

  .menu-item,
  .tool-item {
    width: 48px;
    height: 48px;
  }

  .menu-item .icon,
  .tool-item .icon {
    width: 18px;
    height: 18px;
  }

  .menu-text,
  .tool-text {
    font-size: 9px;
  }

  .right-side-tool-bar {
    right: 16px;
    bottom: 16px;
  }
}
</style>
