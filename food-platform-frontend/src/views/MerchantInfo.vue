<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { merchantApi } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const merchant = ref(null)
const editing = ref(false)
const form = ref({ shopName: '', shopDesc: '', logo: '', address: '', category: '' })
const loading = ref(false)

const categories = ['川菜', '粤菜', '湘菜', '鲁菜', '苏菜', '浙菜', '闽菜', '徽菜', '甜品', '小吃', '综合']

const fetchMerchantInfo = async () => {
  loading.value = true
  try {
    const res = await merchantApi.getInfo()
    merchant.value = res.data
    form.value = { ...res.data }
  } catch (e) {
    if (e.response?.status === 404 || e.message?.includes('不存在')) {
      merchant.value = null
    }
  } finally {
    loading.value = false
  }
}

const handleUpdate = async () => {
  loading.value = true
  try {
    await merchantApi.updateInfo(form.value)
    ElMessage.success('更新成功')
    editing.value = false
    await fetchMerchantInfo()
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(fetchMerchantInfo)
</script>

<template>
  <div class="merchant-info" v-loading="loading">
    <el-card v-if="merchant === null && !loading">
      <el-empty description="您还不是商家">
        <el-button type="primary" @click="router.push('/merchant/apply')">申请入驻</el-button>
      </el-empty>
    </el-card>

    <el-card v-else-if="merchant">
      <template #header>
        <div class="header">
          <h2>商家信息</h2>
          <div>
            <el-button type="success" @click="router.push('/coupon/manage')">优惠券管理</el-button>
            <el-button type="warning" @click="router.push('/coupon/verify')">核销优惠券</el-button>
            <el-button v-if="!editing" type="primary" @click="editing = true">编辑</el-button>
          </div>
        </div>
      </template>

      <template v-if="!editing">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="店铺名称">{{ merchant.shopName }}</el-descriptions-item>
          <el-descriptions-item label="主营类目">{{ merchant.category }}</el-descriptions-item>
          <el-descriptions-item label="店铺简介">{{ merchant.shopDesc }}</el-descriptions-item>
          <el-descriptions-item label="店铺地址">{{ merchant.address }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <template v-else>
        <el-form :model="form" label-width="100px">
          <el-form-item label="店铺名称">
            <el-input v-model="form.shopName" />
          </el-form-item>
          <el-form-item label="主营类目">
            <el-select v-model="form.category">
              <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
            </el-select>
          </el-form-item>
          <el-form-item label="店铺简介">
            <el-input v-model="form.shopDesc" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="店铺地址">
            <el-input v-model="form.address" />
          </el-form-item>
          <el-form-item label="Logo">
            <el-input v-model="form.logo" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleUpdate">保存</el-button>
            <el-button @click="editing = false">取消</el-button>
          </el-form-item>
        </el-form>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.merchant-info {
  max-width: 600px;
  margin: 0 auto;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
h2 {
  margin: 0;
}
</style>
