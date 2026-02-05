<template>
  <div class="ai-player-manager">
    <h2>AI玩家管理</h2>
    <div class="manager-actions">
      <el-button type="primary" @click="openCreateDialog">创建AI玩家</el-button>
    </div>
    <el-card class="ai-player-list-card">
      <template #header>
        <div class="card-header">
          <h3>AI玩家列表</h3>
        </div>
      </template>
      <el-table :data="aiPlayers" style="width: 100%">
        <el-table-column prop="name" label="AI玩家名称" />
        <el-table-column prop="modelType" label="模型类型" />
        <el-table-column prop="modelName" label="模型名称" />
        <el-table-column prop="personality" label="个性" />
        <el-table-column prop="strategy" label="策略" />
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            {{ scope.row.status === 1 ? '可用' : '禁用' }}
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button type="primary" size="small" @click="editAiPlayer(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteAiPlayer(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑AI玩家对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form :model="aiPlayerForm" :rules="aiPlayerRules" ref="aiPlayerFormRef" label-width="120px">
        <el-form-item label="AI玩家名称" prop="name">
          <el-input v-model="aiPlayerForm.name" placeholder="请输入AI玩家名称" />
        </el-form-item>
        <el-form-item label="模型类型" prop="modelType">
          <el-select v-model="aiPlayerForm.modelType" placeholder="请选择模型类型">
            <el-option label="OpenAI GPT" value="openai" />
            <el-option label="Anthropic Claude" value="anthropic" />
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="Moonshot" value="moonshot" />
            <el-option label="ModelScope" value="modelscope" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="aiPlayerForm.modelName" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="aiPlayerForm.apiKey" type="password" placeholder="请输入API Key" />
        </el-form-item>
        <el-form-item label="API Base URL" prop="apiBaseUrl">
          <el-input v-model="aiPlayerForm.apiBaseUrl" placeholder="请输入API Base URL（可选）" />
        </el-form-item>
        <el-form-item label="个性" prop="personality">
          <el-input v-model="aiPlayerForm.personality" placeholder="请输入AI玩家个性" />
        </el-form-item>
        <el-form-item label="策略" prop="strategy">
          <el-input v-model="aiPlayerForm.strategy" placeholder="请输入AI玩家策略" />
        </el-form-item>
        <el-form-item label="温度" prop="temperature">
          <el-slider v-model="aiPlayerForm.temperature" :min="0" :max="2" :step="0.1" />
          <span class="slider-value">{{ aiPlayerForm.temperature }}</span>
        </el-form-item>
        <el-form-item label="最大 tokens" prop="maxTokens">
          <el-input-number v-model="aiPlayerForm.maxTokens" :min="100" :max="4096" :step="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveAiPlayer" :loading="loading">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const aiPlayers = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('创建AI玩家')
const loading = ref(false)
const aiPlayerFormRef = ref(null)

const aiPlayerForm = reactive({
  id: null,
  name: '',
  modelType: 'openai',
  modelName: 'gpt-3.5-turbo',
  apiKey: '',
  apiBaseUrl: '',
  personality: '',
  strategy: '',
  temperature: 0.7,
  maxTokens: 1000,
  status: 1
})

const aiPlayerRules = {
  name: [
    { required: true, message: '请输入AI玩家名称', trigger: 'blur' },
    { min: 2, max: 20, message: 'AI玩家名称长度在2-20个字符之间', trigger: 'blur' }
  ],
  modelType: [
    { required: true, message: '请选择模型类型', trigger: 'change' }
  ],
  modelName: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ],
  apiKey: [
    { required: true, message: '请输入API Key', trigger: 'blur' }
  ]
}

onMounted(() => {
  fetchAiPlayers()
})

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

const openCreateDialog = () => {
  resetForm()
  dialogTitle.value = '创建AI玩家'
  dialogVisible.value = true
}

const editAiPlayer = (aiPlayer) => {
  Object.assign(aiPlayerForm, aiPlayer)
  dialogTitle.value = '编辑AI玩家'
  dialogVisible.value = true
}

const saveAiPlayer = async () => {
  if (!aiPlayerFormRef.value) return
  await aiPlayerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        let response
        if (aiPlayerForm.id) {
          response = await axios.put('/ai/player/update', aiPlayerForm)
        } else {
          response = await axios.post('/ai/player/create', aiPlayerForm)
        }
        if (response.data.code === 200) {
          ElMessage.success(aiPlayerForm.id ? 'AI玩家更新成功' : 'AI玩家创建成功')
          dialogVisible.value = false
          fetchAiPlayers()
        } else {
          ElMessage.error(response.data.message || '操作失败')
        }
      } catch (error) {
        console.error('Save AI player error:', error)
        ElMessage.error('操作失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const deleteAiPlayer = async (id) => {
  try {
    const response = await axios.delete(`/ai/player/delete/${id}`)
    if (response.data.code === 200) {
      ElMessage.success('AI玩家删除成功')
      fetchAiPlayers()
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error) {
    console.error('Delete AI player error:', error)
    ElMessage.error('删除失败')
  }
}

const resetForm = () => {
  Object.assign(aiPlayerForm, {
    id: null,
    name: '',
    modelType: 'openai',
    modelName: 'gpt-3.5-turbo',
    apiKey: '',
    apiBaseUrl: '',
    personality: '',
    strategy: '',
    temperature: 0.7,
    maxTokens: 1000,
    status: 1
  })
  if (aiPlayerFormRef.value) {
    aiPlayerFormRef.value.resetFields()
  }
}
</script>

<style scoped>
.ai-player-manager {
  padding: 20px;
}

.ai-player-manager h2 {
  margin-bottom: 20px;
  color: #303133;
}

.manager-actions {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  color: #303133;
}

.slider-value {
  margin-left: 10px;
  color: #409eff;
}

.dialog-footer {
  text-align: right;
}
</style>
