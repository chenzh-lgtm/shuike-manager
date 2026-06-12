<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>AI Prompt 模板管理</span>
          <el-tag type="warning">编辑 Prompt 将自动创建新版本并激活</el-tag>
        </div>
      </template>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>Prompt 模板说明</template>
        <template #default>
          每个材料类型对应 4 个评审维度（内容完整性/课程标准匹配度/格式规范性/创新性），共 16 个专用 Prompt 模板。
          修改 Prompt 后自动保存为新版本并立即生效，旧版本保留可回滚。
        </template>
      </el-alert>

      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane v-for="(label, mt) in materialLabels" :key="mt" :label="label">
          <el-table :data="getTabData(mt)" v-loading="loading" border stripe style="width:100%">
            <el-table-column label="评审维度" width="160">
              <template #default="{ row }">{{ dimensionLabels[row.dimension] || row.dimension }}</template>
            </el-table-column>
            <el-table-column label="版本" width="80" align="center">
              <template #default="{ row }">{{ row.version }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isActive===1?'success':'info'" size="small">{{ row.isActive===1?'激活':'历史' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Prompt 内容预览" min-width="300" show-overflow-tooltip>
              <template #default="{ row }">{{ row.templateText ? row.templateText.substring(0, 120) + '...' : '-' }}</template>
            </el-table-column>
            <el-table-column label="描述" width="170" show-overflow-tooltip>
              <template #default="{ row }">{{ row.description || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center" fixed="right">
              <template #default="{ row }">
                <el-button text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button text size="small" :type="row.isActive===1?'warning':'success'" @click="toggleActive(row)">
                  {{ row.isActive===1 ? '停用' : '激活' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑 Prompt 模板" width="900px" top="3vh">
      <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
        <el-descriptions-item label="材料类型">{{ materialLabels[editForm.materialType] || editForm.materialType }}</el-descriptions-item>
        <el-descriptions-item label="评审维度">{{ dimensionLabels[editForm.dimension] || editForm.dimension }}</el-descriptions-item>
        <el-descriptions-item label="当前版本">{{ editForm.version }}</el-descriptions-item>
        <el-descriptions-item label="新版本">{{ nextVersion }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="描述">
          <el-input v-model="editForm.description" placeholder="模板用途说明" />
        </el-form-item>
        <el-form-item label="Prompt内容">
          <el-input v-model="editForm.templateText" type="textarea" :rows="20" placeholder="请输入 Prompt 模板内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存为新版本</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { promptApi } from '@/api/common'

const loading = ref(false)
const saving = ref(false)
const activeTab = ref('TEACHING_PLAN')
const editVisible = ref(false)
const allData = ref<Record<string, any[]>>({})

const materialLabels: Record<string, string> = {
  TEACHING_PLAN: '授课计划',
  LESSON_PLAN: '教案',
  COURSEWARE: '课件',
  EXAM_PLAN: '考核方案'
}

const dimensionLabels: Record<string, string> = {
  completeness: '内容完整性',
  standard_match: '与课程标准匹配度',
  format: '格式规范性',
  innovation: '创新性'
}

function getTabData(mt: string) {
  return allData.value[mt] || []
}

const editForm = reactive({
  id: 0, materialType: '', dimension: '', version: '', templateText: '', description: ''
})

const nextVersion = computed(() => {
  try {
    const parts = editForm.version.replace('v', '').split('.')
    return 'v' + parts[0] + '.' + (parseInt(parts[1] || '0') + 1)
  } catch { return 'v1.0' }
})

async function fetchData() {
  loading.value = true
  try {
    const res: any = await promptApi.list('MATERIAL_EVALUATION')
    if (res.code === 200) {
      allData.value = res.data?.templates || {}
    }
  } catch (e) {
    console.error('Prompt模板加载失败:', e)
  } finally { loading.value = false }
}

function openEdit(row: any) {
  editForm.id = row.id
  editForm.materialType = row.materialType
  editForm.dimension = row.dimension
  editForm.version = row.version
  editForm.templateText = row.templateText || ''
  editForm.description = row.description || ''
  editVisible.value = true
}

async function handleSave() {
  if (!editForm.templateText.trim()) { ElMessage.warning('Prompt内容不能为空'); return }
  saving.value = true
  try {
    await promptApi.update(editForm.id, {
      templateText: editForm.templateText,
      description: editForm.description
    })
    ElMessage.success('Prompt 模板已保存为新版本并激活')
    editVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally { saving.value = false }
}

async function toggleActive(row: any) {
  try {
    await promptApi.toggleActive(row.id)
    ElMessage.success(row.isActive === 1 ? '已停用' : '已激活')
    fetchData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => fetchData())
</script>
