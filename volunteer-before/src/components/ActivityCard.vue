<template>
  <article class="card activity-card">
    <div class="a-cover img-cover" :style="cover">
      <span class="cat-tag"><i class="bi" :class="style.icon"></i>{{ a.category }}</span>
      <span class="state-tag" :class="{ full: isFull, live: started && !ended }">{{ stateText }}</span>
      <div class="a-cover-title">
        <h3>{{ a.title }}</h3>
        <div class="a-cover-sub">{{ a.location }}</div>
      </div>
    </div>
    <div class="a-body">
      <div class="a-meta">
        <div class="row"><i class="bi bi-calendar3"></i><span>{{ range }}</span></div>
        <div class="row"><i class="bi bi-hourglass-split"></i><span>可认定 {{ a.hours }} 小时</span></div>
        <div class="row">
          <i class="bi bi-person-badge"></i>
          <router-link
            v-if="a.organizerId"
            :to="`/organizer/${a.organizerId}`"
            class="org-link"
          >{{ a.organizerName || '组织者' }}</router-link>
          <span v-else>{{ a.organizerName || '组织者' }}</span>
        </div>
      </div>

      <div class="progress-row">
        <span>已报名 {{ a.signedCount }}/{{ a.maxCount }}</span>
        <span>余位 {{ remain }}</span>
      </div>
      <div class="progress"><span :style="{ width: percent + '%' }"></span></div>

      <div class="a-actions">
        <router-link :to="`/activity/${a.id}`" class="btn btn-ghost btn-sm">查看详情</router-link>
        <button
          class="btn btn-primary btn-sm"
          :disabled="isFull || ended || started"
          @click="$emit('signup', a)"
        >
          {{ ended ? '已结束' : started ? '进行中' : isFull ? '已满员' : '立即报名' }}
        </button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { styleOf, timeRange, isEnded, isStarted, remainOf, percentOf } from '../utils/category'
import { coverUrl } from '../utils/covers'

const props = defineProps({ a: { type: Object, required: true } })
defineEmits(['signup'])

const style = computed(() => styleOf(props.a.category))
const cover = computed(() => {
  const url = coverUrl(props.a.cover, props.a.category)
  return {
    backgroundImage: `linear-gradient(180deg, rgba(6,40,26,0.18) 0%, rgba(6,40,26,0.82) 100%), url(${url})`
  }
})
const range = computed(() => timeRange(props.a.startTime, props.a.endTime))
const ended = computed(() => isEnded(props.a))
const started = computed(() => isStarted(props.a))
const isFull = computed(() => remainOf(props.a) <= 0)
const remain = computed(() => remainOf(props.a))
const percent = computed(() => percentOf(props.a))
const stateText = computed(() =>
  ended.value ? '已结束' : started.value ? '活动进行中' : isFull.value ? '已满员' : '招募中'
)
</script>
