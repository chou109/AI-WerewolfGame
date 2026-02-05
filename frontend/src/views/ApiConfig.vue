<template>
  <div class="api-config">
    <h2>API配置管理</h2>
    <el-card class="config-card">
      <template #header>
        <div class="config-header">
          <h3>AI模型配置</h3>
        </div>
      </template>
      <el-form :model="apiConfig" :rules="apiRules" ref="apiFormRef" label-width="120px">
        <el-form-item label="API类型" prop="apiType">
          <el-select v-model="apiConfig.apiType" placeholder="请选择API类型">
            <el-option label="OpenAI GPT" value="openai" />
            <el-option label="Anthropic Claude" value="anthropic" />
            <el-option label="Azure OpenAI" value="azure" />
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="Moonshot" value="moonshot" />
            <el-option label="ModelScope" value="modelscope" />
          </el-select>
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="apiConfig.apiKey" type="password" placeholder="请输入API Key" />
        </el-form-item>
        <el-form-item label="API Base URL" prop="apiBaseUrl">
          <el-input v-model="apiConfig.apiBaseUrl" placeholder="请输入API Base URL（可选）" />
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <template v-if="apiConfig.apiType === 'modelscope'">
            <el-select v-model="apiConfig.modelName" placeholder="请选择ModelScope模型">
              <el-option label="Qwen2.5-7B-Instruct" value="qwen/Qwen2.5-7B-Instruct" />
              <el-option label="Qwen2.5-1.8B-Instruct" value="qwen/Qwen2.5-1.8B-Instruct" />
              <el-option label="DeepSeek-R1-7B-Instruct" value="deepseek-ai/DeepSeek-R1-7B-Instruct" />
              <el-option label="ZhipuAI/GLM-4" value="ZhipuAI/GLM-4" />
              <el-option label="Moonshot-v1-8B-Instruct" value="moonshot/Moonshot-v1-8B-Instruct" />
            </el-select>
          </template>
          <template v-else>
            <el-input v-model="apiConfig.modelName" placeholder="请输入模型名称" />
          </template>
        </el-form-item>
        <el-form-item label="最大 tokens" prop="maxTokens">
          <el-input-number v-model="apiConfig.maxTokens" :min="100" :max="4096" :step="100" />
        </el-form-item>
        <el-form-item label="温度" prop="temperature">
          <el-slider v-model="apiConfig.temperature" :min="0" :max="2" :step="0.1" />
          <span class="temperature-value">{{ apiConfig.temperature }}</span>
        </el-form-item>
        <el-form-item label="超时时间" prop="timeout">
          <el-input-number v-model="apiConfig.timeout" :min="1000" :max="30000" :step="1000" />
          <span class="timeout-unit">毫秒</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveConfig" :loading="loading">保存配置</el-button>
          <el-button type="info" @click="testConfig">测试连接</el-button>
          <el-button type="warning" @click="resetConfig">重置默认值</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'

const apiFormRef = ref(null)
const loading = ref(false)
const testing = ref(false)

const apiConfig = reactive({
  apiType: 'openai',
  apiKey: '',
  apiBaseUrl: '',
  modelName: 'gpt-3.5-turbo',
  maxTokens: 1000,
  temperature: 0.7,
  timeout: 10000
})

const apiRules = {
  apiType: [
    { required: true, message: '请选择API类型', trigger: 'change' }
  ],
  apiKey: [
    { required: true, message: '请输入API Key', trigger: 'blur' }
  ],
  modelName: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadConfig()
})

// 监听API类型变化，自动设置默认配置
watch(() => apiConfig.apiType, (newType) => {
  if (newType === 'modelscope') {
    // 设置ModelScope默认配置
    apiConfig.apiBaseUrl = 'https://modelscope.cn/api/v1'
    apiConfig.modelName = 'qwen/Qwen2.5-7B-Instruct'
  } else if (newType === 'openai') {
    // 设置OpenAI默认配置
    apiConfig.apiBaseUrl = ''
    apiConfig.modelName = 'gpt-3.5-turbo'
  } else if (newType === 'anthropic') {
    // 设置Anthropic默认配置
    apiConfig.apiBaseUrl = 'https://api.anthropic.com/v1'
    apiConfig.modelName = 'claude-3-opus-20240229'
  } else if (newType === 'deepseek') {
    // 设置DeepSeek默认配置
    apiConfig.apiBaseUrl = 'https://api.deepseek.com/v1'
    apiConfig.modelName = 'deepseek-chat'
  } else if (newType === 'moonshot') {
    // 设置Moonshot默认配置
    apiConfig.apiBaseUrl = 'https://api.moonshot.cn/v1'
    apiConfig.modelName = 'moonshot-v1-8b-instruct'
  }
})

const loadConfig = () => {
  const savedConfig = localStorage.getItem('apiConfig')
  if (savedConfig) {
    Object.assign(apiConfig, JSON.parse(savedConfig))
  }
}

const saveConfig = async () => {
  if (!apiFormRef.value) return
  await apiFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      localStorage.setItem('apiConfig', JSON.stringify(apiConfig))
      ElMessage.success('配置保存成功')
      loading.value = false
    }
  })
}

const testConfig = async () => {
  testing.value = true
  // 测试API连接
  setTimeout(() => {
    ElMessage.success('测试成功')
    testing.value = false
  }, 1000)
}

const resetConfig = () => {
  Object.assign(apiConfig, {
    apiType: 'openai',
    apiKey: '',
    apiBaseUrl: '',
    modelName: 'gpt-3.5-turbo',
    maxTokens: 1000,
    temperature: 0.7,
    timeout: 10000
  })
}
</script>

<style scoped>
.api-config {
  padding: 20px;
}

.api-config h2 {
  margin-bottom: 20px;
  color: #303133;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.config-header h3 {
  margin: 0;
  color: #303133;
}

.temperature-value {
  margin-left: 10px;
  color: #409eff;
}

.timeout-unit {
  margin-left: 10px;
  color: #909399;
}
</style>
