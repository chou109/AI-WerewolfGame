<template>
  <div class="room-list">
    <h2>游戏房间列表</h2>
    <div class="game-room-list">
      <el-card v-for="room in rooms" :key="room.id" class="game-room-card">
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
        <div class="room-actions">
          <el-button type="primary" size="small" @click="joinRoom(room.id)">加入房间</el-button>
          <el-button type="info" size="small" @click="viewRoom(room.id)">查看详情</el-button>
        </div>
      </el-card>
    </div>
    <div class="create-room">
      <el-button type="success" size="large" @click="goToCreateRoom">创建房间</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '../../stores/game'

const router = useRouter()
const gameStore = useGameStore()
const rooms = ref([])

onMounted(() => {
  fetchRooms()
})

const fetchRooms = async () => {
  await gameStore.fetchRooms()
  rooms.value = gameStore.getRooms
}

const getRoomStatus = (status) => {
  const statusMap = {
    1: '等待中',
    2: '游戏中',
    3: '已结束'
  }
  return statusMap[status] || '未知'
}

const joinRoom = (roomId) => {
  // 加入房间逻辑
  console.log('加入房间:', roomId)
}

const viewRoom = (roomId) => {
  router.push(`/game/room/${roomId}`)
}

const goToCreateRoom = () => {
  router.push('/game/room/create')
}
</script>

<style scoped>
.room-list {
  padding: 20px;
}

.room-list h2 {
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

.room-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.create-room {
  margin-top: 30px;
  text-align: center;
}
</style>
