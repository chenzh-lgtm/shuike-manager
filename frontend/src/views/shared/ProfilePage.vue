<template>
  <div>
    <el-card>
      <template #header><span>个人中心</span></template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ authStore.userInfo?.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ authStore.userInfo?.realName }}</el-descriptions-item>
        <el-descriptions-item label="所属学院">{{ authStore.userInfo?.collegeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ roleNames }}</el-descriptions-item>
      </el-descriptions>
      <el-divider />
      <h4 style="margin-bottom:12px">修改密码</h4>
      <el-form :model="pwdForm" ref="pwdFormRef" label-width="100px" style="max-width:400px">
        <el-form-item label="原密码" required><el-input v-model="pwdForm.oldPassword" type="password" /></el-form-item>
        <el-form-item label="新密码" required><el-input v-model="pwdForm.newPassword" type="password" /></el-form-item>
        <el-form-item label="确认密码" required><el-input v-model="pwdForm.confirmPassword" type="password" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="saving" @click="handleChangePwd">修改密码</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const authStore = useAuthStore()
const saving = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const roleLabels: Record<string,string> = { TEACHER:'教师', COLLEGE_REVIEWER:'专业主任', OFFICE:'教务处', DEAN:'院长' }
const roleNames = computed(() => authStore.roles.map((r:string) => roleLabels[r]||r).join('，'))

async function handleChangePwd() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) { ElMessage.warning('请填写密码'); return }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  saving.value = true
  try { await authApi.changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword }); ElMessage.success('密码修改成功，请重新登录'); authStore.logout(); window.location.href = '/login' }
  catch (e: any) { ElMessage.error(e?.response?.data?.message || '修改失败') }
  finally { saving.value = false }
}
</script>
