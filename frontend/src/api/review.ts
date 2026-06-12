import http from './index'
export const reviewApi = {
  collegePending: (params: any) => http.get('/reviews/college/pending', { params }),
  officePending: (params: any) => http.get('/reviews/office/pending', { params }),
  collegeReview: (planId: number, data: any) => http.post(`/reviews/college/${planId}`, data),
  officeReview: (planId: number, data: any) => http.post(`/reviews/office/${planId}`, data),
  collegeBatch: (data: any) => http.post('/reviews/college/batch', data),
  history: (planId: number) => http.get(`/reviews/history/${planId}`)
}
