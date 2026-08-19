<template>
  <div
    class="filter-container"
    :class="{ 'is-visible': showFilterPanel }"
    @transitionend="handleFilterPanelTransitionEnd"
  >
    <div class="filter-collapse">
      <div class="filter-panel">
        <div class="filter-header">
          <div class="filter-title">{{ filterPanelTitle }}</div>
          <button v-if="hasActiveFilters" type="button" class="clear-filters-btn" @click="clearAllFilters">
            清除全部
          </button>
        </div>

        <div v-if="hasActiveFilters" class="selected-tags" :class="{ compact: !editor }">
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

        <div v-show="showTagList" class="tag-body">
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
          <div ref="tagResults" class="tag-results" :class="{ searching: isTagSearching }">
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
</template>

<script setup>
import { computed, nextTick, onActivated, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import server from '@/util/request'
import { MANGA_TAG_CATEGORIES } from '@/constants/mangaTagCategories'

const visibleLimits = {
  group: 16,
  artist: 16,
  character: 16,
  male: 20,
  female: 36,
  mixed: 10,
  other: 10,
  original: 10,
}
const tagCategories = MANGA_TAG_CATEGORIES.map(category => ({
  ...category,
  visibleLimit: visibleLimits[category.key],
}))

const props = defineProps({
  editor: {
    type: String,
    default: '',
    validator: value => ['', 'title', 'tags'].includes(value),
  },
  activeTitle: {
    type: String,
    default: '',
  },
  selectedTagIds: {
    type: Array,
    default: () => [],
  },
  resultTotal: {
    type: Number,
    default: 0,
  },
  resultPending: {
    type: Boolean,
    default: false,
  },
  isRecycle: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:editor', 'filters-change'])

const renderedEditor = ref(props.editor)
const titleSearchInput = ref(null)
const titleSearchDraft = ref(props.activeTitle)
const tagSearch = ref('')
const tagSearchInput = ref(null)
const tagResults = ref(null)
const activeTagSearchIndex = ref(-1)
let tagListLoading = false
let tagListLoaded = false
let tagListRequestVersion = 0
let pendingFilterClose = ''
let pendingFilterFocus = ''

const tag = reactive({
  ...Object.fromEntries(tagCategories.map(category => [category.field, []])),
  loading: false,
  expandedCategories: Object.fromEntries(tagCategories.map(category => [category.key, false])),
})

const normalizedSelectedTagIds = computed(() => [...new Set(props.selectedTagIds
  .map(tagId => Number(tagId))
  .filter(tagId => Number.isInteger(tagId) && tagId > 0))]
  .sort((a, b) => a - b))
const hasTitleFilter = computed(() => props.activeTitle.length > 0)
const hasActiveTags = computed(() => normalizedSelectedTagIds.value.length > 0)
const hasActiveFilters = computed(() => hasTitleFilter.value || hasActiveTags.value)
const activeFilterCount = computed(() => normalizedSelectedTagIds.value.length + (hasTitleFilter.value ? 1 : 0))
const mangaResultText = computed(() => props.resultPending ? '查询中' : `找到 ${props.resultTotal} 部`)
const showFilterPanel = computed(() => Boolean(props.editor) || hasActiveFilters.value)
const showTitleSearch = computed(() => renderedEditor.value === 'title')
const showTagList = computed(() => renderedEditor.value === 'tags')
const filterPanelTitle = computed(() => {
  if (showTitleSearch.value) return '标题搜索'
  if (showTagList.value) return '标签筛选'
  return '漫画筛选'
})
const normalizedTagSearch = computed(() => normalizeTagName(tagSearch.value))
const isTagSearching = computed(() => normalizedTagSearch.value.length > 0)
const tagGroups = computed(() => tagCategories.map(category => ({
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
  isTagSearching.value ? displayedTagGroups.value.flatMap(group => group.tags) : []
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
  const selectedIds = new Set(normalizedSelectedTagIds.value)
  const resolvedIds = new Set()
  const groups = []
  for (const group of tagGroups.value) {
    const tags = sortedTags(group.tags).filter(item => selectedIds.has(Number(item.tagId)))
    if (tags.length) {
      tags.forEach(item => resolvedIds.add(Number(item.tagId)))
      groups.push({ key: group.key, label: group.label, tags })
    }
  }
  const unresolvedTags = normalizedSelectedTagIds.value
    .filter(tagId => !resolvedIds.has(tagId))
    .map(tagId => ({ tagId, tagName: `标签 #${tagId}` }))
  if (unresolvedTags.length) {
    groups.push({ key: 'unresolved', label: '标签', tags: unresolvedTags })
  }
  return groups
})

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

function finishPendingFilterClose() {
  if (!pendingFilterClose) return
  renderedEditor.value = ''
  pendingFilterClose = ''
}

function focusPendingFilterInput() {
  const input = pendingFilterFocus === 'title'
    ? titleSearchInput.value
    : pendingFilterFocus === 'tags' ? tagSearchInput.value : null
  if (!input || input.disabled) return
  input.focus({ preventScroll: true })
  pendingFilterFocus = ''
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
  titleSearchDraft.value = props.activeTitle
  emit('update:editor', '')
}

function commitTitleSearch() {
  const title = titleSearchDraft.value.trim()
  titleSearchDraft.value = title
  emit('filters-change', { title })
}

function clearTitleSearch() {
  titleSearchDraft.value = ''
  emit('filters-change', { title: '' })
  if (showTitleSearch.value) {
    nextTick(() => titleSearchInput.value?.focus({ preventScroll: true }))
  }
}

function clearAllFilters() {
  titleSearchDraft.value = ''
  emit('filters-change', { title: '', tagIds: [] })
}

function loadTagList(force = false) {
  if (tagListLoading && !force) return
  const requestVersion = ++tagListRequestVersion
  tagListLoading = true
  tag.loading = true
  server.get('/manga-tag/list', {
    params: { deleted: props.isRecycle },
  })
    .then(res => {
      if (requestVersion !== tagListRequestVersion) return
      for (const category of tagCategories) {
        tag[category.field] = res.data[category.field] || []
      }
      tagListLoaded = true
    })
    .catch(error => {
      if (requestVersion !== tagListRequestVersion) return
      ElMessage.error(`获取漫画标签失败【${error}】，请重试`)
    })
    .finally(() => {
      if (requestVersion === tagListRequestVersion) {
        tagListLoading = false
        tag.loading = false
        if (pendingFilterFocus === 'tags') nextTick(focusPendingFilterInput)
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
    sortedTagList.slice(0, visibleLimit).map(item => Number(item.tagId)),
  )
  normalizedSelectedTagIds.value.forEach(tagId => visibleTagIds.add(tagId))
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
    tagResults.value?.querySelector('.tag.keyboard-active')?.scrollIntoView({ block: 'nearest' })
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
  return normalizedSelectedTagIds.value.includes(Number(tagId))
}

function toggleTag(tagId) {
  const normalizedTagId = Number(tagId)
  const nextTagIds = new Set(normalizedSelectedTagIds.value)
  if (nextTagIds.has(normalizedTagId)) {
    nextTagIds.delete(normalizedTagId)
  } else {
    nextTagIds.add(normalizedTagId)
  }
  emit('filters-change', { tagIds: [...nextTagIds] })
}

watch(
  () => props.editor,
  (editor, previousEditor) => {
    if (previousEditor === 'title' && editor !== 'title') {
      titleSearchDraft.value = props.activeTitle
    }
    const panelWasVisible = Boolean(previousEditor) || hasActiveFilters.value
    if (editor) {
      pendingFilterClose = ''
      renderedEditor.value = editor
      pendingFilterFocus = editor
      if (editor === 'tags') loadTagList()
      if (panelWasVisible || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        nextTick(focusPendingFilterInput)
      }
      return
    }

    pendingFilterFocus = ''
    pendingFilterClose = previousEditor
    if (hasActiveFilters.value || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      finishPendingFilterClose()
    }
  },
)

watch(
  () => props.activeTitle,
  title => {
    titleSearchDraft.value = title
  },
)

watch(
  () => props.isRecycle,
  () => {
    if (showTagList.value || hasActiveTags.value) loadTagList(true)
  },
)

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

onMounted(() => {
  if (hasActiveTags.value) loadTagList()
  if (props.editor) {
    pendingFilterFocus = props.editor
    nextTick(focusPendingFilterInput)
  }
})

onActivated(() => {
  if (hasActiveTags.value && !tagListLoaded) loadTagList()
})

onUnmounted(() => {
  tagListRequestVersion += 1
})
</script>

<style scoped>
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
  padding: 16px 18px;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.filter-title {
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
}

.clear-filters-btn {
  padding: 6px 12px;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  background: #ffffff;
  color: #409eff;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s ease;
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
  border: 1px solid #d9ecff;
  border-radius: 8px;
  background: #f5f9ff;
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
  flex: 1;
  flex-wrap: nowrap;
  max-height: none;
  overflow-x: auto;
  overflow-y: hidden;
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
  cursor: pointer;
  font: inherit;
  font-size: 13px;
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
  cursor: pointer;
  font: inherit;
  font-size: 14px;
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
  box-sizing: border-box;
  width: 100%;
  height: 38px;
  padding: 0 42px 0 38px;
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
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.tag-group-count {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
}

.tags-container {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 6px;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  background-color: #f8f9fa;
  color: #343a40;
  cursor: pointer;
  font-family: inherit;
  font-size: 13px;
  transition: all 0.2s ease;
}

.tag:hover {
  border-color: #dee2e6;
  background-color: #e9ecef;
  transform: translateY(-1px);
}

.tag.active {
  border-color: #90caf9;
  background-color: #e3f2fd;
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
  min-width: 20px;
  padding: 2px 4px;
  margin-left: 6px;
  border-radius: 10px;
  background-color: #6c757d;
  color: white;
  font-size: 11px;
  font-weight: 500;
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
  font-size: 12px;
  line-height: 1.2;
  transition: background-color 0.2s ease;
  white-space: nowrap;
}

.expand-tag-btn:hover {
  background: #ecf5ff;
}

.expand-tag-arrow {
  flex: 0 0 auto;
  width: 10px;
  height: 10px;
  fill: currentColor;
  transform-origin: center;
  transition: transform 0.2s ease;
}

.expand-tag-arrow.expanded {
  transform: rotate(180deg);
}

.empty-tags {
  padding: 6px 0;
  color: #909399;
  font-size: 13px;
}

@media (max-width: 991px) and (min-width: 768px) {
  .filter-container {
    margin: 12px auto;
  }
}

@media (max-width: 767px) {
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
}

@media (prefers-reduced-motion: reduce) {
  .filter-container {
    transition: none;
  }
}
</style>
