<template>
  <div class="page-header">
  
  </div>
  <section></section>
  <acg17-footer v-if="anime.disabled"></acg17-footer>
</template>

<script>
import { reactive } from 'vue'
// import { useStore } from 'vuex'
// import { useRouter } from 'vue-router';
// import server from '@/util/request';

// import LoadingHeart from "../components/LoadingHeart";
import Acg17Footer from "../components/Acg17Footer";
import { useRecycleState } from '@/composables/useRecycleState';

export default {
  name: "Anime",
  components: {

    // 'acg17-loading-heart': LoadingHeart,
    'acg17-footer': Acg17Footer,
  },
  setup() {
    // const store = useStore()
    // const router = useRouter()
    
    // 使用全局回收站状态管理
    const { isRecycle, toggleRecycle, setRecycle } = useRecycleState('anime')

    const anime = reactive({
      currentPage: 0, // 当前页
      list: [], // 当前页数据
      loading: false, // 加载下一页时显示loading
      disabled: false,  // 加载到最后一页时禁用加载
      categoryList: [], // 小说分类列表
      tagList: [],  // 小说标签列表
    })

    return { anime, isRecycle, toggleRecycle, setRecycle }
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
</style>
