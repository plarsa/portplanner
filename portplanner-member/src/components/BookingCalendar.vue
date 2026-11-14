<template>
  <div class="calendar">
    <div class="cal-header">
      <button :disabled="!canGoBack" @click="shiftMonth(-1)">‹</button>
      <span class="month">{{ monthLabel }}</span>
      <button :disabled="!canGoForward" @click="shiftMonth(1)">›</button>
    </div>

    <div class="grid">
      <div v-for="d in WEEKDAYS" :key="d" class="weekday">{{ d }}</div>
      <div v-for="blank in leadingBlanks" :key="'b' + blank" class="day blank"></div>
      <button
        v-for="day in days"
        :key="day.date"
        class="day"
        :class="{ busy: day.busy, selectable: day.selectable, on: day.date === selectedDate }"
        :disabled="!day.selectable"
        @click="pickDay(day)">
        {{ day.number }}
      </button>
    </div>

    <div v-if="selectedDate" class="hours">
      <div class="hours-title">{{ selectedDate }} — välj start och slut</div>
      <div class="hour-grid">
        <button
          v-for="h in HOURS"
          :key="h"
          class="hour"
          :class="{ taken: hourTaken(h), inRange: inRange(h), edge: h === startHour || h === endHour }"
          :disabled="hourTaken(h)"
          @click="pickHour(h)">
          {{ String(h).padStart(2, '0') }}
        </button>
      </div>
      <p class="hint">
        <template v-if="startHour === null">Klicka på starttimmen.</template>
        <template v-else-if="endHour === null">Klicka på sluttimmen (bokningen slutar då).</template>
        <template v-else>{{ rangeLabel }}</template>
      </p>
      <p v-if="rangeError" class="range-error">{{ rangeError }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  busy: { type: Array, default: () => [] },
  maxDurationHours: { type: Number, default: 24 },
  maxDaysAhead: { type: Number, default: 30 },
})
const emit = defineEmits(['select'])

const WEEKDAYS = ['Mån', 'Tis', 'Ons', 'Tor', 'Fre', 'Lör', 'Sön']
// A booking ends at the hour clicked, so 0..24 are both pickable as edges.
const HOURS = Array.from({ length: 25 }, (_, i) => i)

const today = startOfDay(new Date())
const cursor = ref(new Date(today.getFullYear(), today.getMonth(), 1))
const selectedDate = ref(null)
const startHour = ref(null)
const endHour = ref(null)
const rangeError = ref('')

function startOfDay(d) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

const lastSelectable = computed(() => {
  const d = new Date(today)
  d.setDate(d.getDate() + props.maxDaysAhead)
  return d
})

function iso(d) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const monthLabel = computed(() =>
  cursor.value.toLocaleDateString('sv-SE', { month: 'long', year: 'numeric' }))

// Monday-first grid, so Sunday (0) becomes the seventh column.
const leadingBlanks = computed(() => {
  const first = new Date(cursor.value.getFullYear(), cursor.value.getMonth(), 1)
  return (first.getDay() + 6) % 7
})

const days = computed(() => {
  const year = cursor.value.getFullYear()
  const month = cursor.value.getMonth()
  const count = new Date(year, month + 1, 0).getDate()
  return Array.from({ length: count }, (_, i) => {
    const date = new Date(year, month, i + 1)
    return {
      number: i + 1,
      date: iso(date),
      busy: props.busy.some(b => overlapsDay(b, date)),
      selectable: date >= today && date <= lastSelectable.value,
    }
  })
})

function overlapsDay(interval, date) {
  const dayStart = new Date(date)
  const dayEnd = new Date(date)
  dayEnd.setDate(dayEnd.getDate() + 1)
  return new Date(interval.startAt) < dayEnd && new Date(interval.endAt) > dayStart
}

const canGoBack = computed(() =>
  cursor.value > new Date(today.getFullYear(), today.getMonth(), 1))
const canGoForward = computed(() =>
  cursor.value < new Date(lastSelectable.value.getFullYear(), lastSelectable.value.getMonth(), 1))

function shiftMonth(delta) {
  cursor.value = new Date(cursor.value.getFullYear(), cursor.value.getMonth() + delta, 1)
}

function pickDay(day) {
  selectedDate.value = day.date
  startHour.value = null
  endHour.value = null
  rangeError.value = ''
}

/** An hour cell is taken when the hour it opens is covered by a busy interval. */
function hourTaken(hour) {
  if (!selectedDate.value || hour === 24) return false
  const from = new Date(`${selectedDate.value}T${String(hour).padStart(2, '0')}:00:00`)
  const to = new Date(from)
  to.setHours(to.getHours() + 1)
  if (to <= new Date()) return true
  return props.busy.some(b => new Date(b.startAt) < to && new Date(b.endAt) > from)
}

function inRange(hour) {
  return startHour.value !== null && endHour.value !== null
    && hour > startHour.value && hour < endHour.value
}

function pickHour(hour) {
  rangeError.value = ''
  if (startHour.value === null || endHour.value !== null) {
    startHour.value = hour
    endHour.value = null
    return
  }
  if (hour <= startHour.value) {
    startHour.value = hour
    return
  }
  if (hour - startHour.value > props.maxDurationHours) {
    rangeError.value = `Längst ${props.maxDurationHours} timmar.`
    return
  }
  endHour.value = hour
}

const rangeLabel = computed(() => {
  if (startHour.value === null || endHour.value === null) return ''
  const hours = endHour.value - startHour.value
  return `${String(startHour.value).padStart(2, '0')}:00–${String(endHour.value).padStart(2, '0')}:00 (${hours} h)`
})

watch([startHour, endHour, selectedDate], () => {
  if (selectedDate.value === null || startHour.value === null || endHour.value === null) {
    emit('select', null)
    return
  }
  const pad = (n) => String(n).padStart(2, '0')
  emit('select', {
    startAt: `${selectedDate.value}T${pad(startHour.value)}:00:00`,
    endAt: endHour.value === 24
      ? `${iso(nextDay(selectedDate.value))}T00:00:00`
      : `${selectedDate.value}T${pad(endHour.value)}:00:00`,
  })
})

function nextDay(dateStr) {
  const d = new Date(`${dateStr}T00:00:00`)
  d.setDate(d.getDate() + 1)
  return d
}
</script>

<style scoped>
.calendar { background: white; border-radius: 10px; padding: 1rem; box-shadow: 0 2px 6px rgba(0,0,0,0.07); }
.cal-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.75rem; }
.month { font-weight: 700; font-size: 0.95rem; text-transform: capitalize; }
.cal-header button { background: #eef2f7; border: none; border-radius: 6px; width: 2rem; height: 2rem; cursor: pointer; font-size: 1rem; }
.cal-header button:disabled { opacity: 0.35; cursor: not-allowed; }

.grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 0.25rem; }
.weekday { font-size: 0.68rem; color: #999; text-align: center; font-weight: 700; padding-bottom: 0.25rem; }
.day { aspect-ratio: 1; border: none; border-radius: 6px; background: #f6f8fb; color: #333;
  font-size: 0.85rem; cursor: pointer; padding: 0; }
.day.blank { background: transparent; cursor: default; }
.day:disabled { color: #ccc; cursor: not-allowed; background: #fafafa; }
.day.busy { box-shadow: inset 0 -3px 0 #e0a030; }
.day.selectable:hover { background: #dce8f6; }
.day.on { background: #1a3a5c; color: white; }

.hours { margin-top: 1rem; border-top: 1px solid #eee; padding-top: 0.75rem; }
.hours-title { font-size: 0.8rem; font-weight: 700; color: #555; margin-bottom: 0.5rem; }
.hour-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(2.6rem, 1fr)); gap: 0.25rem; }
.hour { border: none; border-radius: 5px; background: #f6f8fb; padding: 0.4rem 0; font-size: 0.78rem; cursor: pointer; }
.hour.taken { background: #f0f0f0; color: #c4c4c4; cursor: not-allowed; text-decoration: line-through; }
.hour.inRange { background: #dce8f6; }
.hour.edge { background: #1a3a5c; color: white; font-weight: 700; }
.hint { font-size: 0.78rem; color: #777; margin: 0.5rem 0 0; }
.range-error { font-size: 0.78rem; color: #c0392b; margin: 0.25rem 0 0; }
</style>
