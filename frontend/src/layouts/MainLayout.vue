<template>
  <div class="app-shell">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="sidebar-brand" @click="goHome">
        <div class="brand-icon"><img src="/校徽.jpg" alt="校徽" class="logo-img" /></div>
        <div class="brand-text" v-show="!appStore.sidebarCollapsed">
          <span class="brand-title">水课管理系统</span>
          <span class="brand-sub">教学材料智能分析</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span class="nav-label" v-show="!appStore.sidebarCollapsed">{{ item.title }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer" v-show="!appStore.sidebarCollapsed">
        <div class="footer-role">{{ roleLabel }}</div>
        <div class="footer-user">{{ authStore.userInfo?.realName }}</div>
        <div class="footer-college" v-if="authStore.userInfo?.collegeName">{{ authStore.userInfo.collegeName }}</div>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <button class="toggle-btn" @click="appStore.toggleSidebar()" :title="appStore.sidebarCollapsed ? '展开' : '折叠'">
            <span class="toggle-icon">☰</span>
          </button>
          <nav class="breadcrumb">
            <span class="breadcrumb-item">首页</span>
            <span class="breadcrumb-sep">/</span>
            <span class="breadcrumb-item current" v-if="route.meta.title">{{ route.meta.title }}</span>
          </nav>
        </div>

        <div class="topbar-right">
          <div class="notification-btn" @click="$router.push('/shared/notifications')">
            <span class="bell">🔔</span>
            <span class="badge" v-if="unreadCount">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </div>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-chip">
              <span class="user-avatar">{{ (authStore.userInfo?.realName || '用')[0] }}</span>
              <span class="user-name">{{ authStore.userInfo?.realName }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { notificationApi } from '@/api/common'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const unreadCount = ref(0)

const allMenuItems: Record<string, any[]> = {
  TEACHER: [
    { path: '/teacher/dashboard', title: '工作台', icon: '⊡' },
    { path: '/teacher/submit-material', title: '提交教学材料', icon: '↥' },
    { path: '/teacher/review-progress', title: '材料审核进度', icon: '◷' },
    { path: '/teacher/alignment-reports', title: '对齐分析报告', icon: '⊟' }
  ],
  COLLEGE_REVIEWER: [
    { path: '/college/dashboard', title: '审核工作台', icon: '⊡' },
    { path: '/college/material-reviews', title: '材料审核', icon: '✓' }
  ],
  OFFICE: [
    { path: '/office/dashboard', title: '教务处工作台', icon: '⊡' },
    { path: '/office/ai-review-manage', title: '材料终审', icon: '✓' },
    { path: '/office/material-archive', title: '材料归档', icon: '⊞' },
    { path: '/office/users', title: '用户管理', icon: '👤' },
    { path: '/office/colleges', title: '学院管理', icon: '▣' },
    { path: '/office/courses', title: '课程管理', icon: '▤' },
    { path: '/office/semesters', title: '学期管理', icon: '◷' },
    { path: '/office/prompts', title: 'AI Prompt管理', icon: '⚙' }
  ],
  DEAN: [
    { path: '/dean/dashboard', title: '院长工作台', icon: '⊡' },
    { path: '/dean/talent-plans', title: '人培方案管理', icon: '▦' },
    { path: '/dean/course-standards', title: '课程标准管理', icon: '▤' },
    { path: '/dean/alignment-reports', title: '分析产业需求', icon: '⊟' }
  ]
}

const menuItems = computed(() => {
  const role = authStore.roles.find((r: string) => allMenuItems[r])
  return allMenuItems[role] || []
})

const roleLabels: Record<string, string> = {
  TEACHER: '教师', COLLEGE_REVIEWER: '专业主任', OFFICE: '教务处', DEAN: '院长'
}
const roleLabel = computed(() => {
  const role = authStore.roles[0]
  return roleLabels[role] || ''
})

function isActive(path: string) { return route.path.startsWith(path) }
function goHome() {
  const roles = authStore.roles
  if (roles.includes('TEACHER')) router.push('/teacher/dashboard')
  else if (roles.includes('COLLEGE_REVIEWER')) router.push('/college/dashboard')
  else if (roles.includes('OFFICE')) router.push('/office/dashboard')
  else if (roles.includes('DEAN')) router.push('/dean/dashboard')
}
function handleCommand(cmd: string) {
  if (cmd === 'logout') { authStore.logout(); router.push('/login') }
  else if (cmd === 'profile') router.push('/shared/profile')
}

// 通知铃铛：初始加载 + 每30秒自动刷新
let pollInterval: any = null
async function refreshUnread() {
  try { const res: any = await notificationApi.unreadCount(); unreadCount.value = res.data?.count || 0 } catch {}
}
onMounted(() => { refreshUnread(); pollInterval = setInterval(refreshUnread, 30000) })
onUnmounted(() => { if (pollInterval) clearInterval(pollInterval) })
</script>

<style scoped>
.app-shell {
  display: flex;
  height: 100vh;
  background: var(--color-bg);
  overflow: hidden;
}

/* ═══ 侧边栏 ═══ */
.sidebar {
  width: var(--sidebar-width);
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width var(--transition);
  overflow: hidden;
  flex-shrink: 0;
}
.sidebar.collapsed { width: var(--sidebar-collapsed); }

.sidebar-brand {
  padding: 20px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  min-height: 72px;
}
.brand-icon {
  width: 36px; height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.logo-img { width: 100%; height: 100%; object-fit: cover; }
.brand-title { font-size: 16px; font-weight: 700; color: #fff; display: block; letter-spacing: 0.05em; }
.brand-sub { font-size: 11px; color: var(--sidebar-text); display: block; margin-top: 2px; opacity: 0.7; }

/* 导航 */
.sidebar-nav { flex: 1; padding: 12px 8px; overflow-y: auto; }

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 12px;
  margin-bottom: 2px;
  border-radius: var(--radius-md);
  color: var(--sidebar-text);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition);
  cursor: pointer;
  letter-spacing: 0.03em;
}
.nav-item:hover { background: var(--sidebar-hover); color: #fff; }
.nav-item.active { background: var(--sidebar-hover); color: var(--color-accent); font-weight: 600; }
.nav-icon { font-size: 18px; width: 24px; text-align: center; flex-shrink: 0; opacity: 0.85; }
.nav-label { white-space: nowrap; }

/* 底部 */
.sidebar-footer { padding: 16px; border-top: 1px solid rgba(255,255,255,0.06); }
.footer-role { font-size: 11px; color: var(--color-accent); text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 4px; }
.footer-user { font-size: 13px; color: var(--sidebar-text); }
.footer-college { font-size: 11px; color: var(--sidebar-text); opacity: 0.6; margin-top: 2px; }

/* ═══ 主区域 ═══ */
.main-area { flex: 1; display: flex; flex-direction: column; min-width: 0; }

/* 顶栏 */
.topbar {
  height: var(--header-height);
  background: var(--header-bg);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  flex-shrink: 0;
  box-shadow: 0 1px 2px rgba(15,36,32,0.03);
  z-index: 10;
}
.topbar-left, .topbar-right { display: flex; align-items: center; gap: 16px; }

.toggle-btn {
  width: 32px; height: 32px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition);
  color: var(--color-text-secondary);
}
.toggle-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
.toggle-icon { font-size: 14px; line-height: 1; }

.breadcrumb { display: flex; gap: 6px; font-size: 13px; color: var(--color-text-muted); }
.breadcrumb-sep { margin: 0 2px; }
.breadcrumb-item.current { color: var(--color-text); font-weight: 500; }

.notification-btn { position: relative; cursor: pointer; padding: 6px; font-size: 18px; }
.badge {
  position: absolute; top: 0; right: -2px;
  background: var(--color-danger); color: #fff;
  font-size: 10px; min-width: 16px; height: 16px;
  border-radius: 8px; display: flex; align-items: center; justify-content: center;
  padding: 0 4px; font-weight: 600;
}

.user-chip {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; padding: 4px 8px; border-radius: var(--radius-md);
  transition: background var(--transition);
}
.user-chip:hover { background: var(--color-bg); }
.user-avatar {
  width: 28px; height: 28px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 13px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
}
.user-name { font-size: 13px; color: var(--color-text); font-weight: 500; }

/* 内容区 */
.content {
  flex: 1;
  padding: var(--space-lg);
  overflow-y: auto;
  background: var(--color-bg);
}
</style>
