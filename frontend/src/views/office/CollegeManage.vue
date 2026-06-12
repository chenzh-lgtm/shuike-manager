<template>
  <div>
    <el-card>
      <template #header><div style="display:flex;justify-content:space-between"><span>学院管理</span><el-button type="primary" @click="openDialog()">+ 新增学院</el-button></div></template>
      <el-table :data="tableData" v-loading="loading"><el-table-column prop="code" label="代码" width="100" /><el-table-column prop="name" label="名称" /><el-table-column prop="description" label="描述" /><el-table-column label="操作" width="160"><template #default="{ row }"><el-button text size="small" @click="openDialog(row)">编辑</el-button><el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button></template></el-table-column></el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑学院':'新增学院'" width="400px"><el-form :model="form" label-width="80px"><el-form-item label="代码"><el-input v-model="form.code" /></el-form-item><el-form-item label="名称"><el-input v-model="form.name" /></el-form-item><el-form-item label="描述"><el-input v-model="form.description" /></el-form-item></el-form><template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template></el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { collegeApi } from '@/api/common'

const loading = ref(false); const tableData = ref<any[]>([]); const dialogVisible = ref(false); const isEdit = ref(false)
const form = reactive({ code: '', name: '', description: '' }); let editingId: number | null = null

async function fetchData() { loading.value = true; try { const res: any = await collegeApi.list(); tableData.value = res.data || [] } catch {} finally { loading.value = false } }
function openDialog(row?: any) {
  if (row) { isEdit.value = true; editingId = row.id; form.code = row.code; form.name = row.name; form.description = row.description || '' }
  else { isEdit.value = false; editingId = null; form.code = ''; form.name = ''; form.description = '' }
  dialogVisible.value = true
}
async function handleSave() {
  try { if (editingId) { await collegeApi.update(editingId, form) } else { await collegeApi.create(form) }; ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
  catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}
async function handleDelete(row: any) { await ElMessageBox.confirm('确认删除？','提示',{type:'warning'}); await collegeApi.delete(row.id); ElMessage.success('已删除'); fetchData() }
onMounted(() => fetchData())
</script>
