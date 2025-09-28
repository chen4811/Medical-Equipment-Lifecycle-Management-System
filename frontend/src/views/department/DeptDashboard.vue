<template>
  <div class="page">
    <!-- Top Section: Title + Actions -->
    <div class="page-header">
      <div class="title">
        <h2>Department Dashboard</h2>
        <p class="muted">
          Department ID:
          <span v-if="departmentId">{{ departmentId }}</span>
          <span v-else>Not Available</span>
          <span v-if="lastUpdated" class="dot">•</span>
          <span v-if="lastUpdated">Last Updated: {{ formatTime(lastUpdated) }}</span>
        </p>
      </div>
      <div class="actions">
        <button class="btn" :disabled="loading" @click="refresh">
          {{ loading ? 'Refreshing…' : 'Refresh Data' }}
        </button>
      </div>
    </div>

    <!-- Error Message -->
    <div v-if="error" class="alert">
      <strong>Failed to load:</strong>{{ error }}
    </div>

    <!-- Skeleton Loading -->
    <div v-if="loading && !hasAnyData" class="skeleton-grid">
      <div class="skeleton-card" v-for="i in 4" :key="i"></div>
    </div>

    <!-- KPI Grid -->
    <div v-else class="card" style="padding:16px;">
      <div class="grid">
        <StatCard label="In-Use Devices" :value="inUse" />
        <StatCard label="Devices Under Repair" :value="underRepair" />
        <StatCard label="Today Usage Logs" :value="todosUsage" />
        <StatCard label="Unfinished Repairments" :value="todosRepair" />
      </div>
    </div>

    <!-- Derived Metrics -->
    <div class="grid two">
      <div class="card padded">
        <h3>Usage & Repair Overview</h3>
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

        <!-- Progress Bar: In Use vs. Repair -->
        <div class="progress">
          <div
              class="progress-inuse"
              :style="{ width: inUseBar + '%' }"
              :title="`In Use ${inUse}`"
          ></div>
          <div
              class="progress-repair"
              :style="{ width: underRepairBar + '%' }"
              :title="`Under Repair ${underRepair}`"
          ></div>
        </div>

        <div class="legend">
          <span class="badge inuse"></span> In Use {{ inUse }}
          <span class="spacer"></span>
          <span class="badge repair"></span> Under Repair {{ underRepair }}
        </div>
      </div>

      <div class="card padded">
        <h3>Todo Overview (Today)</h3>
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

        <!-- Todo Progress Bar -->
        <div class="progress thin">
          <div
              class="progress-usage"
              :style="{ width: usageTodoBar + '%' }"
              :title="`Usage Todos ${todosUsage}`"
          ></div>
          <div
              class="progress-repair"
              :style="{ width: repairTodoBar + '%' }"
              :title="`Repair Todos ${todosRepair}`"
          ></div>
        </div>

        <div class="legend">
          <span class="badge usage"></span> Usage Todos {{ todosUsage }}
          <span class="spacer"></span>
          <span class="badge repair"></span> Repair Todos {{ todosRepair }}
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import StatCard from '@/components/layout/StatCard.vue'
import { ref, onMounted, computed } from 'vue'
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
    new Date(date).toLocaleString(undefined, { hour12: false })

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
  const res = await axios.get('/req/department/id', { params: { accountId: id } })
  return res.data
}

const getStats = async (deptId) => {
  const res = await axios.get('/req/dept/dashboard/stats', {
    params: { departmentId: deptId },
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
.page {
  display: grid;
  gap: 16px;
}

/* Top Section */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}
.title h2 {
  margin: 0 0 4px 0;
  font-size: 20px;
}
.muted {
  color: #6b7280;
  padding-top: 5px;
  padding-bottom: 5px;
  font-style: italic;
}
.muted.small {
  font-size: 12px;
}
.dot {
  margin: 0 6px;
}

/* Card Styling */
.card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}
.padded {
  padding: 16px;
}

/* Grid */
.grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}
.grid.two {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

/* Progress Bar */
.progress {
  height: 12px;
  border-radius: 999px;
  background: #f3f4f6;
  overflow: hidden;
  margin: 20px 0 20px;
  display: flex;
}
.progress.thin { height: 10px; }
.progress-inuse { background: #3b82f6; }
.progress-repair { background: #ef4444; }
.progress-usage { background: #10b981; }

/* Legend */
.legend {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #6b7280;
}
.badge {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
  margin-right: 6px;
}
.badge.inuse { background: #3b82f6; }
.badge.repair { background: #ef4444; }
.badge.usage { background: #10b981; }
.spacer { flex: 1; }

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
.todo.total { background: #fff7ed; border-color: #fed7aa; }
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
.btn:hover { background: #f9fafb; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
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
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
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
