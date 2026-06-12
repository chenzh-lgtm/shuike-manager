<template>
  <div>
    <el-card>
      <template #header><span>{{ isEdit ? '编辑授课计划' : '提交授课计划' }}</span></template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width:800px">
        <el-form-item label="课程" prop="courseId"><el-select v-model="form.courseId" placeholder="请选择课程" style="width:100%"><el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="授课学期" prop="semesterId"><el-select v-model="form.semesterId" placeholder="请选择学期" style="width:100%"><el-option v-for="s in semesters" :key="s.id" :label="s.name" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="授课班级"><el-input v-model="form.classInfo" placeholder="如：数学2024-1班, 数学2024-2班" /></el-form-item>
        <el-form-item label="教材信息"><el-input v-model="form.textbookInfo" placeholder="教材名称、出版社等" /></el-form-item>
        <el-form-item label="计划摘要"><el-input v-model="form.summary" type="textarea" :rows="4" placeholder="请输入授课计划摘要" /></el-form-item>
        <el-form-item label="附件上传">
          <el-upload :auto-upload="false" :on-change="handleFileChange" :file-list="fileList" multiple>
            <el-button type="primary">选择文件</el-button>
            <template #tip><div class="el-upload__tip">支持 .doc .docx .pdf，单文件 ≤ 20MB</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button @click="$router.back()">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave(false)">保存草稿</el-button>
          <el-button type="success" :loading="saving" @click="handleSave(true)">提交审核</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { teachingPlanApi } from '@/api/teachingPlan'
import { courseApi, semesterApi, fileApi } from '@/api/common'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const formRef = ref()
const saving = ref(false)
const courses = ref<any[]>([])
const semesters = ref<any[]>([])
const fileList = ref<any[]>([])
const fileIds = ref<number[]>([])

const form = reactive({
  courseId: null as any,
  semesterId: null as any,
  classInfo: '',
  textbookInfo: '',
  summary: ''
})

const rules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  semesterId: [{ required: true, message: '请选择学期', trigger: 'change' }]
}

async function handleFileChange(file: any) {
  try {
    const res: any = await fileApi.upload(file.raw)
    if (res.data?.fileId) fileIds.value.push(res.data.fileId)
  } catch { ElMessage.error('文件上传失败') }
}

async function handleSave(submit: boolean) {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data: any = { ...form }
    if (isEdit) {
      await teachingPlanApi.update(Number(route.params.id), data)
      ElMessage.success('修改成功')
    } else {
      const res: any = await teachingPlanApi.create(data)
      if (fileIds.value.length > 0 && res.data?.id) {
        await teachingPlanApi.addFiles(res.data.id, fileIds.value)
      }
      ElMessage.success('创建成功')
      if (submit && res.data?.id) await teachingPlanApi.submit(res.data.id)
    }
    router.push('/teacher/teaching-plans')
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
  finally { saving.value = false }
}

onMounted(async () => {
  try { const res: any = await courseApi.list({ page: 1, pageSize: 100 }); courses.value = res.data?.records || [] } catch {}
  try { const res: any = await semesterApi.list(); semesters.value = res.data || [] } catch {}
  if (isEdit) {
    try {
      const res: any = await teachingPlanApi.getById(Number(route.params.id))
      Object.assign(form, { courseId: res.data.courseId, semesterId: res.data.semesterId, classInfo: res.data.classInfo, textbookInfo: res.data.textbookInfo, summary: res.data.summary })
    } catch {}
  }
})

import { computed } from 'vue'
</script>
