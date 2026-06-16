<template>
  <div>
    <el-card>
      <template #header>
        <span style="font-weight:bold;font-size:16px">提交教学材料</span>
      </template>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom:20px">
        <template #title>提交说明</template>
        请选择材料类型并上传对应文件，提交后系统将自动进行AI评审。支持 Word/Pdf/图片，单文件≤20MB，最多10个。
      </el-alert>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width:700px">
        <el-form-item label="材料类型" prop="materialType">
          <el-select v-model="form.materialType" placeholder="请选择">
            <el-option label="授课计划" value="TEACHING_PLAN" />
            <el-option label="教案" value="LESSON_PLAN" />
            <el-option label="课件" value="COURSEWARE" />
            <el-option label="考核方案" value="EXAM_PLAN" />
          </el-select>
        </el-form-item>

        <el-form-item label="文件上传">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :file-list="uploadFiles"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            multiple
            drag
            accept=".doc,.docx,.pdf,.jpg,.jpeg,.png"
          >
            <div style="padding:30px 0">
              <div style="font-size:48px;margin-bottom:8px">📁</div>
              <div style="color:#606266">拖拽文件或<em>点击选择</em></div>
            </div>
          </el-upload>
          <div style="margin-top:8px" v-if="uploadFiles.length">
            <el-tag v-for="(f,i) in uploadFiles" :key="i" closable @close="removeFile(i)" style="margin:2px 4px" :type="f.status==='done'?'success':f.status==='uploading'?'warning':'info'">
              {{ f.name }} {{ f.status==='uploading'?'上传中...':f.status==='done'?'✓':'' }}
            </el-tag>
          </div>
        </el-form-item>

        <el-form-item label="关联课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="选择课程" style="width:100%" filterable>
            <el-option v-for="c in courses" :key="c.id" :label="c.name+' ('+c.code+')'" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="授课学期">
          <el-select v-model="form.semesterId" placeholder="选择学期" style="width:100%">
            <el-option v-for="s in semesters" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <div style="font-size:11px;color:var(--color-text-muted);margin-top:4px">默认使用当前学期</div>
        </el-form-item>

        <el-form-item label="材料描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选，简要描述" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="doSubmit" style="width:100%" :disabled="uploading">
            {{ uploading ? '文件上传中...' : submitting ? '提交中...' : '确认提交' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { phaseMaterialApi, courseApi, fileApi, semesterApi } from '@/api/common'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const formRef = ref()
const uploadRef = ref()
const submitting = ref(false)
const uploading = ref(false)
const courses = ref<any[]>([])
const semesters = ref<any[]>([])
const uploadFiles = ref<any[]>([])

const form = reactive({
  materialType: '',
  courseId: null as number | null,
  semesterId: null as number | null,
  description: ''
})
const rules = {
  materialType: [{ required: true, message: '请选择材料类型', trigger: 'change' }],
  courseId: [{ required: true, message: '请选择关联课程', trigger: 'change' }]
}

function onFileChange(_file: any, fileList: any[]) {
  uploadFiles.value = fileList
}

function onFileRemove(_file: any, fileList: any[]) {
  uploadFiles.value = fileList
}

function removeFile(idx: number) {
  uploadFiles.value.splice(idx, 1)
}

async function doSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (uploadFiles.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }

  // 1. 上传所有文件
  uploading.value = true
  const fileIds: number[] = []
  for (const f of uploadFiles.value) {
    if (!f.raw) continue
    try {
      const res: any = await fileApi.upload(f.raw, 'MATERIAL', form.materialType)
      if (res.code === 200 && res.data?.fileId) {
        fileIds.push(res.data.fileId)
        f.status = 'done'
      } else {
        ElMessage.error('文件 ' + f.name + ' 上传失败')
        uploading.value = false
        return
      }
    } catch {
      ElMessage.error('文件 ' + f.name + ' 上传失败')
      uploading.value = false
      return
    }
  }
  uploading.value = false

  if (fileIds.length === 0) {
    ElMessage.error('没有文件上传成功')
    return
  }

  // 2. 创建材料 + 3. 关联文件(自动触发AI评审)
  submitting.value = true
  try {
    let successCount = 0
    for (let i = 0; i < fileIds.length; i++) {
      const fileName = uploadFiles.value[i]?.name || ('文件'+(i+1))
      const matRes: any = await phaseMaterialApi.create({
        materialType: form.materialType,
        courseId: form.courseId,
        semesterId: form.semesterId,
        description: form.description ? form.description + ' - ' + fileName : fileName
      })
      const materialId = matRes.data?.id
      if (!materialId) continue
      await phaseMaterialApi.addFiles(materialId, [fileIds[i]])
      successCount++
    }

    ElMessage.success('提交成功，共' + successCount + '份材料，待审核')
    form.materialType = ''; form.courseId = null; form.semesterId = null as any; form.description = ''
    uploadFiles.value = []
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try { const res: any = await courseApi.list({ page:1, pageSize:500, collegeId: authStore.userInfo?.collegeId }); courses.value = res.data?.records || [] } catch {}
  try { const sres: any = await semesterApi.list(); semesters.value = sres.data || []; const active = semesters.value.find((s:any)=>s.isActive===1); if (active) form.semesterId = active.id } catch {}
})
</script>
