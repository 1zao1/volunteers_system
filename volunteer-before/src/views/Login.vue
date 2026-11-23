<template>
  <div class="auth-app">
    <div class="auth-bg" :style="bgStyle"></div>
    <div class="auth-veil"></div>

    <div class="auth-panel">
      <div class="auth-brand">
        <span class="logo-mark"><i class="bi bi-stars"></i></span>
        <div>
          <div class="brand-name">予光</div>
          <div class="brand-sub">YuGuang · 让善意有回响</div>
        </div>
      </div>

      <h1 class="auth-h1">欢迎回来</h1>
      <p class="auth-p">登录后可以报名活动、发布活动与查看工时。</p>

      <form class="auth-form" @submit.prevent="onSubmit">
        <label class="auth-field">
          <span>用户名</span>
          <input v-model.trim="form.username" placeholder="请输入用户名" autocomplete="username" />
        </label>
        <label class="auth-field">
          <span>密码</span>
          <input v-model.trim="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </label>

        <button class="auth-btn" :disabled="loading">
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </form>

      <div class="auth-links">
        <router-link to="/activities">先浏览活动</router-link>
        <router-link to="/register">注册新账号</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { useAuthStore } from '../store/auth'
import { toast } from '../utils/toast'
import bg from '../assets/login-bg.jpg'

const router = useRouter()
const auth = useAuthStore()
const bgStyle = { backgroundImage: `url(${bg})` }
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function onSubmit() {
  if (!form.username || !form.password) return toast.error('请填写用户名和密码')
  loading.value = true
  try {
    const res = await api.login({ username: form.username, password: form.password })
    auth.setSession(res.token, {
      id: res.id,
      name: res.name,
      nickname: res.nickname,
      avatar: res.avatar,
      signature: res.signature,
      creditScore: res.creditScore,
      role: res.role
    })
    toast.success(`欢迎回来，${res.nickname || res.name}`)
    router.push(res.role === 'organizer' ? '/dashboard' : '/activities')
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>
