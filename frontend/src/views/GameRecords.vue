<template>
  <div class="records-page">
    <h2>{{ $t('gameRecords.title') }}</h2>
    <el-card>
      <template #header><h3>{{ $t('gameRecords.title') }}</h3></template>
      <el-table :data="records" style="width:100%">
        <el-table-column prop="id" :label="$t('gameRecords.roomId')" width="100" />
        <el-table-column prop="gameBoard" :label="$t('roomList.gameBoard')" width="150" />
        <el-table-column prop="playerCount" :label="$t('roomList.playerCount')" width="100" />
        <el-table-column prop="winner" :label="$locale==='zh-CN'?'获胜方':'Winner'" width="120" />
        <el-table-column prop="duration" :label="$locale==='zh-CN'?'时长':'Duration'" width="100" />
        <el-table-column prop="startTime" :label="$locale==='zh-CN'?'开始时间':'Start'" width="180">
          <template #default="s">{{ s.row.startTime ? new Date(s.row.startTime).toLocaleString() : '' }}</template>
        </el-table-column>
        <el-table-column :label="$t('roomDetail.action')" width="150">
          <template #default="s">
            <el-button type="primary" size="small" @click="viewRecord(s.row.id)">{{ $t('gameRecords.viewRecords') }}</el-button>
            <el-button type="danger" size="small" @click="deleteRecord(s.row.id)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" :total="total" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { useGameStore } from '../stores/game'
import { ElMessage } from 'element-plus'

const { proxy } = getCurrentInstance()
const $t = proxy.$t; const $locale = proxy.$locale
const gameStore = useGameStore()
const records = ref([]); const page = ref(1); const size = ref(10); const total = ref(0)

const viewRecord = (id) => console.log('View', id)
const deleteRecord = (id) => { ElMessage.success($locale==='zh-CN'?'已删除':'Deleted') }
</script>

<style scoped>
.records-page { padding: 10px 0; }
.records-page h2 { margin-bottom: 20px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
