<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="login-brand-mark">差</div>
        <div>
          <div class="login-brand-title">差旅报销系统</div>
          <div class="login-brand-subtitle">Travel Expense Reimbursement</div>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登  录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f4f7fb;
}

.login-card {
  width: 400px;
  padding: 44px 40px 32px;
  background: #fff;
  border: 1px solid #e5ebf1;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 36px;
  justify-content: center;
}

.login-brand-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #1f9d8a;
  color: #fff;
  font-weight: 700;
  font-size: 20px;
}

.login-brand-title {
  font-size: 20px;
  font-weight: 700;
  color: #17202a;
}

.login-brand-subtitle {
  margin-top: 2px;
  color: #9fb0bf;
  font-size: 12px;
}

.login-form {
  margin-top: 8px;
}

.login-btn {
  width: 100%;
}
</style>
