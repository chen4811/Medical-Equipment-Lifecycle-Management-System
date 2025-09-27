<template>
  <div class="card" style="padding:16px;">
    <div class="title-lg">Equipment Requests</div>
    <div class="subtitle" style="margin-top:8px;">Request new devices or accessories for the department.</div>

    <!-- Filters & Create -->
    <div class="filters" style="margin-top:16px; display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap:12px;">
      <input class="input" v-model="filters.keyword" placeholder="Search by ID/content" />
      <select class="input" v-model="filters.status">
        <option value="">All Status</option>
        <option value="under-review">Under Review</option>
        <option value="procuring">Procuring</option>
        <option value="arrived">Arrived</option>
        <option value="terminated">Terminated</option>
        <option value="finished">Finished</option>
      </select>
      <div style="display:flex; gap:8px;">
        <button class="btn" @click="resetFilters">Reset</button>
        <button class="btn btn-primary" @click="openCreate">New Request</button>
      </div>
    </div>

    <!-- Table -->
    <div class="table-wrapper" style="margin-top:16px; overflow:auto;">
      <table class="table" style="table-layout:fixed; width:100%;">
        <thead>
        <tr>
          <th>ID</th>
          <th>Equipment Type</th>
          <th>Count</th>
          <th>Status</th>
          <th>Reason</th>
          <th>Created At</th>
          <th style="width:140px;">Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="r in filtered" :key="r.procureId">
          <td>{{ r.procureId }}</td>
          <td>{{ r.equipmentTypeId }}</td>
          <td>{{ r.count }}</td>
          <td>{{ r.status }}</td>
          <td>{{ r.reason }}</td>
          <td>{{ fmt(r.createdAt) }}</td>
          <td style="white-space:nowrap;">
            <button class="btn btn-red" @click="cancel(r)" v-if="r.status === 'under-review'">Cancel</button>
            <button class="btn btn-green" v-else @click="view(r)">View</button>
          </td>
        </tr>
        <tr v-if="filtered.length === 0">
          <td colspan="7" style="text-align:center; color:var(--color-muted);">No requests</td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- Create Modal -->
    <div v-if="modal.open" class="modal-backdrop">
      <div class="modal card">
        <div class="title-lg">New Equipment Request</div>
        <div class="form-grid">
          <div>
            <label>Equipment Type</label>
            <input class="input" v-model="modal.form.equipmentTypeId" placeholder="e.g. Laptop, Monitor" />
          </div>
          <div>
            <label>Count</label>
            <input class="input" v-model="modal.form.count" type="number" placeholder="e.g. 5" />
          </div>
          <div>
            <label>Supplier</label>
            <input class="input" v-model="modal.form.supplierId" placeholder="e.g. ABC Supplier" />
          </div>
          <div style="grid-column: 1 / -1;">
            <label>Reason</label>
            <textarea class="input" v-model="modal.form.reason" placeholder="Describe your need, quantity, preferred vendor, budget, etc."></textarea>
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
import { reactive, computed, onMounted } from 'vue';
import axios from 'axios';

// State and filters
const state = reactive({
  list: []
});
const filters = reactive({
  keyword: '',
  status: ''
});

const filtered = computed(() => {
  const kw = filters.keyword.toLowerCase();
  return state.list.filter(r => {
    const matchKw = !kw || `${r.procureId} ${r.reason}`.toLowerCase().includes(kw);
    const matchStatus = !filters.status || r.status === filters.status;
    return matchKw && matchStatus;
  });
});

// Reset filters
function resetFilters() {
  filters.keyword = '';
  filters.status = '';
}

// Modal for creating new request
const modal = reactive({
  open: false,
  form: {
    equipmentTypeId: '',
    count: '',
    supplierId: '',
    reason: ''
  }
});

function openCreate() {
  modal.open = true;
  modal.form = { equipmentTypeId: '', count: '', supplierId: '', reason: '' };
}

function closeCreate() {
  modal.open = false;
}

// Save a new equipment request
async function save() {
  try {
    const procureRequestData = {
      equipmentTypeId: modal.form.equipmentTypeId,
      count: modal.form.count,
      supplierId: modal.form.supplierId,
      status: 'under-review',
      reason: modal.form.reason,
      requesterId: '0001',  // Update with actual requester ID
    };

    const response = await axios.post('/req/dept/procure/logs', procureRequestData);
    alert("Request submitted successfully!");
    closeCreate();
    fetchProcureRequests();  // Refresh the list after submission
  } catch (error) {
    console.error(error);
    alert("Failed to submit request.");
  }
}

// Cancel the request
async function cancel(r) {
  try {
    // 更新请求的状态为 Terminated
    r.status = 'terminated';

    // 发送 PUT 请求更新状态到数据库
    const response = await axios.put(`/req/dept/procure/logs/${r.procureId}`, {
      status: 'terminated'
    });

    // 如果更新成功
    alert('Request canceled successfully!');
  } catch (error) {
    console.error(error);
    alert('Failed to cancel the request.');
  }
}

// View the request details (demo placeholder)
function view(r) {
  alert(`View ${r.procureId} (demo only)`);
}

// Fetch procurement requests for the department
async function fetchProcureRequests() {
  try {
    const departmentId = '0001';  // Update with actual department ID
    const response = await axios.get('/req/dept/procure/logs', { params: { departmentId } });
    state.list = response.data;
  } catch (error) {
    console.error("Failed to fetch procure requests:", error);
  }
}

// Format date for display
function fmt(ts) {
  try {
    return new Date(ts).toLocaleString();
  } catch {
    return ts;
  }
}

// Fetch requests on component mount
onMounted(() => {
  fetchProcureRequests(); // Fetch data for the current department
});
</script>

<style scoped>
.table {
  width: 100%;
  border-collapse: collapse;
}
.table th, .table td {
  padding: 10px 12px;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
  white-space: normal;
  word-break: break-word;
}
.table th {
  background: #f9fafb;
  font-weight: 700;
}
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.modal {
  width: min(720px, 100%);
  padding: 16px;
}
.form-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}
textarea.input {
  height: 120px;
  resize: vertical;
}
</style>
