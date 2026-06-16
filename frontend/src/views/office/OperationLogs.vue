<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:bold;font-size:16px">操作日志</span>
          <span style="font-size:12px;color:var(--color-text-muted)">记录所有关键操作，保留3年</span>
        </div>
      </template>

      <!-- 筛选栏 -->
      <el-form :inline="true">
        <el-form-item label="操作模块">
          <el-select v-model="query.module" clearable @change="fetchData" style="width:140px">
            <el-option label="全部" value="" />
            <el-option v-for="m in moduleOptions" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="query.action" clearable @change="fetchData" style="width:120px">
            <el-option label="全部" value="" />
            <el-option v-for="a in actionOptions" :key="a" :label="a" :value="a" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:340px"
            @change="onDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="搜索用户名/操作详情" clearable @clear="fetchData" @keyup.enter="fetchData" style="width:200px">
            <template #prefix><span>🔍</span></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志表格 -->
      <el-table :data="tableData" v-loading="loading" style="width:100%" stripe size="small" row-key="id">
        <el-table-column label="时间" width="165">
          <template #default="{ row }">
            <span style="font-size:12px;color:var(--color-text-secondary)">{{ (row.createdAt || '-').replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作描述" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-weight:500">{{ row.detail }}</span>
          </template>
        </el-table-column>
        <el-table-column label="模块" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.module || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.costMs != null" :style="{ color: row.costMs > 1000 ? 'var(--color-danger)' : '' }">{{ row.costMs }}ms</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP" width="135" />
      </el-table>

      <!-- 分页 -->
      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 无详情弹窗，detail已作为主列展示 -->
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { operationLogApi } from '@/api/operationLog'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const dateRange = ref<any[] | null>(null)

const query = reactive({
  page: 1,
  pageSize: 10,
  module: '',
  action: '',
  keyword: '',
  startTime: '',
  endTime: ''
})

// 模块选项（与应用中 annotation 保持一致）
const moduleOptions = ['认证', '材料管理', '审核管理', '用户管理', '对齐分析', 'Prompt管理']

// 操作类型选项
const actionOptions = ['LOGIN', 'CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT', 'IMPORT', 'ANALYZE', 'REVIEW']

/** 时间范围变更 */
function onDateChange(val: any) {
  if (val && val.length === 2) {
    query.startTime = val[0]
    query.endTime = val[1]
  } else {
    query.startTime = ''
    query.endTime = ''
  }
  fetchData()
}

/** 查询数据 */
async function fetchData() {
  loading.value = true
  try {
    const res: any = await operationLogApi.list({
      page: query.page,
      pageSize: query.pageSize,
      module: query.module || undefined,
      action: query.action || undefined,
      keyword: query.keyword || undefined,
      startTime: query.startTime || undefined,
      endTime: query.endTime || undefined
    })
    if (res.code === 200) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
pre {
  margin: 0;
  font-family: 'SFMono-Regular', Consolas, monospace;
  line-height: 1.6;
}
</style>
