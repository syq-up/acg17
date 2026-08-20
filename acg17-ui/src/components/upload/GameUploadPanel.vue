<template>
  <section class="game-section">
    <form class="modern-form" @submit.prevent="addGame">
      <div class="form-row">
        <div class="form-group">
          <label for="game-name" class="form-label"><span class="required">*</span>游戏名称</label>
          <input
            id="game-name"
            ref="gameNameInput"
            v-model="game.name"
            class="form-input"
            placeholder="请输入游戏名称"
            autocomplete="off"
            required
          >
        </div>
        <div class="form-group">
          <label for="game-chinese-title" class="form-label">中文名称</label>
          <input id="game-chinese-title" v-model="game.chineseTitle" class="form-input" placeholder="请输入中文名称" autocomplete="off">
        </div>
        <div class="form-group">
          <label for="game-version" class="form-label">版本号</label>
          <input id="game-version" v-model="game.version" class="form-input" placeholder="请输入版本号" autocomplete="off">
        </div>
      </div>

      <div class="game-details-row">
        <div class="game-images-row">
          <div class="form-group">
            <label class="form-label"><span class="required">*</span>游戏封面</label>
            <div class="game-cover-container">
              <input ref="coverInput" type="file" accept="image/*" hidden @change="selectSingleImage('cover', $event)">
              <div
                v-if="!game.cover.file"
                class="cover-upload-card"
                :class="{ dragover: dragging.cover }"
                @click="openFileDialog(coverInput)"
                @dragenter.prevent="dragging.cover = true"
                @dragover.prevent
                @dragleave.prevent="dragging.cover = false"
                @drop.prevent="dropSingleImage('cover', $event)"
              >
                <div class="upload-placeholder">
                  <div class="upload-icon"><icon icon="#icon-upload"></icon></div>
                  <div class="upload-text">
                    <p class="primary-text">点击上传游戏封面</p>
                    <p class="hint-text">支持 JPG、PNG 格式，不超过 10MB</p>
                  </div>
                </div>
              </div>
              <div
                v-else
                class="cover-avatar"
                :class="{ dragover: dragging.cover }"
                @dragenter.prevent="dragging.cover = true"
                @dragover.prevent
                @dragleave.prevent="dragging.cover = false"
                @drop.prevent="dropSingleImage('cover', $event)"
              >
                <img :src="game.cover.url" alt="游戏封面" class="cover-avatar-img">
                <div class="cover-avatar-actions">
                  <button class="action-btn" type="button" title="更换封面" @click="openFileDialog(coverInput)">
                    <icon icon="#icon-replace"></icon>
                  </button>
                  <button class="action-btn delete-btn" type="button" title="删除封面" @click="removeSingleImage('cover')">
                    <icon icon="#icon-delete"></icon>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">游戏图标</label>
            <div class="game-cover-container">
              <input ref="iconInput" type="file" accept="image/*" hidden @change="selectSingleImage('icon', $event)">
              <div
                v-if="!game.icon.file"
                class="cover-upload-card"
                :class="{ dragover: dragging.icon }"
                @click="openFileDialog(iconInput)"
                @dragenter.prevent="dragging.icon = true"
                @dragover.prevent
                @dragleave.prevent="dragging.icon = false"
                @drop.prevent="dropSingleImage('icon', $event)"
              >
                <div class="upload-placeholder">
                  <div class="upload-icon"><icon icon="#icon-upload"></icon></div>
                  <div class="upload-text">
                    <p class="primary-text">点击上传游戏图标</p>
                    <p class="hint-text">支持 JPG、PNG 格式，不超过 1MB</p>
                  </div>
                </div>
              </div>
              <div
                v-else
                class="cover-avatar"
                :class="{ dragover: dragging.icon }"
                @dragenter.prevent="dragging.icon = true"
                @dragover.prevent
                @dragleave.prevent="dragging.icon = false"
                @drop.prevent="dropSingleImage('icon', $event)"
              >
                <img :src="game.icon.url" alt="游戏图标" class="cover-avatar-img">
                <div class="cover-avatar-actions">
                  <button class="action-btn" type="button" title="更换图标" @click="openFileDialog(iconInput)">
                    <icon icon="#icon-replace"></icon>
                  </button>
                  <button class="action-btn delete-btn" type="button" title="删除图标" @click="removeSingleImage('icon')">
                    <icon icon="#icon-delete"></icon>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="form-group game-description-group">
          <label for="game-description" class="form-label">游戏简介</label>
          <textarea
            id="game-description"
            v-model="game.description"
            class="form-textarea"
            placeholder="请输入游戏简介"
            rows="5"
          ></textarea>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label">游戏预览</label>
        <div class="game-preview-wall">
          <input ref="previewInput" type="file" accept="image/*" multiple hidden @change="selectPreviewImages">
          <button
            class="preview-upload-card"
            :class="{ dragover: dragging.preview }"
            type="button"
            @click="openFileDialog(previewInput)"
            @dragenter.prevent="dragging.preview = true"
            @dragover.prevent
            @dragleave.prevent="dragging.preview = false"
            @drop.prevent="dropPreviewImages"
          >
            <span class="upload-placeholder">
              <span class="upload-icon"><icon icon="#icon-upload"></icon></span>
              <span class="upload-text">
                <span class="primary-text">点击添加预览图片</span>
                <span class="hint-text">支持 JPG、PNG 格式</span>
              </span>
            </span>
          </button>

          <div v-for="(preview, index) in game.previews" :key="preview.url" class="preview-card">
            <img :src="preview.url" alt="游戏预览" class="preview-card-img" @click="openPreview(preview.url)">
            <div class="preview-card-actions">
              <button class="action-btn preview-btn" type="button" title="预览" @click="openPreview(preview.url)">
                <icon icon="#icon-view"></icon>
              </button>
              <button class="action-btn delete-btn" type="button" title="删除" @click="removePreview(index)">
                <icon icon="#icon-delete"></icon>
              </button>
            </div>
          </div>
        </div>

        <div v-if="previewUrl" class="image-preview-dialog" @click="closePreview">
          <div class="preview-content" @click.stop>
            <img :src="previewUrl" alt="预览图片" class="preview-full-image">
            <button class="close-preview-btn" type="button" @click="closePreview">
              <icon icon="#icon-close"></icon>
            </button>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <span v-if="game.error" class="error-message">{{ game.error }}</span>
        <button class="submit-btn" type="submit" :disabled="game.uploading">
          <icon icon="#icon-upload"></icon>
          {{ game.uploading ? '上传中...' : '上传游戏' }}
        </button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { onUnmounted, reactive, ref } from 'vue'
import server from '@/util/request'

const emit = defineEmits(['completed'])

const gameNameInput = ref(null)
const coverInput = ref(null)
const iconInput = ref(null)
const previewInput = ref(null)
const previewUrl = ref('')
const dragging = reactive({ cover: false, icon: false, preview: false })

const game = reactive({
  name: '',
  chineseTitle: '',
  version: '',
  description: '',
  cover: { file: null, url: '' },
  icon: { file: null, url: '' },
  previews: [],
  error: '',
  uploading: false,
})

onUnmounted(releaseObjectUrls)

function openFileDialog(input) {
  if (!input) return
  input.value = ''
  input.click()
}

function selectSingleImage(kind, event) {
  const file = event.target.files[0]
  if (file) processSingleImage(kind, file)
}

function dropSingleImage(kind, event) {
  dragging[kind] = false
  const file = event.dataTransfer.files[0]
  if (file) processSingleImage(kind, file)
}

function processSingleImage(kind, file) {
  const maxSizeMb = kind === 'icon' ? 1 : 10
  if (!validateImage(file, maxSizeMb)) return

  revokeUrl(game[kind].url)
  game[kind].file = file
  game[kind].url = URL.createObjectURL(file)
  ElMessage.success(kind === 'cover' ? '游戏封面已选择' : '游戏图标已选择')
}

function removeSingleImage(kind) {
  revokeUrl(game[kind].url)
  game[kind].file = null
  game[kind].url = ''
}

function selectPreviewImages(event) {
  addPreviewImages(Array.from(event.target.files))
}

function dropPreviewImages(event) {
  dragging.preview = false
  addPreviewImages(Array.from(event.dataTransfer.files))
}

function addPreviewImages(files) {
  for (const file of files) {
    if (validateImage(file, 10)) {
      game.previews.push({ file, url: URL.createObjectURL(file) })
    }
  }
}

function validateImage(file, maxSizeMb) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  if (file.size >= maxSizeMb * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${maxSizeMb}MB！`)
    return false
  }
  return true
}

function removePreview(index) {
  revokeUrl(game.previews[index]?.url)
  game.previews.splice(index, 1)
}

function openPreview(url) {
  previewUrl.value = url
}

function closePreview() {
  previewUrl.value = ''
}

async function addGame() {
  const name = game.name.trim()
  if (!name) {
    game.error = '「游戏名称」不能为空'
    gameNameInput.value?.focus()
    return
  }
  if (!game.cover.file) {
    game.error = '请选择「游戏封面」'
    return
  }

  const formData = new FormData()
  formData.append('title', name)
  formData.append('chineseTitle', game.chineseTitle.trim())
  formData.append('version', game.version.trim())
  formData.append('description', game.description.trim())
  formData.append('cover', game.cover.file)
  if (game.icon.file) formData.append('icon', game.icon.file)
  for (const preview of game.previews) {
    formData.append('previewImages', preview.file)
  }

  game.error = ''
  game.uploading = true
  try {
    const response = await server.post('/game/addGame', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    if (response.code !== 200) {
      throw new Error(response.message || '上传失败')
    }
    ElMessage.success(`游戏「${name}」上传成功！`)
    emit('completed')
  } catch (error) {
    console.error('上传游戏失败:', error)
    ElMessage.error(`上传游戏失败: ${error.response?.data?.message || error.message}`)
  } finally {
    game.uploading = false
  }
}

function releaseObjectUrls() {
  revokeUrl(game.cover.url)
  revokeUrl(game.icon.url)
  for (const preview of game.previews) revokeUrl(preview.url)
}

function revokeUrl(url) {
  if (url) URL.revokeObjectURL(url)
}
</script>

<style scoped>
.game-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.game-details-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 12px;
}

.game-images-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

/* 游戏封面容器 - Element Plus Avatar 风格 */
.game-cover-container {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cover-upload-card {
  box-sizing: border-box;
  width: 100%;
  height: 130px;
  border: 2px dashed #d1d5db;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  position: relative;
  overflow: hidden;
  pointer-events: auto;
}

.cover-upload-card::before,
.preview-upload-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, rgba(var(--upload-primary-rgb), 0.05) 0%, transparent 70%);
  z-index: 0;
}

.cover-upload-card::after,
.preview-upload-card::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: -45%;
  width: 36%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.55), transparent);
  transform: skewX(-18deg) translateX(-180%);
  transition: transform 0.7s ease;
  pointer-events: none;
  z-index: 0;
}

.cover-upload-card:hover,
.preview-upload-card:hover {
  border-color: var(--upload-primary);
  background: linear-gradient(135deg, #f5faff 0%, var(--upload-primary-soft) 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(var(--upload-primary-rgb), 0.15);
}

.cover-upload-card:hover::after,
.preview-upload-card:hover::after {
  transform: skewX(-18deg) translateX(520%);
}

.cover-upload-card.dragover,
.preview-upload-card.dragover {
  border-color: var(--upload-primary);
  background: linear-gradient(135deg, var(--upload-primary-soft) 0%, var(--upload-primary-soft-strong) 100%);
  transform: scale(1.02);
  box-shadow: 0 12px 35px rgba(var(--upload-primary-rgb), 0.25);
}

.cover-upload-card * {
  pointer-events: none;
}

.upload-placeholder {
  box-sizing: border-box;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  color: #64748b;
}

.upload-placeholder .upload-icon {
  box-sizing: border-box;
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  margin-bottom: 8px;
  padding: 10px;
  background: var(--upload-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(var(--upload-primary-rgb), 0.3);
  transition: all 0.3s ease;
}

.upload-placeholder .upload-icon .icon {
  width: 24px;
  height: 24px;
  fill: white;
}

.cover-upload-card:hover .upload-icon,
.preview-upload-card:hover .upload-icon {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 6px 20px rgba(var(--upload-primary-rgb), 0.4);
}

.cover-upload-card.dragover .upload-icon,
.preview-upload-card.dragover .upload-icon {
  transform: scale(1.08) rotate(-4deg);
}

.upload-placeholder .upload-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: center;
  color: #64748b;
}

.upload-placeholder .upload-text p,
.upload-placeholder .upload-text span {
  margin: 0;
  line-height: 1.4;
}

.upload-placeholder .primary-text {
  color: #1e293b;
  font-size: 14px;
  font-weight: 600;
}

.upload-placeholder .hint-text {
  padding: 6px 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  color: #94a3b8;
  font-size: 11px;
}

.cover-avatar {
  box-sizing: border-box;
  position: relative;
  width: 100%;
  height: 130px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  transition: all 0.3s;
}

.cover-avatar:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.cover-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.cover-avatar-actions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.cover-avatar:hover .cover-avatar-actions {
  opacity: 1;
}

.action-btn {
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  color: #606266;
}

.action-btn:hover {
  background: #fff;
  transform: scale(1.1);
}

.action-btn.delete-btn:hover {
  color: #f56c6c;
}

.action-btn .icon {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

/* 游戏预览照片墙 - Element Plus Upload 照片墙风格 */
.game-preview-wall {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 8px;
}

.preview-upload-card {
  box-sizing: border-box;
  width: 100%;
  height: auto;
  aspect-ratio: 22 / 15;
  border: 2px dashed #d1d5db;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  position: relative;
  overflow: hidden;
  order: -1;
  pointer-events: auto;
  font: inherit;
}

.preview-upload-card .upload-icon {
  width: 48px;
  height: 48px;
}

.preview-upload-card * {
  pointer-events: none;
}

.preview-upload-card .upload-text,
.preview-upload-card .upload-placeholder {
  display: flex;
  flex-direction: column;
}

.preview-card {
  box-sizing: border-box;
  position: relative;
  width: 100%;
  height: auto;
  aspect-ratio: 22 / 15;
  border: 1px solid #c0c4cc;
  border-radius: 6px;
  overflow: hidden;
  transition: all 0.3s;
}

.preview-card:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.preview-card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s;
}

.preview-card:hover .preview-card-img {
  transform: scale(1.05);
}

.preview-card-actions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.preview-card:hover .preview-card-actions {
  opacity: 1;
}

.preview-btn:hover {
  color: var(--upload-primary);
}

/* 图片预览对话框 */
.image-preview-dialog {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  cursor: pointer;
}

.preview-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  cursor: default;
}

.preview-full-image {
  max-width: 100%;
  max-height: 100%;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.close-preview-btn {
  position: absolute;
  top: -40px;
  right: 0;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: #606266;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.close-preview-btn:hover {
  background: #fff;
  color: var(--upload-primary);
}

.close-preview-btn .icon {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

@media (max-width: 768px) {
  .game-details-row,
  .game-images-row {
    grid-template-columns: 1fr;
  }

  .game-preview-wall {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  }
}

@media (prefers-reduced-motion: reduce) {
  .cover-upload-card,
  .preview-upload-card,
  .upload-placeholder .upload-icon,
  .cover-upload-card::after,
  .preview-upload-card::after {
    transition: none;
  }

  .cover-upload-card:hover,
  .preview-upload-card:hover,
  .cover-upload-card.dragover,
  .preview-upload-card.dragover,
  .cover-upload-card:hover .upload-icon,
  .preview-upload-card:hover .upload-icon,
  .cover-upload-card.dragover .upload-icon,
  .preview-upload-card.dragover .upload-icon {
    transform: none;
  }
}
</style>
