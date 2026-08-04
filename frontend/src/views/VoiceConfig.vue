<template>
  <div class="voice-config">
    <section class="voice-intro">
      <div>
        <span class="section-kicker">03 / VOICE LAYER</span>
        <h2>{{ $t('voiceConfig.title') }}</h2>
        <p>{{ $t('voiceConfig.subtitle') }}</p>
      </div>
      <span class="support-status" :class="{ supported: engineReady }">
        <i></i>{{ engineStatusText }}
      </span>
    </section>

    <el-card class="config-card">
      <template #header>
        <div class="config-header">
          <div>
            <span class="card-kicker">PLAYBACK</span>
            <h3>{{ $t('voiceConfig.playbackTitle') }}</h3>
          </div>
          <el-switch v-model="voiceConfig.enabled" :active-text="$t('voiceConfig.autoRead')" />
        </div>
      </template>

      <el-form :model="voiceConfig" label-position="top">
        <el-form-item :label="$t('voiceConfig.engine')">
          <el-radio-group v-model="voiceConfig.engine">
            <el-radio-button value="browser">{{ $t('voiceConfig.browserEngine') }}</el-radio-button>
            <el-radio-button value="cloud">{{ $t('voiceConfig.cloudEngine') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <div class="switch-grid">
          <div class="switch-item">
            <div><strong>{{ $t('voiceConfig.readPlayers') }}</strong><span>{{ $t('voiceConfig.readPlayersHint') }}</span></div>
            <el-switch v-model="voiceConfig.readPlayers" />
          </div>
          <div class="switch-item">
            <div><strong>{{ $t('voiceConfig.readReferee') }}</strong><span>{{ $t('voiceConfig.readRefereeHint') }}</span></div>
            <el-switch v-model="voiceConfig.readReferee" />
          </div>
        </div>

        <template v-if="voiceConfig.engine === 'browser'">
          <el-alert class="voice-note" :title="$t('voiceConfig.localNote')" type="info" :closable="false" show-icon />
          <el-form-item class="voice-language-filter" :label="$t('voiceConfig.voiceLanguage')">
            <el-select v-model="voiceLanguageFilter" :placeholder="$t('voiceConfig.voiceLanguagePlaceholder')" style="width: 100%">
              <el-option :label="$t('voiceConfig.allLanguages')" value="all" />
              <el-option v-for="option in voiceLanguageOptions" :key="option.key" :label="`${option.label} (${option.count})`" :value="option.key" />
            </el-select>
          </el-form-item>
          <div class="field-grid">
            <el-form-item :label="$t('voiceConfig.playerVoice')">
              <el-select v-model="voiceConfig.voiceURI" :placeholder="$t('voiceConfig.voicePlaceholder')" :disabled="!speechSupported || !voices.length" style="width: 100%">
                <el-option-group v-for="group in browserVoiceGroups" :key="`player-${group.key}`" :label="group.label">
                  <el-option v-for="voice in group.voices" :key="voice.voiceURI" :label="voiceLabel(voice)" :value="voice.voiceURI" />
                </el-option-group>
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('voiceConfig.refereeVoice')">
              <el-select v-model="voiceConfig.refereeVoiceURI" clearable :placeholder="$t('voiceConfig.sameAsPlayer')" :disabled="!speechSupported || !voices.length" style="width: 100%">
                <el-option-group v-for="group in browserVoiceGroups" :key="`referee-${group.key}`" :label="group.label">
                  <el-option v-for="voice in group.voices" :key="`referee-${voice.voiceURI}`" :label="voiceLabel(voice)" :value="voice.voiceURI" />
                </el-option-group>
              </el-select>
            </el-form-item>
          </div>
          <span class="field-hint">{{ voices.length ? $t('voiceConfig.voiceHint') : $t('voiceConfig.noVoices') }}</span>
        </template>

        <div class="control-grid">
          <el-form-item :label="$t('voiceConfig.speed')">
            <div class="slider-line"><el-slider v-model="voiceConfig.rate" :min="0.5" :max="2" :step="0.1" :disabled="voiceConfig.engine === 'cloud' || !speechSupported" /><span>{{ voiceConfig.rate.toFixed(1) }}x</span></div>
          </el-form-item>
          <el-form-item :label="$t('voiceConfig.pitch')">
            <div class="slider-line"><el-slider v-model="voiceConfig.pitch" :min="0.5" :max="2" :step="0.1" :disabled="voiceConfig.engine === 'cloud'" /><span>{{ voiceConfig.pitch.toFixed(1) }}</span></div>
          </el-form-item>
          <el-form-item :label="$t('voiceConfig.volume')">
            <div class="slider-line"><el-slider v-model="voiceConfig.volume" :min="0" :max="1" :step="0.1" /><span>{{ Math.round(voiceConfig.volume * 100) }}%</span></div>
          </el-form-item>
        </div>
      </el-form>
    </el-card>

    <el-card v-if="voiceConfig.engine === 'cloud'" class="cloud-card">
      <template #header>
        <div class="config-header">
          <div><span class="card-kicker">CLOUD TTS</span><h3>{{ $t('voiceConfig.cloudTitle') }}</h3></div>
          <span class="cloud-status" :class="{ configured: backendConfigured }">{{ backendStatusText }}</span>
        </div>
      </template>

      <el-alert class="voice-note" :title="$t('voiceConfig.cloudNote')" type="warning" :closable="false" show-icon />
      <el-form :model="voiceConfig.cloud" label-position="top">
        <div class="field-grid">
          <el-form-item :label="$t('voiceConfig.provider')">
            <el-select v-model="voiceConfig.cloud.provider" style="width: 100%" @change="applyProviderDefaults">
              <el-option label="OpenAI / OpenAI Compatible" value="openai" />
              <el-option label="Azure Speech" value="azure" />
              <el-option :label="$t('voiceConfig.customProvider')" value="custom" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('voiceConfig.apiBaseUrl')">
            <el-input v-model="voiceConfig.cloud.apiBaseUrl" :placeholder="endpointPlaceholder" clearable />
            <span class="field-hint">{{ endpointHint }}</span>
          </el-form-item>
        </div>

        <div class="field-grid">
          <el-form-item :label="$t('voiceConfig.apiKey')">
            <el-input v-model="voiceConfig.cloud.apiKey" type="password" show-password :placeholder="$t('voiceConfig.apiKeyPlaceholder')" autocomplete="off" />
            <el-checkbox v-model="voiceConfig.cloud.saveApiKey">{{ $t('voiceConfig.rememberApiKey') }}</el-checkbox>
          </el-form-item>
          <el-form-item :label="$t('voiceConfig.model')">
            <el-input v-model="voiceConfig.cloud.model" :disabled="isNativeAzure" placeholder="gpt-4o-mini-tts" />
          </el-form-item>
        </div>

        <div class="field-grid">
          <el-form-item :label="$t('voiceConfig.playerVoice')">
            <el-select v-model="voiceConfig.cloud.playerVoice" filterable allow-create default-first-option style="width: 100%">
              <el-option v-for="voice in cloudVoiceOptions" :key="`player-${voice}`" :label="voice" :value="voice" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('voiceConfig.refereeVoice')">
            <el-select v-model="voiceConfig.cloud.refereeVoice" filterable allow-create default-first-option style="width: 100%">
              <el-option v-for="voice in cloudVoiceOptions" :key="`referee-${voice}`" :label="voice" :value="voice" />
            </el-select>
          </el-form-item>
        </div>

        <div class="cloud-control-grid">
          <el-form-item :label="$t('voiceConfig.cloudSpeed')">
            <div class="slider-line cloud-slider"><el-slider v-model="voiceConfig.cloud.speed" :min="0.25" :max="4" :step="0.1" /><span>{{ voiceConfig.cloud.speed.toFixed(1) }}x</span></div>
          </el-form-item>
          <el-form-item :label="$t('voiceConfig.audioFormat')">
            <el-select v-model="voiceConfig.cloud.responseFormat" style="width: 100%">
              <el-option v-for="format in audioFormats" :key="format.value" :label="format.label" :value="format.value" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('voiceConfig.timeout')">
            <el-input-number v-model="voiceConfig.cloud.timeout" :min="3000" :max="120000" :step="1000" />
          </el-form-item>
        </div>
        <el-checkbox v-model="voiceConfig.cloud.fallbackToBrowser">{{ $t('voiceConfig.fallbackToBrowser') }}</el-checkbox>
      </el-form>
      <div class="cache-panel">
        <div class="cache-copy">
          <strong>{{ $locale === 'zh-CN' ? '云端语音缓存' : 'Cloud voice cache' }}</strong>
          <span v-if="cacheStats">{{ cacheStats.cacheCount }} 条 · {{ formatBytes(cacheStats.totalBytes) }}</span>
          <span v-else>{{ $locale === 'zh-CN' ? '缓存服务未启用' : 'Cache unavailable' }}</span>
        </div>
        <div class="cache-actions">
          <el-button size="small" :loading="cacheLoading" @click="loadCacheStats">{{ $locale === 'zh-CN' ? '刷新' : 'Refresh' }}</el-button>
          <el-button size="small" :loading="cacheClearing" @click="clearOldCache">{{ $locale === 'zh-CN' ? '清理30天前' : 'Purge 30d old' }}</el-button>
          <el-button size="small" type="danger" :loading="cacheClearing" @click="clearAllCache">{{ $locale === 'zh-CN' ? '清空缓存' : 'Clear cache' }}</el-button>
        </div>
      </div>
    </el-card>

    <section class="test-panel">
      <div class="test-copy">
        <strong>{{ $t('voiceConfig.testTitle') }}</strong>
        <el-input v-model="testText" type="textarea" :rows="2" :maxlength="300" show-word-limit />
      </div>
      <div class="test-actions">
        <el-radio-group v-model="testSpeaker" size="small">
          <el-radio-button value="referee">{{ $t('voiceConfig.referee') }}</el-radio-button>
          <el-radio-button value="player">{{ $t('voiceConfig.player') }}</el-radio-button>
        </el-radio-group>
        <el-button type="primary" :loading="testing" :disabled="!canTest" @click="testVoice">{{ $t('voiceConfig.testVoice') }}</el-button>
        <el-button @click="saveConfig">{{ $t('voiceConfig.saveConfig') }}</el-button>
        <el-button text @click="resetConfig">{{ $t('voiceConfig.reset') }}</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { DEFAULT_VOICE_CONFIG, getVoiceLanguageKey, getVoiceLanguageLabel, groupVoicesByLanguage, isSpeechSynthesisSupported, loadVoiceConfig, saveVoiceConfig as persistVoiceConfig, speakText, stopSpeaking } from '../composables/useSpeechSynthesis.js'

const { proxy } = getCurrentInstance()
const $t = proxy.$t
const $locale = proxy.$locale
const speechSupported = ref(isSpeechSynthesisSupported())
const voices = ref([])
const voiceLanguageFilter = ref('all')
const testing = ref(false)
const backendStatus = ref({ openaiConfigured: false, azureConfigured: false })
const proxyReachable = ref(false)
const cacheStats = ref(null)
const cacheLoading = ref(false)
const cacheClearing = ref(false)
const voiceConfig = reactive(loadVoiceConfig())
const testText = ref($t('voiceConfig.testText'))
const testSpeaker = ref('referee')

const openAiVoices = ['alloy', 'ash', 'ballad', 'coral', 'echo', 'fable', 'nova', 'onyx', 'sage', 'shimmer', 'verse']
const azureVoices = ['zh-CN-XiaoxiaoNeural', 'zh-CN-YunxiNeural', 'zh-CN-YunjianNeural', 'zh-CN-XiaoyiNeural', 'en-US-JennyNeural', 'en-US-GuyNeural']
const cloudVoiceOptions = computed(() => voiceConfig.cloud.provider === 'azure' ? azureVoices : openAiVoices)
const audioFormats = computed(() => voiceConfig.cloud.provider === 'azure'
  ? [{ label: 'MP3', value: 'mp3' }, { label: 'WAV', value: 'wav' }, { label: 'OGG', value: 'ogg' }]
  : [{ label: 'MP3', value: 'mp3' }, { label: 'WAV', value: 'wav' }, { label: 'OPUS', value: 'opus' }])
const isNativeAzure = computed(() => voiceConfig.cloud.provider === 'azure' && voiceConfig.cloud.apiBaseUrl.includes('tts.speech.microsoft.com'))
const backendConfigured = computed(() => voiceConfig.cloud.provider === 'azure' ? backendStatus.value.azureConfigured : backendStatus.value.openaiConfigured)
const engineReady = computed(() => voiceConfig.engine === 'cloud' ? proxyReachable.value : speechSupported.value)
const canTest = computed(() => Boolean(testText.value.trim()) && (voiceConfig.engine === 'cloud' || speechSupported.value))
const engineStatusText = computed(() => voiceConfig.engine === 'cloud'
  ? (proxyReachable.value ? $t('voiceConfig.cloudReady') : $t('voiceConfig.cloudUnavailable'))
  : (speechSupported.value ? $t('voiceConfig.supported') : $t('voiceConfig.unsupported')))
const backendStatusText = computed(() => backendConfigured.value ? $t('voiceConfig.serverKeyReady') : $t('voiceConfig.serverKeyMissing'))
const endpointPlaceholder = computed(() => voiceConfig.cloud.provider === 'azure'
  ? 'https://eastasia.tts.speech.microsoft.com/cognitiveservices/v1'
  : (voiceConfig.cloud.provider === 'custom' ? 'https://example.com/v1/audio/speech' : 'https://api.openai.com/v1/audio/speech'))
const endpointHint = computed(() => voiceConfig.cloud.provider === 'azure' ? $t('voiceConfig.azureEndpointHint') : $t('voiceConfig.openAiEndpointHint'))
const currentLocale = computed(() => (typeof $locale === 'string' ? $locale : $locale?.value) || 'zh-CN')
const voiceLanguageOptions = computed(() => {
  const counts = new Map()
  voices.value.forEach(voice => {
    const key = getVoiceLanguageKey(voice)
    counts.set(key, (counts.get(key) || 0) + 1)
  })
  return [...counts.entries()]
    .sort(([first], [second]) => first.localeCompare(second))
    .map(([key, count]) => ({ key, label: getVoiceLanguageLabel(key, currentLocale.value), count }))
})
const browserVoiceGroups = computed(() => groupVoicesByLanguage(voices.value, voiceLanguageFilter.value, currentLocale.value))

const loadVoices = () => {
  if (!speechSupported.value) return
  voices.value = window.speechSynthesis.getVoices()
  if (voiceLanguageFilter.value !== 'all' && !voiceLanguageOptions.value.some(option => option.key === voiceLanguageFilter.value)) voiceLanguageFilter.value = 'all'
  if (!voiceConfig.voiceURI && voices.value.length) {
    const preferred = voices.value.find(voice => voice.lang?.toLowerCase().startsWith('zh')) || voices.value[0]
    voiceConfig.voiceURI = preferred.voiceURI
  }
}
const formatBytes = bytes => {
  const value = Number(bytes) || 0
  if (value >= 1024 * 1024 * 1024) return (value / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
  if (value >= 1024 * 1024) return (value / (1024 * 1024)).toFixed(1) + ' MB'
  if (value >= 1024) return (value / 1024).toFixed(1) + ' KB'
  return value + ' B'
}
const loadCacheStats = async () => {
  cacheLoading.value = true
  try {
    const response = await axios.get('/voice/cache/stats')
    if (response.data?.code === 200) cacheStats.value = response.data.data
  } catch {
    cacheStats.value = null
  } finally {
    cacheLoading.value = false
  }
}
const clearAllCache = async () => {
  cacheClearing.value = true
  try {
    const response = await axios.delete('/voice/cache')
    const cleared = response.data?.data?.cleared ?? 0
    ElMessage.success($locale === 'zh-CN' ? '已清空 ' + cleared + ' 条语音缓存' : 'Cleared ' + cleared + ' cached voices')
    await loadCacheStats()
  } catch {
    ElMessage.error($locale === 'zh-CN' ? '清空缓存失败' : 'Failed to clear cache')
  } finally {
    cacheClearing.value = false
  }
}
const clearOldCache = async () => {
  cacheClearing.value = true
  try {
    const response = await axios.delete('/voice/cache/old', { params: { days: 30 } })
    const cleared = response.data?.data?.cleared ?? 0
    ElMessage.success($locale === 'zh-CN' ? '已清理 ' + cleared + ' 条过期缓存' : 'Cleared ' + cleared + ' expired voices')
    await loadCacheStats()
  } catch {
    ElMessage.error($locale === 'zh-CN' ? '清理过期缓存失败' : 'Failed to purge expired cache')
  } finally {
    cacheClearing.value = false
  }
}
const loadCloudStatus = async () => {
  try {
    const response = await axios.get('/voice/status')
    if (response.data?.code === 200) {
      proxyReachable.value = true
      Object.assign(backendStatus.value, response.data.data)
    }
  } catch {
    proxyReachable.value = false
    backendStatus.value = { openaiConfigured: false, azureConfigured: false }
  }
}
const voiceLabel = voice => `${voice.name} (${voice.lang})`
const applyProviderDefaults = provider => {
  if (!audioFormats.value.some(format => format.value === voiceConfig.cloud.responseFormat)) voiceConfig.cloud.responseFormat = 'mp3'
  if (provider === 'azure') {
    voiceConfig.cloud.playerVoice = 'zh-CN-YunxiNeural'
    voiceConfig.cloud.refereeVoice = 'zh-CN-XiaoxiaoNeural'
  } else if (!openAiVoices.includes(voiceConfig.cloud.playerVoice)) {
    voiceConfig.cloud.playerVoice = 'alloy'
    voiceConfig.cloud.refereeVoice = 'onyx'
  }
}
const saveConfig = () => {
  persistVoiceConfig(voiceConfig)
  ElMessage.success($t('voiceConfig.configSaved'))
}
const testVoice = async () => {
  stopSpeaking()
  persistVoiceConfig(voiceConfig)
  testing.value = true
  try {
    const played = await speakText(testText.value, {
      force: true,
      interrupt: true,
      speaker: testSpeaker.value,
      cloud: { ...voiceConfig.cloud },
      engine: voiceConfig.engine,
      voiceURI: voiceConfig.voiceURI,
      refereeVoiceURI: voiceConfig.refereeVoiceURI,
      rate: voiceConfig.rate,
      pitch: voiceConfig.pitch,
      volume: voiceConfig.volume,
      onFallback: error => ElMessage.warning(`${$t('voiceConfig.fallbackUsed')}: ${error.message || $t('voiceConfig.playFailed')}`)
    })
    if (!played) throw new Error($t('voiceConfig.playFailed'))
  } catch (error) {
    ElMessage.error(`${$t('voiceConfig.testFailed')}: ${error.message || $t('voiceConfig.playFailed')}`)
  } finally {
    testing.value = false
  }
}
const resetConfig = () => {
  stopSpeaking()
  Object.assign(voiceConfig, JSON.parse(JSON.stringify(DEFAULT_VOICE_CONFIG)))
  loadVoices()
  persistVoiceConfig(voiceConfig)
}
onMounted(() => {
  loadVoices()
  loadCloudStatus()
  loadCacheStats()
  window.speechSynthesis?.addEventListener('voiceschanged', loadVoices)
})
onUnmounted(() => {
  window.speechSynthesis?.removeEventListener('voiceschanged', loadVoices)
  stopSpeaking()
})
</script>

<style scoped>
.voice-config { width: min(980px, 100%); margin: 0 auto; padding: 26px 0 82px; }
.voice-intro { display: flex; justify-content: space-between; align-items: end; gap: 24px; padding: 26px 0 36px; }
.section-kicker, .card-kicker { color: #d9b55d; font: 700 10px/1 var(--font-heading); letter-spacing: .17em; }
.voice-intro h2 { margin: 14px 0 12px; color: #edf4f8; font-size: clamp(32px, 4vw, 48px); letter-spacing: 0; }
.voice-intro p { max-width: 660px; margin: 0; color: #9eafbe; font-size: 16px; line-height: 1.65; }
.support-status, .cloud-status { display: inline-flex; align-items: center; gap: 8px; color: #c38b8b; font: 700 11px/1 var(--font-heading); white-space: nowrap; }
.support-status i { width: 7px; height: 7px; border-radius: 50%; background: #c38b8b; }
.support-status.supported, .cloud-status.configured { color: #9bd09f; }
.support-status.supported i { background: #9bd09f; }
.config-card, .cloud-card { border-color: rgba(180, 204, 222, .18) !important; background: linear-gradient(155deg, #101d2a, #0b141f) !important; }
.voice-language-filter { margin-bottom: 18px; }
.cloud-card { margin-top: 18px; }
.cache-panel { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: 16px; padding: 14px 16px; border: 1px solid rgba(180, 204, 222, .18); border-radius: 10px; background: rgba(9, 18, 27, .55); }
.cache-copy { display: flex; flex-direction: column; gap: 4px; }
.cache-copy strong { color: #edf4f8; font: 700 13px/1 var(--font-heading); }
.cache-copy span { color: #9eafbe; font-size: 12px; }
.cache-actions { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.config-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.config-header h3 { margin: 8px 0 0; color: #eff5fa; font-size: 21px; letter-spacing: 0; }
.voice-note { margin: 18px 0 22px; }
.field-grid, .switch-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; }
.switch-grid { margin: 8px 0 20px; }
.switch-item { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 14px 16px; border: 1px solid rgba(180, 204, 222, .15); border-radius: 6px; background: rgba(5, 12, 19, .34); }
.switch-item strong, .switch-item span { display: block; }
.switch-item strong { color: #e5edf3; font-size: 13px; }
.switch-item span, .field-hint { margin-top: 5px; color: #8294a3; font-size: 12px; line-height: 1.5; }
.control-grid, .cloud-control-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 20px; margin-top: 10px; }
.slider-line { display: flex; align-items: center; gap: 12px; width: 100%; min-width: 0; }
.slider-line :deep(.el-slider) { flex: 1; min-width: 120px; }
.slider-line :deep(.el-slider__runway) { height: 7px; background: #344653; border-radius: 4px; }
.slider-line :deep(.el-slider__bar) { height: 7px; background: #d9b55d !important; border-radius: 4px; }
.slider-line :deep(.el-slider__button) { width: 17px; height: 17px; border: 2px solid #d9b55d !important; background: #f2e1b4; }
.slider-line :deep(.el-slider.is-disabled .el-slider__runway) { background: #283640; }
.slider-line :deep(.el-slider.is-disabled .el-slider__bar) { background: #687782 !important; }
.slider-line > span { min-width: 46px; color: #e4bd65; font-weight: 700; text-align: right; }
.cloud-slider { min-width: 220px; }
.test-panel { display: flex; justify-content: space-between; align-items: end; gap: 22px; margin-top: 18px; padding: 20px; border-top: 1px solid rgba(180, 204, 222, .18); border-bottom: 1px solid rgba(180, 204, 222, .18); background: rgba(8, 17, 26, .68); }
.test-copy { flex: 1; min-width: 0; }
.test-copy strong { display: block; margin-bottom: 10px; color: #e9f0f5; }
.test-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
@media (max-width: 760px) {
  .voice-intro { display: block; }
  .support-status { margin-top: 18px; }
  .field-grid, .switch-grid, .control-grid, .cloud-control-grid { grid-template-columns: 1fr; gap: 4px; }
  .test-panel { display: block; }
  .test-actions { margin-top: 14px; }
}
</style>
