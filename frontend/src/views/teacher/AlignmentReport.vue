<template>
  <div class="report-page">
    <!-- 报告列表 -->
    <el-card v-if="!viewingReport">
      <template #header><span style="font-weight:600">产业需求对齐分析报告（本学院）</span></template>
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
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewReport(row)">📊 查看报告</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :total="total" layout="total, prev, pager, next" @change="fetchData" />
    </el-card>

    <!-- ====== 报告主体（与院长端共用样式，教师只能读） ====== -->
    <div v-if="viewingReport && report" class="report-body">
      <div class="report-toolbar">
        <el-button @click="viewingReport=false">← 返回列表</el-button>
        <span class="report-title">{{ report.majorName }} — 产业需求对齐分析报告</span>
        <span class="report-date">分析时间: {{ report.createdAt }}</span>
      </div>

      <div class="report-section"><h2 class="section-title">一、专业概况与产业背景</h2>
        <div class="section-text" v-if="rp.overview">{{ rp.overview }}</div>
        <el-row :gutter="16" style="margin-top:16px" v-if="rp.industryBackground">
          <el-col :span="16"><div class="section-text">{{ rp.industryBackground?.summary }}</div></el-col>
          <el-col :span="8"><div class="trend-list"><div class="trend-title">📈 产业发展趋势</div><div v-for="(t,i) in (rp.industryBackground?.trends||[])" :key="i" class="trend-item">{{ i+1 }}. {{ t }}</div></div></el-col>
        </el-row>
        <div style="margin-top:12px" v-if="rp.industryKeywords?.length"><span class="label-sm">产业关键词：</span><el-tag v-for="k in rp.industryKeywords" :key="k" size="small" effect="plain" style="margin:2px 4px 2px 0">{{ k }}</el-tag></div>
      </div>

      <div class="report-section"><h2 class="section-title">二、产业人才需求分析</h2>
        <div class="section-text" v-if="rp.demandAnalysis?.summary">{{ rp.demandAnalysis.summary }}</div>
        <el-row :gutter="16" style="margin-top:16px" v-if="rp.demandAnalysis">
          <el-col :span="14"><div class="chart-card"><div class="chart-header">岗位需求分布</div><div ref="jobsChartRef" class="chart-sm"></div></div></el-col>
          <el-col :span="10"><div class="chart-card"><div class="chart-header">技能要求雷达图</div><div ref="skillRadarRef" class="chart-sm"></div></div></el-col>
        </el-row>
      </div>

      <div class="report-section"><h2 class="section-title">三、课程体系分析</h2>
        <div class="section-text" v-if="rp.courseAnalysis?.summary">{{ rp.courseAnalysis.summary }}</div>
        <el-row :gutter="16" style="margin-top:16px" v-if="rp.courseAnalysis">
          <el-col :span="14"><div class="chart-card"><div class="chart-header">课程体系评分</div><div ref="courseRadarRef" class="chart-sm"></div></div></el-col>
          <el-col :span="10"><div class="chart-card"><div class="chart-header">核心课程排行</div><div ref="coreChartRef" class="chart-sm"></div></div></el-col>
        </el-row>
      </div>

      <div class="report-section" v-if="rp.learningOutcomes"><h2 class="section-title">四、学习成果分析</h2><div class="section-text">{{ rp.learningOutcomes?.summary }}</div></div>

      <div class="report-section" v-if="rp.employmentAnalysis"><h2 class="section-title">五、就业前景</h2><div class="section-text">{{ rp.employmentAnalysis?.summary }}</div></div>

      <div class="report-section"><h2 class="section-title">六、综合评分</h2>
        <el-row :gutter="16"><el-col :span="8"><div class="big-score"><span class="big-num" :style="{color:scoreColor(rp.coverageScore||70)}">{{ rp.coverageScore||'--' }}</span><span class="big-unit">分</span><div class="big-label">总体覆盖度</div></div></el-col><el-col :span="16"><div ref="dimRadarRef" class="chart-md"></div></el-col></el-row>
        <div v-if="rp.summary" class="conclusion-box">{{ rp.summary }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import * as echarts from 'echarts'
import { alignmentApi } from '@/api/common'

const loading=ref(false);const viewingReport=ref(false)
const tableData=ref<any[]>([]);const total=ref(0);const page=ref(1)
const report=ref<any>(null)
const jobsChartRef=ref();const skillRadarRef=ref();const courseRadarRef=ref();const coreChartRef=ref();const dimRadarRef=ref()

const rp=computed(()=>{if(!report.value?.reportJson)return{};try{return JSON.parse(report.value.reportJson)}catch{return{}}})

function scoreColor(s:number){if(s>=90)return'#4a7c59';if(s>=75)return'#409EFF';if(s>=60)return'#c8963e';return'#b84c3d'}
function gradeText(s:number){if(s>=90)return'优秀';if(s>=75)return'良好';if(s>=60)return'合格';return'待改进'}

async function viewReport(row:any){try{const r:any=await alignmentApi.reportDetail(row.id);report.value=r.data;viewingReport.value=true;await nextTick();buildCharts()}catch{}}

function buildCharts(){
  if(!rp.value)return
  const r=rparse(rp.value)
  if(jobsChartRef.value&&r.jobs){const c=echarts.init(jobsChartRef.value);c.setOption({tooltip:{trigger:'axis'},grid:{left:'3%',right:'8%',bottom:'3%',top:'10%',containLabel:true},xAxis:{type:'value'},yAxis:{type:'category',data:r.jobs.map((j:any)=>j.title),inverse:true,axisLabel:{fontSize:10}},series:[{type:'bar',data:r.jobs.map((j:any)=>j.count),itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#1a3c34'},{offset:1,color:'#4a7c59'}]),borderRadius:[0,4,4,0]},label:{show:true,position:'right',fontSize:10}}]})}
  if(skillRadarRef.value&&r.skillRadar){const c=echarts.init(skillRadarRef.value);c.setOption({tooltip:{},radar:{indicator:Object.entries(r.skillRadar).map(([k,v])=>({name:k,max:100})),center:['50%','55%'],radius:'65%'},series:[{type:'radar',data:[{value:Object.values(r.skillRadar),areaStyle:{color:'rgba(200,150,62,0.2)'},lineStyle:{color:'#c8963e',width:2}}]}]})}
  if(courseRadarRef.value&&r.courseRadar){const c=echarts.init(courseRadarRef.value);c.setOption({tooltip:{},radar:{indicator:Object.entries(r.courseRadar).map(([k,v])=>({name:k,max:100})),center:['50%','55%'],radius:'65%'},series:[{type:'radar',data:[{value:Object.values(r.courseRadar),areaStyle:{color:'rgba(26,60,52,0.15)'},lineStyle:{color:'#1a3c34',width:2}}]}]})}
  if(coreChartRef.value&&r.core){const items=r.core.reverse();const c=echarts.init(coreChartRef.value);c.setOption({tooltip:{trigger:'axis'},grid:{left:'3%',right:'8%',bottom:'3%',top:'10%',containLabel:true},xAxis:{type:'value',max:100},yAxis:{type:'category',data:items.map((i:any)=>i.name),axisLabel:{fontSize:10}},series:[{type:'bar',data:items.map((i:any)=>i.importance),itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#c8963e'},{offset:1,color:'#e8c97a'}]),borderRadius:[0,4,4,0]},label:{show:true,position:'right',fontSize:10}}]})}
  if(dimRadarRef.value&&r.dimScores){const c=echarts.init(dimRadarRef.value);c.setOption({tooltip:{},radar:{indicator:Object.entries(r.dimScores).map(([k,v])=>({name:k,max:100})),center:['50%','55%'],radius:'70%'},series:[{type:'radar',data:[{value:Object.values(r.dimScores),areaStyle:{color:'rgba(74,124,89,0.2)'},lineStyle:{color:'#4a7c59',width:2}}]}]})}
}

function rparse(raw:any){return{jobs:raw.demandAnalysis?.jobPositions||[],skillRadar:raw.demandAnalysis?.skillRadar||null,courseRadar:raw.courseAnalysis?.courseScoreRadar||null,core:(raw.courseAnalysis?.coreCourses||[]).slice(0,8),dimScores:raw.dimensionScores||null}}

async function fetchData(){loading.value=true;try{const r:any=await alignmentApi.reports({page:page.value,pageSize:10});tableData.value=r.data?.records||[];total.value=r.data?.total||0}catch{}finally{loading.value=false}}
onMounted(()=>fetchData())
</script>

<style scoped>
.report-body{min-height:100%}
.report-toolbar{display:flex;align-items:center;gap:16px;padding:12px 0;margin-bottom:20px;border-bottom:1px solid var(--color-border)}
.report-title{font-family:var(--font-display);font-size:18px;font-weight:700;color:var(--color-primary);flex:1}
.report-date{font-size:12px;color:var(--color-text-muted)}
.report-section{margin-bottom:24px;padding:20px;background:var(--color-surface);border-radius:var(--radius-md);box-shadow:var(--shadow-card)}
.section-title{font-family:var(--font-display);font-size:18px;font-weight:700;color:var(--color-primary);margin-bottom:12px;padding-bottom:8px;border-bottom:2px solid var(--color-primary);letter-spacing:0.03em}
.section-text{font-size:14px;line-height:2;color:var(--color-text);text-indent:2em;text-align:justify}
.trend-list{background:var(--color-bg);padding:16px;border-radius:var(--radius-md)}.trend-title{font-weight:600;font-size:14px;margin-bottom:10px;color:var(--color-primary)}.trend-item{font-size:13px;color:var(--color-text-secondary);padding:4px 0;border-bottom:1px dashed var(--color-border)}.trend-item:last-child{border-bottom:none}
.chart-card{background:var(--color-bg);padding:12px;border-radius:var(--radius-md);margin-bottom:12px}.chart-header{font-size:13px;font-weight:600;color:var(--color-text);margin-bottom:4px}.chart-sm{width:100%;height:260px}.chart-md{width:100%;height:300px}
.label-sm{font-size:12px;color:var(--color-text-muted);margin-bottom:6px}
.big-score{text-align:center;padding:20px}.big-num{font-size:72px;font-weight:700;font-family:var(--font-display);line-height:1}.big-unit{font-size:18px;color:var(--color-text-muted)}.big-label{font-size:14px;color:var(--color-text-secondary);margin-top:8px}
.conclusion-box{padding:20px;background:linear-gradient(135deg,var(--color-primary-bg),#fdf6ec);border-radius:var(--radius-md);font-size:14px;line-height:2.2;color:var(--color-text);text-indent:2em;text-align:justify;margin-top:16px;border-left:4px solid var(--color-accent)}
</style>
