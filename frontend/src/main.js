import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/main.css'
import App from './App.vue'
import router from './router'

// 创建 Vue 应用并注册状态管理、路由和 Element Plus 组件库。
createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app')
