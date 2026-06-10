<template>
  <el-container class="app-shell">
    <el-aside width="232px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">差</div>
        <div>
          <div class="brand-title">差旅报销</div>
          <div class="brand-subtitle">Travel Expense</div>
        </div>
      </div>
      <el-menu router :default-active="$route.path" class="side-menu">
        <el-menu-item index="/reimbursements">
          <el-icon><Document /></el-icon>
          <span>报销单主界面</span>
        </el-menu-item>
        <el-menu-item index="/statistics/personal">
          <el-icon><TrendCharts /></el-icon>
          <span>个人信息统计</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <div class="topbar-title">{{ pageTitle }}</div>
          <div class="topbar-subtitle">{{ pageSubtitle }}</div>
        </div>
      </el-header>
      <el-main class="main-panel">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Document, TrendCharts } from '@element-plus/icons-vue'

const route = useRoute()

const pageTitle = computed(() => {
  if (route.path.startsWith('/statistics')) return '个人信息统计'
  if (route.path.includes('/new')) return '新建差旅报销单'
  if (route.params.reimNo) return '报销单详情'
  return '报销单主界面'
})

const pageSubtitle = computed(() => {
  if (route.path.startsWith('/statistics')) return '按报销人查看金额趋势和费用归属结构'
  if (route.path.includes('/new') || route.params.reimNo) return '维护基础信息、行程、每日补助和费用分摊'
  return '查询、创建、提交、删除和作废差旅报销单'
})
</script>
