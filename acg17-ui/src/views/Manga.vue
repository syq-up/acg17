<template>
  <section>
    <div class="side-btn-group left-btn-group" :style="{ right: '50%', marginRight: (containerWidth / 2 + 30) + 'px' }">
      <div class="side-btn" @click="randomManga">
        <icon icon="#icon-random"></icon>
      </div>
      <div class="side-btn" :class="{ 'active': showTitleSearch || hasTitleFilter }" @click="toggleTitleSearch">
        <icon icon="#icon-search"></icon>
        <span v-if="hasTitleFilter" class="side-btn-status" aria-hidden="true"></span>
      </div>
      <div class="side-btn" :class="{ 'active': tag.showTagList || hasActiveTags }" @click="openTagList">
        <icon icon="#icon-tag"></icon>
        <span v-if="hasActiveTags" class="side-btn-badge">{{ selectedTagIds.length }}</span>
      </div>
      <div class="side-btn" v-show="showBackToTop" @click="scrollToTop">
        <icon icon="#icon-sort-asc"></icon>
      </div>
    </div>
    <div class="side-btn right-btn" v-show="showBackToTop" @click="scrollToTop" :style="{ left: '50%', marginLeft: (containerWidth / 2 + 30) + 'px' }">
      <icon icon="#icon-sort-asc"></icon>
    </div>

    <div
      class="filter-container"
      :class="{ 'is-visible': showFilterPanel }"
      @transitionend="handleFilterPanelTransitionEnd"
    >
      <div class="filter-collapse">
        <div class="filter-panel">
          <div class="filter-header">
            <div class="filter-title">{{ filterPanelTitle }}</div>
            <button v-if="hasActiveFilters" type="button" class="clear-filters-btn" @click="clearAllFilters">清除全部</button>
          </div>
          <div v-if="hasActiveFilters" class="selected-tags" :class="{ compact: !showTitleSearch && !tag.showTagList }">
            <div class="selected-tags-summary">
              <span class="selected-tags-label">已启用 {{ activeFilterCount }} 项筛选</span>
              <span class="selected-tags-result" role="status" aria-live="polite">{{ mangaResultText }}</span>
            </div>
            <div class="selected-tag-groups">
              <div v-if="hasTitleFilter" class="selected-tag-group">
                <span class="selected-tag-category">标题</span>
                <button
                  type="button"
                  class="selected-tag"
                  :aria-label="`清除标题搜索 ${activeTitle}`"
                  @click="clearTitleSearch"
                >
                  <span>{{ activeTitle }}</span>
                  <span class="selected-tag-remove" aria-hidden="true">×</span>
                </button>
              </div>
              <div v-for="group in selectedTagGroups" :key="'selected-group-' + group.key" class="selected-tag-group">
                <span class="selected-tag-category">{{ group.label }}</span>
                <button
                  v-for="selectedTag in group.tags"
                  :key="'selected-' + selectedTag.tagId"
                  type="button"
                  class="selected-tag"
                  :aria-label="`移除标签 ${selectedTag.tagName}`"
                  @click="toggleTag(selectedTag.tagId)"
                >
                  <span>{{ selectedTag.tagName }}</span>
                  <span class="selected-tag-remove" aria-hidden="true">×</span>
                </button>
              </div>
            </div>
          </div>

          <form v-show="showTitleSearch" class="title-search-body" role="search" @submit.prevent="commitTitleSearch">
            <div class="filter-search-box title-search-box">
              <icon icon="#icon-search" class="filter-search-icon"></icon>
              <input
                ref="titleSearchInput"
                v-model="titleSearchDraft"
                type="search"
                maxlength="255"
                placeholder="搜索漫画原文标题或中文标题"
                aria-label="搜索漫画标题"
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

          <div v-show="tag.showTagList" class="tag-body">
            <div class="tag-search-area">
              <div class="filter-search-box">
                <icon icon="#icon-search" class="filter-search-icon"></icon>
                <input
                  ref="tagSearchInput"
                  v-model="tagSearch"
                  type="search"
                  placeholder="输入标签名称进行查找"
                  aria-label="搜索标签"
                  autocomplete="off"
                  spellcheck="false"
                  :disabled="tag.loading"
                  @keydown="handleTagSearchKeydown"
                >
                <button v-if="tagSearch" type="button" class="clear-filter-search" aria-label="清空标签搜索" @click="clearTagSearch">
                  <icon icon="#icon-close"></icon>
                </button>
              </div>
              <div v-if="isTagSearching" class="tag-search-summary" role="status" aria-live="polite">
                <span>匹配 <strong>{{ matchingTagCount }}</strong> 个标签</span>
                <span v-if="matchingCategorySummary">· {{ matchingCategorySummary }}</span>
              </div>
            </div>
            <div class="tag-results" :class="{ searching: isTagSearching }">
              <div
                v-for="group in displayedTagGroups"
                :key="group.key"
                class="tag-group"
              >
                <div class="tag-group-heading">
                  <span class="tag-group-label">{{ group.label }}</span>
                  <span class="tag-group-count">{{ group.countText }}</span>
                </div>
                <div class="tags-container">
                  <button
                    v-for="item in group.tags"
                    :key="group.key + '-' + item.tagId"
                    type="button"
                    class="tag"
                    :class="{
                      active: isTagActive(item.tagId),
                      'keyboard-active': isTagSearching && item.searchIndex === activeTagSearchIndex
                    }"
                    :aria-pressed="isTagActive(item.tagId)"
                    @click="toggleTag(item.tagId)"
                    @mouseenter="setActiveTagSearchIndex(item.searchIndex)"
                  >
                    <template v-for="(part, partIndex) in item.nameParts" :key="partIndex">
                      <mark v-if="part.match">{{ part.text }}</mark>
                      <span v-else>{{ part.text }}</span>
                    </template>
                    <span class="tag-count">{{ item.tagCount }}</span>
                  </button>
                  <button v-if="group.canToggle" type="button" class="expand-tag-btn" @click="toggleExpand(group.key)">
                    {{ group.expanded ? '收起' : `展开其余 ${group.hiddenCount} 个` }}
                    <icon
                      icon="#icon-down"
                      class="expand-tag-arrow"
                      :class="{ expanded: group.expanded }"
                      aria-hidden="true"
                    ></icon>
                  </button>
                </div>
              </div>
              <div v-if="tag.loading && !hasDisplayedTags" class="empty-tags">标签加载中…</div>
              <div v-else-if="!hasDisplayedTags" class="empty-tags">
                {{ isTagSearching ? `没有找到包含“${tagSearch.trim()}”的标签` : '暂无可用标签' }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ul class="manga-container unselectable" v-infinite-scroll="loadMoreManga" :infinite-scroll-disabled="manga.disabled || !pageActive">
      <li v-for="(manga) in manga.list" :key="manga.id" @click="goToMangaDetail(manga.id)">
        <img class="manga-img" :src="withMediaStyle(manga.cover, 'small')" alt="manga">
        <div class="manga-info">
          <div class="manga-title">{{ manga.chineseTitle || manga.title }}</div>
        </div>
      </li>
    </ul>
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
import { reactive, onMounted, onUnmounted, onActivated, onDeactivated, watch, ref, nextTick, computed } from 'vue'
// import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router';
import { withMediaStyle } from '@/util/media'
import server from '@/util/request';

import LoadingHeart from "../components/LoadingHeart";
import Acg17Footer from "../components/Acg17Footer";
import { useRecycleState } from '@/composables/useRecycleState';

const TAG_CATEGORIES = [
  { key: 'group', field: 'groupTags', label: '团队', visibleLimit: 16 },
  { key: 'artist', field: 'artistTags', label: '艺术家', visibleLimit: 16 },
  { key: 'character', field: 'characterTags', label: '角色', visibleLimit: 16 },
  { key: 'male', field: 'maleTags', label: '男性', visibleLimit: 20 },
  { key: 'female', field: 'femaleTags', label: '女性', visibleLimit: 36 },
  { key: 'mixed', field: 'mixedTags', label: '混合', visibleLimit: 10 },
  { key: 'other', field: 'otherTags', label: '其他', visibleLimit: 10 },
  { key: 'original', field: 'originalTags', label: '原作', visibleLimit: 10 },
]

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

function normalizeTagName(value) {
  return String(value ?? '').trim().toLocaleLowerCase()
}

function getTagMatchRank(tagName, query) {
  const normalizedName = normalizeTagName(tagName)
  if (normalizedName === query) return 0
  if (normalizedName.startsWith(query)) return 1
  return 2
}

function createTagNameParts(tagName, query) {
  const name = String(tagName ?? '')
  if (!query) return [{ text: name, match: false }]
  const matchIndex = normalizeTagName(name).indexOf(query)
  if (matchIndex < 0) return [{ text: name, match: false }]
  return [
    { text: name.slice(0, matchIndex), match: false },
    { text: name.slice(matchIndex, matchIndex + query.length), match: true },
    { text: name.slice(matchIndex + query.length), match: false },
  ].filter(part => part.text)
}

export default {
  name: "Manga",
  components: {
    'acg17-loading-heart': LoadingHeart,
    'acg17-footer': Acg17Footer,
  },
  setup() {
    // const store = useStore()
    const router = useRouter()
    const route = useRoute()

    // 使用全局回收站状态管理
    const { isRecycle, toggleRecycle, setRecycle } = useRecycleState('manga')

    const manga = reactive({
      currentPage: 0, // 当前页
      list: [], // 漫画数据
      loading: false, // 加载下一页时显示loading
      disabled: false,  // 加载到最后一页时禁用加载
      total: 0, // 总记录数
    })

    function goToMangaDetail(id) {
      router.push(`/acg/manga/${id}`)
    }

    const containerWidth = ref(1380)
    const showBackToTop = ref(false)
    const filterPanelOpen = ref(false)
    const showTitleSearch = ref(false)
    const titleSearchInput = ref(null)
    const tagSearch = ref('')
    const tagSearchInput = ref(null)
    const activeTagSearchIndex = ref(-1)
    const activeTitle = computed(() => normalizeTitle(route.query.title))
    const titleSearchDraft = ref(activeTitle.value)
    const selectedTagIds = computed(() => parseTagIds(route.query.tagIds))
    const selectedTagKey = computed(() => selectedTagIds.value.join(','))
    const activeFilterKey = computed(() => JSON.stringify([activeTitle.value, selectedTagKey.value]))
    const normalizedTagSearch = computed(() => normalizeTagName(tagSearch.value))
    const isTagSearching = computed(() => normalizedTagSearch.value.length > 0)
    const hasTitleFilter = computed(() => activeTitle.value.length > 0)
    const hasActiveTags = computed(() => selectedTagIds.value.length > 0)
    const hasActiveFilters = computed(() => hasTitleFilter.value || hasActiveTags.value)
    const activeFilterCount = computed(() => selectedTagIds.value.length + (hasTitleFilter.value ? 1 : 0))
    const mangaResultText = computed(() => (
      manga.currentPage === 0 || (manga.loading && manga.currentPage === 1)
        ? '查询中'
        : `找到 ${manga.total} 部`
    ))
    const emptyMangaText = computed(() => {
      if (hasTitleFilter.value && hasActiveTags.value) {
        return `没有找到标题包含“${activeTitle.value}”且同时包含这些标签的漫画`
      }
      if (hasTitleFilter.value) return `没有找到标题包含“${activeTitle.value}”的漫画`
      if (hasActiveTags.value) return '没有同时包含这些标签的漫画'
      return '暂无漫画'
    })
    let resizeObserver = null
    const pageActive = ref(false)
    let loadedFilterKey = activeFilterKey.value
    let mangaRequestVersion = 0
    let tagListLoading = false
    let tagListLoaded = false
    let tagListRequestVersion = 0
    let pendingFilterClose = null
    let pendingFilterFocus = null

    const handleScroll = () => {
      showBackToTop.value = window.scrollY > 500
    }

    const updateWidth = () => {
      const container = document.querySelector('.manga-container')
      if (container) {
        containerWidth.value = container.clientWidth
      }
    }

    // 分页加载漫画，用于无限滚动
    function loadManga() {
      if (manga.loading || manga.disabled) return

      const requestVersion = mangaRequestVersion
      manga.loading = true
      manga.disabled = true

      // 构建查询参数
      const params = {
        pageNum: ++manga.currentPage,
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
          // records.length!==0：当前页非空页，可能存在下一页，对当前页数据进行下一步处理
          // records.length===0：当前页为空页，不存在下一页，置disabled=true，不再请求下一页
          if (response.data.records.length !== 0) {
            manga.list.push(...response.data.records)
            manga.disabled = false
          } else {
            manga.disabled = true
          }
        })
        .catch(err => {
          if (requestVersion !== mangaRequestVersion) return
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

    const tag = reactive({
      showTagList: false, // 是否显示标签列表
      groupTags: [], // 团队标签
      artistTags: [], // 艺术家标签
      characterTags: [], // 角色标签
      maleTags: [], // 男性标签
      femaleTags: [], // 女性标签
      mixedTags: [], // 混合标签
      otherTags: [], // 其他标签
      originalTags: [], // 原作标签
      loading: false,
      expandedCategories: Object.fromEntries(TAG_CATEGORIES.map(category => [category.key, false]))
    })

    const showFilterPanel = computed(() => (
      filterPanelOpen.value || hasActiveFilters.value
    ))
    const filterPanelTitle = computed(() => {
      if (showTitleSearch.value) return '标题搜索'
      if (tag.showTagList) return '标签筛选'
      return '漫画筛选'
    })

    const tagGroups = computed(() => TAG_CATEGORIES.map(category => ({
      ...category,
      tags: tag[category.field],
    })))

    const displayedTagGroups = computed(() => {
      const query = normalizedTagSearch.value
      let searchIndex = 0
      return tagGroups.value
        .map(group => {
          const sortedGroupTags = sortedTags(group.tags)
          const collapsedTags = getCollapsedTags(sortedGroupTags, group.visibleLimit)
          const expanded = !query && tag.expandedCategories[group.key]
          const sourceTags = query
            ? sortedGroupTags
              .filter(item => normalizeTagName(item.tagName).includes(query))
              .sort((a, b) => {
                const rankDifference = getTagMatchRank(a.tagName, query) - getTagMatchRank(b.tagName, query)
                if (rankDifference) return rankDifference
                const countDifference = Number(b.tagCount || 0) - Number(a.tagCount || 0)
                if (countDifference) return countDifference
                return String(a.tagName).localeCompare(String(b.tagName), 'zh-CN')
              })
            : expanded ? sortedGroupTags : collapsedTags
          const tags = sourceTags.map(item => ({
            ...item,
            nameParts: createTagNameParts(item.tagName, query),
            searchIndex: query ? searchIndex++ : -1,
          }))
          return {
            ...group,
            tags,
            expanded,
            canToggle: !query && collapsedTags.length < sortedGroupTags.length,
            hiddenCount: Math.max(0, sortedGroupTags.length - collapsedTags.length),
            countText: query ? `${tags.length} 个匹配` : `共 ${sortedGroupTags.length} 个`,
          }
        })
        .filter(group => group.tags.length || group.canToggle)
    })

    const tagSearchResults = computed(() => (
      isTagSearching.value
        ? displayedTagGroups.value.flatMap(group => group.tags)
        : []
    ))
    const matchingTagCount = computed(() => tagSearchResults.value.length)
    const matchingCategorySummary = computed(() => displayedTagGroups.value
      .filter(group => group.tags.length)
      .map(group => `${group.label} ${group.tags.length}`)
      .join(' · '))
    const hasDisplayedTags = computed(() => (
      displayedTagGroups.value.some(group => group.tags.length || group.canToggle)
    ))

    const selectedTagGroups = computed(() => {
      const selectedIds = new Set(selectedTagIds.value)
      const resolvedIds = new Set()
      const groups = []
      for (const group of tagGroups.value) {
        const tags = sortedTags(group.tags).filter(item => selectedIds.has(Number(item.tagId)))
        if (tags.length) {
          tags.forEach(item => resolvedIds.add(Number(item.tagId)))
          groups.push({ key: group.key, label: group.label, tags })
        }
      }
      const unresolvedTags = selectedTagIds.value
        .filter(tagId => !resolvedIds.has(tagId))
        .map(tagId => ({ tagId, tagName: `标签 #${tagId}` }))
      if (unresolvedTags.length) {
        groups.push({ key: 'unresolved', label: '标签', tags: unresolvedTags })
      }
      return groups
    })

    function finishPendingFilterClose() {
      if (pendingFilterClose === 'title') showTitleSearch.value = false
      if (pendingFilterClose === 'tag') tag.showTagList = false
      pendingFilterClose = null
    }

    function closeFilterPanel(editor) {
      filterPanelOpen.value = false
      pendingFilterClose = editor
      pendingFilterFocus = null
      if (hasActiveFilters.value || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        finishPendingFilterClose()
      }
    }

    function focusPendingFilterInput() {
      const input = pendingFilterFocus === 'title'
        ? titleSearchInput.value
        : pendingFilterFocus === 'tag' ? tagSearchInput.value : null
      if (!input || input.disabled) return
      input.focus({ preventScroll: true })
      pendingFilterFocus = null
    }

    function requestFilterInputFocus(editor, panelWasVisible) {
      pendingFilterFocus = editor
      if (panelWasVisible || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        nextTick(focusPendingFilterInput)
      }
    }

    function handleFilterPanelTransitionEnd(event) {
      if (event.target !== event.currentTarget || event.propertyName !== 'grid-template-rows') return
      if (!showFilterPanel.value) {
        finishPendingFilterClose()
      } else {
        nextTick(focusPendingFilterInput)
      }
    }

    function closeTitleSearch() {
      titleSearchDraft.value = activeTitle.value
      closeFilterPanel('title')
    }

    function toggleTitleSearch() {
      if (showTitleSearch.value && filterPanelOpen.value) {
        closeTitleSearch()
        return
      }
      const panelWasVisible = showFilterPanel.value
      pendingFilterClose = null
      filterPanelOpen.value = true
      showTitleSearch.value = true
      tag.showTagList = false
      titleSearchDraft.value = activeTitle.value
      scrollToTop()
      requestFilterInputFocus('title', panelWasVisible)
    }

    // 打开/关闭标签列表
    function openTagList() {
      if (tag.showTagList && filterPanelOpen.value) {
        closeFilterPanel('tag')
        scrollToTop()
        return
      }
      const panelWasVisible = showFilterPanel.value
      pendingFilterClose = null
      filterPanelOpen.value = true
      tag.showTagList = true
      showTitleSearch.value = false
      titleSearchDraft.value = activeTitle.value
      scrollToTop()
      loadTagList()
      requestFilterInputFocus('tag', panelWasVisible)
    }

    function loadTagList(force = false) {
      if (tagListLoading && !force) return
      const requestVersion = ++tagListRequestVersion
      tagListLoading = true
      tag.loading = true
      server.get('/manga-tag/list', {
        params: { deleted: isRecycle.value }
      })
        .then(res => {
          if (requestVersion !== tagListRequestVersion) return
          for (const category of TAG_CATEGORIES) {
            tag[category.field] = res.data[category.field] || []
          }
          tagListLoaded = true
        })
        .catch(err => {
          if (requestVersion !== tagListRequestVersion) return
          ElMessage.error('获取漫画标签失败【' + err + '】，请重试')
        })
        .finally(() => {
          if (requestVersion === tagListRequestVersion) {
            tagListLoading = false
            tag.loading = false
            if (pendingFilterFocus === 'tag') nextTick(focusPendingFilterInput)
          }
        })
    }

    function sortedTags(tags) {
      return tags.slice().sort((a, b) => {
        const countDifference = Number(b.tagCount || 0) - Number(a.tagCount || 0)
        if (countDifference) return countDifference
        return String(a.tagName).localeCompare(String(b.tagName), 'zh-CN')
      })
    }

    function getCollapsedTags(sortedTagList, visibleLimit) {
      const visibleTagIds = new Set(
        sortedTagList.slice(0, visibleLimit).map(item => Number(item.tagId))
      )
      selectedTagIds.value.forEach(tagId => visibleTagIds.add(tagId))
      return sortedTagList.filter(item => visibleTagIds.has(Number(item.tagId)))
    }

    function toggleExpand(category) {
      tag.expandedCategories[category] = !tag.expandedCategories[category]
    }

    function setActiveTagSearchIndex(index) {
      if (index >= 0) activeTagSearchIndex.value = index
    }

    function scrollActiveTagIntoView() {
      nextTick(() => {
        document.querySelector('.tag.keyboard-active')?.scrollIntoView({ block: 'nearest' })
      })
    }

    function moveActiveTagSearchIndex(step) {
      const resultCount = tagSearchResults.value.length
      if (!resultCount) return
      activeTagSearchIndex.value = (
        activeTagSearchIndex.value + step + resultCount
      ) % resultCount
      scrollActiveTagIntoView()
    }

    function clearTagSearch() {
      tagSearch.value = ''
      activeTagSearchIndex.value = -1
      nextTick(() => tagSearchInput.value?.focus({ preventScroll: true }))
    }

    function handleTagSearchKeydown(event) {
      if (event.key === 'Escape' && tagSearch.value) {
        event.preventDefault()
        clearTagSearch()
        return
      }
      if (!isTagSearching.value) return
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault()
        moveActiveTagSearchIndex(event.key === 'ArrowDown' ? 1 : -1)
        return
      }
      if (event.key === 'Enter' && activeTagSearchIndex.value >= 0) {
        const activeTag = tagSearchResults.value[activeTagSearchIndex.value]
        if (activeTag) {
          event.preventDefault()
          toggleTag(activeTag.tagId)
        }
      }
    }

    function isTagActive(tagId) {
      return selectedTagIds.value.includes(Number(tagId))
    }

    function updateFilters({ title = activeTitle.value, tagIds = selectedTagIds.value } = {}) {
      const normalizedTitle = normalizeTitle(title)
      const normalizedTagIds = [...new Set(tagIds)].sort((a, b) => a - b)
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

    function commitTitleSearch() {
      const title = normalizeTitle(titleSearchDraft.value)
      titleSearchDraft.value = title
      updateFilters({ title })
    }

    function clearTitleSearch() {
      titleSearchDraft.value = ''
      updateFilters({ title: '' })
      if (showTitleSearch.value) {
        nextTick(() => titleSearchInput.value?.focus({ preventScroll: true }))
      }
    }

    function updateTagFilter(tagIds) {
      updateFilters({ tagIds })
    }

    function toggleTag(tagId) {
      const normalizedTagId = Number(tagId)
      const nextTagIds = new Set(selectedTagIds.value)
      if (nextTagIds.has(normalizedTagId)) {
        nextTagIds.delete(normalizedTagId)
      } else {
        nextTagIds.add(normalizedTagId)
      }
      updateTagFilter([...nextTagIds])
    }

    function clearTagFilter() {
      updateTagFilter([])
    }

    function clearAllFilters() {
      titleSearchDraft.value = ''
      updateFilters({ title: '', tagIds: [] })
    }

    function scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: "smooth"
      })
    }

    function resetMangaList() {
      mangaRequestVersion += 1
      loadedFilterKey = activeFilterKey.value
      manga.list = []
      manga.currentPage = 0
      manga.total = 0
      manga.loading = false
      manga.disabled = false
      loadManga()
    }

    // 监听回收站状态变化，重新获取数据
    watch(isRecycle, () => {
      resetMangaList()
      if (tag.showTagList || hasActiveTags.value) loadTagList(true)
    })

    // 监听标题和标签筛选变化，重新获取数据
    watch(activeFilterKey, currentFilterKey => {
      if (route.name === 'Manga' && currentFilterKey !== loadedFilterKey) {
        resetMangaList()
      }
    })

    watch(activeTitle, title => {
      titleSearchDraft.value = title
    })

    watch(hasActiveFilters, active => {
      if (active && pendingFilterClose) finishPendingFilterClose()
    })

    watch(normalizedTagSearch, query => {
      activeTagSearchIndex.value = query && matchingTagCount.value ? 0 : -1
    })

    watch(matchingTagCount, resultCount => {
      if (!resultCount) {
        activeTagSearchIndex.value = -1
      } else if (isTagSearching.value && (activeTagSearchIndex.value < 0 || activeTagSearchIndex.value >= resultCount)) {
        activeTagSearchIndex.value = 0
      }
    })

    function activatePageListeners() {
      pageActive.value = true
      window.addEventListener('scroll', handleScroll)
      handleScroll()
      nextTick(() => {
        if (!pageActive.value) return
        updateWidth()
        window.addEventListener('resize', updateWidth)
        if (!resizeObserver) {
          resizeObserver = new ResizeObserver(() => updateWidth())
        }
        const container = document.querySelector('.manga-container')
        if (container) resizeObserver.observe(container)
      })
    }

    function deactivatePageListeners() {
      pageActive.value = false
      window.removeEventListener('scroll', handleScroll)
      window.removeEventListener('resize', updateWidth)
      if (resizeObserver) resizeObserver.disconnect()
    }

    // 组件挂载时获取数据
    onMounted(() => {
      loadManga()
      if (hasActiveTags.value) loadTagList()
    })

    onActivated(() => {
      if (activeFilterKey.value !== loadedFilterKey) {
        resetMangaList()
      }
      if (hasActiveTags.value && !tagListLoaded) loadTagList()
      activatePageListeners()
    })

    onDeactivated(deactivatePageListeners)
    onUnmounted(deactivatePageListeners)

    return {
      manga,
      withMediaStyle,
      mangaResultText,
      emptyMangaText,
      goToMangaDetail,
      isRecycle,
      toggleRecycle,
      setRecycle,
      loadManga,
      loadMoreManga,
      pageActive,
      randomManga,
      scrollToTop,
      showBackToTop,
      containerWidth,
      showTitleSearch,
      showFilterPanel,
      filterPanelTitle,
      titleSearchInput,
      titleSearchDraft,
      activeTitle,
      hasTitleFilter,
      hasActiveFilters,
      activeFilterCount,
      toggleTitleSearch,
      closeTitleSearch,
      handleFilterPanelTransitionEnd,
      commitTitleSearch,
      clearTitleSearch,
      clearAllFilters,
      tag,
      tagSearch,
      tagSearchInput,
      displayedTagGroups,
      matchingTagCount,
      matchingCategorySummary,
      hasDisplayedTags,
      isTagSearching,
      activeTagSearchIndex,
      openTagList,
      selectedTagIds,
      selectedTagGroups,
      isTagActive,
      toggleTag,
      hasActiveTags,
      clearTagFilter,
      clearTagSearch,
      handleTagSearchKeydown,
      setActiveTagSearchIndex,
      toggleExpand,
    }
  }
}
</script>

<style scoped>
/* 响应式CSS变量 */
* {
  --column: 6;
  --width: 220px;
  --height: calc(var(--width) * 1.41 + var(--title-height));
  --gap: 12px;
  --container-padding: 20px;
  --title-height: 50px;
}

.page-header ::v-deep(.navi-menu) {
  max-width: 100%;
  width: auto;
}

section {
  margin: 84px auto 20px;
  max-width: 100%;
  padding: 0 var(--container-padding);
  box-sizing: border-box;
  /* 屏幕高度 - 自身上下外边距高度 - 页脚高度 */
  min-height: calc(100vh - 104px - 200px);
}

.filter-container {
  display: grid;
  grid-template-rows: 0fr;
  max-width: 1380px;
  margin: 0 auto;
  opacity: 0;
  overflow: hidden;
  transition:
    grid-template-rows 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    margin-bottom 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.2s ease;
  pointer-events: none;
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
  background: #ffffff;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  padding: 16px 18px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.filter-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.clear-filters-btn {
  border: 1px solid #e9ecef;
  background: #ffffff;
  color: #409eff;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
}

.clear-filters-btn:hover {
  border-color: #409eff;
  background: #f0f8ff;
}

.selected-tags {
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #f5f9ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;
}

.selected-tags.compact {
  flex-direction: row;
  align-items: center;
  margin-bottom: 0;
  overflow: hidden;
}

.selected-tags-summary {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 7px;
  min-width: 0;
}

.selected-tags-label,
.selected-tags-result {
  flex: 0 0 auto;
  color: #606266;
  font-size: 13px;
}

.selected-tags-label {
  font-weight: 600;
}

.selected-tags-result::before {
  content: "·";
  margin-right: 7px;
  color: #a8abb2;
}

.selected-tag-groups {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  min-width: 0;
  max-height: 112px;
  overflow-y: auto;
  scrollbar-width: thin;
}

.selected-tags.compact .selected-tag-groups {
  flex-wrap: nowrap;
  flex: 1;
  max-height: none;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: thin;
}

.selected-tag-group {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 6px;
}

.selected-tag-category {
  padding: 3px 6px;
  border-radius: 4px;
  background: #e3f2fd;
  color: #607d8b;
  font-size: 11px;
  font-weight: 600;
}

.selected-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 7px;
  border: 1px solid #90caf9;
  border-radius: 999px;
  background: #ffffff;
  color: #1976d2;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.selected-tag:hover {
  border-color: #f89898;
  background: #fef0f0;
  color: #f56c6c;
}

.selected-tag-remove {
  font-size: 16px;
  line-height: 12px;
}

.title-search-body {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-search-box {
  flex: 1;
  min-width: 0;
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

.tag-search-area {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.filter-search-box {
  position: relative;
  display: flex;
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

.filter-search-box input:disabled {
  background: #f5f7fa;
  cursor: wait;
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

.tag-search-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  color: #606266;
  font-size: 12px;
}

.tag-search-summary strong {
  color: #1976d2;
}

.tag-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tag-results {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: start;
  max-height: 600px;
  padding-right: 5px;
  overflow-y: auto;
  scrollbar-width: thin;
}

.tag-results.searching {
  display: flex;
  flex-direction: column;
  max-height: 460px;
}

.tag-group {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  align-items: start;
  min-width: 0;
  padding: 10px 0;
}

.tag-group-heading {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  padding-top: 3px;
}

.tag-group-label {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.tag-group-count {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}

.tag {
  display: inline-flex;
  align-items: center;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  padding: 4px 6px;
  font-size: 13px;
  color: #343a40;
  transition: all 0.2s ease;
  cursor: pointer;
  font-family: inherit;
}

.tag:hover {
  background-color: #e9ecef;
  border-color: #dee2e6;
  transform: translateY(-1px);
}

.tag.active {
  background-color: #e3f2fd;
  border-color: #90caf9;
  color: #1976d2;
}

.tag.keyboard-active {
  border-color: #409eff;
  outline: 2px solid rgba(64, 158, 255, 0.22);
  outline-offset: 1px;
}

.tag mark {
  padding: 0;
  background: #fff1a8;
  color: inherit;
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

.tag.active .tag-count {
  background-color: #1976d2;
}

.expand-tag-btn {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 4px;
  padding: 4px 6px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #409eff;
  cursor: pointer;
  transition: background-color 0.2s ease;
  font-size: 12px;
  line-height: 1.2;
  white-space: nowrap;
}

.expand-tag-btn:hover {
  background: #ecf5ff;
}

.expand-tag-arrow {
  width: 10px;
  height: 10px;
  flex: 0 0 auto;
  fill: currentColor;
  transform-origin: center;
  transition: transform 0.2s ease;
}

.expand-tag-arrow.expanded {
  transform: rotate(180deg);
}

.empty-tags {
  color: #909399;
  font-size: 13px;
  padding: 6px 0;
}

.left-btn-group {
  position: fixed;
  top: 84px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

ul {
  width: 100%;
  max-width: 1380px;
  padding: 0;
  margin: 0 auto;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(var(--column), 1fr);
  grid-auto-rows: var(--height);
  gap: var(--gap);
  justify-content: center;
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

.side-btn:hover {
  background-color: #f2f6fc;
  color: #409eff;
}
.side-btn.active {
  background-color: #d9ecff;
  color: #409eff;
}

.side-btn svg, .side-btn .icon {
  width: 24px;
  height: 24px;
  fill: currentColor;
}

.left-btn {
  position: fixed;
  top: 84px;
}

.right-btn {
  position: fixed;
  bottom: 50px;
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

ul li .manga-img {
  box-sizing: border-box;
  width: 100%;
  height: calc(var(--width) * 1.41);
  border-radius: 10px 10px 0 0;
  object-fit: contain;
  object-position: center;
  display: block;
}

ul li .manga-info {
  padding: 8px 12px;
  background-color: #ffffff;
  border-radius: 0 0 10px 10px;
  overflow: hidden;
  transition: all 0.3s ease;
  height: var(--title-height);
  box-sizing: border-box;
}

ul li:hover .manga-info {
  background-color: #f8f9fa;
  overflow: visible;
  height: auto;
  min-height: var(--title-height);
  padding: 12px;
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

ul li:hover .manga-title {
  line-clamp: unset;
  -webkit-line-clamp: unset;
  overflow: visible;
}

/* 超大屏幕 (≥1400px) - 6列 */
@media (min-width: 1400px) {
  * {
    --column: 6;
    --width: 220px;
    --gap: 12px;
    --container-padding: 20px;
  }
}

/* 大屏幕 (1200px - 1399px) - 5列 */
@media (max-width: 1399px) and (min-width: 1200px) {
  * {
    --column: 5;
    --width: 200px;
    --gap: 12px;
    --container-padding: 20px;
  }
}

/* 桌面 (992px - 1199px) - 4列 */
@media (max-width: 1199px) and (min-width: 992px) {
  * {
    --column: 4;
    --width: 180px;
    --gap: 10px;
    --container-padding: 20px;
  }
}

/* 平板横屏 (768px - 991px) - 3列 */
@media (max-width: 991px) and (min-width: 768px) {
  * {
    --column: 3;
    --width: 160px;
    --gap: 10px;
    --container-padding: 16px;
  }

  section {
    margin: 74px auto 20px;
  }

  .filter-container {
    margin: 12px auto;
  }

  .manga-title {
    font-size: 13px;
    line-height: 18px;
  }
}

/* 小屏幕设备 (≤767px) - 2列 */
@media (max-width: 767px) {
  * {
    --column: 2;
    --width: 140px;
    --gap: 10px;
    --container-padding: 12px;
  }

  section {
    margin: 64px auto 20px;
  }

  .filter-panel {
    padding: 12px;
  }

  .filter-header {
    gap: 8px;
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

  .selected-tags {
    gap: 7px;
  }

  .selected-tags.compact {
    flex-direction: column;
    align-items: stretch;
  }

  .selected-tags-summary {
    flex-wrap: wrap;
  }

  .selected-tag-groups {
    gap: 7px 10px;
  }

  .tag-results.searching {
    max-height: 360px;
  }

  .tag-group {
    grid-template-columns: 54px minmax(0, 1fr);
    gap: 8px;
    padding: 9px;
  }

  .tag-group-label {
    font-size: 13px;
  }

  .tag {
    font-size: 12px;
  }

  .manga-title {
    font-size: 12px;
    line-height: 16px;
  }

  ul li .manga-info {
    padding: 6px 8px;
    height: 40px;
  }

  ul li:hover .manga-info {
    padding: 8px;
  }

  ul li:hover {
    transform: translateY(-4px) scale(1.01);
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  ul li:hover {
    transform: none;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    height: var(--height);
  }

  ul li:hover .manga-info {
    background-color: #ffffff;
    overflow: hidden;
    height: 50px;
  }

  ul li:hover .manga-title {
    line-clamp: 2;
    -webkit-line-clamp: 2;
    overflow: hidden;
  }

  ul li:active {
    transform: scale(0.98);
    transition: transform 0.1s ease;
  }
}

/* 高分辨率屏幕优化 */
@media (-webkit-min-device-pixel-ratio: 2),
(min-resolution: 192dpi) {
  ul li {
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  }

  ul li:hover {
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
  }
}

@media (prefers-reduced-motion: reduce) {
  .filter-container {
    transition: none;
  }
}

/* 打印样式 */
@media print {
  section {
    margin: 0;
    padding: 0;
  }

  ul li {
    break-inside: avoid;
    box-shadow: none;
    border: 1px solid #ddd;
  }

  ul li:hover {
    transform: none;
    box-shadow: none;
  }
}
</style>
