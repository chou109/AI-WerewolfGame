<template>
  <div class="game-play">
    <!-- ===== Top Bar ===== -->
    <div class="game-topbar">
      <div class="topbar-left">
        <span class="topbar-icon">🌕</span>
        <h2 class="topbar-title">{{ $t('gamePlay.title') }}</h2>
        <span v-if="roomInfo.gameBoard && roomInfo.playerCount" class="room-config-tag">
          {{ boardLabel }} · {{ roomInfo.playerCount }}{{ $locale === 'zh-CN' ? '人场' : ' players' }}
        </span>
        <span v-if="gameStarted" class="phase-tag" :class="currentPhase">{{ phaseLabel }}</span>
      </div>
      <div class="topbar-center">
        <template v-if="!gameStarted">
          <button class="btn-start" @click="startGame">
            <span>⚔️</span> {{ $t('gamePlay.startGame') }}
          </button>
        </template>
        <template v-else>
          <div class="game-stats">
            <span class="stat">{{ $locale==='zh-CN'?'第':'R' }}{{ currentRound }} {{ $locale==='zh-CN'?'夜':'night' }}</span>
            <span class="stat-divider">·</span>
            <span class="stat">{{ $locale==='zh-CN'?'第':'D' }}{{ currentDay }} {{ $locale==='zh-CN'?'天':'day' }}</span>
          </div>
        </template>
      </div>
      <div class="topbar-right">
        <div v-if="gameStarted" class="view-controls">
          <div class="speed-group">
            <span class="ctrl-label">{{ $t('gamePlay.displaySpeed') }}</span>
            <button v-for="s in speeds" :key="s.key" class="speed-btn" :class="{ active: typewriterSpeed === s.key }" @click="typewriterSpeed = s.key; setSpeed(s.key)">{{ s.label }}</button>
          </div>
          <div class="toggle-group">
            <span class="ctrl-label">{{ $t('gamePlay.thinkingProcess') }}</span>
            <button class="toggle-btn" :class="{ active: showThinking }" @click="showThinking = !showThinking">{{ showThinking ? '👁' : '—' }}</button>
          </div>
        </div>
        <button v-if="gameStarted" class="btn-pause" :class="{ active: isGamePaused }" @click="toggleGamePause">
          {{ isGamePaused ? ($locale === 'zh-CN' ? '继续游戏' : 'Resume') : ($locale === 'zh-CN' ? '暂停游戏' : 'Pause') }}
        </button>
        <button v-if="gameStarted" class="btn-exit" @click="exitGame">{{ $t('gamePlay.exitGame') }}</button>
      </div>
    </div>

    <!-- ===== View Mode Bar ===== -->
    <div v-if="gameStarted" class="view-bar">
      <div class="view-mode-toggle">
        <button v-for="m in viewModes" :key="m.key" class="view-btn" :class="{ active: currentViewMode === m.key }" @click="currentViewMode = m.key">{{ m.label }}</button>
      </div>
      <div v-if="currentViewMode === 'player'" class="view-player-select">
        <el-select v-model="selectedPlayerId" :placeholder="$t('gamePlay.selectPlayer')" size="small" style="width:140px">
          <el-option v-for="p in alivePlayers" :key="p.id" :label="p.playerName" :value="p.id" />
        </el-select>
      </div>
      <span v-if="isGamePaused" class="paused-status">{{ $locale === 'zh-CN' ? '游戏已暂停' : 'GAME PAUSED' }}</span>
    </div>

    <!-- ===== Main Layout ===== -->
    <div class="game-layout">
      <!-- Left: Players 1-6 -->
      <div class="side-panel">
        <div class="player-badges">
          <div v-for="pos in leftPositions" :key="pos.number" class="player-badge" :class="{ occupied: pos.player, locked: pos.locked, dead: pos.player && !pos.player.isAlive, speaking: pos.player?.isSpeaking }" @click="handlePositionClick(pos)">
            <template v-if="pos.player">
              <div class="badge-frame">
                <div class="badge-ring"></div>
                <span class="badge-num">{{ pos.player.playerNumber }}</span>
                <span class="badge-name">{{ pos.player.playerName }}</span>
                <span v-if="pos.player.isSheriff && pos.player.isAlive" class="badge-crown">👑</span>
                <span v-if="!pos.player.isAlive" class="badge-skull">💀</span>
                <span v-if="gameStarted && pos.player.role && canViewRole(pos.player)" class="badge-role" :class="getRoleClass(pos.player.role)">{{ getRoleName(pos.player.role) }}</span>
              </div>
            </template>
            <template v-else-if="!pos.locked">
              <div class="badge-empty">
                <span class="badge-plus">+</span>
                <span>{{ $locale==='zh-CN'?'添加':'Add' }}</span>
              </div>
            </template>
            <template v-else>
              <div class="badge-locked">
                <span>🔒</span>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- Center: Dialogue -->
      <div class="center-panel">
        <div v-if="gameStarted && currentViewMode === 'god'" class="game-ledger">
          <div class="ledger-item">
            <span class="ledger-label">{{ $locale === 'zh-CN' ? '流程' : 'PHASE' }}</span>
            <strong>{{ phaseLabel }}</strong>
          </div>
          <div class="ledger-item">
            <span class="ledger-label">{{ $locale === 'zh-CN' ? '存活' : 'ALIVE' }}</span>
            <strong>{{ alivePlayers.length }} / {{ players.length }}</strong>
          </div>
          <div v-if="speechOrder.length" class="ledger-item ledger-order">
            <span class="ledger-label">{{ $locale === 'zh-CN' ? '发言顺序' : 'SPEAKING ORDER' }}</span>
            <strong>{{ speechOrder.map(id => getPlayerNumberById(id)).join(' → ') }}</strong>
          </div>
          <div v-if="nightState.explanation" class="ledger-item ledger-result">
            <span class="ledger-label">{{ $locale === 'zh-CN' ? '夜间结算' : 'NIGHT RESULT' }}</span>
            <strong>{{ nightState.explanation }}</strong>
          </div>
        </div>

        <!-- AI Thinking -->
        <div v-if="currentViewMode === 'god' && aiThinkingPlayers.length" class="thinking-bar">
          <span v-for="pid in aiThinkingPlayers" :key="pid" class="thinking-item">
            <span class="thinking-dot"></span>
            {{ $t('gamePlay.thinking', { name: getPlayerNameById(pid) }) }}
          </span>
        </div>

        <!-- AI Speech Card -->
        <div v-if="aiSpeakingContent" class="speech-flash">
          <div class="speech-card">
            <div class="speech-header">
              <span class="speech-avatar">{{ getPlayerEmoji(aiSpeakingContent.playerId) }}</span>
              <div>
                <div class="speech-name">{{ aiSpeakingContent.playerName }}</div>
                <div class="speech-label">{{ speechPaused || isGamePaused ? ($locale === 'zh-CN' ? '发言已暂停' : 'Speech paused') : ($locale==='zh-CN'?'正在发言':'Speaking') }}</div>
              </div>
              <div class="speech-timer" :class="{ warning: speechTimeRemaining <= 20, paused: speechPaused || isGamePaused }">
                <strong>{{ formattedSpeechTime }}</strong>
                <small>{{ $locale === 'zh-CN' ? '120秒内' : '120 SEC LIMIT' }}</small>
              </div>
            </div>

            <!-- Thinking section -->
            <div v-if="currentViewMode === 'god' && showThinking && aiSpeakingContent.thinking" class="think-box">
              <div class="think-title">🧠 {{ $t('gamePlay.thinkingProcess') }}</div>
              <div class="think-text">{{ aiSpeakingContent.thinking }}</div>
            </div>

            <!-- Speech bubble -->
            <div class="speech-bubble">
              <div class="bubble-arrow"></div>
              <div class="bubble-text">{{ typewriterDisplayedText }}<span v-if="isTyping" class="cursor-blink">|</span></div>
            </div>

            <div class="speech-actions">
              <button class="speech-btn" :disabled="isGamePaused" @click="toggleSpeechPause">
                {{ speechPaused ? ($locale === 'zh-CN' ? '继续发言' : 'Continue') : ($locale === 'zh-CN' ? '暂停发言' : 'Pause speech') }}
              </button>
              <button v-if="isTyping" class="speech-btn" :disabled="isGamePaused" @click="skipTypewriter">{{ $locale === 'zh-CN' ? '显示全文' : 'Show full text' }}</button>
              <button class="speech-btn primary" :disabled="isTyping || speechPaused || isGamePaused" @click="passSpeech">
                {{ $locale === 'zh-CN' ? '过麦' : 'Pass' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Chat messages -->
        <div class="chat-scroll-shell">
          <div class="chat-area" ref="dialogContentRef" @scroll.passive="handleChatScroll">
            <div v-for="(msg, i) in filteredDialogMessages" :key="i" class="chat-msg" :class="msg.type">
            <!-- Referee message -->
            <template v-if="msg.type === 'referee'">
              <div class="referee-msg">
                <span class="referee-icon">⚖️</span>
                <span>
                  {{ msg.content }}
                  <small v-if="msg.detail && currentViewMode === 'god'" class="referee-detail">{{ msg.detail }}</small>
                </span>
              </div>
            </template>
            <template v-else-if="msg.type === 'thinking' || msg.type === 'night-action' || msg.type === 'vote-action'">
              <div class="private-log" :class="msg.type">
                <span class="private-log-label">{{ msg.type === 'thinking' ? 'AI THINKING' : (msg.type === 'vote-action' ? 'PRIVATE VOTE' : 'NIGHT ACTION') }}</span>
                <strong>{{ msg.sender }}</strong>
                <span>{{ msg.content }}</span>
              </div>
            </template>
            <!-- Player / Wolf message as bubble -->
            <template v-else>
              <div class="bubble-row" :class="{ mine: msg.sender === ($locale==='zh-CN'?'我':'Me') || isOwnMessage(msg.sender) }">
                <div class="bubble-avatar">{{ getSenderEmoji(msg.sender) }}</div>
                <div class="bubble-wrap">
                  <div class="bubble-sender">{{ msg.sender }}</div>
                  <div class="bubble-body" :class="{ wolf: msg.type === 'wolf' }">
                    {{ msg.content }}
                  </div>
                  <div class="bubble-time">{{ msg.time }}</div>
                </div>
              </div>
            </template>
            </div>
          </div>
          <button v-if="showScrollToBottom" class="scroll-bottom-btn" @click="scrollToBottom(true)" :aria-label="$locale === 'zh-CN' ? '回到底部' : 'Back to latest'">
            <span>↓</span>{{ $locale === 'zh-CN' ? '回到底部' : 'LATEST' }}
          </button>
        </div>

        <!-- Input -->
        <div class="chat-input">
          <el-input v-model="inputMessage" type="textarea" :rows="2" :placeholder="$t('gamePlay.inputPlaceholder')" :disabled="!canSpeak || isGamePaused" resize="none" @keydown.enter.ctrl="sendMessage" />
          <button class="send-btn" :disabled="!canSpeak || isGamePaused || !inputMessage.trim()" @click="sendMessage">➤</button>
        </div>
      </div>

      <!-- Right: Players 7-12 -->
      <div class="side-panel">
        <div class="player-badges">
          <div v-for="pos in rightPositions" :key="pos.number" class="player-badge" :class="{ occupied: pos.player, locked: pos.locked, dead: pos.player && !pos.player.isAlive, speaking: pos.player?.isSpeaking }" @click="handlePositionClick(pos)">
            <template v-if="pos.player">
              <div class="badge-frame">
                <div class="badge-ring"></div>
                <span class="badge-num">{{ pos.player.playerNumber }}</span>
                <span class="badge-name">{{ pos.player.playerName }}</span>
                <span v-if="pos.player.isSheriff && pos.player.isAlive" class="badge-crown">👑</span>
                <span v-if="!pos.player.isAlive" class="badge-skull">💀</span>
                <span v-if="gameStarted && pos.player.role && canViewRole(pos.player)" class="badge-role" :class="getRoleClass(pos.player.role)">{{ getRoleName(pos.player.role) }}</span>
              </div>
            </template>
            <template v-else-if="!pos.locked">
              <div class="badge-empty">
                <span class="badge-plus">+</span>
                <span>{{ $locale==='zh-CN'?'添加':'Add' }}</span>
              </div>
            </template>
            <template v-else>
              <div class="badge-locked">
                <span>🔒</span>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- Add AI Dialog -->
    <el-dialog v-model="addAiDialogVisible" :title="$t('gamePlay.addAiPlayer')" width="400px">
      <el-form>
        <el-form-item :label="$t('gamePlay.selectAiPlayer')">
          <el-select v-model="selectedAiPlayerId" :placeholder="$t('gamePlay.selectAiPlayer')" style="width:100%">
            <el-option v-for="ai in availableAiPlayers" :key="ai.id" :label="ai.name" :value="ai.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addAiDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmAddAiPlayer" :disabled="!selectedAiPlayerId">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, getCurrentInstance, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { useTypewriter } from '../../composables/useTypewriter.js'
import { speakText, stopSpeaking, pauseSpeaking, resumeSpeaking } from '../../composables/useSpeechSynthesis.js'
import { PACK_WOLF_ROLES, WEREWOLF_KNOWLEDGE, WOLF_TEAM_ROLES, getBoardRules, getRoleSummary, isPackWolfRole, isWolfTeamRole } from '../../game/rules.js'

const { proxy } = getCurrentInstance()
const $t = proxy.$t
const $locale = proxy.$locale

const route = useRoute()
const router = useRouter()
const roomId = route.params.roomId || route.params.id || 1

const { displayedText: typewriterDisplayedText, isTyping, startTypewriter, pauseTypewriter, resumeTypewriter, skipToEnd, setSpeed } = useTypewriter()

function createEmptyNightState() {
  return {
    guardTargetId: null,
    previousGuardTargetId: null,
    wolfVotes: [],
    wolfTargetId: null,
    wolfTieCandidates: [],
    witchSaved: false,
    witchPoisonTargetId: null,
    miraclePoisonTargetId: null,
    miracleGuardTargetId: null,
    miracleSkillTargetId: null,
    wolfBeautyTargetId: null,
    gargoyleTargetId: null,
    gargoyleResult: null,
    gravediggerResult: null,
    seerTargetId: null,
    seerResult: null,
    deaths: [],
    explanation: ''
  }
}

// State
const players = ref([])
const currentRound = ref(1)
const currentDay = ref(1)
const currentPhase = ref('night')
const gameStarted = ref(false)
const roomInfo = ref({})
const aiThinkingPlayers = ref([])
const aiSpeakingContent = ref(null)
const aiPlayers = ref([])
const selectedAiPlayerId = ref(null)
const addAiDialogVisible = ref(false)
const currentPosition = ref(null)
const currentViewMode = ref('god')
const selectedPlayerId = ref(null)
const showThinking = ref(localStorage.getItem('showThinking') === 'true')
const typewriterSpeed = ref(localStorage.getItem('typewriterSpeed') || 'normal')
const dialogContentRef = ref(null)
const dialogMessages = ref([])
const inputMessage = ref('')
const speakingPlayer = ref(null)
const speechOrder = ref([])
const speechIndex = ref(-1)
const phaseRunning = ref(false)
const isGamePaused = ref(false)
const speechTimeRemaining = ref(120)
const speechPaused = ref(false)
const isChatNearBottom = ref(true)
const showScrollToBottom = ref(false)
const lastPublicNightReport = ref('')
const lastGuardTargetId = ref(null)
const voteHistory = ref([])
const sheriffDirection = ref('clockwise')
const sheriffElectionDone = ref(false)
const lastExiledPlayerId = ref(null)
const lastWordsGiven = ref(false)
const dayInterrupted = ref(false)
const miracleMerchantState = reactive({ used: false, merchantId: null, luckyId: null, skill: null, pendingDeath: false })
const wolfBeautyState = reactive({ previousTargetId: null, targetId: null })
const lastApiRequestTime = ref(0)
const API_REQUEST_INTERVAL = 1200
const aiConfigCache = new Map()
const playerMemories = reactive({})
const gameRules = Object.freeze({
  guardCanSelfProtect: true,
  guardCannotRepeatTarget: true,
  sameGuardAndSaveKills: true
})
const witchInventory = reactive({ antidote: 1, poison: 1 })
const nightState = reactive(createEmptyNightState())
let gameLoopVersion = 0
let activeSpeechResolver = null
let activeSpeechTimer = null
let speechPausedByGame = false
let pauseWaiters = []
let hunterSkillUsed = new Set()
let wolfKingSkillUsed = new Set()
let knightSkillUsed = new Set()

const SPEEDS = { slow: 80, normal: 50, fast: 30 }
const SPEECH_LIMIT_SECONDS = 120
const speeds = [{ key: 'slow', label: $locale === 'zh-CN' ? '慢' : 'S' }, { key: 'normal', label: $locale === 'zh-CN' ? '常' : 'N' }, { key: 'fast', label: $locale === 'zh-CN' ? '快' : 'F' }]
const viewModes = [{ key: 'god', label: $t('gamePlay.godView') }, { key: 'player', label: $t('gamePlay.playerView') }, { key: 'spectator', label: $t('gamePlay.spectatorView') }]

watch(showThinking, v => localStorage.setItem('showThinking', v))
watch(typewriterSpeed, v => { localStorage.setItem('typewriterSpeed', v); setSpeed(v) })
setSpeed(typewriterSpeed.value)

// Computed
const alivePlayers = computed(() => players.value.filter(p => p.isAlive))
const wolfRolesArr = WOLF_TEAM_ROLES
const boardRules = computed(() => getBoardRules(roomInfo.value.gameBoard || route.query.gameBoard, roomInfo.value.playerCount || 12))
const roleAliases = {
  guard: ['守卫', 'Guard'],
  witch: ['女巫', 'Witch'],
  seer: ['预言家', 'Seer'],
  hunter: ['猎人', 'Hunter'],
  wolfKing: ['狼王'],
  whiteWolf: ['白狼王'],
  wolfBeauty: ['狼美人'],
  miracle: ['奇迹商人'],
  gargoyle: ['石像鬼'],
  gravedigger: ['守墓人'],
  knight: ['骑士']
}
const phaseLabel = computed(() => {
  const zh = currentLocale() === 'zh-CN'
  const m = {
    night: zh ? '夜间准备' : 'Night setup',
    night_miracle: zh ? '奇迹商人行动' : 'Merchant action',
    night_gargoyle: zh ? '石像鬼查验' : 'Gargoyle check',
    night_gravedigger: zh ? '守墓人行动' : 'Gravedigger action',
    night_guard: zh ? '守卫行动' : 'Guard action',
    night_wolf: zh ? '狼人行动' : 'Wolf action',
    night_wolf_beauty: zh ? '狼美人行动' : 'Wolf Beauty action',
    night_witch: zh ? '女巫行动' : 'Witch action',
    night_seer: zh ? '预言家行动' : 'Seer action',
    day: $t('gamePlay.dayPhase'),
    sheriff: zh ? '警长竞选' : 'Sheriff election',
    speak: $t('gamePlay.speakPhase'),
    vote: $t('gamePlay.votePhase')
  }
  return m[currentPhase.value] || ''
})
const boardLabel = computed(() => $t(`gameBoard.${roomInfo.value.gameBoard || 'standard'}`))
const formattedSpeechTime = computed(() => {
  const minutes = Math.floor(speechTimeRemaining.value / 60)
  const seconds = String(speechTimeRemaining.value % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
})
const leftPositions = computed(() => {
  const ps = []
  for (let i = 1; i <= 6; i++) ps.push({ number: i, player: players.value.find(p => p.playerNumber === i), locked: roomInfo.value.playerCount && i > roomInfo.value.playerCount })
  return ps
})
const rightPositions = computed(() => {
  const ps = []
  for (let i = 7; i <= 12; i++) ps.push({ number: i, player: players.value.find(p => p.playerNumber === i), locked: roomInfo.value.playerCount && i > roomInfo.value.playerCount })
  return ps
})
const availableAiPlayers = computed(() => {
  const used = players.value.map(p => p.aiPlayerId).filter(id => id)
  return aiPlayers.value.filter(ai => !used.includes(ai.id))
})
const filteredDialogMessages = computed(() => {
  if (currentViewMode.value === 'god') return dialogMessages.value
  if (currentViewMode.value === 'spectator') return dialogMessages.value.filter(m => (m.visibility || 'public') === 'public')
  if (currentViewMode.value === 'player' && selectedPlayerId.value) {
    const sp = players.value.find(p => p.id === selectedPlayerId.value)
    if (!sp) return []
    return dialogMessages.value.filter(m => {
      const visibility = m.visibility || 'public'
      if (visibility === 'public') return true
      if (visibility === 'private') return m.privateFor === sp.id
      if (visibility === 'wolves') return isPackWolf(sp)
      return visibility === 'god' ? false : m.sender === sp.playerName
    })
  }
  return dialogMessages.value.filter(m => (m.visibility || 'public') === 'public')
})
const canSpeak = computed(() => {
  if (currentPhase.value === 'speak' && speakingPlayer.value) {
    if (selectedPlayerId.value) return speakingPlayer.value === selectedPlayerId.value
    return true
  }
  if (currentPhase.value === 'night' && selectedPlayerId.value) {
    const sp = players.value.find(p => p.id === selectedPlayerId.value)
    return sp && isPackWolf(sp)
  }
  return false
})

// Helpers
const getRoleName = (role) => {
  const m = { '狼人':'gamePlay.role.werewolf','狼王':'gamePlay.role.wolfKing','狼美人':'gamePlay.role.wolfBeauty','白狼王':'gamePlay.role.whiteWolf','石像鬼':'gamePlay.role.gargoyle','平民':'gamePlay.role.villager','预言家':'gamePlay.role.seer','女巫':'gamePlay.role.witch','猎人':'gamePlay.role.hunter','守卫':'gamePlay.role.guard','奇迹商人':'gamePlay.role.miracleMerchant','守墓人':'gamePlay.role.gravekeeper','愚者':'gamePlay.role.fool','骑士':'gamePlay.role.knight' }
  return m[role] ? $t(m[role]) : role
}
const getRoleClass = (role) => {
  if (wolfRolesArr.includes(role)) return 'wolf'
  if (['平民','Villager'].includes(role)) return 'villager'
  return 'god'
}
const getPlayerNameById = (pid) => { const p = players.value.find(x => x.id === pid); return p ? p.playerName : '' }
const getPlayerEmoji = (pid) => {
  const p = players.value.find(x => x.id === pid)
  if (!p) return '🧑'
  if (!p.isAlive) return '💀'
  if (p.isSheriff) return '👑'
  if (wolfRolesArr.includes(p.role)) return '🐺'
  if (['预言家','Seer'].includes(p.role)) return '🔮'
  if (['女巫','Witch'].includes(p.role)) return '🧪'
  if (['猎人','Hunter'].includes(p.role)) return '🏹'
  if (['守卫','Guard'].includes(p.role)) return '🛡️'
  return '🧑'
}
const getSenderEmoji = (name) => {
  const p = players.value.find(x => x.playerName === name)
  return p ? getPlayerEmoji(p.id) : '🧑'
}
const isOwnMessage = (sender) => {
  if (!selectedPlayerId.value) return false
  const sp = players.value.find(p => p.id === selectedPlayerId.value)
  return sp && sp.playerName === sender
}
const currentLocale = () => ($locale?.value || $locale) === 'en-US' ? 'en-US' : 'zh-CN'
const getPlayerNumberById = (pid) => players.value.find(p => p.id === pid)?.playerNumber || '?'
const canViewRole = (player) => currentViewMode.value === 'god' || (currentViewMode.value === 'player' && selectedPlayerId.value === player.id)
const hasRole = (player, key) => roleAliases[key]?.includes(player?.role)
const isWolfRole = player => isWolfTeamRole(player?.role)
const isPackWolf = player => isPackWolfRole(player?.role)
const isBoard = key => boardRules.value.key === key

// Game setup
const startGame = async () => {
  const required = roomInfo.value.playerCount || 12
  if (players.value.length < required) { ElMessage.warning($t('gamePlay.notEnoughPlayers', { count: required })); return }
  await loadGameData()
  await distributeRoles()
  players.value.forEach(p => { if (p.aiPlayerId) p.userId = -1 })
  initializeGameState()
  await startGamePhase()
  gameStarted.value = true
  notifyPlayerRoles()
  const version = ++gameLoopVersion
  void runGameLoop(version)
}
const notifyPlayerRoles = () => {
  players.value.forEach(p => {
    const teammates = isPackWolf(p)
      ? players.value.filter(other => other.id !== p.id && isPackWolf(other)).map(other => `${other.playerNumber}号${other.playerName}`).join('、')
      : ''
    const winCondition = isWolfRole(p) ? '所有平民或所有神职出局；石像鬼属于狼人阵营' : '放逐全部狼人阵营玩家'
    const content = currentLocale() === 'zh-CN'
      ? `你的身份是${getRoleName(p.role)}，胜利条件：${winCondition}。${teammates ? `狼队友：${teammates}。` : '除身份技能获得的信息外，其他玩家身份均未知。'}本局板子规则：${boardRules.value.special}`
      : `Your role is ${getRoleName(p.role)}. Win condition: ${isWolfRole(p) ? 'eliminate every villager or every good role' : 'eliminate every wolf-team player'}.${teammates ? ` Wolf teammates: ${teammates}.` : ' Other roles are unknown unless learned through your ability.'} Board rule: ${boardRules.value.special}`
    addGameMessage({ sender: $t('gamePlay.referee'), content, type: 'referee', visibility: 'private', privateFor: p.id })
  })
}
const exitGame = () => {
  ElMessageBox.confirm($t('gamePlay.confirmExit'), $t('gamePlay.exitGameTitle'), { confirmButtonText: $t('common.confirm'), cancelButtonText: $t('common.cancel'), type: 'warning' })
    .then(() => {
      gameStarted.value = false
      phaseRunning.value = false
      isGamePaused.value = false
      gameLoopVersion++
      releasePauseWaiters()
      closeSpeech('exit')
      stopSpeaking()
      router.push(`/game/room/detail/${roomId}`)
    }).catch(() => {})
}
const handlePositionClick = (pos) => {
  if (pos.locked) return
  if (pos.player) {
    ElMessageBox.confirm($t('gamePlay.deleteConfirm', { name: pos.player.playerName }), $t('gamePlay.deletePlayerTitle'), { confirmButtonText: $t('common.confirm'), cancelButtonText: $t('common.cancel'), type: 'warning' })
      .then(async () => {
        try { await axios.post('/game/player/remove', { roomId, playerId: pos.player.id }); ElMessage.success($t('gamePlay.playerDeleted', { name: pos.player.playerName })); await loadGameData() } catch (e) { ElMessage.error($t('common.failed')) }
      }).catch(() => {})
  } else if (!gameStarted.value) { currentPosition.value = pos; addAiDialogVisible.value = true }
}
const confirmAddAiPlayer = async () => {
  if (!selectedAiPlayerId.value || !currentPosition.value) return
  const ai = aiPlayers.value.find(x => x.id == selectedAiPlayerId.value)
  if (!ai) return
  try {
    const r = await axios.post('/game/player/add', { roomId, userId: -1, aiPlayerId: selectedAiPlayerId.value, playerNumber: currentPosition.value.number, playerName: ai.name })
    if (r.data.code === 200 || r.status === 200) { ElMessage.success($t('gamePlay.aiPlayerAdded', { name: ai.name })); await loadGameData(); selectedAiPlayerId.value = null; currentPosition.value = null; addAiDialogVisible.value = false }
  } catch (e) { ElMessage.error($t('common.failed')) }
}
const loadGameData = async () => {
  try {
    const rr = await axios.get(`/game/room/info/${roomId}`)
    let room = rr.data.code === 200 ? rr.data.data : rr.data
    if (room) {
      roomInfo.value = room
      try {
        const pr = await axios.get(`/game/player/list/${roomId}`)
        let pl = pr.data.code === 200 ? pr.data.data : (Array.isArray(pr.data) ? pr.data : [])
        players.value = pl.map(p => ({ id: p.id, playerNumber: p.playerNumber || p.id, playerName: p.playerName || p.name || `P${p.id}`, role: p.role || '', isAlive: p.status === 1, isSpeaking: false, isSheriff: p.isSheriff === 1, userId: p.userId, aiPlayerId: p.aiPlayerId }))
      } catch (e) { players.value = [] }
      try { const ar = await axios.get('/ai/player/available'); aiPlayers.value = ar.data.code === 200 ? ar.data.data : (Array.isArray(ar.data) ? ar.data : []) } catch (e) {}
    }
    if (players.value.length) selectedPlayerId.value = players.value[0].id
    if (!dialogMessages.value.length) {
      addRefereeMessage($t('gamePlay.gameWelcome'))
      addRefereeMessage(currentLocale() === 'zh-CN'
        ? `本局为${roomInfo.value.playerCount || 12}人场，游戏板子：${boardLabel.value}。角色将严格按照该板子配置分配。`
        : `This is a ${roomInfo.value.playerCount || 12}-player game using the ${boardLabel.value} board. Roles follow this board configuration.`)
      addRefereeMessage(currentLocale() === 'zh-CN'
        ? `板子规则：${boardRules.value.special} 角色配置：${getRoleSummary(boardRules.value)}。`
        : `Board rules: ${boardRules.value.special} Roles: ${getRoleSummary(boardRules.value)}.`)
      addRefereeMessage($t('gamePlay.gameHelp'))
    }
  } catch (e) { roomInfo.value = { playerCount: 12 }; players.value = [] }
}
const distributeRoles = async () => {
  try {
    const bc = await loadBoardConfig(); const roles = []; bc.roles.forEach(rc => { for (let i = 0; i < rc.count; i++) roles.push(rc.role) })
    shuffleArray(roles); players.value.forEach((p, i) => { p.role = i < roles.length ? roles[i] : '平民' })
    addRefereeMessage($t('gamePlay.rolesDistributed', { roles: bc.roles.map(r => `${getRoleName(r.role)}×${r.count}`).join('、') }))
  } catch (e) {}
}
const loadBoardConfig = async () => {
  return boardRules.value
}
const shuffleArray = (a) => { for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [a[i], a[j]] = [a[j], a[i]] } return a }
const startGamePhase = () => { currentPhase.value = 'night' }

const sendMessage = () => {
  if (!inputMessage.value.trim() || !canSpeak.value || isGamePaused.value) return
  const cp = players.value.find(p => p.id === selectedPlayerId.value)
  const name = cp ? cp.playerName : ($locale==='zh-CN'?'我':'Me')
  let type = 'player'
  if (currentPhase.value === 'night' && cp && isPackWolf(cp)) type = 'wolf'
  addGameMessage({ sender: name, content: inputMessage.value.trim(), type, visibility: type === 'wolf' ? 'wolves' : 'public' })
  inputMessage.value = ''; scrollToBottom()
}
const addGameMessage = ({ sender, content, type = 'player', visibility = 'public', privateFor = null, detail = '' }) => {
  dialogMessages.value.push({ sender, content, time: new Date().toLocaleTimeString(), type, visibility, privateFor, detail })
  scrollToBottom()
}
const addRefereeMessage = (content, options = {}) => addGameMessage({
  sender: $t('gamePlay.referee'), content, type: 'referee', visibility: options.visibility || 'public',
  privateFor: options.privateFor || null, detail: options.detail || ''
})
const addDialogMessage = (sender, content, type = 'player', options = {}) => addGameMessage({
  sender, content, type, visibility: options.visibility || 'public', privateFor: options.privateFor || null, detail: options.detail || ''
})
const handleChatScroll = () => {
  const el = dialogContentRef.value
  if (!el) return
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight
  isChatNearBottom.value = distance <= 80
  showScrollToBottom.value = !isChatNearBottom.value
}
const scrollToBottom = (force = false) => nextTick(() => {
  const el = dialogContentRef.value
  if (!el) return
  if (force || isChatNearBottom.value) {
    el.scrollTop = el.scrollHeight
    isChatNearBottom.value = true
    showScrollToBottom.value = false
  } else {
    showScrollToBottom.value = true
  }
})

const releasePauseWaiters = () => {
  const waiters = pauseWaiters
  pauseWaiters = []
  waiters.forEach(resolve => resolve())
}
const waitWhilePaused = async () => {
  while (isGamePaused.value && gameStarted.value) {
    await new Promise(resolve => pauseWaiters.push(resolve))
  }
}
const pauseSpeechPlayback = () => {
  if (!aiSpeakingContent.value || speechPaused.value) return
  speechPaused.value = true
  pauseTypewriter()
  pauseSpeaking()
}
const resumeSpeechPlayback = () => {
  if (!aiSpeakingContent.value || !speechPaused.value) return
  speechPaused.value = false
  resumeTypewriter()
  resumeSpeaking()
}
const toggleSpeechPause = () => {
  if (isGamePaused.value || !aiSpeakingContent.value) return
  if (speechPaused.value) resumeSpeechPlayback()
  else pauseSpeechPlayback()
}
const toggleGamePause = () => {
  if (!gameStarted.value) return
  if (!isGamePaused.value) {
    isGamePaused.value = true
    speechPausedByGame = Boolean(aiSpeakingContent.value && !speechPaused.value)
    if (speechPausedByGame) pauseSpeechPlayback()
    addRefereeMessage(currentLocale() === 'zh-CN' ? '游戏已暂停，所有阶段计时与流程推进均已冻结。' : 'Game paused. All timers and phase progression are frozen.')
    return
  }

  isGamePaused.value = false
  if (speechPausedByGame) resumeSpeechPlayback()
  speechPausedByGame = false
  releasePauseWaiters()
  addRefereeMessage(currentLocale() === 'zh-CN' ? '游戏继续。' : 'Game resumed.')
}
const startSpeechTimer = () => {
  if (activeSpeechTimer) clearInterval(activeSpeechTimer)
  speechTimeRemaining.value = SPEECH_LIMIT_SECONDS
  activeSpeechTimer = setInterval(() => {
    if (isGamePaused.value || speechPaused.value) return
    speechTimeRemaining.value = Math.max(0, speechTimeRemaining.value - 1)
    if (speechTimeRemaining.value === 0) closeSpeech('timeout')
  }, 1000)
}
const closeSpeech = (reason = 'pass') => {
  skipToEnd()
  stopSpeaking()
  if (activeSpeechTimer) clearInterval(activeSpeechTimer)
  activeSpeechTimer = null
  speechPaused.value = false
  speechPausedByGame = false
  if (reason === 'timeout' && aiSpeakingContent.value) {
    addRefereeMessage(currentLocale() === 'zh-CN' ? '本次发言时间已到，主持人自动过麦。' : 'Speaking time expired. The moderator passed the turn.')
  }
  aiSpeakingContent.value = null
  if (activeSpeechResolver) {
    const resolve = activeSpeechResolver
    activeSpeechResolver = null
    resolve()
  }
}
const skipTypewriter = () => {
  skipToEnd()
  stopSpeaking()
  speechPaused.value = false
}
const passSpeech = () => {
  if (isTyping.value || speechPaused.value || isGamePaused.value) return
  closeSpeech('pass')
}

const startPlayerSpeaking = (pid) => {
  if (!gameStarted.value) return
  players.value.forEach(p => { p.isSpeaking = p.id === pid })
  speakingPlayer.value = players.value.find(p => p.id === pid)
  if (speakingPlayer.value) {
    addRefereeMessage(currentLocale() === 'zh-CN'
      ? `请${speakingPlayer.value.playerNumber}号${speakingPlayer.value.playerName}发言。`
      : `Player ${speakingPlayer.value.playerNumber}, ${speakingPlayer.value.playerName}, please speak.`)
  }
}
const endPlayerSpeaking = () => {
  players.value.forEach(p => { p.isSpeaking = false })
  if (speakingPlayer.value) addRefereeMessage(currentLocale() === 'zh-CN'
    ? `${speakingPlayer.value.playerNumber}号发言结束。`
    : `Player ${speakingPlayer.value.playerNumber} has finished speaking.`)
  speakingPlayer.value = null
}
const setSheriff = (pid) => {
  players.value.forEach(p => { p.isSheriff = p.id === pid })
  const s = players.value.find(p => p.id === pid)
  if (s) addRefereeMessage($t('gamePlay.sheriffElected', { name: s.playerName }))
}
const killPlayer = (pid) => {
  const p = players.value.find(x => x.id === pid)
  if (p?.isAlive) { p.isAlive = false; p.isSpeaking = false }
}
const checkGameEnd = () => {
  const alive = alivePlayers.value
  const aw = alive.filter(isWolfRole).length
  const villagers = alive.filter(player => ['平民', 'Villager'].includes(player.role)).length
  const gods = alive.filter(player => !isWolfRole(player) && !['平民', 'Villager'].includes(player.role)).length
  if (aw === 0) { addRefereeMessage($t('gamePlay.goodWin')); gameStarted.value = false; phaseRunning.value = false; return true }
  if (villagers === 0 || gods === 0) { addRefereeMessage($t('gamePlay.wolfWin')); gameStarted.value = false; phaseRunning.value = false; return true }
  if (!alivePlayers.value.length) { addRefereeMessage($t('gamePlay.allDead')); gameStarted.value = false; phaseRunning.value = false; return true }
  return false
}

const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))
const phaseDelay = async (ms = 500) => {
  let remaining = Math.max(120, Math.round(ms * ({ slow: 1, normal: 0.65, fast: 0.35 }[typewriterSpeed.value] || 0.65)))
  while (remaining > 0 && gameStarted.value) {
    await waitWhilePaused()
    const slice = Math.min(remaining, 100)
    await delay(slice)
    if (!isGamePaused.value) remaining -= slice
  }
}
const randomItem = (items) => items.length ? items[Math.floor(Math.random() * items.length)] : null
const isLoopActive = (version) => gameStarted.value && gameLoopVersion === version

const initializeGameState = () => {
  currentRound.value = 1
  currentDay.value = 1
  currentPhase.value = 'night'
  isGamePaused.value = false
  speechPaused.value = false
  speechTimeRemaining.value = SPEECH_LIMIT_SECONDS
  speechOrder.value = []
  speechIndex.value = -1
  voteHistory.value = []
  lastPublicNightReport.value = ''
  lastGuardTargetId.value = null
  hunterSkillUsed = new Set()
  wolfKingSkillUsed = new Set()
  knightSkillUsed = new Set()
  sheriffDirection.value = 'clockwise'
  sheriffElectionDone.value = false
  lastExiledPlayerId.value = null
  lastWordsGiven.value = false
  dayInterrupted.value = false
  Object.assign(miracleMerchantState, { used: false, merchantId: null, luckyId: null, skill: null, pendingDeath: false })
  Object.assign(wolfBeautyState, { previousTargetId: null, targetId: null })
  witchInventory.antidote = 1
  witchInventory.poison = 1
  Object.assign(nightState, createEmptyNightState())
  Object.keys(playerMemories).forEach(key => delete playerMemories[key])
  players.value.forEach(player => {
    player.isAlive = true
    player.isSpeaking = false
    playerMemories[player.id] = { privateKnowledge: [], speeches: [], votes: [], checks: [] }
  })
}

const resetNightState = () => {
  Object.assign(nightState, createEmptyNightState(), { previousGuardTargetId: lastGuardTargetId.value })
}

const publicSituationSummary = (language = currentLocale()) => {
  const alive = alivePlayers.value.map(p => language === 'en-US' ? String(p.playerNumber) : `${p.playerNumber}号`).join(language === 'en-US' ? ', ' : '、') || (language === 'en-US' ? 'none' : '无')
  const dead = players.value.filter(p => !p.isAlive).map(p => language === 'en-US' ? String(p.playerNumber) : `${p.playerNumber}号`).join(language === 'en-US' ? ', ' : '、') || (language === 'en-US' ? 'none' : '无')
  return language === 'zh-CN'
    ? `第${currentDay.value}天，存活${alivePlayers.value.length}人（${alive}），已死亡：${dead}。`
    : `Day ${currentDay.value}. ${alivePlayers.value.length} alive (${alive}); dead: ${dead}.`
}

const getPublicHistory = () => dialogMessages.value
  .filter(message => (message.visibility || 'public') === 'public' && ['player', 'referee'].includes(message.type))
  .slice(-28)
  .map(message => `${message.sender}: ${message.content}`)

const getAiConfig = async (player) => {
  if (!player?.aiPlayerId) return { language: 'zh-CN', maxTokens: 1000 }
  if (aiConfigCache.has(player.aiPlayerId)) return aiConfigCache.get(player.aiPlayerId)
  try {
    const response = await axios.get(`/ai/player/info/${player.aiPlayerId}`)
    const config = response.data.code === 200 ? response.data.data : null
    if (config) {
      config.language = config.language || 'zh-CN'
      aiConfigCache.set(player.aiPlayerId, config)
      return config
    }
  } catch (error) {
    addRefereeMessage('AI 配置读取失败，已使用本地决策继续游戏。', { visibility: 'god', detail: error.message })
  }
  return { language: 'zh-CN', maxTokens: 1000 }
}

const parseStructuredResponse = (content) => {
  if (!content) return null
  const stripped = String(content).replace(/```(?:json)?/gi, '').replace(/```/g, '').trim()
  const start = stripped.indexOf('{')
  const end = stripped.lastIndexOf('}')
  if (start < 0 || end <= start) return null
  try { return JSON.parse(stripped.slice(start, end + 1)) } catch { return null }
}

const normalizeApiUrl = (baseUrl) => {
  let url = (baseUrl || 'https://api.deepseek.com/v1').trim().replace(/\/+$/, '')
  if (!url.endsWith('/chat/completions')) url += '/chat/completions'
  return url
}

const callStructuredAi = async (config, systemPrompt, userPrompt) => {
  if (!config?.apiKey) return null
  const sinceLastRequest = Date.now() - lastApiRequestTime.value
  if (sinceLastRequest < API_REQUEST_INTERVAL) await delay(API_REQUEST_INTERVAL - sinceLastRequest)
  lastApiRequestTime.value = Date.now()
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 45000)
  try {
    const temperature = typeof config.temperature === 'number'
      ? (config.temperature > 2 ? config.temperature / 10 : config.temperature)
      : 0.7
    const body = {
      model: config.modelName || 'deepseek-chat',
      messages: [{ role: 'system', content: systemPrompt }, { role: 'user', content: userPrompt }],
      temperature,
      max_tokens: Math.min(2200, Math.max(900, Number(config.maxTokens) || 1000)),
      response_format: { type: 'json_object' }
    }
    const response = await fetch(normalizeApiUrl(config.apiBaseUrl), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${config.apiKey}` },
      body: JSON.stringify(body),
      signal: controller.signal
    })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const data = await response.json()
    const message = data.choices?.[0]?.message || data.output?.choices?.[0]?.message || {}
    const parsed = parseStructuredResponse(message.content)
    if (parsed && !parsed.thinking && message.reasoning_content) parsed.thinking = message.reasoning_content
    return parsed
  } catch (error) {
    addRefereeMessage('模型请求失败，本次已使用本地逻辑继续。', { visibility: 'god', detail: error.name === 'AbortError' ? '请求超时' : error.message })
    return null
  } finally {
    clearTimeout(timeout)
  }
}

const roleVictoryCondition = (player, language) => {
  if (language === 'en-US') return isWolfRole(player)
    ? 'The wolf team wins when every villager or every good special role is eliminated.'
    : 'The good team wins by eliminating every wolf-team role.'
  return isWolfRole(player) ? '所有平民出局或所有神职出局' : '找出并放逐全部狼人阵营角色'
}

const buildPlayerKnowledge = (player, language) => {
  const memory = playerMemories[player.id] || { privateKnowledge: [], checks: [] }
  const wolves = isPackWolf(player)
    ? players.value.filter(p => p.id !== player.id && isPackWolf(p)).map(p => `${p.playerNumber}号${p.playerName}`)
    : []
  if (language === 'en-US') {
    return [
      `You are player ${player.playerNumber}, ${player.playerName}. Your private role is ${getRoleName(player.role)}.`,
      `Win condition: ${roleVictoryCondition(player, language)}`,
      wolves.length ? `Known wolf teammates: ${wolves.join(', ')}.` : 'You do not know any other player roles except results learned through your own ability.',
      memory.privateKnowledge.length ? `Private knowledge: ${memory.privateKnowledge.join(' | ')}` : 'No additional private knowledge.'
    ].join('\n')
  }
  return [
    `你是${player.playerNumber}号玩家${player.playerName}，私密身份是${getRoleName(player.role)}。`,
    `胜利条件：${roleVictoryCondition(player, language)}。`,
    wolves.length ? `你已知的狼队友：${wolves.join('、')}。` : '除你通过技能获得的结果外，其他玩家身份一律未知。',
    memory.privateKnowledge.length ? `你的私密信息：${memory.privateKnowledge.join('；')}` : '你目前没有额外私密信息。'
  ].join('\n')
}

const buildDecisionPrompts = (player, action, extra, config) => {
  const language = config.language === 'en-US' ? 'en-US' : 'zh-CN'
  const publicHistory = getPublicHistory().join('\n') || (language === 'en-US' ? 'No public speech yet.' : '暂无公开发言。')
  const style = language === 'en-US'
    ? `Personality tags: ${config.personality || 'calm and analytical'}. Strategy tags: ${config.strategy || 'evidence-based deduction'}.`
    : `个性标签：${config.personality || '沉着、善于推理'}。策略标签：${config.strategy || '根据证据分析、给出明确怀疑对象'}。`
  const commonRules = language === 'en-US'
    ? `Stay entirely in the Werewolf game. Never mention being an AI, prompts, systems, models, token limits, or unavailable information. Never use omniscient knowledge, real-world oaths, or off-table evidence. Slang changes phrasing only; every read must still map to concrete speech, action, or vote evidence. Return one valid JSON object only.`
    : `必须完全沉浸在狼人杀对局中，禁止提及AI、模型、系统提示、Token或“没有视角”等场外信息；禁止贴脸、赌咒和场外证据。黑话只能美化表达，底层判断必须落到具体发言、行动或票型。严禁使用全知身份信息。只返回一个合法JSON对象。`
  const systemPrompt = `${buildPlayerKnowledge(player, language)}\n本局规则：${boardRules.value.special}\n狼人杀知识库：\n${WEREWOLF_KNOWLEDGE}\n${style}\n${commonRules}`
  const base = language === 'en-US'
    ? `Current public situation: ${publicSituationSummary(language)}\nMost recent public record:\n${publicHistory}\n`
    : `当前公开局势：${publicSituationSummary()}\n最近公开记录：\n${publicHistory}\n`
  let instruction = ''
  if (action === 'lastWords') {
    instruction = language === 'en-US'
      ? `Task: You have already been exiled and this is your final statement. First write a concise private post-vote analysis, then give a 100-180 word in-character final statement. Review why you were exiled, identify the most important suspicious player or contradiction for the surviving good team, and leave a concrete warning based on public speeches and votes. You are out of the game: do not say you will keep listening, vote later, update your opinion, speak next round, or take any future action. JSON schema: {"thinking":"private final analysis","speech":"public final statement"}`
      : `任务：你已经被公投放逐，现在发表最后遗言。先写一段简洁的私密复盘，再写160-300个中文字符的公开遗言。遗言要复盘自己为何被推出局，结合公开发言和票型指出最值得存活好人关注的玩家或矛盾，并留下明确警示。你已经出局，严禁说“继续听发言”“之后再投”“准备投给”“下一轮发言”“再调整判断”等任何自己未来还会参与游戏的内容。JSON格式：{"thinking":"私密最终复盘","speech":"公开遗言"}`
  } else if (action === 'speech' || action === 'pkSpeech' || action === 'sheriffSpeech') {
    const stageName = action === 'pkSpeech' ? '平票PK发言' : action === 'sheriffSpeech' ? '警长竞选发言' : '正常白天发言'
    const specialSchema = hasRole(player, 'whiteWolf') && action === 'speech'
      ? ',"explode":是否自爆,"target":自爆带走的玩家编号或null'
      : ''
    instruction = language === 'en-US'
      ? `Task: give a ${stageName} speech. First write private strategic analysis, then a 120-220 word public in-character speech. Analyze actual night results, prior speeches or votes, name concrete suspects and a voting intention. JSON schema: {"thinking":"private strategy","speech":"public speech"${specialSchema}}`
      : `任务：${stageName}。先写简洁的私密局势分析，再写180-350个中文字符的公开发言。必须结合真实夜间结果、此前发言或票型，至少点出一名具体怀疑对象或矛盾并说明投票倾向。JSON格式：{"thinking":"私密策略摘要","speech":"公开发言"${specialSchema}}`
  } else if (action === 'guard') {
    instruction = language === 'en-US'
      ? `Choose one target number to guard, or null to leave empty. You may guard yourself, but cannot guard the same target on consecutive nights. A simultaneous guard and antidote on the wolf target causes that player to die. Candidates: ${extra.candidates}. JSON: {"thinking":"private decision summary","target":number|null}`
      : `请选择一名守护目标，也可以空守。可自守，但不能连续两晚守同一人；狼刀目标若同时被守卫和女巫救，会因同守同救死亡。可选：${extra.candidates}。JSON：{"thinking":"私密决策摘要","target":玩家编号或null}`
  } else if (action === 'wolf') {
    instruction = language === 'en-US'
      ? `Independently submit one kill target without seeing teammates' votes. Candidates: ${extra.candidates}. JSON: {"thinking":"private wolf strategy summary","target":number}`
      : `请独立提交一个击杀目标，你看不到其他狼人的本轮投票。可按策略选择空刀（target为null）或允许规则内的狼王自刀。可选：${extra.candidates}。JSON：{"thinking":"私密狼队策略摘要","target":玩家编号或null}`
  } else if (action === 'witch') {
    instruction = language === 'en-US'
      ? `Wolf target is ${extra.wolfTarget}. Antidote remaining: ${extra.antidote}; poison remaining: ${extra.poison}. Decide whether to save and optionally poison one living player. You do not know the guard target. JSON: {"thinking":"private decision summary","useAntidote":boolean,"poisonTarget":number|null}`
      : `狼人刀口是${extra.wolfTarget}。剩余解药${extra.antidote}瓶、毒药${extra.poison}瓶。决定是否救人及是否毒一名存活玩家；你不知道守卫目标。JSON：{"thinking":"私密决策摘要","useAntidote":true或false,"poisonTarget":玩家编号或null}`
  } else if (action === 'seer') {
    instruction = language === 'en-US'
      ? `Choose one living player to inspect. Candidates: ${extra.candidates}. JSON: {"thinking":"private decision summary","target":number}`
      : `请选择一名存活玩家查验。可选：${extra.candidates}。JSON：{"thinking":"私密决策摘要","target":玩家编号}`
  } else if (action === 'vote') {
    instruction = language === 'en-US'
      ? `Privately vote to exile one candidate. You must choose and cannot abstain. Candidates: ${extra.candidates}. JSON: {"thinking":"private vote reasoning","target":number}`
      : `请私下投票放逐一名候选人，必须选择，不得弃票。候选：${extra.candidates}。JSON：{"thinking":"私密投票理由","target":玩家编号}`
  } else if (action === 'hunter') {
    instruction = language === 'en-US'
      ? `You died by ${extra.cause} and may fire your Hunter ability once. Choose exactly one living target based only on public evidence. Candidates: ${extra.candidates}. JSON: {"thinking":"private shooting rationale","target":number}`
      : `你因${extra.cause}出局，可以发动一次猎人技能。请只根据公开信息，从存活玩家中选择一名开枪带走。可选：${extra.candidates}。JSON：{"thinking":"私密开枪理由","target":玩家编号}`
  } else if (action === 'miracle') {
    instruction = language === 'en-US'
      ? `Use your once-per-game Merchant ability. Choose another player and grant check, poison, or guard. Candidates: ${extra.candidates}. JSON: {"thinking":"private plan","target":number,"skill":"check|poison|guard"}`
      : `发动每局一次的奇迹商人技能：选择另一名玩家成为幸运儿，并授予查验、毒药、守护之一。可选：${extra.candidates}。JSON：{"thinking":"私密授予策略","target":玩家编号,"skill":"check或poison或guard"}`
  } else if (action === 'grantedCheck' || action === 'gargoyle') {
    instruction = language === 'en-US'
      ? `Choose one player to inspect. ${action === 'gargoyle' ? 'You learn the exact role and may not repeat a target.' : 'You learn good or wolf.'} Candidates: ${extra.candidates}. JSON: {"thinking":"private check plan","target":number}`
      : `选择一名玩家查验。${action === 'gargoyle' ? '你会得知具体身份，不能验自己或重复验人。' : '你会得知好人或狼人。'}可选：${extra.candidates}。JSON：{"thinking":"私密查验策略","target":玩家编号}`
  } else if (action === 'grantedPoison' || action === 'grantedGuard' || action === 'wolfBeauty') {
    const ability = action === 'grantedPoison' ? '一次性毒药' : action === 'grantedGuard' ? '一次性守护' : '狼美人魅惑'
    instruction = `使用${ability}选择一名目标；狼美人不可连续两夜魅惑同一人。可选：${extra.candidates}。JSON：{"thinking":"私密技能策略","target":玩家编号}`
  } else if (action === 'sheriffCampaign') {
    instruction = language === 'en-US'
      ? `Decide whether to run for sheriff and choose clockwise or counterclockwise speaking order if elected. JSON: {"thinking":"private campaign plan","run":boolean,"direction":"clockwise|counterclockwise"}`
      : `决定是否上警，并选择当选后希望采用顺时针或逆时针发言。预言家通常应上警，狼人可悍跳。JSON：{"thinking":"私密竞选策略","run":true或false,"direction":"clockwise或counterclockwise"}`
  } else if (action === 'sheriffVote') {
    instruction = language === 'en-US'
      ? `Vote for one sheriff candidate. You may vote for yourself. Candidates: ${extra.candidates}. JSON: {"thinking":"private sheriff read","target":number}`
      : `请从候选人中投票选出警长，可以投自己。候选：${extra.candidates}。JSON：{"thinking":"私密警长判断","target":玩家编号}`
  } else if (action === 'sheriffTransfer') {
    instruction = `你是死亡警长，必须把警徽传给一名存活玩家，或撕掉警徽（target为null）。可选：${extra.candidates}。JSON：{"thinking":"私密警徽流判断","target":玩家编号或null}`
  } else if (action === 'knight') {
    instruction = `你可在放逐投票前发动每局一次的骑士决斗，也可保留技能。可选：${extra.candidates}。JSON：{"thinking":"私密决斗判断","use":true或false,"target":玩家编号或null}`
  } else if (action === 'wolfKing') {
    instruction = `你符合狼王开枪条件，可带走一名存活玩家。可选：${extra.candidates}。JSON：{"thinking":"私密开枪策略","target":玩家编号}`
  }
  return { language, systemPrompt, userPrompt: `${base}\n${instruction}` }
}

const requestPlayerDecision = async (player, action, extra = {}) => {
  await waitWhilePaused()
  const config = await getAiConfig(player)
  await waitWhilePaused()
  const prompts = buildDecisionPrompts(player, action, extra, config)
  aiThinkingPlayers.value = [...new Set([...aiThinkingPlayers.value, player.id])]
  try {
    const result = await callStructuredAi(config, prompts.systemPrompt, prompts.userPrompt)
    await waitWhilePaused()
    return { ...(result || {}), language: prompts.language }
  } finally {
    aiThinkingPlayers.value = aiThinkingPlayers.value.filter(id => id !== player.id)
  }
}

const resolvePlayerTarget = (value, candidates) => {
  if (value === null || value === undefined || value === '') return null
  const numeric = Number(value)
  return candidates.find(player => Number(player.playerNumber) === numeric)
    || candidates.find(player => Number(player.id) === numeric)
    || null
}

const recordPrivateThinking = (player, thinking, stage) => {
  if (!thinking) return
  addGameMessage({
    sender: `${player.playerNumber}号 ${player.playerName}`,
    content: `[${stage}] ${String(thinking).trim()}`,
    type: 'thinking',
    visibility: 'god'
  })
}

const runGrantedSkillAction = async (version, lucky, skill) => {
  if (!lucky?.isAlive || !isLoopActive(version)) return
  const candidates = alivePlayers.value.filter(player => player.id !== lucky.id)
  if (!candidates.length) return
  const action = skill === 'check' ? 'grantedCheck' : skill === 'poison' ? 'grantedPoison' : 'grantedGuard'
  const decision = await requestPlayerDecision(lucky, action, { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(lucky, decision.thinking, `幸运儿${skill}`)
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  if (!target) return
  nightState.miracleSkillTargetId = target.id
  if (skill === 'check') {
    const result = isWolfRole(target) ? '狼人' : '好人'
    playerMemories[lucky.id].privateKnowledge.push(`奇迹查验${target.playerNumber}号，结果为${result}`)
    addGameMessage({ sender: `${lucky.playerNumber}号幸运儿`, content: `一次性查验${target.playerNumber}号：${result}`, type: 'night-action', visibility: 'private', privateFor: lucky.id })
  } else if (skill === 'poison') {
    nightState.miraclePoisonTargetId = target.id
    addGameMessage({ sender: `${lucky.playerNumber}号幸运儿`, content: `对${target.playerNumber}号使用一次性毒药`, type: 'night-action', visibility: 'private', privateFor: lucky.id })
  } else {
    nightState.miracleGuardTargetId = target.id
    addGameMessage({ sender: `${lucky.playerNumber}号幸运儿`, content: `一次性守护${target.playerNumber}号`, type: 'night-action', visibility: 'private', privateFor: lucky.id })
  }
  await phaseDelay()
}

const runMiracleMerchantAction = async version => {
  currentPhase.value = 'night_miracle'
  if (!isBoard('miracle_merchant') || miracleMerchantState.used) return
  const merchant = alivePlayers.value.find(player => hasRole(player, 'miracle'))
  if (!merchant || !isLoopActive(version)) return
  addRefereeMessage('奇迹商人最先睁眼，选择幸运儿和一项一次性技能。', { visibility: 'god', detail: '每局限一次；幸运儿若为狼人则不获技能，奇迹商人次日出局。' })
  const candidates = alivePlayers.value.filter(player => player.id !== merchant.id)
  const decision = await requestPlayerDecision(merchant, 'miracle', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(merchant, decision.thinking, '奇迹商人授予')
  const lucky = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  const skill = ['check', 'poison', 'guard'].includes(decision.skill) ? decision.skill : randomItem(['check', 'poison', 'guard'])
  if (!lucky) return
  Object.assign(miracleMerchantState, { used: true, merchantId: merchant.id, luckyId: lucky.id, skill, pendingDeath: isWolfRole(lucky) })
  addGameMessage({ sender: `${merchant.playerNumber}号奇迹商人`, content: `选择${lucky.playerNumber}号为幸运儿，授予${{ check: '查验', poison: '毒药', guard: '守护' }[skill]}`, type: 'night-action', visibility: 'private', privateFor: merchant.id })
  if (isWolfRole(lucky)) {
    addRefereeMessage('幸运儿属于狼人阵营，技能授予失败，奇迹商人将在天亮时出局。', { visibility: 'god' })
    return
  }
  playerMemories[lucky.id].privateKnowledge.push(`奇迹商人授予你一次性${{ check: '查验', poison: '毒药', guard: '守护' }[skill]}技能，须在本夜使用`)
  await runGrantedSkillAction(version, lucky, skill)
}

const runWolfBeautyAction = async version => {
  currentPhase.value = 'night_wolf_beauty'
  if (!isBoard('wolf_beauty_knight')) return
  const beauty = alivePlayers.value.find(player => hasRole(player, 'wolfBeauty'))
  if (!beauty || !isLoopActive(version)) return
  const candidates = alivePlayers.value.filter(player => player.id !== beauty.id && player.id !== wolfBeautyState.targetId)
  if (!candidates.length) return
  addRefereeMessage('狼美人单独选择今夜魅惑目标。', { visibility: 'god', detail: '不能连续两夜魅惑同一人；被骑士决斗出局不触发殉情。' })
  const decision = await requestPlayerDecision(beauty, 'wolfBeauty', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(beauty, decision.thinking, '狼美人魅惑')
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  wolfBeautyState.previousTargetId = wolfBeautyState.targetId
  wolfBeautyState.targetId = target?.id || null
  nightState.wolfBeautyTargetId = target?.id || null
  if (target) addGameMessage({ sender: `${beauty.playerNumber}号狼美人`, content: `魅惑${target.playerNumber}号${target.playerName}`, type: 'night-action', visibility: 'private', privateFor: beauty.id })
  await phaseDelay()
}

const runGargoyleAction = async version => {
  currentPhase.value = 'night_gargoyle'
  if (!isBoard('gargoyle_gravedigger')) return
  const gargoyle = alivePlayers.value.find(player => hasRole(player, 'gargoyle'))
  if (!gargoyle || !isLoopActive(version)) return
  const checkedIds = (playerMemories[gargoyle.id]?.checks || []).map(check => check.targetId)
  const candidates = alivePlayers.value.filter(player => player.id !== gargoyle.id && !checkedIds.includes(player.id))
  if (!candidates.length) return
  addRefereeMessage('石像鬼睁眼查验一名未查验玩家的具体身份。', { visibility: 'god' })
  const decision = await requestPlayerDecision(gargoyle, 'gargoyle', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(gargoyle, decision.thinking, '石像鬼查验')
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  if (!target) return
  nightState.gargoyleTargetId = target.id
  nightState.gargoyleResult = target.role
  playerMemories[gargoyle.id].checks.push({ day: currentDay.value, targetId: target.id, result: target.role })
  playerMemories[gargoyle.id].privateKnowledge.push(`第${currentRound.value}夜查验${target.playerNumber}号，具体身份为${target.role}`)
  addGameMessage({ sender: `${gargoyle.playerNumber}号石像鬼`, content: `查验${target.playerNumber}号：${target.role}`, type: 'night-action', visibility: 'private', privateFor: gargoyle.id })
  await phaseDelay()
}

const runGravediggerAction = async () => {
  currentPhase.value = 'night_gravedigger'
  if (!isBoard('gargoyle_gravedigger') || currentRound.value < 2) return
  const gravedigger = alivePlayers.value.find(player => hasRole(player, 'gravedigger'))
  const exiled = players.value.find(player => player.id === lastExiledPlayerId.value)
  if (!gravedigger || !exiled) return
  const result = isWolfRole(exiled) ? '狼人' : '好人'
  nightState.gravediggerResult = result
  playerMemories[gravedigger.id].privateKnowledge.push(`上一白天被放逐的${exiled.playerNumber}号属于${result}阵营`)
  addGameMessage({ sender: `${gravedigger.playerNumber}号守墓人`, content: `上个白天被放逐的${exiled.playerNumber}号是${result}`, type: 'night-action', visibility: 'private', privateFor: gravedigger.id })
  addRefereeMessage('守墓人强制获知了上一名放逐者的阵营。', { visibility: 'god', detail: `${exiled.playerNumber}号：${result}` })
}

const runGuardAction = async (version) => {
  currentPhase.value = 'night_guard'
  addRefereeMessage('守卫请睁眼并选择守护目标。', { visibility: 'god', detail: '可自守、可空守、不可连续两晚守同一人；同守同救判定死亡。' })
  const guard = alivePlayers.value.find(player => hasRole(player, 'guard'))
  if (!guard || !isLoopActive(version)) {
    addRefereeMessage('本局无存活守卫，跳过守卫行动。', { visibility: 'god' })
    return
  }
  const candidates = alivePlayers.value.filter(player => !gameRules.guardCannotRepeatTarget || player.id !== lastGuardTargetId.value)
  const decision = await requestPlayerDecision(guard, 'guard', { candidates: candidates.map(p => `${p.playerNumber}号`).join('、') })
  recordPrivateThinking(guard, decision.thinking, '守卫行动')
  const fallback = Math.random() < 0.12 ? null : randomItem(candidates)
  const target = resolvePlayerTarget(decision.target, candidates) || fallback
  nightState.guardTargetId = target?.id || null
  lastGuardTargetId.value = nightState.guardTargetId
  addGameMessage({ sender: `${guard.playerNumber}号守卫`, content: target ? `守护${target.playerNumber}号${target.playerName}` : '选择空守', type: 'night-action', visibility: 'private', privateFor: guard.id })
  await phaseDelay()
}

const runWolfActions = async (version) => {
  currentPhase.value = 'night_wolf'
  addRefereeMessage('狼人请睁眼。每名狼人独立提交刀口，由上帝按多数票结算。', { visibility: 'god' })
  const packWolves = alivePlayers.value.filter(isPackWolf)
  const gargoyle = alivePlayers.value.find(player => hasRole(player, 'gargoyle'))
  const wolves = packWolves.length ? packWolves : (gargoyle ? [gargoyle] : [])
  const candidates = alivePlayers.value.filter(player => !isWolfRole(player) || (packWolves.length && hasRole(player, 'wolfKing')))
  if (!packWolves.length && gargoyle) {
    addRefereeMessage('其他狼人已全部出局，石像鬼本夜获得单独袭击能力。', { visibility: 'god' })
  }
  for (const wolf of wolves) {
    if (!isLoopActive(version) || !candidates.length) break
    const decision = await requestPlayerDecision(wolf, 'wolf', { candidates: candidates.map(p => `${p.playerNumber}号`).join('、') })
    recordPrivateThinking(wolf, decision.thinking, '狼人刀口')
    const deliberatelyEmpty = Object.prototype.hasOwnProperty.call(decision, 'target') && decision.target === null
    const target = deliberatelyEmpty ? null : (resolvePlayerTarget(decision.target, candidates) || randomItem(candidates))
    if (!target) continue
    nightState.wolfVotes.push({ voterId: wolf.id, targetId: target.id })
    addGameMessage({ sender: `${wolf.playerNumber}号狼人`, content: `提交击杀${target.playerNumber}号${target.playerName}`, type: 'night-action', visibility: 'private', privateFor: wolf.id })
  }
  const counts = new Map()
  nightState.wolfVotes.forEach(vote => counts.set(vote.targetId, (counts.get(vote.targetId) || 0) + 1))
  const maxVotes = Math.max(0, ...counts.values())
  const tiedIds = [...counts.entries()].filter(([, count]) => count === maxVotes).map(([id]) => id)
  nightState.wolfTieCandidates = tiedIds
  nightState.wolfTargetId = randomItem(tiedIds)
  const target = players.value.find(player => player.id === nightState.wolfTargetId)
  const ballot = nightState.wolfVotes.map(vote => `${getPlayerNumberById(vote.voterId)}号→${getPlayerNumberById(vote.targetId)}号`).join('，') || '无有效投票'
  addRefereeMessage(target ? `狼人最终刀口：${target.playerNumber}号${target.playerName}` : '狼人没有形成有效刀口。', {
    visibility: 'god',
    detail: tiedIds.length > 1 ? `投票：${ballot}。最高票平票，已由代码随机裁决。` : `投票：${ballot}。`
  })
  await phaseDelay()
}

const runWitchAction = async (version) => {
  currentPhase.value = 'night_witch'
  const witch = alivePlayers.value.find(player => hasRole(player, 'witch'))
  if (!witch || !isLoopActive(version)) {
    addRefereeMessage('本局无存活女巫，跳过女巫行动。', { visibility: 'god' })
    return
  }
  const wolfTarget = players.value.find(player => player.id === nightState.wolfTargetId)
  addRefereeMessage('女巫请睁眼并决定是否使用药剂。', { visibility: 'god', detail: `解药${witchInventory.antidote}瓶，毒药${witchInventory.poison}瓶；每夜最多使用一瓶，12人局不可自救，同守同救判定死亡。` })
  const poisonCandidates = alivePlayers.value.filter(player => player.id !== witch.id)
  const decision = await requestPlayerDecision(witch, 'witch', {
    wolfTarget: wolfTarget ? `${wolfTarget.playerNumber}号${wolfTarget.playerName}` : '无有效刀口',
    antidote: witchInventory.antidote,
    poison: witchInventory.poison
  })
  recordPrivateThinking(witch, decision.thinking, '女巫行动')
  const maySelfSave = wolfTarget?.id !== witch.id
    || (boardRules.value.witchCanSelfSave === 'first-night' && currentRound.value === 1)
  nightState.witchSaved = Boolean(wolfTarget && witchInventory.antidote > 0 && maySelfSave && (decision.useAntidote ?? (currentRound.value === 1 && Math.random() < 0.6)))
  let poisonTarget = resolvePlayerTarget(decision.poisonTarget, poisonCandidates)
  if (!decision.poisonTarget && Math.random() >= 0.12) poisonTarget = null
  if (witchInventory.poison <= 0) poisonTarget = null
  if (nightState.witchSaved) poisonTarget = null
  nightState.witchPoisonTargetId = poisonTarget?.id || null
  if (nightState.witchSaved) witchInventory.antidote--
  if (nightState.witchPoisonTargetId) witchInventory.poison--
  const actions = [nightState.witchSaved ? `对${wolfTarget.playerNumber}号使用解药` : '未使用解药']
  actions.push(poisonTarget ? `对${poisonTarget.playerNumber}号使用毒药` : '未使用毒药')
  addGameMessage({ sender: `${witch.playerNumber}号女巫`, content: actions.join('；'), type: 'night-action', visibility: 'private', privateFor: witch.id })
  await phaseDelay()
}

const runSeerAction = async (version) => {
  currentPhase.value = 'night_seer'
  const seer = alivePlayers.value.find(player => hasRole(player, 'seer'))
  if (!seer || !isLoopActive(version)) {
    addRefereeMessage('本局无存活预言家，跳过查验。', { visibility: 'god' })
    return
  }
  const checkedIds = (playerMemories[seer.id]?.checks || []).map(check => check.targetId)
  let candidates = alivePlayers.value.filter(player => player.id !== seer.id && !checkedIds.includes(player.id))
  if (!candidates.length) candidates = alivePlayers.value.filter(player => player.id !== seer.id)
  addRefereeMessage('预言家请睁眼并选择查验目标。', { visibility: 'god' })
  const decision = await requestPlayerDecision(seer, 'seer', { candidates: candidates.map(p => `${p.playerNumber}号`).join('、') })
  recordPrivateThinking(seer, decision.thinking, '预言家查验')
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  if (!target) return
  const result = wolfRolesArr.includes(target.role) ? '狼人' : '好人'
  nightState.seerTargetId = target.id
  nightState.seerResult = result
  playerMemories[seer.id].checks.push({ day: currentDay.value, targetId: target.id, result })
  playerMemories[seer.id].privateKnowledge.push(`第${currentRound.value}夜查验${target.playerNumber}号，结果为${result}`)
  addGameMessage({ sender: `${seer.playerNumber}号预言家`, content: `查验${target.playerNumber}号${target.playerName}：${result}`, type: 'night-action', visibility: 'private', privateFor: seer.id })
  await phaseDelay()
}

const resolveNight = () => {
  const deaths = new Set()
  const wolfTarget = players.value.find(player => player.id === nightState.wolfTargetId)
  const guarded = Boolean(wolfTarget && (nightState.guardTargetId === wolfTarget.id || nightState.miracleGuardTargetId === wolfTarget.id))
  const saved = Boolean(wolfTarget && nightState.witchSaved)
  let mainReason = '狼人没有形成有效刀口'
  if (wolfTarget) {
    if (guarded && saved && gameRules.sameGuardAndSaveKills) {
      deaths.add(wolfTarget.id)
      mainReason = `${wolfTarget.playerNumber}号同时被守卫守护和女巫解救，按“同守同救”规则仍然死亡`
    } else if (guarded) {
      mainReason = `${wolfTarget.playerNumber}号的狼刀被守卫成功挡下`
    } else if (saved) {
      mainReason = `${wolfTarget.playerNumber}号被女巫使用解药救下`
    } else {
      deaths.add(wolfTarget.id)
      mainReason = `${wolfTarget.playerNumber}号遭狼人击杀且未获得有效保护`
    }
  }
  const poisoned = players.value.filter(player => [nightState.witchPoisonTargetId, nightState.miraclePoisonTargetId].includes(player.id))
  poisoned.forEach(player => deaths.add(player.id))
  if (miracleMerchantState.pendingDeath && miracleMerchantState.merchantId) deaths.add(miracleMerchantState.merchantId)
  nightState.deaths = [...deaths]
  const poisonDetail = poisoned.length ? `；毒药带走${poisoned.map(player => `${player.playerNumber}号`).join('、')}` : ''
  const merchantDetail = miracleMerchantState.pendingDeath ? '；幸运儿为狼人，奇迹商人遭受反噬出局' : ''
  nightState.explanation = `${mainReason}${poisonDetail}${merchantDetail}`
  if (!nightState.deaths.length) nightState.explanation = `平安夜：${mainReason}`
  addRefereeMessage('夜间行动结算完成。', { visibility: 'god', detail: nightState.explanation })
}

const runNightPhase = async (version) => {
  currentPhase.value = 'night'
  resetNightState()
  const actionLabels = { miracle: '奇迹商人', gravedigger: '守墓人', gargoyle: '石像鬼', guard: '守卫', wolves: '狼人', wolfBeauty: '狼美人', witch: '女巫', seer: '预言家' }
  addRefereeMessage(currentLocale() === 'zh-CN'
    ? `第${currentRound.value}夜开始，天黑请闭眼。本板子行动顺序：${boardRules.value.nightOrder.map(action => actionLabels[action]).join('→')}。`
    : `Night ${currentRound.value} begins. Board action order: ${boardRules.value.nightOrder.join(' -> ')}.`)
  const actions = {
    miracle: () => runMiracleMerchantAction(version),
    gravedigger: () => runGravediggerAction(version),
    gargoyle: () => runGargoyleAction(version),
    guard: () => runGuardAction(version),
    wolves: () => runWolfActions(version),
    wolfBeauty: () => runWolfBeautyAction(version),
    witch: () => runWitchAction(version),
    seer: () => runSeerAction(version)
  }
  for (const action of boardRules.value.nightOrder) {
    if (!isLoopActive(version)) break
    await actions[action]?.()
  }
  if (isLoopActive(version)) resolveNight()
}

const resolveHunterSkill = async (hunter, version, cause) => {
  if (!hunter || !hasRole(hunter, 'hunter') || hunterSkillUsed.has(hunter.id) || !isLoopActive(version)) return
  hunterSkillUsed.add(hunter.id)
  if (cause === 'poison') {
    addRefereeMessage(`${hunter.playerNumber}号${hunter.playerName}是猎人，但因被女巫毒杀不能发动技能。`)
    return
  }

  const candidates = alivePlayers.value.filter(player => player.id !== hunter.id)
  if (!candidates.length) {
    addRefereeMessage(`${hunter.playerNumber}号${hunter.playerName}是猎人，但场上已没有可开枪的目标。`)
    return
  }

  addRefereeMessage(`${hunter.playerNumber}号${hunter.playerName}确认猎人身份，正在选择开枪目标。`)
  const decision = await requestPlayerDecision(hunter, 'hunter', {
    cause: cause === 'exile' ? '公投放逐' : cause === 'hunter' ? '猎人枪击' : '夜间死亡',
    candidates: candidates.map(player => `${player.playerNumber}号`).join('、')
  })
  if (!isLoopActive(version)) return
  recordPrivateThinking(hunter, decision.thinking, '猎人开枪')
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  if (!target) return

  killPlayer(target.id)
  addRefereeMessage(`${hunter.playerNumber}号猎人发动技能，开枪带走${target.playerNumber}号${target.playerName}。`)
  await phaseDelay(500)
  await resolveDeathEffects(target, version, 'hunter')
}

const transferSheriffBadge = async (deadSheriff, version, consumed = false) => {
  if (!deadSheriff?.isSheriff) return
  deadSheriff.isSheriff = false
  if (consumed) {
    addRefereeMessage('警徽被狼人自爆吞掉，本局警徽作废。')
    return
  }
  const candidates = alivePlayers.value.filter(player => player.id !== deadSheriff.id)
  if (!candidates.length || !isLoopActive(version)) {
    addRefereeMessage('警长出局且无可传递目标，警徽作废。')
    return
  }
  const decision = await requestPlayerDecision(deadSheriff, 'sheriffTransfer', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(deadSheriff, decision.thinking, '警徽流')
  const target = resolvePlayerTarget(decision.target, candidates)
  if (!target) {
    addRefereeMessage(`${deadSheriff.playerNumber}号警长选择撕掉警徽。`)
    return
  }
  setSheriff(target.id)
  addRefereeMessage(`${deadSheriff.playerNumber}号将警徽传给${target.playerNumber}号${target.playerName}。`)
}

const resolveWolfKingSkill = async (wolfKing, version, cause) => {
  if (!wolfKing || !hasRole(wolfKing, 'wolfKing') || wolfKingSkillUsed.has(wolfKing.id) || !isLoopActive(version)) return
  const allowed = ['exile', 'hunter', 'night'].includes(cause)
  if (!allowed) {
    addRefereeMessage('狼王本次死因不满足开枪条件，技能未发动。', { visibility: 'god', detail: `死因：${cause}` })
    return
  }
  wolfKingSkillUsed.add(wolfKing.id)
  const candidates = alivePlayers.value.filter(player => player.id !== wolfKing.id)
  if (!candidates.length) return
  const decision = await requestPlayerDecision(wolfKing, 'wolfKing', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(wolfKing, decision.thinking, '狼王开枪')
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  if (!target) return
  killPlayer(target.id)
  addRefereeMessage(`${wolfKing.playerNumber}号狼王发动技能，开枪带走${target.playerNumber}号${target.playerName}。`)
  await phaseDelay(500)
  await resolveDeathEffects(target, version, 'wolfKing')
}

const resolveWolfBeautyLink = async (beauty, version, cause) => {
  if (!beauty || !hasRole(beauty, 'wolfBeauty') || cause === 'knight' || !wolfBeautyState.targetId || !isLoopActive(version)) return
  const linked = players.value.find(player => player.id === wolfBeautyState.targetId && player.isAlive)
  wolfBeautyState.targetId = null
  if (!linked) return
  killPlayer(linked.id)
  addRefereeMessage(`${linked.playerNumber}号${linked.playerName}因狼美人魅惑而殉情出局。`)
  await resolveDeathEffects(linked, version, 'romance')
}

const resolveDeathEffects = async (player, version, cause, options = {}) => {
  if (!player || !isLoopActive(version)) return
  await resolveHunterSkill(player, version, cause)
  await resolveWolfKingSkill(player, version, cause)
  await resolveWolfBeautyLink(player, version, cause)
  await transferSheriffBadge(player, version, options.consumeSheriff)
}

const announceDay = async (version) => {
  currentPhase.value = 'day'
  addRefereeMessage(currentLocale() === 'zh-CN' ? `天亮了，现在是第${currentDay.value}天。` : `Dawn breaks. Day ${currentDay.value} begins.`)
  nightState.deaths.forEach(killPlayer)
  if (!nightState.deaths.length) {
    lastPublicNightReport.value = currentLocale() === 'zh-CN' ? '昨夜是平安夜，没有人死亡。' : 'It was a peaceful night. No one died.'
  } else {
    const numbers = nightState.deaths.map(id => `${getPlayerNumberById(id)}号`).join('、')
    lastPublicNightReport.value = currentLocale() === 'zh-CN'
      ? `昨夜${numbers}玩家死亡，死亡不分先后。`
      : `Last night, player(s) ${numbers} died. Deaths are unordered.`
  }
  addRefereeMessage(lastPublicNightReport.value, { detail: nightState.explanation })
  if (nightState.deaths.length && currentRound.value === 1 && Number(boardRules.value.players) === 12 && !lastWordsGiven.value) {
    const firstNightDead = players.value.find(player => player.id === nightState.deaths[0])
    await giveLastWords(firstNightDead, version, 'night')
  }
  for (const playerId of nightState.deaths) {
    const player = players.value.find(candidate => candidate.id === playerId)
    const cause = [nightState.witchPoisonTargetId, nightState.miraclePoisonTargetId].includes(playerId) ? 'poison' : 'night'
    await resolveDeathEffects(player, version, cause)
  }
  addRefereeMessage(currentLocale() === 'zh-CN' ? `当前局势简报：${publicSituationSummary()}` : `Situation briefing: ${publicSituationSummary()}`)
  await phaseDelay(800)
}

const contextualFallbackSpeech = (player, language, action = 'speech') => {
  const others = alivePlayers.value.filter(p => p.id !== player.id)
  const recentSpeakers = dialogMessages.value.filter(m => m.type === 'player' && (m.visibility || 'public') === 'public').slice(-3)
  const suspect = recentSpeakers.length
    ? players.value.find(p => p.playerName === recentSpeakers[0].sender && p.isAlive && p.id !== player.id) || randomItem(others)
    : randomItem(others)
  const second = others.find(p => p.id !== suspect?.id) || suspect
  const checkedWolf = (playerMemories[player.id]?.checks || []).find(check => check.result === '狼人')
  const latestVote = voteHistory.value[voteHistory.value.length - 1]
  const votesAgainstPlayer = latestVote?.ballots?.filter(ballot => ballot.targetId === player.id).map(ballot => getPlayerNumberById(ballot.voterId)) || []
  if (action === 'lastWords') {
    if (language === 'en-US') {
      const voteReview = votesAgainstPlayer.length
        ? `Players ${votesAgainstPlayer.join(', ')} formed the votes that sent me out.`
        : 'The final vote pushed me out without a stable public case.'
      return `I have been exiled, so this is my final assessment rather than another campaign speech. ${voteReview} The surviving good players should review whether those votes followed concrete contradictions or merely gathered after the room found an easy target. My strongest warning is about player ${suspect?.playerNumber || '?'}, whose position has not been supported by a consistent chain of reasons. Compare that slot directly with player ${second?.playerNumber || '?'} and check who changed their read only after the likely result became clear. ${checkedWolf ? `The most important information I leave behind is that player ${getPlayerNumberById(checkedWolf.targetId)} should be treated as the priority wolf suspect.` : 'Do not clear anyone for sounding confident; use the public vote record and contradictions.'} That is the conclusion I leave with the table.`
    }
    const voteReview = votesAgainstPlayer.length
      ? `刚才${votesAgainstPlayer.join('号、')}号的票共同把我推出了局。`
      : '刚才的票型并没有形成一条稳定、公开的放逐理由。'
    return `我已经被放逐出局，这段遗言只做最后复盘。${voteReview}存活的好人要重新核对这些票究竟来自具体矛盾，还是在场上出现容易推动的目标后集中跟票。我最后最想提醒的是${suspect?.playerNumber || '?'}号：这个位置给出的怀疑链条并不完整，需要和${second?.playerNumber || '?'}号的站边、改票时机放在一起检查，看谁是在结果逐渐明确后才临时调整口径。${checkedWolf ? `我留下的最重要信息是${getPlayerNumberById(checkedWolf.targetId)}号应当作为狼人重点处理。` : '不要因为语气强势就轻易认好，公开票型和前后矛盾才是能留下来的证据。'}这是我留给场上的最终判断。`
  }
  if (language === 'en-US') {
    const englishNightReport = nightState.deaths.length
      ? `Last night, players ${nightState.deaths.map(getPlayerNumberById).join(', ')} died.`
      : 'Last night was peaceful and no one died.'
    return `I will base this on the board rather than make a vague pass. ${englishNightReport} The current survivor list and the order of prior speeches matter because later speakers can adapt their stories. My first focus is player ${suspect?.playerNumber || '?'}. Their position has not yet been tied to a clear reason, and I want them to explain whom they suspect, what specific statement created that suspicion, and where their vote is going. I am also comparing that answer with player ${second?.playerNumber || '?'} so that we can see whether either one is simply following the room. ${checkedWolf ? `I have strong game information against player ${getPlayerNumberById(checkedWolf.targetId)}, so that slot is my priority today.` : 'For now I will not treat confidence as evidence; contradictions, voting movement, and attempts to avoid naming a target are more useful.'} My current voting preference is player ${checkedWolf ? getPlayerNumberById(checkedWolf.targetId) : suspect?.playerNumber || '?'}, but I will update it if the remaining speeches provide a stronger inconsistency.`
  }
  return `我先基于场上的真实信息发言，不做一句话划水。${lastPublicNightReport.value || '目前刚进入白天讨论。'}现在需要把夜间结果、发言顺序和每个人给出的理由放在一起看，因为越靠后的玩家越容易顺着前面的结论调整说法。我目前第一关注${suspect?.playerNumber || '?'}号：这个位置需要明确说明怀疑谁、依据是哪一句发言或哪一个行为，以及最终准备投给谁，不能只说“局势复杂”。同时我会对照${second?.playerNumber || '?'}号的站边和票型，看两者是否存在互相抬身份或机械跟票。${checkedWolf ? `我掌握到${getPlayerNumberById(checkedWolf.targetId)}号存在明确的狼人信息，因此今天优先处理这个位置。` : '在没有硬信息前，我不会把语气强势当成好人证据，更重视前后矛盾、回避点人和临时改变票型。'}现阶段我的投票倾向是${checkedWolf ? getPlayerNumberById(checkedWolf.targetId) : suspect?.playerNumber || '?'}号，但会继续听完后置位，再根据新的矛盾调整。`
}

const sanitizeSpeech = (speech, player, language, action = 'speech') => {
  const cleaned = String(speech || '').replace(/```[\s\S]*?```/g, '').trim()
  const hasMeta = /(作为AI|我是AI|系统提示|语言模型|token|as an ai|system prompt|language model)/i.test(cleaned)
  const invalidLastWords = action === 'lastWords' && /(继续听|听完后|等后置位|后面发言|之后再投|准备投给|投票倾向|下一轮|明天我|再调整判断|我(?:还|会|将).*?(?:听|投|发言|观察|调整)|i will (?:keep listening|listen|vote|update)|i(?:'ll| am going to).*?(?:listen|vote|speak|update)|my (?:current )?voting preference|next round|later speeches)/i.test(cleaned)
  const minimum = action === 'lastWords'
    ? (language === 'en-US' ? 420 : 150)
    : (language === 'en-US' ? 580 : 180)
  if (!cleaned || hasMeta || invalidLastWords) return contextualFallbackSpeech(player, language, action)
  if (cleaned.length < minimum) return `${cleaned}\n${contextualFallbackSpeech(player, language, action)}`
  return cleaned
}

const presentSpeech = (player, speech, thinking, language) => new Promise(resolve => {
  if (activeSpeechResolver) closeSpeech()
  aiSpeakingContent.value = { playerId: player.id, playerName: `${player.playerNumber}号 ${player.playerName}`, content: speech, thinking }
  startTypewriter(speech, { speed: SPEEDS[typewriterSpeed.value] || 50 })
  speakText(speech, { lang: language })
  activeSpeechResolver = resolve
  startSpeechTimer()
})

const generatePublicSpeech = async (player, action = 'speech') => {
  const decision = await requestPlayerDecision(player, action)
  const speech = sanitizeSpeech(decision.speech, player, decision.language, action)
  const thinking = String(decision.thinking || (decision.language === 'en-US'
    ? `I need to connect the public night result with concrete speech and vote evidence while protecting my role.`
    : `需要把公开夜间结果与具体发言、票型联系起来，同时根据身份规划是否暴露信息。`)).trim()
  return {
    speech,
    thinking,
    language: decision.language,
    explode: Boolean(decision.explode),
    target: decision.target,
    useKnight: Boolean(decision.use)
  }
}

const createRandomSpeechOrder = () => {
  const ordered = [...alivePlayers.value].sort((a, b) => a.playerNumber - b.playerNumber)
  if (!ordered.length) return { ids: [], direction: 'clockwise', start: null }
  const startIndex = Math.floor(Math.random() * ordered.length)
  const sheriff = alivePlayers.value.find(player => player.isSheriff)
  const clockwise = sheriff ? sheriffDirection.value === 'clockwise' : Math.random() < 0.5
  const ids = []
  for (let offset = 0; offset < ordered.length; offset++) {
    const index = clockwise
      ? (startIndex + offset) % ordered.length
      : (startIndex - offset + ordered.length) % ordered.length
    ids.push(ordered[index].id)
  }
  return { ids, direction: clockwise ? 'clockwise' : 'counterclockwise', start: ordered[startIndex] }
}

const runSheriffElection = async version => {
  if (!boardRules.value.sheriff || sheriffElectionDone.value || !isLoopActive(version)) return
  currentPhase.value = 'sheriff'
  addRefereeMessage('第一天天亮后进入警长竞选。所有存活玩家私下决定是否上警，竞选发言结束后公开投票。')
  const campaignPlans = new Map()
  const candidates = []
  for (const player of [...alivePlayers.value]) {
    const decision = await requestPlayerDecision(player, 'sheriffCampaign')
    recordPrivateThinking(player, decision.thinking, '警长竞选')
    const run = decision.run === undefined ? Math.random() < 0.55 : Boolean(decision.run)
    if (run) {
      candidates.push(player)
      campaignPlans.set(player.id, decision.direction === 'counterclockwise' ? 'counterclockwise' : 'clockwise')
    }
  }
  const finalCandidates = candidates.length ? candidates : [randomItem(alivePlayers.value)]
  addRefereeMessage(`本次上警玩家：${finalCandidates.map(player => `${player.playerNumber}号`).join('、')}。`)
  await runSpeechPhase(version, finalCandidates.map(player => player.id), 'sheriffSpeech')
  if (!isLoopActive(version)) return
  const counts = new Map(finalCandidates.map(player => [player.id, 0]))
  for (const voter of [...alivePlayers.value]) {
    const decision = await requestPlayerDecision(voter, 'sheriffVote', { candidates: finalCandidates.map(player => `${player.playerNumber}号`).join('、') })
    recordPrivateThinking(voter, decision.thinking, '警长投票')
    const target = resolvePlayerTarget(decision.target, finalCandidates) || randomItem(finalCandidates)
    if (target) counts.set(target.id, (counts.get(target.id) || 0) + 1)
  }
  const max = Math.max(...counts.values())
  const leaders = finalCandidates.filter(player => counts.get(player.id) === max)
  const winner = randomItem(leaders)
  if (winner) {
    setSheriff(winner.id)
    sheriffDirection.value = campaignPlans.get(winner.id) || 'clockwise'
    axios.put('/game/player/setSheriff', { roomId, playerId: winner.id }).catch(() => {})
    addRefereeMessage(`${winner.playerNumber}号${winner.playerName}当选警长，之后由其决定${sheriffDirection.value === 'clockwise' ? '顺时针' : '逆时针'}发言。${leaders.length > 1 ? '平票结果由代码随机裁决。' : ''}`)
  }
  sheriffElectionDone.value = true
}

const runSpeechPhase = async (version, onlyCandidates = null, action = 'speech') => {
  currentPhase.value = 'speak'
  let orderInfo
  if (onlyCandidates) {
    orderInfo = { ids: onlyCandidates.filter(id => players.value.find(p => p.id === id)?.isAlive), direction: 'pk', start: null }
  } else {
    orderInfo = createRandomSpeechOrder()
    speechOrder.value = orderInfo.ids
    const directionText = orderInfo.direction === 'clockwise' ? '顺时针' : '逆时针'
    addRefereeMessage(`进入发言阶段。本轮由代码随机决定从${orderInfo.start?.playerNumber}号开始，按${directionText}发言。顺序：${orderInfo.ids.map(getPlayerNumberById).join('→')}号。`)
  }
  for (let index = 0; index < orderInfo.ids.length; index++) {
    if (!isLoopActive(version)) break
    speechIndex.value = index
    const player = players.value.find(p => p.id === orderInfo.ids[index] && p.isAlive)
    if (!player) continue
    startPlayerSpeaking(player.id)
    const result = await generatePublicSpeech(player, action)
    if (!isLoopActive(version)) break
    if (action === 'speech' && hasRole(player, 'whiteWolf') && isBoard('white_wolf_knight') && result.explode) {
      closeSpeech('pass')
      killPlayer(player.id)
      dayInterrupted.value = true
      addRefereeMessage(`${player.playerNumber}号白狼王在白天发言阶段自爆，立即终止发言并进入黑夜。`)
      const candidates = alivePlayers.value.filter(candidate => candidate.id !== player.id)
      const target = resolvePlayerTarget(result.target, candidates) || randomItem(candidates)
      if (target) {
        killPlayer(target.id)
        addRefereeMessage(`白狼王自爆带走${target.playerNumber}号${target.playerName}。`)
        await resolveDeathEffects(target, version, 'whiteWolf')
      }
      await resolveDeathEffects(player, version, 'whiteWolf', { consumeSheriff: true })
      endPlayerSpeaking()
      break
    }
    recordPrivateThinking(player, result.thinking, action === 'pkSpeech' ? 'PK发言' : '白天发言')
    addDialogMessage(player.playerName, result.speech, 'player')
    playerMemories[player.id]?.speeches.push({ day: currentDay.value, speech: result.speech })
    await presentSpeech(player, result.speech, result.thinking, result.language)
    endPlayerSpeaking()
    await phaseDelay(260)
  }
  speechIndex.value = -1
  if (!onlyCandidates && isLoopActive(version)) addRefereeMessage('所有存活玩家发言完毕，主持人现在进入放逐投票。')
}

const runKnightAction = async version => {
  if (!isBoard('wolf_beauty_knight') && !isBoard('white_wolf_knight')) return
  const knight = alivePlayers.value.find(player => hasRole(player, 'knight'))
  if (!knight || knightSkillUsed.has(knight.id) || !isLoopActive(version)) return
  const candidates = alivePlayers.value.filter(player => player.id !== knight.id)
  addRefereeMessage('警长竞选结束，骑士可在放逐投票前翻牌决斗，也可以保留技能。')
  const decision = await requestPlayerDecision(knight, 'knight', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(knight, decision.thinking, '骑士决斗')
  if (!decision.use) {
    addGameMessage({ sender: `${knight.playerNumber}号骑士`, content: '本轮保留决斗技能', type: 'night-action', visibility: 'private', privateFor: knight.id })
    return
  }
  knightSkillUsed.add(knight.id)
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  if (!target) return
  killPlayer(knight.id)
  addRefereeMessage(`${knight.playerNumber}号骑士翻牌决斗${target.playerNumber}号。`)
  if (isWolfRole(target)) {
    killPlayer(target.id)
    dayInterrupted.value = true
    addRefereeMessage(`${target.playerNumber}号是狼人阵营，骑士决斗成功，立即进入黑夜。`)
    await resolveDeathEffects(target, version, 'knight')
  } else {
    addRefereeMessage(`${target.playerNumber}号是好人，骑士决斗失败，骑士出局，白天继续。`)
  }
  await resolveDeathEffects(knight, version, 'knight')
}

const collectVoteRound = async (version, candidateIds = null, label = '公投') => {
  const eligibleCandidates = alivePlayers.value.filter(player => !candidateIds || candidateIds.includes(player.id))
  const ballots = []
  for (const voter of [...alivePlayers.value]) {
    if (!isLoopActive(version)) break
    const candidates = eligibleCandidates.filter(candidate => candidate.id !== voter.id)
    if (!candidates.length) continue
    const decision = await requestPlayerDecision(voter, 'vote', { candidates: candidates.map(p => `${p.playerNumber}号`).join('、') })
    recordPrivateThinking(voter, decision.thinking, `${label}投票`)
    const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
    if (!target) continue
    ballots.push({ voterId: voter.id, targetId: target.id })
    playerMemories[voter.id]?.votes.push({ day: currentDay.value, targetId: target.id, label })
    addGameMessage({ sender: `${voter.playerNumber}号 ${voter.playerName}`, content: `投给${target.playerNumber}号${target.playerName}`, type: 'vote-action', visibility: 'private', privateFor: voter.id })
  }
  const counts = new Map()
  ballots.forEach(ballot => {
    const voter = players.value.find(player => player.id === ballot.voterId)
    counts.set(ballot.targetId, (counts.get(ballot.targetId) || 0) + (voter?.isSheriff ? 1.5 : 1))
  })
  const max = Math.max(0, ...counts.values())
  const leaders = [...counts.entries()].filter(([, count]) => count === max).map(([id]) => id)
  voteHistory.value.push({ day: currentDay.value, label, ballots, leaders })
  const publicBallots = ballots.map(ballot => `${getPlayerNumberById(ballot.voterId)}号→${getPlayerNumberById(ballot.targetId)}号`).join('，')
  addRefereeMessage(`${label}结果：${publicBallots || '无有效票'}。`)
  return { ballots, leaders, max }
}

const giveLastWords = async (player, version, reason = 'exile') => {
  if (!player || !isLoopActive(version)) return
  if (reason === 'exile' && (currentDay.value !== 1 || lastWordsGiven.value)) {
    addRefereeMessage(`${player.playerNumber}号出局，本局遗言名额已使用，不再发表遗言。`)
    return
  }
  if (reason === 'night' && (currentRound.value !== 1 || Number(boardRules.value.players) !== 12 || lastWordsGiven.value)) return
  lastWordsGiven.value = true
  addRefereeMessage(reason === 'night'
    ? `${player.playerNumber}号${player.playerName}是首夜出局玩家，请发表遗言。`
    : `${player.playerNumber}号${player.playerName}被公投出局，请发表遗言。`)
  startPlayerSpeaking(player.id)
  const result = await generatePublicSpeech(player, 'lastWords')
  recordPrivateThinking(player, result.thinking, '遗言')
  addDialogMessage(player.playerName, result.speech, 'player')
  await presentSpeech(player, result.speech, result.thinking, result.language)
  endPlayerSpeaking()
}

const runVotePhase = async (version) => {
  currentPhase.value = 'vote'
  await runKnightAction(version)
  if (dayInterrupted.value || !isLoopActive(version)) return
  addRefereeMessage('主持人发起放逐投票。所有存活玩家必须私下提交一名放逐目标，不得弃票。')
  let result = await collectVoteRound(version)
  if (!isLoopActive(version) || !result.leaders.length) {
    addRefereeMessage('没有形成有效投票，本日无人出局。')
    return
  }
  let exileId = result.leaders.length === 1 ? result.leaders[0] : null
  if (!exileId) {
    const pkByCode = Math.random() < 0.5
    const tiedNumbers = result.leaders.map(getPlayerNumberById).join('号、') + '号'
    if (!pkByCode) {
      addRefereeMessage(`${tiedNumbers}平票。主持人通过代码随机采用“平安日”规则，本日无人出局。`)
      return
    }
    addRefereeMessage(`${tiedNumbers}平票。主持人通过代码随机采用PK流程，平票玩家依次补充发言。`)
    await runSpeechPhase(version, result.leaders, 'pkSpeech')
    currentPhase.value = 'vote'
    result = await collectVoteRound(version, result.leaders, 'PK重投')
    exileId = result.leaders.length === 1 ? result.leaders[0] : null
    if (!exileId) {
      addRefereeMessage('PK重投仍然平票，本日无人出局。')
      return
    }
  }
  const exiled = players.value.find(player => player.id === exileId)
  killPlayer(exileId)
  lastExiledPlayerId.value = exileId
  await giveLastWords(exiled, version)
  await resolveDeathEffects(exiled, version, 'exile')
}

const runGameLoop = async (version) => {
  if (phaseRunning.value) return
  phaseRunning.value = true
  await phaseDelay(500)
  try {
    while (isLoopActive(version)) {
      await waitWhilePaused()
      await runNightPhase(version)
      if (!isLoopActive(version)) break
      await waitWhilePaused()
      await announceDay(version)
      if (checkGameEnd() || !isLoopActive(version)) break
      await waitWhilePaused()
      if (boardRules.value.sheriff && !sheriffElectionDone.value) {
        await runSheriffElection(version)
        if (checkGameEnd() || !isLoopActive(version)) break
      }
      await runSpeechPhase(version)
      if (!isLoopActive(version)) break
      if (dayInterrupted.value) {
        if (checkGameEnd() || !isLoopActive(version)) break
        dayInterrupted.value = false
        currentRound.value++
        currentDay.value++
        await phaseDelay(700)
        continue
      }
      await waitWhilePaused()
      await runVotePhase(version)
      if (checkGameEnd() || !isLoopActive(version)) break
      currentRound.value++
      currentDay.value++
      await phaseDelay(700)
    }
  } catch (error) {
    console.error('Game loop failed:', error)
    addRefereeMessage('游戏流程发生异常，已暂停。', { visibility: 'public', detail: error.message })
  } finally {
    if (gameLoopVersion === version) phaseRunning.value = false
  }
}

onMounted(() => loadGameData())
onUnmounted(() => {
  gameLoopVersion++
  gameStarted.value = false
  isGamePaused.value = false
  releasePauseWaiters()
  closeSpeech('unmount')
  stopSpeaking()
})
</script>

<style scoped>
.game-play {
  min-height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ===== Top Bar ===== */
.game-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: linear-gradient(180deg, rgba(26,18,16,0.95), rgba(20,14,12,0.95));
  border-bottom: 1px solid var(--gold-dark);
  backdrop-filter: blur(10px);
  gap: 16px;
  flex-wrap: wrap;
}
.topbar-left { display: flex; align-items: center; gap: 12px; }
.topbar-icon { font-size: 1.5rem; }
.topbar-title { font-size: 1.2rem; margin: 0; color: var(--gold); }
.room-config-tag {
  padding: 3px 10px; border: 1px solid rgba(201,169,110,0.28); border-radius: 4px;
  color: var(--text-secondary); background: rgba(201,169,110,0.07);
  font: 600 0.7rem/1.4 var(--font-heading); white-space: nowrap;
}
.phase-tag {
  padding: 3px 14px; border-radius: 12px; font-family: var(--font-heading); font-size: 0.75rem;
  background: rgba(201,169,110,0.12); color: var(--gold); border: 1px solid rgba(201,169,110,0.3);
}
.phase-tag.night { background: rgba(100,100,180,0.15); border-color: rgba(150,150,220,0.3); color: #aab; }
.phase-tag.day { background: rgba(180,160,100,0.15); border-color: rgba(200,180,120,0.3); color: var(--gold); }

.topbar-center { display: flex; align-items: center; }
.btn-start {
  padding: 10px 32px; font-family: var(--font-heading); font-weight: 700; font-size: 1rem;
  background: linear-gradient(135deg, var(--gold-dark), var(--gold)); color: var(--bg-deepest);
  border: none; border-radius: 6px; cursor: pointer; box-shadow: var(--shadow-gold);
  transition: all var(--transition-normal); display: flex; align-items: center; gap: 8px;
}
.btn-start:hover { box-shadow: var(--shadow-gold-strong); transform: translateY(-1px); }

.game-stats { display: flex; align-items: center; gap: 10px; font-family: var(--font-heading); color: var(--text-secondary); font-size: 0.9rem; }
.stat-divider { color: var(--gold-dark); }

.topbar-right { display: flex; align-items: center; gap: 16px; }
.btn-pause {
  padding: 6px 16px; font-family: var(--font-heading); font-size: 0.8rem;
  background: rgba(201,169,110,0.08); color: var(--gold); border: 1px solid rgba(201,169,110,0.35);
  border-radius: 4px; cursor: pointer; transition: all var(--transition-normal);
}
.btn-pause:hover, .btn-pause.active { background: rgba(201,169,110,0.18); border-color: var(--gold); }
.btn-exit {
  padding: 6px 18px; font-family: var(--font-heading); font-size: 0.8rem;
  background: transparent; color: var(--text-muted); border: 1px solid rgba(139,58,58,0.4);
  border-radius: 4px; cursor: pointer; transition: all var(--transition-normal);
}
.btn-exit:hover { border-color: var(--blood); color: #c44; box-shadow: 0 0 12px rgba(139,0,0,0.2); }

.view-controls { display: flex; gap: 14px; align-items: center; }
.speed-group, .toggle-group { display: flex; align-items: center; gap: 6px; }
.ctrl-label { font-size: 0.7rem; color: var(--text-muted); font-family: var(--font-heading); }
.speed-btn, .toggle-btn {
  padding: 2px 8px; font-family: var(--font-heading); font-size: 0.7rem;
  background: var(--bg-input); color: var(--text-muted); border: 1px solid rgba(201,169,110,0.2);
  border-radius: 3px; cursor: pointer; transition: all var(--transition-normal);
}
.speed-btn.active, .toggle-btn.active { background: rgba(201,169,110,0.15); color: var(--gold); border-color: var(--gold-dark); }
.speed-btn:hover, .toggle-btn:hover { border-color: var(--gold); }

/* ===== View Bar ===== */
.view-bar {
  display: flex; align-items: center; justify-content: center; gap: 14px;
  padding: 8px 20px; background: rgba(13,10,8,0.8); border-bottom: 1px solid rgba(201,169,110,0.1);
}
.paused-status {
  padding: 3px 10px; border: 1px solid rgba(214,87,69,0.55); border-radius: 4px;
  color: #e48778; background: rgba(139,0,0,0.15); font: 700 0.68rem/1.4 var(--font-heading);
}
.view-mode-toggle { display: flex; gap: 4px; }
.view-btn {
  padding: 4px 16px; font-family: var(--font-heading); font-size: 0.75rem;
  background: transparent; color: var(--text-muted); border: 1px solid transparent;
  border-radius: 4px; cursor: pointer; transition: all var(--transition-normal);
}
.view-btn.active { background: rgba(201,169,110,0.1); color: var(--gold); border-color: rgba(201,169,110,0.3); }
.view-btn:hover { color: var(--gold); }

/* ===== Game Layout ===== */
.game-layout {
  display: grid;
  grid-template-columns: 220px 1fr 220px;
  gap: 16px;
  flex: 1;
  padding: 16px;
  min-height: 0;
}

/* ===== Side Panels ===== */
.side-panel {
  position: sticky; top: 16px; align-self: start;
  max-height: calc(100vh - 180px); overflow-y: auto;
}
.player-badges { display: flex; flex-direction: column; gap: 10px; align-items: center; }

.player-badge {
  width: 110px; cursor: pointer; transition: all var(--transition-normal);
  position: relative;
}
.player-badge:hover { transform: scale(1.04); }
.player-badge.speaking .badge-frame { border-color: var(--gold) !important; animation: glow 2s ease-in-out infinite; }
.player-badge.dead .badge-frame { opacity: 0.5; filter: grayscale(0.6); }

.badge-frame {
  background: var(--bg-card); border: 1px solid rgba(201,169,110,0.2);
  border-radius: 12px; padding: 10px 8px; text-align: center;
  position: relative; overflow: hidden;
}
.badge-frame::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; height: 2px;
  background: linear-gradient(90deg, transparent, var(--gold-dark), transparent); opacity: 0.4;
}
.badge-ring {
  position: absolute; inset: 3px; border-radius: 9px;
  border: 1px solid rgba(201,169,110,0.1); pointer-events: none;
}
.badge-num {
  display: block; font-family: var(--font-heading); font-size: 0.7rem;
  color: var(--text-muted); margin-bottom: 2px;
}
.badge-name {
  display: block; font-family: var(--font-heading); font-size: 0.8rem;
  font-weight: 600; color: var(--text-primary); margin-bottom: 2px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.badge-crown { position: absolute; top: 2px; right: 6px; font-size: 0.9rem; }
.badge-skull { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); font-size: 1.5rem; opacity: 0.7; }
.badge-role {
  display: inline-block; padding: 1px 8px; border-radius: 8px;
  font-size: 0.65rem; font-family: var(--font-heading); font-weight: 600;
}
.badge-role.wolf { background: rgba(139,0,0,0.3); color: #f66; }
.badge-role.villager { background: rgba(180,180,180,0.15); color: #bbb; border: 1px solid rgba(180,180,180,0.3); }
.badge-role.god { background: rgba(201,169,110,0.15); color: var(--gold); }

.badge-empty {
  background: var(--bg-input); border: 1px dashed rgba(201,169,110,0.2);
  border-radius: 12px; padding: 16px 8px; text-align: center;
  color: var(--text-muted); font-size: 0.75rem; transition: all var(--transition-normal);
}
.badge-empty:hover { border-color: var(--gold); color: var(--gold); }
.badge-plus { display: block; font-size: 1.4rem; margin-bottom: 2px; }

.badge-locked {
  background: rgba(13,10,8,0.5); border: 1px solid rgba(201,169,110,0.06);
  border-radius: 12px; padding: 16px 8px; text-align: center;
  font-size: 1.2rem; opacity: 0.3;
}

/* ===== Center: Chat ===== */
.center-panel { display: flex; flex-direction: column; min-height: 0; gap: 10px; }

.game-ledger {
  display: grid; grid-template-columns: auto auto minmax(180px, 1fr); gap: 8px;
  padding: 9px; border: 1px solid rgba(201,169,110,0.16); border-radius: 6px;
  background: rgba(13,10,8,0.78);
}
.ledger-item { min-width: 90px; padding: 6px 9px; border-left: 2px solid rgba(201,169,110,0.32); }
.ledger-label { display: block; margin-bottom: 4px; color: var(--text-muted); font-size: 0.62rem; }
.ledger-item strong { color: var(--text-secondary); font-size: 0.76rem; line-height: 1.4; }
.ledger-order { min-width: 0; }
.ledger-order strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ledger-result { grid-column: 1 / -1; }
.ledger-result strong { color: #d6bd82; }

.thinking-bar { display: flex; gap: 10px; flex-wrap: wrap; }
.thinking-item {
  display: flex; align-items: center; gap: 6px;
  padding: 4px 12px; border-radius: 12px;
  background: rgba(122,110,94,0.1); border: 1px solid rgba(122,110,94,0.2);
  font-size: 0.8rem; color: var(--text-secondary);
}
.thinking-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--gold); animation: blink 1s infinite;
}

/* Speech card */
.speech-flash { animation: fadeIn 0.3s ease; }
.speech-card {
  background: var(--bg-card); border: 1px solid var(--gold-dark);
  border-radius: 12px; padding: 16px; box-shadow: var(--shadow-gold);
}
.speech-header { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.speech-avatar { font-size: 2rem; }
.speech-name { font-family: var(--font-heading); font-weight: 600; color: var(--gold); }
.speech-label { font-size: 0.75rem; color: var(--text-muted); }
.speech-timer { margin-left: auto; min-width: 72px; text-align: right; color: var(--gold); }
.speech-timer strong { display: block; font: 700 1.15rem/1.1 var(--font-heading); }
.speech-timer small { display: block; margin-top: 3px; color: var(--text-muted); font-size: 0.58rem; }
.speech-timer.warning strong { color: #e48778; }
.speech-timer.paused { opacity: 0.55; }

.think-box { margin-bottom: 12px; padding: 10px 14px; background: rgba(230,162,60,0.06); border-left: 3px solid #e6a23c; border-radius: 0 6px 6px 0; }
.think-title { font-size: 0.8rem; color: #e6a23c; margin-bottom: 6px; font-family: var(--font-heading); }
.think-text { font-size: 0.85rem; color: var(--text-secondary); font-style: italic; line-height: 1.5; }

.speech-bubble {
  position: relative; background: var(--bg-parchment);
  border: 1px solid rgba(201,169,110,0.2); border-radius: 8px;
  padding: 14px 18px; margin-bottom: 10px;
}
.bubble-arrow {
  position: absolute; top: -8px; left: 20px;
  width: 0; height: 0;
  border-left: 8px solid transparent; border-right: 8px solid transparent;
  border-bottom: 8px solid var(--bg-parchment);
}
.bubble-text { font-size: 0.95rem; line-height: 1.7; color: var(--text-primary); }
.cursor-blink { color: var(--gold); animation: blink 0.7s infinite; }

.speech-actions { display: flex; justify-content: flex-end; gap: 8px; }
.speech-btn {
  padding: 5px 16px; font-family: var(--font-heading); font-size: 0.75rem; border-radius: 4px;
  background: var(--bg-input); color: var(--text-secondary); border: 1px solid rgba(201,169,110,0.2); cursor: pointer;
}
.speech-btn.primary { background: var(--gold-dark); color: var(--bg-deepest); border-color: var(--gold-dark); }
.speech-btn:hover { border-color: var(--gold); }
.speech-btn:disabled { opacity: 0.38; cursor: not-allowed; }

/* Chat area */
.chat-scroll-shell {
  position: relative;
  height: clamp(380px, 58vh, 680px);
  min-height: 0;
}
.chat-area {
  height: 100%; overflow-y: auto; padding: 12px 8px;
  background: rgba(13,10,8,0.5); border-radius: 8px;
  border: 1px solid rgba(201,169,110,0.08);
  display: flex; flex-direction: column; gap: 10px;
}
.scroll-bottom-btn {
  position: absolute; right: 18px; bottom: 16px; z-index: 3;
  display: inline-flex; align-items: center; gap: 6px; padding: 7px 12px;
  border: 1px solid rgba(201,169,110,0.45); border-radius: 4px;
  background: rgba(24,17,14,0.95); color: var(--gold); cursor: pointer;
  box-shadow: 0 4px 14px rgba(0,0,0,0.3); font: 700 0.68rem/1 var(--font-heading);
}
.scroll-bottom-btn:hover { border-color: var(--gold); }

/* Referee message */
.referee-msg {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 6px 16px; font-size: 0.8rem; color: var(--text-muted);
  text-align: center;
}
.referee-icon { font-size: 0.9rem; opacity: 0.6; }
.referee-detail { display: block; margin-top: 3px; color: #d6bd82; font-size: 0.68rem; line-height: 1.45; }

.private-log {
  display: grid; grid-template-columns: 94px minmax(90px, auto) 1fr; align-items: start; gap: 9px;
  padding: 8px 10px; border-left: 2px solid #8c7a55; border-radius: 0 4px 4px 0;
  background: rgba(201,169,110,0.055); color: var(--text-secondary); font-size: 0.76rem; line-height: 1.5;
}
.private-log.thinking { border-left-color: #b98a3c; background: rgba(185,138,60,0.07); }
.private-log.vote-action { border-left-color: #738ba0; background: rgba(115,139,160,0.07); }
.private-log-label { color: var(--text-muted); font: 700 0.58rem/1.6 var(--font-heading); }
.private-log strong { color: var(--gold); }

/* Bubble */
.bubble-row { display: flex; gap: 10px; align-items: flex-start; animation: fadeIn 0.3s ease; }
.bubble-row.mine { flex-direction: row-reverse; }
.bubble-avatar { font-size: 1.6rem; flex-shrink: 0; width: 36px; text-align: center; }
.bubble-wrap { max-width: 70%; }
.bubble-sender { font-size: 0.7rem; color: var(--text-muted); margin-bottom: 2px; font-family: var(--font-heading); }
.bubble-row.mine .bubble-sender { text-align: right; }
.bubble-body {
  background: var(--bg-parchment); border: 1px solid rgba(201,169,110,0.15);
  border-radius: 8px; padding: 10px 14px; font-size: 0.9rem; line-height: 1.5;
  color: var(--text-primary); word-break: break-word;
}
.bubble-body.wolf { background: rgba(139,0,0,0.1); border-color: rgba(139,58,58,0.3); }
.bubble-time { font-size: 0.65rem; color: var(--text-muted); margin-top: 2px; }
.bubble-row.mine .bubble-time { text-align: right; }

/* Chat input */
.chat-input {
  display: flex; gap: 8px; align-items: flex-end;
  padding: 8px 0 0;
}
.send-btn {
  width: 40px; height: 40px; flex-shrink: 0;
  font-size: 1.2rem; border-radius: 6px; border: 1px solid rgba(201,169,110,0.3);
  background: var(--bg-input); color: var(--gold); cursor: pointer;
  transition: all var(--transition-normal); display: flex; align-items: center; justify-content: center;
}
.send-btn:hover:not(:disabled) { background: var(--gold-dark); color: var(--bg-deepest); }
.send-btn:disabled { opacity: 0.3; cursor: not-allowed; }

@media (max-width: 1100px) { .game-layout { grid-template-columns: 1fr; grid-template-rows: auto 1fr auto; } .side-panel { position: static; max-height: 160px; } .player-badges { flex-direction: row; flex-wrap: wrap; justify-content: center; } }
@media (max-width: 700px) { .game-ledger { grid-template-columns: 1fr 1fr; } .ledger-order { grid-column: 1 / -1; } .private-log { grid-template-columns: 1fr; gap: 3px; } }
@media (max-width: 600px) { .game-topbar { flex-direction: column; } .topbar-left, .topbar-right { width: 100%; justify-content: center; } }
</style>
