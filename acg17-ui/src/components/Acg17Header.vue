<template>
  <header
    id="header"
    ref="headerRef"
    class="header"
    :class="headerLayoutClasses"
  >
    <div class="left">
      <acg17-side-menu></acg17-side-menu>
      <h1 class="title unselectable" @click="goToHome" style="cursor: pointer;">Acg 17</h1>
    </div>
    <div ref="centerRef" class="center">
      <div ref="navLeftRef" class="nav-left unselectable">
        <nav class="nav">
          <router-link v-for="item in navi.list" :key="item.index" :to="item.path"
            :class="{ 'active-navi': isRouteActive(item.path) }">
            {{ item.title }}
          </router-link>
        </nav>
      </div>
      <div v-if="!isAccountRoute" ref="navRightRef" class="nav-right unselectable">
        <div class="btn-group">
          <button
            type="button"
            :class="{ active: isRecycle }"
            :aria-pressed="isRecycle"
            @click="toggleRecycle"
          >
            回收站
          </button>
          <button
            type="button"
            :class="{ active: !isRecycle }"
            :aria-pressed="!isRecycle"
            @click="setRecycle(false)"
          >
            全部
          </button>
        </div>
      </div>

    </div>
    <div class="right unselectable">
      <div class="welcome">
        Hi, <span style="color: #409eff;" v-text="displayName"></span>~
      </div>
      <acg17-upload></acg17-upload>
      <el-dropdown trigger="click">
        <div class="avatar">
          <img :src="$store.state.userInfo.avatarUrl" alt="avatar">
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="goToAccount">
              <span class="avatar-menu-item">
                <icon icon="#icon-user"></icon>
                个人中心
              </span>
            </el-dropdown-item>
            <el-dropdown-item @click="logout">
              <span class="avatar-menu-item">
                <icon icon="#icon-logout"></icon>
                退出登录
              </span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import Acg17SideMenu from './Acg17SideMenu';
import Acg17Upload from "./Acg17Upload";
import { useRecycleState } from '@/composables/useRecycleState';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import server from '@/util/request';

const HEADER_LAYOUT_MODE = Object.freeze({
  FULL: 0,
  NO_WELCOME: 1,
  TIGHT: 2,
  NO_NAV: 3,
  DENSE: 4,
  ULTRA: 5,
})

export default {
  name: "Acg17Header",
  components: {
    'acg17-side-menu': Acg17SideMenu,
    'acg17-upload': Acg17Upload,
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()
    const headerRef = ref(null)
    const centerRef = ref(null)
    const navLeftRef = ref(null)
    const navRightRef = ref(null)
    const headerLayoutMode = ref(HEADER_LAYOUT_MODE.FULL)
    let headerResizeObserver = null
    let layoutFrame = 0
    let layoutRequest = 0

    const displayName = computed(() =>
      store.state.userInfo.nickname || store.state.userInfo.username || '用户'
    )
    const isAccountRoute = computed(() => route.path.startsWith('/account'))
    const headerLayoutClasses = computed(() => ({
      'header-hidden': !store.state.acg17Header.show,
      'layout-no-welcome': headerLayoutMode.value >= HEADER_LAYOUT_MODE.NO_WELCOME,
      'layout-tight': headerLayoutMode.value >= HEADER_LAYOUT_MODE.TIGHT,
      'layout-no-nav': headerLayoutMode.value >= HEADER_LAYOUT_MODE.NO_NAV,
      'layout-dense': headerLayoutMode.value >= HEADER_LAYOUT_MODE.DENSE,
      'layout-ultra': headerLayoutMode.value >= HEADER_LAYOUT_MODE.ULTRA,
    }))

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

    function headerLayoutFits() {
      const header = headerRef.value
      const center = centerRef.value
      if (!header || !center) return true

      const visibleGroups = [navLeftRef.value, navRightRef.value]
        .filter(element => element && element.getBoundingClientRect().width > 0)
      const centerGap = Number.parseFloat(window.getComputedStyle(center).columnGap) || 0
      const requiredCenterWidth = visibleGroups.reduce(
        (width, element) => width + element.scrollWidth,
        0,
      ) + Math.max(0, visibleGroups.length - 1) * centerGap

      return requiredCenterWidth <= center.clientWidth + 0.5
        && header.scrollWidth <= header.clientWidth + 0.5
    }

    async function updateHeaderLayout(requestId) {
      for (let mode = HEADER_LAYOUT_MODE.FULL; mode <= HEADER_LAYOUT_MODE.ULTRA; mode += 1) {
        if (requestId !== layoutRequest) return
        headerLayoutMode.value = mode
        await nextTick()
        if (requestId !== layoutRequest) return
        if (headerLayoutFits()) return
      }
    }

    function scheduleHeaderLayout() {
      if (!headerRef.value) return
      layoutRequest += 1
      const requestId = layoutRequest
      if (layoutFrame) window.cancelAnimationFrame(layoutFrame)
      layoutFrame = window.requestAnimationFrame(() => {
        layoutFrame = 0
        updateHeaderLayout(requestId)
      })
    }

    onMounted(() => {
      headerResizeObserver = new ResizeObserver(scheduleHeaderLayout)
      headerResizeObserver.observe(headerRef.value)
      scheduleHeaderLayout()
      document.fonts?.ready.then(scheduleHeaderLayout)
    })

    onUnmounted(() => {
      layoutRequest += 1
      headerResizeObserver?.disconnect()
      if (layoutFrame) window.cancelAnimationFrame(layoutFrame)
    })

    watch(
      [() => route.path, displayName, isAccountRoute, isRecycle],
      async () => {
        await nextTick()
        scheduleHeaderLayout()
      },
      { flush: 'post' },
    )

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

    const goToAccount = () => {
      router.push({ name: 'Account' })
    }

    // 判断路由是否激活
    const isRouteActive = (path) => {
      return route.path.startsWith(path)
    }

    return {
      navi,
      isRecycle,
      toggleRecycle,
      setRecycle,
      logout,
      goToHome,
      goToAccount,
      isRouteActive,
      displayName,
      isAccountRoute,
      headerRef,
      centerRef,
      navLeftRef,
      navRightRef,
      headerLayoutClasses,
    }
  }
}
</script>

<style scoped>
.header {
  --header-padding-x: 24px;
  --header-section-gap: 20px;
  --center-group-gap: 20px;
  --brand-gap: 20px;
  --right-gap: 16px;
  --nav-padding-x: 16px;
  --filter-padding-x: 14px;
  --upload-padding-x: 24px;
  --title-font-size: 1.3rem;
  --avatar-size: 40px;
  --nav-height: 40px;
  --filter-height: 32px;
  --filter-group-padding: 4px;
  --filter-group-radius: 12px;
  box-sizing: border-box;
  width: 100vw;
  height: 64px;
  padding: 12px var(--header-padding-x);
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr) max-content;
  column-gap: var(--header-section-gap);
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

.header .left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.header .left .title {
  margin: 0 0 0 var(--brand-gap);
  font-weight: 600;
  font-size: var(--title-font-size);
  color: #409eff;
  white-space: nowrap;
}

.header .center {
  width: 100%;
  max-width: 1380px;
  min-width: 0;
  justify-self: center;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--center-group-gap);
}

.header .nav-left,
.header .nav-right {
  flex: 0 0 auto;
  display: flex;
  min-width: 0;
}

.header .nav-right {
  justify-content: flex-end;
}

.header .right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--right-gap);
  min-width: 0;
}

.header .right ::v-deep(.btn-upload) {
  padding: 6px var(--upload-padding-x);
  border-radius: 8px;
}

.header .right ::v-deep(.btn-upload-content),
.header .center .nav-right .btn-group button {
  font-family: 'Blueaka', sans-serif;
}

.header .center .nav-right .btn-group button {
  line-height: 1;
}

.header .right .welcome {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header .right .avatar {
  width: var(--avatar-size);
  height: var(--avatar-size);
  cursor: pointer;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header .right .avatar img {
  width: 100%;
  height: 100%;
  border-radius: 8px;
}

.header .nav {
  display: flex;
  gap: 4px;
}

.header .nav a {
  box-sizing: border-box;
  height: var(--nav-height);
  padding: 0 var(--nav-padding-x);
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

.header .btn-group {
  display: flex;
  gap: 6px;
  background-color: #f8fafc;
  padding: var(--filter-group-padding);
  border-radius: var(--filter-group-radius);
  border: 1px solid #e2e8f0;
}

.header .btn-group button {
  box-sizing: border-box;
  height: var(--filter-height);
  padding: 0 var(--filter-padding-x);
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

.header.layout-no-welcome .welcome {
  display: none;
}

.header.layout-tight {
  --header-padding-x: 16px;
  --header-section-gap: 14px;
  --center-group-gap: 12px;
  --brand-gap: 16px;
  --right-gap: 12px;
  --nav-padding-x: 12px;
  --filter-padding-x: 12px;
  --upload-padding-x: 20px;
}

.header.layout-no-nav .nav-left {
  display: none;
}

.header.layout-no-nav .center {
  justify-content: flex-end;
}

.header.layout-dense {
  --header-padding-x: 12px;
  --header-section-gap: 8px;
  --center-group-gap: 8px;
  --brand-gap: 12px;
  --right-gap: 8px;
  --nav-padding-x: 10px;
  --filter-padding-x: 10px;
  --upload-padding-x: 16px;
}

.header.layout-ultra {
  --header-padding-x: 8px;
  --header-section-gap: 4px;
  --center-group-gap: 4px;
  --brand-gap: 8px;
  --right-gap: 6px;
  --nav-padding-x: 8px;
  --filter-padding-x: 8px;
  --upload-padding-x: 12px;
  --title-font-size: 1.05rem;
  --avatar-size: 36px;
  --nav-height: 36px;
  --filter-height: 30px;
  --filter-group-padding: 3px;
  --filter-group-radius: 10px;
}

.avatar-menu-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.avatar-menu-item .icon {
  width: 16px;
  height: 16px;
  fill: currentColor;
}
</style>
