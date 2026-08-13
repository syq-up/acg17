import { computed } from 'vue'
import { useStore } from 'vuex'

/**
 * 可复用的回收站状态管理组合函数
 * @param {string|Ref<string>} pageName - 页面名称 (illustration, manga, anime, novel) 或响应式引用
 * @returns {object} 包含回收站状态和切换方法的对象
 */
export function useRecycleState(pageName) {
  const store = useStore()
  
  // 处理响应式和非响应式的页面名称
  const pageNameRef = computed(() => {
    return typeof pageName === 'string' ? pageName : pageName.value
  })

  // 计算属性：获取当前页面的回收站状态
  const isRecycle = computed({
    get: () => store.state.pageStates[pageNameRef.value]?.isRecycle || false,
    set: (value) => store.commit('setPageRecycleState', { pageName: pageNameRef.value, isRecycle: value })
  })

  // 切换回收站状态的方法
  const toggleRecycle = () => {
    store.commit('togglePageRecycleState', pageNameRef.value)
  }

  // 设置回收站状态的方法
  const setRecycle = (value) => {
    store.commit('setPageRecycleState', { pageName: pageNameRef.value, isRecycle: value })
  }

  return {
    isRecycle,
    toggleRecycle,
    setRecycle
  }
}

/**
 * 获取API端点的辅助函数
 * @param {string} basePath - 基础路径 (如 '/illustration', '/novel')
 * @param {boolean} isRecycle - 是否为回收站
 * @returns {string} 完整的API端点
 */
export function getApiEndpoint(basePath, isRecycle) {
  return basePath + (isRecycle ? '/getRecycleList' : '/getList')
}

/**
 * 通用的数据加载函数
 * @param {object} options - 配置选项
 * @param {string} options.basePath - API基础路径
 * @param {boolean} options.isRecycle - 是否为回收站
 * @param {number} options.pageNum - 页码
 * @param {function} options.server - 请求函数
 * @returns {Promise} 请求Promise
 */
export function loadData({ basePath, isRecycle, pageNum, server }) {
  const endpoint = getApiEndpoint(basePath, isRecycle)
  const params = { pageNum }
  return server.get(endpoint, { params })
}