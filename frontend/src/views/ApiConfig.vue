<template>
  <div class="ai-player-manager">
    <section class="manager-intro">
      <div>
        <span class="manager-kicker">01 / {{ $locale === 'zh-CN' ? '推理角色库' : 'REASONING ROSTER' }}</span>
        <h2>{{ $t('aiPlayer.title') }}</h2>
        <p>{{ $locale === 'zh-CN' ? '为每位 AI 玩家分配模型、性格与策略。连接配置将安全保存在本地服务中。' : 'Assign a model, personality, and strategy to every AI player in the roster.' }}</p>
      </div>
      <el-button type="primary" class="create-player-button" @click="openCreateDialog">
        <span>＋</span>{{ $t('aiPlayer.create') }}
      </el-button>
    </section>

    <el-card class="global-defaults-card">
      <template #header>
        <div class="card-header">
          <div>
            <span class="card-kicker">DEFAULT CONNECTION</span>
            <h3>{{ $t('aiPlayer.globalDefaults') }}</h3>
          </div>
          <span class="card-status"><i></i>{{ $locale === 'zh-CN' ? '可选配置' : 'OPTIONAL' }}</span>
        </div>
      </template>
      <el-form :model="globalDefaults" class="defaults-form" label-position="top">
        <el-form-item :label="$t('aiPlayer.apiKey')">
          <el-input v-model="globalDefaults.apiKey" type="password" :placeholder="$t('aiPlayer.apiKeyPlaceholder')" show-password />
        </el-form-item>
        <el-form-item :label="$t('aiPlayer.apiBaseUrl')">
          <el-input v-model="globalDefaults.apiBaseUrl" :placeholder="$t('aiPlayer.apiBaseUrlPlaceholder')" />
        </el-form-item>
        <el-form-item class="defaults-action">
          <el-button type="primary" @click="saveGlobalDefaults">{{ $t('aiPlayer.saveGlobalDefaults') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="ai-player-list-card">
      <template #header>
        <div class="card-header">
          <div>
            <span class="card-kicker">ACTIVE ROSTER</span>
            <h3>{{ $t('aiPlayer.listTitle') }}</h3>
          </div>
          <span class="roster-count">{{ aiPlayers.length }} {{ $locale === 'zh-CN' ? '位玩家' : 'PLAYERS' }}</span>
        </div>
      </template>
      <el-table :data="aiPlayers" class="player-table" table-layout="fixed" style="width: 100%">
        <el-table-column :label="$locale === 'zh-CN' ? '头像' : 'Avatar'" width="74" align="center">
          <template #default="scope">
            <div class="table-avatar">
              <img v-if="scope.row.avatarUrl" :src="scope.row.avatarUrl" :alt="scope.row.name" />
              <span v-else>{{ avatarPlaceholder(scope.row.name) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('aiPlayer.name')" width="142" show-overflow-tooltip />
        <el-table-column prop="modelType" :label="$t('aiPlayer.modelType')" width="118" show-overflow-tooltip />
        <el-table-column prop="modelName" :label="$t('aiPlayer.modelName')" min-width="170" show-overflow-tooltip />
        <el-table-column :label="$t('aiPlayer.speechLanguage')" width="104">
          <template #default="scope">{{ scope.row.language === 'en-US' ? 'English' : '中文' }}</template>
        </el-table-column>
        <el-table-column prop="personality" :label="$t('aiPlayer.personality')" min-width="154" show-overflow-tooltip />
        <el-table-column prop="strategy" :label="$t('aiPlayer.strategy')" min-width="220" show-overflow-tooltip />
        <el-table-column :label="$t('aiPlayer.status')" width="88">
          <template #default="scope">
            <span class="availability" :class="{ offline: scope.row.status !== 1 }"><i></i>{{ scope.row.status === 1 ? $t('common.enabled') : $t('common.disabled') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('aiPlayer.action')" width="218" fixed="right">
          <template #default="scope">
            <div class="table-actions">
              <el-button type="primary" size="small" @click="editAiPlayer(scope.row)">{{ $t('common.edit') }}</el-button>
              <el-button size="small" @click="openVoiceConfig(scope.row)">{{ $locale === 'zh-CN' ? '语音' : 'Voice' }}</el-button>
              <el-button type="danger" size="small" @click="deleteAiPlayer(scope.row.id)">{{ $t('common.delete') }}</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px">
      <el-form :model="aiPlayerForm" :rules="aiPlayerRules" ref="aiPlayerFormRef" label-width="130px">
        <!-- Quick Setup Presets -->
        <el-form-item :label="$t('aiPlayer.quickSetup')">
          <el-select v-model="selectedPreset" :placeholder="$t('aiPlayer.providerPreset')" style="width: 100%" @change="applyPreset">
            <el-option :label="'OpenAI'" value="openai" />
            <el-option :label="'DeepSeek'" value="deepseek" />
            <el-option :label="'Anthropic (Claude)'" value="anthropic" />
            <el-option :label="'ModelScope'" value="modelscope" />
            <el-option :label="$t('aiPlayer.customProvider')" value="custom" />
          </el-select>
        </el-form-item>

        <el-form-item :label="$t('aiPlayer.name')" prop="name">
          <el-input v-model="aiPlayerForm.name" :placeholder="$t('aiPlayer.nameRequired')" />
        </el-form-item>

        <el-form-item :label="$locale === 'zh-CN' ? '玩家头像' : 'Player avatar'">
          <div class="avatar-editor">
            <button type="button" class="avatar-preview" @click="avatarInputRef?.click()">
              <img v-if="aiPlayerForm.avatarUrl" :src="aiPlayerForm.avatarUrl" :alt="aiPlayerForm.name" />
              <span v-else>{{ avatarPlaceholder(aiPlayerForm.name) }}</span>
            </button>
            <div class="avatar-actions">
              <input ref="avatarInputRef" class="avatar-file-input" type="file" accept="image/png,image/jpeg,image/webp" @change="handleAvatarUpload" />
              <el-button @click="avatarInputRef?.click()">{{ $locale === 'zh-CN' ? '上传头像' : 'Upload' }}</el-button>
              <el-button v-if="aiPlayerForm.avatarUrl" text @click="aiPlayerForm.avatarUrl = ''">{{ $locale === 'zh-CN' ? '移除' : 'Remove' }}</el-button>
              <span class="field-hint">{{ $locale === 'zh-CN' ? '支持 PNG、JPG、WebP，保存前自动压缩。' : 'PNG, JPG or WebP; compressed before saving.' }}</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item :label="$t('aiPlayer.apiKey')" prop="apiKey">
          <el-input v-model="aiPlayerForm.apiKey" type="password" :placeholder="$t('aiPlayer.apiKeyPlaceholder')" show-password />
        </el-form-item>

        <el-form-item :label="$t('aiPlayer.apiBaseUrl')" prop="apiBaseUrl">
          <el-input v-model="aiPlayerForm.apiBaseUrl" :placeholder="$t('aiPlayer.apiBaseUrlPlaceholder')" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="fetchModelList" :loading="fetchingModels">
            {{ $t('aiPlayer.fetchModels') }}
          </el-button>
          <el-button @click="testConnection" :loading="testingConnection" :type="connectionStatus === 'success' ? 'success' : (connectionStatus === 'failed' ? 'danger' : 'default')">
            {{ testingConnection ? $t('aiPlayer.testing') : $t('aiPlayer.testConnection') }}
            <el-icon v-if="connectionStatus === 'success'" style="margin-left:4px;color:#67c23a"><component :is="'CircleCheck'" /></el-icon>
            <el-icon v-if="connectionStatus === 'failed'" style="margin-left:4px;color:#f56c6c"><component :is="'CircleClose'" /></el-icon>
          </el-button>
          <span v-if="modelList.length > 0" class="model-list-info">
            {{ $t('aiPlayer.modelsFetched', { count: modelList.length }) }}
          </span>
        </el-form-item>

        <el-form-item :label="$t('aiPlayer.modelType')" prop="modelType">
          <el-select v-model="aiPlayerForm.modelType" :placeholder="$t('aiPlayer.modelTypeRequired')" style="width: 100%" :disabled="modelList.length === 0">
            <el-option v-for="type in modelTypes" :key="type.value" :label="type.label" :value="type.value" />
          </el-select>
        </el-form-item>

        <el-form-item :label="$t('aiPlayer.modelName')" prop="modelName">
          <el-select v-if="modelList.length > 0 && aiPlayerForm.modelType" v-model="aiPlayerForm.modelName" :placeholder="$t('aiPlayer.modelNameRequired')" style="width: 100%">
            <el-option v-for="model in filteredModels" :key="model.id || model.name" :label="model.name || model.id" :value="model.id || model.name" />
          </el-select>
          <el-input v-else v-model="aiPlayerForm.modelName" :placeholder="$t('aiPlayer.modelNameRequired')" style="width: 100%" />
        </el-form-item>

        <el-form-item :label="$t('aiPlayer.speechLanguage')">
          <div class="language-field">
            <el-segmented v-model="aiPlayerForm.language" :options="speechLanguageOptions" />
            <span class="field-hint">{{ $t('aiPlayer.speechLanguageHint') }}</span>
          </div>
        </el-form-item>

        <el-form-item :label="$t('aiPlayer.personality')">
          <div class="tag-field">
            <el-select
              v-model="selectedPersonalityTags"
              multiple
              filterable
              allow-create
              default-first-option
              :reserve-keyword="false"
              :placeholder="$t('aiPlayer.personalityPlaceholder')"
              style="width: 100%"
            >
              <el-option v-for="tag in personalityPresets" :key="tag" :label="tag" :value="tag" />
            </el-select>
            <span class="field-hint">{{ $t('aiPlayer.personalityHint') }}</span>
          </div>
        </el-form-item>

        <el-form-item :label="$t('aiPlayer.strategy')">
          <div class="tag-field">
            <el-select
              v-model="selectedStrategyTags"
              multiple
              filterable
              allow-create
              default-first-option
              :reserve-keyword="false"
              :placeholder="$t('aiPlayer.strategyPlaceholder')"
              style="width: 100%"
            >
              <el-option v-for="tag in strategyPresets" :key="tag" :label="tag" :value="tag" />
            </el-select>
            <span class="field-hint">{{ $t('aiPlayer.strategyHint') }}</span>
          </div>
        </el-form-item>

        <el-form-item :label="$t('aiPlayer.temperature')">
          <div class="temperature-field">
            <div class="temperature-control">
              <el-slider v-model="aiPlayerForm.temperature" :min="0" :max="2" :step="0.1" style="flex:1" />
              <span class="slider-value">{{ aiPlayerForm.temperature }}</span>
              <span class="temp-label">{{ temperatureLabel }}</span>
            </div>
            <span class="field-hint">{{ $t('aiPlayer.temperatureHelp') }}</span>
          </div>
        </el-form-item>

        <el-form-item :label="$t('aiPlayer.maxTokens')">
          <el-input-number v-model="aiPlayerForm.maxTokens" :min="100" :max="8192" :step="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="primary" @click="saveAiPlayer" :loading="loading">{{ $t('common.save') }}</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="voiceDialogVisible" :title="voicePlayer ? `${voicePlayer.name} · 独立语音配置` : '独立语音配置'" width="520px">
      <el-form :model="voiceForm" label-position="top">
        <div class="voice-switch-row">
          <div><strong>继承全局语音</strong><span>使用“语音配置”页面中的统一引擎、音色和播放参数</span></div>
          <el-switch v-model="voiceForm.inherit" />
        </div>
        <template v-if="!voiceForm.inherit">
          <div class="voice-switch-row">
            <div><strong>启用该 AI 的语音</strong><span>只影响当前 AI 玩家</span></div>
            <el-switch v-model="voiceForm.enabled" />
          </div>
          <el-form-item label="语音引擎">
            <el-radio-group v-model="voiceForm.engine">
              <el-radio-button value="browser">浏览器语音</el-radio-button>
              <el-radio-button value="cloud">云端语音</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="voiceForm.engine === 'browser'" label="音色">
            <el-select v-model="voiceForm.voiceURI" clearable filterable placeholder="自动选择匹配发言语言的音色" style="width:100%">
              <el-option v-for="voice in browserVoices" :key="voice.voiceURI" :label="`${voice.name} (${voice.lang})`" :value="voice.voiceURI" />
            </el-select>
          </el-form-item>
          <el-form-item v-else label="云端音色">
            <el-select v-model="voiceForm.cloudVoice" filterable allow-create default-first-option placeholder="选择或输入音色名称" style="width:100%">
              <el-option v-for="voice in cloudVoiceOptions" :key="voice" :label="voice" :value="voice" />
            </el-select>
            <span class="field-hint">服务商、接口地址和密钥仍使用全局云端语音配置。</span>
          </el-form-item>
          <div class="voice-sliders">
            <el-form-item label="语速">
              <div class="voice-slider"><el-slider v-model="voiceForm.rate" :min="0.5" :max="2" :step="0.1" /><span>{{ voiceForm.rate.toFixed(1) }}x</span></div>
            </el-form-item>
            <el-form-item label="音调">
              <div class="voice-slider"><el-slider v-model="voiceForm.pitch" :min="0.5" :max="2" :step="0.1" :disabled="voiceForm.engine === 'cloud'" /><span>{{ voiceForm.pitch.toFixed(1) }}</span></div>
            </el-form-item>
            <el-form-item label="音量">
              <div class="voice-slider"><el-slider v-model="voiceForm.volume" :min="0" :max="1" :step="0.1" /><span>{{ Math.round(voiceForm.volume * 100) }}%</span></div>
            </el-form-item>
          </div>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="testVoiceConfig">试听</el-button>
        <el-button @click="voiceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveVoiceConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { speakText, stopSpeaking } from '../composables/useSpeechSynthesis.js'

const { proxy } = getCurrentInstance()
const $t = proxy.$t
const $locale = proxy.$locale

const router = useRouter()
const aiPlayers = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref($t('aiPlayer.create'))
const loading = ref(false)
const aiPlayerFormRef = ref(null)
const avatarInputRef = ref(null)
const modelList = ref([])
const modelTypes = ref([])
const fetchingModels = ref(false)
const testingConnection = ref(false)
const connectionStatus = ref('') // '', 'success', 'failed'
const selectedPreset = ref('')
const voiceDialogVisible = ref(false)
const voicePlayer = ref(null)
const browserVoices = ref([])
const voiceForm = reactive({ inherit: true, enabled: true, engine: 'browser', voiceURI: '', cloudVoice: 'alloy', rate: 1, pitch: 1, volume: 1 })
const AI_VOICE_STORAGE_KEY = 'werewolf:ai-voice-configs'
const cloudVoiceOptions = ['alloy', 'ash', 'ballad', 'coral', 'echo', 'fable', 'nova', 'onyx', 'sage', 'shimmer', 'verse', 'zh-CN-XiaoxiaoNeural', 'zh-CN-YunxiNeural', 'zh-CN-YunjianNeural', 'en-US-JennyNeural', 'en-US-GuyNeural']

const createVoiceConfig = saved => ({
  inherit: saved?.inherit !== false,
  enabled: saved?.enabled !== false,
  engine: saved?.engine === 'cloud' ? 'cloud' : 'browser',
  voiceURI: saved?.voiceURI || '',
  cloudVoice: saved?.cloudVoice || 'alloy',
  rate: Math.min(2, Math.max(0.5, Number(saved?.rate) || 1)),
  pitch: Math.min(2, Math.max(0.5, Number(saved?.pitch) || 1)),
  volume: Math.min(1, Math.max(0, Number.isFinite(Number(saved?.volume)) ? Number(saved.volume) : 1))
})
const readAiVoiceConfigs = () => {
  try { return JSON.parse(localStorage.getItem(AI_VOICE_STORAGE_KEY) || '{}') }
  catch { return {} }
}
const loadBrowserVoices = () => {
  if (!window.speechSynthesis) return
  browserVoices.value = window.speechSynthesis.getVoices()
}
const openVoiceConfig = player => {
  voicePlayer.value = player
  Object.assign(voiceForm, createVoiceConfig(readAiVoiceConfigs()[player.id]))
  voiceDialogVisible.value = true
}
const saveVoiceConfig = () => {
  if (!voicePlayer.value) return
  const configs = readAiVoiceConfigs()
  configs[voicePlayer.value.id] = createVoiceConfig(voiceForm)
  localStorage.setItem(AI_VOICE_STORAGE_KEY, JSON.stringify(configs))
  ElMessage.success(`${voicePlayer.value.name}的独立语音配置已保存`)
  voiceDialogVisible.value = false
}
const testVoiceConfig = async () => {
  if (!voicePlayer.value) return
  stopSpeaking()
  const overrides = voiceForm.inherit ? {} : {
    enabled: voiceForm.enabled,
    engine: voiceForm.engine,
    voiceURI: voiceForm.voiceURI,
    rate: voiceForm.rate,
    pitch: voiceForm.pitch,
    volume: voiceForm.volume,
    readPlayers: true,
    cloud: { playerVoice: voiceForm.cloudVoice, speed: voiceForm.rate }
  }
  await speakText(`${voicePlayer.value.name}语音测试。`, { ...overrides, force: true, interrupt: true, speaker: 'player', lang: voicePlayer.value.language || 'zh-CN' })
    .catch(error => ElMessage.error(`试听失败：${error.message || '语音引擎不可用'}`))
}

const splitTags = (value) => String(value || '')
  .split(/[、,，;；]/)
  .map(tag => tag.trim())
  .filter(Boolean)

const selectedPersonalityTags = computed({
  get: () => splitTags(aiPlayerForm.personality),
  set: tags => { aiPlayerForm.personality = [...new Set(tags)].join('、') }
})

const selectedStrategyTags = computed({
  get: () => splitTags(aiPlayerForm.strategy),
  set: tags => { aiPlayerForm.strategy = [...new Set(tags)].join('、') }
})

const currentLocale = () => $locale?.value || $locale
const personalityPresets = computed(() => currentLocale() === 'en-US'
  ? ['Calm', 'Logical', 'Intuitive', 'Assertive', 'Friendly', 'Talkative', 'Suspicious', 'Bold', 'Deceptive', 'Reserved', 'Empathetic', 'Steady']
  : ['沉着冷静', '逻辑缜密', '直觉敏锐', '强势自信', '温和亲和', '幽默健谈', '谨慎多疑', '激进果断', '善于伪装', '话少克制', '善于倾听', '情绪稳定'])

const strategyPresets = computed(() => currentLocale() === 'en-US'
  ? ['Listen First', 'Lead Discussion', 'Drive the Vote', 'Find Contradictions', 'Track Votes', 'Map Roles', 'Protect Power Roles', 'Hide Identity', 'Create Conflict', 'Deep Cover', 'Hard Push', 'Flexible Vote']
  : ['先听后判', '主动带队', '强势归票', '深挖矛盾', '关注票型', '盘身份链', '保护神职', '隐藏身份', '制造对立', '倒钩潜伏', '冲锋站边', '灵活变票'])

const speechLanguageOptions = computed(() => [
  { label: '中文', value: 'zh-CN' },
  { label: 'English', value: 'en-US' }
])

const avatarPlaceholder = name => String(name || 'AI').trim().slice(0, 2).toUpperCase() || 'AI'

const compressAvatar = file => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onerror = () => reject(new Error('read_failed'))
  reader.onload = () => {
    const image = new Image()
    image.onerror = () => reject(new Error('invalid_image'))
    image.onload = () => {
      const maxSide = 512
      const scale = Math.min(1, maxSide / Math.max(image.width, image.height))
      const canvas = document.createElement('canvas')
      canvas.width = Math.max(1, Math.round(image.width * scale))
      canvas.height = Math.max(1, Math.round(image.height * scale))
      const context = canvas.getContext('2d')
      context.fillStyle = '#101820'
      context.fillRect(0, 0, canvas.width, canvas.height)
      context.drawImage(image, 0, 0, canvas.width, canvas.height)
      resolve(canvas.toDataURL('image/jpeg', 0.84))
    }
    image.src = reader.result
  }
  reader.readAsDataURL(file)
})

const handleAvatarUpload = async event => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/') || file.size > 5 * 1024 * 1024) {
    ElMessage.warning($locale === 'zh-CN' ? '请选择不超过 5MB 的图片文件。' : 'Choose an image smaller than 5 MB.')
    return
  }
  try {
    aiPlayerForm.avatarUrl = await compressAvatar(file)
  } catch {
    ElMessage.error($locale === 'zh-CN' ? '头像读取失败，请更换图片。' : 'Could not read this image.')
  }
}

// Global defaults
const globalDefaults = reactive({
  apiKey: localStorage.getItem('globalApiKey') || '',
  apiBaseUrl: localStorage.getItem('globalApiBaseUrl') || ''
})

const aiPlayerForm = reactive({
  id: null,
  name: '',
  modelType: '',
  modelName: '',
  apiKey: '',
  apiBaseUrl: '',
  personality: '',
  strategy: '',
  language: 'zh-CN',
  avatarUrl: '',
  temperature: 0.7,
  maxTokens: 1000,
  status: 1
})

// Provider presets
const PRESETS = {
  openai: {
    apiBaseUrl: 'https://api.openai.com/v1',
    modelType: 'openai',
    models: [
      { id: 'gpt-4o', name: 'GPT-4o' },
      { id: 'gpt-4o-mini', name: 'GPT-4o Mini' },
      { id: 'gpt-4-turbo', name: 'GPT-4 Turbo' },
      { id: 'gpt-3.5-turbo', name: 'GPT-3.5 Turbo' }
    ]
  },
  deepseek: {
    apiBaseUrl: 'https://api.deepseek.com/v1',
    modelType: 'deepseek',
    models: [
      { id: 'deepseek-chat', name: 'DeepSeek V3' },
      { id: 'deepseek-reasoner', name: 'DeepSeek R1' }
    ]
  },
  anthropic: {
    apiBaseUrl: 'https://api.anthropic.com/v1',
    modelType: 'claude',
    models: [
      { id: 'claude-sonnet-4-20250514', name: 'Claude Sonnet 4' },
      { id: 'claude-3-5-sonnet-20241022', name: 'Claude 3.5 Sonnet' },
      { id: 'claude-3-haiku-20240307', name: 'Claude 3 Haiku' }
    ]
  },
  modelscope: {
    apiBaseUrl: 'https://api-inference.modelscope.cn/v1',
    modelType: 'modelscope',
    models: [
      { id: 'Qwen/Qwen2.5-72B-Instruct', name: 'Qwen 2.5 72B' },
      { id: 'deepseek-ai/DeepSeek-V3', name: 'DeepSeek V3' }
    ]
  }
}

const temperatureLabel = computed(() => {
  const t = aiPlayerForm.temperature
  if (t <= 0.4) return $t('aiPlayer.temperatureLabels.stable')
  if (t <= 0.9) return $t('aiPlayer.temperatureLabels.balanced')
  return $t('aiPlayer.temperatureLabels.varied')
})

const filteredModels = computed(() => {
  if (!aiPlayerForm.modelType) return []
  return modelList.value.filter(model => {
    const modelInfo = (model.name || model.id || model.model || '').toLowerCase()
    switch (aiPlayerForm.modelType) {
      case 'qwen': return modelInfo.includes('qwen')
      case 'deepseek': return modelInfo.includes('deepseek')
      case 'glm': return modelInfo.includes('glm')
      case 'moonshot': return modelInfo.includes('moonshot')
      case 'gpt': case 'openai': return modelInfo.includes('gpt') || true
      case 'claude': case 'anthropic': return modelInfo.includes('claude') || true
      case 'modelscope': return true
      default: return true
    }
  })
})

const aiPlayerRules = {
  name: [
    { required: true, message: () => $t('aiPlayer.nameRequired'), trigger: 'blur' },
    { min: 2, max: 20, message: () => $t('aiPlayer.nameLength'), trigger: 'blur' }
  ],
  modelType: [{ required: true, message: () => $t('aiPlayer.modelTypeRequired'), trigger: 'change' }],
  modelName: [{ required: true, message: () => $t('aiPlayer.modelNameRequired'), trigger: 'blur' }],
  apiKey: [{ required: true, message: () => $t('aiPlayer.apiKeyRequired'), trigger: 'blur' }]
}

onMounted(() => {
  fetchAiPlayers()
  loadBrowserVoices()
  window.speechSynthesis?.addEventListener('voiceschanged', loadBrowserVoices)
})
onUnmounted(() => {
  window.speechSynthesis?.removeEventListener('voiceschanged', loadBrowserVoices)
  stopSpeaking()
})

const fetchAiPlayers = async () => {
  try {
    const response = await axios.get('/ai/player/available')
    if (response.data.code === 200) {
      aiPlayers.value = response.data.data
    }
  } catch (error) {
    console.error('Fetch AI players error:', error)
    ElMessage.error($t('aiPlayer.fetchListFailed'))
  }
}

const applyPreset = (presetKey) => {
  if (presetKey === 'custom') {
    selectedPreset.value = ''
    return
  }
  const preset = PRESETS[presetKey]
  if (preset) {
    aiPlayerForm.apiBaseUrl = preset.apiBaseUrl
    aiPlayerForm.modelType = preset.modelType
    modelList.value = preset.models
    modelTypes.value = [{ value: preset.modelType, label: presetKey === 'anthropic' ? 'Claude' : (presetKey === 'modelscope' ? 'ModelScope' : presetKey.charAt(0).toUpperCase() + presetKey.slice(1)) }]
    ElMessage.success(`已应用 ${presetKey} 预设配置 / ${presetKey} preset applied`)
  }
}

const saveGlobalDefaults = () => {
  localStorage.setItem('globalApiKey', globalDefaults.apiKey)
  localStorage.setItem('globalApiBaseUrl', globalDefaults.apiBaseUrl)
  ElMessage.success($t('aiPlayer.saveGlobalDefaults'))
}

const openCreateDialog = () => {
  resetForm()
  dialogTitle.value = $t('aiPlayer.create')
  dialogVisible.value = true
}

const editAiPlayer = (aiPlayer) => {
  resetForm()
  Object.assign(aiPlayerForm, aiPlayer)
  dialogTitle.value = $t('aiPlayer.edit')
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(aiPlayerForm, {
    id: null, name: '', modelType: '', modelName: '',
    apiKey: globalDefaults.apiKey || '', apiBaseUrl: globalDefaults.apiBaseUrl || '',
    personality: '', strategy: '', language: 'zh-CN', avatarUrl: '', temperature: 0.7, maxTokens: 1000, status: 1
  })
  modelList.value = []
  modelTypes.value = []
  selectedPreset.value = ''
  connectionStatus.value = ''
}

watch(() => aiPlayerForm.modelType, (newType) => {
  if (newType === 'modelscope') aiPlayerForm.apiBaseUrl = 'https://api-inference.modelscope.cn/v1'
  else if (newType === 'openai' || newType === 'gpt') aiPlayerForm.apiBaseUrl = 'https://api.openai.com/v1'
  else if (newType === 'anthropic' || newType === 'claude') aiPlayerForm.apiBaseUrl = 'https://api.anthropic.com/v1'
})

const testConnection = async () => {
  if (!aiPlayerForm.apiKey) {
    ElMessage.warning($t('aiPlayer.needApiKey'))
    return
  }
  if (!aiPlayerForm.apiBaseUrl) {
    ElMessage.warning($t('aiPlayer.needApiUrl'))
    return
  }

  testingConnection.value = true
  connectionStatus.value = ''

  try {
    let apiUrl = aiPlayerForm.apiBaseUrl.trim()
    if (!apiUrl.startsWith('http')) apiUrl = 'https://' + apiUrl
    if (!apiUrl.endsWith('/')) apiUrl += '/'

    const response = await axios.get(`${apiUrl}models`, {
      headers: { 'Authorization': `Bearer ${aiPlayerForm.apiKey}` },
      timeout: 10000
    })

    if (response.data) {
      connectionStatus.value = 'success'
      ElMessage.success($t('aiPlayer.testSuccess'))
    } else {
      connectionStatus.value = 'failed'
      ElMessage.error($t('aiPlayer.testFailed'))
    }
  } catch (error) {
    connectionStatus.value = 'failed'
    ElMessage.error($t('aiPlayer.testFailed') + ': ' + (error.message || ''))
  } finally {
    testingConnection.value = false
  }
}

const saveAiPlayer = async () => {
  if (!aiPlayerFormRef.value) return
  await aiPlayerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        let formData = { ...aiPlayerForm }
        if (formData.apiKey) formData.apiKey = formData.apiKey.replace(/`/g, '').trim()
        if (formData.apiBaseUrl) formData.apiBaseUrl = formData.apiBaseUrl.replace(/`/g, '').trim()

        let response
        if (aiPlayerForm.id) {
          response = await axios.put('/ai/player/update', formData)
        } else {
          response = await axios.post('/ai/player/create', formData)
        }
        if (response.data.code === 200) {
          ElMessage.success(aiPlayerForm.id ? $t('aiPlayer.updateSuccess') : $t('aiPlayer.createSuccess'))
          dialogVisible.value = false
          fetchAiPlayers()
        } else {
          ElMessage.error(response.data.message || $t('aiPlayer.operationFailed'))
        }
      } catch (error) {
        console.error('Save AI player error:', error)
        ElMessage.error($t('aiPlayer.operationFailed'))
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
      ElMessage.success($t('aiPlayer.deleteSuccess'))
      fetchAiPlayers()
    } else {
      ElMessage.error(response.data.message || $t('aiPlayer.operationFailed'))
    }
  } catch (error) {
    console.error('Delete AI player error:', error)
    ElMessage.error($t('aiPlayer.operationFailed'))
  }
}

const fetchModelList = async () => {
  if (!aiPlayerForm.apiKey) { ElMessage.warning($t('aiPlayer.needApiKey')); return }
  if (!aiPlayerForm.apiBaseUrl) { ElMessage.warning($t('aiPlayer.needApiUrl')); return }
  if (fetchingModels.value) { ElMessage.warning($t('aiPlayer.fetchingModels')); return }

  fetchingModels.value = true
  try {
    let apiUrl = aiPlayerForm.apiBaseUrl.trim()
    if (!apiUrl.startsWith('http')) apiUrl = 'https://' + apiUrl
    if (!apiUrl.endsWith('/')) apiUrl += '/'

    const response = await axios.get(`${apiUrl}models`, {
      headers: { 'Authorization': `Bearer ${aiPlayerForm.apiKey}` }
    })

    if (response.data && Array.isArray(response.data.data)) {
      modelList.value = response.data.data
      const types = new Set()
      modelList.value.forEach(model => {
        const modelInfo = (model.name || model.id || model.model || '').toLowerCase()
        if (modelInfo.includes('qwen')) types.add('qwen')
        else if (modelInfo.includes('deepseek')) types.add('deepseek')
        else if (modelInfo.includes('glm')) types.add('glm')
        else if (modelInfo.includes('moonshot')) types.add('moonshot')
        else if (modelInfo.includes('gpt')) types.add('gpt')
        else if (modelInfo.includes('claude')) types.add('claude')
        else types.add('modelscope')
      })
      const typeMap = { qwen: 'Qwen', deepseek: 'DeepSeek', glm: 'GLM', moonshot: 'Moonshot', gpt: 'GPT', claude: 'Claude', modelscope: 'ModelScope' }
      modelTypes.value = Array.from(types).map(type => ({ value: type, label: typeMap[type] || type }))
      ElMessage.success($t('aiPlayer.modelsFetchedDetail', { types: types.size, count: modelList.value.length }))
    } else {
      ElMessage.error($t('aiPlayer.formatError'))
    }
  } catch (error) {
    console.error('Fetch models error:', error)
    ElMessage.error($t('aiPlayer.fetchFailed') + ': ' + (error.message || ''))
  } finally {
    fetchingModels.value = false
  }
}
</script>

<style scoped>
.ai-player-manager { width: min(1320px, 100%); margin: 0 auto; padding: 26px 0 82px; }
.manager-intro { display: flex; align-items: end; justify-content: space-between; gap: 32px; padding: 28px 0 38px; }
.manager-kicker, .card-kicker { display: block; color: #d9b55d; font: 700 10px/1 var(--font-heading); letter-spacing: .17em; }
.manager-intro h2 { margin: 14px 0 12px; color: #edf4f8; font-size: clamp(34px, 4vw, 50px); letter-spacing: -.045em; }
.manager-intro p { max-width: 560px; margin: 0; color: #9eafbe; font-size: 16px; line-height: 1.65; }
.create-player-button { min-height: 44px; padding: 0 18px; font-size: 11px; }
.create-player-button span { margin-right: 7px; font-size: 16px; }

.global-defaults-card, .ai-player-list-card { margin: 0 0 18px; border-color: rgba(180, 204, 222, .18) !important; background: linear-gradient(155deg, #101d2a, #0b141f) !important; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h3 { margin: 8px 0 0; color: #eff5fa; font-size: 21px; letter-spacing: -.025em; }
.card-status, .roster-count { display: inline-flex; align-items: center; gap: 7px; color: #aab9c6; font: 700 10px/1 var(--font-heading); letter-spacing: .1em; }
.card-status i { width: 6px; height: 6px; border-radius: 50%; background: #98d59e; box-shadow: 0 0 0 4px rgba(152, 213, 158, .1); }
.roster-count { padding: 7px 9px; border: 1px solid rgba(190, 209, 224, .17); border-radius: 4px; color: #d3dce5; }

.defaults-form { display: grid; grid-template-columns: minmax(220px, 1fr) minmax(280px, 1.3fr) auto; gap: 16px; align-items: end; }
.defaults-form :deep(.el-form-item) { margin: 0; }
.defaults-action { align-self: end; }
.defaults-action :deep(.el-form-item__content) { min-height: 32px; }

.player-table :deep(.el-table__inner-wrapper::before) { display: none; }
.player-table :deep(.el-table__header-wrapper th) { height: 44px; }
.player-table :deep(.el-table__body td) { height: 68px; }
.player-table :deep(.el-table__cell) { padding: 0 12px; }
.player-table :deep(.cell) { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #d7e1e9; font-size: 14px; }
.player-table :deep(.el-table__header .cell) { color: #dcb861; font: 700 10px/1 var(--font-heading); letter-spacing: .09em; }
.player-table :deep(.el-table__fixed-right) { box-shadow: -8px 0 16px rgba(3, 9, 14, .22); }
.table-avatar, .avatar-preview {
  display: inline-flex; align-items: center; justify-content: center; overflow: hidden;
  width: 42px; height: 42px; border: 1px solid rgba(217, 181, 93, .38); border-radius: 50%;
  background: #17232d; color: #e5c574; font: 700 11px/1 var(--font-heading);
}
.table-avatar img, .avatar-preview img { width: 100%; height: 100%; object-fit: cover; }
.avatar-editor { display: flex; align-items: center; gap: 16px; width: 100%; }
.avatar-preview { width: 82px; height: 82px; flex: 0 0 82px; padding: 0; cursor: pointer; }
.avatar-preview span { font-size: 18px; }
.avatar-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.avatar-actions .field-hint { width: 100%; margin-top: 0; }
.avatar-file-input { display: none; }
.availability { display: inline-flex; align-items: center; gap: 6px; color: #a8d8a2; font: 700 10px/1 var(--font-heading); letter-spacing: .06em; }
.availability i { width: 6px; height: 6px; border-radius: 50%; background: #90d69a; box-shadow: 0 0 0 3px rgba(144, 214, 154, .1); }
.availability.offline { color: #b98484; }.availability.offline i { background: #bc7878; }
.table-actions { display: flex; gap: 7px; }
.table-actions :deep(.el-button) { min-width: 51px; padding: 6px 8px; font-size: 10px; }
.model-list-info { margin-left: 10px; color: #8fd29b; font-size: 13px; }
.slider-value { color: #e4bd65; font-weight: bold; min-width: 30px; }.temp-label { color: #9eafbe; font-size: 12px; }
.tag-field, .temperature-field, .language-field { width: 100%; }
.tag-field :deep(.el-select__wrapper) { min-height: 42px; height: auto; }
.tag-field :deep(.el-tag) { border-color: rgba(217, 181, 93, .36); background: rgba(217, 181, 93, .12); color: #f0cf7b; }
.field-hint { display: block; margin-top: 7px; color: #8294a3; font-size: 12px; line-height: 1.5; }
.temperature-control { display: flex; align-items: center; gap: 12px; width: 100%; }
.temperature-control :deep(.el-slider__runway) { min-width: 180px; height: 6px; background: #344653; }
.temperature-control :deep(.el-slider__bar) { height: 6px; background: #d9b55d !important; }
.temperature-control :deep(.el-slider__button) { width: 16px; height: 16px; background: #f3dfaa; }
.dialog-footer { display: flex; justify-content: flex-end; }
.voice-switch-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; padding: 12px 14px; border: 1px solid rgba(180, 204, 222, .15); border-radius: 6px; background: rgba(5, 12, 19, .34); }
.voice-switch-row strong, .voice-switch-row span { display: block; }
.voice-switch-row strong { color: #e8eef3; font-size: 13px; }
.voice-switch-row span { margin-top: 4px; color: #8294a3; font-size: 12px; }
.voice-sliders { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.voice-slider { display: flex; align-items: center; gap: 8px; }
.voice-slider :deep(.el-slider) { flex: 1; min-width: 70px; }
.voice-slider > span { min-width: 36px; color: #e4bd65; font-size: 12px; text-align: right; }

@media (max-width: 850px) { .manager-intro { display: block; }.create-player-button { margin-top: 24px; }.defaults-form { grid-template-columns: 1fr; }.defaults-action { justify-self: start; }.voice-sliders { grid-template-columns: 1fr; gap: 4px; } }
</style>
