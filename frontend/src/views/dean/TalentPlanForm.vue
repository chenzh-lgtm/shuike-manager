<template>
  <div>
    <el-card>
      <template #header><span style="font-weight:bold">{{ isEdit ? '编辑人培方案' : '新增人培方案' }}</span></template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width:750px">
        <el-form-item label="专业名称" prop="majorName">
          <el-input v-model="form.majorName" placeholder="如: 数学与应用数学" />
        </el-form-item>
        <el-form-item label="专业代码">
          <el-input v-model="form.majorCode" placeholder="如: 070101" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input v-model="form.grade" placeholder="如: 2024" />
        </el-form-item>
        <el-form-item label="所属学院" prop="collegeId">
          <el-select v-model="form.collegeId" placeholder="请选择" style="width:100%" disabled>
            <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <div style="font-size:11px;color:var(--color-text-muted);margin-top:4px">默认使用您所属的学院</div>
        </el-form-item>

        <!-- 文件上传 -->
        <el-form-item label="方案文档">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :file-list="fileList"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            :limit="1"
            drag
            accept=".doc,.docx,.pdf"
          >
            <div style="padding:30px 0">
              <div style="font-size:48px;margin-bottom:8px">📄</div>
              <div style="color:#606266">拖拽或<em>点击上传</em>人培方案文档</div>
              <div style="color:#909399;font-size:12px;margin-top:4px">支持 .doc .docx .pdf</div>
            </div>
          </el-upload>
        </el-form-item>

        <!-- 解析预览 -->
        <el-form-item v-if="parsedText" label="文档内容">
          <el-alert type="success" :closable="false" show-icon style="margin-bottom:8px"
            :title="'解析成功，共 ' + parsedText.length + ' 字符'" />
          <el-input v-model="parsedText" type="textarea" :rows="10" readonly
            placeholder="文档内容将自动解析显示在此" />
        </el-form-item>
        <el-form-item v-if="uploading" label="">
          <el-progress :percentage="uploadPercent" :status="uploadPercent===100?'success':''" />
          <span style="color:#409EFF;margin-left:8px">正在上传并解析文档...</span>
        </el-form-item>

        <el-form-item>
          <el-button @click="$router.back()">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { collegeApi, fileApi } from '@/api/common'
import { talentPlanApi } from '@/api/talentPlan'
import { useAuthStore } from '@/stores/auth'

const route = useRoute(); const router = useRouter(); const authStore = useAuthStore()
const isEdit = ref(!!route.params.id)
const deanCollegeId = authStore.userInfo?.collegeId  // 院长的所属学院
const saving = ref(false)
const uploading = ref(false)
const uploadPercent = ref(0)
const colleges = ref<any[]>([])
const fileList = ref<any[]>([])
const parsedText = ref('')
const fileUrl = ref('')

const form = reactive({
  majorName: '', majorCode: '', grade: '', collegeId: null as any
})
const rules = {
  majorName: [{ required: true, message: '请输入专业名称', trigger: 'blur' }],
  grade: [{ required: true, message: '请输入年级', trigger: 'blur' }],
  collegeId: [{ required: true, message: '请选择学院', trigger: 'change' }]
}

async function onFileChange(file: any, fl: any[]) {
  fileList.value = fl
  if (!file.raw) return
  uploading.value = true
  uploadPercent.value = 30
  try {
    const res: any = await fileApi.uploadParse(file.raw, 'TALENT_PLAN')
    uploadPercent.value = 100
    if (res.code === 200 && res.data) {
      parsedText.value = res.data.parsedText || ''
      fileUrl.value = res.data.fileUrl || ''
      ElMessage.success('文档解析成功，共 ' + (parsedText.value.length) + ' 字符')
    } else {
      ElMessage.error('文档解析失败')
    }
  } catch {
    ElMessage.error('上传解析失败')
  } finally { uploading.value = false }
}

function onFileRemove() { fileList.value = []; parsedText.value = ''; fileUrl.value = '' }

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data: any = {
      ...form,
      fileUrl: fileUrl.value,
      contentText: parsedText.value
    }
    if (isEdit.value) {
      await talentPlanApi.update(Number(route.params.id), data)
    } else {
      await talentPlanApi.create(data)
    }
    ElMessage.success('保存成功'); router.push('/dean/talent-plans')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally { saving.value = false }
}

const formRef = ref()
onMounted(async () => {
  // 加载学院列表并默认选中院长的学院
  try { const r: any = await collegeApi.list(); colleges.value = r.data || [] } catch {}
  if (!isEdit.value) {
    form.collegeId = deanCollegeId  // 新增时默认院长的学院
  }
  if (isEdit.value) {
    try {
      const r: any = await talentPlanApi.getById(Number(route.params.id))
      const d = r.data
      Object.assign(form, { majorName: d.majorName, majorCode: d.majorCode, grade: d.grade, collegeId: d.collegeId })
      if (d.contentText) { parsedText.value = d.contentText }
      if (d.fileUrl) { fileUrl.value = d.fileUrl; fileList.value = [{ name: '已上传文档', url: d.fileUrl }] }
    } catch {}
  }
})
</script>
