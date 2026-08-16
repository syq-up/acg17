export function withMediaStyle(path, style) {
  if (!path) return ''

  const url = new URL(path, window.location.origin)
  url.searchParams.set('style', style)

  return path.startsWith('/')
    ? `${url.pathname}${url.search}${url.hash}`
    : url.toString()
}
