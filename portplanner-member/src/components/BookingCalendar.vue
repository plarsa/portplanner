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
          :class="{ taken: hourUnavailable(h), inRange: inRange(h), edge: h === startHour || h === endHour }"
          :disabled="hourUnavailable(h)"
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

// The exact instant the backend stops accepting a start, mirrored from RentalRuleValidator.
const latestStart = computed(() => {
  const d = new Date()
  d.setDate(d.getDate() + props.maxDaysAhead)
  return d
})
/** The last day that still holds at least one acceptable start hour. */
const lastSelectable = computed(() => startOfDay(latestStart.value))

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

/**
 * Whether an hour cannot open a booking: it is covered by a busy interval, it is not in the
 * future, or it is past the object's booking horizon. Hour 24 only ever ends a booking, so it
 * is judged by the range check in pickHour instead.
 */
function hourUnavailable(hour) {
  if (!selectedDate.value || hour === 24) return false
  const from = hourStart(hour)
  if (from <= new Date()) return true
  if (from > latestStart.value) return true
  const to = new Date(from)
  to.setHours(to.getHours() + 1)
  return props.busy.some(b => new Date(b.startAt) < to && new Date(b.endAt) > from)
}

function hourStart(hour) {
  return new Date(`${selectedDate.value}T${String(hour).padStart(2, '0')}:00:00`)
}

/** Finding 3: an end hour may not be dragged across an hour that is already taken. */
function rangeIsClear(from, to) {
  for (let h = from; h < to; h++) {
    if (hourUnavailable(h)) return false
  }
  return true
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
  if (!rangeIsClear(startHour.value, hour)) {
    rangeError.value = 'Någon timme i intervallet är redan bokad.'
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
.calendar {
  background: var(--surface);
  border: 1px solid var(--paper-border);
  border-radius: var(--radius);
  padding: 18px;
}
.cal-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.month { font-weight: 600; color: var(--ink); text-transform: capitalize; }
.cal-header button {
  background: var(--surface);
  border: 1px solid var(--paper-border);
  border-radius: var(--radius-sm);
  width: 2rem;
  height: 2rem;
  cursor: pointer;
  color: var(--sea);
  font-size: 1rem;
}
.cal-header button:hover:not(:disabled) { background: var(--sea-pale); }
.cal-header button:disabled { opacity: 0.35; cursor: not-allowed; }

.grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
.weekday {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  text-align: center;
  padding-bottom: 6px;
}

/* Every number on this surface is mono: it is the content, not decoration. */
.day, .hour { font-family: var(--font-mono); }
.day {
  aspect-ratio: 1;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: var(--paper-deep);
  color: var(--text);
  font-size: 0.85rem;
  cursor: pointer;
  padding: 0;
}
.day.blank { background: transparent; cursor: default; }
.day:disabled { color: var(--text-muted); background: var(--paper); cursor: not-allowed; }
/* A busy day is marked, but colour is never the only signal: it stays selectable
   and the hour grid shows which hours are actually taken. */
.day.busy { box-shadow: inset 0 -3px 0 var(--warn-border); }
.day.selectable:hover { background: var(--sea-pale); }
.day.on { background: var(--sea); color: #fff; }

.hours { margin-top: 18px; border-top: 1px solid var(--paper-border); padding-top: 14px; }
.hours-title {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-bottom: 10px;
}
.hour-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(2.6rem, 1fr)); gap: 4px; }
.hour {
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: var(--paper-deep);
  color: var(--text);
  padding: 6px 0;
  font-size: 0.78rem;
  cursor: pointer;
}
.hour.taken { background: var(--paper); color: var(--text-muted); cursor: not-allowed; text-decoration: line-through; }
.hour.inRange { background: var(--sea-pale); }
.hour.edge { background: var(--sea); color: #fff; font-weight: 600; }
.range-error { font-size: 0.88rem; color: var(--bad-text); margin: 6px 0 0; }
.hint { margin: 10px 0 0; }
</style>
