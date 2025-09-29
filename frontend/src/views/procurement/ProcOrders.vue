<template>
    <div class="card" style="padding:16px;">
        <div class="title-lg">Purchase Orders</div>
        <div class="subtitle" style="margin-top:8px;">
            Update order status and push to equipment team for onboarding.
        </div>

        <!-- Filters -->
        <div class="ui-toolbar" style="margin-top:12px; display:flex; flex-wrap:wrap; gap:12px;">
            <input class="input" v-model="filters.keyword" placeholder="Search by PO / vendor / type"
                   style="min-width:220px;"/>

            <div style="min-width:220px;">
                <label>Status</label>
                <MultiSelect v-model="filters.statuses" :options="statusOptions" placeholder="All status"/>
            </div>

            <div style="min-width:220px;">
                <label>Vendor</label>
                <MultiSelect v-model="filters.vendorIds" :options="vendorOptions" placeholder="All vendors"/>
            </div>

            <div style="min-width:220px;">
                <label>Equipment Type</label>
                <MultiSelect v-model="filters.typeIds" :options="typeOptions" placeholder="All types"/>
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

            <div style="display:flex; gap:8px; align-items:end;">
                <button class="btn" @click="resetFilters">Reset</button>
            </div>
        </div>

        <!-- Table -->
        <div class="table-wrapper" style="margin-top:16px; overflow:auto;">
            <table class="ui-table" style="table-layout:auto; width:100%;">
                <thead>
                <tr>
                    <th>PO ID</th>
                    <th>Equipment Type</th>
                    <th>Qty</th>
                    <th>Vendor</th>
                    <th>Unit Price</th>
                    <th>Total Price</th>
                    <th>Status</th>
                    <th style="width:260px;">Actions</th>
                </tr>
                </thead>
                <tbody>
                <tr v-if="loading">
                    <td colspan="8">
                        <TableSkeleton :rows="6"/>
                    </td>
                </tr>
                <tr v-else-if="paged.length===0">
                    <td colspan="8" style="text-align:center; color:var(--color-muted);">No orders</td>
                </tr>
                <tr v-else v-for="o in paged" :key="o.procureId">
                    <td>#{{ o.procureId }}</td>
                    <td>{{ typeName(o.equipmentTypeId) }}</td>
                    <td>{{ o.count }}</td>
                    <td>{{ vendorName(o.supplierId) }}</td>
                    <td>{{ money(unitPriceOf(o.supplierId, o.equipmentTypeId)) }}</td>
                    <td>{{ money(unitPriceOf(o.supplierId, o.equipmentTypeId) * o.count) }}</td>
                    <td>{{ o.status }}</td>
                    <td style="white-space:nowrap;">
                        <!-- Under Review 按钮不需要 -->
                        <button
                            class="btn btn-blue"
                            style="margin-left:6px;"
                            :disabled="!canStart(o)"
                            :class="{ nohover: !canStart(o) }"
                            @click="flow(o, 'procuring')"
                        >
                            Start
                        </button>

                        <button
                            class="btn btn-green"
                            style="margin-left:6px;"
                            :disabled="!canConfirmArrival(o)"
                            :class="{ nohover: !canConfirmArrival(o) }"
                            @click="flow(o, 'arrived')"
                        >
                            Confirm Arrival
                        </button>

                        <button
                            class="btn btn-red"
                            style="margin-left:6px;"
                            :disabled="!canTerminate(o)"
                            :class="{ nohover: !canTerminate(o) }"
                            @click="flow(o, 'terminated')"
                        >
                            Terminate
                        </button>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>

        <!-- Pagination -->
        <div class="ui-pagination" style="margin-top:12px;">
            <button class="btn" :disabled="page===1" :class="{ nohover: page===1 }" @click="page=1">First</button>
            <button class="btn" :disabled="page===1" :class="{ nohover: page===1 }" @click="page--">Prev</button>
            <span style="color:var(--color-muted);">Page {{ page }} / {{ totalPages }}</span>
            <button class="btn" :disabled="page===totalPages" :class="{ nohover: page===totalPages }" @click="page++">
                Next
            </button>
            <button class="btn" :disabled="page===totalPages" :class="{ nohover: page===totalPages }"
                    @click="page=totalPages">Last
            </button>
            <select class="input" style="width:auto;" v-model.number="pageSize">
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
            </select>
        </div>
    </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted, watch} from 'vue'
import MultiSelect from '@/components/MultiSelect.vue'
import TableSkeleton from '@/components/admin/TableSkeleton.vue'

/** 与后端一致的状态值 */
const STATUSES = ['under-review', 'procuring', 'arrived', 'terminated']

const loading = ref(true)
const orders = ref([])     // [{procureId, equipmentTypeId, count, supplierId, status}]
const vendors = ref([])    // [{id,name}]
const types = ref([])      // [{id,name}]
const quotes = ref([])     // [{supplierId,typeId,price}]

/* ---------- filters ---------- */
const filters = reactive({
    keyword: '',
    statuses: [],         // 支持多选
    vendorIds: [],
    typeIds: [],
    qtyMin: undefined,
    qtyMax: undefined,
    amountMin: undefined,
    amountMax: undefined,
})

/* ---------- options ---------- */
const statusOptions = computed(() => STATUSES.map(s => ({value: s, label: s})))
const vendorOptions = computed(() => vendors.value.map(v => ({value: String(v.id), label: v.name})))
const typeOptions = computed(() => types.value.map(t => ({value: String(t.id), label: t.name})))

/* ------- helpers ------- */
function money(n) {
    try {
        return Number(n || 0).toLocaleString(undefined, {style: 'currency', currency: 'USD'})
    } catch {
        return String(n)
    }
}

function vendorName(id) {
    const v = vendors.value.find(v => String(v.id) === String(id));
    return v ? v.name : id
}

function typeName(id) {
    const t = types.value.find(t => String(t.id) === String(id));
    return t ? t.name : id
}

function unitPriceOf(supplierId, typeId) {
    const q = quotes.value.find(q => q.supplierId === String(supplierId) && q.typeId === String(typeId))
    return q ? Number(q.price || 0) : 0
}

/** ---------- 按钮可用性规则 ---------- */
/**
 * - arrived：全部不能操作
 * - terminated：全部不能操作
 * - under-review：有 vendor -> 只能 Start / Terminate；没有 vendor -> 只能 Terminate
 * - procuring：可以 Confirm Arrival / Terminate，不能 Start
 */
function hasVendor(o) {
    const sid = String(o.supplierId || '')
    return !!sid && sid !== '0000'
}

function canStart(o) {
    if (o.status === 'arrived' || o.status === 'terminated') return false
    if (o.status === 'procuring') return false
    if (o.status === 'under-review') return hasVendor(o) // 有 vendor 才能 start
    return false
}

function canConfirmArrival(o) {
    if (o.status === 'arrived' || o.status === 'terminated') return false
    // under-review 不允许 Confirm Arrival；procuring 才能
    return o.status === 'procuring'
}

function canTerminate(o) {
    if (o.status === 'arrived' || o.status === 'terminated') return false
    // 其余状态均可终止
    return o.status === 'under-review' || o.status === 'procuring'
}

function resetFilters() {
    filters.keyword = ''
    filters.statuses = []
    filters.vendorIds = []
    filters.typeIds = []
    filters.qtyMin = undefined
    filters.qtyMax = undefined
    filters.amountMin = undefined
    filters.amountMax = undefined
}

/* ------- computed & pagination ------- */
const filtered = computed(() => {
    const kw = (filters.keyword || '').toLowerCase()
    const qmin = typeof filters.qtyMin === 'number' ? filters.qtyMin : -Infinity
    const qmax = typeof filters.qtyMax === 'number' ? filters.qtyMax : Infinity
    const amin = typeof filters.amountMin === 'number' ? filters.amountMin : -Infinity
    const amax = typeof filters.amountMax === 'number' ? filters.amountMax : Infinity

    return orders.value.filter(o => {
        const up = unitPriceOf(o.supplierId, o.equipmentTypeId)
        const amt = up * (o.count || 0)

        const hitKw = !kw || [`#${o.procureId}`, vendorName(o.supplierId), typeName(o.equipmentTypeId)]
            .some(s => (s || '').toLowerCase().includes(kw))
        const hitStatus = filters.statuses.length === 0 || filters.statuses.includes(o.status)
        const hitVendor = filters.vendorIds.length === 0 || filters.vendorIds.includes(String(o.supplierId))
        const hitType = filters.typeIds.length === 0 || filters.typeIds.includes(String(o.equipmentTypeId))
        const hitQty = (o.count || 0) >= qmin && (o.count || 0) <= qmax
        const hitAmt = amt >= amin && amt <= amax

        return hitKw && hitStatus && hitVendor && hitType && hitQty && hitAmt
    })
})

const page = ref(1)
const pageSize = ref(10)
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))
watch([filtered, pageSize], () => {
    if (page.value > totalPages.value) page.value = totalPages.value
})
const paged = computed(() => {
    const start = (page.value - 1) * pageSize.value
    return filtered.value.slice(start, start + pageSize.value)
})

/* ------- API ------- */
async function loadOrders() {
    const r = await fetch('/req/proc/orders');
    const j = await r.json()
    orders.value = j.code === '000'
        ? (j.data || []).map(x => ({
            procureId: Number(x.procure_id || x.procureId),
            equipmentTypeId: String(x.equipment_type_id || x.equipmentTypeId),
            count: Number(x.count || 0),
            supplierId: String(x.supplier_id || x.supplierId || ''), // 可能为空或'0000'
            status: String(x.status || 'under-review'),
        }))
        : []
}

async function loadVendors() {
    const r = await fetch('/req/proc/vendors');
    const j = await r.json()
    vendors.value = j.code === '000'
        ? (j.data || []).map(x => ({id: String(x.supplier_id || x.id), name: x.supplier_name || x.name || '-'}))
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

/* 状态流转（仅在前述可用性判断为 true 时才会触发） */
async function flow(o, toStatus) {
    if (o.status === toStatus) return
    const r = await fetch('/req/proc/order/status', {
        method: 'PUT',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({procure_id: o.procureId, status: toStatus})
    })
    const j = await r.json().catch(() => ({code: 'ERR'}))
    if (j.code === '000') o.status = toStatus
    else alert(j.message || 'Failed to update status')
}

/* ------- init ------- */
onMounted(async () => {
    loading.value = true
    try {
        await Promise.all([loadVendors(), loadTypes(), loadQuotes(), loadOrders()])
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

/* 禁用按钮去掉 hover/指针效果 */
.btn.nohover {
    pointer-events: none;
    opacity: .6;
}
</style>
