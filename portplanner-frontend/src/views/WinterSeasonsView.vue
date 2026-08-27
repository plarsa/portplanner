<template>
  <AppLayout>
    <div class="page-header">
      <h2>Vinterupptagning</h2>
      <button class="btn-primary" @click="openCreateSeason">+ Ny säsong</button>
    </div>

    <div v-if="!seasons.length && !loading" class="empty-state">Inga vintersäsonger skapade ännu.</div>

    <div v-for="s in seasons" :key="s.id" class="season-card">
      <div class="season-header">
        <div class="season-meta">
          <span class="season-name">{{ s.name }}</span>
          <span :class="['season-status', s.status.toLowerCase()]">{{ statusLabel(s.status) }}</span>
          <span class="season-dates" v-if="s.startDate">{{ s.startDate }} – {{ s.endDate }}</span>
        </div>
        <div class="season-actions">
          <router-link :to="`/winter/yard/${s.id}`" class="btn-secondary">Gårdsplan</router-link>
          <button class="btn-secondary" @click="openEditSeason(s)">Redigera</button>
          <button class="btn-danger-sm" @click="deleteSeason(s)">Ta bort</button>
        </div>
      </div>

      <!-- Slots -->
      <div class="slots-section">
        <div class="slots-header">
          <span class="section-label">Upptagningstider</span>
          <button class="btn-sm" @click="openCreateSlot(s)">+ Ny tid</button>
        </div>
        <div v-if="!slotsBySeason[s.id]?.length" class="slots-empty">Inga tider skapade</div>
        <div v-for="slot in slotsBySeason[s.id]" :key="slot.id" class="slot-row">
          <div class="slot-info">
            <span class="slot-date">{{ slot.slotDate }}</span>
            <span class="slot-time">{{ slot.startTime }} – {{ slot.endTime }}</span>
            <span class="slot-cap">{{ slot.bookedCount }}/{{ slot.capacity }} anmälda</span>
          </div>
          <div class="slot-actions">
            <button class="btn-sm" @click="openBookings(slot, s)">Anmälningar</button>
            <button class="btn-sm" @click="openEditSlot(slot, s)">Redigera</button>
            <button class="btn-danger-sm" @click="deleteSlot(slot, s)">Ta bort</button>
          </div>
        </div>
      </div>

      <!-- Pricing -->
      <div class="pricing-section">
        <div class="section-label">Prisregel</div>
        <div v-if="!pricingBySeason[s.id]?.length" class="slots-empty">
          Ingen prisregel —
          <button class="btn-inline" @click="openCreatePricing(s)">Lägg till</button>
        </div>
        <div v-else v-for="rule in pricingBySeason[s.id]" :key="rule.id" class="pricing-row">
          <span>{{ rule.pricePerSqm }} kr/m²</span>
          <span v-if="rule.extraWidthThresholdM">· Extra bredd &gt;{{ rule.extraWidthThresholdM }}m: {{ rule.extraWidthSurchargePerDm }} kr/dm</span>
          <span v-if="rule.extraLengthThresholdM">· Extra längd &gt;{{ rule.extraLengthThresholdM }}m: {{ rule.extraLengthSurchargePerDm }} kr/dm</span>
          <span v-if="rule.minPrice">· Minpris: {{ rule.minPrice }} kr</span>
          <button class="btn-sm" @click="openEditPricing(rule, s)">Redigera</button>
        </div>
      </div>
    </div>

    <!-- Season modal -->
    <BaseModal v-if="seasonModal" :title="editingSeason ? 'Redigera säsong' : 'Ny vintersäsong'"
               @close="seasonModal = false" @save="saveSeason">
      <div class="form-grid">
        <label>År *<input v-model.number="seasonForm.year" type="number" min="2020" max="2100" /></label>
        <label>Status
          <select v-model="seasonForm.status">
            <option value="PLANNING">Planering</option>
            <option value="ACTIVE">Aktiv</option>
            <option value="CLOSED">Avslutad</option>
          </select>
        </label>
        <label class="full">Namn *<input v-model="seasonForm.name" /></label>
        <label>Från<input v-model="seasonForm.startDate" type="date" /></label>
        <label>Till<input v-model="seasonForm.endDate" type="date" /></label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>

    <!-- Slot modal -->
    <BaseModal v-if="slotModal" :title="editingSlot ? 'Redigera tid' : 'Ny upptagningstid'"
               @close="slotModal = false" @save="saveSlot">
      <div class="form-grid">
        <label class="full">Datum *<input v-model="slotForm.slotDate" type="date" /></label>
        <label>Från *<input v-model="slotForm.startTime" type="time" /></label>
        <label>Till *<input v-model="slotForm.endTime" type="time" /></label>
        <label>Kapacitet<input v-model.number="slotForm.capacity" type="number" min="1" /></label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>

    <!-- Bookings panel -->
    <div v-if="activeSlot" class="detail-overlay" @click.self="activeSlot = null">
      <div class="detail-card wide">
        <div class="detail-header">
          <h3>Anmälningar — {{ activeSlot.slotDate }} {{ activeSlot.startTime }}–{{ activeSlot.endTime }}</h3>
          <button class="close-btn" @click="activeSlot = null">✕</button>
        </div>
        <div class="booking-actions">
          <button class="btn-primary" @click="openCreateBooking">+ Ny anmälan</button>
        </div>
        <table class="booking-table">
          <thead><tr><th>Båt</th><th>Ägare</th><th>Status</th><th>Pris</th><th></th></tr></thead>
          <tbody>
            <tr v-for="bk in activeBookings" :key="bk.id">
              <td>{{ bk.boatModel }}</td>
              <td>{{ bk.personName }}</td>
              <td><span :class="['status-chip', bk.status.toLowerCase()]">{{ bk.status }}</span></td>
              <td>
                <button v-if="bk.status === 'CONFIRMED'" class="btn-sm" @click="showPrice(bk)">Pris</button>
              </td>
              <td class="booking-row-actions">
                <button v-if="bk.status === 'REQUESTED'" class="btn-sm" @click="confirmBooking(bk)">Bekräfta</button>
                <button class="btn-danger-sm" @click="cancelBooking(bk)">Avboka</button>
              </td>
            </tr>
            <tr v-if="!activeBookings.length">
              <td colspan="5" class="empty">Inga anmälningar</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- New booking modal -->
    <BaseModal v-if="bookingModal" title="Ny anmälan" @close="bookingModal = false" @save="saveBooking">
      <div class="form-grid">
        <label class="full">Båt *
          <select v-model.number="bookingForm.boatId" @change="onBoatSelect">
            <option value="">Välj båt…</option>
            <option v-for="b in allBoats" :key="b.id" :value="b.id">{{ b.model }} ({{ b.ownerName }})</option>
          </select>
        </label>
        <label class="full">Ägare *
          <select v-model.number="bookingForm.personId">
            <option value="">Välj person…</option>
            <option v-for="p in allPersons" :key="p.id" :value="p.id">{{ p.firstName }} {{ p.lastName }}</option>
          </select>
        </label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>

    <!-- Pricing modal -->
    <BaseModal v-if="pricingModal" :title="editingPricing ? 'Redigera prisregel' : 'Ny prisregel'"
               @close="pricingModal = false" @save="savePricing">
      <div class="form-grid">
        <label class="full">Pris per m² (kr) *<input v-model.number="pricingForm.pricePerSqm" type="number" step="0.01" min="0" /></label>
        <label>Extra bredd &gt; (m)<input v-model.number="pricingForm.extraWidthThresholdM" type="number" step="0.1" /></label>
        <label>Tilläggsavgift (kr/dm)<input v-model.number="pricingForm.extraWidthSurchargePerDm" type="number" step="0.01" /></label>
        <label>Extra längd &gt; (m)<input v-model.number="pricingForm.extraLengthThresholdM" type="number" step="0.1" /></label>
        <label>Tilläggsavgift (kr/dm)<input v-model.number="pricingForm.extraLengthSurchargePerDm" type="number" step="0.01" /></label>
        <label class="full">Minpris (kr)<input v-model.number="pricingForm.minPrice" type="number" step="0.01" /></label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>

    <!-- Price popup -->
    <div v-if="priceResult" class="detail-overlay" @click.self="priceResult = null">
      <div class="detail-card">
        <div class="detail-header">
          <h3>Prisberäkning — {{ priceResult.boatModel }}</h3>
          <button class="close-btn" @click="priceResult = null">✕</button>
        </div>
        <dl class="price-grid">
          <dt>Yta</dt><dd>{{ priceResult.lengthM }} × {{ priceResult.widthM }} m = {{ priceResult.areaSqm }} m²</dd>
          <dt>Grundpris</dt><dd>{{ priceResult.basePrice }} kr</dd>
          <dt v-if="priceResult.widthSurcharge > 0">Breddtillägg</dt><dd v-if="priceResult.widthSurcharge > 0">{{ priceResult.widthSurcharge }} kr</dd>
          <dt v-if="priceResult.lengthSurcharge > 0">Längdtillägg</dt><dd v-if="priceResult.lengthSurcharge > 0">{{ priceResult.lengthSurcharge }} kr</dd>
          <dt class="total-label">Totalt</dt><dd class="total-value">{{ priceResult.totalPrice }} kr</dd>
        </dl>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
import {
  getWinterSeasons, createWinterSeason, updateWinterSeason, deleteWinterSeason,
  getHaulOutSlots, createHaulOutSlot, updateHaulOutSlot, deleteHaulOutSlot,
  getHaulOutBookingsBySlot, createHaulOutBooking, confirmHaulOutBooking, deleteHaulOutBooking,
  getBookingPrice,
  getPricingRules, createPricingRule, updatePricingRule
} from '../api/winterHaulOut'
import { getBoats } from '../api/boats'
import { getPersons } from '../api/persons'

const seasons = ref([])
const slotsBySeason = ref({})
const pricingBySeason = ref({})
const loading = ref(false)
const err = ref('')

const seasonModal = ref(false)
const editingSeason = ref(null)
const seasonForm = ref(emptySeasonForm())

const slotModal = ref(false)
const editingSlot = ref(null)
const activeSeasonForSlot = ref(null)
const slotForm = ref(emptySlotForm())

const activeSlot = ref(null)
const activeBookings = ref([])
const activeSeasonForBooking = ref(null)

const bookingModal = ref(false)
const bookingForm = ref({ boatId: '', personId: '' })
const allBoats = ref([])
const allPersons = ref([])

const pricingModal = ref(false)
const editingPricing = ref(null)
const activeSeasonForPricing = ref(null)
const pricingForm = ref(emptyPricingForm())

const priceResult = ref(null)

function emptySeasonForm() {
  return { year: new Date().getFullYear(), name: '', startDate: '', endDate: '', status: 'PLANNING' }
}
function emptySlotForm() {
  return { slotDate: '', startTime: '', endTime: '', capacity: 1 }
}
function emptyPricingForm() {
  return { pricePerSqm: '', extraWidthThresholdM: '', extraWidthSurchargePerDm: '', extraLengthThresholdM: '', extraLengthSurchargePerDm: '', minPrice: '' }
}

function statusLabel(s) {
  return { PLANNING: 'Planering', ACTIVE: 'Aktiv', CLOSED: 'Avslutad' }[s] ?? s
}

async function load() {
  loading.value = true
  try {
    const { data } = await getWinterSeasons()
    seasons.value = data
    await Promise.all(data.map(async (s) => {
      const [slotsRes, priceRes] = await Promise.all([getHaulOutSlots(s.id), getPricingRules(s.id)])
      slotsBySeason.value[s.id] = slotsRes.data
      pricingBySeason.value[s.id] = priceRes.data
    }))
  } finally { loading.value = false }
}

function openCreateSeason() { editingSeason.value = null; seasonForm.value = emptySeasonForm(); err.value = ''; seasonModal.value = true }
function openEditSeason(s) {
  editingSeason.value = s
  seasonForm.value = { year: s.year, name: s.name, startDate: s.startDate ?? '', endDate: s.endDate ?? '', status: s.status }
  err.value = ''; seasonModal.value = true
}
async function saveSeason() {
  err.value = ''
  try {
    const payload = { ...seasonForm.value, startDate: seasonForm.value.startDate || null, endDate: seasonForm.value.endDate || null }
    if (editingSeason.value) await updateWinterSeason(editingSeason.value.id, payload)
    else await createWinterSeason(payload)
    seasonModal.value = false; await load()
  } catch (e) { err.value = e.response?.data?.error || 'Något gick fel' }
}
async function deleteSeason(s) {
  if (!confirm(`Ta bort säsong ${s.name}?`)) return
  try { await deleteWinterSeason(s.id); await load() }
  catch (e) { alert(e.response?.data?.error || 'Kunde inte ta bort') }
}

function openCreateSlot(season) { editingSlot.value = null; activeSeasonForSlot.value = season; slotForm.value = emptySlotForm(); err.value = ''; slotModal.value = true }
function openEditSlot(slot, season) {
  editingSlot.value = slot; activeSeasonForSlot.value = season
  slotForm.value = { slotDate: slot.slotDate, startTime: slot.startTime, endTime: slot.endTime, capacity: slot.capacity }
  err.value = ''; slotModal.value = true
}
async function saveSlot() {
  err.value = ''
  try {
    const payload = { ...slotForm.value, seasonId: activeSeasonForSlot.value.id }
    if (editingSlot.value) await updateHaulOutSlot(editingSlot.value.id, payload)
    else await createHaulOutSlot(payload)
    slotModal.value = false; await load()
  } catch (e) { err.value = e.response?.data?.error || 'Något gick fel' }
}
async function deleteSlot(slot, season) {
  if (!confirm(`Ta bort tid ${slot.slotDate}?`)) return
  try { await deleteHaulOutSlot(slot.id); await load() }
  catch (e) { alert(e.response?.data?.error || 'Kunde inte ta bort') }
}

async function openBookings(slot, season) {
  activeSlot.value = slot; activeSeasonForBooking.value = season
  const { data } = await getHaulOutBookingsBySlot(slot.id)
  activeBookings.value = data
}
function openCreateBooking() { bookingForm.value = { boatId: '', personId: '' }; err.value = ''; bookingModal.value = true }
async function onBoatSelect() {
  const boat = allBoats.value.find(b => b.id === bookingForm.value.boatId)
  if (boat) bookingForm.value.personId = boat.ownerId
}
async function saveBooking() {
  err.value = ''
  try {
    await createHaulOutBooking({ slotId: activeSlot.value.id, boatId: bookingForm.value.boatId, personId: bookingForm.value.personId })
    bookingModal.value = false
    await openBookings(activeSlot.value, activeSeasonForBooking.value)
    await load()
  } catch (e) { err.value = e.response?.data?.error || 'Något gick fel' }
}
async function confirmBooking(bk) {
  await confirmHaulOutBooking(bk.id)
  await openBookings(activeSlot.value, activeSeasonForBooking.value)
}
async function cancelBooking(bk) {
  if (!confirm(`Avboka ${bk.boatModel}?`)) return
  await deleteHaulOutBooking(bk.id)
  await openBookings(activeSlot.value, activeSeasonForBooking.value)
  await load()
}
async function showPrice(bk) {
  const { data } = await getBookingPrice(bk.id)
  priceResult.value = data
}

function openCreatePricing(season) { editingPricing.value = null; activeSeasonForPricing.value = season; pricingForm.value = emptyPricingForm(); err.value = ''; pricingModal.value = true }
function openEditPricing(rule, season) {
  editingPricing.value = rule; activeSeasonForPricing.value = season
  pricingForm.value = { pricePerSqm: rule.pricePerSqm, extraWidthThresholdM: rule.extraWidthThresholdM ?? '', extraWidthSurchargePerDm: rule.extraWidthSurchargePerDm ?? '', extraLengthThresholdM: rule.extraLengthThresholdM ?? '', extraLengthSurchargePerDm: rule.extraLengthSurchargePerDm ?? '', minPrice: rule.minPrice ?? '' }
  err.value = ''; pricingModal.value = true
}
async function savePricing() {
  err.value = ''
  try {
    const nullify = v => (v === '' ? null : v)
    const payload = {
      seasonId: activeSeasonForPricing.value.id,
      pricePerSqm: pricingForm.value.pricePerSqm,
      extraWidthThresholdM: nullify(pricingForm.value.extraWidthThresholdM),
      extraWidthSurchargePerDm: nullify(pricingForm.value.extraWidthSurchargePerDm),
      extraLengthThresholdM: nullify(pricingForm.value.extraLengthThresholdM),
      extraLengthSurchargePerDm: nullify(pricingForm.value.extraLengthSurchargePerDm),
      minPrice: nullify(pricingForm.value.minPrice)
    }
    if (editingPricing.value) await updatePricingRule(editingPricing.value.id, payload)
    else await createPricingRule(payload)
    pricingModal.value = false; await load()
  } catch (e) { err.value = e.response?.data?.error || 'Något gick fel' }
}

onMounted(async () => {
  await load()
  const [boatsRes, personsRes] = await Promise.all([getBoats(), getPersons()])
  allBoats.value = boatsRes.data
  allPersons.value = personsRes.data
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; }
h2 { font-size: 1.5rem; }
.empty-state { color: #999; text-align: center; padding: 3rem; }

.season-card { background: white; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.07); margin-bottom: 1.25rem; overflow: hidden; }
.season-header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.25rem; border-bottom: 1px solid #eee; flex-wrap: wrap; gap: 0.5rem; }
.season-meta { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; }
.season-name { font-weight: 700; font-size: 1.05rem; }
.season-dates { font-size: 0.82rem; color: #888; }
.season-status { font-size: 0.72rem; font-weight: 700; padding: 0.15rem 0.5rem; border-radius: 10px; }
.season-status.planning { background: #fff3cd; color: #856404; }
.season-status.active { background: #d4edda; color: #155724; }
.season-status.closed { background: #f0f0f0; color: #666; }
.season-actions { display: flex; gap: 0.5rem; }

.slots-section { padding: 0.75rem 1.25rem; border-bottom: 1px solid #f0f0f0; }
.pricing-section { padding: 0.75rem 1.25rem; }
.slots-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
.section-label { font-size: 0.72rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: #888; }
.slots-empty { font-size: 0.85rem; color: #bbb; }
.slot-row { display: flex; justify-content: space-between; align-items: center; padding: 0.4rem 0; border-bottom: 1px solid #f8f8f8; flex-wrap: wrap; gap: 0.4rem; }
.slot-row:last-child { border-bottom: none; }
.slot-info { display: flex; gap: 0.75rem; font-size: 0.88rem; align-items: center; }
.slot-date { font-weight: 600; }
.slot-time { color: #555; }
.slot-cap { font-size: 0.78rem; color: #888; }
.slot-actions { display: flex; gap: 0.4rem; }
.pricing-row { display: flex; gap: 1rem; align-items: center; flex-wrap: wrap; font-size: 0.88rem; }

.btn-primary { background: #1a3a5c; color: white; padding: 0.5rem 1.1rem; border-radius: 6px; border: none; cursor: pointer; font-size: 0.85rem; }
.btn-primary:hover { background: #234e7a; }
.btn-secondary { background: #e8f0fe; color: #1a3a5c; padding: 0.4rem 0.8rem; border-radius: 5px; border: none; cursor: pointer; font-size: 0.82rem; text-decoration: none; display: inline-block; }
.btn-secondary:hover { background: #d0e2ff; }
.btn-sm { background: #e8f0fe; color: #1a3a5c; padding: 0.25rem 0.6rem; border-radius: 4px; border: none; cursor: pointer; font-size: 0.78rem; }
.btn-sm:hover { background: #d0e2ff; }
.btn-danger-sm { background: #fee; color: #c0392b; padding: 0.25rem 0.6rem; border-radius: 4px; border: none; cursor: pointer; font-size: 0.78rem; }
.btn-danger-sm:hover { background: #fcc; }
.btn-inline { background: none; border: none; color: #1a3a5c; cursor: pointer; font-size: 0.85rem; text-decoration: underline; padding: 0; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.form-grid .full { grid-column: 1 / -1; }
label { display: flex; flex-direction: column; font-size: 0.85rem; font-weight: 600; gap: 0.3rem; }
label input, label select { padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.95rem; font-weight: 400; }
.error { color: #c0392b; font-size: 0.85rem; margin-top: 0.5rem; }

.detail-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); z-index: 100; display: flex; align-items: center; justify-content: center; }
.detail-card { background: white; border-radius: 12px; width: 480px; max-width: 95vw; box-shadow: 0 8px 32px rgba(0,0,0,0.18); overflow: hidden; }
.detail-card.wide { width: 680px; }
.detail-header { display: flex; justify-content: space-between; align-items: flex-start; padding: 1rem 1.4rem; border-bottom: 1px solid #eee; }
.detail-header h3 { font-size: 1rem; margin: 0; }
.close-btn { background: none; border: none; font-size: 1.1rem; color: #888; cursor: pointer; padding: 0.2rem 0.4rem; }
.close-btn:hover { color: #333; background: #f0f0f0; border-radius: 4px; }

.booking-actions { padding: 0.75rem 1.4rem; border-bottom: 1px solid #eee; }
.booking-table { width: 100%; border-collapse: collapse; }
.booking-table th { background: #f8f8f8; padding: 0.6rem 1rem; text-align: left; font-size: 0.82rem; color: #555; border-bottom: 1px solid #eee; }
.booking-table td { padding: 0.6rem 1rem; border-bottom: 1px solid #f0f0f0; font-size: 0.88rem; }
.booking-row-actions { display: flex; gap: 0.4rem; white-space: nowrap; }
.status-chip { font-size: 0.7rem; font-weight: 700; padding: 0.15rem 0.45rem; border-radius: 8px; }
.status-chip.requested { background: #fff3cd; color: #856404; }
.status-chip.confirmed { background: #d4edda; color: #155724; }
.status-chip.completed { background: #cce5ff; color: #004085; }
.status-chip.cancelled { background: #f0f0f0; color: #999; }
.empty { color: #999; text-align: center; padding: 1.5rem; }

.price-grid { display: grid; grid-template-columns: 140px 1fr; gap: 0.5rem 1rem; padding: 1.2rem 1.4rem; margin: 0; }
dt { font-size: 0.82rem; font-weight: 600; color: #888; }
dd { font-size: 0.9rem; color: #222; margin: 0; }
.total-label { color: #1a3a5c; font-weight: 700; font-size: 0.9rem; }
.total-value { color: #1a3a5c; font-weight: 700; font-size: 1.1rem; }
</style>
