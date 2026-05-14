<script setup>
import { ref, onMounted } from 'vue'
import { couponApi } from '../api'
import { ElMessage } from 'element-plus'

const coupons = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const loading = ref(false)

const typeOptions = [
  { value: 1, label: '满减券' },
  { value: 2, label: '折扣券' },
  { value: 3, label: '免费券' }
]

const fetchCoupons = async () => {
  loading.value = true
  try {
    const res = await couponApi.list({ current: current.value, size: size.value })
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

const toggleStatus = async (coupon) => {
  const newStatus = coupon.status === 1 ? 0 : 1
  try {
    await couponApi.updateStatus(coupon.id, newStatus)
    ElMessage.success(newStatus === 1 ? '已上架' : '已下架')
    fetchCoupons()
  } catch (e) {
    // handled
  }
}

const getTypeLabel = (type) => typeOptions.find(t => t.value === type)?.label || ''
const getTypeDesc = (coupon) => {
  if (coupon.type === 1) return `满${coupon.threshold}减${coupon.discount}`
  if (coupon.type === 2) return `${coupon.discount * 10}折`
  return '免费领取'
}

onMounted(fetchCoupons)
</script>

<template>
  <div class="coupon-manage">
    <div class="header">
      <h2>优惠券管理</h2>
      <el-text type="info" size="small">优惠券需在发布文章时创建</el-text>
    </div>

    <el-table :data="coupons" v-loading="loading" style="width: 100%">
      <el-table-column prop="title" label="券名称" min-width="150" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 1 ? 'danger' : row.type === 2 ? 'warning' : 'success'" size="small">
            {{ getTypeLabel(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优惠内容" width="130">
        <template #default="{ row }">{{ getTypeDesc(row) }}</template>
      </el-table-column>
      <el-table-column label="价格" width="80">
        <template #default="{ row }">{{ row.originalPrice > 0 ? `¥${row.originalPrice}` : '免费' }}</template>
      </el-table-column>
      <el-table-column label="库存" width="120">
        <template #default="{ row }">{{ row.claimedCount }}/{{ row.totalCount }}</template>
      </el-table-column>
      <el-table-column label="限领" width="80">
        <template #default="{ row }">{{ row.limitPerUser || 1 }}张/人</template>
      </el-table-column>
      <el-table-column label="有效期" width="180">
        <template #default="{ row }">{{ row.startTime?.slice(0, 10) }} ~ {{ row.endTime?.slice(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button :type="row.status === 1 ? 'warning' : 'success'" link @click="toggleStatus(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination v-model:current-page="current" :page-size="size" :total="total"
        layout="prev, pager, next" @current-change="handlePageChange" />
    </div>
  </div>
</template>

<style scoped>
.coupon-manage { padding: 20px 0; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header h2 { margin: 0; }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
</style>
