<template>
  <header id="header" :class="$store.state.acg17Header.show ? 'header' : 'header header-hidden'">
    <div class="left">
      <acg17-side-menu></acg17-side-menu>
      <h1 class="title unselectable" @click="goToHome" style="cursor: pointer;">Acg 17</h1>
    </div>
    <div class="center">
      <div class="nav-left unselectable">
        <nav class="nav">
          <router-link v-for="item in navi.list" :key="item.index" :to="item.path"
            :class="{ 'active-navi': isRouteActive(item.path) }">
            {{ item.title }}
          </router-link>
        </nav>
      </div>
      <div class="middle">
        <acg17-search></acg17-search>
      </div>
      <div class="nav-right unselectable">
        <div class="btn-group">
          <button :class="isRecycle ? 'active' : ''" @click="toggleRecycle">
            回收站
          </button>
          <button :class="!isRecycle ? 'active' : ''" @click="() => setRecycle(false)">
            全部
          </button>
        </div>
      </div>

    </div>
    <div class="right unselectable">
      <div class="welcome" style="white-space: nowrap;">
        Hi, <span style="color: #409eff;" v-text="$store.state.userInfo.username"></span>~
      </div>
      <button class="btn-search-mobile">
        <icon icon="#icon-search"></icon>
      </button>
      <acg17-upload></acg17-upload>
      <el-dropdown trigger="click">
        <div class="avatar">
          <img :src="$store.state.userInfo.avatarUrl" alt="avatar">
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>
              <router-link to="/">
                <icon icon="#icon-user"></icon>个人信息
              </router-link>
            </el-dropdown-item>
            <el-dropdown-item divided>
              <router-link to="/">
                <icon icon="#icon-setting"></icon>设置
              </router-link>
            </el-dropdown-item>
            <el-dropdown-item divided @click="logout">
              <a href="javascript:void(0)" style="text-decoration: none; color: inherit;">
                <icon icon="#icon-logout"></icon>退出
              </a>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script>
import { computed } from 'vue';
import Acg17SideMenu from './Acg17SideMenu';
import Acg17Search from './Acg17Search';
import Acg17Upload from "./Acg17Upload";
import { useRecycleState } from '@/composables/useRecycleState';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import server from '@/util/request';

export default {
  name: "Acg17Header",
  components: {
    'acg17-side-menu': Acg17SideMenu,
    'acg17-search': Acg17Search,
    'acg17-upload': Acg17Upload,
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()

    // 导航菜单navi
    const navi = {
      list: [
        { index: '01', title: '插画', path: '/acg/illustration', },
        { index: '02', title: '漫画', path: '/acg/manga', },
        // { index: '03', title: '动画', path: '/acg/anime', },
        { index: '03', title: '游戏', path: '/acg/game', },
        { index: '04', title: '小说', path: '/acg/novel', },
      ]
    }

    // 根据当前路由确定页面名称（响应式）
    const currentPageName = computed(() => {
      const path = route.path
      if (path.includes('/illustration')) return 'illustration'
      if (path.includes('/manga')) return 'manga'
      if (path.includes('/anime')) return 'anime'
      if (path.includes('/novel')) return 'novel'
      if (path.includes('/game')) return 'game'
      return 'illustration' // 默认值
    })

    // 使用回收站状态管理，传入响应式的页面名称
    const { isRecycle, toggleRecycle, setRecycle } = useRecycleState(currentPageName)

    // 退出登录函数
    const logout = async () => {
      try {
        if (store.state.accessToken) {
          await server.post('/user/logout', undefined, { timeout: 10000 })
        }
      } finally {
        localStorage.removeItem('accessToken')
        store.commit('setAccessToken', '')
        store.commit('updateUserInfo', {})
        router.replace('/')
      }
    }

    // 跳转到首页
    const goToHome = () => {
      router.push('/')
    }

    // 判断路由是否激活
    const isRouteActive = (path) => {
      return route.path.startsWith(path)
    }

    return { navi, isRecycle, toggleRecycle, setRecycle, logout, goToHome, isRouteActive }
  }
}
</script>

<style scoped>
.header {
  box-sizing: border-box;
  width: 100vw;
  height: 64px;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 11;
  background-color: #ffffff;
  transition: transform .3s ease-in-out;
  font-family: 'Blueaka', sans-serif;
}

.header.header-hidden {
  transform: translateY(-100%);
}

.header .left,
.header .right {
  flex: 1;
}

.header .right {
  display: flex;
  justify-content: flex-end;
}

.header .right ::v-deep(.btn-upload) {
  padding: 6px 24px;
  border-radius: 8px;
}

.header .right ::v-deep(.btn-upload-content) {
  font-family: 'Blueaka', sans-serif;
}

.header .left {
  display: -webkit-flex;
  display: flex;
  align-items: center;
}

.header .middle {
  position: relative;
}

.header .left .title {
  margin: 0 0 0 20px;
  font-weight: 600;
  font-size: 1.3rem;
  color: #409eff;
  white-space: nowrap;
}

/* 中间区域样式 */
.header .center {
  width: 1380px;
  margin: 0 10px;
  display: flex;
  align-items: center;
}

.header .center .nav-left {
  flex: 1;
  display: flex;
  justify-content: flex-start;
}

.header .center .middle {
  flex: 0 1 528px;
  /* 不放大，可缩小，基础宽度528px */
  min-width: 200px;
  /* 最小宽度，防止过度压缩 */
}

.header .center .nav-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.header .center .nav-right .btn-group {
  display: flex;
  justify-content: flex-end;
}

.header .center .nav-right .btn-group button {
  font-family: 'Blueaka', sans-serif;
}

.header .center .nav-right .btn-group button {
  font-family: 'Blueaka', sans-serif;
  line-height: 1;
}

@media screen and (max-width:715px) {
  .header .center .nav-right {
    display: none;
  }
}

@media screen and (max-width:499px) {
  .header .center .nav {
    display: none;
  }

  .header .center .middle {
    display: none;
  }

  .header .center .btn-group {
    display: none;
  }
}

@media screen and (min-width:500px) {
  .header .middle .title {
    display: none;
  }
}

.header .right {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
  align-items: center;
}

.header .right .btn-search-mobile {
  width: 40px;
  height: 40px;
  padding: 7px;
  margin: 0 -7px;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.2s ease 0s, color 0.2s ease 0s;
  background-color: transparent;
}

.header .right .btn-search-mobile:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.header .right .btn-search-mobile .icon {
  width: 26px;
  height: 26px;
  fill: rgb(133, 133, 133);
}

@media screen and (max-width:1100px) {
  .header .right .welcome {
    display: none;
  }
}

@media screen and (min-width:500px) {
  .header .right .btn-search-mobile {
    display: none;
  }
}

/* 用户头像 start */
.header .right .avatar {
  width: 40px;
  height: 40px;
  cursor: pointer;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header .right .avatar img {
  width: 100%;
  height: 100%;
  border-radius: 8px;
}
/* 用户头像 end */

/* 导航菜单 nav start */
.header .nav {
  display: flex;
  gap: 4px;
}

.header .nav a {
  box-sizing: border-box;
  height: 40px;
  padding: 0 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  font-weight: 500;
  font-size: 14px;
  color: #6b7280;
  transition: all 0.2s ease;
  text-decoration: none;
  cursor: pointer;
  white-space: nowrap;
  position: relative;
}

.header .nav a:hover {
  color: #374151;
  background-color: #f3f4f6;
}

.header .nav .active-navi {
  color: #1f2937;
  background-color: #e5e7eb;
  font-weight: 600;
}

.header .nav .active-navi::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  background-color: #3b82f6;
  border-radius: 2px;
}

@media screen and (max-width:940px) {
  .header .nav {
    display: none;
  }
}

/* 导航菜单 nav end */

/* 按钮组 btn-group start */
.header .btn-group {
  display: flex;
  gap: 6px;
  background-color: #f8fafc;
  padding: 4px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.header .btn-group button {
  box-sizing: border-box;
  height: 32px;
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
  user-select: none;
  cursor: pointer;
  background-color: transparent;
  color: #64748b;
  white-space: nowrap;
}

.header .btn-group button:hover {
  background-color: #e2e8f0;
  color: #475569;
}

.header .btn-group .active {
  background-color: #ffffff;
  color: #1e293b;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.06);
}

.header .btn-group .active:hover {
  background-color: #ffffff;
  color: #1e293b;
}

/* 按钮组 btn-group end */

.el-dropdown-menu__item a {
  display: grid;
  grid-template-columns: repeat(2, max-content);
  gap: 6px;
  align-items: center;
}

.el-dropdown-menu__item .icon {
  width: 20px;
  height: 20px;
  fill: currentColor;
}
</style>
