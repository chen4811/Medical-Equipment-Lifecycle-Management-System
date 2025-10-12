<template>
    <div class="page">
        <div class="card p16">
            <header class="header">
                <div>
                    <div class="title-lg">Department Overview</div>
                    <div class="subtitle" style="margin-top:8px;">Department Dashboard</div>
                </div>
                <div class="actions">
                    <button class="btn" :disabled="loading" @click="refresh">
                        {{ loading ? 'Refreshing…' : 'Refresh Data' }}
                    </button>
                </div>
            </header>

            <!-- Error -->
            <div v-if="error" class="alert">
                <strong>Failed to load:</strong>{{ error }}
            </div>

            <!-- Skeleton -->
            <div v-else-if="loading && !hasAnyData" class="skeleton-grid">
                <div class="skeleton-card" v-for="i in 4" :key="i"></div>
            </div>

            <!-- 内容块：与其它角色 Dashboard 保持一致的结构与间距 -->
            <template v-else>
                <!-- KPI -->
                <div class="grid">
                    <StatCard label="In-Use Devices" :value="inUse"/>
                    <StatCard label="Devices Under Repair" :value="underRepair"/>
                    <StatCard label="Today Usage Logs" :value="todosUsage"/>
                    <StatCard label="Unfinished Repairments" :value="todosRepair"/>
                </div>

                <!-- 二列区块：与采购/设备页相同的自适应宽度 -->
                <div class="stats-grid">
                    <div class="card p16">
                        <div class="subtitle">Usage & Repair Overview</div>
                        <p class="muted">Collect from in-use and under-repair data</p>

                        <div class="metric-row">
                            <div class="metric">
                                <div class="metric-label">Total Devices (Estimated)</div>
                                <div class="metric-value">{{ totalDevices }}</div>
                            </div>
                            <div class="metric">
                                <div class="metric-label">Repair Ratio</div>
                                <div class="metric-value">{{ (repairRatio * 100).toFixed(1) }}%</div>
                            </div>
                        </div>

                        <div class="progress">
                            <div class="progress-inuse" :style="{ width: inUseBar + '%' }" :title="`In Use ${inUse}`"></div>
                            <div class="progress-repair" :style="{ width: underRepairBar + '%' }" :title="`Under Repair ${underRepair}`"></div>
                        </div>

                        <div class="legend between">
                            <div><span class="badge inuse"></span> In Use {{ inUse }}</div>
                            <div><span class="badge repair"></span> Under Repair {{ underRepair }}</div>
                        </div>
                    </div>

                    <div class="card p16">
                        <div class="subtitle">Todo Overview (Today)</div>
                        <p class="muted">View today log from current stats</p>

                        <div class="todo-grid">
                            <div class="todo">
                                <div class="todo-label">Usage Logs Pending</div>
                                <div class="todo-value">{{ todosUsage }}</div>
                            </div>
                            <div class="todo">
                                <div class="todo-label">Repairs Pending</div>
                                <div class="todo-value">{{ todosRepair }}</div>
                            </div>
                            <div class="todo total">
                                <div class="todo-label">Total Todos</div>
                                <div class="todo-value">{{ totalTodos }}</div>
                            </div>
                        </div>

                        <div class="progress thin">
                            <div class="progress-usage" :style="{ width: usageTodoBar + '%' }" :title="`Usage Todos ${todosUsage}`"></div>
                            <div class="progress-repair" :style="{ width: repairTodoBar + '%' }" :title="`Repair Todos ${todosRepair}`"></div>
                        </div>

                        <div class="legend between">
                            <div><span class="badge usage"></span> Usage Todos {{ todosUsage }}</div>
                            <div><span class="badge repair"></span> Repair Todos {{ todosRepair }}</div>
                        </div>
                    </div>
                </div>
            </template>
        </div>

    </div>
</template>

<script setup>
import StatCard from '@/components/layout/StatCard.vue'
import {ref, onMounted, computed} from 'vue'
import axios from 'axios'

/** -------------------------
 *  State Management
 *  ------------------------- */
const inUse = ref(0)
const underRepair = ref(0)
const todosUsage = ref(0)
const todosRepair = ref(0)

const departmentId = ref(null)
const accountId = ref(Number(localStorage.getItem('account_id')))

const loading = ref(false)
const error = ref('')
const lastUpdated = ref(null)

/** -------------------------
 *  Utility Functions
 *  ------------------------- */
const safeNumber = (v) => (Number.isFinite(v) && v >= 0 ? v : 0)
const formatTime = (date) =>
    new Date(date).toLocaleString(undefined, {hour12: false})

/** -------------------------
 *  Derived Data (No additional requests)
 *  ------------------------- */
const totalDevices = computed(() => {
    // Only uses inUse + underRepair for an "estimate" of total devices
    return safeNumber(inUse.value) + safeNumber(underRepair.value)
})

const repairRatio = computed(() => {
    const total = totalDevices.value
    if (!total) return 0
    return safeNumber(underRepair.value) / total
})

// Progress Bar Ratios (In Use / Under Repair)
const inUseBar = computed(() => {
    const total = totalDevices.value
    if (!total) return 0
    return (safeNumber(inUse.value) / total) * 100
})
const underRepairBar = computed(() => {
    const total = totalDevices.value
    if (!total) return 0
    return (safeNumber(underRepair.value) / total) * 100
})

// Todo Statistics
const totalTodos = computed(() => {
    return safeNumber(todosUsage.value) + safeNumber(todosRepair.value)
})
const usageTodoBar = computed(() => {
    const total = totalTodos.value
    if (!total) return 0
    return (safeNumber(todosUsage.value) / total) * 100
})
const repairTodoBar = computed(() => {
    const total = totalTodos.value
    if (!total) return 0
    return (safeNumber(todosRepair.value) / total) * 100
})

const hasAnyData = computed(() =>
    [inUse.value, underRepair.value, todosUsage.value, todosRepair.value].some(
        (v) => Number.isFinite(v) && v > 0
    )
)

/** -------------------------
 *  Data Fetching
 *  ------------------------- */
const getDepartmentId = async (accId) => {
    const id = Number(accId)
    if (!Number.isFinite(id)) return null
    const res = await axios.get('/req/department/id', {params: {accountId: id}})
    return res.data
}

const getStats = async (deptId) => {
    const res = await axios.get('/req/dept/dashboard/stats', {
        params: {departmentId: deptId},
    })
    return res.data
}

const refresh = async () => {
    loading.value = true
    error.value = ''
    try {
        if (!departmentId.value) {
            departmentId.value = await getDepartmentId(accountId.value)
        }
        const stats = await getStats(departmentId.value)
        inUse.value = safeNumber(stats.inUse)
        underRepair.value = safeNumber(stats.underRepair)
        todosUsage.value = safeNumber(stats.todosUsage)
        todosRepair.value = safeNumber(stats.todosRepair)
        lastUpdated.value = Date.now()
    } catch (e) {
        console.error(e)
        error.value = e?.response?.data?.message || e?.message || 'Unknown error'
    } finally {
        loading.value = false
    }
}

/** -------------------------
 *  Lifecycle
 *  ------------------------- */
onMounted(async () => {
    await refresh()
})
</script>

<style scoped>
.page { padding: 0; }

/* Header 与 buttons 统一 */
.header { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:12px; }

/* Card 在全局 theme.css 中已统一，这里仅控制内边距 */
.p16 { padding:16px; }

/* Grid */
.grid {
    display: grid;
    gap: 16px;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

/* 与采购/设备页一致的二列自适应网格 */
.stats-grid {
    margin-top: 16px;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
    gap: 16px;
}

/* Progress Bar */
.progress {
    height: 10px;
    border-radius: 999px;
    background: #f3f4f6;
    overflow: hidden;
    margin: 12px 0 10px;
    display: flex;
}

.progress.thin {
    height: 10px;
}

.progress-inuse {
    background: #3b82f6;
}

.progress-repair {
    background: #ef4444;
}

.progress-usage {
    background: #10b981;
}

/* Legend */
.legend {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: #6b7280;
}

.legend.between { justify-content: space-between; }

.badge {
    width: 10px;
    height: 10px;
    border-radius: 2px;
    display: inline-block;
    margin-right: 6px;
}

.badge.inuse {
    background: #3b82f6;
}

.badge.repair {
    background: #ef4444;
}

.badge.usage {
    background: #10b981;
}

/* 统一移除 spacer 方案，改用 .legend.between */

/* Metric Row */
.metric-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 12px;
    margin: 8px 0 4px;
}

.metric {
    background: #f9fafb;
    border: 1px solid #f3f4f6;
    border-radius: 10px;
    padding: 12px;
}

.metric-label {
    font-size: 12px;
    color: #6b7280;
}

.metric-value {
    font-size: 22px;
    font-weight: 700;
}

/* Todo Section */
.todo-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 12px;
    margin: 8px 0 12px;
}

.todo {
    background: #f9fafb;
    border: 1px solid #f3f4f6;
    border-radius: 10px;
    padding: 12px;
}

.todo.total {
    background: #fff7ed;
    border-color: #fed7aa;
}

.todo-label {
    font-size: 12px;
    color: #6b7280;
}

.todo-value {
    margin-top: 6px;
    font-size: 20px;
    font-weight: 700;
}

/* Buttons */
.actions, .quick-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.btn {
    border: 1px solid #e5e7eb;
    background: #fff;
    padding: 8px 12px;
    border-radius: 10px;
    cursor: pointer;
}

.btn:hover {
    background: #f9fafb;
}

.btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.btn.ghost {
    background: #ffffff;
}

/* Skeleton */
.skeleton-grid {
    display: grid;
    gap: 16px;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.skeleton-card {
    height: 92px;
    border-radius: 12px;
    background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 37%, #f3f4f6 63%);
    background-size: 400% 100%;
    animation: shimmer 1.4s ease infinite;
}

@keyframes shimmer {
    0% {
        background-position: 100% 0;
    }
    100% {
        background-position: -100% 0;
    }
}

/* Alert */
.alert {
    border: 1px solid #fecaca;
    background: #fef2f2;
    color: #b91c1c;
    padding: 10px 12px;
    border-radius: 10px;
}
</style>
