<template>
  <div>
    <el-card v-loading="loading">
      <template #header><div style="display:flex;justify-content:space-between"><span>AI 评审复核</span><el-button @click="$router.back()">返回</el-button></div></template>
      <el-row :gutter="20" v-if="detail">
        <el-col :span="8" style="text-align:center"><div style="font-size:64px;font-weight:bold">{{ detail.score }}</div><div>AI 综合评分</div></el-col>
        <el-col :span="16">
          <div v-for="(v,k) in dimScores" :key="k" style="margin-bottom:8px"><span>{{ k }}</span><el-progress :percentage="v" style="width:200px;margin-left:10px" /></div>
        </el-col>
      </el-row>
      <div style="margin-top:16px" v-if="detail"><h4>优化建议</h4><div v-for="(item,idx) in groupedSuggestions" :key="idx" style="margin-bottom:8px"><el-alert :title="'【'+item.dimension+'】'" type="info" show-icon :closable="false"><template #default><ul style="margin:4px 0 0 16px"><li v-for="(iss,j) in item.issues" :key="j">{{ iss }}</li></ul><div v-if="item.direction" style="color:#909399;margin-top:6px">→ {{ item.direction }}</div></template></el-alert></div></div>
      <el-divider />
      <el-form label-width="100px">
        <el-form-item label="复核决定"><el-radio-group v-model="action"><el-radio value="CONFIRM">确认通过</el-radio><el-radio value="REJECT">驳回修改</el-radio></el-radio-group></el-form-item>
        <el-form-item label="调整评分"><el-switch v-model="modifyScore" /> <el-input-number v-if="modifyScore" v-model="modifiedScore" :min="0" :max="100" style="margin-left:10px;width:100px" /></el-form-item>
        <el-form-item label="修改理由"><el-input v-model="modifyReason" type="textarea" :rows="2" v-if="modifyScore" /></el-form-item>
        <el-form-item label="复核意见"><el-input v-model="reviewComment" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="修改要求" v-if="action==='REJECT'"><el-input v-model="revisionRequirements" type="textarea" :rows="3" placeholder="驳回时必填" /></el-form-item>
        <el-form-item label="截止日期" v-if="action==='REJECT'"><el-date-picker v-model="deadline" type="date" placeholder="选择日期" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="saving" @click="handleSubmit">确认提交</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { aiApi } from '@/api/aiEvaluation'
import { manualReviewApi } from '@/api/manualReview'

const route = useRoute(); const router = useRouter()
const loading = ref(false); const saving = ref(false); const detail = ref<any>(null)
const action = ref('CONFIRM'); const modifyScore = ref(false); const modifiedScore = ref(80)
const modifyReason = ref(''); const reviewComment = ref(''); const revisionRequirements = ref('')
const deadline = ref<Date | null>(null)

const dimScores = computed(() => { try { return JSON.parse(detail.value?.dimensionScores || '{}') } catch { return {} } })
const suggestions = computed(() => { try { return JSON.parse(detail.value?.suggestions || '[]') } catch { return [] } })
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

async function handleSubmit() {
  if (action.value === 'REJECT' && !revisionRequirements.value) { ElMessage.warning('驳回时请填写修改要求'); return }
  saving.value = true
  try {
    await manualReviewApi.review(Number(route.params.id), {
      action: action.value,
      modifiedScore: modifyScore.value ? modifiedScore.value : null,
      modifyReason: modifyReason.value,
      reviewComment: reviewComment.value,
      revisionRequirements: action.value === 'REJECT' ? revisionRequirements.value : null,
      deadline: deadline.value ? deadline.value.toISOString().split('T')[0] : null
    })
    ElMessage.success('复核完成'); router.push('/office/ai-review-manage')
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
  finally { saving.value = false }
}

onMounted(async () => {
  loading.value = true
  try { const res: any = await aiApi.getById(Number(route.params.id)); detail.value = res.data } catch {} finally { loading.value = false }
})
</script>
