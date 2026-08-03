<template>
  <el-container :class="{ 'game-shell': isGamePlay }">
    <el-header>
      <div class="header-content">
        <div class="logo" @click="$router.push('/')">
          <span class="logo-icon">🛡️</span>
          <span class="logo-text">{{ $locale === 'zh-CN' ? 'AI狼人杀' : 'AI Werewolf' }}</span>
        </div>
        <div class="nav">
          <el-menu :default-active="activeIndex" class="el-menu-demo" mode="horizontal" @select="handleSelect">
            <el-menu-item index="home">{{ $t('nav.home') }}</el-menu-item>
            <el-menu-item index="roomList">{{ $t('nav.roomList') }}</el-menu-item>
            <el-menu-item index="createRoom">{{ $t('nav.createRoom') }}</el-menu-item>
            <el-menu-item index="apiConfig">{{ $t('nav.aiPlayerManagement') }}</el-menu-item>
            <el-menu-item index="voiceConfig">{{ $t('nav.voiceConfig') }}</el-menu-item>
            <el-menu-item index="gameRecords">{{ $t('nav.gameRecords') }}</el-menu-item>
          </el-menu>
        </div>
        <div class="header-actions">
          <el-tooltip :content="$locale === 'zh-CN' ? 'Switch to English' : '切换到中文'" placement="bottom">
            <button class="lang-btn" @click="toggleLang">
              {{ $locale === 'zh-CN' ? 'EN' : '中' }}
            </button>
          </el-tooltip>
          <div class="user-info">
            <template v-if="userStore.getIsLoggedIn">
              <el-dropdown>
                <span class="user-name">{{ userStore.getUserInfo?.nickname || userStore.getUserInfo?.username }}</span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item>{{ $t('nav.personalCenter') }}</el-dropdown-item>
                    <el-dropdown-item @click="logout">{{ $t('nav.logout') }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <el-button type="primary" size="small" @click="goToLogin">{{ $t('nav.login') }}</el-button>
              <el-button size="small" class="ghost-btn" @click="goToRegister">{{ $t('nav.register') }}</el-button>
            </template>
          </div>
        </div>
      </div>
    </el-header>
    <el-main :class="{ 'home-main': route.path === '/', 'game-main': isGamePlay }">
      <el-breadcrumb v-if="route.path !== '/' && !isGamePlay" class="breadcrumb" separator="·">
        <el-breadcrumb-item v-for="item in breadcrumbItems" :key="item.path" :to="item.path">
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
      <router-view />
    </el-main>
    <el-footer v-if="!isGamePlay">
      <div class="footer-content">
        <div class="footer-ornament">✦</div>
        <p>{{ $t('home.copyright') }}</p>
        <p class="footer-sub">{{ $t('home.poweredBy') }}</p>
      </div>
    </el-footer>
  </el-container>
</template>

<script setup>
import { ref, onMounted, computed, watch, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'

const { proxy } = getCurrentInstance()
const $t = proxy.$t
const $locale = proxy.$locale
const $toggleLocale = proxy.$toggleLocale

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const activeIndex = ref('home')
const isGamePlay = computed(() => route.path.includes('/game/play/'))

const breadcrumbItems = computed(() => {
  const path = route.path
  const items = [{ title: $t('breadcrumb.home'), path: '/' }]
  if (path.includes('/game/room/list')) { items.push({ title: $t('breadcrumb.gameManagement'), path: '/game/room/list' }, { title: $t('breadcrumb.roomList'), path: '/game/room/list' }) }
  else if (path.includes('/game/room/create')) { items.push({ title: $t('breadcrumb.gameManagement'), path: '/game/room/list' }, { title: $t('breadcrumb.createRoom'), path: '/game/room/create' }) }
  else if (path.includes('/game/room/')) { items.push({ title: $t('breadcrumb.gameManagement'), path: '/game/room/list' }, { title: $t('breadcrumb.roomDetail'), path: path }) }
  else if (path.includes('/game/play/')) { items.push({ title: $t('breadcrumb.gameManagement'), path: '/game/room/list' }, { title: $t('breadcrumb.inGame'), path: path }) }
  else if (path.includes('/ai-players')) { items.push({ title: $t('breadcrumb.aiPlayerManagement'), path: '/ai-players' }) }
  else if (path.includes('/voice')) { items.push({ title: $t('breadcrumb.voiceConfig'), path: '/voice' }) }
  else if (path.includes('/records')) { items.push({ title: $t('breadcrumb.gameRecords'), path: '/records' }) }
  return items
})

onMounted(() => { userStore.loadUserInfo(); updateActiveIndex() })
watch(() => route.path, () => updateActiveIndex())

const updateActiveIndex = () => {
  const p = route.path
  if (p === '/') activeIndex.value = 'home'
  else if (p.includes('room/list')) activeIndex.value = 'roomList'
  else if (p.includes('room/create')) activeIndex.value = 'createRoom'
  else if (p.includes('/ai-players')) activeIndex.value = 'apiConfig'
  else if (p.includes('/voice')) activeIndex.value = 'voiceConfig'
  else if (p.includes('/records')) activeIndex.value = 'gameRecords'
}

const handleSelect = (key) => {
  activeIndex.value = key
  const m = { home: '/', roomList: '/game/room/list', createRoom: '/game/room/create', apiConfig: '/ai-players', voiceConfig: '/voice', gameRecords: '/records' }
  if (m[key]) router.push(m[key])
}

const goToLogin = () => router.push('/login')
const goToRegister = () => router.push('/register')
const logout = () => { userStore.logout(); router.push('/login') }
const toggleLang = () => { $toggleLocale(); location.reload() }
</script>

<style scoped>
.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 100%;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}
.logo-icon {
  font-size: 28px;
  filter: drop-shadow(0 0 6px rgba(201,169,110,0.4));
}
.logo-text {
  font-family: var(--font-heading);
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--gold);
  letter-spacing: 0.08em;
  text-shadow: 0 0 20px rgba(201,169,110,0.3);
}

.nav { flex: 1; margin: 0 20px; overflow: hidden; }

.header-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.lang-btn {
  width: 38px;
  height: 38px;
  border-radius: 4px;
  font-family: var(--font-heading);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--gold);
  background: rgba(201,169,110,0.08);
  border: 1px solid rgba(201,169,110,0.3);
  cursor: pointer;
  transition: all var(--transition-normal);
}
.lang-btn:hover {
  background: rgba(201,169,110,0.18);
  border-color: var(--gold);
  box-shadow: var(--shadow-gold);
  transform: scale(1.05);
}

.ghost-btn {
  background: transparent !important;
  border: 1px solid rgba(201,169,110,0.3) !important;
  color: var(--text-secondary) !important;
}
.ghost-btn:hover {
  border-color: var(--gold) !important;
  color: var(--gold) !important;
}

.user-name {
  color: var(--text-secondary);
  cursor: pointer;
  font-family: var(--font-heading);
  font-size: 0.85rem;
}
.user-name:hover { color: var(--gold); }

.user-info { display: flex; align-items: center; gap: 8px; }

.breadcrumb { margin-bottom: 20px; }
:deep(.home-main) { padding: 0 !important; overflow: hidden; }
.game-shell { height: 100vh; min-height: 100vh; overflow: hidden; }
.game-main { height: calc(100vh - 60px); padding: 0 !important; overflow: hidden; }

.footer-content { text-align: center; padding: 10px 0; }
.footer-content p { margin: 4px 0; color: var(--text-muted); font-size: 0.85rem; }
.footer-sub { font-size: 0.75rem !important; opacity: 0.6; }
.footer-ornament {
  font-size: 1.2rem;
  color: var(--gold-dark);
  margin-bottom: 8px;
  opacity: 0.6;
}
</style>
