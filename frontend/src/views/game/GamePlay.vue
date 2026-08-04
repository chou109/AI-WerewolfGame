<template>
  <div class="game-play">
    <Transition name="deal-fade">
      <div v-if="roleDealVisible" class="role-deal-overlay">
        <div class="deal-stage" :class="roleDealStage">
          <div class="deal-deck" aria-hidden="true">
            <div v-for="index in 7" :key="index" class="deal-card" :style="{ '--card-index': index - 4 }">
              <span>AI</span><strong>WEREWOLF</strong>
            </div>
          </div>
          <div class="deal-copy">
            <span>IDENTITY DRAW</span>
            <strong>{{ roleDealMessage }}</strong>
            <small>{{ roleDealProgress }}</small>
          </div>
        </div>
      </div>
    </Transition>

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
          <el-option v-for="p in players" :key="p.id" :label="getViewerLabel(p)" :value="p.id" />
        </el-select>
      </div>
      <span v-if="isGamePaused" class="paused-status">{{ $locale === 'zh-CN' ? '游戏已暂停' : 'GAME PAUSED' }}</span>
    </div>

    <!-- ===== Main Layout ===== -->
    <div class="game-layout">
      <!-- Left: Players 1-6 -->
      <div class="side-panel">
        <div class="player-badges">
          <div v-for="pos in leftPositions" :key="pos.number" class="player-badge" :class="{ occupied: pos.player, locked: pos.locked, dead: pos.player && !pos.player.isAlive, speaking: pos.player?.isSpeaking, viewing: currentViewMode === 'player' && selectedPlayerId === pos.player?.id }" @click="handlePositionClick(pos)">
            <template v-if="pos.player">
              <div class="badge-frame">
                <div class="badge-ring"></div>
                <span class="badge-num">{{ pos.player.playerNumber }}</span>
                <span v-if="pos.player.isSheriff && pos.player.isAlive" class="badge-crown-top" :title="$locale === 'zh-CN' ? '警长' : 'Sheriff'">♛</span>
                <span v-else-if="pos.player.isSheriffCandidate && pos.player.isAlive && !sheriffElectionDone" class="badge-hand" :title="$locale === 'zh-CN' ? '已上警' : 'Running for sheriff'">✋</span>
                <span v-if="gameStarted && getVisibleRoleLabel(pos.player)" class="badge-role" :class="getVisibleRoleClass(pos.player)">{{ getVisibleRoleLabel(pos.player) }}</span>
                <div class="badge-avatar">
                  <img v-if="getPlayerAvatar(pos.player.id)" :src="getPlayerAvatar(pos.player.id)" :alt="pos.player.playerName" />
                  <span v-else>{{ getAvatarPlaceholder(pos.player.playerName) }}</span>
                </div>
                <button v-if="pos.player.aiPlayerId" class="badge-voice" :title="$locale === 'zh-CN' ? '单独语音配置' : 'Player voice settings'" @click.stop="openPlayerVoiceDialog(pos.player)">🔊</button>
                <div class="badge-footer">
                  <span class="badge-name">{{ pos.player.playerName }}</span>
                </div>
                <span v-if="!pos.player.isAlive" class="badge-skull">×</span>
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
          <div v-if="aiRequestState.pending || aiRequestState.inFlight || aiRequestState.lastError" class="ledger-item ledger-ai-status">
            <span class="ledger-label">{{ $locale === 'zh-CN' ? '模型请求' : 'MODEL REQUESTS' }}</span>
            <strong>
              {{ $locale === 'zh-CN' ? `排队 ${aiRequestState.pending} · 进行中 ${aiRequestState.inFlight}` : `Queued ${aiRequestState.pending} · Active ${aiRequestState.inFlight}` }}
            </strong>
            <small v-if="aiRequestState.lastError">{{ aiRequestState.lastError }}</small>
          </div>
        </div>

        <div v-if="decisionWindow.active" class="decision-window">
          <div>
            <span class="decision-dot"></span>
            <strong>{{ decisionWindow.label }}</strong>
            <small>{{ $locale === 'zh-CN' ? '所有参与者同时决策' : 'All participants decide simultaneously' }}</small>
          </div>
          <span class="decision-progress">{{ decisionWindow.completed }} / {{ decisionWindow.total }}</span>
          <strong class="decision-countdown">{{ decisionWindow.remaining }}s</strong>
        </div>

        <!-- AI Thinking -->
        <div v-if="currentViewMode === 'god' && showThinking && aiThinkingPlayers.length" class="thinking-bar">
          <span v-for="pid in aiThinkingPlayers" :key="pid" class="thinking-item">
            <span class="thinking-dot"></span>
            {{ $t('gamePlay.thinking', { name: getPlayerNameById(pid) }) }}
          </span>
        </div>

        <!-- AI Speech Card -->
        <div v-if="aiSpeakingContent" class="speech-flash">
          <div class="speech-card">
            <div class="speech-header">
              <div class="speech-avatar">
                <img v-if="getPlayerAvatar(aiSpeakingContent.playerId)" :src="getPlayerAvatar(aiSpeakingContent.playerId)" :alt="aiSpeakingContent.playerName" />
                <span v-else>{{ getAvatarPlaceholder(aiSpeakingContent.playerName) }}</span>
              </div>
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
              <span class="auto-pass-status">{{ $locale === 'zh-CN' ? 'AI 将在发言完成后自动过麦' : 'AI will pass automatically' }}</span>
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
                <div class="bubble-avatar">
                  <img v-if="getSenderAvatar(msg.sender)" :src="getSenderAvatar(msg.sender)" :alt="msg.sender" />
                  <span v-else>{{ getAvatarPlaceholder(msg.sender) }}</span>
                </div>
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
          <div v-for="pos in rightPositions" :key="pos.number" class="player-badge" :class="{ occupied: pos.player, locked: pos.locked, dead: pos.player && !pos.player.isAlive, speaking: pos.player?.isSpeaking, viewing: currentViewMode === 'player' && selectedPlayerId === pos.player?.id }" @click="handlePositionClick(pos)">
            <template v-if="pos.player">
              <div class="badge-frame">
                <div class="badge-ring"></div>
                <span class="badge-num">{{ pos.player.playerNumber }}</span>
                <span v-if="pos.player.isSheriff && pos.player.isAlive" class="badge-crown-top" :title="$locale === 'zh-CN' ? '警长' : 'Sheriff'">♛</span>
                <span v-else-if="pos.player.isSheriffCandidate && pos.player.isAlive && !sheriffElectionDone" class="badge-hand" :title="$locale === 'zh-CN' ? '已上警' : 'Running for sheriff'">✋</span>
                <span v-if="gameStarted && getVisibleRoleLabel(pos.player)" class="badge-role" :class="getVisibleRoleClass(pos.player)">{{ getVisibleRoleLabel(pos.player) }}</span>
                <div class="badge-avatar">
                  <img v-if="getPlayerAvatar(pos.player.id)" :src="getPlayerAvatar(pos.player.id)" :alt="pos.player.playerName" />
                  <span v-else>{{ getAvatarPlaceholder(pos.player.playerName) }}</span>
                </div>
                <button v-if="pos.player.aiPlayerId" class="badge-voice" :title="$locale === 'zh-CN' ? '单独语音配置' : 'Player voice settings'" @click.stop="openPlayerVoiceDialog(pos.player)">🔊</button>
                <div class="badge-footer">
                  <span class="badge-name">{{ pos.player.playerName }}</span>
                </div>
                <span v-if="!pos.player.isAlive" class="badge-skull">×</span>
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

    <el-dialog v-model="playerVoiceDialogVisible" :title="voiceEditingPlayer ? `${voiceEditingPlayer.playerNumber}号 ${voiceEditingPlayer.playerName} · 单独语音配置` : '单独语音配置'" width="520px" class="player-voice-dialog">
      <el-form :model="playerVoiceDraft" label-position="top">
        <div class="voice-switch-row">
          <div><strong>继承 AI 默认语音</strong><span>优先使用 AI 玩家列表中的语音设置；未设置时继承全局语音</span></div>
          <el-switch v-model="playerVoiceDraft.inherit" />
        </div>
        <template v-if="!playerVoiceDraft.inherit">
          <div class="voice-switch-row">
            <div><strong>启用该玩家语音</strong><span>只控制当前 AI 玩家</span></div>
            <el-switch v-model="playerVoiceDraft.enabled" />
          </div>
          <el-form-item label="语音引擎">
            <el-radio-group v-model="playerVoiceDraft.engine">
              <el-radio-button value="browser">浏览器语音</el-radio-button>
              <el-radio-button value="cloud">云端语音</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="playerVoiceDraft.engine === 'browser'" label="音色">
            <el-select v-model="playerVoiceLanguageFilter" :placeholder="$t('voiceConfig.voiceLanguagePlaceholder')" style="width:100%; margin-bottom: 8px">
              <el-option :label="$t('voiceConfig.allLanguages')" value="all" />
              <el-option v-for="option in playerVoiceLanguageOptions" :key="option.key" :label="`${option.label} (${option.count})`" :value="option.key" />
            </el-select>
            <el-select v-model="playerVoiceDraft.voiceURI" clearable filterable placeholder="自动选择匹配语言的音色" style="width:100%">
              <el-option-group v-for="group in playerVoiceGroups" :key="group.key" :label="group.label">
                <el-option v-for="voice in group.voices" :key="voice.voiceURI" :label="`${voice.name} (${voice.lang})`" :value="voice.voiceURI" />
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item v-else label="云端音色">
            <el-select v-model="playerVoiceDraft.cloudVoice" filterable allow-create default-first-option placeholder="选择或输入音色名称" style="width:100%">
              <el-option v-for="voice in cloudPlayerVoices" :key="voice" :label="voice" :value="voice" />
            </el-select>
          </el-form-item>
          <div class="player-voice-sliders">
            <el-form-item label="语速">
              <div class="voice-slider"><el-slider v-model="playerVoiceDraft.rate" :min="0.5" :max="2" :step="0.1" /><span>{{ playerVoiceDraft.rate.toFixed(1) }}x</span></div>
            </el-form-item>
            <el-form-item label="音调">
              <div class="voice-slider"><el-slider v-model="playerVoiceDraft.pitch" :min="0.5" :max="2" :step="0.1" :disabled="playerVoiceDraft.engine === 'cloud'" /><span>{{ playerVoiceDraft.pitch.toFixed(1) }}</span></div>
            </el-form-item>
            <el-form-item label="音量">
              <div class="voice-slider"><el-slider v-model="playerVoiceDraft.volume" :min="0" :max="1" :step="0.1" /><span>{{ Math.round(playerVoiceDraft.volume * 100) }}%</span></div>
            </el-form-item>
          </div>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="testPlayerVoice">试听</el-button>
        <el-button @click="playerVoiceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePlayerVoiceConfig">保存</el-button>
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
import { getVoiceLanguageKey, getVoiceLanguageLabel, groupVoicesByLanguage, speakText, stopSpeaking, pauseSpeaking, resumeSpeaking } from '../../composables/useSpeechSynthesis.js'
import { getAiPlayerKey, getGlobalApiKey } from '../../utils/apiKeys.js'
import { useUserStore } from '../../stores/user'
import { BOARD_RULES, PACK_WOLF_ROLES, WEREWOLF_KNOWLEDGE, WOLF_TEAM_ROLES, getBoardRules, getRoleSummary, isPackWolfRole, isWolfTeamRole } from '../../game/rules.js'

const { proxy } = getCurrentInstance()
const $t = proxy.$t
const $locale = proxy.$locale

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
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
    nightmareTargetId: null,
    silencerTargetId: null,
    magicianSwapIds: [],
    mechanicalWolfTargetId: null,
    mechanicalWolfRole: null,
    mediumTargetId: null,
    mediumResult: null,
    dreamerTargetId: null,
    evilKnightSeerReflected: false,
    evilKnightPoisonReflected: false,
    cursedFoxSeerTargetId: null,
    wolvesBlocked: false,
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
const isRoomOwner = computed(() => {
  const uid = Number(userStore.userInfo?.id)
  return uid > 0 && Number(roomInfo.value.creatorId) === uid
})
const boardConfigOverrides = ref(null)
const roleDealVisible = ref(false)
const roleDealStage = ref('shuffle')
const roleDealMessage = ref('身份牌洗牌中')
const roleDealProgress = ref('0 / 0')
const aiThinkingPlayers = ref([])
const decisionWindow = reactive({ active: false, label: '', remaining: 0, total: 0, completed: 0 })
const aiSpeakingContent = ref(null)
const aiPlayers = ref([])
const selectedAiPlayerId = ref(null)
const addAiDialogVisible = ref(false)
const currentPosition = ref(null)
const currentViewMode = ref('god')
const selectedPlayerId = ref(null)
const showThinking = ref(localStorage.getItem('showThinking') === 'true')
const typewriterSpeed = ref(localStorage.getItem('typewriterSpeed') || 'normal')
const playerVoiceDialogVisible = ref(false)
const voiceEditingPlayer = ref(null)
const browserVoices = ref([])
const playerVoiceLanguageFilter = ref('all')
const playerVoiceDraft = reactive({ inherit: true, enabled: true, engine: 'browser', voiceURI: '', cloudVoice: 'alloy', rate: 1, pitch: 1, volume: 1 })
const playerVoiceConfigs = reactive({})
const aiProfileVoiceConfigs = reactive({})
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
const sheriffBadgeLost = ref(false)
const publicRoleClaims = reactive({})
const publicMiracleClaims = reactive({})
const lastExiledPlayerId = ref(null)
const firstNightLastWordsPlayerIds = new Set()
const firstDayLastWordsGiven = ref(false)
const dayInterrupted = ref(false)
const resumeStage = ref('night')
const silencedPlayerId = ref(null)
const bonusDayPending = ref(0)
const bonusNightPending = ref(0)
const miracleMerchantState = reactive({ used: false, merchantId: null, luckyId: null, skill: null, pendingDeath: false, transactionFailed: false, giftResolved: false })
const wolfBeautyState = reactive({ previousTargetId: null, targetId: null })
const idiotFlippedIds = new Set()
const bomberSuppressedIds = new Set()
const specialDeathProcessedIds = new Set()
const stalkerState = reactive({ used: false, pendingTargetId: null })
const silencerState = reactive({ previousTargetId: null })
const nightmareState = reactive({ previousTargetId: null })
const magicianState = reactive({ previousPair: '' })
const cupidState = reactive({ used: false, pairIds: [], resolving: false, mixed: false })
const wolfBrotherState = reactive({ brotherId: null, sisterId: null, awakened: false, awakenRound: null })
const janusState = reactive({ used: false, playerId: null, chosenRole: null })
const mechanicalWolfState = reactive({ learnedRole: null, playerId: null })
const dreamerState = reactive({ previousTargetId: null, currentTargetId: null })
const evilKnightState = reactive({ seerReflected: false, witchReflected: false })
const shapeshifterState = reactive({ playerId: null, originalRole: null })
const cursedFoxState = reactive({ playerId: null, killedBySeer: false })
const gameResult = ref(null)
const API_MIN_START_INTERVAL_PER_PROVIDER = 250
const AI_MAX_CONCURRENCY_PER_PROVIDER = 4
const AI_PROVIDER_COOLDOWN_AFTER_FAILURES = 3
const AI_PROVIDER_COOLDOWN_BASE_MS = 5000
const AI_PROVIDER_COOLDOWN_MAX_MS = 30000
const aiProviderGates = new Map()
const aiProviderHealth = new Map()
let lastAiFailureNoticeAt = 0
const AI_MAX_ATTEMPTS = 2
const AI_RETRYABLE_STATUSES = new Set([408, 425, 429, 500, 502, 503, 504])
const aiConfigCache = new Map()
const aiRequestState = reactive({ pending: 0, inFlight: 0, succeeded: 0, failed: 0, lastError: '', lastStatus: '', lastAt: 0 })
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
let activeAutoPassTimer = null
let activeTypewriterCompleteHandler = null
let decisionWindowTimer = null
let speechPausedByGame = false
let pauseWaiters = []
let refereeSpeechBarrier = Promise.resolve()
let hunterSkillUsed = new Set()
let wolfKingSkillUsed = new Set()
let knightSkillUsed = new Set()
let lastSnapshotSavedAt = 0
let snapshotWriteQueue = Promise.resolve()
let finalizationPromise = null

const SPEEDS = { slow: 80, normal: 50, fast: 30 }
const SPEECH_LIMIT_SECONDS = 120
const DECISION_WINDOW_SECONDS = 30
const speeds = [{ key: 'slow', label: $locale === 'zh-CN' ? '慢' : 'S' }, { key: 'normal', label: $locale === 'zh-CN' ? '常' : 'N' }, { key: 'fast', label: $locale === 'zh-CN' ? '快' : 'F' }]
const viewModes = [{ key: 'god', label: $t('gamePlay.godView') }, { key: 'player', label: $t('gamePlay.playerView') }, { key: 'spectator', label: $t('gamePlay.spectatorView') }]

watch(showThinking, v => localStorage.setItem('showThinking', v))
watch(typewriterSpeed, v => { localStorage.setItem('typewriterSpeed', v); setSpeed(v) })
setSpeed(typewriterSpeed.value)

const playerVoiceStorageKey = `werewolf:player-voices:${roomId}`
const AI_VOICE_STORAGE_KEY = 'werewolf:ai-voice-configs'
const cloudPlayerVoices = ['alloy', 'ash', 'ballad', 'coral', 'echo', 'fable', 'nova', 'onyx', 'sage', 'shimmer', 'verse', 'zh-CN-XiaoxiaoNeural', 'zh-CN-YunxiNeural', 'zh-CN-YunjianNeural', 'en-US-JennyNeural', 'en-US-GuyNeural']
const loadPlayerVoiceConfigs = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(playerVoiceStorageKey) || '{}')
    Object.assign(playerVoiceConfigs, saved && typeof saved === 'object' ? saved : {})
  } catch {
    Object.keys(playerVoiceConfigs).forEach(key => delete playerVoiceConfigs[key])
  }
}
const savePlayerVoiceConfigs = () => localStorage.setItem(playerVoiceStorageKey, JSON.stringify(playerVoiceConfigs))
const loadAiProfileVoiceConfigs = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(AI_VOICE_STORAGE_KEY) || '{}')
    Object.assign(aiProfileVoiceConfigs, saved && typeof saved === 'object' ? saved : {})
  } catch {
    Object.keys(aiProfileVoiceConfigs).forEach(key => delete aiProfileVoiceConfigs[key])
  }
}
const createPlayerVoiceConfig = saved => ({
  inherit: saved?.inherit !== false,
  enabled: saved?.enabled !== false,
  engine: saved?.engine === 'cloud' ? 'cloud' : 'browser',
  voiceURI: saved?.voiceURI || '',
  cloudVoice: saved?.cloudVoice || 'alloy',
  rate: Math.min(2, Math.max(0.5, Number(saved?.rate) || 1)),
  pitch: Math.min(2, Math.max(0.5, Number(saved?.pitch) || 1)),
  volume: Math.min(1, Math.max(0, Number.isFinite(Number(saved?.volume)) ? Number(saved.volume) : 1))
})
const getPlayerVoiceOverrides = player => {
  const roomConfig = playerVoiceConfigs[player?.id]
  const profileConfig = aiProfileVoiceConfigs[player?.aiPlayerId]
  const config = roomConfig && !roomConfig.inherit
    ? roomConfig
    : profileConfig && !profileConfig.inherit
      ? profileConfig
      : null
  if (!config) return {}
  return {
    enabled: config.enabled,
    engine: config.engine,
    voiceURI: config.voiceURI,
    rate: config.rate,
    pitch: config.pitch,
    volume: config.volume,
    readPlayers: config.enabled,
    cloud: { playerVoice: config.cloudVoice, speed: config.rate }
  }
}
const openPlayerVoiceDialog = player => {
  if (!player?.aiPlayerId) return
  voiceEditingPlayer.value = player
  Object.assign(playerVoiceDraft, createPlayerVoiceConfig(playerVoiceConfigs[player.id]))
  playerVoiceDialogVisible.value = true
}
const savePlayerVoiceConfig = () => {
  if (!voiceEditingPlayer.value) return
  playerVoiceConfigs[voiceEditingPlayer.value.id] = createPlayerVoiceConfig(playerVoiceDraft)
  savePlayerVoiceConfigs()
  persistGameSnapshot(resumeStage.value)
  ElMessage.success('该 AI 玩家的语音配置已保存')
  playerVoiceDialogVisible.value = false
}
const testPlayerVoice = async () => {
  const player = voiceEditingPlayer.value
  if (!player) return
  stopSpeaking()
  const overrides = playerVoiceDraft.inherit
    ? { ...getPlayerVoiceOverrides(player), force: true, speaker: 'player' }
    : { ...getPlayerVoiceOverrides({ id: '__draft__' }), force: true, speaker: 'player' }
  if (!playerVoiceDraft.inherit) Object.assign(overrides, {
    enabled: playerVoiceDraft.enabled,
    engine: playerVoiceDraft.engine,
    voiceURI: playerVoiceDraft.voiceURI,
    rate: playerVoiceDraft.rate,
    pitch: playerVoiceDraft.pitch,
    volume: playerVoiceDraft.volume,
    readPlayers: true,
    cloud: { playerVoice: playerVoiceDraft.cloudVoice, speed: playerVoiceDraft.rate }
  })
  await speakText(`${player.playerNumber}号${player.playerName}语音测试。`, overrides).catch(error => ElMessage.error(`试听失败：${error.message || '语音引擎不可用'}`))
}
const loadBrowserVoices = () => {
  if (typeof window === 'undefined' || !window.speechSynthesis) return
  browserVoices.value = window.speechSynthesis.getVoices()
  if (playerVoiceLanguageFilter.value !== 'all' && !playerVoiceLanguageOptions.value.some(option => option.key === playerVoiceLanguageFilter.value)) playerVoiceLanguageFilter.value = 'all'
}

// Computed
const playerVoiceLanguageOptions = computed(() => {
  const counts = new Map()
  browserVoices.value.forEach(voice => {
    const key = getVoiceLanguageKey(voice)
    counts.set(key, (counts.get(key) || 0) + 1)
  })
  return [...counts.entries()]
    .sort(([first], [second]) => first.localeCompare(second))
    .map(([key, count]) => ({ key, label: getVoiceLanguageLabel(key, currentLocale()), count }))
})
const playerVoiceGroups = computed(() => groupVoicesByLanguage(browserVoices.value, playerVoiceLanguageFilter.value, currentLocale()))
const alivePlayers = computed(() => players.value.filter(p => p.isAlive))
const wolfRolesArr = WOLF_TEAM_ROLES
const boardRules = computed(() => {
  if (boardConfigOverrides.value && Array.isArray(boardConfigOverrides.value.roles)) return boardConfigOverrides.value
  return getBoardRules(roomInfo.value.gameBoard || route.query.gameBoard, roomInfo.value.playerCount || 12)
})
const allBoardRoleNames = [...new Set(Object.values(BOARD_RULES).flatMap(rule => rule.roles.map(item => item.role)))].sort((a, b) => b.length - a.length)
const boardRoleNames = computed(() => new Set([
  ...boardRules.value.roles.map(item => item.role),
  ...players.value.map(player => player.role).filter(Boolean)
]))
const boardRolePattern = new RegExp(allBoardRoleNames.map(role => role.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|'), 'g')
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
  knight: ['骑士'],
  idiot: ['白痴'],
  stalker: ['潜行者'],
  silencer: ['禁言长老'],
  bear: ['熊'],
  nightmare: ['梦魇'],
  evilKnight: ['恶灵骑士'],
  magician: ['魔术师'],
  mechanicalWolf: ['机械狼'],
  medium: ['通灵师'],
  bomber: ['炸弹人'],
  sun: ['太阳'],
  moon: ['月亮'],
  cupid: ['丘比特'],
  wolfBrother: ['狼兄'],
  wolfSister: ['狼弟'],
  janus: ['千面人'],
  cursedFox: ['咒狐'],
  shapeshifter: ['百变狼王'],
  dreamer: ['摄梦人']
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
    night_nightmare: zh ? '梦魇行动' : 'Nightmare action',
    night_silencer: zh ? '禁言长老行动' : 'Silencer action',
    night_magician: zh ? '魔术师行动' : 'Magician action',
    night_mechanical_wolf: zh ? '机械狼行动' : 'Mechanical Wolf action',
    night_medium: zh ? '通灵师行动' : 'Medium action',
    night_cupid: zh ? '丘比特行动' : 'Cupid action',
    night_janus: zh ? '千面人行动' : 'Janus action',
    night_dreamer: zh ? '摄梦人行动' : 'Dreamer action',
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
const isRoleNotice = message => Boolean(
  message && (
    message.visibility === 'role'
    || (message.type === 'referee' && /^(你的身份是|Your role is)/.test(String(message.content || '').trim()))
  )
)
const filteredDialogMessages = computed(() => {
  const committedMessages = dialogMessages.value.filter(message => !message.hiddenDuringSpeech)
  const visibleMessages = showThinking.value
    ? committedMessages
    : committedMessages.filter(message => message.type !== 'thinking')
  if (currentViewMode.value === 'god') return visibleMessages.filter(message => !isRoleNotice(message))
  if (currentViewMode.value === 'spectator') return visibleMessages.filter(m => (m.visibility || 'public') === 'public')
  if (currentViewMode.value === 'player' && selectedPlayerId.value) {
    const sp = players.value.find(p => p.id === selectedPlayerId.value)
    if (!sp) return []
    return visibleMessages.filter(m => {
      const visibility = m.visibility || 'public'
      if (visibility === 'public') return true
      if (visibility === 'role') return m.privateFor === sp.id
      if (visibility === 'private') return m.privateFor === sp.id
      if (visibility === 'wolves') return isPackWolf(sp)
      return visibility === 'god' ? false : m.sender === sp.playerName
    })
  }
  return visibleMessages.filter(m => (m.visibility || 'public') === 'public')
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
  const m = { '狼人':'gamePlay.role.werewolf','狼王':'gamePlay.role.wolfKing','狼美人':'gamePlay.role.wolfBeauty','白狼王':'gamePlay.role.whiteWolf','石像鬼':'gamePlay.role.gargoyle','平民':'gamePlay.role.villager','预言家':'gamePlay.role.seer','女巫':'gamePlay.role.witch','猎人':'gamePlay.role.hunter','守卫':'gamePlay.role.guard','奇迹商人':'gamePlay.role.miracleMerchant','守墓人':'gamePlay.role.gravekeeper','愚者':'gamePlay.role.fool','骑士':'gamePlay.role.knight','白痴':'gamePlay.role.idiot','潜行者':'gamePlay.role.stalker','禁言长老':'gamePlay.role.silencer','熊':'gamePlay.role.bear','梦魇':'gamePlay.role.nightmare','恶灵骑士':'gamePlay.role.evilKnight','魔术师':'gamePlay.role.magician','机械狼':'gamePlay.role.mechanicalWolf','通灵师':'gamePlay.role.medium','炸弹人':'gamePlay.role.bomber','太阳':'gamePlay.role.sun','月亮':'gamePlay.role.moon','丘比特':'gamePlay.role.cupid','狼兄':'gamePlay.role.wolfBrother','狼弟':'gamePlay.role.wolfSister','千面人':'gamePlay.role.janus','咒狐':'gamePlay.role.cursedFox','百变狼王':'gamePlay.role.shapeshifter','摄梦人':'gamePlay.role.dreamer' }
  return m[role] ? $t(m[role]) : role
}
const getRoleClass = (role) => {
  if (wolfRolesArr.includes(role)) return 'wolf'
  if (['平民','Villager'].includes(role)) return 'villager'
  return 'god'
}
const getPlayerNameById = (pid) => { const p = players.value.find(x => x.id === pid); return p ? p.playerName : '' }
const getViewerLabel = player => player?.isAlive ? player.playerName : `${player?.playerName || ''}（已出局）`
const getPlayerAvatar = pid => players.value.find(player => player.id === pid)?.avatarUrl || ''
const getSenderAvatar = name => players.value.find(player => player.playerName === name)?.avatarUrl || ''
const getAvatarPlaceholder = name => String(name || 'AI').replace(/^\d+号\s*/, '').trim().slice(0, 4) || 'AI'
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
const getKnownRole = player => {
  if (!player?.role) return ''
  if (currentViewMode.value === 'god') return player.role
  if (currentViewMode.value !== 'player' || !selectedPlayerId.value) return ''
  const viewer = players.value.find(candidate => candidate.id === selectedPlayerId.value)
  if (!viewer) return ''
  if (viewer.id === player.id) return player.role
  if (isPackWolf(viewer) && isPackWolf(player)) return player.role
  const check = (playerMemories[viewer.id]?.checks || []).slice().reverse().find(item => item.targetId === player.id)
  return check?.result || ''
}
const canViewRole = player => Boolean(getKnownRole(player))
const getVisibleRoleLabel = player => {
  const known = getKnownRole(player)
  if (!known) return ''
  if (known === '好人') return currentLocale() === 'zh-CN' ? '好人阵营' : 'Good'
  if (known === '狼人') return currentLocale() === 'zh-CN' ? '狼人阵营' : 'Wolf'
  return getRoleName(known)
}
const getVisibleRoleClass = player => {
  const known = getKnownRole(player)
  if (known === '狼人') return 'wolf'
  if (known === '好人') return 'god'
  return getRoleClass(known)
}
const hasRole = (player, key) => roleAliases[key]?.includes(player?.role)
const isWolfRole = player => isWolfTeamRole(player?.role)
const isPackWolf = player => isPackWolfRole(player?.role)
const isBoard = key => boardRules.value.key === key
const hasAbilityRole = (player, key) => hasRole(player, key) || (shapeshifterState.playerId === player?.id && roleAliases[key]?.includes(shapeshifterState.originalRole))
const isNightmareDisabled = player => Boolean(player && nightState.nightmareTargetId === player.id)
const swapNightTargetId = targetId => {
  const [first, second] = nightState.magicianSwapIds || []
  if (!targetId || !first || !second) return targetId
  if (targetId === first) return second
  if (targetId === second) return first
  return targetId
}
const roleForSeer = player => {
  if (hasRole(player, 'wolfSister') && wolfBrotherState.brotherId && players.value.find(p => p.id === wolfBrotherState.brotherId)?.isAlive && !wolfBrotherState.awakened) return '好人'
  if (hasRole(player, 'mechanicalWolf') || isWolfRole(player)) return '狼人'
  return '好人'
}

const gameSnapshotStorageKey = `werewolf:game-state:${roomId}`
const finalizedSnapshotStorageKey = `${gameSnapshotStorageKey}:finalized`
const snapshotClone = value => JSON.parse(JSON.stringify(value))
const clearAndAssignReactive = (target, source) => {
  Object.keys(target).forEach(key => delete target[key])
  Object.assign(target, source || {})
}
const buildGameSnapshot = stage => {
  lastSnapshotSavedAt = Math.max(Date.now(), lastSnapshotSavedAt + 1)
  return ({
  version: 3,
  savedAt: lastSnapshotSavedAt,
  roomId: Number(roomId),
  resumeStage: stage || resumeStage.value || 'night',
  gameStarted: gameStarted.value,
  roomInfo: snapshotClone(roomInfo.value),
  players: players.value.map(player => ({
    id: player.id,
    playerNumber: player.playerNumber,
    playerName: player.playerName,
    role: player.role,
    isAlive: player.isAlive,
    isSheriff: player.isSheriff,
    isSheriffCandidate: player.isSheriffCandidate,
    userId: player.userId,
    aiPlayerId: player.aiPlayerId,
    avatarUrl: player.avatarUrl || ''
  })),
  currentRound: currentRound.value,
  currentDay: currentDay.value,
  currentPhase: currentPhase.value,
  dialogMessages: dialogMessages.value.slice(-800),
  speechOrder: snapshotClone(speechOrder.value),
  speechIndex: speechIndex.value,
  voteHistory: snapshotClone(voteHistory.value),
  lastPublicNightReport: lastPublicNightReport.value,
  lastGuardTargetId: lastGuardTargetId.value,
  sheriffDirection: sheriffDirection.value,
  sheriffElectionDone: sheriffElectionDone.value,
  sheriffBadgeLost: sheriffBadgeLost.value,
  publicRoleClaims: snapshotClone(publicRoleClaims),
  publicMiracleClaims: snapshotClone(publicMiracleClaims),
  lastExiledPlayerId: lastExiledPlayerId.value,
  firstNightLastWordsPlayerIds: [...firstNightLastWordsPlayerIds],
  firstDayLastWordsGiven: firstDayLastWordsGiven.value,
  dayInterrupted: dayInterrupted.value,
  silencedPlayerId: silencedPlayerId.value,
  bonusDayPending: bonusDayPending.value,
  bonusNightPending: bonusNightPending.value,
  miracleMerchantState: snapshotClone(miracleMerchantState),
  wolfBeautyState: snapshotClone(wolfBeautyState),
  stalkerState: snapshotClone(stalkerState),
  silencerState: snapshotClone(silencerState),
  nightmareState: snapshotClone(nightmareState),
  magicianState: snapshotClone(magicianState),
  cupidState: snapshotClone(cupidState),
  wolfBrotherState: snapshotClone(wolfBrotherState),
  janusState: snapshotClone(janusState),
  mechanicalWolfState: snapshotClone(mechanicalWolfState),
  dreamerState: snapshotClone(dreamerState),
  evilKnightState: snapshotClone(evilKnightState),
  shapeshifterState: snapshotClone(shapeshifterState),
  cursedFoxState: snapshotClone(cursedFoxState),
  witchInventory: snapshotClone(witchInventory),
  nightState: snapshotClone(nightState),
  playerMemories: snapshotClone(playerMemories),
  playerVoiceConfigs: snapshotClone(playerVoiceConfigs),
  idiotFlippedIds: [...idiotFlippedIds],
  bomberSuppressedIds: [...bomberSuppressedIds],
  specialDeathProcessedIds: [...specialDeathProcessedIds],
  hunterSkillUsed: [...hunterSkillUsed],
  wolfKingSkillUsed: [...wolfKingSkillUsed],
  knightSkillUsed: [...knightSkillUsed],
  showThinking: showThinking.value,
  typewriterSpeed: typewriterSpeed.value,
  currentViewMode: currentViewMode.value,
  selectedPlayerId: selectedPlayerId.value,
  gameResult: snapshotClone(gameResult.value)
  })
}
const persistGameSnapshot = (stage = resumeStage.value) => {
  // 仅房主写入本地与服务端快照；成员视角以服务端投影为准，避免完整状态回写或泄漏
  if (!players.value.length || !gameStarted.value || !isRoomOwner.value) return
  const snapshot = buildGameSnapshot(stage)
  const serialized = JSON.stringify(snapshot)
  localStorage.setItem(gameSnapshotStorageKey, serialized)
  // 语音配置仅属于本机，不随快照上传服务端
  const remotePayload = { ...snapshot }
  delete remotePayload.playerVoiceConfigs
  snapshotWriteQueue = snapshotWriteQueue
    .catch(() => {})
    .then(() => axios.put('/game/state', { roomId: Number(roomId), savedAt: snapshot.savedAt, stateJson: JSON.stringify(remotePayload) }))
    .catch(() => {})
  return snapshotWriteQueue
}
const readRemoteGameSnapshot = async () => {
  try {
    const params = {}
    if (!isRoomOwner.value) {
      const viewer = players.value.find(player => Number(player.userId) === Number(userStore.userInfo?.id))
      if (viewer) params.viewerId = viewer.id
    }
    const response = await axios.get(`/game/state/${roomId}`, { params })
    const stateJson = response.data?.code === 200 ? response.data.data?.stateJson : null
    return stateJson ? JSON.parse(stateJson) : null
  } catch {
    return null
  }
}
const restoreGameSnapshot = async () => {
  const remoteSnapshot = await readRemoteGameSnapshot()
  let localSnapshot = null
  try { localSnapshot = JSON.parse(localStorage.getItem(gameSnapshotStorageKey) || 'null') } catch { localSnapshot = null }
  let snapshot = null
  if (isRoomOwner.value) {
    snapshot = [localSnapshot, remoteSnapshot]
      .filter(item => item && Number(item.roomId) === Number(roomId) && Array.isArray(item.players))
      .sort((a, b) => Number(b.savedAt || 0) - Number(a.savedAt || 0))[0]
  } else {
    // 非房主只信任服务端按本人视角投影的结果，本地若残留完整快照立即清除
    localStorage.removeItem(gameSnapshotStorageKey)
    snapshot = remoteSnapshot && Number(remoteSnapshot.roomId) === Number(roomId) && Array.isArray(remoteSnapshot.players)
      ? remoteSnapshot
      : null
  }
  if (!snapshot || snapshot.players.length !== players.value.length) {
    if (Number(roomInfo.value.status) === 3) {
      localStorage.removeItem(gameSnapshotStorageKey)
      if (isRoomOwner.value) { try { await axios.delete(`/game/state/${roomId}`) } catch {} }
    }
    return false
  }
  if (snapshot.gameResult?.winner) {
    if (!isRoomOwner.value) {
      localStorage.removeItem(gameSnapshotStorageKey)
      roomInfo.value = { ...roomInfo.value, status: 3, winner: snapshot.gameResult.winner }
      return false
    }
    if (localStorage.getItem(finalizedSnapshotStorageKey) === String(snapshot.savedAt || '')) {
      localStorage.removeItem(gameSnapshotStorageKey)
      if (isRoomOwner.value) { try { await axios.delete(`/game/state/${roomId}`) } catch {} }
      roomInfo.value = { ...roomInfo.value, status: 3, winner: snapshot.gameResult.winner }
      return false
    }
    localStorage.setItem(finalizedSnapshotStorageKey, String(snapshot.savedAt || Date.now()))
    const result = snapshot.gameResult
    const finishedDate = new Date(result.finishedAt || Date.now())
    const finishedAt = Number.isNaN(finishedDate.getTime()) ? new Date().toISOString() : finishedDate.toISOString()
    const actionContent = JSON.stringify({
      winner: result.winner,
      board: snapshot.roomInfo?.gameBoard || boardRules.value.key,
      playerCount: snapshot.roomInfo?.playerCount || snapshot.players.length,
      day: snapshot.currentDay || 1,
      round: snapshot.currentRound || 1,
      startedAt: snapshot.roomInfo?.startTime || null,
      finishedAt,
      players: snapshot.players.map(player => ({ number: player.playerNumber, name: player.playerName, role: player.role, alive: player.isAlive })),
      publicMessages: (snapshot.dialogMessages || [])
        .filter(item => (item.visibility || 'public') === 'public')
        .slice(-40)
        .map(item => ({ sender: item.sender, content: String(item.content || '').slice(0, 240), time: item.time, type: item.type }))
    })
    try {
      await axios.post('/game/record/finish', {
        roomId: Number(roomId),
        dayNumber: snapshot.currentDay || 1,
        phase: 'finished',
        actionType: 'game_end',
        actionContent,
        winner: result.winner
      })
    } catch {
      try { await axios.put('/game/room/end', { roomId: Number(roomId), winner: result.winner }) } catch {}
    }
    localStorage.removeItem(gameSnapshotStorageKey)
    if (isRoomOwner.value) { try { await axios.delete(`/game/state/${roomId}`) } catch {} }
    roomInfo.value = { ...roomInfo.value, status: 3, winner: result.winner, endTime: finishedAt }
    return false
  }
  if (Number(roomInfo.value.status) === 3) {
    localStorage.removeItem(gameSnapshotStorageKey)
    if (isRoomOwner.value) { try { await axios.delete(`/game/state/${roomId}`) } catch {} }
    return false
  }
  lastSnapshotSavedAt = Number(snapshot.savedAt || 0)
  const currentIds = new Set(players.value.map(player => String(player.id)))
  if (snapshot.players.some(player => !currentIds.has(String(player.id)))) return false

  const savedPlayers = new Map(snapshot.players.map(player => [String(player.id), player]))
  players.value = players.value.map(player => ({ ...player, ...savedPlayers.get(String(player.id)), avatarUrl: player.avatarUrl || savedPlayers.get(String(player.id))?.avatarUrl || '' }))
  Object.assign(roomInfo.value, snapshot.roomInfo || {})
  currentRound.value = Number(snapshot.currentRound || 1)
  currentDay.value = Number(snapshot.currentDay || 1)
  currentPhase.value = snapshot.currentPhase || 'night'
  gameStarted.value = Boolean(snapshot.gameStarted)
  dialogMessages.value = Array.isArray(snapshot.dialogMessages)
    ? snapshot.dialogMessages.map(message => ({ ...message, visibility: isRoleNotice(message) ? 'role' : (message.visibility || 'public'), hiddenDuringSpeech: false }))
    : []
  speechOrder.value = Array.isArray(snapshot.speechOrder) ? snapshot.speechOrder : []
  speechIndex.value = Number.isInteger(snapshot.speechIndex) ? snapshot.speechIndex : -1
  voteHistory.value = Array.isArray(snapshot.voteHistory) ? snapshot.voteHistory : []
  lastPublicNightReport.value = snapshot.lastPublicNightReport || ''
  lastGuardTargetId.value = snapshot.lastGuardTargetId || null
  sheriffDirection.value = snapshot.sheriffDirection || 'clockwise'
  sheriffElectionDone.value = Boolean(snapshot.sheriffElectionDone)
  sheriffBadgeLost.value = Boolean(snapshot.sheriffBadgeLost)
  clearAndAssignReactive(publicRoleClaims, snapshot.publicRoleClaims)
  clearAndAssignReactive(publicMiracleClaims, snapshot.publicMiracleClaims)
  lastExiledPlayerId.value = snapshot.lastExiledPlayerId || null
  firstNightLastWordsPlayerIds.clear()
  const legacyMessages = Array.isArray(snapshot.dialogMessages) ? snapshot.dialogMessages : []
  const savedNightLastWords = Array.isArray(snapshot.firstNightLastWordsPlayerIds)
    ? snapshot.firstNightLastWordsPlayerIds
    : legacyMessages.filter(message => /首夜出局玩家，请发表遗言/.test(message.content || '')).map(message => {
        const number = Number(String(message.content).match(/(\d+)号/)?.[1])
        return players.value.find(player => Number(player.playerNumber) === number)?.id
      }).filter(Boolean)
  savedNightLastWords.forEach(id => firstNightLastWordsPlayerIds.add(id))
  firstDayLastWordsGiven.value = Boolean(snapshot.firstDayLastWordsGiven ?? legacyMessages.some(message => /被公投出局，请发表遗言/.test(message.content || '')))
  dayInterrupted.value = Boolean(snapshot.dayInterrupted)
  silencedPlayerId.value = snapshot.silencedPlayerId || null
  bonusDayPending.value = Number(snapshot.bonusDayPending || 0)
  bonusNightPending.value = Number(snapshot.bonusNightPending || 0)
  Object.assign(miracleMerchantState, snapshot.miracleMerchantState || {})
  Object.assign(wolfBeautyState, snapshot.wolfBeautyState || {})
  Object.assign(stalkerState, snapshot.stalkerState || {})
  Object.assign(silencerState, snapshot.silencerState || {})
  Object.assign(nightmareState, snapshot.nightmareState || {})
  Object.assign(magicianState, snapshot.magicianState || {})
  Object.assign(cupidState, snapshot.cupidState || {})
  Object.assign(wolfBrotherState, snapshot.wolfBrotherState || {})
  Object.assign(janusState, snapshot.janusState || {})
  Object.assign(mechanicalWolfState, snapshot.mechanicalWolfState || {})
  Object.assign(dreamerState, snapshot.dreamerState || {})
  Object.assign(evilKnightState, snapshot.evilKnightState || {})
  Object.assign(shapeshifterState, snapshot.shapeshifterState || {})
  Object.assign(cursedFoxState, snapshot.cursedFoxState || {})
  if (snapshot.witchInventory) Object.assign(witchInventory, snapshot.witchInventory)
  Object.assign(nightState, createEmptyNightState(), snapshot.nightState || {})
  clearAndAssignReactive(playerMemories, snapshot.playerMemories)
  Object.values(playerMemories).forEach(memory => {
    if (Array.isArray(memory?.privateKnowledge)) {
      memory.privateKnowledge = memory.privateKnowledge.filter(item => !/主持人已确认.*交易失败|privately confirmed.*transaction failed/i.test(String(item)))
    }
  })
  if (snapshot.playerVoiceConfigs) {
    clearAndAssignReactive(playerVoiceConfigs, snapshot.playerVoiceConfigs)
    savePlayerVoiceConfigs()
  }
  idiotFlippedIds.clear(); (snapshot.idiotFlippedIds || []).forEach(id => idiotFlippedIds.add(id))
  bomberSuppressedIds.clear(); (snapshot.bomberSuppressedIds || []).forEach(id => bomberSuppressedIds.add(id))
  specialDeathProcessedIds.clear(); (snapshot.specialDeathProcessedIds || []).forEach(id => specialDeathProcessedIds.add(id))
  hunterSkillUsed = new Set(snapshot.hunterSkillUsed || [])
  wolfKingSkillUsed = new Set(snapshot.wolfKingSkillUsed || [])
  knightSkillUsed = new Set(snapshot.knightSkillUsed || [])
  showThinking.value = snapshot.showThinking !== false
  typewriterSpeed.value = snapshot.typewriterSpeed || typewriterSpeed.value
  currentViewMode.value = snapshot.currentViewMode || 'god'
  selectedPlayerId.value = snapshot.selectedPlayerId || players.value[0]?.id || null
  if (!isRoomOwner.value) {
    const viewer = players.value.find(player => Number(player.userId) === Number(userStore.userInfo?.id))
    currentViewMode.value = 'player'
    selectedPlayerId.value = viewer?.id || players.value[0]?.id || null
  }
  gameResult.value = snapshot.gameResult || null
  resumeStage.value = snapshot.resumeStage || 'night'
  roleDealVisible.value = false
  aiThinkingPlayers.value = []
  aiSpeakingContent.value = null
  players.value.forEach(player => { player.isSpeaking = false })
  return true
}

// Game setup
const startGame = async () => {
  const required = roomInfo.value.playerCount || 12
  if (players.value.length < required) { ElMessage.warning($t('gamePlay.notEnoughPlayers', { count: required })); return }
  if (finalizationPromise) await finalizationPromise
  await loadGameData({ restore: false })
  initializeGameState()
  await playRoleDealAnimation(distributeRoles)
  players.value.forEach(p => { if (p.aiPlayerId) p.userId = -1 })
  await startGamePhase()
  gameStarted.value = true
  localStorage.removeItem(finalizedSnapshotStorageKey)
  notifyPlayerRoles()
  resumeStage.value = 'night'
  persistGameSnapshot('night')
  const version = ++gameLoopVersion
  void runGameLoop(version, 'night')
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
    addGameMessage({ sender: $t('gamePlay.referee'), content, type: 'referee', visibility: 'role', privateFor: p.id })
  })
}
const exitGame = () => {
  ElMessageBox.confirm($t('gamePlay.confirmExit'), $t('gamePlay.exitGameTitle'), { confirmButtonText: $t('common.confirm'), cancelButtonText: $t('common.cancel'), type: 'warning' })
    .then(() => {
      gameLoopVersion++
      phaseRunning.value = false
      isGamePaused.value = false
      releasePauseWaiters()
      closeSpeech('exit')
      stopSpeaking()
      persistGameSnapshot(resumeStage.value)
      gameStarted.value = false
      router.push(`/game/room/detail/${roomId}`)
    }).catch(() => {})
}
const handlePositionClick = (pos) => {
  if (pos.locked) return
  if (pos.player) {
    if (gameStarted.value) {
      if (currentViewMode.value === 'player') selectedPlayerId.value = pos.player.id
      return
    }
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
const loadGameData = async (options = {}) => {
  try {
    const rr = await axios.get(`/game/room/info/${roomId}`)
    let room = rr.data.code === 200 ? rr.data.data : rr.data
    if (room) {
      roomInfo.value = room
      try {
        const boardKey = room.gameBoard || route.query.gameBoard
        if (boardKey) {
          const br = await axios.get(`/game/board/${boardKey}`)
          const payload = br.data?.code === 200 ? br.data.data : null
          if (payload?.board && Array.isArray(payload.board.roles)) {
            boardConfigOverrides.value = payload.board
            const roomVersion = String(room.boardVersion || payload.schemaVersion)
            if (String(payload.schemaVersion) !== roomVersion) {
              ElMessage.warning(currentLocale() === 'zh-CN'
                ? '板子配置版本已更新，与创建房间时不一致，请留意规则变化。'
                : 'Board config version changed since this room was created.')
            }
          }
        }
      } catch {}
      try {
        const pr = await axios.get(`/game/player/list/${roomId}`)
        let pl = pr.data.code === 200 ? pr.data.data : (Array.isArray(pr.data) ? pr.data : [])
        players.value = pl.map(p => ({ id: p.id, playerNumber: p.playerNumber || p.id, playerName: p.playerName || p.name || `P${p.id}`, role: p.role || '', isAlive: p.status === 1, isSpeaking: false, isSheriff: Number(roomInfo.value.status) !== 3 && p.isSheriff === 1, isSheriffCandidate: false, userId: p.userId, aiPlayerId: p.aiPlayerId, avatarUrl: '' }))
      } catch (e) { players.value = [] }
      try {
        const ar = await axios.get('/ai/player/available')
        aiPlayers.value = ar.data.code === 200 ? ar.data.data : (Array.isArray(ar.data) ? ar.data : [])
        const aiById = new Map(aiPlayers.value.map(ai => [Number(ai.id), ai]))
        players.value.forEach(player => { player.avatarUrl = aiById.get(Number(player.aiPlayerId))?.avatarUrl || '' })
      } catch (e) {}
    }
    if (players.value.length) selectedPlayerId.value = players.value[0].id
    if (options.restore && await restoreGameSnapshot()) return true
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
    return false
  } catch (e) { roomInfo.value = { playerCount: 12 }; players.value = []; return false }
}
const distributeRoles = async () => {
  try {
    const bc = await loadBoardConfig(); const roles = []; bc.roles.forEach(rc => { for (let i = 0; i < rc.count; i++) roles.push(rc.role) })
    shuffleArray(roles); players.value.forEach((p, i) => { p.role = i < roles.length ? roles[i] : '平民' })
    if (isBoard('shapeshifter_wolfking')) {
      const candidates = players.value.filter(player => ['预言家', '女巫', '猎人', '摄梦人', '熊'].includes(player.role))
      const chosen = randomItem(candidates)
      if (chosen) {
        shapeshifterState.playerId = chosen.id
        shapeshifterState.originalRole = chosen.role
        chosen.role = '百变狼王'
        playerMemories[chosen.id]?.privateKnowledge.push(`你是百变狼王，本局复制的变异能力来自${shapeshifterState.originalRole}。`)
        addRefereeMessage(`百变狼王已随机变异为${getRoleName(shapeshifterState.originalRole)}能力；真实身份仍属于狼人阵营。`, { visibility: 'god' })
      }
    }
    const brother = players.value.find(player => hasRole(player, 'wolfBrother'))
    const sister = players.value.find(player => hasRole(player, 'wolfSister'))
    wolfBrotherState.brotherId = brother?.id || null
    wolfBrotherState.sisterId = sister?.id || null
    const fox = players.value.find(player => hasRole(player, 'cursedFox'))
    cursedFoxState.playerId = fox?.id || null
    addRefereeMessage($t('gamePlay.rolesDistributed', { roles: bc.roles.map(r => `${getRoleName(r.role)}×${r.count}`).join('、') }))
  } catch (e) {}
}
const playRoleDealAnimation = async dealRoles => {
  roleDealVisible.value = true
  roleDealStage.value = 'shuffle'
  roleDealMessage.value = currentLocale() === 'zh-CN' ? '身份牌洗牌中' : 'Shuffling identity cards'
  roleDealProgress.value = `0 / ${players.value.length}`
  await delay(850)
  roleDealStage.value = 'spread'
  roleDealMessage.value = currentLocale() === 'zh-CN' ? '正在为每位玩家抽取身份' : 'Drawing one identity per player'
  roleDealProgress.value = `${players.value.length} ${currentLocale() === 'zh-CN' ? '张身份牌' : 'identity cards'}`
  await delay(1050)
  await dealRoles()
  roleDealStage.value = 'complete'
  roleDealMessage.value = currentLocale() === 'zh-CN' ? '身份已私密送达' : 'Identities delivered privately'
  roleDealProgress.value = `${players.value.length} / ${players.value.length}`
  await delay(750)
  roleDealVisible.value = false
}
const loadBoardConfig = async () => {
  return boardRules.value
}
const shuffleArray = (a) => { for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [a[i], a[j]] = [a[j], a[i]] } return a }
const startGamePhase = async () => {
  currentPhase.value = 'night'
  const response = await axios.put('/game/room/start', { roomId: Number(roomId) })
  if (response.data?.code !== 200) throw new Error(response.data?.message || '房间状态更新失败')
  roomInfo.value = { ...roomInfo.value, status: 2, startTime: new Date().toISOString() }
}

const sendMessage = () => {
  if (!inputMessage.value.trim() || !canSpeak.value || isGamePaused.value) return
  const cp = players.value.find(p => p.id === selectedPlayerId.value)
  const name = cp ? cp.playerName : ($locale==='zh-CN'?'我':'Me')
  let type = 'player'
  if (currentPhase.value === 'night' && cp && isPackWolf(cp)) type = 'wolf'
  addGameMessage({ sender: name, content: inputMessage.value.trim(), type, visibility: type === 'wolf' ? 'wolves' : 'public' })
  inputMessage.value = ''; scrollToBottom()
}
const addGameMessage = ({ sender, content, type = 'player', visibility = 'public', privateFor = null, detail = '', hiddenDuringSpeech = false }) => {
  const message = { sender, content, time: new Date().toLocaleTimeString(), type, visibility, privateFor, detail, hiddenDuringSpeech }
  dialogMessages.value.push(message)
  scrollToBottom()
  return message
}
const addRefereeMessage = (content, options = {}) => {
  const visibility = options.visibility || 'public'
  addGameMessage({
    sender: $t('gamePlay.referee'), content, type: 'referee', visibility,
    privateFor: options.privateFor || null, detail: options.detail || ''
  })
  if (visibility === 'public' && options.voice !== false) {
    const task = refereeSpeechBarrier
      .catch(() => false)
      .then(() => speakText(content, { speaker: 'referee', lang: currentLocale() }))
    refereeSpeechBarrier = task.catch(error => {
      console.warn('Referee speech failed:', error)
      return false
    })
    return refereeSpeechBarrier
  }
  return Promise.resolve(false)
}
const waitForRefereeSpeech = () => refereeSpeechBarrier.catch(() => false)
const addDialogMessage = (sender, content, type = 'player', options = {}) => addGameMessage({
  sender, content, type, visibility: options.visibility || 'public', privateFor: options.privateFor || null, detail: options.detail || '', hiddenDuringSpeech: Boolean(options.hiddenDuringSpeech)
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
    else pauseSpeaking()
    addRefereeMessage(currentLocale() === 'zh-CN' ? '游戏已暂停，所有阶段计时与流程推进均已冻结。' : 'Game paused. All timers and phase progression are frozen.', { voice: false })
    return
  }

  isGamePaused.value = false
  if (speechPausedByGame) resumeSpeechPlayback()
  else resumeSpeaking()
  speechPausedByGame = false
  releasePauseWaiters()
  addRefereeMessage(currentLocale() === 'zh-CN' ? '游戏继续。' : 'Game resumed.', { voice: false })
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
  stopSpeaking({ clearQueue: false })
  if (activeSpeechTimer) clearInterval(activeSpeechTimer)
  if (activeAutoPassTimer) clearTimeout(activeAutoPassTimer)
  activeSpeechTimer = null
  activeAutoPassTimer = null
  activeTypewriterCompleteHandler = null
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
  stopSpeaking({ clearQueue: false })
  speechPaused.value = false
  activeTypewriterCompleteHandler?.()
}
const passSpeech = () => {
  if (isTyping.value || speechPaused.value || isGamePaused.value) return
  closeSpeech('pass')
}

const startPlayerSpeaking = async (pid, announce = true) => {
  if (!gameStarted.value) return
  players.value.forEach(p => { p.isSpeaking = p.id === pid })
  speakingPlayer.value = players.value.find(p => p.id === pid)
  if (speakingPlayer.value && announce) {
    await addRefereeMessage(currentLocale() === 'zh-CN'
      ? `请${speakingPlayer.value.playerNumber}号${speakingPlayer.value.playerName}发言。`
      : `Player ${speakingPlayer.value.playerNumber}, ${speakingPlayer.value.playerName}, please speak.`)
  }
}
const endPlayerSpeaking = async () => {
  players.value.forEach(p => { p.isSpeaking = false })
  if (speakingPlayer.value) await addRefereeMessage(currentLocale() === 'zh-CN'
    ? `${speakingPlayer.value.playerNumber}号发言结束。`
    : `Player ${speakingPlayer.value.playerNumber} has finished speaking.`)
  speakingPlayer.value = null
}
const setSheriff = (pid) => {
  players.value.forEach(p => { p.isSheriff = p.id === pid; p.isSheriffCandidate = false })
}
const killPlayer = (pid) => {
  const p = players.value.find(x => x.id === pid)
  if (p?.isAlive) { p.isAlive = false; p.isSpeaking = false }
}
const finishGame = (winner, message) => {
  if (gameResult.value || finalizationPromise) return true
  const finishedAt = new Date()
  gameResult.value = { winner, finishedAt: finishedAt.getTime() }
  gameStarted.value = false
  phaseRunning.value = false
  gameLoopVersion++
  isGamePaused.value = false
  releasePauseWaiters()
  closeSpeech('game-end')
  const speechTask = addRefereeMessage(message)
  localStorage.removeItem(gameSnapshotStorageKey)
  roomInfo.value = { ...roomInfo.value, status: 3, winner, endTime: finishedAt.toISOString() }
  const pendingSnapshotWrites = snapshotWriteQueue.catch(() => {})
  const actionContent = JSON.stringify({
    winner,
    board: roomInfo.value.gameBoard || boardRules.value.key,
    playerCount: roomInfo.value.playerCount || players.value.length,
    day: currentDay.value,
    round: currentRound.value,
    startedAt: roomInfo.value.startTime || null,
    finishedAt: finishedAt.toISOString(),
    players: players.value.map(player => ({ number: player.playerNumber, name: player.playerName, role: player.role, alive: player.isAlive })),
    publicMessages: dialogMessages.value
      .filter(item => (item.visibility || 'public') === 'public')
      .slice(-40)
      .map(item => ({ sender: item.sender, content: String(item.content || '').slice(0, 240), time: item.time, type: item.type }))
  })
  finalizationPromise = (async () => {
    const recordTask = (async () => {
      try {
        await axios.post('/game/record/finish', {
          roomId: Number(roomId),
          dayNumber: currentDay.value,
          phase: 'finished',
          actionType: 'game_end',
          actionContent,
          winner
        })
      } catch (error) {
        console.warn('Game record finalization failed:', error)
        try {
          await axios.put('/game/room/end', { roomId: Number(roomId), winner })
        } catch {}
      }
    })()
    const snapshotCleanupTask = (async () => {
      await pendingSnapshotWrites
      try { await axios.delete(`/game/state/${roomId}`) } catch {}
    })()
    await Promise.allSettled([speechTask, recordTask, snapshotCleanupTask])
  })().catch(error => console.warn('Game finalization failed:', error))
  return true
}
const checkGameEnd = () => {
  const alive = alivePlayers.value
  const livingLovers = cupidState.pairIds.filter(id => alive.some(player => player.id === id))
  if (cupidState.mixed && livingLovers.length === 2 && alive.length === 2) {
    return finishGame('情侣第三方', '人狼情侣存活到最后，第三方阵营获胜。')
  }
  const livingFox = alive.find(player => hasRole(player, 'cursedFox'))
  if (livingFox && alive.length === 1) {
    return finishGame('咒狐', `${livingFox.playerNumber}号咒狐独自存活到最后，咒狐获胜。`)
  }
  const aw = alive.filter(isWolfRole).length
  const villagers = alive.filter(player => ['平民', 'Villager'].includes(player.role)).length
  const gods = alive.filter(player => !isWolfRole(player) && !['平民', 'Villager'].includes(player.role)).length
  if (isBoard('black_death')) {
    if (alive.length <= 2) return finishGame('好人', '黑死病无狼局结束：仅剩两名玩家，主持人公布本局实际没有狼人。')
    return false
  }
  if (aw === 0) return finishGame('好人', $t('gamePlay.goodWin'))
  if (villagers === 0 || gods === 0) return finishGame('狼人', $t('gamePlay.wolfWin'))
  if (!alivePlayers.value.length) return finishGame('平局', $t('gamePlay.allDead'))
  return false
}

const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))
const phaseDelay = async (ms = 500) => {
  await waitForRefereeSpeech()
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
  gameResult.value = null
  finalizationPromise = null
  isGamePaused.value = false
  decisionWindow.active = false
  decisionWindow.remaining = 0
  decisionWindow.completed = 0
  decisionWindow.total = 0
  decisionWindow.label = ''
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
  sheriffBadgeLost.value = false
  lastExiledPlayerId.value = null
  firstNightLastWordsPlayerIds.clear()
  firstDayLastWordsGiven.value = false
  dayInterrupted.value = false
  resumeStage.value = 'night'
  silencedPlayerId.value = null
  bonusDayPending.value = 0
  bonusNightPending.value = 0
  idiotFlippedIds.clear()
  bomberSuppressedIds.clear()
  specialDeathProcessedIds.clear()
  Object.assign(stalkerState, { used: false, pendingTargetId: null })
  Object.assign(silencerState, { previousTargetId: null })
  Object.assign(nightmareState, { previousTargetId: null })
  Object.assign(magicianState, { previousPair: '' })
  Object.assign(cupidState, { used: false, pairIds: [], resolving: false, mixed: false })
  Object.assign(wolfBrotherState, { brotherId: null, sisterId: null, awakened: false, awakenRound: null })
  Object.assign(janusState, { used: false, playerId: null, chosenRole: null })
  Object.assign(mechanicalWolfState, { learnedRole: null, playerId: null })
  Object.assign(dreamerState, { previousTargetId: null, currentTargetId: null })
  Object.assign(evilKnightState, { seerReflected: false, witchReflected: false })
  Object.assign(shapeshifterState, { playerId: null, originalRole: null })
  Object.assign(cursedFoxState, { playerId: null, killedBySeer: false })
  Object.assign(miracleMerchantState, { used: false, merchantId: null, luckyId: null, skill: null, pendingDeath: false, transactionFailed: false, giftResolved: false })
  Object.assign(wolfBeautyState, { previousTargetId: null, targetId: null })
  witchInventory.antidote = 1
  witchInventory.poison = 1
  Object.assign(nightState, createEmptyNightState())
  Object.keys(playerMemories).forEach(key => delete playerMemories[key])
  Object.keys(publicRoleClaims).forEach(key => delete publicRoleClaims[key])
  Object.keys(publicMiracleClaims).forEach(key => delete publicMiracleClaims[key])
  players.value.forEach(player => {
    player.isAlive = true
    player.isSpeaking = false
    player.isSheriff = false
    player.isSheriffCandidate = false
    playerMemories[player.id] = { privateKnowledge: [], speeches: [], votes: [], checks: [], observations: [], miracleGift: null }
  })
}

const resetNightState = () => {
  Object.assign(nightState, createEmptyNightState(), { previousGuardTargetId: lastGuardTargetId.value })
}

const promptSituationSummary = (language = currentLocale()) => {
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
      config.apiKey = cleanApiKey(getAiPlayerKey(player.aiPlayerId) || getGlobalApiKey())
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

const normalizeApiUrl = (baseUrl, modelType) => {
  let url = (baseUrl || (isAnthropicModel(modelType) ? 'https://api.anthropic.com/v1' : 'https://api.deepseek.com/v1')).trim().replace(/\/+$/, '')
  if (!/^https?:\/\//i.test(url)) url = `https://${url}`
  if (isAnthropicModel(modelType)) {
    if (/\/messages$/.test(url)) return url
    return url.endsWith('/v1') ? `${url}/messages` : `${url}/v1/messages`
  }
  if (!/\/chat\/completions$/.test(url)) {
    const hasPath = /\/[^/]+$/.test(url.replace(/^https?:\/\/[^/]+/, ''))
    url += `${hasPath ? '' : '/v1'}/chat/completions`
  }
  return url
}

const cleanApiKey = apiKey => (apiKey || '').replace(/`/g, '').trim().replace(/^['"]|['"]$/g, '')

const isAnthropicModel = modelType => ['claude', 'anthropic'].includes(String(modelType || '').toLowerCase())

const providerKeyOf = config => {
  const type = String(config?.modelType || '').toLowerCase() || 'openai-compatible'
  const base = String(config?.apiBaseUrl || '').trim().replace(/\/+$/, '').toLowerCase() || 'default'
  return `${type}::${base}`
}

const acquireAiRequestSlot = async providerKey => {
  let gate = aiProviderGates.get(providerKey)
  if (!gate) {
    gate = { queue: [], inFlight: 0, lastStartAt: 0 }
    aiProviderGates.set(providerKey, gate)
  }
  if (gate.inFlight >= AI_MAX_CONCURRENCY_PER_PROVIDER) {
    await new Promise(resolve => gate.queue.push(resolve))
  }
  gate.inFlight += 1
  const health = aiProviderHealth.get(providerKey)
  let cooldownMs = 0
  if (health && health.consecutiveFailures >= AI_PROVIDER_COOLDOWN_AFTER_FAILURES) {
    cooldownMs = Math.min(AI_PROVIDER_COOLDOWN_MAX_MS, AI_PROVIDER_COOLDOWN_BASE_MS * (2 ** (health.consecutiveFailures - AI_PROVIDER_COOLDOWN_AFTER_FAILURES)))
  }
  const waitMs = Math.max(cooldownMs, API_MIN_START_INTERVAL_PER_PROVIDER - (Date.now() - gate.lastStartAt))
  if (waitMs > 0) await delay(waitMs)
  gate.lastStartAt = Date.now()
  return () => {
    gate.inFlight = Math.max(0, gate.inFlight - 1)
    const next = gate.queue.shift()
    if (next) next()
  }
}

const recordAiProviderResult = (providerKey, ok) => {
  let health = aiProviderHealth.get(providerKey)
  if (!health) {
    health = { consecutiveFailures: 0, successes: 0, failures: 0, lastFailureAt: 0 }
    aiProviderHealth.set(providerKey, health)
  }
  if (ok) {
    health.consecutiveFailures = 0
    health.successes += 1
  } else {
    health.consecutiveFailures += 1
    health.failures += 1
    health.lastFailureAt = Date.now()
  }
}

const reportAiRequestFailure = (error, providerKey = '') => {
  const now = Date.now()
  const status = error?.status ? `HTTP ${error.status}` : (error?.name === 'AbortError' ? '请求超时' : error?.message || '网络错误')
  aiRequestState.failed += 1
  aiRequestState.lastStatus = status
  aiRequestState.lastError = status
  aiRequestState.lastAt = now
  if (now - lastAiFailureNoticeAt < 10000) return
  lastAiFailureNoticeAt = now
  addRefereeMessage('模型服务暂时不可用，已重试并使用本地逻辑继续。', { visibility: 'god', detail: `${status}${providerKey ? `（供应商：${providerKey}）` : ''}。503通常表示上游服务繁忙或临时不可用，并非游戏流程错误。` })
}

const callStructuredAi = async (config, systemPrompt, userPrompt) => {
  if (!config?.apiKey) return null
  const modelType = String(config.modelType || '').toLowerCase()
  const isAnthropic = isAnthropicModel(modelType)
  const providerKey = providerKeyOf(config)
  aiRequestState.pending += 1
  const temperature = typeof config.temperature === 'number'
    ? (config.temperature > 2 ? config.temperature / 10 : config.temperature)
    : 0.7
  const maxTokens = Math.min(2200, Math.max(900, Number(config.maxTokens) || 1000))
  const apiKey = cleanApiKey(config.apiKey)
  const buildRequestBody = () => isAnthropic
    ? {
        model: config.modelName || 'claude-3-5-sonnet-latest',
        max_tokens: maxTokens,
        temperature,
        system: systemPrompt,
        messages: [{ role: 'user', content: userPrompt }]
      }
    : {
        model: config.modelName || 'deepseek-chat',
        messages: [{ role: 'system', content: systemPrompt }, { role: 'user', content: userPrompt }],
        temperature,
        max_tokens: maxTokens,
        response_format: { type: 'json_object' }
      }
  const buildHeaders = () => {
    const headers = { 'Content-Type': 'application/json' }
    if (isAnthropic) {
      headers['x-api-key'] = apiKey
      headers['anthropic-version'] = '2023-06-01'
    } else {
      headers.Authorization = `Bearer ${apiKey}`
    }
    return headers
  }
  const extractMessage = data => {
    if (isAnthropic) {
      const blocks = data?.content
      let text = ''
      let thinking = ''
      if (Array.isArray(blocks)) {
        for (const block of blocks) {
          if (typeof block === 'string') text += block
          else if (block?.type === 'text') text += block.text || ''
          else if (block?.type === 'thinking') thinking += block.thinking || ''
        }
      } else if (typeof blocks === 'string') text = blocks
      return { text, thinking }
    }
    const message = data.choices?.[0]?.message || data.output?.choices?.[0]?.message || {}
    return { text: message.content, thinking: message.reasoning_content || '' }
  }
  let lastError = null
  try {
    for (let attempt = 1; attempt <= AI_MAX_ATTEMPTS; attempt++) {
      await waitWhilePaused()
      const controller = new AbortController()
      const timeout = setTimeout(() => controller.abort(), 45000)
      let releaseSlot = null
      try {
        releaseSlot = await acquireAiRequestSlot(providerKey)
        aiRequestState.inFlight += 1
        const response = await fetch(normalizeApiUrl(config.apiBaseUrl, modelType), {
          method: 'POST',
          headers: buildHeaders(),
          body: JSON.stringify(buildRequestBody()),
          signal: controller.signal
        })
        if (!response.ok) {
          const error = new Error(`HTTP ${response.status}`)
          error.status = response.status
          error.retryAfter = response.headers.get('retry-after') || ''
          throw error
        }
        const data = await response.json()
        const { text, thinking } = extractMessage(data)
        const parsed = parseStructuredResponse(text)
        if (parsed && !parsed.thinking && thinking) parsed.thinking = thinking
        if (!parsed) throw new Error('Invalid response format')
        recordAiProviderResult(providerKey, true)
        aiRequestState.succeeded += 1
        aiRequestState.lastStatus = 'OK'
        aiRequestState.lastError = ''
        aiRequestState.lastAt = Date.now()
        return parsed
      } catch (error) {
        lastError = error
        recordAiProviderResult(providerKey, false)
        const retryable = AI_RETRYABLE_STATUSES.has(error?.status) || error?.name === 'AbortError' || error?.name === 'TypeError'
        if (!retryable || attempt >= AI_MAX_ATTEMPTS) break
        const retryAfter = Number(error?.retryAfter)
        const backoff = Number.isFinite(retryAfter) && retryAfter > 0
          ? Math.min(8000, retryAfter * 1000)
          : Math.min(8000, 1200 * (2 ** (attempt - 1)))
        await delay(backoff + Math.floor(Math.random() * 300))
      } finally {
        if (releaseSlot) releaseSlot()
        if (aiRequestState.inFlight > 0) aiRequestState.inFlight -= 1
        clearTimeout(timeout)
      }
    }
    reportAiRequestFailure(lastError, providerKey)
    return null
  } finally {
    if (aiRequestState.pending > 0) aiRequestState.pending -= 1
  }
}

const roleVictoryCondition = (player, language) => {
  if (hasRole(player, 'cursedFox')) return language === 'en-US' ? 'Survive alone as the final living player.' : '独自存活到最后'
  if (cupidState.mixed && cupidState.pairIds.includes(player.id)) return language === 'en-US' ? 'As a mixed-alignment lover pair, eliminate every other player.' : '作为人狼情侣第三方，清除除情侣外的所有玩家'
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
  const miracleGift = playerMemories[player.id]?.miracleGift
  const giftName = language === 'en-US'
    ? ({ check: 'inspection', poison: 'poison', guard: 'protection' }[miracleMerchantState.skill] || miracleMerchantState.skill)
    : ({ check: '查验', poison: '毒药', guard: '守护' }[miracleMerchantState.skill] || miracleMerchantState.skill)
  const claimedRoles = Object.entries(publicRoleClaims).map(([id, role]) => `${getPlayerNumberById(Number(id))}号声明${role}`).join('；') || '暂无公开身份声明'
  const publicMiracleFacts = Object.values(publicMiracleClaims).map(claim => language === 'en-US'
    ? `Player ${claim.merchantNumber} publicly claimed Miracle Merchant${claim.luckyNumber ? ` and named player ${claim.luckyNumber} as recipient` : ''}${claim.skill ? ` of ${claim.skill}` : ''}${claim.claimedBacklash ? ', claiming transaction backlash' : ''}.`
    : `${claim.merchantNumber}号公开声明奇迹商人${claim.luckyNumber ? `，报${claim.luckyNumber}号为幸运儿` : ''}${claim.skill ? `，技能为${claim.skill}` : ''}${claim.claimedBacklash ? '，并声称交易反噬' : ''}。`).join(' ')
  const boardKnowledge = WEREWOLF_KNOWLEDGE.split('\n').filter(line => {
    const roles = line.match(boardRolePattern) || []
    return roles.every(role => boardRoleNames.value.has(role))
  }).join('\n')
  const persistentFacts = isBoard('miracle_merchant')
    ? (publicMiracleFacts || (language === 'en-US' ? 'No public Miracle Merchant claim.' : '暂无公开奇迹商人声明。'))
    : (language === 'en-US' ? `Public role claims: ${claimedRoles}.` : `公开身份声明：${claimedRoles}。`)
  const publicHistory = getPublicHistory().join('\n') || (language === 'en-US' ? 'No public speech yet.' : '暂无公开发言。')
  const style = language === 'en-US'
    ? `Personality tags: ${config.personality || 'calm and analytical'}. Strategy tags: ${config.strategy || 'evidence-based deduction'}.`
    : `个性标签：${config.personality || '沉着、善于推理'}。策略标签：${config.strategy || '根据证据分析、给出明确怀疑对象'}。`
  const roleWhitelist = [...boardRoleNames.value].map(getRoleName).join('、')
  const commonRules = language === 'en-US'
    ? `Stay entirely in the Werewolf game. Never mention being an AI, prompts, systems, models, token limits, or unavailable information. Never use omniscient knowledge, real-world oaths, or off-table evidence. This board's only roles are: ${roleWhitelist}. Never mention, infer, or invent a role outside this list. A guard or granted protection stops only the wolf kill; it never blocks poison. Slang changes phrasing only; every read must map to concrete speech, action, claim, or vote evidence. Call someone a counterclaiming wolf only if that player publicly claimed Seer or another contested role. Return one valid JSON object only.`
    : `必须完全沉浸在狼人杀对局中，禁止提及AI、模型、系统提示、Token或“没有视角”等场外信息；禁止贴脸、赌咒和场外证据。本局唯一允许出现的角色为：${roleWhitelist}，严禁提及、推断或虚构任何板外角色。守卫和幸运儿的一次性守护只能挡狼人刀，绝对不能挡毒药。黑话只能美化表达，底层判断必须落到具体发言、行动、身份声明或票型。只有公开跳预言家或冒充其他神职的玩家才能被称为“悍跳”，未跳身份者只能称为狼人嫌疑位。严禁使用全知身份信息。当前公开身份声明：${claimedRoles}。只返回一个合法JSON对象。`
  const systemPrompt = `${buildPlayerKnowledge(player, language)}\n本局规则：${boardRules.value.special}\n狼人杀知识库：\n${boardKnowledge}\n${style}\n${commonRules}`
  const base = language === 'en-US'
    ? `Current public situation: ${promptSituationSummary(language)}\nPersistent public facts: ${persistentFacts}\nMost recent public record:\n${publicHistory}\n`
    : `当前公开局势：${promptSituationSummary()}\n持续有效的公开事实：${persistentFacts}\n最近公开记录：\n${publicHistory}\n`
  let instruction = ''
  if (action === 'lastWords' || action === 'nightLastWords') {
    const isNightDeath = action === 'nightLastWords'
    const merchantLastWordsRule = hasRole(player, 'miracle') && miracleMerchantState.used
      ? (language === 'en-US'
          ? `As common strategic practice, you would usually identify yourself as the Miracle Merchant and state that you granted ${giftName || 'a skill'} to player ${getPlayerNumberById(miracleMerchantState.luckyId)}, but this is a recommendation rather than a forced output. The system does not tell you whether your death was caused by a wolf kill or transaction backlash, so you may only present both possibilities and ask the recipient to verify the gift.`
          : `按常见打法，你通常会在遗言中说明自己是奇迹商人，并交代昨夜把${giftName || '技能'}给了${getPlayerNumberById(miracleMerchantState.luckyId)}号，但这只是会玩的惯例，不是强制输出。系统不会告诉你自己死于狼刀还是交易反噬，你只能根据公开现象提出两种可能并让幸运儿对账，不能宣称主持人确认了死因。`)
      : ''
    instruction = language === 'en-US'
      ? `Task: You died ${isNightDeath ? 'during the night; the moderator did not publicly reveal the cause' : 'by public exile'} and this is your final statement. First write a concise private analysis, then give a 100-180 word in-character final statement. ${merchantLastWordsRule} Review only events that already happened and leave one concrete warning. You are out of the game: never say you will keep listening, vote later, update your opinion, speak next round, or take any future action. JSON schema: {"thinking":"private final analysis","speech":"public final statement"}`
      : `任务：你${isNightDeath ? '在昨夜死亡，主持人只公开死讯、没有公开死因' : '已经被公投放逐'}，现在发表最后遗言。先写简洁的私密复盘，再写160-300个中文字符的公开遗言。${merchantLastWordsRule}只复盘已经发生的夜间结果、公开发言和票型，留下一个明确警示。你已经出局，严禁说“继续听发言”“之后再投”“准备投给”“下一轮发言”“再调整判断”等任何未来行动。JSON格式：{"thinking":"私密最终复盘","speech":"公开遗言"}`
  } else if (action === 'speech' || action === 'pkSpeech' || action === 'sheriffSpeech' || action === 'sheriffPkSpeech') {
    const stageName = action === 'pkSpeech' ? '平票PK发言' : action === 'sheriffPkSpeech' ? '警徽PK发言' : action === 'sheriffSpeech' ? '警长竞选发言' : '正常白天发言'
    const specialSchema = (hasRole(player, 'whiteWolf') && action === 'speech') || (isPackWolf(player) && ['sheriffSpeech', 'sheriffPkSpeech'].includes(action))
      ? ',"explode":是否自爆,"target":自爆带走的玩家编号或null'
      : ''
    const miracleSpeechRule = miracleGift?.skill === 'check' && miracleGift.result
      ? (language === 'en-US'
          ? `You are the lucky recipient of a one-use inspection. You must publicly say that you received a mysterious gift and accurately report player ${getPlayerNumberById(miracleGift.targetId)} as ${miracleGift.result === '狼人' ? 'a wolf result' : 'a good result'}. You do not know who the Merchant is. ${miracleGift.result === '狼人' ? 'Ask the Merchant to authenticate the result and push this target.' : 'Report the good result while allowing the Merchant to remain hidden.'}`
          : `你是获得奇迹查验的幸运儿，本轮公开发言必须明确说“昨夜收到一份礼物”，并准确报出${getPlayerNumberById(miracleGift.targetId)}号是${miracleGift.result === '狼人' ? '查杀' : '金水'}；不得隐藏或篡改结果，也不知道老板是谁。${miracleGift.result === '狼人' ? '应呼唤老板公开认证并优先归票该查杀。' : '应报出金水，但可以让老板继续隐藏。'}`)
      : (hasRole(player, 'miracle') && miracleMerchantState.used
          ? (language === 'en-US'
               ? `You are the Miracle Merchant and only know that you gave ${miracleMerchantState.skill} to player ${getPlayerNumberById(miracleMerchantState.luckyId)}. You do not receive any system-confirmed death cause. Common play is to wait for the recipient's account before authenticating it, but choose freely according to your strategy.`
               : `你是奇迹商人，只知道自己把${giftName}技能给了${getPlayerNumberById(miracleMerchantState.luckyId)}号，系统不会向你确认任何死因。常见打法是先听幸运儿是否报出正确礼物信息再决定相认，但这只是策略建议，可按人设和局势自由选择。`)
          : (isBoard('miracle_merchant')
              ? (language === 'en-US'
                  ? 'If a dead player publicly claimed Miracle Merchant, analyze both wolf-kill and failed-transaction backlash possibilities. Check the claimed recipient, gift type, and the recipient report before treating anyone as mechanically confirmed.'
                  : '若已有死亡玩家公开跳奇迹商人，必须讨论狼刀与交易失败反噬两种可能，并核对其所报幸运儿、技能类型及幸运儿回报，不能无视这条信息，也不能在未对账前机械认定死因。')
              : ''))
    instruction = language === 'en-US'
      ? `Task: give a ${stageName} speech. First write private strategic analysis, then a 120-220 word public in-character speech. Analyze actual night results, prior speeches or votes, name concrete suspects and a voting intention. ${miracleSpeechRule} End naturally and decide a short pause before passing. JSON schema: {"thinking":"private strategy","speech":"public speech","claimRole":"seer|merchant|other|none","pass_microphone":true,"pass_delay_seconds":0.8-4${specialSchema}}`
      : `任务：${stageName}。先写简洁的私密局势分析，再写180-350个中文字符的公开发言。必须结合真实夜间结果、此前发言或票型，至少点出一名具体怀疑对象或矛盾并说明投票倾向。${miracleSpeechRule}发言自然收束，并自行决定结束后停顿多久过麦。JSON格式：{"thinking":"私密策略摘要","speech":"公开发言","claimRole":"seer或merchant或other或none","pass_microphone":true,"pass_delay_seconds":0.8到4${specialSchema}}`
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
      ? `${extra.mustChoose ? 'This is a tie-break PK vote: you must choose one listed candidate and cannot abstain.' : 'This is a regular exile vote: you may abstain, but must explain why because abstention leaves a suspicious voting record.'} Candidates: ${extra.candidates}. JSON: {"thinking":"private vote reasoning","target":number|null,"abstain":boolean}`
      : `${extra.mustChoose ? '本轮是平票后的PK重投，必须从PK台候选人中选择，不得弃票。' : '本轮是常规放逐投票，可以弃票（压手），但必须说明原因，因为弃票会留下需要解释的票型。'}候选：${extra.candidates}。JSON：{"thinking":"私密投票理由","target":玩家编号或null,"abstain":true或false}`
  } else if (action === 'hunter') {
    instruction = language === 'en-US'
      ? `You died by ${extra.cause} and may fire once or deliberately hold your shot if evidence is insufficient. Candidates: ${extra.candidates}. JSON: {"thinking":"private shooting rationale","fire":boolean,"target":number|null}`
      : `你因${extra.cause}出局，可以选择开枪，也可以在证据不足时闷枪。请只根据公开信息判断。可选：${extra.candidates}。JSON：{"thinking":"私密开枪理由","fire":true或false,"target":玩家编号或null}`
  } else if (action === 'miracle') {
    instruction = language === 'en-US'
      ? `Use your once-per-game Merchant ability. Choose another player and grant check, poison, or guard. Candidates: ${extra.candidates}. JSON: {"thinking":"private plan","target":number,"skill":"check|poison|guard"}`
      : `发动每局一次的奇迹商人技能：选择另一名玩家成为幸运儿，并授予查验、毒药、守护之一。首夜“查验”是常规最高收益选择，除非你有明确战术理由才改给毒药或守护。你不知道幸运儿的真实身份。可选：${extra.candidates}。JSON：{"thinking":"私密授予策略","target":玩家编号,"skill":"check或poison或guard"}`
  } else if (action === 'grantedCheck' || action === 'gargoyle') {
    instruction = language === 'en-US'
      ? `Choose one player to inspect. ${action === 'gargoyle' ? 'You learn the exact role and may not repeat a target.' : 'You learn good or wolf.'} Candidates: ${extra.candidates}. JSON: {"thinking":"private check plan","target":number}`
      : `选择一名玩家查验。${action === 'gargoyle' ? '你会得知具体身份，不能验自己或重复验人。' : '你会得知好人或狼人。'}可选：${extra.candidates}。JSON：{"thinking":"私密查验策略","target":玩家编号}`
  } else if (action === 'grantedPoison' || action === 'grantedGuard') {
    const ability = action === 'grantedPoison' ? '一次性毒药' : '一次性守护'
    const limitation = action === 'grantedGuard' ? '守护只能挡狼人刀，不能挡毒药。' : ''
    instruction = `使用${ability}选择一名目标。${limitation}可选：${extra.candidates}。JSON：{"thinking":"私密技能策略","target":玩家编号}`
  } else if (action === 'wolfBeauty') {
    instruction = `使用狼美人魅惑选择一名目标，不能连续两夜魅惑同一人。可选：${extra.candidates}。JSON：{"thinking":"私密技能策略","target":玩家编号}`
  } else if (action === 'sheriffCampaign') {
    instruction = language === 'en-US'
      ? `Decide freely whether to run for sheriff and choose clockwise or counterclockwise speaking order if elected. Common strategy is for the real Seer to run, for Villagers to weigh the value and risk of running, and for wolves to coordinate claimants and below-line votes, but none of these are enforced restrictions. Any role may run, multiple wolves may run, and your choice must follow your own identity, personality, and plan. JSON: {"thinking":"private campaign plan","run":boolean,"direction":"clockwise|counterclockwise","tacticalReason":"reason or empty"}`
      : `根据自己的身份、人设和策略自由决定是否上警，并选择当选后希望采用顺时针或逆时针发言。常见惯例是真预言家上警、平民谨慎评估上警收益、狼队协调悍跳与警下票，但这些都不是系统限制：任何身份都可以上警，狼队也可以多人上警。JSON：{"thinking":"私密竞选策略","run":true或false,"direction":"clockwise或counterclockwise","tacticalReason":"上警理由或空字符串"}`
  } else if (action === 'sheriffWithdraw') {
    instruction = language === 'en-US'
      ? `After all campaign speeches, decide freely whether to withdraw. A real Seer commonly stays and a tactical claimant often withdraws, but these are strategic conventions rather than enforced rules. You may stay even without publicly claiming a power role. JSON: {"thinking":"private withdrawal reasoning","withdraw":boolean}`
      : `所有警上发言结束，现在根据自己的策略自由决定是否退水。真预言家通常不退水、诈身份玩家常选择退水，但这只是常见打法而非系统限制；即使没有公开跳神职，也可以选择继续竞选。JSON：{"thinking":"私密退水判断","withdraw":true或false}`
  } else if (action === 'sheriffVote') {
    instruction = language === 'en-US'
      ? `You are below the sheriff line. ${extra.mustChoose ? 'This is a tie-break PK vote, so you must choose one PK candidate.' : 'In the first sheriff vote you may abstain, but should explain the voting consequence.'} Candidates cannot vote. Candidates: ${extra.candidates}. JSON: {"thinking":"private sheriff read","target":number|null,"abstain":boolean}`
      : `你属于警下玩家（包括刚退水者），仍在警上的候选人没有投票权。${extra.mustChoose ? '本轮是平票后的警徽PK重投，必须从PK台候选人中选择，不得弃票。' : '这是第一次警徽投票，允许弃票，狼人杀中俗称“压手”，但需要解释票型后果。'}候选：${extra.candidates}。JSON：{"thinking":"私密警长判断","target":玩家编号或null,"abstain":true或false}`
  } else if (action === 'sheriffTransfer') {
    instruction = `你是死亡警长，必须把警徽传给一名存活玩家，或撕掉警徽（target为null）。可选：${extra.candidates}。JSON：{"thinking":"私密警徽流判断","target":玩家编号或null}`
  } else if (action === 'knight') {
    instruction = `你可在放逐投票前发动每局一次的骑士决斗，也可保留技能。可选：${extra.candidates}。JSON：{"thinking":"私密决斗判断","use":true或false,"target":玩家编号或null}`
  } else if (action === 'wolfKing') {
    instruction = `你符合狼王开枪条件，可带走一名存活玩家。可选：${extra.candidates}。JSON：{"thinking":"私密开枪策略","target":玩家编号}`
  }
  if (action === 'observeSpeech') {
    instruction = language === 'en-US'
      ? `While player ${extra.speakerNumber} is speaking, privately update your read. Compare the speech with public claims, prior statements, and votes. Do not produce public speech. JSON: {"thinking":"private updated read"}`
      : `在${extra.speakerNumber}号发言期间，私下更新你自己的判断。把这段发言与公开身份声明、此前口径和票型比较；只分析真实出现的信息，不要生成公开发言。对方本次发言：${extra.speech}。JSON：{"thinking":"私密旁听判断"}`
  }
  if (!instruction && ['nightmare', 'silencer', 'magician', 'mechanicalWolf', 'medium', 'cupid', 'janus', 'dreamer', 'stalker'].includes(action)) {
    const labels = {
      nightmare: 'Choose one player to fear; their night ability is disabled and the previous target cannot repeat.',
      silencer: 'Choose one player to silence tomorrow; the previous target cannot repeat.',
      magician: 'Choose two different living players whose night targets will be swapped; do not repeat the same pair.',
      mechanicalWolf: 'Learn one living player exact role and choose a copied skill: check, poison, or guard.',
      medium: 'Choose one living player and learn their exact role.',
      cupid: 'On the first night choose two different living players to link as lovers.',
      janus: 'On the first night choose one role card; if a wolf card is present, you must choose wolf.',
      dreamer: 'Choose one living player to dream. Dreaming the same player on consecutive nights eliminates that player.',
      stalker: 'Decide whether to use the once-per-game assassination on the player you voted for who survived exile.'
    }
    instruction = `${labels[action]} Candidates: ${extra.candidates || 'provided by moderator'}. JSON: {"thinking":"private strategy","use":true,"target":number|null,"target2":number|null,"skill":"check|poison|guard","role":"wolf|villager|seer|witch|hunter"}`
  }
  return { language, systemPrompt, userPrompt: `${base}\n${instruction}` }
}

const requestPlayerDecision = async (player, action, extra = {}) => {
  await waitForRefereeSpeech()
  await waitWhilePaused()
  const config = await getAiConfig(player)
  await waitWhilePaused()
  const prompts = buildDecisionPrompts(player, action, extra, config)
  aiThinkingPlayers.value = [...new Set([...aiThinkingPlayers.value, player.id])]
  try {
    const result = await callStructuredAi(config, prompts.systemPrompt, prompts.userPrompt)
    await waitWhilePaused()
    return { ...(result || {}), language: prompts.language, usedLocalFallback: !result }
  } finally {
    aiThinkingPlayers.value = aiThinkingPlayers.value.filter(id => id !== player.id)
  }
}

const runDecisionWindow = async (version, participants, action, extraBuilder, label, seconds = DECISION_WINDOW_SECONDS) => {
  const eligible = participants.filter(player => player?.isAlive && isLoopActive(version))
  const results = new Map()
  if (!eligible.length) return results
  const participantIds = eligible.map(player => player.id)
  decisionWindow.active = true
  decisionWindow.label = label
  decisionWindow.remaining = seconds
  decisionWindow.total = eligible.length
  decisionWindow.completed = 0
  let timerResolve
  const deadline = new Promise(resolve => { timerResolve = resolve })
  decisionWindowTimer = setInterval(() => {
    if (isGamePaused.value) return
    decisionWindow.remaining = Math.max(0, decisionWindow.remaining - 1)
    if (decisionWindow.remaining === 0) timerResolve('timeout')
  }, 1000)
  const tasks = eligible.map(async player => {
    const decision = await requestPlayerDecision(player, action, extraBuilder(player))
    results.set(player.id, decision)
    if (decisionWindow.active && decisionWindow.label === label) decisionWindow.completed = results.size
    return decision
  })
  const allDone = Promise.allSettled(tasks).then(() => 'complete')
  await Promise.race([allDone, deadline])
  if (decisionWindowTimer) clearInterval(decisionWindowTimer)
  decisionWindowTimer = null
  decisionWindow.active = false
  decisionWindow.label = ''
  decisionWindow.remaining = 0
  decisionWindow.total = 0
  decisionWindow.completed = 0
  if (participantIds.length && results.size < participantIds.length) {
    addRefereeMessage(`${label}时间到，未提交的玩家由主持人按规则补充决定。`, { visibility: 'god' })
  }
  return results
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
  nightState.miracleSkillTargetId = swapNightTargetId(target.id)
  if (skill === 'check') {
    const result = isWolfRole(target) ? '狼人' : '好人'
    playerMemories[lucky.id].checks.push({ day: currentDay.value, targetId: target.id, result })
    playerMemories[lucky.id].miracleGift = { skill: 'check', targetId: target.id, result }
    playerMemories[lucky.id].privateKnowledge.push(`你使用系统赋予的一次性查验，${target.playerNumber}号结果为${result}`)
    addGameMessage({ sender: `${lucky.playerNumber}号幸运儿`, content: `一次性查验${target.playerNumber}号：${result}`, type: 'night-action', visibility: 'private', privateFor: lucky.id })
  } else if (skill === 'poison') {
    nightState.miraclePoisonTargetId = swapNightTargetId(target.id)
    addGameMessage({ sender: `${lucky.playerNumber}号幸运儿`, content: `对${target.playerNumber}号使用一次性毒药`, type: 'night-action', visibility: 'private', privateFor: lucky.id })
  } else {
    nightState.miracleGuardTargetId = swapNightTargetId(target.id)
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
  const skill = ['check', 'poison', 'guard'].includes(decision.skill) ? decision.skill : (currentRound.value === 1 ? 'check' : randomItem(['check', 'poison', 'guard']))
  if (!lucky) return
  const transactionFailed = isWolfRole(lucky)
  Object.assign(miracleMerchantState, { used: true, merchantId: merchant.id, luckyId: lucky.id, skill, pendingDeath: transactionFailed, transactionFailed })
  playerMemories[merchant.id].privateKnowledge.push(`你已把一次性${{ check: '查验', poison: '毒药', guard: '守护' }[skill]}交给${lucky.playerNumber}号；你不知道其阵营。`)
  addGameMessage({ sender: `${merchant.playerNumber}号奇迹商人`, content: `选择${lucky.playerNumber}号为幸运儿，授予${{ check: '查验', poison: '毒药', guard: '守护' }[skill]}`, type: 'night-action', visibility: 'private', privateFor: merchant.id })
  if (transactionFailed) {
    addRefereeMessage('幸运儿属于狼人阵营，技能授予失败，奇迹商人将在天亮时出局。', { visibility: 'god' })
    return
  }
  playerMemories[lucky.id].miracleGift = { skill, targetId: null, result: null }
  playerMemories[lucky.id].privateKnowledge.push(`系统赋予你一次性${{ check: '查验', poison: '毒药', guard: '守护' }[skill]}技能，须在本夜使用；你不知道技能来自哪位玩家。`)
}

const runNightmareAction = async version => {
  currentPhase.value = 'night_nightmare'
  if (!isBoard('nightmare_guard')) return
  const nightmare = alivePlayers.value.find(player => hasRole(player, 'nightmare'))
  if (!nightmare || !isLoopActive(version)) return
  const candidates = alivePlayers.value.filter(player => player.id !== nightmare.id && player.id !== nightmareState.previousTargetId)
  if (!candidates.length) return
  const decision = await requestPlayerDecision(nightmare, 'nightmare', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(nightmare, decision.thinking, '梦魇恐惧')
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  nightmareState.previousTargetId = target?.id || null
  nightState.nightmareTargetId = target?.id || null
  nightState.wolvesBlocked = Boolean(target && isWolfRole(target))
  if (target) addRefereeMessage(`梦魇恐惧了${target.playerNumber}号；该玩家本夜技能失效。`, { visibility: 'god' })
  await phaseDelay()
}

const runSilencerAction = async version => {
  currentPhase.value = 'night_silencer'
  if (!isBoard('stalker_silencer')) return
  const silencer = alivePlayers.value.find(player => hasRole(player, 'silencer'))
  if (!silencer || !isLoopActive(version)) return
  const candidates = alivePlayers.value.filter(player => player.id !== silencer.id && player.id !== silencerState.previousTargetId)
  if (!candidates.length) return
  const decision = await requestPlayerDecision(silencer, 'silencer', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(silencer, decision.thinking, '禁言长老')
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  silencerState.previousTargetId = target?.id || null
  nightState.silencerTargetId = target?.id || null
  silencedPlayerId.value = target?.id || null
  if (target) addRefereeMessage(`禁言长老选择了${target.playerNumber}号，明天该玩家不能发言。`, { visibility: 'god' })
  await phaseDelay()
}

const runMagicianAction = async version => {
  currentPhase.value = 'night_magician'
  if (!isBoard('magician_wolfking')) return
  const magician = alivePlayers.value.find(player => hasRole(player, 'magician'))
  if (!magician || !isLoopActive(version)) return
  const candidates = [...alivePlayers.value]
  const decision = await requestPlayerDecision(magician, 'magician', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(magician, decision.thinking, '魔术师交换')
  const first = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  const secondCandidates = candidates.filter(player => player.id !== first?.id)
  const second = resolvePlayerTarget(decision.target2, secondCandidates) || randomItem(secondCandidates)
  if (!first || !second) return
  const pair = [first.id, second.id].sort().join(':')
  if (pair === magicianState.previousPair) {
    addRefereeMessage('魔术师不能连续两晚交换同一对，本晚交换无效。', { visibility: 'god' })
    return
  }
  magicianState.previousPair = pair
  nightState.magicianSwapIds = [first.id, second.id]
  addRefereeMessage(`魔术师交换了${first.playerNumber}号与${second.playerNumber}号的本夜目标。`, { visibility: 'god' })
  await phaseDelay()
}

const runCupidAction = async version => {
  currentPhase.value = 'night_cupid'
  if (!isBoard('cupid') || cupidState.used) return
  const cupid = alivePlayers.value.find(player => hasRole(player, 'cupid'))
  if (!cupid || !isLoopActive(version)) return
  const candidates = alivePlayers.value.filter(player => player.id !== cupid.id)
  const decision = await requestPlayerDecision(cupid, 'cupid', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(cupid, decision.thinking, '丘比特连情侣')
  const first = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  const secondCandidates = candidates.filter(player => player.id !== first?.id)
  const second = resolvePlayerTarget(decision.target2, secondCandidates) || randomItem(secondCandidates)
  if (!first || !second) return
  Object.assign(cupidState, { used: true, pairIds: [first.id, second.id], mixed: isWolfRole(first) !== isWolfRole(second) })
  playerMemories[first.id]?.privateKnowledge.push(`你与${second.playerNumber}号成为情侣，任何一方死亡你都会殉情。`)
  playerMemories[second.id]?.privateKnowledge.push(`你与${first.playerNumber}号成为情侣，任何一方死亡你都会殉情。`)
  addRefereeMessage(`丘比特已连接${first.playerNumber}号与${second.playerNumber}号为情侣；双方已私下互认。`, { visibility: 'god' })
  await phaseDelay()
}

const runJanusAction = async version => {
  currentPhase.value = 'night_janus'
  if (!isBoard('janus') || janusState.used) return
  const janus = alivePlayers.value.find(player => hasRole(player, 'janus'))
  if (!janus || !isLoopActive(version)) return
  const cards = shuffleArray(['狼人', '平民', '预言家', '女巫', '猎人']).slice(0, 2)
  const hasWolfCard = cards.some(role => isWolfTeamRole(role))
  const candidates = cards.map((role, index) => `${index + 1}号牌:${getRoleName(role)}`).join('、')
  const decision = await requestPlayerDecision(janus, 'janus', { candidates })
  recordPrivateThinking(janus, decision.thinking, '千面人选牌')
  let chosenRole = Number(decision.target) === 2 ? cards[1] : cards[0]
  if (hasWolfCard) chosenRole = cards.find(role => isWolfTeamRole(role)) || chosenRole
  janusState.used = true
  janusState.playerId = janus.id
  janusState.chosenRole = chosenRole
  janus.role = chosenRole
  addRefereeMessage(`千面人已完成首夜选牌，成为${getRoleName(chosenRole)}。`, { visibility: 'god' })
  await phaseDelay()
}

const runMechanicalWolfAction = async version => {
  currentPhase.value = 'night_mechanical_wolf'
  if (!isBoard('medium_mechanical_wolf')) return
  const wolf = alivePlayers.value.find(player => hasRole(player, 'mechanicalWolf'))
  if (!wolf || !isLoopActive(version)) return
  const candidates = alivePlayers.value.filter(player => player.id !== wolf.id)
  const decision = await requestPlayerDecision(wolf, 'mechanicalWolf', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(wolf, decision.thinking, '机械狼学习')
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  if (!target) return
  const learnedRole = target.role
  Object.assign(mechanicalWolfState, { learnedRole, playerId: wolf.id })
  nightState.mechanicalWolfTargetId = target.id
  nightState.mechanicalWolfRole = learnedRole
  playerMemories[wolf.id]?.privateKnowledge.push(`本夜学习${target.playerNumber}号的具体身份：${learnedRole}`)
  if (hasRole(target, 'seer')) {
    const copied = randomItem(alivePlayers.value.filter(player => player.id !== wolf.id))
    if (copied) playerMemories[wolf.id]?.privateKnowledge.push(`复制预言家查验：${copied.playerNumber}号为${roleForSeer(copied)}`)
  } else if (hasRole(target, 'guard')) {
    const guardTarget = randomItem(alivePlayers.value.filter(player => player.id !== wolf.id))
    nightState.miracleGuardTargetId = guardTarget?.id || null
  } else if (hasRole(target, 'witch')) {
    const poisonTarget = randomItem(alivePlayers.value.filter(player => player.id !== wolf.id))
    nightState.miraclePoisonTargetId = poisonTarget?.id || null
  }
  addRefereeMessage(`机械狼本夜学习了${target.playerNumber}号的具体身份，并尝试复制其技能。`, { visibility: 'god' })
  await phaseDelay()
}

const runMediumAction = async version => {
  currentPhase.value = 'night_medium'
  if (!isBoard('medium_mechanical_wolf')) return
  const medium = alivePlayers.value.find(player => hasRole(player, 'medium'))
  if (!medium || !isLoopActive(version)) return
  const candidates = alivePlayers.value.filter(player => player.id !== medium.id)
  if (!candidates.length) return
  const decision = await requestPlayerDecision(medium, 'medium', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(medium, decision.thinking, '通灵师查验')
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  if (!target) return
  const result = hasRole(target, 'mechanicalWolf') ? '狼人' : target.role
  nightState.mediumTargetId = target.id
  nightState.mediumResult = result
  playerMemories[medium.id]?.privateKnowledge.push(`通灵师查验${target.playerNumber}号，具体身份为${result}`)
  addGameMessage({ sender: `${medium.playerNumber}号通灵师`, content: `查验${target.playerNumber}号：${result}`, type: 'night-action', visibility: 'private', privateFor: medium.id })
  await phaseDelay()
}

const runDreamerAction = async version => {
  currentPhase.value = 'night_dreamer'
  if (!isBoard('shapeshifter_wolfking')) return
  const dreamer = alivePlayers.value.find(player => hasAbilityRole(player, 'dreamer'))
  if (!dreamer || !isLoopActive(version)) return
  const candidates = alivePlayers.value.filter(player => player.id !== dreamer.id)
  if (!candidates.length) return
  const decision = await requestPlayerDecision(dreamer, 'dreamer', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(dreamer, decision.thinking, '摄梦人行动')
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  dreamerState.previousTargetId = dreamerState.currentTargetId
  dreamerState.currentTargetId = target?.id || null
  nightState.dreamerTargetId = target?.id || null
  if (target) addRefereeMessage(`摄梦人选择了${target.playerNumber}号；连续两夜摄梦同一目标会令其出局。`, { visibility: 'god' })
  await phaseDelay()
}

const revealWolfBrotherKnowledge = () => {
  const brother = players.value.find(player => player.id === wolfBrotherState.brotherId)
  const sister = players.value.find(player => player.id === wolfBrotherState.sisterId)
  if (brother && sister && brother.isAlive && sister.isAlive && currentRound.value === 1) {
    playerMemories[brother.id]?.privateKnowledge.push(`你的狼弟是${sister.playerNumber}号，首夜已互认。`)
    playerMemories[sister.id]?.privateKnowledge.push(`你的狼兄是${brother.playerNumber}号，首夜已互认。`)
  }
  if (brother && !brother.isAlive && sister?.isAlive && !wolfBrotherState.awakened) {
    wolfBrotherState.awakened = true
    wolfBrotherState.awakenRound = currentRound.value
    playerMemories[sister.id]?.privateKnowledge.push('狼兄已经出局，从下一夜起你苏醒并可参与狼刀。')
  }
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
  if (!gargoyle || !isLoopActive(version) || isNightmareDisabled(gargoyle)) return
  const checkedIds = (playerMemories[gargoyle.id]?.checks || []).map(check => check.targetId)
  const candidates = alivePlayers.value.filter(player => player.id !== gargoyle.id && !checkedIds.includes(player.id))
  if (!candidates.length) return
  addRefereeMessage('石像鬼睁眼查验一名未查验玩家的具体身份。', { visibility: 'god' })
  const decision = await requestPlayerDecision(gargoyle, 'gargoyle', { candidates: candidates.map(player => `${player.playerNumber}号`).join('、') })
  recordPrivateThinking(gargoyle, decision.thinking, '石像鬼查验')
  const target = resolvePlayerTarget(decision.target, candidates) || randomItem(candidates)
  if (!target) return
  const effectiveTargetId = swapNightTargetId(target.id)
  const effectiveTarget = players.value.find(player => player.id === effectiveTargetId) || target
  nightState.gargoyleTargetId = effectiveTarget.id
  nightState.gargoyleResult = effectiveTarget.role
  playerMemories[gargoyle.id].checks.push({ day: currentDay.value, targetId: effectiveTarget.id, result: effectiveTarget.role })
  playerMemories[gargoyle.id].privateKnowledge.push(`第${currentRound.value}夜查验${effectiveTarget.playerNumber}号，具体身份为${effectiveTarget.role}`)
  addGameMessage({ sender: `${gargoyle.playerNumber}号石像鬼`, content: `查验${effectiveTarget.playerNumber}号：${effectiveTarget.role}`, type: 'night-action', visibility: 'private', privateFor: gargoyle.id })
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
  if (!guard || !isLoopActive(version) || isNightmareDisabled(guard)) {
    addRefereeMessage('本局无存活守卫，跳过守卫行动。', { visibility: 'god' })
    return
  }
  const candidates = alivePlayers.value.filter(player => !gameRules.guardCannotRepeatTarget || player.id !== lastGuardTargetId.value)
  const decision = await requestPlayerDecision(guard, 'guard', { candidates: candidates.map(p => `${p.playerNumber}号`).join('、') })
  recordPrivateThinking(guard, decision.thinking, '守卫行动')
  const fallback = Math.random() < 0.12 ? null : randomItem(candidates)
  const target = resolvePlayerTarget(decision.target, candidates) || fallback
  nightState.guardTargetId = target ? swapNightTargetId(target.id) : null
  lastGuardTargetId.value = nightState.guardTargetId
  addGameMessage({ sender: `${guard.playerNumber}号守卫`, content: target ? `守护${target.playerNumber}号${target.playerName}` : '选择空守', type: 'night-action', visibility: 'private', privateFor: guard.id })
  await phaseDelay()
}

const runWolfActions = async (version) => {
  currentPhase.value = 'night_wolf'
  revealWolfBrotherKnowledge()
  if (nightState.wolvesBlocked) {
    addRefereeMessage('梦魇恐惧了狼人，狼队本夜空刀。', { visibility: 'god' })
    await phaseDelay()
    return
  }
  addRefereeMessage('狼人请睁眼。每名狼人独立提交刀口，由上帝按多数票结算。', { visibility: 'god' })
  const packWolves = alivePlayers.value.filter(player => isPackWolf(player) || (wolfBrotherState.awakened && hasRole(player, 'wolfSister')))
  const gargoyle = alivePlayers.value.find(player => hasRole(player, 'gargoyle'))
  const wolves = packWolves.length ? packWolves : (gargoyle ? [gargoyle] : [])
  const candidates = alivePlayers.value.filter(player => !isWolfRole(player) || (packWolves.length && hasRole(player, 'wolfKing')))
  if (!packWolves.length && gargoyle) {
    addRefereeMessage('其他狼人已全部出局，石像鬼本夜获得单独袭击能力。', { visibility: 'god' })
  }
  const wolfDecisions = await runDecisionWindow(version, wolves, 'wolf', () => ({ candidates: candidates.map(p => `${p.playerNumber}号`).join('、') }), '狼人刀口决策')
  for (const wolf of wolves) {
    if (!isLoopActive(version) || !candidates.length) break
    const decision = wolfDecisions.get(wolf.id) || {}
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
  nightState.wolfTargetId = swapNightTargetId(randomItem(tiedIds))
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
  const witch = alivePlayers.value.find(player => hasAbilityRole(player, 'witch'))
  if (!witch || !isLoopActive(version) || isNightmareDisabled(witch)) {
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
  const effectivePoisonId = poisonTarget ? swapNightTargetId(poisonTarget.id) : null
  nightState.witchPoisonTargetId = effectivePoisonId
  const effectivePoisonTarget = players.value.find(player => player.id === effectivePoisonId)
  if (effectivePoisonTarget && hasRole(effectivePoisonTarget, 'evilKnight')) nightState.evilKnightPoisonReflected = true
  if (nightState.witchSaved) witchInventory.antidote--
  if (nightState.witchPoisonTargetId) witchInventory.poison--
  const actions = [nightState.witchSaved ? `对${wolfTarget.playerNumber}号使用解药` : '未使用解药']
  actions.push(poisonTarget ? `对${poisonTarget.playerNumber}号使用毒药` : '未使用毒药')
  addGameMessage({ sender: `${witch.playerNumber}号女巫`, content: actions.join('；'), type: 'night-action', visibility: 'private', privateFor: witch.id })
  await phaseDelay()
}

const runSeerAction = async (version) => {
  currentPhase.value = 'night_seer'
  const seer = alivePlayers.value.find(player => hasAbilityRole(player, 'seer'))
  if (!seer || !isLoopActive(version) || isNightmareDisabled(seer)) {
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
  const effectiveTargetId = swapNightTargetId(target.id)
  const effectiveTarget = players.value.find(player => player.id === effectiveTargetId) || target
  const result = roleForSeer(effectiveTarget)
  nightState.seerTargetId = effectiveTarget.id
  nightState.seerResult = result
  playerMemories[seer.id].checks.push({ day: currentDay.value, targetId: effectiveTarget.id, result })
  playerMemories[seer.id].privateKnowledge.push(`第${currentRound.value}夜查验${effectiveTarget.playerNumber}号，结果为${result}`)
  addGameMessage({ sender: `${seer.playerNumber}号预言家`, content: `查验${effectiveTarget.playerNumber}号${effectiveTarget.playerName}：${result}`, type: 'night-action', visibility: 'private', privateFor: seer.id })
  if (hasRole(effectiveTarget, 'evilKnight') && !evilKnightState.seerReflected) nightState.evilKnightSeerReflected = true
  if (hasRole(effectiveTarget, 'cursedFox')) nightState.cursedFoxSeerTargetId = effectiveTarget.id
  await phaseDelay()
}

const resolveNight = () => {
  const deaths = new Set()
  const wolfTarget = players.value.find(player => player.id === nightState.wolfTargetId)
  const guarded = Boolean(wolfTarget && (nightState.guardTargetId === wolfTarget.id || nightState.miracleGuardTargetId === wolfTarget.id))
  const saved = Boolean(wolfTarget && nightState.witchSaved)
  let mainReason = '狼人没有形成有效刀口'
  if (wolfTarget) {
    if (hasRole(wolfTarget, 'cursedFox')) {
      mainReason = `${wolfTarget.playerNumber}号咒狐免疫狼人夜刀`
    } else if (hasRole(wolfTarget, 'evilKnight')) {
      mainReason = `${wolfTarget.playerNumber}号恶灵骑士免疫夜间伤害`
    } else if (guarded && saved && gameRules.sameGuardAndSaveKills) {
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
  const poisoned = players.value.filter(player => [nightState.witchPoisonTargetId, nightState.miraclePoisonTargetId].includes(player.id) && !hasRole(player, 'evilKnight'))
  poisoned.forEach(player => deaths.add(player.id))
  if (nightState.evilKnightSeerReflected && !evilKnightState.seerReflected && !evilKnightState.witchReflected) {
    const seer = alivePlayers.value.find(player => hasAbilityRole(player, 'seer'))
    if (seer) deaths.add(seer.id)
    evilKnightState.seerReflected = true
  }
  if (nightState.evilKnightPoisonReflected && !evilKnightState.seerReflected && !evilKnightState.witchReflected) {
    const witch = alivePlayers.value.find(player => hasAbilityRole(player, 'witch'))
    if (witch) deaths.add(witch.id)
    evilKnightState.witchReflected = true
  }
  if (nightState.cursedFoxSeerTargetId) {
    deaths.add(nightState.cursedFoxSeerTargetId)
    cursedFoxState.killedBySeer = true
  }
  if (dreamerState.currentTargetId && dreamerState.currentTargetId === dreamerState.previousTargetId) deaths.add(dreamerState.currentTargetId)
  const dreamer = players.value.find(player => hasAbilityRole(player, 'dreamer'))
  if (dreamer && deaths.has(dreamer.id) && dreamerState.currentTargetId) deaths.add(dreamerState.currentTargetId)
  const merchant = players.value.find(player => player.id === miracleMerchantState.merchantId)
  if (miracleMerchantState.pendingDeath && merchant?.isAlive) {
    deaths.add(merchant.id)
  }
  nightState.deaths = [...deaths]
  const poisonDetail = poisoned.length ? `；毒药带走${poisoned.map(player => `${player.playerNumber}号`).join('、')}` : ''
  const merchantDetail = miracleMerchantState.pendingDeath && merchant?.isAlive ? '；幸运儿为狼人，奇迹商人遭受反噬出局' : ''
  const reflectionDetail = nightState.evilKnightSeerReflected || nightState.evilKnightPoisonReflected ? '；恶灵骑士触发一次性反伤' : ''
  const foxDetail = nightState.cursedFoxSeerTargetId ? '；咒狐被查验后出局' : ''
  nightState.explanation = `${mainReason}${poisonDetail}${merchantDetail}${reflectionDetail}${foxDetail}`
  if (!nightState.deaths.length) nightState.explanation = `平安夜：${mainReason}`
  addRefereeMessage('夜间行动结算完成。', { visibility: 'god', detail: nightState.explanation })
}

const runNightPhase = async (version) => {
  currentPhase.value = 'night'
  resetNightState()
  const actionLabels = { miracle: '奇迹商人', gravedigger: '守墓人', gargoyle: '石像鬼', guard: '守卫', wolves: '狼人', wolfBeauty: '狼美人', witch: '女巫', seer: '预言家', nightmare: '梦魇', silencer: '禁言长老', magician: '魔术师', mechanicalWolf: '机械狼', medium: '通灵师', cupid: '丘比特', janus: '千面人', dreamer: '摄梦人' }
  addRefereeMessage(currentLocale() === 'zh-CN'
    ? `第${currentRound.value}夜开始，天黑请闭眼。本板子行动顺序：${boardRules.value.nightOrder.map(action => actionLabels[action]).join('→')}。`
    : `Night ${currentRound.value} begins. Board action order: ${boardRules.value.nightOrder.join(' -> ')}.`)
  const actions = {
    miracle: () => runMiracleMerchantAction(version),
    gravedigger: () => runGravediggerAction(version),
    gargoyle: () => runGargoyleAction(version),
    nightmare: () => runNightmareAction(version),
    silencer: () => runSilencerAction(version),
    magician: () => runMagicianAction(version),
    mechanicalWolf: () => runMechanicalWolfAction(version),
    medium: () => runMediumAction(version),
    cupid: () => runCupidAction(version),
    janus: () => runJanusAction(version),
    dreamer: () => runDreamerAction(version),
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
  if (isBoard('miracle_merchant') && miracleMerchantState.used && !miracleMerchantState.pendingDeath && !miracleMerchantState.giftResolved) {
    const lucky = players.value.find(player => player.id === miracleMerchantState.luckyId && player.isAlive)
    if (lucky) await runGrantedSkillAction(version, lucky, miracleMerchantState.skill)
    miracleMerchantState.giftResolved = true
  }
  if (isLoopActive(version)) resolveNight()
}

const resolveHunterSkill = async (hunter, version, cause) => {
  if (!hunter || !hasAbilityRole(hunter, 'hunter') || hunterSkillUsed.has(hunter.id) || !isLoopActive(version) || bomberSuppressedIds.has(hunter.id)) return
  hunterSkillUsed.add(hunter.id)
  if (cause === 'poison' || (cause === 'night' && isNightmareDisabled(hunter))) {
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
  const target = resolvePlayerTarget(decision.target, candidates)
  if (decision.fire === false || !target) {
    addRefereeMessage(`${hunter.playerNumber}号猎人选择不开枪。`)
    return
  }

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
  if (!wolfKing || !hasRole(wolfKing, 'wolfKing') || wolfKingSkillUsed.has(wolfKing.id) || !isLoopActive(version) || bomberSuppressedIds.has(wolfKing.id)) return
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

const resolveCupidLink = async (player, version) => {
  if (cupidState.resolving || !cupidState.pairIds.includes(player?.id)) return
  const partnerId = cupidState.pairIds.find(id => id !== player.id)
  const partner = players.value.find(candidate => candidate.id === partnerId && candidate.isAlive)
  if (!partner) return
  cupidState.resolving = true
  killPlayer(partner.id)
  addRefereeMessage(`${partner.playerNumber}号${partner.playerName}因情侣关系殉情出局。`)
  await resolveDeathEffects(partner, version, 'romance')
  cupidState.resolving = false
}

const registerSpecialDeath = player => {
  if (!player || specialDeathProcessedIds.has(player.id)) return
  specialDeathProcessedIds.add(player.id)
  if (hasRole(player, 'sun')) {
    bonusDayPending.value++
    addRefereeMessage('太阳死亡，结算后将额外进行一个白天。')
  }
  if (hasRole(player, 'moon')) {
    bonusNightPending.value++
    addRefereeMessage('月亮死亡，结算后将额外进行一个夜晚。')
  }
}

const resolveDeathEffects = async (player, version, cause, options = {}) => {
  if (!player || !isLoopActive(version)) return
  registerSpecialDeath(player)
  if (!options.suppressSkills && !bomberSuppressedIds.has(player.id)) {
    await resolveHunterSkill(player, version, cause)
    await resolveWolfKingSkill(player, version, cause)
    await resolveWolfBeautyLink(player, version, cause)
  }
  await resolveCupidLink(player, version)
  await transferSheriffBadge(player, version, options.consumeSheriff)
}

const settleNightWithoutAnnouncement = async version => {
  nightState.deaths.forEach(killPlayer)
  for (const playerId of nightState.deaths) {
    const player = players.value.find(candidate => candidate.id === playerId)
    const cause = [nightState.witchPoisonTargetId, nightState.miraclePoisonTargetId].includes(playerId) ? 'poison' : 'night'
    await resolveDeathEffects(player, version, cause)
  }
  miracleMerchantState.pendingDeath = false
  addRefereeMessage('因警长竞选自爆，本日不公布首夜死讯；首夜结算已由主持人在后台完成。', { visibility: 'god', detail: nightState.explanation })
}

const announceDay = async (version) => {
  currentPhase.value = 'day'
  await addRefereeMessage(currentLocale() === 'zh-CN' ? `天亮了，现在是第${currentDay.value}天。` : `Dawn breaks. Day ${currentDay.value} begins.`)
  nightState.deaths.forEach(killPlayer)
  if (!nightState.deaths.length) {
    lastPublicNightReport.value = currentLocale() === 'zh-CN' ? '昨夜是平安夜，没有人死亡。' : 'It was a peaceful night. No one died.'
  } else {
    const numbers = nightState.deaths.map(id => `${getPlayerNumberById(id)}号`).join('、')
    lastPublicNightReport.value = currentLocale() === 'zh-CN'
      ? `昨夜${numbers}玩家死亡，死亡不分先后。`
      : `Last night, player(s) ${numbers} died. Deaths are unordered.`
  }
  await addRefereeMessage(lastPublicNightReport.value, { detail: nightState.explanation })
  if (isBoard('bear_hunter_idiot') || isBoard('shapeshifter_wolfking')) {
    const bear = alivePlayers.value.find(player => hasAbilityRole(player, 'bear'))
    if (bear) {
      const seats = [...players.value].sort((a, b) => a.playerNumber - b.playerNumber)
      const index = seats.findIndex(player => player.id === bear.id)
      const neighbours = index < 0 ? [] : [seats[(index - 1 + seats.length) % seats.length], seats[(index + 1) % seats.length]].filter(player => player?.isAlive)
      const roaring = neighbours.some(isWolfRole)
      await addRefereeMessage(roaring ? '熊咆哮了：熊相邻的存活玩家中存在狼人。' : '熊没有咆哮：熊相邻的存活玩家中没有狼人。')
    }
  }
  if (nightState.deaths.length && currentRound.value === 1 && Number(boardRules.value.players) === 12) {
    for (const playerId of shuffleArray([...nightState.deaths])) {
      const firstNightDead = players.value.find(player => player.id === playerId)
      await giveLastWords(firstNightDead, version, 'night')
    }
  }
  for (const playerId of nightState.deaths) {
    const player = players.value.find(candidate => candidate.id === playerId)
    const cause = [nightState.witchPoisonTargetId, nightState.miraclePoisonTargetId].includes(playerId) ? 'poison' : 'night'
    await resolveDeathEffects(player, version, cause)
  }
  miracleMerchantState.pendingDeath = false
  await addRefereeMessage(currentLocale() === 'zh-CN' ? `当前存活${alivePlayers.value.length}人。` : `${alivePlayers.value.length} players remain alive.`)
  await phaseDelay(800)
}

const contextualFallbackSpeech = (player, language, action = 'speech') => {
  const others = alivePlayers.value.filter(p => p.id !== player.id)
  const recentSpeeches = dialogMessages.value
    .filter(message => message.type === 'player' && (message.visibility || 'public') === 'public' && message.sender !== player.playerName)
    .slice(-3)
  const latestSpeech = recentSpeeches.at(-1)
  const suspect = latestSpeech
    ? players.value.find(p => p.playerName === latestSpeech.sender && p.isAlive && p.id !== player.id) || randomItem(others)
    : randomItem(others)
  const second = others.find(p => p.id !== suspect?.id) || suspect
  const memory = playerMemories[player.id] || { checks: [], votes: [] }
  const latestCheck = (memory.checks || []).slice(-1)[0]
  const checkedWolf = (memory.checks || []).slice().reverse().find(check => check.result === '狼人')
  const latestVote = voteHistory.value[voteHistory.value.length - 1]
  const votesAgainstPlayer = latestVote?.ballots?.filter(ballot => ballot.targetId === player.id).map(ballot => getPlayerNumberById(ballot.voterId)) || []
  const ownVote = (memory.votes || []).slice(-1)[0]
  const excerpt = String(latestSpeech?.content || '').replace(/\s+/g, ' ').slice(0, 42)
  const merchantClaim = Object.values(publicMiracleClaims).find(claim => !players.value.find(player => player.id === claim.merchantId)?.isAlive)
    || Object.values(publicMiracleClaims)[0]
  const merchantClaimant = players.value.find(candidate => candidate.id === merchantClaim?.merchantId)
  const isLastWords = action === 'lastWords' || action === 'nightLastWords'
  const isNightLastWords = action === 'nightLastWords'
  const giftLabel = { check: '查验', poison: '毒药', guard: '守护' }[miracleMerchantState.skill] || '技能'
  if (isLastWords) {
    if (hasRole(player, 'miracle') && miracleMerchantState.used) {
      const targetNumber = getPlayerNumberById(miracleMerchantState.luckyId)
      if (language === 'en-US') {
        return `I am the Miracle Merchant. Last night I granted a one-use ${miracleMerchantState.skill || 'ability'} to player ${targetNumber}. The public death report cannot tell whether I died to the wolf kill or transaction backlash, so player ${targetNumber} should report whether the gift arrived and how it was used. Compare that account with the night result before drawing a conclusion.`
      }
      return `我是奇迹商人。昨夜我把一次性${giftLabel}交给了${targetNumber}号。公开死讯无法判断我是被狼刀还是交易反噬，${targetNumber}号可以说明是否收到${giftLabel}以及如何使用，场上再结合夜间结果对账。两种死因目前都只能作为可能性，不能机械认定。`
    }
    if (language === 'en-US') {
      const exitReview = isNightLastWords
        ? 'I died during the night, and the public announcement does not prove whether the cause was the wolf kill, poison, or another role interaction.'
        : (votesAgainstPlayer.length ? `Players ${votesAgainstPlayer.join(', ')} cast the votes that exiled me.` : 'I was exiled without a stable public case.')
      return `${exitReview} ${latestCheck ? `My confirmed inspection on player ${getPlayerNumberById(latestCheck.targetId)} was ${latestCheck.result}.` : ''} My final warning is player ${suspect?.playerNumber || '?'}. Compare that slot's statement${excerpt ? `, “${excerpt}”` : ''} with player ${second?.playerNumber || '?'} and the public voting record. I am out and will take no further action; this is the evidence I leave to the living players.`
    }
    const exitReview = isNightLastWords
      ? '我在昨夜倒牌，公开死讯本身不能证明我是被狼刀、毒药还是其他技能结算带走。'
      : (votesAgainstPlayer.length ? `刚才${votesAgainstPlayer.join('号、')}号的票共同把我推出了局。` : '我已经被公投出局，但公开票型没有形成稳定理由。')
    const roleInfo = latestCheck
      ? `我能留下的硬信息是：${getPlayerNumberById(latestCheck.targetId)}号查验为${latestCheck.result === '狼人' ? '查杀' : '金水'}。`
      : ''
    return `${exitReview}${roleInfo}我最后提醒场上核对${suspect?.playerNumber || '?'}号${excerpt ? `刚才“${excerpt}”这段发言` : '的公开立场'}，再和${second?.playerNumber || '?'}号的站边及票型放在一起看。${checkedWolf ? `${getPlayerNumberById(checkedWolf.targetId)}号是我留下的首要狼人信息。` : '不要凭语气认好，也不要忽略临时改口和跟票关系。'}我已经出局，不再参与后续行动，这些是我留给存活玩家的判断。`
  }
  if (['sheriffSpeech', 'sheriffPkSpeech'].includes(action)) {
    const latestCheck = (playerMemories[player.id]?.checks || []).slice(-1)[0]
    if (hasRole(player, 'seer')) {
      return `我上警竞选警长，我是预言家。${latestCheck ? `昨夜查验${getPlayerNumberById(latestCheck.targetId)}号，结果是${latestCheck.result === '狼人' ? '查杀' : '金水'}。` : '首夜查验信息会在拿到结果后完整报出。'}我会把警徽流留在两个需要重点观察的位置，警下请根据验人、发言逻辑和对跳关系投票，不要只听语气站边。如果有对跳预言家，我会逐项核对其验人理由、警徽流和狼坑是否完整。`
    }
    return `我选择上警，是为了把自己的判断放到警上接受全场检验。我目前关注${suspect?.playerNumber || '?'}号和${second?.playerNumber || '?'}号，重点会看他们是否给出具体身份声明、验人信息、上警收益和完整警徽流。是否退水要结合对跳关系、警下票型和后续发言判断，我会在退水环节明确表态。`
  }
  if (language === 'en-US') {
    const speechEvidence = latestSpeech ? `Player ${suspect?.playerNumber} said “${excerpt}”; I want the reason and intended vote behind that statement.` : 'I am early in the order, so there is no prior speech to invent a contradiction from.'
    const voteEvidence = ownVote?.targetId ? `My previous public vote was on player ${getPlayerNumberById(ownVote.targetId)}.` : (ownVote?.abstained ? 'I abstained in the previous vote and must account for that choice.' : 'There is no previous public vote from me to use yet.')
    return `${lastPublicNightReport.value || 'The day discussion has begun.'} ${speechEvidence} ${voteEvidence} ${latestCheck ? `My inspection on player ${getPlayerNumberById(latestCheck.targetId)} was ${latestCheck.result}, which is the strongest information I have.` : `My current focus is player ${suspect?.playerNumber || '?'}, compared directly with player ${second?.playerNumber || '?'}.`} I will base the vote on role claims, contradictions, and the public ballot rather than repeated generic wording.`
  }
  const speechEvidence = latestSpeech
    ? `前置位${suspect?.playerNumber}号刚才说“${excerpt}”，我要求他把这句话对应到具体怀疑对象和投票理由，不能只留结论。`
    : '我在本轮发言顺序靠前，目前没有前置位发言可以盘，所以不会凭空编造矛盾。'
  const voteEvidence = ownVote?.targetId
    ? `上一轮我投给了${getPlayerNumberById(ownVote.targetId)}号，这一票要继续结合今天的新发言复核。`
    : '目前没有可复盘的个人历史票型，我先明确本轮观察重点。'
  const merchantEvidence = merchantClaimant
    ? `${merchantClaimant.playerNumber}号已公开声明奇迹商人${merchantClaimant.isAlive ? '' : '并倒牌'}${merchantClaim?.luckyNumber ? `，报${merchantClaim.luckyNumber}号为幸运儿` : ''}${merchantClaim?.skill ? `、赠予${merchantClaim.skill}` : ''}。必须先完成幸运儿对账，再区分狼刀与交易失败反噬，不能把这条线跳过去。`
    : ''
  const hardInfo = latestCheck
    ? `我掌握的查验结果是${getPlayerNumberById(latestCheck.targetId)}号为${latestCheck.result === '狼人' ? '查杀' : '金水'}，这是本轮优先级最高的信息。`
    : `我暂时重点比较${suspect?.playerNumber || '?'}号和${second?.playerNumber || '?'}号，观察谁在回避角色声明、跟随结论或临时改变票型。`
  return `${lastPublicNightReport.value || '现在进入白天讨论。'}${speechEvidence}${voteEvidence}${merchantEvidence}${hardInfo}在出现更硬的信息前，我不会凭语气认好；我的表态会跟着公开身份、具体矛盾和真实票型走。`
}

const normalizeGameTerms = speech => String(speech || '').replace(/(\d+)号([^。！？\n]{0,18})(悍跳狼|悍跳)/g, (match, number, bridge, term) => {
  const target = players.value.find(player => Number(player.playerNumber) === Number(number))
  if (target && publicRoleClaims[target.id] === '预言家') return match
  return `${number}号${bridge}${term === '悍跳狼' ? '狼人嫌疑位' : '身份表述可疑'}`
})

const enforceMiracleGiftSpeech = (speech, player, language) => {
  const gift = playerMemories[player.id]?.miracleGift
  if (gift?.skill !== 'check' || !gift.result || !gift.targetId) return speech
  const number = getPlayerNumberById(gift.targetId)
  const hasReport = new RegExp(`${number}号[^。！？\\n]{0,24}${gift.result === '狼人' ? '查杀|狼人' : '金水|好人'}`).test(speech)
  if (hasReport) return speech
  if (language === 'en-US') {
    return `I received a mysterious gift last night and inspected player ${number}: the result was ${gift.result === '狼人' ? 'wolf' : 'good'}. ${speech}`
  }
  const report = gift.result === '狼人'
    ? `我昨夜收到一份神秘礼物，用它查验了${number}号，结果是查杀。老板如果听到了，请出来为这份信息作证，今天优先处理${number}号。`
    : `我昨夜收到一份神秘礼物，用它查验了${number}号，结果是金水。老板暂时不必抢先暴露，我会先把这条信息交给好人阵营。`
  return `${report}${speech}`
}

const truncateAfterPass = (speech, language) => {
  const source = String(speech || '')
  const pattern = language === 'en-US'
    ? /(?:^|[,.!?]\s*)(?:I\s+)?(?:pass\s+the\s+microphone|pass)(?=[.!?]|$)[.!?]?/i
    : /(?:^|[，,。！？]\s*)(?:我(?:就)?先?)?(?:过麦|过了)(?=[。！!]|$)[。！!]?/
  const match = pattern.exec(source)
  return match ? source.slice(0, match.index + match[0].length) : source
}

const removeDuplicateSentences = speech => {
  const seen = new Set()
  return String(speech || '').split(/(?<=[。！？.!?])/).filter(sentence => {
    const key = sentence.replace(/\s+/g, '').trim()
    if (!key || seen.has(key)) return false
    seen.add(key)
    return true
  }).join('').trim()
}

const registerPublicRoleClaim = (player, speech) => {
  const content = String(speech || '')
  if (/(我是|我这里是|我跳)(真)?预言家|I (?:am|claim) (?:the )?seer/i.test(content)) publicRoleClaims[player.id] = '预言家'
  else if (/(我是|我这里是|我跳)(真)?奇迹商人|我是老板|I (?:am|claim) (?:the )?(?:merchant|miracle merchant)/i.test(content)) {
    publicRoleClaims[player.id] = '奇迹商人'
    const targetMatch = content.match(/(?:交给了?|给了?|幸运儿(?:是|为)?)(\d+)号/)
      || content.match(/(?:recipient|gave|granted)[^.!?\d]{0,24}(?:player\s*)?(\d+)/i)
    const skillMatch = content.match(/查验|毒药|守护|inspection|poison|protection/i)
    const normalizedSkill = { inspection: '查验', poison: '毒药', protection: '守护' }[String(skillMatch?.[0] || '').toLowerCase()] || skillMatch?.[0] || ''
    const existing = publicMiracleClaims[player.id] || {}
    publicMiracleClaims[player.id] = {
      merchantId: player.id,
      merchantNumber: player.playerNumber,
      luckyNumber: Number(targetMatch?.[1]) || existing.luckyNumber || null,
      skill: normalizedSkill || existing.skill || '',
      claimedBacklash: existing.claimedBacklash || /交易失败|反噬|transaction (?:failed|backlash)|backlash/i.test(content)
    }
  }
}

const sanitizeSpeech = (speech, player, language, action = 'speech') => {
  let cleaned = normalizeGameTerms(String(speech || '').replace(/```[\s\S]*?```/g, '').trim())
  cleaned = removeDuplicateSentences(truncateAfterPass(cleaned, language))
  const hasMeta = /(作为AI|我是AI|系统提示|语言模型|token|as an ai|system prompt|language model|JSON格式|JSON schema|私密策略摘要|必须结合真实夜间结果|候选：.*JSON)/i.test(cleaned)
  const mentionedRoles = cleaned.match(boardRolePattern) || []
  const hasOffBoardRole = mentionedRoles.some(role => !boardRoleNames.value.has(role))
  const claimsGuardBlocksPoison = /(?:守卫|守护|盾)[^。！？\n]{0,18}(?:挡住|免疫|防住|救下)[^。！？\n]{0,8}(?:毒|毒药)|(?:毒|毒药)[^。！？\n]{0,18}(?:被)?(?:守卫|守护|盾)[^。！？\n]{0,10}(?:挡住|免疫|防住|救下)/.test(cleaned)
  const isLastWords = ['lastWords', 'nightLastWords'].includes(action)
  const invalidLastWords = isLastWords && /(继续听|听完后|等后置位|后面发言|之后再投|准备投给|投票倾向|下一轮|明天我|再调整判断|我(?:还|会|将).*?(?:听|投|发言|观察|调整)|i will (?:keep listening|listen|vote|update)|i(?:'ll| am going to).*?(?:listen|vote|speak|update)|my (?:current )?voting preference|next round|later speeches)/i.test(cleaned)
  const minimum = isLastWords
    ? (language === 'en-US' ? 160 : 70)
    : (language === 'en-US' ? 220 : 90)
  if (!cleaned || cleaned.length < minimum || hasMeta || invalidLastWords || hasOffBoardRole || claimsGuardBlocksPoison) cleaned = contextualFallbackSpeech(player, language, action)
  cleaned = enforceMiracleGiftSpeech(cleaned, player, language)
  return cleaned
}

const presentSpeech = (player, speech, thinking, language, passDelaySeconds = 1.4) => new Promise(resolve => {
  if (activeSpeechResolver) closeSpeech()
  aiSpeakingContent.value = { playerId: player.id, playerName: `${player.playerNumber}号 ${player.playerName}`, content: speech, thinking }
  let typingComplete = false
  let playbackComplete = false
  let autoPassScheduled = false
  const scheduleAutoPass = () => {
    if (!typingComplete || !playbackComplete || autoPassScheduled || !aiSpeakingContent.value) return
    autoPassScheduled = true
    const waitMs = Math.round(Math.min(4, Math.max(0.8, Number(passDelaySeconds) || 1.4)) * 1000)
    const passWhenReady = () => {
      if (isGamePaused.value || speechPaused.value) {
        activeAutoPassTimer = setTimeout(passWhenReady, 350)
        return
      }
      closeSpeech('ai')
    }
    activeAutoPassTimer = setTimeout(passWhenReady, waitMs)
  }
  activeTypewriterCompleteHandler = () => {
    typingComplete = true
    scheduleAutoPass()
  }
  startTypewriter(speech, { speed: SPEEDS[typewriterSpeed.value] || 50, onComplete: activeTypewriterCompleteHandler })
  void speakText(speech, { ...getPlayerVoiceOverrides(player), lang: language, speaker: 'player' })
    .catch(error => console.warn('Player speech failed:', error))
    .finally(() => {
      playbackComplete = true
      scheduleAutoPass()
    })
  activeSpeechResolver = resolve
  startSpeechTimer()
})

const generatePublicSpeech = async (player, action = 'speech') => {
  const decision = await requestPlayerDecision(player, action)
  const speech = sanitizeSpeech(decision.speech, player, decision.language, action)
  registerPublicRoleClaim(player, speech)
  const thinking = String(decision.thinking || (decision.language === 'en-US'
    ? `I need to connect the public night result with concrete speech and vote evidence while protecting my role.`
    : `需要把公开夜间结果与具体发言、票型联系起来，同时根据身份规划是否暴露信息。`)).trim()
  return {
    speech,
    thinking,
    language: decision.language,
    explode: Boolean(decision.explode),
    target: decision.target,
    useKnight: Boolean(decision.use),
    claimRole: decision.claimRole || '',
    passDelaySeconds: Number(decision.pass_delay_seconds) || 1.4
  }
}

const runAudienceThinking = async (speaker, speech, version) => {
  const observers = alivePlayers.value.filter(player => player.id !== speaker.id)
  await Promise.allSettled(observers.map(async observer => {
    if (!isLoopActive(version)) return
    const decision = await requestPlayerDecision(observer, 'observeSpeech', {
      speakerNumber: `${speaker.playerNumber}号`,
      speech
    })
    if (!isLoopActive(version)) return
    const fallback = currentLocale() === 'zh-CN'
      ? `旁听${speaker.playerNumber}号：把这段发言与其公开身份声明、上一轮口径和票型对照，暂时保留对${speaker.playerNumber}号的判断。`
      : `Listening to player ${speaker.playerNumber}: compare this speech with their public claim, prior wording, and vote record before changing my read.`
    const thinking = String(decision.thinking || fallback).trim()
    playerMemories[observer.id].observations.push({ day: currentDay.value, speakerId: speaker.id, thinking })
    playerMemories[observer.id].observations = playerMemories[observer.id].observations.slice(-10)
    recordPrivateThinking(observer, thinking, `旁听${speaker.playerNumber}号`)
  }))
}

const createRandomSpeechOrder = () => {
  const ordered = [...alivePlayers.value].sort((a, b) => a.playerNumber - b.playerNumber)
  if (!ordered.length) return { ids: [], direction: 'clockwise', start: null }
  const sheriff = alivePlayers.value.find(player => player.isSheriff)
  const clockwise = sheriff ? sheriffDirection.value === 'clockwise' : Math.random() < 0.5
  const startIndex = sheriff
    ? ordered.findIndex(player => player.id === sheriff.id) + (clockwise ? 1 : -1)
    : Math.floor(Math.random() * ordered.length)
  const ids = []
  const total = sheriff ? ordered.length - 1 : ordered.length
  for (let offset = 0; offset < total; offset++) {
    const index = clockwise
      ? (startIndex + offset) % ordered.length
      : (startIndex - offset + ordered.length) % ordered.length
    if (ordered[index]?.id !== sheriff?.id) ids.push(ordered[index].id)
  }
  if (sheriff) ids.push(sheriff.id)
  const normalizedStartIndex = (startIndex + ordered.length) % ordered.length
  return { ids, direction: clockwise ? 'clockwise' : 'counterclockwise', start: ordered[normalizedStartIndex] }
}

const formatPublicVoteSummary = (ballots, voters) => {
  const groups = new Map()
  ballots.forEach(ballot => {
    if (!groups.has(ballot.targetId)) groups.set(ballot.targetId, [])
    groups.get(ballot.targetId).push(ballot.voterId)
  })
  const grouped = [...groups.entries()]
    .sort((a, b) => Number(getPlayerNumberById(a[0])) - Number(getPlayerNumberById(b[0])))
    .map(([targetId, voterIds]) => `${getPlayerNumberById(targetId)}号：${voterIds.map(id => {
      const voter = players.value.find(player => player.id === id)
      return `${getPlayerNumberById(id)}号${voter?.isSheriff ? '（警长1.5票）' : ''}`
    }).join('、')}`)
  const votedIds = new Set(ballots.map(ballot => ballot.voterId))
  const abstainers = voters.filter(voter => !votedIds.has(voter.id)).map(voter => `${voter.playerNumber}号`)
  if (abstainers.length) grouped.push(`弃票：${abstainers.join('、')}`)
  return grouped.join('；') || '无人投出有效票'
}

const collectSheriffVote = async (version, candidates, voters, label, mustChoose = false) => {
  const ballots = []
  const decisions = await runDecisionWindow(version, voters, 'sheriffVote', voter => {
    const options = candidates.filter(candidate => candidate.id !== voter.id)
    return { candidates: options.map(player => `${player.playerNumber}号`).join('、'), mustChoose }
  }, label)
  for (const voter of voters) {
    if (!isLoopActive(version)) break
    const options = candidates.filter(candidate => candidate.id !== voter.id)
    if (!options.length) continue
    const decision = decisions.get(voter.id) || {}
    recordPrivateThinking(voter, decision.thinking, label)
    const selectedTarget = resolvePlayerTarget(decision.target, options)
    const target = selectedTarget || ((mustChoose || decision.usedLocalFallback) ? randomItem(options) : null)
    if (!target || (!mustChoose && decision.abstain === true)) {
      addGameMessage({ sender: `${voter.playerNumber}号 ${voter.playerName}`, content: '警徽投票弃票（压手）', type: 'vote-action', visibility: 'private', privateFor: voter.id })
      continue
    }
    ballots.push({ voterId: voter.id, targetId: target.id })
    addGameMessage({ sender: `${voter.playerNumber}号 ${voter.playerName}`, content: `警徽票投给${target.playerNumber}号${target.playerName}`, type: 'vote-action', visibility: 'private', privateFor: voter.id })
  }
  const counts = new Map(candidates.map(candidate => [candidate.id, 0]))
  ballots.forEach(ballot => counts.set(ballot.targetId, (counts.get(ballot.targetId) || 0) + 1))
  const max = Math.max(0, ...counts.values())
  const leaders = candidates.filter(candidate => counts.get(candidate.id) === max && max > 0)
  await addRefereeMessage(`${label}票型：${formatPublicVoteSummary(ballots, voters)}。`)
  return { ballots, counts, leaders, max }
}

const runSheriffElection = async version => {
  if (!boardRules.value.sheriff || sheriffElectionDone.value || !isLoopActive(version)) return
  currentPhase.value = 'sheriff'
  players.value.forEach(player => { player.isSheriffCandidate = false })
  await addRefereeMessage('第一天天亮后进入警长竞选：先上警，再随机发言，随后退水，最后只由警下玩家投票。第一次警徽投票允许弃票，狼人杀中俗称“压手”；警上未退水者没有警徽投票权。')
  const campaignPlans = new Map()
  const candidates = []
  const campaignDecisions = await runDecisionWindow(version, [...alivePlayers.value], 'sheriffCampaign', () => ({}), '上警决策')
  for (const player of [...alivePlayers.value]) {
    const decision = campaignDecisions.get(player.id) || {}
    recordPrivateThinking(player, decision.thinking, '警长竞选')
    const run = decision.run === undefined ? hasRole(player, 'seer') : Boolean(decision.run)
    if (run) {
      player.isSheriffCandidate = true
      candidates.push(player)
      campaignPlans.set(player.id, decision.direction === 'counterclockwise' ? 'counterclockwise' : 'clockwise')
    }
  }
  if (!candidates.length) {
    players.value.forEach(player => { player.isSheriffCandidate = false })
    sheriffBadgeLost.value = true
    sheriffElectionDone.value = true
    await addRefereeMessage('本轮没有玩家上警，警徽流失，本局不再产生警长。')
    return
  }
  await addRefereeMessage(`本次上警玩家：${candidates.map(player => `${player.playerNumber}号`).join('、')}。`)
  const campaignSpeech = await runSpeechPhase(version, shuffleArray([...candidates]).map(player => player.id), 'sheriffSpeech')
  if (campaignSpeech?.interrupted || dayInterrupted.value) {
    players.value.forEach(player => { player.isSheriffCandidate = false })
    sheriffBadgeLost.value = true
    sheriffElectionDone.value = true
    addRefereeMessage('竞选发言中发生狼人自爆，警徽被吞，竞选立即中止并进入黑夜。')
    return
  }
  if (!isLoopActive(version)) return
  await addRefereeMessage('警上发言结束，开始退水。退水玩家恢复警徽投票权，但失去警长候选资格。')
  const finalCandidates = []
  const withdrawDecisions = await runDecisionWindow(version, candidates, 'sheriffWithdraw', () => ({}), '退水决策')
  for (const player of candidates) {
    const decision = withdrawDecisions.get(player.id) || {}
    recordPrivateThinking(player, decision.thinking, '警长退水')
    const withdraw = decision.withdraw === undefined ? false : Boolean(decision.withdraw)
    if (withdraw) {
      player.isSheriffCandidate = false
      addRefereeMessage(`${player.playerNumber}号退水，恢复警徽投票权。`)
    } else {
      finalCandidates.push(player)
    }
  }
  if (!finalCandidates.length) {
    players.value.forEach(player => { player.isSheriffCandidate = false })
    sheriffBadgeLost.value = true
    sheriffElectionDone.value = true
    await addRefereeMessage('所有警上玩家都已退水，警徽流失。')
    return
  }
  if (finalCandidates.length === 1) {
    const winner = finalCandidates[0]
    setSheriff(winner.id)
    sheriffDirection.value = campaignPlans.get(winner.id) || 'clockwise'
    axios.put('/game/player/setSheriff', { roomId, playerId: winner.id }).catch(() => {})
    await addRefereeMessage(`${winner.playerNumber}号成为唯一未退水候选人，当选警长。`)
    sheriffElectionDone.value = true
    return
  }
  const voters = alivePlayers.value.filter(player => !finalCandidates.some(candidate => candidate.id === player.id) && !idiotFlippedIds.has(player.id))
  let voteResult = await collectSheriffVote(version, finalCandidates, voters, '警徽投票', false)
  let leaders = voteResult.leaders
  if (!leaders.length) {
    sheriffBadgeLost.value = true
    players.value.forEach(player => { player.isSheriffCandidate = false })
    await addRefereeMessage('警徽投票无人获得有效票，警徽流失，本局不再产生警长。')
    sheriffElectionDone.value = true
    return
  }
  if (leaders.length > 1) {
    await addRefereeMessage(`警徽投票平票：${leaders.map(player => `${player.playerNumber}号`).join('、')}，进入第一轮PK发言。`)
    const pk = await runSpeechPhase(version, shuffleArray([...leaders]).map(player => player.id), 'sheriffPkSpeech')
    if (pk?.interrupted || dayInterrupted.value) {
      players.value.forEach(player => { player.isSheriffCandidate = false })
      sheriffBadgeLost.value = true
      sheriffElectionDone.value = true
      return
    }
    voteResult = await collectSheriffVote(version, leaders, voters, '第一次警徽PK重投', true)
    leaders = voteResult.leaders
    if (leaders.length > 1) {
      await addRefereeMessage(`第一次警徽PK重投仍然平票：${leaders.map(player => `${player.playerNumber}号`).join('、')}，进入第二轮PK发言后进行最终重投。`)
      const secondPk = await runSpeechPhase(version, shuffleArray([...leaders]).map(player => player.id), 'sheriffPkSpeech')
      if (secondPk?.interrupted || dayInterrupted.value) {
        players.value.forEach(player => { player.isSheriffCandidate = false })
        sheriffBadgeLost.value = true
        sheriffElectionDone.value = true
        return
      }
      voteResult = await collectSheriffVote(version, leaders, voters, '最终警徽PK重投', true)
      leaders = voteResult.leaders
    }
  }
  const winner = leaders.length === 1 ? leaders[0] : null
  if (winner) {
    setSheriff(winner.id)
    sheriffDirection.value = campaignPlans.get(winner.id) || 'clockwise'
    axios.put('/game/player/setSheriff', { roomId, playerId: winner.id }).catch(() => {})
    await addRefereeMessage(`${winner.playerNumber}号${winner.playerName}当选警长，之后由其决定${sheriffDirection.value === 'clockwise' ? '顺时针' : '逆时针'}发言。`)
  } else {
    sheriffBadgeLost.value = true
    players.value.forEach(player => { player.isSheriffCandidate = false })
    await addRefereeMessage('最终警徽PK重投仍然平票，警徽流失，本局不再产生警长。')
  }
  sheriffElectionDone.value = true
}

const runSpeechPhase = async (version, onlyCandidates = null, action = 'speech') => {
  currentPhase.value = 'speak'
  let orderInfo
  let resumeIndex = 0
  if (!onlyCandidates && action === 'speech' && resumeStage.value === 'speech' && speechOrder.value.length && speechIndex.value >= 0) {
    orderInfo = { ids: speechOrder.value.filter(id => players.value.find(player => player.id === id)?.isAlive), direction: sheriffDirection.value, start: null }
    resumeIndex = Math.max(0, speechIndex.value)
  } else if (onlyCandidates) {
    orderInfo = { ids: onlyCandidates.filter(id => players.value.find(p => p.id === id)?.isAlive), direction: 'pk', start: null }
  } else {
    orderInfo = createRandomSpeechOrder()
    speechOrder.value = orderInfo.ids
    const directionText = orderInfo.direction === 'clockwise' ? '顺时针' : '逆时针'
    const sheriff = alivePlayers.value.find(player => player.isSheriff)
    await addRefereeMessage(sheriff
      ? `警长${sheriff.playerNumber}号决定从自己${orderInfo.direction === 'clockwise' ? '右侧' : '左侧'}开始，按${directionText}发言，警长末置位归票。顺序：${orderInfo.ids.map(getPlayerNumberById).join('→')}号。`
      : `进入发言阶段。本轮由代码随机决定从${orderInfo.start?.playerNumber}号开始，按${directionText}发言。顺序：${orderInfo.ids.map(getPlayerNumberById).join('→')}号。`)
  }
  for (let index = resumeIndex; index < orderInfo.ids.length; index++) {
    if (!isLoopActive(version)) break
    speechIndex.value = index
    const player = players.value.find(p => p.id === orderInfo.ids[index] && p.isAlive)
    if (!player) continue
    if (player.id === silencedPlayerId.value) {
      addRefereeMessage(`${player.playerNumber}号被禁言长老禁言，本轮跳过发言。`)
      if (!onlyCandidates && action === 'speech') {
        speechIndex.value = index + 1
        persistGameSnapshot('speech')
      }
      continue
    }
    await startPlayerSpeaking(player.id)
    const result = await generatePublicSpeech(player, action)
    if (!isLoopActive(version)) break
    if (['sheriffSpeech', 'sheriffPkSpeech'].includes(action) && isPackWolf(player) && result.explode) {
      killPlayer(player.id)
      dayInterrupted.value = true
      sheriffBadgeLost.value = true
      addRefereeMessage(`${player.playerNumber}号狼人于警长竞选发言中自爆，发言与竞选立即终止，直接进入黑夜。`)
      await resolveDeathEffects(player, version, 'explode')
      await endPlayerSpeaking()
      speechIndex.value = -1
      return { interrupted: true, explodingPlayerId: player.id }
    }
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
      await endPlayerSpeaking()
      break
    }
    recordPrivateThinking(player, result.thinking, ['pkSpeech', 'sheriffPkSpeech'].includes(action) ? 'PK发言' : (action === 'sheriffSpeech' ? '警上发言' : '白天发言'))
    playerMemories[player.id]?.speeches.push({ day: currentDay.value, speech: result.speech })
    const audienceThinking = runAudienceThinking(player, result.speech, version)
    const speechMessage = addDialogMessage(player.playerName, result.speech, 'player', { hiddenDuringSpeech: true })
    if (!onlyCandidates && action === 'speech') {
      speechIndex.value = index + 1
      resumeStage.value = 'speech'
      persistGameSnapshot('speech')
    }
    await presentSpeech(player, result.speech, result.thinking, result.language, result.passDelaySeconds)
    speechMessage.hiddenDuringSpeech = false
    if (!isLoopActive(version)) return { interrupted: true, aborted: true }
    await audienceThinking
    await endPlayerSpeaking()
    await phaseDelay(260)
  }
  speechIndex.value = -1
  if (!onlyCandidates && isLoopActive(version)) await addRefereeMessage('所有存活玩家发言完毕，主持人现在进入放逐投票。')
  return { interrupted: false }
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

const collectVoteRound = async (version, candidateIds = null, label = '公投', mustChoose = false) => {
  const eligibleCandidates = alivePlayers.value.filter(player => !candidateIds || candidateIds.includes(player.id))
  const ballots = []
  const voters = [...alivePlayers.value].filter(voter => !idiotFlippedIds.has(voter.id))
  alivePlayers.value.filter(voter => idiotFlippedIds.has(voter.id)).forEach(voter => {
    addRefereeMessage(`${voter.playerNumber}号白痴已翻牌，失去投票权。`, { visibility: 'god' })
  })
  const decisions = await runDecisionWindow(version, voters, 'vote', voter => {
    const candidates = eligibleCandidates.filter(candidate => candidate.id !== voter.id)
    return { candidates: candidates.map(p => `${p.playerNumber}号`).join('、'), mustChoose }
  }, label)
  for (const voter of voters) {
    if (!isLoopActive(version)) break
    const candidates = eligibleCandidates.filter(candidate => candidate.id !== voter.id)
    if (!candidates.length) continue
    const decision = decisions.get(voter.id) || {}
    recordPrivateThinking(voter, decision.thinking, `${label}投票`)
    const selectedTarget = resolvePlayerTarget(decision.target, candidates)
    const target = selectedTarget || ((mustChoose || decision.usedLocalFallback) ? randomItem(candidates) : null)
    if (!target || (!mustChoose && decision.abstain === true)) {
      playerMemories[voter.id]?.votes.push({ day: currentDay.value, targetId: null, label, abstained: true })
      addGameMessage({ sender: `${voter.playerNumber}号 ${voter.playerName}`, content: '本轮弃票', type: 'vote-action', visibility: 'private', privateFor: voter.id })
      continue
    }
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
  const abstainVoterIds = voters.filter(voter => !ballots.some(ballot => ballot.voterId === voter.id)).map(voter => voter.id)
  voteHistory.value.push({ day: currentDay.value, label, ballots, leaders, abstainVoterIds })
  await addRefereeMessage(`${label}票型：${formatPublicVoteSummary(ballots, voters)}。`)
  return { ballots, leaders, max }
}

const giveLastWords = async (player, version, reason = 'exile') => {
  if (!player || !isLoopActive(version)) return
  if (reason === 'exile' && (currentDay.value !== 1 || firstDayLastWordsGiven.value)) {
    await addRefereeMessage(`${player.playerNumber}号${player.playerName}被公投出局。`)
    return
  }
  if (reason === 'night' && (currentRound.value !== 1 || Number(boardRules.value.players) !== 12 || firstNightLastWordsPlayerIds.has(player.id))) return
  if (reason === 'night') firstNightLastWordsPlayerIds.add(player.id)
  else firstDayLastWordsGiven.value = true
  await addRefereeMessage(reason === 'night'
    ? `${player.playerNumber}号${player.playerName}是首夜出局玩家，请发表遗言。`
    : `${player.playerNumber}号${player.playerName}被公投出局，请发表遗言。`)
  await startPlayerSpeaking(player.id, false)
  const speechAction = reason === 'night' ? 'nightLastWords' : 'lastWords'
  const result = await generatePublicSpeech(player, speechAction)
  recordPrivateThinking(player, result.thinking, '遗言')
  await presentSpeech(player, result.speech, result.thinking, result.language)
  addDialogMessage(player.playerName, result.speech, 'player')
  await endPlayerSpeaking()
}

const resolveStalkerAfterVote = async (version, ballots, exileId) => {
  if (!isBoard('stalker_silencer') || stalkerState.used || !isLoopActive(version)) return
  const stalker = alivePlayers.value.find(player => hasRole(player, 'stalker'))
  if (!stalker) return
  const ballot = ballots.find(item => item.voterId === stalker.id)
  const target = players.value.find(player => player.id === ballot?.targetId && player.isAlive && player.id !== exileId)
  if (!target) return
  const decision = await requestPlayerDecision(stalker, 'stalker', { candidates: `${target.playerNumber}号` })
  recordPrivateThinking(stalker, decision.thinking, '潜行者刺杀')
  if (decision.use === false) return
  stalkerState.used = true
  stalkerState.pendingTargetId = target.id
  killPlayer(target.id)
  addRefereeMessage(`潜行者发动每局一次的刺杀，带走了其投票但未被放逐的${target.playerNumber}号${target.playerName}。`)
  await resolveDeathEffects(target, version, 'stalker')
}

const resolveBomberExile = async (bomber, version, ballots) => {
  killPlayer(bomber.id)
  const voters = ballots
    .filter(ballot => ballot.targetId === bomber.id)
    .map(ballot => players.value.find(player => player.id === ballot.voterId))
    .filter(player => player?.isAlive)
  voters.forEach(player => {
    bomberSuppressedIds.add(player.id)
    killPlayer(player.id)
  })
  addRefereeMessage(`${bomber.playerNumber}号炸弹人被放逐后翻牌爆炸，带走所有投给自己的玩家：${voters.map(player => `${player.playerNumber}号`).join('、') || '无人'}。被带走者不能发动角色技能。`)
  await resolveDeathEffects(bomber, version, 'exile')
  for (const voter of voters) await resolveDeathEffects(voter, version, 'bomber', { suppressSkills: true })
}

const runVotePhase = async (version) => {
  currentPhase.value = 'vote'
  await runKnightAction(version)
  if (dayInterrupted.value || !isLoopActive(version)) return
  await addRefereeMessage('主持人发起常规放逐投票。所有存活且拥有投票权的玩家同时私下决定；本轮允许弃票，但弃票会公开计入票型。')
  let result = await collectVoteRound(version, null, '公投', false)
  if (!isLoopActive(version) || !result.leaders.length) {
    await addRefereeMessage('没有形成有效投票，本日无人出局。')
    return
  }
  let exileId = result.leaders.length === 1 ? result.leaders[0] : null
  if (!exileId) {
    const tiedNumbers = result.leaders.map(getPlayerNumberById).join('号、') + '号'
    await addRefereeMessage(`${tiedNumbers}平票，进入第一轮PK发言；PK后所有有投票权的存活玩家必须从PK台中选择，不得弃票。`)
    await runSpeechPhase(version, result.leaders, 'pkSpeech')
    currentPhase.value = 'vote'
    result = await collectVoteRound(version, result.leaders, '第一次PK重投', true)
    exileId = result.leaders.length === 1 ? result.leaders[0] : null
    if (!exileId) {
      const secondPkIds = [...result.leaders]
      await addRefereeMessage(`第一次PK重投仍然平票，${secondPkIds.map(getPlayerNumberById).join('号、')}号进入第二轮PK发言，之后进行最终重投。`)
      await runSpeechPhase(version, secondPkIds, 'pkSpeech')
      currentPhase.value = 'vote'
      result = await collectVoteRound(version, secondPkIds, '最终PK重投', true)
      exileId = result.leaders.length === 1 ? result.leaders[0] : null
      if (!exileId) {
        await addRefereeMessage('最终PK重投仍然平票，本日无人出局。')
        return
      }
    }
  }
  const exiled = players.value.find(player => player.id === exileId)
  if (hasRole(exiled, 'idiot') && !idiotFlippedIds.has(exiled.id)) {
    idiotFlippedIds.add(exiled.id)
    exiled.isAlive = true
    await addRefereeMessage(`${exiled.playerNumber}号白痴被投票放逐，翻牌免死；此后仍可发言但失去投票权。`)
    await resolveStalkerAfterVote(version, result.ballots, exileId)
    return
  }
  if (hasRole(exiled, 'bomber')) {
    await resolveBomberExile(exiled, version, result.ballots)
    await resolveStalkerAfterVote(version, result.ballots, exileId)
    return
  }
  killPlayer(exileId)
  lastExiledPlayerId.value = exileId
  await giveLastWords(exiled, version)
  await resolveDeathEffects(exiled, version, 'exile')
  await resolveStalkerAfterVote(version, result.ballots, exileId)
}

const runQueuedExtraPhases = async version => {
  while (bonusDayPending.value > 0 && isLoopActive(version)) {
    bonusDayPending.value--
    currentDay.value++
    silencedPlayerId.value = null
    currentPhase.value = 'day'
    addRefereeMessage(`太阳效果发动：现在是额外的第${currentDay.value}天，不经过黑夜。`)
    await runSpeechPhase(version)
    if (!isLoopActive(version)) return
    await runVotePhase(version)
    if (checkGameEnd()) return
  }
  while (bonusNightPending.value > 0 && isLoopActive(version)) {
    bonusNightPending.value--
    currentRound.value++
    currentDay.value++
    addRefereeMessage(`月亮效果发动：进入额外的第${currentRound.value}夜。`)
    await runNightPhase(version)
    if (!isLoopActive(version)) return
    await announceDay(version)
    if (checkGameEnd()) return
    if (boardRules.value.sheriff && !sheriffElectionDone.value) await runSheriffElection(version)
    await runSpeechPhase(version)
    if (!isLoopActive(version)) return
    await runVotePhase(version)
    if (checkGameEnd()) return
  }
}

const saveLoopCheckpoint = stage => {
  resumeStage.value = stage
  persistGameSnapshot(stage)
}

const runGameLoop = async (version, initialStage = 'night') => {
  if (phaseRunning.value) return
  phaseRunning.value = true
  let stage = initialStage || 'night'
  await phaseDelay(500)
  try {
    while (isLoopActive(version)) {
      await waitWhilePaused()
      if (stage === 'night') {
        await runNightPhase(version)
        if (!isLoopActive(version)) break
        stage = boardRules.value.sheriff && !sheriffElectionDone.value ? 'sheriff' : 'announce'
        saveLoopCheckpoint(stage)
      }
      if (stage === 'sheriff') {
        await runSheriffElection(version)
        if (!isLoopActive(version)) break
        if (dayInterrupted.value) {
          await settleNightWithoutAnnouncement(version)
          if (checkGameEnd() || !isLoopActive(version)) break
          dayInterrupted.value = false
          currentRound.value++
          currentDay.value++
          stage = 'night'
          saveLoopCheckpoint(stage)
          await phaseDelay(700)
          continue
        }
        if (checkGameEnd()) break
        stage = 'announce'
        saveLoopCheckpoint(stage)
      }
      if (stage === 'announce') {
        await announceDay(version)
        if (checkGameEnd() || !isLoopActive(version)) break
        speechOrder.value = []
        speechIndex.value = -1
        stage = 'speech'
        saveLoopCheckpoint(stage)
      }
      if (stage === 'speech') {
        await runSpeechPhase(version, null, 'speech')
        if (!isLoopActive(version)) break
        if (dayInterrupted.value) {
          if (checkGameEnd() || !isLoopActive(version)) break
          dayInterrupted.value = false
          currentRound.value++
          currentDay.value++
          stage = 'night'
          saveLoopCheckpoint(stage)
          await phaseDelay(700)
          continue
        }
        stage = 'vote'
        saveLoopCheckpoint(stage)
      }
      if (stage === 'vote') {
        await runVotePhase(version)
        if (checkGameEnd() || !isLoopActive(version)) break
        await runQueuedExtraPhases(version)
        if (checkGameEnd() || !isLoopActive(version)) break
        silencedPlayerId.value = null
        currentRound.value++
        currentDay.value++
        stage = 'night'
        saveLoopCheckpoint(stage)
        await phaseDelay(700)
      }
    }
  } catch (error) {
    console.error('Game loop failed:', error)
    addRefereeMessage('游戏流程发生异常，已暂停。', { visibility: 'public', detail: error.message })
  } finally {
    if (gameLoopVersion === version) phaseRunning.value = false
  }
}

onMounted(async () => {
  loadPlayerVoiceConfigs()
  loadAiProfileVoiceConfigs()
  loadBrowserVoices()
  window.speechSynthesis?.addEventListener('voiceschanged', loadBrowserVoices)
  const restored = await loadGameData({ restore: true })
  if (restored && gameStarted.value) {
    if (isRoomOwner.value) {
      ElMessage.success('已恢复本房间的游戏状态，将从上次安全节点继续。')
      const version = ++gameLoopVersion
      void runGameLoop(version, resumeStage.value)
    } else {
      ElMessage.info('已恢复本房间的视角状态；游戏流程由房主推进。')
    }
  }
})
onUnmounted(() => {
  const shouldPersist = gameStarted.value
  gameLoopVersion++
  phaseRunning.value = false
  if (decisionWindowTimer) clearInterval(decisionWindowTimer)
  decisionWindowTimer = null
  decisionWindow.active = false
  isGamePaused.value = false
  releasePauseWaiters()
  closeSpeech('unmount')
  stopSpeaking()
  if (shouldPersist) persistGameSnapshot(resumeStage.value)
  gameStarted.value = false
  window.speechSynthesis?.removeEventListener('voiceschanged', loadBrowserVoices)
})
</script>

<style scoped>
.game-play {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
  overflow: hidden;
}

.role-deal-overlay {
  position: fixed; inset: 0; z-index: 2400; display: grid; place-items: center;
  background: rgba(4, 8, 12, .94); backdrop-filter: blur(12px);
}
.deal-stage { display: grid; place-items: center; gap: 42px; width: min(720px, 92vw); text-align: center; }
.deal-deck { position: relative; width: 210px; height: 260px; perspective: 1100px; }
.deal-card {
  --offset: calc(var(--card-index) * 17px);
  position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px;
  border: 1px solid rgba(222, 190, 112, .7); border-radius: 8px;
  background: repeating-linear-gradient(45deg, #142532 0 8px, #10202b 8px 16px);
  box-shadow: 0 18px 48px rgba(0, 0, 0, .56); transform-origin: 50% 110%;
  transform: translateX(var(--offset)) rotate(calc(var(--card-index) * 2deg)); transition: transform .75s ease, opacity .55s ease;
}
.deal-card::before { content: ''; position: absolute; inset: 10px; border: 1px solid rgba(222, 190, 112, .3); border-radius: 5px; }
.deal-card span { color: #e6c56f; font: 700 34px/1 var(--font-heading); }
.deal-card strong { color: #b9c9d4; font: 700 10px/1 var(--font-heading); }
.deal-stage.shuffle .deal-card { animation: dealShuffle .72s ease-in-out infinite alternate; }
.deal-stage.spread .deal-card { transform: translateX(calc(var(--card-index) * 62px)) rotate(calc(var(--card-index) * 9deg)); }
.deal-stage.complete .deal-card { transform: translateY(-32px) scale(.92); opacity: .88; }
.deal-copy span { display: block; margin-bottom: 12px; color: #8297a8; font: 700 10px/1 var(--font-heading); }
.deal-copy strong { display: block; color: #f0d488; font: 700 24px/1.35 var(--font-heading); }
.deal-copy small { display: block; margin-top: 10px; color: #a8b7c2; }
.deal-fade-enter-active, .deal-fade-leave-active { transition: opacity .35s ease; }
.deal-fade-enter-from, .deal-fade-leave-to { opacity: 0; }
@keyframes dealShuffle { from { transform: translateX(calc(var(--offset) - 8px)) rotate(calc(var(--card-index) * 1deg)); } to { transform: translateX(calc(var(--offset) + 8px)) rotate(calc(var(--card-index) * 3deg)); } }
@keyframes pulse { 50% { opacity: .45; transform: scale(.8); } }

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
  flex: 0 0 auto;
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
  flex: 0 0 auto;
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
  overflow: hidden;
}

/* ===== Side Panels ===== */
.side-panel {
  height: 100%; min-height: 0; overflow: hidden;
}
.player-badges { display: grid; grid-template-rows: repeat(6, minmax(0, 1fr)); gap: 8px; align-items: stretch; justify-items: center; height: 100%; }

.player-badge {
  width: min(100%, 154px); height: 100%; min-height: 0; max-height: 104px; cursor: pointer; transition: all var(--transition-normal);
  position: relative;
}
.player-badge:hover { transform: scale(1.04); }
.player-badge.viewing .badge-frame { border-color: var(--gold) !important; box-shadow: 0 0 0 2px rgba(224, 187, 91, .32), 0 0 22px rgba(224, 187, 91, .18); }
.player-badge.speaking .badge-frame { border-color: var(--gold) !important; animation: glow 2s ease-in-out infinite; }
.player-badge.dead .badge-frame { opacity: 0.5; filter: grayscale(0.6); }

.badge-frame {
  background: var(--bg-card); border: 1px solid rgba(201,169,110,0.2);
  border-radius: 7px; padding: 6px 8px; text-align: center;
  position: relative; overflow: hidden; height: 100%; min-height: 72px;
  display: grid; grid-template-rows: 1fr auto; align-items: center;
}
.badge-frame::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; height: 2px;
  background: linear-gradient(90deg, transparent, var(--gold-dark), transparent); opacity: 0.4;
}
.badge-ring {
  position: absolute; inset: 3px; border-radius: 5px;
  border: 1px solid rgba(201,169,110,0.1); pointer-events: none;
}
.badge-num {
  position: absolute; left: 7px; bottom: 5px; z-index: 2; font-family: var(--font-heading); font-size: 0.65rem;
  color: var(--text-muted);
}
.badge-name {
  display: block; min-width: 0; font-family: var(--font-heading); font-size: 0.72rem;
  font-weight: 600; color: var(--text-primary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.badge-avatar {
  align-self: center; justify-self: center; display: flex; align-items: center; justify-content: center; overflow: hidden;
  width: 52px; height: 52px; border: 1px solid rgba(201,169,110,.36); border-radius: 50%;
  background: #18242d; color: #e1c271; font: 700 10px/1.1 var(--font-heading);
}
.badge-avatar img { width: 100%; height: 100%; object-fit: cover; }
.badge-avatar span { width: 44px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.badge-footer { display: flex; align-items: center; justify-content: center; gap: 5px; min-width: 0; padding: 0 17px; }
.badge-crown-top { position: absolute; top: 4px; left: 6px; z-index: 3; color: #f1ce70; font-size: 0.95rem; text-shadow: 0 0 10px rgba(241, 206, 112, .5); }
.badge-hand { position: absolute; top: 5px; left: 6px; z-index: 3; font-size: 0.85rem; }
.badge-voice { position: absolute; right: 5px; bottom: 5px; z-index: 4; display: grid; place-items: center; width: 22px; height: 22px; padding: 0; border: 1px solid rgba(222, 190, 112, .35); border-radius: 50%; color: #e8c870; background: rgba(9, 18, 26, .8); cursor: pointer; font-size: .7rem; }
.badge-voice:hover { border-color: var(--gold); background: rgba(222, 190, 112, .18); }
.badge-skull { position: absolute; inset: 0; z-index: 4; display: grid; place-items: center; color: #ef8e84; background: rgba(7, 9, 11, .45); font: 700 2rem/1 var(--font-heading); }
.badge-role {
  position: absolute; top: 5px; right: 5px; z-index: 3; display: inline-block; max-width: 74px; padding: 2px 5px; border-radius: 3px;
  font-size: 0.56rem; font-family: var(--font-heading); font-weight: 600; overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
}
.badge-role.wolf { background: rgba(139,0,0,0.3); color: #f66; }
.badge-role.villager { background: rgba(180,180,180,0.15); color: #bbb; border: 1px solid rgba(180,180,180,0.3); }
.badge-role.god { background: rgba(201,169,110,0.15); color: var(--gold); }

.badge-empty {
  background: var(--bg-input); border: 1px dashed rgba(201,169,110,0.2);
  border-radius: 7px; padding: 16px 8px; text-align: center; height: 100%;
  color: var(--text-muted); font-size: 0.75rem; transition: all var(--transition-normal);
}
.badge-empty:hover { border-color: var(--gold); color: var(--gold); }
.badge-plus { display: block; font-size: 1.4rem; margin-bottom: 2px; }

.badge-locked {
  background: rgba(13,10,8,0.5); border: 1px solid rgba(201,169,110,0.06);
  border-radius: 7px; padding: 16px 8px; text-align: center; height: 100%;
  font-size: 1.2rem; opacity: 0.3;
}

/* ===== Center: Chat ===== */
.center-panel { display: flex; flex-direction: column; min-height: 0; height: 100%; gap: 8px; overflow: hidden; }

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

.decision-window { display: flex; flex: 0 0 auto; align-items: center; justify-content: space-between; gap: 12px; min-height: 34px; padding: 6px 11px; border: 1px solid rgba(217, 181, 93, .35); border-radius: 5px; color: #e8d49d; background: rgba(108, 78, 26, .2); }
.decision-window > div { display: flex; align-items: center; gap: 8px; min-width: 0; }
.decision-window small { color: #9d8e6f; font-size: .68rem; }
.decision-dot { width: 7px; height: 7px; border-radius: 50%; background: #e8c870; box-shadow: 0 0 0 4px rgba(232, 200, 112, .12); animation: pulse 1.2s ease-in-out infinite; }
.decision-progress { color: #b9c6ce; font: 700 .72rem/1 var(--font-heading); }
.decision-countdown { min-width: 42px; color: #f2ce72; font: 700 .95rem/1 var(--font-heading); text-align: right; }
.thinking-bar { display: flex; flex: 0 0 auto; gap: 8px; flex-wrap: nowrap; min-height: 28px; overflow-x: auto; overflow-y: hidden; }
.thinking-item {
  display: flex; align-items: center; gap: 6px;
  padding: 4px 12px; border-radius: 12px;
  background: rgba(122,110,94,0.1); border: 1px solid rgba(122,110,94,0.2);
  flex: 0 0 auto; font-size: 0.8rem; color: var(--text-secondary);
}
.thinking-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--gold); animation: blink 1s infinite;
}

/* Speech card */
.speech-flash {
  flex: 0 0 clamp(150px, 30vh, 280px); min-height: 150px; max-height: 42%;
  animation: fadeIn 0.3s ease; overflow-x: hidden; overflow-y: auto; scrollbar-gutter: stable;
}
.speech-card {
  background: var(--bg-card); border: 1px solid var(--gold-dark);
  border-radius: 7px; padding: 13px; min-height: 100%; box-shadow: var(--shadow-gold);
}
.speech-header { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.speech-avatar { display: flex; align-items: center; justify-content: center; overflow: hidden; width: 42px; height: 42px; flex: 0 0 42px; border: 1px solid rgba(201,169,110,.4); border-radius: 50%; background: #18242d; color: #e1c271; font: 700 9px/1 var(--font-heading); }
.speech-avatar img { width: 100%; height: 100%; object-fit: cover; }
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
.auto-pass-status { align-self: center; color: var(--text-muted); font-size: .7rem; }
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
  flex: 1 1 auto;
  height: auto;
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

.voice-switch-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; padding: 12px 14px; border: 1px solid rgba(180, 204, 222, .15); border-radius: 6px; background: rgba(5, 12, 19, .34); }
.voice-switch-row strong, .voice-switch-row span { display: block; }
.ledger-ai-status small { display: block; margin-top: 4px; color: #c38b8b; font-size: 11px; }
.voice-switch-row strong { color: #e8eef3; font-size: 13px; }
.voice-switch-row span { margin-top: 4px; color: #8294a3; font-size: 12px; }
.player-voice-sliders { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.voice-slider { display: flex; align-items: center; gap: 8px; }
.voice-slider :deep(.el-slider) { flex: 1; min-width: 70px; }
.voice-slider > span { min-width: 34px; color: #e4bd65; font-size: 12px; text-align: right; }

/* Bubble */
.bubble-row { display: flex; gap: 10px; align-items: flex-start; animation: fadeIn 0.3s ease; }
.bubble-row.mine { flex-direction: row-reverse; }
.bubble-avatar { display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0; width: 36px; height: 36px; border-radius: 50%; background: #18242d; color: #e1c271; font: 700 8px/1 var(--font-heading); text-align: center; }
.bubble-avatar img { width: 100%; height: 100%; object-fit: cover; }
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

@media (max-width: 1100px) {
  .game-layout { grid-template-columns: 1fr; grid-template-rows: 82px minmax(0, 1fr) 82px; gap: 8px; padding: 8px; }
  .player-badges { grid-template-columns: repeat(6, minmax(0, 1fr)); grid-template-rows: 1fr; gap: 6px; }
  .player-badge { width: 100%; max-height: 82px; }
  .badge-avatar { width: 38px; height: 38px; }
}
@media (max-width: 700px) { .game-ledger { grid-template-columns: 1fr 1fr; } .ledger-order { grid-column: 1 / -1; } .private-log { grid-template-columns: 1fr; gap: 3px; } .player-voice-sliders { grid-template-columns: 1fr; gap: 4px; } }
@media (max-width: 600px) {
  .game-topbar { padding: 7px 9px; gap: 7px; flex-wrap: nowrap; overflow-x: auto; }
  .topbar-title, .ctrl-label, .room-config-tag { display: none; }
  .topbar-right { gap: 7px; }
  .view-bar { padding: 5px 8px; }
  .view-btn { padding: 3px 7px; }
  .game-layout { grid-template-rows: 68px minmax(0, 1fr) 68px; }
  .badge-frame { min-height: 62px; padding: 4px; }
  .badge-avatar { width: 32px; height: 32px; }
  .badge-footer { padding: 0 10px; }
  .badge-name { font-size: .6rem; }
  .badge-role { max-width: 48px; }
}
</style>
