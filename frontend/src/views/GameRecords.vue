<template>
  <div class="game-records">
    <h2>游戏记录</h2>
    <el-card class="records-card">
      <template #header>
        <div class="records-header">
          <h3>游戏历史记录</h3>
        </div>
      </template>
      <el-table :data="records" style="width: 100%">
        <el-table-column prop="id" label="记录ID" width="100" />
        <el-table-column prop="gameBoard" label="游戏板子" width="150" />
        <el-table-column prop="playerCount" label="玩家数量" width="100" />
        <el-table-column prop="winner" label="获胜方" width="120" />
        <el-table-column prop="duration" label="游戏时长" width="120" />
        <el-table-column prop="startTime" label="开始时间" width="200">
          <template #default="scope">
            {{ formatDate(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button type="primary" size="small" @click="viewRecord(scope.row.id)">查看详情</el-button>
            <el-button type="danger" size="small" @click="deleteRecord(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useGameStore } from '../stores/game'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const gameStore = useGameStore()

const records = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

onMounted(() => {
  fetchRecords()
})

const fetchRecords = async () => {
  loading.value = true
  await gameStore.fetchRecords({
    page: currentPage.value,
    size: pageSize.value
  })
  records.value = gameStore.getRecords
  total.value = gameStore.getTotalRecords
  loading.value = false
}

const handleSizeChange = (size) => {
  pageSize.value = size
  fetchRecords()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  fetchRecords()
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleString()
}

const viewRecord = (recordId) => {
  // 查看记录详情
  console.log('查看记录:', recordId)
}

const deleteRecord = async (recordId) => {
  // 删除记录
  console.log('删除记录:', recordId)
  ElMessage.success('记录删除成功')
  fetchRecords()
}
</script>

<style scoped>
.game-records {
  padding: 20px;
}

.game-records h2 {
  margin-bottom: 20px;
  color: #303133;
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.records-header h3 {
  margin: 0;
  color: #303133;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
