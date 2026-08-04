<template>
  <div class="room-detail-page">
    <section class="room-intro">
      <div>
        <span class="room-kicker">{{ room.status === 1 ? 'WAITING TABLE' : room.status === 2 ? 'ACTIVE TABLE' : 'CLOSED TABLE' }}</span>
        <h2>{{ room.roomName || $t('roomDetail.title') }}</h2>
        <div class="room-meta-line">
          <span class="room-state"><i></i>{{ getStatus(room.status) }}</span>
          <span>{{ $t('gameBoard.' + room.gameBoard) }}</span>
          <span>{{ room.playerCount }} {{ $locale === 'zh-CN' ? '人局' : 'PLAYERS' }}</span>
          <span>{{ room.hasPassword ? ($locale === 'zh-CN' ? '私密房间' : 'PRIVATE TABLE') : ($locale === 'zh-CN' ? '公开房间' : 'OPEN TABLE') }}</span>
        </div>
      </div>
      <div class="room-intro-actions">
        <button class="text-action" @click="router.push('/game/room/list')">← {{ $t('common.back') }}</button>
        <button class="enter-action" @click="enterRoom">{{ $t('roomDetail.enterRoom') }} <span>→</span></button>
      </div>
    </section>

    <section class="detail-layout">
      <main class="roster-panel">
        <div class="panel-heading">
          <div><span>01 / {{ $locale === 'zh-CN' ? '入座名单' : 'SEATING ROSTER' }}</span><h3>{{ $t('roomDetail.playerList') }}</h3></div>
          <b>{{ players.length }} / {{ room.playerCount || 0 }}</b>
        </div>

        <div class="seat-grid">
          <article v-for="slot in seatSlots" :key="slot.number" class="seat" :class="{ occupied: slot.player, empty: !slot.player }">
            <template v-if="slot.player">
              <span class="seat-number">{{ String(slot.number).padStart(2, '0') }}</span>
              <span class="seat-avatar">{{ slot.player.userId === -1 ? '✦' : '◌' }}</span>
              <div class="seat-copy"><b>{{ getPlayerName(slot.player) }}</b><small>{{ slot.player.userId === -1 ? 'AI PLAYER' : ($locale === 'zh-CN' ? '真人玩家' : 'HUMAN PLAYER') }}</small></div>
              <button v-if="room.status === 1 && isOwner" class="remove-player" @click="deletePlayer(slot.player.id)" :aria-label="$t('roomDetail.deletePlayer')">×</button>
            </template>
            <template v-else>
              <span class="seat-number">{{ String(slot.number).padStart(2, '0') }}</span>
              <span class="empty-seat-mark">＋</span>
              <span class="empty-seat-label">{{ $locale === 'zh-CN' ? '空位' : 'OPEN SEAT' }}</span>
            </template>
          </article>
        </div>
      </main>

      <aside class="room-sidebar">
        <section v-if="room.status === 1 && isOwner" class="side-panel add-panel">
          <span class="side-kicker">02 / {{ $locale === 'zh-CN' ? '邀请 AI' : 'INVITE AI' }}</span>
          <h3>{{ $t('roomDetail.addAiPlayer') }}</h3>
          <p>{{ $locale === 'zh-CN' ? '选择一位已配置的 AI 玩家，并安排其座位。' : 'Choose a configured AI player and assign an open seat.' }}</p>
          <el-button
            class="quick-fill-button"
            :loading="quickFilling"
            :disabled="adding || quickFilling || !availableNums.length || !availableAiPlayers.length"
            @click="quickFillPlayers"
          >
            {{ $locale === 'zh-CN' ? '随机填满空位' : 'FILL OPEN SEATS' }}
          </el-button>
          <el-form class="add-form" label-position="top">
            <el-form-item :label="$t('roomDetail.selectAiPlayer')">
              <el-select v-model="selectedAi" :placeholder="$t('roomDetail.selectAiPlayer')">
                <el-option v-for="player in availableAiPlayers" :key="player.id" :label="player.name" :value="player.id" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('roomDetail.playerNumber')">
              <el-select v-model="aiNum" :placeholder="$t('roomDetail.selectPlayerNumber')">
                <el-option v-for="number in availableNums" :key="number" :label="number" :value="number" />
              </el-select>
            </el-form-item>
            <el-button type="primary" class="add-ai-button" @click="addAiPlayer" :loading="adding" :disabled="quickFilling || !selectedAi || !aiNum">{{ $t('roomDetail.addAiPlayerBtn') }}</el-button>
          </el-form>
        </section>

        <section class="side-panel game-ready">
          <span class="side-kicker">03 / {{ $locale === 'zh-CN' ? '游戏状态' : 'TABLE STATUS' }}</span>
          <h3>{{ canStart ? ($locale === 'zh-CN' ? '圆桌已就绪' : 'TABLE IS READY') : ($locale === 'zh-CN' ? '等待所有玩家' : 'WAITING FOR PLAYERS') }}</h3>
          <p>{{ canStart ? ($locale === 'zh-CN' ? '所有座位均已确认，可以开始本局游戏。' : 'Every seat is confirmed. The game can begin.') : ($locale === 'zh-CN' ? `还需要 ${Math.max(0, (room.playerCount || 0) - players.length)} 位玩家入座。` : `${Math.max(0, (room.playerCount || 0) - players.length)} more players are needed.`) }}</p>
          <button v-if="isOwner" class="start-game-button" :disabled="!canStart" @click="startGame"><span>✦</span>{{ $t('roomDetail.startGame') }}</button>
          <button class="leave-button" @click="leaveRoom">{{ $t('roomDetail.leaveRoom') }}</button>
        </section>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useGameStore } from '../../stores/game'
import { useUserStore } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const { proxy } = getCurrentInstance()
const $t = proxy.$t
const $locale = proxy.$locale
const router = useRouter()
const route = useRoute()
const gameStore = useGameStore()
const userStore = useUserStore()
const room = reactive({})
const players = ref([])
const aiPlayers = ref([])
const selectedAi = ref(null)
const aiNum = ref(null)
const adding = ref(false)
const quickFilling = ref(false)

const canStart = computed(() => room.status === 1 && room.playerCount === players.value.length)
const isOwner = computed(() => Number(room.creatorId) === Number(userStore.userInfo?.id))
const availableNums = computed(() => Array.from({ length: room.playerCount || 0 }, (_, index) => index + 1).filter(number => !players.value.some(player => player.playerNumber === number)))
const availableAiPlayers = computed(() => aiPlayers.value.filter(ai => !players.value.some(player => player.aiPlayerId === ai.id)))
const seatSlots = computed(() => Array.from({ length: room.playerCount || 0 }, (_, index) => ({ number: index + 1, player: players.value.find(player => player.playerNumber === index + 1) })))

onMounted(async () => { await fetchRoom(); await fetchAi(); await fetchPlayers() })
const fetchRoom = async () => { const detail = await gameStore.getRoomById(route.params.id); if (detail) Object.assign(room, detail) }
const fetchPlayers = async () => { const list = await gameStore.getPlayersByRoomId(route.params.id); players.value = (list || []).sort((a, b) => a.playerNumber - b.playerNumber) }
const fetchAi = async () => { try { const response = await axios.get('/ai/player/available'); if (response.data.code === 200) aiPlayers.value = response.data.data } catch {} }
const getStatus = status => ({ 1: $t('roomList.waiting'), 2: $t('roomList.playing'), 3: $t('roomList.ended') }[status] || $t('common.unknown'))
const getPlayerName = player => {
  if (player.userId === -1) return aiPlayers.value.find(ai => ai.id == player.aiPlayerId)?.name || player.playerName || `AI ${player.playerNumber}`
  return player.playerName || `Player ${player.playerNumber}`
}
const deletePlayer = async id => { try { await axios.post('/game/player/remove', { roomId: route.params.id, playerId: id }); ElMessage.success($t('roomDetail.playerDeleted')); await fetchPlayers() } catch { ElMessage.error($t('roomDetail.addFailed')) } }
const addAiPlayer = async () => {
  if (!selectedAi.value || !aiNum.value) return
  adding.value = true
  try {
    const ai = aiPlayers.value.find(player => player.id === selectedAi.value)
    const response = await axios.post('/game/player/add', { roomId: route.params.id, userId: -1, aiPlayerId: selectedAi.value, playerNumber: aiNum.value, playerName: ai.name })
    if (response.data.code === 200) { ElMessage.success($t('roomDetail.aiPlayerAdded', { name: ai.name })); selectedAi.value = null; aiNum.value = null; await fetchPlayers() }
  } catch { ElMessage.error($t('roomDetail.addFailed')) } finally { adding.value = false }
}
const shuffle = items => {
  const shuffled = [...items]
  for (let index = shuffled.length - 1; index > 0; index--) {
    const randomIndex = Math.floor(Math.random() * (index + 1))
    ;[shuffled[index], shuffled[randomIndex]] = [shuffled[randomIndex], shuffled[index]]
  }
  return shuffled
}
const quickFillPlayers = async () => {
  if (quickFilling.value) return

  const openSeats = [...availableNums.value]
  const candidates = shuffle(availableAiPlayers.value).slice(0, openSeats.length)
  if (!openSeats.length || !candidates.length) return

  quickFilling.value = true
  try {
    const results = await Promise.all(candidates.map(async (ai, index) => {
      try {
        const response = await axios.post('/game/player/add', {
          roomId: route.params.id,
          userId: -1,
          aiPlayerId: ai.id,
          playerNumber: openSeats[index],
          playerName: ai.name
        })
        return response.data.code === 200
      } catch {
        return false
      }
    }))

    const filledCount = results.filter(Boolean).length
    await fetchPlayers()
    if (filledCount === openSeats.length) {
      ElMessage.success($locale.value === 'zh-CN' ? `已随机填充 ${filledCount} 名 AI 玩家` : `${filledCount} AI players added at random`)
    } else if (filledCount > 0) {
      const remainingCount = Math.max(0, openSeats.length - filledCount)
      ElMessage.warning($locale.value === 'zh-CN' ? `已填充 ${filledCount} 名，仍缺少 ${remainingCount} 名可用 AI 玩家` : `${filledCount} players added; ${remainingCount} seats remain open`)
    } else {
      ElMessage.error($locale.value === 'zh-CN' ? '快速填充失败，请稍后重试' : 'Unable to fill seats. Please try again.')
    }
  } finally {
    quickFilling.value = false
  }
}
const startGame = async () => { if (!canStart.value) return; const success = await gameStore.startGame(route.params.id); if (success) { ElMessage.success($t('roomDetail.gameStarted')); router.push(`/game/play/${route.params.id}`) } else ElMessage.error($t('roomDetail.startFailed')) }
const leaveRoom = async () => { await gameStore.leaveRoom(route.params.id); ElMessage.success($t('roomDetail.leftRoom')); router.push('/game/room/list') }
const enterRoom = async () => {
  if (room.hasPassword && !isOwner.value) {
    try {
      const { value } = await ElMessageBox.prompt($locale.value === 'zh-CN' ? '请输入房间密码' : 'Enter room password', $locale.value === 'zh-CN' ? '私密房间' : 'PRIVATE TABLE', {
        confirmButtonText: $locale.value === 'zh-CN' ? '进入' : 'Enter',
        cancelButtonText: $locale.value === 'zh-CN' ? '取消' : 'Cancel',
        inputType: 'password',
        inputValidator: input => (input ? true : ($locale.value === 'zh-CN' ? '请输入密码' : 'Password is required'))
      })
      const response = await axios.post('/game/room/verify', { roomId: route.params.id, password: value || '' })
      if (response.data.code !== 200) {
        ElMessage.error($locale.value === 'zh-CN' ? '房间密码错误' : 'Wrong room password')
        return
      }
    } catch {
      return
    }
  }
  router.push(`/game/play/${route.params.id}`)
}
</script>

<style scoped>
.room-detail-page { width: min(1240px, 100%); margin: 0 auto; padding: 26px 0 84px; }.room-intro { display: flex; align-items: end; justify-content: space-between; gap: 30px; padding: 30px 0 38px; }.room-kicker, .side-kicker { color: #d9b55d; font: 700 10px/1 var(--font-heading); letter-spacing: .18em; }.room-intro h2 { margin: 14px 0 12px; color: #eef4f8; font-size: clamp(34px, 4vw, 50px); letter-spacing: -.045em; }.room-meta-line { display: flex; flex-wrap: wrap; gap: 12px; color: #9eafbe; font: 700 10px/1 var(--font-heading); letter-spacing: .08em; }.room-meta-line > span:not(:first-child)::before { margin-right: 12px; color: #546575; content: '·'; }.room-state { color: #a7d8a2; }.room-state i { display: inline-block; width: 6px; height: 6px; margin-right: 7px; border-radius: 50%; background: #93d99d; box-shadow: 0 0 0 4px rgba(147, 217, 157, .1); }.room-intro-actions { display: flex; align-items: center; gap: 18px; }.text-action { border: 0; color: #b2c0cb; background: transparent; cursor: pointer; font: 700 10px/1 var(--font-heading); letter-spacing: .1em; }.text-action:hover { color: #e3bd66; }.enter-action { min-height: 42px; padding: 0 16px; border: 1px solid rgba(217, 181, 93, .7); border-radius: 7px; color: #ebc76d; background: transparent; cursor: pointer; font: 700 10px/1 var(--font-heading); letter-spacing: .08em; }.enter-action span { margin-left: 9px; font-size: 15px; }.enter-action:hover { background: rgba(217, 181, 93, .1); }
.detail-layout { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 18px; align-items: start; }.roster-panel, .side-panel { border: 1px solid rgba(180, 204, 222, .18); border-radius: 12px; background: linear-gradient(155deg, #101d2a, #0b141f); }.roster-panel { padding: 28px; }.panel-heading { display: flex; align-items: end; justify-content: space-between; padding-bottom: 23px; border-bottom: 1px solid rgba(180, 204, 222, .14); }.panel-heading span { color: #d9b55d; font: 700 10px/1 var(--font-heading); letter-spacing: .15em; }.panel-heading h3 { margin: 9px 0 0; color: #eff4f8; font-size: 22px; }.panel-heading > b { padding: 7px 9px; border: 1px solid rgba(180, 204, 222, .17); border-radius: 4px; color: #d4dee6; font: 700 10px/1 var(--font-heading); letter-spacing: .08em; }.seat-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; padding-top: 20px; }.seat { position: relative; display: flex; align-items: center; min-height: 76px; padding: 13px; border: 1px solid rgba(180, 204, 222, .13); border-radius: 8px; background: #0b1520; }.seat.empty { opacity: .6; border-style: dashed; }.seat-number { align-self: start; margin-right: 13px; color: #768798; font: 700 10px/1 var(--font-heading); letter-spacing: .12em; }.seat-avatar, .empty-seat-mark { display: grid; width: 31px; height: 31px; margin-right: 11px; place-items: center; border: 1px solid rgba(217, 181, 93, .24); border-radius: 50%; color: #dfb95e; font: 400 19px/1 Georgia, serif; }.empty-seat-mark { color: #8597a7; border-color: rgba(180, 204, 222, .16); }.seat-copy { min-width: 0; flex: 1; }.seat-copy b { display: block; overflow: hidden; color: #e8eff4; font: 700 14px/1.25 var(--font-heading); text-overflow: ellipsis; white-space: nowrap; }.seat-copy small, .empty-seat-label { color: #8ca0af; font: 700 9px/1 var(--font-heading); letter-spacing: .1em; }.empty-seat-label { margin-left: 4px; }.remove-player { position: absolute; top: 9px; right: 10px; width: 21px; height: 21px; border: 0; color: #9f7777; background: transparent; cursor: pointer; font-size: 16px; }.remove-player:hover { color: #df8888; }.room-sidebar { display: grid; gap: 18px; }.side-panel { padding: 24px; }.side-panel h3 { margin: 12px 0 10px; color: #eff4f8; font-size: 20px; }.side-panel p { margin: 0; color: #99aaba; font-size: 14px; line-height: 1.65; }.quick-fill-button { width: 100%; min-height: 43px; margin-top: 20px; border-color: rgba(217, 181, 93, .72); color: #e7c469; background: rgba(217, 181, 93, .08); font: 700 10px/1 var(--font-heading); letter-spacing: .08em; }.quick-fill-button:hover:not(:disabled) { border-color: #e6c76c; color: #fff0b5; background: rgba(217, 181, 93, .14); }.add-form { margin-top: 20px; padding-top: 20px; border-top: 1px solid rgba(180, 204, 222, .12); }.add-ai-button { width: 100%; margin-top: 4px; }.game-ready { background: linear-gradient(155deg, #132334, #0b141f); }.start-game-button, .leave-button { width: 100%; min-height: 45px; margin-top: 24px; border-radius: 7px; cursor: pointer; font: 700 10px/1 var(--font-heading); letter-spacing: .09em; }.start-game-button { border: 1px solid #e6c76c; color: #100f0b; background: linear-gradient(135deg, #e8ca70, #bd8d31); }.start-game-button span { margin-right: 7px; }.start-game-button:disabled { border-color: rgba(180, 204, 222, .16); color: #8192a2; background: #101a25; cursor: not-allowed; }.leave-button { margin-top: 10px; border: 1px solid rgba(180, 204, 222, .16); color: #a7b6c2; background: transparent; }.leave-button:hover { border-color: rgba(192, 125, 125, .55); color: #dc8a8a; }
@media (max-width: 900px) { .detail-layout { grid-template-columns: 1fr; }.room-sidebar { grid-template-columns: repeat(2, 1fr); }.seat-grid { grid-template-columns: repeat(3, 1fr); } }.room-intro-actions { flex-wrap: wrap; }
@media (max-width: 620px) { .room-intro { display: block; }.room-intro-actions { margin-top: 24px; }.room-sidebar, .seat-grid { grid-template-columns: 1fr; }.roster-panel, .side-panel { padding: 20px; } }
</style>
