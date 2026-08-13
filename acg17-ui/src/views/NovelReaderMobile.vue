<template>
  <div class="page-header" :style="menu.show ? 'top: 0;' : ''">
    <div class="item" @click="$router.back(-1)">
      <icon icon="#icon-left"></icon>
    </div>
    <div class="menu">
      <div class="item" @click="openUpdatePanel">
        <icon icon="#icon-edit"></icon>
      </div>
      <div class="item">
        <icon icon="#icon-other"></icon>
      </div>
    </div>
  </div>
  <div class="read-wrap" @click="handleClick">
    <div class="top">
      <h1 v-text="chapter.content.title"></h1>
    </div>
    <article>
      <section>
        <h3 class="title" v-text="chapter.content.title"></h3>
        <div class="content">
          <p v-for="(paragraph, index) in chapter.content.content" :key="index" class="paragraph">
            {{ paragraph }}
          </p>
        </div>
        <div class="control" :style="chapter.loading ? 'visibility: hidden;' : ''">
          <a :class="chapter.currentChapterIndex===0 ? 'disabled' : ''" @click="changeChapter(-1)">上一章</a>
          <span></span>
          <a>目录</a>
          <span></span>
          <a :class="chapter.currentChapterIndex===chapter.chapterList.length-1 ? 'disabled' : ''" @click="changeChapter(1)">下一章</a>
        </div>
      </section>
    </article>
  </div>
  <footer class="footer" :style="menu.show ? 'bottom: 0;' : ''">
    <div :class="'item '+(menu.subMenu===1 ? 'on' : '')" @click="menu.subMenu=1">
      <icon icon="#icon-catalog"></icon>
      <span>目录</span>
    </div>
    <div :class="'item '+(menu.subMenu===2 ? 'on' : '')" @click="menu.subMenu=2">
      <icon icon="#icon-progress"></icon>
      <span>进度</span>
    </div>
    <div :class="'item '+(menu.subMenu===3 ? 'on' : '')" @click="menu.subMenu=3">
      <icon icon="#icon-setting"></icon>
      <span>设置</span>
    </div>
    <div :class="'item '+(menu.subMenu===4 ? 'on' : '')" @click="menu.subMenu=4">
      <icon icon="#icon-night"></icon>
      <span>夜间</span>
    </div>
  </footer>

  <div :class="'shadow ' + (chapter.loading ? 'active' : '')" style="display: flex;flex-direction: column;justify-content: center;">
    <acg17-loading-heart></acg17-loading-heart>
  </div>

  <aside :class="'shadow ' + (menu.subMenu===1 ? 'active' : '')" @click="menu.subMenu=0">
    <div class="catalog-wrap" @click.stop>
      <h3 class="title">目录</h3>
      <div id="catalog" class="catalog" @touchstart="handleTouchstart" @touchmove="handleTouchmove" @touchend="handleTouchend">
        <div class="catalog-header">
          <h4 class="left">共{{ chapter.chapterList.length }}章</h4>
          <a class="right">倒序</a>
        </div>
        <ul class="catalog-main">
          <li v-for="(item, i) in chapter.chapterList" :key="'chapterList-'+item.id" @click="toChapter(item.id, i)"
              :class="i === chapter.currentChapterIndex ? 'on' : ''">{{ item.title }}</li>
        </ul>
      </div>
    </div>
  </aside>

  <el-dialog v-model="update.show" title="更新章节" width="90%">
    <el-input v-model="update.title" placeholder="章节名" />
    <div style="margin-bottom: 16px;"></div>
    <el-input v-model="update.content" :rows="10" type="textarea" placeholder="章节内容" />
    <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeUpdatePanel">取消</el-button>
          <el-button type="primary" @click="updateChapter">确定</el-button>
        </span>
    </template>
  </el-dialog>
</template>

<script>
import { onBeforeMount, reactive, onBeforeUnmount } from "vue";
import { useRoute, useRouter } from "vue-router";
import server from '@/util/request';
import LoadingHeart from "../components/LoadingHeart";
import { ElMessage } from "element-plus";

export default {
  name: "NovelReaderMobile",
  components: {
    'acg17-loading-heart': LoadingHeart,
  },
  setup() {
    const route = useRoute()
    const router = useRouter()

    const chapter = reactive({
      novel: {},
      chapterList: [{}],
      content: {},
      currentChapterIndex: 0,
      loading: false,
    })
    // 加载小说，和第一章内容
    onBeforeMount(()=>{
      // 头部、底部浮层菜单，默认不显示
      document.getElementById('header').style.top = '-64px'
      // 显示loading图标
      chapter.loading = true
      // 加载小说、章节、和第一章的内容
      server.get('/novel/getContentById/'+route.params.novelId)
        .then(res=>{
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
        .catch(err=>{
          chapter.loading = false
          console.log(err)
        })
    })
    // 切换章节（上一章、下一章）
    function changeChapter(change) {
      // 第一章不能跳上一章，最后一章不能跳下一章
      if (chapter.currentChapterIndex === 0 && change === -1) {
        ElMessage.warning("已经是第一章了。")
        return
      }
      if (chapter.currentChapterIndex === chapter.chapterList.length-1 && change === 1) {
        ElMessage.warning("已经是最后一章了。")
        return
      }
      // 清除当前章节内容
      chapter.content = {}
      // 加载新章节内容
      chapter.currentChapterIndex = chapter.currentChapterIndex + change
      getChapter(chapter.chapterList[chapter.currentChapterIndex].id)
    }
    // 跳转章节
    function toChapter(id, i) {
      chapter.currentChapterIndex = i
      // 关闭浮层
      handleClick()
      getChapter(id)
    }
    // 获取章节内容
    function getChapter(id) {
      chapter.loading = true
      server.get('/novel-chapter/getContentById/'+id)
          .then(res=>{
            chapter.loading = false
            chapter.content = res.data
          })
          .catch(err=>{
            chapter.loading = false
            console.log(err)
          })
    }

    // 上下菜单栏
    const menu = reactive({
      show: false, // 是否显示
      subMenu: 0, // 当前激活的子菜单
    })
    // 屏幕高度
    const screenHeight = screen.height
    // 屏幕点击事件（上方向前翻页，中间打开/关闭浮层菜单，下方向后翻页）
    function handleClick(e) {
      if (menu.show) {
        menu.subMenu = 0
        menu.show = false
      } else {
        if (e.y < screenHeight*0.3) {
          document.documentElement.scrollTop -= screenHeight*0.65
        } else if (e.y > screenHeight*0.6) {
          document.documentElement.scrollTop += screenHeight*0.65
        } else {
          menu.show = true
        }
      }
    }

    // 更新章节
    const update = reactive({
      show: false,
      title: '',
      content: '',
    })
    function openUpdatePanel() {
      update.show = true
      update.title = chapter.content.title
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
        content: update.content.split('\n\n').filter(p => p.trim() !== ''),
      }
      server.post('/novel-chapter/updateChapter', data)
          .then(()=>{
            ElMessage.success(`更新章节「${data.title}」成功。`)
            chapter.content.title = update.title
            chapter.content.content = data.content
            closeUpdatePanel()
          })
          .catch(err=>{
            console.log(err)
          })
    }

    // 目录浮层，解决滚动穿透问题
    let catalog = {
      touchstartY: 0, // 目录浮层，滑动事件开始位置
      cancelable: true, // 是否可取消当前滑动事件（在单次滑动过程中达到顶部和底部时，不取消滑动的默认事件）
      e: null, // 可滚动区域（类型为 HTMLElement 对象）
    }
    function handleTouchstart(e) {
      catalog.e = document.getElementById('catalog')
      catalog.touchstartY = e.touches[0].clientY
    }
    function handleTouchmove(e) {
      if ((catalog.e.scrollTop === 0 && e.touches[0].clientY > catalog.touchstartY) ||
          (catalog.e.scrollTop === catalog.e.scrollHeight-catalog.e.clientHeight && e.touches[0].clientY < catalog.touchstartY)) {
        if (catalog.touchcancel) {
          e.preventDefault()
        }
      } else {
        if (catalog.touchcancel) {
          catalog.touchcancel = false
        }
      }
    }
    function handleTouchend() {
      catalog.touchcancel = true
    }

    // 页面销毁前，存在对header的修改
    onBeforeUnmount(()=>{
      document.getElementById('header').style.top = '0'
    })

    return {
      chapter, changeChapter, toChapter,
      update, openUpdatePanel, closeUpdatePanel, updateChapter,
      menu, handleClick, handleTouchstart, handleTouchmove, handleTouchend,
    }
  }
}
</script>

<style scoped>
::v-deep(::-webkit-scrollbar) {
  width: 0;
}
.page-header {
  height: 3rem;
  display: -webkit-flex;
  display: flex;
  justify-content: space-between;
  position: fixed;
  top: -3rem;
  left: 0;
  right: 0;
  z-index: 11;
  background-color: rgba(0,0,0,.9);
  transition: top .2s;
}
.page-header .menu {
  display: -webkit-flex;
  display: flex;
  justify-content: flex-end;
  justify-items: center;
  align-items: center;
}
.page-header .item {
  box-sizing: border-box;
  width: 3rem;
  height: 3rem;
  padding: 0.75rem;
  color: #ffffffdd;
}
.page-header .item .icon {
  width: 100%;
  height: 100%;
  fill: currentColor;
}
.read-wrap {
  background:
      url(../assets/image/bg/bg_mobile_novel_article_top.jpg) no-repeat center top,
      url(../assets/image/bg/bg_mobile_novel_article_center.jpg) repeat-y center 100px,
      url(../assets/image/bg/bg_mobile_novel_article_bottom.jpg) no-repeat center bottom;
  background-size: 100%;
}
.read-wrap .top {
  height: 2.75rem;
  position: fixed;
  top: 0;
  right: 0;
  left: 0;
  z-index: 1;
  background: url(../assets/image/bg/bg_mobile_novel_article_top.jpg) no-repeat center top;
  background-size: 100%;
}
.read-wrap .top h1 {
  padding: 1rem;
  font-size: .75rem;
  font-weight: normal;
  color: rgba(0,0,0,.4);
}
.read-wrap article {
  min-height: calc(100vh - 2.75rem);
  margin: 2.75rem 1rem 0;
  font-size: 1.125rem;
  line-height: 1.8;
  overflow: hidden;
  text-align: justify;
}
article .title {
  margin: 1em 0;
  font-size: 1.5em;
  font-weight: 400;
  line-height: 1.2;
}
article .content .paragraph {
  margin: 0.1em 0;
  font-size: 1.15em;
  word-break: break-all;
  text-indent: 2em;
}
article .control {
  width: 100%;
  height: 2.5rem;
  margin: 1.5rem 0 3rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
article .control a {
  box-sizing: border-box;
  width: 30%;
  padding: 0 1rem;
  border-radius: 0.125rem;
  font-size: .8125rem;
  color: #ffffff;
  line-height: 1.875rem;
  text-align: center;
  background-color: #ed424b;
}
article .control span {
  height: 1.5rem;
  border-right: 1px solid #262626;
}
article .control .disabled {
  color: #ffffffa0;
  background-color: #ed424ba0;
}
.footer {
  height: 3.5rem;
  display: grid;
  grid-template-columns: repeat(4, 25vw);
  grid-template-rows: repeat(1, 3.5rem);
  justify-items: center;
  align-items: center;
  position: fixed;
  bottom: -3.5rem;
  left: 0;
  right: 0;
  z-index: 11;
  background-color: rgba(0,0,0,.9);
  transition: bottom .2s;
}
.footer .item {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #ffffffcc;
}
.footer .item .icon {
  width: 1.375rem;
  height: 1.375rem;
  fill: currentColor;
}
.footer .item span {
  font-size: 0.625rem;
}
.footer .on {
  color: #ffffff;
}
.shadow {
  width: 100vw;
  height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 19;
  background-color: rgba(0,0,0,0);
  visibility: hidden;
  transition: visibility .2s, background-color .2s;
}
.shadow.active {
  background-color: rgba(0,0,0,.4);
  visibility: visible;
}
.catalog-wrap {
  position: fixed;
  top: 0;
  left: 4rem;
  right: 0;
  bottom: 0;
  z-index: 19;
  background-color: #ffffff;
  transform: translateX(100%);
  transition: transform .2s;
}
.shadow.active .catalog-wrap {
  transform: translateX(0);
}
.catalog-wrap .title {
  border-bottom: 1px solid #ed424b;
  font-size: .875rem;
  line-height: 2.75rem;
  font-weight: normal;
  color: #ed424b;
  text-align: center;
}
.catalog-wrap .catalog {
  max-height: calc(100vh - 3rem);
  overflow: auto;
}
.catalog-wrap .catalog .catalog-header {
  height: 1.75rem;
  padding: .5rem 1rem;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.catalog-wrap .catalog .catalog-header .left {
  font-size: .875rem;
  font-weight: 700;
}
.catalog-wrap .catalog .catalog-header .right {
  font-size: .875rem;
  color: #33373d;
}
.catalog-wrap .catalog .catalog-main {
  padding-left: 1rem;
}
.catalog-wrap .catalog .catalog-main li {
  padding: .75rem 2rem 0.75rem 0;
  border-bottom: 1px solid #f0f1f2;
  font-size: 0.875rem;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow:hidden;
}
.catalog-wrap .catalog .catalog-main .on {
  color: #ed424b;
}
</style>
