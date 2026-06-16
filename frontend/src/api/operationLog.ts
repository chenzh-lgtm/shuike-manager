import http from './index'

/** 操作日志 API */
export const operationLogApi = {
  /**
   * 分页查询操作日志
   * @param params 查询参数
   */
  list(params: {
    page?: number
    pageSize?: number
    module?: string
    action?: string
    keyword?: string
    startTime?: string
    endTime?: string
  }) {
    return http.get('/operation-logs', { params })
  }
}
