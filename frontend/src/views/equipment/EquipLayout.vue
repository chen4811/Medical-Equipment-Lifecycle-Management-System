<template>
  <div class="admin-page">
    <AppSidebar :items="menuItems" />
    <main class="content">
      <AdminTopbar />
      <div class="page-header">
        <div class="heading">
          <div class="page-title">{{ pageTitle }}</div>
          <div class="page-date">{{ today }}</div>
        </div>
      </div>
      <div class="content-body">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<script setup>
import AppSidebar from '@/components/common/AppSidebar.vue'
import AdminTopbar from '@/components/admin/AdminTopbar.vue'
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const menuItems = [
  { label: 'Dashboard', to: '/equipment/dashboard' },
  { label: 'Equipment Ledger', to: '/equipment/ledger' },
  { label: 'Onboarding & Profiling', to: '/equipment/onboarding' },
  { label: 'Repairs', to: '/equipment/repairs' },
  { label: 'Scrap Management', to: '/equipment/scrap' },
]

const route = useRoute()
const pageTitle = computed(() => route.meta.title || 'Equipment')
function formatDate(d) { try { return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) } catch { return '' } }
const today = formatDate(new Date())
</script>

<style scoped>
.admin-page { display: grid; grid-template-columns: 240px 1fr; height: 100vh; }
.content { display: flex; flex-direction: column; gap: 16px; padding: 16px; }
.page-header { display:flex; flex-direction:column; gap:16px; }
.heading { display:flex; align-items:baseline; justify-content:space-between; }
.page-title { font-size: 28px; font-weight: 800; }
.page-date { color: var(--color-muted); font-size: 14px; font-weight: 600; }
.content-body { padding: 16px; }
</style>


