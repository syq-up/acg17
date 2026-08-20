<template>
  <button class="btn-side-menu" @click="drawer = true">
    <icon icon="#icon-menu-1"></icon>
  </button>
  <el-drawer
    v-model="drawer"
    direction="ltr"
    :size="232"
    :with-header="false"
    append-to-body
  >
    <div class="header">
      <button class="btn-side-menu-on" @click="drawer = false">
        <icon icon="#icon-menu-1"></icon>
      </button>
      <h1 class="title">Acg 17</h1>
    </div>
    <ul>
      <li style="margin-left: 24px; font-size: 14px; color: rgb(133, 133, 133); font-weight: bold;">导航</li>
      <li>
        <router-link active-class="active" to="/">
          <div class="item">
            <icon icon="#icon-home"></icon>
            <span>首页</span>
          </div>
        </router-link>
      </li>
      <li style="margin: 8px 0 8px 24px; height: 1px; background-color: #eee"></li>
      <li v-for="item in catalogList" :key="'sidebar-catalog-'+item.index">
        <router-link active-class="active" :to="item.path">
          <div class="item">
            <icon :icon="item.icon"></icon>
            <span v-text="item.title"></span>
          </div>
        </router-link>
      </li>
    </ul>
  </el-drawer>
</template>

<script>
import { ref } from 'vue'

export default {
  name: 'Acg17SideMenu',
  setup() {
    const drawer = ref(false);
    const catalogList = [
      {index: '01', title: "插画", icon: "#icon-illustration", path: "/acg/illustration"},
      {index: '02', title: "漫画", icon: "#icon-manga", path: "/acg/manga"},
      // {index: '03', title: "动画", icon: "#icon-anime", path: "/acg/anime"},
      {index: '03', title: "游戏", icon: "#icon-game", path: "/acg/game"},
      {index: '04', title: "小说", icon: "#icon-novel", path: "/acg/novel"}
    ]
    return { drawer, catalogList };
  }
}
</script>

<style scoped>
.btn-side-menu {
  width: 40px;
  height: 40px;
  padding: 8px;
  margin: 0 -8px;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.2s ease 0s, color 0.2s ease 0s;
  background-color: transparent;
}
.btn-side-menu:hover {
  background-color: rgba(0, 0, 0, 0.04);
}
.btn-side-menu .icon {
  width: 24px;
  height: 24px;
  fill: rgb(133, 133, 133);
}

.header {
  width: 100%;
  height: 64px;
  padding: 12px 24px;
  margin-bottom: 20px;
  display: -webkit-flex;
  display: flex;
  align-items: center;
}
.header .btn-side-menu-on {
  width: 40px;
  height: 40px;
  padding: 8px;
  margin: 0 -8px;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  background-color: rgba(0, 0, 0, 0.04);
}
.btn-side-menu-on .icon {
  width: 24px;
  height: 24px;
  fill: rgb(71, 71, 71);
}
.header .title {
  margin: 0 0 0 20px;
  font-weight: 600;
  font-size: 1.3rem;
  color: #409eff;
}

ul {
  display: flex;
  flex-flow: column;
  list-style: none;
}
ul li a {
  display: block;
  padding: calc(0.5em);
  text-decoration: none;
  background: transparent;
  border: none;
  outline: none;
  touch-action: manipulation;
  user-select: none;
  font-size: inherit;
  line-height: inherit;
  border-radius: 0;
  color: rgba(0, 0, 0, 0.16);
  transition: color 0.2s ease 0s;
}
ul li .item {
  display: flex;
  align-items: center;
  padding: 1px 17px;
}
ul li .item .icon {
  width: 24px;
  height: 24px;
  fill: rgb(133, 133, 133);
  margin-right: 8px;
}
ul li .item span {
  font-size: 16px;
  line-height: 24px;
  color: rgb(71, 71, 71);
}
ul li a:hover {
  background: rgba(0, 0, 0, 0.04);
}
.active {
  background: rgba(0, 0, 0, 0.04);
}
.active .item .icon {
  fill: #409eff;
}
.active .item span {
  color: #409eff;
}
</style>
