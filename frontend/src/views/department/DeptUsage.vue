<template>
  <div class="card" style="padding:16px;">
    <div class="title-lg">Usage Logging</div>
    <p class="subtitle" style="margin-top:8px;">Create and browse device usage logs.</p>

    <div class="ui-toolbar" style="margin-top:16px;">
      <input class="input" v-model="filters.keyword" placeholder="Search by recorder/device/remark" />
      <div>
        <label>Date</label>
        <input class="input" type="date" v-model="filters.date" />
      </div>
      <div style="display:flex; gap:8px;">
        <button class="btn" @click="resetFilters">Reset</button>
        <button class="btn btn-primary" @click="openCreate">Add Usage</button>
      </div>
    </div>

    <div class="table-wrapper" style="margin-top:16px; overflow:auto;">
      <table class="ui-table" style="table-layout:auto; width:100%;">
        <thead>
          <tr>
            <th>Time</th>
            <th>Recorder</th>
            <th>Device</th>
            <th>Remark</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in rows" :key="r.id">
            <td>{{ fmt(r.time) }}</td>
            <td>{{ r.recorderId }}</td>
            <td>{{ r.equipmentId }}</td>
            <td>{{ r.remark }}</td>
          </tr>
          <tr v-if="rows.length===0"><td colspan="4">No logs</td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="modal.open" class="ui-modal-backdrop">
      <div class="ui-modal card">
        <div class="title-lg">Add Usage</div>
        <div class="ui-form-grid">
          <div>
            <label>Device</label>
            <select class="input" v-model="modal.form.deviceId">
              <option v-for="d in deviceOptions" :key="d.value" :value="d.value">{{ d.label }}</option>
            </select>
          </div>
          <div>
            <label>Remark</label>
            <input class="input" v-model="modal.form.remark" placeholder="note, exception, purpose ..." />
          </div>
        </div>
        <div style="display:flex; gap:8px; justify-content:flex-end; margin-top:16px;">
          <button class="btn" @click="closeCreate">Cancel</button>
          <button class="btn btn-primary" @click="save">Submit</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import axios from 'axios'
const filters = reactive({ keyword: '', date: '' })
const rows = reactive([])
const modal = reactive({ open: false, form: { deviceId:'', remark:'' } })
const deviceOptions = ref([])
function resetFilters(){ filters.keyword=''; filters.date=''; fetchLogs() }
function openCreate(){ modal.open=true; modal.form={ deviceId:(deviceOptions.value[0]?.value||''), remark:'' } }
function closeCreate(){ modal.open=false }
async function fetchLogs(){
  try {
    // 需要 equipmentId 参数，若未选则不请求
    if (!modal.form.deviceId) return
    const r = await axios.get('/req/dept/usage/logs', { params: { equipmentId: modal.form.deviceId } })
    rows.splice(0, rows.length, ...(r.data || []))
  } catch {}
}
async function save(){
  try {
    const recorderId = localStorage.getItem('account_id') || ''
    if (!recorderId) return
    await axios.post('/req/dept/usage/logs', { recorderId:String(recorderId), targetEquipmentId: modal.form.deviceId, remark: modal.form.remark })
    modal.open=false; fetchLogs()
  } catch {}
}
function fmt(ts){ try { return new Date(ts).toLocaleString('en-US', { hour12:false }) } catch { return ts } }
// load device options for dropdown
;(async () => { try { const r = await axios.get('/req/devices'); const arr = Array.isArray(r.data) ? r.data : []; deviceOptions.value = arr.map(x => ({ value: x.equipmentId, label: `${x.equipmentId} - ${x.equipmentTypeName||''}` })); if (!modal.form.deviceId && deviceOptions.value[0]) modal.form.deviceId = deviceOptions.value[0].value; fetchLogs() } catch {} })()
fetchLogs()
</script>


