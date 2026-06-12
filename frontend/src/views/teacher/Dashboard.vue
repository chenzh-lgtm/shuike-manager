<template>
  <div class="dashboard">
    <h2 class="page-title">工作台</h2>

    <div class="stat-grid">
      <div class="stat-card" @click="$router.push('/teacher/review-progress')">
        <div class="stat-value muted">{{ stats.total }}</div>
        <div class="stat-label">总计提交</div>
      </div>
      <div class="stat-card">
        <div class="stat-value primary">{{ stats.reviewing }}</div>
        <div class="stat-label">审核中</div>
      </div>
      <div class="stat-card">
        <div class="stat-value success">{{ stats.approved }}</div>
        <div class="stat-label">已通过</div>
      </div>
      <div class="stat-card">
        <div class="stat-value danger">{{ stats.rejected }}</div>
        <div class="stat-label">已驳回</div>
      </div>
    </div>

    <div class="quick-row">
      <div class="quick-card" @click="$router.push('/teacher/submit-material')">
        <span class="quick-icon">↥</span>
        <div class="quick-title">提交教学材料</div>
        <div class="quick-desc">授课计划 / 教案 / 课件 / 考核方案</div>
      </div>
      <div class="quick-card" @click="$router.push('/teacher/review-progress')">
        <span class="quick-icon">◷</span>
        <div class="quick-title">材料审核进度</div>
        <div class="quick-desc">查看审核状态与反馈意见</div>
      </div>
      <div class="quick-card" @click="$router.push('/teacher/alignment-reports')">
        <span class="quick-icon">⊟</span>
        <div class="quick-title">对齐分析报告</div>
        <div class="quick-desc">产业需求与人培方案分析</div>
      </div>
    </div>

    <el-card v-if="recentList.length" class="recent-card">
      <template #header><span class="card-title">最近提交的材料</span></template>
      <el-table :data="recentList" size="small">
        <el-table-column label="材料类型" width="100"><template #default="{ row }">{{ typeLabels[row.materialType]||row.materialType }}</template></el-table-column>
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'; import { phaseMaterialApi } from '@/api/common'
const typeLabels: Record<string,string> = { TEACHING_PLAN:'授课计划', LESSON_PLAN:'教案', COURSEWARE:'课件', EXAM_PLAN:'考核方案' }
const stats = reactive({ total:0, reviewing:0, approved:0, rejected:0 }); const recentList = ref<any[]>([])
function statusType(s:string){ const m:any={AI_EVALUATING:'warning',AI_COMPLETED:'',COLLEGE_APPROVED:'warning',OFFICE_APPROVED:'success',AI_REJECTED:'danger'}; return m[s]||'info' }
function statusLabel(s:string){ const m:any={AI_EVALUATING:'AI评审中',AI_COMPLETED:'待主任审核',COLLEGE_APPROVED:'待教务处审核',OFFICE_APPROVED:'已通过',AI_REJECTED:'已驳回'}; return m[s]||s }
onMounted(async()=>{ try{ const r:any=await phaseMaterialApi.list({page:1,pageSize:100}); const items=r.data?.records||[]; stats.total=items.length; stats.reviewing=items.filter((i:any)=>['AI_EVALUATING','AI_COMPLETED','COLLEGE_APPROVED'].includes(i.status)).length; stats.approved=items.filter((i:any)=>i.status==='OFFICE_APPROVED').length; stats.rejected=items.filter((i:any)=>i.status==='AI_REJECTED').length; recentList.value=items.slice(0,5) }catch{} })
</script>

<style scoped>
.page-title { font-family:var(--font-display); font-size:22px; font-weight:700; color:var(--color-primary); margin-bottom:var(--space-lg); letter-spacing:0.04em; }
.stat-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:var(--space-md); margin-bottom:var(--space-lg); }
.stat-card { background:var(--color-surface); border-radius:var(--radius-md); padding:24px; text-align:center; cursor:pointer; box-shadow:var(--shadow-card); transition:all var(--transition); }
.stat-card:hover { transform:translateY(-2px); box-shadow:var(--shadow-md); }
.stat-value { font-size:36px; font-weight:700; font-family:var(--font-display); }
.stat-value.muted { color:var(--color-text-muted); } .stat-value.primary { color:var(--color-primary); }
.stat-value.success { color:var(--color-success); } .stat-value.danger { color:var(--color-danger); }
.stat-label { font-size:13px; color:var(--color-text-muted); margin-top:6px; letter-spacing:0.02em; }
.quick-row { display:grid; grid-template-columns:repeat(3,1fr); gap:var(--space-md); margin-bottom:var(--space-lg); }
.quick-card { background:var(--color-surface); border-radius:var(--radius-md); padding:28px; text-align:center; cursor:pointer; box-shadow:var(--shadow-card); transition:all var(--transition); border:1px solid transparent; }
.quick-card:hover { border-color:var(--color-primary); transform:translateY(-2px); box-shadow:var(--shadow-md); }
.quick-icon { font-size:28px; display:block; margin-bottom:12px; }
.quick-title { font-size:15px; font-weight:600; color:var(--color-text); margin-bottom:4px; }
.quick-desc { font-size:12px; color:var(--color-text-muted); }
.card-title { font-weight:600; font-size:15px; letter-spacing:0.03em; }
.recent-card { margin-top:0; }
</style>
