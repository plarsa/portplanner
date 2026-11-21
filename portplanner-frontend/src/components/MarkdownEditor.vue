<template>
  <div class="md-editor">
    <div class="md-toolbar">
      <button class="btn btn-secondary btn-sm" v-for="b in BUTTONS" :key="b.kind" type="button" :title="b.title"
              @click="apply(b.kind)">{{ b.label }}</button>
    </div>
    <textarea class="input" ref="area" :value="modelValue" :rows="rows" :placeholder="placeholder"
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
.md-editor { display: flex; flex-direction: column; gap: 8px; }
.md-toolbar { display: flex; flex-wrap: wrap; gap: 6px; }
.md-toolbar .btn { padding: 5px 10px; font-size: 0.8rem; }
textarea.input { font-family: var(--font); resize: vertical; line-height: 1.45; }
.md-preview-title {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.md-preview {
  border: 1px dashed var(--paper-border);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  background: var(--paper);
  font-size: 0.9rem;
}
.md-preview-empty { color: var(--text-muted); }
/* The preview is injected with v-html, so its elements need :deep to be reached. */
.markdown-body :deep(h4) { font-size: 0.95rem; margin: 10px 0 2px; color: var(--ink); }
.markdown-body :deep(h5) { font-size: 0.9rem; margin: 10px 0 2px; color: var(--ink); }
.markdown-body :deep(p) { margin: 4px 0; }
.markdown-body :deep(ul) { margin: 4px 0; padding-left: 18px; }
.markdown-body :deep(li) { margin: 2px 0; }
.markdown-body :deep(> :first-child) { margin-top: 0; }
.markdown-body :deep(> :last-child) { margin-bottom: 0; }
</style>
