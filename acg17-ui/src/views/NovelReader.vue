<template>
  <div class="novel-reader" contenteditable="true" autofocus @keydown.left="changeChapter(-1)"
    @keydown.right="changeChapter(1)" @keydown.esc="$router.back()" :style="{
      '--reader-max-width': settings.selectedWidth === 'auto' ? '900px' : settings.selectedWidth + 'px'
    }">

    <!-- 顶部导航栏 -->
    <div class="reader-header" contenteditable="false">
      <div class="header-content">
        <div class="header-left">
          <el-button @click="$router.back()" text class="back-btn">
            <icon icon="#icon-left"></icon>
            返回
          </el-button>
          <div class="book-info">
            <h1 class="book-title">{{ chapter.novel.title }}</h1>
          </div>
        </div>

      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-container" contenteditable="false">

      <!-- 左侧菜单栏 -->
      <div class="left-side-menu-bar" ref="leftMenuBarRef">
        <!-- 目录菜单 -->
        <div class="menu-item-container">
          <div class="menu-item" @click="toggleCatalogMenu">
            <icon icon="#icon-catalog"></icon>
            <span class="menu-text">目录</span>
          </div>
          <!-- 目录面板 -->
          <div v-show="catalogMenu.show" class="side-menu-panel catalog-panel" ref="catalogPanelRef">
            <div class="panel-header">
              <div>
                <span style="margin-right: 12px;">章节目录</span>
                <span class="chapter-count">共 {{ chapter.chapterList.length }} 章</span>
              </div>
              <button class="close-btn" @click="catalogMenu.show = false">×</button>
            </div>
            <div class="panel-content">
              <div v-for="(item, i) in chapter.chapterList" :key="item.id" @click="toChapter(item.id, i)"
                :class="{ 'active-chapter': i === chapter.currentChapterIndex }" class="chapter-item">
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
                  <button @click="openUpdatePanel" class="chapter-management-option">
                    <icon icon="#icon-edit"></icon>
                    修改章节
                  </button>
                  <button @click="toUploadChapter" class="chapter-management-option">
                    <icon icon="#icon-add"></icon>
                    新增章节
                  </button>
                </div>
              </div>

              <!-- 阅读设置操作 -->
              <el-divider content-position="left">阅读设置</el-divider>

              <!-- 正文字体 -->
              <div class="setting-item">
                <label class="setting-label">正文字体</label>
                <div class="font-options">
                  <button v-for="font in settings.fontOptions" :key="font.value"
                    @click="settings.selectedFont = font.value"
                    :class="['font-option', { 'active': settings.selectedFont === font.value }]">
                    {{ font.label }}
                  </button>
                </div>
              </div>

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

              <!-- 翻页模式 -->
              <div class="setting-item">
                <label class="setting-label">翻页模式</label>
                <div class="page-mode-options">
                  <button @click="settings.pageMode = 'chapter'"
                    :class="['page-mode-option', { 'active': settings.pageMode === 'chapter' }]">
                    章节翻页
                  </button>
                  <button @click="settings.pageMode = 'scroll'"
                    :class="['page-mode-option', { 'active': settings.pageMode === 'scroll' }]">
                    滚动翻页
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧工具栏 -->
      <div class="right-side-tool-bar">
        <div class="tool-item" @click="scrollToTop" title="回到顶部">
          <icon icon="#icon-left" style="transform: rotate(90deg);"></icon>
          <span class="tool-text">顶部</span>
        </div>
      </div>

      <!-- 阅读主区域 -->
      <div class="reader-main">
        <div class="content-container">
          <article v-show="!chapter.loading" class="chapter-article">
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
            <div class="chapter-content" :style="{
              fontSize: settings.fontSize + 'px',
              fontFamily: settings.selectedFont === 'Blueaka' ? '\'Blueaka\', \'PingFang SC\', sans-serif' :
                settings.selectedFont === 'SimSun' ? '\'SimSun\', serif' :
                  '\'KaiTi\', serif'
            }">
              <p v-for="(paragraph, index) in chapter.content.content" :key="index" class="paragraph">
                {{ paragraph }}
              </p>
            </div>
          </article>

          <!-- 加载状态 -->
          <div v-show="chapter.loading" class="loading-container">
            <acg17-loading-heart></acg17-loading-heart>
          </div>
        </div>

        <!-- 章节导航 -->
        <div class="chapter-navigation">
          <el-button :disabled="chapter.content.id === chapter.chapterList[0].id" @click="changeChapter(-1)"
            class="nav-button prev-btn">
            <icon icon="#icon-left"></icon>
            上一章
          </el-button>

          <el-dropdown trigger="hover" class="catalog-dropdown-nav">
            <el-button text class="catalog-btn">
              <icon icon="#icon-catalog"></icon>
              目录
              <span class="dropdown-arrow-small">▼</span>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu class="catalog-dropdown-menu">
                <div class="catalog-header">
                  <span>章节目录</span>
                  <span class="chapter-count">共 {{ chapter.chapterList.length }} 章</span>
                </div>
                <el-dropdown-item v-for="(item, i) in chapter.chapterList" :key="item.id" @click="toChapter(item.id, i)"
                  :class="{ 'active-chapter': i === chapter.currentChapterIndex }" class="chapter-dropdown-item">
                  <span class="chapter-number">{{ i + 1 }}</span>
                  <span class="dropdown-chapter-title">{{ item.title }}</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-button :disabled="chapter.content.id === chapter.chapterList[chapter.chapterList.length - 1].id"
            @click="changeChapter(1)" class="nav-button next-btn">
            下一章
            <icon icon="#icon-right"></icon>
          </el-button>
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
import { reactive, onBeforeMount, onMounted, onUnmounted, ref } from 'vue';
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router';
import server from '@/util/request';
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

    // 面板引用
    const catalogPanelRef = ref(null)
    const managementPanelRef = ref(null)
    const leftMenuBarRef = ref(null)

    const chapter = reactive({
      novel: { id: '', title: '', author: '', tags: [], totalWords: '', updateTime: '' },
      chapterList: [{}],
      content: { id: '', title: '', content: [], totalWords: '', updateTime: '' },
      currentChapterIndex: 0,
      loading: false,
    })
    // 加载小说，和第一章内容
    onBeforeMount(() => {
      chapter.loading = true
      server.get('/novel/getContentById/' + route.params.novelId)
        .then(res => {
          // 如果res.data为空，则进入404页面
          if (!res.data) {
            router.push('/404')
            return
          }
          chapter.novel = res.data
          chapter.chapterList = res.chapterList
          chapter.content = res.firstChapter
          chapter.loading = false
        })
        .catch(err => {
          chapter.loading = false
          console.log(err)
        })
    })
    // 切换章节
    function changeChapter(change) {
      // 第一章不能跳上一章，最后一章不能跳下一章
      if (chapter.currentChapterIndex === 0 && change === -1) return
      if (chapter.currentChapterIndex === chapter.chapterList.length - 1 && change === 1) return
      // 加载章节内容
      chapter.currentChapterIndex = chapter.currentChapterIndex + change
      getChapter(chapter.chapterList[chapter.currentChapterIndex].id)
    }

    // 跳转章节
    function toChapter(id, i) {
      chapter.currentChapterIndex = i
      getChapter(id)
    }
    function getChapter(id) {
      chapter.loading = true
      server.get('/novel-chapter/getContentById/' + id)
        .then(res => {
          chapter.loading = false
          chapter.content = res.data
        })
        .catch(err => {
          chapter.loading = false
          console.log(err)
        })
    }

    // 阅读设置
    const settings = reactive({
      selectedFont: 'Blueaka',
      fontSize: 18,
      selectedWidth: 900,
      pageMode: 'chapter',
      fontOptions: [
        { label: 'Blueaka', value: 'Blueaka' },
        { label: '苹方', value: 'PingFang SC' },
        { label: '无衬线体', value: 'sans-serif' }
      ],
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

    // 添加和移除事件监听器
    onMounted(() => {
      document.addEventListener('click', handleClickOutside)
    })

    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside)
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

    // 回到顶部功能
    function scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      })
    }

    return {
      chapter, changeChapter,
      toChapter,
      settings, increaseFontSize, decreaseFontSize,
      update, openUpdatePanel, closeUpdatePanel, updateChapter, toUploadChapter,
      catalogMenu, managementMenu, toggleCatalogMenu, toggleManagementMenu,
      catalogPanelRef, managementPanelRef, leftMenuBarRef,
      scrollToTop,
    }
  }
}
</script>

<style scoped>
/* 主容器 */
.novel-reader {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  outline: none;
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
  gap: 8px;
  color: #606266;
  font-size: 14px;
}

.back-btn:hover {
  color: #409eff;
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
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
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
  box-shadow: 0 6px 25px rgba(64, 158, 255, 0.2);
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
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
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
  box-shadow: 0 6px 25px rgba(64, 158, 255, 0.2);
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
  width: 400px;
  max-height: 70vh;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(15px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  z-index: 11;
  overflow: hidden;
  animation: slideInRight 0.3s ease-out;
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(-20px);
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
  padding: 16px 20px;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  color: white;
  font-weight: 600;
  font-size: 16px;
}

.panel-header .chapter-count {
  font-size: 12px;
  opacity: 0.95;
  font-weight: normal;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 20px;
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
  max-height: calc(70vh - 60px);
  overflow-y: auto;
  padding: 0;
}

.panel-content ::v-deep(.el-divider--horizontal) {
  margin: 30px 0;
}

.panel-content ::v-deep(.el-divider--horizontal):first-child {
  margin-top: 12px;
}

.panel-content ::v-deep(.el-divider__text.is-left) {
  left: 12px;
  padding: 12px;
}


/* 目录面板样式 */
.catalog-panel .panel-content {
  padding: 8px 0;
}

.chapter-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-bottom: 1px solid #f0f0f0;
}

.chapter-item:hover {
  background: #f8f9ff;
  color: #409eff;
}

.chapter-item.active-chapter {
  background: linear-gradient(135deg, #ecf5ff 0%, #e6f7ff 100%);
  color: #409eff;
  font-weight: 500;
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
  padding: 20px;
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
  margin: 20px 0;
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.setting-item:last-child {
  margin-bottom: 0;
}

.setting-item .setting-label {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  min-width: 60px;
  flex-shrink: 0;
  line-height: 32px;
}

/* 管理面板中的字体选项 */
.setting-item .font-options {
  display: flex;
  gap: 8px;
  flex: 1;
}

.setting-item .font-option {
  padding: 8px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #f5f7fa;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  flex: 1;
  text-align: center;
}

.setting-item .font-option:hover {
  border-color: #409eff;
  color: #409eff;
}

.setting-item .font-option.active {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}

/* 管理面板中的字体大小控制 */
.setting-item .font-size-controls {
  display: flex;
  align-items: center;
  gap: 0;
  flex: 1;
}

.setting-item .size-btn {
  width: 50px;
  height: 32px;
  border: 1px solid #e4e7ed;
  background: #f5f7fa;
  color: #606266;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.setting-item .size-btn:first-child {
  border-radius: 6px 0 0 6px;
  border-right: none;
}

.setting-item .size-btn:last-child {
  border-radius: 0 6px 6px 0;
  border-left: none;
}

.setting-item .size-btn:hover {
  background: #ecf5ff;
  color: #409eff;
  z-index: 1;
  border-color: #409eff;
}

.setting-item .font-size-display {
  width: 50px;
  height: 32px;
  border: 1px solid #e4e7ed;
  background: #f5f7fa;
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
  gap: 6px;
  flex-wrap: wrap;
  flex: 1;
}

.setting-item .width-option {
  padding: 6px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #f5f7fa;
  color: #606266;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 45px;
  text-align: center;
}

.setting-item .width-option:hover {
  border-color: #409eff;
  color: #409eff;
}

.setting-item .width-option.active {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}

/* 管理面板中的翻页模式选项 */
.setting-item .page-mode-options {
  display: flex;
  gap: 8px;
  flex: 1;
}

.setting-item .page-mode-option {
  padding: 8px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #f5f7fa;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  flex: 1;
  text-align: center;
}

.setting-item .page-mode-option:hover {
  border-color: #409eff;
  color: #409eff;
}

.setting-item .page-mode-option.active {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}

/* 管理面板中的章节管理选项 */
.setting-item .chapter-management-options {
  display: flex;
  gap: 8px;
  flex: 1;
}

.setting-item .chapter-management-option {
  padding: 8px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #f5f7fa;
  color: #606266;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
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
  background: #ecf5ff;
}

.setting-item .chapter-management-option .icon {
  width: 12px;
  height: 12px;
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
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
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
  font-family: 'PingFang SC', sans-serif;
}

.chapter-content .paragraph {
  margin: 1.2em 0;
  text-indent: 2em;
  line-height: inherit;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}

/* 章节导航 */
.chapter-navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  border-radius: 12px;
  padding: 20px 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.nav-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  font-size: 16px;
}

.catalog-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 16px;
}

.catalog-btn:hover {
  color: #409eff;
}

/* 导航区域的目录下拉菜单 */
.catalog-dropdown-nav {
  margin: 0 16px;
}

.dropdown-arrow-small {
  margin-left: 6px;
  font-size: 8px;
  color: #909399;
  transition: transform 0.3s ease;
  display: inline-block;
}

.catalog-dropdown-nav:hover .dropdown-arrow-small {
  transform: rotate(180deg);
  color: #409eff;
}

/* 设置对话框 */
.settings-dialog {
  border-radius: 12px;
}

.settings-content {
  padding: 20px 0;
}

.setting-group {
  margin-bottom: 32px;
}

.setting-group:last-child {
  margin-bottom: 0;
}

.setting-label {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 字体选项 */
.font-options {
  display: flex;
  gap: 12px;
}

.font-option {
  padding: 8px 20px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.font-option:hover {
  border-color: #409eff;
  color: #409eff;
}

.font-option.active {
  border-color: #f56c6c;
  background: #fef0f0;
  color: #f56c6c;
}

/* 字体大小控制 */
.font-size-controls {
  display: flex;
  align-items: center;
  gap: 20px;
}

.size-btn {
  width: 60px;
  height: 40px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  color: #606266;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.size-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.font-size-display {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  min-width: 30px;
  text-align: center;
}

/* 宽度选项 */
.width-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.width-option {
  padding: 8px 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 60px;
  text-align: center;
}

.width-option:hover {
  border-color: #409eff;
  color: #409eff;
}

.width-option.active {
  border-color: #f56c6c;
  background: #fef0f0;
  color: #f56c6c;
}

/* 翻页模式选项 */
.page-mode-options {
  display: flex;
  gap: 12px;
}

.page-mode-option {
  padding: 12px 24px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  flex: 1;
  text-align: center;
}

.page-mode-option:hover {
  border-color: #409eff;
  color: #409eff;
}

.page-mode-option.active {
  border-color: #f56c6c;
  background: #fef0f0;
  color: #f56c6c;
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
    padding: 16px 20px;
    margin-left: 0;
    /* 移动端取消左边距 */
    flex-direction: column;
    gap: 16px;
  }

  .side-menu-panel {
    left: 16px;
    right: 16px;
    top: calc(64px + 90px);
    width: auto;
    max-height: 60vh;
  }

  .nav-button {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .header-left {
    gap: 12px;
  }

  .book-info {
    display: none;
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

/* Element Plus 下拉菜单样式覆盖 */
:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
}

:deep(.el-dropdown-menu__item .icon) {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: #f5f7fa;
  color: #409eff;
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
