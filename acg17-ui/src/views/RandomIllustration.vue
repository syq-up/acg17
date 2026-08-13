<template>
  <img v-show="random.error.length === 0" :src="random.url" alt="" @dblclick="getRandomImage">
  <p v-show="random.error.length !== 0" v-text="random.error"></p>
</template>

<script>
import { onMounted, reactive } from "vue";
import server from '@/util/request';

export default {
  name: "RandomIllustration",
  setup() {
    const random = reactive({
      url: '',
      error: '',
    })

    // 获取随机图片的函数
    const getRandomImage = () => {
      // 清空之前的错误信息
      random.error = ''

      server.get('/illustration/getRandomArtwork')
        .then(res => {
          if (res.data?.urlMiddle) {
            random.url = res.data.urlMiddle
            return
          }
          random.url = ''
          random.error = '暂无可用插画'
        })
        .catch(() => {
          random.url = ''
          random.error = '随机插画加载失败'
        })
    }

    // 组件挂载时获取第一张图片
    onMounted(() => {
      getRandomImage()
    })

    return { random, getRandomImage }
  }
}
</script>

<style scoped>
img {
  width: 100%;
  height: 100%;
  max-width: 100vw;
  max-height: 100vh;
  object-fit: contain;
  display: block;
}
</style>
