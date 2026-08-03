<template>
  <div class="records-page">
    <h2>{{ $t('gameRecords.title') }}</h2>
    <el-card v-loading="loading">
      <template #header>
        <div class="records-header">
          <h3>{{ $t('gameRecords.title') }}</h3>
          <el-button @click="fetchRecords">{{ $locale === 'zh-CN' ? '刷新' : 'Refresh' }}</el-button>
        </div>
      </template>
      <el-empty v-if="!loading && !records.length" :description="$locale === 'zh-CN' ? '暂无已完成的对局记录' : 'No completed games yet'" />
      <el-table v-else :data="pagedRecords" style="width:100%">
        <el-table-column prop="roomId" :label="$t('gameRecords.roomId')" width="100" />
        <el-table-column :label="$t('roomList.gameBoard')" min-width="180">
          <template #default="s">{{ boardName(s.row.gameBoard) }}</template>
        </el-table-column>
        <el-table-column prop="playerCount" :label="$t('roomList.playerCount')" width="100" />
        <el-table-column prop="winner" :label="$locale==='zh-CN'?'获胜方':'Winner'" width="120" />
        <el-table-column prop="duration" :label="$locale==='zh-CN'?'时长':'Duration'" width="100" />
        <el-table-column prop="startTime" :label="$locale==='zh-CN'?'开始时间':'Start'" width="180">
          <template #default="s">{{ s.row.startTime ? new Date(s.row.startTime).toLocaleString() : '' }}</template>
        </el-table-column>
        <el-table-column :label="$t('roomDetail.action')" width="150">
          <template #default="s">
            <el-button type="primary" size="small" @click="viewRecord(s.row)">{{ $t('gameRecords.viewRecords') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" :total="total" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" :title="$locale === 'zh-CN' ? '对局详情' : 'Game details'" width="760px">
      <template v-if="selectedRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="$t('gameRecords.roomId')">{{ selectedRecord.roomId }}</el-descriptions-item>
          <el-descriptions-item :label="$locale === 'zh-CN' ? '获胜方' : 'Winner'">{{ selectedRecord.winner }}</el-descriptions-item>
          <el-descriptions-item :label="$t('roomList.gameBoard')">{{ boardName(selectedRecord.gameBoard) }}</el-descriptions-item>
          <el-descriptions-item :label="$locale === 'zh-CN' ? '结束天数' : 'Final day'">{{ selectedRecord.dayNumber }}</el-descriptions-item>
        </el-descriptions>
        <h4>{{ $locale === 'zh-CN' ? '玩家结算' : 'Players' }}</h4>
        <div class="player-results">
          <span v-for="player in selectedRecord.payload.players || []" :key="player.number" :class="{ dead: !player.alive }">
            {{ player.number }}号 {{ player.name }} · {{ player.role }}
          </span>
        </div>
        <h4>{{ $locale === 'zh-CN' ? '末段公开记录' : 'Recent public log' }}</h4>
        <div class="record-log">
          <p v-for="(message, index) in selectedRecord.payload.publicMessages || []" :key="index"><strong>{{ message.sender }}：</strong>{{ message.content }}</p>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const { proxy } = getCurrentInstance()
const $t = proxy.$t; const $locale = proxy.$locale
const currentLocale = () => ($locale?.value || $locale) === 'en-US' ? 'en-US' : 'zh-CN'
const records = ref([]); const page = ref(1); const size = ref(10); const total = computed(() => records.value.length)
const loading = ref(false)
const detailVisible = ref(false)
const selectedRecord = ref(null)
const pagedRecords = computed(() => records.value.slice((page.value - 1) * size.value, page.value * size.value))

const parsePayload = value => {
  try { return JSON.parse(value || '{}') }
  catch { return {} }
}
const formatDuration = (start, end) => {
  if (!start || !end) return '-'
  const seconds = Math.max(0, Math.floor((new Date(end) - new Date(start)) / 1000))
  const minutes = Math.floor(seconds / 60)
  return minutes >= 60 ? `${Math.floor(minutes / 60)}h ${minutes % 60}m` : `${minutes}m ${seconds % 60}s`
}
const boardName = key => key ? $t(`gameBoard.${key}`) : '-'
const fetchRecords = async () => {
  loading.value = true
  try {
    const [recordResponse, roomResponse] = await Promise.all([
      axios.get('/game/record/finished'),
      axios.get('/game/room/list')
    ])
    const finished = recordResponse.data?.code === 200 ? recordResponse.data.data : []
    const rooms = roomResponse.data?.code === 200 ? roomResponse.data.data : []
    const roomMap = new Map(rooms.map(room => [Number(room.id), room]))
    records.value = finished.map(record => {
      const payload = parsePayload(record.actionContent)
      const room = roomMap.get(Number(record.roomId)) || {}
      return {
        ...record,
        payload,
        gameBoard: payload.board || room.gameBoard,
        playerCount: payload.playerCount || room.playerCount,
        winner: record.targetPlayer || payload.winner || room.winner || '-',
        startTime: room.startTime || record.createTime,
        duration: formatDuration(room.startTime, room.endTime || record.createTime)
      }
    })
    page.value = 1
  } catch (error) {
    console.error('Game records load failed:', error)
    const prefix = currentLocale() === 'zh-CN' ? '游戏记录加载失败' : 'Could not load game records'
    ElMessage.error(`${prefix}：${error.message || '未知错误'}`)
  } finally {
    loading.value = false
  }
}
const viewRecord = record => { selectedRecord.value = record; detailVisible.value = true }
onMounted(fetchRecords)
</script>

<style scoped>
.records-page { padding: 10px 0; }
.records-page h2 { margin-bottom: 20px; }
.records-header { display:flex; align-items:center; justify-content:space-between; }
.records-header h3 { margin:0; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
.player-results { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:8px; margin-bottom:18px; }
.player-results span { padding:8px 10px; border:1px solid #d9d9d9; border-radius:4px; }
.player-results span.dead { opacity:.55; }
.record-log { max-height:320px; overflow:auto; border-top:1px solid #e5e5e5; }
.record-log p { margin:0; padding:9px 2px; border-bottom:1px solid #ededed; line-height:1.55; }
</style>
