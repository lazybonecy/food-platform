<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { articleApi } from '../api'

const router = useRouter()
const route = useRoute()

const articles = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(20)
const category = ref('')
const keyword = ref('')
const activeTab = ref('hot')
const loading = ref(false)
const imgErrors = ref({})

const categories = ['全部', '川菜', '粤菜', '湘菜', '鲁菜', '苏菜', '浙菜', '闽菜', '徽菜', '甜品', '小吃']

const fetchArticles = async () => {
  loading.value = true
  try {
    const params = { current: current.value, size: size.value }
    if (category.value && category.value !== '全部') {
      params.category = category.value
    }
    if (keyword.value) {
      params.keyword = keyword.value
    }
    const apiFn = activeTab.value === 'hot' ? articleApi.hot : articleApi.list
    const res = await apiFn(params)
    articles.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tab) => {
  activeTab.value = tab
  current.value = 1
  fetchArticles()
}

const handleCategoryChange = (cat) => {
  category.value = cat
  current.value = 1
  fetchArticles()
}

// 监听 URL 参数变化（来自顶部搜索框）
watch(() => route.query.q, (newQ) => {
  keyword.value = newQ || ''
  current.value = 1
  fetchArticles()
})

const handlePageChange = (page) => {
  current.value = page
  fetchArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const formatCount = (num) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return String(num)
}

// 随机高度让瀑布流更自然（纯展示用途）
const getCoverHeight = (id) => {
  const heights = [200, 240, 280, 180, 260, 220, 300]
  return heights[id % heights.length]
}

onMounted(() => {
  if (route.query.q) {
    keyword.value = route.query.q
  }
  fetchArticles()
})
</script>

<template>
  <div class="explore-page">
    <!-- 搜索结果提示 -->
    <div class="search-hint" v-if="keyword">
      <span class="hint-text">搜索「{{ keyword }}」的结果</span>
      <span class="hint-clear" @click="keyword = ''; router.push('/articles'); fetchArticles()">清除</span>
    </div>

    <!-- 分类标签栏 -->
    <div class="channel-bar">
      <div class="channel-inner">
        <div class="tab-switch">
          <span
            class="tab-btn"
            :class="{ active: activeTab === 'hot' }"
            @click="handleTabChange('hot')"
          >热门</span>
          <span
            class="tab-btn"
            :class="{ active: activeTab === 'latest' }"
            @click="handleTabChange('latest')"
          >最新</span>
        </div>
        <div class="channel-divider"></div>
        <div class="channel-list">
          <span
            v-for="cat in categories"
            :key="cat"
            class="channel-item"
            :class="{ active: category === cat || (!category && cat === '全部') }"
            @click="handleCategoryChange(cat)"
          >{{ cat }}</span>
        </div>
      </div>
    </div>

    <!-- 瀑布流 -->
    <div class="waterfall" v-loading="loading">
      <div
        v-for="item in articles"
        :key="item.id"
        class="note-item"
        @click="router.push(`/articles/${item.id}`)"
      >
        <!-- 封面 -->
        <div class="cover-wrap">
          <img
            v-if="item.coverImage && !imgErrors[item.id]"
            :src="item.coverImage"
            :alt="item.title"
            loading="lazy"
            class="cover-img"
            @error="imgErrors[item.id] = true"
          />
          <div class="cover-fallback" :style="{ height: getCoverHeight(item.id) + 'px' }" v-if="!item.coverImage || imgErrors[item.id]">
            <span class="cover-icon"> </span>
          </div>
        </div>

        <!-- 内容区 -->
        <div class="note-content">
          <div class="note-title">{{ item.title }}</div>
          <div class="note-meta">
            <div class="author">
              <div class="author-avatar">
                <el-icon :size="12"><User /></el-icon>
              </div>
              <span class="author-name">商家 #{{ item.merchantId }}</span>
            </div>
            <div class="like-btn">
              <el-icon :size="14"><StarFilled /></el-icon>
              <span>{{ formatCount(item.likeCount) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="!loading && articles.length === 0" description="暂无内容" />

    <!-- 分页 -->
    <div class="load-more" v-if="total > size">
      <el-pagination
        v-model:current-page="current"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.explore-page {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 20px 40px;
}

/* ===== 搜索提示 ===== */
.search-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fffbe6;
  border-radius: 8px;
  margin-bottom: 8px;
}

.hint-text {
  font-size: 14px;
  color: #333;
}

.hint-clear {
  font-size: 13px;
  color: #ff2442;
  cursor: pointer;
}

.hint-clear:hover {
  text-decoration: underline;
}

/* ===== 分类栏 ===== */
.channel-bar {
  position: sticky;
  top: 56px;
  z-index: 100;
  background: #f5f5f5;
  padding: 12px 0;
}

.channel-inner {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 8px 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.tab-switch {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.tab-btn {
  padding: 6px 16px;
  border-radius: 18px;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.tab-btn:hover {
  color: #333;
}

.tab-btn.active {
  background: #333;
  color: #fff;
}

.channel-divider {
  width: 1px;
  height: 20px;
  background: #e8e8e8;
  flex-shrink: 0;
}

.channel-list {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.channel-list::-webkit-scrollbar {
  display: none;
}

.channel-item {
  padding: 6px 14px;
  border-radius: 18px;
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}

.channel-item:hover {
  color: #333;
  background: #f5f5f5;
}

.channel-item.active {
  color: #333;
  font-weight: 600;
  background: #f0f0f0;
}

/* ===== 瀑布流 ===== */
.waterfall {
  column-count: 5;
  column-gap: 14px;
  padding-top: 12px;
}

.note-item {
  break-inside: avoid;
  margin-bottom: 14px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
}

.note-item:hover {
  transform: translateY(-2px);
}

/* 封面 */
.cover-wrap {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 8px 8px 0 0;
  min-height: 120px;
}

.cover-img {
  width: 100%;
  display: block;
  object-fit: cover;
}

.cover-fallback {
  width: 100%;
  background: linear-gradient(135deg, #ffeaa7, #fdcb6e);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-icon {
  font-size: 36px;
}

/* 内容区 */
.note-content {
  padding: 10px 10px 12px;
}

.note-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
  word-break: break-all;
}

.note-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.author {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.author-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #999;
}

.author-name {
  font-size: 11px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #ff2442;
  flex-shrink: 0;
}

/* ===== 分页 ===== */
.load-more {
  display: flex;
  justify-content: center;
  padding: 24px 0 8px;
}

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .waterfall { column-count: 4; }
}

@media (max-width: 900px) {
  .waterfall { column-count: 3; }
}

@media (max-width: 600px) {
  .waterfall { column-count: 2; column-gap: 10px; }
  .note-item { margin-bottom: 10px; }
  .explore-page { padding: 0 10px 24px; }
  .channel-inner { padding: 6px 10px; }
}
</style>
