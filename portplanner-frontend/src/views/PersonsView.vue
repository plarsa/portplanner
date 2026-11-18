<template>
  <AppLayout>
    <div class="page-header">
      <h2>Personer</h2>
      <div class="header-right">
        <div class="search-bar">
          <input v-model="search" placeholder="Sök namn eller e-post…" @input="load" />
        </div>
        <button class="btn-primary" @click="openCreate">+ Ny person</button>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Namn</th><th class="col-email">E-post</th><th>Telefon</th><th class="col-address">Adress</th><th class="col-boats">Båtar</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in persons" :key="p.id" class="clickable-row" @click="openDetail(p)">
            <td>{{ p.firstName }} {{ p.lastName }}</td>
            <td class="col-email">{{ p.email }}</td>
            <td>{{ p.phone || '–' }}</td>
            <td class="col-address address-cell">
              <span v-if="p.address">{{ p.address }}<span v-if="p.postalCode">, {{ p.postalCode }}</span></span>
              <span v-else class="muted">–</span>
            </td>
            <td class="col-boats">{{ p.boatCount }}</td>
          </tr>
          <tr v-if="!persons.length">
            <td colspan="5" class="empty">Inga personer hittades</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="detail" class="detail-overlay" @click.self="detail = null">
      <div class="detail-card">
        <div class="detail-header">
          <h3>{{ detail.person.firstName }} {{ detail.person.lastName }}</h3>
          <button class="close-btn" @click="detail = null">✕</button>
        </div>
        <div class="detail-scroll">
          <dl class="detail-grid">
            <dt>E-post</dt><dd>{{ detail.person.email || '–' }}</dd>
            <dt>Telefon</dt><dd>{{ detail.person.phone || '–' }}</dd>
            <dt>Adress</dt><dd>{{ detail.person.address ? detail.person.address + (detail.person.postalCode ? ', ' + detail.person.postalCode : '') : '–' }}</dd>
            <dt>Fastighetsbeteckning</dt><dd>{{ detail.person.propertyDesignation || '–' }}</dd>
            <dt v-if="detail.person.notes">Noteringar</dt>
            <dd v-if="detail.person.notes" class="notes-text">{{ detail.person.notes }}</dd>
          </dl>
          <div class="account-section">
            <div class="boats-title">Konto</div>
            <div v-if="accountFor(detail.person)" class="account-line">
              <span class="account-user">{{ accountFor(detail.person).username }}</span>
              <span class="account-roles">{{ accountFor(detail.person).roles.join(', ') }}</span>
              <span :class="['account-status', accountFor(detail.person).status.toLowerCase()]">
                {{ statusText(accountFor(detail.person).status) }}
              </span>
            </div>
            <div v-else class="boats-empty">Inget konto</div>
            <div v-if="accountFor(detail.person)?.lastLoginAt" class="account-login">
              Senaste inloggning {{ formatDateTime(accountFor(detail.person).lastLoginAt) }}
            </div>
          </div>
          <div class="boats-section">
            <div class="boats-title">Båtar</div>
            <div v-if="detailLoading" class="boats-loading">Laddar…</div>
            <div v-else-if="!detail.boats.length" class="boats-empty">Inga registrerade båtar</div>
            <div v-for="b in detail.boats" :key="b.id" class="boat-block">
              <div class="boat-block-header">
                <span class="boat-block-name">{{ b.model }}</span>
                <span class="boat-block-dims">{{ b.widthM }} m × {{ b.lengthM }} m</span>
              </div>
              <div v-if="b.assignment" class="boat-assignment">
                <span class="assign-chip occupied">Tilldelad</span>
                <span class="assign-where">{{ b.assignment.dockName }} · Plats {{ b.assignment.slipNumber }}</span>
                <span class="assign-since">fr.o.m. {{ formatDate(b.assignment.assignedDate) }}</span>
              </div>
              <div v-else class="boat-assignment">
                <span class="assign-chip free">Ingen plats</span>
              </div>
            </div>
          </div>
        </div>
        <div class="detail-footer">
          <button class="btn-danger" @click="removeFromDetail">Ta bort</button>
          <button v-if="accountFor(detail.person)" @click="resetFor(detail.person)">Nytt lösenord</button>
          <button v-else @click="openCreateAccount(detail.person)">Skapa konto</button>
          <button class="btn-primary" @click="openEdit(detail.person); detail = null">Redigera</button>
        </div>
      </div>
    </div>

    <BaseModal v-if="accountModal" title="Skapa konto" saveLabel="Skapa"
               @close="accountModal = false" @save="createAccount">
      <div class="form-grid">
        <label class="full">E-post
          <input :value="accountForm.email" readonly />
        </label>
        <div class="full roles-field">
          <span class="roles-title">Roller *</span>
          <label v-for="r in roles" :key="r" class="role-check">
            <input type="checkbox" :value="r" v-model="accountForm.roles" />{{ r }}
          </label>
        </div>
      </div>
      <p class="hint">Kontot får ett lösenord som visas en gång.</p>
      <p v-if="accountErr" class="error">{{ accountErr }}</p>
    </BaseModal>

    <div v-if="password" class="detail-overlay" @click.self="password = null">
      <div class="pw-card">
        <div class="detail-header">
          <h3>Lösenord för {{ password.username }}</h3>
          <button class="close-btn" @click="password = null">✕</button>
        </div>
        <div class="pw-body">
          <code class="pw-value">{{ password.value }}</code>
          <p class="pw-note">Visas bara nu och kan inte hämtas igen. Skriv ner det.</p>
          <p class="pw-note">Kontots aktiva sessioner har avslutats.</p>
          <p v-if="password.isSelf" class="pw-warning">
            Det här är ditt eget konto, så du blir utloggad. Logga in med det nya lösenordet.
          </p>
        </div>
        <div class="detail-footer">
          <button class="btn-primary" @click="password = null">Klar</button>
        </div>
      </div>
    </div>

    <BaseModal v-if="modal" :title="editing ? 'Redigera person' : 'Ny person'" @close="modal = false" @save="save">
      <div class="form-grid">
        <label>Förnamn *<input v-model="form.firstName" required /></label>
        <label>Efternamn *<input v-model="form.lastName" required /></label>
        <label class="full">E-post *<input v-model="form.email" type="email" required /></label>
        <label>Telefon<input v-model="form.phone" /></label>
        <label>Postnummer<input v-model="form.postalCode" /></label>
        <label class="full">Adress<input v-model="form.address" placeholder="Gatuadress" /></label>
        <label class="full">Fastighetsbeteckning<input v-model="form.propertyDesignation" placeholder="t.ex. Örby 1:23" /></label>
        <label class="full">Noteringar<textarea v-model="form.notes" rows="4" placeholder="Fri text…" /></label>
      </div>
      <p v-if="err" class="error">{{ err }}</p>
    </BaseModal>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import BaseModal from '../components/BaseModal.vue'
import { getPersons, createPerson, updatePerson, deletePerson } from '../api/persons'
import { getBoats } from '../api/boats'
import { getAssignments } from '../api/assignments'
import { getUsers, getAssignableRoles, createUser, resetPassword } from '../api/users'
import { useAuthStore } from '../stores/auth'

const persons = ref([])
const search = ref('')
const modal = ref(false)
const editing = ref(null)
const detail = ref(null)
const detailLoading = ref(false)
const err = ref('')
const auth = useAuthStore()
/** personId -> account, so the panel knows about the account without a second request. */
const accountByPerson = ref({})
const roles = ref([])
const accountModal = ref(false)
const accountForm = ref({ personId: null, email: '', roles: ['member'] })
const accountErr = ref('')
const password = ref(null)

const accountFor = (p) => accountByPerson.value[p.id] ?? null

const STATUS_TEXT = { INVITED: 'Inbjudet', ACTIVE: 'Aktivt', DISABLED: 'Avstängt' }
const statusText = (s) => STATUS_TEXT[s] ?? s

function formatDateTime(t) {
  return t ? new Date(t).toLocaleString('sv-SE', { dateStyle: 'short', timeStyle: 'short' }) : ''
}

function showPassword(account, temporaryPassword) {
  password.value = {
    username: account.username,
    value: temporaryPassword,
    isSelf: account.username === auth.username,
  }
}

function openCreateAccount(p) {
  accountErr.value = ''
  accountForm.value = { personId: p.id, email: p.email, roles: ['member'] }
  accountModal.value = true
}

async function createAccount() {
  accountErr.value = ''
  if (accountForm.value.roles.length === 0) {
    accountErr.value = 'Välj minst en roll'
    return
  }
  try {
    const { data } = await createUser({
      email: accountForm.value.email,
      // The API wants role:level, while /roles gives bare names. An account made here
      // always gets write access.
      roles: accountForm.value.roles.map((r) => `${r}:rw`),
      personId: accountForm.value.personId,
    })
    accountModal.value = false
    await loadAccounts()
    showPassword(data.user, data.temporaryPassword)
  } catch (e) {
    accountErr.value = e.response?.data?.error || 'Kunde inte skapa kontot'
  }
}

async function resetFor(p) {
  const account = accountFor(p)
  if (!account) return
  if (!confirm(`Ge ${account.username} ett nytt lösenord? Det gamla slutar gälla direkt.`)) return
  try {
    const { data } = await resetPassword(account.id)
    await loadAccounts()
    showPassword(data.user, data.temporaryPassword)
  } catch (e) {
    alert(e.response?.data?.error || 'Kunde inte byta lösenord')
  }
}

async function loadAccounts() {
  // A role that may read persons but not accounts gets 403 here. The person list must still
  // work for them, so a failure leaves the map empty and the panel simply shows no account.
  let data
  try {
    data = (await getUsers()).data
  } catch {
    accountByPerson.value = {}
    return
  }
  const map = {}
  // The configured superadmin has no id and no person, so it never lands here.
  for (const u of data) {
    if (u.personId != null && u.id != null) map[u.personId] = u
  }
  accountByPerson.value = map
}

const emptyForm = () => ({ firstName: '', lastName: '', email: '', phone: '', address: '', postalCode: '', propertyDesignation: '', notes: '' })
const form = ref(emptyForm())

async function load() {
  const { data } = await getPersons(search.value)
  persons.value = data
  await loadAccounts()
}

function formatDate(d) {
  return d ? new Date(d + 'T00:00:00').toLocaleDateString('sv-SE') : '–'
}

async function openDetail(p) {
  detail.value = { person: p, boats: [] }
  detailLoading.value = true
  try {
    const [{ data: boats }, { data: assignments }] = await Promise.all([getBoats(p.id), getAssignments()])
    const activeByBoat = {}
    for (const a of assignments) { if (a.status === 'ACTIVE') activeByBoat[a.boatId] = a }
    detail.value.boats = boats.map(b => ({ ...b, assignment: activeByBoat[b.id] ?? null }))
  } finally { detailLoading.value = false }
}

function openCreate() { editing.value = null; form.value = emptyForm(); err.value = ''; modal.value = true }
function openEdit(p) {
  editing.value = p
  form.value = { firstName: p.firstName, lastName: p.lastName, email: p.email, phone: p.phone || '', address: p.address || '', postalCode: p.postalCode || '', propertyDesignation: p.propertyDesignation || '', notes: p.notes || '' }
  err.value = ''; modal.value = true
}

async function save() {
  err.value = ''
  try {
    if (editing.value) await updatePerson(editing.value.id, form.value)
    else await createPerson(form.value)
    modal.value = false; await load()
  } catch (e) { err.value = e.response?.data?.error || 'Något gick fel' }
}

async function remove(p) {
  if (!confirm(`Ta bort ${p.firstName} ${p.lastName}?`)) return
  try { await deletePerson(p.id); await load() }
  catch (e) { alert(e.response?.data?.error || 'Kunde inte ta bort personen') }
}

async function removeFromDetail() {
  const p = detail.value.person
  if (!confirm(`Ta bort ${p.firstName} ${p.lastName}?`)) return
  try { await deletePerson(p.id); detail.value = null; await load() }
  catch (e) { alert(e.response?.data?.error || 'Kunde inte ta bort personen') }
}

onMounted(async () => {
  await load()
  // The roles the create-account modal offers; harmless to fetch once up front.
  try {
    roles.value = (await getAssignableRoles()).data
  } catch { roles.value = ['member'] }
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; flex-wrap: wrap; gap: 0.75rem; }
h2 { font-size: 1.5rem; }
.header-right { display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap; }
.search-bar input { padding: 0.5rem 0.8rem; border: 1px solid #ddd; border-radius: 6px; width: min(280px, 100%); }
.table-wrap { background: white; border-radius: 10px; overflow-x: auto; -webkit-overflow-scrolling: touch; box-shadow: 0 2px 8px rgba(0,0,0,0.07); }
table { width: 100%; border-collapse: collapse; }
th { background: #f8f8f8; padding: 0.75rem 1rem; text-align: left; font-size: 0.85rem; color: #555; border-bottom: 1px solid #eee; }
td { padding: 0.75rem 1rem; border-bottom: 1px solid #f0f0f0; font-size: 0.9rem; }
td.address-cell { font-size: 0.85rem; color: #444; max-width: 200px; }
.muted { color: #bbb; }
.empty { color: #999; text-align: center; padding: 2rem; }
button { padding: 0.35rem 0.8rem; border-radius: 5px; border: none; cursor: pointer; font-size: 0.85rem; background: #e8f0fe; color: #1a3a5c; }
button:hover { background: #d0e2ff; }
.btn-primary { background: #1a3a5c; color: white; padding: 0.5rem 1.1rem; border-radius: 6px; border: none; cursor: pointer; }
.btn-primary:hover { background: #234e7a; }
.btn-danger { background: #fee; color: #c0392b; padding: 0.5rem 1.1rem; border-radius: 6px; border: none; cursor: pointer; }
.btn-danger:hover { background: #fcc; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.form-grid .full { grid-column: 1 / -1; }
label { display: flex; flex-direction: column; font-size: 0.85rem; font-weight: 600; gap: 0.3rem; }
label input, label textarea { padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.95rem; font-weight: 400; }
label textarea { resize: vertical; font-family: inherit; }
.error { color: #c0392b; font-size: 0.85rem; margin-top: 0.5rem; }
.clickable-row { cursor: pointer; transition: background 0.1s; }
.clickable-row:hover { background: #f5f8ff; }
.detail-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); z-index: 100; display: flex; align-items: center; justify-content: center; }
.detail-card { background: white; border-radius: 12px; width: 480px; max-width: 95vw; height: min(85vh, 720px); display: flex; flex-direction: column; box-shadow: 0 8px 32px rgba(0,0,0,0.18); overflow: hidden; }
.detail-header { flex-shrink: 0; display: flex; justify-content: space-between; align-items: center; padding: 1.1rem 1.4rem; border-bottom: 1px solid #eee; }
.detail-header h3 { font-size: 1.15rem; margin: 0; }
.close-btn { background: none; border: none; font-size: 1.1rem; color: #888; cursor: pointer; padding: 0.2rem 0.4rem; }
.close-btn:hover { color: #333; background: #f0f0f0; border-radius: 4px; }
.detail-scroll { flex: 1; overflow-y: auto; min-height: 0; }
.detail-grid { display: grid; grid-template-columns: 150px 1fr; gap: 0.5rem 1rem; padding: 1.2rem 1.4rem; margin: 0; }
dt { font-size: 0.82rem; font-weight: 600; color: #888; align-self: start; padding-top: 0.1rem; }
dd { font-size: 0.9rem; color: #222; margin: 0; }
.notes-text { white-space: pre-wrap; color: #555; font-style: italic; }
.detail-footer { flex-shrink: 0; padding: 0.9rem 1.4rem; border-top: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; }
.boats-section { border-top: 1px solid #eee; padding: 1rem 1.4rem; }
.boats-title { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: #888; margin-bottom: 0.75rem; }
.boats-loading { color: #aaa; font-size: 0.85rem; }
.boats-empty { color: #bbb; font-size: 0.88rem; }
.boat-block { background: #f8faff; border: 1px solid #e4ecff; border-radius: 8px; padding: 0.75rem 1rem; margin-bottom: 0.6rem; }
.boat-block-header { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 0.45rem; }
.boat-block-name { font-weight: 700; font-size: 0.95rem; color: #1a3a5c; }
.boat-block-dims { font-size: 0.78rem; color: #888; }
.boat-assignment { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
.assign-chip { font-size: 0.72rem; font-weight: 700; padding: 0.15rem 0.5rem; border-radius: 10px; }
.assign-chip.occupied { background: #f8d7da; color: #721c24; }
.assign-chip.free { background: #d4edda; color: #155724; }
.assign-where { font-size: 0.85rem; font-weight: 600; color: #333; }
.assign-since { font-size: 0.78rem; color: #999; margin-left: auto; }
@media (max-width: 640px) {
  .col-email, .col-address, .col-boats { display: none; }
  .page-header { flex-direction: column; align-items: stretch; }
  .header-right { flex-direction: column; align-items: stretch; }
}
.account-section { margin-top: 1.25rem; }
.account-line { display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: center; font-size: 0.88rem; }
.account-user { font-weight: 600; }
.account-roles { color: #666; font-size: 0.82rem; }
.account-status { border-radius: 4px; padding: 0.1rem 0.45rem; font-size: 0.75rem; font-weight: 700; }
.account-status.active { background: #e8f6ec; color: #1d7a3a; }
.account-status.invited { background: #fff4e0; color: #9a6400; }
.account-status.disabled { background: #f0f0f0; color: #888; }
.account-login { color: #888; font-size: 0.78rem; margin-top: 0.3rem; }

.pw-card { background: white; border-radius: 10px; width: 100%; max-width: 420px;
  display: flex; flex-direction: column; box-shadow: 0 8px 32px rgba(0,0,0,0.2); }
.pw-body { padding: 1.25rem 1.5rem; }
.pw-value { display: block; font-size: 1.15rem; font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  background: #f4f7fb; border-radius: 6px; padding: 0.75rem; text-align: center; letter-spacing: 0.04em;
  user-select: all; }
.pw-note { font-size: 0.82rem; color: #666; margin: 0.6rem 0 0; }
.pw-warning { font-size: 0.82rem; color: #9a6400; background: #fff4e0; border-radius: 6px;
  padding: 0.5rem 0.6rem; margin: 0.6rem 0 0; }
.hint { color: #666; font-size: 0.8rem; margin-top: 0.5rem; }
.roles-field { display: flex; flex-direction: column; gap: 0.3rem; }
.roles-title { font-size: 0.85rem; font-weight: 600; }
.role-check { flex-direction: row; align-items: center; gap: 0.4rem; font-weight: 400; }

@media (max-width: 400px) {
  .detail-grid { grid-template-columns: 1fr; }
  dt { margin-top: 0.4rem; }
}
</style>
