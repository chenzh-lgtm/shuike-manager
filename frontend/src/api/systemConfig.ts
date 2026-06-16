import http from './index'

/** 系统配置 API */
export const systemConfigApi = {
  /** 获取所有配置项 */
  list() {
    return http.get('/system-configs')
  },
  /** 更新单个配置项 */
  update(id: number, configValue: string) {
    return http.put(`/system-configs/${id}`, { configValue })
  }
}
