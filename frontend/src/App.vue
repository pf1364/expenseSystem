<template>
  <el-container class="app-shell">
    <!-- 未登录时不显示侧边栏 -->
    <el-aside v-if="authStore.isLoggedIn" width="232px" class="sidebar">
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

    <!-- 登录页面不显示外层容器，由 Login.vue 自己渲染 -->
    <template v-if="$route.path === '/login'">
      <router-view />
    </template>

    <el-container v-else>
      <el-header class="topbar">
        <div>
          <div class="topbar-title">{{ pageTitle }}</div>
          <div class="topbar-subtitle">{{ pageSubtitle }}</div>
        </div>
        <!-- 右侧用户信息 + 退出 -->
        <div v-if="authStore.isLoggedIn" class="topbar-user">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              <span class="user-name">{{ authStore.user?.displayName || authStore.user?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-panel">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, TrendCharts, UserFilled, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  // 应用启动时检查登录状态
  if (!authStore.isLoggedIn) {
    authStore.checkAuth()
  }
})

async function handleCommand(command) {
  if (command === 'logout') {
    await authStore.logout()
    router.push('/login')
  }
}

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

<style scoped>
.topbar-user {
  margin-left: auto;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #465460;
  font-size: 14px;
}

.user-info:hover {
  color: #1f9d8a;
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
