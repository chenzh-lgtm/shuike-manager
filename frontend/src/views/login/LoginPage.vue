<template>
  <div class="login-page">
    <div class="login-hero">
      <div class="hero-pattern"></div>
      <div class="hero-content">
        <img src="/校徽.jpg" alt="校徽" class="hero-logo" />
        <div class="hero-badge">v1.0</div>
        <h1 class="hero-title">水课管理系统</h1>
        <p class="hero-desc">教学材料智能化分析与审核平台</p>
        <div class="hero-features">
          <div class="feature-item"><span class="feature-dot"></span>AI 智能评审与优化建议</div>
          <div class="feature-item"><span class="feature-dot"></span>教学材料全流程线上审核</div>
          <div class="feature-item"><span class="feature-dot"></span>实时了解产业需求</div>
        </div>
      </div>
    </div>

    <div class="login-form-area">
      <div class="form-card">
        <div class="form-header"><h2>欢迎回来</h2><p>请登录您的账号</p></div>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名">
              <template #prefix>
                <span class="input-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                </span>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password @keyup.enter="handleLogin">
              <template #prefix>
                <span class="input-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                </span>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item><el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">登 录</el-button></el-form-item>
        </el-form>
        <div class="form-footer"><p>测试账号: admin / teacher1 / reviewer1 / dean1</p><p>统一密码: 123456</p></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'; import { useRouter, useRoute } from 'vue-router'; import { ElMessage } from 'element-plus'; import { useAuthStore } from '@/stores/auth'; import { authApi } from '@/api/auth'
const router = useRouter(); const route = useRoute(); const authStore = useAuthStore(); const formRef = ref(); const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = { username: [{ required: true, message: '请输入用户名', trigger: 'blur' }], password: [{ required: true, message: '请输入密码', trigger: 'blur' }] }
async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false); if (!valid) return
  loading.value = true
  try {
    const res: any = await authApi.login(form); authStore.setAuth(res.data.token, res.data.refreshToken, res.data.userInfo)
    ElMessage.success('登录成功')
    const roles = res.data.userInfo.roles; const redirect = route.query.redirect as string
    if (roles.includes('TEACHER')) router.push(redirect || '/teacher/dashboard')
    else if (roles.includes('COLLEGE_REVIEWER')) router.push(redirect || '/college/dashboard')
    else if (roles.includes('OFFICE')) router.push(redirect || '/office/dashboard')
    else if (roles.includes('DEAN')) router.push(redirect || '/dean/dashboard')
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '登录失败') } finally { loading.value = false }
}
</script>

<style scoped>
.login-page { display: flex; height: 100vh; background: var(--color-surface); }
.login-hero {
  flex: 1; background: linear-gradient(160deg, #0f2420 0%, #1a3c34 40%, #2a5c50 100%);
  position: relative; display: flex; align-items: center; justify-content: center; overflow: hidden; min-width: 0;
}
.hero-pattern { position: absolute; inset: 0; background: radial-gradient(circle at 20% 80%, rgba(200,150,62,0.08) 0%, transparent 50%), radial-gradient(circle at 80% 20%, rgba(200,150,62,0.05) 0%, transparent 50%); pointer-events: none; }
.hero-content { position: relative; z-index: 1; color: #fff; text-align: center; padding: 48px; max-width: 420px; width: 100%; display: flex; flex-direction: column; align-items: center; }
.hero-logo { display: block; width: 60px; height: 60px; margin-bottom: 20px; border-radius: 12px; object-fit: cover; }
.hero-badge { display: inline-block; padding: 4px 14px; border: 1px solid rgba(200,150,62,0.4); border-radius: 20px; font-size: 12px; letter-spacing: 0.08em; color: var(--color-accent-light); margin-bottom: 24px; }
.hero-title { font-family: var(--font-display); font-size: 36px; font-weight: 700; letter-spacing: 0.06em; margin-bottom: 12px; line-height: 1.3; }
.hero-desc { font-size: 15px; color: rgba(255,255,255,0.6); margin-bottom: 40px; letter-spacing: 0.04em; }
.hero-features { text-align: left; }
.feature-item { display: flex; align-items: center; gap: 12px; padding: 10px 0; font-size: 14px; color: rgba(255,255,255,0.75); border-bottom: 1px solid rgba(255,255,255,0.06); }
.feature-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--color-accent); flex-shrink: 0; }

.login-form-area { width: 480px; display: flex; align-items: center; justify-content: center; padding: 48px; background: var(--color-bg); flex-shrink: 0; }
.form-card { width: 100%; max-width: 360px; }
.form-header { text-align: center; margin-bottom: 32px; }
.form-header h2 { font-family: var(--font-display); font-size: 26px; font-weight: 700; color: var(--color-primary); letter-spacing: 0.04em; margin-bottom: 6px; }
.form-header p { font-size: 14px; color: var(--color-text-muted); }
.login-btn { width: 100% !important; height: 44px !important; font-size: 16px !important; letter-spacing: 0.08em !important; }
.login-btn:hover { transform: translateY(-1px); box-shadow: var(--shadow-md); }
.input-icon { display: flex; align-items: center; margin-left: 4px; color: var(--color-text-muted); }
.input-icon svg { width: 18px; height: 18px; transition: color 0.2s; }
.el-input:focus-within .input-icon svg, .el-input.is-focus .input-icon svg { color: var(--color-primary); }
.form-footer { text-align: center; margin-top: 24px; padding: 16px; background: var(--color-card); border-radius: var(--radius-md); }
.form-footer p { font-size: 12px; color: var(--color-text-muted); line-height: 1.8; }
@media (max-width: 768px) { .login-hero { display: none; } .login-form-area { width: 100%; } }
</style>
