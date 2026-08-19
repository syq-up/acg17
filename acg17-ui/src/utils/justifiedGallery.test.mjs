import assert from 'node:assert/strict'
import test from 'node:test'

import { createJustifiedRows, getTargetRowHeight } from './justifiedGallery.mjs'

const item = (ratio, id = ratio) => ({ id, ratio })
const sum = (values) => values.reduce((total, value) => total + value, 0)

test('returns no rows for empty items or a missing container width', () => {
  assert.deepEqual(createJustifiedRows([], 800), [])
  assert.deepEqual(createJustifiedRows([item(1)], 0), [])
  assert.deepEqual(createJustifiedRows([item(1)], undefined), [])
})

test('fills complete rows while preserving ratios', () => {
  const width = 1000
  const rows = createJustifiedRows(
    [item(1, 'a'), item(1, 'b'), item(1, 'c'), item(1, 'd'), item(1, 'e'), item(1, 'f')],
    width,
  )
  const row = rows[0]
  const expectedHeight = (width - row.gap * (row.items.length - 1)) / sum(row.items.map(entry => entry.ratio))

  assert.equal(row.items.length, 5)
  assert.ok(Math.abs(row.height - expectedHeight) < 1e-10)
  assert.ok(Math.abs(sum(row.items.map(entry => entry.width)) + row.gap * 4 - width) < 1e-10)
  row.items.forEach(entry => assert.equal(entry.width / row.height, entry.ratio))
})

test('leaves a short final row at the target height', () => {
  const width = 1000
  const rows = createJustifiedRows([item(1), item(1), item(1), item(1), item(1), item(1)], width)
  const lastRow = rows.at(-1)

  assert.equal(lastRow.items.length, 1)
  assert.equal(lastRow.height, getTargetRowHeight(width))
  assert.ok(sum(lastRow.items.map(entry => entry.width)) < width)
})

test('shrinks a final panoramic image so it cannot overflow', () => {
  const width = 800
  const rows = createJustifiedRows([item(10)], width)
  const row = rows[0]

  assert.equal(row.height, width / 10)
  assert.equal(row.items[0].width, width)
})

test('uses a square fallback for invalid ratios', () => {
  const width = 800
  const rows = createJustifiedRows([
    item(0, 'zero'),
    item(-2, 'negative'),
    item(Number.NaN, 'nan'),
    item(Number.POSITIVE_INFINITY, 'infinite'),
  ], width)
  const row = rows[0]

  assert.deepEqual(row.items.map(entry => entry.ratio), [1, 1, 1, 1])
  assert.ok(row.items.every(entry => entry.width === row.height))
  assert.ok(sum(row.items.map(entry => entry.width)) + row.gap * 3 < width)
})
