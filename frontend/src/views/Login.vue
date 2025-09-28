<template>
  <div class="login-page">
    <div class="overlay">
      <div class="brand">Medical Equipment Lifecycle Management System</div>
      <div class="login-card card">
        <div class="title-lg">Sign in</div>
        <form class="form" @submit.prevent="onSubmit">
          <label>Name</label>
          <input class="input" v-model="name" placeholder="Enter name" />
          <label>Password</label>
          <input class="input" type="password" v-model="password" placeholder="Enter password" />
          <div class="actions">
            <button class="btn btn-primary" type="submit">Sign In</button>
            <button class="btn" type="button" @click="openReset">Forgot password?</button>
          </div>
          <div v-if="error" class="error">{{ error }}</div>
        </form>
      </div>
      <!-- Reset Password Modal -->
      <div v-if="reset.open" class="ui-modal-backdrop">
        <div class="ui-modal card">
          <div class="title-lg">Reset Password</div>
          <div v-if="reset.step===1" class="ui-form-grid">
            <div style="grid-column:1/-1">
              <label>Email</label>
              <input class="input" v-model="reset.email" placeholder="you@example.com" />
            </div>
          </div>
          <div v-else-if="reset.step===2" class="ui-form-grid">
            <div style="grid-column:1/-1; color: var(--color-muted);">We have sent a 6-digit code to your email. The code is valid for 120 seconds.</div>
            <div>
              <label>Verification Code</label>
              <input class="input" v-model="reset.code" placeholder="6 digits" />
            </div>
          </div>
          <div v-else class="ui-form-grid">
            <div>
              <label>New Password</label>
              <div style="display:flex; gap:8px; align-items:center;">
                <input class="input" :type="reset.show ? 'text' : 'password'" v-model="reset.newPwd" placeholder="New password" />
                <button class="btn" style="width:auto;" @click="reset.show=!reset.show">{{ reset.show ? 'Hide' : 'Show' }}</button>
              </div>
            </div>
          </div>
          <div style="display:flex; gap:8px; justify-content:flex-end; margin-top:16px;">
            <button class="btn" @click="reset.open=false">Close</button>
            <button v-if="reset.step===1" class="btn btn-primary" :disabled="reset.sending" @click="sendCode">Send code</button>
            <button v-else-if="reset.step===2" class="btn btn-primary" :disabled="reset.sending" @click="verifyCode">Verify</button>
            <button v-else class="btn btn-primary" :disabled="reset.sending" @click="commitReset">Reset password</button>
          </div>
          <div v-if="reset.error" style="color:#dc2626; font-size:12px; margin-top:8px;">{{ reset.error }}</div>
        </div>
      </div>
    </div>
  </div>
  
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
function showDialog(message) {
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
  overlay.querySelector('#ok').addEventListener('click', () => { document.body.removeChild(overlay) })
}

const router = useRouter()
const name = ref('')
const password = ref('')
const error = ref('')
const reset = reactive({ open: false, step: 1, email: '', code: '', newPwd: '', show: false, sending: false, error: '' })
function openReset(){ reset.open = true; reset.step = 1; reset.email=''; reset.code=''; reset.error='' }
async function sendCode(){
  reset.error = ''; if (!reset.email) { reset.error = 'Email is required'; return }
  try { reset.sending = true; const r = await fetch(`/req/getVCode?email=${encodeURIComponent(reset.email)}`); const j = await r.json(); if (j.code !== '000') reset.error = j.message || 'Failed to send code'; else reset.step = 2 } catch { reset.error = 'Network error' } finally { reset.sending = false }
}

async function verifyCode(){
  reset.error = '';
  if (!reset.code) { reset.error = 'Code is required'; return }
  try { reset.sending = true; const r = await fetch('/req/account/verifyCode', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: reset.email, code: reset.code }) }); const j = await r.json(); if (j.code !== '000') reset.error = j.message || 'Invalid or expired code'; else reset.step = 3 } catch { reset.error = 'Network error' } finally { reset.sending = false }
}

async function commitReset(){
  reset.error = '';
  if (!reset.newPwd) { reset.error = 'New password is required'; return }
  try { reset.sending = true; const r = await fetch('/req/account/commitReset', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: reset.email, code: reset.code, newPwd: reset.newPwd }) }); const j = await r.json(); if (j.code !== '000') reset.error = j.message || 'Failed to reset password'; else { reset.open = false; showDialog('Password reset successfully. Please sign in with your new password.') } } catch { reset.error = 'Network error' } finally { reset.sending = false }
}

async function onSubmit() {
  error.value = ''
  try {
    const resp = await fetch(`/req/account/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: name.value, password: password.value }),
    })
    if (!resp.ok) throw new Error('HTTP')
    const json = await resp.json()
    if (json && json.code === '000') {
      const id = String(json.data?.account_id || '')
      const role = String(json.data?.role || '')
      const usernameFromApi = String(json.data?.username || name.value)
      if (!id || !role) throw new Error('Empty id/role')
      localStorage.setItem('demo_logged_in', '1')
      localStorage.setItem('account_id', id)
      localStorage.setItem('role', role)
      localStorage.setItem('demo_username', usernameFromApi)
      if (role === 'Admin' || role === 'SYS_ADMIN') return router.push('/admin')
      if (role === 'E-Manager' || role === 'EQUIP_MANAGER') return router.push('/equipment')
      if (role === 'D-User' || role === 'DEPT_USER') return router.push('/department')
      if (role === 'P-Staff' || role === 'PROC_STAFF') return router.push('/procurement')
      return router.push('/admin')
    } else {
      error.value = 'Invalid username or password.'
    }
  } catch (e) {
    error.value = 'Network error. Please try again.'
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background-image: url('https://images.unsplash.com/photo-1496307653780-42ee777d4833?q=80&w=2069&auto=format&fit=crop');
  background-size: cover;
  background-position: center;
}
.overlay {
  min-height: 100vh;
  width: 100%;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 24px;
  box-sizing: border-box;
}
.brand {
  color: #fff;
  font-size: clamp(20px, 3vw, 32px);
  font-weight: 800;
  text-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.login-card {
  width: clamp(520px, 36vw, 720px);
  padding: clamp(20px, 2.5vw, 32px);
}
.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
}
.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.forgot {
  font-size: 13px;
  color: #fff;
  text-decoration: underline;
}
.error {
  color: #ffd1d1;
  font-size: 13px;
  margin-top: 8px;
}
</style>

 


