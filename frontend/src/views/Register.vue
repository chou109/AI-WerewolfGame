<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-icon">📜</div>
      <h2>{{ $locale === 'zh-CN' ? 'AI狼人杀' : 'AI Werewolf' }}</h2>
      <p class="auth-sub">{{ $t('auth.registerTitle') }}</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="auth-form">
        <el-form-item :label="$t('auth.username')" prop="username">
          <el-input v-model="form.username" :placeholder="$t('auth.usernameRequired')" />
        </el-form-item>
        <el-form-item :label="$t('auth.password')" prop="password">
          <el-input v-model="form.password" type="password" :placeholder="$t('auth.passwordRequired')" show-password />
        </el-form-item>
        <el-form-item :label="$t('auth.confirmPassword')" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" :placeholder="$t('auth.confirmPassword')" show-password />
        </el-form-item>
        <el-form-item :label="$t('auth.nickname')" prop="nickname">
          <el-input v-model="form.nickname" :placeholder="$t('auth.nickname')" />
        </el-form-item>
        <el-form-item :label="$t('auth.email')" prop="email">
          <el-input v-model="form.email" :placeholder="$t('auth.email')" />
        </el-form-item>
        <el-form-item>
          <button type="button" class="auth-btn" @click="register" :disabled="loading">
            {{ loading ? ($locale==='zh-CN'?'注册中...':'Registering...') : $t('auth.register') }}
          </button>
        </el-form-item>
      </el-form>
      <p class="auth-link">{{ $t('auth.hasAccount') }} <a @click="$router.push('/login')">{{ $t('auth.goLogin') }}</a></p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const { proxy } = getCurrentInstance()
const $t = proxy.$t; const $locale = proxy.$locale
const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null); const loading = ref(false)
const form = reactive({ username: '', password: '', confirmPassword: '', nickname: '', email: '' })
const rules = {
  username: [{ required: true, message: () => $t('auth.usernameRequired'), trigger: 'blur' }, { min: 3, max: 20, message: '3-20 chars', trigger: 'blur' }],
  password: [{ required: true, message: () => $t('auth.passwordRequired'), trigger: 'blur' }, { min: 6, message: () => $t('auth.passwordMinLength'), trigger: 'blur' }],
  confirmPassword: [{ required: true, message: () => $t('auth.confirmPassword'), trigger: 'blur' }, { validator: (r, v, cb) => { if (v !== form.password) cb(new Error($t('auth.passwordsNotMatch'))); else cb() }, trigger: 'blur' }],
  nickname: [{ required: true, message: () => $t('auth.nickname'), trigger: 'blur' }, { min: 2, max: 20, message: '2-20 chars', trigger: 'blur' }],
  email: [{ required: true, message: () => $t('auth.email'), trigger: 'blur' }, { type: 'email', message: () => $t('auth.emailInvalid'), trigger: 'blur' }]
}
const register = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (v) => {
    if (!v) return
    loading.value = true
    const result = await userStore.register({ username: form.username, password: form.password, nickname: form.nickname, email: form.email })
    loading.value = false
    if (result.ok) {
      ElMessage.success($t('auth.registerSuccess'))
      router.push('/login')
      return
    }
    const errorKeys = {
      USERNAME_EXISTS: 'usernameExists',
      EMAIL_EXISTS: 'emailExists',
      INVALID_PARAMS: 'registerFailed',
      REGISTER_FAILED: 'registerFailed',
      NETWORK: 'serverUnreachable',
      TIMEOUT: 'serverUnreachable'
    }
    const key = errorKeys[result.errorCode]
    ElMessage.error(key ? $t(`auth.${key}`) : (result.message || $t('auth.registerFailed')))
  })
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: radial-gradient(ellipse at 50% 30%, #1a1520 0%, var(--bg-deepest) 70%); }
.auth-card { width: 460px; max-width: 92vw; padding: 36px 28px; background: var(--bg-card); border: 1px solid var(--gold-dark); border-radius: 16px; box-shadow: var(--shadow-gold), var(--shadow-card); text-align: center; }
.auth-icon { font-size: 2.8rem; margin-bottom: 8px; }
.auth-card h2 { font-size: 1.5rem; color: var(--gold); margin-bottom: 4px; }
.auth-sub { color: var(--text-secondary); font-size: 0.9rem; margin-bottom: 24px; }
.auth-form { text-align: left; }
.auth-btn { width: 100%; padding: 12px; font-family: var(--font-heading); font-weight: 700; font-size: 1rem; background: linear-gradient(135deg, var(--gold-dark), var(--gold)); color: var(--bg-deepest); border: none; border-radius: 6px; cursor: pointer; transition: all var(--transition-normal); box-shadow: var(--shadow-gold); }
.auth-btn:hover:not(:disabled) { box-shadow: var(--shadow-gold-strong); transform: translateY(-1px); }
.auth-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.auth-link { margin-top: 20px; color: var(--text-muted); font-size: 0.85rem; }
.auth-link a { color: var(--gold); cursor: pointer; text-decoration: underline; }
</style>
