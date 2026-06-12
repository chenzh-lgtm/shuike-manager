<template>
  <div>
    <el-card v-loading="loading">
      <template #header><div style="display:flex;justify-content:space-between"><span>授课计划详情</span><el-button @click="$router.back()">返回</el-button></div></template>
      <el-descriptions v-if="plan" :column="2" border>
        <el-descriptions-item label="课程">{{ plan.courseId }}</el-descriptions-item>
        <el-descriptions-item label="授课班级">{{ plan.classInfo }}</el-descriptions-item>
        <el-descriptions-item label="教材">{{ plan.textbookInfo }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(plan.status)">{{ statusLabel(plan.status) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ plan.submitTime }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="plan?.summary" style="margin-top:16px"><h4>授课计划摘要</h4><div v-html="plan.summary" style="padding:10px;background:#f5f5f5;margin-top:8px;border-radius:4px"></div></div>
      <div style="margin-top:16px"><h4>附件列表</h4>
        <el-table :data="files" style="width:100%;margin-top:8px">
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column prop="fileType" label="类型" width="80" />
          <el-table-column prop="fileSize" label="大小" width="100"><template #default="{ row }">{{ (row.fileSize/1024).toFixed(1) }}KB</template></el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { teachingPlanApi } from '@/api/teachingPlan'

const route = useRoute()
const loading = ref(false)
const plan = ref<any>(null)
const files = ref<any[]>([])

function statusType(s: string) { const m: any = { DRAFT:'info', SUBMITTED:'', COLLEGE_PASSED:'warning', APPROVED:'success', REJECTED:'danger' }; return m[s] || 'info' }
function statusLabel(s: string) { const m: any = { DRAFT:'草稿', SUBMITTED:'待学院审核', COLLEGE_PASSED:'待教务处审核', APPROVED:'已通过', REJECTED:'待修改' }; return m[s] || s }

onMounted(async () => {
  const id = Number(route.params.id)
  loading.value = true
  try {
    const res: any = await teachingPlanApi.getById(id); plan.value = res.data
    const fres: any = await teachingPlanApi.getFiles(id); files.value = fres.data || []
  } catch {}
  finally { loading.value = false }
})
</script>
