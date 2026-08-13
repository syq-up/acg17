<template>
  <el-popover :width="528" trigger="manual" v-model:visible="popover">
    <template #reference>
      <div class="out">
        <input placeholder="搜索作品" @focus="showPopover">
        <icon icon="#icon-search"></icon>
      </div>
    </template>
    <div>
      <div class="history">
        <div class="list-header">
          <span>记录</span>
          <button class="right">清除记录</button>
        </div>
        <div class="list">
          <div class="item">
            <span>幼井osanai</span>
            <icon icon="#icon-to"></icon>
          </div>
          <div class="item">
            <span>loli</span>
            <icon icon="#icon-to"></icon>
          </div>
        </div>
      </div>
    </div>
  </el-popover>
  <transition name="fade">
    <div id="searchShade" v-show="shade" @click="hidePopover"></div>
  </transition>
</template>

<script>
import { onMounted, ref } from 'vue'

export default {
  name: 'Acg17Search',
  setup() {
    const popover = ref(false)
    const shade = ref(false)
    // 放到app下，不遮盖header
    onMounted(() => {
      document.getElementById('app').appendChild(document.getElementById('searchShade'))
    })
    function showPopover() {
      popover.value = true
      shade.value = true
      document.body.classList.add("ban-scroll")
    }
    function hidePopover() {
      popover.value = false
      shade.value = false
      document.body.classList.remove("ban-scroll")
    }
    return { popover, shade, showPopover, hidePopover }
  }
}
</script>

<style scoped>
.out input {
  box-sizing: border-box;
  width: 100%;
  height: 40px;
  padding: 9px 8px 9px 36px;
  font-size: 14px;
  line-height: 22px;
  outline: none;
  border: none;
  border-radius: 4px;
  color: rgb(71, 71, 71);
  background-color: rgba(0, 0, 0, 0.04);
  transition: background-color 0.2s ease 0s, color 0.2s ease 0s;
}
.out input::placeholder {
  font-family: 'Blueaka', sans-serif;
}
.out input::-webkit-input-placeholder {
  font-family: 'Blueaka', sans-serif;
}
.out input:hover {
  background-color: rgba(0, 0, 0, 0.08);
}
.out input:focus {
  border: 2px solid #409eff;
}
.out .icon {
  width: 16px;
  height: 16px;
  fill: rgb(173, 173, 173);
  position: absolute;
  top: 12px;
  left: 12px;
}
#searchShade {
  width: 100vw;
  height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 9;
  background-color: rgba(0, 0, 0, 0.4);
}
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
.fade-enter-active {
  animation: fadeIn .2s linear;
}
.fade-leave-active {
  animation: fadeIn .2s linear reverse;
}
.history {
  padding: 16px 0 24px;
}
.history .list-header {
  padding: 0 16px 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.history .list-header span {
  font-size: 14px;
  font-weight: bold;
  color: rgb(71, 71, 71);
}
.history .list-header button {
  padding: 0;
  cursor: pointer;
  outline: none;
  border: none;
  font-size: 14px;
  color: rgb(133, 133, 133);
  background-color: transparent;
}
.history .list .item {
  box-sizing: border-box;
  width: 100%;
  height: 40px;
  padding: 0 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: none;
  cursor: pointer;
}
.history .list .item:hover {
  background-color: rgba(0, 0, 0, 0.04);
}
.history .list .item span {
  font-size: 14px;
  color: rgb(71, 71, 71);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.history .list .item .icon {
  width: 24px;
  height: 24px;
  fill: rgb(173, 173, 173);
}
</style>
