// API Key 本地存储工具：密钥只在用户输入时写入本机，服务端永不回传。
const GLOBAL_API_KEY = 'globalApiKey'

export const getGlobalApiKey = () => localStorage.getItem(GLOBAL_API_KEY) || ''

export const getAiPlayerKey = id => (id ? localStorage.getItem(`aiKey_${id}`) || '' : '')

export const setAiPlayerKey = (id, apiKey) => {
  if (!id) return
  const clean = String(apiKey || '').replace(/`/g, '').trim()
  if (clean) localStorage.setItem(`aiKey_${id}`, clean)
  else localStorage.removeItem(`aiKey_${id}`)
}

export const removeAiPlayerKey = id => {
  if (id) localStorage.removeItem(`aiKey_${id}`)
}