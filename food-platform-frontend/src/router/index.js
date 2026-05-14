import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/articles' },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  { path: '/articles', name: 'ArticleList', component: () => import('../views/ArticleList.vue') },
  { path: '/articles/:id', name: 'ArticleDetail', component: () => import('../views/ArticleDetail.vue') },
  { path: '/articles/publish', name: 'ArticlePublish', component: () => import('../views/ArticlePublish.vue'), meta: { requireAuth: true } },
  { path: '/merchant/apply', name: 'MerchantApply', component: () => import('../views/MerchantApply.vue'), meta: { requireAuth: true } },
  { path: '/merchant/info', name: 'MerchantInfo', component: () => import('../views/MerchantInfo.vue'), meta: { requireAuth: true } },
  { path: '/user/info', name: 'UserInfo', component: () => import('../views/UserInfo.vue'), meta: { requireAuth: true } },
  { path: '/coupon/manage', name: 'CouponManage', component: () => import('../views/CouponManage.vue'), meta: { requireAuth: true } },
  { path: '/coupon/verify', name: 'CouponVerify', component: () => import('../views/CouponVerify.vue'), meta: { requireAuth: true } },
  { path: '/my-coupons', name: 'MyCoupons', component: () => import('../views/MyCoupons.vue'), meta: { requireAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')
  if (to.meta.requireAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
