<template>
  <div class="office-dashboard" v-loading="loading">
    <!-- 顶部 KPI 卡片 -->
    <div class="kpi-row">
      <div class="kpi-card" v-for="card in kpiCards" :key="card.label" :style="{ '--kpi-color': card.color }">
        <div class="kpi-num">{{ card.value }}</div>
        <div class="kpi-label">{{ card.label }}</div>
        <div class="kpi-sub">{{ card.sub }}</div>
      </div>
    </div>

    <!-- 第一行图表：材料类型统计 + AI评分分布 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-header">各材料类型审核状态分布</div>
        <div ref="typeChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">AI 评分分布</div>
        <div ref="scoreChartRef" class="chart-body"></div>
      </div>
    </div>

    <!-- 第二行图表：学院分布 + 教师Top10 + 各材料平均分 -->
    <div class="chart-row">
      <div class="chart-card chart-card-half">
        <div class="chart-header">各学院各材料类型AI平均分</div>
        <div ref="collegeChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card chart-card-half">
        <div class="chart-header">教师提交 Top10</div>
        <div ref="teacherChartRef" class="chart-body"></div>
      </div>
    </div>

    <!-- 第三行：审核状态统计 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-header">审核流转概览</div>
        <div ref="flowChartRef" class="chart-body chart-body-sm"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api/common'

const loading = ref(true)
const data = ref<any>({})
const typeChartRef = ref(); const scoreChartRef = ref()
const collegeChartRef = ref(); const teacherChartRef = ref(); const flowChartRef = ref()

const kpiCards = ref([
  { label: '材料总数', value: 0, sub: '全部材料', color: '#1a3c34' },
  { label: '待教务处终审', value: 0, sub: '主任已审核待您处理', color: '#c8963e' },
  { label: '已通过', value: 0, sub: '审核完成归档', color: '#4a7c59' },
  { label: '平均分', value: '--', sub: 'AI评分均值', color: '#5b7f95' }
])

function buildTypeChart(raw: any) {
  if (!typeChartRef.value) return
  const chart = echarts.init(typeChartRef.value)
  const types = raw.typeStats || []
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: raw.statusLabels || [], bottom: 0, textStyle: { fontSize: 11 } },
    grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: types.map((t:any) => t.name), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    series: (raw.statusLabels || []).map((label: string, i: number) => ({
      name: label, type: 'bar', stack: 'total', barWidth: 50,
      data: types.map((t:any) => (t.values||[])[i]||0),
      itemStyle: { borderRadius: i===raw.statusLabels.length-1?[4,4,0,0]:0 },
      color: ['#5b7f95','#c8963e','#409EFF','#4a7c59','#b84c3d'][i]
    }))
  })
}

function buildScoreChart(scoreDist: number[]) {
  if (!scoreChartRef.value || !scoreDist) return
  const chart = echarts.init(scoreChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', right: 0, top: 'center', textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['50%', '75%'], center: ['38%', '50%'],
      label: { show: true, formatter: '{b}\n{c}份', fontSize: 10 },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold' } },
      data: [
        { value: scoreDist[0]||0, name: '60分以下', itemStyle: { color: '#b84c3d' } },
        { value: scoreDist[1]||0, name: '60-74分', itemStyle: { color: '#c8963e' } },
        { value: scoreDist[2]||0, name: '75-84分', itemStyle: { color: '#5b7f95' } },
        { value: scoreDist[3]||0, name: '85-94分', itemStyle: { color: '#409EFF' } },
        { value: scoreDist[4]||0, name: '95-100分', itemStyle: { color: '#4a7c59' } }
      ].filter(d => d.value > 0)
    }]
  })
}

/**
 * 各学院各材料类型AI平均分 — 分组柱状图
 * collegeAvgData: { "计算机学院":{"TEACHING_PLAN":80,...}, "数学学院":{...} }
 * labels: ["授课计划","教案","课件","考核方案"]
 * keys:   ["TEACHING_PLAN","LESSON_PLAN","COURSEWARE","EXAM_PLAN"]
 */
function buildCollegeChart(collegeAvgData: any, labels: string[], keys: string[]) {
  if (!collegeChartRef.value || !collegeAvgData || !Object.keys(collegeAvgData).length) return
  const chart = echarts.init(collegeChartRef.value)
  const collegeNames = Object.keys(collegeAvgData)
  // 每种材料类型生成一个柱状系列
  const colorMap: Record<string,string> = {
    TEACHING_PLAN: '#1a3c34', LESSON_PLAN: '#c8963e',
    COURSEWARE: '#409EFF', EXAM_PLAN: '#4a7c59'
  }
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: labels, bottom: 0, textStyle: { fontSize: 11 } },
    grid: { left: '3%', right: '4%', bottom: '14%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: collegeNames, axisLabel: { fontSize: 10, rotate: collegeNames.length > 5 ? 20 : 0 } },
    yAxis: { type: 'value', name: '平均分', min: 0, max: 100, axisLabel: { fontSize: 10 } },
    series: keys.map((k, i) => ({
      name: labels[i], type: 'bar', barWidth: '50%',
      data: collegeNames.map(cn => (collegeAvgData[cn] && collegeAvgData[cn][k]) ? collegeAvgData[cn][k] : 0),
      itemStyle: { color: colorMap[k] || '#999', borderRadius: [4,4,0,0] },
      emphasis: { itemStyle: { borderRadius: [4,4,0,0] } }
    }))
  })
}

function buildTeacherChart(topTeachers: any[]) {
  if (!teacherChartRef.value || !topTeachers?.length) return
  const chart = echarts.init(teacherChartRef.value)
  const names = topTeachers.map((t:any) => t.name).reverse()
  const counts = topTeachers.map((t:any) => t.count).reverse()
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '12%', bottom: '3%', top: '8%', containLabel: true },
    xAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    yAxis: { type: 'category', data: names, axisLabel: { fontSize: 10 } },
    series: [{
      type: 'bar', data: counts, label: { show: true, position: 'right', fontSize: 10, color: '#6b6258' },
      itemStyle: { color: new echarts.graphic.LinearGradient(0,0,1,0,[
        {offset:0,color:'#c8963e'},{offset:1,color:'#e8c97a'}
      ]), borderRadius: [0,4,4,0] }
    }]
  })
}

function buildFlowChart(raw: any) {
  if (!flowChartRef.value) return
  const chart = echarts.init(flowChartRef.value)
  const statusMap = (raw.statusCounts || {}) as Record<string,number>
  const labels = ['AI评审中','待主任审核','待教务处审核','已通过','已驳回']
  const keys = ['AI_EVALUATING','AI_COMPLETED','COLLEGE_APPROVED','OFFICE_APPROVED','AI_REJECTED']
  const values = keys.map(k => statusMap[k] || 0)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    series: [{
      type: 'line', data: values, smooth: true, symbol: 'circle', symbolSize: 10,
      lineStyle: { width: 3, color: '#1a3c34' }, itemStyle: { color: '#c8963e' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[
        {offset:0,color:'rgba(26,60,52,0.2)'},{offset:1,color:'rgba(26,60,52,0.02)'}
      ])}
    }]
  })
}

onMounted(async () => {
  try {
    const res: any = await dashboardApi.office()
    data.value = res.data || {}

    // KPI
    const sc = data.value.statusCounts || {}
    kpiCards.value[0].value = data.value.totalMaterials || 0
    kpiCards.value[1].value = sc['COLLEGE_APPROVED'] || 0
    kpiCards.value[2].value = sc['OFFICE_APPROVED'] || 0
    const scores = data.value.typeAvgScores || []
    const totalEval = scores.reduce((s:number,t:any)=>s+t.count,0)
    const totalSum = scores.reduce((s:number,t:any)=>s+t.avg*t.count,0)
    kpiCards.value[3].value = totalEval > 0 ? (totalSum/totalEval).toFixed(1) : '--'
    kpiCards.value[3].sub = '共' + totalEval + '份材料有评分'

    nextTick(() => {
      buildTypeChart(data.value)
      buildScoreChart(data.value.scoreDistribution)
      buildCollegeChart(data.value.collegeMaterialAvgScores, data.value.materialTypeLabels||[], data.value.materialTypeKeys||[])
      buildTeacherChart(data.value.topTeachers)
      buildFlowChart(data.value)
    })
  } catch {} finally { loading.value = false }
})
</script>

<style scoped>
.office-dashboard { min-height: 100%; }
.kpi-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.kpi-card {
  background: var(--color-surface); border-radius: var(--radius-md);
  padding: 24px; box-shadow: var(--shadow-card); position: relative; overflow: hidden;
  transition: transform 0.2s;
}
.kpi-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.kpi-card::before {
  content: ''; position: absolute; top: 0; left: 0; width: 4px; height: 100%;
  background: var(--kpi-color); border-radius: 4px 0 0 4px;
}
.kpi-num { font-size: 36px; font-weight: 700; color: var(--kpi-color); font-family: var(--font-display); }
.kpi-label { font-size: 13px; color: var(--color-text-secondary); margin-top: 6px; font-weight: 500; }
.kpi-sub { font-size: 11px; color: var(--color-text-muted); margin-top: 2px; }
.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.chart-card {
  background: var(--color-surface); border-radius: var(--radius-md);
  box-shadow: var(--shadow-card); padding: 16px;
}
.chart-card-full { grid-column: 1 / -1; }
.chart-header { font-size: 14px; font-weight: 600; color: var(--color-text); margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid var(--color-border); }
.chart-body { width: 100%; height: 320px; }
.chart-body-sm { height: 240px; }
</style>
