<template>
  <section>
    <div class="side-btn-group left-btn-group">
      <button
        type="button"
        class="side-btn"
        :disabled="illustration.list.length === 0"
        aria-label="随机插画"
        title="随机插画"
        @click="randomArtwork"
      >
        <icon icon="#icon-random"></icon>
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

    <div ref="galleryContainer" class="gallery-container" v-infinite-scroll="loadArtworks" :infinite-scroll-disabled="illustration.disabled">
      <div v-for="(row, rowIndex) in layoutRows" :key="rowIndex" class="gallery-row" :style="{ height: row.height + 'px', gap: row.gap + 'px', marginBottom: row.gap + 'px' }">
        <div v-for="entry in row.items" :key="entry.item.id ?? entry.index" class="gallery-item"
             :class="{
               'is-dragging': drag.isDrag && entry.item === drag.sourceItem,
             }"
             :style="{ width: entry.width + 'px' }"
             draggable="true"
             @dragstart="handleDragStart($event, entry.index)"
             @dragover="handleDragover"
             @dragenter="handleDragenter($event, entry.index)"
             @drop="handleDrop($event, entry.index)"
             @dragend="handleDragend"
        >
          <img :src="withMediaStyle(entry.item.url, 'small')" class="gallery-img" loading="lazy" @click="openPreview(entry.index)">
          
          <div class="item-action-overlay">
            <div class="overlay-left" v-show="!drag.isDrag">
               <div class="action"><!-- checkbox-on -->
                  <icon icon="#icon-checkbox-off"></icon>
               </div>
            </div>
            <div class="overlay-right" v-show="!drag.isDrag">
               <div class="action delete" v-show="!isRecycle" @click.stop="deleteArtwork(entry.index)">
                  <icon icon="#icon-delete"></icon>
               </div>
               <div class="action restore" v-show="isRecycle" @click.stop="restoreArtwork(entry.index)">
                  <icon icon="#icon-restore"></icon>
               </div>
            </div>
          </div>
          
        </div>
      </div>
    </div>
    <acg17-loading-heart v-show="illustration.loading"></acg17-loading-heart>
  </section>
  <acg17-footer v-if="illustration.disabled"></acg17-footer>
  <div v-show="preview.show">
    <div @click="preview.show = false" class="preview-shade"></div>
    <img class="preview-artwork" :src="preview.show ? illustration.list[preview.index].url : ''" alt=""
         @touchstart.stop="touchstart" @touchmove.stop="touchmove" @touchend.stop="touchend">
    <div @click="preview.show = false" class="close-shade-btn hvr-grow">
      <icon icon="#icon-close"></icon>
    </div>
    <div @click="lastPage" v-show="preview.index !== 0" class="last-artwork-btn hvr-grow">
      <icon icon="#icon-left"></icon>
    </div>
    <div @click="nextPage" v-show="preview.index !== illustration.list.length-1" class="next-artwork-btn hvr-grow">
      <icon icon="#icon-right"></icon>
    </div>
  </div>
</template>

<script>
import { reactive, watch, ref, computed, onMounted, onUnmounted } from 'vue'
import server from '@/util/request';

import LoadingHeart from "../components/LoadingHeart";
import Acg17Footer from "../components/Acg17Footer";
import { useRecycleState, loadData } from '@/composables/useRecycleState';
import { useBackToTop } from '@/composables/useBackToTop';
import { withMediaStyle } from '@/util/media';
import { createJustifiedRows } from '@/utils/justifiedGallery.mjs';

export default {
  name: "Illustration",
  components: {

    'acg17-loading-heart': LoadingHeart,
    'acg17-footer': Acg17Footer,
  },
  setup() {
    // 使用全局回收站状态管理
    const { isRecycle, toggleRecycle, setRecycle } = useRecycleState('illustration')
    
    const illustration = reactive({
      currentPage: 0, // 当前页
      list: [], // 当前页数据
      loading: false, // 加载下一页时显示loading
      disabled: false,  // 加载到最后一页时禁用加载
    })

    // --- 布局逻辑 Start ---
    const galleryContainer = ref(null)
    const containerWidth = ref(0)
    let resizeObserver = null;

    const { showBackToTop, scrollToTop } = useBackToTop()

    const layoutRows = computed(() => createJustifiedRows(illustration.list, containerWidth.value));

    const updateWidth = () => {
      if (galleryContainer.value) {
        containerWidth.value = galleryContainer.value.clientWidth
      }
    };

    onMounted(() => {
        updateWidth()
        resizeObserver = new ResizeObserver(updateWidth)
        resizeObserver.observe(galleryContainer.value)
    });

    onUnmounted(() => {
        if (resizeObserver) resizeObserver.disconnect();
    });
    // --- 布局逻辑 End ---

    // 分页加载图片，当前页
    function loadArtworks() {
      illustration.loading = true
      illustration.disabled = true
      
      loadData({
        basePath: '/illustration',
        isRecycle: isRecycle.value,
        pageNum: ++illustration.currentPage,
        server
      })
      .then(res=>{
        // records.length!==0：当前页非空页，可能存在下一页，对当前页图像数据进行下一步处理
        // records.length===0：当前页为空页，不存在下一页，置disabled=true，不再请求下一页
        if (res.data.records.length!==0) {
          illustration.list.push(...res.data.records)
          illustration.disabled = false
        } else {
          illustration.disabled = true
        }
        illustration.loading = false
      })
      .catch(err=>{
        console.log(err)
      })
    }
    // 监听切换回收站列表
    watch(isRecycle, ()=>{
      illustration.list = []
      illustration.currentPage = 0
      illustration.disabled = false
      loadArtworks()
    })

    // 删除插画作品
    function deleteArtwork(i) {
      server.delete('/illustration/' + illustration.list[i].id)
          .then(()=>{
            illustration.list.splice(i, 1)
            ElMessage.success('插画删除成功！')
          })
          .catch(err=>{
            console.log(err)
          })
    }
    // 回收已删除的插画作品
    function restoreArtwork(i) {
      server.put('/illustration/' + illustration.list[i].id + '/restore')
          .then(()=>{
            illustration.list.splice(i, 1)
            ElMessage.success('插画回收成功！')
          })
          .catch(err=>{
            console.log(err)
          })
    }

    // 全屏预览功能
    const preview = reactive({
      show: false,  // 当前是否开启了全屏预览
      index: 0, // 当前预览的插画下标
      startX: 0,  // 滑动开始的位置
      moveX: 0, // 滑动移动的距离
    })
    // 开启全屏预览
    function openPreview(i) {
      preview.show = true
      preview.index = i
    }
    function lastPage() {
      if (preview.index !== 0) {
        preview.index--
      }
    }
    function nextPage() {
      if (preview.index !== illustration.list.length-1) {
        preview.index++
      }
    }
    // 全屏预览左右滑动切换插画
    function touchstart(e) {
      preview.startX = 0
      // touches类数组，等于1时表示此时有只有一只手指在触摸屏幕
      if (e.touches.length === 1) {
        // 记录开始位置
        preview.startX = e.touches[0].clientX;
      }
    }
    function touchmove(e) {
      preview.moveX = e.touches[0].clientX - preview.startX
    }
    function touchend() {
      if (preview.moveX > 75) {
        lastPage()
      } else if (preview.moveX < -75) {
        nextPage()
      }
      preview.moveX = 0
    }

    // 拖拽排序功能
    const drag = reactive({
      isDrag: false,  // 当前是否处于拖拽
      originalList: [], // 原list
      dragIndex: -1,  // 当前拖拽元素的位置（原list中的下标）
      dragenterIndex: -1, // 当前拖拽元素进入的位置（原list中的下标）
      dropIndex: -1,  // 拖拽释放的位置（原list中的下标）
      sourceItem: null, // 当前拖拽的插画
    })
    function handleDragStart(e, i) {
      drag.isDrag = true
      drag.originalList = [...illustration.list]
      drag.dragIndex = i
      drag.dragenterIndex = i
      drag.sourceItem = illustration.list[i]

      e.dataTransfer.effectAllowed = 'move'
    }

    function handleDragover(e) {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move'
    }
    function handleDragenter(e, i) {
      e.preventDefault()
      if (!drag.isDrag || i === drag.dragenterIndex) return

      illustration.list.splice(drag.dragenterIndex, 1)
      illustration.list.splice(i, 0, drag.originalList[drag.dragIndex])
      drag.dragenterIndex = i
    }
    function handleDrop(e, i) {
      e.preventDefault();
      drag.dropIndex = i
    }
    function handleDragend(e) {
      e.preventDefault();
      // 若 拖拽元素最后进入位置 !== 最后释放位置，说明元素在区域外释放，不予处理，返回原位置。
      // 若 释放位置 === 原位置，说明没有变更位置。
      // 否则，再变更其排序位置
      if (drag.dragenterIndex !== drag.dropIndex) {
        illustration.list.splice(drag.dragenterIndex, 1)
        illustration.list.splice(drag.dragIndex, 0, drag.originalList[drag.dragIndex])
        ElMessage.info("位置变更取消。")
      } else if (drag.dropIndex === drag.dragIndex) {
        ElMessage.info("位置变更取消。")
      } else {
        const data = {
          id: drag.originalList[drag.dragIndex].id,
          targetId: drag.originalList[drag.dropIndex].id,
        }
        server.post('/illustration/reorder', data)
            .then(()=>{
              ElMessage.success("插画排序位置变更成功！")
            })
            .catch(err=>{
              // 位置变更失败，返回原位置
              illustration.list.splice(drag.dropIndex, 1)
              illustration.list.splice(drag.dragIndex, 0, drag.originalList[drag.dragIndex])
              console.log(err)
            })
      }
      // 拖拽结束
      drag.isDrag = false
      drag.sourceItem = null
    }

    // 随机打开一个插画
    function randomArtwork() {
      if (illustration.list.length === 0) return

      const randomIndex = Math.floor(Math.random() * illustration.list.length)
      openPreview(randomIndex)
    }

    return {
      illustration, loadArtworks, layoutRows,
      isRecycle, toggleRecycle, setRecycle,
      preview, openPreview, lastPage, nextPage, touchstart, touchmove, touchend,
      deleteArtwork, restoreArtwork,
      drag, handleDragStart, handleDragover, handleDragenter, handleDrop, handleDragend,
      galleryContainer, containerWidth, randomArtwork, scrollToTop, showBackToTop, withMediaStyle
    }
  }
}
</script>

<style scoped>
section {
  --page-side-actions-z-index: 8;
  margin: 84px auto 20px;
  max-width: 1380px;
  /* 屏幕高度 - 自身上下外边距高度 - 页脚高度 */
  min-height: calc(100vh - 104px - 200px);
}

.gallery-container {
    width: 100%;
    margin: 0 auto;
    position: relative;
}

.gallery-row {
    display: flex;
}

.gallery-item {
    flex: 0 0 auto;
    height: 100%;
    position: relative;
    border-radius: 4px;
    overflow: hidden;
    background-color: #252525;
    cursor: pointer;
    transition: filter 0.2s;
}

.gallery-item.is-dragging {
    opacity: 0.01;
}

.gallery-item:hover {
    filter: brightness(0.9);
}

.gallery-img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
}

.item-action-overlay {
    position: absolute;
    top: 0px;
    left: 0px;
    right: 0px;
    display: flex;
    justify-content: space-between;
    opacity: 0;
    transition: opacity 0.2s;
    pointer-events: none;
}

.gallery-item:hover .item-action-overlay {
    opacity: 1;
}

.item-action-overlay > * {
    pointer-events: auto;
}

.overlay-left, .overlay-right {
    display: flex;
    gap: 8px;
}

.action {
    width: 32px;
    height: 32px;
    border-radius: 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    cursor: pointer;
}

.action:hover {
    background: rgba(255,255,255,0.2);
}

.action.checkbox-on {
    background-color: #138dff;
}

.action.delete {
    background-color: #f56c6c;
}
.action.restore {
    background-color: #138dff;
}

.action svg, .action .icon {
    width: 90%;
    height: 90%;
    fill: currentColor;
}

/* 大图预览，遮罩层 start */
.preview-shade {
  width: 100vw;
  height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 18;
  background-color: rgba(0, 0, 0, 0.5);
}
.preview-artwork {
  max-width: 100vw;
  max-height: 100vh;
  position: fixed;
  top: 50vh;
  left: 50vw;
  transform: translate(-50%, -50%);
  z-index: 19;
}
.close-shade-btn {
  position: fixed;
  top: 5vh;
  right: 5vh;
  width: 60px;
  height: 60px;
  z-index: 20;
}
.last-artwork-btn {
  position: fixed;
  top: 10vh;
  left: 5vh;
  width: 60px;
  height: 80vh;
  z-index: 19;
}
.next-artwork-btn {
  position: fixed;
  top: 10vh;
  right: 5vh;
  width: 60px;
  height: 80vh;
  z-index: 19;
}
.close-shade-btn .icon,
.last-artwork-btn .icon,
.next-artwork-btn .icon {
  width: 100%;
  height: 100%;
  fill: #ffffff;
}
/* 大图预览，遮罩层 end */

/* 鼠标触碰时动画 */
/* Grow */
.hvr-grow {
  display: inline-block;
  vertical-align: middle;
  -webkit-transform: perspective(1px) translateZ(0);
  transform: perspective(1px) translateZ(0);
  box-shadow: 0 0 1px rgba(0, 0, 0, 0);
  -webkit-transition-duration: 0.3s;
  transition-duration: 0.3s;
  -webkit-transition-property: transform;
  transition-property: transform;
  cursor: pointer;
}
.hvr-grow:hover, .hvr-grow:focus, .hvr-grow:active {
  -webkit-transform: scale(1.4);
  transform: scale(1.4);
}

@media (max-width: 767px) {
  section {
    padding-bottom: calc(76px + env(safe-area-inset-bottom, 0px));
  }

}
</style>
