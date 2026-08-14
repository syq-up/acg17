<template>
  <section class="manga-detail">
    <div class="side-btn-group left-btn-group" :style="{ right: '50%', marginRight: (containerWidth / 2 + 30) + 'px' }">
      <div class="side-btn" @click="randomManga">
        <icon icon="#icon-random"></icon>
      </div>
      <div class="side-btn" @click="goBack">
        <icon icon="#icon-left"></icon>
      </div>
      <div class="side-btn" v-show="showBackToTopLeft" @click="scrollToTop">
        <icon icon="#icon-sort-asc"></icon>
      </div>
    </div>

    <div class="side-btn right-btn" v-show="showBackToTopRight" @click="scrollToTop" :style="{ left: '50%', marginLeft: (containerWidth / 2 + 30) + 'px' }">
      <icon icon="#icon-sort-asc"></icon>
    </div>

    <div class="manga-container">
      <!-- 漫画封面 -->
      <div class="manga-cover unselectable">
        <img :src="manga.cover" :alt="manga.title" />
      </div>

      <!-- 漫画信息 -->
      <div class="manga-info">
        <h1 class="manga-title">{{ manga.title }} ✨ {{ manga.chineseTitle }}</h1>

        <div v-for="group in tagGroups" :key="group.category" class="info-row unselectable">
          <span class="label">{{ group.name }}:</span>
          <div class="tags-container">
            <span
              v-for="tag in group.tags"
              :key="tag.tagId"
              class="tag"
              @click="searchByTag(tag.tagId)"
            >
              {{ tag.tagName }} <span class="tag-count">{{ tag.tagCount }}</span>
            </span>
            <button
              class="edit-tag-btn"
              type="button"
              :title="`编辑${group.name}标签`"
              :disabled="!manga.id"
              @click="openTagEditor(group.category)"
            >
              <icon icon="#icon-edit" class="edit-icon"></icon>
            </button>
          </div>
        </div>

        <div class="info-row unselectable">
          <span class="label">页数:</span>
          <span class="value">{{ manga.pageCount || 0 }}</span>
        </div>

        <div class="info-row unselectable" v-if="manga.updateTime">
          <span class="label">更新时间:</span>
          <span class="value">{{ manga.updateTime }}</span>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons unselectable">
          <button class="btn-primary" @click="startReading">
            开始阅读
          </button>
          <button :class="manga.favorite ? 'btn-icon favorite' : 'btn-icon'" @click="toggleFavorite"
            :title="manga.favorite ? '取消喜欢' : '添加喜欢'">
            <icon :icon="manga.favorite ? '#icon-favorite-y' : '#icon-favorite-n'" class="icon-svg"></icon>
          </button>
          <button :class="manga.deleted ? 'btn-icon restore' : 'btn-icon'" @click="toggleDeleteStatus"
            :title="manga.deleted ? '恢复漫画' : '删除漫画'">
            <icon :icon="manga.deleted ? '#icon-restore' : '#icon-delete'" class="icon-svg"></icon>
          </button>
          <button class="btn-icon" title="下载">
            <icon icon="#icon-download" class="icon-svg"></icon>
          </button>
        </div>
      </div>
    </div>

    <!-- 漫画描述 -->
    <!-- <div class="manga-description">
      <h3>简介</h3>
      <p>{{ manga.description || '这是一部精彩的漫画作品，讲述了一个引人入胜的故事...' }}</p>
    </div> -->

    <!-- 漫画章节列表 -->
    <div class="manga-chapters unselectable" v-if="mangaChapters.length > 1">
      <h3>章节列表</h3>
      <div class="chapters-simple-list">
        <div v-for="chapterItem in mangaChapters" :key="'c' + chapterItem.chapter"
          :class="['chapter-simple-item', { 'active': manga.currentChapter === chapterItem.chapter }]"
          @click="goToChapter(chapterItem.chapter)">
          {{ chapterItem.title || `第${chapterItem.chapter}话` }}
        </div>
      </div>
    </div>

    <!-- 漫画页面缩略图 -->
    <div class="manga-pages unselectable">
      <h3>漫画页面</h3>
      <ul class="pages-list">
        <li v-for="pageItem in mangaPages" :key="'p' + pageItem.page" class="page-item"
          @click="goToPage(pageItem.page)">
          <img :src="pageItem.path" :alt="`第${pageItem.page}页`" />
          <div class="page-remove" @click.stop="removeMangaPage(pageItem.page)">×</div>
          <div class="page-number">{{ pageItem.page }}</div>
        </li>
      </ul>
    </div>

    <!-- 相关推荐 -->
    <div class="related-manga unselectable">
      <h3>相关推荐</h3>
      <div class="related-list">
        <div v-for="item in relatedManga" :key="item.id" class="related-item" @click="goToManga(item.id)">
          <img :src="item.cover" :alt="item.title" />
          <div class="related-title">{{ item.title }}</div>
        </div>
      </div>
    </div>
  </section>

  <manga-tag-editor
    v-if="activeTagCategory"
    :manga-id="manga.id"
    :category="activeTagCategory"
    :current-tags="activeTagEditorTags"
    @close="closeTagEditor"
    @updated="loadMangaDetail"
  />

  <acg17-footer></acg17-footer>
</template>

<script>
import { computed, reactive, onMounted, onUnmounted, ref, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Acg17Footer from '../components/Acg17Footer'
import MangaTagEditor from '../components/manga-detail/MangaTagEditor.vue'
import server from '@/util/request';
import { ElMessage } from 'element-plus';

const TAG_CATEGORIES = [
  { category: 'artist', key: 'artistTags', name: '艺术家' },
  { category: 'character', key: 'characterTags', name: '角色' },
  { category: 'male', key: 'maleTags', name: '男性' },
  { category: 'female', key: 'femaleTags', name: '女性' },
  { category: 'mixed', key: 'mixedTags', name: '混合' },
  { category: 'other', key: 'otherTags', name: '其他' },
  { category: 'original', key: 'originalTags', name: '原作' },
]

export default {
  name: 'MangaDetail',
  components: {
    'acg17-footer': Acg17Footer,
    MangaTagEditor,
  },
  setup() {
    const route = useRoute()
    const router = useRouter()

    const manga = reactive({
      id: null,
      title: '',
      chineseTitle: '',
      cover: '',
      description: '',
      pages: [],
      characterTags: [],
      maleTags: [],
      femaleTags: [],
      mixedTags: [],
      otherTags: [],
      originalTags: [],
      artistTags: [],
      favorite: false,
      deleted: false,
      updateTime: '',
      pageCount: 0,
      currentChapter: 0,
    })

    const relatedManga = reactive([])

    // 漫画页面数据直接使用后端返回的pages
    const mangaPages = reactive([])

    // 章节列表数据
    const mangaChapters = reactive([])

    const activeTagCategory = ref('')
    const tagGroups = computed(() => TAG_CATEGORIES.map(group => ({
      ...group,
      tags: [...(manga[group.key] || [])]
        .sort((a, b) => (b.tagCount || 0) - (a.tagCount || 0)),
    })))
    const activeTagEditorTags = computed(() => (
      tagGroups.value.find(group => group.category === activeTagCategory.value)?.tags || []
    ))

    const containerWidth = ref(1380)
    const showBackToTopLeft = ref(false)
    const showBackToTopRight = ref(false)
    let resizeObserver = null

    const handleScroll = () => {
      showBackToTopLeft.value = window.scrollY > 50
      showBackToTopRight.value = window.scrollY > 500
    }

    const updateWidth = () => {
      const container = document.querySelector('.manga-container')
      if (container) {
        containerWidth.value = container.clientWidth
      }
    }

    // 从后端加载漫画详情数据
    async function loadMangaDetail() {
      try {
        const mangaId = parseInt(route.params.id)
        const res = await server.get(`/manga/${mangaId}`)

        if (res.code === 200) {
          // 如果res.data为空，则进入404页面
          if (!res.data) {
            router.push('/404')
            return
          }
          const mangaData = res.data
          Object.assign(manga, mangaData)

          // 计数页数
          let pageCount = 0
          if (mangaData.pages && mangaData.pages.length > 0) {
            mangaData.pages.forEach(chapter => {
              if (chapter.pagelist && chapter.pagelist.length > 0) {
                chapter.pageCount = chapter.pagelist.length
                pageCount += chapter.pageCount
              }
            })
          }
          manga.pageCount = pageCount

          // 设置漫画章节数据
          mangaChapters.length = 0
          if (mangaData.pages && mangaData.pages.length > 0) {
            mangaChapters.push(...mangaData.pages)

            // 设置漫画页面数据
            mangaPages.length = 0
            if (mangaChapters[0].pagelist && mangaChapters[0].pagelist.length > 0) {
              mangaPages.push(...mangaChapters[0].pagelist)
              manga.currentChapter = mangaChapters[0].chapter
              manga.cover = mangaPages[0].path
            }
          }

        } else {
          console.error('获取漫画详情失败:', res.message)
        }
      } catch (error) {
        console.error('加载漫画详情时发生错误:', error)
      }
    }

    // 加载相关推荐漫画
    async function loadRelatedManga() {
      try {
        const response = await server.get('/manga/list', {
          params: {
            pageNum: 1,
            deleted: false
          }
        })

        if (response.code === 200 && response.data && response.data.records) {
          // 取前4个作为相关推荐，排除当前漫画
          const currentMangaId = parseInt(route.params.id)
          const filteredManga = response.data.records
            .filter(item => item.id !== currentMangaId)
            .slice(0, 4)

          relatedManga.length = 0
          relatedManga.push(...filteredManga)
        }
      } catch (error) {
        console.error('加载相关推荐时发生错误:', error)
      }
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
      loadMangaDetail()
      loadRelatedManga()
      nextTick(() => {
        updateWidth()
        window.addEventListener('resize', updateWidth)
        resizeObserver = new ResizeObserver(() => updateWidth())
        const container = document.querySelector('.manga-container')
        if (container) resizeObserver.observe(container)
      })
    })

    watch(() => route.params.id, () => {
      closeTagEditor()
      loadMangaDetail()
      loadRelatedManga()
    })

    onUnmounted(() => {
      window.removeEventListener('scroll', handleScroll)
      window.removeEventListener('resize', updateWidth)
      if (resizeObserver) resizeObserver.disconnect()
    })

    async function toggleFavorite() {
      try {
        const newFavoriteStatus = !manga.favorite
        const res = await server.put(`/manga/${manga.id}/favorite?favorite=${newFavoriteStatus}`)
        if (res.code === 200) {
          manga.favorite = !manga.favorite
        }
      } catch (error) {
        console.error('收藏操作失败:', error)
      }
    }

    async function toggleDeleteStatus() {
      try {
        if (manga.deleted) {
          // 恢复漫画
          const res = await server.put(`/manga/${manga.id}/restore`)
          if (res.code === 200) {
            manga.deleted = false
            ElMessage.success('漫画已恢复')
          } else {
            ElMessage.error('恢复漫画失败')
          }
        } else {
          // 删除漫画
          const res = await server.delete(`/manga/${manga.id}`)
          if (res.code === 200) {
            manga.deleted = true
            ElMessage.success('漫画已删除')
          } else {
            ElMessage.error('删除漫画失败')
          }
        }
      } catch (error) {

        console.error('删除/恢复操作失败:', error)
      }
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

      if (chapterItem && chapterItem.pagelist && chapterItem.pagelist.length > 0) {
        manga.currentChapter = chapterItem.chapter
        mangaPages.length = 0
        mangaPages.push(...chapterItem.pagelist)
        // 跳转到章节的第一页
        // router.push({
        //   path: `/acg/manga/${manga.id}/${manga.currentChapter}/1`,
        //   state: {
        //     mangaData: {
        //       id: manga.id,
        //       currentChapter: manga.currentChapter,
        //       title: manga.title,
        //       pages: [...mangaPages]
        //     }
        //   }
        // })
      }
    }

    // 根据标签搜索漫画
    function searchByTag(tagId) {
      router.push({
        path: '/acg/manga',
        query: { tagId }
      })
    }

    // 标签编辑器仅保存当前打开的分类，其余状态由编辑器组件管理
    function openTagEditor(category) {
      activeTagCategory.value = category
    }

    function closeTagEditor() {
      activeTagCategory.value = ''
    }

    // 删除漫画页
    async function removeMangaPage(pageNum) {
      try {
        const res = await server.delete(`/manga/delete/page`, {
          params: {
            mangaId: manga.id,
            chapterId: manga.currentChapter,
            pageNum: pageNum
          }
        })
        if (res.code === 200) {
          const index = mangaPages.findIndex(page => page.page === pageNum)
          if (index !== -1) {
            mangaPages.splice(index, 1)
          }
        }
      } catch (error) {
        console.error('删除漫画页失败:', error)
      }
    }

    return {
      manga,
      relatedManga,
      mangaPages,
      mangaChapters,
      tagGroups,
      activeTagCategory,
      activeTagEditorTags,
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
      containerWidth,
      openTagEditor,
      closeTagEditor,
      loadMangaDetail,
      removeMangaPage
    }
  }
}
</script>

<style scoped>
.manga-detail {
  max-width: 1380px;
  margin: 80px auto 20px;
  padding: 20px;
  background-color: #ffffff;
  min-height: calc(100vh - 140px - 20px - 200px);
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

.side-btn svg, .side-btn .icon {
  width: 24px;
  height: 24px;
  fill: currentColor;
}

.left-btn-group {
  position: fixed;
  top: 84px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.right-btn {
  position: fixed;
  bottom: 50px;
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
  width: 350px;
  height: auto;
  object-fit: cover;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.manga-info {
  flex: 1;
  padding: 20px 0 70px;
  height: auto;
  position: relative;
}

.manga-title {
  font-size: 28px;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 20px;
  line-height: 1.4;
}

.info-row {
  display: flex;
  margin-bottom: 12px;
}

.label {
  font-weight: 600;
  color: #495057;
  margin-right: 12px;
}

.value {
  color: #495057;
  flex: 1;
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

.category {
  background-color: #e3f2fd;
  color: #1976d2;
  padding: 4px 12px;
  border-radius: 15px;
  font-size: 14px;
  display: inline-block;
}

.status.completed {
  color: #4caf50;
  font-weight: 600;
}

.status.ongoing {
  color: #ff9800;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 15px;
  bottom: 10px;
  position: absolute;
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

.btn-secondary {
  background-color: transparent;
  color: #409eff;
  border: 2px solid #409eff;
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-secondary:hover {
  background-color: #409eff;
  color: white;
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

/* .manga-description {
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
} */

.manga-pages {
  margin-bottom: 40px;
}

.manga-pages h3 {
  font-size: 20px;
  color: #2c3e50;
  margin-bottom: 20px;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.pages-list {
  display: grid;
  grid-template-columns: repeat(6, 220px);
  gap: 12px;
  list-style: none;
  padding: 0;
  margin: 0;
}

.page-item {
  position: relative;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.page-item:hover {
  transform: translateY(-5px);
}

.page-item img {
  width: 220px;
  height: 310px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.page-remove {
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
  display: none;
}

.page-item:hover .page-remove {
  display: block;
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
@media screen and (max-width: 768px) {
  .manga-detail {
    margin: 20px auto;
    padding: 15px;
  }

  .manga-container {
    flex-direction: column;
    gap: 20px;
  }

  .manga-cover img {
    width: 100%;
    max-width: 250px;
    height: 350px;
    margin: 0 auto;
    display: block;
  }

  .manga-title {
    font-size: 24px;
  }

  .action-buttons {
    flex-wrap: wrap;
  }

  .related-list {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 15px;
  }

  .related-item img {
    height: 160px;
  }

  .pages-list {
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
  }

  .page-item img {
    width: 100%;
    height: 200px;
  }
}

@media screen and (max-width: 480px) {
  .pages-list {
    grid-template-columns: repeat(2, 1fr);
  }

  .page-item img {
    height: 180px;
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

.chapters-simple-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.chapter-simple-item {
  padding: 8px 16px;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  background-color: #fff;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #333;
  font-weight: 500;
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
  margin-left: 8px;
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

</style>
