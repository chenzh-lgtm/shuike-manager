<template>
  <div>
    <el-card>
      <template #header><span>待审核列表</span></template>
      <div style="margin-bottom:16px">
        <el-button type="primary" :disabled="!selectedIds.length" @click="batchApprove">批量通过({{ selectedIds.length }})</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" style="width:100%" @selection-change="handleSelect">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="courseId" label="课程" width="80" />
        <el-table-column prop="teacherId" label="教师ID" width="80" />
        <el-table-column prop="classInfo" label="授课班级" min-width="140" show-overflow-tooltip />
        <el-table-column prop="submitTime" label="提交时间" width="110" />
        <el-table-column label="操作" width="160"><template #default="{ row }"><el-button type="primary" size="small" @click="$router.push('/college/reviews/'+row.id)">审核</el-button></template></el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :total="total" layout="total, prev, pager, next" @change="fetchData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { reviewApi } from '@/api/review'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const selectedIds = ref<number[]>([])

function handleSelect(rows: any[]) { selectedIds.value = rows.map((r: any) => r.id) }

async function fetchData() {
  loading.value = true
  try { const res: any = await reviewApi.collegePending({ page: page.value, pageSize: 10 }); tableData.value = res.data?.records || []; total.value = res.data?.total || 0 }
  catch {} finally { loading.value = false }
}

async function batchApprove() {
  await reviewApi.collegeBatch({ planIds: selectedIds.value, action: 'APPROVE', comment: '批量通过' })
  ElMessage.success('批量审核完成'); selectedIds.value = []; fetchData()
}

onMounted(() => fetchData())
</script>
