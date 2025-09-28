<template>
  <div class="card" style="padding:16px;">
    <div class="title-lg">Onboarding & Profiling</div>
    <div class="subtitle" style="margin-top:8px;">Receiving arrived purchase orders for onboarding.</div>

    <div class="cards" style="margin-top:16px; display:grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap:12px;">
      <div v-for="o in orders" :key="o.id || o.procureId" class="card" style="padding:0; overflow:hidden;">
        <div class="image-wrap">
          <img :src="getDeviceImage(o.equipmentTypeName)" @error="DeviceImgError" alt="device" />
          <div class="status-badge">PO</div>
        </div>
        <div style="padding:12px; display:grid; gap:6px;">
          <div class="title-md">{{ o.equipmentTypeName }}</div>
          <div class="subtitle">OrderID: {{ o.id ?? o.procureId }}</div>
          <div class="muted">Quantity: {{ o.count ?? 0 }}</div>
          <div class="muted">Supplier: {{ o.supplierName }}</div>
          <div style="display:flex; gap:8px; margin-top:8px;">
            <button class="btn btn-green" @click="onboard(o)">Onboard</button>
          </div>
        </div>
      </div>
      <div v-if="orders.length === 0" class="subtitle" style="padding:16px;">No arrived orders</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import xray from '@/assets/xray.png'
import ecg from '@/assets/ecg.png'
import defibrillator from '@/assets/defibrillator.png'
import bloodAnalyzer from '@/assets/blood_analyzer.png'
import infusionPump from '@/assets/infusion_pump.png'
import defaultImg from '@/assets/defaultImg.png'
const orders = ref([])

async function loadOrders() {
  try {
    const res = await axios.get('/req/arrived-orders')
    orders.value = Array.isArray(res.data) ? res.data : res.data.orders ?? []
    console.log('Loaded orders:', orders.value)
  } catch (err) {
    console.error('Failed to load purchase orders', err)
    orders.value = []
  }
}

async function onboard(o) {
  try {
    await axios.post(`/req/onboard/${o.procureId}`);
    alert(`Order ${o.procure_id} The goods have been successfully warehoused.`);
    await loadOrders();
  } catch (err) {
    console.error('Onboarding failed', err);
    alert('Inventory entry failed.');
  }
}

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

onMounted(loadOrders)
</script>

<style scoped>
.image-wrap { position: relative; width: 100%; aspect-ratio: 16/9; background: #f3f4f6; }
.image-wrap img { width: 100%; height: 100%; object-fit: cover; display:block; }
.status-badge { position: absolute; left: 12px; top: 12px; background: rgba(0,0,0,0.6); color: #fff; padding: 2px 8px; border-radius: 999px; font-size: 12px; }
</style>
