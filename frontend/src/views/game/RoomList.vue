<template>
  <div class="room-hall">
    <section class="hall-intro">
      <div>
        <span class="hall-kicker">01 / {{ $locale === 'zh-CN' ? '游戏大厅' : 'GAME LOBBY' }}</span>
        <h2>{{ $t('roomList.title') }}</h2>
        <p>{{ $locale === 'zh-CN' ? '进入一张正在等待的圆桌，或从零开始筹备今夜的游戏。' : 'Join a table waiting for players, or prepare tonight’s game from scratch.' }}</p>
      </div>
      <button class="hall-create" @click="goToCreateRoom"><span>＋</span>{{ $t('roomList.createRoom') }}</button>
    </section>

    <section class="hall-toolbar">
      <div class="hall-filter"><span class="filter-dot"></span>{{ $locale === 'zh-CN' ? '开放房间' : 'OPEN ROOMS' }}</div>
      <div class="room-total">{{ rooms.length }} {{ $locale === 'zh-CN' ? '张圆桌' : 'TABLES' }}</div>
    </section>

    <div v-if="rooms.length" class="room-grid">
      <article v-for="(room, index) in rooms" :key="room.id" class="room-card" @click="viewRoom(room.id)">
        <div class="room-card-head">
          <span class="room-no">{{ String(index + 1).padStart(2, '0') }}</span>
          <span class="room-state" :class="`state-${room.status}`"><i></i>{{ getRoomStatus(room.status) }}</span>
        </div>
        <div class="room-symbol" :class="`symbol-${room.status}`">{{ getRoomSymbol(room) }}</div>
        <h3>{{ room.roomName }}</h3>
        <p class="room-board">{{ $t('roomList.boardName.' + room.gameBoard) }}</p>
        <div class="room-specs">
          <span><b>{{ room.playerCount }}</b>{{ $locale === 'zh-CN' ? ' 人局' : ' PLAYERS' }}</span>
          <span class="spec-rule"></span>
          <span>{{ room.roomCode || ($locale === 'zh-CN' ? '未设密码' : 'OPEN TABLE') }}</span>
        </div>
        <div class="room-card-footer" @click.stop>
          <button class="room-enter" @click="joinRoom(room.id)">{{ $t('roomList.joinRoom') }} <span>→</span></button>
          <button class="room-delete" @click="deleteRoom(room.id)" :aria-label="$t('roomList.deleteRoom')">×</button>
        </div>
      </article>
    </div>

    <section v-else class="empty-hall">
      <div class="empty-mark">◌</div>
      <h3>{{ $locale === 'zh-CN' ? '圆桌尚未点亮' : 'NO TABLES ARE LIT' }}</h3>
      <p>{{ $locale === 'zh-CN' ? '创建第一个房间，邀请 AI 玩家加入。' : 'Create the first room and invite AI players to the table.' }}</p>
      <button @click="goToCreateRoom">{{ $t('roomList.createRoom') }} <span>→</span></button>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '../../stores/game'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const { proxy } = getCurrentInstance()
const $t = proxy.$t
const $locale = proxy.$locale
const router = useRouter()
const gameStore = useGameStore()
const rooms = ref([])

onMounted(fetchRooms)

async function fetchRooms() {
  await gameStore.fetchRooms()
  rooms.value = gameStore.getRooms || []
}

const getRoomStatus = status => ({
  1: $t('roomList.waiting'),
  2: $t('roomList.playing'),
  3: $t('roomList.ended')
}[status] || $t('common.unknown'))

const getRoomSymbol = room => {
  if (room.status === 2) return '⚔'
  if (room.status === 3) return '✕'
  return room.gameBoard === 'miracle_merchant' ? '✦' : room.gameBoard === 'wolfking_guard' ? '♛' : '◈'
}

const goToCreateRoom = () => router.push('/game/room/create')
const joinRoom = id => router.push(`/game/room/${id}`)
const viewRoom = id => router.push(`/game/room/${id}`)

async function deleteRoom(id) {
  try {
    await axios.delete(`/game/room/delete/${id}`)
    ElMessage.success($t('roomList.deleteSuccess'))
    fetchRooms()
  } catch {
    ElMessage.error($t('roomList.deleteFailed'))
  }
}
</script>

<style scoped>
.room-hall { width: min(1240px, 100%); margin: 0 auto; padding: 24px 0 82px; }
.hall-intro { display: flex; align-items: end; justify-content: space-between; gap: 32px; padding: 30px 0 38px; }
.hall-kicker { color: #d9b55d; font: 700 10px/1 var(--font-heading); letter-spacing: .18em; }
.hall-intro h2 { margin: 14px 0 13px; color: #f1f5f8; font-size: clamp(34px, 4vw, 50px); letter-spacing: -.045em; }
.hall-intro p { max-width: 460px; margin: 0; color: #9eafbe; font-size: 16px; line-height: 1.65; }
.hall-create { min-height: 44px; padding: 0 18px; border: 1px solid #e6c76c; border-radius: 7px; color: #11100d; background: linear-gradient(135deg, #e8ca70, #bd8d31); cursor: pointer; font: 700 11px/1 var(--font-heading); letter-spacing: .08em; box-shadow: 0 10px 28px rgba(206, 158, 61, .18); }
.hall-create span { margin-right: 7px; font-size: 16px; }
.hall-create:hover { filter: brightness(1.08); transform: translateY(-1px); }
.hall-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 14px 0; border-top: 1px solid rgba(180, 204, 222, .13); border-bottom: 1px solid rgba(180, 204, 222, .13); }
.hall-filter, .room-total { color: #b4c1cc; font: 700 10px/1 var(--font-heading); letter-spacing: .14em; }
.hall-filter { display: flex; align-items: center; gap: 8px; color: #e1ba60; }
.filter-dot { width: 7px; height: 7px; border-radius: 50%; background: #92d49c; box-shadow: 0 0 0 4px rgba(146, 212, 156, .12); }
.room-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; padding-top: 24px; }
.room-card { min-height: 285px; padding: 23px; border: 1px solid rgba(180, 204, 222, .15); border-radius: 12px; color: #eef3f7; background: linear-gradient(155deg, #101d2a, #0b141f); cursor: pointer; transition: .25s ease; }
.room-card:hover { transform: translateY(-5px); border-color: rgba(218, 181, 93, .58); background: linear-gradient(155deg, #152536, #0d1824); box-shadow: 0 20px 42px rgba(0, 0, 0, .22); }
.room-card-head { display: flex; align-items: center; justify-content: space-between; }
.room-no { color: #8191a0; font: 700 10px/1 var(--font-heading); letter-spacing: .18em; }
.room-state { display: inline-flex; gap: 6px; align-items: center; color: #aebdca; font: 700 9px/1 var(--font-heading); letter-spacing: .09em; }
.room-state i { width: 6px; height: 6px; border-radius: 50%; background: #8fd099; box-shadow: 0 0 0 3px rgba(143, 208, 153, .12); }
.room-state.state-2 i { background: #dfb95e; box-shadow: 0 0 0 3px rgba(223, 185, 94, .12); }
.room-state.state-3 i { background: #a87070; box-shadow: 0 0 0 3px rgba(168, 112, 112, .12); }
.room-symbol { margin: 36px 0 24px; color: #e3bb60; font: 400 42px/.8 Georgia, serif; }
.symbol-2 { color: #e4c26e; }.symbol-3 { color: #a87979; }
.room-card h3 { margin: 0 0 8px; color: #f0f5f8; font-size: 21px; letter-spacing: -.025em; }
.room-board { min-height: 24px; margin: 0; color: #91a3b4; font-size: 14px; }
.room-specs { display: flex; align-items: center; gap: 11px; margin-top: 22px; color: #afbdc9; font: 700 10px/1 var(--font-heading); letter-spacing: .09em; }
.room-specs b { margin-right: 3px; color: #e4bd65; font-size: 14px; }.spec-rule { width: 1px; height: 13px; background: rgba(193, 209, 223, .28); }
.room-card-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 23px; padding-top: 16px; border-top: 1px solid rgba(180, 204, 222, .12); }
.room-enter { border: 0; color: #e3bd66; background: transparent; cursor: pointer; font: 700 10px/1 var(--font-heading); letter-spacing: .12em; }.room-enter span { margin-left: 8px; font-size: 15px; }.room-enter:hover { color: #fff0b5; }
.room-delete { width: 27px; height: 27px; border: 1px solid rgba(188, 135, 135, .26); border-radius: 50%; color: #9c7777; background: transparent; cursor: pointer; font-size: 17px; line-height: 1; }.room-delete:hover { border-color: #b36c6c; color: #d78989; }
.empty-hall { display: grid; justify-items: center; padding: 105px 20px; text-align: center; border: 1px solid rgba(180, 204, 222, .14); border-top: 0; background: linear-gradient(180deg, rgba(14, 26, 38, .38), rgba(7, 14, 22, .3)); }.empty-mark { color: #d9b55d; font: 400 64px/.8 Georgia, serif; }.empty-hall h3 { margin: 22px 0 10px; color: #edf3f7; font-size: 24px; }.empty-hall p { margin: 0; color: #9cadbc; }.empty-hall button { margin-top: 24px; border: 0; color: #e3bd66; background: transparent; cursor: pointer; font: 700 10px/1 var(--font-heading); letter-spacing: .13em; }.empty-hall button span { margin-left: 8px; font-size: 16px; }
@media (max-width: 900px) { .room-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 620px) { .room-hall { padding-top: 6px; }.hall-intro { display: block; }.hall-create { margin-top: 24px; }.room-grid { grid-template-columns: 1fr; } }
</style>
