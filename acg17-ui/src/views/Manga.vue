<template>
  <section>
    <div class="side-btn-group left-btn-group" :style="{ right: '50%', marginRight: (containerWidth / 2 + 30) + 'px' }">
      <div class="side-btn" @click="randomManga">
        <icon icon="#icon-random"></icon>
      </div>
      <div class="side-btn" :class="{ 'active': tag.showTagList }" @click="openTagList">
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

    <div class="tag-container" :class="{ 'is-visible': tag.showTagList || hasActiveTags }">
      <div class="tag-panel">
        <div class="tag-header">
          <div class="tag-title">标签筛选</div>
          <button v-if="hasActiveTags" type="button" class="clear-tags-btn" @click="clearTagFilter">清除筛选</button>
        </div>
        <div v-if="hasActiveTags" class="selected-tags" :class="{ compact: !tag.showTagList }">
          <span class="selected-tags-label">已选 {{ selectedTagIds.length }} · {{ mangaResultText }}</span>
          <div class="selected-tag-list">
            <button
              v-for="selectedTag in selectedTags"
              :key="'selected-' + selectedTag.tagId"
              type="button"
              class="selected-tag"
              :aria-label="`移除标签 ${selectedTag.tagName}`"
              @click="toggleTag(selectedTag.tagId)"
            >
              <span v-if="selectedTag.categoryLabel" class="selected-tag-category">{{ selectedTag.categoryLabel }}</span>
              <span>{{ selectedTag.tagName }}</span>
              <span class="selected-tag-remove" aria-hidden="true">×</span>
            </button>
          </div>
        </div>
        <div v-show="tag.showTagList" class="tag-body">
          <div class="tag-search-area">
            <div class="tag-search-box">
              <icon icon="#icon-search" class="tag-search-icon"></icon>
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
              <button v-if="tagSearch" type="button" class="clear-tag-search" aria-label="清空标签搜索" @click="clearTagSearch">
                <icon icon="#icon-close"></icon>
              </button>
            </div>
            <div v-if="isTagSearching" class="tag-search-summary" role="status" aria-live="polite">
              <span>匹配 <strong>{{ matchingTagCount }}</strong> 个标签</span>
              <span v-if="matchingCategorySummary">· {{ matchingCategorySummary }}</span>
            </div>
          </div>
          <div class="tag-results" :class="{ searching: isTagSearching }">
            <div v-for="group in displayedTagGroups" :key="group.key" class="info-row">
              <span class="label">{{ group.label }}:</span>
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
                <button v-if="group.hasHidden" type="button" class="expand-tag-btn" @click="toggleExpand(group.key)">
                  {{ tag.expand[group.key] ? '收起' : '展开' }}
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

    <ul class="manga-container unselectable" v-infinite-scroll="loadManga" :infinite-scroll-disabled="manga.disabled">
      <li v-for="(manga) in manga.list" :key="manga.id" @click="goToMangaDetail(manga.id)">
        <img class="manga-img" :src="manga.cover" alt="manga">
        <div class="manga-info">
          <div class="manga-title">{{ manga.chineseTitle || manga.title }}</div>
        </div>
      </li>
    </ul>
    <div v-if="!manga.loading && manga.disabled && manga.list.length === 0" class="empty-manga">
      <span>{{ hasActiveTags ? '没有同时包含这些标签的漫画' : '暂无漫画' }}</span>
      <button v-if="hasActiveTags" type="button" @click="clearTagFilter">清除筛选</button>
    </div>
    <acg17-loading-heart v-show="manga.loading"></acg17-loading-heart>
  </section>
  <acg17-footer v-if="manga.disabled"></acg17-footer>
</template>

<script>
import { reactive, onMounted, onUnmounted, onActivated, onDeactivated, watch, ref, nextTick, computed } from 'vue'
// import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router';
import server from '@/util/request';

import LoadingHeart from "../components/LoadingHeart";
import Acg17Footer from "../components/Acg17Footer";
import { useRecycleState } from '@/composables/useRecycleState';
import { ElMessage } from "element-plus";

const TAG_CATEGORIES = [
  { key: 'group', field: 'groupTags', label: '团队', expanded: false },
  { key: 'artist', field: 'artistTags', label: '艺术家', expanded: false },
  { key: 'character', field: 'characterTags', label: '角色', expanded: true },
  { key: 'male', field: 'maleTags', label: '男性', expanded: true },
  { key: 'female', field: 'femaleTags', label: '女性', expanded: false },
  { key: 'mixed', field: 'mixedTags', label: '混合', expanded: true },
  { key: 'other', field: 'otherTags', label: '其他', expanded: true },
  { key: 'original', field: 'originalTags', label: '原作', expanded: true },
]

function parseTagIds(queryValue) {
  const values = Array.isArray(queryValue) ? queryValue : [queryValue]
  return [...new Set(values
    .flatMap(value => String(value ?? '').split(','))
    .map(value => Number(value))
    .filter(value => Number.isInteger(value) && value > 0))]
    .sort((a, b) => a - b)
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
    const tagSearch = ref('')
    const tagSearchInput = ref(null)
    const activeTagSearchIndex = ref(-1)
    const selectedTagIds = computed(() => parseTagIds(route.query.tagIds))
    const selectedTagKey = computed(() => selectedTagIds.value.join(','))
    const normalizedTagSearch = computed(() => normalizeTagName(tagSearch.value))
    const isTagSearching = computed(() => normalizedTagSearch.value.length > 0)
    const mangaResultText = computed(() => (
      manga.currentPage === 0 || (manga.loading && manga.currentPage === 1)
        ? '查询中'
        : `找到 ${manga.total} 部`
    ))
    let resizeObserver = null
    let pageActive = false
    let loadedTagIds = selectedTagKey.value
    let mangaRequestVersion = 0
    let tagListLoading = false
    let tagListLoaded = false
    let tagListRequestVersion = 0

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
      expand: Object.fromEntries(TAG_CATEGORIES.map(category => [category.key, category.expanded]))
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
          const sourceTags = query
            ? group.tags
              .filter(item => normalizeTagName(item.tagName).includes(query))
              .sort((a, b) => {
                const rankDifference = getTagMatchRank(a.tagName, query) - getTagMatchRank(b.tagName, query)
                if (rankDifference) return rankDifference
                const countDifference = Number(b.tagCount || 0) - Number(a.tagCount || 0)
                if (countDifference) return countDifference
                return String(a.tagName).localeCompare(String(b.tagName), 'zh-CN')
              })
            : getVisibleTags(group.tags, group.key)
          const tags = sourceTags.map(item => ({
            ...item,
            nameParts: createTagNameParts(item.tagName, query),
            searchIndex: query ? searchIndex++ : -1,
          }))
          return {
            ...group,
            tags,
            hasHidden: !query && hasHiddenTags(group.tags),
          }
        })
        .filter(group => group.tags.length || group.hasHidden)
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
      displayedTagGroups.value.some(group => group.tags.length || group.hasHidden)
    ))

    const selectedTags = computed(() => {
      const tagLookup = new Map()
      for (const group of tagGroups.value) {
        for (const item of group.tags) {
          tagLookup.set(Number(item.tagId), {
            ...item,
            categoryLabel: group.label,
          })
        }
      }
      return selectedTagIds.value.map(tagId => tagLookup.get(tagId) || {
        tagId,
        tagName: `标签 #${tagId}`,
        categoryLabel: '',
      })
    })

    // 打开/关闭标签列表
    function openTagList() {
      tag.showTagList = !tag.showTagList
      scrollToTop()
      // 点击标签列表时，关闭搜索框
      if (tag.showTagList) {
        loadTagList()
        nextTick(() => tagSearchInput.value?.focus())
      }
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
          }
        })
    }

    const hasActiveTags = computed(() => selectedTagIds.value.length > 0)

    function sortedTags(tags) {
      return tags.slice().sort((a, b) => b.tagCount - a.tagCount)
    }

    function getVisibleTags(tags, category) {
      const list = sortedTags(tags)
      if (tag.expand[category]) return list
      return list.filter(item => item.tagCount > 3)
    }

    function hasHiddenTags(tags) {
      const list = sortedTags(tags)
      return list.some(item => item.tagCount <= 3)
    }

    function toggleExpand(category) {
      tag.expand[category] = !tag.expand[category]
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
      nextTick(() => tagSearchInput.value?.focus())
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

    function updateTagFilter(tagIds) {
      const normalizedTagIds = [...new Set(tagIds)].sort((a, b) => a - b)
      router.push({
        path: '/acg/manga',
        query: normalizedTagIds.length
          ? { tagIds: normalizedTagIds.join(',') }
          : {}
      })
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

    function scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: "smooth"
      })
    }

    function resetMangaList() {
      mangaRequestVersion += 1
      loadedTagIds = selectedTagKey.value
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

    // 监听路由查询参数变化，重新获取数据
    watch(selectedTagKey, currentTagIds => {
      if (route.name === 'Manga' && currentTagIds !== loadedTagIds) {
        resetMangaList()
      }
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
      pageActive = true
      window.addEventListener('scroll', handleScroll)
      handleScroll()
      nextTick(() => {
        if (!pageActive) return
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
      pageActive = false
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
      if (selectedTagKey.value !== loadedTagIds) {
        resetMangaList()
      }
      if (hasActiveTags.value && !tagListLoaded) loadTagList()
      activatePageListeners()
    })

    onDeactivated(deactivatePageListeners)
    onUnmounted(deactivatePageListeners)

    return { manga, mangaResultText, goToMangaDetail, isRecycle, toggleRecycle, setRecycle, loadManga, randomManga, scrollToTop, showBackToTop, containerWidth, tag, tagSearch, tagSearchInput, displayedTagGroups, matchingTagCount, matchingCategorySummary, hasDisplayedTags, isTagSearching, activeTagSearchIndex, openTagList, selectedTagIds, selectedTags, isTagActive, toggleTag, hasActiveTags, clearTagFilter, clearTagSearch, handleTagSearchKeydown, setActiveTagSearchIndex, toggleExpand }
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

.tag-container {
  max-width: 1380px;
  margin: 0 auto;
  max-height: 0;
  opacity: 0;
  overflow: hidden;
  transition: max-height 0.5s ease-out, opacity 0.4s ease-out 0.2s, margin-bottom 0.1s ease-out 0.3s;
  pointer-events: none;
}

.tag-container.is-visible {
  margin-bottom: 20px;
  max-height: 1000px;
  opacity: 1;
  transition: max-height 0.5s ease-in, opacity 0.3s ease-in, margin-bottom 0.1s ease-out 0.2s;
  pointer-events: auto;
}

.tag-panel {
  background: #ffffff;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  padding: 16px 18px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.tag-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.tag-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.clear-tags-btn {
  border: 1px solid #e9ecef;
  background: #ffffff;
  color: #409eff;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
}

.clear-tags-btn:hover {
  border-color: #409eff;
  background: #f0f8ff;
}

.selected-tags {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #f5f9ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;
}

.selected-tags.compact {
  margin-bottom: 0;
}

.selected-tags-label {
  flex: 0 0 auto;
  padding-top: 4px;
  color: #606266;
  font-size: 13px;
  font-weight: 600;
}

.selected-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  min-width: 0;
  max-height: 64px;
  overflow-y: auto;
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
  background: #e3f2fd;
  border-color: #64b5f6;
}

.selected-tag-category {
  color: #78909c;
  font-size: 11px;
}

.selected-tag-remove {
  font-size: 16px;
  line-height: 12px;
}

.tag-search-area {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.tag-search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.tag-search-icon {
  position: absolute;
  left: 11px;
  width: 18px;
  height: 18px;
  color: #909399;
  pointer-events: none;
}

.tag-search-box input {
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

.tag-search-box input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
}

.tag-search-box input:disabled {
  background: #f5f7fa;
  cursor: wait;
}

.tag-search-box input::-webkit-search-cancel-button {
  display: none;
}

.clear-tag-search {
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

.clear-tag-search:hover {
  background: #f2f6fc;
  color: #409eff;
}

.clear-tag-search svg {
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
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tag-results.searching {
  max-height: 460px;
  padding: 2px 5px 2px 2px;
  overflow-y: auto;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.label {
  font-weight: 600;
  color: #495057;
  min-width: 52px;
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
  border: 1px solid #e9ecef;
  background: #ffffff;
  color: #409eff;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px;
  line-height: 1.2;
}

.expand-tag-btn:hover {
  border-color: #409eff;
  background: #f0f8ff;
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
  transition: transform 0.6s ease;
}

.tag-container.is-visible + ul {
  transform: translateY(10px);
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

  .tag-container {
    margin: 12px auto;
  }

  .label {
    min-width: 44px;
    font-size: 13px;
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

  .tag-panel {
    padding: 12px;
  }

  .tag-header {
    gap: 8px;
  }

  .selected-tags {
    flex-direction: column;
    gap: 7px;
  }

  .selected-tags-label {
    padding-top: 0;
  }

  .tag-results.searching {
    max-height: 360px;
  }

  .tag-search-box input {
    font-size: 13px;
  }

  .label {
    min-width: 40px;
    font-size: 12px;
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
