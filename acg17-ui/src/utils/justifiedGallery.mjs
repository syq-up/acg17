const DEFAULT_GAP = 4
const DEFAULT_MIN_ROW_HEIGHT = 150
const DEFAULT_MAX_ROW_HEIGHT = 280

export function normalizeRatio(value) {
  const ratio = Number(value)
  return Number.isFinite(ratio) && ratio > 0 ? ratio : 1
}

export function getTargetRowHeight(containerWidth, options = {}) {
  const width = Number(containerWidth)
  if (!Number.isFinite(width) || width <= 0) return 0

  const minRowHeight = Number.isFinite(options.minRowHeight)
    ? options.minRowHeight
    : DEFAULT_MIN_ROW_HEIGHT
  const maxRowHeight = Number.isFinite(options.maxRowHeight)
    ? options.maxRowHeight
    : DEFAULT_MAX_ROW_HEIGHT
  const preferredRowHeight = Number.isFinite(options.targetRowHeight)
    ? options.targetRowHeight
    : width * 0.22

  return Math.min(maxRowHeight, Math.max(minRowHeight, preferredRowHeight))
}

function getGap(options) {
  const gap = Number(options.gap)
  return Number.isFinite(gap) && gap >= 0 ? gap : DEFAULT_GAP
}

function getRowHeight(entries, containerWidth, gap) {
  const availableWidth = containerWidth - gap * Math.max(0, entries.length - 1)
  const totalRatio = entries.reduce((sum, entry) => sum + entry.ratio, 0)

  if (availableWidth <= 0 || totalRatio <= 0) return 0
  return availableWidth / totalRatio
}

function makeRow(entries, containerWidth, gap, targetRowHeight, isLastRow) {
  const justifiedHeight = getRowHeight(entries, containerWidth, gap)
  const height = isLastRow
    ? Math.min(targetRowHeight, justifiedHeight)
    : justifiedHeight

  return {
    height,
    gap,
    items: entries.map(entry => ({
      item: entry.item,
      index: entry.index,
      ratio: entry.ratio,
      width: height * entry.ratio,
    })),
  }
}

/**
 * Build rows for a left-to-right justified gallery.
 *
 * Rows are closed greedily when adding the next item brings their calculated
 * height below the target. The closer of the two candidate heights wins.
 * The final row is intentionally left aligned and only shrinks when its
 * target-height widths would overflow the container.
 */
export function createJustifiedRows(items, containerWidth, options = {}) {
  if (!Array.isArray(items) || items.length === 0) return []

  const width = Number(containerWidth)
  if (!Number.isFinite(width) || width <= 0) return []

  const gap = getGap(options)
  const targetRowHeight = getTargetRowHeight(width, options)
  const entries = items.map((item, index) => ({
    item,
    index,
    ratio: normalizeRatio(item?.ratio),
  }))
  const rows = []
  let currentRow = []

  entries.forEach(entry => {
    if (currentRow.length === 0) {
      currentRow.push(entry)
      return
    }

    const currentHeight = getRowHeight(currentRow, width, gap)
    const candidateRow = [...currentRow, entry]
    const candidateHeight = getRowHeight(candidateRow, width, gap)

    if (candidateHeight >= targetRowHeight) {
      currentRow = candidateRow
      return
    }

    const currentDistance = Math.abs(currentHeight - targetRowHeight)
    const candidateDistance = Math.abs(candidateHeight - targetRowHeight)

    if (candidateDistance <= currentDistance) {
      rows.push(makeRow(candidateRow, width, gap, targetRowHeight, false))
      currentRow = []
    } else {
      rows.push(makeRow(currentRow, width, gap, targetRowHeight, false))
      currentRow = [entry]
    }
  })

  if (currentRow.length > 0) {
    rows.push(makeRow(currentRow, width, gap, targetRowHeight, true))
  }

  return rows
}

export const JUSTIFIED_GALLERY_DEFAULTS = Object.freeze({
  gap: DEFAULT_GAP,
  minRowHeight: DEFAULT_MIN_ROW_HEIGHT,
  maxRowHeight: DEFAULT_MAX_ROW_HEIGHT,
})
