<template>
  <div class="game-play">
    <h2>狼人杀游戏</h2>
    
    <!-- 游戏控制按钮 -->
    <div class="game-controls">
      <el-button v-if="!gameStarted" type="primary" size="large" @click="startGame">开始游戏</el-button>
      <template v-else>
        <el-button type="danger" size="large" @click="exitGame">退出游戏</el-button>
        <div class="view-mode-toggle">
          <el-radio-group v-model="currentViewMode" size="small">
            <el-radio-button label="god">上帝视角</el-radio-button>
            <el-radio-button label="player">玩家视角</el-radio-button>
            <el-radio-button label="spectator">旁观视角</el-radio-button>
          </el-radio-group>
          <div v-if="currentViewMode === 'player'" class="player-view-selector">
            <el-select v-model="selectedPlayerId" placeholder="选择玩家" size="small" style="width: 120px; margin-left: 10px">
              <el-option
                v-for="player in alivePlayers"
                :key="player.id"
                :label="player.playerName"
                :value="player.id"
              />
            </el-select>
          </div>
        </div>
      </template>
    </div>
    
    <!-- 游戏主布局 -->
    <div class="game-layout">
      <!-- 左侧：6个玩家位置 -->
      <div class="left-panel">
        <div class="player-positions">
          <div 
            v-for="position in leftPositions" 
            :key="position.number"
            class="player-position"
            :class="{
              'occupied': position.player,
              'empty': !position.player && !position.locked,
              'locked': position.locked
            }"
            @click="handlePositionClick(position)"
          >
            <div v-if="position.player" class="player-info">
              <div class="player-number">{{ position.player.playerNumber }}号</div>
              <div class="player-name">{{ position.player.playerName }}</div>
              <div class="player-status" v-if="gameStarted">{{ position.player.isAlive ? '存活' : '死亡' }}</div>
              <div class="sheriff-badge" v-if="gameStarted && position.player.isSheriff && position.player.isAlive">警长</div>
              <div v-if="gameStarted && position.player.role" class="role-badge" :class="getRoleClass(position.player.role)">{{ position.player.role }}</div>
            </div>
            <div v-else-if="!position.locked" class="empty-position">
              <el-icon class="plus-icon"><Plus /></el-icon>
              <span>添加</span>
            </div>
            <div v-else class="locked-position">
              <el-icon class="lock-icon"><Lock /></el-icon>
              <span>锁定</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 中间：对话区域 -->
      <div class="center-panel">
        <!-- 对话区域 -->
        <el-card class="dialog-card">
          <template #header>
            <div class="panel-header">
              <h3>对话信息</h3>
              <el-button size="small" @click="clearDialog">清空</el-button>
            </div>
          </template>
          
          <!-- AI思考状态 -->
          <div v-if="aiThinkingPlayers.length > 0" class="ai-thinking-status">
            <el-alert
              v-for="playerId in aiThinkingPlayers"
              :key="playerId"
              :title="`${players && players.value ? (players.value.find(p => p.id === playerId)?.playerName || 'AI玩家') : 'AI玩家'}正在思考...`"
              type="info"
              :closable="false"
              show-icon
            />
          </div>
          
          <!-- AI发言对话框 -->
          <div v-if="aiSpeakingContent" class="ai-speaking-dialog">
            <el-dialog
              :title="aiSpeakingContent.playerName + '的发言'"
              width="60%"
              :close-on-click-modal="false"
              :close-on-press-escape="false"
              :show-close="false"
              :visible="true"
            >
              <div class="ai-speech-content">
                {{ aiSpeakingContent.content }}
              </div>
              <template #footer>
                <div class="dialog-footer">
                  <el-button type="primary" @click="aiSpeakingContent = null">确定</el-button>
                </div>
              </template>
            </el-dialog>
          </div>
          
          <div class="dialog-content">
            <div 
              v-for="(message, index) in filteredDialogMessages" 
              :key="index"
              class="dialog-message"
              :class="message.type"
            >
              <div class="message-header">
                <span class="message-sender">{{ message.sender }}</span>
                <span class="message-time">{{ message.time }}</span>
              </div>
              <div class="message-content">{{ message.content }}</div>
            </div>
          </div>
          <div class="dialog-input">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="2"
              placeholder="请输入消息..."
              :disabled="!canSpeak"
              resize="none"
            />
            <div class="input-actions">
              <el-button type="primary" @click="sendMessage" :disabled="!canSpeak || !inputMessage.trim()">发送</el-button>
            </div>
          </div>
        </el-card>
      </div>
      
      <!-- 右侧：6个玩家位置 -->
      <div class="right-panel">
        <div class="player-positions">
          <div 
            v-for="position in rightPositions" 
            :key="position.number"
            class="player-position"
            :class="{
              'occupied': position.player,
              'empty': !position.player && !position.locked,
              'locked': position.locked
            }"
            @click="handlePositionClick(position)"
          >
            <div v-if="position.player" class="player-info">
              <div class="player-number">{{ position.player.playerNumber }}号</div>
              <div class="player-name">{{ position.player.playerName }}</div>
              <div class="player-status" v-if="gameStarted">{{ position.player.isAlive ? '存活' : '死亡' }}</div>
              <div class="sheriff-badge" v-if="gameStarted && position.player.isSheriff && position.player.isAlive">警长</div>
              <div v-if="gameStarted && position.player.role" class="role-badge" :class="getRoleClass(position.player.role)">{{ position.player.role }}</div>
            </div>
            <div v-else-if="!position.locked" class="empty-position">
              <el-icon class="plus-icon"><Plus /></el-icon>
              <span>添加</span>
            </div>
            <div v-else class="locked-position">
              <el-icon class="lock-icon"><Lock /></el-icon>
              <span>锁定</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 添加AI玩家对话框 -->
    <el-dialog
      v-model="addAiDialogVisible"
      title="添加AI玩家"
      width="400px"
    >
      <el-form>
        <el-form-item label="选择AI玩家">
          <el-select v-model="selectedAiPlayerId" placeholder="请选择AI玩家" style="width: 100%">
            <el-option
              v-for="aiPlayer in availableAiPlayers"
              :key="aiPlayer.id"
              :label="aiPlayer.name"
              :value="aiPlayer.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="addAiDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmAddAiPlayer" :disabled="!selectedAiPlayerId">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { Plus, Lock } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const roomId = route.params.id || 1 // 默认使用roomId为1，避免undefined

// 游戏状态
const players = ref([])
const currentRound = ref(1)
const currentDay = ref(1)
const currentPhase = ref('night') // night, day, speak, vote
const speakTime = ref(120)
const speakingPlayer = ref(null)
const gameMode = ref('经典模式')
const gameStarted = ref(false)
const roomInfo = ref({})

// 游戏操作目标
const voteTarget = ref(null)
const skillTarget = ref(null)

// AI思考状态
const aiThinkingPlayers = ref([])
const aiSpeakingContent = ref(null)

// AI玩家相关
const aiPlayers = ref([])
const selectedAiPlayerId = ref(null)
const addAiDialogVisible = ref(false)
const currentPosition = ref(null)

// 视角控制
const currentViewMode = ref('god')
const selectedPlayerId = ref(null)
const isGameMaster = ref(true)

// 对话和消息
const dialogMessages = ref([])
const inputMessage = ref('')

// API请求队列和频率控制
const apiRequestQueue = ref([])
const isApiRequesting = ref(false)
const lastApiRequestTime = ref(0)
const API_REQUEST_INTERVAL = 5000 // 5秒间隔，避免频率限制
const MAX_RETRY_COUNT = 3 // 最大重试次数

// 奇迹商人相关状态
const merchantSkillUsed = ref(false)
const luckyPlayerId = ref(null)
const luckyPlayerSkill = ref(null) // 'check', 'poison', 'shield'

// 计算属性
const alivePlayers = computed(() => players.value.filter(p => p.isAlive))
const wolfPlayers = computed(() => players.value.filter(p => ['狼人', '狼王', '狼美人', '白狼王', '石像鬼'].includes(p.role)))
const villagerPlayers = computed(() => players.value.filter(p => p.role === '平民'))
const godPlayers = computed(() => players.value.filter(p => ['预言家', '女巫', '猎人', '守卫', '奇迹商人', '守墓人', '愚者', '骑士'].includes(p.role)))

// 可用的AI玩家
const availableAiPlayers = computed(() => {
  return aiPlayers.value
})

// 玩家位置布局
const leftPositions = computed(() => {
  const positions = []
  // 左侧6个位置
  for (let i = 1; i <= 6; i++) {
    const player = players.value.find(p => p.playerNumber === i)
    const locked = roomInfo.value.playerCount && i > roomInfo.value.playerCount
    positions.push({
      number: i,
      player,
      locked
    })
  }
  return positions
})

const rightPositions = computed(() => {
  const positions = []
  // 右侧6个位置
  for (let i = 7; i <= 12; i++) {
    const player = players.value.find(p => p.playerNumber === i)
    const locked = roomInfo.value.playerCount && i > roomInfo.value.playerCount
    positions.push({
      number: i,
      player,
      locked
    })
  }
  return positions
})

const currentPhaseText = computed(() => {
  const phaseMap = {
    night: '夜晚',
    day: '白天',
    speak: '发言阶段',
    vote: '投票阶段'
  }
  return phaseMap[currentPhase.value] || '未知'
})

const phaseType = computed(() => {
  const typeMap = {
    night: 'warning',
    day: 'success',
    speak: 'info',
    vote: 'primary'
  }
  return typeMap[currentPhase.value] || 'info'
})



// 过滤后的对话消息
const filteredDialogMessages = computed(() => {
  if (currentViewMode.value === 'god') {
    // 上帝视角显示全部对话信息
    return dialogMessages.value
  } else if (currentViewMode.value === 'player' && selectedPlayerId.value) {
    // 玩家视角根据情况判断显示内容
    const selectedPlayer = players.value.find(p => p.id === selectedPlayerId.value)
    if (!selectedPlayer) return []
    
    return dialogMessages.value.filter(message => {
      // 显示裁判消息
      if (message.type === 'referee') {
        return true
      }
      // 显示自己的消息
      if (message.sender === selectedPlayer.playerName || message.sender === '我') {
        return true
      }
      // 显示其他玩家的公开消息
      if (message.type === 'player') {
        return true
      }
      // 狼人可以看到其他狼人的密聊
      if (message.type === 'wolf' && ['狼人', '狼王', '狼美人', '白狼王', '石像鬼'].includes(selectedPlayer.role)) {
        return true
      }
      return false
    })
  } else {
    // 旁观视角只显示公开消息
    return dialogMessages.value.filter(message => {
      return message.type === 'referee' || message.type === 'player'     
    })
  }
})

// 权限控制
const canSpeak = computed(() => {
  if (currentPhase.value === 'speak' && speakingPlayer.value) {
    // 发言阶段，只有轮到发言的玩家才能发言
    if (selectedPlayerId.value) {
      return speakingPlayer.value === selectedPlayerId.value
    }
    return true // 上帝视角可以发言
  } else if (currentPhase.value === 'night') {
    // 夜晚阶段，狼人可以密聊
    if (selectedPlayerId.value) {
      const selectedPlayer = players.value.find(p => p.id === selectedPlayerId.value)
      return selectedPlayer && selectedPlayer.role === '狼人'
    }
  }
  return false
})

const canVote = computed(() => {
  return currentPhase.value === 'vote'
})

const canUseSkill = computed(() => {
  return currentPhase.value === 'night'
})

const canPass = computed(() => {
  return true
})

// 视角逻辑
const getRoleClass = (role) => {
  // 根据身份返回对应的CSS类
  if (['狼人', '狼王', '狼美人', '白狼王', '石像鬼'].includes(role)) {
    return 'role-wolf'
  } else if (role === '平民') {
    return 'role-villager'
  } else if (['预言家', '女巫', '猎人', '守卫', '奇迹商人', '守墓人', '愚者', '骑士'].includes(role)) {
    return 'role-god'
  }
  return ''
}

const shouldShowRole = (player) => {
  if (currentViewMode.value === 'god') {
    return true
  } else if (currentViewMode.value === 'player' && selectedPlayerId.value) {
    const selectedPlayer = players.value.find(p => p.id === selectedPlayerId.value)
    if (['狼人', '狼王', '狼美人', '白狼王', '石像鬼'].includes(selectedPlayer.role)) {
      // 狼人可以看到其他狼人
      return ['狼人', '狼王', '狼美人', '白狼王', '石像鬼'].includes(player.role) || player.id === selectedPlayerId.value
    } else if (['预言家', '女巫', '猎人', '守卫', '奇迹商人', '守墓人', '愚者', '骑士'].includes(selectedPlayer.role)) {
      // 神职只能看到自己的身份
      return player.id === selectedPlayerId.value
    } else {
      // 平民只能看到自己的身份
      return player.id === selectedPlayerId.value
    }
  } else {
    // 旁观视角看不到任何身份
    return false
  }
}

// 方法
const startGame = async () => {
  // 初始化游戏
  await loadGameData()
  await distributeRoles()
  
  // 检查AI玩家设置
  checkAiPlayers()
  
  // 打印所有玩家信息
  console.log('游戏开始时的玩家列表:', players.value)
  players.value.forEach(p => {
    console.log(`玩家 ${p.playerName}: id=${p.id}, userId=${p.userId}, aiPlayerId=${p.aiPlayerId}, role=${p.role}`)
  })
  
  await startGamePhase()
  gameStarted.value = true
  
  // 通知玩家身份（仅在玩家视角显示）
  notifyPlayerRoles()
  
  // 自动推进游戏流程
  startGameFlow()
}

const startGameFlow = () => {
  console.log('startGameFlow - 自动推进游戏流程')
  console.log('startGameFlow - 当前阶段:', currentPhase.value)
  
  // 启动游戏流程，开始夜晚阶段
  setTimeout(() => {
    console.log('startGameFlow - 准备调用nextPhase')
    nextPhase()
  }, 1000)
}

// 检查AI玩家的userId设置
const checkAiPlayers = () => {
  // 确保AI玩家的userId被正确设置为-1
  players.value.forEach(player => {
    if (player.aiPlayerId) {
      player.userId = -1
    }
  })
}

const notifyPlayerRoles = () => {
  // 通知玩家身份（仅在玩家视角显示）
  players.value.forEach(player => {
    // 上帝视角显示所有身份
    if (currentViewMode.value === 'god') {
      addRefereeMessage(`${player.playerName}的身份是：${player.role}`)
    } else if (currentViewMode.value === 'player' && selectedPlayerId.value === player.id) {
      // 玩家视角只显示自己的身份
      addRefereeMessage(`你的身份是：${player.role}`)
    }
  })
}

const exitGame = () => {
  // 退出游戏
  ElMessageBox.confirm('确定要退出游戏吗？', '退出游戏', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    gameStarted.value = false
    router.push(`/game/room/detail/${roomId}`)
  }).catch(() => {
    // 取消退出
  })
}

const handlePositionClick = (position) => {
  // 处理位置点击
  if (position.locked) {
    // 锁定的位置，点击无效果
    return
  }
  
  if (!position.player && !gameStarted.value) {
    // 空位置且游戏未开始，显示添加AI玩家对话框
    currentPosition.value = position
    addAiDialogVisible.value = true
  }
}

const confirmAddAiPlayer = async () => {
  // 确认添加AI玩家
  if (!selectedAiPlayerId.value || !currentPosition.value) {
    ElMessage.warning('请选择AI玩家')
    return
  }
  
  try {
    const aiPlayer = aiPlayers.value.find(ai => ai.id == selectedAiPlayerId.value)
    if (!aiPlayer) {
      ElMessage.error('AI玩家不存在')
      return
    }
    
    // 调用后端API添加AI玩家
    const response = await axios.post('/game/player/add', {
      roomId,
      userId: -1,
      aiPlayerId: selectedAiPlayerId.value,
      playerNumber: currentPosition.value.number,
      playerName: aiPlayer.name
    })
    
    if (response.data.code === 200) {
      ElMessage.success(`AI玩家 ${aiPlayer.name} 已添加`)
      // 刷新玩家列表
      await loadGameData()
      // 重置选择
      selectedAiPlayerId.value = null
      currentPosition.value = null
      addAiDialogVisible.value = false
    } else {
      ElMessage.error('添加AI玩家失败')
    }
  } catch (error) {
    console.error('Add AI player error:', error)
    ElMessage.error('添加AI玩家失败')
  }
}

const loadGameData = async () => {
  // 加载游戏数据
  try {
    // 从后端获取房间信息
    const roomResponse = await axios.get(`/game/room/info/${roomId}`)
    if (roomResponse.data.code === 200) {
      const room = roomResponse.data.data
      roomInfo.value = room
      const playerCount = room.playerCount || 12 // 从房间信息获取玩家数量
      gameMode.value = room.gameBoard === 'standard' ? '标准场' : 
                       room.gameBoard === 'miracle_merchant' ? '奇迹商人场' : 
                       room.gameBoard === 'wolfking_guard' ? '狼王守卫场' : 
                       room.gameBoard === 'wolf_beauty_knight' ? '狼美人骑士场' : 
                       room.gameBoard === 'white_wolf_knight' ? '白狼王骑士场' : 
                       room.gameBoard === 'gargoyle_gravedigger' ? '石像鬼守墓人场' : '经典模式'
      
      // 从后端获取玩家列表
      const playersResponse = await axios.get(`/game/player/list/${roomId}`)
      if (playersResponse.data.code === 200) {
        const playerList = playersResponse.data.data
        // 转换玩家数据
        players.value = playerList.map(p => ({
          id: p.id,
          playerNumber: p.playerNumber,
          playerName: p.playerName,
          role: p.role || '',
          isAlive: p.status === 1,
          isSpeaking: false,
          isSheriff: p.isSheriff === 1,
          userId: p.userId,
          aiPlayerId: p.aiPlayerId
        }))
      }
      
      // 从后端获取AI玩家列表
      const aiPlayersResponse = await axios.get('/ai/player/available')
      if (aiPlayersResponse.data.code === 200) {
        aiPlayers.value = aiPlayersResponse.data.data
      }
    } else {
      // 如果获取房间信息失败，使用默认值
      roomInfo.value = {
        playerCount: 12
      }
      
      // 先从后端获取AI玩家列表
      let availableAiPlayers = []
      try {
        const aiPlayersResponse = await axios.get('/ai/player/available')
        if (aiPlayersResponse.data.code === 200) {
          availableAiPlayers = aiPlayersResponse.data.data
        }
      } catch (error) {
        console.error('获取AI玩家列表失败:', error)
      }
      
      // 生成模拟玩家数据（全部为AI玩家）
      const mockPlayers = []
      for (let i = 1; i <= 12; i++) {
        // 随机分配AI玩家配置
        const aiPlayerIndex = Math.floor(Math.random() * availableAiPlayers.length)
        const aiPlayer = availableAiPlayers[aiPlayerIndex]
        const aiPlayerId = aiPlayer ? aiPlayer.id : (i % 5) + 1
        
        mockPlayers.push({
          id: i,
          playerNumber: i,
          playerName: `AI玩家${i}`,
          role: '',
          isAlive: true,
          isSpeaking: false,
          isSheriff: false,
          userId: -1, // 设置为AI玩家
          aiPlayerId: aiPlayerId // 使用从后端获取的AI配置
        })
      }
      players.value = mockPlayers
    }
    
    // 设置默认视角
    if (players.value.length > 0) {
      selectedPlayerId.value = players.value[0].id
    }
    
    // 添加裁判消息
    addRefereeMessage('游戏开始！欢迎来到狼人杀游戏。')
    addRefereeMessage(`本局游戏为${roomInfo.value.playerCount || 12}人场，配置：根据板子自动分配。`)
    addRefereeMessage('天黑请闭眼，游戏开始！')
  } catch (error) {
    console.error('Load game data error:', error)
    // 出错时使用默认值
    roomInfo.value = {
      playerCount: 12
    }
    // 生成模拟玩家数据（全部为AI玩家）
    const mockPlayers = []
    for (let i = 1; i <= 12; i++) {
      mockPlayers.push({
        id: i,
        playerNumber: i,
        playerName: `AI玩家${i}`,
        role: '',
        isAlive: true,
        isSpeaking: false,
        isSheriff: false,
        userId: -1, // 设置为AI玩家
        aiPlayerId: i % 5 + 1 // 循环使用1-5的AI配置
      })
    }
    players.value = mockPlayers
    
    // 设置默认视角
    if (players.value.length > 0) {
      selectedPlayerId.value = players.value[0].id
    }
    
    // 添加裁判消息
    addRefereeMessage('游戏开始！欢迎来到狼人杀游戏。')
    addRefereeMessage(`本局游戏为${roomInfo.value.playerCount || 12}人场，配置：根据板子自动分配。`)
    addRefereeMessage('天黑请闭眼，游戏开始！')
    ElMessage.error('加载游戏数据失败，使用默认数据')
  }
}

const distributeRoles = async () => {
  // 身份牌随机发放
  try {
    // 读取游戏板子配置
    const boardConfig = await loadBoardConfig()
    
    // 生成身份牌列表
    const roles = []
    boardConfig.roles.forEach(roleConfig => {
      for (let i = 0; i < roleConfig.count; i++) {
        roles.push(roleConfig.role)
      }
    })
    
    // 打乱身份牌
    shuffleArray(roles)
    
    // 发放身份牌
    players.value.forEach((player, index) => {
      if (index < roles.length) {
        player.role = roles[index]
      } else {
        player.role = '平民' // 默认平民
      }
    })
    
    addRefereeMessage(`身份牌发放完成！本局游戏配置：${boardConfig.roles.map(r => `${r.role}×${r.count}`).join('、')}`)
  } catch (error) {
    console.error('Distribute roles error:', error)
    ElMessage.error('身份牌发放失败')
  }
}

const loadBoardConfig = async () => {
  // 加载游戏板子配置
  try {
    // 从URL参数获取游戏板子配置
    let gameBoard = route.query.gameBoard || 'standard'
    let roomPlayerCount = parseInt(route.query.playerCount) || 12
    
    // 尝试从后端获取房间信息（如果有的话）
    try {
      const roomResponse = await axios.get(`/game/room/info/${roomId}`)
      if (roomResponse.data.code === 200) {
        if (!route.query.gameBoard) {
          gameBoard = roomResponse.data.data.gameBoard || 'standard'
        }
        if (!route.query.playerCount) {
          roomPlayerCount = roomResponse.data.data.playerCount || 12
        }
      }
    } catch (error) {
      console.log('Get room info error, using URL params:', error)
    }
    
    // 根据游戏板子返回配置
    const playerCount = roomPlayerCount || players.value.length || 12
    
    console.log(`Load board config: gameBoard=${gameBoard}, playerCount=${playerCount}`)
    
    switch (gameBoard) {
      case 'standard':
        // 12人标准场配置
        return {
          player_count: 12,
          roles: [
            { role: '狼人', count: 4 },
            { role: '平民', count: 4 },
            { role: '预言家', count: 1 },
            { role: '女巫', count: 1 },
            { role: '猎人', count: 1 },
            { role: '愚者', count: 1 }
          ]
        }
      case 'miracle_merchant':
        // 12人奇迹商人场配置
        return {
          player_count: 12,
          roles: [
            { role: '狼人', count: 3 },
            { role: '狼王', count: 1 },
            { role: '平民', count: 4 },
            { role: '预言家', count: 1 },
            { role: '女巫', count: 1 },
            { role: '守卫', count: 1 },
            { role: '奇迹商人', count: 1 }
          ]
        }
      case 'wolfking_guard':
        // 12人狼王守卫场配置
        return {
          player_count: 12,
          roles: [
            { role: '狼人', count: 3 },
            { role: '狼王', count: 1 },
            { role: '平民', count: 4 },
            { role: '预言家', count: 1 },
            { role: '女巫', count: 1 },
            { role: '猎人', count: 1 },
            { role: '守卫', count: 1 }
          ]
        }
      case 'wolf_beauty_knight':
        // 12人狼美人骑士场配置
        return {
          player_count: 12,
          roles: [
            { role: '狼人', count: 3 },
            { role: '狼美人', count: 1 },
            { role: '平民', count: 4 },
            { role: '预言家', count: 1 },
            { role: '女巫', count: 1 },
            { role: '骑士', count: 1 },
            { role: '守卫', count: 1 }
          ]
        }
      case 'white_wolf_knight':
        // 12人白狼王骑士场配置
        return {
          player_count: 12,
          roles: [
            { role: '狼人', count: 3 },
            { role: '白狼王', count: 1 },
            { role: '平民', count: 4 },
            { role: '预言家', count: 1 },
            { role: '女巫', count: 1 },
            { role: '骑士', count: 1 },
            { role: '守卫', count: 1 }
          ]
        }
      case 'gargoyle_gravedigger':
        // 12人石像鬼守墓人场配置
        return {
          player_count: 12,
          roles: [
            { role: '狼人', count: 3 },
            { role: '石像鬼', count: 1 },
            { role: '平民', count: 4 },
            { role: '预言家', count: 1 },
            { role: '女巫', count: 1 },
            { role: '守墓人', count: 1 },
            { role: '猎人', count: 1 }
          ]
        }
      default:
        // 标准场配置
        if (playerCount === 12) {
          return {
            player_count: 12,
            roles: [
              { role: '狼人', count: 4 },
              { role: '平民', count: 4 },
              { role: '预言家', count: 1 },
              { role: '女巫', count: 1 },
              { role: '猎人', count: 1 },
              { role: '守卫', count: 1 }
            ]
          }
        } else if (playerCount === 10) {
          return {
            player_count: 10,
            roles: [
              { role: '狼人', count: 3 },
              { role: '平民', count: 4 },
              { role: '预言家', count: 1 },
              { role: '女巫', count: 1 },
              { role: '猎人', count: 1 }
            ]
          }
        } else if (playerCount === 9) {
          return {
            player_count: 9,
            roles: [
              { role: '狼人', count: 3 },
              { role: '平民', count: 3 },
              { role: '预言家', count: 1 },
              { role: '女巫', count: 1 },
              { role: '猎人', count: 1 }
            ]
          }
        } else {
          // 默认12人标准场配置
          return {
            player_count: 12,
            roles: [
              { role: '狼人', count: 4 },
              { role: '平民', count: 4 },
              { role: '预言家', count: 1 },
              { role: '女巫', count: 1 },
              { role: '猎人', count: 1 },
              { role: '守卫', count: 1 }
            ]
          }
        }
    }
  } catch (error) {
    console.error('Load board config error:', error)
    // 返回默认配置
    return {
      player_count: 12,
      roles: [
        { role: '狼人', count: 4 },
        { role: '平民', count: 4 },
        { role: '预言家', count: 1 },
        { role: '女巫', count: 1 },
        { role: '猎人', count: 1 },
        { role: '守卫', count: 1 }
      ]
    }
  }
}

const shuffleArray = (array) => {
  // 打乱数组
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[array[i], array[j]] = [array[j], array[i]]
  }
  return array
}

const startGamePhase = () => {
  console.log('startGamePhase - 开始游戏阶段')
  currentPhase.value = 'night'
  console.log('startGamePhase - 当前阶段设置为:', currentPhase.value)
  addRefereeMessage('夜晚降临，狼人请睁眼。')
  console.log('startGamePhase - 已添加裁判消息')
}

const sendMessage = () => {
  if (!inputMessage.value.trim() || !canSpeak.value) return
  
  // 获取当前选中玩家的名称
  const currentPlayer = players.value.find(p => p.id === selectedPlayerId.value)
  const senderName = currentPlayer ? currentPlayer.playerName : '我'
  
  // 判断消息类型
  let messageType = 'player'
  if (currentPhase.value === 'night' && currentPlayer && currentPlayer.role === '狼人') {
    messageType = 'wolf' // 狼人夜晚密聊
  }
  
  const message = {
    sender: senderName,
    content: inputMessage.value.trim(),
    time: new Date().toLocaleTimeString(),
    type: messageType
  }
  
  dialogMessages.value.push(message)
  inputMessage.value = ''
  
  // 滚动到底部
  scrollToBottom()
}



const addRefereeMessage = (content) => {
  console.log('addRefereeMessage - 添加裁判消息:', content)
  const dialogMessage = {
    sender: '裁判',
    content: content,
    time: new Date().toLocaleTimeString(),
    type: 'referee'
  }
  dialogMessages.value.push(dialogMessage)
  console.log('addRefereeMessage - 消息已添加到dialogMessages')
  
  scrollToBottom()
}

const addDialogMessage = (sender, content, messageType = 'player') => {
  // 添加对话消息
  const message = {
    sender: sender,
    content: content,
    time: new Date().toLocaleTimeString(),
    type: messageType
  }
  console.log('添加对话消息:', message)
  dialogMessages.value.push(message)
  
  // 滚动到底部
  scrollToBottom()
}

const scrollToBottom = () => {
  setTimeout(() => {
    const dialogContent = document.querySelector('.dialog-content')
    if (dialogContent) {
      dialogContent.scrollTop = dialogContent.scrollHeight
    }
  }, 100)
}

const clearDialog = () => {
  dialogMessages.value = []
}

const vote = () => {
  // 投票逻辑
  if (!alivePlayers.value.length) return
  
  ElMessageBox.confirm('请选择要放逐的玩家', '投票放逐', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    showCancelButton: true,
    beforeClose: (action, instance, done) => {
      if (action === 'confirm') {
        const selectedPlayer = players.value.find(p => p.id === voteTarget.value)
        if (selectedPlayer) {
          addRefereeMessage(`${selectedPlayer.playerName}被放逐出局！身份是：${selectedPlayer.role}`)
          killPlayer(selectedPlayer.id)
          done()
        } else {
          ElMessage.warning('请选择要放逐的玩家')
        }
      } else {
        done()
      }
    }
  }).then(() => {
    // 投票成功
  }).catch(() => {
    // 取消投票
  })
}

const useSkill = () => {
  // 使用技能逻辑
  const selectedPlayer = players.value.find(p => p.id === selectedPlayerId.value)
  if (!selectedPlayer) return
  
  switch (selectedPlayer.role) {
    case '预言家':
      // 预言家验人
      useSeerSkill()
      break
    case '女巫':
      // 女巫用药
      useWitchSkill()
      break
    case '守卫':
      // 守卫守护
      useGuardSkill()
      break
    case '奇迹商人':
      // 奇迹商人给技能
      useMerchantSkill()
      break
    case '狼人':
    case '狼王':
    case '狼美人':
    case '白狼王':
    case '石像鬼':
      // 狼人杀人
      useWolfKillSkill()
      break
    default:
      ElMessage.info('该角色没有技能')
  }
}

// 预言家技能
const useSeerSkill = () => {
  ElMessageBox.confirm('请选择要查验的玩家', '预言家验人', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info',
    showCancelButton: true,
    beforeClose: (action, instance, done) => {
      if (action === 'confirm') {
        const targetPlayer = players.value.find(p => p.id === skillTarget.value)
        if (targetPlayer) {
          const identity = ['狼人', '狼王', '狼美人', '白狼王', '石像鬼'].includes(targetPlayer.role) ? '狼人' : '好人'
          addRefereeMessage(`预言家查验了${targetPlayer.playerName}，身份是：${identity}`)
          done()
        } else {
          ElMessage.warning('请选择要查验的玩家')
        }
      } else {
        done()
      }
    }
  })
}

// 女巫技能
const useWitchSkill = () => {
  ElMessageBox.confirm('请选择要使用的药水', '女巫用药', {
    confirmButtonText: '使用解药',
    cancelButtonText: '使用毒药',
    type: 'success',
    showCancelButton: true,
    beforeClose: (action, instance, done) => {
      if (action === 'confirm') {
        // 使用解药
        addRefereeMessage('女巫使用了解药')
        done()
      } else if (action === 'cancel') {
        // 使用毒药
        ElMessageBox.confirm('请选择要毒杀的玩家', '使用毒药', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'danger',
          showCancelButton: true,
          beforeClose: (subAction, subInstance, subDone) => {
            if (subAction === 'confirm') {
              const targetPlayer = players.value.find(p => p.id === skillTarget.value)
              if (targetPlayer) {
                addRefereeMessage(`女巫毒死了${targetPlayer.playerName}`)
                killPlayer(targetPlayer.id)
                subDone()
                done()
              } else {
                ElMessage.warning('请选择要毒杀的玩家')
              }
            } else {
              subDone()
              done()
            }
          }
        })
      }
    }
  })
}

// 守卫技能
const useGuardSkill = () => {
  ElMessageBox.confirm('请选择要守护的玩家', '守卫守护', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    showCancelButton: true,
    beforeClose: (action, instance, done) => {
      if (action === 'confirm') {
        const targetPlayer = players.value.find(p => p.id === skillTarget.value)
        if (targetPlayer) {
          addRefereeMessage(`守卫守护了${targetPlayer.playerName}`)
          done()
        } else {
          ElMessage.warning('请选择要守护的玩家')
        }
      } else {
        done()
      }
    }
  })
}

// 奇迹商人技能
const useMerchantSkill = () => {
  if (merchantSkillUsed.value) {
    ElMessage.warning('奇迹商人已经使用过技能')
    return
  }
  
  ElMessageBox.confirm('请选择要给予的技能类型', '奇迹商人给技能', {
    confirmButtonText: '查验',
    cancelButtonText: '毒药',
    distinguishCancelAndClose: true,
    type: 'success',
    showCancelButton: true,
    beforeClose: (action, instance, done) => {
      if (action === 'confirm') {
        // 给予查验技能
        ElMessageBox.confirm('请选择要给予查验技能的玩家', '选择幸运儿', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'info',
          showCancelButton: true,
          beforeClose: (subAction, subInstance, subDone) => {
            if (subAction === 'confirm') {
              const targetPlayer = players.value.find(p => p.id === skillTarget.value)
              if (targetPlayer) {
                const merchant = players.value.find(p => p.role === '奇迹商人')
                if (targetPlayer.id === merchant.id) {
                  ElMessage.warning('不能选择自己')
                  return
                }
                
                // 检查目标是否为狼人
                const isWolf = ['狼人', '狼王', '狼美人', '白狼王', '石像鬼'].includes(targetPlayer.role)
                
                merchantSkillUsed.value = true
                luckyPlayerId.value = targetPlayer.id
                luckyPlayerSkill.value = 'check'
                
                addRefereeMessage(`奇迹商人选择了${targetPlayer.playerName}作为幸运儿，给予查验技能`)
                
                // 如果目标是狼人，奇迹商人死亡
                if (isWolf) {
                  setTimeout(() => {
                    addRefereeMessage(`${targetPlayer.playerName}是狼人，奇迹商人死亡`)
                    killPlayer(merchant.id)
                  }, 1000)
                } else {
                  setTimeout(() => {
                    addRefereeMessage(`${targetPlayer.playerName}是好人，奇迹商人存活`)
                  }, 1000)
                }
                
                subDone()
                done()
              } else {
                ElMessage.warning('请选择要给予查验技能的玩家')
              }
            } else {
              subDone()
              done()
            }
          }
        })
      } else if (action === 'cancel') {
        // 给予毒药技能
        ElMessageBox.confirm('请选择要给予毒药技能的玩家', '选择幸运儿', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'danger',
          showCancelButton: true,
          beforeClose: (subAction, subInstance, subDone) => {
            if (subAction === 'confirm') {
              const targetPlayer = players.value.find(p => p.id === skillTarget.value)
              if (targetPlayer) {
                const merchant = players.value.find(p => p.role === '奇迹商人')
                if (targetPlayer.id === merchant.id) {
                  ElMessage.warning('不能选择自己')
                  return
                }
                
                // 检查目标是否为狼人
                const isWolf = ['狼人', '狼王', '狼美人', '白狼王', '石像鬼'].includes(targetPlayer.role)
                
                merchantSkillUsed.value = true
                luckyPlayerId.value = targetPlayer.id
                luckyPlayerSkill.value = 'poison'
                
                addRefereeMessage(`奇迹商人选择了${targetPlayer.playerName}作为幸运儿，给予毒药技能`)
                
                // 如果目标是狼人，奇迹商人死亡
                if (isWolf) {
                  setTimeout(() => {
                    addRefereeMessage(`${targetPlayer.playerName}是狼人，奇迹商人死亡`)
                    killPlayer(merchant.id)
                  }, 1000)
                } else {
                  setTimeout(() => {
                    addRefereeMessage(`${targetPlayer.playerName}是好人，奇迹商人存活`)
                  }, 1000)
                }
                
                subDone()
                done()
              } else {
                ElMessage.warning('请选择要给予毒药技能的玩家')
              }
            } else {
              subDone()
              done()
            }
          }
        })
      } else {
        done()
      }
    }
  })
}

// 狼人杀人技能
const useWolfKillSkill = () => {
  ElMessageBox.confirm('请选择要击杀的玩家', '狼人杀人', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'danger',
    showCancelButton: true,
    beforeClose: (action, instance, done) => {
      if (action === 'confirm') {
        const targetPlayer = players.value.find(p => p.id === skillTarget.value)
        if (targetPlayer) {
          addRefereeMessage(`狼人击杀了${targetPlayer.playerName}`)
          killPlayer(targetPlayer.id)
          done()
        } else {
          ElMessage.warning('请选择要击杀的玩家')
        }
      } else {
        done()
      }
    }
  })
}

const pass = () => {
  // 跳过逻辑
  ElMessage.info('跳过操作')
}

// 玩家状态管理
const killPlayer = (playerId) => {
  const player = players.value.find(p => p.id === playerId)
  if (player && player.isAlive) {
    player.isAlive = false
    player.isSpeaking = false
    
    // 处理猎人开枪
    if (player.role === '猎人') {
      addRefereeMessage(`${player.playerName}（猎人）死亡，发动技能开枪！`)
      // 这里应该弹出选择开枪目标的界面
    }
    
    addRefereeMessage(`${player.playerName}（${player.role}）死亡！`)
    
    // 检查游戏是否结束
    checkGameEnd()
  }
}

const startPlayerSpeaking = (playerId) => {
  // 开始发言
  players.value.forEach(p => {
    p.isSpeaking = p.id === playerId
  })
  speakingPlayer.value = players.value.find(p => p.id === playerId)
  addRefereeMessage(`${speakingPlayer.value.playerName}开始发言`)
  
  // 如果是AI玩家，触发思考和发言
  if (speakingPlayer.value && speakingPlayer.value.userId === -1) {
    startAiThinking(playerId)
  }
}

const endPlayerSpeaking = () => {
  // 结束发言
  players.value.forEach(p => {
    p.isSpeaking = false
  })
  if (speakingPlayer.value) {
    addRefereeMessage(`${speakingPlayer.value.playerName}发言结束`)
  }
  speakingPlayer.value = null
}

const setSheriff = (playerId) => {
  // 设置警长
  players.value.forEach(p => {
    p.isSheriff = p.id === playerId
  })
  const sheriff = players.value.find(p => p.id === playerId)
  if (sheriff) {
    addRefereeMessage(`${sheriff.playerName}当选为警长！`)
  }
}

const checkGameEnd = () => {
  // 检查游戏是否结束
  const aliveWolves = wolfPlayers.value.filter(p => p.isAlive).length
  const aliveVillagers = [...villagerPlayers.value, ...godPlayers.value].filter(p => p.isAlive).length
  
  if (aliveWolves === 0) {
    addRefereeMessage('狼人全部死亡，好人阵营胜利！')
    // 游戏结束逻辑
    gameStarted.value = false
    return true
  } else if (aliveWolves >= aliveVillagers) {
    addRefereeMessage('狼人数量大于等于好人数量，狼人阵营胜利！')
    // 游戏结束逻辑
    gameStarted.value = false
    return true
  } else if (alivePlayers.value.length === 0) {
    addRefereeMessage('所有玩家都死亡了，游戏结束！')
    gameStarted.value = false
    return true
  }
  return false
}

// AI发言相关方法
const startAiThinking = (playerId) => {
  // AI开始思考
  const player = players.value.find(p => p.id === playerId)
  console.log('startAiThinking - player:', player, 'userId:', player?.userId, 'aiPlayerId:', player?.aiPlayerId)
  if (player && player.userId === -1) { // 确认是AI玩家
    aiThinkingPlayers.value.push(playerId)
    console.log('AI玩家开始思考:', player.playerName)
    
    // 模拟思考过程
    setTimeout(() => {
      generateAiSpeech(playerId)
    }, 3000 + Math.random() * 2000) // 3-5秒思考时间
  }
}

const generateAiSpeech = async (playerId) => {
  // 生成AI发言
  const player = players.value.find(p => p.id === playerId)
  if (!player) return
  
  console.log('generateAiSpeech - 开始生成发言:', player.playerName, 'aiPlayerId:', player.aiPlayerId)
  
  try {
    // 获取AI玩家配置
    console.log('正在获取AI玩家配置, aiPlayerId:', player.aiPlayerId)
    const aiPlayerResponse = await axios.get(`/ai/player/info/${player.aiPlayerId}`)
    console.log('AI玩家配置响应:', aiPlayerResponse.data)
    
    if (aiPlayerResponse.data.code !== 200) {
      throw new Error('获取AI玩家配置失败')
    }
    
    const aiPlayerConfig = aiPlayerResponse.data.data
    console.log('AI玩家配置:', aiPlayerConfig)
    
    // 构建对话上下文
    const context = buildAiContext(player)
    console.log('对话上下文:', context)
    
    // 调用AI API生成对话
    console.log('开始调用AI API...')
    const speechContent = await callAiApi(aiPlayerConfig, context)
    console.log('AI API返回的发言内容:', speechContent)
    
    // 限制发言长度不超过500字
    const finalSpeechContent = speechContent.substring(0, 500)
    
    // 结束思考
    aiThinkingPlayers.value = aiThinkingPlayers.value.filter(id => id !== playerId)
    
    // 显示发言内容
    aiSpeakingContent.value = {
      playerId: player.id,
      playerName: player.playerName,
      content: finalSpeechContent
    }
    
    // 判断消息类型
    let messageType = 'player'
    if (currentPhase.value === 'night' && player.role === '狼人') {
      messageType = 'wolf' // 狼人夜晚密聊
    }
    
    // 添加到对话消息
    addDialogMessage(player.playerName, finalSpeechContent, messageType)
    
    // 3秒后清除发言对话框
    setTimeout(() => {
      aiSpeakingContent.value = null
      
      // 结束当前玩家发言并切换到下一个玩家
      endPlayerSpeaking()
      
      // 切换到下一个玩家发言
      const currentIndex = alivePlayers.value.findIndex(p => p.id === playerId)
      const nextIndex = (currentIndex + 1) % alivePlayers.value.length
      if (alivePlayers.value[nextIndex]) {
        startPlayerSpeaking(alivePlayers.value[nextIndex].id)
      } else {
        // 所有玩家发言完毕，进入投票阶段
        nextPhase()
      }
    }, 3000)
  } catch (error) {
    console.error('AI发言生成失败:', error)
    
    // 如果AI调用失败，使用本地生成的发言
    const personality = getAiPersonality(player)
    const strategy = getAiStrategy(player)
    let speechContent = generateSpeechContent(player, personality, strategy)
    speechContent = speechContent.substring(0, 500)
    
    // 结束思考
    aiThinkingPlayers.value = aiThinkingPlayers.value.filter(id => id !== playerId)
    
    // 显示发言内容
    aiSpeakingContent.value = {
      playerId: player.id,
      playerName: player.playerName,
      content: speechContent
    }
    
    // 判断消息类型
    let messageType = 'player'
    if (currentPhase.value === 'night' && player.role === '狼人') {
      messageType = 'wolf' // 狼人夜晚密聊
    }
    
    // 添加到对话消息
    addDialogMessage(player.playerName, speechContent, messageType)
    
    // 3秒后清除发言对话框
    setTimeout(() => {
      aiSpeakingContent.value = null
      
      // 结束当前玩家发言并切换到下一个玩家
      endPlayerSpeaking()
      
      // 切换到下一个玩家发言
      const currentIndex = alivePlayers.value.findIndex(p => p.id === playerId)
      const nextIndex = (currentIndex + 1) % alivePlayers.value.length
      if (alivePlayers.value[nextIndex]) {
        startPlayerSpeaking(alivePlayers.value[nextIndex].id)
      } else {
        // 所有玩家发言完毕，进入投票阶段
        nextPhase()
      }
    }, 3000)
  }
}

const buildAiContext = (player) => {
  // 构建AI对话上下文
  const context = {
    playerName: player.playerName,
    playerNumber: player.playerNumber,
    role: player.role,
    isAlive: player.isAlive,
    currentPhase: currentPhase.value,
    currentRound: currentRound.value,
    currentDay: currentDay.value,
    alivePlayers: alivePlayers.value.map(p => ({
      playerNumber: p.playerNumber,
      playerName: p.playerName,
      role: p.role,
      isAlive: p.isAlive
    })),
    dialogHistory: filteredDialogMessages.value.map(m => ({
      sender: m.sender,
      content: m.content,
      time: m.time,
      type: m.type
    })),
    gameMode: gameMode.value,
    roomInfo: roomInfo.value
  }
  
  return context
}

const callAiApi = async (aiPlayerConfig, context, retryCount = 0) => {
  if (retryCount >= MAX_RETRY_COUNT) {
    console.error('API调用重试次数已达上限，使用本地生成')
    return generateLocalSpeech(context)
  }
  
  // 检查是否正在请求
  if (isApiRequesting.value) {
    console.log('API请求正在进行中，等待...')
    return new Promise((resolve) => {
      apiRequestQueue.value.push({ aiPlayerConfig, context, resolve, retryCount })
    })
  }
  
  // 检查请求间隔
  const now = Date.now()
  const timeSinceLastRequest = now - lastApiRequestTime.value
  if (timeSinceLastRequest < API_REQUEST_INTERVAL) {
    const waitTime = API_REQUEST_INTERVAL - timeSinceLastRequest
    console.log(`等待${waitTime}ms后发送API请求`)
    await new Promise(resolve => setTimeout(resolve, waitTime))
  }
  
  // 调用AI API生成对话
  isApiRequesting.value = true
  lastApiRequestTime.value = Date.now()
  
  try {
    const prompt = buildAiPrompt(context)
    
    let apiUrl, requestData
    
    if (aiPlayerConfig.modelType === 'deepseek') {
      let baseUrl = aiPlayerConfig.apiBaseUrl || 'https://api.deepseek.com/v1'
      if (!baseUrl.endsWith('/')) {
        baseUrl = baseUrl + '/'
      }
      if (!baseUrl.endsWith('chat/completions')) {
        baseUrl = baseUrl + 'chat/completions'
      }
      apiUrl = baseUrl
      requestData = {
        model: aiPlayerConfig.modelName || 'deepseek-chat',
        messages: [
          {
            role: 'system',
            content: `你是一个狼人杀游戏的玩家，你的身份是${context.role}，你的名字是${context.playerName}。请根据游戏情况进行发言。`
          },
          {
            role: 'user',
            content: prompt
          }
        ],
        temperature: (aiPlayerConfig.temperature || 7) / 10,
        max_tokens: aiPlayerConfig.maxTokens || 500
      }
    } else {
      let baseUrl = aiPlayerConfig.apiBaseUrl || 'https://api.openai.com/v1'
      if (!baseUrl.endsWith('/')) {
        baseUrl = baseUrl + '/'
      }
      if (!baseUrl.endsWith('chat/completions')) {
        baseUrl = baseUrl + 'chat/completions'
      }
      apiUrl = baseUrl
      requestData = {
        model: aiPlayerConfig.modelName || 'gpt-3.5-turbo',
        messages: [
          {
            role: 'system',
            content: `你是一个狼人杀游戏的玩家，你的身份是${context.role}，你的名字是${context.playerName}。请根据游戏情况进行发言。`
          },
          {
            role: 'user',
            content: prompt
          }
        ],
        temperature: (aiPlayerConfig.temperature || 7) / 10,
        max_tokens: aiPlayerConfig.maxTokens || 500
      }
    }
    
    console.log('API URL:', apiUrl)
    console.log('请求数据:', requestData)
    
    const response = await fetch(apiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${aiPlayerConfig.apiKey}`
      },
      body: JSON.stringify(requestData)
    })
    
    console.log('API响应状态:', response.status)
    
    if (!response.ok) {
      if (response.status === 429) {
        console.warn(`API调用频率过高，等待后重试 (第${retryCount + 1}次)`)
        await new Promise(resolve => setTimeout(resolve, 10000))
        isApiRequesting.value = false
        return callAiApi(aiPlayerConfig, context, retryCount + 1)
      }
      if (response.status === 402) {
        console.warn('API密钥无效或余额不足，使用本地生成')
        throw new Error('API密钥无效或余额不足')
      }
      throw new Error(`AI API调用失败: ${response.status}`)
    }
    
    const data = await response.json()
    
    if (data.output && data.output.choices) {
      return data.output.choices[0].message.content
    } else if (data.choices) {
      return data.choices[0].message.content
    } else {
      console.error('API返回格式不正确:', data)
      throw new Error('API返回格式不正确')
    }
  } catch (error) {
    console.error('API调用错误:', error)
    if (retryCount < MAX_RETRY_COUNT) {
      console.warn(`API调用失败，等待后重试 (第${retryCount + 1}次)`)
      await new Promise(resolve => setTimeout(resolve, 10000))
      isApiRequesting.value = false
      return callAiApi(aiPlayerConfig, context, retryCount + 1)
    }
    return generateLocalSpeech(context)
  } finally {
    isApiRequesting.value = false
    
    if (apiRequestQueue.value.length > 0) {
      const nextRequest = apiRequestQueue.value.shift()
      setTimeout(() => {
        callAiApi(nextRequest.aiPlayerConfig, nextRequest.context, nextRequest.retryCount || 0).then(nextRequest.resolve)
      }, API_REQUEST_INTERVAL)
    }
  }
}

const generateLocalSpeech = (context) => {
  const speeches = [
    '我觉得这个情况很复杂，需要仔细分析。',
    '根据目前的发言，我觉得有些可疑的地方。',
    '我建议大家冷静思考，不要急于下结论。',
    '这个游戏的规则很明确，我们需要按照规则来。',
    '我觉得我们应该先理清楚思路再发言。',
    '这个情况需要更多的信息才能做出判断。',
    '我建议大家多听听别人的意见。',
    '这个游戏需要团队合作，我们要互相配合。',
    '我觉得现在的局势对我们有利。',
    '我们需要保持警惕，不能掉以轻心。'
  ]
  return speeches[Math.floor(Math.random() * speeches.length)]
}

const buildAiPrompt = (context) => {
  // 构建AI提示词
  let prompt = `游戏信息：\n`
  prompt += `- 当前阶段：${context.currentPhase}\n`
  prompt += `- 当前回合：${context.currentRound}\n`
  prompt += `- 当前天数：${context.currentDay}\n`
  prompt += `- 你的身份：${context.role}\n`
  prompt += `- 你的编号：${context.playerNumber}\n`
  prompt += `- 你的名字：${context.playerName}\n\n`
  
  prompt += `存活玩家列表：\n`
  context.alivePlayers.forEach(p => {
    prompt += `${p.playerNumber}号 ${p.playerName}\n`
  })
  
  prompt += `\n对话历史：\n`
  context.dialogHistory.forEach(m => {
    prompt += `[${m.time}] ${m.sender}: ${m.content}\n`
  })
  
  prompt += `\n请根据以上信息，以${context.role}的身份进行发言。发言要符合你的身份和游戏情况。`
  
  return prompt
}

const getAiPersonality = (player) => {
  // 获取AI性格
  // 这里可以根据AI玩家的配置或随机生成
  const personalities = [
    { type: '谨慎型', speechStyle: '逻辑严谨，措辞谨慎', emotionalExpression: '冷静' },
    { type: '激进型', speechStyle: '直接攻击，语气强硬', emotionalExpression: '激动' },
    { type: '逻辑型', speechStyle: '条理清晰，分析深入', emotionalExpression: '理性' },
    { type: '直觉型', speechStyle: '凭感觉发言，直觉判断', emotionalExpression: '感性' },
    { type: '表演型', speechStyle: '生动形象，善于煽动', emotionalExpression: '丰富' },
    { type: '沉默型', speechStyle: '言简意赅，少言寡语', emotionalExpression: '内敛' }
  ]
  
  // 简单实现：根据玩家编号取模
  return personalities[player.playerNumber % personalities.length]
}

const getAiStrategy = (player) => {
  // 获取AI策略
  const strategies = {
    '狼人': ['伪装好人', '寻找神位', '保护同伴', '控制局面'],
    '预言家': ['报查验信息', '带队分析', '找狼', '安排工作'],
    '女巫': ['隐藏身份', '救好人', '毒狼人', '分析局势'],
    '猎人': ['隐藏身份', '准备开枪', '分析局势', '站边好人'],
    '守卫': ['保护神位', '隐藏身份', '分析刀型', '配合女巫'],
    '平民': ['找狼', '表水', '跟神走', '分析局势']
  }
  
  return strategies[player.role] || ['找狼', '表水', '跟神走']
}

const generateSpeechContent = (player, personality, strategy) => {
  // 生成发言内容
  const role = player.role
  const day = currentDay.value
  const phase = currentPhase.value
  
  // 基础发言模板
  let templates = []
  
  switch (role) {
    case '狼人':
      templates = [
        `大家好，我是${personality.type}平民，第${day}天了，场上情况很复杂。`,
        `我觉得${strategy[0]}很重要，我们需要仔细分析。`,
        `刚才的发言中，我发现了一些问题，${strategy[1]}是关键。`,
        `我建议大家关注一下${strategy[2]}，这对好人阵营很重要。`,
        `我的发言结束，过。`
      ]
      break
    case '预言家':
      templates = [
        `我是预言家，第${day}天验人信息是：...`,
        `根据我的查验，${strategy[0]}是关键。`,
        `我建议今天出狼，${strategy[1]}是我的带队方向。`,
        `女巫请救我，守卫请守我，${strategy[2]}。`,
        `我的发言结束，警徽流是...`
      ]
      break
    case '女巫':
      templates = [
        `我是平民，第${day}天了，场上情况需要仔细分析。`,
        `我觉得${strategy[0]}很重要，大家不要乱踩。`,
        `刚才的发言中，我觉得有人在带节奏，${strategy[1]}。`,
        `我建议大家冷静分析，${strategy[2]}是关键。`,
        `我的发言结束，过。`
      ]
      break
    default:
      templates = [
        `大家好，我是${personality.type}平民，第${day}天了。`,
        `我觉得场上情况需要仔细分析，${strategy[0]}是关键。`,
        `刚才的发言中，我注意到了一些细节，${strategy[1]}。`,
        `我建议大家跟随神的带队，${strategy[2]}。`,
        `我的发言结束，过。`
      ]
  }
  
  // 根据性格调整发言风格
  let content = templates.join(' ')
  
  if (personality.type === '激进型') {
    content = content.replace(/我觉得/gi, '我强烈认为')
    content = content.replace(/建议/gi, '要求')
  } else if (personality.type === '沉默型') {
    content = content.substring(0, content.length * 0.7) // 缩短发言
  } else if (personality.type === '表演型') {
    content = content + '！' // 增加感叹号
  }
  
  return content
}

const nextPhase = () => {
  console.log('nextPhase - 进入下一阶段')
  const phases = ['night', 'day', 'speak', 'vote']
  const currentIndex = phases.indexOf(currentPhase.value)
  const nextIndex = (currentIndex + 1) % phases.length
  currentPhase.value = phases[nextIndex]
  console.log('nextPhase - 当前阶段从', phases[currentIndex], '切换到', currentPhase.value)
  
  if (currentPhase.value === 'night') {
    currentRound.value++
    addRefereeMessage('天黑请闭眼。')
    
    // 夜晚行动顺序
    setTimeout(() => {
      addRefereeMessage('狼人请睁眼，选择要击杀的目标。')
      // 狼人行动逻辑 - 随机选择一个非狼人目标
      setTimeout(() => {
        const nonWolfPlayers = alivePlayers.value.filter(p => !['狼人', '狼王', '狼美人', '白狼王', '石像鬼'].includes(p.role))
        if (nonWolfPlayers.length > 0) {
          const target = nonWolfPlayers[Math.floor(Math.random() * nonWolfPlayers.length)]
          addRefereeMessage(`狼人选择击杀了${target.playerName}。`)
          // 这里可以添加女巫救药逻辑
        }
      }, 2000)
    }, 1000)
    
    setTimeout(() => {
      addRefereeMessage('狼人请闭眼，预言家请睁眼。')
      // 预言家行动逻辑 - 随机查验一个玩家
      setTimeout(() => {
        const seer = alivePlayers.value.find(p => p.role === '预言家')
        if (seer) {
          const checkTargets = alivePlayers.value.filter(p => p.id !== seer.id)
          if (checkTargets.length > 0) {
            const target = checkTargets[Math.floor(Math.random() * checkTargets.length)]
            const identity = ['狼人', '狼王', '狼美人', '白狼王', '石像鬼'].includes(target.role) ? '狼人' : '好人'
            addRefereeMessage(`预言家查验了${target.playerName}，身份是：${identity}`)
          }
        }
      }, 2000)
    }, 3000)
    
    setTimeout(() => {
      addRefereeMessage('预言家请闭眼，奇迹商人请睁眼。')
      // 奇迹商人行动逻辑
      setTimeout(() => {
        const merchant = alivePlayers.value.find(p => p.role === '奇迹商人')
        if (merchant && !merchantSkillUsed.value) {
          addRefereeMessage('奇迹商人选择了一名玩家作为幸运儿。')
          // 随机选择一个非自己的玩家
          const possibleTargets = alivePlayers.value.filter(p => p.id !== merchant.id)
          if (possibleTargets.length > 0) {
            const target = possibleTargets[Math.floor(Math.random() * possibleTargets.length)]
            // 随机选择技能类型
            const skillType = Math.random() > 0.5 ? '查验' : '毒药'
            addRefereeMessage(`奇迹商人选择了${target.playerName}作为幸运儿，给予${skillType}技能。`)
            
            // 检查目标是否为狼人
            const isWolf = ['狼人', '狼王', '狼美人', '白狼王', '石像鬼'].includes(target.role)
            if (isWolf) {
              setTimeout(() => {
                addRefereeMessage(`${target.playerName}是狼人，奇迹商人死亡。`)
                killPlayer(merchant.id)
              }, 1000)
            } else {
              setTimeout(() => {
                addRefereeMessage(`${target.playerName}是好人，奇迹商人存活。`)
              }, 1000)
            }
            
            // 标记技能已使用
            merchantSkillUsed.value = true
            luckyPlayerId.value = target.id
            luckyPlayerSkill.value = skillType === '查验' ? 'check' : 'poison'
          }
        }
      }, 2000)
    }, 5000)
    
    setTimeout(() => {
      addRefereeMessage('奇迹商人请闭眼，女巫请睁眼。')
      // 女巫行动逻辑
    }, 7000)
    
    setTimeout(() => {
      addRefereeMessage('女巫请闭眼，守卫请睁眼。')
      // 守卫行动逻辑
    }, 9000)
    
    setTimeout(() => {
      addRefereeMessage('守卫请闭眼，天亮了！')
      // 随机宣布死亡情况
      setTimeout(() => {
        nextPhase() // 进入白天
      }, 1000)
    }, 11000)
  } else if (currentPhase.value === 'day') {
    currentDay.value++
    addRefereeMessage(`天亮了，现在是第${currentDay.value}天。`)
    
    // 宣布夜晚死亡情况
    // 随机决定是否有人死亡
    const hasDeath = Math.random() > 0.5
    if (hasDeath && alivePlayers.value.length > 0) {
      const deadPlayer = alivePlayers.value[Math.floor(Math.random() * alivePlayers.value.length)]
      addRefereeMessage(`昨夜${deadPlayer.playerName}死亡，身份是：${deadPlayer.role}`)
      killPlayer(deadPlayer.id)
    } else {
      addRefereeMessage('昨夜平安夜，没有人死亡。')
    }
    
    // 幸运儿技能使用
    if (luckyPlayerId.value && luckyPlayerSkill.value) {
      const luckyPlayer = alivePlayers.value.find(p => p.id === luckyPlayerId.value)
      if (luckyPlayer) {
        setTimeout(() => {
          addRefereeMessage(`幸运儿${luckyPlayer.playerName}使用了${luckyPlayerSkill.value === 'check' ? '查验' : '毒药'}技能。`)
          if (luckyPlayerSkill.value === 'check') {
            // 查验技能
            const checkTargets = alivePlayers.value.filter(p => p.id !== luckyPlayer.id)
            if (checkTargets.length > 0) {
              const target = checkTargets[Math.floor(Math.random() * checkTargets.length)]
              const identity = ['狼人', '狼王', '狼美人', '白狼王', '石像鬼'].includes(target.role) ? '狼人' : '好人'
              addRefereeMessage(`幸运儿查验了${target.playerName}，身份是：${identity}`)
            }
          } else if (luckyPlayerSkill.value === 'poison') {
            // 毒药技能
            const poisonTargets = alivePlayers.value.filter(p => p.id !== luckyPlayer.id)
            if (poisonTargets.length > 0) {
              const target = poisonTargets[Math.floor(Math.random() * poisonTargets.length)]
              addRefereeMessage(`幸运儿选择毒杀了${target.playerName}`)
              killPlayer(target.id)
            }
          }
          // 重置幸运儿技能
          luckyPlayerId.value = null
          luckyPlayerSkill.value = null
        }, 2000)
      }
    }
    
    // 警长竞选（第一局）
    if (currentDay.value === 1) {
      addRefereeMessage('现在开始警长竞选，请想要竞选警长的玩家举手。')
      // 模拟警长竞选
      setTimeout(() => {
        const candidates = alivePlayers.value.filter(p => ['预言家', '猎人', '女巫'].includes(p.role))
        if (candidates.length > 0) {
          const sheriff = candidates[Math.floor(Math.random() * candidates.length)]
          setSheriff(sheriff.id)
        }
        // 警长竞选完成后，进入发言阶段
        setTimeout(() => {
          nextPhase()
        }, 3000)
      }, 4000) // 增加延迟，确保幸运儿技能使用完成
    } else {
      // 不是第一天，直接进入发言阶段
      setTimeout(() => {
        nextPhase()
      }, 4000) // 增加延迟，确保幸运儿技能使用完成
    }
  } else if (currentPhase.value === 'speak') {
    addRefereeMessage('发言阶段开始，请按顺序发言。')
    
    // 开始发言
    const firstSpeaker = alivePlayers.value[0]
    if (firstSpeaker) {
      startPlayerSpeaking(firstSpeaker.id)
    } else {
      // 没有人活着，游戏结束
      checkGameEnd()
    }
  } else if (currentPhase.value === 'vote') {
    addRefereeMessage('投票阶段开始，请选择要放逐的玩家。')
    
    // 模拟投票逻辑
    setTimeout(() => {
      if (alivePlayers.value.length > 0) {
        // 随机选择一个玩家放逐
        const votedPlayer = alivePlayers.value[Math.floor(Math.random() * alivePlayers.value.length)]
        addRefereeMessage(`${votedPlayer.playerName}被放逐出局！身份是：${votedPlayer.role}`)
        killPlayer(votedPlayer.id)
      }
      
      // 检查游戏是否结束
      if (!checkGameEnd()) {
        // 游戏未结束，进入下一夜
        nextPhase()
      }
    }, 5000)
  }
}

// 生命周期
onMounted(() => {
  startGame()
})
</script>

<style scoped>
.game-play {
  padding: 20px;
}

.game-play h2 {
  margin-bottom: 20px;
  color: #303133;
  text-align: center;
}

/* 游戏控制按钮 */
.game-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.view-mode-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.player-view-selector {
  display: flex;
  align-items: center;
}

/* 游戏主布局 */
.game-layout {
  display: grid;
  grid-template-columns: 250px 1fr 250px;
  gap: 20px;
  height: calc(100vh - 200px);
}

/* 面板通用样式 */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
}

/* 左侧和右侧面板 */
.left-panel,
.right-panel {
  display: flex;
  flex-direction: column;
}

.player-positions {
  display: flex;
  flex-direction: column;
  gap: 15px;
  height: 100%;
  overflow-y: auto;
  padding: 10px;
}

.player-position {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid #e4e7ed;
  margin: 0 auto;
}

.player-position.occupied {
  background-color: #f0f9eb;
  border-color: #c2e7b0;
}

.player-position.empty {
  background-color: #ecf5ff;
  border-color: #91d5ff;
}

.player-position.empty:hover {
  background-color: #e6f7ff;
  border-color: #69c0ff;
  transform: scale(1.05);
}

.player-position.locked {
  background-color: #f5f7fa;
  border-color: #dcdfe6;
  cursor: not-allowed;
}

.player-info {
  text-align: center;
  padding: 10px;
  width: 100%;
}

.player-number {
  position: absolute;
  top: 5px;
  left: 5px;
  background-color: #409eff;
  color: white;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: bold;
}

.player-name {
  font-weight: bold;
  margin: 5px 0;
  font-size: 14px;
  color: #303133;
}

.player-status {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.sheriff-badge {
  position: absolute;
  top: 5px;
  right: 5px;
  background-color: #fadb14;
  color: #303133;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  font-weight: bold;
}

.role-badge {
  position: absolute;
  bottom: 5px;
  right: 5px;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  font-weight: bold;
}

.role-badge.role-wolf {
  background-color: #f56c6c;
  color: white;
}

.role-badge.role-villager {
  background-color: #ffffff;
  color: #303133;
  border: 1px solid #dcdfe6;
}

.role-badge.role-god {
  background-color: #e6a23c;
  color: white;
}

.empty-position {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #409eff;
}

.plus-icon {
  font-size: 24px;
  margin-bottom: 5px;
}

.locked-position {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #909399;
}

.lock-icon {
  font-size: 24px;
  margin-bottom: 5px;
}

/* 中间面板 */
.center-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
}

.dialog-content {
  max-height: 400px;
  overflow-y: auto;
  margin-bottom: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.dialog-message {
  margin-bottom: 15px;
  padding: 10px;
  border-radius: 8px;
  border-left: 4px solid #e4e7ed;
}

.dialog-message.player {
  background-color: #ecf5ff;
  border-left-color: #409eff;
}

.dialog-message.referee {
  background-color: #f0f9eb;
  border-left-color: #67c23a;
}

.dialog-message.system {
  background-color: #fafafa;
  border-left-color: #909399;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 12px;
}

.message-sender {
  font-weight: bold;
  color: #303133;
}

.message-time {
  color: #909399;
}

.message-content {
  font-size: 14px;
  line-height: 1.4;
  color: #303133;
}

.dialog-input {
  margin-top: 10px;
}

.input-actions {
  margin-top: 10px;
  text-align: right;
}

.ai-thinking-status {
  margin-bottom: 15px;
}

.ai-thinking-status .el-alert {
  margin-bottom: 8px;
}

.ai-speech-content {
  line-height: 1.6;
  font-size: 16px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  min-height: 100px;
}

.view-mode-selector {
  margin-top: 10px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .game-layout {
    grid-template-columns: 200px 1fr 200px;
  }
}

@media (max-width: 992px) {
  .game-layout {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto;
    height: auto;
  }
  
  .left-panel,
  .center-panel,
  .right-panel {
    width: 100%;
  }
  
  .player-positions {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    height: auto;
  }
  
  .player-position {
    width: 120px;
    height: 120px;
  }
}
</style>
