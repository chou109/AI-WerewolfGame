// useTypewriter composable - progressive text reveal with speed control
import { ref, onUnmounted } from 'vue'

const SPEEDS = {
  slow: 80,    // 80ms per character
  normal: 50,  // 50ms per character
  fast: 30     // 30ms per character
}

export function useTypewriter() {
  const displayedText = ref('')
  const isTyping = ref(false)
  const isComplete = ref(false)
  const isPaused = ref(false)
  let timer = null
  let currentIndex = 0
  let fullText = ''
  let currentSpeed = SPEEDS.normal
  let completeCallback = null

  function typeNextChar() {
    if (isPaused.value || !isTyping.value) return
    if (currentIndex < fullText.length) {
      displayedText.value += fullText[currentIndex]
      currentIndex++
      timer = setTimeout(typeNextChar, currentSpeed)
    } else {
      timer = null
      isTyping.value = false
      isComplete.value = true
      if (completeCallback) completeCallback()
    }
  }

  function startTypewriter(text, options = {}) {
    // Clean up any existing timer
    stopTypewriter()

    fullText = text || ''
    currentIndex = 0
    currentSpeed = options.speed || SPEEDS.normal
    completeCallback = options.onComplete || null
    displayedText.value = ''
    isTyping.value = true
    isComplete.value = false
    isPaused.value = false

    if (!fullText || fullText.length === 0) {
      isTyping.value = false
      isComplete.value = true
      if (options.onComplete) options.onComplete()
      return
    }

    // Small initial delay for natural feel
    timer = setTimeout(typeNextChar, 200)
  }

  function pauseTypewriter() {
    if (!isTyping.value || isPaused.value) return
    if (timer) clearTimeout(timer)
    timer = null
    isPaused.value = true
  }

  function resumeTypewriter() {
    if (!isTyping.value || !isPaused.value) return
    isPaused.value = false
    timer = setTimeout(typeNextChar, currentSpeed)
  }

  function skipToEnd() {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    if (fullText) {
      displayedText.value = fullText
    }
    isTyping.value = false
    isComplete.value = true
    isPaused.value = false
  }

  function stopTypewriter() {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    isTyping.value = false
    isPaused.value = false
  }

  function setSpeed(speedKey) {
    if (SPEEDS[speedKey]) {
      currentSpeed = SPEEDS[speedKey]
    }
  }

  function getAvailableSpeeds() {
    return [
      { key: 'slow', label: '慢速 / Slow', value: SPEEDS.slow },
      { key: 'normal', label: '正常 / Normal', value: SPEEDS.normal },
      { key: 'fast', label: '快速 / Fast', value: SPEEDS.fast }
    ]
  }

  onUnmounted(() => {
    stopTypewriter()
  })

  return {
    displayedText,
    isTyping,
    isComplete,
    isPaused,
    startTypewriter,
    pauseTypewriter,
    resumeTypewriter,
    skipToEnd,
    stopTypewriter,
    setSpeed,
    getAvailableSpeeds,
    SPEEDS
  }
}
