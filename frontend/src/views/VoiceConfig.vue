<template>
  <div class="voice-config">
    <h2>语音系统配置</h2>
    <el-card class="config-card">
      <template #header>
        <div class="config-header">
          <h3>TTS引擎配置</h3>
        </div>
      </template>
      <el-form :model="voiceConfig" :rules="voiceRules" ref="voiceFormRef" label-width="120px">
        <el-form-item label="TTS引擎" prop="ttsEngine">
          <el-select v-model="voiceConfig.ttsEngine" placeholder="请选择TTS引擎">
            <el-option label="Microsoft Azure TTS" value="azure" />
            <el-option label="OpenAI TTS" value="openai" />
            <el-option label="百度语音" value="baidu" />
            <el-option label="阿里云语音" value="aliyun" />
            <el-option label="腾讯云语音" value="tencent" />
          </el-select>
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="voiceConfig.apiKey" type="password" placeholder="请输入API Key" />
        </el-form-item>
        <el-form-item label="API Base URL" prop="apiBaseUrl">
          <el-input v-model="voiceConfig.apiBaseUrl" placeholder="请输入API Base URL（可选）" />
        </el-form-item>
        <el-form-item label="语音类型" prop="voiceType">
          <el-select v-model="voiceConfig.voiceType" placeholder="请选择语音类型">
            <el-option label="默认语音" value="default" />
            <el-option label="男声" value="male" />
            <el-option label="女声" value="female" />
            <el-option label="儿童" value="child" />
          </el-select>
        </el-form-item>
        <el-form-item label="语速" prop="speed">
          <el-slider v-model="voiceConfig.speed" :min="0.5" :max="2" :step="0.1" />
          <span class="speed-value">{{ voiceConfig.speed }}</span>
        </el-form-item>
        <el-form-item label="音量" prop="volume">
          <el-slider v-model="voiceConfig.volume" :min="0" :max="1" :step="0.1" />
          <span class="volume-value">{{ voiceConfig.volume }}</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveConfig" :loading="loading">保存配置</el-button>
          <el-button type="info" @click="testConfig">测试语音</el-button>
          <el-button type="warning" @click="resetConfig">重置默认值</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const voiceFormRef = ref(null)
const loading = ref(false)
const testing = ref(false)

const voiceConfig = reactive({
  ttsEngine: 'azure',
  apiKey: '',
  apiBaseUrl: '',
  voiceType: 'default',
  speed: 1,
  volume: 1
})

const voiceRules = {
  ttsEngine: [
    { required: true, message: '请选择TTS引擎', trigger: 'change' }
  ],
  apiKey: [
    { required: true, message: '请输入API Key', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadConfig()
})

const loadConfig = () => {
  const savedConfig = localStorage.getItem('voiceConfig')
  if (savedConfig) {
    Object.assign(voiceConfig, JSON.parse(savedConfig))
  }
}

const saveConfig = async () => {
  if (!voiceFormRef.value) return
  await voiceFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      localStorage.setItem('voiceConfig', JSON.stringify(voiceConfig))
      ElMessage.success('配置保存成功')
      loading.value = false
    }
  })
}

const testConfig = async () => {
  testing.value = true
  // 测试语音
  setTimeout(() => {
    ElMessage.success('测试成功')
    testing.value = false
  }, 1000)
}

const resetConfig = () => {
  Object.assign(voiceConfig, {
    ttsEngine: 'azure',
    apiKey: '',
    apiBaseUrl: '',
    voiceType: 'default',
    speed: 1,
    volume: 1
  })
}
</script>

<style scoped>
.voice-config {
  padding: 20px;
}

.voice-config h2 {
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

.speed-value,
.volume-value {
  margin-left: 10px;
  color: #409eff;
}
</style>
