<template>
  <div>
    <el-card>
      <template #header><span>校级终审列表</span></template>
      <el-table :data="tableData" v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="courseId" label="课程" width="80" />
        <el-table-column prop="teacherId" label="教师" width="80" />
        <el-table-column prop="classInfo" label="授课班级" min-width="140" show-overflow-tooltip />
        <el-table-column prop="collegeReviewTime" label="学院审核时间" width="110" />
        <el-table-column label="操作" width="160"><template #default="{ row }"><el-button type="primary" size="small" @click="$router.push('/office/final-reviews/'+row.id)">终审</el-button></template></el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :total="total" layout="total, prev, pager, next" @change="fetchData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { reviewApi } from '@/api/review'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)

async function fetchData() {
  loading.value = true
  try { const res: any = await reviewApi.officePending({ page: page.value, pageSize: 10 }); tableData.value = res.data?.records || []; total.value = res.data?.total || 0 }
  catch {} finally { loading.value = false }
}
onMounted(() => fetchData())
</script>
