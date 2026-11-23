<template>
  <section class="app-hero" :style="heroStyle">
    <div class="app-hero-inner">
      <span class="hero-pill"><i class="bi bi-stars"></i> 承德市社区志愿服务平台</span>
      <h1>让善意流动<br />让每一次付出都有回响</h1>
      <p>从社区义诊到赛事保障，选一个与你时间和技能都合拍的活动。</p>

      <div class="search hero-search">
        <i class="bi bi-search"></i>
        <input v-model.trim="keyword" placeholder="搜索活动名称，如：义诊、马拉松、支教…" />
        <button v-if="keyword" class="clear-btn" @click="keyword = ''"><i class="bi bi-x-lg"></i></button>
      </div>

      <div class="hero-stats">
        <div class="hs"><b>{{ overview.activityCount ?? '—' }}</b><span>招募中活动</span></div>
        <div class="hs"><b>{{ overview.volunteerCount ?? '—' }}</b><span>志愿者</span></div>
        <div class="hs"><b>{{ overview.hoursTotal ?? '—' }}</b><span>已认定工时</span></div>
        <div class="hs"><b>{{ overview.signupCount ?? '—' }}</b><span>报名人次</span></div>
      </div>

      <div v-if="!auth.isLogin" class="hero-guest">
        <span>登录后即可报名，未登录也可以随便逛逛</span>
        <router-link to="/login" class="btn btn-light btn-sm">登录 / 注册</router-link>
      </div>
    </div>
  </section>

  <div class="chips app-chips">
    <button class="chip" :class="{ active: category === '全部' }" @click="category = '全部'">
      全部 <span class="count">{{ list.length }}</span>
    </button>
    <button
      v-for="c in categories"
      :key="c.name"
      class="chip"
      :class="{ active: category === c.name }"
      @click="category = c.name"
    >
      {{ c.name }} <span class="count">{{ c.count }}</span>
    </button>
  </div>

  <div v-if="err" class="panel" style="display: flex; gap: 14px; align-items: center; padding: 20px 22px">
    <i class="bi bi-exclamation-triangle-fill" style="font-size: 22px; color: #c2410c"></i>
    <div style="flex: 1">
      <div class="strong">活动数据加载失败</div>
      <div class="muted" style="font-size: 13px; margin-top: 2px">{{ err }}</div>
    </div>
    <button class="btn btn-primary btn-sm" @click="load">重新加载</button>
  </div>

  <div class="cards-grid" v-else-if="filtered.length">
    <ActivityCard v-for="a in filtered" :key="a.id" :a="a" @signup="signup" />
  </div>

  <div v-else-if="loading" class="cards-grid">
    <div v-for="i in 3" :key="i" class="card" style="height: 330px">
      <div class="skeleton" style="height: 100%"></div>
    </div>
  </div>

  <EmptyState v-else icon="bi-calendar-x" title="没有匹配的活动" desc="换个关键词或分类试试，新活动每周都会上架。" />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import ActivityCard from '../components/ActivityCard.vue'
import EmptyState from '../components/EmptyState.vue'
import { toast } from '../utils/toast'
import { useAuthStore } from '../store/auth'
import hero from '../assets/hero.jpg'

const auth = useAuthStore()
const router = useRouter()
const list = ref([])
const overview = ref({})
const keyword = ref('')
const category = ref('全部')
const loading = ref(true)
const err = ref('')
const heroStyle = { backgroundImage: `url(${hero})` }

const categories = computed(() => {
  const map = new Map()
  list.value.forEach((a) => map.set(a.category, (map.get(a.category) || 0) + 1))
  return [...map.entries()].map(([name, count]) => ({ name, count }))
})

const filtered = computed(() =>
  list.value.filter((a) => {
    const okCat = category.value === '全部' || a.category === category.value
    const okKey =
      !keyword.value ||
      (a.title || '').includes(keyword.value) ||
      (a.location || '').includes(keyword.value)
    return okCat && okKey
  })
)

async function load() {
  loading.value = true
  err.value = ''
  try {
    const [actsRes, ovRes] = await Promise.allSettled([api.getActivities(), api.getOverview()])
    if (actsRes.status !== 'fulfilled') throw actsRes.reason
    const acts = actsRes.value
    list.value = Array.isArray(acts) ? acts : acts?.data || []
    overview.value = ovRes.status === 'fulfilled' && ovRes.value ? ovRes.value : {}
  } catch (e) {
    err.value = e.message
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function signup(a) {
  if (!auth.isLogin) {
    toast.error('登录后才能报名活动')
    router.push('/login')
    return
  }
  if (auth.role === 'organizer' && a.organizerId === auth.user?.id) {
    return toast.error('这是你发布的活动，不能报名自己的活动')
  }
  const credit = auth.creditScore
  if (credit < 60) {
    return toast.error(`您现在的信誉分为：${credit}，低于60分，暂不能报名新活动，每 2 小时恢复 1 分`)
  } else if (credit < 85) {
    const ok = window.confirm(`您现在的信誉分为：${credit}，低于85分，需要做活动换取信誉分，2小时换取1信誉分，直至达到85及以上。低信誉期间同时只能有 1 个未完成的报名。确定继续报名吗？`)
    if (!ok) return
  } else if (credit === 85) {
    const ok = window.confirm('您当前信誉分为85分，若再次违约你就会成为“低信誉志愿者”。确定继续报名吗？')
    if (!ok) return
  }
  try {
    await api.signup(a.id)
    toast.success('报名成功，等待组织者审核')
    await load()
  } catch (e) {
    toast.error(e.message)
  }
}

onMounted(load)
</script>
