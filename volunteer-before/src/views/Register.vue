<template>
  <div class="auth-app register-app">
    <div class="auth-bg" :style="bgStyle"></div>
    <div class="auth-veil"></div>

    <div class="auth-panel wide">
      <div class="auth-brand">
        <span class="logo-mark"><i class="bi bi-stars"></i></span>
        <div>
          <div class="brand-name">加入予光</div>
          <div class="brand-sub">选一个身份，开始你的志愿生活</div>
        </div>
      </div>

      <div class="role-picker">
        <button
          v-for="r in roles"
          :key="r.key"
          type="button"
          class="role-card"
          :class="{ active: form.role === r.key }"
          @click="form.role = r.key"
        >
          <i class="bi" :class="r.icon"></i>
          <span class="t">{{ r.name }}</span>
          <span class="d">{{ r.desc }}</span>
        </button>
      </div>

      <form class="auth-form" @submit.prevent="onSubmit">
        <label class="auth-field">
          <span>用户名</span>
          <input v-model.trim="form.username" placeholder="2-20 个字符，建议英文或拼音" />
        </label>
        <label class="auth-field">
          <span>手机号</span>
          <input v-model.trim="form.phone" maxlength="11" placeholder="11 位手机号" />
        </label>
        <label class="auth-field">
          <span>密码</span>
          <input v-model.trim="form.password" type="password" placeholder="至少 6 位" />
        </label>
        <label class="auth-field">
          <span>技能标签（选填）</span>
          <input v-model.trim="form.skills" placeholder="如：医疗急救,摄影,英语口语" />
        </label>

        <button class="auth-btn" :disabled="loading">
          {{ loading ? '提交中…' : `注册成为${currentRole}` }}
        </button>
      </form>

      <div class="auth-links">
        <router-link to="/login">已有账号，去登录</router-link>
        <router-link to="/activities">先看看活动</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { toast } from '../utils/toast'
import bg from '../assets/login-bg.jpg'

const router = useRouter()
const bgStyle = { backgroundImage: `url(${bg})` }
const loading = ref(false)
const form = reactive({ role: 'volunteer', username: '', phone: '', password: '', skills: '' })

const roles = [
  { key: 'volunteer', name: '志愿者', desc: '报名活动、累计工时与信誉', icon: 'bi-person-heart' },
  { key: 'organizer', name: '组织者', desc: '发布活动、审核报名、认定工时', icon: 'bi-megaphone' }
]

const currentRole = computed(() => roles.find((r) => r.key === form.role)?.name || '志愿者')

async function onSubmit() {
  if (!form.username || !form.phone || !form.password) return toast.error('请填写完整信息')
  if (!/^1\d{10}$/.test(form.phone)) return toast.error('手机号格式不正确')
  if (form.password.length < 6) return toast.error('密码至少 6 位')
  loading.value = true
  try {
    await api.register({
      username: form.username,
      phone: form.phone,
      password: form.password,
      role: form.role,
      skills: form.skills.split(',').map((s) => s.trim()).filter(Boolean)
    })
    toast.success(`注册成功，你已成为${currentRole.value}，请登录`)
    router.push('/login')
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>
