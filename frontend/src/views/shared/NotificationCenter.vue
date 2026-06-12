<template>
  <div class="noti-page">
    <div class="noti-header">
      <h2 class="page-title">通知中心</h2>
      <div style="display:flex;gap:10px;align-items:center">
        <span v-if="unreadTotal" class="unread-badge">{{ unreadTotal }} 条未读</span>
        <el-button text type="primary" @click="markAllRead" :disabled="!unreadTotal">全部已读</el-button>
      </div>
    </div>

    <!-- 分类筛选 -->
    <div class="filter-bar">
      <span v-for="f in filters" :key="f.key" class="filter-chip" :class="{active:activeFilter===f.key}" @click="activeFilter=f.key">
        {{ f.label }}
        <span v-if="f.count>0" class="chip-count">{{ f.count }}</span>
      </span>
    </div>

    <div v-if="filteredList.length" class="noti-list">
      <div v-for="n in filteredList" :key="n.id" class="noti-card" :class="{unread:n.isRead===0}" @click="handleClick(n)">
        <div class="noti-dot" v-if="n.isRead===0"></div>
        <div class="noti-body">
          <div class="noti-title">{{ n.title }}</div>
          <div class="noti-content">{{ n.content }}</div>
          <div class="noti-time">{{ formatTime(n.createdAt) }}</div>
        </div>
        <div class="noti-arrow">→</div>
      </div>
      <div style="margin-top:16px;display:flex;justify-content:center">
        <el-pagination v-if="total>10" v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" small @current-change="fetchData" />
      </div>
    </div>

    <el-empty v-else description="暂无通知" :image-size="80" style="margin-top:60px" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { notificationApi } from '@/api/common'

const router = useRouter()
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const activeFilter = ref('all')

const filters = computed(() => [
  { key:'all', label:'全部', count:list.value.length },
  { key:'unread', label:'未读', count:list.value.filter(n=>n.isRead===0).length },
  { key:'NEW_MATERIAL', label:'新材料', count:list.value.filter(n=>n.type==='NEW_MATERIAL').length },
  { key:'AI_REVIEW_COMPLETED', label:'AI评审', count:list.value.filter(n=>n.type==='AI_REVIEW_COMPLETED').length },
  { key:'AI_MANUAL_REVIEW', label:'审核结果', count:list.value.filter(n=>n.type==='AI_MANUAL_REVIEW').length },
  { key:'FINAL_REVIEW', label:'待终审', count:list.value.filter(n=>n.type==='FINAL_REVIEW').length }
])

const unreadTotal = computed(() => list.value.filter(n => n.isRead === 0).length)
const filteredList = computed(() => {
  if (activeFilter.value === 'all') return list.value
  if (activeFilter.value === 'unread') return list.value.filter(n => n.isRead === 0)
  return list.value.filter(n => n.type === activeFilter.value)
})

function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff/60000)+'分钟前'
  if (diff < 86400000) return Math.floor(diff/3600000)+'小时前'
  return t.substring(0,16)
}

async function handleClick(n: any) {
  if (n.isRead === 0) {
    try { await notificationApi.markRead(n.id); n.isRead = 1 } catch {}
  }
  if (n.targetUrl) router.push(n.targetUrl)
}

async function markAllRead() {
  try { await notificationApi.markAllRead(); fetchData() } catch {}
}

async function fetchData() {
  try {
    const res: any = await notificationApi.list({ page: page.value, pageSize: 100 })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {}
}

onMounted(() => fetchData())
</script>

<style scoped>
.noti-page{min-height:100%}
.noti-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px}
.page-title{font-family:var(--font-display);font-size:22px;font-weight:700;color:var(--color-primary);letter-spacing:0.04em}
.unread-badge{background:var(--color-danger);color:#fff;padding:3px 12px;border-radius:12px;font-size:12px;font-weight:600}

.filter-bar{display:flex;gap:8px;margin-bottom:20px;flex-wrap:wrap}
.filter-chip{padding:6px 16px;border-radius:20px;font-size:13px;cursor:pointer;border:1px solid var(--color-border);color:var(--color-text-secondary);transition:all 0.15s;background:var(--color-surface)}
.filter-chip:hover{border-color:var(--color-primary);color:var(--color-primary)}
.filter-chip.active{background:var(--color-primary);color:#fff;border-color:var(--color-primary)}
.chip-count{margin-left:4px;opacity:0.7;font-size:11px}

.noti-list{display:flex;flex-direction:column;gap:6px}
.noti-card{display:flex;align-items:center;padding:16px 20px;background:var(--color-surface);border-radius:var(--radius-md);cursor:pointer;transition:all 0.15s;box-shadow:var(--shadow-sm);gap:12px}
.noti-card:hover{box-shadow:var(--shadow-md);transform:translateX(4px)}
.noti-card.unread{background:#f5faf7;border-left:3px solid var(--color-primary)}
.noti-dot{width:8px;height:8px;border-radius:50%;background:var(--color-primary);flex-shrink:0}
.noti-body{flex:1;min-width:0}
.noti-title{font-size:14px;font-weight:600;color:var(--color-text);margin-bottom:4px}
.noti-card.unread .noti-title{color:var(--color-primary)}
.noti-content{font-size:13px;color:var(--color-text-secondary);line-height:1.5;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.noti-time{font-size:11px;color:var(--color-text-muted);margin-top:6px}
.noti-arrow{color:var(--color-text-muted);font-size:14px;flex-shrink:0}
</style>
