<template>
  <div>
    <el-card>
      <template #header><div style="display:flex;justify-content:space-between;align-items:center"><span>我的授课计划</span><el-button type="primary" @click="$router.push('/teacher/teaching-plans/create')">+ 提交授课计划</el-button></div></template>
      <el-form :inline="true" :model="query">
        <el-form-item label="学期"><el-select v-model="query.semesterId" clearable placeholder="全部" @change="fetchData"><el-option v-for="s in semesters" :key="s.id" :label="s.name" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.status" clearable placeholder="全部" @change="fetchData"><el-option label="草稿" value="DRAFT" /><el-option label="待学院审核" value="SUBMITTED" /><el-option label="待教务处审核" value="COLLEGE_PASSED" /><el-option label="已通过" value="APPROVED" /><el-option label="待修改" value="REJECTED" /></el-select></el-form-item>
      </el-form>
      <el-table :data="tableData" v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="courseId" label="课程ID" width="80" />
        <el-table-column prop="classInfo" label="授课班级" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="110" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="$router.push('/teacher/teaching-plans/'+row.id)">查看</el-button>
            <el-button v-if="row.status==='DRAFT'||row.status==='REJECTED'" text size="small" type="primary" @click="$router.push('/teacher/teaching-plans/'+row.id+'/edit')">编辑</el-button>
            <el-button v-if="row.status==='SUBMITTED'" text size="small" type="warning" @click="handleWithdraw(row)">撤回</el-button>
            <el-button v-if="row.status==='DRAFT'" text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.page" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @change="fetchData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { teachingPlanApi } from '@/api/teachingPlan'
import { semesterApi } from '@/api/common'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const semesters = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, semesterId: null as any, status: null as any })

function statusType(s: string) { const m: any = { DRAFT:'info', SUBMITTED:'', COLLEGE_PASSED:'warning', APPROVED:'success', REJECTED:'danger' }; return m[s] || 'info' }
function statusLabel(s: string) { const m: any = { DRAFT:'草稿', SUBMITTED:'待学院审核', COLLEGE_PASSED:'待教务处审核', APPROVED:'已通过', REJECTED:'待修改' }; return m[s] || s }

async function fetchData() {
  loading.value = true
  try { const res: any = await teachingPlanApi.list(query); tableData.value = res.data?.records || []; total.value = res.data?.total || 0 }
  catch {}
  finally { loading.value = false }
}

async function handleWithdraw(row: any) {
  await ElMessageBox.confirm('确认撤回该授课计划？', '提示', { type: 'warning' })
  await teachingPlanApi.withdraw(row.id)
  ElMessage.success('已撤回'); fetchData()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除该授课计划？', '提示', { type: 'warning' })
  await teachingPlanApi.delete(row.id)
  ElMessage.success('已删除'); fetchData()
}

onMounted(async () => {
  try { const res: any = await semesterApi.list(); semesters.value = res.data || [] } catch {}
  fetchData()
})
</script>
