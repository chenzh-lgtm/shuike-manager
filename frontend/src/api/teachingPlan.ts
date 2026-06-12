import http from './index'
export const teachingPlanApi = {
  list: (params: any) => http.get('/teaching-plans', { params }),
  getById: (id: number) => http.get(`/teaching-plans/${id}`),
  create: (data: any) => http.post('/teaching-plans', data),
  update: (id: number, data: any) => http.put(`/teaching-plans/${id}`, data),
  delete: (id: number) => http.delete(`/teaching-plans/${id}`),
  submit: (id: number) => http.post(`/teaching-plans/${id}/submit`),
  withdraw: (id: number) => http.post(`/teaching-plans/${id}/withdraw`),
  getFiles: (id: number) => http.get(`/teaching-plans/${id}/files`),
  addFiles: (id: number, fileIds: number[]) => http.post(`/teaching-plans/${id}/files`, { fileIds })
}
