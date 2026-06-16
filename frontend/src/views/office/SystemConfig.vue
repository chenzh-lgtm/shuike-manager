<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:bold;font-size:16px">系统配置</span>
          <el-tag type="info">修改后立即生效，请谨慎操作</el-tag>
        </div>
      </template>

      <el-alert type="warning" :closable="false" show-icon style="margin-bottom:16px">
        <template #default>部分配置（如JWT过期时间）需重启服务后生效。审核流程配置实时生效。</template>
      </el-alert>

      <el-tabs v-model="activeTab" type="border-card">
        <!-- 审核流程 -->
        <el-tab-pane label="审核流程" name="review">
          <el-table :data="filterConfigs('review')" border stripe style="width:100%">
            <el-table-column label="配置项" width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ configLabels[row.configKey] || row.configKey }}</template>
            </el-table-column>
            <el-table-column label="当前值" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.configValue === 'true' ? 'success' : 'danger'" size="small">
                  {{ row.configValue === 'true' ? '已启用' : '已关闭' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="说明" min-width="300" show-overflow-tooltip>
              <template #default="{ row }">{{ row.description }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center" fixed="right">
              <template #default="{ row }">
                <el-button text size="small" type="primary" @click="toggleBool(row)">
                  {{ row.configValue === 'true' ? '关闭' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 安全设置 -->
        <el-tab-pane label="安全设置" name="security">
          <el-table :data="filterConfigs('security')" border stripe style="width:100%">
            <el-table-column label="配置项" width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ configLabels[row.configKey] || row.configKey }}</template>
            </el-table-column>
            <el-table-column label="当前值" width="120" align="center">
              <template #default="{ row }">{{ row.configValue }}</template>
            </el-table-column>
            <el-table-column label="说明" min-width="300" show-overflow-tooltip>
              <template #default="{ row }">{{ row.description }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center" fixed="right">
              <template #default="{ row }">
                <el-button text size="small" type="primary" @click="openEdit(row)">修改</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- AI 设置 -->
        <el-tab-pane label="AI 设置" name="ai">
          <el-table :data="filterConfigs('ai')" border stripe style="width:100%">
            <el-table-column label="配置项" width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ configLabels[row.configKey] || row.configKey }}</template>
            </el-table-column>
            <el-table-column label="当前值" width="120" align="center">
              <template #default="{ row }">{{ row.configValue }}</template>
            </el-table-column>
            <el-table-column label="说明" min-width="300" show-overflow-tooltip>
              <template #default="{ row }">{{ row.description }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center" fixed="right">
              <template #default="{ row }">
                <el-button text size="small" type="primary" @click="openEdit(row)">修改</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 其他 -->
        <el-tab-pane label="其他设置" name="other">
          <el-table :data="filterConfigs('other')" border stripe style="width:100%">
            <el-table-column label="配置项" width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ configLabels[row.configKey] || row.configKey }}</template>
            </el-table-column>
            <el-table-column label="当前值" width="120" align="center">
              <template #default="{ row }">{{ row.configValue }}</template>
            </el-table-column>
            <el-table-column label="说明" min-width="300" show-overflow-tooltip>
              <template #default="{ row }">{{ row.description }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center" fixed="right">
              <template #default="{ row }">
                <el-button text size="small" type="primary" @click="openEdit(row)">修改</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 修改弹窗 -->
    <el-dialog v-model="editVisible" title="修改配置" width="450px">
      <el-descriptions :column="1" border size="small" style="margin-bottom:16px">
        <el-descriptions-item label="配置项">{{ configLabels[editForm.configKey] || editForm.configKey }}</el-descriptions-item>
        <el-descriptions-item label="说明">{{ editForm.description }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="新值">
          <el-input v-model="editForm.configValue" placeholder="请输入新的配置值" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { systemConfigApi } from '@/api/systemConfig'

const loading = ref(false)
const saving = ref(false)
const activeTab = ref('review')
const editVisible = ref(false)
const allConfigs = ref<any[]>([])

/** 配置项-分类映射 */
const categoryMap: Record<string, string> = {
  enable_college_review: 'review',

  enable_captcha: 'security',
  max_login_attempts: 'security',
  lock_duration_minutes: 'security',
  jwt_access_expire_seconds: 'security',
  jwt_refresh_expire_seconds: 'security',

  ai_temperature: 'ai',
  ai_default_model: 'ai',

  max_file_size_mb: 'other',
  max_file_count: 'other',
  notification_poll_seconds: 'other'
}

/** 配置项-中文名映射 */
const configLabels: Record<string, string> = {
  enable_college_review: '启用学院审核',

  enable_captcha: '登录验证码',
  max_login_attempts: '最大登录失败次数',
  lock_duration_minutes: '账户锁定时间(分钟)',
  jwt_access_expire_seconds: 'Access Token过期(秒)',
  jwt_refresh_expire_seconds: 'Refresh Token过期(秒)',

  ai_temperature: 'AI模型温度',
  ai_default_model: '默认AI模型',

  max_file_size_mb: '单文件最大(MB)',
  max_file_count: '单次最大文件数',
  notification_poll_seconds: '通知轮询间隔(秒)'
}

function filterConfigs(cat: string) {
  return allConfigs.value.filter(c => categoryMap[c.configKey] === cat)
}

const editForm = reactive({
  id: 0, configKey: '', configValue: '', description: ''
})

async function fetchData() {
  loading.value = true
  try {
    const res: any = await systemConfigApi.list()
    if (res.code === 200) allConfigs.value = res.data || []
  } finally { loading.value = false }
}

function openEdit(row: any) {
  editForm.id = row.id
  editForm.configKey = row.configKey
  editForm.configValue = row.configValue
  editForm.description = row.description
  editVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    await systemConfigApi.update(editForm.id, editForm.configValue)
    ElMessage.success('配置已更新')
    editVisible.value = false
    fetchData()
  } catch {
    ElMessage.error('保存失败')
  } finally { saving.value = false }
}

/** 布尔类型快速切换 */
async function toggleBool(row: any) {
  const newVal = row.configValue === 'true' ? 'false' : 'true'
  try {
    await systemConfigApi.update(row.id, newVal)
    ElMessage.success(`${configLabels[row.configKey] || row.configKey} 已${newVal === 'true' ? '启用' : '关闭'}`)
    fetchData()
  } catch { ElMessage.error('操作失败') }
}

onMounted(() => fetchData())
</script>
