<template>
  <div class="dialog-lab">
    <section class="lab-intro">
      <div>
        <span class="lab-kicker">01 / {{ $locale === 'zh-CN' ? '推理实验台' : 'REASONING LAB' }}</span>
        <h2>{{ $t('aiDialogTest.title') }}</h2>
        <p>{{ $locale === 'zh-CN' ? '选择一位 AI 玩家，直接检验模型风格、上下文理解与对话响应。' : 'Select an AI player to test its style, context awareness, and conversation responses.' }}</p>
      </div>
      <div class="agent-selector">
        <label>{{ $t('aiDialogTest.selectAiPlayer') }}</label>
        <el-select v-model="selectedAiPlayerId" :placeholder="$t('aiDialogTest.selectAiPlayer')">
          <el-option v-for="aiPlayer in aiPlayers" :key="aiPlayer.id" :label="aiPlayer.name" :value="aiPlayer.id" />
        </el-select>
      </div>
    </section>

    <section class="lab-layout">
      <main class="conversation-panel">
        <div class="conversation-heading">
          <div><span>SESSION</span><h3>{{ $t('aiDialogTest.dialogHistory') }}</h3></div>
          <button @click="clearHistory" :disabled="!dialogHistory.length">{{ $t('aiDialogTest.clearHistory') }}</button>
        </div>
        <div ref="historyRef" class="dialog-history">
          <div v-if="!dialogHistory.length" class="dialog-empty">
            <span>✦</span>
            <h4>{{ $locale === 'zh-CN' ? '等待第一条消息' : 'WAITING FOR A MESSAGE' }}</h4>
            <p>{{ $locale === 'zh-CN' ? '选择 AI 玩家并输入一段对话，开始测试。' : 'Choose an AI player and send a message to begin testing.' }}</p>
          </div>
          <article v-for="(message, index) in dialogHistory" :key="index" class="message-row" :class="message.role === 'user' ? 'user-message' : 'ai-message'">
            <span class="message-avatar">{{ message.role === 'user' ? '◌' : '✦' }}</span>
            <div class="message-bubble">
              <div class="message-meta"><b>{{ message.role === 'user' ? $t('aiDialogTest.me') : (selectedAiPlayer?.name || 'AI') }}</b><time>{{ message.time }}</time></div>
              <p>{{ message.content }}</p>
            </div>
          </article>
        </div>
        <div class="composer">
          <el-input v-model="inputMessage" type="textarea" :rows="3" :placeholder="$t('aiDialogTest.inputPlaceholder')" resize="none" @keydown.enter.ctrl="sendMessage" />
          <div class="composer-footer"><span>{{ $locale === 'zh-CN' ? 'Ctrl + Enter 发送' : 'CTRL + ENTER TO SEND' }}</span><el-button type="primary" @click="sendMessage" :loading="sending">{{ $t('aiDialogTest.send') }} <b>→</b></el-button></div>
        </div>
      </main>

      <aside class="agent-panel">
        <span class="side-kicker">AGENT PROFILE</span>
        <template v-if="selectedAiPlayer">
          <div class="agent-emblem">✦</div>
          <h3>{{ selectedAiPlayer.name }}</h3>
          <p class="agent-model">{{ selectedAiPlayer.modelName }}</p>
          <div class="agent-rule"></div>
          <dl>
            <div><dt>{{ $t('aiDialogTest.modelType') }}</dt><dd>{{ selectedAiPlayer.modelType }}</dd></div>
            <div><dt>{{ $t('aiDialogTest.personality') }}</dt><dd>{{ selectedAiPlayer.personality || '—' }}</dd></div>
            <div><dt>{{ $t('aiDialogTest.strategy') }}</dt><dd>{{ selectedAiPlayer.strategy || '—' }}</dd></div>
            <div><dt>{{ $t('aiDialogTest.apiKey') }}</dt><dd>{{ selectedAiPlayer.maskedApiKey || (getAiPlayerKey(selectedAiPlayer.id) ? '••••' : $t('aiDialogTest.notConfigured')) }}</dd></div>
            <div><dt>{{ $t('aiDialogTest.temperature') }}</dt><dd>{{ selectedAiPlayer.temperature || 0.7 }}</dd></div>
            <div><dt>{{ $t('aiDialogTest.maxTokens') }}</dt><dd>{{ selectedAiPlayer.maxTokens || 1000 }}</dd></div>
          </dl>
        </template>
        <template v-else>
          <div class="agent-placeholder">◌</div>
          <h3>{{ $locale === 'zh-CN' ? '尚未选择玩家' : 'NO AGENT SELECTED' }}</h3>
          <p>{{ $locale === 'zh-CN' ? '从上方选择一位已配置的 AI 玩家。' : 'Choose a configured AI player from above.' }}</p>
        </template>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, getCurrentInstance } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { getAiPlayerKey, getGlobalApiKey } from '../utils/apiKeys'

const { proxy } = getCurrentInstance()
const $t = proxy.$t
const $locale = proxy.$locale
const aiPlayers = ref([])
const selectedAiPlayerId = ref(null)
const inputMessage = ref('')
const dialogHistory = ref([])
const sending = ref(false)
const historyRef = ref(null)

const selectedAiPlayer = computed(() => aiPlayers.value.find(player => player.id === selectedAiPlayerId.value))
onMounted(fetchAiPlayers)

async function fetchAiPlayers() {
  try {
    const response = await axios.get('/ai/player/available')
    if (response.data.code === 200) aiPlayers.value = response.data.data
  } catch {
    ElMessage.error($t('aiPlayer.fetchListFailed'))
  }
}

const cleanApiKey = apiKey => (apiKey || '').replace(/`/g, '').trim().replace(/^['"]|['"]$/g, '')

const validateApiConfig = aiPlayer => {
  const apiKey = getAiPlayerKey(aiPlayer.id) || getGlobalApiKey()
  if (!apiKey) return null
  let url = aiPlayer.apiBaseUrl || ''
  if (!url.startsWith('http')) url = `https://${url}`
  url = url.replace(/\/$/, '')
  const isAnthropic = ['claude', 'anthropic'].includes(aiPlayer.modelType)
  const fullUrl = `${url}${isAnthropic ? '/messages' : '/chat/completions'}`
  const headers = { 'Content-Type': 'application/json' }
  const cleanKey = cleanApiKey(apiKey)
  if (isAnthropic) headers['x-api-key'] = cleanKey
  else headers.Authorization = `Bearer ${cleanKey}`
  return { fullUrl, headers, isAnthropic }
}

const scrollHistory = () => nextTick(() => { if (historyRef.value) historyRef.value.scrollTop = historyRef.value.scrollHeight })

async function sendMessage() {
  if (!selectedAiPlayer.value) { ElMessage.warning($t('aiDialogTest.selectPlayerFirst')); return }
  if (!inputMessage.value.trim()) { ElMessage.warning($t('aiDialogTest.enterContent')); return }
  const content = inputMessage.value.trim()
  dialogHistory.value.push({ role: 'user', content, time: new Date().toLocaleTimeString() })
  inputMessage.value = ''
  scrollHistory()
  sending.value = true
  try {
    const aiPlayer = selectedAiPlayer.value
    const config = validateApiConfig(aiPlayer)
    if (!config) {
      ElMessage.warning($t('aiDialogTest.noApiKey'))
      return
    }
    const system = `You are ${aiPlayer.name}, ${aiPlayer.personality || 'an AI assistant'}. ${aiPlayer.strategy || ''}`
    const requestBody = config.isAnthropic
      ? { model: aiPlayer.modelName, max_tokens: aiPlayer.maxTokens || 1000, temperature: aiPlayer.temperature || 0.7, messages: [{ role: 'user', content: `${system}\n\n${dialogHistory.value.map(item => `${item.role}: ${item.content}`).join('\n')}` }] }
      : { model: aiPlayer.modelName, temperature: aiPlayer.temperature || 0.7, max_tokens: aiPlayer.maxTokens || 1000, messages: [{ role: 'system', content: system }, ...dialogHistory.value.map(item => ({ role: item.role, content: item.content }))] }
    const response = await axios.post(config.fullUrl, requestBody, { headers: config.headers, timeout: 30000 })
    const reply = config.isAnthropic ? response.data?.content?.[0]?.text : response.data?.choices?.[0]?.message?.content
    if (!reply) throw new Error('Invalid response format')
    dialogHistory.value.push({ role: 'assistant', content: reply.trim(), time: new Date().toLocaleTimeString() })
  } catch (error) {
    const errorMessage = error.response?.status === 401 ? $t('aiDialogTest.authFailed') : error.response?.status === 429 ? $t('aiDialogTest.rateLimited') : error.message?.includes('timeout') ? $t('aiDialogTest.timeout') : `${$t('aiDialogTest.sendFailed')}: ${error.message || ''}`
    ElMessage.error(errorMessage)
    dialogHistory.value.push({ role: 'assistant', content: `⚠ ${errorMessage}`, time: new Date().toLocaleTimeString() })
  } finally {
    sending.value = false
    scrollHistory()
  }
}

const clearHistory = () => { dialogHistory.value = []; ElMessage.success($t('aiDialogTest.historyCleared')) }
</script>

<style scoped>
.dialog-lab { width: min(1240px, 100%); margin: 0 auto; padding: 26px 0 84px; }.lab-intro { display: flex; align-items: end; justify-content: space-between; gap: 30px; padding: 30px 0 38px; }.lab-kicker, .side-kicker { color: #d9b55d; font: 700 10px/1 var(--font-heading); letter-spacing: .18em; }.lab-intro h2 { margin: 14px 0 12px; color: #eef4f8; font-size: clamp(34px, 4vw, 50px); letter-spacing: -.045em; }.lab-intro p { max-width: 580px; margin: 0; color: #9dadbc; font-size: 16px; line-height: 1.65; }.agent-selector { width: 300px; }.agent-selector label { display: block; margin-bottom: 9px; color: #d9b55d; font: 700 10px/1 var(--font-heading); letter-spacing: .1em; }
.lab-layout { display: grid; grid-template-columns: minmax(0, 1fr) 310px; gap: 18px; align-items: stretch; }.conversation-panel, .agent-panel { border: 1px solid rgba(180, 204, 222, .18); border-radius: 12px; background: linear-gradient(155deg, #101d2a, #0b141f); }.conversation-panel { display: flex; flex-direction: column; min-height: 650px; overflow: hidden; }.conversation-heading { display: flex; align-items: center; justify-content: space-between; padding: 22px 24px; border-bottom: 1px solid rgba(180, 204, 222, .14); }.conversation-heading span { color: #d9b55d; font: 700 10px/1 var(--font-heading); letter-spacing: .14em; }.conversation-heading h3 { margin: 9px 0 0; color: #eff4f8; font-size: 21px; }.conversation-heading button { border: 0; color: #9eafbe; background: transparent; cursor: pointer; font: 700 10px/1 var(--font-heading); letter-spacing: .09em; }.conversation-heading button:hover:not(:disabled) { color: #e3bd66; }.conversation-heading button:disabled { opacity: .35; cursor: not-allowed; }
.dialog-history { flex: 1; overflow-y: auto; padding: 24px; background: radial-gradient(circle at 80% 0%, rgba(28, 51, 72, .38), transparent 35%); }.dialog-empty { display: grid; min-height: 350px; place-content: center; justify-items: center; text-align: center; }.dialog-empty span, .agent-placeholder { color: #d9b55d; font: 400 58px/.8 Georgia, serif; }.dialog-empty h4 { margin: 20px 0 9px; color: #edf3f7; font-size: 19px; }.dialog-empty p { max-width: 280px; margin: 0; color: #8fa1b1; font-size: 14px; line-height: 1.6; }.message-row { display: flex; gap: 11px; margin-bottom: 18px; }.message-row.user-message { flex-direction: row-reverse; }.message-avatar { display: grid; flex: 0 0 31px; width: 31px; height: 31px; place-items: center; border: 1px solid rgba(217, 181, 93, .3); border-radius: 50%; color: #dfb95e; font: 400 18px/1 Georgia, serif; }.message-bubble { max-width: min(78%, 600px); padding: 12px 14px; border: 1px solid rgba(180, 204, 222, .14); border-radius: 9px; background: #0a1420; }.user-message .message-bubble { border-color: rgba(217, 181, 93, .24); background: #182536; }.message-meta { display: flex; gap: 10px; align-items: center; margin-bottom: 7px; }.message-meta b { color: #e2bd66; font: 700 10px/1 var(--font-heading); letter-spacing: .08em; }.message-meta time { color: #728595; font-size: 11px; }.message-bubble p { margin: 0; color: #dbe5ec; font-size: 15px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
.composer { padding: 16px; border-top: 1px solid rgba(180, 204, 222, .14); background: #0d1824; }.composer-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }.composer-footer > span { color: #718494; font: 700 9px/1 var(--font-heading); letter-spacing: .1em; }.composer-footer b { margin-left: 8px; font-size: 15px; }
.agent-panel { padding: 24px; }.agent-emblem { margin: 45px 0 21px; color: #e3bd66; font: 400 64px/.7 Georgia, serif; }.agent-panel h3 { margin: 0 0 7px; overflow: hidden; color: #eff4f8; font-size: 24px; text-overflow: ellipsis; white-space: nowrap; }.agent-model { margin: 0; color: #9babba; font-size: 13px; word-break: break-all; }.agent-rule { height: 1px; margin: 24px 0; background: rgba(180, 204, 222, .15); }.agent-panel dl { display: grid; gap: 17px; margin: 0; }.agent-panel dl div { display: grid; gap: 6px; }.agent-panel dt { color: #7f91a0; font: 700 9px/1 var(--font-heading); letter-spacing: .12em; }.agent-panel dd { margin: 0; color: #d9e2e9; font-size: 14px; line-height: 1.45; word-break: break-word; }.agent-placeholder { margin: 110px 0 24px; }.agent-panel > p { color: #91a2b1; font-size: 14px; line-height: 1.6; }
@media (max-width: 850px) { .lab-intro { display: block; }.agent-selector { width: min(100%, 360px); margin-top: 24px; }.lab-layout { grid-template-columns: 1fr; }.agent-panel { display: none; } }
</style>
