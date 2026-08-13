<template>
  <router-view/>
  <!-- 全局loading组件 -->
  <div v-show="$store.state.globalLoading" class="global-loading-overlay">
    <loading-heart />
  </div>
</template>

<style>
html, body {
  scroll-behavior: smooth;
}
html, body, ul, li, th, td, dl, dd, h1, h2, h3, h4 {
  margin: 0;
  padding: 0;
}
ul, li {
  list-style: none;
}
a, a:focus, a:hover {
  cursor: pointer;
  color: inherit;
  text-decoration: none;
}
.ban-scroll {
  overflow-x: hidden;
  overflow-y: hidden;
}
/* 全局loading样式 */
.global-loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(255, 255, 255, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(2px);
}
.global-loading-overlay .loading {
  transform: scale(2);
  filter: drop-shadow(0 6px 12px rgba(0, 0, 0, 0.4));
}
/* 全局图标样式 */
.icon {
  width: 1em;
  height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
  display: inline-block;
}
/* 全局不可选中样式 */
.unselectable {
  user-select: none;
}
 /* 全局字体样式 */
.font-blueaka {
  font-family: 'Blueaka', sans-serif;
 }


/* element-plus组件 自定义样式 */
.el-drawer__body {
  padding: 0 !important;
}
.el-popper {
  padding: 0 !important;
}
</style>
