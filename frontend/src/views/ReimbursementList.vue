<template>
  <div>
    <div class="toolbar">
      <el-form :model="query" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="报销单号">
              <el-input v-model="query.reimNo" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="标题">
              <el-input v-model="query.title" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="事由">
              <el-input v-model="query.reason" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="单据状态">
              <el-select v-model="query.billStatus" clearable>
                <el-option v-for="item in billStatuses" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="费用归属公司">
              <el-select v-model="query.reimCompanyName" clearable filterable>
                <el-option v-for="item in companies" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="报销部门">
              <el-select v-model="query.reimDepartmentName" clearable filterable>
                <el-option v-for="item in departments" :key="item.id" :label="`${item.name} ${item.no}`" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="报销人">
              <el-select v-model="selectedEmployeeId" clearable filterable @change="selectEmployee">
                <el-option v-for="item in employees" :key="item.id" :label="`${item.name} ${item.no}`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="业务类型">
              <el-cascader
                v-model="selectedBusinessTypeId"
                :options="businessTypeTree"
                :props="businessTypeProps"
                clearable
                filterable
                @change="selectBusinessType"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="table-actions">
          <el-button :icon="Refresh" @click="reset">清除</el-button>
          <el-button type="primary" :icon="Search" @click="load">搜索</el-button>
          <el-button type="success" :icon="Plus" @click="router.push('/reimbursements/new')">新增</el-button>
        </div>
      </el-form>
    </div>

    <div class="section">
      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column label="序号" width="72" align="center">
          <template #default="{ $index }">{{ rowNo($index) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="132" align="center">
          <template #default="{ row }">
            <el-tooltip content="查看" placement="top">
              <el-button class="icon-action" link type="primary" :icon="Reading" @click="view(row.reimNo)" />
            </el-tooltip>
            <el-tooltip :content="editTooltip(row)" placement="top">
              <el-button
                class="icon-action"
                link
                :type="row.billStatus === 'DRAFT' && !row.lockHolderName ? 'primary' : 'info'"
                :icon="EditPen"
                :disabled="row.billStatus !== 'DRAFT' || !!row.lockHolderName"
                @click="edit(row.reimNo)"
              />
            </el-tooltip>
            <el-dropdown trigger="click" @command="command => handleMore(command, row)">
              <el-button class="icon-action" link type="primary" :icon="MoreFilled" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="delete" :disabled="row.billStatus !== 'DRAFT'">删除</el-dropdown-item>
                  <el-dropdown-item command="push" disabled>手工推送</el-dropdown-item>
                  <el-dropdown-item command="copy">复制</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <el-table-column prop="reimNo" label="报销单号" min-width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row.reimNo)">{{ row.reimNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="billStatusName" label="单据状态" width="100" />
        <el-table-column label="报销人" min-width="130">
          <template #default="{ row }">{{ withNo(row.reimburserName, row.reimburserNo) }}</template>
        </el-table-column>
        <el-table-column label="报销部门" min-width="170">
          <template #default="{ row }">{{ withNo(row.reimDepartmentName, row.reimDepartmentNo) }}</template>
        </el-table-column>
        <el-table-column prop="reimCompanyNames" label="费用归属公司" min-width="190" show-overflow-tooltip />
        <el-table-column prop="businessTypeName" label="业务类型" min-width="130" />
        <el-table-column prop="title" label="报销标题" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row.reimNo)">{{ row.title }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="报销事由" min-width="180" show-overflow-tooltip />
        <el-table-column prop="allowanceAmount" label="补助金额" width="120" align="right">
          <template #default="{ row }">{{ money(row.allowanceAmount) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
      </el-table>
      <el-pagination
        class="pager"
        layout="total, sizes, prev, pager, next"
        :total="total"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        @change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, MoreFilled, Plus, Reading, Refresh, Search } from '@element-plus/icons-vue'
import { billStatuses, businessTypeTree, companies, departments, employees, findBusinessType } from '../data/options'
import { copyReimbursement, deleteDraft, pageReimbursements } from '../api/reimbursement'

const router = useRouter()

// 列表数据及加载状态。
const loading = ref(false)
const records = ref([])
const total = ref(0)
const selectedEmployeeId = ref('')
const selectedBusinessTypeId = ref('')
const businessTypeProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  emitPath: false
}

// 查询对象字段与后端 ReimbursementPageQuery 保持一致。
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  reimNo: '',
  title: '',
  reason: '',
  reimCompanyName: '',
  reimDepartmentName: '',
  reimburserKeyword: '',
  businessTypeName: '',
  billStatus: ''
})

/** 按当前页和每页条数计算跨页连续序号。 */
function rowNo(index) {
  return (query.pageNum - 1) * query.pageSize + index + 1
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function withNo(name, no) {
  if (!name) return ''
  return no ? `${name} ${no}` : name
}

function selectEmployee(id) {
  // 下拉保存员工 ID，发给后端的查询值使用姓名。
  const employee = employees.find(item => item.id === id)
  query.reimburserKeyword = employee ? employee.name : ''
}

function selectBusinessType(id) {
  // 级联选择器保存叶子 ID，后端当前按业务类型名称精确查询。
  const business = findBusinessType(id)
  query.businessTypeName = business ? business.name : ''
}

async function load() {
  // 查询期间显示表格 loading，无论成功失败都在 finally 中关闭。
  loading.value = true
  try {
    const data = await pageReimbursements(query)
    records.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function reset() {
  // 同时清空控件选择值和真正发送给后端的查询对象。
  selectedEmployeeId.value = ''
  selectedBusinessTypeId.value = ''
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    reimNo: '',
    title: '',
    reason: '',
    reimCompanyName: '',
    reimDepartmentName: '',
    reimburserKeyword: '',
    businessTypeName: '',
    billStatus: ''
  })
  load()
}

function view(reimNo) {
  router.push(`/reimbursements/${reimNo}`)
}

function edit(reimNo) {
  router.push(`/reimbursements/${reimNo}?edit=true`)
}

function editTooltip(row) {
  if (row.lockHolderName) return row.lockHolderName + ' 正在编辑'
  if (row.billStatus !== 'DRAFT') return '非草稿不可修改'
  return '修改'
}

async function handleMore(command, row) {
  // 下拉菜单命令在此统一分发，手工推送目前为禁用占位项。
  if (command === 'delete') {
    await remove(row.reimNo)
  }
  if (command === 'copy') {
    await copy(row.reimNo)
  }
}

async function remove(reimNo) {
  // 删除是不可恢复操作，必须经过二次确认。
  await ElMessageBox.confirm('确认删除该草稿单据？', '删除确认', { type: 'warning' })
  await deleteDraft(reimNo)
  ElMessage.success('删除成功')
  load()
}

async function copy(reimNo) {
  // 后端会生成新单号并把复制结果固定为草稿。
  await ElMessageBox.confirm('确认复制该报销单并生成新的草稿单？', '复制确认', { type: 'info' })
  const data = await copyReimbursement(reimNo)
  ElMessage.success(`复制成功，新单号：${data.reimNo}`)
  load()
}

// 页面首次进入时自动加载第一页数据。
onMounted(load)
</script>

<style scoped>
.pager {
  justify-content: flex-end;
  margin-top: 14px;
}

.icon-action {
  width: 26px;
  height: 26px;
  margin: 0 2px;
  font-size: 16px;
}

:deep(.el-cascader) {
  width: 100%;
}
</style>
