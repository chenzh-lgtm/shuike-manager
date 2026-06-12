export interface UserInfo {
  id: number
  username: string
  realName: string
  collegeId: number | null
  collegeName?: string
  roles: string[]
  avatar: string | null
}

export interface LoginRequest {
  username: string
  password: string
}
