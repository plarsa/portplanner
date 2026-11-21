<template>
  <teleport to="body">
    <div class="modal-backdrop" @click.self="$emit('close')">
      <div class="modal">
        <div class="modal-head">
          <h3>{{ title }}</h3>
          <button class="close-btn" aria-label="Stäng" @click="$emit('close')">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
          </button>
        </div>
        <div class="modal-body">
          <slot />
        </div>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="$emit('close')">Avbryt</button>
          <button class="btn btn-primary btn-sm" @click="$emit('save')">{{ saveLabel }}</button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
defineProps({ title: String, saveLabel: { type: String, default: 'Spara' } })
defineEmits(['close', 'save'])
</script>

<style scoped>
.modal { max-height: 92vh; display: flex; flex-direction: column; }
.modal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 22px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--paper-border);
}
.modal-head h3 { margin: 0; }
.close-btn { background: none; border: 0; padding: 4px; cursor: pointer; color: var(--text-muted); }
.close-btn:hover { color: var(--ink); }
.close-btn svg { width: 18px; height: 18px; }
.modal-body { padding: 14px 0 0; overflow-y: auto; }
.modal-actions { padding-top: 14px; border-top: 1px solid var(--paper-border); }
</style>
