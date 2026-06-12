import http from './index'
export const talentPlanApi = {
  list: (params: any) => http.get('/talent-plans', { params }),
  getById: (id: number) => http.get(`/talent-plans/${id}`),
  create: (data: any) => http.post('/talent-plans', data),
  update: (id: number, data: any) => http.put(`/talent-plans/${id}`, data),
  delete: (id: number) => http.delete(`/talent-plans/${id}`),
  updateMapping: (id: number, data: any) => http.put(`/talent-plans/${id}/mapping`, data)
}
