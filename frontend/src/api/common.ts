import http from './index'
export const collegeApi = {
  list: () => http.get('/colleges'),
  create: (data: any) => http.post('/colleges', data),
  update: (id: number, data: any) => http.put(`/colleges/${id}`, data),
  delete: (id: number) => http.delete(`/colleges/${id}`)
}
export const courseApi = {
  list: (params: any) => http.get('/courses', { params }),
  create: (data: any) => http.post('/courses', data),
  update: (id: number, data: any) => http.put(`/courses/${id}`, data),
  delete: (id: number) => http.delete(`/courses/${id}`)
}
export const semesterApi = {
  list: () => http.get('/semesters'),
  create: (data: any) => http.post('/semesters', data),
  update: (id: number, data: any) => http.put(`/semesters/${id}`, data),
  delete: (id: number) => http.delete(`/semesters/${id}`),
  activate: (id: number) => http.put(`/semesters/${id}/activate`)
}
export const userApi = {
  list: (params: any) => http.get('/users', { params }),
  create: (data: any) => http.post('/users', data),
  update: (id: number, data: any) => http.put(`/users/${id}`, data),
  updateStatus: (id: number, status: number) => http.put(`/users/${id}/status`, { status }),
  delete: (id: number) => http.delete(`/users/${id}`)
}
export const notificationApi = {
  list: (params: any) => http.get('/notifications', { params }),
  unreadCount: () => http.get('/notifications/unread-count'),
  markRead: (id: number) => http.put(`/notifications/${id}/read`),
  markAllRead: () => http.put('/notifications/read-all')
}
export const dashboardApi = {
  teacher: () => http.get('/dashboard/teacher'),
  college: () => http.get('/dashboard/college'),
  office: () => http.get('/dashboard/office'),
  dean: () => http.get('/dashboard/dean')
}
export const fileApi = {
  upload: (file: File, type: string = 'PLAN', materialType: string = '') => {
    const form = new FormData()
    form.append('file', file)
    form.append('type', type)
    if (materialType) form.append('materialType', materialType)
    return http.post('/files/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  uploadParse: (file: File, type: string = 'TALENT_PLAN') => {
    const form = new FormData()
    form.append('file', file)
    form.append('type', type)
    return http.post('/files/upload-parse', form, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  download: (fileId: number) => http.get(`/files/${fileId}/download`, { params: { mode: 'download' } }),
  preview: (fileId: number) => http.get(`/files/${fileId}/download`, { params: { mode: 'preview' } })
}
export const phaseMaterialApi = {
  list: (params: any) => http.get('/phase-materials', { params }),
  reviewerList: (params: any) => http.get('/phase-materials/reviewer', { params }),
  reviewerFilters: () => http.get('/phase-materials/reviewer/filters'),
  getById: (id: number) => http.get(`/phase-materials/${id}`),
  create: (data: any) => http.post('/phase-materials', data),
  delete: (id: number) => http.delete(`/phase-materials/${id}`),
  getFiles: (id: number) => http.get(`/phase-materials/${id}/files`),
  addFiles: (id: number, fileIds: number[]) => http.post(`/phase-materials/${id}/files`, { fileIds }),
  resubmit: (id: number) => http.post(`/phase-materials/${id}/resubmit`)
}
export const courseStandardApi = {
  list: (params: any) => http.get('/course-standards', { params }),
  getById: (id: number) => http.get(`/course-standards/${id}`),
  create: (data: any) => http.post('/course-standards', data),
  update: (id: number, data: any) => http.put(`/course-standards/${id}`, data),
  delete: (id: number) => http.delete(`/course-standards/${id}`)
}
export const alignmentApi = {
  analyze: (tcpId: number) => http.post('/alignment/analyze', { tcpId }),
  reports: (params: any) => http.get('/alignment/reports', { params }),
  reportDetail: (id: number) => http.get(`/alignment/reports/${id}`),
  delete: (id: number) => http.delete(`/alignment/reports/${id}`)
}
export const promptApi = {
  list: (scene: string) => http.get('/prompt-templates', { params: { scene } }),
  getById: (id: number) => http.get(`/prompt-templates/${id}`),
  update: (id: number, data: any) => http.put(`/prompt-templates/${id}`, data),
  toggleActive: (id: number) => http.put(`/prompt-templates/${id}/toggle-active`)
}
