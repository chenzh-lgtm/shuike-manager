import http from './index'
export const aiApi = {
  submit: (materialId: number) => http.post('/ai-evaluations', { materialId }),
  getById: (id: number) => http.get(`/ai-evaluations/${id}`),
  myList: (params: any) => http.get('/ai-evaluations/my', { params }),
  allList: (params: any) => http.get('/ai-evaluations/all', { params })
}
