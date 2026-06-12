import http from './index'
export const manualReviewApi = {
  pending: (params: any) => http.get('/manual-reviews/pending', { params }),
  history: (params: any) => http.get('/manual-reviews/history', { params }),
  byEvaluation: (evaluationId: number) => http.get(`/manual-reviews/evaluation/${evaluationId}`),
  review: (evaluationId: number, data: any) => http.post(`/manual-reviews/${evaluationId}`, data)
}
