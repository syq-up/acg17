<template>
  <div class="login-page">
    <div class="background-container"></div>
    <div class="logo unselectable font-blueaka">
      <img src="../assets/logo.png" alt="ACGN LOGO" />
      <h1 @click="goToHome">ACGN</h1>
    </div>
    <!-- 背景气泡（装饰元素） -->
    <div v-for="(style, index) in bubbles" :key="index" class="bubble" :style="style"></div>

    <form class="login-container unselectable" @submit.prevent="login">
      <h1 class="login-title">Welcome</h1>
      <div class="input-group">
        <label class="visually-hidden" for="login-username">Username</label>
        <input id="login-username" name="username" type="text" v-model="form.username" placeholder="Username"
          autocomplete="username" maxlength="64" :disabled="form.loading" @input="clearError">
      </div>
      <div class="input-group">
        <label class="visually-hidden" for="login-password">Password</label>
        <input id="login-password" name="password" type="password" v-model="form.password" placeholder="Password"
          autocomplete="current-password" maxlength="72" :disabled="form.loading" @input="clearError">
      </div>
      
      <div class="error-message" :class="{ show: form.error }">
        {{ form.error }}
      </div>

      <button class="login-btn" type="submit" :disabled="form.loading">
        <span v-if="!form.loading">Login</span>
        <span v-else>Loading...</span>
      </button>
    </form>

    <div class="footer font-blueaka">
      Copyright © 2022&nbsp;
      <a href="https://acg17.shiyq.top">acg17.shiyq.top</a>&nbsp;
      All Rights Reserved&nbsp;
      <a href="http://www.beian.gov.cn/" target="_blank" style="color: #409effd0">豫ICP备2021006771号</a>
    </div>
  </div>
</template>

<script>
import { onMounted, reactive } from "vue"
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import server from '@/util/request'

export default {
  name: 'Login',
  setup() {
    const store = useStore()
    const router = useRouter()

    // 页面挂载前，检查是否已登录，或本地是否有上次的登录信息
    onMounted(async () => {
      // 检查是否已持有访问令牌
      let accessToken = store.state.accessToken
      if (!accessToken) {
        accessToken = localStorage.getItem('accessToken')
        if (!accessToken) return
        store.commit('setAccessToken', accessToken)
      }
      // 存在accessToken，直接进入已登录状态
      // 请求用户信息，更新全局用户信息
      form.loading = true
      try {
        await getUserInfo()
      } catch (error) {
        form.error = getErrorMessage(error)
      } finally {
        form.loading = false
      }
    });

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
    // 表单
    const form = reactive({
      username: '',
      password: '',
      error: '',  // 提示的错误信息
      loading: false,  // 登录和注册结果到来前的loading
    });

    const clearError = () => {
      form.error = '';
    };



    // 检查用户名格式
    function checkUsername() {
      if (form.username.trim().length === 0) {
        form.error = 'Please enter your username.'
      } else {
        form.error = ''
        return true
      }
      return false
    }
    // 检查密码格式
    function checkPassword() {
      if (form.password.length === 0) {
        form.error = 'Please enter your password.'
      } else if (form.password.length < 8 || form.password.length > 72) {
        form.error = 'Passwords must be between 8 to 72 characters.'
      } else {
        form.error = ''
        return true
      }
      return false
    }

    // 登录
    async function login() {
      if (form.loading) return
      clearError();
      // 验证表单，不通过直接结束
      if (!checkUsername() || !checkPassword()) {
        return
      }

      form.loading = true
      const data = {
        username: form.username.trim(),
        password: form.password,
      }
      try {
        const res = await server.post('/user/login', data, { timeout: 15000 })
        store.commit('setAccessToken', res.data.accessToken)
        localStorage.setItem('accessToken', res.data.accessToken)
        await getUserInfo()
      } catch (error) {
        form.error = getErrorMessage(error)
      } finally {
        form.loading = false
      }
    }



    // 请求用户信息
    async function getUserInfo() {
      const res = await server.get('/user-info/getInfo', { timeout: 15000 })
      store.commit('updateUserInfo', res.data)

      const redirect = router.currentRoute.value.query.redirect
      const safeRedirect = typeof redirect === 'string'
        && redirect.startsWith('/')
        && !redirect.startsWith('//')
      await router.replace(safeRedirect ? redirect : { name: 'Index' })
    }

    function getErrorMessage(error) {
      if (typeof error === 'string') return error
      return error?.message || 'Login failed. Please try again.'
    }

    // 跳转到首页
    const goToHome = () => {
      router.push('/')
    }
    return { form, bubbles, clearError, login, goToHome };
  },
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Pacifico&display=swap');

* {
    /* 用于background-container外边距、页脚高度 */
    --m-width: calc(min(3vh, 3vw));
    --m-height: calc(min(4vh, 4vw));
    box-sizing: border-box;
}

.login-page {
    margin: 0;
    padding: 0;
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
    z-index: 1;
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

/* Login Container */
.login-container {
    position: relative;
    z-index: 10;
    width: 400px;
    padding: 40px;
    background: rgba(255, 255, 255, 0.15);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-radius: 20px;
    border: 1px solid rgba(255, 255, 255, 0.4);
    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
    display: flex;
    flex-direction: column;
    align-items: center;
    animation: fadeIn .5s ease-out;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
}

.login-title {
    font-family: 'Pacifico', cursive;
    font-size: 3rem;
    color: #fff;
    margin-bottom: 30px;
    text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}

.input-group {
    width: 100%;
    margin-bottom: 20px;
    position: relative;
}

.visually-hidden {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
}

.input-group input {
    width: 100%;
    padding: 14px 20px 15px;
    background: rgba(255, 255, 255, 0.2);
    border: none;
    outline: none;
    border-radius: 30px;
    color: #fff;
    font-size: 1rem;
    font-family: 'Blueaka', sans-serif;
    line-height: 1.5rem;
    transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    box-shadow: inset 0 2px 5px rgba(0, 0, 0, 0.05);
    backface-visibility: hidden;
}

.input-group input::placeholder {
    color: rgba(255, 255, 255, 0.7);
}

.input-group input:hover, .input-group input:focus {
    background: rgba(255, 255, 255, 0.3);
    box-shadow: 0 8px 25px rgba(255, 255, 255, 0.2), inset 0 2px 5px rgba(0, 0, 0, 0.05);
    transform: translateY(-4px) scale(1.02);
}

.input-group input:focus {
     box-shadow: 0 12px 35px rgba(255, 255, 255, 0.3), inset 0 2px 5px rgba(0, 0, 0, 0.05);
}

.login-btn {
    width: 100%;
    padding: 14px 15px 15px;
    border: none;
    border-radius: 30px;
    background: linear-gradient(135deg, #4facfe, #00f2fe, #4facfe);
    background-size: 200% 200%;
    color: #fff;
    font-size: 1.2rem;
    font-weight: bold;
    cursor: pointer;
    transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
    margin-top: 10px;
    font-family: 'Blueaka', sans-serif;
    line-height: 1.5rem;
    position: relative;
    overflow: hidden;
}

.login-btn::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
    transition: left 0.6s ease;
}

.login-btn:hover {
    background-position: 100% 100%;
    box-shadow: 0 12px 35px rgba(0, 0, 0, 0.3);
    transform: translateY(-4px) scale(1.02);
}

.login-btn:hover::before {
    left: 100%;
}

.login-btn:active {
    transform: translateY(-2px) scale(0.98);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
}

.login-btn:disabled {
    cursor: wait;
    opacity: 0.7;
    transform: none;
}

.error-message {
    width: 100%;
    max-height: 0;
    margin-bottom: 0;
    overflow: hidden;
    background: linear-gradient(to right, #ff8383, #ea4040);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    color: #ff4b2b; /* Fallback */
    font-size: 1rem;
    font-weight: 600;
    font-family: 'Blueaka', sans-serif;
    line-height: 1.5rem;
    text-align: center;
    filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
    transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    opacity: 0;
    transform: translateY(-10px);
}

.error-message.show {
    max-height: 40px;
    margin-bottom: 15px;
    opacity: 1;
    transform: translateY(0);
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
