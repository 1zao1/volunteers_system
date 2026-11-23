<template>
  <div class="section-title">
    <div>
      <h2>我的工时</h2>
      <div class="sub">已认定工时来自组织者确认；活动结束后，由组织者认定工时</div>
    </div>
    <router-link to="/activities" class="btn btn-soft btn-sm">
      <i class="bi bi-clock"></i> 参加更多活动
    </router-link>
  </div>

  <section class="big-hero">
    <div>
      <div class="k">累 计 服 务 时 长</div>
      <div class="v">{{ total }}<small>小时</small></div>
      <div class="k" style="margin-top: 6px">
        <i class="bi bi-patch-check"></i>
        {{ level }}
      </div>
    </div>
    <div class="side-stats">
      <div>
        <div class="n">{{ issued.length }}</div>
        <div class="l">服务次数</div>
      </div>
      <div>
        <div class="n">{{ activities }}</div>
        <div class="l">参与活动</div>
      </div>
      <div>
        <div class="n">{{ avg }}</div>
        <div class="l">平均单次（小时）</div>
      </div>
    </div>
  </section>

  <div v-if="upcoming.length" class="panel mt-24 pending-panel">
    <div class="panel-head">
      <h3><i class="bi bi-calendar-check"></i> 待参与（{{ upcoming.length }}）</h3>
      <span class="sub">报名已通过，活动尚未结束；结束后由组织者认定工时</span>
    </div>
    <div v-for="x in upcoming" :key="x.id" class="tl pending">
      <div class="tl-date">{{ fmtDate(x.startTime) }}</div>
      <div>
        <div class="tl-title">{{ x.activityTitle }}</div>
        <div class="tl-sub">
          <span v-if="x.category"><i class="bi bi-tag"></i> {{ x.category }}</span>
          <span style="margin-left: 10px"><i class="bi bi-geo-alt"></i> {{ x.location || '—' }}</span>
        </div>
      </div>
      <div class="tl-hours pending-hours">预计 {{ x.hours }}<small>h</small></div>
    </div>
  </div>

  <div v-if="pending.length" class="panel mt-24 pending-panel">
    <div class="panel-head">
      <h3><i class="bi bi-hourglass-split"></i> 待认定（{{ pending.length }}）</h3>
      <span class="sub">活动已结束，等待组织者认定工时</span>
    </div>
    <div v-for="x in pending" :key="x.id" class="tl pending">
      <div class="tl-date">{{ fmtDate(x.endTime) }}</div>
      <div>
        <div class="tl-title">{{ x.activityTitle }}</div>
        <div class="tl-sub">
          <span v-if="x.category"><i class="bi bi-tag"></i> {{ x.category }}</span>
          <span style="margin-left: 10px"><i class="bi bi-geo-alt"></i> {{ x.location || '—' }}</span>
        </div>
      </div>
      <div class="tl-hours pending-hours">+{{ x.hours }}<small>h</small></div>
    </div>
  </div>

  <div class="panel mt-24">
    <div class="panel-head">
      <h3>已认定记录</h3>
      <span class="sub">按时间倒序，共 {{ issued.length }} 条</span>
    </div>

    <div v-if="issued.length">
      <div v-for="x in issued" :key="x.id" class="tl">
        <div class="tl-date">{{ fmtDate(x.date) }}</div>
        <div>
          <div class="tl-title">{{ x.activityTitle }}</div>
          <div class="tl-sub">
            <span v-if="x.category"><i class="bi bi-tag"></i> {{ x.category }}</span>
            <span style="margin-left: 10px"><i class="bi bi-geo-alt"></i> {{ x.location || '—' }}</span>
          </div>
        </div>
        <div class="tl-hours">{{ x.hours }}<small>h</small></div>
      </div>
    </div>

    <EmptyState
      v-else-if="!loading"
      icon="bi-clock-history"
      title="还没有已认定的工时"
      desc="参加活动并完成服务后，组织者确认出席即可计入工时。"
    />
    <div v-else class="skeleton" style="height: 140px"></div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api'
import EmptyState from '../components/EmptyState.vue'
import { fmtDate } from '../utils/category'
import { toast } from '../utils/toast'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const issued = ref([])
const pending = ref([])
const upcoming = ref([])
const loading = ref(true)

const total = computed(() => issued.value.reduce((s, x) => s + (Number(x.hours) || 0), 0))
const activities = computed(() => new Set(issued.value.map((x) => x.activityTitle)).size)
const avg = computed(() => (issued.value.length ? (total.value / issued.value.length).toFixed(1) : 0))

const level = computed(() => {
  const h = total.value
  if (h >= 100) return '五星志愿者 · 累计超 100 小时'
  if (h >= 50) return '四星志愿者 · 累计 50 小时以上'
  if (h >= 20) return '三星志愿者 · 累计 20 小时以上'
  if (h >= 10) return '二星志愿者 · 累计 10 小时以上'
  if (h > 0) return '一星志愿者 · 已开始记录服务时长'
  return pending.value.length ? '服务已完成，等待工时结算' : (upcoming.value.length ? '已报名活动，等待参与' : '尚未开始记录，先报名一个活动吧')
})

async function load() {
  loading.value = true
  try {
    const data = await api.getMyHours()
    if (Array.isArray(data)) {
      issued.value = data
      pending.value = []
      upcoming.value = []
    } else {
      issued.value = data?.issued || []
      pending.value = data?.pending || []
      upcoming.value = data?.upcoming || []
    }
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
