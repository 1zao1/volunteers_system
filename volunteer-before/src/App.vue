<template>
  <div class="app-shell">
    <NavBar />
    <main class="page">
      <div class="container">
        <router-view />
      </div>
    </main>
    <Toast />
  </div>
</template>

<script setup>
import NavBar from './components/NavBar.vue'
import Toast from './components/Toast.vue'
</script>
