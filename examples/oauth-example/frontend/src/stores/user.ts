import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCurrentUser, logout as apiLogout } from '@/api/oauth'
import type { CurrentUser } from '@/api/oauth'

export const useUserStore = defineStore('user', () => {
  // 状态
  const user = ref<CurrentUser | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 计算属性
  const isLoggedIn = computed(() => !!user.value)
  const token = computed(() => localStorage.getItem('access_token'))

  // 方法
  async function fetchUser() {
    if (!localStorage.getItem('access_token')) {
      return
    }

    loading.value = true
    error.value = null

    try {
      user.value = await getCurrentUser()
    } catch (e: any) {
      error.value = e.message || '获取用户信息失败'
      user.value = null
    } finally {
      loading.value = false
    }
  }

  function setToken(accessToken: string) {
    localStorage.setItem('access_token', accessToken)
  }

  async function logout() {
    try {
      await apiLogout()
    } catch (e) {
      // 忽略错误
    } finally {
      user.value = null
      localStorage.removeItem('access_token')
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    // 状态
    user,
    loading,
    error,
    // 计算属性
    isLoggedIn,
    token,
    // 方法
    fetchUser,
    setToken,
    logout,
    clearError
  }
})


