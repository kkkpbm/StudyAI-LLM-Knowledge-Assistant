<template>
  <div class="ai-chat">
    <!-- 头部 -->
    <header class="chat-topbar">
      <div class="chat-brand">
        <div class="brand-dot" :class="{ 'brand-dot--thinking': loading }"></div>
        <div>
          <span class="brand-title">知识助手 AI</span>
          <span class="brand-status" v-if="!loading">{{ chatMode === 'knowledge' ? '在线 · 基于你的知识库回答' : '在线 · 不检索知识库的快速闲聊' }}</span>
          <span class="brand-status brand-status--active" v-else>正在思考中...</span>
        </div>
      </div>
      <div class="topbar-actions">
        <div class="mode-switch" aria-label="对话模式">
          <button type="button" :class="{ active: chatMode === 'knowledge' }" :disabled="loading" @click="chatMode = 'knowledge'">知识库问答</button>
          <button type="button" :class="{ active: chatMode === 'chat' }" :disabled="loading" @click="chatMode = 'chat'">普通闲聊</button>
        </div>
        <el-button text size="small" @click="clearHistory" class="action-btn">
          <el-icon :size="16"><Delete /></el-icon>
          <span class="hide-on-mobile">清空</span>
        </el-button>
      </div>
    </header>

    <!-- 消息区 -->
    <div class="chat-body" ref="msgContainer" @scroll="onScroll">
      <!-- 欢迎 -->
      <div v-if="messages.length === 0" class="welcome animate-fade-in-up">
        <div class="welcome-icon-box">
          <el-icon :size="36" color="#fff"><ChatDotRound /></el-icon>
        </div>
        <h2>有什么我可以帮你的？</h2>
        <p class="welcome-desc">基于你的知识库智能问答 · 知识图谱提取 · 学习规划</p>
        <div class="quick-chips">
          <button v-for="q in quickQuestions" :key="q" class="chip" @click="askQuestion(q)">
            {{ q }}
          </button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div
        v-for="(msg, i) in messages"
        :key="i"
        class="message-row animate-msg-in"
        :class="msg.role"
        :style="{ animationDelay: '0s' }"
        v-show="msg.role !== 'assistant' || Boolean(msg.content.trim())"
      >
        <div class="msg-avatar-sm">
          <el-avatar :size="32" v-if="msg.role === 'assistant'" class="ava-ai">
            <el-icon :size="16"><ChatDotRound /></el-icon>
          </el-avatar>
          <el-avatar :size="32" v-else class="ava-user">
            {{ username?.charAt(0) }}
          </el-avatar>
        </div>
        <div class="msg-body">
          <div v-if="msg.role === 'assistant'" class="msg-bubble" v-html="msg.renderedContent" />
          <div v-else class="msg-bubble">{{ msg.content }}</div>
          <div v-if="msg.sources?.length" class="source-panel">
            <span class="source-label">回答参考了 {{ msg.sources.length }} 篇笔记</span>
            <button v-for="source in msg.sources" :key="source.noteId"
                    @click="router.push(`/notes/${source.noteId}`)">
              <strong>{{ source.title }}</strong>
              <small>{{ Math.round((source.score || 0) * 100) }}% 相关</small>
            </button>
          </div>
          <div class="msg-foot">
            <span class="msg-time">{{ msg.time }}</span>
            <button
              v-if="msg.role === 'assistant' && msg.content"
              class="msg-copy"
              @click="saveAsNote(i)"
              title="保存为笔记"
            >
              <el-icon :size="13"><DocumentAdd /></el-icon>
            </button>
            <button v-if="msg.role === 'assistant'" class="msg-copy" @click="copyMessage(msg.content)">
              <el-icon :size="13"><CopyDocument /></el-icon>
            </button>
            <button
              v-if="msg.role === 'assistant' && i === messages.length - 1 && !loading"
              class="msg-copy"
              @click="regenerate(i)"
              title="重新生成"
            >
              <el-icon :size="13"><Refresh /></el-icon>
            </button>
          </div>
        </div>
      </div>

      <!-- 思考中 -->
      <div v-if="loading && !hasFirstToken" class="message-row assistant animate-msg-in">
        <div class="msg-avatar-sm">
          <el-avatar :size="32" class="ava-ai ava-thinking">
            <el-icon :size="16"><ChatDotRound /></el-icon>
          </el-avatar>
        </div>
        <div class="msg-body">
          <div class="thinking-card">
            <div class="thinking-phase">
              <span class="phase-icon" :class="{ done: thinkingPhase !== 'search' }">
                <el-icon :size="14"><Search /></el-icon>
              </span>
              <span class="phase-text">{{ chatMode === 'knowledge' ? '检索知识库' : '准备快速回答' }}</span>
              <span class="phase-check" v-if="thinkingPhase !== 'search'">
                <el-icon :size="12"><Check /></el-icon>
              </span>
            </div>
            <div class="thinking-phase" v-if="thinkingPhase !== 'search'">
              <span class="phase-icon" :class="{ done: thinkingPhase === 'done' }">
                <el-icon :size="14"><Loading /></el-icon>
              </span>
              <span class="phase-text">AI 正在生成回答</span>
              <span class="phase-dots" v-if="thinkingPhase === 'gen'"><span></span><span></span><span></span></span>
              <span class="phase-check" v-else><el-icon :size="12"><Check /></el-icon></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 回到底部 -->
      <Transition name="fade-up">
        <button v-if="showScrollBtn" class="scroll-bottom-btn" @click="scrollToBottom">
          <el-icon :size="16"><ArrowDown /></el-icon>
        </button>
      </Transition>
    </div>

    <!-- 输入区 -->
    <footer class="chat-footer">
      <div class="input-row">
        <textarea
          ref="textareaRef"
          v-model="input"
          class="chat-input"
          placeholder="输入你的问题，Enter 发送，Shift+Enter 换行..."
          :disabled="loading"
          rows="1"
          @keydown="onKeydown"
          @input="autoResize"
        ></textarea>
        <!-- 停止按钮 -->
        <button v-if="loading" class="stop-button" @click="stopGeneration" title="停止生成">
          <span class="stop-icon"></span>
        </button>
        <!-- 发送按钮 -->
        <button v-else class="send-button" @click="send" :disabled="!input.trim()">
          <el-icon :size="18"><Promotion /></el-icon>
        </button>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { syncChatMemory, aiChatStream, type StreamMeta, type HistoryMsg, type ChatMode } from '@/api/ai'
import { getChatHistory, saveChatMessage, clearChatHistory } from '@/api/chatHistory'
import { createNote } from '@/api/notes'
import { useUserStore } from '@/stores/user'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Loading, Check, ArrowDown, Refresh, DocumentAdd } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()
const username = ref(userStore.username)
const input = ref('')
const loading = ref(false)
const hasFirstToken = ref(false)
const thinkingPhase = ref<'search' | 'gen' | 'done'>('search')
const chatMode = ref<ChatMode>('knowledge')
interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  renderedContent: string
  time: string
  sources?: Array<{ noteId: number | string; title: string; snippet: string; score: number }>
  mode?: ChatMode
}

const messages = ref<ChatMessage[]>([])
const msgContainer = ref<HTMLElement>()
const textareaRef = ref<HTMLTextAreaElement>()
const showScrollBtn = ref(false)
const userScrolledUp = ref(false)

let abortController: AbortController | null = null

async function loadHistory() {
  try {
    const res: any = await getChatHistory(200)
    const records = res.data || []
    messages.value = records.map((r: any) => ({
      role: r.role === 'assistant' ? 'assistant' : 'user',
      content: r.content,
      renderedContent: r.role === 'assistant' ? renderMarkdown(r.content) : '',
      time: r.createdAt ? new Date(r.createdAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '',
    }))
  } catch { messages.value = [] }
}

async function clearHistory() {
  try { await clearChatHistory() } catch { /* */ }
  messages.value = []
  ElMessage.success('聊天记录已清除')
}

onMounted(() => {
  loadHistory()
  nextTick(() => scrollToBottom())
})
onBeforeUnmount(() => {
  abortController?.abort()
})

const quickQuestions = ref([
  '帮我总结最近的学习笔记',
  '生成一个学习计划',
  '解释什么是知识图谱',
  '推荐相关的学习资源',
])

// ── 输入处理 ──

function onKeydown(e: KeyboardEvent) {
  // IME 输入中不触发发送
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault()
    send()
  }
}

function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 160) + 'px'
}

// ── 滚动管理 ──

function onScroll() {
  const el = msgContainer.value
  if (!el) return
  const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 80
  userScrolledUp.value = !atBottom
  showScrollBtn.value = !atBottom
}

function scrollToBottom() {
  const el = msgContainer.value
  if (!el) return
  el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
  userScrolledUp.value = false
  showScrollBtn.value = false
}

function autoScrollIfNeeded() {
  if (!userScrolledUp.value) {
    scrollToBottom()
  }
}

// ── 发送 & 流式接收 ──

async function send() {
  const q = input.value.trim()
  if (!q || loading.value) return

  messages.value.push({ role: 'user', content: q, renderedContent: '', time: fmtTime(), mode: chatMode.value })
  saveChatMessage({ role: 'user', content: q }).catch(() => {})

  input.value = ''
  autoResize()
  loading.value = true
  hasFirstToken.value = false
  thinkingPhase.value = 'search'
  await nextTick()
  scrollToBottom()

  const idx = messages.value.push({ role: 'assistant', content: '', renderedContent: '', time: fmtTime() }) - 1

  // 创建 AbortController 用于取消
  abortController = new AbortController()

  // 构建最近对话历史（排除刚添加的占位消息）
  const recentHistory: HistoryMsg[] = messages.value
    .slice(0, -2)
    .slice(-10)
    .map(m => ({ role: m.role, content: m.content }))

  try {
    let raw = ''
    let streamMeta: StreamMeta | null = null
    let renderPending = false

    for await (const chunk of aiChatStream(q, undefined, abortController.signal, recentHistory, chatMode.value)) {
      if (typeof chunk === 'string') {
        if (!hasFirstToken.value) {
          hasFirstToken.value = true
          thinkingPhase.value = 'gen'
        }
        raw += chunk

        // 用 requestAnimationFrame 合并同一帧内的多次更新，始终 marked() 避免闪烁
        if (!renderPending) {
          renderPending = true
          requestAnimationFrame(async () => {
            renderPending = false
            messages.value[idx].content = raw
            messages.value[idx].renderedContent = renderMarkdown(raw)
            if (!userScrolledUp.value) {
              const el = msgContainer.value
              if (el) el.scrollTop = el.scrollHeight
            }
          })
        }
      } else {
        streamMeta = chunk
      }
    }

    // 等待最后一帧渲染完成
    await new Promise(r => requestAnimationFrame(r))
    thinkingPhase.value = 'done'

    // 服务端可能仅返回结束元数据而没有正文；不能保留一个空的助手气泡，
    // 也不能将空内容同步进长期记忆或聊天记录。
    if (!raw.trim()) {
      messages.value[idx].content = '抱歉，AI 未返回有效内容，请稍后重试。'
      messages.value[idx].renderedContent = renderMarkdown(messages.value[idx].content)
      return
    }

    messages.value[idx].content = raw
    messages.value[idx].renderedContent = renderMarkdown(raw)
    messages.value[idx].sources = streamMeta?.sources || []
    syncChatMemory(q, raw).catch(() => {})
    saveChatMessage({ role: 'assistant', content: raw }).catch(() => {})

    if (streamMeta?.should_save) {
      try {
        await ElMessageBox.confirm(
          '未在您的笔记中找到相关知识，是否将此问答保存为新笔记？',
          '保存为笔记',
          { confirmButtonText: '保存', cancelButtonText: '暂不', type: 'info' }
        )
        await createNote({
          title: streamMeta.suggested_title || q.slice(0, 50),
          contentMd: `**问题**：${q}\n\n**回答**：${raw}`,
        })
        ElMessage.success('已保存为笔记')
      } catch { /* 用户取消 */ }
    }
  } catch (e: any) {
    if (e?.name === 'AbortError') {
      // 用户手动停止
      if (!messages.value[idx].content) {
        messages.value[idx].content = '（已停止生成）'
        messages.value[idx].renderedContent = renderMarkdown(messages.value[idx].content)
      }
    } else if (!messages.value[idx].content) {
      messages.value[idx].content = '抱歉，AI 服务暂不可用，请稍后再试'
      messages.value[idx].renderedContent = renderMarkdown(messages.value[idx].content)
    }
  } finally {
    loading.value = false
    abortController = null
    await nextTick()
    autoScrollIfNeeded()
  }
}

function stopGeneration() {
  abortController?.abort()
}

async function saveAsNote(msgIdx: number) {
  // 找到该 assistant 消息对应的上一条 user 消息
  const assistantMsg = messages.value[msgIdx]
  const userMsg = messages.value[msgIdx - 1]
  if (!assistantMsg || !userMsg || userMsg.role !== 'user') return
  try {
    const question = userMsg.content
    const answer = assistantMsg.content
    await createNote({
      title: question.slice(0, 50),
      contentMd: `**问题**：${question}\n\n**回答**：${answer}`,
    })
    ElMessage.success('已保存为笔记')
  } catch {
    ElMessage.error('保存失败')
  }
}

async function regenerate(msgIdx: number) {
  // 找到该 assistant 消息对应的上一条 user 消息
  const userMsg = messages.value[msgIdx - 1]
  if (!userMsg || userMsg.role !== 'user') return
  // 删除当前 assistant 消息
  messages.value.splice(msgIdx, 1)
  // 用原始问题重新发送
  chatMode.value = userMsg.mode || 'knowledge'
  input.value = userMsg.content
  await nextTick()
  send()
}

function askQuestion(q: string) { input.value = q; send() }

function renderMarkdown(content: string) {
  return DOMPurify.sanitize(marked.parse(content) as string)
}

function copyMessage(c: string) {
  navigator.clipboard.writeText(c.replace(/<[^>]*>/g, ''))
  ElMessage.success('已复制')
}

function fmtTime() {
  return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.ai-chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  background: rgba(252,251,247,.94);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

/* ── 顶栏 ── */
.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-light);
  background: linear-gradient(90deg, rgba(232,245,241,.78), rgba(252,251,247,.9));
}
.chat-brand { display: flex; align-items: center; gap: var(--space-3); }
.brand-dot {
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--success-500);
  box-shadow: 0 0 0 3px rgba(16,185,129,0.15);
  transition: all 0.3s;
}
.brand-dot--thinking {
  background: var(--brand-500);
  box-shadow: 0 0 0 3px rgba(49,94,251,0.16);
  animation: pulse 1.2s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.3); }
}
.brand-title { font-family: var(--font-display); font-size: var(--text-base); font-weight: 700; display: block; line-height: 1.3; }
.brand-status { font-size: var(--text-xs); color: var(--text-secondary); display: block; }
.brand-status--active { color: var(--brand-600); font-weight: 500; }
.topbar-actions { display:flex; align-items:center; gap:var(--space-2); }
.mode-switch {
  display:flex;
  padding:3px;
  border:1px solid var(--border-light);
  border-radius:999px;
  background:rgba(255,255,255,.72);
}
.mode-switch button {
  border:0;
  border-radius:999px;
  padding:6px 10px;
  color:var(--text-secondary);
  background:transparent;
  font:600 12px/1 var(--font-body, inherit);
  cursor:pointer;
  transition:all .2s ease;
}
.mode-switch button.active { color:#fff; background:var(--brand-700); box-shadow:0 3px 8px rgba(49,94,251,.2); }
.mode-switch button:disabled { cursor:not-allowed; opacity:.55; }
.action-btn { color: var(--text-secondary); }

/* ── 消息区 ── */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  background-color: #f0eee6;
  background-image: radial-gradient(rgba(49,94,251,.09) .7px, transparent .7px);
  background-size: 18px 18px;
  position: relative;
  scroll-behavior: smooth;
  overflow-anchor: none;
}
.chat-body::before { content:''; position:absolute; inset:0; pointer-events:none; background:linear-gradient(90deg,rgba(255,255,255,.34),transparent 18%,transparent 82%,rgba(255,255,255,.34)); }

/* 欢迎 */
.welcome { width:min(100%,760px); align-self:center; text-align: center; padding: var(--space-12) var(--space-5); }
.welcome-icon-box {
  width: 72px; height: 72px;
  margin: 0 auto var(--space-5);
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, var(--brand-800), var(--brand-400));
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 24px rgba(49,94,251,0.25);
}
.welcome h2 { font-size: var(--text-2xl); font-weight: 700; margin-bottom: var(--space-2); }
.welcome-desc { color: var(--text-secondary); font-size: var(--text-sm); margin-bottom: var(--space-6); }
.quick-chips { display: flex; flex-wrap: wrap; gap: var(--space-2); justify-content: center; }
.chip {
  padding: 8px 16px;
  border: 1px solid var(--border-light);
  border-radius: 20px;
  background: var(--bg-surface);
  font-size: var(--text-sm);
  color: var(--text-primary);
  cursor: pointer;
  font-family: inherit;
  transition: all var(--transition-fast);
}
.chip:hover {
  background: var(--brand-50);
  border-color: var(--brand-300);
  color: var(--brand-600);
}

/* 消息入场动画 */
.animate-msg-in {
  animation: msgIn 0.35s ease-out both;
}
@keyframes msgIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 消息行 */
.message-row {
  display: flex;
  gap: var(--space-3);
  width: min(100%, 860px);
}
.message-row.user { width:auto; max-width:min(68%,640px); align-self: flex-end; flex-direction: row-reverse; }
.message-row.assistant { align-self: flex-start; }

.ava-ai { background: linear-gradient(135deg, var(--brand-800), var(--brand-400)); color: #fff; }
.ava-thinking {
  animation: avatarPulse 2s ease-in-out infinite;
}
@keyframes avatarPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(49,94,251,0.32); }
  50% { box-shadow: 0 0 0 8px rgba(49,94,251,0); }
}
.ava-user { background: linear-gradient(135deg, var(--success-500), var(--success-600)); color: #fff; font-weight: 600; }

.msg-body { max-width: 100%; }
.msg-bubble {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
  font-size: var(--text-sm);
  line-height: 1.65;
  word-break: break-word;
  border: 1px solid rgba(203,199,184,.7);
  box-shadow: var(--shadow-xs);
}
.message-row.user .msg-bubble {
  background: linear-gradient(135deg, var(--brand-800), var(--brand-600));
  border-color: transparent;
  color: #fff;
}
.msg-foot {
  display: flex; align-items: center; gap: var(--space-2);
  margin-top: 4px; padding: 0 4px;
}

.source-panel { margin-top:10px; padding:10px; display:flex; flex-wrap:wrap; gap:7px; border:1px solid var(--brand-100); border-radius:var(--radius-md); background:var(--brand-50); }
.source-label { width:100%; color:var(--brand-700); font-size:10px; font-weight:700; letter-spacing:.04em; }
.source-panel button { display:flex; align-items:center; gap:8px; padding:7px 10px; border:1px solid var(--brand-200); border-radius:999px; color:var(--text-primary); background:#fff; cursor:pointer; }
.source-panel button:hover { border-color:var(--brand-500); }
.source-panel small { color:var(--brand-600); font-size:9px; }
.msg-time { font-size: 11px; color: var(--text-tertiary); }
.msg-copy {
  border: none; background: none; cursor: pointer;
  color: var(--text-tertiary); padding: 2px;
  transition: color var(--transition-fast);
  display: inline-flex; align-items: center;
}
.msg-copy:hover { color: var(--brand-600); }

/* 思考中卡片 */
.thinking-card {
  padding: var(--space-3) var(--space-4);
  background: var(--bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 220px;
}
.thinking-phase {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}
.phase-icon {
  width: 24px; height: 24px;
  border-radius: 50%;
  background: var(--gray-100);
  display: flex; align-items: center; justify-content: center;
  color: var(--text-tertiary);
  flex-shrink: 0;
  transition: all 0.3s;
}
.phase-icon.done {
  background: var(--success-50);
  color: var(--success-500);
}
.phase-check {
  margin-left: auto;
  color: var(--success-500);
  display: flex; align-items: center;
}
.phase-dots {
  display: flex; gap: 3px;
  margin-left: auto;
}
.phase-dots span {
  width: 5px; height: 5px;
  border-radius: 50%;
  background: var(--brand-400);
  animation: dotPulse 1.4s ease-in-out infinite both;
}
.phase-dots span:nth-child(1) { animation-delay: 0s; }
.phase-dots span:nth-child(2) { animation-delay: 0.2s; }
.phase-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes dotPulse {
  0%, 80%, 100% { transform: scale(0.5); opacity: 0.3; }
  40% { transform: scale(1); opacity: 1; }
}

/* 回到底部按钮 */
.scroll-bottom-btn {
  position: sticky;
  bottom: 8px;
  align-self: center;
  width: 36px; height: 36px;
  border-radius: 50%;
  border: 1px solid var(--border-light);
  background: var(--bg-surface);
  box-shadow: var(--shadow-md);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  z-index: 10;
}
.scroll-bottom-btn:hover {
  color: var(--brand-600);
  border-color: var(--brand-300);
  transform: translateY(-2px);
}
.fade-up-enter-active, .fade-up-leave-active { transition: all 0.25s ease; }
.fade-up-enter-from, .fade-up-leave-to { opacity: 0; transform: translateY(8px); }

/* ── 输入区 ── */
.chat-footer {
  padding: var(--space-3) var(--space-5);
  border-top: 1px solid var(--border-light);
  background: var(--bg-surface);
}
.input-row {
  width:min(100%,920px);
  margin:0 auto;
  display: flex; align-items: flex-end; gap: var(--space-3);
  background: var(--gray-50);
  border-radius: var(--radius-xl);
  padding: 4px 4px 4px 16px;
  border: 1px solid var(--border-light);
  transition: border-color var(--transition-fast);
}
.input-row:focus-within {
  border-color: var(--brand-400);
  box-shadow: 0 0 0 3px rgba(49,94,251,0.08);
}
.chat-input {
  flex: 1;
  border: none; background: transparent;
  font-size: var(--text-sm); font-family: inherit;
  color: var(--text-primary);
  outline: none;
  resize: none;
  padding: 8px 0;
  line-height: 1.5;
  min-height: 24px;
  max-height: 160px;
}
.chat-input::placeholder { color: var(--text-tertiary); }

/* 发送按钮 */
.send-button {
  width: 38px; height: 38px;
  border: none; border-radius: 50%;
  background: var(--brand-700);
  color: #fff;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}
.send-button:hover:not(:disabled) {
  transform: scale(1.06);
  box-shadow: 0 5px 14px rgba(49,94,251,0.28);
}
.send-button:disabled { opacity: 0.4; cursor: not-allowed; }

/* 停止按钮 */
.stop-button {
  width: 38px; height: 38px;
  border: none; border-radius: 50%;
  background: var(--danger-500);
  color: #fff;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all var(--transition-fast);
  flex-shrink: 0;
  animation: stopPulse 1.5s ease-in-out infinite;
}
@keyframes stopPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239,68,68,0.4); }
  50% { box-shadow: 0 0 0 8px rgba(239,68,68,0); }
}
.stop-button:hover {
  transform: scale(1.06);
}
.stop-icon {
  width: 14px; height: 14px;
  background: #fff;
  border-radius: 3px;
}

@media (max-width: 768px) {
  .mode-switch button { padding:6px 8px; font-size:11px; }
  .message-row { max-width: 90%; }
  .chat-body { padding: var(--space-3); }
  .chat-footer { padding: var(--space-2) var(--space-3); }
  .hide-on-mobile { display: none; }
}
</style>
