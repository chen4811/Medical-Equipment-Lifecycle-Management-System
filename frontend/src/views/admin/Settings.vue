<template>
    <div class="card" style="padding:16px;">
        <div class="title-lg">System Settings</div>
        <p class="subtitle" style="margin-top:8px;">Configure system-wide preferences.</p>

        <div class="settings-grid">
            <div class="setting-item card" style="padding:16px;">
                <div style="font-weight:700;">Maintenance Mode</div>
                <div class="subtitle">When enabled, only Admins can access the system.</div>
                <div style="display:flex; gap:8px; align-items:center; margin-top:12px;">
                    <span :style="{color: state.enabled ? '#16a34a' : 'var(--color-muted)'}">Current: {{
                            state.enabled ? 'ON' : 'OFF'
                        }}</span>
                    <button class="btn" :disabled="state.loading" @click="refresh">Refresh</button>
                    <button class="btn btn-primary" :disabled="state.loading" @click="toggleMaintenance">
                        {{ state.enabled ? 'Disable' : 'Enable' }} Maintenance Mode
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import {reactive, onMounted} from 'vue'

const state = reactive({enabled: false, loading: false})

function toast(message) {
    let overlay = document.createElement('div')
    overlay.style.position = 'fixed'
    overlay.style.inset = '0'
    overlay.style.background = 'rgba(0,0,0,0.35)'
    overlay.style.display = 'flex'
    overlay.style.alignItems = 'center'
    overlay.style.justifyContent = 'center'
    overlay.style.zIndex = '9999'
    const box = document.createElement('div')
    box.className = 'card'
    box.style.padding = '16px'
    box.style.maxWidth = '420px'
    box.style.minWidth = '280px'
    box.innerHTML = `<div style="font-weight:700;">Notice</div><div style="margin-top:8px;">${message}</div><div style="margin-top:12px; display:flex; justify-content:flex-end;"><button id="ok" class="btn btn-primary">OK</button></div>`
    overlay.appendChild(box)
    document.body.appendChild(overlay)
    overlay.querySelector('#ok').addEventListener('click', () => {
        document.body.removeChild(overlay)
    })
}

async function refresh() {
    try {
        state.loading = true
        const r = await fetch('/req/admin/getMaintenanceModeStatus')
        const j = await r.json().catch(() => ({code: 'ERR'}))
        if (j.code === '000') state.enabled = Boolean(j.data?.enabled)
        else toast(j.message || 'Failed to fetch status')
    } catch {
        toast('Network error')
    } finally {
        state.loading = false
    }
}

async function toggleMaintenance() {
    try {
        state.loading = true
        const operatorId = localStorage.getItem('account_id') || '0'
        const r = await fetch('/req/admin/changeMaintenanceModeStatus', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({enabled: !state.enabled, operator_id: operatorId})
        })
        const j = await r.json().catch(() => ({code: 'ERR'}))
        if (j.code === '000') {
            state.enabled = Boolean(j.data?.enabled)
            toast(state.enabled ? 'Maintenance Mode Enabled' : 'Maintenance Mode Disabled')
        } else {
            toast(j.message || 'Failed to change status')
        }
    } catch {
        toast('Network error')
    } finally {
        state.loading = false
    }
}

onMounted(refresh)
</script>

<style scoped>
.settings-grid {
    margin-top: 16px;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 12px;
}
</style>

