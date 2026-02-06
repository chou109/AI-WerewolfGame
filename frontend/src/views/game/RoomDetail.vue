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
          <el-table-column prop="playerName" label="玩家名称" />
          <el-table-column prop="role" label="角色" />
          <el-table-column prop="status" label="状态">
            <template #default="scope">
              {{ getPlayerStatus(scope.row.status) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="room-actions">
        <el-button type="primary" size="large" @click="startGame" :disabled="!canStartGame">开始游戏</el-button>
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

const router = useRouter()
const route = useRoute()
const gameStore = useGameStore()
const room = reactive({})
const players = ref([])
const loading = ref(false)

const canStartGame = computed(() => {
  return room.status === 1 && room.playerCount === (players.value || []).length
})

onMounted(() => {
  fetchRoomDetail()
  fetchPlayers()
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
  players.value = playerList
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

.room-actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>
