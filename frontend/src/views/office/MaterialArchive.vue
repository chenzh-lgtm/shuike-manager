<template>
  <div>
    <el-card>
      <template #header><span style="font-weight:bold;font-size:16px">材料归档</span></template>
      <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center">
        <el-select v-model="filterType" placeholder="材料类型" clearable style="width:140px" @change="fetchData">
          <el-option label="授课计划" value="TEACHING_PLAN"/><el-option label="教案" value="LESSON_PLAN"/>
          <el-option label="课件" value="COURSEWARE"/><el-option label="考核方案" value="EXAM_PLAN"/>
        </el-select>
        <el-radio-group v-model="archiveMode" size="small" @change="fetchData">
          <el-radio-button value="approved">已通过</el-radio-button>
          <el-radio-button value="">全部</el-radio-button>
        </el-radio-group>
        <el-input v-model="keyword" placeholder="搜索教师/课程/描述" clearable style="width:200px" @change="fetchData"/>
        <el-button type="primary" :disabled="!selectedIds.length" @click="batchDownload">📥 批量下载({{selectedIds.length}})</el-button>
        <el-button type="success" @click="exportExcel">📊 导出Excel</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" @selection-change="onSelect" style="width:100%">
        <el-table-column type="selection" width="45"/>
        <el-table-column prop="id" label="编号" width="65"/>
        <el-table-column label="教师" width="90"><template #default="{row}">{{ row.teacherName }}</template></el-table-column>
        <el-table-column label="课程" width="130" show-overflow-tooltip><template #default="{row}">{{ row.courseName }}</template></el-table-column>
        <el-table-column label="材料类型" width="100"><template #default="{row}">{{ typeLabel(row.materialType) }}</template></el-table-column>
        <el-table-column prop="description" label="描述" min-width="120" show-overflow-tooltip/>
        <el-table-column label="文件" width="80" align="center"><template #default="{row}">{{ row.fileCount }}个</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{row}">
            <el-button text size="small" type="primary" @click="previewFiles(row)">📄 预览</el-button>
            <el-button text size="small" type="success" @click="downloadAll(row)">⬇ 下载</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="page" :total="total" layout="total,prev,pager,next" @change="fetchData"/>
    </el-card>

    <!-- 文件预览弹窗 -->
    <el-dialog v-model="previewVisible" title="文件预览" width="850px" top="3vh" destroy-on-close>
      <div v-if="previewFilesList.length">
        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane v-for="(f,i) in previewFilesList" :key="i" :label="f.fileName" :name="String(i)">
            <div v-if="isPdf(f)" style="text-align:center;padding:20px">
              <div style="font-size:48px;margin-bottom:12px">📑</div>
              <div>PDF文件 — 点击下方按钮在新窗口查看</div>
              <el-button type="primary" style="margin-top:12px" @click="openFile(f)">在新窗口查看PDF</el-button>
            </div>
            <div v-else-if="previewTexts[i]" class="preview-text">{{ previewTexts[i] }}</div>
            <div v-else style="text-align:center;padding:40px">⏳ 加载中...</div>
            <el-button size="small" style="margin-top:8px" @click="openFile(f)">下载原文件</el-button>
          </el-tab-pane>
        </el-tabs>
      </div>
      <el-empty v-else description="暂无附件"/>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { phaseMaterialApi, fileApi } from '@/api/common'
import http from '@/api/index'

const loading=ref(false);const tableData=ref<any[]>([]);const total=ref(0);const page=ref(1)
const filterType=ref('');const keyword=ref('');const archiveMode=ref('approved')
const selectedIds=ref<number[]>([])
const previewVisible=ref(false);const previewFilesList=ref<any[]>([]);const previewTexts=ref<string[]>([]);const activeTab=ref('0')

const typeLabels:Record<string,string>={TEACHING_PLAN:'授课计划',LESSON_PLAN:'教案',COURSEWARE:'课件',EXAM_PLAN:'考核方案'}
function typeLabel(t:string){return typeLabels[t]||t}
function statusType(s:string){const m:any={AI_EVALUATING:'warning',AI_COMPLETED:'',COLLEGE_APPROVED:'warning',OFFICE_APPROVED:'success',AI_REJECTED:'danger'};return m[s]||'info'}
function statusLabel(s:string){const m:any={AI_EVALUATING:'AI评审中',AI_COMPLETED:'待主任审核',COLLEGE_APPROVED:'待教务处审核',OFFICE_APPROVED:'已通过',AI_REJECTED:'已驳回'};return m[s]||s}
function isPdf(f:any){return f.fileName&&f.fileName.toLowerCase().endsWith('.pdf')}
function onSelect(rows:any[]){selectedIds.value=rows.map((r:any)=>r.id)}

async function fetchData(){
  loading.value=true
  try{
    const params:any={page:page.value,pageSize:10,status:archiveMode.value}
    if(filterType.value)params.materialType=filterType.value
    if(keyword.value.trim())params.keyword=keyword.value.trim()
    const r:any=await http.get('/phase-materials/archive',{params})
    tableData.value=r.data?.records||[];total.value=r.data?.total||0
  }catch{}finally{loading.value=false}
}

async function previewFiles(row:any){
  previewFilesList.value=row.files||[]
  previewTexts.value=Array(previewFilesList.value.length).fill('')
  previewVisible.value=true;activeTab.value='0'
  for(let i=0;i<previewFilesList.value.length;i++){
    const f=previewFilesList.value[i]
    if(isPdf(f)){previewTexts.value[i]='【PDF文件】';continue}
    try{const r:any=await fileApi.preview(f.id);previewTexts.value[i]=r.data?.textContent||'[无法预览]'}catch{previewTexts.value[i]='[加载失败]'}
  }
}

function openFile(f:any){fileApi.download(f.id).then((r:any)=>{if(r.data?.url)window.open(r.data.url,'_blank')}).catch(()=>ElMessage.error('打开失败'))}

async function downloadAll(row:any){for(const f of(row.files||[])){try{const r:any=await fileApi.download(f.id);if(r.data?.url)window.open(r.data.url,'_blank')}catch(e){}}}

async function batchDownload(){for(const id of selectedIds.value){const row=tableData.value.find((r:any)=>r.id===id);if(row)await downloadAll(row)}}

async function exportExcel(){
  try {
    const params: any = {}; if (filterType.value) params.materialType = filterType.value
    const res = await http.get('/phase-materials/export/approved', { params, responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([res as any], { type: 'text/csv;charset=UTF-8' }))
    const a = document.createElement('a'); a.href = url; a.download = '已通过材料名单.csv'; a.click()
    URL.revokeObjectURL(url); ElMessage.success('导出成功')
  } catch { ElMessage.error('导出失败') }
}

onMounted(()=>fetchData())
</script>

<style scoped>
.preview-text{max-height:500px;overflow-y:auto;padding:16px;background:#f8f8f6;border:1px solid var(--color-border);border-radius:4px;font-size:13px;line-height:1.8;white-space:pre-wrap}
</style>
