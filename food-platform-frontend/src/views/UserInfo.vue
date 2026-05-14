<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '../api'
import { ElMessage } from 'element-plus'

const user = ref(null)
const editing = ref(false)
const form = ref({ nickname: '', avatar: '', phone: '', email: '' })
const loading = ref(false)

const fetchUserInfo = async () => {
  loading.value = true
  try {
    const res = await userApi.getInfo()
    user.value = res.data
    form.value = { nickname: res.data.nickname, avatar: res.data.avatar, phone: res.data.phone, email: res.data.email }
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleUpdate = async () => {
  loading.value = true
  try {
    await userApi.updateInfo(form.value)
    ElMessage.success('更新成功')
    editing.value = false
    await fetchUserInfo()
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(fetchUserInfo)
</script>

<template>
  <div class="user-info" v-loading="loading">
    <el-card v-if="user">
      <template #header>
        <div class="header">
          <h2>个人中心</h2>
          <el-button v-if="!editing" type="primary" @click="editing = true">编辑</el-button>
        </div>
      </template>

      <template v-if="!editing">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ user.nickname }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ user.role === 1 ? '商家' : '学生' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ user.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ user.createTime }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <template v-else>
        <el-form :model="form" label-width="80px">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" />
          </el-form-item>
          <el-form-item label="头像">
            <el-input v-model="form.avatar" placeholder="头像URL" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" />
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
.user-info {
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
