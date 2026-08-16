import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
// iconfont
import './assets/iconfont/iconfont.js'
// Blueaka字体
import './assets/fonts/Blueaka/Blueaka.css'
import './assets/fonts/Blueaka_Bold/Blueaka_Bold.css'
// 全局组件
import Icon from './components/Icon';
import LoadingHeart from './components/LoadingHeart';

createApp(App).use(store).use(router).component('icon', Icon).component('loading-heart', LoadingHeart).mount('#app')
