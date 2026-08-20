<template>
  <div class="novel-page">
    <!-- 侧边栏筛选 -->
    <div class="sidebar">
      <el-affix :offset="76" :z-index="9">
        <div class="filter-panel">
          <!-- 搜索框 -->
          <div class="search-section">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索书名或作者..."
              aria-label="搜索小说"
              clearable
              class="search-input"
              size="large"
              @input="handleSearch"
            >
              <template #prefix>
                <icon icon="#icon-search"></icon>
              </template>
            </el-input>
          </div>

          <el-divider />

          <!-- 标签筛选 -->
          <div class="filter-section">
            <div class="filter-header">
              <h3 class="filter-title">
                <icon icon="#icon-tag"></icon>
                <span>标签筛选</span>
              </h3>
              <el-button v-if="selectedTag !== null" @click="selectTag(null)" text size="small" class="clear-btn">
                清除
              </el-button>
            </div>
            <div class="tag-cloud">
              <el-tag
                class="tag-item"
                :class="{ active: selectedTag === null }"
                :aria-pressed="selectedTag === null"
                title="全部"
                effect="plain"
                role="button"
                tabindex="0"
                @click="selectTag(null)"
                @keydown.enter.prevent="selectTag(null)"
                @keydown.space.prevent="selectTag(null)"
              >
                全部
              </el-tag>
              <el-tag
                v-for="item in novel.tagList"
                :key="'tags-' + item.id"
                class="tag-item"
                :class="{ active: selectedTag === item.id }"
                :aria-pressed="selectedTag === item.id"
                :title="item.name"
                effect="plain"
                role="button"
                tabindex="0"
                @click="selectTag(item.id)"
                @keydown.enter.prevent="selectTag(item.id)"
                @keydown.space.prevent="selectTag(item.id)"
              >
                {{ item.name }}
              </el-tag>
            </div>
          </div>


        </div>
      </el-affix>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 顶部工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="sort-section">
            <span class="sort-label">
              <icon icon="#icon-sort"></icon>
              排序方式
            </span>
            <div class="sort-buttons">
              <el-button :type="sortType === 'created' ? 'primary' : ''" @click="setSortType('created')"
                class="sort-btn">
                <icon icon="#icon-time"></icon>
                最近添加
              </el-button>
              <div class="sort-btn-group">
                <el-button :type="sortType === 'words' ? 'primary' : ''" @click="setSortType('words')" class="sort-btn">
                  <icon icon="#icon-words"></icon>
                  字数排序
                </el-button>
                <el-button v-if="sortType === 'words'" @click="toggleSortOrder" size="small" class="sort-order-btn">
                  <icon :icon="sortOrder === 'desc' ? '#icon-sort-asc' : '#icon-sort-desc'"></icon>
                </el-button>
              </div>
              <div class="sort-btn-group">
                <el-button :type="sortType === 'updated' ? 'primary' : ''" @click="setSortType('updated')" class="sort-btn">
                  <icon icon="#icon-time"></icon>
                  更新时间
                </el-button>
                <el-button v-if="sortType === 'updated'" @click="toggleSortOrder" size="small" class="sort-order-btn">
                  <icon :icon="sortOrder === 'desc' ? '#icon-sort-asc' : '#icon-sort-desc'"></icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="showStats" class="toolbar-right">
          <div class="stats">
            <div class="stat-item">
              <div class="stat-label">作品总数</div>
              <div class="stat-value">{{ $store.state.userInfo.novelCount }}本</div>
            </div>
            <el-divider direction="vertical" />
            <div class="stat-item">
              <div class="stat-label">总字数</div>
              <div class="stat-value">{{ $store.state.userInfo.novelWords }}字</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 小说列表 -->
      <div class="novel-table-container">
        <el-table v-if="novel.list.length" :data="novel.list" v-infinite-scroll="loadMoreNovels"
          :infinite-scroll-disabled="novel.loading || novel.error || novel.disabled || !pageActive"
          @row-click="toNovelContent" row-key="id" table-layout="fixed" stripe class="novel-table">

          <el-table-column prop="title" label="书名" min-width="250">
            <template #default="scope">
              <div class="title-cell" :title="scope.row.title">
                <span class="novel-title">{{ scope.row.title }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column v-if="visibleColumns.tags" prop="tags" label="标签" width="240">
            <template #default="scope">
              <div
                v-if="scope.row.tags && scope.row.tags.length"
                class="tags-cell"
                :title="scope.row.tags.join('、')"
              >
                <el-tag
                  v-for="(tag, i) in scope.row.tags.slice(0, 3)"
                  :key="'tag-' + i"
                  size="small"
                  class="novel-tag"
                >
                  {{ tag }}
                </el-tag>
                <span v-if="scope.row.tags.length > 3" class="tag-overflow-count">
                  +{{ scope.row.tags.length - 3 }}
                </span>
              </div>
              <span v-else class="empty-value">—</span>
            </template>
          </el-table-column>

          <el-table-column v-if="visibleColumns.author" prop="author" label="作者" width="120">
            <template #default="scope">
              <span class="author-name" :title="scope.row.author">{{ scope.row.author || '—' }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="visibleColumns.words"
            prop="totalWords"
            label="字数"
            width="110"
            align="right"
            header-align="right"
          >
            <template #default="scope">
              <span class="word-count">{{ formatWordCount(scope.row.totalWords) }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="visibleColumns.updated"
            prop="updateTime"
            label="更新时间"
            width="160"
            align="center"
          >
            <template #default="scope">
              <span class="update-time">{{ scope.row.updateTime || '—' }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="visibleColumns.actions" label="操作" width="96" align="center">
            <template #default="scope">
              <div class="action-buttons" @click.stop>
                <el-button 
                  v-if="!isRecycle" 
                  @click="toUploadChapter(scope.row.id, scope.row.title)"
                  text
                  size="small"
                  class="action-button action-add"
                  aria-label="新增章节"
                  title="新增章节"
                >
                  <icon icon="#icon-add"></icon>
                </el-button>
                <el-button 
                  v-if="!isRecycle" 
                  @click="deleteNovel(scope.row.id)"
                  text
                  size="small"
                  class="action-button action-delete"
                  aria-label="删除小说"
                  title="删除小说"
                >
                  <icon icon="#icon-delete"></icon>
                </el-button>
                <el-button 
                  v-if="isRecycle" 
                  @click="restoreNovel(scope.row.id)"
                  text
                  size="small"
                  class="action-button action-restore"
                  aria-label="恢复小说"
                  title="恢复小说"
                >
                  <icon icon="#icon-restore"></icon>
                </el-button>
              </div>
            </template>
          </el-table-column>

          <el-table-column
            v-if="visibleColumns.indicator"
            width="44"
            align="center"
            class-name="indicator-column"
          >
            <template #default>
              <icon icon="#icon-right" class="row-indicator"></icon>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="novel.error" class="list-state list-error" role="alert">
          <span>小说列表加载失败，请重试</span>
          <el-button type="primary" plain @click="retryLoadNovels">重新加载</el-button>
        </div>

        <div v-else-if="!novel.loading && novel.disabled && novel.list.length === 0" class="list-state list-empty">
          <span>{{ emptyNovelText }}</span>
          <div v-if="hasActiveFilters" class="list-state-actions">
            <el-button v-if="activeKeyword" plain @click="clearSearch">清除搜索</el-button>
            <el-button v-if="selectedTag !== null" plain @click="selectTag(null)">清除标签</el-button>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="novel.loading" class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>
    </div>
  </div>

  <acg17-footer v-if="novel.disabled && novel.list.length"></acg17-footer>
</template>

<script>
import {
  computed,
  onActivated,
  onBeforeMount,
  onDeactivated,
  onMounted,
  onUnmounted,
  reactive,
  ref,
  watch,
} from 'vue'
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router'
import server from '@/util/request'
import Acg17Footer from '../components/Acg17Footer'
import { useRecycleState } from '@/composables/useRecycleState'

const SORT_TYPES = new Set(['created', 'words', 'updated'])
const SEARCH_DELAY = 300
const RESPONSIVE_LEVEL = Object.freeze({
  FULL: 0,
  NO_WORDS: 1,
  NO_UPDATED: 2,
  TOP_FILTER: 3,
  NO_AUTHOR: 4,
  NO_TAGS: 5,
  NO_ACTIONS: 6,
})

function getResponsiveLevel(width) {
  if (width <= 480) return RESPONSIVE_LEVEL.NO_ACTIONS
  if (width <= 640) return RESPONSIVE_LEVEL.NO_TAGS
  if (width <= 900) return RESPONSIVE_LEVEL.NO_AUTHOR
  if (width <= 1100) return RESPONSIVE_LEVEL.TOP_FILTER
  if (width <= 1280) return RESPONSIVE_LEVEL.NO_UPDATED
  if (width <= 1366) return RESPONSIVE_LEVEL.NO_WORDS
  return RESPONSIVE_LEVEL.FULL
}

function firstQueryValue(value) {
  return Array.isArray(value) ? value[0] : value
}

function normalizeKeyword(value) {
  return String(firstQueryValue(value) ?? '').trim()
}

function normalizeTagId(value) {
  const tagId = Number(firstQueryValue(value))
  return Number.isInteger(tagId) && tagId > 0 ? tagId : null
}

function normalizeSortType(value) {
  const sortType = String(firstQueryValue(value) ?? '')
  return SORT_TYPES.has(sortType) ? sortType : 'created'
}

function normalizeSortOrder(value) {
  return firstQueryValue(value) === 'asc' ? 'asc' : 'desc'
}

function formatWordCount(value) {
  if (value === null || value === undefined || value === '') return '—'
  const wordCount = Number(value)
  return Number.isFinite(wordCount) ? wordCount.toLocaleString('zh-CN') : (value || '—')
}

export default {
  name: 'Novel',
  components: {
    'acg17-footer': Acg17Footer
  },
  setup() {
    const store = useStore()
    const route = useRoute()
    const router = useRouter()
    const { isRecycle } = useRecycleState('novel')

    const novel = reactive({
      currentPage: 0,
      list: [],
      loading: false,
      error: false,
      disabled: false,
      total: 0,
      tagList: [],
    })

    const activeKeyword = computed(() => normalizeKeyword(route.query.keyword))
    const selectedTag = computed(() => normalizeTagId(route.query.tagId))
    const sortType = computed(() => normalizeSortType(route.query.sortBy))
    const sortOrder = computed(() => (
      sortType.value === 'created' ? 'desc' : normalizeSortOrder(route.query.sortOrder)
    ))
    const searchKeyword = ref(activeKeyword.value)
    const pageActive = ref(false)
    const responsiveLevel = ref(getResponsiveLevel(window.innerWidth))
    const visibleColumns = computed(() => ({
      words: responsiveLevel.value < RESPONSIVE_LEVEL.NO_WORDS,
      updated: responsiveLevel.value < RESPONSIVE_LEVEL.NO_UPDATED,
      author: responsiveLevel.value < RESPONSIVE_LEVEL.NO_AUTHOR,
      tags: responsiveLevel.value < RESPONSIVE_LEVEL.NO_TAGS,
      actions: responsiveLevel.value < RESPONSIVE_LEVEL.NO_ACTIONS,
      indicator: responsiveLevel.value >= RESPONSIVE_LEVEL.NO_ACTIONS,
    }))
    const showStats = computed(() => responsiveLevel.value < RESPONSIVE_LEVEL.TOP_FILTER)
    const activeQueryKey = computed(() => JSON.stringify([
      activeKeyword.value,
      selectedTag.value,
      sortType.value,
      sortOrder.value,
    ]))
    const hasActiveFilters = computed(() => (
      activeKeyword.value.length > 0 || selectedTag.value !== null
    ))
    const activeTagName = computed(() => novel.tagList.find(
      item => Number(item.id) === selectedTag.value,
    )?.name || '所选标签')
    const emptyNovelText = computed(() => {
      if (activeKeyword.value && selectedTag.value !== null) {
        return `没有找到书名或作者包含“${activeKeyword.value}”且带有“${activeTagName.value}”标签的小说`
      }
      if (activeKeyword.value) {
        return `没有找到书名或作者包含“${activeKeyword.value}”的小说`
      }
      if (selectedTag.value !== null) {
        return `没有找到带有“${activeTagName.value}”标签的小说`
      }
      return isRecycle.value ? '回收站中暂无小说' : '暂无小说'
    })

    let loadedQueryKey = activeQueryKey.value
    let novelRequestVersion = 0
    let searchTimer = 0

    onBeforeMount(() => {
      server.get('/novel-tag/getList')
        .then(response => {
          novel.tagList = Array.isArray(response.data) ? response.data : []
        })
        .catch(error => {
          console.error('获取小说标签失败:', error)
        })
    })

    function loadNovels() {
      if (novel.loading || novel.error || novel.disabled) return

      const requestVersion = novelRequestVersion
      const pageNum = novel.currentPage + 1
      novel.loading = true
      novel.error = false

      const params = {
        pageNum,
        deleted: isRecycle.value,
        sortBy: sortType.value,
        sortOrder: sortOrder.value,
      }
      if (selectedTag.value !== null) params.tagId = selectedTag.value
      if (activeKeyword.value) params.keyword = activeKeyword.value

      server.get('/novel/getList', { params })
        .then(response => {
          if (requestVersion !== novelRequestVersion) return

          const pageData = response.data || {}
          const records = Array.isArray(pageData.records) ? pageData.records : []
          const existingIds = new Set(novel.list.map(item => item.id))
          novel.list.push(...records.filter(item => !existingIds.has(item.id)))
          novel.total = Number(pageData.total) || 0
          novel.currentPage = Number(pageData.current) || pageNum
          novel.disabled = records.length === 0 || novel.list.length >= novel.total
        })
        .catch(error => {
          if (requestVersion !== novelRequestVersion) return
          novel.error = true
          console.error('获取小说列表失败:', error)
        })
        .finally(() => {
          if (requestVersion === novelRequestVersion) novel.loading = false
        })
    }

    function loadMoreNovels() {
      if (!pageActive.value) return
      loadNovels()
    }

    function updateResponsiveLevel() {
      responsiveLevel.value = getResponsiveLevel(window.innerWidth)
    }

    function retryLoadNovels() {
      if (novel.loading) return
      novel.error = false
      loadNovels()
    }

    function resetNovelList() {
      novelRequestVersion += 1
      loadedQueryKey = activeQueryKey.value
      novel.currentPage = 0
      novel.list = []
      novel.loading = false
      novel.error = false
      novel.disabled = false
      novel.total = 0
      loadNovels()
    }

    function updateListQuery(overrides = {}) {
      const keyword = Object.hasOwn(overrides, 'keyword')
        ? normalizeKeyword(overrides.keyword)
        : activeKeyword.value
      const tagId = Object.hasOwn(overrides, 'tagId')
        ? normalizeTagId(overrides.tagId)
        : selectedTag.value
      const nextSortType = Object.hasOwn(overrides, 'sortType')
        ? normalizeSortType(overrides.sortType)
        : sortType.value
      const nextSortOrder = nextSortType === 'created'
        ? 'desc'
        : Object.hasOwn(overrides, 'sortOrder')
          ? normalizeSortOrder(overrides.sortOrder)
          : sortOrder.value
      const nextQueryKey = JSON.stringify([keyword, tagId, nextSortType, nextSortOrder])
      if (nextQueryKey === activeQueryKey.value) return

      const query = {}
      if (keyword) query.keyword = keyword
      if (tagId !== null) query.tagId = String(tagId)
      if (nextSortType !== 'created') query.sortBy = nextSortType
      if (nextSortType !== 'created' && nextSortOrder === 'asc') query.sortOrder = 'asc'

      router.replace({ name: 'Novel', query })
    }

    function clearSearchTimer() {
      if (!searchTimer) return
      window.clearTimeout(searchTimer)
      searchTimer = 0
    }

    function handleSearch(value) {
      clearSearchTimer()
      const keyword = normalizeKeyword(value)
      if (keyword === activeKeyword.value) return

      searchTimer = window.setTimeout(() => {
        searchTimer = 0
        updateListQuery({ keyword })
      }, SEARCH_DELAY)
    }

    function clearSearch() {
      clearSearchTimer()
      searchKeyword.value = ''
      updateListQuery({ keyword: '' })
    }

    function selectTag(tagId) {
      updateListQuery({ tagId })
    }

    function setSortType(type) {
      const nextType = normalizeSortType(type)
      if (nextType === 'created') {
        updateListQuery({ sortType: 'created', sortOrder: 'desc' })
        return
      }

      updateListQuery({
        sortType: nextType,
        sortOrder: sortType.value === nextType && sortOrder.value === 'desc' ? 'asc' : 'desc',
      })
    }

    function toggleSortOrder() {
      if (sortType.value === 'created') return
      updateListQuery({ sortOrder: sortOrder.value === 'desc' ? 'asc' : 'desc' })
    }

    function toNovelContent(row) {
      clearSearchTimer()
      const novelId = typeof row === 'object' ? row.id : row
      router.push('/acg/novel/' + novelId)
    }

    function toUploadChapter(novelId, novelTitle) {
      store.commit('openUploadDrawer', {
        type: 'novel',
        mode: 'chapter',
        context: { novelId, novelTitle },
      })
    }

    function deleteNovel(novelId) {
      server.delete('/novel/' + novelId)
        .then(() => {
          const index = novel.list.findIndex(item => item.id === novelId)
          if (index !== -1) novel.list.splice(index, 1)
          ElMessage.success('小说删除成功！')
        })
        .catch(error => {
          console.error('删除小说失败:', error)
        })
    }

    function restoreNovel(novelId) {
      server.put(`/novel/${novelId}/restore`)
        .then(() => {
          const index = novel.list.findIndex(item => item.id === novelId)
          if (index !== -1) novel.list.splice(index, 1)
          ElMessage.success('小说恢复成功！')
        })
        .catch(error => {
          console.error('恢复小说失败:', error)
        })
    }

    watch(activeKeyword, keyword => {
      clearSearchTimer()
      searchKeyword.value = keyword
    })

    watch(activeQueryKey, queryKey => {
      if (route.name === 'Novel' && queryKey !== loadedQueryKey) resetNovelList()
    })

    watch(isRecycle, resetNovelList)

    onMounted(() => {
      pageActive.value = true
      window.addEventListener('resize', updateResponsiveLevel)
      loadNovels()
    })

    onActivated(() => {
      pageActive.value = true
      if (activeQueryKey.value !== loadedQueryKey) {
        resetNovelList()
      } else if (!novel.list.length && !novel.loading && !novel.disabled) {
        loadNovels()
      }
    })

    onDeactivated(() => {
      pageActive.value = false
      clearSearchTimer()
    })

    onUnmounted(() => {
      novelRequestVersion += 1
      window.removeEventListener('resize', updateResponsiveLevel)
      clearSearchTimer()
    })

    return {
      activeKeyword,
      clearSearch,
      deleteNovel,
      emptyNovelText,
      formatWordCount,
      handleSearch,
      hasActiveFilters,
      isRecycle,
      loadMoreNovels,
      novel,
      pageActive,
      restoreNovel,
      retryLoadNovels,
      searchKeyword,
      selectedTag,
      selectTag,
      setSortType,
      showStats,
      sortOrder,
      sortType,
      toNovelContent,
      toUploadChapter,
      toggleSortOrder,
      visibleColumns,
    }
  }
}
</script>

<style scoped>
.novel-page {
  display: flex;
  gap: 24px;
  padding: 84px 24px 24px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: calc(100vh - 64px - 108px);
}

/* 侧边栏样式 */
.sidebar {
  width: 300px;
  flex-shrink: 0;
  position: relative;
}

.filter-panel {
  font-family: 'Blueaka', sans-serif;
  box-sizing: border-box;
  max-height: calc(100vh - 88px);
  padding: 20px;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e8edf3;
  box-shadow: 0 4px 18px rgba(31, 45, 61, 0.07);
}

/* 搜索区域 */
.search-section {
  flex: 0 0 auto;
}

.search-section :deep(input::placeholder) {
  font-family: 'Blueaka', sans-serif;
  color: #a0a8b5;
}

.search-input {
  width: 100%;
}

/* 筛选区域 */
.filter-section {
  min-height: 0;
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
}

.filter-section:last-child {
  margin-bottom: 0;
}

.filter-header {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.filter-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.filter-title .icon {
  width: 18px;
  height: 18px;
  fill: #409eff;
}

.clear-btn {
  min-height: 24px;
  padding: 0 4px;
  font-size: 12px;
  color: #8a94a3;
}

.clear-btn:hover {
  color: #409eff;
}

/* 筛选选项 */
.filter-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  background: #f7f7f7;
}

.filter-option:hover {
  background: #f0f9ff;
  border-color: #e1f5fe;
  transform: translateX(4px);
}

.filter-option.active {
  background: linear-gradient(135deg, #409eff, #67c23a);
  color: white;
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.option-text {
  font-size: 14px;
  font-weight: 400;
}

.option-count {
  font-size: 12px;
  opacity: 0.8;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 12px;
}

.filter-option:not(.active) .option-count {
  background: rgba(0, 0, 0, 0.1);
  color: #666;
}

/* 标签云 */
.tag-cloud {
  min-height: 0;
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 7px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #d8dee8 transparent;
}

.tag-cloud::-webkit-scrollbar {
  width: 6px;
}

.tag-cloud::-webkit-scrollbar-thumb {
  border-radius: 6px;
  background: #d8dee8;
}

.tag-item {
  max-width: 100%;
  height: 30px;
  padding: 0 9px;
  cursor: pointer;
  font-size: 13px;
  color: #5f6977;
  background: #f6f8fb;
  border-radius: 8px;
  border: 1px solid #e1e7ee;
  transition: color 0.2s ease, background-color 0.2s ease,
  border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.tag-item:hover {
  transform: translateY(-1px);
  color: #409eff;
  background: #f0f7ff;
  border-color: #409eff;
  box-shadow: 0 3px 8px rgba(64, 158, 255, 0.12);
}

.tag-item:focus-visible {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.16);
}

.tag-item.active {
  background: #409eff !important;
  color: white;
  border-color: #409eff !important;
  box-shadow: 0 3px 9px rgba(64, 158, 255, 0.22);
}

.tag-item :deep(.el-tag__content) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}



/* 主内容区样式 */
.main-content {
  flex: 1;
  min-width: 0;
}

/* 工具栏样式 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 12px 20px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
}

.toolbar ::v-deep(span) {
  font-family: 'Blueaka', sans-serif;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

/* 排序区域 */
.sort-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.sort-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
}

.sort-label i {
  font-size: 16px;
  color: #409eff;
}

.sort-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

.sort-btn-group {
  display: flex;
  align-items: center;
  gap: 4px;
}

.sort-btn {
  border-radius: 8px;
  transition: all 0.3s ease;
  border: 1px solid #e4e7ed;
  background: #fafafa;
}

.sort-btn:hover {
  background: #f0f9ff;
  border-color: #409eff;
  transform: translateY(-1px);
}

.sort-btn i {
  margin-right: 4px;
}

.sort-order-btn {
  border-radius: 8px;
  transition: all 0.3s ease;
  border: none;
  background: linear-gradient(135deg, #409eff, #67c23a);
  color: white;
  margin-left: 0;
  min-width: 32px;
  height: 32px;
  padding: 4px 6px;
}

.sort-order-btn:hover {
  background: linear-gradient(135deg, #66b1ff, #85ce61);
  transform: translateY(-1px);
}

.sort-order-btn .icon {
  width: 14px;
  height: 14px;
}



.stats {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stats .stat-item {
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-family: 'Blueaka', sans-serif;
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}

/* 表格容器 */
.novel-table-container {
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 4px 18px rgba(31, 45, 61, 0.07);
  border: 1px solid #e8edf3;
  overflow: hidden;
}

.novel-table-container :deep(.cell) {
  font-family: 'Blueaka', sans-serif;
  padding: 0 16px;
}

/* 表格样式 */
.novel-table {
  width: 100%;
  --el-table-border-color: #edf0f4;
  --el-table-header-bg-color: #f6f8fb;
  --el-table-header-text-color: #606a78;
  --el-table-row-hover-bg-color: #f1f7ff;
  --el-table-striped-bg-color: #fafbfd;
  color: #3d4551;
}

.novel-table :deep(.el-table__header-wrapper th.el-table__cell) {
  height: 48px;
  padding: 0;
  background: #f6f8fb;
  border-bottom: 1px solid #e4e9f0;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.novel-table :deep(.el-table__body-wrapper td.el-table__cell) {
  height: 64px;
  padding: 0;
  border-bottom-color: #edf0f4;
  transition: background-color 0.2s ease;
}

.novel-table :deep(.el-table__body tr) {
  cursor: pointer;
}

.novel-table :deep(.el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: #fafbfd;
}

.novel-table :deep(.el-table__body tr:hover > td.el-table__cell) {
  background: #f1f7ff !important;
}

.novel-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

/* 表格单元格样式 */
.title-cell {
  min-width: 0;
  font-family: 'Blueaka', sans-serif;
  overflow: hidden;
}

.novel-title {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  line-height: 1.5;
  font-weight: 500;
  color: #273142;
  cursor: pointer;
  transition: color 0.2s ease;
}

.novel-table :deep(.el-table__row:hover) .novel-title {
  color: #409eff;
}

.tags-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  overflow: hidden;
}

.novel-tag {
  flex: 0 0 auto;
  max-width: 88px;
  font-size: 13px;
  border-radius: 6px;
  color: #5b6b80;
  background: #f2f5f9;
  border-color: #dfe6ee;
}

.novel-tag :deep(.el-tag__content) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-overflow-count {
  flex: 0 0 auto;
  min-width: 30px;
  height: 24px;
  padding: 0 6px;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1;
  color: #7b8492;
  background: #eef1f5;
}

.author-name {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: #606266;
}

.word-count {
  font-size: 14px;
  color: #465365;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.update-time {
  font-size: 13px;
  color: #7b8492;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.empty-value {
  color: #c0c4cc;
}

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
}

.action-buttons .action-button {
  width: 30px;
  height: 30px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 0;
  border-radius: 8px;
  color: #8a94a3;
  transition: color 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
}

.action-buttons .action-button + .action-button {
  margin-left: 0;
}

.action-buttons .action-button .icon {
  width: 16px;
  height: 16px;
}

.action-buttons .action-button:hover {
  transform: translateY(-1px);
}

.action-buttons .action-add:hover,
.action-buttons .action-restore:hover {
  color: #409eff;
  background: #ecf5ff;
}

.action-buttons .action-delete:hover {
  color: #f56c6c;
  background: #fef0f0;
}

.row-indicator {
  width: 16px;
  height: 16px;
  color: #a0a8b5;
}

.novel-table :deep(.indicator-column .cell) {
  padding: 0;
  text-overflow: clip;
}

/* 加载状态 */
.loading-container {
  padding: 24px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
}

.list-state {
  min-height: 260px;
  padding: 48px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  color: #909399;
  text-align: center;
  font-family: 'Blueaka', sans-serif;
}

.list-state-actions {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}

.list-state-actions .el-button + .el-button {
  margin-left: 0;
}

.list-error {
  color: #606266;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .sidebar {
    width: 280px;
  }
}

@media (max-width: 1100px) {
  .novel-page {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .sidebar :deep(.el-affix),
  .sidebar :deep(.el-affix > div) {
    display: contents;
  }
}

@media (max-width: 992px) {
  .filter-panel {
    padding: 20px;
  }

  .toolbar {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .toolbar-left {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .toolbar-right {
    justify-content: center;
  }

  .sort-section {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .sort-buttons {
    justify-content: center;
    flex-wrap: wrap;
  }

  .sort-btn-group {
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .novel-page {
    padding: 84px 16px 16px;
  }

  .filter-panel {
    padding: 16px;
  }

  .filter-options {
    gap: 6px;
  }

  .filter-option {
    padding: 10px 12px;
  }

  .sort-buttons {
    flex-wrap: wrap;
  }

  .sort-btn {
    font-size: 12px;
    padding: 6px 10px;
  }

  .sort-order-btn {
    font-size: 11px;
    padding: 3px 4px;
    min-width: 28px;
    height: 28px;
  }

  .toolbar {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .novel-page {
    padding: 84px 0 12px;
  }

  .filter-panel,
  .toolbar,
  .novel-table-container {
    border-right: none;
    border-left: none;
    border-radius: 0;
    box-shadow: none;
  }

  .filter-tags {
    gap: 6px;
  }

  .filter-tag {
    font-size: 12px;
  }

  .toolbar-left {
    gap: 8px;
  }

  .sort-label {
    font-size: 12px;
    justify-content: center;
  }

  .sort-buttons .sort-btn {
    flex: 1;
    min-width: 0;
  }
}

/* 图标尺寸样式 */
.filter-title .icon,
.sort-label .icon {
  width: 18px;
  height: 18px;
  margin-right: 4px;
}

.sort-btn .icon {
  width: 16px;
  height: 16px;
  margin-right: 4px;
}

.el-dropdown-menu__item .icon {
  width: 16px;
  height: 16px;
  margin-right: 8px;
}

/* Element Plus 组件样式覆盖 */
:deep(.el-divider) {
  margin: 16px 0;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-dropdown-menu__item i) {
  margin-right: 8px;
}

:deep(.el-skeleton) {
  padding: 0;
}

/* 表格样式覆盖 */
:deep(.el-table) {
  border: none;
}

:deep(.el-table__header) {
  background-color: #fafafa;
}

:deep(.el-table th) {
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
  color: #303133;
}

:deep(.el-table td) {
  border-bottom: 1px solid #f5f5f5;
}

:deep(.el-table__row:hover) {
  background-color: #f8f9fa;
  cursor: pointer;
}

:deep(.el-table__row:hover .novel-title) {
  color: #409eff;
}

/* 搜索框样式覆盖 */
:deep(.search-input .el-input__wrapper) {
  min-height: 44px;
  padding: 0 12px;
  background: #f7f9fc;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e1e7ee inset;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

:deep(.search-input .el-input__wrapper:hover) {
  background: #ffffff;
  box-shadow: 0 0 0 1px #cbd4df inset;
}

:deep(.search-input .el-input__wrapper.is-focus) {
  background: #ffffff;
  box-shadow: 0 0 0 1px #409eff inset, 0 0 0 3px rgba(64, 158, 255, 0.1);
}

:deep(.search-input .el-input__inner) {
  font-family: 'Blueaka', sans-serif;
  font-size: 14px;
  color: #303846;
}

/* 搜索框图标样式 */
:deep(.search-input .el-input__prefix) {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
}

:deep(.search-input .el-input__prefix .icon) {
  width: 17px;
  height: 17px;
  color: #8a94a3;
  transition: color 0.2s ease;
}

:deep(.search-input .el-input__wrapper.is-focus .el-input__prefix .icon) {
  color: #409eff;
}

/* 按钮样式覆盖 */
:deep(.sort-btn.el-button--primary) {
  background: linear-gradient(135deg, #409eff, #67c23a);
  border: none;
  color: white;
}

:deep(.sort-btn.el-button--primary:hover) {
  background: linear-gradient(135deg, #66b1ff, #85ce61);
  transform: translateY(-1px);
}

/* 标签样式覆盖 */
:deep(.tag-item.el-tag--plain) {
  background: #f6f8fb;
  border-color: #e1e7ee;
}

:deep(.tag-item.el-tag--plain:hover) {
  background: #f0f7ff;
  border-color: #409eff;
}

/* 分割线样式 */
:deep(.el-divider--horizontal) {
  flex: 0 0 auto;
  margin: 18px 0;
  border-color: #edf0f4;
}
</style>
