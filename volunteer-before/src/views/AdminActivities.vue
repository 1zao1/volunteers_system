<template>
  <div class="section-title">
    <div>
      <h2>活动管理</h2>
      <div class="sub">
        发布、编辑、上下架活动，报名人数与余位实时同步
      </div>
    </div>
    <button v-if="canManage" class="btn btn-primary btn-sm" @click="openCreate">
      <i class="bi bi-plus-lg"></i> 新建活动
    </button>
  </div>

  <div class="panel">
    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th style="width: 26%">活动</th>
            <th>分类</th>
            <th>开始时间</th>
            <th>报名截止</th>
            <th>报名 / 名额</th>
            <th>工时</th>
            <th>状态</th>
            <th style="text-align: right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in list" :key="a.id">
            <td>
              <div class="flex center gap-8">
                <span class="thumb" :style="{ backgroundImage: `url(${coverUrl(a.cover, a.category)})` }"></span>
                <div>
                  <div class="strong">{{ a.title }}</div>
                  <div class="muted" style="font-size: 12.5px">
                    <i class="bi bi-geo-alt"></i> {{ a.location }}
                  </div>
                </div>
              </div>
            </td>
            <td>
              <span class="pill grey"><i class="bi" :class="styleOf(a.category).icon"></i> {{ a.category }}</span>
            </td>
            <td class="muted nowrap">{{ fmtTime(a.startTime) }}</td>
            <td class="muted nowrap">{{ a.signupDeadline ? fmtTime(a.signupDeadline) : '—' }}</td>
            <td class="nowrap">{{ a.signedCount }} / {{ a.maxCount }}</td>
            <td class="nowrap">{{ a.hours }} h</td>
            <td>
              <span class="pill nowrap" :class="statusPill(a).cls">{{ statusPill(a).text }}</span>
            </td>
            <td>
              <div v-if="canManage" class="flex gap-8" style="justify-content: flex-end">
                <button class="btn btn-ghost btn-sm" @click="goGroup(a)"><i class="bi bi-people"></i> 群聊</button>
                <button class="btn btn-ghost btn-sm" @click="openEdit(a)">编辑</button>
                <button v-if="!a.ended" class="btn btn-soft btn-sm" @click="toggle(a)">
                  {{ a.status === 'published' ? '下架' : '上架' }}
                </button>
                <span v-else class="muted nowrap" style="font-size: 12.5px">已自动下架</span>
                <button class="btn btn-danger btn-sm" @click="remove(a)">删除</button>
              </div>
              <div v-else class="muted" style="font-size: 12px; text-align: right">只读</div>
            </td>
          </tr>
          <tr v-if="!list.length">
            <td colspan="8" class="muted" style="text-align: center; padding: 34px">
              还没有活动，点击右上角「新建活动」发布第一场
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <!-- 新建 / 编辑弹窗 -->
  <div v-if="showModal" class="modal-mask" @click.self="showModal = false">
    <div class="modal" style="max-width: 720px">
      <div class="modal-head">
        <div>
          <h3>{{ form.id ? '编辑活动' : '新建活动' }}</h3>
          <div class="sub">发布后立即出现在活动广场，志愿者可报名</div>
        </div>
        <button class="btn-plain" @click="showModal = false"><i class="bi bi-x-lg"></i></button>
      </div>

      <div class="form-row">
        <div class="field" style="flex: 2">
          <label class="label">活动名称<span class="req">*</span></label>
          <input v-model.trim="form.title" class="input" placeholder="如：社区义诊健康筛查" />
        </div>
        <div class="field">
          <label class="label">活动分类<span class="req">*</span></label>
          <select v-model="form.category" class="select">
            <option v-for="c in CATEGORIES" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label class="label">活动封面</label>
        <div class="cover-picker">
          <button
            v-for="c in COVERS"
            :key="c.key"
            class="cover-thumb"
            :class="{ active: form.cover === c.key }"
            :style="{ backgroundImage: `url(${c.url})` }"
            :title="c.name"
            @click="form.cover = c.key"
          >
            <span>{{ c.name.split(' · ')[0] }}</span>
          </button>
          <button class="cover-thumb upload-cover" title="从桌面上传图片" @click="pickCoverFile">
            <i class="bi bi-upload"></i>
            <span>{{ isLocalCover ? '已上传本地图片' : '从本地上传' }}</span>
          </button>
          <input ref="coverInput" type="file" accept="image/*" style="display: none" @change="onCoverFile" />
        </div>
        <div v-if="isLocalCover" class="cover-local-hint">
          <span class="thumb" :style="{ backgroundImage: `url(${form.cover})` }"></span>
          <span class="muted">当前使用本地图片（已压缩），点上方其他插画可替换</span>
        </div>
        <div class="field" style="margin-top: 10px">
          <input v-model.trim="form.cover" class="input" placeholder="也可粘贴自定义图片地址 https://…" />
        </div>
        <div class="hint">默认按分类自动匹配，可点选上方插画或换成自己的图片链接</div>
      </div>

      <div class="field">
        <label class="label">活动地点<span class="req">*</span></label>
        <input v-model.trim="form.location" class="input" placeholder="如：承德市双桥区中心广场" />
      </div>

      <div class="form-row">
        <div class="field">
          <label class="label">开始时间<span class="req">*</span></label>
          <input v-model="form.startTime" type="datetime-local" class="input" />
        </div>
        <div class="field">
          <label class="label">结束时间<span class="req">*</span></label>
          <input v-model="form.endTime" type="datetime-local" class="input" />
        </div>
        <div class="field">
          <label class="label">报名截止</label>
          <input v-model="form.signupDeadline" type="datetime-local" class="input" />
        </div>
      </div>

      <div class="form-row">
        <div class="field">
          <label class="label">招募人数上限<span class="req">*</span></label>
          <input v-model.number="form.maxCount" type="number" min="1" class="input" />
        </div>
        <div class="field">
          <label class="label">可认定工时（小时）<span class="req">*</span></label>
          <input v-model.number="form.hours" type="number" min="1" class="input" />
        </div>
        <div class="field">
          <label class="label">发布状态</label>
          <select v-model="form.status" class="select">
            <option value="published">立即上架</option>
            <option value="draft">存为草稿</option>
            <option value="closed">标记已结束</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label class="label">活动详情</label>
        <textarea
          v-model.trim="form.description"
          class="textarea"
          rows="3"
          placeholder="岗位职责、注意事项、集合方式等，志愿者报名后会看到这段说明"
        ></textarea>
      </div>

      <div class="modal-foot">
        <button class="btn btn-ghost" @click="showModal = false">取消</button>
        <button class="btn btn-primary" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : form.id ? '保存修改' : '发布活动' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../api'
import { fmtTime, styleOf, CATEGORIES } from '../utils/category'
import { COVERS, coverUrl } from '../utils/covers'
import { toast } from '../utils/toast'
import { useAuthStore } from '../store/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const canManage = computed(() => auth.role === 'organizer')
const list = ref([])
const showModal = ref(false)
const saving = ref(false)
const coverInput = ref(null)
const isLocalCover = computed(() => (form.cover || '').startsWith('data:image'))

function pickCoverFile() {
  coverInput.value?.click()
}

function onCoverFile(e) {
  const f = e.target.files?.[0]
  if (!f) return
  if (f.size > 8 * 1024 * 1024) return toast.error('图片超过 8MB，请压缩后再上传')
  const reader = new FileReader()
  reader.onload = () => {
    const img = new Image()
    img.onload = () => {
      const maxW = 1600
      const scale = Math.min(1, maxW / img.width)
      const w = Math.round(img.width * scale)
      const h = Math.round(img.height * scale)
      const canvas = document.createElement('canvas')
      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0, w, h)
      form.cover = canvas.toDataURL('image/jpeg', 0.82)
      toast.success('封面已上传')
    }
    img.src = reader.result
  }
  reader.readAsDataURL(f)
  e.target.value = ''
}

const blank = () => ({
  id: 0,
  title: '',
  category: '社区服务',
  cover: '',
  location: '',
  startTime: '',
  endTime: '',
  signupDeadline: '',
  maxCount: 20,
  hours: 3,
  status: 'published',
  description: ''
})

const form = reactive(blank())

const STATUS = {
  published: { text: '招募中', cls: 'green' },
  draft: { text: '草稿', cls: 'grey' },
  closed: { text: '已结束', cls: 'amber' },
  cancelled: { text: '已取消', cls: 'rose' }
}
const statusPill = (a) =>
  a.ended ? { text: '已结束', cls: 'amber' } : (STATUS[a.status] || { text: a.status, cls: 'grey' })

function toInput(v) {
  return v ? String(v).replace(' ', 'T').slice(0, 16) : ''
}
function fromInput(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  try {
    list.value = (await api.getAllActivities()) || []
  } catch (e) {
    toast.error(e.message)
  }
}

function openCreate() {
  Object.assign(form, blank())
  showModal.value = true
}

function openEdit(a) {
  Object.assign(form, {
    id: a.id,
    title: a.title,
    category: a.category,
    cover: a.cover || '',
    location: a.location,
    startTime: toInput(a.startTime),
    endTime: toInput(a.endTime),
    signupDeadline: toInput(a.signupDeadline),
    maxCount: a.maxCount,
    hours: a.hours,
    status: a.status,
    description: a.description || ''
  })
  showModal.value = true
}

async function save() {
  if (!form.title || !form.location) return toast.error('活动名称和地点必填')
  if (!form.startTime || !form.endTime) return toast.error('请选择开始与结束时间')
  if (fromInput(form.endTime) <= fromInput(form.startTime)) return toast.error('结束时间必须晚于开始时间')
  if (form.signupDeadline && fromInput(form.signupDeadline) > fromInput(form.startTime)) {
    return toast.error('报名截止时间不能晚于活动开始时间')
  }

  saving.value = true
  const payload = {
    title: form.title,
    category: form.category,
    cover: form.cover,
    location: form.location,
    startTime: fromInput(form.startTime),
    endTime: fromInput(form.endTime),
    signupDeadline: form.signupDeadline ? fromInput(form.signupDeadline) : null,
    maxCount: Number(form.maxCount),
    hours: Number(form.hours),
    status: form.status,
    description: form.description
  }
  try {
    if (form.id) {
      await api.updateActivity(form.id, payload)
      toast.success('活动已更新')
    } else {
      await api.createActivity(payload)
      toast.success('活动已发布')
    }
    showModal.value = false
    await load()
  } catch (e) {
    toast.error(e.message)
  } finally {
    saving.value = false
  }
}

async function toggle(a) {
  try {
    await api.changeActivityStatus(a.id, a.status === 'published' ? 'draft' : 'published')
    toast.success(a.status === 'published' ? '已下架，活动广场不再展示' : '已上架')
    await load()
  } catch (e) {
    toast.error(e.message)
  }
}

async function remove(a) {
  if (!window.confirm(`删除「${a.title}」后，其报名与工时记录也会一并清除，确定继续？`)) return
  try {
    await api.deleteActivity(a.id)
    toast.success('已删除')
    await load()
  } catch (e) {
    toast.error(e.message)
  }
}

const router = useRouter()
function goGroup(a) {
  router.push({ path: '/messages', query: { group: String(a.id) } })
}

onMounted(load)
</script>
