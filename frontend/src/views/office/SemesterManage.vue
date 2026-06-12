<template>
  <div>
    <el-card>
      <template #header><div style="display:flex;justify-content:space-between"><span>学期管理</span><el-button type="primary" @click="openDialog()">+ 新增学期</el-button></div></template>
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="code" label="代码" width="140" /><el-table-column prop="name" label="名称" min-width="200" /><el-table-column prop="startDate" label="开始日期" width="120" /><el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column label="当前学期" width="100"><template #default="{ row }"><el-tag v-if="row.isActive===1" type="success">当前</el-tag></template></el-table-column>
        <el-table-column label="操作" width="220"><template #default="{ row }"><el-button text size="small" @click="openDialog(row)">编辑</el-button><el-button v-if="row.isActive!==1" text size="small" type="primary" @click="handleActivate(row)">设为当前</el-button><el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑学期':'新增学期'" width="400px">
      <el-form :model="form" label-width="80px"><el-form-item label="代码"><el-input v-model="form.code" placeholder="如: 2025-2026-1" /></el-form-item><el-form-item label="名称"><el-input v-model="form.name" placeholder="如: 2025-2026学年第一学期" /></el-form-item><el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" placeholder="选择日期" style="width:100%" /></el-form-item><el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date" placeholder="选择日期" style="width:100%" /></el-form-item></el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { semesterApi } from '@/api/common'

const loading = ref(false); const tableData = ref<any[]>([]); const dialogVisible = ref(false); const isEdit = ref(false)
const form = reactive({ code:'', name:'', startDate:null as any, endDate:null as any }); let editingId: number|null = null

async function fetchData() { loading.value = true; try { const res: any = await semesterApi.list(); tableData.value = res.data || [] } catch {} finally { loading.value = false } }
function openDialog(row?: any) {
  if (row) { isEdit.value = true; editingId = row.id; form.code = row.code; form.name = row.name; form.startDate = row.startDate; form.endDate = row.endDate }
  else { isEdit.value = false; editingId = null; form.code = ''; form.name = ''; form.startDate = null; form.endDate = null }
  dialogVisible.value = true
}
async function handleSave() {
  try { if (editingId) { await semesterApi.update(editingId, form) } else { await semesterApi.create(form) }; ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
  catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}
async function handleActivate(row: any) { await semesterApi.activate(row.id); ElMessage.success('已设为当前学期'); fetchData() }
async function handleDelete(row: any) { await ElMessageBox.confirm('确认删除？','提示',{type:'warning'}); await semesterApi.delete(row.id); ElMessage.success('已删除'); fetchData() }
onMounted(() => fetchData())
</script>
