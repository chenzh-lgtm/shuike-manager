import http from './index'
export const authApi = {
  login: (data: { username: string; password: string }) => http.post('/auth/login', data),
  refresh: (refreshToken: string) => http.post('/auth/refresh', { refreshToken }),
  changePassword: (data: { oldPassword: string; newPassword: string }) => http.put('/auth/password', data)
}
