<template>
  <div class="card" style="padding:16px;">
    <div class="title-lg">My Department Devices</div>
    <div class="subtitle" style="margin-top:8px;">Readonly device list and details for this department.</div>

    <div class="ui-toolbar" style="margin-top:16px;">
      <input class="input" v-model="keyword" placeholder="Search by id/type/vendor" />
      <div>
        <label>Status</label>
        <select class="input" v-model="status">
          <option value="">All status</option>
          <option value="In Use">In Use</option>
          <option value="Under Repair">Under Repair</option>
        </select>
      </div>
    </div>

    <div class="cards" style="margin-top:16px; display:grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap:12px;">
      <div v-for="d in filteredDevices" :key="d.equipmentId" class="device-card card" style="padding:0; overflow:hidden; cursor:pointer;" @click="openDetail(d)">
        <div class="image-wrap">
          <img :src="getDeviceImage(d.equipmentTypeName)" @error="DeviceImgError" alt="device" />
          <div class="status-badge" :data-status="d.status">{{ d.status }}</div>
        </div>
        <div style="padding:12px; display:grid; gap:6px;">
          <div class="title-md">{{ d.equipmentTypeName }}</div>
          <div class="subtitle">ID: {{ d.equipmentId }}</div>
          <div class="muted">{{ shortDesc(d.description) }}</div>
          <div class="muted">Vendor: {{ d.supplierId }}</div>
          <div style="display:flex; gap:8px; margin-top:8px;">
            <button class="btn" @click.stop="openUsageLogs(d)">Usage Logs</button>
            <button class="btn" @click.stop="addUsage(d)">Add Usage</button>
            <button class="btn" @click.stop="openRepairLog(d)">Repair Log</button>
            <button class="btn" @click.stop="openNewRepairTicket(d)">New Repair Ticket</button>
          </div>
        </div>
      </div>
      <div v-if="filteredDevices.length === 0" class="subtitle" style="padding:16px;">No data</div>
    </div>

    <!-- Usage Logs Modal -->
    <div v-if="usage.open" class="ui-modal-backdrop">
      <div class="ui-modal card">
        <div class="title-lg">Usage Logs - {{ usage.device.equipmentId }}</div>
        <div class="subtitle">Records for this device</div>

        <div v-if="usage.loading" class="muted" style="margin-top:12px;">Loading...</div>
        <div v-else-if="usage.error" class="muted" style="margin-top:12px; color:#b91c1c;">{{ usage.error }}</div>

        <div v-else style="margin-top:12px; display:grid; gap:8px;">
          <div v-for="u in usage.list" :key="u.logId" class="card" style="padding:12px;">
            <div><b>Time:</b> {{ fmt(u.time) }}</div>
            <div><b>Recorder:</b> {{ u.recorderId }}</div>
            <div><b>Remark:</b> {{ u.remark || '-' }}</div>
          </div>
          <div v-if="usage.list.length === 0" class="subtitle">No logs</div>
        </div>

        <div style="display:flex; gap:8px; justify-content:flex-end; margin-top:16px;">
          <button class="btn" @click="closeUsage">Close</button>
        </div>
      </div>
    </div>

    <!-- Add Usage Modal -->
    <div v-if="add.open" class="ui-modal-backdrop">
      <div class="ui-modal card">
        <div class="title-lg">Add Usage</div>
        <div class="ui-form-grid">
          <div style="grid-column:1/-1; color: var(--color-muted);">
            Device: {{ add.device.equipmentId }}
          </div>
          <div>
            <label>Purpose</label>
            <input class="input" v-model="add.form.purpose" placeholder="e.g. surgery support" />
          </div>
          <div>
            <label>Exception</label>
            <input class="input" v-model="add.form.exception" placeholder="optional" />
          </div>
          <div style="grid-column: 1 / -1;">
            <label>Remark</label>
            <input class="input" v-model="add.form.remark" placeholder="note ..." />
          </div>
        </div>
        <div style="display:flex; gap:8px; justify-content:flex-end; margin-top:16px;">
          <button class="btn" @click="closeAdd">Cancel</button>
          <button class="btn btn-primary" @click="saveUsage">Submit</button>
        </div>
      </div>
    </div>

    <!-- Repair Log Modal -->
    <div v-if="repairLog.open" class="ui-modal-backdrop">
      <div class="ui-modal card">
        <div class="title-lg">Repair Logs - {{ repairLog.device.equipmentId }}</div>
        <div class="subtitle">Repair records for this device</div>

        <div v-if="repairLog.loading" class="muted" style="margin-top:12px;">Loading...</div>
        <div v-else-if="repairLog.error" class="muted" style="margin-top:12px; color:#b91c1c;">{{ repairLog.error }}</div>

        <div v-else style="margin-top:12px; display:grid; gap:8px;">
          <div v-for="r in repairLog.list" :key="r.ticketId" class="card" style="padding:12px;">
            <div><b>Ticket ID:</b> {{ r.ticketId }}</div>
            <div><b>Created At:</b> {{ fmt(r.createdAt) }}</div>
            <div><b>Finished At:</b> {{ r.finishedAt ? fmt(r.finishedAt) : 'N/A' }}</div>
            <div><b>Notes:</b> {{ r.notes || '-' }}</div>
            <div><b>Cost:</b> {{ r.cost ? '$' + r.cost.toFixed(2) : 'N/A' }}</div>
            <div><b>Status:</b> {{ r.status }}</div>
            <div><b>Requester ID:</b> {{ r.requesterId }}</div>
            <div><b>Manager ID:</b> {{ r.managerId }}</div>
          </div>
          <div v-if="repairLog.list.length === 0" class="subtitle">No repair logs</div>
        </div>

        <div style="display:flex; gap:8px; justify-content:flex-end; margin-top:16px;">
          <button class="btn" @click="closeRepairLog">Close</button>
        </div>
      </div>
    </div>

    <!-- New Repair Ticket Modal -->
    <div v-if="newRepairTicket.open" class="ui-modal-backdrop">
      <div class="ui-modal card">
        <div class="title-lg">New Repair Ticket</div>
        <div class="ui-form-grid">
          <div>
            <label>Device ID</label>
            <input class="input" v-model="newRepairTicket.form.deviceId" placeholder="e.g. EQ-0001" />
          </div>
          <div>
            <label>Type</label>
            <select class="input" v-model="newRepairTicket.form.type">
              <option>Repair</option>
              <option>Maintenance</option>
              <option>Part Replacement</option>
            </select>
          </div>
          <div style="grid-column: 1 / -1;">
            <label>Description</label>
            <textarea class="input" v-model="newRepairTicket.form.description" placeholder="Describe the issue"></textarea>
          </div>
        </div>
        <div style="display:flex; gap:8px; justify-content:flex-end; margin-top:16px;">
          <button class="btn" @click="closeNewRepairTicket">Cancel</button>
          <button class="btn btn-primary" @click="saveRepairTicket">Submit</button>
        </div>
      </div>
    </div>

    <!-- Device Detail Drawer -->
    <div v-if="detail.open" class="drawer-backdrop" @click="closeDetail">
      <div class="drawer card" @click.stop>
        <div class="title-lg">Device Detail</div>
        <div class="subtitle">ID: {{ detail.device.equipmentId }}</div>
        <div class="detail-grid">
          <div class="detail-image">
            <img :src="getDeviceImageUrl(detail.device.equipmentId)" @error="onDeviceImgError" alt="device" />
          </div>
          <div class="detail-info">
            <div style="display:grid; gap:8px;">
              <div><b>Type:</b> {{ detail.device.equipmentTypeName }}</div>
              <div><b>Status:</b> {{ detail.device.status }}</div>
              <div><b>Vendor:</b> {{ detail.device.supplierId }}</div>
              <div><b>Description:</b> {{ detail.device.description || 'No description' }}</div>
              <div><b>Diseases:</b> {{ (detail.device.diseases || []).join(', ') || '-' }}</div>
            </div>
            <div style="display:flex; gap:8px; margin-top:16px;">
              <button class="btn" @click="closeDetail">Close</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, watch } from 'vue'
import DateTimePicker from '@/components/DateTimePicker.vue'
import { getDeviceImageUrl, onDeviceImgError } from '@/utils/images.js'
import axios from 'axios'

const keyword = ref('')
const status = ref('')
const devices = reactive([])
const departmentId = ref(null)
const accountId = Number(localStorage.getItem('account_id') || 'θ')

import xray from '@/assets/xray.png'
import ecg from '@/assets/ecg.png'
import defibrillator from '@/assets/defibrillator.png'
import bloodAnalyzer from '@/assets/blood_analyzer.png'
import infusionPump from '@/assets/infusion_pump.png'
import defaultImg from '@/assets/defaultImg.png'

const getDepartmentId = async (accountId) => {
  try {
    const response = await axios.get(`/req/department/id`, {
      params: { accountId }
    })
    console.log(response.data);
    departmentId.value = response.data
  } catch (error) {
    console.error('Error fetching department ID:', error)
  }
}

const filteredDevices = computed(() => {
  const kw = keyword.value.toLowerCase()
  return devices.filter(d => {
    const matchKw = !kw || `${d.equipmentId} ${d.equipmentTypeName} ${d.supplierId}`.toLowerCase().includes(kw)
    const matchStatus = !status.value || d.status === status.value
    return matchKw && matchStatus
  })
})

onMounted(async () => {
  // 获取 departmentId
  if (accountId !== 'θ') {
    await getDepartmentId(accountId)
  }
  await fetchDevices()
})

watch([keyword, status], () => {
  fetchDevices()
})

const deviceImageMap = {
  'X-Ray Machine': xray,
  'ECG Monitor': ecg,
  'Defibrillator': defibrillator,
  'Blood Analyzer': bloodAnalyzer,
  'Infusion Pump': infusionPump
}

function getDeviceImage(typeName) {
  let defaultImg;
  return deviceImageMap[typeName] || defaultImg
}

function DeviceImgError(event) {
  event.target.src = defaultImg
}

async function fetchDevices() {
  if (!departmentId.value) return  // Only fetch devices if departmentId is available

  try {
    const response = await axios.get('/req/devices', {
      params: {
        keyword: keyword.value,
        statuses: status.value ? [status.value] : [],
        departmentIds: [departmentId.value]  // Send departmentId dynamically
      }
    })
    console.log('Devices API Response:', response.data)
    devices.splice(0, devices.length, ...response.data)  // Update devices list
  } catch (error) {
    console.error('Error fetching devices:', error)
  }
}

// Usage Log Modal
const usage = reactive({ open: false, device: {}, list: [], loading: false, error: '' })

function openUsageLogs(d) {
  usage.open = true
  usage.device = d
  usage.list = []
  usage.error = ''
  fetchUsageLogs(d.equipmentId)
}

function closeUsage() {
  usage.open = false
}

async function fetchUsageLogs(equipmentId) {
  usage.loading = true
  try {
    const response = await axios.get('/req/dept/usage/logs/equip', { params: { equipmentId } })
    console.log('LOG Response:', response.data);
    usage.list = Array.isArray(response.data) ? response.data : []
  } catch (err) {
    console.error(err)
    usage.error = 'Failed to load logs'
  } finally {
    usage.loading = false
  }
}

// Add Usage Modal
const add = reactive({ open: false, device: {}, form: { start: '', end: '', purpose: '', remark: '', exception: '' }, saving: false, error: '' })

function addUsage(d) {
  add.open = true
  add.device = d
  add.form.remark = ''
  add.error = ''
}

function closeAdd() {
  add.open = false
}

async function saveUsage() {
  console.log("Save button clicked");
  if (!add.form.remark.trim()) {
    add.error = 'Remark is required'
    return
  }
  let tempRemark;
  if (!add.form.start.trim() || !add.form.end.trim()) {
    tempRemark = "note: " + add.form.remark.trim() + "; exception:" + add.form.exception.trim() + "; purpose:" + add.form.purpose.trim();
  }
  else{
    tempRemark = "note: " + add.form.remark.trim() + "; exception:" + add.form.exception.trim() + "; purpose:" + add.form.purpose.trim() + "; from" + add.form.start.trim() + " to " + add.form.end.trim();
  }
  add.saving = true
  add.error = ''
  try {
    const recorderId = localStorage.getItem('account_id') || ''
    if (!recorderId) { add.error = 'Not logged in'; add.saving=false; return }
    await axios.post('/req/dept/usage/logs', {
      recorderId: String(recorderId),
      targetEquipmentId: add.device.equipmentId,
      remark: tempRemark,
    })
    closeAdd()
    // 保存成功后刷新当前设备的日志（如果日志面板开着）
    if (usage.open && usage.device.equipmentId === add.device.equipmentId) {
      fetchUsageLogs(add.device.equipmentId)
    }
  } catch (err) {
    console.error(err)
    add.error = 'Failed to save'
  } finally {
    add.saving = false
  }
}

// Repair Log Modal
const repairLog = reactive({ open: false, device: {}, list: [], loading: false, error: '' })
function openRepairLog(d) { repairLog.open = true; repairLog.device = d; fetchRepairLogs(d.equipmentId) }
function closeRepairLog() { repairLog.open = false }

async function fetchRepairLogs(equipmentId) {
  repairLog.loading = true
  try {
    const response = await axios.get('/req/dept/repair/logs', { params: { equipmentId } })
    console.log(response.data);

    // 处理返回的数据
    if (Array.isArray(response.data) && response.data.length > 0) {
      // 设置维修日志数据
      repairLog.list = response.data.map(item => ({
        ticketId: item.ticketId,
        createdAt: item.createdAt,
        finishedAt: item.finishedAt,
        notes: item.notes,
        cost: item.cost,
        result: item.result,
        status: item.status,
        departmentId: item.departmentId,
        requesterId: item.requesterId,
        managerId: item.managerId,
        equipmentId: item.equipmentId,
      }));
    } else {
      repairLog.list = [];
    }
  } catch (err) {
    console.error(err)
    repairLog.error = 'Failed to load repair logs'
  } finally {
    repairLog.loading = false
  }
}

// Repair Modal
const newRepairTicket = reactive({ open: false, form: { deviceId: '',departmentId: departmentId.value, type: 'Repair', description: '' } })
function openNewRepairTicket(d) {
  newRepairTicket.open = true;
  newRepairTicket.form.deviceId = d.equipmentId
  newRepairTicket.form.departmentId = departmentId.value
  newRepairTicket.form.type = 'Repair'
  newRepairTicket.form.description = ''
}
function closeNewRepairTicket() { newRepairTicket.open = false }

async function saveRepairTicket() {
  try {
    const repairTicketData = {
      equipmentId: newRepairTicket.form.deviceId,
      notes: newRepairTicket.form.description,
      cost: 0,
      result: '',
      status: 'Pending',
      departmentId: departmentId.value,
      requesterId: '2',
      managerId: '0'
    };
    await axios.post('/req/dept/repair/logs', repairTicketData)
    alert("Repair ticket submitted successfully!")
    closeNewRepairTicket()
  } catch (error) {
    console.error(error)
    alert("Failed to submit repair ticket.")
  }
}

// Device Detail Drawer
const detail = reactive({ open: false, device: {} })
function openDetail(d) { detail.open = true; detail.device = d }
function closeDetail() { detail.open = false }

function fmt(ts) { try { return new Date(ts).toLocaleString('en-US', { hour12: false }) } catch { return ts } }
function shortDesc(text) { const t = text || ''; return t.length > 100 ? `${t.slice(0, 100)}…` : t }
</script>

<style scoped>
.device-card .image-wrap { position: relative; width: 100%; aspect-ratio: 16/9; background: #f3f4f6; }
.device-card .image-wrap img { width: 100%; height: 100%; object-fit: cover; display:block; }
.device-card .status-badge { position: absolute; left: 12px; top: 12px; background: rgba(0,0,0,0.6); color: #fff; padding: 2px 8px; border-radius: 999px; font-size: 12px; }

.drawer-backdrop { position: fixed; inset:0; background: rgba(0,0,0,0.35); display:flex; }
.drawer { margin-left:auto; width:min(920px, 100%); padding:16px; }
.detail-grid { display:grid; grid-template-columns: 1fr 1fr; gap:16px; margin-top:12px; }
.detail-image img { width:100%; height: 320px; object-fit: cover; border-radius: 8px; background: #f3f4f6; }
.detail-info { display:flex; flex-direction: column; }

@media (max-width: 800px) {
  .detail-grid { grid-template-columns: 1fr; }
}

.modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,0.35); display:flex; align-items:center; justify-content:center; padding:16px; }
.modal { width: min(720px, 100%); padding: 16px; }
.form-grid { margin-top: 16px; display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
textarea.input { height: 120px; resize: vertical; }
</style>
