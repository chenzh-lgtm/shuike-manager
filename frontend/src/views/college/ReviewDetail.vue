<template>
  <div>
    <el-card v-loading="loading">
      <template #header><div style="display:flex;justify-content:space-between"><span>审核详情</span><el-button @click="$router.back()">返回</el-button></div></template>
      <el-descriptions v-if="plan" :column="2" border>
        <el-descriptions-item label="课程ID">{{ plan.courseId }}</el-descriptions-item>
        <el-descriptions-item label="授课班级">{{ plan.classInfo }}</el-descriptions-item>
        <el-descriptions-item label="教材">{{ plan.textbookInfo }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag>{{ plan.status }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ plan.submitTime }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:20px" v-if="plan?.summary"><h4>授课计划摘要</h4><div v-html="plan.summary" style="padding:10px;background:#f5f5f5;border-radius:4px;margin-top:8px"></div></div>
      <div style="margin-top:20px"><h4>附件</h4><el-table :data="files" style="width:100%;margin-top:8px"><el-table-column prop="fileName" label="文件名" /><el-table-column prop="fileType" label="类型" width="80" /></el-table></div>
      <div style="margin-top:20px"><h4>审核历史</h4><el-timeline><el-timeline-item v-for="r in history" :key="r.id" :timestamp="r.reviewTime" :type="r.action==='APPROVE'?'success':'danger'">{{ r.reviewLevel==='COLLEGE'?'学院':'教务处' }}-{{ r.action==='APPROVE'?'通过':'驳回' }}: {{ r.comment }}</el-timeline-item></el-timeline></div>
      <el-divider />
      <el-form v-if="plan?.status==='SUBMITTED'" label-width="80px">
        <el-form-item label="审核意见"><el-input v-model="comment" type="textarea" :rows="3" placeholder="驳回时必填" /></el-form-item>
        <el-form-item><el-button type="success" @click="handleReview('APPROVE')">✅ 通过，提交教务处</el-button><el-button type="danger" @click="handleReview('REJECT')">❌ 驳回修改</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { teachingPlanApi } from '@/api/teachingPlan'
import { reviewApi } from '@/api/review'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const plan = ref<any>(null)
const files = ref<any[]>([])
const history = ref<any[]>([])
const comment = ref('')

async function handleReview(action: string) {
  if (action === 'REJECT' && !comment.value) { ElMessage.warning('驳回时请填写审核意见'); return }
  await reviewApi.collegeReview(Number(route.params.id), { action, comment: comment.value })
  ElMessage.success('审核完成'); router.push('/college/reviews')
}

onMounted(async () => {
  loading.value = true
  const id = Number(route.params.id)
  try {
    const res: any = await teachingPlanApi.getById(id); plan.value = res.data
    const fres: any = await teachingPlanApi.getFiles(id); files.value = fres.data || []
    const hres: any = await reviewApi.history(id); history.value = hres.data || []
  } catch {} finally { loading.value = false }
})
</script>
