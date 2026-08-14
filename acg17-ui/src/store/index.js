import { createStore } from 'vuex'
import defaultAvatarUrl from '../assets/avatar.png'

export default createStore({
  state: {
    // 除登录外，所有请求需携带的访问令牌
    accessToken: '',
    // 一些全局用户信息
    userInfo: {
      username: '',
      nickname: '',
      avatarUrl: '',
      illustrationCount: 0,
      illustrationStorageBytes: 0,
      mangaCount: 0,
      mangaStorageBytes: 0,
      novelCount: 0,
      novelWords: 0,
      createTime: '',
    },
    // 上传面板
    uploadDrawer: {
      show: false,
      type: '',
      mode: '',
      context: {},
    },
    // 顶栏
    acg17Header: {
      show: true,
    },
    // 全局loading状态
    globalLoading: false,
    // 页面状态管理
    pageStates: {
      illustration: {
        isRecycle: false,
      },
      manga: {
        isRecycle: false,
      },
      anime: {
        isRecycle: false,
      },
      novel: {
        isRecycle: false,
      },
      game: {
        isRecycle: false,
      },
    }
  },
  mutations: {
    // 设置访问令牌
    setAccessToken(state, newValue) {
      state.accessToken = newValue
    },
    // 更新全局用户信息
    updateUserInfo(state, newValue) {
      if (newValue && Object.keys(newValue).length > 0) {
        for (const key in newValue) {
          if (Object.hasOwnProperty.call(state.userInfo, key))
            state.userInfo[key] = newValue[key]
        }
        state.userInfo.avatarUrl = state.userInfo.avatarUrl || defaultAvatarUrl
      } else {
        // 保持对象引用，只重置属性值以维持响应式
        Object.keys(state.userInfo).forEach(key => {
          if (key === 'username' || key === 'nickname' || key === 'avatarUrl' || key === 'createTime') {
            state.userInfo[key] = ''
          } else {
            state.userInfo[key] = 0
          }
        })
      }
    },
    // 打开上传面板
    openUploadDrawer(state, { type, mode = '', context = {} }) {
      state.uploadDrawer.type = type
      state.uploadDrawer.mode = mode
      state.uploadDrawer.context = context
      state.uploadDrawer.show = true
    },
    closeUploadDrawer(state) {
      state.uploadDrawer.show = false
    },
    // 显示&隐藏顶栏
    toggleAcg17Header(state) {
      state.acg17Header.show = !state.acg17Header.show
    },
    // 切换页面回收站状态
    togglePageRecycleState(state, pageName) {
      if (state.pageStates[pageName]) {
        state.pageStates[pageName].isRecycle = !state.pageStates[pageName].isRecycle
      }
    },
    // 设置页面回收站状态
    setPageRecycleState(state, { pageName, isRecycle }) {
      if (state.pageStates[pageName]) {
        state.pageStates[pageName].isRecycle = isRecycle
      }
    },
    // 显示全局loading
    showGlobalLoading(state) {
      state.globalLoading = true
    },
    // 隐藏全局loading
    hideGlobalLoading(state) {
      state.globalLoading = false
    },
  },
  actions: {
  },
  modules: {
  }
})
