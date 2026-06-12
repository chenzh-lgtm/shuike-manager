<template>
  <div class="dashboard">
    <h2 class="page-title">院长工作台</h2>

    <div class="stat-grid">
      <div class="stat-card" @click="$router.push('/dean/talent-plans')">
        <div class="stat-value primary">{{ stats.talentPlanCount }}</div>
        <div class="stat-label">人培方案总数</div>
      </div>
      <div class="stat-card" @click="$router.push('/dean/course-standards')">
        <div class="stat-value accent">{{ stats.courseStandardCount }}</div>
        <div class="stat-label">课程标准总数</div>
      </div>
    </div>

    <div class="quick-row">
      <div class="quick-card" @click="$router.push('/dean/talent-plans/create')">
        <span class="quick-icon">▦</span>
        <div class="quick-title">新增人培方案</div>
        <div class="quick-desc">上传专业人才培养方案文档</div>
      </div>
      <div class="quick-card" @click="$router.push('/dean/course-standards/create')">
        <span class="quick-icon">▤</span>
        <div class="quick-title">新增课程标准</div>
        <div class="quick-desc">上传课程教学标准文档</div>
      </div>
      <div class="quick-card" @click="$router.push('/dean/alignment-reports')">
        <span class="quick-icon">⊟</span>
        <div class="quick-title">分析产业需求</div>
        <div class="quick-desc">产业需求与人培方案分析</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { dashboardApi } from '@/api/common'
const stats = reactive({ talentPlanCount: 0, courseStandardCount: 0 })
onMounted(async () => { try { const s: any = await dashboardApi.dean(); Object.assign(stats, s.data) } catch {} })
</script>

<style scoped>
.page-title { font-family:var(--font-display); font-size:22px; font-weight:700; color:var(--color-primary); margin-bottom:var(--space-lg); letter-spacing:0.04em; }
.stat-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:var(--space-md); margin-bottom:var(--space-lg); }
.stat-card { background:var(--color-surface); border-radius:var(--radius-md); padding:24px; text-align:center; cursor:pointer; box-shadow:var(--shadow-card); transition:all var(--transition); }
.stat-card:hover { transform:translateY(-2px); box-shadow:var(--shadow-md); }
.stat-value { font-size:36px; font-weight:700; font-family:var(--font-display); }
.stat-value.primary{color:var(--color-primary)} .stat-value.accent{color:var(--color-accent)}
.stat-label{font-size:13px;color:var(--color-text-muted);margin-top:6px;letter-spacing:0.02em}
.quick-row { display:grid; grid-template-columns:repeat(3,1fr); gap:var(--space-md); }
.quick-card { background:var(--color-surface); border-radius:var(--radius-md); padding:28px; text-align:center; cursor:pointer; box-shadow:var(--shadow-card); transition:all var(--transition); border:1px solid transparent; }
.quick-card:hover { border-color:var(--color-primary); transform:translateY(-2px); box-shadow:var(--shadow-md); }
.quick-icon { font-size:28px; display:block; margin-bottom:12px; }
.quick-title { font-size:15px; font-weight:600; color:var(--color-text); margin-bottom:4px; }
.quick-desc { font-size:12px; color:var(--color-text-muted); }
</style>
