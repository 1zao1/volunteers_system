<template>
  <header class="nav" :class="{ 'nav-guest': !auth.isLogin }">
    <div class="container nav-inner">
      <router-link to="/activities" class="logo">
        <span class="logo-mark"><i class="bi bi-stars"></i></span>
        <span class="logo-text">予<span>光</span></span>
      </router-link>

      <nav class="nav-links">
        <router-link
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="nav-link"
          :class="{ active: isActive(m.path) }"
        >
          <i class="bi" :class="m.icon"></i>{{ m.text }}
          <span v-if="m.path === '/messages' && unread" class="nav-badge">{{ unread > 99 ? '99+' : unread }}</span>
        </router-link>
      </nav>

      <div class="nav-right">
        <template v-if="!auth.isLogin">
          <router-link to="/login" class="btn btn-soft btn-sm"><i class="bi bi-person"></i> 我的</router-link>
          <router-link to="/login" class="btn btn-primary btn-sm"><i class="bi bi-box-arrow-in-right"></i> 登录</router-link>
        </template>
        <template v-else>
          <router-link to="/profile" class="user-chip">
            <img v-if="isImgAvatar" :src="auth.avatar" class="avatar avatar-img" alt="头像" />
            <span v-else-if="iconAvatar" class="avatar" :style="{ background: iconAvatar.color }">
              <i class="bi" :class="iconAvatar.icon"></i>
            </span>
            <span v-else class="avatar">{{ auth.initial }}</span>
            <span class="user-name">{{ auth.displayName }}</span>
          </router-link>
          <button class="btn btn-ghost btn-sm" @click="logout">
            <i class="bi bi-box-arrow-right"></i>退出
          </button>
        </template>
      </div>
    </div>
  </header>

  <!-- App 风格底部标签栏（移动端显示） -->
  <nav class="tabbar">
    <router-link
      v-for="t in tabs"
      :key="t.path"
      :to="t.path"
      class="tab-item"
      :class="{ active: isActive(t.path) }"
    >
      <i class="bi" :class="t.icon"></i>
      <span>{{ t.text }}</span>
      <span v-if="t.path === '/messages' && unread" class="tab-badge">{{ unread > 99 ? '99+' : unread }}</span>
    </router-link>
  </nav>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const unread = ref(0)
let unreadTimer = null

async function refreshUnread() {
  if (!auth.isLogin) {
    unread.value = 0
    return
  }
  try {
    unread.value = Number(await api.getUnread()) || 0
  } catch (e) {
    unread.value = 0
  }
}

onMounted(() => {
  refreshUnread()
  unreadTimer = setInterval(refreshUnread, 20000)
})
onUnmounted(() => {
  if (unreadTimer) clearInterval(unreadTimer)
})
watch(() => auth.token, refreshUnread)
watch(() => route.path, (p) => {
  if (p === '/messages') unread.value = 0
})

const all = [
  { path: '/activities', text: '活动广场', icon: 'bi-compass' },
  { path: '/my-registrations', text: '我的报名', icon: 'bi-journal-check', roles: ['volunteer'] },
  { path: '/my-hours', text: '我的工时', icon: 'bi-clock-history', roles: ['volunteer'] },
  { path: '/dashboard', text: '工作台', icon: 'bi-speedometer2', roles: ['organizer'] },
  { path: '/admin/activities', text: '活动管理', icon: 'bi-calendar2-week', roles: ['organizer'] },
  { path: '/admin/audit', text: '报名审核', icon: 'bi-clipboard-check', roles: ['organizer'] },
  { path: '/messages', text: '消息', icon: 'bi-chat-dots' },
  { path: '/profile', text: '我的', icon: 'bi-person-badge' }
]

const menus = computed(() => {
  if (!auth.isLogin) return all.filter((m) => m.path === '/activities')
  return all.filter((m) => !m.roles || m.roles.includes(auth.role))
})

const tabs = computed(() => {
  if (!auth.isLogin) {
    return [
      { path: '/activities', text: '活动', icon: 'bi-compass' },
      { path: '/login', text: '我的', icon: 'bi-person' }
    ]
  }
  if (auth.role === 'organizer') {
    return [
      { path: '/activities', text: '活动', icon: 'bi-compass' },
      { path: '/dashboard', text: '工作台', icon: 'bi-speedometer2' },
      { path: '/admin/audit', text: '审核', icon: 'bi-clipboard-check' },
      { path: '/messages', text: '消息', icon: 'bi-chat-dots' },
      { path: '/profile', text: '我的', icon: 'bi-person' }
    ]
  }
  return [
    { path: '/activities', text: '活动', icon: 'bi-compass' },
    { path: '/my-registrations', text: '报名', icon: 'bi-journal-check' },
    { path: '/my-hours', text: '工时', icon: 'bi-clock-history' },
    { path: '/messages', text: '消息', icon: 'bi-chat-dots' },
    { path: '/profile', text: '我的', icon: 'bi-person' }
  ]
})

const isImgAvatar = computed(() => (auth.avatar || '').startsWith('data:image'))

const iconAvatar = computed(() => {
  const v = auth.avatar || ''
  if (!v.startsWith('avatar:')) return null
  const [icon, color] = v.slice(7).split('|')
  return { icon, color }
})

function isActive(path) {
  return route.path === path || (path === '/activities' && route.path.startsWith('/activity/'))
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
