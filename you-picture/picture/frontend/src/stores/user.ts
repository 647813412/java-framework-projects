import { defineStore } from 'pinia'
import { getUserLoginUsingGet, userLogoutUsingGet } from '@/api/userController'

export const useUserStore = defineStore('user', {
  state: () => ({
    loginUser: null as API.LoginUserVO | null,
    /** 是否已尝试获取过登录状态 */
    userFetched: false,
  }),
  getters: {
    isLoggedIn: (state) => !!state.loginUser,
    isAdmin: (state) => state.loginUser?.userRole === 'admin',
  },
  actions: {
    setLoginUser(user: API.LoginUserVO | null) {
      this.loginUser = user
    },
    async fetchLoginUser() {
      try {
        const res: any = await getUserLoginUsingGet()
        if (res.data) {
          this.loginUser = res.data
        }
      } catch {
        this.loginUser = null
      } finally {
        this.userFetched = true
      }
    },
    async logout() {
      try {
        await userLogoutUsingGet()
      } finally {
        this.loginUser = null
      }
    },
  },
})
