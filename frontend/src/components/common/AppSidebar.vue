<template>
    <aside class="admin-sidebar" role="navigation" aria-label="Sidebar">
        <div class="brand">
            <div class="logo" aria-hidden="true"></div>
            <div class="name">Return False</div>
        </div>

        <nav class="menu" role="menubar" aria-label="Main menu">
            <RouterLink
                v-for="i in items"
                :key="i.to"
                class="item"
                :to="i.to"
                :title="i.label"
                aria-label="i.label"
            >
                <span class="icon" aria-hidden="true">{{ i.icon || '📁' }}</span>
                <span class="label">{{ i.label }}</span>
            </RouterLink>
        </nav>

        <div class="bottom" v-if="showAccount">
            <RouterLink class="item" :to="accountPath" title="Account">
                <span class="icon">👤</span><span class="label">Account</span>
            </RouterLink>
            <button class="item link" @click="logout" title="Logout" aria-label="Logout">
                <span class="icon">🚪</span><span class="label">Logout</span>
            </button>
        </div>
    </aside>
</template>

<script setup>
import {computed} from 'vue'
import {useRouter, useRoute} from 'vue-router'

const props = defineProps({
    items: {type: Array, default: () => []},
    showAccount: {type: Boolean, default: true},
})
const router = useRouter()
const route = useRoute()
const accountPath = computed(() => {
    try {
        const seg = (route.path || '/admin').split('/')[1] || 'admin'
        return `/${seg}/account`
    } catch {
        return '/admin/account'
    }
})

function logout() {
    localStorage.removeItem('demo_logged_in')
    localStorage.removeItem('account_id')
    localStorage.removeItem('role')
    localStorage.removeItem('demo_username')
    router.push('/login')
}
</script>

<style scoped>
.admin-sidebar {
    width: 248px;
    background: #ffffff;
    border-right: 1px solid #eef2f7;
    height: 100vh;
    position: sticky;
    top: 0;
    display: flex;
    flex-direction: column;
    gap: 14px;
    padding: 18px 16px;
    box-sizing: border-box;
}

/* Brand */
.brand {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 8px;
}

.logo {
    width: 22px;
    height: 22px;
    border-radius: 6px;
    background: linear-gradient(135deg, #60a5fa, #34d399);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.name {
    font-weight: 800;
    color: #1f2937;
    font-size: var(--fs-xl);
}

/* Menu */
.menu {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.item {
    display: flex;
    align-items: center;
    gap: 10px;
    color: #111827;
    text-decoration: none;
    padding: 10px 12px;
    border-radius: 12px;
    font-size: var(--fs-lg);
    transition: background 120ms ease, color 120ms ease;
}

/* active (provided by vue-router as router-link-active) */
.item.router-link-active {
    background: #f1f5f9;
    font-weight: 700;
}

/* hover / focus */
.item:hover,
.item:focus {
    background: rgba(0, 0, 0, 0.03);
    outline: none;
}

/* Icon */
.icon {
    width: 18px;
    text-align: center;
    color: var(--color-muted);
    font-size: 16px;
}

/* label */
.label {
    display: inline-block;
}

/* Bottom area */
.bottom {
    margin-top: auto;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.link {
    background: transparent;
    border: none;
    text-align: left;
    cursor: pointer;
    padding: 10px 12px;
}

/* Make buttons behave like links visually */
button.item {
    display: flex;
    align-items: center;
    gap: 10px;
    color: #111827;
    background: transparent;
    border: none;
    padding: 0;
}

/* small responsive tweak: keep sidebar usable on narrow screens */
@media (max-width: 720px) {
    .admin-sidebar {
        width: 220px;
        padding-left: 12px;
        padding-right: 12px;
    }
}
</style>
