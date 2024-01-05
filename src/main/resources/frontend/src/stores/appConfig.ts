import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

type ImportMeta = {
    VITE_APP_BACKEND_URL: string
}

// @ts-ignore
const env: ImportMeta = import.meta.env;
export const useAppConfigStore = defineStore('appConfig', () => {
  const backendUrl = ref<string>(env.VITE_APP_BACKEND_URL);

  return { backendUrl }
})
