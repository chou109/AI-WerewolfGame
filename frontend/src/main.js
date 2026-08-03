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

// 请求拦截器
axios.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    // 只有在没有设置Authorization头的情况下才设置
    if (token && !config.headers.Authorization) {
      config.headers.Authorization = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

const app = createApp(App)

app.use(router)
app.use(pinia)
app.use(ElementPlus)
app.use(i18n)

// 响应拦截器
axios.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response && error.response.status === 401) {
      // 未授权，跳转到登录页
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

app.mount('#app')
