<template>
  <div class="voice-config">
    <section class="voice-intro">
      <div>
        <span class="section-kicker">03 / VOICE LAYER</span>
        <h2>{{ $t('voiceConfig.title') }}</h2>
        <p>{{ $t('voiceConfig.subtitle') }}</p>
      </div>
      <span class="support-status" :class="{ supported: speechSupported }">
        <i></i>{{ speechSupported ? $t('voiceConfig.supported') : $t('voiceConfig.unsupported') }}
      </span>
    </section>

    <el-card class="config-card">
      <template #header>
        <div class="config-header">
          <div>
            <span class="card-kicker">LOCAL SPEECH</span>
            <h3>{{ $t('voiceConfig.localTitle') }}</h3>
          </div>
          <el-switch v-model="voiceConfig.enabled" :active-text="$t('voiceConfig.autoRead')" />
        </div>
      </template>

      <el-alert class="voice-note" :title="$t('voiceConfig.localNote')" type="info" :closable="false" show-icon />

      <el-form :model="voiceConfig" ref="voiceFormRef" label-position="top">
        <el-form-item :label="$t('voiceConfig.voice')">
          <el-select v-model="voiceConfig.voiceURI" :placeholder="$t('voiceConfig.voicePlaceholder')" :disabled="!speechSupported || !voices.length" style="width: 100%">
            <el-option v-for="voice in voices" :key="voice.voiceURI" :label="voiceLabel(voice)" :value="voice.voiceURI" />
          </el-select>
          <span class="field-hint">{{ voices.length ? $t('voiceConfig.voiceHint') : $t('voiceConfig.noVoices') }}</span>
        </el-form-item>

        <div class="control-grid">
          <el-form-item :label="$t('voiceConfig.speed')">
            <div class="slider-line"><el-slider v-model="voiceConfig.rate" :min="0.5" :max="2" :step="0.1" :disabled="!speechSupported" /><span>{{ voiceConfig.rate.toFixed(1) }}×</span></div>
          </el-form-item>
          <el-form-item :label="$t('voiceConfig.pitch')">
            <div class="slider-line"><el-slider v-model="voiceConfig.pitch" :min="0.5" :max="2" :step="0.1" :disabled="!speechSupported" /><span>{{ voiceConfig.pitch.toFixed(1) }}</span></div>
          </el-form-item>
          <el-form-item :label="$t('voiceConfig.volume')">
            <div class="slider-line"><el-slider v-model="voiceConfig.volume" :min="0" :max="1" :step="0.1" :disabled="!speechSupported" /><span>{{ Math.round(voiceConfig.volume * 100) }}%</span></div>
          </el-form-item>
        </div>

        <el-form-item>
          <el-button type="primary" :disabled="!speechSupported" @click="testVoice">{{ $t('voiceConfig.testVoice') }}</el-button>
          <el-button @click="saveConfig">{{ $t('voiceConfig.saveConfig') }}</el-button>
          <el-button text @click="resetConfig">{{ $t('voiceConfig.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="cloud-card">
      <template #header>
        <div class="config-header"><div><span class="card-kicker">CLOUD ENGINES</span><h3>{{ $t('voiceConfig.cloudTitle') }}</h3></div><span class="coming-soon">{{ $t('voiceConfig.comingSoon') }}</span></div>
      </template>
      <p>{{ $t('voiceConfig.cloudNote') }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { ElMessage } from 'element-plus'
import { DEFAULT_VOICE_CONFIG, isSpeechSynthesisSupported, loadVoiceConfig, saveVoiceConfig as persistVoiceConfig, speakText, stopSpeaking } from '../composables/useSpeechSynthesis.js'

const { proxy } = getCurrentInstance()
const $t = proxy.$t
const speechSupported = ref(isSpeechSynthesisSupported())
const voices = ref([])
const voiceFormRef = ref(null)
const voiceConfig = reactive(loadVoiceConfig())

const loadVoices = () => {
  if (!speechSupported.value) return
  voices.value = window.speechSynthesis.getVoices()
  if (!voiceConfig.voiceURI && voices.value.length) {
    const preferred = voices.value.find(voice => voice.lang?.toLowerCase().startsWith('zh')) || voices.value[0]
    voiceConfig.voiceURI = preferred.voiceURI
  }
}
const voiceLabel = voice => `${voice.name} (${voice.lang})`
const saveConfig = () => { persistVoiceConfig(voiceConfig); ElMessage.success($t('voiceConfig.configSaved')) }
const testVoice = () => { if (!speechSupported.value) return; persistVoiceConfig(voiceConfig); speakText($t('voiceConfig.testText'), { force: true }) }
const resetConfig = () => { Object.assign(voiceConfig, DEFAULT_VOICE_CONFIG); loadVoices(); stopSpeaking() }
onMounted(() => { loadVoices(); window.speechSynthesis?.addEventListener('voiceschanged', loadVoices) })
onUnmounted(() => { window.speechSynthesis?.removeEventListener('voiceschanged', loadVoices); stopSpeaking() })
</script>

<style scoped>
.voice-config { width: min(920px, 100%); margin: 0 auto; padding: 26px 0 82px; }
.voice-intro { display: flex; justify-content: space-between; align-items: end; gap: 24px; padding: 26px 0 36px; }
.section-kicker, .card-kicker { color: #d9b55d; font: 700 10px/1 var(--font-heading); letter-spacing: .17em; }
.voice-intro h2 { margin: 14px 0 12px; color: #edf4f8; font-size: clamp(32px, 4vw, 48px); letter-spacing: -.04em; }
.voice-intro p { max-width: 620px; margin: 0; color: #9eafbe; font-size: 16px; line-height: 1.65; }
.support-status { display: inline-flex; align-items: center; gap: 8px; color: #c38b8b; font: 700 11px/1 var(--font-heading); letter-spacing: .08em; white-space: nowrap; }
.support-status i { width: 7px; height: 7px; border-radius: 50%; background: #c38b8b; }
.support-status.supported { color: #9bd09f; }.support-status.supported i { background: #9bd09f; }
.config-card, .cloud-card { border-color: rgba(180, 204, 222, .18) !important; background: linear-gradient(155deg, #101d2a, #0b141f) !important; }
.cloud-card { margin-top: 18px; }
.config-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.config-header h3 { margin: 8px 0 0; color: #eff5fa; font-size: 21px; }
.coming-soon { color: #d9b55d; font: 700 10px/1 var(--font-heading); letter-spacing: .12em; }
.voice-note { margin-bottom: 22px; }
.field-hint, .cloud-card p { display: block; margin: 8px 0 0; color: #8294a3; font-size: 12px; line-height: 1.6; }
.control-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.slider-line { display: flex; align-items: center; gap: 12px; }
.slider-line :deep(.el-slider) { flex: 1; }.slider-line > span { min-width: 46px; color: #e4bd65; font-weight: 700; text-align: right; }
@media (max-width: 700px) { .voice-intro { display: block; }.support-status { margin-top: 18px; }.control-grid { grid-template-columns: 1fr; gap: 0; } }
</style>
