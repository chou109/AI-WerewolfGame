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
      {
        path: '',
        name: 'Home',
        component: () => import('../views/Home.vue')
      },
      {
        path: 'game/room/list',
        name: 'RoomList',
        component: () => import('../views/game/RoomList.vue')
      },
      {
        path: 'game/room/create',
        name: 'CreateRoom',
        component: () => import('../views/game/CreateRoom.vue')
      },
      {
        path: 'game/room/:id',
        name: 'RoomDetail',
        component: () => import('../views/game/RoomDetail.vue')
      },
      {
        path: 'game/play/:roomId',
        name: 'GamePlay',
        component: () => import('../views/game/GamePlay.vue')
      },
      {
        path: 'api',
        name: 'ApiConfig',
        component: () => import('../views/ApiConfig.vue')
      },
      {
        path: 'voice',
        name: 'VoiceConfig',
        component: () => import('../views/VoiceConfig.vue')
      },
      {
        path: 'ai-dialog-test',
        name: 'AIDialogTest',
        component: () => import('../views/AIDialogTest.vue')
      },
      {
        path: 'records',
        name: 'GameRecords',
        component: () => import('../views/GameRecords.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
