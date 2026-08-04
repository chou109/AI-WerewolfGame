import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/',
    component: () => import('../components/Layout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue'), meta: { requiresAuth: true } },
      { path: 'game/room/list', name: 'RoomList', component: () => import('../views/game/RoomList.vue'), meta: { requiresAuth: true } },
      { path: 'game/room/create', name: 'CreateRoom', component: () => import('../views/game/CreateRoom.vue'), meta: { requiresAuth: true } },
      { path: 'game/room/:id', name: 'RoomDetail', component: () => import('../views/game/RoomDetail.vue'), meta: { requiresAuth: true } },
      { path: 'game/play/:roomId', name: 'GamePlay', component: () => import('../views/game/GamePlay.vue'), meta: { requiresAuth: true } },
      { path: 'ai-players', name: 'ApiConfig', component: () => import('../views/ApiConfig.vue'), meta: { requiresAuth: true } },
      { path: 'voice', name: 'VoiceConfig', component: () => import('../views/VoiceConfig.vue'), meta: { requiresAuth: true } },
      { path: 'ai-dialog-test', name: 'AIDialogTest', component: () => import('../views/AIDialogTest.vue'), meta: { requiresAuth: true } },
      { path: 'records', name: 'GameRecords', component: () => import('../views/GameRecords.vue'), meta: { requiresAuth: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if ((to.path === '/login' || to.path === '/register') && token) {
    return { path: '/' }
  }
  return true
})

export default router
