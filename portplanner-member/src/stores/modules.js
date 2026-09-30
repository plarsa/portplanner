import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getModules } from '../api/modules'

export const useModulesStore = defineStore('modules', () => {
  const list = ref([])
  const loaded = ref(false)
  const notice = ref('')

  function find(id) {
    return list.value.find(m => m.id === id)
  }

  async function load() {
    try {
      list.value = await getModules()
    } catch {
      list.value = []
    }
    loaded.value = true
  }

  function clear() {
    list.value = []
    loaded.value = false
    notice.value = ''
  }

  const isAccessible = id => !!find(id)?.accessible
  const canWrite = id => find(id)?.access === 'write'
  const settings = id => find(id)?.settings ?? {}

  return { list, loaded, notice, load, clear, isAccessible, canWrite, settings }
})
