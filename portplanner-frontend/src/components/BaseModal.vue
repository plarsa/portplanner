<template>
  <teleport to="body">
    <div class="overlay" @click.self="$emit('close')">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ title }}</h3>
          <button class="close-btn" @click="$emit('close')">✕</button>
        </div>
        <div class="modal-body">
          <slot />
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="$emit('close')">Avbryt</button>
          <button class="btn-primary" @click="$emit('save')">{{ saveLabel }}</button>
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
.overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center; z-index: 100;
}
.modal {
  background: white; border-radius: 10px; width: 100%; max-width: 480px;
  max-height: 90vh; display: flex; flex-direction: column;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1.25rem 1.5rem; border-bottom: 1px solid #eee;
}
.modal-header h3 { margin: 0; font-size: 1.1rem; }
.close-btn { background: none; border: none; font-size: 1.1rem; cursor: pointer; color: #666; }
.modal-body { padding: 1.5rem; overflow-y: auto; }
.modal-footer {
  display: flex; justify-content: flex-end; gap: 0.75rem;
  padding: 1rem 1.5rem; border-top: 1px solid #eee;
}
.btn-primary {
  background: #1a3a5c; color: white; border: none;
  padding: 0.5rem 1.25rem; border-radius: 6px; cursor: pointer; font-size: 0.9rem;
}
.btn-primary:hover { background: #234e7a; }
.btn-secondary {
  background: #eee; color: #333; border: none;
  padding: 0.5rem 1.25rem; border-radius: 6px; cursor: pointer; font-size: 0.9rem;
}
.btn-secondary:hover { background: #ddd; }
</style>
