<template>
    <div class="card" style="padding:16px;">
        <div class="title-lg">Vendors</div>
        <div class="subtitle" style="margin-top:8px;">
            Manage suppliers and their offers (equipment type ↔ price).
        </div>

        <!-- Filters -->
        <div class="ui-toolbar" style="margin-top:12px; display:flex; flex-wrap:wrap; gap:12px;">
            <input class="input" v-model="filters.keyword" placeholder="Search by supplier / type / contact"
                   style="min-width:220px;"/>

            <div style="min-width:220px;">
                <label>Supplier</label>
                <MultiSelect
                    v-model="filters.supplierIds"
                    :options="supplierOptions"
                    placeholder="All suppliers"/>
            </div>

            <div style="min-width:220px;">
                <label>Equipment Type</label>
                <MultiSelect
                    v-model="filters.typeIds"
                    :options="typeOptions"
                    placeholder="All types"/>
            </div>

            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:8px; min-width:260px;">
                <div>
                    <label>Min Price</label>
                    <input class="input" type="number" min="0" step="1" v-model.number="filters.minPrice"
                           placeholder="0"/>
                </div>
                <div>
                    <label>Max Price</label>
                    <input class="input" type="number" min="0" step="1" v-model.number="filters.maxPrice"
                           placeholder="Any"/>
                </div>
            </div>

            <div style="display:flex; gap:8px; align-items:end;">
                <button class="btn" @click="resetFilters">Reset</button>
                <button class="btn" @click="exportCsv">Export CSV</button>
                <button class="btn btn-primary" @click="openNewOffer">Add Offer</button>
                <button class="btn" @click="openNewSupplier">Add Supplier</button>
            </div>
        </div>

        <!-- Table -->
        <div class="table-wrapper" style="margin-top:16px; overflow:auto;">
            <table class="ui-table" style="table-layout:auto; width:100%;">
                <thead>
                <tr>
                    <th>Supplier</th>
                    <th>Contact</th>
                    <th>Equipment Type</th>
                    <th>Price</th>
                    <th style="width:220px;">Actions</th>
                </tr>
                </thead>
                <tbody>
                <tr v-if="loading">
                    <td colspan="5">
                        <TableSkeleton :rows="6"/>
                    </td>
                </tr>
                <tr v-else-if="filteredRows.length===0">
                    <td colspan="5">
                        <EmptyState title="No vendors" hint="Try filters or add an offer/supplier."/>
                    </td>
                </tr>
                <tr v-else v-for="r in filteredRows" :key="r.key">
                    <td>{{ r.supplierName }}</td>
                    <td>{{ r.contact || '-' }}</td>
                    <td>{{ r.typeName }}</td>
                    <td>{{ money(r.price) }}</td>
                    <td style="white-space:nowrap;">
                        <button class="btn btn-blue" @click="openEditOffer(r)">Edit Offer</button>
                        <button class="btn btn-red" style="margin-left:8px;" @click="removeOffer(r)">Delete Offer
                        </button>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>

        <!-- Add/Edit Offer Modal -->
        <div v-if="offerModal.open" class="modal-backdrop">
            <div class="modal card">
                <div class="title-lg">{{ offerModal.mode === 'create' ? 'Add Offer' : 'Edit Offer' }}</div>
                <div class="form-grid">
                    <div>
                        <label>Supplier</label>
                        <select class="input" v-model="offerModal.form.supplierId">
                            <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
                        </select>
                    </div>
                    <div>
                        <label>Equipment Type</label>
                        <select class="input" v-model="offerModal.form.equipmentTypeId">
                            <option v-for="t in types" :key="t.id" :value="t.id">{{ t.name }}</option>
                        </select>
                    </div>
                    <div>
                        <label>Price</label>
                        <input class="input" type="number" min="0" step="1" v-model.number="offerModal.form.price"/>
                    </div>
                </div>
                <div style="display:flex; gap:8px; justify-content:flex-end; margin-top:16px;">
                    <button class="btn" @click="closeOffer">Cancel</button>
                    <button class="btn btn-primary" @click="saveOffer">Save</button>
                </div>
            </div>
        </div>

        <!-- Add Supplier Modal -->
        <div v-if="supplierModal.open" class="modal-backdrop">
            <div class="modal card">
                <div class="title-lg">Add Supplier</div>
                <div class="form-grid">
                    <div>
                        <label>Supplier Name</label>
                        <input class="input" v-model="supplierModal.form.name" placeholder="Supplier name"/>
                    </div>
                    <div>
                        <label>Contact</label>
                        <input class="input" v-model="supplierModal.form.contact" placeholder="Contact person / phone"/>
                    </div>
                </div>
                <div style="display:flex; gap:8px; justify-content:flex-end; margin-top:16px;">
                    <button class="btn" @click="closeNewSupplier">Cancel</button>
                    <button class="btn btn-primary" @click="saveNewSupplier">Save</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted} from 'vue'
import EmptyState from '@/components/admin/EmptyState.vue'
import TableSkeleton from '@/components/admin/TableSkeleton.vue'
import MultiSelect from '@/components/MultiSelect.vue'

/** APIs:
 *  GET  /req/proc/vendors
 *  POST /req/proc/vendor
 *  GET  /req/proc/equipmentTypes
 *  GET  /req/proc/quotes
 *  POST /req/proc/quote
 *  PUT  /req/proc/quote
 *  DELETE /req/proc/quote?supplierId=&equipmentTypeId=
 */

const loading = ref(true)
const suppliers = ref([]) // [{id,name,contact}]
const types = ref([])     // [{id,name}]
const quotes = ref([])    // [{supplierId,typeId,price}]

/* ---------- Filters ---------- */
const filters = reactive({
    keyword: '',
    supplierIds: [],
    typeIds: [],
    minPrice: '',
    maxPrice: ''
})

const supplierOptions = computed(() => suppliers.value.map(s => ({value: String(s.id), label: s.name})))
const typeOptions = computed(() => types.value.map(t => ({value: String(t.id), label: t.name})))

function resetFilters() {
    filters.keyword = ''
    filters.supplierIds = []
    filters.typeIds = []
    filters.minPrice = ''
    filters.maxPrice = ''
}

/* ---------- Helpers ---------- */
function money(n) {
    try {
        return Number(n || 0).toLocaleString(undefined, {style: 'currency', currency: 'USD'})
    } catch {
        return String(n)
    }
}

const sName = id => suppliers.value.find(s => String(s.id) === String(id))?.name || id
const sContact = id => suppliers.value.find(s => String(s.id) === String(id))?.contact || ''
const tName = id => types.value.find(t => String(t.id) === String(id))?.name || id

/* 扁平数据（全部） */
const allRows = computed(() =>
    quotes.value.map(q => ({
        key: `${q.supplierId}-${q.typeId}`,
        supplierId: String(q.supplierId),
        supplierName: sName(q.supplierId),
        contact: sContact(q.supplierId),
        equipmentTypeId: String(q.typeId),
        typeName: tName(q.typeId),
        price: Number(q.price || 0)
    }))
)

/* 过滤后的数据 */
const filteredRows = computed(() => {
    const kw = (filters.keyword || '').toLowerCase().trim()
    const min = filters.minPrice === '' || filters.minPrice === null ? -Infinity : Number(filters.minPrice)
    const max = filters.maxPrice === '' || filters.maxPrice === null ? Infinity : Number(filters.maxPrice)

    return allRows.value.filter(r => {
        const byKw = !kw || [
            r.supplierName,
            r.typeName,
            r.contact
        ].some(x => (x || '').toLowerCase().includes(kw))

        const bySupplier = !filters.supplierIds.length || filters.supplierIds.includes(String(r.supplierId))
        const byType = !filters.typeIds.length || filters.typeIds.includes(String(r.equipmentTypeId))
        const byPrice = (Number.isFinite(min) ? r.price >= min : true) &&
            (Number.isFinite(max) ? r.price <= max : true)

        return byKw && bySupplier && byType && byPrice
    })
})

/* ---------- Export ---------- */
function exportCsv() {
    const rows = [
        ['Supplier', 'Contact', 'Equipment Type', 'Price'],
        ...filteredRows.value.map(r => [r.supplierName, r.contact, r.typeName, r.price])
    ]
    const csv = rows.map(r => r.map(x => `"${String(x ?? '').replaceAll('"', '""')}"`).join(',')).join('\n')
    const blob = new Blob([csv], {type: 'text/csv;charset=utf-8;'})
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'vendor-offers.csv'
    a.click()
    URL.revokeObjectURL(url)
}

/* ---------- API ---------- */
async function loadSuppliers() {
    const r = await fetch('/req/proc/vendors');
    const j = await r.json()
    suppliers.value = j.code === '000'
        ? (j.data || []).map(x => ({
            id: String(x.supplier_id || x.id),
            name: x.supplier_name || x.name || '-',
            contact: x.contact || ''
        }))
        : []
}

async function loadTypes() {
    const r = await fetch('/req/proc/equipmentTypes');
    const j = await r.json()
    types.value = j.code === '000'
        ? (j.data || []).map(x => ({
            id: String(x.equipment_type_id || x.id),
            name: x.equipment_type_name || x.name || '-'
        }))
        : []
}

async function loadQuotes() {
    const r = await fetch('/req/proc/quotes');
    const j = await r.json()
    quotes.value = j.code === '000'
        ? (j.data || []).map(x => ({
            supplierId: String(x.supplier_id || x.supplierId),
            typeId: String(x.equipment_type_id || x.equipmentTypeId),
            price: Number(x.price || 0)
        }))
        : []
}

/* ---------- Offer modal ---------- */
const offerModal = reactive({
    open: false,
    mode: 'create',
    form: {supplierId: '', equipmentTypeId: '', price: 0}
})

function openNewOffer() {
    offerModal.open = true
    offerModal.mode = 'create'
    offerModal.form = {
        supplierId: suppliers.value[0]?.id || '',
        equipmentTypeId: types.value[0]?.id || '',
        price: 0
    }
}

function openEditOffer(row) {
    offerModal.open = true
    offerModal.mode = 'edit'
    offerModal.form = {
        supplierId: row.supplierId,
        equipmentTypeId: row.equipmentTypeId,
        price: row.price
    }
}

function closeOffer() {
    offerModal.open = false
}

async function saveOffer() {
    const p = {...offerModal.form}
    if (!p.supplierId || !p.equipmentTypeId) return alert('Supplier & Equipment Type are required')
    const method = offerModal.mode === 'create' ? 'POST' : 'PUT'
    const r = await fetch('/req/proc/quote', {
        method,
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({supplier_id: p.supplierId, equipment_type_id: p.equipmentTypeId, price: p.price})
    })
    const j = await r.json().catch(() => ({code: 'ERR'}))
    if (j.code !== '000') return alert(j.message || 'Failed to save offer')
    await loadQuotes()
    closeOffer()
}

async function removeOffer(row) {
    if (!confirm(`Delete offer: ${row.supplierName} - ${row.typeName}?`)) return
    const r = await fetch(`/req/proc/quote?supplierId=${encodeURIComponent(row.supplierId)}&equipmentTypeId=${encodeURIComponent(row.equipmentTypeId)}`, {method: 'DELETE'})
    const j = await r.json().catch(() => ({code: 'ERR'}))
    if (j.code !== '000') return alert(j.message || 'Failed to delete offer')
    await loadQuotes()
}

/* ---------- Supplier modal ---------- */
const supplierModal = reactive({
    open: false,
    form: {name: '', contact: ''}
})

function openNewSupplier() {
    supplierModal.open = true;
    supplierModal.form = {name: '', contact: ''}
}

function closeNewSupplier() {
    supplierModal.open = false
}

async function saveNewSupplier() {
    const p = supplierModal.form
    if (!p.name || !p.name.trim()) return alert('Supplier name is required')
    const r = await fetch('/req/proc/vendor', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({supplier_name: p.name.trim(), contact: p.contact || ''})
    })
    const j = await r.json().catch(() => ({code: 'ERR'}))
    if (j.code !== '000') return alert(j.message || 'Failed to add supplier')
    await loadSuppliers()
    closeNewSupplier()
}

/* init */
onMounted(async () => {
    loading.value = true
    try {
        await Promise.all([loadSuppliers(), loadTypes(), loadQuotes()])
    } finally {
        loading.value = false
    }
})
</script>

<style scoped>
.ui-table {
    width: 100%;
    border-collapse: collapse;
}

.ui-table th, .ui-table td {
    padding: 10px 12px;
    border-bottom: 1px solid #e5e7eb;
    text-align: left;
    white-space: normal;
    word-break: break-word;
}

.ui-table th {
    background: #f9fafb;
    font-weight: 700;
}

.modal-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.35);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
}

.modal {
    width: min(720px, 100%);
    padding: 16px;
}

.form-grid {
    margin-top: 16px;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 12px;
}
</style>
