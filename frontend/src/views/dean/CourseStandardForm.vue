<template>
  <div>
    <el-card>
      <template #header><span style="font-weight:bold">{{ isEdit ? '编辑课程标准' : '新增课程标准' }}</span></template>
      <el-form ref="formRef" :model="form" label-width="100px" style="max-width:750px">
        <el-form-item label="关联课程" required>
          <el-select v-model="form.courseId" placeholder="请选择" style="width:100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name+' ('+c.code+')'" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联人培方案">
          <el-select v-model="form.tcpId" clearable placeholder="可选" style="width:100%">
            <el-option v-for="t in tcpList" :key="t.id" :label="t.majorName+' ('+t.grade+')'" :value="t.id" />
          </el-select>
        </el-form-item>

        <!-- 文件上传 -->
        <el-form-item label="标准文档">
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
              <div style="color:#606266">拖拽或<em>点击上传</em>课程标准文档</div>
              <div style="color:#909399;font-size:12px;margin-top:4px">支持 .doc .docx .pdf</div>
            </div>
          </el-upload>
        </el-form-item>

        <el-form-item v-if="parsedText" label="文档内容">
          <el-alert type="success" :closable="false" show-icon style="margin-bottom:8px"
            :title="'解析成功，共 ' + parsedText.length + ' 字符，可作为AI评审参考依据'" />
          <el-input v-model="parsedText" type="textarea" :rows="10" readonly />
        </el-form-item>
        <el-form-item v-if="uploading" label="">
          <el-progress :percentage="uploadPercent" style="width:300px" />
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
import { courseStandardApi, courseApi, fileApi } from '@/api/common'
import { talentPlanApi } from '@/api/talentPlan'

const route = useRoute(); const router = useRouter()
const isEdit = ref(!!route.params.id); const saving = ref(false); const uploading = ref(false)
const uploadPercent = ref(0); const courses = ref<any[]>([]); const tcpList = ref<any[]>([])
const fileList = ref<any[]>([]); const parsedText = ref(''); const fileUrl = ref('')

const form = reactive({ courseId: null as any, tcpId: null as any })

async function onFileChange(file: any, fl: any[]) {
  fileList.value = fl
  if (!file.raw) return
  uploading.value = true; uploadPercent.value = 30
  try {
    const res: any = await fileApi.uploadParse(file.raw, 'COURSE_STANDARD')
    uploadPercent.value = 100
    if (res.code === 200 && res.data) {
      parsedText.value = res.data.parsedText || ''
      fileUrl.value = res.data.fileUrl || ''
      ElMessage.success('文档解析成功')
    } else { ElMessage.error('解析失败') }
  } catch { ElMessage.error('上传失败') }
  finally { uploading.value = false }
}

function onFileRemove() { fileList.value = []; parsedText.value = ''; fileUrl.value = '' }

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data: any = {
      courseId: form.courseId, tcpId: form.tcpId,
      fileUrl: fileUrl.value, contentText: parsedText.value
    }
    if (isEdit.value) { await courseStandardApi.update(Number(route.params.id), data) }
    else { await courseStandardApi.create(data) }
    ElMessage.success('保存成功'); router.push('/dean/course-standards')
  } catch (e: any) { ElMessage.error('操作失败') }
  finally { saving.value = false }
}

const formRef = ref()
onMounted(async () => {
  try { const r: any = await courseApi.list({ page:1,pageSize:500 }); courses.value = r.data?.records||[] } catch {}
  try { const r: any = await talentPlanApi.list({ page:1,pageSize:500 }); tcpList.value = r.data?.records||[] } catch {}
  if (isEdit.value) {
    try {
      const r: any = await courseStandardApi.getById(Number(route.params.id))
      const d = r.data
      Object.assign(form, { courseId: d.courseId, tcpId: d.tcpId })
      if (d.contentText) { parsedText.value = d.contentText }
      if (d.fileUrl) { fileUrl.value = d.fileUrl; fileList.value = [{ name:'已上传文档', url: d.fileUrl }] }
    } catch {}
  }
})
</script>
