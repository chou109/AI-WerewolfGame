import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
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
    path: '/game',
    name: 'Game',
    component: () => import('../views/Game.vue'),
    children: [
      {
        path: 'room/list',
        name: 'RoomList',
        component: () => import('../views/game/RoomList.vue')
      },
      {
        path: 'room/create',
        name: 'CreateRoom',
        component: () => import('../views/game/CreateRoom.vue')
      },
      {
        path: 'room/:id',
        name: 'RoomDetail',
        component: () => import('../views/game/RoomDetail.vue')
      },
      {
        path: 'play/:roomId',
        name: 'GamePlay',
        component: () => import('../views/game/GamePlay.vue')
      },
      {
        path: 'ai/players',
        name: 'AiPlayerManager',
        component: () => import('../views/game/AiPlayerManager.vue')
      }
    ]
  },
  {
    path: '/api',
    name: 'ApiConfig',
    component: () => import('../views/ApiConfig.vue')
  },
  {
    path: '/voice',
    name: 'VoiceConfig',
    component: () => import('../views/VoiceConfig.vue')
  },
  {
    path: '/records',
    name: 'GameRecords',
    component: () => import('../views/GameRecords.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
