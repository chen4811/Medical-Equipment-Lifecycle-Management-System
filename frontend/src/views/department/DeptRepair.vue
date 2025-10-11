<template>
    <div class="card" style="padding:16px;">
        <div class="title-lg">Repair Requests</div>
        <div class="subtitle" style="margin-top:8px;">Create and track repair/maintenance/parts replacement tickets.
        </div>

        <!-- Filters -->
        <div class="ui-toolbar" style="margin-top:16px;">
            <input class="input" v-model="filters.keyword" placeholder="Search by id/device/desc"/>
            <select class="input" v-model="filters.type">
                <option value="">All types</option>
                <option>Repair</option>
                <option>Maintenance</option>
                <option>Part Replacement</option>
            </select>
            <select class="input" v-model="filters.status">
                <option value="">All status</option>
                <option value="pending">Pending</option>
                <option value="in-progress">In Repair</option>
                <option value="under-acceptance">In Acceptance</option>
                <option value="completed">Completed</option>
                <option value="rejected">Rejected</option>
            </select>
            <div style="display:flex; gap:8px;">
                <button class="btn" @click="resetFilters">Reset</button>
                <button class="btn btn-primary" @click="openCreate">New Ticket</button>
            </div>
        </div>

        <!-- Table -->
        <div class="table-wrapper" style="margin-top:16px; overflow:auto;">
            <table class="ui-table" style="table-layout:auto; width:100%;">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Device</th>
                    <th>Type</th>
                    <th>Description</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th style="width:160px;">Actions</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="t in filtered" :key="t.ticketId">
                    <td>{{ t.ticketId }}</td>
                    <td>{{ t.equipmentId }}</td>
                    <td>{{ t.type || 'Repair' }}</td>
                    <td>{{ t.notes }}</td>
                    <td>{{ t.status }}</td>
                    <td>{{ fmt(t.createdAt) }}</td>
                    <td>
                        <button class="btn" @click="accept(t)" v-if="t.status === 'In Acceptance'">Confirm</button>
                        <button class="btn" @click="view(t)" v-else>View</button>
                    </td>
                </tr>
                <tr v-if="filtered.length === 0">
                    <td colspan="7" style="text-align:center; color:var(--color-muted);">No tickets</td>
                </tr>
                </tbody>
            </table>
        </div>

        <!-- Create Modal -->
        <div v-if="modal.open" class="ui-modal-backdrop">
            <div class="ui-modal card">
                <div class="title-lg">New Repair Ticket</div>
                <div class="ui-form-grid">
                    <div>
                        <label>Device ID</label>
                        <input class="input" v-model="modal.form.deviceId" placeholder="e.g. EQ-0001"/>
                    </div>
                    <div>
                        <label>Type</label>
                        <select class="input" v-model="modal.form.type">
                            <option>Repair</option>
                            <option>Maintenance</option>
                            <option>Part Replacement</option>
                        </select>
                    </div>
                    <div style="grid-column: 1 / -1;">
                        <label>Description</label>
                        <textarea class="input" v-model="modal.form.description"
                                  placeholder="Describe the issue"></textarea>
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
import {reactive, computed, onMounted, ref} from 'vue';
import axios from 'axios';

// Initialize the state and filters
const state = reactive({
    list: []
});
const filters = reactive({
    keyword: '',
    type: '',
    status: ''
});
const modal = reactive({
    open: false,
    form: {
        deviceId: '',
        type: 'Repair',
        description: ''
    }
});

const departmentId = ref(null)
const accountId = Number(localStorage.getItem('account_id') || 'θ')

const getDepartmentId = async (accountId) => {
    try {
        const response = await axios.get(`/req/department/id`, {
            params: {accountId}
        })
        console.log(response.data);
        departmentId.value = response.data
    } catch (error) {
        console.error('Error fetching department ID:', error)
    }
}

// Filtered tickets based on filters
const filtered = computed(() => {
    const kw = filters.keyword.toLowerCase();
    return state.list.filter(t => {
        const matchKw = !kw || `${t.id} ${t.deviceId} ${t.description}`.toLowerCase().includes(kw);
        const matchType = !filters.type || t.type === filters.type;
        const matchStatus = !filters.status || t.status === filters.status;
        return matchKw && matchType && matchStatus;
    });
});

// Fetch repair tickets for a specific department
async function fetchRepairTickets(departmentId) {
    try {
        const response = await axios.get('/req/dept/repair/logs/id', {params: {departmentId}});
        state.list = response.data;
    } catch (error) {
        console.error("Failed to fetch repair tickets:", error);
    }
}

// Reset filters
function resetFilters() {
    filters.keyword = '';
    filters.type = '';
    filters.status = '';
}

// Open modal to create a new repair ticket
function openCreate() {
    modal.open = true;
    modal.form = {deviceId: '', type: 'Repair', description: ''};
}

// Close the modal for creating a new repair ticket
function closeCreate() {
    modal.open = false;
}

// Save new repair ticket
async function save() {
    try {
        const repairTicketData = {
            equipmentId: modal.form.deviceId,
            notes: modal.form.description,
            cost: 0,
            result: '',
            status: 'Pending',
            departmentId: departmentId.value,
            requesterId: '2',
            managerId: '0'
        };
        await axios.post('/req/dept/repair/logs', repairTicketData);
        showDialog('Repair ticket submitted successfully!')
        closeCreate();
        fetchRepairTickets(departmentId.value); // Fetch updated tickets
    } catch (error) {
        console.error(error);
        showDialog('Failed to submit repair ticket.')
    }
}

// Mark ticket as completed
function accept(t) {
    t.status = 'Completed';
}

// View ticket details (demo placeholder)
function view(t) {
    showDialog(`View ${t.id} (demo only)`)
}

// Format date to local string
function fmt(ts) {
    try {
        return new Date(ts).toLocaleString();
    } catch {
        return ts;
    }
}

// Fetch tickets when component is mounted
onMounted(async () => {
    // 获取 departmentId
    if (accountId !== 'θ') {
        await getDepartmentId(accountId)
    }
    await fetchRepairTickets(departmentId.value);
});

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
    overlay.querySelector('#ok').addEventListener('click', () => {
        document.body.removeChild(overlay)
    })
}

</script>

<style scoped>
.ui-form-grid textarea.input {
    height: 120px;
    resize: vertical;
}

textarea.input {
    height: 120px;
    resize: vertical;
}
</style>
