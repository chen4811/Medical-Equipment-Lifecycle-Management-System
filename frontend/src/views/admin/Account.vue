<template>
    <div class="card" style="padding:16px;">
        <div class="title-lg">Account</div>
        <div class="subtitle" style="margin-top:8px;">Manage your profile and security.</div>

        <div class="grid">
            <div class="section card" style="padding:16px;">
                <div style="font-weight:700;">Profile</div>
                <div class="form">
                    <label>Display Name</label>
                    <input class="input" v-model="profile.name" placeholder="Your name"/>
                    <label>Email</label>
                    <input class="input" v-model="profile.email" placeholder="you@example.com"/>
                    <div style="display:flex; gap:8px; justify-content:flex-end;">
                        <button class="btn" @click="resetProfile">Reset</button>
                        <button class="btn btn-primary" @click="saveProfile">Save changes</button>
                    </div>
                </div>
            </div>

            <div class="section card" style="padding:16px;">
                <div style="font-weight:700;">Security</div>
                <div class="subtitle">Update your password regularly to keep your account secure.</div>
                <div style="display:flex; justify-content:flex-end; margin-top:8px;">
                    <button class="btn btn-primary" @click="openPwdModal">Change password</button>
                </div>
            </div>
        </div>

        <!-- Change password modal -->
        <div v-if="pwdModal" class="ui-modal-backdrop">
            <div class="ui-modal card">
                <div class="title-lg">Change Password</div>
                <div class="ui-form-grid">
                    <div>
                        <label>Current Password</label>
                        <div style="display:flex; gap:8px; align-items:center;">
                            <input class="input" :type="show.cur ? 'text' : 'password'" v-model="pwd.current"
                                   placeholder="Current password"/>
                            <button class="btn" style="width:auto;" @click="show.cur=!show.cur">
                                {{ show.cur ? 'Hide' : 'Show' }}
                            </button>
                        </div>
                    </div>
                    <div>
                        <label>New Password</label>
                        <div style="display:flex; gap:8px; align-items:center;">
                            <input class="input" :type="show.new ? 'text' : 'password'" v-model="pwd.next"
                                   placeholder="New password"/>
                            <button class="btn" style="width:auto;" @click="show.new=!show.new">
                                {{ show.new ? 'Hide' : 'Show' }}
                            </button>
                        </div>
                    </div>
                    <div>
                        <label>Confirm New Password</label>
                        <div style="display:flex; gap:8px; align-items:center;">
                            <input class="input" :type="show.confirm ? 'text' : 'password'" v-model="pwd.confirm"
                                   placeholder="Confirm new password"/>
                            <button class="btn" style="width:auto;" @click="show.confirm=!show.confirm">
                                {{ show.confirm ? 'Hide' : 'Show' }}
                            </button>
                        </div>
                    </div>
                </div>
                <div style="display:flex; gap:8px; justify-content:flex-end; margin-top:16px;">
                    <button class="btn" @click="closePwdModal">Cancel</button>
                    <button class="btn btn-primary" @click="changePassword">Update</button>
                </div>
            </div>
        </div>
    </div>

</template>

<script setup>
import {reactive, onMounted, ref} from 'vue'

const profile = reactive({name: '', email: ''})
const pwd = reactive({current: '', next: '', confirm: ''})
const show = reactive({cur: false, new: false, confirm: false})
const pwdModal = ref(false)

async function sha256Hex(message) {
    const msgUint8 = new TextEncoder().encode(message)
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgUint8)
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
}

function toast(msg) {
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
    box.innerHTML = `<div style="font-weight:700;">Notice</div><div style="margin-top:8px;">${msg}</div><div style="margin-top:12px; display:flex; justify-content:flex-end;"><button id="ok" class="btn btn-primary">OK</button></div>`
    overlay.appendChild(box)
    document.body.appendChild(overlay)
    overlay.querySelector('#ok').addEventListener('click', () => {
        document.body.removeChild(overlay)
    })
}

function resetProfile() {
    try {
        const name = localStorage.getItem('demo_username') || ''
        profile.name = name
    } catch {
    }
}

async function saveProfile() {
    try {
        const accountId = localStorage.getItem('account_id') || ''
        if (!accountId) return toast('Not logged in')
        const resp = await fetch('/req/admin/user', {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                account_id: String(accountId),
                name: profile.name,
                password: '',
                role: '',
                department_id: '',
                email: profile.email
            })
        })
        const j = await resp.json().catch(() => ({code: 'ERR'}))
        if (j.code !== '000') return toast(j.message || 'Failed to save')
        localStorage.setItem('demo_username', profile.name || '')
        toast('Saved')
    } catch {
        toast('Network error')
    }
}

function resetPwdForm() {
    pwd.current = '';
    pwd.next = '';
    pwd.confirm = ''
}

function openPwdModal() {
    pwdModal.value = true
}

function closePwdModal() {
    pwdModal.value = false;
    resetPwdForm();
    show.cur = false;
    show.new = false;
    show.confirm = false
}

async function changePassword() {
    if (!pwd.next || pwd.next !== pwd.confirm) return toast('Passwords do not match')
    const accountId = localStorage.getItem('account_id') || ''
    if (!accountId) return toast('Not logged in')
    try {
        const resp = await fetch('/req/admin/resetPwd', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                accountId,
                newPwd: await sha256Hex(pwd.next),
                currentPwd: pwd.current ? await sha256Hex(pwd.current) : '',
                operator_id: accountId
            })
        })
        const json = await resp.json()
        if (json.code === '000') {
            toast('Password updated');
            closePwdModal()
        } else {
            toast(json.message || 'Failed to update')
        }
    } catch {
        toast('Network error')
    }
}

onMounted(() => {
    resetProfile();
    // load email from backend
    (async () => {
        try {
            const id = Number(localStorage.getItem('account_id') || '0')
            if (!id) return
            const r = await fetch(`/req/admin/users`)
            const j = await r.json()
            if (j.code === '000') {
                const me = (j.data || []).find(u => String(u.id) === String(id))
                if (me && me.email) profile.email = me.email
            }
        } catch {
        }
    })()
})
</script>

<style scoped>
.grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 16px;
    margin-top: 16px;
}

.form {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 12px;
}

.row {
    display: flex;
    gap: 16px;
    align-items: center;
}
</style>



