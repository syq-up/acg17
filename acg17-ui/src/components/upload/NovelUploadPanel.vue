<template>
  <section class="novel-section">
    <div class="tab-header">
      <button class="tab-btn" :class="{ active: mode === 'novel' }" type="button" @click="switchToNovel">
        <icon icon="#icon-book"></icon>
        新增书籍
      </button>
      <button
        class="tab-btn"
        :class="{ active: mode === 'chapter' }"
        type="button"
        :disabled="!chapter.novelId"
        @click="switchToChapter"
      >
        <icon icon="#icon-novel"></icon>
        新增章节
      </button>
    </div>

    <form v-if="mode === 'novel'" class="modern-form" @submit.prevent="addNovel">
      <div class="form-row">
        <div class="form-group">
          <label for="novel-title" class="form-label">
            <span class="required">*</span>书名
          </label>
          <input
            id="novel-title"
            ref="novelTitleInput"
            v-model="novel.title"
            class="form-input"
            placeholder="请输入书名"
            autocomplete="off"
            maxlength="100"
            required
          >
        </div>
        <div class="form-group">
          <label for="novel-author" class="form-label">作者</label>
          <input
            id="novel-author"
            v-model="novel.author"
            class="form-input"
            placeholder="请输入作者名"
            autocomplete="off"
            maxlength="100"
          >
        </div>
      </div>

      <div class="form-group">
        <label class="form-label">标签</label>
        <div class="tag-selector">
          <button v-if="!novel.addingTag" class="tag-item add-tag-btn" type="button" @click="startAddTag">
            <icon icon="#icon-quill-pen" class="add-icon"></icon>
          </button>
          <div v-else class="tag-item add-tag-input">
            <input
              ref="newTagInput"
              v-model="novel.newTagName"
              placeholder="输入标签名"
              maxlength="32"
              @keyup.enter.prevent="confirmAddTag"
              @keyup.esc="cancelAddTag"
            >
            <icon icon="#icon-ok" class="confirm-icon" @click="confirmAddTag"></icon>
            <icon icon="#icon-close" class="cancel-icon" @click="cancelAddTag"></icon>
          </div>

          <button
            v-for="tag in novel.tagList"
            :key="tag.id"
            class="tag-item"
            :class="{ active: novel.tags.includes(tag.name) }"
            type="button"
            @click="toggleTag(tag.name)"
          >
            {{ tag.name }}
          </button>
        </div>
      </div>

      <div class="form-actions">
        <span v-if="novel.error" class="error-message">{{ novel.error }}</span>
        <button class="submit-btn" type="submit" :disabled="novel.submitting">
          <icon icon="#icon-upload"></icon>
          {{ novel.submitting ? '新增中...' : '新增书籍' }}
        </button>
      </div>
    </form>

    <form v-else class="modern-form" @submit.prevent="addChapter">
      <div class="form-group">
        <label class="form-label"><span class="required">*</span>书名</label>
        <input :value="chapter.novelTitle" class="form-input" readonly disabled>
      </div>

      <div class="form-group">
        <label for="chapter-title" class="form-label">
          <span class="required">*</span>章节名
        </label>
        <input
          id="chapter-title"
          ref="chapterTitleInput"
          v-model="chapter.title"
          class="form-input"
          placeholder="请输入章节名"
          autocomplete="off"
          maxlength="150"
          required
        >
      </div>

      <div class="form-group">
        <label for="chapter-content" class="form-label">
          <span class="required">*</span>章节内容
        </label>
        <textarea
          id="chapter-content"
          ref="chapterContentInput"
          v-model="chapter.content"
          class="form-textarea"
          placeholder="请输入章节内容"
          rows="8"
          required
        ></textarea>
      </div>

      <div class="form-actions">
        <span v-if="chapter.error" class="error-message">{{ chapter.error }}</span>
        <button class="submit-btn" type="submit" :disabled="chapter.submitting">
          <icon icon="#icon-upload"></icon>
          {{ chapter.submitting ? '新增中...' : '新增章节' }}
        </button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import server from '@/util/request'

const props = defineProps({
  initialMode: {
    type: String,
    default: 'novel',
    validator: value => ['novel', 'chapter'].includes(value),
  },
  novelContext: {
    type: Object,
    default: () => ({}),
  },
})

const mode = ref(props.initialMode)
const novelTitleInput = ref(null)
const chapterTitleInput = ref(null)
const chapterContentInput = ref(null)
const newTagInput = ref(null)

const novel = reactive({
  title: '',
  author: '',
  tags: [],
  tagList: [],
  error: '',
  addingTag: false,
  newTagName: '',
  submitting: false,
})

const chapter = reactive({
  novelId: props.novelContext.novelId || '',
  novelTitle: props.novelContext.novelTitle || '',
  title: '',
  content: '',
  error: '',
  submitting: false,
})

onMounted(() => {
  if (mode.value === 'novel') loadNovelTags()
})

async function loadNovelTags() {
  try {
    const res = await server.get('/novel-tag/getList')
    novel.tagList = res.data || []
  } catch (error) {
    ElMessage.error(`获取标签列表失败【${error}】。`)
  }
}

function switchToNovel() {
  mode.value = 'novel'
  novel.error = ''
  if (!novel.tagList.length) loadNovelTags()
}

function switchToChapter() {
  if (!chapter.novelId) return
  mode.value = 'chapter'
  chapter.error = ''
}

async function addNovel() {
  const title = novel.title.trim()
  if (!title) {
    novel.error = '「书名」不能为空'
    novelTitleInput.value?.focus()
    return
  }

  novel.error = ''
  novel.submitting = true
  try {
    const res = await server.post('/novel/addNovel', {
      title,
      author: novel.author.trim(),
      tags: novel.tags,
    })
    ElMessage.success(`新增小说「${res.data.title}」成功。`)
    chapter.novelId = res.data.id
    chapter.novelTitle = res.data.title
    mode.value = 'chapter'
  } catch (error) {
    ElMessage.error(`新增小说「${title}」失败【${error}】。`)
  } finally {
    novel.submitting = false
  }
}

function toggleTag(tagName) {
  const index = novel.tags.indexOf(tagName)
  if (index === -1) {
    novel.tags.push(tagName)
  } else {
    novel.tags.splice(index, 1)
  }
}

async function startAddTag() {
  novel.addingTag = true
  novel.newTagName = ''
  await nextTick()
  newTagInput.value?.focus()
}

function confirmAddTag() {
  const tagName = novel.newTagName.trim()
  if (tagName && !novel.tagList.some(tag => tag.name === tagName)) {
    novel.tagList.push({ id: `new-${tagName}`, name: tagName })
    novel.tags.push(tagName)
  }
  cancelAddTag()
}

function cancelAddTag() {
  novel.addingTag = false
  novel.newTagName = ''
}

async function addChapter() {
  if (!chapter.title.trim()) {
    chapter.error = '章节名不能为空'
    chapterTitleInput.value?.focus()
    return
  }
  if (!chapter.content.trim()) {
    chapter.error = '内容不能为空'
    chapterContentInput.value?.focus()
    return
  }

  const data = {
    novelId: chapter.novelId,
    title: chapter.title.trim(),
    content: chapter.content.split(/\r?\n\s*\r?\n/).filter(paragraph => paragraph.trim()),
  }

  chapter.error = ''
  chapter.submitting = true
  try {
    await server.post('/novel-chapter/addChapter', data)
    ElMessage.success(`新增章节「${data.title}」成功。`)
    chapter.title = ''
    chapter.content = ''
  } catch (error) {
    ElMessage.error(`新增章节「${data.title}」失败【${error}】。`)
  } finally {
    chapter.submitting = false
  }
}
</script>

<style scoped>
/* 小说上传区域 */
.novel-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 标签选择器 */
.tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 13px;
  color: #6b7280;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;
  font: inherit;
}

.tag-item:hover {
  border-color: var(--upload-primary);
  color: var(--upload-primary);
}

.tag-item.active {
  border-color: var(--upload-primary);
  background: var(--upload-primary);
  color: white;
}

/* 新增标签样式 */
.tag-item.add-tag-btn {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px dashed #cbd5e1;
  color: #64748b;
  font-size: 13px;
  padding: 8px 16px;
  gap: 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.tag-item.add-tag-btn:hover {
  background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
  border-color: var(--upload-primary);
  color: var(--upload-primary);
  box-shadow: 0 2px 4px rgba(var(--upload-primary-rgb), 0.1);
}

.tag-item.add-tag-input {
  padding: 6px 8px;
  gap: 6px;
  background: white;
  border: 1px solid var(--upload-primary);
}

.tag-item.add-tag-input input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #374151;
  width: 80px;
  padding: 0;
  line-height: 1.2;
}

.tag-item.add-tag-input input::placeholder {
  color: #9ca3af;
}

.tag-item.add-tag-btn .add-icon,
.tag-item.add-tag-input .confirm-icon,
.tag-item.add-tag-input .cancel-icon {
  width: 14px;
  height: 14px;
  cursor: pointer;
  transition: all 0.2s;
  fill: #6b7280;
}

.tag-item.add-tag-input .confirm-icon:hover {
  fill: #10b981;
}

.tag-item.add-tag-input .cancel-icon:hover {
  fill: #ef4444;
}
</style>
