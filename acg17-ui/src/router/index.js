import { createRouter, createWebHashHistory } from 'vue-router'
import store from '../store'
import server from '@/util/request'

const routes = [
  {
    path: '/',
    name: 'Index',
    component: () => import('../views/Index'),
    meta: { title: '首页', authRequired: false, },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login'),
    meta: { title: '登录', authRequired: false, },
  },
  {
    path: '/acg',
    name: 'Acg',
    component: () => import('../views/Acg17Container'),
    meta: { title: 'Acg', authRequired: true, },
    children: [
      {
        path: 'illustration',
        name: 'Illustration',
        component: () => import('../views/Illustration'),
        meta: { title: '插画', },
      },
      {
        path: 'manga',
        name: 'Manga',
        component: () => import('../views/Manga'),
        meta: { title: '漫画', },
      },
      {
        path: 'manga/:id',
        name: 'MangaDetail',
        component: () => import('../views/MangaDetail'),
        meta: { title: '漫画详情', },
      },
      {
        path: 'manga/:id/:chapterNum/:pageNum?',
        name: 'MangaReader',
        component: () => import('../views/MangaReader'),
        meta: { title: '漫画阅读', },
      },
      {
        path: 'anime',
        name: 'Anime',
        component: () => import('../views/Anime'),
        meta: { title: '动画', },
      },
      {
        path: 'novel',
        name: 'Novel',
        component: () => import('../views/Novel'),
        meta: { title: '小说', },
      },
      {
        path: 'novel/:novelId',
        name: 'NovelReader',
        component: () => import('../views/NovelReader'),
        meta: { title: '小说', },
      },
      {
        path: 'game',
        name: 'Game',
        component: () => import('../views/Game'),
        meta: { title: '游戏', },
      },
    ],
    redirect: '/acg/illustration',
  },
  {
    path: '/account',
    component: () => import('../views/Acg17Container'),
    meta: { title: '个人中心', authRequired: true, },
    children: [
      {
        path: '',
        name: 'Account',
        component: () => import('../views/Account'),
        meta: { title: '个人中心', },
      },
    ],
  },
  {
    path: '/random',
    name: 'Random',
    component: () => import('../views/RandomIllustration'),
    meta: { title: '随机插画一张！', authRequired: false, },
  },
  {
    path: '/404',
    name: '404',
    component: () => import('../views/404'),
    meta: { title: '页面走丢了...', authRequired: false, },
  },
  {
    path: '/:pathMatch(.*)',
    redirect: '/404',
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }

    return { top: 0 }
  }
})

// 前置路由守卫
router.beforeEach((to, from, next)=>{
  // 显示全局loading
  store.commit('showGlobalLoading')
  // 页面需要权限，而全局store没有访问令牌时
  if (to.meta.authRequired && !store.state.accessToken) {
    // 查询本地是否有accessToken存储
    const accessToken = localStorage.getItem('accessToken')
    // 有，则进入已登录状态，更新token，并获取用户信息；没有，则跳转到登录页
    if (accessToken) {
      store.commit('setAccessToken', accessToken)
      // 请求用户信息
      server.get('/user-info/getInfo', { timeout: 15000 })
          .then(res=>{
            // 成功获取用户信息后，
            // 更新全局store存储的用户信息
            store.commit('updateUserInfo', res.data)
            // 跳转到指定页面
            document.title = to.meta.title + ' - acg17'
            next()
          })
          .catch(()=>{
            // 获取用户信息失败，跳转到登录页
            next({name: 'Login', query: {redirect: to.fullPath}})
          })
    } else {
      next({name: 'Login', query: {redirect: to.fullPath}})
    }
  } else {
    document.title = to.meta.title + ' - acg17'
    next()
  }
});

// 存储上一页路径
let prevRoutePath = ''
router.$prevRoutePath = () => prevRoutePath

// 后置路由守卫
router.afterEach((to, from) => {
  // 隐藏全局loading
  store.commit('hideGlobalLoading')
  // 保存上一页路径
  prevRoutePath = from.fullPath
})

export default router
