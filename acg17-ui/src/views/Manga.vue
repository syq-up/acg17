<template>
  <section>
    <div class="side-btn-group left-btn-group">
      <button type="button" class="side-btn" aria-label="随机漫画" title="随机漫画" @click="randomManga">
        <icon icon="#icon-random"></icon>
      </button>
      <button
        type="button"
        class="side-btn"
        :class="{ 'active': filterEditor === 'title' || hasTitleFilter }"
        aria-label="标题搜索"
        title="标题搜索"
        @click="toggleFilterEditor('title')"
      >
        <icon icon="#icon-search"></icon>
        <span v-if="hasTitleFilter" class="side-btn-status" aria-hidden="true"></span>
      </button>
      <button
        type="button"
        class="side-btn"
        :class="{ 'active': filterEditor === 'tags' || hasActiveTags }"
        aria-label="标签筛选"
        title="标签筛选"
        @click="toggleFilterEditor('tags')"
      >
        <icon icon="#icon-tag"></icon>
        <span v-if="hasActiveTags" class="side-btn-badge">{{ selectedTagIds.length }}</span>
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

    <manga-filter-panel
      v-model:editor="filterEditor"
      :active-title="activeTitle"
      :selected-tag-ids="selectedTagIds"
      :result-total="manga.total"
      :result-pending="manga.loading && manga.currentPage === 0"
      :is-recycle="isRecycle"
      @filters-change="updateFilters"
    />

    <ul
      class="manga-container unselectable"
      v-infinite-scroll="loadMoreManga"
      :infinite-scroll-disabled="manga.loading || manga.error || manga.disabled || !pageActive"
    >
      <li v-for="(manga) in manga.list" :key="manga.id">
        <router-link :to="{ name: 'MangaDetail', params: { id: manga.id } }" class="manga-card">
          <div class="manga-cover">
            <img
              v-if="manga.cover && !coverErrorIds.has(manga.id)"
              class="manga-img"
              :src="withMediaStyle(manga.cover, 'small')"
              :alt="(manga.chineseTitle || manga.title || '漫画') + '封面'"
              loading="lazy"
              decoding="async"
              @error="handleCoverError(manga.id)"
            >
            <div
              v-else
              class="manga-cover-placeholder"
              role="img"
              :aria-label="(manga.chineseTitle || manga.title || '漫画') + '封面'"
            >
              暂无封面
            </div>
            <span v-if="manga.favorite" class="manga-favorite" aria-label="已收藏" title="已收藏">★</span>
          </div>
          <div class="manga-info">
            <div class="manga-title" :title="manga.chineseTitle || manga.title">{{ manga.chineseTitle || manga.title }}</div>
            <div class="manga-title manga-title-expanded" aria-hidden="true">{{ manga.chineseTitle || manga.title }}</div>
          </div>
        </router-link>
      </li>
    </ul>
    <div v-if="manga.error" class="manga-load-error" role="alert">
      <span>加载失败，请重试</span>
      <button type="button" @click="retryLoadManga">重试</button>
    </div>
    <div v-if="!manga.loading && manga.disabled && manga.list.length === 0" class="empty-manga">
      <span>{{ emptyMangaText }}</span>
      <div v-if="hasActiveFilters" class="empty-manga-actions">
        <button v-if="hasTitleFilter" type="button" @click="clearTitleSearch">清除标题</button>
        <button v-if="hasActiveTags" type="button" @click="clearTagFilter">清除标签</button>
      </div>
    </div>
    <acg17-loading-heart v-show="manga.loading"></acg17-loading-heart>
  </section>
  <acg17-footer v-if="manga.disabled"></acg17-footer>
</template>

<script>
import { reactive, onMounted, onUnmounted, onActivated, onDeactivated, watch, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router';
import { withMediaStyle } from '@/util/media'
import server from '@/util/request';

import LoadingHeart from "../components/LoadingHeart";
import Acg17Footer from "../components/Acg17Footer";
import MangaFilterPanel from '@/components/manga/MangaFilterPanel.vue'
import { useBackToTop } from '@/composables/useBackToTop'
import { useRecycleState } from '@/composables/useRecycleState';

function parseTagIds(queryValue) {
  const values = Array.isArray(queryValue) ? queryValue : [queryValue]
  return [...new Set(values
    .flatMap(value => String(value ?? '').split(','))
    .map(value => Number(value))
    .filter(value => Number.isInteger(value) && value > 0))]
    .sort((a, b) => a - b)
}

function normalizeTitle(value) {
  const queryValue = Array.isArray(value) ? value[0] : value
  return String(queryValue ?? '').trim()
}

export default {
  name: "Manga",
  components: {
    'acg17-loading-heart': LoadingHeart,
    'acg17-footer': Acg17Footer,
    MangaFilterPanel,
  },
  setup() {
    const router = useRouter()
    const route = useRoute()

    // 使用全局回收站状态管理
    const { isRecycle } = useRecycleState('manga')

    const manga = reactive({
      currentPage: 0, // 当前页
      list: [], // 漫画数据
      loading: false, // 加载下一页时显示loading
      error: false, // 加载失败
      disabled: false,  // 加载到最后一页时禁用加载
      total: 0, // 总记录数
    })

    function goToMangaDetail(id) {
      router.push(`/acg/manga/${id}`)
    }

    const { showBackToTop, scrollToTop } = useBackToTop()
    const filterEditor = ref('')
    const activeTitle = computed(() => normalizeTitle(route.query.title))
    const selectedTagIds = computed(() => parseTagIds(route.query.tagIds))
    const selectedTagKey = computed(() => selectedTagIds.value.join(','))
    const activeFilterKey = computed(() => JSON.stringify([activeTitle.value, selectedTagKey.value]))
    const hasTitleFilter = computed(() => activeTitle.value.length > 0)
    const hasActiveTags = computed(() => selectedTagIds.value.length > 0)
    const hasActiveFilters = computed(() => hasTitleFilter.value || hasActiveTags.value)
    const emptyMangaText = computed(() => {
      if (hasTitleFilter.value && hasActiveTags.value) {
        return `没有找到标题包含“${activeTitle.value}”且同时包含这些标签的漫画`
      }
      if (hasTitleFilter.value) return `没有找到标题包含“${activeTitle.value}”的漫画`
      if (hasActiveTags.value) return '没有同时包含这些标签的漫画'
      return '暂无漫画'
    })
    const coverErrorIds = reactive(new Set())
    const pageActive = ref(false)
    let loadedFilterKey = activeFilterKey.value
    let mangaRequestVersion = 0

    // 分页加载漫画，用于无限滚动
    function loadManga() {
      if (manga.loading || manga.error || manga.disabled) return

      const requestVersion = mangaRequestVersion
      const page = manga.currentPage + 1
      manga.loading = true
      manga.error = false

      // 构建查询参数
      const params = {
        pageNum: page,
        deleted: isRecycle.value
      }

      if (hasTitleFilter.value) {
        params.title = activeTitle.value
      }

      if (selectedTagIds.value.length) {
        params.tagIds = selectedTagIds.value.join(',')
      }

      server.get('/manga/list', {
        params: params
      })
        .then(response => {
          if (requestVersion !== mangaRequestVersion) return
          manga.total = Number(response.data.total) || 0
          const records = Array.isArray(response.data.records) ? response.data.records : []
          manga.list.push(...records)
          manga.currentPage = page
          manga.disabled = records.length === 0 || manga.list.length >= manga.total
        })
        .catch(err => {
          if (requestVersion !== mangaRequestVersion) return
          manga.error = true
          console.log(err)
        })
        .finally(() => {
          if (requestVersion === mangaRequestVersion) manga.loading = false
        })
    }

    function loadMoreManga() {
      if (!pageActive.value) return
      loadManga()
    }

    function retryLoadManga() {
      if (manga.loading) return
      manga.error = false
      loadManga()
    }

    function handleCoverError(id) {
      coverErrorIds.add(id)
    }

    // 随机打开一个漫画
    function randomManga() {
      server.get('/manga/random')
        .then(response => {
          if (response.data) {
            goToMangaDetail(response.data.id)
          }
        })
        .catch(err => {
          console.log(err)
        })
    }

    function toggleFilterEditor(editor) {
      filterEditor.value = filterEditor.value === editor ? '' : editor
      if (filterEditor.value) scrollToTop()
    }

    function updateFilters({ title = activeTitle.value, tagIds = selectedTagIds.value } = {}) {
      const normalizedTitle = normalizeTitle(title)
      const normalizedTagIds = parseTagIds(tagIds)
      const nextFilterKey = JSON.stringify([normalizedTitle, normalizedTagIds.join(',')])
      if (nextFilterKey === activeFilterKey.value) return

      const query = {}
      if (normalizedTitle) query.title = normalizedTitle
      if (normalizedTagIds.length) query.tagIds = normalizedTagIds.join(',')

      router.push({
        path: '/acg/manga',
        query
      })
    }

    function clearTitleSearch() {
      updateFilters({ title: '' })
    }

    function clearTagFilter() {
      updateFilters({ tagIds: [] })
    }

    function resetMangaList() {
      mangaRequestVersion += 1
      loadedFilterKey = activeFilterKey.value
      manga.list = []
      manga.currentPage = 0
      manga.total = 0
      manga.loading = false
      manga.error = false
      manga.disabled = false
      coverErrorIds.clear()
      loadManga()
    }

    // 监听回收站状态变化，重新获取数据
    watch(isRecycle, () => {
      resetMangaList()
    })

    // 监听标题和标签筛选变化，重新获取数据
    watch(activeFilterKey, currentFilterKey => {
      if (route.name === 'Manga' && currentFilterKey !== loadedFilterKey) {
        resetMangaList()
      }
    })

    function activatePage() {
      pageActive.value = true
    }

    function deactivatePage() {
      pageActive.value = false
    }

    // 组件挂载时获取数据
    onMounted(() => {
      loadManga()
    })

    onActivated(() => {
      if (activeFilterKey.value !== loadedFilterKey) {
        resetMangaList()
      }
      activatePage()
    })

    onDeactivated(deactivatePage)
    onUnmounted(deactivatePage)

    return {
      manga,
      withMediaStyle,
      emptyMangaText,
      coverErrorIds,
      goToMangaDetail,
      isRecycle,
      retryLoadManga,
      handleCoverError,
      loadMoreManga,
      pageActive,
      randomManga,
      scrollToTop,
      showBackToTop,
      filterEditor,
      toggleFilterEditor,
      activeTitle,
      hasTitleFilter,
      hasActiveFilters,
      selectedTagIds,
      hasActiveTags,
      updateFilters,
      clearTitleSearch,
      clearTagFilter,
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
  --title-height: 50px;
  margin: 84px auto 20px;
  max-width: 100%;
  padding: 0 var(--container-padding);
  box-sizing: border-box;
  /* 屏幕高度 - 自身上下外边距高度 - 页脚高度 */
  min-height: calc(100vh - 104px - 200px);
}

.manga-container {
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

.side-btn-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  box-sizing: border-box;
  border: 2px solid #ffffff;
  border-radius: 10px;
  background: #f56c6c;
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
}

.side-btn.active {
  background-color: #d9ecff;
  color: #409eff;
}

.empty-manga {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  min-height: 240px;
  color: #909399;
  font-size: 14px;
}

.empty-manga-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.empty-manga button {
  padding: 7px 14px;
  border: 1px solid #b3d8ff;
  border-radius: 6px;
  background: #ffffff;
  color: #409eff;
  font: inherit;
  cursor: pointer;
}

.empty-manga button:hover {
  background: #ecf5ff;
}

.manga-load-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin: 20px auto;
  color: #f56c6c;
  font-size: 14px;
}

.manga-load-error button {
  padding: 6px 14px;
  border: 1px solid #f5c6cb;
  border-radius: 6px;
  background: #ffffff;
  color: #c0392b;
  font: inherit;
  cursor: pointer;
}

.manga-container > li {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  min-width: 0;
  width: 100%;
  height: auto;
  overflow: visible;
  border-radius: 10px;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  z-index: 1;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.manga-card {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  width: 100%;
  height: 100%;
  color: inherit;
  text-decoration: none;
  border-radius: 10px;
}

.manga-card:focus-visible {
  outline: 3px solid #409eff;
  outline-offset: 3px;
}

.manga-container > li .manga-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1.41;
}

.manga-container > li:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.25);
  z-index: 10;
}

.manga-container > li .manga-img {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  aspect-ratio: 1 / 1.41;
  border-radius: 10px 10px 0 0;
  background-color: #f5f7fa;
  object-fit: contain;
  object-position: center;
  display: block;
}

.manga-cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  border-radius: 10px 10px 0 0;
  background-color: #f5f7fa;
  color: #909399;
  font-size: 13px;
}

.manga-favorite {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
  color: #f5b301;
  font-size: 20px;
  line-height: 1;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.35);
  pointer-events: none;
}

.manga-container > li .manga-info {
  position: relative;
  flex: 0 0 var(--title-height);
  display: flex;
  align-items: center;
  padding: 5px 12px;
  background-color: #ffffff;
  border-radius: 0 0 10px 10px;
  overflow: hidden;
  transition: all 0.3s ease;
  height: var(--title-height);
  box-sizing: border-box;
}

.manga-container > li:hover .manga-info {
  background-color: #f8f9fa;
}

.manga-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  line-height: 20px;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.3s ease;
  word-break: break-word;
}

.manga-title.manga-title-expanded {
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

@media (hover: hover) and (pointer: fine) {
  .manga-container > li:hover .manga-info {
    overflow: visible;
  }

  .manga-container > li:hover .manga-title-expanded {
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

  .manga-title {
    font-size: 13px;
    line-height: 18px;
  }
}

/* 小屏幕设备 */
@media (max-width: 767px) {
  section {
    --gap: 10px;
    --container-padding: 12px;
    --title-height: 40px;
    margin: 64px auto 20px;
    padding-bottom: calc(76px + env(safe-area-inset-bottom, 0px));
  }

  .manga-title {
    font-size: 12px;
    line-height: 16px;
  }

  .manga-container > li .manga-info {
    padding: 4px 8px;
  }

  .manga-container > li:hover {
    transform: translateY(-4px) scale(1.01);
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .manga-container > li:hover {
    transform: none;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .manga-container > li:hover .manga-info {
    background-color: #ffffff;
  }

  .manga-container > li:active {
    transform: scale(0.98);
    transition: transform 0.1s ease;
  }
}

/* 高分辨率屏幕优化 */
@media (-webkit-min-device-pixel-ratio: 2),
(min-resolution: 192dpi) {
  .manga-container > li {
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  }

  .manga-container > li:hover {
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
  }
}

/* 打印样式 */
@media print {
  section {
    margin: 0;
    padding: 0;
  }

  .manga-container > li {
    break-inside: avoid;
    box-shadow: none;
    border: 1px solid #ddd;
  }

  .manga-container > li:hover {
    transform: none;
    box-shadow: none;
  }
}
</style>
