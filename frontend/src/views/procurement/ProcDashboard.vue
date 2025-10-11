<template>
    <div class="page">
        <div class="card p16">
            <header class="header">
                <div class="title">
                    <div class="h1">Procurement Overview</div>
                    <div class="sub">Procurement Dashboard</div>
                </div>
                <div class="actions">
                    <button class="btn" @click="refresh" :disabled="loading">Refresh</button>
                    <button class="btn ghost" @click="exportCsv" :disabled="loading">Export CSV</button>
                </div>
            </header>

            <div v-if="loading" class="grid">
                <div class="skeleton" v-for="i in 6" :key="i"></div>
            </div>

            <div v-else-if="error" class="error">
                <div>Failed to load: {{ error }}</div>
                <button class="btn" @click="refresh">Retry</button>
            </div>

            <template v-else>
                <!-- Top stat cards -->
                <div class="grid">
                    <StatCard label="Open Requisitions" :value="openReqs"/>
                    <StatCard label="Pending Receiving" :value="pendingReceiving"/>
                    <StatCard label="Budget Spent (USD)" :value="spent"/>
                    <StatCard label="Vendors" :value="vendorCount"/>
                </div>

                <!-- KPIs -->
                <div class="kpis">
                    <div class="kpi">
                        <div class="kpi-title">Total Orders</div>
                        <div class="kpi-value">{{ fmt(totalOrders) }}</div>
                        <div class="kpi-desc">= Under Review + Procuring + Arrived (+ Terminated)</div>
                    </div>
                    <div class="kpi">
                        <div class="kpi-title">Average Order Value</div>
                        <div class="kpi-value">{{ fmtMoney(avgOrderValue) }}</div>
                        <div class="kpi-desc">Based on non-terminated orders</div>
                    </div>
                    <div class="kpi">
                        <div class="kpi-title">Open Rate</div>
                        <div class="kpi-value">{{ (openRate * 100).toFixed(1) }}%</div>
                        <div class="progress">
                            <div class="bar" :style="{ width: (openRate * 100) + '%' }"></div>
                        </div>
                        <div class="kpi-desc">Under Review / Total Orders</div>
                    </div>
                    <div class="kpi">
                        <div class="kpi-title">Arrival Rate</div>
                        <div class="kpi-value">{{ (arrivalRate * 100).toFixed(1) }}%</div>
                        <div class="progress">
                            <div class="bar success" :style="{ width: (arrivalRate * 100) + '%' }"></div>
                        </div>
                        <div class="kpi-desc">Arrived / Total Orders</div>
                    </div>
                </div>

                <!-- Charts -->
                <div class="stats-grid">
                    <!-- 1: Donut -->
                    <div class="card stat-card">
                        <div class="chart-title">Orders by Status</div>
                        <apexchart height="220" type="donut" :options="cOrdersByStatus.options"
                                   :series="cOrdersByStatus.series"/>
                    </div>

                    <!-- 2: Bar -->
                    <div class="card stat-card">
                        <div class="chart-title">Spend by Supplier (Top 5)</div>
                        <apexchart height="220" type="bar" :options="cSpendBySupplier.options"
                                   :series="cSpendBySupplier.series"/>
                    </div>

                    <!-- 3: Bar -->
                    <div class="card stat-card">
                        <div class="chart-title">Units by Equipment</div>
                        <apexchart height="220" type="bar" :options="cUnitsByType.options"
                                   :series="cUnitsByType.series"/>
                    </div>

                    <!-- 4: Line -->
                    <div class="card stat-card">
                        <div class="chart-title">Monthly Spend (last 6 months)</div>
                        <apexchart height="220" type="line" :options="cMonthlySpendLine.options"
                                   :series="cMonthlySpendLine.series"/>
                    </div>

                    <!-- 5: Area -->
                    <div class="card stat-card">
                        <div class="chart-title">Order Trend (last 12 weeks)</div>
                        <apexchart height="220" type="area" :options="cWeeklyOrdersArea.options"
                                   :series="cWeeklyOrdersArea.series"/>
                    </div>

                    <!-- 6: Stacked Bar -->
                    <div class="card stat-card">
                        <div class="chart-title">Monthly Orders by Status (stacked)</div>
                        <apexchart height="220" type="bar" :options="cMonthlyOrdersStacked.options"
                                   :series="cMonthlyOrdersStacked.series"/>
                    </div>

                    <!-- 7: Heatmap -->
                    <div class="card stat-card">
                        <div class="chart-title">Supplier × Month Spend (heatmap, top 5)</div>
                        <apexchart height="260" type="heatmap" :options="cSupplierMonthHeatmap.options"
                                   :series="cSupplierMonthHeatmap.series"/>
                    </div>

                    <!-- 8: Radar -->
                    <div class="card stat-card">
                        <div class="chart-title">Top Equipment Types (radar by units)</div>
                        <apexchart height="260" type="radar" :options="cTopTypesRadar.options"
                                   :series="cTopTypesRadar.series"/>
                    </div>
                </div>

                <!-- Recent orders -->
                <div class="card p16" style="margin-top:16px;">
                    <div class="title-sm" style="font-weight:700;">Recent Orders</div>
                    <div class="table-wrapper" style="margin-top:8px; overflow:auto;">
                        <table class="ui-table" style="width:100%; table-layout:auto;">
                            <thead>
                            <tr>
                                <th style="width:120px;">Procure ID</th>
                                <th>Supplier</th>
                                <th>Equipment Type</th>
                                <th style="width:100px;">Qty</th>
                                <th style="width:140px;">Status</th>
                                <th style="width:140px;">Amount</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr v-for="o in recentOrders" :key="o.id">
                                <td>{{ o.id }}</td>
                                <td>{{ supplierName(o.supplierId) }}</td>
                                <td>{{ typeName(o.typeId) }}</td>
                                <td>{{ o.count }}</td>
                                <td>{{ statusLabel(o.status) }}</td>
                                <td>{{ fmtMoney(orderAmount(o)) }}</td>
                            </tr>
                            <tr v-if="recentOrders.length===0">
                                <td colspan="6" class="subtitle">No orders</td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                    <p class="timestamp">Last updated: {{ lastUpdated }}</p>
                </div>

                <!-- Summary -->
                <div class="card p16">
                    <div class="subtitle">Procurement Health Summary</div>
                    <p class="note">
                        There are <b>{{ fmt(totalOrders) }}</b> orders in total, with
                        <b>{{ fmt(countBy('under-review')) }}</b> under review,
                        <b>{{ fmt(countBy('procuring')) }}</b> procuring,
                        and <b>{{ fmt(countBy('arrived')) }}</b> arrived. Current spend is
                        <b>{{ fmtMoney(spent) }}</b> across <b>{{ fmt(vendorCount) }}</b> active suppliers.
                    </p>
                </div>
            </template>
        </div>
    </div>
</template>

<script setup>
import StatCard from '@/components/layout/StatCard.vue'
import {ref, computed, onMounted} from 'vue'

const loading = ref(false)
const error = ref('')
const lastUpdated = ref('')

const orders = ref([])
const quotes = ref([])
const vendors = ref([])
const types = ref([])

const STATUS_LABEL = {
    'under-review': 'Under Review',
    'procuring': 'Procuring',
    'arrived': 'Arrived',
    'terminated': 'Terminated',
}

function statusLabel(s) {
    return STATUS_LABEL[s] || s || '-'
}

function fmt(n) {
    return new Intl.NumberFormat().format(Number(n || 0))
}

function fmtMoney(n) {
    return `$${Number(n || 0).toLocaleString()}`
}

function parseDateMaybe(v) {
    if (!v) return null
    const d = new Date(v)
    return isNaN(d.getTime()) ? null : d
}

/* ---------- Date helpers (ASCII-only labels) ---------- */
function pad(n) {
    return String(n).padStart(2, '0')
}

function monthKey(d) {
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
}      // YYYY-MM
function monthLabel(d) {
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
}   // YYYY-MM
function startOfWeek(d) {
    const x = new Date(d);
    const day = x.getDay() || 7;
    x.setHours(0, 0, 0, 0);
    x.setDate(x.getDate() - (day - 1));
    return x
}

function weekKey(d) {
    const s = startOfWeek(d);
    return `${s.getFullYear()}-${pad(s.getMonth() + 1)}-${pad(s.getDate())}`
} // YYYY-MM-DD
function weekLabel(d) {
    return `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}     // MM-DD

async function loadOrders() {
    const r = await fetch('/req/proc/orders')
    const j = await r.json()
    const raw = j.code === '000'
        ? (j.data || []).map(x => ({
            id: Number(x.procure_id ?? x.id ?? 0),
            supplierId: String(x.supplier_id ?? x.supplierId ?? ''),
            typeId: String(x.equipment_type_id ?? x.equipmentTypeId ?? ''),
            count: Number(x.count ?? 0),
            status: String(x.status ?? 'under-review'),
            createdAt: parseDateMaybe(x.created_at ?? x.createdAt ?? x.order_date ?? x.orderDate ?? null),
        }))
        : []

    // Backfill dates if all are missing: spread over the last 12 weeks
    const needBackfill = raw.every(o => !o.createdAt)
    if (needBackfill) {
        const now = new Date()
        raw.sort((a, b) => (a.id ?? 0) - (b.id ?? 0)).forEach((o, idx) => {
            const weeksBack = Math.min(11, idx % 12)
            const d = new Date(now)
            d.setDate(d.getDate() - weeksBack * 7)
            o.createdAt = d
        })
    }
    orders.value = raw
}

async function loadQuotes() {
    const r = await fetch('/req/proc/quotes')
    const j = await r.json()
    quotes.value = j.code === '000'
        ? (j.data || []).map(x => ({
            supplierId: String(x.supplier_id ?? x.supplierId ?? ''),
            typeId: String(x.equipment_type_id ?? x.equipmentTypeId ?? ''),
            price: Number(x.price ?? 0),
        }))
        : []
}

async function loadVendors() {
    const r = await fetch('/req/proc/vendors')
    const j = await r.json()
    vendors.value = j.code === '000'
        ? (j.data || []).map(x => ({
            id: String(x.supplier_id ?? x.id ?? ''),
            name: String(x.supplier_name ?? x.name ?? x.id ?? '-'),
        }))
        : []
}

async function loadTypes() {
    const r = await fetch('/req/proc/equipmentTypes')
    const j = await r.json()
    types.value = j.code === '000'
        ? (j.data || []).map(x => ({
            id: String(x.equipment_type_id ?? x.id ?? ''),
            name: String(x.equipment_type_name ?? x.name ?? x.id ?? ''),
        }))
        : []
}

function supplierName(id) {
    return vendors.value.find(v => v.id === id)?.name || id || '-'
}

function typeName(id) {
    return types.value.find(t => t.id === id)?.name || id || '-'
}

function unitPriceOf(supplierId, typeId) {
    const q = quotes.value.find(q => q.supplierId === supplierId && q.typeId === typeId)
    return q ? Number(q.price || 0) : 0
}

function orderAmount(o) {
    return unitPriceOf(o.supplierId, o.typeId) * (o.count || 0)
}

function countBy(status) {
    return orders.value.filter(o => o.status === status).length
}

const openReqs = computed(() => countBy('under-review'))
const pendingReceiving = computed(() => countBy('arrived'))
const spent = computed(() =>
    orders.value.filter(o => o.status !== 'terminated').reduce((sum, o) => sum + orderAmount(o), 0)
)
const vendorCount = computed(() => vendors.value.length)

const totalOrders = computed(() => orders.value.length)
const nonTerminated = computed(() => orders.value.filter(o => o.status !== 'terminated'))
const avgOrderValue = computed(() => {
    const c = nonTerminated.value.length
    return c ? nonTerminated.value.reduce((s, o) => s + orderAmount(o), 0) / c : 0
})
const openRate = computed(() => totalOrders.value ? countBy('under-review') / totalOrders.value : 0)
const arrivalRate = computed(() => totalOrders.value ? countBy('arrived') / totalOrders.value : 0)

/* Base charts */
const cOrdersByStatus = computed(() => {
    const keys = ['under-review', 'procuring', 'arrived', 'terminated']
    const series = keys.map(k => countBy(k))
    return {
        series,
        options: {
            labels: keys.map(k => STATUS_LABEL[k]),
            legend: {position: 'bottom'},
            dataLabels: {enabled: true},
        }
    }
})

const cSpendBySupplier = computed(() => {
    const map = new Map()
    for (const o of orders.value) {
        if (o.status === 'terminated') continue
        const key = o.supplierId || '-'
        map.set(key, (map.get(key) || 0) + orderAmount(o))
    }
    const rows = Array.from(map.entries()).sort((a, b) => b[1] - a[1]).slice(0, 5)
    return {
        series: [{name: 'Amount', data: rows.map(([, v]) => Math.round(v))}],
        options: {
            chart: {toolbar: {show: false}},
            plotOptions: {bar: {columnWidth: '55%'}},
            dataLabels: {enabled: true},
            xaxis: {categories: rows.map(([k]) => supplierName(k))},
            yaxis: {labels: {formatter: v => `$${Math.round(v).toLocaleString()}`}}
        }
    }
})

const cUnitsByType = computed(() => {
    const map = new Map()
    for (const o of orders.value) {
        const key = o.typeId || '-'
        map.set(key, (map.get(key) || 0) + (o.count || 0))
    }
    const rows = Array.from(map.entries()).sort((a, b) => b[1] - a[1]).slice(0, 8)
    return {
        series: [{name: 'Units', data: rows.map(([, v]) => v)}],
        options: {
            chart: {toolbar: {show: false}},
            plotOptions: {bar: {columnWidth: '55%'}},
            dataLabels: {enabled: true},
            xaxis: {categories: rows.map(([k]) => typeName(k))}
        }
    }
})

/* Line: monthly spend (6 months) */
const cMonthlySpendLine = computed(() => {
    const now = new Date()
    const months = []
    for (let i = 5; i >= 0; i--) months.push(new Date(now.getFullYear(), now.getMonth() - i, 1))
    const keys = months.map(d => monthKey(d))
    const labels = months.map(d => monthLabel(d)) // ASCII yyyy-mm
    const map = new Map(keys.map(k => [k, 0]))
    for (const o of orders.value) {
        const d = o.createdAt;
        if (!d) continue
        const k = monthKey(d);
        if (map.has(k)) map.set(k, map.get(k) + orderAmount(o))
    }
    return {
        series: [{name: 'Spend', data: keys.map(k => Math.round(map.get(k) || 0))}],
        options: {
            chart: {toolbar: {show: false}, zoom: {enabled: false}},
            stroke: {curve: 'smooth', width: 3},
            dataLabels: {enabled: false},
            xaxis: {categories: labels},
            yaxis: {labels: {formatter: v => `$${Math.round(v).toLocaleString()}`}},
            tooltip: {y: {formatter: v => `$${Math.round(v).toLocaleString()}`}}
        }
    }
})

/* Area: weekly orders (12 weeks) */
const cWeeklyOrdersArea = computed(() => {
    const now = new Date()
    const weeks = []
    for (let i = 11; i >= 0; i--) {
        const d = new Date(now);
        d.setDate(d.getDate() - i * 7);
        weeks.push(d)
    }
    const keys = weeks.map(d => weekKey(d))
    const labels = weeks.map(d => weekLabel(d)) // ASCII mm-dd
    const map = new Map(keys.map(k => [k, 0]))
    for (const o of orders.value) {
        const d = o.createdAt;
        if (!d) continue
        const k = weekKey(d);
        if (map.has(k)) map.set(k, map.get(k) + 1)
    }
    return {
        series: [{name: 'Orders', data: keys.map(k => map.get(k) || 0)}],
        options: {
            chart: {toolbar: {show: false}, zoom: {enabled: false}},
            stroke: {curve: 'smooth', width: 2},
            dataLabels: {enabled: false},
            fill: {type: 'gradient', gradient: {opacityFrom: 0.35, opacityTo: 0.05}},
            xaxis: {categories: labels},
            yaxis: {decimalsInFloat: 0},
            tooltip: {y: {formatter: v => `${v} orders`}}
        }
    }
})

/* Stacked Bar: monthly orders by status (6 months) */
const cMonthlyOrdersStacked = computed(() => {
    const now = new Date()
    const months = []
    for (let i = 5; i >= 0; i--) months.push(new Date(now.getFullYear(), now.getMonth() - i, 1))
    const keys = months.map(d => monthKey(d))
    const labels = months.map(d => monthLabel(d)) // ASCII yyyy-mm
    const statuses = ['under-review', 'procuring', 'arrived', 'terminated']
    const base = () => new Map(keys.map(k => [k, 0]))
    const bucket = {
        'under-review': base(),
        'procuring': base(),
        'arrived': base(),
        'terminated': base(),
    }
    for (const o of orders.value) {
        const d = o.createdAt;
        if (!d) continue
        const k = monthKey(d);
        if (!bucket[o.status] || !bucket[o.status].has(k)) continue
        bucket[o.status].set(k, bucket[o.status].get(k) + 1)
    }
    return {
        series: statuses.map(s => ({name: STATUS_LABEL[s], data: keys.map(k => bucket[s].get(k) || 0)})),
        options: {
            chart: {stacked: true, toolbar: {show: false}},
            plotOptions: {bar: {columnWidth: '55%'}},
            dataLabels: {enabled: false},
            xaxis: {categories: labels},
            legend: {position: 'bottom'}
        }
    }
})

/* Heatmap: supplier × month spend (top 5 suppliers, 6 months) */
const cSupplierMonthHeatmap = computed(() => {
    const now = new Date()
    const months = []
    for (let i = 5; i >= 0; i--) months.push(new Date(now.getFullYear(), now.getMonth() - i, 1))
    const keys = months.map(d => monthKey(d))
    const labels = months.map(d => monthLabel(d)) // ASCII yyyy-mm

    const spendByVendor = new Map()
    for (const o of orders.value) {
        if (o.status === 'terminated') continue
        spendByVendor.set(o.supplierId, (spendByVendor.get(o.supplierId) || 0) + orderAmount(o))
    }
    const topVendors = Array.from(spendByVendor.entries()).sort((a, b) => b[1] - a[1]).slice(0, 5).map(([id]) => id)

    const matrix = new Map(topVendors.map(v => [v, new Map(keys.map(k => [k, 0]))]))
    for (const o of orders.value) {
        if (!topVendors.includes(o.supplierId)) continue
        const d = o.createdAt;
        if (!d) continue
        const k = monthKey(d)
        if (matrix.get(o.supplierId)?.has(k)) {
            matrix.get(o.supplierId).set(k, matrix.get(o.supplierId).get(k) + orderAmount(o))
        }
    }

    const series = topVendors.map(v => ({
        name: supplierName(v),
        data: keys.map((k, idx) => ({x: labels[idx], y: Math.round(matrix.get(v).get(k) || 0)}))
    }))

    return {
        series,
        options: {
            chart: {toolbar: {show: false}},
            dataLabels: {enabled: false},
            colors: ['#dbeafe', '#93c5fd', '#3b82f6', '#1d4ed8'],
            plotOptions: {heatmap: {shadeIntensity: 0.4}},
        }
    }
})

/* Radar: top equipment types by units */
const cTopTypesRadar = computed(() => {
    const map = new Map()
    for (const o of orders.value) {
        map.set(o.typeId, (map.get(o.typeId) || 0) + (o.count || 0))
    }
    const top = Array.from(map.entries()).sort((a, b) => b[1] - a[1]).slice(0, 6)
    const labels = top.map(([t]) => typeName(t))
    const data = top.map(([, v]) => v)
    return {
        series: [{name: 'Units', data}],
        options: {
            chart: {toolbar: {show: false}},
            xaxis: {categories: labels},
            dataLabels: {enabled: true}
        }
    }
})

const recentOrders = computed(() =>
    [...orders.value]
        .sort((a, b) => (b.createdAt?.getTime?.() || 0) - (a.createdAt?.getTime?.() || 0))
        .slice(0, 6)
)

async function refresh() {
    loading.value = true
    error.value = ''
    try {
        await Promise.all([loadOrders(), loadQuotes(), loadVendors(), loadTypes()])
        const d = new Date()
        lastUpdated.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    } catch (err) {
        error.value = err?.message || 'Unknown error'
    } finally {
        loading.value = false
    }
}

function exportCsv() {
    const rows = [
        ['Metric', 'Value'],
        ['Total Orders', totalOrders.value],
        ['Open Requisitions', openReqs.value],
        ['Pending Receiving', pendingReceiving.value],
        ['Budget Spent (USD)', spent.value],
        ['Vendors', vendorCount.value],
        ['Average Order Value', Math.round(avgOrderValue.value)],
        ['', ''],
        ['Status', 'Count'],
        ['Under Review', countBy('under-review')],
        ['Procuring', countBy('procuring')],
        ['Arrived', countBy('arrived')],
        ['Terminated', countBy('terminated')],
    ]
    const csv = rows.map(r => r.join(',')).join('\n')
    const blob = new Blob([csv], {type: 'text/csv;charset=utf-8;'})
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `proc_dashboard_${Date.now()}.csv`
    a.click()
    URL.revokeObjectURL(url)
}

onMounted(refresh)
</script>

<style scoped>
/* layout & buttons */
.page {
    padding: 16px;
}

.card {
    background: #fff;
    border: 1px solid #eee;
    border-radius: 12px;
}

.p16 {
    padding: 16px;
}

.header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
}

.title .h1 {
    font-size: 18px;
    font-weight: 700;
}

.title .sub {
    color: #6B7280;
    font-size: 13px;
}

.actions {
    display: flex;
    gap: 8px;
}

.btn {
    padding: 8px 12px;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
    background: #111827;
    color: #fff;
    font-size: 13px;
}

.btn.ghost {
    background: #fff;
    color: #111827;
}

.btn:disabled {
    opacity: .6;
    cursor: not-allowed;
}

/* grids */
.grid {
    display: grid;
    gap: 16px;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.stats-grid {
    margin-top: 16px;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 16px;
}

.stat-card {
    padding: 8px;
}

.chart-title {
    font-weight: 700;
    margin: 12px 12px 0;
    font-size: 14px;
}

/* KPIs */
.kpis {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 16px;
    margin-top: 16px;
}

.kpi {
    background: #fff;
    border: 1px solid #eee;
    border-radius: 12px;
    padding: 12px;
}

.kpi-title {
    font-size: 13px;
    color: #6B7280;
}

.kpi-value {
    font-size: 22px;
    font-weight: 700;
    margin: 6px 0;
}

.kpi-desc {
    font-size: 12px;
    color: #6B7280;
    margin-top: 6px;
}

.progress {
    height: 8px;
    background: #F3F4F6;
    border-radius: 999px;
    overflow: hidden;
}

.bar {
    height: 100%;
    background: #2563EB;
}

.bar.success {
    background: #10B981;
}

/* skeleton & error */
.skeleton {
    height: 84px;
    background: linear-gradient(90deg, #f3f4f6 25%, #eceff3 37%, #f3f4f6 63%);
    border-radius: 12px;
    animation: shimmer 1.2s infinite;
}

@keyframes shimmer {
    0% {
        background-position: -200px 0;
    }
    100% {
        background-position: calc(200px + 100%) 0;
    }
}

.error {
    padding: 16px;
    color: #B91C1C;
}

/* misc */
.subtitle {
    font-weight: 600;
    margin-bottom: 8px;
}

.note {
    color: #374151;
    line-height: 1.6;
}

.timestamp {
    color: #6B7280;
    font-size: 12px;
    margin-top: 10px;
}
</style>
