<script setup>
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { aiApi } from '../api'

const router = useRouter()
const showChat = ref(false)
const messages = ref([
  { role: 'ai', content: '你好！我是美食小助手，有什么可以帮你的？', type: 'chat', articles: [], coupons: [] }
])
const inputText = ref('')
const loading = ref(false)
const chatBody = ref(null)
const suggestions = ['推荐辣的', '好喝的奶茶', '学生党平价美食', '今天吃什么']

const toggleChat = () => {
  showChat.value = !showChat.value
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatBody.value) {
      chatBody.value.scrollTop = chatBody.value.scrollHeight
    }
  })
}

const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text, type: 'chat', articles: [], coupons: [] })
  inputText.value = ''
  scrollToBottom()

  loading.value = true
  try {
    const userId = getUserId()
    const res = await aiApi.chat({ message: text, userId })
    messages.value.push({
      role: 'ai',
      content: res.data.reply,
      type: res.data.type || 'chat',
      articles: res.data.articles || [],
      coupons: res.data.coupons || []
    })
  } catch (e) {
    messages.value.push({ role: 'ai', content: '抱歉，暂时无法处理您的请求，请稍后再试。', type: 'chat', articles: [], coupons: [] })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const getUserId = () => {
  const token = localStorage.getItem('accessToken')
  if (!token) return null
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.userId || payload.sub || null
  } catch (e) { return null }
}

const goToArticle = (id) => {
  router.push(`/articles/${id}`)
}

const clearHistory = async () => {
  const userId = getUserId()
  if (userId) {
    try {
      await aiApi.clearHistory({ userId })
    } catch (e) { /* ignore */ }
  }
  messages.value = [
    { role: 'ai', content: '你好！我是美食小助手，有什么可以帮你的？', type: 'chat', articles: [], coupons: [] }
  ]
}

const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <div class="ai-widget">
    <!-- 聊天面板 -->
    <transition name="panel">
      <div v-if="showChat" class="ai-panel" @click.stop>
        <div class="ai-header">
          <span class="ai-header-title">美食小助手</span>
          <div class="ai-header-actions">
            <span class="ai-header-clear" @click="clearHistory" title="清除对话历史">&#x21bb;</span>
            <span class="ai-header-close" @click="showChat = false">&times;</span>
          </div>
        </div>

        <div class="ai-body" ref="chatBody">
          <div v-for="(msg, i) in messages" :key="i" class="ai-msg" :class="msg.role">
            <div class="ai-bubble" :class="{ formatted: msg.role === 'ai' }">{{ msg.content }}</div>
            <!-- 文章卡片 -->
            <div v-if="msg.articles && msg.articles.length > 0" class="ai-articles">
              <div v-for="(article, idx) in msg.articles" :key="article.id"
                class="ai-article-card" @click="goToArticle(article.id)">
                <div class="ai-article-header">
                  <span v-if="msg.type === 'ranking'" class="ai-article-rank">{{ idx + 1 }}</span>
                  <div class="ai-article-title">{{ article.title }}</div>
                </div>
                <div class="ai-article-info">
                  <span v-if="article.category" class="ai-article-cat">{{ article.category }}</span>
                  <span v-if="article.reason" class="ai-article-reason">{{ article.reason }}</span>
                </div>
                <div class="ai-article-link">查看详情 <el-icon><ArrowRight /></el-icon></div>
              </div>
            </div>
            <!-- 优惠券卡片 -->
            <div v-if="msg.coupons && msg.coupons.length > 0" class="ai-coupons">
              <div v-for="coupon in msg.coupons" :key="coupon.id" class="ai-coupon-card">
                <div class="ai-coupon-left">
                  <span class="ai-coupon-amount">{{ coupon.discountAmount }}</span>
                  <span class="ai-coupon-label">元</span>
                </div>
                <div class="ai-coupon-right">
                  <div class="ai-coupon-name">{{ coupon.name }}</div>
                  <div class="ai-coupon-detail">满{{ coupon.minAmount }}元可用</div>
                  <div class="ai-coupon-expiry">
                    <span v-if="coupon.daysLeft !== undefined && coupon.daysLeft < 0" class="expired">已过期</span>
                    <span v-else-if="coupon.daysLeft === 0" class="expiring">今天到期</span>
                    <span v-else-if="coupon.daysLeft !== undefined" class="valid">剩余{{ coupon.daysLeft }}天</span>
                    <span v-else>有效期至 {{ coupon.endTime }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="loading" class="ai-msg ai">
            <div class="ai-bubble loading">
              <span class="loading-text">虾虾正在努力思考中<span class="loading-dots"><span>.</span><span>.</span><span>.</span></span></span>
            </div>
          </div>

          <!-- 快捷推荐标签 -->
          <div v-if="!loading && messages.length <= 2" class="ai-suggestions">
            <span v-for="tag in suggestions" :key="tag" class="ai-tag" @click="inputText = tag; handleSend()">{{ tag }}</span>
          </div>
        </div>

        <div class="ai-footer">
          <input
            v-model="inputText"
            placeholder="告诉我你想吃什么..."
            maxlength="200"
            @keydown="handleKeydown"
          />
          <button @click="handleSend" :disabled="!inputText.trim() || loading">
            <el-icon><Promotion /></el-icon>
          </button>
        </div>
      </div>
    </transition>

    <!-- 浮动按钮 -->
    <div class="ai-fab" @click="toggleChat" :class="{ active: showChat }">
      <svg v-if="!showChat" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
        <circle cx="12" cy="7" r="4"/>
      </svg>
      <span v-else>&times;</span>
    </div>
  </div>
</template>

<style scoped>
.ai-widget {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 3000;
}

/* 浮动按钮 */
.ai-fab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff2442, #ff6b81);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(255, 36, 66, 0.4);
  transition: all 0.3s;
  user-select: none;
}

.ai-fab:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 24px rgba(255, 36, 66, 0.5);
}

.ai-fab.active {
  background: #666;
  box-shadow: 0 4px 16px rgba(0,0,0,0.2);
  font-size: 24px;
}

/* 聊天面板 */
.ai-panel {
  position: absolute;
  bottom: 68px;
  right: 0;
  width: 380px;
  height: 520px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-header {
  padding: 16px 20px;
  background: linear-gradient(135deg, #ff2442, #ff6b81);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.ai-header-title {
  font-size: 15px;
  font-weight: 600;
}

.ai-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-header-clear {
  font-size: 18px;
  cursor: pointer;
  opacity: 0.8;
  transition: opacity 0.15s;
}

.ai-header-clear:hover {
  opacity: 1;
}

.ai-header-close {
  font-size: 22px;
  cursor: pointer;
  line-height: 1;
  opacity: 0.8;
}

.ai-header-close:hover {
  opacity: 1;
}

/* 消息区 */
.ai-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-msg {
  display: flex;
  flex-direction: column;
  max-width: 85%;
}

.ai-msg.user {
  align-self: flex-end;
}

.ai-msg.ai {
  align-self: flex-start;
}

.ai-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.ai-msg.user .ai-bubble {
  background: #ff2442;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-msg.ai .ai-bubble {
  background: #f5f5f5;
  color: #333;
  border-bottom-left-radius: 4px;
}

.ai-bubble.formatted {
  white-space: pre-line;
  line-height: 1.7;
}

/* 加载动画 */
.ai-bubble.loading {
  padding: 12px 18px;
}

.loading-text {
  font-size: 13px;
  color: #999;
}

.loading-dots span {
  animation: dot-blink 1.4s infinite;
  opacity: 0;
}

.loading-dots span:nth-child(1) { animation-delay: 0s; }
.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dot-blink {
  0%   { opacity: 0; }
  20%  { opacity: 1; }
  100% { opacity: 0; }
}

/* 文章卡片 */
.ai-articles {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.ai-article-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-left: 3px solid #ff2442;
  border-radius: 10px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.ai-article-card:hover {
  border-color: #ff2442;
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.15);
}

.ai-article-card:hover .ai-article-link {
  color: #ff2442;
}

.ai-article-title {
  font-size: 14px;
  font-weight: 600;
  color: #222;
  margin-bottom: 4px;
}

.ai-article-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.ai-article-cat {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #fff0f2;
  color: #ff2442;
  flex-shrink: 0;
}

.ai-article-reason {
  font-size: 12px;
  color: #888;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-article-link {
  font-size: 12px;
  color: #bbb;
  display: flex;
  align-items: center;
  gap: 2px;
  transition: color 0.15s;
}

.ai-article-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.ai-article-rank {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #ff2442;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 优惠券卡片 */
.ai-coupons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.ai-coupon-card {
  display: flex;
  border: 1px solid #f0f0f0;
  border-left: 3px solid #ff6b81;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.ai-coupon-left {
  width: 72px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff2442, #ff6b81);
  color: #fff;
  padding: 10px 0;
  flex-shrink: 0;
}

.ai-coupon-amount {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}

.ai-coupon-label {
  font-size: 11px;
  opacity: 0.9;
}

.ai-coupon-right {
  flex: 1;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.ai-coupon-name {
  font-size: 14px;
  font-weight: 600;
  color: #222;
  margin-bottom: 2px;
}

.ai-coupon-detail {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.ai-coupon-expiry {
  font-size: 11px;
}

.ai-coupon-expiry .expired {
  color: #999;
}

.ai-coupon-expiry .expiring {
  color: #ff2442;
  font-weight: 600;
}

.ai-coupon-expiry .valid {
  color: #52c41a;
}

/* 快捷标签 */
.ai-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 4px;
}

.ai-tag {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 16px;
  background: #fff0f2;
  color: #ff2442;
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid #ffe0e5;
}

.ai-tag:hover {
  background: #ff2442;
  color: #fff;
}

/* 输入区 */
.ai-footer {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.ai-footer input {
  flex: 1;
  border: 1px solid #e8e8e8;
  border-radius: 20px;
  padding: 8px 16px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.ai-footer input:focus {
  border-color: #ff2442;
}

.ai-footer button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #ff2442;
  border: none;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s;
}

.ai-footer button:hover {
  background: #e61e3a;
}

.ai-footer button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* 进入/退出动画 */
.panel-enter-active,
.panel-leave-active {
  transition: all 0.3s ease;
}

.panel-enter-from,
.panel-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

/* 响应式 */
@media (max-width: 480px) {
  .ai-panel {
    width: calc(100vw - 32px);
    right: -8px;
    height: 60vh;
  }
}
</style>
