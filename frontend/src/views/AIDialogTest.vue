<template>
  <div class="ai-dialog-test">
    <h2>AI玩家对话测试</h2>
    <div class="test-container">
      <!-- AI玩家选择 -->
      <div class="ai-player-select">
        <h3>选择AI玩家</h3>
        <el-select v-model="selectedAiPlayerId" placeholder="请选择AI玩家" style="width: 100%">
          <el-option
            v-for="aiPlayer in aiPlayers"
            :key="aiPlayer.id"
            :label="aiPlayer.name"
            :value="aiPlayer.id"
          />
        </el-select>
      </div>

      <!-- 对话区域 -->
      <div class="dialog-area">
        <h3>对话历史</h3>
        <div class="dialog-history">
          <div
            v-for="(message, index) in dialogHistory"
            :key="index"
            :class="['message-item', message.role === 'user' ? 'user-message' : 'ai-message']"
          >
            <div class="message-header">
              <span class="message-role">{{ message.role === 'user' ? '我' : selectedAiPlayer?.name }}</span>
              <span class="message-time">{{ message.time }}</span>
            </div>
            <div class="message-content">{{ message.content }}</div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            placeholder="请输入对话内容..."
            resize="none"
          />
          <div class="input-actions">
            <el-button type="primary" @click="sendMessage" :loading="sending">发送</el-button>
            <el-button @click="clearHistory">清空历史</el-button>
          </div>
        </div>
      </div>

      <!-- AI玩家信息 -->
      <div v-if="selectedAiPlayer" class="ai-player-info">
        <h3>AI玩家信息</h3>
        <el-card>
          <el-descriptions :column="1">
            <el-descriptions-item label="名称">{{ selectedAiPlayer.name }}</el-descriptions-item>
            <el-descriptions-item label="模型类型">{{ selectedAiPlayer.modelType }}</el-descriptions-item>
            <el-descriptions-item label="模型名称">{{ selectedAiPlayer.modelName }}</el-descriptions-item>
            <el-descriptions-item label="个性">{{ selectedAiPlayer.personality || '无' }}</el-descriptions-item>
            <el-descriptions-item label="策略">{{ selectedAiPlayer.strategy || '无' }}</el-descriptions-item>
            <el-descriptions-item label="温度">{{ selectedAiPlayer.temperature || 0.7 }}</el-descriptions-item>
            <el-descriptions-item label="最大tokens">{{ selectedAiPlayer.maxTokens || 1000 }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const aiPlayers = ref([])
const selectedAiPlayerId = ref(null)
const inputMessage = ref('')
const dialogHistory = ref([])
const sending = ref(false)

// 获取选中的AI玩家信息
const selectedAiPlayer = computed(() => {
  return aiPlayers.value.find(player => player.id === selectedAiPlayerId.value)
})

// 加载AI玩家列表
onMounted(() => {
  fetchAiPlayers()
})

const fetchAiPlayers = async () => {
  try {
    const response = await axios.get('/ai/player/available')
    if (response.data.code === 200) {
      aiPlayers.value = response.data.data
    }
  } catch (error) {
    console.error('Fetch AI players error:', error)
    ElMessage.error('获取AI玩家列表失败')
  }
}

// 发送消息
const sendMessage = async () => {
  if (!selectedAiPlayer.value) {
    ElMessage.warning('请先选择AI玩家')
    return
  }

  if (!inputMessage.value.trim()) {
    ElMessage.warning('请输入对话内容')
    return
  }

  // 添加用户消息到历史
  const userMessage = {
    role: 'user',
    content: inputMessage.value.trim(),
    time: new Date().toLocaleString()
  }
  dialogHistory.value.push(userMessage)

  // 清空输入框
  const messageContent = inputMessage.value.trim()
  inputMessage.value = ''

  sending.value = true
  try {
    // 构建API请求
    const aiPlayer = selectedAiPlayer.value
    const apiUrl = aiPlayer.apiBaseUrl || ''
    let apiKey = aiPlayer.apiKey || ''
    const modelName = aiPlayer.modelName || ''

    if (!apiUrl || !apiKey || !modelName) {
      throw new Error('AI玩家配置不完整，缺少API信息')
    }

    // 特殊检查：ModelScope API
    if (apiUrl.includes('modelscope.cn')) {
      console.log('ModelScope API 检查:', {
        apiKeyLength: apiKey.length,
        apiKeyStartsWith: apiKey.substring(0, 10) + '...',
        apiUrl: apiUrl
      })
      
      // 检查API Key格式
      if (!apiKey || apiKey.length < 20) {
        throw new Error('ModelScope API Key 格式不正确，长度至少为20个字符')
      }
      
      // 自动处理API密钥格式：去除ms-前缀
      if (apiKey.startsWith('ms-')) {
        apiKey = apiKey.substring(3)
        console.log('自动去除API密钥中的ms-前缀')
        console.log('处理后的API密钥:', apiKey.substring(0, 10) + '...')
      }
      
      // 检查API端点
      if (!apiUrl.includes('api-inference.modelscope.cn')) {
        console.warn('提示：推荐使用 api-inference.modelscope.cn 端点以获得更好的兼容性')
      }
    }

    // 确保API URL格式正确
    let finalApiUrl = apiUrl.trim()
    if (!finalApiUrl.startsWith('http')) {
      finalApiUrl = 'https://' + finalApiUrl
    }
    // 统一处理：不要添加结尾斜杠，保持与SDK示例一致
    if (finalApiUrl.endsWith('/')) {
      finalApiUrl = finalApiUrl.substring(0, finalApiUrl.length - 1)
    }
    
    // 自动修正ModelScope API端点（在添加端点路径之前）
    if (finalApiUrl.includes('modelscope.cn')) {
      if (!finalApiUrl.includes('api-inference.modelscope.cn')) {
        console.log('当前ModelScope API Base URL:', finalApiUrl)
        console.log('提示：如果遇到401错误，请检查API Key是否正确')
      } else {
        console.log('当前ModelScope API Base URL:', finalApiUrl)
        console.log('提示：api-inference.modelscope.cn 是推荐的端点，兼容性更好')
      }
    }
    
    // 根据API类型设置不同的认证方式
    const headers = {
      'Content-Type': 'application/json'
    }
    
    // 处理不同类型的API认证
    if (aiPlayer.modelType === 'modelscope' || aiPlayer.modelType === 'deepseek' || aiPlayer.modelType === 'qwen' || aiPlayer.modelType === 'glm' || aiPlayer.modelType === 'moonshot') {
      // ModelScope 及相关模型认证方式
      headers['Authorization'] = `Bearer ${apiKey}`
    } else if (aiPlayer.modelType === 'openai' || aiPlayer.modelType === 'gpt') {
      // OpenAI 认证方式
      headers['Authorization'] = `Bearer ${apiKey}`
    } else if (aiPlayer.modelType === 'claude' || aiPlayer.modelType === 'anthropic') {
      // Anthropic 认证方式
      headers['x-api-key'] = apiKey
    } else {
      // 默认认证方式
      headers['Authorization'] = `Bearer ${apiKey}`
    }

    // 根据API类型设置正确的端点
    if (aiPlayer.modelType === 'modelscope' || aiPlayer.modelType === 'deepseek' || aiPlayer.modelType === 'qwen' || aiPlayer.modelType === 'glm' || aiPlayer.modelType === 'moonshot') {
      // ModelScope 及相关模型端点
      finalApiUrl += '/chat/completions'
      console.log('添加端点路径后的完整API URL:', finalApiUrl)
    } else if (aiPlayer.modelType === 'openai' || aiPlayer.modelType === 'gpt') {
      // OpenAI 端点
      finalApiUrl += '/chat/completions'
    } else if (aiPlayer.modelType === 'claude' || aiPlayer.modelType === 'anthropic') {
      // Anthropic 端点
      finalApiUrl += '/messages'
    } else {
      // 默认端点
      finalApiUrl += '/chat/completions'
    }
    
    console.log('API请求信息:', {
      modelType: aiPlayer.modelType,
      apiUrl: finalApiUrl,
      modelName: modelName,
      headers: headers
    })

    // 构建请求体
    let requestBody = {}
    
    if (aiPlayer.modelType === 'modelscope') {
      // ModelScope 请求体
      requestBody = {
        model: modelName,
        messages: [
          {
            role: 'system',
            content: `你是${aiPlayer.name}，${aiPlayer.personality || '一个普通的AI助手'}。${aiPlayer.strategy || ''}`
          },
          ...dialogHistory.value.map(msg => ({
            role: msg.role,
            content: msg.content
          }))
        ],
        temperature: aiPlayer.temperature || 0.7,
        max_tokens: aiPlayer.maxTokens || 1000
      }
    } else if (aiPlayer.modelType === 'openai' || aiPlayer.modelType === 'gpt') {
      // OpenAI 请求体
      requestBody = {
        model: modelName,
        messages: [
          {
            role: 'system',
            content: `你是${aiPlayer.name}，${aiPlayer.personality || '一个普通的AI助手'}。${aiPlayer.strategy || ''}`
          },
          ...dialogHistory.value.map(msg => ({
            role: msg.role,
            content: msg.content
          }))
        ],
        temperature: aiPlayer.temperature || 0.7,
        max_tokens: aiPlayer.maxTokens || 1000
      }
    } else if (aiPlayer.modelType === 'claude' || aiPlayer.modelType === 'anthropic') {
      // Anthropic 请求体
      requestBody = {
        model: modelName,
        messages: [
          {
            role: 'user',
            content: `你是${aiPlayer.name}，${aiPlayer.personality || '一个普通的AI助手'}。${aiPlayer.strategy || ''}\n\n${dialogHistory.value.map(msg => `${msg.role}: ${msg.content}`).join('\n')}`
          }
        ],
        temperature: aiPlayer.temperature || 0.7,
        max_tokens: aiPlayer.maxTokens || 1000
      }
    } else {
      // 默认请求体
      requestBody = {
        model: modelName,
        messages: [
          {
            role: 'system',
            content: `你是${aiPlayer.name}，${aiPlayer.personality || '一个普通的AI助手'}。${aiPlayer.strategy || ''}`
          },
          ...dialogHistory.value.map(msg => ({
            role: msg.role,
            content: msg.content
          }))
        ],
        temperature: aiPlayer.temperature || 0.7,
        max_tokens: aiPlayer.maxTokens || 1000
      }
    }
    
    console.log('API请求体:', requestBody)
    
    const response = await axios.post(finalApiUrl, requestBody, {
      headers,
      timeout: 30000 // 30秒超时
    })

    console.log('API响应:', response.data)

    // 处理响应
    if (aiPlayer.modelType === 'modelscope') {
      // ModelScope 响应处理
      if (response.data && response.data.choices && response.data.choices.length > 0) {
        const aiResponse = response.data.choices[0].message
        if (aiResponse && aiResponse.content) {
          // 添加AI回复到历史
          dialogHistory.value.push({
            role: 'assistant',
            content: aiResponse.content.trim(),
            time: new Date().toLocaleString()
          })
        }
      } else {
        throw new Error('AI响应格式不正确')
      }
    } else if (aiPlayer.modelType === 'openai' || aiPlayer.modelType === 'gpt') {
      // OpenAI 响应处理
      if (response.data && response.data.choices && response.data.choices.length > 0) {
        const aiResponse = response.data.choices[0].message
        if (aiResponse && aiResponse.content) {
          // 添加AI回复到历史
          dialogHistory.value.push({
            role: 'assistant',
            content: aiResponse.content.trim(),
            time: new Date().toLocaleString()
          })
        }
      } else {
        throw new Error('AI响应格式不正确')
      }
    } else if (aiPlayer.modelType === 'claude' || aiPlayer.modelType === 'anthropic') {
      // Anthropic 响应处理
      if (response.data && response.data.content && response.data.content.length > 0) {
        const aiResponse = response.data.content[0]
        if (aiResponse && aiResponse.text) {
          // 添加AI回复到历史
          dialogHistory.value.push({
            role: 'assistant',
            content: aiResponse.text.trim(),
            time: new Date().toLocaleString()
          })
        }
      } else {
        throw new Error('AI响应格式不正确')
      }
    } else {
      // 默认响应处理
      if (response.data && response.data.choices && response.data.choices.length > 0) {
        const aiResponse = response.data.choices[0].message
        if (aiResponse && aiResponse.content) {
          // 添加AI回复到历史
          dialogHistory.value.push({
            role: 'assistant',
            content: aiResponse.content.trim(),
            time: new Date().toLocaleString()
          })
        }
      } else {
        throw new Error('AI响应格式不正确')
      }
    }
  } catch (error) {
    console.error('Send message error:', error)
    console.error('Error details:', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      config: {
        url: error.config?.url,
        method: error.config?.method,
        headers: error.config?.headers
      }
    })
    
    let errorMessage = '发送消息失败'
    if (error.response?.status === 401) {
      errorMessage = '认证失败：API Key 可能错误或已过期\n\n请检查：\n1. API Key是否正确复制\n2. API Key是否已过期\n3. 是否有该模型的访问权限\n4. API Key是否启用了对应模型的访问'
    } else if (error.response?.status === 404) {
      errorMessage = 'API端点不存在：请检查API Base URL是否正确'
    } else if (error.response?.status === 429) {
      errorMessage = '请求过于频繁：请稍后再试'
    } else if (error.response?.status === 500) {
      errorMessage = '服务器错误：请检查API服务状态'
    } else if (error.message?.includes('ERR_NAME_NOT_RESOLVED')) {
      errorMessage = '网络错误：DNS解析失败，请检查网络连接和DNS设置\n\n建议：\n1. 切换网络环境（如手机热点）\n2. 使用其他模型服务（如OpenAI）\n3. 检查DNS设置，尝试使用8.8.8.8'
    } else if (error.message?.includes('Network Error')) {
      errorMessage = '网络错误：无法连接到API服务，请检查网络连接'
    } else if (error.message?.includes('timeout')) {
      errorMessage = '请求超时：API响应时间过长，请检查网络连接'
    } else if (error.message) {
      errorMessage = `发送消息失败：${error.message}`
    }
    
    ElMessage.error(errorMessage)
    
    // 添加错误消息到历史
    dialogHistory.value.push({
      role: 'assistant',
      content: `错误：${errorMessage}`,
      time: new Date().toLocaleString()
    })
  } finally {
    sending.value = false
  }
}

// 清空历史
const clearHistory = () => {
  dialogHistory.value = []
  ElMessage.success('对话历史已清空')
}
</script>

<style scoped>
.ai-dialog-test {
  padding: 20px;
}

.test-container {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.ai-player-select {
  max-width: 400px;
}

.dialog-area {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.dialog-history {
  max-height: 500px;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
}

.message-item {
  margin-bottom: 20px;
  padding: 12px;
  border-radius: 8px;
  max-width: 80%;
}

.user-message {
  align-self: flex-end;
  margin-left: auto;
  background-color: #ecf5ff;
  border: 1px solid #d9ecff;
}

.ai-message {
  align-self: flex-start;
  background-color: #ffffff;
  border: 1px solid #e4e7ed;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.message-role {
  font-weight: 500;
  color: #606266;
}

.message-content {
  font-size: 14px;
  line-height: 1.5;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.input-area {
  border-top: 1px solid #e4e7ed;
  padding: 20px;
  background-color: #ffffff;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.ai-player-info {
  max-width: 500px;
}

/* 滚动条样式 */
.dialog-history::-webkit-scrollbar {
  width: 6px;
}

.dialog-history::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.dialog-history::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.dialog-history::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>