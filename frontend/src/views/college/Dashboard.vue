<template>
  <div class="college-dashboard" v-loading="loading">
    <h2 class="page-title">审核工作台</h2>

    <!-- KPI 卡片 -->
    <div class="kpi-row">
      <div class="kpi-card" v-for="card in kpiCards" :key="card.label" :style="{'--kpi-color':card.color}" :class="{clickable:!!card.link}" @click="card.link && $router.push(card.link)">
        <div class="kpi-num">{{ card.value }}</div>
        <div class="kpi-label">{{ card.label }}</div>
        <div class="kpi-sub">{{ card.sub }}</div>
      </div>
    </div>

    <!-- 第一行：材料类型审核状态 + 教师提交排行 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-header">各材料类型审核状态</div>
        <div ref="typeChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">教师提交统计</div>
        <div ref="teacherChartRef" class="chart-body"></div>
      </div>
    </div>

    <!-- 第二行：教师评分对比 + AI评分分布 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-header">教师评分对比（平均分/最高分）</div>
        <div ref="scoreChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">材料类型平均分 + AI评分分布</div>
        <div ref="avgChartRef" class="chart-body"></div>
      </div>
    </div>

    <!-- 第三行：高分排行 -->
    <div class="chart-row">
      <div class="chart-card chart-card-full">
        <div class="chart-header">🏆 高分材料排行 Top10</div>
        <div ref="topChartRef" class="chart-body chart-body-sm"></div>
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
const typeChartRef = ref(); const teacherChartRef = ref()
const scoreChartRef = ref(); const avgChartRef = ref(); const topChartRef = ref()

const kpiCards = ref([
  { label:'材料总数', value:0, sub:'本院全部材料', color:'#1a3c34', link:'/college/material-reviews' },
  { label:'待主任审核', value:0, sub:'AI已完成评分待您审核', color:'#c8963e', link:'/college/material-reviews' },
  { label:'已审核通过', value:0, sub:'含待教务处终审', color:'#4a7c59' },
  { label:'已驳回', value:0, sub:'需教师修改后重提', color:'#b84c3d' }
])

function buildTypeChart(raw: any) {
  if (!typeChartRef.value) return; const c = echarts.init(typeChartRef.value)
  const types = raw.typeStats || []; const labels = raw.statusLabels || []
  c.setOption({
    tooltip:{trigger:'axis',axisPointer:{type:'shadow'}}, legend:{data:labels,bottom:0,textStyle:{fontSize:11}},
    grid:{left:'3%',right:'4%',bottom:'14%',top:'8%',containLabel:true},
    xAxis:{type:'category',data:types.map((t:any)=>t.name),axisLabel:{fontSize:11}},
    yAxis:{type:'value',axisLabel:{fontSize:10}},
    series:labels.map((l:string,i:number)=>({
      name:l,type:'bar',stack:'total',barWidth:45,
      data:types.map((t:any)=>(t.values||[])[i]||0),
      itemStyle:{borderRadius:i===labels.length-1?[4,4,0,0]:0},
      color:['#c8963e','#5b7f95','#4a7c59','#b84c3d'][i]
    }))
  })
}

function buildTeacherChart(raw: any) {
  if (!teacherChartRef.value) return; const c = echarts.init(teacherChartRef.value)
  const ts = (raw.teacherStats || []).slice(0,8)
  const types = ['授课计划','教案','课件','考核方案']
  const colors = ['#2a5c50','#5b7f95','#c8963e','#4a7c59']
  c.setOption({
    tooltip:{trigger:'axis',axisPointer:{type:'shadow'}}, legend:{data:types,bottom:0,textStyle:{fontSize:10}},
    grid:{left:'3%',right:'4%',bottom:'14%',top:'8%',containLabel:true},
    xAxis:{type:'category',data:ts.map((t:any)=>t.name),axisLabel:{fontSize:10,rotate:15}},
    yAxis:{type:'value',axisLabel:{fontSize:10}},
    series:types.map((tp:string,i:number)=>({
      name:tp,type:'bar',stack:'total',barWidth:36,
      data:ts.map((t:any)=>{
        const td=t.typeDetails||{}
        return (td['TEACHING_PLAN']&&i===0?td['TEACHING_PLAN']:0)+
               (td['LESSON_PLAN']&&i===1?td['LESSON_PLAN']:0)+
               (td['COURSEWARE']&&i===2?td['COURSEWARE']:0)+
               (td['EXAM_PLAN']&&i===3?td['EXAM_PLAN']:0)
      }),
      color:colors[i]
    }))
  })
}

function buildScoreChart(raw: any) {
  if (!scoreChartRef.value) return; const c = echarts.init(scoreChartRef.value)
  const ts = raw.teacherScores || []
  c.setOption({
    tooltip:{trigger:'axis'}, legend:{data:['平均分','最高分','最低分'],bottom:0,textStyle:{fontSize:10}},
    grid:{left:'3%',right:'4%',bottom:'14%',top:'8%',containLabel:true},
    xAxis:{type:'category',data:ts.map((t:any)=>t.name),axisLabel:{fontSize:10,rotate:15}},
    yAxis:{type:'value',min:0,max:100,axisLabel:{fontSize:10}},
    series:[
      {name:'平均分',type:'bar',data:ts.map((t:any)=>t.avg),itemStyle:{color:'#1a3c34',borderRadius:[6,6,0,0]},barWidth:16},
      {name:'最高分',type:'bar',data:ts.map((t:any)=>t.max),itemStyle:{color:'#4a7c59',borderRadius:[6,6,0,0]},barWidth:16},
      {name:'最低分',type:'bar',data:ts.map((t:any)=>t.min),itemStyle:{color:'#c8963e',borderRadius:[6,6,0,0]},barWidth:16}
    ]
  })
}

function buildAvgChart(raw: any) {
  if (!avgChartRef.value) return; const c = echarts.init(avgChartRef.value)
  const ts = raw.typeAvgScores || []
  const dist = raw.scoreDistribution || [0,0,0,0,0]
  c.setOption({
    tooltip:{trigger:'axis'},
    grid:[
      {left:'5%',right:'55%',bottom:'5%',top:'10%'},
      {left:'55%',right:'5%',bottom:'5%',top:'10%'}
    ],
    xAxis:[
      {gridIndex:0,type:'value',min:0,max:100,name:'平均分',nameTextStyle:{fontSize:10}},
      {gridIndex:1}
    ],
    yAxis:[
      {gridIndex:0,type:'category',data:ts.map((t:any)=>t.name),inverse:true,axisLabel:{fontSize:11}},
      {gridIndex:1}
    ],
    series:[
      {type:'bar',xAxisIndex:0,yAxisIndex:0,
       data:ts.map((t:any)=>({value:t.avg,itemStyle:{color:t.avg>=75?'#4a7c59':t.avg>=60?'#c8963e':'#b84c3d',borderRadius:[0,6,6,0]}})),
       label:{show:true,position:'right',fontSize:11,formatter:'{c}分'}},
      {type:'pie',xAxisIndex:1,yAxisIndex:1,center:['50%','50%'],radius:['45%','70%'],
       label:{fontSize:10},emphasis:{label:{fontSize:13}},
       data:[
         {value:dist[0]||0,name:'< 60分',itemStyle:{color:'#b84c3d'}},
         {value:dist[1]||0,name:'60-74分',itemStyle:{color:'#c8963e'}},
         {value:dist[2]||0,name:'75-84分',itemStyle:{color:'#5b7f95'}},
         {value:dist[3]||0,name:'85-94分',itemStyle:{color:'#409EFF'}},
         {value:dist[4]||0,name:'95-100分',itemStyle:{color:'#4a7c59'}}
       ].filter(d=>d.value>0)}
    ]
  })
}

function buildTopChart(raw: any) {
  if (!topChartRef.value) return; const c = echarts.init(topChartRef.value)
  const items = (raw.topMaterials || []).reverse()
  const typeLabels:Record<string,string>={TEACHING_PLAN:'授',LESSON_PLAN:'教',COURSEWARE:'课',EXAM_PLAN:'考'}
  c.setOption({
    tooltip:{trigger:'axis',axisPointer:{type:'shadow'}},
    grid:{left:'3%',right:'10%',bottom:'3%',top:'8%',containLabel:true},
    xAxis:{type:'value',name:'评分',axisLabel:{fontSize:10}},
    yAxis:{type:'category',
      data:items.map((t:any)=> (t.teacherName||'')+' - '+(typeLabels[t.materialType]||t.materialType)),
      axisLabel:{fontSize:10}},
    series:[{type:'bar',
      data:items.map((t:any,i:number)=> ({
        value:t.score,
        itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,0,[
          {offset:0,color:'#1a3c34'},{offset:1,color:i<3?'#c8963e':'#4a7c59'}
        ]),borderRadius:[0,4,4,0]}
      })),
      label:{show:true,position:'right',fontSize:11,formatter:'{c}分',color:'#6b6258'}
    }]
  })
}

onMounted(async()=>{
  try{
    const res:any=await dashboardApi.college()
    data.value=res.data||{}
    kpiCards.value[0].value=data.value.totalMaterials||0
    kpiCards.value[1].value=data.value.pendingReview||0
    kpiCards.value[2].value=data.value.reviewed||0
    kpiCards.value[3].value=data.value.rejected||0
    nextTick(()=>{
      buildTypeChart(data.value)
      buildTeacherChart(data.value)
      buildScoreChart(data.value)
      buildAvgChart(data.value)
      buildTopChart(data.value)
    })
  }catch{}finally{loading.value=false}
})
</script>

<style scoped>
.page-title{font-family:var(--font-display);font-size:22px;font-weight:700;color:var(--color-primary);margin-bottom:var(--space-lg);letter-spacing:0.04em}
.kpi-row{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:20px}
.kpi-card{background:var(--color-surface);border-radius:var(--radius-md);padding:24px;box-shadow:var(--shadow-card);position:relative;overflow:hidden;transition:transform 0.2s}
.kpi-card.clickable{cursor:pointer}
.kpi-card:hover{transform:translateY(-2px);box-shadow:var(--shadow-md)}
.kpi-card::before{content:'';position:absolute;top:0;left:0;width:4px;height:100%;background:var(--kpi-color);border-radius:4px 0 0 4px}
.kpi-num{font-size:36px;font-weight:700;color:var(--kpi-color);font-family:var(--font-display)}
.kpi-label{font-size:13px;color:var(--color-text-secondary);margin-top:6px;font-weight:500}
.kpi-sub{font-size:11px;color:var(--color-text-muted);margin-top:2px}
.chart-row{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px}
.chart-card{background:var(--color-surface);border-radius:var(--radius-md);box-shadow:var(--shadow-card);padding:16px}
.chart-card-full{grid-column:1/-1}
.chart-header{font-size:14px;font-weight:600;color:var(--color-text);margin-bottom:8px;padding-bottom:8px;border-bottom:1px solid var(--color-border)}
.chart-body{width:100%;height:340px}
.chart-body-sm{height:280px}
</style>
