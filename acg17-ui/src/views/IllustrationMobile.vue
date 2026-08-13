<template>
  <div class="page-header">
  
  </div>
  <section>
    <ul v-infinite-scroll="loadArtworks" :infinite-scroll-disabled="illustration.disabled">
      <li v-for="(artwork, i) in illustration.list" :key="artwork.id" @click="openPreview(i)">
        <img class="artwork-img" :src="artwork.urlTiny" alt="artwork">
      </li>
    </ul>
    <acg17-loading-heart v-show="illustration.loading"></acg17-loading-heart>
  </section>
  <acg17-footer v-if="illustration.disabled"></acg17-footer>
  <div v-show="preview.show">
    <div @click="preview.show = false" class="preview-shade"
         @touchstart.stop="touchstart" @touchmove.stop="touchmove" @touchend.stop="touchend"></div>
    <img class="preview-artwork" :src="preview.show ? illustration.list[preview.index].urlMiddle : ''" alt=""
         @touchstart.stop="touchstart" @touchmove.stop="touchmove" @touchend.stop="touchend">
    <div @click="preview.show = false" class="close-shade-btn">
      <icon icon="#icon-close"></icon>
    </div>
    <div @click="lastPage" v-show="preview.index !== 0" class="last-artwork-btn">
      <icon icon="#icon-left"></icon>
    </div>
    <div @click="nextPage" v-show="preview.index !== illustration.list.length-1" class="next-artwork-btn">
      <icon icon="#icon-right"></icon>
    </div>
  </div>
</template>

<script>
import { reactive } from 'vue'
import server from '@/util/request';

import LoadingHeart from "../components/LoadingHeart";
import Acg17Footer from "../components/Acg17Footer";

export default {
  name: "IllustrationMobile",
  components: {

    'acg17-loading-heart': LoadingHeart,
    'acg17-footer': Acg17Footer,
  },
  setup() {
    const illustration = reactive({
      isRecycle: false, // 当前是否查询回收站列表
      currentPage: 0, // 当前页
      list: [], // 当前页数据
      loading: false, // 加载下一页时显示loading
      disabled: false,  // 加载到最后一页时禁用加载
    })
    // 分页加载图片，当前页
    function loadArtworks() {
      illustration.loading = true
      // 参数
      const params = {
        pageNum: ++illustration.currentPage
      }
      server.get('/illustration' + (illustration.isRecycle ? '/getRecycleList' : '/getList'), {params})
          .then(res=>{
            // records.length!==0：当前页非空页，可能存在下一页，对当前页图像数据进行下一步处理
            // records.length===0：当前页为空页，不存在下一页，置disabled=true，不再请求下一页
            if (res.data.records.length!==0) {
              illustration.list.push(...res.data.records)
            } else {
              illustration.disabled = true
            }
            illustration.loading = false
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
    // 上一张插画
    function lastPage() {
      if (preview.index !== 0) {
        preview.index--
      }
    }
    // 下一张插画
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

    return {
      illustration, loadArtworks,
      preview, openPreview, lastPage, nextPage, touchstart, touchmove, touchend,
    }
  }
}
</script>

<style scoped>
.page-header ::v-deep(.navi-menu) {
  width: 99vw;
}
section {
  margin: 84px auto 20px;
  width: 100vw;
  /* 屏幕高度 - 自身上下外边距高度 - 页脚高度 */
  min-height: calc(100vh - 104px - 200px);
}
ul {
  width: 100vw;
  padding: 0;
  margin: 0;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(2, 50vw);
  grid-template-rows: repeat(1, 50vw);
  grid-auto-rows: 50vw;
}
ul li {
  box-sizing: border-box;
  width: 50vw;
  height: 50vw;
  overflow: hidden;
}
ul li .artwork-img {
  box-sizing: border-box;
  width: 50vw;
  height: 50vw;
  object-fit: cover;
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
  right: 3vw;
  width: 40px;
  height: 40px;
  z-index: 19;
}
.last-artwork-btn {
  position: fixed;
  top: 50vh;
  left: 3vw;
  width: 40px;
  height: 40px;
  z-index: 19;
}
.next-artwork-btn {
  position: fixed;
  top: 50vh;
  right: 3vw;
  width: 40px;
  height: 40px;
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
</style>