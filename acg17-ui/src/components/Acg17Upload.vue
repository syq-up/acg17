<template>
  <el-dropdown trigger="click">
    <button class="btn-upload" type="button">
      <span class="btn-upload-content">上传作品<icon icon="#icon-down"></icon></span>
    </button>
    <button class="btn-upload-mobile" type="button" title="上传作品">
      <icon icon="#icon-upload"></icon>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="item in uploadCatalog" :key="item.type" @click="openUpload(item.type)">
          <span class="upload-catalog-item">
            <icon :icon="item.icon"></icon>
            {{ item.title }}
          </span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>

  <el-dialog
    v-model="dialog.show"
    :title="activeUpload?.title || '上传作品'"
    width="1000px"
    class="upload-dialog"
    append-to-body
    destroy-on-close
    @closed="resetDialog"
  >
    <div v-if="dialog.show" class="dialog-content">
      <illustration-upload-panel v-if="dialog.type === 'illustration'" />
      <manga-upload-panel v-else-if="dialog.type === 'manga'" @completed="closeDialog" />
      <game-upload-panel v-else-if="dialog.type === 'game'" @completed="closeDialog" />
      <novel-upload-panel
        v-else-if="dialog.type === 'novel'"
        :initial-mode="dialog.mode"
        :novel-context="dialog.context"
      />
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { useStore } from 'vuex'
import IllustrationUploadPanel from './upload/IllustrationUploadPanel.vue'
import MangaUploadPanel from './upload/MangaUploadPanel.vue'
import GameUploadPanel from './upload/GameUploadPanel.vue'
import NovelUploadPanel from './upload/NovelUploadPanel.vue'

const uploadCatalog = [
  { type: 'illustration', title: '上传插画', icon: '#icon-illustration' },
  { type: 'manga', title: '上传漫画', icon: '#icon-manga' },
  { type: 'game', title: '上传游戏', icon: '#icon-game' },
  { type: 'novel', title: '上传小说', icon: '#icon-novel' },
]

const store = useStore()
const dialog = reactive({
  show: false,
  type: '',
  mode: 'novel',
  context: {},
})
const activeUpload = computed(() => uploadCatalog.find(item => item.type === dialog.type))

watch(
  () => store.state.uploadDrawer.show,
  show => {
    if (!show) return

    const request = store.state.uploadDrawer
    openUpload(request.type, {
      mode: request.mode,
      context: request.context,
    })
    store.commit('closeUploadDrawer')
  },
)

function openUpload(type, options = {}) {
  if (!uploadCatalog.some(item => item.type === type)) return

  dialog.type = type
  dialog.mode = options.mode || (type === 'novel' ? 'novel' : '')
  dialog.context = options.context || {}
  dialog.show = true
}

function closeDialog() {
  dialog.show = false
}

function resetDialog() {
  dialog.type = ''
  dialog.mode = 'novel'
  dialog.context = {}
}
</script>

<style scoped>
.btn-upload {
  margin: 0;
  padding: 9px 24px;
  border: none;
  border-radius: 100vw;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
  color: rgba(0, 0, 0, 0.64);
  background-color: rgba(0, 0, 0, 0.04);
}

.btn-upload-content {
  display: grid;
  grid-template-columns: repeat(2, max-content);
  gap: 4px;
  align-items: center;
}

.btn-upload-content .icon {
  width: 10px;
  height: 10px;
  fill: currentColor;
}

.btn-upload-mobile {
  display: block;
  margin: 0;
  padding: 8px;
  border: none;
  border-radius: 100vw;
  cursor: pointer;
  color: rgba(0, 0, 0, 0.64);
  background-color: rgba(0, 0, 0, 0.04);
}

.btn-upload-mobile:hover,
.btn-upload:hover {
  color: #409eff;
  background-color: rgba(64, 158, 255, 0.1);
}

.btn-upload-mobile .icon {
  width: 18px;
  height: 18px;
  fill: currentColor;
}

.upload-catalog-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.upload-catalog-item .icon {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

@media screen and (min-width: 580px) {
  .btn-upload-mobile {
    display: none;
  }
}

@media not screen and (min-width: 580px) {
  .btn-upload {
    display: none;
  }
}
</style>

<style src="./upload/upload-form.css"></style>
