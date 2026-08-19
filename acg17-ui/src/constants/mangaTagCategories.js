export const MANGA_TAG_CATEGORIES = [
  { key: 'group', categoryId: 8, field: 'groupTags', label: '团队' },
  { key: 'artist', categoryId: 7, field: 'artistTags', label: '艺术家' },
  { key: 'character', categoryId: 1, field: 'characterTags', label: '角色' },
  { key: 'male', categoryId: 2, field: 'maleTags', label: '男性' },
  { key: 'female', categoryId: 3, field: 'femaleTags', label: '女性' },
  { key: 'mixed', categoryId: 4, field: 'mixedTags', label: '混合' },
  { key: 'other', categoryId: 5, field: 'otherTags', label: '其他' },
  { key: 'original', categoryId: 6, field: 'originalTags', label: '原作' },
]

export function getMangaTagCategory(key) {
  return MANGA_TAG_CATEGORIES.find(category => category.key === key)
}
