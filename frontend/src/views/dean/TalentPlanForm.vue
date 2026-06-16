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

      <!-- 结构化字段编辑（编辑模式可见） -->
      <el-divider v-if="isEdit" content-position="left">培养目标与课程体系</el-divider>
      <el-tabs v-if="isEdit" v-model="structTab" type="border-card" style="margin-top:16px">
        <el-tab-pane label="培养目标" name="targets">
          <el-input v-model="struct.targets" type="textarea" :rows="5" placeholder="请输入培养目标（每行一条）" />
        </el-tab-pane>
        <el-tab-pane label="毕业要求" name="requirements">
          <el-input v-model="struct.requirements" type="textarea" :rows="5" placeholder="请输入毕业要求（每行一条）" />
        </el-tab-pane>
        <el-tab-pane label="课程体系" name="courseSystem">
          <el-input v-model="struct.courseSystem" type="textarea" :rows="6" placeholder="请输入课程体系描述" />
        </el-tab-pane>
        <el-tab-pane label="指标点映射矩阵" name="matrix">
          <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px" title="填写培养目标与课程的对应关系（0-5）" />
          <div v-if="matrixTargets.length===0" style="padding:40px;text-align:center;color:var(--color-text-muted)">
            请先在「培养目标」和「课程体系」标签页中填写内容（每行一条），再回到此处填写映射关系
          </div>
          <div v-else style="overflow-x:auto">
            <table class="matrix-table">
              <thead>
                <tr><th style="min-width:120px">课程 \ 目标</th>
                  <th v-for="(t,i) in matrixTargets" :key="i" style="min-width:100px;font-size:11px">{{ t.substring(0,20) }}</th></tr>
              </thead>
              <tbody>
                <tr v-for="(c,ci) in matrixCourses" :key="ci">
                  <td style="font-weight:500;white-space:nowrap">{{ c.substring(0,20) }}</td>
                  <td v-for="(t,ti) in matrixTargets" :key="ti" align="center">
                    <el-input-number v-model="matrixValues[ci][ti]" :min="0" :max="5" size="small" style="width:60px" @change="onMatrixChange" /></td>
                </tr>
              </tbody>
            </table>
            <div style="margin-top:8px;font-size:11px;color:var(--color-text-muted)">0=无关联 1=弱 3=中 5=强</div>
          </div>
        </el-tab-pane>
      </el-tabs>
      <div v-if="isEdit" style="margin-top:16px;text-align:right">
        <el-button type="primary" :loading="savingStruct" @click="saveStruct">保存结构化字段</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { collegeApi, fileApi } from '@/api/common'
import { talentPlanApi } from '@/api/talentPlan'
import { useAuthStore } from '@/stores/auth'

const route = useRoute(); const router = useRouter(); const authStore = useAuthStore()
const isEdit = ref(!!route.params.id)
const deanCollegeId = authStore.userInfo?.collegeId
const saving = ref(false); const savingStruct = ref(false)
const uploading = ref(false)
const uploadPercent = ref(0)
const colleges = ref<any[]>([])
const fileList = ref<any[]>([])
const parsedText = ref('')
const structTab = ref('targets')

const struct = reactive({ targets: '', requirements: '', courseSystem: '' })
const matrixValues = ref<number[][]>([])

const matrixTargets = computed(() => struct.targets.split('\n').filter(l => l.trim()))
const matrixCourses = computed(() => struct.courseSystem.split('\n').filter(l => l.trim()))

function getMatrix() {
  const rows: number[][] = []
  for (let r = 0; r < matrixCourses.value.length; r++) {
    const row: number[] = []
    for (let c = 0; c < matrixTargets.value.length; c++) {
      row.push((matrixValues.value[r] && matrixValues.value[r][c]) || 0)
    }
    rows.push(row)
  }
  return JSON.stringify(rows)
}

function onMatrixChange() { /* 触发响应更新 */ }

async function saveStruct() {
  savingStruct.value = true
  try {
    await talentPlanApi.updateMapping(Number(route.params.id), {
      targets: struct.targets,
      requirements: struct.requirements,
      courseSystem: struct.courseSystem,
      mappingMatrix: getMatrix()
    })
    ElMessage.success('结构化字段保存成功')
  } catch { ElMessage.error('保存失败') } finally { savingStruct.value = false }
}
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
      // 加载结构化字段
      struct.targets = d.targets || ''
      struct.requirements = d.requirements || ''
      struct.courseSystem = d.courseSystem || ''
      if (d.mappingMatrix) {
        try { const m = JSON.parse(d.mappingMatrix); if (Array.isArray(m)) matrixValues.value = m } catch {}
      }
    } catch {}
  }
})
</script>

<style scoped>
.matrix-table { border-collapse: collapse; width: 100%; font-size: 12px; }
.matrix-table th, .matrix-table td { border: 1px solid var(--el-border-color-light); padding: 6px 8px; }
.matrix-table th { background: var(--el-fill-color-light); font-weight: 600; }
.matrix-table td { text-align: center; }
.matrix-table tr:hover td { background: var(--el-fill-color-lighter); }
</style>
