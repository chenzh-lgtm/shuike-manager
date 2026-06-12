import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { storage } from '@/utils/storage'
import type { UserInfo } from '@/types/user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(storage.get('token'))
  const refreshToken = ref<string | null>(storage.get('refreshToken'))
  const userInfo = ref<UserInfo | null>(storage.getObj('userInfo'))

  const isLoggedIn = computed(() => !!token.value)
  const roles = computed(() => userInfo.value?.roles || [])

  function setAuth(t: string, rt: string, user: UserInfo) {
    token.value = t; refreshToken.value = rt; userInfo.value = user
    storage.set('token', t); storage.set('refreshToken', rt); storage.setObj('userInfo', user)
  }

  function logout() {
    token.value = null; refreshToken.value = null; userInfo.value = null
    storage.remove('token'); storage.remove('refreshToken'); storage.remove('userInfo')
  }

  function hasRole(role: string) { return roles.value.includes(role) }

  return { token, refreshToken, userInfo, isLoggedIn, roles, setAuth, logout, hasRole }
})
