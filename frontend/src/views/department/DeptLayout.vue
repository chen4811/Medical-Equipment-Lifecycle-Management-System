<template>
    <div class="admin-page">
        <AppSidebar :items="deptItems" :showAccount="true"/>
        <main class="content">
            <AdminTopbar/>
            <div class="page-header">
                <div class="heading">
                    <div class="page-title">{{ roleText }}</div>
                    <div class="page-date">{{ today }}</div>
                </div>
            </div>
            <div class="content-body">
                <RouterView/>
            </div>
        </main>
    </div>
</template>

<script setup>
import AppSidebar from '@/components/common/AppSidebar.vue'
import AdminTopbar from '@/components/admin/AdminTopbar.vue'
import {useRoute} from 'vue-router'
import {computed} from 'vue'

const route = useRoute()
const pageTitle = computed(() => route.meta.title || 'Department')

function formatDate(d) {
    try {
        return d.toLocaleDateString('en-GB', {day: '2-digit', month: 'short', year: 'numeric'})
    } catch {
        return ''
    }
}

const today = formatDate(new Date())

const role = computed(() => localStorage.getItem('role') || '')
const roleText = computed(() => {
    const r = role.value
    if (r === 'Admin' || r === 'SYS_ADMIN') return 'Administrator'
    if (r === 'E-Manager' || r === 'EQUIP_MANAGER') return 'Equipment Manager'
    if (r === 'D-User' || r === 'DEPT_USER') return 'Department User'
    if (r === 'P-Staff' || r === 'PROC_STAFF') return 'Procurement Staff'
    return r
})

const deptItems = [
    {label: 'Dashboard', to: '/department/dashboard', icon: '🏠'},
    {label: 'My Devices', to: '/department/my-devices', icon: '💻'},
    {label: 'Usage', to: '/department/usage', icon: '📈'},
    {label: 'Repair Request', to: '/department/repair', icon: '🧰'},
    {label: 'Equipment Requests', to: '/department/requests', icon: '📝'},
]
</script>

<style scoped>
.admin-page {
    display: grid;
    grid-template-columns: 240px 1fr;
    height: 100vh;
    gap: 0;
    overflow: hidden; /* ✅ 阻止外层滚动 */
}

.content {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 16px;
    overflow: auto; /* ✅ 只让右侧内容区滚动 */
    -webkit-overflow-scrolling: touch; /* ✅ 移动端平滑滚动 */
}

.page-header {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 0 16px;
}

.heading {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.page-title {
    font-size: 28px;
    font-weight: 800;
}

.page-date {
    color: var(--color-muted);
    font-size: 14px;
    font-weight: 600;
}

.content-body {
    padding: 16px;
    background: transparent;
}
</style>
