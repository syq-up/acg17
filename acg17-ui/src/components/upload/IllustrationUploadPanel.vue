<template>
  <section class="upload-section">
    <el-upload
      class="upload-area"
      drag
      multiple
      :show-file-list="false"
      accept="image/*"
      :before-upload="beforeUpload"
      :http-request="uploadIllustration"
    >
      <div class="upload-content">
        <div class="upload-icon">
          <icon icon="#icon-upload"></icon>
        </div>
        <div class="upload-text">
          <p class="primary-text">拖拽图片到此处上传</p>
          <p class="secondary-text">或 <em>点击选择文件</em></p>
          <p class="hint-text">支持 JPG、PNG、GIF 格式，单个文件不超过 100MB</p>
        </div>
      </div>
    </el-upload>

    <div v-if="images.length" class="uploaded-images">
      <h4>已上传的图片</h4>
      <div class="image-grid">
        <div v-for="image in images" :key="image.id" class="image-item">
          <img :src="image.url" alt="">
          <div class="image-overlay">
            <button class="remove-btn" type="button" @click="removeImage(image.id)">
              <icon icon="#icon-close"></icon>
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import server from '@/util/request'
import uploadLoadingUrl from '@/assets/icon/upload/loading.svg'

const images = ref([])

function beforeUpload(file) {
  if (file.size > 100 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 100MB')
    return false
  }

  images.value.push({ id: String(file.uid), url: uploadLoadingUrl })
  return true
}

async function uploadIllustration({ file }) {
  const temporaryId = String(file.uid)
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await server.post('/illustration/upload', formData)
    const image = images.value.find(item => item.id === temporaryId)
    if (image) {
      image.id = res.data.id
      image.url = res.data.thumbnailUrl
    }
  } catch (error) {
    images.value = images.value.filter(item => item.id !== temporaryId)
    ElMessage.error(`上传失败【${error}】，请重试`)
  }
}

function removeImage(id) {
  images.value = images.value.filter(image => image.id !== id)
}
</script>

<style scoped>
/* 插画上传区域样式 */
.upload-section {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.upload-area {
  width: 100%;
  position: relative;
}

.upload-area ::v-deep(.el-upload) {
  width: 100%;
}

.upload-area ::v-deep(.el-upload-dragger) {
  box-sizing: border-box;
  width: 100%;
  height: 200px;
  padding: 0;
  border: 2px dashed #d1d5db;
  border-radius: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.upload-area ::v-deep(.el-upload-dragger:hover) {
  border-color: var(--upload-primary);
  background: linear-gradient(135deg, #f5faff 0%, var(--upload-primary-soft) 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(var(--upload-primary-rgb), 0.15);
}

.upload-area ::v-deep(.el-upload-dragger.is-dragover) {
  border-color: var(--upload-primary);
  background: linear-gradient(135deg, var(--upload-primary-soft) 0%, var(--upload-primary-soft-strong) 100%);
  transform: scale(1.02);
  box-shadow: 0 12px 35px rgba(var(--upload-primary-rgb), 0.25);
}

.upload-area ::v-deep(.el-upload-dragger)::after {
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
  z-index: 1;
}

.upload-area ::v-deep(.el-upload-dragger:hover)::after {
  transform: skewX(-18deg) translateX(520%);
}

.upload-content {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 12px 24px;
  text-align: center;
  position: relative;
  z-index: 2;
}

.upload-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at center, rgba(var(--upload-primary-rgb), 0.05) 0%, transparent 70%);
  z-index: -1;
}

.upload-icon {
  box-sizing: border-box;
  flex: 0 0 auto;
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--upload-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(var(--upload-primary-rgb), 0.3);
  transition: all 0.3s ease;
}

.upload-icon .icon {
  width: 24px;
  height: 24px;
  fill: white;
}

.upload-area:hover .upload-icon {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 6px 20px rgba(var(--upload-primary-rgb), 0.4);
}

.upload-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.upload-text .primary-text {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  line-height: 1.4;
}

.upload-text .secondary-text {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  line-height: 1.5;
}

.upload-text .secondary-text em {
  color: var(--upload-primary);
  font-style: normal;
  font-weight: 600;
  text-decoration: underline;
  text-decoration-color: rgba(var(--upload-primary-rgb), 0.3);
  text-underline-offset: 2px;
}

.upload-text .hint-text {
  font-size: 12px;
  color: #94a3b8;
  margin: 8px 0 0 0;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

/* 已上传图片展示 */
.uploaded-images {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #e2e8f0;
}

.uploaded-images h4 {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 20px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.uploaded-images h4::before {
  content: '';
  width: 4px;
  height: 20px;
  background: var(--upload-primary);
  border-radius: 2px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.image-item {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 1;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
}

.image-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  border-color: rgba(var(--upload-primary-rgb), 0.3);
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: all 0.3s ease;
}

.image-item:hover img {
  transform: scale(1.05);
  filter: brightness(1.1);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.6) 0%, rgba(0, 0, 0, 0.4) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(2px);
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.remove-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
  position: relative;
}

.remove-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, transparent 100%);
  pointer-events: none;
}

.remove-btn:hover {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(239, 68, 68, 0.6);
}

.remove-btn:active {
  transform: scale(0.95);
}

.remove-btn .icon {
  width: 18px;
  height: 18px;
  fill: currentColor;
  z-index: 1;
}

@media (max-width: 768px) {
  .upload-area .upload-content {
    padding: 4px 16px;
  }

  .upload-area .upload-icon {
    padding: 10px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .upload-area ::v-deep(.el-upload-dragger),
  .upload-area ::v-deep(.el-upload-dragger)::after,
  .upload-area .upload-icon {
    transition: none;
  }

  .upload-area ::v-deep(.el-upload-dragger:hover),
  .upload-area ::v-deep(.el-upload-dragger.is-dragover),
  .upload-area:hover .upload-icon {
    transform: none;
  }
}
</style>
