// i18n system - reactive, no external dependency
import { reactive, computed } from 'vue'
import zhCN from './zh-CN.js'
import enUS from './en-US.js'

const messages = { 'zh-CN': zhCN, 'en-US': enUS }

// Reactive state
const state = reactive({
  locale: localStorage.getItem('locale') || 'zh-CN'
})

// Get nested translation by dot-separated key
function getNestedValue(obj, path) {
  if (!obj || !path) return path
  const keys = path.split('.')
  let current = obj
  for (const key of keys) {
    if (current[key] === undefined || current[key] === null) return path
    current = current[key]
  }
  return current
}

// Translation function
export function $t(key, params = {}) {
  const msgs = messages[state.locale] || messages['zh-CN']
  let value = getNestedValue(msgs, key)

  // If not found, try the other locale as fallback
  if (value === key) {
    const fallbackLocale = state.locale === 'zh-CN' ? 'en-US' : 'zh-CN'
    const fallbackMsgs = messages[fallbackLocale]
    value = getNestedValue(fallbackMsgs, key)
  }

  // Parameter interpolation: {name} -> value
  if (typeof value === 'string' && params) {
    Object.keys(params).forEach(param => {
      value = value.replace(new RegExp(`\\{${param}\\}`, 'g'), params[param])
    })
  }

  return value
}

// Set locale
export function setLocale(locale) {
  if (messages[locale]) {
    state.locale = locale
    localStorage.setItem('locale', locale)
  }
}

// Get current locale
export function getLocale() {
  return state.locale
}

// Toggle locale
export function toggleLocale() {
  const next = state.locale === 'zh-CN' ? 'en-US' : 'zh-CN'
  setLocale(next)
  return next
}

// i18n plugin for Vue
export default {
  install(app) {
    // Make $t available in all components
    app.config.globalProperties.$t = $t
    app.config.globalProperties.$locale = computed(() => state.locale)
    app.config.globalProperties.$setLocale = setLocale
    app.config.globalProperties.$toggleLocale = toggleLocale

    // Also provide as injectable
    app.provide('$t', $t)
    app.provide('$locale', computed(() => state.locale))
  }
}

export { messages, state }
