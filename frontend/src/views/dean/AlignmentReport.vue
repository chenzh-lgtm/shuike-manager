<template>
  <div class="report-page">
    <!-- 触发分析 -->
    <el-card class="trigger-card">
      <div class="trigger-row">
        <span style="font-weight:600;font-size:15px;white-space:nowrap">分析产业需求</span>
        <el-select v-model="tcpId" placeholder="选择人培方案" style="width:280px"><el-option v-for="t in tcpList" :key="t.id" :label="t.majorName+' ('+t.grade+')'" :value="t.id" /></el-select>
        <el-button type="primary" :loading="analyzing" @click="handleAnalyze">🚀 开始AI分析</el-button>
      </div>
      <div style="font-size:12px;color:var(--color-text-muted);margin-top:8px">
        AI将基于人才培养方案全文，从产业背景、人才需求、课程体系、学习成果、就业前景等维度进行深度分析，生成学术级分析报告。
      </div>
    </el-card>

    <!-- 报告列表 -->
    <el-card v-if="!viewingReport" class="list-card">
      <template #header><span style="font-weight:600">历史分析报告</span></template>
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="majorName" label="专业" width="160" />
        <el-table-column label="覆盖度评分" width="130" align="center">
          <template #default="{ row }">
            <template v-if="row.coverageScore">
              <span :style="{color:scoreColor(row.coverageScore),fontWeight:'700',fontSize:'16px'}">{{ row.coverageScore }}分</span>
              <el-tag :type="row.coverageScore>=75?'success':row.coverageScore>=60?'warning':'danger'" size="small" style="margin-left:6px">{{ gradeText(row.coverageScore) }}</el-tag>
            </template>
            <span v-else style="color:var(--color-text-muted)">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="分析时间" width="170" />
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewReport(row)">📊 查看</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :total="total" layout="total, prev, pager, next" @change="fetchData" />
    </el-card>

    <!-- ====== 报告主体 ====== -->
    <div v-if="viewingReport && report" class="report-body">
      <div class="report-toolbar">
        <el-button @click="viewingReport=false">← 返回列表</el-button>
        <span class="report-title">{{ report.majorName }} — 产业需求对齐分析报告</span>
        <span class="report-date">分析时间: {{ report.createdAt }}</span>
        <el-button type="primary" size="small" @click="exportPdf" style="margin-left:auto">📄 导出PDF</el-button>
      </div>

      <!-- 一、总体概述 -->
      <div class="report-section">
        <h2 class="section-title">一、专业概况与产业背景</h2>
        <div class="section-text" v-if="rp.overview">{{ rp.overview }}</div>
        <el-row :gutter="16" style="margin-top:16px" v-if="rp.industryBackground">
          <el-col :span="16">
            <div class="section-text">{{ rp.industryBackground?.summary }}</div>
          </el-col>
          <el-col :span="8">
            <div class="trend-list">
              <div class="trend-title">📈 产业发展趋势</div>
              <div v-for="(t,i) in (rp.industryBackground?.trends||[])" :key="i" class="trend-item">{{ i+1 }}. {{ t }}</div>
            </div>
          </el-col>
        </el-row>
        <div style="margin-top:12px" v-if="rp.industryKeywords?.length">
          <span class="label-sm">产业关键词：</span>
          <el-tag v-for="k in rp.industryKeywords" :key="k" size="small" effect="plain" style="margin:2px 4px 2px 0">{{ k }}</el-tag>
        </div>
      </div>

      <!-- 二、产业人才需求分析 -->
      <div class="report-section">
        <h2 class="section-title">二、产业人才需求分析</h2>
        <div class="section-text" v-if="rp.demandAnalysis?.summary">{{ rp.demandAnalysis.summary }}</div>
        <el-row :gutter="16" style="margin-top:16px" v-if="rp.demandAnalysis">
          <el-col :span="14">
            <div class="chart-card"><div class="chart-header">岗位需求分布</div><div ref="jobsChartRef" class="chart-sm"></div></div>
          </el-col>
          <el-col :span="10">
            <div class="chart-card"><div class="chart-header">技能要求雷达图</div><div ref="skillRadarRef" class="chart-sm"></div></div>
          </el-col>
        </el-row>
        <div style="margin-top:12px" v-if="rp.demandAnalysis?.requiredSkills?.length">
          <div class="label-sm">核心技能需求：</div>
          <div class="skill-tags">
            <span v-for="sk in rp.demandAnalysis.requiredSkills" :key="sk.skill" class="skill-tag">
              {{ sk.skill }} <small>({{ sk.level }})</small>
            </span>
          </div>
        </div>
      </div>

      <!-- 三、课程体系分析 -->
      <div class="report-section">
        <h2 class="section-title">三、课程体系与产业需求匹配度分析</h2>
        <div class="section-text" v-if="rp.courseAnalysis?.summary">{{ rp.courseAnalysis.summary }}</div>
        <el-row :gutter="16" style="margin-top:16px" v-if="rp.courseAnalysis">
          <el-col :span="14">
            <div class="chart-card"><div class="chart-header">课程体系评分雷达图</div><div ref="courseRadarRef" class="chart-sm"></div></div>
          </el-col>
          <el-col :span="10">
            <div class="chart-card"><div class="chart-header">核心课程重要度排行</div><div ref="coreChartRef" class="chart-sm"></div></div>
          </el-col>
        </el-row>
        <div style="margin-top:16px" v-if="rp.courseAnalysis">
          <el-tabs type="border-card">
            <el-tab-pane v-if="rp.courseAnalysis.coreCourses?.length" :label="'专业核心课('+rp.courseAnalysis.coreCourses.length+')'">
              <el-table :data="rp.courseAnalysis.coreCourses" size="small"><el-table-column prop="name" label="课程名称" width="200" /><el-table-column prop="credits" label="学分" width="60" align="center" /><el-table-column label="重要度" width="200"><template #default="{row}"><el-progress :percentage="row.importance" :color="row.importance>=70?'#4a7c59':'#c8963e'" :show-text="false" /><span style="margin-left:6px;font-weight:600">{{ row.importance }}</span></template></el-table-column><el-table-column prop="reason" label="课程定位" show-overflow-tooltip /></el-table>
            </el-tab-pane>
            <el-tab-pane v-if="rp.courseAnalysis.electiveCourses?.length" :label="'选修课('+rp.courseAnalysis.electiveCourses.length+')'">
              <el-table :data="rp.courseAnalysis.electiveCourses" size="small"><el-table-column prop="name" label="课程名称" width="200" /><el-table-column prop="credits" label="学分" width="60" align="center" /><el-table-column prop="suggestion" label="选课建议" show-overflow-tooltip /></el-table>
            </el-tab-pane>
            <el-tab-pane v-if="rp.courseAnalysis.publicCourses?.length" :label="'公共课('+rp.courseAnalysis.publicCourses.length+')'">
              <el-table :data="rp.courseAnalysis.publicCourses" size="small"><el-table-column prop="name" label="课程名称" width="200" /><el-table-column prop="credits" label="学分" width="60" align="center" /><el-table-column prop="relevance" label="产业关联度" show-overflow-tooltip /></el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <!-- 四、学习成果分析 -->
      <div class="report-section" v-if="rp.learningOutcomes">
        <h2 class="section-title">四、学生学习成果与能力达成分析</h2>
        <div class="section-text">{{ rp.learningOutcomes?.summary }}</div>
        <el-row :gutter="16" style="margin-top:16px">
          <el-col :span="12">
            <div class="dual-metric"><div class="metric-item"><span class="metric-label">知识掌握度</span><el-progress type="circle" :percentage="rp.learningOutcomes?.knowledgeLevel?.score||0" :width="90" :color="'#1a3c34'" /><div class="metric-detail"><div v-for="s in (rp.learningOutcomes?.knowledgeLevel?.strengths||[])" :key="s" class="metric-good">✓ {{ s }}</div><div v-for="w in (rp.learningOutcomes?.knowledgeLevel?.weaknesses||[])" :key="w" class="metric-bad">✗ {{ w }}</div></div></div></div>
          </el-col>
          <el-col :span="12">
            <div class="dual-metric"><div class="metric-item"><span class="metric-label">能力培养度</span><el-progress type="circle" :percentage="rp.learningOutcomes?.abilityLevel?.score||0" :width="90" :color="'#c8963e'" /><div class="metric-detail"><div v-for="s in (rp.learningOutcomes?.abilityLevel?.strengths||[])" :key="s" class="metric-good">✓ {{ s }}</div><div v-for="w in (rp.learningOutcomes?.abilityLevel?.weaknesses||[])" :key="w" class="metric-bad">✗ {{ w }}</div></div></div></div>
          </el-col>
        </el-row>
        <el-table v-if="rp.learningOutcomes?.graduateCompetencies?.length" :data="rp.learningOutcomes.graduateCompetencies" size="small" style="margin-top:12px">
          <el-table-column prop="competency" label="毕业能力项" width="200" /><el-table-column prop="target" label="目标水平" width="100" align="center" /><el-table-column prop="current" label="当前达成度" />
        </el-table>
      </div>

      <!-- 五、就业分析 -->
      <div class="report-section" v-if="rp.employmentAnalysis">
        <h2 class="section-title">五、就业前景分析</h2>
        <div class="section-text">{{ rp.employmentAnalysis?.summary }}</div>
        <el-row :gutter="16" style="margin-top:16px">
          <el-col :span="14">
            <el-table :data="rp.employmentAnalysis?.targetIndustries" size="small"><el-table-column prop="name" label="目标行业" /><el-table-column prop="demand" label="需求程度" width="100" align="center"><template #default="{row}"><el-tag :type="row.demand==='高'?'success':row.demand==='中'?'warning':'info'" size="small">{{ row.demand }}</el-tag></template></el-table-column><el-table-column prop="growth" label="增长趋势" /></el-table>
          </el-col>
          <el-col :span="10">
            <div class="employ-stats">
              <div class="employ-item"><span class="employ-label">预计就业率</span><span class="employ-val">{{ rp.employmentAnalysis?.employmentRate }}</span></div>
              <div class="employ-item"><span class="employ-label">起薪范围</span><span class="employ-val">{{ rp.employmentAnalysis?.startingSalary }}</span></div>
              <div class="employ-item"><span class="employ-label">就业竞争力</span><el-progress :percentage="rp.employmentAnalysis?.competitivenessScore||0" :color="'#c8963e'" :stroke-width="8" /></div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 六、差距分析 -->
      <div class="report-section" v-if="rp.gapAnalysis?.length">
        <h2 class="section-title">六、人培方案与产业需求差距分析</h2>
        <div v-for="(g,i) in rp.gapAnalysis" :key="i" class="gap-item">
          <div class="gap-header"><el-tag :type="g.severity==='高'?'danger':g.severity==='中'?'warning':'info'" size="small">{{ g.severity }}优</el-tag><span class="gap-area">{{ g.area }}</span></div>
          <div class="gap-desc">{{ g.gap }}</div>
          <div class="gap-impact">💡 影响：{{ g.impact }}</div>
        </div>
      </div>

      <!-- 七、改进建议 -->
      <div class="report-section" v-if="rp.suggestions?.length">
        <h2 class="section-title">七、改进建议与改革方向</h2>
        <div v-for="(s,i) in rp.suggestions" :key="i" class="suggest-card">
          <div class="suggest-meta">
            <el-tag :type="s.priority==='高'?'danger':s.priority==='中'?'warning':'info'" size="small">{{ s.priority }}优先级</el-tag>
            <el-tag type="info" size="small" effect="plain">{{ s.category }}</el-tag>
            <el-tag :type="s.difficulty==='高'?'danger':'success'" size="small" effect="plain">{{ s.difficulty }}难度</el-tag>
          </div>
          <div class="suggest-content">{{ s.content }}</div>
        </div>
      </div>

      <!-- 八、综合评分 -->
      <div class="report-section">
        <h2 class="section-title">八、综合评分与结论</h2>
        <el-row :gutter="16">
          <el-col :span="8"><div class="big-score"><span class="big-num" :style="{color:scoreColor(rp.coverageScore||70)}">{{ rp.coverageScore||'--' }}</span><span class="big-unit">分</span><div class="big-label">总体覆盖度</div></div></el-col>
          <el-col :span="16"><div ref="dimRadarRef" class="chart-md"></div></el-col>
        </el-row>
        <div v-if="rp.summary" class="conclusion-box">{{ rp.summary }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { ElMessageBox } from 'element-plus'
import { alignmentApi } from '@/api/common'
import { talentPlanApi } from '@/api/talentPlan'

const loading=ref(false);const analyzing=ref(false);const viewingReport=ref(false)
const tableData=ref<any[]>([]);const total=ref(0);const page=ref(1)
const tcpList=ref<any[]>([]);const tcpId=ref<number|null>(null)
const report=ref<any>(null)
const jobsChartRef=ref();const skillRadarRef=ref();const courseRadarRef=ref();const coreChartRef=ref();const dimRadarRef=ref()

const rp=computed(()=>{
  if(!report.value?.reportJson) return {}
  try{return JSON.parse(report.value.reportJson)}catch{return{}}
})

function scoreColor(s:number){if(s>=90)return'#4a7c59';if(s>=75)return'#409EFF';if(s>=60)return'#c8963e';return'#b84c3d'}
function gradeText(s:number){if(s>=90)return'优秀';if(s>=75)return'良好';if(s>=60)return'合格';return'待改进'}

async function handleAnalyze(){
  if(!tcpId.value){ElMessage.warning('请选择人培方案');return}
  analyzing.value=true
  try{
    const res:any=await alignmentApi.analyze(tcpId.value)
    ElMessage.success(res.message||'AI分析已启动，预计1-2分钟完成，请稍后刷新列表查看报告')
    setTimeout(()=>{fetchData()},5000)
  }catch(e:any){ElMessage.error(e?.response?.data?.message||'启动失败')}finally{analyzing.value=false}
}

async function viewReport(row:any){
  try{const r:any=await alignmentApi.reportDetail(row.id);report.value=r.data;viewingReport.value=true;await nextTick();buildCharts()}catch{ElMessage.error('加载报告失败')}
}

function exportPdf() {
  if (!report.value?.id) return
  const token = (window as any).__AUTH_TOKEN__ || localStorage.getItem('token') || ''
  window.open('/api/alignment/reports/' + report.value.id + '/pdf?token=' + encodeURIComponent(token), '_blank')
}

function buildCharts(){
  if(!rp.value) return
  const r=rparse(rp.value)

  if(jobsChartRef.value&&r.jobs){
    const c=echarts.init(jobsChartRef.value)
    c.setOption({tooltip:{trigger:'axis'},grid:{left:'3%',right:'8%',bottom:'3%',top:'8%',containLabel:true},xAxis:{type:'value'},yAxis:{type:'category',data:r.jobs.map((j:any)=>j.title),inverse:true,axisLabel:{fontSize:10}},series:[{type:'bar',data:r.jobs.map((j:any)=>j.count),itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#1a3c34'},{offset:1,color:'#4a7c59'}]),borderRadius:[0,4,4,0]},label:{show:true,position:'right',fontSize:10}}]})
  }
  if(skillRadarRef.value&&r.skillRadar){
    const c=echarts.init(skillRadarRef.value)
    c.setOption({tooltip:{},radar:{indicator:Object.entries(r.skillRadar).map(([k,v])=>({name:k,max:100})),center:['50%','55%'],radius:'65%'},series:[{type:'radar',data:[{value:Object.values(r.skillRadar),name:'技能要求',areaStyle:{color:'rgba(200,150,62,0.2)'},lineStyle:{color:'#c8963e',width:2}}]}]})
  }
  if(courseRadarRef.value&&r.courseRadar){
    const c=echarts.init(courseRadarRef.value)
    c.setOption({tooltip:{},radar:{indicator:Object.entries(r.courseRadar).map(([k,v])=>({name:k,max:100})),center:['50%','55%'],radius:'65%'},series:[{type:'radar',data:[{value:Object.values(r.courseRadar),name:'课程评分',areaStyle:{color:'rgba(26,60,52,0.15)'},lineStyle:{color:'#1a3c34',width:2}}]}]})
  }
  if(coreChartRef.value&&r.core){
    const c=echarts.init(coreChartRef.value)
    const items=r.core.reverse()
    c.setOption({tooltip:{trigger:'axis'},grid:{left:'3%',right:'8%',bottom:'3%',top:'8%',containLabel:true},xAxis:{type:'value',max:100},yAxis:{type:'category',data:items.map((i:any)=>i.name),axisLabel:{fontSize:10}},series:[{type:'bar',data:items.map((i:any)=>i.importance),itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#c8963e'},{offset:1,color:'#e8c97a'}]),borderRadius:[0,4,4,0]},label:{show:true,position:'right',fontSize:10}}]})
  }
  if(dimRadarRef.value&&r.dimScores){
    const c=echarts.init(dimRadarRef.value)
    c.setOption({tooltip:{},radar:{indicator:Object.entries(r.dimScores).map(([k,v])=>({name:k,max:100})),center:['50%','55%'],radius:'70%'},series:[{type:'radar',data:[{value:Object.values(r.dimScores),name:'综合评分',areaStyle:{color:'rgba(74,124,89,0.2)'},lineStyle:{color:'#4a7c59',width:2}}]}]})
  }
}

function rparse(raw:any){
  return{
    jobs:raw.demandAnalysis?.jobPositions||[],
    skillRadar:raw.demandAnalysis?.skillRadar||null,
    courseRadar:raw.courseAnalysis?.courseScoreRadar||null,
    core:(raw.courseAnalysis?.coreCourses||[]).slice(0,8),
    dimScores:raw.dimensionScores||null
  }
}

async function fetchData(){loading.value=true;try{const r:any=await alignmentApi.reports({page:page.value,pageSize:10});tableData.value=r.data?.records||[];total.value=r.data?.total||0}catch{}finally{loading.value=false}}

async function handleDelete(row:any){
  try{await ElMessageBox.confirm('确认删除该分析报告？','提示',{type:'warning'});await alignmentApi.delete(row.id);ElMessage.success('已删除');fetchData()}catch{}
}
onMounted(async()=>{try{const r:any=await talentPlanApi.list({page:1,pageSize:100});tcpList.value=r.data?.records||[]}catch{};fetchData()})
</script>

<style scoped>
.report-page{min-height:100%}
.trigger-card{margin-bottom:16px}.trigger-row{display:flex;align-items:center;gap:12px}
.list-card{margin-bottom:16px}

.report-toolbar{display:flex;align-items:center;gap:16px;padding:12px 0;margin-bottom:20px;border-bottom:1px solid var(--color-border)}
.report-title{font-family:var(--font-display);font-size:18px;font-weight:700;color:var(--color-primary);flex:1}
.report-date{font-size:12px;color:var(--color-text-muted)}

.report-section{margin-bottom:28px;padding:20px;background:var(--color-surface);border-radius:var(--radius-md);box-shadow:var(--shadow-card)}
.section-title{font-family:var(--font-display);font-size:18px;font-weight:700;color:var(--color-primary);margin-bottom:12px;padding-bottom:8px;border-bottom:2px solid var(--color-primary);letter-spacing:0.03em}
.section-text{font-size:14px;line-height:2;color:var(--color-text);text-indent:2em;text-align:justify}

.trend-list{background:var(--color-bg);padding:16px;border-radius:var(--radius-md)}
.trend-title{font-weight:600;font-size:14px;margin-bottom:10px;color:var(--color-primary)}
.trend-item{font-size:13px;color:var(--color-text-secondary);padding:4px 0;border-bottom:1px dashed var(--color-border)}
.trend-item:last-child{border-bottom:none}

.chart-card{background:var(--color-bg);padding:12px;border-radius:var(--radius-md);margin-bottom:12px}
.chart-header{font-size:13px;font-weight:600;color:var(--color-text);margin-bottom:4px}
.chart-sm{width:100%;height:260px}
.chart-md{width:100%;height:300px}

.label-sm{font-size:12px;color:var(--color-text-muted);margin-bottom:6px}
.skill-tags{display:flex;flex-wrap:wrap;gap:6px}
.skill-tag{padding:4px 12px;background:var(--color-primary-bg);color:var(--color-primary);border-radius:20px;font-size:12px;font-weight:500}

.dual-metric{display:flex;justify-content:center}.metric-item{display:flex;align-items:center;gap:16px}
.metric-label{font-size:13px;font-weight:600;color:var(--color-text);writing-mode:vertical-lr}
.metric-detail{font-size:12px}.metric-good{color:var(--color-success);margin-bottom:2px}.metric-bad{color:var(--color-danger);margin-bottom:2px}

.employ-stats{display:flex;flex-direction:column;gap:16px}.employ-item{display:flex;flex-direction:column;gap:4px}.employ-label{font-size:12px;color:var(--color-text-muted)}.employ-val{font-size:20px;font-weight:700;color:var(--color-primary);font-family:var(--font-display)}

.gap-item{padding:14px;background:var(--color-bg);border-radius:var(--radius-md);margin-bottom:10px;border-left:3px solid var(--color-accent)}.gap-header{display:flex;align-items:center;gap:8px;margin-bottom:6px}.gap-area{font-weight:700;font-size:14px;color:var(--color-text)}.gap-desc{font-size:13px;color:var(--color-text-secondary);line-height:1.8;margin-bottom:6px}.gap-impact{font-size:12px;color:var(--color-accent);font-weight:500}

.suggest-card{padding:16px;background:var(--color-bg);border-radius:var(--radius-md);margin-bottom:10px}.suggest-meta{display:flex;gap:8px;margin-bottom:8px}.suggest-content{font-size:14px;line-height:1.8;color:var(--color-text)}

.big-score{text-align:center;padding:20px}.big-num{font-size:72px;font-weight:700;font-family:var(--font-display);line-height:1}.big-unit{font-size:18px;color:var(--color-text-muted)}.big-label{font-size:14px;color:var(--color-text-secondary);margin-top:8px}

.conclusion-box{padding:20px;background:linear-gradient(135deg,var(--color-primary-bg),#fdf6ec);border-radius:var(--radius-md);font-size:14px;line-height:2.2;color:var(--color-text);text-indent:2em;text-align:justify;margin-top:16px;border-left:4px solid var(--color-accent)}
</style>
