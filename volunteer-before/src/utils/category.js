export const CATEGORY_STYLE = {
  医疗健康: { from: '#0a5c38', to: '#14b36a', icon: 'bi-heart-pulse' },
  环境保护: { from: '#035e93', to: '#38bdf8', icon: 'bi-tree' },
  教育助学: { from: '#8a5300', to: '#f2b03c', icon: 'bi-mortarboard' },
  社区服务: { from: '#0b6b6b', to: '#2dd4bf', icon: 'bi-house-heart' },
  大型赛会: { from: '#8f1710', to: '#ef6351', icon: 'bi-trophy' },
  赛事保障: { from: '#8f1710', to: '#ef6351', icon: 'bi-trophy' },
  公益宣传: { from: '#8a1f52', to: '#f472b6', icon: 'bi-megaphone' },
  文化服务: { from: '#4c2a8a', to: '#8b5cf6', icon: 'bi-book' },
  应急救援: { from: '#9a4a00', to: '#fb923c', icon: 'bi-shield-exclamation' }
}

export const CATEGORIES = [
  '医疗健康',
  '社区服务',
  '教育助学',
  '环境保护',
  '文化服务',
  '公益宣传',
  '应急救援',
  '赛事保障'
]

export function styleOf(category) {
  return CATEGORY_STYLE[category] || { from: '#0a5c38', to: '#14b36a', icon: 'bi-stars' }
}

export function coverStyle(category) {
  const s = styleOf(category)
  return { background: `linear-gradient(135deg, ${s.from} 0%, ${s.to} 100%)` }
}

export function detailHeadStyle(category) {
  const s = styleOf(category)
  return {
    background: `radial-gradient(110% 130% at 92% 0%, rgba(255,255,255,0.20), transparent 60%), linear-gradient(120deg, ${s.from} 0%, ${s.to} 100%)`
  }
}

/** 后端已按 yyyy-MM-dd HH:mm 返回 */
export function fmtTime(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 16)
}

export function fmtDate(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 10)
}

/** 同日则省略结束日期 */
export function timeRange(start, end) {
  const s = fmtTime(start)
  const e = fmtTime(end)
  if (!s || !e || s === '—' || e === '—') return '—'
  const sameDay = fmtDate(start) === fmtDate(end)
  return sameDay ? `${s} ~ ${e.slice(11)}` : `${s} ~ ${e}`
}

export function isEnded(activity) {
  if (!activity?.endTime) return false
  const t = new Date(String(activity.endTime).replace(' ', 'T')).getTime()
  return Number.isFinite(t) && t < Date.now()
}

export function isStarted(activity) {
  if (!activity?.startTime) return false
  const t = new Date(String(activity.startTime).replace(' ', 'T')).getTime()
  return Number.isFinite(t) && t < Date.now()
}

export function remainOf(a) {
  return Math.max(0, (a?.maxCount || 0) - (a?.signedCount || 0))
}

export function percentOf(a) {
  if (!a?.maxCount) return 0
  return Math.min(100, Math.round(((a.signedCount || 0) / a.maxCount) * 100))
}

export const STATUS_TEXT = {
  pending: { text: '待审核', cls: 'amber' },
  approved: { text: '已通过', cls: 'green' },
  rejected: { text: '已驳回', cls: 'rose' },
  absent: { text: '缺席', cls: 'rose' }
}

/** 信誉分展示分档 */
export function creditLevel(score) {
  const c = Number(score ?? 100)
  if (c >= 95) return { text: '优秀', cls: 'green' }
  if (c >= 85) return { text: '正常', cls: 'green' }
  if (c >= 80) return { text: '偏低', cls: 'amber' }
  return { text: '受限', cls: 'rose' }
}
