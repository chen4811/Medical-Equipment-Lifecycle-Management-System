<template>
    <div class="page">
        <div class="card p16">
            <header class="header">
                <div>
                    <div class="title-lg">Admin Overview</div>
                    <div class="subtitle" style="margin-top:8px;">System Dashboard</div>
                </div>
            </header>

            <div v-if="error" class="error">{{ error }}</div>

            <template v-else>
                <div class="grid">
                    <StatCard label="Total Departments" :value="overall.departmentCount"/>
                    <StatCard label="Total Employees" :value="overall.employeeCount"/>
                    <StatCard label="Total Tickets" :value="overall.ticketCount"/>
                </div>

                <div class="stats-grid">
                    <div class="card p16">
                        <div class="subtitle">Employees by Department</div>
                        <div v-if="loading" class="skeleton" style="height:220px; border-radius:12px;"></div>
                        <apexchart v-else height="220" type="bar" :options="charts.deptBar.options" :series="charts.deptBar.series"/>
                    </div>
                    <div class="card p16">
                        <div class="subtitle">Users by Role</div>
                        <div v-if="loading" class="skeleton" style="height:220px; border-radius:12px;"></div>
                        <apexchart v-else height="220" type="donut" :options="charts.rolesPie.options" :series="charts.rolesPie.series"/>
                    </div>
                    <div class="card p16">
                        <div class="subtitle">Tickets by Category</div>
                        <div v-if="loading" class="skeleton" style="height:220px; border-radius:12px;"></div>
                        <apexchart v-else height="220" type="bar" :options="charts.ticketsBar.options" :series="charts.ticketsBar.series"/>
                    </div>
                </div>
            </template>
        </div>
    </div>
</template>

<script setup>
import StatCard from '@/components/layout/StatCard.vue'
import {reactive, onMounted, ref} from 'vue'

const loading = ref(false)
const error = ref('')

const overall = reactive({departmentCount: 0, employeeCount: 0, ticketCount: 0})

const charts = reactive({
    deptBar: {
        options: {
            chart: {toolbar: {show: false}},
            plotOptions: {bar: {columnWidth: '55%'}},
            xaxis: {categories: [], labels: {show: true}},
            dataLabels: {enabled: true},
            colors: ['#3b82f6']
        },
        series: [{name: 'Employees', data: []}]
    },
    rolesPie: {
        options: {
            labels: [],
            legend: {show: true, position: 'bottom'},
            dataLabels: {enabled: true},
            colors: ['#60a5fa', '#34d399', '#fbbf24', '#f87171']
        },
        series: []
    },
    ticketsBar: {
        options: {
            chart: {toolbar: {show: false}},
            plotOptions: {bar: {columnWidth: '55%'}},
            xaxis: {categories: [], labels: {show: true}},
            dataLabels: {enabled: true},
            colors: ['#10b981']
        },
        series: [{name: 'Tickets', data: []}]
    },
})

async function loadAdminDashboard() {
    loading.value = true
    error.value = ''
    try {
        const r = await fetch('/req/admin/dashboard')
        const j = await r.json()
        const data = j.data || {}
        if (j.code === '000' && data.overall) Object.assign(overall, data.overall)

        const departments = Array.isArray(data.departments) ? data.departments : []
        charts.deptBar.options.xaxis.categories = departments.map(d => String(d.name || 'Dept'))
        charts.deptBar.series[0].data = departments.map(d => Number(d.count || 0))

        const usersByRole = Array.isArray(data.usersByRole) ? data.usersByRole : []
        const roleMap = {
            'Admin': 'System Admin',
            'E-Manager': 'Equip Manager',
            'D-User': 'Dept User',
            'P-Staff': 'Proc Staff',
            'SYS_ADMIN': 'System Admin',
            'EQUIP_MANAGER': 'Equip Manager',
            'DEPT_USER': 'Dept User',
            'PROC_STAFF': 'Proc Staff'
        }
        charts.rolesPie.options.labels = usersByRole.map(r => String(roleMap[r.role] || r.role || 'Unknown'))
        charts.rolesPie.series = usersByRole.map(r => Number(r.count || 0))

        const ticketCategories = Array.isArray(data.ticketCategories) ? data.ticketCategories : []
        charts.ticketsBar.options.xaxis.categories = ticketCategories.map(t => String(t.name))
        charts.ticketsBar.series[0].data = ticketCategories.map(t => Number(t.count || 0))
    } catch (e) {
        error.value = e?.message || 'Failed to load dashboard'
    } finally {
        loading.value = false
    }
}

onMounted(loadAdminDashboard)
</script>

<style scoped>
.page { padding: 0; }
.p16 { padding: 16px; }
.header { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; }
.grid { display:grid; gap:16px; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); }
.stats-grid { margin-top:16px; display:grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap:16px; }
.error { color: #B91C1C; padding: 8px 0; }
</style>


