<template>
    <div class="dashboard">
        <!-- 顶部指标卡 -->
        <div class="grid">
            <StatCard label="Open Requisitions" :value="openReqs" />
            <StatCard label="Pending Receiving" :value="pendingReceiving" />
            <StatCard label="Budget Spent (USD)" :value="spent" />
            <StatCard label="Vendors" :value="vendorCount" />
        </div>

        <!-- 图表区：对齐 Admin 的风格 -->
        <div class="stats-grid">
            <div class="card stat-card">
                <div class="chart-title">Orders by Status</div>
                <apexchart height="220" type="donut"
                           :options="cOrdersByStatus.options"
                           :series="cOrdersByStatus.series" />
            </div>

            <div class="card stat-card">
                <div class="chart-title">Spend by Supplier (Top 5)</div>
                <apexchart height="220" type="bar"
                           :options="cSpendBySupplier.options"
                           :series="cSpendBySupplier.series" />
            </div>

            <div class="card stat-card">
                <div class="chart-title">Units by Equipment</div>
                <apexchart height="220" type="bar"
                           :options="cUnitsByType.options"
                           :series="cUnitsByType.series" />
            </div>
        </div>

        <!-- 最近订单 -->
        <div class="card" style="margin-top:16px; padding:16px;">
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
        </div>
    </div>
</template>

<script setup>
import StatCard from '@/components/layout/StatCard.vue'
import { ref, computed, onMounted } from 'vue'

// 后端：/req/proc/orders  /req/proc/quotes  /req/proc/vendors  /req/proc/equipmentTypes
const orders = ref([])
const quotes = ref([])
const vendors = ref([])
const types = ref([]) // 设备类型字典

const STATUS_LABEL = {
    'under-review': 'Under Review',
    'procuring': 'Procuring',
    'arrived': 'Arrived',
    'terminated': 'Terminated',
}

function statusLabel(s) { return STATUS_LABEL[s] || s || '-' }
function fmtMoney(n) { return `$${Number(n || 0).toLocaleString()}` }

async function loadOrders() {
    const r = await fetch('/req/proc/orders'); const j = await r.json()
    orders.value = j.code === '000'
        ? (j.data || []).map(x => ({
            id: Number(x.procure_id ?? x.id ?? 0),
            supplierId: String(x.supplier_id ?? x.supplierId ?? ''),
            typeId: String(x.equipment_type_id ?? x.equipmentTypeId ?? ''),
            count: Number(x.count ?? 0),
            status: String(x.status ?? 'under-review'),
        }))
        : []
}

async function loadQuotes() {
    const r = await fetch('/req/proc/quotes'); const j = await r.json()
    quotes.value = j.code === '000'
        ? (j.data || []).map(x => ({
            supplierId: String(x.supplier_id ?? x.supplierId ?? ''),
            typeId: String(x.equipment_type_id ?? x.equipmentTypeId ?? ''),
            price: Number(x.price ?? 0),
        }))
        : []
}

async function loadVendors() {
    const r = await fetch('/req/proc/vendors'); const j = await r.json()
    vendors.value = j.code === '000'
        ? (j.data || []).map(x => ({
            id: String(x.supplier_id ?? x.id ?? ''),
            name: String(x.supplier_name ?? x.name ?? x.id ?? '-'),
        }))
        : []
}

async function loadTypes() {
    const r = await fetch('/req/proc/equipmentTypes'); const j = await r.json()
    types.value = j.code === '000'
        ? (j.data || []).map(x => ({
            id: String(x.equipment_type_id ?? x.id ?? ''),
            name: String(x.equipment_type_name ?? x.name ?? x.id ?? ''),
        }))
        : []
}

function supplierName(id) { return vendors.value.find(v => v.id === id)?.name || id || '-' }
function typeName(id) { return types.value.find(t => t.id === id)?.name || id || '-' }
function unitPriceOf(supplierId, typeId) {
    const q = quotes.value.find(q => q.supplierId === supplierId && q.typeId === typeId)
    return q ? Number(q.price || 0) : 0
}
function orderAmount(o) { return unitPriceOf(o.supplierId, o.typeId) * (o.count || 0) }

// 顶部指标
const openReqs = computed(() => orders.value.filter(o => o.status === 'under-review').length)
const pendingReceiving = computed(() => orders.value.filter(o => o.status === 'arrived').length)
const spent = computed(() =>
    orders.value
        .filter(o => o.status !== 'terminated')
        .reduce((sum, o) => sum + orderAmount(o), 0)
)
const vendorCount = computed(() => vendors.value.length)

// 图表：订单状态分布
const cOrdersByStatus = computed(() => {
    const keys = ['under-review','procuring','arrived','terminated']
    const series = keys.map(k => orders.value.filter(o => o.status === k).length)
    return {
        series,
        options: {
            labels: keys.map(k => STATUS_LABEL[k]),
            legend: { position: 'bottom' },
            dataLabels: { enabled: true },
        }
    }
})

// 图表：按供应商支出 Top 5
const cSpendBySupplier = computed(() => {
    const map = new Map()
    for (const o of orders.value) {
        if (o.status === 'terminated') continue
        const key = o.supplierId || '-'
        map.set(key, (map.get(key) || 0) + orderAmount(o))
    }
    const rows = Array.from(map.entries())
        .sort((a,b) => b[1] - a[1])
        .slice(0, 5)
    return {
        series: [{ name: 'Amount', data: rows.map(([,v]) => Math.round(v)) }],
        options: {
            chart: { toolbar: { show: false } },
            plotOptions: { bar: { columnWidth: '55%' } },
            dataLabels: { enabled: true },
            xaxis: { categories: rows.map(([k]) => supplierName(k)) }
        }
    }
})

// 图表：按设备类型的数量
const cUnitsByType = computed(() => {
    const map = new Map()
    for (const o of orders.value) {
        const key = o.typeId || '-'
        map.set(key, (map.get(key) || 0) + (o.count || 0))
    }
    const rows = Array.from(map.entries()).sort((a,b) => b[1]-a[1]).slice(0, 8)
    return {
        series: [{ name: 'Units', data: rows.map(([,v]) => v) }],
        options: {
            chart: { toolbar: { show: false } },
            plotOptions: { bar: { columnWidth: '55%' } },
            dataLabels: { enabled: true },
            xaxis: { categories: rows.map(([k]) => typeName(k)) }
        }
    }
})

// 最近订单（按 id 倒序取前 6 条）
const recentOrders = computed(() =>
    [...orders.value].sort((a,b) => (b.id ?? 0) - (a.id ?? 0)).slice(0, 6)
)

onMounted(async () => {
    await Promise.all([loadOrders(), loadQuotes(), loadVendors(), loadTypes()])
})
</script>

<style scoped>
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
.stat-card { padding: 8px; }
.chart-title { font-weight: 700; margin: 12px 12px 0; font-size: 14px; }
</style>
