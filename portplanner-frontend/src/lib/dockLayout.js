/*
 * The painted dock layout.
 *
 * Version 2 stores the grid as one run-length encoded string per row: a count
 * followed by its character, where "." is water, L land, D jetty and S a slip.
 * The slips' ids follow in reading order. A 60x40 harbour that took 21 kB as a
 * list of cell objects takes about 2 kB this way.
 *
 * Version 1 (a list of {row, col, type, slipId}) is still read, so layouts drawn
 * before the change keep working; they are written back as version 2 the next
 * time the dock is saved.
 */

const CHAR_BY_TYPE = { LAND: 'L', DOCK: 'D', SLIP: 'S' }
const TYPE_BY_CHAR = { L: 'LAND', D: 'DOCK', S: 'SLIP' }

export const DEFAULT_WIDTH = 30
export const DEFAULT_HEIGHT = 20

export function cellKey(row, col) {
  return `${row},${col}`
}

/** Expands one run-length encoded row into an array of characters. */
function expandRow(row, width) {
  const out = []
  let count = ''
  for (const ch of row) {
    if (ch >= '0' && ch <= '9') {
      count += ch
      continue
    }
    const n = count === '' ? 1 : Number(count)
    for (let i = 0; i < n; i++) out.push(ch)
    count = ''
  }
  return out.slice(0, width)
}

function parseV2(data) {
  const gridWidth = data.w ?? DEFAULT_WIDTH
  const gridHeight = data.h ?? DEFAULT_HEIGHT
  const slipIds = data.s ?? []
  const cells = {}
  let nextSlip = 0
  ;(data.g ?? []).forEach((row, r) => {
    expandRow(row, gridWidth).forEach((ch, c) => {
      const type = TYPE_BY_CHAR[ch]
      if (!type) return
      cells[cellKey(r, c)] =
        type === 'SLIP' ? { type, slipId: slipIds[nextSlip++] ?? null } : { type }
    })
  })
  return { cells, gridWidth, gridHeight }
}

function parseV1(data) {
  const cells = {}
  for (const cell of data.cells ?? []) {
    cells[cellKey(cell.row, cell.col)] =
      cell.type === 'SLIP' ? { type: 'SLIP', slipId: cell.slipId } : { type: cell.type }
  }
  return {
    cells,
    gridWidth: data.gridWidth ?? DEFAULT_WIDTH,
    gridHeight: data.gridHeight ?? DEFAULT_HEIGHT,
  }
}

/** Returns { cells, gridWidth, gridHeight }, or null for missing or corrupt data. */
export function parseLayout(json) {
  if (!json) return null
  try {
    const data = JSON.parse(json)
    return data.v === 2 ? parseV2(data) : parseV1(data)
  } catch {
    return null
  }
}

export function serializeLayout({ cells, gridWidth, gridHeight }) {
  const rows = []
  const slipIds = []
  for (let r = 0; r < gridHeight; r++) {
    let out = ''
    let run = 0
    let prev = null
    const flush = () => {
      if (prev === null) return
      out += run > 1 ? `${run}${prev}` : prev
    }
    for (let c = 0; c < gridWidth; c++) {
      const cell = cells[cellKey(r, c)]
      const ch = cell ? CHAR_BY_TYPE[cell.type] ?? '.' : '.'
      if (cell?.type === 'SLIP') slipIds.push(cell.slipId ?? null)
      if (ch === prev) {
        run++
      } else {
        flush()
        prev = ch
        run = 1
      }
    }
    flush()
    rows.push(out)
  }
  return JSON.stringify({ v: 2, w: gridWidth, h: gridHeight, g: rows, s: slipIds })
}
