<template>
  <el-container>
    <el-header>
      <div class="header-content">
        <div class="logo">
          <h1>AI狼人杀游戏</h1>
        </div>
        <div class="nav">
          <el-menu :default-active="activeIndex" class="el-menu-demo" mode="horizontal" @select="handleSelect">
            <el-menu-item index="home">首页</el-menu-item>
            <el-menu-item index="roomList">游戏房间</el-menu-item>
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
      <div class="home-content">
        <div class="banner">
          <h2>欢迎来到AI狼人杀游戏</h2>
          <p>体验全新的AI驱动狼人杀游戏，与智能AI一起享受推理的乐趣</p>
          <div class="banner-buttons">
            <el-button type="primary" size="large" @click="goToRoomList">进入游戏</el-button>
            <el-button type="info" size="large" @click="goToApiConfig">配置API</el-button>
          </div>
        </div>
        
        <div class="features">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-card class="feature-card">
                <template #header>
                  <div class="feature-header">
                    <el-icon class="feature-icon"><i-ep-aim /></el-icon>
                    <span>智能AI玩家</span>
                  </div>
                </template>
                <div class="feature-content">
                  <p>基于大语言模型的AI玩家，具有不同性格和策略，提供真实的游戏体验</p>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="feature-card">
                <template #header>
                  <div class="feature-header">
                    <el-icon class="feature-icon"><i-ep-setting /></el-icon>
                    <span>灵活配置</span>
                  </div>
                </template>
                <div class="feature-content">
                  <p>支持9人局和12人局的多种板子配置，可根据需要调整游戏规则</p>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="feature-card">
                <template #header>
                  <div class="feature-header">
                    <el-icon class="feature-icon"><i-ep-microphone /></el-icon>
                    <span>语音系统</span>
                  </div>
                </template>
                <div class="feature-content">
                  <p>集成多种TTS引擎，支持个性化语音，让游戏更加生动</p>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
        
        <div class="game-modes">
          <h3>游戏模式</h3>
          <div class="mode-list">
            <el-card class="mode-card">
              <div class="mode-header">
                <h4>9人标准局</h4>
                <span class="player-count">9人</span>
              </div>
              <div class="mode-content">
                <p>配置：3狼人、3平民、1预言家、1女巫、1猎人</p>
                <el-button type="primary" size="small" @click="createRoom(9, 'standard')">创建房间</el-button>
              </div>
            </el-card>
            <el-card class="mode-card">
              <div class="mode-header">
                <h4>12人狼王守卫场</h4>
                <span class="player-count">12人</span>
              </div>
              <div class="mode-content">
                <p>配置：3狼人、1狼王、4平民、1预言家、1女巫、1猎人、1守卫</p>
                <el-button type="primary" size="small" @click="createRoom(12, 'wolfking_guard')">创建房间</el-button>
              </div>
            </el-card>
            <el-card class="mode-card">
              <div class="mode-header">
                <h4>12人奇迹商人场</h4>
                <span class="player-count">12人</span>
              </div>
              <div class="mode-content">
                <p>配置：4狼人、4平民、1预言家、1女巫、1守卫、1奇迹商人</p>
                <el-button type="primary" size="small" @click="createRoom(12, 'miracle_merchant')">创建房间</el-button>
              </div>
            </el-card>
          </div>
        </div>
      </div>
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
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useGameStore } from '../stores/game'

const router = useRouter()
const userStore = useUserStore()
const gameStore = useGameStore()
const activeIndex = ref('home')

onMounted(() => {
  userStore.loadUserInfo()
})

const handleSelect = (key) => {
  activeIndex.value = key
  switch (key) {
    case 'home':
      router.push('/')
      break
    case 'roomList':
      router.push('/game/room/list')
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

const goToRoomList = () => {
  router.push('/game/room/list')
}

const goToApiConfig = () => {
  router.push('/api')
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}

const createRoom = (playerCount, gameBoard) => {
  if (!userStore.getIsLoggedIn) {
    router.push('/login')
    return
  }
  router.push(`/game/room/create?playerCount=${playerCount}&gameBoard=${gameBoard}`)
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

.home-content {
  padding: 20px 0;
}

.banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 60px 40px;
  border-radius: 10px;
  text-align: center;
  margin-bottom: 40px;
}

.banner h2 {
  margin: 0 0 20px;
  font-size: 36px;
}

.banner p {
  margin: 0 0 30px;
  font-size: 18px;
  opacity: 0.9;
}

.banner-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.features {
  margin-bottom: 40px;
}

.feature-card {
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.feature-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.feature-icon {
  font-size: 24px;
  color: #409eff;
}

.game-modes {
  margin-bottom: 40px;
}

.game-modes h3 {
  margin-bottom: 20px;
  color: #303133;
}

.mode-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.mode-card {
  transition: all 0.3s ease;
}

.mode-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.mode-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.mode-header h4 {
  margin: 0;
  color: #303133;
}

.player-count {
  background-color: #ecf5ff;
  color: #409eff;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 14px;
}

.mode-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.mode-content p {
  margin: 0;
  color: #606266;
  font-size: 14px;
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
