<template>
    <div class="page">
        <div class="card p16">
            <header class="header">
                <div class="title">
                    <div class="h1">Device Operations Overview</div>
                    <div class="sub">Equipment Manager Dashboard</div>
                </div>
                <div class="actions">
                    <button class="btn" @click="loadDashboard" :disabled="loading">
                        Refresh
                    </button>
                    <button class="btn ghost" @click="exportCsv" :disabled="loading">
                        Export CSV
                    </button>
                </div>
            </header>

            <!-- Loading Skeleton -->
            <div v-if="loading" class="grid">
                <div class="skeleton" v-for="i in 7" :key="i"></div>
            </div>

            <!-- Error State -->
            <div v-else-if="error" class="error">
                <div>Failed to load: {{ error }}</div>
                <button class="btn" @click="loadDashboard">Retry</button>
            </div>

            <!-- Normal Content -->
            <template v-else>
                <div class="grid">
                    <StatCard label="Devices: Unassigned" :value="overview.unassigned"/>
                    <StatCard label="Devices: In Use" :value="overview.inUse"/>
                    <StatCard label="Devices: Under Repair" :value="overview.underRepair"/>
                    <StatCard label="Devices: Pending Scrap" :value="overview.pendingScrap"/>
                    <StatCard label="Today: Pending Response" :value="today.pendingResponse"/>
                    <StatCard label="Today: In Progress" :value="today.inProgress"/>
                    <StatCard label="Today: Pending Acceptance" :value="today.pendingAcceptance"/>
                </div>

                <!-- Derived Metrics -->
                <div class="kpis">
                    <div class="kpi">
                        <div class="kpi-title">Total Devices</div>
                        <div class="kpi-value">{{ fmt(totalDevices) }}</div>
                        <div class="kpi-desc">= Unassigned + In Use + Under Repair + Pending Scrap</div>
                    </div>
                    <div class="kpi">
                        <div class="kpi-title">Utilization Rate</div>
                        <div class="kpi-value">{{ (utilizationRate * 100).toFixed(1) }}%</div>
                        <div class="progress">
                            <div class="bar" :style="{ width: (utilizationRate * 100) + '%' }"></div>
                        </div>
                        <div class="kpi-desc">In Use / Total Devices</div>
                    </div>
                    <div class="kpi">
                        <div class="kpi-title">Repair Rate</div>
                        <div class="kpi-value">{{ (repairRate * 100).toFixed(1) }}%</div>
                        <div class="progress">
                            <div class="bar warn" :style="{ width: (repairRate * 100) + '%' }"></div>
                        </div>
                        <div class="kpi-desc">Under Repair / Total Devices</div>
                    </div>
                    <div class="kpi">
                        <div class="kpi-title">Pending Scrap Ratio</div>
                        <div class="kpi-value">{{ (scrapRate * 100).toFixed(1) }}%</div>
                        <div class="progress">
                            <div class="bar danger" :style="{ width: (scrapRate * 100) + '%' }"></div>
                        </div>
                        <div class="kpi-desc">Pending Scrap / Total Devices</div>
                    </div>
                </div>

                <!-- Visualizations -->
                <div class="viz">
                    <div class="card p16">
                        <div class="subtitle">Device Status Distribution</div>
                        <DonutChart
                            :segments="deviceSegments"
                            :total="totalDevices"
                            aria-label="Device status distribution donut chart"
                        />
                        <ul class="legend">
                            <li v-for="s in deviceSegments" :key="s.label">
                                <span class="dot" :style="{ background: s.color }"></span>
                                {{ s.label }}: {{ fmt(s.value) }} ({{ ((s.value / totalDevices) * 100).toFixed(1) }}%)
                            </li>
                        </ul>
                    </div>

                    <div class="card p16">
                        <div class="subtitle">Today's Ticket Progress</div>
                        <StackedBar
                            :parts="ticketParts"
                            :total="todayTotal"
                            aria-label="Today's ticket stacked bar chart"
                        />
                        <ul class="legend">
                            <li v-for="p in ticketParts" :key="p.label">
                                <span class="dot" :style="{ background: p.color }"></span>
                                {{ p.label }}: {{ fmt(p.value) }}
                                ({{ todayTotal ? ((p.value / todayTotal) * 100).toFixed(1) : 0 }}%)
                            </li>
                        </ul>
                    </div>
                </div>

                <!-- Detailed Table -->
                <div class="card p16">
                    <div class="subtitle">Detailed Data</div>
                    <table class="table">
                        <thead>
                        <tr>
                            <th>Category</th>
                            <th class="right">Quantity</th>
                            <th class="right">Proportion</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr v-for="row in detailRows" :key="row.key">
                            <td>{{ row.name }}</td>
                            <td class="right">{{ fmt(row.value) }}</td>
                            <td class="right">{{ row.ratio }}</td>
                        </tr>
                        <tr>
                            <td><b>Total (Devices)</b></td>
                            <td class="right"><b>{{ fmt(totalDevices) }}</b></td>
                            <td class="right">100.0%</td>
                        </tr>
                        <tr>
                            <td><b>Total (Today's Tickets)</b></td>
                            <td class="right"><b>{{ fmt(todayTotal) }}</b></td>
                            <td class="right">100.0%</td>
                        </tr>
                        </tbody>
                    </table>
                </div>

                <!-- Health Summary -->
                <div class="card p16">
                    <div class="subtitle">Operation Health Summary</div>
                    <p class="note">
                        Currently, there are <b>{{ fmt(totalDevices) }}</b> devices, of which <b>{{
                            fmt(overview.inUse)
                        }}</b> are in use (utilization rate
                        <b>{{ (utilizationRate * 100).toFixed(1) }}%</b>). <b>{{ fmt(overview.underRepair) }}</b>
                        devices are under repair,
                        and <b>{{ fmt(overview.pendingScrap) }}</b> devices are pending scrap. Today, there are a total
                        of <b>{{ fmt(todayTotal) }}</b> tickets,
                        with <b>{{ fmt(today.inProgress) }}</b> in progress, <b>{{ fmt(today.pendingResponse) }}</b>
                        awaiting response,
                        and <b>{{ fmt(today.pendingAcceptance) }}</b> pending acceptance.
                    </p>
                    <p class="timestamp">Last updated: {{ lastUpdated }}</p>
                </div>
            </template>
        </div>
    </div>
</template>

<script setup>
import {reactive, ref, computed, onMounted} from 'vue'
import axios from 'axios'
import StatCard from '@/components/layout/StatCard.vue'

/** ---------- States ---------- */
const loading = ref(false)
const error = ref('')
const lastUpdated = ref('')

const overview = reactive({
    unassigned: 0,
    inUse: 0,
    underRepair: 0,
    pendingScrap: 0,
})

const today = reactive({
    pendingResponse: 0,
    inProgress: 0,
    pendingAcceptance: 0,
})

/** ---------- Derived Computations ---------- */
const totalDevices = computed(() =>
    overview.unassigned + overview.inUse + overview.underRepair + overview.pendingScrap
)
const todayTotal = computed(() =>
    today.pendingResponse + today.inProgress + today.pendingAcceptance
)

const utilizationRate = computed(() =>
    totalDevices.value ? overview.inUse / totalDevices.value : 0
)
const repairRate = computed(() =>
    totalDevices.value ? overview.underRepair / totalDevices.value : 0
)
const scrapRate = computed(() =>
    totalDevices.value ? overview.pendingScrap / totalDevices.value : 0
)

const deviceSegments = computed(() => ([
    {label: 'Unassigned', value: overview.unassigned, color: '#6B7280'},
    {label: 'In Use', value: overview.inUse, color: '#2563EB'},
    {label: 'Under Repair', value: overview.underRepair, color: '#F59E0B'},
    {label: 'Pending Scrap', value: overview.pendingScrap, color: '#DC2626'},
]))

const ticketParts = computed(() => ([
    {label: 'Pending Response', value: today.pendingResponse, color: '#6B7280'},
    {label: 'In Progress', value: today.inProgress, color: '#10B981'},
    {label: 'Pending Acceptance', value: today.pendingAcceptance, color: '#2563EB'},
]))

const detailRows = computed(() => {
    const dev = deviceSegments.value.map(s => ({
        key: 'dev-' + s.label,
        name: 'Devices · ' + s.label,
        value: s.value,
        ratio: totalDevices.value ? ((s.value / totalDevices.value) * 100).toFixed(1) + '%' : '0.0%'
    }))
    const tk = ticketParts.value.map(p => ({
        key: 'tk-' + p.label,
        name: 'Today Tickets · ' + p.label,
        value: p.value,
        ratio: todayTotal.value ? ((p.value / todayTotal.value) * 100).toFixed(1) + '%' : '0.0%'
    }))
    return [...dev, ...tk]
})

/** ---------- Helper Functions ---------- */
const fmt = (n) => new Intl.NumberFormat().format(n)

/** ---------- Actions ---------- */
async function loadDashboard() {
    loading.value = true
    error.value = ''
    try {
        const res = await axios.get('/req/dashboard/overview')
        Object.assign(overview, res.data.equipmentCounts)
        Object.assign(today, res.data.todayTickets)
        lastUpdated.value = new Date().toLocaleString()
    } catch (err) {
        console.error('Failed to load dashboard data', err)
        error.value = err?.message || 'Unknown error'
    } finally {
        loading.value = false
    }
}

function exportCsv() {
    // Export existing data, no need to request again
    const rows = [
        ['Category', 'Key', 'Value'],
        ['Devices', 'Unassigned', overview.unassigned],
        ['Devices', 'In Use', overview.inUse],
        ['Devices', 'Under Repair', overview.underRepair],
        ['Devices', 'Pending Scrap', overview.pendingScrap],
        ['Today Tickets', 'Pending Response', today.pendingResponse],
        ['Today Tickets', 'In Progress', today.inProgress],
        ['Today Tickets', 'Pending Acceptance', today.pendingAcceptance],
    ]
    const csv = rows.map(r => r.join(',')).join('\n')
    const blob = new Blob([csv], {type: 'text/csv;charset=utf-8;'})
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `dashboard_${Date.now()}.csv`
    a.click()
    URL.revokeObjectURL(url)
}

onMounted(loadDashboard)

/** ---------- Lightweight Visual Components ---------- */
const DonutChart = {
    name: 'DonutChart',
    props: {
        segments: {type: Array, required: true},
        total: {type: Number, required: true},
        size: {type: Number, default: 160},
        thickness: {type: Number, default: 20}
    },
    computed: {
        arcs() {
            const sum = this.total || 1
            const circumference = Math.PI * (this.size - this.thickness)
            let offset = 0
            return this.segments.map(s => {
                const len = circumference * (s.value / sum)
                const arc = {color: s.color, dash: `${len} ${circumference - len}`, offset}
                offset += len
                return arc
            })
        }
    },
    template: `
        <div class="donut-wrap">
            <svg :width="size" :height="size" :viewBox="['0 0', size, size].join(' ')">
                <g :transform="'rotate(-90 '+ size/2 +' '+ size/2 +')'">
                    <circle
                        :cx="size/2" :cy="size/2" :r="(size - thickness)/2"
                        fill="none" stroke="#EEE" :stroke-width="thickness"/>
                    <circle
                        v-for="(a, i) in arcs" :key="i"
                        :cx="size/2" :cy="size/2" :r="(size - thickness)/2"
                        fill="none" :stroke="a.color" :stroke-width="thickness"
                        stroke-linecap="butt"
                        :stroke-dasharray="a.dash"
                        :stroke-dashoffset="-a.offset"
                    />
                </g>
                <text :x="size/2" :y="size/2" text-anchor="middle" dominant-baseline="central" class="donut-text">
                    {{ total }}
                </text>
            </svg>
        </div>
    `
}

const StackedBar = {
    name: 'StackedBar',
    props: {
        parts: {type: Array, required: true}, // [{label, value, color}]
        total: {type: Number, required: true},
        height: {type: Number, default: 18}
    },
    computed: {
        widths() {
            const t = this.total || 1
            return this.parts.map(p => ({
                color: p.color,
                width: (p.value / t) * 100
            }))
        }
    },
    template: `
        <div class="stacked" :style="{ height: height + 'px' }" role="img">
            <div
                v-for="(w, i) in widths"
                :key="i"
                class="stacked-part"
                :style="{ width: w.width + '%', background: w.color }"
            />
        </div>
    `
}
</script>

<style scoped>
/* layout */
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

.grid {
    display: grid;
    gap: 16px;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

/* kpis */
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

.bar.warn {
    background: #F59E0B;
}

.bar.danger {
    background: #DC2626;
}

/* viz */
.viz {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 16px;
    margin-top: 16px;
}

.subtitle {
    font-weight: 600;
    margin-bottom: 8px;
}

.legend {
    display: flex;
    flex-direction: column;
    gap: 6px;
    margin-top: 10px;
    font-size: 13px;
    color: #374151;
}

.legend .dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    display: inline-block;
    margin-right: 6px;
    vertical-align: middle;
}

/* donut */
.donut-wrap {
    display: flex;
    justify-content: center;
    align-items: center;
}

.donut-text {
    font-size: 14px;
    fill: #111827;
}

/* table */
.table {
    width: 100%;
    border-collapse: collapse;
    font-size: 14px;
}

.table th, .table td {
    padding: 8px 10px;
    border-bottom: 1px solid #f0f0f0;
}

.table th {
    text-align: left;
    color: #6B7280;
    font-weight: 500;
}

.table .right {
    text-align: right;
}

/* states */
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
.note {
    color: #374151;
    line-height: 1.6;
}

.timestamp {
    color: #6B7280;
    font-size: 12px;
    margin-top: 6px;
}
</style>
