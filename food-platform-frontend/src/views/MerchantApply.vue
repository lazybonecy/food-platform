<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { merchantApi } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const form = ref({ shopName: '', shopDesc: '', logo: '', address: '', category: '' })
const loading = ref(false)

const categories = ['川菜', '粤菜', '湘菜', '鲁菜', '苏菜', '浙菜', '闽菜', '徽菜', '甜品', '小吃', '综合']

const handleApply = async () => {
  if (!form.value.shopName) {
    ElMessage.warning('请填写店铺名称')
    return
  }
  loading.value = true
  try {
    const res = await merchantApi.apply(form.value)
    // Save new tokens with merchant role
    if (res.data?.accessToken) {
      localStorage.setItem('accessToken', res.data.accessToken)
      localStorage.setItem('refreshToken', res.data.refreshToken)
    }
    ElMessage.success('申请成功，已升级为商家')
    router.push('/merchant/info')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="apply-container">
    <el-card>
      <template #header>
        <h2>商家入驻申请</h2>
      </template>
      <el-form :model="form" label-width="100px">
        <el-form-item label="店铺名称">
          <el-input v-model="form.shopName" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="主营类目">
          <el-select v-model="form.category" placeholder="请选择主营类目">
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="店铺简介">
          <el-input v-model="form.shopDesc" type="textarea" :rows="3" placeholder="请输入店铺简介" />
        </el-form-item>
        <el-form-item label="店铺地址">
          <el-input v-model="form.address" placeholder="请输入店铺地址" />
        </el-form-item>
        <el-form-item label="Logo">
          <el-input v-model="form.logo" placeholder="Logo图片URL（可选）" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleApply">提交申请</el-button>
          <el-button @click="router.push('/')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.apply-container {
  max-width: 600px;
  margin: 0 auto;
}
h2 {
  margin: 0;
}
</style>
