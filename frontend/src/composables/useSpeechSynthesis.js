import axios from 'axios'

export const DEFAULT_VOICE_CONFIG = {
  enabled: true,
  engine: 'browser',
  readPlayers: true,
  readReferee: true,
  voiceURI: '',
  refereeVoiceURI: '',
  rate: 1,
  pitch: 1,
  volume: 1,
  cloud: {
    provider: 'openai',
    apiBaseUrl: '',
    apiKey: '',
    saveApiKey: false,
    model: 'gpt-4o-mini-tts',
    playerVoice: 'alloy',
    refereeVoice: 'onyx',
    responseFormat: 'mp3',
    speed: 1,
    timeout: 30000,
    fallbackToBrowser: true
  }
}

const speechQueue = []
let processingQueue = false
let playbackPaused = false
let activeCancel = null
let activeAudio = null
let activeObjectUrl = ''
let currentJob = null

const cloneDefaults = () => JSON.parse(JSON.stringify(DEFAULT_VOICE_CONFIG))
const clamp = (value, min, max, fallback) => {
  const number = Number(value)
  return Number.isFinite(number) ? Math.min(max, Math.max(min, number)) : fallback
}

export function isSpeechSynthesisSupported() {
  return typeof window !== 'undefined' &&
    'speechSynthesis' in window &&
    'SpeechSynthesisUtterance' in window
}

const normaliseVoiceLanguage = value => {
  const raw = typeof value === 'string' ? value : value?.lang
  return String(raw || '').trim().replace(/_/g, '-').toLowerCase() || 'und'
}

export function getVoiceLanguageKey(value) {
  return normaliseVoiceLanguage(value).split('-')[0]
}

export function getVoiceLanguageLabel(language, locale = 'zh-CN') {
  const key = String(language || 'und').toLowerCase()
  if (key === 'und') return locale === 'en-US' ? 'Other languages' : '其他语言'
  try {
    return new Intl.DisplayNames([locale === 'en-US' ? 'en' : 'zh-CN'], { type: 'language' }).of(key) || key
  } catch {
    return key
  }
}

export function groupVoicesByLanguage(voices, selectedLanguage = 'all', locale = 'zh-CN') {
  const groups = new Map()
  const filtered = (voices || []).filter(voice => selectedLanguage === 'all' || getVoiceLanguageKey(voice) === selectedLanguage)
  filtered.forEach(voice => {
    const localeCode = normaliseVoiceLanguage(voice)
    if (!groups.has(localeCode)) groups.set(localeCode, [])
    groups.get(localeCode).push(voice)
  })
  return [...groups.entries()]
    .sort(([first], [second]) => first.localeCompare(second))
    .map(([localeCode, groupVoices]) => ({
      key: localeCode,
      label: `${getVoiceLanguageLabel(localeCode, locale)} (${localeCode})`,
      voices: groupVoices.slice().sort((first, second) => String(first.name || '').localeCompare(String(second.name || '')))
    }))
}

export function loadVoiceConfig() {
  if (typeof localStorage === 'undefined') return cloneDefaults()

  try {
    const saved = JSON.parse(localStorage.getItem('voiceConfig') || '{}')
    const sessionApiKey = typeof sessionStorage === 'undefined' ? '' : sessionStorage.getItem('voiceApiKey') || ''
    const defaults = cloneDefaults()
    return {
      ...defaults,
      ...saved,
      enabled: saved.enabled ?? defaults.enabled,
      engine: ['browser', 'cloud'].includes(saved.engine) ? saved.engine : defaults.engine,
      readPlayers: saved.readPlayers ?? defaults.readPlayers,
      readReferee: saved.readReferee ?? defaults.readReferee,
      voiceURI: saved.voiceURI || '',
      refereeVoiceURI: saved.refereeVoiceURI || '',
      rate: clamp(saved.rate ?? saved.speed, 0.5, 2, defaults.rate),
      pitch: clamp(saved.pitch, 0.5, 2, defaults.pitch),
      volume: clamp(saved.volume, 0, 1, defaults.volume),
      cloud: {
        ...defaults.cloud,
        ...(saved.cloud || {}),
        apiKey: sessionApiKey || saved.cloud?.apiKey || '',
        saveApiKey: Boolean(saved.cloud?.saveApiKey),
        speed: clamp(saved.cloud?.speed, 0.25, 4, defaults.cloud.speed),
        timeout: clamp(saved.cloud?.timeout, 3000, 120000, defaults.cloud.timeout)
      }
    }
  } catch {
    return cloneDefaults()
  }
}

export function saveVoiceConfig(config) {
  if (typeof localStorage === 'undefined') return
  const cloud = { ...DEFAULT_VOICE_CONFIG.cloud, ...(config.cloud || {}) }
  if (typeof sessionStorage !== 'undefined') {
    if (cloud.apiKey) sessionStorage.setItem('voiceApiKey', String(cloud.apiKey).trim())
    else sessionStorage.removeItem('voiceApiKey')
  }
  const saved = {
    enabled: Boolean(config.enabled),
    engine: config.engine === 'cloud' ? 'cloud' : 'browser',
    readPlayers: config.readPlayers !== false,
    readReferee: config.readReferee !== false,
    voiceURI: config.voiceURI || '',
    refereeVoiceURI: config.refereeVoiceURI || '',
    rate: clamp(config.rate, 0.5, 2, 1),
    pitch: clamp(config.pitch, 0.5, 2, 1),
    volume: clamp(config.volume, 0, 1, 1),
    cloud: {
      provider: ['openai', 'azure', 'custom'].includes(cloud.provider) ? cloud.provider : 'openai',
      apiBaseUrl: String(cloud.apiBaseUrl || '').trim(),
      apiKey: cloud.saveApiKey ? String(cloud.apiKey || '').trim() : '',
      saveApiKey: Boolean(cloud.saveApiKey),
      model: String(cloud.model || 'gpt-4o-mini-tts').trim(),
      playerVoice: String(cloud.playerVoice || 'alloy').trim(),
      refereeVoice: String(cloud.refereeVoice || 'onyx').trim(),
      responseFormat: ['mp3', 'wav', 'ogg', 'opus'].includes(cloud.responseFormat) ? cloud.responseFormat : 'mp3',
      speed: clamp(cloud.speed, 0.25, 4, 1),
      timeout: clamp(cloud.timeout, 3000, 120000, 30000),
      fallbackToBrowser: cloud.fallbackToBrowser !== false
    }
  }
  localStorage.setItem('voiceConfig', JSON.stringify(saved))
}

const resolveConfig = overrides => {
  const loaded = loadVoiceConfig()
  const config = {
    ...loaded,
    ...overrides,
    cloud: { ...loaded.cloud, ...(overrides.cloud || {}) }
  }
  const speaker = overrides.speaker || 'player'
  if (!config.enabled && !overrides.force) return null
  if (!overrides.force && speaker === 'referee' && !config.readReferee) return null
  if (!overrides.force && speaker !== 'referee' && !config.readPlayers) return null
  return { ...config, speaker }
}

const selectedBrowserVoice = config => {
  const voices = window.speechSynthesis.getVoices()
  const voiceURI = config.speaker === 'referee'
    ? (config.refereeVoiceURI || config.voiceURI)
    : config.voiceURI
  const requestedLang = String(config.lang || '').toLowerCase()
  return voices.find(voice => voice.voiceURI === voiceURI)
    || (requestedLang && voices.find(voice => voice.lang?.toLowerCase().startsWith(requestedLang.split('-')[0])))
    || voices.find(voice => voice.lang?.toLowerCase().startsWith('zh'))
    || voices[0]
}

const waitUntilResumed = async job => {
  while (playbackPaused && !job.cancelled) await new Promise(resolve => setTimeout(resolve, 80))
  return !job.cancelled
}

const playWithBrowser = async (job, config) => {
  if (!isSpeechSynthesisSupported()) throw new Error('当前浏览器不支持系统语音')
  if (!await waitUntilResumed(job)) return false

  return new Promise(resolve => {
    const utterance = new SpeechSynthesisUtterance(job.text)
    const voice = selectedBrowserVoice(config)
    if (voice) utterance.voice = voice
    utterance.lang = config.lang || voice?.lang || 'zh-CN'
    utterance.rate = clamp(config.rate, 0.5, 2, 1)
    utterance.pitch = clamp(config.pitch, 0.5, 2, 1)
    utterance.volume = clamp(config.volume, 0, 1, 1)

    let settled = false
    const finish = success => {
      if (settled) return
      settled = true
      activeCancel = null
      resolve(success)
    }
    utterance.onend = () => finish(true)
    utterance.onerror = () => finish(false)
    activeCancel = () => {
      window.speechSynthesis.cancel()
      finish(false)
    }
    window.speechSynthesis.speak(utterance)
  })
}

const cleanupAudio = () => {
  if (activeAudio) {
    activeAudio.pause()
    activeAudio.src = ''
  }
  activeAudio = null
  if (activeObjectUrl) URL.revokeObjectURL(activeObjectUrl)
  activeObjectUrl = ''
}

const playWithCloud = async (job, config) => {
  if (!await waitUntilResumed(job)) return false
  const cloud = config.cloud
  const voice = config.speaker === 'referee' ? cloud.refereeVoice : cloud.playerVoice
  const headers = {}
  if (cloud.apiKey) headers['X-Voice-Api-Key'] = cloud.apiKey
  const controller = new AbortController()
  const cancelRequest = () => controller.abort()
  activeCancel = cancelRequest
  let response
  try {
    response = await axios.post('/voice/synthesize', {
      text: job.text,
      provider: cloud.provider,
      apiBaseUrl: cloud.apiBaseUrl,
      model: cloud.model,
      voice,
      language: config.lang || 'zh-CN',
      responseFormat: cloud.responseFormat,
      speed: cloud.speed,
      timeout: cloud.timeout
    }, {
      headers,
      responseType: 'blob',
      timeout: cloud.timeout + 3000,
      signal: controller.signal
    })
  } finally {
    if (activeCancel === cancelRequest) activeCancel = null
  }

  if (job.cancelled) return false

  const blob = response.data instanceof Blob
    ? response.data
    : new Blob([response.data], { type: response.headers['content-type'] || 'audio/mpeg' })
  if (!blob.size) throw new Error('云端语音没有返回音频')
  activeObjectUrl = URL.createObjectURL(blob)
  activeAudio = new Audio(activeObjectUrl)
  activeAudio.volume = clamp(config.volume, 0, 1, 1)

  return new Promise((resolve, reject) => {
    let settled = false
    const finish = (success, error) => {
      if (settled) return
      settled = true
      activeCancel = null
      cleanupAudio()
      if (error) reject(error)
      else resolve(success)
    }
    activeAudio.onended = () => finish(true)
    activeAudio.onerror = () => finish(false, new Error('云端音频无法播放'))
    activeCancel = () => finish(false)
    activeAudio.play().catch(error => finish(false, error))
  })
}

const playJob = async job => {
  const { config } = job
  if (config.engine !== 'cloud') return playWithBrowser(job, config)
  try {
    return await playWithCloud(job, config)
  } catch (error) {
    if (job.cancelled) return false
    console.warn('Cloud speech failed:', error)
    if (!config.cloud.fallbackToBrowser) throw error
    config.onFallback?.(error)
    return playWithBrowser(job, { ...config, engine: 'browser' })
  }
}

const processSpeechQueue = async () => {
  if (processingQueue || playbackPaused) return
  processingQueue = true
  try {
    while (speechQueue.length) {
      const job = speechQueue.shift()
      currentJob = job
      try {
        job.resolve(await playJob(job))
      } catch (error) {
        job.reject(error)
      } finally {
        currentJob = null
      }
    }
  } finally {
    processingQueue = false
  }
}

export function speakText(text, overrides = {}) {
  const content = String(text || '').trim()
  const config = resolveConfig(overrides)
  if (!content || !config) return Promise.resolve(false)

  if (overrides.interrupt) stopSpeaking()
  const result = new Promise((resolve, reject) => speechQueue.push({ text: content, config, resolve, reject, cancelled: false }))
  void processSpeechQueue()
  return result
}

export function stopSpeaking(options = {}) {
  const clearQueue = options.clearQueue !== false
  playbackPaused = false
  if (currentJob) currentJob.cancelled = true
  activeCancel?.()
  activeCancel = null
  cleanupAudio()
  if (isSpeechSynthesisSupported()) window.speechSynthesis.cancel()
  if (clearQueue) {
    while (speechQueue.length) speechQueue.shift().resolve(false)
  }
}

export function pauseSpeaking() {
  playbackPaused = true
  if (isSpeechSynthesisSupported() && window.speechSynthesis.speaking && !window.speechSynthesis.paused) {
    window.speechSynthesis.pause()
  }
  if (activeAudio && !activeAudio.paused) activeAudio.pause()
}

export function resumeSpeaking() {
  playbackPaused = false
  if (isSpeechSynthesisSupported() && window.speechSynthesis.paused) window.speechSynthesis.resume()
  if (activeAudio?.paused) activeAudio.play().catch(error => console.warn('Resume cloud speech failed:', error))
  void processSpeechQueue()
}
