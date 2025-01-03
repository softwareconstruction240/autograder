import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

type User = {
  repoUrl: string
  netId: string
  firstName: string
  lastName: string
  role: 'STUDENT' | 'ADMIN'
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)

  const token =
    document.cookie
      .split('; ')
      .find((row) => row.startsWith('token'))
      ?.split('=')[1] || ''

  const isLoggedIn = computed(() => user.value !== null)

  const isFullyRegistered = computed(() => {
    const user = useAuthStore().user
    if (user == null) {
      return false
    }
    if (user.role == 'STUDENT') {
      if (!user.repoUrl) {
        return false
      }
    }
    return true
  })

  return { user, token, isLoggedIn, isFullyRegistered }
})
