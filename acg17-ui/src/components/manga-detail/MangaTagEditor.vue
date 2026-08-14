<template>
  <div class="tag-editor-overlay" @click="close">
    <div class="tag-editor-modal" @click.stop>
      <div class="tag-editor-header">
        <h3>编辑{{ categoryName }}标签</h3>
        <button class="close-btn" type="button" @click="close">&times;</button>
      </div>

      <div class="tag-editor-content">
        <section class="current-tags-section">
          <h4>当前标签</h4>
          <div class="tags-list">
            <span v-for="tag in localCurrentTags" :key="`current-${tag.tagId}`" class="tag current-tag">
              {{ tag.tagName }}
              <button
                class="remove-tag-btn"
                type="button"
                title="删除标签"
                :disabled="updatingTagIds.has(tag.tagId)"
                @click="removeTagFromManga(tag.tagId)"
              >
                &times;
              </button>
            </span>
            <span v-if="localCurrentTags.length === 0" class="no-tags">暂无标签</span>
          </div>
        </section>

        <section class="available-tags-section">
          <h4>可用标签</h4>
          <div class="tags-list">
            <button
              v-for="tag in availableTags"
              :key="`available-${tag.tagId}`"
              :class="['tag', 'available-tag', { added: isTagAdded(tag.tagId) }]"
              type="button"
              :disabled="isTagAdded(tag.tagId) || updatingTagIds.has(tag.tagId)"
              @click="addTagToManga(tag.tagId)"
            >
              {{ tag.tagName }}
              <span class="tag-count">{{ tag.tagCount || 0 }}</span>
            </button>
            <span v-if="availableTags.length === 0" class="no-tags">暂无可用标签</span>
          </div>
        </section>

        <section class="add-tag-section">
          <h4>添加新标签</h4>
          <div class="add-tag-form">
            <input
              v-model="newTagName"
              type="text"
              placeholder="输入新标签名称"
              class="tag-input"
              @keyup.enter="createAndAddTag"
            >
            <button
              class="add-btn"
              type="button"
              :disabled="creatingTag || !newTagName.trim()"
              @click="createAndAddTag"
            >
              添加
            </button>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import server from '@/util/request'

const CATEGORY_NAMES = {
  character: '角色',
  male: '男性',
  female: '女性',
  mixed: '混合',
  other: '其他',
  original: '原作',
}

const props = defineProps({
  mangaId: {
    type: [Number, String],
    required: true,
  },
  category: {
    type: String,
    required: true,
  },
  currentTags: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['close', 'updated'])

const availableTags = ref([])
const localCurrentTags = ref([])
const newTagName = ref('')
const creatingTag = ref(false)
const updatingTagIds = ref(new Set())
const categoryName = computed(() => CATEGORY_NAMES[props.category] || '')

watch(
  () => props.category,
  category => {
    newTagName.value = ''
    loadAvailableTags(category)
  },
  { immediate: true },
)

watch(
  () => props.currentTags,
  tags => {
    localCurrentTags.value = tags.slice()
  },
  { immediate: true },
)

async function loadAvailableTags(category) {
  try {
    const res = await server.get(`/manga-tag/category/${category}`)
    if (res.code === 200 && props.category === category) {
      availableTags.value = (res.data || [])
        .slice()
        .sort((a, b) => (b.tagCount || 0) - (a.tagCount || 0))
    }
  } catch (error) {
    console.error('获取标签列表失败:', error)
  }
}

async function addTagToManga(tagId) {
  if (isTagAdded(tagId) || updatingTagIds.value.has(tagId)) return false

  setTagUpdating(tagId, true)
  try {
    const res = await server.post(`/manga/${props.mangaId}/tags`, null, {
      params: { tagId },
    })
    if (res.code === 200) {
      const tag = availableTags.value.find(item => item.tagId === tagId)
      if (tag && !isTagAdded(tagId)) {
        localCurrentTags.value.push(tag)
      }
      emit('updated')
      await loadAvailableTags(props.category)
      return true
    }
  } catch (error) {
    console.error('添加标签失败:', error)
  } finally {
    setTagUpdating(tagId, false)
  }

  return false
}

async function removeTagFromManga(tagId) {
  if (updatingTagIds.value.has(tagId)) return

  setTagUpdating(tagId, true)
  try {
    const res = await server.delete(`/manga/${props.mangaId}/tags`, {
      params: { tagId },
    })
    if (res.code === 200) {
      localCurrentTags.value = localCurrentTags.value.filter(tag => tag.tagId !== tagId)
      emit('updated')
      await loadAvailableTags(props.category)
    }
  } catch (error) {
    console.error('删除标签失败:', error)
  } finally {
    setTagUpdating(tagId, false)
  }
}

async function createAndAddTag() {
  const tagName = newTagName.value.trim()
  if (!tagName || creatingTag.value) return

  creatingTag.value = true
  try {
    const res = await server.post('/manga-tag/get-or-create-by-category', null, {
      params: {
        tagName,
        category: props.category,
      },
    })

    if (res.code === 200) {
      await loadAvailableTags(props.category)
      const tagId = res.data.tagId ?? res.data.id
      if (await addTagToManga(tagId)) {
        newTagName.value = ''
      }
    }
  } catch (error) {
    console.error('创建标签失败:', error)
  } finally {
    creatingTag.value = false
  }
}

function isTagAdded(tagId) {
  return localCurrentTags.value.some(tag => tag.tagId === tagId)
}

function setTagUpdating(tagId, updating) {
  const nextIds = new Set(updatingTagIds.value)
  if (updating) {
    nextIds.add(tagId)
  } else {
    nextIds.delete(tagId)
  }
  updatingTagIds.value = nextIds
}

function close() {
  emit('close')
}
</script>

<style scoped>
.tag-editor-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.tag-editor-modal {
  background: white;
  border-radius: 8px;
  width: 1000px;
  max-width: 90vw;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.tag-editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e9ecef;
  background-color: #f8f9fa;
}

.tag-editor-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #6c757d;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.close-btn:hover {
  background-color: #e9ecef;
  color: #495057;
}

.tag-editor-content {
  padding: 20px;
  max-height: calc(80vh - 80px);
  overflow-y: auto;
}

.current-tags-section,
.available-tags-section,
.add-tag-section {
  margin-bottom: 24px;
}

.add-tag-section {
  margin-bottom: 0;
}

.tag-editor-content h4 {
  margin: 0 0 12px;
  color: #495057;
  font-size: 16px;
  font-weight: 600;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 40px;
  max-height: 400px;
  overflow-y: auto;
  align-items: flex-start;
  padding: 4px;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  background-color: #fafafa;
}

.tag {
  display: inline-flex;
  align-items: center;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  padding: 4px 8px;
  font: inherit;
  font-size: 13px;
  color: #343a40;
  transition: all 0.2s ease;
}

.tag-count {
  margin-left: 6px;
  background-color: #6c757d;
  color: white;
  border-radius: 10px;
  padding: 2px 4px;
  font-size: 11px;
  font-weight: 500;
  min-width: 20px;
  text-align: center;
}

.current-tag {
  background-color: #e3f2fd;
  border-color: #2196f3;
  color: #1976d2;
  position: relative;
  padding-right: 24px;
}

.available-tag {
  cursor: pointer;
}

.available-tag:hover:not(:disabled) {
  background-color: #e9ecef;
  border-color: #409eff;
  transform: translateY(-1px);
}

.available-tag.added {
  background-color: #d4edda;
  border-color: #c3e6cb;
  color: #155724;
  cursor: not-allowed;
  opacity: 0.7;
}

.remove-tag-btn {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #dc3545;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 2px;
}

.remove-tag-btn:hover:not(:disabled) {
  background-color: #dc3545;
  color: white;
}

.remove-tag-btn:disabled {
  cursor: wait;
  opacity: 0.5;
}

.no-tags {
  color: #6c757d;
  font-style: italic;
  padding: 8px 4px;
}

.add-tag-form {
  display: flex;
  gap: 12px;
  align-items: center;
}

.tag-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease;
}

.tag-input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.add-btn {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.2s ease;
}

.add-btn:hover:not(:disabled) {
  background-color: #337ab7;
}

.add-btn:disabled {
  background-color: #e9ecef;
  color: #6c757d;
  cursor: not-allowed;
}

@media screen and (max-width: 768px) {
  .tag-editor-modal {
    width: 95%;
    max-height: 90vh;
  }

  .tag-editor-header,
  .tag-editor-content {
    padding: 15px;
  }

  .add-tag-form {
    flex-direction: column;
    align-items: stretch;
  }

  .add-btn {
    width: 100%;
  }
}
</style>
