<script setup>
import { ref, onMounted } from 'vue'
import { couponApi } from '../api'
import { ElMessage } from 'element-plus'

const coupons = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)
const activeTab = ref('unused')

const statusMap = {
  0: { label: '未使用', type: 'success' },
  1: { label: '已使用', type: 'info' },
  2: { label: '已过期', type: 'danger' }
}

const typeMap = { 1: '满减券', 2: '折扣券', 3: '免费券' }

const fetchCoupons = async () => {
  loading.value = true
  try {
    const res = await couponApi.myCoupons({ current: current.value, size: size.value })
    coupons.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    // handled
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  current.value = page
  fetchCoupons()
}

const getDiscountDesc = (coupon) => {
  if (coupon.type === 1) return `满${coupon.threshold}减${coupon.discount}`
  if (coupon.type === 2) return `${coupon.discount * 10}折`
  return '免费'
}

const copyCode = (code) => {
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success('券码已复制')
  }).catch(() => {
    ElMessage.info('券码：' + code)
  })
}

const filteredCoupons = ref([])

const filterCoupons = () => {
  const statusFilter = activeTab.value === 'unused' ? 0 : activeTab.value === 'used' ? 1 : 2
  filteredCoupons.value = coupons.value.filter(c => c.status === statusFilter)
}

onMounted(async () => {
  await fetchCoupons()
  filterCoupons()
})

const handleTabChange = () => {
  filterCoupons()
}
</script>

<template>
  <div class="my-coupons">
    <h2>我的优惠券</h2>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="未使用" name="unused" />
      <el-tab-pane label="已使用" name="used" />
      <el-tab-pane label="已过期" name="expired" />
    </el-tabs>

    <div v-loading="loading" class="coupon-list">
      <div v-for="coupon in filteredCoupons" :key="coupon.id" class="coupon-card" :class="'status-' + coupon.status">
        <div class="coupon-left">
          <div class="coupon-value">
            <template v-if="coupon.type === 1">¥{{ coupon.discount }}</template>
            <template v-else-if="coupon.type === 2">{{ coupon.discount * 10 }}折</template>
            <template v-else>免费</template>
          </div>
          <div class="coupon-type">{{ typeMap[coupon.type] }}</div>
        </div>
        <div class="coupon-right">
          <h3>{{ coupon.couponTitle }}</h3>
          <p class="desc">{{ getDiscountDesc(coupon) }}</p>
          <p class="expire">有效期至：{{ coupon.endTime?.slice(0, 10) }}</p>
          <div v-if="coupon.status === 0" class="code-row">
            <span class="code">券码：{{ coupon.couponCode }}</span>
            <el-button type="primary" size="small" link @click="copyCode(coupon.couponCode)">复制</el-button>
          </div>
          <div v-if="coupon.status === 1" class="used-info">已于 {{ coupon.usedTime?.slice(0, 16) }} 使用</div>
        </div>
        <div class="coupon-status">
          <el-tag :type="statusMap[coupon.status]?.type" size="small">
            {{ statusMap[coupon.status]?.label }}
          </el-tag>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && filteredCoupons.length === 0" description="暂无优惠券" />

    <div class="pagination">
      <el-pagination v-model:current-page="current" :page-size="size" :total="total"
        layout="prev, pager, next" @current-change="handlePageChange" />
    </div>
  </div>
</template>

<style scoped>
.my-coupons { padding: 20px 0; }
h2 { margin-bottom: 16px; }
.coupon-list { display: flex; flex-direction: column; gap: 12px; }
.coupon-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.coupon-card.status-1, .coupon-card.status-2 { opacity: 0.6; }
.coupon-left {
  background: linear-gradient(135deg, #f56c6c, #e6a23c);
  color: #fff;
  padding: 20px;
  text-align: center;
  min-width: 100px;
}
.coupon-value { font-size: 28px; font-weight: bold; }
.coupon-type { font-size: 12px; margin-top: 4px; opacity: 0.9; }
.coupon-right { flex: 1; padding: 16px 20px; }
.coupon-right h3 { margin: 0 0 8px; font-size: 16px; }
.desc { color: #606266; margin: 0 0 4px; font-size: 14px; }
.expire { color: #909399; font-size: 13px; margin: 0 0 8px; }
.code-row { display: flex; align-items: center; gap: 8px; }
.code { font-family: monospace; font-size: 14px; color: #409eff; background: #ecf5ff; padding: 2px 8px; border-radius: 4px; }
.used-info { color: #909399; font-size: 13px; }
.coupon-status { padding: 16px; }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
</style>
