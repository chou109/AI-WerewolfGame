<template>
  <div class="create-room">
    <h2>创建游戏房间</h2>
    <el-form :model="roomForm" :rules="roomRules" ref="roomFormRef" label-width="100px">
      <el-form-item label="房间名称" prop="roomName">
        <el-input v-model="roomForm.roomName" placeholder="请输入房间名称" />
      </el-form-item>
      <el-form-item label="玩家数量" prop="playerCount">
        <el-select v-model="roomForm.playerCount" placeholder="请选择玩家数量">
          <el-option label="9人" value="9" />
          <el-option label="12人" value="12" />
        </el-select>
      </el-form-item>
      <el-form-item label="游戏板子" prop="gameBoard">
        <el-select v-model="roomForm.gameBoard" placeholder="请选择游戏板子">
          <el-option label="标准场" value="standard" />
          <el-option label="狼王守卫场" value="wolfking_guard" />
          <el-option label="奇迹商人场" value="miracle_merchant" />
        </el-select>
      </el-form-item>
      <el-form-item label="房间密码" prop="password">
          <el-input v-model="roomForm.password" placeholder="请输入房间密码（可选）" />
        </el-form-item>
        <el-form-item label="AI玩家" prop="aiPlayers">
          <el-select v-model="roomForm.aiPlayers" multiple placeholder="请选择AI玩家（可选）">
            <el-option 
              v-for="aiPlayer in aiPlayers" 
              :key="aiPlayer.id" 
              :label="`${aiPlayer.name} (${aiPlayer.modelType})`" 
              :value="aiPlayer.id" 
            />
          </el-select>
          <div class="ai-player-hint">
            <el-button type="text" @click="goToAiPlayerManager">管理AI玩家</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="createRoom" :loading="loading">创建房间</el-button>
          <el-button type="info" @click="cancel">取消</el-button>
        </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useGameStore } from '../../stores/game'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const gameStore = useGameStore()
const roomFormRef = ref(null)
const loading = ref(false)

const roomForm = reactive({
  roomName: '',
  playerCount: route.query.playerCount || '9',
  gameBoard: route.query.gameBoard || 'standard',
  password: '',
  aiPlayers: []
})

const aiPlayers = ref([])

const roomRules = {
  roomName: [
    { required: true, message: '请输入房间名称', trigger: 'blur' },
    { min: 2, max: 20, message: '房间名称长度在2-20个字符之间', trigger: 'blur' }
  ],
  playerCount: [
    { required: true, message: '请选择玩家数量', trigger: 'change' }
  ],
  gameBoard: [
    { required: true, message: '请选择游戏板子', trigger: 'change' }
  ]
}

const createRoom = async () => {
  if (!roomFormRef.value) return
  await roomFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      const success = await gameStore.createRoom({
        roomName: roomForm.roomName,
        playerCount: parseInt(roomForm.playerCount),
        gameBoard: roomForm.gameBoard,
        roomCode: '',
        status: 1,
        creatorId: 1,
        aiPlayers: roomForm.aiPlayers
      })
      loading.value = false
      if (success) {
        ElMessage.success('房间创建成功')
        router.push(`/game/room/${gameStore.getCurrentRoom.id}`)
      } else {
        ElMessage.error('房间创建失败')
      }
    }
  })
}

const cancel = () => {
  router.push('/game/room/list')
}

const goToAiPlayerManager = () => {
  router.push('/api')
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
}

onMounted(() => {
  fetchAiPlayers()
})
</script>

<style scoped>
.create-room {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.create-room h2 {
  margin-bottom: 20px;
  color: #303133;
}
</style>
