<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:bold;font-size:16px">材料终审（教务处）</span>
          <el-radio-group v-model="statusFilter" size="small" @change="fetchData">
            <el-radio-button value="COLLEGE_APPROVED">待教务处终审</el-radio-button>
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="OFFICE_APPROVED">已通过</el-radio-button>
            <el-radio-button value="AI_REJECTED">已驳回</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>教务处终审说明</template>
        主任已审核通过的材料将流转到此处。请查看AI评分详情后，做出最终审核决定。审核通过后教师端才会显示"已通过"。
      </el-alert>

      <el-table :data="tableData" v-loading="loading" style="width:100%" :default-sort="{prop:'createdAt',order:'descending'}">
        <el-table-column prop="id" label="编号" width="65" />
        <el-table-column label="教师" width="90"><template #default="{ row }">{{ row.teacherName || '-' }}</template></el-table-column>
        <el-table-column label="课程" width="140" show-overflow-tooltip><template #default="{ row }">{{ row.courseName || '-' }}</template></el-table-column>
        <el-table-column label="材料类型" width="100"><template #default="{ row }">{{ typeLabel(row.materialType) }}</template></el-table-column>
        <el-table-column label="AI评分" width="130" align="center">
          <template #default="{ row }">
            <template v-if="row.aiScore != null">
              <span :style="{color:scoreColor(row.aiScore),fontWeight:'bold',fontSize:'16px'}">{{ row.aiScore }}分</span>
              <el-tag :type="row.aiScore>=75?'success':row.aiScore>=60?'warning':'danger'" size="small" style="margin-left:4px">{{ row.aiScore>=90?'优秀':row.aiScore>=75?'良好':row.aiScore>=60?'合格':'待改进' }}</el-tag>
            </template>
            <el-tag v-else type="warning" size="small">评审中</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="110" align="center">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="材料文件" width="160" align="center">
          <template #default="{ row }">
            <el-button text size="small" type="primary" :loading="previewLoading === row.id" @click="openFilePreview(row)">📄 预览</el-button>
            <el-button text size="small" type="success" @click="downloadFileRow(row)">⬇ 下载</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="155" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="showDetail(row)">终审</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:center">
        <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 审核弹窗 -->
    <el-dialog v-model="dialogVisible" title="教务处终审" width="900px" top="3vh">
      <template v-if="currentItem">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header><span style="font-weight:bold">AI 智能评分</span></template>
              <div v-if="currentItem.aiScore != null" style="text-align:center">
                <el-progress type="dashboard" :percentage="currentItem.aiScore||0" :width="140" :color="currentItem.aiScore>=90?'#67C23A':currentItem.aiScore>=75?'#409EFF':currentItem.aiScore>=60?'#E6A23C':'#F56C6C'">
                  <template #default="{p}"><div style="font-size:40px;font-weight:bold">{{p}}</div><div style="font-size:13px;color:#909399">{{scoreGrade(currentItem.aiScore)}}</div></template>
                </el-progress>
                <div v-for="(v,k) in dimScores" :key="k" style="display:flex;align-items:center;margin:6px 0;font-size:12px">
                  <span style="width:80px;text-align:right;margin-right:8px">{{dimLabel(k)}}:</span>
                  <el-progress :percentage="v" :color="v>=75?'#67C23A':v>=60?'#E6A23C':'#F56C6C'" :show-text="false" style="flex:1" />
                  <span style="width:36px;font-weight:bold;margin-left:4px">{{v}}</span>
                </div>
              </div>
            </el-card>
            <el-card v-if="groupedSuggestions.length" style="margin-top:12px" shadow="hover">
              <template #header><span style="font-weight:bold">AI 优化建议（{{ groupedSuggestions.length }} 个维度）</span></template>
              <div v-for="(item,idx) in groupedSuggestions" :key="idx" style="padding:10px;background:#fdf6ec;border-radius:4px;margin-bottom:8px;font-size:13px">
                <span style="color:#E6A23C;font-weight:bold;font-size:14px">【{{ item.dimension }}】</span>
                <ul style="margin:6px 0 6px 18px;color:#303133"><li v-for="(iss,j) in item.issues" :key="j" style="margin-bottom:3px">{{ iss }}</li></ul>
                <div v-if="item.direction" style="color:#909399">→ {{ item.direction }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="教师">{{ currentItem.teacherName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="课程">{{ currentItem.courseName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="材料类型">{{ typeLabel(currentItem.materialType) }}</el-descriptions-item>
              <el-descriptions-item label="提交时间">{{ currentItem.submitTime }}</el-descriptions-item>
              <el-descriptions-item label="AI评分">{{ currentItem.aiScore != null ? currentItem.aiScore+'分' : '评审中' }}</el-descriptions-item>
            </el-descriptions>
            <el-divider />
            <el-form label-width="80px">
              <el-form-item label="审核意见"><el-input v-model="comment" type="textarea" :rows="3" placeholder="请填写审核意见（驳回时必填）" /></el-form-item>
              <el-form-item v-if="action==='REJECT'" label="修改要求"><el-input v-model="revisionRequirements" type="textarea" :rows="2" placeholder="请指出需要修改的具体内容" /></el-form-item>
              <el-form-item>
                <el-radio-group v-model="action" style="margin-bottom:8px"><el-radio-button value="CONFIRM">✅ 确认通过</el-radio-button><el-radio-button value="REJECT">❌ 驳回修改</el-radio-button></el-radio-group>
              </el-form-item>
              <el-form-item>
                <el-button type="success" :loading="saving" @click="handleReview('CONFIRM')">通过（终审）</el-button>
                <el-button type="danger" :loading="saving" @click="handleReview('REJECT')">驳回</el-button>
              </el-form-item>
            </el-form>
          </el-col>
        </el-row>
      </template>
    </el-dialog>

    <!-- 文本预览弹窗 -->
    <el-dialog v-model="txtPreviewVisible" title="文件预览" width="800px" top="3vh">
      <div style="margin-bottom:8px;color:var(--color-text-secondary)">文件：{{ txtPreviewName }}</div>
      <div class="preview-text">{{ txtPreviewContent }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { phaseMaterialApi, fileApi } from '@/api/common'
import { manualReviewApi } from '@/api/manualReview'

const loading=ref(false);const saving=ref(false);const dialogVisible=ref(false)
const previewLoading=ref(0);const txtPreviewVisible=ref(false);const txtPreviewName=ref('');const txtPreviewContent=ref('')
const tableData=ref<any[]>([]);const total=ref(0);const page=ref(1);const statusFilter=ref('COLLEGE_APPROVED')
const currentItem=ref<any>(null);const comment=ref('');const action=ref('CONFIRM');const revisionRequirements=ref('')

const typeLabels:Record<string,string>={TEACHING_PLAN:'授课计划',LESSON_PLAN:'教案',COURSEWARE:'课件',EXAM_PLAN:'考核方案'}
function typeLabel(t:string){return typeLabels[t]||t}
function scoreColor(s:number){if(s>=90)return'#67C23A';if(s>=75)return'#409EFF';if(s>=60)return'#E6A23C';return'#F56C6C'}
function scoreGrade(s:number){if(s>=90)return'优秀';if(s>=75)return'良好';if(s>=60)return'合格';return'待改进'}
function statusType(s:string){const m:any={AI_EVALUATING:'warning',AI_COMPLETED:'',COLLEGE_APPROVED:'warning',OFFICE_APPROVED:'success',AI_REJECTED:'danger'};return m[s]||'info'}
function statusLabel(s:string){const m:any={AI_EVALUATING:'AI评审中',AI_COMPLETED:'待主任审核',COLLEGE_APPROVED:'待教务处终审',OFFICE_APPROVED:'已通过',AI_REJECTED:'已驳回'};return m[s]||s}
function dimLabel(k:string){const m:any={completeness:'内容完整性',standard_match:'课标匹配度',format:'格式规范性',innovation:'创新性'};return m[k]||k}
const dimScores=computed(()=>{try{return JSON.parse(currentItem.value?.dimensionScores||'{}')}catch{return{}}})
const suggestions=computed(()=>{try{return JSON.parse(currentItem.value?.suggestions||'[]')}catch{return[]}})
const groupedSuggestions=computed(()=>{if(!Array.isArray(suggestions.value))return[];const map:Record<string,any>={};for(const s of suggestions.value){const d=s.dimension||'综合';if(!map[d])map[d]={dimension:d,issues:[],direction:''};map[d].issues.push(s.issue||'');if(s.direction&&!map[d].direction)map[d].direction=s.direction}return Object.values(map)})

async function fetchData(){loading.value=true;try{const params:any={page:page.value,pageSize:10};if(statusFilter.value)params.status=statusFilter.value;const r:any=await phaseMaterialApi.reviewerList(params);tableData.value=r.data?.records||[];total.value=r.data?.total||0}catch{}finally{loading.value=false}}
async function openFilePreview(row:any){previewLoading.value=row.id;try{const fres:any=await phaseMaterialApi.getFiles(row.id);const files=fres.data||[];if(!files.length){ElMessage.warning('无附件');return}const f=files[0];const isPdf=f.fileName&&f.fileName.toLowerCase().endsWith('.pdf');if(isPdf){const r:any=await fileApi.preview(f.id);if(r.data?.url)window.open(r.data.url,'_blank')}else{const r:any=await fileApi.preview(f.id);if(r.data?.textContent){txtPreviewName.value=f.fileName;txtPreviewContent.value=r.data.textContent;txtPreviewVisible.value=true}}}catch(e){ElMessage.error('预览失败')}finally{previewLoading.value=0}}
async function downloadFileRow(row:any){try{const fres:any=await phaseMaterialApi.getFiles(row.id);const files=fres.data||[];if(!files.length){ElMessage.warning('无附件');return}const r:any=await fileApi.download(files[0].id);if(r.data?.url)window.open(r.data.url,'_blank')}catch{ElMessage.error('下载失败')}}
function openTextPreview(name:string,text:string){txtPreviewName.value=name;txtPreviewContent.value=text;txtPreviewVisible.value=true}
function showDetail(item:any){currentItem.value=item;action.value='CONFIRM';comment.value='';revisionRequirements.value='';dialogVisible.value=true}
async function handleReview(a:string){if(a==='REJECT'&&!comment.value){ElMessage.warning('驳回时请填写审核意见');return}saving.value=true;try{await manualReviewApi.review(currentItem.value.evaluationId,{action:a,modifiedScore:null,modifyReason:null,reviewComment:comment.value,revisionRequirements:a==='REJECT'?revisionRequirements.value:null,deadline:null,reviewLevel:'OFFICE'});ElMessage.success(a==='CONFIRM'?'终审通过':'已驳回');dialogVisible.value=false;fetchData()}catch(e:any){ElMessage.error('操作失败')}finally{saving.value=false}}
onMounted(()=>fetchData())
</script>

<style scoped>
.preview-text{max-height:500px;overflow-y:auto;padding:16px;background:#f8f8f6;border:1px solid var(--color-border);border-radius:4px;font-size:13px;line-height:1.8;white-space:pre-wrap}
</style>
