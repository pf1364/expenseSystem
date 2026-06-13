<template>
  <div class="reim-form-page">
    <div class="bill-header">
      <h2>差旅费用报销单</h2>
      <span>日期：{{ today }}</span>
    </div>

    <form-section title="基础信息" v-model:expanded="basicExpanded">
      <el-form :model="form" label-width="112px" :disabled="readonly">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="报销标题">
              <el-input v-model="form.title" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报销人">
              <el-select v-model="form.reimburserId" filterable clearable @change="selectEmployee">
                <el-option v-for="item in employees" :key="item.id" :label="`${item.name} ${item.no}`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报销部门">
              <el-select v-model="form.reimDepartmentId" filterable clearable @change="selectDepartment">
                <el-option v-for="item in departments" :key="item.id" :label="`${item.name} ${item.no}`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用归属公司" required>
              <el-select v-model="selectedCompanyId" filterable clearable @change="selectBaseCompany">
                <el-option v-for="item in companies" :key="item.id" :label="`${item.name} ${item.no}`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务类型" required>
              <el-cascader
                v-model="form.businessTypeId"
                :options="businessTypeTree"
                :props="businessTypeProps"
                clearable
                filterable
                @change="selectBusiness"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出差事由">
              <el-input v-model="form.reason" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </form-section>

    <form-section title="补录行程" v-model:expanded="itineraryExpanded">
      <template #actions>
        <el-button v-if="!readonly" type="primary" :icon="Plus" @click="openItineraryDialog()">补录行程</el-button>
      </template>
      <el-table :data="form.itineraries" border>
        <el-table-column label="序号" width="72" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="出行人员" min-width="140">
          <template #default="{ row }">{{ withNo(row.travelerName, row.travelerNo) }}</template>
        </el-table-column>
        <el-table-column label="出差日期" min-width="180">
          <template #default="{ row }">{{ dateRange(row) }}</template>
        </el-table-column>
        <el-table-column label="行程" min-width="160">
          <template #default="{ row }">{{ routeText(row) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="行程说明" min-width="200" show-overflow-tooltip />
        <el-table-column v-if="!readonly" label="操作" width="126" align="center">
          <template #default="{ row, $index }">
            <el-tooltip content="修改" placement="top">
              <el-button class="icon-action" link type="primary" :icon="EditPen" @click="openItineraryDialog(row, $index)" />
            </el-tooltip>
            <el-tooltip content="复制" placement="top">
              <el-button class="icon-action" link type="primary" :icon="CopyDocument" @click="copyItinerary(row)" />
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-button class="icon-action" link type="danger" :icon="Delete" @click="removeItinerary($index)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </form-section>

    <form-section title="补助信息" v-model:expanded="allowanceExpanded">
      <el-tooltip effect="dark" placement="top" :content="allowanceTip">
        <div class="allowance-tip">{{ allowanceTip }}</div>
      </el-tooltip>
      <el-table :data="form.itineraries" border>
        <el-table-column label="序号" width="72" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="出行人" min-width="140">
          <template #default="{ row }">{{ withNo(row.travelerName, row.travelerNo) }}</template>
        </el-table-column>
        <el-table-column label="出差日期" min-width="180">
          <template #default="{ row }">{{ dateRange(row) }}</template>
        </el-table-column>
        <el-table-column label="补助天数" width="100" align="right">
          <template #default="{ row }">{{ row.days || dayCount(row) }}</template>
        </el-table-column>
        <el-table-column label="行程" min-width="150">
          <template #default="{ row }">{{ routeText(row) }}</template>
        </el-table-column>
        <el-table-column label="补助城市" min-width="120">
          <template #default="{ row }">{{ row.endCityName }}</template>
        </el-table-column>
        <el-table-column label="申请金额" width="120" align="right">
          <template #default="{ row }">{{ money(standardTotal(row)) }}</template>
        </el-table-column>
        <el-table-column label="补助金额" width="120" align="right">
          <template #default="{ row }">{{ money(itineraryAmount(row)) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="72" align="center">
          <template #default="{ row }">
            <el-tooltip content="修改" placement="top">
              <el-button class="icon-action" link type="primary" :icon="EditPen" @click="openAllowanceDialog(row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </form-section>

    <form-section title="费用合计" v-model:expanded="totalExpanded">
      <el-row :gutter="14" class="total-row">
        <el-col :span="6">
          <div class="total-item"><span>补助总金额</span><strong>{{ money(totalAmount) }}</strong></div>
        </el-col>
        <el-col :span="6">
          <div class="total-item"><span>餐费补助</span><strong>{{ money(mealTotal) }}</strong></div>
        </el-col>
        <el-col :span="6">
          <div class="total-item"><span>交通补助</span><strong>{{ money(trafficTotal) }}</strong></div>
        </el-col>
        <el-col :span="6">
          <div class="total-item"><span>通讯补助</span><strong>{{ money(communicationTotal) }}</strong></div>
        </el-col>
      </el-row>
    </form-section>

    <form-section title="费用归属及分摊" v-model:expanded="allocationExpanded">
      <template #actions>
        <el-button v-if="!readonly" type="primary" :icon="Plus" @click="addAllocation">添加一行</el-button>
      </template>
      <el-table :data="form.allocations" border>
        <el-table-column label="序号" width="72" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="费用归属" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.allocationOwnerId" filterable :disabled="readonly" @change="id => selectAllocationCompany(row, id)">
              <el-option v-for="item in companies" :key="item.id" :label="`${item.name} ${item.no}`" :value="item.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="项目" min-width="180">
          <template #default="{ row }">
            <el-select v-model="row.businessName" :disabled="readonly">
              <el-option v-for="item in businessTypes" :key="item.id" :label="item.name" :value="item.name" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column width="170" align="right">
          <template #header>
            <div class="ratio-header">
              <span>分摊比例</span>
              <el-tooltip content="均摊" placement="top">
                <el-button link type="primary" :icon="Refresh" :disabled="readonly || form.allocations.length === 0" @click="splitAllocationsEvenly" />
              </el-tooltip>
            </div>
          </template>
          <template #default="{ row, $index }">
            <el-input-number
              v-model="row.allocationRatio"
              :min="0"
              :max="100"
              :precision="2"
              :controls="false"
              :disabled="readonly || $index === 0"
              @change="value => updateAllocationRatio($index, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="分摊金额" width="170" align="right">
          <template #default="{ row }">
            <el-input-number v-model="row.allocationAmount" :min="0" :precision="2" :controls="false" disabled />
          </template>
        </el-table-column>
        <el-table-column v-if="!readonly" label="操作" width="90" align="center">
          <template #default="{ $index }">
            <el-tooltip content="删除" placement="top">
              <el-button class="icon-action" link type="danger" :icon="Delete" @click="removeAllocation($index)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <div class="amount-summary">
        <span>合计</span>
        <span>分摊比例 <strong>{{ allocationRatioTotal.toFixed(2) }}%</strong></span>
        <span>分摊金额 <strong>CNY {{ money(allocationAmountTotal) }}</strong></span>
      </div>
    </form-section>

    <div class="form-footer">
      <el-button :icon="Back" @click="router.push('/reimbursements')">返回</el-button>
      <el-button v-if="!readonly" :icon="DocumentChecked" @click="saveDraft">保存草稿</el-button>
      <el-button v-if="!readonly" type="primary" :icon="Upload" @click="submit">提交</el-button>
    </div>

    <el-dialog v-model="itineraryDialog.visible" title="补录行程" width="760px" destroy-on-close>
      <div class="dialog-tip">
        仅可补录未从申请单带入或未产生费用的行程信息。跨天跨城行程填写说明：出发城市-到达城市：武汉-北京；出发日期-到达日期：1号-5号；1号~5号补助按北京匹配。
      </div>
      <el-form :model="itineraryDialog.form" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="出行人" required>
              <el-select v-model="itineraryDialog.form.travelerId" filterable @change="selectDialogTraveler">
                <el-option v-for="item in employees" :key="item.id" :label="`${item.name} ${item.no}`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出发城市" required>
              <el-select v-model="itineraryDialog.form.startCityName" filterable @change="city => selectDialogCity(city, 'start')">
                <el-option v-for="city in cities" :key="city.cityCode" :label="city.cityName" :value="city.cityName" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到达城市" required>
              <el-select v-model="itineraryDialog.form.endCityName" filterable @change="city => selectDialogCity(city, 'end')">
                <el-option v-for="city in cities" :key="city.cityCode" :label="city.cityName" :value="city.cityName" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出发日期" required>
              <el-date-picker v-model="itineraryDialog.form.startDate" value-format="YYYY-MM-DD" type="date" :disabled-date="disabledFutureDate" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到达日期" required>
              <el-date-picker v-model="itineraryDialog.form.endDate" value-format="YYYY-MM-DD" type="date" :disabled-date="disabledFutureDate" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="行程说明" required>
              <el-input v-model="itineraryDialog.form.description" type="textarea" maxlength="500" show-word-limit :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="itineraryDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveItineraryDialog">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="allowanceDialog.visible" title="补助日历" width="1080px" class="allowance-dialog" destroy-on-close>
      <div v-if="allowanceDialog.itinerary" class="allowance-calendar">
        <aside class="calendar-side">
          <div class="side-title">出差类型 <span>{{ form.businessTypeName || '-' }}</span></div>
          <div class="date-card">
            <div class="date-line">
              <span>开始日期</span>
              <strong>{{ allowanceDialog.itinerary.startDate }}</strong>
            </div>
            <div class="date-pill">{{ dateRange(allowanceDialog.itinerary) }}　{{ dayCount(allowanceDialog.itinerary) }}天</div>
            <div class="date-line">
              <span>结束日期</span>
              <strong>{{ allowanceDialog.itinerary.endDate }}</strong>
            </div>
          </div>
          <div class="side-totals">
            <div><span>补助金额</span><b>CNY {{ money(itineraryAmount(allowanceDialog.itinerary)) }}</b></div>
            <div><span>标准总额</span><b>CNY {{ money(standardTotal(allowanceDialog.itinerary)) }}</b></div>
            <div><span>餐补金额</span><b>CNY {{ money(itineraryMealAmount(allowanceDialog.itinerary)) }}</b></div>
            <div><span>交补金额</span><b>CNY {{ money(itineraryTrafficAmount(allowanceDialog.itinerary)) }}</b></div>
            <div><span>通讯金额</span><b>CNY {{ money(itineraryCommunicationAmount(allowanceDialog.itinerary)) }}</b></div>
          </div>
        </aside>

        <section class="calendar-main">
          <div class="calendar-title">
            <div class="calendar-title-left">
              <el-checkbox :model-value="isAllSelected()" :indeterminate="isAllIndeterminate()" @change="toggleAll" />
              <span>出差补助</span>
            </div>
          </div>
          <el-table :data="allowanceDialog.itinerary.allowanceDays" border size="small">
            <el-table-column label="日期/星期" width="150">
              <template #default="{ row }">
                <div class="day-cell">
                  <div>
                    <span>{{ row.allowanceDate }}</span>
                    <small>{{ row.weekName }}</small>
                  </div>
                  <el-checkbox :model-value="isDayAllSelected(row)" :indeterminate="isDayIndeterminate(row)" @change="value => toggleDay(row, value)" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="cityName" label="补助城市" width="110" />
            <el-table-column min-width="190">
              <template #header>
                <header-check label="餐费补助" :checked="isTypeAllSelected('meal')" :indeterminate="isTypeIndeterminate('meal')" @change="value => toggleType('meal', value)" />
              </template>
              <template #default="{ row }">
                <allowance-input :row="row" type="meal" :readonly="readonly" @change="calcDay(row)" />
              </template>
            </el-table-column>
            <el-table-column min-width="190">
              <template #header>
                <header-check label="交通补助" :checked="isTypeAllSelected('traffic')" :indeterminate="isTypeIndeterminate('traffic')" @change="value => toggleType('traffic', value)" />
              </template>
              <template #default="{ row }">
                <allowance-input :row="row" type="traffic" :readonly="readonly" @change="calcDay(row)" />
              </template>
            </el-table-column>
            <el-table-column min-width="190">
              <template #header>
                <header-check label="通讯补助" :checked="isTypeAllSelected('communication')" :indeterminate="isTypeIndeterminate('communication')" @change="value => toggleType('communication', value)" />
              </template>
              <template #default="{ row }">
                <allowance-input :row="row" type="communication" :readonly="readonly" @change="calcDay(row)" />
              </template>
            </el-table-column>
          </el-table>
        </section>
      </div>
      <template #footer>
        <el-button @click="allowanceDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="allowanceDialog.visible = false">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElButton, ElCheckbox, ElInputNumber, ElMessage, ElMessageBox } from 'element-plus'
import { Back, CopyDocument, Delete, DocumentChecked, EditPen, Plus, Refresh, Upload } from '@element-plus/icons-vue'
import { businessTypeTree, businessTypes, companies, departments, employees, fallbackCities, findBusinessType } from '../data/options'
import { createAndSubmit, createDraft, generateAllowanceDays, getReimbursement, listCityAllowances, submitDraft, updateReimbursement } from '../api/reimbursement'

const props = defineProps({
  reimNo: String
})

const router = useRouter()

// 城市标准由后端加载；人员、部门、公司和业务类型按训练营要求使用前端固定选项。
const cities = ref([])
const selectedCompanyId = ref('')

// 五个业务分区分别维护展开/折叠状态。
const basicExpanded = ref(true)
const itineraryExpanded = ref(true)
const allowanceExpanded = ref(true)
const totalExpanded = ref(true)
const allocationExpanded = ref(true)

// form 是新增、编辑、保存和提交共同使用的页面数据源。
const form = reactive(emptyForm())
const today = new Date().toLocaleDateString('zh-CN')
const allowanceTip = '1、请根据实际出差日期选择补助 2、出差期间当日有用餐安排的请自行核减当日餐补 3、出差期间当日有用车的，请自行核减当日交补'
const businessTypeProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  emitPath: false
}
const itineraryDialog = reactive({
  visible: false,
  index: -1,
  form: emptyItinerary()
})
const allowanceDialog = reactive({
  visible: false,
  itinerary: null
})

// 已存在且状态不是草稿的报销单只允许查看。
const readonly = computed(() => Boolean(form.reimNo && form.billStatus && form.billStatus !== 'DRAFT'))

// 页面合计用于实时交互展示；最终金额仍由后端按城市标准重新校验和汇总。
const allAllowanceDays = computed(() => form.itineraries.flatMap(item => item.allowanceDays || []))
const totalAmount = computed(() => allAllowanceDays.value.reduce((sum, day) => sum + Number(day.dayAmount || 0), 0))
const mealTotal = computed(() => allAllowanceDays.value.reduce((sum, day) => sum + Number(day.mealAmount || 0), 0))
const trafficTotal = computed(() => allAllowanceDays.value.reduce((sum, day) => sum + Number(day.trafficAmount || 0), 0))
const communicationTotal = computed(() => allAllowanceDays.value.reduce((sum, day) => sum + Number(day.communicationAmount || 0), 0))
const allocationRatioTotal = computed(() => form.allocations.reduce((sum, item) => sum + Number(item.allocationRatio || 0), 0))
const allocationAmountTotal = computed(() => form.allocations.reduce((sum, item) => sum + Number(item.allocationAmount || 0), 0))

// 通用分区组件统一处理标题、右侧操作按钮和折叠箭头。
const FormSection = defineComponent({
  props: {
    title: String,
    expanded: Boolean
  },
  emits: ['update:expanded'],
  setup(sectionProps, { emit, slots }) {
    return () => h('div', { class: 'section' }, [
      h('div', { class: 'section-head compact' }, [
        h('div', { class: 'section-title-wrap' }, [
          h('h3', { class: 'section-title' }, sectionProps.title)
        ]),
        h('div', { class: 'section-actions' }, [
          ...(slots.actions?.() || [])
        ]),
        h(ElButton, {
          link: true,
          class: 'collapse-btn',
          onClick: () => emit('update:expanded', !sectionProps.expanded)
        }, () => sectionProps.expanded ? '^' : 'v')
      ]),
      sectionProps.expanded ? h('div', { class: 'section-body' }, slots.default?.()) : null
    ])
  }
})

// 补助日历列头复选框，支持全选、取消和半选状态。
const HeaderCheck = defineComponent({
  props: {
    label: String,
    checked: Boolean,
    indeterminate: Boolean
  },
  emits: ['change'],
  setup(headerProps, { emit }) {
    return () => h('div', { class: 'header-check' }, [
      h('span', headerProps.label),
      h(ElCheckbox, {
        modelValue: headerProps.checked,
        indeterminate: headerProps.indeterminate,
        'onUpdate:modelValue': value => emit('change', value)
      })
    ])
  }
})

// 单项补助输入控件：标准只读，用户只能取消或在标准范围内调低金额。
const AllowanceInput = defineComponent({
  props: {
    row: Object,
    type: String,
    readonly: Boolean
  },
  emits: ['change'],
  setup(inputProps, { emit }) {
    const fields = {
      meal: ['mealSelected', 'mealAmount', 'mealStandard'],
      traffic: ['trafficSelected', 'trafficAmount', 'trafficStandard'],
      communication: ['communicationSelected', 'communicationAmount', 'communicationStandard']
    }
    return () => {
      const [selectedField, amountField, standardField] = fields[inputProps.type]
      return h('div', { class: 'allowance-input' }, [
        h('div', { class: 'standard-line' }, `CNY ${money(inputProps.row[standardField])} / 天`),
        h('div', { class: 'input-line' }, [
          h(ElCheckbox, {
            modelValue: inputProps.row[selectedField] === 1,
            disabled: inputProps.readonly,
            'onUpdate:modelValue': value => {
              setItemSelected(inputProps.row, inputProps.type, value)
              emit('change')
            }
          }),
          h(ElInputNumber, {
            modelValue: Number(inputProps.row[amountField] || 0),
            min: 0,
            max: Number(inputProps.row[standardField] || 0),
            precision: 2,
            controls: false,
            disabled: inputProps.readonly || inputProps.row[selectedField] !== 1,
            'onUpdate:modelValue': value => {
              inputProps.row[amountField] = Number(value || 0)
              emit('change')
            }
          })
        ])
      ])
    }
  }
})

/** 创建新增页面使用的空报销单模型。 */
function emptyForm() {
  return {
    reimNo: '',
    billStatus: '',
    title: '',
    reimburserId: '',
    reimburserNo: '',
    reimburserName: '',
    reimDepartmentId: '',
    reimDepartmentNo: '',
    reimDepartmentName: '',
    reimCompanyNames: '',
    businessTypeId: '',
    businessTypeNo: '',
    businessTypeName: '',
    reason: '',
    itineraries: [],
    allocations: []
  }
}

/** 创建行程弹窗使用的空行程模型。 */
function emptyItinerary() {
  return {
    localKey: '',
    travelerId: '',
    travelerNo: '',
    travelerName: '',
    startCityCode: '',
    startCityName: '',
    endCityCode: '',
    endCityName: '',
    startDate: '',
    endDate: '',
    days: 0,
    routeText: '',
    description: '',
    allowanceDays: []
  }
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function withNo(name, no) {
  if (!name) return ''
  return no ? `${name} ${no}` : name
}

function dateRange(row) {
  return row.startDate && row.endDate ? `${row.startDate} 至 ${row.endDate}` : ''
}

function routeText(row) {
  return row.startCityName && row.endCityName ? `${row.startCityName} - ${row.endCityName}` : ''
}

function allowanceRoute(row) {
  return row.endCityName ? `${row.endCityName} - ${row.endCityName}` : ''
}

function dayCount(row) {
  if (!row.startDate || !row.endDate) return 0
  const start = new Date(row.startDate)
  const end = new Date(row.endDate)
  return Math.floor((end - start) / 86400000) + 1
}

function itineraryAmount(row) {
  return (row.allowanceDays || []).reduce((sum, day) => sum + Number(day.dayAmount || 0), 0)
}

function itineraryMealAmount(row) {
  return (row.allowanceDays || []).reduce((sum, day) => sum + Number(day.mealAmount || 0), 0)
}

function itineraryTrafficAmount(row) {
  return (row.allowanceDays || []).reduce((sum, day) => sum + Number(day.trafficAmount || 0), 0)
}

function itineraryCommunicationAmount(row) {
  return (row.allowanceDays || []).reduce((sum, day) => sum + Number(day.communicationAmount || 0), 0)
}

function standardTotal(row) {
  return (row.allowanceDays || []).reduce((sum, day) => {
    const meal = day.mealSelected === 1 ? Number(day.mealStandard || 0) : 0
    const traffic = day.trafficSelected === 1 ? Number(day.trafficStandard || 0) : 0
    const communication = day.communicationSelected === 1 ? Number(day.communicationStandard || 0) : 0
    return sum + meal + traffic + communication
  }, 0)
}

function selectEmployee(id) {
  const employee = employees.find(item => item.id === id)
  form.reimburserNo = employee?.no || ''
  form.reimburserName = employee?.name || ''
}

function selectDepartment(id) {
  const department = departments.find(item => item.id === id)
  form.reimDepartmentNo = department?.no || ''
  form.reimDepartmentName = department?.name || ''
}

function selectBaseCompany(id) {
  // 基础信息选择公司后，如果还没有分摊，则自动建立一条 100% 公司分摊。
  const company = companies.find(item => item.id === id)
  form.reimCompanyNames = company?.name || ''
  if (company && form.allocations.length === 0) {
    form.allocations.push(newAllocation(company, 100))
    recalcAllocations()
  }
}

function selectBusiness(id) {
  const business = findBusinessType(id)
  form.businessTypeNo = business?.no || ''
  form.businessTypeName = business?.name || ''
  form.allocations.forEach(item => {
    if (!item.businessName) {
      item.businessId = form.businessTypeId
      item.businessName = form.businessTypeName
    }
  })
}

function selectAllocationCompany(row, id) {
  // 同时保存公司 ID、编号和名称，后端分摊表需要这些字段。
  const company = companies.find(item => item.id === id)
  row.allocationOwnerType = 'COMPANY'
  row.allocationOwnerNo = company?.no || ''
  row.allocationOwnerName = company?.name || ''
}

function openItineraryDialog(row, index = -1) {
  // 修改时深拷贝行程，取消弹窗不会污染列表中的原数据。
  itineraryDialog.index = index
  itineraryDialog.form = row ? JSON.parse(JSON.stringify(row)) : {
    ...emptyItinerary(),
    localKey: crypto.randomUUID(),
    travelerId: form.reimburserId,
    travelerNo: form.reimburserNo,
    travelerName: form.reimburserName
  }
  itineraryDialog.visible = true
}

function copyItinerary(row) {
  // 清空数据库主键后复制，保存时后端会把它识别为新行程和新补助明细。
  const copied = JSON.parse(JSON.stringify(row))
  delete copied.id
  copied.localKey = crypto.randomUUID()
  copied.allowanceDays = (copied.allowanceDays || []).map(day => {
    const next = { ...day }
    delete next.id
    return next
  })
  openItineraryDialog(copied, -1)
}

async function removeItinerary(index) {
  await ElMessageBox.confirm('确认删除该补录行程？', '删除确认', { type: 'warning' })
  form.itineraries.splice(index, 1)
  syncAllAllocationAmounts()
}

function selectDialogTraveler(id) {
  const employee = employees.find(item => item.id === id)
  itineraryDialog.form.travelerNo = employee?.no || ''
  itineraryDialog.form.travelerName = employee?.name || ''
}

function selectDialogCity(name, type) {
  const city = cities.value.find(item => item.cityName === name)
  if (!city) return
  if (type === 'start') {
    itineraryDialog.form.startCityCode = city.cityCode
  } else {
    itineraryDialog.form.endCityCode = city.cityCode
  }
}

function disabledFutureDate(date) {
  return date.getTime() > Date.now()
}

async function saveItineraryDialog() {
  // 完成必填校验后，根据目的地和日期生成对应的每日补助。
  const row = itineraryDialog.form
  if (!row.travelerId || !row.startCityName || !row.endCityName || !row.startDate || !row.endDate || !row.description) {
    ElMessage.warning('补录行程所有字段均为必填')
    return
  }
  if (new Date(row.endDate) < new Date(row.startDate)) {
    ElMessage.warning('到达日期不可早于出发日期')
    return
  }
  row.days = dayCount(row)
  row.routeText = routeText(row)
  row.allowanceDays = await buildAllowanceDays(row)
  if (itineraryDialog.index >= 0) {
    form.itineraries.splice(itineraryDialog.index, 1, row)
  } else {
    form.itineraries.push(row)
  }
  syncAllAllocationAmounts()
  itineraryDialog.visible = false
}

async function buildAllowanceDays(row) {
  // 优先使用后端结果；后端会从数据库读取目的地城市补助标准。
  try {
    return await generateAllowanceDays({
      startCityCode: row.startCityCode,
      startCityName: row.startCityName,
      endCityCode: row.endCityCode,
      endCityName: row.endCityName,
      startDate: row.startDate,
      endDate: row.endDate
    })
  } catch (error) {
    const city = cities.value.find(item => item.cityName === row.endCityName)
    return buildFallbackAllowanceDays(row, city)
  }
}

function buildFallbackAllowanceDays(row, city) {
  // 接口不可用时仅用于页面预览；保存时后端仍会重新校验标准和金额。
  const days = []
  const standard = city || fallbackCities[0]
  const start = new Date(row.startDate)
  for (let i = 0; i < dayCount(row); i++) {
    const current = new Date(start)
    current.setDate(start.getDate() + i)
    const dateText = current.toISOString().slice(0, 10)
    const day = {
      allowanceDate: dateText,
      weekName: ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][current.getDay()],
      cityCode: standard.cityCode,
      cityName: standard.cityName,
      cityLevel: standard.cityLevel,
      mealStandard: Number(standard.mealStandard || 0),
      mealSelected: 1,
      mealAmount: Number(standard.mealStandard || 0),
      trafficStandard: Number(standard.trafficStandard || 40),
      trafficSelected: 1,
      trafficAmount: Number(standard.trafficStandard || 40),
      communicationStandard: Number(standard.communicationStandard || 40),
      communicationSelected: 1,
      communicationAmount: Number(standard.communicationStandard || 40)
    }
    calcDay(day)
    days.push(day)
  }
  return days
}

function openAllowanceDialog(row) {
  // 弹窗直接编辑当前行程的 allowanceDays，变更会实时影响费用合计。
  allowanceDialog.itinerary = row
  allowanceDialog.visible = true
}

function selectedFields(type) {
  return {
    meal: ['mealSelected', 'mealAmount', 'mealStandard'],
    traffic: ['trafficSelected', 'trafficAmount', 'trafficStandard'],
    communication: ['communicationSelected', 'communicationAmount', 'communicationStandard']
  }[type]
}

function setItemSelected(row, type, value) {
  // 勾选时保留原金额或恢复标准金额，取消时强制清零。
  const [selectedField, amountField, standardField] = selectedFields(type)
  row[selectedField] = value ? 1 : 0
  row[amountField] = value ? Number(row[amountField] || row[standardField] || 0) : 0
  calcDay(row)
}

function daySelectedCount(row) {
  return ['mealSelected', 'trafficSelected', 'communicationSelected'].filter(field => row[field] === 1).length
}

function isDayAllSelected(row) {
  return daySelectedCount(row) === 3
}

function isDayIndeterminate(row) {
  const count = daySelectedCount(row)
  return count > 0 && count < 3
}

function toggleDay(row, value) {
  // 一次勾选或取消某一天的三种补助。
  setItemSelected(row, 'meal', value)
  setItemSelected(row, 'traffic', value)
  setItemSelected(row, 'communication', value)
  calcDay(row)
}

function typeSelectedCount(type) {
  const [selectedField] = selectedFields(type)
  return allowanceDialog.itinerary?.allowanceDays?.filter(day => day[selectedField] === 1).length || 0
}

function isTypeAllSelected(type) {
  const days = allowanceDialog.itinerary?.allowanceDays || []
  return days.length > 0 && typeSelectedCount(type) === days.length
}

function isTypeIndeterminate(type) {
  const count = typeSelectedCount(type)
  const days = allowanceDialog.itinerary?.allowanceDays || []
  return count > 0 && count < days.length
}

function toggleType(type, value) {
  // 一次勾选或取消所有日期中的同一种补助。
  ;(allowanceDialog.itinerary?.allowanceDays || []).forEach(day => setItemSelected(day, type, value))
}

function selectedTotalCount() {
  return (allowanceDialog.itinerary?.allowanceDays || []).reduce((sum, day) => sum + daySelectedCount(day), 0)
}

function isAllSelected() {
  const days = allowanceDialog.itinerary?.allowanceDays || []
  return days.length > 0 && selectedTotalCount() === days.length * 3
}

function isAllIndeterminate() {
  const count = selectedTotalCount()
  const days = allowanceDialog.itinerary?.allowanceDays || []
  return count > 0 && count < days.length * 3
}

function toggleAll(value) {
  // 控制当前行程所有日期和所有补助项目。
  ;(allowanceDialog.itinerary?.allowanceDays || []).forEach(day => toggleDay(day, value))
}

function calcDay(row) {
  // 未勾选项目按 0 计算，并同步当日合计及分摊金额。
  const meal = row.mealSelected === 1 ? Number(row.mealAmount || 0) : 0
  const traffic = row.trafficSelected === 1 ? Number(row.trafficAmount || 0) : 0
  const communication = row.communicationSelected === 1 ? Number(row.communicationAmount || 0) : 0
  row.dayAmount = Number((meal + traffic + communication).toFixed(2))
  syncAllAllocationAmounts()
}

function addAllocation() {
  // 新增行默认比例和金额为 0，首行随后根据其他行比例自动反算。
  form.allocations.push(newAllocation(null, 0))
  recalcAllocations()
}

function newAllocation(company, ratio) {
  return {
    allocationOwnerType: 'COMPANY',
    allocationOwnerId: company?.id || '',
    allocationOwnerNo: company?.no || '',
    allocationOwnerName: company?.name || '',
    businessId: form.businessTypeId,
    businessName: form.businessTypeName,
    allocationRatio: ratio,
    allocationAmount: Number((totalAmount.value * ratio / 100).toFixed(2)),
    sortNo: form.allocations.length + 1
  }
}

function updateAllocationRatio(index, value) {
  // 第一行不可编辑；第 2 行以后变化时，第一行承担剩余比例。
  if (index === 0) {
    recalcAllocations()
    return
  }
  form.allocations[index].allocationRatio = Number(value || 0)
  const otherTotal = form.allocations.slice(1).reduce((sum, item) => sum + Number(item.allocationRatio || 0), 0)
  if (otherTotal > 100) {
    form.allocations[index].allocationRatio = 0
    ElMessage.warning('第2行及以后分摊比例合计不能超过100%')
  }
  recalcAllocations()
}

function recalcAllocations() {
  // 首行作为差额行，保证页面展示比例合计为 100%。
  if (form.allocations.length === 0) {
    return
  }
  form.allocations.forEach((item, index) => {
    item.allocationOwnerType = 'COMPANY'
    item.sortNo = index + 1
  })
  if (form.allocations.length === 1) {
    form.allocations[0].allocationRatio = 100
  } else {
    const otherTotal = form.allocations.slice(1).reduce((sum, item) => sum + Number(item.allocationRatio || 0), 0)
    form.allocations[0].allocationRatio = Number(Math.max(0, 100 - otherTotal).toFixed(2))
  }
  syncAllAllocationAmounts()
}

function splitAllocationsEvenly() {
  // 以 0.01% 为最小单位均摊，除不尽的尾差放在第一行。
  const count = form.allocations.length
  if (count === 0) return
  const baseCents = Math.floor(10000 / count)
  const firstCents = 10000 - baseCents * (count - 1)
  form.allocations.forEach((item, index) => {
    item.allocationRatio = Number(((index === 0 ? firstCents : baseCents) / 100).toFixed(2))
  })
  syncAllAllocationAmounts()
}

function syncAllocationAmount(row) {
  // 分摊金额由补助总额乘比例得到，页面不允许手工修改。
  row.allocationAmount = Number((totalAmount.value * Number(row.allocationRatio || 0) / 100).toFixed(2))
}

function syncAllAllocationAmounts() {
  form.allocations.forEach(syncAllocationAmount)
}

async function removeAllocation(index) {
  // 至少保留一条分摊信息，多行删除前进行二次确认。
  if (form.allocations.length <= 1) {
    ElMessage.warning('至少保留一条分摊信息')
    return
  }
  await ElMessageBox.confirm('确认删除当前分摊信息？', '删除确认', { type: 'warning' })
  form.allocations.splice(index, 1)
  recalcAllocations()
}

function validateClient() {
  // 前端校验用于及时提示，不能替代后端日期、城市和金额安全校验。
  if (!form.businessTypeId) {
    ElMessage.error('业务类型不能为空')
    return false
  }
  if (!selectedCompanyId.value && !form.reimCompanyNames) {
    ElMessage.error('费用归属公司不能为空')
    return false
  }
  for (const itinerary of form.itineraries) {
    for (const day of itinerary.allowanceDays || []) {
      const checks = [
        ['餐补', day.mealSelected, day.mealAmount, day.mealStandard],
        ['交通补助', day.trafficSelected, day.trafficAmount, day.trafficStandard],
        ['通讯补助', day.communicationSelected, day.communicationAmount, day.communicationStandard]
      ]
      for (const [label, selected, amount, standard] of checks) {
        if (selected === 1 && Number(amount || 0) > Number(standard || 0)) {
          ElMessage.error(`${day.allowanceDate}${label}不能超过标准金额`)
          return false
        }
      }
    }
  }
  return true
}

function payload() {
  // 页面比例按百分数展示，发送前转换为后端和数据库使用的 0-1。
  const data = JSON.parse(JSON.stringify(form))
  data.allocations = (data.allocations || []).map(item => ({
    ...item,
    allocationOwnerType: 'COMPANY',
    allocationRatio: Number((Number(item.allocationRatio || 0) / 100).toFixed(6))
  }))
  return data
}

async function saveDraft() {
  // 新单据调用创建草稿；已有单据调用更新，成功后使用后端单号更新路由。
  if (!validateClient()) return
  recalcAllocations()
  const data = form.reimNo ? await updateReimbursement(form.reimNo, payload()) : await createDraft(payload())
  ElMessage.success('草稿已保存')
  if (!form.reimNo) {
    form.reimNo = data.reimNo
    form.billStatus = data.billStatus
    router.replace(`/reimbursements/${data.reimNo}`)
  }
}

async function submit() {
  // 已有草稿先保存最新修改再提交；新单据直接创建并提交。
  if (!validateClient()) return
  recalcAllocations()
  if (form.reimNo) {
    await updateReimbursement(form.reimNo, payload())
    await submitDraft(form.reimNo)
  } else {
    await createAndSubmit(payload())
  }
  ElMessage.success('提交成功')
  router.push('/reimbursements')
}

async function loadDetail() {
  // 加载详情，并把数据库 0-1 分摊比例转换回页面百分数。
  if (!props.reimNo) {
    return
  }
  const detail = await getReimbursement(props.reimNo)
  Object.assign(form, detail)
  selectedCompanyId.value = companies.find(item => item.name === detail.reimCompanyNames)?.id || ''
  form.itineraries = (detail.itineraries || []).map(item => ({ ...item, localKey: item.id || crypto.randomUUID() }))
  form.allocations = (detail.allocations || []).map(item => ({
    ...item,
    allocationOwnerType: 'COMPANY',
    allocationRatio: Number(item.allocationRatio || 0) * 100
  }))
  recalcAllocations()
}

onMounted(async () => {
  // 先加载城市标准，再根据路由参数决定是否回显已有报销单。
  try {
    const remoteCities = await listCityAllowances()
    cities.value = remoteCities?.length ? remoteCities : fallbackCities
  } catch (error) {
    cities.value = fallbackCities
  }
  await loadDetail()
})
</script>

<style scoped>
.reim-form-page {
  max-width: 1200px;
  margin: 0 auto;
}

.bill-header {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  height: 52px;
  margin-bottom: 12px;
}

.bill-header h2 {
  margin: 0;
  text-align: center;
  font-size: 20px;
}

.bill-header span {
  color: #6f7e8c;
  font-size: 14px;
}

.section-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto 32px;
  align-items: center;
  column-gap: 8px;
  height: 36px;
  margin: -16px -16px 14px;
  padding: 0 4px 0 12px;
  background: #f1f3f6;
  border-bottom: 1px solid #e5ebf1;
}

.section-title-wrap {
  min-width: 0;
}

.section-title {
  margin: 0;
  padding-left: 8px;
  border-left: 3px solid #1f9d8a;
  overflow: hidden;
  font-size: 14px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
}

.collapse-btn {
  width: 32px;
  min-width: 32px;
  padding: 0;
  color: #6f7e8c;
  font-size: 18px;
  font-weight: 700;
}

.icon-action {
  width: 26px;
  height: 26px;
  margin: 0 2px;
  font-size: 16px;
}

.dialog-tip,
.allowance-tip {
  margin-bottom: 12px;
  padding: 10px 12px;
  overflow: hidden;
  color: #7a5a12;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #fff7df;
  border: 1px solid #f1d58c;
  border-radius: 6px;
}

.total-row {
  margin-bottom: 4px;
}

.total-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 78px;
  padding: 14px;
  background: #f6f8fb;
  border: 1px solid #e5ebf1;
  border-radius: 6px;
}

.total-item span {
  color: #6f7e8c;
  font-size: 13px;
}

.total-item strong {
  color: #1f9d8a;
  font-size: 20px;
}

.ratio-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.amount-summary {
  display: flex;
  justify-content: flex-end;
  gap: 42px;
  margin-top: 12px;
  padding: 10px 14px;
  background: #fff7eb;
  border: 1px solid #f5e0bd;
}

.allowance-calendar {
  display: grid;
  grid-template-columns: 220px 1fr;
  min-height: 430px;
  border: 1px solid #e5ebf1;
}

.calendar-side {
  padding: 14px;
  border-right: 1px solid #e5ebf1;
}

.side-title {
  margin-bottom: 12px;
  font-size: 13px;
}

.side-title span {
  margin-left: 10px;
  color: #f06f2f;
}

.date-card {
  padding: 10px;
  background: #f8fbff;
  border: 1px solid #d7e9ff;
  border-radius: 6px;
}

.date-line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
}

.date-pill {
  margin: 8px 0;
  padding: 8px;
  color: #fff;
  text-align: center;
  background: #168bd4;
  border-radius: 4px;
  font-size: 12px;
}

.side-totals {
  margin-top: 20px;
}

.side-totals div {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 12px;
}

.side-totals b {
  color: #f06f2f;
  font-weight: 700;
}

.calendar-main {
  padding: 14px;
}

.calendar-title {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-bottom: 10px;
}

.calendar-title-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.day-cell {
  display: grid;
  grid-template-columns: 1fr 22px;
  gap: 6px;
  align-items: center;
}

.day-cell small {
  display: block;
  margin-top: 2px;
  color: #6f7e8c;
}

.header-check {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.allowance-input {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.standard-line {
  color: #f06f2f;
  font-size: 12px;
}

.input-line {
  display: grid;
  grid-template-columns: 22px 1fr;
  gap: 4px;
  align-items: center;
}

:deep(.el-cascader),
:deep(.el-date-editor.el-input) {
  width: 100%;
}
</style>

<style>
.reim-form-page .section-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto 32px;
  align-items: center;
  column-gap: 8px;
  height: 36px;
  margin: -16px -16px 14px;
  padding: 0 4px 0 12px;
  background: #f1f3f6;
  border-bottom: 1px solid #e5ebf1;
}

.reim-form-page .section-title-wrap {
  min-width: 0;
}

.reim-form-page .section-head .section-title {
  margin: 0;
  padding-left: 8px;
  overflow: hidden;
  font-size: 14px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-left: 3px solid #1f9d8a;
}

.reim-form-page .section-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
}

.reim-form-page .collapse-btn {
  width: 32px;
  min-width: 32px;
  padding: 0;
  color: #6f7e8c;
  font-size: 18px;
  font-weight: 700;
}
</style>
