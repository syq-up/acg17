<template>
  <div class="page-404" >
    <div class="background-container"></div>
    <div class="logo unselectable font-blueaka">
      <img src="../assets/logo.png" alt="ACG17 LOGO" @click="getRandomImage" />
      <h1 @click="goToHome">ACG17</h1>
    </div>
    <!-- 背景气泡（装饰元素） -->
    <div v-for="(style, index) in bubbles" :key="index" class="bubble" :style="style"></div>

    <!-- 未登录状态：中央显示404信息 -->
    <div v-if="!isLoggedIn" class="center-content font-blueaka">
      <div class="error-info">
        <h1 class="error-code">404</h1>
        <h2 class="error-title">页面未找到</h2>
        <p class="error-description">抱歉，您访问的页面不存在或已被移除</p>
        <div class="button-group">
          <button @click="goBack" class="back-button">返回上一页</button>
          <router-link to="/" class="back-home">返回首页</router-link>
        </div>
      </div>
    </div>

    <!-- 登录状态：左侧404信息，右侧随机图片 -->
    <div v-else class="logged-in-layout font-blueaka">
      <div class="left-content">
        <div class="error-info">
          <h1 class="error-code">404</h1>
          <h2 class="error-title">页面未找到</h2>
          <p class="error-description">抱歉，您访问的页面不存在或已被移除</p>
          <div class="button-group">
            <button @click="goBack" class="back-button">返回上一页</button>
            <router-link to="/" class="back-home">返回首页</router-link>
          </div>
        </div>
      </div>
      <div class="right-content">
        <img v-show="random.error.length === 0" :src="random.url" alt="随机插画" @dblclick="getRandomImage"
          class="random-image">
        <p v-show="random.error.length !== 0" v-text="random.error" class="error-message"></p>
      </div>
    </div>

    <div class="footer font-blueaka">
      Copyright © 2022&nbsp;
      <div class="hide"><a href="https://acg17.shiyq.top">ACG17</a>&nbsp;</div>
      <div class="hide">All Rights Reserved&nbsp;</div>
      <a href="http://www.beian.gov.cn/" target="_blank" style="color: #409effd0">豫ICP备2021006771号</a>
    </div>
  </div>
</template>

<script>
import { onMounted, reactive, computed } from "vue";
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import server from '@/util/request';

export default {
  name: "404",
  setup() {
    const store = useStore();
    const router = useRouter();

    // 判断是否登录
    const isLoggedIn = computed(() => {
      return store.state.userInfo.username && store.state.userInfo.username.length > 0;
    });

    // 随机图片状态
    const random = reactive({
      url: '',
      error: '',
    });

    // 获取随机图片的函数
    const getRandomImage = () => {
      // 清空之前的错误信息
      random.error = '';

      server.get('/illustration/getRandomArtwork')
        .then(res => {
          if (res.data?.url) {
            random.url = res.data.url;
            return;
          }
          random.url = '';
          random.error = '暂无可用插画';
        })
        .catch(() => {
          random.url = '';
          random.error = '随机插画加载失败';
        });
    };

    // 气泡样式生成
    const bubbles = Array.from({ length: 20 }, () => {
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
    });

    // 返回上一页
    const goBack = () => {
      router.go(-1);
    };

    // 组件挂载时，如果已登录则获取随机图片
    onMounted(() => {
      if (isLoggedIn.value) {
        getRandomImage();
      }
    });

    // 跳转到首页
    const goToHome = () => {
      router.push('/')
    }

    return { isLoggedIn, random, bubbles, getRandomImage, goBack, goToHome };
  }
}
</script>

<style scoped>
* {
  /* 用于background-container外边距、页脚高度 */
  --m-width: calc(min(3vh, 3vw));
  --m-height: calc(min(4vh, 4vw));
}

.page-404 {
  margin: 0;
  padding: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  display: flex;
  align-items: center;
  justify-content: center;
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
  cursor: pointer;
}

.logo h1:hover {
  color: #409eff;
  text-shadow: 0 0 20px rgba(64, 158, 255, 0.6);
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

/* 未登录状态 - 中央内容 */
.center-content {
  position: relative;
  z-index: 2;
  text-align: center;
}

/* 登录状态 - 左右布局 */
.logged-in-layout {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  position: relative;
  z-index: 2;
}

.left-content {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.right-content {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

/* 错误信息样式 */
@keyframes fadeIn {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
}

.error-info {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  text-align: center;
  width: 360px;
  animation: fadeIn .5s ease-out;
}

.error-code {
  font-size: 6rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 20px 0;
  text-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  line-height: 1;
}

.error-title {
  font-size: 1.8rem;
  font-weight: 600;
  color: #fff;
  margin: 0 0 16px 0;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}

.error-description {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 30px 0;
  line-height: 1.6;
}

/* 按钮组样式 */
.button-group {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.back-button {
  display: inline-block;
  padding: 12px 24px;
  background: linear-gradient(135deg, #67c23a, #5daf34);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(103, 194, 58, 0.3);
}

.back-button:hover {
  background: linear-gradient(135deg, #5daf34, #529b2e);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(103, 194, 58, 0.4);
}

.back-home {
  display: inline-block;
  padding: 12px 24px;
  background: linear-gradient(135deg, #409eff, #3a8ee6);
  color: white;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
}

.back-home:hover {
  background: linear-gradient(135deg, #3a8ee6, #337ecc);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
}

/* 随机图片样式 */
.random-image {
  max-width: 66vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 16px;
  cursor: pointer;
  transition: transform 0.3s ease;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);
}

.random-image:hover {
  transform: scale(1.02);
}

.error-message {
  color: #f56c6c;
  font-size: 1rem;
  text-align: center;
  padding: 40px;
  margin: 0;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
}

/* 响应式设计 */
@media screen and (max-width: 768px) {
  .logged-in-layout {
    flex-direction: column;
  }

  .left-content,
  .right-content {
    flex: none;
    padding: 20px;
  }

  .error-info {
    padding: 30px 20px;
  }

  .error-code {
    font-size: 4rem;
  }

  .error-title {
    font-size: 1.4rem;
  }

  .random-image {
    max-width: 90vw;
    max-height: 50vh;
  }
}

@media screen and (max-width: 480px) {
  .error-info {
    padding: 20px 15px;
  }

  .error-code {
    font-size: 3rem;
  }

  .error-title {
    font-size: 1.2rem;
  }

  .error-description {
    font-size: 0.9rem;
  }

  .button-group {
    flex-direction: column;
    gap: 8px;
  }

  .back-button,
  .back-home {
    padding: 10px 20px;
    font-size: 13px;
  }

  .right-content,
  .hide {
    display: none;
  }
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
