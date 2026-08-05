import { defineStore } from 'pinia'
import axios from 'axios'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    token: localStorage.getItem('token') || '',
    isLoggedIn: !!localStorage.getItem('token')
  }),
  getters: {
    getUserInfo: (state) => state.userInfo,
    getToken: (state) => state.token,
    getIsLoggedIn: (state) => state.isLoggedIn
  },
  actions: {
    async login(username, password) {
      try {
        const response = await axios.post('/user/login', { username, password })
        const data = response.data
        if (data.code === 200) {
          this.userInfo = data.data
          this.token = data.token || ''
          this.isLoggedIn = !!this.token
          localStorage.setItem('token', this.token)
          localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
          if (!this.token) {
            return { ok: false, errorCode: 'TOKEN_MISSING', message: data.message || '' }
          }
          return { ok: true, errorCode: '', message: data.message || '' }
        }
        return { ok: false, errorCode: data.errorCode || 'INVALID_CREDENTIALS', message: data.message || '' }
      } catch (error) {
        console.error('Login error:', error)
        const data = error.response && error.response.data
        return {
          ok: false,
          errorCode: data && data.errorCode ? data.errorCode : (error.code === 'ECONNABORTED' ? 'TIMEOUT' : 'NETWORK'),
          message: (data && data.message) || ''
        }
      }
    },
    async register(userData) {
      try {
        const response = await axios.post('/user/register', userData)
        return {
          ok: response.data.code === 200,
          errorCode: response.data.errorCode || '',
          message: response.data.message || ''
        }
      } catch (error) {
        console.error('Register error:', error)
        const data = error.response && error.response.data
        return {
          ok: false,
          errorCode: data && data.errorCode ? data.errorCode : (error.code === 'ECONNABORTED' ? 'TIMEOUT' : 'NETWORK'),
          message: (data && data.message) || ''
        }
      }
    },
    logout() {
      this.userInfo = null
      this.token = ''
      this.isLoggedIn = false
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    },
    loadUserInfo() {
      const savedUserInfo = localStorage.getItem('userInfo')
      if (savedUserInfo) {
        try {
          this.userInfo = JSON.parse(savedUserInfo)
        } catch {
          this.userInfo = null
        }
      }
    }
  }
})
