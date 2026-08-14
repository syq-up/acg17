<template>
  <section class="manga-section">
    <div class="tab-header">
      <button class="tab-btn" :class="{ active: mode === 'manga' }" type="button" @click="switchToManga">
        <icon icon="#icon-manga"></icon>
        新增漫画
      </button>
      <button class="tab-btn" :class="{ active: mode === 'chapter' }" type="button" @click="switchToChapter">
        <icon icon="#icon-manga"></icon>
        新增章节
      </button>
    </div>

    <form v-if="mode === 'manga'" class="modern-form" @submit.prevent="addManga">
      <div class="form-row">
        <div class="form-group">
          <label for="manga-title" class="form-label"><span class="required">*</span>标题</label>
          <input
            id="manga-title"
            ref="mangaTitleInput"
            v-model="manga.title"
            class="form-input"
            placeholder="请输入漫画标题"
            autocomplete="off"
            required
          >
        </div>
        <div class="form-group">
          <label for="manga-title-cn" class="form-label">中文标题</label>
          <input id="manga-title-cn" v-model="manga.chineseTitle" class="form-input" placeholder="请输入中文标题" autocomplete="off">
        </div>
      </div>

      <div class="form-group">
        <div class="tag-header">
          <label class="form-label">标签分类</label>
          <div class="tag-mode-toggle">
            <button class="toggle-btn" :class="{ active: manga.tagMode === 'input' }" type="button" @click="manga.tagMode = 'input'">
              输入
            </button>
            <button class="toggle-btn" :class="{ active: manga.tagMode === 'select' }" type="button" @click="manga.tagMode = 'select'">
              选择
            </button>
          </div>
        </div>

        <div class="tag-categories">
          <div v-for="group in tagGroups" :key="group.key" class="tag-category">
            <h5>{{ group.label }}</h5>
            <div v-if="manga.tagMode === 'input'" class="tag-input-container">
              <input
                v-model="group.input"
                class="tag-input"
                :placeholder="group.placeholder"
                @keyup.enter.prevent="addTag(group)"
              >
              <button class="add-tag-btn" type="button" @click="addTag(group)">添加</button>
            </div>
            <div class="tag-display-area">
              <div class="selected-tags">
                <button
                  v-for="tag in group.selected"
                  :key="tag.tagId"
                  class="selected-tag"
                  type="button"
                  @click="removeTag(group, tag)"
                >
                  {{ tag.tagName }}
                </button>
              </div>
              <div v-if="manga.tagMode === 'select'" class="available-tags">
                <button
                  v-for="tag in group.available"
                  :key="tag.tagId"
                  class="available-tag"
                  :class="{ selected: isTagSelected(group, tag) }"
                  type="button"
                  @click="toggleTag(group, tag)"
                >
                  {{ tag.tagName }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="form-row manga-file-row">
        <div class="form-group">
          <label class="form-label"><span class="required">*</span>漫画文件</label>
          <div
            class="custom-upload"
            :class="{ dragover: manga.dragCounter > 0 }"
            @click="mangaFileInput?.click()"
            @dragenter.prevent="startDrag(manga)"
            @dragover.prevent
            @dragleave.prevent="endDrag(manga)"
            @drop.prevent="dropFile(manga, $event, '漫画文件')"
          >
            <input ref="mangaFileInput" type="file" accept=".zip" hidden @click.stop @change="selectFile(manga, $event, '漫画文件')">
            <div class="upload-content">
              <div class="upload-icon"><icon icon="#icon-upload"></icon></div>
              <div class="upload-text">
                <p class="primary-text">拖拽漫画文件到此处上传</p>
                <p class="secondary-text">或 <em>点击选择文件</em></p>
                <p class="hint-text">仅支持 ZIP 格式，单个文件不超过 500MB</p>
              </div>
            </div>
          </div>
        </div>

        <div class="form-group upload-submit-group">
          <div v-if="manga.selectedFile" class="file-list selected-file-list">
            <div class="file-item">
              <span class="file-name">{{ manga.selectedFile.name }}</span>
              <button class="remove-btn" type="button" @click="removeSelectedFile(manga, mangaFileInput)">
                <icon icon="#icon-delete"></icon>
              </button>
            </div>
          </div>
          <div class="form-actions">
            <span v-if="manga.error" class="error-message">{{ manga.error }}</span>
            <button class="submit-btn" type="submit" :disabled="manga.uploading">
              <icon icon="#icon-upload"></icon>
              {{ manga.uploading ? '上传中...' : '上传漫画' }}
            </button>
          </div>
        </div>
      </div>
    </form>

    <form v-else class="modern-form" @submit.prevent="addChapter">
      <div class="form-row manga-file-row">
        <div class="form-group">
          <label for="manga-chapter-target" class="form-label"><span class="required">*</span>漫画</label>
          <el-select
            id="manga-chapter-target"
            v-model="chapter.mangaId"
            class="manga-select"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="输入标题搜索漫画"
            :remote-method="searchMangas"
            :loading="chapter.searching"
            @visible-change="handleMangaSelectVisible"
          >
            <el-option
              v-for="option in chapter.mangaOptions"
              :key="option.id"
              :label="mangaOptionLabel(option)"
              :value="option.id"
            />
          </el-select>
        </div>

        <div class="form-group">
          <label for="manga-chapter-title" class="form-label"><span class="required">*</span>章节标题</label>
          <input
            id="manga-chapter-title"
            ref="chapterTitleInput"
            v-model="chapter.title"
            class="form-input"
            placeholder="请输入章节标题"
            autocomplete="off"
            required
          >
        </div>
      </div>

      <div class="form-group">
        <label class="form-label"><span class="required">*</span>漫画文件</label>
        <div
          class="custom-upload"
          :class="{ dragover: chapter.dragCounter > 0 }"
          @click="chapterFileInput?.click()"
          @dragenter.prevent="startDrag(chapter)"
          @dragover.prevent
          @dragleave.prevent="endDrag(chapter)"
          @drop.prevent="dropFile(chapter, $event, '章节文件')"
        >
          <input ref="chapterFileInput" type="file" accept=".zip" hidden @click.stop @change="selectFile(chapter, $event, '章节文件')">
          <div class="upload-content">
            <div class="upload-icon"><icon icon="#icon-upload"></icon></div>
            <div class="upload-text">
              <p class="primary-text">拖拽章节文件到此处上传</p>
              <p class="secondary-text">或 <em>点击选择文件</em></p>
              <p class="hint-text">仅支持 ZIP 格式，单个文件不超过 500MB</p>
            </div>
          </div>
        </div>

        <div v-if="chapter.selectedFile" class="file-list">
          <div class="file-item">
            <span class="file-name">{{ chapter.selectedFile.name }}</span>
            <button class="remove-btn" type="button" @click="removeSelectedFile(chapter, chapterFileInput)">
              <icon icon="#icon-delete"></icon>
            </button>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <span v-if="chapter.error" class="error-message">{{ chapter.error }}</span>
        <button class="submit-btn" type="submit" :disabled="chapter.uploading">
          <icon icon="#icon-upload"></icon>
          {{ chapter.uploading ? '上传中...' : '上传章节' }}
        </button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import server from '@/util/request'

const emit = defineEmits(['completed'])

const TAG_CATEGORIES = [
  { key: 'artist', category: 7, label: '艺术家', placeholder: '输入艺术家名后按回车添加' },
  { key: 'group', category: 8, label: '团队', placeholder: '输入团队名后按回车添加' },
  { key: 'character', category: 1, label: '角色', placeholder: '输入角色名后按回车添加' },
  { key: 'male', category: 2, label: '男性', placeholder: '输入男性标签后按回车添加' },
  { key: 'female', category: 3, label: '女性', placeholder: '输入女性标签后按回车添加' },
  { key: 'mixed', category: 4, label: '混合', placeholder: '输入混合标签后按回车添加' },
  { key: 'other', category: 5, label: '其他', placeholder: '输入其他标签后按回车添加' },
  { key: 'original', category: 6, label: '原作', placeholder: '输入原作名后按回车添加' },
]

const mode = ref('manga')
const mangaTitleInput = ref(null)
const chapterTitleInput = ref(null)
const mangaFileInput = ref(null)
const chapterFileInput = ref(null)
let temporaryTagId = -1
let mangaSearchSequence = 0

const manga = reactive({
  title: '',
  chineseTitle: '',
  selectedFile: null,
  dragCounter: 0,
  tagMode: 'input',
  error: '',
  uploading: false,
})

const chapter = reactive({
  mangaId: null,
  mangaOptions: [],
  searching: false,
  title: '',
  selectedFile: null,
  dragCounter: 0,
  error: '',
  uploading: false,
})

const tagGroups = reactive(TAG_CATEGORIES.map(category => ({
  ...category,
  input: '',
  available: [],
  selected: [],
})))

onMounted(loadMangaTags)

async function loadMangaTags() {
  try {
    const res = await server.get('/manga-tag/list')
    for (const group of tagGroups) {
      group.available = (res.data?.[`${group.key}Tags`] || [])
        .slice()
        .sort((a, b) => (b.tagCount || 0) - (a.tagCount || 0))
    }
  } catch (error) {
    ElMessage.error(`获取漫画标签失败【${error}】，请重试`)
  }
}

function addTag(group) {
  const names = parseTagNames(group.input)
  for (const tagName of names) {
    if (!group.selected.some(tag => tag.tagName === tagName)) {
      group.selected.push({ tagId: temporaryTagId--, tagName, tagCount: 0 })
    }
  }
  group.input = ''
}

function parseTagNames(input) {
  return input
    .split('#')
    .map(tag => tag.trim())
    .filter(Boolean)
}

function removeTag(group, tag) {
  const index = group.selected.findIndex(selected => selected.tagId === tag.tagId)
  if (index !== -1) group.selected.splice(index, 1)
}

function toggleTag(group, tag) {
  if (isTagSelected(group, tag)) {
    removeTag(group, tag)
  } else {
    group.selected.push(tag)
  }
}

function isTagSelected(group, tag) {
  return group.selected.some(selected => selected.tagId === tag.tagId)
}

function startDrag(target) {
  target.dragCounter += 1
}

function endDrag(target) {
  target.dragCounter = Math.max(0, target.dragCounter - 1)
}

function dropFile(target, event, label) {
  target.dragCounter = 0
  const file = event.dataTransfer.files[0]
  if (file) processZipFile(target, file, label)
}

function selectFile(target, event, label) {
  const file = event.target.files[0]
  if (file) processZipFile(target, file, label)
}

function processZipFile(target, file, label) {
  if (file.type !== 'application/zip' && !file.name.toLowerCase().endsWith('.zip')) {
    ElMessage.error('只能上传 ZIP 格式的文件！')
    return
  }
  if (file.size >= 500 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 500MB！')
    return
  }

  target.selectedFile = file
  ElMessage.success(`${label}已选择，点击提交按钮进行上传`)
}

function removeSelectedFile(target, input) {
  target.selectedFile = null
  if (input) input.value = ''
}

async function addManga() {
  const title = manga.title.trim()
  if (!title) {
    manga.error = '「标题」不能为空'
    mangaTitleInput.value?.focus()
    return
  }
  if (!manga.selectedFile) {
    manga.error = '请选择「漫画文件」'
    return
  }

  const formData = new FormData()
  formData.append('title', title)
  formData.append('chineseTitle', manga.chineseTitle.trim())
  formData.append('tags', JSON.stringify(tagGroups.flatMap(group => (
    group.selected.map(tag => ({
      ...(tag.tagId > 0 ? { tagId: tag.tagId } : {}),
      tagName: tag.tagName,
      category: group.category,
    }))
  ))))
  formData.append('file', manga.selectedFile)

  manga.error = ''
  manga.uploading = true
  try {
    const res = await server.post('/manga/addManga', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    ElMessage.success(`新增漫画「${res.data}」成功。`)
    emit('completed')
  } catch (error) {
    manga.error = `上传失败【${error}】，请重试`
  } finally {
    manga.uploading = false
  }
}

function switchToManga() {
  mode.value = 'manga'
  manga.error = ''
}

function switchToChapter() {
  mode.value = 'chapter'
  chapter.error = ''
  if (!chapter.mangaOptions.length) searchMangas('')
}

async function searchMangas(query) {
  const sequence = ++mangaSearchSequence
  chapter.searching = true
  try {
    const res = await server.get('/manga/list', {
      params: {
        pageNum: 1,
        deleted: false,
        title: query?.trim() || undefined,
      },
    })
    if (sequence === mangaSearchSequence) {
      chapter.mangaOptions = res.data?.records || []
    }
  } catch (error) {
    if (sequence === mangaSearchSequence) {
      chapter.error = `搜索漫画失败【${error}】，请重试`
    }
  } finally {
    if (sequence === mangaSearchSequence) chapter.searching = false
  }
}

function handleMangaSelectVisible(visible) {
  if (visible && !chapter.mangaOptions.length && !chapter.searching) searchMangas('')
}

function mangaOptionLabel(option) {
  const chineseTitle = option.chineseTitle && option.chineseTitle !== option.title
    ? `（${option.chineseTitle}）`
    : ''
  return `${option.title}${chineseTitle} #${option.id}`
}

async function addChapter() {
  if (!chapter.mangaId) {
    chapter.error = '请选择漫画'
    return
  }
  if (!chapter.title.trim()) {
    chapter.error = '请输入章节标题'
    chapterTitleInput.value?.focus()
    return
  }
  if (!chapter.selectedFile) {
    chapter.error = '请选择「漫画章节文件」'
    return
  }

  const formData = new FormData()
  formData.append('title', chapter.title.trim())
  formData.append('file', chapter.selectedFile)

  chapter.error = ''
  chapter.uploading = true
  try {
    const res = await server.post(`/manga/${chapter.mangaId}/chapters`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    ElMessage.success(`新增漫画章节「${res.data.title}」成功。`)
    emit('completed')
  } catch (error) {
    chapter.error = `上传失败【${error}】，请重试`
  } finally {
    chapter.uploading = false
  }
}
</script>

<style scoped>
.tag-input-container {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.tag-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.tag-input:focus {
  outline: none;
  border-color: var(--upload-primary);
  box-shadow: 0 0 0 3px rgba(var(--upload-primary-rgb), 0.1);
}

.add-tag-btn {
  border: none;
  border-radius: 6px;
  background: var(--upload-primary);
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.add-tag-btn:hover {
  background: var(--upload-primary-hover);
}

/* 已选标签区域 */
.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
  min-height: 24px;
  padding: 6px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f9fafb;
}

.selected-tag {
  padding: 3px 6px;
  background: var(--upload-primary);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

/* 可选标签区域 */
.available-tags {
  max-height: 210px;
  overflow-y: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #ffffff;
}

.available-tags::-webkit-scrollbar {
  width: 6px;
}

.available-tags::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.available-tags::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.available-tags::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.available-tag {
  padding: 2px 6px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #ffffff;
  color: #374151;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.available-tag:hover {
  border-color: var(--upload-primary);
  background: var(--upload-primary-soft);
  color: var(--upload-primary);
}

.available-tag.selected {
  border-color: var(--upload-primary);
  background: var(--upload-primary);
  color: white;
}

/* 漫画上传区域 */
.manga-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.upload-submit-group {
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-submit-group .form-actions {
  margin: 0;
}

/* 上传内容样式 */
.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 16px;
  text-align: center;
}

.upload-icon {
  width: 32px;
  height: 32px;
  fill: var(--upload-primary);
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.upload-icon .icon {
  width: 32px;
  height: 32px;
}

.upload-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.primary-text {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin: 0;
  line-height: 1.4;
}

.secondary-text {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
  line-height: 1.3;
}

.secondary-text em {
  color: var(--upload-primary);
  font-style: normal;
  font-weight: 500;
  text-decoration: underline;
  text-decoration-color: rgba(var(--upload-primary-rgb), 0.3);
}

.hint-text {
  font-size: 11px;
  color: #9ca3af;
  margin: 2px 0 0;
  line-height: 1.2;
}

/* 标签分类样式 */
.tag-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.tag-mode-toggle {
  display: flex;
  background: #f1f5f9;
  border-radius: 6px;
  padding: 2px;
  gap: 2px;
}

.toggle-btn {
  padding: 6px 12px;
  border: none;
  background: transparent;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #64748b;
}

.toggle-btn:hover {
  color: #475569;
  background: rgba(255, 255, 255, 0.5);
}

.toggle-btn.active {
  background: white;
  color: var(--upload-primary);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.tag-categories {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 12px;
}

.tag-category {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.tag-category h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8f0;
}

/* 漫画自定义上传组件样式 */
.custom-upload {
  box-sizing: border-box;
  border: 2px dashed #d1d5db;
  border-radius: 16px;
  padding: 32px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  position: relative;
  overflow: hidden;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: auto;
}

.custom-upload::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at center, rgba(var(--upload-primary-rgb), 0.05) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.custom-upload:hover {
  border-color: var(--upload-primary);
  background: linear-gradient(135deg, #f5faff 0%, var(--upload-primary-soft) 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(var(--upload-primary-rgb), 0.15);
}

.custom-upload:hover::before {
  opacity: 1;
}

.custom-upload:active {
  transform: translateY(-1px) scale(0.98);
  box-shadow: 0 4px 15px rgba(var(--upload-primary-rgb), 0.2);
}

.custom-upload.dragover {
  border-color: var(--upload-primary);
  background: linear-gradient(135deg, var(--upload-primary-soft) 0%, var(--upload-primary-soft-strong) 100%);
  transform: scale(1.02);
  box-shadow: 0 12px 35px rgba(var(--upload-primary-rgb), 0.25);
}

.custom-upload.dragover::before {
  opacity: 1;
}

.custom-upload * {
  pointer-events: none;
}

/* 文件列表样式 */
.file-list {
  margin-top: 16px;
}

.file-item {
  display: grid;
  grid-template-columns: auto 40px;
  align-items: center;
  justify-content: space-between;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
}

.file-name {
  color: #374151;
  font-weight: 500;
}

.remove-btn {
  background: none;
  border: none;
  color: #ef4444;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-btn:hover {
  background: #fee2e2;
}

.remove-btn .icon {
  width: 16px;
  height: 16px;
}

.manga-file-row {
  grid-template-columns: 1fr 1fr;
}

.selected-file-list {
  width: 80%;
}

.selected-tag,
.available-tag {
  font: inherit;
}

@media (max-width: 768px) {
  .manga-file-row {
    grid-template-columns: 1fr;
  }

  .selected-file-list {
    width: 100%;
  }
}
</style>
