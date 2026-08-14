<template>
  <div class="novel-page">
    <!-- 侧边栏筛选 -->
    <div class="sidebar">
      <el-affix :offset="0" :z-index="9">
        <div class="filter-panel">
          <!-- 搜索框 -->
          <div class="search-section">
            <el-input v-model="searchKeyword" placeholder="搜索小说..." clearable @input="handleSearch" class="search-input"
              size="large">
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
                <icon icon="#icon-other"></icon>
                <span>标签筛选</span>
              </h3>
              <el-button v-if="selectedTag !== null" @click="selectTag(null)" text size="small" class="clear-btn">
                清除
              </el-button>
            </div>
            <div class="tag-cloud">
              <el-tag class="tag-item" :class="{ active: selectedTag === null }" @click="selectTag(null)" effect="plain">
                全部
              </el-tag>
              <el-tag v-for="item in novel.tagList" :key="'tags-' + item.id" class="tag-item"
                :class="{ active: selectedTag === item.id }" @click="selectTag(item.id)" effect="plain">
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
              <el-button :type="sortType === 'popularity' ? 'primary' : ''" @click="setSortType('popularity')"
                class="sort-btn">
                <icon icon="#icon-favorite-y"></icon>
                人气排序
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
                <el-button :type="sortType === 'time' ? 'primary' : ''" @click="setSortType('time')" class="sort-btn">
                  <icon icon="#icon-time"></icon>
                  更新时间
                </el-button>
                <el-button v-if="sortType === 'time'" @click="toggleSortOrder" size="small" class="sort-order-btn">
                  <icon :icon="sortOrder === 'desc' ? '#icon-sort-asc' : '#icon-sort-desc'"></icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="toolbar-right">
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
        <el-table :data="novel.list" v-infinite-scroll="loadArtworks" :infinite-scroll-disabled="novel.disabled"
          @row-click="toNovelContent" stripe class="novel-table">

          <el-table-column prop="title" label="书名" min-width="240">
            <template #default="scope">
              <div class="title-cell">
                <span class="novel-title">{{ scope.row.title }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="tags" label="标签" min-width="150">
            <template #default="scope">
              <div class="tags-cell" v-if="scope.row.tags && scope.row.tags.length">
                <el-tag v-for="(tag, i) in scope.row.tags" :key="'tag-' + i" size="small" class="tag-item">
                  {{ tag }}
                </el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="author" label="作者" width="100" align="center">
            <template #default="scope">
              <span class="author-name">{{ scope.row.author }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="totalWords" label="字数" width="90" align="center">
            <template #default="scope">
              <span class="word-count">{{ scope.row.totalWords }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="updateTime" label="更新时间" width="130" align="center">
            <template #default="scope">
              <span class="update-time">{{ scope.row.updateTime }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="90" align="center">
            <template #default="scope">
              <div class="action-buttons" @click.stop>
                <el-button 
                  v-if="!isRecycle" 
                  @click="toUploadChapter(scope.row.id, scope.row.title)"
                  text
                  size="small"
                  title="新增章节"
                >
                  <icon icon="#icon-add"></icon>
                </el-button>
                <el-button 
                  v-if="!isRecycle" 
                  @click="deleteNovel(scope.row.id)"
                  text
                  size="small"
                  title="删除小说"
                >
                  <icon icon="#icon-delete"></icon>
                </el-button>
                <el-button 
                  v-if="isRecycle" 
                  @click="restoreNovel(scope.row.id)"
                  text
                  size="small"
                  title="恢复小说"
                  class="no-margin"
                >
                  <icon icon="#icon-restore"></icon>
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 加载状态 -->
      <div v-if="novel.loading" class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>
    </div>
  </div>

  <acg17-footer v-if="novel.disabled"></acg17-footer>
</template>

<script>
import { reactive, ref, onBeforeMount, watch } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router';
import server from '@/util/request';
import Acg17Footer from "../components/Acg17Footer";
import { ElMessage } from "element-plus";
import { useRecycleState } from '@/composables/useRecycleState';

export default {
  name: "Novel",
  components: {
    'acg17-footer': Acg17Footer
  },
  setup() {
    const store = useStore()
    const router = useRouter()

    // 使用全局回收站状态管理
    const { isRecycle, toggleRecycle, setRecycle } = useRecycleState('novel')

    // 筛选状态
    const selectedTag = ref(null)
    const sortType = ref('popularity')
    const sortOrder = ref('desc') // 'asc' 升序, 'desc' 降序
    const searchKeyword = ref('')


    const novel = reactive({
      currentPage: 0, // 当前页
      list: [], // 当前页数据
      loading: false, // 加载下一页时显示loading
      disabled: false,  // 加载到最后一页时禁用加载
      tagList: [],  // 小说标签列表
    })
    // 加载标签列表
    onBeforeMount(() => {
      server.get('/novel-tag/getList')
        .then(res => {
          novel.tagList = res.data
        })
        .catch(err => {
          console.log(err)
        })
    })
    // 分页加载小说作品
    function loadArtworks() {
      novel.loading = true
      novel.disabled = true

      server.get('/novel/getList', {
        params: {
          pageNum: ++novel.currentPage,
          deleted: isRecycle.value,
          tagId: selectedTag.value || undefined,
          keyword: searchKeyword.value.trim() || undefined
        }
      })
        .then(res => {
          novel.list.push(...res.data.records)
          // records.length < 页大小：表示最后一页，置disabled=true，不再请求下一页
          novel.disabled = res.data.records.length < res.data.size
          novel.loading = false

          // 如果当前有排序设置，对新加载的数据进行排序
          if (sortType.value !== 'popularity') {
            sortNovelList()
          }
        })
        .catch(err => {
          console.log(err)
        })
    }
    // 跳转小说内容页
    function toNovelContent(row) {
      const novelId = typeof row === 'object' ? row.id : row
      router.push('/acg/novel/' + novelId)
    }
    // 上传章节
    function toUploadChapter(novelId, novelTitle) {
      store.commit('openUploadDrawer', {
        type: 'novel',
        mode: 'chapter',
        context: { novelId, novelTitle },
      })
    }
    // 删除小说
    function deleteNovel(novelId) {
      server.delete('/novel/' + novelId)
        .then(() => {
          // 从列表中移除对应的小说
          const index = novel.list.findIndex(item => item.id === novelId)
          if (index !== -1) {
            novel.list.splice(index, 1)
          }
          ElMessage.success('小说删除成功！')
        })
        .catch(err => {
          console.log(err)
        })
    }

    // 恢复小说
    function restoreNovel(novelId) {
      server.put(`/novel/${novelId}/restore`)
        .then(() => {
          // 从列表中移除对应的小说
          const index = novel.list.findIndex(item => item.id === novelId)
          if (index !== -1) {
            novel.list.splice(index, 1)
          }
          ElMessage.success('小说恢复成功！')
        })
        .catch(err => {
          console.log(err)
        })
    }

    // 筛选功能
    function selectTag(tag) {
      selectedTag.value = tag
      resetList()
    }

    function setSortType(type) {
      // 如果点击的是同一个排序类型，则切换升序/降序
      if (sortType.value === type && type !== 'popularity') {
        sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
      } else {
        sortType.value = type
        // 默认降序
        sortOrder.value = 'desc'
      }

      // 如果是人气排序，不做处理，直接返回
      if (type === 'popularity') {
        resetList()
        return
      }

      // 对当前列表进行排序
      sortNovelList()
    }

    // 排序小说列表
    function sortNovelList() {
      if (sortType.value === 'popularity') {
        return // 人气排序不处理
      }

      novel.list.sort((a, b) => {
        let valueA, valueB

        if (sortType.value === 'words') {
          // 字数排序 - 将字符串转换为数字进行比较
          valueA = parseInt(a.totalWords?.replace(/[^\d]/g, '') || '0')
          valueB = parseInt(b.totalWords?.replace(/[^\d]/g, '') || '0')
        } else if (sortType.value === 'time') {
          // 时间排序 - 将时间字符串转换为Date对象进行比较
          valueA = new Date(a.updateTime || '1970-01-01')
          valueB = new Date(b.updateTime || '1970-01-01')
        }

        // 根据排序方向返回比较结果
        if (sortOrder.value === 'asc') {
          return valueA > valueB ? 1 : valueA < valueB ? -1 : 0
        } else {
          return valueA < valueB ? 1 : valueA > valueB ? -1 : 0
        }
      })
    }

    // 切换排序方向
    function toggleSortOrder() {
      if (sortType.value !== 'popularity') {
        sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
        sortNovelList()
      }
    }

    function resetList() {
      novel.list = []
      novel.currentPage = 0
      novel.disabled = false
      loadArtworks()
    }

    // 搜索处理
    function handleSearch() {
      resetList()
    }

    // 监听切换回收站列表
    watch(isRecycle, () => {
      resetList()
    })

    return {
      novel,
      selectedTag,
      sortType,
      sortOrder,
      searchKeyword,
      loadArtworks,
      toNovelContent,
      toUploadChapter,
      deleteNovel,
      restoreNovel,
      selectTag,
      setSortType,
      sortNovelList,
      toggleSortOrder,
      handleSearch,
      isRecycle,
      toggleRecycle,
      setRecycle
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
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
  backdrop-filter: blur(10px);
}

/* 搜索区域 */
.search-section {
  margin-bottom: 20px;
}

.search-section :deep(input::placeholder) {
  font-family: 'Blueaka', sans-serif;
}

.search-section ::v-deep(input::-webkit-input-placeholder) {
  font-family: 'Blueaka', sans-serif;
}

.search-input {
  border-radius: 12px;
}

/* 筛选区域 */
.filter-section {
  margin-bottom: 24px;
}

.filter-section:last-child {
  margin-bottom: 0;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
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
  color: #909399;
  padding: 0;
  font-size: 12px;
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
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.tag-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
  border-color: #409eff;
}

.tag-item.active {
  background: #409eff !important;
  color: white;
  border-color: #409eff !important;
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
  padding: 18px 24px 16px;
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
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.novel-table-container ::v-deep(.cell) {
  font-family: 'Blueaka', sans-serif;
  padding: 0 8px;
}

/* 表格样式 */
.novel-table {
  width: 100%;
}

/* 表格单元格样式 */
.title-cell {
  padding: 8px 0;
  font-family: 'Blueaka', sans-serif;
}

.novel-title {
  font-size: 16px;
  font-weight: 400;
  color: #303133;
  cursor: pointer;
  transition: color 0.3s ease;
}

.novel-title:hover {
  color: #409eff;
}

.tags-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px 0;
}

.tag-item {
  font-size: 12px;
  border-radius: 4px;
}

.author-name {
  font-size: 14px;
  color: #606266;
}

.word-count {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.update-time {
  font-size: 12px;
  color: #909399;
}

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
}

.action-buttons .el-button {
  transition: all 0.3s ease;
  color: #909399;
  width: 24px;
  height: 24px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.el-button+.el-button {
  margin-left: 0;
}

.action-buttons .el-button .icon {
  width: 16px;
  height: 16px;
}

.action-buttons .el-button:hover {
  transform: translateY(-1px);
  color: #409eff;
}

.action-buttons .no-margin {
  margin-left: 6px;
}

/* 加载状态 */
.loading-container {
  padding: 24px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .sidebar {
    width: 280px;
  }
}

@media (max-width: 992px) {
  .sidebar {
    width: 260px;
  }

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
    flex-direction: column;
    padding: 84px 16px 16px;
  }

  .sidebar {
    width: 100%;
    position: relative;
    margin-bottom: 20px;
  }

  .sidebar .el-affix {
    position: static !important;
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

  .stats {
    flex-direction: column;
    gap: 8px;
  }

  /* 隐藏部分表格列 */
  :deep(.el-table .el-table__cell:nth-child(4)),
  :deep(.el-table .el-table__header .el-table__cell:nth-child(4)) {
    display: none;
  }
}

@media (max-width: 640px) {

  /* 隐藏更多表格列 */
  :deep(.el-table .el-table__cell:nth-child(3)),
  :deep(.el-table .el-table__header .el-table__cell:nth-child(3)),
  :deep(.el-table .el-table__cell:nth-child(6)),
  :deep(.el-table .el-table__header .el-table__cell:nth-child(6)) {
    display: none;
  }
}

@media (max-width: 480px) {
  .novel-page {
    padding: 84px 12px 12px;
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

  /* 只显示关键列 */
  :deep(.el-table .el-table__cell:nth-child(5)),
  :deep(.el-table .el-table__header .el-table__cell:nth-child(5)) {
    display: none;
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

:deep(.el-table .el-tag) {
  margin-right: 4px;
  margin-bottom: 2px;
}

/* 搜索框样式覆盖 */
:deep(.search-input .el-input__inner) {
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
}

:deep(.search-input .el-input__inner:focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

/* 搜索框图标样式 */
:deep(.search-input .el-input__prefix) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.search-input .el-input__prefix .icon) {
  margin-left: -4px;
  width: 20px;
  height: 20px;
  color: #909399;
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
  background: #fafafa;
  border-color: #e4e7ed;
}

:deep(.tag-item.el-tag--plain:hover) {
  background: #f0f9ff;
  border-color: #409eff;
}

/* 分割线样式 */
:deep(.el-divider--horizontal) {
  margin: 20px 0;
  border-color: #f0f0f0;
}
</style>
