import { defineStore } from 'pinia'

const ROLE_MAP = {
  volunteer: { name: '志愿者', icon: 'bi-person-heart' },
  organizer: { name: '组织者', icon: 'bi-megaphone' }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    isLogin: (s) => !!s.token,
    role: (s) => s.user?.role || '',
    roleName: (s) => ROLE_MAP[s.user?.role]?.name || '',
    roleIcon: (s) => ROLE_MAP[s.user?.role]?.icon || 'bi-person',
    isOrganizer: (s) => s.user?.role === 'organizer',
    displayName: (s) => s.user?.nickname || s.user?.name || '—',
    creditScore: (s) => (s.user?.creditScore ?? 100),
    initial: (s) => (s.user?.nickname || s.user?.name || '?').slice(0, 1).toUpperCase(),
    avatar: (s) => s.user?.avatar || '',
    signature: (s) => s.user?.signature || ''
  },
  actions: {
    setSession(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },
    patchUser(patch) {
      this.user = { ...(this.user || {}), ...patch }
      localStorage.setItem('user', JSON.stringify(this.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
