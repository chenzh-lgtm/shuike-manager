<template>
  <div>
    <el-card>
      <template #header><div style="display:flex;justify-content:space-between"><span>课程标准管理</span><el-button type="primary" @click="$router.push('/dean/course-standards/create')">+ 新增</el-button></div></template>
      <el-table :data="tableData" v-loading="loading" style="width:100%">
        <el-table-column label="关联课程" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.courseName || ('课程'+row.courseId) }}</template>
        </el-table-column>
        <el-table-column label="文档" width="90" align="center"><template #default="{ row }"><el-tag :type="row.contentText?'success':'info'" size="small">{{ row.contentText?'已解析':'未上传' }}</el-tag></template></el-table-column>
        <el-table-column prop="status" label="状态" width="80"><template #default="{ row }"><el-tag :type="row.status==='ACTIVE'?'success':'info'" size="small">{{ row.status==='ACTIVE'?'启用':'归档' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="200"><template #default="{ row }"><el-button text size="small" @click="$router.push('/dean/course-standards/'+row.id+'/edit')">编辑</el-button><el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :total="total" layout="total, prev, pager, next" @change="fetchData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseStandardApi } from '@/api/common'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0); const page = ref(1)

async function fetchData() { loading.value = true; try { const res: any = await courseStandardApi.list({ page: page.value, pageSize: 10 }); tableData.value = res.data?.records || []; total.value = res.data?.total || 0 } catch {} finally { loading.value = false } }
async function handleDelete(row: any) { await ElMessageBox.confirm('确认删除？','提示',{type:'warning'}); await courseStandardApi.delete(row.id); ElMessage.success('已删除'); fetchData() }
onMounted(() => fetchData())
</script>
