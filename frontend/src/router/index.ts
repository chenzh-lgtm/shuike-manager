import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginPage.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/teacher',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true, roles: ['TEACHER'] },
      redirect: '/teacher/dashboard',
      children: [
        { path: 'dashboard', name: 'TeacherDashboard', component: () => import('@/views/teacher/Dashboard.vue'), meta: { title: '工作台' } },
        { path: 'submit-material', name: 'SubmitMaterial', component: () => import('@/views/teacher/SubmitMaterial.vue'), meta: { title: '提交教学材料' } },
        { path: 'review-progress', name: 'ReviewProgress', component: () => import('@/views/teacher/ReviewProgress.vue'), meta: { title: '材料审核进度' } },
        { path: 'alignment-reports', name: 'TeacherAlignment', component: () => import('@/views/teacher/AlignmentReport.vue'), meta: { title: '对齐分析报告' } }
      ]
    },
    {
      path: '/college',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true, roles: ['COLLEGE_REVIEWER'] },
      redirect: '/college/dashboard',
      children: [
        { path: 'dashboard', name: 'CollegeDashboard', component: () => import('@/views/college/Dashboard.vue'), meta: { title: '审核工作台' } },
        { path: 'material-reviews', name: 'MaterialReviewList', component: () => import('@/views/college/MaterialReviewList.vue'), meta: { title: '材料审核' } }
      ]
    },
    {
      path: '/office',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true, roles: ['OFFICE'] },
      redirect: '/office/dashboard',
      children: [
        { path: 'dashboard', name: 'OfficeDashboard', component: () => import('@/views/office/Dashboard.vue'), meta: { title: '教务处工作台' } },
        { path: 'ai-review-manage', name: 'AiReviewManage', component: () => import('@/views/office/AiReviewManage.vue'), meta: { title: '材料终审' } },
        { path: 'material-archive', name: 'MaterialArchive', component: () => import('@/views/office/MaterialArchive.vue'), meta: { title: '材料归档' } },
        { path: 'ai-review-manage/:id', name: 'OfficeAiReviewDetail', component: () => import('@/views/office/AiReviewDetail.vue'), meta: { title: 'AI 复核详情' } },
        { path: 'users', name: 'UserManage', component: () => import('@/views/office/UserManage.vue'), meta: { title: '用户管理' } },
        { path: 'colleges', name: 'CollegeManage', component: () => import('@/views/office/CollegeManage.vue'), meta: { title: '学院管理' } },
        { path: 'courses', name: 'CourseManage', component: () => import('@/views/office/CourseManage.vue'), meta: { title: '课程管理' } },
        { path: 'semesters', name: 'SemesterManage', component: () => import('@/views/office/SemesterManage.vue'), meta: { title: '学期管理' } },
        { path: 'prompts', name: 'PromptManage', component: () => import('@/views/office/PromptManage.vue'), meta: { title: 'AI Prompt管理' } },
        { path: 'operation-logs', name: 'OperationLogs', component: () => import('@/views/office/OperationLogs.vue'), meta: { title: '操作日志' } },
        { path: 'system-config', name: 'SystemConfig', component: () => import('@/views/office/SystemConfig.vue'), meta: { title: '系统配置' } }
      ]
    },
    {
      path: '/dean',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true, roles: ['DEAN'] },
      redirect: '/dean/dashboard',
      children: [
        { path: 'dashboard', name: 'DeanDashboard', component: () => import('@/views/dean/Dashboard.vue'), meta: { title: '院长工作台' } },
        { path: 'talent-plans', name: 'TalentPlanList', component: () => import('@/views/dean/TalentPlanList.vue'), meta: { title: '人培方案管理' } },
        { path: 'talent-plans/create', name: 'TalentPlanCreate', component: () => import('@/views/dean/TalentPlanForm.vue'), meta: { title: '新增人培方案' } },
        { path: 'talent-plans/:id/edit', name: 'TalentPlanEdit', component: () => import('@/views/dean/TalentPlanForm.vue'), meta: { title: '编辑人培方案' } },
        { path: 'course-standards', name: 'CourseStandardList', component: () => import('@/views/dean/CourseStandardList.vue'), meta: { title: '课程标准管理' } },
        { path: 'course-standards/create', name: 'CourseStandardCreate', component: () => import('@/views/dean/CourseStandardForm.vue'), meta: { title: '新增课程标准' } },
        { path: 'course-standards/:id/edit', name: 'CourseStandardEdit', component: () => import('@/views/dean/CourseStandardForm.vue'), meta: { title: '编辑课程标准' } },
        { path: 'alignment-reports', name: 'DeanAlignment', component: () => import('@/views/dean/AlignmentReport.vue'), meta: { title: '分析产业需求' } }
      ]
    },
    {
      path: '/shared',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: 'notifications', name: 'NotificationCenter', component: () => import('@/views/shared/NotificationCenter.vue'), meta: { title: '通知中心' } },
        { path: 'profile', name: 'Profile', component: () => import('@/views/shared/ProfilePage.vue'), meta: { title: '个人中心' } }
      ]
    },
    { path: '/', redirect: '/login' },
    { path: '/:pathMatch(.*)*', redirect: '/login' }
  ]
})

function getRoleDashboard(roles: string[]): string {
  if (roles.includes('TEACHER')) return '/teacher/dashboard'
  if (roles.includes('COLLEGE_REVIEWER')) return '/college/dashboard'
  if (roles.includes('OFFICE')) return '/office/dashboard'
  if (roles.includes('DEAN')) return '/dean/dashboard'
  return '/login'
}

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  if (!to.meta.requiresAuth) {
    if (to.name === 'Login' && authStore.isLoggedIn) {
      return next(getRoleDashboard(authStore.roles))
    }
    return next()
  }
  if (!authStore.isLoggedIn) return next({ name: 'Login', query: { redirect: to.fullPath } })
  const allowedRoles = to.meta.roles as string[] | undefined
  if (allowedRoles && allowedRoles.length > 0) {
    if (!authStore.roles.some(r => allowedRoles.includes(r))) {
      return next(getRoleDashboard(authStore.roles))
    }
  }
  next()
})

export default router
