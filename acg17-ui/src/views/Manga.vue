<template>
  <section>
    <div class="side-btn-group left-btn-group" :style="{ right: '50%', marginRight: (containerWidth / 2 + 30) + 'px' }">
      <div class="side-btn" @click="randomManga">
        <icon icon="#icon-random"></icon>
      </div>
      <div class="side-btn" :class="{ 'active': tag.showTagList }" @click="openTagList">
        <icon icon="#icon-tag"></icon>
      </div>
      <div class="side-btn" v-show="showBackToTop" @click="scrollToTop">
        <icon icon="#icon-sort-asc"></icon>
      </div>
    </div>
    <div class="side-btn right-btn" v-show="showBackToTop" @click="scrollToTop" :style="{ left: '50%', marginLeft: (containerWidth / 2 + 30) + 'px' }">
      <icon icon="#icon-sort-asc"></icon>
    </div>

    <div class="tag-container" :class="{ 'is-visible': tag.showTagList }">
      <div class="tag-panel">
        <div class="tag-header">
          <div class="tag-title">标签筛选</div>
          <button class="clear-tags-btn" v-if="hasActiveTag" @click="clearTagFilter">清除筛选</button>
        </div>
        <div class="tag-body">
          <div class="info-row" v-if="tag.artistTags.length">
            <span class="label">艺术家:</span>
            <div class="tags-container">
              <span v-for="item in getVisibleTags(tag.artistTags, 'artist')" :key="'artist-' + item.tagId" class="tag" :class="{ active: isTagActive(item.tagId) }" @click="searchByTag(item.tagId)">
                {{ item.tagName }} <span class="tag-count">{{ item.tagCount }}</span>
              </span>
              <button class="expand-tag-btn" v-if="hasHiddenTags(tag.artistTags, 'artist')" @click="toggleExpand('artist')">{{ tag.expand.artist ? '收起' : '展开' }}</button>
            </div>
          </div>
          <div class="info-row" v-if="tag.characterTags.length">
            <span class="label">角色:</span>
            <div class="tags-container">
              <span v-for="item in getVisibleTags(tag.characterTags, 'character')" :key="'character-' + item.tagId" class="tag" :class="{ active: isTagActive(item.tagId) }" @click="searchByTag(item.tagId)">
                {{ item.tagName }} <span class="tag-count">{{ item.tagCount }}</span>
              </span>
              <button class="expand-tag-btn" v-if="hasHiddenTags(tag.characterTags, 'character')" @click="toggleExpand('character')">{{ tag.expand.character ? '收起' : '展开' }}</button>
            </div>
          </div>
          <div class="info-row" v-if="tag.maleTags.length">
            <span class="label">男性:</span>
            <div class="tags-container">
              <span v-for="item in getVisibleTags(tag.maleTags, 'male')" :key="'male-' + item.tagId" class="tag" :class="{ active: isTagActive(item.tagId) }" @click="searchByTag(item.tagId)">
                {{ item.tagName }} <span class="tag-count">{{ item.tagCount }}</span>
              </span>
              <button class="expand-tag-btn" v-if="hasHiddenTags(tag.maleTags, 'male')" @click="toggleExpand('male')">{{ tag.expand.male ? '收起' : '展开' }}</button>
            </div>
          </div>
          <div class="info-row" v-if="tag.femaleTags.length">
            <span class="label">女性:</span>
            <div class="tags-container">
              <span v-for="item in getVisibleTags(tag.femaleTags, 'female')" :key="'female-' + item.tagId" class="tag" :class="{ active: isTagActive(item.tagId) }" @click="searchByTag(item.tagId)">
                {{ item.tagName }} <span class="tag-count">{{ item.tagCount }}</span>
              </span>
              <button class="expand-tag-btn" v-if="hasHiddenTags(tag.femaleTags, 'female')" @click="toggleExpand('female')">{{ tag.expand.female ? '收起' : '展开' }}</button>
            </div>
          </div>
          <div class="info-row" v-if="tag.mixedTags.length">
            <span class="label">混合:</span>
            <div class="tags-container">
              <span v-for="item in getVisibleTags(tag.mixedTags, 'mixed')" :key="'mixed-' + item.tagId" class="tag" :class="{ active: isTagActive(item.tagId) }" @click="searchByTag(item.tagId)">
                {{ item.tagName }} <span class="tag-count">{{ item.tagCount }}</span>
              </span>
              <button class="expand-tag-btn" v-if="hasHiddenTags(tag.mixedTags, 'mixed')" @click="toggleExpand('mixed')">{{ tag.expand.mixed ? '收起' : '展开' }}</button>
            </div>
          </div>
          <div class="info-row" v-if="tag.otherTags.length">
            <span class="label">其他:</span>
            <div class="tags-container">
              <span v-for="item in getVisibleTags(tag.otherTags, 'other')" :key="'other-' + item.tagId" class="tag" :class="{ active: isTagActive(item.tagId) }" @click="searchByTag(item.tagId)">
                {{ item.tagName }} <span class="tag-count">{{ item.tagCount }}</span>
              </span>
              <button class="expand-tag-btn" v-if="hasHiddenTags(tag.otherTags, 'other')" @click="toggleExpand('other')">{{ tag.expand.other ? '收起' : '展开' }}</button>
            </div>
          </div>
          <div class="info-row" v-if="tag.originalTags.length">
            <span class="label">原作:</span>
            <div class="tags-container">
              <span v-for="item in getVisibleTags(tag.originalTags, 'original')" :key="'original-' + item.tagId" class="tag" :class="{ active: isTagActive(item.tagId) }" @click="searchByTag(item.tagId)">
                {{ item.tagName }} <span class="tag-count">{{ item.tagCount }}</span>
              </span>
              <button class="expand-tag-btn" v-if="hasHiddenTags(tag.originalTags, 'original')" @click="toggleExpand('original')">{{ tag.expand.original ? '收起' : '展开' }}</button>
            </div>
          </div>
          <div class="empty-tags" v-if="!hasAnyTags">暂无可用标签</div>
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
    <acg17-loading-heart v-show="manga.loading"></acg17-loading-heart>
  </section>
  <acg17-footer v-if="manga.disabled"></acg17-footer>
</template>

<script>
import { reactive, onMounted, onUnmounted, watch, ref, nextTick, computed } from 'vue'
// import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router';
import server from '@/util/request';

import LoadingHeart from "../components/LoadingHeart";
import Acg17Footer from "../components/Acg17Footer";
import { useRecycleState } from '@/composables/useRecycleState';
import { ElMessage } from "element-plus";

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
    let resizeObserver = null

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
      manga.loading = true
      manga.disabled = true

      // 构建查询参数
      const params = {
        pageNum: ++manga.currentPage,
        deleted: isRecycle.value
      }

      if (route.query.tagId) {
        params.tagId = route.query.tagId
      }

      server.get('/manga/list', {
        params: params
      })
        .then(response => {
          // records.length!==0：当前页非空页，可能存在下一页，对当前页数据进行下一步处理
          // records.length===0：当前页为空页，不存在下一页，置disabled=true，不再请求下一页
          if (response.data.records.length !== 0) {
            manga.list.push(...response.data.records)
            manga.disabled = false
          } else {
            manga.disabled = true
          }
          manga.loading = false
        })
        .catch(err => {
          console.log(err)
          manga.loading = false
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
      artistTags: [], // 艺术家标签
      characterTags: [], // 角色标签
      maleTags: [], // 男性标签
      femaleTags: [], // 女性标签
      mixedTags: [], // 混合标签
      otherTags: [], // 其他标签
      originalTags: [], // 原作标签
      expand: {
        artist: false,
        character: true,
        male: true,
        female: false,
        mixed: true,
        other: true,
        original: true
      }
    })

    // 打开/关闭标签列表
    function openTagList() {
      tag.showTagList = !tag.showTagList
      scrollToTop()
      // 点击标签列表时，关闭搜索框
      if (tag.showTagList) {
        loadTagList()
      }
    }

    function loadTagList() {
      server.get('/manga-tag/list', {
        params: { deleted: isRecycle.value }
      })
        .then(res => {
          tag.artistTags = res.data.artistTags || []
          tag.characterTags = res.data.characterTags || []
          tag.maleTags = res.data.maleTags || []
          tag.femaleTags = res.data.femaleTags || []
          tag.mixedTags = res.data.mixedTags || []
          tag.otherTags = res.data.otherTags || []
          tag.originalTags = res.data.originalTags || []
        })
        .catch(err => {
          ElMessage.error('获取漫画标签失败【' + err + '】，请重试')
        })
    }

    const hasActiveTag = computed(() => {
      return !!route.query.tagId
    })

    const hasAnyTags = computed(() => {
      return tag.artistTags.length || tag.characterTags.length || tag.maleTags.length || tag.femaleTags.length || tag.mixedTags.length || tag.otherTags.length || tag.originalTags.length
    })

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

    function isTagActive(tagId) {
      return String(route.query.tagId || '') === String(tagId)
    }

    function searchByTag(tagId) {
      router.push({
        path: '/acg/manga',
        query: { tagId }
      })
    }

    function clearTagFilter() {
      router.push({
        path: '/acg/manga',
        query: {}
      })
    }

    function scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: "smooth"
      })
    }

    // 监听回收站状态变化，重新获取数据
    watch(isRecycle, () => {
      manga.list = []
      manga.currentPage = 0
      manga.disabled = false
      loadManga()
      if (tag.showTagList) loadTagList()
    })

    // 监听路由查询参数变化，重新获取数据
    watch(() => route.query, () => {
      manga.list = []
      manga.currentPage = 0
      manga.disabled = false
      loadManga()
    }, { deep: true })

    // 组件挂载时获取数据
    onMounted(() => {
      window.addEventListener('scroll', handleScroll)
      loadManga()
      nextTick(() => {
        updateWidth()
        window.addEventListener('resize', updateWidth)
        resizeObserver = new ResizeObserver(() => updateWidth())
        const container = document.querySelector('.manga-container')
        if (container) resizeObserver.observe(container)
      })
    })

    onUnmounted(() => {
      window.removeEventListener('scroll', handleScroll)
      window.removeEventListener('resize', updateWidth)
      if (resizeObserver) resizeObserver.disconnect()
    })

    return { manga, goToMangaDetail, isRecycle, toggleRecycle, setRecycle, loadManga, randomManga, scrollToTop, showBackToTop, containerWidth, tag, openTagList, sortedTags, isTagActive, searchByTag, hasActiveTag, clearTagFilter, hasAnyTags, getVisibleTags, hasHiddenTags, toggleExpand }
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

.tag-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
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
  padding: 4px 8px;
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

.tag.active {
  background-color: #e3f2fd;
  border-color: #90caf9;
  color: #1976d2;
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
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
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
