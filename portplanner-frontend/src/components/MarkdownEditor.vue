<template>
  <div class="md-editor">
    <div class="md-toolbar">
      <button v-for="b in BUTTONS" :key="b.kind" type="button" :title="b.title"
              @click="apply(b.kind)">{{ b.label }}</button>
    </div>
    <textarea ref="area" :value="modelValue" :rows="rows" :placeholder="placeholder"
              @input="$emit('update:modelValue', $event.target.value)"></textarea>
    <div class="md-preview-title">Förhandsvisning</div>
    <div v-if="preview" class="md-preview markdown-body" v-html="preview"></div>
    <div v-else class="md-preview md-preview-empty">Inget skrivet än</div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { renderMarkdown } from '../lib/markdown'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  rows: { type: [Number, String], default: 8 },
})
const emit = defineEmits(['update:modelValue'])

const BUTTONS = [
  { kind: 'heading', label: 'Rubrik', title: 'Gör raden till en rubrik' },
  { kind: 'bold', label: 'Fet', title: 'Gör den markerade texten fet' },
  { kind: 'italic', label: 'Kursiv', title: 'Gör den markerade texten kursiv' },
  { kind: 'list', label: 'Punktlista', title: 'Gör raderna till en punktlista' },
]

/** The markers the buttons write. Wrapping marks surround the selection, prefixes start a line. */
const WRAPPERS = { bold: '**', italic: '*' }
const PREFIXES = { heading: '## ', list: '- ' }

const area = ref(null)
const preview = computed(() => renderMarkdown(props.modelValue))

/**
 * Rewrites the selection in the textarea and keeps it selected afterwards, so the buttons can
 * be pressed one after another. With nothing selected, a wrapping button inserts the word the
 * writer is about to replace and a prefix lands on the line the cursor is on.
 */
function apply(kind) {
  const el = area.value
  if (!el) return
  const text = props.modelValue ?? ''
  const { selectionStart: start, selectionEnd: end } = el
  let next
  let caret

  if (WRAPPERS[kind]) {
    const mark = WRAPPERS[kind]
    const selected = text.slice(start, end) || 'text'
    next = text.slice(0, start) + mark + selected + mark + text.slice(end)
    caret = [start + mark.length, start + mark.length + selected.length]
  } else {
    const prefix = PREFIXES[kind]
    const lineStart = text.lastIndexOf('\n', start - 1) + 1
    const lineEnd = text.indexOf('\n', end) === -1 ? text.length : text.indexOf('\n', end)
    const block = text.slice(lineStart, lineEnd).split('\n').map((line) => prefix + line).join('\n')
    next = text.slice(0, lineStart) + block + text.slice(lineEnd)
    caret = [lineStart + prefix.length, lineStart + block.length]
  }

  emit('update:modelValue', next)
  nextTick(() => {
    el.focus()
    el.setSelectionRange(caret[0], caret[1])
  })
}
</script>

<style scoped>
.md-editor { display: flex; flex-direction: column; gap: 0.4rem; }
.md-toolbar { display: flex; flex-wrap: wrap; gap: 0.35rem; }
.md-toolbar button { background: #f0f4f9; border: 1px solid #dde4ec; border-radius: 5px;
  padding: 0.25rem 0.6rem; font-size: 0.78rem; font-weight: 600; color: #1a3a5c; cursor: pointer; }
.md-toolbar button:hover { background: #e2eaf4; }
textarea { padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.95rem;
  font-weight: 400; font-family: inherit; resize: vertical; line-height: 1.45; }
.md-preview-title { font-size: 0.75rem; font-weight: 600; color: #888; text-transform: uppercase;
  letter-spacing: 0.04em; }
.md-preview { border: 1px dashed #dde4ec; border-radius: 5px; padding: 0.6rem 0.75rem;
  background: #fafcfe; font-size: 0.85rem; font-weight: 400; }
.md-preview-empty { color: #aaa; font-style: italic; }
/* The preview is injected with v-html, so its elements need :deep to be reached. */
.markdown-body :deep(h4) { font-size: 0.92rem; margin: 0.6rem 0 0.2rem; color: #1a3a5c; }
.markdown-body :deep(h5) { font-size: 0.85rem; margin: 0.5rem 0 0.2rem; color: #1a3a5c; }
.markdown-body :deep(p) { margin: 0.25rem 0; }
.markdown-body :deep(ul) { margin: 0.25rem 0; padding-left: 1.1rem; }
.markdown-body :deep(li) { margin: 0.1rem 0; }
.markdown-body :deep(> :first-child) { margin-top: 0; }
.markdown-body :deep(> :last-child) { margin-bottom: 0; }
</style>
