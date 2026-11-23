<template>
  <div class="section-title">
    <div>
      <h2>组织者工作台</h2>
      <div class="sub">发布活动、审核自己活动的报名，并在认定工时时给出信誉分</div>
    </div>
    <button class="btn btn-ghost btn-sm" @click="load"><i class="bi bi-arrow-clockwise"></i> 刷新</button>
  </div>

  <div class="stats-grid">
    <div class="stat">
      <div class="icon green"><i class="bi bi-calendar2-week"></i></div>
      <div><div class="num">{{ d.totalActivities ?? 0 }}</div><div class="label">我发布的活动（{{ d.publishedActivities ?? 0 }} 场招募中）</div></div>
    </div>
    <div class="stat" style="cursor: pointer" @click="$router.push('/admin/audit')">
      <div class="icon amber"><i class="bi bi-clipboard-check"></i></div>
      <div><div class="num">{{ d.pendingAudit ?? 0 }}</div><div class="label">待我审核的报名</div></div>
    </div>
    <div class="stat" style="cursor: pointer" @click="$router.push('/admin/audit')">
      <div class="icon sky"><i class="bi bi-hourglass-split"></i></div>
      <div><div class="num">{{ d.pendingHours ?? 0 }}</div><div class="label">待认定工时</div></div>
    </div>
    <div class="stat">
      <div class="icon rose"><i class="bi bi-clock-history"></i></div>
      <div><div class="num">{{ d.totalHours ?? 0 }}</div><div class="label">我活动已认定工时</div></div>
    </div>
  </div>

  <div class="two-col mt-24">
    <div class="card list-card" style="padding: 18px 20px">
      <span class="bar"></span>
      <div class="main">
        <div class="t" style="font-size: 15px">待审核报名</div>
        <div class="s">{{ d.pendingAudit ?? 0 }} 条等待处理，通过后志愿者才算出勤</div>
      </div>
      <router-link to="/admin/audit" class="btn btn-soft btn-sm">去审核</router-link>
    </div>
    <div class="card list-card" style="padding: 18px 20px">
      <span class="bar" style="background: linear-gradient(180deg, #035e93, #38bdf8)"></span>
      <div class="main">
        <div class="t" style="font-size: 15px">发布新活动</div>
        <div class="s">当前 {{ d.publishedActivities ?? 0 }} 场正在招募</div>
      </div>
      <router-link to="/admin/activities" class="btn btn-soft btn-sm">去发布</router-link>
    </div>
  </div>

  <div class="two-col mt-24">
    <div class="panel">
      <div class="panel-head"><h3>活动分类分布</h3><span class="sub">按活动数量统计</span></div>
      <div class="bar-list">
        <div v-for="c in d.categoryStats || []" :key="c.name" class="bar-row">
          <span class="bar-label">{{ c.name }}</span>
          <span class="bar-track"><span class="bar-fill" :style="{ width: barWidth(c.count) }"></span></span>
          <span class="bar-val">{{ c.count }}</span>
        </div>
        <div v-if="!(d.categoryStats || []).length" class="muted">暂无数据</div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head"><h3>近 7 天报名趋势</h3><span class="sub">共 {{ trendTotal }} 人次</span></div>
      <div class="trend">
        <div v-for="t in d.trend || []" :key="t.date" class="trend-col">
          <span class="muted" style="font-size: 11.5px">{{ t.count }}</span>
          <span class="trend-bar" :style="{ height: trendHeight(t.count) }"></span>
          <span class="trend-day">{{ t.date }}</span>
        </div>
      </div>
    </div>
  </div>

  <div class="panel mt-24">
    <div class="panel-head"><h3>最近报名</h3><span class="sub">你活动的报名记录</span></div>
    <div class="table-wrap">
      <table class="table">
        <thead><tr><th>志愿者</th><th>活动</th><th>报名时间</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="r in d.recent || []" :key="r.id">
            <td class="strong">{{ r.username }}</td>
            <td>{{ r.activityTitle }}</td>
            <td class="muted">{{ fmtTime(r.signupTime) }}</td>
            <td><span class="pill" :class="statusOf(r.status).cls">{{ statusOf(r.status).text }}</span></td>
          </tr>
          <tr v-if="!(d.recent || []).length"><td colspan="4" class="muted" style="text-align: center; padding: 28px">暂无报名记录</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api'
import { fmtTime, STATUS_TEXT } from '../utils/category'
import { toast } from '../utils/toast'

const d = ref({})
const statusOf = (s) => STATUS_TEXT[s] || { text: s, cls: 'grey' }

const maxCat = computed(() => Math.max(1, ...(d.value.categoryStats || []).map((c) => c.count)))
function barWidth(n) { return `${Math.round((n / maxCat.value) * 100)}%` }

const maxTrend = computed(() => Math.max(1, ...(d.value.trend || []).map((t) => t.count)))
const trendTotal = computed(() => (d.value.trend || []).reduce((s, t) => s + t.count, 0))
function trendHeight(n) { return `${Math.max(6, Math.round((n / maxTrend.value) * 100))}%` }

async function load() {
  try {
    d.value = (await api.getDashboard()) || {}
  } catch (e) {
    toast.error(e.message)
  }
}

onMounted(load)
</script>
