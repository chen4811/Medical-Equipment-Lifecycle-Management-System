<template>
    <div class="admin-page">
        <AppSidebar :items="menuItems"/>
        <main class="content">
            <AdminTopbar/>
            <div class="page-header">
                <div class="heading">
                    <div class="page-title">{{ pageTitle }}</div>
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

const menuItems = [
    {label: 'Dashboard', to: '/procurement/dashboard'},
    // {label: 'Bids / RFQ', to: '/procurement/bids'},
    {label: 'Vendors', to: '/procurement/vendors'},
    {label: 'Purchase Requests', to: '/procurement/plans'},
    {label: 'Purchase Orders', to: '/procurement/orders'},
    // {label: 'Contracts & Budget', to: '/procurement/contracts'},
    {label: 'Receiving', to: '/procurement/receiving'},
]

const route = useRoute()
const pageTitle = computed(() => route.meta.title || 'Procurement')

function formatDate(d) {
    try {
        return d.toLocaleDateString('en-GB', {day: '2-digit', month: 'short', year: 'numeric'})
    } catch {
        return ''
    }
}

const today = formatDate(new Date())
</script>

<style scoped>
.admin-page {
    display: grid;
    grid-template-columns: 240px 1fr;
    height: 100vh;
}

.content {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 16px;
}

.page-header {
    display: flex;
    flex-direction: column;
    gap: 16px;
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
}
</style>
