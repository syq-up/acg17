<template>
  <div class="index-page">
    <div class="background-container"></div>
    <div class="logo unselectable font-blueaka">
      <img src="../assets/logo.png" alt="ACGN LOGO" @click="refreshCategoryBg" />
      <h1>ACGN</h1>
    </div>
    <!-- 背景气泡（装饰元素） -->
    <div v-for="n in 20" :key="n" class="bubble" :style="getBubbleStyle(n)"></div>

    <div class="card-container unselectable">
      <div v-for="(item, index) in catalogList" :key="item.index" class="card-wrapper" @click="toRoute(item.path)">
        <div class="card" :class="`card-${index + 1}`">
          <!-- Clipper for background and art -->
          <div class="card-clipper">
            <div class="char-art" :style="item.bgUrl ? { backgroundImage: 'url(' + item.bgUrl + ')' } : {}">
              <!-- 卡片装饰元素（CSS 形状） -->
              <div class="decoration" style="top: 10%; left: 10%; width: 50px; height: 50px;"></div>
              <div class="decoration" style="bottom: 30%; right: -20px; width: 100px; height: 100px;"></div>
            </div>
            <div class="card-inner"></div>
          </div>
          <!-- 卡片铭牌-->
          <div class="name-tag">
            <h2 class="name-text">{{ item.title }}</h2>
          </div>
        </div>
      </div>
    </div>

    <div class="footer font-blueaka">
      Copyright © 2022&nbsp;
      <a href="https://acg17.shiyq.top">acg17.shiyq.top</a>&nbsp;
      All Rights Reserved&nbsp;
      <a href="http://www.beian.gov.cn/" target="_blank" style="color: #409effd0">豫ICP备2021006771号</a>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'

export default {
  name: 'Index',
  setup() {
    const store = useStore()
    const router = useRouter()

    // 分类目录
    const bgImgPrefix = ref('')
    // 初始化背景前缀
    const initBgImgPrefix = () => {
      bgImgPrefix.value = store.state.userInfo.username
        ? `/api/public-assets/bg_category_r18/${Math.floor(Math.random()*1)+3}/`
        : `/api/public-assets/bg_category_r17/${Math.floor(Math.random()*1)+1}/`
    }
    // 页面加载时初始化
    initBgImgPrefix()
    
    const catalogList = computed(() => [
      {index: '01', title: 'Illustration', path: '/acg/illustration', bgUrl: bgImgPrefix.value + '1.jpg',},
      {index: '02', title: 'Manga', path: '/acg/manga', bgUrl: bgImgPrefix.value + '2.jpg',},
      {index: '03', title: 'Game', path: '/acg/game', bgUrl: bgImgPrefix.value + '3.jpg',},
      {index: '04', title: 'Novel', path: '/acg/novel', bgUrl: bgImgPrefix.value + '4.jpg',},
      {index: '05', title: 'Jellyfin', path: '/acg/anime', bgUrl: bgImgPrefix.value + '5.jpg',},
    ])
    
    // 路由跳转
    function toRoute(path) {
      router.push(path)
    }

    // 气泡样式生成
    const getBubbleStyle = () => {
      const size = Math.random() * 60 + 20 + 'px';
      const left = Math.random() * 100 + '%';
      const delay = Math.random() * 15 + 's';
      const duration = Math.random() * 10 + 10 + 's';
      return {
        width: size,
        height: size,
        left: left,
        animationDelay: delay,
        animationDuration: duration
      };
    }

    // 刷新分类背景
    const refreshCategoryBg = () => {
      initBgImgPrefix()
    }

    return { catalogList, toRoute, getBubbleStyle, refreshCategoryBg }
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Nunito:wght@400;700&family=Pacifico&display=swap');

* {
  /* 用于background-container外边距、页脚高度 */
  --m-width: calc(min(3vh, 3vw));
  --m-height: calc(min(4vh, 4vw));
}

.index-page {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.background-container {
  margin: var(--m-height) var(--m-width);
  width: calc(100vw - var(--m-width) * 2);
  height: calc(100vh - var(--m-height) * 2);
  position: absolute;
  top: 0;
  left: 0;
  border-radius: 35px;
  /* Glassmorphism Effect */
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px 0 rgba(97, 101, 161, 0.3);
}

.logo {
  position: absolute;
  top: calc(var(--m-height) + 25px);
  left: calc(var(--m-width) + 25px);
  display: flex;
  align-items: center;
  z-index: 100;
}

.logo img {
  width: 40px;
  height: 40px;
  margin-right: 18px;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
  cursor: pointer;
}

.logo img:hover {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 6px 20px rgba(255, 255, 255, 0.4);
}

.logo h1 {
  color: #fff;
  font-weight: 700;
  font-size: 1.3rem;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  letter-spacing: 1px;
  transition: all 0.3s ease;
}

/* Background Bubbles */
.bubble {
  position: absolute;
  bottom: -50px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  animation: float 15s infinite linear;
  z-index: 0;
}

@keyframes float {
  0% { transform: translateY(0) rotate(0deg); opacity: 0; }
  10% { opacity: 0.5; }
  90% { opacity: 0.5; }
  100% { transform: translateY(-120vh) rotate(360deg); opacity: 0; }
}

/* Card Container */
.card-container {
  display: flex;
  gap: 15px;
  padding: 20px 0 40px;
  z-index: 10;
  align-items: center;
}

/* Card Wrapper for Float Animation */
.card-wrapper {
  position: relative;
}

/* Character Card */
.card {
  width: 200px;
  height: 620px;
  border-radius: 8px;
  position: relative;
  overflow: visible; 
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  background-size: cover;
  background-position: center;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
}

/* Card Content Clipping Wrapper */
.card-clipper {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  z-index: 0;
}

/* High/Low Alignment - Moved to wrapper */
/* 1, 3, 5 Low */
.card-wrapper:nth-child(odd) {
  transform: skewY(-5deg) translateY(30px);
  animation: floatCardOdd 3s ease-in-out infinite;
}
/* 2, 4 High */
.card-wrapper:nth-child(even) {
  transform: skewY(-5deg) translateY(-30px);
  animation: floatCardEven 3s ease-in-out infinite;
}

/* Pause animation on hover */
.card-wrapper:hover {
  animation-play-state: paused;
  z-index: 20; /* Bring forward */
}

@keyframes floatCardOdd {
  0%, 100% { transform:skewY(-5deg) translateY(30px); }
  50% { transform: skewY(-5deg) translateY(20px); }
}

@keyframes floatCardEven {
  0%, 100% { transform: skewY(-5deg) translateY(-30px); }
  50% { transform: skewY(-5deg) translateY(-20px); }
}

/* Hover Effects - Only affect current card inner */
.card:hover {
  transform: translateY(-10px) scale(1.05); /* Float up relative to wrapper */
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.4);
}

/* 卡片内层内容 */
.card-inner {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0) 60%, rgba(0,0,0,0.3) 100%);
  z-index: 1;
}

/* Character Image Placeholder (Gradient/Art) */
.char-art {
  width: 100%;
  height: 104%; /* Taller to cover skew gaps */
  position: absolute;
  top: -2%;
  left: 0;
  transition: transform 0.5s ease;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  transform: skewY(5deg); 
}

.card:hover .char-art {
  transform: skewY(5deg) scale(1.1);
}

/* 卡片铭牌 */
.name-tag {
  position: absolute;
  bottom: 10%;
  width: 101%; /* Match card width */
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  text-align: center;
  z-index: 10; /* Ensure it's above the clipped content */
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
  transform: skewY(5deg) rotate(-5deg) skewX(-11deg);
}

/* 卡片铭牌偏移量 */
.card-wrapper:nth-child(1) .name-tag { left: -10%; }
.card-wrapper:nth-child(2) .name-tag { left: -4%; }
.card-wrapper:nth-child(3) .name-tag { left: 2%; }
.card-wrapper:nth-child(4) .name-tag { left: 6%; }
.card-wrapper:nth-child(5) .name-tag { left: 12%; }

.card-wrapper:hover .name-tag {
  background: rgba(255, 255, 255, 0.4);
  transform: translateY(-10px) skewY(5deg) rotate(-5deg) skewX(-11deg);
}

.name-text {
  font-family: 'Pacifico', cursive;
  font-size: 2.6rem;
  line-height: 1.5;
  color: #fff;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.15);
  background: -webkit-linear-gradient(#fff, #eee);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  /* Text follows the tag's rotation/skew */
  display: inline-block;
}

.card:hover .name-text {
  text-shadow: 2px 2px 4px rgba(170, 210, 255, 0.25);
}

/* 卡片背景色，已被背景图片覆盖 */
.card-1 .char-art { background-image: linear-gradient(to bottom, #ffdee9 0%, #b5fffc 100%); } /* Pink to Blue */
.card-2 .char-art { background-image: linear-gradient(to bottom, #ffe259 0%, #ffa751 100%); } /* Yellow to Orange */
.card-3 .char-art { background-image: linear-gradient(to bottom, #E0C3FC 0%, #8EC5FC 100%); } /* Purple to Blue */
.card-4 .char-art { background-image: linear-gradient(to bottom, #4facfe 0%, #00f2fe 100%); } /* Blue */
.card-5 .char-art { background-image: linear-gradient(to bottom, #fad0c4 0%, #ffd1ff 100%); } /* Pink */

/* 卡片装饰元素（CSS 形状） */
.decoration {
  position: absolute;
  border-radius: 50%;
  background: rgba(255,255,255,0.3);
  z-index: 1;
}

.footer {
  position: absolute;
  bottom: 0;
  width: 100%;
  height: var(--m-height);
  display: flex;
  justify-content: center;
  align-items: center;
  animation: slideUp 0.4s ease-out 0.3s both;
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.9rem;
  z-index: 100;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.2);
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.footer a {
  color: inherit;
  text-decoration: none;
}

.footer a:hover {
  text-decoration: underline;
}
</style>
