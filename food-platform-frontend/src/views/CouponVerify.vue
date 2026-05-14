<script setup>
import { ref } from 'vue'
import { couponApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const couponCode = ref('')
const couponInfo = ref(null)
const loading = ref(false)

const typeMap = { 1: '满减券', 2: '折扣券', 3: '免费券' }

const handleVerify = async () => {
  if (!couponCode.value.trim()) {
    ElMessage.warning('请输入券码')
    return
  }
  loading.value = true
  try {
    const res = await couponApi.verify(couponCode.value.trim())
    couponInfo.value = res.data
  } catch (e) {
    couponInfo.value = null
  } finally {
    loading.value = false
  }
}

const handleConfirm = async () => {
  try {
    await ElMessageBox.confirm('确认核销此优惠券？', '确认操作')
    await couponApi.confirmVerify(couponCode.value.trim())
    ElMessage.success('核销成功')
    couponInfo.value = null
    couponCode.value = ''
  } catch (e) {
    // cancelled or error
  }
}

const getDiscountDesc = (info) => {
  if (!info) return ''
  if (info.type === 1) return `满${info.threshold}减${info.discount}`
  if (info.type === 2) return `${info.discount * 10}折`
  return '免费'
}
</script>

<template>
  <div class="coupon-verify">
    <h2>核销优惠券</h2>

    <el-card class="verify-card">
      <div class="input-row">
        <el-input
          v-model="couponCode"
          placeholder="请输入券码"
          size="large"
          clearable
          @keyup.enter="handleVerify"
          style="max-width: 400px"
        />
        <el-button type="primary" size="large" :loading="loading" @click="handleVerify">查询</el-button>
      </div>

      <div v-if="couponInfo" class="result">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="券码">{{ couponInfo.couponCode }}</el-descriptions-item>
          <el-descriptions-item label="券名称">{{ couponInfo.couponTitle }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ typeMap[couponInfo.type] }}</el-descriptions-item>
          <el-descriptions-item label="优惠内容">{{ getDiscountDesc(couponInfo) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="couponInfo.status === 0 ? 'success' : couponInfo.status === 1 ? 'info' : 'danger'">
              {{ couponInfo.statusDesc }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="couponInfo.usedTime" label="使用时间">
            {{ couponInfo.usedTime }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="actions">
          <el-button
            v-if="couponInfo.status === 0"
            type="success"
            size="large"
            @click="handleConfirm"
          >
            确认核销
          </el-button>
          <el-button v-else disabled size="large">
            {{ couponInfo.status === 1 ? '已核销' : '已过期' }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.coupon-verify { padding: 20px 0; max-width: 600px; margin: 0 auto; }
h2 { margin-bottom: 20px; }
.input-row { display: flex; gap: 12px; }
.result { margin-top: 24px; }
.actions { margin-top: 20px; text-align: center; }
</style>
