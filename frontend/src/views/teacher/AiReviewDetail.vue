<template>
  <div>
    <div style="margin-bottom:16px">
      <el-button @click="$router.back()">← 返回</el-button>
      <el-button type="primary" @click="$router.push('/teacher/ai-review')">我的评审列表</el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card v-loading="loading" shadow="hover">
          <div style="text-align:center">
            <div style="font-size:14px;color:#909399;margin-bottom:16px">AI 综合评分</div>
            <el-progress v-if="detail" type="dashboard" :percentage="detail.score||0" :width="160" :stroke-width="10" :color="scoreColor(detail.score||0)">
              <template #default="{ percentage }">
                <div style="font-size:48px;font-weight:bold;line-height:1">{{ percentage }}</div>
                <div style="font-size:14px;color:#909399;margin-top:4px">{{ scoreGrade(detail.score||0) }}</div>
              </template>
            </el-progress>
            <div style="margin-top:12px">
              <el-tag :type="statusTagType(detail?.status)" size="small">{{ statusLabel(detail?.status) }}</el-tag>
              <span v-if="detail?.evalTime" style="margin-left:8px;color:#909399;font-size:13px">{{ detail.evalTime }}</span>
            </div>
          </div>
        </el-card>
        <el-card v-if="review" style="margin-top:16px" shadow="hover">
          <template #header><span>教务处复核意见</span></template>
          <el-result :icon="review.action==='CONFIRM'?'success':'error'" :title="review.action==='CONFIRM'?'已确认通过':'驳回修改'" :sub-title="review.reviewComment">
            <template v-if="review.action==='REJECT'" #extra>
              <div style="text-align:left;font-size:13px;color:#909399">
                <p><strong>修改要求：</strong>{{ review.revisionRequirements }}</p>
                <p><strong>截止日期：</strong>{{ review.deadline }}</p>
                <el-button type="primary" size="small" @click="$router.push('/teacher/teaching-plans')">去修改</el-button>
              </div>
            </template>
          </el-result>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card v-loading="loading" shadow="hover">
          <template #header><span>四维度评分详情</span></template>
          <div v-for="(item, dim) in dimensionItems" :key="dim" style="margin-bottom:20px;padding:12px;background:#fafafa;border-radius:6px">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
              <div><span style="font-size:15px;font-weight:bold">{{ item.label }}</span><el-tag size="small" style="margin-left:8px">权重 {{ item.weight }}%</el-tag></div>
              <span :style="{fontSize:'24px',fontWeight:'bold',color:scoreColor(dimScores[dim]||0)}">{{ dimScores[dim] || '--' }}<span style="font-size:14px">分</span></span>
            </div>
            <el-progress :percentage="dimScores[dim]||0" :color="scoreColor(dimScores[dim]||0)" :stroke-width="8" />
          </div>
        </el-card>

        <el-card v-if="groupedSuggestions.length" style="margin-top:16px" shadow="hover">
          <template #header><span>AI 优化建议（{{ groupedSuggestions.length }} 个维度）</span></template>
          <el-timeline>
            <el-timeline-item v-for="(item, idx) in groupedSuggestions" :key="idx" type="primary" placement="top">
              <el-alert :title="'【' + item.dimension + '】'" type="warning" show-icon :closable="false">
                <template #default>
                  <ul style="margin:4px 0 0 16px;font-size:13px">
                    <li v-for="(iss, j) in item.issues" :key="j" style="margin-bottom:3px">{{ iss }}</li>
                  </ul>
                  <div v-if="item.direction" style="color:#909399;margin-top:6px">→ {{ item.direction }}</div>
                </template>
              </el-alert>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card v-else-if="detail?.status==='COMPLETED'" style="margin-top:16px" shadow="hover">
          <el-result icon="success" title="评审通过" sub-title="所有维度评分均达到良好及以上" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { aiApi } from '@/api/aiEvaluation'
import { manualReviewApi } from '@/api/manualReview'

const route = useRoute()
const loading = ref(false)
const detail = ref<any>(null)
const review = ref<any>(null)

const dimensionItems: Record<string, any> = {
  completeness: { label: '内容完整性', weight: 28 },
  standard_match: { label: '与课程标准匹配度', weight: 30 },
  format: { label: '格式规范性', weight: 20 },
  innovation: { label: '创新性', weight: 16 },
  ai_generated: { label: 'AI生成检测', weight: 10 }
}

const dimScores = computed(() => {
  if (!detail.value?.dimensionScores) return {}
  try { return JSON.parse(detail.value.dimensionScores) } catch { return {} }
})

const suggestions = computed(() => {
  if (!detail.value?.suggestions) return []
  try { return JSON.parse(detail.value.suggestions) } catch { return [] }
})
const groupedSuggestions = computed(() => {
  if (!suggestions.value || !Array.isArray(suggestions.value)) return []
  const map: Record<string, any> = {}
  for (const s of suggestions.value) {
    const dim = s.dimension || '综合'
    if (!map[dim]) map[dim] = { dimension: dim, issues: [], direction: '' }
    map[dim].issues.push(s.issue || '')
    if (s.direction && !map[dim].direction) map[dim].direction = s.direction
  }
  return Object.values(map)
})

function scoreColor(s: number) {
  if (s >= 90) return '#67C23A'; if (s >= 75) return '#409EFF'
  if (s >= 60) return '#E6A23C'; return '#F56C6C'
}

function scoreGrade(s: number) {
  if (s >= 90) return '优秀'; if (s >= 75) return '良好'
  if (s >= 60) return '合格'; return '待改进'
}

function statusTagType(status: string) {
  const m: any = { PENDING:'info', EVALUATING:'warning', COMPLETED:'success', FAILED:'danger' }; return m[status] || 'info'
}

function statusLabel(status: string) {
  const m: any = { PENDING:'排队中', EVALUATING:'AI评审中', COMPLETED:'已完成', FAILED:'失败' }; return m[status] || status
}

onMounted(async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res: any = await aiApi.getById(id); detail.value = res.data
    const revRes: any = await manualReviewApi.byEvaluation(id)
    review.value = revRes.data?.[0] || null
  } catch {} finally { loading.value = false }
})
</script>
