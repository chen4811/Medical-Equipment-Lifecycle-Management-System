<template>
    <div class="card" style="padding:16px;">
        <div class="title-lg">Purchase Requests</div>
        <div class="subtitle" style="margin-top:8px;">
            Review requests (orders in <code>under-review</code>) and assign vendors to create purchase orders.
        </div>

        <!-- Filters + New Plan -->
        <div class="ui-toolbar" style="margin-top:12px; display:flex; flex-wrap:wrap; gap:12px; align-items:end;">
            <input
                class="input"
                v-model="filters.keyword"
                placeholder="Search by ID / type / supplier"
                style="min-width:220px;"
            />

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
                <input
                    class="input"
                    type="number"
                    v-model.number="filters.amountMin"
                    placeholder="Amount ≥ $"
                    style="width:140px;"
                />
                <input
                    class="input"
                    type="number"
                    v-model.number="filters.amountMax"
                    placeholder="Amount ≤ $"
                    style="width:140px;"
                />
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
                    <th>Requester</th>
                    <th>Department</th>
                    <th style="min-width:220px;">Supplier</th>
                    <th>Unit Price</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th style="width:220px;">Actions</th>
                </tr>
                </thead>
                <tbody>
                <tr v-if="loading">
                    <td colspan="10">
                        <TableSkeleton :rows="6"/>
                    </td>
                </tr>
                <tr v-else-if="filtered.length === 0">
                    <td colspan="10" style="text-align:center; color:var(--color-muted);">No requests</td>
                </tr>
                <tr v-else v-for="p in filtered" :key="p.procureId">
                    <td>#{{ p.procureId }}</td>
                    <td>{{ typeName(p.equipmentTypeId) }}</td>
                    <td>{{ p.count }}</td>

                    <!-- Requester / Department 使用映射表 -->
                    <td>{{ requesterName(p.requesterId) }}</td>
                    <td>{{ departmentName(p) }}</td>

                    <!-- 供应商选择（仅显示该类型有报价的供应商；选择 Select 时清空 DB） -->
                    <td>
                        <select class="input" v-model="p.supplierId" @change="onSupplierChange(p)">
                            <option value="0000">— Select —</option>
                            <option v-for="v in suppliersForType(p.equipmentTypeId)" :key="v.id" :value="v.id">
                                {{ v.name }}
                            </option>
                        </select>
                    </td>

                    <td>{{ money(unitPriceOf(p.supplierId, p.equipmentTypeId)) }}</td>
                    <td>{{ money(fallbackAmount(p)) }}</td>
                    <td>{{ p.status }}</td>

                    <td style="white-space:nowrap;">
                        <button class="btn btn-red" @click="reject(p)">Reject</button>
                        <button
                            class="btn btn-green"
                            style="margin-left:8px;"
                            :disabled="!canStart(p)"
                            :class="{ nohover: !canStart(p) }"
                            @click="startProcurement(p)"
                        >
                            Start
                        </button>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>

        <!-- New Plan Modal（对齐 Vendors 弹窗风格：modal-backdrop / modal / form-grid / footer-actions） -->
        <div v-if="newPlan.open" class="modal-backdrop">
            <div class="modal card">
                <div class="title-lg">New Purchase Plan</div>

                <div class="form-grid">
                    <div style="grid-column: 1 / -1;">
                        <label>Device Name <span class="req">*</span></label>
                        <input class="input" v-model="newPlan.form.deviceName" placeholder="e.g. ECG Monitor"/>
                    </div>

                    <div style="grid-column: 1 / -1;">
                        <label>Specification <span class="req">*</span></label>
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
                        <label>Quantity <span class="req">*</span></label>
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
                        <textarea
                            class="input"
                            rows="3"
                            v-model="newPlan.form.remark"
                            placeholder="Optional notes, delivery expectation, etc."
                        ></textarea>
                    </div>
                </div>

                <div class="footer-actions">
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

const loading = ref(true)

const suppliers = ref([]) // [{id,name}]
const types = ref([]) // [{id,name}]
const quotes = ref([]) // [{supplierId,typeId,price}]
const requests = ref([]) // 订单（默认 under-review）
const accounts = ref([]) // [{id,name,departmentId}]
const departments = ref([]) // [{id,name}]

/* ---------- helpers ---------- */
function money(n) {
    try {
        return Number(n || 0).toLocaleString(undefined, {style: 'currency', currency: 'USD'})
    } catch {
        return String(n)
    }
}

function supplierName(id) {
    const v = suppliers.value.find(v => String(v.id) === String(id))
    return v ? v.name : id === '0000' ? '-' : id
}

function typeName(id) {
    const t = types.value.find(t => String(t.id) === String(id))
    return t ? t.name : id
}

function unitPriceOf(sid, tid) {
    const q = quotes.value.find(q => q.supplierId === String(sid) && q.typeId === String(tid))
    return q ? Number(q.price || 0) : 0
}

function fallbackAmount(p) {
    const up = unitPriceOf(p.supplierId, p.equipmentTypeId)
    const calc = up * (p.count || 0)
    return calc > 0 ? calc : Number(p.budget || 0)
}

function suppliersForType(typeId) {
    const ids = new Set(quotes.value.filter(q => q.typeId === String(typeId)).map(q => q.supplierId))
    return suppliers.value.filter(s => ids.has(String(s.id)))
}

function hasVendor(p) {
    const sid = String(p.supplierId || '')
    return !!sid && sid !== '0000'
}

function canStart(p) {
    return p.status === 'under-review' && hasVendor(p)
}

/* ---------- Requester / Department 名称映射 ---------- */
function requesterName(accountId) {
    const a = accounts.value.find(x => String(x.id) === String(accountId))
    return a ? a.name || a.id : accountId || '-'
}

function departmentName(p) {
    // 优先订单上的 departmentId，其次取该请求人的部门
    const depId = String(
        p.departmentId || accounts.value.find(x => String(x.id) === String(p.requesterId))?.departmentId || ''
    )
    if (!depId) return '-'
    const d = departments.value.find(x => String(x.id) === depId)
    return d ? d.name : depId
}

/* ---------- filter options ---------- */
const supplierOptions = computed(() => suppliers.value.map(v => ({value: String(v.id), label: v.name})))
const typeOptions = computed(() => types.value.map(t => ({value: String(t.id), label: t.name})))
const statusOptions = computed(() => [
    {value: 'under-review', label: 'Under Review'},
    {value: 'procuring', label: 'Procuring'},
    {value: 'arrived', label: 'Arrived'},
    {value: 'terminated', label: 'Terminated'}
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
    amountMax: undefined
})

function resetFilters() {
    filters.keyword = ''
    filters.vendorIds = []
    filters.typeIds = []
    filters.statuses = ['under-review']
    filters.qtyMin = filters.qtyMax = filters.amountMin = filters.amountMax = undefined
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
        const amt = up > 0 ? up * (p.count || 0) : Number(p.budget || 0)
        const matchKw =
            !kw ||
            [
                `#${p.procureId}`,
                supplierName(p.supplierId),
                typeName(p.equipmentTypeId),
                requesterName(p.requesterId),
                departmentName(p),
                p.deviceName || '',
                p.spec || ''
            ].some(s => (s || '').toLowerCase().includes(kw))
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
    suppliers.value =
        j.code === '000'
            ? (j.data || []).map(x => ({
                id: String(x.supplier_id || x.id),
                name: x.supplier_name || x.name || '-'
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

async function loadRequests() {
    const r = await fetch('/req/proc/orders')
    const j = await r.json()
    const all =
        j.code === '000'
            ? (j.data || []).map(x => ({
                procureId: Number(x.procure_id || x.procureId),
                equipmentTypeId: String(x.equipment_type_id || x.equipmentTypeId || ''),
                count: Number(x.count || 0),
                supplierId: String(x.supplier_id || x.supplierId || '0000'),
                status: String(x.status || 'under-review'),
                deviceName: x.device_name || x.deviceName || '',
                spec: x.spec || x.specification || '',
                budget: Number(x.budget || 0),
                requesterId: String(x.requester_id || x.requesterId || ''),
                departmentId: String(x.department_id || x.departmentId || '')
            }))
            : []
    requests.value = all
}

/* 账户、部门（方案B关键） */
async function loadAccounts() {
    // 使用现有 AdminController 的 /req/admin/users
    const r = await fetch('/req/admin/users')
    const j = await r.json().catch(() => ({code: 'ERR', data: []}))
    accounts.value =
        j.code === '000'
            ? (j.data || []).map(x => ({
                id: String(x.id || x.account_id),
                // 兼容 username / name / display_name
                name: x.name || x.username || x.display_name || '-',
                departmentId: String(x.department_id || x.departmentId || '')
            }))
            : []
}

async function loadDepartments() {
    const r = await fetch('/req/admin/departments')
    const j = await r.json().catch(() => ({code: 'ERR', data: []}))
    departments.value =
        j.code === '000'
            ? (j.data || []).map(x => ({
                id: String(x.id || x.department_id || x.departmentId),
                name: x.name || x.department_name || x.departmentName || '-'
            }))
            : []
}

/* ---------- 行内：供应商变更（含清空） ---------- */
async function onSupplierChange(p) {
    if (!p.supplierId || p.supplierId === '0000') {
        // 清空
        const r = await fetch('/req/proc/order/assign', {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({procure_id: p.procureId, supplier_id: '0000', count: p.count})
        })
        const j = await r.json().catch(() => ({code: 'ERR'}))
        if (j.code !== '000') {
            alert(j.message || 'Failed to clear supplier')
            await loadRequests()
        }
        return
    }
    // 选择供应商
    const r = await fetch('/req/proc/order/assign', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({procure_id: p.procureId, supplier_id: p.supplierId, count: p.count})
    })
    const j = await r.json().catch(() => ({code: 'ERR'}))
    if (j.code !== '000') {
        alert(j.message || 'Failed to update supplier')
        await loadRequests()
    }
}

/* ---------- Actions ---------- */
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

async function startProcurement(p) {
    if (!canStart(p)) return
    const r = await fetch('/req/proc/order/status', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({procure_id: p.procureId, status: 'procuring'})
    })
    const j = await r.json().catch(() => ({code: 'ERR'}))
    if (j.code !== '000') return alert(j.message || 'Failed to start')
    p.status = 'procuring'
}

/* ---------- New Plan（弹窗控制） ---------- */
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
        requesterId: '',
        departmentId: ''
    })
    closeNewPlan()
}

/* init */
onMounted(async () => {
    loading.value = true
    try {
        await Promise.all([loadVendors(), loadTypes(), loadQuotes(), loadRequests(), loadAccounts(), loadDepartments()])
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
    white-space: nowrap;
}

.ui-table th {
    background: #f9fafb;
    font-weight: 700;
}

/* 禁用按钮去掉 hover/指针效果 */
.btn.nohover {
    pointer-events: none;
    opacity: 0.6;
}

/* ====== 新增：与 Vendors 页一致的弹窗样式 ====== */
.modal-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.35);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
    z-index: 1000;
}

.modal {
    width: min(720px, 100%);
    padding: 16px;
}

/* 两列自适应表单栅格（与 Vendors 对齐） */
.form-grid {
    margin-top: 16px;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 12px;
}

/* 底部操作按钮区域右对齐 */
.footer-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 16px;
}

.req {
    color: #dc2626;
}
</style>
