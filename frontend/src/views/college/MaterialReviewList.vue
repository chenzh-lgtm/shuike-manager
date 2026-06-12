<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:bold;font-size:16px">教学材料审核（含AI评分）</span>
          <el-radio-group v-model="statusFilter" size="small" @change="fetchData">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="AI_COMPLETED">待主任审核</el-radio-button>
            <el-radio-button value="COLLEGE_APPROVED">待教务处审核</el-radio-button>
            <el-radio-button value="OFFICE_APPROVED">已通过</el-radio-button>
            <el-radio-button value="AI_REJECTED">已驳回</el-radio-button>
          </el-radio-group>
        </div>
        <div style="margin-top:10px;display:flex;gap:10px;align-items:center">
          <el-select v-model="filterCourseId" placeholder="选择课程" clearable style="width:200px" @change="onCourseChange" @clear="onFilterChange">
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-select v-model="filterTeacherId" placeholder="选择教师" clearable style="width:160px" @change="onFilterChange" @clear="onFilterChange">
            <el-option v-for="t in teacherOptions" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width:100%" :default-sort="{prop:'createdAt',order:'descending'}">
        <el-table-column prop="id" label="编号" width="65" />
        <el-table-column label="教师" width="90">
          <template #default="{ row }">{{ row.teacherName || '-' }}</template>
        </el-table-column>
        <el-table-column label="课程" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.courseName || '-' }}</template>
        </el-table-column>
        <el-table-column label="材料类型" width="100">
          <template #default="{ row }">{{ typeLabel(row.materialType) }}</template>
        </el-table-column>
        <el-table-column label="AI评分" width="140" align="center">
          <template #default="{ row }">
            <template v-if="row.aiScore != null">
              <el-progress type="dashboard" :percentage="row.aiScore||0" :width="45"
                :color="row.aiScore>=90?'#67C23A':row.aiScore>=75?'#409EFF':row.aiScore>=60?'#E6A23C':'#F56C6C'" />
              <span :style="{color:scoreColor(row.aiScore),fontWeight:'bold',fontSize:'14px'}">{{ row.aiScore }}分</span>
            </template>
            <template v-else>
              <el-tag v-if="row.status==='AI_EVALUATING'" type="warning" size="small">评审中</el-tag>
              <el-tag v-else type="info" size="small">暂无</el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="材料描述" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="材料文件" width="160" align="center">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="openFilePreview(row)" :loading="previewLoading === row.id">📄 预览</el-button>
            <el-button text size="small" type="success" @click="downloadFileRow(row)">⬇ 下载</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="160" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="showDetail(row)">审核</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:center">
        <el-pagination v-model:current-page="page" :page-size="10" :total="total"
          layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 审核详情弹窗 -->
    <el-dialog v-model="dialogVisible" :title="'材料审核 — ' + (typeLabel(currentItem?.materialType) || '')" width="960px" top="2vh" destroy-on-close>
      <template v-if="currentItem">

        <!-- 材料信息条 -->
        <div class="info-bar">
          <div class="info-item"><span class="info-label">教师</span><span class="info-val">{{ currentItem.teacherName || '-' }}</span></div>
          <div class="info-divider"></div>
          <div class="info-item"><span class="info-label">课程</span><span class="info-val">{{ currentItem.courseName || '-' }}</span></div>
          <div class="info-divider"></div>
          <div class="info-item"><span class="info-label">提交时间</span><span class="info-val">{{ currentItem.submitTime }}</span></div>
          <div class="info-divider"></div>
          <div class="info-item"><span class="info-label">状态</span><el-tag :type="statusType(currentItem.status)" size="small">{{ statusLabel(currentItem.status) }}</el-tag></div>
        </div>

        <!-- 描述 -->
        <div v-if="currentItem.description" class="desc-box">{{ currentItem.description }}</div>

        <!-- 主内容区：左评分 + 右文件/操作 -->
        <div class="main-grid">
          <!-- 左：AI评分 -->
          <div class="left-panel">
            <div v-if="currentItem.aiScore != null" class="score-panel">
              <div class="score-gauge">
                <el-progress type="dashboard" :percentage="currentItem.aiScore||0" :width="120" :stroke-width="8"
                  :color="currentItem.aiScore>=90?'#4a7c59':currentItem.aiScore>=75?'#409EFF':currentItem.aiScore>=60?'#c8963e':'#b84c3d'">
                  <template #default="{ percentage }"><div class="score-num">{{ percentage }}</div><div style="font-size:12px;color:var(--color-text-muted)">{{ scoreGrade(currentItem.aiScore) }}</div></template>
                </el-progress>
                <div class="score-title">AI 综合评分</div>
              </div>
              <div class="dim-bars">
                <div v-for="(v,k) in dimScores" :key="k" class="dim-row">
                  <span class="dim-label">{{ dimLabel(k) }}</span>
                  <el-progress :percentage="v" :color="v>=75?'#4a7c59':v>=60?'#c8963e':'#b84c3d'" :show-text="false" :stroke-width="6" style="flex:1" />
                  <span class="dim-val">{{ v }}</span>
                </div>
              </div>
            </div>
            <div v-else class="score-loading">⏳ AI 正在评审中...</div>

            <!-- 建议 -->
            <div v-if="groupedSuggestions.length" class="suggest-panel">
              <div class="suggest-title">AI 优化建议</div>
              <div v-for="(item, idx) in groupedSuggestions" :key="idx" class="suggest-item">
                <div class="suggest-dim">【{{ item.dimension }}】</div>
                <ul class="suggest-list"><li v-for="(iss, j) in item.issues" :key="j">{{ iss }}</li></ul>
                <div v-if="item.direction" class="suggest-dir">→ {{ item.direction }}</div>
              </div>
            </div>

            <!-- 文件 -->
            <div class="file-panel">
              <div class="file-title">📎 附件文件（{{ currentFiles.length }}）</div>
              <div v-if="!currentFiles.length" class="file-empty">暂无附件</div>
              <div v-for="f in currentFiles" :key="f.id" class="file-row">
                <span class="file-name">{{ f.fileName }}</span>
                <span class="file-size">{{ formatSize(f.fileSize) }}</span>
                <span class="file-actions">
                  <el-button text size="small" type="primary" @click="openSingleFile(f)">📄 预览</el-button>
                  <el-button text size="small" type="success" @click="downloadSingleFile(f)">⬇ 下载</el-button>
                </span>
              </div>
            </div>
          </div>

          <!-- 右：审核操作 -->
          <div class="right-panel">
            <!-- 已审核 -->
            <template v-if="['COLLEGE_APPROVED','OFFICE_APPROVED','AI_REJECTED'].includes(currentItem.status) || currentItem.reviewAction">
              <div class="review-done" :class="currentItem.status==='OFFICE_APPROVED'||currentItem.reviewAction==='CONFIRM'?'done-ok':'done-reject'">
                <div class="done-icon">{{ currentItem.reviewAction==='CONFIRM'||currentItem.status==='COLLEGE_APPROVED'||currentItem.status==='OFFICE_APPROVED'?'✅':'❌' }}</div>
                <div class="done-title">{{ currentItem.status==='OFFICE_APPROVED'||currentItem.reviewAction==='CONFIRM'?'已审核通过':currentItem.status==='COLLEGE_APPROVED'?'已提交教务处终审':'已驳回修改' }}</div>
                <div class="done-sub">{{ currentItem.reviewComment || '无审核意见' }}</div>
              </div>
            </template>

            <!-- 待审核表单 -->
            <template v-else-if="currentItem.status==='AI_COMPLETED' && currentItem.aiScore != null">
              <div class="review-form">
                <div class="form-section">
                  <label class="form-label">审核决定</label>
                  <div class="decision-toggle">
                    <div class="decision-tile" :class="{active:action==='CONFIRM'}" @click="action='CONFIRM'">
                      <span class="dec-icon">✓</span>
                      <span class="dec-title">通过</span>
                      <span class="dec-sub">提交教务处终审</span>
                    </div>
                    <div class="decision-tile" :class="{active:action==='REJECT'}" @click="action='REJECT'">
                      <span class="dec-icon">✕</span>
                      <span class="dec-title">驳回</span>
                      <span class="dec-sub">退回教师修改</span>
                    </div>
                  </div>
                </div>

                <div class="form-section">
                  <label class="form-label">审核意见</label>
                  <el-input v-model="comment" type="textarea" :rows="4" :placeholder="action==='REJECT'?'驳回时请务必填写审核意见和修改要求':'选填，可对教师留言（驳回时必填）'" />
                </div>

                <div v-if="action==='REJECT'" class="form-section">
                  <label class="form-label">修改要求</label>
                  <el-input v-model="revisionRequirements" type="textarea" :rows="3" placeholder="请指出具体需要修改的内容，以便教师修改后重新提交" />
                </div>

                <div class="form-actions">
                  <el-button size="large" @click="dialogVisible=false">取消</el-button>
                  <el-button v-if="action==='CONFIRM'" size="large" class="btn-confirm" :loading="saving" @click="handleReview('CONFIRM')">
                    确认通过
                  </el-button>
                  <el-button v-else size="large" class="btn-reject" :loading="saving" @click="handleReview('REJECT')">
                    驳回修改
                  </el-button>
                </div>
              </div>
            </template>

            <div v-else class="review-waiting">⏳ 等待 AI 评审完成...</div>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 文件内容预览弹窗 -->
    <el-dialog v-model="showPreviewText" title="文件内容预览" width="800px" top="3vh">
      <div style="margin-bottom:8px;color:var(--color-text-secondary)">文件名：{{ previewFileName2 }}（{{ previewText.length }}字符）</div>
      <div style="max-height:500px;overflow-y:auto;padding:16px;background:#f8f8f6;border:1px solid var(--color-border);border-radius:4px;font-size:13px;line-height:1.8;white-space:pre-wrap;font-family:var(--font-body)">{{ previewText }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { phaseMaterialApi, fileApi } from '@/api/common'
import { manualReviewApi } from '@/api/manualReview'

const loading = ref(false); const saving = ref(false); const dialogVisible = ref(false)
const tableData = ref<any[]>([]); const total = ref(0); const page = ref(1); const statusFilter = ref('')
const filterCourseId = ref<number | null>(null); const filterTeacherId = ref<number | null>(null)
const courseOptions = ref<any[]>([]); const teacherOptions = ref<any[]>([])
const currentItem = ref<any>(null); const currentFiles = ref<any[]>([])
const comment = ref(''); const action = ref('CONFIRM'); const revisionRequirements = ref('')
const previewLoading = ref(0)

const typeLabels: Record<string, string> = {
  TEACHING_PLAN:'授课计划', LESSON_PLAN:'教案', COURSEWARE:'课件', EXAM_PLAN:'考核方案'
}
function typeLabel(t: string) { return typeLabels[t] || t }
function statusType(s: string) {
  const m: any = { AI_EVALUATING:'warning', AI_COMPLETED:'', COLLEGE_APPROVED:'warning', OFFICE_APPROVED:'success', AI_REJECTED:'danger' }
  return m[s] || 'info'
}
function statusLabel(s: string) {
  const m: any = { AI_EVALUATING:'AI评审中', AI_COMPLETED:'待主任审核', COLLEGE_APPROVED:'待教务处审核', OFFICE_APPROVED:'已通过', AI_REJECTED:'已驳回' }
  return m[s] || s
}
function scoreColor(s: number) {
  if (s>=90) return '#67C23A'; if (s>=75) return '#409EFF'; if (s>=60) return '#E6A23C'; return '#F56C6C'
}
function scoreGrade(s: number) {
  if (s>=90) return '优秀'; if (s>=75) return '良好'; if (s>=60) return '合格'; return '待改进'
}
function dimLabel(k: string) {
  const m: any = { completeness:'内容完整性', standard_match:'课标匹配度', format:'格式规范性', innovation:'创新性' }
  return m[k] || k
}
function formatSize(b: number) {
  if (!b) return '0 B'; if (b<1024) return b+' B'; if (b<1024*1024) return (b/1024).toFixed(1)+' KB'; return (b/(1024*1024)).toFixed(1)+' MB'
}
const dimScores = computed(() => {
  if (!currentItem.value?.dimensionScores) return {}
  try { return JSON.parse(currentItem.value.dimensionScores) } catch { return {} }
})
// 按维度归并建议（旧数据可能每个维度多条，新数据每维度1条）
const groupedSuggestions = computed(() => {
  if (!currentItem.value?.suggestions) return []
  try {
    const raw = JSON.parse(currentItem.value.suggestions)
    if (!Array.isArray(raw)) return []
    const map: Record<string, { dimension: string; issues: string[]; direction: string }> = {}
    for (const s of raw) {
      const dim = s.dimension || '综合'
      if (!map[dim]) map[dim] = { dimension: dim, issues: [], direction: '' }
      map[dim].issues.push(s.issue || '')
      if (s.direction && !map[dim].direction) map[dim].direction = s.direction
    }
    return Object.values(map)
  } catch { return [] }
})

function onCourseChange(cid: number | null) {
  filterTeacherId.value = null
  if (cid && cid > 0) {
    const course = courseOptions.value.find((c:any) => c.id === cid)
    teacherOptions.value = course?.teachers || []
  } else {
    teacherOptions.value = []
  }
  onFilterChange()
}

function onFilterChange() { page.value = 1; fetchData() }

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: 10 }
    if (statusFilter.value) params.status = statusFilter.value
    if (filterCourseId.value && filterCourseId.value > 0) params.courseId = filterCourseId.value
    if (filterTeacherId.value && filterTeacherId.value > 0) params.teacherId = filterTeacherId.value
    const res: any = await phaseMaterialApi.reviewerList(params)
    tableData.value = res.data?.records || []; total.value = res.data?.total || 0
  } catch {} finally { loading.value = false }
}

async function loadFilters() {
  try {
    const r: any = await phaseMaterialApi.reviewerFilters()
    courseOptions.value = r.data?.courses || []
  } catch {}
}

async function showDetail(item: any) {
  currentItem.value = item; action.value = 'CONFIRM'; comment.value = ''; revisionRequirements.value = ''
  try { const fres: any = await phaseMaterialApi.getFiles(item.id); currentFiles.value = fres.data || [] } catch { currentFiles.value = [] }
  dialogVisible.value = true
}

// 表格行：获取材料文件列表，打开第一个文件的预览
async function openFilePreview(row: any) {
  previewLoading.value = row.id
  try {
    const fres: any = await phaseMaterialApi.getFiles(row.id)
    const files = fres.data || []
    if (files.length === 0) { ElMessage.warning('该材料暂无附件'); return }
    openWithNativeViewer(files[0])
  } catch { ElMessage.error('获取文件失败') }
  finally { previewLoading.value = 0 }
}

// 表格行：下载第一个文件
async function downloadFileRow(row: any) {
  try {
    const fres: any = await phaseMaterialApi.getFiles(row.id)
    const files = fres.data || []
    if (files.length === 0) { ElMessage.warning('该材料暂无附件'); return }
    downloadWithPresignedUrl(files[0])
  } catch { ElMessage.error('下载失败') }
}

// 弹窗内：打开单个文件（浏览器原生查看）
function openSingleFile(file: any) {
  openWithNativeViewer(file)
}

// 弹窗内：下载单个文件
function downloadSingleFile(file: any) {
  downloadWithPresignedUrl(file)
}

// 预览弹出框
const previewText = ref(''); const previewFileName2 = ref('')
const showPreviewText = ref(false)
function isPdfFile(f: any) { return f.fileName && f.fileName.toLowerCase().endsWith('.pdf') }

// 预览：PDF新窗口打开，DOCX弹窗展示文本
async function openWithNativeViewer(file: any) {
  try {
    if (isPdfFile(file)) {
      const res: any = await fileApi.preview(file.id)
      if (res.code === 200 && res.data?.url) window.open(res.data.url, '_blank')
    } else {
      const res: any = await fileApi.preview(file.id)
      if (res.code === 200 && res.data?.textContent && res.data.textContent.length > 0) {
        previewText.value = res.data.textContent
        previewFileName2.value = res.data.fileName
        showPreviewText.value = true
      } else {
        ElMessage.warning('该文件暂不支持预览')
      }
    }
  } catch { ElMessage.error('文件预览失败') }
}

// 下载文件（强制下载）
async function downloadWithPresignedUrl(file: any) {
  try {
    const res: any = await fileApi.download(file.id)
    if (res.code === 200 && res.data?.url) window.open(res.data.url, '_blank')
  } catch { ElMessage.error('下载失败') }
}

async function handleReview(a: string) {
  if (a === 'REJECT' && !comment.value) { ElMessage.warning('驳回时请填写审核意见'); return }
  saving.value = true
  try {
    await manualReviewApi.review(currentItem.value.evaluationId, {
      action: a,
      modifiedScore: null,
      modifyReason: null,
      reviewComment: comment.value,
      revisionRequirements: a === 'REJECT' ? revisionRequirements.value : null,
      deadline: null,
      reviewLevel: 'COLLEGE'
    })
    ElMessage.success(a === 'CONFIRM' ? '已通过，已提交教务处终审' : '已驳回修改')
    dialogVisible.value = false; fetchData()
  } catch (e: any) { ElMessage.error('操作失败') }
  finally { saving.value = false }
}

onMounted(async () => { await loadFilters(); fetchData() })
</script>

<style scoped>
/* ─── 审核弹窗 ─── */
.info-bar { display:flex; align-items:center; gap:0; padding:12px 16px; background:var(--color-bg); border-radius:var(--radius-md); margin-bottom:12px; flex-wrap:wrap; }
.info-item { display:flex; flex-direction:column; padding:0 16px; }
.info-label { font-size:11px; color:var(--color-text-muted); text-transform:uppercase; letter-spacing:0.06em; margin-bottom:2px; }
.info-val { font-size:13px; color:var(--color-text); font-weight:500; }
.info-divider { width:1px; height:32px; background:var(--color-border); }
.desc-box { padding:10px 16px; background:#fdf6ec; border-left:3px solid var(--color-accent); border-radius:0 var(--radius-sm) var(--radius-sm) 0; font-size:13px; color:var(--color-text-secondary); margin-bottom:16px; }

/* 主网格 */
.main-grid { display:grid; grid-template-columns:1.2fr 1fr; gap:20px; }
.left-panel { min-width:0; }
.right-panel { min-width:0; }

/* 评分面板 */
.score-panel { display:flex; gap:20px; align-items:center; padding:16px; background:var(--color-bg); border-radius:var(--radius-md); margin-bottom:12px; }
.score-gauge { text-align:center; flex-shrink:0; }
.score-num { font-size:38px; font-weight:700; font-family:var(--font-display); line-height:1; }
.score-title { font-size:13px; color:var(--color-text-muted); margin-top:4px; }
.score-loading { text-align:center;padding:24px;color:var(--color-text-muted); }
.dim-bars { flex:1; min-width:0; }
.dim-row { display:flex;align-items:center;gap:8px;margin-bottom:8px; }
.dim-label { font-size:12px;color:var(--color-text-secondary);width:90px;text-align:right;flex-shrink:0; }
.dim-val { font-size:13px;font-weight:700;width:28px;text-align:center;color:var(--color-text); }

/* 建议 */
.suggest-panel { padding:16px; background:var(--color-bg); border-radius:var(--radius-md); margin-bottom:12px; }
.suggest-title { font-weight:600;font-size:14px;margin-bottom:10px;color:var(--color-text); }
.suggest-item { padding:8px 10px;background:#fdf6ec;border-radius:4px;margin-bottom:6px;font-size:12px; }
.suggest-dim { color:#c8963e;font-weight:700;font-size:13px;margin-bottom:4px; }
.suggest-list { margin:4px 0 4px 14px;color:var(--color-text); }
.suggest-list li { margin-bottom:2px; }
.suggest-dir { color:var(--color-text-muted);margin-top:4px;font-size:11px; }

/* 文件 */
.file-panel { padding:16px; background:var(--color-bg); border-radius:var(--radius-md); }
.file-title { font-weight:600;font-size:14px;margin-bottom:10px;color:var(--color-text); }
.file-empty { color:var(--color-text-muted);font-size:13px; }
.file-row { display:flex;align-items:center;padding:8px 0;border-bottom:1px solid var(--color-border);gap:8px; }
.file-row:last-child { border-bottom:none; }
.file-name { flex:1;font-size:13px;color:var(--color-text);overflow:hidden;text-overflow:ellipsis;white-space:nowrap; }
.file-size { font-size:11px;color:var(--color-text-muted);width:50px;text-align:right;flex-shrink:0; }
.file-actions { flex-shrink:0; }

/* 审核表单 */
.review-done { text-align:center;padding:40px 20px;border-radius:var(--radius-md); }
.review-done.done-ok { background:#f0f9f4; }
.review-done.done-reject { background:#fef2f2; }
.done-icon { font-size:40px;margin-bottom:12px; }
.done-title { font-size:16px;font-weight:700;color:var(--color-text);margin-bottom:6px; }
.done-sub { font-size:13px;color:var(--color-text-muted); }
.review-waiting { text-align:center;padding:40px;color:var(--color-text-muted); }

.review-form { display:flex;flex-direction:column;gap:16px; }
.form-section { display:flex;flex-direction:column;gap:6px; }
.form-label { font-size:13px;font-weight:600;color:var(--color-text);margin-bottom:2px; }
.decision-toggle { display:grid; grid-template-columns:1fr 1fr; gap:10px; }
.decision-tile {
  padding:18px 12px; text-align:center; border:2px solid var(--color-border); border-radius:var(--radius-md);
  cursor:pointer; transition:all 0.2s ease; background:var(--color-surface);
  display:flex; flex-direction:column; align-items:center; gap:4px;
}
.decision-tile:hover { border-color:var(--color-text-muted); transform:translateY(-1px); }
.decision-tile.active {
  border-color:var(--color-primary); background:var(--color-primary-bg);
  box-shadow:0 0 0 1px var(--color-primary);
}
.dec-icon { font-size:22px; font-weight:700; line-height:1; }
.decision-tile:first-child .dec-icon { color:var(--color-success); }
.decision-tile:last-child .dec-icon { color:var(--color-danger); }
.dec-title { font-size:14px; font-weight:700; color:var(--color-text); }
.dec-sub { font-size:11px; color:var(--color-text-muted); }
.decision-tile.active .dec-title { color:var(--color-primary); }
.decision-tile.active .dec-sub { color:var(--color-primary); opacity:0.7; }

.btn-confirm { background:var(--color-success)!important;border-color:var(--color-success)!important;color:#fff!important;font-weight:600; }
.btn-confirm:hover { background:#3d6b4a!important; }
.btn-reject { background:var(--color-danger)!important;border-color:var(--color-danger)!important;color:#fff!important;font-weight:600; }
.btn-reject:hover { background:#9e3d32!important; }

.form-actions { display:flex;gap:10px;justify-content:flex-end;padding-top:8px; }
</style>
