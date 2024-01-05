import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

type User = {
    repoUrl: string
    netId: string
    firstName: string
    lastName: string
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)

  const isLoggedIn = computed(() => user.value !== null)
  return { user, isLoggedIn }
})
