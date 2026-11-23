<template>
  <div class="profile-head">
    <div class="cover-glow"></div>
    <div class="inner">
      <div class="avatar-wrap" @click="pickAvatar">
        <img v-if="isImg" :src="profile.avatar" class="big-avatar img" alt="头像" />
        <span v-else class="big-avatar">{{ initial }}</span>
        <span class="edit-badge"><i class="bi bi-camera"></i></span>
      </div>
      <div class="who">
        <div class="name-row">
          <h1>{{ profile.nickname || profile.username }}</h1>
          <span class="pill green"><i class="bi" :class="roleIcon"></i> {{ roleName }}</span>
        </div>
        <div class="sig">{{ profile.signature || '这个人很低调，还没写签名～' }}</div>
        <div class="meta">
          <span><i class="bi bi-at"></i> {{ profile.username }}</span>
          <span v-if="profile.phone"><i class="bi bi-telephone"></i> {{ profile.phone }}</span>
          <span v-if="profile.skills"><i class="bi bi-tags"></i> {{ profile.skills }}</span>
        </div>
      </div>
      <button class="btn btn-ghost" @click="showEdit = true">
        <i class="bi bi-pencil"></i> 编辑资料
      </button>
    </div>
  </div>

  <div class="profile-grid mt-24">
    <div class="panel">
      <div class="panel-head"><h3>信誉分</h3><span class="sub">违约一次扣 5 分，每 2 小时自动恢复 1 分</span></div>
      <div class="credit-box">
        <div class="credit-num">{{ profile.creditScore ?? 100 }}<small>/ 100</small></div>
        <div class="credit-bar"><span :style="{ width: (profile.creditScore ?? 100) + '%' }" :class="{ low: (profile.creditScore ?? 100) < 85 }"></span></div>
        <div class="credit-desc">{{ profile.creditDesc || '信誉良好' }}</div>
      </div>
      <div class="hint">低于 85 分时每次报名都会提示；2 小时恢复 1 分，直到回到 85 分。组织者与志愿者同样适用。</div>
    </div>

    <div class="panel">
      <div class="panel-head"><h3>{{ statTitle }}</h3><span class="sub">来自你的真实记录</span></div>
      <div class="mini-stats">
        <div>
          <div class="n">{{ stat.a }}</div>
          <div class="l">{{ stat.aLabel }}</div>
        </div>
        <div>
          <div class="n">{{ stat.b }}</div>
          <div class="l">{{ stat.bLabel }}</div>
        </div>
        <div>
          <div class="n">{{ stat.c }}</div>
          <div class="l">{{ stat.cLabel }}</div>
        </div>
      </div>
    </div>
  </div>

  <div class="panel mt-24">
    <div class="panel-head"><h3>账号信息</h3></div>
    <div class="info-grid">
      <div class="kv"><span class="k">用户 ID</span><span class="v">#{{ profile.id }}</span></div>
      <div class="kv"><span class="k">角色</span><span class="v">{{ roleName }}</span></div>
      <div class="kv"><span class="k">注册时间</span><span class="v">{{ profile.createTime || '—' }}</span></div>
      <div class="kv"><span class="k">登录用户名</span><span class="v">{{ profile.username }}</span></div>
    </div>
  </div>

  <div v-if="showEdit" class="modal-mask" @click.self="showEdit = false">
    <div class="modal" style="max-width: 520px">
      <div class="modal-head">
        <div>
          <h3>编辑个人资料</h3>
          <div class="sub">头像、昵称与签名会显示在导航栏和报名记录里</div>
        </div>
        <button class="btn-plain" @click="showEdit = false"><i class="bi bi-x-lg"></i></button>
      </div>

      <div class="field">
        <label class="label">昵称</label>
        <input v-model.trim="form.nickname" class="input" maxlength="20" placeholder="最多 20 个字" />
      </div>

      <div class="field">
        <label class="label">个性签名</label>
        <textarea v-model.trim="form.signature" class="textarea" rows="2" maxlength="80" placeholder="一句话介绍自己，最多 80 字"></textarea>
      </div>

      <div class="field">
        <label class="label">头像</label>
        <div class="avatar-picker">
          <button
            v-for="p in presets"
            :key="p.icon + p.color"
            class="preset"
            :style="{ background: p.color }"
            :class="{ active: form.avatar === `avatar:${p.icon}|${p.color}` }"
            @click="form.avatar = `avatar:${p.icon}|${p.color}`"
          >
            <i class="bi" :class="p.icon"></i>
          </button>
          <button class="preset upload" @click="pickAvatar"><i class="bi bi-upload"></i></button>
        </div>
        <div class="hint">也可以上传一张本地图片（自动压缩到 160px 以内）</div>
      </div>

      <div v-if="avatarPreview" class="avatar-preview">
        <span class="muted">当前头像预览</span>
        <img v-if="isPreviewImg" :src="form.avatar" alt="预览" />
        <span v-else class="avatar">{{ previewLetter }}</span>
      </div>

      <div class="modal-foot">
        <button class="btn btn-ghost" @click="showEdit = false">取消</button>
        <button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
      </div>
    </div>
  </div>

  <input ref="fileInput" type="file" accept="image/*" style="display: none" @change="onFile" />
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../api'
import { useAuthStore } from '../store/auth'
import { toast } from '../utils/toast'

const auth = useAuthStore()
const profile = ref({})
const showEdit = ref(false)
const saving = ref(false)
const fileInput = ref(null)
const form = reactive({ nickname: '', signature: '', avatar: '' })

const roleName = computed(() => auth.roleName)
const roleIcon = computed(() => auth.roleIcon)
const initial = computed(() => (profile.value.nickname || profile.value.username || '?').slice(0, 1).toUpperCase())
const isImg = computed(() => (profile.value.avatar || '').startsWith('data:image'))
const isPreviewImg = computed(() => (form.avatar || '').startsWith('data:image'))
const previewLetter = computed(() => (form.nickname || profile.value.username || '?').slice(0, 1).toUpperCase())
const avatarPreview = computed(() => !!form.avatar)

const presets = [
  { icon: 'bi-person-heart', color: '#0a5c38' },
  { icon: 'bi-flower1', color: '#2f8f6b' },
  { icon: 'bi-star', color: '#b45309' },
  { icon: 'bi-camera', color: '#0369a1' },
  { icon: 'bi-book', color: '#5b21b6' },
  { icon: 'bi-sun', color: '#be123c' }
]

const stat = ref({ a: 0, b: 0, c: 0, aLabel: '', bLabel: '', cLabel: '' })
const statTitle = computed(() => '我的志愿足迹')

async function load() {
  try {
    profile.value = (await api.getProfile()) || {}
    auth.patchUser({
      nickname: profile.value.nickname,
      avatar: profile.value.avatar,
      signature: profile.value.signature,
      creditScore: profile.value.creditScore
    })
  } catch (e) {
    toast.error(e.message)
  }
  loadStats()
}

async function loadStats() {
  try {
    if (auth.role === 'volunteer') {
      const [regs, hours] = await Promise.all([api.getMyRegistrations(), api.getMyHours()])
      const issued = hours?.issued || []
      stat.value = {
        a: (regs || []).length, aLabel: '累计报名',
        b: issued.reduce((s, x) => s + (Number(x.hours) || 0), 0), bLabel: '已认定工时',
        c: new Set(issued.map((x) => x.activityTitle)).size, cLabel: '参与活动'
      }
    } else if (auth.role === 'organizer') {
      const regs = await api.getRegistrations('all')
      stat.value = {
        a: (regs || []).length, aLabel: '收到报名',
        b: (regs || []).filter((r) => r.status === 'pending').length, bLabel: '待审核',
        c: (regs || []).filter((r) => r.status === 'approved').length, cLabel: '已通过'
      }
    } else {
      const d = await api.getDashboard()
      stat.value = {
        a: d.totalActivities ?? 0, aLabel: '全站活动',
        b: d.totalVolunteers ?? 0, bLabel: '志愿者',
        c: d.lowCredit ?? 0, cLabel: '低信誉人数'
      }
    }
  } catch (e) {
    /* 统计失败不影响主页 */
  }
}

function pickAvatar() {
  if (!showEdit.value) {
    showEdit.value = true
    form.nickname = profile.value.nickname || ''
    form.signature = profile.value.signature || ''
    form.avatar = profile.value.avatar || ''
  }
  fileInput.value?.click()
}

function onFile(e) {
  const f = e.target.files?.[0]
  if (!f) return
  const reader = new FileReader()
  reader.onload = () => {
    const img = new Image()
    img.onload = () => {
      const size = 160
      const canvas = document.createElement('canvas')
      canvas.width = size
      canvas.height = size
      const ctx = canvas.getContext('2d')
      const scale = Math.min(size / img.width, size / img.height)
      const w = img.width * scale
      const h = img.height * scale
      ctx.fillStyle = '#0a5c38'
      ctx.fillRect(0, 0, size, size)
      ctx.drawImage(img, (size - w) / 2, (size - h) / 2, w, h)
      form.avatar = canvas.toDataURL('image/jpeg', 0.82)
    }
    img.src = reader.result
  }
  reader.readAsDataURL(f)
  e.target.value = ''
}

async function save() {
  saving.value = true
  try {
    await api.updateProfile({ nickname: form.nickname, signature: form.signature, avatar: form.avatar })
    toast.success('资料已更新')
    showEdit.value = false
    await load()
  } catch (e) {
    toast.error(e.message)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  form.nickname = auth.displayName
  load()
})
</script>
