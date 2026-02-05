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
        const response = await axios.post('/user/login', {
          username,
          password
        })
        if (response.data.code === 200) {
          this.userInfo = response.data.data
          this.token = 'Bearer ' + (response.data.token || '')
          this.isLoggedIn = true
          localStorage.setItem('token', this.token)
          localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
          return true
        }
        return false
      } catch (error) {
        console.error('Login error:', error)
        return false
      }
    },
    async register(userData) {
      try {
        const response = await axios.post('http://localhost:8081/api/user/register', userData)
        return response.data.code === 200
      } catch (error) {
        console.error('Register error:', error)
        return false
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
        this.userInfo = JSON.parse(savedUserInfo)
      }
    }
  }
})
