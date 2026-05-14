<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { articleApi, couponApi } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const form = ref({ title: '', content: '', category: '', coverImage: '' })
const loading = ref(false)

// Coupon form - inline creation
const addCoupon = ref(false)
const couponForm = ref({
  title: '',
  description: '',
  type: 1,
  threshold: null,
  discount: null,
  originalPrice: 0,
  totalCount: 100,
  limitPerUser: 1,
  startTime: '',
  endTime: ''
})

const categories = ['川菜', '粤菜', '湘菜', '鲁菜', '苏菜', '浙菜', '闽菜', '徽菜', '甜品', '小吃']

const typeOptions = [
  { value: 1, label: '满减券', desc: '满X减Y' },
  { value: 2, label: '折扣券', desc: 'X折优惠' },
  { value: 3, label: '免费券', desc: '免费领取' }
]

const handlePublish = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  if (addCoupon.value) {
    if (!couponForm.value.title || !couponForm.value.discount || !couponForm.value.totalCount) {
      ElMessage.warning('请填写完整的优惠券信息')
      return
    }
    if (!couponForm.value.startTime || !couponForm.value.endTime) {
      ElMessage.warning('请设置优惠券有效期')
      return
    }
  }

  loading.value = true
  try {
    // 1. Publish article first
    const articleRes = await articleApi.publish(form.value)
    const articleId = articleRes.data

    // 2. Create coupon with articleId, then update article with couponId
    if (addCoupon.value && articleId) {
      const couponRes = await couponApi.create({
        ...couponForm.value,
        articleId
      })
      const couponId = couponRes.data
      if (couponId) {
        await articleApi.update(articleId, { couponId })
      }
    }

    ElMessage.success('发布成功')
    router.push('/articles')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="publish-container">
    <el-card>
      <template #header>
        <h2>发布文章</h2>
      </template>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入文章标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择分类">
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图">
          <el-input v-model="form.coverImage" placeholder="封面图片URL（可选）" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="请输入文章内容" />
        </el-form-item>

        <!-- Coupon Section -->
        <el-divider content-position="left">附带优惠券（可选）</el-divider>
        <el-form-item>
          <el-switch v-model="addCoupon" active-text="附带优惠券" inactive-text="不附带" />
        </el-form-item>

        <template v-if="addCoupon">
          <el-form-item label="券名称">
            <el-input v-model="couponForm.title" placeholder="如：新店开业特惠" />
          </el-form-item>
          <el-form-item label="券类型">
            <el-radio-group v-model="couponForm.type">
              <el-radio v-for="t in typeOptions" :key="t.value" :value="t.value">{{ t.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="couponForm.type === 1" label="满减门槛">
            <el-input-number v-model="couponForm.threshold" :min="0" :precision="2" placeholder="满多少可用" />
          </el-form-item>
          <el-form-item :label="couponForm.type === 2 ? '折扣率' : '减免金额'">
            <el-input-number v-model="couponForm.discount" :min="0" :precision="2"
              :step="couponForm.type === 2 ? 0.1 : 10"
              :placeholder="couponForm.type === 2 ? '如0.8代表八折' : '减免金额'" />
          </el-form-item>
          <el-form-item label="购买价格">
            <el-input-number v-model="couponForm.originalPrice" :min="0" :precision="2" :step="1" />
            <span class="form-hint">0 = 免费领取</span>
          </el-form-item>
          <el-form-item label="发行数量">
            <el-input-number v-model="couponForm.totalCount" :min="1" :step="10" />
          </el-form-item>
          <el-form-item label="每人限领">
            <el-input-number v-model="couponForm.limitPerUser" :min="1" :max="10" />
            <span class="form-hint">每用户最多可领/购数量</span>
          </el-form-item>
          <el-form-item label="有效期">
            <el-date-picker v-model="couponForm.startTime" type="datetime" placeholder="开始时间"
              value-format="YYYY-MM-DDTHH:mm:ss" style="width: 45%" />
            <span style="margin: 0 8px">至</span>
            <el-date-picker v-model="couponForm.endTime" type="datetime" placeholder="结束时间"
              value-format="YYYY-MM-DDTHH:mm:ss" style="width: 45%" />
          </el-form-item>
          <el-form-item label="券描述">
            <el-input v-model="couponForm.description" type="textarea" :rows="2" placeholder="券描述（选填）" />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handlePublish">发布</el-button>
          <el-button @click="router.push('/articles')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.publish-container {
  max-width: 800px;
  margin: 0 auto;
}
h2 {
  margin: 0;
}
.form-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 13px;
}
</style>
