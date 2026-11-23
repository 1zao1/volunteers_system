<template>
  <section class="chat-page">
    <aside class="chat-side" :class="{ 'chat-hide': active }">
      <div class="chat-side-head">
        <h2>消息</h2>
        <div class="sub">私聊组织者，或在活动群里和同场志愿者一起沟通</div>

        <div class="chat-search">
          <i class="bi bi-search"></i>
          <input v-model.trim="keyword" placeholder="搜索联系人或群聊名称" />
          <button v-if="keyword" class="clear-btn" @click="keyword = ''"><i class="bi bi-x-lg"></i></button>
        </div>

        <div class="chat-mode">
          <button :class="{ active: mode === 'dm' }" @click="switchMode('dm')">
            <i class="bi bi-chat-dots"></i> 私聊
            <span v-if="dmUnread" class="chat-tab-badge">{{ dmUnread > 99 ? '99+' : dmUnread }}</span>
          </button>
          <button :class="{ active: mode === 'group' }" @click="switchMode('group')">
            <i class="bi bi-people"></i> 活动群聊
            <span v-if="groupUnread" class="chat-tab-badge">{{ groupUnread > 99 ? '99+' : groupUnread }}</span>
          </button>
        </div>
      </div>

      <div v-if="mode === 'dm'" class="chat-contact-list">
        <div
          v-for="c in filteredContacts"
          :key="'dm-' + c.id"
          class="chat-contact"
          :class="{ active: chatType === 'dm' && active && active.id === c.id }"
          @click="openContact(c)"
        >
          <span class="avatar">{{ (c.name || '?').slice(0, 1).toUpperCase() }}</span>
          <span class="cc-main">
            <span class="cc-name">
              <i v-if="c.pinned" class="bi bi-pin-angle-fill pin-flag" title="已置顶"></i>
              <span class="cc-name-text" :title="c.name">{{ c.name }}</span>
              <span class="role-tag" :class="c.role">{{ c.role === 'organizer' ? '组织者' : '志愿者' }}</span>
            </span>
            <span class="cc-last">
              <span class="cc-last-text">{{ c.lastText || '有问题可以直接问 ta' }}</span>
            </span>
          </span>
          <span class="conv-actions" @click.stop>
            <button class="conv-btn" :class="{ on: c.pinned }" :title="c.pinned ? '取消置顶' : '置顶'" @click.stop="togglePin(c, 'dm')">
              <i class="bi" :class="c.pinned ? 'bi-pin-angle-fill' : 'bi-pin-angle'"></i>
            </button>
            <button class="conv-btn danger" title="删除会话" @click.stop="removeConversation(c, 'dm')">
              <i class="bi bi-trash3"></i>
            </button>
          </span>
          <span class="cc-right">
            <span class="cc-time">{{ shortTime(c.lastTime) }}</span>
            <span v-if="c.unread" class="cc-dot">{{ c.unread > 99 ? '99+' : c.unread }}</span>
          </span>
        </div>

        <div v-if="loading && !contacts.length" class="chat-skeleton">
          <div v-for="i in 3" :key="i" class="skeleton" style="height: 62px; border-radius: 16px; margin-bottom: 10px"></div>
        </div>
        <EmptyState
          v-if="!loading && !filteredContacts.length"
          icon="bi-chat-dots"
          :title="keyword ? '没有匹配的联系人' : '暂时还没有可以私聊的人'"
          :desc="keyword ? '换个关键词试试，也可以切换到活动群聊搜索。' : '报名活动通过后就能联系组织者；组织者可以联系报名过自己活动的志愿者。'"
        />
      </div>

      <div v-else class="chat-contact-list">
        <div
          v-for="g in filteredGroups"
          :key="'g-' + g.id"
          class="chat-contact group-row"
          :class="{ active: chatType === 'group' && active && active.id === g.id }"
          @click="openGroup(g)"
        >
          <span class="group-cover" :style="groupCover(g)"></span>
          <span class="cc-main">
            <span class="cc-name">
              <i v-if="g.pinned" class="bi bi-pin-angle-fill pin-flag" title="已置顶"></i>
              <span class="cc-name-text" :title="g.title">{{ g.title }}</span>
            </span>
            <span class="cc-last">
              <span class="cc-last-text">{{ g.lastText || '群聊已建立，来打个招呼吧' }}</span>
            </span>
          </span>
          <span class="conv-actions" @click.stop>
            <button class="conv-btn" :class="{ on: g.pinned }" :title="g.pinned ? '取消置顶' : '置顶'" @click.stop="togglePin(g, 'group')">
              <i class="bi" :class="g.pinned ? 'bi-pin-angle-fill' : 'bi-pin-angle'"></i>
            </button>
            <button class="conv-btn danger" title="删除会话" @click.stop="removeConversation(g, 'group')">
              <i class="bi bi-trash3"></i>
            </button>
          </span>
          <span class="cc-right">
            <span class="cc-time">{{ g.memberCount }} 人</span>
            <span v-if="g.unread" class="cc-dot">{{ g.unread > 99 ? '99+' : g.unread }}</span>
            <span class="cc-time">{{ shortTime(g.lastTime) }}</span>
          </span>
        </div>

        <div v-if="loadingGroups && !groups.length" class="chat-skeleton">
          <div v-for="i in 3" :key="i" class="skeleton" style="height: 62px; border-radius: 16px; margin-bottom: 10px"></div>
        </div>
        <EmptyState
          v-if="!loadingGroups && !filteredGroups.length"
          icon="bi-people"
          :title="keyword ? '没有匹配的群聊' : '还没有可参与的活动群'"
          :desc="keyword ? '换个关键词试试，也可以切换到私聊搜索。' : '组织者会为自己发布的活动建群；报名通过后你就会出现在群里。'"
        />
      </div>
    </aside>

    <main class="chat-main" :class="{ 'chat-hide': !active }">
      <template v-if="active">
        <div class="chat-head">
          <button class="btn-plain chat-back" @click="backToList"><i class="bi bi-chevron-left"></i></button>
          <span v-if="chatType === 'dm'" class="avatar">{{ (active.name || '?').slice(0, 1).toUpperCase() }}</span>
          <span v-else class="group-cover small" :style="groupCover(active)"></span>
          <div class="chat-head-info">
            <div class="strong">{{ chatType === 'dm' ? active.name : active.title }}</div>
            <div class="muted">
              {{ chatType === 'dm'
                ? (active.role === 'organizer' ? '组织者 · 活动发布方' : '志愿者')
                : `活动群聊 · ${active.memberCount ?? 0} 人` }}
            </div>
          </div>
        </div>

        <div ref="scrollBox" class="chat-msgs">
          <div v-for="m in msgs" :key="m.id" class="bubble-row" :class="{ mine: m.mine }">
            <div v-if="chatType === 'group' && !m.mine" class="bubble-name">
              {{ m.senderName }}
              <span v-if="m.senderRole === 'organizer'" class="role-tag organizer">组织者</span>
            </div>
            <div class="bubble">{{ m.content }}</div>
            <div class="bubble-time">{{ m.time }}</div>
          </div>
          <div v-if="!msgs.length" class="chat-empty">
            <i class="bi bi-chat-heart"></i>
            <div>{{ chatType === 'group' ? '群里还没有消息，发第一条吧' : '发条消息，开始交流吧' }}</div>
          </div>
        </div>

        <div class="chat-input">
          <input v-model="draft" maxlength="500" placeholder="输入消息，回车发送…" @keyup.enter="send" />
          <button class="btn btn-primary" :disabled="!draft.trim() || sending" @click="send">
            <i class="bi bi-send"></i>
          </button>
        </div>
      </template>

      <div v-else class="chat-placeholder">
        <i class="bi bi-chat-square-dots"></i>
        <div>选择左侧的会话开始聊天</div>
      </div>
    </main>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import EmptyState from '../components/EmptyState.vue'
import { toast } from '../utils/toast'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const mode = ref('dm')
const chatType = ref('dm')
const contacts = ref([])
const groups = ref([])
const active = ref(null)
const msgs = ref([])
const draft = ref('')
const loading = ref(true)
const loadingGroups = ref(true)
const sending = ref(false)
const scrollBox = ref(null)
let timer = null

const dmUnread = computed(() => contacts.value.reduce((s, c) => s + (Number(c.unread) || 0), 0))
const groupUnread = computed(() => groups.value.reduce((s, g) => s + (Number(g.unread) || 0), 0))

const keyword = ref('')
const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase())
const filteredContacts = computed(() => {
  const k = normalizedKeyword.value
  if (!k) return contacts.value
  return contacts.value.filter((c) =>
    [c.name, c.username, c.lastText, c.role === 'organizer' ? '组织者' : '志愿者']
      .filter(Boolean).some((v) => String(v).toLowerCase().includes(k))
  )
})
const filteredGroups = computed(() => {
  const k = normalizedKeyword.value
  if (!k) return groups.value
  return groups.value.filter((g) =>
    [g.title, g.category, g.lastText, '活动群聊', '群聊']
      .filter(Boolean).some((v) => String(v).toLowerCase().includes(k))
  )
})

const shortTime = (t) => {
  if (!t) return ''
  const s = String(t)
  return s.length >= 16 ? s.slice(5) : s
}

const groupCover = (g) => {
  const name = g.cover && /^(medical|culture|publicity|community|education|environment|rescue)$/.test(g.cover)
    ? g.cover
    : null
  if (g.cover && String(g.cover).startsWith('data:')) return { backgroundImage: `url(${g.cover})` }
  if (g.cover && String(g.cover).startsWith('http')) return { backgroundImage: `url(${g.cover})` }
  return { backgroundImage: `url(${new URL(`../assets/covers/${name || 'community'}.jpg`, import.meta.url).href})` }
}

async function loadContacts(silent = false) {
  if (!silent) loading.value = true
  try {
    const data = (await api.getContacts()) || []
    contacts.value = Array.isArray(data) ? data : []
    if (chatType.value === 'dm' && active.value) {
      const hit = contacts.value.find((c) => c.id === active.value.id)
      if (hit) active.value = { ...active.value, ...hit }
    }
  } catch (e) {
    if (!silent) toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadGroups(silent = false) {
  if (!silent) loadingGroups.value = true
  try {
    const data = (await api.getGroupList()) || []
    groups.value = Array.isArray(data) ? data : []
    if (chatType.value === 'group' && active.value) {
      const hit = groups.value.find((g) => g.id === active.value.id)
      if (hit) active.value = { ...active.value, ...hit }
    }
  } catch (e) {
    if (!silent) toast.error(e.message)
  } finally {
    loadingGroups.value = false
  }
}

async function loadThread(silent = false) {
  if (!active.value) return
  try {
    const data = chatType.value === 'dm'
      ? await api.getThread(active.value.id)
      : await api.getGroupMessages(active.value.id)
    msgs.value = Array.isArray(data) ? data : []
    if (chatType.value === 'group' && active.value) {
      active.value.unread = 0
      const hit = groups.value.find((g) => g.id === active.value.id)
      if (hit) hit.unread = 0
    }
    if (!silent) await scrollBottom()
  } catch (e) {
    if (!silent) toast.error(e.message)
    msgs.value = []
  }
}

function switchMode(m) {
  if (mode.value === m) return
  mode.value = m
  chatType.value = m
  active.value = null
  msgs.value = []
  router.replace({ path: '/messages' })
}

function openContact(c) {
  chatType.value = 'dm'
  active.value = { ...c }
  msgs.value = []
  router.replace({ path: '/messages', query: { to: String(c.id), name: c.name || '' } })
  loadThread()
}

async function openGroup(g) {
  chatType.value = 'group'
  active.value = { ...g }
  msgs.value = []
  router.replace({ path: '/messages', query: { group: String(g.id) } })
  await loadThread()
  g.unread = 0
  if (active.value && active.value.id === g.id) active.value.unread = 0
}

function backToList() {
  active.value = null
  msgs.value = []
  router.replace({ path: '/messages' })
}

async function togglePin(item, type) {
  const next = !item.pinned
  try {
    await api.pinConversation(type, item.id, next)
    item.pinned = next
    if (type === 'dm') await loadContacts(true)
    else await loadGroups(true)
  } catch (e) {
    toast.error(e.message)
  }
}

async function removeConversation(item, type) {
  const label = type === 'dm' ? `与 ${item.name} 的会话` : `「${item.title}」群聊`
  if (!window.confirm(`删除${label}？聊天记录不会删除，之后有新消息会重新出现。`)) return
  try {
    await api.deleteConversation(type, item.id)
    if (type === 'dm') {
      contacts.value = contacts.value.filter((c) => c.id !== item.id)
    } else {
      groups.value = groups.value.filter((g) => g.id !== item.id)
    }
    if (active.value && active.value.id === item.id &&
        ((type === 'dm' && chatType.value === 'dm') || (type === 'group' && chatType.value === 'group'))) {
      backToList()
    }
  } catch (e) {
    toast.error(e.message)
  }
}

async function send() {
  const text = draft.value.trim()
  if (!text || !active.value) return
  sending.value = true
  try {
    if (chatType.value === 'dm') await api.sendMessage(active.value.id, text)
    else await api.sendGroupMessage(active.value.id, text)
    draft.value = ''
    await loadThread(true)
    await scrollBottom()
  } catch (e) {
    toast.error(e.message)
  } finally {
    sending.value = false
  }
}

async function scrollBottom() {
  await nextTick()
  if (scrollBox.value) scrollBox.value.scrollTop = scrollBox.value.scrollHeight
}

async function openGroupById(id) {
  let g = groups.value.find((x) => x.id === id)
  if (!g) {
    try {
      const info = await api.getGroupInfo(id)
      g = info
    } catch (e) {
      toast.error(e.message)
      return
    }
  }
  chatType.value = 'group'
  active.value = { ...g }
  await loadThread()
  if (g.unread) g.unread = 0
  if (active.value && active.value.id === g.id) active.value.unread = 0
}

onMounted(async () => {
  if (!auth.isLogin) {
    toast.error('请先登录后再聊天')
    router.push('/login')
    return
  }
  await Promise.all([loadContacts(), loadGroups()])

  const to = Number(route.query.to)
  const groupId = Number(route.query.group)
  if (groupId) {
    mode.value = 'group'
    await openGroupById(groupId)
  } else if (to) {
    const hit = contacts.value.find((c) => c.id === to)
    chatType.value = 'dm'
    active.value = hit || { id: to, name: String(route.query.name || '组织者'), role: 'organizer', unread: 0 }
    await loadThread()
  } else if (window.innerWidth > 860 && contacts.value.length) {
    chatType.value = 'dm'
    active.value = { ...contacts.value[0] }
    await loadThread()
  }

  timer = setInterval(async () => {
    await loadContacts(true)
    await loadGroups(true)
    if (active.value) await loadThread(true)
  }, 8000)
})

watch(
  () => route.query,
  async (q) => {
    const gid = Number(q.group)
    const to = Number(q.to)
    if (gid) {
      if (chatType.value === 'group' && active.value && active.value.id === gid) return
      mode.value = 'group'
      await openGroupById(gid)
    } else if (to) {
      if (chatType.value === 'dm' && active.value && active.value.id === to) return
      const hit = contacts.value.find((c) => c.id === to)
      chatType.value = 'dm'
      active.value = hit || { id: to, name: String(q.name || '组织者'), role: 'organizer', unread: 0 }
      await loadThread()
    }
  }
)

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>
