<template>
    <div class="card" style="padding:16px;">
        <div class="title-lg">Vendors</div>
        <div class="subtitle" style="margin-top:8px;">
            Manage suppliers and their offers (equipment type ↔ price).
        </div>

        <!-- Filters -->
        <div class="ui-toolbar" style="margin-top:12px; display:flex; flex-wrap:wrap; gap:12px;">
            <input
                class="input"
                v-model="filters.keyword"
                placeholder="Search by supplier / type / contact"
                style="min-width:220px;"
            />

            <div style="min-width:220px;">
                <label>Supplier</label>
                <MultiSelect
                    v-model="filters.supplierIds"
                    :options="supplierOptions"
                    placeholder="All suppliers"
                />
            </div>

            <div style="min-width:220px;">
                <label>Equipment Type</label>
                <MultiSelect
                    v-model="filters.typeIds"
                    :options="typeOptions"
                    placeholder="All types"
                />
            </div>

            <div style="display:grid; grid-template-columns: 1fr 1fr; gap:8px; min-width:260px;">
                <div>
                    <label>Min Price</label>
                    <input
                        class="input"
                        type="number"
                        min="0"
                        step="1"
                        v-model.number="filters.minPrice"
                        placeholder="0"
                    />
                </div>
                <div>
                    <label>Max Price</label>
                    <input
                        class="input"
                        type="number"
                        min="0"
                        step="1"
                        v-model.number="filters.maxPrice"
                        placeholder="Any"
                    />
                </div>
            </div>

            <div style="display:flex; gap:8px; align-items:end;">
                <button class="btn" @click="resetFilters">Reset</button>
                <button class="btn" @click="exportCsv">Export CSV</button>
                <!-- 仅保留 Add Supplier -->
                <button class="btn btn-primary" @click="openNewSupplier">Add</button>
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
                        <EmptyState title="No vendors" hint="Try filters or add a supplier with initial offers."/>
                    </td>
                </tr>
                <tr v-else v-for="r in filteredRows" :key="r.key">
                    <td>{{ r.supplierName }}</td>
                    <td>{{ r.contact || '-' }}</td>
                    <td>{{ r.typeName }}</td>
                    <td>{{ money(r.price) }}</td>
                    <td style="white-space:nowrap;">
                        <button class="btn btn-blue" @click="openEditOffer(r)">Edit</button>
                        <button class="btn btn-red" style="margin-left:8px;" @click="removeOffer(r)">Delete
                        </button>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>

        <!-- Edit Offer Modal (仅用于编辑已有报价) -->
        <div v-if="offerModal.open" class="modal-backdrop">
            <div class="modal card">
                <div class="title-lg">Edit</div>
                <div class="form-grid">
                    <div>
                        <label>Supplier</label>
                        <select class="input" v-model="offerModal.form.supplierId" disabled>
                            <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
                        </select>
                    </div>
                    <div>
                        <label>Equipment Type</label>
                        <select class="input" v-model="offerModal.form.equipmentTypeId" disabled>
                            <option v-for="t in types" :key="t.id" :value="t.id">{{ t.name }}</option>
                        </select>
                    </div>
                    <div>
                        <label>Price</label>
                        <input class="input" type="number" min="0" step="1" v-model.number="offerModal.form.price"/>
                    </div>
                </div>
                <div class="footer-actions">
                    <button class="btn" @click="closeOffer">Cancel</button>
                    <button class="btn btn-primary" @click="saveOffer">Save</button>
                </div>
            </div>
        </div>

        <!-- Add Supplier Modal (精简 & 对齐) -->
        <div v-if="supplierModal.open" class="modal-backdrop">
            <div class="modal card">
                <div class="title-lg">Add Offer</div>

                <!-- 基本信息（必填） -->
                <div class="form-grid compact">
                    <div>
                        <label>Supplier Name <span class="req">*</span></label>
                        <input class="input" v-model.trim="supplierModal.form.name" placeholder="Supplier name"/>
                    </div>
                    <div>
                        <label>Contact <span class="req">*</span></label>
                        <input class="input" v-model.trim="supplierModal.form.contact" placeholder="Contact person"/>
                    </div>
                </div>

                <!-- 初始报价行 -->
                <div class="title-sm" style="font-weight:700; margin:12px 0 8px;">Initial Offers</div>
                <div class="offer-grid header">
                    <div>Equipment Type</div>
                    <div>Price</div>
                    <div>Note</div>
                    <div></div>
                </div>
                <div v-for="(row, idx) in supplierModal.offerRows" :key="idx" class="offer-grid">
                    <div>
                        <select class="input" v-model="row.equipmentTypeId">
                            <option value="" disabled>Select type</option>
                            <option v-for="t in types" :key="t.id" :value="t.id">{{ t.name }}</option>
                        </select>
                    </div>
                    <div>
                        <input class="input" type="number" min="0" step="1" v-model.number="row.price" placeholder="0"/>
                    </div>
                    <div>
                        <input class="input" v-model.trim="row.note" placeholder="Optional"/>
                    </div>
                    <div class="right">
                        <button class="btn" @click="removeOfferRow(idx)">Remove</button>
                    </div>
                </div>
                <div class="row-end">
                    <button class="btn" @click="addOfferRow">Add Offer Row</button>
                </div>

                <!-- 底部操作 -->
                <div class="footer-actions">
                    <button class="btn" @click="closeNewSupplier">Cancel</button>
                    <button class="btn btn-primary" @click="saveNewSupplier" :disabled="savingSupplier">
                        {{ savingSupplier ? 'Saving...' : 'Save' }}
                    </button>
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
        const byKw =
            !kw ||
            [r.supplierName, r.typeName, r.contact].some(x => (x || '').toLowerCase().includes(kw))

        const bySupplier = !filters.supplierIds.length || filters.supplierIds.includes(String(r.supplierId))
        const byType = !filters.typeIds.length || filters.typeIds.includes(String(r.equipmentTypeId))
        const byPrice =
            (Number.isFinite(min) ? r.price >= min : true) &&
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
    const r = await fetch('/req/proc/vendors')
    const j = await r.json()
    suppliers.value =
        j.code === '000'
            ? (j.data || []).map(x => ({
                id: String(x.supplier_id || x.id),
                name: x.supplier_name || x.name || '-',
                contact: x.contact || ''
            }))
            : []
}

async function loadTypes() {
    const r = await fetch('/req/proc/equipmentTypes')
    const j = await r.json()
    types.value =
        j.code === '000'
            ? (j.data || []).map(x => ({
                id: String(x.equipment_type_id || x.id),
                name: x.equipment_type_name || x.name || '-'
            }))
            : []
}

async function loadQuotes() {
    const r = await fetch('/req/proc/quotes')
    const j = await r.json()
    quotes.value =
        j.code === '000'
            ? (j.data || []).map(x => ({
                supplierId: String(x.supplier_id || x.supplierId),
                typeId: String(x.equipment_type_id || x.equipmentTypeId),
                price: Number(x.price || 0)
            }))
            : []
}

/* ---------- Offer edit modal ---------- */
const offerModal = reactive({
    open: false,
    form: {supplierId: '', equipmentTypeId: '', price: 0}
})

function openEditOffer(row) {
    offerModal.open = true
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
    const r = await fetch('/req/proc/quote', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            supplier_id: p.supplierId,
            equipment_type_id: p.equipmentTypeId,
            price: Number(p.price || 0)
        })
    })
    const j = await r.json().catch(() => ({code: 'ERR'}))
    if (j.code !== '000') return alert(j.message || 'Failed to save offer')
    await loadQuotes()
    closeOffer()
}

async function removeOffer(row) {
    if (!confirm(`Delete offer: ${row.supplierName} - ${row.typeName}?`)) return
    const r = await fetch(
        `/req/proc/quote?supplierId=${encodeURIComponent(row.supplierId)}&equipmentTypeId=${encodeURIComponent(
            row.equipmentTypeId
        )}`,
        {method: 'DELETE'}
    )
    const j = await r.json().catch(() => ({code: 'ERR'}))
    if (j.code !== '000') return alert(j.message || 'Failed to delete offer')
    await loadQuotes()
}

/* ---------- Supplier modal (合并后唯一入口) ---------- */
const savingSupplier = ref(false)
const supplierModal = reactive({
    open: false,
    form: {name: '', contact: ''},
    offerRows: [{equipmentTypeId: '', price: 0, note: ''}]
})

function addOfferRow() {
    supplierModal.offerRows.push({equipmentTypeId: '', price: 0, note: ''})
}

function removeOfferRow(idx) {
    supplierModal.offerRows.splice(idx, 1)
}

function openNewSupplier() {
    supplierModal.open = true
    supplierModal.form = {name: '', contact: ''}
    supplierModal.offerRows = [{equipmentTypeId: '', price: 0, note: ''}]
}

function closeNewSupplier() {
    supplierModal.open = false
}

async function saveNewSupplier() {
    if (!supplierModal.form.name.trim()) return alert('Supplier name is required')
    if (!supplierModal.form.contact.trim()) return alert('Contact is required')

    savingSupplier.value = true
    try {
        // 1) 新建 supplier
        const r = await fetch('/req/proc/vendor', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                supplier_name: supplierModal.form.name.trim(),
                contact: supplierModal.form.contact.trim()
            })
        })
        const j = await r.json().catch(() => ({code: 'ERR'}))
        if (j.code !== '000') return alert(j.message || 'Failed to add supplier')

        // 获取新 supplier_id
        let newId = String(j?.data?.supplier_id || '')
        if (!newId) {
            await loadSuppliers()
            const found = suppliers.value.find(s => s.name === supplierModal.form.name.trim())
            if (found) newId = String(found.id)
        }
        if (!newId) return alert('Supplier created but ID not returned.')

        // 2) 批量新增报价（过滤未填完整的行）
        const rows = supplierModal.offerRows
            .filter(r => r.equipmentTypeId && Number.isFinite(Number(r.price)) && Number(r.price) >= 0)
            .map(r => ({
                supplier_id: newId,
                equipment_type_id: String(r.equipmentTypeId),
                price: Number(r.price)
            }))

        for (const payload of rows) {
            const rr = await fetch('/req/proc/quote', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            })
            const jj = await rr.json().catch(() => ({code: 'ERR'}))
            if (jj.code !== '000') throw new Error(jj.message || 'Failed to add offer')
        }

        await Promise.all([loadSuppliers(), loadQuotes()])
        closeNewSupplier()
    } catch (err) {
        console.error(err)
        alert(err?.message || 'Save failed')
    } finally {
        savingSupplier.value = false
    }
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

.ui-table th,
.ui-table td {
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

/* 更紧凑的基础信息两列栅格 */
.form-grid {
    margin-top: 16px;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 12px;
}

.form-grid.compact {
    margin-top: 12px;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 12px;
}

.req {
    color: #dc2626;
}

/* 报价行表格式布局 */
.offer-grid {
    display: grid;
    grid-template-columns: 1.2fr 0.8fr 1.2fr auto;
    gap: 8px;
    align-items: center;
    margin-bottom: 8px;
}

.offer-grid.header {
    color: #6b7280;
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 6px;
}

.row-end {
    display: flex;
    justify-content: flex-end;
    margin-top: 4px;
}

/* 底部操作按钮区域右对齐 */
.footer-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 16px;
}

.right {
    text-align: right;
}
</style>
