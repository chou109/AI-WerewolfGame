<template>
  <el-container>
    <el-header>
      <div class="header-content">
        <div class="logo">
          <h1>AI狼人杀游戏</h1>
        </div>
        <div class="nav">
          <el-menu :default-active="activeIndex" class="el-menu-demo" mode="horizontal" @select="handleSelect">
            <el-menu-item index="roomList">房间列表</el-menu-item>
            <el-menu-item index="createRoom">创建房间</el-menu-item>
            <el-menu-item index="apiConfig">API配置</el-menu-item>
            <el-menu-item index="voiceConfig">语音配置</el-menu-item>
            <el-menu-item index="gameRecords">游戏记录</el-menu-item>
          </el-menu>
        </div>
        <div class="user-info">
          <template v-if="userStore.getIsLoggedIn">
            <el-dropdown>
              <span class="user-name">{{ userStore.getUserInfo?.nickname || userStore.getUserInfo?.username }}</span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>个人中心</el-dropdown-item>
                  <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" @click="goToLogin">登录</el-button>
            <el-button type="info" @click="goToRegister">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
    <el-footer>
      <div class="footer-content">
        <p>© 2026 AI狼人杀游戏 版权所有</p>
        <p>基于Spring Boot + Vue 3开发</p>
      </div>
    </el-footer>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const activeIndex = ref('roomList')

onMounted(() => {
  userStore.loadUserInfo()
  updateActiveIndex()
})

const updateActiveIndex = () => {
  const path = route.path
  if (path.includes('room/list')) {
    activeIndex.value = 'roomList'
  } else if (path.includes('room/create')) {
    activeIndex.value = 'createRoom'
  }
}

const handleSelect = (key) => {
  activeIndex.value = key
  switch (key) {
    case 'roomList':
      router.push('/game/room/list')
      break
    case 'createRoom':
      router.push('/game/room/create')
      break
    case 'apiConfig':
      router.push('/api')
      break
    case 'voiceConfig':
      router.push('/voice')
      break
    case 'gameRecords':
      router.push('/records')
      break
  }
}

const goToLogin = () => {
  router.push('/login')
}

const goToRegister = () => {
  router.push('/register')
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 100%;
}

.logo h1 {
  margin: 0;
  font-size: 24px;
  color: #fff;
}

.nav {
  flex: 1;
  margin: 0 40px;
}

.user-name {
  color: #fff;
  cursor: pointer;
  margin-right: 10px;
}

.footer-content {
  text-align: center;
}

.footer-content p {
  margin: 5px 0;
  color: #909399;
  font-size: 14px;
}
</style>
