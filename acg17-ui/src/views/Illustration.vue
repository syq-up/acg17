<template>
  <section>
    <div class="gallery-container" v-infinite-scroll="loadArtworks" :infinite-scroll-disabled="illustration.disabled">
      <!-- Random Button -->
      <div class="side-btn left-btn" @click="randomArtwork" :style="{ right: '50%', marginRight: (containerWidth / 2 + 30) + 'px' }">
        <icon icon="#icon-random"></icon>
      </div>
      <!-- Back to Top Button -->
      <div class="side-btn right-btn" v-show="showBackToTop" @click="scrollToTop" :style="{ left: '50%', marginLeft: (containerWidth / 2 + 30) + 'px' }">
        <icon icon="#icon-sort-asc"></icon>
      </div>

      <div v-for="(row, rowIndex) in layoutRows" :key="rowIndex" class="gallery-row" :style="{ height: row.height + 'px', gap: row.gap + 'px', marginBottom: row.gap + 'px' }">
        <div v-for="item in row.items" :key="item.id" class="gallery-item"
             :id="illustration.list.indexOf(item)"
             draggable="true"
             @dragstart="handleDragStart($event, illustration.list.indexOf(item))"
             @dragover="handleDragover"
             @dragenter="handleDragenter($event, illustration.list.indexOf(item))"
             @dragleave="handleDragleave($event, illustration.list.indexOf(item))"
             @drop="handleDrop($event, illustration.list.indexOf(item))"
             @dragend="handleDragend"
        >
          <img :src="item.urlTiny" class="gallery-img" :style="{ width: row.height * item.ratio + 'px' }" loading="lazy" @click="openPreview(illustration.list.indexOf(item))">
          
          <div class="item-action-overlay">
            <div class="overlay-left" v-show="!drag.isDrag">
               <div class="action"><!-- checkbox-on -->
                  <icon icon="#icon-checkbox-off"></icon>
               </div>
            </div>
            <div class="overlay-right" v-show="!drag.isDrag">
               <div class="action delete" v-show="!isRecycle" @click.stop="deleteArtwork(illustration.list.indexOf(item))">
                  <icon icon="#icon-delete"></icon>
               </div>
               <div class="action restore" v-show="isRecycle" @click.stop="restoreArtwork(illustration.list.indexOf(item))">
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
    <img class="preview-artwork" :src="preview.show ? illustration.list[preview.index].urlMiddle : ''" alt=""
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
import { reactive, watch, ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import server from '@/util/request';

import LoadingHeart from "../components/LoadingHeart";
import Acg17Footer from "../components/Acg17Footer";
import { ElMessage } from "element-plus";
import { useRecycleState, loadData } from '@/composables/useRecycleState';

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
    const containerWidth = ref(1380);
    const targetRowHeight = 250; 
    let resizeObserver = null;

    const showBackToTop = ref(false);
    const handleScroll = () => {
      showBackToTop.value = window.scrollY > 500;
    };

    const layoutRows = computed(() => {
        const rows = [];
        let currentRow = [];
        let currentWeight = 0;
        const gap = 4; 

        illustration.list.forEach(item => {
            const ratio = item.ratio || 1;
            // 规则：若宽高比<=1（竖向），每行约7个 -> 权重 1/7
            // 规则：若宽高比>1（横向），每行约5个 -> 权重 1/5
            const isVertical = ratio <= 1;
            const itemWeight = isVertical ? 1/7 : 1/5;

            // 如果当前行已满（权重>=1），先结算上一行
            if (currentWeight >= 1 - 0.01) {
                const totalGap = (currentRow.length - 1) * gap;
                const totalRatio = currentRow.reduce((sum, it) => sum + (it.ratio || 1), 0);
                const rowHeight = (containerWidth.value - totalGap) / totalRatio;
                
                rows.push({ items: [...currentRow], height: rowHeight, gap: gap });
                
                currentRow = [];
                currentWeight = 0;
            }

            currentRow.push(item);
            currentWeight += itemWeight;
        });
        
        // 处理最后一行
        if (currentRow.length > 0) {
            // 如果最后一行刚好满了
            if (currentWeight >= 1 - 0.01) {
                const totalGap = (currentRow.length - 1) * gap;
                const totalRatio = currentRow.reduce((sum, it) => sum + (it.ratio || 1), 0);
                const rowHeight = (containerWidth.value - totalGap) / totalRatio;
                rows.push({ items: currentRow, height: rowHeight, gap: gap });
            } else {
                // 不满，保持目标高度
                rows.push({
                    items: currentRow,
                    height: targetRowHeight,
                    gap: gap,
                });
            }
        }
        
        return rows;
    });

    const updateWidth = () => {
        // 使用 .gallery-container 或 .gallery-scroll-area
        const container = document.querySelector('.gallery-container');
        if (container) {
            // containerWidth.value = Math.min(container.clientWidth, 1380);
            // 实际上我们想要的是容器的宽度
             containerWidth.value = container.clientWidth;
        }
    };

    onMounted(() => {
        window.addEventListener('scroll', handleScroll);
        nextTick(() => {
           updateWidth();
           window.addEventListener('resize', updateWidth);
           resizeObserver = new ResizeObserver(() => updateWidth());
           const container = document.querySelector('.gallery-container');
           if (container) resizeObserver.observe(container);
        })
    });

    onUnmounted(() => {
        window.removeEventListener('scroll', handleScroll);
        window.removeEventListener('resize', updateWidth);
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
          // 处理宽高比 ratio
          const records = res.data.records.map(item => {
            if (!item.ratio)
                item.ratio = 1 // 默认正方形
            return item
          })
          illustration.list.push(...records)
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
      server.get('/illustration/deleteById/'+illustration.list[i].id)
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
      server.get('/illustration/restoreById/'+illustration.list[i].id)
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
    })
    function handleDragStart(e, i) {
      drag.isDrag = true
      drag.originalList = [...illustration.list]
      drag.dragIndex = i
      drag.dragenterIndex = i

      e.dataTransfer.effectAllowed = 'move'
      document.getElementById(i).style.opacity = '0.01'
    }

    function handleDragover(e) {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move'
    }
    function handleDragenter(e, i) {
      illustration.list.splice(drag.dragenterIndex, 1)
      illustration.list.splice(i, 0, drag.originalList[drag.dragIndex])
      drag.dragenterIndex = i

      document.getElementById(i).style.opacity = '0.01'
    }
    function handleDragleave(e, i) {
      document.getElementById(i).style.opacity = '1'
    }
    function handleDrop(e, i) {
      e.preventDefault();
      drag.dropIndex = i

      document.getElementById(i).style.opacity = '1'
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
    }

    // 随机打开一个插画
    function randomArtwork() {
      const randomIndex = Math.floor(Math.random() * illustration.list.length)
      openPreview(randomIndex)
    }

    function scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: "smooth"
      });
    }

    return {
      illustration, loadArtworks, layoutRows,
      isRecycle, toggleRecycle, setRecycle,
      preview, openPreview, lastPage, nextPage, touchstart, touchmove, touchend,
      deleteArtwork, restoreArtwork,
      drag, handleDragStart, handleDragover, handleDragenter, handleDragleave, handleDrop, handleDragend,
      containerWidth, randomArtwork, scrollToTop, showBackToTop
    }
  }
}
</script>

<style scoped>
section {
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

.left-btn {
    position: fixed;
    top: 84px;
    /* right is handled by inline style */
}

.right-btn {
    position: fixed;
    bottom: 50px;
    /* left is handled by inline style */
}

.gallery-row {
    display: flex;
}

.gallery-item {
    height: 100%;
    position: relative;
    border-radius: 4px;
    overflow: hidden;
    background-color: #252525;
    cursor: pointer;
    transition: filter 0.2s;
}

.gallery-item:hover {
    filter: brightness(0.9);
}

.gallery-img {
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
</style>
