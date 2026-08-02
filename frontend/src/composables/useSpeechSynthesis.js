export const DEFAULT_VOICE_CONFIG = {
  enabled: true,
  voiceURI: '',
  rate: 1,
  pitch: 1,
  volume: 1
}

export function isSpeechSynthesisSupported() {
  return typeof window !== 'undefined' &&
    'speechSynthesis' in window &&
    'SpeechSynthesisUtterance' in window
}

export function loadVoiceConfig() {
  if (typeof localStorage === 'undefined') return { ...DEFAULT_VOICE_CONFIG }

  try {
    const saved = JSON.parse(localStorage.getItem('voiceConfig') || '{}')
    return {
      ...DEFAULT_VOICE_CONFIG,
      enabled: saved.enabled ?? DEFAULT_VOICE_CONFIG.enabled,
      voiceURI: saved.voiceURI || '',
      rate: Number(saved.rate ?? saved.speed ?? DEFAULT_VOICE_CONFIG.rate),
      pitch: Number(saved.pitch ?? DEFAULT_VOICE_CONFIG.pitch),
      volume: Number(saved.volume ?? DEFAULT_VOICE_CONFIG.volume)
    }
  } catch {
    return { ...DEFAULT_VOICE_CONFIG }
  }
}

export function saveVoiceConfig(config) {
  localStorage.setItem('voiceConfig', JSON.stringify({
    enabled: Boolean(config.enabled),
    voiceURI: config.voiceURI || '',
    rate: Number(config.rate),
    pitch: Number(config.pitch),
    volume: Number(config.volume)
  }))
}

export function stopSpeaking() {
  if (isSpeechSynthesisSupported()) window.speechSynthesis.cancel()
}

export function pauseSpeaking() {
  if (isSpeechSynthesisSupported() && window.speechSynthesis.speaking && !window.speechSynthesis.paused) {
    window.speechSynthesis.pause()
  }
}

export function resumeSpeaking() {
  if (isSpeechSynthesisSupported() && window.speechSynthesis.paused) {
    window.speechSynthesis.resume()
  }
}

export function speakText(text, overrides = {}) {
  if (!text || !isSpeechSynthesisSupported()) return false

  const config = { ...loadVoiceConfig(), ...overrides }
  if (!config.enabled && !overrides.force) return false

  const utterance = new SpeechSynthesisUtterance(text)
  const voices = window.speechSynthesis.getVoices()
  const requestedLang = String(config.lang || '').toLowerCase()
  const selectedVoice = voices.find(voice => voice.voiceURI === config.voiceURI)
    || (requestedLang && voices.find(voice => voice.lang?.toLowerCase().startsWith(requestedLang.split('-')[0])))
    || voices.find(voice => voice.lang?.toLowerCase().startsWith('zh'))
    || voices[0]

  if (selectedVoice) utterance.voice = selectedVoice
  utterance.lang = config.lang || selectedVoice?.lang || 'zh-CN'
  utterance.rate = Math.min(2, Math.max(0.5, Number(config.rate) || 1))
  utterance.pitch = Math.min(2, Math.max(0.5, Number(config.pitch) || 1))
  utterance.volume = Math.min(1, Math.max(0, Number(config.volume) || 1))

  window.speechSynthesis.cancel()
  window.speechSynthesis.speak(utterance)
  return true
}
