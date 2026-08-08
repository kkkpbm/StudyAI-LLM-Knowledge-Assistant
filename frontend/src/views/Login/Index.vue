<template>
  <div class="login-page">
    <div class="paper-grid" aria-hidden="true"></div>
    <main class="login-frame animate-scale-in">
      <section class="atlas-panel">
        <div class="atlas-brand">
          <div class="atlas-mark"><span></span><span></span><span></span></div>
          <div><strong>知径</strong><small>KNOWLEDGE ATLAS</small></div>
        </div>
        <div class="atlas-copy">
          <span class="eyebrow">PERSONAL KNOWLEDGE SYSTEM · 01</span>
          <h1>让每一次学习<br><em>留下路径</em></h1>
          <p>收集零散灵感，建立知识关系，再让 AI 帮你从已有思考中找到下一步。</p>
        </div>
        <div class="atlas-steps">
          <div><b>01</b><span>记录</span><small>捕捉值得保留的想法</small></div>
          <div><b>02</b><span>连接</span><small>看见概念之间的关系</small></div>
          <div><b>03</b><span>生长</span><small>把知识转化为行动</small></div>
        </div>
        <div class="atlas-orbit" aria-hidden="true"><i></i><i></i><i></i></div>
      </section>

      <section class="login-container">
        <div class="form-intro">
          <span class="form-number">{{ showRegister ? 'NEW / 02' : 'ENTRY / 01' }}</span>
          <h2>{{ showRegister ? '创建知识空间' : '回到你的知识空间' }}</h2>
          <p>{{ showRegister ? '从第一篇笔记开始建立个人知识地图。' : '继续整理、连接和推进你的学习。' }}</p>
        </div>

        <div class="form-section" v-if="!showRegister">
        <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin">
          <label class="field-label">用户名</label>
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="输入用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
          <label class="field-label">密码</label>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" size="large" class="submit-btn">
              进入知识空间 <span class="button-arrow">→</span>
            </el-button>
          </el-form-item>
        </el-form>
        <p class="switch-tip">
          还没有账号？
          <el-button link type="primary" @click="showRegister = true">立即注册</el-button>
        </p>
      </div>

      <div class="form-section" v-else>
        <el-form :model="regForm" :rules="regRules" ref="regFormRef" @submit.prevent="handleRegister">
          <label class="field-label">用户名</label>
          <el-form-item prop="username">
            <el-input
              v-model="regForm.username"
              placeholder="用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
          <label class="field-label">密码</label>
          <el-form-item prop="password">
            <el-input
              v-model="regForm.password"
              type="password"
              placeholder="密码（至少6位）"
              size="large"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>
          <label class="field-label">邮箱</label>
          <el-form-item prop="email">
            <el-input
              v-model="regForm.email"
              placeholder="邮箱（选填）"
              size="large"
              :prefix-icon="Message"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" :loading="regLoading" size="large" class="submit-btn">
              创建知识空间 <span class="button-arrow">→</span>
            </el-button>
          </el-form-item>
        </el-form>
        <p class="switch-tip">
          已有账号？
          <el-button link type="primary" @click="showRegister = false">返回登录</el-button>
        </p>
      </div>
        <div class="privacy-note"><span>●</span> 你的知识内容仅属于当前账户</div>
      </section>
    </main>
    <p class="footer-text">知径 · Personal Knowledge Atlas · 2026</p>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, shallowRef } from 'vue'
import { useUserStore } from '@/stores/user'
import { register as registerApi } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'

const userStore = useUserStore()
const loading = ref(false)
const regLoading = ref(false)
const formRef = ref()
const regFormRef = ref()
const showRegister = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const regForm = reactive({ username: '', password: '', email: '' })
const regRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  const valid = await regFormRef.value?.validate().catch(() => false)
  if (!valid) return
  regLoading.value = true
  try {
    await registerApi(regForm)
    ElMessage.success('注册成功，请登录')
    showRegister.value = false
    form.username = regForm.username
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; padding: 34px; display: grid; place-items: center; position: relative; overflow: auto; background: #eae7dd; }
.paper-grid { position: fixed; inset: 0; background-image: linear-gradient(rgba(49,94,251,.045) 1px, transparent 1px), linear-gradient(90deg, rgba(49,94,251,.045) 1px, transparent 1px); background-size: 42px 42px; mask-image: linear-gradient(to bottom, #000, transparent 90%); }
.login-frame { width: min(1080px, 100%); min-height: 680px; position: relative; z-index: 1; display: grid; grid-template-columns: 1.12fr .88fr; overflow: hidden; border: 1px solid rgba(20,43,41,.16); border-radius: 28px; background: var(--bg-surface); box-shadow: 0 38px 90px rgba(25,50,46,.16); }
.atlas-panel { padding: clamp(36px,5vw,64px); position: relative; overflow: hidden; display: flex; flex-direction: column; color: #eef4ed; background: radial-gradient(circle at 83% 12%, rgba(88,177,159,.22), transparent 26%), #102a28; }
.atlas-panel::after { content: ''; width: 220px; height: 220px; position: absolute; right: -80px; bottom: -80px; border: 1px solid rgba(255,255,255,.1); border-radius: 50%; box-shadow: 0 0 0 40px rgba(255,255,255,.025), 0 0 0 80px rgba(255,255,255,.018); }
.atlas-brand { display: flex; align-items: center; gap: 13px; }
.atlas-brand > div:last-child { display: flex; flex-direction: column; }
.atlas-brand strong { font-family: var(--font-display); font-size: 21px; letter-spacing: .14em; }
.atlas-brand small { margin-top: 4px; color: rgba(232,245,236,.46); font: 600 8px/1 var(--font-mono); letter-spacing: .15em; }
.atlas-mark { width: 42px; height: 42px; position: relative; border: 1px solid rgba(121,209,192,.34); border-radius: 13px 13px 13px 4px; }
.atlas-mark::before, .atlas-mark::after { content: ''; width: 22px; height: 1px; position: absolute; left: 9px; background: #69baa9; transform-origin: left; }
.atlas-mark::before { top: 13px; transform: rotate(35deg); }.atlas-mark::after { top: 30px; transform: rotate(-48deg); }
.atlas-mark span { width: 7px; height: 7px; position: absolute; z-index: 1; border-radius: 50%; background: #7acbbb; }
.atlas-mark span:nth-child(1){left:6px;top:8px}.atlas-mark span:nth-child(2){right:6px;top:21px}.atlas-mark span:nth-child(3){left:11px;bottom:4px;background:var(--accent-400)}
.atlas-copy { margin: auto 0; max-width: 470px; }
.eyebrow, .form-number { color: #77c6b6; font: 650 9px/1 var(--font-mono); letter-spacing: .16em; }
.atlas-copy h1 { margin: 20px 0 22px; font-family: var(--font-display); font-size: clamp(42px,5vw,67px); font-weight: 700; line-height: 1.14; letter-spacing: .01em; }
.atlas-copy h1 em { color: #83d0c0; font-style: normal; }
.atlas-copy p { max-width: 400px; color: rgba(228,241,234,.62); font-size: 14px; line-height: 1.9; }
.atlas-steps { display: grid; grid-template-columns: repeat(3,1fr); border-top: 1px solid rgba(255,255,255,.11); }
.atlas-steps div { padding: 18px 12px 0 0; display: grid; grid-template-columns: auto 1fr; gap: 2px 8px; }
.atlas-steps b { color: var(--accent-400); font: 500 9px/1 var(--font-mono); }.atlas-steps span { font-size: 12px; font-weight: 700; }.atlas-steps small { grid-column: 2; color: rgba(225,239,232,.38); font-size: 9px; }
.atlas-orbit { position: absolute; right: 9%; top: 26%; width: 110px; height: 110px; opacity: .25; border: 1px solid #75c8b7; border-radius: 50%; }
.atlas-orbit::before { content: ''; position: absolute; inset: 22px; border: 1px dashed #75c8b7; border-radius: 50%; }.atlas-orbit i { width: 6px; height: 6px; position: absolute; border-radius: 50%; background: #9be0d2; }.atlas-orbit i:nth-child(1){left:8px;top:20px}.atlas-orbit i:nth-child(2){right:4px;top:52px}.atlas-orbit i:nth-child(3){left:48px;bottom:-3px;background:var(--accent-400)}

.login-container { padding: clamp(38px,5vw,64px); display: flex; flex-direction: column; justify-content: center; background: linear-gradient(145deg,#fff 0%,#f8f6ef 100%); }
.form-intro { margin-bottom: 34px; }.form-number { color: var(--accent-500); }.form-intro h2 { margin: 14px 0 7px; font-family: var(--font-display); font-size: 27px; }.form-intro p { color: var(--text-secondary); font-size: 12px; }
.field-label { display: block; margin: 0 0 7px 2px; color: var(--text-secondary); font-size: 11px; font-weight: 700; letter-spacing: .06em; }
.form-section :deep(.el-form-item) { margin-bottom: 19px; }.form-section :deep(.el-input__wrapper) { min-height: 47px; background: #fff !important; }
.submit-btn { width: 100%; height: 48px; margin-top: 7px; display: flex; font-size: 13px; letter-spacing: .04em; }.button-arrow { margin-left: auto; font-size: 18px; }
.switch-tip { margin-top: 20px; color: var(--text-secondary); text-align: center; font-size: 12px; }
.privacy-note { margin-top: 26px; color: var(--text-tertiary); text-align: center; font-size: 10px; }.privacy-note span { margin-right: 6px; color: var(--success-500); font-size: 7px; }
.footer-text { position: fixed; z-index: 2; right: 26px; bottom: 16px; color: rgba(20,43,41,.42); font: 550 9px/1 var(--font-mono); letter-spacing: .09em; }

@media (max-width: 820px) { .login-page { padding: 18px; }.login-frame { grid-template-columns: 1fr; min-height: 0; }.atlas-panel { min-height: 270px; padding: 30px; }.atlas-copy { margin: 42px 0 20px; }.atlas-copy h1 { margin: 12px 0; font-size: 36px; }.atlas-copy p { display: none; }.atlas-steps { display: none; }.atlas-orbit { right: 8%; top: 38%; }.login-container { padding: 34px 30px; } }
@media (max-width: 480px) { .login-page { padding: 0; }.login-frame { min-height: 100vh; border: 0; border-radius: 0; }.atlas-panel { min-height: 220px; }.atlas-copy { margin-top: 28px; }.atlas-copy h1 { font-size: 30px; }.footer-text { display: none; } }
</style>
