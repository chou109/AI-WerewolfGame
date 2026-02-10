<template>
  <div class="room-detail">
    <h2>房间详情</h2>
    <el-card class="room-info-card">
      <template #header>
        <div class="room-header">
          <h3>{{ room.roomName }}</h3>
          <span class="room-code">{{ room.roomCode }}</span>
        </div>
      </template>
      <div class="room-info">
        <p>玩家数量：{{ room.playerCount }}人</p>
        <p>游戏板子：{{ room.gameBoard }}</p>
        <p>状态：{{ getRoomStatus(room.status) }}</p>
      </div>
      <div class="player-list">
        <h4>玩家列表</h4>
        <el-table :data="players" style="width: 100%">
          <el-table-column prop="playerNumber" label="编号" width="80" />
          <el-table-column prop="playerName" label="玩家名称">
            <template #default="scope">
              {{ getPlayerName(scope.row) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态">
            <template #default="scope">
              {{ getPlayerStatus(scope.row.status) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" v-if="room.status === 1">
            <template #default="scope">
              <el-button type="danger" size="small" @click="deletePlayer(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <!-- AI玩家加入功能 -->
      <div v-if="room.status === 1" class="ai-player-section">
        <h4>AI玩家加入</h4>
        <el-form :inline="true" class="ai-player-form">
          <el-form-item label="选择AI玩家">
            <el-select v-model="selectedAiPlayerId" placeholder="请选择AI玩家" style="width: 200px">
              <el-option
                v-for="aiPlayer in aiPlayers"
                :key="aiPlayer.id"
                :label="aiPlayer.name"
                :value="aiPlayer.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="玩家编号">
            <el-select v-model="aiPlayerNumber" placeholder="请选择玩家编号" style="width: 120px">
              <el-option
                v-for="number in availablePlayerNumbers"
                :key="number"
                :label="number"
                :value="number"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="addAiPlayerToRoom" :loading="addingAiPlayer" :disabled="!selectedAiPlayerId || !aiPlayerNumber">添加AI玩家</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <div class="room-actions">
        <el-button type="primary" size="large" @click="enterRoom">进入房间</el-button>
        <el-button type="success" size="large" @click="startGame" :disabled="!canStartGame">开始游戏</el-button>
        <el-button type="danger" size="large" @click="leaveRoom">离开房间</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useGameStore } from '../../stores/game'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const gameStore = useGameStore()
const room = reactive({})  
const players = ref([])  
const aiPlayers = ref([])  
const selectedAiPlayerId = ref(null)  
const aiPlayerNumber = ref(null)  // 改为null，让用户从下拉列表选择  
const loading = ref(false)  
const addingAiPlayer = ref(false)  

const canStartGame = computed(() => {  
  return room.status === 1 && room.playerCount === (players.value || []).length  
})  

// 计算可用的玩家编号  
const availablePlayerNumbers = computed(() => {  
  const totalCount = room.playerCount || 12  // 默认12人  
  const usedNumbers = players.value.map(p => p.playerNumber)  
  const numbers = []  
  
  for (let i = 1; i <= totalCount; i++) {  
    if (!usedNumbers.includes(i)) {  
      numbers.push(i)  
    }  
  }  
  
  return numbers  
})

onMounted(() => {
  fetchRoomDetail()
  // 先获取AI玩家列表，再获取房间玩家列表
  fetchAiPlayers().then(() => {
    fetchPlayers()
  })
})

const fetchRoomDetail = async () => {
  const roomId = route.params.id
  if (!roomId) {
    ElMessage.error('房间ID不存在')
    router.push('/game/room/list')
    return
  }
  const roomDetail = await gameStore.getRoomById(roomId)
  Object.assign(room, roomDetail)
}

const fetchPlayers = async () => {
  const roomId = route.params.id
  if (!roomId) return
  const playerList = await gameStore.getPlayersByRoomId(roomId)
  // 确保每个AI玩家都有aiPlayerId字段
  const processedPlayers = playerList.map(player => {
    // 如果是AI玩家但没有aiPlayerId字段，尝试从playerName中提取
    if (player.userId === -1 && !player.aiPlayerId) {
      // 假设playerName格式为"AI玩家 X"，尝试从aiPlayers中匹配
      const aiPlayer = aiPlayers.value.find(ai => ai.name.includes(player.playerName))
      if (aiPlayer) {
        player.aiPlayerId = aiPlayer.id
      }
    }
    return player
  })
  
  // 按编号排序
  processedPlayers.sort((a, b) => a.playerNumber - b.playerNumber)
  
  players.value = processedPlayers
}

const deletePlayer = async (playerId) => {
  // 从房间中移除玩家（调用后端API实现软删除）
  try {
    const roomId = route.params.id
    const response = await axios.post('/game/player/remove', {
      roomId,
      playerId
    })
    
    if (response.data.code === 200) {
      // 从前端列表中移除玩家
      const playerIndex = players.value.findIndex(p => p.id === playerId)
      if (playerIndex !== -1) {
        const deletedPlayer = players.value[playerIndex]
        players.value.splice(playerIndex, 1)
        ElMessage.success(`玩家 ${getPlayerName(deletedPlayer)} 已从列表中移除`)
      }
    } else {
      ElMessage.error('玩家删除失败')
    }
  } catch (error) {
    console.error('Delete player error:', error)
    ElMessage.error('玩家删除失败')
  }
}

const fetchAiPlayers = async () => {
  try {
    const response = await axios.get('/ai/player/available')
    if (response.data.code === 200) {
      aiPlayers.value = response.data.data
    }
  } catch (error) {
    console.error('Fetch AI players error:', error)
    ElMessage.error('获取AI玩家列表失败')
  }
  return Promise.resolve()
}

const getRoomStatus = (status) => {
  const statusMap = {
    1: '等待中',
    2: '游戏中',
    3: '已结束'
  }
  return statusMap[status] || '未知'
}

const getPlayerStatus = (status) => {
  const statusMap = {
    1: '在线',
    2: '离线'
  }
  return statusMap[status] || '未知'
}

const getPlayerName = (player) => {
  if (player.userId === -1) {
    // 是AI玩家，尝试从aiPlayers中找到对应的名称
    console.log('AI Player:', player)
    console.log('AI Players:', aiPlayers.value)
    
    // 方法1：通过aiPlayerId匹配
    if (player.aiPlayerId) {
      const aiPlayer = aiPlayers.value.find(ai => ai.id == player.aiPlayerId) // 使用==运算符，忽略类型差异
      if (aiPlayer) {
        return aiPlayer.name
      }
    }
    
    // 方法2：通过playerName匹配
    if (player.playerName) {
      const aiPlayer = aiPlayers.value.find(ai => ai.name === player.playerName)
      if (aiPlayer) {
        return aiPlayer.name
      }
    }
    
    // 方法3：通过名称包含关系匹配
    if (player.playerName) {
      const aiPlayer = aiPlayers.value.find(ai => ai.name.includes(player.playerName) || player.playerName.includes(ai.name))
      if (aiPlayer) {
        return aiPlayer.name
      }
    }
    
    return player.playerName || `AI玩家 ${player.playerNumber}`
  }
  return player.playerName || `玩家 ${player.playerNumber}`
}

const addAiPlayerToRoom = async () => {
  if (!selectedAiPlayerId.value) {
    ElMessage.warning('请选择AI玩家')
    return
  }
  
  if (!aiPlayerNumber.value) {
    ElMessage.warning('请选择玩家编号')
    return
  }
  
  addingAiPlayer.value = true
  try {
    const roomId = route.params.id
    const aiPlayer = aiPlayers.value.find(ai => ai.id == selectedAiPlayerId.value) // 使用==运算符，忽略类型差异
    
    // 调用后端API添加AI玩家到房间
    const response = await axios.post('/game/player/add', {
      roomId,
      userId: -1, // 使用-1表示AI玩家
      aiPlayerId: selectedAiPlayerId.value, // 传递AI玩家ID
      playerNumber: aiPlayerNumber.value,
      playerName: aiPlayer.name // 直接传递AI玩家名称
    })
    
    if (response.data.code === 200) {
      ElMessage.success(`AI玩家 ${aiPlayer.name} 已加入房间`)
      // 刷新玩家列表
      await fetchPlayers()
      // 重置选择
      selectedAiPlayerId.value = null
      aiPlayerNumber.value = null // 重置为null，让用户重新选择
    } else {
      ElMessage.error('添加AI玩家失败')
    }
  } catch (error) {
    console.error('Add AI player error:', error)
    ElMessage.error('添加AI玩家失败')
  } finally {
    addingAiPlayer.value = false
  }
}

const startGame = async () => {
  if (!canStartGame.value) return
  loading.value = true
  const success = await gameStore.startGame(route.params.id)
  loading.value = false
  if (success) {
    ElMessage.success('游戏开始')
    router.push(`/game/play/${route.params.id}`)
  } else {
    ElMessage.error('游戏开始失败')
  }
}

const leaveRoom = async () => {
  const success = await gameStore.leaveRoom(route.params.id)
  if (success) {
    ElMessage.success('已离开房间')
    router.push('/game/room/list')
  } else {
    ElMessage.error('离开房间失败')
  }
}

const enterRoom = () => {
  // 进入房间，跳转到游戏页面
  router.push(`/game/play/${route.params.id}`)
}
</script>

<style scoped>
.room-detail {
  padding: 20px;
}

.room-detail h2 {
  margin-bottom: 20px;
  color: #303133;
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.room-header h3 {
  margin: 0;
  color: #303133;
}

.room-code {
  background-color: #ecf5ff;
  color: #409eff;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 14px;
}

.room-info {
  margin: 15px 0;
  line-height: 1.5;
}

.player-list {
  margin: 20px 0;
}

.player-list h4 {
  margin: 0 0 10px 0;
  color: #303133;
}

.ai-player-section {
  margin: 20px 0;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.ai-player-section h4 {
  margin: 0 0 15px 0;
  color: #303133;
}

.ai-player-form {
  margin-bottom: 10px;
}

.room-actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>
