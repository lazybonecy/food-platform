import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '',
  timeout: 10000
})

// 请求拦截器 - 添加 token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 - 处理错误
api.interceptors.response.use(
  response => {
    const { data } = response
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(data)
    }
    return data
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      window.location.href = '/login'
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

// 认证 API
export const authApi = {
  register: (data) => api.post('/api/auth/register', data),
  login: (data) => api.post('/api/auth/login', data),
  refresh: (refreshToken) => api.post('/api/auth/refresh', { refreshToken })
}

// 用户 API
export const userApi = {
  getInfo: () => api.get('/api/user/info'),
  updateInfo: (data) => api.put('/api/user/info', data)
}

// 文章 API
export const articleApi = {
  list: (params) => api.get('/api/article/list', { params }),
  hot: (params) => api.get('/api/article/hot', { params }),
  detail: (id) => api.get(`/api/article/${id}`),
  publish: (data) => api.post('/api/article', data),
  update: (id, data) => api.put(`/api/article/${id}`, data),
  delete: (id) => api.delete(`/api/article/${id}`),
  like: (id) => api.post(`/api/article/${id}/like`),
  unlike: (id) => api.post(`/api/article/${id}/unlike`),
  collect: (id) => api.post(`/api/article/${id}/collect`),
  uncollect: (id) => api.post(`/api/article/${id}/uncollect`)
}

// 商家 API
export const merchantApi = {
  apply: (data) => api.post('/api/merchant/apply', data),
  getInfo: () => api.get('/api/merchant/info'),
  updateInfo: (data) => api.put('/api/merchant/info', data)
}

// 评论 API
export const commentApi = {
  list: (articleId) => api.get(`/api/comment/article/${articleId}`),
  add: (data) => api.post('/api/comment', data),
  delete: (id) => api.delete(`/api/comment/${id}`)
}

// 通知 API
export const notificationApi = {
  list: (params) => api.get('/api/notification/list', { params }),
  unreadCount: () => api.get('/api/notification/unread-count'),
  markRead: (id) => api.put(`/api/notification/${id}/read`),
  markAllRead: () => api.put('/api/notification/read-all')
}

// 优惠券 API
export const couponApi = {
  // 商家端
  create: (data) => api.post('/api/coupon', data),
  update: (id, data) => api.put(`/api/coupon/${id}`, data),
  list: (params) => api.get('/api/coupon/list', { params }),
  detail: (id) => api.get(`/api/coupon/${id}`),
  updateStatus: (id, status) => api.put(`/api/coupon/${id}/status`, { status }),
  // 核销端
  verify: (couponCode) => api.post('/api/coupon/verify', { couponCode }),
  confirmVerify: (couponCode) => api.post('/api/coupon/verify/confirm', { couponCode }),
  // 用户端
  claim: (id) => api.post(`/api/coupon/${id}/claim`),
  flashClaim: (id) => api.post(`/api/coupon/${id}/flash-claim`),
  myCoupons: (params) => api.get('/api/coupon/my', { params }),
  myCouponDetail: (id) => api.get(`/api/coupon/my/${id}`)
}

// AI 推荐 API
export const aiApi = {
  chat: (data) => api.post('/api/ai/chat', data, { timeout: 60000 }),
  clearHistory: (data) => api.post('/api/ai/clear-history', data)
}

export default api
