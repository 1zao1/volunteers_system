import request from './request'

export default {
  // 认证
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),

  // 个人资料
  getProfile: () => request.get('/profile'),
  updateProfile: (data) => request.put('/profile', data),

  // 活动（公开，未登录也能看）
  getActivities: (params) => request.get('/activities', { params }),
  getActivity: (id) => request.get(`/activities/${id}`),
  getOverview: () => request.get('/stats/overview'),

  // 报名 / 工时（本人）
  signup: (activityId) => request.post('/registrations', { activityId }),
  cancelSignup: (id) => request.delete(`/registrations/${id}`),
  getMyRegistrations: () => request.get('/registrations/my'),
  getMyHours: () => request.get('/attendance/my'),

  // 私聊（志愿者 ↔ 组织者）
  getContacts: () => request.get('/messages/contacts'),
  getThread: (userId) => request.get(`/messages/with/${userId}`),
  sendMessage: (toUserId, content) => request.post('/messages', { toUserId, content }),
  getUnread: () => request.get('/messages/unread'),
  pinConversation: (type, targetId, pinned) =>
    request.post('/conversations/pin', { type, targetId, pinned }),
  deleteConversation: (type, targetId) =>
    request.post('/conversations/delete', { type, targetId }),

  // 活动群聊（组织者 + 审核通过的志愿者）
  getGroupList: () => request.get('/group-chat/list'),
  getGroupInfo: (activityId) => request.get(`/group-chat/${activityId}/info`),
  getGroupMessages: (activityId) => request.get(`/group-chat/${activityId}/messages`),
  sendGroupMessage: (activityId, content) =>
    request.post(`/group-chat/${activityId}/messages`, { content }),

  // 组织者评价（志愿者 → 组织者）
  getPendingReviews: () => request.get('/reviews/pending'),
  submitReview: (registrationId, rating, comment) =>
    request.post('/reviews', { registrationId, rating, comment }),
  getReceivedReviews: () => request.get('/reviews/received'),

  // 组织者公开主页（游客可查看）
  getOrganizer: (id) => request.get(`/organizers/${id}`),

  // 组织者：看板
  getDashboard: () => request.get('/admin/dashboard'),

  // 组织者：活动
  getAllActivities: () => request.get('/admin/activities'),
  createActivity: (data) => request.post('/admin/activities', data),
  updateActivity: (id, data) => request.put(`/admin/activities/${id}`, data),
  changeActivityStatus: (id, status) => request.patch(`/admin/activities/${id}/status`, { status }),
  deleteActivity: (id) => request.delete(`/admin/activities/${id}`),

  // 组织者：报名审核、工时与信誉分
  getRegistrations: (status) => request.get('/admin/registrations', { params: { status } }),
  auditRegistration: (id, status) => request.post(`/admin/registrations/${id}/audit`, { status }),
  markAbsent: (id) => request.post(`/admin/registrations/${id}/absent`),
  issueHours: (registrationId, hours, creditScore) =>
    request.post('/admin/attendance', { registrationId, hours, creditScore })
}
