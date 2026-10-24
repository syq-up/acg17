<template>
  <el-popover
    trigger="hover"
    :trigger-keys="[]"
    placement="top-start"
    :width="360"
    :show-after="220"
    :hide-after="180"
    :enterable="true"
    :persistent="false"
    :disabled="!hasDetails"
    :popper-style="{ maxWidth: 'calc(100vw - 24px)', padding: '18px 20px', borderRadius: '12px' }"
  >
    <template #reference><slot /></template>
    <div class="manga-tag-description">
      <div class="tag-heading">{{ getMangaTagName(tag) }}</div>
      <div v-if="categoryName" class="tag-category">{{ categoryName }}</div>
      <div v-if="tag.raw" class="tag-raw"><span>原始标签</span><code>{{ tag.raw }}</code></div>
      <p v-if="description" class="tag-intro">{{ description }}</p>
      <div v-if="references.length" class="tag-references">
        <div class="tag-references-label">参考链接</div>
        <a
          v-for="link in references"
          :key="link.url"
          class="tag-reference"
          :href="link.url"
          target="_blank"
          rel="noopener noreferrer"
        >
          <span><span class="tag-reference-name">{{ link.name }}</span><span class="tag-reference-host">{{ link.host }}</span></span>
          <span aria-hidden="true">↗</span>
        </a>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { computed } from 'vue'
import { MANGA_TAG_CATEGORIES } from '@/constants/mangaTagCategories'
import { getMangaTagName } from '@/util/mangaTags'

const props = defineProps({
  tag: {
    type: [Object, String],
    required: true,
  },
})

const categoryName = computed(() => MANGA_TAG_CATEGORIES
  .find(category => category.categoryId === props.tag.category)?.label || '')
const description = computed(() => String(props.tag.intro || '')
  .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
  .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
  .replace(/`([^`]+)`/g, '$1')
  .replace(/<br\s*\/?>/gi, '\n')
  .trim())
const references = computed(() => Array.from(
  String(props.tag.links || '').matchAll(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g),
).flatMap(([, name, href]) => {
  try {
    const url = new URL(href)
    return [{ name, url: url.href, host: url.hostname }]
  } catch {
    return []
  }
}))
const hasDetails = computed(() => {
  const name = props.tag.name?.trim()
  return Boolean(
    (name && name !== props.tag.raw?.trim()) || description.value || references.value.length,
  )
})
</script>

<style scoped>
.manga-tag-description {
  color: #303133;
  overflow-wrap: anywhere;
  max-height: min(420px, calc(100vh - 72px));
  overflow-y: auto;
  font-family: inherit;
  line-height: 1.6;
}

.tag-heading {
  font-size: 16px;
  font-weight: 600;
}

.tag-category {
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
}

.tag-raw {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin-top: 14px;
  color: #909399;
  font-size: 12px;
}

.tag-raw code {
  padding: 2px 6px;
  border-radius: 4px;
  background: #f5f3f8;
  color: #606266;
  font-size: inherit;
}

.tag-intro {
  margin: 14px 0 0;
  white-space: pre-line;
  font-size: 13px;
  line-height: 1.8;
}

.tag-references {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.tag-references-label {
  color: #909399;
  font-size: 12px;
}

.tag-reference {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 10px;
  color: #6851c5;
  text-decoration: none;
  font-size: 13px;
}

.tag-reference:hover .tag-reference-name {
  text-decoration: underline;
}

.tag-reference-host {
  display: block;
  color: #909399;
  font-size: 11px;
}
</style>
