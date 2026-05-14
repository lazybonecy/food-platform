<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { notificationApi } from './api'
import { connectWs, disconnectWs } from './ws'
import { ElNotification } from 'element-plus'
import AiChatWidget from './components/AiChatWidget.vue'

const router = useRouter()
const isLoggedIn = ref(!!localStorage.getItem('accessToken'))
const searchQuery = ref('')

// WebSocket 收到实时通知
const handleWsMessage = (data) => {
  if (data.type !== 'notification') return
  unreadCount.value++
  // 通知文章详情页刷新评论
  window.dispatchEvent(new CustomEvent('ws-notification', { detail: data }))
  // 如果通知面板已打开，插入新通知到列表顶部
  if (showNotifications.value) {
    notifications.value.unshift({
      id: data.id,
      fromNickname: data.fromNickname,
      type: data.notifyType,
      typeDesc: data.typeDesc,
      articleId: data.articleId,
      articleTitle: data.articleTitle,
      commentId: data.commentId,
      content: data.content,
      isRead: 0,
      createTime: new Date().toISOString()
    })
  }
  // 弹出实时提示
  const preview = data.content ? (data.content.length > 30 ? data.content.slice(0, 30) + '...' : data.content) : ''
  ElNotification({
    title: `${data.fromNickname} ${data.typeDesc}`,
    message: preview || (data.articleTitle ? `「${data.articleTitle}」` : ''),
    type: 'info',
    duration: 4000,
    onClick: () => {
      if (data.articleId) {
        const hash = data.commentId ? `#comment-${data.commentId}` : ''
        router.push(`/articles/${data.articleId}${hash}`)
      }
    }
  })
}

// 监听登录状态变化（跨组件通知）
const onAuthChanged = () => {
  isLoggedIn.value = !!localStorage.getItem('accessToken')
  if (isLoggedIn.value) {
    fetchUnreadCount()
    connectWs(handleWsMessage)
  } else {
    unreadCount.value = 0
    notifications.value = []
    disconnectWs()
  }
}

// 页面重新可见时刷新未读数
const onVisibilityChange = () => {
  if (document.visibilityState === 'visible' && isLoggedIn.value) {
    fetchUnreadCount()
  }
}

// 通知相关
const unreadCount = ref(0)
const notifications = ref([])
const showNotifications = ref(false)

const fetchUnreadCount = async () => {
  if (!isLoggedIn.value) return
  try {
    const res = await notificationApi.unreadCount()
    unreadCount.value = res.data.count
  } catch (e) { /* ignore */ }
}

const fetchNotifications = async () => {
  if (!isLoggedIn.value) return
  try {
    const res = await notificationApi.list()
    notifications.value = res.data || []
  } catch (e) { /* ignore */ }
}

const handleOpenNotifications = () => {
  showNotifications.value = !showNotifications.value
  if (showNotifications.value) {
    fetchNotifications()
  }
}

const handleNotificationClick = async (item) => {
  // 标记已读
  if (!item.isRead) {
    try {
      await notificationApi.markRead(item.id)
      item.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (e) { /* ignore */ }
  }
  // 跳转到文章评论位置
  showNotifications.value = false
  if (item.articleId) {
    const hash = item.commentId ? `#comment-${item.commentId}` : ''
    router.push(`/articles/${item.articleId}${hash}`)
  }
}

const handleMarkAllRead = async () => {
  try {
    await notificationApi.markAllRead()
    unreadCount.value = 0
    notifications.value.forEach(n => n.isRead = 1)
  } catch (e) { /* ignore */ }
}

const formatNotifyTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return timeStr.slice(5, 10)
}

const getTypeIcon = (type) => {
  if (type === 1) return ' '
  if (type === 2) return ' '
  return '✉️'
}

const handleLogout = () => {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  window.dispatchEvent(new Event('auth-changed'))
  router.push('/login')
}

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: '/articles', query: { q: searchQuery.value.trim() } })
  }
}

// 点击外部关闭通知面板
const handleClickOutside = (e) => {
  if (showNotifications.value && !e.target.closest('.notify-wrap')) {
    showNotifications.value = false
  }
}

onMounted(() => {
  fetchUnreadCount()
  if (isLoggedIn.value) {
    connectWs(handleWsMessage)
  }
  document.addEventListener('click', handleClickOutside)
  window.addEventListener('auth-changed', onAuthChanged)
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onUnmounted(() => {
  disconnectWs()
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('auth-changed', onAuthChanged)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<template>
  <div class="app-wrapper">
    <header class="navbar">
      <div class="navbar-inner">
        <!-- 左侧 Logo -->
        <div class="nav-left">
          <div class="logo" @click="router.push('/')">
            <span class="logo-mark"> </span>
            <span class="logo-text">味集</span>
          </div>
        </div>

        <!-- 中间导航 -->
        <div class="nav-center">
          <router-link to="/articles" class="nav-link">
            <el-icon><Compass /></el-icon>
            <span>发现</span>
          </router-link>
          <router-link v-if="isLoggedIn" to="/articles/publish" class="nav-link">
            <el-icon><EditPen /></el-icon>
            <span>发布</span>
          </router-link>
          <router-link v-if="isLoggedIn" to="/my-coupons" class="nav-link">
            <el-icon><Ticket /></el-icon>
            <span>券包</span>
          </router-link>
        </div>

        <!-- 右侧搜索 + 通知 + 用户 -->
        <div class="nav-right">
          <div class="search-wrap">
            <el-input
              v-model="searchQuery"
              placeholder="搜索美食..."
              clearable
              @keyup.enter="handleSearch"
              class="search-ipt"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>

          <!-- 通知铃铛 -->
          <template v-if="isLoggedIn">
            <div class="notify-wrap">
              <div class="notify-btn" @click="handleOpenNotifications">
                <el-icon :size="18"><Bell /></el-icon>
                <span v-if="unreadCount > 0" class="notify-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
              </div>

              <!-- 通知面板 -->
              <div v-if="showNotifications" class="notify-panel">
                <div class="notify-panel-header">
                  <span class="notify-panel-title">消息通知</span>
                  <span class="notify-read-all" @click="handleMarkAllRead" v-if="unreadCount > 0">全部已读</span>
                </div>
                <div class="notify-list">
                  <div v-if="notifications.length === 0" class="notify-empty">
                    <p>暂无通知</p>
                  </div>
                  <div v-for="item in notifications" :key="item.id"
                    class="notify-item" :class="{ unread: !item.isRead }"
                    @click="handleNotificationClick(item)">
                    <div class="notify-icon">{{ getTypeIcon(item.type) }}</div>
                    <div class="notify-content">
                      <div class="notify-text">
                        <span class="notify-sender">{{ item.fromNickname }}</span>
                        <span class="notify-action">{{ item.typeDesc }}</span>
                      </div>
                      <div class="notify-preview" v-if="item.content">{{ item.content.length > 40 ? item.content.slice(0, 40) + '...' : item.content }}</div>
                      <div class="notify-article" v-if="item.articleTitle">
                        <span class="notify-article-title">「{{ item.articleTitle }}」</span>
                      </div>
                      <div class="notify-time">{{ formatNotifyTime(item.createTime) }}</div>
                    </div>
                    <div v-if="!item.isRead" class="notify-dot"></div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-if="isLoggedIn">
            <el-dropdown trigger="click">
              <div class="avatar-btn">
                <el-icon :size="18"><User /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/user/info')">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/merchant/info')">
                    <el-icon><Shop /></el-icon>商家中心
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/coupon/manage')">
                    <el-icon><Ticket /></el-icon>优惠券管理
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <div class="auth-btns">
              <el-button text class="btn-login" @click="router.push('/login')">登录</el-button>
              <el-button class="btn-register" @click="router.push('/register')">注册</el-button>
            </div>
          </template>
        </div>
      </div>
    </header>

    <main class="page-body">
      <router-view :key="$route.fullPath" />
    </main>

    <!-- AI 美食推荐助手 -->
    <AiChatWidget />
  </div>
</template>

<style scoped>
.app-wrapper {
  min-height: 100vh;
  background: #f5f5f5;
}

/* ===== 导航栏 ===== */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  z-index: 1000;
}

.navbar-inner {
  max-width: 1240px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

/* Logo */
.nav-left {
  flex-shrink: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
}

.logo-mark {
  font-size: 24px;
}

.logo-text {
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, #ff2442, #ff6b81);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
}

/* 中间导航 */
.nav-center {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 18px;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
  text-decoration: none;
  transition: all 0.15s;
}

.nav-link:hover {
  color: #333;
  background: #f5f5f5;
}

.nav-link.router-link-active {
  color: #ff2442;
  font-weight: 600;
  background: #fff0f2;
}

/* 右侧 */
.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.search-wrap {
  width: 220px;
}

.search-ipt :deep(.el-input__wrapper) {
  border-radius: 20px;
  background: #f5f5f5;
  box-shadow: none;
  padding-left: 4px;
}

.search-ipt :deep(.el-input__wrapper:hover),
.search-ipt :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #ddd inset;
}

/* 通知铃铛 */
.notify-wrap {
  position: relative;
}

.notify-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #666;
  transition: all 0.15s;
  position: relative;
}

.notify-btn:hover {
  background: #eee;
  color: #333;
}

.notify-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background: #ff2442;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  line-height: 1;
}

/* 通知面板 */
.notify-panel {
  position: absolute;
  top: 40px;
  right: 0;
  width: 360px;
  max-height: 480px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.12);
  overflow: hidden;
  z-index: 2000;
}

.notify-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.notify-panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #222;
}

.notify-read-all {
  font-size: 12px;
  color: #ff2442;
  cursor: pointer;
}

.notify-read-all:hover {
  text-decoration: underline;
}

.notify-list {
  overflow-y: auto;
  max-height: 420px;
}

.notify-empty {
  text-align: center;
  padding: 40px 0;
  color: #ccc;
  font-size: 14px;
}

.notify-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 20px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}

.notify-item:hover {
  background: #f9f9f9;
}

.notify-item.unread {
  background: #fff8f8;
}

.notify-icon {
  font-size: 20px;
  flex-shrink: 0;
  margin-top: 2px;
}

.notify-content {
  flex: 1;
  min-width: 0;
}

.notify-text {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
}

.notify-sender {
  font-weight: 600;
  color: #222;
}

.notify-action {
  color: #666;
  margin-left: 2px;
}

.notify-preview {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notify-article {
  margin-top: 4px;
}

.notify-article-title {
  font-size: 12px;
  color: #999;
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notify-time {
  font-size: 11px;
  color: #bbb;
  margin-top: 4px;
}

.notify-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff2442;
  flex-shrink: 0;
  margin-top: 6px;
}

.avatar-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #666;
  transition: all 0.15s;
}

.avatar-btn:hover {
  background: #eee;
  color: #333;
}

.auth-btns {
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-login {
  color: #666;
  font-size: 14px;
}

.btn-register {
  background: #ff2442;
  border-color: #ff2442;
  border-radius: 20px;
  font-size: 13px;
  padding: 6px 18px;
}

.btn-register:hover {
  background: #e61e3a;
  border-color: #e61e3a;
}

/* ===== 页面主体 ===== */
.page-body {
  padding-top: 56px;
  min-height: 100vh;
}
</style>
