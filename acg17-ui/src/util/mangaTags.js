export function getMangaTagName(tag) {
  if (typeof tag === 'string') return tag
  return tag?.name?.trim() || tag?.raw || ''
}

export function normalizeMangaTagQuery(value) {
  return String(value ?? '').trim().toLocaleLowerCase()
}

export function getMangaTagMatchRank(tag, query) {
  const normalizedQuery = normalizeMangaTagQuery(query)
  if (!normalizedQuery) return 0
  const values = [getMangaTagName(tag), typeof tag === 'string' ? tag : tag?.raw]
    .map(normalizeMangaTagQuery)
  if (values.some(value => value === normalizedQuery)) return 0
  if (values.some(value => value.startsWith(normalizedQuery))) return 1
  if (values.some(value => value.includes(normalizedQuery))) return 2
  return 3
}

export function matchesMangaTag(tag, query) {
  return getMangaTagMatchRank(tag, query) < 3
}
