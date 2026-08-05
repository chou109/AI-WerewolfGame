<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-icon">🏰</div>
      <h2>{{ $locale === 'zh-CN' ? 'AI狼人杀' : 'AI Werewolf' }}</h2>
      <p class="auth-sub">{{ $t('auth.loginTitle') }}</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" class="auth-form">
        <el-form-item :label="$t('auth.username')" prop="username">
          <el-input v-model="form.username" :placeholder="$t('auth.usernameRequired')" />
        </el-form-item>
        <el-form-item :label="$t('auth.password')" prop="password">
          <el-input v-model="form.password" type="password" :placeholder="$t('auth.passwordRequired')" show-password />
        </el-form-item>
        <el-form-item>
          <button type="button" class="auth-btn" @click="login" :disabled="loading">
            {{ loading ? ($locale==='zh-CN'?'登录中...':'Logging in...') : $t('auth.login') }}
          </button>
        </el-form-item>
      </el-form>
      <p class="auth-link">{{ $t('auth.noAccount') }} <a @click="$router.push('/register')">{{ $t('auth.goRegister') }}</a></p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const { proxy } = getCurrentInstance()
const $t = proxy.$t; const $locale = proxy.$locale
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null); const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: () => $t('auth.usernameRequired'), trigger: 'blur' }, { min: 3, max: 20, message: '3-20 chars', trigger: 'blur' }],
  password: [{ required: true, message: () => $t('auth.passwordRequired'), trigger: 'blur' }, { min: 6, message: () => $t('auth.passwordMinLength'), trigger: 'blur' }]
}
const login = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (v) => {
    if (!v) return
    loading.value = true
    const result = await userStore.login(form.username, form.password)
    loading.value = false
    if (result.ok) {
      ElMessage.success($t('auth.loginSuccess'))
      router.push(route.query.redirect || '/')
      return
    }
    const errorKeys = {
      INVALID_CREDENTIALS: 'invalidCredentials',
      TOKEN_MISSING: 'tokenMissing',
      NETWORK: 'serverUnreachable',
      TIMEOUT: 'serverUnreachable'
    }
    const key = errorKeys[result.errorCode]
    ElMessage.error(key ? $t(`auth.${key}`) : (result.message || $t('auth.loginFailed')))
  })
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: radial-gradient(ellipse at 50% 30%, #1a1520 0%, var(--bg-deepest) 70%); }
.auth-card { width: 400px; max-width: 90vw; padding: 40px 32px; background: var(--bg-card); border: 1px solid var(--gold-dark); border-radius: 16px; box-shadow: var(--shadow-gold), var(--shadow-card); text-align: center; }
.auth-icon { font-size: 3rem; margin-bottom: 12px; }
.auth-card h2 { font-size: 1.5rem; color: var(--gold); margin-bottom: 4px; }
.auth-sub { color: var(--text-secondary); font-size: 0.9rem; margin-bottom: 28px; }
.auth-form { text-align: left; }
.auth-btn { width: 100%; padding: 12px; font-family: var(--font-heading); font-weight: 700; font-size: 1rem; background: linear-gradient(135deg, var(--gold-dark), var(--gold)); color: var(--bg-deepest); border: none; border-radius: 6px; cursor: pointer; transition: all var(--transition-normal); box-shadow: var(--shadow-gold); }
.auth-btn:hover:not(:disabled) { box-shadow: var(--shadow-gold-strong); transform: translateY(-1px); }
.auth-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.auth-link { margin-top: 20px; color: var(--text-muted); font-size: 0.85rem; }
.auth-link a { color: var(--gold); cursor: pointer; text-decoration: underline; }
</style>
