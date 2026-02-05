<template>
  <div class="game-play">
    <h2>游戏进行中</h2>
    <el-card class="game-status-card">
      <template #header>
        <div class="game-header">
          <h3>游戏状态</h3>
          <span class="game-phase">{{ currentPhase }}</span>
        </div>
      </template>
      <div class="game-info">
        <p>当前轮次：第{{ currentRound }}轮</p>
        <p>剩余玩家：{{ remainingPlayers }}人</p>
        <p>发言时间：{{ speakTime }}秒</p>
      </div>
    </el-card>
    
    <el-card class="player-status-card">
      <template #header>
        <div class="player-header">
          <h3>玩家状态</h3>
        </div>
      </template>
      <div class="player-grid">
        <div 
          v-for="player in players" 
          :key="player.id" 
          class="player-card"
          :class="{
            'player-alive': player.isAlive,
            'player-dead': !player.isAlive,
            'player-speaking': player.isSpeaking
          }"
        >
          <div class="player-name">{{ player.playerName }}</div>
          <div class="player-role" v-if="player.role && showRoles">{{ player.role }}</div>
          <div class="player-status">{{ player.isAlive ? '存活' : '死亡' }}</div>
        </div>
      </div>
    </el-card>
    
    <el-card class="game-action-card">
      <template #header>
        <div class="action-header">
          <h3>游戏操作</h3>
        </div>
      </template>
      <div class="game-actions">
        <el-button type="primary" @click="sendMessage" v-if="canSpeak">发送消息</el-button>
        <el-button type="success" @click="vote" v-if="canVote">投票</el-button>
        <el-button type="info" @click="useSkill" v-if="canUseSkill">使用技能</el-button>
        <el-button type="warning" @click="pass" v-if="canPass">跳过</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useGameStore } from '../../stores/game'

const router = useRouter()
const route = useRoute()
const gameStore = useGameStore()
const currentRound = ref(1)
const remainingPlayers = ref(12)
const speakTime = ref(120)
const currentPhase = ref('白天发言')
const showRoles = ref(false)

const players = ref([
  { id: 1, playerName: '玩家1', role: '狼人', isAlive: true, isSpeaking: false },
  { id: 2, playerName: '玩家2', role: '预言家', isAlive: true, isSpeaking: false },
  { id: 3, playerName: '玩家3', role: '女巫', isAlive: true, isSpeaking: false },
  { id: 4, playerName: '玩家4', role: '猎人', isAlive: true, isSpeaking: false },
  { id: 5, playerName: '玩家5', role: '守卫', isAlive: true, isSpeaking: false },
  { id: 6, playerName: '玩家6', role: '平民', isAlive: true, isSpeaking: false },
  { id: 7, playerName: '玩家7', role: '平民', isAlive: true, isSpeaking: false },
  { id: 8, playerName: '玩家8', role: '平民', isAlive: true, isSpeaking: false },
  { id: 9, playerName: '玩家9', role: '平民', isAlive: true, isSpeaking: false },
  { id: 10, playerName: '玩家10', role: '狼人', isAlive: true, isSpeaking: false },
  { id: 11, playerName: '玩家11', role: '狼人', isAlive: true, isSpeaking: false },
  { id: 12, playerName: '玩家12', role: '狼人', isAlive: true, isSpeaking: false }
])

const canSpeak = computed(() => {
  return currentPhase.value === '白天发言'
})

const canVote = computed(() => {
  return currentPhase.value === '投票阶段'
})

const canUseSkill = computed(() => {
  return currentPhase.value === '夜晚行动'
})

const canPass = computed(() => {
  return true
})

onMounted(() => {
  startGame()
})

const startGame = () => {
  // 开始游戏逻辑
  console.log('游戏开始')
}

const sendMessage = () => {
  // 发送消息逻辑
  console.log('发送消息')
}

const vote = () => {
  // 投票逻辑
  console.log('投票')
}

const useSkill = () => {
  // 使用技能逻辑
  console.log('使用技能')
}

const pass = () => {
  // 跳过逻辑
  console.log('跳过')
}
</script>

<style scoped>
.game-play {
  padding: 20px;
}

.game-play h2 {
  margin-bottom: 20px;
  color: #303133;
}

.game-header,
.player-header,
.action-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.game-header h3,
.player-header h3,
.action-header h3 {
  margin: 0;
  color: #303133;
}

.game-phase {
  background-color: #ecf5ff;
  color: #409eff;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 14px;
}

.game-info {
  margin: 15px 0;
  line-height: 1.5;
}

.player-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
  margin-top: 15px;
}

.player-card {
  padding: 10px;
  border-radius: 8px;
  text-align: center;
  transition: all 0.3s ease;
}

.player-alive {
  background-color: #f0f9eb;
  border: 1px solid #c2e7b0;
}

.player-dead {
  background-color: #fef0f0;
  border: 1px solid #fbc4c4;
  opacity: 0.6;
}

.player-speaking {
  background-color: #e6f7ff;
  border: 1px solid #91d5ff;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(145, 213, 255, 0.7);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(145, 213, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(145, 213, 255, 0);
  }
}

.player-name {
  font-weight: bold;
  margin-bottom: 5px;
}

.player-role {
  font-size: 12px;
  margin-bottom: 5px;
  color: #606266;
}

.player-status {
  font-size: 12px;
  color: #909399;
}

.game-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}
</style>
