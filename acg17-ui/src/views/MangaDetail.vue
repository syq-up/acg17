<template>
  <section class="manga-detail">
    <div class="side-btn-group left-btn-group">
      <button type="button" class="side-btn" aria-label="随机漫画" title="随机漫画" @click="randomManga">
        <icon icon="#icon-random"></icon>
      </button>
      <button type="button" class="side-btn" aria-label="返回上一页" title="返回上一页" @click="goBack">
        <icon icon="#icon-left"></icon>
      </button>
      <button
        v-show="showBackToTopLeft"
        type="button"
        class="side-btn"
        aria-label="返回顶部"
        title="返回顶部"
        @click="scrollToTop"
      >
        <icon icon="#icon-sort-asc"></icon>
      </button>
    </div>

    <button
      v-show="showBackToTopRight"
      type="button"
      class="side-btn right-btn"
      aria-label="返回顶部"
      title="返回顶部"
      @click="scrollToTop"
    >
      <icon icon="#icon-sort-asc"></icon>
    </button>

    <div v-if="isLoading" class="detail-state" role="status" aria-live="polite">
      正在加载漫画详情…
    </div>

    <div v-else-if="loadError" class="detail-state detail-state-error" role="alert">
      <p>{{ loadError }}</p>
      <button type="button" class="state-retry-btn" @click="loadMangaDetail">重试</button>
    </div>

    <template v-else-if="hasLoaded">
      <div class="manga-container">
        <!-- 漫画封面 -->
        <div class="manga-cover unselectable">
          <img :src="withMediaStyle(manga.cover, 'medium')" :alt="displayTitle" />
        </div>

        <!-- 漫画信息 -->
        <div class="manga-info">
          <h1 class="manga-title">{{ displayTitle }}</h1>
          <p v-if="showOriginalSubtitle" class="manga-original-title">{{ originalTitle }}</p>

          <div class="manga-meta" aria-label="漫画信息">
            <span>{{ mangaChapters.length }}章</span>
            <span class="meta-separator" aria-hidden="true">·</span>
            <span>{{ manga.pageCount }}页</span>
            <template v-if="updateTimeText">
              <span class="meta-separator" aria-hidden="true">·</span>
              <span>更新于 {{ updateTimeText }}</span>
            </template>
          </div>

          <div v-for="group in tagGroups" :key="group.key" class="info-row unselectable">
            <span class="label">{{ group.label }}:</span>
            <div class="tags-container">
              <button
                v-for="tag in group.tags"
                :key="tag.tagId"
                type="button"
                class="tag"
                :title="`按${tag.tagName}筛选`"
                @click="searchByTag(tag.tagId)"
              >
                {{ tag.tagName }} <span class="tag-count">{{ tag.tagCount }}</span>
              </button>
              <button
                v-if="isTagManagementMode"
                class="edit-tag-btn"
                type="button"
                :title="`编辑${group.label}标签`"
                :aria-label="`编辑${group.label}标签`"
                :disabled="!manga.id"
                @click="openTagEditor(group.key)"
              >
                <icon icon="#icon-edit" class="edit-icon"></icon>
              </button>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-buttons unselectable">
            <button
              type="button"
              class="btn-primary"
              :disabled="!canStartReading"
              @click="startReading"
            >
              开始阅读
            </button>
            <button
              type="button"
              :class="manga.favorite ? 'btn-icon favorite' : 'btn-icon'"
              :disabled="isFavoritePending"
              :aria-label="manga.favorite ? '取消喜欢' : '添加喜欢'"
              :title="manga.favorite ? '取消喜欢' : '添加喜欢'"
              @click="toggleFavorite"
            >
              <icon :icon="manga.favorite ? '#icon-favorite-y' : '#icon-favorite-n'" class="icon-svg"></icon>
            </button>
            <button
              type="button"
              :class="manga.deleted ? 'btn-icon restore' : 'btn-icon'"
              :disabled="isDeletePending"
              :aria-label="manga.deleted ? '恢复漫画' : '删除漫画'"
              :title="manga.deleted ? '恢复漫画' : '删除漫画'"
              @click="toggleDeleteStatus"
            >
              <icon :icon="manga.deleted ? '#icon-restore' : '#icon-delete'" class="icon-svg"></icon>
            </button>
            <button
              :class="['btn-icon', { 'tag-management-active': isTagManagementMode }]"
              type="button"
              :title="tagManagementToggleLabel"
              :aria-label="tagManagementToggleLabel"
              :aria-pressed="isTagManagementMode"
              @click="toggleTagManagement"
            >
              <icon icon="#icon-tag" class="icon-svg"></icon>
            </button>
          </div>
        </div>
      </div>

      <div
        v-if="typeof manga.description === 'string' && manga.description.trim()"
        class="manga-description"
      >
        <h3>简介</h3>
        <p>{{ manga.description }}</p>
      </div>

      <!-- 漫画章节列表 -->
      <div v-if="mangaChapters.length !== 1" class="manga-chapters unselectable">
        <h3>章节列表</h3>
        <div v-if="mangaChapters.length === 0" class="empty-state">暂无章节</div>
        <div v-else class="chapters-simple-list">
          <button
            v-for="chapterItem in mangaChapters"
            :key="'c' + chapterItem.chapter"
            type="button"
            :class="['chapter-simple-item', { active: manga.currentChapter === chapterItem.chapter }]"
            :aria-pressed="manga.currentChapter === chapterItem.chapter"
            @click="goToChapter(chapterItem.chapter)"
          >
            <span>{{ chapterItem.title || `第${chapterItem.chapter}话` }}</span>
            <span class="chapter-page-count">{{ chapterItem.pageCount }}页</span>
          </button>
        </div>
      </div>

      <!-- 漫画页面缩略图 -->
      <div class="manga-pages unselectable">
        <h3 ref="pagesHeading" tabindex="-1">漫画页面</h3>
        <div v-if="mangaChapters.length === 0" class="empty-state">暂无可阅读页面</div>
        <div v-else-if="mangaPages.length === 0" class="empty-state">当前章节暂无页面</div>
        <ul v-else class="pages-list">
          <li v-for="pageItem in visibleMangaPages" :key="'p' + pageItem.page" class="page-item"
            @click="goToPage(pageItem.page)">
            <img :src="withMediaStyle(pageItem.path, 'small')" :alt="`第${pageItem.page}页`" loading="lazy" decoding="async" />
            <button
              type="button"
              class="page-remove"
              :disabled="isRemovingPage(pageItem.page)"
              :aria-label="`删除第${pageItem.page}页`"
              :title="`删除第${pageItem.page}页`"
              @click.stop="removeMangaPage(pageItem.page)"
            >×</button>
            <div class="page-number">{{ pageItem.page }}</div>
          </li>
          <li
            v-if="hasMoreMangaPages"
            ref="mangaPagesSentinel"
            class="manga-pages-sentinel"
            role="status"
            aria-live="polite"
          >
            正在加载更多页面…
          </li>
        </ul>
      </div>

      <!-- 相关推荐 -->
      <div class="related-manga unselectable">
        <h3>相关推荐</h3>
        <div class="related-list">
          <div v-for="item in relatedManga" :key="item.id" class="related-item" @click="goToManga(item.id)">
            <img :src="withMediaStyle(item.cover, 'small')" :alt="item.title" />
            <div class="related-title">{{ item.title }}</div>
          </div>
        </div>
      </div>
    </template>
  </section>

  <manga-tag-editor
    v-if="activeTagCategory"
    :manga-id="manga.id"
    :category="activeTagCategory"
    :current-tags="activeTagEditorTags"
    @close="closeTagEditor"
    @updated="refreshMangaTags"
  />

  <acg17-footer></acg17-footer>
</template>

<script>
import { computed, reactive, onMounted, onUnmounted, ref, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Acg17Footer from '../components/Acg17Footer'
import MangaTagEditor from '../components/manga-detail/MangaTagEditor.vue'
import { withMediaStyle } from '@/util/media'
import server from '@/util/request'
import { MANGA_TAG_CATEGORIES } from '@/constants/mangaTagCategories'

const INITIAL_MANGA_PAGE_COUNT = 18
const MANGA_PAGE_BATCH_SIZE = 6

export default {
  name: 'MangaDetail',
  components: {
    'acg17-footer': Acg17Footer,
    MangaTagEditor,
  },
  setup() {
    const route = useRoute()
    const router = useRouter()

    function createEmptyManga() {
      return {
        id: null,
        title: '',
        chineseTitle: '',
        cover: '',
        description: '',
        pages: [],
        ...Object.fromEntries(MANGA_TAG_CATEGORIES.map(category => [category.field, []])),
        favorite: false,
        deleted: false,
        updateTime: '',
        pageCount: 0,
        currentChapter: 0,
      }
    }

    const manga = reactive(createEmptyManga())

    const relatedManga = reactive([])

    const isLoading = ref(false)
    const loadError = ref('')
    const hasLoaded = ref(false)
    const isFavoritePending = ref(false)
    const isDeletePending = ref(false)
    const pendingPageDeletes = reactive(new Set())
    const pagesHeading = ref(null)
    let latestRequestVersion = 0

    // 漫画页面数据直接使用后端返回的pages
    const mangaPages = reactive([])
    const visiblePageCount = ref(INITIAL_MANGA_PAGE_COUNT)
    const mangaPagesSentinel = ref(null)
    const visibleMangaPages = computed(() => mangaPages.slice(0, visiblePageCount.value))
    const hasMoreMangaPages = computed(() => visiblePageCount.value < mangaPages.length)
    let mangaPagesObserver = null

    // 章节列表数据
    const mangaChapters = reactive([])

    const activeTagCategory = ref('')
    const isTagManagementMode = ref(false)
    const allTagGroups = computed(() => MANGA_TAG_CATEGORIES.map(group => ({
      ...group,
      tags: [...(manga[group.field] || [])]
        .sort((a, b) => (b.tagCount || 0) - (a.tagCount || 0)),
    })))
    const tagGroups = computed(() => (
      isTagManagementMode.value
        ? allTagGroups.value
        : allTagGroups.value.filter(group => group.tags.length > 0)
    ))
    const tagManagementToggleLabel = computed(() => (
      isTagManagementMode.value ? '退出标签管理' : '进入标签管理'
    ))
    const activeTagEditorTags = computed(() => (
      allTagGroups.value.find(group => group.key === activeTagCategory.value)?.tags || []
    ))

    const showBackToTopLeft = ref(false)
    const showBackToTopRight = ref(false)
    const displayTitle = computed(() => (
      typeof manga.chineseTitle === 'string' && manga.chineseTitle.trim()
        ? manga.chineseTitle.trim()
        : (typeof manga.title === 'string' ? manga.title.trim() : '')
    ))
    const originalTitle = computed(() => (
      typeof manga.title === 'string' ? manga.title.trim() : ''
    ))
    const updateTimeText = computed(() => (
      typeof manga.updateTime === 'string' ? manga.updateTime.trim() : ''
    ))
    const showOriginalSubtitle = computed(() => (
      Boolean(originalTitle.value)
      && Boolean(typeof manga.chineseTitle === 'string' && manga.chineseTitle.trim())
      && originalTitle.value !== manga.chineseTitle.trim()
    ))
    const hasCurrentChapterPages = computed(() => (
      mangaChapters.length > 0 && mangaPages.length > 0
    ))
    const canStartReading = computed(() => (
      hasLoaded.value
      && Number.isInteger(manga.id)
      && manga.id > 0
      && hasCurrentChapterPages.value
    ))

    const handleScroll = () => {
      showBackToTopLeft.value = window.scrollY > 50
      showBackToTopRight.value = window.scrollY > 500
    }

    function supportsIntersectionObserver() {
      return typeof window !== 'undefined' && typeof window.IntersectionObserver === 'function'
    }

    function disconnectMangaPagesObserver() {
      if (mangaPagesObserver) {
        mangaPagesObserver.disconnect()
        mangaPagesObserver = null
      }
    }

    function observeMangaPagesSentinel() {
      if (!supportsIntersectionObserver()) {
        visiblePageCount.value = mangaPages.length
        disconnectMangaPagesObserver()
        return
      }

      if (!hasMoreMangaPages.value || !mangaPagesSentinel.value) {
        disconnectMangaPagesObserver()
        return
      }

      // The sentinel may remain inside the expanded root after a batch is added.
      // Recreate the observer so its initial intersection is evaluated again.
      disconnectMangaPagesObserver()
      mangaPagesObserver = new window.IntersectionObserver((entries) => {
        if (entries.some(entry => entry.isIntersecting)) {
          visiblePageCount.value = Math.min(
            visiblePageCount.value + MANGA_PAGE_BATCH_SIZE,
            mangaPages.length
          )
        }
      }, {
        rootMargin: '0px 0px 500px 0px',
        threshold: 0,
      })

      mangaPagesObserver.observe(mangaPagesSentinel.value)
    }

    function resetMangaPageVisibility() {
      disconnectMangaPagesObserver()
      visiblePageCount.value = supportsIntersectionObserver()
        ? Math.min(INITIAL_MANGA_PAGE_COUNT, mangaPages.length)
        : mangaPages.length
      nextTick(observeMangaPagesSentinel)
    }

    function clearMangaDetail() {
      Object.assign(manga, createEmptyManga())
      mangaChapters.length = 0
      mangaPages.length = 0
      pendingPageDeletes.clear()
      resetMangaPageVisibility()
    }

    watch(
      [() => mangaPages.length, visiblePageCount],
      () => {
        nextTick(observeMangaPagesSentinel)
      },
      { flush: 'post' }
    )

    function getRouteMangaId() {
      const routeId = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id
      const normalizedId = String(routeId || '')
      if (!/^\d+$/.test(normalizedId)) {
        return null
      }

      const mangaId = Number(normalizedId)
      return Number.isSafeInteger(mangaId) && mangaId > 0 ? mangaId : null
    }

    function isNotFoundError(error) {
      const status = error?.response?.status || error?.status || error?.code
      return status === 404 || (
        typeof error === 'string'
        && (/\b404\b/.test(error) || error.includes('不存在'))
      )
    }

    function normalizeChapters(pages) {
      if (!Array.isArray(pages)) {
        return []
      }

      return pages.map((chapter, index) => {
        const pagelist = Array.isArray(chapter?.pagelist) ? [...chapter.pagelist] : []
        return {
          ...chapter,
          chapter: chapter?.chapter ?? index + 1,
          title: chapter?.title || '',
          pagelist,
          pageCount: pagelist.length,
        }
      })
    }

    function syncPageMetrics() {
      mangaChapters.forEach(chapter => {
        chapter.pagelist = Array.isArray(chapter.pagelist) ? chapter.pagelist : []
        chapter.pageCount = chapter.pagelist.length
      })
      manga.pageCount = mangaChapters.reduce((total, chapter) => total + chapter.pageCount, 0)

      const currentChapter = mangaChapters.find(chapter => chapter.chapter === manga.currentChapter)
      const currentPages = currentChapter?.pagelist || []
      mangaPages.splice(0, mangaPages.length, ...currentPages)
      visiblePageCount.value = Math.min(Math.max(visiblePageCount.value, 0), mangaPages.length)
      nextTick(observeMangaPagesSentinel)
    }

    function applyMangaData(mangaData, mangaId) {
      const chapters = normalizeChapters(mangaData.pages)
      Object.assign(manga, mangaData, { id: mangaData.id ?? mangaId, pages: chapters })
      manga.id = Number(manga.id)
      mangaChapters.push(...chapters)
      manga.currentChapter = chapters[0]?.chapter ?? 0
      mangaPages.push(...(chapters[0]?.pagelist || []))
      syncPageMetrics()
      resetMangaPageVisibility()
    }

    // 从后端加载漫画详情数据
    async function loadMangaDetail() {
      const requestVersion = ++latestRequestVersion
      const mangaId = getRouteMangaId()
      isLoading.value = true
      loadError.value = ''
      hasLoaded.value = false
      clearMangaDetail()

      if (!mangaId) {
        isLoading.value = false
        router.push('/404')
        return
      }

      try {
        const res = await server.get(`/manga/${mangaId}`)
        if (requestVersion !== latestRequestVersion) {
          return
        }
        if (res?.code === 404 || (res?.code === 200 && !res.data)) {
          router.push('/404')
          return
        }
        if (res?.code !== 200 || !res.data) {
          throw new Error(res?.message || '漫画详情请求失败')
        }

        applyMangaData(res.data, mangaId)
        hasLoaded.value = true
      } catch (error) {
        if (requestVersion !== latestRequestVersion) {
          return
        }
        if (isNotFoundError(error)) {
          router.push('/404')
          return
        }
        console.error('加载漫画详情时发生错误:', error)
        loadError.value = '漫画详情加载失败，请稍后重试。'
      } finally {
        if (requestVersion === latestRequestVersion) {
          isLoading.value = false
        }
      }
    }

    async function refreshMangaTags() {
      const mangaId = Number(manga.id)
      const routeMangaId = getRouteMangaId()
      const requestVersion = latestRequestVersion
      if (!Number.isSafeInteger(mangaId) || mangaId <= 0 || mangaId !== routeMangaId) {
        return
      }

      try {
        const res = await server.get(`/manga/${mangaId}`)
        const refreshedManga = res?.data
        if (
          res?.code !== 200
          || !refreshedManga
          || Number(refreshedManga.id) !== mangaId
          || getRouteMangaId() !== mangaId
          || Number(manga.id) !== mangaId
          || latestRequestVersion !== requestVersion
        ) {
          throw new Error('漫画标签刷新结果已失效')
        }

        MANGA_TAG_CATEGORIES.forEach(category => {
          if (Object.prototype.hasOwnProperty.call(refreshedManga, category.field)) {
            manga[category.field] = Array.isArray(refreshedManga[category.field])
              ? refreshedManga[category.field]
              : []
          }
        })
      } catch (error) {
        console.error('刷新漫画标签失败:', error)
      }
    }

    // 加载相关推荐漫画
    async function loadRelatedManga() {
      // TODO 后端提供了一个接口来获取相关推荐漫画
    }

    // 返回上一页（但排除漫画页)
    function goBack() {
      const prevPath = router.$prevRoutePath()
      // 定义匹配 /acg/manga/*/*/* 的正则表达式
      const acgMangaPageReg = /^\/acg\/manga\/[^/]+\/[^/]+\/[^/]+$/
      if (acgMangaPageReg.test(prevPath)) {
        router.push('/acg/manga')
      } else {
        router.back()
      }
    }

    function goToManga(id) {
      router.push(`/acg/manga/${id}`)
    }

    function startReading() {
      if (!canStartReading.value) {
        return
      }
      // 传递漫画数据到MangaReader页面，避免重复请求
      router.push({
        path: `/acg/manga/${manga.id}/${manga.currentChapter}/1`,
        state: {
          mangaData: {
            id: manga.id,
            title: manga.title,
            currentChapter: manga.currentChapter,
            pages: [...mangaPages]
          }
        }
      })
    }

    function goToPage(pageNumber) {
      if (!canStartReading.value || !mangaPages.some(page => page.page === pageNumber)) {
        return
      }
      // 传递漫画数据到MangaReader页面，避免重复请求
      router.push({
        path: `/acg/manga/${manga.id}/${manga.currentChapter}/${pageNumber}`,
        state: {
          mangaData: {
            id: manga.id,
            currentChapter: manga.currentChapter,
            title: manga.title,
            pages: [...mangaPages]
          }
        }
      })
    }

    onMounted(() => {
      window.addEventListener('scroll', handleScroll)
      handleScroll()
    })

    watch(() => route.params.id, () => {
      isTagManagementMode.value = false
      closeTagEditor()
      loadMangaDetail()
      loadRelatedManga()
    }, { immediate: true })

    onUnmounted(() => {
      latestRequestVersion += 1
      window.removeEventListener('scroll', handleScroll)
      disconnectMangaPagesObserver()
    })

    async function toggleFavorite() {
      if (!hasLoaded.value || !manga.id || isFavoritePending.value) {
        return
      }

      isFavoritePending.value = true
      const newFavoriteStatus = !manga.favorite
      try {
        const res = await server.put(`/manga/${manga.id}/favorite?favorite=${newFavoriteStatus}`)
        if (res?.code !== 200) {
          throw new Error('收藏操作失败')
        }
        manga.favorite = newFavoriteStatus
        ElMessage.success(newFavoriteStatus ? '已添加喜欢' : '已取消喜欢')
      } catch (error) {
        console.error('收藏操作失败:', error)
        if (error instanceof Error) {
          ElMessage.error('喜欢操作失败')
        }
      } finally {
        isFavoritePending.value = false
      }
    }

    async function toggleDeleteStatus() {
      if (!hasLoaded.value || !manga.id || isDeletePending.value) {
        return
      }

      isDeletePending.value = true
      try {
        if (manga.deleted) {
          // 恢复漫画
          const res = await server.put(`/manga/${manga.id}/restore`)
          if (res?.code !== 200) {
            throw new Error('恢复漫画失败')
          }
          manga.deleted = false
          ElMessage.success('漫画已恢复')
        } else {
          // 删除漫画
          await ElMessageBox.confirm(
            '确定删除这本漫画吗？删除后可以恢复。',
            '删除漫画',
            {
              confirmButtonText: '删除',
              cancelButtonText: '取消',
              type: 'warning'
            }
          )
          const res = await server.delete(`/manga/${manga.id}`)
          if (res?.code !== 200) {
            throw new Error('删除漫画失败')
          }
          manga.deleted = true
          ElMessage.success('漫画已删除')
        }
      } catch (error) {
        if (!isMessageCancelled(error)) {
          console.error('删除/恢复操作失败:', error)
          if (error instanceof Error) {
            ElMessage.error(manga.deleted ? '恢复漫画失败' : '删除漫画失败')
          }
        }
      } finally {
        isDeletePending.value = false
      }
    }

    function isMessageCancelled(error) {
      return error === 'cancel' || error === 'close' || error?.action === 'cancel' || error?.action === 'close'
    }

    // 随机打开一个漫画
    function randomManga() {
      scrollToTop()
      server.get('/manga/random')
        .then(response => {
          if (response.data) {
            router.push(`/acg/manga/${response.data.id}`)
          }
        })
        .catch(err => {
          console.log(err)
        })
    }

    function scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: "smooth"
      })
    }

    // 跳转到指定章节
    function goToChapter(chapterNum) {
      const chapterItem = mangaChapters.find(c => c.chapter === chapterNum)

      if (chapterItem) {
        manga.currentChapter = chapterItem.chapter
        mangaPages.splice(0, mangaPages.length, ...(chapterItem.pagelist || []))
        resetMangaPageVisibility()
        nextTick(() => {
          pagesHeading.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
        })
      }
    }

    // 根据标签搜索漫画
    function searchByTag(tagId) {
      router.push({
        path: '/acg/manga',
        query: { tagIds: String(tagId) }
      })
    }

    // 标签编辑器仅保存当前打开的分类，其余状态由编辑器组件管理
    function openTagEditor(category) {
      activeTagCategory.value = category
    }

    function toggleTagManagement() {
      isTagManagementMode.value = !isTagManagementMode.value
      if (!isTagManagementMode.value) {
        closeTagEditor()
      }
    }

    function closeTagEditor() {
      activeTagCategory.value = ''
    }

    // 删除漫画页
    async function removeMangaPage(pageNum) {
      if (!hasLoaded.value || !manga.id || pendingPageDeletes.has(pageNum)) {
        return
      }

      pendingPageDeletes.add(pageNum)
      try {
        await ElMessageBox.confirm(
          `确定删除第 ${pageNum} 页吗？删除后无法恢复。`,
          '删除漫画页',
          {
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        const res = await server.delete(`/manga/delete/page`, {
          params: {
            mangaId: manga.id,
            chapterId: manga.currentChapter,
            pageNum: pageNum
          }
        })
        if (res?.code !== 200) {
          throw new Error('删除漫画页失败')
        }
        const currentChapter = mangaChapters.find(chapter => chapter.chapter === manga.currentChapter)
        if (currentChapter) {
          currentChapter.pagelist = currentChapter.pagelist.filter(page => page.page !== pageNum)
        }
        syncPageMetrics()
        ElMessage.success(`第 ${pageNum} 页已删除`)
      } catch (error) {
        if (!isMessageCancelled(error)) {
          console.error('删除漫画页失败:', error)
          if (error instanceof Error) {
            ElMessage.error(`删除第 ${pageNum} 页失败`)
          }
        }
      } finally {
        pendingPageDeletes.delete(pageNum)
      }
    }

    function isRemovingPage(pageNum) {
      return pendingPageDeletes.has(pageNum)
    }

    return {
      manga,
      withMediaStyle,
      relatedManga,
      isLoading,
      loadError,
      hasLoaded,
      displayTitle,
      originalTitle,
      updateTimeText,
      showOriginalSubtitle,
      canStartReading,
      mangaPages,
      visibleMangaPages,
      hasMoreMangaPages,
      mangaPagesSentinel,
      mangaChapters,
      tagGroups,
      isTagManagementMode,
      tagManagementToggleLabel,
      activeTagCategory,
      activeTagEditorTags,
      pagesHeading,
      isFavoritePending,
      isDeletePending,
      isRemovingPage,
      goBack,
      goToManga,
      startReading,
      goToPage,
      goToChapter,
      toggleFavorite,
      toggleDeleteStatus,
      searchByTag,
      randomManga,
      scrollToTop,
      showBackToTopLeft,
      showBackToTopRight,
      openTagEditor,
      toggleTagManagement,
      closeTagEditor,
      loadMangaDetail,
      refreshMangaTags,
      removeMangaPage
    }
  }
}
</script>

<style scoped>
.manga-detail {
  --column: 6;
  --page-max-width: 220px;
  --gap: 12px;
  --container-padding: 20px;
  width: 100%;
  max-width: 1420px;
  margin: 84px auto 20px;
  padding: var(--container-padding);
  box-sizing: border-box;
  background-color: #ffffff;
  min-height: calc(100vh - 140px - 20px - 200px);
}

.detail-state {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: #495057;
  text-align: center;
}

.detail-state p {
  margin: 0;
}

.detail-state-error {
  color: #b42318;
}

.state-retry-btn {
  border: 1px solid #409eff;
  border-radius: 6px;
  padding: 8px 16px;
  background: #fff;
  color: #337ab7;
  cursor: pointer;
  font: inherit;
}

.manga-container {
  display: flex;
  gap: 30px;
  margin-bottom: 40px;
  height: auto;
}

.manga-cover {
  flex-shrink: 0;
  height: auto;
}

.manga-cover img {
  display: block;
  width: min(350px, 30vw);
  height: auto;
  aspect-ratio: 1 / 1.41;
  object-fit: contain;
  background-color: #f5f7fa;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.manga-info {
  flex: 1;
  min-width: 0;
  height: auto;
}

.manga-title {
  font-size: 28px;
  font-weight: bold;
  color: #2c3e50;
  margin: 0 0 6px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.manga-original-title {
  margin: 0 0 14px;
  color: #6c757d;
  font-size: 15px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.manga-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  color: #6c757d;
  font-size: 14px;
}

.meta-separator {
  color: #adb5bd;
}

.info-row {
  display: flex;
  margin-bottom: 8px;
}

.label {
  font-weight: 600;
  color: #495057;
  margin-right: 12px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}

.tag {
  appearance: none;
  font: inherit;
  display: inline-flex;
  align-items: center;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  padding: 3px 6px;
  font-size: 13px;
  color: #343a40;
  transition: all 0.2s ease;
  cursor: pointer;
}

.tag:hover {
  background-color: #e9ecef;
  border-color: #dee2e6;
  transform: translateY(-1px);
}

.tag-count {
  margin-left: 6px;
  background-color: #6c757d;
  color: white;
  border-radius: 10px;
  padding: 2px 4px;
  font-size: 11px;
  font-weight: 500;
  min-width: 20px;
  text-align: center;
}

.action-buttons {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
  margin-top: 24px;
}

.btn-primary {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.btn-primary:hover {
  background-color: #337ab7;
}

.btn-primary:disabled {
  background-color: #b8c2cc;
  color: #f8f9fa;
  cursor: not-allowed;
  box-shadow: none;
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

.btn-icon.tag-management-active {
  background-color: #409eff;
  color: white;
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

.icon {
  font-size: 18px;
  line-height: 1;
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

.btn-icon:disabled,
.page-remove:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  transform: none;
  box-shadow: none;
}

.manga-description {
  margin-bottom: 40px;
}

.manga-description h3 {
  font-size: 20px;
  color: #2c3e50;
  margin-bottom: 15px;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.manga-description p {
  line-height: 1.6;
  color: #555;
  font-size: 16px;
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.manga-pages {
  margin-bottom: 40px;
}

.manga-pages h3 {
  font-size: 20px;
  color: #2c3e50;
  margin-bottom: 20px;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
  scroll-margin-top: 84px;
}

.pages-list {
  width: 100%;
  max-width: 1380px;
  display: grid;
  grid-template-columns: repeat(var(--column), minmax(0, var(--page-max-width)));
  gap: var(--gap);
  justify-content: center;
  list-style: none;
  padding: 0;
  margin: 0;
}

.page-item {
  position: relative;
  min-width: 0;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.page-item:hover {
  transform: translateY(-5px);
}

.page-item img {
  display: block;
  width: 100%;
  height: auto;
  aspect-ratio: 1 / 1.41;
  object-fit: contain;
  object-position: center;
  background-color: #f5f7fa;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.page-remove {
  appearance: none;
  position: absolute;
  top: 8px;
  right: 8px;
  background-color: rgba(255, 255, 255, 0.7);
  color: #f56565;
  padding: 2.5px 5px;
  border-radius: 4px;
  border: 1px solid #f56565;
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  line-height: 1;
  cursor: pointer;
  display: none;
}

.page-item:hover .page-remove {
  display: block;
}

@media (hover: none) and (pointer: coarse) {
  .page-remove {
    display: block;
  }
}

.page-number {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.manga-pages-sentinel {
  grid-column: 1 / -1;
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6c757d;
  font-size: 13px;
}

.related-manga h3 {
  font-size: 20px;
  color: #2c3e50;
  margin-bottom: 20px;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.related-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 20px;
}

.related-item {
  cursor: pointer;
  transition: transform 0.3s ease;
  text-align: center;
}

.related-item:hover {
  transform: translateY(-5px);
}

.related-item img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.related-title {
  margin-top: 10px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
  line-height: 1.3;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 响应式设计 */
@media (max-width: 1169px) {
  .manga-detail {
    --column: 5;
  }
}

@media (max-width: 979px) {
  .manga-detail {
    --column: 4;
  }
}

@media (max-width: 779px) {
  .manga-detail {
    --column: 3;
  }
}

@media (max-width: 579px) {
  .manga-detail {
    --column: 2;
  }
}

@media (max-width: 991px) and (min-width: 768px) {
  .manga-detail {
    --gap: 10px;
    --container-padding: 16px;
    margin: 74px auto 20px;
  }
}

@media screen and (max-width: 768px) {

  .manga-container {
    flex-direction: column;
    gap: 20px;
  }

  .manga-cover {
    width: 100%;
  }

  .manga-cover img {
    width: 100%;
    max-width: 350px;
    aspect-ratio: 1 / 1.41;
    height: auto;
    margin: 0 auto;
  }

  .manga-title {
    font-size: 24px;
  }

  .action-buttons {
    flex-wrap: wrap;
    gap: 10px;
  }

  .related-list {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 15px;
  }

  .related-item img {
    height: 160px;
  }
}

@media (max-width: 767px) {
  .manga-detail {
    --gap: 10px;
    --container-padding: 12px;
    margin: 64px auto 20px;
    padding-bottom: calc(76px + env(safe-area-inset-bottom, 0px));
  }

}

/* 章节列表样式 */
.manga-chapters {
  margin-bottom: 40px;
}

.manga-chapters h3 {
  font-size: 20px;
  color: #2c3e50;
  margin-bottom: 20px;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.empty-state {
  padding: 28px 16px;
  border: 1px dashed #ced4da;
  border-radius: 8px;
  color: #6c757d;
  text-align: center;
  background: #f8f9fa;
}

.chapters-simple-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.chapter-simple-item {
  appearance: none;
  font: inherit;
  padding: 8px 16px;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  background-color: #fff;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #333;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  text-align: left;
}

.chapter-page-count {
  color: #6c757d;
  font-size: 12px;
  font-weight: 400;
}

.chapter-simple-item:hover {
  border-color: #409eff;
  background-color: #f0f8ff;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.chapter-simple-item.active {
  background-color: #409eff;
  border-color: #409eff;
  color: white;
  font-weight: 600;
}

.chapter-simple-item.active .chapter-page-count {
  color: #eaf4ff;
}

.chapter-simple-item.active:hover {
  background-color: #337ab7;
  border-color: #337ab7;
  color: white;
}

/* 编辑按钮样式 */
.edit-tag-btn {
  background: none;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  padding: 4px 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.edit-tag-btn:hover {
  border-color: #409eff;
  background-color: #f0f8ff;
}

.edit-tag-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.edit-icon {
  width: 14px;
  height: 14px;
  fill: #6c757d;
}

.edit-tag-btn:hover .edit-icon {
  fill: #409eff;
}

.manga-detail button:focus-visible,
.manga-detail h3:focus-visible {
  outline: 3px solid rgba(64, 158, 255, 0.55);
  outline-offset: 3px;
}

</style>
