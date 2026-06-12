<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:bold;font-size:16px">用户管理</span>
          <div style="display:flex;gap:8px">
            <el-button @click="downloadTemplate">📥 下载导入模板</el-button>
            <el-button type="success" @click="openImportDialog">📁 批量导入</el-button>
            <el-button type="primary" @click="openDialog()">+ 新增用户</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true">
        <el-form-item label="学院"><el-select v-model="query.collegeId" clearable @change="fetchData"><el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item><el-input v-model="query.keyword" placeholder="用户名/姓名" clearable @change="fetchData" /></el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width:100%">
        <el-table-column prop="username" label="用户名(工号)" width="130" />
        <el-table-column prop="realName" label="姓名" width="90" />
        <el-table-column label="学院" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.collegeName || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="r in (row.roles||[])" :key="r" size="small" style="margin:1px 2px">{{ roleLabel(r) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }"><el-tag :type="row.status===1?'success':'danger'" size="small">{{ row.status===1?'启用':'禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text size="small" @click="toggleStatus(row)">{{ row.status===1?'禁用':'启用' }}</el-button>
            <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="query.page" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑用户':'新增用户'" width="500px">
      <el-form :model="form" ref="formRef" label-width="80px">
        <el-form-item label="工号" required><el-input v-model="form.username" placeholder="用户名/工号" /></el-form-item>
        <el-form-item label="密码" :required="!isEdit">
          <el-input v-model="form.password" type="password" :placeholder="isEdit?'不修改请留空':'默认fzrjxy+工号'" />
          <div style="font-size:11px;color:var(--color-text-muted)">不填则默认: fzrjxy + 工号</div>
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="所属学院">
          <el-select v-model="form.collegeId" style="width:100%" filterable placeholder="选择学院">
            <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="form.roles">
            <el-checkbox value="TEACHER">教师</el-checkbox>
            <el-checkbox value="COLLEGE_REVIEWER">专业主任</el-checkbox>
            <el-checkbox value="OFFICE">教务处</el-checkbox>
            <el-checkbox value="DEAN">院长</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="importVisible" title="批量导入用户" width="650px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>导入说明</template>
        <ul style="margin:4px 0 0 14px;font-size:13px;color:#606266">
          <li>支持 CSV/TXT 格式，每行一个用户，逗号分隔</li>
          <li>格式：<b>工号,姓名,密码(可选),学院名称或ID,角色(用-连接)</b></li>
          <li>密码留空则默认 <code>fzrjxy+工号</code></li>
          <li>学院可填名称(如"计算机学院")或ID(如"2")</li>
          <li>角色：教师/主任/教务处/院长，多个角色用 - 连接，如"教师-主任"</li>
        </ul>
      </el-alert>

      <el-upload ref="importUploadRef" drag :auto-upload="false" :limit="1" :on-change="handleImportFile" :file-list="importFileList" accept=".csv,.txt">
        <div style="padding:30px 0"><div style="font-size:48px">📁</div><div>拖拽或点击选择导入文件</div></div>
      </el-upload>

      <div v-if="importResult" style="margin-top:16px">
        <el-alert :type="importResult.fail>0?'warning':'success'" :closable="false" show-icon :title="'导入完成：成功'+importResult.success+'条，失败'+importResult.fail+'条'" />
        <div v-if="importResult.errors && importResult.errors.length" style="margin-top:8px;max-height:200px;overflow-y:auto">
          <div v-for="(e,i) in importResult.errors" :key="i" style="font-size:12px;color:var(--color-danger);padding:2px 0">{{ e }}</div>
        </div>
      </div>

      <template #footer>
        <el-button @click="importVisible=false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="doImport" :disabled="!importFile">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi, collegeApi } from '@/api/common'
import http from '@/api/index'

const loading = ref(false); const tableData = ref<any[]>([]); const total = ref(0); const colleges = ref<any[]>([])
const dialogVisible = ref(false); const isEdit = ref(false); const formRef = ref()
const query = reactive({ page: 1, pageSize: 10, collegeId: null as any, keyword: '' })
const form = reactive({ username: '', password: '', realName: '', collegeId: null as any, roles: [] as string[] })
let editingId: number | null = null

const importVisible = ref(false); const importing = ref(false)
const importFile = ref<File | null>(null); const importFileList = ref<any[]>([]); const importResult = ref<any>(null)

function roleLabel(r: string) { const m: any = { TEACHER:'教师', COLLEGE_REVIEWER:'专业主任', OFFICE:'教务处', DEAN:'院长' }; return m[r] || r }

async function fetchData() {
  loading.value = true
  try { const res: any = await userApi.list(query); tableData.value = res.data?.records || []; total.value = res.data?.total || 0 } catch {} finally { loading.value = false }
}

function openDialog(row?: any) {
  if (row) {
    isEdit.value = true; editingId = row.id; dialogVisible.value = true
    form.username = row.username; form.password = ''; form.realName = row.realName
    form.collegeId = row.collegeId; form.roles = row.roles || []
  } else {
    isEdit.value = false; editingId = null; dialogVisible.value = true
    form.username = ''; form.password = ''; form.realName = ''; form.collegeId = null; form.roles = []
  }
}

async function handleSave() {
  const data: any = { username: form.username, realName: form.realName, collegeId: form.collegeId, roles: form.roles }
  if (!isEdit.value) data.password = form.password || ('fzrjxy' + form.username)
  else if (form.password) data.password = form.password
  try {
    if (editingId) { await userApi.update(editingId, data) } else { await userApi.create(data) }
    ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

async function toggleStatus(row: any) {
  try { await userApi.updateStatus(row.id, row.status === 1 ? 0 : 1); ElMessage.success('已更新'); fetchData() } catch { ElMessage.error('操作失败') }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' })
  try { await userApi.delete(row.id); ElMessage.success('已删除'); fetchData() } catch {}
}

function downloadTemplate() {
  window.open('/api/users/template', '_blank')
}

function openImportDialog() { importFile.value = null; importFileList.value = []; importResult.value = null; importVisible.value = true }

function handleImportFile(file: any) { importFile.value = file.raw; importFileList.value = [file] }

async function doImport() {
  if (!importFile.value) { ElMessage.warning('请先选择文件'); return }
  importing.value = true
  try {
    const formData = new FormData(); formData.append('file', importFile.value)
    const res: any = await http.post('/users/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    importResult.value = res.data || {}
    ElMessage.success(res.message || '导入完成')
    fetchData()
  } catch (e: any) { ElMessage.error('导入失败') } finally { importing.value = false }
}

onMounted(async () => {
  try { const r: any = await collegeApi.list(); colleges.value = r.data || [] } catch {}
  fetchData()
})
</script>
