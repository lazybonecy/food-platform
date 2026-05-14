<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, couponApi, commentApi } from '../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const article = ref(null)
const coupon = ref(null)
const loading = ref(false)

const flashLoading = ref(false)
const stockDisplay = ref(0)
let stockPollingTimer = null

const isLoggedIn = ref(!!localStorage.getItem('accessToken'))
const currentUserId = ref(null)

// 评论相关
const comments = ref([])
const commentText = ref('')
const replyTo = ref(null)
const commentLoading = ref(false)
const imgError = ref(false)

// 获取当前用户ID（从 token 解析）
const parseUserId = () => {
  const token = localStorage.getItem('accessToken')
  if (!token) return
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    currentUserId.value = payload.userId || payload.sub || null
  } catch (e) { /* ignore */ }
}

const saleStatus = computed(() => {
  if (!coupon.value) return 'unknown'
  const now = new Date()
  const start = new Date(coupon.value.startTime)
  const end = new Date(coupon.value.endTime)
  if (now < start) return 'upcoming'
  if (now > end) return 'expired'
  if (stockDisplay.value <= 0) return 'soldout'
  return 'active'
})

const commentCount = computed(() => {
  let count = comments.value.length
  for (const c of comments.value) {
    count += (c.replies?.length || 0)
  }
  return count
})

const fetchArticle = async () => {
  loading.value = true
  try {
    const res = await articleApi.detail(route.params.id)
    article.value = res.data
    if (article.value.couponId) {
      try {
        const couponRes = await couponApi.detail(article.value.couponId)
        coupon.value = couponRes.data
        stockDisplay.value = coupon.value.remainCount
        startStockPolling()
      } catch (e) { /* ignore */ }
    }
  } catch (e) { /* handled */ } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
  try {
    const res = await commentApi.list(route.params.id)
    comments.value = res.data || []
  } catch (e) { /* ignore */ }
}

const handleAddComment = async () => {
  if (!commentText.value.trim()) return
  commentLoading.value = true
  try {
    const data = {
      articleId: Number(route.params.id),
      content: commentText.value.trim()
    }
    if (replyTo.value) {
      // parentId 始终传顶级评论ID，保证两层树结构正确
      data.parentId = replyTo.value.parentId || replyTo.value.id
      // replyToUserId 指定被回复的人（用于通知）
      data.replyToUserId = replyTo.value.userId
      // 内容加上"回复 @XXX："前缀
      data.content = `回复 @${replyTo.value.nickname}：${data.content}`
    }
    const res = await commentApi.add(data)
    const newComment = res.data

    if (replyTo.value) {
      // 添加到顶级评论的回复列表
      const topId = replyTo.value.parentId || replyTo.value.id
      const parent = comments.value.find(c => c.id === topId)
      if (parent) {
        if (!parent.replies) parent.replies = []
        parent.replies.push(newComment)
      }
      replyTo.value = null
    } else {
      comments.value.push(newComment)
    }
    commentText.value = ''
    ElMessage.success('评论成功')
  } catch (e) { /* handled */ } finally {
    commentLoading.value = false
  }
}

const handleDeleteComment = async (comment, isReply, parentId) => {
  try {
    await commentApi.delete(comment.id)
    if (isReply && parentId) {
      const parent = comments.value.find(c => c.id === parentId)
      if (parent) {
        parent.replies = parent.replies.filter(r => r.id !== comment.id)
      }
    } else {
      comments.value = comments.value.filter(c => c.id !== comment.id)
    }
    ElMessage.success('已删除')
  } catch (e) { /* handled */ }
}

const startReply = (comment) => {
  replyTo.value = comment
  nextTick(() => {
    document.querySelector('.comment-input textarea')?.focus()
  })
}

const cancelReply = () => {
  replyTo.value = null
}

const handleLike = async () => {
  try {
    if (article.value.liked) {
      await articleApi.unlike(article.value.id)
      article.value.liked = false
      article.value.likeCount--
    } else {
      await articleApi.like(article.value.id)
      article.value.liked = true
      article.value.likeCount++
    }
  } catch (e) { /* handled */ }
}

const handleCollect = async () => {
  try {
    if (article.value.collected) {
      await articleApi.uncollect(article.value.id)
      article.value.collected = false
      article.value.collectCount--
    } else {
      await articleApi.collect(article.value.id)
      article.value.collected = true
      article.value.collectCount++
    }
  } catch (e) { /* handled */ }
}

const handleFlashClaim = async () => {
  if (flashLoading.value) return
  flashLoading.value = true
  try {
    await couponApi.flashClaim(coupon.value.id)
    ElMessage.success('抢购成功，等待处理！')
    stockDisplay.value = Math.max(0, stockDisplay.value - 1)
  } catch (e) { /* handled */ } finally {
    flashLoading.value = false
  }
}

const startStockPolling = () => {
  stockPollingTimer = setInterval(async () => {
    try {
      const res = await couponApi.detail(coupon.value.id)
      stockDisplay.value = res.data.remainCount
    } catch (e) { /* ignore */ }
  }, 3000)
}

const getCouponDesc = () => {
  if (!coupon.value) return ''
  if (coupon.value.type === 1) return `满${coupon.value.threshold}减${coupon.value.discount}`
  if (coupon.value.type === 2) return `${coupon.value.discount * 10}折优惠`
  return '免费领取'
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return timeStr.slice(0, 10)
}

const getAvatarText = (nickname) => {
  if (!nickname || nickname === '用户') return 'U'
  return nickname.charAt(0)
}

// 实时刷新评论（收到当前文章的 WebSocket 通知时）
const onWsNotification = (e) => {
  const data = e.detail
  if (data.articleId && String(data.articleId) === String(route.params.id)) {
    fetchComments()
  }
}

// 滚动到评论锚点
const scrollToHash = () => {
  const hash = route.hash
  if (!hash || !hash.startsWith('#comment-')) return
  nextTick(() => {
    const el = document.getElementById(hash.slice(1))
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      el.classList.add('comment-highlight')
      setTimeout(() => el.classList.remove('comment-highlight'), 2000)
    }
  })
}

watch(() => route.hash, () => scrollToHash())

onMounted(() => {
  parseUserId()
  fetchArticle()
  fetchComments().then(scrollToHash)
  window.addEventListener('ws-notification', onWsNotification)
})
onUnmounted(() => {
  if (stockPollingTimer) clearInterval(stockPollingTimer)
  window.removeEventListener('ws-notification', onWsNotification)
})
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <div class="detail-layout" v-if="article">
      <!-- 左侧：文章内容 -->
      <div class="detail-left">
        <!-- 封面图 -->
        <div class="detail-cover" v-if="article.coverImage && !imgError">
          <img :src="article.coverImage" :alt="article.title" @error="imgError = true" />
        </div>

        <!-- 文章信息 -->
        <div class="detail-main">
          <div class="detail-header">
            <span class="detail-category" v-if="article.category">{{ article.category }}</span>
            <h1 class="detail-title">{{ article.title }}</h1>
            <div class="detail-meta">
              <span>{{ article.createTime }}</span>
              <span class="meta-dot">·</span>
              <span>{{ article.viewCount }} 浏览</span>
            </div>
          </div>

          <div class="detail-content">{{ article.content }}</div>

          <!-- 互动栏 -->
          <div class="action-bar">
            <div class="action-btn" :class="{ active: article.liked }" @click="handleLike">
              <el-icon :size="20"><Star /></el-icon>
              <span>{{ article.likeCount || 0 }}</span>
            </div>
            <div class="action-btn" :class="{ active: article.collected }" @click="handleCollect">
              <el-icon :size="20"><Collection /></el-icon>
              <span>{{ article.collectCount || 0 }}</span>
            </div>
            <div class="action-btn" @click="router.push('/articles')">
              <el-icon :size="20"><Back /></el-icon>
              <span>返回</span>
            </div>
          </div>
        </div>

        <!-- 优惠券卡片 -->
        <div v-if="coupon" class="coupon-section"
          :class="{ 'flash-sale': saleStatus === 'active', 'sold-out': saleStatus === 'soldout' }">
          <div class="coupon-card">
            <div class="coupon-left">
              <div class="coupon-value">
                <template v-if="coupon.type === 1">¥{{ coupon.discount }}</template>
                <template v-else-if="coupon.type === 2">{{ coupon.discount * 10 }}折</template>
                <template v-else>免费</template>
              </div>
              <div class="coupon-type-label">{{ coupon.typeDesc }}</div>
            </div>
            <div class="coupon-info">
              <h3>{{ coupon.title }}</h3>
              <p class="coupon-desc">{{ getCouponDesc() }}</p>
              <p class="coupon-expire">有效期至：{{ coupon.endTime?.slice(0, 10) }}</p>
              <p class="coupon-stock">
                剩余 <strong>{{ stockDisplay }}</strong>/{{ coupon.totalCount }} 张
                <span v-if="coupon.limitPerUser > 0" class="limit-badge">限{{ coupon.limitPerUser }}张/人</span>
              </p>
            </div>
            <div class="coupon-action">
              <div v-if="saleStatus === 'upcoming'" class="countdown-box">
                <p class="countdown-label">距开抢还有</p>
                <el-countdown :value="new Date(coupon.startTime).getTime()" format="HH:mm:ss"
                  @finish="stockDisplay = coupon.remainCount" />
              </div>
              <el-button v-else-if="saleStatus === 'active' && isLoggedIn"
                type="danger" size="large" :loading="flashLoading" class="flash-btn" @click="handleFlashClaim">
                {{ coupon.originalPrice > 0 ? `¥${coupon.originalPrice} 立即抢购` : '立即抢购' }}
              </el-button>
              <el-button v-else-if="saleStatus === 'soldout'" type="info" size="large" disabled>已抢光</el-button>
              <el-button v-else-if="saleStatus === 'expired'" type="info" size="large" disabled>已结束</el-button>
              <el-button v-else-if="!isLoggedIn" type="danger" size="large" @click="router.push('/login')">登录后抢购</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：评论区 -->
      <div class="detail-right">
        <div class="comment-panel">
          <!-- 评论标题 -->
          <div class="comment-header">
            <span class="comment-title">评论</span>
            <span class="comment-count">{{ commentCount }}</span>
          </div>

          <!-- 评论列表 -->
          <div class="comment-list">
            <div v-if="comments.length === 0" class="comment-empty">
              <p>暂无评论，快来抢沙发吧~</p>
            </div>

            <div v-for="comment in comments" :key="comment.id" class="comment-item" :id="`comment-${comment.id}`">
              <!-- 顶级评论 -->
              <div class="comment-main">
                <div class="comment-avatar">
                  <span>{{ getAvatarText(comment.nickname) }}</span>
                </div>
                <div class="comment-body">
                  <div class="comment-meta">
                    <span class="comment-nickname">{{ comment.nickname }}</span>
                    <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                  </div>
                  <div class="comment-text">{{ comment.content }}</div>
                  <div class="comment-actions">
                    <span class="comment-action-btn" @click="startReply(comment)">回复</span>
                    <span class="comment-action-btn delete-btn"
                      v-if="currentUserId && currentUserId == comment.userId"
                      @click="handleDeleteComment(comment, false)">删除</span>
                  </div>

                  <!-- 回复列表 -->
                  <div v-if="comment.replies && comment.replies.length > 0" class="reply-list">
                    <div v-for="reply in comment.replies" :key="reply.id" class="reply-item" :id="`comment-${reply.id}`">
                      <div class="comment-avatar small">
                        <span>{{ getAvatarText(reply.nickname) }}</span>
                      </div>
                      <div class="comment-body">
                        <div class="comment-meta">
                          <span class="comment-nickname">{{ reply.nickname }}</span>
                          <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                        </div>
                        <div class="comment-text">{{ reply.content }}</div>
                        <div class="comment-actions">
                          <span class="comment-action-btn" @click="startReply(reply)">回复</span>
                          <span class="comment-action-btn delete-btn"
                            v-if="currentUserId && currentUserId == reply.userId"
                            @click="handleDeleteComment(reply, true, comment.id)">删除</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 评论输入框 -->
          <div class="comment-input-area">
            <div v-if="replyTo" class="reply-hint">
              <span>回复 {{ replyTo.nickname }}</span>
              <span class="reply-cancel" @click="cancelReply">取消</span>
            </div>
            <div v-if="isLoggedIn" class="comment-input">
              <textarea
                v-model="commentText"
                :placeholder="replyTo ? `回复 ${replyTo.nickname}...` : '说点什么吧...'"
                maxlength="500"
                rows="3"
              ></textarea>
              <div class="input-footer">
                <span class="char-count">{{ commentText.length }}/500</span>
                <el-button type="danger" size="small" :loading="commentLoading"
                  :disabled="!commentText.trim()" @click="handleAddComment">
                  发布
                </el-button>
              </div>
            </div>
            <div v-else class="comment-login-hint">
              <el-button type="danger" @click="router.push('/login')">登录后评论</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px 40px;
}

.detail-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* ===== 左侧文章区 ===== */
.detail-left {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
}

.detail-cover {
  width: 100%;
  max-height: 460px;
  overflow: hidden;
}

.detail-cover img {
  width: 100%;
  display: block;
  object-fit: cover;
}

.detail-main {
  padding: 24px 28px;
}

.detail-header {
  margin-bottom: 24px;
}

.detail-category {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  color: #fff;
  background: #ff2442;
  margin-bottom: 12px;
}

.detail-title {
  font-size: 24px;
  font-weight: 700;
  color: #222;
  line-height: 1.4;
  margin: 0 0 12px;
}

.detail-meta {
  font-size: 13px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 6px;
}

.meta-dot {
  font-size: 8px;
}

.detail-content {
  font-size: 16px;
  line-height: 1.9;
  color: #333;
  white-space: pre-wrap;
  margin-bottom: 32px;
}

/* 互动栏 */
.action-bar {
  display: flex;
  gap: 24px;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.action-btn:hover {
  background: #f5f5f5;
}

.action-btn.active {
  color: #ff2442;
}

.action-btn.active :deep(.el-icon) {
  color: #ff2442;
}

/* ===== 右侧评论区 ===== */
.detail-right {
  width: 380px;
  flex-shrink: 0;
  position: sticky;
  top: 76px;
  max-height: calc(100vh - 96px);
}

.comment-panel {
  background: #fff;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 96px);
}

.comment-header {
  padding: 20px 20px 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.comment-title {
  font-size: 16px;
  font-weight: 600;
  color: #222;
}

.comment-count {
  font-size: 13px;
  color: #999;
  margin-left: 6px;
}

/* 评论列表 */
.comment-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 20px;
  min-height: 0;
}

.comment-empty {
  text-align: center;
  padding: 40px 0;
  color: #ccc;
  font-size: 14px;
}

.comment-item {
  margin-bottom: 16px;
}

.comment-main {
  display: flex;
  gap: 10px;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b81, #ff2442);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.comment-avatar.small {
  width: 28px;
  height: 28px;
  font-size: 11px;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-nickname {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.comment-time {
  font-size: 11px;
  color: #bbb;
}

.comment-text {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  word-break: break-all;
}

.comment-actions {
  display: flex;
  gap: 12px;
  margin-top: 6px;
}

.comment-action-btn {
  font-size: 12px;
  color: #999;
  cursor: pointer;
  user-select: none;
}

.comment-action-btn:hover {
  color: #666;
}

.comment-action-btn.delete-btn {
  color: #ccc;
}

.comment-action-btn.delete-btn:hover {
  color: #ff2442;
}

/* 评论锚点高亮动画 */
.comment-item.comment-highlight,
.reply-item.comment-highlight {
  animation: highlight-flash 2s ease-out;
}

@keyframes highlight-flash {
  0% { background: #fff3cd; }
  100% { background: transparent; }
}

/* 回复列表 */
.reply-list {
  margin-top: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  padding: 12px;
}

.reply-item {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.reply-item:last-child {
  margin-bottom: 0;
}

/* 评论输入区 */
.comment-input-area {
  border-top: 1px solid #f0f0f0;
  padding: 16px 20px;
  flex-shrink: 0;
}

.reply-hint {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
  padding: 6px 10px;
  background: #f5f5f5;
  border-radius: 6px;
}

.reply-cancel {
  color: #ff2442;
  cursor: pointer;
}

.comment-input textarea {
  width: 100%;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.comment-input textarea:focus {
  border-color: #ff2442;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.char-count {
  font-size: 12px;
  color: #ccc;
}

.comment-login-hint {
  text-align: center;
  padding: 12px 0;
}

/* ===== 优惠券 ===== */
.coupon-section {
  padding: 0 28px 28px;
}

.coupon-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: #fff9f0;
  border: 1px solid #ffe0b2;
  border-radius: 12px;
  padding: 20px;
}

.coupon-left {
  background: linear-gradient(135deg, #ff2442, #ff6b81);
  color: #fff;
  padding: 16px 20px;
  text-align: center;
  border-radius: 10px;
  min-width: 80px;
}

.coupon-value {
  font-size: 28px;
  font-weight: bold;
}

.coupon-type-label {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.9;
}

.coupon-info {
  flex: 1;
}

.coupon-info h3 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #333;
}

.coupon-desc {
  margin: 0 0 4px;
  color: #666;
  font-size: 14px;
}

.coupon-expire {
  margin: 0 0 4px;
  color: #999;
  font-size: 13px;
}

.coupon-stock {
  margin: 0;
  color: #e6a23c;
  font-size: 13px;
}

.coupon-stock strong {
  color: #ff2442;
  font-size: 16px;
}

.limit-badge {
  background: #fff3e0;
  color: #e6a23c;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  margin-left: 8px;
}

.coupon-action {
  text-align: center;
}

.flash-btn {
  font-size: 16px;
  padding: 12px 28px;
  border-radius: 24px;
  animation: pulse-btn 1.5s infinite;
}

.countdown-box {
  text-align: center;
}

.countdown-label {
  color: #ff2442;
  font-size: 13px;
  margin-bottom: 4px;
}

.flash-sale {
  animation: pulse-border 2s infinite;
}

.sold-out {
  opacity: 0.6;
}

@keyframes pulse-border {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 36, 66, 0.3); }
  50% { box-shadow: 0 0 0 8px rgba(255, 36, 66, 0); }
}

@keyframes pulse-btn {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .detail-layout {
    flex-direction: column;
  }
  .detail-right {
    width: 100%;
    position: static;
    max-height: none;
  }
  .comment-panel {
    max-height: 500px;
  }
}

@media (max-width: 768px) {
  .detail-page { padding: 0; }
  .detail-left { border-radius: 0; }
  .detail-main { padding: 16px; }
  .coupon-section { padding: 0 16px 16px; }
  .coupon-card { flex-direction: column; align-items: stretch; }
  .coupon-action { margin-top: 12px; }
  .detail-right { border-radius: 0; }
  .comment-panel { border-radius: 0; }
}
</style>
