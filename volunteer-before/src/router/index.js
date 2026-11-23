import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/', redirect: '/activities' },
  { path: '/login', component: () => import('../views/Login.vue'), meta: { guest: true } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { guest: true } },
  { path: '/activities', component: () => import('../views/ActivityList.vue'), meta: { public: true } },
  { path: '/activity/:id', component: () => import('../views/ActivityDetail.vue'), meta: { public: true } },
  { path: '/organizer/:id', component: () => import('../views/OrganizerProfile.vue'), meta: { public: true } },
  { path: '/my-registrations', component: () => import('../views/MyRegistrations.vue') },
  { path: '/my-hours', component: () => import('../views/MyHours.vue') },
  { path: '/profile', component: () => import('../views/Profile.vue') },
  { path: '/messages', component: () => import('../views/Messages.vue') },
  {
    path: '/dashboard',
    component: () => import('../views/AdminDashboard.vue'),
    meta: { roles: ['organizer'] }
  },
  {
    path: '/admin/activities',
    component: () => import('../views/AdminActivities.vue'),
    meta: { roles: ['organizer'] }
  },
  {
    path: '/admin/audit',
    component: () => import('../views/AdminAudit.vue'),
    meta: { roles: ['organizer'] }
  },
  { path: '/:pathMatch(.*)*', redirect: '/activities' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) return true
  if (!to.meta.guest && !auth.isLogin) return '/login'
  if (to.meta.guest && auth.isLogin) return '/activities'
  if (to.meta.roles && !to.meta.roles.includes(auth.role)) return '/activities'
  return true
})

export default router
