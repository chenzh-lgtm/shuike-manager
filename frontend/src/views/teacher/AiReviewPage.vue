<template>
  <div>
    <!-- 新评审提交区 -->
    <el-card style="margin-bottom:20px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>提交 AI 材料评审</span>
          <el-tag type="info">AI 将对教学材料进行四维度评审：内容完整性、课程标准匹配度、格式规范性、创新性</el-tag>
        </div>
      </template>
      <el-steps :active="submitStep" finish-status="success" align-center style="margin-bottom:20px">
        <el-step title="选择材料类型" />
        <el-step title="上传文件" />
        <el-step title="关联课程" />
        <el-step title="提交评审" />
      </el-steps>
      <el-form label-width="110px" style="max-width:700px">
        <el-form-item label="材料类型" required>
          <el-select v-model="materialType" placeholder="请选择材料类型" style="width:100%">
            <el-option label="授课计划" value="TEACHING_PLAN"><span>授课计划</span><span style="float:right;color:#8492a6;font-size:13px">学期教学安排</span></el-option>
            <el-option label="教案" value="LESSON_PLAN"><span>教案</span><span style="float:right;color:#8492a6;font-size:13px">课堂教学设计</span></el-option>
            <el-option label="课件" value="COURSEWARE"><span>课件</span><span style="float:right;color:#8492a6;font-size:13px">教学PPT等</span></el-option>
            <el-option label="考核方案" value="EXAM_PLAN"><span>考核方案</span><span style="float:right;color:#8492a6;font-size:13px">考试/考查方案</span></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="上传材料">
          <el-upload drag :auto-upload="false" :on-change="handleFileChange" :file-list="uploadFiles" multiple accept=".doc,.docx,.pdf,.jpg,.png">
            <div style="padding:20px 0">
              <div style="font-size:40px;margin-bottom:8px">📁</div>
              <div style="color:#606266">拖拽文件到此处，或<em>点击上传</em></div>
              <div class="el-upload__tip" style="margin-top:8px">支持 .doc .docx .pdf .jpg .png，单文件 ≤ 20MB，最多 10 个文件</div>
            </div>
          </el-upload>
          <div style="margin-top:8px" v-if="uploadFiles.length">
            <el-tag v-for="(f,i) in uploadFiles" :key="i" closable @close="removeFile(i)" style="margin-right:8px;margin-bottom:4px" type="info">{{ f.name }} ({{ formatSize(f.size) }})</el-tag>
          </div>
        </el-form-item>
        <el-form-item label="关联课程" required>
          <el-select v-model="courseId" placeholder="请选择关联课程（用于匹配课程标准）" style="width:100%" filterable>
            <el-option v-for="c in courses" :key="c.id" :label="c.name + ' (' + c.code + ')'" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="材料描述">
          <el-input v-model="description" type="textarea" :rows="2" placeholder="可选，简要描述材料内容" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit" style="width:100%">
            {{ submitting ? '正在提交...' : '🚀 开始 AI 评审' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 评审历史 -->
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>评审历史（{{ total }} 条）</span>
          <el-button text type="primary" @click="fetchHistory">刷新</el-button>
        </div>
      </template>
      <el-table :data="historyData" v-loading="loading" style="width:100%" :default-sort="{prop:'createdAt',order:'descending'}">
        <el-table-column prop="id" label="编号" width="70" />
        <el-table-column label="综合评分" width="120" align="center">
          <template #default="{ row }">
            <template v-if="row.status==='COMPLETED'">
              <el-progress type="dashboard" :percentage="row.score||0" :width="50" :color="scoreColor(row.score)">
                <template #default="{ percentage }"><span style="font-size:14px;font-weight:bold">{{ percentage }}</span></template>
              </el-progress>
            </template>
            <template v-else-if="row.status==='EVALUATING'||row.status==='PENDING'">
              <el-tag type="warning" size="small"><el-icon class="is-loading"><Loading /></el-icon> 评审中</el-tag>
            </template>
            <template v-else>
              <el-tag type="danger" size="small">失败</el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="评审状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="复核状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.reviewAction==='CONFIRM'" type="success" size="small">已确认</el-tag>
            <el-tag v-else-if="row.reviewAction==='REJECT'" type="danger" size="small">已驳回</el-tag>
            <el-tag v-else type="info" size="small">待复核</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="evalTime" label="评审时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status==='COMPLETED'" text size="small" type="primary" @click="$router.push('/teacher/ai-review/'+row.id)">查看详情</el-button>
            <el-button v-else-if="row.status==='FAILED'" text size="small" type="warning" @click="handleRetry(row)">重新评审</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;display:flex;justify-content:center">
        <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="fetchHistory" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { aiApi } from '@/api/aiEvaluation'
import { phaseMaterialApi, courseApi, fileApi } from '@/api/common'
import type { UploadFile } from 'element-plus'

const materialType = ref('TEACHING_PLAN')
const courseId = ref<number | null>(null)
const description = ref('')
const submitting = ref(false)
const loading = ref(false)
const courses = ref<any[]>([])
const historyData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const uploadFiles = ref<UploadFile[]>([])
const materialFileIds = ref<number[]>([])
const submitStep = ref(0)

const canSubmit = computed(() => materialType.value && courseId.value)

function scoreColor(score: number) {
  if (score >= 90) return '#67C23A'
  if (score >= 75) return '#409EFF'
  if (score >= 60) return '#E6A23C'
  return '#F56C6C'
}

function statusTagType(status: string) {
  const m: any = { PENDING: 'info', EVALUATING: 'warning', COMPLETED: 'success', FAILED: 'danger' }
  return m[status] || 'info'
}

function statusLabel(status: string) {
  const m: any = { PENDING: '排队中', EVALUATING: 'AI评审中', COMPLETED: '已完成', FAILED: '失败' }
  return m[status] || status
}

function formatSize(bytes: number) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function removeFile(index: number) {
  uploadFiles.value.splice(index, 1)
  materialFileIds.value.splice(index, 1)
}

async function handleFileChange(file: UploadFile) {
  if (!file.raw) return
  uploadFiles.value.push(file)
  try {
    const res: any = await fileApi.upload(file.raw, 'MATERIAL')
    if (res.data?.fileId) materialFileIds.value.push(res.data.fileId)
    submitStep.value = 2
  } catch { ElMessage.error('文件 ' + file.name + ' 上传失败') }
}

async function handleSubmit() {
  if (!canSubmit.value) { ElMessage.warning('请完善必填信息'); return }
  submitting.value = true; submitStep.value = 4
  try {
    const matRes: any = await phaseMaterialApi.create({
      materialType: materialType.value,
      courseId: courseId.value,
      description: description.value
    })
    const materialId = matRes.data?.id
    if (materialFileIds.value.length > 0 && materialId) {
      await phaseMaterialApi.addFiles(materialId, materialFileIds.value)
    }
    await aiApi.submit(materialId)
    ElMessage.success('AI 评审已提交，正在调用大模型分析中，请稍候查看结果')
    uploadFiles.value = []; materialFileIds.value = []; description.value = ''; submitStep.value = 0
    fetchHistory()
    // 15秒后自动刷新一次，获取评审结果
    setTimeout(() => fetchHistory(), 15000)
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '提交失败') }
  finally { submitting.value = false }
}

async function handleRetry(row: any) {
  try { await aiApi.submit(row.materialId); ElMessage.success('已重新提交评审'); fetchHistory() }
  catch (e: any) { ElMessage.error('重试失败') }
}

async function fetchHistory() {
  loading.value = true
  try {
    const res: any = await aiApi.myList({ page: page.value, pageSize: 10 })
    historyData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {} finally { loading.value = false }
}

onMounted(async () => {
  try { const res: any = await courseApi.list({ page: 1, pageSize: 100 }); courses.value = res.data?.records || [] } catch {}
  fetchHistory()
})
</script>

<style scoped>
.el-upload-dragger { width: 100%; }
</style>
