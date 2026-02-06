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
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="aiPlayerForm.apiKey" type="password" placeholder="请输入API Key" />
        </el-form-item>
        <el-form-item label="API Base URL" prop="apiBaseUrl">
          <el-input v-model="aiPlayerForm.apiBaseUrl" placeholder="请输入API Base URL" />
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            @click="fetchModelList"
            :loading="fetchingModels"
          >
            获取模型列表
          </el-button>
          <span v-if="modelList.length > 0" class="model-list-info">
            已获取 {{ modelList.length }} 个模型
          </span>
        </el-form-item>
        <el-form-item label="模型类型" prop="modelType">
          <template v-if="modelList.length > 0">
            <el-select v-model="aiPlayerForm.modelType" placeholder="请选择模型类型" style="width: 100%">
              <template v-if="modelTypes.length > 0">
                <el-option 
                  v-for="type in modelTypes" 
                  :key="type.value" 
                  :label="type.label" 
                  :value="type.value" 
                />
              </template>
              <template v-else>
                <el-option label="暂无可用模型类型" value="" disabled />
              </template>
            </el-select>
          </template>
          <template v-else>
            <el-select v-model="aiPlayerForm.modelType" placeholder="请先获取模型列表" disabled style="width: 100%">
              <el-option label="请先获取模型列表" value="" disabled />
            </el-select>
          </template>
        </el-form-item>
        <el-form-item label="模型" prop="modelName">
          <template v-if="modelList.length > 0 && aiPlayerForm.modelType">
            <el-select v-model="aiPlayerForm.modelName" placeholder="请选择模型" style="width: 100%">
              <template v-if="filteredModels.length > 0">
                <el-option 
                  v-for="model in filteredModels" 
                  :key="model.id || model.name" 
                  :label="model.name || model.id" 
                  :value="model.id || model.name" 
                />
              </template>
              <template v-else>
                <el-option label="该类型暂无可用模型" value="" disabled />
              </template>
            </el-select>
          </template>
          <template v-else-if="modelList.length > 0">
            <el-select v-model="aiPlayerForm.modelName" placeholder="请先选择模型类型" disabled style="width: 100%">
              <el-option label="请先选择模型类型" value="" disabled />
            </el-select>
          </template>
          <template v-else>
            <el-input v-model="aiPlayerForm.modelName" placeholder="请先获取模型列表" disabled style="width: 100%" />
          </template>
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
import { ref, reactive, onMounted, computed, watch } from 'vue'
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
  modelType: '',
  modelName: '',
  apiKey: '',
  apiBaseUrl: '',
  personality: '',
  strategy: '',
  temperature: 0.7,
  maxTokens: 1000,
  status: 1
})

const modelList = ref([])
const modelTypes = ref([])
const fetchingModels = ref(false)

// 根据选择的模型类型过滤模型列表
const filteredModels = computed(() => {
  if (!aiPlayerForm.modelType) {
    return []
  }
  
  console.log('Filtering models for type:', aiPlayerForm.modelType)
  const filtered = modelList.value.filter(model => {
    const modelInfo = model.name || model.id || model.model || JSON.stringify(model)
    if (!modelInfo) return false
    
    console.log('Checking model:', modelInfo)
    const lowerInfo = modelInfo.toLowerCase()
    
    // 处理不同格式的模型信息
    switch (aiPlayerForm.modelType) {
      case 'qwen':
        return lowerInfo.includes('qwen')
      case 'deepseek':
        return lowerInfo.includes('deepseek')
      case 'glm':
        return lowerInfo.includes('glm')
      case 'moonshot':
        return lowerInfo.includes('moonshot')
      case 'gpt':
      case 'openai':
        return lowerInfo.includes('gpt') || true // 对于openai类型，显示所有模型
      case 'claude':
      case 'anthropic':
        return lowerInfo.includes('claude') || true // 对于anthropic类型，显示所有模型
      case 'modelscope':
        return true // 显示所有模型
      default:
        return true // 默认显示所有模型
    }
  })
  
  console.log('Filtered models:', filtered)
  return filtered
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

const resetForm = () => {
  Object.assign(aiPlayerForm, {
    id: null,
    name: '',
    modelType: '',
    modelName: '',
    apiKey: '',
    apiBaseUrl: '',
    personality: '',
    strategy: '',
    temperature: 0.7,
    maxTokens: 1000,
    status: 1
  })
  modelList.value = []
  modelTypes.value = []
}

// 监听模型类型变化，设置默认配置
watch(
    () => aiPlayerForm.modelType,
    (newType) => {
      if (newType === 'modelscope') {
        aiPlayerForm.apiBaseUrl = 'https://api-inference.modelscope.cn/v1'
      } else if (newType === 'openai') {
        aiPlayerForm.apiBaseUrl = 'https://api.openai.com/v1'
      } else if (newType === 'anthropic') {
        aiPlayerForm.apiBaseUrl = 'https://api.anthropic.com/v1'
      }
      // 不要清空模型列表，保留已获取的模型数据
    }
  )

const saveAiPlayer = async () => {
  if (!aiPlayerFormRef.value) return
  await aiPlayerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 自动处理ModelScope API密钥格式：去除ms-前缀
        let formData = { ...aiPlayerForm }
        if (formData.modelType === 'modelscope' && formData.apiKey && formData.apiKey.startsWith('ms-')) {
          formData.apiKey = formData.apiKey.substring(3)
          console.log('自动去除ModelScope API密钥中的ms-前缀')
        }
        
        let response
        if (aiPlayerForm.id) {
          response = await axios.put('/ai/player/update', formData)
        } else {
          response = await axios.post('/ai/player/create', formData)
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

const fetchModelList = async () => {
  if (!aiPlayerForm.apiKey) {
    ElMessage.warning('请先输入API Key')
    return
  }
  
  if (!aiPlayerForm.apiBaseUrl) {
    ElMessage.warning('请先输入API Base URL')
    return
  }
  
  if (fetchingModels.value) {
    ElMessage.warning('正在获取模型列表，请稍候')
    return
  }
  
  fetchingModels.value = true
  try {
    // 确保API Base URL格式正确
    let apiUrl = aiPlayerForm.apiBaseUrl.trim()
    if (!apiUrl.startsWith('http')) {
      apiUrl = 'https://' + apiUrl
    }
    if (!apiUrl.endsWith('/')) {
      apiUrl += '/'
    }
    
    const response = await axios.get(`${apiUrl}models`, {
      headers: {
        'Authorization': `Bearer ${aiPlayerForm.apiKey}`
      }
    })
    
    if (response.data && Array.isArray(response.data.data)) {
      modelList.value = response.data.data
      
      // 从模型数据中提取模型类型
      const types = new Set()
      
      // 打印模型数据结构，方便调试
      console.log('Model data structure:', modelList.value[0])
      
      modelList.value.forEach(model => {
        // 尝试从不同字段中提取信息
        const modelInfo = model.name || model.id || model.model || JSON.stringify(model)
        console.log('Model info:', modelInfo)
        
        // 从模型名称中提取更详细的类型信息
        if (modelInfo) {
          const lowerInfo = modelInfo.toLowerCase()
          if (lowerInfo.includes('qwen')) {
            types.add('qwen')
          } else if (lowerInfo.includes('deepseek')) {
            types.add('deepseek')
          } else if (lowerInfo.includes('glm')) {
            types.add('glm')
          } else if (lowerInfo.includes('moonshot')) {
            types.add('moonshot')
          } else if (lowerInfo.includes('gpt')) {
            types.add('gpt')
          } else if (lowerInfo.includes('claude')) {
            types.add('claude')
          } else {
            // 其他ModelScope模型
            types.add('modelscope')
          }
        }
      })
      
      // 转换为下拉框选项格式
      modelTypes.value = Array.from(types).map(type => {
        const typeMap = {
          'qwen': 'Qwen',
          'deepseek': 'DeepSeek',
          'glm': 'GLM',
          'moonshot': 'Moonshot',
          'gpt': 'GPT',
          'claude': 'Claude',
          'modelscope': 'ModelScope'
        }
        return {
          value: type,
          label: typeMap[type] || type
        }
      })
      
      ElMessage.success(`模型列表获取成功，发现 ${types.size} 种模型类型，共 ${modelList.value.length} 个模型`)
    } else {
      ElMessage.error('模型列表获取失败：响应格式不正确')
    }
  } catch (error) {
    console.error('Fetch models error:', error)
    ElMessage.error(`模型列表获取失败：${error.message || '未知错误'}`)
  } finally {
    fetchingModels.value = false
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

.ai-player-list-card {
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

.model-list-info {
  margin-left: 10px;
  color: #67c23a;
}

.slider-value {
  margin-left: 10px;
  color: #409eff;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
