<template>
  <AppLayout>
    <div class="toolbar">
      <h2>Personer</h2>
      <div class="right">
        <div class="search-bar">
          <input class="input" v-model="search" placeholder="Sök namn eller e-post…" @input="load" />
        </div>
        <button class="btn btn-primary btn-sm" @click="openCreate">Ny person</button>
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

    <div v-if="detail" class="modal-backdrop" @click.self="detail = null">
      <div class="modal detail">
        <div class="detail-head">
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
              <span :class="['pill', accountPill(accountFor(detail.person).status)]">
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
                <span class="pill pill-ok">Tilldelad</span>
                <span class="assign-where">{{ b.assignment.dockName }} · Plats {{ b.assignment.slipNumber }}</span>
                <span class="assign-since">fr.o.m. {{ formatDate(b.assignment.assignedDate) }}</span>
              </div>
              <div v-else class="boat-assignment">
                <span class="pill pill-idle">Ingen plats</span>
              </div>
            </div>
          </div>
        </div>
        <div class="detail-footer">
          <button class="btn btn-danger btn-sm" @click="removeFromDetail">Ta bort</button>
          <button class="btn btn-secondary btn-sm" v-if="accountFor(detail.person)" @click="resetFor(detail.person)">Nytt lösenord</button>
          <button class="btn btn-secondary btn-sm" v-else @click="openCreateAccount(detail.person)">Skapa konto</button>
          <button class="btn btn-primary btn-sm" @click="openEdit(detail.person); detail = null">Redigera</button>
        </div>
      </div>
    </div>

    <BaseModal v-if="accountModal" title="Skapa konto" saveLabel="Skapa"
               @close="accountModal = false" @save="createAccount">
      <div class="form-grid">
        <label class="full">E-post
          <input class="input" :value="accountForm.email" readonly />
        </label>
        <div class="full roles-field">
          <span class="roles-title">Roller *</span>
          <label v-for="r in roles" :key="r" class="role-check">
            <input class="input" type="checkbox" :value="r" v-model="accountForm.roles" />{{ r }}
          </label>
        </div>
      </div>
      <p class="hint">Kontot får ett lösenord som visas en gång.</p>
      <p v-if="accountErr" class="error">{{ accountErr }}</p>
    </BaseModal>

    <div v-if="password" class="modal-backdrop" @click.self="password = null">
      <div class="modal pw-card">
        <div class="detail-head">
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
          <button class="btn btn-primary btn-sm" @click="password = null">Klar</button>
        </div>
      </div>
    </div>

    <BaseModal v-if="modal" :title="editing ? 'Redigera person' : 'Ny person'" @close="modal = false" @save="save">
      <div class="form-grid">
        <label>Förnamn *<input class="input" v-model="form.firstName" required /></label>
        <label>Efternamn *<input class="input" v-model="form.lastName" required /></label>
        <label class="full">E-post *<input class="input" v-model="form.email" type="email" required /></label>
        <label>Telefon<input class="input" v-model="form.phone" /></label>
        <label>Postnummer<input class="input" v-model="form.postalCode" /></label>
        <label>Adress<input class="input" v-model="form.address" placeholder="Gatuadress" /></label>
        <label>Fastighetsbeteckning<input class="input" v-model="form.propertyDesignation" placeholder="t.ex. Örby 1:23" /></label>
        <label class="full">Noteringar<textarea class="input" v-model="form.notes" rows="3" placeholder="Fri text…" /></label>
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

/** Account state in the guide's roles: live is green, invited waits, disabled is dormant. */
function accountPill(status) {
  return { ACTIVE: 'pill-ok', INVITED: 'pill-warn', DISABLED: 'pill-idle' }[status] ?? 'pill-idle'
}
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
.search-bar .input { width: min(280px, 100%); }
.clickable-row { cursor: pointer; }
.empty { display: block; text-align: center; padding: 44px; }
.address-cell { max-width: 220px; font-size: 0.88rem; }

.detail { max-width: 480px; height: min(85vh, 720px); display: flex; flex-direction: column; padding: 0; }
.detail-head {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 22px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--paper-border);
}
.detail-head h3 { margin: 0; }
.close-btn { background: none; border: 0; color: var(--text-muted); cursor: pointer; font-size: 1rem; padding: 2px 6px; }
.close-btn:hover { color: var(--ink); }
.detail-scroll { flex: 1; overflow-y: auto; min-height: 0; }
.detail-grid { display: grid; grid-template-columns: 150px 1fr; gap: 10px 22px; padding: 18px 22px; margin: 0; }
dt {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  align-self: start;
  padding-top: 2px;
}
dd { margin: 0; font-size: 0.9rem; color: var(--text); }
.notes-text { white-space: pre-wrap; color: var(--text-muted); }
.detail-footer {
  flex-shrink: 0;
  padding: 14px 22px;
  border-top: 1px solid var(--paper-border);
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
}
/* Removing a person is the one destructive action here, so it keeps its distance. */
.detail-footer .btn-danger { margin-right: auto; }

.boats-section { border-top: 1px solid var(--paper-border); padding: 18px 22px; }
.boats-title {
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-bottom: 10px;
}
.boats-loading, .boats-empty { color: var(--text-muted); font-size: 0.9rem; }
.boat-block {
  border: 1px solid var(--paper-border);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  margin-bottom: 10px;
}
.boat-block-header { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.boat-block-name { font-weight: 600; color: var(--ink); }
.boat-block-dims { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-muted); }
.boat-assignment { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.assign-where { font-size: 0.9rem; font-weight: 500; color: var(--ink); }
.assign-since { font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-muted); margin-left: auto; }

/* Same block as the boat list below it: a hairline above, the card's own padding. */
.account-section { border-top: 1px solid var(--paper-border); padding: 18px 22px; }
.account-line { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; font-size: 0.9rem; }
.account-user { font-weight: 600; color: var(--ink); }
.account-roles, .account-login { font-size: 0.82rem; color: var(--text-muted); }
.account-login { margin-top: 4px; }

/* A one-time password is a code: mono, roomy, and selected in one click. */
.pw-card { max-width: 420px; padding: 0; }
.pw-body { padding: 18px 22px; }
.pw-value {
  display: block;
  font-family: var(--font-mono);
  font-size: 1.15rem;
  letter-spacing: 0.04em;
  background: var(--paper-deep);
  border-radius: var(--radius-sm);
  padding: 14px;
  text-align: center;
  user-select: all;
}
.pw-note { font-size: 0.85rem; color: var(--text-muted); margin: 10px 0 0; }
.pw-warning {
  font-size: 0.85rem;
  color: var(--warn-text);
  background: var(--warn-bg);
  border: 1px solid var(--warn-border);
  border-radius: var(--radius-sm);
  padding: 8px 10px;
  margin: 10px 0 0;
}

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 14px; }
.form-grid .full { grid-column: 1 / -1; }
.form-grid label, .roles-title {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 500;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.form-grid .input { font-family: var(--font); text-transform: none; letter-spacing: 0; }
.form-grid textarea.input { resize: vertical; }
.roles-field { display: flex; flex-direction: column; gap: 6px; }
.role-check {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  font-family: var(--font);
  font-size: 0.92rem;
  letter-spacing: 0;
  text-transform: none;
  color: var(--text);
}

@media (max-width: 640px) {
  .col-email, .col-address, .col-boats { display: none; }
  .toolbar, .toolbar .right { flex-direction: column; align-items: stretch; }
  .form-grid { grid-template-columns: 1fr; }
}
@media (max-width: 400px) {
  .detail-grid { grid-template-columns: 1fr; }
  dt { margin-top: 6px; }
}
</style>
