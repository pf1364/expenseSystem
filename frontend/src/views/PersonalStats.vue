<template>
  <div>
    <div class="toolbar stats-filter">
      <el-form inline>
        <el-form-item label="报销人">
          <el-select v-model="selectedName" @change="load">
            <el-option v-for="item in employees" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <el-row :gutter="14" class="metric-row">
      <el-col :span="6">
        <div class="metric"><span>报销单总数</span><strong>{{ stats.totalCount }}</strong></div>
      </el-col>
      <el-col :span="6">
        <div class="metric"><span>累计报销金额</span><strong>{{ money(stats.totalAmount) }}</strong></div>
      </el-col>
      <el-col :span="6">
        <div class="metric"><span>已提交数量</span><strong>{{ stats.submittedCount }}</strong></div>
      </el-col>
      <el-col :span="6">
        <div class="metric"><span>草稿数量</span><strong>{{ stats.draftCount }}</strong></div>
      </el-col>
    </el-row>

    <el-row :gutter="14">
      <el-col :span="14">
        <div class="section">
          <h3 class="section-title">月度报销金额</h3>
          <div ref="barRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="section">
          <h3 class="section-title">费用归属公司结构</h3>
          <div ref="pieRef" class="chart"></div>
        </div>
      </el-col>
    </el-row>

    <div class="section">
      <h3 class="section-title">最近报销单</h3>
      <el-table :data="stats.recentBills" border stripe>
        <el-table-column prop="reimNo" label="报销单号" min-width="160" />
        <el-table-column prop="billStatusName" label="状态" width="96" />
        <el-table-column prop="reimCompanyNames" label="费用归属公司" min-width="180" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="allowanceAmount" label="金额" width="120" align="right">
          <template #default="{ row }">{{ money(row.allowanceAmount) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { employees } from '../data/options'
import { personalStatistics } from '../api/reimbursement'

const selectedName = ref(employees[0].name)
const barRef = ref()
const pieRef = ref()
let barChart
let pieChart

const stats = reactive({
  totalCount: 0,
  draftCount: 0,
  submittedCount: 0,
  totalAmount: 0,
  monthlyAmounts: [],
  companyShares: [],
  recentBills: []
})

function money(value) {
  return Number(value || 0).toFixed(2)
}

async function load() {
  const employee = employees.find(item => item.name === selectedName.value)
  const data = await personalStatistics({ reimburserName: selectedName.value, reimburserNo: employee?.no })
  Object.assign(stats, data)
  await nextTick()
  renderCharts()
}

function renderCharts() {
  barChart ||= echarts.init(barRef.value)
  pieChart ||= echarts.init(pieRef.value)
  const months = (stats.monthlyAmounts || []).map(item => item.month)
  const amounts = (stats.monthlyAmounts || []).map(item => Number(item.amount || 0))
  barChart.setOption({
    color: ['#1f9d8a'],
    tooltip: { trigger: 'axis' },
    grid: { left: 42, right: 18, top: 24, bottom: 34 },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: amounts, barMaxWidth: 42 }]
  })
  pieChart.setOption({
    color: ['#1f9d8a', '#e4a93c', '#536dfe', '#d95d66', '#6f7e8c'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      data: (stats.companyShares || []).map(item => ({ name: item.companyName, value: Number(item.amount || 0) }))
    }]
  })
}

function resizeCharts() {
  barChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.stats-filter {
  display: flex;
  align-items: center;
}

.metric-row {
  margin-bottom: 14px;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 92px;
  padding: 18px;
  background: #fff;
  border: 1px solid #e5ebf1;
  border-radius: 8px;
}

.metric span {
  color: #6f7e8c;
  font-size: 13px;
}

.metric strong {
  color: #17202a;
  font-size: 26px;
}
</style>
