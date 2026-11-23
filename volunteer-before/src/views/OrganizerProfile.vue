<template>
  <router-link to="/activities" class="back-link">
    <i class="bi bi-arrow-left"></i> 返回活动广场
  </router-link>

  <template v-if="o">
    <section class="org-hero">
      <div class="org-avatar" :style="avatarStyle">
        <span v-if="!isImg">{{ initial }}</span>
      </div>
      <div class="org-main">
        <div class="org-name">
          {{ o.nickname }}
          <span class="role-tag organizer">活动组织者</span>
        </div>
        <div class="org-sig">{{ o.signature || '这位组织者还没有填写简介' }}</div>
        <div class="org-meta">
          <span><i class="bi bi-award"></i> 信誉分 <b>{{ o.creditScore ?? 100 }}</b></span>
          <span v-if="o.reviewCount"><i class="bi bi-star-fill"></i> {{ o.avgRating }} 分 · {{ o.reviewCount }} 条评价</span>
          <span v-else><i class="bi bi-star"></i> 暂无评价</span>
          <span v-if="o.joinedAt"><i class="bi bi-calendar3"></i> {{ String(o.joinedAt).slice(0, 10) }} 加入</span>
        </div>
      </div>
      <button
        v-if="auth.isLogin && auth.user?.id !== o.id"
        class="btn btn-soft btn-sm"
        @click="contact"
      >
        <i class="bi bi-chat-dots"></i> 私信 TA
      </button>
    </section>

    <div class="stats-grid" style="grid-template-columns: repeat(4, 1fr); margin-bottom: 20px">
      <div class="stat">
        <div class="icon green"><i class="bi bi-calendar-check"></i></div>
        <div><div class="num">{{ o.activityCount }}</div><div class="label">发布活动</div></div>
      </div>
      <div class="stat">
        <div class="icon sky"><i class="bi bi-people"></i></div>
        <div><div class="num">{{ o.volunteerCount }}</div><div class="label">服务过的志愿者</div></div>
      </div>
      <div class="stat">
        <div class="icon amber"><i class="bi bi-clock-history"></i></div>
        <div><div class="num">{{ o.totalHours }}</div><div class="label">累计认定工时</div></div>
      </div>
      <div class="stat">
        <div class="icon rose"><i class="bi bi-star-half"></i></div>
        <div><div class="num">{{ o.badCount }}</div><div class="label">收到的差评</div></div>
      </div>
    </div>

    <div class="org-grid">
      <div class="panel">
        <div class="panel-head">
          <h3>志愿者评价</h3>
          <span class="sub">活动结束后由参加过的志愿者打分</span>
        </div>
        <div v-if="reviews.length" class="review-list">
          <div v-for="(r, i) in reviews.slice(0, 8)" :key="i" class="review-item">
            <div class="review-head">
              <span class="avatar">{{ (r.volunteerName || '?').slice(0, 1).toUpperCase() }}</span>
              <div class="review-who">
                <div class="strong">{{ r.volunteerName }}</div>
                <div class="muted">{{ r.activityTitle }}</div>
              </div>
              <div class="stars">
                <i v-for="n in 5" :key="n" class="bi" :class="n <= (r.rating || 0) ? 'bi-star-fill on' : 'bi-star'"></i>
              </div>
            </div>
            <div v-if="r.comment" class="review-text">{{ r.comment }}</div>
            <div class="review-time">{{ r.time }}</div>
          </div>
        </div>
        <div v-else class="org-empty">
          <i class="bi bi-chat-square-heart"></i>
          <div>还没有志愿者评价</div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <h3>正在招募</h3>
          <span class="sub">{{ o.upcomingCount }} 场</span>
        </div>
        <div v-if="o.activities?.length" class="org-acts">
          <router-link
            v-for="a in o.activities"
            :key="a.id"
            :to="`/activity/${a.id}`"
            class="org-act"
          >
            <span class="thumb" :style="{ backgroundImage: `url(${coverUrl(a.cover, a.category)})` }"></span>
            <span class="org-act-main">
              <span class="strong">{{ a.title }}</span>
              <span class="muted">{{ fmtTime(a.startTime) }} · {{ a.location }}</span>
            </span>
            <span class="muted nowrap">{{ a.signedCount }}/{{ a.maxCount }} 人</span>
          </router-link>
        </div>
        <div v-else class="org-empty">
          <i class="bi bi-calendar-x"></i>
          <div>暂时没有正在招募的活动</div>
        </div>
      </div>
    </div>
  </template>

  <div v-else class="panel" style="padding: 60px; text-align: center">
    <i class="bi bi-hourglass" style="font-size: 30px; color: var(--muted)"></i>
    <p class="muted">加载中…</p>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { fmtTime } from '../utils/category'
import { coverUrl } from '../utils/covers'
import { toast } from '../utils/toast'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const o = ref(null)

const reviews = computed(() => o.value?.reviews || [])

const isImg = computed(() => {
  const a = o.value?.avatar
  return a && (String(a).startsWith('data:') || String(a).startsWith('http'))
})
const avatarStyle = computed(() =>
  isImg.value ? { backgroundImage: `url(${o.value.avatar})` } : {}
)
const initial = computed(() => (o.value?.nickname || '?').slice(0, 1).toUpperCase())

function contact() {
  if (!auth.isLogin) {
    toast.error('登录后才能给组织者发消息')
    return router.push('/login')
  }
  router.push({ path: '/messages', query: { to: String(o.value.id), name: o.value.nickname } })
}

onMounted(async () => {
  try {
    o.value = await api.getOrganizer(route.params.id)
  } catch (e) {
    toast.error(e.message)
  }
})
</script>
