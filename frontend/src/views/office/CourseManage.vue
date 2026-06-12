<template>
  <div>
    <el-card>
      <template #header><div style="display:flex;justify-content:space-between"><span>课程管理</span><el-button type="primary" @click="openDialog()">+ 新增课程</el-button></div></template>
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="code" label="代码" width="100" /><el-table-column prop="name" label="名称" /><el-table-column prop="creditHours" label="学分" width="80" /><el-table-column prop="theoryHours" label="理论学时" width="100" /><el-table-column prop="practiceHours" label="实践学时" width="100" />
        <el-table-column label="操作" width="160"><template #default="{ row }"><el-button text size="small" @click="openDialog(row)">编辑</el-button><el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :total="total" layout="total, prev, pager, next" @change="fetchData" />
    </el-card>
    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑课程':'新增课程'" width="500px">
      <el-form :model="form" label-width="80px"><el-form-item label="代码"><el-input v-model="form.code" /></el-form-item><el-form-item label="名称"><el-input v-model="form.name" /></el-form-item><el-form-item label="所属学院"><el-select v-model="form.collegeId" style="width:100%"><el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item><el-form-item label="学分"><el-input-number v-model="form.creditHours" :min="0" /></el-form-item><el-form-item label="理论学时"><el-input-number v-model="form.theoryHours" :min="0" /></el-form-item><el-form-item label="实践学时"><el-input-number v-model="form.practiceHours" :min="0" /></el-form-item><el-form-item label="描述"><el-input v-model="form.description" /></el-form-item></el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseApi, collegeApi } from '@/api/common'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0); const page = ref(1); const colleges = ref<any[]>([])
const dialogVisible = ref(false); const isEdit = ref(false)
const form = reactive({ code:'', name:'', collegeId:null as any, creditHours:0, theoryHours:0, practiceHours:0, description:'' }); let editingId: number|null = null

async function fetchData() { loading.value = true; try { const res: any = await courseApi.list({ page: page.value, pageSize: 10 }); tableData.value = res.data?.records || []; total.value = res.data?.total || 0 } catch {} finally { loading.value = false } }
function openDialog(row?: any) {
  if (row) { isEdit.value = true; editingId = row.id; Object.assign(form, row) } else { isEdit.value = false; editingId = null; Object.assign(form, {code:'',name:'',collegeId:null,creditHours:0,theoryHours:0,practiceHours:0,description:''}) }
  dialogVisible.value = true
}
async function handleSave() {
  try { if (editingId) { await courseApi.update(editingId, form) } else { await courseApi.create(form) }; ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}
async function handleDelete(row: any) { await ElMessageBox.confirm('确认删除？','提示',{type:'warning'}); await courseApi.delete(row.id); ElMessage.success('已删除'); fetchData() }
onMounted(async () => { try { const res: any = await collegeApi.list(); colleges.value = res.data || [] } catch {}; fetchData() })
</script>
