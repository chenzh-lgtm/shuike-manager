<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:bold;font-size:16px">材料审核进度</span>
          <el-button type="primary" @click="$router.push('/teacher/submit-material')">+ 提交新材料</el-button>
        </div>
      </template>

      <el-radio-group v-model="statusFilter" size="small" style="margin-bottom:16px" @change="fetchData">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="AI_EVALUATING">AI评审中</el-radio-button>
        <el-radio-button value="AI_COMPLETED">待主任审核</el-radio-button>
        <el-radio-button value="COLLEGE_APPROVED">待教务处审核</el-radio-button>
        <el-radio-button value="OFFICE_APPROVED">已通过</el-radio-button>
        <el-radio-button value="AI_REJECTED">已驳回</el-radio-button>
      </el-radio-group>

      <el-table :data="tableData" v-loading="loading" style="width:100%" :default-sort="{prop:'submitTime',order:'descending'}">
        <el-table-column prop="id" label="编号" width="65" />
        <el-table-column label="课程名称" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.courseName || '-' }}</template>
        </el-table-column>
        <el-table-column label="材料类型" width="100">
          <template #default="{ row }">{{ typeLabel(row.materialType) }}</template>
        </el-table-column>
        <el-table-column label="学期" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.semesterName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="120" show-overflow-tooltip />
        <el-table-column label="当前审核进度" width="150" align="center">
          <template #default="{ row }">
            <div class="stage-dots"><span v-for="(s,i) in ['AI评审','主任','教务处']" :key="i" class="stage-dot" :class="dotClass(row, i)">{{ s }}</span></div>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="115" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="150" />
        <el-table-column label="操作" width="170" align="center" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.status==='AI_REJECTED'" text size="small" type="warning" @click="handleReupload(row)">重传</el-button>
            <el-button v-if="!['COLLEGE_APPROVED','OFFICE_APPROVED'].includes(row.status)" text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:center">
        <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="dialogVisible" title="审核详情" width="700px">
      <template v-if="currentItem">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="材料编号">{{ currentItem.id }}</el-descriptions-item>
          <el-descriptions-item label="材料类型">{{ typeLabel(currentItem.materialType) }}</el-descriptions-item>
          <el-descriptions-item label="课程名称">{{ currentItem.courseName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ currentItem.submitTime }}</el-descriptions-item>
          <el-descriptions-item label="审核状态"><el-tag :type="statusType(currentItem.status)" size="small">{{ statusLabel(currentItem.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="材料描述" :span="2">{{ currentItem.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-top:16px">
          <h4>已上传文件</h4>
          <el-table :data="currentFiles" style="width:100%;margin-top:8px" v-if="currentFiles.length">
            <el-table-column prop="fileName" label="文件名" />
            <el-table-column prop="fileType" label="类型" width="70" />
            <el-table-column label="大小" width="100"><template #default="{ row }">{{ formatSize(row.fileSize) }}</template></el-table-column>
          </el-table>
          <el-empty v-else description="暂无文件" :image-size="60" />
        </div>

        <div v-if="currentItem.reviewStageLabel" style="margin-top:20px">
          <el-divider />
          <h4 style="margin-bottom:12px">审核进度：{{ currentItem.reviewStageLabel }}</h4>
          <el-result v-if="currentItem.status==='OFFICE_APPROVED'" icon="success" title="审核已通过" :sub-title="currentItem.reviewComment || ''" />
          <el-result v-else-if="currentItem.status==='AI_REJECTED'" icon="error" title="审核已驳回" :sub-title="currentItem.reviewComment || ''">
            <template #extra>
              <el-alert v-if="currentItem.revisionRequirements" type="error" :closable="false" show-icon style="margin-top:8px;text-align:left">
                <template #title>修改要求</template><p>{{ currentItem.revisionRequirements }}</p>
                <p v-if="currentItem.deadline" style="color:#F56C6C;margin-top:4px">截止日期：{{ currentItem.deadline }}</p>
              </el-alert>
              <el-button type="warning" style="margin-top:12px" @click="dialogVisible=false; handleReupload(currentItem)">重新上传材料</el-button>
            </template>
          </el-result>
          <el-result v-else icon="info" title="等待审核" :sub-title="stageDesc(currentItem.status)" />
        </div>
        <div v-else style="margin-top:20px"><el-divider /><el-result icon="info" title="审核进行中" sub-title="材料正在审核流程中，请耐心等待" /></div>
      </template>
    </el-dialog>

    <!-- 重新上传弹窗 -->
    <el-dialog v-model="reuploadVisible" title="重新上传材料" width="500px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom:16px" title="请根据审核意见修改后重新上传" />
      <el-upload ref="reuploadRef" drag :auto-upload="false" :on-change="handleReuploadFile" :file-list="reuploadFiles" multiple accept=".doc,.docx,.pdf,.jpg,.jpeg,.png">
        <div style="padding:20px 0"><div style="font-size:40px">📁</div><div>拖拽或点击上传</div></div>
      </el-upload>
      <template #footer>
        <el-button @click="reuploadVisible=false">取消</el-button>
        <el-button type="primary" :loading="reuploading" @click="confirmReupload">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { phaseMaterialApi, fileApi } from '@/api/common'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0); const page = ref(1)
const statusFilter = ref(''); const dialogVisible = ref(false); const currentItem = ref<any>(null)
const currentFiles = ref<any[]>([]); const reuploadVisible = ref(false); const reuploading = ref(false)
const reuploadFiles = ref<any[]>([]); const reuploadFileIds = ref<number[]>([])
const reuploadItem = ref<any>(null)

const typeLabels: Record<string, string> = {
  TEACHING_PLAN:'授课计划', LESSON_PLAN:'教案', COURSEWARE:'课件', EXAM_PLAN:'考核方案'
}
function typeLabel(t: string) { return typeLabels[t] || t }
function statusType(s: string) {
  const m: any = { AI_EVALUATING:'warning', AI_COMPLETED:'', COLLEGE_APPROVED:'warning', OFFICE_APPROVED:'success', AI_REJECTED:'danger' }
  return m[s] || 'info'
}
function stageDesc(s:string){ const m:any={AI_EVALUATING:'AI正在评审您的材料',AI_COMPLETED:'主任正在审核您的材料',COLLEGE_APPROVED:'教务处正在对您的材料进行终审',OFFICE_APPROVED:'审核已全部通过',AI_REJECTED:'审核被驳回，请根据意见修改'}; return m[s]||'审核进行中' }
function stageStep(s: string) { const m:any={AI_EVALUATING:0,AI_COMPLETED:1,COLLEGE_APPROVED:2,OFFICE_APPROVED:3,AI_REJECTED:-1}; return m[s]!=null?m[s]:0 }
function dotClass(row:any, idx:number) {
  const s = row.reviewStage || row.status
  const step = stageStep(s)
  if (s==='AI_REJECTED') return 'rejected'
  if (s==='OFFICE_APPROVED') return 'done'   // 全部通过
  if (idx < step) return 'done'                // 已完成的阶段
  if (idx == step) return 'current'            // 当前阶段
  return 'pending'                              // 未开始的阶段
}
function statusLabel(s: string) {
  const m: any = { AI_EVALUATING:'AI评审中', AI_COMPLETED:'待主任审核', COLLEGE_APPROVED:'待教务处审核', OFFICE_APPROVED:'已通过', AI_REJECTED:'已驳回' }
  return m[s] || s
}
function formatSize(b: number) {
  if (!b) return '0 B'; if (b<1024) return b+' B'; if (b<1024*1024) return (b/1024).toFixed(1)+' KB'; return (b/(1024*1024)).toFixed(1)+' MB'
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: 10 }
    if (statusFilter.value) params.status = statusFilter.value
    const res: any = await phaseMaterialApi.list(params)
    tableData.value = res.data?.records || []; total.value = res.data?.total || 0
  } catch {} finally { loading.value = false }
}

async function showDetail(item: any) {
  currentItem.value = item
  try { const fres: any = await phaseMaterialApi.getFiles(item.id); currentFiles.value = fres.data || [] } catch { currentFiles.value = [] }
  dialogVisible.value = true
}

async function handleDelete(item: any) {
  await ElMessageBox.confirm('确认删除该材料？', '提示', { type: 'warning' })
  await phaseMaterialApi.delete(item.id)
  ElMessage.success('已删除'); fetchData()
}

function handleReupload(item: any) {
  reuploadItem.value = item; reuploadFiles.value = []; reuploadFileIds.value = []; reuploadVisible.value = true
}

async function handleReuploadFile(file: any) {
  reuploadFiles.value.push(file)
  try { const mt = reuploadItem?.materialType || ''; const res: any = await fileApi.upload(file.raw, 'MATERIAL', mt); if (res.data?.fileId) reuploadFileIds.value.push(res.data.fileId) } catch { ElMessage.error('上传失败') }
}

async function confirmReupload() {
  if (!reuploadItem.value) return
  reuploading.value = true
  try {
    // 先上传新文件（如果有）
    if (reuploadFileIds.value.length > 0) {
      await phaseMaterialApi.addFiles(reuploadItem.value.id, reuploadFileIds.value)
    }
    // 重置状态并重新触发AI评审
    await phaseMaterialApi.resubmit(reuploadItem.value.id)
    ElMessage.success('已重新提交，AI正在重新评审')
    reuploadVisible.value = false
    fetchData()
  } catch { ElMessage.error('提交失败') } finally { reuploading.value = false }
}

onMounted(() => fetchData())
</script>

<style scoped>
.stage-dots { display: flex; gap: 6px; justify-content: center; }
.stage-dot { padding: 2px 8px; border-radius: 12px; font-size: 11px; color: #bdbdbd; background: #f5f5f5; white-space: nowrap; }
.stage-dot.done { background: #e8f5e9; color: #2e7d32; font-weight: 600; }
.stage-dot.pending { background: #ffebee; color: #c62828; }
.stage-dot.current { background: #fff3e0; color: #e65100; font-weight: 600; }
.stage-dot.rejected { background: #ffebee; color: #c62828; font-weight: 600; }
.progress-timeline { display: flex; align-items: center; gap: 1px; }
.progress-node { display: flex; flex-direction: column; align-items: center; gap: 3px; }
.dot { width: 22px; height: 22px; border-radius: 50%; background: #e0e0e0; color: #999; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: bold; }
.dot.done { background: #67C23A; color: white; }
.dot.active { background: #409EFF; color: white; }
.dot.rejected { background: #F56C6C; color: white; }
.label { font-size: 10px; color: #909399; white-space: nowrap; }
.line { color: #e0e0e0; font-size: 9px; margin-bottom: 16px; }
.line.done { color: #67C23A; }
</style>
