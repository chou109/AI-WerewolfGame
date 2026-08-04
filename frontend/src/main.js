import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import axios from 'axios'
import i18n from './i18n'

// 配置axios
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'
axios.defaults.timeout = 10000
axios.defaults.headers.common['Content-Type'] = 'application/json'

// 请求拦截器：自动附带登录 token
axios.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token && !config.headers.Authorization) {
      config.headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

const app = createApp(App)

app.use(router)
app.use(pinia)
app.use(ElementPlus)
app.use(i18n)

// 响应拦截器
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401 && router.currentRoute.value.path !== '/login') {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    }
    return Promise.reject(error)
  }
)

app.mount('#app')
