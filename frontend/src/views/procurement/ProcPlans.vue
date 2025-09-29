<template>
    <div class="card" style="padding:16px;">
        <div class="title-lg">Purchase Requests</div>
        <div class="subtitle" style="margin-top:8px;">
            Review requests (orders in <code>under-review</code>) and assign vendors to create purchase orders.
        </div>

        <!-- Filters + New Plan（保留你现有筛选与新增计划） -->
        <div class="ui-toolbar" style="margin-top:12px; display:flex; flex-wrap:wrap; gap:12px; align-items:end;">
            <input class="input" v-model="filters.keyword" placeholder="Search by ID / type / supplier"
                   style="min-width:220px;"/>

            <div style="min-width:220px;">
                <label>Supplier</label>
                <MultiSelect v-model="filters.vendorIds" :options="supplierOptions" placeholder="All suppliers"/>
            </div>

            <div style="min-width:220px;">
                <label>Equipment Type</label>
                <MultiSelect v-model="filters.typeIds" :options="typeOptions" placeholder="All types"/>
            </div>

            <div style="min-width:220px;">
                <label>Status</label>
                <MultiSelect v-model="filters.statuses" :options="statusOptions" placeholder="All status"/>
            </div>

            <div style="display:flex; gap:8px; align-items:end;">
                <input class="input" type="number" v-model.number="filters.qtyMin" placeholder="Qty ≥"
                       style="width:120px;"/>
                <input class="input" type="number" v-model.number="filters.qtyMax" placeholder="Qty ≤"
                       style="width:120px;"/>
            </div>

            <div style="display:flex; gap:8px; align-items:end;">
                <input class="input" type="number" v-model.number="filters.amountMin" placeholder="Amount ≥ $"
                       style="width:140px;"/>
                <input class="input" type="number" v-model.number="filters.amountMax" placeholder="Amount ≤ $"
                       style="width:140px;"/>
            </div>

            <div style="display:flex; gap:8px; align-items:end; margin-left:auto;">
                <button class="btn" @click="resetFilters">Reset</button>
                <button class="btn btn-primary" @click="openNewPlan">Add</button>
            </div>
        </div>

        <!-- Table -->
        <div class="table-wrapper" style="margin-top:16px; overflow:auto;">
            <table class="ui-table" style="table-layout:auto; width:100%;">
                <thead>
                <tr>
                    <th style="min-width:100px;">Req ID</th>
                    <th>Equipment Type</th>
                    <th>Qty</th>
                    <th style="min-width:220px;">Supplier</th>
                    <th>Unit Price</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th style="width:220px;">Actions</th>
                </tr>
                </thead>
                <tbody>
                <tr v-if="loading">
                    <td colspan="8">
                        <TableSkeleton :rows="6"/>
                    </td>
                </tr>
                <tr v-else-if="filtered.length===0">
                    <td colspan="8" style="text-align:center; color:var(--color-muted);">No requests</td>
                </tr>
                <tr v-else v-for="p in filtered" :key="p.procureId">
                    <td>#{{ p.procureId }}</td>
                    <td>{{ typeName(p.equipmentTypeId) }}</td>
                    <td>{{ p.count }}</td>

                    <!-- 修改点：供应商选择（仅显示该类型有报价的供应商） -->
                    <td>
                        <select
                            class="input"
                            v-model="p.supplierId"
                            @change="onSupplierChange(p)"
                        >
                            <option value="0000">— Select —</option>
                            <option
                                v-for="v in suppliersForType(p.equipmentTypeId)"
                                :key="v.id"
                                :value="v.id"
                            >
                                {{ v.name }}
                            </option>
                        </select>
                    </td>

                    <td>{{ money(unitPriceOf(p.supplierId, p.equipmentTypeId)) }}</td>
                    <td>{{ money(fallbackAmount(p)) }}</td>
                    <td>{{ p.status }}</td>

                    <td style="white-space:nowrap;">
                        <button class="btn btn-red" @click="reject(p)">Reject</button>
                        <button class="btn btn-green" style="margin-left:8px;" @click="openAssign(p)">Assign Vendor
                        </button>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>

        <!-- Assign vendor / create PO（保留） -->
        <div v-if="modal.open" class="modal-backdrop">
            <div class="modal card">
                <div class="title-lg">Assign Vendor / Create PO</div>
                <div class="form-grid">
                    <div>
                        <label>Request ID</label>
                        <input class="input" :value="'#'+modal.req?.procureId" disabled/>
                    </div>
                    <div>
                        <label>Equipment Type</label>
                        <input class="input" :value="typeName(modal.req?.equipmentTypeId)" disabled/>
                    </div>
                    <div>
                        <label>Supplier</label>
                        <select class="input" v-model="modal.form.supplierId">
                            <option value="0000">— Select —</option>
                            <option v-for="v in suppliersForType(modal.req?.equipmentTypeId)" :key="v.id" :value="v.id">
                                {{ v.name }}
                            </option>
                        </select>
                    </div>
                    <div>
                        <label>Quantity</label>
                        <input class="input" type="number" min="1" v-model.number="modal.form.count"/>
                    </div>
                    <div>
                        <label>Unit Price</label>
                        <input class="input"
                               :value="money(unitPriceOf(modal.form.supplierId, modal.req?.equipmentTypeId))" disabled/>
                    </div>
                    <div>
                        <label>Amount</label>
                        <input class="input"
                               :value="money(unitPriceOf(modal.form.supplierId, modal.req?.equipmentTypeId) * (modal.form.count||0))"
                               disabled/>
                    </div>
                </div>
                <div style="display:flex; gap:8px; justify-content:flex-end; margin-top:16px;">
                    <button class="btn" @click="closeAssign">Cancel</button>
                    <button class="btn btn-primary" @click="createPO">Create</button>
                </div>
            </div>
        </div>

        <!-- New Plan（保留） -->
        <div v-if="newPlan.open" class="modal-backdrop">
            <div class="modal card">
                <div class="title-lg">New Purchase Plan</div>
                <div class="form-grid">
                    <div style="grid-column: 1 / -1;">
                        <label>Device Name</label>
                        <input class="input" v-model="newPlan.form.deviceName" placeholder="e.g. ECG Monitor"/>
                    </div>
                    <div style="grid-column: 1 / -1;">
                        <label>Specification</label>
                        <input class="input" v-model="newPlan.form.spec" placeholder="Key specs / model / accessories"/>
                    </div>
                    <div>
                        <label>Equipment Type (dict)</label>
                        <select class="input" v-model="newPlan.form.typeId">
                            <option value="">— Optional —</option>
                            <option v-for="t in types" :key="t.id" :value="t.id">{{ t.name }}</option>
                        </select>
                    </div>
                    <div>
                        <label>Quantity</label>
                        <input class="input" type="number" min="1" v-model.number="newPlan.form.count"/>
                    </div>
                    <div>
                        <label>Budget (USD)</label>
                        <input class="input" type="number" min="0" step="1" v-model.number="newPlan.form.budget"/>
                    </div>
                    <div>
                        <label>Preferred Supplier</label>
                        <select class="input" v-model="newPlan.form.supplierId">
                            <option value="0000">— None —</option>
                            <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
                        </select>
                    </div>
                    <div style="grid-column: 1 / -1;">
                        <label>Remarks</label>
                        <textarea class="input" rows="3" v-model="newPlan.form.remark"
                                  placeholder="Optional notes, delivery expectation, etc."></textarea>
                    </div>
                </div>
                <div style="display:flex; gap:8px; justify-content:flex-end; margin-top:16px;">
                    <button class="btn" @click="closeNewPlan">Cancel</button>
                    <button class="btn btn-primary" @click="submitNewPlan">Submit</button>
                </div>
            </div>
        </div>

    </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted} from 'vue'
import MultiSelect from '@/components/MultiSelect.vue'
import TableSkeleton from '@/components/admin/TableSkeleton.vue'

/** 新增后的交互说明
 * - 行内 Supplier 下拉：仅显示对该 equipment_type_id 有报价的供应商（由 /req/proc/quotes 决定）。
 * - 选择后：更新 p.supplierId -> 重新计算单价与金额 -> 调用 PUT /req/proc/order/assign 持久化到 DB（更新 tb_procure_order.supplier_id）。
 * - 若需要独立接口，也可在后端加：PUT /req/proc/order/vendor {procure_id, supplier_id}。
 */

const loading = ref(true)

const suppliers = ref([])  // [{id,name}]
const types = ref([])      // [{id,name}]
const quotes = ref([])     // [{supplierId,typeId,price}]
const requests = ref([])   // 全量订单（默认看 under-review）

/* ---------- helpers ---------- */
function money(n) {
    try {
        return Number(n || 0).toLocaleString(undefined, {style: 'currency', currency: 'USD'})
    } catch {
        return String(n)
    }
}

function supplierName(id) {
    const v = suppliers.value.find(v => String(v.id) === String(id));
    return v ? v.name : (id === '0000' ? '-' : id)
}

function typeName(id) {
    const t = types.value.find(t => String(t.id) === String(id));
    return t ? t.name : id
}

function unitPriceOf(supplierId, typeId) {
    const q = quotes.value.find(q => q.supplierId === String(supplierId) && q.typeId === String(typeId))
    return q ? Number(q.price || 0) : 0
}

/* 若无单价则用预算兜底 */
function fallbackAmount(p) {
    const up = unitPriceOf(p.supplierId, p.equipmentTypeId)
    const calc = up * (p.count || 0)
    return calc > 0 ? calc : Number(p.budget || 0)
}

/* 仅列出对指定设备类型有报价的供应商 */
function suppliersForType(typeId) {
    const supplierIds = new Set(quotes.value.filter(q => q.typeId === String(typeId)).map(q => q.supplierId))
    return suppliers.value.filter(s => supplierIds.has(String(s.id)))
}

/* ---------- filter options ---------- */
const supplierOptions = computed(() => suppliers.value.map(v => ({value: String(v.id), label: v.name})))
const typeOptions = computed(() => types.value.map(t => ({value: String(t.id), label: t.name})))
const statusOptions = computed(() => [
    {value: 'under-review', label: 'Under Review'},
    {value: 'procuring', label: 'Procuring'},
    {value: 'arrived', label: 'Arrived'},
    {value: 'terminated', label: 'Terminated'},
])

/* ---------- filters ---------- */
const filters = reactive({
    keyword: '',
    vendorIds: [],
    typeIds: [],
    statuses: ['under-review'],
    qtyMin: undefined,
    qtyMax: undefined,
    amountMin: undefined,
    amountMax: undefined,
})

function resetFilters() {
    filters.keyword = ''
    filters.vendorIds = []
    filters.typeIds = []
    filters.statuses = ['under-review']
    filters.qtyMin = undefined
    filters.qtyMax = undefined
    filters.amountMin = undefined
    filters.amountMax = undefined
}

/* ---------- data -> filtered ---------- */
const filtered = computed(() => {
    const kw = (filters.keyword || '').toLowerCase()
    const qmin = typeof filters.qtyMin === 'number' ? filters.qtyMin : -Infinity
    const qmax = typeof filters.qtyMax === 'number' ? filters.qtyMax : Infinity
    const amin = typeof filters.amountMin === 'number' ? filters.amountMin : -Infinity
    const amax = typeof filters.amountMax === 'number' ? filters.amountMax : Infinity

    return requests.value.filter(p => {
        const up = unitPriceOf(p.supplierId, p.equipmentTypeId)
        const amt = (up > 0 ? up * (p.count || 0) : Number(p.budget || 0))

        const matchKw = !kw || [`#${p.procureId}`, supplierName(p.supplierId), typeName(p.equipmentTypeId), (p.deviceName || ''), (p.spec || '')]
            .some(s => (s || '').toLowerCase().includes(kw))

        const matchVendor = filters.vendorIds.length === 0 || filters.vendorIds.includes(String(p.supplierId))
        const matchType = filters.typeIds.length === 0 || filters.typeIds.includes(String(p.equipmentTypeId))
        const matchStatus = filters.statuses.length === 0 || filters.statuses.includes(String(p.status))

        const matchQty = (p.count || 0) >= qmin && (p.count || 0) <= qmax
        const matchAmt = amt >= amin && amt <= amax

        return matchKw && matchVendor && matchType && matchStatus && matchQty && matchAmt
    })
})

/* ---------- APIs ---------- */
async function loadVendors() {
    const r = await fetch('/req/proc/vendors')
    const j = await r.json()
    suppliers.value = j.code === '000'
        ? (j.data || []).map(x => ({id: String(x.supplier_id || x.id), name: x.supplier_name || x.name || '-'}))
        : []
}

async function loadTypes() {
    const r = await fetch('/req/proc/equipmentTypes')
    const j = await r.json()
    types.value = j.code === '000'
        ? (j.data || []).map(x => ({
            id: String(x.equipment_type_id || x.id),
            name: x.equipment_type_name || x.name || '-'
        }))
        : []
}

async function loadQuotes() {
    const r = await fetch('/req/proc/quotes')
    const j = await r.json()
    quotes.value = j.code === '000'
        ? (j.data || []).map(x => ({
            supplierId: String(x.supplier_id || x.supplierId),
            typeId: String(x.equipment_type_id || x.equipmentTypeId),
            price: Number(x.price || 0)
        }))
        : []
}

async function loadRequests() {
    const r = await fetch('/req/proc/orders')
    const j = await r.json()
    const all = j.code === '000'
        ? (j.data || []).map(x => ({
            procureId: Number(x.procure_id || x.procureId),
            equipmentTypeId: String(x.equipment_type_id || x.equipmentTypeId || ''),
            count: Number(x.count || 0),
            supplierId: String(x.supplier_id || x.supplierId || '0000'),
            status: String(x.status || 'under-review'),
            deviceName: x.device_name || x.deviceName || '',
            spec: x.spec || x.specification || '',
            budget: Number(x.budget || 0),
        }))
        : []
    requests.value = all
}

/* ---------- 行内：更换供应商 ---------- */
async function onSupplierChange(p) {
    // 若选择了 “— Select —” 则不提交，仅刷新显示
    if (!p.supplierId || p.supplierId === '0000') return

    // 使用已有接口 /assign 持久化 supplier 变更（数量传原值）
    const body = {procure_id: p.procureId, supplier_id: p.supplierId, count: p.count}
    const r = await fetch('/req/proc/order/assign', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body)
    })
    const j = await r.json().catch(() => ({code: 'ERR'}))
    if (j.code !== '000') {
        alert(j.message || 'Failed to update supplier')
        // 回滚 UI
        await loadRequests()
        return
    }
    // 成功：金额与单价的显示会自动根据 p.supplierId 变化而更新
}

/* ---------- actions: reject / assign ---------- */
async function reject(p) {
    const r = await fetch('/req/proc/order/status', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({procure_id: p.procureId, status: 'terminated'})
    })
    const j = await r.json().catch(() => ({code: 'ERR'}))
    if (j.code !== '000') return alert(j.message || 'Failed to reject')
    requests.value = requests.value.filter(x => x.procureId !== p.procureId)
}

const modal = reactive({open: false, req: null, form: {supplierId: '0000', count: 1}})

function openAssign(p) {
    modal.open = true
    modal.req = p
    // 默认仅给出该类型有报价的供应商
    modal.form.supplierId = suppliersForType(p.equipmentTypeId)[0]?.id || '0000'
    modal.form.count = p.count || 1
}

function closeAssign() {
    modal.open = false
}

async function createPO() {
    const body = {procure_id: modal.req.procureId, supplier_id: modal.form.supplierId, count: modal.form.count}
    const r = await fetch('/req/proc/order/assign', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body)
    })
    const j = await r.json().catch(() => ({code: 'ERR'}))
    if (j.code !== '000') return alert(j.message || 'Failed to assign vendor')

    const r2 = await fetch('/req/proc/order/status', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({procure_id: modal.req.procureId, status: 'procuring'})
    })
    const j2 = await r2.json().catch(() => ({code: 'ERR'}))
    if (j2.code !== '000') return alert(j2.message || 'Failed to move status')

    requests.value = requests.value.filter(x => x.procureId !== modal.req.procureId)
    closeAssign()
}

/* ---------- New Plan（保持不变） ---------- */
const newPlan = reactive({
    open: false,
    form: {deviceName: '', spec: '', typeId: '', count: 1, budget: 0, supplierId: '0000', remark: ''}
})

function openNewPlan() {
    newPlan.open = true
}

function closeNewPlan() {
    newPlan.open = false
}

async function submitNewPlan() {
    const f = newPlan.form
    if (!f.deviceName.trim()) return alert('Device Name is required')
    if (!f.spec.trim()) return alert('Specification is required')
    if (!Number(f.count) || f.count < 1) return alert('Quantity must be ≥ 1')
    if (f.budget < 0) return alert('Budget must be ≥ 0')
    const payload = {
        device_name: f.deviceName.trim(),
        spec: f.spec.trim(),
        count: Number(f.count),
        budget: Number(f.budget || 0),
        equipment_type_id: f.typeId || '',
        supplier_id: f.supplierId || '0000',
        remark: f.remark || '',
        status: 'under-review'
    }
    const r = await fetch('/req/proc/plan', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload)
    })
    const j = await r.json().catch(() => ({code: 'ERR'}))
    if (j.code !== '000') return alert(j.message || 'Failed to create plan')
    const newId = Number(j.data?.procure_id || Date.now())
    requests.value.unshift({
        procureId: newId,
        equipmentTypeId: String(f.typeId || ''),
        count: Number(f.count),
        supplierId: String(f.supplierId || '0000'),
        status: 'under-review',
        deviceName: f.deviceName,
        spec: f.spec,
        budget: Number(f.budget || 0),
    })
    closeNewPlan()
}

/* init */
onMounted(async () => {
    loading.value = true
    try {
        await Promise.all([loadVendors(), loadTypes(), loadQuotes(), loadRequests()])
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
    white-space: nowrap;
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
